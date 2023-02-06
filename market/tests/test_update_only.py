# coding: utf-8

import pytest
from datetime import datetime, timedelta
from hamcrest import assert_that, equal_to

import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock
from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.proto.business.Business_pb2 import BusinessStatus
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.yatf.matchers.yt_rows_matchers import HasOffers
from market.idx.yatf.resources.datacamp.datacamp_tables import (
    DataCampBasicOffersTable,
    DataCampPartnersTable,
    DataCampBusinessStatusTable,
)
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.utils.utils import create_timestamp_from_json
from market.idx.datacamp.yatf.utils import create_update_meta, dict2tskv
from market.idx.pylibrary.datacamp.conversion import offer_to_basic_row
from market.proto.common.common_pb2 import PriceExpression
from market.pylibrary.proto_utils import message_from_data

BLOCKED_BUSINESS = 1000
NOW_UTC = datetime.utcnow()  # JSON serializer should always use UTC
time_pattern = "%Y-%m-%dT%H:%M:%SZ"
current_time = NOW_UTC.strftime(time_pattern)
current_ts = create_timestamp_from_json(current_time)

FUTURE_UTC = NOW_UTC + timedelta(minutes=45)

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
                    'price': {
                        'basic': {
                            'binary_price': {
                                'price': 10
                            },
                            'meta': {
                                'timestamp': NOW_UTC.strftime(time_pattern)
                            }
                        }
                    },
                },
            }, {
                'basic': {
                    'identifiers': {
                        'business_id': 1,
                        'offer_id': 'o2',
                    },
                    'meta': {
                        'scope': DTC.BASIC,
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
                },
            }]
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
                    }),
                ]),
            },
        ],
    )


@pytest.fixture(scope='module')
def business_status_table(yt_server, config):
    return DataCampBusinessStatusTable(yt_server, config.yt_business_status_tablepath, data=[
        {
            'business_id': BLOCKED_BUSINESS,
            'status': BusinessStatus(
                value=BusinessStatus.Status.LOCKED,
            ).SerializeToString(),
        },
    ])


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
def basic_offers_table(yt_server, config):
    return DataCampBasicOffersTable(yt_server, config.yt_basic_offers_tablepath, data=[
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='o1',
            ),
            price=DTC.OfferPrice(
                basic=DTC.PriceBundle(
                    binary_price=PriceExpression(
                        price=100,
                    ),
                    meta=create_update_meta(10),
                ),
            ),
        ))])


@pytest.yield_fixture(scope='module')
def piper(yt_server, log_broker_stuff, config, datacamp_messages_topic, partners_table, basic_offers_table,
          business_status_table):
    resources = {
        'config': config,
        'datacamp_messages_topic': datacamp_messages_topic,
        'partners_table': partners_table,
        'business_status_table': business_status_table,
        'basic_offers_table': basic_offers_table,
    }
    with PiperTestEnv(yt_server, log_broker_stuff, update_only=True, **resources) as piper_env:
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


def test_update_only(inserter, config, basic_offers_table):
    """ В стейте оффер o1.
        В пайпер в режиме update_only приходят два оффера: o1 и o2.
        Ожидаем, что оффер o1 обновится, оффер o2 не создастся
    """
    basic_offers_table.load()
    assert_that(len(basic_offers_table.data), equal_to(1))
    assert_that(basic_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': 'o1',
            },
            'price': {
                'basic': {
                    'binary_price': {
                        'price': 10
                    },
                }
            },
        }, DTC.Offer())
    ]))
