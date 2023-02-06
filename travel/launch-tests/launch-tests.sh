#!/usr/bin/env bash

# shellcheck source=/dev/null
source "$ARCADIA_PATH/$TRAVEL_CI_PATH/registry/init.sh"

_tsPrepare

SCRIPT_DIR="$( cd -- "$( dirname -- "${BASH_SOURCE[0]:-$0}"; )" &> /dev/null && pwd 2> /dev/null; )";
result=$(ts-node "$SCRIPT_DIR/launch-tests.ts")

# shellcheck source=/dev/null
source "$ARCADIA_PATH/$TRAVEL_CI_PATH/registry/exit.sh" "${result}"
