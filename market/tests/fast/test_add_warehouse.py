# coding: utf-8

import pytest
import six
from hamcrest import assert_that, has_length, equal_to, greater_than

import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.controllers.stroller.yatf.test_env import make_stroller
from market.idx.datacamp.yatf.matchers.matchers import HasStatus
from market.idx.datacamp.yatf.utils import dict2tskv, create_update_meta
from market.idx.datacamp.yatf.matchers.matchers import HasSerializedDatacampMessages
from market.idx.yatf.matchers.protobuf_matchers import IsProtobufMap
from market.idx.pylibrary.datacamp.conversion import offer_to_service_row

BUSINESS_ID = 1

PARTNERS_TABLE_DATA = [
    {
        'shop_id': 1,
        'mbi': '\n\n'.join([
            dict2tskv({'shop_id': 1, 'warehouse_id': 100, 'datafeed_id': 1, 'business_id': BUSINESS_ID}),
            dict2tskv({'shop_id': 1, 'warehouse_id': 200, 'datafeed_id': 2, 'business_id': BUSINESS_ID})
        ])
    },
    {
        'shop_id': 2,
        'mbi': dict2tskv({'shop_id': 2, 'warehouse_id': 100, 'datafeed_id': 3})
    },
    {
        'shop_id': 3,
        'mbi': None
    }
]

SERVICE_OFFERS_TABLE_DATA = [
    offer_to_service_row(DTC.Offer(
        identifiers=DTC.OfferIdentifiers(
            business_id=BUSINESS_ID,
            offer_id='T1001',
            shop_id=1,
        ),
        meta=DTC.OfferMeta(
            rgb=DTC.BLUE,
        ),
        stock_info=DTC.OfferStockInfo(
            partner_stocks_default=DTC.OfferStocks(
                count=100,
                meta=create_update_meta(10, source=DTC.PUSH_PARTNER_OFFICE),
            ),
        )
    ))]


@pytest.fixture(scope='module')
def partners():
    return PARTNERS_TABLE_DATA


@pytest.fixture(scope='module')
def service_offers():
    return SERVICE_OFFERS_TABLE_DATA


@pytest.yield_fixture(scope='module')
def stroller(
    config,
    yt_server,
    log_broker_stuff,
    partners_table,
    service_offers_table,
    united_offers_topic,
):
    with make_stroller(
        config,
        yt_server,
        log_broker_stuff,
        shopsdat_cacher=True,
        partners_table=partners_table,
        service_offers_table=service_offers_table,
        united_offers_topic=united_offers_topic,
    ) as stroller_env:
        yield stroller_env


def do_request_add_warehouse(client, shop_id, feed_id, warehouse_id, united=False):
    return client.post('/shops/{}/add_warehouse?feed_id={}&warehouse_id={}&united={}'.format(
        shop_id, feed_id, warehouse_id, united
    ))


def test_add_ff_warehouse(stroller):
    """Проверяем, что ff-склады не добавляются в единое хранилище"""
    shop_id = 1
    feed_id = 10
    warehouse_id = 145

    response = do_request_add_warehouse(stroller, shop_id=shop_id, feed_id=feed_id, warehouse_id=warehouse_id, united=True)
    assert_that(response, HasStatus(400))
    assert_that(six.ensure_str(response.data), equal_to('Bad warehouse_id: can`t create fullfillment warehouse.'))


def test_add_united_warehouse(stroller, united_offers_topic):
    """ Проверяем добавление склада в единое хранилище:
        - на основе сервисных офферов создаются actual service и stock-оффера
        - actual service оффер содержит только идентификаторы и новую мету
        - stock оффер содержит идентификаторы, новую мету и партнерские стоки (из service оффера)
    """
    shop_id = 1
    feed_id = 10
    warehouse_id = 150

    response = do_request_add_warehouse(stroller, shop_id=shop_id, feed_id=feed_id, warehouse_id=warehouse_id, united=True)
    assert_that(response, HasStatus(200))
    assert_that(six.ensure_str(response.data), equal_to('{} offers were copied to warehouse {}'.format(1, warehouse_id)))

    messages = united_offers_topic.read(count=1)
    assert_that(messages, has_length(1))

    assert_that(messages, HasSerializedDatacampMessages([{
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': 'T1001',
                    },
                    'meta': {
                        'scope': DTC.BASIC
                    }
                },
                'actual': IsProtobufMap({
                    shop_id: {
                        'warehouse': IsProtobufMap({
                            warehouse_id: {
                                'identifiers': {
                                    'business_id': BUSINESS_ID,
                                    'offer_id': 'T1001',
                                    'shop_id': shop_id,
                                    'warehouse_id': warehouse_id,
                                    'feed_id': feed_id,
                                },
                                'meta': {
                                    'scope': DTC.SERVICE,
                                    'rgb': DTC.BLUE,
                                },
                                'stock_info': {
                                    'partner_stocks': {
                                        'count': 100,
                                        'meta': {
                                            'timestamp': {
                                                'seconds': 10
                                            },
                                            'source': DTC.PUSH_PARTNER_OFFICE
                                        }
                                    }
                                }
                            }
                        })
                    }
                }),
            }]
        }]
    }]))

    # проверяем, что мета у actual service и stock офферов - новая
    message = DatacampMessage()
    message.ParseFromString(messages[0])
    united = message.united_offers[0].offer[0]
    assert_that(united.actual[shop_id].warehouse[warehouse_id].meta.ts_created.seconds, greater_than(10))
