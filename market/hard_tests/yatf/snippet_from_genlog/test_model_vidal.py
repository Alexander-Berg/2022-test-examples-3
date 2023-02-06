# coding=utf-8

from hamcrest import assert_that
import pytest

from market.idx.generation.yatf.matchers.snippet_diff_builder.env_matchers import HasOutputStateRecord
from market.idx.generation.yatf.test_envs.snippet_diff_builder import SnippetDiffBuilderTestEnv
from market.idx.offers.yatf.resources.offers_indexer.model_vidal import ModelVidal
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


VIDAL_MODEL_ID=1
VIDAL_ATC_CODE=23181290
VIDAL_XLS_NAME='ATCCode'
VIDAL_ATC_CODE_VALUE='j05ax13'


test_data = [
    {
        'offer_id': '1',
        'vidal_atc_code': VIDAL_ATC_CODE_VALUE,
    },
]


@pytest.fixture(scope="module")
def model_vidal():
    return ModelVidal([
        ExportReportModel(
            id=VIDAL_MODEL_ID,
            parameter_values=[
                ParameterValue(
                    param_id=VIDAL_ATC_CODE,
                    xsl_name=VIDAL_XLS_NAME,
                    str_value=[
                        LocalizedString(value=VIDAL_ATC_CODE_VALUE)
                    ]
                )
            ]
        )
    ])


@pytest.fixture(scope="module")
def genlog_rows():
    offers = []
    for data in test_data:
        offer = default_genlog(
            offer_id=data['offer_id'],
            model_id=VIDAL_MODEL_ID,
        )
        offers.append(offer)
    return offers


@pytest.fixture(scope="module")
def genlog_table(yt_server, genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0000'), genlog_rows)
    genlog_table.dump()
    return genlog_table


@pytest.yield_fixture(scope='module')
def offers_processor_workflow(yt_server, model_vidal, genlog_table):
    input_table_paths = [genlog_table.get_path()]
    resources = {
        'model_vidal': model_vidal,
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


def test_model_vidal_snippet(genlog_snippet_workflow):
    expected_archive = [
        {
            'offer_id': t['offer_id'],
            'vidal_atc_code': t['vidal_atc_code'],
        }
        for t in test_data
    ]
    for expected in expected_archive:
        assert_that(
            genlog_snippet_workflow,
            HasOutputStateRecord({'value': expected})
        )
