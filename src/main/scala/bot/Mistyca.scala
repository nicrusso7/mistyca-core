package bot

import bot.knowledge.vocabulary.Contexts
import bot.knowledge.vocabulary.Stages
import bot.knowledge.adaptation.Actions
import bot.knowledge.cases.CaseFactory

import bot.reasoning.Reasoner

import org.apache.spark.SparkContext
import org.apache.spark.graphx.Graph

object Mistyca {
  
  def learn(sc: SparkContext, json_training: String) : (String,String,String) = {
    //TODO validate input training
    //learn Context
    val ctx_resume = Contexts.train(sc, json_training)
    //learn Actions
    val actions_resume = Actions.train(sc, json_training, true)
    //learn intents (cases)
    val intents_resume = CaseFactory.train(sc, json_training, true)
    (ctx_resume,actions_resume,intents_resume)
  }
  
  def ask(sentence: String, knowledge: Graph[Array[String], String], sim_measures_model:String) : String = {
    val reasoner = new Reasoner()
    //run analysis
    val analysis_graph = reasoner.evaluateCases(knowledge, sentence, sim_measures_model)
    //extract response
    analysis_graph.vertices.filter(v => v._2(0).equals(Stages.ANALYSIS_END_MARKER)).collect()(0)._2(1)
  }
}