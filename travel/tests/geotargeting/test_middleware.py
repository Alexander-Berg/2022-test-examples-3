# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

import pytest
from django.test.client import RequestFactory

from common.geotargeting.middleware import Ip
from common.tester.utils.replace_setting import replace_setting


@pytest.mark.parametrize('get_ip, x_real_ip, expected', [
    ('1.0.0.0', '0.1.0.0', '1.0.0.0'),
    ('', '0.1.0.0', '0.1.0.0'),
    ('', '', None),
])
@replace_setting('REMOTE_ADDR_GET_PARAM', 'ip')
@replace_setting('REMOTE_ADDR_META_VARIABLE_FALLBACK', None)
def test_ip_client_ip(get_ip, x_real_ip, expected):
    request = RequestFactory(**{
        'HTTP_X_REAL_IP': x_real_ip,
    }).get('/', {'ip': get_ip})

    Ip().process_request(request)
    assert request.client_ip == expected


@replace_setting('REMOTE_ADDR_GET_PARAM', 'ip')
@replace_setting('REMOTE_ADDR_META_VARIABLE_FALLBACK', 'REMOTE_ADDR')
def test_ip_client_ip_fallback():
    request = RequestFactory(**{
        'HTTP_X_REAL_IP': '',
        'REMOTE_ADDR': '127.0.0.3'
    }).get('/', {'ip': ''})

    Ip().process_request(request)
    assert request.client_ip == '127.0.0.3'
