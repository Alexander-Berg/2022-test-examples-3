# -*- coding: utf-8 -*-
import datetime
import mock

import pytest

from travel.avia.ticket_daemon.tests.partners.helper import (
    expected_variants, get_mocked_response, get_query, assert_variants_equal,
    SettlementMock
)
from travel.avia.ticket_daemon.ticket_daemon.partners import onetwotrip32
from travel.avia.library.python.tester.factories import create_partner


@mock.patch('requests.get', return_value=get_mocked_response('onetwotrip32.json'))
@pytest.mark.dbuser
def test_onetwotrip_query(mocked_request):
    expected = expected_variants('onetwotrip32_expected.json')
    test_query = get_query(
        point_to=SettlementMock(iata='PAR', code='FR', id=2),
        date_forward=datetime.date(2018, 11, 23),
        date_backward=None,
    )
    test_query.importer.partners.append(create_partner())
    variants = next(onetwotrip32.query(test_query))
    assert_variants_equal(expected, variants)


@mock.patch('requests.get', return_value=get_mocked_response('onetwotrip32_bad_baggage.json'))
@pytest.mark.dbuser
def test_onetwotrip_query_bad_baggage(mocked_request):
    expected = expected_variants('onetwotrip32_bad_baggage_expected.json')
    test_query = get_query(
        point_to=SettlementMock(iata='PAR', code='FR', id=2),
        date_forward=datetime.date(2018, 11, 23),
        date_backward=None,
    )
    test_query.importer.partners.append(create_partner())
    variants = next(onetwotrip32.query(test_query))
    assert_variants_equal(expected, variants)
