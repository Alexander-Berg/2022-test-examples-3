# coding: utf-8
from hamcrest import assert_that, has_item
import pytest
import os

from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.proto.common.SchemaType_pb2 import PUSH

from market.idx.datacamp.routines.yatf.resources.config_mock import RoutinesConfigMock
from market.idx.datacamp.routines.yatf.test_env import SenderToMinerTestEnv
from market.idx.datacamp.routines.yatf.utils import (
    make_basic_offer,
    make_service_offer,
    make_actual_service_offer,
    make_united_offer_matcher
)
from market.idx.yatf.matchers.lb_matchers import HasNoUnreadData
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf


EXPECTED_MINED_OFFERS = 1


@pytest.fixture(scope='module')
def basic_offers_table_data():
    return [
        make_basic_offer(business_id, shop_sku, merge_data={
            'identifiers': {
                'feed_id': feed_id
            }
        }) for business_id, shop_sku, feed_id in [
            (1, 'offer-id', None),
        ]
    ]


@pytest.fixture(scope='module')
def service_offers_table_data():
    return [
        make_service_offer(business_id, shop_sku, shop_id, merge_data={
            'identifiers': {
                'feed_id': feed_id
            }
        }) for business_id, shop_sku, shop_id, feed_id in [
            (1, 'offer-id', 1, None),
        ]
    ]


@pytest.fixture(scope='module')
def actual_service_offers_table_data():
    return [
        make_actual_service_offer(business_id, shop_sku, shop_id, warehouse_id, merge_data={
            'identifiers': {
                'feed_id': feed_id
            }
        }) for business_id, shop_sku, shop_id, warehouse_id, feed_id in [
            (1, 'offer-id', 1, 10, None),
            (1, 'offer-id', 1, 33, None),
        ]
    ]


@pytest.fixture(scope='module')
def partners_table_data():
    return [
        {
            'shop_id': 1,
            'status': 'publish',
            'mbi': [{
                'shop_id': 1,
                'business_id': 1,
                'datafeed_id': 123,
                'is_push_partner': True,
                'warehouse_id': 10,
                'is_enabled': True
            }, {
                'shop_id': 1,
                'business_id': 1,
                'datafeed_id': 124,
                'is_push_partner': True,
                'warehouse_id': 33,
                'is_enabled': True
            }],
            'partner_additional_info': {
                'partner_schema': PUSH,
            }
        },
    ]


@pytest.fixture(scope='module')
def config(yt_server, log_broker_stuff, united_miner_topic):
    cfg = {
        'general': {
            'agile_basic_select_rows_limit_enabled': True,
            'basic_select_rows_limit': 5,
            'united_select_rows_limit': 5,
            'shops_amount_to_enable_agile_select_limit': 1,
            'agile_basic_select_rows_limit_minimum': 1
        },
        'miner': {
            'united_topic': united_miner_topic.topic,
            'united_mining_mode_permille': 1000,
        },
        'routines': {
            'enable_mining': True,
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
def sender_to_miner(yt_server, config, partners_table, united_miner_topic,
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


@pytest.fixture(scope='module')
def united_miner_topic_data(sender_to_miner, united_miner_topic):
    united_data = united_miner_topic.read(count=EXPECTED_MINED_OFFERS)
    # проверяем, что в топике больше нет данных, которые мы можем вычитать
    assert_that(united_miner_topic, HasNoUnreadData())

    return united_data


def test_mine_in_united_format(united_miner_topic_data):
    """Проверяем, что офферы отправляются на майнинг в united формате
    """
    assert_that(united_miner_topic_data, has_item(
        IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [
                    make_united_offer_matcher(1, 'offer-id', 1, 10, with_actual=True),
                ]
            }, {
                'offer': [
                    make_united_offer_matcher(1, 'offer-id', 1, 33, with_actual=True),
                ]
            }]
        })
    ))
