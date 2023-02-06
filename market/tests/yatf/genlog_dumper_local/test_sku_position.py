#!/usr/bin/env python
# coding: utf-8
import pytest
from hamcrest import assert_that

from market.idx.generation.yatf.resources.genlog_dumper.input_records_proto import InputRecordsProto, make_gl_record
from market.idx.generation.yatf.resources.genlog_dumper.input_run_options import RunOptions
from market.idx.generation.yatf.matchers.genlog_dumper.env_matchers import HasSkuPositionRecord
from market.idx.generation.yatf.test_envs.genlog_dumper import (
    GenlogDumperTestEnv,
    RUN_RESOURCE_NAME,
    OFFERS_RESOURCE_NAME
)
from market.idx.pylibrary.offer_flags.flags import OfferFlags


def get_binary_ware_md5(id):
    return (id + '==').decode('base64')


@pytest.fixture(scope="module")
def offers():
    offer1 = make_gl_record(
        ware_md5='1irstOffer0V7gLLUBANyg',
        binary_ware_md5=get_binary_ware_md5('1irstOffer0V7gLLUBANyg'),
        feed_id=101967,
        market_sku=666,
        flags=OfferFlags.BLUE_OFFER,
    )

    offer2 = make_gl_record(
        ware_md5='2irstOffer0V7gLLUBANyg',
        binary_ware_md5=get_binary_ware_md5('2irstOffer0V7gLLUBANyg'),
        feed_id=101967,
        market_sku=777,
        flags=OfferFlags.BLUE_OFFER,
    )

    msku = make_gl_record(
        ware_md5='3irstOffer0V7gLLUBANyg',
        binary_ware_md5=get_binary_ware_md5('3irstOffer0V7gLLUBANyg'),
        feed_id=101967,
        market_sku=888,
        flags=(OfferFlags.BLUE_OFFER | OfferFlags.MARKET_SKU),
    )

    not_blue = make_gl_record(
        ware_md5='4irstOffer0V7gLLUBANyg',
        binary_ware_md5=get_binary_ware_md5('4irstOffer0V7gLLUBANyg'),
        feed_id=101967,
        market_sku=123,
    )

    return [offer1, offer2, msku, not_blue]


@pytest.yield_fixture(scope="module")
def genlog_dumper(offers):
    gd_resources = {
        RUN_RESOURCE_NAME: RunOptions([
            '--dumper', 'SKU_POSITION_MAPPING',
            '--dumper', 'WARE_MD5',
        ]),
        OFFERS_RESOURCE_NAME: InputRecordsProto(offers)
    }
    with GenlogDumperTestEnv(**gd_resources) as env:
        env.execute()
        env.verify()
        yield env


def test_sku_position(genlog_dumper, offers):
    """
    check SkuPositionMappingDumper
    There is more details test in mmap r/w library:
    https://a.yandex-team.ru/arc/library/libsku/ut
    """

    expected = []
    ordered_offers = genlog_dumper.ordered_offers(offers)

    for offset, offer in enumerate(ordered_offers):
        if offer.market_sku == 666 or offer.market_sku == 777:
            el = [
                ('FeedId', str(offer.feed_id)),
                ('Market SKU', str(offer.market_sku)),
                ('Offers offsets', str(offset)),
            ]
            expected.append(el)

    assert_that(genlog_dumper,
                HasSkuPositionRecord('1', expected),
                u'msku_positions.mmap contains expected document')
