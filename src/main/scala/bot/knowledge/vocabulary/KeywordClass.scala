package bot.knowledge.vocabulary

class KeywordClass(query_array:Array[String], case_keyword_class:Array[String], language:String) {
  /*
   * Vertices Markers
   */
  val BASIC_KEYWORD_MARKER = "{base}"
  val CUSTOM_KEYWORD_MARKER = "{custom}"
  val CONTEXT_BASED_KEYWORD_MARKER = "{context_based}"
  
  val CUSTOM_KEYWORD_DIVISOR = "{:}"
  
  /*
	 * Primitives Keywords Classes
	 */
	val PLAIN_TEXT_CLASS = "PLAIN_TEXT"
	val NUMBER_CLASS = "NUMBER"
	val BASIC_CLASSES = Array(PLAIN_TEXT_CLASS,NUMBER_CLASS)
	/*
	 * Primitives Keywords Classes IDs
	 */
	val basic_ids: Map[String,Long] = Map(
	    PLAIN_TEXT_CLASS -> 2000000000000000000L,
	    NUMBER_CLASS -> 2000000000000000001L)
  
  def PLAIN_TEXT() : (String,String,String) = {
    (PLAIN_TEXT_CLASS,"",query_array.mkString(" "))
  }
  
  def NUMBER() : (String,String,String) = {
    val result_seq = query_array.intersect(number_values(language).keySet.toSeq) ++ query_array.intersect(number_values(language).valuesIterator.toSeq)
    val value = if(result_seq.length > 1) result_seq.mkString(Stages.CONFLICT_MARKER) else if(result_seq.length == 0) Stages.MISSING_MARKER else result_seq(0)
		//check (and mark) possible conflicts
		(NUMBER_CLASS,value,value)
  }
  
  val number_values:Map[String,Map[String, String]] = Map(
				Languages.English_GB -> Map("one" -> "1",
						"two" -> "2",
						"three" -> "3",
						"four" -> "4",
						"five" -> "5",
						"six" -> "6",
						"seven" -> "7",
						"eight" -> "8",
						"nine" -> "9",
						"ten" -> "10",
						"eleven" -> "11",
						"twelve" -> "12",
						"thirteen" -> "13",
						"fourteen" -> "14",
						"fifteen" -> "15",
						"sixteen" -> "16",
						"seventeen" -> "17",
						"eighteen" -> "18",
						"nineteen" -> "19",
						"twenty" -> "20",
						"twenty one" -> "21",
						"twenty two" -> "22",
						"twenty three" -> "23",
						"twenty four" -> "24",
						"twenty five" -> "25",
						"twenty six" -> "26",
						"twenty seven" -> "27",
						"twenty eight" -> "28",
						"twenty nine" -> "29",
						"thirty" -> "30",
						"thirty one" -> "31",
						"thirty two" -> "32",
						"thirty three" -> "33",
						"thirty four" -> "34",
						"thirty five" -> "35",
						"thirty six" -> "36",
						"thirty seven" -> "37",
						"thirty eight" -> "38",
						"thirty nine" -> "39",
						"forty" -> "40",
						"forty one" -> "41",
						"forty two" -> "42",
						"forty three" -> "43",
						"forty four" -> "44",
						"forty five" -> "45",
						"forty six" -> "46",
						"forty seven" -> "47",
						"forty eight" -> "48",
						"forty nine" -> "49",
						"fifty" -> "50",
						"fifty one" -> "51",
						"fifty two" -> "52",
						"fifty three" -> "53",
						"fifty four" -> "54",
						"fifty five" -> "55",
						"fifty six" -> "56",
						"fifty seven" -> "57",
						"fifty eight" -> "58",
						"fifty nine" -> "59",
						"sixty" -> "60",
						"sixty one" -> "61",
						"sixty two" -> "62",
						"sixty three" -> "63",
						"sixty four" -> "64",
						"sixty five" -> "65",
						"sixty six" -> "66",
						"sixty seven" -> "67",
						"sixty eight" -> "68",
						"sixty nine" -> "69",
						"seventy" -> "70",
						"seventy one" -> "71",
						"seventy two" -> "72",
						"seventy three" -> "73",
						"seventy four" -> "74",
						"seventy five" -> "75",
						"seventy six" -> "76",
						"seventy seven" -> "77",
						"seventy eight" -> "78",
						"seventy nine" -> "79",
						"eighty" -> "80",
						"eighty one" -> "81",
						"eighty two" -> "82",
						"eighty three" -> "83",
						"eighty four" -> "84",
						"eighty five" -> "85",
						"eighty six" -> "86",
						"eighty seven" -> "87",
						"eighty eight" -> "88",
						"eighty nine" -> "89",
						"ninety" -> "90",
						"ninety one" -> "91",
						"ninety two" -> "92",
						"ninety three" -> "93",
						"ninety four" -> "94",
						"ninety five" -> "95",
						"ninety six" -> "96",
						"ninety seven" -> "97",
						"ninety eight" -> "98",
						"ninety nine" -> "99",
						"one hundred" -> "100",
						"zero" -> "0"),
				Languages.Italian_IT -> Map("uno" -> "1",
						"due" -> "2",
						"tre" -> "3",
						"quattro" -> "4",
						"cinque" -> "5",
						"sei" -> "6",
						"sette" -> "7",
						"otto" -> "8",
						"nove" -> "9",
						"dieci" -> "10",
						"undici" -> "11",
						"dodici" -> "12",
						"tredici" -> "13",
						"quattordici" -> "14",
						"quindici" -> "15",
						"sedici" -> "16",
						"diciassette" -> "17",
						"diciotto" -> "18",
						"diciannove" -> "19",
						"venti" -> "20",
						"ventuno" -> "21",
						"ventidue" -> "22",
						"ventitre" -> "23",
						"ventiquattro" -> "24",
						"venticinque" -> "25",
						"ventisei" -> "26",
						"ventisette" -> "27",
						"ventotto" -> "28",
						"ventinove" -> "29",
						"trenta" -> "30",
						"trentuno" -> "31",
						"trentadue" -> "32",
						"trentatre" -> "33",
						"treantaquattro" -> "34",
						"trentacinque" -> "35",
						"trentasei" -> "36",
						"trentasette" -> "37",
						"trentotto" -> "38",
						"trentanove" -> "39",
						"quaranta" -> "40",
						"quarantuno" -> "41",
						"quarantadue" -> "42",
						"quarantatre" -> "43",
						"quarantaquattro" -> "44",
						"quarantacinque" -> "45",
						"quarantasei" -> "46",
						"quarantasette" -> "47",
						"quarantotto" -> "48",
						"quarantanove" -> "49",
						"cinquanta" -> "50",
						"cinquantuno" -> "51",
						"cinquantadue" -> "52",
						"cinquantatre" -> "53",
						"cinquantaquattro" -> "54",
						"cinquantacinque" -> "55",
						"cinquantasei" -> "56",
						"cinquantasette" -> "57",
						"cinquantotto" -> "58",
						"cinquantanove" -> "59",
						"sessanta" -> "60",
						"sessantuno" -> "61",
						"sessantadue" -> "62",
						"sessantatre" -> "63",
						"sessantaquattro" -> "64",
						"sessantacinque" -> "65",
						"sessantasei" -> "66",
						"sessantasette" -> "67",
						"sessantotto" -> "68",
						"sessantanove" -> "69",
						"settanta" -> "70",
						"settantuno" -> "71",
						"settantadue" -> "72",
						"settantatre" -> "73",
						"settantaquattro" -> "74",
						"settantacinque" -> "75",
						"settantasei" -> "76",
						"settantasette" -> "77",
						"settantotto" -> "78",
						"settantanove" -> "79",
						"ottanta" -> "80",
						"ottantuno" -> "81",
						"ottantadue" -> "82",
						"ottantatre" -> "83",
						"ottantaquattro" -> "84",
						"ottantacinque" -> "85",
						"ottantasei" -> "86",
						"ottantasette" -> "87",
						"ottantotto" -> "88",
						"ottantanove" -> "89",
						"novanta" -> "90",
						"novantuno" -> "91",
						"novantadue" -> "92",
						"novantatre" -> "93",
						"novantaquattro" -> "94",
						"novantacinque" -> "95",
						"novantasei" -> "96",
						"novantasette" -> "97",
						"novantotto" -> "98",
						"novantanove" -> "99",
						"cento" -> "100",
						"zero" -> "0"))
		
}