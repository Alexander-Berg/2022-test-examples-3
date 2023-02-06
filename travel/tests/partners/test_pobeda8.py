# -*- coding: utf-8 -*-
import datetime
import mock

from travel.avia.ticket_daemon.tests.partners.helper import (
    expected_variants, get_mocked_response, get_query, assert_variants_equal, SettlementMock
)
from travel.avia.ticket_daemon.ticket_daemon.partners import pobeda8
from travel.avia.ticket_daemon.ticket_daemon.partners.pobeda8 import BundleCode


@mock.patch.object(pobeda8, 'BUNDLE_CODES', (BundleCode.BASE, BundleCode.PLUS, BundleCode.PREMIUM))
@mock.patch('requests.get', return_value=get_mocked_response('pobeda8.json'))
def test_pobeda_query(mocked_request):
    test_query = get_query(
        point_from=SettlementMock(iata='VKO', code='RU', id=3),
        point_to=SettlementMock(iata='LED', code='RU', id=2),
        date_forward=datetime.date(2020, 6, 20),
        date_backward=datetime.date(2020, 6, 26),
        passengers={'adults': 1, 'infants': 0, 'children': 0},
    )
    expected = expected_variants('pobeda8_expected.json')
    variants = next(pobeda8.query(test_query))
    assert_variants_equal(expected, variants)


@mock.patch.object(pobeda8, 'BUNDLE_CODES', (BundleCode.BASE, BundleCode.PLUS, BundleCode.PREMIUM))
@mock.patch('requests.get', return_value=get_mocked_response('pobeda8_infants.json'))
def test_pobeda_query_infants(mocked_request):
    test_query = get_query(
        point_from=SettlementMock(iata='VKO', code='RU', id=3),
        point_to=SettlementMock(iata='BGY', code='RU', id=2),
        date_forward=datetime.date(2020, 9, 20),
        date_backward=None,
        passengers={'adults': 1, 'infants': 1, 'children': 0},
    )
    expected = expected_variants('pobeda8_infants_expected.json')
    variants = next(pobeda8.query(test_query))
    assert_variants_equal(expected, variants)


@mock.patch.object(pobeda8, 'BUNDLE_CODES', (BundleCode.BASE, BundleCode.PLUS, BundleCode.PREMIUM,
                                             BundleCode.BASE_AND_BAGGAGE10, BundleCode.BASE_AND_BAGGAGE20))
@mock.patch('requests.get', return_value=get_mocked_response('pobeda8_new_deeplink.json'))
def test_pobeda_new_bundles_and_deeplink(mocked_request):
    test_query = get_query(
        point_from=SettlementMock(iata='VKO', code='RU', id=30),
        point_to=SettlementMock(iata='SVX', code='RU', id=20),
        date_forward=datetime.date(2020, 12, 24),
        date_backward=None,
        passengers={'adults': 1, 'infants': 0, 'children': 0},
    )
    expected = expected_variants('pobeda8_new_deeplink_expected.json')
    variants = next(pobeda8.query(test_query))
    assert_variants_equal(expected, variants)


@mock.patch.object(pobeda8, 'BUNDLE_CODES', (BundleCode.BASE_AND_BAGGAGE10,))
@mock.patch('requests.get', return_value=get_mocked_response('pobeda8_extended_baggage.json'))
def test_pobeda_extended_baggage(mocked_request):
    test_query = get_query(
        point_from=SettlementMock(iata='VKO', code='RU', id=77),
        point_to=SettlementMock(iata='SVX', code='RU', id=88),
        date_forward=datetime.date(2020, 2, 10),
        date_backward=None,
        passengers={'adults': 1, 'infants': 1, 'children': 1},
    )
    expected = expected_variants('pobeda8_extended_baggage_expected.json')
    variants = next(pobeda8.query(test_query))
    assert_variants_equal(expected, variants)


@mock.patch.object(pobeda8, 'BUNDLE_CODES', (BundleCode.BASE_AND_BAGGAGE10, BundleCode.BASE_AND_BAGGAGE20))
@mock.patch('requests.get', return_value=get_mocked_response('pobeda8_no_10kg_price.json'))
def test_pobeda_no_10kg_price(mocked_request):
    test_query = get_query(
        point_from=SettlementMock(iata='VKO', code='RU', id=77),
        point_to=SettlementMock(iata='SVX', code='RU', id=88),
        date_forward=datetime.date(2020, 2, 10),
        date_backward=None,
        passengers={'adults': 1, 'infants': 0, 'children': 0},
    )
    expected = expected_variants('pobeda8_no_10kg_price_expected.json')
    variants = next(pobeda8.query(test_query))
    assert_variants_equal(expected, variants)


@mock.patch.object(pobeda8, 'BUNDLE_CODES', (BundleCode.BASE,))
@mock.patch('requests.get', return_value=get_mocked_response('pobeda8_filter_route_pairs.json'))
def test_pobeda_filter_route_pairs(mocked_request):
    test_query = get_query(
        point_from=SettlementMock(iata='VKO', code='RU', id=777),
        point_to=SettlementMock(iata='AYT', code='TR', id=888),
        date_forward=datetime.date(2021, 2, 12),
        date_backward=datetime.date(2021, 2, 26),
        passengers={'adults': 1, 'infants': 0, 'children': 0},
    )
    expected = expected_variants('pobeda8_filter_route_pairs_expected.json')
    variants = next(pobeda8.query(test_query))
    assert_variants_equal(expected, variants)


def test_book():
    order_data = {
        'url': 'http://example.com/new',
    }
    expected_redir_data = 'http://example.com/new'
    assert pobeda8.book(order_data) == expected_redir_data
