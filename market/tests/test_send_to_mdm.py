# coding: utf-8

import pytest
from datetime import datetime
from hamcrest import assert_that, not_, greater_than

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
from market.idx.pylibrary.datacamp.conversion import offer_to_service_row, offer_to_basic_row

NOW_UTC = datetime.utcnow()  # JSON serializer should always use UTC
time_pattern = "%Y-%m-%dT%H:%M:%SZ"
current_time = NOW_UTC.strftime(time_pattern)

BUSINESS_ID = 1
# синий магазин
BLUE_SHOP_ID = 2
# НЕ должны отправлять не-синие оффера
WHITE_SHOP_ID = 4
# отправляем оффер, даже если у него нет актуальной части
BLUE_SHOP_ID_ONLY_ORIGINAL = 5
WAREHOUSE_ID = 145

DATACAMP_MESSAGES = [
    {
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': 'T1000',
                    },
                    'meta': {
                        'scope': DTC.BASIC,
                        'ts_created': current_time
                    },
                    'content': {
                        'market': {
                            'category_id': 20,
                            'meta': {
                                'timestamp': current_time
                            }
                        },
                        'partner': {
                            'original': {
                                # поле триггерящее отправку в mdm
                                'barcode': {
                                    'value': ["123"],
                                    'meta' : {
                                        'timestamp': current_time,
                                    }
                                }
                            }
                        }
                    }
                },
                'service': {
                    BLUE_SHOP_ID: {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': 'T1000',
                            'shop_id': BLUE_SHOP_ID,
                        },
                        'meta': {
                            'scope': DTC.SERVICE,
                            'rgb': DTC.BLUE
                        },
                    }
                }
            }, {
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': 'invisible_offer',
                    },
                    'meta': {
                        'scope': DTC.BASIC,
                        'ts_created': current_time
                    },
                    'content': {
                        'partner': {
                            'original': {
                                'barcode': {
                                    'value': ["123"],
                                    'meta' : {
                                        'timestamp': current_time,
                                    }
                                }
                            }
                        }
                    }
                },
                'service': {
                    BLUE_SHOP_ID: {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': 'invisible_offer',
                            'shop_id': BLUE_SHOP_ID_ONLY_ORIGINAL,
                        },
                        'meta': {
                            'scope': DTC.SERVICE,
                            'rgb': DTC.BLUE
                        },
                    }
                }
            }]
        }]
    }
]


@pytest.fixture(scope='module')
def datacamp_messages_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def mdm_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def subscription_service_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff, 'subscription-service')
    return topic


@pytest.fixture(scope='module')
def config(yt_server, log_broker_stuff, datacamp_messages_topic, mdm_topic, subscription_service_topic):
    cfg = {
        'general': {
            'color': 'white',
            'enable_subscription_dispatcher': True,
        },
        'logbroker': {
            'datacamp_messages_topic': datacamp_messages_topic.topic,
            'mdm_topic': mdm_topic.topic,
            'subscription_service_topic': subscription_service_topic.topic,
        },
    }
    return PiperConfigMock(yt_server=yt_server, log_broker_stuff=log_broker_stuff, config=cfg)


@pytest.fixture(scope='module')
def basic_offers_table(yt_server, config):
    return DataCampBasicOffersTable(yt_server, config.yt_basic_offers_tablepath, data=[
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id='T1000',
            ),
            meta=create_meta(10),
            content=DTC.OfferContent(
                market=DTC.MarketContent(
                    category_id=10,
                    meta=create_update_meta(10)
                )
            ))),
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id='invisible_offer',
            ),
            meta=create_meta(10),
            status=DTC.OfferStatus(
                invisible=DTC.Flag(
                    flag=True
                )
            )))
        ])


@pytest.fixture(scope='module')
def service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(yt_server, config.yt_service_offers_tablepath, data=[
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id='T1000',
                shop_id=BLUE_SHOP_ID,
            ),
            meta=create_meta(10, color=DTC.BLUE),
            status=DTC.OfferStatus(
                united_catalog=DTC.Flag(
                    flag=True,
                )
            )
        )),
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id='T1000',
                shop_id=WHITE_SHOP_ID,
            ),
            meta=create_meta(10, color=DTC.WHITE),
        )),
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id='T1000',
                shop_id=BLUE_SHOP_ID_ONLY_ORIGINAL,
            ),
            meta=create_meta(10, color=DTC.BLUE),
            status=DTC.OfferStatus(
                united_catalog=DTC.Flag(
                    flag=True,
                )
            )
        )),
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id='invisible_offer',
                shop_id=BLUE_SHOP_ID_ONLY_ORIGINAL,
            ),
            meta=create_meta(10, color=DTC.BLUE),
            status=DTC.OfferStatus(
                united_catalog=DTC.Flag(
                    flag=True,
                )
            )
        ))])


@pytest.fixture(scope='module')
def actual_service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(yt_server, config.yt_actual_service_offers_tablepath, data=[
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id='T1000',
                shop_id=BLUE_SHOP_ID,
                warehouse_id=WAREHOUSE_ID
            ),
            meta=create_meta(10, color=DTC.BLUE),
        )),
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id='T1000',
                shop_id=WHITE_SHOP_ID,
                warehouse_id=0
            ),
            meta=create_meta(10, color=DTC.WHITE),
        ))])


@pytest.yield_fixture(scope='module')
def piper(
    yt_server,
    log_broker_stuff,
    config,
    datacamp_messages_topic,
    mdm_topic,
    basic_offers_table,
    service_offers_table,
    actual_service_offers_table,
    subscription_service_topic
):
    resources = {
        'config': config,
        'datacamp_messages_topic': datacamp_messages_topic,
        'mdm_topic': mdm_topic,
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
        'subscription_service_topic': subscription_service_topic,
    }
    with PiperTestEnv(yt_server, log_broker_stuff, **resources) as piper_env:
        piper_env.verify()
        yield piper_env


def test_write_to_mdm(piper, datacamp_messages_topic, mdm_topic):
    for message in DATACAMP_MESSAGES:
        datacamp_messages_topic.write(message_from_data(message, DatacampMessage()).SerializeToString())
    wait_until(lambda: piper.united_offers_processed >= 1)

    data = mdm_topic.read(count=1)
    assert_that(data[0], IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': 'T1000',
                    }
                },
            }]
        }]
    }))

    assert_that(data[0], not_(IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': 'invisible_offer',
                    }
                },
            }]
        }]
    })))

    # невидимые оффера не пишутся подписчикам
    assert_that(data[0], not_(IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [{
                'service': IsProtobufMap({
                    WHITE_SHOP_ID: {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'shop_id': WHITE_SHOP_ID,
                            'offer_id': 'T1000',
                        },
                    },
                }),
            }]
        }]
    })))

    datacamp_messages_topic.write(message_from_data(
        {
            'united_offers': [{
                'offer': [{
                    'basic': {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': 'T1000',
                        },
                        'meta': {
                            'mdm_force_send': {
                                'ts': current_time,
                                'meta': {
                                    'timestamp': current_time
                                }
                            }
                        }
                    },
                }]
            }]
        }, DatacampMessage()).SerializeToString())

    data = mdm_topic.read(count=1)
    assert_that(data[0], IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': 'T1000',
                    },
                    'meta': {
                        'mdm_force_send': {
                            'ts': {
                                'seconds': greater_than(0)
                            }
                        }
                    }
                },
                'service': IsProtobufMap({
                    BLUE_SHOP_ID_ONLY_ORIGINAL: {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'shop_id': BLUE_SHOP_ID_ONLY_ORIGINAL,
                            'offer_id': 'T1000',
                        },
                    },
                }),
            }]
        }]
    }))

    # проверяем, что в топике больше нет данных, которые мы можем вычитать
    assert_that(mdm_topic, HasNoUnreadData())
