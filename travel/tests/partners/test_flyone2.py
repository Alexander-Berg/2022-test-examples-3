# -*- coding: utf-8 -*-
from datetime import date

import mock

from travel.avia.ticket_daemon.ticket_daemon.partners import flyone2
from travel.avia.ticket_daemon.tests.partners.helper import (
    get_mocked_response, get_query, assert_variants_equal,
    expected_variants, SettlementMock
)


@mock.patch('travel.avia.ticket_daemon.ticket_daemon.partners.flyone2.get_token', return_value='token')
@mock.patch('requests.post', return_value=get_mocked_response('flyone2_oneway.json'))
def test_flyone2_one_way_query(*mocks):
    expected = expected_variants('flyone2_oneway_expected.json')
    test_query = get_query(
        point_from=SettlementMock(iata='KIV', code='MD', id=10313),
        point_to=SettlementMock(iata='VKO', code='RU', id=213),
        date_forward=date(2021, 6, 18),
        date_backward=None,
    )
    variants = next(flyone2.query(test_query))

    assert_variants_equal(expected, variants)


@mock.patch('travel.avia.ticket_daemon.ticket_daemon.partners.flyone2.get_token', return_value='token')
@mock.patch('requests.post', return_value=get_mocked_response('flyone2_roundtrip.json'))
def test_flyone2_return_query(*mocks):
    expected = expected_variants('flyone2_roundtrip_expected.json')
    test_query = get_query(
        point_from=SettlementMock(iata='VKO', code='RU', id=213),
        point_to=SettlementMock(iata='CDG', code='FR', id=11111),
        date_forward=date(2021, 6, 18),
        date_backward=date(2021, 6, 28)
    )
    variants = next(flyone2.query(test_query))

    assert_variants_equal(expected, variants)


def test_book():
    order_data = {
        'url': 'https://bookings.flyone.eu/FlightResult?depCity=KIV&arrCity=VKO&startDate=18-Jun-2021&adult=1&child=0&infant=0&ob=5F135-ST'  # noqa
    }
    test_marker = 'testmarker'
    with mock.patch('travel.avia.ticket_daemon.ticket_daemon.partners.flyone2._generate_alphanumeric_marker', return_value=test_marker):
        redir_data = flyone2.book(order_data)
    assert '1002-testmarker' == redir_data['marker']
    assert order_data['url'] in redir_data['url']
    assert '&clickid=1002-testmarker' in redir_data['url']
