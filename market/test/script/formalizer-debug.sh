#!/bin/sh
SERVANT=formalizer
log_rc=$SERVANT.log

. $SERVANT-debug.conf

$exec $* >> $log_rc 2>&1 &

echo $! > $SERVANT.pid 

 