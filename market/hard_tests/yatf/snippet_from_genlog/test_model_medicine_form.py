# coding=utf-8

from hamcrest import (
    assert_that,
)
import pytest

from market.idx.generation.yatf.matchers.snippet_diff_builder.env_matchers import HasOutputStateRecord
from market.idx.generation.yatf.test_envs.snippet_diff_builder import SnippetDiffBuilderTestEnv

from market.idx.offers.yatf.resources.offers_indexer.model_medicine_form import ModelMedicineForm
from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.offers.yatf.utils.fixtures import default_genlog
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join


test_data = [
    {
        'offer_id': '1',
        'model_id': 1,
        'model_medicine_form_param': '123',
        'model_medicine_form_option': '456',
    },
    {
        'offer_id': '2',
        'model_id': 2,
        'model_medicine_form_param': None,
        'model_medicine_form_option': None,
    },
]


@pytest.fixture(scope="module")
def model_medicine_form():
    return ModelMedicineForm([(1, 123, 456)])


@pytest.fixture(scope="module")
def genlog_rows():
    feed = []
    for data in test_data:
        offer = default_genlog(
            offer_id=data['offer_id'],
            model_id=data['model_id'],
        )
        feed.append(offer)
    return feed


@pytest.fixture(scope="module")
def genlog_table(yt_server, genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0000'), genlog_rows)
    genlog_table.dump()
    return genlog_table


@pytest.yield_fixture(scope='module')
def offers_processor_workflow(yt_server, model_medicine_form, genlog_table):
    input_table_paths = [genlog_table.get_path()]

    resources = {
        'model_medicine_form': model_medicine_form,
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


def test_model_medicine_form_snippet(genlog_snippet_workflow):
    expected_archive = [
        {
            'offer_id': x['offer_id'],
            'model_medicine_form_param': x['model_medicine_form_param'],
            'model_medicine_form_option': x['model_medicine_form_option'],
        }
        for x in test_data
    ]
    for expected in expected_archive:
        assert_that(
            genlog_snippet_workflow,
            HasOutputStateRecord({'value': expected})
        )
