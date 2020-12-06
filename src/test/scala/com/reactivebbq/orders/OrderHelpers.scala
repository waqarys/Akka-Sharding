package com.reactivebbq.orders

import scala.util.Random

trait OrderHelpers {
  private val rnd = new Random(System.currentTimeMillis())
  private val rndInts = (1 to 10000).map(_ => rnd.nextInt(1000000)).toSet.iterator

  def generateOrderId(): OrderId = OrderId()
  def generateServer(): Server = Server("ServerName"+rndInts.next())
  def generateTable(): Table = Table(rndInts.next())

  def generateOrderItem(
    name: String = "ItemName"+rndInts.next(),
    specialInstructions: String = "SpecialInstructions"+rndInts.next()
  ): OrderItem = {
    OrderItem(
      name,
      specialInstructions
    )
  }

  def generateOrderItems(quantity: Int = rnd.nextInt(10)): Seq[OrderItem] = {
    (1 to quantity).map(_ => generateOrderItem())
  }

  def generateOrder(
    orderId: OrderId = generateOrderId(),
    serverId: Server = generateServer(),
    tableNumber: Table = generateTable(),
    items: Seq[OrderItem] = generateOrderItems()
  ): Order = Order(
    orderId,
    serverId,
    tableNumber,
    items
  )
}
