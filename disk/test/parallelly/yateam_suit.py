# -*- coding: utf-8 -*-
import mock

from hamcrest import assert_that, matches_regexp
from nose_parameterized import parameterized

from test.base import get_search_item_response
from test.base_suit import patch_http_client_open_url
from test.helpers.stubs.services import PassportStub, StaffServiceMockHelper, StaffServiceSmartMockHelper, RateLimiterStub
from test.helpers.stubs.manager import StubsManager
from test.parallelly.json_api.base import CommonJsonApiTestCase
from test.parallelly.office_suit import OfficeTestCase

import mpfs.engine.process

from mpfs.common import errors
from mpfs.common.static import codes
from mpfs.common.errors import ResourceNotFound
from mpfs.core.metastorage.control import user_index
from mpfs.core.user.constants import YATEAM_DIR_PATH

from mpfs.core.address import ResourceId, Address
from mpfs.core.factory import get_resource_by_resource_id, get_resource_by_address
from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase
from mpfs.core.services.search_service import DiskSearch
from mpfs.core.social.publicator import Publicator
from mpfs.core.user.base import User
from mpfs.core.yateam.logic import is_yateam_subtree, resync_yateam_users, resync_user
from mpfs.metastorage.mongo.collections.blockings import BlockingsCollection

db = CollectionRoutedDatabase()


class BaseYaTeamTestCase(CommonJsonApiTestCase):
    YATEAM_UID_1 = '1234'
    YATEAM_UID_2 = '123456'
    stubs_manager = StubsManager(class_stubs=set(StubsManager.DEFAULT_CLASS_STUBS) | {RateLimiterStub})

    def setup_method(self, method):
        super(BaseYaTeamTestCase, self).setup_method(method)
        StaffServiceSmartMockHelper.clear_user_info_cache()

    def _make_yateam(self, uid, is_dismissed=False, yateam_uid='1234'):
        if uid:
            PassportStub.update_info_by_uid(uid, is_2fa_enabled=True)
        with StaffServiceMockHelper.mock_get_user_info(yateam_uid, uid=uid, is_dismissed=is_dismissed):
            self.json_ok('staff_user_changed_callback', {'yateam_uid': yateam_uid})

    def _assert_nda_folder(self, uid):
        self.json_ok('list', {'uid': uid, 'path': YATEAM_DIR_PATH, 'show_nda': 1})

    def _assert_no_nda_folder(self, uid):
        self.json_error('list', {'uid': uid, 'path': YATEAM_DIR_PATH, 'show_nda': 1}, code=codes.LIST_NOT_FOUND)

    def _assert_user_has_yateam_uid(self, uid, yateam_uid):
        if yateam_uid:
            assert user_index.find({'_id': uid})[0]['yateam_uid'] == yateam_uid
        else:
            assert 'yateam_uid' not in user_index.find({'_id': uid})[0]

    def _assert_user_has_yateam_service(self, uid):
        services_names = [x['name'] for x in self.billing_ok('service_list', {'uid': uid, 'ip': 'localhost'})]
        assert 'yandex_staff' in services_names

    def _assert_user_does_not_have_yateam_service(self, uid):
        services_names = [x['name'] for x in self.billing_ok('service_list', {'uid': uid, 'ip': 'localhost'})]
        assert 'yandex_staff' not in services_names


class YaTeamTestCase(BaseYaTeamTestCase):

    def test_staff_creation_by_callback(self):
        self._assert_no_nda_folder(self.uid)
        self._make_yateam(self.uid)
        self._assert_nda_folder(self.uid)
        self._assert_user_has_yateam_uid(self.uid, self.YATEAM_UID_1)
        self._assert_user_has_yateam_service(self.uid)

    def test_staff_yateam_unlinked(self):
        self._make_yateam(self.uid)
        self._make_yateam(None)

        self._assert_user_has_yateam_uid(self.uid, None)
        self._assert_user_does_not_have_yateam_service(self.uid)

    def test_staff_link_changed(self):
        self._make_yateam(self.uid)
        self._make_yateam(None)

        self.json_ok('user_init', {'uid': self.uid_1})
        self._make_yateam(self.uid_1)

        self._assert_user_has_yateam_uid(self.uid, None)
        self._assert_user_does_not_have_yateam_service(self.uid)

        self._assert_user_has_yateam_uid(self.uid_1, self.YATEAM_UID_1)
        self._assert_user_has_yateam_service(self.uid_1)

    def test_staff_yateam_link_was_changed_without_removing_current_link(self):
        """Проверяем изменение ятимного логина при пропщуенном колбеке

        При изменении в стаффе привязанного аккаунта поведение должно выглядеть следующим образом:
            колбек -> на стаффе отсутствует прилинкованный логин -> удаляем свойства ятимности
            колбек -> на стаффе появлися новый прилинованный логин -> добавялем ятимные ништяки новому пользователю

        В данном тесте проверям нештатное поведение, когда первый колбек не пришел и при походе в стафф мы обнаруживаем,
        что прилинкованный логин изменился. В таком случае мы удаляем ятимность для старого пользователя и добавляем
        для нового.
        """
        self._make_yateam(self.uid)

        self.json_ok('user_init', {'uid': self.uid_1})
        self._make_yateam(self.uid_1)

        self._assert_user_has_yateam_uid(self.uid, None)
        self._assert_user_does_not_have_yateam_service(self.uid)

        self._assert_user_has_yateam_uid(self.uid_1, self.YATEAM_UID_1)
        self._assert_user_has_yateam_service(self.uid_1)

    def test_staff_yateam_dismissed_user(self):
        self._make_yateam(self.uid)
        self._make_yateam(self.uid, True)
        self._assert_user_has_yateam_uid(self.uid, None)
        self._assert_user_has_yateam_service(self.uid)

    def test_yateam_dir_already_exists(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': YATEAM_DIR_PATH})
        self.upload_file(self.uid, YATEAM_DIR_PATH + '/test_1')
        correct_list_res = self.json_ok('list', {'uid': self.uid, 'path': YATEAM_DIR_PATH})
        self._make_yateam(self.uid)
        assert self.json_ok('list', {'uid': self.uid, 'path': YATEAM_DIR_PATH}) == correct_list_res
        self._assert_user_has_yateam_uid(self.uid, self.YATEAM_UID_1)

    def test_yateam_user_has_no_disk(self):
        self._make_yateam(self.uid_1)
        assert self.json_ok('user_check', {'uid': self.uid_1})['need_init']

    def test_create_yadisk_during_init(self):
        StaffServiceSmartMockHelper.add_user_info(self.YATEAM_UID_1, uid=self.uid_1)
        PassportStub.update_info_by_uid(self.uid_1, has_staff=True, is_2fa_enabled=True)

        with mock.patch('mpfs.core.yateam.logic.YATEAM_ENABLE_FOR_ALL', True), \
                mock.patch('mpfs.core.yateam.logic.YATEAM_ENABLED_YATEAM_UIDS', []), \
                StaffServiceSmartMockHelper.mock_get_user_info():
            self.json_ok('user_init', {'uid': self.uid_1})

        self._assert_nda_folder(self.uid_1)
        self._assert_user_has_yateam_uid(self.uid_1, self.YATEAM_UID_1)

    def test_nda_folder_not_created_during_init_if_yateam_user_is_dismissed(self):
        StaffServiceSmartMockHelper.add_user_info(self.YATEAM_UID_1, uid=self.uid_1, is_dismissed=True)
        PassportStub.update_info_by_uid(self.uid_1, has_staff=True, is_2fa_enabled=True)

        with mock.patch('mpfs.core.yateam.logic.YATEAM_ENABLE_FOR_ALL', True), \
                mock.patch('mpfs.core.yateam.logic.YATEAM_ENABLED_YATEAM_UIDS', []), \
                StaffServiceSmartMockHelper.mock_get_user_info():
            self.json_ok('user_init', {'uid': self.uid_1})

        self._assert_no_nda_folder(self.uid_1)
        self._assert_user_has_yateam_uid(self.uid_1, None)

    def test_nda_folder_not_created_during_init_if_2fa_is_disabled(self):
        StaffServiceSmartMockHelper.add_user_info(self.YATEAM_UID_1, uid=self.uid_1)
        PassportStub.update_info_by_uid(self.uid_1, has_staff=True, is_2fa_enabled=False)

        with mock.patch('mpfs.core.yateam.logic.YATEAM_ENABLE_FOR_ALL', True), \
                mock.patch('mpfs.core.yateam.logic.YATEAM_ENABLED_YATEAM_UIDS', []), \
                StaffServiceSmartMockHelper.mock_get_user_info():
            self.json_ok('user_init', {'uid': self.uid_1})

        self._assert_no_nda_folder(self.uid_1)
        self._assert_user_has_yateam_uid(self.uid_1, self.YATEAM_UID_1)

    def test_nda_folder_not_created_during_init_if_yateam_is_disabled(self):
        StaffServiceSmartMockHelper.add_user_info(self.YATEAM_UID_1, uid=self.uid_1)
        PassportStub.update_info_by_uid(self.uid_1, has_staff=True, is_2fa_enabled=True)

        with mock.patch('mpfs.core.yateam.logic.YATEAM_ENABLE_FOR_ALL', False), \
                mock.patch('mpfs.core.yateam.logic.YATEAM_ENABLED_YATEAM_UIDS', []), \
                StaffServiceSmartMockHelper.mock_get_user_info():
            self.json_ok('user_init', {'uid': self.uid_1})

        self._assert_no_nda_folder(self.uid_1)
        self._assert_user_has_yateam_uid(self.uid_1, self.YATEAM_UID_1)

    def test_nda_folder_created_during_init_if_yateam_is_disabled_and_yateam_uid_is_in_exception_list(self):
        StaffServiceSmartMockHelper.add_user_info(self.YATEAM_UID_1, uid=self.uid_1)
        PassportStub.update_info_by_uid(self.uid_1, has_staff=True, is_2fa_enabled=True)

        with mock.patch('mpfs.core.yateam.logic.YATEAM_ENABLE_FOR_ALL', False), \
                mock.patch('mpfs.core.yateam.logic.YATEAM_ENABLED_YATEAM_UIDS', [self.YATEAM_UID_1]), \
                StaffServiceSmartMockHelper.mock_get_user_info():
            self.json_ok('user_init', {'uid': self.uid_1})

        self._assert_nda_folder(self.uid_1)
        self._assert_user_has_yateam_uid(self.uid_1, self.YATEAM_UID_1)

    def test_yateam_dir_in_default_folders(self):
        default_folders = self.json_ok('default_folders', {'uid': self.uid, 'check_exist': 1})
        assert not default_folders['yateamnda']['exist']
        self._make_yateam(self.uid)
        default_folders = self.json_ok('default_folders', {'uid': self.uid, 'check_exist': 1})
        assert default_folders['yateamnda']['exist']

    def test_show_nda(self):
        assert not mpfs.engine.process.get_show_nda()
        self.json_ok('user_info', {'uid': self.uid, 'show_nda': 1})
        assert mpfs.engine.process.get_show_nda()

    def test_staff_creation_by_admin_endpoint(self):
        self._assert_no_nda_folder(self.uid)
        PassportStub.update_info_by_uid(self.uid, is_2fa_enabled=True)
        self.json_ok('staff_make_yateam_admin', {'uid': self.uid, 'yateam_uid': self.YATEAM_UID_1})
        self._assert_nda_folder(self.uid)
        self._assert_user_has_yateam_uid(self.uid, self.YATEAM_UID_1)
        self._assert_user_has_yateam_service(self.uid)

    def test_staff_reset_by_admin_endpoint(self):
        PassportStub.update_info_by_uid(self.uid, is_2fa_enabled=True)
        with StaffServiceSmartMockHelper.mock_get_user_info():
            self.json_ok('staff_make_yateam_admin', {'uid': self.uid, 'yateam_uid': self.YATEAM_UID_1})
        self._assert_user_has_yateam_uid(self.uid, self.YATEAM_UID_1)
        self._assert_nda_folder(self.uid)

        with StaffServiceSmartMockHelper.mock_get_user_info():
            self.json_ok('staff_reset_yateam_admin', {'uid': self.uid})
        self._assert_user_has_yateam_uid(self.uid, None)
        self._assert_user_does_not_have_yateam_service(self.uid)
        self._assert_no_nda_folder(self.uid)

    def test_forced_yateam_user(self):
        PassportStub.update_info_by_uid(self.uid, is_2fa_enabled=True)
        with mock.patch('mpfs.core.services.staff_service.YATEAM_FORCE_ENABLED_UIDS', [self.uid]), \
                mock.patch('mpfs.core.yateam.logic._create_yateam_dir') as create_yateam_dir_stub, \
                mock.patch('mpfs.core.yateam.logic.reset_yateam') as reset_yateam_stub, \
                StaffServiceSmartMockHelper.mock_get_user_info():
            resync_user(self.uid)
            user = User(self.uid)
            assert user.is_yateam()
            resync_user(self.uid)
            assert create_yateam_dir_stub.call_count > 0 and reset_yateam_stub.call_count == 0


class PublicEndpointsReadAccessYaTeamTestCase(BaseYaTeamTestCase):
    def setup_method(self, method):
        super(PublicEndpointsReadAccessYaTeamTestCase, self).setup_method(method)

        self.json_ok('user_init', {'uid': self.uid_1})
        self.json_ok('user_init', {'uid': self.uid_3})

        self.owner_uid = self.uid_1
        self.yateam_uid = self.uid
        self.common_uid = self.uid_3

        self._make_yateam(self.yateam_uid)

        self.json_ok('mkdir', {'uid': self.owner_uid, 'path': YATEAM_DIR_PATH})
        self.json_ok('mkdir', {'uid': self.owner_uid, 'path': YATEAM_DIR_PATH + '/pub_dir'})
        self.upload_file(self.owner_uid, YATEAM_DIR_PATH + '/test_public')
        self.upload_video(self.owner_uid, YATEAM_DIR_PATH + '/video')
        self.upload_file(self.owner_uid, YATEAM_DIR_PATH + '/test_archive.zip')

        self.json_ok('mkdir', {'uid': self.owner_uid, 'path': YATEAM_DIR_PATH + '/pub_dir/nested_dir'})
        self.upload_file(self.owner_uid, YATEAM_DIR_PATH + '/pub_dir/nested')

        self.public_folder_hash = self.json_ok('set_public', {'uid': self.owner_uid, 'path': YATEAM_DIR_PATH + '/pub_dir'})['hash']
        self.public_file_hash = self.json_ok('set_public', {'uid': self.owner_uid, 'path': YATEAM_DIR_PATH + '/test_public'})['hash']
        self.public_video_hash = self.json_ok('set_public', {'uid': self.owner_uid, 'path': YATEAM_DIR_PATH + '/video'})['hash']
        self.public_archive_hash = self.json_ok('set_public', {'uid': self.owner_uid, 'path': YATEAM_DIR_PATH + '/test_archive.zip'})['hash']

    def test_set_public_yateam_root_fail(self):
        u"""Протестировать невозможность опубликовать корневую папку для любых пользователей."""
        self.json_ok('mkdir', {'uid': self.common_uid, 'path': YATEAM_DIR_PATH})
        self.json_error('set_public', {'uid': self.yateam_uid, 'path': YATEAM_DIR_PATH, 'show_nda': 0}, code=codes.YATEAM_DIR_MODIFY_ERROR)
        self.json_error('set_public', {'uid': self.yateam_uid, 'path': YATEAM_DIR_PATH, 'show_nda': 1}, code=codes.YATEAM_DIR_MODIFY_ERROR)
        self.json_error('set_public', {'uid': self.common_uid, 'path': YATEAM_DIR_PATH, 'show_nda': 0}, code=codes.YATEAM_DIR_MODIFY_ERROR)
        self.json_error('set_public', {'uid': self.common_uid, 'path': YATEAM_DIR_PATH, 'show_nda': 1}, code=codes.YATEAM_DIR_MODIFY_ERROR)

    def test_set_public_yateam_subtree_success(self):
        u"""Протестировать возможность опубликовать ресурсы внутри папки YATEAM_DIR_PATH.

        Для любых пользователей. Не NDA пользователь просто не увидит ресурсы внутри папки,
        следовательно для него не запрещаем явно.
        """
        self.json_ok('mkdir', {'uid': self.common_uid, 'path': YATEAM_DIR_PATH})
        self.json_ok('mkdir', {'uid': self.common_uid, 'path': YATEAM_DIR_PATH + '/test_nda_0'})
        self.json_ok('mkdir', {'uid': self.common_uid, 'path': YATEAM_DIR_PATH + '/test_nda_1'})
        self.json_ok('mkdir', {'uid': self.yateam_uid, 'path': YATEAM_DIR_PATH + '/test_nda_0'})
        self.json_ok('mkdir', {'uid': self.yateam_uid, 'path': YATEAM_DIR_PATH + '/test_nda_1'})

        self.json_ok('set_public', {'uid': self.yateam_uid, 'path': YATEAM_DIR_PATH + '/test_nda_0', 'show_nda': 0})
        self.json_ok('set_public', {'uid': self.yateam_uid, 'path': YATEAM_DIR_PATH + '/test_nda_1', 'show_nda': 1})
        self.json_ok('set_public', {'uid': self.common_uid, 'path': YATEAM_DIR_PATH + '/test_nda_0', 'show_nda': 0})
        self.json_ok('set_public', {'uid': self.common_uid, 'path': YATEAM_DIR_PATH + '/test_nda_1', 'show_nda': 1})

    def test_public_info(self):
        self.check_show_nda_for_endpoint('public_info', self.public_folder_hash, True)

    def test_public_info_with_relative_path(self):
        self.check_show_nda_for_endpoint('public_info', self.public_folder_hash + ':/nested', True)

    def test_public_url(self):
        self.check_show_nda_for_endpoint('public_url', self.public_file_hash, True)

    def test_public_list(self):
        self.check_show_nda_for_endpoint('public_list', self.public_folder_hash, True)

    def test_public_list_with_relative_path(self):
        self.check_show_nda_for_endpoint('public_list', self.public_folder_hash + ':/nested', True)

    def test_search_public_list(self):
        self.check_show_nda_for_endpoint('search_public_list', self.public_folder_hash, True)

    def test_public_fulltree(self):
        self.check_show_nda_for_endpoint('public_fulltree', self.public_folder_hash, True)

    def test_public_fulltree_with_relative_path(self):
        self.check_show_nda_for_endpoint('public_fulltree', self.public_folder_hash + ':/nested_dir', True)

    def test_public_copy(self):
        self.check_show_nda_for_endpoint('public_copy', self.public_file_hash, False)

    def test_async_public_copy(self):
        self.check_show_nda_for_endpoint('async_public_copy', self.public_file_hash, False)

    def test_public_video_url(self):
        self.check_show_nda_for_endpoint('public_video_url', self.public_video_hash, False)

    def test_public_direct_url(self):
        self.check_show_nda_for_endpoint('public_direct_url', self.public_file_hash, True,
                                         ok_method=self.service_ok, error_method=self.service_error)

    def test_extract_file_from_archive(self):
        args = {
            'src_file': '1.jpg',
            'dst': '/disk/1.jpg',
        }
        with patch_http_client_open_url():
            self.check_show_nda_for_endpoint('extract_file_from_archive', self.public_archive_hash, False, args=args)

    def check_show_nda_for_endpoint(self, endpoint, private_hash, is_uid_optional, args={}, ok_method=None, error_method=None):
        ok_method = ok_method or self.json_ok
        error_method = error_method or self.json_error

        # пока владелец не яндексоид, смотреть можно всем
        ok_method(endpoint, dict({'uid': self.common_uid, 'private_hash': private_hash}, **args))
        ok_method(endpoint, dict({'uid': self.common_uid, 'private_hash': private_hash, 'show_nda': 0}, **args))
        ok_method(endpoint, dict({'uid': self.common_uid, 'private_hash': private_hash, 'show_nda': 1}, **args))
        ok_method(endpoint, dict({'uid': self.yateam_uid, 'private_hash': private_hash}, **args))
        ok_method(endpoint, dict({'uid': self.yateam_uid, 'private_hash': private_hash, 'show_nda': 0}, **args))
        ok_method(endpoint, dict({'uid': self.yateam_uid, 'private_hash': private_hash, 'show_nda': 1}, **args))

        self._make_yateam(self.owner_uid, yateam_uid='123456')

        # владелец яндексоид, но 2fa не включена: папка не считается НДА, смотреть можно всем
        PassportStub.update_info_by_uid(self.owner_uid, is_2fa_enabled=False)
        ok_method(endpoint, dict({'uid': self.common_uid, 'private_hash': private_hash}, **args))
        ok_method(endpoint, dict({'uid': self.common_uid, 'private_hash': private_hash, 'show_nda': 0}, **args))
        ok_method(endpoint, dict({'uid': self.common_uid, 'private_hash': private_hash, 'show_nda': 1}, **args))
        ok_method(endpoint, dict({'uid': self.yateam_uid, 'private_hash': private_hash}, **args))
        ok_method(endpoint, dict({'uid': self.yateam_uid, 'private_hash': private_hash, 'show_nda': 0}, **args))
        ok_method(endpoint, dict({'uid': self.yateam_uid, 'private_hash': private_hash, 'show_nda': 1}, **args))

        PassportStub.update_info_by_uid(self.owner_uid, is_2fa_enabled=True)

        error_method(endpoint, dict({'uid': self.common_uid, 'private_hash': private_hash}, **args), code=codes.RESOURCE_NOT_FOUND)
        error_method(endpoint, dict({'uid': self.common_uid, 'private_hash': private_hash, 'show_nda': 0}, **args), code=codes.RESOURCE_NOT_FOUND)
        error_method(endpoint, dict({'uid': self.common_uid, 'private_hash': private_hash, 'show_nda': 1}, **args), code=codes.RESOURCE_NOT_FOUND)

        if is_uid_optional:
            error_method(endpoint, dict({'private_hash': private_hash}, **args), code=codes.RESOURCE_NOT_FOUND)
            error_method(endpoint, dict({'private_hash': private_hash, 'show_nda': 0}, **args), code=codes.RESOURCE_NOT_FOUND)
            error_method(endpoint, dict({'private_hash': private_hash, 'show_nda': 1}, **args), code=codes.RESOURCE_NOT_FOUND)
            ok_method(endpoint, dict({'private_hash': private_hash, 'show_nda': 'kladun'}, **args))

        PassportStub.update_info_by_uid(self.yateam_uid, is_2fa_enabled=False)
        error_method(endpoint, dict({'uid': self.yateam_uid, 'private_hash': private_hash, 'show_nda': 1}, **args))

        PassportStub.update_info_by_uid(self.yateam_uid, is_2fa_enabled=True)
        ok_method(endpoint, dict({'uid': self.yateam_uid, 'private_hash': private_hash, 'show_nda': 0}, **args))
        ok_method(endpoint, dict({'uid': self.yateam_uid, 'private_hash': private_hash, 'show_nda': 1}, **args))
        ok_method(endpoint, dict({'uid': self.yateam_uid, 'private_hash': private_hash, 'show_nda': 'kladun'}, **args))
        ok_method(endpoint, dict({'uid': self.yateam_uid, 'private_hash': private_hash, 'show_nda': 'web'}, **args))


class ShortURLFormatYaTeamTestCase(BaseYaTeamTestCase):
    """Класс тестов формата коротких урлов для ятимных ресурсов в различных читающих ручках.
    """

    NDA_SHORT_URL_REGEX = r'https://disk\.yandex\.ru/public/nda/\?hash=[\w/%]+'

    def setup_method(self, method):
        super(ShortURLFormatYaTeamTestCase, self).setup_method(method)

        self._make_yateam(self.uid)

        for i in xrange(5):
            path = YATEAM_DIR_PATH + '/pub_dir_%d' % i
            self.json_ok('mkdir', {'uid': self.uid, 'path': path})
            opts = {'uid': self.uid, 'path': path, 'show_nda': 1}
            self.json_ok('set_public', opts)

        path = YATEAM_DIR_PATH + '/test_public'
        self.upload_file(self.uid, path)
        opts['path'] = path
        self.json_ok('set_public', opts)

    def test_set_public(self):
        path = YATEAM_DIR_PATH + '/test_set_public'
        self.upload_file(self.uid, path)
        response = self.json_ok('set_public', {'uid': self.uid, 'path': path, 'show_nda': 1})
        assert_that(response['short_url'], matches_regexp(ShortURLFormatYaTeamTestCase.NDA_SHORT_URL_REGEX))
        assert_that(response['short_url_named'], matches_regexp(ShortURLFormatYaTeamTestCase.NDA_SHORT_URL_REGEX))

    def test_info(self):
        self.assert_endpoint_returns_correct_short_urls(
            'info', {'path': YATEAM_DIR_PATH + '/pub_dir_1'}, self.handle_info_get_urls_and_resource_id)

    def test_list(self):
        self.assert_endpoint_returns_correct_short_urls(
            'list', {'path': YATEAM_DIR_PATH}, self.handle_list_get_urls_and_resource_id)

    def test_fulltree(self):
        self.assert_endpoint_returns_correct_short_urls(
            'fulltree', {'path': YATEAM_DIR_PATH}, self.handle_fulltree_get_urls_and_resource_id)

    def test_get_last_files(self):
        self.assert_endpoint_returns_correct_short_urls(
            'get_last_files', {}, self.handle_get_last_files_get_urls_and_resource_id)

    def test_info_by_file_id(self):
        address = Address.Make(self.uid, YATEAM_DIR_PATH + '/pub_dir_1')
        resource = get_resource_by_address(self.uid, address)
        self.assert_endpoint_returns_correct_short_urls(
            'info_by_file_id',
            {'file_id': resource.resource_id.file_id},
            self.handle_info_get_urls_and_resource_id)

    def test_new_search(self):
        search_response = get_search_item_response(YATEAM_DIR_PATH + '/pub_dir_1', '*')
        with mock.patch.object(DiskSearch, 'open_url') as mocked_open_url:
            mocked_open_url.return_value = search_response
            self.assert_endpoint_returns_correct_short_urls(
                'new_search',
                {'path': YATEAM_DIR_PATH + '/pub_dir_1', 'query': '*'},
                self.handle_new_search_get_urls_and_resource_id)

    def test_lenta_block_list(self):
        self.assert_endpoint_returns_correct_short_urls(
            'lenta_block_list',
            {'path': YATEAM_DIR_PATH, 'mtime_gte': '100500'},
            self.handle_list_get_urls_and_resource_id)

    def assert_endpoint_returns_correct_short_urls(self, endpoint, query_args, get_urls_and_resource_id):
        base_query_args = {'uid': self.uid, 'meta': 'short_url,short_url_named,resource_id', 'show_nda': 1}
        base_query_args.update(query_args)
        response = self.json_ok(endpoint, base_query_args)

        total_public_resources = 0
        for short_url, short_url_named, resource_id in get_urls_and_resource_id(response):
            resource = get_resource_by_resource_id(self.uid, ResourceId.parse(resource_id))
            assert Publicator.get_short_url(resource) == short_url
            assert Publicator.get_short_url_named(resource) == short_url_named
            assert_that(short_url, matches_regexp(ShortURLFormatYaTeamTestCase.NDA_SHORT_URL_REGEX))
            assert_that(short_url_named, matches_regexp(ShortURLFormatYaTeamTestCase.NDA_SHORT_URL_REGEX))
            total_public_resources += 1

        assert total_public_resources

    @staticmethod
    def handle_info_get_urls_and_resource_id(response):
        meta = response['meta']
        if 'short_url' in meta:
            yield meta['short_url'], meta['short_url_named'], meta['resource_id']

    @staticmethod
    def handle_list_get_urls_and_resource_id(response):
        for item in response:
            meta = item['meta']
            if item['path'] != YATEAM_DIR_PATH and 'short_url' in meta:
                yield meta['short_url'], meta['short_url_named'], meta['resource_id']

    @staticmethod
    def handle_fulltree_get_urls_and_resource_id(response):
        items = [response]
        while items:
            item = items.pop()
            items.extend(item['list'])
            meta = item['this']['meta']
            if 'short_url' in meta:
                yield meta['short_url'], meta['short_url_named'], meta['resource_id']

    @staticmethod
    def handle_get_last_files_get_urls_and_resource_id(response):
        for item in response:
            if not is_yateam_subtree(item['path']):
                continue
            meta = item['meta']
            if 'short_url' in meta:
                yield meta['short_url'], meta['short_url_named'], meta['resource_id']

    @staticmethod
    def handle_new_search_get_urls_and_resource_id(response):
        for item in response['results']:
            meta = item['meta']
            if 'short_url' in meta:
                yield meta['short_url'], meta['short_url_named'], meta['resource_id']


YaTeamDirModificationTestCases = [
        # test_name, is_yateam, has_2fa, folder_expected
        ('common_user__without_2fa', False, False, False),
        ('common_user__with_2fa', False, True, False),
        ('yateam_user__without_2fa', True, False, False),
        ('yateam_user__with_2fa', True, True, True),
    ]


class YaTeamDirModificationTestCase(BaseYaTeamTestCase):
    """Класс тестов проверки ограничения доступа к ручкам модификаций ресурсов,
    связанного с тем, что юзер является яндексоидом."""

    def setup_method(self, method):
        super(YaTeamDirModificationTestCase, self).setup_method(method)
        self._make_yateam(self.uid)  # yateam
        self.create_user(self.uid_1)  # not yateam
        self.json_ok('mkdir', {'uid': self.uid_1, 'path': YATEAM_DIR_PATH})

    def _get_effective_uid(self, is_yateam):
        uid = self.uid
        if not is_yateam:
            uid = self.uid_1
        return uid

    @staticmethod
    def _is_yateam_dir_root_exist(uid):
        try:
            get_resource_by_address(uid, Address.Make(uid, YATEAM_DIR_PATH))
        except ResourceNotFound:
            return False
        return True

    @parameterized.expand(YaTeamDirModificationTestCases)
    def test_move(self, test_name, is_yateam, has_2fa, folder_expected):
        u"""После перемещения папки ``YATEAM_DIR_PATH`` у яндексоида она снова должна появиться.
        """
        uid = self._get_effective_uid(is_yateam)
        PassportStub.update_info_by_uid(uid, is_2fa_enabled=has_2fa)
        self.json_ok('move', {'uid': uid, 'src': YATEAM_DIR_PATH, 'dst': YATEAM_DIR_PATH + ' (1)'})
        assert folder_expected == self._is_yateam_dir_root_exist(uid)

    @parameterized.expand(YaTeamDirModificationTestCases)
    def test_async_move(self, test_name, is_yateam, has_2fa, folder_expected):
        u"""После перемещения папки ``YATEAM_DIR_PATH`` у яндексоида она снова должна появиться.
        """
        uid = self._get_effective_uid(is_yateam)
        PassportStub.update_info_by_uid(uid, is_2fa_enabled=has_2fa)
        self.async_ok('async_move', {'uid': uid, 'src': YATEAM_DIR_PATH, 'dst': YATEAM_DIR_PATH + ' (1)'})
        assert folder_expected == self._is_yateam_dir_root_exist(uid)

    @parameterized.expand(YaTeamDirModificationTestCases)
    def test_trash_append(self, test_name, is_yateam, has_2fa, folder_expected):
        u"""После удаления в корзину папки ``YATEAM_DIR_PATH`` у яндексоида она снова должна появиться.
        """
        uid = self._get_effective_uid(is_yateam)
        PassportStub.update_info_by_uid(uid, is_2fa_enabled=has_2fa)
        self.json_ok('trash_append', {'uid': uid, 'path': YATEAM_DIR_PATH})
        assert folder_expected == self._is_yateam_dir_root_exist(uid)

    @parameterized.expand(YaTeamDirModificationTestCases)
    def test_async_trash_append(self, test_name, is_yateam, has_2fa, folder_expected):
        u"""После удаления в корзину папки ``YATEAM_DIR_PATH`` у яндексоида она снова должна появиться.
        """
        uid = self._get_effective_uid(is_yateam)
        PassportStub.update_info_by_uid(uid, is_2fa_enabled=has_2fa)
        self.async_ok('async_trash_append', {'uid': uid, 'path': YATEAM_DIR_PATH})
        assert folder_expected == self._is_yateam_dir_root_exist(uid)

    @parameterized.expand(YaTeamDirModificationTestCases)
    def test_rm(self, test_name, is_yateam, has_2fa, folder_expected):
        u"""После удаления в папки ``YATEAM_DIR_PATH`` у яндексоида она снова должна появиться.
        """
        uid = self._get_effective_uid(is_yateam)
        PassportStub.update_info_by_uid(uid, is_2fa_enabled=has_2fa)
        self.json_ok('rm', {'uid': uid, 'path': YATEAM_DIR_PATH})
        assert folder_expected == self._is_yateam_dir_root_exist(uid)

    @parameterized.expand(YaTeamDirModificationTestCases)
    def test_async_rm(self, test_name, is_yateam, has_2fa, folder_expected):
        u"""После удаления в папки ``YATEAM_DIR_PATH`` у яндексоида она снова должна появиться.
        """
        uid = self._get_effective_uid(is_yateam)
        PassportStub.update_info_by_uid(uid, is_2fa_enabled=has_2fa)
        self.async_ok('async_rm', {'uid': uid, 'path': YATEAM_DIR_PATH})
        assert folder_expected == self._is_yateam_dir_root_exist(uid)

    @parameterized.expand(YaTeamDirModificationTestCases)
    def test_share_create_group(self, test_name, is_yateam, has_2fa, folder_expected):
        u"""Всем запрещено делать ``YATEAM_DIR_PATH`` общей папкой."""
        uid = self._get_effective_uid(is_yateam)
        PassportStub.update_info_by_uid(uid, is_2fa_enabled=has_2fa)
        query_args = {'uid': uid, 'path': YATEAM_DIR_PATH}
        endpoint = 'share_create_group'
        self.json_error(endpoint, query_args, code=codes.YATEAM_DIR_MODIFY_ERROR)
        query_args['show_nda'] = 1
        self.json_error(endpoint, query_args, code=codes.YATEAM_DIR_MODIFY_ERROR)

    @parameterized.expand(YaTeamDirModificationTestCases)
    def test_share_folder_creation_with_first_invite(self, test_name, is_yateam, has_2fa, folder_expected):
        u"""Всем запрещено делать ``YATEAM_DIR_PATH`` общей папкой."""
        uid = self._get_effective_uid(is_yateam)
        PassportStub.update_info_by_uid(uid, is_2fa_enabled=has_2fa)
        query_args = {
            'uid': uid,
            'path': YATEAM_DIR_PATH,
            'universe_login': self.email_3,
            'universe_service': 'email',
            'rights': 660,
        }

        endpoint = 'share_invite_user'
        self.json_error(endpoint, query_args, code=codes.YATEAM_DIR_MODIFY_ERROR)
        query_args['show_nda'] = 1
        self.json_error(endpoint, query_args, code=codes.YATEAM_DIR_MODIFY_ERROR)


class MoveYateamFolderToChiefTestCase(BaseYaTeamTestCase):
    YATEAM_UID_1 = '1234'
    YATEAM_UID_2 = '123456'

    def setup_method(self, method):
        super(MoveYateamFolderToChiefTestCase, self).setup_method(method)
        PassportStub.update_info_by_uid(self.uid, is_2fa_enabled=True)
        PassportStub.update_info_by_uid(self.uid_3, is_2fa_enabled=True)

    def _staff_callback(self, yateam_uid):
        self.json_ok('staff_user_changed_callback', {'yateam_uid': yateam_uid})

    def _fill_nda_folder(self, uid, block_files=False):
        self.json_ok('mkdir', {'uid': uid, 'path': YATEAM_DIR_PATH + '/subfolder'})
        self.upload_file(uid, YATEAM_DIR_PATH + '/1.jpg')
        self.upload_file(uid, YATEAM_DIR_PATH + '/subfolder/1.jpg')

        if block_files:
            hid_1 = self.json_ok('info', {'uid': uid, 'path':  YATEAM_DIR_PATH + '/1.jpg', 'meta': 'hid'})['meta']['hid']
            hid_2 = self.json_ok('info', {'uid': uid, 'path': YATEAM_DIR_PATH + '/subfolder/1.jpg', 'meta': 'hid'})['meta']['hid']
            BlockingsCollection().insert({'_id': hid_1, 'data': 'i_dont_know_what_to_put_here'})
            BlockingsCollection().insert({'_id': hid_2, 'data': 'i_dont_know_what_to_put_here'})

    def _assert_nda_with_filling_removed(self, uid):
        hidden_data_contents = list(db.hidden_data.find({'uid': uid}))
        paths_with_possible_timestamp = {x['key'] for x in hidden_data_contents}
        paths = {x[:x.find(':')] if ':' in x else x for x in paths_with_possible_timestamp}
        hidden_yateam_root = YATEAM_DIR_PATH.replace('disk', 'hidden')
        assert {hidden_yateam_root, hidden_yateam_root + '/1.jpg', hidden_yateam_root + '/subfolder', hidden_yateam_root + '/subfolder/1.jpg'} <= paths

    def _assert_folder_filling(self, uid, root_path):
        root_folder_contents = self.json_ok('list', {'uid': uid, 'path': root_path, 'meta': ''})
        root_folder_item_paths = {x['path'] for x in root_folder_contents}
        assert root_folder_item_paths == {root_path, root_path + '/1.jpg', root_path + '/subfolder'}

        subfolder_path = root_path + '/subfolder'
        subfolder_contents = self.json_ok('list', {'uid': uid, 'path': subfolder_path, 'meta': ''})
        subfolder_item_paths = {x['path'] for x in subfolder_contents}
        assert subfolder_item_paths == {subfolder_path, subfolder_path + '/1.jpg'}

    def _assert_nda_folder_empty(self, uid):
        contents = self.json_ok('list', {'uid': uid, 'path': YATEAM_DIR_PATH, 'show_nda': 1, 'meta': ''})
        assert {x['path'] for x in contents} == {YATEAM_DIR_PATH}

    def test__user_dismissed__no_chief__nda_folder_removed(self):
        with StaffServiceSmartMockHelper.mock_get_user_info(), \
                mock.patch('mpfs.core.job_handlers.yateam.YATEAM_REMOVE_FOLDER_COPY_TO_CHIEF', True):
            StaffServiceSmartMockHelper.add_user_info(self.YATEAM_UID_1, uid=self.uid)
            self._staff_callback(self.YATEAM_UID_1)
            self._assert_nda_folder(self.uid)
            self._fill_nda_folder(self.uid)

            StaffServiceSmartMockHelper.add_user_info(self.YATEAM_UID_1, uid=self.uid, is_dismissed=True)
            self._staff_callback(self.YATEAM_UID_1)
            self._assert_no_nda_folder(self.uid)
            self._assert_nda_with_filling_removed(self.uid)

    def test__user_dismissed__chief_has_no_uid__nda_folder_removed(self):
        with StaffServiceSmartMockHelper.mock_get_user_info(), \
                mock.patch('mpfs.core.job_handlers.yateam.YATEAM_REMOVE_FOLDER_COPY_TO_CHIEF', True):
            StaffServiceSmartMockHelper.add_user_info(self.YATEAM_UID_1, uid=self.uid, chief_yateam_uid=self.YATEAM_UID_2)
            StaffServiceSmartMockHelper.add_user_info(self.YATEAM_UID_2)
            self._staff_callback(self.YATEAM_UID_1)
            self._assert_nda_folder(self.uid)
            self._fill_nda_folder(self.uid)

            StaffServiceSmartMockHelper.add_user_info(self.YATEAM_UID_1, uid=self.uid, chief_yateam_uid=self.YATEAM_UID_2, is_dismissed=True)
            self._staff_callback(self.YATEAM_UID_1)
            self._assert_no_nda_folder(self.uid)
            self._assert_nda_with_filling_removed(self.uid)

    def test__user_dismissed__chief_uid_not_initialized__nda_folder_removed(self):
        with StaffServiceSmartMockHelper.mock_get_user_info(), \
                mock.patch('mpfs.core.job_handlers.yateam.YATEAM_REMOVE_FOLDER_COPY_TO_CHIEF', True):
            StaffServiceSmartMockHelper.add_user_info(self.YATEAM_UID_1, uid=self.uid, chief_yateam_uid=self.YATEAM_UID_2)
            StaffServiceSmartMockHelper.add_user_info(self.YATEAM_UID_2, uid='uninitialized')
            self._staff_callback(self.YATEAM_UID_1)
            self._assert_nda_folder(self.uid)
            self._fill_nda_folder(self.uid)

            StaffServiceSmartMockHelper.add_user_info(self.YATEAM_UID_1, uid=self.uid, chief_yateam_uid=self.YATEAM_UID_2, is_dismissed=True)
            self._staff_callback(self.YATEAM_UID_1)
            self._assert_no_nda_folder(self.uid)
            self._assert_nda_with_filling_removed(self.uid)

    def test__user_dismissed__chief_ok__nda_folder_moved(self):
        self.json_ok('user_init', {'uid': self.uid_3})
        with StaffServiceSmartMockHelper.mock_get_user_info(), \
                mock.patch('mpfs.core.job_handlers.yateam.YATEAM_REMOVE_FOLDER_COPY_TO_CHIEF', True):
            StaffServiceSmartMockHelper.add_user_info(self.YATEAM_UID_1, uid=self.uid, chief_yateam_uid=self.YATEAM_UID_2, yateam_login='emperor')
            StaffServiceSmartMockHelper.add_user_info(self.YATEAM_UID_2, uid=self.uid_3)
            self._staff_callback(self.YATEAM_UID_1)
            self._staff_callback(self.YATEAM_UID_2)
            self._assert_nda_folder(self.uid)
            self._assert_nda_folder(self.uid_3)
            self._fill_nda_folder(self.uid)

            StaffServiceSmartMockHelper.add_user_info(self.YATEAM_UID_1, uid=self.uid, chief_yateam_uid=self.YATEAM_UID_2, yateam_login='emperor', is_dismissed=True)
            self._staff_callback(self.YATEAM_UID_1)
            self._assert_no_nda_folder(self.uid)
            self._assert_nda_with_filling_removed(self.uid)
            self._assert_folder_filling(self.uid_3, YATEAM_DIR_PATH + '/emperor')

    def test__user_dismissed__chief_dismissed__nda_folder_removed(self):
        self.json_ok('user_init', {'uid': self.uid_3})
        with StaffServiceSmartMockHelper.mock_get_user_info(), \
                mock.patch('mpfs.core.job_handlers.yateam.YATEAM_REMOVE_FOLDER_COPY_TO_CHIEF', True):
            StaffServiceSmartMockHelper.add_user_info(self.YATEAM_UID_1, uid=self.uid, chief_yateam_uid=self.YATEAM_UID_2, yateam_login='emperor')
            StaffServiceSmartMockHelper.add_user_info(self.YATEAM_UID_2, uid=self.uid_3, is_dismissed=True)
            self._staff_callback(self.YATEAM_UID_1)
            self._staff_callback(self.YATEAM_UID_2)
            self._assert_nda_folder(self.uid)
            self._assert_no_nda_folder(self.uid_3)
            self._fill_nda_folder(self.uid)

            StaffServiceSmartMockHelper.add_user_info(self.YATEAM_UID_1, uid=self.uid, chief_yateam_uid=self.YATEAM_UID_2, yateam_login='emperor', is_dismissed=True)
            self._staff_callback(self.YATEAM_UID_1)
            self._assert_no_nda_folder(self.uid)
            self._assert_nda_with_filling_removed(self.uid)
            self._assert_no_nda_folder(self.uid_3)

    def test__user_dismissed__chief_with_same_folder__nda_folder_moved_with_autosuffix(self):
        self.json_ok('user_init', {'uid': self.uid_3})
        with StaffServiceSmartMockHelper.mock_get_user_info(), \
                mock.patch('mpfs.core.job_handlers.yateam.YATEAM_REMOVE_FOLDER_COPY_TO_CHIEF', True):
            StaffServiceSmartMockHelper.add_user_info(self.YATEAM_UID_1, uid=self.uid, chief_yateam_uid=self.YATEAM_UID_2, yateam_login='emperor')
            StaffServiceSmartMockHelper.add_user_info(self.YATEAM_UID_2, uid=self.uid_3)
            self._staff_callback(self.YATEAM_UID_1)
            self._staff_callback(self.YATEAM_UID_2)
            self._assert_nda_folder(self.uid)
            self._assert_nda_folder(self.uid_3)
            self._fill_nda_folder(self.uid)
            self.json_ok('mkdir', {'uid': self.uid_3, 'path': YATEAM_DIR_PATH + '/emperor'})

            StaffServiceSmartMockHelper.add_user_info(self.YATEAM_UID_1, uid=self.uid, chief_yateam_uid=self.YATEAM_UID_2, yateam_login='emperor', is_dismissed=True)
            self._staff_callback(self.YATEAM_UID_1)
            self._assert_no_nda_folder(self.uid)
            self._assert_nda_with_filling_removed(self.uid)
            self._assert_folder_filling(self.uid_3, YATEAM_DIR_PATH + '/emperor (1)')

    def test__user_dismissed__staff_has_no_chief__nda_folder_removed(self):
        with StaffServiceSmartMockHelper.mock_get_user_info(), \
                mock.patch('mpfs.core.job_handlers.yateam.YATEAM_REMOVE_FOLDER_COPY_TO_CHIEF', True):
            StaffServiceSmartMockHelper.add_user_info(self.YATEAM_UID_1, uid=self.uid, chief_yateam_uid=self.YATEAM_UID_2)
            self._staff_callback(self.YATEAM_UID_1)
            self._assert_nda_folder(self.uid)
            self._fill_nda_folder(self.uid)

            StaffServiceSmartMockHelper.add_user_info(self.YATEAM_UID_1, uid=self.uid, chief_yateam_uid=self.YATEAM_UID_2, is_dismissed=True)
            self._staff_callback(self.YATEAM_UID_1)
            self._assert_no_nda_folder(self.uid)
            self._assert_nda_with_filling_removed(self.uid)

    def test__user_dismissed__chief_ok__copy_disabled__nda_folder_removed(self):
        self.json_ok('user_init', {'uid': self.uid_3})
        with StaffServiceSmartMockHelper.mock_get_user_info(), \
                mock.patch('mpfs.core.job_handlers.yateam.YATEAM_REMOVE_FOLDER_COPY_TO_CHIEF', False):
            StaffServiceSmartMockHelper.add_user_info(self.YATEAM_UID_1, uid=self.uid, chief_yateam_uid=self.YATEAM_UID_2, yateam_login='emperor')
            StaffServiceSmartMockHelper.add_user_info(self.YATEAM_UID_2, uid=self.uid_3)
            self._staff_callback(self.YATEAM_UID_1)
            self._staff_callback(self.YATEAM_UID_2)
            self._assert_nda_folder(self.uid)
            self._assert_nda_folder(self.uid_3)
            self._fill_nda_folder(self.uid)

            StaffServiceSmartMockHelper.add_user_info(self.YATEAM_UID_1, uid=self.uid, chief_yateam_uid=self.YATEAM_UID_2, yateam_login='emperor', is_dismissed=True)
            self._staff_callback(self.YATEAM_UID_1)
            self._assert_no_nda_folder(self.uid)
            self._assert_nda_with_filling_removed(self.uid)
            self._assert_nda_folder_empty(self.uid_3)

    def test__user_dismissed__has_blocked_file__nda_folder_is_removed(self):
        with StaffServiceSmartMockHelper.mock_get_user_info(), \
                mock.patch('mpfs.core.job_handlers.yateam.YATEAM_REMOVE_FOLDER_COPY_TO_CHIEF', False):
            StaffServiceSmartMockHelper.add_user_info(self.YATEAM_UID_1, uid=self.uid)
            self._staff_callback(self.YATEAM_UID_1)
            self._fill_nda_folder(self.uid, block_files=True)

            StaffServiceSmartMockHelper.add_user_info(self.YATEAM_UID_1, uid=self.uid, chief_yateam_uid=self.YATEAM_UID_2, yateam_login='emperor', is_dismissed=True)
            self._staff_callback(self.YATEAM_UID_1)
            self._assert_no_nda_folder(self.uid)
            self._assert_nda_with_filling_removed(self.uid)

    def test__user_disabeld_2fa__chief_ok__nda_folder_moved(self):
        self.json_ok('user_init', {'uid': self.uid_3})
        with StaffServiceSmartMockHelper.mock_get_user_info(), \
                mock.patch('mpfs.core.job_handlers.yateam.YATEAM_REMOVE_FOLDER_COPY_TO_CHIEF', True):
            StaffServiceSmartMockHelper.add_user_info(self.YATEAM_UID_1, uid=self.uid, chief_yateam_uid=self.YATEAM_UID_2, yateam_login='emperor')
            StaffServiceSmartMockHelper.add_user_info(self.YATEAM_UID_2, uid=self.uid_3)
            self._staff_callback(self.YATEAM_UID_1)
            self._staff_callback(self.YATEAM_UID_2)
            self._assert_nda_folder(self.uid)
            self._assert_nda_folder(self.uid_3)
            self._fill_nda_folder(self.uid)

            PassportStub.update_info_by_uid(self.uid, is_2fa_enabled=False)
            self._staff_callback(self.YATEAM_UID_1)
            self._assert_no_nda_folder(self.uid)
            self._assert_nda_with_filling_removed(self.uid)
            self._assert_folder_filling(self.uid_3, YATEAM_DIR_PATH + '/emperor')

    def test__user_disabeld_2fa__chief_ok__copy_disabled__nda_folder_removed(self):
        self.json_ok('user_init', {'uid': self.uid_3})
        with StaffServiceSmartMockHelper.mock_get_user_info(), \
                mock.patch('mpfs.core.job_handlers.yateam.YATEAM_REMOVE_FOLDER_COPY_TO_CHIEF', False):
            StaffServiceSmartMockHelper.add_user_info(self.YATEAM_UID_1, uid=self.uid, chief_yateam_uid=self.YATEAM_UID_2, yateam_login='emperor')
            StaffServiceSmartMockHelper.add_user_info(self.YATEAM_UID_2, uid=self.uid_3)
            self._staff_callback(self.YATEAM_UID_1)
            self._staff_callback(self.YATEAM_UID_2)
            self._assert_nda_folder(self.uid)
            self._assert_nda_folder(self.uid_3)
            self._fill_nda_folder(self.uid)

            PassportStub.update_info_by_uid(self.uid, is_2fa_enabled=False)
            self._staff_callback(self.YATEAM_UID_1)
            self._assert_no_nda_folder(self.uid)
            self._assert_nda_with_filling_removed(self.uid)
            self._assert_nda_folder_empty(self.uid_3)


class YateamChangelogTestCase(BaseYaTeamTestCase):
    def test_remove_create_nda_folder_for_yateam_user_returns_404(self):
        self._make_yateam(self.uid)
        version = mpfs.engine.process.usrctl().version(self.uid)
        self.json_ok('rm', {'uid': self.uid, 'path': YATEAM_DIR_PATH})
        self.json_error('deltas', {'uid': self.uid, 'path': '/disk', 'base_revision': version}, code=codes.VERSION_NOT_FOUND)

    def test_remove_create_nda_folder_for_yateam_user_without_2fa_returns_deltas(self):
        self._make_yateam(self.uid)
        PassportStub.update_info_by_uid(self.uid, is_2fa_enabled=False)
        version = mpfs.engine.process.usrctl().version(self.uid)
        self.json_ok('rm', {'uid': self.uid, 'path': YATEAM_DIR_PATH})
        self.json_ok('mkdir', {'uid': self.uid, 'path': YATEAM_DIR_PATH})
        self.json_ok('deltas', {'uid': self.uid, 'path': '/disk', 'base_revision': version})

    def test_remove_create_nda_folder_for_usual_user_returns_deltas(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': YATEAM_DIR_PATH})
        version = mpfs.engine.process.usrctl().version(self.uid)
        self.json_ok('rm', {'uid': self.uid, 'path': YATEAM_DIR_PATH})
        self.json_ok('mkdir', {'uid': self.uid, 'path': YATEAM_DIR_PATH})
        self.json_ok('deltas', {'uid': self.uid, 'path': '/disk', 'base_revision': version})


class ResyncYateamUsersTestCase(BaseYaTeamTestCase):
    def test_new_yateam_user_with_2fa(self):
        PassportStub.update_info_by_uid(self.uid, is_2fa_enabled=True)
        with StaffServiceSmartMockHelper.mock_get_user_info(), \
                mock.patch('mpfs.core.yateam.logic.YATEAM_ENABLE_FOR_ALL', True):
            StaffServiceSmartMockHelper.add_user_info(self.YATEAM_UID_1, uid=self.uid)
            resync_yateam_users()
        self._assert_nda_folder(self.uid)
        self._assert_user_has_yateam_uid(self.uid, self.YATEAM_UID_1)
        self._assert_user_has_yateam_service(self.uid)

    def test_new_yateam_user_without_2fa(self):
        PassportStub.update_info_by_uid(self.uid, is_2fa_enabled=False)
        with StaffServiceSmartMockHelper.mock_get_user_info(), \
                mock.patch('mpfs.core.yateam.logic.YATEAM_ENABLE_FOR_ALL', True):
            StaffServiceSmartMockHelper.add_user_info(self.YATEAM_UID_1, uid=self.uid)
            resync_yateam_users()
        self._assert_no_nda_folder(self.uid)
        self._assert_user_has_yateam_uid(self.uid, self.YATEAM_UID_1)
        self._assert_user_has_yateam_service(self.uid)

    def test_yateam_user_removed_login(self):
        self._make_yateam(self.uid, yateam_uid=self.YATEAM_UID_1)
        with StaffServiceSmartMockHelper.mock_get_user_info(), \
                mock.patch('mpfs.core.yateam.logic.YATEAM_ENABLE_FOR_ALL', True):
            StaffServiceSmartMockHelper.add_user_info(self.YATEAM_UID_1)
            resync_yateam_users()
        self._assert_no_nda_folder(self.uid)
        self._assert_user_has_yateam_uid(self.uid, None)
        self._assert_user_does_not_have_yateam_service(self.uid)

    def test_yateam_user_changed_login_with_2fa(self):
        self.json_ok('user_init', {'uid': self.uid_1})
        self._make_yateam(self.uid_1, yateam_uid=self.YATEAM_UID_1)
        PassportStub.update_info_by_uid(self.uid, is_2fa_enabled=True)
        with StaffServiceSmartMockHelper.mock_get_user_info(), \
                mock.patch('mpfs.core.yateam.logic.YATEAM_ENABLE_FOR_ALL', True):
            StaffServiceSmartMockHelper.add_user_info(self.YATEAM_UID_1, uid=self.uid)
            resync_yateam_users()
        self._assert_no_nda_folder(self.uid_1)
        self._assert_user_has_yateam_uid(self.uid_1, None)
        self._assert_user_does_not_have_yateam_service(self.uid_1)
        self._assert_nda_folder(self.uid)
        self._assert_user_has_yateam_uid(self.uid, self.YATEAM_UID_1)
        self._assert_user_has_yateam_service(self.uid)

    def test_yateam_user_changed_login_without_2fa(self):
        self.json_ok('user_init', {'uid': self.uid_1})
        self._make_yateam(self.uid_1, yateam_uid=self.YATEAM_UID_1)
        PassportStub.update_info_by_uid(self.uid, is_2fa_enabled=False)
        with StaffServiceSmartMockHelper.mock_get_user_info(), \
                mock.patch('mpfs.core.yateam.logic.YATEAM_ENABLE_FOR_ALL', True):
            StaffServiceSmartMockHelper.add_user_info(self.YATEAM_UID_1, uid=self.uid)
            resync_yateam_users()
        self._assert_no_nda_folder(self.uid_1)
        self._assert_user_has_yateam_uid(self.uid_1, None)
        self._assert_user_does_not_have_yateam_service(self.uid_1)
        self._assert_no_nda_folder(self.uid)
        self._assert_user_has_yateam_uid(self.uid, self.YATEAM_UID_1)
        self._assert_user_has_yateam_service(self.uid)

    def test_yateam_user_enabled_2fa(self):
        with StaffServiceSmartMockHelper.mock_get_user_info(), \
                mock.patch('mpfs.core.yateam.logic.YATEAM_ENABLE_FOR_ALL', True):
            StaffServiceSmartMockHelper.add_user_info(self.YATEAM_UID_1, uid=self.uid)
            PassportStub.update_info_by_uid(self.uid, is_2fa_enabled=False)
            self.json_ok('staff_user_changed_callback', {'yateam_uid': self.YATEAM_UID_1})
            self._assert_no_nda_folder(self.uid)
            self._assert_user_has_yateam_uid(self.uid, self.YATEAM_UID_1)
            self._assert_user_has_yateam_service(self.uid)

            PassportStub.update_info_by_uid(self.uid, is_2fa_enabled=True)
            resync_yateam_users()
        self._assert_nda_folder(self.uid)
        self._assert_user_has_yateam_uid(self.uid, self.YATEAM_UID_1)
        self._assert_user_has_yateam_service(self.uid)

    def test_yateam_user_disabled_2fa(self):
        with StaffServiceSmartMockHelper.mock_get_user_info(), \
                mock.patch('mpfs.core.yateam.logic.YATEAM_ENABLE_FOR_ALL', True):
            StaffServiceSmartMockHelper.add_user_info(self.YATEAM_UID_1, uid=self.uid)
            PassportStub.update_info_by_uid(self.uid, is_2fa_enabled=True)
            self.json_ok('staff_user_changed_callback', {'yateam_uid': self.YATEAM_UID_1})
            self._assert_nda_folder(self.uid)
            self._assert_user_has_yateam_uid(self.uid, self.YATEAM_UID_1)
            self._assert_user_has_yateam_service(self.uid)

            PassportStub.update_info_by_uid(self.uid, is_2fa_enabled=False)
            resync_yateam_users()
        self._assert_no_nda_folder(self.uid)
        self._assert_user_has_yateam_uid(self.uid, self.YATEAM_UID_1)
        self._assert_user_has_yateam_service(self.uid)

    def test_yateam_user_dismissed(self):
        self._make_yateam(self.uid, yateam_uid=self.YATEAM_UID_1)
        with StaffServiceSmartMockHelper.mock_get_user_info(), \
                mock.patch('mpfs.core.yateam.logic.YATEAM_ENABLE_FOR_ALL', True):
            StaffServiceSmartMockHelper.add_user_info(self.YATEAM_UID_1, uid=self.uid, is_dismissed=True)
            resync_yateam_users()
        self._assert_no_nda_folder(self.uid)
        self._assert_user_has_yateam_uid(self.uid, None)
        self._assert_user_has_yateam_service(self.uid)

    def test_invalid_yateam_uid(self):
        self._make_yateam(self.uid, yateam_uid=self.YATEAM_UID_1)
        with StaffServiceSmartMockHelper.mock_get_user_info(), \
                mock.patch('mpfs.core.yateam.logic.YATEAM_ENABLE_FOR_ALL', True):
            resync_yateam_users()
        self._assert_no_nda_folder(self.uid)
        self._assert_user_has_yateam_uid(self.uid, None)
        self._assert_user_does_not_have_yateam_service(self.uid)

    def test_invalid_yateam_uid_sync(self):
        self._make_yateam(self.uid, yateam_uid=self.YATEAM_UID_1)
        with StaffServiceSmartMockHelper.mock_get_user_info(), \
             mock.patch('mpfs.core.yateam.logic.YATEAM_ENABLE_FOR_ALL', True):
            resync_user(self.uid)
        self._assert_no_nda_folder(self.uid)
        self._assert_user_has_yateam_uid(self.uid, None)
        self._assert_user_does_not_have_yateam_service(self.uid)


class PublicCopyTestCase(BaseYaTeamTestCase, OfficeTestCase):
    def setup_method(self, method):
        super(PublicCopyTestCase, self).setup_method(method)

        self.json_ok('mkdir', {'uid': self.uid, 'path': YATEAM_DIR_PATH})

        self.upload_file(self.uid, YATEAM_DIR_PATH + '/public_file')
        self.public_hash = self.json_ok('set_public', {'uid': self.uid, 'path': YATEAM_DIR_PATH + '/public_file'})['hash']
        self.public_name = 'public_file'

        self.upload_file(self.uid, YATEAM_DIR_PATH + '/office.docx')
        self.public_office_hash = self.json_ok('set_public', {'uid': self.uid, 'path': YATEAM_DIR_PATH + '/office.docx'})['hash']
        self.public_office_name = 'office.docx'

        self.json_ok('user_init', {'uid': self.uid_1})
        self.json_ok('mkdir', {'uid': self.uid_1, 'path': YATEAM_DIR_PATH})
        self.json_ok('mksysdir', {'uid': self.uid_1, 'type': 'downloads'})

    @parameterized.expand(['public_copy', 'async_public_copy'])
    def test_public_copy_from_nda_folder_with_yateam_uid_saves_to_nda(self, method):
        self._make_yateam(self.uid, yateam_uid=self.YATEAM_UID_1)
        self._make_yateam(self.uid_1, yateam_uid=self.YATEAM_UID_2)

        self.json_ok(method, {'uid': self.uid_1, 'private_hash': self.public_hash, 'show_nda': 1})

        yateam_contents = self.json_ok('list', {'uid': self.uid_1, 'path': YATEAM_DIR_PATH, 'meta': ''})
        download_contents = self.json_ok('list', {'uid': self.uid_1, 'path': u'/disk/Загрузки', 'meta': ''})
        assert self.public_name in {x['name'] for x in yateam_contents}
        assert self.public_name not in {x['name'] for x in download_contents}

    @parameterized.expand(['public_copy', 'async_public_copy'])
    def test_public_copy_from_nda_folder_without_yateam_uid_saves_to_downloads(self, method):
        self.json_ok(method, {'uid': self.uid_1, 'private_hash': self.public_hash})

        yateam_contents = self.json_ok('list', {'uid': self.uid_1, 'path': YATEAM_DIR_PATH, 'meta': ''})
        download_contents = self.json_ok('list', {'uid': self.uid_1, 'path': u'/disk/Загрузки', 'meta': ''})
        assert self.public_name not in {x['name'] for x in yateam_contents}
        assert self.public_name in {x['name'] for x in download_contents}

    @parameterized.expand(['public_copy', 'async_public_copy'])
    def test_public_copy_from_nda_folder_without_2fa_saves_to_downloads(self, method):
        self._make_yateam(self.uid, yateam_uid=self.YATEAM_UID_1)
        self._make_yateam(self.uid_1, yateam_uid=self.YATEAM_UID_2)
        PassportStub.update_info_by_uid(self.uid, is_2fa_enabled=False)
        PassportStub.update_info_by_uid(self.uid_1, is_2fa_enabled=False)

        self.json_ok(method, {'uid': self.uid_1, 'private_hash': self.public_hash})

        yateam_contents = self.json_ok('list', {'uid': self.uid_1, 'path': YATEAM_DIR_PATH, 'meta': ''})
        download_contents = self.json_ok('list', {'uid': self.uid_1, 'path': u'/disk/Загрузки', 'meta': ''})
        assert self.public_name not in {x['name'] for x in yateam_contents}
        assert self.public_name in {x['name'] for x in download_contents}

    @parameterized.expand(['public_copy', 'async_public_copy'])
    def test_public_copy_from_nda_folder_with_yateam_uid_with_save_path_not_in_nda_throws_error(self, method):
        self._make_yateam(self.uid, yateam_uid=self.YATEAM_UID_1)
        self._make_yateam(self.uid_1, yateam_uid=self.YATEAM_UID_2)

        self.json_ok('mkdir', {'uid': self.uid_1, 'path': '/disk/folder'})
        arguments = {'uid': self.uid_1, 'private_hash': self.public_hash, 'show_nda': 1, 'save_path': '/disk/folder'}
        self.json_error(method, arguments, code=codes.CODE_ERROR, title=errors.BadArguments.__name__)

        yateam_contents = self.json_ok('list', {'uid': self.uid_1, 'path': YATEAM_DIR_PATH, 'meta': ''})
        download_contents = self.json_ok('list', {'uid': self.uid_1, 'path': u'/disk/Загрузки', 'meta': ''})
        folder_contents = self.json_ok('list', {'uid': self.uid_1, 'path': '/disk/folder', 'meta': ''})
        assert self.public_name not in {x['name'] for x in yateam_contents}
        assert self.public_name not in {x['name'] for x in download_contents}
        assert self.public_name not in {x['name'] for x in folder_contents}

    @parameterized.expand(['public_copy', 'async_public_copy'])
    def test_public_copy_from_nda_folder_with_yateam_uid_with_save_path_in_nda_saves_to_path(self, method):
        self._make_yateam(self.uid, yateam_uid=self.YATEAM_UID_1)
        self._make_yateam(self.uid_1, yateam_uid=self.YATEAM_UID_2)

        self.json_ok('mkdir', {'uid': self.uid_1, 'path': YATEAM_DIR_PATH + '/folder'})
        arguments = {'uid': self.uid_1, 'private_hash': self.public_hash, 'show_nda': 1, 'save_path': YATEAM_DIR_PATH + '/folder'}
        self.json_ok(method, arguments)

        yateam_contents = self.json_ok('list', {'uid': self.uid_1, 'path': YATEAM_DIR_PATH, 'meta': ''})
        download_contents = self.json_ok('list', {'uid': self.uid_1, 'path': u'/disk/Загрузки', 'meta': ''})
        folder_contents = self.json_ok('list', {'uid': self.uid_1, 'path': YATEAM_DIR_PATH + '/folder', 'meta': ''})
        assert self.public_name not in {x['name'] for x in yateam_contents}
        assert self.public_name not in {x['name'] for x in download_contents}
        assert self.public_name in {x['name'] for x in folder_contents}

    @parameterized.expand(['public_copy', 'async_public_copy'])
    def test_public_copy_from_nda_folder_without_yateam_uid_with_save_path_not_in_nda_saves_to_path(self, method):
        self.json_ok('mkdir', {'uid': self.uid_1, 'path': '/disk/folder'})
        self.json_ok(method, {'uid': self.uid_1, 'private_hash': self.public_hash, 'show_nda': 1, 'save_path': '/disk/folder'})

        yateam_contents = self.json_ok('list', {'uid': self.uid_1, 'path': YATEAM_DIR_PATH, 'meta': ''})
        download_contents = self.json_ok('list', {'uid': self.uid_1, 'path': u'/disk/Загрузки', 'meta': ''})
        folder_contents = self.json_ok('list', {'uid': self.uid_1, 'path': '/disk/folder', 'meta': ''})
        assert self.public_name not in {x['name'] for x in yateam_contents}
        assert self.public_name not in {x['name'] for x in download_contents}
        assert self.public_name in {x['name'] for x in folder_contents}

    @parameterized.expand(['public_copy', 'async_public_copy'])
    def test_public_copy_from_nda_folder_without_2fa_with_save_path_not_in_nda_saves_to_path(self, method):
        self._make_yateam(self.uid, yateam_uid=self.YATEAM_UID_1)
        self._make_yateam(self.uid_1, yateam_uid=self.YATEAM_UID_2)
        PassportStub.update_info_by_uid(self.uid, is_2fa_enabled=False)
        PassportStub.update_info_by_uid(self.uid_1, is_2fa_enabled=False)

        self.json_ok('mkdir', {'uid': self.uid_1, 'path': '/disk/folder'})
        self.json_ok(method, {'uid': self.uid_1, 'private_hash': self.public_hash, 'show_nda': 1, 'save_path': '/disk/folder'})

        yateam_contents = self.json_ok('list', {'uid': self.uid_1, 'path': YATEAM_DIR_PATH, 'meta': ''})
        download_contents = self.json_ok('list', {'uid': self.uid_1, 'path': u'/disk/Загрузки', 'meta': ''})
        folder_contents = self.json_ok('list', {'uid': self.uid_1, 'path': '/disk/folder', 'meta': ''})
        assert self.public_name not in {x['name'] for x in yateam_contents}
        assert self.public_name not in {x['name'] for x in download_contents}
        assert self.public_name in {x['name'] for x in folder_contents}

    def test_office_action_data_from_nda_folder_with_yateam_uid_saves_to_nda(self):
        self._make_yateam(self.uid, yateam_uid=self.YATEAM_UID_1)
        self._make_yateam(self.uid_1, yateam_uid=self.YATEAM_UID_2)

        arguments = {'uid': self.uid_1, 'action': 'edit', 'service_id': 'public', 'service_file_id': self.public_office_hash, 'show_nda': 1}
        self.json_ok('office_action_data', arguments)

        yateam_contents = self.json_ok('list', {'uid': self.uid_1, 'path': YATEAM_DIR_PATH, 'meta': ''})
        download_contents = self.json_ok('list', {'uid': self.uid_1, 'path': u'/disk/Загрузки', 'meta': ''})
        assert self.public_office_name in {x['name'] for x in yateam_contents}
        assert self.public_office_name not in {x['name'] for x in download_contents}

    def test_office_action_data_from_nda_folder_without_yateam_uid_saves_to_downloads(self):
        arguments = {'uid': self.uid_1, 'action': 'edit', 'service_id': 'public', 'service_file_id': self.public_office_hash}
        self.json_ok('office_action_data', arguments)

        yateam_contents = self.json_ok('list', {'uid': self.uid_1, 'path': YATEAM_DIR_PATH, 'meta': ''})
        download_contents = self.json_ok('list', {'uid': self.uid_1, 'path': u'/disk/Загрузки', 'meta': ''})
        assert self.public_office_name not in {x['name'] for x in yateam_contents}
        assert self.public_office_name in {x['name'] for x in download_contents}


class PassportUser2faChangedTestCase(BaseYaTeamTestCase):
    def test_yateam_user_enabled_2fa(self):
        with StaffServiceSmartMockHelper.mock_get_user_info(), \
                mock.patch('mpfs.core.yateam.logic.YATEAM_ENABLE_FOR_ALL', True):
            StaffServiceSmartMockHelper.add_user_info(self.YATEAM_UID_1, uid=self.uid)
            PassportStub.update_info_by_uid(self.uid, is_2fa_enabled=False)
            self.json_ok('staff_user_changed_callback', {'yateam_uid': self.YATEAM_UID_1})
            self._assert_no_nda_folder(self.uid)
            self._assert_user_has_yateam_uid(self.uid, self.YATEAM_UID_1)
            self._assert_user_has_yateam_service(self.uid)

            PassportStub.update_info_by_uid(self.uid, is_2fa_enabled=True)
            self.json_ok('passport_user_2fa_changed', json={'uid': self.uid})
        self._assert_nda_folder(self.uid)
        self._assert_user_has_yateam_uid(self.uid, self.YATEAM_UID_1)
        self._assert_user_has_yateam_service(self.uid)

    def test_yateam_user_disabled_2fa(self):
        with StaffServiceSmartMockHelper.mock_get_user_info(), \
                mock.patch('mpfs.core.yateam.logic.YATEAM_ENABLE_FOR_ALL', True):
            StaffServiceSmartMockHelper.add_user_info(self.YATEAM_UID_1, uid=self.uid)
            PassportStub.update_info_by_uid(self.uid, is_2fa_enabled=True)
            self.json_ok('staff_user_changed_callback', {'yateam_uid': self.YATEAM_UID_1})
            self._assert_nda_folder(self.uid)
            self._assert_user_has_yateam_uid(self.uid, self.YATEAM_UID_1)
            self._assert_user_has_yateam_service(self.uid)

            PassportStub.update_info_by_uid(self.uid, is_2fa_enabled=False)
            self.json_ok('passport_user_2fa_changed', json={'uid': self.uid})
        self._assert_no_nda_folder(self.uid)
        self._assert_user_has_yateam_uid(self.uid, self.YATEAM_UID_1)
        self._assert_user_has_yateam_service(self.uid)

    def test_dismissed_yateam_user_enabled_2fa(self):
        with StaffServiceSmartMockHelper.mock_get_user_info(), \
                mock.patch('mpfs.core.yateam.logic.YATEAM_ENABLE_FOR_ALL', True):
            StaffServiceSmartMockHelper.add_user_info(self.YATEAM_UID_1, uid=self.uid, is_dismissed=True)
            PassportStub.update_info_by_uid(self.uid, is_2fa_enabled=False)
            self.json_ok('staff_user_changed_callback', {'yateam_uid': self.YATEAM_UID_1})
            self._assert_no_nda_folder(self.uid)
            self._assert_user_has_yateam_uid(self.uid, None)
            self._assert_user_does_not_have_yateam_service(self.uid)

            PassportStub.update_info_by_uid(self.uid, is_2fa_enabled=True)
            self.json_ok('passport_user_2fa_changed', json={'uid': self.uid})
        self._assert_no_nda_folder(self.uid)
        self._assert_user_has_yateam_uid(self.uid, None)
        self._assert_user_does_not_have_yateam_service(self.uid)

    def test_user_enabled_2fa(self):
        with StaffServiceSmartMockHelper.mock_get_user_info(), \
                mock.patch('mpfs.core.yateam.logic.YATEAM_ENABLE_FOR_ALL', True):
            PassportStub.update_info_by_uid(self.uid, is_2fa_enabled=True)
            self.json_ok('passport_user_2fa_changed', json={'uid': self.uid})
        self._assert_no_nda_folder(self.uid)
        self._assert_user_has_yateam_uid(self.uid, None)
        self._assert_user_does_not_have_yateam_service(self.uid)

    def test_user_disabled_2fa(self):
        with StaffServiceSmartMockHelper.mock_get_user_info(), \
                mock.patch('mpfs.core.yateam.logic.YATEAM_ENABLE_FOR_ALL', True):
            PassportStub.update_info_by_uid(self.uid, is_2fa_enabled=False)
            self.json_ok('passport_user_2fa_changed', json={'uid': self.uid})
        self._assert_no_nda_folder(self.uid)
        self._assert_user_has_yateam_uid(self.uid, None)
        self._assert_user_does_not_have_yateam_service(self.uid)
