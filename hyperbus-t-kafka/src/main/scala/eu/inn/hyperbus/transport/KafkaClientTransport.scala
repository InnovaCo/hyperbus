package eu.inn.hyperbus.transport

import java.io.ByteArrayOutputStream
import java.util.Properties

import com.typesafe.config.Config
import eu.inn.hyperbus.transport.api._
import eu.inn.hyperbus.transport.api.uri.Uri
import eu.inn.hyperbus.transport.kafkatransport.ConfigLoader
import eu.inn.hyperbus.util.ConfigUtils._
import eu.inn.hyperbus.util.StringSerializer
import org.apache.kafka.clients.producer.{Callback, KafkaProducer, ProducerRecord, RecordMetadata}
import org.slf4j.LoggerFactory

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future, Promise}

case class KafkaRoute(uri: Uri,
                      kafkaTopic: String,
                      kafkaPartitionKeys: List[String])

class KafkaPartitionKeyIsNotDefined(message: String) extends RuntimeException(message)

class KafkaClientTransport(producerProperties: Properties,
                           routes: List[KafkaRoute],
                           logMessages: Boolean = false,
                           encoding: String = "UTF-8")
                          (implicit val executionContext: ExecutionContext) extends ClientTransport {

  def this(config: Config) = this(
    producerProperties = ConfigLoader.loadProducerProperties(config.getConfig("producer")),
    routes = ConfigLoader.loadRoutes(config.getConfigList("routes")),
    logMessages = config.getOptionBoolean("log-messages") getOrElse false,
    encoding = config.getOptionString("encoding").getOrElse("UTF-8")
  )(scala.concurrent.ExecutionContext.global) // todo: configurable ExecutionContext like in akka?

  protected[this] val log = LoggerFactory.getLogger(this.getClass)
  protected[this] val producer = new KafkaProducer[String, String](producerProperties)

  override def ask[OUT <: TransportResponse](message: TransportRequest, outputDeserializer: Deserializer[OUT]): Future[OUT] = ???

  override def publish(message: TransportRequest): Future[PublishResult] = {
    routes.find(r ⇒ r.uri.matchUri(message.uri)) map (publishToRoute(_, message)) getOrElse {
      throw new NoTransportRouteException(s"Kafka producer (client). Uri: ${message.uri}")
    }
  }

  override def shutdown(duration: FiniteDuration): Future[Boolean] = {
    Future {
      producer.close()
      true
    } recover {
      case e: Throwable ⇒
        log.error("Can't close kafka producer", e)
        false
    }
  }

  private def publishToRoute(route: KafkaRoute, message: TransportRequest): Future[PublishResult] = {
    val messageString = StringSerializer.serializeToString(message)

    val record: ProducerRecord[String, String] =
      if (route.kafkaPartitionKeys.isEmpty) {
        // no partition key
        new ProducerRecord(route.kafkaTopic, messageString)
      }
      else {
        val recordKey = route.kafkaPartitionKeys.map { key: String ⇒ // todo: check partition key logic
          message.uri.args.getOrElse(key,
            throw new KafkaPartitionKeyIsNotDefined(s"Argument $key is not defined for ${message.uri}")
          ).specific
        }.foldLeft("")(_ + "," + _.replace("\\","\\\\").replace(",", "\\,"))

        new ProducerRecord(route.kafkaTopic, recordKey.substring(1), messageString)
      }

    if (logMessages && log.isTraceEnabled) {
      log.trace(s"Sending to kafka. ${route.kafkaTopic} ${if (record.key() != null) "/" + record.key} : #${message.hashCode()} $messageString")
    }

    val promise = Promise[PublishResult]()

    producer.send(record, new Callback {
      def onCompletion(recordMetadata: RecordMetadata, e: Exception): Unit = {
        if (e != null) {
          promise.failure(e)
          log.error(s"Can't send to kafka. ${route.kafkaTopic} ${if (record.key() != null) "/" + record.key} : $message", e)
        }
        else {
          promise.success(
            new PublishResult {
              def sent = Some(true)

              def offset = Some(s"${recordMetadata.partition()}/${recordMetadata.offset()}}")

              override def toString = s"PublishResult(sent=$sent,offset=$offset)"
            }
          )
          if (logMessages && log.isTraceEnabled) {
            log.trace(s"Sent to kafka. ${route.kafkaTopic} ${if (record.key() != null) "/" + record.key} : #${message.hashCode().toHexString}." +
              s"Offset: ${recordMetadata.offset()} partition: ${recordMetadata.partition()}")
          }
        }
      }
    })
    promise.future
  }
}
