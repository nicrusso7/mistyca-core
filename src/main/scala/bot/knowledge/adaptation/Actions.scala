package bot.knowledge.adaptation

import org.apache.spark.graphx._
import org.apache.spark.rdd.RDD
import scala.collection.mutable.ArrayBuffer
import skills.SkillHandler
import utils.Json

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
    //reorder args TODO rude!
    var args:ArrayBuffer[String] = new ArrayBuffer[String]()
    var entry:(String,String,String) = (null,null,null) 
    message._2(0)._5.head._1.split(ARGS_MARKER).foreach { case arg =>
      if(entry._1 == null) {
        entry = (arg,null,null)
      }
      else if(entry._2 == null) {
        entry = (null,arg,null)
      }
      else if(entry._3 == null) {
        entry = (null,null,arg)
      }
      else {
        args :+ (entry._1 + ARGS_MARKER + entry._3)
        entry = (null,null,null)
      }
    }
    // fill json request template
    val parser = new Json();
    val json_req = parser.fillJSON(args.toArray)
    //invoke Skill Handler
    val skillHandler = new SkillHandler()
    //passing (Connector Name, Skill Name, Args)
    val result = skillHandler.launchSkill(value(2), value(1), json_req)
    (result(0),result(1))
  }
  
  /*
   * Perform async Action
   */
  def ASYNC_ACTION = "{asyncAction}"
  
  def asyncAction(value: Array[String], message: (String, Array[(String, String, (String, String), (String, String), Set[(String, String, String)])])) {
    //TODO
  }
}