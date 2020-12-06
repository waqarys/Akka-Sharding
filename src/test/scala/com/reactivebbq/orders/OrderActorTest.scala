package com.reactivebbq.orders

import akka.actor.Status
import akka.cluster.sharding.ShardRegion
import akka.testkit.TestProbe
import com.reactivebbq.orders.OrderActor._
import org.scalatest.WordSpec

import scala.collection.mutable
import scala.concurrent.Future

class OrderActorTest extends WordSpec with AkkaSpec with OrderHelpers {

  class MockRepo extends InMemoryOrderRepository {
    private val updates: mutable.Queue[Function1[Order, Future[Order]]] = mutable
      .Queue()
    private val finds: mutable.Queue[Function1[OrderId, Future[Option[Order]]]] = mutable.Queue()

    override def update(order: Order) = {
      if(updates.nonEmpty)
        updates.dequeue()(order)
      else
        super.update(order)
    }

    override def find(orderId: OrderId) = {
      if(finds.nonEmpty)
        finds.dequeue()(orderId)
      else
        super.find(orderId)
    }

    def mockUpdate(u: Order => Future[Order]) = {
      updates.enqueue(u)
      this
    }

    def mockFind(f: OrderId => Future[Option[Order]]) = {
      finds.enqueue(f)
      this
    }
  }

  class TestContext() {
    val repo = new MockRepo()
    val orderId = generateOrderId()
    val sender = TestProbe()
    val parent = TestProbe()

    val orderActor = parent.childActorOf(
      OrderActor.props(repo),
      orderId.value.toString
    )

    def openOrder(): Order = {
      val server = generateServer()
      val table = generateTable()

      sender.send(orderActor, OpenOrder(server, table))
      sender.expectMsgType[OrderOpened].order
    }
  }

  "idExtractor" should {
    "return the expected id and message" in {
      val orderId = generateOrderId()
      val message = GetOrder()
      val envelope = Envelope(orderId, message)

      val result = entityIdExtractor(envelope)

      assert(result === (orderId.value.toString, message))
    }
  }

  "shardIdExtractor" should {
    "return the expected shard id" in {
      val orderId = generateOrderId()
      val message = GetOrder()
      val envelope = Envelope(orderId, message)
      val startEntity = ShardRegion.StartEntity(orderId.value.toString)

      val envelopeShard = shardIdExtractor(envelope)
      val startEntityShard = shardIdExtractor(startEntity)

      assert(envelopeShard === startEntityShard)
      assert(envelopeShard === Math.abs(orderId.value.toString.hashCode % 30).toString)
    }
  }

  "The Actor" should {
    "Load it's state from the repository when created." in new TestContext {
      val order = generateOrder()
      repo.update(order).futureValue

      val actor = parent
        .childActorOf(
          OrderActor.props(repo),
          order.id.value.toString)

      sender.send(actor, GetOrder())
      sender.expectMsg(order)
    }
    "Terminate when it fails to load from the repo" in new TestContext {
      val order = generateOrder()

      val mockRepo = new MockRepo()
      mockRepo.mockFind(_ => Future.failed(new Exception("Repo Failed")))

      val actor = parent
        .childActorOf(
          OrderActor.props(mockRepo),
          order.id.value.toString)

      parent.watch(actor)
      parent.expectTerminated(actor)
    }
  }

  "OpenOrder" should {
    "initialize the Order" in new TestContext {
      val server = generateServer()
      val table = generateTable()

      sender.send(orderActor, OpenOrder(server, table))
      val order = sender.expectMsgType[OrderOpened].order

      assert(repo.find(order.id).futureValue === Some(order))

      assert(order.server === server)
      assert(order.table === table)
    }
    "return an error if the Order is already opened" in new TestContext {
      val server = generateServer()
      val table = generateTable()

      sender.send(orderActor, OpenOrder(server, table))
      sender.expectMsgType[OrderOpened].order

      sender.send(orderActor, OpenOrder(server, table))
      sender.expectMsg(Status.Failure(DuplicateOrderException(orderId)))
    }
    "return the repository failure if the repository fails and fail" in new TestContext() {
      val server = generateServer()
      val table = generateTable()

      parent.watch(orderActor)

      val expectedException = new RuntimeException("Repository Failure")
      repo.mockUpdate(_ => Future.failed(expectedException))

      sender.send(orderActor, OpenOrder(server, table))
      val result = sender.expectMsg(Status.Failure(expectedException))

      parent.expectTerminated(orderActor)
    }
    "not allow further interactions while it's in progress" in new TestContext() {
      val order = generateOrder(orderId = orderId, items = Seq.empty)

      repo.mockUpdate {
        order => Future {
          Thread.sleep(50)
          order
        }
      }

      sender.send(orderActor, OpenOrder(order.server, order.table))
      sender.send(orderActor, OpenOrder(order.server, order.table))

      sender.expectMsg(OrderOpened(order))
      sender.expectMsg(Status.Failure(DuplicateOrderException(orderId)))
    }
  }

  "AddItemToOrder" should {
    "return an OrderNotFoundException if the order hasn't been Opened." in new TestContext {
      val item = generateOrderItem()

      sender.send(orderActor, AddItemToOrder(item))
      sender.expectMsg(Status.Failure(OrderNotFoundException(orderId)))
    }
    "add the item to the order" in new TestContext {
      val order = openOrder()

      val item = generateOrderItem()

      sender.send(orderActor, AddItemToOrder(item))
      sender.expectMsg(ItemAddedToOrder(order.withItem(item)))
    }
    "add multiple items to the order" in new TestContext {
      val order = openOrder()

      val items = generateOrderItems(10)

      items.foldLeft(order) {
        case (prevOrder, item) =>
          val updated = prevOrder.withItem(item)

          sender.send(orderActor, AddItemToOrder(item))
          sender.expectMsg(ItemAddedToOrder(updated))

          updated
      }
    }
    "return the repository failure if the repository fails and fail" in new TestContext() {
      val order = openOrder()

      parent.watch(orderActor)

      val item = generateOrderItem()

      val expectedException = new Exception("Repository Failure")
      repo.mockUpdate(_ => Future.failed(expectedException))

      sender.send(orderActor, AddItemToOrder(item))
      sender.expectMsg(Status.Failure(expectedException))

      parent.expectTerminated(orderActor)
    }
    "not allow further interactions while it's in progress" in new TestContext() {
      val order = openOrder()

      val item1 = generateOrderItem()
      val updated1 = order.withItem(item1)

      val item2 = generateOrderItem()
      val updated2 = updated1.withItem(item2)

      repo.mockUpdate {
        order => Future {
          Thread.sleep(50)
          order
        }
      }

      sender.send(orderActor, AddItemToOrder(item1))
      sender.send(orderActor, AddItemToOrder(item2))

      sender.expectMsg(ItemAddedToOrder(updated1))
      sender.expectMsg(ItemAddedToOrder(updated2))
    }
  }

  "GetOrder" should {
    "return an OrderNotFoundException if the order hasn't been Opened." in new TestContext {
      sender.send(orderActor, GetOrder())
      sender.expectMsg(Status.Failure(OrderNotFoundException(orderId)))
    }
    "return an open order" in new TestContext {
      val order = openOrder()

      sender.send(orderActor, GetOrder())
      sender.expectMsg(order)
    }
    "return an updated order" in new TestContext {
      val order = openOrder()
      val item = generateOrderItem()

      sender.send(orderActor, AddItemToOrder(item))
      sender.expectMsgType[ItemAddedToOrder].order

      sender.send(orderActor, GetOrder())
      sender.expectMsg(order.withItem(item))
    }
  }
}
