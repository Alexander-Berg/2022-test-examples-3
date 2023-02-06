# -*- coding: utf-8 -*-

import mock
import time

from contextlib import contextmanager

from mpfs.core.queue import mpfs_queue
from mpfs.core.social.share.notifier import IndexerNotifier

from test.common.sharing import CommonSharingMethods


class ShareKickFromGroupTestCase(CommonSharingMethods):
    """Тестовый класс для метода `share_kick_from_group`."""
    api_method_name = 'share_kick_from_group'

    def teardown_method(self, method):
        self.inspect_all()
        super(ShareKickFromGroupTestCase, self).teardown_method(method)

    def test_kick_user_search_index_async_notification(self):
        """Проверить что при удалении пользователя из группы его индекс в поиске обновляется асинхронно и туда
        отправляются корректные данные."""
        owner_uid = self.uid
        invited_user_uid = self.uid_3
        invited_user_email = self.email_3

        self.create_user(owner_uid)
        self.create_user(invited_user_uid)

        # создаем папку владельца и кладем туда внутрь папку и файл
        self.json_ok('mkdir', {'uid': owner_uid, 'path': '/disk/owner_shared'})
        self.json_ok('mkdir', {'uid': owner_uid, 'path': '/disk/owner_shared/folder_inside'})
        self.upload_file(owner_uid, '/disk/owner_shared/file_inside.jpg')

        gid = self.create_group(path='/disk/owner_shared')
        hsh = self.invite_user(uid=invited_user_uid, email=invited_user_email, path='/disk/owner_shared')
        self.activate_invite(uid=invited_user_uid, hash=hsh)

        # меняем название папки у приглашенного пользователя
        self.json_ok('move', {
            'uid': invited_user_uid,
            'src': '/disk/owner_shared',
            'dst': '/disk/user_shared'
        })

        with mock.patch.object(IndexerNotifier, 'push_leave_folder') as mocked_push_leave_folder:
            self.mail_ok(self.api_method_name, {
                'uid': owner_uid,
                'user_uid': invited_user_uid,
                'gid': gid,
            })
            assert mocked_push_leave_folder.called
            args, kwargs = mocked_push_leave_folder.call_args
            all_args = dict(zip(('folder_index', 'action', 'uid'), args), **kwargs)
            assert all_args['action'] == 'delete'
            assert 'uid' in all_args
            assert all_args['uid'] == invited_user_uid
            assert 'folder_index' in all_args
            folder_index = all_args['folder_index']
            assert '/disk/user_shared' in folder_index
            assert '/disk/user_shared/folder_inside' in folder_index
            assert '/disk/user_shared/file_inside.jpg' in folder_index

            assert folder_index['/disk/user_shared']['id'] == '/disk/user_shared'
            assert folder_index['/disk/user_shared']['name'] == 'user_shared'
            assert folder_index['/disk/user_shared']['uid'] == invited_user_uid

            assert folder_index['/disk/user_shared/folder_inside']['id'] == '/disk/user_shared/folder_inside'
            assert folder_index['/disk/user_shared/folder_inside']['name'] == 'folder_inside'
            assert folder_index['/disk/user_shared/folder_inside']['uid'] == invited_user_uid

            assert folder_index['/disk/user_shared/file_inside.jpg']['id'] == '/disk/user_shared/file_inside.jpg'
            assert folder_index['/disk/user_shared/file_inside.jpg']['name'] == 'file_inside.jpg'
            assert folder_index['/disk/user_shared/file_inside.jpg']['uid'] == invited_user_uid

    def test_kick_user_search_index_async_index(self):
        """Проверить что при удалении пользователя из группы его индекс в поиске обновляется асинхронно
        и ручка отрабатывает быстро даже на большой индексации (без private copy)"""
        owner_uid = self.uid
        invited_user_uid = self.uid_3
        invited_user_email = self.email_3

        self.create_user(owner_uid)
        self.create_user(invited_user_uid)

        # создаем папку владельца и кладем туда внутрь папку и файл
        self.json_ok('mkdir', {'uid': owner_uid, 'path': '/disk/owner_shared'})
        self.json_ok('mkdir', {'uid': owner_uid, 'path': '/disk/owner_shared/folder_inside'})
        self.upload_file(owner_uid, '/disk/owner_shared/file_inside.jpg')

        gid = self.create_group(path='/disk/owner_shared')
        hsh = self.invite_user(uid=invited_user_uid, email=invited_user_email, path='/disk/owner_shared')
        self.activate_invite(uid=invited_user_uid, hash=hsh)

        # меняем название папки у приглашенного пользователя
        self.json_ok('move', {
            'uid': invited_user_uid,
            'src': '/disk/owner_shared',
            'dst': '/disk/user_shared'
        })

        with mock.patch.object(mpfs_queue, 'put') as mpfs_queue_put_mock:
            self.mail_ok(self.api_method_name, {
                'uid': owner_uid,
                'user_uid': invited_user_uid,
                'gid': gid,
            })
            self.assertIn(
                'group_user_kicked_notify_search_index',
                map(lambda a: a[0][1], mpfs_queue_put_mock.call_args_list)
            )
