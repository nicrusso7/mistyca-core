package bot.knowledge.adaptation

import org.apache.spark.graphx._
import org.apache.spark.rdd.RDD

object Actions {
  
  def train(json_training:String) : String = {
    return "TODO";
  }
  
  def export(path:String) : String = {
    return "TODO";
  }
  
  def buildGraph(vertices:RDD[(VertexId, (String, String))], edges:RDD[Edge[String]]) : Graph[Array[String], String] = {
    return null;
  }
}