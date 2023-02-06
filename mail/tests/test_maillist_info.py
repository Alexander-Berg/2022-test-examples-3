# coding: utf-8
import json

from django.core.urlresolvers import reverse
from django.test import TestCase
from django.test.client import RequestFactory

from mlcore.apiv2.views.lists_exists import lists_exists
from .base import ApiMixin


class ListsExistsTestCase(ApiMixin, TestCase):
    def setUp(self):
        super(ListsExistsTestCase, self).setUp()
        self.url = reverse('apiv2:maillist_exists')
        self.factory = RequestFactory()
        self.view = lists_exists

    def test_lists_exists(self, **extra):
        response = self.get_default_response(emails='a@b.c,%s' % self.list_open.email)
        data = json.loads(response.content)
        maillists = data['maillists']
        assert self.list_open.email in maillists
        assert 'a@b.c' not in maillists
