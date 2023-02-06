# coding: utf-8

import pytest
from datetime import datetime
from hamcrest import assert_that, not_, has_item

import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.proto.models.MarketSku_pb2 import ModelStatus
from market.idx.datacamp.proto.models.MarketSkuMboContent_pb2 import MarketSkuMboContent
from market.idx.datacamp.miner.yatf.resources.config import MinerConfig
from market.idx.datacamp.miner.yatf.test_env import MinerTestEnv
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf, IsProtobufMap, IsProtobuf
from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampMskuTable
from market.pylibrary.proto_utils import message_from_data

BUSINESS_ID = 111
WAREHOUSE_ID = 145

OFFERS = [
    {
        'offer_id': 'o1',
        'shop_id': 1,  # скрытие по msku + по магазину
        'msku': 1234
    },
    {
        'offer_id': 'o1',
        'shop_id': 2,  # скрытие по msku
        'msku': 1234
    },
    {
        'offer_id': 'o2',
        'shop_id': 1,
        'msku': 5678  # нет скрытий
    },
    {
        'offer_id': 'will_remove_old_abo_hidings',
        'shop_id': 1,
        'msku': 100500,  # такого msku нет в таблице, затираем старые скрытия
        'has_old_abo_hidings': True
    },
    {
        'offer_id': 'o3',
        'shop_id': 1,  # скрытие по msku
        'msku': 91011
    },
    {
        'offer_id': 'with_cargo_type',
        'shop_id': 3,
        'msku': 3  # нет скрытий або, есть данные по msku
    },
    {
        'offer_id': 'will_remove_cargo_type_unknown_msku',
        'shop_id': 3,
        'msku': 100500,  # такой msku нет в таблице, карготипы на оффере надо подчистить
    },
    {
        'offer_id': 'will_remove_cargo_type_no_msku',
        'shop_id': 3,
    },
    {
        'offer_id': 'msku_published_on_market',
        'shop_id': 3,
        'msku': 5678  # нет скрытий або, есть опубликованный msku
    },
    {
        'offer_id': 'msku_not_published_on_market',
        'shop_id': 3,
        'msku': 8765  # нет скрытий або, нет опубликованного msku в явном виде
    },
]

TS = 1618763231
MSKU_TS = datetime.utcfromtimestamp(TS).strftime('%Y-%m-%dT%H:%M:%SZ')


@pytest.fixture(scope='module')
def datacamp_msku_table_data():
    return [
        {
            'id': 1234,
            'status': message_from_data({
                # скрытие по msku
                'abo_status': {
                    'reason': DTC.MANUALLY_HIDDEN,
                    'meta': {
                        'source': DTC.MARKET_ABO_MSKU
                    }
                },
                'abo_shop_status': {
                    # скрытие по msku в пределах магазина
                    1: {
                        'reason': DTC.WRONG_SKU_MAPPING,
                        'meta': {
                            'source': DTC.MARKET_ABO_MSKU_SHOP
                        }
                    },
                    # раскрытие по msku в пределах магазина
                    2: {
                        'meta': {
                            'source': DTC.MARKET_ABO_MSKU_SHOP
                        }
                    }
                }
            }, ModelStatus()).SerializeToString()
        },
        # нет никаких скрытий по msku
        {
            'id': 5678,
            'mbo_content': message_from_data({
                'meta': {
                    'timestamp': MSKU_TS,
                    'source': DTC.MARKET_MBO
                },
                'msku': {
                    'published_on_market': True,
                }
            }, MarketSkuMboContent()).SerializeToString()
        },
        # msku снято с публикации
        {
            'id': 8765,
            'mbo_content': message_from_data({
                'meta': {
                    'timestamp': MSKU_TS,
                    'source': DTC.MARKET_MBO
                },
                'msku': {
                    'published_on_market': False,
                }
            }, MarketSkuMboContent()).SerializeToString()
        },
        {
            'id': 91011,
            'status': message_from_data({
                # скрытие по msku, заполнен вердикт
                'abo_status': {
                    'reason': DTC.MANUALLY_HIDDEN,
                    'meta': {
                        'source': DTC.MARKET_ABO_MSKU
                    },
                    'verdict': {
                        'results': [{
                            'is_banned': True,
                            'abo_reason': DTC.MANUALLY_HIDDEN,
                            'messages': [{
                                'namespace': 'ABO_WHITE',
                                'level': DTC.Explanation.ERROR,
                                'code': "BAD_QUALITY_129"
                            }],
                        }]
                    }
                },
            }, ModelStatus()).SerializeToString()
        },
        # возьмем карготип 950
        {
            'id': 3,
            'mbo_content': message_from_data({
                'meta': {
                    'timestamp': MSKU_TS,
                    'source': DTC.MARKET_MBO
                },
                'msku': {
                    'parameter_values': [
                        {
                            'bool_value': True,
                            'xsl_name': 'cargoType950'
                        },
                        {
                            'bool_value': False,
                            'xsl_name': 'cargoType300'
                        }
                    ]
                }
            }, MarketSkuMboContent()).SerializeToString()
        }
    ]


@pytest.fixture(scope='module')
def miner_config(log_broker_stuff, input_topic, output_topic,
                 yt_server, yt_token, datacamp_msku_table_path,
                 offers_blog_topic):
    cfg = MinerConfig()
    cfg.create_datacamp_logger_initializer(log_broker_stuff, offers_blog_topic, enable_trace_log=False)

    reader = cfg.create_lbk_reader_batch_processor(log_broker_stuff, input_topic, united=True)
    writer = cfg.create_lbk_writer_processor(log_broker_stuff, output_topic)
    unpacker = cfg.create_proto_unpacker_processor()
    adapter_converter = cfg.create_offer_adapter_converter()
    datacamp_msku_enricher = cfg.create_datacamp_msku_enricher_processor(
        yt_server=yt_server,
        yt_token=yt_token.path,
        yt_table_path=datacamp_msku_table_path,
        enable_hiding_offers_without_published_msku=True,
    )

    cfg.create_link(reader, unpacker)
    cfg.create_link(unpacker, adapter_converter)
    cfg.create_link(adapter_converter, datacamp_msku_enricher)
    cfg.create_link(datacamp_msku_enricher, writer)

    return cfg


@pytest.yield_fixture(scope='module')
def miner(miner_config, input_topic, output_topic, yt_server,
          datacamp_msku_table_path, datacamp_msku_table_data,
          offers_blog_topic):
    resources = {
        'miner_cfg': miner_config,
        'input_topic': input_topic,
        'output_topic': output_topic,
        'offers_blog_topic': offers_blog_topic,
        'datacamp_msku_table': DataCampMskuTable(
            yt_stuff=yt_server,
            path=datacamp_msku_table_path,
            data=datacamp_msku_table_data
        ),
    }
    with MinerTestEnv(**resources) as miner:
        miner.verify()
        yield miner


@pytest.yield_fixture(scope='module')
def messages(input_topic, output_topic):
    master_data_with_cargo = {
        'cargo_type': {
            'value': [100, 200],
        }
    }

    message = message_from_data({
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': offer['offer_id'],
                    },
                    'content': {
                        'binding': {
                            'approved': {'market_sku_id': offer['msku']} if offer['offer_id'] != 'will_remove_cargo_type_no_msku' else None
                        },
                        'master_data': master_data_with_cargo if offer['offer_id'].startswith('will_remove_cargo_type') else None
                    },
                },
                'service': {
                    offer['shop_id']: {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': offer['offer_id'],
                            'shop_id': offer['shop_id'],
                            'warehouse_id': WAREHOUSE_ID,
                        },
                        'meta': {'rgb': DTC.BLUE},
                        'status': {
                            'united_catalog': {
                                'flag': True
                            },
                            'disabled': [{
                                'meta': {'source': DTC.MARKET_ABO_MSKU},
                                'flag': True
                            }, {
                                'meta': {'source': DTC.MARKET_ABO_MSKU_SHOP},
                                'flag': True
                            }] if 'has_old_abo_hidings' in offer else None
                        }
                    }}
            } for offer in OFFERS]
        }]}, DatacampMessage())

    input_topic.write(message.SerializeToString())
    return output_topic.read(1, wait_timeout=10)


def test_not_touch_basic_part(miner, messages):
    for offer in OFFERS:
        assert_that(messages[0], IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [{
                    'basic': {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': offer['offer_id'],
                        },
                        'status': None,
                        'resolution': None,
                    }
                }]
            }]}))


def test_set_msku_hidings(miner, messages):
    assert_that(messages[0], IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [{
                'service': IsProtobufMap({
                    1: {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': 'o1',
                            'shop_id': 1,
                        },
                        'status': {
                            "disabled": [{
                                'flag': True,
                                'meta': {
                                    'source': DTC.MARKET_ABO_MSKU,
                                    'applier': DTC.MINER,
                                },
                            }, {
                                'flag': True,
                                'meta': {
                                    'source': DTC.MARKET_ABO_MSKU_SHOP,
                                    'applier': DTC.MINER,
                                },
                            }]
                        },
                        'resolution': {
                            'by_source': [{
                                'verdict': [{
                                    'results': [{
                                        'is_banned': True,
                                        'abo_reason': DTC.MANUALLY_HIDDEN,
                                        'messages': [{
                                            'namespace': 'ABO',
                                            'level': DTC.Explanation.ERROR,
                                            'code': "MANUALLY_HIDDEN"
                                        }],
                                    }]
                                }],
                                'meta': {
                                    'source': DTC.MARKET_ABO_MSKU,
                                    'applier': DTC.MINER,
                                }
                            }, {
                                'verdict': [{
                                    'results': [{
                                        'is_banned': True,
                                        'abo_reason': DTC.WRONG_SKU_MAPPING,
                                        'messages': [{
                                            'namespace': 'ABO',
                                            'level': DTC.Explanation.ERROR,
                                            'code': "WRONG_SKU_MAPPING"
                                        }],
                                    }]
                                }],
                                'meta': {
                                    'source': DTC.MARKET_ABO_MSKU_SHOP,
                                    'applier': DTC.MINER,
                                }
                            }]
                        },
                    },
                })
            }, {
                'service': IsProtobufMap({
                    1: {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': 'o3',
                            'shop_id': 1,
                        },
                        'status': {
                            "disabled": [{
                                'flag': True,
                                'meta': {
                                    'source': DTC.MARKET_ABO_MSKU,
                                    'applier': DTC.MINER,
                                },
                            }]
                        },
                        'resolution': {
                            'by_source': [{
                                'verdict': [{
                                    'results': [{
                                        'is_banned': True,
                                        'abo_reason': DTC.MANUALLY_HIDDEN,
                                        'messages': [{
                                            'namespace': 'ABO_WHITE',
                                            'level': DTC.Explanation.ERROR,
                                            'code': "BAD_QUALITY_129"
                                        }],
                                    }]
                                }],
                                'meta': {
                                    'source': DTC.MARKET_ABO_MSKU,
                                    'applier': DTC.MINER,
                                }
                            }]
                        },
                    },
                })
            }]
        }]}))


def test_remove_msku_shop_hiding(miner, messages):
    assert_that(messages[0], IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [{
                'service': IsProtobufMap({
                    2: {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': 'o1',
                            'shop_id': 2,
                        },
                        'status': {
                            "disabled": [{
                                'flag': True,
                                'meta': {
                                    'source': DTC.MARKET_ABO_MSKU,
                                },
                            }]
                        },
                        'resolution': {
                            'by_source': [{
                                'verdict': [{
                                    'results': [{
                                        'is_banned': True,
                                        'abo_reason': DTC.MANUALLY_HIDDEN,
                                    }]
                                }],
                                'meta': {
                                    'source': DTC.MARKET_ABO_MSKU,
                                }
                            }]
                        },
                    },
                })
            }]
        }]}))

    assert_that(messages[0], not_(IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [{
                'service': IsProtobufMap({
                    3: {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': 'o1',
                            'shop_id': 3,
                        },
                        'status': {
                            "disabled": [{
                                'meta': {
                                    'source': DTC.MARKET_ABO_MSKU_SHOP,
                                },
                            }]
                        },
                        'resolution': {
                            'by_source': [{
                                'meta': {
                                    'source': DTC.MARKET_ABO_MSKU_SHOP,
                                }
                            }]
                        },
                    },
                })
            }]
        }]})))


def test_not_set_msku_hidings(miner, messages):
    assert_that(messages[0], IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [{
                'service': IsProtobufMap({
                    1: {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': 'o2',
                            'shop_id': 1,
                        },
                        'status': {
                            'disabled': [
                                {'flag': False, 'meta': {'source': DTC.MARKET_IDX}}
                            ]
                        },
                    },
                })
            }]
        }]}))


def test_cargo_types(miner, messages):
    assert_that(messages[0], IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': 'with_cargo_type'
                    },
                    'content': {
                        'master_data': {
                            'cargo_type': {
                                'value': [950]
                            }
                        }
                    }
                },
                'service': IsProtobufMap({
                    3: {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': 'with_cargo_type',
                            'shop_id': 3,
                        },
                        'delivery': {
                            'delivery_info': {
                                'cargo_type': [950],
                                'meta': {
                                    'timestamp': {
                                        'seconds': TS
                                    },
                                    'applier': DTC.MINER
                                },
                            },
                        }
                    },
                })
            },
            {
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': 'will_remove_cargo_type_unknown_msku'
                    },
                    'content': {
                        'master_data': {
                            'cargo_type': {
                                'value': []
                            }
                        }
                    }
                }
            },
            {
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': 'will_remove_cargo_type_no_msku'
                    },
                    'content': {
                        'master_data': {
                            'cargo_type': {
                                'value': []
                            }
                        }
                    }
                }
            }
            ]
        }]}))


def test_hide_by_msku_dsbs_offers(miner, input_topic, output_topic):
    message = message_from_data({
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': 'dsbs_offer_hidden_by_msku',
                    },
                    'content': {
                        'binding': {
                            'uc_mapping': {
                                'market_sku_id': 1234
                            }
                        }
                    }
                },
                'service': {
                    4: {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': 'dsbs_offer_hidden_by_msku',
                            'shop_id': 4,
                            'warehouse_id': 0,
                        },
                        'meta': {'rgb': DTC.WHITE},
                        'partner_info': {
                            'is_dsbs': True
                        },
                    }
                }
            }]
        }]}, DatacampMessage())

    input_topic.write(message.SerializeToString())
    data = output_topic.read(1, wait_timeout=10)

    assert_that(data[0], IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [{
                'service': IsProtobufMap({
                    4: {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': 'dsbs_offer_hidden_by_msku',
                            'shop_id': 4,
                        },
                        'status': {
                            "disabled": [{
                                'flag': True,
                                'meta': {
                                    'source': DTC.MARKET_ABO_MSKU,
                                    'applier': DTC.MINER,
                                },
                            }]
                        },
                    },
                })
            }]
        }]}))


def test_clear_old_abo_hidings(miner, messages):
    assert_that(messages[0], IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [{
                'service': IsProtobufMap({
                    1: {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': 'will_remove_old_abo_hidings',
                            'shop_id': 1,
                        },
                        'status': {
                            "disabled": [{
                                'flag': False,
                                'meta': {
                                    'source': DTC.MARKET_ABO_MSKU,
                                    'applier': DTC.MINER,
                                },
                            }, {
                                'flag': False,
                                'meta': {
                                    'source': DTC.MARKET_ABO_MSKU_SHOP,
                                    'applier': DTC.MINER,
                                },
                            }]
                        },
                    },
                })
            }]
        }]}))


def test_hide_offers_without_published_msku(miner, messages):
    for offer_id, shop_id, msku in zip(['o1', 'o3', 'msku_published_on_market'], [1, 1, 3], [1234, 91011, 5678]):
        assert_that(messages[0], IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [{
                    'service': IsProtobufMap({
                        shop_id: {
                            'identifiers': {
                                'business_id': BUSINESS_ID,
                                'offer_id': offer_id,
                                'shop_id': shop_id,
                            },
                            "status": {
                                "disabled": not_(has_item(IsProtobuf({
                                    "flag": True,
                                    "meta": {
                                        "source": DTC.MARKET_IDX,
                                        "applier": DTC.MINER,
                                    },
                                }))),
                            },
                            "resolution": {
                                "by_source": not_(has_item(IsProtobuf(
                                    {
                                        "meta": {
                                            "source": DTC.MARKET_IDX,
                                        },
                                        "verdict": [
                                            {
                                                "results": [
                                                    {
                                                        "is_banned": True,
                                                        "messages": [
                                                            {
                                                                "code": "49n",
                                                                "params": [
                                                                    {
                                                                        "name": "code",
                                                                        "value": "49n"
                                                                    },
                                                                    {
                                                                        "name": "msku",
                                                                        "value": str(msku)
                                                                    }
                                                                ],
                                                                "details": "{\"code\":\"49n\",\"msku\":" + str(msku) + "}",
                                                                "level": DTC.Explanation.ERROR
                                                            }
                                                        ]
                                                    }
                                                ]
                                            }
                                        ]
                                    }
                                )))
                            },
                        }
                    })
                }]
            }]
        }))
    for offer_id, shop_id, msku in zip(['msku_not_published_on_market'], [3], [8765]):
        assert_that(messages[0], IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [{
                    'service': IsProtobufMap({
                        shop_id: {
                            'identifiers': {
                                'business_id': BUSINESS_ID,
                                'offer_id': offer_id,
                                'shop_id': shop_id,
                            },
                            "status": {
                                "disabled": [
                                    {
                                        "flag": True,
                                        "meta": {
                                            "source": DTC.MARKET_IDX,
                                            "applier": DTC.MINER,
                                        },
                                    },
                                ],
                            },
                            "resolution": {
                                "by_source": [
                                    {
                                        "meta": {
                                            "source": DTC.MARKET_IDX,
                                        },
                                        "verdict": [
                                            {
                                                "results": [
                                                    {
                                                        "is_banned": True,
                                                        "messages": [
                                                            {
                                                                "code": "49n",
                                                                "params": [
                                                                    {
                                                                        "name": "code",
                                                                        "value": "49n"
                                                                    },
                                                                    {
                                                                        "name": "msku",
                                                                        "value": str(msku)
                                                                    }
                                                                ],
                                                                "details": "{\"code\":\"49n\",\"msku\":" + str(msku) + "}",
                                                                "level": DTC.Explanation.ERROR
                                                            }
                                                        ]
                                                    }
                                                ]
                                            }
                                        ]
                                    }
                                ]
                            },
                        }
                    })
                }]
            }]
        }))
