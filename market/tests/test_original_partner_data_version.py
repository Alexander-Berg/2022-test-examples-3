# coding: utf-8

import pytest
from datetime import datetime, timedelta
from hamcrest import assert_that, equal_to, greater_than, less_than

import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock
from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.pylibrary.datacamp.utils import wait_until
from market.pylibrary.proto_utils import message_from_data
from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampBasicOffersTable, DataCampServiceOffersTable
from market.idx.yatf.matchers.yt_rows_matchers import HasOffers, deserialize_united_row
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.utils.utils import create_timestamp_from_json

NOW_UTC = datetime.utcnow()  # JSON serializer should always use UTC
time_pattern = "%Y-%m-%dT%H:%M:%SZ"
current_time = NOW_UTC.strftime(time_pattern)
current_ts = create_timestamp_from_json(current_time)
FUTURE_UTC_1 = NOW_UTC + timedelta(hours=10)
FUTURE_UTC_2 = NOW_UTC + timedelta(hours=20)

BUSINESS_ID = 1
SHOP_ID = 1
OFFER_ID = 'offer_1'


def offer_meta(scope=None):
    scope = DTC.SERVICE if scope == "service" else DTC.BASIC
    return {
        'scope': scope,
        'rgb': DTC.WHITE,
        'ts_created': NOW_UTC.strftime(time_pattern)
    }


def offer_ids(scope=None):
    ids = {
        'business_id': BUSINESS_ID,
        'offer_id': OFFER_ID,
    }
    if scope == "service":
        ids.update({'shop_id': SHOP_ID})
    return ids

DATACAMP_MESSAGES = [
    {
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': offer_ids(),
                    'meta': offer_meta(),
                },
                'service': {
                    1: {
                        'identifiers': offer_ids(scope="service"),
                        'meta': offer_meta(scope="service"),
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
                    },
                },
            }]
        },
        ]
    },
]


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


def write_message_prepare_for_check(update, piper, datacamp_messages_topic, basic_offers_table, service_offers_table):
    united_offers_processed = piper.united_offers_processed
    datacamp_messages_topic.write(message_from_data(update, DatacampMessage()).SerializeToString())
    wait_until(lambda: piper.united_offers_processed >= united_offers_processed + 1)
    basic_offers_table.load()
    service_offers_table.load()


def test_original_partner_data_version_updates(inserter,
                                               config,
                                               piper,
                                               datacamp_messages_topic,
                                               basic_offers_table,
                                               service_offers_table,
                                               actual_service_offers_table
                                               ):
    # 0) проверяем, что новый оффер записан в таблицу и все версии инициализированы
    basic_offers_table.load()
    service_offers_table.load()

    assert_that(len(basic_offers_table.data), equal_to(1))
    assert_that(len(service_offers_table.data), equal_to(1))

    assert_that(basic_offers_table.data, HasOffers([message_from_data({
        'identifiers': offer_ids()
    }, DTC.Offer())]))
    basic_offer_version = 0
    for row in basic_offers_table.data:
        offer = message_from_data(deserialize_united_row(row), DTC.Offer())
        if offer.identifiers.offer_id == OFFER_ID and offer.identifiers.business_id == BUSINESS_ID:
            basic_offer_version = offer.status.version.offer_version.counter
            assert_that(offer.status.version.offer_version.counter, greater_than(1000))
            assert_that(offer.status.version.actual_content_version.counter, greater_than(1000))
            assert_that(offer.status.version.uc_data_version.counter, greater_than(1000))
            assert_that(offer.status.version.master_data_version.counter, greater_than(1000))
            break

    assert_that(service_offers_table.data, HasOffers([message_from_data({
        'identifiers': offer_ids('service')
    }, DTC.Offer())]))
    service_offer_version = 0
    for row in service_offers_table.data:
        offer = message_from_data(deserialize_united_row(row), DTC.Offer())
        if offer.identifiers.offer_id == OFFER_ID and offer.identifiers.business_id == BUSINESS_ID and offer.identifiers.shop_id == SHOP_ID:
            service_offer_version = offer.status.version.offer_version.counter
            assert_that(offer.status.version.offer_version.counter, greater_than(1000))
            assert_that(offer.status.version.actual_content_version.counter, greater_than(1000))
            assert_that(offer.status.version.uc_data_version.counter, greater_than(1000))
            assert_that(offer.status.version.master_data_version.counter, greater_than(1000))
            assert_that(offer.status.version.original_partner_data_version.counter, equal_to(0))
            break

    # 1) отправляем обновление состоящее только из базовой части
    # убеждаемся, что в сервисной части не поднимается offer_version - тк в сервисной части ничего не изменилось,
    update = {
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': offer_ids(),
                    'meta': offer_meta(),
                    'content': {
                        'partner': {
                            'original': {
                                'description': {
                                    'value': 'https://original.com/basic',
                                    'meta': {
                                        'timestamp': NOW_UTC.strftime(time_pattern)
                                    }
                                }
                            }
                        }
                    },
                },
            }]
        }],
    }
    write_message_prepare_for_check(update, piper, datacamp_messages_topic, basic_offers_table, service_offers_table)

    for row in basic_offers_table.data:
        offer = message_from_data(deserialize_united_row(row), DTC.Offer())
        if offer.identifiers.offer_id == OFFER_ID and offer.identifiers.business_id == BUSINESS_ID:
            assert_that(offer.status.version.offer_version.counter, greater_than(basic_offer_version))
            assert_that(offer.status.version.actual_content_version.counter, equal_to(basic_offer_version))
            assert_that(offer.status.version.uc_data_version.counter, greater_than(basic_offer_version))
            assert_that(offer.status.version.master_data_version.counter, equal_to(basic_offer_version))
            basic_offer_version = offer.status.version.offer_version.counter
            break

    for row in service_offers_table.data:
        offer = message_from_data(deserialize_united_row(row), DTC.Offer())
        if offer.identifiers.offer_id == OFFER_ID and offer.identifiers.business_id == BUSINESS_ID and offer.identifiers.shop_id == SHOP_ID:
            assert_that(offer.status.version.offer_version.counter, equal_to(service_offer_version))
            assert_that(offer.status.version.actual_content_version.counter, equal_to(service_offer_version))
            assert_that(offer.status.version.uc_data_version.counter, equal_to(service_offer_version))
            assert_that(offer.status.version.master_data_version.counter, equal_to(service_offer_version))
            assert_that(offer.status.version.original_partner_data_version.counter, equal_to(0))
            break

    # 2) отправляем обновление состоящее только из сервисной части
    # убеждаемся, что в сервисной части поднимается
    # и offer_version - тк изменился сервисный оффер, и original_partner_data_version, тк изменилось поле из этого счетчика
    update = {
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': offer_ids()
                },
                'service': {
                    1: {
                        'identifiers': offer_ids(scope="service"),
                        'meta': offer_meta(scope="service"),
                        'delivery': {
                            'partner': {
                                'original': {
                                    'available': {
                                        'flag': True,
                                        'meta': {
                                            'timestamp': FUTURE_UTC_1.strftime(time_pattern)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }]
        }],
    }

    write_message_prepare_for_check(update, piper, datacamp_messages_topic, basic_offers_table, service_offers_table)

    for row in basic_offers_table.data:
        offer = message_from_data(deserialize_united_row(row), DTC.Offer())
        if offer.identifiers.offer_id == OFFER_ID and offer.identifiers.business_id == BUSINESS_ID:
            assert_that(offer.status.version.offer_version.counter, equal_to(basic_offer_version))
            assert_that(offer.status.version.actual_content_version.counter, less_than(basic_offer_version))
            assert_that(offer.status.version.uc_data_version.counter, equal_to(basic_offer_version))
            assert_that(offer.status.version.master_data_version.counter, less_than(basic_offer_version))
            basic_offer_version = offer.status.version.offer_version.counter
            break

    for row in service_offers_table.data:
        offer = message_from_data(deserialize_united_row(row), DTC.Offer())
        if offer.identifiers.offer_id == OFFER_ID and offer.identifiers.business_id == BUSINESS_ID and offer.identifiers.shop_id == SHOP_ID:
            assert_that(offer.status.version.offer_version.counter, greater_than(service_offer_version))
            assert_that(offer.status.version.actual_content_version.counter, equal_to(service_offer_version))
            assert_that(offer.status.version.uc_data_version.counter, equal_to(service_offer_version))
            assert_that(offer.status.version.master_data_version.counter, equal_to(service_offer_version))
            assert_that(offer.status.version.original_partner_data_version.counter, greater_than(service_offer_version))
            service_offer_version = offer.status.version.offer_version.counter
            break

    # 3) отправляем обновление состоящее из сервисной части, но только из цены (не входит в original_partner_data_version)
    # убеждаемся, что в сервисной части поднимается только offer_version
    update = {
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': offer_ids()
                },
                'service': {
                    1: {
                        'identifiers': offer_ids(scope="service"),
                        'meta': offer_meta(scope="service"),
                        'price': {
                            'basic': {
                                'binary_price': {
                                    'price': 30
                                },
                                'meta': {
                                    'timestamp': FUTURE_UTC_1.strftime(time_pattern)
                                }
                            }
                        },
                    }
                }
            }]
        }],
    }

    write_message_prepare_for_check(update, piper, datacamp_messages_topic, basic_offers_table, service_offers_table)

    for row in service_offers_table.data:
        offer = message_from_data(deserialize_united_row(row), DTC.Offer())
        if offer.identifiers.offer_id == OFFER_ID and offer.identifiers.business_id == BUSINESS_ID and offer.identifiers.shop_id == SHOP_ID:
            assert_that(offer.status.version.offer_version.counter, greater_than(service_offer_version))
            assert_that(offer.status.version.actual_content_version.counter, less_than(service_offer_version))
            assert_that(offer.status.version.uc_data_version.counter, less_than(service_offer_version))
            assert_that(offer.status.version.master_data_version.counter, less_than(service_offer_version))
            assert_that(offer.status.version.original_partner_data_version.counter, equal_to(service_offer_version))
            service_offer_version = offer.status.version.offer_version.counter
            break

    for row in basic_offers_table.data:
        offer = message_from_data(deserialize_united_row(row), DTC.Offer())
        if offer.identifiers.offer_id == OFFER_ID and offer.identifiers.business_id == BUSINESS_ID:
            assert_that(offer.status.version.offer_version.counter, equal_to(basic_offer_version))
            assert_that(offer.status.version.actual_content_version.counter, less_than(basic_offer_version))
            assert_that(offer.status.version.uc_data_version.counter, equal_to(basic_offer_version))
            assert_that(offer.status.version.master_data_version.counter, less_than(basic_offer_version))
            basic_offer_version = offer.status.version.offer_version.counter
            break

    # 4) отправляем обновление состоящее только из сервисной части (поле входит в original_partner_data_version)
    # убеждаемся, что original_partner_data_version не инкрементится (3=>4), а копируется из offer_version причем глобальной,
    # (те из offer_version базового оффера(5), а не offer_version сервисного(4))
    update = {
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': offer_ids()
                },
                'service': {
                    1: {
                        'identifiers': offer_ids(scope="service"),
                        'meta': offer_meta(scope="service"),
                        'delivery': {
                            'partner': {
                                'original': {
                                    'available': {
                                        'flag': False,
                                        'meta': {
                                            'timestamp': FUTURE_UTC_2.strftime(time_pattern)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }]
        }],
    }

    write_message_prepare_for_check(update, piper, datacamp_messages_topic, basic_offers_table, service_offers_table)

    for row in basic_offers_table.data:
        offer = message_from_data(deserialize_united_row(row), DTC.Offer())
        if offer.identifiers.offer_id == OFFER_ID and offer.identifiers.business_id == BUSINESS_ID:
            assert_that(offer.status.version.offer_version.counter, equal_to(basic_offer_version))
            assert_that(offer.status.version.actual_content_version.counter, less_than(basic_offer_version))
            assert_that(offer.status.version.uc_data_version.counter, equal_to(basic_offer_version))
            assert_that(offer.status.version.master_data_version.counter, less_than(basic_offer_version))
            basic_offer_version = offer.status.version.offer_version.counter
            break

    for row in service_offers_table.data:
        offer = message_from_data(deserialize_united_row(row), DTC.Offer())
        if offer.identifiers.offer_id == OFFER_ID and offer.identifiers.business_id == BUSINESS_ID and offer.identifiers.shop_id == SHOP_ID:
            assert_that(offer.status.version.offer_version.counter, greater_than(service_offer_version))
            service_offer_version = offer.status.version.offer_version.counter
            assert_that(offer.status.version.actual_content_version.counter, less_than(service_offer_version))
            assert_that(offer.status.version.uc_data_version.counter, less_than(service_offer_version))
            assert_that(offer.status.version.master_data_version.counter, less_than(service_offer_version))
            assert_that(offer.status.version.original_partner_data_version.counter, greater_than(basic_offer_version))
            break
