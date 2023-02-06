#!/bin/sh


timetail -t java -n 60 /var/log/yandex/disk/uploader-events.log | /usr/bin/disk_uploader.uploader-events.py | grep -E '^uploader_count_'

#exit ${PIPESTATUS[0]}
exit $?

