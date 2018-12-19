package bot.knowledge.similarity.comparators

import bot.knowledge.vocabulary.Stages
import bot.knowledge.vocabulary.KeywordClass
import utils.Regex

object SemanticComparator {
  
  /*
   * Semantic similarity measure function used to calculate the presence in the plain Input query
   * of at least one words pattern of the Patterns Array component of the current Case.   
   * 
   * if P(i) - I == EmptySet then 1.0 else 0.0 
   *  
   * Returns a binary value (0 or 1), where 1 represent a match.
   */
  def PatternMatch(query_array:Array[String], case_patterns_array:Array[String]) : (Double,String) = {
    val match_result = case_patterns_array.par.filter(pattern => pattern.split(",").par.diff(query_array).length == 0)
    if(match_result.length > 0) {
      (1,match_result(0))
    }
    else {
      (0,Stages.MISSING_MARKER)
    }
  }
  
  /*
   * Semantic similarity measure function used to calculate the similarity between the plain Input query
   * and the Patterns Array component of the current Case.   
   * 
   * [0,1] = (|P*| - |P*-I|)/|P*|
   * 
   * Where P* is the more similar Pattern for the current Case.
   * Returns a double value between 0 and 1, where 1 represent a max match.
   */
  def PatternPercentageMatch(query_array:Array[String], case_patterns_array:Array[String]) : (Double,String) = {
    val max_match_pattern = case_patterns_array.reduce((x,y)=> if(x.split(",").par.diff(query_array).length < y.split(",").par.diff(query_array).length) x else y)
    val match_result = (max_match_pattern.split(",").length - max_match_pattern.split(",").diff(query_array).length).toDouble / max_match_pattern.split(",").length
    if(match_result == 0.0) {
      (match_result,Stages.MISSING_MARKER)
    }
    else {
      (match_result,max_match_pattern)
    }
  }
  
  /*
   * Semantic similarity measure function used to verify the presence of the verb of the plain Input query
   * in the Verbs Array component of the current Case.   
   * 
   * if |V - I| < |V| then 1.0 else 0.0 
   *  
   * Returns a binary value (0 or 1), where 1 represent a match.
   */
  def VerbMatch(query_array:Array[String], case_verbs_array:Array[String]) : (Double,String) = {
    PatternMatch(query_array,case_verbs_array)
  }
  
  /*
   * Semantic similarity measure function used to verify the presence of a Keyword in the plain Input query
   *  
   * Returns a tuple with the Keyword and the formal value parsed or the missing marker.  
   */
  def KeywordsClassesMatch(query_array:Array[String], case_keyword_class:Array[String], language:String) : (String,String,String) = {
//      cache: org.apache.spark.broadcast.Broadcast[scala.collection.mutable.Map[String, (scala.collection.mutable.Map[String,Array[(String,String)]], scala.collection.mutable.Map[String,Array[(String,String)]])]],
//      user_id:String
    //evaluate KC type
    val kc = new KeywordClass(query_array,case_keyword_class,language)
    case_keyword_class(0) match {
      case kc.BASIC_KEYWORD_MARKER => {
        //reflection on KeywordClass class 
		    val process = kc.getClass.getMethod(case_keyword_class(1))
		    process.invoke(kc).asInstanceOf[(String,String,String)]
      }
      case kc.CUSTOM_KEYWORD_MARKER => {
        //search in the vertex values
        val custom_values = case_keyword_class.drop(2)
        val regex = new Regex()
        val matches = custom_values.filter(k=> regex.containsWord(k.split(kc.CUSTOM_KEYWORD_DIVISOR)(0), query_array.mkString(" ")))
        if(matches.length == 1) {
          (case_keyword_class(1),matches(0).split(kc.CUSTOM_KEYWORD_DIVISOR)(0),matches(0).split(kc.CUSTOM_KEYWORD_DIVISOR)(1))
        }
        else {
          //this means no match OR multiple match. Mark as missing.
          (case_keyword_class(1),Stages.MISSING_MARKER,Stages.MISSING_MARKER)
        }
      }
      case kc.CONTEXT_BASED_KEYWORD_MARKER => {
        //search in the broadcasted cache map TODO
//        val collection = cache.value(user_id)._2(case_keyword_class(1))
//        val regex = new Regex()
//        val matches = collection.filter(k=> regex.containsWord(k._1, query_array.mkString(" ")))
//        if(matches.length == 1) {
//          (case_keyword_class(1),matches(0)._1,matches(0)._2)
//        }
//        else {
//          //this means no match OR multiple match. Mark as missing.
          (case_keyword_class(1),Stages.MISSING_MARKER,Stages.MISSING_MARKER)
//        }
      }
    }
  }
}