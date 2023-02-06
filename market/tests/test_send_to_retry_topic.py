# coding: utf-8

import pytest
from hamcrest import assert_that, has_items, has_length

from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import (
    OfferIdentifiers,
    PUSH_PARTNER_API,
    MARKET_ABO,
    MARKET_PRICELABS,
    BLUE,
    WHITE,
)
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import Offer as DatacampOffer
from market.idx.datacamp.proto.api.SyncChangeOffer_pb2 import ChangeOfferRequest
from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage

from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf, IsProtobufMap
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampBasicOffersTable, DataCampServiceOffersTable

from market.idx.datacamp.yatf.utils import create_meta, create_status

time_pattern = "%Y-%m-%dT%H:%M:%SZ"


def make_api_data(shop_id, offer_id, warehouse_id, status, ts, source=PUSH_PARTNER_API, color=BLUE):
    return DatacampOffer(
        identifiers=OfferIdentifiers(
            shop_id=shop_id,
            offer_id=offer_id,
            warehouse_id=warehouse_id,
            business_id=1,
        ),
        meta=create_meta(color=color),
        status=create_status(status, ts, source),
    )


API_DATA = [
    make_api_data(1, 'T1000', 10, True, 100),
    # данные не своего цвета не должны фильтроваться
    make_api_data(1, 'T800', 0, True, 100, color=WHITE),
    # данные с MARKET_ABO и (пока) только синего цвета не должны игнорироваться
    make_api_data(1, 'T900', 10, True, 100, MARKET_ABO),
    # данные с MARKET_PRICELABS
    make_api_data(1, 'T950', 10, True, 100, MARKET_PRICELABS),
]


@pytest.fixture(scope='module')
def basic_offers_table(yt_server, config):
    return DataCampBasicOffersTable(yt_server, config.yt_basic_offers_tablepath)


@pytest.fixture(scope='module')
def service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(yt_server, config.yt_service_offers_tablepath)


@pytest.fixture(scope='module')
def actual_service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(yt_server, config.yt_actual_service_offers_tablepath)


@pytest.fixture(scope='session')
def api_data_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def retry_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def config(yt_server, log_broker_stuff, api_data_topic, retry_topic):
    cfg = {
        'general': {
            'color': 'blue',
        },
        'logbroker': {
            'api_data_topic': api_data_topic.topic,
            'retry_topic': retry_topic.topic,
        },
    }
    return PiperConfigMock(yt_server=yt_server, log_broker_stuff=log_broker_stuff, config=cfg)


@pytest.yield_fixture(scope='module')
def piper(
    yt_server,
    log_broker_stuff,
    config,
    api_data_topic,
    basic_offers_table,
    service_offers_table,
    actual_service_offers_table,
    retry_topic,
):
    resources = {
        'config': config,
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
        'api_data_topic': api_data_topic,
        'retry_topic': retry_topic,
    }
    with PiperTestEnv(yt_server, log_broker_stuff, **resources) as piper_env:
        piper_env.verify()
        yield piper_env


def make_united_offer_matcher(business_id, offer_id, shop_id, warehouse_id, feed_id=None):
    identifiers = {
        'business_id': business_id,
        'shop_id': shop_id,
        'offer_id': offer_id,
        'warehouse_id': warehouse_id,
    }
    if feed_id:
        identifiers['feed_id'] = feed_id

    return {
        'united_offers': [
            {
                'offer': [
                    {
                        'basic': {
                            'identifiers': {
                                'business_id': business_id,
                                'offer_id': offer_id,
                            },
                        },
                        'service': IsProtobufMap(
                            {
                                shop_id: {
                                    'identifiers': identifiers,
                                }
                            }
                        ),
                    }
                ]
            }
        ],
        'tech_info': {
            'current_retry_count': 1,
            'fail_reason': has_length(1),
        }
    }


def test_send_to_retry_topic(piper, api_data_topic, retry_topic):
    """
    с указанным retry_topic datacamp_client бросает Retry limit exceeded, ожидаем увидеть все офера в retry топике
    """
    for api in API_DATA:
        request = ChangeOfferRequest()
        request.offer.extend([api])
        api_data_topic.write(request.SerializeToString())

    result = retry_topic.read(count=4, wait_timeout=60)
    assert_that(
        result,
        has_items(
            *[
                IsSerializedProtobuf(DatacampMessage, make_united_offer_matcher(business_id=1, offer_id='T1000', shop_id=1, warehouse_id=10)),
                IsSerializedProtobuf(DatacampMessage, make_united_offer_matcher(business_id=1, offer_id='T800', shop_id=1, warehouse_id=0)),
                IsSerializedProtobuf(DatacampMessage, make_united_offer_matcher(business_id=1, offer_id='T900', shop_id=1, warehouse_id=10)),
                IsSerializedProtobuf(DatacampMessage, make_united_offer_matcher(business_id=1, offer_id='T950', shop_id=1, warehouse_id=10)),
            ]
        ),
    )
