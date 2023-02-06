#!/usr/bin/env bash

set -ex

repos="config-market-dist-stable config-market-dist-market-stable"

apt-get -qq update && apt-get -y install $repos

NODEJS_VERSION="16.14.0"
NPM_VERSION="6.14.16"

NODEJS_BASE_DIR=/usr/local/lib/nodejs
NODEJS_VERSION_ARCH=node-v$NODEJS_VERSION-linux-x64
NODEJS_BIN=$NODEJS_BASE_DIR/$NODEJS_VERSION_ARCH/bin

mkdir -p $NODEJS_BASE_DIR
curl -sS https://nodejs.org/dist/v$NODEJS_VERSION/$NODEJS_VERSION_ARCH.tar.xz | tar -xJv -C $NODEJS_BASE_DIR

ln -s $NODEJS_BIN/node /usr/bin/node
ln -s $NODEJS_BIN/npm /usr/bin/npm
ln -s $NODEJS_BIN/npx /usr/bin/npx

npm --global config set registry=https://npm.yandex-team.ru/
npm --global config set user root

npm install -g npm@$NPM_VERSION

echo 'Versions:'
node --version
npm --version

export PATH="/usr/bin/node:$PATH"
