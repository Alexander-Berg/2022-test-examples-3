#!/bin/bash

set -eu

#usage="$0 TMP_DIR"

#if [ $# -le 0 ]; then
#    echo "${usage}"
#    exit 1
#fi

#tmp_path=$1

# hardcode path
tmp_path=/u0/nginx/tmp

if [ ! -d ${tmp_path} ] ; then
    echo "Directory '${tmp_path}' does not exist"
    exit 2
fi

u0_usage=`df -h ${tmp_path} | tail -n 1 | awk '{print $(NF-1)}' | sed 's/%//g'`

if [ "${u0_usage}" -lt 90 ] ;then
    clean_time=720
elif [ "${u0_usage}" -lt 93 ] ; then
    clean_time=180
elif [ "${u0_usage}" -lt 95 ] ; then
    clean_time=90
else 
    clean_time=30
fi

find ${tmp_path}/ -maxdepth 1 -type f -mmin +${clean_time} -delete 2>&1 >/dev/null

exit 0

