# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

from datetime import datetime

import mock
import pytest

from travel.avia.price_prediction.api.v1 import check_price_pb2
from travel.avia.price_prediction.api.v1.check_price_pb2 import TCheckPriceReq, TCheckPricesReq
from travel.avia.library.python.price_prediction import PriceCategory
from travel.avia.library.python.tester.factories import create_partner
from travel.avia.library.python.ticket_daemon.memo import reset_all_caches
from travel.avia.ticket_daemon.ticket_daemon.api.result import add_price_prediction
from travel.avia.ticket_daemon.ticket_daemon.api.result.add_price_prediction import add_price_prediction_category
from travel.avia.ticket_daemon.ticket_daemon.daemon_tester import create_flight, create_query, create_variant
from travel.avia.ticket_daemon.ticket_daemon.settings.price_prediction import PRICE_PREDICTION_TIMEOUT

pytestmark = [pytest.mark.dbuser]

GOOD_CATEGORY_RESULT = {0: check_price_pb2.TPricePrediction.CATEGORY_GOOD}


@pytest.fixture(scope='function', autouse=True)
def _reset_all_caches():
    reset_all_caches()


def _create_query(**kwargs):
    return create_query(
        from_is_settlement=False,
        to_is_settlement=False,
        attach_from_settlement=False,
        attach_to_settlement=False,
        **kwargs
    )


def _create_variant(**kwargs):
    partner = kwargs.pop('partner', None) or create_partner()
    query = kwargs.pop('query', None) or _create_query()
    forward_flights = kwargs.pop('forward_flights', None) or [
        create_flight(
            station_from=query.point_from,
            station_to=query.point_to,
            local_departure=datetime(2021, 3, 24, 12, 20),
            number='DP 262',
        ),
    ]
    return create_variant(query, partner, forward_flights=forward_flights, **kwargs)


@mock.patch.object(add_price_prediction.feature_flags, 'use_price_prediction', return_value=True)
@mock.patch.object(add_price_prediction.price_prediction_client, 'check_prices', return_value=GOOD_CATEGORY_RESULT)
def test_add_price_prediction_category(m_check_price, m_use_price_prediction):
    query = _create_query()
    variants = [_create_variant(query=query, price=123, currency='RUR')]

    add_price_prediction_category(variants, query)

    assert m_check_price.call_count == 1
    m_check_price.assert_called_once_with(
        TCheckPricesReq(CheckPricesReq={
            0: TCheckPriceReq(
                PointFromKey=query.point_from.point_key,
                PointToKey=query.point_to.point_key,
                Routes='DP 262',
                LocalDeparture=1616588400,
                AdultSeats=1,
                ChildrenSeats=0,
                InfantSeats=0,
                Price=123,
            )
        }),
        timeout=PRICE_PREDICTION_TIMEOUT,
        tvm_service_ticket=None,
    )
    assert variants[0].price_category == PriceCategory.GOOD


@mock.patch.object(add_price_prediction.feature_flags, 'use_price_prediction', return_value=False)
@mock.patch.object(add_price_prediction.price_prediction_client, 'check_prices', return_value=GOOD_CATEGORY_RESULT)
def test_add_price_prediction_category_turned_off(m_check_price, m_use_price_prediction):
    query = _create_query()
    variants = [_create_variant(query=query, price=123, currency='RUR')]

    add_price_prediction_category(variants, query)

    assert not m_check_price.called


@mock.patch.object(add_price_prediction.feature_flags, 'use_price_prediction', return_value=True)
@mock.patch.object(add_price_prediction.price_prediction_client, 'check_prices', return_value=GOOD_CATEGORY_RESULT)
def test_add_price_prediction_category_skip_not_ru_version(m_check_price, m_use_price_prediction):
    query = _create_query(national_version='tr')
    variants = [_create_variant(query=query, price=123, currency='RUR')]

    add_price_prediction_category(variants, query)

    assert not m_check_price.called


@mock.patch.object(add_price_prediction.feature_flags, 'use_price_prediction', return_value=True)
@mock.patch.object(add_price_prediction.price_prediction_client, 'check_prices', return_value=GOOD_CATEGORY_RESULT)
def test_add_price_prediction_category_skip_with_backward(m_check_price, m_use_price_prediction):
    query = _create_query(return_date='2021-03-29')
    variants = [_create_variant(query=query, price=123, currency='RUR')]
    variants[0].backward.segments = variants[0].forward.segments

    add_price_prediction_category(variants, query)

    assert not m_check_price.called


@mock.patch.object(add_price_prediction.feature_flags, 'use_price_prediction', return_value=True)
@mock.patch.object(add_price_prediction.price_prediction_client, 'check_prices', return_value=GOOD_CATEGORY_RESULT)
def test_add_price_prediction_category_one_call_for_2_similar_variants(m_check_price, m_use_price_prediction):
    reset_all_caches()
    query = _create_query()
    partner = create_partner()
    variants = [
        _create_variant(query=query, partner=partner, price=123, currency='RUR'),
        _create_variant(query=query, partner=partner, price=100500, currency='RUR'),
    ]

    add_price_prediction_category(variants, query)

    assert m_check_price.call_count == 1
    m_check_price.assert_called_once_with(
        TCheckPricesReq(CheckPricesReq={
            0: TCheckPriceReq(
                PointFromKey=query.point_from.point_key,
                PointToKey=query.point_to.point_key,
                Routes='DP 262',
                LocalDeparture=1616588400,
                AdultSeats=1,
                ChildrenSeats=0,
                InfantSeats=0,
                Price=123,
            )
        }),
        timeout=PRICE_PREDICTION_TIMEOUT,
        tvm_service_ticket=None,
    )
    assert variants[0].price_category == PriceCategory.GOOD
