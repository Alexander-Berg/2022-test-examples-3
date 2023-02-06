# -*- coding: utf-8 -*-

from django.test.client import RequestFactory
from mock import Mock

from travel.avia.library.python.common.geotargeting.middleware import GeoTargeting

from travel.avia.library.python.tester.testcase import TestCase
from travel.avia.library.python.tester.utils.replace_setting import replace_setting


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
        middleware.tune_middleware = Mock()
        middleware.client_city_middleware = Mock()
        middleware.redirect_by_tune_middleware = Mock()
        middleware.redirect_to_national_version_middleware = Mock()

        assert middleware.process_request(request) is None

        middleware.ip_middleware.process_request.assert_called_once_with(request)
        middleware.tune_middleware.process_request.assert_called_once_with(request)
        middleware.client_city_middleware.process_request.assert_called_once_with(request)
        middleware.redirect_by_tune_middleware.process_request.assert_not_called()
        middleware.redirect_to_national_version_middleware.process_request.assert_not_called()

    @replace_setting('GEOTARGETING_DISABLE_REDIRECT_PREFIXES', '/nothingtodisable')
    def test_without_redirect(self):
        middleware = GeoTargeting()
        request = self.factory.get('/some/')

        middleware.ip_middleware = Mock()
        middleware.tune_middleware = Mock()
        middleware.client_city_middleware = Mock()
        middleware.redirect_by_tune_middleware = Mock()
        middleware.redirect_by_tune_middleware.process_request = Mock(return_value=True)
        middleware.redirect_to_national_version_middleware = Mock()

        assert middleware.process_request(request) is True

        middleware.ip_middleware.process_request.assert_called_once_with(request)
        middleware.tune_middleware.process_request.assert_called_once_with(request)
        middleware.client_city_middleware.process_request.assert_called_once_with(request)
        middleware.redirect_by_tune_middleware.process_request.assert_called_once_with(request)
        middleware.redirect_to_national_version_middleware.process_request.assert_not_called()

    @replace_setting('GEOTARGETING_DISABLE_REDIRECT_PREFIXES', '/nothingtodisable')
    def test_with_tune_redirect(self):
        middleware = GeoTargeting()
        request = self.factory.get('/some/')

        middleware.ip_middleware = Mock()
        middleware.tune_middleware = Mock()
        middleware.client_city_middleware = Mock()
        middleware.redirect_by_tune_middleware.process_request = Mock(return_value=False)
        middleware.redirect_to_national_version_middleware = Mock()
        middleware.redirect_to_national_version_middleware.process_request = Mock(return_value=True)

        assert middleware.process_request(request) is True

        middleware.ip_middleware.process_request.assert_called_once_with(request)
        middleware.tune_middleware.process_request.assert_called_once_with(request)
        middleware.client_city_middleware.process_request.assert_called_once_with(request)
        middleware.redirect_by_tune_middleware.process_request.assert_called_once_with(request)
        middleware.redirect_to_national_version_middleware.process_request.assert_called_once_with(request)
