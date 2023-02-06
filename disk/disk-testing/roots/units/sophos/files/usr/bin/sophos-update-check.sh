#!/bin/sh

set -eu

LOG=$1

LS_SIZE=`cat ${LOG} | wc -c`

if [ ${LS_SIZE} -gt 1 ] ; then
    echo "2; Something in log file (${LOG})."
    exit 0
fi

echo "0; OK"
exit 0

