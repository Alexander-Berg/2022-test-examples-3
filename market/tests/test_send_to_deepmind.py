# coding: utf-8

from hamcrest import assert_that
import pytest
from datetime import datetime, timedelta

import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.datacamp.yatf.utils import create_meta, create_update_meta
from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.resources.datacamp.datacamp_tables import (
    DataCampBasicOffersTable,
    DataCampServiceOffersTable,
)
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf, IsProtobufMap
from market.pylibrary.proto_utils import message_from_data
from market.idx.pylibrary.datacamp.conversion import offer_to_basic_row


NOW_UTC = datetime.utcnow()  # JSON serializer should always use UTC
time_pattern = "%Y-%m-%dT%H:%M:%SZ"

FUTURE_UTC = NOW_UTC + timedelta(minutes=45)


def make_united_offer(offer_id):
    offer = {
        'basic': {
            'identifiers': {
                'business_id': 1,
                'offer_id': offer_id,
            },
            'meta': {
                'scope': DTC.BASIC,
                'ts_created': NOW_UTC.strftime(time_pattern)
            },
            'content': {
                'market': {
                    'real_uc_version': {
                        'counter': 1
                    }
                },
                'partner': {
                    'actual': {
                        'title': {
                            'value': 'offer_title',
                            'meta': {
                                'timestamp': NOW_UTC.strftime(time_pattern)
                            }
                        }
                    }
                },
                'binding': {
                    'approved': {
                        'market_category_id': 222,
                        'market_sku_id': 222,
                        'meta': {
                            'timestamp': NOW_UTC.strftime(time_pattern)
                        }
                    },
                },
            },
        },
        'service': {
            1: {
                'identifiers': {
                    'business_id': 1,
                    'offer_id': offer_id,
                    'shop_id': 1,
                    'warehouse_id': 1,
                },
                'meta': {
                    'rgb': DTC.BLUE,
                    'scope': DTC.SERVICE,
                    'ts_created': NOW_UTC.strftime(time_pattern)
                },
            }
        }
    }
    return offer


DATACAMP_MESSAGES = [
    {
        'united_offers': [{
            'offer': [
                make_united_offer('T1000'),
            ]
        }]
    }
]


@pytest.fixture(scope='module')
def basic_offers_table(yt_server, config):
    return DataCampBasicOffersTable(yt_server, config.yt_basic_offers_tablepath, data=[
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='T1000',
            ),
            meta=create_meta(10),
            content=DTC.OfferContent(
                binding=DTC.ContentBinding(
                    approved=DTC.Mapping(
                        market_category_id=1,
                        market_category_name='cat',
                        market_model_id=1,
                        market_model_name='mmn',
                        market_sku_id=111,
                        market_sku_name='msn',
                        meta=create_update_meta(10),
                    ),
                    meta=create_update_meta(10)
                )
            )))])


@pytest.fixture(scope='module')
def actual_service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(yt_server, config.yt_actual_service_offers_tablepath)


@pytest.fixture(scope='module')
def service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(yt_server, config.yt_service_offers_tablepath)


@pytest.fixture(scope='module')
def datacamp_messages_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def deepmind_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def config(yt_server, log_broker_stuff, datacamp_messages_topic, deepmind_topic):
    cfg = {
        'general': {
            'color': 'blue',
            'batch_size': 10,
        },
        'logbroker': {
            'datacamp_messages_topic': datacamp_messages_topic.topic,
            'deepmind_topic': deepmind_topic.topic
        }
    }
    return PiperConfigMock(yt_server=yt_server, log_broker_stuff=log_broker_stuff, config=cfg)


@pytest.yield_fixture(scope='module')
def piper(yt_server, log_broker_stuff, deepmind_topic, datacamp_messages_topic,
          basic_offers_table, service_offers_table, actual_service_offers_table, config):
    resources = {
        'config': config,
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
        'deepmind_topic': deepmind_topic,
        'datacamp_messages_topic': datacamp_messages_topic,
    }
    with PiperTestEnv(yt_server, log_broker_stuff, **resources) as piper_env:
        piper_env.verify()
        yield piper_env


def test_send_content_update_to_deepmind(piper, datacamp_messages_topic, deepmind_topic):
    for message in DATACAMP_MESSAGES:
        datacamp_messages_topic.write(message_from_data(message, DatacampMessage()).SerializeToString())
    wait_until(lambda: piper.united_offers_processed >= 1)

    data = deepmind_topic.read(count=1)
    assert_that(data[0], IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': 1,
                        'offer_id': 'T1000',
                    },
                    'content': {
                        'partner': {
                            'actual': {
                                'title': {
                                    'value': 'offer_title'
                                }
                            }
                        },
                        'binding': {
                            'approved': {
                                'market_category_id': 222,
                                'market_sku_id': 222,
                            },
                        },
                    },
                },
                'service': IsProtobufMap({
                    1: {
                        'identifiers': {
                            'business_id': 1,
                            'offer_id': 'T1000',
                            'shop_id': 1
                        }
                    }
                })
            }]
        }]
    }))
