#!/usr/bin/env sh

# Скрипт, который собирает проект, заливает статику в S3 и Docker образ в Registry

set -e

VERSION=${npm_package_version}

DOCKER_USERNAME='robot-frontend'
DOCKER_PASSWORD=${DOCKER_OUATH_TOKEN}
REGISTRY='registry.yandex.net'
PREFIX='mathcongress'
IMAGE='admin'

S3_PROJECT='office';
S3_KEY_ID="${MATHCONGRESS_AWS_ACCESS_KEY_ID}"
S3_ACCESS_KEY="${MATHCONGRESS_AWS_SECRET_ACCESS_KEY}"
S3_ASSET_PATH="${PREFIX}/${S3_PROJECT}/${VERSION}"
S3_ENDPOINT="https://s3.mds.yandex.net"

ASSET_PREFIX="https://yastatic.net/s3/mathcongress/office/${VERSION}"

docker login -u ${DOCKER_USERNAME} -p ${DOCKER_PASSWORD} registry.yandex.net

docker build . \
  --build-arg APP_VERSION=${VERSION} \
  --build-arg S3_ASSET_PATH=${S3_ASSET_PATH} \
  --build-arg AWS_ACCESS_KEY_ID=${S3_KEY_ID} \
  --build-arg AWS_SECRET_ACCESS_KEY=${S3_ACCESS_KEY} \
  --build-arg S3_ENDPOINT=${S3_ENDPOINT} \
  --tag ${REGISTRY}/${PREFIX}/${IMAGE}:${VERSION}

docker push ${REGISTRY}/${PREFIX}/${IMAGE}:${VERSION}

export DOCKER_IMAGE_TAG=${VERSION}
export YENV=production
