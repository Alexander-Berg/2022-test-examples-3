# -*- coding: utf-8 -*-
import mock

from travel.avia.ticket_daemon.tests.partners.helper import (
    expected_variants, get_mocked_response, get_query, assert_variants_equal
)
from travel.avia.ticket_daemon.ticket_daemon.partners import ozon2


@mock.patch('requests.get', return_value=get_mocked_response('ozon2.json'))
def test_ozon2_query(mocked_request):
    expected = expected_variants('ozon2_expected.json')
    variants = next(ozon2.query(get_query()))
    assert_variants_equal(expected, variants)
