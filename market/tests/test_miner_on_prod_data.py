# coding: utf-8

import pytest
import yatest.common
import os
import logging

from google.protobuf.json_format import MessageToJson

from market.idx.datacamp.miner.yatf.resources.config import MinerConfig
from market.idx.datacamp.miner.yatf.test_env import MinerTestEnv
from market.idx.yatf.resources.lbk_data import LbkInputData

from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage


@pytest.fixture(scope='session')
def lbk_input_data():
    path = os.path.join(yatest.common.source_path(), 'market', 'idx', 'datacamp', 'miner', 'tests', 'data', 'datacamp-offers-to-miner')
    return LbkInputData(path, format='json_list', proto_cls=DatacampMessage)


@pytest.fixture(scope='module')
def miner_config(log_broker_stuff, input_topic, output_topic):
    cfg = MinerConfig()

    reader = cfg.create_lbk_reader_batch_processor(log_broker_stuff, input_topic, united=True)
    writer = cfg.create_lbk_writer_processor(log_broker_stuff, output_topic)
    unpacker = cfg.create_proto_unpacker_processor()
    adapter_converter = cfg.create_offer_adapter_converter()

    cfg.create_link(reader, unpacker)
    cfg.create_link(unpacker, adapter_converter)
    cfg.create_link(adapter_converter, writer)

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


def test_miner_on_prod_data(miner, lbk_input_data, input_topic, output_topic):
    """Проверяем майнер на продовых данных"""
    input_topic.push_data(lbk_input_data)

    data = output_topic.read(count=1)

    log = logging.getLogger()
    log.info("Offers in table")

    offers = DatacampMessage()
    offers.ParseFromString(data[0])

    for serialized_offer in offers.united_offers[0].offer:
        log.info(MessageToJson(serialized_offer, preserving_proto_field_name=True))
