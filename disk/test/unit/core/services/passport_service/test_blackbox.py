# -*- coding: utf-8 -*-

import mock
from nose_parameterized import parameterized

from mpfs.common import errors
import requests
import unittest
from mpfs.common.util import to_json, from_json
from mpfs.config import settings


class BlackBoxServiceTestCase(unittest.TestCase):

    @mock.patch.dict(settings.services, {'tvm': {'enabled': False}, 'tvm_2_0': {'enabled': False}})
    def test_error_response_logged_to_error_log(self):
        """Проверить, что ошибка, возвращенная сервисом blackbox логируется в лог ошибок.

        https://doc.yandex-team.ru/blackbox/concepts/blackboxErrors.xml#blackboxErrors
        """
        bad_response = {
            'exception': {
                'value': 'DB_EXCEPTION',
                'id': 100500
            },
            'error': 'BlackBox error: ...'
        }

        response = requests.Response()
        response._content = to_json(bad_response)
        with mock.patch(
            'mpfs.core.services.passport_service.BlackboxService.request',
            return_value=response
        ), \
        mock.patch(
            'mpfs.core.services.passport_service.error_log',
            return_value=mock.MagicMock()
        ) as mocked_error_log:
                mocked_error_log.configure_mock(**{
                    'info.return_value': None
                })
                try:
                    from mpfs.core.services.passport_service import BlackboxService
                    BlackboxService().check_oauth_token('Winter is coming...', '127.0.0.1')
                except errors.PassportBadResult:
                    pass

                mocked_error_log.info.assert_called_once()
                args, kwargs = mocked_error_log.info.call_args
                (message,) = args
                message = from_json(message)
                assert 'error' in message
                assert message['error'] == 'BlackBox error: ...'
                assert 'exception' in message
                assert message['exception']['id'] == 100500
                assert message['exception']['value'] == 'DB_EXCEPTION'
