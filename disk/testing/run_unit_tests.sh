#!/bin/bash

tools/testing/setup_before_tests.sh

cd test

BRANCH=$(git symbolic-ref --short HEAD || echo non-master)
if [[ "$BRANCH" == "master" ]]; then
  COVERAGE_OPTIONS="--cov=mpfs --cov-config=.coveragerc"
  coverage erase
fi

py.test -v --junit-xml=unit.xml ${COVERAGE_OPTIONS: } unit
