#!/bin/bash

set -u

if [ $# -eq 3 ]; then
    log_file=$1
    check_time=$2
    err_crit=$3
else
    echo "USAGE: $0 LOGFILE CHECKTIME ERRCOUNT"
    exit 2;
fi

if [ ! -r ${log_file} ]; then
    echo "0; Can not open log file for read."
    exit 0;
fi

count_err=`timetail -j 10000 -t java -n ${check_time} ${log_file} | grep 'the connected server supports BSON document sizes up to 0 bytes' | wc -l`

if [ ${count_err} -gt ${err_crit} ]; then
    echo "2; Found ${count_err} errors in the last ${check_time} seconds."
    exit 2;
else
    echo "0; Errors were found in the last ${check_time} seconds: ${count_err}"
    exit 0
fi


exit 0

