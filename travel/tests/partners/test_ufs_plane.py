# -*- coding: utf-8 -*-
import mock
import pytest
from lxml import etree

from travel.avia.ticket_daemon.tests.partners.helper import (
    get_mocked_response, get_query, assert_variants_equal, expected_variants)
from travel.avia.ticket_daemon.ticket_daemon.partners import ufs_plane2


@mock.patch('requests.get', return_value=get_mocked_response('ufs_plane2-one_way.xml'))
def test_ufs_plane2_one_way_query(mocked_request):
    expected = expected_variants('ufs_plane2-one_way.json')
    test_query = get_query()
    variants = next(ufs_plane2.query(test_query))
    assert_variants_equal(expected, variants)


@pytest.mark.parametrize('route_tag,fare_tag,expected_baggage', [
    ('<route_forward baggage="1PC"/>', '<fare luggage="True" pieces_of_luggage="1" luggage_weight="1PC"/>', '1pc'),
    ('<route_forward baggage="1X23 KG"/>', '<fare luggage="True" pieces_of_luggage="1" luggage_weight="1X23 KG"/>', '1pc 23kg'),
    ('<route_forward baggage="0 PC"/>', '<fare luggage="False"/>', '0pc'),
    ('<route_forward baggage="NO"/>', '<fare luggage="False"/>', '0pc'),
    ('<route_forward baggage="10 KG"/>', '<fare luggage="True" pieces_of_luggage="1" luggage_weight="10 KG"/>', '1pc 10kg'),
    ('<route_forward baggage="1PC"/>', '<fare luggage="True" pieces_of_luggage="1" luggage_weight="10 KG"/>', '1pc 10kg'),
    ('<route_forward baggage="БЕЗ БАГАЖА"/>', '<fare luggage="False"/>', '0pc'),
])
def test_get_baggage(route_tag, fare_tag, expected_baggage):
    baggage = ufs_plane2.get_baggage(etree.fromstring(route_tag), etree.fromstring(fare_tag))
    assert str(baggage) == expected_baggage
