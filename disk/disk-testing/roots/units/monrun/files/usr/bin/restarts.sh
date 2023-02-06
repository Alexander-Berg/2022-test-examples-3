#!/bin/sh
#
# Provides: restarts


me=${0##*/}     # strip path
me=${me%.*}     # strip extension
BASE=$HOME/agents
TMP=$BASE/tmp
LOG=/var/log/daemon.log

die () {
        echo "PASSIVE-CHECK:$me;$1;$2"
        exit 0
}


if [ -f ${LOG} ]; then
        count=$(/usr/bin/timetail -t syslog $LOG | fgrep "logger:"| fgrep -c 'restarted')

        if [ $count -gt 3 ]; then
                die 2 " $count rows "
        else
                die 0 "OK! $count rows "
        fi
else
        die 2 " File not exists "
fi
