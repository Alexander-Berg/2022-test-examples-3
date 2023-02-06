# coding: utf-8

import pytest

from datetime import datetime, timedelta
from hamcrest import assert_that, not_

from market.idx.yatf.matchers.yt_rows_matchers import HasOffers
from market.idx.yatf.utils.utils import create_timestamp_from_json

from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock
from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.proto.offer import DataCampOffer_pb2 as DTC
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.datacamp.yatf.utils import create_meta

from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampServiceOffersTable

from market.pylibrary.proto_utils import message_from_data
from market.idx.pylibrary.datacamp.conversion import offer_to_service_row


"""Тест проверяет выставление disabled c источником DTC.MARKET_IDX новым офферам.
(ранее - всем офферам не из парсера, теперь - всем)
(сделано, чтобы мы не индексировали офферы, для которых не проставлены offer_flags
основывающиеся на данных из шопсдата или просто недообогащенные офферы)
"""

current_utc = datetime.utcnow()  # JSON serializer should always use UTC
future_utc = datetime.utcnow() + timedelta(minutes=45)
old_utc = datetime.utcnow() - timedelta(minutes=45)

time_pattern = "%Y-%m-%dT%H:%M:%SZ"
current_time = current_utc.strftime(time_pattern)
future_time = future_utc.strftime(time_pattern)
old_time = old_utc.strftime(time_pattern)

CREATED_TS = create_timestamp_from_json(current_time)
FUTURE_TS = create_timestamp_from_json(future_time)
OLD_TS = create_timestamp_from_json(old_time)


@pytest.fixture(scope='module')
def service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(yt_server, config.yt_service_offers_tablepath, data=[
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=4,
                offer_id='4',
                shop_id=4,
            ),
            meta=create_meta(10, color=DTC.BLUE),
            price=DTC.OfferPrice(
                basic=DTC.PriceBundle(
                    binary_price=DTC.PriceExpression(
                        id='RUR',
                        price=100,
                    ),
                    meta=DTC.UpdateMeta(
                        timestamp=CREATED_TS,
                    )
                ),
                enable_auto_discounts=DTC.Flag(
                    flag=True,
                    meta=DTC.UpdateMeta(
                        timestamp=CREATED_TS
                    )
                )
            )
        ))])


@pytest.fixture(scope='module')
def actual_service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(yt_server, config.yt_actual_service_offers_tablepath, data=[
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=4,
                offer_id='4',
                shop_id=4,
                warehouse_id=147
            ),
            meta=create_meta(10, color=DTC.BLUE),
        ))])


@pytest.fixture(scope='session')
def datacamp_messages():
    return [{
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': 1,
                        'offer_id': '1',
                    },
                },
                'service': {
                    1: {
                        'identifiers': {
                            'business_id': 1,
                            'offer_id': '1',
                            'shop_id': 1,
                            'warehouse_id': 147
                        },
                        'meta': {
                            'ts_created': current_time,
                            'scope': DTC.SERVICE
                        },
                        'price': {
                            'basic': {
                                'binary_price': {
                                    'id': 'RUR',
                                    'price': 10023100
                                }
                            }
                        }
                    }
                }
            }, {
                'basic': {
                    'identifiers': {
                        'business_id': 2,
                        'offer_id': '2',
                    },
                },
                'service': {
                    2: {
                        'identifiers': {
                            'business_id': 2,
                            'offer_id': '2',
                            'shop_id': 2,
                            'warehouse_id': 147
                        },
                        'meta': {
                            'ts_created': current_time,
                            'scope': DTC.SERVICE
                        },
                        'price': {
                            'basic': {
                                'binary_price': {
                                    'id': 'RUR',
                                    'price': 10023100
                                },
                                'meta': {
                                    'timestamp': current_time,
                                    'source': DTC.PUSH_PARTNER_FEED,
                                }
                            }
                        }
                    }
                }
            }, {
                'basic': {
                    'identifiers': {
                        'business_id': 3,
                        'offer_id': '3',
                    },
                },
                'service': {
                    3: {
                        'identifiers': {
                            'business_id': 3,
                            'offer_id': '3',
                            'shop_id': 3,
                            'warehouse_id': 147
                        },
                        'meta': {
                            'ts_created': current_time,
                            'scope': DTC.SERVICE
                        },
                        'price': {
                            'basic': {
                                'binary_price': {
                                    'id': 'RUR',
                                    'price': 10023100
                                },
                                'meta': {
                                    'timestamp': current_time,
                                    'source': DTC.PUSH_PARTNER_API,
                                }
                            }
                        }
                    }
                }
            }, {
                'basic': {
                    'identifiers': {
                        'business_id': 5,
                        'offer_id': '5',
                    },
                },
                'service': {
                    5: {
                        'identifiers': {
                            'business_id': 5,
                            'offer_id': '5',
                            'shop_id': 5,
                            'warehouse_id': 147
                        },
                        'meta': {
                            'ts_created': current_time,
                            'scope': DTC.SERVICE
                        },
                        'price': {
                            'basic': {
                                'binary_price': {
                                    'id': 'RUR',
                                    'price': 10023100
                                },
                                'meta': {
                                    'timestamp': current_time,
                                    'source': DTC.PUSH_PARTNER_API,
                                }
                            }
                        },
                        'status': {
                            'disabled': [{
                                'flag': True,
                                'meta': {
                                    'timestamp': old_time,
                                    'source': DTC.MARKET_IDX
                                }
                            }]
                        }
                    }
                }
            }, {
                'basic': {
                    'identifiers': {
                        'business_id': 6,
                        'offer_id': '6',
                    },
                },
                'service': {
                    6: {
                        'identifiers': {
                            'business_id': 6,
                            'offer_id': '6',
                            'shop_id': 6,
                            'warehouse_id': 147
                        },
                        'meta': {
                            'ts_created': current_time,
                            'scope': DTC.SERVICE
                        },
                    }
                }
            }]
        }]
    }]


@pytest.fixture(scope='module')
def datacamp_messages_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def config(yt_server, log_broker_stuff, datacamp_messages_topic):
    cfg = {
        'logbroker': {
            'datacamp_messages_topic': datacamp_messages_topic.topic,
        },
        'general': {
            'color': 'blue',
        }
    }
    return PiperConfigMock(yt_server=yt_server,
                           log_broker_stuff=log_broker_stuff,
                           config=cfg)


@pytest.yield_fixture(scope='module')
def controller(yt_server, log_broker_stuff, config, datacamp_messages_topic, service_offers_table, actual_service_offers_table):
    resources = {
        'config': config,
        'datacamp_messages_topic': datacamp_messages_topic,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table
    }
    with PiperTestEnv(yt_server, log_broker_stuff, **resources) as controller_env:
        controller_env.verify()
        yield controller_env


@pytest.fixture(scope='module')
def inserter(datacamp_messages, controller, datacamp_messages_topic):
    def do_insert(messages):
        processed_messages = controller.united_offers_processed
        for message in messages:
            datacamp_messages_topic.write(message_from_data(message, DatacampMessage()).SerializeToString())
        wait_until(lambda: controller.united_offers_processed >= processed_messages + len(messages))

    do_insert(datacamp_messages)

    return do_insert


def test_no_source(inserter, controller):
    assert_that(controller.service_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': '1',
                'shop_id': 1
            },
            'status': {
                'publish_by_partner': DTC.AVAILABLE,
            }
        }, DTC.Offer())]))

    assert_that(controller.actual_service_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': '1',
                'shop_id': 1,
                'warehouse_id': 147
            },
            'status': {
                'publish': DTC.HIDDEN,
                'disabled': [
                    {
                        'flag': True,
                        'meta': {
                            'source': DTC.MARKET_IDX,
                            'timestamp': current_time
                        },
                    },
                ],
            }
        }, DTC.Offer())]))


def test_source_push_feed(inserter, controller):
    assert_that(controller.service_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 2,
                'offer_id': '2',
                'shop_id': 2
            },
            'status': {
                'publish_by_partner': DTC.AVAILABLE,
            }
        }, DTC.Offer())]))

    assert_that(controller.actual_service_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 2,
                'offer_id': '2',
                'shop_id': 2,
                'warehouse_id': 147
            },
            'status': {
                'publish': DTC.HIDDEN,
                'disabled': [
                    {
                        'flag': True,
                        'meta': {
                            'source': DTC.MARKET_IDX,
                            'timestamp': current_time,
                        },
                    },
                ],
            }
        }, DTC.Offer())]))


def test_source_push_api(inserter, controller):
    assert_that(controller.service_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 3,
                'offer_id': '3',
                'shop_id': 3
            },
            'status': {
                'publish_by_partner': DTC.AVAILABLE,
            }
        }, DTC.Offer())]))

    assert_that(controller.actual_service_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 3,
                'offer_id': '3',
                'shop_id': 3,
                'warehouse_id': 147
            },
            'status': {
                'publish': DTC.HIDDEN,
                'disabled': [
                    {
                        'flag': True,
                        'meta': {
                            'source': DTC.MARKET_IDX,
                            'timestamp': current_time,
                        },
                    },
                ],
            }
        }, DTC.Offer())]))


def test_old_offer(inserter, controller, datacamp_messages_topic):
    # пришло обновление для оффера, который уже был в таблице - не будет установлен disabled с DTC.MARKET_IDX
    message = {
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': 4,
                        'offer_id': '4',
                    },
                },
                'service': {
                    4: {
                        'identifiers': {
                            'business_id': 4,
                            'offer_id': '4',
                            'shop_id': 4,
                            'warehouse_id': 147
                        },
                        'meta': {
                            'ts_created': future_time,
                            'scope': DTC.SERVICE
                        },
                        'price': {
                            'basic': {
                                'binary_price': {
                                    'id': 'RUR',
                                    'price': 10023200
                                },
                                'meta': {
                                    'source': DTC.PUSH_PARTNER_API,
                                    'timestamp': future_time,
                                }
                            }
                        }
                    }
                }
            }]
        }]
    }

    inserter([message])

    # шаг 1: проверяем, что оффер обновился
    assert_that(controller.service_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 4,
                'offer_id': '4',
                'shop_id': 4
            },
            'status': {
                'publish_by_partner': DTC.AVAILABLE,
            },
            'price': {
                'basic': {
                    'meta': {
                        'source': DTC.PUSH_PARTNER_API,
                        'timestamp': future_time
                    },
                    'binary_price': {
                        'price': 10023200,
                        'id': 'RUR',
                    }
                },
                'enable_auto_discounts': {
                    'meta': {
                        'timestamp': current_time
                    },
                    'flag': True
                }
            }
        }, DTC.Offer())]))

    # шаг 2: проверяем, что флага нет
    assert_that(controller.actual_service_offers_table.data, not_(HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 4,
                'offer_id': '4',
                'shop_id': 4,
                'warehouse_id': 147
            },
            'status': {
                'disabled': [
                    {
                        'flag': True,
                        'meta': {
                            'source': DTC.MARKET_IDX,
                        },
                    },
                ],
            }
        }, DTC.Offer())])))


def test_new_offer_with_market_idx_already(inserter, controller):
    # шаг 1: проверяем, что флаг приехавший сразу новым оффером попал в хранилище
    assert_that(controller.service_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 5,
                'offer_id': '5',
                'shop_id': 5
            },
            'status': {
                'publish_by_partner': DTC.AVAILABLE,
            }
        }, DTC.Offer())]))

    assert_that(controller.actual_service_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 5,
                'offer_id': '5',
                'shop_id': 5,
                'warehouse_id': 147
            },
            'status': {
                'publish': DTC.HIDDEN,
                'ready_for_publication': None,  # ready_for_publication пусто, что соотв. дефолтовому значению NOT_READY
                'disabled': [
                    {
                        'flag': True,
                        'meta': {
                            'source': DTC.MARKET_IDX,
                            'timestamp': old_time,
                        },
                    },
                ],
            }
        }, DTC.Offer())]))

    # шаг 2: проверяем, что флаг при вставке не добавился лишний с новым ts
    assert_that(controller.actual_service_offers_table.data, not_(HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 5,
                'offer_id': '5',
                'shop_id': 5,
                'warehouse_id': 147
            },
            'status': {
                'publish': DTC.HIDDEN,
                'disabled': [
                    {
                        'flag': True,
                        'meta': {
                            'source': DTC.MARKET_IDX,
                            'timestamp': current_time,
                        },
                    },
                ],
            }
        }, DTC.Offer())])))


def test_empty_offer(inserter, controller):
    assert_that(controller.service_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 6,
                'offer_id': '6',
                'shop_id': 6
            },
            'status': {
                'publish_by_partner': DTC.AVAILABLE,
            }
        }, DTC.Offer())]))

    assert_that(controller.actual_service_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 6,
                'offer_id': '6',
                'shop_id': 6,
                'warehouse_id': 147
            },
            'status': {
                'publish': DTC.HIDDEN,
                'disabled': [
                    {
                        'flag': True,
                        'meta': {
                            'source': DTC.MARKET_IDX,
                            'timestamp': current_time,
                        },
                    },
                ],
            }
        }, DTC.Offer())]))
