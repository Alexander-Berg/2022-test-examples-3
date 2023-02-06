# -*- coding: utf-8 -*-
import datetime

import mock

from travel.avia.ticket_daemon.tests.partners.helper import (
    expected_variants, get_mocked_response, get_query, assert_variants_equal,
    SettlementMock
)
from travel.avia.ticket_daemon.ticket_daemon.partners import megotravel


@mock.patch('requests.get', return_value=get_mocked_response('megotravel.xml'))
def test_megotravel_query(mocked_request):
    expected = expected_variants('megotravel.json')
    test_query = get_query(
        point_to=SettlementMock(iata='MOW', code='RU', id=213),
        date_forward=datetime.date(2018, 12, 25),
        date_backward=datetime.date(2018, 12, 27),
    )
    variants = next(megotravel.query(test_query))
    assert_variants_equal(expected, variants)
