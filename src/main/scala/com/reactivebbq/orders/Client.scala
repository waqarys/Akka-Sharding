package com.reactivebbq.orders

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, MessageEntity}
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Random, Success}

object Client extends App with OrderJsonFormats {
  val usage =
    """
      | USAGE:
      |     sbt 'runMain com.reactivebbq.orders.Client open <serverName> <tableNumber>'
      |     sbt 'runMain com.reactivebbq.orders.Client add <orderId> <itemName>' <specialInstructions>'
      |     sbt 'runMain com.reactivebbq.orders.Client find <orderId>'
    """.stripMargin

  require(args.length > 0, usage)

  val config = ConfigFactory.load("client.conf")

  implicit val system: ActorSystem = ActorSystem("client", config)
  implicit val ec: ExecutionContext = system.dispatcher

  val ports = Random.shuffle(Seq(8000, 8001, 8002)).iterator

  def port = {
    if(ports.hasNext)
      ports.next()
    else
      throw new RuntimeException("Tried all available ports without success.")
  }

  val result = args(0) match {
    case "open" =>
      runIf(args.length == 3) {
        openOrder(args(1), args(2).toInt)
      }
    case "add" =>
      runIf(args.length == 4) {
        addItem(args(1), args(2), args(3))
      }
    case "find" =>
      runIf(args.length == 2) {
        findOrder(args(1))
      }
  }

  result.andThen {
    case Success(order) =>
      system.log.info(order.toString)
      system.terminate()
    case Failure(ex) =>
      system.log.error(ex, "Failure")
      system.terminate()
  }

  private def runIf(requirement: Boolean)(run: => Future[String]): Future[String] = {
    if(!requirement) {
      Future.failed(new IllegalArgumentException("Invalid Command: \n" + usage))
    } else {
      run
    }
  }

  private def openOrder(server: String, table: Int): Future[String] = {
    val command = OrderActor.OpenOrder(Server(server), Table(table))
    val url = s"http://localhost:$port/order"

    (for {
      requestEntity <- Marshal(command).to[MessageEntity]
      request = HttpRequest(HttpMethods.POST, url, entity = requestEntity)
      response <- Http().singleRequest(request)
      responseEntity <- response.entity.toStrict(5.seconds)
    } yield {
      new String(responseEntity.data.toArray)
    }).recoverWith {
      case ex =>
        system.log.warning(s"Attempt to connect to $url failed. Retrying.")
        openOrder(server, table)
    }
  }

  private def addItem(orderId: String, itemName: String, specialInstructions: String): Future[String] = {
    val command = OrderActor.AddItemToOrder(OrderItem(itemName, specialInstructions))
    val url = s"http://localhost:$port/order/$orderId/items"

    (for {
      requestEntity <- Marshal(command).to[MessageEntity]
      request = HttpRequest(HttpMethods.POST, url, entity = requestEntity)
      response <- Http().singleRequest(request)
      responseEntity <- response.entity.toStrict(5.seconds)
    } yield {
      new String(responseEntity.data.toArray)
    }).recoverWith {
      case ex =>
        system.log.warning(s"Attempt to connect to $url failed. Retrying.")
        addItem(orderId, itemName, specialInstructions)
    }
  }

  private def findOrder(orderId: String): Future[String] = {
    val url = s"http://localhost:$port/order/$orderId"

    (for {
      response <- Http().singleRequest(HttpRequest(HttpMethods.GET, url))
      responseEntity <- response.entity.toStrict(5.seconds)
    } yield {
      new String(responseEntity.data.toArray)
    }).recoverWith {
      case ex =>
        system.log.warning(s"Attempt to connect to $url failed. Retrying.")
        findOrder(orderId)
    }
  }

}
