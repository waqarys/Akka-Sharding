package com.reactivebbq.orders

import akka.actor.Status
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{ContentTypes, MessageEntity, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.TestProbe
import com.reactivebbq.orders.OrderActor.OrderNotFoundException
import org.scalatest.WordSpec
import org.scalatest.concurrent.ScalaFutures

class OrderRoutesTest
  extends WordSpec
    with ScalatestRouteTest
    with OrderHelpers
    with OrderJsonFormats
    with ScalaFutures {

  val orders = TestProbe()
  val http = new OrderRoutes(orders.ref)

  "POST to /order" should {
    "create a new order" in {
      val order = generateOrder()
      val request = OrderActor.OpenOrder(order.server, order.table)

      val result = Post("/order")
        .withEntity(Marshal(request).to[MessageEntity].futureValue) ~>
        http.routes ~>
        runRoute

      orders.expectMsgPF() {
        case OrderActor.Envelope(_, OrderActor.OpenOrder(server, stable)) =>
          assert(server === order.server)
          assert(stable === order.table)
        case msg => fail("Unexpected Message: "+msg)
      }

      orders.reply(OrderActor.OrderOpened(order))

      check {
        assert(status === StatusCodes.OK)
        assert(contentType === ContentTypes.`application/json`)
        assert(entityAs[Order] === order)
      } (result)
    }
  }

  "GET to /order/{id}" should {
    "return the order" in {
      val order = generateOrder()
      val result = Get(s"/order/${order.id.value.toString}") ~>
        http.routes ~>
        runRoute

      orders.expectMsg(OrderActor.Envelope(order.id, OrderActor.GetOrder()))
      orders.reply(order)

      check {
        assert(status === StatusCodes.OK)
        assert(contentType === ContentTypes.`application/json`)
        assert(entityAs[Order] === order)
      }(result)
    }
    "return a meaningful error if the order doesn't exist" in {
      val orderId = generateOrderId()
      val result = Get(s"/order/${orderId.value.toString}") ~>
        http.routes ~>
        runRoute

      val expectedError = OrderActor.OrderNotFoundException(orderId)

      orders.expectMsg(OrderActor.Envelope(orderId, OrderActor.GetOrder()))
      orders.reply(Status.Failure(expectedError))

      check {
        assert(status === StatusCodes.NotFound)
        assert(contentType === ContentTypes.`text/plain(UTF-8)`)
        assert(entityAs[String] === expectedError.getMessage)
      }(result)
    }
  }

  "POST to /order/{id}/items" should {
    "add an item to the order" in {
      val item = generateOrderItem()
      val order = generateOrder().withItem(item)
      val request = OrderActor.AddItemToOrder(item)

      val result = Post(s"/order/${order.id.value.toString}/items")
        .withEntity(Marshal(request).to[MessageEntity].futureValue) ~>
        http.routes ~>
        runRoute

      orders.expectMsg(OrderActor.Envelope(order.id, request))
      orders.reply(OrderActor.ItemAddedToOrder(order))

      check {
        assert(status === StatusCodes.OK)
        assert(contentType === ContentTypes.`application/json`)
        assert(entityAs[Order] === order)
      }(result)
    }
    "return a meaningful error if the order doesn't exist" in {
      val orderId = generateOrderId()
      val item = generateOrderItem()
      val request = OrderActor.AddItemToOrder(item)

      val expectedError = OrderNotFoundException(orderId)

      val result = Post(s"/order/${orderId.value.toString}/items")
        .withEntity(Marshal(request).to[MessageEntity].futureValue) ~>
        http.routes ~>
        runRoute

      orders.expectMsg(OrderActor.Envelope(orderId, request))
      orders.reply(Status.Failure(expectedError))

      check {
        assert(status === StatusCodes.NotFound)
        assert(contentType === ContentTypes.`text/plain(UTF-8)`)
        assert(entityAs[String] === expectedError.getMessage)
      }(result)    }
  }

}
