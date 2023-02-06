#!/usr/bin/env python
# coding: utf-8
"""
Проверяем проставление вендорских ставок на оффер и звездочки рекомендованных магазинов
На входе vendor_bids.csv из выгрузки mbi в формате "vendor_id\tvendor_ds_id\tcategory_id\tshop_id\tvbid\tis_recommended"
На выходе vendor_values_binary: vendor_bid, vendor_ds_id и is_recommended_by_vendor для каждого оффера
Для каждого оффера ищем в vendor_bids.csv по vendor_id, shop_id и по category_id
Если нет данных непосредственно для категории оффера - ищем для его родительских категорий
"""

import pytest
from hamcrest import assert_that

from market.idx.offers.yatf.utils.fixtures import default_genlog
from market.idx.offers.yatf.resources.offers_indexer.vendor_bids import VendorBids
from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.offers.yatf.matchers.offers_processor.env_matchers import HasGenlogRecordRecursive
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable


test_data = [
    {
        # found in vendor_bids.csv
        'offer_id': '1',
        'incoming_vendor_id': 1,
        'incoming_shop_id': 100,
        'incoming_category_id': 91491,
        'vendor_bid': 123,
        'vendor_ds_id': 10,
        'is_recommended_by_vendor': True,
    },
    {
        # found in vendor_bids.csv
        'offer_id': '2',
        'incoming_vendor_id': 2,
        'incoming_shop_id': 200,
        'incoming_category_id': 91491,
        'vendor_bid': 456,
        'vendor_ds_id': 20,
        'is_recommended_by_vendor': False,
    },
    {
        # not found in vendor_bids.csv
        'offer_id': '3',
        'incoming_vendor_id': 3,
        'incoming_shop_id': 300,
        'incoming_category_id': 91491,
        'vendor_bid': 0,
        'vendor_ds_id': 0,
        'is_recommended_by_vendor': False,
    },
    {
        # found in vendor_bids.csv because of category parent propagation
        'offer_id': '4',
        'incoming_vendor_id': 2,
        'incoming_shop_id': 300,
        'incoming_category_id': 91491,
        'vendor_bid': 789,
        'vendor_ds_id': 21,
        'is_recommended_by_vendor': True,
    },
]


@pytest.fixture(scope="module")
def vendor_bids():
    vendor_bids = VendorBids()
    # vendor_id, vendor_ds_id, category_id, shop_id, vbid, is_recommended
    vendor_bids.add(1, 10, 91491, 100, 123, True)
    vendor_bids.add(2, 20, 91491, 200, 456, False)
    vendor_bids.add(2, 21, 91461, 300, 789, True)
    return vendor_bids


@pytest.fixture(scope="module")
def genlog_rows():
    offers = []
    for data in test_data:
        offer = default_genlog(
            offer_id=data['offer_id'],
            vendor_id=data['incoming_vendor_id'],
            category_id=data['incoming_category_id'],
            shop_id=data['incoming_shop_id'],
        )
        offers.append(offer)
    return offers


@pytest.fixture(scope="module")
def genlog_table(yt_server, genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0000'), genlog_rows)
    genlog_table.dump()
    return genlog_table


@pytest.yield_fixture(scope="module")
def workflow(yt_server, genlog_table, vendor_bids):
    input_table_paths = [genlog_table.get_path()]

    resources = {
        'vendor_bids_csv': vendor_bids,
    }

    with OffersProcessorTestEnv(
            yt_server,
            use_genlog_scheme=True,
            input_table_paths=input_table_paths,
            **resources
    ) as env:
        env.execute()
        env.verify()
        yield env


def test_vendor_bids(workflow):
    for data in test_data:
        assert_that(
            workflow,
            HasGenlogRecordRecursive({
                'offer_id': data['offer_id'],
                'vendor_bids': {
                    'is_recommended': data['is_recommended_by_vendor'],
                    'vendor_data_source': data['vendor_ds_id'],
                    'vendor_bid': data['vendor_bid'],
                },
            })
        )
