# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock
import pytest
from blackbox import odict
from django.contrib.auth.models import AnonymousUser, User
from django.test.client import RequestFactory

from common.middleware import yateamuser
from common.middleware.blackbox_session import SessionInvalid
from common.middleware.yateamuser import YaTeamUserAuth
from common.tester.utils.replace_setting import replace_setting


def get_request():
    return RequestFactory().get('')


@pytest.mark.dbuser
class TestYaTeamUserAuth(object):
    def test_auth_valid(self):
        User.objects.create(id=42, username='somebody')

        request = get_request()

        with mock.patch.object(yateamuser, 'get_session_for_request') as m_get_session, \
                replace_setting('TVM_SERVICE_ID', 666):
            m_get_session.return_value = odict(fields={'login': 'somebody'})

            YaTeamUserAuth().process_request(request)
            m_get_session.assert_called_once_with(request, blackbox_url='http://blackbox.yandex-team.ru/blackbox',
                                                  tvm_ticket=None)
            assert isinstance(request.user, User)
            assert request.user.id == 42

    def test_no_user(self):
        request = get_request()
        with mock.patch.object(yateamuser, 'get_session_for_request') as m_get_session, \
                replace_setting('TVM_SERVICE_ID', 666):
            m_get_session.return_value = odict(fields={'login': 'nobody'})

            YaTeamUserAuth().process_request(request)
            m_get_session.assert_called_once_with(request, blackbox_url='http://blackbox.yandex-team.ru/blackbox',
                                                  tvm_ticket=None)
            assert isinstance(request.user, AnonymousUser)

    @pytest.mark.parametrize('get_session_result', [SessionInvalid(), [None]])
    def test_blackbox_err(self, get_session_result):
        request = get_request()

        with mock.patch.object(yateamuser, 'get_session_for_request') as m_get_session, \
                replace_setting('TVM_SERVICE_ID', 666):
            m_get_session.side_effect = get_session_result

            YaTeamUserAuth().process_request(request)
            m_get_session.assert_called_once_with(request, blackbox_url='http://blackbox.yandex-team.ru/blackbox',
                                                  tvm_ticket=None)
            assert isinstance(request.user, AnonymousUser)
