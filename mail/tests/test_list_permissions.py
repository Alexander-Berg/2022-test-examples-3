# coding: utf-8
import json

from django.core.urlresolvers import reverse
from django.test import TestCase
from django.test.client import RequestFactory

from django_intranet_stuff.models import Staff, Group, GroupMembership
from mlcore.apiv2.views import check_rights
from .base import ApiMixin


class ListPermissionsTestCase(ApiMixin, TestCase):
    def setUp(self):
        super(ListPermissionsTestCase, self).setUp()
        self.factory = RequestFactory()
        self.url = reverse('apiv2:check_rights')
        self.view = check_rights

    def get_default_response(self, **extra):
        args = {'user_suid': self.user_suid, 'folder_suid': self.list_open.fsuid}
        args.update(extra)
        res = super(ListPermissionsTestCase, self).get_default_response(**args)
        return res

    def test_neok(self):
        """ На неверные данные поулчаем `bad request` """
        response = self.get_default_response(user_suid=1)
        assert response.status_code == 400

        response = self.get_default_response(folder_suid=1)
        assert response.status_code == 400

        request = self.factory.get(self.url, {})
        response = self.view(request)
        assert response.status_code == 400

    def test_open(self):
        """ Проверим что открытая рассылка доступна для чтения неподписанным """
        response = self.get_default_response(user_suid=self.bad_user_suid)
        assert json.loads(response.content)['read']

    def test_close(self):
        """ Имеющий права может читать закрытую рассылку """
        response = self.get_default_response(folder_suid=self.list_closed.fsuid)
        assert response.status_code == 200
        assert json.loads(response.content)['read']

    def test_bad_close(self):
        """ человек не имеющий прав не может читать рассылку """
        extra = {'user_suid': self.bad_user_suid,
                 'folder_suid': self.list_closed.fsuid}
        response = self.get_default_response(**extra)
        assert response.status_code == 200
        assert json.loads(response.content)['read'] is False

    def test_group(self):
        """ человек в группе с правами на рассылку может читать """
        response = self.get_default_response(folder_suid=self.list_group.fsuid)
        assert response.status_code == 200
        assert json.loads(response.content)['read']

    def test_bad_group(self):
        """ человек не в группе с правами на рассылку не может читать """
        extra = {'user_suid': self.bad_user_suid,
                 'folder_suid': self.list_group.fsuid}
        response = self.get_default_response(**extra)
        assert response.status_code == 200
        assert json.loads(response.content)['read'] is False

