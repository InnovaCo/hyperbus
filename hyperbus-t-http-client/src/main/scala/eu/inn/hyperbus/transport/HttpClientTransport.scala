package eu.inn.hyperbus.transport

import com.sun.tools.javac.util.Log.PrefixKind
import com.typesafe.config.Config
import eu.inn.hyperbus.model._
import eu.inn.hyperbus.serialization.{MessageDeserializer, StringSerializer}
import eu.inn.hyperbus.transport.api.{TransportRequest, _}
import eu.inn.hyperbus.transport.httpclient.{ConfigLoader, HttpClientConfig, HttpClientRoute}
import org.asynchttpclient.{AsyncCompletionHandler, RequestBuilder, Response}
import org.slf4j.LoggerFactory

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future, Promise}

class HttpClientTransport(httpClientConfig: HttpClientConfig, routes: List[HttpClientRoute])
                         (implicit val executionContext: ExecutionContext) extends ClientTransport {

  def this(config: Config) = this(
    ConfigLoader.loadHttpClientConfig(config, "http"),
    ConfigLoader.loadRoutes(config, "routes")
  )(scala.concurrent.ExecutionContext.global) // todo: configurable ExecutionContext like in akka?

  protected[this] val log = LoggerFactory.getLogger(this.getClass)
  protected[this] val httpClient = httpClientConfig.createHttpClient()

  override def ask(request: TransportRequest, outputDeserializer: Deserializer[TransportResponse]): Future[TransportResponse] = {
    findRoute(request).map { route ⇒
      val requestBuilder = new RequestBuilder()
      requestBuilder.setMethod(request.method)
      requestBuilder.setUrl(appendUri(route.urlPrefix, extractUriAndQueryString(request))) // todo: implement rewrite
      setHeaders(route.additionalHeaders, requestBuilder)
      setHeaders(request.headers, requestBuilder) // todo: translate hyperbus-headers to http
      if (request.method != Method.GET && !request.body.isInstanceOf[EmptyBody]) {
        val bodyString = StringSerializer.serializeToString(request.body)
        requestBuilder.setBody(bodyString)
      }

      val promise = Promise[Response]()
      httpClient.executeRequest(requestBuilder.build(), new AsyncCompletionHandler[Response]{
        override def onCompleted(response: Response): Response = {
          if (response.getStatusCode < 400) {

          }

          promise.success(response)
          response
        }
      })

      promise.future
    } getOrElse {
      Future.failed(new NoTransportRouteException(s"HttpClientTransport. Uri: ${request.uri}"))
    }
  }

  override def publish(message: TransportRequest): Future[PublishResult] =
    ask(message, MessageDeserializer.deserializeResponseWith(_)(StandardResponse.apply)) map { response ⇒
    new PublishResult {
      override def sent: Option[Boolean] = Some(true)
      override def offset: Option[String] = None
    }
  }

  private def findRoute(request: TransportRequest): Option[HttpClientRoute] = {
    routes.find(_.requestMatcher.matchMessage(request))
  }

  override def shutdown(duration: FiniteDuration): Future[Boolean] = {
    Future {
      //producer.close()
      true
    } recover {
      case e: Throwable ⇒
        log.error("Can't close http-client transport", e)
        false
    }
  }

  private def extractUriAndQueryString(request: TransportRequest): String = {
    val queryString = request.body match {
      case queryBody: QueryBody ⇒
        queryBody.toQueryString()

      case dynamic: DynamicBody ⇒
        QueryBody(dynamic.content).toQueryString()

      case other ⇒
        ""
    }

    if (queryString.nonEmpty)
      request.uri.formatted + "?" + queryString
    else
      request.uri.formatted
  }

  private def appendUri(prefix: String, postfix: String): String = {
    if (prefix.endsWith("/") && postfix.startsWith("/")) {
      prefix + postfix.substring(1)
    }
    else {
      prefix + postfix
    }
  }

  private def setHeaders(headers: Map[String, Seq[String]], requestBuilder: RequestBuilder): Unit = {
    headers.foreach { case (header, values) ⇒
      values.foreach(requestBuilder.addHeader(header, _))
    }
  }
}
