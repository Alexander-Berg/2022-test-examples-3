# coding: utf-8

import pytest
from hamcrest import assert_that, all_of
import logging

from market.idx.generation.yatf.test_envs.mr_mindexer import MrMindexerBuildTestEnv, MrMindexerMergeTestEnv
from market.idx.generation.yatf.resources.mr_mindexer.mr_mindexer_helpers import MrMindexerMergeOptions, MrMindexerMergeIndexType

from market.idx.offers.yatf.matchers.offers_indexer.env_matchers import HasLiterals, HasNoLiterals
from market.idx.yatf.resources.glue_config import GlueConfig

import yt.wrapper as yt


@pytest.fixture(scope='module')
def offers():
    return [{'feed_id': 1, 'offer_id': '1', 'glue_fields': [{'glue_id': yt.yson.YsonUint64(15), 'int64_value': 1337}]}]


@pytest.fixture(scope='module')
def glue_config():
    return GlueConfig(
    {'Fields': [
        {
            'glue_id': 0,
            'declared_cpp_type': 'UINT32',
            'target_name': 'useless',
            'is_from_datacamp': True,
            'source_field_path': 'unused'
        },
        {
            'glue_id': 15,
            'declared_cpp_type': 'INT64',
            'target_name': 'mbid',
            'is_from_datacamp': True,
            'source_field_path': 'unused2',
            'use_as_search_literal': True
        }
    ]}, 'glue_config.json')


@pytest.yield_fixture(scope="module")
def mr_mindexer_direct(yt_server, offers, glue_config):
    build_resources = {
        'glue_config': glue_config
    }

    with MrMindexerBuildTestEnv(**build_resources) as build_env:
        build_env.execute_from_offers_list(yt_server, offers)
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


def test_literals(mr_mindexer_direct):
    logging.info(mr_mindexer_direct.literal_lemmas.literals)
    assert_that(mr_mindexer_direct, all_of(
        HasLiterals('#mbid="1337', ['0']),
        HasNoLiterals('#useless', ['0'])
    ))
