# -*- coding: utf-8 -*-

from __future__ import unicode_literals

from mpfs.core.user.base import User
from test.helpers.stubs.services import PassportStub
from test.helpers.stubs.users import MailishUserStub
from test.helpers.stubs.manager import StubsManager
from test.parallelly.json_api.base import CommonJsonApiTestCase


class MailishUserTestCase(CommonJsonApiTestCase):
    """Набор тестов для пользователя типа `mailish`.

    https://wiki.yandex-team.ru/disk/mpfs/meps/mep-029/.
    """

    # отключаем дефолтную заглушку паспорта, так как нам нужная более тонкая
    # настройка для проверки
    stubs_manager = StubsManager(
        class_stubs=set(StubsManager.DEFAULT_CLASS_STUBS) - {PassportStub}
    )

    test_mailish_user_uid_1 = '4008083026'

    def run_000_user_check(self, uid=None):
        pass

    def test_user_init_for_mailish_user(self):
        uid = self.test_mailish_user_uid_1
        with MailishUserStub(uid=uid) as stub:
            self.json_ok('user_init', {
             'uid': uid
            })
            # проверяем, что мы не отправили письмо пользователю
            assert not stub.mocked_user_send_welcome_mail.called

        # проверяем, что основные ручки отрабатывают корректно
        self.json_ok('user_info', {'uid': uid})
        self.json_ok('info', {'uid': uid, 'path': '/disk'})
        self.json_ok('info', {'uid': uid, 'path': '/attach'})

        # проверяем, что выданы необходимые услуги
        result = self.billing_ok('service_list', {'uid': uid, 'ip': '127.0.0.1'})
        assert len(result) == 1
        [service] = result
        assert service['name'] == 'initial_10gb'
        assert service['expires'] is None

        # проверяем объект пользователя
        user = User(uid)
        assert user.is_mailish()

        # проверяем что ручка проверки инициализированности говорит что все ок
        # (мы не проверяем для таких пользователей подписку в паспорте)
        with MailishUserStub(uid=uid):
            result = self.json_ok('user_check', {'uid': uid})
            assert result['need_init'] == '0'

    def test_public_info_for_mailish_user(self):
        uid = self.test_mailish_user_uid_1
        email = 'yndx.test.mailish.1@gmail.com'
        with MailishUserStub(uid=uid, email=email, display_name=email):
            self.json_ok('user_init', {
             'uid': uid
            })

        self.upload_file(uid, '/attach/test.docx')
        attach_list = self.json_ok('list', {'uid': uid, 'path': '/attach', 'meta': ''})
        public_hash = None
        path = None
        for resource in attach_list:
            if resource['name'] != 'test.docx':
                continue
            public_hash = resource['meta']['public_hash']
            path = resource['path']
            break

        assert path
        assert public_hash

        with MailishUserStub(
            uid=uid, email=email, display_name=email,
            regname=email, login=email
        ):
            result = self.json_ok('public_info', {'private_hash': public_hash})
            assert 'user' in result
            assert result['user']['display_name'] == email
            assert result['user']['login'] == email
            assert result['user']['username'] == email

    def test_public_url_for_mailish_user(self):
        uid = self.test_mailish_user_uid_1
        with MailishUserStub(uid=uid):
            self.json_ok('user_init', {
             'uid': uid
            })

        self.upload_file(uid, '/attach/test.docx')
        attach_list = self.json_ok('list', {'uid': uid, 'path': '/attach', 'meta': ''})
        public_hash = None
        path = None
        for resource in attach_list:
            if resource['name'] != 'test.docx':
                continue
            public_hash = resource['meta']['public_hash']
            path = resource['path']
            break

        assert path
        assert public_hash

        with MailishUserStub(uid=uid):
            self.json_ok('public_url', {'private_hash': public_hash})

    def test_check_user_init_returns_true_for_mailish(self):
        uid = self.test_mailish_user_uid_1
        email = 'yndx.test.mailish.1@gmail.com'
        with MailishUserStub(uid=uid, email=email, display_name=email):
            resp = self.json_ok('can_init_user', {'uid': uid})
        assert resp['can_init'] == '1'
