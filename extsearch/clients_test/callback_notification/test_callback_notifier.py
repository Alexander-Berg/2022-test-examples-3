from unittest.mock import Mock
from unittest import TestCase
from dataclasses import dataclass
from typing import Optional, List
from requests import RequestException

from extsearch.video.ugc.sqs_moderation.clients.callback_notifier import (
    CallbackNotifier, CallbackError, ServiceDescription
)


@dataclass
class MockClientService:
    Name: str
    UniversalUrl: Optional[str] = None
    NonRetryableNotificationResponseCode: Optional[List[int]] = None


def get_mock_notifier() -> CallbackNotifier:
    client_config = Mock()
    client_config.ClientService = [
        MockClientService(Name='none_url', UniversalUrl=None),
        MockClientService(Name='wrong_url', UniversalUrl='fff'),
        MockClientService(
            Name='retryable_response', UniversalUrl='https://test', NonRetryableNotificationResponseCode=[404]
        ),
        MockClientService(
            Name='no_retryable_response', UniversalUrl='https://test', NonRetryableNotificationResponseCode=[]
        ),
    ]
    resp = Mock()
    resp.ok = False
    resp.status_code = 404

    tvm_client = Mock()
    session = Mock()
    session.post = Mock(return_value=resp)

    return CallbackNotifier(session, client_config, tvm_client)


def get_mock_response(code: int):
    resp = Mock()
    resp.ok = True if code == 200 else False
    resp.status_code = code
    return resp


class TestCallbackNotifier(TestCase):

    def setUp(self) -> None:
        notifier = get_mock_notifier()

        self.tvm_client = notifier.tvm_client
        self.session = notifier.session

        self.default_data = {'transcoder_status_str': '1'}
        self.notifier = notifier

    def test_notify_service_without_url(self):
        self.assertFalse(
            self.notifier.notify_service('none_url', '111', self.default_data),
            'Url is none, must not notify'
        )
        self.assertFalse(self.notifier.session.post.called)

    def test_notify_service_wrong_url(self):
        self.assertFalse(
            self.notifier.notify_service('wrong_url', '111', self.default_data),
            'Url is wrong, must not notify'
        )
        self.assertFalse(self.notifier.session.post.called)

    def test_notify_service_with_retryable_response(self):
        self.assertFalse(
            self.notifier.notify_service('retryable_response', '111', self.default_data),
            'Service have retryable response code, and must not send notification'
        )
        self.assertTrue(self.notifier.session.post.called)

    def test_notify_service_without_retryable_response(self):
        with self.assertRaises(CallbackError, msg='Service have no retryable response code, must rise CallbackError'):
            self.notifier.notify_service('no_retryable_response', '111', self.default_data),
        self.assertTrue(self.notifier.session.post.called)

    def test_wrong_data(self):
        self.assertFalse(
            self.notifier.notify_service('no_retryable_response', '111', {'transcoder_status_str': ''}),
            'Wrong data send, must not send notification'
        )
        self.assertFalse(self.notifier.session.post.called)

    def test_wrong_service(self):
        self.assertFalse(
            self.notifier.notify_service('sssss', '111', self.default_data),
            'Wrong service name, must not send notification'
        )
        self.assertFalse(self.notifier.session.post.called)

    def test_notification(self):
        notifier = get_mock_notifier()
        resp = Mock()
        resp.ok = True
        resp.status_code = 200
        notifier.session.post = Mock(return_value=resp)

        self.assertTrue(
            notifier.notify_service('retryable_response', '111', self.default_data),
            'Response is ok, must send notification'
        )
        self.assertTrue(notifier.session.post.called)

    def test_request_error(self):
        notifier = get_mock_notifier()
        notifier.session.post = Mock(side_effect=RequestException)
        with self.assertRaises(CallbackError,
                               msg='session.post rise RequestException, exception must be rised CallbackError!'):
            notifier.notify_service('retryable_response', '111', self.default_data)
        self.assertTrue(notifier.session.post.called)


class TestServiceDescription(TestCase):

    def setUp(self) -> None:
        self.service_description = {
            'with_template': ServiceDescription(name='', url_template='http://test.ru/%s', non_retryable_codes=[]),
            'without_template': ServiceDescription(name='', url_template='http://test.ru/', non_retryable_codes=[]),
            'retryable': ServiceDescription(name='', url_template='http://test.ru/', non_retryable_codes=[404]),
            'non_retryable': ServiceDescription(name='', url_template='http://test.ru/', non_retryable_codes=[]),
        }

    def test_get_url_with_template(self):
        service = self.service_description['with_template']
        self.assertEqual('http://test.ru/123', service.get_url('123'))

    def test_get_url_without_template(self):
        service = self.service_description['without_template']
        self.assertEqual('http://test.ru/', service.get_url('123'))

    def test_retryable_response(self):
        resp = get_mock_response(404)
        service = self.service_description['retryable']
        self.assertFalse(service.retryable_response(resp))

    def not_retryable_response(self):
        resp = get_mock_response(404)
        service = self.service_description['non_retryable']
        self.assertTrue(service.retryable_response(resp))
