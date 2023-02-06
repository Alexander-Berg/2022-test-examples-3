#!/usr/bin/env python
# coding: utf-8
import pytest

from market.idx.offers.yatf.utils.fixtures import default_genlog
from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.offers.yatf.matchers.offers_processor.env_matchers import HasGenlogRecord
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join

from hamcrest import assert_that


@pytest.fixture(scope="module")
def genlog_rows():
    return [
        default_genlog(
            **{'classifier_magic_id': '73ba6bb98d2aec3f32056a63fb1b9a04'}
        )
    ]


@pytest.fixture(scope="module")
def genlog_table(yt_server, genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0000'), genlog_rows)
    genlog_table.dump()
    return genlog_table


@pytest.yield_fixture(scope="module")
def workflow(yt_server, genlog_table):
    input_table_paths = [genlog_table.get_path()]

    with OffersProcessorTestEnv(
            yt_server,
            use_genlog_scheme=True,
            input_table_paths=input_table_paths,
    ) as env:
        env.execute()
        env.verify()
        yield env


def test_classifier_magic_id(genlog_rows, workflow):
    expected = genlog_rows[0]['classifier_magic_id']

    assert_that(workflow,
                HasGenlogRecord({'classifier_magic_id': expected}),
                u'В генлоге classifier_magic_id есть')
