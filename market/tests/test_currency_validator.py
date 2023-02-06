# coding: utf-8

from hamcrest import assert_that
import os
import pytest
import yatest.common

from market.pylibrary.proto_utils import message_from_data

import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC

from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf, IsProtobufMap

from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.miner.yatf.resources.config import MinerConfig
from market.idx.datacamp.miner.yatf.test_env import MinerTestEnv


@pytest.yield_fixture(scope="module")
def currency_rates_path():
    return os.path.join(
        yatest.common.source_path(),
        'market', 'idx', 'yatf', 'resources', 'stubs', 'getter', 'mbi', 'currency_rates_no_bank_for_EUR.xml'
    )


@pytest.fixture(scope='module')
def miner_config(offers_blog_topic, log_broker_stuff, input_topic, output_topic, currency_rates_path):
    cfg = MinerConfig()
    cfg.create_datacamp_logger_initializer(log_broker_stuff, offers_blog_topic, enable_trace_log=False)
    cfg.create_miner_initializer(currency_rates=currency_rates_path)

    reader = cfg.create_lbk_reader_batch_processor(log_broker_stuff, input_topic, True)
    writer = cfg.create_lbk_writer_processor(log_broker_stuff, output_topic)
    unpacker = cfg.create_proto_unpacker_processor()
    adapter_converter = cfg.create_offer_adapter_converter()
    currency_validator = cfg.create_currency_validator()

    cfg.create_link(reader, unpacker)
    cfg.create_link(unpacker, adapter_converter)
    cfg.create_link(adapter_converter, currency_validator)
    cfg.create_link(currency_validator, writer)

    return cfg


@pytest.yield_fixture(scope='module')
def miner(miner_config, input_topic, output_topic, offers_blog_topic):
    resources = {
        'miner_cfg': miner_config,
        'input_topic': input_topic,
        'output_topic': output_topic,
        'offers_blog_topic': offers_blog_topic,
    }
    with MinerTestEnv(**resources) as miner:
        miner.verify()
        yield miner


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
                        'price': offer_data['price'],
                        'meta': {
                            'rgb': DTC.WHITE
                        },
                    }}
            }]
        }]}, DatacampMessage())
    input_topic.write(message.SerializeToString())
    return output_topic.read(count=1)


def test_currency_validator_base(miner, input_topic, output_topic):
    """Проверяем, что работает загрузка валютного файла в miner initializer и отрабатывает валидация валют
    Используем валютный файл currency_rates_no_bank_for_EUR.xml, в котором нет банка для EUR => не может быть реф.валютой"""
    actual_offer = {
        'identifiers': {
            'shop_id': 10296180,
            'business_id': 10296180,
            'offer_id': 'offerWithInvalidCurrency',
        },
        'price': {
            'basic': {
                'binary_price': {
                    'price': 5000000000,
                    'id': 'USD',
                    'ref_id': 'EUR',
                }
            }
        },
    }

    data = write_read_offer_lbk(actual_offer, input_topic, output_topic)
    assert_that(data[0], IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': 10296180,
                        'offer_id': 'offerWithInvalidCurrency',
                    },
                },
                'service': IsProtobufMap({
                    10296180: {
                        'identifiers': {
                            'shop_id': 10296180,
                            'business_id': 10296180,
                            'offer_id': 'offerWithInvalidCurrency',
                        },
                        'price': None,
                        'status': {
                            'disabled': [
                                {
                                    'flag': True,
                                    'meta': {
                                        'source': DTC.MARKET_IDX
                                    }
                                }
                            ],
                        },
                    },
                })
            }]
        }]}))
