#!/bin/bash

if [ -d /var/log/disk-logs/ ] ; then
    logs=$(ls -1 /var/log/disk-logs | wc -l)
    if [ $logs -eq 0 ] ; then
        echo "0;Ok, all logs was uploaded"
    else 
        if flock -n /var/run/disk-logbackup.lock true ; then
            echo "2;Failed, there are $logs logs waiting for upload"
        else
            echo "0;Logs are uploading"
        fi
    fi
else
    echo "2;No /var/log/disk-logs dir"
fi
exit 0
