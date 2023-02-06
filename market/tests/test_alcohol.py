# coding: utf-8

import pytest

from hamcrest import assert_that, has_item, is_not, empty

import market.idx.datacamp.proto.offer.UnitedOffer_pb2 as DTC

from market.idx.datacamp.miner.yatf.resources.config import MinerConfig
from market.idx.datacamp.miner.yatf.test_env import MinerTestEnv
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.yatf.utils import dict2tskv
from market.idx.datacamp.yatf.matchers.matchers import HasSerializedDatacampMessages
from market.idx.yatf.matchers.lb_matchers import HasNoUnreadData
from market.idx.yatf.matchers.protobuf_matchers import IsProtobufMap, IsProtobuf
from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampPartnersTable
from market.idx.yatf.resources.shops_outlet import ShopsOutlet
from yatest.common.network import PortManager
from market.pylibrary.proto_utils import message_from_data
from market.idx.feeds.feedparser.yatf.resources.ucdata_pbs import UcHTTPData


DC_GENERATION = 10
BUSINESS_ID = 999
ALCO_CATEGORY_ID = 16155466  # Алкоголь - Вино
NOT_ALCO_CATEGORY_ID = 1

SHOP_ID_ALCOHOL_STATUS_SBX_NO_LICENSE = 1
SHOP_ID_ALCOHOL_STATUS_SBX_HAS_LICENSE = 2
SHOP_ID_ALCOHOL_STATUS_SBX_HAS_LICENSE_PS = 3
SHOP_ID_ALCOHOL_STATUS_SBX_NEW_LICENSE_ONLY_PS = 4
SHOP_ID_ALCOHOL_STATUS_REAL_HAS_LICENSE = 5
SHOP_ID_ALCOHOL_STATUS_REAL_HAS_LICENSE_PS = 6
SHOP_ID_ALCOHOL_STATUS_REAL_NEW_LICENSE_ONLY = 7
SHOP_ID_ALCOHOL_STATUS_UNKNOWN_NO_LEGAL_INFO = 8
SHOP_ID_ALCOHOL_STATUS_UNKNOWN_HAS_LEGAL_INFO = 545773  # Винный дом "Каудаль" (from alcodata)
SHOP_ID_ALCOHOL_STATUS_NO_HAS_LICENSE = 9
SHOP_ID_ALCOHOL_STATUS_REAL_HAS_LICENSE_NOT_ALCO_TYPE = 10
SHOP_ID_ALCOHOL_STATUS_REAL_HAS_LICENSE_NOT_ALCO_CATEGORY = 11


def gen_feed_id(shop_id):
    return shop_id * 10


def gen_offer_id(shop_id):
    return '{}'.format(shop_id)


# shop_id, is_enabled, is_tested, alcohol_status
SHOPS = [
    (SHOP_ID_ALCOHOL_STATUS_SBX_NO_LICENSE, True, False, 'SBX', DTC.ALCO, ALCO_CATEGORY_ID),
    (SHOP_ID_ALCOHOL_STATUS_SBX_HAS_LICENSE, True, False, 'SBX', DTC.ALCO, ALCO_CATEGORY_ID),
    (SHOP_ID_ALCOHOL_STATUS_SBX_HAS_LICENSE_PS, False, True, 'SBX', DTC.ALCO, ALCO_CATEGORY_ID),
    (SHOP_ID_ALCOHOL_STATUS_SBX_NEW_LICENSE_ONLY_PS, False, True, 'SBX', DTC.ALCO, ALCO_CATEGORY_ID),
    (SHOP_ID_ALCOHOL_STATUS_REAL_HAS_LICENSE, True, False, 'REAL', DTC.ALCO, ALCO_CATEGORY_ID),
    (SHOP_ID_ALCOHOL_STATUS_REAL_HAS_LICENSE_PS, False, True, 'REAL', DTC.ALCO, ALCO_CATEGORY_ID),
    (SHOP_ID_ALCOHOL_STATUS_REAL_NEW_LICENSE_ONLY, True, False, 'REAL', DTC.ALCO, ALCO_CATEGORY_ID),
    (SHOP_ID_ALCOHOL_STATUS_REAL_HAS_LICENSE_NOT_ALCO_TYPE, True, False, 'REAL', DTC.UNKNOWN_PRODUCT_TYPE,
     ALCO_CATEGORY_ID),
    (SHOP_ID_ALCOHOL_STATUS_REAL_HAS_LICENSE_NOT_ALCO_CATEGORY, True, False, 'REAL', DTC.ALCO, NOT_ALCO_CATEGORY_ID),
    (SHOP_ID_ALCOHOL_STATUS_UNKNOWN_NO_LEGAL_INFO, True, False, None, DTC.ALCO, ALCO_CATEGORY_ID),
    (SHOP_ID_ALCOHOL_STATUS_UNKNOWN_HAS_LEGAL_INFO, True, False, None, DTC.ALCO, ALCO_CATEGORY_ID),
    (SHOP_ID_ALCOHOL_STATUS_NO_HAS_LICENSE, True, False, 'NO', DTC.ALCO, ALCO_CATEGORY_ID),
]


OFFERS = [
    {
        'id': gen_offer_id(shop_id),
        'shop_id': shop_id,
        'type': offer_type,
        'uc_data': {
            'category_id': category_id
        }
    } for shop_id, is_enabled, is_tested, alcohol_status, offer_type, category_id in SHOPS
]


UC_DATA_BASE = {
    'category_id': 1,
    'market_category_name': 'Some category',
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
    'market_model_name': 'model',
    'market_sku_id': 1,
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
def partner_data():
    return [
        {
            'shop_id': shop_id,
            'mbi': dict2tskv({
                'shop_id': shop_id,
                'business_id': BUSINESS_ID,
                'datafeed_id': gen_feed_id(shop_id),
                'is_enabled': is_enabled,
                'is_tested': is_tested,
                'alcohol': alcohol_status if alcohol_status else None,
                'united_catalog_status': 'SUCCESS',
                'is_site_market': 'true'
            }),
            'status': 'publish'
        } for shop_id, is_enabled, is_tested, alcohol_status, offer_type, category_id in SHOPS
    ]


@pytest.fixture(scope='module')
def shops_outlet_mmap():
    return ShopsOutlet.from_shops({
        shop_id: [{
            'shop_point_id': 'M21',
            'id': 432,
            'name': 'Cool Outlet',
            'type': 'retail',
            'locality_name': 'Cool Outlet',
            'gps_coord': '123.45,6788.90',
            'region_id': '213',
            'licenses': [{'type': 'ALCOHOL', 'check_status': license}] if license else [],
        }] for shop_id, license in [
            (SHOP_ID_ALCOHOL_STATUS_SBX_NO_LICENSE, None),
            (SHOP_ID_ALCOHOL_STATUS_SBX_HAS_LICENSE, 'SUCCESS'),
            (SHOP_ID_ALCOHOL_STATUS_SBX_HAS_LICENSE_PS, 'SUCCESS'),
            (SHOP_ID_ALCOHOL_STATUS_SBX_NEW_LICENSE_ONLY_PS, 'NEW'),
            (SHOP_ID_ALCOHOL_STATUS_REAL_HAS_LICENSE, 'SUCCESS'),
            (SHOP_ID_ALCOHOL_STATUS_REAL_HAS_LICENSE_NOT_ALCO_TYPE, 'SUCCESS'),
            (SHOP_ID_ALCOHOL_STATUS_REAL_HAS_LICENSE_NOT_ALCO_CATEGORY, 'SUCCESS'),
            (SHOP_ID_ALCOHOL_STATUS_REAL_HAS_LICENSE_PS, 'SUCCESS'),
            (SHOP_ID_ALCOHOL_STATUS_REAL_NEW_LICENSE_ONLY, 'NEW'),
            (SHOP_ID_ALCOHOL_STATUS_UNKNOWN_NO_LEGAL_INFO, None),
            (SHOP_ID_ALCOHOL_STATUS_UNKNOWN_HAS_LEGAL_INFO, None),
            (SHOP_ID_ALCOHOL_STATUS_NO_HAS_LICENSE, 'SUCCESS'),
        ]
    })


@pytest.yield_fixture(scope='module')
def uc_server():
    def merge_dicts(dict1, dict2):
        final_dict = dict1.copy()
        final_dict.update(dict2)
        return final_dict

    with PortManager() as pm:
        port = pm.get_port()
        server = UcHTTPData.from_shops_dict({
            offer['shop_id']: [merge_dicts(UC_DATA_BASE, offer.get('uc_data'))]
            for offer in OFFERS
        }, port=port)
        yield server


@pytest.fixture(scope='module')
def miner_config(
    yt_server,
    log_broker_stuff,
    input_topic,
    output_topic,
    offers_blog_topic,
    uc_server,
    yt_token,
    partner_info_table_path,
    shops_outlet_mmap,
):
    cfg = MinerConfig()

    cfg.create_miner_initializer(
        yt_server=yt_server,
        token=yt_token.path,
        partners_table_path=partner_info_table_path,
    )
    cfg.create_datacamp_logger_initializer(log_broker_stuff, offers_blog_topic)

    reader = cfg.create_lbk_reader_batch_processor(log_broker_stuff, input_topic, True)
    writer = cfg.create_lbk_writer_processor(log_broker_stuff, output_topic)
    unpacker = cfg.create_proto_unpacker_processor()
    adapter_converter = cfg.create_offer_adapter_converter()
    offer_content_converter = cfg.create_offer_content_converter('white')
    uc_enricher = cfg.create_uc_enricher_processor(uc_server)
    delivery_validator = cfg.create_delivery_validator(shops_outlet_mmap)

    cfg.create_link(reader, unpacker)
    cfg.create_link(unpacker, adapter_converter)
    cfg.create_link(adapter_converter, offer_content_converter)
    cfg.create_link(offer_content_converter, uc_enricher)
    cfg.create_link(uc_enricher, delivery_validator)
    cfg.create_link(delivery_validator, writer)

    return cfg


@pytest.yield_fixture(scope='module')
def miner(
    yt_server,
    miner_config,
    input_topic, output_topic,
    uc_server,
    partner_info_table_path,
    partner_data,
    offers_blog_topic,
    shops_outlet_mmap,
):
    resources = {
        'miner_cfg': miner_config,
        'input_topic': input_topic,
        'output_topic': output_topic,
        'blog_topic': offers_blog_topic,
        'uc_server': uc_server,
        'partners_table': DataCampPartnersTable(
            yt_stuff=yt_server,
            path=partner_info_table_path,
            data=partner_data
        ),
        'shops_outlet_mmap': shops_outlet_mmap,
    }
    with MinerTestEnv(**resources) as miner:
        miner.verify()
        yield miner


@pytest.fixture(scope='module')
def write_read_offer_lbk(miner, input_topic, output_topic):
    input_topic.write(message_from_data({
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': offer['id'],
                    },
                    'content': {
                        'partner': {
                            'original': {
                                'type': {
                                    'value': offer['type']
                                },
                                'name': {
                                    'value': 'name',
                                },
                            }
                        }
                    }
                },
                'service': {
                    offer['shop_id']: {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': offer['id'],
                            'shop_id': offer['shop_id'],
                            'feed_id': gen_feed_id(offer['shop_id']),
                            'warehouse_id': 0,
                        },
                        'content': {
                            'partner': {
                                'original': {
                                    'url': {
                                        'value': 'http://datacamp.ru/pretty_offer'
                                    }
                                }
                            }
                        },
                        'delivery': {
                            'partner': {
                                'original': {
                                    'delivery': {
                                        'flag': True,
                                        'meta': {
                                            'source': DTC.MARKET_IDX
                                        }
                                    },
                                    'delivery_options': {
                                        'options': [{
                                            'DaysMin': 1,
                                            'DaysMax': 3,
                                            'Cost': 0,
                                        }],
                                        'meta': {
                                            'source': DTC.MARKET_IDX
                                        }
                                    },
                                    'pickup': {
                                        'flag': True,
                                        'meta': {
                                            'source': DTC.MARKET_IDX
                                        }
                                    },
                                    'pickup_options': {
                                        'options': [{
                                            'DaysMin': 1,
                                            'DaysMax': 3,
                                            'Cost': 0,
                                        }],
                                        'meta': {
                                            'source': DTC.MARKET_IDX
                                        }
                                    }
                                }
                            }
                        },
                        'status': {
                            'disabled': [
                                {
                                    'flag': False,
                                    'meta': {
                                        'source': DTC.MARKET_IDX
                                    }
                                },
                            ],
                        },
                        'meta': {
                            'rgb': DTC.WHITE
                        }
                    }
                }
            }]
        } for offer in OFFERS]
    }, DatacampMessage()).SerializeToString())

    result = output_topic.read(1)
    assert_that(output_topic, HasNoUnreadData())
    return result


@pytest.mark.parametrize('shop_id', [
    # Офферы магазина со статусом алкоголя SBX не продаем, если нет лицензий
    SHOP_ID_ALCOHOL_STATUS_SBX_NO_LICENSE,
    # Офферы магазина со статусом алкоголя SBX не продаем в боевом индексе, даже если есть лицензии
    SHOP_ID_ALCOHOL_STATUS_SBX_HAS_LICENSE,
    # Офферы магазина со статусом алкоголя REAL не продаем, если нет SUCCESS-лицензий
    SHOP_ID_ALCOHOL_STATUS_REAL_NEW_LICENSE_ONLY,
    # Офферы магазина со статусом алкоголя UNKNOWN не продаем, если нет информаци о лицензиях в alocdata.inc
    SHOP_ID_ALCOHOL_STATUS_UNKNOWN_NO_LEGAL_INFO,
    # Офферы магазина со статусом алкоголя NO не продаем, даже если в аутлетах есть лицензии
    SHOP_ID_ALCOHOL_STATUS_NO_HAS_LICENSE,
], ids=[
    'ALCOHOL_STATUS_SBX_NO_LICENSE',
    'ALCOHOL_STATUS_SBX_HAS_LICENSE',
    'ALCOHOL_STATUS_REAL_NEW_LICENSE_ONLY',
    'ALCOHOL_STATUS_UNKNOWN_NO_LEGAL_INFO',
    'ALCOHOL_STATUS_NO_HAS_LICENSE',
])
def test_alcohol_not_valid(write_read_offer_lbk, shop_id):
    """ Невалидные параметры для алкоголя """
    offer_id = gen_offer_id(shop_id)
    assert_that(write_read_offer_lbk, HasSerializedDatacampMessages([{
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': offer_id,
                    },
                    'content': {
                        'partner': {
                            'original': None,
                            'actual': {
                                'type': {
                                    'value': DTC.ALCO,
                                },
                            },
                        },
                        'market': {
                            'category_id': ALCO_CATEGORY_ID,
                        }
                    }
                },
                'service': IsProtobufMap({
                    shop_id: {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': offer_id,
                            'shop_id': shop_id,
                        },
                        'status': {
                            'disabled': [
                                {
                                    'flag': True,
                                    'meta': {
                                        'source': DTC.MARKET_IDX
                                    }
                                },
                            ],
                        },
                        'meta': {
                            'rgb': DTC.WHITE
                        },
                        'resolution': {
                            'by_source': has_item(IsProtobuf({
                                'meta': {
                                    'source': DTC.MARKET_IDX,
                                },
                                'verdict': [{
                                    'results': [{
                                        'is_banned': True,
                                        'messages': [{
                                            'code': '45Q',  # OE45Q_OFFER_SEEMS_TO_BE_ALCO
                                        }]
                                    }]
                                }]
                            }))
                        }
                    },
                })
            }]
        }]
    }]))


@pytest.mark.parametrize('shop_id', [
    # Офферы магазина со статусом алкоголя SBX продаем на planeshift, если есть лицензии
    SHOP_ID_ALCOHOL_STATUS_SBX_HAS_LICENSE_PS,
    # Офферы магазина со статусом алкоголя SBX продаем на planeshift, даже если есть только NEW-лицензии
    SHOP_ID_ALCOHOL_STATUS_SBX_NEW_LICENSE_ONLY_PS,
    # Офферы магазина со статусом алкоголя REAL продаем, если есть SUCCESS-лицензии
    SHOP_ID_ALCOHOL_STATUS_REAL_HAS_LICENSE,
    # Офферы магазина со статусом алкоголя REAL продаем и на planeshift, если есть SUCCESS-лицензии
    SHOP_ID_ALCOHOL_STATUS_REAL_HAS_LICENSE_PS,
    # Офферы магазина со статусом алкоголя UNKNOWN продаем, если есть информаци о лицензиях в alocdata.inc
    SHOP_ID_ALCOHOL_STATUS_UNKNOWN_HAS_LEGAL_INFO,
], ids=[
    'ALCOHOL_STATUS_SBX_HAS_LICENSE_PS',
    'ALCOHOL_STATUS_SBX_NEW_LICENSE_ONLY_PS',
    'ALCOHOL_STATUS_REAL_HAS_LICENSE',
    'ALCOHOL_STATUS_REAL_HAS_LICENSE_PS',
    'ALCOHOL_STATUS_UNKNOWN_HAS_LEGAL_INFO',
])
def test_alcohol_valid(write_read_offer_lbk, shop_id):
    """ Валидные параметры для алкоголя """
    offer_id = gen_offer_id(shop_id)
    assert_that(write_read_offer_lbk, HasSerializedDatacampMessages([{
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': offer_id,
                    },
                    'content': {
                        'partner': {
                            'original': None,
                            'actual': {
                                'type': {
                                    'value': DTC.ALCO,
                                },
                            },
                        },
                        'market': {
                            'category_id': ALCO_CATEGORY_ID,
                        }
                    }
                },
                'service': IsProtobufMap({
                    shop_id: {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': offer_id,
                            'shop_id': shop_id,
                        },
                        'status': None,
                        'meta': {
                            'rgb': DTC.WHITE
                        },
                        # Не принимаем опции курьерской доставки из фида, а pickup принимаем
                        'delivery': {
                            'partner': {
                                'actual': {
                                    'delivery': {
                                        'flag': False
                                    },
                                    'delivery_options': {
                                        'options': empty()
                                    },
                                    'pickup': {
                                        'flag': True
                                    },
                                    'pickup_options': {
                                        'options': [{
                                            'DaysMin': 1,
                                            'DaysMax': 3,
                                            'Cost': 0,
                                        }]
                                    }
                                }
                            }
                        },
                        'resolution': {
                            'by_source': is_not(has_item(IsProtobuf({
                                'meta': {
                                    'source': DTC.MARKET_IDX,
                                },
                                'verdict': [{
                                    'results': [{
                                        'is_banned': True,
                                        'messages': [{
                                            'code': '45Q',  # OE45Q_OFFER_SEEMS_TO_BE_ALCO
                                        }]
                                    }]
                                }]
                            })))
                        }
                    },
                })
            }]
        }]
    }]))


@pytest.mark.parametrize('shop_id, type, category_id, code', [
    # Скрываем офферы, помеченные как алкогольные, но для которых УК вернул неалкогольную категорию
    # (OE494_OFFER_SEEMS_TO_BE_NON_ALCO)
    (SHOP_ID_ALCOHOL_STATUS_REAL_HAS_LICENSE_NOT_ALCO_CATEGORY, DTC.ALCO, NOT_ALCO_CATEGORY_ID, '494'),
    # Офферы, без указания типа "алкголь", магазина со статусом алкоголя REAL и SUCCESS-лицензиями не продаем, т.к.
    # УК сматчил этот оффер в категорию алкоголя (OE45Q_OFFER_SEEMS_TO_BE_ALCO)
    (SHOP_ID_ALCOHOL_STATUS_REAL_HAS_LICENSE_NOT_ALCO_TYPE, DTC.UNKNOWN_PRODUCT_TYPE, ALCO_CATEGORY_ID, '45Q'),
], ids=[
    'ALCOHOL_STATUS_REAL_HAS_LICENSE_NOT_ALCO_CATEGORY',
    'ALCOHOL_STATUS_REAL_HAS_LICENSE_NOT_ALCO_TYPE',
])
def test_alcohol_category_validation(write_read_offer_lbk, shop_id, type, category_id, code):
    """ Валидация принадлежности алкогольной категории """
    offer_id = gen_offer_id(shop_id)
    assert_that(write_read_offer_lbk, HasSerializedDatacampMessages([{
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': offer_id,
                    },
                    'content': {
                        'partner': {
                            'original': None,
                            'actual': {
                                'type': {
                                    'value': type,
                                },
                            },
                        },
                        'market': {
                            'category_id': category_id,
                        }
                    }
                },
                'service': IsProtobufMap({
                    shop_id: {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': offer_id,
                            'shop_id': shop_id,
                        },
                        'status': {
                            'disabled': [
                                {
                                    'flag': True,
                                    'meta': {
                                        'source': DTC.MARKET_IDX
                                    }
                                },
                            ],
                        },
                        'meta': {
                            'rgb': DTC.WHITE
                        },
                        'resolution': {
                            'by_source': has_item(IsProtobuf({
                                'meta': {
                                    'source': DTC.MARKET_IDX,
                                },
                                'verdict': [{
                                    'results': [{
                                        'is_banned': True,
                                        'messages': [{
                                            'code': code,
                                        }]
                                    }]
                                }]
                            }))
                        }
                    },
                })
            }]
        }]
    }]))
