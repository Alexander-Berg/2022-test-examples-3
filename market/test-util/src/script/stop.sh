#!/bin/sh

. ./config.sh

echo 'Stopping program: '
echo $APP
kill -9 `cat /var/run/checkout/${APP}.pid`
echo 'Program was stopped'
