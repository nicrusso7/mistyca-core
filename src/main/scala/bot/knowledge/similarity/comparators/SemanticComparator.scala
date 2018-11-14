package bot.knowledge.similarity.comparators

import bot.knowledge.vocabulary.Stages

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
    val match_result = case_patterns_array.par.filter(pattern => pattern.split(",").par.diff(case_patterns_array).length == 0)
    if(match_result.length > 0) {
      (1,match_result(0))
    }
    else {
      (0,Stages.MISSING_MARKER)
    }
  }
  
  def PatternPercentageMatch(query_array:Array[String], case_patterns_array:Array[String]) : (Double,String) = {
    return (0.0,"");
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
    val match_result = if(case_verbs_array.par.diff(query_array).length < case_verbs_array.length) 1 else 0
    if(match_result > 0) {
      val parsed_verb = case_verbs_array.intersect(query_array)
      (match_result,parsed_verb(0))
    }
    else {
      (match_result,Stages.MISSING_MARKER)
    }
  }
  
  def KeywordsClassesMatch(query_array:Array[String], case_keyword_class:Array[String]) : (String,String,String) = {
    //TODO
    return (null,null,null)
  }
}