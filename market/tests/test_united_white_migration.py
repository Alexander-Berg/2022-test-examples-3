# coding: utf-8

import pytest
from datetime import datetime, timedelta
from hamcrest import assert_that, equal_to

import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock
from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.pylibrary.datacamp.utils import wait_until
from market.pylibrary.proto_utils import message_from_data
from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampBasicOffersTable, DataCampServiceOffersTable
from market.idx.yatf.matchers.yt_rows_matchers import HasOffers
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.utils.utils import create_timestamp_from_json

NOW_UTC = datetime.utcnow()  # JSON serializer should always use UTC
time_pattern = "%Y-%m-%dT%H:%M:%SZ"
current_time = NOW_UTC.strftime(time_pattern)
current_ts = create_timestamp_from_json(current_time)
FUTURE_UTC_1 = NOW_UTC + timedelta(minutes=10)
FUTURE_UTC_2 = NOW_UTC + timedelta(minutes=20)
FUTURE_UTC_3 = NOW_UTC + timedelta(minutes=30)
FUTURE_UTC_4 = NOW_UTC + timedelta(minutes=40)
FUTURE_UTC_5 = NOW_UTC + timedelta(minutes=50)

DATACAMP_MESSAGES = [
    {
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': 1,
                        'offer_id': 'o1',
                    },
                    'meta': {
                        'scope': DTC.BASIC,
                        'ts_created': NOW_UTC.strftime(time_pattern)
                    },
                },
                'service': {
                    1: {
                        'identifiers': {
                            'business_id': 1,
                            'offer_id': 'o1',
                            'shop_id': 1,
                            'warehouse_id': 0,
                        },
                        'meta': {
                            'scope': DTC.SERVICE,
                            'rgb': DTC.WHITE,
                            'ts_created': NOW_UTC.strftime(time_pattern)
                        },
                        'price': {
                            'basic': {
                                'binary_price': {
                                    'price': 20
                                },
                                'meta': {
                                    'timestamp': NOW_UTC.strftime(time_pattern)
                                }
                            }
                        },
                        'content': {
                            'partner': {
                                'original': {
                                    'url': {
                                        'value': 'https://original.com/basic',
                                        'meta': {
                                            'timestamp': NOW_UTC.strftime(time_pattern)
                                        }
                                    },
                                    'supplier_info': {
                                        'name': 'basic supplier_info',
                                        'meta': {
                                            'timestamp': NOW_UTC.strftime(time_pattern)
                                        }
                                    },
                                },
                                'actual': {
                                    'url': {
                                        'value': 'https://actual.com/basic',
                                        'meta': {
                                            'timestamp': NOW_UTC.strftime(time_pattern)
                                        }
                                    },
                                    'sales_notes': {
                                        'value': 'actual sales_notes',
                                        'meta': {
                                            'timestamp': NOW_UTC.strftime(time_pattern)
                                        }
                                    },
                                    'quantity': {
                                        'min': 2,
                                        'step': 4,
                                        'meta': {
                                            'timestamp': NOW_UTC.strftime(time_pattern)
                                        }
                                    },
                                },
                                'original_terms': {
                                    'sales_notes': {
                                        'value': 'original_terms sales_notes',
                                        'meta': {
                                            'timestamp': NOW_UTC.strftime(time_pattern)
                                        }
                                    },
                                    'quantity': {
                                        'min': 1,
                                        'step': 2,
                                        'meta': {
                                            'timestamp': NOW_UTC.strftime(time_pattern)
                                        }
                                    },
                                }
                            }
                        },
                        'delivery': {
                            'calculator': {
                                'real_deliverycalc_generation': 1500,
                                'meta': {
                                    'timestamp': NOW_UTC.strftime(time_pattern)
                                }
                            },
                            'partner': {
                                'actual': {
                                    'pickup': {
                                        'flag': True,
                                        'meta': {
                                            'timestamp': NOW_UTC.strftime(time_pattern)
                                        }
                                    }
                                },
                                'original': {
                                    'pickup': {
                                        'flag': True,
                                        'meta': {
                                            'timestamp': NOW_UTC.strftime(time_pattern)
                                        }
                                    }
                                }
                            }
                        }
                    },
                    2: {
                        'identifiers': {
                            'business_id': 1,
                            'offer_id': 'o1',
                            'shop_id': 2,
                            'warehouse_id': 2,
                        },
                        'meta': {
                            'scope': DTC.SERVICE,
                            'rgb': DTC.BLUE,
                            'ts_created': NOW_UTC.strftime(time_pattern)
                        },
                        'price': {
                            'basic': {
                                'binary_price': {
                                    'price': 30
                                },
                                'meta': {
                                    'timestamp': NOW_UTC.strftime(time_pattern)
                                }
                            }
                        },
                    }
                },
            }]
        },
        ]
    },
    {
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': 2,
                        'offer_id': 'o1',
                    },
                    'meta': {
                        'scope': DTC.BASIC,
                        'ts_created': NOW_UTC.strftime(time_pattern)
                    },
                },
                'service': {
                    3: {
                        'identifiers': {
                            'business_id': 2,
                            'offer_id': 'o1',
                            'shop_id': 3,
                            'warehouse_id': 3,
                        },
                        'meta': {
                            'scope': DTC.SERVICE,
                            'rgb': DTC.BLUE,
                            'ts_created': NOW_UTC.strftime(time_pattern)
                        },
                        'status': {
                            'united_catalog': {
                                'flag': True,
                            }
                        },
                        'price': {
                            'basic': {
                                'binary_price': {
                                    'price': 30
                                },
                                'meta': {
                                    'timestamp': NOW_UTC.strftime(time_pattern)
                                }
                            }
                        },
                    },
                },
            }]
        }]
    },
]


def basic_offer_with_price(business_id, price=None):
    offer = {
        'identifiers': {
            'business_id': business_id,
            'offer_id': 'o1',
        },
    }
    if price:
        offer['price'] = {
            'basic': {
                'binary_price': {
                    'price': price
                },
            },
        }

    return message_from_data(offer, DTC.Offer())


def service_offers_with_price(business_id, white_shop_id=None, blue_shop_id=None, white_price=None, blue_price=None):
    result = []
    if white_price:
        result.append(message_from_data({
            'identifiers': {
                'business_id': business_id,
                'offer_id': 'o1',
                'shop_id': white_shop_id,
            },
            'meta': {
                'scope': DTC.SERVICE,
                'rgb': DTC.WHITE,
            },
            'price': {
                'basic': {
                    'binary_price': {
                        'price': white_price
                    },
                }
            },
        }, DTC.Offer()))
    if blue_price:
        result.append(message_from_data({
            'identifiers': {
                'business_id': business_id,
                'offer_id': 'o1',
                'shop_id': blue_shop_id,
            },
            'meta': {
                'scope': DTC.SERVICE,
                'rgb': DTC.BLUE,
            },
            'price': {
                'basic': {
                    'binary_price': {
                        'price': blue_price
                    },
                }
            },
        }, DTC.Offer()))

    return result


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
            'color': 'white',
        },
    }
    return PiperConfigMock(yt_server=yt_server, log_broker_stuff=log_broker_stuff, config=cfg)


@pytest.fixture(scope='module')
def basic_offers_table(yt_server, config):
    return DataCampBasicOffersTable(yt_server, config.yt_basic_offers_tablepath)


@pytest.fixture(scope='module')
def service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(yt_server, config.yt_service_offers_tablepath)


@pytest.fixture(scope='module')
def actual_service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(yt_server, config.yt_actual_service_offers_tablepath)


@pytest.yield_fixture(scope='module')
def piper(yt_server, log_broker_stuff, config, datacamp_messages_topic, basic_offers_table,
          service_offers_table, actual_service_offers_table):
    resources = {
        'config': config,
        'datacamp_messages_topic': datacamp_messages_topic,
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
    }
    with PiperTestEnv(yt_server, log_broker_stuff, **resources) as piper_env:
        piper_env.verify()
        yield piper_env


@pytest.fixture(scope='module')
def inserter(piper, datacamp_messages_topic):
    united_offers_processed = piper.united_offers_processed

    for message in DATACAMP_MESSAGES:
        datacamp_messages_topic.write(message_from_data(message, DatacampMessage()).SerializeToString())

    wait_until(lambda: piper.united_offers_processed >= united_offers_processed + len(DATACAMP_MESSAGES))


def test_update_united_offer(inserter,
                             config,
                             piper,
                             datacamp_messages_topic,
                             basic_offers_table,
                             service_offers_table,
                             actual_service_offers_table
                             ):
    expected_content = {
        'partner': {
            'original': {
                'url': {
                    'value': 'https://original.com/basic',
                },
                'supplier_info': {
                    'name': 'basic supplier_info',
                },
            },
            'original_terms': {
                'sales_notes': {
                    'value': 'original_terms sales_notes',
                },
                'quantity': {
                    'min': 1,
                    'step': 2,
                },
            },
            'actual': {
                'url': {
                    'value': 'https://actual.com/basic',
                },
                'sales_notes': {
                    'value': 'actual sales_notes',
                },
                'quantity': {
                    'min': 2,
                    'step': 4,
                },
            },
        }
    }

    expected_delivery = {
        'calculator': {
            'real_deliverycalc_generation': 1500,
        },
        'partner': {
            'actual': {
                'pickup': {
                    'flag': True,
                }
            },
            'original': {
                'pickup': {
                    'flag': True,
                }
            }
        }
    }

    expected_original_delivery = {
        'partner': {
            'original': expected_delivery['partner']['original']
        }
    }

    expected_actual_delivery = {
        'calculator': expected_delivery['calculator'],
        'partner': {
            'actual': expected_delivery['partner']['actual']
        }
    }

    basic_offers_table.load()
    assert_that(len(basic_offers_table.data), equal_to(2))
    # fields_placement_version по дефолту 1, но в базовую часть цена не копируется
    basic_offer = basic_offer_with_price(business_id=1)

    assert_that(basic_offers_table.data, HasOffers([basic_offer]))

    service_offers_table.load()
    assert_that(len(service_offers_table.data), equal_to(3))
    assert_that(service_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': 'o1',
                'shop_id': 1,
            },
            'price': {
                'basic': {
                    'binary_price': {
                        'price': 20
                    },
                }
            },
            'content': expected_content,
            'delivery': expected_original_delivery
        }, DTC.Offer()),
        message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': 'o1',
                'shop_id': 2,
            },
            'price': {
                'basic': {
                    'binary_price': {
                        'price': 30
                    },
                }
            },
        }, DTC.Offer())
    ]))

    actual_service_offers_table.load()

    assert_that(len(actual_service_offers_table.data), equal_to(3))
    assert_that(actual_service_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': 'o1',
                'warehouse_id': 0,
                'shop_id': 1,
            },
            'delivery': expected_actual_delivery,
        }, DTC.Offer()),
        message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': 'o1',
                'warehouse_id': 2,
                'shop_id': 2,
            },
        }, DTC.Offer())]))

    # 1) отправляем обновление состоящее только из базовой части
    update = {
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': 1,
                        'offer_id': 'o1',
                    },
                    'meta': {
                        'scope': DTC.BASIC,
                        'ts_created': NOW_UTC.strftime(time_pattern)
                    },
                    'price': {
                        'basic': {
                            'binary_price': {
                                'price': 40
                            },
                            'meta': {
                                'timestamp': FUTURE_UTC_1.strftime(time_pattern)
                            }
                        }
                    },
                },
            }]
        }],
    }

    united_offers_processed = piper.united_offers_processed
    datacamp_messages_topic.write(message_from_data(update, DatacampMessage()).SerializeToString())
    wait_until(lambda: piper.united_offers_processed >= united_offers_processed + 1)

    basic_offers_table.load()
    # TODO(isabirzyanov): MARKETINDEXER-38835
    # basic_offer = basic_offer_with_price(business_id=1, price=40)  # в базовой части прислали цену - в базовую и сохраним
    basic_offer = basic_offer_with_price(business_id=1)
    assert_that(basic_offers_table.data, HasOffers([basic_offer]))

    service_offers = service_offers_with_price(business_id=1, white_shop_id=1, blue_shop_id=2, white_price=20, blue_price=30)
    service_offers_table.load()
    assert_that(service_offers_table.data, HasOffers(service_offers))

    # 2) отправляем обновление цены в белой сервисной части (без флага)
    update = {
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': 1,
                        'offer_id': 'o1',
                    },
                },
                'service': {
                    1: {
                        'identifiers': {
                            'business_id': 1,
                            'offer_id': 'o1',
                            'shop_id': 1,
                        },
                        'meta': {
                            'rgb': DTC.WHITE,
                            'scope': DTC.SERVICE,
                            'ts_created': NOW_UTC.strftime(time_pattern)
                        },
                        'price': {
                            'basic': {
                                'binary_price': {
                                    'price': 50
                                },
                                'meta': {
                                    'timestamp': FUTURE_UTC_3.strftime(time_pattern)
                                }
                            }
                        },
                    },
                },
            }]
        }],
    }

    united_offers_processed = piper.united_offers_processed
    datacamp_messages_topic.write(message_from_data(update, DatacampMessage()).SerializeToString())
    wait_until(lambda: piper.united_offers_processed >= united_offers_processed + 1)

    basic_offers_table.load()
    # TODO(isabirzyanov): MARKETINDEXER-38835
    # basic_offer = basic_offer_with_price(business_id=1, price=40)  # старая цена из базового
    basic_offer = basic_offer_with_price(business_id=1)
    assert_that(basic_offers_table.data, HasOffers([basic_offer]))

    service_offers = service_offers_with_price(business_id=1, white_shop_id=1, blue_shop_id=2, white_price=50, blue_price=30)
    service_offers_table.load()
    assert_that(service_offers_table.data, HasOffers(service_offers))

    # 3) отправляем обновление цены в СИНЕЙ сервисной части,
    # не смотря на наличие флага, цена будет применена только на синий сервисный оффер
    update = {
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': 1,
                        'offer_id': 'o1',
                    },
                },
                'service': {
                    1: {
                        'identifiers': {
                            'business_id': 1,
                            'offer_id': 'o1',
                            'shop_id': 2,
                        },
                        'meta': {
                            'rgb': DTC.BLUE,
                            'scope': DTC.SERVICE,
                            'ts_created': NOW_UTC.strftime(time_pattern)
                        },
                        'price': {
                            'basic': {
                                'binary_price': {
                                    'price': 160
                                },
                                'meta': {
                                    'timestamp': FUTURE_UTC_4.strftime(time_pattern)
                                }
                            }
                        },
                        'status': {
                            'fields_placement_version': {
                                'value': 1
                            }
                        },
                    },
                },
            }]
        }],
    }

    united_offers_processed = piper.united_offers_processed
    datacamp_messages_topic.write(message_from_data(update, DatacampMessage()).SerializeToString())
    wait_until(lambda: piper.united_offers_processed >= united_offers_processed + 1)

    basic_offers_table.load()
    # TODO(isabirzyanov): MARKETINDEXER-38835
    # basic_offer = basic_offer_with_price(business_id=1, price=40)  # 40 - синий апдейт не трогает ее
    basic_offer = basic_offer_with_price(business_id=1)
    assert_that(basic_offers_table.data, HasOffers([basic_offer]))

    # белая 50 - остается прежней белой
    # синяя 160 - синий апдейт - только синяя сервисная цена изменилась
    service_offers = service_offers_with_price(business_id=1, white_shop_id=1, blue_shop_id=2, white_price=50, blue_price=160)
    service_offers_table.load()
    assert_that(service_offers_table.data, HasOffers(service_offers))


def test_update_united_offer_blue_with_united_catalog(inserter,
                                                      config,
                                                      piper,
                                                      datacamp_messages_topic,
                                                      basic_offers_table,
                                                      service_offers_table
                                                      ):
    """Тест проверяет, что для синего оффера единого каталога цена размножится в базовую часть"""
    basic_offers_table.load()
    # fields_placement_version по дефолту 1 =>
    # скопируем в базовую часть цену из синей сервисной части для Единого Каталого
    basic_offer = basic_offer_with_price(business_id=2)

    assert_that(basic_offers_table.data, HasOffers([basic_offer]))

    service_offers = service_offers_with_price(business_id=2, blue_shop_id=3, blue_price=30)
    service_offers_table.load()
    assert_that(service_offers_table.data, HasOffers(service_offers))
