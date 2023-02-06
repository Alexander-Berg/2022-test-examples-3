# -*- coding: utf-8 -*-
import mock
from lxml import etree

import pytest

from travel.avia.ticket_daemon.tests.partners.helper import (
    get_mocked_response, get_query, assert_variants_equal,
    expected_variants
)
from travel.avia.ticket_daemon.ticket_daemon.partners import citytravel


@mock.patch('requests.get', return_value=get_mocked_response('citytravel.xml'))
def test_citytravel_query(mocked_request):
    expected = expected_variants('citytravel.json')
    test_query = get_query()
    variants = next(citytravel.query(test_query))
    assert_variants_equal(expected, variants)


@mock.patch('requests.get', return_value=get_mocked_response('citytravel_fare_codes.xml'))
def test_citytravel_query_fare_codes(mocked_request):
    expected = expected_variants('citytravel_fare_codes_result.json')
    test_query = get_query()
    variants = next(citytravel.query(test_query))
    assert_variants_equal(expected, variants)


@pytest.mark.parametrize('flight_tag,expected_baggage', [
    ('<luggage_weight>10</luggage_weight><luggage>true</luggage>', '1pc 10kg'),
    ('<pieces_of_luggage>2</pieces_of_luggage><luggage_weight>15</luggage_weight><luggage>true</luggage>', '2pc 15kg'),
    ('<pieces_of_luggage>2</pieces_of_luggage><luggage>true</luggage>', '2pc'),
    ('<luggage>false</luggage>', '0pc'),
    ('', 'None')
])
def test_get_baggage(flight_tag, expected_baggage):

    f_tag = etree.fromstring('<flight>%s</flight>' % flight_tag)
    baggage = citytravel.get_baggage(f_tag)
    assert str(baggage) == expected_baggage
