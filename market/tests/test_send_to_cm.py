# coding: utf-8

import pytest
from datetime import datetime
from hamcrest import assert_that

from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock
from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.datacamp.yatf.utils import create_meta, create_update_meta
from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampBasicOffersTable, DataCampServiceOffersTable
from market.idx.yatf.matchers.lb_matchers import HasNoUnreadData
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf, IsProtobufMap
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.pylibrary.proto_utils import message_from_data


NOW_UTC = datetime.utcnow()  # JSON serializer should always use UTC
time_pattern = "%Y-%m-%dT%H:%M:%SZ"
current_time = NOW_UTC.strftime(time_pattern)

BUSINESS_ID = 1
SHOP_ID = 2

DATACAMP_MESSAGES = [
    {
        'united_offers': [{
            'offer': [{
                'service': {
                    SHOP_ID : {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': 'T1000',
                            'shop_id': SHOP_ID
                        },
                        'meta': {
                            'scope': DTC.SERVICE,
                            'rgb': DTC.WHITE,
                        },
                        'content': {
                            'partner': {
                                'original': {
                                    'description':  {
                                        'value': 'new description',
                                        'meta': {
                                            'timestamp': current_time,
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
            }]
        }]
    }
]


@pytest.fixture(scope='module')
def datacamp_messages_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def cm_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def config(yt_server, log_broker_stuff, datacamp_messages_topic, cm_topic):
    cfg = {
        'general': {
            'color': 'white',
        },
        'logbroker': {
            'datacamp_messages_topic': datacamp_messages_topic.topic,
            'cm_topic': cm_topic.topic
        },
    }
    return PiperConfigMock(yt_server=yt_server, log_broker_stuff=log_broker_stuff, config=cfg)


@pytest.fixture(scope='module')
def basic_offers_table(yt_server, config):
    return DataCampBasicOffersTable(yt_server, config.yt_basic_offers_tablepath, data=[
        {
            'business_id': BUSINESS_ID,
            'shop_sku': 'T1000',
            'offer': DTC.Offer(
                identifiers=DTC.OfferIdentifiers(
                    business_id=BUSINESS_ID,
                    offer_id='T1000',
                ),
                meta=create_meta(10),
                pictures=DTC.OfferPictures(
                    partner=DTC.PartnerPictures(
                        original=DTC.SourcePictures(
                            meta=create_update_meta(10),
                            source=[
                                DTC.SourcePicture(url='picture url'),
                            ]
                        )
                    )
                ),
            ).SerializeToString()
        }
    ])


@pytest.fixture(scope='module')
def service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(yt_server, config.yt_service_offers_tablepath, data=[
        {
            'business_id': BUSINESS_ID,
            'shop_sku': 'T1000',
            'shop_id': SHOP_ID,
            'warehouse_id': 0,
            'outlet_id': 0,
            'offer': DTC.Offer(
                identifiers=DTC.OfferIdentifiers(
                    business_id=BUSINESS_ID,
                    offer_id='T1000',
                    shop_id=SHOP_ID,
                ),
                meta=create_meta(10, color=DTC.WHITE),
                content=DTC.OfferContent(
                    partner=DTC.PartnerContent(
                        original=DTC.OriginalSpecification(
                            name=DTC.StringValue(
                                meta=create_update_meta(10),
                                value="name",
                            )
                        )
                    )
                )
            ).SerializeToString()
        }
    ])


@pytest.fixture(scope='module')
def actual_service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(yt_server, config.yt_actual_service_offers_tablepath, data=[
        {
            'business_id': BUSINESS_ID,
            'shop_sku': 'T1000',
            'shop_id': SHOP_ID,
            'warehouse_id': 0,
            'outlet_id': 0,
            'offer': DTC.Offer(
                identifiers=DTC.OfferIdentifiers(
                    business_id=BUSINESS_ID,
                    offer_id='T1000',
                    shop_id=SHOP_ID,
                    warehouse_id=0
                ),
                meta=create_meta(10, color=DTC.WHITE),
                partner_info=DTC.PartnerInfo(
                    meta=create_update_meta(10),
                    shop_name="shop name"
                )
            ).SerializeToString()
        }
    ])


@pytest.yield_fixture(scope='module')
def piper(
    yt_server,
    log_broker_stuff,
    config,
    datacamp_messages_topic,
    cm_topic,
    basic_offers_table,
    service_offers_table,
    actual_service_offers_table
):
    resources = {
        'config': config,
        'datacamp_messages_topic': datacamp_messages_topic,
        'cm_topic': cm_topic,
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table
    }
    with PiperTestEnv(yt_server, log_broker_stuff, **resources) as piper_env:
        piper_env.verify()
        yield piper_env


def test_write_to_cm(piper, datacamp_messages_topic, cm_topic):
    for message in DATACAMP_MESSAGES:
        datacamp_messages_topic.write(message_from_data(message, DatacampMessage()).SerializeToString())
    wait_until(lambda: piper.united_offers_processed >= 1)

    data = cm_topic.read(count=1)
    assert_that(data[0], IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': 'T1000',
                    },
                    'pictures': {
                        'partner': {
                            'original': {
                                'source': [{
                                    'url': 'picture url'
                                }]
                            }
                        }
                    }
                },
                'service': IsProtobufMap({
                    SHOP_ID: {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'shop_id': SHOP_ID,
                            'offer_id': 'T1000',
                            'warehouse_id': 0,
                        },
                        'content': {
                            'partner': {
                                'original': {
                                    'name': {
                                        'value': 'name'
                                    },
                                    'description': {
                                        'value': 'new description'
                                    }
                                }
                            },
                        },
                        'partner_info': {
                            'shop_name': 'shop name'
                        }
                    },
                }),
            }]
        }]
    }))

    # проверяем, что в топике больше нет данных, которые мы можем вычитать
    assert_that(cm_topic, HasNoUnreadData())
