package eu.inn.hyperbus.transport.httpclient

import com.typesafe.config.Config

object ConfigLoader {
  import eu.inn.binders.tconfig._

  def loadHttpClientConfig(config: Config, path: String): HttpClientConfig = {
    config.read[HttpClientConfig](path)
  }

  def loadRoutes(config: Config, path: String): List[HttpClientRoute] = {
    config.read[List[HttpClientRoute]](path)
  }
}
