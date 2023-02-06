# coding: utf-8
import json
import time
import os
from hamcrest import assert_that, has_items, anything
import pytest

from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.proto.common.SchemaType_pb2 import PUSH
from market.idx.datacamp.proto.offer import DataCampOffer_pb2 as DTC

from market.idx.datacamp.routines.yatf.resources.config_mock import RoutinesConfigMock
from market.idx.datacamp.routines.yatf.test_env import SenderToMinerTestEnv
from market.idx.yatf.matchers.lb_matchers import HasNoUnreadData
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf, IsProtobufMap
from market.idx.yatf.utils.utils import create_pb_timestamp

from market.pylibrary.proto_utils import message_from_data


def make_basic_offer(business_id, shop_sku, last_mining_ts=None):
    if last_mining_ts is None:
        last_mining_ts = int(time.time())

    return message_from_data({
        'identifiers': {
            'business_id': business_id,
            'offer_id': shop_sku
        },
        'tech_info': {
            'last_mining': {
                'meta': {
                    'timestamp': create_pb_timestamp(last_mining_ts).ToJsonString()
                }
            }
        }
    }, DTC.Offer())


def make_service_offer(business_id, shop_sku, shop_id):
    return message_from_data({
        'identifiers': {
            'business_id': business_id,
            'offer_id': shop_sku,
            'shop_id': shop_id,
            'warehouse_id': 0,
        }
    }, DTC.Offer())


def make_actual_service_offer(business_id, shop_sku, shop_id, warehouse_id):
    return message_from_data({
        'identifiers': {
            'business_id': business_id,
            'offer_id': shop_sku,
            'shop_id': shop_id,
            'warehouse_id': warehouse_id,
        }
    }, DTC.Offer())


@pytest.fixture(scope='module')
def basic_offers_table_data():
    return [
        make_basic_offer(business_id, shop_sku, last_mining_ts)
        for business_id, shop_sku, last_mining_ts in [
            # offer that was mined recently -- do not send
            (1, 'T1000', None),
            # offer that was mined long time ago -- send
            (1, 'T2000', 0),
            # forced, recent -- send
            (2, 'T3000', None),
            # forced, not recent -- send
            (2, 'T4000', 0),
        ]
    ]


@pytest.fixture(scope='module')
def service_offers_table_data():
    return [
        make_service_offer(business_id, shop_sku, shop_id)
        for business_id, shop_sku, shop_id in [
            (1, 'T1000', 1),
            (1, 'T2000', 1),
            (2, 'T3000', 2),
            (2, 'T4000', 2),
        ]
    ]


@pytest.fixture(scope='module')
def actual_service_offers_table_data():
    return [
        make_actual_service_offer(business_id, shop_sku, shop_id, warehouse_id)
        for business_id, shop_sku, shop_id, warehouse_id in [
            (1, 'T1000', 1, 10),
            (1, 'T2000', 1, 10),
            (2, 'T3000', 2, 10),
            (2, 'T4000', 2, 10),
        ]
    ]


@pytest.fixture(scope='module')
def partners_table_data():
    return [
        {
            'shop_id': 1,
            'status': 'publish',
            'mbi': {'shop_id': 1, 'business_id': 1, 'is_push_partner': True, 'warehouse_id': 10, 'is_enabled': True},
            'partner_additional_info': {
                'partner_schema': PUSH,
            }
        },
        {
            'shop_id': 2,
            'status': 'publish',
            'mbi': {'shop_id': 2, 'business_id': 2, 'is_push_partner': True, 'warehouse_id': 10, 'is_enabled': True},
            'partner_additional_info': {
                'partner_schema': PUSH,
            }
        },
    ]


@pytest.fixture(scope='module')
def states_table_data():
    return [{
        'key': 2,
        'state': json.dumps({'force': True}),
    }]


@pytest.fixture(scope='module')
def config(yt_server, log_broker_stuff, united_miner_topic):
    cfg = {
        'miner': {'united_topic': united_miner_topic.topic},
        'routines': {
            'enable_mining': True,
            'filter_offers_by_search_tables': True
        },
        'ydb': {
            'database_end_point': os.getenv('YDB_ENDPOINT'),
            'database_path': os.getenv('YDB_DATABASE'),
            'coordination_node_path': '/coordination',
            'publishing_semaphore_name': 'mr_cluster_provider_publishing_semaphore'
        },
    }
    return RoutinesConfigMock(yt_server=yt_server, log_broker_stuff=log_broker_stuff, config=cfg)


@pytest.yield_fixture(scope='module')
def sender_to_miner(
    yt_server,
    config,
    partners_table,
    business_status_table,
    united_miner_topic,
    basic_offers_table,
    service_offers_table,
    actual_service_offers_table,
    basic_offers_search_table,
    service_offers_search_table,
    actual_service_offers_search_table,
    states_table,
):
    resources = {
        'config': config,
        'partners_table': partners_table,
        'business_status_table': business_status_table,
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
        'basic_search_offers_table': basic_offers_search_table,
        'service_search_offers_table': service_offers_search_table,
        'actual_service_search_offers_table': actual_service_offers_search_table,
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
    Проверяем, что недавно помайненные офферы не отправляются на перемайнинг, за исключением случая с forced state
    """
    data = united_miner_topic.read(count=2)

    assert_that(
        data,
        has_items(
            IsSerializedProtobuf(
                DatacampMessage, {'united_offers': [{'offer': [make_united_offer_matcher(1, 'T2000', 1, 10)]}]}
            ),
            IsSerializedProtobuf(
                DatacampMessage,
                {
                    'united_offers': [
                        {
                            'offer': [
                                make_united_offer_matcher(2, 'T3000', 2, 10),
                                make_united_offer_matcher(2, 'T4000', 2, 10),
                            ]
                        }
                    ]
                },
            ),
        ),
    )

    assert_that(united_miner_topic, HasNoUnreadData())
