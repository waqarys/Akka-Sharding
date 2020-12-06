package com.reactivebbq.orders

import java.util.UUID

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, DeserializationException, JsString, JsValue, JsonFormat}

trait OrderJsonFormats extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val orderIdFormat = new JsonFormat[OrderId] {
    def write(orderId: OrderId) = JsString(orderId.value.toString)
    def read(value: JsValue): OrderId = {
      value match {
        case JsString(uuid) => OrderId(UUID.fromString(uuid))
        case _              => throw DeserializationException("Expected UUID string")
      }
    }
  }

  implicit val serverFormat = jsonFormat1(Server)
  implicit val tableFormat = jsonFormat1(Table)
  implicit val orderItemFormat = jsonFormat2(OrderItem)
  implicit val orderFormat = jsonFormat4(Order)
  implicit val openOrderFormat = jsonFormat2(OrderActor.OpenOrder)
  implicit val addItemToOrderFormat = jsonFormat1(OrderActor.AddItemToOrder)
}
