#!/bin/sh
set -e
LOG_FOLDER=/var/log/fake-services-for-loadtest
mkdir -m0755 -p $LOG_FOLDER
chown root:root $LOG_FOLDER
exec svlogd -tt $LOG_FOLDER
