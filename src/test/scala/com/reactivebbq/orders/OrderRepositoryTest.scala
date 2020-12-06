package com.reactivebbq.orders

import org.scalatest.WordSpec
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait OrderRepositoryTest extends WordSpec with OrderHelpers with ScalaFutures {
  val orderRepo: OrderRepository

  "The repository" should {
    "be thread safe" in {
      val updateResults = Future.sequence((1 to 100).map { _ =>
        val order = generateOrder()
        orderRepo.update(order)
      }).futureValue

      val findResults = Future.sequence(updateResults.map { order =>
        orderRepo.find(order.id)
      }).futureValue.flatten

      assert(findResults.sortBy(_.id.toString) === updateResults.sortBy(_.id.toString))
    }
  }

  "find" should {
    "return None if the repository is empty" in {
      val orderId = generateOrderId()
      val result = orderRepo.find(orderId).futureValue

      assert(result === None)
    }
    "return None if the repository is not empty, but the order doesn't exist" in {
      (1 to 10).foreach(_ => orderRepo.update(generateOrder()).futureValue)

      val orderId = generateOrderId()
      val result = orderRepo.find(orderId).futureValue

      assert(result === None)
    }
    "return the order if it is the only one in the repo" in {
      val order = generateOrder()

      val updateResult = orderRepo.update(order).futureValue
      val findResult = orderRepo.find(order.id).futureValue

      assert(updateResult === order)
      assert(findResult === Some(order))
    }
    "return the correct order when there is more than one in the repo" in {
      (1 to 10).foreach(_ => orderRepo.update(generateOrder()).futureValue)

      val order = generateOrder()

      val updateResult = orderRepo.update(order).futureValue
      val findResult = orderRepo.find(order.id).futureValue

      assert(updateResult === order)
      assert(findResult === Some(order))
    }
    "return each correct order when there is more than one in the repo" in {
      val order1 = generateOrder()
      val order2 = generateOrder()

      orderRepo.update(order1).futureValue
      orderRepo.update(order2).futureValue

      val findResult1 = orderRepo.find(order1.id).futureValue
      val findResult2 = orderRepo.find(order2.id).futureValue

      assert(findResult1 === Some(order1))
      assert(findResult2 === Some(order2))
    }
  }

  "update" should {
    "add the order to the repo if it doesn't exist" in {
      val order = generateOrder()

      val updateResult = orderRepo.update(order).futureValue
      val findResult = orderRepo.find(order.id).futureValue

      assert(updateResult === order)
      assert(findResult === Some(order))
    }
    "overwrite the order if it already exists" in {
      val order = generateOrder()
      val updated = order.withItem(generateOrderItem())

      val insertResult = orderRepo.update(order).futureValue
      val updateResult = orderRepo.update(updated).futureValue
      val findResult = orderRepo.find(order.id).futureValue

      assert(insertResult === order)
      assert(updateResult === updated)
      assert(findResult === Some(updated))
    }
  }
}

class InMemoryOrderRepositoryTest extends OrderRepositoryTest {
  override val orderRepo = new InMemoryOrderRepository
}

class SQLOrderRepositoryTest extends OrderRepositoryTest with IntegrationPatience {
  override val orderRepo = new SQLOrderRepository()
}
