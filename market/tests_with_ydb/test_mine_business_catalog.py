# coding: utf-8
import json
import time
import os

from hamcrest import assert_that, has_items, has_length
import pytest

from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.routines.yatf.resources.config_mock import RoutinesConfigMock
from market.idx.datacamp.routines.yatf.test_env import HttpRoutinesTestEnv, SenderToMinerTestEnv
from market.idx.datacamp.routines.yatf.utils import (
    make_basic_offer,
    make_service_offer,
    make_actual_service_offer,
)
from market.idx.datacamp.yatf.matchers.matchers import HasStatus
from market.idx.datacamp.yatf.utils import create_tech_info_dict
# from market.idx.yatf.matchers.env_matchers import IsSerializedJson
from market.idx.yatf.matchers.lb_matchers import HasNoUnreadData
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf


VERDICT_CODE = '49i'


@pytest.fixture(scope='module')
def basic_offers_table_data():
    return [
        make_basic_offer(business_id, shop_sku, merge_data={
            'tech_info': create_tech_info_dict(last_mining=last_mining_ts) if last_mining_ts else {}
        }) for business_id, shop_sku, last_mining_ts in [
            (101, 'T600', None),
            (102, 'T1000', 1000),
        ]
    ]


@pytest.fixture(scope='module')
def service_offers_table_data():
    return [
        make_service_offer(business_id, shop_sku, shop_id, merge_data={
            'tech_info': create_tech_info_dict(last_mining=last_mining_ts) if last_mining_ts else {}
        }) for business_id, shop_sku, shop_id, last_mining_ts in []
    ]


@pytest.fixture(scope='module')
def actual_service_offers_table_data():
    return [
        make_actual_service_offer(business_id, shop_sku, shop_id, warehouse_id)
        for business_id, shop_sku, shop_id, warehouse_id in []
    ]


@pytest.fixture(scope='module')
def partners_table_data():
    return []


@pytest.fixture(scope='module')
def states_table_data():
    return [
        {
            'key': key,
            'state': json.dumps({
                'last_touch_time': time.time()
            })
        } for key in [101, 102]
    ]


@pytest.fixture(scope='module')
def config(yt_server, log_broker_stuff, united_miner_topic):
    cfg = {
        'general': {
            'color': 'white',
        },
        'routines': {
            'enable_mining': True,
            'enable_overload_control': True,
            'force_mining_limit': 10,
            'mining_time': 10**9,
        },
        'miner': {
            'united_topic': united_miner_topic.topic
        },
        'ydb': {
            'database_end_point': os.getenv('YDB_ENDPOINT'),
            'database_path': os.getenv('YDB_DATABASE'),
            'coordination_node_path': '/coordination',
            'publishing_semaphore_name': 'mr_cluster_provider_publishing_semaphore'
        },
    }
    return RoutinesConfigMock(
        yt_server=yt_server,
        log_broker_stuff=log_broker_stuff,
        config=cfg)


@pytest.yield_fixture(scope='module')
def routines_http(yt_server, config, partners_table, states_table, basic_offers_table,
                    service_offers_table, actual_service_offers_table):
    resources = {
        'partners_table': partners_table,
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
        'states_table': states_table,
        'config': config,
    }
    with HttpRoutinesTestEnv(yt_server, **resources) as routines_http_env:
        yield routines_http_env


@pytest.yield_fixture(scope='function')
def sender_to_miner(yt_server, config, united_miner_topic, partners_table,
                    basic_offers_table, service_offers_table, actual_service_offers_table):
    resources = {
        'config': config,
        'partners_table': partners_table,
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
        'united_miner_topic': united_miner_topic,
    }
    with SenderToMinerTestEnv(yt_server, **resources) as miner_env:
        miner_env.verify()
        yield miner_env


def test_mine_business_catalog_sync(sender_to_miner, united_miner_topic, routines_http):
    response = routines_http.post('/mine?business_id={business_id}&sync'.format(business_id=101))
    assert_that(response, HasStatus(200))
    data = united_miner_topic.read(count=1)
    assert_that(data, has_length(1))
    assert_that(data, has_items(*[
        IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [{
                    'basic': {
                        'identifiers': {
                            'business_id': 101,
                            'offer_id': 'T600',
                        },
                    },
                }]
            }]
        }),
    ]))


def test_ze_last_test(sender_to_miner, united_miner_topic, routines_http):
    """
    Проверяем, что в топике больше нет данных, которые мы можем вычитать
    """
    assert_that(united_miner_topic, HasNoUnreadData())
