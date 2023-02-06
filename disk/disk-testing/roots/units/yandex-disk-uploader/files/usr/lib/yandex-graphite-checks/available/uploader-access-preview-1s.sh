#!/bin/bash

timetail -t java -n 60 /var/log/yandex/disk/uploader-access.log | grep '\"GET /generate-preview' | awk 'BEGIN{ total=0; gt1s=0; } { total++; if ($NF > 1) {gt1s++} } END{ printf "uploader_access_preview_gt1s %d\n", gt1s; printf "uploader_access_preview_total %d\n", total; }'

