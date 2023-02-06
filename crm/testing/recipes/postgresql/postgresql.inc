USE_RECIPE(antiadblock/postgres_local/recipe/recipe --port 5432 --user postgres --db_name postgres)

# Postgres 11.6
IF (OS_LINUX)
    DATA(sbr://1490193510)
ELSEIF (OS_DARWIN)
    DATA(sbr://1490218487)
ENDIF()

DEPENDS(
  antiadblock/postgres_local/recipe
)
