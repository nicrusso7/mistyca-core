package bot.reasoning

import org.apache.spark.graphx._
import bot.knowledge.adaptation.Actions
import bot.knowledge.similarity.Measures
import bot.knowledge.vocabulary.Stages
import bot.knowledge.similarity.comparators.SemanticComparator

import scala.collection.mutable.ArrayBuffer
import bot.knowledge.similarity.comparators.SyntaxComparator

class Reasoner(user_id:String, cache:org.apache.spark.broadcast.Broadcast[scala.collection.mutable.Map[String, (scala.collection.mutable.Map[String,Array[(String,String)]], scala.collection.mutable.Map[String,Array[(String,String)]])]]) {
  /*
   * First step function to retrieve the more similar case. It ranks all the Cases in the selected Contexts (in parallel) applying the 
   * selected Similarity Measures Model. 
   */
  def evaluateCases(cases_graph:Graph[Array[String], String], input_query:String, sim_measures_model:String) : Graph[Array[String], String] = {
    
    sim_measures_model match {
      
      case Measures.SEMANTIC_ONLY => {
        //Message format => (Input_Query, Array(Context.Action_Name, Language, (Verb_Match,Verb_Parsed), (Patterns_Percent_Match,Pattern_Parsed), Set(Keywords_Class,Keyword,Keyword_Parsed)))
        val initialMsg: (String, Array[(String, String, (String,String), (String,String), Set[(String,String,String)])]) = (input_query, null)
	      cases_graph.pregel(initialMsg, Integer.MAX_VALUE, EdgeDirection.Out)(vprog_sem, sendMsg_sem, mergeMsg_sem)
      }
      case Measures.SYNTAX_SEMANTIC => {
        //Message format => (Input_Query, Array(Context.Action_Name, Language, (Stanford_Match,Form_Parsed), (Patterns_Match,Pattern_Parsed), Set(Keywords_Class,Keyword,Keyword_Parsed)))
        val initialMsg: (String, Array[(String, String, (String,String), (String,String), Set[(String,String,String)])]) = (input_query, null)
	      cases_graph.pregel(initialMsg, Integer.MAX_VALUE, EdgeDirection.Out)(vprog_syn_sem, sendMsg_syn_sem, mergeMsg_syn_sem)
      }
      case _ => {
        //not supported model
        null
      }
    }
  }
  
  /*
   * PREGEL functions implementation, Semantic-Only model.
   * Message format => (Input_Query, Array(Context.Action_Name, Language, (Verb_Match,Verb_Parsed), (Patterns_Percent_Match,Pattern_Parsed), Set(Keywords_Class,Keyword,Keyword_Parsed)), Response_Code)
   */
  def vprog_sem(vertexId: VertexId, value: Array[String], message: (String, Array[(String, String, (String,String), (String,String), Set[(String,String,String)])])): Array[String] = {
    if(message._2 == null) {
      //initial message pulsing, all neurons are stimulated
      if(vertexId <= Stages.LV0_UPPER) {
        //STAGE 1 - Verb Match
        IntentRetrieval.verbMatch(value,message)
      }
      else {
        //neuron not involved, return same value
        value
      }
    }
    else {
      //evaluate stages
      if(vertexId >= Stages.LV1_LOWER && vertexId <= Stages.LV1_UPPER) {
        //STAGE 2 - Pattern Percentage Match
        IntentRetrieval.PatternPercentageMatch(value,message)
      }
      else if(vertexId >= Stages.LV2_LOWER && vertexId <= Stages.LV2_UPPER) {
        //STAGE 3 - Keywords Classes Match
        IntentRetrieval.keywordsClassesMatch(value,message,cache,user_id)
      }
      else if(vertexId >= Stages.LV3_LOWER && vertexId <= Stages.LV3_UPPER) {
        //STAGE 4 - Cases Evaluation
        IntentRetrieval.casesEvaluation(value,message)
      }
      else if(vertexId == Stages.LV4) {
        //STAGE 5 - Contexts Evaluation
        IntentRetrieval.contextsEvaluation(value,message)
      }
      else if(vertexId >= Stages.LV5_LOWER && vertexId <= Stages.LV5_UPPER) {
        //STAGE 6 - Perform Action
        IntentAdaptation.performAction(value,message)
      }
      else {
        //STAGE 7 - Answer
        IntentAdaptation.answer(value,message)
      }
    }
  }
  //Message format => (Input_Query, Array(Context.Action_Name, Language, (Verb_Match,Verb_Parsed), (Patterns_Percent_Match,Pattern_Parsed), Set(Keywords_Class,Keyword,Keyword_Parsed)), Response_Code)
  def sendMsg_sem(triplet: EdgeTriplet[Array[String], String]): Iterator[(VertexId, (String, Array[(String, String, (String,String), (String,String), Set[(String,String,String)])]))] = {
    if(triplet.srcAttr(0).equals(Stages.EVALUATED_MARKER)) {
      //evaluate stage
      if(triplet.srcId <= Stages.LV0_UPPER) {
        //pulse stage 2
        Iterator((triplet.dstId,(triplet.srcAttr(1), Array((triplet.attr, null, (triplet.srcAttr(2),triplet.srcAttr(3)), null, null)))))
      }
      else if(triplet.srcId >= Stages.LV1_LOWER && triplet.srcId <= Stages.LV1_UPPER) {
        //pulse stage 3
        Iterator((triplet.dstId,(triplet.srcAttr(1), Array((triplet.srcAttr(2), triplet.attr, (triplet.srcAttr(3),triplet.srcAttr(4)),(triplet.srcAttr(5),triplet.srcAttr(6)),null)))))
      }
      else if(triplet.srcId >= Stages.LV2_LOWER && triplet.srcId <= Stages.LV2_UPPER) {
        //pulse stage 4
        //isolate analysis array 
        val analysis_array = triplet.srcAttr.drop(2)
        //filter by Context.Action_Name
        var filtered_analysis = ArrayBuffer[(String, String, (String,String), (String,String), Set[(String,String,String)])]()
        //TODO omg, we can do better...
        for(i<-1 to analysis_array.length/9){
          if(analysis_array(9*(i-1)).equals(triplet.attr)) {
            filtered_analysis += ((analysis_array(9*(i-1)), analysis_array((9*(i-1))+1), (analysis_array((9*(i-1))+2), analysis_array((9*(i-1))+3)), (analysis_array((9*(i-1))+4), analysis_array((9*(i-1))+5)), 
                Set((analysis_array((9*(i-1))+6),analysis_array((9*(i-1))+7),analysis_array((9*(i-1))+8)))))
          }
        }
        //route msgs to Stage 4 by Context.Action_Name
        Iterator((triplet.dstId,(triplet.srcAttr(1),filtered_analysis.toArray))) 
      }
      else if(triplet.srcId >= Stages.LV3_LOWER && triplet.srcId <= Stages.LV3_UPPER) {
        //stage 5: forward Cases analysis to contexts evaluator vertex
        if(!triplet.srcAttr(1).equals(Stages.MISSING_MARKER)) {
          val args_array = triplet.srcAttr.drop(7)
          Iterator((triplet.dstId,(triplet.srcAttr(1), Array((triplet.srcAttr(2), triplet.srcAttr(3),(triplet.srcAttr(4),triplet.srcAttr(5)),(triplet.srcAttr(4),triplet.srcAttr(6)),Set((args_array.mkString(Actions.ARGS_MARKER),null,null)))))))
        }
        else {
          //missing Context msg
          Iterator((triplet.dstId,(triplet.srcAttr(1), Array((triplet.attr, Stages.MISSING_MARKER,null,null,null)))))
        }
      }
      else if(triplet.srcId == Stages.LV4) {
        if(!triplet.srcAttr(1).equals(Stages.MISUNDERSTANDING_MARKER)) {
          //route to Adaptation Stages by Context.Action_Name
          if(triplet.attr.equals(triplet.srcAttr(2))) {
            val args_array = triplet.srcAttr.drop(7)
            Iterator((triplet.dstId,(triplet.srcAttr(1),Array((triplet.srcAttr(2),triplet.srcAttr(3),(triplet.srcAttr(4),""),(triplet.srcAttr(5),triplet.srcAttr(6)),Set((args_array(0),null,null)))))))
          }
          else {
            //deactivate neuron
            Iterator.empty
          }
        }
        else {
          if(triplet.attr.equals(Actions.MISUNDERSTANDING_ACTION)) {
            //route to Misunderstanding
            Iterator((triplet.dstId,(triplet.srcAttr(1),null)))
          }
          else {
            //deactivate neuron
            Iterator.empty
          }
        }
      }
      else {
        //if sync action was performed, route by response code and language ID
        if(!triplet.srcAttr(1).equals(Actions.ASYNC_ACTION_MARKER)) {
          if(triplet.attr.equals(triplet.srcAttr(1))) {
            Iterator((triplet.dstId,(triplet.srcAttr(2),null)))
          }
          else {
            //deactivate neuron
            Iterator.empty
          }
        }
        else {
          //async action performed, the new thread will handle (and push) the response.
          //PREGEL stops here. deactivate neuron
          Iterator.empty
        }
      }
    }
    else {
      //deactivate neuron
      Iterator.empty
    }
  }
  
  def mergeMsg_sem(msg1:(String, Array[(String, String, (String,String), (String,String), Set[(String,String,String)])]), msg2:(String, Array[(String, String, (String,String), (String,String), Set[(String,String,String)])]))
    : (String, Array[(String, String, (String,String), (String,String), Set[(String,String,String)])]) = {
    //regardless the stage, merge msgs
    (msg1._1,msg1._2 ++ msg2._2)
  }
  
  /*
   * Pregel functions implementation, Syntax-Semantic model. 
   * Message format => (Input_Query, Language, Array(Context.Action_Name, (Stanford_Match,Form_Parsed), (Patterns_Match,Pattern_Parsed), (Keywords_Class,Keyword_Parsed)))
   */
  
  def vprog_syn_sem(vertexId: VertexId, value: Array[String], message: (String, Array[(String, String, (String,String), (String,String), Set[(String,String,String)])])): Array[String] = {
    if(message._2 == null) {
      //initial message pulsing, all neurons are stimulated
      if(vertexId <= Stages.LV0_UPPER) {
        //STAGE 1 - Stanford Match
        IntentRetrieval.stanfordParsing(value(0),message._1)
      }
      else {
        //neuron not involved, return same value
        value
      }
    }
    else {
      //evaluate stages
      if(vertexId >= Stages.LV1_LOWER && vertexId <= Stages.LV1_UPPER) {
        //STAGE 2 - Pattern Match
        IntentRetrieval.PatternMatch(value,message)
      }
      else if(vertexId >= Stages.LV2_LOWER && vertexId <= Stages.LV2_UPPER) {
        //STAGE 3 - Keywords Classes Match
        IntentRetrieval.keywordsClassesMatch(value,message,cache,user_id)
      }
      else if(vertexId >= Stages.LV3_LOWER && vertexId <= Stages.LV3_UPPER) {
        //STAGE 4 - Cases Evaluation
        IntentRetrieval.casesEvaluation(value,message)
      }
      else if(vertexId == Stages.LV4) {
        //STAGE 5 - Contexts Evaluation
        IntentRetrieval.contextsEvaluation(value,message)
      }
      else if(vertexId >= Stages.LV5_LOWER && vertexId <= Stages.LV5_UPPER) {
        //STAGE 6 - Perform Action
        IntentAdaptation.performAction(value,message)
      }
      else {
        //STAGE 7 - Answer
        IntentAdaptation.answer(value,message)
      }
    }
  }
  
  //Message format => (Input_Query, Language, Array(Context.Action_Name, (Stanford_Match,Form_Parsed), (Patterns_Match,Pattern_Parsed), (Keywords_Class,Keyword_Parsed)))
  //TODO CODE REPLICATION!! from stage 3 it's equals to sem-only model
  def sendMsg_syn_sem(triplet: EdgeTriplet[Array[String], String]): Iterator[(VertexId, (String, Array[(String, String, (String,String), (String,String), Set[(String,String,String)])]))] = {
    if(triplet.srcAttr(0).equals(Stages.EVALUATED_MARKER)) {
      //evaluate stage
      if(triplet.srcId <= Stages.LV0_UPPER) {
        val analysis = IntentRetrieval.stanfordMatch(triplet.srcAttr(3).split(","), triplet.attr.split(","))
        //pulse stage 2
        Iterator((triplet.dstId,(triplet.srcAttr(1), Array((null, triplet.srcAttr(2), analysis, null, null)))))
      }
      else if(triplet.srcId >= Stages.LV1_LOWER && triplet.srcId <= Stages.LV1_UPPER) {
        //pulse stage 3
        Iterator((triplet.dstId,(triplet.srcAttr(1), Array((triplet.attr, triplet.srcAttr(2), (triplet.srcAttr(3),triplet.srcAttr(4)),(triplet.srcAttr(5),triplet.srcAttr(6)),null)))))
      }
      else if(triplet.srcId >= Stages.LV2_LOWER && triplet.srcId <= Stages.LV2_UPPER) {
        //pulse stage 4
        //isolate analysis array 
        val analysis_array = triplet.srcAttr.drop(2)
        //filter by Context.Action_Name
        var filtered_analysis = ArrayBuffer[(String, String, (String,String), (String,String), Set[(String,String,String)])]()
        //TODO omg, we can do better...
        for(i<-1 to analysis_array.length/9){
          if(analysis_array(9*(i-1)).equals(triplet.attr)) {
            filtered_analysis += ((analysis_array(9*(i-1)), analysis_array((9*(i-1))+1), (analysis_array((9*(i-1))+2), analysis_array((9*(i-1))+3)), (analysis_array((9*(i-1))+4), analysis_array((9*(i-1))+5)), 
                Set((analysis_array((9*(i-1))+6),analysis_array((9*(i-1))+7),analysis_array((9*(i-1))+8)))))
          }
        }
        //route msgs to Stage 4 by Context.Action_Name
        Iterator((triplet.dstId,(triplet.srcAttr(1),filtered_analysis.toArray))) 
      }
      else if(triplet.srcId >= Stages.LV3_LOWER && triplet.srcId <= Stages.LV3_UPPER) {
        //stage 5: forward Cases analysis to contexts evaluator vertex
        if(!triplet.srcAttr(1).equals(Stages.MISSING_MARKER)) {
          val args_array = triplet.srcAttr.drop(7)
          Iterator((triplet.dstId,(triplet.srcAttr(1), Array((triplet.srcAttr(2), triplet.srcAttr(3),(triplet.srcAttr(4),triplet.srcAttr(5)),(triplet.srcAttr(4),triplet.srcAttr(6)),Set((args_array.mkString(Actions.ARGS_MARKER),null,null)))))))
        }
        else {
          //missing Context msg
          Iterator((triplet.dstId,(triplet.srcAttr(1), Array((triplet.attr, Stages.MISSING_MARKER,null,null,null)))))
        }
      }
      else if(triplet.srcId == Stages.LV4) {
        if(!triplet.srcAttr(1).equals(Stages.MISUNDERSTANDING_MARKER)) {
          //route to Adaptation Stages by Context.Action_Name
          if(triplet.attr.equals(triplet.srcAttr(2))) {
            val args_array = triplet.srcAttr.drop(7)
            Iterator((triplet.dstId,(triplet.srcAttr(1),Array((triplet.srcAttr(2),triplet.srcAttr(3),(triplet.srcAttr(4),""),(triplet.srcAttr(5),triplet.srcAttr(6)),Set((args_array(0),null,null)))))))
          }
          else {
            //deactivate neuron
            Iterator.empty
          }
        }
        else {
          if(triplet.attr.equals(Actions.MISUNDERSTANDING_ACTION)) {
            //route to Misunderstanding
            Iterator((triplet.dstId,(triplet.srcAttr(1),null)))
          }
          else {
            //deactivate neuron
            Iterator.empty
          }
        }
      }
      else {
        //if sync action was performed, route by response code and language ID
        if(!triplet.srcAttr(1).equals(Actions.ASYNC_ACTION_MARKER)) {
          if(triplet.attr.equals(triplet.srcAttr(1))) {
            Iterator((triplet.dstId,(triplet.srcAttr(2),null)))
          }
          else {
            //deactivate neuron
            Iterator.empty
          }
        }
        else {
          //async action performed, the new thread will handle (and push) the response.
          //PREGEL stops here. deactivate neuron
          Iterator.empty
        }
      }
    }
    else {
      //deactivate neuron
      Iterator.empty
    }
  }
  
  def mergeMsg_syn_sem(msg1: (String, Array[(String, String, (String,String), (String,String), Set[(String,String,String)])]), msg2: (String, Array[(String, String, (String,String), (String,String), Set[(String,String,String)])]))
   : (String, Array[(String, String, (String,String), (String,String), Set[(String,String,String)])]) = {
    //evaluate stage
    if(msg1._2(0)._4 == null){
      //STAGE 1 - get max result
      if(msg1._2(0)._3._1.toDouble > msg2._2(0)._3._1.toDouble) msg1 else msg2
    }
    else {
      //regardless the stage, merge msgs
      (msg1._1,msg1._2 ++ msg2._2)
    }
  }
}