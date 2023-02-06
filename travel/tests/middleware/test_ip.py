# -*- coding: utf-8 -*-

from django.test.client import RequestFactory
from mock import patch

from travel.avia.library.python.common.geotargeting.middleware import Ip as IpMiddleware

from travel.avia.library.python.tester.testcase import TestCase


class TestIpMiddleware(TestCase):
    MY_IP = '127.0.0.1'
    OVERRIDE_IP = '128.0.0.1'

    def setUp(self):
        self.factory = RequestFactory()

    def test_override_ip_in_get_params(self):
        with patch.object(IpMiddleware, '_is_yandex_network', return_value=None) as mock_method:
            middleware = IpMiddleware()
            request = self.factory.get('/some/', {'ip': self.OVERRIDE_IP})
            request.META['REMOTE_ADDR'] = self.MY_IP
            middleware.process_request(request)

            assert request.client_ip == self.OVERRIDE_IP
            mock_method.assert_called_once_with(self.OVERRIDE_IP)

    def test_ip_in_meta_only_params(self):
        with patch.object(IpMiddleware, '_is_yandex_network', return_value=None) as mock_method:
            middleware = IpMiddleware()
            request = self.factory.get('/some/')
            request.META['REMOTE_ADDR'] = self.MY_IP
            middleware.process_request(request)

            assert request.client_ip == self.MY_IP
            mock_method.assert_called_once_with(self.MY_IP)

    def test_is_yandex_ip_params(self):
        with patch.object(IpMiddleware, '_is_yandex_network', return_value=True) as mock_method:
            middleware = IpMiddleware()
            request = self.factory.get('/some/')
            request.META['REMOTE_ADDR'] = self.MY_IP
            middleware.process_request(request)

            assert request.yandex
            assert request.is_yandex_network
            mock_method.assert_called_once_with(self.MY_IP)

    def test_is_not_yandex_ip_params(self):
        with patch.object(IpMiddleware, '_is_yandex_network', return_value=None) as mock_method:
            middleware = IpMiddleware()
            request = self.factory.get('/some/')
            request.META['REMOTE_ADDR'] = self.MY_IP
            middleware.process_request(request)

            assert request.yandex is None
            assert request.is_yandex_network is None
            mock_method.assert_called_once_with(self.MY_IP)
