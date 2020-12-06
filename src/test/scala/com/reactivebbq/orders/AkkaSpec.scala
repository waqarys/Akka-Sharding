package com.reactivebbq.orders

import akka.actor.ActorSystem
import akka.util.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, Suite}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

trait AkkaSpec extends BeforeAndAfterAll with ScalaFutures { _: Suite =>
  protected implicit val system: ActorSystem = ActorSystem(this.getClass.getSimpleName)
  protected implicit val ec: ExecutionContext = system.dispatcher
  protected implicit val askTimeout: Timeout = Timeout(5.seconds)

  override protected def afterAll(): Unit = {
    system.terminate()
    super.afterAll()
  }
}
