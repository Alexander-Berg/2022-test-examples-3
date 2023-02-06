#!/usr/bin/env bash

rm -rf testopithecus-logging/common/code/mail/logging
rm -rf testopithecus-logging/common/ys/
mkdir -p testopithecus-logging/common/code/mail/logging
mkdir -p testopithecus-logging/common/ys/
cp -R ./../common/code/mail/logging ./testopithecus-logging/common/code/mail/
cp -R ./../common/ys/ ./testopithecus-logging/common/ys
cd testopithecus-logging
npm install
npm run prepare
