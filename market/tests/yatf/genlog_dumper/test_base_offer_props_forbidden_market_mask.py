#!/usr/bin/env python
# coding: utf-8
import pytest
from hamcrest import assert_that

from market.idx.generation.yatf.matchers.genlog_dumper.env_matchers import HasBaseOfferPropsFbRecursive
from market.idx.generation.yatf.resources.genlog_dumper.input_records_proto import InputRecordsProto, make_gl_record
from market.idx.generation.yatf.resources.genlog_dumper.input_run_options import RunOptions
from market.idx.generation.yatf.test_envs.genlog_dumper import (
    GenlogDumperTestEnv,
    RUN_RESOURCE_NAME,
    OFFERS_RESOURCE_NAME
)

from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.offers.yatf.utils.fixtures import (
    default_genlog,
    default_blue_genlog,
    get_binary_ware_md5
)

from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable

from market.idx.offers.yatf.resources.offers_indexer.gl_mbo_pb import GlMboPb

from market.proto.content.mbo.MboParameters_pb2 import Category
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join


@pytest.fixture(scope="module")
def gl_mbo():
    return [
        Category(
            hid=90401,
            parameter=[]
        )
    ]


@pytest.fixture(scope="module")
def test_table():
    IS_PSKU = 1 << 4

    def get_tablerow(inp, exp):
        return {
            "offer": inp,
            "expect": exp
        }

    table = [
        get_tablerow(
            default_genlog(
                category_id=90401,
                ware_md5='1irstOffer0V7gLLUBANyg',
                binary_ware_md5=get_binary_ware_md5('1irstOffer0V7gLLUBANyg'),
                market_sku=999,
                is_fake_msku_offer=True,
                is_psku=True,
            ),
            {
                'forbidden_market_mask': IS_PSKU,
            }
        ),
        get_tablerow(
            default_genlog(
                category_id=90401,
                ware_md5='2irstOffer0V7gLLUBANyg',
                binary_ware_md5=get_binary_ware_md5('2irstOffer0V7gLLUBANyg'),
                market_sku=999,
                is_psku=True,
            ),
            {
                'forbidden_market_mask': IS_PSKU,
            }
        ),
        get_tablerow(
            default_blue_genlog(
                category_id=90401,
                ware_md5='3irstOffer0V7gLLUBANyg',
                binary_ware_md5=get_binary_ware_md5('3irstOffer0V7gLLUBANyg'),
                market_sku=998,
            ),
            {
                'forbidden_market_mask': 0,
            }
        ),
        get_tablerow(
            default_genlog(
                category_id=90401,
                ware_md5='4irstOffer0V7gLLUBANyg',
                binary_ware_md5=get_binary_ware_md5('4irstOffer0V7gLLUBANyg'),
                market_sku=998,
                is_fake_msku_offer=True,
            ),
            {
                'forbidden_market_mask': 0,
            }
        )
    ]
    return table


@pytest.fixture(scope='module')
def genlog_rows(test_table):
    return [row["offer"] for row in test_table]


@pytest.fixture(scope="module")
def genlog_table(yt_server, genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0000'), genlog_rows)
    genlog_table.dump()
    return genlog_table


@pytest.yield_fixture(scope="module")
def offers_processor_workflow(gl_mbo, yt_server, genlog_table):
    input_table_paths = [genlog_table.get_path()]

    resources = {
        'gl_mbo_pbuf_sn': GlMboPb(gl_mbo),
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


@pytest.yield_fixture(scope="module")
def genlog_dumper(offers_processor_workflow):
    records = []
    for offer in offers_processor_workflow.genlog_dicts:
        records.append(make_gl_record(
            binary_ware_md5=offer.get('binary_ware_md5'),
            forbidden_market_mask=offer.get('forbidden_market_mask'),
        ))

    gd_resources = {
        RUN_RESOURCE_NAME: RunOptions([
            '--dumper', 'BASE_OFFER_PROPS',
            '--dumper', 'WARE_MD5'
        ]),
        OFFERS_RESOURCE_NAME: InputRecordsProto(records)
    }
    with GenlogDumperTestEnv(**gd_resources) as env:
        env.execute()
        env.verify()
        yield env


def test_forbidden_market_mask(genlog_dumper, test_table):
    """
    Проверяем, что битовая маска forbidden_market_mask правильно проставляется в offers_indexer
    согласно соответствующему флагу в описании оффера
    """
    expected = {}
    offset_by_md5 = {}

    for i in range(len(test_table)):
        offset_by_md5[genlog_dumper.ware_md5.get_ware_md5(i)] = i

    for idx, row in enumerate(test_table):
        ware_md5 = row['offer']['ware_md5']
        offset = offset_by_md5[ware_md5]

        expected_offer_mask = row['expect']
        forbidden_market_mask = expected_offer_mask['forbidden_market_mask'] if 'forbidden_market_mask' in expected_offer_mask else 0

        expected[offset] = {'ForbiddenMarketMask': forbidden_market_mask}

    assert_that(
        genlog_dumper,
        HasBaseOfferPropsFbRecursive(expected),
        'base-offer-props.fb contains expected forbidden market masks'
    )
