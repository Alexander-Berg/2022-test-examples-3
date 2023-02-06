$rating_grade_bound = 1;
$min_rating_good = 0;

$model_rating = (
    select *
    from `//home/market/rating_source`
    join `//home/market/good_models` using (model_id)
);

$grade_count = (
    select *
    from `//home/market/grade` as g
    join `//home/market/grade_model` as mg on g.id = mg.grade_id
    group by g.id
);

select *
from $_table('//home/cdc/market/_YT_ENV_/tablename')
join range($_dir('//home/market/production/pers-grade/tables/grade')) --_INLINE_
join model_rating using (model_id)
join grade_count using (model_id)
