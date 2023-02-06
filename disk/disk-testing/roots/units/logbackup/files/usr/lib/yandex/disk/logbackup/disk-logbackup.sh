#!/bin/bash

if [ $# -eq 0 ] 
then
    OPT_COLLECT=1
    OPT_SEND=1
    OPT_SLEEP=1
else 
    while [ $# -gt 0 ] ; do
        case "$1" in
            -c)
                OPT_COLLECT=1
                OPT_SLEEP=0
                shift
                ;;
            -s)
                OPT_SEND=1
                OPT_SLEEP=0
                shift
                ;;
            -a)
                OPT_COLLECT=1
                OPT_SEND=1
                shift
                ;;
            *)
                echo "USAGE: $0 [-c] [-s] [-a]"
                exit 1
                ;; 
        esac
    done
fi


hostname=`hostname -s` ;
fullhostname=`hostname -f`
hosttype=$(curl -ks http://c.yandex-team.ru/api-cached/hosts/${fullhostname}?format=yaml | awk '/group: / {print $NF}' | tr -d ' ' | sed -e "s/disk_//g" | sed -e "s/mail_//g")

if [[ $hosttype =~ downloader ]] ; then
    # disk_downloader_central, disk_downlodaer_regional* goes to disk_downloader
    hosttype=downloader
fi

if [ "$hosttype" = "No hosts found" ] ; then
    echo "Can't detect hosttype"
    exit 1
elif [ x"$hosttype" = "x" ] ; then
    echo "Empty hosttype"
    exit 1
fi

date=`/bin/date --date=yesterday +%Y%m%d` ;
logspooldir="/var/log/disk-logs" ;
logstorehost="logstore.disk.yandex.net";

logs=`find /var/log/ -type f -name '*log' | grep -vf /etc/disk-logbackup.blacklist`
logs+=('/var/log/messages')

# Check, if spooldir exists
if [ ! -d $logspooldir ] ; then
    echo "$logspooldir not found" ; 
    exit -1 ; 
fi

if [ "${OPT_COLLECT}" == "1" ] ; then
    # Prepare log for upload
    for log in ${logs[@]} ; do
        logname=`ls -1 $log |awk -F "/var/log/" '{print $2}' |sed -e "s/\//\_/g" ` ;
        logdest="$logspooldir/$logname.$date.gz"
        if [ -f $log.0 ] ; then
            gzip -c "$log.0" > "$logdest"
        elif [ -f $log.0.gz ] ; then
            cp "$log.0.gz" "$logdest"
        elif [ -f $log.1 ] ; then
            gzip -c "$log.1" > "$logdest"
        elif [ -f $log.1.gz ] ; then
            cp "$log.1.gz" "$logdest"
        elif ls -1 $log-${date}-*.gz > /dev/null 2>&1 ; then
            cat $log-${date}-*.gz > "$logdest"
        fi
    done
fi

if [ "${OPT_SLEEP}" == "1" ] ; then
    # Sleep for async
    sleep $((RANDOM%7200+90))
fi

if [ "${OPT_SEND}" == "1" ] ; then
    # Upload the staff
    for try in 1 2 3; do 
        /usr/bin/rsync --password-file=/usr/lib/yandex/disk/logbackup/rsync.password -av $logspooldir/ rsync://disk@$logstorehost/backup/$hosttype/$hostname/
        if [ $? -ne 0 ] ; then
            echo "Failed to rsync, try #${try}"
        else
            echo "Rsync ok, removing tmp files"
            rm -f $logspooldir/*
            exit 0
        fi
    done
fi
echo "Failed to rsync data!" 1>&2
exit 1

