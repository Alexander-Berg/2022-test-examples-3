#!/bin/bash
#set -x
rsync_opts="--whole-file -av --sockopts=SO_SNDBUF=20000000,SO_RCVBUF=20000000 --delete-before"
# use the first parameter, or default if it's empty
source="${1:-rsync://fastbone.ppcbackup04i.yandex.ru/mongo-ro/moddb/persistent}"
basedir=/opt
user=mongodb

instances=""
for i in /opt/mongodb*; do instances="$instances $(basename $i)"; done
echo "Found instances: $instances"

for inst in $instances configsvr; do
	echo "Syncronizing from $source/$inst/ to $basedir/$inst"
	rsync $rsync_opts $source/$inst/ $basedir/$inst
	chown -R $user $basedir/$inst
done

