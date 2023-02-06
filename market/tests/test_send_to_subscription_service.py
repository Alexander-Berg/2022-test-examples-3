# coding: utf-8

from hamcrest import assert_that, has_items, not_none, empty, is_not
import pytest
import time

from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
import market.idx.datacamp.proto.subscription.subscription_pb2 as SUBS
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock

from market.idx.yatf.matchers.lb_matchers import HasNoUnreadData
from market.idx.yatf.matchers.yt_rows_matchers import HasOffers
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf, IsProtobufMap
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.datacamp.yatf.utils import create_update_meta_dict
from market.pylibrary.proto_utils import proto_path_from_str_path, message_from_data


BUSINESS_ID = 1
SHOP_ID = 1
OFFER_ID = 'T1000'


@pytest.fixture(scope='module')
def united_offers_table_data():
    return [{
        'basic': {
            'identifiers': {'business_id': BUSINESS_ID, 'offer_id': OFFER_ID},
            'pictures': {
                'partner': {
                    'original': {
                        'meta': create_update_meta_dict(0),
                        'source': [
                            {'url': '2000url1/'},
                            {'url': 'https://2000url2/'},
                            {'url': '2000url3/'},
                            {'url': 'https://2000url4/'},
                        ]
                    }
                }
            }
        },
        'service': {
            SHOP_ID: {
                'identifiers': {'business_id': BUSINESS_ID, 'offer_id': OFFER_ID, 'shop_id': SHOP_ID}
            }
        },
        'actual': {
            SHOP_ID: {
                'warehouse': {
                    0: {
                        'identifiers': {'business_id': BUSINESS_ID, 'offer_id': OFFER_ID, 'shop_id': SHOP_ID, 'warehouse_id': 0}
                    }
                }
            }
        }
    }, {
        "basic": {
            "identifiers": {
                "business_id": BUSINESS_ID,
                "offer_id": "only_integral_status_changes",
            },
            "meta": {
                "scope": DTC.BASIC
            },
            "content": {
                "binding": {
                    "approved": {
                        "market_sku_id": 101670964243,
                    }
                },
                "master_data": {
                    "version": {
                        "value": {
                            "counter": 1769790921991488800
                        }
                    }
                },
                "status": {
                    "content_system_status": {
                        "status_content_version": {
                            "counter": 1769582474343533800
                        }
                    }
                }
            },
            "status": {
                "consistency": {
                    "mboc_consistency": False,
                    "pricelabs_consistency": False
                },
                "version": {
                    "actual_content_version": {
                        "counter": 1769582474343533800
                    },
                    "master_data_version": {
                        "counter": 1769634527199693300
                    },
                    "offer_version": {
                        "counter": 1770046660450269000
                    },
                    "original_partner_data_version": {
                        "counter": 1769443713613973200
                    },
                    "uc_data_version": {
                        "counter": 1769443713613973200
                    }
                }
            }
        },
        "service": {
            SHOP_ID: {
                "identifiers": {
                    "business_id": BUSINESS_ID,
                    "offer_id": "only_integral_status_changes",
                    "real_feed_id": 4259192,
                    "shop_id": SHOP_ID
                },
                "meta": {
                    "data_source": DTC.PUSH_PARTNER_FEED,
                    "rgb": DTC.BLUE,
                    "scope": DTC.SERVICE
                },
                "content": {
                    "partner": {
                        "original_terms": {
                            "supply_plan": {
                                "value": DTC.SupplyPlan.WILL_SUPPLY
                            }
                        }
                    },
                    "status": {
                        "content_system_status": {
                            "service_offer_state": DTC.CONTENT_STATE_NEED_CONTENT
                        }
                    }
                },
                "price": {
                    "basic": {
                        "binary_oldprice": {
                            "price": 29990000000
                        },
                        "binary_price": {
                            "price": 13500000000
                        },
                        "vat": 6
                    },
                    "enable_auto_discounts": {
                        "flag": False,
                    },
                },
                "partner_info": {
                    "has_warehousing": True,
                    "is_disabled": False,
                    "is_ignore_stocks": False,
                    "is_preproduction": True,
                    "program_type": 3,
                },
                "status": {
                    "disabled": [
                        {
                            "flag": False,
                            "meta": {
                                "source": DTC.PUSH_PARTNER_FEED,
                            }
                        },
                        {
                            "flag": False,
                            "meta": {
                                "source": DTC.MARKET_MBO,
                            }
                        }
                    ],
                    "publish_by_partner": DTC.AVAILABLE,
                    "version": {
                        "actual_content_version": {
                            "counter": 1769443713613973200
                        },
                        "master_data_version": {
                            "counter": 1769443713613973200
                        },
                        "offer_version": {
                            "counter": 1770034827815435300
                        },
                        "original_partner_data_version": {
                            "counter": 1769443714687629800
                        },
                        "uc_data_version": {
                            "counter": 1769443713613973200
                        }
                    }
                }
            }
        },
        "actual": {
            SHOP_ID: {
                "warehouse": {
                    145: {
                        "identifiers": {
                            "business_id": BUSINESS_ID,
                            "shop_id": SHOP_ID,
                            "warehouse_id": 145,
                            "offer_id": "only_integral_status_changes",
                            "feed_id": 4259192,
                        },
                        "meta": {
                            "rgb": DTC.BLUE,
                            "scope": DTC.SERVICE
                        },
                        'price': {
                            "enable_auto_discounts": {
                                "flag": True,
                            }
                        },
                        "status": {
                            "disabled": [
                                {
                                    "flag": False,
                                    "meta": {
                                        "source": DTC.MARKET_IDX,
                                    }
                                },
                                {
                                    "flag": False,
                                    "meta": {
                                        "source": DTC.MARKET_STOCK,
                                    }
                                }
                            ],
                            "has_gone": {
                                "flag": False,
                                "meta": {
                                    "source": DTC.MARKET_IDX,
                                }
                            },
                            "publish": DTC.AVAILABLE,
                            "ready_for_publication": {
                                "meta": {
                                    "source": DTC.MARKET_IDX,
                                },
                                "value": DTC.ReadinessForPublicationStatus.READY
                            },
                            "version": {
                                "actual_content_version": {
                                    "counter": 1769790921991488800
                                },
                                "master_data_version": {
                                    "counter": 1769790921991488800
                                },
                                "offer_version": {
                                    "counter": 1769790921991488800
                                },
                                "uc_data_version": {
                                    "counter": 1769790921991488800
                                }
                            }
                        },
                        "stock_info": {
                            "market_stocks": {
                                "count": 10,
                                "meta": {
                                    "source": DTC.MARKET_STOCK,
                                }
                            }
                        },
                        "tech_info": {
                            "last_mining": {
                                "original_partner_data_version": {
                                    "counter": 1769443714687629800
                                }
                            }
                        }
                    }
                }
            }
        },
    }, {
        'basic': {
            'identifiers': {'business_id': BUSINESS_ID, 'offer_id': 'offer.with.update.from.mboc'},
            'content': {
                'binding': {
                    'approved': {
                        'market_sku_id': 123
                    }
                }
            },
            'status': {
                'version': {
                    'offer_version': {
                        'counter': 100
                    },
                    'master_data_version': {
                        'counter': 100
                    }
                }
            }
        },
        'service': {
            SHOP_ID: {
                'identifiers': {'business_id': BUSINESS_ID, 'offer_id': 'offer.with.update.from.mboc', 'shop_id': SHOP_ID},
                'meta': {
                    'rgb': DTC.BLUE,
                    'scope': DTC.SERVICE,
                },
                'status': {
                    'version': {
                        'offer_version': {
                            'counter': 100
                        },
                        'master_data_version': {
                            'counter': 100
                        }
                    }
                }
            }
        },
    }]


UPDATED_OFFER_MESSAGES = [
    message_from_data({
        'united_offers': [{'offer': [
            {
                'basic': {
                    'identifiers': {'business_id': BUSINESS_ID, 'offer_id': OFFER_ID},
                    'pictures': {
                        'partner': {
                            'original': {
                                'meta': create_update_meta_dict(int(time.time())),
                                'source': [
                                    {'url': 'https://2000url2/'},
                                    {'url': 'https://newsourceurl/'},
                                    {'url': 'new2url/'},
                                    {'url': '2000url3/'},
                                ]
                            }
                        }
                    }
                },
                'service': {
                    SHOP_ID: {
                        'identifiers': {'business_id': BUSINESS_ID, 'offer_id': OFFER_ID, 'shop_id': SHOP_ID},
                        'price': {
                            'basic': {
                                'binary_price': {
                                    'id': 'RUR',
                                    'price': 100
                                },
                                'meta': create_update_meta_dict(int(time.time()))
                            }
                        }
                    }
                }
            },
        ]}]
    }, DatacampMessage()),
    message_from_data({
        'united_offers': [{'offer': [
            {
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': 'only_integral_status_changes'
                    },
                },
                'service': {
                    SHOP_ID: {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': 'only_integral_status_changes',
                            'shop_id': SHOP_ID,
                            'warehouse_id': 145
                        },
                        "partner_info": {
                            "has_warehousing": True,
                            "is_disabled": False,
                            "is_ignore_stocks": False,
                            "is_preproduction": False,
                            "program_type": 3,
                            "meta": create_update_meta_dict(int(time.time()))
                        }
                    }
                }
            }
        ]}]
    }, DatacampMessage()),
    message_from_data({
        'united_offers': [
            {
                'offer': [
                    {
                        'basic': {
                            'identifiers': {
                                'business_id': BUSINESS_ID,
                                'offer_id': 'offer.with.update.from.mboc',
                            },
                            'content': {
                                'binding': {
                                    'approved': {
                                        'market_sku_id': 321,
                                        'meta': create_update_meta_dict(int(time.time()))
                                    },
                                }
                            }
                        }
                    }
                ]
            }
        ]
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
def config(yt_server, log_broker_stuff, datacamp_messages_topic, subscription_service_topic):
    cfg = {
        'general': {
            'color': 'white',
            'batch_size': 10,
        },
        'logbroker': {
            'datacamp_messages_topic': datacamp_messages_topic.topic,
            'subscription_service_topic': subscription_service_topic.topic,
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
):
    resources = {
        'config': config,
        'datacamp_messages_topic': datacamp_messages_topic,
        'actual_service_offers_table': actual_service_offers_table,
        'service_offers_table': service_offers_table,
        'basic_offers_table': basic_offers_table,
        'subscription_service_topic': subscription_service_topic,
    }
    with PiperTestEnv(yt_server, log_broker_stuff, **resources) as piper_env:
        piper_env.verify()
        yield piper_env


@pytest.yield_fixture(scope='module')
def output_data(piper, datacamp_messages_topic, subscription_service_topic):
    for message in UPDATED_OFFER_MESSAGES:
        datacamp_messages_topic.write(message.SerializeToString())

    wait_until(lambda: piper.united_offers_processed >= len(UPDATED_OFFER_MESSAGES), timeout=60)
    data = subscription_service_topic.read(3)
    assert_that(subscription_service_topic, HasNoUnreadData())
    return data


def test_subscription_message(output_data):
    assert_that(
        output_data,
        has_items(
            IsSerializedProtobuf(
                SUBS.UnitedOffersSubscriptionMessage,
                {
                    'subscription_request': [
                        {
                            'business_id': BUSINESS_ID,
                            'offer_id': OFFER_ID,
                            'basic_changed_for_integral_status': True,
                            'basic': {
                                'updated_fields': [
                                    proto_path_from_str_path(DTC.Offer, 'pictures.partner.original'),
                                    proto_path_from_str_path(DTC.Offer, 'status.consistency.pricelabs_consistency'),
                                    proto_path_from_str_path(DTC.Offer, 'status.version.actual_content_version'),
                                    proto_path_from_str_path(DTC.Offer, 'status.consistency.mboc_consistency'),
                                    proto_path_from_str_path(DTC.Offer, 'status.bannerland_picture_version'),
                                ],
                                'new_picture_urls': [
                                    'new2url/',
                                    'https://newsourceurl/',
                                ],
                                'warehouse_price_updated': False,
                            },
                            'service': IsProtobufMap(
                                {
                                    SHOP_ID: {
                                        'updated_fields': {
                                            'updated_fields': [
                                                proto_path_from_str_path(DTC.Offer, 'price.basic'),
                                                proto_path_from_str_path(DTC.Offer, 'price'),
                                                proto_path_from_str_path(DTC.Offer, 'status.version.actual_content_version'),
                                                proto_path_from_str_path(DTC.Offer, 'status.has_gone'),
                                                proto_path_from_str_path(DTC.Offer, 'status.publish_by_partner'),
                                            ],
                                            'warehouse_price_updated': False,
                                            'updated_existence_fields': [proto_path_from_str_path(DTC.Offer, 'price.basic')],
                                        },
                                        'warehouse': IsProtobufMap(
                                            {
                                                0: {
                                                    'warehouse_price_updated': False,
                                                    'updated_fields': [
                                                        proto_path_from_str_path(DTC.Offer, 'status.has_gone'),
                                                        proto_path_from_str_path(DTC.Offer, 'status.publish'),
                                                    ],
                                                }
                                            }
                                        ),
                                        'status_update': {
                                            'old_status': DTC.OfferStatus.ResultStatus.NOT_PUBLISHED_CHECKING,
                                            'new_status': DTC.OfferStatus.ResultStatus.NOT_PUBLISHED_CHECKING,
                                        },
                                    }
                                }
                            ),
                        }
                    ],
                    'source': not_none(),
                },
            ),
            IsSerializedProtobuf(
                SUBS.UnitedOffersSubscriptionMessage,
                {
                    'subscription_request': [
                        {
                            'business_id': BUSINESS_ID,
                            'offer_id': 'only_integral_status_changes',
                            'basic_changed_for_integral_status': False,
                            'basic': {
                                'updated_fields': empty(),
                                'warehouse_price_updated': False,
                            },
                            'service': IsProtobufMap(
                                {
                                    SHOP_ID: {
                                        'updated_fields': {
                                            'updated_fields': empty(),
                                            'warehouse_price_updated': False,
                                        },
                                        'warehouse': IsProtobufMap(
                                            {
                                                145: {
                                                    'updated_fields': empty(),
                                                    'warehouse_price_updated': False,
                                                }
                                            }
                                        ),
                                        'status_update': {
                                            'old_status': DTC.OfferStatus.ResultStatus.PUBLISHED,
                                            'new_status': DTC.OfferStatus.ResultStatus.NOT_PUBLISHED_CHECKING,
                                        },
                                    }
                                }
                            ),
                        }
                    ],
                    'source': not_none(),
                },
            ),
        ),
    )


def test_new_field_trigger_mask(piper, output_data):
    """ При добавлении нового поля оно попадает в маску
    """
    assert_that(piper.basic_offers_table.data, is_not(HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': BUSINESS_ID,
                'offer_id': 'offer.with.update.from.mboc',
            },
            'status': {
                'version': {
                    'master_data_version': {
                        'counter': 100
                    }
                }
            },
        }, DTC.Offer())
    ])))
    assert_that(piper.basic_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': BUSINESS_ID,
                'offer_id': 'offer.with.update.from.mboc',
            },
        }, DTC.Offer())
    ]))
    assert_that(
        output_data,
        has_items(
            IsSerializedProtobuf(
                SUBS.UnitedOffersSubscriptionMessage,
                {
                    'subscription_request': [
                        {
                            'business_id': BUSINESS_ID,
                            'offer_id': 'offer.with.update.from.mboc',
                            'basic': {
                                'updated_fields': [
                                    proto_path_from_str_path(
                                        DTC.Offer,
                                        'content.binding.approved',
                                    ),
                                ],
                            },
                        }
                    ],
                },
            ),
        )
    )
