# -*- coding: utf-8 -*-
import pytest

from base import BaseShardingMethods

import mpfs.engine.process
from mpfs.config import settings

dbctl = mpfs.engine.process.dbctl()


class CohabitationTestCase(BaseShardingMethods):
    def setup_method(self, method):
        super(CohabitationTestCase, self).setup_method(method)
        self.create_user(self.uid, noemail=1)
        settings.mongo['options']['new_registration'] = False
        self.create_user(self.old_scheme_uid, noemail=1)
        settings.mongo['options']['new_registration'] = True

    def upload_to_shared_and_move(self, uid1, uid2, path=None):
        opts = {
            'uid': uid2,
            'src': path,
            'dst': '/disk/sharedmoved/'
        }
        self.json_ok('async_move', opts)

        self.upload_file(uid1, '%s/random.file' % path)
        file_info = self.json_ok('info', {
            'uid': uid2,
            'path': '/disk/sharedmoved/random.file',
            'meta': '',
        })
        assert file_info is not None

    def make_a_lot_of_folders(self, uid, path=None, amount=0):
        for i in xrange(amount):
            self.json_ok('mkdir', {'uid': uid, 'path': '%s/folder %s' % (path, i)})
            self.json_ok('mkdir', {'uid': uid, 'path': '%s/folder %s/subfolder' % (path, i)})

    def check_shared_folder_diff(self, owner, guest, path=None, rights=660, amount=0):
        # создаем ОП и создаем разных каталогов
        self.create_shared_folder(owner, guest, path=path, rights=rights)
        self.make_a_lot_of_folders(owner, path=path, amount=amount)

        # получаем версию приглашенного юзера
        guest_version = int(self.json_ok('user_info', {'uid': guest})['version'])

        # проверяем, что листинг папки работает
        list_result = self.json_ok('list', {'uid': guest, 'path': path})
        assert len(list_result), amount + 1

        # проверяем, что полный дифф на папку работает
        diff_result = self.json_ok('diff', {'uid': guest, 'path': path})
        assert len(diff_result['result']), amount * 2 + 1

        # проверяем, что версионный дифф на папку тоже работает
        self.json_ok('mkdir', {'uid': owner, 'path': '%s/new' % path})
        versioned_diff_result = self.json_ok('diff', {'uid': guest, 'path': path, 'version': guest_version})
        assert len(versioned_diff_result['result']), 1
        assert versioned_diff_result['result'][0]['key'] == '%s/new' % path

    @pytest.mark.skipif(True, reason='https://st.yandex-team.ru/CHEMODAN-32396')
    def test_old_user_hardlink(self):
        self.upload_and_hardlink(self.old_scheme_uid, self.uid)

    def test_new_user_hardlink(self):
        self.upload_and_hardlink(self.uid, self.old_scheme_uid)

    def test_old_user_share_folder(self):
        self.create_shared_folder(self.old_scheme_uid, self.uid, path='/disk/shared')
        self.upload_to_shared_and_move(self.old_scheme_uid, self.uid, path='/disk/shared')

    def test_new_user_share_folder(self):
        self.create_shared_folder(self.uid, self.old_scheme_uid, path='/disk/shared')
        self.upload_to_shared_and_move(self.uid, self.old_scheme_uid, path='/disk/shared')

    def test_old_user_share_folder_diff_by_new_user_small_folder(self):
        """
        Проверяем диффы на ОП
        https://st.yandex-team.ru/CHEMODAN-22644

        Тест для малой папки
        Владелец - на монгосе, друг - на шарде
        """
        self.check_shared_folder_diff(
            self.old_scheme_uid,
            self.uid,
            path='/disk/shared',
            rights=640,
            amount=10
        )

    def test_old_user_share_folder_diff_by_new_user_large_folder(self):
        """
        Проверяем диффы на ОП
        https://st.yandex-team.ru/CHEMODAN-22644

        Тест для большой папки
        Владелец - на монгосе, друг - на шарде
        """
        self.check_shared_folder_diff(
            self.old_scheme_uid,
            self.uid,
            path='/disk/shared',
            rights=640,
            amount=60
        )

    def test_new_user_share_folder_diff_by_old_user_small_folder(self):
        """
        Проверяем диффы на ОП
        https://st.yandex-team.ru/CHEMODAN-22644

        Тест для малой папки
        Владелец - на шарде, друг - на монгосе
        """
        self.check_shared_folder_diff(
            self.uid,
            self.old_scheme_uid,
            path='/disk/shared',
            rights=640,
            amount=10
        )

    def test_new_user_share_folder_diff_by_old_user_large_folder(self):
        """
        Проверяем диффы на ОП
        https://st.yandex-team.ru/CHEMODAN-22644

        Тест для большой папки
        Владелец - на шарде, друг - на монгосе
        """
        self.check_shared_folder_diff(
            self.uid,
            self.old_scheme_uid,
            path='/disk/shared',
            rights=640,
            amount=10
        )


