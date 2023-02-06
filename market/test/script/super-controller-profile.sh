#!/bin/sh
SERVANT=super-controller
log_rc=$SERVANT.log

. super-controller-profile.conf

$exec >> $log_rc 2>&1 &

echo $! > $SERVANT.pid 
 