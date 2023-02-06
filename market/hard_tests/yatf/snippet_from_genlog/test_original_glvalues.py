# coding: utf-8

from base64 import b64encode
from hamcrest import (
    assert_that,
)
import pytest

from market.idx.generation.yatf.matchers.snippet_diff_builder.env_matchers import HasOutputStateRecord
from market.idx.generation.yatf.test_envs.snippet_diff_builder import SnippetDiffBuilderTestEnv

from market.idx.offers.yatf.resources.offers_indexer.gl_mbo_pb import GlMboPb
from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.offers.yatf.utils.fixtures import default_genlog

from market.proto.content.mbo.MboParameters_pb2 import (
    Category,
    Parameter,
    BOOLEAN,
    ENUM,
    NUMERIC,
    NUMERIC_ENUM,
)
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join
import yt.wrapper as yt


@pytest.fixture(scope="module")
def gl_mbo():
    return [
        Category(
            hid=90401,
            parameter=[
                Parameter(
                    id=1,
                    xsl_name='bool_param1',
                    value_type=BOOLEAN,
                    published=True
                ),
                Parameter(
                    id=2,
                    xsl_name='numeric_param1',
                    value_type=NUMERIC,
                    published=True
                ),
                Parameter(
                    id=3,
                    xsl_name='enum_param1',
                    value_type=ENUM,
                    published=True
                ),
                Parameter(
                    id=4,
                    xsl_name='numeric_enum_param1',
                    value_type=NUMERIC_ENUM,
                    published=True
                ),
            ]
        )
    ]


@pytest.fixture(scope="module")
def genlog_rows():
    return [
        default_genlog(
            offer_id='msku',
            category_id=90401,
            market_sku=999,
            is_fake_msku_offer=True,
            offer_params=[
                {'enriched_param_id': yt.yson.YsonUint64(1), 'id': yt.yson.YsonUint64(1)},
                {'enriched_param_id': yt.yson.YsonUint64(2), 'num': 101.0, 'original': True},
                {'enriched_param_id': yt.yson.YsonUint64(3), 'id': yt.yson.YsonUint64(102), 'original': True},
                {'enriched_param_id': yt.yson.YsonUint64(3), 'id': yt.yson.YsonUint64(103)},
                {'enriched_param_id': yt.yson.YsonUint64(4), 'id': yt.yson.YsonUint64(104)},
            ]
        ),
        default_genlog(
            offer_id='not_msku',
            category_id=90401,
            market_sku=999,
            is_fake_msku_offer=False,
            offer_params=[
                {'enriched_param_id': yt.yson.YsonUint64(1), 'id': yt.yson.YsonUint64(100), 'original': True},
                {'enriched_param_id': yt.yson.YsonUint64(2), 'num': 101.0, 'original': True},
                {'enriched_param_id': yt.yson.YsonUint64(3), 'id': yt.yson.YsonUint64(102), 'original': True},
                {'enriched_param_id': yt.yson.YsonUint64(3), 'id': yt.yson.YsonUint64(103)},
                {'enriched_param_id': yt.yson.YsonUint64(4), 'id': yt.yson.YsonUint64(104)},
            ]
        ),
    ]


@pytest.fixture(scope="module")
def genlog_table(yt_server, genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0000'), genlog_rows)
    genlog_table.dump()
    return genlog_table


@pytest.yield_fixture(scope='module')
def offers_processor_workflow(yt_server, gl_mbo, genlog_table):
    input_table_paths = [genlog_table.get_path()]
    resources = {
        'gl_mbo_pbuf_sn': GlMboPb(gl_mbo)
    }

    with OffersProcessorTestEnv(
            yt_server,
            use_genlog_scheme=True,
            input_table_paths=input_table_paths,
            **resources
    ) as env:
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
def expected_original_glvalues_snippet():
    # (@bzz13) I am so sorry. (gromoi) :((((((
    result = '\x01\x00\x00\x00'                     # vector size
    result += '\x03\x00\x00\x00\x00\x00\x00\x00'    # param_id = 3
    result += '\x66\x00\x00\x00\x00\x00\x00\x00'    # value_id = 102
    expected = b64encode(result)

    return [
        {
            'offer_id': 'msku',
            'original_glvalues': expected,
        },
        {
            'offer_id': 'not_msku',
            'original_glvalues': None,
        },
    ]


def test_original_glvalues_snippet(
        genlog_snippet_workflow,
        expected_original_glvalues_snippet
):
    for expected in expected_original_glvalues_snippet:
        assert_that(
            genlog_snippet_workflow,
            HasOutputStateRecord({'value': expected})
        )
