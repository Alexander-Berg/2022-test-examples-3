# -*- coding: utf-8 -*-
import base64
import urllib
import urlparse
from httplib import FORBIDDEN

import pytest
from mock import mock
from nose_parameterized import parameterized
from urllib2 import HTTPError
from requests.models import Response, Request, PreparedRequest

from mpfs.common.errors import (
    TelemostApiNotFound,
    TelemostApiForbidden,
    TelemostApiBadRequest,
    TelemostApiGone
)
from mpfs.core.services.passport_service import PlatformUserDetailsHelper
from mpfs.core.services.telemost_api_service import TelemostViaTCMService, TemplatedTelemostService, telemost_api, \
    MultiTelemostRequestsPoweredServiceBase
from mpfs.platform.common import PlatformUser
from mpfs.platform.utils.uid_formatters import to_yateam_uid
from test.base import DiskTestCase
from test.fixtures.services import TCMData
from test.helpers.stubs.resources import users_info
from test.helpers.telemost_api_responses_data import (
    CreatedConfData,
    UserInfoData,
    PeersData,
    ConfShortInfoData,
    AuthURIData,
    LeaveRoomData,
    BroadcastData,
    StreamData,
    StreamConnectionData,
)
from test.parallelly.api.disk.base import DiskApiTestCase

from mpfs.common.static import tags, codes
from mpfs.common.util import from_json, to_json, ctimestamp
from mpfs.config import settings
from mpfs.core.services.clck_service import clck
from mpfs.platform.v1.telemost.handlers import (
    ConferencesCryptAgent,
    PLATFORM_TELEMOST_XMPP_USERS,
    XMPPLimitTypes,
    ConferenceURIParser,
)
from mpfs.platform.v1.telemost.permissions import (
    TelemostPermission,
)
from mpfs.platform.v2.telemost.permissions import (
    TelemostConferenceCreatePermission,
)
from mpfs.platform.v1.disk.permissions import WebDavPermission, MobileMailPermission

from test.base_suit import UserTestCaseMixin
from test.fixtures import users
from test.helpers.stubs.services import (
    PassportStub,
    TelemostApiRawResponseStub,
    TelemostApiStub,
    TCMStub,
)


def construct_requests_resp(status=200, content='',
                            request_method='GET', request_url='https://telemost.dst.yandex.ru/v2/stub',
                            request_params=None, request_data=None, request_headers=None, request_body=None):
    request = Request(request_method, request_url,
                      params=request_params, data=request_data, headers=request_headers)
    request.body = request_body

    resp = Response()
    resp.status_code = status
    resp.request = request
    if isinstance(content, dict):
        content = to_json(content)
    resp._content = content

    return resp


class CreateConferenceTestCase(UserTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.INTERNAL
    api_version = 'v1'
    method = 'POST'
    url = 'telemost/conferences'
    staff_uid = users.user_1.uid
    telemost_uid = users.user_7.uid
    non_staff_uid = users.user_3.uid
    enabled_via_config_uid = users.user_6.uid

    def setup_method(self, method):
        super(CreateConferenceTestCase, self).setup_method(method)
        users_info.update_info_by_uid(self.staff_uid, has_staff=True, is_2fa_enabled=True)
        users_info.update_info_by_uid(self.telemost_uid, has_telemost=True, is_2fa_enabled=True)

    @parameterized.expand([
        ('staff', staff_uid,
         PLATFORM_TELEMOST_XMPP_USERS[XMPPLimitTypes.STAFF]['user_id'],
         PLATFORM_TELEMOST_XMPP_USERS[XMPPLimitTypes.STAFF]['password']),
        ('telemost_sid', telemost_uid,
         PLATFORM_TELEMOST_XMPP_USERS[XMPPLimitTypes.STAFF]['user_id'],
         PLATFORM_TELEMOST_XMPP_USERS[XMPPLimitTypes.STAFF]['password']),
        # добавлен в конфиге в список с разрешениями на создание и в список фейковых стафф-юзеров
        ('enabled_via_config', enabled_via_config_uid,
         PLATFORM_TELEMOST_XMPP_USERS[XMPPLimitTypes.STAFF]['user_id'],
         PLATFORM_TELEMOST_XMPP_USERS[XMPPLimitTypes.STAFF]['password']),
        ('non_staff', non_staff_uid,
         PLATFORM_TELEMOST_XMPP_USERS[XMPPLimitTypes.STAFF]['user_id'],
         PLATFORM_TELEMOST_XMPP_USERS[XMPPLimitTypes.STAFF]['password']),
    ])
    def test_default(self, case_name, uid, expected_xmpp_user_id, expected_xmpp_password):
        with self.specified_client(scopes=TelemostPermission.scopes, uid=uid):
            resp = self.client.request(self.method, self.url)

        assert resp.status_code == 201
        data = from_json(resp.content)
        assert data['uri']
        assert data['xmpp_password'] == expected_xmpp_password
        assert data['xmpp_user_id'] == expected_xmpp_user_id

    def test_scopes(self):
        with self.specified_client(scopes=MobileMailPermission.scopes, uid=self.staff_uid):
            resp = self.client.request(self.method, self.url)

        assert resp.status_code == 201

    @parameterized.expand([
        ('staff', staff_uid, 1),
        ('telemost_sid', telemost_uid, 0)
    ])
    def test_sub_on_sid(self, case_name, uid, expected_calls_count):
        with self.specified_client(scopes=TelemostPermission.scopes, uid=uid), \
             mock.patch('mpfs.core.services.passport_service.PassportAPI.subscribe') as mocked_passport_api_sub:
            resp = self.client.request(self.method, self.url)

        assert resp.status_code == 201
        assert mocked_passport_api_sub.call_count == expected_calls_count

    @parameterized.expand([
        ('default', None, 1),
        ('staff_only', {'staff_only': True}, 0)
    ])
    def test_params(self, case_name, body, expected_params):
        with self.specified_client(scopes=TelemostPermission.scopes), \
             mock.patch('mpfs.core.services.common_service.RequestsPoweredServiceBase.request') as mocked_request:
            resp = self.client.request(self.method, self.url, data=body, headers={'Content-Type': 'application/json'})

        assert resp.status_code == 201

    @parameterized.expand([
        ('default', None, None, True),
        ('staff_only', {'staff_only': True}, False, False)
    ])
    def test_yandex_team(self, case_name, body, expected_staff_only, expected_external_meeting):
        with self.specified_client(uid=to_yateam_uid(self.uid)), \
             mock.patch('mpfs.platform.v1.telemost.handlers.PLATFORM_TELEMOST_API_ENABLED', True), \
             mock.patch('mpfs.core.services.common_service.RequestsPoweredServiceBase.request') as mocked_request:
            self.client.request(self.method, self.url, data=body, headers={'Content-Type': 'application/json'})

        # первый 0 - args; 1 - kwargs
        # 0 - method; 1 - URL; 2 - params
        params = mocked_request.call_args[0][2]
        if expected_staff_only is not None:
            assert params['staff_only'] == expected_staff_only
        assert params['external_meeting'] == expected_external_meeting

    def test_invalid_password_returns_400(self):
        with self.specified_client(scopes=TelemostPermission.scopes, uid=self.staff_uid):
            resp = self.client.request(self.method, self.url,
                                       data={'password': 'x' * (ConferencesCryptAgent.USER_SECRET_MAX_LENGHT + 1)})

        assert resp.status_code == 400

    def test_disabled_by_config(self):
        with self.specified_client(scopes=TelemostPermission.scopes, uid=self.staff_uid), \
             mock.patch(
                 'mpfs.platform.v1.telemost.handlers.PLATFORM_TELEMOST_DISABLE_ACCESS_TO_CREATE_FOR_UIDS_PERCENT', 100):
            resp = self.client.request(self.method, self.url)

        assert resp.status_code == 403


@pytest.mark.skipif(True, reason='tests should be updated with new TelemostAPI')
class JoinConferenceTestCase(UserTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    method = 'GET'
    url = 'telemost/conferences/%s/connection'
    staff_uid = users.user_1.uid
    non_staff_uid = users.user_3.uid
    enabled_via_config_uid = users.user_6.uid

    def setup_method(self, method):
        super(JoinConferenceTestCase, self).setup_method(method)
        PassportStub.update_info_by_uid(self.staff_uid, has_staff=True, is_2fa_enabled=True)
        TelemostApiStub(raise_exception=True, exception_to_raise=TelemostApiNotFound('ConferenceNotFound')).start()

    def create_conference(self, uid=staff_uid, password=None, staff_only=None):
        body = {}
        if password is not None:
            body['password'] = password
        if staff_only is not None:
            body['staff_only'] = staff_only

        with self.specified_client(scopes=TelemostPermission.scopes, uid=uid):
            resp = self.client.request('POST', 'telemost/conferences', data=body)

        return from_json(resp.content)

    @parameterized.expand([
        ('default', ''),
        ('with_params', '?enot=123'),
    ])
    def test_default(self, case_name, addition_uri):
        conference_info = self.create_conference()
        uri = urllib.quote(conference_info['uri'] + addition_uri, safe='')

        with self.specified_client(scopes=TelemostPermission.scopes, uid=self.staff_uid):
            resp = self.client.request(self.method, self.url % uri)

        assert resp.status_code == 200
        data = from_json(resp.content)
        assert all(key in data
                   for key in ('conf_id', 'conf_pwd', 'conf_server', 'uri'))
        assert data['xmpp_password'] == PLATFORM_TELEMOST_XMPP_USERS[XMPPLimitTypes.STAFF]['password']
        assert data['xmpp_user_id'] == PLATFORM_TELEMOST_XMPP_USERS[XMPPLimitTypes.STAFF]['user_id']

    def test_no_sub_for_mailish(self):
        # этот uid в заглушке Паспорта помечен как is_mailish
        mailish_user_uid = '520161280'
        with mock.patch('mpfs.core.services.passport_service.PassportAPI.subscribe') as mocked_passport_api_sub:
            conference_info = self.create_conference(uid=mailish_user_uid)
            mocked_passport_api_sub.assert_not_called()

        uri = urllib.quote(conference_info['uri'], safe='')

        with self.specified_client(scopes=TelemostPermission.scopes, uid=mailish_user_uid), \
             mock.patch('mpfs.core.services.passport_service.PassportAPI.subscribe') as mocked_passport_api_sub:
            resp = self.client.request(self.method, self.url % uri)

        assert resp.status_code == 200
        mocked_passport_api_sub.assert_not_called()

    def test_non_ascii_unquoted_symbols_in_conf_uri(self):
        conference_info = self.create_conference()
        uri = urllib.quote(conference_info['uri'], safe='') + '%E2%80%8B'

        with self.specified_client(scopes=TelemostPermission.scopes, uid=self.staff_uid):
            resp = self.client.request(self.method, self.url % uri)

        assert resp.status_code != 500

    def test_white_listed_gets_unlimited_xmpp_creds(self):
        white_listed_uid = users.user_4.uid
        PassportStub.update_info_by_uid(white_listed_uid, has_staff=True, is_2fa_enabled=True)
        conference_info = self.create_conference(uid=white_listed_uid)

        assert conference_info['xmpp_password'] == PLATFORM_TELEMOST_XMPP_USERS[XMPPLimitTypes.UNLIM]['password']
        assert conference_info['xmpp_user_id'] == PLATFORM_TELEMOST_XMPP_USERS[XMPPLimitTypes.UNLIM]['user_id']

        uri = urllib.quote(conference_info['uri'], safe='')

        with self.specified_client(scopes=TelemostPermission.scopes, uid=self.non_staff_uid):
            resp = self.client.request(self.method, self.url % uri)

        assert resp.status_code == 200
        invite_info = from_json(resp.content)
        assert invite_info['xmpp_password'] == PLATFORM_TELEMOST_XMPP_USERS[XMPPLimitTypes.UNLIM]['password']
        assert invite_info['xmpp_user_id'] == PLATFORM_TELEMOST_XMPP_USERS[XMPPLimitTypes.UNLIM]['user_id']

    def test_invalid_password_returns_400(self):
        conference_info = self.create_conference()

        with self.specified_client(scopes=TelemostPermission.scopes, uid=self.staff_uid):
            resp = self.client.request(self.method, self.url % urllib.quote(conference_info['uri'], safe=''),
                                       data={'password': 'x' * (ConferencesCryptAgent.USER_SECRET_MAX_LENGHT + 1)})

        assert resp.status_code == 400

    def test_wrong_password_returns_404(self):
        correct_pass = '12345'
        conference_info = self.create_conference(password=correct_pass)

        with self.specified_client(scopes=TelemostPermission.scopes, uid=self.staff_uid):
            resp = self.client.request(self.method, self.url % urllib.quote(conference_info['uri'], safe=''),
                                       data={'password': correct_pass + '6'})

        assert resp.status_code == 404

    @parameterized.expand([
        ('non_base64', 'brute_forcing'),
        ('bad_format_for_encrypted_data', base64.b64encode('brute_forcing')),
    ])
    def test_wrong_encrypted_conf_key_returns_404(self, case_name, encrypted_conf_key):
        url_wo_correct_hash = 'https://telemost.online/join/%s' % encrypted_conf_key
        _, short_url = clck.generate(url_wo_correct_hash)
        encoded_url = urllib.quote(short_url, safe='')

        with self.specified_client(scopes=TelemostPermission.scopes, uid=self.staff_uid):
            resp = self.client.request(self.method, self.url % encoded_url)

        assert resp.status_code == 404

    def test_invalid_conf_url_returns_404(self):
        url_wo_correct_hash = 'https://telemost.online/join/brute_forcing'
        _, short_url = clck.generate(url_wo_correct_hash)
        encoded_url = urllib.quote(short_url, safe='')

        with self.specified_client(scopes=TelemostPermission.scopes, uid=self.staff_uid):
            resp = self.client.request(self.method, self.url % encoded_url)

        assert resp.status_code == 404

    @parameterized.expand([
        ('not_clck_url', 'https://pwned.me/link/to/download/big/file'),
        ('wrong_url_format', 'meow'),
    ])
    def test_wrong_url_returns_404(self, case_name, url):
        encoded_url = urllib.quote(url, safe='')

        with self.specified_client(scopes=TelemostPermission.scopes, uid=self.staff_uid):
            resp = self.client.request(self.method, self.url % encoded_url)

        assert resp.status_code == 404

    def test_no_clck_request_for_invalid_uri(self):
        encoded_url = urllib.quote('http://dummy.ya.net/d/' + 'a' * 41, safe='')

        with self.specified_client(scopes=TelemostPermission.scopes, uid=self.staff_uid), \
             mock.patch('mpfs.core.services.clck_service.Clck.short_url_to_full_url') as mocked_clck:
            self.client.request(self.method, self.url % encoded_url)
            mocked_clck.assert_not_called()

    def test_for_non_staff_returns_403(self):
        conference_info = self.create_conference(staff_only=True)

        with self.specified_client(scopes=TelemostPermission.scopes, uid=self.non_staff_uid):
            resp = self.client.request(self.method, self.url % urllib.quote(conference_info['uri'], safe=''))

        assert resp.status_code == 403

    def test_non_staff_but_enabled_in_config_can_join_staff_only(self):
        conference_info = self.create_conference(staff_only=True)

        with self.specified_client(scopes=TelemostPermission.scopes, uid=self.enabled_via_config_uid):
            resp = self.client.request(self.method, self.url % urllib.quote(conference_info['uri'], safe=''))

        assert resp.status_code == 200

    @parameterized.expand([
        ('anonymous',
         {'scopes': TelemostPermission.scopes, 'uid': None},
         'cloud_api_user_telemost', '127.0.0.1'),
        ('authorized',
         {'scopes': TelemostPermission.scopes, 'uid': staff_uid},
         'cloud_api_user_telemost_join', staff_uid),
    ])
    def test_rate_limiter(self, case_name, client_params, expected_rl_group, expected_rl_key):
        conference_info = self.create_conference()
        uri = urllib.quote(conference_info['uri'], safe='')

        with self.specified_client(**client_params), \
             mock.patch(
                 'mpfs.core.services.rate_limiter_service.RateLimiterService.is_limit_exceeded',
                 return_value=False,
             ) as mocked_rate_limiter:
            resp = self.client.request(self.method, self.url % uri)

        assert resp.status_code == 200
        group, key = mocked_rate_limiter.call_args[0]
        assert group == expected_rl_group
        assert key == expected_rl_key

    @parameterized.expand([('unlimited', True,
                            PLATFORM_TELEMOST_XMPP_USERS[XMPPLimitTypes.UNLIM]['user_id'],
                            PLATFORM_TELEMOST_XMPP_USERS[XMPPLimitTypes.UNLIM]['password']),
                           ('with_limits', False,
                            PLATFORM_TELEMOST_XMPP_USERS[XMPPLimitTypes.DEFAULT]['user_id'],
                            PLATFORM_TELEMOST_XMPP_USERS[XMPPLimitTypes.DEFAULT]['password'])])
    def test_conf_key_with_old_unlimited_key(self, case_name, unlimited_value, expected_xmpp_user_id,
                                             expected_xmpp_password):
        conf_key_dict = {'conf_id': 'conf_id_value',
                         'conf_pwd': 'conf_pwd_value',
                         'staff_only': False,
                         'unlimited': unlimited_value,
                         'timestamp': ctimestamp(),
                         'salt': 'salt_value'}
        raw_conf_key = to_json(conf_key_dict)
        encrypted_conf_key = ConferencesCryptAgent.encrypt_conf_key(raw_conf_key)
        conf_uri = ConferenceURIParser.build(encrypted_conf_key)
        uri = urllib.quote(conf_uri, safe='')

        with self.specified_client(scopes=TelemostPermission.scopes, uid=self.non_staff_uid):
            resp = self.client.request(self.method, self.url % uri)

        assert resp.status_code == 200
        data = from_json(resp.content)
        assert all(key in data
                   for key in ('conf_id', 'conf_pwd', 'conf_server', 'uri'))
        assert data['xmpp_password'] == expected_xmpp_password
        assert data['xmpp_user_id'] == expected_xmpp_user_id


class LogStatisticsTestCase(UserTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.INTERNAL
    api_version = 'v1'
    method = 'POST'
    url = 'telemost/stat/log'

    @parameterized.expand([('default', to_json({'stats_report': {'owner': 'raccoon',
                                                                 'quality': 'the-best',
                                                                 'score': 17}}),
                            200),
                           ('empty_body', None,
                            400),
                           ('too_huge_body', to_json({'stats_report': {'owner': 'raccoon' * 40000,
                                                                       'quality': 'the-best',
                                                                       'score': 17}}),
                            400)])
    def test_default(self, case_name, body, expected_status_code):
        with self.specified_client(scopes=TelemostPermission.scopes):
            resp = self.client.request(self.method, self.url, data=body)

        assert resp.status_code == expected_status_code


class CreateTelemostApiConferenceTestCase(UserTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.INTERNAL
    api_version = 'v1'
    method = 'POST'
    url = 'telemost/conferences'
    staff_uid = users.user_1.uid

    def setup_method(self, method):
        super(CreateTelemostApiConferenceTestCase, self).setup_method(method)
        users_info.update_info_by_uid(self.staff_uid, has_staff=True, is_2fa_enabled=True)
        TelemostApiStub(create_response={'uri': 'https://telemost.yandex.ru/j/12345678901234',
                                         'conf_id': 'conf_id_stub',
                                         'conf_pwd': 'conf_pwd_stub',
                                         'limit_type': XMPPLimitTypes.STAFF.upper(),
                                         'client_configuration': from_json('{"connection_config": {"test_param": 1}, "room_config": {"test_value": "val"}}')}).start()

    def test_telemost_api_conference_creation(self):
        with self.specified_client(scopes=TelemostPermission.scopes, uid=self.staff_uid), mock.patch(
                'mpfs.platform.v1.telemost.handlers.PLATFORM_TELEMOST_API_ENABLED', True):
            resp = self.client.request(self.method, self.url)

        assert resp.status_code == 201
        data = from_json(resp.content)
        assert data['uri'] == 'https://telemost.yandex.ru/j/12345678901234'
        assert data['conf_id'] == 'conf_id_stub'
        assert data['conf_pwd'] == 'conf_pwd_stub'
        assert data['xmpp_password'] == PLATFORM_TELEMOST_XMPP_USERS[XMPPLimitTypes.STAFF]['password']
        assert data['xmpp_user_id'] == PLATFORM_TELEMOST_XMPP_USERS[XMPPLimitTypes.STAFF]['user_id']
        assert data['connection_config'] == from_json('{"test_param": 1}')
        assert data['room_config'] == from_json('{"test_value": "val"}')


class JoinTelemostApiConferenceTestCase(UserTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.INTERNAL
    api_version = 'v1'
    method = 'GET'
    url = 'telemost/conferences/%s/connection'
    staff_uid = users.user_1.uid

    def setup_method(self, method):
        super(JoinTelemostApiConferenceTestCase, self).setup_method(method)
        users_info.update_info_by_uid(self.staff_uid, has_staff=True, is_2fa_enabled=True)

    def test_telemost_api_not_found(self):
        TelemostApiStub(raise_exception=True, exception_to_raise=TelemostApiNotFound('ConferenceNotFound')).start()
        with self.specified_client(scopes=TelemostPermission.scopes, uid=self.staff_uid), mock.patch(
                'mpfs.platform.v1.telemost.handlers.PLATFORM_TELEMOST_API_ENABLED', True):
            resp = self.client.request(self.method,
                                       self.url % urllib.quote('https://telemost.yandex.ru/j/12345678901234', ''))

        assert resp.status_code == 404

    def test_telemost_api_forbidden(self):
        TelemostApiStub(raise_exception=True, exception_to_raise=TelemostApiForbidden('ConferenceNotAvailable')).start()
        with self.specified_client(scopes=TelemostPermission.scopes, uid=self.staff_uid), mock.patch(
                'mpfs.platform.v1.telemost.handlers.PLATFORM_TELEMOST_API_ENABLED', True):
            resp = self.client.request(self.method,
                                       self.url % urllib.quote('https://telemost.yandex.ru/j/12345678901234', ''))

        assert resp.status_code == 403

    def test_telemost_api_bad_request(self):
        TelemostApiStub(raise_exception=True, exception_to_raise=TelemostApiBadRequest('BadUri')).start()
        with self.specified_client(scopes=TelemostPermission.scopes, uid=self.staff_uid), mock.patch(
                'mpfs.platform.v1.telemost.handlers.PLATFORM_TELEMOST_API_ENABLED', True):
            resp = self.client.request(self.method,
                                       self.url % urllib.quote('https://telemost.yandeyx.ru/j/12345678901234', ''))

        assert resp.status_code == 404

    def test_telemost_api_gone(self):
        TelemostApiStub(raise_exception=True, exception_to_raise=TelemostApiGone('ConferenceLinkExpired')).start()
        with self.specified_client(scopes=TelemostPermission.scopes, uid=self.staff_uid), mock.patch(
                'mpfs.platform.v1.telemost.handlers.PLATFORM_TELEMOST_API_ENABLED', True):
            resp = self.client.request(self.method,
                                       self.url % urllib.quote('https://telemost.yandeyx.ru/j/12345678901234', ''))

        assert resp.status_code == 410

    def test_display_name_when_yt_user(self):
        user_info_from_blackbox = {'attributes': {},
                                   'dbfields': {'userinfo.lang.uid': 'en',
                                                'userinfo.firstname.uid': u'Хомячок',
                                                'userinfo.lastname.uid': u'Пушистянович'}}
        TelemostApiStub(get_response={'uri': 'https://telemost.yandex.ru/j/12345678901234',
                                      'conf_id': 'conf_id_stub',
                                      'conf_pwd': 'conf_pwd_stub',
                                      'limit_type': XMPPLimitTypes.STAFF.upper(),
                                      'client_configuration': from_json('{}')}).start()
        platform_user = PlatformUser()
        platform_user.uid = to_yateam_uid(self.uid)
        platform_user.details = PlatformUserDetailsHelper.construct_details_from(user_info_from_blackbox)
        with self.specified_client(scopes=TelemostPermission.scopes, platform_user=platform_user), \
             mock.patch('mpfs.platform.v1.telemost.handlers.PLATFORM_TELEMOST_API_ENABLED', True):
            resp = self.client.request(self.method,
                                       self.url % urllib.quote('https://telemost.yandex.ru/j/12345678901234', ''))

            assert resp.status_code == 200
            data = from_json(resp.content)
            assert data['display_name'] == user_info_from_blackbox['dbfields']['userinfo.firstname.uid']

    def test_telemost_api_joined(self):
        TelemostApiStub(get_response={'uri': 'https://telemost.yandex.ru/j/12345678901234',
                                      'conf_id': 'conf_id_stub',
                                      'conf_pwd': 'conf_pwd_stub',
                                      'limit_type': XMPPLimitTypes.STAFF.upper(),
                                      'client_configuration': from_json('{}')}).start()
        with self.specified_client(scopes=TelemostPermission.scopes, uid=self.staff_uid), mock.patch(
                'mpfs.platform.v1.telemost.handlers.PLATFORM_TELEMOST_API_ENABLED', True):
            resp = self.client.request(self.method,
                                       self.url % urllib.quote('https://telemost.yandex.ru/j/12345678901234', ''))

        assert resp.status_code == 200
        data = from_json(resp.content)
        assert data['uri'] == 'https://telemost.yandex.ru/j/12345678901234'
        assert data['conf_id'] == 'conf_id_stub'
        assert data['conf_pwd'] == 'conf_pwd_stub'
        assert data['xmpp_password'] == PLATFORM_TELEMOST_XMPP_USERS[XMPPLimitTypes.STAFF]['password']
        assert data['xmpp_user_id'] == PLATFORM_TELEMOST_XMPP_USERS[XMPPLimitTypes.STAFF]['user_id']


class V2CreateConferenceTestCase(UserTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v2'
    method = 'POST'
    url = 'telemost/conferences'
    staff_uid = users.user_1.uid
    telemost_api_400_resp = Response()

    def setup_method(self, method):
        super(V2CreateConferenceTestCase, self).setup_method(method)
        users_info.update_info_by_uid(self.staff_uid, has_staff=True, is_2fa_enabled=True)

        request = Request('POST', 'https://telemost.dst.yandex.ru/v2/conferences',
                          params='uid=%s' % self.uid, data=None, headers={})
        request.body = None

        self.success_telemsot_api_resp = Response()
        self.success_telemsot_api_resp.status_code = 201
        self.success_telemsot_api_resp.request = request
        self.success_telemsot_api_resp._content = to_json(CreatedConfData.DEFAULT)

        self.telemost_api_400_resp.status_code = 400
        self.telemost_api_400_resp.request = request
        self.telemost_api_400_resp._content = to_json(CreatedConfData.BAD_REQUEST)

    def test_default(self):
        expected_data = CreatedConfData.DEFAULT
        self.success_telemsot_api_resp._content = to_json(expected_data)

        with self.specified_client(scopes=TelemostPermission.scopes), \
             mock.patch('requests.Session.send',
                        return_value=self.success_telemsot_api_resp):
            resp = self.client.request(self.method, self.url)

        assert resp.status_code == 201
        data = from_json(resp.content)
        assert data['uri'] == expected_data['uri']
        assert data['ws_uri'] == expected_data['ws_uri']
        assert data['room_id'] == expected_data['room_id']
        assert data['safe_room_id'] == expected_data['safe_room_id']
        assert data['peer_id'] == expected_data['peer_id']
        assert data['client_configuration'] == expected_data['client_configuration']

    @parameterized.expand([
        ('default', None, 'ru'),
        ('en', 'en', 'en'),
        ('ru', 'ru', 'ru'),
        ('complex', 'en-US,en;q=0.9,ru;q=0.8', 'en'),
    ])
    def test_headers(self, case_name, lang, expected_lang):
        client_instance_id = 'client_instance_id'
        headers = {}
        if lang is not None:
            headers['Accept-Language'] = lang
        headers['Client-Instance-Id'] = client_instance_id

        with self.specified_client(scopes=TelemostPermission.scopes), \
             mock.patch('mpfs.core.services.telemost_api_service.TelemostApiService.request') as mocked_telemost_api:
            self.client.request(self.method, self.url, headers=headers)

        url = mocked_telemost_api.call_args[0][1]
        query_parameters = urlparse.parse_qs(urlparse.urlparse(url).query)
        lang_values = query_parameters['lang']

        assert lang_values
        actual_lang = lang_values[0]
        assert actual_lang == expected_lang
        assert client_instance_id == query_parameters['client_instance_id'][0]

    def test_no_headers(self):
        headers = {}

        with self.specified_client(scopes=TelemostPermission.scopes), \
             mock.patch('mpfs.core.services.telemost_api_service.TelemostApiService.request') as mocked_telemost_api:
            self.client.request(self.method, self.url, headers=headers)

        url = mocked_telemost_api.call_args[0][1]
        query_parameters = urlparse.parse_qs(urlparse.urlparse(url).query)

        assert 'client_instance_id' not in query_parameters

    def test_no_user_id(self):
        expected_data = CreatedConfData.WO_USER_ID
        self.success_telemsot_api_resp._content = to_json(expected_data)

        with self.specified_client(scopes=TelemostPermission.scopes), \
             mock.patch('requests.Session.send',
                        return_value=self.success_telemsot_api_resp):
            resp = self.client.request(self.method, self.url)

        assert resp.status_code == 201
        data = from_json(resp.content)
        assert data['uri'] == expected_data['uri']
        assert data['ws_uri'] == expected_data['ws_uri']
        assert data['room_id'] == expected_data['room_id']
        assert 'peer_id' not in data

    def test_no_client_config(self):
        expected_data = CreatedConfData.WO_CLIENT_CONFIG
        self.success_telemsot_api_resp._content = to_json(expected_data)

        with self.specified_client(scopes=TelemostPermission.scopes), \
             mock.patch('requests.Session.send',
                        return_value=self.success_telemsot_api_resp):
            resp = self.client.request(self.method, self.url)

        assert resp.status_code == 201
        data = from_json(resp.content)
        assert data['uri'] == expected_data['uri']
        assert data['ws_uri'] == expected_data['ws_uri']
        assert data['room_id'] == expected_data['room_id']
        assert data['peer_id'] == expected_data['peer_id']
        assert 'client_configuration' not in data

    def test_staff_only(self):
        expected_data = CreatedConfData.DEFAULT
        self.success_telemsot_api_resp._content = to_json(expected_data)

        with self.specified_client(scopes=TelemostPermission.scopes), \
             mock.patch('requests.Session.send',
                        return_value=self.success_telemsot_api_resp):
            resp = self.client.request(self.method, self.url,
                                       data={'staff_only': True},
                                       headers={'Content-Type': 'application/json'})

        assert resp.status_code == 201
        data = from_json(resp.content)
        assert data['uri'] == expected_data['uri']
        assert data['ws_uri'] == expected_data['ws_uri']
        assert data['room_id'] == expected_data['room_id']
        assert data['peer_id'] == expected_data['peer_id']
        assert data['peer_token'] == expected_data['peer_token']
        assert data['media_session_id'] == expected_data['media_session_id']

    @parameterized.expand([
        ('bad-request', telemost_api_400_resp, 400),
    ])
    def test_error_handling(self, case_name, resp_from_telemost_api, expected_status):
        with self.specified_client(scopes=TelemostPermission.scopes), \
             mock.patch('requests.Session.send',
                        return_value=resp_from_telemost_api):
            resp = self.client.request(self.method, self.url)

        assert resp.status_code == expected_status

    def test_permissions(self):
        scopes_to_status = (
            ([], 403),
            (TelemostConferenceCreatePermission.scopes, 201),
            (WebDavPermission.scopes, 201),
            (TelemostPermission.scopes, 201),
            (MobileMailPermission.scopes, 201),
        )

        with TelemostApiRawResponseStub():
            self._permissions_test(scopes_to_status, self.method, self.url)


class V2GetUserInfoTestCase(UserTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v2'
    method = 'GET'
    url = 'telemost/users/me'

    def test_default(self):
        expected_data = UserInfoData.DEFAULT
        expected_login = 'homyachok'

        with self.specified_client(scopes=TelemostPermission.scopes, login=expected_login), \
             mock.patch('requests.Session.send',
                        return_value=construct_requests_resp(content=expected_data)):
            resp = self.client.request(self.method, self.url)

        assert resp.status_code == 200
        data = from_json(resp.content)
        assert data['uid'] == expected_data['uid']
        assert data['display_name'] == expected_data['display_name']
        assert data['avatar_url'] == expected_data['avatar_url']
        assert data['login'] == expected_login
        assert data['is_yandex_staff'] == expected_data['is_yandex_staff']

    def test_wo_avatar_url(self):
        expected_data = UserInfoData.WO_AVATAR_URL

        with self.specified_client(scopes=TelemostPermission.scopes), \
             mock.patch('requests.Session.send',
                        return_value=construct_requests_resp(content=expected_data)):
            resp = self.client.request(self.method, self.url)

        assert resp.status_code == 200
        data = from_json(resp.content)
        assert data['uid'] == expected_data['uid']
        assert data['display_name'] == expected_data['display_name']
        assert data['is_yandex_staff'] == expected_data['is_yandex_staff']
        assert 'avatar_url' not in data

    def test_wo_uid(self):
        expected_data = UserInfoData.WO_UID

        with self.specified_client(scopes=TelemostPermission.scopes), \
             mock.patch('requests.Session.send',
                        return_value=construct_requests_resp(content=expected_data)):
            resp = self.client.request(self.method, self.url)

        assert resp.status_code == 200
        data = from_json(resp.content)
        assert data['avatar_url'] == expected_data['avatar_url']
        assert data['display_name'] == expected_data['display_name']
        assert data['is_yandex_staff'] == expected_data['is_yandex_staff']
        assert 'uid' not in data


class V2GetConnectionInfoTestCase(UserTestCaseMixin, DiskApiTestCase):
    # TODO switch to EXTERNAL
    api_mode = tags.platform.INTERNAL
    api_version = 'v2'
    method = 'GET'
    url = 'telemost/conferences/%s/connection'
    JOIN_URL = urllib.quote('https://telemost.yandex.ru/j/homyak', safe='')

    def test_default(self):
        expected_data = CreatedConfData.DEFAULT

        with self.specified_client(scopes=TelemostPermission.scopes), \
             mock.patch('requests.Session.send',
                        return_value=construct_requests_resp(content=expected_data)):
            resp = self.client.request(self.method, self.url % self.JOIN_URL)

        assert resp.status_code == 200
        data = from_json(resp.content)
        assert data['uri'] == expected_data['uri']
        assert data['ws_uri'] == expected_data['ws_uri']
        assert data['room_id'] == expected_data['room_id']
        assert data['safe_room_id'] == expected_data['safe_room_id']
        assert data['peer_id'] == expected_data['peer_id']
        assert data['client_configuration'] == expected_data['client_configuration']

    @parameterized.expand([
        ('default', None, 'ru'),
        ('en', 'en', 'en'),
        ('ru', 'ru', 'ru'),
    ])
    def test_headers(self, case_name, lang, expected_lang):
        client_instance_id = 'client_instance_id'
        headers = {}
        if lang is not None:
            headers['Accept-Language'] = lang
        headers['Client-Instance-Id'] = client_instance_id

        with self.specified_client(scopes=TelemostPermission.scopes), \
             mock.patch('mpfs.core.services.telemost_api_service.TelemostApiService.request') as mocked_telemost_api:
            self.client.request(self.method, self.url % self.JOIN_URL, headers=headers)

        url = mocked_telemost_api.call_args[0][1]
        query_parameters = urlparse.parse_qs(urlparse.urlparse(url).query)
        lang_values = query_parameters['lang']

        assert lang_values
        actual_lang = lang_values[0]
        assert actual_lang == expected_lang
        assert client_instance_id == query_parameters['client_instance_id'][0]

    def test_no_headers(self):
        headers = {}

        with self.specified_client(scopes=TelemostPermission.scopes), \
             mock.patch('mpfs.core.services.telemost_api_service.TelemostApiService.request') as mocked_telemost_api:
            self.client.request(self.method, self.url % self.JOIN_URL, headers=headers)

        url = mocked_telemost_api.call_args[0][1]
        query_parameters = urlparse.parse_qs(urlparse.urlparse(url).query)

        assert 'client_instance_id' not in query_parameters

    def test_not_passing_params(self):
        expected_data = CreatedConfData.DEFAULT

        with self.specified_client(uid=None), \
             mock.patch('requests.Session.send',
                        return_value=construct_requests_resp(content=expected_data)), \
             mock.patch(
                 'mpfs.platform.v2.telemost.handlers.GetConferenceConnectionHandler.request_service'
             ) as mocked_request_services:
            self.client.request(self.method, self.url % self.JOIN_URL)

            actual_url = mocked_request_services.call_args[0][0]

        assert 'uid=' not in actual_url
        assert 'display_name=' not in actual_url

    def test_passing_params(self):
        expected_data = CreatedConfData.DEFAULT
        display_name = u'Пушистая Лиса'

        with self.specified_client(scopes=TelemostPermission.scopes), \
             mock.patch('requests.Session.send',
                        return_value=construct_requests_resp(content=expected_data)), \
             mock.patch(
                 'mpfs.platform.v2.telemost.handlers.GetConferenceConnectionHandler.request_service'
             ) as mocked_request_services:
            self.client.request(self.method, u'%s?display_name=%s' % (self.url % self.JOIN_URL, display_name))

            actual_url = mocked_request_services.call_args[0][0]

        assert 'uid=%s' % self.uid in actual_url
        assert u'display_name=%s' % urllib.quote_plus(display_name.encode('utf-8'), safe='') in actual_url

    def test_passing_params_with_yandex_team(self):
        expected_data = CreatedConfData.DEFAULT
        expected_display_name = u'Хомячочек'

        with self.specified_client(uid=to_yateam_uid(self.uid),
                                   scopes=TelemostPermission.scopes,
                                   user_details={'firstname': expected_display_name}), \
             mock.patch('requests.Session.send',
                        return_value=construct_requests_resp(content=expected_data)), \
             mock.patch(
                 'mpfs.platform.v2.telemost.handlers.GetConferenceConnectionHandler.request_service'
             ) as mocked_request_services:
            self.client.request(self.method, self.url % self.JOIN_URL)

            actual_url = mocked_request_services.call_args[0][0]

        assert u'display_name=%s' % urllib.quote_plus(expected_display_name.encode('utf-8'), safe='') in actual_url

    def test_non_auth(self):
        expected_data = CreatedConfData.DEFAULT

        with self.specified_client(uid=None), \
             mock.patch('requests.Session.send',
                        return_value=construct_requests_resp(content=expected_data)):
            resp = self.client.request(self.method, self.url % self.JOIN_URL)

        assert resp.status_code == 200
        data = from_json(resp.content)
        assert data['uri'] == expected_data['uri']
        assert data['ws_uri'] == expected_data['ws_uri']
        assert data['room_id'] == expected_data['room_id']
        assert data['peer_id'] == expected_data['peer_id']
        assert data['peer_token'] == expected_data['peer_token']
        assert data['media_session_id'] == expected_data['media_session_id']

    def test_expired_link(self):
        expected_data = CreatedConfData.GONE

        with self.specified_client(uid=None), \
             mock.patch('requests.Session.send',
                        return_value=construct_requests_resp(status=410, content=expected_data)):
            resp = self.client.request(self.method, self.url % self.JOIN_URL)

        assert resp.status_code == 410

    def test_link_not_come(self):
        expected_data = CreatedConfData.NOT_COME

        with self.specified_client(uid=None), \
            mock.patch('requests.Session.send',
                       return_value=construct_requests_resp(status=403, content=expected_data)):
            resp = self.client.request(self.method, self.url % self.JOIN_URL)

        assert resp.status_code == 403
        data = from_json(resp.content)
        assert data['error'] == 'TelemostInviteLinkNotComeError'
        assert 'start_event_time' in data
        assert data['start_event_time']

    def test_stream_not_started(self):
        expected_data = CreatedConfData.STREAM_NOT_STARTED

        with self.specified_client(uid=None), \
            mock.patch('requests.Session.send',
                       return_value=construct_requests_resp(status=403, content=expected_data)):
            resp = self.client.request(self.method, self.url % self.JOIN_URL)

        assert resp.status_code == 403
        data = from_json(resp.content)
        assert data['error'] == 'TelemostStreamNotStartedError'

    def test_forbidden_access_to_private_conference(self):
        expected_data = CreatedConfData.FORBIDDEN_TO_PRIVATE

        with self.specified_client(uid=None), \
            mock.patch('requests.Session.send',
                       return_value=construct_requests_resp(status=403, content=expected_data)):
            resp = self.client.request(self.method, self.url % self.JOIN_URL)

        assert resp.status_code == 403
        data = from_json(resp.content)
        assert data['error'] == 'TelemostForbiddenToPrivateError'

    def test_not_found(self):
        expected_data = CreatedConfData.NOT_FOUND

        with self.specified_client(uid=None), \
             mock.patch('requests.Session.send',
                        return_value=construct_requests_resp(status=404, content=expected_data)):
            resp = self.client.request(self.method, self.url % self.JOIN_URL)

        assert resp.status_code == 404

    def test_403_without_error_name(self):
        expected_data = CreatedConfData.WO_ERROR_NAME

        with self.specified_client(uid=None), \
             mock.patch('requests.Session.send',
                        return_value=construct_requests_resp(status=403, content=expected_data)):
            resp = self.client.request(self.method, self.url % self.JOIN_URL)

        assert resp.status_code == 403

    def test_yandex_team_401(self):
        expected_data = ConfShortInfoData.FAILED_TO_ACCESS_YANDEX_TEAM_CONF

        with self.specified_client(uid=None), \
             mock.patch('requests.Session.send',
                        return_value=construct_requests_resp(status=403, content=expected_data)):
            resp = self.client.request(self.method, self.url % self.JOIN_URL, headers={'User-Agent': 'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.111 Safari/537.36'})

        assert resp.status_code == 401
        data = from_json(resp.content)
        assert data['error'] == 'TelemostUnauthorizedError'
        assert 'yandex_team_join_link' in data
        assert data['yandex_team_join_link']

    @parameterized.expand([
        ('desktop', 'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.111 Safari/537.36', True),
        ('yandex_search', 'Mozilla/5.0 (Linux; Android 8.0.0; LLD-L31) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.5 BroPP/1.0 Mobile Safari/537.36 YandexSearch/20.74', False),
        ('mobile_safari', 'Mozilla/5.0 (BB10; Touch) AppleWebKit/537.35+ (KHTML, like Gecko) Version/10.3.2.2876 Mobile Safari/537.35+', False)
    ])
    def test_yandex_team_forbidden(self, case_name, user_agent_header, should_contain_yt_link):
        expected_data = ConfShortInfoData.FAILED_TO_ACCESS_YANDEX_TEAM_CONF
        YANDEX_TEAM_CONF_KEY_LENGTH = 38
        join_url = urllib.quote(
            'http://dummy.ya.net/d/' + '7' * YANDEX_TEAM_CONF_KEY_LENGTH + '?yt-token=secret_shared_from_my_friend',
            safe=''
        )

        with self.specified_client(scopes=TelemostPermission.scopes), \
             mock.patch('requests.Session.send',
                        return_value=construct_requests_resp(status=403, content=expected_data)):
            resp = self.client.request(self.method, self.url % join_url, headers={'User-Agent': user_agent_header})

        assert resp.status_code == 403
        data = from_json(resp.content)
        if should_contain_yt_link:
            assert 'yandex_team_join_link' in data
            assert data['yandex_team_join_link']
        else:
            assert 'yandex_team_join_link' not in data


class V1GetAuthorizationURITestCase(UserTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.INTERNAL
    api_version = 'v1'
    method = 'GET'
    url = 'telemost/yandex-team/conferences/%s/authorize'
    JOIN_URL = urllib.quote('https://telemost.yandex.ru/j/homyak', safe='')

    def test_default(self):
        expected_data = AuthURIData.DEFAULT

        with self.specified_client(scopes=TelemostPermission.scopes), \
                 TelemostApiRawResponseStub(content=expected_data):
            resp = self.client.request(self.method, self.url % self.JOIN_URL)

        assert resp.status_code == 200
        data = from_json(resp.content)
        assert len(data) == 1
        assert data['uri'] == expected_data['uri']

    def test_return_redirect(self):
        expected_data = AuthURIData.DEFAULT

        with self.specified_client(scopes=TelemostPermission.scopes), \
             TelemostApiRawResponseStub(content=expected_data):
            resp = self.client.request(self.method, self.url % self.JOIN_URL + '?return_redirect=true')

        assert resp.status_code == 302
        assert resp.headers['Location'] == expected_data['uri']

    @parameterized.expand([
        ('yandex_search', 'Mozilla/5.0 (Linux; Android 8.0.0; LLD-L31) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.5 BroPP/1.0 Mobile Safari/537.36 YandexSearch/20.74'),
        ('mobile_safari', 'Mozilla/5.0 (BB10; Touch) AppleWebKit/537.35+ (KHTML, like Gecko) Version/10.3.2.2876 Mobile Safari/537.35+')
    ])
    def test_return_redirect_to_telemost_for_touch_without_authorization(self, case_name, user_agent):
        expected_data = AuthURIData.DEFAULT

        with self.specified_client(uid=None), \
                 TelemostApiRawResponseStub(content=expected_data):
            resp = self.client.request(self.method,
                                       self.url % self.JOIN_URL + '?return_redirect=true',
                                       headers={'User-Agent': user_agent})

        assert resp.status_code == 302
        assert 'passport.yandex-team.ru' not in resp.headers['Location']
        assert resp.headers['Location'] == expected_data['uri']

    @parameterized.expand([
        ('desktop_ff', 'Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:80.0) Gecko/20100101 Firefox/80.0'),
        ('desktop_yabro', 'Mozilla/5.0 (Windows NT 6.2; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/84.0.4147.105 YaBrowser/20.8.1.79 Yowser/2.5 Safari/537.36')
    ])
    def test_return_redirect_to_passport_auth(self, case_name, user_agent):
        with self.specified_client(uid=None):
            resp = self.client.request(self.method,
                                       self.url % self.JOIN_URL + '?return_redirect=true',
                                       headers={'User-Agent': user_agent})

        assert resp.status_code == 302
        assert self.JOIN_URL in resp.headers['Location']
        assert 'passport.yandex-team.ru' in resp.headers['Location']

    @parameterized.expand([
        ('default', ''),
        ('redirect', '?return_redirect=true')
    ])
    def test_404(self, case_name, params):
        expected_data = AuthURIData.NOT_FOUND

        with self.specified_client(scopes=TelemostPermission.scopes), \
             TelemostApiRawResponseStub(status=404, content=expected_data):
            resp = self.client.request(self.method, self.url % self.JOIN_URL + params)

        assert resp.status_code == 404

    @parameterized.expand([
        ('default', ''),
        ('redirect', '?return_redirect=true')
    ])
    def test_410(self, case_name, params):
        expected_data = AuthURIData.GONE

        with self.specified_client(scopes=TelemostPermission.scopes), \
             TelemostApiRawResponseStub(status=410, content=expected_data):
            resp = self.client.request(self.method, self.url % self.JOIN_URL + params)

        assert resp.status_code == 410


class V2GetPeersTestCase(UserTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v2'
    method = 'PUT'
    url = 'telemost/conferences/%s/peers'
    JOIN_URL = urllib.quote('https://telemost.yandex.ru/j/homyak', safe='')

    def test_default(self):
        expected_data = PeersData.DEFAULT

        with self.specified_client(scopes=TelemostPermission.scopes), \
             mock.patch('requests.Session.send',
                        return_value=construct_requests_resp(content=expected_data, request_method='PUT')):
            resp = self.client.request(self.method, self.url % self.JOIN_URL, data={'peers_ids': ['meow', 'phyr']})

        assert resp.status_code == 200
        data = from_json(resp.content)
        assert len(data['items']) == len(expected_data['items'])
        for i, peer_info in enumerate(data['items']):
            assert peer_info['uid'] == expected_data['items'][i]['uid']
            assert peer_info['display_name'] == expected_data['items'][i]['display_name']
            assert peer_info['avatar_url'] == expected_data['items'][i]['avatar_url']
            assert peer_info['peer_id'] == expected_data['items'][i]['peer_id']

    def test_wo_optional(self):
        expected_data = PeersData.WO_OPTIONAL

        with self.specified_client(scopes=TelemostPermission.scopes), \
             mock.patch('requests.Session.send',
                        return_value=construct_requests_resp(content=expected_data, request_method='PUT')):
            resp = self.client.request(self.method, self.url % self.JOIN_URL, data={'peers_ids': ['meow', 'phyr']})

        assert resp.status_code == 200
        data = from_json(resp.content)
        assert len(data['items']) == len(expected_data['items'])
        for i, peer_info in enumerate(data['items']):
            assert 'uid' not in expected_data['items'][i]
            assert peer_info['display_name'] == expected_data['items'][i]['display_name']
            assert 'avatar_url' not in expected_data['items'][i]
            assert peer_info['peer_id'] == expected_data['items'][i]['peer_id']

    def test_expired_link(self):
        expected_data = PeersData.GONE

        with self.specified_client(uid=None), \
             mock.patch('requests.Session.send',
                        return_value=construct_requests_resp(status=410, content=expected_data, request_method='PUT')):
            resp = self.client.request(self.method, self.url % self.JOIN_URL, data={'peers_ids': ['meow', 'phyr']})

        assert resp.status_code == 410

    def test_empty_body(self):
        with self.specified_client(uid=None):
            resp = self.client.request(self.method, self.url % self.JOIN_URL)

        assert resp.status_code == 400

    def test_wrong_body(self):
        with self.specified_client(uid=None):
            resp = self.client.request(self.method, self.url % self.JOIN_URL, data={'wrong_field': ['yozh']})

        assert resp.status_code == 400

    def test_not_found(self):
        expected_data = PeersData.NOT_FOUND

        with self.specified_client(uid=None), \
             mock.patch('requests.Session.send',
                        return_value=construct_requests_resp(status=404, content=expected_data, request_method='PUT')):
            resp = self.client.request(self.method, self.url % self.JOIN_URL, data={'peers_ids': ['meow', 'phyr']})

        assert resp.status_code == 404


class V2GetConferenceInfoTestCase(UserTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v2'
    method = 'GET'
    url = 'telemost/conferences/%s'
    JOIN_URL = urllib.quote('https://telemost.yandex.ru/j/homyak', safe='')
    YANDEX_TEAM_CONF_KEY_LENGTH = 38

    def test_default(self):
        expected_data = ConfShortInfoData.DEFAULT

        with self.specified_client(scopes=TelemostPermission.scopes), \
            mock.patch('requests.Session.send',
                       return_value=construct_requests_resp(content=expected_data, request_method='PUT')):
            resp = self.client.request(self.method, self.url % self.JOIN_URL)

        assert resp.status_code == 200
        data = from_json(resp.content)
        assert data['uri'] == expected_data['uri']
        assert data['room_id'] == expected_data['room_id']
        assert data['safe_room_id'] == expected_data['safe_room_id']

    def test_expired_link(self):
        expected_data = ConfShortInfoData.GONE

        with self.specified_client(uid=None), \
             mock.patch('requests.Session.send',
                        return_value=construct_requests_resp(status=410, content=expected_data, request_method='PUT')):
            resp = self.client.request(self.method, self.url % self.JOIN_URL)

        assert resp.status_code == 410

    def test_not_found(self):
        expected_data = ConfShortInfoData.NOT_FOUND

        with self.specified_client(uid=None), \
             mock.patch('requests.Session.send',
                        return_value=construct_requests_resp(status=404, content=expected_data, request_method='PUT')):
            resp = self.client.request(self.method, self.url % self.JOIN_URL)

        assert resp.status_code == 404

    def test_yandex_team_401(self):
        expected_data = ConfShortInfoData.FAILED_TO_ACCESS_YANDEX_TEAM_CONF

        with self.specified_client(uid=None), \
             mock.patch('requests.Session.send',
                        return_value=construct_requests_resp(status=403, content=expected_data, request_method='PUT')):
            resp = self.client.request(self.method, self.url % self.JOIN_URL, headers={'User-Agent': 'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.111 Safari/537.36'})

        assert resp.status_code == 401
        data = from_json(resp.content)
        assert data['error'] == 'TelemostUnauthorizedError'
        assert 'yandex_team_join_link' in data
        assert data['yandex_team_join_link']

    def test_yandex_team_staff_only_forbidden(self):
        expected_data = ConfShortInfoData.FORBIDDEN_YANDEX_TEAM_CONF

        with self.specified_client(uid=None), \
             mock.patch('requests.Session.send',
                        return_value=construct_requests_resp(status=403, content=expected_data, request_method='PUT')):
            resp = self.client.request(self.method, self.url % self.JOIN_URL)

        assert resp.status_code == 403
        data = from_json(resp.content)
        assert data['error'] == 'TelemostYaTeamConferenceForbiddenError'

    @parameterized.expand([
        ('with_token', urllib.quote(
            'http://dummy.ya.net/d/' + '7' * YANDEX_TEAM_CONF_KEY_LENGTH + '?yt-token=secret_shared_from_my_friend',
            safe=''
        )),
        ('without_token', urllib.quote('http://dummy.ya.net/d/' + '3' * YANDEX_TEAM_CONF_KEY_LENGTH, safe=''))
    ])
    def test_yandex_team_forbidden(self, case_name, join_url):
        expected_data = ConfShortInfoData.FAILED_TO_ACCESS_YANDEX_TEAM_CONF

        with self.specified_client(scopes=TelemostPermission.scopes), \
             mock.patch('requests.Session.send',
                        return_value=construct_requests_resp(status=403, content=expected_data, request_method='PUT')):
            resp = self.client.request(self.method, self.url % join_url, headers={'User-Agent': 'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.111 Safari/537.36'})

        assert resp.status_code == 403
        data = from_json(resp.content)
        assert 'yandex_team_join_link' in data
        assert data['yandex_team_join_link']


class PutTelemostFOSTestCase(UserTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.INTERNAL
    api_version = 'v1'
    method = 'PUT'
    url = 'telemost/clients/fos'

    def setup_method(self, method):
        super(PutTelemostFOSTestCase, self).setup_method(method)
        self.create_user(self.uid, noemail=1)

    def test_default(self):
        support_emails = settings.feedback['telemost_fos_email']
        with self.specified_client(uid=self.uid),\
            mock.patch('mpfs.core.queue.mpfs_queue.put') as mock_obj:

                app_version = 'test_version'
                os_version = 'test_os'
                subject = 'test_theme'
                support_text = 'what'
                recipient_type = 'testers'
                resp = self.client.request(self.method, self.url,
                                           query={
                                               'app_version': app_version,
                                               'reply_email': 'test@test.test',
                                               'os_version': os_version,
                                               'subject': subject,
                                               'recipient_type': recipient_type,
                                           },
                                           data={'fos_support_text': support_text, 'debug_info': '123'},
                                           headers={'Content-Type': 'application/json'})
                assert resp
                assert resp.status_code == 200
                data = None
                for call in mock_obj.call_args_list:
                    if call[0][-1] == 'send_email':
                        data = call[0][0]
                assert data
                assert not data.viewkeys() ^ {'email_to', 'template_name', 'sender_name', 'sender_email', 'template_args'}
                assert not data['template_args'].viewkeys() ^ {'body', 'subject'}
                assert data['template_args']['subject'] == '%s - %s - %s' % (os_version, app_version, subject)
                assert support_text in data['template_args']['body']
                assert os_version in data['template_args']['body']
                assert app_version in data['template_args']['body']
                assert data['email_to'] == support_emails[recipient_type]

    def test_without_uid(self):
        with self.specified_client(uid=None):
            resp = self.client.request(self.method, self.url,
                                       query={
                                           'app_version': 'test_version',
                                           'reply_email': 'test@test.test',
                                           'os_version': 'test_os',
                                           'subject': 'test_theme',
                                           'recipient_type': 'testers',
                                       },
                                       data={'fos_support_text': 'what'},
                                       headers={'Content-Type': 'application/json'})
        assert resp.status_code == 200

    @parameterized.expand([
        ('recipient_type', 'test@test.test', 'someone', {'fos_support_text': 'what'}),
        ('email', 'test@test@t.st', 'testers', {'fos_support_text': 'what'}),
        ('no_email', None, 'testers', {'fos_support_text': 'what'}),
    ])
    def test_invalid_data(self, test_name, reply_email, recipient_type, data):
        with self.specified_client(uid=None):
            resp = self.client.put(self.url,
                                   query={
                                       'app_version': 'test_version',
                                       'reply_email': reply_email,
                                       'expire_seconds': '1211222',
                                       'os_version': 'test_os',
                                       'subject': 'test_theme',
                                       'recipient_type': recipient_type,
                                   },
                                   data=data,
                                   headers={'Content-Type': 'application/json'})
            assert resp.status_code == 400
            assert 'FieldValidationError' in resp.content

    def test_send_no_support_text(self):
        with self.specified_client(uid=None):
            resp = self.client.put(self.url,
                                   query={
                                       'app_version': 'test_version',
                                       'reply_email': 'test@test.test',
                                       'expire_seconds': '1211222',
                                       'os_version': 'test_os',
                                       'subject': 'test_theme',
                                       'recipient_type': 'testers',
                                   },
                                   data={'fos_support_txt': 'what', 'debug_info': 'debug'},
                                   headers={'Content-Type': 'application/json'})
            assert resp.status_code == 400
            assert 'TelemostClientBadRequest' in resp.content


class V2LeaveTestCase(UserTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v2'
    method = 'POST'
    url = 'telemost/rooms/room_id/leave'
    query = {
                'peer_id': 'peer-id',
                'media_session_id': 'media-session-id',
                'peer_token': 'peer-token'
    }

    def test_default(self):
        expected_data = LeaveRoomData.DEFAULT

        with self.specified_client(uid=None), \
            mock.patch('requests.Session.send',
                       return_value=construct_requests_resp(status=200, content=expected_data, request_method=self.method)):
            resp = self.client.request(self.method, self.url, query=self.query)

        assert resp.status_code == 200

    def test_conflict(self):
        expected_data = LeaveRoomData.CONFLICT

        with self.specified_client(uid=None), \
             mock.patch('requests.Session.send',
                        return_value=construct_requests_resp(status=409, content=expected_data, request_method=self.method)):
            resp = self.client.request(self.method, self.url, query=self.query)

        assert resp.status_code == 409

    def test_not_found(self):
        expected_data = LeaveRoomData.NOT_FOUND

        with self.specified_client(uid=None), \
             mock.patch('requests.Session.send',
                        return_value=construct_requests_resp(status=404, content=expected_data, request_method=self.method)):
            resp = self.client.request(self.method, self.url, query=self.query)

        assert resp.status_code == 404


class V2GetUserFeatureTogglesTestCase(UserTestCaseMixin, DiskApiTestCase):

    api_mode = tags.platform.INTERNAL
    api_version = 'v2'
    method = 'GET'
    url = 'telemost/clients/features'
    telemost_uid = users.user_7.uid

    def test_default(self):
        with self.specified_client(uid=self.telemost_uid):
            resp = self.client.request(self.method, self.url)

        self.assertStatusCodeEqual(resp.status_code, 200)
        data = from_json(resp.content)

        fields = {'promote_mail360'}
        assert not data.viewkeys() ^ fields
        for value in data.itervalues():
            assert not {'enabled'} ^ value.viewkeys()

        assert data['promote_mail360']['enabled'] is True

    def test_with_promote_mail360(self):
        users_info.update_info_by_uid(self.telemost_uid, has_mail360=True)

        with self.specified_client(uid=self.telemost_uid):
            resp = self.client.request(self.method, self.url)

        self.assertStatusCodeEqual(resp.status_code, 200)
        data = from_json(resp.content)

        assert data['promote_mail360']['enabled'] is False


# ===============================================================================
# Broadcasts
# ===============================================================================

class V2CreateBroadcastTestCase(UserTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v2'
    method = 'PUT'
    url = 'telemost/conferences/%s/broadcast'
    JOIN_URL = urllib.quote('https://telemost.yandex.ru/j/homyak', safe='')

    def test_with_caption(self):
        expected_data = BroadcastData.DEFAULT

        with self.specified_client(scopes=TelemostPermission.scopes), \
            mock.patch('requests.Session.send',
                       return_value=construct_requests_resp(content=expected_data, request_method='PUT')):
            resp = self.client.request(self.method, self.url % self.JOIN_URL,
                                       data={'caption': 'Caption', 'description': 'Description'})

        assert resp.status_code == 200
        data = from_json(resp.content)
        assert data['broadcast_uri'] == expected_data['broadcast_uri']
        assert data['broadcast_chat_path'] == expected_data['broadcast_chat_path']

    def test_without_caption(self):
        expected_data = BroadcastData.DEFAULT

        with self.specified_client(scopes=TelemostPermission.scopes), \
            mock.patch('requests.Session.send',
                       return_value=construct_requests_resp(content=expected_data, request_method='PUT')):
            resp = self.client.request(self.method, self.url % self.JOIN_URL,
                                       data={})

        assert resp.status_code == 200
        data = from_json(resp.content)
        assert data['broadcast_uri'] == expected_data['broadcast_uri']
        assert data['broadcast_chat_path'] == expected_data['broadcast_chat_path']


class V2StreamsTestCase(UserTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v2'
    method = 'POST'
    url = 'telemost/conferences/%s/broadcast/%s/'
    JOIN_URL = urllib.quote('https://telemost.yandex.ru/j/homyak', safe='')
    BROADCAST_URI = urllib.quote('https://telemost.yandex.ru/live/homyak', safe='')

    def test_start(self):
        expected_data = StreamData.START
        self.url += 'start'

        with self.specified_client(scopes=TelemostPermission.scopes), \
            mock.patch('requests.Session.send',
                       return_value=construct_requests_resp(content=expected_data, request_method='POST')):
            resp = self.client.request(self.method, self.url % (self.JOIN_URL, self.BROADCAST_URI))

        assert resp.status_code == 200
        data = from_json(resp.content)
        assert data['owner_uid'] == expected_data['owner_uid']
        assert data['started_at'] == expected_data['started_at']
        assert data['ugc_live_slug'] == expected_data['ugc_live_slug']

    def test_stop(self):
        expected_data = StreamData.STOP
        self.url += 'stop'

        with self.specified_client(scopes=TelemostPermission.scopes), \
            mock.patch('requests.Session.send',
                       return_value=construct_requests_resp(content=expected_data, request_method='POST')) as send_mock:
            resp = self.client.request(self.method, self.url % (self.JOIN_URL, self.BROADCAST_URI))

        assert resp.status_code == 200
        data = from_json(resp.content)
        assert data['owner_uid'] == expected_data['owner_uid']
        assert data['started_at'] == expected_data['started_at']
        assert data['stopped_at'] == expected_data['stopped_at']

        request = send_mock.call_args[0][0]
        assert isinstance(request, PreparedRequest)

        assert 'uid=' in request.url

    def test_stop_from_translator(self):
        self.url += 'stop?uid=1&translator_token=token'

        with self.specified_client(uid=None, scopes=TelemostPermission.scopes):
            with mock.patch('requests.Session.send') as send_mock:
                self.client.request(self.method, self.url % (self.JOIN_URL, self.BROADCAST_URI))

        request = send_mock.call_args[0][0]
        assert isinstance(request, PreparedRequest)

        assert 'uid=' not in request.url
        assert 'translator_token=token' in request.url


class V2StreamsViewerTestCase(UserTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v2'
    method = 'GET'
    url = 'telemost/broadcast/%s/connection'
    BROADCAST_URI = urllib.quote('https://telemost.yandex.ru/live/homyak', safe='')

    def test_connection_data(self):
        expected_data = StreamConnectionData.DEFAULT

        with self.specified_client(scopes=TelemostPermission.scopes), \
            mock.patch('requests.Session.send',
                       return_value=construct_requests_resp(content=expected_data, request_method='GET')):
            resp = self.client.request(self.method, self.url % self.BROADCAST_URI)

        assert resp.status_code == 200
        data = from_json(resp.content)
        assert data['stream_uri'] == expected_data['stream_uri']


@mock.patch('mpfs.core.services.telemost_api_service.SEPARATE_TELEMOST_CLUSTERS',
            {'enabled': True,
             'enabled_for': ['128280859'],
             'percentage': 0,
             'conf_join_short_url_re': '^https:\/\/[^/]+/j\/(?P<conf_key>[^/?#]{3,40})((\?|#)(?P<params>[^/]*)?)?$',
             'fallback_hosts': ['fallback-telemost-api.dst.yandex.net'],
             'fallback_host_on_create': 'fallback-for-create-telemost-api.dst.yandex.net'})
class TCMTestCase(DiskTestCase):
    url = 'telemost/conferences/%s'
    YANDEX_TEAM_CONF_KEY_LENGTH = 38
    CONF_KEY_LENGTH = 14
    SHORT_JOIN_URL = 'https://telemost.yandex.ru/j/' + '3' * CONF_KEY_LENGTH
    exp_uid = '128280859'
    nonexp_uid = '66778801'

    @parameterized.expand([
        ('yt_with_token',
         'https://telemost.yandex.ru/j/' + '7' * YANDEX_TEAM_CONF_KEY_LENGTH + '?yt-token=secret_shared_from_my_friend'),
        ('short', SHORT_JOIN_URL)
    ])
    def test_join(self, case_name, join_url):
        expected_base_url = settings.services['TemplatedTelemostService']['base_url'] % from_json(TCMData.DEFAULT)['balancer_host']

        with TCMStub():
            telemost_service = TelemostViaTCMService.get_telemost_api(self.exp_uid, uri=join_url)

        assert isinstance(telemost_service, TemplatedTelemostService)
        assert telemost_service.base_url == expected_base_url

    def test_join_user_wo_exp(self):
        with TCMStub():
            telemost_service = TelemostViaTCMService.get_telemost_api(
                self.nonexp_uid,
                uri=self.SHORT_JOIN_URL
            )

        assert telemost_service is telemost_api

    def test_create_user_wo_exp(self):
        with TCMStub():
            telemost_service = TelemostViaTCMService.get_telemost_api_for_create(self.nonexp_uid)

        assert telemost_service is telemost_api

    def test_join_with_tcm_error(self):
        with TCMStub(side_effect=HTTPError('', FORBIDDEN, '', None, None)):
            telemost_service = TelemostViaTCMService.get_telemost_api(
                self.exp_uid,
                uri=self.SHORT_JOIN_URL
            )

        assert isinstance(telemost_service, MultiTelemostRequestsPoweredServiceBase)

    def test_create_with_tcm_error(self):
        expected_base_url = settings.services['TemplatedTelemostService']['base_url'] % 'fallback-for-create-telemost-api.dst.yandex.net'

        with TCMStub(side_effect=HTTPError('', FORBIDDEN, '', None, None)):
            telemost_service = TelemostViaTCMService.get_telemost_api_for_create(self.exp_uid)

        assert isinstance(telemost_service, TemplatedTelemostService)
        assert telemost_service.base_url == expected_base_url


class TelemostEnableBroadcastFeatureFanOutTestCase(DiskApiTestCase):
    api_mode = tags.platform.INTERNAL
    api_version = 'v2'
    method = 'PUT'
    url = 'telemost/billing/broadcast/enable'

    def test_enable(self):
        self.url = 'telemost/billing/broadcast/enable?uid=123'

        with self.specified_client(scopes=TelemostPermission.scopes), \
            mock.patch('mpfs.core.services.telemost_api_service.TelemostApiService.request',
                       return_value=construct_requests_resp(request_method='PUT', status=200)):
            resp = self.client.request(self.method, self.url)

        assert resp.status_code == 200

    def test_disable(self):
        self.url = 'telemost/billing/broadcast/disable?uid=123'

        with self.specified_client(scopes=TelemostPermission.scopes), \
            mock.patch('requests.Session.send',
                       return_value=construct_requests_resp(request_method='PUT', status=200)):
            resp = self.client.request(self.method, self.url)

        assert resp.status_code == 200
