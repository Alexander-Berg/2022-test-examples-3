INCLUDE(${ARCADIA_ROOT}/crypta/lib/inc/local_resource_service.inc)
INCLUDE(${ARCADIA_ROOT}/crypta/lib/inc/rtmr_test.inc)

USE_RECIPE(
    crypta/lib/recipes/juggler_push_mock/juggler_push
)

DEPENDS(
    crypta/lib/recipes/juggler_push_mock
)
