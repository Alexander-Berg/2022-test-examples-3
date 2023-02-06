#!/bin/bash

proc_count=`pgrep -l -f 'queueworker.py.*queue-index' | wc -l`
worker_count=`awk '/\[workers_number\]/ {section = 1} /general/ { if(section) { print $3 }}' /etc/mpfs/queue-index.conf.$(cat /etc/yandex/environment.type)`
log_entries=`timetail -t java -n 300 /var/log/mpfs/queue-index.log | wc -l`
queue_count=`curl -ks "http://localhost/queue/count?queue=queue-index"`

if [[ $proc_count -lt $worker_count || $log_entries -eq 0 && $queue_count -ne 0 ]] ; then
	logger -p daemon.err "mpfs.queue.index: $proc_count of $worker_count workers found, no logs for last 5 minutes"
	service mpfs-queue-index stop > /dev/null 2>&1
	sleep 5
	pkill -f 'queueworker.py.*queue-index'
	service mpfs-queue-index start > /dev/null 2>&1
	logger -p daemon.err "mpfs.queue.index restarted"
	exit 1
fi

exit 0

