# coding: utf-8

import pytest
from hamcrest import assert_that

from market.idx.datacamp.miner.yatf.resources.config import MinerConfig
from market.idx.datacamp.miner.yatf.test_env import MinerTestEnv
from market.idx.datacamp.yatf.matchers.matchers import HasSerializedDatacampMessages
from market.idx.datacamp.yatf.utils import dict2tskv

from market.idx.yatf.matchers.protobuf_matchers import IsProtobufMap
from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampPartnersTable

from market.idx.datacamp.proto.offer import UnitedOffer_pb2 as DTC
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage

from market.pylibrary.proto_utils import message_from_data


BUSINESS_ID = 122
SHOP_ID = 123
FEED_ID = 124
OFFER_ID = "offer"

PARTNER_DATA = [
    {
        'shop_id': SHOP_ID,
        'mbi': dict2tskv({
            'shop_id': SHOP_ID,
            'business_id': BUSINESS_ID,
            'datafeed_id': FEED_ID,
            'warehouse_id': 0,
            'shopname': 'direct_shop',
            'direct_status': 'REAL',
            'direct_search_snippet_gallery': True
        }),
        'status': 'publish'
    },
]


@pytest.fixture(scope='module')
def miner_config(yt_server, log_broker_stuff, input_topic, output_topic, yt_token, partner_info_table_path):
    cfg = MinerConfig()

    cfg.create_miner_initializer(
        yt_server=yt_server,
        token=yt_token.path,
        partners_table_path=partner_info_table_path,
    )

    reader = cfg.create_lbk_reader_batch_processor(log_broker_stuff, input_topic, united=True)
    writer = cfg.create_lbk_writer_processor(log_broker_stuff, output_topic)
    unpacker = cfg.create_proto_unpacker_processor()
    adapter_converter = cfg.create_offer_adapter_converter()

    cfg.create_link(reader, unpacker)
    cfg.create_link(unpacker, adapter_converter)
    cfg.create_link(adapter_converter, writer)

    return cfg


@pytest.yield_fixture(scope='module')
def miner(yt_server, miner_config, input_topic, output_topic, partner_info_table_path):

    resources = {
        'miner_cfg': miner_config,
        'input_topic': input_topic,
        'output_topic': output_topic,
        'partners_table': DataCampPartnersTable(
            yt_stuff=yt_server,
            path=partner_info_table_path,
            data=PARTNER_DATA,
        ),
    }
    with MinerTestEnv(**resources) as miner:
        miner.verify()
        yield miner


def test_muticolored_direct(miner, input_topic, output_topic):
    """Проверяем, что miner проставляет все нужные цвета в оффер
    """
    request = message_from_data(
        {
            'united_offers': [{
                'offer': [{
                    'basic': {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': OFFER_ID,
                        }
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
                                'rgb': DTC.BLUE,
                            }
                        }
                    }
                }]
            }]
        },
        DatacampMessage()
    )

    input_topic.write(request.SerializeToString())
    data = output_topic.read(count=1)

    assert_that(data, HasSerializedDatacampMessages([{
        'united_offers': [{
            'offer': [{
                'service': IsProtobufMap({
                    SHOP_ID: {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': OFFER_ID,
                            'shop_id': SHOP_ID,
                            'feed_id': FEED_ID,
                            'warehouse_id': 0,
                        },
                        'meta': {
                            'rgb': DTC.DIRECT_SEARCH_SNIPPET_GALLERY,
                            'platforms': IsProtobufMap({
                                DTC.DIRECT_SEARCH_SNIPPET_GALLERY: True,
                                #  DTC.DIRECT: True,
                                #  DTC.UNKNOWN_COLOR: False,
                                #  DTC.WHITE: False,
                                #  DTC.BLUE: False,
                                #  DTC.TURBO: False,
                                #  DTC.DIRECT_SITE_PREVIEW: False,
                                #  DTC.DIRECT_STANDBY: False,
                                #  DTC.DIRECT_GOODS_ADS: False,
                                #  DTC.EDA: False,
                                #  DTC.LAVKA: False,
                            }),
                        }
                    }
                })
            }]
        }]
    }]))
