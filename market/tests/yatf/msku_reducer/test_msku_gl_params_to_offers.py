# coding: utf-8

import pytest

from hamcrest import assert_that, equal_to

from market.idx.generation.yatf.test_envs.msku_reducer import MskuReducerTestEnv, MskuReducerMode
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogRow
from market.idx.generation.yatf.resources.genlog_dumper.input_records_proto import (
    make_params,
    make_param_entry
)
from market.proto.common.common_pb2 import TCategParamsEntryGenlog as ParamsEntry


FEED_ID = 1
MSKU1=1
MSKU2=2
MSKU3=3
MSKU4=4

MSKU1_WAREMD5='hc1cVZiClnllcxjhGX0_m1'
MSKU2_WAREMD5='hc1cVZiClnllcxjhGX0_m2'
MSKU3_WAREMD5='hc1cVZiClnllcxjhGX0_m3'
OFFER1_WAREMD5='hc1cVZiClnllcxjhGX0_o1'
OFFER2_WAREMD5='hc1cVZiClnllcxjhGX0_o2'
OFFER3_WAREMD5='hc1cVZiClnllcxjhGX0_o3'
OFFER4_WAREMD5='hc1cVZiClnllcxjhGX0_o4'
OFFER5_WAREMD5='hc1cVZiClnllcxjhGX0_o5'

CATEGORY_ID = 1
MODEL_ID = 1

PARAM1_ID = 10
PARAM1_VALUE_ID_OFFER = 100
PARAM1_VALUE_ID_OFFER2 = 102
PARAM1_VALUE_ID_MSKU = 101

PARAM2_ID = 20
PARAM2_VALUE_ID_OFFER = 200
PARAM2_VALUE_ID_MSKU = 201

OFFER_PARAMS = make_params(
    model=MODEL_ID,
    category=CATEGORY_ID,
    values=[
        make_param_entry(key=PARAM1_ID, id=PARAM1_VALUE_ID_OFFER),
        make_param_entry(key=PARAM2_ID, id=PARAM2_VALUE_ID_OFFER),
    ]
)

OFFER2_PARAMS = make_params(
    model=MODEL_ID,
    category=CATEGORY_ID,
    values=[
        make_param_entry(key=PARAM1_ID, id=PARAM1_VALUE_ID_OFFER2),
    ]
)

MSKU_PARAMS = make_params(
    model=MODEL_ID,
    category=CATEGORY_ID,
    values=[
        make_param_entry(key=PARAM2_ID, id=PARAM2_VALUE_ID_MSKU),
    ]
)

PARAMS_EMPTY = ParamsEntry()


# sequence_number здесь называется id
@pytest.fixture(scope="module")
def offers(request):
    return [
        # MSKU1 с gl-параметрами и два оффера (белый и синий)
        GenlogRow(shard_id=0, id=0, feed_id=FEED_ID, offer_id=OFFER1_WAREMD5, is_fake_msku_offer=False, market_sku=MSKU1,
                  params_entry=OFFER_PARAMS.SerializeToString()),
        GenlogRow(shard_id=0, id=1, feed_id=FEED_ID, offer_id=OFFER2_WAREMD5, is_fake_msku_offer=False, market_sku=MSKU1,
                  params_entry=OFFER2_PARAMS.SerializeToString()),
        GenlogRow(shard_id=0, id=2, feed_id=FEED_ID, offer_id=MSKU1_WAREMD5, is_fake_msku_offer=True, market_sku=MSKU1,
                  params_entry=MSKU_PARAMS.SerializeToString()),

        # MSKU2 c пустыми gl-параметров и его оффер
        GenlogRow(shard_id=0, id=3, feed_id=FEED_ID, offer_id=OFFER3_WAREMD5, is_fake_msku_offer=False, market_sku=MSKU2,
                  params_entry=OFFER_PARAMS.SerializeToString()),
        GenlogRow(shard_id=0, id=4, feed_id=FEED_ID, offer_id=MSKU2_WAREMD5, is_fake_msku_offer=True, market_sku=MSKU2,
                  params_entry=PARAMS_EMPTY.SerializeToString()),

        # мску без офферов
        GenlogRow(shard_id=0, id=5, feed_id=FEED_ID, offer_id=MSKU3_WAREMD5, is_fake_msku_offer=True, market_sku=MSKU3,
                  params_entry=MSKU_PARAMS.SerializeToString()),
        # офферa без мску
        GenlogRow(shard_id=0, id=6, feed_id=FEED_ID, offer_id=OFFER4_WAREMD5, is_fake_msku_offer=False, market_sku=MSKU4,
                  params_entry=OFFER_PARAMS.SerializeToString()),
        GenlogRow(shard_id=0, id=7, feed_id=FEED_ID, offer_id=OFFER5_WAREMD5, is_fake_msku_offer=False, market_sku=None,
                  params_entry=OFFER2_PARAMS.SerializeToString()),
    ]


@pytest.yield_fixture(scope="module")
def workflow(yt_server, offers):
    with MskuReducerTestEnv(yt_server, offers) as env:
        env.execute(mode=MskuReducerMode.PROPAGATE_MSKU_DATA_TO_OFFERS)
        env.verify()
        yield env


GL_PARAMS_EXPECTED_DATA = [
    {'sequence_number': 0, 'params_entry': MSKU_PARAMS},
    {'sequence_number': 1, 'params_entry': MSKU_PARAMS},
    {'sequence_number': 2, 'params_entry': MSKU_PARAMS},

    {'sequence_number': 3, 'params_entry': PARAMS_EMPTY},
    {'sequence_number': 4, 'params_entry': PARAMS_EMPTY},

    {'sequence_number': 5, 'params_entry': MSKU_PARAMS},
    {'sequence_number': 6, 'params_entry': OFFER_PARAMS},
    {'sequence_number': 7, 'params_entry': OFFER2_PARAMS},
]


def test_offers_count(workflow, offers):
    """Проверяем число записей в выходной таблице
    В нее должны попасть все оффера и мску из входной
    """
    assert_that(len(workflow.result_genlog_table), equal_to(len(offers)))


def test_gl_filters(workflow):
    """Проверяем проставление gl-параметров мску в оффера
    """
    for index in xrange(len(GL_PARAMS_EXPECTED_DATA)):
        assert_that(workflow.result_genlog_table[index]['sequence_number'], equal_to(GL_PARAMS_EXPECTED_DATA[index]['sequence_number']))
        params = ParamsEntry()
        params.ParseFromString(workflow.result_genlog_table[index]['params_entry'])
        assert_that(params, equal_to(GL_PARAMS_EXPECTED_DATA[index]['params_entry']))
