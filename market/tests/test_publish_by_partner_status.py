# coding: utf-8

import pytest
from datetime import datetime, timedelta
from hamcrest import assert_that

from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock
from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.datacamp.yatf.utils import create_update_meta
from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampBasicOffersTable, DataCampServiceOffersTable
from market.idx.yatf.matchers.yt_rows_matchers import HasOffers
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.utils.utils import create_timestamp_from_json
from market.idx.pylibrary.datacamp.conversion import offer_to_basic_row, offer_to_service_row

from market.pylibrary.proto_utils import message_from_data

NOW_UTC = datetime.utcnow()  # JSON serializer should always use UTC
time_pattern = "%Y-%m-%dT%H:%M:%SZ"
current_time = NOW_UTC.strftime(time_pattern)
current_ts = create_timestamp_from_json(current_time)
FUTURE_UTC = NOW_UTC + timedelta(minutes=10)
future_time = FUTURE_UTC.strftime(time_pattern)
future_ts = create_timestamp_from_json(future_time)
OLD_UTC = NOW_UTC + timedelta(minutes=10)
old_time = OLD_UTC.strftime(time_pattern)
old_ts = create_timestamp_from_json(old_time)


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
                        'ts_created': current_time,
                    },
                },
                'service': {
                    # new offer which is disabled by partner
                    1: {
                        'identifiers': {
                            'business_id': 1,
                            'offer_id': 'o1',
                            'shop_id': 1,
                        },
                        'meta': {
                            'scope': DTC.SERVICE,
                            'rgb': DTC.WHITE,
                            'ts_created': current_time,
                        },
                        'status': {
                            'disabled': [
                                {
                                    'flag': True,
                                    'meta': {
                                        'source': DTC.PUSH_PARTNER_FEED,
                                        'timestamp': current_time,
                                    },
                                },
                            ],
                        },
                    },
                    # new offer which is enabled by partner
                    2: {
                        'identifiers': {
                            'business_id': 1,
                            'offer_id': 'o1',
                            'shop_id': 2,
                        },
                        'meta': {
                            'scope': DTC.SERVICE,
                            'rgb': DTC.WHITE,
                            'ts_created': current_time,
                        },
                        'status': {
                            'disabled': [
                                {
                                    'flag': False,
                                    'meta': {
                                        'source': DTC.PUSH_PARTNER_FEED,
                                        'timestamp': current_time,
                                    },
                                },
                            ],
                        },
                    },
                    # new offer with no flags by partner
                    3: {
                        'identifiers': {
                            'business_id': 1,
                            'offer_id': 'o1',
                            'shop_id': 3,
                        },
                        'meta': {
                            'scope': DTC.SERVICE,
                            'rgb': DTC.WHITE,
                            'ts_created': current_time,
                        },
                    },
                    # old offer with new disable partner-flag (different partner sources)
                    # publish_by_partner : AVAILABLE -> HIDDEN
                    4: {
                        'identifiers': {
                            'business_id': 1,
                            'offer_id': 'o1',
                            'shop_id': 4,
                        },
                        'meta': {
                            'scope': DTC.SERVICE,
                            'rgb': DTC.WHITE,
                            'ts_created': current_time,
                        },
                        'status': {
                            'disabled': [
                                {
                                    'flag': True,
                                    'meta': {
                                        'source': DTC.PUSH_PARTNER_OFFICE,
                                        'timestamp': future_time,
                                    },
                                },
                            ],
                        },
                    },
                    # old offer with new disable partner-flag (fresh timestamp)
                    # publish_by_partner : AVAILABLE -> HIDDEN
                    5: {
                        'identifiers': {
                            'business_id': 1,
                            'offer_id': 'o1',
                            'shop_id': 5,
                        },
                        'meta': {
                            'scope': DTC.SERVICE,
                            'rgb': DTC.WHITE,
                            'ts_created': current_time,
                        },
                        'status': {
                            'disabled': [
                                {
                                    'flag': True,
                                    'meta': {
                                        'source': DTC.PUSH_PARTNER_FEED,
                                        'timestamp': future_time,
                                    },
                                },
                            ],
                        },
                    },
                    # old offer with new enable partner-flag (fresh timestamp)
                    # publish_by_partner : HIDDEN -> AVAILABLE
                    6: {
                        'identifiers': {
                            'business_id': 1,
                            'offer_id': 'o1',
                            'shop_id': 6,
                        },
                        'meta': {
                            'scope': DTC.SERVICE,
                            'rgb': DTC.WHITE,
                            'ts_created': current_time,
                        },
                        'status': {
                            'disabled': [
                                {
                                    'flag': False,
                                    'meta': {
                                        'source': DTC.PUSH_PARTNER_FEED,
                                        'timestamp': future_time,
                                    },
                                },
                            ],
                        },
                    },
                    # old offer with new disable partner-flag (new source, old timestamp)
                    # publish_by_partner : HIDDEN -> HIDDEN
                    # new value for disabled_by_partner_since_ts
                    7: {
                        'identifiers': {
                            'business_id': 1,
                            'offer_id': 'o1',
                            'shop_id': 7,
                        },
                        'meta': {
                            'scope': DTC.SERVICE,
                            'rgb': DTC.WHITE,
                            'ts_created': current_time,
                        },
                        'status': {
                            'disabled': [
                                {
                                    'flag': True,
                                    'meta': {
                                        'source': DTC.PUSH_PARTNER_OFFICE,
                                        'timestamp': future_time,
                                    },
                                },
                            ],
                        },
                    },
                }
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
    return DataCampBasicOffersTable(yt_server, config.yt_basic_offers_tablepath, data=[
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='o1',
                shop_id=4,
            ),
            meta=DTC.OfferMeta(
                scope=DTC.BASIC,
                rgb=DTC.WHITE,
            ),
        ))])


@pytest.fixture(scope='module')
def service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(yt_server, config.yt_service_offers_tablepath, data=[
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='o1',
                shop_id=4,
            ),
            meta=DTC.OfferMeta(
                scope=DTC.SERVICE,
                rgb=DTC.WHITE,
            ),
            status=DTC.OfferStatus(
                publish_by_partner=DTC.AVAILABLE,
                disabled=[
                    DTC.Flag(
                        flag=False,
                        meta=create_update_meta(current_ts.seconds, DTC.PUSH_PARTNER_FEED),
                    ),
                ],
            )
        )),
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='o1',
                shop_id=5,
            ),
            meta=DTC.OfferMeta(
                scope=DTC.SERVICE,
                rgb=DTC.WHITE,
            ),
            status=DTC.OfferStatus(
                publish_by_partner=DTC.AVAILABLE,
                disabled=[
                    DTC.Flag(
                        flag=False,
                        meta=create_update_meta(current_ts.seconds, DTC.PUSH_PARTNER_FEED),
                    ),
                ],
            )
        )),
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='o1',
                shop_id=6,
            ),
            meta=DTC.OfferMeta(
                scope=DTC.SERVICE,
                rgb=DTC.WHITE,
            ),
            status=DTC.OfferStatus(
                publish_by_partner=DTC.HIDDEN,
                disabled_by_partner_since_ts=current_ts,
                disabled=[
                    DTC.Flag(
                        flag=True,
                        meta=create_update_meta(current_ts.seconds, DTC.PUSH_PARTNER_FEED),
                    ),
                ],
            )
        )),
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='o1',
                shop_id=7,
            ),
            meta=DTC.OfferMeta(
                scope=DTC.SERVICE,
                rgb=DTC.WHITE,
            ),
            status=DTC.OfferStatus(
                publish_by_partner=DTC.HIDDEN,
                disabled_by_partner_since_ts=current_ts,
                disabled=[
                    DTC.Flag(
                        flag=True,
                        meta=create_update_meta(current_ts.seconds, DTC.PUSH_PARTNER_FEED),
                    ),
                ],
            )
        ))])


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


def test_new_offer_disabled_by_partner(inserter, config, piper, datacamp_messages_topic,
                                       basic_offers_table, service_offers_table, actual_service_offers_table):
    """Тест проверяет, что новый оффер отключенный партнером получит правильные значения publish_by_partner
     и disabled_by_partner_since_ts"""
    service_offers_table.load()
    assert_that(service_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': 'o1',
                'shop_id': 1,
            },
            'status': {
                'publish_by_partner': DTC.HIDDEN,
                'disabled_by_partner_since_ts': current_time,
                'disabled': [
                    {
                        'flag': True,
                        'meta': {
                            'source': DTC.PUSH_PARTNER_FEED,
                            'timestamp': current_time
                        },
                    },
                ],
            },
        }, DTC.Offer())]))


def test_new_offer_enabled_by_partner(inserter, config, piper, datacamp_messages_topic,
                                      basic_offers_table, service_offers_table, actual_service_offers_table):
    """Тест проверяет, что новый оффер включенный партнером получит правильные значения publish_by_partner
     и не получит значения disabled_by_partner_since_ts"""
    service_offers_table.load()
    assert_that(service_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': 'o1',
                'shop_id': 2,
            },
            'status': {
                'publish_by_partner': DTC.AVAILABLE,
                'disabled_by_partner_since_ts': None,
                'disabled': [
                    {
                        'flag': False,
                        'meta': {
                            'source': DTC.PUSH_PARTNER_FEED,
                            'timestamp': current_time
                        },
                    },
                ],
            },
        }, DTC.Offer())]))


def test_new_offer_without_any_flags_by_partner(inserter, config, piper, datacamp_messages_topic,
                                                basic_offers_table, service_offers_table, actual_service_offers_table):
    """Тест проверяет, что новый оффер вообще без партнерских флажков включения получит правильные значения publish_by_partner
     и не получит значения disabled_by_partner_since_ts"""
    service_offers_table.load()
    assert_that(service_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': 'o1',
                'shop_id': 3,
            },
            'status': {
                'publish_by_partner': DTC.AVAILABLE,
                'disabled_by_partner_since_ts': None,
                'disabled': [],
            },
        }, DTC.Offer())]))


def test_old_enabled_offer_with_disabled_by_partner_new_source(inserter, config, piper, datacamp_messages_topic,
                                                               basic_offers_table, service_offers_table, actual_service_offers_table):
    """Тест проверяет, что если оффер был включен партнером, и прийдет отключение с новым партнерским источником,
    то он получит правильные значения publish_by_partner и disabled_by_partner_since_ts"""
    service_offers_table.load()
    assert_that(service_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': 'o1',
                'shop_id': 4,
            },
            'status': {
                'publish_by_partner': DTC.HIDDEN,
                'disabled_by_partner_since_ts': future_time,
                'disabled': [
                    {
                        'flag': True,
                        'meta': {
                            'source': DTC.PUSH_PARTNER_OFFICE,
                            'timestamp': future_time
                        },
                    },
                ],
            },
        }, DTC.Offer())]))


def test_old_enabled_offer_disabled_by_partner_new_time(inserter, config, piper, datacamp_messages_topic,
                                                        basic_offers_table, service_offers_table, actual_service_offers_table):
    """Тест проверяет, что если оффер был включен партнером, и прийдет отключение с уже существующим партнерским источником и свежим ts,
    то значения publish_by_partner и disabled_by_partner_since_ts не изменятся"""
    service_offers_table.load()
    assert_that(service_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': 'o1',
                'shop_id': 5,
            },
            'status': {
                'publish_by_partner': DTC.HIDDEN,
                'disabled_by_partner_since_ts': future_time,
                'disabled': [
                    {
                        'flag': True,
                        'meta': {
                            'source': DTC.PUSH_PARTNER_FEED,
                            'timestamp': future_time
                        },
                    },
                ],
            },
        }, DTC.Offer())
    ]))


def test_old_disabled_offer_enabled_by_partner_new_time(inserter, config, piper, datacamp_messages_topic,
                                                        basic_offers_table, service_offers_table, actual_service_offers_table):
    """Тест проверяет, что если оффер был выключен партнером, и прийдет включение,
    то он получит правильные значения publish_by_partner и удаление disabled_by_partner_since_ts"""
    service_offers_table.load()
    assert_that(service_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': 'o1',
                'shop_id': 6,
            },
            'status': {
                'publish_by_partner': DTC.AVAILABLE,
                'disabled_by_partner_since_ts': None,
                'disabled': [
                    {
                        'flag': False,
                        'meta': {
                            'source': DTC.PUSH_PARTNER_FEED,
                            'timestamp': future_time
                        },
                    },
                ],
            },
        }, DTC.Offer())]))


def test_old_disabled_offer_with_disabled_by_partner_new_source(inserter, config, piper, datacamp_messages_topic,
                                                                basic_offers_table, service_offers_table, actual_service_offers_table):
    """Тест проверяет, что если оффер был выключен партнером, и прийдет отключение с новым партнерским источником,
    то значения publish_by_partner и disabled_by_partner_since_ts не изменятся"""
    service_offers_table.load()
    assert_that(service_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': 'o1',
                'shop_id': 7,
            },
            'status': {
                'publish_by_partner': DTC.HIDDEN,
                'disabled_by_partner_since_ts': current_time,
                'disabled': [
                    {
                        'flag': True,
                        'meta': {
                            'source': DTC.PUSH_PARTNER_OFFICE,
                            'timestamp': future_time
                        },
                    },
                ],
            },
        }, DTC.Offer())]))
