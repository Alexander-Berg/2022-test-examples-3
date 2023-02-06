#!/bin/bash

tools/testing/setup_before_tests.sh

cd test

BRANCH=$(git symbolic-ref --short HEAD || echo non-master)
if [[ "$BRANCH" == "master" ]]; then
  COVERAGE_OPTIONS="--cov=mpfs.platform --cov-append --cov-config=.platform-coveragerc"
fi

py.test -v --reruns=3 --junit-xml=platform-report.xml ${COVERAGE_OPTIONS: } -n 8 parallelly/api
CODE=$?

if [[ "$BRANCH" == "master" ]]; then
  coverage combine
  coverage xml
fi

exit $CODE
