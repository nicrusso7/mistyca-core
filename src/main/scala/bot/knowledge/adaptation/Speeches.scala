package bot.knowledge.adaptation

import bot.knowledge.vocabulary.Languages
import java.util.Random
import scala.collection.mutable.ArrayBuffer

object Speeches {
  
  val MISUNDERSTANDING_SPEECHES:Map[String,Array[String]] = Map(
      Languages.English_GB -> Array("I'm sorry but I don't understand..","I'm so sorry..but I don't know what you mean"),
      Languages.Italian_IT -> Array("Mi dispiace ma non ho capito..","Scusami, non ho capito cosa intendi.."))
      
  def getSpeech(available_speeches:Array[String], api_response_message:String) : String = {
	  val rand = new Random(System.currentTimeMillis());
		val random_index = rand.nextInt(available_speeches.length);
		if(available_speeches(0).contains(Actions.REPLACE_MARKER)) {
			//complete message w/ API message
		  var param_speech = available_speeches(0);
		  val copy_pattern = available_speeches(0);
		  var analysis = new ArrayBuffer[String]();
		  var params = api_response_message;
		  for(v <- params.split(",")) {
		    if(param_speech.contains(Actions.REPLACE_MARKER)) {
		      param_speech = param_speech.replaceFirst(Actions.REPLACE_MARKER, v);
		      if(!param_speech.contains(Actions.REPLACE_MARKER)) {
		        param_speech = param_speech + ".";
		        analysis += param_speech;
		        param_speech = copy_pattern;
		      }
		    }
		  }
		  analysis.mkString(" ");
		}
		else {
		  //return random speech
			available_speeches(random_index);
		}
	}
}