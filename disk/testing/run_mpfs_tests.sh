#!/bin/bash

#SETUP: очистка логов пред тестами
rm -rf /var/log/mpfs/*

cd test

BRANCH=$(git symbolic-ref --short HEAD || echo non-master)
if [[ "$BRANCH" == "master" ]]; then
  COVERAGE_OPTIONS="--cov=mpfs --cov-config=.coveragerc --cov-append"
fi

py.test -v --dist=loadscope --reruns=3 --junit-xml=report.xml ${COVERAGE_OPTIONS: } -n 8 --ignore=parallelly/api/ parallelly
CODE=$?
py.test -v --reruns=3 --junit-xml=report-consistently.xml ${COVERAGE_OPTIONS: } consistently
(( CODE = CODE || $? ))
py.test -v --reruns=3 --junit-xml=report-component.xml ${COVERAGE_OPTIONS: } component
(( CODE = CODE || $? ))

coverage combine
coverage xml

exit $CODE
