#!/bin/bash

echo "Start deploy sitesearch-serp-test_production"

set -e

if [[ -z ${DOCKER_IMAGE_REPO} ]]; then
    DOCKER_IMAGE_REPO=registry.yandex.net/wmfront/sitesearch-serp-test
fi

export DOCKER_IMAGE_TAG=$(date +%Y-%m-%d_%H-%M)
export BETA_SLUG="pr-${TRENDBOX_PULL_REQUEST_NUMBER}"

set -o nounset

npm install

docker login -u ${DOCKER_USERNAME} -p ${DOCKER_OAUTH_TOKEN} registry.yandex.net

if [[ "$YENV" = "production" ]]; then
    docker build -t ${DOCKER_IMAGE_REPO}:${DOCKER_IMAGE_TAG} .
else
    docker build --build-arg PR_NUMBER=${TRENDBOX_PULL_REQUEST_NUMBER} -t ${DOCKER_IMAGE_REPO}:${DOCKER_IMAGE_TAG} .
fi

docker push ${DOCKER_IMAGE_REPO}:${DOCKER_IMAGE_TAG}
echo

if [[ "$YENV" = "production" ]]; then
    echo "Start deploy to production stage sitesearch-serp-test_production"
    npm run deploy:production
else
    echo "Start deploy to beta stage sitesearch-serp-test_testing-beta-${BETA_SLUG}"
    npm run deploy:beta
fi
