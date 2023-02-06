#!/usr/bin/env bash
set -e
_dirname="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

source "${_dirname}/../common"

test_main() {
    msg "Running tests..."

    export NODE_PATH="/usr/lib/node_modules/"

    cd $repo_root
    npm run ci:jest
}

test_main
