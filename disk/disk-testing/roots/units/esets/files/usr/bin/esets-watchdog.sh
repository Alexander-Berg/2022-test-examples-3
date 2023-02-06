#!/bin/bash


function restart_esets() {
#    /etc/init.d/esets stop >/dev/null 2>&1
#    sleep 1
#    killall -9 esets_daemon >/dev/null 2>&1
#    sleep 3
#    /etc/init.d/esets start >/dev/null
    /etc/init.d/esets restart >/dev/null 
}



esets_cmd="/usr/bin/esets-check-icap.py"
esets_check_file="/u0/av_tmp/test-av"

esets_check_out=`$esets_cmd 2>&1`
esets_check_rcode=$?

if [ $esets_check_rcode -ne 0 ] ; then
        restart_esets
        logger -p daemon.err "esets restarted: failed to check esets, check returned ${esets_check_rcode}. Output was: ${esets_check_out}."
        exit 1
fi

exit 0
