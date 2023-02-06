USE_RECIPE(travel/rasp/suburban_tasks/bin/tests_recipe/suburban_tasks_recipe)

INCLUDE(${ARCADIA_ROOT}/travel/library/recipes/recipe_lib.inc)
INCLUDE(${ARCADIA_ROOT}/travel/rasp/library/common_recipe/recipe_lib.inc)

DEPENDS(
    travel/rasp/suburban_tasks/bin/tests_recipe
    travel/rasp/suburban_tasks/bin/tests_recipe/package
    travel/library/recipes
)
