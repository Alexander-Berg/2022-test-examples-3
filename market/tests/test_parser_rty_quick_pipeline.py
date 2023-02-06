# coding: utf-8

from datetime import datetime
from hamcrest import assert_that, equal_to
import pytest

from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock
from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.pylibrary.datacamp.conversion import offer_to_basic_row, offer_to_service_row
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.datacamp.yatf.utils import create_meta, dict2tskv, create_price
from market.idx.yatf.matchers.lb_matchers import HasNoUnreadData
from market.idx.yatf.resources.datacamp.datacamp_tables import (
    DataCampBasicOffersTable,
    DataCampServiceOffersTable,
    DataCampPartnersTable,
)
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.pylibrary.proto_utils import message_from_data
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC

NOW_UTC = datetime.utcnow()  # JSON serializer should always use UTC
time_pattern = "%Y-%m-%dT%H:%M:%SZ"
current_time = NOW_UTC.strftime(time_pattern)

OFFER_ID='offer_id'
BUSINESS_ID=1
FEED_ID=11

UNITED_OFFERS = [{
    'united_offers': [{
        'offer': [{
            'basic': {
                'identifiers': {
                    'business_id': BUSINESS_ID,
                    'offer_id': OFFER_ID,
                    'feed_id': FEED_ID
                },
                'content': {
                    'partner': {
                        'original': {
                            'weight': {
                                'value_mg': 1000000
                            },
                            'dimensions': {
                                'width_mkm': 20000,
                                'length_mkm': 10000,
                                'height_mkm': 30000
                            }
                        }
                    }
                }
            },
            'service': {
                101: {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': OFFER_ID,
                        'shop_id': 101,
                        'feed_id': FEED_ID
                    },
                    'price': {
                        'basic': {
                            'binary_price': {
                                'price': 1100000000,
                                'rate': 'CBRF',
                                'id': 'USD'
                            },
                            'meta': {
                                'timestamp': current_time
                            }
                        }
                    }
                }
            }
        }]
    }]
}]


@pytest.fixture(scope='module')
def partners_table(yt_server, config):
    return DataCampPartnersTable(
        yt_server,
        config.yt_partners_tablepath,
        data=[
            {
                'shop_id': 101,
                'mbi': '\n\n'.join([
                    dict2tskv({
                        'shop_id': 101,
                        'business_id': BUSINESS_ID,
                        'united_catalog_status': 'NO',
                        'is_site_market': 'true'
                    }),
                ]),
            },
        ],
    )


@pytest.fixture(scope='module')
def basic_offers_table(yt_server, config):
    return DataCampBasicOffersTable(yt_server, config.yt_basic_offers_tablepath, data=[
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=BUSINESS_ID, offer_id=OFFER_ID),
            meta=create_meta(10, color=DTC.UNKNOWN_COLOR, scope=DTC.BASIC),
        ))])


@pytest.fixture(scope='module')
def service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(yt_server, config.yt_service_offers_tablepath, data=[
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=BUSINESS_ID, offer_id=OFFER_ID, shop_id=101, feed_id=FEED_ID),
            meta=create_meta(10, color=DTC.UNKNOWN_COLOR, scope=DTC.SERVICE),
            price=create_price(100, 200)
        ))])


@pytest.fixture(scope='module')
def actual_service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(yt_server, config.yt_actual_service_offers_tablepath, data=[
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=BUSINESS_ID, offer_id=OFFER_ID, shop_id=101, feed_id=FEED_ID),
            meta=create_meta(10, color=DTC.UNKNOWN_COLOR, scope=DTC.SERVICE),
        ))])


@pytest.fixture(scope='module')
def qoffers_quick_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def rty_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff, 'rty_topic')
    return topic


@pytest.fixture(scope='module')
def config(yt_server, log_broker_stuff, qoffers_quick_topic, rty_topic):
    cfg = {
        'logbroker': {
            'quick_pipeline_topic': qoffers_quick_topic.topic,  # qoffers quick input topic
            'rty_topic': rty_topic.topic,             # output topic
        },
        'general': {
            'color': 'blue',
        }
    }
    return PiperConfigMock(
        yt_server=yt_server,
        log_broker_stuff=log_broker_stuff,
        config=cfg
    )


@pytest.yield_fixture(scope='module')
def piper(
    yt_server,
    log_broker_stuff,
    config,
    basic_offers_table,
    service_offers_table,
    actual_service_offers_table,
    partners_table,
    qoffers_quick_topic,
    rty_topic
):
    resources = {
        'config': config,
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
        'partners_table': partners_table,
        'quick_pipeline_topic': qoffers_quick_topic,
        'rty_topic': rty_topic,
    }
    with PiperTestEnv(yt_server, log_broker_stuff, **resources) as piper_env:
        piper_env.verify()
        yield piper_env


def test_quick_pipeline_update(rty_topic, qoffers_quick_topic, piper):
    united_offers = 0
    for message in UNITED_OFFERS:
        for offers in message['united_offers']:
            united_offers += len(offers)
        qoffers_quick_topic.write(message_from_data(message, DatacampMessage()).SerializeToString())
    wait_until(lambda: piper.united_offers_processed >= united_offers)

    result = rty_topic.read(count=1)
    assert_that(rty_topic, HasNoUnreadData())
    assert_that(len(result), equal_to(1))
