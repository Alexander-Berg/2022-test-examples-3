import pytest

from hamcrest import assert_that
from market.pylibrary.proto_utils import message_from_data
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf, IsProtobufMap
from market.idx.yatf.matchers.yt_rows_matchers import HasOffers
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.datacamp.proto.offer import DataCampOffer_pb2 as DTC
from market.idx.datacamp.proto.api import OffersBatch_pb2 as OffersBatch
from market.idx.datacamp.yatf.matchers.matchers import HasSerializedDatacampMessages, HasStatus
from market.idx.datacamp.yatf.utils import create_meta, create_update_meta
from market.idx.datacamp.controllers.stroller.yatf.test_env import make_stroller
from market.idx.datacamp.dispatcher.yatf.test_env import DispatcherTestEnv
from market.idx.datacamp.dispatcher.yatf.resources.config import DispatcherConfig
from market.idx.pylibrary.datacamp.conversion import offer_to_service_row, offer_to_basic_row

TIMESTAMP = '2021-08-01T15:55:55Z'
BLOCKED_BUSINESS = 1000


@pytest.fixture(scope='module')
def basic_offers():
    return [
    offer_to_basic_row(DTC.Offer(
        identifiers=DTC.OfferIdentifiers(
            business_id=1,
            offer_id='offer_for_mboc_after_migration',
        ),
        meta=create_meta(10, scope=DTC.BASIC),
        status=DTC.OfferStatus(
            version=DTC.VersionStatus(
                offer_version=DTC.VersionCounter(
                    counter=2
                ),
                uc_data_version=DTC.VersionCounter(
                    counter=1
                ),
            ),
            consistency=DTC.ConsistencyStatus(
                mboc_consistency=True,
                pricelabs_consistency=True,
            )
        ),
        content=DTC.OfferContent(
            market=DTC.MarketContent(
                meta=create_update_meta(10),
                real_uc_version=DTC.VersionCounter(
                    counter=1
                )
            )
        ),
    )),
    offer_to_basic_row(DTC.Offer(
        identifiers=DTC.OfferIdentifiers(
            business_id=1,
            offer_id='offer_for_mboc_after_delete',
        ),
        meta=create_meta(10, scope=DTC.BASIC),
        status=DTC.OfferStatus(
            version=DTC.VersionStatus(
                offer_version=DTC.VersionCounter(
                    counter=3
                ),
                uc_data_version=DTC.VersionCounter(
                    counter=3
                ),
            ),
            consistency=DTC.ConsistencyStatus(
                mboc_consistency=False,
            )
        ),
        content=DTC.OfferContent(
            market=DTC.MarketContent(
                meta=create_update_meta(10),
                real_uc_version=DTC.VersionCounter(
                    counter=1
                )
            )
        ),
    )),
    offer_to_basic_row(DTC.Offer(
        identifiers=DTC.OfferIdentifiers(
            business_id=BLOCKED_BUSINESS,
            offer_id='offer_for_mboc_after_migration_not_consistent',
        ),
        meta=create_meta(10, scope=DTC.BASIC),
        status=DTC.OfferStatus(
            version=DTC.VersionStatus(
                offer_version=DTC.VersionCounter(
                    counter=2
                ),
                uc_data_version=DTC.VersionCounter(
                    counter=1
                ),
            ),
            consistency=DTC.ConsistencyStatus(
                mboc_consistency=True,
                pricelabs_consistency=True,
            )
        ),
        content=DTC.OfferContent(
            market=DTC.MarketContent(
                meta=create_update_meta(10),
                real_uc_version=DTC.VersionCounter(
                    counter=1
                )
            )
        ),
    )),
]


@pytest.fixture(scope='module')
def service_offers():
    return [
    offer_to_service_row(DTC.Offer(
        identifiers=DTC.OfferIdentifiers(
            business_id=1,
            offer_id='offer_for_mboc_after_delete',
            shop_id=2,
        ),
        meta=create_meta(10, color=DTC.BLUE, scope=DTC.SERVICE),
    )),
]


@pytest.fixture(scope='module')
def mboc_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def mboc_regular_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.yield_fixture(scope='module')
def stroller(
        config,
        yt_server,
        log_broker_stuff,
        partners_table,
        basic_offers_table,
        service_offers_table,
        actual_service_offers_table,
):
    with make_stroller(
            config,
            yt_server,
            log_broker_stuff,
            shopsdat_cacher=True,
            partners_table=partners_table,
            basic_offers_table=basic_offers_table,
            service_offers_table=service_offers_table,
            actual_service_offers_table=actual_service_offers_table,
    ) as stroller_env:
        yield stroller_env


@pytest.fixture(scope='module')
def dispatcher_config(
    yt_token,
    yt_server,
    log_broker_stuff,
    basic_offers_table,
    service_offers_table,
    actual_service_offers_table,
    mboc_topic,
    mboc_regular_topic,
    subscription_service_topic,
):
    cfg = DispatcherConfig()
    cfg.create_initializer(yt_server=yt_server, yt_token_path=yt_token.path)

    reader = cfg.create_lb_reader(log_broker_stuff, subscription_service_topic)
    unpacker = cfg.create_subscription_message_unpacker()
    dispatcher = cfg.create_subscription_dispatcher(
        basic_offers_table.table_path, service_offers_table.table_path, actual_service_offers_table.table_path
    )
    cfg.create_link(reader, unpacker)
    cfg.create_link(unpacker, dispatcher)

    mboc_sender = cfg.create_mboc_sender('MBOC_LB_SENDER')
    mboc_lb_writer = cfg.create_lb_writer(log_broker_stuff, mboc_topic)

    cfg.create_link(dispatcher, mboc_sender)
    cfg.create_link(mboc_sender, mboc_lb_writer)

    mboc_regular_sender = cfg.create_mboc_sender('MBOC_LB_SENDER', mode='regular')
    mboc_regular_lb_writer = cfg.create_lb_writer(log_broker_stuff, mboc_regular_topic)

    cfg.create_link(dispatcher, mboc_regular_sender)
    cfg.create_link(mboc_regular_sender, mboc_regular_lb_writer)

    return cfg


@pytest.yield_fixture(scope='module')
def dispatcher(
    dispatcher_config,
    basic_offers_table,
    service_offers_table,
    actual_service_offers_table,
    subscription_service_topic,
    mboc_topic,
    mboc_regular_topic,
):
    resources = {
        'dispatcher_config': dispatcher_config,
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
        'subscription_service_topic': subscription_service_topic,
        'mboc_topic': mboc_topic,
        'mboc_regular_topic': mboc_regular_topic,
    }

    with DispatcherTestEnv(**resources) as env:
        env.verify()
        yield env


def test_sender_to_mboc_after_migration(dispatcher, stroller, mboc_topic, mboc_regular_topic):
    """ Проверка отправки в MBOC после миграции услуги """
    business_id = 1
    shop_id = 2
    offer_id = 'offer_for_mboc_after_migration'

    basic = {
        'identifiers': {
            'business_id': business_id,
            'offer_id': offer_id,
        },
        "status": {
            "consistency": {
                "mboc_consistency": False,
                "pricelabs_consistency": False,
            }
        },
        'content': {
            'market': {
                'meta': {
                    'timestamp': TIMESTAMP
                },
            },
            'partner': {
                'original': {
                    'vendor_code': {
                        'value': '9036',
                        'meta': {
                            'timestamp': TIMESTAMP
                        }
                    }
                }
            }
        },
        'tech_info': {
            'last_mining': {
                'meta': {
                    'timestamp': TIMESTAMP,
                },
                'revision': 123,
            }
        },
    }
    service = {
        'identifiers': {
            'business_id': business_id,
            'offer_id': offer_id,
            'shop_id': shop_id,
        },
        'meta': {
            'ts_created': TIMESTAMP,
        },
    }

    body = message_from_data({
        'entries': [{
            'method': OffersBatch.RequestMethod.POST,
            'offer': {
                'basic': basic,
                'service': {
                    shop_id: service
                }
            }
        }]
    }, OffersBatch.UnitedOffersBatchRequest())

    response = stroller.post('/v1/offers/united/batch', data=body.SerializeToString())
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(OffersBatch.UnitedOffersBatchResponse, {
        'entries': [{
            'united_offer': {
                'basic': {
                    'identifiers': {
                        'business_id': business_id,
                        'offer_id': offer_id,
                    },
                },
                'service': IsProtobufMap({
                    shop_id: {
                        'identifiers': {
                            'business_id': business_id,
                            'offer_id': offer_id,
                            'shop_id': shop_id,
                        },
                    },
                }),
            }
        }]
    }))
    assert_that(stroller.basic_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': business_id,
                'offer_id': offer_id,
            },
            "status": {
                # Консистентность будет true, т.к. в обновлении стоит revision
                "consistency": {
                    "mboc_consistency": True,
                    "pricelabs_consistency": True,
                }
            },
            'tech_info': {
                'last_mining': {
                    'revision': 123,
                }
            },
        }, DTC.Offer())
    ]))
    assert_that(stroller.service_offers_table.data, HasOffers([
        message_from_data(service, DTC.Offer())
    ]))

    expected_data = [{
        'united_offers': [{
            'offer': [
                {
                    'basic': {
                        'identifiers': {
                            'business_id': business_id,
                            'offer_id': offer_id,
                        },
                        "status": {
                            "consistency": {
                                "mboc_consistency": True,
                            }
                        },
                    },
                    'service': IsProtobufMap({
                        shop_id: {}
                    }),
                }
            ]
        }]
    }]
    assert_that(mboc_regular_topic.read(count=1), HasSerializedDatacampMessages(expected_data))
    assert_that(mboc_topic.read(count=1), HasSerializedDatacampMessages(expected_data))


def test_sender_to_mboc_removed_offer(dispatcher, stroller, mboc_topic):
    """ Проверка отправки в MBOC признака удаления сервисного оффера после удаления сервисной части неконсистентного
        оффера """
    business_id = 1
    shop_id = 2
    offer_id = 'offer_for_mboc_after_delete'

    service = {
        'identifiers': {
            'business_id': business_id,
            'offer_id': offer_id,
            'shop_id': shop_id,
        },
        'status': {
            'removed': {
                'flag': True,
                'meta': {
                    'timestamp': TIMESTAMP,
                }
            },
        },
    }

    body = message_from_data({
        'entries': [{
            'method': OffersBatch.RequestMethod.POST,
            'offer': {
                'service': {
                    shop_id: service
                }
            }
        }]
    }, OffersBatch.UnitedOffersBatchRequest())

    response = stroller.post('/v1/offers/united/batch', data=body.SerializeToString())
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(OffersBatch.UnitedOffersBatchResponse, {
        'entries': [{
            'united_offer': {
                'basic': {
                    'identifiers': {
                        'business_id': business_id,
                        'offer_id': offer_id,
                    },
                },
                'service': IsProtobufMap({
                    shop_id: {
                        'identifiers': {
                            'business_id': business_id,
                            'offer_id': offer_id,
                            'shop_id': shop_id,
                        },
                    },
                }),
            }
        }]
    }))
    assert_that(stroller.basic_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': business_id,
                'offer_id': offer_id,
            },
            "status": {
                "consistency": {
                    "mboc_consistency": False,
                }
            },
        }, DTC.Offer())
    ]))
    assert_that(stroller.service_offers_table.data, HasOffers([
        message_from_data(service, DTC.Offer())
    ]))

    data = mboc_topic.read(count=1)
    assert_that(data, HasSerializedDatacampMessages([{
        'united_offers': [{
            'offer': [
                {
                    'basic': {
                        'identifiers': {
                            'business_id': business_id,
                            'offer_id': offer_id,
                        },
                        "status": {
                            "consistency": {
                                "mboc_consistency": False,
                            }
                        },
                    },
                    'service': IsProtobufMap({
                        shop_id: {
                            'identifiers': {
                                'business_id': business_id,
                                'offer_id': offer_id,
                                'shop_id': shop_id,
                            },
                            'status': {
                                'removed': {
                                    'flag': True,
                                },
                            },
                        }
                    }),
                }
            ]
        }]
    }]))


def test_sender_to_mboc_not_consistent_offer_after_migration(stroller, mboc_topic):
    """ Проверка отправки в MBOC во время миграции услуги, приведшей к созданию неконсистентного оффера """
    business_id = BLOCKED_BUSINESS
    shop_id = 2
    offer_id = 'offer_for_mboc_after_migration_not_consistent'

    basic = {
        'identifiers': {
            'business_id': business_id,
            'offer_id': offer_id,
        },
        "status": {
            "consistency": {
                "mboc_consistency": True,
                "pricelabs_consistency": True,
            }
        },
        'content': {
            'market': {
                'meta': {
                    'timestamp': TIMESTAMP
                },
                'real_uc_version': {
                    'counter': 1,
                }
            },
            'partner': {
                'original': {
                    'vendor_code': {
                        'value': '9036',
                        'meta': {
                            'timestamp': TIMESTAMP
                        }
                    }
                }
            }
        },
    }
    service = {
        'identifiers': {
            'business_id': business_id,
            'offer_id': offer_id,
            'shop_id': shop_id,
        },
        'meta': {
            'ts_created': TIMESTAMP,
            'rgb': DTC.WHITE,
        },
    }

    body = message_from_data({
        'entries': [{
            'method': OffersBatch.RequestMethod.POST,
            'offer': {
                'basic': basic,
                'service': {
                    shop_id: service
                }
            }
        }]
    }, OffersBatch.UnitedOffersBatchRequest())

    response = stroller.post('/v1/offers/united/batch', data=body.SerializeToString())
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(OffersBatch.UnitedOffersBatchResponse, {
        'entries': [{
            'united_offer': {
                'basic': {
                    'identifiers': {
                        'business_id': business_id,
                        'offer_id': offer_id,
                    },
                },
                'service': IsProtobufMap({
                    shop_id: {
                        'identifiers': {
                            'business_id': business_id,
                            'offer_id': offer_id,
                            'shop_id': shop_id,
                        },
                    },
                }),
            }
        }]
    }))
    assert_that(stroller.basic_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': business_id,
                'offer_id': offer_id,
            },
            "status": {
                "consistency": {
                    "mboc_consistency": False,
                    "pricelabs_consistency": False,
                }
            },
            'content': {
                'market': {
                    'real_uc_version': {
                        'counter': 1,
                    }
                },
                'partner': {
                    'original': {
                        'vendor_code': {
                            'value': '9036',
                        }
                    }
                }
            },
        }, DTC.Offer())
    ]))
    assert_that(stroller.service_offers_table.data, HasOffers([
        message_from_data(service, DTC.Offer())
    ]))

    data = mboc_topic.read(count=1)
    assert_that(data, HasSerializedDatacampMessages([{
        'united_offers': [{
            'offer': [
                {
                    'basic': {
                        'identifiers': {
                            'business_id': business_id,
                            'offer_id': offer_id,
                        },
                        "status": {
                            "consistency": {
                                "mboc_consistency": False,
                            }
                        },
                    },
                    'service': IsProtobufMap({
                        shop_id: {}
                    }),
                }
            ]
        }]
    }]))
