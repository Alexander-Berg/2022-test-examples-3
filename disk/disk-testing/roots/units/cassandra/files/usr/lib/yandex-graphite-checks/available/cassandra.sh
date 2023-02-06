#!/bin/bash

data_dir=`cat /etc/cassandra/cassandra.yaml | grep data_file_directories | cut -d':' -f 2 | sed 's/ //g'`
commitlog_dir=`cat /etc/cassandra/cassandra.yaml | grep commitlog_directory | cut -d':' -f 2 | sed 's/ //g'`
data_used=`du -s $data_dir | awk {' print $1*1024 '}`
data_avail=`df $data_dir | grep -v 'Filesystem' | awk 'BEGIN { OFMT = "%.0f" } END { print $4*1024 }'`
commitlog_used=`du -s $commitlog_dir | awk {' print $1*1024 '}`
commitlog_avail=`df $commitlog_dir | grep -v 'Filesystem' | awk 'BEGIN { OFMT = "%.0f" } END { print $4*1024 }'`

echo -e "data_used $data_used"
echo -e "data_avail $data_avail"
echo -e "commitlog_used $commitlog_used"
echo -e "commitlog_avail $commitlog_avail"

