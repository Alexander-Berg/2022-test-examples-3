#!/bin/bash

set -e

DIR=`dirname $0`
cd "$DIR"

ya make # нижеследующий ya make собирает только зависимости, поэтому его одного не достаточно
ya make --get-deps=libs-test-mysql
ya tool java11 \
    -classpath 'libs-test-mysql.jar:libs-test-mysql/bin/*' \
    ru.yandex.direct.test.mysql.DirectMysqlRunner \
    "$@"
