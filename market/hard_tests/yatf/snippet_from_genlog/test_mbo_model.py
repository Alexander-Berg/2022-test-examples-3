# coding: utf-8

from base64 import b64encode
from hamcrest import (
    assert_that,
)
import pytest

from market.idx.generation.yatf.matchers.snippet_diff_builder.env_matchers import HasOutputStateRecord
from market.idx.generation.yatf.test_envs.snippet_diff_builder import SnippetDiffBuilderTestEnv

from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.offers.yatf.utils.fixtures import default_genlog

from market.proto.content.mbo.ExportReportModel_pb2 import (
    ExportReportModel,
    ParameterValue,
    LocalizedString,
)
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join


MBO_MODEL = ExportReportModel(
    parameter_values=[
        ParameterValue(
            param_id=101,
            bool_value=True,
        ),
        ParameterValue(
            param_id=102,
            option_id=1102,
        ),
        ParameterValue(
            param_id=103,
            str_value=[
                LocalizedString(
                    isoCode='RU',
                    value='text_value'
                ),
            ]
        ),
        ParameterValue(
            param_id=104,
            numeric_value='1104',
        ),
    ]
)


@pytest.fixture(scope="module")
def genlog_rows():
    return [
        default_genlog(
            offer_id='OFFER_WITH_MBO',
        ),
        default_genlog(
            offer_id='OFFER_WITHOUT_MBO',
        ),
        default_genlog(
            offer_id='BLUE_OFFER_WITH_MBO',
            is_blue_offer=True,
        ),
        default_genlog(
            offer_id='BLUE_OFFER_WITHOUT_MBO',
            is_blue_offer=True,
        ),
        default_genlog(
            offer_id='MSKU_WITH_MBO',
            is_fake_msku_offer=True,
            mbo_model=b64encode(MBO_MODEL.SerializeToString()),
        ),
        default_genlog(
            offer_id='MSKU_WITHOUT_MBO',
            is_fake_msku_offer=True,
        ),
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
def expected_mbo_models_snippet():
    return [
        {
            'offer_id': 'OFFER_WITH_MBO',
            'MboModel': None,
        },
        {
            'offer_id': 'OFFER_WITHOUT_MBO',
            'MboModel': None,
        },
        {
            'offer_id': 'BLUE_OFFER_WITH_MBO',
            'MboModel': None,
        },
        {
            'offer_id': 'BLUE_OFFER_WITHOUT_MBO',
            'MboModel': None,
        },
        {
            'offer_id': 'MSKU_WITH_MBO',
            'MboModel': b64encode(MBO_MODEL.SerializeToString()),
        },
        {
            'offer_id': 'MSKU_WITHOUT_MBO',
            'MboModel': None,
        },
    ]


def test_mbo_model_snippet(genlog_snippet_workflow, expected_mbo_models_snippet):
    for expected in expected_mbo_models_snippet:
        assert_that(
            genlog_snippet_workflow,
            HasOutputStateRecord({'value': expected})
        )
