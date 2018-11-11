package bot.reasoning

import org.apache.spark.graphx._
import bot.knowledge.adaptation.model.Intent
import bot.knowledge.similarity.Measures
import bot.knowledge.vocabulary.Stages
import bot.knowledge.similarity.comparators.SemanticComparator
import scala.collection.mutable.ArrayBuffer
import utils.Regex
import utils.StanfordNLP
import bot.knowledge.similarity.comparators.SyntaxComparator

object Reasoner {
  /*
   * First step function to retrieve the more similar case. It ranks all the Cases in the selected Contexts (in parallel) applying the 
   * selected Similarity Measures Model. 
   */
  def evaluateCases(cases_graph:Graph[Array[String], String], input_query:String, sim_measures_model:String) : Graph[Array[String], String] = {
    
    sim_measures_model match {
      
      case Measures.SEMANTIC_ONLY => {
        //Message format => (Input_Query, Language, Array(Context.Action_Name, (Verb_Match,Verb_Parsed), (Patterns_Percent_Match,Pattern_Parsed), (Keywords_Class,Keyword_Parsed)))
        val initialMsg: (String, String, Array[(String, (String,String), (String,String), (String,String))]) = (input_query, null, null)
	      cases_graph.pregel(initialMsg, Integer.MAX_VALUE, EdgeDirection.Out)(vprog_sem, sendMsg_sem, mergeMsg_sem)
      }
      case Measures.SYNTAX_SEMANTIC => {
        //Message format => (Input_Query, Language, Array(Context.Action_Name, (Stanford_Match,Form_Parsed), (Patterns_Match,Pattern_Parsed), (Keywords_Class,Keyword_Parsed)))
        val initialMsg: (String, String, Array[(String, (String,String), (String,String), (String,String))]) = (input_query, null, null)
	      cases_graph.pregel(initialMsg, Integer.MAX_VALUE, EdgeDirection.Out)(vprog_syn_sem, sendMsg_syn_sem, mergeMsg_syn_sem)
      }
      case _ => {
        //not supported model
        null
      }
    }
  }
  
  /*
   * Pregel functions implementation, Semantic-Only model. 
   */
  
  def vprog_sem(vertexId: VertexId, value: Array[String], message: (String, String, Array[(String, (String,String), (String,String), (String,String))])): Array[String] = {
    if(message._3 == null) {
      //initial message pulsing, all neurons are stimulated
      if(vertexId <= Stages.LV0_UPPER) {
        //STAGE 1 - Verb Match
        val analysis = SemanticComparator.VerbMatch(message._1.split(" "),value)
        //create new vertex value and mark the vertex as evaluated
        Array(Stages.EVALUATED_MARKER,message._1,analysis._1.toString(),analysis._2)
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
        val analysis = SemanticComparator.PatternPercentageMatch(message._1.split(" "),value)
        //create new vertex value and mark the vertex as evaluated
        Array(Stages.EVALUATED_MARKER,message._1,message._3(0)._1,message._3(0)._2._1,message._3(0)._2._2,analysis._1.toString(),analysis._2)
      }
      else if(vertexId >= Stages.LV2_LOWER && vertexId <= Stages.LV2_UPPER) {
        //STAGE 3 - Keywords Classes Match
        val analysis = SemanticComparator.KeywordsClassesMatch(message._1.split(" "),message._2,value)
        //create new vertex value and mark the vertex as evaluated
        Array(Stages.EVALUATED_MARKER,message._1,message._2) ++ message._3.map(an=> Array(an._1, an._2._1, an._2._2, an._3._1, an._3._2, analysis._1.toString(), analysis._2)).reduce((a,b)=>a++b)
      }
      else if(vertexId >= Stages.LV3_LOWER && vertexId <= Stages.LV3_UPPER ) {
        //group msgs by Context.Action
        val zipped_analysis = message._3.groupBy(f=>(f._1,f._2,f._3)).map(f=>f._1->f._2.map(h=>(h._4)))
        //resolve conflicts between Keywords Classes (eventually) and weighs the coefficients
        val cleaned_analysis = zipped_analysis.map(act=>(act._1._1,(act._1._2._1.toDouble*0.5+act._1._3._1.toDouble*0.5).toString(),act._1._2._2,act._1._3._2)->resolveConflicts(act._2))
        //retain non-missed non-ambiguous cases
        val retained_analysis = cleaned_analysis.filter(an=>an._2.forall(pair=> !pair._2.equals(Stages.MISSING_MARKER)))
        if(retained_analysis.isEmpty){
          //no valid cases for this context, mark as missed
          Array(Stages.MISSING_MARKER)
        }
        else  {
          //get best match for this context
          //TODO 
          //store analysis (Input_Query, Language, Array(Context.Action, Similarity_Coefficient, Root_Parsed, Pattern_Parsed, Array(Keyword_Class,Keyword_Parsed)))
          Array(message._1,message._2) ++ retained_analysis.map(an=> Array(an._1._1,an._1._2,an._1._3,an._1._4) ++ an._2.map(f=>Array(f._1,f._2)).reduce((a,b)=>a++b)).reduce((a,b)=>a++b)
        }
      }
      else {
          //TODO
        null
      }
    }
  }
  
  def resolveConflicts(args:Array[(String,String)]) : Array[(String,String)] = {
    if(args.forall(pair=> !pair._2.equals(Stages.MISSING_MARKER))) {
      var cleaned_args:Array[(String, String)] = Array.empty;
      args.foreach{ case arg =>
        if(arg._2.contains(Stages.CONFLICT_MARKER)) {
          var partial_cleaned_args:Array[(String, String)] = Array.empty;
          val other_args_set = args.diff(Array(arg))
		      val other_args_array = other_args_set.map(a=>a._2).toArray
		      val canditates_args_array = arg._2.split(Stages.CONFLICT_MARKER)
		      val regex = new Regex()
          for(cand<-canditates_args_array) {
            if(!regex.contains(cand, other_args_array)) {
              partial_cleaned_args :+ ((arg._1,cand))
            }
          }
          if(partial_cleaned_args.length == 1) {
            cleaned_args = cleaned_args ++ partial_cleaned_args
          }
          else {
            //ambiguity, mark it
            cleaned_args :+ ((arg._1,Stages.AMBIGUOUS_MARKER))
          }
        }
        else {
          cleaned_args :+ arg
        }
      }
      cleaned_args
    }
    else {
      args
    }
  }
  
  def sendMsg_sem(triplet: EdgeTriplet[Array[String], String]): Iterator[(VertexId, (String, String, Array[(String, (String,String), (String,String), (String,String))]))] = {
    if(triplet.srcAttr(0).equals(Stages.EVALUATED_MARKER)) {
      //evaluate stage
      if(triplet.srcId <= Stages.LV0_UPPER) {
        //pulse stage 2
        Iterator((triplet.dstId,(triplet.srcAttr(1), null, Array((triplet.attr, (triplet.srcAttr(2),triplet.srcAttr(3)), null, null)))))
      }
      else if(triplet.srcId >= Stages.LV1_LOWER && triplet.srcId <= Stages.LV1_UPPER) {
        //pulse stage 3
        Iterator((triplet.dstId,(triplet.srcAttr(1), triplet.attr, Array((triplet.srcAttr(2),(triplet.srcAttr(3),triplet.srcAttr(4)),(triplet.srcAttr(5),triplet.srcAttr(6)),null)))))
      }
      else {
        //isolate analysis array 
        val analysis_array = triplet.srcAttr.drop(3)
        //filter by Context.Action_Name
        var filtered_analysis = ArrayBuffer[(String,(String,String),(String,String),(String,String))]()
        //TODO omg, we can do better...
        for(i<-1 to analysis_array.length/7){
          if(analysis_array(7*(i-1)).equals(triplet.attr)) {
            filtered_analysis += ((analysis_array(7*(i-1)),(analysis_array((7*(i-1))+1),analysis_array((7*(i-1))+2)),(analysis_array((7*(i-1))+3),
                analysis_array((7*(i-1))+4)),(analysis_array((7*(i-1))+5),analysis_array((7*(i-1))+6))))
          }
        }
        //route msgs to Stage 4 by Context.Action_Name
        Iterator((triplet.dstId,(triplet.srcAttr(1), triplet.srcAttr(2), filtered_analysis.toArray))) 
      }
    }
    else {
      //deactivate neuron
      Iterator.empty
    }
  }
  
  def mergeMsg_sem(msg1:(String,String,Array[(String,(String,String),(String,String),(String,String))]), msg2:(String,String,Array[(String,(String,String),(String,String),(String,String))]))
    : (String, String, Array[(String, (String,String), (String,String), (String,String))]) = {
    //regardless the stage, merge msgs
    (msg1._1,msg1._2,msg1._3 ++ msg2._3)
  }
  
  /*
   * Pregel functions implementation, Syntax-Semantic model. 
   * Message format => (Input_Query, Language, Array(Context.Action_Name, (Stanford_Match,Form_Parsed), (Patterns_Match,Pattern_Parsed), (Keywords_Class,Keyword_Parsed)))
   */
  
  def vprog_syn_sem(vertexId: VertexId, value: Array[String], message: (String, String, Array[(String, (String,String), (String,String), (String,String))])): Array[String] = {
    if(message._3 == null) {
      //initial message pulsing, all neurons are stimulated
      if(vertexId <= Stages.LV0_UPPER) {
        //STAGE 1 - Stanford Match
        val parsed_sentence = new StanfordNLP().parseSentence(message._1,value(0))
        //create new vertex value and mark the vertex as evaluated
        Array(Stages.EVALUATED_MARKER,message._1,value(0),parsed_sentence)
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
        val analysis = SemanticComparator.PatternMatch(message._1.split(" "),value)
        //create new vertex value and mark the vertex as evaluated
        Array(Stages.EVALUATED_MARKER,message._1,message._2,message._3(0)._2._1,message._3(0)._2._2,analysis._1.toString(),analysis._2)
      }
      else if(vertexId >= Stages.LV2_LOWER && vertexId <= Stages.LV2_UPPER) {
        //STAGE 3 - Keywords Classes Match
        val analysis = SemanticComparator.KeywordsClassesMatch(message._1.split(" "),message._2,value)
        //create new vertex value and mark the vertex as evaluated
        Array(Stages.EVALUATED_MARKER,message._1,message._2) ++ message._3.map(an=> Array(an._1, an._2._1, an._2._2, an._3._1, an._3._2, analysis._1.toString(), analysis._2)).reduce((a,b)=>a++b)
      }
      else {
        //group msgs by Context.Action
        val zipped_analysis = message._3.groupBy(f=>(f._1,f._2,f._3)).map(f=>f._1->f._2.map(h=>(h._4)))
        //resolve conflicts between Keywords Classes (eventually) and weighs the coefficients
        val cleaned_analysis = zipped_analysis.map(act=>(act._1._1,(act._1._2._1.toDouble*0.5+act._1._3._1.toDouble*0.5).toString(),act._1._2._2,act._1._3._2)->resolveConflicts(act._2))
        //retain non-missed non-ambiguous cases
        val retained_analysis = cleaned_analysis.filter(an=>an._2.forall(pair=> !pair._2.equals(Stages.MISSING_MARKER)))
        if(retained_analysis.isEmpty){
          //no valid cases for this context, mark as missed
          Array(Stages.MISSING_MARKER)
        }
        else {
          //store analysis (Input_Query, Language, Array(Context.Action, Similarity_Coefficient, Root_Parsed, Pattern_Parsed, Array(Keyword_Class,Keyword_Parsed)))
          Array(message._1,message._2) ++ retained_analysis.map(an=> Array(an._1._1,an._1._2,an._1._3,an._1._4) ++ an._2.map(f=>Array(f._1,f._2)).reduce((a,b)=>a++b)).reduce((a,b)=>a++b)
        }
      }
    }
  }
  //Message format => (Input_Query, Language, Array(Context.Action_Name, (Stanford_Match,Form_Parsed), (Patterns_Match,Pattern_Parsed), (Keywords_Class,Keyword_Parsed)))
  def sendMsg_syn_sem(triplet: EdgeTriplet[Array[String], String]): Iterator[(VertexId, (String, String, Array[(String, (String,String), (String,String), (String,String))]))] = {
    if(triplet.srcAttr(0).equals(Stages.EVALUATED_MARKER)) {
      //evaluate stage
      if(triplet.srcId <= Stages.LV0_UPPER) {
        val analysis = SyntaxComparator.StanfordMatch(triplet.srcAttr(3).split(" "), triplet.attr.split(","))
        //pulse stage 2
        Iterator((triplet.dstId,(triplet.srcAttr(1), triplet.srcAttr(2), Array((null, (analysis.toString(),triplet.attr), null, null)))))
      }
      else if(triplet.srcId >= Stages.LV1_LOWER && triplet.srcId <= Stages.LV1_UPPER) {
        //pulse stage 3
        Iterator((triplet.dstId,(triplet.srcAttr(1), triplet.srcAttr(2), Array((triplet.attr,(triplet.srcAttr(3),triplet.srcAttr(4)),(triplet.srcAttr(5),triplet.srcAttr(6)),null)))))
      }
      else {
        //isolate analysis array 
        val analysis_array = triplet.srcAttr.drop(3)
        //filter by Context.Action_Name
        var filtered_analysis = ArrayBuffer[(String,(String,String),(String,String),(String,String))]()
        //TODO omg, we can do better...
        for(i<-1 to analysis_array.length/7){
          if(analysis_array(7*(i-1)).equals(triplet.attr)) {
            filtered_analysis += ((analysis_array(7*(i-1)),(analysis_array((7*(i-1))+1),analysis_array((7*(i-1))+2)),(analysis_array((7*(i-1))+3),
                analysis_array((7*(i-1))+4)),(analysis_array((7*(i-1))+5),analysis_array((7*(i-1))+6))))
          }
        }
        //route msgs to Stage 4 by Context.Action_Name
        Iterator((triplet.dstId,(triplet.srcAttr(1), triplet.srcAttr(2), filtered_analysis.toArray))) 
      }
    }
    else {
      //deactivate neuron
      Iterator.empty
    }
  }
  
  def mergeMsg_syn_sem(msg1: (String, String, Array[(String, (String,String), (String,String), (String,String))]), msg2: (String, String, Array[(String, (String,String), (String,String), (String,String))]))
   : (String, String, Array[(String, (String,String), (String,String), (String,String))]) = {
    //evaluate stage
    if(msg1._3(0)._3 == null){
      //STAGE 1 - get max result
      if(msg1._3(0)._2._1.toDouble > msg2._3(0)._2._1.toDouble) msg1 else msg2
    }
    else {
      //regardless the stage, merge msgs
      (msg1._1,msg1._2,msg1._3 ++ msg2._3)
    }
  }
}