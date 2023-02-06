# -*- coding: utf-8 -*-
from datetime import date

import mock

from travel.avia.ticket_daemon.ticket_daemon.partners import aegean
from travel.avia.ticket_daemon.tests.partners.helper import (
    get_mocked_response, get_query, assert_variants_equal,
    expected_variants, SettlementMock
)


@mock.patch('requests.get', return_value=get_mocked_response('aegean_roundtrip.json'))
def test_aegean_return_query(*mocks):
    expected = expected_variants('aegean_roundtrip_expected.json')
    test_query = get_query(
        point_from=SettlementMock(iata='TLV', code='RU', id=39),
        point_to=SettlementMock(iata='MAN', code='RU', id=38),
        date_forward=date(2019, 11, 9),
        date_backward=date(2019, 11, 19),
    )
    variants = next(aegean.query(test_query))
    assert_variants_equal(expected, variants)


@mock.patch('requests.get', return_value=get_mocked_response('aegean_oneway.json'))
def test_aegean_one_way_query(*mocks):
    expected = expected_variants('aegean_oneway_expected.json')
    test_query = get_query(
        point_from=SettlementMock(iata='MAD', code='RU', id=39),
        point_to=SettlementMock(iata='DME', code='RU', id=38),
        date_forward=date(2019, 9, 27),
        date_backward=None
    )
    variants = next(aegean.query(test_query))
    assert_variants_equal(expected, variants)
