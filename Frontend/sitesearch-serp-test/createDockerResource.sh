#!/bin/bash

PACKAGE_DIR=$(pwd)

# передаем именно этот путь "$PACKAGE_DIR/../../../dockerfile.tar" см. https://st.yandex-team.ru/INFRADUTY-11419
sandboxResource=$(node node_modules/.bin/sandbox upload-resource "$PACKAGE_DIR/../../../dockerfile.tar" --type=DOCKER_CONTENT_RESOURSE --owner=FRONTEND)

echo "${sandboxResource}" >&2
echo "${sandboxResource}" | jq '.id'
