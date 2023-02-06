# coding: utf-8

import pytest
from datetime import datetime
from hamcrest import assert_that, is_not, has_item

import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.miner.yatf.resources.config import MinerConfig
from market.idx.datacamp.miner.yatf.test_env import MinerTestEnv
from market.idx.datacamp.yatf.matchers.matchers import HasSerializedDatacampMessages
from market.idx.yatf.matchers.protobuf_matchers import IsProtobuf
from market.idx.yatf.resources.datacamp.datacamp_tables import ResaleBusinessIdsTable
from market.pylibrary.proto_utils import message_from_data


OFFERS_VERTICAL = [
    {
        'offer_id': '1',
        'title': 'Обычный товар из обычного магазина',
        'business_id': 1111,
        'shop_id': 1,
        'color': DTC.VERTICAL_GOODS_ADS,
        'is_resale': False
    },
    {
        'offer_id': '2',
        'title': 'Обычный товар из табличного магазина',
        'business_id': 1001,
        'shop_id': 2,
        'color': DTC.VERTICAL_GOODS_ADS,
        'is_resale': True
    },
    {
        'offer_id': '3',
        'title': 'Б/у товар из обычного магазина',
        'business_id': 222,
        'shop_id': 3,
        'color': DTC.VERTICAL_GOODS_ADS,
        'is_resale': True
    },
    {
        'offer_id': '4',
        'title': 'Б/у товар из табличного магазина',
        'business_id': 1002,
        'shop_id': 4,
        'color': DTC.VERTICAL_GOODS_ADS,
        'is_resale': True
    },
]

OFFERS_NOT_VERTICAL = [
    {
        'offer_id': '5',
        'title': 'Б/у товар из табличного магазина но не вертикальный',
        'business_id': 1002,
        'shop_id': 5,
        'color': DTC.WHITE,
        'is_resale': None
    },
]

TS = 1618763231
MSKU_TS = datetime.utcfromtimestamp(TS).strftime('%Y-%m-%dT%H:%M:%SZ')


@pytest.fixture(scope='module')
def resale_business_ids_table_data():
    return [
        {
            'business_id': 1001,
        },
        {
            'business_id': 1002,
        },
    ]


@pytest.fixture(scope='module')
def miner_config(log_broker_stuff, input_topic, output_topic,
                 yt_server, yt_token, resale_business_ids_table_path,
                 offers_blog_topic):
    cfg = MinerConfig()
    cfg.create_datacamp_logger_initializer(log_broker_stuff, offers_blog_topic, enable_trace_log=False)

    reader = cfg.create_lbk_reader_batch_processor(log_broker_stuff, input_topic, united=True)
    writer = cfg.create_lbk_writer_processor(log_broker_stuff, output_topic)
    unpacker = cfg.create_proto_unpacker_processor()
    adapter_converter = cfg.create_offer_adapter_converter()
    resale_goods_enricher = cfg.create_resale_goods_enricher(
        yt_server=yt_server,
        yt_token=yt_token,
        yt_table_path=resale_business_ids_table_path,
        is_enabled=True
    )

    cfg.create_link(reader, unpacker)
    cfg.create_link(unpacker, adapter_converter)
    cfg.create_link(adapter_converter, resale_goods_enricher)
    cfg.create_link(resale_goods_enricher, writer)

    return cfg


@pytest.yield_fixture(scope='module')
def miner(miner_config, input_topic, output_topic, yt_server,
          resale_business_ids_table_path, resale_business_ids_table_data,
          offers_blog_topic):
    resources = {
        'miner_cfg': miner_config,
        'input_topic': input_topic,
        'output_topic': output_topic,
        'offers_blog_topic': offers_blog_topic,
        'resale_business_ids_table': ResaleBusinessIdsTable(
            yt_stuff=yt_server,
            path=resale_business_ids_table_path,
            data=resale_business_ids_table_data
        ),
    }
    with MinerTestEnv(**resources) as miner:
        miner.verify()
        yield miner


def test_resale_goods_enricher_vertical(miner, input_topic, output_topic):
    message = message_from_data({
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': offer['business_id'],
                        'offer_id': offer['offer_id'],
                        'shop_id': offer['shop_id'],
                    },
                    'content': {
                        'partner': {
                            'actual': {
                                'title': {
                                    'value': offer['title'],
                                }
                            }
                        }
                    },
                },
                'service': {
                    offer['shop_id']: {
                        'meta': {'rgb': offer['color']},
                    }}
            } for offer in OFFERS_VERTICAL]
        }]}, DatacampMessage())

    input_topic.write(message.SerializeToString())
    data = output_topic.read(1, wait_timeout=10)

    assert_that(data, HasSerializedDatacampMessages([{
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': offer['business_id'],
                        'offer_id': offer['offer_id'],
                        'shop_id': offer['shop_id'],
                    },
                    'content': {
                        'partner': {
                            'actual': {
                                'is_resale': {
                                    'flag': offer['is_resale']
                                }
                            }
                        },
                    },
                },
            } for offer in OFFERS_VERTICAL]
        }]
    }]))


def test_resale_goods_enricher_not_vertical(miner, input_topic, output_topic):
    message = message_from_data({
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': offer['business_id'],
                        'offer_id': offer['offer_id'],
                        'shop_id': offer['shop_id'],
                    },
                    'content': {
                        'partner': {
                            'actual': {
                                'title': {
                                    'value': offer['title'],
                                }
                            }
                        }
                    },
                },
                'service': {
                    offer['shop_id']: {
                        'meta': {'rgb': offer['color']},
                    }}
            } for offer in OFFERS_NOT_VERTICAL]
        }]}, DatacampMessage())

    input_topic.write(message.SerializeToString())
    data = output_topic.read(1, wait_timeout=10)

    assert_that(data, HasSerializedDatacampMessages([{
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': offer['business_id'],
                        'offer_id': offer['offer_id'],
                        'shop_id': offer['shop_id'],
                    },
                    'content': is_not(has_item(IsProtobuf({
                        'partner': {
                            'actual': {
                                'is_resale': {
                                    'flag': offer['is_resale']
                                }
                            }
                        },
                    }))),
                },
            } for offer in OFFERS_NOT_VERTICAL]
        }]
    }]))
