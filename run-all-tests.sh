#!/usr/bin/env bash

TESTS=""
for i in {1..5}; do
  TESTS="$TESTS StreamingWrapper$i"
done
sbt -java-home /tools/batonroot/rodin/devkits/lnx64/jdk1.8.0_144 "test:run-main examples.Launcher$TESTS"
