package bot.knowledge.cases

import org.apache.spark.graphx._
import utils.Json
import bot.knowledge.vocabulary.Stages
import bot.knowledge.vocabulary.KeywordClass
import scala.collection.mutable.ArrayBuffer

import org.apache.spark.rdd.RDD

import org.apache.spark.SparkContext
import api.persistence.Datastore
import bot.knowledge.vocabulary.Contexts
import bot.knowledge.vocabulary.KeywordClass
import bot.knowledge.adaptation.Actions

object CaseFactory {
  
  def train(sc: SparkContext, json_training: String, persistResult: Boolean) : (String, RDD[(VertexId, Array[String])], RDD[Edge[String]]) = {
    //TODO syn-sem model training
    var resume = ""
    var graph_vertices: RDD[(VertexId, Array[String])] = null;
    var graph_edges: RDD[Edge[String]] = null;
    //extract intents
    //parse context json
    val parser = new Json()
    val args = parser.parseJSONArray(json_training, "intents")
    //parse context
    val context = args.get(0).get("context")    
    //init ID counters
    var LV1_count = 0
    //add offset Basic Keywords
    val keyword_class_instance = new KeywordClass(null,null,null)
    var LV2_count = 0 + keyword_class_instance.basic_ids.keySet.size
    var LV3_count = 0
    //cases_path -> (vertices,edges)
    var cases_map = scala.collection.mutable.Map[String, (ArrayBuffer[(VertexId,Array[String])], ArrayBuffer[Edge[String]])]()
    args.keySet().toArray().foreach{ case intent_index =>
      //extract intent
      val intent = args.get(intent_index)
      //build graph
      var vertices = new ArrayBuffer[(VertexId,Array[String])]()
      var edges = new ArrayBuffer[Edge[String]]()
      //root node LV3
      val root_LV3 = (Stages.LV3_LOWER + LV3_count, Array(intent.get("language") + ":" + context))
      //link root nodes
      val root_edge = Edge(root_LV3._1, Stages.LV4, intent.get("language") + ":" + context)
      vertices += root_LV3
      //update counter LV1
      LV3_count = LV3_count + 1
      edges += root_edge
      //extract utterances
      val utterances = parser.parseJSONObject(intent.get("utterances"))
      //extract tags
      val tags = parser.parseJSONObject(intent.get("tags"))
      //extract pattern
      var LV1_vertex_value = new ArrayBuffer[String]()
      utterances.keySet().toArray().foreach{ case utterance_key => 
        val utterance = utterances.get(utterance_key)
        val current_tags = tags.get(utterance_key)
        //remove tags
        var pattern = utterance
        current_tags.split(",").foreach{ case tag =>
          pattern = pattern.replace(tag, "")
        }
        //clean and format string
        pattern = pattern.replace(" ", ",").split(",").filter(s=> !s.equals("")).mkString(",")
        if(!LV1_vertex_value.contains(pattern)) {
          //add pattern to vertex value
          LV1_vertex_value += pattern
        }
      }
      //build LV1 vertex
      val LV1_vertex = (Stages.LV1_LOWER+LV1_count,LV1_vertex_value.toArray)
      vertices += LV1_vertex
      //update counter LV1
      LV1_count = LV1_count + 1
      //evaluate KC involved
      val keywords_classes = parser.parseJSONArray(intent.get("keywords_classes"))
      keywords_classes.foreach{ case kc_name =>
        //check if already involved in others intents
        val kc_stored = vertices.filter(v=> v._1 >= Stages.LV2_LOWER && v._1 <= Stages.LV2_UPPER && v._2(1).equals(kc_name))
        
        if(kc_stored.length > 0) {
          //already involved
          //attach edge
          edges += Edge(LV1_vertex._1, kc_stored(0)._1, intent.get("language") + ":" + context + "." + intent.get("action"))
        }
        else {
          //evaluate KC type
          //is basic?
          if(keyword_class_instance.BASIC_CLASSES.contains(kc_name)) {
            // attach edge from LV1
            edges += Edge(LV1_vertex._1, keyword_class_instance.basic_ids.get(kc_name).get, intent.get("language") + ":" + context + "." + intent.get("action"))
            //add edge LV2-LV3 (if not present)
            if(edges.filter(p=> p.srcId == keyword_class_instance.basic_ids.get(kc_name).get && p.dstId == root_LV3._1 && p.attr.equals(intent.get("language") + ":" + context + "." + intent.get("action"))).length == 0) {
              edges += Edge(keyword_class_instance.basic_ids.get(kc_name).get, root_LV3._1, intent.get("language") + ":" + context + "." + intent.get("action"))
            }
          }//TODO switch-case adding context based KC.
          else {
            //custom KC, extract values
            val values_map = parser.parseJSONObject(parser.parseJSONObject(intent.get("custom_classes_definition")).get(kc_name))
            //map values into vertex
            val LV2_vertex = (Stages.LV2_LOWER+LV2_count, Array(keyword_class_instance.CUSTOM_KEYWORD_MARKER,kc_name) ++ values_map.keySet().toArray().map(k=> k + keyword_class_instance.CUSTOM_KEYWORD_DIVISOR + values_map.get(k)))
            // attach edge from LV1
            edges += Edge(LV1_vertex._1, LV2_vertex._1, intent.get("language") + ":" + context + "." + intent.get("action"))
            //persist LV2 vertex
            vertices += LV2_vertex
            //add edge LV2-LV3
            edges += Edge(LV2_vertex._1, root_LV3._1, intent.get("language") + ":" + context + "." + intent.get("action"))
            //update counter LV2
            LV2_count = LV2_count + 1
          }
        }
      }
      //if cases already exists, replace them
      val cases_path = Contexts.CONTEXTS_BASE_PATH + context + "/cases/" + intent.get("language") + "/"
      if(cases_map.contains(cases_path)) {
        //merge vertices and edges
        cases_map.put(cases_path, (cases_map.get(cases_path).get._1 ++ vertices, cases_map.get(cases_path).get._2 ++ edges))
      }
      else {
        cases_map.put(cases_path, (vertices,edges))
      }
    }
    cases_map.foreach{ case pair =>
      //persist to hdfs?
      if(persistResult) {
        //parallelize
        graph_vertices = sc.parallelize(pair._2._1)
        graph_edges = sc.parallelize(pair._2._2)     
        Datastore.createFolder(sc, pair._1)
        //store RDDs
  	    Datastore.saveAsObjectFile[(VertexId, Array[String])](graph_vertices, pair._1 + "vertices")
  	    Datastore.saveAsObjectFile[Edge[String]](graph_edges, pair._1 + "edges")
      }
  	  //build resume
      resume =  resume + "\n" + pair._1 + "\n" + "[New vertices: " + pair._2._1.map(f=> (f._1,f._2.mkString("|"))).mkString(",") + "]"  + "\n" + "[New edges: " + pair._2._2.mkString(",") + "]"
    } 
    (resume, graph_vertices, graph_edges)
  }
  
  def export(path:String) : String = {
    return "TODO";
  }
  
  def buildGraph(sc: SparkContext, contexts:Array[String], languages:Array[String]) : Graph[Array[String], String] = {
    //knowledge graph vertices and edges
    var knowledge_vertices:ArrayBuffer[RDD[(VertexId, Array[String])]] = new ArrayBuffer[RDD[(VertexId, Array[String])]]()
    var knowledge_edges:ArrayBuffer[RDD[Edge[String]]] = new ArrayBuffer[RDD[Edge[String]]]()
    //loop context
    contexts.foreach { case ctx =>
      //load actions
      val actions_path = Contexts.CONTEXTS_BASE_PATH + ctx + "/actions/"
      val vertices = Datastore.objectFile[(VertexId, Array[String])](sc, actions_path + "vertices")
      val edges = Datastore.objectFile[Edge[String]](sc, actions_path + "edges")
      knowledge_vertices += vertices
      knowledge_edges += edges
      //loop languages
      languages.foreach { case lang_id =>
        //load cases
        val cases_path = Contexts.CONTEXTS_BASE_PATH + ctx + "/cases/" + lang_id + "/"
        val vertices = Datastore.objectFile[(VertexId, Array[String])](sc, cases_path + "vertices")
        val edges = Datastore.objectFile[Edge[String]](sc, cases_path + "edges")
        knowledge_vertices += vertices
        knowledge_edges += edges
        //load speeches
        val speech_path = Contexts.CONTEXTS_BASE_PATH + ctx + "/actions/" + lang_id + "/"
        val speech_vertices = Datastore.objectFile[(VertexId, Array[String])](sc, speech_path + "vertices")
        val speech_edges = Datastore.objectFile[Edge[String]](sc, speech_path + "edges")
        knowledge_vertices += speech_vertices
        knowledge_edges += speech_edges
      }
    }
    var vertices:ArrayBuffer[(VertexId, Array[String])] = new ArrayBuffer[(VertexId, Array[String])]()
    var edges:ArrayBuffer[Edge[String]] = new ArrayBuffer[Edge[String]]()
    //collect values
    knowledge_vertices.foreach(f=> vertices = vertices ++ f.collect())
    knowledge_edges.foreach(f=> edges = edges ++ f.collect())
    //add Basic Keyword Classes
    val keyword_class_instance = new KeywordClass(null,null,null)
    keyword_class_instance.BASIC_CLASSES.foreach{ case kc =>
      if(edges.filter(p=> keyword_class_instance.basic_ids.get(kc).get == p.srcId).length != 0) {
        //involved KC, add to graph
        val LV2_vertex = (keyword_class_instance.basic_ids.get(kc).get, Array(keyword_class_instance.BASIC_KEYWORD_MARKER,kc))
        vertices += LV2_vertex
      }
    }
    //add Root LV4 vertex
    val root_vertex = (Stages.LV4, Array(Stages.LV4_VALUE))
    vertices += root_vertex
    //add Misunderstanding Basic Action vertex and edge
    val misundertanding_vertex = (Actions.basic_ids.get(Actions.MISUNDERSTANDING_ACTION).get, Array(Actions.MISUNDERSTANDING_ACTION))
    val misunderstanding_edge = Edge(Stages.LV4, Actions.basic_ids.get(Actions.MISUNDERSTANDING_ACTION).get, Actions.MISUNDERSTANDING_ACTION)
    vertices += misundertanding_vertex
    edges += misunderstanding_edge
    //build graph
    Graph.apply(sc.parallelize(vertices), sc.parallelize(edges))
  }
}