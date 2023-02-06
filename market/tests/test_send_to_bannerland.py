# coding: utf-8

import pytest
from datetime import datetime

import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
import market.idx.datacamp.proto.offer.OfferPictures_pb2 as OfferPictures
import market.idx.datacamp.proto.tables.Partner_pb2 as Partner
from hamcrest import assert_that, has_items
from market.idx.datacamp.proto.api.ExportMessage_pb2 import ExportMessage
from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock
from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.datacamp.yatf.utils import dict2tskv
from market.pylibrary.proto_utils import message_from_data
from market.idx.yatf.matchers.lb_matchers import HasNoUnreadData
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf, IsProtobufMap
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampPartnersTable


NOW_UTC = datetime.utcnow()
BUISNESS_ID = 1
SHOP_ID = 1
WAREHOUSE_ID = 1
OFFER_ID = 'o1'
OFFER_ID_2 = 'o2'
OFFER_ID_3 = 'o3'
OFFER_ID_4 = 'o4'
OFFER_ID_5 = 'o5'
SERVICE_PRICE = 20
CATEGORY_ID = 1
CATEGORY_NAME = 'cat_name'
DIRECT_CATEGORY_ID = 2
DIRECT_CATEGORY_NAME = 'important_product_name'
OGRN = 'sup_ogrn'
SUPPLIER_NAME = 'sup'
CURRENT_TIME = NOW_UTC.strftime("%Y-%m-%dT%H:%M:%SZ")
CURRENT_TIMESTAMP = int((NOW_UTC - datetime(1970, 1, 1)).total_seconds())
UC_DATA_VERSION = 1
PICTURE_ORIGINAL_URL = 'http://example.com'
SECOND_PICTURE_ORIGINAL_URL = 'http://example.com/second'
NS_YABS_PERFORMANCE = 'yabs_performance'

VALID_UNITED_OFFER = [
    {
        'united_offers': [
            {
                'offer': [
                    {
                        'basic': {
                            'identifiers': {
                                'business_id': BUISNESS_ID,
                                'offer_id': OFFER_ID,
                            },
                            'meta': {
                                'ts_created': CURRENT_TIME,
                            },
                            'status': {
                                'version': {'uc_data_version': {'counter': UC_DATA_VERSION}},
                            },
                            'content': {
                                'market': {
                                    'real_uc_version': {'counter': UC_DATA_VERSION},
                                },
                                'partner': {
                                    'original': {
                                        'category': {'id': CATEGORY_ID, 'name': CATEGORY_NAME},
                                        'name': {'value': 'partner name'},
                                        'original_name': {'value': 'original name'}
                                    }
                                },
                            },
                            'pictures': {
                                'partner': {
                                    'multi_actual': {
                                        PICTURE_ORIGINAL_URL: {
                                            'by_namespace': {
                                                NS_YABS_PERFORMANCE: {
                                                    'status': OfferPictures.MarketPicture.AVAILABLE
                                                }
                                            }
                                        }
                                    },
                                    'original': {
                                        'source': [{
                                            'url': PICTURE_ORIGINAL_URL
                                        }]
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
                                    'rgb': DTC.DIRECT_STANDBY
                                },
                                'price': {
                                    'basic': {
                                        'binary_price': {'price': SERVICE_PRICE},
                                    }
                                },
                                'content': {
                                    'partner': {
                                        'original': {
                                            'supplier_info': {'ogrn': OGRN, 'name': SUPPLIER_NAME},
                                            'url': {
                                                'value': 'http://original_host1.ru/offer_1',
                                                'meta': {'timestamp': CURRENT_TIME}
                                            },
                                            'direct_category': {'id': DIRECT_CATEGORY_ID, 'name': DIRECT_CATEGORY_NAME},
                                        }
                                    }
                                },
                                'status': {
                                    'preview': {
                                        'flag': True
                                    }
                                }
                            }
                        },
                    }
                ]
            }
        ]
    }
]

ONLY_SECOND_PICTURE_UNITED_OFFER = [
    {
        'united_offers': [
            {
                'offer': [
                    {
                        'basic': {
                            'identifiers': {
                                'business_id': BUISNESS_ID,
                                'offer_id': OFFER_ID_5,
                            },
                            'meta': {
                                'ts_created': CURRENT_TIME,
                            },
                            'status': {
                                'version': {'uc_data_version': {'counter': UC_DATA_VERSION}},
                            },
                            'content': {
                                'market': {
                                    'real_uc_version': {'counter': UC_DATA_VERSION},
                                },
                                'partner': {
                                    'original': {
                                        'category': {'id': CATEGORY_ID, 'name': CATEGORY_NAME},
                                    }
                                },
                            },
                            'pictures': {
                                'partner': {
                                    'multi_actual': {
                                        SECOND_PICTURE_ORIGINAL_URL: {
                                            'by_namespace': {
                                                NS_YABS_PERFORMANCE: {
                                                    'status': OfferPictures.MarketPicture.AVAILABLE
                                                }
                                            }
                                        }
                                    },
                                    'original': {
                                        'source': [{
                                            'url': PICTURE_ORIGINAL_URL
                                        },
                                        {
                                            'url': SECOND_PICTURE_ORIGINAL_URL
                                        }]
                                    }
                                }
                            }
                        },
                        'service': {
                            1: {
                                'identifiers': {
                                    'business_id': BUISNESS_ID,
                                    'offer_id': OFFER_ID_5,
                                    'shop_id': SHOP_ID,
                                    'warehouse_id': WAREHOUSE_ID,
                                },
                                'meta': {
                                    'ts_created': CURRENT_TIME,
                                    'rgb': DTC.DIRECT_STANDBY
                                },
                                'price': {
                                    'basic': {
                                        'binary_price': {'price': SERVICE_PRICE},
                                    }
                                },
                                'content': {
                                    'partner': {
                                        'original': {
                                            'url': {
                                                'value': 'http://original_host1.ru/offer_1',
                                                'meta': {'timestamp': CURRENT_TIME}
                                            },
                                            'direct_category': {'id': DIRECT_CATEGORY_ID, 'name': DIRECT_CATEGORY_NAME},
                                        },
                                    }
                                },
                                'status': {
                                    'preview': {
                                        'flag': True
                                    }
                                }
                            }
                        },
                    }
                ]
            }
        ]
    }
]

NO_PREVIEW_UNITED_OFFER = [
    {
        'united_offers': [
            {
                'offer': [
                    {
                        'basic': {
                            'identifiers': {
                                'business_id': BUISNESS_ID,
                                'offer_id': OFFER_ID_3,
                            },
                            'meta': {
                                'ts_created': CURRENT_TIME,
                            },
                            'status': {
                                'version': {'uc_data_version': {'counter': UC_DATA_VERSION}},
                            },
                            'content': {
                                'market': {
                                    'real_uc_version': {'counter': UC_DATA_VERSION},
                                },
                                'partner': {
                                    'original': {
                                        'category': {'id': CATEGORY_ID, 'name': CATEGORY_NAME},
                                    }
                                },
                            },
                            'pictures': {
                                'partner': {
                                    'multi_actual': {
                                        PICTURE_ORIGINAL_URL: {
                                            'by_namespace': {
                                                NS_YABS_PERFORMANCE: {
                                                    'status': OfferPictures.MarketPicture.AVAILABLE
                                                }
                                            }
                                        }
                                    },
                                    'original': {
                                        'source': [{
                                            'url': PICTURE_ORIGINAL_URL
                                        }]
                                    }
                                }
                            }
                        },
                        'service': {
                            1: {
                                'identifiers': {
                                    'business_id': BUISNESS_ID,
                                    'offer_id': OFFER_ID_3,
                                    'shop_id': SHOP_ID,
                                    'warehouse_id': WAREHOUSE_ID,
                                },
                                'meta': {
                                    'ts_created': CURRENT_TIME,
                                    'rgb': DTC.DIRECT_STANDBY
                                },
                                'price': {
                                    'basic': {
                                        'binary_price': {'price': SERVICE_PRICE},
                                    }
                                },
                                'content': {
                                    'partner': {
                                        'original': {
                                            'supplier_info': {'ogrn': OGRN, 'name': SUPPLIER_NAME},
                                            'url': {
                                                'value': 'http://original_host2.ru/offer_3',
                                                'meta': {'timestamp': CURRENT_TIME}
                                            },
                                            'direct_category': {'id': DIRECT_CATEGORY_ID, 'name': DIRECT_CATEGORY_NAME},
                                        },
                                    }
                                }
                            }
                        },
                    }
                ]
            }
        ]
    }
]

NO_PICTURE_UNITED_OFFER = [
    {
        'united_offers': [
            {
                'offer': [
                    {
                        'basic': {
                            'identifiers': {
                                'business_id': BUISNESS_ID,
                                'offer_id': OFFER_ID_4,
                            },
                            'meta': {
                                'ts_created': CURRENT_TIME,
                            },
                            'status': {
                                'version': {'uc_data_version': {'counter': UC_DATA_VERSION}},
                            },
                            'content': {
                                'market': {
                                    'real_uc_version': {'counter': UC_DATA_VERSION},
                                },
                                'partner': {
                                    'original': {
                                        'category': {'id': CATEGORY_ID, 'name': CATEGORY_NAME},
                                    }
                                },
                            }
                        },
                        'service': {
                            1: {
                                'identifiers': {
                                    'business_id': BUISNESS_ID,
                                    'offer_id': OFFER_ID_4,
                                    'shop_id': SHOP_ID,
                                    'warehouse_id': WAREHOUSE_ID,
                                },
                                'meta': {
                                    'ts_created': CURRENT_TIME,
                                    'rgb': DTC.DIRECT_STANDBY
                                },
                                'price': {
                                    'basic': {
                                        'binary_price': {'price': SERVICE_PRICE},
                                    }
                                },
                                'content': {
                                    'partner': {
                                        'original': {
                                            'supplier_info': {'ogrn': OGRN, 'name': SUPPLIER_NAME},
                                            'url': {
                                                'value': 'http://original_host1.ru/offer_4',
                                                'meta': {'timestamp': CURRENT_TIME}
                                            },
                                            'direct_category': {'id': DIRECT_CATEGORY_ID, 'name': DIRECT_CATEGORY_NAME},
                                        }
                                    }
                                },
                                'delivery': {
                                    'partner': {
                                        'original': {
                                            'available': {
                                                'flag': True
                                            }
                                        }
                                    }
                                },
                                'resolution': {
                                    'direct': {
                                        'bigmod_verdict': {
                                            'Flags': [
                                                10013
                                            ],
                                            'Verdict': 0
                                        }
                                    }
                                },
                                'status': {
                                    'preview': {
                                        'flag': True
                                    }
                                }
                            }
                        },
                    }
                ]
            }
        ]
    }
]

NO_PREVIEW_NO_PICTURE_UNITED_OFFER = [
    {
        'united_offers': [
            {
                'offer': [
                    {
                        'basic': {
                            'identifiers': {'business_id': BUISNESS_ID, 'offer_id': OFFER_ID_2},
                            'meta': {'ts_created': CURRENT_TIME},
                            'status': {'version': {'uc_data_version': {'counter': UC_DATA_VERSION}}},
                            'content': {
                                'market': {
                                    'real_uc_version': {'counter': UC_DATA_VERSION},
                                },
                                'partner': {
                                    'original': {
                                        'category': {'id': CATEGORY_ID, 'name': CATEGORY_NAME},
                                    }
                                },
                            },
                        },
                        'service': {
                            1: {
                                'identifiers': {
                                    'business_id': BUISNESS_ID,
                                    'offer_id': OFFER_ID_2,
                                    'shop_id': SHOP_ID,
                                    'warehouse_id': WAREHOUSE_ID,
                                },
                                'meta': {'ts_created': CURRENT_TIME, 'rgb': DTC.WHITE},
                                'price': {'basic': {'binary_price': {'price': SERVICE_PRICE}}},
                                'content': {
                                    'partner': {
                                        'original': {
                                            'supplier_info': {'ogrn': OGRN, 'name': SUPPLIER_NAME},
                                            'url': {
                                                'value': 'http://original_host1.ru/offer_2',
                                                'meta': {'timestamp': CURRENT_TIME}
                                            },
                                            'direct_category': {'id': DIRECT_CATEGORY_ID, 'name': DIRECT_CATEGORY_NAME},
                                        }
                                    }
                                },
                            }
                        },
                    }
                ]
            }
        ]
    }
]


SHOPS = [
    {
        'shop_id': SHOP_ID,
        'mbi': '\n\n'.join([dict2tskv({'shop_id': 2, 'datafeed_id': 1})]),  # when empty, supplier won't load the record into cache
        'resolved_redirect_info': Partner.ResolvedRedirectInfo(
            items=[
                Partner.ResolvedRedirectInfoItem(original_host='http://original_host1.ru', target_host='http://target_host1.ru'),
                Partner.ResolvedRedirectInfoItem(original_host='http://original_host2.ru', target_host='http://target_host2.ru'),
            ]
        ).SerializeToString()
    }
]


@pytest.fixture(scope='module')
def datacamp_messages():
    return VALID_UNITED_OFFER + ONLY_SECOND_PICTURE_UNITED_OFFER + NO_PREVIEW_NO_PICTURE_UNITED_OFFER + NO_PREVIEW_UNITED_OFFER + NO_PICTURE_UNITED_OFFER


@pytest.fixture(scope='module')
def datacamp_messages_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def bannerland_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff, 'bannerland_topic')
    return topic


@pytest.fixture(scope='module')
def bannerland_preview_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff, 'bannerland_preview_topic')
    return topic


@pytest.fixture(scope='module')
def partners_table(yt_server, config):
    return DataCampPartnersTable(
        yt_server,
        config.yt_partners_tablepath,
        data=SHOPS,
    )


@pytest.fixture(scope='module')
def config(yt_server, log_broker_stuff, datacamp_messages_topic, bannerland_topic, bannerland_preview_topic):
    cfg = {
        'logbroker': {
            'datacamp_messages_topic': datacamp_messages_topic.topic,
            'bannerland_topic': bannerland_topic.topic,
            'bannerland_preview_topic': bannerland_preview_topic.topic,
        },
    }
    return PiperConfigMock(yt_server=yt_server, log_broker_stuff=log_broker_stuff, config=cfg)


@pytest.yield_fixture(scope='module')
def piper(yt_server, log_broker_stuff, config, datacamp_messages_topic, bannerland_topic, bannerland_preview_topic, partners_table):
    resources = {
        'config': config,
        'datacamp_messages_topic': datacamp_messages_topic,
        'bannerland_topic': bannerland_topic,
        'bannerland_preview_topic': bannerland_preview_topic,
        'partners_table': partners_table,
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


def test_send_to_bannerland(inserter, bannerland_topic):
    data = bannerland_topic.read(count=3, wait_timeout=5)

    assert_that(
        data,
        has_items(
            IsSerializedProtobuf(ExportMessage, {
                'offer': {
                    'business_id': BUISNESS_ID,
                    'offer_id': OFFER_ID,
                    'offer_yabs_id': 9861167587634647073,
                    'price': {'currency': 1, 'price': SERVICE_PRICE},
                    'shop_id': SHOP_ID,
                    'timestamp': {'seconds': CURRENT_TIMESTAMP},
                    'original_content': {
                        'url': 'http://original_host1.ru/offer_1',
                        'name': 'original name'
                    },
                    'redirect': {'redirected_host_from_partners': 'http://target_host1.ru'}
                }
            }),
            IsSerializedProtobuf(ExportMessage, {
                'offer': {
                    'business_id': BUISNESS_ID,
                    'offer_id': OFFER_ID_3,
                    'offer_yabs_id': 9359689643716251694,
                    'price': {'currency': 1, 'price': SERVICE_PRICE},
                    'shop_id': SHOP_ID,
                    'timestamp': {'seconds': CURRENT_TIMESTAMP},
                    'original_content': {'url': 'http://original_host2.ru/offer_3'},
                    'redirect': {'redirected_host_from_partners': 'http://target_host2.ru'}
                }
            }),
            IsSerializedProtobuf(ExportMessage, {
                'offer': {
                    'business_id': BUISNESS_ID,
                    'offer_id': OFFER_ID_4,
                    'offer_yabs_id': 17969830383438281894,
                    'price': {'currency': 1, 'price': SERVICE_PRICE},
                    'shop_id': SHOP_ID,
                    'timestamp': {'seconds': CURRENT_TIMESTAMP},
                    'original_content': {
                        'url': 'http://original_host1.ru/offer_4',
                        'available': True
                    },
                    'bigmod': {
                        'Flags': [
                            10013
                        ],
                        'Verdict': 0
                    },
                    'redirect': {'redirected_host_from_partners': 'http://target_host1.ru'}
                }
            })
        )
    )

    assert_that(bannerland_topic, HasNoUnreadData())


def test_send_to_bannerland_preview(inserter, bannerland_preview_topic):
    data = bannerland_preview_topic.read(count=2, wait_timeout=5)

    assert_that(
        data,
        has_items(
            IsSerializedProtobuf(ExportMessage, {
                'offer': {
                    'original_content': {
                        'shop_category_id': DIRECT_CATEGORY_ID,
                        'shop_category_name': DIRECT_CATEGORY_NAME,
                        'supplier': OGRN
                    },
                    'business_id': BUISNESS_ID,
                    'offer_id': OFFER_ID,
                    'offer_yabs_id': 9861167587634647073,
                    'price': {'currency': 1, 'price': SERVICE_PRICE},
                    'shop_id': SHOP_ID,
                    'timestamp': {'seconds': CURRENT_TIMESTAMP},
                    'service': {
                        'platform': 2,
                        'preview': True
                    },
                    'multi_actual_pictures': IsProtobufMap({
                        PICTURE_ORIGINAL_URL: {
                            'by_namespace': IsProtobufMap({
                                NS_YABS_PERFORMANCE: {
                                    'status': OfferPictures.MarketPicture.AVAILABLE
                                }
                            })
                        }
                    })
                }
            }),
            IsSerializedProtobuf(ExportMessage, {
                'offer': {
                    'offer_id': OFFER_ID_4
                }
            })
        )
    )

    assert_that(bannerland_preview_topic, HasNoUnreadData())
