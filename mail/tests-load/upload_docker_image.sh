#!/bin/bash

set -eo pipefail

VERSION=`date "+%Y-%m-%d-%H-%M"`;
BUILDNAME="botserver-mocks:r${VERSION}"
BUILDTAG=registry.yandex.net/mail/xiva/${BUILDNAME}
BUILD_DIR="$(mktemp -d -t botserver_docker_loadtest_build-XXXXXX)"
COMMON_DIR="../../rtec/tests-load-common"

echo "Building in $BUILD_DIR"
mkdir -p "$BUILD_DIR"
rm -rf "$BUILD_DIR/*"
cp -r "$COMMON_DIR"/* "$BUILD_DIR/"
cp -r "deploy"/* "$BUILD_DIR/"

docker build --pull --network=host \
    --build-arg deploydir="." \
    --tag ${BUILDTAG} -f Dockerfile "$BUILD_DIR"
docker push ${BUILDTAG}
