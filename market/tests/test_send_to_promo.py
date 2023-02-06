# coding: utf-8

import pytest
from datetime import datetime, timedelta
from hamcrest import assert_that, not_

import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock
from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.pylibrary.datacamp.utils import wait_until
from market.pylibrary.proto_utils import message_from_data
from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampBasicOffersTable, DataCampServiceOffersTable
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf, IsProtobufMap
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.matchers.yt_rows_matchers import HasOffers
from market.idx.pylibrary.datacamp.conversion import offer_to_service_row, offer_to_basic_row

NOW_UTC = datetime.utcnow()  # JSON serializer should always use UTC
OLD_TIME_UTC = NOW_UTC - timedelta(minutes=45)
time_pattern = "%Y-%m-%dT%H:%M:%SZ"
old_ts = OLD_TIME_UTC.strftime(time_pattern)


@pytest.fixture(scope='module', params=['blue', 'white'])
def color(request):
    return DTC.BLUE if request.param == 'blue' else DTC.WHITE


@pytest.fixture(scope='module')
def datacamp_messages(color):
    return [{
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
                },
                'service': {
                    1: {
                        'identifiers': {
                            'business_id': 1,
                            'offer_id': 'o1',
                            'shop_id': 1,
                            'warehouse_id': 1,
                        },
                        'meta': {
                            'scope': DTC.SERVICE,
                            'rgb': color,
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
                        'promos': {
                            'anaplan_promos' : {
                                'all_promos' : {
                                    'meta': {
                                        'timestamp': NOW_UTC.strftime(time_pattern),
                                    },
                                    'promos' : [{'id': 'promo_1'}],
                                }
                            }
                        },
                    },
                }
            },
            {
                'basic': {
                    'identifiers': {
                        'business_id': 2,
                        'offer_id': 'o2',
                    },
                    'meta': {
                        'scope': DTC.BASIC,
                        'ts_created': NOW_UTC.strftime(time_pattern)
                    },
                },
                'service': {
                    2: {
                        'identifiers': {
                            'business_id': 2,
                            'offer_id': 'o2',
                            'shop_id': 2,
                        },
                        'meta': {
                            'scope': DTC.SERVICE,
                            'rgb': color,
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
                        'promos': {
                            'anaplan_promos' : {
                                'all_promos' : {
                                    'meta': {
                                        'timestamp': NOW_UTC.strftime(time_pattern),
                                    },
                                    'promos' : [
                                        {'id': 'promo_1'},
                                    ],
                                },
                            },
                        },
                    },
                },
            },
            {
                'basic': {
                    'identifiers': {
                        'business_id': 3,
                        'offer_id': 'o3',
                    },
                    'meta': {
                        'scope': DTC.BASIC,
                        'ts_created': NOW_UTC.strftime(time_pattern)
                    },
                },
                'service': {
                    3: {
                        'identifiers': {
                            'business_id': 3,
                            'offer_id': 'o3',
                            'shop_id': 3,
                        },
                        'meta': {
                            'scope': DTC.SERVICE,
                            'rgb': color,
                        },
                        'price': {
                            'basic': {
                                'binary_price': {
                                    'price': 30
                                },
                                'meta': {
                                    'timestamp': NOW_UTC.strftime(time_pattern)
                                }
                            }
                        },
                        'promos': {
                            'anaplan_promos' : {
                                'all_promos' : {
                                    'meta': {
                                        'timestamp': NOW_UTC.strftime(time_pattern),
                                    },
                                    'promos' : [
                                        {'id': 'promo_1'},
                                        {'id': 'promo_2'},
                                        {'id': 'promo_3'},
                                    ],
                                },
                            },
                        },
                    },
                },
            }]
        }]
    }]


@pytest.fixture(scope='module')
def promo_in_messages_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def promo_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def config(yt_server, log_broker_stuff, promo_in_messages_topic, promo_topic, color):
    cfg = {
        'logbroker': {
            'promo_in_messages_topic' : promo_in_messages_topic.topic,
        },
        'general': {
            'color': color,
        },
        'promo_sender': {
            'topic': promo_topic.topic,
        },
    }
    return PiperConfigMock(yt_server=yt_server, log_broker_stuff=log_broker_stuff, config=cfg)


@pytest.fixture(scope='module')
def basic_offers_table(yt_server, config):
    return DataCampBasicOffersTable(yt_server, config.yt_basic_offers_tablepath, data=[
        offer_to_basic_row(message_from_data({
            'identifiers': {
                'business_id': 2,
                'offer_id': 'o2',
            },
            'meta': {
                'scope': DTC.BASIC,
            },
        }, DTC.Offer())),
        offer_to_basic_row(message_from_data({
            'identifiers': {
                'business_id': 3,
                'offer_id': 'o3',
            },
            'meta': {
                'scope': DTC.BASIC,
            },
        }, DTC.Offer())),
    ])


@pytest.fixture(scope='module')
def service_offers_table(yt_server, config, color):
    return DataCampServiceOffersTable(yt_server, config.yt_service_offers_tablepath, data=[
        offer_to_service_row(message_from_data({
            'identifiers': {
                'business_id': 2,
                'offer_id': 'o2',
                'shop_id': 2,
            },
            'meta': {
                'scope': DTC.SERVICE,
                'rgb': color,
            },
            'promos': {
                'anaplan_promos' : {
                    'active_promos' : {
                        'promos' : [
                            {'id': 'promo_1'},
                            {'id': 'promo_2'},
                        ],
                    },
                    'all_promos' : {
                        'promos' : [
                            {'id': 'promo_1'},
                            {'id': 'promo_2'},
                        ],
                    },
                },
            },
        }, DTC.Offer())),
        offer_to_service_row(message_from_data({
            'identifiers': {
                'business_id': 3,
                'offer_id': 'o3',
                'shop_id': 3,
            },
            'meta': {
                'scope': DTC.SERVICE,
                'rgb': color,
            },
            'promos': {
                'anaplan_promos' : {
                    'active_promos' : {
                        'promos' : [
                            {'id': 'promo_1'},
                        ],
                    },
                    'all_promos' : {
                        'meta': {
                            'timestamp': old_ts,
                        },
                        'promos' : [
                            {'id': 'promo_1'},
                            {'id': 'promo_2'},
                        ],
                    },
                },
            },
        }, DTC.Offer())),
    ])


@pytest.fixture(scope='module')
def actual_service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(yt_server, config.yt_actual_service_offers_tablepath)


@pytest.yield_fixture(scope='module')
def piper(
    yt_server,
    log_broker_stuff,
    config,
    promo_in_messages_topic,
    basic_offers_table,
    service_offers_table,
    actual_service_offers_table,
    promo_topic
):
    resources = {
        'config': config,
        'promo_topic': promo_topic,
        'promo_in_messages_topic': promo_in_messages_topic,
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
    }
    with PiperTestEnv(yt_server, log_broker_stuff, **resources) as piper_env:
        piper_env.verify()
        yield piper_env


@pytest.fixture(scope='module')
def inserter(piper, promo_in_messages_topic, datacamp_messages):
    for message in datacamp_messages:
        promo_in_messages_topic.write(message_from_data(message, DatacampMessage()).SerializeToString())

    wait_until(lambda: piper.united_offers_processed >= 3)


@pytest.fixture(scope='module')
def expected_service_offer(color):
    return {
        'identifiers': {
            'business_id': 1,
            'shop_id': 1,
            'offer_id': 'o1',
        },
        'price': {
            'basic': {
                'binary_price': {
                    'price': 10
                },
            }
        },
        'promos': {
            'anaplan_promos': {
                'all_promos': {
                    'promos': [{'id': 'promo_1'}],
                }
            }
        },
    }


def test_write_to_promo(inserter, promo_topic, service_offers_table, expected_service_offer):
    batch = promo_topic.read(1)[0]

    service_offers_table.load()
    assert_that(service_offers_table.data, HasOffers([message_from_data(expected_service_offer, DTC.Offer())]))

    assert_that(batch, IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [
                {
                    'service': IsProtobufMap({
                        1: expected_service_offer,
                    }),
                }
            ]
        }]
    }))


def test_active_promos_1(inserter, piper):
    assert_that(piper.service_offers_table.data, HasOffers([message_from_data(
        {
            'identifiers': {
                'business_id': 2,
                'shop_id': 2,
                'offer_id': 'o2',
            },
            'promos': {
                'anaplan_promos': {
                    'active_promos': {
                        'promos': [{'id': 'promo_1'}],
                    },
                    'all_promos': {
                        'promos': [{'id': 'promo_1'}],
                    }
                }
            },
        }, DTC.Offer()
    )]))
    assert_that(piper.service_offers_table.data, not_(HasOffers([message_from_data(
        {
            'identifiers': {
                'business_id': 2,
                'shop_id': 2,
                'offer_id': 'o2',
            },
            'promos': {
                'anaplan_promos': {
                    'active_promos': {
                        'promos': [{'id': 'promo_2'}],
                    },
                    'all_promos': {
                        'promos': [{'id': 'promo_2'}],
                    }
                }
            },
        }, DTC.Offer()
    )])))


def test_active_promos_3(inserter, piper):
    assert_that(piper.service_offers_table.data, HasOffers([message_from_data(
        {
            'identifiers': {
                'business_id': 3,
                'shop_id': 3,
                'offer_id': 'o3',
            },
            'promos': {
                'anaplan_promos': {
                    'active_promos': {
                        'promos': [{'id': 'promo_1'}],
                    },
                    'all_promos': {
                        'promos': [
                            {'id': 'promo_1'},
                            {'id': 'promo_2'},
                            {'id': 'promo_3'},
                        ],
                    },
                },
            },
        }, DTC.Offer()
    )]))
