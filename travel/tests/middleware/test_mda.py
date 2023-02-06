# -*- coding: utf-8 -*-

from django.test.client import RequestFactory
from mock import Mock, patch

from travel.avia.library.python.common.middleware.host import Host
from travel.avia.library.python.common.middleware.mda import MDAMiddleware, AnonymousYandexUser, YandexUser

from travel.avia.library.python.tester.testcase import TestCase
from travel.avia.library.python.tester.utils.replace_setting import replace_setting


class TestMDAMiddleware(TestCase):
    def setUp(self):
        self.factory = RequestFactory()
        self.middleware = MDAMiddleware()
        self.host_middleware = Host()

    @replace_setting('HOST', 'rasp.yandex.ru')
    def prepare_request(self):
        request = self.factory.get('/geo/?nocookiesupport=yes')
        self.host_middleware.process_request(request)
        self.middleware.process_request(request)

        return request

    def test_non_authorized(self):
        with patch.object(MDAMiddleware, '_get_blackbox', return_value=Mock()):
            request = self.prepare_request()

            assert request.yauser.is_authenticated() is False
            assert request.yauser.login == AnonymousYandexUser.login

    def test_subscribed_user(self):
        session = {
            'uid': 1234,
            'fields': {
                'staff_login': False,
                'avia_subscription': True,
                'lang': 'en'
            },
            'redirect': False
        }
        yauser = YandexUser(int(session['uid']), False, session['fields'], session['redirect'])

        assert (yauser and yauser.is_authenticated() and yauser.avia_subscription) is True
