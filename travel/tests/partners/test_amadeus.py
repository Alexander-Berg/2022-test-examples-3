# -*- coding: utf-8 -*-
import datetime

import mock

from travel.avia.ticket_daemon.tests.partners.helper import (
    expected_variants, get_mocked_response, get_query, assert_variants_equal, SettlementMock
)
from travel.avia.ticket_daemon.ticket_daemon.partners import amadeus


@mock.patch('requests.get', return_value=get_mocked_response('amadeus.json'))
@mock.patch('travel.avia.ticket_daemon.ticket_daemon.partners.amadeus.build_search_params', return_value=[{}])
def test_amadeus_query(mocked_request, mocked_build_search_params):
    test_query = get_query(
        point_from=SettlementMock(iata='SVO', code='RU', id=3),
        point_to=SettlementMock(iata='BCN', code='RU', id=2),
        date_forward=datetime.date(2019, 4, 15),
        date_backward=datetime.date(2019, 4, 17),
    )
    expected = expected_variants('amadeus_expected.json')
    variants = next(amadeus.query(test_query))
    assert_variants_equal(expected, variants)
