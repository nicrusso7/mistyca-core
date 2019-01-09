package api

import org.apache.spark.graphx.Graph
import bot.knowledge.cases.CaseFactory
import bot.Mistyca
import api.persistence.Datastore
import scala.collection.mutable.ArrayBuffer

import org.apache.spark.SparkContext

object Controller {
  
  def initKnowledge(sc: SparkContext, languages: Array[String], contexts: Array[String]) : Graph[Array[String], String] = {
    CaseFactory.buildGraph(sc, contexts, languages)
  }
  
  def trainModel(sc: SparkContext) : Array[(String,String,String)] = {
    //train with all the .json files in /training
    //TODO loop over all files
    var resumes = new ArrayBuffer[(String,String,String)]()
    val json_training = sc.textFile("/training/Math_SKILL.json").toLocalIterator.mkString
    resumes += Mistyca.learn(sc, json_training)
    resumes.toArray
  }
  
  def ask(sentence: String, knowledge: Graph[Array[String], String], sim_measures_model:String) : String = {
    Mistyca.ask(sentence, knowledge, sim_measures_model)
  }
  
  //TODO askAsync, trainAsync
  //TODO initCache (broadcast vars)
  
}