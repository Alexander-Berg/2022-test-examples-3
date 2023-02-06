#!/bin/bash


if [ $# -eq 0 ] ; then
    echo "$0 time_sec crit_count"
    exit 2
fi

log_time=$1
log_crit=$2

log_entries=$(timetail -t java -n ${log_time} /var/log/mpfs/error-tskv.log | grep 'Not primary while performing update' | wc -l)
if [[ $log_entries -gt $log_crit ]] ; then
    logger -p daemon.err "mpfs queues: found ${log_entries} 'Not primary while performing update' records"
    for queue in disk index minor photoslice ; do
        service mpfs-queue-${queue} restart >/dev/null
    done
    logger -p daemon.err "mpfs queues: found ${log_entries} 'Not primary while performing update' records, queues were restarted"
    exit 1
fi

exit 0

