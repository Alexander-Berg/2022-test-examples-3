#!/bin/sh

. ./config.sh

USER=checkout
SERVANT=checkout-push-api
JAVA_HOME=/usr/local/java7
PATH="${JAVA_HOME}/bin:${PATH}"
LANG=en_US.UTF-8
LOG_RC=/var/log/${USER}/${SERVANT}.log.shell

CLASSPATH=`find lib -maxdepth 3 -name '*.jar' -printf '%p:'`${CLASSPATH}

#echo $CLASSPATH

export PATH LANG JAVA_HOME CLASSPATH

if [ -e /etc/yandex/environment.type ] ; then
    ENV_TYPE=$(cat /etc/yandex/environment.type)
    echo environment.type is ${ENV_TYPE} >> ${LOG_RC}
else
    echo Error: /etc/yandex/environment.type does not exists >> ${LOG_RC}
    exit 1
fi

exec -a "${APP}" \
     java -ea -classpath "${CLASSPATH}" ${flags} \
     -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=${DEBUG_PORT} \
     -Xms64m -Xmx256m -XX:+UseParallelGC \
     -Dhost.name=$(/bin/hostname -f) \
     -Denvironment=${ENV_TYPE} \
     -Doracle.net.tns_admin=/etc/oracle \
     -Dmodule.properties.file=app.properties \
     -Dorg.apache.commons.logging.LogFactory=org.apache.commons.logging.impl.LogFactoryImpl \
     ru.yandex.market.checkout.common.spring.Main >> ${LOG_RC} 2>&1 &

echo $! > /var/run/${USER}/${APP}.pid
