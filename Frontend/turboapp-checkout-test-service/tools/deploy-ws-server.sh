#!/bin/bash
set -e

npm run build:server

[ $(git rev-parse --is-inside-git-dir) ] && COMMIT_SHA="`git rev-list -n1 HEAD`" || COMMIT_SHA="`arc log --max-count 1 --format '{commit}'`"

source tools/build-docker.sh $COMMIT_SHA

cat .config/deploy/.yd_tmpl.yml \
    | sed "s/{{VERSION}}/${COMMIT_SHA}/" \
    > .yd_testing.yml

DCTL_YP_TOKEN=$TAP_DCTL_YP_TOKEN ya tool dctl put stage -c xdc .yd_testing.yml
rm .yd_testing.yml

./node_modules/.bin/tap-awacs-deploy deploy \
    --balancer tap-testing \
    --upstream-id checkout-test-service-ws-testing \
    --spec-file ./.config/awacs/testing-server.yml \
    --order 1000
