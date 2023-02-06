# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

from django.test.client import RequestFactory

from common.geotargeting.middleware import Ip as IpMiddleware
from common.tester.testcase import TestCase
from common.tester.utils.replace_setting import replace_setting


class TestIpMiddleware(TestCase):
    MY_IP = '127.0.0.1'
    OVERRIDE_IP = '128.0.0.1'

    def setUp(self):
        self.factory = RequestFactory()

    @replace_setting('REMOTE_ADDR_META_VARIABLE', 'REMOTE_ADDR')
    def test_override_ip_in_get_params(self):
        with replace_setting('REMOTE_ADDR_GET_PARAM', 'ip'):
            middleware = IpMiddleware()
            request = self.factory.get('/some/', {'ip': self.OVERRIDE_IP})
            request.META['REMOTE_ADDR'] = self.MY_IP
            middleware.process_request(request)

            assert request.client_ip == self.OVERRIDE_IP

    @replace_setting('REMOTE_ADDR_META_VARIABLE', 'REMOTE_ADDR')
    def test_ip_in_meta_only_params(self):
        middleware = IpMiddleware()
        request = self.factory.get('/some/')
        request.META['REMOTE_ADDR'] = self.MY_IP
        middleware.process_request(request)

        assert request.client_ip == self.MY_IP
