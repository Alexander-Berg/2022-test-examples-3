#!/bin/bash -e

npm run compile

docker build --build-arg NODEJS=10-xenial --network=host -t docker-test-10 --pull .
docker build --build-arg NODEJS=12-xenial --network=host -t docker-test-12 --pull .
docker build --build-arg NODEJS=14-bionic --network=host -t docker-test-14 --pull .

docker run --rm docker-test-10
docker run --rm docker-test-12
docker run --rm docker-test-14
