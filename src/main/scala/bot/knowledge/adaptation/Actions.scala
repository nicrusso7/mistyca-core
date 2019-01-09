package bot.knowledge.adaptation

import org.apache.spark.graphx._
import org.apache.spark.rdd.RDD
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.Path
import scala.collection.mutable.ArrayBuffer
import skills.SkillHandler
import bot.knowledge.vocabulary.Stages
import utils.Json
import utils.Settings

import org.apache.spark.SparkContext
import api.persistence.Datastore
import bot.knowledge.vocabulary.Contexts

object Actions {

  /*
   * Vertices Markers
   */
  val SYNC_ACTION_MARKER = "{syncAction}"
  val ASYNC_ACTION_MARKER = "{asyncAction}"
  
  /*
   * Action Marker
   */
  val REPLACE_MARKER = "%s"
  val ARGS_MARKER = "_=_"
  
  /*
   * Training functions
   */
  def train(sc: SparkContext, json_training: String, persistResult: Boolean) : (String, RDD[(VertexId, Array[String])], RDD[Edge[String]]) = {
    var resume = ""
    var graph_vertices: RDD[(VertexId, Array[String])] = null;
    var graph_edges: RDD[Edge[String]] = null;
    //parse context json
    val parser = new Json()
    val args = parser.parseJSONArray(json_training, "actions")
    //parse context
    val context = args.get(0).get("context") 
    //add Basic Actions offset
    var LV5_count = 0 + basic_ids.keySet.size
    var LV6_count = 0
    var LV5_vertices = new ArrayBuffer[(VertexId,Array[String])]()
    var LV5_edges = new ArrayBuffer[Edge[String]]()
    var vertex_action:(VertexId,Array[String]) = (0,null)
    //actions_path -> (vertices,edges)
    var actions_map = scala.collection.mutable.Map[String, (ArrayBuffer[(VertexId,Array[String])], ArrayBuffer[Edge[String]])]()
    args.keySet().toArray().foreach{ case action_index =>
      val action = args.get(action_index)
      var vertices = new ArrayBuffer[(VertexId,Array[String])]()
      var edges = new ArrayBuffer[Edge[String]]()
      //check LV5 duplicate
			if (LV5_vertices.filter(v=> v._2(1).equals(action.get("name"))).length == 0) {
			  //build action vertex value
        val vertex_value = Array(action.get("type"), action.get("name"), action.get("connector"))
        //assign progressive ids, CaseFactory will map them
        vertex_action = (Stages.LV5_LOWER + LV5_count,vertex_value)
        LV5_vertices += vertex_action
        LV5_count = LV5_count + 1
        //edge to root
        val root_edge = Edge(Stages.LV4,vertex_action._1,context+"."+vertex_action._2(1))
        LV5_edges += root_edge
			}
      else {
        //get current vertex
        vertex_action = LV5_vertices.filter(v=> v._2(1).equals(action.get("name")))(0)
      }
      //link result codes and speeches
      val speeches = parser.parseJSONObject(action.get("speeches"))
      speeches.keySet().toArray().foreach { case code =>
        val speech_vertex_value = parser.parseJSONArray(speeches.get(code))
        //assign progressive ids, CaseFactory will map them
        val vertex_speech = (Stages.LV6_LOWER + LV6_count,speech_vertex_value)
        //link edge with code
        val action_edge = Edge(vertex_action._1,vertex_speech._1,code+"_"+action.get("language"))
        //store vertex and edge
        vertices += vertex_speech
        edges += action_edge
        LV6_count = LV6_count + 1
      }
      //if action already exists, replace it
      val action_path = Contexts.CONTEXTS_BASE_PATH + context + "/actions/" + action.get("language") + "/"
      if(actions_map.contains(action_path)) {
        //merge vertices and edges
        actions_map.put(action_path, (actions_map.get(action_path).get._1 ++ vertices, actions_map.get(action_path).get._2 ++ edges))
      }
      else {
        actions_map.put(action_path, (vertices,edges))
      }
		  //build resume
      resume = resume + "\n" + action_path + "\n" + "[New vertices: " + vertices.map(f=> (f._1,f._2.mkString("|"))).mkString(",") + "]"  + "\n" + "[New edges: " + edges.mkString(",") + "]"
    }
    //persist to hdfs?
    if(persistResult) {
      //parallelize LV5
      graph_vertices = sc.parallelize(LV5_vertices)
      graph_edges = sc.parallelize(LV5_edges)
      //store RDDs
  	  Datastore.saveAsObjectFile[(VertexId, Array[String])](graph_vertices, Contexts.CONTEXTS_BASE_PATH + context + "/actions/vertices")
  	  Datastore.saveAsObjectFile[Edge[String]](graph_edges, Contexts.CONTEXTS_BASE_PATH + context + "/actions/edges")
  	  actions_map.foreach{ case pair =>
        //parallelize LV6
        graph_vertices = sc.parallelize(pair._2._1)
        graph_edges = sc.parallelize(pair._2._2)
        Datastore.createFolder(sc, pair._1)
        //store RDDs
    	  Datastore.saveAsObjectFile[(VertexId, Array[String])](graph_vertices, pair._1 + "vertices")
    	  Datastore.saveAsObjectFile[Edge[String]](graph_edges, pair._1 + "edges")
      }
    }
    //append LV5 resume
    resume = resume + "\n" + "[New LV5 vertices: " + LV5_vertices.map(f=> (f._1,f._2.mkString("|"))).mkString(",") + "]"  + "\n" + "[New LV5 edges: " + LV5_edges.mkString(",") + "]"
    return (resume, graph_vertices, graph_edges)
  }
  
  def export(path:String) : String = {
    return "TODO";
  }
  
  /*
   * Primitives Action functions
   */ 
  val MISUNDERSTANDING_ACTION = "{Misunderstanding}"
  val DEFAULT_LANGUAGE = "DEFAULT_LANGUAGE"
  
  val basic_ids: Map[String,Long] = Map(
	    MISUNDERSTANDING_ACTION -> 5000000000000000000L)
  
  def misunderstanding() : String = {
    //load default language setting
    val settings_reader = new Settings()
    val language = settings_reader.getSettingsValue(DEFAULT_LANGUAGE)
    Speeches.getSpeech(Speeches.MISUNDERSTANDING_SPEECHES.get(language).get, null)
    //return message
  }
  
  /*
   * Perform sync Action
   */
  def SYNC_ACTION = "{syncAction}"
  
  def syncAction(value: Array[String], message: (String, Array[(String, String, (String, String), (String, String), Set[(String, String, String)])])): (String,String) = {
    //reorder args
    var args:ArrayBuffer[String] = new ArrayBuffer[String]()
    //TODO omg, we can do better..
    val target = message._2(0)._5.head._1.split(ARGS_MARKER)
    for(i<-1 to target.length/3) {
      args += (target(3*(i-1))) + ARGS_MARKER + (target((3*(i-1))+2))
    }
    // fill json request template
    val parser = new Json();
    val json_req = parser.fillJSON(args.toArray)
    //invoke Skill Handler
    val skillHandler = new SkillHandler()
    //passing (Connector Name, Skill Name, Args)
    val result = skillHandler.launchSkill(value(2), value(1), json_req)
    if(result == null) {
      (json_req,args.mkString("@"))
    }
    else {
      (result(0) + "_" + message._2(0)._2,result(1))
    }
  }
  
  /*
   * Perform async Action
   */
  def ASYNC_ACTION = "{asyncAction}"
  
  def asyncAction(value: Array[String], message: (String, Array[(String, String, (String, String), (String, String), Set[(String, String, String)])])) {
    //TODO
  }
}