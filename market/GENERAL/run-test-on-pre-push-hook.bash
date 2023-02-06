#!/usr/bin/env bash

diff_file_count=$#

if [[ $diff_file_count -eq 0 ]]; then
    exit 0
elif [[ $diff_file_count -gt 500 ]]; then
    BABEL_ENV=test $(npm bin --silent)/jest --maxWorkers=50%
    exit $?
else
    BABEL_ENV=test $(npm bin --silent)/jest --maxWorkers=50% --bail --findRelatedTests $@
    exit $?
fi
