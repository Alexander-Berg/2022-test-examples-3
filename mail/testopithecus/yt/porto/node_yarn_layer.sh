#!/bin/bash

set -e

# INSTALL CURL
apt-get update
apt-get install -y curl

# INSTALL NODE
curl -sL https://deb.nodesource.com/setup_12.x | bash -
apt-get -y update
apt-get -y install nodejs

# CONFIGURE PACKAGES
npm i ts-node@^8.5.4 typescript@^3.7.3 @types/node@^12.12.21 -g
