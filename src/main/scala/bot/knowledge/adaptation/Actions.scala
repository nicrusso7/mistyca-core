package bot.knowledge.adaptation

import org.apache.spark.graphx._
import org.apache.spark.rdd.RDD
import scala.collection.mutable.ArrayBuffer
import skills.SkillHandler
import bot.knowledge.vocabulary.Stages
import utils.Json

import org.apache.spark.SparkContext
import api.persistence.Datastore
import bot.knowledge.vocabulary.Contexts

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
  def train(sc: SparkContext, json_training: String, persistResult: Boolean) : String = {
    var resume = ""
    //parse context json
    val parser = new Json()
    val args = parser.parseJSONArray(json_training, "actions")
    //root node LV4
    val root = (Stages.LV4, Array(Stages.LV4_VALUE))
    //parse context
    val context = args.get(0).get("context") 
    //build graph
    args.keySet().toArray().foreach{ case action_index =>
      val action = args.get(action_index)
      var vertices = new ArrayBuffer[(VertexId,Array[String])]()
      var edges = new ArrayBuffer[Edge[String]]()
      //add root
      vertices += root
      //build action vertex value
      val vertex_value = Array(action.get("type"), action.get("name"), action.get("connector"))
      //assign progressive ids, CaseFactory will map them
      val vertex_action = (Stages.LV5_LOWER,vertex_value)
      vertices += vertex_action
      //edge to root
      val root_edge = Edge(root._1,vertex_action._1,context+"."+vertex_action._2(1))
      edges += root_edge
      //link result codes and speeches
      val speeches = parser.parseJSONObject(action.get("speeches"))
      var speech_count = 0
      speeches.keySet().toArray().foreach { case code =>
        val speech_vertex_value = parser.parseJSONArray(speeches.get(code))
        //assign progressive ids, CaseFactory will map them
        val vertex_speech = (Stages.LV6_LOWER + speech_count,speech_vertex_value)
        //link edge with code
        val action_edge = Edge(vertex_action._1,vertex_speech._1,code+"_"+action.get("language"))
        //store vertex and edge
        vertices += vertex_speech
        edges += action_edge
        speech_count = speech_count + 1
      }
      //if action already exists, replace it
      val action_path = Contexts.CONTEXTS_BASE_PATH + context + "/actions/" + action.get("language") + "/"
      if(persistResult) {
        //parallelize
        val graph_vertices: RDD[(VertexId, Array[String])] = sc.parallelize(vertices)
        val graph_edges: RDD[Edge[String]] = sc.parallelize(edges)
        Datastore.createFolder(sc, action_path)
        //store RDDs
  		  Datastore.saveAsObjectFile[(VertexId, Array[String])](graph_vertices, action_path);
  		  Datastore.saveAsObjectFile[Edge[String]](graph_edges, action_path);
      }
		  //build resume
      resume = resume + "\n" + action_path + "\n" + "[New vertices: " + vertices.map(f=> (f._1,f._2.mkString("|"))).mkString(",") + "]"  + "\n" + "[New edges: " + edges.mkString(",") + "]"
    }
    return resume;
  }
  
  def export(path:String) : String = {
    return "TODO";
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