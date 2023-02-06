# coding: utf-8

import uuid
import pytest
from datetime import datetime, timedelta

from market.idx.yatf.resources.lbk_topic import LbkTopic
from hamcrest import assert_that, has_items, greater_than
from market.idx.datacamp.routines.yatf.utils import (
    make_basic_offer,
    make_service_offer,
    make_actual_service_offer
)

from market.idx.yatf.resources.yt_table_resource import YtTableResource
from market.idx.datacamp.routines.yatf.resources.config_mock import RoutinesConfigMock
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf, IsProtobufMap
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.routines.yatf.test_env import HttpRoutinesTestEnv


@pytest.fixture(scope='module')
def mboc_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def miner_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def config(yt_server, log_broker_stuff, mboc_topic, miner_topic):
    cfg = {
        'general': {
            'color': 'white',
        },
        'routines': {
            'mboc_topic': mboc_topic.topic
        },
        'miner': {
            'output_topic': miner_topic.topic
        },
    }
    return RoutinesConfigMock(yt_server=yt_server, log_broker_stuff=log_broker_stuff, config=cfg)


@pytest.fixture(scope='module')
def basic_offers_table_data():
    price = {
        'basic': {
            'binary_price': {
                'price': 10
            }
        }
    }

    return [
        make_basic_offer(1, 'T600', merge_data={
            'price': price,
            'status': {
                'consistency': {
                    'mboc_consistency': True
                }
            }
        }),
        make_basic_offer(1, 'T700', merge_data={
            'price': price
        }),
        make_basic_offer(1, 'T800', merge_data={
            'price': price,
            'status': {
                'consistency': {
                    'mboc_consistency': True
                }
            }
        }),
        make_basic_offer(1, 'only_basic_offer', merge_data={
            'price': price
        }),
    ]


@pytest.fixture(scope='module')
def service_offers_table_data():
    return [
        make_service_offer(business_id, shop_sku, shop_id)
        for business_id, shop_sku, shop_id in [
            (1, 'T600', 1),
            (1, 'T700', 1),
            (1, 'T700', 2),
        ]
    ]


@pytest.fixture(scope='module')
def actual_service_offers_table_data():
    return [
        make_actual_service_offer(business_id, shop_sku, shop_id, warehouse_id)
        for business_id, shop_sku, shop_id, warehouse_id in [
            (1, 'T600', 1, 10),
            (1, 'T600', 1, 33),
            (1, 'T700', 1, 10),
            (1, 'T700', 3, 10),
        ]
    ]


@pytest.fixture(scope='module')
def source_offer_ids_table(yt_server):
    return YtTableResource(
        yt_server,
        '//home/offer_ids' + str(uuid.uuid4()),
        attributes=dict(
            dynamic=False,
            external=False,
            schema=[
                dict(name='business_id', type='uint32'),
                dict(name='offer_id', type='string'),
            ]
        ),
        data=[
            {'business_id': 1, 'offer_id': 'T600'},
            {'business_id': 1, 'offer_id': 'T700'},
        ])


@pytest.fixture(scope='module')
def saas_source_offer_ids_table(yt_server):
    return YtTableResource(
        yt_server,
        '//home/offer_ids' + str(uuid.uuid4()),
        attributes=dict(
            dynamic=False,
            external=False,
            schema=[
                dict(name='business_id', type='uint32'),
                dict(name='offer_id', type='string'),
            ]
        ),
        data=[
            {'business_id': 1, 'offer_id': 'only_basic_offer'},
        ])


@pytest.yield_fixture(scope='module')
def routines_http(yt_server, config, basic_offers_table, service_offers_table, actual_service_offers_table,
                  source_offer_ids_table, saas_source_offer_ids_table, mboc_topic, miner_topic):
    resources = {
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
        'source_offer_ids_table': source_offer_ids_table,
        'saas_source_offer_ids_table': saas_source_offer_ids_table,
        'config': config,
        'mboc_topic': mboc_topic,
        'miner_topic': miner_topic,
    }
    with HttpRoutinesTestEnv(yt_server, **resources) as routines_http_env:
        yield routines_http_env


def test_send_to_mbo(mboc_topic, routines_http):
    routines_http.post('/send_to_subscriber?business_id=1&subscriber=MBOC_SUBSCRIBER')
    data = mboc_topic.read(1)

    assert_that(data, has_items(*[
        IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [
                    {
                        'basic': {
                            'identifiers': {
                                'business_id': 1,
                                'offer_id': 'T600'
                            }
                        },
                        'service': IsProtobufMap({
                            1: {}
                        })
                    }
                ]
            }]
        }),
    ]))

    assert_that(data, has_items(*[
        IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [
                    {
                        'basic': {
                            'identifiers': {
                                'business_id': 1,
                                'offer_id': 'T800'
                            }
                        },
                    }
                ]
            }]
        })
    ]))


def test_send_to_saas(miner_topic, routines_http):
    routines_http.post('/send_to_subscriber?business_id=1&shop_id=1&subscriber=SAAS_SUBSCRIBER')
    data = miner_topic.read(1)

    past = (datetime.utcnow() - timedelta(minutes=45) - datetime(1970, 1, 1)).total_seconds()

    assert_that(data, has_items(*[
        IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [
                    {
                        'basic': {
                            'identifiers': {
                                'business_id': 1,
                                'offer_id': 'T600'
                            },
                            'meta': {
                                'saas_force_send': {
                                    'meta': {
                                        'timestamp': {
                                            'seconds': greater_than(past)
                                        }
                                    }
                                }
                            }
                        },
                    },
                    {
                        'basic': {
                            'identifiers': {
                                'business_id': 1,
                                'offer_id': 'T700'
                            },
                            'meta': {
                                'saas_force_send': {
                                    'meta': {
                                        'timestamp': {
                                            'seconds': greater_than(past)
                                        }
                                    }
                                }
                            }
                        },
                    }
                ]
            }]
        }),
    ]))


# identical tests commented out
@pytest.mark.parametrize('subscriber,field', [
#    ('DIRECT_SEARCH_SNIPPET_MODERATION_SUBSCRIBER', 'direct_search_snippet_moderation_force_send'),
#    ('CMI_PROMO_SUBSCRIBER', 'cmi_promo_force_send'),
#    ('MDM_SUBSCRIBER', 'mdm_force_send'),
#    ('PRICELABS_SUBSCRIBER', 'pricelabs_force_send'),
#    ('IRIS_SUBSCRIBER', 'iris_force_send'),
#    ('TURBO_SUBSCRIBER', 'turbo_force_send'),
#    ('PICROBOT_SUBSCRIBER', 'picrobot_force_send'),
#    ('CM_SUBSCRIBER', 'cm_force_send'),
#    ('PROMO_SAAS_SUBSCRIBER', 'promo_saas_force_send'),
#    ('MBI_OFFER_BIDS_REPLY_SUBSCRIBER', 'mbi_offer_bids_reply_force_send'),
#    ('PARTNER_STOCK_SUBSCRIBER', 'partner_stock_force_send'),
    ('REPORT_RTY_SUBSCRIBER', 'report_rty_force_send'),
#    ('SAMOVAR_SUBSCRIBER', 'samovar_force_send'),
#    ('BANNERLAND_PREVIEW_SUBSCRIBER', 'bannerland_preview_force_send'),
#    ('BANNERLAND_SUBSCRIBER', 'bannerland_force_send')
#    ('SORT_DC_SUBSCRIBER', 'sort_dc_force_send'),
#    ('MODEL_MINER_SUBSCRIBER', 'model_miner_force_send'),
#    ('CONTENT_STORAGE_MAPPINGS_SUBSCRIBER', 'content_storage_mappings_force_send'),
   ('PICROBOT_VIDEO_SUBSCRIBER', 'picrobot_video_force_send'),
])
def test_send_to_subscriber(miner_topic, routines_http, subscriber, field):
    routines_http.post('/send_to_subscriber?business_id=1&shop_id=1&subscriber={0}'.format(subscriber))
    data = miner_topic.read(1)

    past = (datetime.utcnow() - timedelta(minutes=45) - datetime(1970, 1, 1)).total_seconds()

    assert_that(data, has_items(*[
        IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [
                    {
                        'basic': {
                            'identifiers': {
                                'business_id': 1,
                                'offer_id': 'T600'
                            }
                        },
                        'service': IsProtobufMap({
                            1: {
                                'identifiers': {
                                    'business_id': 1,
                                    'shop_id': 1,
                                    'offer_id': 'T600'
                                },
                                'meta': {
                                    field: {
                                        'meta': {
                                            'timestamp': {
                                                'seconds': greater_than(past)
                                            }
                                        }
                                    }
                                }
                            }
                        })
                    },
                    {
                        'basic': {
                            'identifiers': {
                                'business_id': 1,
                                'offer_id': 'T700'
                            }
                        },
                        'service': IsProtobufMap({
                            1: {
                                'identifiers': {
                                    'business_id': 1,
                                    'shop_id': 1,
                                    'offer_id': 'T700'
                                },
                                'meta': {
                                    field: {
                                        'meta': {
                                            'timestamp': {
                                                'seconds': greater_than(past)
                                            }
                                        }
                                    }
                                }
                            }
                        })
                    }
                ]
            }]
        }),
    ]))


def test_send_offers_from_table(miner_topic, routines_http, source_offer_ids_table, config):
    routines_http.post('/send_to_subscriber?sandbox=false&table_path={table_path}&proxy={proxy}&subscriber={subscriber}'.format(
        table_path=source_offer_ids_table.table_path,
        proxy=config.yt_meta_proxy,
        subscriber='PARTNER_STOCK_SUBSCRIBER'
    ))
    data = miner_topic.read(1)

    past = (datetime.utcnow() - timedelta(minutes=45) - datetime(1970, 1, 1)).total_seconds()

    assert_that(data, has_items(*[
        IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [
                    {
                        'basic': {
                            'identifiers': {
                                'business_id': 1,
                                'offer_id': 'T600'
                            }
                        },
                        'service': IsProtobufMap({
                            1: {
                                'identifiers': {
                                    'business_id': 1,
                                    'shop_id': 1,
                                    'offer_id': 'T600'
                                },
                                'meta': {
                                    'partner_stock_force_send': {
                                        'meta': {
                                            'timestamp': {
                                                'seconds': greater_than(past)
                                            }
                                        }
                                    }
                                }
                            }
                        })
                    }
                ]
            }]
        }),
    ]))


def test_send_saas_offers_from_table(miner_topic, routines_http, saas_source_offer_ids_table, config):
    routines_http.post('/send_to_subscriber?sandbox=false&table_path={table_path}&proxy={proxy}&subscriber={subscriber}'.format(
        table_path=saas_source_offer_ids_table.table_path,
        proxy=config.yt_meta_proxy,
        subscriber='SAAS_SUBSCRIBER'
    ))
    data = miner_topic.read(1)

    past = (datetime.utcnow() - timedelta(minutes=45) - datetime(1970, 1, 1)).total_seconds()

    assert_that(data, has_items(*[
        IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [
                    {
                        'basic': {
                            'identifiers': {
                                'business_id': 1,
                                'offer_id': 'only_basic_offer'
                            },
                            'meta': {
                                'saas_force_send': {
                                    'meta': {
                                        'timestamp': {
                                            'seconds': greater_than(past)
                                        }
                                    }
                                }
                            }
                        },
                    }
                ]
            }]
        }),
    ]))
