# -*- coding: utf-8 -*-

from django.test.client import RequestFactory
from mock import Mock

from common.geotargeting.middleware import GeoTargeting
from common.tester.testcase import TestCase


class TestGeoTargetingMiddleware(TestCase):
    def setUp(self):
        self.factory = RequestFactory()

    def test_exclude_path(self):
        middleware = GeoTargeting()
        request = self.factory.get('/api/')

        middleware.process_request(request)

        assert request.client_city is None
        assert request.client_city_real is None

    def test_exclude_but_not_exclude_path(self):
        middleware = GeoTargeting()
        request = self.factory.get('/api/tickets/redirect/')

        middleware.ip_middleware = Mock()
        middleware.client_city_middleware = Mock()

        assert middleware.process_request(request) is None

        middleware.ip_middleware.process_request.assert_called_once_with(request)
        middleware.client_city_middleware.process_request.assert_called_once_with(request)
