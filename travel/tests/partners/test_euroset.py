# -*- coding: utf-8 -*-
import datetime
import mock

from travel.avia.ticket_daemon.tests.partners.helper import (
    expected_variants, get_mocked_response, get_query, assert_variants_equal,
    SettlementMock
)
from travel.avia.ticket_daemon.ticket_daemon.partners import euroset


@mock.patch('requests.get', return_value=get_mocked_response('euroset.xml'))
def test_euroset_query(mocked_request):
    expected = expected_variants('euroset.json')
    test_query = get_query(
        point_to=SettlementMock(iata='KIV', code='UA', id=10313),
        date_forward=datetime.date(2017, 3, 24),
        date_backward=datetime.date(2017, 1, 4),
    )
    variants = next(euroset.query(test_query))
    assert_variants_equal(expected, variants)
