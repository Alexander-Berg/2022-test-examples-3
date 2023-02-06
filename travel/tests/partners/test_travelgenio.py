# -*- coding: utf-8 -*-
from datetime import date
import mock

from travel.avia.ticket_daemon.tests.partners.helper import (
    expected_variants, get_mocked_response, get_query, assert_variants_equal,
    SettlementMock)
from travel.avia.ticket_daemon.ticket_daemon.partners import travelgenio


@mock.patch('requests.get', return_value=get_mocked_response('travelgenio_one_way.xml'))
def test_travelgenio_one_way_query(mocked_request):
    expected = expected_variants('travelgenio_one_way.json')
    test_query = get_query(
        date_backward=None,
        date_forward=date(2017, 9, 1),
        point_from=SettlementMock(iata='SVX', code='RU', id=54),
        point_to=SettlementMock(iata='MOW', code='RU', id=213),
    )
    variants = next(travelgenio.query(test_query))
    assert_variants_equal(expected, variants)


@mock.patch('requests.get', return_value=get_mocked_response('travelgenio_return.xml'))
def test_travelgenio_return_query(mocked_request):
    expected = expected_variants('travelgenio_return.json')
    test_query = get_query(
        date_backward=date(2017, 9, 10),
        date_forward=date(2017, 9, 1),
        point_from=SettlementMock(iata='SVX', code='RU', id=54),
        point_to=SettlementMock(iata='MOW', code='RU', id=213),
    )
    variants = next(travelgenio.query(test_query))
    assert_variants_equal(expected, variants)


@mock.patch('requests.get', return_value=get_mocked_response('travelgenio_empty.xml'))
def test_travelgenio_with_empty_answer(mocked_request):
    variants = next(travelgenio.query(get_query()))
    assert not variants.variants
