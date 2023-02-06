# -*- coding: utf-8 -*-
import posixpath
import uuid
import mock

from mpfs.common.static import tags
from mpfs.common.util import trace_calls

from test.base_suit import SharingTestCaseMixin
from mpfs.engine.queue2.celery import BaseTask
from test.helpers.utils import check_task_called
from test.parallelly.api.base import ApiTestCase
from test.base import DiskTestCase


class NotifySmartCacheTestCase(DiskTestCase, ApiTestCase, SharingTestCaseMixin):
    """Набор тестов для проверки уведомлений SmartCache в результате
    изменений файлов фотосреза"""

    api_version = 'v1'
    api_mode = tags.platform.INTERNAL

    def setup_method(self, method):
        super(NotifySmartCacheTestCase, self).setup_method(method)
        self.login = self.uid
        self.fake_apply_async = check_task_called(
            BaseTask.apply_async,
            'mpfs.core.job_handlers.indexer.handle_notify_smartcache_photoslice_changed'
        )

    def teardown_method(self, method):
        self.remove_uploaded_files()
        super(NotifySmartCacheTestCase, self).teardown_method(method)

    def _create_folder(self, path='/disk/some_folder'):
        folder_data = {'uid': self.uid, 'path': '{}:{}'.format(self.uid, path)}
        self.json_ok('mkdir', folder_data)

        return folder_data

    def _upload_unique_photo_slice_file(self, base_path='disk', meta=None):
        if not meta:
            meta = {'mimetype': 'image/jpeg', 'etime': '2012-04-05T10:00:00Z'}

        name = 'photo_{}.raw'.format(uuid.uuid4().get_hex())
        full_path = posixpath.join('/', base_path, name)
        self.upload_file(self.uid, full_path, file_data=meta)

        return full_path

    def test_file_created(self):
        """Проверка уведомления после добавления файла фотосреза"""

        with mock.patch.object(BaseTask, 'apply_async', self.fake_apply_async) as mocked_notify_smartcache_changed:
            self._upload_unique_photo_slice_file()
            assert mocked_notify_smartcache_changed.called

    def test_file_removed(self):
        """Проверка уведомления после удаления файла фотосреза"""
        with mock.patch.object(BaseTask, 'apply_async', self.fake_apply_async) as mocked_notify_smartcache_changed:
            self._upload_unique_photo_slice_file()
            assert mocked_notify_smartcache_changed.call_count == 1

            self.json_ok('rm', opts=self.uploaded_files.pop())
            assert mocked_notify_smartcache_changed.call_count == 2

    def test_file_prop_changed(self):
        """Проверка уведомления после изменения свойств файла фотосреза"""
        with mock.patch.object(BaseTask, 'apply_async', self.fake_apply_async) as mocked_notify_smartcache_changed:
            self._upload_unique_photo_slice_file()
            assert mocked_notify_smartcache_changed.call_count == 1

            uploaded_file = self.uploaded_files.pop()
            uploaded_file.update({'foo': 'spam'})

            self.json_ok('setprop', opts=uploaded_file)
            assert mocked_notify_smartcache_changed.call_count == 2

    def test_folder_removed(self):
        """Проверка уведомления после удаления папки, содержащей файл фотосреза"""
        folder_path = '/disk/some_folder'
        folder_data = self._create_folder(folder_path)

        with mock.patch.object(BaseTask, 'apply_async', self.fake_apply_async) as mocked_notify_smartcache_changed:
            self._upload_unique_photo_slice_file(base_path=folder_path)
            assert mocked_notify_smartcache_changed.call_count == 1

            self.json_ok('rm', opts=folder_data)
            assert mocked_notify_smartcache_changed.call_count == 2

    def test_file_trash_append(self):
        """Проверка уведомления после отправки файла в корзину"""
        with mock.patch.object(BaseTask, 'apply_async', self.fake_apply_async) as mocked_notify_smartcache_changed:
            self._upload_unique_photo_slice_file()
            assert mocked_notify_smartcache_changed.call_count == 1

            self.json_ok('trash_append', opts=self.uploaded_files.pop())
            assert mocked_notify_smartcache_changed.call_count == 2

    def test_file_trash_drop_all(self):
        """Проверка уведомления после удаления файла из корзины"""
        with mock.patch.object(BaseTask, 'apply_async', self.fake_apply_async) as mocked_notify_smartcache_changed:
            self._upload_unique_photo_slice_file()
            assert mocked_notify_smartcache_changed.call_count == 1

            trashed_file = self.uploaded_files.pop()
            self.json_ok('trash_append', opts=trashed_file)
            assert mocked_notify_smartcache_changed.call_count == 2

    def test_folder_trash_append(self):
        """Проверка уведомления после отправки папки, содержащей файла фотосреза, в корзину"""
        folder_path = '/disk/some_folder'
        folder_data = self._create_folder(folder_path)

        with mock.patch.object(BaseTask, 'apply_async', self.fake_apply_async) as mocked_notify_smartcache_changed:
            self._upload_unique_photo_slice_file(base_path=folder_path)
            assert mocked_notify_smartcache_changed.call_count == 1

            self.json_ok('trash_append', opts=folder_data)
            assert mocked_notify_smartcache_changed.call_count == 2

    def test_file_moved(self):
        """Проверка уведомления после перемещения файла фотосреза"""
        with mock.patch.object(BaseTask, 'apply_async', self.fake_apply_async) as mocked_notify_smartcache_changed:
            file_path = self._upload_unique_photo_slice_file()
            assert mocked_notify_smartcache_changed.call_count == 1

            self.json_ok('move', opts={'uid': self.uid, 'src': file_path, 'dst': '/disk/1.raw'})
            assert mocked_notify_smartcache_changed.call_count == 2

    def test_file_copied(self):
        """Проверка уведомления после копирования файла фотосреза"""
        with mock.patch.object(BaseTask, 'apply_async', self.fake_apply_async) as mocked_notify_smartcache_changed:
            file_path = self._upload_unique_photo_slice_file()
            assert mocked_notify_smartcache_changed.call_count == 1

            self.json_ok('copy', {'uid': self.uid, 'src': file_path, 'dst': '/disk/1.raw'})
            assert mocked_notify_smartcache_changed.call_count == 2

    def test_trash_restore(self):
        """Проверка уведомления после восстановления файла фотосреза из корзины"""
        with mock.patch.object(BaseTask, 'apply_async', self.fake_apply_async) as mocked_notify_smartcache_changed:
            file_path = self._upload_unique_photo_slice_file()
            assert mocked_notify_smartcache_changed.call_count == 1

            uploaded_file = self.uploaded_files.pop()
            trash_path = self.json_ok('trash_append', opts=uploaded_file)['this']['id']
            assert mocked_notify_smartcache_changed.call_count == 2

            uploaded_file.update({'path': trash_path})
            self.json_ok('trash_restore', opts=uploaded_file)
            assert mocked_notify_smartcache_changed.call_count == 3

    def test_copy_no_etime_from_photocamera(self):
        """ Проверить, что уведомляется смарткеш после того,
        как файл без etime, но из папки Фотокамера был скопирован в корневую директорию
        """
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/Фотокамера'})

        with mock.patch.object(BaseTask, 'apply_async', self.fake_apply_async) as mocked_notify_smartcache_changed:
            meta = {'mimetype': 'image/png'}
            base_path = '/disk/Фотокамера'
            file_path = self._upload_unique_photo_slice_file(base_path, meta)
            assert mocked_notify_smartcache_changed.call_count == 1

            self.json_ok('copy', {'uid': self.uid, 'src': file_path, 'dst': '/disk/1.png'})
            assert mocked_notify_smartcache_changed.call_count == 2

    def test_invited_unshared(self):
        """ Проверить, что приходит уведомление в результате изменений файлов фотосреза
        находящихся в расшаренной папке.

        Два кейса:
         1. уведомляем пользователя, принявшего инвайт
         2. после отзыва прав у приглашенного, уведомление обоих пользователей
        """

        other_uid = '1253912'
        self.create_user(other_uid)

        shared_folder = '/disk/shared'
        self.json_ok('mkdir', {'uid': self.uid, 'path': shared_folder})

        with mock.patch.object(BaseTask, 'apply_async', self.fake_apply_async) as mocked_notify_smartcache_changed:
            self._upload_unique_photo_slice_file(base_path=shared_folder)
            assert mocked_notify_smartcache_changed.call_count == 1

            group = self.json_ok('share_create_group', opts={'uid': self.uid, 'path': shared_folder})
            invite_hash = self.share_invite(group['gid'], other_uid)
            self.json_ok('share_activate_invite', opts={'uid': other_uid, 'hash': invite_hash})
            assert mocked_notify_smartcache_changed.call_count == 2

            self.json_ok('share_unshare_folder', opts={'uid': self.uid, 'gid': group['gid']})
            # 4 - т.к. оба пользователя уведомляются об изменениях
            assert mocked_notify_smartcache_changed.call_count == 4
