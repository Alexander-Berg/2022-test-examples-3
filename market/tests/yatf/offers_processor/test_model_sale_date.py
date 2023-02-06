# coding: utf-8

""" Проверяет прокидывание sale_date в файлике model_sale_dates.gz до поля model_sale_begin_ts оффера в генлоге. """

import pytest
import datetime
import calendar

from market.idx.offers.yatf.resources.offers_indexer.model_sale_dates import ModelSaleDates
from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.offers.yatf.utils.fixtures import default_genlog, genererate_default_pictures
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join

SALE_DATE = datetime.datetime.now().date()
SALE_BEGIN_TIMESTAMP = calendar.timegm(SALE_DATE.timetuple())
from market.idx.offers.yatf.matchers.offers_processor.env_matchers import HasGenlogRecord
from hamcrest import assert_that


@pytest.fixture(scope="module")
def genlog_rows():
    return [
        default_genlog(**{
            # model 1 is ok: we have record with this model in model_sale_dates.gz : SALE_BEGIN_TIMESTAMP
            # offer with sale date
            'model_id': 1,
            'offer_id': '1',
        }),
        default_genlog(**{
            # cluster 2 is fine too: we have record with this cluster in model_sale_dates.gz : SALE_BEGIN_TIMESTAMP + 1
            # offer with sale date
            'offer_id': '7',
            'model_id': 0,
            'cluster_id': 2,
            'pictures': genererate_default_pictures(),
        }),
        default_genlog(**{
            # model 3 is not ok: we don't have record with this model in model_sale_dates.gz
            # offer without sale date
            'model_id': 3,
            'offer_id': '3',
        })
    ]


@pytest.fixture(scope="module")
def genlog_table(yt_server, genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0000'), genlog_rows)
    genlog_table.dump()
    return genlog_table


@pytest.fixture(scope="module")
def model_sale_dates():
    return ModelSaleDates(
        {
            1: SALE_BEGIN_TIMESTAMP,
            2: SALE_BEGIN_TIMESTAMP+1
        }
    )


@pytest.yield_fixture(scope="module")
def workflow(genlog_table, model_sale_dates, yt_server):
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
        env.verify()
        yield env


def test_sale_begin_ts_field(workflow):
    assert_that(
        workflow,
        HasGenlogRecord({
            'offer_id': '1',
            'model_id': 1,
            'model_sale_begin_ts': str(SALE_BEGIN_TIMESTAMP)
        })
    )

    assert_that(
        workflow,
        HasGenlogRecord({
            'offer_id': '7',
            'model_sale_begin_ts': str(SALE_BEGIN_TIMESTAMP+1)
        })
    )

    for genlog in workflow.genlog:
        if genlog.offer_id == '3':
            assert not genlog.model_sale_begin_ts
