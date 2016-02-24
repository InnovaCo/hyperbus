import java.io.{InputStream, OutputStream}
import java.util.concurrent.atomic.AtomicInteger

import akka.actor.{Actor, ActorLogging}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import akka.event.LoggingReceive
import akka.testkit.TestActorRef
import com.typesafe.config.ConfigFactory
import eu.inn.binders._
import eu.inn.binders.json._
import eu.inn.hyperbus.IdGenerator
import eu.inn.hyperbus.transport._
import eu.inn.hyperbus.transport.api._
import eu.inn.hyperbus.transport.api.matchers.TransportRequestMatcher
import eu.inn.hyperbus.transport.api.uri.Uri
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{BeforeAndAfter, FreeSpec, Matchers}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future, Promise}


// move mocks to separate assembly
case class MockRequest(uriPattern: String, message: String,
                       headers: Map[String, Seq[String]] = Map.empty,
                       correlationId: String = IdGenerator.create(),
                       messageId: String = IdGenerator.create()) extends TransportRequest {
  def uri: Uri = Uri(uriPattern)

  override def serialize(output: OutputStream): Unit = {
    SerializerFactory.findFactory().withStreamGenerator(output)(_.bind(this))
  }
}

case class MockResponse(message: String,
                        headers: Map[String, Seq[String]] = Map.empty,
                        correlationId: String = IdGenerator.create(),
                        messageId: String = IdGenerator.create()) extends TransportResponse {
  override def serialize(output: OutputStream): Unit = {
    SerializerFactory.findFactory().withStreamGenerator(output)(_.bind(this))
  }
}

object MockRequestDeserializer extends Deserializer[MockRequest] {
  override def apply(input: InputStream): MockRequest = {
    SerializerFactory.findFactory().withStreamParser(input)(_.unbind[MockRequest])
  }
}

object MockResponseDeserializer extends Deserializer[MockResponse] {
  override def apply(input: InputStream): MockResponse = {
    SerializerFactory.findFactory().withStreamParser(input)(_.unbind[MockResponse])
  }
}

class TestActorX extends Actor with ActorLogging {
  val membersUp = new AtomicInteger(0)
  val memberUpPromise = Promise[Unit]()
  val memberUpFuture: Future[Unit] = memberUpPromise.future

  override def receive: Receive = LoggingReceive {
    case MemberUp(member) => {
      membersUp.incrementAndGet()
      memberUpPromise.success({})
      log.info("Member is ready!")
    }
  }
}

class DistribAkkaTransportTest extends FreeSpec with ScalaFutures with Matchers with BeforeAndAfter {
  //implicit var actorSystem: ActorSystem = null

  var transportManager: TransportManager = null
  before {
    val transportConfiguration = TransportConfigurationLoader.fromConfig(ConfigFactory.load())
    transportManager = new TransportManager(transportConfiguration)
    ActorSystemRegistry.get("eu-inn").foreach { implicit actorSystem ⇒
      val testActor = TestActorRef[TestActorX]
      Cluster(actorSystem).subscribe(testActor, initialStateMode = InitialStateAsEvents, classOf[MemberEvent])
    }
  }

  after {
    if (transportManager != null) {
      Await.result(transportManager.shutdown(20.seconds), 20.seconds)
    }
  }

  "DistributedAkkaTransport " - {
    "Send and Receive" in {
      import ExecutionContext.Implicits.global
      val cnt = new AtomicInteger(0)

      val id = transportManager.onCommand(TransportRequestMatcher(Some(Uri("/topic/{abc}"))), MockRequestDeserializer, null) { msg: MockRequest =>
        Future {
          cnt.incrementAndGet()
          MockResponse(msg.message.reverse)
        }
      }

      val id2 = transportManager.onCommand(TransportRequestMatcher(Some(Uri("/topic/{abc}"))), MockRequestDeserializer, null) { msg: MockRequest =>
        Future {
          cnt.incrementAndGet()
          MockResponse(msg.message.reverse)
        }
      }

      val id3 = transportManager.onEvent(TransportRequestMatcher(Some(Uri("/topic/{abc}"))), "sub1", MockRequestDeserializer) { msg: MockRequest =>
        msg.message should equal("12345")
        cnt.incrementAndGet()
        Future.successful({})
      }

      val id4 = transportManager.onEvent(TransportRequestMatcher(Some(Uri("/topic/{abc}"))), "sub1", MockRequestDeserializer) { msg: MockRequest =>
        msg.message should equal("12345")
        cnt.incrementAndGet()
        Future.successful({})
      }

      val id5 = transportManager.onEvent(TransportRequestMatcher(Some(Uri("/topic/{abc}"))), "sub2", MockRequestDeserializer) { msg: MockRequest =>
        msg.message should equal("12345")
        cnt.incrementAndGet()
        Future.successful({})
      }

      Thread.sleep(500) // we need to wait until subscriptions will go acros the

      val f: Future[MockResponse] = transportManager.ask(MockRequest("/topic/{abc}", "12345"), MockResponseDeserializer)

      whenReady(f, timeout(Span(5, Seconds))) { msg =>
        msg shouldBe a[MockResponse]
        msg.message should equal("54321")
        Thread.sleep(500) // give chance to increment to another service (in case of wrong implementation)
        cnt.get should equal(3)

        /*
        todo: this doesn't work for some reason, maybe it's bug in DistributedPubSub
        todo: find a way to know if the subscription is complete? future?

        transportManager.off(id)
        transportManager.off(id2)
        transportManager.off(id3)
        transportManager.off(id4)
        transportManager.off(id5)


        Thread.sleep(1000)

        val f2: Future[MockResponse] = transportManager.ask(MockRequest("/topic/{abc}", "12345"), MockResponseDeserializer)

        whenReady(f2.failed, timeout(Span(10, Seconds))) { e =>
          e.printStackTrace()
          e shouldBe a[NoTransportRouteException]
        }*/

        val f3: Future[MockResponse] = transportManager.ask(MockRequest("not-existing-topic", "12345"), MockResponseDeserializer)

        whenReady(f3.failed, timeout(Span(1, Seconds))) { e =>
          e shouldBe a[NoTransportRouteException]
        }
      }
    }

    /*"Dispatcher test" in {
      val cnt = new AtomicInteger(0)

      val id = serviceBus.process[String, String](Topic("/topic/{abc}", PartitionArgs(Map.empty)),
        mockDecoder, mockExtractor[String], null) { s =>
        cnt.incrementAndGet()
        Thread.sleep(15000)
        mockResult(s.reverse)
      }

      val futures = 0 to 300 map { _ ⇒
        serviceBus.ask[String, String](Topic("/topic/{abc}", PartitionArgs(Map.empty)),
          "12345",
          mockEncoder, mockDecoder)
      }
      import scala.concurrent.ExecutionContext.Implicits.global
      val f = Future.sequence(futures)

      val result = Await.ready(f, 120.seconds)
      println(result)
      /*whenReady(f) { s =>
        s should equal("54321")
        Thread.sleep(500) // give chance to increment to another service (in case of wrong implementation)
        cnt.get should equal(100)
      }*/
    }*/
  }
}
