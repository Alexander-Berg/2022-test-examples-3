DEPENDS(
    mapreduce/yt/python/recipe
    yt/packages/latest
    contrib/tools/python
)

USE_RECIPE(
    mapreduce/yt/python/recipe/yt_recipe --wait-tablet-cell-initialization --node-config "{bus_server={bind_retry_count=1}}"
)
