#!/bin/bash

[[ "$TRACE" ]] && set -x
set -eEo pipefail

function run_tests {
    pushd ./tests-system

    export PYTHONPATH="$PYTHONPATH:../../core:./certificates"
    export PYTHONDONTWRITEBYTECODE=1

    if (( $# > 0 )); then
        ARGS=${@}
    else
        ARGS="-x -v extapi/*.py api1/*.py api2/*.py general/*.py webui/*.py service_manager/*.py apns_queue/*.py webpushapi/*.py idm/*.py"
    fi

    nosetests $ARGS
    popd
}

source "../core/pycommon/exec_tools.sh"
wait_for_ping "localhost:18083"

run_tests ${@:1}
