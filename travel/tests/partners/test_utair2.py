# -*- coding: utf-8 -*-
import mock

from travel.avia.ticket_daemon.tests.partners.helper import (
    expected_variants, get_mocked_response, get_query, assert_variants_equal,
    SettlementMock
)
from travel.avia.ticket_daemon.ticket_daemon.partners import utair2


def test_utair2_query():
    with mock.patch('requests.post', return_value=get_mocked_response('utair2.xml')):
        expected = expected_variants('utair2.json')

        test_query = get_query(
            point_to=SettlementMock(iata='LED', code='RU', id=2)
        )
        variants = next(utair2.query(test_query))
        assert_variants_equal(expected, variants)


def test_baggage_utair2_query():
    with mock.patch('requests.post', return_value=get_mocked_response('utair2_baggage.xml')):
        expected = expected_variants('utair2_baggage.json')
        test_query = get_query(date_backward=None)
        variants = next(utair2.query(test_query))
        assert_variants_equal(expected, variants)
