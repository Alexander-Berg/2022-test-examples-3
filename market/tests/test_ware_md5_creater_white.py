# coding: utf-8

import pytest
from hamcrest import assert_that

from market.idx.datacamp.miner.yatf.resources.config import MinerConfig
from market.idx.datacamp.miner.yatf.test_env import MinerTestEnv
from market.idx.yatf.matchers.protobuf_matchers import IsProtobufMap
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.yatf.matchers.matchers import HasSerializedDatacampMessages
from market.pylibrary.proto_utils import message_from_data

WHITE_WARE_MD5_V2_PERMILLE = 1000
BUSINESS_ID = 1
BLUE_SHOP_ID = 10264170
BLUE_FEED_ID = 200396945
WHITE_SHOP_ID = 10264169
WHITE_FEED_ID = 200396944
DATACAMP_MESSAGES = [{
    'united_offers': [{
        'offer': [{
            'basic': {
                'identifiers': {
                    'business_id': BUSINESS_ID,
                    'offer_id': offer_id,
                },
                'content': {
                    'binding': {
                        'partner': {
                            'market_sku_id': 100126173307,
                        }
                    }
                }
            },
            'service': {
                WHITE_SHOP_ID: {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': offer_id,
                        'shop_id': WHITE_SHOP_ID,
                        'feed_id': WHITE_FEED_ID,
                        'warehouse_id': 0,
                        'extra': {
                            'ware_md5': ware_md5,
                        } if ware_md5 else None
                    },
                    'meta': {
                        'rgb': DTC.WHITE
                    },
                    'status': {
                        'united_catalog': {
                            'flag': True,
                        }
                    },
                },
            }
        } for offer_id, ware_md5 in (
            ('offer.without.waremd5', None),
            ('offer.with.waremd5', 'LTb0b0kAB9sXXYgQJd6_SQ')
        )]
    }]
}]


@pytest.fixture(scope='module')
def miner_config(log_broker_stuff, input_topic, output_topic):
    cfg = MinerConfig()

    reader = cfg.create_lbk_reader_batch_processor(log_broker_stuff, input_topic, united=True)
    writer = cfg.create_lbk_writer_processor(log_broker_stuff, output_topic)
    unpacker = cfg.create_proto_unpacker_processor()
    adapter_converter = cfg.create_offer_adapter_converter()
    ware_md5_creator = cfg.create_ware_md5_white_creator(WHITE_WARE_MD5_V2_PERMILLE)
    blue_ware_md5_creator = cfg.create_ware_md5_creator()
    foodtech_ware_md5_creator = cfg.create_ware_md5_foodtech_creator()

    cfg.create_link(reader, unpacker)
    cfg.create_link(unpacker, adapter_converter)
    cfg.create_link(adapter_converter, ware_md5_creator)
    cfg.create_link(ware_md5_creator, blue_ware_md5_creator)
    cfg.create_link(blue_ware_md5_creator, foodtech_ware_md5_creator)
    cfg.create_link(foodtech_ware_md5_creator, writer)

    return cfg


@pytest.yield_fixture(scope='module')
def miner(miner_config, input_topic, output_topic):

    resources = {
        'miner_cfg': miner_config,
        'input_topic': input_topic,
        'output_topic': output_topic,
    }
    with MinerTestEnv(**resources) as miner:
        miner.verify()
        yield miner


@pytest.yield_fixture(scope='module')
def workflow(miner, input_topic, output_topic):
    for datacamp_message in DATACAMP_MESSAGES:
        input_topic.write(message_from_data(datacamp_message, DatacampMessage()).SerializeToString())
    yield output_topic.read(count=len(DATACAMP_MESSAGES), wait_timeout=5)


def test_ware_md5_init(workflow):
    """ Установка ware_md5, если у оффера его нет
    """
    offer_id = 'offer.without.waremd5'
    assert_that(workflow, HasSerializedDatacampMessages([{
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': offer_id,
                        'extra': None,
                    },
                },
                'service': IsProtobufMap({
                    WHITE_SHOP_ID: {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': offer_id,
                            'shop_id': WHITE_SHOP_ID,
                            'extra': {
                                'ware_md5': 'KQrPoNtv-PxFV6tugzwqgQ',
                            }
                        },
                    },
                })
            }]
        }]
    }]))


def test_ware_md5_update(workflow):
    """ Установка ware_md5, если у оффера он уже стоит: меняется
    """
    offer_id = 'offer.with.waremd5'
    assert_that(workflow, HasSerializedDatacampMessages([{
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': offer_id,
                        'extra': None,
                    },
                },
                'service': IsProtobufMap({
                    WHITE_SHOP_ID: {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': offer_id,
                            'shop_id': WHITE_SHOP_ID,
                            'extra': {
                                'ware_md5': '6cHAaGtKzzYAEVfS8bTORw',
                            }
                        },
                    },
                })
            }]
        }]
    }]))
