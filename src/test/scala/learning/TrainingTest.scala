package learning

import org.scalatest.FunSuite
import scala.io.Source

import bot.knowledge.adaptation.Actions
import bot.knowledge.cases.CaseFactory
import bot.reasoning.IntentRetrieval

class TrainingTest extends FunSuite {
  test("Train Actions Test") {
    val json_training = Source.fromURL(getClass.getResource("/Math_SKILL.json")).mkString
    println(Actions.train(null, json_training, false)._1)
  }
  
  test("Train Cases Test") {
    val json_training = Source.fromURL(getClass.getResource("/Math_SKILL.json")).mkString
    println(CaseFactory.train(null, json_training, false)._1)
  }
  
  test("Confict Test") {
    println(IntentRetrieval.resolveConflicts(Array(("NUMBER","3_conflict_4","3_conflict_4"),("NUMBER","3_conflict_4","3_conflict_4"),("OPERATOR","plus","+"))).mkString("|"))
  }
  
//  test("SyncAction Test") {
//    val message = ("",Array(null,null,null,null,))
//  }
}