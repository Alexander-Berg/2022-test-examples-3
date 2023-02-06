# coding: utf-8

"""Проверяет прокидывание published_on_blue_market до флага синего оффера.
"""

from hamcrest import assert_that, equal_to
import pytest

from market.idx.generation.yatf.test_envs.mr_mindexer import MrMindexerBuildTestEnv, MrMindexerMergeTestEnv
from market.idx.generation.yatf.resources.mr_mindexer.mr_mindexer_helpers import MrMindexerMergeOptions, MrMindexerMergeIndexType

from market.idx.offers.yatf.matchers.offers_indexer.env_matchers import HasLiterals
from market.idx.yatf.resources.model_ids import ModelIds
from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.offers.yatf.utils.fixtures import default_genlog
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join


@pytest.fixture(scope="module")
def genlog_rows():
    blue_model_blue_offer = default_genlog()
    blue_model_blue_offer['offer_id'] = '1'
    blue_model_blue_offer['model_id'] = 5
    blue_model_blue_offer['is_blue_offer'] = True
    blue_model_blue_offer['ware_md5'] = 'nomkVuHL2Q0wYGPdnvvfKg'

    white_model_blue_offer = default_genlog()
    white_model_blue_offer['offer_id'] = '2'
    white_model_blue_offer['model_id'] = 2
    white_model_blue_offer['is_blue_offer'] = True
    white_model_blue_offer['ware_md5'] = 'AomkVuHL2Q0wYGPdnvvfKg'

    white_model_white_offer = default_genlog()
    white_model_white_offer['offer_id'] = '3'
    white_model_white_offer['model_id'] = 2
    white_model_white_offer['ware_md5'] = 'BomkVuHL2Q0wYGPdnvvfKg'

    return [
        blue_model_blue_offer,
        white_model_blue_offer,
        white_model_white_offer
    ]


@pytest.fixture(scope="module")
def model_ids():
    return ModelIds([1, 2, 3, 666], blue_ids=[4, 5])


@pytest.fixture(scope="module")
def genlog_table(yt_server, genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0000'), genlog_rows)
    genlog_table.dump()
    return genlog_table


@pytest.yield_fixture(scope='module')
def offers_processor_workflow(yt_server, genlog_table, model_ids):
    input_table_paths = [genlog_table.get_path()]
    resources = {
        'model_ids': model_ids,
    }

    with OffersProcessorTestEnv(
            yt_server,
            use_genlog_scheme=True,
            input_table_paths=input_table_paths,
            **resources
    ) as env:
        env.execute()
        yield env


@pytest.yield_fixture(scope="module")
def mr_mindexer_build(yt_server, offers_processor_workflow):
    with MrMindexerBuildTestEnv() as build_env:
        build_env.execute_from_offers_list(yt_server, offers_processor_workflow.genlog_dicts)
        build_env.verify()
        yield build_env


@pytest.yield_fixture(scope="module")
def mr_mindexer_aa(yt_server, mr_mindexer_build):
    resourses = {
        'merge_options': MrMindexerMergeOptions(
            input_portions_path=mr_mindexer_build.yt_index_portions_path,
            part=0,
            index_type=MrMindexerMergeIndexType.AA,
        ),
    }

    with MrMindexerMergeTestEnv(**resourses) as env:
        env.execute(yt_server)
        env.verify()
        env.outputs['indexaa'].load()
        yield env


@pytest.yield_fixture(scope="module")
def mr_mindexer_direct(yt_server, mr_mindexer_build):
    resourses = {
        'merge_options': MrMindexerMergeOptions(
            input_portions_path=mr_mindexer_build.yt_index_portions_path,
            part=0,
            index_type=MrMindexerMergeIndexType.DIRECT,
        ),
    }

    with MrMindexerMergeTestEnv(**resourses) as env:
        env.execute(yt_server)
        env.verify()
        yield env


@pytest.yield_fixture(scope="module")
def mr_mindexer_direct_arc(yt_server, mr_mindexer_build):
    resourses = {
        'merge_options': MrMindexerMergeOptions(
            input_portions_path=mr_mindexer_build.yt_index_portions_path,
            part=0,
            index_type=MrMindexerMergeIndexType.DIRECT_ARCH,
        ),
    }

    with MrMindexerMergeTestEnv(**resourses) as env:
        env.execute(yt_server)
        env.verify()
        env.outputs['indexarc'].load()
        yield env


@pytest.fixture(scope="module")
def doc_id_by_offer_id(mr_mindexer_direct_arc):
    mapping = {}
    arc = mr_mindexer_direct_arc.outputs['indexarc']
    for i in arc.doc_ids:
        offer_id = arc.load_doc_description(i)['offer_id']
        mapping[offer_id] = i
    return mapping


def test_blue_offer_params(mr_mindexer_aa, mr_mindexer_direct, doc_id_by_offer_id):
    doc_id = doc_id_by_offer_id['1']
    attrs = mr_mindexer_aa.outputs['indexaa'].get_group_attributes(doc_id)
    assert_that(attrs.get('hyper_ts'), equal_to('ts0'))
    assert_that(attrs.get('hyper_ts_blue'), equal_to('5'))
    assert_that(mr_mindexer_direct, HasLiterals('#hyper_id="5', [doc_id]))

    doc_id = doc_id_by_offer_id['2']
    attrs = mr_mindexer_aa.outputs['indexaa'].get_group_attributes(doc_id)
    assert_that(attrs.get('hyper_ts'), equal_to('2'))
    assert_that(attrs.get('hyper_ts_blue'), equal_to('2'))
    assert_that(mr_mindexer_direct, HasLiterals('#hyper_id="2', [doc_id]))

    doc_id = doc_id_by_offer_id['3']
    attrs = mr_mindexer_aa.outputs['indexaa'].get_group_attributes(doc_id)
    assert_that(attrs.get('hyper_ts'), equal_to('2'))
    assert_that(attrs.get('hyper_ts_blue'), equal_to('2'))
    assert_that(mr_mindexer_direct, HasLiterals('#hyper_id="2', [doc_id]))
