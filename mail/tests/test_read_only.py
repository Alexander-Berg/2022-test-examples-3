# coding: utf-8
import json

from django.core.urlresolvers import reverse
from django.test import TestCase
from django.test.client import RequestFactory

from django_intranet_stuff.models import Staff, Group, GroupMembership
from mlcore.apiv2.views import readonly_maillists
from .base import ApiMixin



class ReadonlyTestCase(ApiMixin, TestCase):
    def setUp(self):
        super(ReadonlyTestCase, self).setUp()
        self.url = reverse('apiv2:readonly')
        self.factory = RequestFactory()
        self.view = readonly_maillists

    def get_default_response(self, **extra):
        return super(ReadonlyTestCase, self).get_default_response()

    def test_readonly(self, **extra):
        """
        Проверим что в выгрузке ридонли есть доступы у bad_user и user до рассылки readonly
        """
        response = self.get_default_response()

        data = json.loads(response.content)
        users = data[self.list_readonly.email]
        assert self.bad_user.staff.login in users
        assert self.user.staff.login in users
