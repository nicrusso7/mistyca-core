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

object CaseFactory {
  
  def train(sc: SparkContext, json_training: String, persistResult: Boolean) : String = {
    //TODO syn-sem model training
    var resume = ""
    //extract intents
    //parse context json
    val parser = new Json()
    val args = parser.parseJSONArray(json_training, "intents")
    //root node LV4
    val root_LV4 = (Stages.LV4, Array(Stages.LV4_VALUE))
    //parse context
    val context = args.get(0).get("context")
    //root node LV3
    val root_LV3 = (Stages.LV3_LOWER, Array(context))
    //link root nodes
    val root_edge = Edge(root_LV3._1, root_LV4._1, context)
    var LV1_count = 0
    var LV2_count = 0
    args.keySet().toArray().foreach{ case intent_index =>
      val intent = args.get(intent_index)
      //build graph
      var vertices = new ArrayBuffer[(VertexId,Array[String])]()
      var edges = new ArrayBuffer[Edge[String]]()
      vertices += root_LV4
      vertices += root_LV3
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
        //add pattern to vertex value
        LV1_vertex_value += pattern
      }
      //build LV1 vertex
      val LV1_vertex = (Stages.LV1_LOWER+LV1_count,LV1_vertex_value.toArray)
      //update counter LV1
      LV1_count = LV1_count + 1
      vertices += LV1_vertex
      //evaluate KC involved
      val keywords_classes = parser.parseJSONArray(intent.get("keywords_classes"))
      keywords_classes.foreach{ case kc_name =>
        //check if already involved in others intents
        val kc_stored = vertices.filter(v=> v._1 >= Stages.LV2_LOWER && v._1 <= Stages.LV2_UPPER && v._2(1).equals(kc_name))
        val keyword_class_instance = new KeywordClass(null,null,null)
        if(kc_stored.length > 0) {
          //already involved
          //attach edge
          edges += Edge(LV1_vertex._1, kc_stored(0)._1, intent.get("language"))
        }
        else {
          //evaluate KC type
          //is basic?
          if(keyword_class_instance.BASIC_CLASSES.contains(kc_name)) {
            //create basic KC vertex
            val LV2_vertex = (Stages.LV2_LOWER+LV2_count, Array(keyword_class_instance.BASIC_KEYWORD_MARKER,kc_name))
            // attach edge from LV1
            edges += Edge(LV1_vertex._1, LV2_vertex._1, intent.get("language"))
            //persist LV2 vertex
            vertices += LV2_vertex
            //add edge LV2-LV3
            edges += Edge(LV2_vertex._1, root_LV3._1, context + "." + intent.get("action"))
            
          }//TODO switch-case adding context based KC. CODE REPLICATION!!!!
          else {
            //custom KC, extract values
            val values_map = parser.parseJSONObject(parser.parseJSONObject(intent.get("custom_classes_definition")).get(kc_name))
            //map values into vertex
            val LV2_vertex = (Stages.LV2_LOWER+LV2_count, Array(keyword_class_instance.CUSTOM_KEYWORD_MARKER,kc_name) ++ values_map.keySet().toArray().map(k=> k + keyword_class_instance.CUSTOM_KEYWORD_DIVISOR + values_map.get(k)))
            // attach edge from LV1
            edges += Edge(LV1_vertex._1, LV2_vertex._1, intent.get("language"))
            //persist LV2 vertex
            vertices += LV2_vertex
            //add edge LV2-LV3
            edges += Edge(LV2_vertex._1, root_LV3._1, context + "." + intent.get("action"))
          }
          //update counter LV2
          LV2_count = LV2_count + 1
        }
      }
      //if cases already exists, replace them
      val cases_path = Contexts.CONTEXTS_BASE_PATH + context + "/cases/" + intent.get("language") + "/"
      if(persistResult) {
        //parallelize
        val graph_vertices: RDD[(VertexId, Array[String])] = sc.parallelize(vertices)
        val graph_edges: RDD[Edge[String]] = sc.parallelize(edges)     
        Datastore.createFolder(sc, cases_path)
        //store RDDs
    	  Datastore.saveAsObjectFile[(VertexId, Array[String])](graph_vertices, cases_path);
    	  Datastore.saveAsObjectFile[Edge[String]](graph_edges, cases_path);
      }
  	  //build resume
      resume =  resume + "\n" + cases_path + "\n" + "[New vertices: " + vertices.map(f=> (f._1,f._2.mkString("|"))).mkString(",") + "]"  + "\n" + "[New edges: " + edges.mkString(",") + "]"
    }
    
    return resume;
  }
  
  def export(path:String) : String = {
    return "TODO";
  }
  
  def buildGraph(contexts:Array[String], languages:Array[String]) : Graph[Array[String], String] = {
    return null;
  }
}