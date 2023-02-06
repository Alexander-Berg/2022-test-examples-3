#!/bin/bash

log_size=`stat --printf="%s\n" /var/log/mpfs/requests-tskv.log`

if [ ${log_size} -lt 2684354560 ]; then
    exit 0
fi

# read pids from log
pids_active=`timetail -t java -n 300 /var/log/mpfs/requests-tskv.log | grep -oP '\tpid=\K\d+' | sort | uniq`
pids_active_count=`echo ${pids_active} | tr ' ' '\n' | wc -l`

# collect pids from ps
pids_all=`pgrep -u nginx queue2.py`
pids_all_count=`echo ${pids_all} | tr ' ' '\n' | wc -l`

# diff
pids_smoking=`echo $pids_active $pids_all | tr ' ' '\n' | sort | uniq -u`
pids_smoking_count=`echo ${pids_smoking} | tr ' ' '\n' | wc -l`

# kill
echo ${pids_smoking} | tr ' ' '\n' | while read qpid ; do 
#    ps -p $qpid -o pid,comm=
    kill -SIGTERM $qpid
#    echo $qpid
done


logger -p daemon.err "wd-mpfs-queue2: total pids were found: ${pids_all_count}, active: ${pids_active_count}, killed: $pids_smoking_count"



