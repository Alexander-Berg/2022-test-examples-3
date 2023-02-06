#!/usr/bin/env python
# -*- coding: utf-8 -*-

import pytest
import yatest
import subprocess
import datetime

from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix, YtStuff, YtConfig
STEP_NAME = "clicks_stats_calcer"
BERU_VIRTUAL_FEED_ID = 475690

YT_SERVER = None


def setup_group_access(yt):
    indexer_group = "idm-group:69548"
    groups = yt.list("//sys/groups")
    if indexer_group not in groups:
        yt.create("group", attributes={"name": "idm-group:69548"}, force=True)


def setup_module(module):
    module.YT_SERVER = YtStuff(YtConfig(wait_tablet_cell_initialization=True))
    module.YT_SERVER.start_local_yt()
    setup_group_access(module.YT_SERVER.get_yt_client())


def create_clicks_table(yt_client, table_path):
    yt_client.create('table', table_path, recursive=True, force=True, attributes=dict(
        schema=[
            dict(name='filter', type='int64'),
            dict(name='click_type_id', type='uint64'),
            dict(name='state', type='int64'),
            dict(name='feed_id', type='string'),
            dict(name='offer_id', type='string'),
            dict(name='offer_price', type='uint64'),
        ]
    ))


def create_genlog_table(yt_client, table_path):
    yt_client.create('table', table_path, recursive=True, attributes=dict(
        schema=[
            dict(name='model_id', type='uint64'),
            dict(name='cluster_id', type='uint64'),
            dict(name='market_sku', type='uint64'),
            dict(name='feed_id', type='uint64'),
            dict(name='offer_id', type='string'),
        ]
    ))


def create_currency_table(yt_client, table_path):
    yt_client.create('table', table_path, recursive=True, attributes=dict(
        schema=[
            dict(name='currency_from', type='string'),
            dict(name='currency_to', type='string'),
            dict(name='rate', type='double'),
        ]
    ))


@pytest.fixture(scope='module', params=[0, 1])
def use_msku_stats_for_blue_offer(request):
    return request.param


@pytest.yield_fixture(scope="module")
def test_folder(use_msku_stats_for_blue_offer):
    return "{}/{}/workdir".format(get_yt_prefix(), use_msku_stats_for_blue_offer)


@pytest.yield_fixture(scope="module")
def clicks_dir(test_folder):
    return "{}/clicks".format(test_folder)


@pytest.yield_fixture(scope="module")
def click_tables(clicks_dir):
    return ["{}/{}".format(clicks_dir, (datetime.datetime.now() - datetime.timedelta(days=diff)).strftime("%Y-%m-%d")) for diff in range(2, 4)]


@pytest.yield_fixture(scope="module")
def genlog_table(test_folder):
    return "{}/genlog".format(test_folder)


@pytest.yield_fixture(scope="module")
def currency_table(test_folder):
    return "{}/currency".format(test_folder)


@pytest.yield_fixture(scope="module")
def bin_work_dir(test_folder):
    return "{}/workdir".format(test_folder)


@pytest.yield_fixture(scope="module")
def zip_clicks():
    return "2"


@pytest.yield_fixture(scope="module")
def input_data():
    return {
        'clicks': [
            [
                {'feed_id': '1205', 'offer_id': '00001', 'offer_price': 1700, 'filter': 0, 'click_type_id': 0, 'state': 1},
                {'feed_id': '1205', 'offer_id': '00002', 'offer_price': 1800, 'filter': 0, 'click_type_id': 0, 'state': 1},
                {'feed_id': '1205', 'offer_id': '00002', 'offer_price': 1900, 'filter': 0, 'click_type_id': 0, 'state': 1},
                # выкинем потому что клик невалидный
                {'feed_id': '1205', 'offer_id': '00001', 'offer_price': 1700, 'filter': 1, 'click_type_id': 0, 'state': 1},
                {'feed_id': '1205', 'offer_id': '00001', 'offer_price': 1700, 'filter': 0, 'click_type_id': 1, 'state': 1},
                {'feed_id': '1205', 'offer_id': '00001', 'offer_price': 1700, 'filter': 0, 'click_type_id': 0, 'state': 0},
                # просто какой-то левый оффер без модели и кластера
                {'feed_id': '1205', 'offer_id': '00000', 'offer_price': 1700, 'filter': 0, 'click_type_id': 0, 'state': 1},
                {'feed_id': str(BERU_VIRTUAL_FEED_ID), 'offer_id': '30001', 'offer_price': 1000, 'filter': 0, 'click_type_id': 0, 'state': 1},
            ], [
                {'feed_id': '1205', 'offer_id': '00002', 'offer_price': 1900, 'filter': 0, 'click_type_id': 0, 'state': 1},
                {'feed_id': '1205', 'offer_id': '00003', 'offer_price': 1400, 'filter': 0, 'click_type_id': 0, 'state': 1},
                {'feed_id': '1205', 'offer_id': '00003', 'offer_price': 1500, 'filter': 0, 'click_type_id': 0, 'state': 1},
                {'feed_id': '1205', 'offer_id': '00004', 'offer_price': 1600, 'filter': 0, 'click_type_id': 0, 'state': 1},
                {'feed_id': str(BERU_VIRTUAL_FEED_ID), 'offer_id': '30000', 'offer_price': 1100, 'filter': 0, 'click_type_id': 0, 'state': 1},
                {'feed_id': str(BERU_VIRTUAL_FEED_ID), 'offer_id': '30001', 'offer_price': 1200, 'filter': 0, 'click_type_id': 0, 'state': 1},
            ]
        ],
        'genlog': [
            {'feed_id': 1205, 'offer_id': '00000', 'model_id': None, 'cluster_id': None},  # 1 клик
            {'feed_id': 1205, 'offer_id': '00001', 'model_id': 10000, 'cluster_id': None},  # 1 клик
            {'feed_id': 1205, 'offer_id': '00002', 'model_id': 10001, 'cluster_id': None},  # 3 клика
            {'feed_id': 1205, 'offer_id': '00003', 'model_id': 10001, 'cluster_id': None},  # 2 клика
            # у офера 00004 есть msku но в стату для msku он не попадет потому что он не синий
            {'feed_id': 1205, 'offer_id': '00004', 'model_id': None, 'cluster_id': 20000, 'market_sku': 30000},  # 1 клик
            {'feed_id': BERU_VIRTUAL_FEED_ID, 'offer_id': '30000', 'market_sku': 30000, 'model_id': 10001},  # 1 клик
            {'feed_id': BERU_VIRTUAL_FEED_ID, 'offer_id': '30001', 'market_sku': 30001},  # 2 клика
        ]
    }


@pytest.yield_fixture(scope="module")
def output_data(use_msku_stats_for_blue_offer):
    result = {
        'output_model_offer_stats': [
            {'feed_id': 1205, 'offer_id': '00001', 'clicks_count': 1, 'clicks_prices': [x * pow(10, 7) for x in [1700]], 'model_id': 10000},
            {'feed_id': 1205, 'offer_id': '00002', 'clicks_count': 3, 'clicks_prices': [x * pow(10, 7) for x in [1800, 1900, 1900]], 'model_id': 10001},
            {'feed_id': 1205, 'offer_id': '00003', 'clicks_count': 2, 'clicks_prices': [x * pow(10, 7) for x in [1400, 1500]], 'model_id': 10001},
        ],
        'output_cluster_offer_stats': [
            {'feed_id': 1205, 'offer_id': '00004', 'clicks_count': 1, 'clicks_prices': [x * pow(10, 7) for x in [1600]], 'cluster_id': 20000},
        ],
        'output_simple_offer_stats': [
            {'feed_id': 1205, 'offer_id': '00000', 'clicks_count': 1, 'clicks_prices': [x * pow(10, 7) for x in [1700]]},
        ],
        'output_model_stats': [
            {'model_id': 10000, 'clicks_prices': [x * pow(10, 7) for x in [1700]], 'max_clicks_count_on_offer': 1, 'sum_clicks_count': 1, 'zip_clicks': 1},
        ],
        'output_cluster_stats': [
            {'cluster_id': 20000, 'clicks_prices': [x * pow(10, 7) for x in [1600]], 'max_clicks_count_on_offer': 1, 'sum_clicks_count': 1, 'zip_clicks': 1},
        ]
    }

    if use_msku_stats_for_blue_offer:
        result['output_msku_offer_stats'] = [
            {'feed_id': BERU_VIRTUAL_FEED_ID, 'offer_id': '30000', 'clicks_count': 1, 'clicks_prices': [x * pow(10, 7) for x in [1100]], 'market_sku': 30000},
            {'feed_id': BERU_VIRTUAL_FEED_ID, 'offer_id': '30001', 'clicks_count': 2, 'clicks_prices': [x * pow(10, 7) for x in [1000, 1200]], 'market_sku': 30001},
        ]
        result['output_msku_stats'] = [
            {'market_sku': 30000, 'clicks_prices': [x * pow(10, 7) for x in [1100]], 'max_clicks_count_on_offer': 1, 'sum_clicks_count': 1, 'zip_clicks': 1},
            {'market_sku': 30001, 'clicks_prices': [x * pow(10, 7) for x in [1000, 1200]], 'max_clicks_count_on_offer': 2, 'sum_clicks_count': 2, 'zip_clicks': 1},
        ]
        result['output_model_stats'] += [
            {'model_id': 10001, 'clicks_prices': [x * pow(10, 7) for x in [1400, 1800]], 'max_clicks_count_on_offer': 3, 'sum_clicks_count': 5, 'zip_clicks': 2}
        ]

    else:
        result['output_model_offer_stats'] += [
            {'feed_id': BERU_VIRTUAL_FEED_ID, 'offer_id': '30000', 'clicks_count': 1, 'clicks_prices': [x * pow(10, 7) for x in [1100]], 'model_id': 10001},
        ]
        result['output_simple_offer_stats'] += [
            {'feed_id': BERU_VIRTUAL_FEED_ID, 'offer_id': '30001', 'clicks_count': 2, 'clicks_prices': [x * pow(10, 7) for x in [1000, 1200]]},
        ]
        result['output_model_stats'] += [
            {'model_id': 10001, 'clicks_prices': [x * pow(10, 7) for x in [1100, 1800]], 'max_clicks_count_on_offer': 3, 'sum_clicks_count': 6, 'zip_clicks': 3}
        ]

    return result


def run_bin(cmdlist):
    try:
        res = subprocess.check_output(cmdlist)
        print res
    except Exception as e:
        print e
        raise


def check_table(expected, table):
    given = [x for x in table]
    assert len(expected) == len(given)
    for i in range(0, len(expected)):
        for key, value in expected[i].items():
            if isinstance(value, list):
                assert set(value) == set(given[i][key])
            else:
                assert value == given[i][key]


def test_output(clicks_dir, click_tables, genlog_table, currency_table, bin_work_dir, zip_clicks, use_msku_stats_for_blue_offer, input_data, output_data):
    global YT_SERVER
    yt_client = YT_SERVER.get_yt_client()

    click_tbl_idx = 0
    for tbl in click_tables:
        create_clicks_table(yt_client, tbl)
        yt_client.write_table(tbl, input_data["clicks"][click_tbl_idx])
        click_tbl_idx += 1

    create_genlog_table(yt_client, genlog_table)
    yt_client.write_table(genlog_table, input_data["genlog"])

    create_currency_table(yt_client, currency_table)

    cmdlist = [
        yatest.common.binary_path('market/idx/promos/pricedrops/bin/yt_pricedrops_cpp'),
        "--specific-step", "clicks_stats_calcer",
        "--server", YT_SERVER.get_server(),
        "--genlog-table-path", genlog_table,
        "--clicks-table-dir", clicks_dir,
        "--mstat-currency-table-path", currency_table,
        "--working-dir", bin_work_dir,
        "--zip-clicks", zip_clicks,
        "--use-msku-stats-for-blue-offer", str(use_msku_stats_for_blue_offer)
    ]
    run_bin(cmdlist)

    click_stats_calcer_tables = yt_client.list("{}/{}".format(bin_work_dir, STEP_NAME))
    if use_msku_stats_for_blue_offer:
        assert len(click_stats_calcer_tables) == 11  # model_offer, cluster_offer, msku_offer, simple_offer, model, cluster, msku + 3 genlogs
    else:
        assert len(click_stats_calcer_tables) == 10  # model_offer, cluster_offer, simple_offer, model, cluster + 3 genlogs + genlog_msku + msku_offer

    model_offer_path = ""
    cluster_offer_path = ""
    msku_offer_path = ""
    simple_offer_path = ""
    model_path = ""
    cluster_path = ""
    msku_path = ""
    for path in click_stats_calcer_tables:
        if "model_offer_clicks_stats_" in path:
            model_offer_path = path
        if "cluster_offer_clicks_stats_" in path:
            cluster_offer_path = path
        if "msku_offer_clicks_stats_" in path:
            msku_offer_path = path
        if "simple_offer_clicks_stats_" in path:
            simple_offer_path = path
        if "model_clicks_stats_" in path:
            model_path = path
        if "cluster_clicks_stats_" in path:
            cluster_path = path
        if "msku_clicks_stats_" in path:
            msku_path = path
    assert model_offer_path and simple_offer_path and cluster_offer_path and model_path and cluster_path
    if use_msku_stats_for_blue_offer:
        assert msku_offer_path and msku_path

    check_table(output_data['output_model_offer_stats'], yt_client.read_table("{}/{}/{}".format(bin_work_dir, STEP_NAME, model_offer_path)))
    check_table(output_data['output_cluster_offer_stats'], yt_client.read_table("{}/{}/{}".format(bin_work_dir, STEP_NAME, cluster_offer_path)))
    check_table(output_data['output_simple_offer_stats'], yt_client.read_table("{}/{}/{}".format(bin_work_dir, STEP_NAME, simple_offer_path)))
    check_table(output_data['output_model_stats'], yt_client.read_table("{}/{}/{}".format(bin_work_dir, STEP_NAME, model_path)))
    check_table(output_data['output_cluster_stats'], yt_client.read_table("{}/{}/{}".format(bin_work_dir, STEP_NAME, cluster_path)))

    if use_msku_stats_for_blue_offer:
        check_table(output_data['output_msku_offer_stats'], yt_client.read_table("{}/{}/{}".format(bin_work_dir, STEP_NAME, msku_offer_path)))
        check_table(output_data['output_msku_stats'], yt_client.read_table("{}/{}/{}".format(bin_work_dir, STEP_NAME, msku_path)))
