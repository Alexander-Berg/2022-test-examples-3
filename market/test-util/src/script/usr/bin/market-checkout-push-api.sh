#!/bin/sh
SERVANT=market-checkout-push-api
USER=checkout
LOG_RC=/var/log/${USER}/${SERVANT}.log.shell

echo "$(date -R) starting $0 $* (pid : $$) " >> ${LOG_RC}

JAVA_HOME=/usr/local/java7
PATH="${JAVA_HOME}/bin:${PATH}"

CLASSPATH=$(find /usr/lib/yandex/${SERVANT} -name '*.jar' -printf '%p:')${CLASSPATH}
CLASSPATH=/usr/lib/yandex/${SERVANT}/${SERVANT}.jar:${CLASSPATH}

export PATH LANG JAVA_HOME CLASSPATH

cd /etc/yandex/${SERVANT}

if [ -e /etc/yandex/environment.type ] ; then
    ENV_TYPE=$(cat /etc/yandex/environment.type)
    echo environment.type is ${ENV_TYPE} >> ${LOG_RC}
else
    echo Error: /etc/yandex/environment.type does not exists >> ${LOG_RC}
    exit 1
fi

java -classpath ${CLASSPATH} \
     -Doracle.net.tns_admin=/etc/oracle \
     -Djava.awt.headless=true \
     -Dorg.apache.commons.logging.LogFactory=org.apache.commons.logging.impl.LogFactoryImpl \
     -Dru.yandex.market.checkout.common.LogInitiallizerClass=ru.yandex.common.util.application.LoggerInitializer \
     -Dmodule.properties.file=app.properties \
     -server -Xverify:none \
     -Xmx512m -Xms64m -XX:+UseParallelGC -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=/var/log/${USER}/hprof/${SERVANT} \
     -XX:-OmitStackTraceInFastThrow \
     -Dhost.name=$(/bin/hostname -f) \
     -Dsun.net.client.defaultConnectTimeout=10000 \
     -Dsun.net.client.defaultReadTimeout=10000 \
     -Dsun.net.inetaddr.ttl=86400 -Dhttp.keepAlive=false \
     -Dspring.profiles.active=${ENV_TYPE} \
     -Denvironment=${ENV_TYPE} \
     -Djava.net.preferIPv4Stack=true \
     -Dservant.log.file=/var/log/${USER}/${SERVANT}.log \
     ru.yandex.market.checkout.common.spring.Main >> ${LOG_RC} 2>&1 &

echo $! > /var/run/${USER}/${SERVANT}.pid
