# coding: utf-8
import json
from django.core.urlresolvers import reverse
from django.test import TestCase
from django.test.client import RequestFactory

from mlcore.apiv2.views import mass_lists_subscribers

from .base import ApiMixin, create_token, create_maillist


TOKEN = 'cal'

class CalendarTestCase(ApiMixin, TestCase):

    def setUp(self):
        super(CalendarTestCase, self).setUp()

        self.url = reverse('apiv2:calendar-listinfo')
        self.factory = RequestFactory()
        self.view = mass_lists_subscribers
        create_token(TOKEN)

        # рассылка подписана на рассылку
        self.a = create_maillist('cal-a', inbox=['cal-u1', ])
        self.b = create_maillist('cal-b', inbox=['cal-u2', ], emails=[self.a.email, ])

    def test_ok(self):
        """ Проверим что отвечает 400 на пустой запрос """
        response = self.get_default_response(token=TOKEN)
        assert response.status_code == 400

    def test_calendar_maillist_info(self, **extra):
        """
        Базовая проверка mass_lists_subscribers
        """

        # открытая рассылка
        data = self.get_json_response(emails=self.list_open.email, token=TOKEN)
        print __name__, "test_calendar_maillist_info self.list_open", data
        data = data['result'][self.list_open.email]
        #  {u'result': {u'list_open@yandex-team.ru': {u'is_internal': True, u'is_open': True, u'subscribers': [{u'login': u'chuck_norris', u'email': u'', u'imap': False, u'inbox': False}]}}}
        self.assertIs(data['is_internal'], True)
        self.assertIs(data['is_open'], True)
        self.assertEquals(len(data['subscribers']), 1)

        # закрытая рассылка
        data = self.get_json_response(emails=self.list_readonly.email, token=TOKEN)
        print __name__, "test_calendar_maillist_info self.list_readonly", data
        data = data['result'][self.list_readonly.email]
        self.assertIs(data['readonly'], True)
        self.assertEquals(data['who_can_write'], [])

        # рассылка подписана на рассылку
        data = self.get_json_response(emails=self.b.email, expand='yes', token=TOKEN)
        print __name__, "test_calendar_maillist_info self.a", data
        data = data['result'][self.b.email]
        self.assertTrue('cal-u1@yandex-team.ru' in [d['email'] for d in data['subscribers']])

