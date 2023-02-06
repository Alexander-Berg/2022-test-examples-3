# -*- coding: utf-8 -*-
import mock
import re
from six.moves.urllib.parse import parse_qsl
import time

import pytest

from travel.avia.library.python.tester.factories import create_partner
from travel.avia.ticket_daemon.ticket_daemon.api.redirect import fetch_redirect_data
from travel.avia.ticket_daemon.tests.partners.helper import (
    expected_variants, get_mocked_response, get_query, assert_variants_equal, SettlementMock,
)
from travel.avia.ticket_daemon.ticket_daemon.daemon.utils import BadPartnerResponse
from travel.avia.ticket_daemon.ticket_daemon.partners import aeroflot4


@mock.patch.object(aeroflot4.requests, 'post', return_value=get_mocked_response('aeroflot/aeroflot4/aeroflot.xml'))
def test_aeroflot4_query(mocked_request):
    expected = expected_variants('aeroflot/aeroflot4/aeroflot.json')
    q = get_query()
    q.station_iatas_from = ['MOW']
    q.station_iatas_to = ['SVX']
    variants = next(aeroflot4.query(q))
    assert_variants_equal(expected, variants)
    mocked_request.assert_called_once()


@mock.patch.object(aeroflot4.requests, 'post', return_value=get_mocked_response('aeroflot/aeroflot4/aeroflot_booking_info.xml'))
def test_aeroflot4_booking_info(mocked_request):
    """
    This test verifies that the truncated xml in booking_info still is a valid shopping request.
    """
    expected = expected_variants('aeroflot/aeroflot4/aeroflot_booking_info.json')
    q = get_query()
    q.station_iatas_from = ['MOW']
    q.station_iatas_to = ['SVX']
    variants = next(aeroflot4.query(q))
    assert_variants_equal(expected, variants)
    mocked_request.assert_called_once()


@mock.patch.object(aeroflot4.requests, 'post', return_value=get_mocked_response('aeroflot/aeroflot4/aeroflot_one_flight_many_fares.xml'))
def test_aeroflot4_query_one_flight_many_fares(mocked_request):
    """
    This test verifies that xml minimizing preserves not only selected offer, but also all offers with the same set of
    segments (they are considered a different fare for the same flight).
    """
    expected = expected_variants('aeroflot/aeroflot4/aeroflot_one_flight_many_fares.json')
    q = get_query()
    q.station_iatas_from = ['MOW']
    q.station_iatas_to = ['SVX']
    variants = next(aeroflot4.query(q))
    assert_variants_equal(expected, variants)
    mocked_request.assert_called_once()


@mock.patch.object(aeroflot4.requests, 'post', return_value=get_mocked_response('aeroflot/aeroflot4/aeroflot_empty_result.xml'))
def test_aeroflot4_query_empty_result(mocked_request):
    q = get_query()
    q.station_iatas_from = ['MOW']
    q.station_iatas_to = ['SVX']
    variants = next(aeroflot4.query(q))
    assert not list(variants)
    mocked_request.assert_called_once()


@mock.patch.object(aeroflot4.requests, 'post', return_value=get_mocked_response('aeroflot/aeroflot4/aeroflot_errors.xml'))
def test_aeroflot4_query_error(mocked_request):
    q = get_query()
    q.station_iatas_from = ['MOW']
    q.station_iatas_to = ['SVX']
    with pytest.raises(BadPartnerResponse, match=re.compile('Errors:')):
        next(aeroflot4.query(q))
    mocked_request.assert_called_once()


@mock.patch.object(aeroflot4.requests, 'post', return_value=get_mocked_response('aeroflot/aeroflot4/aeroflot_cyrillic_error.xml'))
def test_aeroflot4_query_cyrillic_error(mocked_request):
    q = get_query()
    q.station_iatas_from = ['MOW']
    q.station_iatas_to = ['SVX']
    with pytest.raises(BadPartnerResponse, match=re.compile('Errors:')):
        next(aeroflot4.query(q))
    mocked_request.assert_called_once()


def test_aeroflot4_generate_marker():
    marker1 = aeroflot4.generate_marker()
    time.sleep(0.001)
    marker2 = aeroflot4.generate_marker()

    assert re.match('^YA[0-9A-Z]{8}$', marker1)
    assert re.match('^YA[0-9A-Z]{8}$', marker2)
    assert marker1 != marker2


@mock.patch('travel.avia.ticket_daemon.ticket_daemon.partners.aeroflot4.generate_marker', return_value='test_marker')
@mock.patch('travel.avia.ticket_daemon.ticket_daemon.api.yaclid.YaClid.dumps', return_value='demo_yaclid')
@pytest.mark.dbuser
def test_book(mock_yaclid, mock_marker):
    mock_marker.__name__ = 'generate_marker'
    partner = create_partner(code='aeroflot', query_module_name='aeroflot4', marker='referrer')
    url = 'https://afl-test.test.aeroflot.ru/sb/app/ru-ru#/passengers?adults=1&children=1&infants=0&' \
          'segments=JFK20180801SVO.SU0123.N.NVUHA.N_SVO20180801SVX.SU1406.N.NVUHA.I-SVX20180810SVO.SU1409.N.NVUHA.N_SVO20180810JFK.SU0100.N.NVUHA.I&referrer=YandexAvia'
    redir_data = fetch_redirect_data(partner, {'url': url})

    assert redir_data['marker'] == 'test_marker'
    url_parts = redir_data['url'].split('#/passengers?')
    assert 'https://afl-test.test.aeroflot.ru/sb/app/ru-ru?yaclid=demo_yaclid' == url_parts[0]
    query = dict(parse_qsl(url_parts[1], keep_blank_values=True))
    assert query == {
        'adults': '1',
        'children': '1',
        'infants': '0',
        'segments': 'JFK20180801SVO.SU0123.N.NVUHA.N_SVO20180801SVX.SU1406.N.NVUHA.I-SVX20180810SVO.SU1409.N.NVUHA.N_SVO20180810JFK.SU0100.N.NVUHA.I',
        'referrer': 'test_markerAvia',
    }


@mock.patch.object(aeroflot4.requests, 'post', return_value=get_mocked_response('aeroflot/aeroflot4/aeroflot_round_trip.xml'))
def test_aeroflot4_query_round_trip(mocked_request):
    expected = expected_variants('aeroflot/aeroflot4/aeroflot_round_trip.json')
    q = get_query()
    q.station_iatas_from = ['LED']
    q.station_iatas_to = ['NYC']
    variants = next(aeroflot4.query(q))
    assert_variants_equal(expected, variants)
    mocked_request.assert_called_once()


@mock.patch.object(aeroflot4.requests, 'post', return_value=get_mocked_response('aeroflot/aeroflot4/aeroflot_children.xml'))
def test_aeroflot4_query_children(mocked_request):
    expected = expected_variants('aeroflot/aeroflot4/aeroflot_children.json')
    q = get_query(point_to=SettlementMock(iata='JFK', code='RU', id=555),)
    q.station_iatas_from = ['SVO']
    q.station_iatas_to = ['JFK']
    q.passengers['adults'] = 2
    variants = next(aeroflot4.query(q))
    assert_variants_equal(expected, variants)
    mocked_request.assert_called_once()
