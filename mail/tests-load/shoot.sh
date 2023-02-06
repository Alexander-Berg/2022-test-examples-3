#!/bin/bash

if [[ -z "$1" ]]; then
    echo "usage: shoot.sh <json payload file>"
    exit 1;
fi

# -r              Don't exit on socket receive errors.
# -k              Use HTTP KeepAlive feature
# -n requests     Number of requests to perform
# -c concurrency  Number of multiple requests to make at a time
ab -p "$1" -T "application/json" -r -k \
    -n 10000 -c 100 \
    botserver-1.botserver.loadtest.botserver.mail.stable.qloud-d.yandex.net/webhook
