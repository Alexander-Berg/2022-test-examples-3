DEPENDS(
    crypta/ltp/viewer/services/api/bin
    crypta/ltp/viewer/services/worker/bin
)

DATA(
    arcadia/crypta/ltp/viewer/services/api/docker/templates/config.template.yaml
    arcadia/crypta/ltp/viewer/services/worker/docker/templates/config.template.yaml
)

INCLUDE(${ARCADIA_ROOT}/crypta/lib/inc/local_chyt.inc)
INCLUDE(${ARCADIA_ROOT}/crypta/lib/inc/local_yql.inc)
INCLUDE(${ARCADIA_ROOT}/kikimr/public/tools/lbk_recipe/recipe_stable.inc)
INCLUDE(${ARCADIA_ROOT}/kikimr/public/tools/ydb_recipe/recipe_stable.inc)
