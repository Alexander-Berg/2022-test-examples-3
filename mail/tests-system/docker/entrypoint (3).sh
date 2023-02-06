#!/bin/bash

set -eo pipefail
set -ux

bash /usr/bin/run_app.sh &

if [[ -z "$@" ]]; then
    sleep inf
else
    "$@"
fi
