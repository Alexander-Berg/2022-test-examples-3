# -*- coding: utf-8 -*-

import mock

import push_client
import test_common


class TestPushClient(object):
    def test_push_client_not_running(self):
        test_common.generate_monitoring_config()
        with mock.patch('subprocess.Popen') as mocked_popen:
            process_mock = mock.Mock()
            attrs = {
                'communicate.return_value': ('[]\n', 'blah blah fail!\n'),
                'returncode': 1,
            }
            process_mock.configure_mock(**attrs)
            mocked_popen.return_value = process_mock
            with test_common.OutputCapture() as capture:
                push_client.main()
        assert capture.get_stderr() == ''
        assert capture.get_stdout() == 'PASSIVE-CHECK:market-report-push-client-status;2;market_health: blah blah fail!\n'
