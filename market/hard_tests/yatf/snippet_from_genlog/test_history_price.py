# coding=utf-8

import pytest

from market.idx.generation.yatf.test_envs.snippet_diff_builder import SnippetDiffBuilderTestEnv

from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv

from market.idx.offers.yatf.utils.fixtures import (
    default_genlog,
    default_blue_genlog,
    generate_binary_price_dict,
)
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join


@pytest.fixture(scope='module')
def genlog_rows():
    return [
        # valid discount, white offer, history price passed validation,
        # history price should be in indexarc
        default_genlog(
            offer_id='1',
            binary_price=generate_binary_price_dict(600),
            binary_oldprice=generate_binary_price_dict(700),
            binary_unverified_oldprice=generate_binary_price_dict(700),
            binary_history_price=generate_binary_price_dict(800),
            snippet_history_price='800',
            history_price_is_valid=True,
        ),
        # valid discount, white offer, history price didn't pass validation,
        # history price shouldn't be in indexarc
        default_genlog(
            offer_id='2',
            binary_price=generate_binary_price_dict(600),
            binary_oldprice=generate_binary_price_dict(700),
            binary_unverified_oldprice=generate_binary_price_dict(700),
            binary_history_price=generate_binary_price_dict(800),
            history_price_is_valid=False,
        ),
        # valid discount, blue offer, history price passed validation,
        # history price should be in indexarc
        default_blue_genlog(
            offer_id='3',
            binary_price=generate_binary_price_dict(600),
            binary_oldprice=generate_binary_price_dict(700),
            binary_unverified_oldprice=generate_binary_price_dict(700),
            binary_history_price=generate_binary_price_dict(800),
            snippet_history_price='800',
            history_price_is_valid=True,
        ),
        # valid discount, blue offer, history price didn't pass validation,
        # history price should be in indexarc
        default_blue_genlog(
            offer_id='4',
            binary_price=generate_binary_price_dict(600),
            binary_oldprice=generate_binary_price_dict(700),
            binary_unverified_oldprice=generate_binary_price_dict(700),
            binary_history_price=generate_binary_price_dict(800),
            snippet_history_price='800',
            history_price_is_valid=False,
        ),
    ]


@pytest.fixture(scope="module")
def genlog_table(yt_server, genlog_rows):
    genlog_table = GenlogOffersTable(yt_server, ypath_join(get_yt_prefix(), '0000'), genlog_rows)
    genlog_table.dump()
    return genlog_table


@pytest.yield_fixture(scope='module')
def offers_processor_workflow(yt_server, genlog_table):
    input_table_paths = [genlog_table.get_path()]

    with OffersProcessorTestEnv(yt_server, use_genlog_scheme=True, input_table_paths=input_table_paths) as env:
        env.execute()
        yield env


@pytest.yield_fixture(scope='module')
def genlog_snippet_workflow(yt_server, offers_processor_workflow):
    genlogs = []
    for id, glProto in enumerate(offers_processor_workflow.genlog_dicts):
        genlogs.append(glProto)

    with SnippetDiffBuilderTestEnv(
        'genlog_snippet_workflow',
        yt_server,
        offers=[],
        genlogs=genlogs,
        models=[],
        state=[],
    ) as env:
        env.execute()
        env.verify()
        yield env


@pytest.yield_fixture(scope="module")
def expected_binary_history_price_snippet():
    return [
        {
            'offer_id': '1',
            'history_price': '800',
            'history_price_is_valid': None,  # it means ok
            'unverified_old_price': '700',
        },
        {
            'offer_id': '2',
            'history_price': None,
            'history_price_is_valid': '0',
            'unverified_old_price': '700',
        },
        {
            'offer_id': '3',
            'history_price': '800',
            'history_price_is_valid': None,  # it means ok
            'unverified_old_price': '700',
        },
        {
            'offer_id': '4',
            'history_price': '800',  # blue specific
            'history_price_is_valid': '0',
            'unverified_old_price': '700',
        },
    ]
