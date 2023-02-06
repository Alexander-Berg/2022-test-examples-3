import mock
from lib.tests import test_utils
from lib.utils import sandbox
from unittest import TestCase

TEST_TICKET = 'https://sandbox.yandex-team.ru/task/91470926/view'
TEST_RESOURCE = 'Resource download link: http://proxy.sandbox.yandex-team.ru/423062249'


class TestSandbox(TestCase):
    def test_create_sandbox_ticket(self):
        with mock.patch('lib.utils.sandbox.lib.common.do_shell_with_retry') as mock_do_shell:
            mock_do_shell.return_value = [TEST_TICKET, TEST_RESOURCE]

            res = sandbox.create_sandbox_resource('my-type', 'module.tar', 'my-module', '1.11', 'My custom message')

            message = 'My custom message Upload archives: module.tar; module: my-module; version 1.11'
            command = [sandbox.SB_API_PATH, '-t', 'my-type', '-u', 'robot-mrk-teamcity', '-o', 'MARKET',
                       '-a', 'linux', '-d', message, 'module.tar']

            mock_do_shell.assert_called_once_with(command)

            self.assertEqual('my-type', res.res_type)
            self.assertEqual(res.ticket.url, TEST_TICKET)
            self.assertEqual(res.ticket.number, 91470926)
            self.assertEqual(res.res_type, 'my-type')
            self.assertEqual(res.res_id, 423062249)

    def test_create_sandbox_release(self):
        # TODO Create test
        pass

    def test_read_sandbox_parameters(self):
        params = sandbox.read_sandbox_parameters(test_utils.GRADLE_MULTI_APP_PROJECT_PATH)
        expected = {
            'post_build_task': {
                'task_type': 'MARKET_TEST_TASK',
                'parameters': {
                    'priority': {'class': 'SERVICE', 'subclass': 'NORMAL'},
                    'notifications': [
                        {
                            'recipients': [],
                            'statuses': ['FAILURE', 'SUCCESS', 'EXCEPTION', 'NO_RES', 'TIMEOUT'],
                            'transport': 'email'
                        }
                    ],
                    'description': '',
                    'owner': 'MARKET',
                    'custom_fields': [
                        {'name': 'some string', 'value': 'some_value'}
                    ]
                }
            },
            'resource_type': 'MARKET_TEST_RESOURCE'
        }

        self.assertDictEqual(expected, params)

    def test_extract_ticket(self):
        s = sandbox._extract_ticket(TEST_TICKET)
        self.assertEqual(s.url, TEST_TICKET)
        self.assertEqual(s.number, 91470926)

    def test_extract_resource_id(self):
        res_id = sandbox._extract_resource_id("Resource download link: http://proxy.sandbox.yandex-team.ru/423062249")
        self.assertEqual(423062249, res_id)
