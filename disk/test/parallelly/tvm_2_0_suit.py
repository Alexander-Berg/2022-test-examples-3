# -*- coding: utf-8 -*-
import json

import mock

from attrdict import AttrDict
from contextlib import contextmanager
from copy import deepcopy
from hamcrest import assert_that, all_of, contains_string
from nose_parameterized import parameterized
from ticket_parser2.api.v1.exceptions import TvmException

import mpfs
from mpfs.core.client import MPFSClient
from mpfs.core.services.discovery_service import DiscoveryService
from mpfs.core.zookeeper.hooks import SettingsChangeEvent
from mpfs.engine.process import get_tvm_2_0_user_ticket, setup_handlers_groups, setup_tvm_2_0_clients
from mpfs.frontend.api.disk import DiskApi
from test.base_suit import UserTestCaseMixin
from test.helpers.stubs.base import BaseStub
from test.helpers.stubs.manager import StubsManager
from test.helpers.stubs.services import PassportStub, HbfServiceStub
from test.parallelly.api.disk.base import DiskApiTestCase

from mpfs.common.static import tags
from mpfs.common.static.tags.tvm import TVM2_SILENT_MODE_LOG_PREFIX
from mpfs.common.static.tags.conf_sections import TVM2, TVM2_TICKETS_ONLY, TVM2_CLIENT_IDS
from mpfs.common.util import to_json, from_json
from mpfs.config import settings
from mpfs.core.event_dispatcher.dispatcher import EventDispatcher
from mpfs.core.services.passport_service import BlackboxService, Passport
from mpfs.core.services.tvm_2_0_service import tvm2, TVM2Ticket, TVM2Service
from mpfs.engine import process
from mpfs.platform.auth import ClientNetworks, TVM2Auth, TVM2TicketsOnlyAuth, OAuthAuth, PassportCookieAuth
from mpfs.platform.events import dispatcher
from mpfs.platform.v1.disk.handlers import MpfsProxyHandler
from mpfs.platform.v1.data.handlers import ListAppUsersHandler

SERVICES_TVM_2_0_CLIENT_ID = settings.services[TVM2]['client_id']
AUTH_CLIENTS_PLATFORM_PROD_TVM_2_0_CLIENT_IDS_0 = settings.auth['clients']['platform_prod'][TVM2][TVM2_CLIENT_IDS][0]
SERVICES_BLACKBOXSERVICE_TVM_2_0_CLIENT_ID = settings.services['BlackboxService'][TVM2]['client_id']


class TVM2NetworksMocksMixin(object):
    fake_settings = {'without_allowed_networks': {'auth': [{'name': 'ok-service',
                                                            'enabled': True,
                                                            'auth_methods': ['cookie', TVM2],
                                                            'allowed_origin_hosts': [
                                                                '^disk\.yandex\.(ru|ua|by|kz|com|com\.tr)$',
                                                                'yandex.net'],
                                                            'oauth_client_id': '123-test-123',
                                                            'oauth_client_name': 'internal-test',
                                                            'oauth_scopes': ['cloud_api:disk.read',
                                                                             'cloud_api:disk.write', 'yadisk:all'],
                                                            'tvm_2_0_client_ids': [SERVICES_TVM_2_0_CLIENT_ID]}]}}

    @classmethod
    def setup_class(cls):
        super(TVM2NetworksMocksMixin, cls).setup_class()
        cls.fake_settings['with_allowed_networks'] = deepcopy(cls.fake_settings['without_allowed_networks'])
        cls.fake_settings['with_allowed_networks']['auth'][0][ClientNetworks.ALLOWED_NETWORK_ADDRESSES] = [
            '2a02:6b8:c00::/40']
        cls.fake_settings['with_empty_allowed_networks'] = deepcopy(cls.fake_settings['without_allowed_networks'])
        cls.fake_settings['with_empty_allowed_networks']['auth'][0][ClientNetworks.ALLOWED_NETWORK_ADDRESSES] = []

        # Формируем диспатчер, который деграет callback'и только нашего класса
        cls.client_networks_dispatcher = EventDispatcher([SettingsChangeEvent])
        client_networks_observers = [observer
                                     for observer in dispatcher._observers[SettingsChangeEvent]
                                     if isinstance(observer.callback.im_self, ClientNetworks)]
        cls.client_networks_dispatcher._observers[SettingsChangeEvent] = client_networks_observers


class TestTVM2Base(DiskApiTestCase):
    # чтобы переполучить токен, нужно зайти по урлу https://oauth-test.yandex.ru/authorize?response_type=token&client_id=3474abd16abd4019b4af586bf1e7e717
    oauth_token = 'AgAAAACy1EoXAAAPDgbIOmBzkkBIlIboM25k2tI'
    token_uid = '3000257047'
    user_ticket = 'user_ticket:spam:unique_sign'
    service_ticket = 'service_ticket'
    remote_addr = '10.10.1.54'
    addr_with_tvm2_auth = '2a02:6b8:0:1466:a:a:a:a'
    addr_with_tvm2_signandts_auth = '2a02:6b8:c01:200:a:a:a:a'
    addr_without_auth = '192.168.0.19'

    @staticmethod
    def _get_service_ticket(dst_client_id):
        return tvm2.get_new_service_ticket(dst_client_id)

    @classmethod
    def _get_user_ticket(cls):
        blackbox = BlackboxService()
        response = blackbox.check_oauth_token(cls.oauth_token, '127.0.0.1')
        raw_ticket = response.get('user_ticket')
        return TVM2Ticket.build_tvm_ticket(raw_ticket)

    @contextmanager
    def mock_for_successful_tvm_auth(self, fake_settings):
        with mock.patch.dict(settings.platform, fake_settings), \
             mock.patch.object(MpfsProxyHandler, 'default_auth_methods', [TVM2TicketAuth()]), \
             mock.patch.object(MpfsProxyHandler, 'auth_methods', []), \
             mock.patch('mpfs.core.services.tvm_2_0_service.TVM2Service.validate_service_ticket',
                        return_value=AttrDict({'src': SERVICES_TVM_2_0_CLIENT_ID})), \
             mock.patch('mpfs.core.services.tvm_2_0_service.TVM2Service.validate_user_ticket',
                        return_value=AttrDict({'default_uid': self.token_uid, 'uids': [self.token_uid]})):
            # Триггерим событие ИзменениеНастроек, чтобы
            # перезапустить сбор разрешенных сетей для клиентов Платформы
            self.client_networks_dispatcher.send(SettingsChangeEvent())

            yield

    def mock_tvm_user_ticket(self, default_uid=None, uids=None):
        if default_uid is None:
            default_uid = self.token_uid
        if uids is None:
            uids = [self.token_uid]

        return mock.patch('mpfs.core.services.tvm_2_0_service.TVM2Service.validate_user_ticket',
                          return_value=AttrDict({'default_uid': long(default_uid), 'uids': [long(uid) for uid in uids]}))


class TestTVM2Setups(TestTVM2Base):
    """
    Тестирование сбора ключей

    Тесты ходят в живой TVM
    """

    def setup_method(self, method):
        super(TestTVM2Setups, self).setup_method(method)
        tvm2.__class__._tvm_service_context = None
        tvm2.__class__._tvm_user_context = None
        process.__tvm_2_0_tickets__ = {}

    @classmethod
    def teardown_class(cls):
        """Перенастраиваем TVM 2.0.

        Чтобы не аффектить сбитыми настройками другие тесты.
        """
        tvm2.update_public_keys()
        tvm2.update_service_tickets()
        super(TestTVM2Setups, cls).teardown_class()

    def test_update_tvm_pub_keys(self):
        assert tvm2._tvm_service_context is None
        assert tvm2._tvm_user_context is None
        tvm2.update_public_keys()
        assert tvm2._tvm_service_context
        assert tvm2._tvm_user_context

    def test_update_tvm_pub_keys_with_error_during_fetching_keys(self):
        assert tvm2._tvm_service_context is None
        assert tvm2._tvm_user_context is None
        with mock.patch('mpfs.core.services.tvm_2_0_service.TVM2Service.get_all_pub_keys', side_effect=Exception):
            tvm2.update_public_keys()
        assert tvm2._tvm_service_context is None
        assert tvm2._tvm_user_context is None

    def test_update_service_ticket(self):
        tvm2.update_public_keys()
        assert SERVICES_BLACKBOXSERVICE_TVM_2_0_CLIENT_ID not in process.__tvm_2_0_tickets__
        tvm2.update_service_tickets()
        assert process.get_tvm_2_0_service_ticket_for_client(SERVICES_BLACKBOXSERVICE_TVM_2_0_CLIENT_ID)

    def test_gets_new_ticket_if_missing(self):
        tvm2.update_public_keys()
        assert SERVICES_BLACKBOXSERVICE_TVM_2_0_CLIENT_ID not in process.__tvm_2_0_tickets__
        assert process.get_tvm_2_0_service_ticket_for_client(SERVICES_BLACKBOXSERVICE_TVM_2_0_CLIENT_ID)

    def test_if_service_ticket_expired_then_get_it_again(self):
        tvm2.update_public_keys()
        tvm2.update_service_tickets()
        old_ticket = 123
        process.__tvm_2_0_tickets__[SERVICES_BLACKBOXSERVICE_TVM_2_0_CLIENT_ID] = TVM2Ticket.build_tvm_ticket(old_ticket)
        with mock.patch.object(TVM2Ticket, 'is_expired', return_value=True):
            process.get_tvm_2_0_service_ticket_for_client(SERVICES_BLACKBOXSERVICE_TVM_2_0_CLIENT_ID)
        assert process.__tvm_2_0_tickets__[SERVICES_BLACKBOXSERVICE_TVM_2_0_CLIENT_ID].value() != old_ticket

    def test_validation_of_service_ticket_updates_public_keys_if_necessary(self):
        tvm2.update_public_keys()
        with mock.patch.object(TVM2Service, 'need_update_pub_keys', return_value=True), \
             mock.patch.object(TVM2Service, 'update_public_keys', wraps=tvm2.update_public_keys) as update_mock:
            try:
                tvm2.validate_service_ticket(TVM2Ticket.build_tvm_ticket(123))
            except Exception:
                pass
            assert update_mock.called

    def test_validation_of_user_ticket_updates_public_keys_if_necessary(self):
        tvm2.update_public_keys()
        with mock.patch.object(TVM2Service, 'need_update_pub_keys', return_value=True), \
             mock.patch.object(TVM2Service, 'update_public_keys', wraps=tvm2.update_public_keys) as update_mock:
            try:
                tvm2.validate_user_ticket(TVM2Ticket.build_tvm_ticket(123))
            except Exception:
                pass
            assert update_mock.called


class TestTVM2Service(TestTVM2Base):
    """
    Тестирование сервиса TVM 2.0

    Тесты ходят в живой TVM
    """
    api_version = 'v1'
    api_mode = tags.platform.EXTERNAL

    def test_get_service_ticket(self):
        assert self._get_service_ticket(SERVICES_TVM_2_0_CLIENT_ID).value()

    def test_validate_service_ticket(self):
        ticket = self._get_service_ticket(SERVICES_TVM_2_0_CLIENT_ID)
        assert tvm2.validate_service_ticket(ticket)

    def test_get_user_ticket(self):
        assert self._get_user_ticket().value()

    def test_validate_user_ticket(self):
        user_ticket = self._get_user_ticket()
        validated_ticket = tvm2.validate_user_ticket(user_ticket)
        assert validated_ticket is not None
        assert str(validated_ticket.default_uid) == self.token_uid

    def test_oauth_authentication_requests_user_ticket(self):
        passport_response = AttrDict({'content': to_json(
            {
                'oauth': {'uid': '3000257047', 'token_id': '1221631', 'device_id': '', 'device_name': '',
                          'scope': 'yadisk:all', 'client_id': 'fab64f0509144d6b8bbe222b54f1d220',
                          'client_name': 'disk-tester-testing', 'client_is_yandex': False},
                'uid': {'value': '3000257047'},
                'login': 'mpfs-test',
                'karma': {'value': 0},
                'status': {'value': 'VALID', 'id': 0}
            }
        )})

        header = {'Authorization': 'OAuth %s' % self.oauth_token}
        with mock.patch.object(MpfsProxyHandler, 'default_auth_methods', [OAuthAuth()]), \
             mock.patch.object(MpfsProxyHandler, 'auth_methods', []), \
             mock.patch('mpfs.core.services.common_service.RequestsPoweredServiceBase.request',
                        return_value=passport_response) as mocked_request:
            resp = self.client.get('disk/resources', query={'path': '/'}, uid=self.token_uid, headers=header)
            if resp.status_code != 200:
                print resp.content
                print mocked_request.call_args_list
            assert resp.status_code == 200
            bb_request = [x for x in mocked_request.call_args_list if x[0][1] == 'blackbox'][0]
            assert bb_request[1]['params']['get_user_ticket'] == 'yes'

    def test_cookie_authentication_requests_user_ticket(self):
        passport_response = AttrDict({'content': to_json(
            {'uid': {'value': self.token_uid, 'lite': False, 'hosted': False},
             'login': 'mpfs-test', 'karma': {'value': 0}, 'status': {'value': 'VALID', 'id': 0}}
        )})
        header = {'Cookie': 'Session_id=mocked_value', 'Host': '127.0.0.1', 'Origin': 'https://yandex.ru/maps'}
        with mock.patch.object(MpfsProxyHandler, 'default_auth_methods', [PassportCookieAuth()]), \
             mock.patch.object(MpfsProxyHandler, 'auth_methods', []), \
             mock.patch('authparser.Session.parse', return_value=1), \
             mock.patch('mpfs.core.services.passport_service.BlackboxServiceBase.request',
                        return_value=passport_response) as mocked_request:
            resp = self.client.get('disk/resources', query={'path': '/'}, uid=self.token_uid, headers=header)
            if resp.status_code != 200:
                print resp.content
                print mocked_request.call_args_list
            assert resp.status_code == 200
            bb_request = [x for x in mocked_request.call_args_list if x[0][1] == 'blackbox'][0]
            assert bb_request[1]['params']['get_user_ticket'] == 'yes'


class TestTVM2PlatformForwarding(TestTVM2Base, UserTestCaseMixin):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    stubs_manager = StubsManager(class_stubs=set(StubsManager.DEFAULT_CLASS_STUBS) - {PassportStub})

    def test_forward_tickets_to_external_service_from_platform(self):
        """
        Тестируем, что платформа прокидывает тикеты, если они был в запросе
        """
        with mock.patch.object(MpfsProxyHandler, 'default_auth_methods', [OAuthAuth()]), \
             mock.patch.object(MpfsProxyHandler, 'auth_methods', []), \
             mock.patch('mpfs.engine.process.get_tvm_2_0_service_ticket_for_client', return_value=TVM2Ticket.build_tvm_ticket(self.service_ticket)):
            header = {'Authorization': 'OAuth %s' % self.oauth_token, 'X-Ya-User-Ticket': self.user_ticket}
            with mock.patch('requests.sessions.Session.send', return_value={}) as mock_requests:
                self.client.get('disk/resources', query={'path': '/'}, uid=self.token_uid, headers=header)
                assert mock_requests.call_args[0][0].headers.get('X-Ya-User-Ticket') == self.user_ticket
                assert mock_requests.call_args[0][0].headers.get('X-Ya-Service-Ticket') == self.service_ticket


class TestTVM2MPFSForwarding(TestTVM2Base, UserTestCaseMixin):
    def test_forward_tickets_to_external_service_from_mpfs(self):
        self.create_user(self.uid)

        headers = {'X-Ya-User-Ticket': self.user_ticket}
        with open('fixtures/xml/mail_service1.xml') as f:
            mail_service_response = f.read()
        with open('fixtures/xml/discovery.xml') as fd:
            xml = fd.read()
        with mock.patch.object(DiscoveryService, 'open_url', return_value=xml):
            DiscoveryService().ensure_cache()


        with mock.patch('mpfs.engine.http.client.open_url', return_value={}) as mock_requests, \
             mock.patch('mpfs.core.services.mail_service.MailStidService.get_mail_stid', return_value='12345.6789'), \
             mock.patch('mpfs.core.services.mail_service.Mail.open_url', return_value=mail_service_response):
            mail_mid = '162129586585340230'
            mail_hid = '1.2'
            self.json_ok('office_action_data', {'uid': self.uid,
                                                'action': 'edit',
                                                'service_id': 'mail',
                                                'service_file_id': '%s/%s' % (mail_mid, mail_hid),
                                                'post_message_origin': 'https://disk.qa.yandex.ru',
                                                'size': '69120',
                                                'filename': 'test.xlsx',
                                                'ext': 'xlsx'}, headers=headers)
        # Берем тикет из сессии запроса выше
        ticket = get_tvm_2_0_user_ticket()

        assert ticket.value() == self.user_ticket


class ClientTVM2AuthTestCaseBase(UserTestCaseMixin, TestTVM2Base):
    api_version = 'v1'
    api_mode = tags.platform.INTERNAL
    auth_method = None

    def fake_settings(self):
        return {
            'auth': [
                {
                    'name': 'ok-service',
                    'enabled': True,
                    'auth_methods': [self.auth_method],
                    'allowed_origin_hosts': ['^disk\.yandex\.(ru|ua|by|kz|com|com\.tr)$', 'yandex.net'],
                    'oauth_client_id': '123-test-123',
                    'oauth_client_name': 'internal-test',
                    'oauth_scopes': ['cloud_api:disk.read', 'cloud_api:disk.write', 'yadisk:all'],
                    'tvm_2_0_client_ids': [SERVICES_TVM_2_0_CLIENT_ID],
                }
            ]
        }

    def auth_check(self, header, expected_code, uid_in_url=False, expected_uid=None):
        with mock.patch.dict(settings.platform, self.fake_settings()), \
            mock.patch.object(MpfsProxyHandler, 'default_auth_methods',
                              [TVM2Auth()] if self.auth_method == TVM2 else [TVM2TicketsOnlyAuth()]), \
            mock.patch.object(MpfsProxyHandler, 'auth_methods', []), \
            mock.patch('mpfs.core.services.tvm_2_0_service.TVM2Service.validate_service_ticket',
                       return_value=AttrDict({'src': SERVICES_TVM_2_0_CLIENT_ID})), \
             self.mock_tvm_user_ticket():
            self.auth_check_uid(header, expected_code, uid_in_url, expected_uid)

    def auth_check_uid(self, header, expected_code, uid_in_url=False, expected_uid=None):
        url_uid = self.uid if uid_in_url else None
        resp = self.client.get('disk', query={'path': '/'}, headers=header, uid=url_uid)
        assert resp.status_code == expected_code

        if expected_code == 200:
            response = json.loads(resp.content)
            assert response['user']['uid'] == (expected_uid or self.uid)

class ClientTVM2AuthTestCase(ClientTVM2AuthTestCaseBase):
    auth_method = TVM2

    def test_success_auth_service_ticket_with_uid_in_header(self):
        self.auth_check({'X-Ya-Service-Ticket': self.service_ticket, 'X-Uid': self.uid}, 200)

    def test_success_auth_service_ticket_with_uid_in_user_ticket(self):
        self.auth_check({'X-Ya-Service-Ticket': self.service_ticket, 'X-Ya-User-Ticket': self.user_ticket}, 200,
                        expected_uid=self.token_uid)

    def test_success_auth_service_ticket_with_uid_in_url(self):
        self.auth_check({'X-Ya-Service-Ticket': self.service_ticket}, 200, uid_in_url=True)

    def test_success_auth_service_ticket_without_uid(self):
        with mock.patch.dict(settings.platform, self.fake_settings()), \
             mock.patch.dict('mpfs.platform.auth.settings.platform', self.fake_settings()), \
             mock.patch.object(ListAppUsersHandler, 'default_auth_methods', [TVM2Auth()]), \
             mock.patch.object(ListAppUsersHandler, 'auth_methods', []), \
             mock.patch('mpfs.core.services.tvm_2_0_service.TVM2Service.validate_service_ticket',
                        return_value=AttrDict({'src': SERVICES_TVM_2_0_CLIENT_ID})), \
            self.mock_tvm_user_ticket():

            header = {'X-Ya-Service-Ticket': self.service_ticket}
            resp = self.client.get('data/users', query={'limit': 2}, headers=header)
            assert resp.status_code == 200

    def test_wrong_service_ticket(self):
        with mock.patch.dict(settings.platform, self.fake_settings()), \
             mock.patch.object(MpfsProxyHandler, 'default_auth_methods', [TVM2Auth()]), \
             mock.patch.object(MpfsProxyHandler, 'auth_methods', []), \
             mock.patch('mpfs.core.services.tvm_2_0_service.TVM2Service.validate_service_ticket',
                        side_effect=TvmException()):

            self.auth_check_uid({'X-Ya-Service-Ticket': 'fake_ticket', 'X-Uid': self.uid}, 401)

    def test_wrong_user_ticket(self):
        with mock.patch.dict(settings.platform, self.fake_settings()), \
             mock.patch.object(MpfsProxyHandler, 'default_auth_methods', [TVM2Auth()]), \
             mock.patch.object(MpfsProxyHandler, 'auth_methods', []), \
             mock.patch('mpfs.core.services.tvm_2_0_service.TVM2Service.validate_service_ticket',
                        return_value=AttrDict({'src': SERVICES_TVM_2_0_CLIENT_ID})), \
             mock.patch('mpfs.core.services.tvm_2_0_service.TVM2Service.validate_user_ticket',
                        side_effect=TvmException()):

            self.auth_check_uid({'X-Ya-Service-Ticket': self.service_ticket, 'X-Ya-User-Ticket': 'wrong_ticket',
                                 'X-Uid': self.uid}, 401)

    def test_uid_in_header_and_ticket_is_ok(self):
        self.auth_check({'X-Ya-Service-Ticket': self.service_ticket, 'X-Ya-User-Ticket': self.user_ticket,
                         'X-Uid': self.token_uid}, 200, expected_uid=self.token_uid)

    def test_uid_from_header_and_from_ticket_not_exists(self):
        with mock.patch.dict(settings.platform, self.fake_settings()), \
                mock.patch.object(MpfsProxyHandler, 'default_auth_methods', [TVM2Auth()]), \
                mock.patch.object(MpfsProxyHandler, 'auth_methods', []), \
                mock.patch('mpfs.core.services.tvm_2_0_service.TVM2Service.validate_service_ticket', return_value=AttrDict({'src': SERVICES_TVM_2_0_CLIENT_ID})), \
                self.mock_tvm_user_ticket(111111L, [111111L]):
            header = {'X-Ya-Service-Ticket': self.service_ticket, 'X-Ya-User-Ticket': self.user_ticket,
                      'X-Uid': self.token_uid}
            resp = self.client.get('disk', query={'path': '/'}, headers=header)
            assert resp.status_code == 401
            assert 'unique_sign' not in from_json(resp.content)['description']
            self.auth_check_uid({'X-Ya-Service-Ticket': self.service_ticket, 'X-Ya-User-Ticket': self.user_ticket,
                             'X-Uid': self.token_uid}, 401)

    @parameterized.expand([
        (False, 401),
        (True, 200)
    ])
    def test_uid_from_header_and_from_ticket_exists_in_additional(self, feature_enabled, expected_status):
        with mock.patch.dict(settings.platform, self.fake_settings()), \
                mock.patch.object(MpfsProxyHandler, 'default_auth_methods', [TVM2Auth()]), \
                mock.patch.object(MpfsProxyHandler, 'auth_methods', []), \
                mock.patch('mpfs.platform.auth.FEATURE_TOGGLES_TVM2_MULTIAUTH', feature_enabled), \
                mock.patch('mpfs.core.services.tvm_2_0_service.TVM2Service.validate_service_ticket', return_value=AttrDict({'src': SERVICES_TVM_2_0_CLIENT_ID})), \
                self.mock_tvm_user_ticket(111111L, [111111L, self.token_uid]):

            self.auth_check_uid({'X-Ya-Service-Ticket': self.service_ticket, 'X-Ya-User-Ticket': self.user_ticket,
                                 'X-Uid': self.token_uid}, expected_status, expected_uid=self.token_uid)

    @parameterized.expand([
        (False, 200),
        (True, 200)
    ])
    def test_uid_from_header_and_from_ticket_exists_in_default(self, feature_enabled, expected_status):
        with mock.patch.dict(settings.platform, self.fake_settings()), \
                mock.patch.object(MpfsProxyHandler, 'default_auth_methods', [TVM2Auth()]), \
                mock.patch.object(MpfsProxyHandler, 'auth_methods', []), \
                mock.patch('mpfs.platform.auth.FEATURE_TOGGLES_TVM2_MULTIAUTH', feature_enabled), \
                mock.patch('mpfs.core.services.tvm_2_0_service.TVM2Service.validate_service_ticket', return_value=AttrDict({'src': SERVICES_TVM_2_0_CLIENT_ID})), \
                self.mock_tvm_user_ticket(self.token_uid, [self.token_uid, 111111L]):

            self.auth_check_uid({'X-Ya-Service-Ticket': self.service_ticket, 'X-Ya-User-Ticket': self.user_ticket,
                                 'X-Uid': self.token_uid}, expected_status, expected_uid=self.token_uid)

    def test_not_number_uid_from_header(self):
        with mock.patch.dict(settings.platform, self.fake_settings()), \
                mock.patch.object(MpfsProxyHandler, 'default_auth_methods', [TVM2Auth()]), \
                mock.patch.object(MpfsProxyHandler, 'auth_methods', []), \
                mock.patch('mpfs.core.services.tvm_2_0_service.TVM2Service.validate_service_ticket', return_value=AttrDict({'src': SERVICES_TVM_2_0_CLIENT_ID})), \
                self.mock_tvm_user_ticket(self.token_uid, [self.token_uid]):

            self.auth_check_uid({'X-Ya-Service-Ticket': self.service_ticket, 'X-Ya-User-Ticket': self.user_ticket,
                                 'X-Uid': 'I\'m not number'}, 401)


class ClientTVM2TicketsOnlyAuthTestCase(ClientTVM2AuthTestCaseBase):

    auth_method = TVM2_TICKETS_ONLY

    def test_fail_auth_service_ticket_with_uid_in_header(self):
        self.auth_check({'X-Ya-Service-Ticket': self.service_ticket, 'X-Uid': self.uid}, 401)

    def test_success_auth_service_ticket_with_uid_in_user_ticket(self):
        self.auth_check({'X-Ya-Service-Ticket': self.service_ticket, 'X-Ya-User-Ticket': self.user_ticket}, 200,
                        expected_uid=self.token_uid)


class TVM2MPFSAuthTestCase(TestTVM2Base, UserTestCaseMixin):
    not_registered_client_id = 401401401

    class SilentSettingsMock(BaseStub):
        def __init__(self, setting):
            self.mocked_settings = mock.patch.dict(settings.auth['tvm_2_0']['silent_mode'], setting)

        def start(self):
            self.mocked_settings.start()
            mpfs.frontend.api.auth.reload_settings()

        def stop(self):
            self.mocked_settings.stop()
            mpfs.frontend.api.auth.reload_settings()

    def setup_method(self, method):
        super(TVM2MPFSAuthTestCase, self).setup_method(method)
        self.create_user(self.uid)

    @parameterized.expand(['ping', 'ping_slb', 'version', 'office_download_redirect'])
    def test_bypass_auth_for_specific_handlers(self, method_name):
        api, _ = self.do_request(method=method_name, client_addr=self.remote_addr)
        assert api.req.http_resp.status <> 401

    @parameterized.expand(['127.0.0.1', '::1'])
    def test_bypass_auth_for_localhost(self, ip):
        api, _ = self.do_request(method='info', opts={'uid': self.uid},
                                 client_addr=ip)
        assert api.req.http_resp.status == 200

    def test_bypass_auth_for_exclusion_by_ip(self):
        with TVM2MPFSAuthTestCase.SilentSettingsMock({'addresses': {self.addr_with_tvm2_auth}}):
            api, _ = self.do_request(method='info', opts={'uid': self.uid},
                                     client_addr=self.addr_with_tvm2_auth)
        assert api.req.http_resp.status == 200

    def test_bypass_auth_for_exclusion_by_ip_subnet(self):
        with TVM2MPFSAuthTestCase.SilentSettingsMock({'addresses': {'2a02:6b8:c0c:4780::/57'}}):
            api, _ = self.do_request(method='info', opts={'uid': self.uid},
                                     client_addr='2a02:6b8:c0c:47ab:0:1406:2536:2')
        assert api.req.http_resp.status == 200

    def test_bypass_auth_for_exclusion_by_ip_network(self):
        with TVM2MPFSAuthTestCase.SilentSettingsMock({'networks': {'_AERONETS_'}}):
            api, _ = self.do_request(method='info', opts={'uid': self.uid},
                                     client_addr=HbfServiceStub.ip_included)
        assert api.req.http_resp.status == 200

    def test_success_service_ticket_validation(self):
        with mock.patch('mpfs.core.services.tvm_2_0_service.TVM2Service.validate_service_ticket',
                        return_value=AttrDict({'src': AUTH_CLIENTS_PLATFORM_PROD_TVM_2_0_CLIENT_IDS_0})):
            header = {'X-Ya-Service-Ticket': self.service_ticket}
            api, _ = self.do_request(method='info', opts={'uid': self.uid}, headers=header,
                                     client_addr=self.addr_with_tvm2_auth)
            assert api.req.http_resp.status == 200

    def test_failed_service_ticket_validation(self):
        with mock.patch('mpfs.core.services.tvm_2_0_service.TVM2Service.validate_service_ticket',
                        side_effect=TvmException()):
            header = {'X-Ya-Service-Ticket': self.service_ticket}
            api, _ = self.do_request(method='info', opts={'uid': self.uid}, headers=header,
                                     client_addr=self.addr_with_tvm2_auth)
            assert api.req.http_resp.status == 401

    def test_service_ticket_client_id_validation(self):
        with mock.patch('mpfs.core.services.tvm_2_0_service.TVM2Service.validate_service_ticket',
                        return_value=AttrDict({'src': self.not_registered_client_id})):
            header = {'X-Ya-Service-Ticket': self.service_ticket}
            api, _ = self.do_request(method='info', opts={'uid': self.uid}, headers=header,
                                     client_addr=self.addr_with_tvm2_auth)
            assert api.req.http_resp.status == 401

    def test_restrict_handlers_validation(self):
        header = {'X-Ya-Service-Ticket': self.service_ticket}
        restricted_client_id = 1
        allowed_client_id = 2
        client_name = 'client_name'
        # turn silent mode off
        silent_mode_mock = TVM2MPFSAuthTestCase.SilentSettingsMock({'global': False})
        # add client to settings
        tvm_clients_mock = mock.patch.dict(settings.auth['clients'], {
            client_name: {
                'tvm_2_0': {
                    'client_ids': [allowed_client_id],
                    'user_ticket_policy': 'no_checks'}}})

        # add calling method to handlers_groups
        handlers_groups_mock = mock.patch.dict(settings.auth['handlers_groups'], {'handler_group': ['info']})
        # allow added handler group to be called by added client
        restricted_handlers_mock = mock.patch.dict(settings.auth['tvm_2_0_restricted_handlers'],
                                                   {'handler_group': [client_name]})
        disallow_general_handlers_mock = mock.patch.dict(settings.auth, {'tvm_2_0_disallow_general_handlers': [client_name]})

        # create two clients to check - restricted and allowed
        restricted_client_id_mock = mock.patch('mpfs.core.services.tvm_2_0_service.TVM2Service.validate_service_ticket',
                           return_value=AttrDict({'src': restricted_client_id}))
        allowed_client_id_mock = mock.patch('mpfs.core.services.tvm_2_0_service.TVM2Service.validate_service_ticket',
                           return_value=AttrDict({'src': allowed_client_id}))

        with silent_mode_mock, handlers_groups_mock, restricted_handlers_mock, tvm_clients_mock:
            setup_handlers_groups()
            setup_tvm_2_0_clients(settings.auth['clients'])
            with restricted_client_id_mock:
                api, _ = self.do_request(method='info', opts={'uid': self.uid}, headers=header,
                                         client_addr=self.addr_with_tvm2_auth)
                assert api.req.http_resp.status == 401

            with allowed_client_id_mock:
                api, _ = self.do_request(method='info', opts={'uid': self.uid}, headers=header,
                                         client_addr=self.addr_with_tvm2_auth)
                assert api.req.http_resp.status == 200

    def test_restrict_general_handlers_validation(self):
        header = {'X-Ya-Service-Ticket': self.service_ticket}
        restricted_client_id = 1
        allowed_client_id = 2
        client_name = 'client_name'
        # turn silent mode off
        silent_mode_mock = TVM2MPFSAuthTestCase.SilentSettingsMock({'global': False})
        # add client to settings
        tvm_clients_mock = mock.patch.dict(settings.auth['clients'], {
            client_name: {
                'tvm_2_0': {
                    'client_ids': [allowed_client_id],
                    'user_ticket_policy': 'no_checks'}}})

        # allow added handler group to be called by added client
        restricted_handlers_mock = mock.patch.dict(settings.auth['tvm_2_0_restricted_handlers'],
                                                   {'handler_group': [client_name]})
        disallow_general_handlers_mock = mock.patch.dict(settings.auth, {'tvm_2_0_disallow_general_handlers': [client_name]})

        # create two clients to check - restricted and allowed
        allowed_client_id_mock = mock.patch('mpfs.core.services.tvm_2_0_service.TVM2Service.validate_service_ticket',
                           return_value=AttrDict({'src': allowed_client_id}))

        with silent_mode_mock, restricted_handlers_mock, tvm_clients_mock:
            setup_handlers_groups()
            setup_tvm_2_0_clients(settings.auth['clients'])
            with allowed_client_id_mock, disallow_general_handlers_mock:
                api, _ = self.do_request(method='info', opts={'uid': self.uid}, headers=header,
                                         client_addr=self.addr_with_tvm2_auth)
                assert api.req.http_resp.status == 401

    def test_handlers_groups_without_restricted_handlers(self):
        header = {'X-Ya-Service-Ticket': self.service_ticket}
        client_id = 1
        # turn silent mode off
        silent_mode_mock = TVM2MPFSAuthTestCase.SilentSettingsMock({'global': False})
        # add client to settings
        tvm_clients_mock = mock.patch.dict(settings.auth['clients'], {
            'client_name': {
                'tvm_2_0': {
                    'client_ids': [client_id],
                    'user_ticket_policy': 'no_checks'}}})

        # add calling method to handlers_groups
        handlers_groups_mock = mock.patch.dict(settings.auth['handlers_groups'], {'handler_group': ['info']})
        # mock client id response
        client_id_mock = mock.patch('mpfs.core.services.tvm_2_0_service.TVM2Service.validate_service_ticket',
                           return_value=AttrDict({'src': client_id}))
        # ensure that our handler_group not in restricted handlers setting
        restricted_handlers_mock = mock.patch.dict(settings.auth['tvm_2_0_restricted_handlers'], {}, clear=True)

        with silent_mode_mock, handlers_groups_mock, client_id_mock, tvm_clients_mock, restricted_handlers_mock:
            setup_handlers_groups()
            setup_tvm_2_0_clients(settings.auth['clients'])
            api, _ = self.do_request(method='info', opts={'uid': self.uid}, headers=header,
                                     client_addr=self.addr_with_tvm2_auth)
            assert api.req.http_resp.status == 200

    def test_service_ticket_client_id_validation_in_silent_mode(self):
        header = {'X-Ya-Service-Ticket': self.service_ticket}
        with mock.patch('mpfs.core.services.tvm_2_0_service.TVM2Service.validate_service_ticket',
                        return_value=AttrDict({'src': self.not_registered_client_id})), \
             TVM2MPFSAuthTestCase.SilentSettingsMock({'global': True}), \
             mock.patch('mpfs.frontend.api.auth.error_log.error') as mocked_error_log:
            api, _ = self.do_request(method='info', opts={'uid': self.uid}, headers=header,
                                     client_addr=self.addr_with_tvm2_auth)

            assert api.req.http_resp.status == 200
            mocked_error_log.assert_called()
            assert_that(mocked_error_log.call_args[0][0],
                        all_of(contains_string(TVM2_SILENT_MODE_LOG_PREFIX),
                               contains_string(str(self.not_registered_client_id))))

    def test_success_user_ticket_validation(self):
        with mock.patch('mpfs.core.services.tvm_2_0_service.TVM2Service.validate_user_ticket',
                        return_value=AttrDict({'src': AUTH_CLIENTS_PLATFORM_PROD_TVM_2_0_CLIENT_IDS_0})), \
             mock.patch('mpfs.core.services.tvm_2_0_service.TVM2Service.validate_service_ticket',
                        return_value=AttrDict({'src': AUTH_CLIENTS_PLATFORM_PROD_TVM_2_0_CLIENT_IDS_0})):
            header = {'X-Ya-User-Ticket': self.user_ticket,
                      'X-Ya-Service-Ticket': self.service_ticket}
            api, _ = self.do_request(method='info', opts={'uid': self.uid}, headers=header,
                                     client_addr=self.addr_with_tvm2_auth)
            assert api.req.http_resp.status == 200

    def test_success_user_ticket_validation_with_multi_auth(self):
        header = {'X-Ya-User-Ticket': self.user_ticket,
                  'X-Ya-Service-Ticket': self.service_ticket}

        with mock.patch('mpfs.core.services.tvm_2_0_service.TVM2Service.validate_user_ticket',
                        return_value=AttrDict({'src': AUTH_CLIENTS_PLATFORM_PROD_TVM_2_0_CLIENT_IDS_0,
                                               'uids': [123L, long(self.uid), 777L],
                                               'default_uid': 123L})), \
             mock.patch('mpfs.core.services.tvm_2_0_service.TVM2Service.validate_service_ticket',
                        return_value=AttrDict({'src': AUTH_CLIENTS_PLATFORM_PROD_TVM_2_0_CLIENT_IDS_0})), \
             mock.patch('mpfs.frontend.api.auth.get_tvm_2_0_clients',
                        return_value={AUTH_CLIENTS_PLATFORM_PROD_TVM_2_0_CLIENT_IDS_0:
                                          MPFSClient('platform_prod',
                                                     {'tvm_2_0': {'client_ids':
                                                                      [AUTH_CLIENTS_PLATFORM_PROD_TVM_2_0_CLIENT_IDS_0],
                                                                  'user_ticket_policy': 'strict'}})}):
            api, _ = self.do_request(method='info', opts={'uid': self.uid}, headers=header,
                                     client_addr=self.addr_with_tvm2_auth)

        assert api.req.http_resp.status == 200

    def test_no_service_ticket_from_authorization_network(self):
        """Проверяем, что если не передавать сервисный тикет из сети с авторизацией, то запрос не пройдет.

        Пускаем запрос из сети с авторизацией.
        При этом не передаем сервисный тикеты (который обязательный для таких сетей).
        Ожидаем, что запрос не пройдет.

        """
        with mock.patch('mpfs.core.services.tvm_2_0_service.TVM2Service.validate_user_ticket',
                        return_value=AttrDict({'src': AUTH_CLIENTS_PLATFORM_PROD_TVM_2_0_CLIENT_IDS_0})), \
             mock.patch('mpfs.core.services.tvm_2_0_service.TVM2Service.validate_service_ticket',
                        return_value=AttrDict({'src': AUTH_CLIENTS_PLATFORM_PROD_TVM_2_0_CLIENT_IDS_0})):
            # Но нет сервисного тикета
            header = {'X-Ya-User-Ticket': self.user_ticket}
            api, _ = self.do_request(method='info', opts={'uid': self.uid}, headers=header,
                                     client_addr=self.addr_with_tvm2_auth)
            assert api.req.http_resp.status == 401

    def test_failed_user_ticket_validation(self):
        with mock.patch('mpfs.core.services.tvm_2_0_service.TVM2Service.validate_user_ticket',
                        side_effect=TvmException()):
            header = {'X-Ya-User-Ticket': self.user_ticket}
            api, _ = self.do_request(method='info', opts={'uid': self.uid}, headers=header,
                                     client_addr=self.addr_with_tvm2_auth)
            assert api.req.http_resp.status == 401

    def test_not_inited_user_ticket_validation(self):
        header = {'X-Ya-User-Ticket': self.user_ticket,
                  'X-Ya-Service-Ticket': self.service_ticket}

        with mock.patch('mpfs.frontend.api.auth.AUTH_BYPASS_AUTH_HANDLERS', []),\
             mock.patch('mpfs.core.services.tvm_2_0_service.TVM2Service.validate_user_ticket',
                        return_value=AttrDict({'src': AUTH_CLIENTS_PLATFORM_PROD_TVM_2_0_CLIENT_IDS_0,
                                               'uids': [123L],
                                               'default_uid': 123L})) as validate_user_ticket, \
             mock.patch('mpfs.core.services.tvm_2_0_service.TVM2Service.validate_service_ticket',
                        return_value=AttrDict({'src': AUTH_CLIENTS_PLATFORM_PROD_TVM_2_0_CLIENT_IDS_0})), \
             mock.patch('mpfs.frontend.api.auth.get_tvm_2_0_clients',
                        return_value={AUTH_CLIENTS_PLATFORM_PROD_TVM_2_0_CLIENT_IDS_0:
                                          MPFSClient('platform_prod',
                                                     {'tvm_2_0': {'client_ids':
                                                                      [AUTH_CLIENTS_PLATFORM_PROD_TVM_2_0_CLIENT_IDS_0],
                                                                  'user_ticket_policy': 'strict'}})}):
            api, _ = self.do_request(method='ping', opts={'uid': 123L}, headers=header,
                                     client_addr=self.addr_with_tvm2_auth)

        assert validate_user_ticket.called
        assert api.req.http_resp.status == 200

    def test_skip_authorization_by_network_address(self):
        """Проверяем, что если запрос и сети без авторизации, то даже с невалидным тикетом запрос будет успешным."""
        with mock.patch('mpfs.core.services.tvm_2_0_service.TVM2Service.validate_service_ticket',
                        side_effect=TvmException()), \
             TVM2MPFSAuthTestCase.SilentSettingsMock({'global': True}):
            mpfs.frontend.api.auth.reload_settings()
            header = {'X-Ya-Service-Ticket': self.service_ticket}
            api, _ = self.do_request(method='info', opts={'uid': self.uid}, headers=header,
                                     client_addr=self.addr_without_auth)
            assert api.req.http_resp.status == 200

    def test_tvm2_and_signandts_network__signandts_authorization__success(self):
        with TVM2MPFSAuthTestCase.SilentSettingsMock({'global': True}):
            api, _ = self.do_request(method='info', opts={'uid': self.uid, 'sign': 'fake_sign', 'ts': 123},
                                     client_addr=self.addr_with_tvm2_signandts_auth)
        assert api.req.http_resp.status == 200

    def test_tvm2_and_signandts_network__tvm2_authorization__success(self):
        with mock.patch('mpfs.core.services.tvm_2_0_service.TVM2Service.validate_user_ticket',
                        return_value=AttrDict({'src': AUTH_CLIENTS_PLATFORM_PROD_TVM_2_0_CLIENT_IDS_0})), \
             mock.patch('mpfs.core.services.tvm_2_0_service.TVM2Service.validate_service_ticket',
                        return_value=AttrDict({'src': AUTH_CLIENTS_PLATFORM_PROD_TVM_2_0_CLIENT_IDS_0})):
            header = {'X-Ya-User-Ticket': self.user_ticket,
                      'X-Ya-Service-Ticket': self.service_ticket}
            api, _ = self.do_request(method='info', opts={'uid': self.uid}, headers=header,
                                     client_addr=self.addr_with_tvm2_signandts_auth)
            assert api.req.http_resp.status == 200

    def test_tvm2_and_signandts_network__no_authorization__failure(self):
        api, _ = self.do_request(method='info', opts={'uid': self.uid},
                                 client_addr=self.addr_with_tvm2_signandts_auth)
        assert api.req.http_resp.status == 401
