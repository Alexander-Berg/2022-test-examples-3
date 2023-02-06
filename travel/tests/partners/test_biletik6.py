# -*- coding: utf-8 -*-
import datetime

import mock

from travel.avia.ticket_daemon.tests.partners.helper import (
    expected_variants, get_mocked_response, get_query, assert_variants_equal, SettlementMock
)
from travel.avia.ticket_daemon.ticket_daemon.partners import biletik6

headers = {'X-Redirect-URL': 'http://test.site/redirect/', 'X-Currency-Code': 'RUB'}


@mock.patch('requests.post', return_value=get_mocked_response('biletik6.json', headers=headers))
def test_biletik6_query(mocked_request):
    test_query = get_query(
        point_from=SettlementMock(iata='VVO', code='RU', id=1),
        point_to=SettlementMock(iata='SVX', code='RU', id=2),
        date_forward=datetime.date(2020, 2, 12),
        date_backward=datetime.date(2020, 2, 14),
    )
    expected = expected_variants('biletik6_expected.json')
    variants = next(biletik6.query(test_query))
    assert_variants_equal(expected, variants)
