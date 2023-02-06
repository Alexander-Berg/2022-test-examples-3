#!/bin/sh

# CHEMODAN-29263

function check_limit {
    count=$1
    limit=$2
    count_current=$(curl -s "http://localhost/queue/count?time_offset=$limit" | grep -Po '\d+')
    if [ -z $count_current ] ; then
        echo "cannot get queue size"
    elif [ "$count_current" -gt "$count" ]; then
        echo "jobs older $limit seconds: $count_current"
    fi
}

if [ "$#" -ne 4 ]; then
    echo "USAGE: $0 low_count low_age high_count high_age"
    exit 1
fi

h_count=$1
h_age=$2

l_count=$3
l_age=$4


result="$(check_limit $h_count $h_age); $(check_limit $l_count $l_age)"

if [ "$result" != "; " ] ; then
    echo "2; $result"
else
    echo "0; Ok"
fi


