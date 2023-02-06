#!/usr/bin/env bash
# Provides: dummy_check

NAME=dummy_check

send () {
    local status=$1
    local description="$2"

    echo "PASSIVE-CHECK:$NAME;$1;$2"
    exit 0
}

send 0 "test check"
