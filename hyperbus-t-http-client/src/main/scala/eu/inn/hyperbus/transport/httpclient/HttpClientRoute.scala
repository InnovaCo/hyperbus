package eu.inn.hyperbus.transport.httpclient

import eu.inn.hyperbus.transport.api.matchers.RequestMatcher

case class HttpClientRoute(requestMatcher: RequestMatcher,
                           urlPrefix: String,
                           additionalHeaders: Map[String,Seq[String]])
