package com.reactivebbq.orders

import org.scalatest.WordSpec

class OrderIdTest extends WordSpec {

  "apply" should {
    "generate a unique id each time it is called" in {
      val ids = (1 to 10).map(_ => OrderId())

      assert(ids.toSet.size === ids.size)
      assert(ids.map(_.value).toSet.size === ids.map(_.value).size)
    }
  }
}
