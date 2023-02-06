DEPENDS(
    extsearch/geo/meta/daemon
    extsearch/geo/meta/rearrs/data
)

DATA(
    arcadia/extsearch/geo/meta/tests/env/fixed/configs
)

INCLUDE(${ARCADIA_ROOT}/extsearch/geo/mocks/geocoder/recipe/recipe.inc)
INCLUDE(${ARCADIA_ROOT}/extsearch/geo/mocks/wizard/recipe/recipe.inc)
INCLUDE(${ARCADIA_ROOT}/library/recipes/tvmapi/recipe.inc)
