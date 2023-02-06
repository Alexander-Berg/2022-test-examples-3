#!/usr/bin/env bash
set -ex

_dirname="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
commit=$(git rev-parse HEAD || arc rev-parse HEAD)

export NO_SKY="${CI}"

pushd "${_dirname}/.."

test_install() {
  rm -rf test
  ./bin/carrier.js --hash "${commit}"

  if [[ ! -f "test/success.txt" ]]; then
    exit 1
  fi
}

test_install
test_install

popd
