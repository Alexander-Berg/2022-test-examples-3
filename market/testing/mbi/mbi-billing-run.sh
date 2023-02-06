#!/bin/bash

HOST=$1
PORT=$2
JOB=$3
COMMAND=$4


case "$COMMAND" in
    is_running)
        if ( echo get-currently-executing-jobs ; echo quit ; sleep 1 ) | nc "$HOST" "$PORT" | grep "$JOB\$" > /dev/null ; then
            exit 0
        else
            exit 1
        fi
        ;;
    wait)
        while "$0" "$1" "$2" "$3" is_running ; do
            printf "."
        done
        ;;
    run)
        echo "Running executer: $JOB"
        "$0" "$1" "$2" "$3" wait
        "$0" "$1" "$2" "$3" start
        "$0" "$1" "$2" "$3" wait
        echo
        ;;
    start)
        ( echo "tms-run $JOB" ; echo quit ; sleep 1 ) | nc "$HOST" "$PORT" > /dev/null
        ;;
    *)
        "$0" "$1" "$2" "$3" run
        ;;
esac

