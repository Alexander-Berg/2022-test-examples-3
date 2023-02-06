TEST_JAVA_CLASSPATH_CMD_TYPE(LIST)

INCLUDE(${ARCADIA_ROOT}/market/lilucrm/test.ya.make)
INCLUDE(${ARCADIA_ROOT}/yql/library/test_framework/recipe/recipe.inc)
INCLUDE(${ARCADIA_ROOT}/market/lilucrm/mcrm/db_support/src/test/general.inc)
INCLUDE(${ARCADIA_ROOT}/market/lilucrm/campaign/src/integration-test/recipes/resources_distributor/recipe.inc)

# Кладём в ${ARCADIA_BUILD_ROOT}/crm_ram_resources те ресурсы, которые необходимо перенести на ram-диск
DATA(sbr://711210081=${ARCADIA_BUILD_ROOT}/crm_ram_resources)  # geodata6.bin

SET(
    UDFS
    ydb/library/yql/udfs/common/datetime2
    ydb/library/yql/udfs/common/digest
    yql/udfs/common/file
    yql/udfs/common/geo
    ydb/library/yql/udfs/common/hyperloglog
    yql/udfs/common/python/python_arc_small
    yql/udfs/common/python/python3
    yql/udfs/common/protobuf
    ydb/library/yql/udfs/common/pire
    ydb/library/yql/udfs/common/re2
    ydb/library/yql/udfs/common/set
    ydb/library/yql/udfs/common/stat
    ydb/library/yql/udfs/common/topfreq
    ydb/library/yql/udfs/common/top
    yql/udfs/common/url
    yql/udfs/common/streaming
    ydb/library/yql/udfs/common/string
    ydb/library/yql/udfs/common/histogram
    ydb/library/yql/udfs/common/json2
    ydb/library/yql/udfs/common/yson2
    ydb/library/yql/udfs/common/math
    yql/udfs/common/unicode
    yql/udfs/examples/callables
    yql/udfs/examples/dicts
    yql/udfs/examples/dummylog
    yql/udfs/examples/lists
    yql/udfs/examples/structs
    yql/udfs/examples/type_inspection
    ydb/library/yql/udfs/logs/dsv
    yql/udfs/test/simple
    yql/udfs/test/test_import
    yql/udfs/crypta/identifiers
)

DEPENDS(
    ${UDFS}
)

SIZE(LARGE)

TAG(
    ya:fat
    ya:force_sandbox
    ya:noretries
)

REQUIREMENTS(
    network:full
    ram_disk:20
    ram:16
)



#Необходимо сбросить режим YQL_DETERMINISTIC_MODE для выполнения параллельных yql запросов,
#чтобы для одинаковых подзапросов создавались временные выходные таблицы с разным id
ENV(YQL_DETERMINISTIC_MODE=)
