#!/bin/bash

unzip -qq testopithecus -d testopithecus
cd testopithecus

mocha --require ts-node/register --R mocha-silent-reporter ./common/__tests__/simple.ts

