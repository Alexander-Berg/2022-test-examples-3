# coding: utf-8

import pytest
from hamcrest import assert_that, equal_to

from market.idx.datacamp.yatf.utils import dict2tskv

from market.idx.datacamp.controllers.stroller.yatf.test_env import make_stroller
from market.idx.datacamp.controllers.stroller.yatf.utils import (
    gen_basic_row,
    gen_service_row,
    gen_actual_service_row,
)
from market.idx.datacamp.proto.api.SyncChangeOffer_pb2 import ChangeOfferRequest, FullOfferResponse
from market.idx.datacamp.yatf.matchers.matchers import HasStatus
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf, ListWithNeededLength

BUSINESS_ID = 1
WAREHOUSE_ID = 0


DATACAMP_TABLE_DATA = [
    (1, "T100", [['active_promo', False]], 10),
    (1, "T200", [], 10),
    (1, "T300", [], 10),
    (2, "T100", [], 10),
]


@pytest.fixture(scope='module')
def basic_offers():
    return [gen_basic_row(BUSINESS_ID, offer_id, ts)
            for shop_id, offer_id, _, ts in DATACAMP_TABLE_DATA]


@pytest.fixture(scope='module')
def service_offers():
    return [gen_service_row(BUSINESS_ID, shop_id, offer_id, ts)
            for shop_id, offer_id, _, ts in DATACAMP_TABLE_DATA]


@pytest.fixture(scope='module')
def actual_service_offers():
    return [gen_actual_service_row(BUSINESS_ID, shop_id, WAREHOUSE_ID, offer_id, promos, ts)
            for shop_id, offer_id, promos, ts in DATACAMP_TABLE_DATA]


@pytest.fixture(scope='module')
def partners():
    return [
        {
            'shop_id': 1,
            'mbi': '\n\n'.join([
                dict2tskv({'shop_id': 1, 'business_id': BUSINESS_ID}),
            ])
        },
        {
            'shop_id': 2,
            'mbi': '\n\n'.join([
                dict2tskv({'shop_id': 2, 'business_id': BUSINESS_ID}),
            ])
        },
    ]


@pytest.yield_fixture(scope='module')
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


def request(client, shop_id, offer_ids, color_name):
    request = ChangeOfferRequest()
    for offer_id in offer_ids:
        offer = request.offer.add()
        offer.identifiers.offer_id = offer_id
        if color_name == 'blue':
            offer.identifiers.warehouse_id = 0
        offer.price.basic.meta.timestamp.GetCurrentTime()

    return client.get('/shops/{}/offers'.format(shop_id), data=request.SerializeToString())


def test_get_offers(stroller, color_name):
    """Тест проверяет, что ручка возвращает только запрошенные оффера"""
    shop_id = 1
    offer_ids = ['T100', 'T300']
    expected_resp = {
        'offer': [
            {
                'identifiers': {
                    'shop_id': 1,
                    'offer_id': 'T100',
                },
                'promos': {  # проверка фильтрации неактивных промо
                    'promo': ListWithNeededLength([
                        {
                            'id': 'active_promo'
                        }
                    ])
                }
            },
            {
                'identifiers': {
                    'shop_id': 1,
                    'offer_id': 'T300',
                },
            }
        ],
    }

    response = request(stroller, shop_id=shop_id, offer_ids=offer_ids, color_name=color_name)
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(FullOfferResponse, expected_resp))
    assert_that(response.headers['Content-type'], equal_to('application/x-protobuf'))


def test_success_is_not_only_for_all_offers(stroller, color_name):
    """Тест проверяет, что если некоторые оффера не находятся, то вернется 200,
    но все потерянные оффера кладутся в определенное поле ответа"""
    shop_id = 1
    offer_ids = ['T100', 'T300', 'UnknownOffer']
    expected_resp = {
        'offer': [
            {
                'identifiers': {
                    'shop_id': 1,
                    'offer_id': 'T100',
                },
            },
        ],
        'missed_offer': [
            {
                'identifiers': {
                    'shop_id': 1,
                    'offer_id': 'UnknownOffer',
                },
            },
        ]
    }

    response = request(stroller, shop_id=shop_id, offer_ids=offer_ids, color_name=color_name)
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(FullOfferResponse, expected_resp))
