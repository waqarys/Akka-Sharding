#!/usr/bin/env bash

set -x

sbt "runMain com.reactivebbq.orders.Main -Dakka.http.server.default-http-port=8002 -Dakka.remote.artery.canonical.port=2553 -Dakka.management.http.port=8560 -Dcinnamon.prometheus.http-server.port=9003"
