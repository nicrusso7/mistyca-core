package bot.reasoning

import bot.knowledge.vocabulary.Stages
import bot.knowledge.adaptation.Actions
import bot.knowledge.adaptation.Speeches

object IntentAdaptation {
  
  def performAction(value:Array[String], message:(String, Array[(String, String, (String, String), (String, String), Set[(String, String, String)])])): Array[String] = {
    //evaluate vertex value for sync-async mode
    value(0) match {
      case Actions.SYNC_ACTION_MARKER => {
        val result = Actions.syncAction(value,message)
        //store result
        Array(Stages.EVALUATED_MARKER,result._1,result._2)
      }
      case Actions.ASYNC_ACTION_MARKER => {
        //launch Action in a new thread 
        Actions.asyncAction(value,message)
        //mark vertex with Async Action marker
        Array(Stages.EVALUATED_MARKER,Actions.ASYNC_ACTION_MARKER)
      }
      case Actions.MISUNDERSTANDING_ACTION => {
        Array(Stages.ANALYSIS_END_MARKER) ++ Array(Actions.misunderstanding())
      }
    } 
  }
  
  def answer(value:Array[String], message:(String, Array[(String, String, (String, String), (String, String), Set[(String, String, String)])])): Array[String] = {
    Array(Stages.ANALYSIS_END_MARKER) ++ Array(Speeches.getSpeech(value, message._1))
  }
}