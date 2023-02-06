# coding: utf-8

import pytest
from hamcrest import assert_that, equal_to, is_not

import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock
from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.yatf.matchers.yt_rows_matchers import HasOffers
from market.idx.yatf.resources.datacamp.datacamp_tables import (
    DataCampBasicOffersTable,
    DataCampServiceOffersTable,
)
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.pylibrary.datacamp.conversion import offer_to_basic_row, offer_to_service_row
from market.pylibrary.proto_utils import message_from_data


def make_united_offer_update(offer_id):
    return {
        'basic': {
            'identifiers': {
                'business_id': 1,
                'offer_id': offer_id,
                'extra': {
                    'ware_md5': 'basic_ware_md5',
                    'classifier_magic_id2': 'basic_classifier_magic_id2',
                    'classifier_good_id': 'basic_classifier_good_id',
                }
            },
            'meta': {
                'scope': DTC.BASIC
            }
        },
        'service': {
            1: {
                'identifiers': {
                    'business_id': 1,
                    'shop_id': 1,
                    'offer_id': offer_id,
                    'warehouse_id': 145,
                    'extra': {
                        'ware_md5': 'service_ware_md5',
                        'classifier_magic_id2': 'service_classifier_magic_id2',
                        'classifier_good_id': 'service_classifier_good_id',
                    }
                },
                'meta': {
                    'scope': DTC.SERVICE
                }
            },
        }
    }


DATACAMP_MESSAGES = [
    {
        'united_offers': [{
            'offer': [make_united_offer_update('offer.with.extra.in.update')]
        }]
    }
]

DATACAMP_MESSAGES_FROM_MINER = [
    {
        'united_offers': [{
            'offer': [make_united_offer_update('offer.with.extra.in.update.from.miner')]
        }]
    }
]


@pytest.fixture(scope='module')
def datacamp_messages_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def miner_input_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def config(yt_server, log_broker_stuff, datacamp_messages_topic, miner_input_topic):
    cfg = {
        'logbroker': {
            'datacamp_messages_topic': datacamp_messages_topic.topic,
            'miner_input_topic': miner_input_topic.topic
        },
    }
    return PiperConfigMock(yt_server=yt_server, log_broker_stuff=log_broker_stuff, config=cfg)


@pytest.fixture(scope='module')
def basic_offers_table(yt_server, config):
    return DataCampBasicOffersTable(yt_server, config.yt_basic_offers_tablepath, data=[
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id=offer_id,
                extra=DTC.OfferExtraIdentifiers(
                    classifier_magic_id2='basic_classifier_magic_id2_old',
                    classifier_good_id='basic_classifier_good_id_old',
                )
            ),
        )) for offer_id in ['offer.with.extra.in.update', 'offer.with.extra.in.update.from.miner']
    ])


@pytest.fixture(scope='module')
def actual_service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(yt_server, config.yt_actual_service_offers_tablepath, data=[
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id=offer_id,
                shop_id=1,
                warehouse_id=145,
                extra=DTC.OfferExtraIdentifiers(
                    ware_md5='service_ware_md5_old',
                    classifier_magic_id2='service_classifier_magic_id2_old',
                    classifier_good_id='service_classifier_good_id_old',
                )
            ),
            meta=DTC.OfferMeta(
                scope=DTC.SERVICE,
                rgb=DTC.WHITE
            )
        )) for offer_id in ['offer.with.extra.in.update', 'offer.with.extra.in.update.from.miner']
    ])


@pytest.yield_fixture(scope='module')
def piper(
    yt_server,
    log_broker_stuff,
    config,
    datacamp_messages_topic,
    miner_input_topic,
    basic_offers_table,
    actual_service_offers_table
):
    resources = {
        'config': config,
        'datacamp_messages_topic': datacamp_messages_topic,
        'miner_input_topic': miner_input_topic,
        'basic_offers_table': basic_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
    }
    with PiperTestEnv(
        yt_server,
        log_broker_stuff,
        update_only=True,
        **resources
    ) as piper_env:
        piper_env.verify()
        yield piper_env


@pytest.fixture(scope='module')
def inserter(piper, datacamp_messages_topic, miner_input_topic):
    united_offers = 0
    for message in DATACAMP_MESSAGES:
        for offers in message['united_offers']:
            united_offers += len(offers)
        datacamp_messages_topic.write(message_from_data(message, DatacampMessage()).SerializeToString())
    for message in DATACAMP_MESSAGES_FROM_MINER:
        for offers in message['united_offers']:
            united_offers += len(offers)
        miner_input_topic.write(message_from_data(message, DatacampMessage()).SerializeToString())

    wait_until(lambda: piper.united_offers_processed >= united_offers)


def test_basic_extra_ids_protection(inserter, config, basic_offers_table):
    """ Защита экстра идентификаторов от неконтроллируемых изменений в базовой части """
    basic_offers_table.load()
    assert_that(len(basic_offers_table.data), equal_to(2))
    assert_that(basic_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': 'offer.with.extra.in.update',
                'extra': {
                    'classifier_magic_id2': 'basic_classifier_magic_id2_old',
                    'classifier_good_id': 'basic_classifier_good_id_old',
                }
            },
        }, DTC.Offer())
    ]))


def test_basic_extra_ids_update(inserter, config, basic_offers_table):
    """ Экстра идентификаторы могут обновляться майнером """
    basic_offers_table.load()
    assert_that(len(basic_offers_table.data), equal_to(2))
    assert_that(basic_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': 'offer.with.extra.in.update.from.miner',
                'extra': {
                    'classifier_magic_id2': 'basic_classifier_magic_id2',
                    'classifier_good_id': 'basic_classifier_good_id',
                }
            },
        }, DTC.Offer())
    ]))


def test_service_extra_ids_protection(inserter, config, actual_service_offers_table):
    """ Защита ware_md5 от неконтроллируемых изменений в сервисной части
        classifier_magic_id2 и classifier_good_id удаляются из сервисной части при любом апдейте
    """
    actual_service_offers_table.load()
    assert_that(len(actual_service_offers_table.data), equal_to(2))
    assert_that(actual_service_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': 'offer.with.extra.in.update',
                'extra': {
                    'ware_md5': 'service_ware_md5_old',
                }
            },
        }, DTC.Offer())
    ]))
    assert_that(actual_service_offers_table.data, is_not(HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': 'offer.with.extra.in.update',
                'extra': {
                    'classifier_magic_id2': 'service_classifier_magic_id2_old',
                    'classifier_good_id': 'service_classifier_good_id_old',
                }
            },
        }, DTC.Offer())
    ])))
    assert_that(actual_service_offers_table.data, is_not(HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': 'offer.with.extra.in.update',
                'extra': {
                    'classifier_magic_id2': 'service_classifier_magic_id2',
                    'classifier_good_id': 'service_classifier_good_id',
                }
            },
        }, DTC.Offer())
    ])))


def test_service_extra_ids_update(inserter, config, actual_service_offers_table):
    """ ware_md5 может обновляться майнером
        classifier_magic_id2 и classifier_good_id удаляются из сервисной части при любом апдейте
    """
    actual_service_offers_table.load()
    assert_that(len(actual_service_offers_table.data), equal_to(2))
    assert_that(actual_service_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': 'offer.with.extra.in.update.from.miner',
                'extra': {
                    'ware_md5': 'service_ware_md5',
                }
            },
        }, DTC.Offer())
    ]))
    assert_that(actual_service_offers_table.data, is_not(HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': 'offer.with.extra.in.update.from.miner',
                'extra': {
                    'classifier_magic_id2': 'service_classifier_magic_id2_old',
                    'classifier_good_id': 'service_classifier_good_id_old',
                }
            },
        }, DTC.Offer())
    ])))
    assert_that(actual_service_offers_table.data, is_not(HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': 'offer.with.extra.in.update.from.miner',
                'extra': {
                    'classifier_magic_id2': 'service_classifier_magic_id2',
                    'classifier_good_id': 'service_classifier_good_id',
                }
            },
        }, DTC.Offer())
    ])))
