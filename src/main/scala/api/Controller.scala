package api

import org.apache.spark.graphx.Graph
import bot.knowledge.cases.CaseFactory

object Controller {
  
  def initKnowledge(languages: Array[String], contexts: Array[String]) : Graph[Array[String], String] = {
    CaseFactory.buildGraph(contexts, languages)
  }
  
  //TODO initCache (broadcast vars)
  
}