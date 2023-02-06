#!/bin/bash

if [ $# -eq 0 ] ; then
    echo "$0 time_sec crit_count"
    exit 
fi

log_time=$1
log_crit=$2

log_entries=$(timetail -t java -n ${log_time} /var/log/mpfs/error-tskv.log | grep 'Not primary while performing update' | wc -l)

if [[ $log_entries -gt $log_crit ]] ; then
    echo "2; ${log_entries} records were found"
else
    echo "0; OK"
fi


exit 0

