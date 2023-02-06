from market.dynamic_pricing.pricing.deadstock_sales.deadstock_sale_stats.sale_stats import (
    is_date_in_season
)
import yatest.common


class __YaTestEnv:  # for "ya make -t ..."
    def __init__(self):
        self.python_bin = yatest.common.python_path()
        self.work_dir = yatest.common.work_path()
        self.binary_path = yatest.common.binary_path
        self.source_path = yatest.common.source_path


_env = __YaTestEnv()


def test_one_year_true():
    assert is_date_in_season(
        date='2021-10-05',
        seasons=[{'from': '03-01', 'to': '06-15'}, {'from': '09-01', 'to': '12-15'}]
    )


def test_one_year_false():
    assert not is_date_in_season(
        date='2021-08-05',
        seasons=[{'from': '03-01', 'to': '06-15'}, {'from': '09-01', 'to': '12-15'}]
    )


def test_new_year_true():
    assert is_date_in_season(
        date='2022-01-05',
        seasons=[{'from': '03-01', 'to': '06-15'}, {'from': '09-01', 'to': '02-15'}]
    )


def test_new_year_false():
    assert not is_date_in_season(
        date='2022-02-18',
        seasons=[{'from': '03-01', 'to': '06-15'}, {'from': '09-01', 'to': '02-15'}]
    )


def test_one_month_new_year_true():
    assert is_date_in_season(
        date='2021-03-05',
        seasons=[{'from': '03-04', 'to': '03-01'}]
    )


def test_one_month_new_year_false():
    assert not is_date_in_season(
        date='2021-03-03',
        seasons=[{'from': '03-04', 'to': '03-01'}]
    )
