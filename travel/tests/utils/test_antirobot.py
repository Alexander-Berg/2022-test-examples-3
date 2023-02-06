# -*- coding: utf-8 -*-
import mock

from travel.avia.library.python.common.utils.antirobot import AntirobotApplication
from travel.avia.library.python.tester.testcase import TestCase


class TestAntirobotApplication(TestCase):
    def setUp(self):
        self.default_environ = {
            'QUERY_STRING': None,
            'PATH_INFO': '/',
            'HTTP_X_REAL_IP': '8.8.8.8',
            'REQUEST_METHOD': 'GET'
        }
        self.start_response = StartResponse()
        self.wsgi_app = Application()
        self.antirobot_app = AntirobotApplication(self.wsgi_app)

    @mock.patch('travel.avia.library.python.common.utils.antirobot.antirobot_yt_log')
    @mock.patch('travel.avia.library.python.common.utils.antirobot.requests')
    def test_antirobot_application_pass(self, mock_requests, mock_logger):
        mock_logger.log = LogMethod()
        mock_requests.get.return_value = mock.Mock(status_code=200)

        response_body = self.antirobot_app(self.default_environ, self.start_response)

        assert response_body is None
        assert self.start_response.status is None
        assert self.start_response.headers is None

        assert mock_logger.log.log_data.get('is_robot') == '0'
        assert mock_logger.log.log_data.get('status_code') not in [302, 403]
        assert mock_logger.log.log_data.get('http_x_real_ip') == '8.8.8.8'
        assert mock_logger.log.log_data.get('tskv_format') == 'rasp-antirobot-log'

    @mock.patch('travel.avia.library.python.common.utils.antirobot.antirobot_yt_log')
    @mock.patch('travel.avia.library.python.common.utils.antirobot.requests')
    def test_antirobot_application_captha(self, mock_requests, mock_logger):
        mock_logger.log = LogMethod()
        mock_requests.get.return_value = mock.Mock(status_code=302, headers={'Location': 'YANDEX'})

        response_body = self.antirobot_app(self.default_environ, self.start_response)

        assert response_body == []
        assert self.start_response.status == '302 Redirect'
        assert self.start_response.headers == [('Location', 'YANDEX')]

        assert mock_logger.log.log_data.get('is_robot') == '1'
        assert mock_logger.log.log_data.get('status_code') == 302
        assert mock_logger.log.log_data.get('http_x_real_ip') == '8.8.8.8'
        assert mock_logger.log.log_data.get('tskv_format') == 'rasp-antirobot-log'

    @mock.patch('travel.avia.library.python.common.utils.antirobot.antirobot_yt_log')
    @mock.patch('travel.avia.library.python.common.utils.antirobot.requests')
    def test_antirobot_application_block(self, mock_requests, mock_logger):
        mock_logger.log = LogMethod()
        mock_requests.get.return_value = mock.Mock(status_code=403, content='BLOCK CONTENT')

        response_body = self.antirobot_app(self.default_environ, self.start_response)

        assert response_body == ['BLOCK CONTENT']
        assert self.start_response.status == '200 OK'
        assert self.start_response.headers == [('Content-Type', 'text/html')]

        assert mock_logger.log.log_data.get('is_robot') == '1'
        assert mock_logger.log.log_data.get('status_code') == 403
        assert mock_logger.log.log_data.get('http_x_real_ip') == '8.8.8.8'
        assert mock_logger.log.log_data.get('tskv_format') == 'rasp-antirobot-log'

    def test_clean_x_forwarded_for(self):
        assert self.antirobot_app.fix_x_forwarded_for('8.8.8.8') == '8.8.8.8'
        assert self.antirobot_app.fix_x_forwarded_for('8.8.8.8, 9.9.9.9') == '8.8.8.8, 9.9.9.9'
        assert self.antirobot_app.fix_x_forwarded_for('::ffff:8.8.8.8') == '8.8.8.8'
        assert self.antirobot_app.fix_x_forwarded_for('9.9.9.9, ::ffff:8.8.8.8') == '9.9.9.9, 8.8.8.8'
        assert self.antirobot_app.fix_x_forwarded_for('::ffff:8.8.8.8, 9.9.9.9') == '8.8.8.8, 9.9.9.9'


class StartResponse(object):
    def __init__(self):
        self.status = None
        self.headers = None

    def __call__(self, status, headers):
        self.status = status
        self.headers = headers


class Application(object):
    def __call__(self, environ, start_response):
        self.environ = environ
        self.start_response = start_response


class LogMethod(object):
    def __init__(self):
        self.log_data = None

    def __call__(self, log_data):
        self.log_data = log_data
