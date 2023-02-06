#!/bin/bash

set -e
mkdir yt_delta

# INSTALL CURL
apt-get update
apt-get install -y curl

# INSTALL NODE
curl -sL https://deb.nodesource.com/setup_12.x | bash -
apt-get -y update
apt-get -y install nodejs
