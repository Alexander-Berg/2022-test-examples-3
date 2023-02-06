DEPENDS(
    mssngr/router/lib/postgres/recipe
)

DATA(sbr://678806418)

USE_RECIPE(
    mssngr/router/lib/postgres/recipe/postgres_recipe search/stoker/tests/db-init.sql
)

