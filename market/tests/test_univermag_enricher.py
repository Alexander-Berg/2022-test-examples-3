# coding: utf-8

import pytest
from hamcrest import assert_that

import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.miner.yatf.resources.config import MinerConfig
from market.idx.datacamp.miner.yatf.test_env import MinerTestEnv
from market.idx.datacamp.yatf.matchers.matchers import HasSerializedDatacampMessages
from market.idx.yatf.resources.datacamp.datacamp_tables import UnivermagsTable
from market.idx.yatf.matchers.protobuf_matchers import IsProtobufMap
from market.pylibrary.proto_utils import message_from_data


BUSINESS_ID = 1111
SHOP_ID = 1
VENDOR_ID = 3
ANOTHER_VENDOR_ID = 4
OFFERS = [
    {
        'offer_id': '1',
        'title': 'Товар для Универмага (новый)',
        'business_id': BUSINESS_ID,
        'shop_id': SHOP_ID,
        'color': DTC.WHITE,
        'vendor_id': VENDOR_ID,
        'is_inivermag_current_value': None,
        'is_univermag_expected_value': True,
    },
    {
        'offer_id': '2',
        'title': 'Товар для Универмага (ранее уже размеченный)',
        'business_id': BUSINESS_ID,
        'shop_id': SHOP_ID,
        'color': DTC.WHITE,
        'vendor_id': VENDOR_ID,
        'is_inivermag_current_value': True,
        'is_univermag_expected_value': True,
    },
    {
        'offer_id': '3',
        'title': 'Обычный товар',
        'business_id': BUSINESS_ID,
        'shop_id': SHOP_ID,
        'color': DTC.WHITE,
        'vendor_id': ANOTHER_VENDOR_ID,
        'is_inivermag_current_value': None,
        'is_univermag_expected_value': None,
    },
    {
        'offer_id': '4',
        'title': 'Обычный товар (ранее был универмажным)',
        'business_id': BUSINESS_ID,
        'shop_id': SHOP_ID,
        'color': DTC.WHITE,
        'vendor_id': ANOTHER_VENDOR_ID,
        'is_inivermag_current_value': True,
        'is_univermag_expected_value': False,
    },
]


@pytest.fixture(scope='module')
def univermags_table_data():
    return [
        {
            'business_id': BUSINESS_ID,
            'shop_id': SHOP_ID,
            'vendor_id': VENDOR_ID,
        },
    ]


@pytest.fixture(scope='module')
def miner_config(log_broker_stuff, input_topic, output_topic,
                 yt_server, yt_token, univermags_table_path,
                 offers_blog_topic):
    cfg = MinerConfig()
    cfg.create_datacamp_logger_initializer(log_broker_stuff, offers_blog_topic, enable_trace_log=False)

    reader = cfg.create_lbk_reader_batch_processor(log_broker_stuff, input_topic, united=True)
    writer = cfg.create_lbk_writer_processor(log_broker_stuff, output_topic)
    unpacker = cfg.create_proto_unpacker_processor()
    adapter_converter = cfg.create_offer_adapter_converter()
    univermag_enricher = cfg.create_univermag_enricher_processor(
        yt_server=yt_server,
        yt_token=yt_token,
        yt_table_path=univermags_table_path,
        enabled=True
    )

    cfg.create_link(reader, unpacker)
    cfg.create_link(unpacker, adapter_converter)
    cfg.create_link(adapter_converter, univermag_enricher)
    cfg.create_link(univermag_enricher, writer)

    return cfg


@pytest.yield_fixture(scope='module')
def miner(miner_config, input_topic, output_topic, yt_server,
          univermags_table_path, univermags_table_data,
          offers_blog_topic):
    resources = {
        'miner_cfg': miner_config,
        'input_topic': input_topic,
        'output_topic': output_topic,
        'offers_blog_topic': offers_blog_topic,
        'resale_business_ids_table': UnivermagsTable(
            yt_stuff=yt_server,
            path=univermags_table_path,
            data=univermags_table_data
        ),
    }
    with MinerTestEnv(**resources) as miner:
        miner.verify()
        yield miner


def test_univermags_enricher(miner, input_topic, output_topic):
    '''Проверяем, что признак is_univermag проставляется в true только тем офферам, которые есть в специальной таблице'''
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
                        },
                        'market': {
                            'enriched_offer': {
                                'vendor_id': offer['vendor_id']
                            }
                        },
                    },
                },
                'service': {
                    offer['shop_id']: {
                        'meta': {'rgb': offer['color']},
                        'status': {
                            'is_univermag': {
                                'meta': {
                                    'applier': DTC.MINER
                                },
                                'flag': offer['is_inivermag_current_value']
                            } if offer['is_inivermag_current_value'] is not None else None
                        }
                    }}
            } for offer in OFFERS]
        }]}, DatacampMessage())

    input_topic.write(message.SerializeToString())
    data = output_topic.read(1)

    assert_that(data, HasSerializedDatacampMessages([{
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': offer['business_id'],
                        'offer_id': offer['offer_id'],
                        'shop_id': offer['shop_id'],
                    },
                },
                'service': IsProtobufMap({
                    offer['shop_id']: {
                        'status': {
                            'is_univermag': {
                                'meta': {
                                    'applier': DTC.MINER
                                },
                                'flag': offer['is_univermag_expected_value']
                            } if offer['is_univermag_expected_value'] is not None else None
                        }
                    }
                })
            } for offer in OFFERS]
        }]
    }]))
