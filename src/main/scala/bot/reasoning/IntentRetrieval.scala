package bot.reasoning

import bot.knowledge.similarity.comparators._
import bot.knowledge.vocabulary.Stages
import utils.Regex
import utils.StanfordNLP
import java.awt.SentEvent

object IntentRetrieval {
  
  
  def stanfordParsing(language:String, input_sentence:String): Array[String] = {
    val parsed_sentence = new StanfordNLP().parseSentence(language,input_sentence)
    //create new vertex value and mark the vertex as evaluated
    Array(Stages.EVALUATED_MARKER,input_sentence,language,parsed_sentence)
  }
  
  def stanfordMatch(parsed_input:Array[String], pattern:Array[String]): (String,String) = { 
    val analysis = SyntaxComparator.StanfordMatch(parsed_input, pattern)
    (analysis.toString(),pattern.mkString(","))
  }
  
  def verbMatch(value:Array[String], message:(String, Array[(String, String, (String, String), (String, String), Set[(String, String, String)])])): Array[String] = { 
    val analysis = SemanticComparator.VerbMatch(message._1.split(" "),value)
    //create new vertex value and mark the vertex as evaluated
    Array(Stages.EVALUATED_MARKER,message._1,analysis._1.toString(),analysis._2)
  }
  
  def PatternMatch(value:Array[String], message:(String, Array[(String, String, (String, String), (String, String), Set[(String, String, String)])])): Array[String] = {
    val analysis = SemanticComparator.PatternMatch(message._1.split(" "),value)
    //create new vertex value and mark the vertex as evaluated
    Array(Stages.EVALUATED_MARKER,message._1,message._2(0)._2,message._2(0)._3._1,message._2(0)._3._2,analysis._1.toString(),analysis._2)
  }
  
  def PatternPercentageMatch(value:Array[String], message:(String, Array[(String, String, (String, String), (String, String), Set[(String, String, String)])])): Array[String] = { 
    val analysis = SemanticComparator.PatternPercentageMatch(message._1.split(" "),value)
    //create new vertex value and mark the vertex as evaluated
    Array(Stages.EVALUATED_MARKER,message._1,message._2(0)._1,message._2(0)._3._1,message._2(0)._3._2,analysis._1.toString(),analysis._2)
  }
  
  def keywordsClassesMatch(value:Array[String], message:(String, Array[(String, String, (String, String), (String, String), Set[(String, String, String)])])): Array[String] = { 
     val analysis = SemanticComparator.KeywordsClassesMatch(message._1.split(" "),value)
    //create new vertex value and mark the vertex as evaluated
    Array(Stages.EVALUATED_MARKER,message._1) ++ message._2.map(an=> Array(an._1, an._2, an._3._1, an._3._2, an._4._1, an._4._2, analysis._1, analysis._2, analysis._3)).reduce((a,b)=>a++b)
  }
  
  def casesEvaluation(value:Array[String], message:(String, Array[(String, String, (String, String), (String, String), Set[(String, String, String)])])): Array[String] = {
    //group msgs by Context.Action and Language_ID
    val zipped_analysis = message._2.groupBy(f=>(f._1,f._2,f._3,f._4)).map(f=>f._1->f._2.map(h=>(h._5)))
    //resolve conflicts between Keywords Classes (eventually) and weighs the coefficients
    val cleaned_analysis = zipped_analysis.map(act=>(act._1._1,act._1._2,(act._1._3._1.toDouble*0.5+act._1._4._1.toDouble*0.5).toString(),act._1._3._2,act._1._4._2)->resolveConflicts(act._2.reduce((x,y)=> x++y).toArray))
    //retain non-missed non-ambiguous cases
    val retained_analysis = cleaned_analysis.filter(an=>an._2.forall(pair=> !pair._2.equals(Stages.MISSING_MARKER)))
    if(retained_analysis.isEmpty){
      //no valid cases for this context, mark as missed
      Array(Stages.EVALUATED_MARKER,message._1) ++ Array(Stages.MISSING_MARKER)
    }
    else  {
      //retain the more similar Case for this Context and store analysis (Input_Query, Context.Action, Language, Similarity_Coefficient, Root_Parsed, Pattern_Parsed, Array(Keywords_Class,Keyword,Keyword_Parsed))
      Array(Stages.EVALUATED_MARKER,message._1) ++ retrieveCase(collection.mutable.Map(retained_analysis.toSeq:_*))          
    }
  }
  
  def contextsEvaluation(value:Array[String], message:(String, Array[(String, String, (String, String), (String, String), Set[(String, String, String)])])) : Array[String] = {
    //retain non-missed Context
    val retained_contexts = message._2.filter(ctx=> !ctx._2.equals(Stages.MISSING_MARKER))
    if(retained_contexts.isEmpty) {
      //no valid Context inferred, misunderstanding
      Array(Stages.EVALUATED_MARKER,Stages.MISUNDERSTANDING_MARKER)
    }
    else {
      //reduce to Case w/ max Keywords Classes match. If multiple matches, then choose the max Similarity Coefficient
      val max_classes_number = retained_contexts.reduceLeft((x,y)=> if(x._5.size > y._5.size) x else y)._5.size
      val max_kc_cases = retained_contexts.filter(p=> p._5.size == max_classes_number)
      if(max_kc_cases.length > 1) {
        //get max Similarity Coefficient
        val max_sim_coeff = max_kc_cases.reduceLeft((x,y)=> if(x._3._1.toDouble > y._3._1.toDouble) x else y)._3._1.toDouble
        val max_sim_cases = retained_contexts.filter(p=> p._3._1.toDouble == max_classes_number)
        if(max_sim_cases.length > 1) {
          //ambiguous, misunderstanding
          //maybe you have to check the model....
          Array(Stages.EVALUATED_MARKER,Stages.MISUNDERSTANDING_MARKER)
        }
        else {
          //store Intent
          Array(Stages.EVALUATED_MARKER,message._1) ++ Array(max_sim_cases(0)._1,max_sim_cases(0)._2,max_sim_cases(0)._3._1,max_sim_cases(0)._3._2,max_sim_cases(0)._4._2) ++ Array(max_sim_cases(0)._5.head._1)
        }
      }
      else {
        //store Intent
        Array(Stages.EVALUATED_MARKER,message._1) ++ Array(max_kc_cases(0)._1,max_kc_cases(0)._2,max_kc_cases(0)._3._1,max_kc_cases(0)._3._2,max_kc_cases(0)._4._2) ++ Array(max_kc_cases(0)._5.head._1)
      }
    }
  }
  
  def resolveConflicts(args:Array[(String,String,String)]) : Array[(String,String,String)] = {
    if(args.forall(pair=> !pair._2.equals(Stages.MISSING_MARKER))) {
      //KeywordClass,Keyword,Formal_Value
      var cleaned_args:Array[(String, String, String)] = Array.empty;
      args.foreach{ case arg =>
        if(arg._2.contains(Stages.CONFLICT_MARKER)) {
          var partial_cleaned_args:Array[(String, String, String)] = Array.empty;
          val other_args_set = args.diff(Array(arg))
		      val other_args_array = other_args_set.map(a=>a._2).toArray
		      val canditates_args_array = arg._2.split(Stages.CONFLICT_MARKER)
		      val regex = new Regex()
          for(cand<-canditates_args_array) {
            if(!regex.contains(cand, other_args_array)) {
              partial_cleaned_args :+ ((arg._1,cand,arg._3))
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
  
  /*
   * Recursive function to retrieve the more similar Case for the current Context
   */
  def retrieveCase(retained_analysis:collection.mutable.Map[(String, String, String, String, String), Array[(String, String, String)]]) : Array[String] = {
    
    if(retained_analysis.keySet.size == 0) {
      Array(Stages.MISSING_MARKER)
    }
    else {
      //sort cases by # of extracted Keywords Classes
      val sorted_map = Map(retained_analysis.toSeq.sortWith(_._2.length > _._2.length):_*)
      //get max etracted Keywords Classes number
      val max_classes_number = sorted_map.head._2.length
      //enumerate the cardinality of the Cases with KC max number
      val max_kc_cases = retained_analysis.filter(row => row._2.length == max_classes_number)
      if(max_kc_cases.keySet.size > 1) {
        //multiple Cases match, evaluate Similarity Coefficient
        //get max Similarity Coefficient
        val max_sim_coeff = max_kc_cases.keySet.reduceLeft((a,b)=> if(a._3.toDouble > b._3.toDouble) a else b)._3.toDouble
        //enumerate the cardinality of the Cases with max Similarity Coefficient
        val max_sim_coeff_cases = max_kc_cases.filter(row => row._1._3.toDouble == max_sim_coeff)
        if(max_sim_coeff_cases.keySet.size > 1) {
          //not acceptable, remove Cases and start again
          retrieveCase(retained_analysis.retain((k,v) => !max_sim_coeff_cases.keySet.contains(k)))
        }
        else {
          //single max match, check Similarity Coefficient Threshold
          if(max_sim_coeff_cases.head._1._3.toDouble >= Stages.ACCEPTANCE_THRESHOLD) {
            //match! store analysis
            Array(max_sim_coeff_cases.head._1._1,max_sim_coeff_cases.head._1._2,max_sim_coeff_cases.head._1._3,max_sim_coeff_cases.head._1._4,max_sim_coeff_cases.head._1._5) ++ max_sim_coeff_cases.head._2.map(a=> Array(a._1,a._2,a._3)).reduce((a,b)=>a++b)
          }
          else {
            //not acceptable, remove Case and start again
            retrieveCase(retained_analysis.retain((k,v) => !max_sim_coeff_cases.keySet.contains(k)))
          }
        }
      }
      else {
        //single max match, check Similarity Coefficient Threshold
        if(max_kc_cases.head._1._3.toDouble >= Stages.ACCEPTANCE_THRESHOLD) {
          //match! store analysis
          Array(max_kc_cases.head._1._1,max_kc_cases.head._1._2,max_kc_cases.head._1._3,max_kc_cases.head._1._4,max_kc_cases.head._1._5) ++ max_kc_cases.head._2.map(a=> Array(a._1,a._2,a._3)).reduce((a,b)=>a++b)
        }
        else {
          //not acceptable, remove Case and start again
          retrieveCase(retained_analysis.retain((k,v) => !max_kc_cases.keySet.contains(k)))
        }
      }
    }
  }
}