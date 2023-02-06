#!/bin/bash

proc_count=`pgrep -l -f 'queueworker.py.*queue-default' | wc -l`
worker_count=`awk '/\[workers_number\]/ {section = 1} /general/ { if(section) { print $3 }}' /etc/mpfs/queue-default.conf.$(cat /etc/yandex/environment.type)`
log_entries=`timetail -t java -n 300 /var/log/mpfs/queue.log | wc -l`
monitor_errors=`/usr/bin/mymtail.sh /var/log/mpfs/queue-error.log queue-wd | fgrep -c 'Monitor thread is dead:'`
queue_count=`curl -ks "http://localhost/queue/count"`

if [[ $monitor_errors -ne 0 || $proc_count -lt $worker_count || $log_entries -eq 0 && $queue_count -ne 0 ]] ; then
    logger -p daemon.err "mpfs.queue: $proc_count of $worker_count workers found, no logs for last 5 minutes"
    service mpfs-queue stop > /dev/null 2>&1
    sleep 5
    pkill -f 'queueworker.py.*queue-default'
    service mpfs-queue start > /dev/null 2>&1
    logger -p daemon.err "mpfs.queue restarted"
    exit 1
fi

exit 0
