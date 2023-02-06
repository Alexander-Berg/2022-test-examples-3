#!/bin/bash

killall -q -9 rcache
trap 'kill $(jobs -p)' EXIT # kill jobs on exit

echo run rcache
cd ..
./rcache etc/rcache/dev.yml &
pid=$!
sleep 2

echo run tests
nosetests --stop --verbose --nocapture tests-system/tests.py