# -*- coding: utf-8 -*-
import mock
from attrdict import AttrDict
from hamcrest import assert_that, calling, raises
from nose_parameterized import parameterized

from mpfs.common.errors import AuthorizationError
from mpfs.common.static.tags.conf_sections import NO_CHECKS, LEGACY, STRICT
from mpfs.config import settings
from mpfs.engine.process import setup_tvm_2_0_clients
from mpfs.frontend.api.auth import TVM2
from test.unit.base import NoDBTestCase


class MPFSTVM2AuthBaseMixin(object):
    src_tvm_id = 1001
    uid = '111'
    request = AttrDict({
        'user': AttrDict({
            'uid': uid
        }),
        'request_headers': {
            'X-Ya-Service-Ticket': 'service_ticket',
            'X-Ya-User-Ticket': 'user_ticket',
        },
        'method_name': '/ping',
        'http_req': AttrDict({'remote_addr': '127.0.0.1'})
    })

    validated_service_ticket = AttrDict({'src': src_tvm_id})

    def _authorize_request(self, validated_user_ticket):
        with mock.patch.dict(settings.auth['clients'], self.mocked_clients_settings), \
                mock.patch('mpfs.frontend.api.auth.TVM2._check_tvm_2_0_service_ticket', return_value=self.validated_service_ticket), \
                mock.patch('mpfs.frontend.api.auth.TVM2._check_and_set_tvm_2_0_user_ticket', return_value=validated_user_ticket):
            TVM2.authorize(self.request)


class MPFSTVM2AuthNoUserTicketChecksTestCase(MPFSTVM2AuthBaseMixin, NoDBTestCase):

    def setUp(self):
        self.mocked_clients_settings = {
            'mail_front': {
                'tvm_2_0': {
                    'client_ids': [MPFSTVM2AuthBaseMixin.src_tvm_id],
                    'user_ticket_policy': NO_CHECKS,
                }
            }
        }
        with mock.patch.dict(settings.auth['clients'], self.mocked_clients_settings):
            setup_tvm_2_0_clients(settings.auth['clients'])

    @parameterized.expand([
        (True,),
        (False,),
    ])
    def test_no_user_ticket(self, policy_presented):
        validated_user_ticket = None
        if not policy_presented:
            self.mocked_clients_settings['mail_front']['tvm_2_0'].pop('user_ticket_policy')
        with mock.patch('mpfs.common.util.logger.MPFSLogger.info') as logger_mock:
            self._authorize_request(validated_user_ticket)
        assert not logger_mock.called

    @parameterized.expand([
        (True,),
        (False,),
    ])
    def test_user_ticket_has_different_uid(self, policy_presented):
        validated_user_ticket = AttrDict({'default_uid': self.uid + '1', 'uids': [self.uid + '1']})
        if not policy_presented:
            self.mocked_clients_settings['mail_front']['tvm_2_0'].pop('user_ticket_policy')
        with mock.patch('mpfs.common.util.logger.MPFSLogger.info') as logger_mock:
            self._authorize_request(validated_user_ticket)
        assert not logger_mock.called

    @parameterized.expand([
        (True,),
        (False,),
    ])
    def test_user_ticket_passed(self, policy_presented):
        validated_user_ticket = AttrDict({'default_uid': self.uid, 'uids': [self.uid]})
        if not policy_presented:
            self.mocked_clients_settings['mail_front']['tvm_2_0'].pop('user_ticket_policy')
        with mock.patch('mpfs.common.util.logger.MPFSLogger.info') as logger_mock:
            self._authorize_request(validated_user_ticket)
        assert not logger_mock.called


class MPFSTVM2AuthLegacyUserTicketChecksTestCase(MPFSTVM2AuthBaseMixin, NoDBTestCase):

    def setUp(self):
        self.mocked_clients_settings = {
            'mail_front': {
                'tvm_2_0': {
                    'client_ids': [MPFSTVM2AuthBaseMixin.src_tvm_id],
                    'user_ticket_policy': LEGACY,
                }
            }
        }
        with mock.patch.dict(settings.auth['clients'], self.mocked_clients_settings):
            setup_tvm_2_0_clients(settings.auth['clients'])

    def test_no_user_ticket(self):
        validated_user_ticket = None
        with mock.patch('mpfs.common.util.logger.MPFSLogger.info') as logger_mock:
            self._authorize_request(validated_user_ticket)
        assert logger_mock.called

    def test_user_ticket_has_different_uid(self):
        validated_user_ticket = AttrDict({'default_uid': self.uid + '1', 'uids': [self.uid + '1']})
        with mock.patch('mpfs.common.util.logger.MPFSLogger.info') as logger_mock:
            self._authorize_request(validated_user_ticket)
        assert logger_mock.called

    def test_user_ticket_passed(self):
        validated_user_ticket = AttrDict({'default_uid': self.uid, 'uids': [self.uid]})
        with mock.patch('mpfs.common.util.logger.MPFSLogger.info') as logger_mock:
            self._authorize_request(validated_user_ticket)
        assert not logger_mock.called


class MPFSTVM2AuthStrictUserTicketChecksTestCase(MPFSTVM2AuthBaseMixin, NoDBTestCase):

    def setUp(self):
        self.mocked_clients_settings = {
            'mail_front': {
                'tvm_2_0': {
                    'client_ids': [MPFSTVM2AuthBaseMixin.src_tvm_id],
                    'user_ticket_policy': STRICT,
                }
            }
        }
        with mock.patch.dict(settings.auth['clients'], self.mocked_clients_settings):
            setup_tvm_2_0_clients(settings.auth['clients'])

    def test_no_user_ticket(self):
        validated_user_ticket = None
        with mock.patch('mpfs.common.util.logger.MPFSLogger.info') as logger_mock:
            self.assertRaises(AuthorizationError, self._authorize_request, validated_user_ticket)
        assert logger_mock.called

    def test_user_ticket_has_different_uid(self):
        validated_user_ticket = AttrDict({'default_uid': self.uid + '1', 'uids': [self.uid + '1']})
        with mock.patch('mpfs.common.util.logger.MPFSLogger.info') as logger_mock:
            self.assertRaises(AuthorizationError, self._authorize_request, validated_user_ticket)
        assert logger_mock.called

    def test_user_ticket_passed(self):
        validated_user_ticket = AttrDict({'default_uid': self.uid, 'uids': [self.uid]})
        with mock.patch('mpfs.common.util.logger.MPFSLogger.info') as logger_mock:
            self._authorize_request(validated_user_ticket)
        assert not logger_mock.called
