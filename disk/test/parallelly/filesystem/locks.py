# -*- coding: utf-8 -*-
import before_after
import pytest
from mock import patch
from hamcrest import assert_that, has_entries, is_not, empty, has_entry
from nose_parameterized import parameterized

from base import CommonFilesystemTestCase

import mpfs.engine.process
import mpfs.core.base as mbase

from mpfs.common import errors
from mpfs.core.address import Address
from mpfs.core.bus import Bus
from mpfs.core.filesystem.helpers import lock
from mpfs.core.metastorage.control import fs_locks
from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase
from mpfs.metastorage.mongo.collections.filesystem import FilesystemLocksCollection
from mpfs.metastorage.postgres.ttl import delete_expired_entries
from mpfs.common.static import codes
from mpfs.metastorage.postgres import ttl
from test.common.sharing import CommonSharingMethods
from test.conftest import INIT_USER_IN_POSTGRES

db = CollectionRoutedDatabase()
usrctl = mpfs.engine.process.usrctl()


class ProgressiveLocksFilesystemTestCase(CommonFilesystemTestCase):
    def setup_method(self, method):
        super(ProgressiveLocksFilesystemTestCase, self).setup_method(method)
        self._mkdirs()
        self._mkfiles()

    def __create_shared_folder(self, owner, guest, path):
        self.create_user(guest, noemail=1)

        opts = {'uid': owner, 'path': path}
        self.json_ok('mkdir', opts)
        self.json_ok('share_create_group', opts)

        opts = {
            'rights': 660,
            'universe_login': 'boo@boo.ru',
            'universe_service': 'email',
            'avatar': 'http://localhost/echo',
            'name': 'mpfs-test',
            'connection_id': '1234',
            'uid': owner,
            'path': path,
        }
        result = self.mail_ok('share_invite_user', opts)

        hsh = None
        for each in result.getchildren():
            if each.tag == 'hash' and each.text and isinstance(each.text, str):
                hsh = each.text
        self.assertTrue(hsh)

        opts = {'hash': hsh, 'uid': guest}
        folder_info = self.json_ok('share_activate_invite', opts)
        self.assertNotEqual(folder_info, None)

    def test_async_operations_fail_if_resource_locked(self):
        Bus().set_lock(Address.Make(self.uid, '/disk/filesystem test folder').id)

        src = Address.Make(self.uid, '/disk/filesystem test folder/inner file').id
        dest = Address.Make(self.uid, '/disk/filesystem test folder/moved inner file').id
        try:
            request = self.get_request({'uid': self.uid,
                                        'src': src,
                                        'dst': dest,
                                        'force': 0,
                                        'callback': None,
                                        'connection_id': None})
            mbase.async_move_resource(request)
        except errors.ResourceLocked:
            pass
        else:
            self.fail('Parent lock doesn`t work')

    def test_simple_lock_with_resource(self):
        """
        ?????????????????? ???????? ???? ?????????? ????????????????, ?????????? ???????????? ????????
        """
        args = {'uid': self.uid, 'path': '/disk/top/'}
        self.json_ok('mkdir', args)
        args = {'uid': self.uid, 'path': '/disk/top/middle/'}
        self.json_ok('mkdir', args)
        args = {'uid': self.uid, 'path': '/disk/top/middle/bottom'}
        self.json_ok('mkdir', args)

        # ?????????? ?????????????? ????????????, ???????????????? ??????-???? ???????????? ?????????? - ??????????????????
        Bus().set_lock(Address.Make(self.uid, '/disk/top/').id)
        args = {'uid': self.uid, 'src': '/disk/top/middle/bottom', 'dst': '/disk/top/middle_moved'}
        self.json_error('async_move', args)
        Bus().unset_lock(Address.Make(self.uid, '/disk/top/').id)

        # ?????????? ???????????? ????????????, ???????????????? ??????-???? ???????????? ?????????????? - ??????????????????
        Bus().set_lock(Address.Make(self.uid, '/disk/top/middle/bottom').id)
        args = {'uid': self.uid, 'src': '/disk/top/middle', 'dst': '/disk/top/middle_moved'}
        self.json_error('async_move', args)
        Bus().unset_lock(Address.Make(self.uid, '/disk/top/middle/bottom').id)

    def test_simple_lock_with_address(self):
        """
        ?????????????????? ???????? ???? ?????????? ????????????????, ?????????? ?????????????? ??????
        """
        args = {'uid': self.uid, 'path': '/disk/top/'}
        self.json_ok('mkdir', args)
        args = {'uid': self.uid, 'path': '/disk/top/middle/'}
        self.json_ok('mkdir', args)
        args = {'uid': self.uid, 'path': '/disk/top/middle/bottom'}
        self.json_ok('mkdir', args)

        # ?????????? ?????????????? ????????????, ???????????????? ??????-???? ???????????? ?????????? - ??????????????????
        Bus().set_lock(Address.Make(self.uid, '/disk/top').id)
        args = {'uid': self.uid, 'path': '/disk/top/middle/bottom/something'}
        self.json_error('mkdir', args)
        Bus().unset_lock(Address.Make(self.uid, '/disk/top').id)

        # ?????????? ???????????? ????????????, ???????????????? ??????-???? ???????????? ?????????????? - ??????????????????
        Bus().set_lock(Address.Make(self.uid, '/disk/top/middle/bottom').id)
        args = {'uid': self.uid, 'path': '/disk/top/middle/something'}
        self.json_ok('mkdir', args)
        Bus().unset_lock(Address.Make(self.uid, '/disk/top/middle/bottom').id)

    def test_share_lock_with_resource(self):
        """
        ?????????????????? ???????? ???? ???????????????? ????, ?????????? ???????????? ????????
        """
        opts = {'uid': self.uid, 'path': '/disk/folder/'}
        self.json_ok('mkdir', opts)

        # ???????????? ?????????????? - ???????????? ???? ?? ???????????????????? ???????? ??????????
        self.__create_shared_folder(self.uid, self.second_uid, '/disk/folder/shared')

        # ???? ????????, ???????? ????????????
        # ???????????? ??????????????????
        args = {'uid': self.uid, 'path': '/disk/folder/shared/top/'}
        self.json_ok('mkdir', args)
        args = {'uid': self.uid, 'path': '/disk/folder/shared/top/middle/'}
        self.json_ok('mkdir', args)
        args = {'uid': self.uid, 'path': '/disk/folder/shared/top/middle/bottom'}
        self.json_ok('mkdir', args)

        # ?????????? ???????????? ??????????????, ?????????????????? ??????-???? ???????????? ??????????
        Bus().set_lock(Address.Make(self.uid, '/disk/folder/shared/top/').id)
        args = {'uid': self.second_uid, 'src': '/disk/shared/top/middle/bottom', 'dst': '/disk/shared/top/middle_moved'}
        self.json_error('async_move', args)
        Bus().unset_lock(Address.Make(self.uid, '/disk/folder/shared/top/').id)

        # ?????????? ???????????? ??????????, ???????????????????? ??????-???? ???????????? ??????????????
        Bus().set_lock(Address.Make(self.uid, '/disk/folder/shared/top/middle/bottom').id)
        args = {'uid': self.second_uid, 'src': '/disk/shared/top/', 'dst': '/disk/shared/top_moved'}
        self.json_error('async_move', args)
        Bus().unset_lock(Address.Make(self.uid, '/disk/folder/shared/top/middle/bottom').id)

    def test_share_lock_with_address(self):
        """
        ?????????????????? ???????? ???? ???????????????? ????, ?????????? ?????????????? ??????
        """
        opts = {'uid': self.uid, 'path': '/disk/folder/'}
        self.json_ok('mkdir', opts)

        # ???????????? ?????????????? - ???????????? ???? ?? ???????????????????? ???????? ??????????
        self.__create_shared_folder(self.uid, self.second_uid, '/disk/folder/shared')

        # ???? ????????, ???????? ????????????
        # ???????????? ??????????????????
        args = {'uid': self.uid, 'path': '/disk/folder/shared/top/'}
        self.json_ok('mkdir', args)
        args = {'uid': self.uid, 'path': '/disk/folder/shared/top/middle/'}
        self.json_ok('mkdir', args)
        args = {'uid': self.uid, 'path': '/disk/folder/shared/top/middle/bottom'}
        self.json_ok('mkdir', args)

        # ?????????? ?????????????? ????????????, ???????????????? ??????-???? ???????????? ?????????? - ??????????????????
        Bus().set_lock(Address.Make(self.uid, '/disk/folder/shared/top').id)
        args = {'uid': self.second_uid, 'path': '/disk/shared/top/middle/bottom/something'}
        self.json_error('mkdir', args)
        Bus().unset_lock(Address.Make(self.uid, '/disk/folder/shared/top').id)

        # ?????????? ???????????? ????????????, ???????????????? ??????-???? ???????????? ?????????????? - ??????????????????
        Bus().set_lock(Address.Make(self.uid, '/disk/folder/shared/top/middle/bottom').id)
        args = {'uid': self.second_uid, 'path': '/disk/shared/top/middle/something'}
        self.json_ok('mkdir', args)
        Bus().unset_lock(Address.Make(self.uid, '/disk/folder/shared/top/middle/bottom').id)

    def _mocked_async_trash_append(self, path):
        with patch('mpfs.core.address.Address.add_trash_suffix'):
            self.json_ok('async_trash_append', {'uid': self.uid, 'path': path})

    def test_trash_locks(self):
        """
        ?????????????????? ?????????????????? ????????-????????

        ?????????? ????????:
        - ?????????? ???????????????????????? ?? ???????? ???????????? ?????????? ????????????????????????
        - ???????????? ???????????? ???????????? ?? ????????????, ???????? ?????????????? ????????????
        - ?????????? ???????????????????????? ?????????????????????????????? ???????????? ??????????
        """
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder/'})

        # ???????????? ?????? ???? ?????????????? - ?????? ?????????? ???? ????????????
        # ?????? ???????????????? ?? ???????????????? ?????? ???????? ??????????????????
        Bus().set_lock(Address.Make(self.uid, '/trash').id)
        self.json_error('async_trash_append', {'uid': self.uid, 'path': '/disk/folder/'}, code=105)
        self.json_error('async_trash_drop_all', {'uid': self.uid}, code=105)
        Bus().unset_lock(Address.Make(self.uid, '/trash').id)

        # ???????????? ?????? ???? ???????????????????? ???????????? ?? ?????????????? - ?????????? ???? ?? ?????? ??????-???? ????????????
        # ?????? ???????????????? ?? ???????????????? ?????? ???????? ??????????????????, ?????????? ????????????
        self._mocked_async_trash_append('/disk/folder/')
        Bus().set_lock(Address.Make(self.uid, '/trash/folder').id)
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder 2/'})
        self._mocked_async_trash_append('/disk/folder 2/')
        self.json_ok('async_trash_restore', {'uid': self.uid, 'path': '/trash/folder 2/'})
        self.json_error('trash_restore', {'uid': self.uid, 'path': '/trash/folder'}, code=105)
        self.json_error('trash_drop_all', {'uid': self.uid}, code=105)
        self.json_error('async_trash_restore', {'uid': self.uid, 'path': '/trash/folder'}, code=105)
        self.json_error('async_trash_drop_all', {'uid': self.uid}, code=105)
        Bus().unset_lock(Address.Make(self.uid, '/trash/folder').id)

        # ?????????????????? ?????? ???? ?????????????????????????????? ????????
        # ?????? ???????? ???????????????? ???????????????? ?????????? ??????????, ?????? ?? ?????????????? ??????????
        # ?????????????? ???????? ??????????????????
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/side/'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/top/'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/top/middle/'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/top/middle/bottom'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/top/middle/near bottom'})
        self._mocked_async_trash_append('/disk/top')

        # ?????? ???? ?????????????? ??????????????, ???????????? ???????????? ???????????? ??????????, ???? ?????????? ??????????
        # ????????????, ???????? ??????????, ????????????????????????
        Bus().set_lock(Address.Make(self.uid, '/trash/top').id)
        self._mocked_async_trash_append('/disk/side')
        self.json_error('trash_restore', {'uid': self.uid, 'path': '/trash/top'}, code=105)
        self.json_error('trash_restore', {'uid': self.uid, 'path': '/trash/top/middle'}, code=105)
        self.json_error('trash_restore', {'uid': self.uid, 'path': '/trash/top/middle/bottom'}, code=105)
        self.json_error('trash_drop_all', {'uid': self.uid}, code=105)
        self.json_error('async_trash_restore', {'uid': self.uid, 'path': '/trash/top'}, code=105)
        self.json_error('async_trash_restore', {'uid': self.uid, 'path': '/trash/top/middle'}, code=105)
        self.json_error('async_trash_restore', {'uid': self.uid, 'path': '/trash/top/middle/bottom'}, code=105)
        self.json_error('async_trash_drop_all', {'uid': self.uid}, code=105)
        Bus().unset_lock(Address.Make(self.uid, '/trash/top').id)

        # ?????? ???? ???????????? ??????????????, ???????????? ???????????? ???????????? ?? ??????, ???? ?????????? ??????????
        Bus().set_lock(Address.Make(self.uid, '/trash/top/middle/bottom').id)
        self.json_ok('async_trash_restore', {'uid': self.uid, 'path': '/trash/top/middle/near bottom'})
        self.json_error('trash_restore', {'uid': self.uid, 'path': '/trash/top/middle'}, code=105)
        self.json_error('trash_restore', {'uid': self.uid, 'path': '/trash/top/middle/bottom'}, code=105)
        self.json_error('trash_drop_all', {'uid': self.uid}, code=105)
        self.json_error('async_trash_restore', {'uid': self.uid, 'path': '/trash/top/middle'}, code=105)
        self.json_error('async_trash_restore', {'uid': self.uid, 'path': '/trash/top/middle/bottom'}, code=105)
        self.json_error('async_trash_drop_all', {'uid': self.uid}, code=105)
        Bus().unset_lock(Address.Make(self.uid, '/trash/top/middle/bottom').id)

    def test_update_lock(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder/'})

        # ???????????????? ?????????????? ???????????? ????????, ???????????????? ?????? ?????? - ?????? ????, ???????????? ???? ??????????????????
        Bus().update_lock(Address.Make(self.uid, '/disk/folder/somedir').id)
        locks = list(fs_locks.find({'uid': self.uid}))
        assert len(locks) == 0

        # ???????????? ?????? ???? ????????????, ?????????????????? ?????? ???? ??????????????????, ?????????? ?????????? ??????????
        Bus().set_lock(Address.Make(self.uid, '/disk/folder/').id)
        lock = fs_locks.find({'uid': self.uid})[0]
        old_lock_dtime = lock['dtime']

        # ??????????????????, ?????? ?????? ??????????
        self.json_error('mkdir', {'uid': self.uid, 'path': '/disk/folder/subfolder'}, code=105)

        # ???????????????? ??????, ??????????????????, ?????? ?????????? ????????????????????
        Bus().update_lock(Address.Make(self.uid, '/disk/folder/').id)
        lock = fs_locks.find({'uid': self.uid})[0]
        new_lock_dtime = lock['dtime']

        assert new_lock_dtime > old_lock_dtime

    def test_update_lock_while_long_operations(self):
        """
        ??????????????????, ?????? ???????? ???????????????????? ?????????????????????????? ???? ???????????? ??????????????????
        """
        # ?????????????? ??????????
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder/'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder/top'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder/top/middle'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder/top/middle/bottom'})

        # ???????????? ?? ??????????????????, ?????? ???????????? ????????????????????
        with patch.object(lock.LockUpdateTimer, 'start') as mocked:
            self.json_ok('async_move', {'uid': self.uid, 'src': '/disk/folder', 'dst': '/disk/moved'})
            self.json_ok('info', {'uid': self.uid, 'path': '/disk/moved'})
            assert mocked.call_count == 1

        # ????????????????
        with patch.object(lock.LockUpdateTimer, 'start') as mocked:
            self.json_ok('async_copy', {'uid': self.uid, 'src': '/disk/moved', 'dst': '/disk/copied'})
            self.json_ok('info', {'uid': self.uid, 'path': '/disk/copied'})
            assert mocked.call_count == 1

        # ????????????????
        with patch.object(lock.LockUpdateTimer, 'start') as mocked:
            with patch('mpfs.core.address.Address.add_trash_suffix'):
                self.json_ok('async_trash_append', {'uid': self.uid, 'path': '/disk/moved'})
            self.json_ok('info', {'uid': self.uid, 'path': '/trash/moved'})
            assert mocked.call_count == 1

        # ???????????? ??????????????
        with patch.object(lock.LockUpdateTimer, 'start') as mocked:
            self.json_ok('async_trash_drop_all', {'uid': self.uid})
            assert mocked.call_count == 1

class LockDetailsTestCase(CommonSharingMethods):
    def setup_method(self, method):
        super(LockDetailsTestCase, self).setup_method(method)
        self.owner_uid = self.uid
        self.invited_uid = self.uid_1
        for uid in (self.owner_uid,
                    self.invited_uid):
            self.json_ok('user_init', {'uid': uid})

        owner_shared_dir = '/disk/dirs_for_classmates/shared'
        self.resource_in_owner_shared = '/disk/dirs_for_classmates/shared/fat_raccoon'
        self.resource_in_invited_shared = '/disk/shared/fat_raccoon'
        for path in ('/disk/dirs_for_classmates',
                     owner_shared_dir,
                     self.resource_in_owner_shared):
            self.json_ok('mkdir', {'uid': self.uid, 'path': path})
        gid = self.create_group(path=owner_shared_dir)
        hsh = self.invite_user(uid=self.invited_uid, path=owner_shared_dir, owner=self.owner_uid,
                               email=self.email_1, ext_gid=gid)
        self.activate_invite(uid=self.invited_uid, hash=hsh)

    def test_lock_details_for_async_operation(self):
        """?????????????????? ???????????? ???????????? ????????.

        ???????????????? ?????????????????? ?????????????????????? ???????????????? ?????? ????????????????.
        ?????????????????? ?? ???????????????? ???????????????? ?????? ????????????????.
        ??????????????:
          0. ?????? ?????????????? ???????????????? ???????????????? ???????????? ResourceLocked ?? ???????????????? ?? ???????????????????? ???????????? ????????????????
          1. ?????????????????????? ???????????????? ?????????????????? ????????????????????
        """
        dir_path = '/disk/New Folder'
        new_dir_path = '/disk/pics'
        self.json_ok('mkdir', {'uid': self.uid, 'path': dir_path})

        self.result = {}

        def move_concurrently(*args, **kwargs):
            """???????????????????????? ???????????????? move.

            ???????????? ?????????????????????? ???? ?????? ?? ???????????????? ???????????? ?? ????????.
            """
            from mpfs.common.errors import ResourceLocked
            from mpfs.common.util import from_json
            _, response = self.do_request(method='move', opts={'uid': self.uid,
                                                               'src': dir_path,
                                                               'dst': new_dir_path})
            self.result = from_json(response)

        with before_after.before('mpfs.core.filesystem.base.Filesystem.base_copy', move_concurrently):
            self.json_ok('async_move', {'uid': self.uid,
                                        'src': dir_path,
                                        'dst': new_dir_path})

        assert_that(self.result, has_entries(code=codes.RESOURCE_LOCKED,
                                             title=is_not(empty()),
                                             data=has_entry('op_type', 'move_resource')))

        # ?????????????????????? ???????????????? ???????????? ?????????????????????? ??????????????
        self.json_ok('info', {'uid': self.uid, 'path': new_dir_path})

    @parameterized.expand([('move', {'src': '/disk/New Folder',
                                     'dst': '/disk/pics'},
                            'move_resource'),
                           ('copy', {'src': '/disk/New Folder',
                                     'dst': '/disk/pics'},
                            'copy_resource'),
                           ('trash_append', {'path': '/disk/New Folder'},
                            'trash_append'),
                           ('rm', {'path': '/disk/New Folder'},
                            'rm')])
    def test_lock_details_for_sync_operation(self, operation, opts, op_type):
        u"""?????????????????? ???????????? ???????????? ????????.

        ???????????????? ?????????????????? ???????????????????? ???????????????? ?????? ????????????????.
        ?????????????????? ?? ???????????????? ???????????????? ?????? ????????????????.
        ??????????????:
          0. ?????? ?????????????? ???????????????? ???????????????? ???????????? ResourceLocked ?? ???????????????? ?? ???????????????????? ???????????? ????????????????
          1. ???????????????????? ???????????????? ?????????????????? ????????????????????
        """
        dir_path = '/disk/New Folder'
        self.json_ok('mkdir', {'uid': self.uid, 'path': dir_path})
        opts['uid'] = self.uid

        self.result = {}

        def move_concurrently(*args, **kwargs):
            """???????????????????????? ???????????????? move.

            ???????????? ?????????????????????? ???? ?????? ?? ???????????????? ???????????? ?? ????????.
            """
            from mpfs.common.util import from_json
            _, response = self.do_request(method=operation, opts=opts)
            self.result = from_json(response)

        with before_after.after('mpfs.metastorage.mongo.collections.filesystem.FilesystemLocksCollection.set',
                                move_concurrently):
            self.json_ok(operation, opts)

        assert_that(self.result, has_entries(code=codes.RESOURCE_LOCKED,
                                             title=is_not(empty()),
                                             data=has_entry('op_type', op_type)))

    def test_locked_by_owner(self):
        self.result = {}

        def move_concurrently(*args, **kwargs):
            """???????????????????????? ???????????????? move.

            ???????????? ?????????????????????? ???? ?????? ?? ???????????????? ???????????? ?? ????????.
            """
            from mpfs.common.util import from_json
            _, response = self.do_request(method='move', opts={'uid': self.invited_uid,
                                                               'src': self.resource_in_invited_shared,
                                                               'dst': '/disk/stolen_fat_raccoon'})
            self.result = from_json(response)

        with before_after.after('mpfs.metastorage.mongo.collections.filesystem.FilesystemLocksCollection.set',
                                move_concurrently):
            self.json_ok('copy', {'uid': self.owner_uid,
                                  'src': self.resource_in_owner_shared,
                                  'dst': self.resource_in_owner_shared + '_2'})

        assert_that(self.result, has_entries(code=codes.RESOURCE_LOCKED,
                                             title=is_not(empty()),
                                             data=has_entries(op_type='copy_resource',
                                                              path=self.resource_in_invited_shared,
                                                              uid=self.owner_uid)))

    def test_locked_by_invited(self):
        self.result = {}

        def move_concurrently(*args, **kwargs):
            """???????????????????????? ???????????????? move.

            ???????????? ?????????????????????? ???? ?????? ?? ???????????????? ???????????? ?? ????????.
            """
            from mpfs.common.util import from_json
            _, response = self.do_request(method='move', opts={'uid': self.owner_uid,
                                                               'src': self.resource_in_owner_shared,
                                                               'dst': '/disk/stolen_fat_raccoon'})
            self.result = from_json(response)

        with before_after.after('mpfs.metastorage.mongo.collections.filesystem.FilesystemLocksCollection.set',
                                move_concurrently):
            self.json_ok('copy', {'uid': self.invited_uid,
                                  'src': self.resource_in_invited_shared,
                                  'dst': self.resource_in_invited_shared + '_2'})

        assert_that(self.result, has_entries(code=codes.RESOURCE_LOCKED,
                                             title=is_not(empty()),
                                             data=has_entries(op_type='copy_resource',
                                                              path=self.resource_in_owner_shared,
                                                              uid=self.owner_uid)))


@pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='Postgres lock expiration tests')
class PostgresFilesystemLocksTestCase(CommonFilesystemTestCase):
    def test_expire_locks(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder'})

        with patch.object(FilesystemLocksCollection, 'TTL', 100000):
            Bus().set_lock(Address.Make(self.uid, '/disk/folder/').id, time_offset=3601)
        lock = fs_locks.find({'uid': self.uid})[0]
        assert lock is not None

        # ??????????????????, ?????? ?????? ??????????
        self.json_error('mkdir', {'uid': self.uid, 'path': '/disk/folder'}, code=codes.RESOURCE_LOCKED)

        delete_expired_entries()

        lock = fs_locks.find({'uid': self.uid})[0]
        assert lock is None

    def test_multiple_locks(self):
        folders_name_with_lock_offset = {
            '/disk/folder1': 3601,
            '/disk/folder2': 3601,
            '/disk/folder3': 0,
        }

        for path in folders_name_with_lock_offset.keys():
            self.json_ok('mkdir', {'uid': self.uid, 'path': path})

        with patch.object(FilesystemLocksCollection, 'TTL', 100000):
            for path, time_offset in folders_name_with_lock_offset.iteritems():
                Bus().set_lock(Address.Make(self.uid, path + '/').id, time_offset=time_offset)
        locks_count = fs_locks.find({'uid': self.uid}).count()
        assert locks_count == len(folders_name_with_lock_offset)

        # ??????????????????, ?????? ?????? ??????????
        for path in folders_name_with_lock_offset.keys():
            self.json_error('mkdir', {'uid': self.uid, 'path': path}, code=codes.RESOURCE_LOCKED)

        delete_expired_entries()

        locks_count = fs_locks.find({'uid': self.uid}).count()
        assert locks_count == 1

    def test_batch_delete_locks(self):
        folders = (
            '/disk/folder1',
            '/disk/folder2',
            '/disk/folder3',
        )

        for path in folders:
            self.json_ok('mkdir', {'uid': self.uid, 'path': path})

        with patch.object(FilesystemLocksCollection, 'TTL', 100000):
            for path in folders:
                Bus().set_lock(Address.Make(self.uid, path + '/').id, time_offset=3601)
        locks_count = fs_locks.find({'uid': self.uid}).count()
        assert locks_count == len(folders)

        # ??????????????????, ?????? ?????? ??????????
        for path in folders:
            self.json_error('mkdir', {'uid': self.uid, 'path': path}, code=codes.RESOURCE_LOCKED)

        with patch.object(ttl, 'POSTGRES_TTL_DELETE_BATCH_SIZE', 2):
            delete_expired_entries()

        locks_count = fs_locks.find({'uid': self.uid}).count()
        assert locks_count == 0
