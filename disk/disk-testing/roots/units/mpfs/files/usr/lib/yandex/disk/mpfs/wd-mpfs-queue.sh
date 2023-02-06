#!/bin/bash

restart_queue() {
    queue_conf=$1
    queue_init=$2
    service mpfs-$queue_init stop > /dev/null 2>&1
    sleep 5
    pkill -f "queueworker.py.*$queue_conf"
    sleep 5
    pkill -9 -f "queueworker.py.*$queue_conf"
    service mpfs-$queue_init start > /dev/null 2>&1
    logger -p daemon.err "mpfs queue $queue_conf restarted"
}


stop_orphans() {
    queue_conf=$1
    queue_init=$2
    pid_path="/var/run/mpfs/${queue_init}.pid"
    if [ ! -r ${pid_path} ]; then
        return
    fi
    current_ppid=$(cat ${pid_path})

#    echo "queue_conf: $queue_conf, queue_init: $queue_init, pid_path: $pid_path, current_ppid: $current_ppid"
#    return
    pgrep -f "queueworker.py.*$queue_conf" | grep -v "${current_ppid}" | while read pid ; do
        ppid=$(cut -d' ' -f 4 /proc/${pid}/stat)
        if [[ ${ppid} -eq 1 ]]; then
            kill $pid
            sleep 3
            kill -9 $pid
            logger -p daemon.err "mpfs queue $queue_conf: orphaned worker (pid=$pid with ppid=$ppid) was killed"
        fi
    done
}

queue=$1
[ -z $queue ] && exit 1
if [ $queue == "queue_default" ] || [ $queue == "queue_disk" ] ; then
    queue_conf="queue_default"
    queue_init="queue-disk"
    queue_stat="queue-default"
    queue_log="queue"
else
    queue_conf=$queue                        # queue_index
    queue_init=$(echo $queue | sed 's/_/-/') # queue-index
    queue_stat=$queue_init                   # queue-index
    queue_log=$queue_init                    # queue-index
fi

log_time=$2
[ -z $log_time ] && exit 1

limit=$[6*3600]

proc_count=$(pgrep -fc "queueworker.py.*$queue_conf")

worker_count=$(grep -A 5 "$queue_conf" /etc/yandex/mpfs/admins_overrides.yaml | awk '/general:/ {print $NF}')

if [ -z "$worker_count" ] ; then
    yandex_env=$(cat /etc/yandex/environment.type)
    worker_count=$(grep -A 15 "$queue_conf" /etc/yandex/mpfs/overrides/${yandex_env}.common.yaml  | awk '/general:/ {print $NF}')
fi

log_entries=$(timetail -t java -n $log_time /var/log/mpfs/${queue_log}.log | wc -l)
monitor_errors=$(/usr/bin/mymtail.sh /var/log/mpfs/${queue_log}-error.log ${queue}-wd | fgrep -c 'Monitor thread is dead:')
queue_count=$(curl -ks "http://localhost/queue/count?queue=$queue_stat" | grep -Po '\d+')
old_jobs_count=$(curl -s "http://localhost/queue/count?queue=$queue_stat&time_offset=$limit" | grep -Po '\d+')

#echo -e "$queue:\nqueue_conf=$queue_conf\nqueue_init=$queue_init\nqueue_stat=$queue_stat\nproc_count=$proc_count of worker_count=$worker_count\n\nqueue_count=$queue_count\nold_jobs_count=$old_jobs_count"

stop_orphans $queue_conf $queue_init

if [[ $monitor_errors -ne 0 || $proc_count -lt $worker_count || $log_entries -eq 0 && $queue_count -ne 0 ]] ; then
    logger -p daemon.err "mpfs queue $queue: $proc_count of $worker_count workers found or no logs for last $log_time seconds"
    restart_queue $queue_conf $queue_init
    exit 1

## CHEMODAN-28779
#elif [ $old_jobs_count -gt 0 ]; then
#    logger -p daemon.err "found $old_jobs_count old jobs, restarting mpfs queue $queue"
#    restart_queue $queue_conf $queue_init
#    exit 1
fi


### CHEMODAN-28907
# check zombie 
# find queue_worker Z
# find his ppid
# if ppid cmdline =~ current then restart

ps axwww -o pid,ppid,stat,cmd | awk '/queueworker.py/{if ($3 ~ "Z"){ print $1 }}' | while read z_pid; do
    z_ppid=`cat /proc/${z_pid}/status | awk '/PPid:/{print $2}'`
    if [ ! -f /proc/${z_ppid}/cmdline ] ; then
        continue
    fi
    grep -qP "queueworker.py.*${queue_conf}" /proc/${z_ppid}/cmdline
    
    if [ "$?" -eq "0" ] ; then
        logger -p daemon.err "mpfs queue $queue: at least one zombie proccess in workers found ($z_pid)"
        restart_queue $queue_conf $queue_init 
    fi
    
done






exit 0

