#!/bin/sh

# shellcheck source=scripts/prepare_nvm.sh
. "$PROJECT_DIR/scripts/ci/prepare_nvm.sh"

cd "$PROJECT_DIR" || exit 1

[ -z "$ya" ] && ya="$ARCADIA_PATH/ya"

set -ex

nvm use

npm ci

if [ -n "$DEFINITIONS_SOURCE" ]; then
  curl $DEFINITIONS_SOURCE > src/java/definitions.ts
fi

npm run test:coverage:report

if [ "$BUILD_ENV" = "testing"  ]; then
  PREFIX_DIR=src BASE_REPORT_URL=$AWS_ENDPOINT/$AWS_BUCKET/coverage/coverage-summary.json  node node_modules/@yandex-market/mbo-coverage-checker/dist/scripts/checkFilesCoverage.js
  cp -r ./coverage ./report
  mv coverage $RESULT_RESOURCES_PATH
  REPORT_PATH="./report/coverage-summary.json" npm run test:coverage:compare
fi

if [ "$BUILD_ENV" = "production"  ]; then
  $ya tool aws configure set default.s3.max_concurrent_requests 25
  $ya tool aws --endpoint-url="$AWS_ENDPOINT" s3 sync $REPORT_FOLDER "s3://$AWS_BUCKET/coverage" --delete --exclude '*.txt' --acl 'public-read'

  echo "$AWS_ENDPOINT/$AWS_BUCKET/coverage/index.html"
  npm run test:coverage:record
fi



