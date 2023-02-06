# coding: utf-8

import pytest
from hamcrest import assert_that

from market.idx.generation.yatf.test_envs.mr_mindexer import MrMindexerBuildTestEnv, MrMindexerMergeTestEnv
from market.idx.generation.yatf.resources.mr_mindexer.mr_mindexer_helpers import MrMindexerMergeOptions, MrMindexerMergeIndexType

from market.idx.offers.yatf.matchers.offers_indexer.env_matchers import HasLiterals, HasNoLiterals
from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.offers.yatf.utils.fixtures import default_genlog
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join
import yt.wrapper as yt


@pytest.fixture(scope="module")
def genlog_rows():
    return [
        default_genlog(ware_md5='jaPRZC2qhM8tmb0yQQapzA'),
        default_genlog(ware_md5='cxlZ3cTeKNxAzS6OMWX51g',
                       installment_options=[
                           {'installment_time_in_days': [yt.yson.YsonUint64(10), yt.yson.YsonUint64(20)],
                            'bnpl_available': False
                            }
                       ]),
        default_genlog(ware_md5='XwMatVDskhEwoWXenZRifQ',
                       installment_options=[
                           {
                               'installment_time_in_days': [yt.yson.YsonUint64(30), yt.yson.YsonUint64(60)],
                               'bnpl_available': False
                           },
                           {
                               'installment_time_in_days': [yt.yson.YsonUint64(45), yt.yson.YsonUint64(60)],
                               'bnpl_available': True
                           },
                       ])
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
            input_table_paths=input_table_paths
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


def test_installment_options(mr_mindexer_direct):
    assert_that(mr_mindexer_direct, HasNoLiterals('#has_installment', [0]))
    assert_that(mr_mindexer_direct, HasNoLiterals('#bnpl_available', [0]))

    assert_that(mr_mindexer_direct, HasLiterals('#has_installment="1', [1]))
    assert_that(mr_mindexer_direct, HasNoLiterals('#bnpl_available', [1]))

    assert_that(mr_mindexer_direct, HasLiterals('#has_installment="1', [2]))
    assert_that(mr_mindexer_direct, HasLiterals('#bnpl_available="1', [2]))
