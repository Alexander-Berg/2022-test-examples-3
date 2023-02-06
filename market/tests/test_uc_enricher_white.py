# coding: utf-8

import pytest
from hamcrest import assert_that
from datetime import datetime, timedelta

from yatest.common.network import PortManager

from market.idx.datacamp.miner.yatf.resources.config import MinerConfig
from market.idx.datacamp.miner.yatf.test_env import MinerTestEnv

from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf, IsProtobuf, IsProtobufMap
from market.idx.yatf.utils.utils import create_timestamp_from_json

from market.idx.datacamp.proto.offer.UnitedOffer_pb2 import UnitedOffersBatch, UnitedOffer
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import (
    OfferIdentifiers,
    Offer as DatacampOffer,
    MARKET_IDX,
    OfferStatus,
    OfferMeta,
    Flag,
    UpdateMeta,
    OfferContent,
    PartnerContent,
    ProcessedSpecification,
    ProductYmlParams,
    OfferYmlParam,
    PriceBundle,
    OfferPrice,
    WHITE,
    ProductType,
    EProductType,
    PartnerContentDescription
)
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
import market.proto.ir.UltraController_pb2 as UC
from market.proto.common.common_pb2 import PriceExpression

from market.idx.feeds.feedparser.yatf.resources.ucdata_pbs import UcHTTPData

SHOP_ID = 1
SHOP_ID_2 = 2
BUSINESS_ID = 1

OFFER_ERROR = {
    'identifiers': {
        'shop_id': SHOP_ID,
        'business_id': BUSINESS_ID,
        'offer_id': '2',
    },
    'uc_data': {
        'offer_problem': [
            {
                'problem_type': UC.OfferProblemType.VENDOR_TAG_NOT_FOUND  # warn
            },
            {
                'problem_type': UC.OfferProblemType.REQUIRED_CATEGORY_PARAM_NOT_FOUND  # error
            }
        ]
    }
}

OFFER_WARN = {
    'identifiers': {
        'shop_id': SHOP_ID,
        'business_id': BUSINESS_ID,
        'offer_id': '22',
    },
    'uc_data': {
        'market_sku_id': 1,
        'offer_problem': [
            {
                'problem_type': UC.OfferProblemType.VENDOR_TAG_NOT_FOUND
            }
        ]
    }
}

OFFER_OK = {
    'identifiers': {
        'shop_id': SHOP_ID,
        'business_id': BUSINESS_ID,
        'offer_id': '222',
    },
    'uc_data': {
        'market_sku_id': 1
    }
}

OFFER_BAD_CURR = {
    'identifiers': {
        'shop_id': SHOP_ID,
        'business_id': BUSINESS_ID,
        'offer_id': 'BAD_CURR',
    },
    'uc_data': {
        'market_sku_id': 1
    },
    'price': 100 * 10 ^ 7,
    'currency': 'BAD_CURR'
}

OFFER_WAS_BAD_STAYED_BAD = {
    'identifiers': {
        'shop_id': SHOP_ID,
        'business_id': BUSINESS_ID,
        'offer_id': '2222',
    },
    'uc_data': {
        'offer_problem': [
            {
                'problem_type': UC.OfferProblemType.VENDOR_TAG_NOT_FOUND  # warn
            },
            {
                'problem_type': UC.OfferProblemType.REQUIRED_CATEGORY_PARAM_NOT_FOUND  # error
            }
        ]
    }
}

OFFERS = [OFFER_ERROR, OFFER_WARN, OFFER_OK, OFFER_WAS_BAD_STAYED_BAD]


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

EXPECTED_MARKET_CONTENT = {
    'category_id': 1009492,
    'dimensions': {
        'weight': 1,
        'height': 1,
        'width': 1,
        'length': 1,
    },
    'market_category': 'category',
    'market_sku_published_on_blue_market': False,
    'market_sku_published_on_market': True,
    'product_name': 'model',
    'vendor_id': 123,
    'ir_data': {
        'classifier_category_id': 1009492,
        'enrich_type': 0,
        'honest_mark_departments': [
            {'name': 'name', 'probability': 1}
        ],
        'matched_id': 11111,
        'probability': 1,
        'skutch_type': 0,
        'classifier_confident_top_percision': 1,
    },
    'vendor_name': "somevendor",
}


def merge_dicts(dict1, dict2):
    final_dict = dict1.copy()
    final_dict.update(dict2)
    return final_dict

# мы обработаем проблемы и навесим ошибки 45W только для офферов без привязки к msku
EXPECTED_BINDING_NO_MSKU = {
    'market_category_name': 'category',
    'market_category_id': 1009492,
    'market_model_name': 'model',
    'market_model_id': 11111,
    'market_sku_name': "sku",
}

EXPECTED_BINDING = merge_dicts(EXPECTED_BINDING_NO_MSKU, {'market_sku_id': 1})


@pytest.yield_fixture(scope='module')
def uc_server():
    with PortManager() as pm:
        port = pm.get_port()
        server = UcHTTPData.from_dict([merge_dicts(UC_DATA_BASE, offer.get('uc_data')) for offer in OFFERS], port=port)
        yield server


@pytest.fixture(scope='module')
def miner_config(log_broker_stuff, input_topic, output_topic, uc_server, yt_server, yt_token):
    cfg = MinerConfig()

    cfg.create_miner_initializer()
    reader = cfg.create_lbk_reader_batch_processor(log_broker_stuff, input_topic, True)
    writer = cfg.create_lbk_writer_processor(log_broker_stuff, output_topic)
    unpacker = cfg.create_proto_unpacker_processor()
    adapter_converter = cfg.create_offer_adapter_converter()
    uc_enricher = cfg.create_uc_enricher_processor(uc_server, enable_uc_requests_deduplicator=True)

    cfg.create_link(reader, unpacker)
    cfg.create_link(unpacker, adapter_converter)
    cfg.create_link(adapter_converter, uc_enricher)
    cfg.create_link(uc_enricher, writer)

    return cfg


@pytest.yield_fixture(scope='module')
def miner(miner_config, input_topic, output_topic, uc_server):
    resources = {
        'miner_cfg': miner_config,
        'input_topic': input_topic,
        'output_topic': output_topic,
        'uc_server': uc_server,
    }
    with MinerTestEnv(**resources) as miner:
        miner.verify()
        yield miner


@pytest.fixture(scope='module')
def united_offers_to_send():
    old_time = (datetime.utcnow() - timedelta(minutes=45)).strftime('%Y-%m-%dT%H:%M:%SZ')
    return [
        # оффер с ошибкой, заодно проверяем на нем отправку offer_params и признака алкогольности в запросе в UC
        DatacampMessage(
            united_offers=[UnitedOffersBatch(
                offer=[UnitedOffer(
                    basic=DatacampOffer(
                        identifiers=OfferIdentifiers(
                            business_id=BUSINESS_ID,
                            offer_id=OFFER_ERROR['identifiers']['offer_id'],
                        ),
                        content=OfferContent(
                            partner=PartnerContent(
                                actual=ProcessedSpecification(  # заполнение остальной части сообщения проверяем в тестах библиотеки
                                    offer_params=ProductYmlParams(
                                        param=[
                                            OfferYmlParam(
                                                name='Цвет',
                                                value='Синий'
                                            ),
                                            OfferYmlParam(
                                                name='Размер',
                                                value='42',
                                                unit='RU'
                                            )
                                        ]
                                    ),
                                    type=ProductType(
                                        value=EProductType.ALCO
                                    )
                                )
                            ),
                            binding=DTC.ContentBinding(
                                mapping_for_uc=DTC.Mapping(
                                    market_sku_id=123,
                                    market_category_id=111,
                                    market_model_id=222
                                )
                            ),
                        ),
                        status=DTC.OfferStatus(
                            version=DTC.VersionStatus(
                                uc_data_version=DTC.VersionCounter(counter=1)
                            )
                        )
                    ),
                    service={
                        SHOP_ID: DatacampOffer(
                            identifiers=OfferIdentifiers(
                                business_id=BUSINESS_ID,
                                shop_id=SHOP_ID,
                                offer_id=OFFER_ERROR['identifiers']['offer_id'],
                            ),
                            price=OfferPrice(
                                basic=PriceBundle(
                                    binary_price=PriceExpression(
                                        price=100 * 10**7,
                                        id='BYN',
                                        ref_id='KZT',
                                        rate='NBK'
                                    )
                                ),
                            ),
                            meta=OfferMeta(rgb=WHITE),
                            status=DTC.OfferStatus(
                                united_catalog=Flag(
                                    flag=False
                                )
                            )
                        )
                    }
                ),
                    UnitedOffer(
                        # оффер с ворнингом
                        basic=DatacampOffer(
                            identifiers=OfferIdentifiers(
                                business_id=BUSINESS_ID,
                                offer_id=OFFER_WARN['identifiers']['offer_id'],
                            ),
                        ),
                        service={
                            SHOP_ID: DatacampOffer(
                                identifiers=OfferIdentifiers(
                                    shop_id=SHOP_ID,
                                    business_id=BUSINESS_ID,
                                    offer_id=OFFER_WARN['identifiers']['offer_id'],
                                ),
                                meta=OfferMeta(rgb=WHITE),
                                status=DTC.OfferStatus()
                            ),
                        }
                    ),
                    UnitedOffer(
                        # оффер, у которого до запроса в UC было скрытие MARKET_IDX, но в новом ответе от UC нет проблем - скрытие
                        # снимем
                        basic=DatacampOffer(
                            identifiers=OfferIdentifiers(
                                business_id=BUSINESS_ID,
                                offer_id=OFFER_OK['identifiers']['offer_id'],
                            ),
                            content=OfferContent(
                                partner=PartnerContent(
                                    partner_content_desc=PartnerContentDescription(
                                        type=13  # alcohol
                                    )
                                ),
                                binding=DTC.ContentBinding(
                                    mapping_for_uc=DTC.Mapping(
                                        market_sku_id=1234,
                                        market_category_id=1111,
                                        market_model_id=2222,
                                        market_sku_type=DTC.Mapping.MarketSkuType.MARKET_SKU_TYPE_MSKU
                                    ),
                                    anti_mapping_for_uc=DTC.AntiMapping(
                                        not_model_id=[2222],
                                        not_sku_id=[1234]
                                    )
                                ),
                            )
                        ),
                        service={
                            SHOP_ID: DatacampOffer(
                                identifiers=OfferIdentifiers(
                                    business_id=BUSINESS_ID,
                                    shop_id=SHOP_ID,
                                    offer_id=OFFER_OK['identifiers']['offer_id'],
                                ),
                                status=OfferStatus(
                                    disabled=[
                                        Flag(
                                            flag=True,
                                            meta=UpdateMeta(
                                                source=MARKET_IDX,
                                                timestamp=create_timestamp_from_json(old_time)
                                            )
                                        )
                                    ],
                                    united_catalog=Flag(
                                        flag=True
                                    )
                                ),
                                meta=OfferMeta(rgb=WHITE)
                            ),
                        }
                    ),
                    UnitedOffer(
                        # оффер с невалидной валютой, проверяем что он не будет отправлен, а остальные - будут
                        basic=DatacampOffer(
                            identifiers=OfferIdentifiers(
                                business_id=BUSINESS_ID,
                                offer_id=OFFER_BAD_CURR['identifiers']['offer_id'],
                            ),
                        ),
                        service={
                            SHOP_ID: DatacampOffer(
                                identifiers=OfferIdentifiers(
                                    shop_id=SHOP_ID,
                                    business_id=BUSINESS_ID,
                                    offer_id=OFFER_BAD_CURR['identifiers']['offer_id'],
                                ),
                                price=OfferPrice(
                                    basic=PriceBundle(
                                        binary_price=PriceExpression(
                                            price=OFFER_BAD_CURR['price'],
                                            id=OFFER_BAD_CURR['currency']
                                        )
                                    )
                                ),
                                meta=OfferMeta(rgb=WHITE),
                                status=DTC.OfferStatus()
                            ),
                        }
                    ),
                    UnitedOffer(
                        # оффер с невалидной валютой, проверяем что он не будет отправлен, а остальные - будут
                        basic=DatacampOffer(
                            identifiers=OfferIdentifiers(
                                business_id=BUSINESS_ID,
                                offer_id=OFFER_BAD_CURR['identifiers']['offer_id'],
                            ),
                        ),
                        service={
                            SHOP_ID: DatacampOffer(
                                identifiers=OfferIdentifiers(
                                    shop_id=SHOP_ID,
                                    business_id=BUSINESS_ID,
                                    offer_id=OFFER_BAD_CURR['identifiers']['offer_id'],
                                ),
                                price=OfferPrice(
                                    basic=PriceBundle(
                                        binary_price=PriceExpression(
                                            price=OFFER_BAD_CURR['price'],
                                            id=OFFER_BAD_CURR['currency']
                                        )
                                    )
                                ),
                                meta=OfferMeta(rgb=WHITE),
                                status=DTC.OfferStatus()
                            ),
                        }
                    ),
                    UnitedOffer(
                        # оффер, у которого до запроса в UC было скрытие MARKET_IDX, в новом ответе UC тоже проблемы - скрытие
                        # сохранится
                        basic=DatacampOffer(
                            identifiers=OfferIdentifiers(
                                business_id=BUSINESS_ID,
                                offer_id=OFFER_WAS_BAD_STAYED_BAD['identifiers']['offer_id'],
                            ),
                        ),
                        service={
                            SHOP_ID: DatacampOffer(
                                identifiers=OfferIdentifiers(
                                    shop_id=SHOP_ID,
                                    business_id=BUSINESS_ID,
                                    offer_id=OFFER_WAS_BAD_STAYED_BAD['identifiers']['offer_id'],
                                ),
                                status=OfferStatus(
                                    disabled=[
                                        Flag(
                                            flag=True,
                                            meta=UpdateMeta(
                                                source=MARKET_IDX,
                                                timestamp=create_timestamp_from_json(old_time)
                                            )
                                        )
                                    ]
                                ),
                                meta=OfferMeta(rgb=WHITE)
                            ),
                        }
                    ),
                ])]),
    ]


@pytest.fixture(scope='module')
def lbk_sender(miner, input_topic, united_offers_to_send):
    for message in united_offers_to_send:
        input_topic.write(message.SerializeToString())


@pytest.fixture(scope='module')
def processed_offers(lbk_sender, miner, output_topic):
    data = output_topic.read(count=1)
    return data


def test_uc_enricher(miner, lbk_sender, output_topic, processed_offers):
    """Проверяем, что
    1. Мы отправляем корректный запрос
    2. Обогащение офера данными от UC корректно работает
    Оффер, для которого от UC возвращается значимая проблема, становится disabled с источником MARKET_IDX
    Оффер, который получает ворнинг - статус не меняется
    Оффер, который раньше был с disabled от MARKET_IDX, а теперь не получил в ответе проблем - перестает быть disabled
    c MARKET_IDX
    """

# ---------------- test part 1
    # наибольший интерес представляет первый оффер в батче - он более наполнен, проверим его
    expected_first_offer_in_batch = [
        {
            'shop_id': 1,
            'shop_offer_id': '2',
            'yml_param': [
                {
                    'name': 'Цвет',
                    'value': 'Синий'
                },
                {
                    'name': 'Размер',
                    'value': '42',
                    'unit': 'RU',
                }
            ],
            'is_sample': False,
            'use_market_dimensions': False,
            'use_cache': False,
            'price': 2899.8272884283247,
            'return_market_names': True,
            'alcohol': True,
            'mapping_for_uc_market_sku_id_from_datacamp': 123,
            'mapping_for_uc_category_id_from_datacamp': 111,
            'mapping_for_uc_model_id_from_datacamp': 222,
        }
    ]

    expected_offer_ok = [
        {
            'is_sample': False,
            'use_market_dimensions': False,
            'use_cache': False,
            'shop_id': 1,
            'shop_offer_id': OFFER_OK['identifiers']['offer_id'],
            'return_market_names': True,
            'alcohol': True,
            'mapping_for_uc_market_sku_id_from_datacamp': 1234,
            'mapping_for_uc_category_id_from_datacamp': 1111,
            'mapping_for_uc_model_id_from_datacamp': 2222,
            'anti_mapping_for_uc_model_id_from_datacamp': [2222],
            'anti_mapping_for_uc_market_sku_id_from_datacamp': [1234],
            'mapping_for_uc_market_sku_type': DTC.Mapping.MarketSkuType.MARKET_SKU_TYPE_MSKU,
        }
    ]

    expected_other_offers = [
        {
            'is_sample': False,
            'use_market_dimensions': False,
            'use_cache': False,
            'shop_id': 1,
            'shop_offer_id': offer['identifiers']['offer_id'],
            'return_market_names': True,
        }
        for offer in [OFFER_WAS_BAD_STAYED_BAD]
    ]

    expected_offers_in_request = {
        'offers': expected_first_offer_in_batch +
            expected_offer_ok +
            expected_other_offers
    }
    requests_to_uc = miner.resources['uc_server'].request

    assert_that(requests_to_uc, IsProtobuf(expected_offers_in_request), 'request to UC has unexpected body')

    # ---------------- test part 2
    assert_that(processed_offers[0], IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [
                {
                    'basic': {
                        'identifiers': {key: value for key, value in list(OFFER_ERROR['identifiers'].items()) if key != 'shop_id'},
                        'content': {
                            'market': merge_dicts(EXPECTED_MARKET_CONTENT, {
                                'enriched_offer': merge_dicts(UC_DATA_BASE, OFFER_ERROR['uc_data']),
                                'real_uc_version': {
                                    'counter': 1
                                }
                            }),
                            'binding': {
                                'uc_mapping': EXPECTED_BINDING_NO_MSKU
                            }
                        },
                    },
                    'service': IsProtobufMap({
                        SHOP_ID: {
                            'identifiers': OFFER_ERROR['identifiers'],
                            'status': {
                                'disabled': [
                                    {
                                        'flag': True,
                                        'meta': {
                                            'source': MARKET_IDX
                                        }
                                    }
                                ]
                            }
                        }
                    })
                },
            ]}]}))

    assert_that(processed_offers[0], IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [
                {
                    'basic': {
                        'identifiers': {key: value for key, value in list(OFFER_WARN['identifiers'].items()) if key != 'shop_id'},
                        'content': {
                            'market': merge_dicts(EXPECTED_MARKET_CONTENT, {
                                'enriched_offer': merge_dicts(UC_DATA_BASE, OFFER_WARN['uc_data'])
                            }),
                            'binding': {
                                'uc_mapping': EXPECTED_BINDING
                            }
                        },
                    },
                    'service': IsProtobufMap({
                        SHOP_ID: {
                            'identifiers': OFFER_WARN['identifiers'],
                        }
                    })
                },
            ]}]}))

    assert_that(processed_offers[0], IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [
                {
                    'basic': {
                        'identifiers': {key: value for key, value in list(OFFER_OK['identifiers'].items()) if key != 'shop_id'},
                        'content': {
                            'market': merge_dicts(EXPECTED_MARKET_CONTENT, {
                                'enriched_offer': UC_DATA_BASE
                            }),
                            'binding': {
                                'uc_mapping': EXPECTED_BINDING
                            }
                        },
                    },
                    'service': IsProtobufMap({
                        SHOP_ID: {
                            'identifiers': OFFER_OK['identifiers'],
                            'status': {
                                'disabled': [
                                    {'flag': False, 'meta': {'source': DTC.MARKET_IDX}}
                                ]
                            }
                        }
                    })
                },
            ]}]}))

    assert_that(processed_offers[0], IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [
                {
                    'basic': {
                        'identifiers': {key: value for key, value in list(OFFER_WAS_BAD_STAYED_BAD['identifiers'].items()) if key != 'shop_id'},
                        'content': {
                            'market': merge_dicts(EXPECTED_MARKET_CONTENT, {
                                'enriched_offer': UC_DATA_BASE
                            }),
                            'binding': {
                                'uc_mapping': EXPECTED_BINDING_NO_MSKU
                            }
                        },
                    },
                    'service': IsProtobufMap({
                        SHOP_ID: {
                            'identifiers': OFFER_WAS_BAD_STAYED_BAD['identifiers'],
                        }
                    })
                },
            ]}]}))

    assert_that(processed_offers[0], IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [
                {
                    'basic': {
                        'identifiers': {key: value for key, value in list(OFFER_BAD_CURR['identifiers'].items()) if key != 'shop_id'},
                        'price': None
                    },
                    'service': IsProtobufMap({
                        SHOP_ID: {
                            'identifiers': OFFER_BAD_CURR['identifiers'],
                        }
                    })
                },
            ]
        }]
    }))
