package com.reactivebbq.orders

import org.h2.tools.Server._
import org.slf4j.LoggerFactory

object H2Database extends App {
  val logger = LoggerFactory.getLogger("root")

  logger.info("STARTING H2 DATABASE SERVER")

  val server = createTcpServer("-ifNotExists")
  server.start()

  logger.info("H2 DATABASE SERVER IS RUNNING")
}
