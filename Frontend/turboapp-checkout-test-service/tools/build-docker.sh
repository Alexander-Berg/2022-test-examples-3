#!/bin/bash
set -e

REGISTRY="registry.yandex.net"
IMAGE="tap/turboapp-checkout-test-service-ws"
[ $(git rev-parse --is-inside-git-dir) ] && VERSION="`git rev-list -n1 HEAD`" || VERSION="`arc log --max-count 1 --format '{commit}'`"
REPO="${REGISTRY}/${IMAGE}"

echo "Authenticating in docker registry"
docker login -u robot-tap -p $TAP_ROBOT_REGISTRY_TOKEN $REGISTRY

echo "Building ${REPO}:${VERSION}"
ID=$(docker build --build-arg APP_VERSION=${VERSION} -q .)

echo "Pushing ${REPO}:${VERSION}"
docker tag ${ID} ${REPO}:${VERSION}
docker push ${REPO}:${VERSION}
docker rmi ${ID} -f
