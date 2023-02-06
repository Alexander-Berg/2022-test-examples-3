# coding: utf-8

import pytest
from datetime import datetime, timedelta
from hamcrest import assert_that, contains_string

from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.proto.api.OfferHistory_pb2 import OfferHistory
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock
from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.datacamp.yatf.utils import create_meta, create_update_meta
from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampBasicOffersTable, DataCampServiceOffersTable
from market.idx.yatf.matchers.lb_matchers import HasNoUnreadData
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.pylibrary.proto_utils import message_from_data
from market.idx.pylibrary.datacamp.conversion import offer_to_service_row, offer_to_basic_row

NOW_UTC = datetime.utcnow()  # JSON serializer should always use UTC
FUTURE_UTC = NOW_UTC + timedelta(minutes=45)
time_pattern = "%Y-%m-%dT%H:%M:%SZ"
current_time = NOW_UTC.strftime(time_pattern)
future_time = FUTURE_UTC.strftime(time_pattern)

BUSINESS_ID = 10
BUSINESS_ID_2 = 20
# белый магазин
WHITE_SHOP_ID = 1
# синий магазин
BLUE_SHOP_ID = 2
# отправляем оффер, даже если у него нет актуальной части
BLUE_SHOP_ID_ONLY_ORIGINAL = 5
WAREHOUSE_ID = 145

DATACAMP_MESSAGES = [
    {
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': 'T1000',
                    },
                },
                'service': {
                    WHITE_SHOP_ID: {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': 'T1000',
                            'shop_id': WHITE_SHOP_ID,
                        },
                        'meta': {
                            'scope': DTC.SERVICE,
                            'rgb': DTC.WHITE
                        },
                        # Изменение партерского скрытия ведет к пересчету status.publish_by_partner
                        # поле publish_by_partner триггерит отправку в offer_history топик
                        'status': {
                            'disabled': [
                                {
                                    'flag': True,
                                    'meta': {
                                        'source': DTC.PUSH_PARTNER_OFFICE,
                                        'timestamp': current_time,
                                    }
                                }
                            ],
                        }
                    }
                }
            }, {
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_ID_2,
                        'offer_id': 'T1000',
                    }
                },
                'service': {
                    BLUE_SHOP_ID: {
                        'identifiers': {
                            'business_id': BUSINESS_ID_2,
                            'offer_id': 'T1000',
                            'shop_id': BLUE_SHOP_ID,
                        },
                        'meta': {
                            'scope': DTC.SERVICE,
                            'rgb': DTC.BLUE
                        },
                        'price': {
                            # поле триггерящее отправку в offer_history топик
                            'original_price_fields': {
                                'vat': {
                                    'value': 5,
                                    'meta': {
                                        'timestamp': current_time,
                                    },
                                }
                            },
                            # поле-триггер для offer_history, но которое не меняется (его не отправляем)
                            'basic': {
                                'binary_price': {
                                    'price': 123 * 10**7,
                                    'id': 'RUR'
                                },
                                'meta': {
                                    'timestamp': current_time,
                                    'source': DTC.PUSH_PARTNER_FEED,
                                }
                            }
                        }
                    }
                }
            }]
        }]
    }
]


@pytest.fixture(scope='module')
def datacamp_messages_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def offer_history_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def config(yt_server, log_broker_stuff, datacamp_messages_topic, offer_history_topic):
    cfg = {
        'general': {
            'color': 'white',
        },
        'logbroker': {
            'datacamp_messages_topic': datacamp_messages_topic.topic,
            'offer_history_topic': offer_history_topic.topic,
        },
    }
    return PiperConfigMock(yt_server=yt_server, log_broker_stuff=log_broker_stuff, config=cfg)


@pytest.fixture(scope='module')
def basic_offers_table(yt_server, config):
    return DataCampBasicOffersTable(yt_server, config.yt_basic_offers_tablepath, data=[
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id='T1000',
            ),
            meta=create_meta(10),
            )),
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID_2,
                offer_id='T1000',
            ),
            meta=create_meta(10)
            ))])


@pytest.fixture(scope='module')
def service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(yt_server, config.yt_service_offers_tablepath, data=[
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id='T1000',
                shop_id=BLUE_SHOP_ID,
            ),
            meta=create_meta(10, color=DTC.BLUE),
        )),
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id='T1000',
                shop_id=BLUE_SHOP_ID_ONLY_ORIGINAL,
            ),
            meta=create_meta(10, color=DTC.BLUE),
            )),
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID_2,
                offer_id='T1000',
                shop_id=BLUE_SHOP_ID,
            ),
            meta=create_meta(10, color=DTC.BLUE),
            price=DTC.OfferPrice(
                basic=DTC.PriceBundle(
                    binary_price=DTC.PriceExpression(
                        id='RUR',
                        price=123 * 10**7,
                    ),
                    meta=create_update_meta(10),
                )
            )
            ))])


@pytest.fixture(scope='module')
def actual_service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(yt_server, config.yt_actual_service_offers_tablepath, data=[
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id='T1000',
                shop_id=BLUE_SHOP_ID,
                warehouse_id=WAREHOUSE_ID
            ),
            meta=create_meta(10, color=DTC.BLUE),
        ))])


@pytest.yield_fixture(scope='module')
def piper(
    yt_server,
    log_broker_stuff,
    config,
    datacamp_messages_topic,
    offer_history_topic,
    basic_offers_table,
    service_offers_table,
    actual_service_offers_table
):
    resources = {
        'config': config,
        'datacamp_messages_topic': datacamp_messages_topic,
        'offer_history_topic': offer_history_topic,
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table
    }
    with PiperTestEnv(yt_server, log_broker_stuff, **resources) as piper_env:
        piper_env.verify()
        yield piper_env


def test_write_to_offer_history(piper, datacamp_messages_topic, offer_history_topic):
    for message in DATACAMP_MESSAGES:
        datacamp_messages_topic.write(message_from_data(message, DatacampMessage()).SerializeToString())
    wait_until(lambda: piper.united_offers_processed >= 1)

    data = offer_history_topic.read(count=2)
    assert_that(data[0], IsSerializedProtobuf(OfferHistory, {
        'business_id': BUSINESS_ID,
        'offer_id': 'T1000',
        'shop_id': WHITE_SHOP_ID,
        'disabled_by_partner': {
            'flag': True
        }
    }))
    assert_that(data[1], IsSerializedProtobuf(OfferHistory, {
        'business_id': BUSINESS_ID_2,
        'offer_id': 'T1000',
        'shop_id': BLUE_SHOP_ID,
        'vat': {
            'value': 5,
        },
        'disabled_by_partner': {
            'flag': False
        }
    }))

    # Проверяем, что неизменившиеся поля не отправляются
    offer_history_message = OfferHistory()
    offer_history_message.ParseFromString(data[1])
    assert not offer_history_message.HasField('price')

    # Проверяем, что заполнен источник (в тестах datacamp_message_unpacker, может поменяться)
    assert_that(offer_history_message.data_source, contains_string("datacamp_message_unpacker"))

    # Шлем новое сообщение - изменение цены для оффера без акт. сервисных частей
    datacamp_messages_topic.write(message_from_data(
        {
            'united_offers': [{
                'offer': [{
                    'basic': {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': 'T1000',
                        },
                    },
                    'service': {
                        BLUE_SHOP_ID_ONLY_ORIGINAL: {
                            'identifiers': {
                                'business_id': BUSINESS_ID,
                                'shop_id': BLUE_SHOP_ID_ONLY_ORIGINAL,
                                'offer_id': 'T1000',
                            },
                            # Цена - триггер для offer_history_subscriber
                            'price': {
                                'basic': {
                                    'binary_price': {
                                        'price': 234 * 10**7,
                                        'id': 'RUR'
                                    },
                                    'meta': {
                                        'timestamp': current_time,
                                        'source': DTC.PUSH_PARTNER_FEED,
                                    }
                                }
                            }
                        },
                    }
                }]
            }]
        }, DatacampMessage()).SerializeToString())

    # Цена должна триггерить запись в offer_history_topic, даже если нет акт.сервисной части
    data = offer_history_topic.read(count=1)
    assert_that(data[0], IsSerializedProtobuf(OfferHistory, {
        'business_id': BUSINESS_ID,
        'offer_id': 'T1000',
        'shop_id': BLUE_SHOP_ID_ONLY_ORIGINAL,
        # Проверяем, что цена доехала
        'price': {
            'binary_price': {
                'price': 234 * 10**7,
                'id': 'RUR'
            },
            'meta': {
                'source': DTC.PUSH_PARTNER_FEED,
            }
        },
        'disabled_by_partner': {
            'flag': False
        }
    }))

    # Шлем новое сообщение - парнерские стоки
    datacamp_messages_topic.write(message_from_data(
        {
            'united_offers': [{
                'offer': [{
                    'basic': {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': 'T1000',
                        },
                    },
                    'service': {
                        BLUE_SHOP_ID: {
                            'identifiers': {
                                'business_id': BUSINESS_ID,
                                'shop_id': BLUE_SHOP_ID,
                                'offer_id': 'T1000',
                                'warehouse_id': WAREHOUSE_ID,
                            },
                            # Стоки - триггер для offer_history_subscriber
                            'stock_info': {
                                'partner_stocks': {
                                    'count': 77,
                                    'meta': {
                                        'timestamp': current_time,
                                        'source': DTC.PUSH_PARTNER_OFFICE,
                                    }
                                }
                            }
                        },
                    }
                }]
            }]
        }, DatacampMessage()).SerializeToString())

    # Партнерские стоки должны триггерить запись в offer_history_topic
    data = offer_history_topic.read(count=1)
    assert_that(data[0], IsSerializedProtobuf(OfferHistory, {
        'business_id': BUSINESS_ID,
        'offer_id': 'T1000',
        'shop_id': BLUE_SHOP_ID,
        'warehouse_id': WAREHOUSE_ID,
        # Проверяем, что стоки доехали
        'partner_stocks': {
            'count': 77,
            'meta': {
                'source': DTC.PUSH_PARTNER_OFFICE,
            }
        },
    }))

    # Шлем новое сообщение - партнерское раскрытие оффера
    datacamp_messages_topic.write(message_from_data(
        {
            'united_offers': [{
                'offer': [{
                    'basic': {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': 'T1000',
                        },
                    },
                    'service': {
                        WHITE_SHOP_ID: {
                            'identifiers': {
                                'business_id': BUSINESS_ID,
                                'shop_id': WHITE_SHOP_ID,
                                'offer_id': 'T1000'
                            },
                            # Партнерское раскрытие - триггер для offer_history_subscriber
                            'status': {
                                'disabled': [
                                    {
                                        'flag': False,
                                        'meta': {
                                            'source': DTC.PUSH_PARTNER_FEED,
                                            'timestamp': future_time,
                                        }
                                    }
                                ],
                            }
                        },
                    }
                }]
            }]
        }, DatacampMessage()).SerializeToString())

    # Партнерское раскрытие должно триггерить запись в offer_history_topic
    data = offer_history_topic.read(count=1)
    assert_that(data[0], IsSerializedProtobuf(OfferHistory, {
        'business_id': BUSINESS_ID,
        'offer_id': 'T1000',
        'shop_id': WHITE_SHOP_ID,
        # Проверяем, что раскрытие доехало
        'disabled_by_partner': {
            'flag': False,
            'meta': {
                'source': DTC.PUSH_PARTNER_FEED,
            }
        },
    }))

    # Шлем новое сообщение - цена в базовой части оффера
    datacamp_messages_topic.write(message_from_data(
        {
            'united_offers': [{
                'offer': [{
                    'basic': {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': 'T1000',
                        },
                        'meta': {
                            'scope': DTC.BASIC,
                        },
                        # поле триггерящее отправку в offer_history топик, цена - в базовой части
                        'price': {
                            'basic': {
                                'binary_price': {
                                    'price': 678 * 10**7,
                                    'id': 'RUR'
                                },
                                'meta': {
                                    'timestamp': future_time,
                                    'source': DTC.PUSH_PARTNER_FEED,
                                }
                            }
                        },
                    },
                    # Пришлось послать сервисный оффер, иначе оффер отсекается в TSubscriptionFilter, потому что не проходит ColorFilter
                    'service': {
                        WHITE_SHOP_ID: {
                            'identifiers': {
                                'business_id': BUSINESS_ID,
                                'shop_id': WHITE_SHOP_ID,
                                'offer_id': 'T1000'
                            },
                        }
                    }
                }]
            }]
        }, DatacampMessage()).SerializeToString())

    # Цен должна триггерить запись в offer_history_topic
    data = offer_history_topic.read(count=1)
    assert_that(data[0], IsSerializedProtobuf(OfferHistory, {
        'business_id': BUSINESS_ID,
        'offer_id': 'T1000',
        'shop_id': None,
        'warehouse_id': None,
        # Проверяем, что цена доехала
        'price': {
            'binary_price': {
                'price': 678 * 10**7,
                'id': 'RUR'
            },
            'meta': {
                'source': DTC.PUSH_PARTNER_FEED,
            }
        },
    }))

    # проверяем, что в топике больше нет данных, которые мы можем вычитать
    assert_that(offer_history_topic, HasNoUnreadData())
