#!/usr/bin/env bash

set -x

sbt "runMain com.reactivebbq.orders.LoadTest -Dcinnamon.prometheus.http-server.port=9004"
