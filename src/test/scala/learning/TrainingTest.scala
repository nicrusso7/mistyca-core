package learning

import org.scalatest.FunSuite
import scala.io.Source

import bot.knowledge.adaptation.Actions
import bot.knowledge.cases.CaseFactory

class TrainingTest extends FunSuite {
  test("Train Actions Test") {
    val json_training = Source.fromURL(getClass.getResource("/Math_SKILL.json")).mkString
    println(Actions.train(null, json_training, false))
  }
  
  test("Train Actions Test") {
    val json_training = Source.fromURL(getClass.getResource("/Math_SKILL.json")).mkString
    println(CaseFactory.train(null, json_training, false))
  }
}