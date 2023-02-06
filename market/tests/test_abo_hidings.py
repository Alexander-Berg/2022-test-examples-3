# coding: utf-8

import pytest
from datetime import datetime
from hamcrest import assert_that

from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock
from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.proto.models.ModelStatus_pb2 import ModelStatus
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.pylibrary.datacamp.utils import wait_until

from market.idx.yatf.matchers.yt_rows_matchers import HasOffers, HasDatacampMskuRows
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf, IsProtobufMap
from market.idx.yatf.resources.datacamp.datacamp_tables import (
    DataCampMskuTable
)
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.pylibrary.proto_utils import message_from_data

NOW_UTC = datetime.utcnow()  # JSON serializer should always use UTC
time_pattern = "%Y-%m-%dT%H:%M:%SZ"
current_time = NOW_UTC.strftime(time_pattern)

BUSINESS_ID = 1
SHOP_ID = 2

OFFER_HIDINGS = [
    {
        'basic': {
            'identifiers': {
                'business_id': BUSINESS_ID,
                'offer_id': 'o1',
            },
        },
        'service': {
            SHOP_ID: {
                'identifiers': {
                    'business_id': BUSINESS_ID,
                    'shop_id': SHOP_ID,
                    'offer_id': 'o1',
                },
                'meta': {
                    'rgb': DTC.BLUE,
                    'scope': DTC.SERVICE
                },
                'status': {
                    'disabled': [{
                        'meta': {
                            'timestamp': current_time,
                            'source': DTC.MARKET_ABO_SHOP_SKU,
                        },
                        'flag': True,
                    }]
                },
                'resolution': {
                    'by_source': [{
                        'meta': {
                            'timestamp': current_time,
                            'source': DTC.MARKET_ABO_SHOP_SKU,
                        },
                        'verdict': [{
                            'results': [{
                                'is_banned': True,
                                'abo_reason': DTC.MANUALLY_HIDDEN
                            }]
                        }]
                    }]
                }
            }
        }
    }
]

MSKU_HIDINGS = [
    {
        'id': 111,
        'status': {
            'abo_status': {
                'meta': {
                    'timestamp': current_time,
                    'source': DTC.MARKET_ABO_MSKU,
                },
                'reason': DTC.WRONG_GOOD_INFO
            }
        }
    },
    {
        'id': 222,
        'status': {
            'abo_shop_status': {
                SHOP_ID: {
                    'meta': {
                        'timestamp': current_time,
                        'source': DTC.MARKET_ABO_MSKU_SHOP,
                    },
                    'reason': DTC.NO_GUARANTEE
                }
            }
        }
    }
]


@pytest.fixture(scope='module')
def datacamp_msku_table(yt_server, config):
    return DataCampMskuTable(yt_server, config.yt_datacamp_msku_tablepath)


@pytest.fixture(scope='module')
def abo_hidings_input_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def config(yt_server, log_broker_stuff, abo_hidings_input_topic):
    cfg = {
        'logbroker': {
            'abo_hidings_input_topic': abo_hidings_input_topic.topic
        },
        'general': {
            'color': 'white',
        }
    }

    return PiperConfigMock(yt_server=yt_server,
                           log_broker_stuff=log_broker_stuff,
                           config=cfg)


@pytest.yield_fixture(scope='module')
def piper(yt_server, log_broker_stuff, config, abo_hidings_input_topic, datacamp_msku_table):
    resources = {
        'config': config,
        'abo_hidings_input_topic': abo_hidings_input_topic,
        'yt_datacamp_msku_tablepath': datacamp_msku_table
    }
    with PiperTestEnv(yt_server, log_broker_stuff, **resources) as piper_env:
        piper_env.verify()
        yield piper_env


def test_update_abo_hidings_from_topic(piper, abo_hidings_input_topic, datacamp_msku_table):
    for offer_hiding in OFFER_HIDINGS:
        message = message_from_data({'united_offers': [{
            'offer': [offer_hiding]
        }]}, DatacampMessage())
        abo_hidings_input_topic.write(message.SerializeToString())

    for msku_hiding in MSKU_HIDINGS:
        message = message_from_data({'market_skus': {
            'msku': [msku_hiding]
        }}, DatacampMessage())
        abo_hidings_input_topic.write(message.SerializeToString())

    wait_until(lambda: piper.united_offers_processed >= len(OFFER_HIDINGS), timeout=60)
    wait_until(lambda: piper.msku_processed >= len(MSKU_HIDINGS), timeout=60)

    piper.service_offers_table.load()
    assert_that(piper.service_offers_table.data, HasOffers([message_from_data({
        'identifiers': {
            'business_id': BUSINESS_ID,
            'shop_id': SHOP_ID,
            'offer_id': 'o1',
        },
        'status': {
            'disabled': [{
                'flag': True,
                'meta': {
                    'source': DTC.MARKET_ABO_SHOP_SKU,
                }
            }]
        },
        'resolution': {
            'by_source': [{
                'verdict': [{
                    'results': [{
                        'is_banned': True,
                        'abo_reason': DTC.MANUALLY_HIDDEN,
                    }]
                }],
                'meta': {
                    'source': DTC.MARKET_ABO_SHOP_SKU
                }
            }]
        }
    }, DTC.Offer())]))

    datacamp_msku_table.load()
    assert_that(datacamp_msku_table.data, HasDatacampMskuRows([
        {
            'id': 111,
            'status': IsSerializedProtobuf(ModelStatus, {
                'abo_status': {
                    'reason': DTC.WRONG_GOOD_INFO,
                    'meta': {
                        'source': DTC.MARKET_ABO_MSKU
                    },
                }
            }),
        },
        {
            'id': 222,
            'status': IsSerializedProtobuf(ModelStatus, {
                'abo_shop_status': IsProtobufMap({
                    SHOP_ID: {
                        'reason': DTC.NO_GUARANTEE,
                        'meta': {
                            'source': DTC.MARKET_ABO_MSKU_SHOP
                        },
                    }
                })
            }),
        }
    ]))
