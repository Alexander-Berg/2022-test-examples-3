# -*- coding: utf-8 -*-

from django.http import HttpResponse

from common.middleware.yandexuid import YandexuidMiddleware, YANDEXUID_MAX_AGE
from common.tester.testcase import TestCase
from common.tests.middleware.request_factory import MiddlewareRequest


YANDEXUID = '123'


class TestProcessResponse(TestCase):
    def test_no_yandexuid(self):
        request = object()
        response = HttpResponse()
        middleware = YandexuidMiddleware()
        middleware.process_response(request, response)

        assert response.cookies == {}

    def test_request_has_yandexuid(self):
        request = MiddlewareRequest(yandexuid=YANDEXUID, tld='ua', has_new_yandexuid=True)
        response = HttpResponse()
        middleware = YandexuidMiddleware()
        middleware.process_response(request, response)

        cookie = response.cookies['yandexuid']
        assert cookie
        assert cookie.value == YANDEXUID
        assert cookie['path'] == '/'
        assert cookie['domain'] == '.yandex.ua'
        assert cookie['max-age'] == YANDEXUID_MAX_AGE
        assert cookie['secure']
