# coding=utf-8
"""
Тест используется для проверки работы вызова indexerf prepare_demand_forecasting
"""

import pytest

from hamcrest import assert_that, equal_to, has_item, has_entries

from market.idx.yatf.matchers.env_matchers import HasExitCode
from market.idx.yatf.resources.yt_table_resource import YtTableResource

from market.idx.generation.indexerf.yatf.test_env import YtIndexErfTestEnv, ErfMode
from market.idx.generation.indexerf.yatf.resources.yt_erf_data import YTPrepareDemandForecastingErfData
from market.idx.generation.indexerf.yatf.resources.erf_output import YtErfOutput

from mapreduce.yt.python.table_schema import extract_column_attributes


demand_forecasting_table_path = "//mstat/demand_forecasting"

demand_forecasting_table_schema = [
    dict(name='sku', type='int64'),
    dict(name='date', type='string'),
    dict(name='demand', type='double'),
]


@pytest.fixture(scope='module')
def demand_forecasting_table(yt_stuff):
    rows = []
    rows.append({'sku': 1, 'date': '2020-01-24', 'demand': 0.1})    # today
    rows.append({'sku': 1, 'date': '2020-01-29', 'demand': 0.15})   # today + 5
    rows.append({'sku': 1, 'date': '2020-02-03', 'demand': 0.2})    # today + 10
    rows.append({'sku': 1, 'date': '2020-02-08', 'demand': 10.0})   # today + 15
    rows.append({'sku': 1, 'date': '2020-02-13', 'demand': 30.0})   # today + 20
    rows.append({'sku': 1, 'date': '2020-02-18', 'demand': 20.0})   # today + 25
    rows.append({'sku': 1, 'date': '2020-01-25', 'demand': 44.0})   # за это число предсказание не нужно - фильтруем
    rows.append({'sku': 1, 'demand': 44.0})  # нет даты - фильтруем

    rows.append({'sku': 2, 'date': '2020-01-24', 'demand': 11.0})   # today
    rows.append({'sku': 2, 'date': '2020-01-29', 'demand': 12.0})   # today + 5
    rows.append({'sku': 2, 'date': '2020-02-03', 'demand': 13.0})   # today + 10
    rows.append({'sku': 2, 'date': '2020-02-08', 'demand': 14.0})   # today + 15
    rows.append({'sku': 2, 'date': '2020-02-13'})   # today + 20, но нет предсказания - фильтруем.  (в результате будет 0)

    return YtTableResource(yt_stuff, demand_forecasting_table_path, data=rows, attributes={'schema': demand_forecasting_table_schema})


@pytest.fixture(scope='module')
def input_data(demand_forecasting_table):
    res = YTPrepareDemandForecastingErfData(demand_forecasting_table, '2020-01-24')
    return res


# output info and YT tables
@pytest.fixture(scope='module')
def erf_output():
    erf_path = '//indexer/stratocaster/result-table-demand-forecasting'
    return YtErfOutput(erf_path, 0)   # result table not sharded


# Execution of binary
@pytest.yield_fixture(scope='module')
def workflow(yt_stuff, input_data, erf_output):
    resources = {
        "input": input_data,
        "output": erf_output
    }

    with YtIndexErfTestEnv(**resources) as env:
        env.execute(ErfMode.PREPARE_DEMAND_FORECASTING, yt_stuff)
        env.verify()
        yield env


@pytest.fixture(scope='module')
def result_features_table(workflow):
    return workflow.result_tables[0]


def test_result_table_exists(yt_stuff, result_features_table):
    assert_that(yt_stuff.get_yt_client().exists(result_features_table.get_path()), 'Table doesn\'t exist')


def should_see_only_expected_features(res_data, expected_data):
    for item in expected_data:
        assert_that(res_data, has_item(has_entries(item)), "No match for result data")
    assert_that(len(res_data), equal_to(len(expected_data)), "No match for result data length")


# Tests
def test_result_table_schema(result_features_table):
    assert_that(extract_column_attributes(list(result_features_table.schema)),
                equal_to([
                    {'required': False, "name": "sku", "type": "int64"},
                    {'required': False, "name": "today_demand", "type": "double"},
                    {'required': False, "name": "in_5_days_demand", "type": "double"},
                    {'required': False, "name": "in_10_days_demand", "type": "double"},
                    {'required': False, "name": "in_15_days_demand", "type": "double"},
                    {'required': False, "name": "in_20_days_demand", "type": "double"},
                    {'required': False, "name": "in_25_days_demand", "type": "double"},
                ]), "Schema is incorrect")


def test_result(result_features_table):
    expected_rows = [
        {'sku': 1, 'today_demand': 0.1, 'in_5_days_demand': 0.15, 'in_10_days_demand': 0.2,
         'in_15_days_demand': 10.0, 'in_20_days_demand': 30.0, 'in_25_days_demand': 20.0},
        {'sku': 2, 'today_demand': 11.0, 'in_5_days_demand': 12.0, 'in_10_days_demand': 13.0,
         'in_15_days_demand': 14.0, 'in_20_days_demand': 0, 'in_25_days_demand': 0},
    ]
    should_see_only_expected_features(result_features_table.data, expected_rows)


@pytest.fixture(params=[{'row': {'sku': 1, 'date': '2020-01-24', 'demand': '0.1'}, 'table': '//mstat/demand_forecasting_bad1'},
                        {'row': {'sku': 1, 'date': 123, 'demand': 0.1}, 'table': '//mstat/demand_forecasting_bad2'},
                        {'row': {'date': '2020-01-24', 'demand': 0.1}, 'table': '//mstat/demand_forecasting_bad3'},
                        ],
                ids=['wrong_demand',
                     'wrong_date',
                     'no_sku'
                     ]
                )
def demand_forecasting_bad_feature(request):
    return request.param


@pytest.fixture()
def demand_forecasting_table_negative(yt_stuff, demand_forecasting_bad_feature):
    rows = []
    rows.append(demand_forecasting_bad_feature['row'])
    return YtTableResource(yt_stuff, demand_forecasting_bad_feature['table'], data=rows)


@pytest.fixture()
def input_data_negative(demand_forecasting_table_negative):
    res = YTPrepareDemandForecastingErfData(demand_forecasting_table_negative, '2020-01-24')
    return res


# output info and YT tables
@pytest.fixture(scope='module')
def erf_output_negative():
    erf_path = '//indexer/stratocaster/result-table-demand-forecasting-bad'
    return YtErfOutput(erf_path, 0)   # result table not sharded


# Execution of binary
@pytest.yield_fixture()
def workflow_negative(yt_stuff, input_data_negative, erf_output_negative):
    resources = {
        "input": input_data_negative,
        "output": erf_output_negative
    }

    with YtIndexErfTestEnv(**resources) as env:
        env.execute(ErfMode.PREPARE_DEMAND_FORECASTING, yt_stuff)
        env.verify(matchers=[HasExitCode(-6)])
        yield env


def test_result_negative(yt_stuff, workflow_negative):
    assert_that(len(workflow_negative.result_tables[0].data) == 0, 'No features because of crashing indexerf')
