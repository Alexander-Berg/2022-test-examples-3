#!/bin/sh
SERVANT=fake-indexer

JAVA_HOME=/usr/local/java8
PATH="$JAVA_HOME/bin:$PATH"
LANG=ru_RU.KOI8-R

JLIB=${JLIB:=~/svn/market/libraries/trunk/common-lib}

CLASSPATH=`find lib $JLIB -maxdepth 3 -name '*.jar' -printf '%p:'`$CLASSPATH

export PATH LANG JAVA_HOME CLASSPATH

exec java -ea -classpath "$CLASSPATH" $flags \
     -Xms64m -Xmx768m -XX:+UseParallelGC \
     -Dfile.encoding=UTF-8 \
     -Dhost.name=`/bin/hostname` \
     -Dorg.apache.commons.logging.LogFactory=org.apache.commons.logging.impl.LogFactoryImpl \
	 -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=6662 \
     ru.yandex.market.supercontroller.fake.Main >> $SERVANT.log 2>&1

echo $! > $SERVANT.pid 
 