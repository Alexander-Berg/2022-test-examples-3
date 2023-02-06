#!/bin/bash
set -e

if [[ ! -z "$TRENDBOX_PULL_REQUEST_NUMBER" ]]; then
    PUBLIC_URL="https://tap-test.s3.mds.yandex.net/checkout-test-service/pr-${TRENDBOX_PULL_REQUEST_NUMBER}" \
    REACT_APP_ENV="development" \
    npm run build:client
elif [[ ! -z "$TRENDBOX_BRANCH" && ! -z "$HEAD_COMMIT_SHA" ]]; then
    PUBLIC_URL="https://tap-test.s3.mds.yandex.net/checkout-test-service/${TRENDBOX_BRANCH}/${HEAD_COMMIT_SHA}" \
    REACT_APP_ENV="production" \
    npm run build:client
else
    echo "Не удалось определить PUBLIC_URL для статики"
    exit 1;
fi
