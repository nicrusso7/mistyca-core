package bot.knowledge.adaptation

import org.apache.spark.graphx._
import org.apache.spark.rdd.RDD

object Actions {
  
  /*
   * Vertices Markers
   */
  val SYNC_ACTION_MARKER = "{action.sync}"
  val ASYNC_ACTION_MARKER = "{action.async}"
  
  /*
   * Action Marker
   */
  val REPLACE_MARKER = "%s"
  val ARGS_MARKER = "{=}"
  
  /*
   * Training functions
   */
  def train(json_training:String) : String = {
    return "TODO";
  }
  
  def export(path:String) : String = {
    return "TODO";
  }
  
  /*
   * Build the Cases Graph LV5-6 
   */
  def buildGraph(vertices:RDD[(VertexId, (String, String))], edges:RDD[Edge[String]]) : Graph[Array[String], String] = {
    return null;
  }
  
  /*
   * Primitives Action functions
   */ 
  def MISUNDERSTANDING_ACTION = "{General.Misunderstanding}"
  
  def misunderstanding() : (String,String) = {
    //TODO get Cache and load default language setting
    //return (200_LangID,"")
    (null,null)
  }
  
  /*
   * Perform sync Action
   */
  def SYNC_ACTION = "{syncAction}"
  
  def syncAction(value: Array[String], message: (String, Array[(String, String, (String, String), (String, String), Set[(String, String, String)])])): (String,String) = {
    //TODO
    null
  }
  
  /*
   * Perform async Action
   */
  def ASYNC_ACTION = "{asyncAction}"
  
  def asyncAction(value: Array[String], message: (String, Array[(String, String, (String, String), (String, String), Set[(String, String, String)])])) {
    //TODO
  }
}