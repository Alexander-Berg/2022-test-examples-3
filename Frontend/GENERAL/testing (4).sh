#!/usr/bin/env bash

BASEDIR=$(dirname $0)

export APP_VERSION=$(date +"%Y-%m-%d_%H-%M-%S")

. ${BASEDIR}/build.sh
. ${BASEDIR}/publishStatic.sh
. ${BASEDIR}/publishDocker.sh

static_version=$1;
echo "APP_VERSION: ${APP_VERSION}"

YENV=production npx --package "@yandex-int/frontend.ci.deploy" frontend-deploy deploy -c ${BASEDIR}/../../.config/deploy/config.testing.yml
