# -*- coding: utf-8 -*-

import mock
import unittest

from travel.avia.ticket_daemon_processing.pretty_fares.messaging.daemon_tasks import requests, get_full_results, TicketDaemonException


class GetFullResultsTestCase(unittest.TestCase):
    def bad_response(self):
        return self.create_response_with_code(400)

    def internal_server_error(self):
        return self.create_response_with_code(500)

    def create_response_with_code(self, code):
        response_mock_object = mock.Mock()
        response_mock_object.status_code = code
        return response_mock_object

    def test_if_status_code_is_not_200_throws_TicketDaemonException(self):
        with mock.patch('time.sleep', return_value=None):
            with mock.patch.object(requests, 'get', return_value=self.bad_response()) as m_get:
                self.assertRaises(TicketDaemonException, lambda: get_full_results('qid'))
                self.assertEqual(m_get.call_count, 3)

            with mock.patch.object(requests, 'get', return_value=self.internal_server_error()) as m_get:
                self.assertRaises(TicketDaemonException, lambda: get_full_results('qid'))
                self.assertEqual(m_get.call_count, 3)

    def test_if_received_successful_response_returns_json_and_text(self):
        requests_get_result_mock = mock.Mock()
        requests_get_result_mock.text = '123'
        requests_get_result_mock.status_code = 200
        requests_get_result_mock.json = mock.Mock(return_value=dict(abc='123'))

        with mock.patch.object(requests, 'get', return_value=requests_get_result_mock) as m_get:
            json_result, text_result = get_full_results('qid')
            self.assertEqual(m_get.call_count, 1)

        self.assertEqual(text_result, '123')
        self.assertDictEqual(json_result, dict(abc='123'))
