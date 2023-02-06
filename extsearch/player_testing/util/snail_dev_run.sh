#!/usr/bin/env bash

set -e -x

cleanup() {
    kill $proxy_pid
    kill $logagg_pid
    kill $certgen_pid
    kill $queue_pid
    exit 0
}

trap "cleanup" SIGINT

export SNAIL_ENV=dev
export SNAIL_DOCKER=1

/bin/cpproxy & proxy_pid=$!
/bin/log_aggregator & logagg_pid=$!
/bin/cert_gen & certgen_pid=$!
/bin/queue_worker & queue_pid=$!

while /bin/true; do
    sleep 60
done
