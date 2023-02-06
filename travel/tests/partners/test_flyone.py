# -*- coding: utf-8 -*-
from datetime import date
import mock
from six.moves.urllib.parse import urlparse, parse_qsl

import pytest

from travel.avia.library.python.tester.factories import create_partner
from travel.avia.ticket_daemon.ticket_daemon.api.redirect import fetch_redirect_data
from travel.avia.ticket_daemon.ticket_daemon.partners import flyone
from travel.avia.ticket_daemon.tests.partners.helper import (
    get_mocked_response, get_query, assert_variants_equal, expected_variants, SettlementMock,
)


@mock.patch('travel.avia.ticket_daemon.ticket_daemon.partners.flyone.get_token', return_value='token')
@mock.patch('requests.post', return_value=get_mocked_response('flyone_oneway.json'))
def test_flyone_one_way_query(*mocks):
    expected = expected_variants('flyone_oneway_expected.json')
    test_query = get_query(
        point_from=SettlementMock(iata='KIV', code='MD', id=10313),
        point_to=SettlementMock(iata='VKO', code='RU', id=213),
        date_forward=date(2021, 6, 18),
        date_backward=None,
    )
    variants = next(flyone.query(test_query))

    assert_variants_equal(expected, variants)


@mock.patch('travel.avia.ticket_daemon.ticket_daemon.partners.flyone.get_token', return_value='token')
@mock.patch('requests.post', return_value=get_mocked_response('flyone_roundtrip.json'))
def test_flyone_return_query(*mocks):
    expected = expected_variants('flyone_roundtrip_expected.json')
    test_query = get_query(
        point_from=SettlementMock(iata='VKO', code='RU', id=213),
        point_to=SettlementMock(iata='CDG', code='FR', id=11111),
        date_forward=date(2021, 6, 18),
        date_backward=date(2021, 6, 28)
    )
    variants = next(flyone.query(test_query))

    assert_variants_equal(expected, variants)


def test_book():
    order_data = {
        'url': 'https://bookings.flyone.eu/FlightResult?depCity=KIV&arrCity=VKO&startDate=18-Jun-2021&adult=1&child=0&infant=0&ob=5F135-ST'  # noqa
    }
    test_marker = 'testmarker'
    with mock.patch('travel.avia.ticket_daemon.ticket_daemon.partners.flyone._generate_alphanumeric_marker', return_value=test_marker):
        redir_data = flyone.book(order_data)
    assert order_data['url'] in redir_data['url']


@mock.patch('travel.avia.ticket_daemon.ticket_daemon.partners.flyone._generate_alphanumeric_marker', return_value='testmarker')
@mock.patch('travel.avia.ticket_daemon.ticket_daemon.api.yaclid.YaClid.dumps', return_value='demo_yaclid')
@pytest.mark.dbuser
def test_complete_book(mock_yaclid, mock_marker):
    partner = create_partner(code='flyone', query_module_name='flyone', marker='clickId')
    url = 'https://bookings.flyone.eu/FlightResult?depCity=KIV&arrCity=VKO&startDate=18-Jun-2021&adult=1&child=0&infant=0&ob=5F135-ST'
    redir_data = fetch_redirect_data(partner, {'url': url})

    assert redir_data['marker'] == '1002-testmarker'

    url = urlparse(redir_data['url'])
    assert 'https' == url.scheme
    assert 'bookings.flyone.eu' == url.netloc
    assert '/FlightResult' == url.path

    query = dict(parse_qsl(url.query, keep_blank_values=True))
    assert query == {
        'clickId': '1002-testmarker',
        'startDate': '18-Jun-2021',
        'infant': '0',
        'yaclid': 'demo_yaclid',
        'ob': '5F135-ST',
        'arrCity': 'VKO',
        'depCity': 'KIV',
        'child': '0',
        'adult': '1',
    }
