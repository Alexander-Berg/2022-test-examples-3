# coding: utf-8

import pytest
from hamcrest import assert_that, empty, has_length

from market.idx.datacamp.miner.yatf.resources.config import MinerConfig
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.proto.offer import DataCampOffer_pb2 as DTC
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf, IsProtobufMap
from market.pylibrary.proto_utils import message_from_data


@pytest.fixture(scope='module')
def miner_config(yt_server, yt_token, log_broker_stuff, input_topic, output_topic, partners_table):
    cfg = MinerConfig()

    cfg.create_miner_initializer(
        yt_server=yt_server,
        token=yt_token.path,
        partners_table_path=partners_table.get_path(),
    )

    reader = cfg.create_lbk_reader_batch_processor(log_broker_stuff, input_topic, united=True)
    writer = cfg.create_lbk_writer_processor(log_broker_stuff, output_topic)
    unpacker = cfg.create_proto_unpacker_processor()
    adapter_converter = cfg.create_offer_adapter_converter()

    cfg.create_link(reader, unpacker)
    cfg.create_link(unpacker, adapter_converter)
    cfg.create_link(adapter_converter, writer)

    return cfg


def write_read_offer_lbk(offer_data, input_topic, output_topic):
    message = message_from_data({
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': offer_data['identifiers']['business_id'],
                        'offer_id': offer_data['identifiers']['offer_id'],
                    },
                },
                'service': {
                    offer_data['identifiers']['shop_id']: {
                        'identifiers': {
                            'business_id': offer_data['identifiers']['business_id'],
                            'shop_id': offer_data['identifiers']['shop_id'],
                            'offer_id': offer_data['identifiers']['offer_id'],
                        },
                        'meta': {
                            'rgb': DTC.WHITE
                        },
                        'status': {
                            'disabled': offer_data.get('disables', [])
                        },
                        'resolution': {
                            'by_source': offer_data.get('verdicts', [])
                        }
                    }
                }
            }]
    }]}, DatacampMessage())
    input_topic.write(message.SerializeToString())

    return output_topic.read(count=1)


@pytest.mark.parametrize('original, expected', [
    (
        [],
        [
            {'flag': False, 'meta': {'source': DTC.MARKET_IDX}}
        ]
    ), (
        [
            {'flag': True, 'meta': {'source': DTC.MARKET_IDX}}
        ],
        [
            {'flag': False, 'meta': {'source': DTC.MARKET_IDX}}
        ]
    ), (
        [
            {'flag': True, 'meta': {'source': DTC.MARKET_IDX}},
            {'flag': False, 'meta': {'source': DTC.MARKET_ABO}},
            {'flag': True, 'meta': {'source': DTC.MARKET_MBI}},
        ],
        [
            {'flag': False, 'meta': {'source': DTC.MARKET_IDX}},
            {'flag': False, 'meta': {'source': DTC.MARKET_ABO}},
            {'flag': True, 'meta': {'source': DTC.MARKET_MBI}},
        ]
    )
], ids=[
    'init_disable_flag',
    'reset_flag_to_false',
    'do_not_change_other_flags'
])
def test_disable_flags(miner, input_topic, output_topic, original, expected):
    """Проверяем, miner сбрасывает disabled флаги MARKET_IDX в начале работы
    """
    offer = {
        'identifiers': {
            'business_id': 1,
            'shop_id': 1,
            'offer_id': 'o1',
        },
        'disables': original
    }

    data = write_read_offer_lbk(offer, input_topic, output_topic)
    assert_that(data[0], IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [{
                'service': IsProtobufMap({
                    1: {
                        'identifiers': {
                            'business_id': 1,
                            'offer_id': 'o1',
                            'shop_id': 1,
                        },
                        'status': {
                            'disabled': expected
                        }
                    },
                })
            }]
        }]})
    )


@pytest.mark.parametrize('original, expected', [
    (
        [
            {
                'meta': {'source': DTC.MARKET_IDX},
                'verdict': [{}]
            }
        ],
        [
            {
                'meta': {'source': DTC.MARKET_IDX},
                'verdict': empty()
            }
        ]
    ), (
        [
            {
                'meta': {'source': DTC.MARKET_IDX},
                'verdict': [{}]
            },
            {
                'meta': {'source': DTC.MARKET_IDX_GENERATION},
                'verdict': [{}]
            }
        ],
        [
            {
                'meta': {'source': DTC.MARKET_IDX},
                'verdict': empty()
            },
            {
                'meta': {'source': DTC.MARKET_IDX_GENERATION},
                'verdict': has_length(1)
            }
        ]
    )
], ids=[
    'reset_market_idx_verdicts',
    'do_not_change_other_verdicts'
])
def test_verdicts(miner, input_topic, output_topic, original, expected):
    """Должны очистить вердикты с MARKET_IDX
    """
    offer = {
        'identifiers': {
            'business_id': 1,
            'shop_id': 1,
            'offer_id': 'o1',
        },
        'verdicts': original
    }

    data = write_read_offer_lbk(offer, input_topic, output_topic)
    assert_that(data[0], IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [{
                'service': IsProtobufMap({
                    1: {
                        'identifiers': {
                            'business_id': 1,
                            'offer_id': 'o1',
                            'shop_id': 1,
                        },
                        'resolution': {
                            'by_source': expected
                        }
                    },
                })
            }]
        }]})
    )


def test_deduplication(miner, input_topic, output_topic):
    """Должна сработать дедупликация по полю status & resolution, так как он не менялся
    """
    offer = {
        'identifiers': {
            'business_id': 1,
            'shop_id': 1,
            'offer_id': 'o1',
        },
        'disables': [
            {'flag': False, 'meta': {'source': DTC.MARKET_IDX}}
        ],
        'verdicts': [
            {
                'meta': {'source': DTC.MARKET_IDX},
                'verdict': []
            }
        ]
    }

    data = write_read_offer_lbk(offer, input_topic, output_topic)
    assert_that(data[0], IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [{
                'service': IsProtobufMap({
                    1: {
                        'identifiers': {
                            'business_id': 1,
                            'offer_id': 'o1',
                            'shop_id': 1,
                        },
                        'status': None,
                        'resolution': None
                    },
                })
            }]
        }]})
    )
