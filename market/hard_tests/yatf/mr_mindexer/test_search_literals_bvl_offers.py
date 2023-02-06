# coding: utf-8

"""
Тест проверяет, что для офферов, у которых есть MBO-параметры
лицензиар, франщиза, персонаж (licensor, hero_global, pers_model)
проставляются соответствующие поисковые литералы
"""

import pytest
from hamcrest import assert_that

from market.idx.generation.yatf.test_envs.mr_mindexer import MrMindexerBuildTestEnv, MrMindexerMergeTestEnv
from market.idx.generation.yatf.resources.mr_mindexer.mr_mindexer_helpers import MrMindexerMergeOptions, MrMindexerMergeIndexType

from market.idx.offers.yatf.matchers.offers_indexer.env_matchers import HasLiterals, HasNoLiterals
from market.idx.offers.yatf.resources.offers_indexer.gl_mbo_pb import GlMboPb
from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.offers.yatf.utils.fixtures import default_genlog

from market.proto.content.mbo.MboParameters_pb2 import Category, Parameter, ENUM
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join
import yt.wrapper as yt


@pytest.fixture(scope="module")
def gl_mbo():
    return [
        Category(
            hid=10682550,
            parameter=[
                # MARETINDEXER-9952
                Parameter(id=15060326, xsl_name='licensor', value_type=ENUM, published=True),
                Parameter(id=14020987, xsl_name='hero_global', value_type=ENUM, published=True),
                Parameter(id=15086295, xsl_name='pers_model', value_type=ENUM, published=True),
            ]
        )
    ]


@pytest.fixture(scope="module")
def genlog_rows():
    return [
        default_genlog(
            ware_md5='09lEaAKkQll1XTaaaaaaaQ',
            offer_id='1',
            category_id=10682550,
            offer_params=[
                {'enriched_param_id': yt.yson.YsonUint64(15060326), 'id': yt.yson.YsonUint64(111)},
                {'enriched_param_id': yt.yson.YsonUint64(14020987), 'id': yt.yson.YsonUint64(222)},
                {'enriched_param_id': yt.yson.YsonUint64(15086295), 'id': yt.yson.YsonUint64(333)},
            ]
        ),
        default_genlog(
            ware_md5='kP3oC5KjARGI5f9EEkNGtA',
            offer_id='2',
            category_id=10682550,
            offer_params=[
                {'enriched_param_id': yt.yson.YsonUint64(15060326), 'id': yt.yson.YsonUint64(444)},
            ]
        ),
        default_genlog(  # wrong category
            ware_md5='yNWXdRty80uhcFXrHX8ohA',
            offer_id='3',
            category_id=90401,
            offer_params=[
                {'enriched_param_id': yt.yson.YsonUint64(14020987), 'id': yt.yson.YsonUint64(555)},
                {'enriched_param_id': yt.yson.YsonUint64(15086295), 'id': yt.yson.YsonUint64(666)},
            ]
        ),
        default_genlog(
            ware_md5='NzA5NDU0MWI2ODgzNDY4Mg',
            offer_id='4',
            category_id=10682550,
            offer_params=[
                {'enriched_param_id': yt.yson.YsonUint64(14020987), 'id': yt.yson.YsonUint64(0)},
                {'enriched_param_id': yt.yson.YsonUint64(15086295), 'id': yt.yson.YsonUint64(0)},
            ]
        ),
    ]


@pytest.fixture(scope="module")
def genlog_table(yt_server, genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0000'), genlog_rows)
    genlog_table.dump()
    return genlog_table


@pytest.yield_fixture(scope='module')
def offers_processor_workflow(yt_server, genlog_table, gl_mbo):
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
        yield env


@pytest.yield_fixture(scope="module")
def mr_mindexer_build(yt_server, offers_processor_workflow):
    with MrMindexerBuildTestEnv() as build_env:
        build_env.execute_from_offers_list(yt_server, offers_processor_workflow.genlog_dicts)
        build_env.verify()
        yield build_env


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


def test_bvl_literals(mr_mindexer_direct, doc_id_by_offer_id):
    # Оффер с поисковыми литералами licensor, hero_global и pers_model
    doc_id = doc_id_by_offer_id['1']
    assert_that(mr_mindexer_direct, HasLiterals('#licensor="111', [doc_id]))
    assert_that(mr_mindexer_direct, HasLiterals('#hero_global="222', [doc_id]))
    assert_that(mr_mindexer_direct, HasLiterals('#pers_model="333', [doc_id]))

    # Оффер с поисковым литералом licensor
    doc_id = doc_id_by_offer_id['2']
    assert_that(mr_mindexer_direct, HasLiterals('#licensor="444', [doc_id]))
    assert_that(mr_mindexer_direct, HasNoLiterals('#hero_global', [doc_id]))
    assert_that(mr_mindexer_direct, HasNoLiterals('#pers_model', [doc_id]))

    # Оффер без BVL поисковых литералов, поскольку в категория нет таких параметров
    doc_id = doc_id_by_offer_id['3']
    assert_that(mr_mindexer_direct, HasNoLiterals('#licensor', [doc_id]))
    assert_that(mr_mindexer_direct, HasNoLiterals('#hero_global', [doc_id]))
    assert_that(mr_mindexer_direct, HasNoLiterals('#pers_model', [doc_id]))

    # Нет литералов, т.к. у одного нет значения, у другого = 0
    doc_id = doc_id_by_offer_id['4']
    assert_that(mr_mindexer_direct, HasNoLiterals('#licensor', [doc_id]))
    assert_that(mr_mindexer_direct, HasNoLiterals('#hero_global', [doc_id]))
    assert_that(mr_mindexer_direct, HasNoLiterals('#pers_model', [doc_id]))
