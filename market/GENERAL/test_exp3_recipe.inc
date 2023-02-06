DEPENDS(market/dev-exp/recipes/exp3-matcher)
USE_RECIPE(
    market/dev-exp/recipes/exp3-matcher/recipe_exp3_matcher
    market/mapi/mapi/src/test-adm-cfg/resources/exp3MatcherResponse.json
)

ENV(
    market.mapi.use.exp3.recipe=true
)
