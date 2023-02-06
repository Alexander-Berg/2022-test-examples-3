
-- mock template

-- allows to mock table calls
DEFINE SUBQUERY $_table($tablename) as
    $target_name = case
    when $tablename = '//home/cdc/market/_YT_ENV_/tablename' then '//mock_2'
    when $tablename = '//home/cdc/market/_YT_ENV_/tablename_2' then '//mock_3'
    when $tablename = '//tmp/yqltest/mocked_var/model_rating' then '//mock_4'
    when $tablename = '//tmp/yqltest/mocked_var/grade_count' then '//mock_5'
    else 'unknown-'||$tablename end
;
    SELECT * FROM $target_name with inline;
END DEFINE;

$_dir = ($path) -> {return case
    when $path = '//home/market/production/pers-grade/tables/grade' then '//mock_1'
    else 'unknown-'||$path end
;};
PRAGMA yt.DefaultMaxJobFails="1";

$rating_grade_bound = 3;
$rating_grade_bound___mocked = 1;
$min_rating_good = 4.2;
$min_rating_good___mocked = 0;

$model_rating = (select * from $_table('//tmp/yqltest/mocked_var/model_rating'));
$model_rating___mocked = (
    select *
    from `//home/market/rating_source`
    join `//home/market/good_models` using (model_id)
);

$grade_count = (select * from $_table('//tmp/yqltest/mocked_var/grade_count'));
$grade_count___mocked = (
    select *
    from `//home/market/grade` as g
    join `//home/market/grade_model` as mg on g.id = mg.grade_id
    group by g.id
);

select *
from $_table('//home/cdc/market/_YT_ENV_/tablename')
join range($_dir('//home/market/production/pers-grade/tables/grade')) with inline
join model_rating using (model_id)
join grade_count using (model_id)
