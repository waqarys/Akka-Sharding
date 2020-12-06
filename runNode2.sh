#!/usr/bin/env bash

set -x

sbt "runMain com.reactivebbq.orders.Main -Dakka.http.server.default-http-port=8001 -Dakka.remote.artery.canonical.port=2552 -Dakka.management.http.port=8559 -Dcinnamon.prometheus.http-server.port=9002"
