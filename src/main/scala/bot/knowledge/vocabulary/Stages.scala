package bot.knowledge.vocabulary

object Stages {
  
  val LV0_LOWER = 0L
  val LV0_UPPER = 999999999999999999L
  
  val LV1_LOWER = 1000000000000000000L
  val LV1_UPPER = 1999999999999999999L
  
  val LV2_LOWER = 2000000000000000000L
  val LV2_UPPER = 2999999999999999999L
  
  val LV3_LOWER = 3000000000000000000L
  val LV3_UPPER = 3999999999999999999L
  
  val LV4 = 4000000000000000000L
  val LV4_VALUE = "{evaluation}"
  
  val LV5_LOWER = 5000000000000000000L
  val LV5_UPPER = 5999999999999999999L
  
  val LV6_LOWER = 6000000000000000000L
  
  
  val EVALUATED_MARKER = "{eval}"
  val CONFLICT_MARKER = "_conflict_"
  val ARGS_MARKER = "_@_"
  val MISSING_MARKER = "{miss}"
  val AMBIGUOUS_MARKER = "{ambiguous}"
  val MISUNDERSTANDING_MARKER = "{misunderstanding}"
  val ANALYSIS_END_MARKER = "{end}"
  val ACCEPTANCE_THRESHOLD = 0.49
}