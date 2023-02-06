#!/bin/bash
set -e

export HEAD_COMMIT_SHA=`node ../../packages/tap-release/cli/get-head-commit-sha.js`

source tools/build.sh
npx static-uploader
source tools/check-static-upload.sh

CSP_RULES=`node tools/generate-csp.js`

./node_modules/.bin/tap-awacs-deploy deploy \
    --balancer tap-testing \
    --upstream-id weather-testing-index \
    --spec-file ./.config/awacs/testing-index.yml \
    --param "CSP_RULES=${CSP_RULES}" \
    --param "HEAD_COMMIT_SHA=${HEAD_COMMIT_SHA}" \
    --order 1002

./node_modules/.bin/tap-awacs-deploy deploy \
    --balancer tap-testing \
    --upstream-id weather-testing-manifest \
    --spec-file ./.config/awacs/testing-manifest.yml \
    --param "HEAD_COMMIT_SHA=${HEAD_COMMIT_SHA}" \
    --order 1000

./node_modules/.bin/tap-awacs-deploy deploy \
    --balancer tap-testing \
    --upstream-id weather-testing-build \
    --spec-file ./.config/awacs/testing-build.yml \
    --param "CSP_RULES=${CSP_RULES}" \
    --param "HEAD_COMMIT_SHA=${HEAD_COMMIT_SHA}" \
    --order 1001
