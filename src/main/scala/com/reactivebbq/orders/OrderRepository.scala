package com.reactivebbq.orders

import java.util.UUID

import javax.persistence._

import scala.concurrent.{ExecutionContext, Future}

import scala.jdk.CollectionConverters._

trait OrderRepository {
  def update(order: Order): Future[Order]
  def find(orderId: OrderId): Future[Option[Order]]
}

class InMemoryOrderRepository()(implicit ec: ExecutionContext) extends OrderRepository {
  private var orders = Map.empty[OrderId, Order]

  override def update(order: Order): Future[Order] = {
    Future {
      synchronized {
        orders = orders.updated(order.id, order)
      }
      order
    }
  }

  override def find(orderId: OrderId): Future[Option[Order]] = {
    Future {
      synchronized {
        orders.get(orderId)
      }
    }
  }
}

@Embeddable
class OrderItemDBO() {
  private var name: String = ""
  private var specialInstructions: String = ""

  def getName: String = name
  def getSpecialInstructions: String = specialInstructions

  def apply(orderItem: OrderItem): OrderItemDBO = {
    name = orderItem.name
    specialInstructions = orderItem.specialInstructions

    this
  }
}

@Entity
@javax.persistence.Table(name = "orders")
class OrderDBO() {

  @Id
  private var id: UUID = _

  private var serverName: String = _
  private var tableNumber: Int = _

  @ElementCollection(targetClass = classOf[OrderItemDBO])
  private var items: java.util.List[OrderItemDBO] = _

  def getServerName: String = serverName
  def getTableNumber: Int = tableNumber
  def getItems: List[OrderItemDBO] = items.asScala.toList

  def apply(order: Order): OrderDBO = {
    id = order.id.value
    serverName = order.server.name
    tableNumber = order.table.number
    items = order.items.map(item => new OrderItemDBO().apply(item)).asJava

    this
  }
}

class SQLOrderRepository()(implicit ec: ExecutionContext) extends OrderRepository {

  private val entityManagerFactory = Persistence.createEntityManagerFactory("reactivebbq.Orders")

  private val threadLocalEntityManager = new ThreadLocal[EntityManager]()

  private def getEntityManager: EntityManager = {
    if(threadLocalEntityManager.get() == null) {
      threadLocalEntityManager.set(entityManagerFactory.createEntityManager())
    }

    threadLocalEntityManager.get()
  }

  private def transaction[A](f: EntityManager => A): A = {
    val entityManager = getEntityManager

    entityManager.getTransaction.begin()
    val result = f(entityManager)
    entityManager.getTransaction.commit()
    result
  }

  override def update(order: Order): Future[Order] = Future {
    val dbo = new OrderDBO().apply(order)
    transaction(_.merge(dbo))
    order
  }

  override def find(orderId: OrderId): Future[Option[Order]] = Future {
    transaction { t =>
      Option(t.find(classOf[OrderDBO], orderId.value))
        .map(dbo => Order(
          orderId,
          Server(dbo.getServerName),
          com.reactivebbq.orders.Table(dbo.getTableNumber),
          dbo.getItems.map(item => OrderItem(item.getName, item.getSpecialInstructions))
        ))
    }
  }
}
