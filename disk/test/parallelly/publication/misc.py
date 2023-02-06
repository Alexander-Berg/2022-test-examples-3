# -*- coding: utf-8 -*-
import urlparse
import mock

from test.common.sharing import CommonSharingMethods
# Импортируем базовый класс перед остальными импортами, чтобы подготовить окружение для тестов.
# Смотри test.mpfs_test_environment_maker для большей информации.
from test.parallelly.publication.base import BasePublicationMethods

import posixpath

import mpfs.core.filesystem.resources.disk

from test.helpers.stubs.services import PassportStub
from test.helpers.stubs.resources.users_info import DEFAULT_USERS_INFO, update_info_by_uid

from mpfs.common.static import codes
from mpfs.core import base as core
from mpfs.core.address import Address, PublicAddress
from mpfs.core.factory import get_resource
from mpfs.engine.process import dbctl
from mpfs.core.filesystem.resources.disk import get_blockings_collection
from mpfs.frontend.request import UserRequest
from mpfs.core.filesystem.quota import Quota


mpfs_db = dbctl().database()


class MiscPublicationTestCase(BasePublicationMethods):
    def test_public_info_uid_user_data(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/1'})
        hsh = self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/1'})['hash']
        hsh = self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/1'})['hash']
        resp = self.json_ok('public_info', {'private_hash': hsh})
        assert resp['user']['uid'] == self.uid

    def test_public_list_trash(self):
        for i in range(5):
            self.upload_file(self.uid, '/disk/%i.jpg' % i, opts={'public': 1})
            if i % 2 != 0:
                self.json_ok('trash_append', {'uid': self.uid, 'path': '/disk/%i.jpg' % i})

        assert len(self.json_ok('list_public', {'uid': self.uid})) == 3
        assert len(self.json_ok('list_public', {'uid': self.uid, 'amount': 0})) == 0
        assert len(self.json_ok('list_public', {'uid': self.uid, 'amount': 1})) == 1
        assert len(self.json_ok('list_public', {'uid': self.uid, 'amount': 2})) == 2
        assert len(self.json_ok('list_public', {'uid': self.uid, 'amount': 3})) == 3
        assert len(self.json_ok('list_public', {'uid': self.uid, 'amount': 4})) == 3
        assert len(self.json_ok('list_public', {'uid': self.uid, 'amount': 5})) == 3

        assert len(self.json_ok('list_public', {'uid': self.uid, 'offset': 1})) == 2
        assert len(self.json_ok('list_public', {'uid': self.uid, 'amount': 1, 'offset': 1})) == 1
        assert len(self.json_ok('list_public', {'uid': self.uid, 'amount': 2, 'offset': 1})) == 2
        assert len(self.json_ok('list_public', {'uid': self.uid, 'amount': 3, 'offset': 1})) == 2

    def test_public_notification(self):
        self.make_file()
        faddr = Address.Make(self.uid, self.pub_file).id
        request = self.get_request({'uid': self.uid, 'path': faddr,
                                    'emails': 'mpfs-dev@yandex.ru',
                                    'message': 'hey, girls! это я',
                                    'connection_id': '',
                                    'locale': 'ru'})
        core.public_notification(request)

    def test_desktop_diff(self):
        self.make_dir(True)
        opts = {
            'uid': self.uid,
            'path': '/disk',
            'meta': ''
            }
        diff_result = self.json_ok('diff', opts)
        found = False
        for element in diff_result['result']:
            if element['key'] == '/disk/pub':
                found = True
                self.assertEqual(element.get('public'), 1)
                break
        self.assertTrue(found)

    def test_make_photostream_public(self):
        self.upload_file(self.uid, '/photostream/file.jpg')
        opts = {
            'uid': self.uid,
            'path': '/disk/Фотокамера',
            'meta': '',
            }
        result = self.json_ok('info', opts)
        self.assertTrue('folder_type' in result['meta'])
        self.assertEqual(result['meta']['folder_type'], 'photostream')
        self.assertFalse('public_hash' in result['meta'])
        result = self.json_ok('set_public', opts)
        self.assertTrue('hash' in result)
        result = self.json_ok('info', opts)
        self.assertTrue('folder_type' in result['meta'])
        self.assertEqual(result['meta']['folder_type'], 'photostream')
        self.assertTrue('public_hash' in result['meta'])

    def test_make_photostream_private(self):
        self.upload_file(self.uid, '/photostream/file.jpg')
        opts = {
            'uid': self.uid,
            'path': '/disk/Фотокамера',
            'meta': '',
            }
        result = self.json_ok('set_public', opts)
        result = self.json_ok('info', opts)
        self.assertTrue('folder_type' in result['meta'])
        self.assertEqual(result['meta']['folder_type'], 'photostream')
        self.assertTrue('public_hash' in result['meta'])
        self.json_ok('set_private', opts)
        result = self.json_ok('info', opts)
        self.assertTrue('folder_type' in result['meta'])
        self.assertEqual(result['meta']['folder_type'], 'photostream')
        self.assertFalse('public_hash' in result['meta'])

    def test_rename_photostream(self):
        self.upload_file(self.uid, '/photostream/file.jpg')
        opts = {
            'uid': self.uid,
            'src': '/disk/Фотокамера',
            'dst': '/disk/Фотокамера_1',
            }
        self.json_ok('async_move', opts)
        opts = {
            'uid': self.uid,
            'path': '/disk/Фотокамера_1',
            'meta': '',
            }
        result = self.json_ok('info', opts)
        self.assertFalse('folder_type' in result['meta'])

    def test_attach_folder(self):
        opts = {'uid': self.uid, 'path': '/disk/to_attach'}
        self.json_ok('mkdir', opts)
        self.upload_file(self.uid, '/disk/to_attach/test.txt')

        opts = {'uid': self.uid, 'src':
                '/disk/to_attach', 'dst': '/attach/to_attach'}
        oid = self.json_ok('async_copy', opts)['oid']

        opts = {'oid': oid, 'uid': self.uid, 'meta': ''}
        public_hash = self.json_ok(
            'status', opts)['resource']['meta']['public_hash']

        opts = {'private_hash': public_hash}
        url = self.json_ok('public_url', opts)
        self.assertTrue('folder' in url)

    def test_restored_resources_have_same_public_hash(self):
        """Проверить, что после восстановления корзины публичных файлов и папок
        публичные хеши сохраняются.
        """
        p_join = posixpath.join

        base_folder = '/disk/pub'
        folders = (base_folder, p_join(base_folder, 'folder1'))
        for folder in folders:
            self.json_ok('mkdir', {'uid': self.uid, 'path': folder})

        files = tuple(p_join(folder, 'file.ext') for folder in folders)
        for file_name in files:
            self.upload_file(self.uid, file_name)

        public_hashes = []
        for path in folders + files:
            response = self.json_ok('set_public', {'uid': self.uid, 'path': path})
            public_hashes.append(response['hash'])

        # Зачем? Но оставил
        self.json_ok('trash_drop_all', {'uid': self.uid})

        trash_folder_path = self.json_ok('trash_append', {'uid': self.uid, 'path': base_folder})['this']['id']
        for path in folders + files:
            path = path.replace(base_folder, trash_folder_path)
            info = self.json_ok('info', {'uid': self.uid, 'path': path, 'meta': ''})
            public_hash = info['meta']['public_hash']
            assert public_hash in public_hashes
            assert 'published' in info['meta']
            assert 'public' not in info['meta']
            self.json_error('public_info', {'private_hash': public_hash},
                            codes.RESOURCE_NOT_FOUND)

        self.json_ok('trash_restore', {'uid': self.uid, 'path': trash_folder_path})
        for path in folders + files:
            info = self.json_ok('info', {'uid': self.uid, 'path': path, 'meta': ''})
            public_hash = info['meta']['public_hash']
            assert public_hash in public_hashes
            assert 'published' not in info['meta']
            assert 'public' in info['meta']
            self.json_ok('public_info', {'private_hash': public_hash})

    def test_public_url_for_blocked_public_file(self):
        """
        Протестировать получение URL для заблокированного публичного файла.

        Когда переданный публичный хеш указывает на корень публичной папки/файла, то мы получаем NotFound.
        """
        self.upload_file(uid=self.uid, path='/disk/public_file.jpg')
        result = self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/public_file.jpg'})
        public_hash = result['hash']
        result = self.support_ok('block_public_file', {
            'moderator': 'moderator',
            'comment': 'comment',
            'private_hash': public_hash,
            'type': 'block_file',
            'view': 'st',
            'link': 'https://rkn.gov.ru/',
            'notify': 0,
        })
        self.assertTrue(result)

        self.json_error('public_url', {
            'private_hash': public_hash
        }, code=codes.RESOURCE_NOT_FOUND)

    def test_public_url_for_deleted_account(self):
        self.upload_file(uid=self.uid, path='/disk/public_file.jpg')
        result = self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/public_file.jpg'})
        public_hash = result['hash']

        with PassportStub(userinfo=DEFAULT_USERS_INFO['deleted_account']) as stub:
            self.json_error('public_url', {
                'private_hash': public_hash
            }, code=codes.RESOURCE_NOT_FOUND)
            assert stub.userinfo.called

    def test_public_url_for_blocked_by_passport_account(self):
        self.upload_file(uid=self.uid, path='/disk/public_file.jpg')
        public_hash = self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/public_file.jpg'})['hash']

        update_info_by_uid(self.uid, is_enabled=False)

        self.json_error('public_url', {'private_hash': public_hash}, code=codes.RESOURCE_NOT_FOUND)

    def test_public_video_url_for_blocked_by_passport_account(self):
        self.upload_file(uid=self.uid, path='/disk/public_file.jpg')
        public_hash = self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/public_file.jpg'})['hash']

        update_info_by_uid(self.uid, is_enabled=False)

        self.json_error('public_video_url', {'private_hash': public_hash}, code=codes.RESOURCE_NOT_FOUND)

    def test_public_url_for_resource_inside_blocked_folder_within_public_folder(self):
        """
        Протестировать получение URL для файла внутри папки, которая является подпапкой публичной папки.

        Когда корень публичной папки не заблокирован, а один из предков внутри публичной папки заблокирован, то
        получаем ResourceBlocked.
        """
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/public_folder'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/public_folder/blocked_folder'})
        self.upload_file(uid=self.uid, path='/disk/public_folder/blocked_folder/trump.jpg')
        result = self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/public_folder'})
        public_hash = result['hash']
        result = self.support_ok('block_public_file', {
            'moderator': 'moderator',
            'comment': 'comment',
            'private_hash': public_hash + ':/blocked_folder',
            'type': 'block_file',
            'view': 'st',
            'link': 'https://rkn.gov.ru/',
            'notify': 0,
        })
        self.assertTrue(result)
        self.json_error('public_url', {
            'private_hash': public_hash + ':/blocked_folder/trump.jpg'
        }, code=codes.RESOURCE_BLOCKED)

    def test_public_url_for_resource_inside_folder_within_blocked_public_folder(self):
        """
        Протестировать получение URL для файла внутри папки, которая является подпапкой публичной папки, но
        при этом публичная папка заблокирована.

        Когда корень публичной папки заблокирован, запрос URL для любого потомка возвращает NotFound (тк при блокировке
        корня публичной папки по сути происходит одновременно её распубликация).
        """
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/public_folder'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/public_folder/sub_folder'})
        self.upload_file(uid=self.uid, path='/disk/public_folder/sub_folder/trump.jpg')
        result = self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/public_folder'})
        public_hash = result['hash']
        result = self.support_ok('block_public_file', {
            'moderator': 'moderator',
            'comment': 'comment',
            'private_hash': public_hash,
            'type': 'block_file',
            'view': 'st',
            'link': 'https://rkn.gov.ru/',
            'notify': 0,
        })
        self.assertTrue(result)
        self.json_error('public_url', {
            'private_hash': public_hash + ':/sub_folder/trump.jpg'
        }, code=codes.RESOURCE_NOT_FOUND)

    def test_public_url_for_resource_inside_two_public_folders_where_one_of_them_blocked(self):
        """
        Протестировать получение URL для файла внутри публичной папки, которая является потомком другой публичной
        заблокированной папки. Урл пытаемся получить относительно незаблокированной публичной папки,
        но которая находится в заблокированной.
        """
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/public_folder_1'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/public_folder_1/public_folder_2'})
        self.upload_file(uid=self.uid, path='/disk/public_folder_1/public_folder_2/trump.jpg')
        result = self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/public_folder_1'})
        public_hash_1 = result['hash']
        result = self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/public_folder_1/public_folder_2'})
        public_hash_2 = result['hash']
        result = self.support_ok('block_public_file', {
            'moderator': 'moderator',
            'comment': 'comment',
            'private_hash': public_hash_1,
            'type': 'block_file',
            'view': 'st',
            'link': 'https://rkn.gov.ru/',
            'notify': 0,
        })
        self.assertTrue(result)
        # сама папка
        self.json_error('public_url', {
            'private_hash': public_hash_2
        }, code=codes.RESOURCE_BLOCKED)
        # ресурс внутри
        self.json_error('public_url', {
            'private_hash': public_hash_2 + ':/trump.jpg'
        }, code=codes.RESOURCE_BLOCKED)

    def test_folder_url_for_subfolder_of_public_folder(self):
        public_folder = '/disk/folder'
        public_folder_subfolder = public_folder + '/subfolder'
        public_folder_subfolder_file = public_folder_subfolder + '/file.txt'

        self.json_ok('mkdir', {'uid': self.uid, 'path': public_folder})
        self.json_ok('mkdir', {'uid': self.uid, 'path': public_folder_subfolder})
        self.upload_file(self.uid, public_folder_subfolder_file)

        public_info = self.json_ok('set_public', {'uid': self.uid, 'path': public_folder})
        private_hash = public_info['hash']

        relative_path = private_hash + ':/subfolder'
        public_url_info = self.json_ok('public_url', {'uid': self.uid,
                                                      'private_hash': relative_path})
        subfolder_folder_url = public_url_info['folder']

        parsed_url = urlparse.urlparse(subfolder_folder_url)
        qs_params = urlparse.parse_qs(parsed_url.query)
        assert qs_params['uid'][0] == '0'

    def test_public_url_with_blockings(self):
        file_path = '/disk/file.txt'
        self.upload_file(self.uid, file_path)

        file_info = self.json_ok('info', {'uid': self.uid, 'path': file_path, 'meta': 'hid'})
        hid = file_info['meta']['hid']

        get_blockings_collection().insert({'_id': hid, 'data': 'i_dont_know_what_to_put_here'})

        public_info = self.json_ok('set_public', {'uid': self.uid, 'path': file_path})
        private_hash = public_info['hash']

        self.json_error('public_url', {'private_hash': private_hash}, code=codes.FILE_NOT_FOUND)
        with mock.patch.object(mpfs.core.filesystem.resources.disk.BlockingsMixin._zaberun_service,
                               'unlimited_users', [self.uid,]):
            self.json_ok('public_url', {'private_hash': private_hash})

    def _prepare_public_file(self, is_owner_paid, is_other_user_paid, is_blocked=True):
        self.create_user(self.uid_1)

        if is_owner_paid:
            self.billing_ok('service_create',
                            {'uid': self.uid, 'ip': '1', 'line': 'primary_2015', 'pid': '10gb_1m_2015'})
        if is_other_user_paid:
            self.billing_ok('service_create',
                            {'uid': self.uid_1, 'ip': '1', 'line': 'primary_2015', 'pid': '10gb_1m_2015'})

        file_path = '/disk/file.txt'
        self.upload_file(self.uid, file_path)

        file_info = self.json_ok('info', {'uid': self.uid, 'path': file_path, 'meta': 'hid'})
        hid = file_info['meta']['hid']

        if is_blocked:
            get_blockings_collection().insert({'_id': hid, 'data': 'i_dont_know_what_to_put_here'})

        public_info = self.json_ok('set_public', {'uid': self.uid, 'path': file_path})
        private_hash = public_info['hash']

        return private_hash

    def test_public_url_for_not_logined_user(self):
        private_hash = self._prepare_public_file(True, True)
        with mock.patch('mpfs.core.filesystem.quota.FEATURE_TOGGLES_SPEED_LIMIT', True):
            self.json_error('public_url', {'private_hash': private_hash}, code=codes.FILE_NOT_FOUND)

    def test_public_url_for_paid_user(self):
        private_hash = self._prepare_public_file(True, True)
        with mock.patch('mpfs.core.filesystem.quota.FEATURE_TOGGLES_SPEED_LIMIT', True):
            self.json_ok('public_url', {'private_hash': private_hash, 'uid': self.uid})

    def test_public_url_for_free_user_and_paid_owner(self):
        private_hash = self._prepare_public_file(True, False)
        with mock.patch('mpfs.core.filesystem.quota.FEATURE_TOGGLES_SPEED_LIMIT', True):
            self.json_error('public_url', {'private_hash': private_hash, 'uid': self.uid_1},
                            code=codes.FILE_NOT_FOUND)

    def test_public_url_for_paid_user_and_free_owner(self):
        private_hash = self._prepare_public_file(False, True)
        with mock.patch('mpfs.core.filesystem.quota.FEATURE_TOGGLES_SPEED_LIMIT', True):
            self.json_ok('public_url', {'private_hash': private_hash, 'uid': self.uid_1})

    def test_public_url_for_not_initialized_user(self):
        not_initialized_uid = '123123123'
        private_hash = self._prepare_public_file(True, True)
        with mock.patch('mpfs.core.filesystem.quota.FEATURE_TOGGLES_SPEED_LIMIT', True):
            self.json_error('public_url', {'private_hash': private_hash, 'uid': not_initialized_uid},
                            code=codes.FILE_NOT_FOUND)

    def test_public_url_for_not_initialized_user_and_not_blocked_file(self):
        not_initialized_uid = '123123123'
        private_hash = self._prepare_public_file(True, True, False)
        with mock.patch('mpfs.core.filesystem.quota.FEATURE_TOGGLES_SPEED_LIMIT', True):
            self.json_ok('public_url', {'private_hash': private_hash, 'uid': not_initialized_uid})

    def test_public_url_check_blockings(self):
        file_path = '/disk/file.txt'
        self.upload_file(self.uid, file_path)

        file_info = self.json_ok('info', {'uid': self.uid, 'path': file_path, 'meta': 'hid'})
        hid = file_info['meta']['hid']

        get_blockings_collection().insert({'_id': hid, 'data': 'i_dont_know_what_to_put_here'})

        public_info = self.json_ok('set_public', {'uid': self.uid, 'path': file_path})
        private_hash = public_info['hash']

        self.json_error('public_url', {'private_hash': private_hash}, code=codes.FILE_NOT_FOUND)
        self.json_ok('public_url', {'private_hash': private_hash, 'check_blockings': 0})

    def test_public_url_of_overdrawn_owner(self):
        Quota().set_limit(1000000, uid=self.uid)
        file_data = {'size': 100000}
        path = '/disk/video_file.mov'
        self.upload_video(self.uid, path, file_data=file_data)
        result = self.json_ok('set_public', {'uid': self.uid, 'path': path, 'connection_id': ''})
        public_hash = result['hash']

        Quota().set_limit(100000, uid=self.uid)
        self.json_error('public_url', {'private_hash': public_hash}, code=codes.FILE_NOT_FOUND)

    def test_public_url_with_speed_limit_as_blockings(self):
        file_path = '/disk/file.txt'
        self.upload_file(self.uid, file_path)

        public_info = self.json_ok('set_public', {'uid': self.uid, 'path': file_path})
        private_hash = public_info['hash']

        req = UserRequest({})
        req.set_args({'uid': self.uid, 'hash': private_hash, 'bytes_downloaded': 1000000000000, 'count': 1})
        core.kladun_download_counter_inc(req)

        self.json_error('public_url', {'private_hash': private_hash}, code=codes.FILE_NOT_FOUND)
        with mock.patch.object(mpfs.core.filesystem.resources.disk.BlockingsMixin._zaberun_service,
                               'unlimited_users', [self.uid,]):
            self.json_ok('public_url', {'private_hash': private_hash})

    def test_public_info_uid_for_image_in_public_folder(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder_1'})
        self.upload_file(self.uid, '/disk/folder_1/file.jpg')
        private_hash = self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/folder_1'})['hash']
        preview_url = self.json_ok('public_info', {'private_hash': private_hash + ':/file.jpg', 'meta': ''})['resource']['meta']['preview']
        parsed_url = urlparse.urlparse(preview_url)
        qs_params = urlparse.parse_qs(parsed_url.query)
        assert qs_params['uid'][0] == '0'

    def test_public_info_for_folder_in_public_folder(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder_1'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder_1/folder_2'})
        private_hash = self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/folder_1'})['hash']
        self.json_ok('public_info', {'private_hash': private_hash + ':/folder_2'})

    def test_public_info_for_resource_with_yadisk_cc_short_url(self):
        uid = self.uid
        path = '/disk/test'
        self.json_ok('mkdir', {'uid': uid, 'path': path})
        result = self.json_ok('set_public', {'uid': uid, 'path': path})
        private_hash = result['hash']

        resource = get_resource(uid, path)

        # имитируем что был в базе урл старого вида
        resource.meta['short_url'] = 'https://yadisk.cc/d/JHskEykqOa'
        resource.save()

        resource = get_resource(uid, path)
        assert resource.meta['short_url'] == 'https://yadisk.cc/d/JHskEykqOa'

        result = self.json_ok('public_info', {'uid': uid, 'private_hash': private_hash, 'meta': ''})
        assert result['resource']['meta']['short_url'] == 'https://yadi.sk/d/JHskEykqOa'

        resource = get_resource(uid, path)
        assert resource.meta['short_url'] == 'https://yadi.sk/d/JHskEykqOa'

    def test_public_info_for_resource_with_yadisk_cc_short_url_named(self):
        uid = self.uid
        path = '/disk/test'
        self.json_ok('mkdir', {'uid': uid, 'path': path})
        result = self.json_ok('set_public', {'uid': uid, 'path': path})
        private_hash = result['hash']

        resource = get_resource(uid, path)

        # имитируем что был в базе урл старого вида
        resource.meta['short_url'] = 'https://yadisk.cc/d/JHskEykqOa'
        resource.save()

        resource = get_resource(uid, path)
        # short_url_named формируется из short_url + name под капотом
        assert resource.meta['short_url_named'] == 'https://yadisk.cc/d/JHskEykqOa?test'

        result = self.json_ok('public_info', {'uid': uid, 'private_hash': private_hash, 'meta': ''})
        assert result['resource']['meta']['short_url_named'] == 'https://yadi.sk/d/JHskEykqOa?test'

        resource = get_resource(uid, path)
        assert resource.meta['short_url_named'] == 'https://yadi.sk/d/JHskEykqOa?test'


class SharedPublicationTestCase(CommonSharingMethods, BasePublicationMethods):
    SHARED_FOLDER = '/disk/Shared'

    def test_public_url_through_relative_hash_as_guest(self):
        """Протестировать что ручка public_url не кидает ошибку `StorageInitUser`
        в случае, когда запрашивается url файла внутри публичной папки, которую опубликовал гость.
        """
        self.create_user(self.uid_3)
        self.json_ok('mkdir', {'uid': self.uid_3, 'path': self.SHARED_FOLDER})
        self.create_share_for_guest(self.uid_3, self.SHARED_FOLDER, self.uid, self.email)

        self.upload_file(uid=self.uid_3, path='/disk/Shared/test.txt')

        response = self.json_ok('set_public', {'uid': self.uid, 'path': self.SHARED_FOLDER})
        assert 'hash' in response

        private_hash = response['hash']
        response = self.json_ok('public_url', {'private_hash': private_hash + ':/test.txt'})
        assert 'file' in response

    def test_public_info_through_relative_hash_as_guest(self):
        """Протестировать что ручка public_info не кидает ошибку `StorageInitUser`
        в случае, когда запрашивается info папки внутри публичной папки, которую опубликовал гость.
        """
        self.create_user(self.uid_3)
        self.json_ok('mkdir', {'uid': self.uid_3, 'path': self.SHARED_FOLDER})
        self.json_ok('mkdir', {'uid': self.uid_3, 'path': self.SHARED_FOLDER + '/Inner'})
        self.create_share_for_guest(self.uid_3, self.SHARED_FOLDER, self.uid, self.email)

        response = self.json_ok('set_public', {'uid': self.uid, 'path': self.SHARED_FOLDER})
        assert 'hash' in response

        private_hash = response['hash']
        self.json_ok('public_info', {'private_hash': private_hash + ':/Inner'})

    def test_traffic_counter_update(self):
        """Протестировать обновление счетчика трафика у файла
        в случае, когда запрашивается url файла внутри публичной папки, которую опубликовал гость.
        """
        self.create_user(self.uid_3)
        self.json_ok('mkdir', {'uid': self.uid_3, 'path': self.SHARED_FOLDER})
        self.create_share_for_guest(self.uid_3, self.SHARED_FOLDER, self.uid, self.email)

        self.upload_file(uid=self.uid_3, path='/disk/Shared/test1.txt')

        response = self.json_ok('set_public', {'uid': self.uid, 'path': self.SHARED_FOLDER})

        private_hash = response['hash']
        pub_address = PublicAddress.Make(private_hash, '/test1.txt')
        req = UserRequest({})
        req.set_args(
            {'hash': pub_address.id,
             'bytes_downloaded': 100, 'count': 1})
        traffic_before = Quota().download_traffic(self.uid_3)
        core.kladun_download_counter_inc(req)
        traffic_after = Quota().download_traffic(self.uid_3)
        self.assertEqual(traffic_before + 100, traffic_after)
