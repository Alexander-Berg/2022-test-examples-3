#!/usr/bin/env python
# -*- coding: utf-8 -*-

import pytest
from datetime import date, timedelta
from market.idx.yatf.resources.yt_table_resource import YtTableResource
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from market.idx.yatf.resources.yql_resource import YtResource, YqlRequestResource
from market.idx.yatf.test_envs.yql_env import YqlTestEnv
from yt.wrapper import ypath_join
import market.idx.marketindexer.marketindexer.shop_vendor_promo_offer_clicks as shop_vendor_promo_offer_clicks


class ClicksTable(YtTableResource):
    def __init__(self, yt_stuff, path, data):
        super(ClicksTable, self).__init__(
            yt_stuff=yt_stuff,
            path=path,
            data=data,
            attributes=dict(
                dynamic=False,
                external=False,
                schema=[
                    dict(name="shop_id", type="uint64"),
                    dict(name="ware_md5", type="string"),
                    dict(name="promo_type", type="int64"),
                    dict(name="filter", type="int64"),
                    dict(name="click_type_id", type="uint64"),
                    dict(name="state", type="int64"),
                ]
            )
        )


class GenlogTable(YtTableResource):
    def __init__(self, yt_stuff, path, data):
        super(GenlogTable, self).__init__(
            yt_stuff=yt_stuff,
            path=path,
            data=data,
            attributes=dict(
                dynamic=False,
                external=False,
                schema=[
                    dict(name="ware_md5", type="string"),
                    dict(name="shop_id", type="uint64"),
                    dict(name="vendor_id", type="uint64"),
                    dict(name="category_id", type="uint64"),
                    dict(name="promo_type", type="uint64"),
                    dict(name="model_id", type="uint64"),
                ]
            )
        )


@pytest.fixture(scope='module', params=["clicks"])
def clicks_tables_dir(request):
    yield request.param


@pytest.fixture(
    scope='module',
    params=[
        {
            'clicks_data': [
                # клики по клёвым офферам из 1 и 2 категории, они будут в статистиках - так что эти категори будут раньше остальных
                {"shop_id": 1, "ware_md5": "kOijrFDJE5Z8xj1nHxMi80", "promo_type": 128, "filter": 0, "click_type_id": 0, "state": 1},
                {"shop_id": 1, "ware_md5": "k0ijrFDJE5Z8xj1nHxMi81", "promo_type": 128, "filter": 0, "click_type_id": 0, "state": 1},
                # клики по офферам из категории 3. Они не будут учитываться в статистиках.
                {"shop_id": 2, "ware_md5": "kOijrFDJE5Z8xj1nHxMi82", "promo_type": 0, "filter": 0, "click_type_id": 0, "state": 1},
                {"shop_id": 2, "ware_md5": "k1ijrFDJE5Z8xj1nHxMi82", "promo_type": 8192, "filter": 0, "click_type_id": 0, "state": 1},
                {"shop_id": 2, "ware_md5": "k2ijrFDJE5Z8xj1nHxMi82", "promo_type": 8192, "filter": 0, "click_type_id": 0, "state": 1},
            ],
            'genlog_data': [
                # категория 1 попадет в магазинные промокоды, но не попадет в вендорные - потому что у нее нет 5 разных моделей
                {'shop_id': 1, 'ware_md5': 'kOijrFDJE5Z8xj1nHxMi80', 'vendor_id': 2, 'category_id': 1, 'promo_type': 128, 'model_id': 1},
                {'shop_id': 1, 'ware_md5': 'k1ijrFDJE5Z8xj1nHxMi80', 'vendor_id': 2, 'category_id': 1, 'promo_type': 128, 'model_id': 1},
                {'shop_id': 1, 'ware_md5': 'k2ijrFDJE5Z8xj1nHxMi80', 'vendor_id': 2, 'category_id': 1, 'promo_type': 128, 'model_id': 1},
                {'shop_id': 1, 'ware_md5': 'k3ijrFDJE5Z8xj1nHxMi80', 'vendor_id': 2, 'category_id': 1, 'promo_type': 128, 'model_id': 1},
                {'shop_id': 1, 'ware_md5': 'k4ijrFDJE5Z8xj1nHxMi80', 'vendor_id': 2, 'category_id': 1, 'promo_type': 128, 'model_id': 1},

                # категория 2 попадет и в магазинные, и в вендорные
                {'shop_id': 1, 'ware_md5': 'k0ijrFDJE5Z8xj1nHxMi81', 'vendor_id': 2, 'category_id': 2, 'promo_type': 128, 'model_id': 1},
                {'shop_id': 1, 'ware_md5': 'k1ijrFDJE5Z8xj1nHxMi81', 'vendor_id': 2, 'category_id': 2, 'promo_type': 128, 'model_id': 2},
                {'shop_id': 1, 'ware_md5': 'k2ijrFDJE5Z8xj1nHxMi81', 'vendor_id': 2, 'category_id': 2, 'promo_type': 128, 'model_id': 3},
                {'shop_id': 1, 'ware_md5': 'k3ijrFDJE5Z8xj1nHxMi81', 'vendor_id': 2, 'category_id': 2, 'promo_type': 128, 'model_id': 4},
                {'shop_id': 1, 'ware_md5': 'k4ijrFDJE5Z8xj1nHxMi81', 'vendor_id': 2, 'category_id': 2, 'promo_type': 128, 'model_id': 5},

                # попадет и в магазинные, и в вендорные - но только после категории 2, которая имеет клики
                # (у этой тоже есть, и даже больше: 2, но по бандлам. Мы их выкидываем их статистик.)
                # не попадает в вендорные промокоды, потому что их всего 4
                {'shop_id': 1, 'ware_md5': 'kOijrFDJE5Z8xj1nHxMi82', 'vendor_id': 2, 'category_id': 3, 'promo_type': 128, 'model_id': 1},
                {'shop_id': 1, 'ware_md5': 'k1ijrFDJE5Z8xj1nHxMi82', 'vendor_id': 2, 'category_id': 3, 'promo_type': 128, 'model_id': 2},
                {'shop_id': 1, 'ware_md5': 'k2ijrFDJE5Z8xj1nHxMi82', 'vendor_id': 2, 'category_id': 3, 'promo_type': 128, 'model_id': 3},
                {'shop_id': 1, 'ware_md5': 'k3ijrFDJE5Z8xj1nHxMi82', 'vendor_id': 2, 'category_id': 3, 'promo_type': 128, 'model_id': 4},
                {'shop_id': 1, 'ware_md5': 'k4ijrFDJE5Z8xj1nHxMi92', 'vendor_id': 2, 'category_id': 3, 'promo_type': 256, 'model_id': 5},

                # не попадет никуда, потому что у офферов нет промо
                {'shop_id': 1, 'ware_md5': 'kOijrFDJE5Z8xj1nHxMi83', 'vendor_id': 2, 'category_id': 4, 'promo_type': None, 'model_id': 1},
                {'shop_id': 1, 'ware_md5': 'k1ijrFDJE5Z8xj1nHxMi83', 'vendor_id': 2, 'category_id': 4, 'promo_type': None, 'model_id': 2},
                {'shop_id': 1, 'ware_md5': 'k2ijrFDJE5Z8xj1nHxMi83', 'vendor_id': 2, 'category_id': 4, 'promo_type': None, 'model_id': 3},
                {'shop_id': 1, 'ware_md5': 'k3ijrFDJE5Z8xj1nHxMi83', 'vendor_id': 2, 'category_id': 4, 'promo_type': None, 'model_id': 4},
                {'shop_id': 1, 'ware_md5': 'k4ijrFDJE5Z8xj1nHxMi93', 'vendor_id': 2, 'category_id': 4, 'promo_type': None, 'model_id': 5},

                # не попадет никуда, потому что у офферов только бандлы - они нам не интересны
                {'shop_id': 1, 'ware_md5': 'kOijrFDJE5Z8xj1nHxMi84', 'vendor_id': 2, 'category_id': 5, 'promo_type': 8192, 'model_id': 1},
                {'shop_id': 1, 'ware_md5': 'k1ijrFDJE5Z8xj1nHxMi84', 'vendor_id': 2, 'category_id': 5, 'promo_type': 8192, 'model_id': 2},
                {'shop_id': 1, 'ware_md5': 'k2ijrFDJE5Z8xj1nHxMi84', 'vendor_id': 2, 'category_id': 5, 'promo_type': 8192, 'model_id': 3},
                {'shop_id': 1, 'ware_md5': 'k3ijrFDJE5Z8xj1nHxMi84', 'vendor_id': 2, 'category_id': 5, 'promo_type': 8192, 'model_id': 4},
                {'shop_id': 1, 'ware_md5': 'k4ijrFDJE5Z8xj1nHxMi94', 'vendor_id': 2, 'category_id': 5, 'promo_type': 8192, 'model_id': 5},
            ],
            'expected': {
                'shop_allpromo': [
                    {'shop_id': 1, 'allpromo_top_clicks_categories': [1, 2, 3]},
                ],
                'shop_promocode': [
                    {'shop_id': 1, 'promocode_top_clicks_categories': [1, 2]}
                ],
                'vendor_allpromo': [
                    {'vendor_id': 2, 'allpromo_top_clicks_categories': [2, 3]},
                ],
                'vendor_promocode': [
                    {'vendor_id': 2, 'promocode_top_clicks_categories': [2]}
                ],
            },
        },
    ],
    ids=['test_shop_vendor_promo_offer_clicks']
)
def tables_data(request):
    return request.param


@pytest.fixture(scope='module')
def yt_prefix():
    return get_yt_prefix()


@pytest.fixture(scope='module')
def clicks_table_path(yt_server, yt_prefix, clicks_tables_dir, tables_data):
    tablepath = ypath_join(yt_prefix,
                           'input',
                           clicks_tables_dir,
                           (date.today() - timedelta(1)).strftime(shop_vendor_promo_offer_clicks.DAY_DT_FORMAT))
    ct = ClicksTable(yt_server, tablepath, tables_data['clicks_data'])
    ct.dump()
    return ct.get_path()


@pytest.fixture(scope='module')
def genlog_table_path(yt_server, yt_prefix, tables_data):
    tablepath = ypath_join(yt_prefix, 'input', 'genlog')
    gt = GenlogTable(yt_server, tablepath, tables_data['genlog_data'])
    gt.dump()
    return gt.get_path()


@pytest.fixture(scope='module')
def output_prefix(yt_prefix):
    return ypath_join(yt_prefix, 'output')


@pytest.fixture(scope='module')
def output_table_name():
    return date.today().strftime(shop_vendor_promo_offer_clicks.DAY_DT_FORMAT)


@pytest.yield_fixture(scope="module")
def simple_run_workflow(yt_server, clicks_table_path, genlog_table_path, output_prefix, output_table_name):
    REMOVE_LINE_TRIGGER = "removethisline"

    template_values = shop_vendor_promo_offer_clicks.ShopVendorPromoOfferClicksTemplateFields()
    template_values.tpl_yt_server = REMOVE_LINE_TRIGGER
    template_values.tpl_yt_pool = REMOVE_LINE_TRIGGER
    template_values.tpl_clicks_table_dir = template_values.tpl_clicks_table_dir = clicks_table_path[0: clicks_table_path.rfind("/")]
    template_values.tpl_offers_table_path = genlog_table_path
    template_values.tpl_shop_allpromo_top_clicks_categories_dir = ypath_join(output_prefix, "shop_allpromo")
    template_values.tpl_shop_promocode_top_clicks_categories_dir = ypath_join(output_prefix, "shop_promocode")
    template_values.tpl_vendor_allpromo_top_clicks_categories_dir = ypath_join(output_prefix, "vendor_allpromo")
    template_values.tpl_vendor_promocode_top_clicks_categories_dir = ypath_join(output_prefix, "vendor_promocode")

    template_values.tpl_output_table_name = output_table_name

    real_query = shop_vendor_promo_offer_clicks._format_query(
        shop_vendor_promo_offer_clicks.yql_query_template,
        template_values
    )
    patched_query = "\n".join([line for line in real_query.split("\n") if REMOVE_LINE_TRIGGER not in line])

    resources = {
        'yt': YtResource(yt_stuff=yt_server),
        'request': YqlRequestResource(patched_query)
    }
    with YqlTestEnv(syntax_version=1, **resources) as test_env:
        test_env.execute()
        yield test_env


@pytest.yield_fixture(scope='module')
def shop_allpromo_table(yt_server, output_prefix, output_table_name):
    yield YtTableResource(yt_server, ypath_join(output_prefix, "shop_allpromo", output_table_name), load=True)


@pytest.yield_fixture(scope='module')
def shop_promocode_table(yt_server, output_prefix, output_table_name):
    yield YtTableResource(yt_server, ypath_join(output_prefix, "shop_promocode", output_table_name), load=True)


@pytest.yield_fixture(scope='module')
def vendor_allpromo_table(yt_server, output_prefix, output_table_name):
    yield YtTableResource(yt_server, ypath_join(output_prefix, "vendor_allpromo", output_table_name), load=True)


@pytest.yield_fixture(scope='module')
def vendor_promocode_table(yt_server, output_prefix, output_table_name):
    yield YtTableResource(yt_server, ypath_join(output_prefix, "vendor_promocode", output_table_name), load=True)


def test_shop_vendor_promo_offer_clicks_query_correct(simple_run_workflow):
    results = simple_run_workflow.yql_results
    assert results.is_success, [str(error) for error in results.errors]


def test_shop_allpromo(tables_data, shop_allpromo_table):
    assert shop_allpromo_table.data == tables_data['expected']['shop_allpromo']


def test_shop_promocode(tables_data, shop_promocode_table):
    assert shop_promocode_table.data == tables_data['expected']['shop_promocode']


def test_vendor_allpromo(tables_data, vendor_allpromo_table):
    assert vendor_allpromo_table.data == tables_data['expected']['vendor_allpromo']


def test_vendor_promocode(tables_data, vendor_promocode_table):
    assert vendor_promocode_table.data == tables_data['expected']['vendor_promocode']
