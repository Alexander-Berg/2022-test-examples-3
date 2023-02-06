# coding: utf-8
import json
import time
import os

from hamcrest import assert_that, has_items
import pytest

from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.routines.yatf.resources.config_mock import RoutinesConfigMock
from market.idx.datacamp.routines.yatf.test_env import SenderToMinerTestEnv
from market.idx.datacamp.routines.yatf.utils import (
    make_basic_offer,
    make_service_offer,
    make_actual_service_offer,
    make_united_offer_matcher
)
from market.idx.datacamp.yatf.utils import create_tech_info_dict
from market.idx.yatf.matchers.lb_matchers import HasNoUnreadData
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf


NOW = time.time()
MINING_TIME = 30 * 60
BLUE_MINING_TIME = 10 * 60
MINING_SHIFT_TIME = 5 * 60


@pytest.fixture(scope='module')
def basic_offers_table_data():
    return [
        make_basic_offer(business_id, shop_sku, {
            'tech_info': create_tech_info_dict(last_mining=1)
        }) for business_id, shop_sku in [
            (10, 'T600'),
            (20, 'T700'),
            (30, 'T800'),
            (40, 'T900'),
        ]
    ]


@pytest.fixture(scope='module')
def service_offers_table_data():
    return [
        make_service_offer(business_id, shop_sku, shop_id)
        for business_id, shop_sku, shop_id in [
            (10, 'T600', 1),
            (20, 'T700', 2),
            (30, 'T800', 3),
            (40, 'T900', 4),
        ]
    ]


@pytest.fixture(scope='module')
def actual_service_offers_table_data():
    return [
        make_actual_service_offer(business_id, shop_sku, shop_id, warehouse_id)
        for business_id, shop_sku, shop_id, warehouse_id in [
            (10, 'T600', 1, 145),
            (20, 'T700', 2, 145),
            (30, 'T800', 3, 0),
            (40, 'T900', 4, 0),
        ]
    ]


@pytest.fixture(scope='module')
def partners_table_data():
    return [
        {
            'shop_id': shop_id,
            'status': 'publish',
            'mbi': {
                'shop_id': shop_id,
                'business_id': business_id,
                'datafeed_id': shop_id * 100,
                'is_push_partner': True,
                'is_site_market': 'true',
                'blue_status': 'REAL' if business_id <= 20 else 'NO',
                'warehouse_id': warehouse_id,
                'is_enabled': True
            }
        }
        for business_id, shop_id, warehouse_id in [
            (10, 1, 145),
            (20, 2, 145),
            (30, 3, 0),
            (40, 4, 0)
        ]
    ]


@pytest.fixture(scope='module')
def states_table_data():
    return [{
        # синий бизнес, не должен майниться
        'key': 10,
        'state': json.dumps({
            'last_touch_time': NOW - 5 * 60
        })
    }, {
        # синий бизнес, должен помайниться
        'key': 20,
        'state': json.dumps({
            'last_touch_time': NOW - 20 * 60
        })
    }, {
        # белый бизнес, не должен майниться
        'key': 30,
        'state': json.dumps({
            'last_touch_time': NOW - 20 * 60
        })
    }, {
        # белый бизнес, должен помайниться
        'key': 40,
        'state': json.dumps({
            'last_touch_time': NOW - 35 * 60
        })
    }]


@pytest.fixture(scope='module')
def config(yt_server, log_broker_stuff, united_miner_topic):
    cfg = {
        'general': {
            'color': 'white',
        },
        'routines': {
            'enable_mining': True,
            'mining_time': MINING_TIME,
            'blue_mining_time': BLUE_MINING_TIME,
            'mining_random_shift_time': MINING_SHIFT_TIME,
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
def sender_to_miner(yt_server, config, united_miner_topic, partners_table, states_table,
                    basic_offers_table, service_offers_table, actual_service_offers_table):
    resources = {
        'config': config,
        'partners_table': partners_table,
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
        'united_miner_topic': united_miner_topic,
        'states_table': states_table,
    }
    with SenderToMinerTestEnv(yt_server, **resources) as miner_env:
        miner_env.verify()
        yield miner_env


def test_mine_diff_colors(sender_to_miner, united_miner_topic):
    """
    Проверяем, что белые и синие оффера майнятся с разными интервалами
    (учитываем, что переобогащение происходит по бизнесам)
    """
    data = united_miner_topic.read(count=2)

    assert_that(data, has_items(*[
        IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [
                    make_united_offer_matcher(20, 'T700', 2, 145),
                ]
            }]
        }),
        IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [
                    make_united_offer_matcher(40, 'T900', 4, 0),
                ]
            }]
        }),
    ]))

    # проверяем, что в топике больше нет данных, которые мы можем вычитать
    assert_that(united_miner_topic, HasNoUnreadData())
