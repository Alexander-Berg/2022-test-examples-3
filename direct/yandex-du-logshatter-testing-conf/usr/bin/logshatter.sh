#!/bin/sh
SERVANT=logshatter
USER=logshatter
LOG_RC=/var/log/$USER/$SERVANT.log.shell
LOG_GC=/var/log/$USER/$SERVANT.log.gc

JAVA_HOME=/usr/local/java8
PATH="$JAVA_HOME/bin:$PATH"


CLASSPATH=`find /usr/lib/yandex/logshatter/ -name '*.jar' -printf '%p\n' | sort -r | tr '\n' ':'`

echo $SERVANT classpath is $CLASSPATH >> $LOG_RC

export PATH LANG JAVA_HOME CLASSPATH

cd /etc/yandex/$SERVANT

echo Started at `date` >> $LOG_RC


if [ -e /etc/yandex/environment.type ] ; then
    ENV_TYPE=$(cat /etc/yandex/environment.type)
    echo environment.type is $ENV_TYPE >> $LOG_RC
else
    echo Error: /etc/yandex/environment.type does not exists >> $LOG_RC
    exit 1
fi

source /etc/yandex/ya-gde/info.sh

if [ -f /etc/ppcinv/localhost.root_dc ] ; then
   SRV_DC=$(cat /etc/ppcinv/localhost.root_dc | tr '[:lower:]' '[:upper:]')
   echo datacenter is $SRV_DC >> $LOG_RC
fi

[ $SRV_DC == "SAS" ] && SRV_DC="MYT"

[ -z "$SRV_DC" ] && { echo "Need to set SRV_DC"; exit 1; }

case $ENV_TYPE in
production)
    MAX_HEAP_SIZE=31g
;;
prestable)
    MAX_HEAP_SIZE=64g
;;
testing)
    MAX_HEAP_SIZE=31g
;;
*)
    MAX_HEAP_SIZE=16g
;;
esac

if [ -e /etc/yandex/logshatter/logshatter-env.sh ] ; then
    . /etc/yandex/logshatter/logshatter-env.sh
fi

DEBUG="-Xdebug -Xverify:none -Xnoagent -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=32185"
#DEBUG="-agentpath:/usr/bin/logshatter-libyjpagent.so=dir=/var/lib/yandex/logshatter/snapshots,port=32185"

if [ -e $LOG_GC ] ; then
    mv $LOG_GC $LOG_GC.1
fi

java -classpath $CLASSPATH $DEBUG \
     -Djava.net.preferIPv6Addresses=true \
     -Djava.net.preferIPv4Stack=false \
     -Dsun.net.inetaddr.ttl=60 \
     -Dsun.net.inetaddr.negative.ttl=0 \
     -showversion -server \
     -Xmx$MAX_HEAP_SIZE -Xms$MAX_HEAP_SIZE -Xss2m -XX:PermSize=128m -XX:MaxPermSize=128m\
     -verbose:gc -XX:+PrintGC -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -Xloggc:$LOG_GC \
     -XX:+UseCompressedOops -XX:AutoBoxCacheMax=10000 \
     -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/var/log/$USER/hprof/ \
     -Dhost.name=`/bin/hostname` \
     -Denvironment=$ENV_TYPE -Ddc=${SRV_DC,,} \
     -Dbean.file=logshatter.xml -Dmodule.properties.file=logshatter.properties \
     ru.yandex.market.log4j2.Main >> $LOG_RC 2>&1 &

echo $! > /var/run/$USER/$SERVANT.pid

