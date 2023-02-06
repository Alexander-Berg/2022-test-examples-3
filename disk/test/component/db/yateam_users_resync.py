# -*- coding: utf-8 -*-
from test.helpers.stubs.services import StaffServiceMockHelper, PassportStub
from test.common.sharding import CommonShardingMethods
from test.parallelly.json_api.base import CommonJsonApiTestCase

from mpfs.core.billing.client import Client
from mpfs.core.billing.service import ServiceList
from mpfs.core.yateam.logic import resync_yateam_user, make_yateam, YATEAM_DIR_PATH

from mpfs.core.factory import get_resource_by_path
from mpfs.common.errors import ResourceNotFound


class ResetYaTeamUserTestCase(CommonJsonApiTestCase, CommonShardingMethods):
    """Класс тестов для пересинхронизации ятимных пользователей.
    """
    def setup_method(self, method):
        super(ResetYaTeamUserTestCase, self).setup_method(method)
        self.yateam_uid = '456'
        self.create_user(self.uid, shard=self.mongodb_unit2)
        PassportStub.update_info_by_uid(self.uid, is_2fa_enabled=True)

        self.yateam_uid_1 = '123'
        self.create_user(self.uid_1, shard=self.mongodb_unit2)
        PassportStub.update_info_by_uid(self.uid_1, is_2fa_enabled=True)
        make_yateam(self.uid_1, self.yateam_uid_1)

        self.yateam_uid_3 = '321'
        self.create_user(self.uid_3, shard=self.mongodb_unit2)
        PassportStub.update_info_by_uid(self.uid_3, is_2fa_enabled=True)
        make_yateam(self.uid_3, self.yateam_uid_3)

    def test_new_yateam_user(self):
        """Протестировать создание папки и 250ГБ для пользователя, который не считается в нашей базе
        яндексоидом, а на стаффе он есть.
        """
        assert not self.check_user_has_yandex_staff_service(self.uid)
        assert not self.check_yateam_root_exists(self.uid)
        with StaffServiceMockHelper.mock_get_user_info(self.yateam_uid, uid=self.uid):
            resync_yateam_user(self.yateam_uid)
        assert self.check_user_has_yandex_staff_service(self.uid)
        assert self.check_yateam_root_exists(self.uid)

    def test_yateam_user_removed_login(self):
        """Протестировать удаление папки и 250ГБ для пользователя,
        который в нашей базе считается яндексоидом, а на стаффе он отвязал логин.
        """
        assert self.check_user_has_yandex_staff_service(self.uid_1)
        assert self.check_yateam_root_exists(self.uid_1)
        with StaffServiceMockHelper.mock_get_user_info(self.yateam_uid_1, login=None):
            resync_yateam_user(self.yateam_uid_1)
        assert not self.check_user_has_yandex_staff_service(self.uid_1)
        assert not self.check_yateam_root_exists(self.uid_1)

    def test_yateam_user_changed_login(self):
        """Протестировать удаление у старого логина и создание у нового логина папки и 250ГБ для пользователя,
        который в нашей базе считается яндексоидом, а на стаффе он поменял логин.
        """
        assert not self.check_user_has_yandex_staff_service(self.uid)
        assert not self.check_yateam_root_exists(self.uid)

        assert self.check_user_has_yandex_staff_service(self.uid_1)
        assert self.check_yateam_root_exists(self.uid_1)
        with StaffServiceMockHelper.mock_get_user_info(self.yateam_uid_1, uid=self.uid, login=self.login):
            resync_yateam_user(self.yateam_uid_1)
        assert not self.check_user_has_yandex_staff_service(self.uid_1)
        assert not self.check_yateam_root_exists(self.uid_1)

        assert self.check_user_has_yandex_staff_service(self.uid)
        assert self.check_yateam_root_exists(self.uid)

    def test_yateam_user_dismissed(self):
        """Протестировать удаление папки и 250ГБ для пользователя, который в нашей базе считается ятимным,
        а на стаффе он считается уволенным.
        """
        with StaffServiceMockHelper.mock_get_user_info(self.yateam_uid_1, uid=self.uid_1, is_dismissed=True):
            resync_yateam_user(self.yateam_uid_1)

        assert self.check_user_has_yandex_staff_service(self.uid_1)
        assert not self.check_yateam_root_exists(self.uid_1)

    @staticmethod
    def check_user_has_yandex_staff_service(uid):
        services = ServiceList(client=Client(uid))
        return any(s for s in services if s['pid'] == 'yandex_staff')

    @staticmethod
    def check_yateam_root_exists(uid):
        try:
            get_resource_by_path(uid, YATEAM_DIR_PATH)
        except ResourceNotFound:
            return False
        return True
