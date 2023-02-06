# coding: utf-8

import pytest
from datetime import datetime
from hamcrest import assert_that, equal_to, is_not

import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock
from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.datacamp.yatf.utils import dict2tskv
from market.idx.yatf.matchers.yt_rows_matchers import HasOffers
from market.idx.yatf.resources.datacamp.datacamp_tables import (
    DataCampBasicOffersTable,
    DataCampServiceOffersTable,
    DataCampPartnersTable,
)
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.pylibrary.proto_utils import message_from_data

NOW_UTC = datetime.utcnow()  # JSON serializer should always use UTC
time_pattern = "%Y-%m-%dT%H:%M:%SZ"


def create_offer(business_id, shop_id, warehouse_id=0):
    return {
        'basic': {
            'identifiers': {
                'business_id': business_id,
                'offer_id': 'o1',
            },
            'content': {
                'binding': {
                    'approved': {
                        'market_category_id': 7812201,
                        'meta': {
                            'timestamp': NOW_UTC.strftime(time_pattern),
                            'source': DTC.MARKET_MBO,
                        },
                    },
                },
                'status': {
                    'content_system_status': {
                        'meta': {
                            'timestamp': NOW_UTC.strftime(time_pattern),
                            'source': DTC.MARKET_MBO,
                        },
                        'cpa_state': DTC.CONTENT_STATE_READY,
                    },
                },
            },
        },
        'service' : {
            shop_id: {
                'identifiers': {
                    'business_id': business_id,
                    'offer_id': 'o1',
                    'shop_id': shop_id,
                    'warehouse_id': warehouse_id,
                },
                'content': {
                    'status': {
                        'content_system_status': {
                            'meta': {
                                'timestamp': NOW_UTC.strftime(time_pattern),
                                'source': DTC.MARKET_MBO,
                            },
                            'service_offer_state': DTC.CONTENT_STATE_CONTENT_PROCESSING,
                            'cpa_state': DTC.CONTENT_STATE_READY,
                        },
                    },
                },
                'delivery': {
                    'market': {
                        'use_yml_delivery': {
                            'flag': True,
                            'meta': {
                                'timestamp': NOW_UTC.strftime(time_pattern),
                            },
                        },
                    },
                },
            },
        },
    }

DATACAMP_MESSAGES = [
    {
        'united_offers': [{
            # white offer
            'offer': [create_offer(business_id=1, shop_id=1032137)],
        },
        {
            # blue offer from united catalog
            'offer': [create_offer(business_id=2, shop_id=1032138, warehouse_id=1456)],
        },
        {
            # blue offer not from united catalog
            'offer': [create_offer(business_id=3, shop_id=1032139, warehouse_id=1456)],
        }]
    }
]


@pytest.fixture(scope='module')
def datacamp_messages_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module', params=['blue', 'white'])
def color(request):
    return request.param


@pytest.fixture(scope='module')
def config(yt_server, log_broker_stuff, datacamp_messages_topic, color):
    cfg = {
        'logbroker': {
            'datacamp_messages_topic': datacamp_messages_topic.topic,
        },
        'general': {
            'color': color,
        },
    }
    return PiperConfigMock(yt_server=yt_server, log_broker_stuff=log_broker_stuff, config=cfg)


@pytest.fixture(scope='module')
def partners_table(yt_server, config):
    return DataCampPartnersTable(
        yt_server,
        config.yt_partners_tablepath,
        data=[
            # original white shop
            {
                'shop_id': 1032137,
                'mbi': '\n\n'.join([
                    dict2tskv({
                        'shop_id': 1032137,
                        'business_id': 1,
                        'united_catalog_status': 'SUCCESS',
                    }),
                ]),
            },
            # blue shop in united_catalog
            {
                'shop_id': 1032138,
                'mbi': '\n\n'.join([
                    dict2tskv({
                        'shop_id': 1032138,
                        'business_id': 2,
                        'blue_status': 'REAL',
                        'united_catalog_status': 'SUCCESS',
                        'warehouse_id': 1456,
                    }),
                ]),
            },
            # blue shop not in united_catalog
            {
                'shop_id': 1032139,
                'mbi': '\n\n'.join([
                    dict2tskv({
                        'shop_id': 1032139,
                        'business_id': 3,
                        'blue_status': 'REAL',
                        'united_catalog_status': 'NO',
                        'warehouse_id': 1456,
                    }),
                ]),
            },
        ],
    )


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
          service_offers_table, actual_service_offers_table, partners_table):
    resources = {
        'config': config,
        'datacamp_messages_topic': datacamp_messages_topic,
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
        'partners_table': partners_table,
    }
    with PiperTestEnv(yt_server, log_broker_stuff, **resources) as piper_env:
        piper_env.verify()
        yield piper_env


@pytest.fixture(scope='module')
def inserter(piper, datacamp_messages_topic):
    united_offers = 0
    for message in DATACAMP_MESSAGES:
        for offers in message['united_offers']:
            united_offers += len(offers)
        datacamp_messages_topic.write(message_from_data(message, DatacampMessage()).SerializeToString())

    wait_until(lambda: piper.united_offers_processed >= united_offers)


def test_basic_content_was_updated(inserter, config, basic_offers_table):
    """Тест проверяет, что ответ по базовой части от MBOC корректно записался в базовую таблицу"""
    def _expected_basic_offer(business_id):
        return {
            'identifiers': {
                'business_id': business_id,
                'offer_id': 'o1',
            },
            'content': {
                'binding': {
                    'approved': {
                        'market_category_id': 7812201,
                        'meta': {
                            'source': DTC.MARKET_MBO,
                        },
                    },
                },
                'status': {
                    'content_system_status': {
                        'cpa_state': DTC.CONTENT_STATE_READY,
                        'meta': {
                            'source': DTC.MARKET_MBO,
                        },
                    },
                },
            },
        }

    basic_offers_table.load()
    assert_that(len(basic_offers_table.data), equal_to(3))
    # проверяем данные которые должны были пролезть в базовую
    assert_that(basic_offers_table.data, HasOffers([
        message_from_data(_expected_basic_offer(1), DTC.Offer()),
        message_from_data(_expected_basic_offer(2), DTC.Offer()),
        message_from_data(_expected_basic_offer(3), DTC.Offer()),
    ]))


def test_service_content_was_updated(inserter, config, service_offers_table):
    """Тест проверяет, что ответ по сервисной части от MBOC корректно записался в сервисную таблицу"""
    def _expected_service_offer(business_id, shop_id):
        return {
            'identifiers': {
                'business_id': business_id,
                'offer_id': 'o1',
                'shop_id': shop_id,
            },
            'content': {
                'status': {
                    'content_system_status': {
                        'service_offer_state': DTC.CONTENT_STATE_CONTENT_PROCESSING,
                        'meta': {
                            'source': DTC.MARKET_MBO,
                        },
                    },
                },
            },
        }

    def _not_expected_service_offer(business_id, shop_id):
        return {
            'identifiers': {
                'business_id': business_id,
                'offer_id': 'o1',
                'shop_id': shop_id
            },
            'content': {
                'status': {
                    'content_system_status': {
                        'cpa_state': DTC.CONTENT_STATE_READY,
                    },
                },
            },
        }

    service_offers_table.load()
    assert_that(len(service_offers_table.data), equal_to(3))
    # проверяем данные которые должны были пролезть в сервисную
    assert_that(service_offers_table.data, HasOffers([
        message_from_data(_expected_service_offer(1, 1032137), DTC.Offer()),
        message_from_data(_expected_service_offer(2, 1032138), DTC.Offer()),
        message_from_data(_expected_service_offer(3, 1032139), DTC.Offer()),
    ]))

    # проверяем, что ничего лишнего не пролезло
    assert_that(service_offers_table.data, is_not(HasOffers([
        message_from_data(_not_expected_service_offer(1, 1032137), DTC.Offer())])))
    assert_that(service_offers_table.data, is_not(HasOffers([
        message_from_data(_not_expected_service_offer(2, 1032138), DTC.Offer())])))
    assert_that(service_offers_table.data, HasOffers([
        message_from_data(_not_expected_service_offer(3, 1032139), DTC.Offer())]))


def test_actual_service_content_was_updated(inserter, config, actual_service_offers_table):
    """Тест проверяет, что в актуальную сервисную часть от MBOC не прорастают данные"""
    def _expected_actual_service_offer(business_id, shop_id):
        return {
            'identifiers': {
                'business_id': business_id,
                'offer_id': 'o1',
                'shop_id': shop_id,
            },
            'delivery': {
                'market': {
                    'use_yml_delivery': {
                        'flag': True,
                    },
                },
            },
        }

    def _not_expected_actual_service_offer(business_id, shop_id):
        return {
            'identifiers': {
                'business_id': business_id,
                'offer_id': 'o1',
                'shop_id': shop_id,
            },
            'content': {
                'status': {
                    'content_system_status': {
                        'meta': {
                            'source': DTC.MARKET_MBO,
                        },
                        'cpa_state': DTC.CONTENT_STATE_READY,
                        'service_offer_state': DTC.CONTENT_STATE_CONTENT_PROCESSING,
                    },
                },
            },
        }

    actual_service_offers_table.load()
    assert_that(len(actual_service_offers_table.data), equal_to(3))
    # проверяем данные которые должны были пролезть в актуальную сервисную
    assert_that(actual_service_offers_table.data, HasOffers([
        message_from_data(_expected_actual_service_offer(1, 1032137), DTC.Offer()),
        message_from_data(_expected_actual_service_offer(2, 1032138), DTC.Offer()),
        message_from_data(_expected_actual_service_offer(3, 1032139), DTC.Offer()),
    ]))

    # проверяем, что ничего лишнего не пролезло
    assert_that(actual_service_offers_table.data, is_not(HasOffers([
        message_from_data(_not_expected_actual_service_offer(1, 1032137), DTC.Offer())
    ])))
    assert_that(actual_service_offers_table.data, is_not(HasOffers([
        message_from_data(_not_expected_actual_service_offer(2, 1032138), DTC.Offer())
    ])))
    assert_that(actual_service_offers_table.data, is_not(HasOffers([
        message_from_data(_not_expected_actual_service_offer(3, 1032139), DTC.Offer())
    ])))
