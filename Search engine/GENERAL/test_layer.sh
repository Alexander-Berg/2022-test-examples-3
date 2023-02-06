#!/usr/bin/env bash

set -xeuo pipefail

apt-get update

python3 -V

apt-get install -y python3-pip build-essential libssl-dev libffi-dev python-dev

pip3 install beautifulsoup4

apt-get clean

rm /var/lib/apt/lists/* -rf
