import eu.inn.hyperbus.transport.httpclient.HeaderUtils
import org.scalatest.{FlatSpec, Matchers}

class HeaderUtilsTest extends FlatSpec with Matchers {
  "Http headers" should "have http prefix in hyperbus" in {
    val http = Map("Accept-Language" → Seq("da, en-gb;q=0.8, en;q=0.7"))
    val hb = HeaderUtils.httpToHyperbus(http)
    hb shouldBe Map("httpAcceptLanguage" → Seq("da, en-gb;q=0.8, en;q=0.7"))
  }

  "Http headers with Hyperbus- prefix" should "transform" in {
    val http = Map("Hyperbus-Message-Id" → Seq("100500"))
    val hb = HeaderUtils.httpToHyperbus(http)
    hb shouldBe Map("messageId" → Seq("100500"))
  }

  "Http Content-Type header should" should "transform" in {
    val http = Map("Content-Type" → Seq("application/vnd.some-type+json"))
    val hb = HeaderUtils.httpToHyperbus(http)
    hb shouldBe Map("contentType" → Seq("some-type"))
  }

  "Hyperbus headers" should "have Hyperbus- prefix when transformed to http" in {
    val hb = Map("messageId" → Seq("100500"))
    val http = HeaderUtils.hyperbusToHttp(hb)
    http shouldBe Map("Hyperbus-Message-Id" → Seq("100500"))
  }

  "Hyperbus http headers" should "transform" in {
    val hb = Map("httpLocation" → Seq("/abc"))
    val http = HeaderUtils.hyperbusToHttp(hb)
    http shouldBe Map("Location" → Seq("/abc"))
  }

  "Hyperbus contentType header should" should "transform" in {
    val hb = Map("contentType" → Seq("some-type"))
    val http = HeaderUtils.hyperbusToHttp(hb)
    http shouldBe Map("Content-Type" → Seq("application/vnd.some-type+json"))
  }
}
