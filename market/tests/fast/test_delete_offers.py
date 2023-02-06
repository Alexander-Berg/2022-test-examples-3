# coding: utf-8

import pytest
from hamcrest import assert_that
from datetime import datetime

from market.idx.datacamp.controllers.stroller.yatf.test_env import make_stroller
from market.idx.datacamp.yatf.matchers.matchers import HasStatus
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf, IsProtobufMap
from market.idx.datacamp.proto.api import SyncChangeOffer_pb2 as SyncApi
from market.idx.datacamp.proto.offer import DataCampOffer_pb2 as DTC
from market.idx.yatf.matchers.yt_rows_matchers import HasDatacampYtUnitedOffersRows, HasOffers
from market.idx.pylibrary.datacamp.conversion import offer_to_basic_row, offer_to_service_row
from market.idx.datacamp.yatf.utils import create_meta, dict2tskv, create_update_meta
from market.idx.yatf.utils.utils import create_timestamp_from_json
from market.pylibrary.proto_utils import message_from_data

NOW_UTC = datetime.utcnow()  # JSON serializer should always use UTC
time_pattern = "%Y-%m-%dT%H:%M:%SZ"
current_time = NOW_UTC.strftime(time_pattern)
current_ts = create_timestamp_from_json(current_time)

BUSINESS_ID = 1
DSBS_SHOP_ID = 1000
FF_SHOP_ID = 1001
DSBS_WAREHOUSE_ID = 654321
FF_WAREHOUSE_ID_145 = 145
FF_WAREHOUSE_ID_172 = 172
REQUEST_MODE = [True, False]


@pytest.fixture(scope='module')
def partners():
    return [
        {
            'shop_id': DSBS_SHOP_ID,
            'mbi': '\n\n'.join([
                dict2tskv({
                    'shop_id': DSBS_SHOP_ID,
                    'business_id': BUSINESS_ID,
                }),
            ]),
        },
        {
            'shop_id': FF_SHOP_ID,
            'mbi': '\n\n'.join([
                dict2tskv({
                    'shop_id': FF_SHOP_ID,
                    'business_id': BUSINESS_ID,
                    'blue_status': 'REAL',
                    'warehouse_id': FF_WAREHOUSE_ID_145,
                }),
                dict2tskv({
                    'shop_id': FF_SHOP_ID,
                    'business_id': BUSINESS_ID,
                    'blue_status': 'REAL',
                    'warehouse_id': FF_WAREHOUSE_ID_172,
                }),
            ]),
        },
    ]


@pytest.fixture(scope='module', params=REQUEST_MODE)
def proto_body_mode(request):
    return request.param


def make_offer_id(offer_id, proto_body_mode):
    return '{}-{}'.format(offer_id, proto_body_mode)


@pytest.fixture(scope='module')
def basic_offers(color_name):
    return [
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id=make_offer_id(offer_id, proto_body_mode),
            ),
            meta=create_meta(10, scope=DTC.BASIC),
        )) for proto_body_mode in REQUEST_MODE for offer_id in [
            'offer_to_remove_basic',
            'offer_to_remove_basic_but_disabled_by_stock',
            'offer_to_remove_basic_but_disabled_by_stock_force_mode',
            'offer_to_remove_service',
            'offer_to_remove_service_but_disabled_by_stock',
            'offer_to_remove_service_but_disabled_by_stock_force_mode',
        ]
    ]


@pytest.fixture(scope='module')
def service_offers(color_name):
    return [
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id=make_offer_id(offer_id, proto_body_mode),
                shop_id=shop_id,
            ),
            meta=create_meta(10, scope=DTC.SERVICE),
        ))
        for proto_body_mode in REQUEST_MODE for shop_id, offer_id in[
            (DSBS_SHOP_ID, 'offer_to_remove_basic'),
            (FF_SHOP_ID, 'offer_to_remove_basic'),
            (DSBS_SHOP_ID, 'offer_to_remove_basic_but_disabled_by_stock'),
            (FF_SHOP_ID, 'offer_to_remove_basic_but_disabled_by_stock'),
            (DSBS_SHOP_ID, 'offer_to_remove_basic_but_disabled_by_stock_force_mode'),
            (FF_SHOP_ID, 'offer_to_remove_basic_but_disabled_by_stock_force_mode'),
            (DSBS_SHOP_ID, 'offer_to_remove_service'),
            (FF_SHOP_ID, 'offer_to_remove_service'),
            (DSBS_SHOP_ID, 'offer_to_remove_service_but_disabled_by_stock'),
            (FF_SHOP_ID, 'offer_to_remove_service_but_disabled_by_stock'),
            (DSBS_SHOP_ID, 'offer_to_remove_service_but_disabled_by_stock_force_mode'),
            (FF_SHOP_ID, 'offer_to_remove_service_but_disabled_by_stock_force_mode'),
        ]
    ]


@pytest.fixture(scope='module')
def actual_service_offers(color_name):
    return [
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id=make_offer_id(offer_id, proto_body_mode),
                shop_id=shop_id,
                warehouse_id=warehouse_id,
            ),
            meta=create_meta(10, scope=DTC.SERVICE),
            stock_info=DTC.OfferStockInfo(
                market_stocks=DTC.OfferStocks(
                    count=stock,
                )
            ) if stock is not None else None,
            status=DTC.OfferStatus(
                disabled=[DTC.Flag(
                    flag=False,
                    meta=create_update_meta(current_ts.seconds, DTC.MARKET_STOCK),
                )]
            ) if is_disabled is not None else None
        ))
        for proto_body_mode in REQUEST_MODE for shop_id, offer_id, warehouse_id, stock, is_disabled in [
            (DSBS_SHOP_ID, 'offer_to_remove_basic', DSBS_WAREHOUSE_ID, None, None),
            (FF_SHOP_ID, 'offer_to_remove_basic', FF_WAREHOUSE_ID_145, None, None),
            (FF_SHOP_ID, 'offer_to_remove_basic', FF_WAREHOUSE_ID_172, None, None),
            (DSBS_SHOP_ID, 'offer_to_remove_basic_but_disabled_by_stock', DSBS_WAREHOUSE_ID, None, True),
            (FF_SHOP_ID, 'offer_to_remove_basic_but_disabled_by_stock', FF_WAREHOUSE_ID_145, None, True),
            (FF_SHOP_ID, 'offer_to_remove_basic_but_disabled_by_stock', FF_WAREHOUSE_ID_172, 1, True),
            (DSBS_SHOP_ID, 'offer_to_remove_basic_but_disabled_by_stock_force_mode', DSBS_WAREHOUSE_ID, None, True),
            (FF_SHOP_ID, 'offer_to_remove_basic_but_disabled_by_stock_force_mode', FF_WAREHOUSE_ID_145, None, True),
            (FF_SHOP_ID, 'offer_to_remove_basic_but_disabled_by_stock_force_mode', FF_WAREHOUSE_ID_172, 1, True),
            (DSBS_SHOP_ID, 'offer_to_remove_service', DSBS_WAREHOUSE_ID, None, None),
            (FF_SHOP_ID, 'offer_to_remove_service', FF_WAREHOUSE_ID_145, None, None),
            (FF_SHOP_ID, 'offer_to_remove_service', FF_WAREHOUSE_ID_172, None, None),
            (DSBS_SHOP_ID, 'offer_to_remove_service_but_disabled_by_stock', DSBS_WAREHOUSE_ID, None, True),
            (FF_SHOP_ID, 'offer_to_remove_service_but_disabled_by_stock', FF_WAREHOUSE_ID_145, None, True),
            (FF_SHOP_ID, 'offer_to_remove_service_but_disabled_by_stock', FF_WAREHOUSE_ID_172, 1, True),
            (DSBS_SHOP_ID, 'offer_to_remove_service_but_disabled_by_stock_force_mode', DSBS_WAREHOUSE_ID, None, True),
            (FF_SHOP_ID, 'offer_to_remove_service_but_disabled_by_stock_force_mode', FF_WAREHOUSE_ID_145, None, True),
            (FF_SHOP_ID, 'offer_to_remove_service_but_disabled_by_stock_force_mode', FF_WAREHOUSE_ID_172, 1, True),
        ]
    ]


@pytest.yield_fixture(scope='module')
def stroller(
        config,
        yt_server,
        log_broker_stuff,
        partners_table,
        basic_offers_table,
        service_offers_table,
        actual_service_offers_table,
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


def make_proto_body(offer_id):
    return SyncApi.DeleteOfferRequest(
        offer_ids=[offer_id]
    ).SerializeToString()


@pytest.mark.parametrize('business_id, offer_id, request_path', [
    (
        BUSINESS_ID,
        'offer_to_remove_basic',
        '/v1/partners/{business_id}/offers/basic/remove?{offer_id}'
    ),
    (
        # Если у одной из складских частей есть сток, то united-оффер можно удалить в force-режиме
        BUSINESS_ID,
        'offer_to_remove_basic_but_disabled_by_stock_force_mode',
        '/v1/partners/{business_id}/offers/basic/remove?force=1&{offer_id}'
    ),
], ids=['no_stock', 'force_remove_with_stock'])
def test_delete_basic(stroller, business_id, offer_id, request_path, proto_body_mode):
    """ Проверка ручки удаления всего united-оффер """
    offer_id = make_offer_id(offer_id, proto_body_mode)
    response = stroller.post(
        request_path.format(
            business_id=business_id,
            offer_id='' if proto_body_mode else 'offer_id={}'.format(offer_id)
        ),
        data=make_proto_body(offer_id) if proto_body_mode else None
    )
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(SyncApi.DeleteOfferResponse, {
        'result': IsProtobufMap({
            offer_id: {
                'status': SyncApi.DeleteOfferResponse.SUCCESS
            }
        }),
    }))
    assert_that(stroller.basic_offers_table.data, HasDatacampYtUnitedOffersRows([{
        'business_id': business_id,
        'shop_sku': offer_id,
        'state_flags': 1,
    }]))
    assert_that(stroller.service_offers_table.data, HasOffers([message_from_data({
        'identifiers': {
            'business_id': business_id,
            'shop_id': shop_id,
            'offer_id': offer_id,
        },
        'status': {
            'removed': {
                'flag': True,
            },
            'disabled': [{
                'flag': True,
                'meta': {
                    'source': DTC.PUSH_PARTNER_OFFICE
                }
            }]
        }
    }, DTC.Offer()) for shop_id in [DSBS_SHOP_ID, FF_SHOP_ID]]))
    assert_that(stroller.actual_service_offers_table.data, HasDatacampYtUnitedOffersRows([{
        'business_id': business_id,
        'shop_id': shop_id,
        'shop_sku': offer_id,
        'warehouse_id': warehouse_id,
        'state_flags': 1,
    } for shop_id, warehouse_id in [
        (DSBS_SHOP_ID, DSBS_WAREHOUSE_ID),
        (FF_SHOP_ID, FF_WAREHOUSE_ID_145),
        (FF_SHOP_ID, FF_WAREHOUSE_ID_172)
    ]]))


@pytest.mark.parametrize('business_id, offer_id, request_path', [
    (
        BUSINESS_ID,
        'offer_to_remove_service',
        '/v1/partners/{business_id}/offers/services/{shop_id}/remove?{offer_id}'
    ),
    (
        # Если у одной из складских частей есть сток, то сервсиный оффер можно удалить в force-режиме
        BUSINESS_ID,
        'offer_to_remove_service_but_disabled_by_stock_force_mode',
        '/v1/partners/{business_id}/offers/services/{shop_id}/remove?force=1&{offer_id}'
    ),
], ids=['no_stock', 'force_remove_with_stock'])
def test_delete_service(stroller, business_id, offer_id, request_path, proto_body_mode):
    """ Проверка ручки удаления сервсиной части united-оффер """
    offer_id = make_offer_id(offer_id, proto_body_mode)
    response = stroller.post(
        request_path.format(
            business_id=business_id,
            shop_id=FF_SHOP_ID,
            offer_id='' if proto_body_mode else 'offer_id={}'.format(offer_id)
        ),
        data=make_proto_body(offer_id) if proto_body_mode else None
    )
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(SyncApi.DeleteOfferResponse, {
        'result': IsProtobufMap({
            offer_id: {
                'status': SyncApi.DeleteOfferResponse.SUCCESS
            }
        }),
    }))
    assert_that(stroller.basic_offers_table.data, HasDatacampYtUnitedOffersRows([{
        'business_id': business_id,
        'shop_sku': offer_id,
        'state_flags': 0,
    }]))
    assert_that(stroller.service_offers_table.data, HasOffers([message_from_data({
        'identifiers': {
            'business_id': business_id,
            'shop_id': shop_id,
            'offer_id': offer_id,
        },
        'status': {
            'removed': {
                'flag': True,
            },
            'disabled': [{
                'flag': True,
                'meta': {
                    'source': DTC.PUSH_PARTNER_OFFICE
                }
            }]
        } if state_flags > 0 else None
    }, DTC.Offer()) for shop_id, state_flags in [(DSBS_SHOP_ID, 0), (FF_SHOP_ID, 1)]]))
    assert_that(stroller.actual_service_offers_table.data, HasDatacampYtUnitedOffersRows([{
        'business_id': business_id,
        'shop_id': shop_id,
        'shop_sku': offer_id,
        'warehouse_id': warehouse_id,
        'state_flags': state_flags,
    } for shop_id, warehouse_id, state_flags in [
        (DSBS_SHOP_ID, DSBS_WAREHOUSE_ID, None),
        (FF_SHOP_ID, FF_WAREHOUSE_ID_145, 1),
        (FF_SHOP_ID, FF_WAREHOUSE_ID_172, 1)
    ]]))


@pytest.mark.parametrize('business_id, offer_id, request_path', [
    (
        BUSINESS_ID,
        'offer_to_remove_basic_but_disabled_by_stock',
        '/v1/partners/{business_id}/offers/basic/remove{offer_id}'
    ),
    (
        BUSINESS_ID,
        'offer_to_remove_service_but_disabled_by_stock',
        '/v1/partners/{business_id}/offers/services/{shop_id}/remove{offer_id}'
    ),
], ids=['basic', 'service'])
def test_cant_delete_offer_disabled_by_stock(stroller, business_id, offer_id, request_path, proto_body_mode):
    """ Если у одной из складских частей есть сток, то оффер удалить невозможно """
    offer_id = make_offer_id(offer_id, proto_body_mode)
    response = stroller.post(
        request_path.format(
            business_id=business_id,
            shop_id=FF_SHOP_ID,
            offer_id='' if proto_body_mode else '?offer_id={}'.format(offer_id)
        ),
        data=make_proto_body(offer_id) if proto_body_mode else None
    )
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(SyncApi.DeleteOfferResponse, {
        'result': IsProtobufMap({
            offer_id: {
                'status': SyncApi.DeleteOfferResponse.FAILED
            }
        }),
    }))
    assert_that(stroller.basic_offers_table.data, HasDatacampYtUnitedOffersRows([{
        'business_id': business_id,
        'shop_sku': offer_id,
        'state_flags': None,
    }]))
    assert_that(stroller.service_offers_table.data, HasDatacampYtUnitedOffersRows([{
        'business_id': business_id,
        'shop_id': shop_id,
        'shop_sku': offer_id,
        'state_flags': None,
    } for shop_id in [DSBS_SHOP_ID, FF_SHOP_ID]]))
    assert_that(stroller.actual_service_offers_table.data, HasDatacampYtUnitedOffersRows([{
        'business_id': business_id,
        'shop_id': shop_id,
        'shop_sku': offer_id,
        'warehouse_id': warehouse_id,
        'state_flags': None,
    } for shop_id, warehouse_id in [
        (DSBS_SHOP_ID, DSBS_WAREHOUSE_ID),
        (FF_SHOP_ID, FF_WAREHOUSE_ID_145),
        (FF_SHOP_ID, FF_WAREHOUSE_ID_172)
    ]]))
