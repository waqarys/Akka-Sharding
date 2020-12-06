package com.reactivebbq.orders

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorLogging, ActorSystem, Props, Status}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.pattern.pipe
import akka.stream.Materializer
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._
import scala.util.Random

object Simulation {
  case object Start
  case object Stop
  def props(targetPorts: Seq[Int])(implicit materializer: Materializer, ec: ExecutionContext): Props = Props(new Simulation(targetPorts))
}

class Simulation(targetPorts: Seq[Int])(implicit materializer: Materializer, ec: ExecutionContext)
  extends Actor
    with ActorLogging
    with OrderJsonFormats {
  import Simulation._

  require(targetPorts.nonEmpty)

  private def port: Int = Random.shuffle(targetPorts).head
  private val http = Http(context.system)

  context.self ! Start

  private var startTime: Long = System.currentTimeMillis()

  override def receive: Receive = {
    case Start =>
      startTime = System.currentTimeMillis()
      run().pipeTo(self)
    case Stop =>
      context.stop(self)
    case order: Order =>
      log.info(s"Order completed in ${System.currentTimeMillis() - startTime} ms")
      context.self ! Start
    case Status.Failure(ex) =>
      log.info(s"Order Failed: ${ex.getMessage}")
  }

  private def run(): Future[Order] = {
    for {
      orderId <- openOrder().map(_.id)
      _ <- retrieveOrder(orderId)
      _ <- addItem(orderId, "Steak")
      _ <- retrieveOrder(orderId)
      _ <- addItem(orderId, "Salad")
      _ <- retrieveOrder(orderId)
      _ <- addItem(orderId, "Milk")
      _ <- retrieveOrder(orderId)
      _ <- addItem(orderId, "Cheesecake")
      order <- retrieveOrder(orderId)
    } yield {
      order
    }
  }

  private def openOrder(): Future[Order] = {
    for {
      requestEntity <- Marshal(OrderActor.OpenOrder(Server("Server"), Table(5))).to[MessageEntity]
      request = HttpRequest(HttpMethods.POST, s"http://localhost:$port/order", entity = requestEntity)
      response <- http.singleRequest(request)
      responseEntity <- response.entity.toStrict(5.seconds)
      order <- Unmarshal(responseEntity).to[Order]
    } yield {
      order
    }
  }

  private def retrieveOrder(id: OrderId): Future[Order] = {
    val request = HttpRequest(HttpMethods.GET, s"http://localhost:$port/order/${id.value.toString}")

    for {
      response <- http.singleRequest(request)
      responseEntity <- response.entity.toStrict(5.seconds)
      order <- Unmarshal(responseEntity).to[Order]
    } yield {
      order
    }
  }

  private def addItem(orderId: OrderId, itemName: String): Future[Order] = {
    for {
      requestEntity <- Marshal(OrderActor.AddItemToOrder(OrderItem(itemName, "None"))).to[MessageEntity]
      request = HttpRequest(HttpMethods.POST, s"http://localhost:$port/order/${orderId.value.toString}/items", entity = requestEntity)
      response <- http.singleRequest(request)
      responseEntity <- response.entity.toStrict(5.seconds)
      order <- Unmarshal(responseEntity).to[Order]
    } yield {
      order
    }
  }
}

object LoadTest extends App {
  val log = LoggerFactory.getLogger(this.getClass)

  val Opt = """-D(\S+)=(\S+)""".r
  args.toList.foreach {
    case Opt(key, value) =>
      log.info(s"Config Override: $key = $value")
      System.setProperty(key, value)
  }

  val config = ConfigFactory.load("loadtest.conf")

  val ports = config.getIntList("reactive-bbq.orders.ports")
    .asScala
    .map(_.intValue())
    .toList

  val testDuration = config.getDuration("load-test.duration", TimeUnit.MILLISECONDS).millis
  val parallelism = config.getInt("load-test.parallelism")
  val rampUpTime = config.getDuration("load-test.ramp-up-time", TimeUnit.MILLISECONDS).millis

  implicit val system: ActorSystem = ActorSystem("LoadTest", config)
  implicit val executionContext: ExecutionContext = system.dispatcher

  system.log.info(s"Creating $parallelism simulations")
  (1 to parallelism).map { _ =>
    Thread.sleep((rampUpTime/parallelism).toMillis)
    val sim = system.actorOf(Simulation.props(ports))
    system.scheduler.scheduleOnce(testDuration, sim, Simulation.Stop)
  }

  system.scheduler.scheduleOnce(testDuration + 15.seconds) {
    system.terminate()
  }
}
