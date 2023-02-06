#!/bin/bash
set -e

export HEAD_COMMIT_SHA=`node ../../packages/tap-release/cli/get-head-commit-sha.js`

source tools/build-static.sh
npx static-uploader

CSP_RULES=`node tools/generate-csp.js`

./node_modules/.bin/tap-awacs-deploy deploy \
    --balancer tap-testing \
    --upstream-id checkout-test-service-testing \
    --spec-file ./.config/awacs/testing.yml \
    --param "CSP_RULES=${CSP_RULES}" \
    --param "HEAD_COMMIT_SHA=${HEAD_COMMIT_SHA}" \
    --order 1001
