#!/bin/bash

DAYS_DEPTH=${1:-30} # default = 30 days
COREDUMPS_PATH=$(sysctl -n kernel.core_pattern)
COREDUMPS_DIR=$(dirname $COREDUMPS_PATH)
COREDUMPS_MASK="$(basename ${COREDUMPS_PATH%%.*}).*"

find $COREDUMPS_DIR -name "$COREDUMPS_MASK" -mtime +$DAYS_DEPTH -delete
