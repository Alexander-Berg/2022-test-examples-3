# coding: utf-8

import pytest
from hamcrest import assert_that, equal_to

from market.idx.datacamp.proto.api.SyncChangeOffer_pb2 import FullOfferResponse
from market.idx.datacamp.yatf.matchers.matchers import HasStatus
from market.idx.datacamp.controllers.stroller.yatf.test_env import make_stroller
from market.idx.datacamp.controllers.stroller.yatf.utils import gen_service_row
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf


@pytest.fixture(scope='module')
def service_offers():
    return [
        gen_service_row(1, 1, 'T1000', 10)
    ]


@pytest.yield_fixture(scope='module')
def stroller(
    config,
    yt_server,
    log_broker_stuff,
    service_offers_table,
):
    with make_stroller(
        config,
        yt_server,
        log_broker_stuff,
        service_offers_table=service_offers_table,
    ) as stroller_env:
        yield stroller_env


def do_request_get_offers(client, shop_id, offer_id, warehouse_id, business_id):
    return client.get('/shops/{}/offers?offer_id={}&warehouse_id={}&business_id={}'.format(
        shop_id, offer_id, warehouse_id, business_id
    ))


def test_get_offers(stroller):
    """Тест проверяет, что ручка возвращает найденный оффер"""
    business_id = 1
    shop_id = 1
    offer_id = 'T1000'
    warehouse_id = 100

    expected_resp = {
        'offer': [
            {
                'identifiers': {
                    'shop_id': 1,
                    'offer_id': 'T1000',
                    'warehouse_id': 100,
                },
            },
        ],
    }

    response = do_request_get_offers(
        stroller,
        shop_id=shop_id,
        offer_id=offer_id,
        warehouse_id=warehouse_id,
        business_id=business_id
    )
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(FullOfferResponse, expected_resp))
    assert_that(response.headers['Content-type'], equal_to('application/x-protobuf'))


def test_not_found_offer(stroller):
    """Тест проверяет, что если офера нет в хранилище, то вернется 404"""
    business_id = 1
    shop_id = 1
    offer_id = 'T1001'
    warehouse_id = 100

    response = do_request_get_offers(
        stroller,
        shop_id=shop_id,
        offer_id=offer_id,
        warehouse_id=warehouse_id,
        business_id=business_id
    )
    assert_that(response, HasStatus(404))
