# coding: utf-8

import pytest
from hamcrest import assert_that

import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.miner.yatf.resources.config import MinerConfig
from market.idx.datacamp.miner.yatf.test_env import MinerTestEnv
from market.idx.datacamp.yatf.utils import dict2tskv

from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf, IsProtobufMap
from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampPartnersTable

from market.pylibrary.proto_utils import message_from_data

BUSINESS_ID = 1
SHOP_ID = 2
FEED_ID = 3
OFFER_ID = "offer"

PARTNER_DATA = []

PARTNER_DATA_UPDATE = [{
    'shop_id': SHOP_ID,
    'mbi': dict2tskv({
        'shop_id': SHOP_ID,
        'shopname': 'best shop',
        'business_id': BUSINESS_ID,
        'datafeed_id': FEED_ID,
        'is_site_market': 'true'
    }),
    'status': 'publish'
}]

DATACAMP_MESSAGES = [{
    'united_offers': [{
        'offer': [{
            'basic': {
                'identifiers': {
                    'business_id': BUSINESS_ID,
                    'offer_id': OFFER_ID,
                },
                'meta': {
                    'scope': DTC.BASIC,
                },
            },
            'service': {
                SHOP_ID: {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': OFFER_ID,
                        'shop_id': SHOP_ID,
                        'feed_id': FEED_ID,
                        'warehouse_id': 0,
                    },
                    'meta': {
                        'rgb': DTC.WHITE,
                        'scope': DTC.SERVICE,
                    },
                },
            }
        }]
    }]
}]


@pytest.fixture(scope='module')
def partners_table(yt_server, partner_info_table_path):
    return DataCampPartnersTable(
        yt_server,
        partner_info_table_path,
        data=PARTNER_DATA,
    )


@pytest.fixture(scope='module')
def config(yt_server, yt_token, log_broker_stuff, input_topic, output_topic, partner_info_table_path):
    cfg = MinerConfig()

    cfg.create_miner_initializer(
        yt_server=yt_server,
        token=yt_token.path,
        partners_table_path=partner_info_table_path,
        enable_dynamic_cache=True
    )

    reader = cfg.create_lbk_reader_batch_processor(log_broker_stuff, input_topic, True)
    writer = cfg.create_lbk_writer_processor(log_broker_stuff, output_topic)
    unpacker = cfg.create_proto_unpacker_processor()
    adapter_converter = cfg.create_offer_adapter_converter()
    shopsdat_enricher = cfg.create_shopsdat_enricher(color='white')

    cfg.create_link(reader, unpacker)
    cfg.create_link(unpacker, adapter_converter)
    cfg.create_link(adapter_converter, shopsdat_enricher)
    cfg.create_link(shopsdat_enricher, writer)

    return cfg


@pytest.yield_fixture(scope='module')
def miner(yt_server, config, input_topic, output_topic, partners_table):
    resources = {
        'miner_cfg': config,
        'input_topic': input_topic,
        'output_topic': output_topic,
        'partners_table': partners_table
    }
    with MinerTestEnv(**resources) as miner:
        miner.verify()
        yield miner


def insert(input_topic):
    for message in DATACAMP_MESSAGES:
        for offers in message['united_offers']:
            input_topic.write(message_from_data(message, DatacampMessage()).SerializeToString())


def test_shopsdat_from_table(miner, partners_table, input_topic, output_topic):
    """Проверяем, что, если записи о партнере не оказывается в кеше шопсдаты,
        он идет за ней в таблицу партнеров.
    """
    insert(input_topic)
    data = output_topic.read(count=1)

    # В кеше шопсдаты нет записи о партнере, так как ее изначально нет в таблице.
    assert_that(data[0], IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': OFFER_ID,
                    },
                    'partner_info': None,
                },
                'service': IsProtobufMap({
                    SHOP_ID: {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': OFFER_ID,
                            'shop_id': SHOP_ID,
                        },
                        'partner_info': None,
                    },
                })
            }]
        }]
    }))

    # Кладем запись о партнере в таблицу.
    partners_table.update(PARTNER_DATA_UPDATE)

    insert(input_topic)
    data = output_topic.read(count=1)

    # Запись о партнере взялась из таблицы, а не из кеша.
    assert_that(data[0], IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': OFFER_ID,
                    },
                    'partner_info': None,
                },
                'service': IsProtobufMap({
                    SHOP_ID: {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': OFFER_ID,
                            'shop_id': SHOP_ID,
                        },
                        'partner_info': {
                            'shop_name': 'best shop',
                        }
                    },
                })
            }]
        }]
    }))
