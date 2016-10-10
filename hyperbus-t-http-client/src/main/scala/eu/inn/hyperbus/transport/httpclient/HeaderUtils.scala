package eu.inn.hyperbus.transport.httpclient

object HeaderUtils {
  val CONTENT_TYPE                = "Content-Type"
  val CERTAIN_CONTENT_TYPE_START  = "application/vnd."
  val CERTAIN_CONTENT_TYPE_END    = "+json"
  val COMMON_CONTENT_TYPE         = "application/json"
  val HYPERBUS_PREFIX = "Hyperbus-"
  val HTTP_PREFIX = "Http-"

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
      case (k, v)
    }
  }

  def hyperbusToHttp(hyperbusHeaders: Map[String, Seq[String]], exclude: Set[String] = Set.empty): Map[String, Seq[String]] = ???
}
