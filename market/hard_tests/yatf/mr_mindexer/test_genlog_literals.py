# coding: utf-8

import pytest
from hamcrest import assert_that
import logging

from market.idx.generation.yatf.test_envs.mr_mindexer import MrMindexerBuildTestEnv, MrMindexerMergeTestEnv
from market.idx.generation.yatf.resources.mr_mindexer.mr_mindexer_helpers import MrMindexerMergeOptions, MrMindexerMergeIndexType

from market.idx.offers.yatf.matchers.offers_indexer.env_matchers import HasLiterals, HasNoLiterals
from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.offers.yatf.utils.fixtures import default_genlog
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join


@pytest.fixture(scope="module")
def genlog_rows():
    return [
        default_genlog(is_eda=True),
        default_genlog(is_eda=False),
        default_genlog(is_lavka=True),
        default_genlog(is_lavka=False),
        default_genlog(is_direct=True),
        default_genlog(is_direct=False),
        default_genlog(is_express=True),
        default_genlog(is_express=False),
        default_genlog(warehouse_id=145),
    ]


@pytest.fixture(scope="module")
def genlog_table(yt_server, genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0000'), genlog_rows)
    genlog_table.dump()
    return genlog_table


@pytest.yield_fixture(scope='module')
def offers_processor_workflow(yt_server, genlog_table):
    input_table_paths = [genlog_table.get_path()]

    with OffersProcessorTestEnv(
            yt_server,
            use_genlog_scheme=True,
            input_table_paths=input_table_paths,
    ) as env:
        env.execute()
        yield env


@pytest.yield_fixture(scope="module")
def mr_mindexer_direct(yt_server, offers_processor_workflow):
    with MrMindexerBuildTestEnv() as build_env:
        build_env.execute_from_offers_list(yt_server, offers_processor_workflow.genlog_dicts)
        build_env.verify()

        resourses = {
            'merge_options': MrMindexerMergeOptions(
                input_portions_path=build_env.yt_index_portions_path,
                part=0,
                index_type=MrMindexerMergeIndexType.DIRECT,
            ),
        }

        with MrMindexerMergeTestEnv(**resourses) as env:
            env.execute(yt_server)
            env.verify()
            yield env


@pytest.fixture(scope="module")
def checkable():
    # genlog field -> literal name
    return {
        'is_eda': 'is_eda',
        'is_lavka': 'is_lavka',
        'is_direct': 'is_direct',
        'is_express': 'is_express',
        'warehouse_id': 'warehouse_id',
    }


def test_literals(genlog_rows, mr_mindexer_direct, checkable):
    logging.info(mr_mindexer_direct.literal_lemmas.literals)
    doc_id = -1
    for offer in genlog_rows:
        doc_id += 1

        for pair in checkable:
            genlog_field = pair[0]
            literal_name = pair[1]

            if offer.get(genlog_field) is True:
                assert_that(mr_mindexer_direct, HasLiterals('#' + literal_name + '="1', [doc_id]))
            elif offer.get(genlog_field) is False:
                assert_that(mr_mindexer_direct, HasLiterals('#' + literal_name + '="0', [doc_id]))
            elif offer.get(genlog_field) is not None:
                assert_that(mr_mindexer_direct, HasLiterals('#' + literal_name + '="{}'.format(offer.get(genlog_field)), [doc_id]))
            else:
                assert_that(mr_mindexer_direct, HasNoLiterals('#' + literal_name, [doc_id]))
