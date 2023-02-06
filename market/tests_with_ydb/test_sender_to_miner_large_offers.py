# coding: utf-8
import time
import os
from hamcrest import assert_that, has_items, anything
import pytest

from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.proto.common.SchemaType_pb2 import PUSH

from market.idx.datacamp.routines.yatf.resources.config_mock import RoutinesConfigMock
from market.idx.datacamp.routines.yatf.test_env import SenderToMinerTestEnv
from market.idx.datacamp.routines.yatf.utils import (
    make_basic_offer,
    make_service_offer,
    make_actual_service_offer
)
from market.idx.yatf.matchers.lb_matchers import HasNoUnreadData
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf, IsProtobufMap

from market.idx.yatf.utils.utils import create_pb_timestamp


BUSINESS_ID = 1
OFFER_ID = 'o1'
NUM_SERVICE_OFFERS = 4
SELECT_ROWS_LIMIT = 3


def make_basic_offer_(business_id, shop_sku, last_mining_ts=None):
    if last_mining_ts is None:
        last_mining_ts = int(time.time())

    return make_basic_offer(business_id, shop_sku, {
        'tech_info': {
            'last_mining': {
                'meta': {
                    'timestamp': create_pb_timestamp(last_mining_ts).ToJsonString()
                }
            }
        }
    })


@pytest.fixture(scope='module')
def basic_offers_table_data():
    return [
        make_basic_offer_(BUSINESS_ID, OFFER_ID, 0)
    ]


@pytest.fixture(scope='module')
def service_offers_table_data():
    return [
        make_service_offer(BUSINESS_ID, OFFER_ID, i+1) for i in range(NUM_SERVICE_OFFERS)
    ]


@pytest.fixture(scope='module')
def actual_service_offers_table_data():
    return [
        make_actual_service_offer(BUSINESS_ID, OFFER_ID, i+1, 0) for i in range(NUM_SERVICE_OFFERS)
    ]


@pytest.fixture(scope='module')
def partners_table_data():
    return [
        {
            'shop_id': i + 1,
            'status': 'publish',
            'mbi': {'shop_id': i + 1, 'business_id': BUSINESS_ID, 'is_push_partner': True, 'warehouse_id': 0, 'is_enabled': True},
            'partner_additional_info': {
                'partner_schema': PUSH,
            }
        } for i in range(NUM_SERVICE_OFFERS)
    ]


@pytest.fixture(scope='module')
def config(yt_server, log_broker_stuff, united_miner_topic):
    cfg = {
        'miner': {'united_topic': united_miner_topic.topic},
        'routines': {'enable_mining': True},
        'ydb': {
            'database_end_point': os.getenv('YDB_ENDPOINT'),
            'database_path': os.getenv('YDB_DATABASE'),
            'coordination_node_path': '/coordination',
            'publishing_semaphore_name': 'mr_cluster_provider_publishing_semaphore'
        },
        'general': {
            'united_select_rows_limit': SELECT_ROWS_LIMIT
        }
    }
    return RoutinesConfigMock(yt_server=yt_server, log_broker_stuff=log_broker_stuff, config=cfg)


@pytest.yield_fixture(scope='module')
def sender_to_miner(
    yt_server,
    config,
    partners_table,
    united_miner_topic,
    basic_offers_table,
    service_offers_table,
    actual_service_offers_table,
    states_table,
):
    resources = {
        'config': config,
        'partners_table': partners_table,
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
        'states_table': states_table,
        'united_miner_topic': united_miner_topic,
    }
    with SenderToMinerTestEnv(yt_server, **resources) as miner_env:
        miner_env.verify()
        yield miner_env


def make_united_offer_matcher(business_id, offer_id, shop_id, warehouse_id):
    identifiers = {
        'business_id': business_id,
        'shop_id': shop_id,
        'offer_id': offer_id,
        'warehouse_id': warehouse_id,
    }

    return {
        'basic': {'identifiers': {'business_id': business_id, 'offer_id': offer_id}, 'tech_info': anything()},
        'service': IsProtobufMap({shop_id: {'identifiers': identifiers}}),
    }


def test_sender(sender_to_miner, united_miner_topic):
    """
    Проверяем что офферы с количество сервисных частей более лимита на вычитку не завешивают сендер
    """
    data = united_miner_topic.read(count=1, wait_timeout=10)

    assert_that(
        data,
        has_items(
            IsSerializedProtobuf(
                DatacampMessage,
                {
                    'united_offers': [
                        {
                            'offer': [
                                make_united_offer_matcher(BUSINESS_ID, OFFER_ID, i + 1, 0)
                                for i in range(NUM_SERVICE_OFFERS)
                            ]
                        }
                    ]
                },
            ),
        ),
    )

    assert_that(united_miner_topic, HasNoUnreadData())
