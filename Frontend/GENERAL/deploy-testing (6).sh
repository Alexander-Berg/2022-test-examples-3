#!/bin/bash
set -e

export HEAD_COMMIT_SHA=`node ../../packages/tap-release/cli/get-head-commit-sha.js`

source tools/build.sh
npx static-uploader

CSP_RULES=`cat build/csp-rules.txt`

./node_modules/.bin/tap-awacs-deploy deploy \
    --balancer tap-testing \
    --upstream-id checkout-testing-index \
    --spec-file ./.config/awacs/testing-index.yml \
    --param "CSP_RULES=${CSP_RULES}" \
    --param "HEAD_COMMIT_SHA=${HEAD_COMMIT_SHA}" \
    --order 1001

./node_modules/.bin/tap-awacs-deploy deploy \
    --balancer tap-testing \
    --upstream-id checkout-testing-manifest \
    --spec-file ./.config/awacs/testing-manifest.yml \
    --param "HEAD_COMMIT_SHA=${HEAD_COMMIT_SHA}" \
    --order 1000

./node_modules/.bin/tap-awacs-deploy deploy \
    --balancer tap-testing \
    --upstream-id "checkout-testing-experiments" \
    --spec-file ./.config/awacs/testing-experiments.yml \
    --order 1000
