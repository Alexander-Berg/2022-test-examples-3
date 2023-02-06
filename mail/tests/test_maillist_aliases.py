# coding: utf-8
import json

from django.core.urlresolvers import reverse
from django.test import TestCase
from django.test.client import RequestFactory

from mlcore.aliases.views import add_alias
from .base import create_token, create_maillist

TOKEN = 'alias'


class CreateMaillistAlias(TestCase):
    def setUp(self):
        super(CreateMaillistAlias, self).setUp()
        self.url = reverse('apiv2:alias_create')
        self.factory = RequestFactory()
        self.view = add_alias
        create_token(TOKEN)

    def post_default_response(self, **args):
        request = self.factory.post(self.url, args)
        response = self.view(request)
        return response

    def post_json_response(self, **extra):
        return json.loads(self.post_default_response(**extra).content)

    def test_ok(self):
        """ Проверим что отвечает 400 на пустой запрос """
        response = self.post_default_response(token=TOKEN)
        assert response.status_code == 400

    # ===== Простейшие тесты для создания алиасов. Ручка асинхронная, поэтому статус ответа только ok. =====

    def test_auto_ru(self):
        """
        Проверим, что рассылки auto.ru с точками создаются
        """
        data = self.post_json_response(email='want.to.test@auto.ru',
                                       destinations='want-to-test-at-auto-ru@yandex-team.ru', token=TOKEN)
        assert data['status'] == 'ok'

    def test_tcrm_users(self):
        """
        Проверим, что tcrm-пользователи создаются
        """
        data = self.post_json_response(
            email='v.fantasy@yandex-team.ru',
            destinations='v-fantasy@mail.yandex-team.ru, tcrm@yandex-team.ru, tcrmlite@yandex-team.ru', token=TOKEN)
        assert data['status'] == 'ok'

    def test_exist_maillist(self):
        """
        Проверим, что на существующую рассылку email-ы подпишутся
        """
        m = create_maillist('test.test')
        data = self.post_json_response(email=m.email,
                                       destinations='tcrm@yandex-team.ru, separator@separator.yandex-team.ru', token=TOKEN)
        assert data['status'] == 'ok'

    # ===== =====

    def test_lifecycle_service_alias(self):
        """
        Проверить процесс создания или добавления подписчика для tcrm@, otrs@, separator@
        """
        pass

    def test_not_create_maillist_for_user_in_passport(self):
        """
        Проверить, что не создаем рассылку в базе ML для пользователя, который есть в паспорте.
        """
        pass

