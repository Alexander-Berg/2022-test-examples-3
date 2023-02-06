# coding: utf-8

from hamcrest import assert_that, greater_than, not_, empty, has_item
import pytest
from datetime import datetime

import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock
from market.idx.datacamp.dispatcher.yatf.test_env import DispatcherTestEnv
from market.idx.datacamp.dispatcher.yatf.resources.config import DispatcherConfig
from market.idx.datacamp.yatf.utils import create_meta
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.resources.datacamp.datacamp_tables import (
    DataCampBasicOffersTable,
    DataCampServiceOffersTable,
)
from market.idx.yatf.matchers.yt_rows_matchers import HasOffers, deserialize_united_row
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf, IsProtobufMap
from market.pylibrary.proto_utils import message_from_data
from market.idx.pylibrary.datacamp.conversion import offer_to_basic_row, offer_to_service_row

BUSINESS_ID = 1
SHOP_ID = 2

NOW_UTC = datetime.utcnow()  # JSON serializer should always use UTC
time_pattern = "%Y-%m-%dT%H:%M:%SZ"
current_ts = NOW_UTC.strftime(time_pattern)

BUSINESS_CATALOG_BUSINESS_ID = 333
NOT_BUSINESS_CATALOG_BUSINESS_ID = 666
ADD_SERVICE_PART_BUSINESS_ID = 800
REMOVE_SERVICE_PART_BUSINESS_ID = 801
DECLINE_REMOVE_SERVICE_PART_BUSINESS_ID = 802

ADD_SERVICE_PART_SHOP_ID = 880
REMOVE_SERVICE_PART_SHOP_ID = 881
DECLINE_REMOVE_SERVICE_PART_SHOP_ID = 882

NOT_IN_BUSINESS_CATALOG_OFFER_ID = "NOT_IN_BUSINESS_CATALOG_OFFER_ID"
CONSISTENT_BUSINESS_CATALOG_OFFER_ID = "CONSISTENT_BUSINESS_CATALOG_OFFER"
NOT_CONSISTENT_BUSINESS_CATALOG_OFFER_ID = "NOT_CONSISTENT_BUSINESS_CATALOG_OFFER_ID"
ADD_SERVICE_PART_OFFER_ID = "ADD_SERVICE_PART_OFFER_ID"
REMOVE_SERVICE_PART_OFFER_ID = "REMOVE_SERVICE_PART_OFFER_ID"
DECLINE_REMOVE_SERVICE_PART_OFFER_ID = "DECLINE_REMOVE_SERVICE_PART_OFFER_ID"

DATACAMP_MESSAGES = [
    {
        'basic': {
            'identifiers': {
                'business_id': BUSINESS_ID,
                'offer_id': 'T1000',
            },
            'content': {
                'partner': {
                    'original': {
                        'country_of_origin': {
                            'value': ['Россия'],
                            'meta': {
                                'timestamp': current_ts,
                                'source': DTC.PUSH_PARTNER_OFFICE,
                            },
                        },
                    },
                }
            }
        },
    },
    {
        'basic': {
            'identifiers': {
                'business_id': BUSINESS_ID,
                'offer_id': 'T2000',
            },
            'content': {
                'partner': {
                    'actual': {
                        'country_of_origin_id': {
                            'meta': {
                                'timestamp': current_ts,
                            },
                            'value': [
                                225
                            ]
                        },
                    },
                },
                'market': {
                    'meta': {
                        'timestamp': current_ts,
                    },
                    'real_uc_version': {
                        'counter': 2
                    },
                },
            },
        }
    },
    {
        'basic': {
            'identifiers': {
                'business_id': BUSINESS_ID,
                'offer_id': 'T3000',
            },
            'content': {
                'partner': {
                    'actual': {
                        'description': {
                            'value': 'description',
                            'meta': {
                                'timestamp': current_ts
                            }
                        }
                    }
                }
            },
        },
    },
    {
        'basic': {
            'identifiers': {
                'business_id': BUSINESS_ID,
                'offer_id': 'T4000',
            },
            'pictures': {
                'partner': {
                    'actual': {
                        'picurl': {
                            'status': DTC.MarketPicture.Status.FAILED,
                            'meta': {
                                'timestamp': current_ts
                            }
                        }
                    },
                },
            },
        },
    },
    # Консистентный оффер из ассортиментного каталога под бизнесом без сервисной части - должны отправлять
    {
        'basic': {
            'identifiers': {
                'business_id': BUSINESS_CATALOG_BUSINESS_ID,
                'offer_id': CONSISTENT_BUSINESS_CATALOG_OFFER_ID,
            },
            'content': {
                'partner': {
                    'actual': {
                        'description': {
                            'value': 'description',
                            'meta': {
                                'timestamp': current_ts
                            }
                        }
                    }
                }
            },
        },
    },
    # Оффер не из каталога под бизнесом без сервисной части - не должны отправлять (пока нет списка цветов в базовой части, не шлём такое)
    {
        'basic': {
            'identifiers': {
                'business_id': NOT_BUSINESS_CATALOG_BUSINESS_ID,
                'offer_id': NOT_IN_BUSINESS_CATALOG_OFFER_ID,
            },
            'content': {
                'partner': {
                    'actual': {
                        'description': {
                            'value': 'description',
                            'meta': {
                                'timestamp': current_ts
                            }
                        }
                    }
                }
            },
        },
    },
    # Неконсистентный оффер из ассортиментного каталога под бизнесом без сервисной части - не должны отправлять
    {
        'basic': {
            'identifiers': {
                'business_id': BUSINESS_CATALOG_BUSINESS_ID,
                'offer_id': NOT_CONSISTENT_BUSINESS_CATALOG_OFFER_ID,
            },
            'content': {
                'partner': {
                    'original': {
                        'country_of_origin': {
                            'value': ['Россия'],
                            'meta': {
                                'timestamp': current_ts,
                                'source': DTC.PUSH_PARTNER_OFFICE,
                            },
                        },
                    },
                }
            },
        },
    },
    {
        'basic': {
            'identifiers': {
                'business_id': ADD_SERVICE_PART_BUSINESS_ID,
                'offer_id': ADD_SERVICE_PART_OFFER_ID,
            },
        },
        'service': {
            ADD_SERVICE_PART_SHOP_ID: {
                'identifiers': {
                    'business_id': ADD_SERVICE_PART_BUSINESS_ID,
                    'offer_id': ADD_SERVICE_PART_OFFER_ID,
                    'shop_id': ADD_SERVICE_PART_SHOP_ID,
                },
                'price': {
                    'basic': {
                        'meta': {
                            'timestamp': current_ts,
                            'source': DTC.PUSH_PARTNER_FEED,
                        },
                    },
                },
            },
        },
    },
    {
        'basic': {
            'identifiers': {
                'business_id': REMOVE_SERVICE_PART_BUSINESS_ID,
                'offer_id': REMOVE_SERVICE_PART_OFFER_ID,
            },
        },
        'service': {
            REMOVE_SERVICE_PART_SHOP_ID: {
                'identifiers': {
                    'business_id': REMOVE_SERVICE_PART_BUSINESS_ID,
                    'offer_id': REMOVE_SERVICE_PART_OFFER_ID,
                    'shop_id': REMOVE_SERVICE_PART_SHOP_ID,
                },
                'status': {
                    'removed': {
                        'flag': True,
                        'meta': {
                            'timestamp': current_ts,
                            'source': DTC.PUSH_PARTNER_FEED,
                        },
                    },
                },
            },
        },
    },
    {
        'basic': {
            'identifiers': {
                'business_id': DECLINE_REMOVE_SERVICE_PART_BUSINESS_ID,
                'offer_id': REMOVE_SERVICE_PART_OFFER_ID,
            },
        },
        'service': {
            DECLINE_REMOVE_SERVICE_PART_SHOP_ID: {
                'identifiers': {
                    'business_id': DECLINE_REMOVE_SERVICE_PART_BUSINESS_ID,
                    'offer_id': DECLINE_REMOVE_SERVICE_PART_OFFER_ID,
                    'shop_id': DECLINE_REMOVE_SERVICE_PART_SHOP_ID,
                },
                'status': {
                    'removed': {
                        'flag': False,
                        'meta': {
                            'timestamp': current_ts,
                            'source': DTC.PUSH_PARTNER_FEED,
                        },
                    },
                },
            },
        },
    },
]


@pytest.fixture(scope='module')
def basic_offers_table(yt_server, piper_config):
    return DataCampBasicOffersTable(yt_server, piper_config.yt_basic_offers_tablepath, data=[
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id='T1000',
            ),
            status=DTC.OfferStatus(
                consistency=DTC.ConsistencyStatus(
                    mboc_consistency=True,
                ),
                version=DTC.VersionStatus(
                    uc_data_version=DTC.VersionCounter(
                        counter=1,
                    ),
                )
            ),
            content=DTC.OfferContent(
                market=DTC.MarketContent(
                    real_uc_version=DTC.VersionCounter(
                        counter=1,
                    ),
                ),
                partner=DTC.PartnerContent(
                    original=DTC.OriginalSpecification(
                        country_of_origin=DTC.StringListValue(
                            value=[
                                'Китай',
                            ]
                        ),
                    ),
                )
            )
        )),
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id='T2000',
            ),
            status=DTC.OfferStatus(
                consistency=DTC.ConsistencyStatus(
                    mboc_consistency=False,
                ),
                version=DTC.VersionStatus(
                    uc_data_version=DTC.VersionCounter(
                        counter=2,
                    ),
                )
            ),
            content=DTC.OfferContent(
                market=DTC.MarketContent(
                    real_uc_version=DTC.VersionCounter(
                        counter=1,
                    ),
                ),
                partner=DTC.PartnerContent(
                    actual=DTC.ProcessedSpecification(
                        country_of_origin_id=DTC.I64ListValue(
                            value=[
                                134,
                            ]
                        ),
                    ),
                )
            )
        )),
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id='T3000',
            ),
            content=DTC.OfferContent(
                market=DTC.MarketContent(
                    real_uc_version=DTC.VersionCounter(
                        counter=1
                    )
                )
            ),
            status=DTC.OfferStatus(
                version=DTC.VersionStatus(
                    uc_data_version=DTC.VersionCounter(
                        counter=1
                    ),
                ),
            ),
        )),
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id='T4000',
            ),
            content=DTC.OfferContent(
                market=DTC.MarketContent(
                    real_uc_version=DTC.VersionCounter(
                        counter=1
                    )
                )
            ),
            status=DTC.OfferStatus(
                version=DTC.VersionStatus(
                    uc_data_version=DTC.VersionCounter(
                        counter=1
                    ),
                ),
            ),
            pictures=DTC.OfferPictures(
                partner=DTC.PartnerPictures(
                    original=DTC.SourcePictures(
                        source=[DTC.SourcePicture(
                            url='picurl'
                        )]
                    ),
                    actual={
                        'picurl': DTC.MarketPicture(
                            status=DTC.MarketPicture.Status.AVAILABLE
                        )
                    }
                )
            )
        )),
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_CATALOG_BUSINESS_ID,
                offer_id=CONSISTENT_BUSINESS_CATALOG_OFFER_ID,
            ),
            meta=DTC.OfferMeta(
                business_catalog=DTC.Flag(
                    flag=True,
                ),
            ),
            content=DTC.OfferContent(
                market=DTC.MarketContent(
                    real_uc_version=DTC.VersionCounter(
                        counter=1
                    )
                )
            ),
            status=DTC.OfferStatus(
                version=DTC.VersionStatus(
                    uc_data_version=DTC.VersionCounter(
                        counter=1
                    ),
                ),
            ),
        )), offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=NOT_BUSINESS_CATALOG_BUSINESS_ID,
                offer_id=NOT_IN_BUSINESS_CATALOG_OFFER_ID,
            ),
            content=DTC.OfferContent(
                market=DTC.MarketContent(
                    real_uc_version=DTC.VersionCounter(
                        counter=1
                    )
                )
            ),
            status=DTC.OfferStatus(
                version=DTC.VersionStatus(
                    uc_data_version=DTC.VersionCounter(
                        counter=1
                    ),
                ),
            ),
        )), offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_CATALOG_BUSINESS_ID,
                offer_id=NOT_CONSISTENT_BUSINESS_CATALOG_OFFER_ID,
            ),
            meta=DTC.OfferMeta(
                business_catalog=DTC.Flag(
                    flag=True,
                ),
            ),
            status=DTC.OfferStatus(
                consistency=DTC.ConsistencyStatus(
                    mboc_consistency=True,
                ),
                version=DTC.VersionStatus(
                    uc_data_version=DTC.VersionCounter(
                        counter=1,
                    ),
                )
            ),
            content=DTC.OfferContent(
                market=DTC.MarketContent(
                    real_uc_version=DTC.VersionCounter(
                        counter=1,
                    ),
                ),
                partner=DTC.PartnerContent(
                    original=DTC.OriginalSpecification(
                        country_of_origin=DTC.StringListValue(
                            value=[
                                'Китай',
                            ]
                        ),
                    ),
                )
            )
        )), offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=ADD_SERVICE_PART_BUSINESS_ID,
                offer_id=ADD_SERVICE_PART_OFFER_ID,
            ),
            content=DTC.OfferContent(
                market=DTC.MarketContent(
                    real_uc_version=DTC.VersionCounter(
                        counter=1
                    )
                )
            ),
            status=DTC.OfferStatus(
                version=DTC.VersionStatus(
                    uc_data_version=DTC.VersionCounter(
                        counter=1
                    ),
                ),
            ),
        )), offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=REMOVE_SERVICE_PART_BUSINESS_ID,
                offer_id=REMOVE_SERVICE_PART_OFFER_ID,
            ),
            content=DTC.OfferContent(
                market=DTC.MarketContent(
                    real_uc_version=DTC.VersionCounter(
                        counter=1
                    )
                )
            ),
            status=DTC.OfferStatus(
                version=DTC.VersionStatus(
                    uc_data_version=DTC.VersionCounter(
                        counter=1
                    ),
                ),
            ),
        )), offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=DECLINE_REMOVE_SERVICE_PART_BUSINESS_ID,
                offer_id=DECLINE_REMOVE_SERVICE_PART_OFFER_ID,
            ),
            content=DTC.OfferContent(
                market=DTC.MarketContent(
                    real_uc_version=DTC.VersionCounter(
                        counter=1
                    )
                )
            ),
            status=DTC.OfferStatus(
                version=DTC.VersionStatus(
                    uc_data_version=DTC.VersionCounter(
                        counter=1
                    ),
                ),
            ),
        ))])


@pytest.fixture(scope='module')
def service_offers_table(yt_server, piper_config):
    return DataCampServiceOffersTable(yt_server, piper_config.yt_service_offers_tablepath, data=[
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id=offer_id,
                shop_id=SHOP_ID,
            ),
            meta=create_meta(10),
        )) for offer_id in ['T1000', 'T2000', 'T3000', 'T4000']
    ] + [
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=REMOVE_SERVICE_PART_BUSINESS_ID,
                offer_id=REMOVE_SERVICE_PART_OFFER_ID,
                shop_id=REMOVE_SERVICE_PART_SHOP_ID,
            ),
            meta=create_meta(10),
        )),
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=DECLINE_REMOVE_SERVICE_PART_BUSINESS_ID,
                offer_id=DECLINE_REMOVE_SERVICE_PART_OFFER_ID,
                shop_id=DECLINE_REMOVE_SERVICE_PART_SHOP_ID,
            ),
            status=DTC.OfferStatus(
                removed=DTC.Flag(
                    flag=True,
                ),
            ),
            meta=create_meta(10),
        )),
    ])


@pytest.fixture(scope='module', params=[True, False])
def send_to_dispatcher_via_qyt(request):
    return request.param


@pytest.fixture(scope='module')
def datacamp_messages_topic(log_broker_stuff):
    return LbkTopic(log_broker_stuff, 'datacamp_messages_topic')


@pytest.fixture(scope='module')
def mboc_topic(log_broker_stuff):
    return LbkTopic(log_broker_stuff, 'mboc_topic')


@pytest.fixture(scope='module')
def subscription_service_topic(log_broker_stuff):
    return LbkTopic(log_broker_stuff, 'subscription_service_topic')


@pytest.fixture(scope='module')
def piper_config(
    yt_server,
    log_broker_stuff,
    datacamp_messages_topic,
    subscription_service_topic,
    send_to_dispatcher_via_qyt,
    qyt_subscription
):
    cfg = {
        'logbroker': {
            'datacamp_messages_topic': datacamp_messages_topic.topic,
            'subscription_service_topic': subscription_service_topic.topic,
        },
        'general': {
            'color': 'blue',
            'send_to_dispatcher_via_qyt': send_to_dispatcher_via_qyt
        },
        'yt': {
            'dispatcher_queue_path': qyt_subscription.path if send_to_dispatcher_via_qyt else None,
        },
    }
    return PiperConfigMock(yt_server=yt_server, log_broker_stuff=log_broker_stuff, config=cfg)


@pytest.yield_fixture(scope='module')
def piper(
    yt_server,
    log_broker_stuff,
    piper_config,
    basic_offers_table,
    service_offers_table,
    datacamp_messages_topic,
    subscription_service_topic,
):
    resources = {
        'config': piper_config,
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'datacamp_messages_topic': datacamp_messages_topic,
        'subscription_service_topic': subscription_service_topic,
    }
    with PiperTestEnv(yt_server, log_broker_stuff, **resources) as piper_env:
        piper_env.verify()
        yield piper_env


@pytest.fixture(scope='module')
def dispatcher_config(
    yt_token,
    yt_server,
    log_broker_stuff,
    piper_config,
    send_to_dispatcher_via_qyt,
    subscription_service_topic,
    mboc_topic,
    qyt_subscription,
    port_manager,
):
    cfg = DispatcherConfig()
    cfg.create_initializer(yt_server=yt_server, yt_token_path=yt_token.path)

    reader = cfg.create_dispatcher_reader(yt_server, qyt_subscription, port_manager) if send_to_dispatcher_via_qyt else cfg.create_lb_reader(log_broker_stuff, subscription_service_topic)
    unpacker = cfg.create_subscription_message_unpacker()
    dispatcher = cfg.create_subscription_dispatcher(
        piper_config.yt_basic_offers_tablepath, piper_config.yt_service_offers_tablepath, piper_config.yt_actual_service_offers_tablepath
    )
    sender = cfg.create_mboc_sender()
    writer = cfg.create_lb_writer(log_broker_stuff, mboc_topic)

    cfg.create_link(reader, unpacker)
    cfg.create_link(unpacker, dispatcher)
    cfg.create_link(dispatcher, sender)
    cfg.create_link(sender, writer)

    return cfg


@pytest.yield_fixture(scope='module')
def dispatcher(
    dispatcher_config,
    basic_offers_table,
    service_offers_table,
    subscription_service_topic,
    mboc_topic,
    send_to_dispatcher_via_qyt,
    qyt_subscription,
):
    resources = {
        'dispatcher_config': dispatcher_config,
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'subscription_service_topic': subscription_service_topic,
        'mboc_topic': mboc_topic,
    }
    if send_to_dispatcher_via_qyt:
        resources['qyt'] = qyt_subscription

    with DispatcherTestEnv(**resources) as env:
        env.verify()
        yield env


@pytest.fixture(scope='module')
def inserter(piper, dispatcher, datacamp_messages_topic):
    datacamp_messages_topic.write(message_from_data({
        'united_offers': [{
            'offer': [message for message in DATACAMP_MESSAGES]
        }],
    }, DatacampMessage()).SerializeToString())


@pytest.fixture(scope='module')
def mboc_output(piper, inserter, dispatcher, mboc_topic):
    wait_until(lambda: dispatcher.subscription_dispatcher_processed >= 8, timeout=120)
    data = mboc_topic.read(count=8, wait_timeout=120)
    return data


def test_not_send_not_consistent_offer(piper, inserter, dispatcher, mboc_output, basic_offers_table):
    # проверяем, что версии правильно обновились и сломалась консистентность
    basic_offers_table.load()
    assert_that(basic_offers_table.data, HasOffers([message_from_data({
        'identifiers': {
            'business_id': BUSINESS_ID,
            'offer_id': 'T1000',
        },
        'content': {
            'partner': {
                'original': {
                    'country_of_origin': {
                        'value': ['Россия'],
                    },
                },
            },
            'market': {
                'real_uc_version': {
                    'counter': 1
                },
            },
        },
        'status': {
            'consistency': {
                'mboc_consistency': False,
            }
        },
    }, DTC.Offer())]))
    for row in basic_offers_table.data:
        offer = message_from_data(deserialize_united_row(row), DTC.Offer())
        if offer.identifiers.offer_id == 'T1000' and offer.identifiers.business_id == BUSINESS_ID:
            assert_that(offer.status.version.uc_data_version.counter, greater_than(1))
            break

    # в mboc ничего отправлено не было
    assert_that(mboc_output, not_(has_item(IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': 'T1000',
                    },
                },
            }]
        }]
    }))))


def test_send_content_update_using_consistency(piper, inserter, dispatcher, mboc_output, basic_offers_table):
    # проверяем, что версии правильно обновились и вернулась косистентность
    basic_offers_table.load()
    assert_that(basic_offers_table.data, HasOffers([message_from_data({
        'identifiers': {
            'business_id': BUSINESS_ID,
            'offer_id': 'T2000',
        },
        'content': {
            'market': {
                'real_uc_version': {
                    'counter': 2
                },
            },
            'partner': {
                'actual': {
                    'country_of_origin_id': {
                        'value': [
                            225
                        ]
                    },
                },
            }
        },
        'status': {
            'consistency': {
                'mboc_consistency': True,
            },
            'version': {
                'uc_data_version': {
                    'counter': 2,
                },
            },
        },
    }, DTC.Offer())]))

    # проверяем, что данные ушли в мбок
    assert_that(mboc_output, has_item(IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': 'T2000',
                    },
                    'status': {
                        'consistency': {
                            'mboc_consistency': True,
                        },
                    },
                },
                'service': IsProtobufMap({
                    2: {}
                })
            }]
        }]
    })))


def test_send_content_update(piper, inserter, dispatcher, mboc_output):
    # проверяем срабатывание триггера при изменении поля, на котором есть подписка
    # оффер при этом должен находиться в консистентном состоянии
    assert_that(mboc_output, has_item(IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': 'T3000',
                    },
                    'content': {
                        'partner': {
                            'actual': {
                                'description': {
                                    'value': 'description',
                                }
                            }
                        }
                    },
                }
            }]
        }]
    })))


def test_send_pictures_update(piper, inserter, dispatcher, mboc_output):
    # Консистентность сохраняется, сообщение должно уйти в топик mboc, так как поменялись данные картинки
    assert_that(mboc_output, has_item(IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': 'T4000',
                    },
                    'pictures': {
                        'partner': {

                            'actual': IsProtobufMap({
                                'picurl': {
                                    'status': DTC.MarketPicture.Status.FAILED,
                                }
                            }),
                        },
                    },
                }
            }]
        }]
    })))


def test_send_business_catalog_offers_without_service(piper, inserter, dispatcher, mboc_output):
    # Проверяем, что оффер из ассортиментного каталога под бизнесом без сервисной части отправляется в mboc

    # Консистентный оффер, должны триггернуться на изменение поля и отправить в mboc
    assert_that(mboc_output, has_item(IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_CATALOG_BUSINESS_ID,
                        'offer_id': CONSISTENT_BUSINESS_CATALOG_OFFER_ID,
                    },
                    'content': {
                        'partner': {
                            'actual': {
                                'description': {
                                    'value': 'description',
                                }
                            }
                        }
                    },
                },
                'service': empty()
            }]
        }]
    })))

    # Оффер без сервисной части и не из каталога под бизнесом - не должны отправлять
    assert_that(mboc_output, not_(has_item(IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': NOT_BUSINESS_CATALOG_BUSINESS_ID,
                        'offer_id': NOT_IN_BUSINESS_CATALOG_OFFER_ID,
                    },
                },
            }]
        }]
    }))))

    # Неконсистентный оффер из каталога под бизнесом - не должны отправлять
    assert_that(mboc_output, not_(has_item(IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_CATALOG_BUSINESS_ID,
                        'offer_id': NOT_CONSISTENT_BUSINESS_CATALOG_OFFER_ID,
                    },
                },
            }]
        }]
    }))))


def test_send_add_service(piper, inserter, dispatcher, mboc_output):
    # Появилась сервисная часть - должны отправить в МБОК
    assert_that(mboc_output, has_item(IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': ADD_SERVICE_PART_BUSINESS_ID,
                        'offer_id': ADD_SERVICE_PART_OFFER_ID,
                    },
                },
                'service': IsProtobufMap({
                    ADD_SERVICE_PART_SHOP_ID: {},
                }),
            }]
        }]
    })))


def test_send_remove_service(piper, inserter, dispatcher, mboc_output):
    # Удалилась сервисная часть - должны отправить в МБОК
    assert_that(mboc_output, has_item(IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': REMOVE_SERVICE_PART_BUSINESS_ID,
                        'offer_id': REMOVE_SERVICE_PART_OFFER_ID,
                    },
                },
                'service': IsProtobufMap({
                    REMOVE_SERVICE_PART_SHOP_ID: {},
                }),
            }]
        }]
    })))


def test_send_decline_remove_service(piper, inserter, dispatcher, mboc_output):
    # Отменили удаление сервисной части - должны отправить в МБОК
    assert_that(mboc_output, has_item(IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': DECLINE_REMOVE_SERVICE_PART_BUSINESS_ID,
                        'offer_id': DECLINE_REMOVE_SERVICE_PART_OFFER_ID,
                    },
                },
                'service': IsProtobufMap({
                    DECLINE_REMOVE_SERVICE_PART_SHOP_ID: {},
                }),
            }]
        }]
    })))
