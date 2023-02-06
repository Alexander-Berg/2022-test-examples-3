# -*- coding: utf-8 -*-
import datetime

import mock

from travel.avia.ticket_daemon.tests.partners.helper import (
    get_mocked_response, get_query, assert_variants_equal,
    SettlementMock, expected_variants
)
from travel.avia.ticket_daemon.ticket_daemon.partners import chabooka2


@mock.patch('requests.post', return_value=get_mocked_response('chabooka2.xml'))
def test_chabooka2_query(mocked_request):
    expected = expected_variants('chabooka_expected.json')

    test_query = get_query(
        point_to=SettlementMock(iata='BCN', code='ES', id=10429),
        date_forward=datetime.date(2017, 1, 28),
        date_backward=datetime.date(2017, 2, 4),
    )
    variants = chabooka2.query(test_query)
    assert_variants_equal(expected, next(variants))
