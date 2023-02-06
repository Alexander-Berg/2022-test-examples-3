#!/bin/sh
find . -name '*.go' -exec ya tool go fmt '{}' \;
