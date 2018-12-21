package api

import org.apache.spark.graphx.Graph
import bot.knowledge.cases.CaseFactory
import bot.Mistyca

import org.apache.spark.SparkContext

object Controller {
  
  def initKnowledge(sc: SparkContext, languages: Array[String], contexts: Array[String]) : Graph[Array[String], String] = {
    CaseFactory.buildGraph(sc, contexts, languages)
  }
  
  def learn(sc: SparkContext, json_training: String) : (String,String,String) = {
    Mistyca.learn(sc, json_training)
  }
  
  def ask(sentence: String, knowledge: Graph[Array[String], String], sim_measures_model:String) : String = {
    Mistyca.ask(sentence, knowledge, sim_measures_model)
  }
  
  //TODO askAsync, trainAsync
  //TODO initCache (broadcast vars)
  
}