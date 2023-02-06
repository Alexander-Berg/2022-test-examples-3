#!/usr/bin/env bash

mkdir -p $RESULT_RESOURCES_PATH/txt_reports

npm run coverage-priority-high-ci

scripts/ci/jest-coverage.js > $RESULT_RESOURCES_PATH/txt_reports/base.txt

arc checkout HEAD^

npm run coverage-priority-high-ci

scripts/ci/jest-coverage.js > $RESULT_RESOURCES_PATH/txt_reports/target.txt

arc reset --hard HEAD

arc checkout ${context.launch_pull_request_info.vcs_info.feature_branch}

node scripts/ci/antidrop.js \
  $RESULT_RESOURCES_PATH/txt_reports/target.txt \
  $RESULT_RESOURCES_PATH/txt_reports/base.txt \
  > $RESULT_RESOURCES_PATH/txt_reports/report.txt
