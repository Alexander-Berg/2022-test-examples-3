# -*- coding: utf-8 -*-
from lxml import etree

import mock

from travel.avia.ticket_daemon.tests.partners.helper import get_mocked_response, get_query, assert_variants_equal, expected_variants
from travel.avia.ticket_daemon.ticket_daemon.partners import biletix_kz


class MockedQueryTracker:
    def wrap_request(*args, **kwargs):
        return type('', (), {'content': kwargs['data']})()


@mock.patch('requests.post', return_value=get_mocked_response('biletix_kz.xml'))
def test_biletix_kz_query(mocked_request):
    expected = expected_variants("biletix_kz.json")
    test_query = get_query()
    variants = next(biletix_kz.query(test_query))
    assert_variants_equal(expected, variants)


def test_KZT_currency_request():
    test_query = get_query()

    xml = biletix_kz.get_data(
        MockedQueryTracker(),
        test_query
    )
    token = etree.fromstring(xml).xpath(
        '//ns1:session_token',
         namespaces=biletix_kz.NSMAP
    )[0]
    token_parts = token.text.split(':')

    assert len(token_parts) == 4
    assert token_parts[-1] == 'KZT'
    assert token_parts[-2] == ''
