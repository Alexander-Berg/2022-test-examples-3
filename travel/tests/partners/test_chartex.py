# -*- coding: utf-8 -*-
import datetime
import urlparse

import mock

from travel.avia.ticket_daemon.tests.partners.helper import (
    get_mocked_response, get_query, assert_variants_equal,
    SettlementMock, expected_variants
)
from travel.avia.ticket_daemon.ticket_daemon.partners import chartex


def test_chartex_query():
    expected = expected_variants('chartex_expected.json')

    test_query = get_query(
        point_to=SettlementMock(iata='GOI', code='IN', id=26812),
        date_forward=datetime.date(2017, 1, 24),
        date_backward=datetime.date(2017, 2, 9),
    )
    test_query.chartex_city_code_from = '5359'
    test_query.chartex_city_code_to = '4363'

    def mocked_responses():
        def wrapper(*args, **kwargs):
            data = dict(urlparse.parse_qsl(kwargs.get('data')))

            if data['request'] == 'step1rub':
                return get_mocked_response('chartex_pages.xml')
            elif data['request'] == 'reisinfo':
                return get_mocked_response('chartex_info.xml')
            else:
                raise AttributeError("Invalid data['request'] request parameter")
        return wrapper

    with mock.patch('requests.post', side_effect=mocked_responses()):
        variants = next(chartex.query(test_query))

    assert_variants_equal(expected, variants)
