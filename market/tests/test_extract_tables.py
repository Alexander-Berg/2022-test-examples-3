import sqlparse

from lib.blueprints.parse_sql import extract_tables


def extract_tables_runner(sql):
    return [x.lower().split(' ')[0] for x in extract_tables(sqlparse.parse(sql)[0])]


def test_extract_tables_simple():
    assert extract_tables_runner("select * from tbl1") == ['tbl1']


def test_extract_tables_aliased():
    assert extract_tables_runner("select * from tbl1 as alias1") == ['tbl1']


extract_tables_complex_sql = '''
select * from tbl1
left join cubes.tbl2 as alias2 on (a = b)
inner join (select * from tbl5) as alias4
where somefield in (select id from tbl3 inner join tbl4 as alias3)
'''


def test_extract_tables_complex():
    assert sorted(extract_tables_runner(extract_tables_complex_sql)) == sorted(['tbl4', 'tbl5', 'tbl2', 'tbl3', 'tbl1'])


extract_tables_grouped_by_sql = '''
SELECT `cubes_clickhouse__cube_cpc_clicks_b2b_analyst`.`category_name` AS `category_name`,
`cubes_clickhouse__cube_cpc_clicks_b2b_analyst`.`region_country_name` AS `region_country_name`
FROM `cubes_clickhouse__cube_cpc_clicks_b2b_analyst`
GROUP BY `category_name`,
`region_country_name`
'''


def test_extract_tables_grouped_by():
    assert extract_tables_runner(extract_tables_grouped_by_sql) == ['cubes_clickhouse__cube_cpc_clicks_b2b_analyst']


extract_tables_grouped_by_ordered_sql = '''
SELECT f2, count(distinct f1) as q
FROM tbl1 group by f2 order by q desc limit 7
'''


def test_extract_tables_grouped_by_ordered():
    assert extract_tables_runner(extract_tables_grouped_by_ordered_sql) == ['tbl1']


def test_extract_tables_ch_example_1():
    ch_example = '''
        SELECT publisher_id, LastSignificantTraficSourceSocialSourceNetworkID, LastSignificantTraficSourceUTMTerm
        FROM cubes.cubes_clickhouse__fact_end2end_analytics
        LIMIT 0, 1000
        '''
    assert extract_tables_runner(ch_example) == ['cubes_clickhouse__fact_end2end_analytics']


extract_tables_ch_example_2_sql = '''
select `publisher_id`, `LastSignificantTraficSourceSocialSourceNetworkID`
from `cubes`.`cubes_clickhouse__fact_end2end_analytics_distributed`
limit 0, 1000 format JSON
'''


def test_extract_tables_ch_example_2():
    assert extract_tables_runner(extract_tables_ch_example_2_sql) == [
        'cubes_clickhouse__fact_end2end_analytics_distributed']


extract_tables_function_1 = """
SELECT * FROM cubes_clickhouse__fact_end2end_analytics_distributed
WHERE EXTRACT(DAY FROM cubes_clickhouse__fact_end2end_analytics_distributed.date) > 0
"""


def test_extract_tables_function_1():
    assert extract_tables_runner(extract_tables_function_1) == ['cubes_clickhouse__fact_end2end_analytics_distributed']


extract_table_chyt_case_table_without_date = """
select count(*) from "//home/market/production/oroboros/sstradomsky/our_table"
"""


def test_extract_table_chyt_case_table_without_date():
    assert extract_tables_runner(extract_table_chyt_case_table_without_date) \
           == \
           ["//home/market/production/oroboros/sstradomsky/our_table"]


extract_table_chyt_case_table_with_date = """
select count(*) from "//home/market/production/oroboros/sstradomsky/our_table/1d/2020-01-18"
"""


def test_extract_table_chyt_case_table_with_date():
    assert extract_tables_runner(extract_table_chyt_case_table_with_date) \
           == \
           ["//home/market/production/oroboros/sstradomsky/our_table/1d/2020-01-18"]


extract_table_chyt_case_table_with_range_of_dates = """
select count(*) from "//home/market/production/oroboros/sstradomsky/our_table/1d/{2020-05-18--2020-05-20}"
"""


def test_extract_table_chyt_case_table_with_range_of_dates():
    assert extract_tables_runner(extract_table_chyt_case_table_with_range_of_dates) \
           == \
           ["//home/market/production/oroboros/sstradomsky/our_table/1d/{2020-05-18--2020-05-20}"]


extract_tables_aliased = '''
SELECT "AaxvwGi3Ku5fLSj6aFW2XZ".lsc_source_path_new_tree_1 AS a_1, toYear("AaxvwGi3Ku5fLSj6aFW2XZ".date) AS a_2,
uniqExactIf("AaxvwGi3Ku5fLSj6aFW2XZ".multiorder_id, "AaxvwGi3Ku5fLSj6aFW2XZ".item_was_gross = 1) AS a_3
FROM cubes.cubes_clickhouse__fact_end2end_analytics_distributed AS "AaxvwGi3Ku5fLSj6aFW2XZ"
WHERE toYear("AaxvwGi3Ku5fLSj6aFW2XZ".date) IN (2019, 2020) AND toMonth("AaxvwGi3Ku5fLSj6aFW2XZ".date) IN (9, 10)
GROUP BY a_1, a_2
LIMIT 1000001
'''


def test_extract_aliased_table():
    assert extract_tables_runner(extract_tables_aliased) \
           == \
           ["cubes_clickhouse__fact_end2end_analytics_distributed"]


extract_table_chyt_case_table_with_range_of_dates_with_alias = "select count(*) from " \
"`//home/market/production/oroboros/sstradomsky/our_table/1d/{2020-05-18--2020-05-20}` " \
"`X_home_market_production_oroboros_sstradomsky_out_table_1d__2020_05_18__2020_05_20_`"


def test_extract_table_chyt_case_table_with_range_of_dates_with_alias():
    assert extract_tables_runner(extract_table_chyt_case_table_with_range_of_dates_with_alias) \
           == \
           ["//home/market/production/oroboros/sstradomsky/our_table/1d/{2020-05-18--2020-05-20}"]
