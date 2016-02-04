package eu.inn.hyperbus.transport.api

import java.io.{ByteArrayOutputStream, OutputStream}

import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream
import com.typesafe.config.ConfigValue
import eu.inn.hyperbus.transport.api.uri.Uri

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.util.matching.Regex

trait TransportMessage {
  def messageId: String

  def correlationId: String

  def serialize(output: OutputStream)

  def serializeToString(encoding: String = "UTF-8"): String = {
    val outputStream = new ByteArrayOutputStream()
    serialize(outputStream)
    outputStream.toString(encoding)
  }
}

trait TransportRequest extends TransportMessage {
  def uri: Uri
}

trait TransportResponse extends TransportMessage

trait PublishResult {
  def sent: Option[Boolean]

  def offset: Option[String]
}

trait ClientTransport {
  def ask[OUT <: TransportResponse](message: TransportRequest, outputDeserializer: Deserializer[OUT]): Future[OUT]

  def publish(message: TransportRequest): Future[PublishResult]

  def shutdown(duration: FiniteDuration): Future[Boolean]
}

trait ServerTransport {
  def process[IN <: TransportRequest](uriFilter: Uri, inputDeserializer: Deserializer[IN], exceptionSerializer: Serializer[Throwable])
                                     (handler: (IN) => Future[TransportResponse]): String

  def subscribe[IN <: TransportRequest](uriFilter: Uri, groupName: String, inputDeserializer: Deserializer[IN])
                                       (handler: (IN) => Future[Unit]): String // todo: Unit -> some useful response?

  def off(subscriptionId: String)

  def shutdown(duration: FiniteDuration): Future[Boolean]
}

class NoTransportRouteException(message: String) extends RuntimeException(message)

