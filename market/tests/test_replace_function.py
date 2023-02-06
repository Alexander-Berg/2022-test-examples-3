import sqlparse

from lib.app import AVAILABLE_DATABASES

clickhouse = AVAILABLE_DATABASES["clickhouse"]


def repl_clickhouse(sql):
    return clickhouse.replace_query_tokens(
        sqlparse.parse(sql)[0],
        ["some_great_table"]
    )


sql_1 = """select count(distinct user_id) as user_qty,
case when count(distinct user_id) > 0 then 1 else 0 end as zzz,
count(distinct order_id), max(user_id) from tbl1 as tbl1_alias
left join (select count(distinct user_id) from tbl2 group by user_id) as tbl2_sum_alias
where user_id = 8
having count(distinct user_id) > 7 and count(user_id) < 12"""

sql_1_result = """select uniq(( /* distinct repl ch-cache */  user_id)) as user_qty,
case when uniq(( /* distinct repl ch-cache */  user_id)) > 0 then 1 else 0 end as zzz,
count(distinct order_id), max(user_id) from tbl1 as tbl1_alias any left join (select uniq(( /* distinct repl ch-cache */  user_id)) from tbl2 group by user_id) as tbl2_sum_alias
where user_id = 8
having uniq(( /* distinct repl ch-cache */  user_id)) > 7 and count(user_id) < 12"""


def test_replace_function_1():
    assert repl_clickhouse(sql_1) == sql_1_result


sql_join = """select count(distinct user_id) as user_qty,
case when count(distinct user_id) > 0 then 1 else 0 end as zzz,
count(distinct order_id), max(user_id) from tbl1 as tbl1_alias
left
join (select count(distinct user_id) from tbl2 group by user_id) as tbl2_sum_alias
right join tbl3 as tbl3_alias on tbl1_alias.order_id=tbl3_alias.id
left join tbl4 as tbl4_alias on tbl2_sum_alias.some_id=tbl4_alias.id
where user_id = 8
having count(distinct user_id) > 7 and count(user_id) < 12"""

sql_join_result = """select uniq(( /* distinct repl ch-cache */  user_id)) as user_qty,
case when uniq(( /* distinct repl ch-cache */  user_id)) > 0 then 1 else 0 end as zzz,
count(distinct order_id), max(user_id) from tbl1 as tbl1_alias any left join (select uniq(( /* distinct repl ch-cache */  user_id)) from tbl2 group by user_id) as tbl2_sum_alias
right join tbl3 as tbl3_alias on tbl1_alias.order_id=tbl3_alias.id any left join tbl4 as tbl4_alias on tbl2_sum_alias.some_id=tbl4_alias.id
where user_id = 8
having uniq(( /* distinct repl ch-cache */  user_id)) > 7 and count(user_id) < 12"""


def test_replace_function_join():
    assert repl_clickhouse(sql_join) == sql_join_result


def test_replace_function_2():
    assert repl_clickhouse(
        "Select toUint64(user_id, other, some) From tbl1") == "Select toUint64(user_id, other, some) From tbl1"


cased_sql = '''select count(distinct ( case when a = 'a' then concat('z', `user_id`) else null end ))
from a'''
cased_sql_replaced = '''select uniq(( /* distinct repl ch-cache */  ( case when a = 'a' then concat('z', `user_id`) else null end )))
from a'''


def test_cased_sql():
    assert repl_clickhouse(cased_sql) == cased_sql_replaced


cased_sql_in_having = '''select * from a
having count( distinct case when a = 'a' then concat('z', `user_id`) else null end) > 3'''
cased_sql_in_having_replaced = '''select * from a
having uniq((  /* distinct repl ch-cache */  case when a = 'a' then concat('z', `user_id`) else null end)) > 3'''


def test_cased_sql_in_having():
    assert repl_clickhouse(cased_sql_in_having) == cased_sql_in_having_replaced


real_sql_1 = '''
select
SELECT COUNT(DISTINCT (CASE WHEN (`cubes_clickhouse__fact_end2end_analytics_distributed`.`record_type` = 'visits')
THEN concat(
  concat(replaceRegexpOne(toString(CAST(`date` AS DATE)), '^\\s+', ''), '_'),
  replaceRegexpOne(toString(`cubes_clickhouse__fact_end2end_analytics_distributed`.`user_id`), '^\\s+', ''))
ELSE NULL END)) AS `TEMP_Calculation_2732840549898121218__1657147588__0_`
from `cubes_clickhouse__fact_end2end_analytics_distributed`
'''

real_sql_replaced_1 = '''
select
SELECT uniq(( /* distinct repl ch-cache */  (CASE WHEN (`cubes_clickhouse__fact_end2end_analytics_distributed`.`record_type` = 'visits')
THEN concat(
  concat(replaceRegexpOne(toString(CAST(`date` AS DATE)), '^\\s+', ''), '_'),
  replaceRegexpOne(toString(`cubes_clickhouse__fact_end2end_analytics_distributed`.`user_id`), '^\\s+', ''))
ELSE NULL END))) AS `TEMP_Calculation_2732840549898121218__1657147588__0_`
from `cubes_clickhouse__fact_end2end_analytics_distributed`
'''


def test_real_sql_1():
    assert repl_clickhouse(real_sql_1) == real_sql_replaced_1


real_sql_2 = '''
SELECT COUNT(DISTINCT (
CASE WHEN (`cubes_clickhouse__fact_end2end_analytics_distributed`.`record_type` = 'visits')
THEN concat(
  concat(replaceRegexpOne(toString(CAST(`date` AS DATE)), '^\\s+', ''), '_'),
  replaceRegexpOne(toString(`cubes_clickhouse__fact_end2end_analytics_distributed`.`user_id`), '^\\s+', ''))
ELSE NULL END)) AS `TEMP_Calculation_2732840549898121218__1657147588__0_`
FROM tbl
'''

real_sql_replaced_2 = '''
SELECT uniq(( /* distinct repl ch-cache */  (
CASE WHEN (`cubes_clickhouse__fact_end2end_analytics_distributed`.`record_type` = 'visits')
THEN concat(
  concat(replaceRegexpOne(toString(CAST(`date` AS DATE)), '^\\s+', ''), '_'),
  replaceRegexpOne(toString(`cubes_clickhouse__fact_end2end_analytics_distributed`.`user_id`), '^\\s+', ''))
ELSE NULL END))) AS `TEMP_Calculation_2732840549898121218__1657147588__0_`
FROM tbl
'''


def test_real_sql_2():
    assert repl_clickhouse(real_sql_2) == real_sql_replaced_2


inner_join_sql_1 = """select count(distinct user_id) as user_qty,
case when count(distinct user_id) > 0 then 1 else 0 end as zzz,
count(distinct order_id), max(user_id) from tbl1 as
tbl1_alias
left join (select count(distinct user_id) from tbl2 group by user_id) as
tbl2_sum_alias
inner join (select count(distinct user_id) from tbl3 group by user_id) as tbl3_sum_alias
where user_id = 8
having count(distinct user_id) > 7 and count(user_id) < 12"""

inner_join_sql_1_result = """select uniq(( /* distinct repl ch-cache */  user_id)) as user_qty,
case when uniq(( /* distinct repl ch-cache */  user_id)) > 0 then 1 else 0 end as zzz,
count(distinct order_id), max(user_id) from tbl1 as
tbl1_alias any left join (select uniq(( /* distinct repl ch-cache */  user_id)) from tbl2 group by user_id) as
tbl2_sum_alias any left join (select uniq(( /* distinct repl ch-cache */  user_id)) from tbl3 group by user_id) as tbl3_sum_alias
where user_id = 8
having uniq(( /* distinct repl ch-cache */  user_id)) > 7 and count(user_id) < 12"""


def test_inner_join_sql_1():
    assert repl_clickhouse(inner_join_sql_1) == inner_join_sql_1_result


wierd_to_string_date_field = (
    "concat(concat(concat(concat("
    "replaceRegexpOne(toString(CAST(trunc(toYear(cubes_clickhouse__cube_stock_movement.date), 0), 'INTEGER')), '^\\s+', ''), '-'), "
    "multiIf(CAST(trunc(toMonth(cubes_clickhouse__cube_stock_movement.date), 0), 'INTEGER') < 10, "
    "concat('0', replaceRegexpOne(toString(CAST(trunc(toMonth(cubes_clickhouse__cube_stock_movement.date), 0), 'INTEGER')), '^\\s+', '')), "
    "replaceRegexpOne(toString(CAST(trunc(toMonth(cubes_clickhouse__cube_stock_movement.date), 0), 'INTEGER')), '^\\s+', ''))), '-'), "
    "multiIf(CAST(trunc(toDayOfMonth(cubes_clickhouse__cube_stock_movement.date), 0), 'INTEGER') < 10, "
    "concat('0', replaceRegexpOne(toString(CAST(trunc(toDayOfMonth(cubes_clickhouse__cube_stock_movement.date), 0), 'INTEGER')), '^\\s+', '')), "
    "replaceRegexpOne(toString(CAST(trunc(toDayOfMonth(cubes_clickhouse__cube_stock_movement.date), 0), 'INTEGER')), '^\\s+', '')))"
)

weird_to_string_date_tableau_sql = """
select
    {}
from cubes_clickhouse__cube_stock_movement
group by
    {}
""".format(wierd_to_string_date_field, wierd_to_string_date_field)

weird_to_string_date_tableau_sql_result = """
select
    toString(cubes_clickhouse__cube_stock_movement.date)
from cubes_clickhouse__cube_stock_movement
group by
    toString(cubes_clickhouse__cube_stock_movement.date)
"""


def test_weird_to_string_date_tableau_sql():
    assert repl_clickhouse(weird_to_string_date_tableau_sql) == weird_to_string_date_tableau_sql_result


weird_to_string_year_tableau_sql = """
select
    replaceRegexpOne(toString(CAST(trunc(toYear(cubes_clickhouse__cube_stock_movement.date), 0), 'INTEGER')), '^\\s+', '')
from cubes_clickhouse__cube_stock_movement
group by
    replaceRegexpOne(toString(CAST(trunc(toYear(cubes_clickhouse__cube_stock_movement.date), 0), 'INTEGER')), '^\\s+', '')
"""

weird_to_string_year_tableau_sql_result = """
select
    toString(toYear(cubes_clickhouse__cube_stock_movement.date))
from cubes_clickhouse__cube_stock_movement
group by
    toString(toYear(cubes_clickhouse__cube_stock_movement.date))
"""


def test_weird_to_string_year_tableau_sql():
    assert repl_clickhouse(weird_to_string_year_tableau_sql) == weird_to_string_year_tableau_sql_result


weird_to_string_month_tableau_sql = """
select
    replaceRegexpOne(toString(CAST(trunc(toMonth(cubes_clickhouse__cube_stock_movement.date), 0), 'INTEGER')), '^\\s+', '')
from cubes_clickhouse__cube_stock_movement
group by
    replaceRegexpOne(toString(CAST(trunc(toMonth(cubes_clickhouse__cube_stock_movement.date), 0), 'INTEGER')), '^\\s+', '')
"""

weird_to_string_month_tableau_sql_result = """
select
    toString(toMonth(cubes_clickhouse__cube_stock_movement.date))
from cubes_clickhouse__cube_stock_movement
group by
    toString(toMonth(cubes_clickhouse__cube_stock_movement.date))
"""


def test_weird_to_string_month_tableau_sql():
    assert repl_clickhouse(weird_to_string_month_tableau_sql) == weird_to_string_month_tableau_sql_result


weird_to_string_day_tableau_sql = """
select
    replaceRegexpOne(toString(CAST(trunc(toDayOfMonth(cubes_clickhouse__cube_stock_movement.date), 0), 'INTEGER')), '^\\s+', '')
from cubes_clickhouse__cube_stock_movement
group by
    replaceRegexpOne(toString(CAST(trunc(toDayOfMonth(cubes_clickhouse__cube_stock_movement.date), 0), 'INTEGER')), '^\\s+', '')
"""

weird_to_string_day_tableau_sql_result = """
select
    toString(toDayOfMonth(cubes_clickhouse__cube_stock_movement.date))
from cubes_clickhouse__cube_stock_movement
group by
    toString(toDayOfMonth(cubes_clickhouse__cube_stock_movement.date))
"""


def test_weird_to_string_day_tableau_sql():
    assert repl_clickhouse(weird_to_string_day_tableau_sql) == weird_to_string_day_tableau_sql_result
