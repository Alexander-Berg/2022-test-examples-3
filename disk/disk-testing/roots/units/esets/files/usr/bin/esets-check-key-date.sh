#!/bin/bash


time_left_crit=$(( 24 * 86400 )) # 24 days

rcode=0
msg=""

for lic in /etc/opt/eset/esets/license/*.lic ; do
    fname=$(basename $lic)

    date_av_hex=$(sed '8!d' $lic | grep EXPIRE | cut -d'"' -f6)
    date_av_ts=$(echo "ibase=16; ${date_av_hex}" | bc )

    date_today=$(date +%s)
    time_left=$(( $date_av_ts - $date_today ))

    if [ $time_left -lt $time_left_crit ]; then
        time_left_days=$(( $time_left / 86400 ))
        msg="${msg}$fname: $time_left_days days left; "
        rcode=2
    fi


done



if [ $rcode -eq 0 ]; then
    echo "0; OK"
else
    echo "$rcode; $msg"
fi




