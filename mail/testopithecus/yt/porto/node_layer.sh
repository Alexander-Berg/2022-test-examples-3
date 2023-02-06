#!/bin/bash

set -e

# INSTALL CURL
apt-get update
apt-get install -y curl

# INSTALL NODE
curl -sL https://deb.nodesource.com/setup_12.x | bash -
apt-get -y update
apt-get -y install nodejs

# CONFIGURE NPM
npm i fs-extra node-gyp @types/node tslint typescript typescript-formatter sync-request ts-node -g

#curl -sS https://dl.yarnpkg.com/debian/pubkey.gpg | sudo apt-key add
