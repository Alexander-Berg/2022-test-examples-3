#!/bin/bash

timetail -n 60 /var/log/nginx/downloader/access.log | perl -ne 'BEGIN{ $total=0; $gt1s=0 } { if ($_ =~ / \"GET \/r?preview\/.*\[proxy \(MISS\) :.* (?:141.8.146.9|\[2a02:6b8:0:3400::2:9\]):80.* (\d+\.\d+) \d+ / ) { $total++; if ($1 > 1) {$gt1s++;} ;} END{ printf "downloader_access_preview_gt1s %d\n", $gt1s; printf "downloader_access_preview_total %d\n", $total }}'

