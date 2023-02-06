# -*- coding: utf-8 -*-
import json
import unittest

import requests_mock
from django.conf import settings

from travel.avia.ticket_daemon.ticket_daemon.application import create_app
from travel.avia.library.python.tester.utils.replace_setting import replace_setting

TEST_APP_CONFIG = {}


def get_client_app():
    return create_app(TEST_APP_CONFIG).test_client()


class TestBookRedirectData(unittest.TestCase):
    BOOKING_RESPONSE = 'booking response content'

    def _send(self):
        return get_client_app().post(
            '/api/1.0/book_redirect/',
            headers={'Content-type': 'application/json'},
            data='{}'
        )

    @replace_setting('BOOKING_SERVICE_URL', 'http://test.booking.ru/')
    def test_book_redirect_ok_returned_status(self):
        url = 'URL'
        with requests_mock.Mocker() as m:
            m.post(settings.BOOKING_SERVICE_URL, text='{"redirectUrl": "%s"}' % url, status_code=200)
            response = self._send()
            assert response.status_code == 200
            data = json.loads(response.data)
            assert data['url'] == url

    @replace_setting('BOOKING_SERVICE_URL', 'http://test.booking.ru/')
    def test_book_redirect_404_returned_status(self):
        with requests_mock.Mocker() as m:
            m.post(settings.BOOKING_SERVICE_URL, text=self.BOOKING_RESPONSE, status_code=404)
            response = self._send()
            assert response.status_code == 400
            self.assertDictEqual(
                json.loads(response.data),
                {
                    'message': 'Booking is not available',
                    'status_code': 404,
                    'content': self.BOOKING_RESPONSE,
                }
            )

    @replace_setting('BOOKING_SERVICE_URL', 'http://test.booking.ru/')
    def test_book_redirect_500_returned_status(self):
        with requests_mock.Mocker() as m:
            m.post(settings.BOOKING_SERVICE_URL, text=self.BOOKING_RESPONSE, status_code=500)
            response = self._send()
            assert response.status_code == 400
            self.assertDictEqual(
                json.loads(response.data),
                {
                    'message': 'Booking is not available',
                    'status_code': 500,
                    'content': self.BOOKING_RESPONSE,
                }
            )
