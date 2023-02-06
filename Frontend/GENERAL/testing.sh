#!/usr/bin/env bash

BASEDIR=$(dirname $0)

export APP_VERSION=beta-${npm_package_version}-$(${BASEDIR}/getCommit.js)
# TODO: После https://st.yandex-team.ru/CONTEST-4965 поправить на CONTEST_AWS_TEST_*
export AWS_ACCESS_KEY_ID=${CONTEST_AWS_ACCESS_KEY_ID}
export AWS_SECRET_ACCESS_KEY=${CONTEST_AWS_SECRET_ACCESS_KEY}
export S3_ENDPOINT='http://s3.mds.yandex.net'
# Пригодятся после https://st.yandex-team.ru/CONTEST-4965
# export S3_ASSET_BASE_URL='https://s3.mdst.yandex.net'

S3_ASSET_PATH="contest/static/contest-admin/${APP_VERSION}"
# Пригодятся после https://st.yandex-team.ru/CONTEST-4965
# S3_ASSET_URL="${S3_ASSET_BASE_URL}/${S3_ASSET_PATH}"
IMAGE_TAG=registry.yandex.net/contest/contest-admin:${APP_VERSION}

apt-get update && \
  apt-get install -y \
  awscli

# for debug
. ${BASEDIR}/printEnv.sh

. ${BASEDIR}/build.sh
. ${BASEDIR}/publishStatic.sh
. ${BASEDIR}/publishDocker.sh

YENV=production npx --package "@yandex-int/frontend.ci.deploy" frontend-deploy deploy -c ${BASEDIR}/../../.config/deploy/config.testing.yml
