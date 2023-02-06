# -*- coding: utf-8 -*-

import mock

from common.middleware.yandexuid import YandexuidMiddleware
from common.tester.testcase import TestCase

YANDEXUID = 123
NEW_YANDEXUID = 456


class Request(object):
    def __init__(self, yandexuid_cookie):
        self.COOKIES = {'yandexuid': yandexuid_cookie}
        self.yandexuid = None


@mock.patch('common.middleware.yandexuid.gen_yandexuid', autospec=True)
@mock.patch('common.middleware.yandexuid.need_new_yandexuid', autospec=True)
class TestProcessRequest(TestCase):
    def test_not_need_new_yandexuid(self, m_need_new_yandexuid, m_gen_yandexuid):
        m_need_new_yandexuid.return_value = False

        request = Request(YANDEXUID)
        middleware = YandexuidMiddleware()

        assert middleware.process_request(request) is None
        assert request.yandexuid == YANDEXUID

    def test_gen_new_yandexuid(self, m_need_new_yandexuid, m_gen_yandexuid):
        m_need_new_yandexuid.return_value = True
        m_gen_yandexuid.return_value = NEW_YANDEXUID

        request = Request(YANDEXUID)
        middleware = YandexuidMiddleware()

        assert middleware.process_request(request) is None
        assert request.yandexuid == NEW_YANDEXUID
