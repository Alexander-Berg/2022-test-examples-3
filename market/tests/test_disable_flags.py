# coding: utf-8

import pytest
from hamcrest import assert_that, is_not
from datetime import datetime, timedelta

from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock
from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import HIDDEN, AVAILABLE, MARKET_IDX, MARKET_STOCK, MARKET_SCM
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import Offer as DatacampOffer
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import PUSH_PARTNER_API, PUSH_PARTNER_OFFICE, PUSH_PARTNER_FEED, MARKET_ABO
from market.idx.datacamp.proto.offer.OfferStatus_pb2 import ReadinessForPublicationStatus
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.datacamp.yatf.utils import dict2tskv
from market.idx.yatf.matchers.yt_rows_matchers import HasOffers
from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampPartnersTable
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.pylibrary.proto_utils import message_from_data


NOW_UTC = datetime.utcnow()  # JSON serializer should always use UTC
time_pattern = "%Y-%m-%dT%H:%M:%SZ"


def make_price(price):
    return {
        'basic': {
            'binary_price': {
                'price': price,
                'id': 'RUR'
            },
            'meta': {
                'timestamp': NOW_UTC.strftime(time_pattern),
                'source': PUSH_PARTNER_FEED,
            }
        }
    }


OFFERS = [
    {
        'identifiers': {
            'shop_id': 1,
            'business_id': 1,
            'offer_id': 'TestPushPartnerDisableFlags01',
            'warehouse_id': 42,
        },
        'status': {
            'disabled': [
                {
                    'flag': True,
                    'meta': {
                        'source': PUSH_PARTNER_API,
                        'timestamp': NOW_UTC.strftime(time_pattern),
                    },
                },
            ],
        },
    },
    {
        'identifiers': {
            'shop_id': 1,
            'business_id': 1,
            'offer_id': 'TestDisableFlagsUpdate01',
            'warehouse_id': 42
        },
        'price': make_price(100),
    },
    {
        'identifiers': {
            'shop_id': 1,
            'business_id': 1,
            'offer_id': 'TestPublishByPartnerStatus01',
            'warehouse_id': 42,
        },
        'status': {
            'disabled': [
                {
                    'flag': True,
                    'meta': {
                        'source': PUSH_PARTNER_API,
                        'timestamp': NOW_UTC.strftime(time_pattern),
                    },
                },
            ],
        },
    },
    {
        'identifiers': {
            'shop_id': 1,
            'business_id': 1,
            'offer_id': 'TestPublishByPartnerStatus02',
            'warehouse_id': 42,
        },
        'status': {
            'disabled': [
                {
                    'flag': True,
                    'meta': {
                        'source': MARKET_ABO,
                        'timestamp': NOW_UTC.strftime(time_pattern),
                    },
                },
            ],
        },
    },
    {
        'identifiers': {
            'shop_id': 1,
            'business_id': 1,
            'offer_id': 'TestHasGone',
            'warehouse_id': 42,
        },
        'price': make_price(100),
        'status': {
            'disabled': [
                {
                    'flag': False,
                    'meta': {
                        'source': MARKET_IDX,
                        'timestamp': NOW_UTC.strftime(time_pattern),
                    },
                },
            ],
            'has_gone': {
                'flag': False,
                'meta': {
                    'source': MARKET_IDX,
                    'timestamp': NOW_UTC.strftime(time_pattern),
                },
            },
        },
    },
    {
        'identifiers': {
            'shop_id': 1,
            'business_id': 1,
            'offer_id': 'TestStockStoreFlagDeletion',
            'warehouse_id': 4,
        },
        'status': {
            'disabled': [
                {
                    'flag': True,
                    'meta': {
                        'source': MARKET_STOCK,
                        'timestamp': NOW_UTC.strftime(time_pattern),
                    },
                },
            ],
        }
    },
    {
        'identifiers': {
            'shop_id': 1,
            'business_id': 1,
            'offer_id': 'TestMboHiding',
            'warehouse_id': 5,
        },
        'price': make_price(100),
        'status': {
            'disabled': [
                {
                    'flag': False,
                    'meta': {
                        'source': MARKET_IDX,
                        'timestamp': NOW_UTC.strftime(time_pattern),
                    },
                },
            ],
        }
    }
]


@pytest.fixture(scope='module')
def partners_table(yt_server, config):
    SHOPS = [
        {
            'shop_id': 1,
            'mbi': '\n\n'.join([
                dict2tskv({
                    'shop_id': 1,
                    'warehouse_id': 5,
                    'datafeed_id': 100,
                    'business_id': 1
                }),
            ]),
            'status': 'publish'
        }
    ]

    return DataCampPartnersTable(
        yt_server,
        config.yt_partners_tablepath,
        data=SHOPS
    )


@pytest.fixture(scope='module')
def offers():
    return [message_from_data(offer, DatacampOffer()) for offer in OFFERS]


@pytest.fixture(scope='module')
def lbk_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def mbo_hiding_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def config(yt_server, log_broker_stuff, lbk_topic, mbo_hiding_topic):
    cfg = {
        'logbroker': {
            'offers_topic': lbk_topic.topic,
            'mbo_hiding_topic': mbo_hiding_topic.topic,
        },
        'general': {
            'color': 'blue',
        }
    }
    return PiperConfigMock(yt_server=yt_server,
                           log_broker_stuff=log_broker_stuff,
                           config=cfg)


@pytest.yield_fixture(scope='module')
def piper(yt_server, log_broker_stuff, config, lbk_topic, mbo_hiding_topic, partners_table):
    resources = {
        'config': config,
        'offers_topic': lbk_topic,
        'partners_table': partners_table,
        'mbo_hiding_topic': mbo_hiding_topic,
    }
    with PiperTestEnv(yt_server, log_broker_stuff, **resources) as piper_env:
        piper_env.verify()
        yield piper_env


@pytest.fixture(scope='module')
def inserter(offers, piper, lbk_topic):
    for offer in offers:
        lbk_topic.write(offer.SerializeToString())

    wait_until(lambda: piper.united_offers_processed >= len(offers))


def test_mbo_hiding(inserter, piper, mbo_hiding_topic):
    """
    Проверяем, что piper:
      - создает офферы со скрытием в ActualServiceOffers
      - создает пустой оффер в BasicOffers
      - создает офферы в ServiceOffers
    """
    ts = (NOW_UTC + timedelta(minutes=45)).strftime(time_pattern)
    message = message_from_data({
        'offers': [{
            'offer': [
                {
                    'identifiers': {
                        'shop_id': 1,
                        'business_id': 1,
                        'offer_id': 'TestMboHiding',
                        'warehouse_id': 5,
                    },
                    'status': {
                        'disabled': [
                            {
                                'flag': True,
                                'meta': {
                                    'source': MARKET_SCM,
                                    'timestamp': ts,
                                },
                            },
                        ],
                    }
                }
            ]
        }]
    }, DatacampMessage())
    offers_processed = piper.united_offers_processed
    mbo_hiding_topic.write(message.SerializeToString())

    wait_until(lambda: piper.united_offers_processed >= offers_processed + 1)

    new_offer_message = message_from_data({
        'offers': [{
            'offer': [
                {
                    'identifiers': {
                        'shop_id': 1,
                        'business_id': 1,
                        'offer_id': 'TestMboHiding2',
                        'warehouse_id': 5,
                    },
                    'status': {
                        'disabled': [
                            {
                                'flag': True,
                                'meta': {
                                    'source': MARKET_SCM,
                                    'timestamp': ts,
                                },
                            },
                        ],
                    }
                }
            ]
        }]
    }, DatacampMessage())
    offers_processed = piper.united_offers_processed
    mbo_hiding_topic.write(new_offer_message.SerializeToString())

    wait_until(lambda: piper.united_offers_processed >= offers_processed + 1)

    piper.basic_offers_table.load()
    assert_that(piper.basic_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': shop_sku,
            }
        }, DatacampOffer()) for shop_sku in ['TestMboHiding', 'TestMboHiding2']]))

    piper.actual_service_offers_table.load()
    assert_that(piper.actual_service_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': data[0],
                'shop_id': 1,
                'warehouse_id': 5,
            },
            'status': {
                'disabled': [{
                    'flag': data[1],
                    'meta': {
                        'source': MARKET_IDX
                    }
                }, {
                    'flag': data[2],
                    'meta': {
                        'source': MARKET_SCM,
                        'timestamp': ts
                    }
                }],
            },
        }, DatacampOffer())
        for data in [
            ('TestMboHiding', False, True),
            ('TestMboHiding2', True, True)
        ]]))

    not_expected_offers = [{'identifiers': {'offer_id': shop_sku, 'business_id': 1}} for shop_sku in ['TestMboHiding', 'TestMboHiding2']]
    piper.service_offers_table.load()
    assert_that(piper.service_offers_table.data, HasOffers([message_from_data(o, DatacampOffer()) for o in not_expected_offers]))


def test_disabled_by_partner_status(inserter, piper, lbk_topic):
    shop_id = 1
    warehouse_id = 42
    offer_id01 = 'TestPublishByPartnerStatus01'
    offer_id02 = 'TestPublishByPartnerStatus02'
    error_msg = 'Publish by partner status is incorrect'

    # шаг 1 - проверяем, что офер с disable флагом от PUSH_PARTNER_API - скрыт партнером,
    # а с disable флагом от MARKET_ABO - не скрыт партнером
    assert_that(piper.service_offers_table.data,
                HasOffers([message_from_data({
                    'identifiers': {
                        'shop_id': 1,
                        'business_id': 1,
                        'offer_id': offer_id01,
                    },
                    'status': {
                        'publish_by_partner': HIDDEN,
                    }
                }, DatacampOffer())]), error_msg)

    assert_that(piper.service_offers_table.data,
                is_not(HasOffers([message_from_data({
                    'identifiers': {
                        'shop_id': 1,
                        'business_id': 1,
                        'offer_id': offer_id02,
                    },
                    'status': {
                        'publish_by_partner': HIDDEN,
                    }
                }, DatacampOffer()),
                ])), error_msg)

    assert_that(piper.actual_service_offers_table.data,
                HasOffers([
                    message_from_data({
                        'identifiers': {
                            'shop_id': 1,
                            'business_id': 1,
                            'offer_id': offer_id01,
                            'warehouse_id': 42,
                        },
                    }, DatacampOffer()),
                    message_from_data({
                        'identifiers': {
                            'shop_id': 1,
                            'business_id': 1,
                            'offer_id': offer_id02,
                            'warehouse_id': 42,
                        },
                    }, DatacampOffer())
                ]), error_msg)

    # шаг 2 - отправляем изменения - раскрытие и скрытие от партнера
    new_time = (NOW_UTC + timedelta(minutes=45)).strftime(time_pattern)
    update_part1 = {
        'identifiers': {
            'shop_id': shop_id,
            'offer_id': offer_id01,
            'warehouse_id': warehouse_id,
            'business_id': 1,
        },
        'status': {
            'disabled': [
                {
                    'flag': False,
                    'meta': {
                        'source': PUSH_PARTNER_OFFICE,
                        'timestamp': new_time,
                    }
                }
            ],
        }
    }
    update_part2 = {
        'identifiers': {
            'shop_id': shop_id,
            'offer_id': offer_id02,
            'warehouse_id': warehouse_id,
            'business_id': 1,
        },
        'status': {
            'disabled': [
                {
                    'flag': True,
                    'meta': {
                        'source': PUSH_PARTNER_FEED,
                        'timestamp': new_time,
                    }
                }
            ],
        }
    }
    offers_processed = piper.united_offers_processed
    lbk_topic.write(message_from_data(update_part1, DatacampOffer()).SerializeToString())
    lbk_topic.write(message_from_data(update_part2, DatacampOffer()).SerializeToString())
    wait_until(lambda: piper.united_offers_processed >= offers_processed + 2)

    # шаг 3 - проверяем, что изменения появились в хранилище
    assert_that(piper.service_offers_table.data,
                HasOffers([message_from_data({
                    'identifiers': {
                        'shop_id': 1,
                        'offer_id': offer_id01,
                        'business_id': 1,
                    },
                    'status': {
                        'publish_by_partner': AVAILABLE,
                    }
                }, DatacampOffer()),
                    message_from_data({
                        'identifiers': {
                            'shop_id': 1,
                            'offer_id': offer_id02,
                            'business_id': 1,
                        },
                        'status': {
                            'publish_by_partner': HIDDEN,
                        }
                    }, DatacampOffer()),
                ]), error_msg)

    assert_that(piper.actual_service_offers_table.data,
                HasOffers([
                    message_from_data({
                        'identifiers': {
                            'shop_id': 1,
                            'offer_id': offer_id01,
                            'warehouse_id': 42,
                            'business_id': 1,
                        },
                    }, DatacampOffer()),
                    message_from_data({
                        'identifiers': {
                            'shop_id': 1,
                            'offer_id': offer_id02,
                            'warehouse_id': 42,
                            'business_id': 1,
                        },
                    }, DatacampOffer())
                ]), error_msg)


def test_disable_flags_from_partner(inserter, piper, lbk_topic):
    """Проверяет, что из 3х флажков с партнерским типом остается только один и самый свежий.
    Другими словами типы PUSH_PARTNER_API, PUSH_PARTNER_OFFICE, PUSH_PARTNER_FEED являются равнозначными."""

    shop_id = 1
    offer_id = 'TestPushPartnerDisableFlags01'
    warehouse_id = 42

    # шаг 1 - проверяем, что исходный флажок с типом PUSH_PARTNER_API попадает в хранилище
    current_time = NOW_UTC.strftime(time_pattern)
    assert_that(piper.service_offers_table.data,
                HasOffers([message_from_data({
                    'identifiers': {
                        'shop_id': shop_id,
                        'offer_id': offer_id,
                        'business_id': 1,
                    },
                    'status': {
                        'disabled': [
                            {
                                'flag': True,
                                'meta': {
                                    'source': PUSH_PARTNER_API,
                                    'timestamp': current_time
                                }
                            }
                        ],
                    }
                }, DatacampOffer())]),
                'Disabled flag from push partner was inserted')

    # шаг 2 - отправляем изменения - флажок со свежим ts и типом PUSH_PARTNER_OFFICE
    new_time = (NOW_UTC + timedelta(minutes=45)).strftime(time_pattern)
    update_part = {
        'identifiers': {
            'shop_id': shop_id,
            'offer_id': offer_id,
            'warehouse_id': warehouse_id,
            'business_id': 1,
        },
        'status': {
            'disabled': [
                {
                    'flag': True,
                    'meta': {
                        'source': PUSH_PARTNER_OFFICE,
                        'timestamp': new_time,
                    }
                }
            ],
        }
    }
    offers_processed = piper.united_offers_processed
    lbk_topic.write(message_from_data(update_part, DatacampOffer()).SerializeToString())
    wait_until(lambda: piper.united_offers_processed >= offers_processed + 1)

    # шаг 3 - проверяем, что изменения появились в хранилище, а старый тип исчез
    assert_that(piper.service_offers_table.data,
                HasOffers([message_from_data({
                    'identifiers': {
                        'shop_id': shop_id,
                        'offer_id': offer_id,
                        'business_id': 1,
                    },
                    'status': {
                        'disabled': [
                            {
                                'flag': True,
                                'meta': {
                                    'source': PUSH_PARTNER_OFFICE,
                                    'timestamp': new_time
                                }
                            }
                        ],
                    }
                }, DatacampOffer())]),
                'Disabled flag from push partner was updated by new ts')

    assert_that(piper.service_offers_table.data,
                is_not(
                    HasOffers([message_from_data({
                        'identifiers': {
                            'shop_id': shop_id,
                            'offer_id': offer_id,
                            'business_id': 1,
                        },
                        'status': {
                            'disabled': [
                                {
                                    'flag': True,
                                    'meta': {
                                        'source': PUSH_PARTNER_API,
                                        'timestamp': current_time,
                                    }
                                }
                            ],
                        }
                    }, DatacampOffer())])),
                'Disabled flag from push partner was updated by source')

    # шаг 4 - отправляем изменения - флажок со старым ts и типом PUSH_PARTNER_FEED
    old_time = (NOW_UTC - timedelta(minutes=45)).strftime(time_pattern)
    update_part = {
        'identifiers': {
            'shop_id': shop_id,
            'offer_id': offer_id,
            'warehouse_id': warehouse_id,
            'business_id': 1,
        },
        'status': {
            'disabled': [
                {
                    'flag': True,
                    'meta': {
                        'source': PUSH_PARTNER_FEED,
                        'timestamp': old_time,
                    }
                }
            ],
        }
    }

    lbk_topic.write(message_from_data(update_part, DatacampOffer()).SerializeToString())
    wait_until(lambda: piper.united_offers_processed >= offers_processed + 2)

    # шаг 5 - проверяем, что в хранилище нет изменений (старый флажок на места, а новый не появился)
    assert_that(piper.service_offers_table.data,
                HasOffers([message_from_data({
                    'identifiers': {
                        'shop_id': shop_id,
                        'offer_id': offer_id,
                        'business_id': 1,
                    },
                    'status': {
                        'disabled': [
                            {
                                'flag': True,
                                'meta': {
                                    'source': PUSH_PARTNER_OFFICE,
                                    'timestamp': new_time,
                                }
                            }
                        ],
                    }
                }, DatacampOffer())]),
                'Disabled flag from push partner was not updated by old ts')

    assert_that(piper.service_offers_table.data,
                is_not(
                    HasOffers([message_from_data({
                        'identifiers': {
                            'shop_id': shop_id,
                            'offer_id': offer_id,
                            'business_id': 1,
                        },
                        'status': {
                            'disabled': [
                                {
                                    'flag': True,
                                    'meta': {
                                        'source': PUSH_PARTNER_FEED,
                                        'timestamp': old_time
                                    }
                                }
                            ],
                        }
                    }, DatacampOffer())])),
                'Disabled flag from push partner was not updated by source with old ts')


def test_offer_partly_update_disable_flags(inserter, piper, lbk_topic):
    """Проверяет, что оффер обновляется частями: флаги disabled"""
    shop_id = 1
    offer_id = 'TestDisableFlagsUpdate01'
    warehouse_id = 42
    # шаг 1 : проверяем, что добавляется новый флаг
    current_time = NOW_UTC.strftime(time_pattern)
    update_part = {
        'identifiers': {
            'shop_id': shop_id,
            'offer_id': offer_id,
            'warehouse_id': warehouse_id,
            'business_id': 1,
        },
        'status': {
            'disabled': [
                {
                    'flag': True,
                    'meta': {
                        'source': MARKET_STOCK,
                        'timestamp': current_time,
                    }
                }
            ],
        }
    }

    offers_processed = piper.united_offers_processed
    lbk_topic.write(message_from_data(update_part, DatacampOffer()).SerializeToString())
    wait_until(lambda: piper.united_offers_processed >= offers_processed + 1)
    assert_that(piper.actual_service_offers_table.data,
                HasOffers([message_from_data({
                    'identifiers': {
                        'shop_id': shop_id,
                        'offer_id': offer_id,
                        'warehouse_id': warehouse_id,
                        'business_id': 1,
                    },
                    'status': {
                        'publish': HIDDEN,
                        'disabled': [
                            {
                                'flag': True,
                                'meta': {
                                    'timestamp': current_time,
                                    'source': MARKET_STOCK,
                                },
                            }]
                    }
                }, DatacampOffer())]),
                'Disabled flag is incorrect')

    # шаг 2 : проверяем, что добавляется флаг с более старым таймстемпом не назначается
    old_time = (NOW_UTC - timedelta(minutes=15)).strftime(time_pattern)
    update_part = {
        'identifiers': {
            'shop_id': shop_id,
            'offer_id': offer_id,
            'warehouse_id': warehouse_id,
            'business_id': 1,
        },
        'status': {
            'disabled': [
                {
                    'flag': True,
                    'meta': {
                        'source': MARKET_STOCK,
                        'timestamp': old_time,
                    }
                }
            ],
        }
    }

    lbk_topic.write(message_from_data(update_part, DatacampOffer()).SerializeToString())
    wait_until(lambda: piper.united_offers_processed >= offers_processed + 2)
    assert_that(piper.actual_service_offers_table.data,
                HasOffers([message_from_data({
                    'identifiers': {
                        'shop_id': shop_id,
                        'offer_id': offer_id,
                        'warehouse_id': warehouse_id,
                        'business_id': 1,
                    },
                    'status': {
                        'publish': HIDDEN,
                        'disabled': [
                            {
                                'flag': True,
                                'meta': {
                                    'timestamp': current_time,
                                    'source': MARKET_STOCK,
                                },
                            }]
                    }
                }, DatacampOffer())]),
                'Disabled flag is incorrect')

    # шаг 3 : проверяем, что добавляется флаг с другим сорсом, но старым таймстемпом устанавливается
    update_part = {
        'identifiers': {
            'shop_id': shop_id,
            'offer_id': offer_id,
            'warehouse_id': warehouse_id,
            'business_id': 1,
        },
        'status': {
            'disabled': [
                {
                    'flag': True,
                    'meta': {
                        'source': PUSH_PARTNER_FEED,
                        'timestamp': old_time,
                    }
                }
            ],
        }
    }
    lbk_topic.write(message_from_data(update_part, DatacampOffer()).SerializeToString())
    wait_until(lambda: piper.united_offers_processed >= offers_processed + 3)
    assert_that(piper.service_offers_table.data,
                HasOffers([message_from_data({
                    'identifiers': {
                        'shop_id': shop_id,
                        'offer_id': offer_id,
                        'business_id': 1,
                    },
                    'status': {
                        'publish_by_partner': HIDDEN,
                        'disabled': [
                            {
                                'flag': True,
                                'meta': {
                                    'timestamp': old_time,
                                    'source': PUSH_PARTNER_FEED,
                                },
                            }
                        ]
                    }
                }, DatacampOffer())]),
                'Disabled flag is incorrect')

    # шаг 4 : проверяем, что если все disabled флаги сняты, то статус фида изменится на available
    enable_time = (NOW_UTC + timedelta(minutes=45)).strftime(time_pattern)
    update_part = {
        'identifiers': {
            'shop_id': shop_id,
            'offer_id': offer_id,
            'warehouse_id': warehouse_id,
            'business_id': 1,
        },
        'status': {
            'disabled': [
                {
                    'flag': False,
                    'meta': {
                        'source': PUSH_PARTNER_FEED,
                        'timestamp': enable_time,
                    }
                },
                {
                    'flag': False,
                    'meta': {
                        'source': MARKET_STOCK,
                        'timestamp': enable_time,
                    }
                },
                {
                    'flag': False,  # появился при создании оффера, тоже убираем
                    'meta': {
                        'source': MARKET_IDX,
                        'timestamp': enable_time,
                    }
                }
            ],
        }
    }

    lbk_topic.write(message_from_data(update_part, DatacampOffer()).SerializeToString())
    wait_until(lambda: piper.united_offers_processed >= offers_processed + 4)
    assert_that(piper.service_offers_table.data,
                HasOffers([message_from_data({
                    'identifiers': {
                        'shop_id': shop_id,
                        'offer_id': offer_id,
                        'business_id': 1,
                    },
                    'status': {
                        'publish_by_partner': AVAILABLE,
                        'disabled': [
                            {
                                'flag': False,
                                'meta': {
                                    'timestamp': enable_time,
                                    'source': PUSH_PARTNER_FEED,
                                },
                            },
                        ]
                    }
                }, DatacampOffer())]),
                'Disabled flag is incorrect')

    assert_that(piper.actual_service_offers_table.data,
                HasOffers([message_from_data({
                    'identifiers': {
                        'shop_id': shop_id,
                        'offer_id': offer_id,
                        'warehouse_id': warehouse_id,
                        'business_id': 1,
                    },
                    'status': {
                        'publish': AVAILABLE,  # высчитывается в AfterUpdate
                        'ready_for_publication': {
                            'value': ReadinessForPublicationStatus.READY,  # высчитывается в AfterUpdate
                        },
                        'has_gone': {
                            'flag': False,
                            'meta': {
                                'source': MARKET_IDX,
                            },
                        },
                        'disabled': [
                            {
                                'flag': False,
                                'meta': {
                                    'timestamp': enable_time,
                                    'source': MARKET_STOCK,
                                },
                            },
                            {
                                'flag': False,
                                'meta': {
                                    'timestamp': enable_time,
                                    'source': MARKET_IDX,
                                },
                            }
                        ]
                    }
                }, DatacampOffer())]),
                'Disabled flag is incorrect')
