package bot.knowledge.vocabulary

import utils.Json
import bot.knowledge.adaptation.Actions
import api.persistence.Datastore

import org.apache.spark.SparkContext

object Contexts {
  
   val CONTEXTS_BASE_PATH = "/knowledge/contexts/"
  
  def train(sc: SparkContext, json_training:String) : String = {
    //parse context json
    val parser = new Json()
    val args = parser.parseJSONObject(json_training, "context")
    //get context name
    val context_name = args.get("name")
    //persist context
    val new_path = CONTEXTS_BASE_PATH + context_name
    Datastore.createFolder(sc, new_path)
    return new_path;
  }
  
  def export(path:String) : String = {
    return "TODO";
  }
}