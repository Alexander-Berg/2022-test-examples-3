#!/bin/bash

. shared.sh

ya_log "stopping kiwi"
if [ -f /usr/local/bin/sadm ]; then 
    ya_log "stopping kiwi..."
    sleep 30
    /usr/local/bin/sadm stop
    ya_log "kiwi was probably stopped"
else
    ya_log "kiwi was not stopped, exiting"
    exit
fi

total_ncpu=`grep -c ^processor /proc/cpuinfo`
for i in $(seq 0 $total_ncpu); do
   echo -e "arr = [0]*(1000000000 / 21)\nfor k in xrange(len(arr)): arr[k] = k % 2\ns = 0\nwhile True:\n\tfor x in xrange(len(arr)):\n\t\ts += arr[x]\n\t\tarr[x] = 1 - arr[x]" | python &
done

