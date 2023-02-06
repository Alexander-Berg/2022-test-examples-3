# coding=utf-8
import pytest

from hamcrest import assert_that, equal_to

from market.proto.indexer.StatsCalc_pb2 import ModelRegionalStats

from market.idx.tools.market_yt_data_upload.yatf.test_env import YtDataUploadTestEnv
from market.idx.yatf.resources.yt_table_resource import YtTableResource


@pytest.fixture(scope='module')
def grs_records():
    return [ModelRegionalStats.Record(model_id=1000787, noffers=2, nretailers=1, onstock=2, bid1=6,
                                      prices=[ModelRegionalStats.Price(),
                                              ModelRegionalStats.Price(median_price=10, max_price=10.1,
                                                                       min_price=9.9, min_oldprice=9.8)],
                                      regions=[10295], dc_count=0),
            ModelRegionalStats.Record(model_id=1000787, noffers=2, nretailers=1, onstock=1, bid1=50,
                                      prices=[ModelRegionalStats.Price(),
                                              ModelRegionalStats.Price(median_price=10, max_price=10.1,
                                                                       min_price=9.9, min_oldprice=9.8)],
                                      regions=[157, 159, 10253, 10262, 10281, 10324, 10335], dc_count=0)
            ]


@pytest.fixture(scope='module')
def grs_stat_table(yt_server, grs_records):
    table = YtTableResource(yt_server, "//home/in/grs",
                            data=[dict(key='k', value=r.SerializeToString()) for r in grs_records])
    table.dump()

    return table


@pytest.yield_fixture(scope='module')
def workflow(yt_server, grs_stat_table):
    resources = {}

    with YtDataUploadTestEnv(**resources) as env:
        env.execute(yt_server, type='grs', output_table="//home/test/grs", input_table=grs_stat_table.get_path())
        env.verify()
        yield env


@pytest.fixture(scope='module')
def result_yt_table(workflow):
    return workflow.outputs.get('result_table')


def test_result_table_exist(result_yt_table, yt_server):
    assert_that(yt_server.get_yt_client().exists(result_yt_table.get_path()), 'Table exist')


def test_result_table_row_count(result_yt_table, grs_records):
    assert_that(len(result_yt_table.data), equal_to(len(grs_records)), "Rows count equal count of grs records in table")
