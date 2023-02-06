#!/usr/bin/env python
# -*- coding: utf-8 -*-

import pytest
import yatest
import subprocess

from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix, YtStuff, YtConfig

YT_SERVER = None
STEP_NAME = "tentop_percent_calcer"


def setup_group_access(yt):
    indexer_group = "idm-group:69548"
    groups = yt.list("//sys/groups")
    if indexer_group not in groups:
        yt.create("group", attributes={"name": "idm-group:69548"}, force=True)


def setup_module(module):
    module.YT_SERVER = YtStuff(YtConfig(wait_tablet_cell_initialization=True))
    module.YT_SERVER.start_local_yt()
    setup_group_access(module.YT_SERVER.get_yt_client())


def create_genlog_table(yt_client, table_path):
    yt_client.create('table', table_path, recursive=True, attributes=dict(
        schema=[
            dict(name='model_id', type='uint64'),
            dict(name='cluster_id', type='uint64'),
            dict(name='market_sku', type='uint64'),
            dict(name='price', type='string'),
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
def genlog_model(test_folder):
    return "{}/genlog_model".format(test_folder)


@pytest.yield_fixture(scope="module")
def genlog_cluster(test_folder):
    return "{}/genlog_cluster".format(test_folder)


@pytest.yield_fixture(scope="module")
def genlog_msku(test_folder):
    return "{}/genlog_msku".format(test_folder)


@pytest.yield_fixture(scope="module")
def bin_work_dir(test_folder):
    return "{}/workdir".format(test_folder)


@pytest.yield_fixture(scope="module")
def currency_table(test_folder):
    return "{}/currency_rates".format(test_folder)


@pytest.yield_fixture(scope="module")
def input_data(use_msku_stats_for_blue_offer):
    result = {
        'currency': [
            {"currency_from": "RUR", "currency_to": "RUR", "rate": 1.0},
            {"currency_from": "USD", "currency_to": "RUR", "rate": 64.0},
        ],
        'genlog_model': [
            {'price': "RUR 17000000", 'model_id': 10000, 'cluster_id': None},
            {'price': "RUR 17000001", 'model_id': 10001, 'cluster_id': None},
            {'price': "RUR 17000002", 'model_id': 10001, 'cluster_id': None},
            {'price': "RUR 17000003", 'model_id': 10001, 'cluster_id': None},
            {'price': "RUR 17000004", 'model_id': 10001, 'cluster_id': None},
            {'price': "RUR 17000005", 'model_id': 10001, 'cluster_id': None},
            {'price': "RUR 17000006", 'model_id': 10001, 'cluster_id': None},
            {'price': "RUR 17000007", 'model_id': 10001, 'cluster_id': None},
            {'price': "RUR 17000008", 'model_id': 10001, 'cluster_id': None},
            {'price': "RUR 17000009", 'model_id': 10001, 'cluster_id': None},
            {'price': "RUR 17000010", 'model_id': 10001, 'cluster_id': None},
        ],
        'genlog_cluster': [
            {'price': "USD 17000000", 'model_id': None, 'cluster_id': 20000},
            {'price': "RUR 17000000", 'model_id': None, 'cluster_id': 20000},
        ],
    }

    if use_msku_stats_for_blue_offer:
        result['genlog_msku'] = [
            {'price': "RUR 11000000", 'market_sku': 30000},
            {'price': "RUR 12000000", 'market_sku': 30000},
            {'price': "RUR 17000000", 'market_sku': 30000},
        ]

    return result


@pytest.yield_fixture(scope="module")
def output_data(use_msku_stats_for_blue_offer):
    result = {
        'output_model_tentop': [
            {'model_id': 10000, 'prices': [17000000], 'tentop_percent_price': 17000000},
            {'model_id': 10001, 'prices': [17000000 + x for x in range(1, 11)], 'tentop_percent_price': 17000009},
        ],
        'output_cluster_tentop': [
            {'cluster_id': 20000, 'prices': [17000000, 1088000000], 'tentop_percent_price': 1088000000},
        ],
    }

    if use_msku_stats_for_blue_offer:
        result['output_msku_tentop'] = [
            {'market_sku': 30000, 'prices': [11000000, 12000000, 17000000], 'tentop_percent_price': 17000000},
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


def test_output(genlog_model, genlog_cluster, genlog_msku, currency_table, bin_work_dir, input_data, output_data, use_msku_stats_for_blue_offer):
    global YT_SERVER
    yt_client = YT_SERVER.get_yt_client()

    create_genlog_table(yt_client, genlog_model)
    yt_client.write_table(genlog_model, input_data["genlog_model"])
    yt_client.run_sort(genlog_model, sort_by="model_id")  # в реальности все уже отсортировано к этом моменту

    create_genlog_table(yt_client, genlog_cluster)
    yt_client.write_table(genlog_cluster, input_data["genlog_cluster"])
    yt_client.run_sort(genlog_cluster, sort_by="cluster_id")  # в реальности все уже отсортировано к этом моменту

    if use_msku_stats_for_blue_offer:
        create_genlog_table(yt_client, genlog_msku)
        yt_client.write_table(genlog_msku, input_data["genlog_msku"])
        yt_client.run_sort(genlog_msku, sort_by="market_sku")  # в реальности все уже отсортировано к этом моменту

    create_currency_table(yt_client, currency_table)
    yt_client.write_table(currency_table, input_data["currency"])

    cmdlist = [
        yatest.common.binary_path('market/idx/promos/pricedrops/bin/yt_pricedrops_cpp'),
        "--specific-step", "tentop_percent_calcer",
        "--server", YT_SERVER.get_server(),
        "--genlog-model-sorted", genlog_model,
        "--genlog-cluster-sorted", genlog_cluster,
        "--genlog-msku-sorted", genlog_msku,
        "--working-dir", bin_work_dir,
        "--mstat-currency-table-path", currency_table,
        "--use-msku-stats-for-blue-offer", str(use_msku_stats_for_blue_offer)
    ]
    run_bin(cmdlist)

    click_stats_calcer_tables = yt_client.list("{}/{}".format(bin_work_dir, STEP_NAME))
    if use_msku_stats_for_blue_offer:
        assert len(click_stats_calcer_tables) == 3
    else:
        assert len(click_stats_calcer_tables) == 2

    model_path = ""
    cluster_path = ""
    msku_path = ""
    for path in click_stats_calcer_tables:
        if "model_tentop_price_" in path:
            model_path = path
        if "cluster_tentop_price_" in path:
            cluster_path = path
        if "msku_tentop_price_" in path:
            msku_path = path
    assert model_path and cluster_path
    if use_msku_stats_for_blue_offer:
        assert msku_path

    check_table(output_data['output_model_tentop'], yt_client.read_table("{}/{}/{}".format(bin_work_dir, STEP_NAME, model_path)))
    check_table(output_data['output_cluster_tentop'], yt_client.read_table("{}/{}/{}".format(bin_work_dir, STEP_NAME, cluster_path)))

    if use_msku_stats_for_blue_offer:
        check_table(output_data['output_msku_tentop'], yt_client.read_table("{}/{}/{}".format(bin_work_dir, STEP_NAME, msku_path)))
