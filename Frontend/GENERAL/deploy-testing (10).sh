#!/bin/bash
set -e

export HEAD_COMMIT_SHA=`node ../../packages/tap-release/cli/get-head-commit-sha.js`

source tools/build.sh
npx static-uploader

./node_modules/.bin/tap-awacs-deploy deploy \
    --balancer tap-testing \
    --upstream-id sampleapp-external-testing-index \
    --spec-file ./.config/awacs/testing-index.yml \
    --param "HEAD_COMMIT_SHA=${HEAD_COMMIT_SHA}" \
    --order 1001

./node_modules/.bin/tap-awacs-deploy deploy \
    --balancer tap-testing \
    --upstream-id sampleapp-external-testing-manifest \
    --spec-file ./.config/awacs/testing-manifest.yml \
    --param "HEAD_COMMIT_SHA=${HEAD_COMMIT_SHA}" \
    --order 1000

