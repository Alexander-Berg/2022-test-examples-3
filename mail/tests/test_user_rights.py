# coding: utf-8

import json
from django.core.urlresolvers import reverse
from django.test import TestCase
from django.test.client import RequestFactory

from milkman.dairy import milkman

from mlcore.ml.models import MailList, Subscribers, SuidLookup, EmailSubscriber
from mlcore.subscribe.backends.yandex_team.models import YandexTeamBackendContext
from mlcore.permissions.models import GroupPermission, ListPermission, Type
from django_intranet_stuff.models import Staff, Group, GroupMembership
from django.contrib.auth.models import User

from mlcore.apiv2.views import check_rights, user_rights, readonly_maillists

from .base import ApiMixin, G

class UserRightsTestCase(ApiMixin, TestCase):
    def setUp(self):
        super(UserRightsTestCase, self).setUp()
        self.url = reverse('apiv2:user_rights')
        self.factory = RequestFactory()
        self.view = user_rights

        # имеет доступ но не подписан
        milkman.deliver(SuidLookup, login=self.bad_list.name, suid=self.bad_list.fsuid)
        milkman.deliver(ListPermission, user=self.bad_user, list=self.bad_list)

    def get_default_response(self, **extra):
        args = {'user_suid': self.user_suid}
        args.update(extra)
        res = super(UserRightsTestCase, self).get_default_response(**args)
        return res

    def test_neok(self):
        """ На неверные данные получаем `bad request` """
        response = self.get_default_response(user_suid=100500)
        assert response.status_code == 400

        request = self.factory.get(self.url, {})
        response = self.view(request)
        assert response.status_code == 400

    def test_check_subscriptions(self):
        """
        Проверим что пользователь подписан на несколько рассылок
        И может читать их все
        """
        response = self.get_default_response()
        data = filter(lambda x: x['subscribed'], json.loads(response.content))
        assert all(map(lambda x: x['permissions']['read'], data))
        assert all(map(lambda x: x['subscribed'], data))

    def test_check_non_subscriptions(self):
        """
        Проверим что чувак может читать рассылку на которую не подписан
        """
        response = self.get_default_response(user_suid=self.bad_user_suid)
        data = filter(lambda x: not x['subscribed'], json.loads(response.content))
        assert filter(lambda x: x['permissions']['read'], data)

    def test_write_rights(self):
        """
        Плохой чувак имеет доступ до ридонли рассылки на запись
        """
        response = self.get_default_response(user_suid=self.bad_user_suid)
        data = filter(lambda x: not x['subscribed'], json.loads(response.content))
        assert filter(lambda x: x['permissions']['write'], data)

    def test_write_group(self):
        """
        Чувак имеет доступ на запись через группу
        """
        response = self.get_default_response()
        data = json.loads(response.content)
        assert filter(lambda x: x['permissions']['write'], data)

