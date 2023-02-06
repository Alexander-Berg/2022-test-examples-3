# -*- coding: utf-8 -*-
import datetime
import mock

import pytest

from travel.avia.ticket_daemon.tests.partners.helper import (
    get_mocked_response, get_query, assert_variants_equal, SettlementMock, expected_variants
)
from travel.avia.ticket_daemon.ticket_daemon.partners import onetwotrip34


@mock.patch('requests.get', return_value=get_mocked_response('onetwotrip34.xml'))
@pytest.mark.dbuser
def test_onetwotrip_query(mocked_request):
    expected = expected_variants('onetwotrip34_expected.json')
    test_query = get_query(
        point_to=SettlementMock(iata='VKO', code='RU', id=2),
        point_from=SettlementMock(iata='LED', code='RU', id=2),
        date_forward=datetime.date(2020, 8, 11),
        date_backward=datetime.date(2020, 8, 20)
    )
    variants = next(onetwotrip34.query(test_query))
    assert_variants_equal(expected, variants)
