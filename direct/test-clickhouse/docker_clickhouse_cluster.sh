#!/bin/bash

set -e

ya make # нижеследующий ya make собирает только зависимости, поэтому его одного не достаточно
ya make --get-deps=deps
exec ya tool java \
    -classpath 'libs-test-clickhouse.jar:deps/bin/*' \
    ru.yandex.direct.test.clickhouse.ClickHouseClusterTool \
    "$@"
