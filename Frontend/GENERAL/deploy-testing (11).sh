#!/bin/bash
set -e

npm run build

[ $(git rev-parse --is-inside-git-dir) ] && COMMIT_SHA="`git rev-list -n1 HEAD`" || COMMIT_SHA="`arc log --max-count 1 --format '{commit}'`"

source tools/build-docker.sh $COMMIT_SHA

cat .config/deploy/.yd_tmpl_testing.yml \
    | sed "s/{{VERSION}}/${COMMIT_SHA}/" \
    | sed "s/{{DB_DELEGATION_TOKEN}}/${SAMPLEAPP_API_TESTING_DELEGATION_TOKEN}/" \
    | sed "s/{{PAYMENT_DELEGATION_TOKEN}}/${SAMPLEAPP_API_TESTING_PAYMENT_DELEGATION_TOKEN}/" \
    | sed "s/{{PLATFORM_DELEGATION_TOKEN}}/${SAMPLEAPP_API_TESTING_PLATFORM_DELEGATION_TOKEN}/" \
    > .yd_testing.yml

DCTL_YP_TOKEN=$TAP_DCTL_YP_TOKEN ya tool dctl put stage -c xdc .yd_testing.yml
rm .yd_testing.yml

./node_modules/.bin/tap-awacs-deploy deploy \
    --balancer tap-testing \
    --upstream-id sampleapp-api-testing \
    --spec-file ./.config/awacs/testing.yml \
    --order 1000
