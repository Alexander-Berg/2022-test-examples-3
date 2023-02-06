#!/usr/bin/env python
# coding: utf-8
import pytest
from hamcrest import assert_that

from market.idx.generation.yatf.resources.genlog_dumper.input_records_proto import InputRecordsProto, make_gl_record
from market.idx.generation.yatf.resources.genlog_dumper.input_run_options import RunOptions
from market.idx.generation.yatf.test_envs.genlog_dumper import (
    GenlogDumperTestEnv,
    RUN_RESOURCE_NAME,
    OFFERS_RESOURCE_NAME
)
from market.idx.offers.yatf.matchers.offers_indexer.env_matchers import HasSkuRecord
from market.idx.pylibrary.offer_flags.flags import OfferFlags
from market.proto.feedparser.OffersData_pb2 import ContexInfo


def get_binary_ware_md5(id):
    return (id + '==').decode('base64')


@pytest.fixture(scope="module")
def blue_offers():
    # original msku
    msku_orig = make_gl_record(
        feed_id=101967,
        ware_md5='1irstOffer0V7gLLUBANyg',
        binary_ware_md5=get_binary_ware_md5('1irstOffer0V7gLLUBANyg'),
        market_sku=1,
        model_id=1,
        shop_sku='somemshopmsku',
        is_blue_offer=True,
        flags=OfferFlags.BLUE_OFFER,
        is_fake_msku_offer=True,
        contex_info=ContexInfo(
            experiment_id='exp',
            experimental_msku_id=2,
            original_model_id=1,
            experimental_model_id=2
        )
    )

    # experimental msku
    msku_exp = make_gl_record(
        feed_id=101967,
        ware_md5='2irstOffer0V7gLLUBANyg',
        binary_ware_md5=get_binary_ware_md5('2irstOffer0V7gLLUBANyg'),
        market_sku=2,
        model_id=1,
        shop_sku='somedifferentshopsku',
        is_blue_offer=True,
        is_fake_msku_offer=True,
        flags=(OfferFlags.BLUE_OFFER | OfferFlags.MARKET_SKU),
        contex_info=ContexInfo(
            experiment_id='exp',
            original_model_id=1,
            experimental_model_id=2,
            original_msku_id=1
        )
    )

    return [msku_orig, msku_exp]


@pytest.yield_fixture(scope="module")
def genlog_dumper(blue_offers):
    gd_resources = {
        RUN_RESOURCE_NAME: RunOptions([
            '--dumper', 'REGIONAL_DELIVERY',
            '--dumper', 'WARE_MD5',
        ]),
        OFFERS_RESOURCE_NAME: InputRecordsProto(blue_offers)
    }
    with GenlogDumperTestEnv(**gd_resources) as env:
        env.execute()
        env.verify()
        yield env


def test_offer_sku_mmap(genlog_dumper, blue_offers):
    """
    Проверяем, что поле SKU имеет значение 1 -- id оригинального MSKU.
    """

    expected = []
    ordered_offers = genlog_dumper.ordered_offers(blue_offers)

    for offset, offer in enumerate(ordered_offers):
        expected.append([
            ('OfferOffset', str(offset)),
            ('SKU', '1'),
            ('ShopSKU', offer.shop_sku),
            ('RefShopId', '0'),
            ('CalendarId', '4294967295'),
            ('Buckets', ''),
            ("DeliveryServiceFlags (2 ^ (bit's index))", ''),
            ("OfferFlags ([bit's index]bit's value)", '[0]0 [3]0 [7]0 [15]0 [24]0 [26]0'),
            ('Cpa', '0'),
            ('DeliveryOffset (from-to)', '0-0')
        ])

    assert_that(genlog_dumper,
                HasSkuRecord('4', expected),
                u'offer_sku.mmap contains expected document')
