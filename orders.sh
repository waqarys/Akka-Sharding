#!/usr/bin/env bash

set -x

sbt "runMain com.reactivebbq.orders.Client ${@}"
