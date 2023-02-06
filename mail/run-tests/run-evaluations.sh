#!/bin/bash

unzip -qq testopithecus -d testopithecus
cd testopithecus

mocha --max-old-space-size=4096 --require ts-node/register --reporter mocha-silent-reporter ./common/__tests__/run-evaluations.ts

