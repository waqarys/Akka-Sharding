package com.reactivebbq.orders

import org.scalatest.WordSpec

class OrderTest extends WordSpec with OrderHelpers {

  "withItem" should {
    "return a copy of the Order with the new item when no previous items exist" in {
      val orderItem = generateOrderItem()

      val order = generateOrder(items = Seq.empty)
      val updated = order.withItem(orderItem)

      assert(order.items === Seq.empty)
      assert(updated.items === Seq(orderItem))
    }
    "return a copy of the Order with the new item appended when previous items exist" in {
      val oldItems = generateOrderItems(10)
      val newItem = generateOrderItem()

      val order = generateOrder(items = oldItems)
      val updated = order.withItem(newItem)

      assert(order.items === oldItems)
      assert(updated.items === oldItems :+ newItem)
    }
  }
}
