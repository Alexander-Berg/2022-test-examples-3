#!/bin/sh
SERVANT=super-controller
log_rc=$SERVANT.log

. super-controller-debug.conf

$exec >> $log_rc 2>&1 &

echo $! > $SERVANT.pid 
 