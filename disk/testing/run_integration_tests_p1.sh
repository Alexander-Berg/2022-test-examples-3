#!/usr/bin/env bash

#SETUP: очистка логов пред тестами
rm -rf /var/log/mpfs/*

cd test

BRANCH=$(git symbolic-ref --short HEAD || echo non-master)
if [[ "$BRANCH" == "master" ]]; then
  COVERAGE_OPTIONS="--cov=mpfs --cov-config=.coveragerc --cov-append"
fi

py.test -v --dist=loadscope --reruns=3 --junit-xml=report.xml ${COVERAGE_OPTIONS: } -n 8 \
  parallelly/b2b \
  parallelly/billing \
  parallelly/dao \
  parallelly/djfs_api \
  parallelly/filesystem \
  parallelly/global_gallery \
  parallelly/json_api \
  parallelly/office_suit.py
CODE=$?

if [[ "$BRANCH" == "master" ]]; then
  coverage combine
  coverage xml
fi

exit $CODE
