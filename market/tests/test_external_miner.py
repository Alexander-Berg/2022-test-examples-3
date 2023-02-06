# coding: utf-8

import pytest
from datetime import datetime
from hamcrest import assert_that

from market.idx.datacamp.miner.yatf.resources.config import MinerConfig
from market.idx.datacamp.proto.external import Offer_pb2 as ExternalProto
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf
from market.idx.datacamp.miner.yatf.test_env import MinerTestEnv
from yatest.common.network import PortManager
from market.pylibrary.proto_utils import message_from_data

from market.idx.feeds.feedparser.yatf.resources.ucdata_pbs import UcHTTPData
from robot.rthub.yql.protos.queries_pb2 import TOfferParserItem


time_pattern = "%Y-%m-%dT%H:%M:%SZ"

UC_DATA_BASE = {
    'category_id': 1009492,
    'classification_type_value': 0,
    'classifier_category_id': 1009492,
    'classifier_confident_top_percision': 1,
    'cluster_created_timestamp': 1558365276902,
    'cluster_id': -1,
    'clutch_type': 103,
    'clutch_vendor_id': 6321244,
    'configuration_id': 0,
    'dimensions': {
        'weight': 1,
        'height': 1,
        'width': 1,
        'length': 1,
    },
    'duplicate_offer_group_id': 0,
    'enrich_type': 0,
    'generated_red_title_status': 1,
    'guru_category_id': 14692853,
    'honest_mark_departments': [
        {'name': 'name', 'probability': 1}
    ],
    'light_match_type': 2,
    'light_model_id': 0,
    'light_modification_id': 0,
    'long_cluster_id': 100390720808,
    'mapped_id': 90401,
    'market_category_name': 'category',
    'market_model_name': 'model',
    'market_sku_name': "sku",
    'market_sku_published_on_blue_market': False,
    'market_sku_published_on_market': True,
    'matched_id': 11111,
    'model_id': 0,
    'probability': 1,
    'skutch_type': 0,
    'vendor_id': 123,
    'market_vendor_name': "somevendor",
}


@pytest.fixture(scope='module')
def miner_config(yt_server, yt_token, log_broker_stuff, input_topic, output_topic, partners_table, uc_server):
    cfg = MinerConfig()

    cfg.create_miner_initializer()

    reader = cfg.create_lbk_reader_batch_processor(log_broker_stuff, input_topic, external=True)
    writer = cfg.create_lbk_writer_processor(log_broker_stuff, output_topic)
    unpacker = cfg.create_proto_unpacker_processor()
    adapter_converter = cfg.create_offer_adapter_converter()
    uc_enricher = cfg.create_uc_enricher_processor(uc_server)

    cfg.create_link(reader, unpacker)
    cfg.create_link(unpacker, adapter_converter)
    cfg.create_link(adapter_converter, uc_enricher)
    cfg.create_link(uc_enricher, writer)

    return cfg


@pytest.yield_fixture(scope='module')
def uc_server():
    with PortManager() as pm:
        port = pm.get_port()
        server = UcHTTPData.from_dict([UC_DATA_BASE for _ in range(1)], port=port)
        yield server


@pytest.yield_fixture(scope='module')
def miner(miner_config, input_topic, output_topic, partners_table, uc_server):
    resources = {
        'miner_cfg': miner_config,
        'input_topic': input_topic,
        'output_topic': output_topic,
        'partners_table': partners_table,
        'uc_server': uc_server
    }

    with MinerTestEnv(**resources) as miner:
        miner.verify()
        yield miner


@pytest.fixture(scope='module')
def write_read_offer_lbk(input_topic, output_topic):
    eoffer = message_from_data({
        'offer_id': 'abc',
        'business_id': 10,
        'timestamp':  datetime.utcfromtimestamp(12345).strftime(time_pattern)
    }, ExternalProto.Offer())
    input_topic.write(TOfferParserItem(SerializedOffer=eoffer.SerializeToString()).SerializeToString())

    return output_topic.read(count=1, wait_timeout=10)


def test_ping(miner, write_read_offer_lbk):
    assert_that(write_read_offer_lbk[0], IsSerializedProtobuf(ExternalProto.Offer, {
        'offer_id': 'abc',
        'business_id': 10,
        'timestamp': {
            'seconds': 12345
        },
        'market_content': {
            'category_id': 1009492,
            'category_name': 'category',
            'vendor': 123,
            'vendor_name': 'somevendor',
            'model_id': 0,
            'model_name': 'model'
    }}))


def test_uc_request(miner, write_read_offer_lbk):
    requests_to_uc = miner.resources['uc_server'].request

    assert_that(len(requests_to_uc.offers), 1)
