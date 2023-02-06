#!/bin/bash

#
# 1. check port
# 2. check pidfile exists
# 3. check pidfile's pid exists
# 4. check pid is drweb
# 5. check memory feeding


function restart_drwebd() {
    /etc/init.d/drwebd stop >/dev/null 2>&1
    killall -9 drwebd.real >/dev/null 2>&1
    sleep 3
    /etc/init.d/drwebd start >/dev/null
}

PIDFILE='/var/drweb/run/drwebd.pid'

nc -z -w 2 localhost 3000 >/dev/null 2>&1
if [ $? != "0" ] ; then 
    restart_drwebd
    logger -p daemon.err "drweb restarted: connect to 3000 port failed"
    exit 1
fi

if [ ! -r "${PIDFILE}" ] ; then
    restart_drwebd
    logger -p daemon.err "drweb restarted: pid file not found (${PIDFILE})"
    exit 1
fi

PID=`head -n 1 ${PIDFILE}`
CMDLINE="/proc/${PID}/cmdline"
if [ ! -r "${CMDLINE}" ] ; then
    restart_drwebd
    logger -p daemon.err "drweb restarted: main proccess not found (${PID})"
    exit 1
fi

grep -q 'drweb' ${CMDLINE}
if [ $? != "0" ] ; then
    restart_drwebd
    logger -p daemon.err "drweb restarted: main proccess does not look like drweb (${PID})"
    exit 1
fi

VMEM_LIMIT=1000000
PID=`pidof /opt/drweb/drwebd 2>/dev/null`
if [ "$PID" != "" ] ; then
    for i in $PID ; do
        if [ -f /proc/$i/status ] ; then
            VMEM_USED=`grep VmRSS /proc/$i/status | awk '{print $2}'`
            if [ $VMEM_USED -ge $VMEM_LIMIT ]; then
                restart_drwebd
                logger -p daemon.err "drweb restarted: memory feeding (at least one worker allocated $VMEM_USED kB)"
                exit 1
            fi
        fi
    done
fi

DRWEB_CMD="/opt/drweb/drwebdc -nlocalhost -p3000 -t10"
DRWEB_CHECK_OUTPUT=`${DRWEB_CMD} $0 2>&1`
DRWEB_CHECK_CODE=$?
if [ ${DRWEB_CHECK_CODE} != "0" ] ; then
    restart_drwebd
    logger -p daemon.err "drweb restarted: failed to check file, check $0 returned ${DRWEB_CHECK_CODE}. Output was: ${DRWEB_CHECK_OUTPUT}."
    exit 1
fi

exit 0

