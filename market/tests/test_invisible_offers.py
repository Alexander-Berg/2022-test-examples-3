# coding: utf-8

from hamcrest import assert_that, has_items, equal_to, not_
import pytest
import time

from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
import market.idx.datacamp.proto.subscription.subscription_pb2 as SUBS
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock

from market.idx.yatf.matchers.lb_matchers import HasNoUnreadData
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.datacamp.yatf.utils import create_update_meta_dict
from market.pylibrary.proto_utils import message_from_data


BUSINESS_ID = 1
SHOP_ID = 2


def make_offer(offer_id, invisible=False):
    return {
        'basic': {
            'identifiers': {
                'business_id': BUSINESS_ID,
                'offer_id': offer_id
            },
            'status': {
                'invisible': {
                    'flag': True
                } if invisible else None
            }
        },
        'service': {
            SHOP_ID: {
                'identifiers': {
                    'business_id': BUSINESS_ID,
                    'shop_id': SHOP_ID,
                    'offer_id': offer_id
                },
                'meta': {
                    'rgb': DTC.WHITE,
                },
                'status': {
                    'invisible': {
                        'flag': True
                    } if invisible else None
                }
            }
        },
        'actual': {
            SHOP_ID: {
                'warehouse': {
                    0: {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'shop_id': SHOP_ID,
                            'warehouse_id': 0,
                            'feed_id': 1234,
                            'offer_id': offer_id,
                        },
                        'meta': {
                            'rgb': DTC.WHITE
                        },
                        'status': {
                            'invisible': {
                                'flag': True
                            } if invisible else None
                        }
                    }
                }
            }
        }
    }


@pytest.fixture(scope='module')
def united_offers_table_data():
    return [
        make_offer('invisible_offer', invisible=True),
        make_offer('not_invisible_anymore_offer', invisible=True),
        make_offer('invisible_since_now_offer', invisible=False)
    ]


DATACAMP_MESSAGES = [
    # Флаг невидимости должен проставляться в самих контроллерах, имулируем логику
    # апдейтами, пока не будет сделан тикет MARKETINDEXER-47475
    message_from_data({
        'united_offers': [{'offer': [
            # создание нового невидимого оффера, должен отправиться
            {
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': 'new_invisible_offer'
                    },
                    'status': {
                        'invisible': {
                            'flag': True
                        }
                    }
                },
                'service': {
                    SHOP_ID: {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'shop_id': SHOP_ID,
                            'offer_id': 'new_invisible_offer',
                        },
                        'meta': {
                            'rgb': DTC.WHITE
                        }
                    }
                },
                'actual': {
                    SHOP_ID: {
                        'warehouse': {
                            0: {
                                'identifiers': {
                                    'business_id': BUSINESS_ID,
                                    'shop_id': SHOP_ID,
                                    'warehouse_id': 0,
                                    'feed_id': 1234,
                                    'offer_id': 'new_invisible_offer',
                                },
                            }
                        }
                    }
                }
            },
            # обновление существующего невидимого оффера, не должен отправиться
            {
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': 'invisible_offer'
                    },
                },
                'service': {
                    SHOP_ID: {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'shop_id': SHOP_ID,
                            'offer_id': 'invisible_offer',
                        },
                        'status': {
                            'disabled': [{
                                'flag': True,
                                'meta': create_update_meta_dict(int(time.time()), source=DTC.PUSH_PARTNER_FEED)
                            }]
                        }
                    }
                },
            },
            # оффер перестает быть невидимым, должен отправиться
            {
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': 'not_invisible_anymore_offer'
                    },
                    'content': {
                        'partner': {
                            'original': {
                                'description': {
                                    'value': 'offer description'
                                }
                            }
                        }
                    },
                    'status': {
                        'invisible': {
                            'flag': False,
                            'meta': create_update_meta_dict(int(time.time()))
                        }
                    }
                },
                'service': {
                    SHOP_ID: {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'shop_id': SHOP_ID,
                            'offer_id': 'not_invisible_anymore_offer',
                        },
                        'status': {
                            'disabled': [{
                                'flag': True,
                                'meta': create_update_meta_dict(int(time.time()), source=DTC.PUSH_PARTNER_FEED)
                            }]
                        }
                    }
                },
            },
            # нормальный оффер становится невидимым, должен отправиться
            {
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': 'invisible_since_now_offer'
                    },
                    'status': {
                        'invisible': {
                            'flag': True,
                            'meta': create_update_meta_dict(int(time.time()))
                        }
                    }
                },
                'service': {
                    SHOP_ID: {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'shop_id': SHOP_ID,
                            'offer_id': 'invisible_since_now_offer',
                        },
                        'status': {
                            'disabled': [{
                                'flag': True,
                                'meta': create_update_meta_dict(int(time.time()), source=DTC.PUSH_PARTNER_FEED)
                            }]
                        }
                    }
                },
            }
        ]}]
    }, DatacampMessage())
]


@pytest.fixture(scope='session')
def datacamp_messages_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='session')
def subscription_service_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff, 'subscription-service')
    return topic


@pytest.fixture(scope='session')
def rty_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff, 'rty_topic')
    return topic


@pytest.fixture(scope='session')
def config(yt_server, log_broker_stuff, datacamp_messages_topic, subscription_service_topic, rty_topic):
    cfg = {
        'general': {
            'color': 'white',
            'batch_size': 10,
        },
        'logbroker': {
            'datacamp_messages_topic': datacamp_messages_topic.topic,
            'subscription_service_topic': subscription_service_topic.topic,
            'rty_topic': rty_topic.topic,
        },
    }
    return PiperConfigMock(yt_server=yt_server, log_broker_stuff=log_broker_stuff, config=cfg)


@pytest.yield_fixture(scope='module')
def piper(
    yt_server,
    log_broker_stuff,
    config,
    datacamp_messages_topic,
    actual_service_offers_table,
    service_offers_table,
    basic_offers_table,
    subscription_service_topic,
    rty_topic
):
    resources = {
        'config': config,
        'datacamp_messages_topic': datacamp_messages_topic,
        'actual_service_offers_table': actual_service_offers_table,
        'service_offers_table': service_offers_table,
        'basic_offers_table': basic_offers_table,
        'subscription_service_topic': subscription_service_topic,
        'rty_topic': rty_topic,
    }
    with PiperTestEnv(yt_server, log_broker_stuff, **resources) as piper_env:
        piper_env.verify()
        yield piper_env


@pytest.yield_fixture(scope='module')
def inserter(piper, datacamp_messages_topic):
    for message in DATACAMP_MESSAGES:
        datacamp_messages_topic.write(message.SerializeToString())

    wait_until(lambda: piper.united_offers_processed >= 4, timeout=60)


def test_subscription_message(inserter, subscription_service_topic):
    data = subscription_service_topic.read(1, wait_timeout=10)
    assert_that(subscription_service_topic, HasNoUnreadData())

    assert_that(
        data,
        has_items(
            IsSerializedProtobuf(
                SUBS.UnitedOffersSubscriptionMessage,
                {
                    'subscription_request': [{
                        'business_id': BUSINESS_ID,
                        'offer_id': 'new_invisible_offer'
                    }],
                },
            ),
            IsSerializedProtobuf(
                SUBS.UnitedOffersSubscriptionMessage,
                {
                    'subscription_request': [{
                        'business_id': BUSINESS_ID,
                        'offer_id': 'not_invisible_anymore_offer'
                    }],
                },
            ),
            IsSerializedProtobuf(
                SUBS.UnitedOffersSubscriptionMessage,
                {
                    'subscription_request': [{
                        'business_id': BUSINESS_ID,
                        'offer_id': 'invisible_since_now_offer'
                    }],
                },
            ),
        ),
    )

    assert_that(
        data,
        not_(has_items(
            IsSerializedProtobuf(
                SUBS.UnitedOffersSubscriptionMessage,
                {
                    'subscription_request': [{
                        'business_id': BUSINESS_ID,
                        'offer_id': 'invisible_offer'
                    }],
                },
            )
        )),
    )


def test_rty_topic(inserter, rty_topic):
    data = rty_topic.read(1, wait_timeout=10)
    assert_that(rty_topic, HasNoUnreadData())
    # только один видимый оффер отправляется в rty
    assert_that(len(data), equal_to(1))
