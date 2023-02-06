IF(OS_LINUX)
    INCLUDE(${ARCADIA_ROOT}/mapreduce/yt/python/recipe/recipe_with_tablets.inc)
    INCLUDE(${ARCADIA_ROOT}/ofd/common/postgres_recipe/recipe/recipe.inc)

    SYSTEM_PROPERTIES(
        org.asynchttpclient.useNativeTransport true
        YQL_DISABLED true
    )
ENDIF()

SYSTEM_PROPERTIES(
    # Отдельный конфиг для логирования при запуске medium тестов из-под YA
    log4j2.configurationFile classpath:log4j2-ya-test.xml
)

TAG(
    ya:huge_logs
)
