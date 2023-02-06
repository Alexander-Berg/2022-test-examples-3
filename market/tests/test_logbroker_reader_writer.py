# coding: utf-8

import pytest
from hamcrest import assert_that

from market.idx.datacamp.miner.yatf.resources.config import MinerConfig
from market.idx.datacamp.miner.yatf.test_env import MinerTestEnv
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import MARKET_IDX
from market.idx.datacamp.yatf.matchers.matchers import HasSerializedDatacampMessages
from market.idx.yatf.matchers.protobuf_matchers import IsProtobufMap
from market.pylibrary.proto_utils import message_from_data


BUSINESS_ID = 1
SHOP_ID = 2

DATACAMP_MESSAGE = {
    'united_offers': [{
        'offer': [{
            'basic': {
                'identifiers': {
                    'business_id': BUSINESS_ID,
                    'offer_id': 'o1',
                },
            },
            'service': {
                SHOP_ID: {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': 'o1',
                        'shop_id': SHOP_ID,
                    },
                    'status': {
                        'united_catalog': {
                            'flag': True
                        },
                        'disabled': [{
                            'flag': False,
                            'meta': {
                                'source': MARKET_IDX
                            }
                        }],
                        'version': {
                            'original_partner_data_version': {
                                'counter': 10
                            }
                        }
                    }
                }
            }
        }]
    }]
}


@pytest.fixture(scope='function')
def miner_config(log_broker_stuff, input_topic, output_topic):
    cfg = MinerConfig()

    reader = cfg.create_lbk_reader_batch_processor(log_broker_stuff, input_topic, united=True)
    writer = cfg.create_lbk_writer_processor(log_broker_stuff, output_topic)
    unpacker = cfg.create_proto_unpacker_processor()

    cfg.create_link(reader, unpacker)
    cfg.create_link(unpacker, writer)

    return cfg


@pytest.yield_fixture(scope='function')
def miner(miner_config, input_topic, output_topic):
    resources = {
        'miner_cfg': miner_config,
        'input_topic': input_topic,
        'output_topic': output_topic,
    }
    with MinerTestEnv(**resources) as miner:
        miner.verify()
        yield miner


def test_simple_miner(miner, input_topic, output_topic):
    """
    Проверяем, что простой майнер, как и хороший backend разработчик,
    умеет перекладывать протобуфы из одного топика в другой,
    а также правильно заполняет tech_info
    """
    input_topic.write(message_from_data(DATACAMP_MESSAGE, DatacampMessage()).SerializeToString())
    data = output_topic.read(1)

    assert_that(data, HasSerializedDatacampMessages([{
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': 'o1',
                    },
                },
                'service': IsProtobufMap({
                    SHOP_ID: {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': 'o1',
                            'shop_id': SHOP_ID,
                        },
                        'status': None,
                        'tech_info': {
                            'last_mining': {
                                'original_partner_data_version': {
                                    'counter': 10
                                }
                            }
                        }
                    }
                })
            }]
        }]
    }]))
