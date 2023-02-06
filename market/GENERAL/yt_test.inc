IF(OS_LINUX)
    INCLUDE(${ARCADIA_ROOT}/mapreduce/yt/python/recipe/recipe.inc)

    SYSTEM_PROPERTIES(
        YT_TOKEN none
        YT_USERNAME root
    )
ENDIF()

PEERDIR(
    market/adv/adv-shop/yt-test
)
