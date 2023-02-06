# coding: utf-8

from datetime import datetime
from hamcrest import assert_that
import pytest

import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.proto.offer.TechCommands_pb2 import COMPLETE_FEED_FINISHED

from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock

from market.idx.yatf.resources.lbk_topic import LbkTopic

from market.idx.yatf.matchers.yt_rows_matchers import HasOffers

from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampServiceOffersTable

from market.idx.pylibrary.datacamp.utils import wait_until
from market.pylibrary.proto_utils import message_from_data
from market.idx.datacamp.yatf.utils import create_meta, create_status, create_stock_info
from market.idx.pylibrary.datacamp.conversion import offer_to_service_row

TIME_UTC = datetime.utcfromtimestamp(20)  # JSON serializer should always use UTC
time_pattern = "%Y-%m-%dT%H:%M:%SZ"
current_time = TIME_UTC.strftime(time_pattern)

# синий партнер с business_id, оффера лежат и в синей таблице, и в united-таблицах
SHOP_ID2 = 2
BUSINESS_ID2 = 2
WAREHOUSE_ID2 = 2


def create_status_dict(flag, source, seconds):
    return {
        'disabled': [{
            'flag': flag,
            'meta': {
                'source': source,
                'timestamp': datetime.utcfromtimestamp(seconds).strftime(time_pattern)
            }
        }]
    }


SERVICE_OFFERS = [
    {
        'input': offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID2,
                shop_id=SHOP_ID2,
                offer_id='222'
            ),
            meta=create_meta(10),
            status=create_status(False, 10, DTC.PUSH_PARTNER_FEED),
            stock_info=create_stock_info(100, 10, DTC.PUSH_PARTNER_FEED, default_stock=True),
        )),
        'output': message_from_data({
            'identifiers': {
                'business_id': BUSINESS_ID2,
                'shop_id': SHOP_ID2,
                'offer_id': "222",
            },
            'status': create_status_dict(True, DTC.PUSH_PARTNER_FEED, 20),
            'stock_info': {
                'partner_stocks_default': {
                    'count': 100,
                    'meta': {
                        'timestamp': datetime.utcfromtimestamp(10).strftime(time_pattern),
                        'source': DTC.PUSH_PARTNER_FEED
                    }
                }
            }
        }, DTC.Offer())
    }
]

ACTUAL_SERVICE_OFFERS = [{
    'input': {
        'business_id': BUSINESS_ID2,
        'shop_id': SHOP_ID2,
        'warehouse_id': WAREHOUSE_ID2,
        'shop_sku': "222",
        'offer': DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID2,
                shop_id=SHOP_ID2,
                warehouse_id=WAREHOUSE_ID2,
                offer_id="222"
            ),
            meta=create_meta(10),
            stock_info=create_stock_info(100, 10, DTC.PUSH_PARTNER_FEED)
        ).SerializeToString()
    },
    'output': message_from_data({
        'identifiers': {
            'business_id': BUSINESS_ID2,
            'shop_id': SHOP_ID2,
            'offer_id': "222",
            'warehouse_id': WAREHOUSE_ID2
        },
        'stock_info': {
            'partner_stocks': {
                'count': 0,
                'meta': {
                    'timestamp': current_time,
                    'source': DTC.PUSH_PARTNER_FEED
                }
            }
        }
    }, DTC.Offer())
}]

SERVICE_DEFAULT_OFFER = {
    'status': {
        'disabled': [{
            'flag': True,
            'meta': {
                'timestamp': current_time,
                'source': DTC.PUSH_PARTNER_FEED,
            },
        }],
    }
}

STOCK_DEFAULT_OFFER = {
    'stock_info': {
        'partner_stocks': {
            'count': 0,
            'meta': {
                'timestamp': current_time,
                'source': DTC.PUSH_PARTNER_FEED
            }
        }
    }
}

TECH_COMMANDS = [
    {
        'timestamp': current_time,
        'command_type': COMPLETE_FEED_FINISHED,
        'command_params': {
            'business_id': BUSINESS_ID2,
            'shop_id': SHOP_ID2,
            'supplemental_id': WAREHOUSE_ID2,
            'complete_feed_command_params': {
                'default_offer_values': SERVICE_DEFAULT_OFFER
            }
        }
    },
    {
        'timestamp': current_time,
        'command_type': COMPLETE_FEED_FINISHED,
        'command_params': {
            'business_id': BUSINESS_ID2,
            'shop_id': SHOP_ID2,
            'supplemental_id': WAREHOUSE_ID2,
            'complete_feed_command_params': {
                'default_offer_values': STOCK_DEFAULT_OFFER
            }
        }
    },
]


@pytest.fixture(scope='module')
def blue_datacamp_messages_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def white_config(
        yt_server,
        log_broker_stuff,
        blue_datacamp_messages_topic):
    cfg = {
        'general': {
            'color': 'white',
        },
        'logbroker': {
            'blue_datacamp_messages_topic': blue_datacamp_messages_topic.topic,
        },
    }
    return PiperConfigMock(yt_server=yt_server,
                           log_broker_stuff=log_broker_stuff,
                           config=cfg)


@pytest.fixture(scope='module')
def service_offers_table(yt_server, white_config):
    return DataCampServiceOffersTable(
        yt_server,
        white_config.yt_service_offers_tablepath,
        data=[offer['input'] for offer in SERVICE_OFFERS])


@pytest.fixture(scope='module')
def actual_service_offers_table(yt_server, white_config):
    return DataCampServiceOffersTable(
        yt_server,
        white_config.yt_actual_service_offers_tablepath,
        data=[offer['input'] for offer in ACTUAL_SERVICE_OFFERS])


@pytest.yield_fixture(scope='module')
def white_piper(
        yt_server,
        log_broker_stuff,
        white_config,
        blue_datacamp_messages_topic,
        service_offers_table,
        actual_service_offers_table):
    resources = {
        'config': white_config,
        'blue_datacamp_messages_topic': blue_datacamp_messages_topic,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
    }
    with PiperTestEnv(yt_server, log_broker_stuff, **resources) as piper_env:
        piper_env.verify()
        yield piper_env


def test_white_piper(blue_datacamp_messages_topic, white_piper):
    for cmd in TECH_COMMANDS:
        message = message_from_data({'tech_command': [cmd]}, DatacampMessage())
        blue_datacamp_messages_topic.write(message.SerializeToString())

    wait_until(lambda: white_piper.united_offers_processed >= 2)
    expected_service_offers = [offer['output'] for offer in SERVICE_OFFERS]
    assert_that(white_piper.service_offers_table.data, HasOffers(expected_service_offers))

    expected_actual_service_offers = [offer['output'] for offer in ACTUAL_SERVICE_OFFERS]
    assert_that(white_piper.actual_service_offers_table.data, HasOffers(expected_actual_service_offers))
