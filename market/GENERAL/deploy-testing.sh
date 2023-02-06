#!/bin/sh

. "$PROJECT_DIR/scripts/ci/prepare_nvm.sh"

cd "$PROJECT_DIR" || exit 1
nvm use

npm ci
npm run build:testing || exit 1
NODE_TLS_REJECT_UNAUTHORIZED='0' AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY AWS_SECRET_ACCESS_KEY=$AWS_SECRET_ACCESS_KEY NODE_YANDEX_CERT_PATH="/etc/ssl/certs/YandexInternalRootCA.pem" npm run deploy:s3:testing

