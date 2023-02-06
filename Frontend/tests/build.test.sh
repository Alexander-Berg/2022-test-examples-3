#!/usr/bin/env bash

rm -rf ./proj && mkdir proj && cd proj && node ../tests/runGenerator.js && npm run lint && npm run build && cd ../
