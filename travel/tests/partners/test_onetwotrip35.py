# -*- coding: utf-8 -*-
import datetime
import mock

import pytest

from travel.avia.ticket_daemon.tests.partners.helper import (
    get_mocked_response, get_query, assert_variants_equal, SettlementMock, expected_variants
)
from travel.avia.ticket_daemon.ticket_daemon.partners import onetwotrip35
from travel.avia.library.python.common.models.schedule import Company


COMPANY_MOCK_FUNC = 'travel.avia.ticket_daemon.ticket_daemon.api.models_utils.company._company_with_codes'
COMPANIES_MOCK = {1: Company(iata='O1', id=1), 2: Company(iata='O2', id=2), 3: Company(iata='DP', id=3)}


@mock.patch('requests.get', return_value=get_mocked_response('onetwotrip35.xml'))
@mock.patch(COMPANY_MOCK_FUNC, return_value=COMPANIES_MOCK)
@pytest.mark.dbuser
def test_onetwotrip_query(mocked_request, mocked_comapny_with_codes):
    expected = expected_variants('onetwotrip35_expected.json')
    test_query = get_query(
        point_to=SettlementMock(iata='VKO', code='RU', id=2),
        point_from=SettlementMock(iata='LED', code='RU', id=2),
        date_forward=datetime.date(2020, 8, 11),
        date_backward=datetime.date(2020, 8, 20)
    )
    variants = next(onetwotrip35.query(test_query))
    assert_variants_equal(expected, variants)


@mock.patch('requests.get', return_value=get_mocked_response('onetwotrip35_bad_op.xml'))
@mock.patch(COMPANY_MOCK_FUNC, return_value=COMPANIES_MOCK)
@pytest.mark.dbuser
def test_onetwotrip_bad_op_query(mocked_request, mocked_comapny_with_codes):
    expected = expected_variants('onetwotrip35_bad_op_expected.json')
    test_query = get_query(
        point_to=SettlementMock(iata='VKO', code='RU', id=2),
        point_from=SettlementMock(iata='LED', code='RU', id=2),
        date_forward=datetime.date(2020, 8, 11),
        date_backward=datetime.date(2020, 8, 20)
    )
    variants = next(onetwotrip35.query(test_query))
    assert_variants_equal(expected, variants)
