#!/bin/bash

function hermione_test() {
    set +e
    HERMIONE_ENV=ci PR_NUMBER=${PR_NUMBER} npm run test:hermione || error="true"
    set -e
    if [ $error = "true" ]
    then
      publish_report
      add_comment "❌ Hermione тесты упали, [отчёт](https://s3.mds.yandex.net/contest-hidden/contest-admin/hermione-reports/${PR_NUMBER}/index.html)"
      exit 1
    else
      add_comment "✅ Hermione тесты пройдены"
    fi
}
