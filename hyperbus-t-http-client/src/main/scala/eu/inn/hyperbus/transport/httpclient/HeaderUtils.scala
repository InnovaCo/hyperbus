package eu.inn.hyperbus.transport.httpclient

import eu.inn.binders.naming.{CamelCaseToHyphenCaseConverter, HyphenCaseToCamelCaseConverter}

object HeaderUtils {
  private val httpToHyperbusConverter = new HyphenCaseToCamelCaseConverter
  private val hyperbusToHttpConverter = new CamelCaseToHyphenCaseConverter

  private val CONTENT_TYPE                = "Content-Type"
  private val CONTENT_TYPE_LC             = CONTENT_TYPE.toLowerCase
  private val CONTENT_TYPE_HB             = httpToHyperbusConverter.convert(CONTENT_TYPE)
  private val CERTAIN_CONTENT_TYPE_START  = "application/vnd."
  private val CERTAIN_CONTENT_TYPE_END    = "+json"
  private val COMMON_CONTENT_TYPE         = "application/json"
  private val HYPERBUS_PREFIX             = "Hyperbus-"
  private val HYPERBUS_PREFIX_LC          = HYPERBUS_PREFIX.toLowerCase
  private val HTTP_PREFIX                 = "Http-"
  private val HTTP_PREFIX_LC              = HTTP_PREFIX.toLowerCase
  private val HTTP_PREFIX_HB              = "http"


  def httpContentTypeToGeneric(httpContentType: Option[String]): Option[String] = {
    httpContentType.map(_.toLowerCase) match {
      case Some(v) if (v.startsWith(CERTAIN_CONTENT_TYPE_START)
        && v.endsWith(CERTAIN_CONTENT_TYPE_END)) ⇒
        val beginIndex = CERTAIN_CONTENT_TYPE_START.length
        val endIndex = v.length - CERTAIN_CONTENT_TYPE_END.length
        val r = v.substring(beginIndex, endIndex)
        if (r.isEmpty)
          None
        else
          Some(r)

      case _ ⇒ None // application/json is also empty contentType for hyperbus
    }
  }

  def genericContentTypeToHttp(contentType: Option[String]): Option[String] = {
    contentType.map{ value ⇒
      CERTAIN_CONTENT_TYPE_START + value + CERTAIN_CONTENT_TYPE_END
      // case None ⇒ Some("application/json") // todo: do we need this?
    }
  }

  def httpToHyperbus(httpHeaders: Map[String, Seq[String]], exclude: Set[String] = Set.empty): Map[String, Seq[String]] = {
    httpHeaders.flatMap {
      case (k, _) if exclude.contains(k) ⇒ None
      case (k, v) if k.toLowerCase == CONTENT_TYPE_LC ⇒ Some( httpToHyperbusConverter.convert(k) →
        v.flatMap(s ⇒ httpContentTypeToGeneric(Some(s)))
      )
      case (k, v) if k.toLowerCase.startsWith(HYPERBUS_PREFIX_LC)
        && k.length > HYPERBUS_PREFIX_LC.length ⇒ Some(
        httpToHyperbusConverter.convert(k.substring(HYPERBUS_PREFIX_LC.length)) → v
      )
      case (k, v) ⇒ Some(httpToHyperbusConverter.convert(HTTP_PREFIX + k) → v)
    }
  }

  def hyperbusToHttp(hyperbusHeaders: Map[String, Seq[String]], exclude: Set[String] = Set.empty): Map[String, Seq[String]] = {
    hyperbusHeaders.flatMap {
      case (k, _) if exclude.contains(k) ⇒ None
      case (CONTENT_TYPE_HB, v) ⇒ Some( CONTENT_TYPE →
        v.flatMap(s ⇒ genericContentTypeToHttp(Some(s)))
      )
      case (k, v) if k.startsWith(HTTP_PREFIX_HB)
        && k.length > HTTP_PREFIX_HB.length ⇒ Some(
        hyperbusToHttpConverter.convert(k.substring(HTTP_PREFIX_HB.length)) → v
      )
      case (k, v) ⇒ Some(HYPERBUS_PREFIX + hyperbusToHttpConverter.convert(k) → v)
    }
  }
}
