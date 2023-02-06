# -*- coding: utf-8 -*-
import datetime

import mock

from travel.avia.ticket_daemon.tests.partners.helper import (
    expected_variants, get_mocked_response, get_query, assert_variants_equal,
    SettlementMock
)
from travel.avia.ticket_daemon.ticket_daemon.partners import deltatours2


@mock.patch('requests.get', return_value=get_mocked_response('deltatours2.xml'))
def test_deltatours2_query(mocked_request):
    expected = expected_variants('deltatours2.json')
    test_query = get_query(
        point_to=SettlementMock(iata='SOF', code='BG', id=9623579),
        date_forward=datetime.date(2017, 1, 20),
        date_backward=datetime.date(2017, 1, 27),
    )
    variants = next(deltatours2.query(test_query))
    assert_variants_equal(expected, variants)
