#!/usr/bin/env python
# coding: utf-8
"""
Проверяем проставление вендорских рекомендаций на оффер
"""

import pytest
from hamcrest import assert_that

from market.idx.offers.yatf.utils.fixtures import default_genlog, default_shops_dat
from market.idx.offers.yatf.resources.offers_indexer.vendor_recommended_business import VendorRecommendedBusiness
from market.idx.yatf.resources.shops_dat import ShopsDat
from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.offers.yatf.matchers.offers_processor.env_matchers import HasGenlogRecordRecursive
from market.idx.pylibrary.offer_flags.flags import OfferFlags
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable


test_data = [
    {
        # found in vendor_recommended_business.csv
        'offer_id': '1',
        'shop_id': 1,
        'business_id': 1,
        'datafeed_id': 1,
        'vendor_id': 1,
        'vendor_ds_id': 10,
        'is_recommended_by_vendor': True,
        'is_official_by_vendor': True,
    },
    {
        # found in vendor_recommended_business.csv
        'offer_id': '2',
        'shop_id': 2,
        'business_id': 2,
        'datafeed_id': 2,
        'vendor_id': 2,
        'vendor_ds_id': 20,
        'is_recommended_by_vendor': False,
        'is_official_by_vendor': False,
    },
    {
        # not found in vendor_recommended_business.csv
        'offer_id': '3',
        'shop_id': 3,
        'business_id': 3,
        'datafeed_id': 3,
        'vendor_id': 3,
        'vendor_ds_id': 0,
        'is_recommended_by_vendor': False,
        'is_official_by_vendor': False,
    },
    {
        # found in vendor_recommended_business.csv
        'offer_id': '4',
        'shop_id': 4,
        'business_id': 4,
        'datafeed_id': 4,
        'vendor_id': 4,
        'vendor_ds_id': 21,
        'is_recommended_by_vendor': True,
        'is_official_by_vendor': False,
    },
]


@pytest.fixture(scope="module")
def vendor_recommended_business():
    vendor_recommended_business = VendorRecommendedBusiness()
    # vendor_id, vendor_ds_id, business_id, is_recommended, is_official
    vendor_recommended_business.add(1, 10, 1, True, True)
    vendor_recommended_business.add(2, 20, 2, False, False)
    vendor_recommended_business.add(4, 21, 4, True, False)
    return vendor_recommended_business


@pytest.fixture(scope="module")
def genlog_rows():
    offers = []
    for data in test_data:
        offer = default_genlog(
            feed_id=data['datafeed_id'],
            shop_id=data['shop_id'],
            offer_id=data['offer_id'],
            vendor_id=data['vendor_id'],
            business_id=data['business_id'],
            flags=OfferFlags.DEPOT | OfferFlags.STORE
        )
        offers.append(offer)
    return offers


@pytest.fixture(scope="module")
def genlog_table(yt_server, genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0000'), genlog_rows)
    genlog_table.dump()
    return genlog_table


@pytest.fixture(scope="module")
def shopsdat():
    shops = []
    for data in test_data:
        shop = default_shops_dat(
            business_id=str(data['business_id']),
            shop_id=str(data['shop_id']),
            datafeed_id=str(data['datafeed_id']),
        )
        shops.append(shop)
    return ShopsDat(shops=shops)


@pytest.yield_fixture(scope="module")
def workflow(yt_server, genlog_table, shopsdat, vendor_recommended_business):
    input_table_paths = [genlog_table.get_path()]

    resources = {
        'vendor_recommended_business_csv': vendor_recommended_business,
        'shops_dat': shopsdat
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


def test_vendor_recommended_business(workflow):
    for data in test_data:
        flags = OfferFlags.DEPOT | OfferFlags.STORE | OfferFlags.MODEL_COLOR_WHITE | OfferFlags.CPC
        if data['is_recommended_by_vendor']:
            flags |= OfferFlags.IS_RECOMMENDED_BY_VENDOR
        if data['is_official_by_vendor']:
            flags |= OfferFlags.IS_OFFICIAL_BY_VENDOR
        assert_that(
            workflow,
            HasGenlogRecordRecursive({
                'offer_id': data['offer_id'],
                'vendor_recommended_business_datasource': data['vendor_ds_id'],
                'flags': flags,
            })
        )
