#!/bin/bash

# wait for unified agent
while ! curl http://localhost:16301/status > /dev/null 2>&1; do sleep 1; done

case "$1" in
    dev7)
        JAVA_ADDITIONAL_ARGS=(
            -Ddb_config=zk:///direct/np/db-config/db-config.dev7.json
        )
        ;;
    devtest)
        JAVA_ADDITIONAL_ARGS=(
            -Ddb_config=zk:///direct/np/db-config/db-config.devtest.json
        )
        ;;
    testing)
        JAVA_ADDITIONAL_ARGS=(
            -Ddb_config=zk:///direct/np/db-config/db-config.test.json
        )
        ;;
esac

JAVA_ADDITIONAL_ARGS+=(
    -Dnetwork_config=zk:///direct/network-config.json
)


JAVA_OPTS=(
    -Xmx4G
    -Dfile.encoding=UTF-8
    -Djava.net.preferIPv4Stack=false
    -Djava.net.preferIPv6Addresses=true
    -Dsun.net.inetaddr.ttl=60
    -Dsun.net.inetaddr.negative.ttl=0
    -Xlog:gc*=info,safepoint=info,age*=trace:file=/var/log/yandex/direct-steps.gc.log:time,uptime,level,tags:filecount=5,filesize=100M
    -XX:+CrashOnOutOfMemoryError
    -XX:+HeapDumpOnOutOfMemoryError
    -XX:-OmitStackTraceInFastThrow
    -XX:HeapDumpPath=/var/log/yandex/direct-steps.hprof
)

JAVA_ARGS=(
    -cp /var/www/direct-steps/direct-steps/*:
    -Djava.library.path=/var/www/direct-steps/direct-steps
    -Dhealth_checker.enable_cache=true
    -Dlog4j.configurationFile=/etc/direct/steps/logging-config/log4j2-deploy.xml
    ru.yandex.direct.teststeps.TestStepsApp
    --log-configs-directory /etc/direct/steps/logging-config/
)

/usr/local/yandex-direct-jdk11/bin/java ${JAVA_OPTS[@]} ${JAVA_ADDITIONAL_ARGS[@]} ${JAVA_ARGS[@]}
