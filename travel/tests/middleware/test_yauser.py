# -*- coding: utf-8 -*-
from collections import namedtuple

from common.middleware.yauser import YaUserMiddleware
from common.tester.testcase import TestCase
from common.tester.utils.replace_setting import replace_setting
from common.tests.middleware.request_factory import MiddlewareRequest

UID = 123
LOGIN = 'prince@yandex.ru'
STAFF_LOGIN = 'prince@yandex-team.ru'
SESSION_FIELDS = {
    'uid': UID,
    'login': LOGIN,
    'staff_login': STAFF_LOGIN
}

SessionDummy = namedtuple('Session', 'uid fields')

middleware = YaUserMiddleware()


def create_request(yauser=None, is_robot=False, is_secure=False, blackbox_session=None):
    return MiddlewareRequest(yauser=yauser, is_robot=is_robot, is_secure=is_secure, blackbox_session=blackbox_session)


class TestYaUserMiddleware(TestCase):
    @replace_setting('DISABLE_YAUTH', True)
    def test_disabled_yauth(self):
        request = create_request()
        middleware.process_request(request)

        assert_anonymous(request)

    @replace_setting('DISABLE_YAUTH', False)
    def test_robot(self):
        request = create_request(is_robot=True)
        middleware.process_request(request)

        assert_anonymous(request)

    @replace_setting('DISABLE_YAUTH', False)
    def test_no_session(self):
        request = create_request(blackbox_session=None)
        middleware.process_request(request)

        assert_anonymous(request)

    @replace_setting('DISABLE_YAUTH', False)
    def test_session(self):
        request = create_request(blackbox_session=SessionDummy(uid=UID, fields=SESSION_FIELDS))
        middleware.process_request(request)

        assert_yandex_user(request)


def assert_anonymous(request):
    yauser = request.yauser
    assert yauser.uid is None
    assert not yauser.is_authenticated()
    assert yauser.login == '(Not logged in)'
    assert yauser.staff_login == '(Not logged in)'


def assert_yandex_user(request):
    yauser = request.yauser
    assert yauser.uid == UID
    assert yauser.is_authenticated()
    assert yauser.login == LOGIN
    assert yauser.staff_login == STAFF_LOGIN
