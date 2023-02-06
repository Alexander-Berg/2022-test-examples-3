# -*- coding: utf-8 -*-
from datetime import date

import mock

from travel.avia.ticket_daemon.tests.partners.helper import (
    expected_variants, get_mocked_response, get_query, assert_variants_equal,
    SettlementMock
)
from travel.avia.ticket_daemon.ticket_daemon.partners import supersaver


@mock.patch('requests.post', return_value=get_mocked_response('supersaver_one_way.xml'))
def test_supersaver_one_way_query(mocked_request):
    expected = expected_variants('supersaver_one_way_result.json')
    test_query = get_query(**{
        'point_from': SettlementMock(iata='HTA', code='RU', id=68),
        'point_to': SettlementMock(iata='KJA', code='RU', id=62),
        'date_forward': date(2018, 8, 29),
        'date_backward': None,
        'passengers': {'adults': 2, 'children': 1, 'infants': 1},
    })
    variants = next(supersaver.query(test_query))
    assert_variants_equal(expected, variants)


@mock.patch('requests.post', return_value=get_mocked_response('supersaver_return.xml'))
def test_supersaver_return_query(mocked_request):
    expected = expected_variants('supersaver_return_result.json')
    test_query = get_query(**{
        'date_forward': date(2018, 8, 29),
        'date_backward': date(2018, 9, 1),
        'passengers': {'adults': 2, 'children': 1, 'infants': 1},
    })
    variants = next(supersaver.query(test_query))
    assert_variants_equal(expected, variants)
