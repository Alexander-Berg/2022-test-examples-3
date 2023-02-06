# yql tests are usually large. Run manually and on release
SIZE(LARGE)
TAG(
    ya:fat
)

# all pers yql tests can run independently
# too many threads could exceed recipe capabilities
# TODO temporary parallel execution is disabled.
# Something is going wrong with recipe when tests are non-stable
# Maybe run later in pools per class.
#FORK_SUBTESTS()
#SPLIT_FACTOR(4)

# yql recipe works only on linux :(
# in other OS some real cluster would be used
IF(OS_LINUX)
    INCLUDE(${ARCADIA_ROOT}/yql/library/test_framework/recipe/recipe.inc)

    # use env to mark, that tests should use recipe instead of real cluster
    ENV(USE_YT_RECIPE=true)

    # yt udfs to use
    DEPENDS(
        ydb/library/yql/udfs/common/datetime
        ydb/library/yql/udfs/common/datetime2
        ydb/library/yql/udfs/common/digest
        yql/udfs/common/python/python3
        ydb/library/yql/udfs/common/re2
        ydb/library/yql/udfs/common/string
        ydb/library/yql/udfs/common/set
        ydb/library/yql/udfs/common/json2
        ydb/library/yql/udfs/common/yson2
        ydb/library/yql/udfs/common/math
        yql/udfs/common/unicode
        ydb/library/yql/udfs/common/stat
    )


ENDIF()

# on non-linux machines require network to run with real cluster
IF(NOT OS_LINUX)
    REQUIREMENTS(
        network:full
    )
ENDIF()
