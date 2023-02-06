DEPENDS(
    crypta/lab/rule_estimator/services/api/bin
    crypta/lab/rule_estimator/services/worker/bin
)

DATA(
    arcadia/crypta/lab/rule_estimator/services/api/docker/templates/config.template.yaml
    arcadia/crypta/lab/rule_estimator/services/worker/docker/templates/config.template.yaml
)

INCLUDE(${ARCADIA_ROOT}/crypta/lib/inc/local_yql.inc)
INCLUDE(${ARCADIA_ROOT}/kikimr/public/tools/lbk_recipe/recipe_stable.inc)
