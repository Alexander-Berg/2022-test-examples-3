# coding: utf-8

import pytest
from datetime import datetime
from hamcrest import assert_that, equal_to

import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock
from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.yatf.matchers.yt_rows_matchers import HasOffers
from market.idx.yatf.resources.datacamp.datacamp_tables import (
    DataCampBasicOffersTable,
    DataCampServiceOffersTable,
    DataCampPartnersTable,
)
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.utils.utils import create_timestamp_from_json, create_pb_timestamp
from market.idx.datacamp.yatf.utils import create_meta, dict2tskv
from market.idx.pylibrary.datacamp.conversion import offer_to_basic_row, offer_to_service_row
from market.pylibrary.proto_utils import message_from_data

BUSINESS_ID = 1
SHOP_ID = 11
WAREHOUSE_ID = 111
OFFER_ID = "TestOffer"

NOW_UTC = datetime.utcnow()  # JSON serializer should always use UTC
time_pattern = "%Y-%m-%dT%H:%M:%SZ"
current_time = NOW_UTC.strftime(time_pattern)
current_ts = create_timestamp_from_json(current_time)

DATACAMP_MESSAGES = [
    {
        'united_offers': [
            {
                # апдейт цены не из разрешенного источника, не должны его принимать
                'offer': [
                    {
                        'basic': {
                            'identifiers': {
                                'business_id': BUSINESS_ID,
                                'offer_id': OFFER_ID,
                            },
                            'content': {
                                'partner': {
                                    'actual': {
                                        'title': {
                                            'value': 'title',
                                            'meta': {
                                                'timestamp': NOW_UTC.strftime(time_pattern)
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        'service': {
                            SHOP_ID: {
                                'identifiers': {
                                    'business_id': BUSINESS_ID,
                                    'offer_id': OFFER_ID,
                                    'shop_id': SHOP_ID,
                                    'warehouse_id': WAREHOUSE_ID,
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
                            },
                        }
                    }
                ]
            },
        ]
    }
]


@pytest.fixture(scope='module')
def datacamp_messages_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def partners_table(yt_server, config):
    return DataCampPartnersTable(
        yt_server,
        config.yt_partners_tablepath,
        data=[
            {
                'shop_id': SHOP_ID,
                'mbi': '\n\n'.join([
                    dict2tskv({
                        'shop_id': SHOP_ID,
                        'business_id': BUSINESS_ID,
                        'cpc': 'NO',
                        'united_catalog_status': 'SUCCESS',
                        'is_site_market': 'true',
                    }),
                ]),
            },
        ],
    )


@pytest.fixture(scope='module')
def config(yt_server, log_broker_stuff, datacamp_messages_topic):
    cfg = {
        'logbroker': {
            'datacamp_messages_topic': datacamp_messages_topic.topic,
        },
        'general': {
            'color': 'white',
        },
        'features': {
            'enable_offer_format_validator': True,
            'restricted_service_price_updates': True,
            'clear_service_price_from_restricted_source': True,
            'allowed_service_price_sources': 'SomeSourceForPrice;SomeOtherSourceForPrice',
        }
    }
    return PiperConfigMock(yt_server=yt_server, log_broker_stuff=log_broker_stuff, config=cfg)


@pytest.fixture(scope='module')
def basic_offers_table(yt_server, config):
    return DataCampBasicOffersTable(yt_server, config.yt_basic_offers_tablepath, data=[
        offer_to_basic_row(
            DTC.Offer(
                identifiers=DTC.OfferIdentifiers(
                    business_id=BUSINESS_ID,
                    offer_id=OFFER_ID
                ),
                meta=create_meta(10)
            )
        ),
    ])


@pytest.fixture(scope='module')
def service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(yt_server, config.yt_service_offers_tablepath, data=[
        offer_to_service_row(
            DTC.Offer(
                identifiers=DTC.OfferIdentifiers(
                    business_id=BUSINESS_ID,
                    offer_id=OFFER_ID,
                    shop_id=SHOP_ID
                ),
                meta=create_meta(10, DTC.WHITE, scope=DTC.SERVICE),
                price=DTC.OfferPrice(
                    basic=DTC.PriceBundle(
                        binary_price=DTC.PriceExpression(
                            id='RUR',
                            price=1000,
                        ),
                        meta=DTC.UpdateMeta(
                            timestamp=create_pb_timestamp(50),
                        )
                    ),
                )
            )
        ),
    ])


@pytest.fixture(scope='module')
def actual_service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(yt_server, config.yt_actual_service_offers_tablepath, data=[
        offer_to_service_row(
            DTC.Offer(
                identifiers=DTC.OfferIdentifiers(
                    business_id=BUSINESS_ID,
                    offer_id=OFFER_ID,
                    shop_id=SHOP_ID,
                    warehouse_id=WAREHOUSE_ID
                ),
                meta=create_meta(10, color=DTC.WHITE, scope=DTC.SERVICE)
            )
        ),
    ])


@pytest.yield_fixture(scope='module')
def piper(yt_server, log_broker_stuff, config, datacamp_messages_topic, partners_table,
          basic_offers_table, service_offers_table, actual_service_offers_table):
    resources = {
        'config': config,
        'datacamp_messages_topic': datacamp_messages_topic,
        'partners_table': partners_table,
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
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


def test_do_not_update_price_from_restricted_source(
        inserter,
        config,
        basic_offers_table,
        service_offers_table,
        actual_service_offers_table
):
    # Проверяем, что не обновляем сервисную цену из источников,
    # которые не разрешены белым списком AllowedServicePriceSources
    basic_offers_table.load()
    assert_that(len(basic_offers_table.data), equal_to(1))
    assert_that(basic_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': BUSINESS_ID,
                'offer_id': OFFER_ID,
            },
            'content': {
                'partner': {
                    'actual': {
                        'title': {
                            'value': 'title',
                        }
                    }
                }
            }
        }, DTC.Offer()),
    ]))

    service_offers_table.load()
    assert_that(len(service_offers_table.data), equal_to(1))
    assert_that(service_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': BUSINESS_ID,
                'offer_id': OFFER_ID,
                'shop_id': SHOP_ID,
            },
            'price': {
                'basic': {
                    'binary_price': {
                        'price': 1000  # не обновилась
                    },
                }
            },
            'meta': {
                'rgb': DTC.WHITE,
            },
        }, DTC.Offer())]))

    actual_service_offers_table.load()
    assert_that(len(actual_service_offers_table.data), equal_to(1))
    assert_that(actual_service_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': BUSINESS_ID,
                'offer_id': OFFER_ID,
                'warehouse_id': WAREHOUSE_ID,
                'shop_id': SHOP_ID,
            },
            'meta': {
                'rgb': DTC.WHITE,
            },
        }, DTC.Offer())]))
