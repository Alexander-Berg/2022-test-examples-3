#!/usr/bin/env python
# -*- coding: utf-8 -*-

import pytest
import yatest
import subprocess

from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix, YtStuff, YtConfig

YT_SERVER = None
STEP_NAME = "offers_data_collector"
BERU_VIRTUAL_FEED_ID = 475690


def setup_group_access(yt):
    indexer_group = "idm-group:69548"
    groups = yt.list("//sys/groups")
    if indexer_group not in groups:
        yt.create("group", attributes={"name": "idm-group:69548"}, force=True)


def setup_module(module):
    module.YT_SERVER = YtStuff(YtConfig(wait_tablet_cell_initialization=True))
    module.YT_SERVER.start_local_yt()
    setup_group_access(module.YT_SERVER.get_yt_client())


def create_table(yt_client, table_path, data):
    yt_client.create('table', table_path, recursive=True, attributes=dict(
        schema=data["schema"]
    ))
    yt_client.write_table(table_path, data["rows"])


@pytest.fixture(scope='module', params=[0, 1])
def use_msku_stats_for_blue_offer(request):
    return request.param


@pytest.yield_fixture(scope="module")
def test_folder(use_msku_stats_for_blue_offer):
    return "{}/{}/workdir".format(get_yt_prefix(), use_msku_stats_for_blue_offer)


@pytest.yield_fixture(scope="module")
def bin_work_dir(test_folder):
    return "{}/workdir".format(test_folder)


@pytest.yield_fixture(scope="module")
def currency_table(test_folder):
    return "{}/currency_rates".format(test_folder)


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
def genlog_simple(test_folder):
    return "{}/genlog_simple".format(test_folder)


@pytest.yield_fixture(scope="module")
def price_history(test_folder):
    return "{}/price_history".format(test_folder)


@pytest.yield_fixture(scope="module")
def category_conversion(test_folder):
    return "{}/category_conversion".format(test_folder)


@pytest.yield_fixture(scope="module")
def model_offer_clicks_stats(test_folder):
    return "{}/model_offer_clicks_stats".format(test_folder)


@pytest.yield_fixture(scope="module")
def cluster_offer_clicks_stats(test_folder):
    return "{}/cluster_offer_clicks_stats".format(test_folder)


@pytest.yield_fixture(scope="module")
def msku_offer_clicks_stats(test_folder):
    return "{}/msku_offer_clicks_stats".format(test_folder)


@pytest.yield_fixture(scope="module")
def simple_offer_clicks_stats(test_folder):
    return "{}/simple_offer_clicks_stats".format(test_folder)


@pytest.yield_fixture(scope="module")
def model_clicks_stats(test_folder):
    return "{}/model_clicks_stats".format(test_folder)


@pytest.yield_fixture(scope="module")
def cluster_clicks_stats(test_folder):
    return "{}/cluster_clicks_stats".format(test_folder)


@pytest.yield_fixture(scope="module")
def msku_clicks_stats(test_folder):
    return "{}/msku_clicks_stats".format(test_folder)


@pytest.yield_fixture(scope="module")
def model_tentop(test_folder):
    return "{}/model_tentop".format(test_folder)


@pytest.yield_fixture(scope="module")
def cluster_tentop(test_folder):
    return "{}/cluster_tentop".format(test_folder)


@pytest.yield_fixture(scope="module")
def msku_tentop(test_folder):
    return "{}/msku_tentop".format(test_folder)


@pytest.yield_fixture(scope="module")
def input_data(use_msku_stats_for_blue_offer):
    result = {
        'currency': {
            'schema': [
                dict(name='currency_from', type='string'),
                dict(name='currency_to', type='string'),
                dict(name='rate', type='double'),
            ],
            'rows': [
                {"currency_from": "RUR", "currency_to": "RUR", "rate": 1.0},
            ]
        },
        'genlog_model_sorted': {
            'schema': [
                dict(name='feed_id', type='uint64'),
                dict(name='offer_id', type='string'),
                dict(name='category_id', type='uint64'),
                dict(name='price', type='string'),
                dict(name='model_id', type='uint64'),
                dict(name='cluster_id', type='uint64'),
            ],
            'rows': [
                {'feed_id': 1000, 'offer_id': '1111', 'category_id': 1111, 'price': "RUR 1230000000", 'model_id': 10000, 'cluster_id': None},  # историческая цена больше текущей
                {'feed_id': 1000, 'offer_id': '2222', 'category_id': 2222, 'price': "RUR 2460000000", 'model_id': 10000, 'cluster_id': None},  # историческая цена меньше текущей
                {'feed_id': 1000, 'offer_id': '3333', 'category_id': 2222, 'price': "RUR 2460000000", 'model_id': 10000, 'cluster_id': None},  # нет исторической цены
            ]
        },
        'genlog_cluster_sorted': {
            'schema': [
                dict(name='feed_id', type='uint64'),
                dict(name='offer_id', type='string'),
                dict(name='category_id', type='uint64'),
                dict(name='price', type='string'),
                dict(name='model_id', type='uint64'),
                dict(name='cluster_id', type='uint64'),
            ],
            'rows': [
                {'feed_id': 1000, 'offer_id': '4444', 'category_id': 3333, 'price': "RUR 1230000000", 'model_id': None, 'cluster_id': 10000},  # истор больше
                {'feed_id': 1000, 'offer_id': '5555', 'category_id': 4444, 'price': "RUR 1230000000", 'model_id': None, 'cluster_id': 20000},  # истор больше
            ]
        },
        'genlog_simple_sorted': {
            'schema': [
                dict(name='feed_id', type='uint64'),
                dict(name='offer_id', type='string'),
                dict(name='category_id', type='uint64'),
                dict(name='price', type='string'),
                dict(name='model_id', type='uint64'),
                dict(name='cluster_id', type='uint64'),
            ],
            'rows': [
                {'feed_id': 1000, 'offer_id': '6666', 'category_id': 5555, 'price': "RUR 1250000000", 'model_id': None, 'cluster_id': None},  # простой оффер, историческая цена есть
                {'feed_id': 1000, 'offer_id': '7777', 'category_id': 5555, 'price': "RUR 1250000000", 'model_id': None, 'cluster_id': None},  # простой оффер, историческая цена нет
            ]
        },
        'price_history': {
            'schema': [
                dict(name='feed_id', type='uint64'),
                dict(name='offer_id', type='string'),
                dict(name='history_price', type='int64'),
            ],
            'rows': [
                {'feed_id': 1000, 'offer_id': '1111', 'history_price': 1530000000},
                {'feed_id': 1000, 'offer_id': '2222', 'history_price': 1530000000},
                {'feed_id': 1000, 'offer_id': '4444', 'history_price': 1530000000},
                {'feed_id': 1000, 'offer_id': '5555', 'history_price': 1530000000},
                {'feed_id': 1000, 'offer_id': '6666', 'history_price': 1730000000},
            ]
        },
        'category_conversion': {
            'schema': [
                dict(name='category_hid', type='int64'),
                dict(name='smoothed_conversion', type='double'),
            ],
            'rows': [
                {'category_hid': 1111, 'smoothed_conversion': 0.1},
                {'category_hid': 2222, 'smoothed_conversion': 0.2},
                {'category_hid': 3333, 'smoothed_conversion': 0.5},
                {'category_hid': 4444, 'smoothed_conversion': 0.5},
            ]
        },
        'model_offer_clicks_stats': {
            'schema': [
                dict(name='feed_id', type='uint64'),
                dict(name='offer_id', type='string'),
                dict(name='clicks_count', type='uint64'),
                dict(name='clicks_prices', type='any'),
                dict(name='model_id', type='uint64'),
            ],
            'rows': [
                {'feed_id': 1000, 'offer_id': '1111', 'clicks_count': 1, 'clicks_prices': [1230000000], 'model_id': 1000},
                {'feed_id': 1000, 'offer_id': '2222', 'clicks_count': 2, 'clicks_prices': [1530000000, 2460000000], 'model_id': 1000},
                {'feed_id': 1000, 'offer_id': '3333', 'clicks_count': 3, 'clicks_prices': [1230000000, 1530000000, 2460000000], 'model_id': 1000},
            ]
        },
        'cluster_offer_clicks_stats': {
            'schema': [
                dict(name='feed_id', type='uint64'),
                dict(name='offer_id', type='string'),
                dict(name='clicks_count', type='uint64'),
                dict(name='clicks_prices', type='any'),
                dict(name='cluster_id', type='uint64'),
            ],
            'rows': [
                {'feed_id': 1000, 'offer_id': '4444', 'clicks_count': 1, 'clicks_prices': [1230000000], 'cluster_id': 10000},
                {'feed_id': 1000, 'offer_id': '5555', 'clicks_count': 2, 'clicks_prices': [1230000000, 1230000000], 'cluster_id': 20000},
            ]
        },
        'simple_offer_clicks_stats': {
            'schema': [
                dict(name='feed_id', type='uint64'),
                dict(name='offer_id', type='string'),
                dict(name='clicks_count', type='uint64'),
                dict(name='clicks_prices', type='any'),
            ],
            'rows': [
                {'feed_id': 1000, 'offer_id': '6666', 'clicks_count': 1, 'clicks_prices': [1240000000]},
                {'feed_id': 1000, 'offer_id': '7777', 'clicks_count': 2, 'clicks_prices': [1250000000, 1260000000]},
            ]
        },
        'model_clicks_stats': {
            'schema': [
                dict(name='model_id', type='uint64'),
                dict(name='clicks_prices', type='any'),
                dict(name='max_clicks_count_on_offer', type='uint64'),
                dict(name='sum_clicks_count', type='uint64'),
                dict(name='zip_clicks', type='uint64'),
            ],
            'rows': [
                {'model_id': 10000, 'clicks_prices': [1230000000, 1530000000], 'max_clicks_count_on_offer': 3, 'sum_clicks_count': 6, 'zip_clicks': 3},
            ]
        },
        'cluster_clicks_stats': {
            'schema': [
                dict(name='cluster_id', type='uint64'),
                dict(name='clicks_prices', type='any'),
                dict(name='max_clicks_count_on_offer', type='uint64'),
                dict(name='sum_clicks_count', type='uint64'),
                dict(name='zip_clicks', type='uint64'),
            ],
            'rows': [
                {'cluster_id': 10000, 'clicks_prices': [1230000000], 'max_clicks_count_on_offer': 1, 'sum_clicks_count': 1, 'zip_clicks': 1},
                {'cluster_id': 20000, 'clicks_prices': [1230000000, 1230000000], 'max_clicks_count_on_offer': 2, 'sum_clicks_count': 2, 'zip_clicks': 1},
            ]
        },
        'model_tentop': {
            'schema': [
                dict(name='model_id', type='uint64'),
                dict(name='tentop_percent_price', type='uint64'),
            ],
            'rows': [
                {'model_id': 10000, 'tentop_percent_price': 2560000000},
            ]
        },
        'cluster_tentop': {
            'schema': [
                dict(name='cluster_id', type='uint64'),
                dict(name='tentop_percent_price', type='uint64'),
            ],
            'rows': [
                {'cluster_id': 10000, 'tentop_percent_price': 2560000000},
                {'cluster_id': 20000, 'tentop_percent_price': 2560000000},
            ]
        },
    }

    if use_msku_stats_for_blue_offer:
        result['price_history']['rows'] += [
            {'feed_id': BERU_VIRTUAL_FEED_ID, 'offer_id': '30000', 'history_price': 1230000000},
            {'feed_id': BERU_VIRTUAL_FEED_ID, 'offer_id': '30001', 'history_price': 1230000000},
        ]

        result['genlog_msku_sorted'] = {
            'schema': [
                dict(name='feed_id', type='uint64'),
                dict(name='offer_id', type='string'),
                dict(name='category_id', type='uint64'),
                dict(name='price', type='string'),
                dict(name='market_sku', type='uint64'),
            ],
            'rows': [
                {'feed_id': BERU_VIRTUAL_FEED_ID, 'offer_id': '30000', 'category_id': 3333, 'price': "RUR 1230000000", 'market_sku': 30000},
                {'feed_id': BERU_VIRTUAL_FEED_ID, 'offer_id': '30001', 'category_id': 4444, 'price': "RUR 1240000000", 'market_sku': 30001},
            ]
        }

        result['msku_offer_clicks_stats'] = {
            'schema': [
                dict(name='feed_id', type='uint64'),
                dict(name='offer_id', type='string'),
                dict(name='clicks_count', type='uint64'),
                dict(name='clicks_prices', type='any'),
                dict(name='market_sku', type='uint64'),
            ],
            'rows': [
                {'feed_id': BERU_VIRTUAL_FEED_ID, 'offer_id': '30000', 'clicks_count': 1, 'clicks_prices': [1230000000], 'market_sku': 30000},
                {'feed_id': BERU_VIRTUAL_FEED_ID, 'offer_id': '30001', 'clicks_count': 2, 'clicks_prices': [1230000000, 1240000000], 'market_sku': 30001},
            ]
        }

        result['msku_clicks_stats'] = {
            'schema': [
                dict(name='market_sku', type='uint64'),
                dict(name='clicks_prices', type='any'),
                dict(name='max_clicks_count_on_offer', type='uint64'),
                dict(name='sum_clicks_count', type='uint64'),
                dict(name='zip_clicks', type='uint64'),
            ],
            'rows': [
                {'market_sku': 30000, 'clicks_prices': [1230000000], 'max_clicks_count_on_offer': 10, 'sum_clicks_count': 100, 'zip_clicks': 1},
                {'market_sku': 30001, 'clicks_prices': [1230000000, 1240000000], 'max_clicks_count_on_offer': 20, 'sum_clicks_count': 200, 'zip_clicks': 1},
            ]
        }

        result['msku_tentop'] = {
            'schema': [
                dict(name='market_sku', type='uint64'),
                dict(name='tentop_percent_price', type='uint64'),
            ],
            'rows': [
                {'market_sku': 30000, 'tentop_percent_price': 1560000000},
                {'market_sku': 30001, 'tentop_percent_price': 1560000000},
            ]
        }

    return result


@pytest.yield_fixture(scope="module")
def output_data(use_msku_stats_for_blue_offer):
    result = {
        'output_model_offer_data': [
            {
                'feed_id': 1000, 'offer_id': '1111', 'clicks_count': 1,
                'clicks_prices': [1230000000, 1530000000], 'clicks_to_buyout': 10, 'history_price': 1530000000,
                'max_clicks_count_on_offer': 3, 'model_id': 10000, 'price': 'RUR 1230000000',
                'pricedrops_max_price': 1453500000, 'pricerange_max_price': 1530000000, 'sum_clicks_count': 6, 'tentop_percent_price':2560000000,
                'valid_history_price': 1530000000, 'recommended_price': 1453500000,
            },
            {
                'feed_id': 1000, 'offer_id': '2222', 'clicks_count': 2,
                'clicks_prices': [1230000000, 1530000000], 'clicks_to_buyout': 5, 'history_price': 1530000000,
                'max_clicks_count_on_offer': 3, 'model_id': 10000, 'price': 'RUR 2460000000',
                'pricedrops_max_price': 1453500000, 'pricerange_max_price': 1530000000, 'sum_clicks_count': 6, 'tentop_percent_price':2560000000,
                'valid_history_price': 1530000000, 'recommended_price': 1453500000,
            },
            {
                'feed_id': 1000, 'offer_id': '3333', 'clicks_count': 3,
                'clicks_prices': [1230000000, 1530000000], 'clicks_to_buyout': 5, 'history_price': 0,
                'max_clicks_count_on_offer': 3, 'model_id': 10000, 'price': 'RUR 2460000000',
                'pricedrops_max_price': 0, 'pricerange_max_price': 1230000000, 'sum_clicks_count': 6, 'tentop_percent_price':2560000000,
                'valid_history_price': 1530000000, 'recommended_price': 0,
            },
        ],
        'output_cluster_offer_data': [
            {
                'feed_id': 1000, 'offer_id': '4444', 'clicks_count': 1,
                'clicks_prices': [1230000000], 'clicks_to_buyout': 2, 'history_price': 1530000000,
                'max_clicks_count_on_offer': 1, 'cluster_id': 10000, 'price': 'RUR 1230000000',
                'pricedrops_max_price': 1453500000L, 'pricerange_max_price': 1230000000L, 'sum_clicks_count': 1, 'tentop_percent_price':2560000000,
                'valid_history_price': 1230000000L, 'recommended_price': 1230000000L,
            },
            {
                'feed_id': 1000, 'offer_id': '5555', 'clicks_count': 2,
                'clicks_prices': [1230000000, 1230000000], 'clicks_to_buyout': 2, 'history_price': 1530000000,
                'max_clicks_count_on_offer': 2, 'cluster_id': 20000, 'price': 'RUR 1230000000',
                'pricedrops_max_price': 1453500000L, 'pricerange_max_price': 1230000000L, 'sum_clicks_count': 2, 'tentop_percent_price':2560000000,
                'valid_history_price': 1230000001L, 'recommended_price': 1230000000L,
            },
        ],
        'output_simple_offer_data': [
            {
                'feed_id': 1000, 'offer_id': '6666', 'clicks_count': 1, 'price': 'RUR 1250000000', 'history_price': 1730000000,
                'valid_history_price': 1730000001, 'recommended_price': 1643500000
            },
            {
                'feed_id': 1000, 'offer_id': '7777', 'clicks_count': 2, 'price': 'RUR 1250000000', 'history_price': 0,
                'valid_history_price': 1, 'recommended_price': 0
            },
        ],
    }

    if use_msku_stats_for_blue_offer:
        result['output_msku_offer_data'] = [
            {
                'feed_id': BERU_VIRTUAL_FEED_ID, 'offer_id': '30000', 'clicks_count': 1,
                'clicks_prices': [1230000000], 'clicks_to_buyout': 2, 'history_price': 1230000000,
                'max_clicks_count_on_offer': 10, 'market_sku': 30000, 'price': 'RUR 1230000000',
                'pricedrops_max_price': 1168500000, 'pricerange_max_price': 1230000000L, 'sum_clicks_count': 100, 'tentop_percent_price':1560000000,
                'valid_history_price': 1230000000, 'recommended_price': 1168500000,
            },
            {
                'feed_id': BERU_VIRTUAL_FEED_ID, 'offer_id': '30001', 'clicks_count': 2,
                'clicks_prices': [1230000000, 1240000000], 'clicks_to_buyout': 2, 'history_price': 1230000000,
                'max_clicks_count_on_offer': 20, 'market_sku': 30001, 'price': 'RUR 1240000000',
                'pricedrops_max_price': 1168500000, 'pricerange_max_price': 1230000000L, 'sum_clicks_count': 200, 'tentop_percent_price':1560000000,
                'valid_history_price': 1240000000, 'recommended_price': 1168500000,
            },
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
            rw = ""
            for k, v in given[i].items():
                rw += str(k) + ": " + str(v) + "\n"
            if isinstance(value, list):
                assert set(value) == set(given[i][key]), rw
            else:

                assert value == given[i][key], rw


def test_output(bin_work_dir, currency_table, genlog_model, genlog_cluster, genlog_msku, genlog_simple, price_history, category_conversion,
                model_offer_clicks_stats, cluster_offer_clicks_stats, msku_offer_clicks_stats, simple_offer_clicks_stats, model_clicks_stats,
                cluster_clicks_stats, msku_clicks_stats, model_tentop, cluster_tentop, msku_tentop, input_data, output_data,
                use_msku_stats_for_blue_offer):
    global YT_SERVER
    yt_client = YT_SERVER.get_yt_client()

    create_table(yt_client, currency_table, input_data['currency'])
    create_table(yt_client, genlog_model, input_data['genlog_model_sorted'])
    create_table(yt_client, genlog_cluster, input_data['genlog_cluster_sorted'])
    create_table(yt_client, genlog_simple, input_data['genlog_simple_sorted'])
    create_table(yt_client, price_history, input_data['price_history'])
    create_table(yt_client, category_conversion, input_data['category_conversion'])
    create_table(yt_client, model_offer_clicks_stats, input_data['model_offer_clicks_stats'])
    create_table(yt_client, cluster_offer_clicks_stats, input_data['cluster_offer_clicks_stats'])
    create_table(yt_client, simple_offer_clicks_stats, input_data['simple_offer_clicks_stats'])
    create_table(yt_client, model_clicks_stats, input_data['model_clicks_stats'])
    create_table(yt_client, cluster_clicks_stats, input_data['cluster_clicks_stats'])
    create_table(yt_client, model_tentop, input_data['model_tentop'])
    create_table(yt_client, cluster_tentop, input_data['cluster_tentop'])

    print('zxcv %s' % use_msku_stats_for_blue_offer)
    print(price_history)

    if use_msku_stats_for_blue_offer:
        create_table(yt_client, genlog_msku, input_data['genlog_msku_sorted'])
        create_table(yt_client, msku_offer_clicks_stats, input_data['msku_offer_clicks_stats'])
        create_table(yt_client, msku_clicks_stats, input_data['msku_clicks_stats'])
        create_table(yt_client, msku_tentop, input_data['msku_tentop'])

    yt_client.run_sort(genlog_model, sort_by="model_id")  # в реальности все уже отсортировано к этом моменту
    yt_client.run_sort(genlog_cluster, sort_by="cluster_id")  # в реальности все уже отсортировано к этом моменту
    yt_client.run_sort(genlog_simple, sort_by=["feed_id", "offer_id"])  # в реальности все уже отсортировано к этом моменту

    if use_msku_stats_for_blue_offer:
        yt_client.run_sort(genlog_msku, sort_by="market_sku")  # в реальности все уже отсортировано к этом моменту

    cmdlist = [
        yatest.common.binary_path('market/idx/promos/pricedrops/bin/yt_pricedrops_cpp'),
        "--specific-step", "offers_data_collector",
        "--server", YT_SERVER.get_server(),
        "--working-dir", bin_work_dir,
        "--mstat-currency-table-path", currency_table,
        "--genlog-model-sorted", genlog_model,
        "--genlog-cluster-sorted", genlog_cluster,
        "--genlog-simple-sorted", genlog_simple,
        "--price-history-table-path", price_history,
        "--category-conversion-table-path", category_conversion,
        "--clicks-stats-output-model-offer", model_offer_clicks_stats,
        "--clicks-stats-output-cluster-offer", cluster_offer_clicks_stats,
        "--clicks-stats-output-simple-offer", simple_offer_clicks_stats,
        "--clicks-stats-output-model", model_clicks_stats,
        "--clicks-stats-output-cluster", cluster_clicks_stats,
        "--tentop-percent-output-model", model_tentop,
        "--tentop-percent-output-cluster", cluster_tentop,
        "--use-msku-stats-for-blue-offer", str(use_msku_stats_for_blue_offer)
    ]
    if use_msku_stats_for_blue_offer:
        cmdlist += [
            "--genlog-msku-sorted", genlog_msku,
            "--clicks-stats-output-msku-offer", msku_offer_clicks_stats,
            "--clicks-stats-output-msku", msku_clicks_stats,
            "--tentop-percent-output-msku", msku_tentop,
        ]
    run_bin(cmdlist)

    click_stats_calcer_tables = yt_client.list("{}/{}".format(bin_work_dir, STEP_NAME))
    if use_msku_stats_for_blue_offer:
        assert len(click_stats_calcer_tables) == 6  # 4 выхлопа + tmp-директория + пофильтрованая история
    else:
        assert len(click_stats_calcer_tables) == 5  # 3 выхлопа + tmp-директория + пофильтрованая история

    model_path = ""
    cluster_path = ""
    simple_path = ""
    msku_path = ""
    for path in click_stats_calcer_tables:
        if path.startswith("model_offers_data_"):
            model_path = path
        if path.startswith("cluster_offers_data_"):
            cluster_path = path
        if path.startswith("msku_offers_data_"):
            msku_path = path
        if path.startswith("simple_offers_data_"):
            simple_path = path
    assert model_path and cluster_path and simple_path
    if use_msku_stats_for_blue_offer:
        assert msku_path

    check_table(output_data['output_model_offer_data'], yt_client.read_table("{}/{}/{}".format(bin_work_dir, STEP_NAME, model_path)))
    check_table(output_data['output_cluster_offer_data'], yt_client.read_table("{}/{}/{}".format(bin_work_dir, STEP_NAME, cluster_path)))
    check_table(output_data['output_simple_offer_data'], yt_client.read_table("{}/{}/{}".format(bin_work_dir, STEP_NAME, simple_path)))

    if use_msku_stats_for_blue_offer:
        check_table(output_data['output_msku_offer_data'], yt_client.read_table("{}/{}/{}".format(bin_work_dir, STEP_NAME, msku_path)))
