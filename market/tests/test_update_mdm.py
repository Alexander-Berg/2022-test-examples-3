# coding: utf-8

import pytest
from datetime import datetime
from hamcrest import assert_that

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
from market.idx.pylibrary.datacamp.conversion import offer_to_basic_row, offer_to_service_row
from market.pylibrary.proto_utils import message_from_data


NOW_UTC = datetime.utcnow()


def create_meta(ts=False):
    result = {
        'source': DTC.MARKET_MDM,
    }
    if ts:
        result['timestamp'] = NOW_UTC.strftime("%Y-%m-%dT%H:%M:%SZ")
    return result


def create_resolution(feature, ts=True):
    return {
        'by_source': [{
            'meta': create_meta(ts),
            'verdict': [{
                'results': [{
                    'applications': [
                        feature
                    ],
                    'is_banned': True
                }]
            }]
        }]
    }


BASIC_FIELDS = [
    'box_count',
    'certificates',
    'customs_comm_code',
    'dimensions',
    'gtins',
    'guarantee_period',
    'guarantee_period_comment',
    'life_time',
    'life_time_comment',
    'manufacturer',
    'manufacturer_countries',
    'shelf_life',
    'shelf_life_comment',
    'shelf_life_required',
    'use_in_mercury',
    'version',
    'vetis_guids',
    'weight_gross',
    'weight_net',
    'weight_tare'
]
SERVICE_FIELDS = [
    'min_shipment',
    'partner_delivery_time',
    'quantum_of_supply',
    'supply_schedule',
    'transport_unit_size'
]


DATACAMP_MESSAGES = [
    {
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': 1,
                        'offer_id': offer_id,
                    },
                    'meta': {
                        'scope': DTC.BASIC
                    },
                    'content': {
                        'master_data': {
                            key: {
                                'meta': create_meta(),
                            } for key in BASIC_FIELDS
                        }
                    },
                    'resolution': create_resolution(DTC.FULFILLMENT)
                },
                'service': {
                    1: {
                        'identifiers': {
                            'business_id': 1,
                            'shop_id': 1,
                            'offer_id': offer_id,
                            'warehouse_id': 145
                        },
                        'meta': {
                            'scope': service_scope
                        } if service_scope else None,
                        'content': {
                            'master_data': {
                                key: {
                                    'meta': create_meta(),
                                } for key in SERVICE_FIELDS
                            }
                        },
                        'resolution': create_resolution(DTC.CPA)
                    },
                }
            }]
        }]
    } for offer_id, service_scope in [
        ('offer.with.mdm.update.service', DTC.SERVICE),
        ('offer.with.mdm.update.default', None)
    ]
]


@pytest.fixture(scope='module')
def partners_table(yt_server, config):
    return DataCampPartnersTable(
        yt_server,
        config.yt_partners_tablepath,
        data=[
            {
                'shop_id': 1,
                'mbi': '\n\n'.join([
                    dict2tskv({
                        'shop_id': 1,
                        'business_id': 1,
                        'blue_status': 'REAL',
                        'united_catalog_status': 'SUCCESS',
                        'warehouse_id': 145
                    }),
                ]),
            },
        ],
    )


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
    }
    return PiperConfigMock(yt_server=yt_server, log_broker_stuff=log_broker_stuff, config=cfg)


@pytest.fixture(scope='module')
def basic_offers_table(yt_server, config):
    return DataCampBasicOffersTable(yt_server, config.yt_basic_offers_tablepath, data=[
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='offer.with.mdm.update.service',
            ),
        )),
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='offer.with.mdm.update.default',
            ),
        ))
    ])


@pytest.fixture(scope='module')
def service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(yt_server, config.yt_service_offers_tablepath, data=[
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='offer.with.mdm.update.service',
                shop_id=1,
                warehouse_id=0,
            ),
            meta=DTC.OfferMeta(
                scope=DTC.SERVICE,
                rgb=DTC.BLUE
            )
        )),
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='offer.with.mdm.update.default',
                shop_id=1,
                warehouse_id=0,
            ),
            meta=DTC.OfferMeta(
                scope=DTC.SERVICE,
                rgb=DTC.BLUE
            )
        )),
    ])


@pytest.yield_fixture(scope='module')
def piper(
    yt_server,
    log_broker_stuff,
    config,
    datacamp_messages_topic,
    partners_table,
    basic_offers_table,
    service_offers_table
):
    resources = {
        'config': config,
        'datacamp_messages_topic': datacamp_messages_topic,
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
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


def test_mdm_update_service_scope(piper, inserter, config, basic_offers_table):
    """ Проверяем, что обновления mdm записываются как надо со scope=SERVICE """
    assert_that(piper.basic_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': 'offer.with.mdm.update.service'
            },
            'content': {
                'master_data': {
                    key: {
                        'meta': create_meta(False),
                    } for key in BASIC_FIELDS
                }
            },
            'resolution': create_resolution(DTC.FULFILLMENT)
        }, DTC.Offer())
    ]))
    assert_that(piper.service_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': 'offer.with.mdm.update.service',
                'shop_id': 1
            },
            # Проставился из шопсдат
            'status': {
                'united_catalog': {
                    'flag': True,
                }
            },
            'content': {
                'master_data': {
                    key: {
                        'meta': create_meta(False),
                    } for key in SERVICE_FIELDS
                }
            },
            'resolution': create_resolution(DTC.CPA)
        }, DTC.Offer())
    ]))
    assert_that(piper.actual_service_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': 'offer.with.mdm.update.service',
                'shop_id': 1,
                'warehouse_id': 145
            },
            'content': None,
            'resolution': None
        }, DTC.Offer())
    ]))


def test_mdm_update_default_scope(piper, inserter, config, basic_offers_table):
    """ Проверяем, что обновления mdm записываются как надо со scope=SELECTIVE """
    assert_that(piper.basic_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': 'offer.with.mdm.update.default'
            },
            'content': {
                'master_data': {
                    key: {
                        'meta': create_meta(False),
                    } for key in BASIC_FIELDS
                }
            },
            'resolution': create_resolution(DTC.FULFILLMENT)
        }, DTC.Offer())
    ]))
    assert_that(piper.service_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': 'offer.with.mdm.update.default',
                'shop_id': 1
            },
            # Проставился из шопсдат
            'status': {
                'united_catalog': {
                    'flag': True,
                }
            },
            'content': {
                'master_data': {
                    key: {
                        'meta': create_meta(False),
                    } for key in SERVICE_FIELDS
                }
            },
            'resolution': create_resolution(DTC.CPA)
        }, DTC.Offer())
    ]))
    assert_that(piper.actual_service_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': 'offer.with.mdm.update.default',
                'shop_id': 1,
                'warehouse_id': 145
            },
            'content': None,
            'resolution': None
        }, DTC.Offer())
    ]))
