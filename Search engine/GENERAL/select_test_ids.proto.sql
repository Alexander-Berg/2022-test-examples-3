/*
Script takes table in blender_squeeze format, selects specified test-ids, dates, vertical, platform.
*/

USE {server};

$control = {control};
$exp = {exp};

$platform = '{platform}';
$position_property = '{position_property}';

INSERT INTO
    [{to_path}]
WITH TRUNCATE

SELECT
    *
FROM
    RANGE([//home/blender/surplus_squeeze/production], [{from_date}], [{to_date}]) AS t
WHERE
    (String::Contains(t.ui, $platform) AND
     -- CAST(t.search_props{{$position_property}} AS int) < 10 AND
     (ListHas(t.test_ids, $control) OR ListHas(t.test_ids, $exp)))
;
