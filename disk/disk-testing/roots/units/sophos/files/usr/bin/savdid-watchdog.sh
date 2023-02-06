#!/bin/bash

#
# 1. check port
# 2. check pidfile exists
# 3. check pidfile's pid exists
# 4. check pid is savdid
# 5. check memory feeding


#set -x

function restart_savdid() {
#    /etc/init.d/savdid stop >/dev/null 2>&1
#    sleep 1
#    killall -9 savdid >/dev/null 2>&1
#    sleep 3
#    /etc/init.d/savdid start >/dev/null
    /etc/init.d/savdid restart >/dev/null
}

PIDFILE='/var/run/savdid.pid'


echo | nc -w 5 -q 0 127.0.0.1 4010 | grep -q "OK SSSP" >/dev/null 2>&1
if [ $? != "0" ] ; then 
    restart_savdid
    logger -p daemon.err "savdid restarted: cannot communicate with daemon"
    exit 1
fi

if [ ! -r "${PIDFILE}" ] ; then
    restart_savdid
    logger -p daemon.err "savdid restarted: pid file not found (${PIDFILE})"
    exit 1
fi

PID=`head -n 1 ${PIDFILE}`
CMDLINE="/proc/${PID}/cmdline"
if [ ! -r "${CMDLINE}" ] ; then
    restart_savdid
    logger -p daemon.err "savdid restarted: proccess with pid ${PID} not found"
    exit 1
fi

grep -q 'savdid' ${CMDLINE}
if [ $? != "0" ] ; then
    restart_savdid
    logger -p daemon.err "savdid restarted: proccess with pid ${PID} does not look like savdid (${PID})"
    exit 1
fi

VMEM_LIMIT=1500000
PID=`pidof savdid 2>/dev/null`
if [ "$PID" != "" ] ; then
    for i in $PID ; do
        if [ -f /proc/$i/status ] ; then
            VMEM_USED=`grep VmRSS /proc/$i/status | awk '{print $2}'`
            if [ $VMEM_USED -ge $VMEM_LIMIT ]; then
                restart_savdid
                logger -p daemon.err "savdid restarted: memory feeding (allocated $VMEM_USED kB)"
                exit 1
            fi
        fi
    done
fi

SOPHOS_CMD="/usr/bin/sophos-check-fname.py"
SOPHOS_CHECK_FILE="/u0/savdi_tmp/test-av"
SOPHOS_CHECK_OUTPUT=`${SOPHOS_CMD} /u0/savdi_tmp/test-av 2>&1`
SOPHOS_CHECK_CODE=$?
if [ -r ${SOPHOS_CHECK_FILE} ] ; then
    if [ ${SOPHOS_CHECK_CODE} != "0" ] ; then
        restart_savdid
        logger -p daemon.err "savdid restarted: failed to check ${SOPHOS_CHECK_FILE}, check returned ${SOPHOS_CHECK_CODE}. Output was: ${SOPHOS_CHECK_OUTPUT}."
        exit 1
    fi
fi

exit 0

