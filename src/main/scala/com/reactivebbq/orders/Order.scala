package com.reactivebbq.orders

import java.util.UUID

object OrderId {
  def apply(): OrderId = OrderId(UUID.randomUUID())
}

case class OrderId(value: UUID)

case class Server(name: String)
case class Table(number: Int)

case class OrderItem(name: String, specialInstructions: String)

case class Order(
  id: OrderId,
  server: Server,
  table: Table,
  items: Seq[OrderItem]
) extends SerializableMessage {

  def withItem(item: OrderItem): Order = {
    this.copy(items = items :+ item)
  }
}
