#!/bin/bash

CLASSPATH=`find /usr/lib/yandex/deb-plugin-sub-project/ -name '*.jar' -printf '%p\n' | sort -r | tr '\n' ':'`

java -classpath $CLASSPATH \
     -Djava.net.preferIPv6Addresses=true \
     -Dsun.net.inetaddr.ttl=60 \
     -Dsun.net.inetaddr.negative.ttl=0 \
     -showversion -server \
      -Xss2m -XX:PermSize=128m -XX:MaxPermSize=128m\
     -verbose:gc -XX:+PrintGC -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps \
     -XX:+UseCompressedOops -XX:AutoBoxCacheMax=10000 \
     -XX:+HeapDumpOnOutOfMemoryError \
     -Dhost.name=`/bin/hostname` \
     -Dbean.file=logshatter.xml -Dmodule.properties.file=logshatter.properties \
     gradle_multi.deb_plugin.Main