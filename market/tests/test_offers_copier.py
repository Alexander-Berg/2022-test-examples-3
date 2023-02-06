# coding: utf-8

import pytest
import time
from hamcrest import assert_that, has_items, greater_than, not_

import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
import market.idx.datacamp.proto.tables.Partner_pb2 as Partner
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.routines.yatf.resources.config_mock import RoutinesConfigMock
from market.idx.datacamp.yatf.utils import create_meta_dict, create_ts
from market.idx.datacamp.routines.yatf.test_env import OffersCopierEnv
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.matchers.lb_matchers import HasNoUnreadData
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf, IsProtobufMap
from market.idx.yatf.matchers.yt_rows_matchers import (
    HasDatacampPartersYtRows,
)


BUSINESS_ID = 1
BUSINESS_ID_WITHOUT_SHOPS = 2
SHOP_ID_COPY_ALL_OFFERS = 10
SHOP_ID_COPY_FILTERED = 11
SHOP_ID_COPY_BY_OFFER_ID = 12
SHOP_ID_COPY_CONTENT = 13
SHOP_ID_COPY_WITH_WAREHOUSE_ID = 14
SHOP_ID_COPY_FILTERED_MARKET_CATEGORY_ID = 15
SHOP_ID_COPY_FILTERED_TS_CREATED = 16
SHOP_ID_COPY_WITHOUT_SRC_SHOP_IDS = 17
WAREHOUSE_ID = 145
NOW_TS = int(time.time())


@pytest.fixture(scope='module')
def config(yt_server, log_broker_stuff, qoffers_topic):
    config = RoutinesConfigMock(
        yt_server=yt_server,
        log_broker_stuff=log_broker_stuff,
        config={
            'general': {
                'color': 'white',
                'yt_home': '//home/datacamp/united'
            },
            'routines': {
                'enable_offers_copier': True,
                'qoffers_topic': qoffers_topic.topic,
            },
        })
    return config


@pytest.fixture(scope='module')
def qoffers_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def basic_offers_table_data():
    return [
        {
            'identifiers': {
                'business_id': BUSINESS_ID,
                'offer_id': 'o1'
            },
            'content': {
                'binding': {
                    'approved': {
                        'market_category_id': 222
                    }
                }
            },
            'meta': create_meta_dict(10, scope=DTC.BASIC)
        },
        {
            'identifiers': {
                'business_id': BUSINESS_ID,
                'offer_id': 'o2'
            },
            'content': {
                'partner': {
                    'original': {
                        'vendor': {
                            'value': 'vendor_value'
                        },
                        'category': {
                            'id': 111
                        }
                    }
                }
            },
            'meta': create_meta_dict(20, scope=DTC.BASIC)
        },
        {
            'identifiers': {
                'business_id': BUSINESS_ID,
                'offer_id': 'o3'
            },
            'meta': create_meta_dict(10, scope=DTC.BASIC)
        },
        {
            'identifiers': {
                'business_id': BUSINESS_ID_WITHOUT_SHOPS ,
                'offer_id': 'o1'
            },
            'content': {
                'binding': {
                    'approved': {
                        'market_category_id': 222
                    }
                }
            },
            'meta': create_meta_dict(10, scope=DTC.BASIC)
        },
        {
            'identifiers': {
                'business_id': BUSINESS_ID_WITHOUT_SHOPS ,
                'offer_id': 'o2'
            },
            'content': {
                'partner': {
                    'original': {
                        'vendor': {
                            'value': 'vendor_value'
                        },
                        'category': {
                            'id': 111
                        }
                    }
                }
            },
            'meta': create_meta_dict(20, scope=DTC.BASIC)
        },
        {
            'identifiers': {
                'business_id': BUSINESS_ID_WITHOUT_SHOPS ,
                'offer_id': 'o3'
            },
            'meta': create_meta_dict(10, scope=DTC.BASIC)
        },
    ]


@pytest.fixture(scope='module')
def service_offers_table_data():
    return [
        {
            'identifiers': {
                'business_id': BUSINESS_ID,
                'offer_id': 'o1',
                'shop_id': 1
            },
            'meta': create_meta_dict(10, DTC.WHITE, DTC.SERVICE)
        },
        {
            'identifiers': {
                'business_id': BUSINESS_ID,
                'offer_id': 'o2',
                'shop_id': 1
            },
            'meta': create_meta_dict(10, DTC.WHITE, DTC.SERVICE)
        },
        {
            'identifiers': {
                'business_id': BUSINESS_ID,
                'offer_id': 'o3',
                'shop_id': 2
            },
            'price': {
                'basic': {
                    'binary_price': {
                        'price': 10
                    }
                },
                'original_price_fields': {
                    'vat': {
                        'value': 5
                    }
                }
            },
            'status': {
                'disabled': [{
                    'flag': True
                }]
            },
            'meta': create_meta_dict(10, DTC.WHITE, DTC.SERVICE)
        },
        {
            'identifiers': {
                'business_id': BUSINESS_ID,
                'offer_id': 'o3',
                'shop_id': 3
            },
            'price': {
                'basic': {
                    'binary_price': {
                        'price': 15
                    }
                },
                'original_price_fields': {
                    'vat': {
                        'value': 6
                    }
                }
            },
            'meta': create_meta_dict(10, DTC.WHITE, DTC.SERVICE)
        },
    ]


@pytest.fixture(scope='module')
def partners_table_data(yt_server, config):
    return [
        {
            # копирование всех офферов без каких-либо фильтров
            'shop_id': SHOP_ID_COPY_ALL_OFFERS,
            'mbi': {
                'business_id': BUSINESS_ID,
                'shop_id': SHOP_ID_COPY_ALL_OFFERS,
            },
            'partner_additional_info': {
                'offers_copying_tasks': [{
                    'business_id': BUSINESS_ID,
                    'dst_shop_id': SHOP_ID_COPY_ALL_OFFERS,
                    'src_shop_ids': [1],
                    'init_ts': create_ts(10).ToJsonString(),
                    'start_ts': None,
                    'finish_ts': None,
                }]
            }
        },
        {
            # копирование офферов подходящих по фильтру vendor/category_id
            'shop_id': SHOP_ID_COPY_FILTERED,
            'mbi': {
                'business_id': BUSINESS_ID,
                'shop_id': SHOP_ID_COPY_FILTERED,
            },
            'partner_additional_info': {
                'offers_copying_tasks': [{
                    'business_id': BUSINESS_ID,
                    'dst_shop_id': SHOP_ID_COPY_FILTERED,
                    'src_shop_ids': [1],
                    'content_filters': {
                        'vendor': ['vendor_value'],
                        'category_ids': [111],
                    },
                    'init_ts': create_ts(10).ToJsonString(),
                    'start_ts': None,
                    'finish_ts': None,
                }]
            }
        },
        {
            # копирование офферов с определенными offer_id
            'shop_id': SHOP_ID_COPY_BY_OFFER_ID,
            'mbi': {
                'business_id': BUSINESS_ID,
                'shop_id': SHOP_ID_COPY_BY_OFFER_ID,
            },
            'partner_additional_info': {
                'offers_copying_tasks': [{
                    'business_id': BUSINESS_ID,
                    'dst_shop_id': SHOP_ID_COPY_BY_OFFER_ID,
                    'src_shop_ids': [1],
                    'offer_ids': {
                        'offer_id': ['o2'],
                    },
                    'init_ts': create_ts(10).ToJsonString(),
                    'start_ts': None,
                    'finish_ts': None,
                }]
            }
        },
        {
            # копирование контента должно происходить из copy_content_from_shop
            'shop_id': SHOP_ID_COPY_CONTENT,
            'mbi': {
                'business_id': BUSINESS_ID,
                'shop_id': SHOP_ID_COPY_CONTENT,
            },
            'partner_additional_info': {
                'offers_copying_tasks': [{
                    'business_id': BUSINESS_ID,
                    'dst_shop_id': SHOP_ID_COPY_CONTENT,
                    'src_shop_ids': [2, 3],
                    'copy_content_from_shop': 2,
                    'rgb': DTC.BLUE,
                    'init_ts': create_ts(10).ToJsonString(),
                    'start_ts': None,
                    'finish_ts': None,
                }]
            }
        },
        {
            # копирование с проставлением warehouse_id
            'shop_id': SHOP_ID_COPY_WITH_WAREHOUSE_ID,
            'mbi': {
                'business_id': BUSINESS_ID,
                'shop_id': SHOP_ID_COPY_WITH_WAREHOUSE_ID,
            },
            'partner_additional_info': {
                'offers_copying_tasks': [{
                    'business_id': BUSINESS_ID,
                    'dst_shop_id': SHOP_ID_COPY_WITH_WAREHOUSE_ID,
                    'dst_warehouse_id': WAREHOUSE_ID,
                    'src_shop_ids': [1],
                    'init_ts': create_ts(10).ToJsonString(),
                    'start_ts': None,
                    'finish_ts': None,
                }]
            }
        },
        {
            # копирование офферов подходящих по фильтру market_category_ids
            'shop_id': SHOP_ID_COPY_FILTERED_MARKET_CATEGORY_ID,
            'mbi': {
                'business_id': BUSINESS_ID,
                'shop_id': SHOP_ID_COPY_FILTERED_MARKET_CATEGORY_ID,
            },
            'partner_additional_info': {
                'offers_copying_tasks': [{
                    'business_id': BUSINESS_ID,
                    'dst_shop_id': SHOP_ID_COPY_FILTERED_MARKET_CATEGORY_ID,
                    'src_shop_ids': [1],
                    'content_filters': {
                        'market_category_ids': [222],
                    },
                    'init_ts': create_ts(10).ToJsonString(),
                    'start_ts': None,
                    'finish_ts': None,
                }]
            }
        },
        {
            # копирование офферов подходящих по фильтру ts_first_added_from/ts_first_added_to
            'shop_id': SHOP_ID_COPY_FILTERED_TS_CREATED,
            'mbi': {
                'business_id': BUSINESS_ID,
                'shop_id': SHOP_ID_COPY_FILTERED_TS_CREATED,
            },
            'partner_additional_info': {
                'offers_copying_tasks': [{
                    'business_id': BUSINESS_ID,
                    'dst_shop_id': SHOP_ID_COPY_FILTERED_TS_CREATED,
                    'src_shop_ids': [1],
                    'content_filters': {
                        'ts_first_added_from': create_ts(15).ToJsonString(),
                        'ts_first_added_to': create_ts(25).ToJsonString(),
                    },
                    'init_ts': create_ts(10).ToJsonString(),
                    'start_ts': None,
                    'finish_ts': None,
                }]
            }
        },
        {
            # копирование без src_shop_ids
            'shop_id': SHOP_ID_COPY_WITHOUT_SRC_SHOP_IDS,
            'mbi': {
                'business_id': BUSINESS_ID_WITHOUT_SHOPS,
                'shop_id': SHOP_ID_COPY_WITHOUT_SRC_SHOP_IDS,
            },
            'partner_additional_info': {
                'offers_copying_tasks': [{
                    'business_id': BUSINESS_ID_WITHOUT_SHOPS,
                    'dst_shop_id': SHOP_ID_COPY_WITHOUT_SRC_SHOP_IDS,
                    'init_ts': create_ts(10).ToJsonString(),
                    'start_ts': None,
                    'finish_ts': None,
                }]
            }
        },
    ]


@pytest.yield_fixture(scope='module')
def offers_copier(
        yt_server,
        config,
        basic_offers_table,
        service_offers_table,
        partners_table,
        qoffers_topic
):
    resources = {
        'config': config,
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'partners_table': partners_table,
        'qoffers_topic': qoffers_topic,
    }
    with OffersCopierEnv(yt_server, **resources) as offers_copier_env:
        offers_copier_env.verify()
        yield offers_copier_env


@pytest.yield_fixture(scope='module')
def qoffers(offers_copier, qoffers_topic):
    result = qoffers_topic.read(count=1)

    # проверяем, что в топике больше нет данных, которые мы можем вычитать
    assert_that(qoffers_topic, HasNoUnreadData())

    return result


def assert_partner_table(partners_table, shop_id):
    assert_that(
        partners_table.data,
        HasDatacampPartersYtRows(
            [{
                'shop_id': shop_id,
                'partner_additional_info': IsSerializedProtobuf(Partner.PartnerAdditionalInfo, {
                    'offers_copying_tasks': [
                        {
                            'id': 0,
                            'start_ts': {
                                'seconds': greater_than(NOW_TS)
                            },
                            'finish_ts': {
                                'seconds': greater_than(NOW_TS)
                            },
                        },
                    ]
                })
            }]
        )
    )


def offer_matcher(offer_id, shop_id, warehouse_id=None, business_id=BUSINESS_ID):
    return {
        'service': IsProtobufMap({
            shop_id: {
                'identifiers': {
                    'business_id': business_id,
                    'offer_id': offer_id,
                    'shop_id': shop_id,
                    'warehouse_id': warehouse_id
                }
            }
        })
    }


def test_copy_all_offers(offers_copier, qoffers):
    assert_partner_table(offers_copier.partners_table, SHOP_ID_COPY_ALL_OFFERS)
    assert_that(qoffers, has_items(*[
        IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [
                    offer_matcher('o1', SHOP_ID_COPY_ALL_OFFERS),
                    offer_matcher('o2', SHOP_ID_COPY_ALL_OFFERS),
                ]
            }]
        }),
    ]))


def test_copy_with_content_filters(offers_copier, qoffers):
    assert_partner_table(offers_copier.partners_table, SHOP_ID_COPY_FILTERED)
    assert_that(qoffers, has_items(*[
        IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [
                    offer_matcher('o2', SHOP_ID_COPY_FILTERED)
                ]
            }]
        }),
    ]))

    assert_that(qoffers, not_(has_items(*[
        IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [
                    offer_matcher('o1', SHOP_ID_COPY_FILTERED)
                ]
            }]
        }),
    ])))


def test_copy_fixed_offers(offers_copier, qoffers):
    assert_partner_table(offers_copier.partners_table, SHOP_ID_COPY_BY_OFFER_ID)
    assert_that(qoffers, has_items(*[
        IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [
                    offer_matcher('o2', SHOP_ID_COPY_BY_OFFER_ID)
                ]
            }]
        }),
    ]))

    assert_that(qoffers, not_(has_items(*[
        IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [
                    offer_matcher('o1', SHOP_ID_COPY_BY_OFFER_ID)
                ]
            }]
        }),
    ])))


def test_copy_proper_content(offers_copier, qoffers):
    assert_partner_table(offers_copier.partners_table, SHOP_ID_COPY_CONTENT)
    assert_that(qoffers, has_items(*[
        IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [
                    {
                        'service': IsProtobufMap({
                            SHOP_ID_COPY_CONTENT: {
                                'identifiers': {
                                    'business_id': BUSINESS_ID,
                                    'offer_id': 'o3',
                                    'shop_id': SHOP_ID_COPY_CONTENT,
                                },
                                'price': {
                                    'basic': {
                                        'binary_price': {
                                            'price': 10
                                        }
                                    },
                                    'original_price_fields': {
                                        'vat': {
                                            'value': 5
                                        }
                                    }
                                },
                                'status': {  # статус не должны копировать, только ставим united_catalog флаг
                                    'united_catalog': {
                                        'flag': True
                                    }
                                },
                                'meta': {
                                    'rgb': DTC.BLUE
                                }
                            }
                        })
                    }
                ]
            }]
        }),
    ]))


def test_copy_and_set_warehouse_id(offers_copier, qoffers):
    assert_partner_table(offers_copier.partners_table, SHOP_ID_COPY_WITH_WAREHOUSE_ID)
    assert_that(qoffers[0], IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [
                offer_matcher('o1', SHOP_ID_COPY_WITH_WAREHOUSE_ID, WAREHOUSE_ID),
                offer_matcher('o2', SHOP_ID_COPY_WITH_WAREHOUSE_ID, WAREHOUSE_ID),
            ]
        }]
    }))


def test_copy_with_market_category_id_filters(offers_copier, qoffers):
    assert_partner_table(offers_copier.partners_table, SHOP_ID_COPY_FILTERED_MARKET_CATEGORY_ID)
    assert_that(qoffers, has_items(*[
        IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [
                    offer_matcher('o1', SHOP_ID_COPY_FILTERED_MARKET_CATEGORY_ID)
                ]
            }]
        }),
    ]))

    assert_that(qoffers, not_(has_items(*[
        IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [
                    offer_matcher('o2', SHOP_ID_COPY_FILTERED_MARKET_CATEGORY_ID)
                ]
            }]
        }),
    ])))


def test_copy_with_ts_created_filters(offers_copier, qoffers):
    assert_partner_table(offers_copier.partners_table, SHOP_ID_COPY_FILTERED_TS_CREATED)
    assert_that(qoffers, has_items(*[
        IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [
                    offer_matcher('o2', SHOP_ID_COPY_FILTERED_TS_CREATED)
                ]
            }]
        }),
    ]))

    assert_that(qoffers, not_(has_items(*[
        IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [
                    offer_matcher('o1', SHOP_ID_COPY_FILTERED_TS_CREATED)
                ]
            }]
        }),
    ])))


def test_copy_without_src_shop_ids(offers_copier, qoffers):
    assert_partner_table(offers_copier.partners_table, SHOP_ID_COPY_WITHOUT_SRC_SHOP_IDS)
    assert_that(qoffers, has_items(*[
        IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [
                    offer_matcher('o1', SHOP_ID_COPY_WITHOUT_SRC_SHOP_IDS, business_id=BUSINESS_ID_WITHOUT_SHOPS),
                    offer_matcher('o2', SHOP_ID_COPY_WITHOUT_SRC_SHOP_IDS, business_id=BUSINESS_ID_WITHOUT_SHOPS),
                    offer_matcher('o3', SHOP_ID_COPY_WITHOUT_SRC_SHOP_IDS, business_id=BUSINESS_ID_WITHOUT_SHOPS),
                ]
            }]
        }),
    ]))
