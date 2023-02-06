# coding: utf-8

import pytest
from hamcrest import assert_that, equal_to, not_, has_length

from market.idx.datacamp.controllers.stroller.yatf.test_env import make_stroller
from market.idx.datacamp.controllers.stroller.yatf.utils import (
    gen_promo_row
)
from market.idx.datacamp.proto.api.SyncGetPromo_pb2 import DeletePromoBatchRequest, DeletePromoBatchResponse
from market.idx.datacamp.proto.promo.Promo_pb2 import PromoType, PromoDescription
from market.idx.datacamp.yatf.matchers.matchers import HasStatus
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf
from market.idx.yatf.matchers.yt_rows_matchers import HasDatacampPromoRows
from market.proto.common.promo_pb2 import ESourceType

# promo_id (str), business_id (int), source (ESourceType), partners ([int])
DATACAMP_PROMO_DATA = [
    ('FirstPromo',   10, ESourceType.ANAPLAN,         [1, 2, 3]),
    ('SecondPromo',  12, ESourceType.PARTNER_SOURCE,  [1]),
    ('ThirdPromo',   11, ESourceType.ANAPLAN,         [2, 3]),
    ('FourthPromo',  10, ESourceType.PARTNER_SOURCE,  [3]),
    ('FifthPromo',   11, ESourceType.PARTNER_SOURCE,  [1, 2]),
]


def get_promo(promo_id, business_id, source, partners):
    return {
        'primary_key': {
            'promo_id': promo_id,
            'business_id': business_id,
            'source': source
        },
        'constraints': {
            'offers_matching_rules': [{
                'supplier_restriction': {
                    'suppliers': {
                        'id': partners
                    }
                }
            }]
        }
    }

FIRST_PROMO = get_promo('FirstPromo', 10, ESourceType.ANAPLAN, [1, 2, 3])
SECOND_PROMO = get_promo('SecondPromo', 12, ESourceType.PARTNER_SOURCE, [1])
THIRD_PROMO = get_promo('ThirdPromo', 11, ESourceType.ANAPLAN, [2, 3])
FOURTH_PROMO = get_promo('FourthPromo', 10, ESourceType.PARTNER_SOURCE, [3])
FIFTH_PROMO = get_promo('FifthPromo', 11, ESourceType.PARTNER_SOURCE, [1, 2])


@pytest.fixture(scope='module')
def promo():
    return [gen_promo_row(promo_id=promo_id,
                          business_id=business_id,
                          source=source,
                          promo_type=PromoType.MARKET_BONUS,
                          ts=10,
                          partners=partners)
            for promo_id, business_id, source, partners in DATACAMP_PROMO_DATA]


@pytest.yield_fixture(scope='module')
def stroller(
        config,
        yt_server,
        log_broker_stuff,
        promo_table
):
    with make_stroller(
            config,
            yt_server,
            log_broker_stuff,
            promo_table=promo_table
    ) as stroller_env:
        yield stroller_env


def test_delete_promo_without_partner(stroller):
    request = DeletePromoBatchRequest()

    # удалим акцию, если в запросе партнер не передан
    entry0 = request.identifiers.add()
    entry0.primary_key.promo_id = 'FirstPromo'
    entry0.primary_key.business_id = 10
    entry0.primary_key.source = ESourceType.ANAPLAN

    len_before_request = len(stroller.promo_table.data)

    response = stroller.post('/v1/promo/delete', data=request.SerializeToString())

    len_after_request = len(stroller.promo_table.data)
    assert_that(len_before_request - len_after_request, equal_to(1))

    expected_removed = [FIRST_PROMO]
    expected_promo_table = [SECOND_PROMO, THIRD_PROMO, FOURTH_PROMO, FIFTH_PROMO]

    for promo in expected_removed:
        assert_that(stroller.promo_table.data, not_(HasDatacampPromoRows([
            {
                'promo': IsSerializedProtobuf(PromoDescription, promo),
            }
        ])))

    for promo in expected_promo_table:
        assert_that(stroller.promo_table.data, (HasDatacampPromoRows([
            {
                'promo': IsSerializedProtobuf(PromoDescription, promo),
            }
        ])))

    assert_that(response, HasStatus(200))
    assert_that(len(response.data), equal_to(0))


def test_delete_promos_with_partner(stroller):
    request = DeletePromoBatchRequest()

    # удалим акцию, если в списке партнеров ровно один партнер - партнер из запроса
    entry0 = request.identifiers.add()
    entry0.primary_key.promo_id = 'SecondPromo'
    entry0.primary_key.business_id = 12
    entry0.primary_key.source = ESourceType.PARTNER_SOURCE
    entry0.partner_id = 1

    # не удалим акцию, если в списке партнеров несколько партнеров (партнер из запроса есть в списке)
    entry1 = request.identifiers.add()
    entry1.primary_key.promo_id = 'ThirdPromo'
    entry1.primary_key.business_id = 11
    entry1.primary_key.source = ESourceType.ANAPLAN
    entry1.partner_id = 2

    # не удалим акцию, если партнера из запроса нет в списке партнеров данной акции
    entry2 = request.identifiers.add()
    entry2.primary_key.promo_id = 'FourthPromo'
    entry2.primary_key.business_id = 10
    entry2.primary_key.source = ESourceType.PARTNER_SOURCE
    entry2.partner_id = 1

    len_before_request = len(stroller.promo_table.data)

    response = stroller.post('/v1/promo/delete', data=request.SerializeToString())

    len_after_request = len(stroller.promo_table.data)
    assert_that(len_before_request - len_after_request, equal_to(1))

    # check response = 2 not deleted promo
    assert_that(response.data, IsSerializedProtobuf(DeletePromoBatchResponse, {
        'identifiers': has_length(2)
    }))
    assert_that(response.data, IsSerializedProtobuf(DeletePromoBatchResponse, {
        'identifiers': [
            {
                'primary_key': {
                    'business_id': 11,
                    'source': ESourceType.ANAPLAN,
                    'promo_id': 'ThirdPromo'
                },
                'partner_id': 2,
            },
            {
                'primary_key': {
                    'business_id': 10,
                    'source': ESourceType.PARTNER_SOURCE,
                    'promo_id': 'FourthPromo'
                },
                'partner_id': 1,
            }
        ]
    }))

    expected_removed = [SECOND_PROMO]
    expected_promo_table = [THIRD_PROMO, FOURTH_PROMO, FIFTH_PROMO]

    for promo in expected_removed:
        assert_that(stroller.promo_table.data, not_(HasDatacampPromoRows([
            {
                'promo': IsSerializedProtobuf(PromoDescription, promo),
            }
        ])))

    for promo in expected_promo_table:
        assert_that(stroller.promo_table.data, (HasDatacampPromoRows([
            {
                'promo': IsSerializedProtobuf(PromoDescription, promo),
            }
        ])))

    assert_that(response, HasStatus(200))

    assert_that(response.data, IsSerializedProtobuf(DeletePromoBatchResponse, {
        'identifiers': has_length(2)
    }))
    assert_that(response.data, IsSerializedProtobuf(DeletePromoBatchResponse, {
        'identifiers': [
            {
                'primary_key': {
                    'business_id': 11,
                    'source': ESourceType.ANAPLAN,
                    'promo_id': 'ThirdPromo'
                },
                'partner_id': 2
            },
            {
                'primary_key': {
                    'business_id': 10,
                    'source': ESourceType.PARTNER_SOURCE,
                    'promo_id': 'FourthPromo'
                },
                'partner_id': 1
            },
        ]
    }))


def test_delete_non_exist_promo(stroller):
    request = DeletePromoBatchRequest()

    # не удалим акцию, если по данному ключу нет акций в таблице
    entry0 = request.identifiers.add()
    entry0.primary_key.promo_id = 'NonExistPromo'
    entry0.primary_key.business_id = 12
    entry0.primary_key.source = ESourceType.ANAPLAN
    entry0.partner_id = 2

    len_before_request = len(stroller.promo_table.data)

    response = stroller.post('/v1/promo/delete', data=request.SerializeToString())

    len_after_request = len(stroller.promo_table.data)
    assert_that(len_before_request - len_after_request, equal_to(0))

    expected_promo_table = [THIRD_PROMO, FOURTH_PROMO, FIFTH_PROMO]

    for promo in expected_promo_table:
        assert_that(stroller.promo_table.data, (HasDatacampPromoRows([
            {
                'promo': IsSerializedProtobuf(PromoDescription, promo),
            }
        ])))

    assert_that(response, HasStatus(200))

    assert_that(response.data, IsSerializedProtobuf(DeletePromoBatchResponse, {
        'identifiers': has_length(1)
    }))
    assert_that(response.data, IsSerializedProtobuf(DeletePromoBatchResponse, {
        'identifiers': [
            {
                'primary_key': {
                    'business_id': 12,
                    'source': ESourceType.ANAPLAN,
                    'promo_id': 'NonExistPromo'
                },
                'partner_id': 2
            },
        ]
    }))
