package bot.knowledge.similarity.comparators

object SyntaxComparator {
  
  /*
   * Similarity measure function used to calculate the similarity between the Stanford Parsing of the Input query
   * and the Stanford Parsing component of the current Case.   
   * 
   * 1 - ( ( |I-C| + |C-I| ) / |I| + |C| ) = [0,1]
   *  
   * This function is equivalent to the Sorensen-Dice coefficient.
   * Returns a double value between 0 and 1, where 1 represent a max match.
   */
  def StanfordMatch(query_array:Array[String], case_array:Array[String]) : Double = {
    return (1 - ( (query_array.par.diff(case_array).length + case_array.par.diff(query_array).length).toDouble / 
        (query_array.length + query_array.length).toDouble));
  }
}