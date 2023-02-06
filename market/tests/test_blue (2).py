# coding: utf-8

import pytest
from hamcrest import assert_that

from market.idx.datacamp.miner.yatf.resources.config import MinerConfig
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import BLUE
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.yatf.matchers.lb_matchers import HasNoUnreadData
from market.pylibrary.proto_utils import message_from_data


DATACAMP_MESSAGES = [
    {
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': 1,
                        'offer_id': 'o1',
                    },
                },
                'service': {
                    10: {
                        'identifiers': {
                            'business_id': 1,
                            'offer_id': 'o1',
                            'shop_id': 10,
                            'warehouse_id': 145,
                        },
                        'meta': {
                            'rgb': BLUE
                        }
                    }
                }
            }]
        }]
    },
    {
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': 1,
                        'offer_id': 'o2',
                    },
                },
                'service': {
                    10: {
                        'identifiers': {
                            'business_id': 1,
                            'offer_id': 'o2',
                            'shop_id': 10,
                            'warehouse_id': 0,
                        },
                        'meta': {
                            'rgb': BLUE
                        }
                    }
                }
            }]
        }]
    }
]


@pytest.fixture(scope='module')
def miner_config(yt_server, yt_token, log_broker_stuff, input_topic, output_topic):
    cfg = MinerConfig()
    cfg.create_miner_initializer(
        yt_server=yt_server,
        token=yt_token.path,
    )

    reader = cfg.create_lbk_reader_batch_processor(log_broker_stuff, input_topic, True)
    writer = cfg.create_lbk_writer_processor(log_broker_stuff, output_topic)
    unpacker = cfg.create_proto_unpacker_processor()
    adapter_converter = cfg.create_offer_adapter_converter()

    cfg.create_link(reader, unpacker)
    cfg.create_link(unpacker, adapter_converter)
    cfg.create_link(adapter_converter, writer)

    return cfg


def write_read_offer_lbk(input_topic, output_topic):
    for m in DATACAMP_MESSAGES:
        input_topic.write(message_from_data(m, DatacampMessage()).SerializeToString())

    # Синий майнер ничего не делает. Кандидат на удаление MARKETINDEXER-42271
    assert_that(output_topic, HasNoUnreadData())
