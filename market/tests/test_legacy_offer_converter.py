# coding: utf-8

import pytest
from datetime import datetime, timedelta
from hamcrest import assert_that, not_, is_not
from market.idx.yatf.matchers.yt_rows_matchers import HasOffers

import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC

from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampBusinessStatusTable, DataCampPartnersTable
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock
from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.datacamp.proto.business.Business_pb2 import BusinessStatus
from market.idx.datacamp.yatf.utils import dict2tskv
from market.idx.yatf.utils.utils import create_timestamp_from_json

from market.pylibrary.proto_utils import message_from_data

BLOCKED_BUSINESS = 1000
UNLOCKED_BUSINESS_SRC = 1001
UNLOCKED_BUSINESS_DST = 1002
UNLOCKED_BUSINESS_SHOP_ID = 1001
NOT_SET_BUSINESS_ID = 1003
NOT_SET_BUSINESS_SHOP_ID = 1004
NOW_UTC = datetime.utcnow()  # JSON serializer should always use UTC
PAST_UTC = NOW_UTC - timedelta(minutes=45)
FUTURE_UTC = NOW_UTC + timedelta(minutes=45)
time_pattern = "%Y-%m-%dT%H:%M:%SZ"

current_time = NOW_UTC.strftime(time_pattern)
current_ts = create_timestamp_from_json(current_time)


OFFERS = [
    # selective offer
    {
        'identifiers': {
            'shop_id': 2,
            'offer_id': '2',
            'business_id': 2,
        },
        'meta': {
            'ts_created': NOW_UTC.strftime(time_pattern),
            'rgb': DTC.WHITE,
            'scope': DTC.SELECTIVE,
        },
        'price': {
            'basic': {
                'meta': {
                    'timestamp': current_time,
                    'source': DTC.PUSH_PARTNER_FEED,
                },
            },
        },
        'status': {
            'disabled': [{
                'flag': True,
                'meta': {
                    'timestamp': current_time,
                    'source': DTC.PUSH_PARTNER_FEED,
                }
            }]
        },
        'content': {
            'partner': {
                'original': {
                    'name': {
                        'value': 'Name 2',
                        'meta': {
                            'timestamp': current_time,
                        },
                    },
                    'url': {
                        'value': 'www.url2.ru',
                        'meta': {
                            'timestamp': current_time,
                        },
                    },
                    'is_resale': {
                        'flag': True,
                        'meta': {
                            'timestamp': current_time,
                        },
                    },
                    'resale_condition': {
                        'value': DTC.ResaleCondition.PERFECT,
                        'meta': {
                            'timestamp': current_time,
                        },
                    },
                    'resale_reason': {
                        'value': DTC.ResaleReason.USED,
                        'meta': {
                            'timestamp': current_time,
                        },
                    },
                    'resale_description': {
                        'value': "foo",
                        'meta': {
                            'timestamp': current_time,
                        },
                    },
                },
                'actual': {
                    'title': {
                        'value': 'Name 2',
                        'meta': {
                            'timestamp': current_time,
                        },
                    },
                    'url': {
                        'value': 'www.url2.ru',
                        'meta': {
                            'timestamp': current_time,
                        },
                    },
                    'is_resale': {
                        'flag': True,
                        'meta': {
                            'timestamp': current_time,
                        },
                    },
                    'resale_condition': {
                        'value': DTC.ResaleCondition.PERFECT,
                        'meta': {
                            'timestamp': current_time,
                        },
                    },
                    'resale_reason': {
                        'value': DTC.ResaleReason.USED,
                        'meta': {
                            'timestamp': current_time,
                        },
                    },
                    'resale_description': {
                        'value': "foo",
                        'meta': {
                            'timestamp': current_time,
                        },
                    },
                },
                'original_terms': {
                    'quantity': {
                        'min': 20,
                        'meta': {
                            'timestamp': current_time,
                        },
                    },
                },
            },
            'market': {
                'category_id': 50,
                'meta': {
                    'timestamp': current_time
                },
            },
        },
    },
    # offer from blocked business
    {
        'identifiers': {
            'shop_id': BLOCKED_BUSINESS,
            'offer_id': 'blocked_business',
            'business_id': BLOCKED_BUSINESS,
        },
        'meta': {
            'ts_created': NOW_UTC.strftime(time_pattern),
            'rgb': DTC.WHITE,
            'scope': DTC.SELECTIVE,
        },
        'price': {
            'basic': {
                'meta': {
                    'timestamp': current_time,
                    'source': DTC.PUSH_PARTNER_FEED,
                },
            },
        },
    },
    # offer for just unlocked business
    {
        'identifiers': {
            'shop_id': UNLOCKED_BUSINESS_SHOP_ID,
            'offer_id': 'unlocked_business',
        },
        'meta': {
            'ts_created': NOW_UTC.strftime(time_pattern),
            'rgb': DTC.WHITE,
            'scope': DTC.SELECTIVE,
        },
        'price': {
            'basic': {
                'meta': {
                    'timestamp': current_time,
                    'source': DTC.PUSH_PARTNER_FEED,
                },
            },
        },
    },
    # offer without business_id
    {
        'identifiers': {
            'shop_id': NOT_SET_BUSINESS_SHOP_ID,
            'offer_id': 'without_business_id',
        },
        'meta': {
            'ts_created': NOW_UTC.strftime(time_pattern),
            'rgb': DTC.WHITE,
            'scope': DTC.SELECTIVE,
        },
        'price': {
            'basic': {
                'meta': {
                    'timestamp': current_time,
                    'source': DTC.PUSH_PARTNER_FEED,
                },
            },
        },
    },
]


@pytest.fixture(scope='module')
def business_status_table(yt_server, config):
    return DataCampBusinessStatusTable(yt_server, config.yt_business_status_tablepath, data=[
        {
            'business_id': BLOCKED_BUSINESS,
            'status': BusinessStatus(
                value=BusinessStatus.Status.LOCKED,
            ).SerializeToString(),
        },
        {
            'business_id': UNLOCKED_BUSINESS_SRC,
            'status': BusinessStatus(
                value=BusinessStatus.Status.UNLOCKED,
                shops={
                    UNLOCKED_BUSINESS_SHOP_ID: UNLOCKED_BUSINESS_DST
                }
            ).SerializeToString(),
        },
        {
            'business_id': UNLOCKED_BUSINESS_DST,
            'status': BusinessStatus(
                value=BusinessStatus.Status.UNLOCKED,
            ).SerializeToString(),
        },
    ])


@pytest.fixture(scope='module')
def offers():
    return [message_from_data(offer, DTC.Offer()) for offer in OFFERS]


@pytest.fixture(scope='module')
def partners_table(yt_server, config):
    return DataCampPartnersTable(
        yt_server,
        config.yt_partners_tablepath,
        data=[
            {
                'shop_id': UNLOCKED_BUSINESS_SHOP_ID,
                'mbi': '\n\n'.join([
                    dict2tskv({
                        'shop_id': UNLOCKED_BUSINESS_SHOP_ID,
                        'business_id': UNLOCKED_BUSINESS_SRC,
                    }),
                ]),
            },
            {
                'shop_id': NOT_SET_BUSINESS_SHOP_ID,
                'mbi': '\n\n'.join([
                    dict2tskv({
                        'shop_id': NOT_SET_BUSINESS_SHOP_ID,
                        'business_id': NOT_SET_BUSINESS_ID,
                    }),
                ]),
            },
        ]
    )


@pytest.fixture(scope='module')
def lbk_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def config(yt_server, log_broker_stuff, lbk_topic):
    cfg = {
        'logbroker': {
            'offers_topic': lbk_topic.topic,
        },
        'general': {
            'color': 'white',
        }
    }
    return PiperConfigMock(yt_server=yt_server,
                           log_broker_stuff=log_broker_stuff,
                           config=cfg)


@pytest.yield_fixture(scope='module')
def piper(yt_server, log_broker_stuff, config, lbk_topic, business_status_table, partners_table):
    resources = {
        'config': config,
        'offers_topic': lbk_topic,
        'business_status_table': business_status_table,
        'partners_table': partners_table,
    }
    with PiperTestEnv(yt_server, log_broker_stuff, **resources) as piper_env:
        piper_env.verify()
        yield piper_env


@pytest.fixture(scope='module')
def inserter(offers, piper, lbk_topic):
    for offer in offers:
        lbk_topic.write(offer.SerializeToString())

    wait_until(lambda: piper.united_offers_processed >= len(offers) - 1)


def test_offer_with_selective_scope_inserted_in_datacamp_tables(inserter, offers, piper):
    """Тест проверяет, что входной DatacampOffer со скоупом SELECTIVE
    будет корректно разложен на базовую и сервисную части"""
    assert_that(
        piper.basic_offers_table.data,
        HasOffers([message_from_data({
            'identifiers': {
                'business_id': 2,
                'offer_id': '2',
            },
            'meta': {
                'scope': DTC.BASIC,
            },
            'content': {
                'partner': {
                    'original': {
                        'name': {
                            'value': 'Name 2',
                        },
                        'is_resale': {
                            'flag': True,
                        },
                        'resale_condition': {
                            'value': DTC.ResaleCondition.PERFECT,
                        },
                        'resale_reason': {
                            'value': DTC.ResaleReason.USED,
                        },
                        'resale_description': {
                            'value': "foo",
                        },
                    },
                    'actual': {
                        'title': {
                            'value': 'Name 2',
                        },
                        'is_resale': {
                            'flag': True,
                        },
                        'resale_condition': {
                            'value': DTC.ResaleCondition.PERFECT,
                        },
                        'resale_reason': {
                            'value': DTC.ResaleReason.USED,
                        },
                        'resale_description': {
                            'value': "foo",
                        },
                    },
                },
            },
        }, DTC.Offer())]))

    # проверяем что quantity не попала в базовую
    assert_that(
        piper.basic_offers_table.data,
        not_(HasOffers([message_from_data({
            'identifiers': {
                'business_id': 2,
                'offer_id': '2',
            },
            'content': {
                'partner': {
                    'original_terms': {
                        'quantity': {
                            'min': 20,
                        },
                    },
                },
            },
        }, DTC.Offer())])))

    assert_that(
        piper.service_offers_table.data,
        HasOffers([message_from_data({
            'identifiers': {
                'business_id': 2,
                'offer_id': '2',
                'shop_id': 2,
            },
            'meta': {
                'scope': DTC.SERVICE,
                'rgb': DTC.WHITE,
            },
            'price': {
                'basic': {
                    'meta': {
                        'source': DTC.PUSH_PARTNER_FEED,
                    },
                },
            },
            'status': {
                'disabled': [{
                    'flag': True,
                    'meta': {
                        'source': DTC.PUSH_PARTNER_FEED,
                    }
                }]
            },
            'content': {
                'partner': {
                    'original': {
                        'url': {
                            'value': 'www.url2.ru',
                        },
                    },
                    'original_terms': {
                        'quantity': {
                            'min': 20,
                        },
                    },
                    'actual': {
                        'url': {
                            'value': 'www.url2.ru',
                        },
                    },
                },
            },
        }, DTC.Offer())]))

    for field in ['actual', 'original']:
        for subfield in ['is_resale', 'resale_condition', 'resale_reason', 'resale_description']:
            assert_that(
                piper.service_offers_table.data,
                not_(HasOffers([message_from_data({
                    'identifiers': {
                        'business_id': 2,
                        'offer_id': '2',
                        'shop_id': 2,
                    },
                    'meta': {
                        'scope': DTC.SERVICE,
                        'rgb': DTC.WHITE,
                    },
                    'content': {
                        'partner': {
                            field: {
                                subfield: {},
                            },
                        },
                    },
                }, DTC.Offer())])))

    assert_that(
        piper.actual_service_offers_table.data,
        HasOffers([message_from_data({
            'identifiers': {
                'business_id': 2,
                'offer_id': '2',
                'shop_id': 2,
            },
            'meta': {
                'scope': DTC.SERVICE,
                'rgb': DTC.WHITE,
            },
        }, DTC.Offer())]))

    assert_that(
        piper.actual_service_offers_table.data,
        not_(HasOffers([message_from_data({
            'identifiers': {
                'business_id': 2,
                'offer_id': '2',
                'shop_id': 2,
            },
            'meta': {
                'scope': DTC.SERVICE,
                'rgb': DTC.WHITE,
            },
            'content': {
                'market': {
                    'category_id': 50,
                },
            },
        }, DTC.Offer())])))


def test_update_restriction_for_blocked_business(inserter, offers, piper):
    """ Проверяем, что оффер заблокированного бизнеса не записывается в таблицы """
    assert_that(piper.basic_offers_table.data, is_not(HasOffers([message_from_data({
        'identifiers': {
            'business_id': BLOCKED_BUSINESS,
            'offer_id': 'blocked_business',
        }
    }, DTC.Offer())])))
    assert_that(piper.service_offers_table.data, is_not(HasOffers([message_from_data({
        'identifiers': {
            'business_id': BLOCKED_BUSINESS,
            'offer_id': 'blocked_business',
        }
    }, DTC.Offer())])))
    assert_that(piper.actual_service_offers_table.data, is_not(HasOffers([message_from_data({
        'identifiers': {
            'business_id': BLOCKED_BUSINESS,
            'offer_id': 'blocked_business',
        }
    }, DTC.Offer())])))


def test_just_unlocked_business(inserter, offers, piper):
    """ Проверяем, что для только что разблокированного бизнеса подменяется business_id """
    assert_that(piper.basic_offers_table.data, HasOffers([message_from_data({
        'identifiers': {
            'business_id': UNLOCKED_BUSINESS_DST,
            'offer_id': 'unlocked_business',
        }
    }, DTC.Offer())]))
    assert_that(piper.service_offers_table.data, HasOffers([message_from_data({
        'identifiers': {
            'business_id': UNLOCKED_BUSINESS_DST,
            'offer_id': 'unlocked_business',
        }
    }, DTC.Offer())]))
    assert_that(piper.actual_service_offers_table.data, HasOffers([message_from_data({
        'identifiers': {
            'business_id': UNLOCKED_BUSINESS_DST,
            'offer_id': 'unlocked_business',
        }
    }, DTC.Offer())]))


def test_not_set_united_catalog(inserter, offers, piper):
    """ Проверяем, что для оффера, у которого нет business_id и который
    не мигрирует из бизнеса в бизнес, флаг united_catalog == false """
    assert_that(piper.service_offers_table.data, HasOffers([message_from_data({
        'identifiers': {
            'business_id': NOT_SET_BUSINESS_ID,
            'offer_id': 'without_business_id',
        },
        'status': {
            'united_catalog': {
                'flag': False
            }
        }
    }, DTC.Offer())]))
