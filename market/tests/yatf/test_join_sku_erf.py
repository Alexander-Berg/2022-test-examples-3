# coding=utf-8
"""
Тест используется для проверки работы вызова indexerf join_sku_erf
"""

import pytest

from hamcrest import assert_that, equal_to, has_item, has_entries

from market.idx.yatf.resources.yt_table_resource import YtTableResource

from market.idx.generation.indexerf.yatf.test_env import YtIndexErfTestEnv, ErfMode
from market.idx.generation.indexerf.yatf.resources.yt_erf_data import YTJoinSkuErfData
from market.idx.generation.indexerf.yatf.resources.erf_output import YtErfOutput

from mapreduce.yt.python.table_schema import extract_column_attributes


blue_offers_sku_path = '//indexer/stratocaster/mi3/main/idx/generation/blue_offers_sku'


blue_offers_sku_table_sharded8 = [
    {'ware_md5': 'WareId_0_1', 'part': 0, 'sku': 1},
    {'ware_md5': 'WareId_0_2', 'part': 0, 'sku': 2},
    {'ware_md5': 'WareId_0_3', 'part': 0, 'sku': 3},
    {'ware_md5': 'WareId_1_1', 'part': 1, 'sku': 1},
    {'ware_md5': 'WareId_2_2', 'part': 2, 'sku': 2},
    {'ware_md5': 'WareId_3_3', 'part': 3, 'sku': 3},
    {'ware_md5': 'WareId_3_4', 'part': 3, 'sku': 4},   # у этой скю нет фич
    {'ware_md5': 'WareId_7_2', 'part': 7, 'sku': 2},
]

blue_offers_sku_table_not_sharded = [
    {'ware_md5': 'WareId_0_1', 'part': 0, 'sku': 1},
    {'ware_md5': 'WareId_0_2', 'part': 0, 'sku': 2},
    {'ware_md5': 'WareId_0_3', 'part': 0, 'sku': 3},
    {'ware_md5': 'WareId_1_1', 'part': 0, 'sku': 1},
    {'ware_md5': 'WareId_2_2', 'part': 0, 'sku': 2},
    {'ware_md5': 'WareId_3_3', 'part': 0, 'sku': 3},
    {'ware_md5': 'WareId_3_4', 'part': 0, 'sku': 4},   # у этой скю нет фич
    {'ware_md5': 'WareId_7_2', 'part': 0, 'sku': 2},
]

expected_sku_erf_ids_data_sharded8 = [
    # shard 0
    [
        {'ware_md5': 'WareId_0_1', 'part': 0, 'sku': 1, 'sku_erf_id': 0},
        {'ware_md5': 'WareId_0_2', 'part': 0, 'sku': 2, 'sku_erf_id': 1},
        {'ware_md5': 'WareId_0_3', 'part': 0, 'sku': 3, 'sku_erf_id': 2},
    ],
    # shard 1
    [
        {'ware_md5': 'WareId_1_1', 'part': 1, 'sku': 1, 'sku_erf_id': 0},
    ],
    # shard 2
    [
        {'ware_md5': 'WareId_2_2', 'part': 2, 'sku': 2, 'sku_erf_id': 1},
    ],
    # shard 3
    [
        {'ware_md5': 'WareId_3_3', 'part': 3, 'sku': 3, 'sku_erf_id': 2},
    ],
    # shard 4, 5, 6
    [], [], [],
    # shard 7
    [
        {'ware_md5': 'WareId_7_2', 'part': 7, 'sku': 2, 'sku_erf_id': 1},
    ],
]

expected_sku_erf_ids_data_not_sharded = [
    # shard 0
    [
        {'ware_md5': 'WareId_0_1', 'part': 0, 'sku': 1, 'sku_erf_id': 0},
        {'ware_md5': 'WareId_0_2', 'part': 0, 'sku': 2, 'sku_erf_id': 1},
        {'ware_md5': 'WareId_0_3', 'part': 0, 'sku': 3, 'sku_erf_id': 2},
        {'ware_md5': 'WareId_1_1', 'part': 0, 'sku': 1, 'sku_erf_id': 0},
        {'ware_md5': 'WareId_2_2', 'part': 0, 'sku': 2, 'sku_erf_id': 1},
        {'ware_md5': 'WareId_3_3', 'part': 0, 'sku': 3, 'sku_erf_id': 2},
        {'ware_md5': 'WareId_7_2', 'part': 0, 'sku': 2, 'sku_erf_id': 1},
    ]
]


@pytest.fixture(
    scope="module",
    params=[
        (8, 8, blue_offers_sku_table_sharded8, expected_sku_erf_ids_data_sharded8),
        (0, 1, blue_offers_sku_table_not_sharded, expected_sku_erf_ids_data_not_sharded),
        (1, 1, blue_offers_sku_table_not_sharded, expected_sku_erf_ids_data_not_sharded),
    ],
    ids=[
        "SHARDED8",
        "NOT_SHARDED",
        "NOT_SHARDED2",
    ]
)
def sharding_params(request):
    return request.param


blue_offers_sku_schema = [
    dict(name='ware_md5', type='string'),
    dict(name='part', type='uint64'),
    dict(name='sku', type='uint64'),
]


@pytest.fixture(scope='module')
def blue_offers_sku_table(yt_stuff, sharding_params):
    blue_parts_count, _, blue_offers_sku_table_rows, _ = sharding_params
    path = blue_offers_sku_path + str(blue_parts_count)
    return YtTableResource(yt_stuff, path, data=blue_offers_sku_table_rows, attributes={'schema': blue_offers_sku_schema})


demand_forecasting_table_path = "//indexer/static_features/erf/custom_features/blue/demand_forecasting"

demand_forecasting_table_schema = [
    dict(name='sku', type='int64'),
    dict(name='today_demand', type='double'),
    dict(name='in_5_days_demand', type='double'),
    dict(name='in_10_days_demand', type='double'),
    dict(name='in_15_days_demand', type='double'),
    dict(name='in_20_days_demand', type='double'),
    dict(name='in_25_days_demand', type='double'),
]


@pytest.fixture(scope='module')
def demand_forecasting_table(yt_stuff, sharding_params):
    blue_parts_count, _, _ , _ = sharding_params
    path = demand_forecasting_table_path + str(blue_parts_count)
    rows = [
        {'sku': 1, 'today_demand': 1.1, 'in_5_days_demand': 1.15, 'in_10_days_demand': 1.2,
         'in_15_days_demand': 10.0, 'in_20_days_demand': 11.0, 'in_25_days_demand': 12.0},
        {'sku': 2, 'today_demand': 2.1, 'in_5_days_demand': 2.15, 'in_10_days_demand': 2.2,
         'in_15_days_demand': 20.0, 'in_20_days_demand': 21.0, 'in_25_days_demand': 22.0},
        {'sku': 3, 'today_demand': 3.1, 'in_5_days_demand': 3.15, 'in_10_days_demand': 3.2,
         'in_15_days_demand': 30.0, 'in_20_days_demand': 31.0, 'in_25_days_demand': 32.0},
        {'sku': 5, 'today_demand': 5.1, 'in_5_days_demand': 5.15, 'in_10_days_demand': 5.2,
         'in_15_days_demand': 50.0, 'in_20_days_demand': 51.0, 'in_25_days_demand': 52.0},
    ]
    return YtTableResource(yt_stuff, path, data=rows, attributes={'schema': demand_forecasting_table_schema})


@pytest.fixture(scope='module')
def input_data(blue_offers_sku_table, demand_forecasting_table, sharding_params):
    blue_parts_count, _, _, _ = sharding_params
    res = YTJoinSkuErfData(demand_forecasting_table, blue_offers_sku_table, blue_parts_count)
    return res


# output info and YT tables
@pytest.fixture(scope='module')
def erf_output(sharding_params):
    blue_parts_count, _, _, _ = sharding_params
    sku_erf_path = '//indexer/stratocaster/result-table-sku_erf' + str(blue_parts_count)
    sku_erf_ids_path = '//indexer/stratocaster/result-table-ids-sku_erf' + str(blue_parts_count)
    return YtErfOutput(sku_erf_path, blue_parts_count, sku_erf_ids_path)


# Execution of binary
@pytest.yield_fixture(scope='module')
def workflow(yt_stuff, input_data, erf_output):
    resources = {
        "input": input_data,
        "output": erf_output
    }

    with YtIndexErfTestEnv(**resources) as env:
        env.execute(ErfMode.JOIN_SKU_ERF, yt_stuff)
        env.verify()
        yield env


def should_see_only_expected_features(res_data, expected_data):
    for item in expected_data:
        assert_that(res_data, has_item(has_entries(item)), "No match for result data")
    assert_that(len(res_data), equal_to(len(expected_data)), "No match for result data length")


# Tests
def test_result_sku_erf_table_count(workflow, sharding_params):
    _, blue_parts_count_expected, _, _ = sharding_params
    assert_that(len(workflow.result_tables), equal_to(blue_parts_count_expected), "output number result tables != blue parts count")


def test_result_ids_table_count(workflow, sharding_params):
    _, blue_parts_count_expected, _, _ = sharding_params
    assert_that(len(workflow.result_ids_tables), equal_to(blue_parts_count_expected), "output number result tables != blue parts count")


def test_result_sku_erf_tables_exists(yt_stuff, workflow):
    for table in workflow.result_tables:
        assert_that(yt_stuff.get_yt_client().exists(table.get_path()), 'Result table doesn\'t exist')


def test_result_sku_erf_ids_tables_exists(yt_stuff, workflow):
    for table in workflow.result_ids_tables:
        assert_that(yt_stuff.get_yt_client().exists(table.get_path()), 'Result ids table doesn\'t exist')


def _do_test_result_ids_table_schema(table):
    assert_that(extract_column_attributes(list(table.schema)),
                equal_to([
                    {'required': False, "name": "ware_md5", "type": "string"},
                    {'required': False, "name": "part", "type": "uint64"},
                    {'required': False, "name": "sku", "type": "uint64"},
                    {'required': False, "name": "sku_erf_id", "type": "uint64"},
                ]), "Schema is incorrect")


def test_result_ids_table_schema(workflow):
    for table in workflow.result_ids_tables:
        _do_test_result_ids_table_schema(table)


def _do_test_result_table_schema(table):
    assert_that(extract_column_attributes(list(table.schema)),
                equal_to([
                    {'required': False, "name": "sku", "type": "int64"},
                    {'required': False, "name": "today_demand", "type": "double"},
                    {'required': False, "name": "in_5_days_demand", "type": "double"},
                    {'required': False, "name": "in_10_days_demand", "type": "double"},
                    {'required': False, "name": "in_15_days_demand", "type": "double"},
                    {'required': False, "name": "in_20_days_demand", "type": "double"},
                    {'required': False, "name": "in_25_days_demand", "type": "double"},
                ]), "Schema is incorrect")


def test_result_table_schema(workflow):
    for table in workflow.result_tables:
        _do_test_result_table_schema(table)


def test_sku_erf_features_result(workflow):
    """Проверяем данные в табличках для sku_erf индекса
    Сейчас в табличках для всех шардов одно и то же - скопированные фичи по предказанию спроса
    """
    expected_rows = [
        {'sku': 1, 'today_demand': 1.1, 'in_5_days_demand': 1.15, 'in_10_days_demand': 1.2,
         'in_15_days_demand': 10.0, 'in_20_days_demand': 11.0, 'in_25_days_demand': 12.0},
        {'sku': 2, 'today_demand': 2.1, 'in_5_days_demand': 2.15, 'in_10_days_demand': 2.2,
         'in_15_days_demand': 20.0, 'in_20_days_demand': 21.0, 'in_25_days_demand': 22.0},
        {'sku': 3, 'today_demand': 3.1, 'in_5_days_demand': 3.15, 'in_10_days_demand': 3.2,
         'in_15_days_demand': 30.0, 'in_20_days_demand': 31.0, 'in_25_days_demand': 32.0},
        {'sku': 5, 'today_demand': 5.1, 'in_5_days_demand': 5.15, 'in_10_days_demand': 5.2,
         'in_15_days_demand': 50.0, 'in_20_days_demand': 51.0, 'in_25_days_demand': 52.0},
    ]
    for table in workflow.result_tables:
        should_see_only_expected_features(table.data, expected_rows)


def test_sku_erf_id_result(workflow, sharding_params):
    """Проверяем таблички со связками синий оффер -> позиция его документа в sku_erf индексе
    Офферы, у которых нет sku_erf фич в этой таблице отсутствуют
    """
    _, blue_parts_count_expected, _, expected_sku_erf_ids_rows = sharding_params
    for shard_num in range(blue_parts_count_expected):
        should_see_only_expected_features(workflow.result_ids_tables[shard_num].data, expected_sku_erf_ids_rows[shard_num])
