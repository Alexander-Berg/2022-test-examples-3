# coding: utf-8

import pytest
from hamcrest import assert_that, equal_to

from google.protobuf.timestamp_pb2 import Timestamp

from market.idx.datacamp.controllers.stroller.yatf.test_env import make_stroller
from market.idx.datacamp.yatf.matchers.matchers import HasStatus
from market.idx.pylibrary.datacamp.conversion import offer_to_basic_row, offer_to_service_row
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf

from market.idx.datacamp.proto.api.SyncChangeOffer_pb2 import FullOfferResponse
from market.idx.datacamp.proto.offer import DataCampOffer_pb2 as DTC
from market.proto.common.common_pb2 import PriceExpression
from market.idx.datacamp.yatf.utils import create_meta, dict2tskv
from market.idx.datacamp.controllers.stroller.yatf.utils import request_with_price


BUSINESS_ID = 1000
SHOP_ID = 1000
WAREHOUSE_ID = 6000

SHOPS = [
    {
        'shop_id': SHOP_ID,
        'mbi': '\n\n'.join([
            dict2tskv({
                'shop_id': SHOP_ID,
                'warehouse_id': WAREHOUSE_ID,
                'datafeed_id': 100,
                'business_id': BUSINESS_ID,
                'is_discounts_enabled': 'true',
            }),
        ]),
    }
]

OFFERS = [
    (
        'T700',
        DTC.OfferPrice(
            basic=DTC.PriceBundle(
                binary_price=PriceExpression(
                    price=900000000,
                )
            )
        )
    ),
    (
        'T800',
        DTC.OfferPrice(
            basic=DTC.PriceBundle(
                binary_price=PriceExpression(
                    price=4000000000,
                )
            )
        )
    ),
    (
        'T900',
        DTC.OfferPrice(
            basic=DTC.PriceBundle(
                binary_price=PriceExpression(
                    price=1000000000,
                ),
            )
        )
    ),
    (
        'T1000',
        DTC.OfferPrice(
            basic=DTC.PriceBundle(
                binary_price=PriceExpression(
                    price=1000000000,
                ),
                binary_oldprice=PriceExpression(
                    price=10000000000,
                ),
                vat=5,
            )
        )
    ),
]


@pytest.fixture()
def partners():
    return SHOPS


@pytest.fixture()
def basic_offers():
    return [
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id=offer_id,
            ),
            meta=create_meta(10, scope=DTC.BASIC),
        )) for offer_id, price in OFFERS
    ]


@pytest.fixture()
def service_offers():
    return [
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id=offer_id,
                shop_id=SHOP_ID,
                warehouse_id=0,
            ),
            meta=create_meta(10, color=DTC.BLUE, scope=DTC.SERVICE),
            price=price,
        ))
        for offer_id, price in OFFERS
    ]


@pytest.fixture()
def actual_service_offers():
    return [
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id=offer_id,
                shop_id=SHOP_ID,
                warehouse_id=WAREHOUSE_ID,
            ),
            meta=create_meta(10, color=DTC.BLUE, scope=DTC.SERVICE),
        )) for offer_id, _ in OFFERS
    ]


@pytest.yield_fixture()
def stroller(
    config,
    yt_server,
    log_broker_stuff,
    basic_offers_table,
    service_offers_table,
    actual_service_offers_table,
    partners_table,
):
    with make_stroller(
        config,
        yt_server,
        log_broker_stuff,
        shopsdat_cacher=True,
        partners_table=partners_table,
        basic_offers_table=basic_offers_table,
        service_offers_table=service_offers_table,
        actual_service_offers_table=actual_service_offers_table,
    ) as stroller_env:
        yield stroller_env


def request_offer(client, shop_id, offer_id, warehouse_id):
    response = client.get('/shops/{}/offers?offer_id={}&warehouse_id={}'.format(shop_id, offer_id, warehouse_id))
    return response


def expected_offer(
        shop_id,
        offer_id,
        warehouse_id,
        source,
        ts,
        price=None,
        oldprice=None,
        vat=None,
        ts_created=None,
        send_price_only_by_uri=False,
        error_reason=None
):
    timestamp = Timestamp()
    timestamp.FromJsonString(ts)

    ts_created_timestamp = Timestamp()
    if ts_created:
        ts_created_timestamp.FromJsonString(ts_created)
    else:
        ts_created_timestamp.FromSeconds(10)

    result = {
        'identifiers': {
            'shop_id': shop_id,
            'offer_id': offer_id,
            'warehouse_id': warehouse_id,
        },
        'price': {
            'basic': {
                'meta': {
                    'source': source,
                },
            },
        },
        'meta': {
            'ts_created': ts_created_timestamp,
        },
    }

    if not send_price_only_by_uri:
        result['price']['basic']['meta']['timestamp'] = {
            'seconds': timestamp.seconds,
        }
        result['meta'] = {
            'ts_created': ts_created_timestamp,
        }

    if price:
        binary_price = {
            'binary_price': {
                'price': price,
            },
        }
        result['price']['basic'].update(binary_price)

    if oldprice:
        binary_oldprice = {
            'binary_oldprice': {
                'price': oldprice,
            },
        }
        result['price']['basic'].update(binary_oldprice)

    if vat:
        result['price']['basic']['vat'] = vat

    if error_reason:
        resolution = {
            'errors': [
                {
                    'reasons': [error_reason]
                }
            ]
        }
        result['resolution'] = resolution

    return result


def test_set_price(stroller):
    """Проверяем, что ручка изменения цены обновит цены и вернет офер с обновленной ценой"""
    source = DTC.PUSH_PARTNER_OFFICE
    timestamp = '2019-02-15T15:55:55Z'
    shop_id = SHOP_ID
    offer_id = 'T700'
    price = 1000000000
    warehouse_id=WAREHOUSE_ID

    expected_data = {
        'offer': [expected_offer(
            shop_id, offer_id, warehouse_id, price=price, source=source, ts=timestamp,
        )],
    }
    response = request_with_price(
        stroller, shop_id=shop_id, offer_id=offer_id, warehouse_id=warehouse_id, price=price, source=source,
        ts=timestamp,
    )
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(FullOfferResponse, expected_data))
    assert_that(response.headers['Content-type'], equal_to('application/x-protobuf'))


def test_set_price_oldprice_vat_to_new_offer(stroller):
    """Проверяем, что для нового оффера цена, старая цена и vat будут установлены и мы сможем увидеть их по ручке get_offer"""
    source = DTC.PUSH_PARTNER_API
    timestamp = '2019-04-18T15:49:00Z'
    shop_id = SHOP_ID
    offer_id = 'NewOffer01'
    price = 5500000000
    oldprice = 10000000000
    vat = 6
    warehouse_id = WAREHOUSE_ID

    expected_data = {
        'offer': [expected_offer(
            shop_id, offer_id, warehouse_id, price=price, oldprice=oldprice, vat=6, source=source, ts=timestamp,
            ts_created=timestamp
        )],
    }

    response = request_with_price(stroller,
                                  shop_id=shop_id,
                                  offer_id=offer_id,
                                  warehouse_id=warehouse_id,
                                  price=price,
                                  oldprice=oldprice,
                                  vat=vat,
                                  source=source,
                                  ts=timestamp)
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(FullOfferResponse, expected_data))

    response = request_offer(stroller, shop_id=shop_id, offer_id=offer_id, warehouse_id=warehouse_id)
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(FullOfferResponse, expected_data))


def test_set_price_by_ts(stroller):
    """
    Проверяет:
    - при вызове записи метка времени обязательна
    - изменения к офферу применяются только при наличии метки времени больше текущей
    """
    source = DTC.PUSH_PARTNER_API
    timestamp_cur = '2019-05-24T18:00:01Z'
    timestamp_new = '2019-05-24T18:00:02Z'
    shop_id = SHOP_ID
    offer_id = 'OID-XX'
    price = 100
    warehouse_id = WAREHOUSE_ID

    # без метки времени нельзя
    response = request_with_price(stroller, shop_id=shop_id, offer_id=offer_id, warehouse_id=warehouse_id, price=price,
                                  source=source, ts=None)
    assert_that(response, HasStatus(403))

    expected_data = {
        'offer': [expected_offer(
            shop_id, offer_id, warehouse_id, price=price, source=source, ts=timestamp_cur, ts_created=timestamp_cur
        )],
    }
    response = request_with_price(stroller, shop_id=shop_id, offer_id=offer_id, warehouse_id=warehouse_id, price=price,
                                  source=source, ts=timestamp_cur)
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(FullOfferResponse, expected_data))

    # а с актуальной меткой времени - поменяет
    expected_data3 = {
        'offer': [expected_offer(
            shop_id, offer_id, warehouse_id, price=price+1, source=source, ts=timestamp_new, ts_created=timestamp_cur
        )],
    }
    response = request_with_price(stroller, shop_id=shop_id, offer_id=offer_id, warehouse_id=warehouse_id, price=price+1,
                                  source=source, ts=timestamp_new)
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(FullOfferResponse, expected_data3))


def test_set_price_oldprice_and_vat(stroller):
    """Проверяем, что для оффера проставляется и цена, и старая цена, и vat, и мы сможем увидеть ее по ручке get_offer"""
    source = DTC.PUSH_PARTNER_API
    timestamp = '2019-02-15T15:55:55Z'
    shop_id = SHOP_ID
    offer_id = 'T900'
    price = 4000000000
    oldprice = 10000000000
    vat = 8
    warehouse_id = WAREHOUSE_ID

    expected_data = {
        'offer': [expected_offer(
            shop_id, offer_id, warehouse_id, price=price, source=source, ts=timestamp, oldprice=oldprice, vat=vat
        )],
    }
    response = request_with_price(stroller,
                                  shop_id=shop_id,
                                  offer_id=offer_id,
                                  warehouse_id=warehouse_id,
                                  price=price,
                                  oldprice=oldprice,
                                  vat=vat,
                                  source=source,
                                  ts=timestamp)
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(FullOfferResponse, expected_data))


def test_delete_oldprice(stroller):
    """Проверяем, что если для оффера не приехала старая цена, то она удалится, а цена и vat остаются без изменений"""
    source = DTC.PUSH_PARTNER_API
    timestamp = '2019-02-15T15:55:55Z'
    shop_id = SHOP_ID
    offer_id = 'T1000'
    warehouse_id = WAREHOUSE_ID

    expected_data = {
        'offer': [expected_offer(
            shop_id, offer_id, warehouse_id, source=source, ts=timestamp, price=100, vat=5
        )],
    }
    response = request_with_price(stroller,
                                  shop_id=shop_id,
                                  offer_id=offer_id,
                                  warehouse_id=warehouse_id,
                                  price=100,
                                  vat=5,
                                  source=source,
                                  ts=timestamp)
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(FullOfferResponse, expected_data))
