#!/usr/bin/env bash

#SETUP: очистка логов пред тестами
rm -rf /var/log/mpfs/*

cd test

BRANCH=$(git symbolic-ref --short HEAD || echo non-master)
if [[ "$BRANCH" == "master" ]]; then
  COVERAGE_OPTIONS="--cov=mpfs --cov-config=.coveragerc --cov-append"
fi

py.test -v --dist=loadscope --reruns=3 --junit-xml=report.xml ${COVERAGE_OPTIONS: } -n 8 \
  --ignore=parallelly/api/ \
  --ignore=parallelly/office_suit.py \
  --ignore=parallelly/yarovaya_storage_optimization_suit.py \
  --ignore=parallelly/b2b/ \
  --ignore=parallelly/billing/ \
  --ignore=parallelly/dao/ \
  --ignore=parallelly/djfs_api/ \
  --ignore=parallelly/filesystem/ \
  --ignore=parallelly/global_galery/ \
  --ignore=parallelly/json_api/ \
  parallelly
CODE=$?

if [[ "$BRANCH" == "master" ]]; then
  coverage combine
  coverage xml
fi

exit $CODE
