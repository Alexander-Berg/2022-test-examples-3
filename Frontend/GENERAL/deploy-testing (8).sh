#!/bin/bash
set -e

export HEAD_COMMIT_SHA=`node ../../packages/tap-release/cli/get-head-commit-sha.js`

source tools/build.sh
npx static-uploader

CSP_RULES=`node tools/generate-csp.js`

./node_modules/.bin/tap-awacs-deploy deploy \
    --balancer tap-testing \
    --upstream-id lavka-testing-index \
    --spec-file ./.config/awacs/testing-index.yml \
    --param "CSP_RULES=${CSP_RULES}" \
    --param "HEAD_COMMIT_SHA=${HEAD_COMMIT_SHA}" \
    --order 1001

./node_modules/.bin/tap-awacs-deploy deploy \
    --balancer tap-testing \
    --upstream-id lavka-testing-manifest \
    --spec-file ./.config/awacs/testing-manifest.yml \
    --param "HEAD_COMMIT_SHA=${HEAD_COMMIT_SHA}" \
    --order 1000

./node_modules/.bin/tap-awacs-deploy deploy \
    --balancer tap-testing \
    --upstream-id lavka-testing-favicon \
    --spec-file ./.config/awacs/testing-favicon.yml \
    --param "HEAD_COMMIT_SHA=${HEAD_COMMIT_SHA}" \
    --order 1000

./node_modules/.bin/tap-awacs-deploy deploy \
    --balancer tap-testing \
    --upstream-id lavka-testing-favicon-192 \
    --spec-file ./.config/awacs/testing-favicon-192.yml \
    --param "HEAD_COMMIT_SHA=${HEAD_COMMIT_SHA}" \
    --order 1000
