# coding: utf-8
from hamcrest import assert_that, is_not
import pytest

from market.idx.datacamp.routines.yatf.test_env import HttpRoutinesTestEnv
from market.idx.datacamp.routines.yatf.resources.config_mock import RoutinesConfigMock
from market.idx.datacamp.yatf.matchers.matchers import HasStatus, HasSerializedDatacampMessages
from market.idx.datacamp.yatf.utils import create_meta_dict, create_update_meta_dict
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.yatf.resources.lbk_topic import LbkTopic


BUSINESS_ID = 1000

OFFERS_DATA = [
    (
        1, 'dropship.offer.with.stock', 10,
            {
                'partner_stocks': {
                    'meta': create_update_meta_dict(100),
                    'count': 1
                }
            }, 12345
    ),
    (
        1, 'dropship.offer.with.stock.empty.msku', 10,
            {
                'partner_stocks': {
                    'meta': create_update_meta_dict(100),
                    'count': 1
                }
            }, None
    ),
    (
        1, 'dropship.offer.without.stock', 10, None, 12345
    ),
    (
        2, 'crossdock.offer.with.stock', 11,
        {
            'partner_stocks': {
                'meta': create_update_meta_dict(1000),
                'count': 2
            }
        }, 12345
    ),
    (
        2, 'crossdock.offer.without.stock', 11, None, 12345
    ),
    (
        3, 'fulfillment.shop.offer.with.stock', 12,
        {
            'partner_stocks': {
                'meta': create_update_meta_dict(10000),
                'count': 3
            }
        }, 12345
    ),
    (
        4, 'disabled.shop.offer.with.stock', 10,
        {
            'partner_stocks': {
                'meta': create_update_meta_dict(100000),
                'count': 4
            }
        }, 12345
    )
]


@pytest.fixture(scope='module')
def business_id():
    return BUSINESS_ID


@pytest.fixture(scope='module')
def partners_stock_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def config(yt_server, log_broker_stuff, partners_stock_topic):
    cfg = {
        'general': {
            'color': 'blue',
        },
        'partners_stock': {
            'topic': partners_stock_topic.topic
        },
    }
    return RoutinesConfigMock(
        yt_server=yt_server,
        log_broker_stuff=log_broker_stuff,
        config=cfg)


@pytest.fixture(scope='module')
def partners_table_data():
    return [
        # dropship
        {
            'shop_id': 1,
            'status': 'publish',
            'mbi': {
                'business_id': BUSINESS_ID,
                'shop_id': 1,
                'datafeed_id': 123,
                'is_push_partner': True,
                'ff_program': 'NO',
                'direct_shipping': True,
                'warehouse_id': 10,
                'blue_status': 'REAL',
            }
        },
        # crossdock
        {
            'shop_id': 2,
            'status': 'publish',
            'mbi': {
                'business_id': BUSINESS_ID,
                'shop_id': 2,
                'datafeed_id': 1234,
                'is_push_partner': True,
                'ff_program': 'REAL',
                'direct_shipping': False,
                'warehouse_id': 11,
                'blue_status': 'REAL',
            }
        },
        # fulfillment
        {
            'shop_id': 3,
            'status': 'publish',
            'mbi': {
                'business_id': BUSINESS_ID,
                'shop_id': 3,
                'datafeed_id': 1234,
                'is_push_partner': True,
                'ff_program': 'REAL',
                'direct_shipping': True,
                'warehouse_id': 12,
                'blue_status': 'REAL',
            }
        },
        {
            'shop_id': 4,
            'status': 'disable',
        },
    ]


@pytest.fixture(scope='module')
def basic_offers_table_data():
    return [
        {
            'identifiers': {
                'business_id': BUSINESS_ID,
                'offer_id': offer_id
            },
            'meta': create_meta_dict(10),
            'content': {
                'binding': {
                    'approved': {
                        'market_sku_id': msku
                    }
                }
            } if msku is not None else None,
        } for shop_id, offer_id, warehouse_id, stock_info, msku in OFFERS_DATA
    ]


@pytest.fixture(scope='module')
def actual_service_offers_table_data():
    return [
        {
            'identifiers': {
                'business_id': BUSINESS_ID,
                'offer_id': offer_id,
                'shop_id': shop_id,
                'warehouse_id': warehouse_id,
            },
            'meta': create_meta_dict(10, color=DTC.BLUE),
            'stock_info': stock_info
        } for shop_id, offer_id, warehouse_id, stock_info, msku in OFFERS_DATA
    ]


@pytest.yield_fixture(scope='module')
def routines_http(yt_server, config, basic_offers_table, actual_service_offers_table, partners_table, partners_stock_topic):
    resources = {
        'partners_table': partners_table,
        'basic_offers_table': basic_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
        'partners_stock_topic': partners_stock_topic,
        'config': config,
    }
    with HttpRoutinesTestEnv(yt_server, **resources) as routines_http_env:
        yield routines_http_env


def test_send_partners_stock(partners_stock_topic, routines_http, business_id):
    """
    После вызова ручки send_partners_stock в топик с партенрскими стоками отправятся все офферы
    """
    response = routines_http.post('/send_partners_stock')
    assert_that(response, HasStatus(200))

    data = partners_stock_topic.read(count=2)
    assert_that(data, HasSerializedDatacampMessages([{
        'offers': [{
            'offer': [
                {
                    'identifiers': {
                        'business_id': business_id,
                        'shop_id': 1,
                        'offer_id': 'dropship.offer.with.stock',
                        'warehouse_id': 10,
                    }
                },
            ]
        }]
    }, {
        'offers': [{
            'offer': [
                {
                    'identifiers': {
                        'business_id': business_id,
                        'shop_id': 2,
                        'offer_id': 'crossdock.offer.with.stock',
                        'warehouse_id': 11,
                    }
                },
            ]
        }]
    }]))
    assert_that(data, is_not(HasSerializedDatacampMessages([{
        'offers': [{
            'offer': [
                {
                    'identifiers': {
                        'business_id': business_id,
                        'offer_id': 'dropship.offer.with.stock.empty.msku',
                    }
                },
            ]
        }]
    }])))
    assert_that(data, is_not(HasSerializedDatacampMessages([{
        'offers': [{
            'offer': [
                {
                    'identifiers': {
                        'business_id': business_id,
                        'offer_id': 'dropship.offer.without.stock',
                    }
                },
            ]
        }]
    }])))
    assert_that(data, is_not(HasSerializedDatacampMessages([{
        'offers': [{
            'offer': [
                {
                    'identifiers': {
                        'business_id': business_id,
                        'offer_id': 'crossdock.offer.without.stock',
                    }
                },
            ]
        }]
    }])))


def test_send_partners_stock_shop_filter(partners_stock_topic, routines_http, business_id):
    """
    Проверяем фильтрацию по shop_id
    """
    response = routines_http.post('/send_partners_stock?shop_id=2')
    assert_that(response, HasStatus(200))

    data = partners_stock_topic.read(count=1)
    assert_that(data, HasSerializedDatacampMessages([{
        'offers': [{
            'offer': [
                {
                    'identifiers': {
                        'business_id': business_id,
                        'shop_id': 2,
                        'offer_id': 'crossdock.offer.with.stock',
                        'warehouse_id': 11,
                    }
                },
            ]
        }]
    }]))


def test_send_partners_stock_warehouse_filter(partners_stock_topic, routines_http, business_id):
    """
    Проверяем фильтрацию по warehouse_id
    """
    response = routines_http.post('/send_partners_stock?warehouse_id=11'.format())
    assert_that(response, HasStatus(200))

    data = partners_stock_topic.read(count=1)
    assert_that(data, HasSerializedDatacampMessages([{
        'offers': [{
            'offer': [
                {
                    'identifiers': {
                        'business_id': business_id,
                        'shop_id': 2,
                        'offer_id': 'crossdock.offer.with.stock',
                        'warehouse_id': 11,
                    }
                },
            ]
        }]
    }]))


def test_send_partners_stock_timestamp_filter(partners_stock_topic, routines_http, business_id):
    """
    Проверяем фильтрацию по timestamp
    """
    response = routines_http.post('/send_partners_stock?timestamp=999')
    assert_that(response, HasStatus(200))

    data = partners_stock_topic.read(count=1)
    assert_that(data, HasSerializedDatacampMessages([{
        'offers': [{
            'offer': [
                {
                    'identifiers': {
                        'business_id': business_id,
                        'shop_id': 2,
                        'offer_id': 'crossdock.offer.with.stock',
                        'warehouse_id': 11,
                    }
                },
            ]
        }]
    }]))


def test_send_partners_stock_shops_filter(partners_stock_topic, routines_http, business_id):
    """
    Проверяем фильтрацию по нескольким shop_id
    """
    response = routines_http.post('/send_partners_stock?shop_id=1&shop_id=2')
    assert_that(response, HasStatus(200))

    data = partners_stock_topic.read(count=2)
    assert_that(data, HasSerializedDatacampMessages([{
        'offers': [{
            'offer': [
                {
                    'identifiers': {
                        'business_id': business_id,
                        'shop_id': 1,
                        'offer_id': 'dropship.offer.with.stock',
                        'warehouse_id': 10,
                    }
                },
            ]
        }]
    }, {
        'offers': [{
            'offer': [
                {
                    'identifiers': {
                        'business_id': business_id,
                        'shop_id': 2,
                        'offer_id': 'crossdock.offer.with.stock',
                        'warehouse_id': 11,
                    }
                },
            ]
        }]
    }]))
