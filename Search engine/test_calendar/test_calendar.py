# coding: utf-8

"""
FIXME
"""

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import json
import requests_mock

from collections import namedtuple

from search.martylib.calendar.client import CalendarClient
from search.martylib.calendar.exceptions import BadCalendarResponse, CalendarDeleteError, EventDeleteError
from search.martylib.test_utils import TestCase


@requests_mock.Mocker()
class TestCalendar(TestCase):

    uid = 666

    base_url = 'https://calendar-api.calcorp-test-back.cmail.yandex.net/internal'

    dates = namedtuple('begin_date', 'end_date')
    dates.begin_date = '2000-01-01T00:00:00'
    dates.end_date = '2000-01-01T00:00:00'

    @classmethod
    def setUpClass(cls):
        super(TestCalendar, cls).setUpClass()
        cls.calendar_client = CalendarClient(666, oauth_token='XXX')

    def test_create_calendar_invalid_response(self, m):
        m.get(
            '{}/create-layer?uid={}'.format(self.base_url, self.uid),
            text='{}'
        )
        with self.assertRaises(BadCalendarResponse):
            self.calendar_client.create_calendar('test', True, ['test@test.ru'])

    def test_create_calendar_normal_response(self, m):
        m.get(
            '{}/create-layer?uid={}'.format(self.base_url, self.uid),
            text=json.dumps({'layerId': 100500})
        )
        self.assertEqual(self.calendar_client.create_calendar('test', True, ['test@test.ru']), 100500)

    def test_create_calendar_invalid_key_response(self, m):
        m.get(
            '{}/create-layer?uid={}'.format(self.base_url, self.uid),
            text=json.dumps({'layerId': 'null'})
        )
        with self.assertRaises(BadCalendarResponse):
            self.calendar_client.create_calendar('test', True, ['test@test.ru'])

    def test_create_event_invalid_response(self, m):
        m.get(
            '{}/create-event?uid={}&tz={}'.format(self.base_url, self.uid, 'Europe/Moscow'),
            text='{}'
        )
        with self.assertRaises(BadCalendarResponse):
            self.calendar_client.create_event(0, 'event', self.dates, 'epsilond1')

    def test_create_event_bad_response(self, m):
        m.get(
            '{}/create-event?uid={}&tz={}'.format(self.base_url, self.uid, 'Europe/Moscow'),
            text=json.dumps({'status': 'ok'})
        )
        with self.assertRaises(BadCalendarResponse):
            self.calendar_client.create_event(0, 'event', self.dates, 'epsilond1')

    def test_create_event_wrong_type_id(self, m):
        m.get(
            '{}/create-event?uid={}&tz={}'.format(self.base_url, self.uid, 'Europe/Moscow'),
            text=json.dumps({'status': 'ok', 'showEventId': 'error'})
        )
        with self.assertRaises(BadCalendarResponse):
            self.calendar_client.create_event(0, 'event', self.dates, 'epsilond1')

    def test_create_event_all_right(self, m):
        m.get(
            '{}/create-event?uid={}&tz={}'.format(self.base_url, self.uid, 'Europe/Moscow'),
            text=json.dumps({'status': 'ok', 'showEventId': 42})
        )
        self.assertEqual(self.calendar_client.create_event(0, 'event', self.dates, 'epsilond1'), 42)

    def test_delete_calendar_exception(self, m):
        m.get(
            '{}/delete-layer?uid={}&id={}'.format(self.base_url, self.uid, 1),
            text=json.dumps({'status': 'fckup'})
        )
        with self.assertRaises(CalendarDeleteError):
            self.calendar_client.delete_calendar(1)

    def test_delete_event_exception(self, m):
        m.get(
            '{}/delete-event?uid={}&id={}'.format(self.base_url, self.uid, 1),
            text=json.dumps({})
        )
        with self.assertRaises(EventDeleteError):
            self.calendar_client.delete_event(1)
