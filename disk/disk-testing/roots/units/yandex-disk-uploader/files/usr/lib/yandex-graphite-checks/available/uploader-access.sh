#!/bin/sh

timetail -t java -n 60 /var/log/yandex/disk/uploader-access.log | /usr/bin/disk_uploader.uploader-access.py | grep -E '^uploader_count_'

#exit ${PIPESTATUS[0]}
exit $?

