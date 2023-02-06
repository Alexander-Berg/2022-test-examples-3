# -*- coding: utf-8 -*-

import mock

from common.middleware.yandexuid import need_new_yandexuid
from common.tester.testcase import TestCase
from common.tests.middleware.request_factory import MiddlewareRequest


def create_request(is_browser=True, is_robot=False, is_ajax=False, method='GET', nocookiesupport=False):
    return MiddlewareRequest(
        is_browser=is_browser,
        is_robot=is_robot,
        is_ajax=is_ajax,
        method=method,
        nocookiesupport=nocookiesupport
    )


@mock.patch('common.middleware.yandexuid.has_valid_yandexuid', return_value=False, autospec=True)
class TestNeedNewYandexuid(TestCase):
    def test_valid_yandexuid(self, m_has_valid_yandexuid):
        m_has_valid_yandexuid.return_value = True
        request = create_request()

        assert not need_new_yandexuid(request)
        m_has_valid_yandexuid.assert_called_once_with(request)

    def test_not_browser(self, m_has_valid_yandexuid):
        request = create_request(is_browser=False)

        assert not need_new_yandexuid(request)

    def test_not_robot(self, m_has_valid_yandexuid):
        request = create_request(is_robot=True)

        assert not need_new_yandexuid(request)

    def test_ajax(self, m_has_valid_yandexuid):
        request = create_request(is_ajax=True)

        assert not need_new_yandexuid(request)

    def test_need_new_yandexuid(self, m_has_valid_yandexuid):
        request = create_request(is_browser=True, is_robot=False, is_ajax=False)

        assert need_new_yandexuid(request)
