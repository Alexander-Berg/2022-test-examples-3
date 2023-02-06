# coding: utf-8

import pytest
import time

from datetime import datetime
from hamcrest import assert_that, has_items, greater_than

from market.idx.datacamp.miner.yatf.resources.config import MinerConfig
from market.idx.datacamp.miner.yatf.test_env import MinerTestEnv
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import WHITE
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf
from market.pylibrary.proto_utils import message_from_data

NOW_TIME = int(time.time())
OLD_TIME = NOW_TIME - 45 * 60


def make_offer(offer_id, last_send):
    offer = {
        'basic': {
            'identifiers': {
                'business_id': 1,
                'offer_id': offer_id,
            },
        },
        'service': {
            1: {
                'identifiers': {
                    'business_id': 1,
                    'offer_id': offer_id,
                    'shop_id': 1,
                    'warehouse_id': 0,
                },
                'meta': {'rgb': WHITE},
            },
        },
    }

    if last_send:
        offer['basic']['meta'] = {
            'content_storage_mappings_force_send': {
                'ts': datetime.utcfromtimestamp(last_send).strftime('%Y-%m-%dT%H:%M:%SZ')
            }
        }

    return offer


def expected_offer(offer_id, ts_matcher):
    result = {
        'basic': {
            'identifiers': {
                'offer_id': offer_id,
            },
            'meta': {},
        }
    }

    if ts_matcher:
        result['basic']['meta'] = {'content_storage_mappings_force_send': {'ts': {'seconds': ts_matcher}}}
    return result


DATACAMP_MESSAGES = [
    {
        'united_offers': [
            {
                'offer': [
                    make_offer('o1', None),
                    make_offer('o2', OLD_TIME),
                    make_offer('o3', NOW_TIME),
                ]
            }
        ]
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
    subscriptions_manager = cfg.create_subscriptions_manager(
        subscriber='CONTENT_STORAGE_MAPPINGS_SUBSCRIBER',
        timestamp_field='meta/content_storage_mappings_force_send',
        interval=10 * 60,
    )

    cfg.create_link(reader, unpacker)
    cfg.create_link(unpacker, adapter_converter)
    cfg.create_link(adapter_converter, subscriptions_manager)
    cfg.create_link(subscriptions_manager, writer)

    return cfg


@pytest.yield_fixture(scope='module')
def miner(yt_server, miner_config, input_topic, output_topic):
    resources = {
        'miner_cfg': miner_config,
        'input_topic': input_topic,
        'output_topic': output_topic,
    }
    with MinerTestEnv(**resources) as miner:
        miner.verify()
        yield miner


def write_read_offer_lbk(input_topic, output_topic):
    for m in DATACAMP_MESSAGES:
        input_topic.write(message_from_data(m, DatacampMessage()).SerializeToString())

    return output_topic.read(len(DATACAMP_MESSAGES))


def test_subscriptions_manager(miner, input_topic, output_topic):
    data = write_read_offer_lbk(input_topic, output_topic)

    assert_that(
        data,
        has_items(
            IsSerializedProtobuf(
                DatacampMessage,
                {
                    'united_offers': [
                        {
                            'offer': [
                                expected_offer('o1', greater_than(NOW_TIME)),
                                expected_offer('o2', greater_than(NOW_TIME)),
                                expected_offer('o3', None),
                            ]
                        }
                    ]
                },
            )
        ),
    )
