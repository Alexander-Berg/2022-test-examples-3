#!/usr/bin/env bash

npm run test-ci

statusCode=$?

mv html_reports $RESULT_RESOURCES_PATH

exit $statusCode
