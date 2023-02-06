#!/usr/bin/env bash

set -e

_dirname="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${_dirname}/common"

rm -rf "${project_root}/test/results/"*

pushd "${project_root}/test/files"
npm install
popd
