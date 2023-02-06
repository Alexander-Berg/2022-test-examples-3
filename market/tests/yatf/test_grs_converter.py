# coding: utf-8

import uuid

import pytest
from hamcrest import assert_that, equal_to

from yt.wrapper import ypath_join

from market.proto.indexer.StatsCalc_pb2 import ModelRegionalStats
from market.idx.yatf.resources.yt_table_resource import YtTableResource

from mapreduce.yt.python.table_schema import extract_column_attributes

from grs4banner_converter.yatf.test_env import GrsBannerConverterTestEnv


@pytest.fixture(scope='session')
def filter_regions():
    return 2, 213


@pytest.fixture(scope='session')
def min_offers_count():
    return 2


test_data = [
    # Простой тест на две записи в статистике, где первая содержит целевые регионы, второая не связана с ними
    ('Simple test',
     [ModelRegionalStats.Record(model_id=1000787L, noffers=20L,
                                nretailers=1L, onstock=2L, bid1=6L,
                                prices=[
                                    ModelRegionalStats.Price(),
                                    ModelRegionalStats.Price(
                                        median_price=9.5,
                                        max_price=10,
                                        min_price=9,
                                        min_oldprice=9.8
                                    )
                                ],
                                regions=[2, 213], dc_count=0L),
      ModelRegionalStats.Record(model_id=18276L, noffers=10L,
                                nretailers=1L, onstock=2L, bid1=6L,
                                prices=[
                                    ModelRegionalStats.Price(),
                                    ModelRegionalStats.Price(
                                        median_price=9.5,
                                        max_price=10,
                                        min_price=9,
                                        min_oldprice=9.8
                                    )
                                ],
                                regions=[131109], dc_count=0L),
      ],
     [{'model_id': 1000787L,
       'region': 2L,
       'noffers': 20L,
       'rub_min_price': 9.0,
       'rub_max_price': 10.0
       },
      {'model_id': 1000787L,
       'region': 213L,
       'noffers': 20L,
       'rub_min_price': 9.0,
       'rub_max_price': 10.0
       },
      ]),

    # В статистике единсвенная запись с регионом являющимся родительским для двух целевых
    ('Parent region for both filtered',
     [ModelRegionalStats.Record(model_id=1000787L, noffers=20L,
                                nretailers=1L, onstock=2L, bid1=6L,
                                prices=[
                                    ModelRegionalStats.Price(),
                                    ModelRegionalStats.Price(
                                        median_price=9.5,
                                        max_price=10,
                                        min_price=9,
                                        min_oldprice=9.8
                                    )
                                ],
                                regions=[225], dc_count=0L),
      ],
     [{'model_id': 1000787L,
       'region': 2L,
       'noffers': 20L,
       'rub_min_price': 9.0,
       'rub_max_price': 10.0
       },
      {'model_id': 1000787L,
       'region': 213L,
       'noffers': 20L,
       'rub_min_price': 9.0,
       'rub_max_price': 10.0
       },
      ]),

    # В статистике запись с регоном не связанным родсвенными связями с целевыми
    ('No region for both filtered',
     [ModelRegionalStats.Record(model_id=1000787L, noffers=20L,
                                nretailers=1L, onstock=2L, bid1=6L,
                                prices=[
                                    ModelRegionalStats.Price(),
                                    ModelRegionalStats.Price(
                                        median_price=9.5,
                                        max_price=10,
                                        min_price=9,
                                        min_oldprice=9.8
                                    )
                                ],
                                regions=[131073], dc_count=0L),
      ],
     []),

    # В статистике запись с регионом - дочернрим для одного из целевых
    ('Child region not include for filtered',
     [ModelRegionalStats.Record(model_id=1000787L, noffers=20L,
                                nretailers=1L, onstock=2L, bid1=6L,
                                prices=[
                                    ModelRegionalStats.Price(),
                                    ModelRegionalStats.Price(
                                        median_price=9.5,
                                        max_price=10,
                                        min_price=9,
                                        min_oldprice=9.8
                                    )
                                ],
                                regions=[216], dc_count=0L),
      ],
     []),

    # Фильтруем статистику по минимальному числу оферов (2)
    ('Check min offers filter regions',
     [ModelRegionalStats.Record(model_id=1000787L, noffers=1L,
                                nretailers=1L, onstock=2L, bid1=6L,
                                prices=[
                                    ModelRegionalStats.Price(),
                                    ModelRegionalStats.Price(
                                        median_price=9.5,
                                        max_price=10,
                                        min_price=9,
                                        min_oldprice=9.8
                                    )
                                ],
                                regions=[213], dc_count=0L),
      ModelRegionalStats.Record(model_id=1000787L, noffers=20L,
                                nretailers=1L, onstock=2L, bid1=6L,
                                prices=[
                                    ModelRegionalStats.Price(),
                                    ModelRegionalStats.Price(
                                        median_price=9.5,
                                        max_price=10,
                                        min_price=9,
                                        min_oldprice=9.8
                                    )
                                ],
                                regions=[213], dc_count=0L),
      ],
     [{'model_id': 1000787L,
       'region': 213L,
       'noffers': 20L,
       'rub_min_price': 9.0,
       'rub_max_price': 10.0
       },
      ]),
]


@pytest.fixture(params=test_data, ids=["SIMPLE", "PARENT", "NOREGION", "CHILD", "MINOFFERS"], scope='module')
def grs_records(request):
    return request.param


@pytest.fixture(scope='module')
def grs_stat_table(yt_server, grs_records):
    _, data, _ = grs_records
    table = YtTableResource(
        yt_server,
        ypath_join("//home", str(uuid.uuid4())),
        data=[dict(key='k', value=r.SerializeToString()) for r in data]
    )
    table.create()

    return table


@pytest.yield_fixture(scope='module')
def workflow(yt_server, grs_stat_table, filter_regions, min_offers_count):
    with GrsBannerConverterTestEnv(yt_server, **{}) as env:
        env.execute(yt_server,
                    output_table=ypath_join("//home", str(uuid.uuid4())),
                    input_table=grs_stat_table.get_path(),
                    regions=filter_regions,
                    min_offers=min_offers_count
                    )

        env.verify()
        yield env


@pytest.fixture(scope='module')
def result_yt_table(workflow):
    return workflow.outputs.get('result_table')


def test_result_table_exist(result_yt_table, yt_server):
    assert_that(yt_server.get_yt_client().exists(result_yt_table.get_path()), 'Table exist')


def test_result_table_schema(result_yt_table):
    assert_that(extract_column_attributes(list(result_yt_table.schema)),
                equal_to([
                    {'required': False, "name": "model_id", "type": "uint64"},
                    {'required': False, "name": "region", "type": "uint64"},
                    {'required': False, "name": "noffers", "type": "uint64"},
                    {'required': False, "name": "rub_min_price", "type": "double"},
                    {'required': False, "name": "rub_max_price", "type": "double"}
                ]), "Schema is correct")


def test_expected_result(result_yt_table, grs_records):
    msg, _, result = grs_records
    assert_that(result_yt_table.data, equal_to(result), msg)
