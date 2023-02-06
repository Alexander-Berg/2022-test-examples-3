# coding: utf-8
import os

import yt.wrapper as yt
from hamcrest import assert_that, has_items
import pytest

from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.routines.yatf.resources.config_mock import RoutinesConfigMock
from market.idx.datacamp.routines.yatf.test_env import HttpRoutinesTestEnv
from market.idx.datacamp.routines.yatf.utils import (
    make_basic_offer,
    make_service_offer,
    make_actual_service_offer,
    make_united_offer_matcher
)
from market.idx.datacamp.yatf.matchers.matchers import HasStatus
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf
from market.idx.datacamp.routines.yatf.test_env import SenderToMinerJobTestEnv

from market.idx.yatf.resources.yt_table_resource import YtTableResource


@pytest.fixture(scope='module')
def basic_offers_table_data():
    return [
        make_basic_offer(business_id, shop_sku)
        for business_id, shop_sku in [
            (101, 'T600'),
            (102, 'T1000'),
            (103, 'T1000'),
        ]
    ]


@pytest.fixture(scope='module')
def service_offers_table_data():
    return [
        make_service_offer(business_id, shop_sku, shop_id)
        for business_id, shop_sku, shop_id in [
            (101, 'T600', 1),
            (102, 'T1000', 2),
            (103, 'T1000', 3),
        ]
    ]


@pytest.fixture(scope='module')
def actual_service_offers_table_data():
    return [
        make_actual_service_offer(business_id, shop_sku, shop_id, warehouse_id)
        for business_id, shop_sku, shop_id, warehouse_id in [
            (101, 'T600', 1, 10),
            (102, 'T1000', 2, 10),
            (103, 'T1000', 3, 10),
        ]
    ]


class IdsTable(YtTableResource):
    def __init__(self, yt_server, path, data=None):
        super(IdsTable, self).__init__(
            yt_stuff=yt_server,
            path=path,
            attributes={'schema': [
                dict(name='business_id', type='uint32', sort_order='ascending'),
                dict(name='shop_sku', type='string', sort_order='ascending'),
                dict(name='shop_id', type='uint32', sort_order='ascending'),
                dict(name='warehouse_id', type='uint32', sort_order='ascending'),
            ]},
            data=data
        )


@pytest.fixture(scope='module')
def config(yt_server, log_broker_stuff, united_miner_topic):
    cfg = {
        'general': {
            'color': 'white',
        },
        'routines': {
            'enable_force_mining_job': True,
            'auto_disable_regular_mining': True
        },
        'miner': {
            'united_topic': united_miner_topic.topic
        },
        'ydb': {
            'database_end_point': os.getenv('YDB_ENDPOINT'),
            'database_path': os.getenv('YDB_DATABASE'),
            'mining_coordination_node_path': 'coordination',
        },
    }
    return RoutinesConfigMock(
        yt_server=yt_server,
        log_broker_stuff=log_broker_stuff,
        config=cfg)


@pytest.fixture(scope='module')
def ids_table_path(config):
    return yt.ypath_join(config.yt_home, 'ids_table')


@pytest.fixture(scope='module')
def ids_table(yt_server, ids_table_path):
    return IdsTable(yt_server, ids_table_path, data=[{
        'business_id': 101,
        'shop_sku': 'T600',
        'shop_id': 1,
        'warehouse_id': 10
    }, {
        'business_id': 102,
        'shop_sku': 'T1000',
        'shop_id': 2,
        'warehouse_id': 10
    }, {
        'business_id': 103,
        'shop_sku': 'T1000',
        'shop_id': 3,
        'warehouse_id': 10
    }])


@pytest.yield_fixture(scope='module')
def routines_http(yt_server, config, partners_table, states_table, basic_offers_table,
                  service_offers_table, actual_service_offers_table, united_miner_topic, ids_table):
    resources = {
        'partners_table': partners_table,
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
        'states_table': states_table,
        'config': config,
        'united_miner_topic': united_miner_topic,
        'ids_table': ids_table
    }
    with HttpRoutinesTestEnv(yt_server, **resources) as routines_http_env:
        yield routines_http_env


@pytest.yield_fixture(scope='function')
def sender_to_miner(yt_server, config, united_miner_topic,
                    basic_offers_table, service_offers_table, actual_service_offers_table):
    resources = {
        'config': config,
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
        'united_miner_topic': united_miner_topic,
    }
    with SenderToMinerJobTestEnv(**resources) as miner_env:
        miner_env.verify()
        yield miner_env


@pytest.fixture(scope='function')
def mine_ids_table(routines_http, ids_table, ids_table_path, yt_server, sender_to_miner):
    response = routines_http.post('/mine_parallel?ids_table={}&yt_cluster={}&num_shards=3'.format(ids_table_path, yt_server.get_server()))
    assert_that(response, HasStatus(200))


def test_force_mine_ids_table(united_miner_topic, mine_ids_table):
    data = united_miner_topic.read(count=3)
    assert_that(data, has_items(*[
        IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [
                    make_united_offer_matcher(101, 'T600', 1, 10),
                ]
            }]
        }),
        IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [
                    make_united_offer_matcher(102, 'T1000', 2, 10),
                ]
            }]
        }),
        IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [
                    make_united_offer_matcher(103, 'T1000', 3, 10),
                ]
            }]
        }),
    ]))
