#!/bin/bash
set -e

export HEAD_COMMIT_SHA=`node ../../packages/tap-release/cli/get-head-commit-sha.js`

for service in taxi taxi-desktop delivery terminal
do
    REACT_APP_SERVICE="$service" source tools/build.sh
    REACT_APP_SERVICE="$service" npx static-uploader
    CSP_RULES=`node tools/generate-csp.js`

    ./node_modules/.bin/tap-awacs-deploy deploy \
        --balancer tap-testing \
        --upstream-id "${service}-testing-index" \
        --spec-file ./.config/awacs/testing-index.yml \
        --param "SERVICE=${service}" \
        --param "CSP_RULES=${CSP_RULES}" \
        --param "HEAD_COMMIT_SHA=${HEAD_COMMIT_SHA}" \
        --order 1001

    ./node_modules/.bin/tap-awacs-deploy deploy \
        --balancer tap-testing \
        --upstream-id "${service}-testing-manifest" \
        --spec-file ./.config/awacs/testing-manifest.yml \
        --param "SERVICE=${service}" \
        --param "HEAD_COMMIT_SHA=${HEAD_COMMIT_SHA}" \
        --order 1000

    ./node_modules/.bin/tap-awacs-deploy deploy \
        --balancer tap-testing \
        --upstream-id "${service}-testing-logo" \
        --spec-file ./.config/awacs/testing-logo.yml \
        --param "SERVICE=${service}" \
        --param "HEAD_COMMIT_SHA=${HEAD_COMMIT_SHA}" \
        --order 1000

    ./node_modules/.bin/tap-awacs-deploy deploy \
        --balancer tap-testing \
        --upstream-id "${service}-testing-check" \
        --spec-file ./.config/awacs/testing-check.yml \
        --param "SERVICE=${service}" \
        --order 1000

    ./node_modules/.bin/tap-awacs-deploy deploy \
        --balancer tap-testing \
        --upstream-id "${service}-testing-favicon" \
        --spec-file ./.config/awacs/testing-favicon.yml \
        --param "SERVICE=${service}" \
        --param "HEAD_COMMIT_SHA=${HEAD_COMMIT_SHA}" \
        --order 1000

    ./node_modules/.bin/tap-awacs-deploy deploy \
        --balancer tap-testing \
        --upstream-id "${service}-testing-experiments" \
        --spec-file ./.config/awacs/testing-experiments.yml \
        --param "SERVICE=${service}" \
        --order 1000

    ./node_modules/.bin/tap-awacs-deploy deploy \
        --balancer tap-testing \
        --upstream-id "${service}-testing-service-worker" \
        --spec-file ./.config/awacs/testing-service-worker.yml \
        --param "SERVICE=${service}" \
        --param "HEAD_COMMIT_SHA=${HEAD_COMMIT_SHA}" \
        --order 1000
done
