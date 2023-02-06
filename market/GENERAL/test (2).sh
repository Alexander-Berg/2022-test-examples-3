#!/bin/sh

# shellcheck source=scripts/prepare_nvm.sh
. "$PROJECT_DIR/scripts/ci/prepare_nvm.sh"
cd "$PROJECT_DIR" || exit 1

nvm install
nvm use
npm i
npm run test:coverage:report
#
npm run bundle:report

PREFIX_DIR=frontend BASE_REPORT_URL=$AWS_ENDPOINT/$AWS_BUCKET_DEPLOY/$PROJECT_NAME/coverage/coverage-summary.json REPORT_FOLDER=./html_reports node node_modules/@yandex-market/mbo-coverage-checker/dist/scripts/checkFilesCoverage.js
#
mv html_reports $RESULT_RESOURCES_PATH
