import eu.inn.hyperbus.transport.httpclient.HeaderUtils
import org.scalatest.{FlatSpec, Matchers}

class HeaderUtilsTest extends FlatSpec with Matchers {
  "Http headers" should "have http prefix in hyperbus" in {
    val http = Map("Accept-Language" → Seq("da, en-gb;q=0.8, en;q=0.7"))
    val hb = HeaderUtils.httpToHyperbus(http)
    hb shouldBe Map("httpAcceptLanguage" → Seq("da, en-gb;q=0.8, en;q=0.7"))
  }

  "Hyperbus headers" should "have Hyperbus- prefix when translated to http" in {
    val hb = Map("messageId" → Seq("100500"))
    val http = HeaderUtils.hyperbusToHttp(hb)
    http shouldBe Map("Hyperbus-Message-Id" → Seq("100500"))
  }
}
