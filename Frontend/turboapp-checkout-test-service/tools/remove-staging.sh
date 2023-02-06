#!/bin/bash
set -e

if [[ -z "$TRENDBOX_PULL_REQUEST_NUMBER" ]]; then
    echo "TRENDBOX_PULL_REQUEST_NUMBER env variable must be set."
    exit 1
fi

./node_modules/.bin/tap-awacs-deploy remove \
    --balancer tap-testing \
    --upstream-id "checkout-test-service-staging-${TRENDBOX_PULL_REQUEST_NUMBER}"
