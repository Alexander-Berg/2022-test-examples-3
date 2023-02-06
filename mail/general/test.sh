#!/bin/bash

./example config.yml &
PID="$!"
sleep 1

curl -s 'http://localhost:14488/handler?uid=123'; echo ''

kill "$PID"
