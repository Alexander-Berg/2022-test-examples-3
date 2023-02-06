# coding: utf-8

import pytest
from datetime import datetime
from protobuf_to_dict import protobuf_to_dict
from hamcrest import assert_that, greater_than, has_entries

from market.idx.datacamp.proto.api.ExportMessage_pb2 import ExportMessage
from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock
from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.pylibrary.datacamp.utils import wait_until
from market.pylibrary.proto_utils import message_from_data
from market.idx.yatf.resources.lbk_topic import LbkTopic


NOW_UTC = datetime.utcnow()
BUISNESS_ID = 1
SHOP_ID = 1
WAREHOUSE_ID = 1
OFFER_ID = 'o1'
SERVICE_PRICE = 20
CATEGORY_ID = 1
CATEGORY_NAME = 'cat_name'
OGRN = 'sup_ogrn'
SUPPLIER_NAME = 'sup'
CURRENT_TIME = NOW_UTC.strftime("%Y-%m-%dT%H:%M:%SZ")
CURRENT_TIMESTAMP = int((NOW_UTC - datetime(1970, 1, 1)).total_seconds())
UC_DATA_VERSION = 1


@pytest.fixture(scope='module')
def datacamp_messages():
    return [{
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': BUISNESS_ID,
                        'offer_id': OFFER_ID,
                    },
                    'meta': {
                        'ts_created': CURRENT_TIME,
                    },
                    'status': {
                        'version': {
                            'uc_data_version': {
                                'counter': UC_DATA_VERSION
                            }
                        },
                    },
                    'content': {
                        'market': {
                            'real_uc_version': {
                                'counter': UC_DATA_VERSION
                            },
                        },
                        'partner': {
                            'original': {
                                'category': {
                                    'id': CATEGORY_ID,
                                    'name': CATEGORY_NAME
                                },
                            }
                        }
                    }
                },
                'service': {
                    1: {
                        'identifiers': {
                            'business_id': BUISNESS_ID,
                            'offer_id': OFFER_ID,
                            'shop_id': SHOP_ID,
                            'warehouse_id': WAREHOUSE_ID,
                        },
                        'meta': {
                            'ts_created': CURRENT_TIME,
                            'rgb': 1
                        },
                        'price': {
                            'basic': {
                                'binary_price': {
                                    'price': SERVICE_PRICE
                                },
                            }
                        },
                        'content': {
                            'partner': {
                                'original': {
                                    'supplier_info': {
                                        'ogrn': OGRN,
                                        'name': SUPPLIER_NAME
                                    }
                                }
                            }
                        }
                    }
                }
            }]
        }]
    }]


@pytest.fixture(scope='module')
def datacamp_messages_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def pricelabs_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def config(yt_server, log_broker_stuff, datacamp_messages_topic, pricelabs_topic):
    cfg = {
        'logbroker': {
            'datacamp_messages_topic': datacamp_messages_topic.topic,
            'pricelabs_topic': pricelabs_topic.topic,
        },
    }
    return PiperConfigMock(yt_server=yt_server, log_broker_stuff=log_broker_stuff, config=cfg)


@pytest.yield_fixture(scope='module')
def piper(yt_server, log_broker_stuff, config, datacamp_messages_topic, pricelabs_topic):
    resources = {
        'config': config,
        'pricelabs_topic': pricelabs_topic,
        'datacamp_messages_topic': datacamp_messages_topic,
    }
    with PiperTestEnv(yt_server, log_broker_stuff, **resources) as piper_env:
        piper_env.verify()
        yield piper_env


@pytest.fixture(scope='module')
def inserter(piper, datacamp_messages_topic, datacamp_messages):
    for message in datacamp_messages:
        input_message = message_from_data(message, DatacampMessage())
        datacamp_messages_topic.write(input_message.SerializeToString())

    wait_until(lambda: piper.united_offers_processed >= 1)


def test_write_to_pricelabs(inserter, pricelabs_topic):
    data = pricelabs_topic.read(count=1)
    actual_message = ExportMessage()
    actual_message.ParseFromString(data[0])

    assert_that(protobuf_to_dict(actual_message), has_entries(
        {
            'offer': has_entries({
                'offer_id': OFFER_ID,
                'business_id': BUISNESS_ID,
                'offer_yabs_id': 9861167587634647073,
                'original_content': has_entries({
                    'shop_category_id': CATEGORY_ID,
                    'shop_category_name': CATEGORY_NAME,
                    'supplier': OGRN
                }),
                'price': has_entries({
                    'currency': 1,
                    'price': SERVICE_PRICE
                }),
                'shop_id': SHOP_ID,
                'timestamp': has_entries({
                    'seconds': CURRENT_TIMESTAMP
                }),
                'service': has_entries({
                    'platform': 1
                }),
                'version': has_entries({'version': greater_than(1)})
            })
        }
    ))
