package reasoning

import org.scalatest.FunSuite
import org.scalatest.BeforeAndAfter

import bot.reasoning.IntentRetrieval
import bot.knowledge.vocabulary.Stages

import scala.collection.mutable.ArrayBuffer

class IntentRetrievalTests extends FunSuite with BeforeAndAfter {
  
  var value_in:ArrayBuffer[String] = _

  before {
    value_in = new ArrayBuffer[String]()
  }
  
  test("Verb Match/Pattern Match function Test") {
    //fill value
    val value = Array("switch,on","turn,up","turn,on")
    
    val message:(String, Array[(String, String, (String, String), (String, String), Set[(String, String, String)])]) = ("turn the lights on", null)
    val analysis = IntentRetrieval.verbMatch(value, message)
    //positive test
    assert(analysis(2).toDouble == 1.0)
    //check extraction
    assert(analysis(3) == value(2))
    
    val message_false:(String, Array[(String, String, (String, String), (String, String), Set[(String, String, String)])]) = ("turn the lights", null)
    val analysis_false = IntentRetrieval.verbMatch(value, message_false)
    //negative test
    assert(analysis_false(2).toDouble == 0.0)
    //check extraction
    assert(analysis_false(3) == Stages.MISSING_MARKER)
  }
  
  test("Pattern Percentage Match function Test") {
    //fill value
    val value = Array("on,the,in", "up,the,in")
    
    val message:(String, Array[(String, String, (String, String), (String, String), Set[(String, String, String)])]) = ("turn on the lights in the living", Array((null, null, ("1.0","turn,on"), null, null)))
    val analysis = IntentRetrieval.PatternPercentageMatch(value, message)
    //positive test
    assert(analysis(5).toDouble == 1.0)
    //check extraction
    assert(analysis(6) == value(0))
    
    val message_1:(String, Array[(String, String, (String, String), (String, String), Set[(String, String, String)])]) = ("turn on the lights living", Array((null, null, ("1.0","turn,on"), null, null)))
    val analysis_1 = IntentRetrieval.PatternPercentageMatch(value, message_1)
    //66% test
    assert(analysis_1(5).toDouble < 0.67)
    //check extraction
    assert(analysis_1(6) == value(0))
    
    val message_2:(String, Array[(String, String, (String, String), (String, String), Set[(String, String, String)])]) = ("turn on lights living", Array((null, null, ("1.0","turn,on"), null, null)))
    val analysis_2 = IntentRetrieval.PatternPercentageMatch(value, message_2)
    //33% test
    assert(analysis_2(5).toDouble < 0.34)
    //check extraction
    assert(analysis_2(6) == value(0))
    
    val message_3:(String, Array[(String, String, (String, String), (String, String), Set[(String, String, String)])]) = ("turn lights living", Array((null, null, ("0.0",Stages.MISSING_MARKER), null, null)))
    val analysis_3 = IntentRetrieval.PatternPercentageMatch(value, message_3)
    //33% test
    assert(analysis_3(5).toDouble == 0.0)
    //check extraction
    assert(analysis_3(6) == Stages.MISSING_MARKER)
  }
}