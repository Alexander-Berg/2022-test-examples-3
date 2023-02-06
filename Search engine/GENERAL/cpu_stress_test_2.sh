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
    guest_resources/cppload &
    sleep 1
done

