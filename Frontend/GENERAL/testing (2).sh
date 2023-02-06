#!/usr/bin/env bash

set -e

BASEDIR=$(dirname $0)

export APP_VERSION=beta-${npm_package_version}-$(${BASEDIR}/getCommit.js)
export AWS_ACCESS_KEY_ID=${CONTEST_AWS_TEST_ACCESS_KEY_ID}
export AWS_SECRET_ACCESS_KEY=${CONTEST_AWS_TEST_SECRET_ACCESS_KEY}
export S3_ENDPOINT='http://s3.mdst.yandex.net'
export S3_ASSET_BASE_URL='https://s3.mdst.yandex.net'

S3_ASSET_PATH="contest/contest-alcina/${APP_VERSION}"
S3_ASSET_URL="${S3_ASSET_BASE_URL}/${S3_ASSET_PATH}"
IMAGE_TAG=registry.yandex.net/contest/contest-alcina:${APP_VERSION}

# for debug
. ${BASEDIR}/printEnv.sh

. ${BASEDIR}/installDependencies.sh
. ${BASEDIR}/build.sh
. ${BASEDIR}/publishStatic.sh
. ${BASEDIR}/publishDocker.sh

YENV=production npx --package "@yandex-int/frontend.ci.deploy" frontend-deploy deploy -c ${BASEDIR}/../../.config/deploy/config.testing.yml
