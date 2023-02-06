#!/bin/bash
set -e

if [[ -z "$TRENDBOX_PULL_REQUEST_NUMBER" ]]; then
    echo "TRENDBOX_PULL_REQUEST_NUMBER env variable must be set."
    exit 1
fi

source tools/build-static.sh
npx static-uploader

CSP_RULES=`node tools/generate-csp.js`

./node_modules/.bin/tap-awacs-deploy deploy \
    --balancer tap-testing \
    --upstream-id "checkout-test-service-staging-${TRENDBOX_PULL_REQUEST_NUMBER}" \
    --spec-file ./.config/awacs/staging.yml \
    --param "PR_NUMBER=${TRENDBOX_PULL_REQUEST_NUMBER}" \
    --param "CSP_RULES=${CSP_RULES}" \
    --order 1000
