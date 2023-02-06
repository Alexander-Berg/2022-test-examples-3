# -*- coding: utf-8 -*-
from collections import defaultdict

import mock

from nose_parameterized import parameterized

import mpfs.engine.process
from mpfs.config import settings
from mpfs.core.billing import Client, Market
from mpfs.core.services.email_sender_service import email_sender
from mpfs.core.services.passport_service import passport
from mpfs.core.user.attach import AttachUser
from mpfs.core.user.base import User, NeedInit
from mpfs.core.user.constants import EMPTY_FILE_HASH_SHA256, EMPTY_FILE_HASH_MD5
from mpfs.core.user.standart import StandartUser
from test.fixtures.kladun import KladunMocker
from test.helpers.stubs.services import PassportStub
from test.parallelly.sharding.base import BaseShardingMethods


dbctl = mpfs.engine.process.dbctl()


def special_chars_workaround(testcase_func, param_num, param):
    return '%s_%s' % (testcase_func.__name__, param_num)


class UserTestCase(BaseShardingMethods):
    def setup_method(self, method):
        super(UserTestCase, self).setup_method(method)

        settings.mongo['options']['new_registration'] = True
        # если вдруг что пошло не так и остался неполноценный юзер, то доделываем и вычищаем
        self.create_user(self.uid, noemail=1)
        self.remove_created_users()

    def test_user_check(self):
        self.json_error('mkdir', {'uid': self.uid, 'path': '/disk/test'}, code=44)

    def test_user_create(self):
        self.json_ok('user_init', {'uid': self.uid})
        user_info = self.json_ok('user_info', {'uid': self.uid})
        assert 'shard' in user_info['db']
        self.created_users_uids.add(self.uid)

    def test_user_check_works(self):
        """
        Проверяем, что на только что созданном пользователе user_check отдает 0
        """
        res = self.json_ok('user_check', {'uid': self.uid})
        assert int(res['need_init']) == 1
        self.json_ok('user_init', {'uid': self.uid})
        res = self.json_ok('user_check', {'uid': self.uid})
        assert int(res['need_init']) == 0

    def test_attach_created_during_attachment_operation_processing(self):
        """
        Проверяем, что у пользователя появляются домен и папки связанные с аттачами только после
        загрузки чего-нибудь в /attach и обработки операции.
        """
        uid = self.uid
        self.json_ok('user_init', {'uid': uid})
        assert int(NeedInit(uid, type='attach')) == 1
        self.json_ok('store', {'uid': uid, 'path': '/disk/test'})
        assert int(NeedInit(uid, type='attach')) == 1
        result = self.json_ok('store', {'uid': uid, 'path': '/attach/test'})
        assert int(NeedInit(uid, type='attach')) == 1

        operation_id = result['oid']
        KladunMocker().mock_kladun_callbacks_for_store(uid, operation_id)
        assert int(NeedInit(uid, type='attach')) == 0

    def test_attach_created_after_attachment_hardlinks(self):
        """
        Та же проверка для случая, если для загружаемого файла можно создать хардлинк
        """
        self.json_ok('user_init', {'uid': self.uid})
        assert int(NeedInit(self.uid, type='attach')) == 1
        # Используем хэши пустого файла, чтобы гарантированно найти хардлинк
        opts = {
            'uid': self.uid,
            'path': '/attach/test',
            'md5': EMPTY_FILE_HASH_MD5,
            'sha256': EMPTY_FILE_HASH_SHA256,
            'size': 0
        }
        self.json_ok('store', opts)
        assert int(NeedInit(self.uid, type='attach')) == 0

    def test_user_create_from_attach(self):
        """
        Проверяем как заводится аттач-юзер в шардированной схеме
        и как он потом переключается обратно на обычного юзера
        """
        # проверяем что аттач влился
        self.upload_file(self.uid, '/attach/attach.txt')
        client = Client(self.uid)
        # Маркет должен быть задан
        assert client.attributes.market == Market.DEFAULT
        contents = self.json_ok('list', {'uid': self.uid, 'path': '/attach/'})
        assert contents[1]['name'] == 'attach.txt'
        self.created_users_uids.add(self.uid)

        # проверяем, что живет на шарде
        user_info = self.json_ok('user_info', {'uid': self.uid})
        assert 'shard' in user_info['db']
        assert user_info['db']['shard'] is not None
        registered_on_shard = user_info['db']['shard']

        # проверяем, что создался обычный пользователь и у него 10ГБ
        resp = self.json_ok('user_check', {'uid': self.uid})
        assert int(resp['need_init']) == 0

        user_info = self.json_ok('user_info', {'uid': self.uid})
        limit = user_info['space']['limit']
        assert limit == 10 * 1024 * 1024 * 1024  # 10ГБ
        assert user_info['db']['shard'] == registered_on_shard

    def test_attach_user_upgraded_to_standard_from_attach_and_got_welcome_mail_via_email_sender(self):
        """
        Проверить, что аттачевый пользователь после очередной загрузки аттача превращается в стандартного
        и ему отсылается приветственное письмо.
        """
        AttachUser.Create(self.uid)
        user = User(self.uid)
        assert isinstance(user, AttachUser)

        userinfo = passport.userinfo(self.uid)
        userinfo['has_disk'] = False

        with PassportStub(userinfo=userinfo), mock.patch.object(email_sender, 'send') as email_sender_send_mock:
            self.upload_file(self.uid, '/attach/attach.txt')
            email_sender_send_mock.assert_called_once()
        user = User(self.uid)
        assert isinstance(user, StandartUser)

    def test_existing_user_did_not_get_welcome_mail_after_attach(self):
        """
        Проверить, что уже существующий пользователь с Диском не получит приветственное письмо после загрузки
        аттача.
        """
        userinfo = passport.userinfo(self.uid)
        userinfo['has_disk'] = True  # <- влияет на отправку письма
        self.create_user(self.uid, noemail=1)

        with PassportStub(userinfo=userinfo), \
                mock.patch.object(StandartUser, 'send_welcome_mail') as mock_send_welcome_mail:
            self.upload_file(self.uid, '/attach/attach.txt')
            assert not mock_send_welcome_mail.called

    def test_not_send_welcome_email_to_new_user_with_noemail_flag(self):
        userinfo = passport.userinfo(self.uid)
        userinfo['has_disk'] = False
        with PassportStub(userinfo=userinfo), \
                mock.patch.object(StandartUser, 'send_welcome_mail') as send_welcome_mail_mock:
            self.json_ok('user_init', {'uid': self.uid, 'noemail': 1})
            assert not send_welcome_mail_mock.called

    def test_new_user_got_welcome_mail_via_email_sender(self):
        userinfo = passport.userinfo(self.uid)
        userinfo['has_disk'] = False
        with PassportStub(userinfo=userinfo), mock.patch.object(email_sender, 'send') as email_sender_send_mock:
            self.json_ok('user_init', {'uid': self.uid})
            email_sender_send_mock.assert_called_once()
