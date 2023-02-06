#!/bin/bash
set -e

npm run build

COMMIT_SHA="`git rev-list -n1 HEAD`"

source tools/build-docker.sh $COMMIT_SHA
source .config/geobase-version.conf

cat .config/deploy/.yd_tmpl_testing.yml \
    | sed "s/{{VERSION}}/${COMMIT_SHA}/" \
    | sed "s/{{GEOBASE_URL}}/${GEOBASE_URL}/" \
    | sed "s/{{GEOBASE_CHECKSUM}}/${GEOBASE_CHECKSUM}/" \
    | sed "s/{{TVM_DELEGATION_TOKEN}}/${TAP_BACKEND_TESTING_TVM_DELEGATION_TOKEN}/" \
    | sed "s/{{LOGBROKER_DELEGATION_TOKEN}}/${TAP_BACKEND_TESTING_LOGBROKER_DELEGATION_TOKEN}/" \
    > .yd_testing.yml

DCTL_YP_TOKEN=$TAP_DCTL_YP_TOKEN ya tool dctl put stage -c xdc .yd_testing.yml
rm .yd_testing.yml

./node_modules/.bin/tap-awacs-deploy deploy \
    --balancer tap-testing \
    --upstream-id tap-backend-testing \
    --spec-file ./.config/awacs/testing.yml \
    --order 1000
