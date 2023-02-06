# coding: utf-8

import pytest
from hamcrest import assert_that, has_items, is_not

import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
import market.idx.datacamp.proto.subscription.subscription_pb2 as SUBS

from market.idx.datacamp.dispatcher.yatf.resources.config import DispatcherConfig
from market.idx.datacamp.dispatcher.yatf.test_env import DispatcherTestEnv
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.datacamp.yatf.matchers.matchers import IsSerializedProtobuf
from market.idx.yatf.matchers.lb_matchers import HasNoUnreadData
from market.idx.yatf.matchers.protobuf_matchers import IsProtobufMap
from market.idx.datacamp.yatf.utils import create_update_meta
from market.pylibrary.proto_utils import message_from_data, proto_path_from_str_path


BUSINESS_ID = 1
# бизнес с не-маркертными магазинами
BUSINESS_ID_NON_MARKET = 2
# синий магазин
BLUE_SHOP_ID = 2
# не-маркетный магазин (директ)
DIRECT_SHOP_ID = 3
# ADV
ADV_SHOP_ID = 4
# DBS
DBS_SHOP_ID = 5


@pytest.fixture(scope='module')
def basic_offers_table_data():
    return [
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=BUSINESS_ID, offer_id='T1000'),
            content=DTC.OfferContent(
                partner=DTC.PartnerContent(
                    original=DTC.OriginalSpecification(
                        expiry=DTC.Expiration(
                            validity_period=DTC.Duration(
                                years=3
                            ),
                            meta=create_update_meta(10),
                        )
                    )
                )
            ),
            status=DTC.OfferStatus(
                version=DTC.VersionStatus(
                    master_data_version=DTC.VersionCounter(
                        counter=3,
                    )
                )
            )
        ),
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=BUSINESS_ID_NON_MARKET, offer_id='T2000'),
            content=DTC.OfferContent(
                partner=DTC.PartnerContent(
                    original=DTC.OriginalSpecification(
                        expiry=DTC.Expiration(
                            validity_period=DTC.Duration(
                                years=2
                            ),
                            meta=create_update_meta(10),
                        )
                    )
                )
            ),
        ),
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=BUSINESS_ID, offer_id='T3000'),
            content=DTC.OfferContent(
                partner=DTC.PartnerContent(
                    original=DTC.OriginalSpecification(
                        expiry=DTC.Expiration(
                            validity_period=DTC.Duration(
                                years=3
                            ),
                            meta=create_update_meta(10),
                        )
                    )
                )
            ),
        ),
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=BUSINESS_ID, offer_id='offer_only_basic'),
            content=DTC.OfferContent(
                partner=DTC.PartnerContent(
                    original=DTC.OriginalSpecification(
                        expiry=DTC.Expiration(
                            validity_period=DTC.Duration(
                                years=3
                            ),
                            meta=create_update_meta(10),
                        )
                    )
                )
            ),
        ),
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=BUSINESS_ID, offer_id='offer_only_basic_business_catalog'),
            meta=DTC.OfferMeta(
                business_catalog=DTC.Flag(
                    flag=True
                )
            ),
            content=DTC.OfferContent(
                partner=DTC.PartnerContent(
                    original=DTC.OriginalSpecification(
                        expiry=DTC.Expiration(
                            validity_period=DTC.Duration(
                                years=3
                            ),
                            meta=create_update_meta(10),
                        )
                    )
                )
            ),
        ),
    ]


@pytest.fixture(scope='module')
def service_offers_table_data():
    return [
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=BUSINESS_ID, offer_id=offer_id, shop_id=BLUE_SHOP_ID),
            meta=DTC.OfferMeta(rgb=DTC.BLUE),
            status=DTC.OfferStatus(
                version=DTC.VersionStatus(
                    master_data_version=DTC.VersionCounter(
                        counter=2,
                    )
                )
            )
        ) for offer_id in ('T1000',)
    ] + [
        # Директ-оффер - не нужно слать в MDM сообщения для не-маркетных офферов
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=BUSINESS_ID_NON_MARKET, offer_id=offer_id, shop_id=DIRECT_SHOP_ID),
            meta=DTC.OfferMeta(rgb=DTC.DIRECT)
        ) for offer_id in ('T2000',)
    ] + [
        # ADV - не нужно слать в MDM (by ColorFilter)
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=BUSINESS_ID, offer_id=offer_id, shop_id=ADV_SHOP_ID),
            meta=DTC.OfferMeta(rgb=DTC.WHITE)
        ) for offer_id in ('T3000',)
    ] + [
        # DBS - нужно слать в MDM (by ColorFilter)
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=BUSINESS_ID, offer_id=offer_id, shop_id=DBS_SHOP_ID),
            meta=DTC.OfferMeta(rgb=DTC.WHITE),
            partner_info=DTC.PartnerInfo(is_dsbs=True),
        ) for offer_id in ('T3000',)
    ]


@pytest.fixture(scope='module')
def actual_service_offers_table_data():
    return [
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=BUSINESS_ID, offer_id=offer_id, shop_id=BLUE_SHOP_ID),
            meta=DTC.OfferMeta(rgb=DTC.BLUE),
            status=DTC.OfferStatus(
                version=DTC.VersionStatus(
                    master_data_version=DTC.VersionCounter(
                        counter=1,
                    )
                )
            )
        ) for offer_id in ('T1000',)
    ] + [
        # Директ-оффер
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=BUSINESS_ID_NON_MARKET, offer_id=offer_id, shop_id=DIRECT_SHOP_ID),
            meta=DTC.OfferMeta(rgb=DTC.DIRECT)
        ) for offer_id in ('T2000',)
    ] + [
        # ADV оффер
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=BUSINESS_ID, offer_id=offer_id, shop_id=ADV_SHOP_ID),
            meta=DTC.OfferMeta(rgb=DTC.WHITE)
        ) for offer_id in ('T3000',)
    ] + [
        # DBS - нужно слать в MDM (by ColorFilter)
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=BUSINESS_ID, offer_id=offer_id, shop_id=DBS_SHOP_ID),
            meta=DTC.OfferMeta(rgb=DTC.WHITE),
            partner_info=DTC.PartnerInfo(is_dsbs=True),
        ) for offer_id in ('T3000',)
    ]


SUBSCRIPTION_MESSAGES = [
    message_from_data({
        'subscription_request': [
            {
                'business_id': BUSINESS_ID,
                'offer_id': 'T1000',
                'basic': {
                    'updated_fields': [
                        proto_path_from_str_path(DTC.Offer, 'content.partner.original.expiry')
                    ],
                }
            }
        ]
    }, SUBS.UnitedOffersSubscriptionMessage()),
    message_from_data({
        'subscription_request': [
            {
                'business_id': BUSINESS_ID_NON_MARKET,
                'offer_id': 'T2000',
                'basic': {
                    'updated_fields': [
                        proto_path_from_str_path(DTC.Offer, 'content.partner.original.expiry')
                    ],
                }
            }
        ]
    }, SUBS.UnitedOffersSubscriptionMessage()),
    message_from_data({
        'subscription_request': [
            {
                'business_id': BUSINESS_ID,
                'offer_id': 'T3000',
                'basic': {
                    'updated_fields': [
                        proto_path_from_str_path(DTC.Offer, 'content.partner.original.expiry')
                    ],
                }
            }
        ]
    }, SUBS.UnitedOffersSubscriptionMessage()),
    message_from_data({
        'subscription_request': [
            {
                'business_id': BUSINESS_ID,
                'offer_id': 'offer_only_basic',
                'basic': {
                    'updated_fields': [
                        proto_path_from_str_path(DTC.Offer, 'content.partner.original.expiry')
                    ],
                }
            }
        ]
    }, SUBS.UnitedOffersSubscriptionMessage()),
    message_from_data({
        'subscription_request': [
            {
                'business_id': BUSINESS_ID,
                'offer_id': 'offer_only_basic_business_catalog',
                'basic': {
                    'updated_fields': [
                        proto_path_from_str_path(DTC.Offer, 'content.partner.original.expiry')
                    ],
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
    unpacker = cfg.create_subscription_message_unpacker()
    dispatcher = cfg.create_subscription_dispatcher(
        basic_offers_table.table_path,
        service_offers_table.table_path,
        actual_service_offers_table.table_path
    )
    filter = cfg.create_subscription_filter('MDM_SUBSCRIBER', extra_params={
        'Color': 'BLUE;WHITE:dbs',
        'UseActualServiceFields': False,
        'Mode': 'MIXED',
        'SendMarketOnlyBasicOffers': True
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

    sent_data = output_topic.read(count=3)
    assert_that(sent_data, has_items(*[
        IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [{
                    'basic': {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': 'T1000',
                        },
                        'content': {
                            'partner': {
                                'original': {
                                    'expiry': {
                                        'validity_period': {
                                            'years': 3,
                                        }
                                    }
                                }
                            }
                        },
                        'status': {
                            'version': {
                                'master_data_version': {
                                    'counter': 3
                                }
                            }
                        }
                    },
                    'service': IsProtobufMap({
                        BLUE_SHOP_ID: {
                            'identifiers': {
                                'shop_id': BLUE_SHOP_ID,
                                'business_id': BUSINESS_ID,
                                'offer_id': 'T1000',
                            },
                            'status': {
                                'version': {
                                    'master_data_version': {
                                        'counter': 2
                                    }
                                }
                            }
                        },
                    })
                }]
            }]
        }),
        IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [{
                    'basic': {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': 'T3000',
                        },
                    },
                    'service': IsProtobufMap({
                        DBS_SHOP_ID: {
                            'identifiers': {
                                'shop_id': DBS_SHOP_ID,
                                'business_id': BUSINESS_ID,
                                'offer_id': 'T3000',
                            },
                        },
                    }),
                }]
            }]
        }),
        IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [{
                    'basic': {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': 'offer_only_basic_business_catalog',
                        },
                    },
                }]
            }]
        }),
    ]))

    assert_that(sent_data, is_not(has_items(*[
        IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [{
                    'basic': {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': 'T3000',
                        },
                    },
                    'service': IsProtobufMap({
                        ADV_SHOP_ID: {
                            'identifiers': {
                                'shop_id': ADV_SHOP_ID,
                                'business_id': BUSINESS_ID,
                                'offer_id': 'T3000',
                            },
                        },
                    }),
                }]
            }]
        })
    ])))

    # Проверяем, что топик опустел, сообщение для не-маркетного оффера (по expiry) не отправлено
    assert_that(output_topic, HasNoUnreadData())
