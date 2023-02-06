# coding: utf-8
import datetime

import pytest

from lib.constants import YT_CLUSTERS
from lib.database.chyt import Chyt

chyt_query_range_of_tables = """
SELECT `X_home_market_production_mstat_dictionaries_stock_sku_1d__2020_10_18__2020_10_19_`.`date` AS `date`
FROM `//home/market/production/mstat/dictionaries/stock_sku/1d/{{2020-10-18--{last_date}}}`
`X_home_market_production_mstat_dictionaries_stock_sku_1d__2020_10_18__2020_10_19_`
"""

chyt_query_range_of_tables_expected_replacing = """
SELECT `X_home_market_production_mstat_dictionaries_stock_sku_1d__2020_10_18__2020_10_19_`.`date` AS `date`
FROM concatYtTablesRange("//home/market/production/mstat/dictionaries/stock_sku/1d", '2020-10-18'{last_date_part})
`X_home_market_production_mstat_dictionaries_stock_sku_1d__2020_10_18__2020_10_19_`
"""


def init_chyt():
    return Chyt(YT_CLUSTERS)


@pytest.mark.parametrize(
    "kwargs,expected_url",
    [
        (
            {
                "database_host": "hahn",
                "clique_name": "chyt_market_production",
                "password": "some_pass"
            },
            "https://hahn.yt.yandex.net/query?database=%2Achyt_market_production&password=some_pass"
        ),
        (
            {
                "database_host": "arnold",
                "clique_name": "chyt_market_production",
                "password": "some_pass"
            },
            "https://arnold.yt.yandex.net/query?database=%2Achyt_market_production&password=some_pass"
        ),
        (
            {
                "database_host": "hahn",
                "clique_name": "ch_public",
                "password": "some_pass1"
            },
            "https://hahn.yt.yandex.net/query?database=%2Ach_public&password=some_pass1"
        )
    ]
)
def test_construct_url(kwargs, expected_url):
    chyt = init_chyt()

    assert chyt.construct_url(**kwargs) == expected_url


def test_get_range_years():
    first_year = "2018"
    last_year = "2021"
    expected_range = ["2018", "2019", "2020", "2021"]

    chyt = init_chyt()
    assert chyt.get_range(first_year, last_year) == expected_range


def test_get_range_months():
    first_month = "2018-01"
    last_month = "2018-05"
    expected_range = ["2018-01", "2018-02", "2018-03", "2018-04", "2018-05"]

    chyt = init_chyt()
    assert chyt.get_range(first_month, last_month) == expected_range


def test_get_range_days():
    first_date = "2018-01-01"
    last_date = "2018-01-04"
    expected_range = ["2018-01-01", "2018-01-02", "2018-01-03", "2018-01-04"]

    chyt = init_chyt()
    assert chyt.get_range(first_date, last_date) == expected_range


def test_get_range_error():
    first_datetime = "2019-01-01T00:00:00"
    last_datetime = "2019-01-03T00:00:00"
    chyt = init_chyt()

    with pytest.raises(Exception) as execinfo:
        chyt.get_range(first_datetime, last_datetime)

    assert str(execinfo.value) == "Unsupported date format. Should be or YYYY, or YYYY-MM, or YYYY-MM-DD"


def get_extended_range_of_tables_one_table():
    table = "//home/market/production/my_table/1d/2020-01-01"
    expected_range_of_tables = ["//home/market/production/my_table/1d/2020-01-01"]
    chyt = init_chyt()

    assert chyt.get_extended_range_of_tables(table) == expected_range_of_tables


def get_extended_range_of_tables_range_of_tables():
    tables = "//home/market/production/my_table/1d/{2020-01-01--2020-01-03}"
    expected_range_of_tables = [
        "//home/market/production/my_table/1d/2020-01-01",
        "//home/market/production/my_table/1d/2020-01-02",
        "//home/market/production/my_table/1d/2020-01-03",
    ]
    chyt = init_chyt()

    assert chyt.get_extended_range_of_tables(tables) == expected_range_of_tables


@pytest.mark.parametrize("input_range_end, converted_range_end",
                         [
                             ("2020-10-19", ", '2020-10-19'"),
                             ("", ", '{}'".format(datetime.date.today().isoformat())),
                         ])
def test_query_replace(input_range_end, converted_range_end):
    chyt = init_chyt()

    assert chyt.replace_query_tokens(
        query_tokens=None,
        tables=[
            "//home/market/production/mstat/dictionaries/stock_sku/1d/{{2020-10-18--{}}}".format(input_range_end)
        ],
        query=chyt_query_range_of_tables.format(last_date=input_range_end)
    ) == chyt_query_range_of_tables_expected_replacing.format(last_date_part=converted_range_end)
