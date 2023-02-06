# coding=utf-8

from hamcrest import (
    assert_that,
)
import pytest
import datetime
import calendar

from market.idx.generation.yatf.matchers.snippet_diff_builder.env_matchers import HasOutputStateRecord
from market.idx.generation.yatf.test_envs.snippet_diff_builder import SnippetDiffBuilderTestEnv

from market.idx.offers.yatf.resources.offers_indexer.model_sale_dates import ModelSaleDates
from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.offers.yatf.utils.fixtures import default_genlog
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join


SALE_DATE = datetime.datetime.now().date()
SALE_BEGIN_TIMESTAMP = calendar.timegm(SALE_DATE.timetuple())

test_data = [
    {
        'offer_id': '1',
        'model_sale_begin_ts': str(SALE_BEGIN_TIMESTAMP),
    },
]


@pytest.fixture(scope="module")
def model_sale_dates():
    return ModelSaleDates({1: SALE_BEGIN_TIMESTAMP})


@pytest.fixture(scope="module")
def genlog_rows():
    feed = []
    for data in test_data:
        offer = default_genlog(
            offer_id=data['offer_id'],
            model_id=1,
        )
        feed.append(offer)
    return feed


@pytest.fixture(scope="module")
def genlog_table(yt_server, genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0000'), genlog_rows)
    genlog_table.dump()
    return genlog_table


@pytest.yield_fixture(scope='module')
def offers_processor_workflow(yt_server, model_sale_dates, genlog_table):
    input_table_paths = [genlog_table.get_path()]
    resources = {
        'model_sale_dates': model_sale_dates,
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


def test_model_sale_begin_ts_snippet(genlog_snippet_workflow):
    expected_archive = [
        {
            'offer_id': x['offer_id'],
            'model_sale_begin_ts': x['model_sale_begin_ts'],
        }
        for x in test_data
    ]
    for expected in expected_archive:
        assert_that(
            genlog_snippet_workflow,
            HasOutputStateRecord({'value': expected})
        )
