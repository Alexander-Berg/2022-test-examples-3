# -*- coding: utf-8 -*-

from django.test.client import RequestFactory
from mock import Mock, patch

from travel.avia.library.python.common.middleware.uatraits_detector import UatraitsDetectorMiddleware

from travel.avia.library.python.tester.testcase import TestCase


class TestUatraitsMiddleware(TestCase):
    UATRAITS_RESULT = 'UATRAITS_RESULT'
    USER_AGENT = 'USER_AGENT'

    def setUp(self):
        self.factory = RequestFactory()

    def test_empty_user_agent(self):
        with patch.object(UatraitsDetectorMiddleware, '_get_detector', return_value=Mock()):
            middleware = UatraitsDetectorMiddleware()
            request = self.factory.get('/search/')

            middleware.process_request(request)

            assert request.uatraits_result == {}
            assert not middleware.detector.detect.called

    def test_with_user_agent(self):
        with patch.object(UatraitsDetectorMiddleware, '_get_detector', return_value=Mock()):
            middleware = UatraitsDetectorMiddleware()
            middleware.detector.detect = Mock(return_value=self.UATRAITS_RESULT)

            request = self.factory.get('/search/', HTTP_USER_AGENT=self.USER_AGENT)

            middleware.process_request(request)

            middleware.detector.detect.assert_called_once_with(self.USER_AGENT)
            assert request.uatraits_result == self.UATRAITS_RESULT
