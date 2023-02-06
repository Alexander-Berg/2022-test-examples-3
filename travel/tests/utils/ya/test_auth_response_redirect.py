# -*- coding: utf-8 -*-

from collections import namedtuple

import mock
from django.http import HttpResponseRedirect

from common.tester.testcase import TestCase
from common.utils.ya import auth_response_redirect


REDIRECT_URL = 'redirect-url'
Request = namedtuple('Request', ['tld'])


@mock.patch('common.utils.ya.get_auth_url', autospec=True, return_value=REDIRECT_URL)
class TestAuthResponseRedirect(TestCase):
    def test_auth_response_redirect(self, m_get_auth_url):
        request = Request(tld='ua')
        response = auth_response_redirect(request)

        assert isinstance(response, HttpResponseRedirect)
        assert response['Location'] == REDIRECT_URL

        cookie = response.cookies.get('Cookie_check')
        assert cookie
        assert cookie.value == 'CheckCookieCheckCookie'
        assert cookie['domain'] == '.yandex.ua'
        assert cookie['path'] == '/'
        assert not cookie['max-age']
        assert not cookie['expires']

        m_get_auth_url.assert_called_once_with(request)
