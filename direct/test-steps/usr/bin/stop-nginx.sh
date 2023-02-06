#!/bin/bash

PID=$(cat /var/run/nginx-direct-steps.pid)
kill -s QUIT $PID
while kill -0 $PID 2>/dev/null; do sleep 1; done
