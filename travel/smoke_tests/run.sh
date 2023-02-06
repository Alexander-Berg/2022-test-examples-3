#!/usr/bin/env bash

echo args: $@
ya make  -t
cd bin/tests && ./travel-rasp-smoke_tests-bin-tests $@
