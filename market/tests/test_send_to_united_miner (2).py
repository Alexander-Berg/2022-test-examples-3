# coding: utf-8

import pytest
from hamcrest import assert_that, has_items

import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
import market.idx.datacamp.proto.subscription.subscription_pb2 as SUBS

from market.idx.datacamp.dispatcher.yatf.resources.config import DispatcherConfig
from market.idx.datacamp.dispatcher.yatf.test_env import DispatcherTestEnv
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.datacamp.yatf.matchers.matchers import IsSerializedProtobuf
from market.idx.yatf.matchers.lb_matchers import HasNoUnreadData
from market.idx.yatf.matchers.protobuf_matchers import IsProtobufMap
from market.pylibrary.proto_utils import message_from_data, proto_path_from_str_path


BUSINESS_ID = 1

SHOP_ID_1 = 11
SHOP_ID_2 = 22

WAREHOUSE_ID_1 = 111
WAREHOUSE_ID_2 = 222

OFFER_REQUIRE_SERVICE_SELECTION = "OfferWithSelect"
OFFER_WITHOUT_SERVICE_SELECTION = "OfferWithoutSelect"


def create_service_with_miner_fields(business_id, shop_id, offer_id, color):
    # Сервисная часть с парочкой полей, на которые подписан майнер
    # Хотим убедиться, что при дозачитывании все поля доедут до майнера
    return DTC.Offer(
        identifiers=DTC.OfferIdentifiers(business_id=business_id, offer_id=offer_id, shop_id=shop_id),
        meta=DTC.OfferMeta(rgb=color),
        price=DTC.OfferPrice(
            basic=DTC.PriceBundle(
                binary_price=DTC.PriceExpression(
                    price=10
                )
            )
        ),
        content=DTC.OfferContent(
            partner=DTC.PartnerContent(
                original_terms=DTC.OriginalTerms(
                    quantity=DTC.Quantity(
                        step=7
                    )
                )
            )
        )
    )


def create_actual_service(business_id, shop_id, warehouse_id, offer_id, color):
    return DTC.Offer(
        identifiers=DTC.OfferIdentifiers(business_id=business_id, offer_id=offer_id, shop_id=shop_id, warehouse_id=warehouse_id),
        meta=DTC.OfferMeta(rgb=color)
    )


@pytest.fixture(scope='module')
def basic_offers_table_data():
    return [
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=BUSINESS_ID, offer_id=OFFER_REQUIRE_SERVICE_SELECTION),
            content=DTC.OfferContent(
                binding=DTC.ContentBinding(
                    partner_mapping_moderation=DTC.PartnerMappingModeration(
                        remapping_result=DTC.RemappingResult(
                            value=DTC.RemappingResult.Result.NOT_CHECKED  # miner_full and miner trigger
                        )
                    )
                )
            )
        ),
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=BUSINESS_ID, offer_id=OFFER_WITHOUT_SERVICE_SELECTION),
            content=DTC.OfferContent(
                partner=DTC.PartnerContent(
                    original=DTC.OriginalSpecification(
                        type_prefix=DTC.StringValue(
                            value='some_prefix'  # miner trigger
                        )
                    )
                )
            ),
        ),
    ]


@pytest.fixture(scope='module')
def service_offers_table_data():
    return [
        create_service_with_miner_fields(
            business_id=BUSINESS_ID,
            shop_id=SHOP_ID_1,
            offer_id=offer_id,
            color=DTC.BLUE) for offer_id in (OFFER_REQUIRE_SERVICE_SELECTION, OFFER_WITHOUT_SERVICE_SELECTION)
    ] + [
        create_service_with_miner_fields(
            business_id=BUSINESS_ID,
            shop_id=SHOP_ID_2,
            offer_id=offer_id,
            color=DTC.WHITE) for offer_id in (OFFER_REQUIRE_SERVICE_SELECTION, OFFER_WITHOUT_SERVICE_SELECTION)
    ]


@pytest.fixture(scope='module')
def actual_service_offers_table_data():
    return [
        create_actual_service(
            business_id=BUSINESS_ID,
            shop_id=SHOP_ID_1,
            warehouse_id=WAREHOUSE_ID_1,
            offer_id=offer_id,
            color=DTC.BLUE) for offer_id in (OFFER_REQUIRE_SERVICE_SELECTION, OFFER_WITHOUT_SERVICE_SELECTION)
    ] + [
        create_actual_service(
            business_id=BUSINESS_ID,
            shop_id=SHOP_ID_2,
            warehouse_id=WAREHOUSE_ID_2,
            offer_id=offer_id,
            color=DTC.WHITE) for offer_id in (OFFER_REQUIRE_SERVICE_SELECTION, OFFER_WITHOUT_SERVICE_SELECTION)
    ]


SUBSCRIPTION_MESSAGES = [
    # Апдейт поля в базовой части, на котором стоит триггеры miner_full_subscriber и miner_subscriber
    # -> должны для него отправить все сервисные
    message_from_data({
        'subscription_request': [
            {
                'business_id': BUSINESS_ID,
                'offer_id': OFFER_REQUIRE_SERVICE_SELECTION,
                'basic': {
                    'updated_fields': [
                        proto_path_from_str_path(DTC.Offer, 'content.binding.partner_mapping_moderation.remapping_result')
                    ],
                }
            }
        ]
    }, SUBS.UnitedOffersSubscriptionMessage()),
    # Апдейт полей в базовой и сервисной частях, на которых стоит только триггер miner_subscriber
    # -> для него должны отправить только одну сервисную часть
    message_from_data({
        'subscription_request': [
            {
                'business_id': BUSINESS_ID,
                'offer_id': OFFER_WITHOUT_SERVICE_SELECTION,
                'basic': {
                    'updated_fields': [
                        proto_path_from_str_path(DTC.Offer, 'content.partner.original')
                    ],
                },
                'service': {
                    SHOP_ID_1: {
                        'updated_fields': {
                            'updated_fields': [
                                proto_path_from_str_path(DTC.Offer, 'content.partner.original_terms'),
                            ]
                        }
                    }
                }
            }
        ]
    }, SUBS.UnitedOffersSubscriptionMessage()),
]


@pytest.fixture(scope='module')
def dispatcher_config(
        yt_token,
        yt_server,
        log_broker_stuff,
        input_topic,
        output_topic,
        basic_offers_table,
        service_offers_table,
        actual_service_offers_table
):
    cfg = DispatcherConfig()
    cfg.create_initializer(
        yt_server=yt_server,
        yt_token_path=yt_token.path
    )

    lb_reader = cfg.create_lb_reader(log_broker_stuff, input_topic)
    # Оставляю для селекта сервисных частей только MINER_FULL, чтобы другие подписчики не могли повлиять на результат
    # Пустая строка в service_select_on_empty_subscribers не добавлятеся (не парсится конфиг),
    # чтобы не менять парсинг конфига добавлю PARTNER_STOCK,
    # в тесте не участвуют поля для этого подписчика - на дозачитывании это не отразится
    unpacker = cfg.create_subscription_message_unpacker(
        service_select_subscribers='MINER_FULL',
        service_select_on_empty_subscribers='PARTNER_STOCK'
    )
    dispatcher = cfg.create_subscription_dispatcher(
        basic_offers_table.table_path,
        service_offers_table.table_path,
        actual_service_offers_table.table_path
    )
    filter = cfg.create_subscription_filter('MINER_SUBSCRIBER', extra_params={
        'Color': 'UNKNOWN_COLOR;WHITE;BLUE',
        'Mode': 'MIXED',
        'AcceptRelaxed': True
    })
    sender = cfg.create_subscription_sender()
    lb_writer = cfg.create_lb_writer(log_broker_stuff, output_topic)

    cfg.create_link(lb_reader, unpacker)
    cfg.create_link(unpacker, dispatcher)
    cfg.create_link(dispatcher, filter)
    cfg.create_link(filter, sender)
    cfg.create_link(sender, lb_writer)

    return cfg


@pytest.yield_fixture(scope='module')
def dispatcher(
        dispatcher_config,
        basic_offers_table,
        service_offers_table,
        actual_service_offers_table,
        input_topic,
        output_topic,
):
    resources = {
        'dispatcher_config': dispatcher_config,
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
        'input_topic': input_topic,
        'output_topic': output_topic,
    }
    with DispatcherTestEnv(**resources) as env:
        env.verify()
        yield env


def test_filtering(dispatcher, input_topic, output_topic):
    for message in SUBSCRIPTION_MESSAGES:
        input_topic.write(message.SerializeToString())

    wait_until(lambda: dispatcher.subscription_dispatcher_processed >= len(SUBSCRIPTION_MESSAGES), timeout=10)

    sent_data = output_topic.read(count=2)

    assert_that(sent_data, has_items(*[
        IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [{
                    'basic': {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': OFFER_REQUIRE_SERVICE_SELECTION,
                        },
                        'content': {
                            'binding': {
                                'partner_mapping_moderation': {
                                    'remapping_result': {
                                        'value': DTC.RemappingResult.Result.NOT_CHECKED
                                    }
                                }
                            }
                        }
                    },
                    'service': IsProtobufMap({
                        shop_id: {
                            'identifiers': {
                                'shop_id': shop_id,
                                'business_id': BUSINESS_ID,
                                'offer_id': OFFER_REQUIRE_SERVICE_SELECTION,
                            },
                            'content': {
                                'partner': {
                                    'original_terms': {
                                        'quantity': {
                                            'step': 7
                                        }
                                    }
                                }
                            },
                            'price': {
                                'basic': {
                                    'binary_price': {
                                        'price': 10
                                    }
                                }
                            }
                        },
                    })
                }]
            } for shop_id in (SHOP_ID_1, SHOP_ID_2)]
        }),
        IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [{
                    'basic': {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': OFFER_WITHOUT_SERVICE_SELECTION,
                        },
                        'content': {
                            'partner': {
                                'original': {
                                    'type_prefix': {
                                        'value': 'some_prefix'
                                    }
                                }
                            }
                        }
                    },
                    'service': IsProtobufMap({
                        SHOP_ID_1: {
                            'identifiers': {
                                'shop_id': SHOP_ID_1,
                                'business_id': BUSINESS_ID,
                                'offer_id': OFFER_WITHOUT_SERVICE_SELECTION,
                            },
                            'content': {
                                'partner': {
                                    'original_terms': {
                                        'quantity': {
                                            'step': 7
                                        }
                                    }
                                }
                            },
                            'price': {
                                'basic': {
                                    'binary_price': {
                                        'price': 10
                                    }
                                }
                            }
                        },
                    }),
                }]
            }]
        }),
    ]))

    # Проверяем, что топик опустел
    assert_that(output_topic, HasNoUnreadData())
