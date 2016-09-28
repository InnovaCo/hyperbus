package eu.inn.hyperbus.transport

import com.typesafe.config.Config
import eu.inn.hyperbus.transport.api._
import org.slf4j.LoggerFactory

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

class HttpClientTransport()
                         (implicit val executionContext: ExecutionContext) extends ClientTransport {

  def this(config: Config) = this()(scala.concurrent.ExecutionContext.global) // todo: configurable ExecutionContext like in akka?

  protected[this] val log = LoggerFactory.getLogger(this.getClass)

  override def ask(message: TransportRequest, outputDeserializer: Deserializer[TransportResponse]): Future[TransportResponse] = ???

  override def publish(message: TransportRequest): Future[PublishResult] = ???

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
}
