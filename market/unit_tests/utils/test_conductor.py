from unittest import TestCase

import mock
import os

from lib.utils import conductor


class TestConductor(TestCase):
    def test_extract_ticket(self):
        c_ticket_out = '''Ticket 1160074 is created successfully. Ticket status is need_info

URL: https://c.yandex-team.ru/tickets/1130665
'''

        c = conductor._extract_ticket(c_ticket_out)
        self.assertEqual(c.url, 'https://c.yandex-team.ru/tickets/1130665')
        self.assertEqual(c.number, 1130665)

    def test_create_ticket(self):
        token = 'some_conductor_token'
        os.environ[conductor.TOKEN_PARAMETER_NAME] = token
        with mock.patch('lib.utils.conductor.lib.common.do_shell_with_retry') as mock_do_shell:
            mock_do_shell.return_value = ['https://c.yandex-team.ru/tickets/1130665']
            c = conductor.create_ticket('testing', 'vladvin', 'My comment', ['my-app=111', 'my-app-2=222'])

            expected_params = ['conductor-ticket', '--oauth', token,
                               '--branch', 'testing', '--cc', 'vladvin@yandex-team.ru',
                               '--comment', 'My comment', 'my-app=111 my-app-2=222']
            mock_do_shell.assert_called_once_with(expected_params)

            self.assertEqual(c.url, 'https://c.yandex-team.ru/tickets/1130665')
            self.assertEqual(c.number, 1130665)
