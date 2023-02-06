#!/usr/bin/env python
# coding: utf-8
import pytest
from hamcrest import (
    assert_that,
    equal_to,
    all_of,
)

from market.idx.offers.yatf.matchers.offers_processor.env_matchers import (
    HasGenlogRecord,
)
from market.idx.offers.yatf.utils.fixtures import (
    default_genlog,
)
from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable


@pytest.fixture(scope="module")
def blue_genlog_rows():
    offers = [
        default_genlog(
            offer_id='1',
            ware_md5='000000000000000000001w',
            is_blue_offer=True,
            market_sku=100500,
            url='https://pokupki.market.yandex.ru/product/100500?offerid=000000000000000000001w',
        ),
    ]
    return offers


@pytest.fixture(scope="module")
def genlog_table(yt_server, blue_genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0000'), blue_genlog_rows)
    genlog_table.dump()
    return genlog_table


@pytest.yield_fixture(scope="module")
def blue_workflow(yt_server, genlog_table):
    input_table_paths = [genlog_table.get_path()]

    with OffersProcessorTestEnv(
            yt_server,
            use_genlog_scheme=True,
            input_table_paths=input_table_paths,
    ) as env:
        env.execute(use_pokupki_domain=True)
        yield env


def test_generation_log_nrecords(blue_workflow, blue_genlog_rows):
    assert_that(len(blue_workflow.genlog), equal_to(len(blue_genlog_rows)), 'GenLog contain wrong number of records')


def test_blue_genlog_url(blue_workflow):
    assert_that(blue_workflow, all_of(
        HasGenlogRecord({
            'offer_id': '1',
            'market_sku': 100500,
            'ware_md5': '000000000000000000001w',
            'url': 'https://pokupki.market.yandex.ru/product/100500?offerid=000000000000000000001w',
        })
    ))
