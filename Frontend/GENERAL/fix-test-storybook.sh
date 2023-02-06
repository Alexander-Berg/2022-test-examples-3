#!/bin/sh

test -d build/platform && rm -rf build/platform;
mkdir -p build/platform/applications/ecom-tap/;
cd build/platform/applications/ecom-tap/;
ln -s ../../../../.stories ./;
