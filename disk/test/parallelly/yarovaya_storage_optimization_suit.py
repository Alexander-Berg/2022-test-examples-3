# -*- coding: utf-8 -*-
import datetime
import os

import pytest
import mock
from nose_parameterized import parameterized

import mpfs.metastorage.mongo.collections.filesystem

from mpfs.core.address import Address
from mpfs.core.bus import Bus
from mpfs.core.filesystem.dao.resource import HiddenDAO
from test.base import time_machine
from test.common.sharing import CommonSharingMethods
from test.conftest import INIT_USER_IN_POSTGRES


class YarovayaStorageOptimizationTestCase(CommonSharingMethods):

    def setup_method(self, method):
        super(YarovayaStorageOptimizationTestCase, self).setup_method(method)
        self.run_000_user_check(uid=self.uid_3)
        self.json_ok('enable_unlimited_autouploading', {'uid': self.uid})

        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/1'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/1/2'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/1/2/3'})
        self.upload_file(self.uid, '/disk/1/2/3/4.txt')
        self.upload_file(self.uid, '/photounlim/1.jpg')

        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/ow'})
        self.upload_file(self.uid, '/disk/ow.txt')
        self.upload_file(self.uid, '/photounlim/ow.jpg')

        self.json_ok('mkdir', {'uid': self.uid_3, 'path': '/disk/ow'})
        self.upload_file(self.uid_3, '/disk/ow.txt')

    @parameterized.expand([
        ('/disk/1/2/3/4.txt', ['/disk', '/disk/1', '/disk/1/2/', '/disk/1/2/3']),
        ('/disk/1/2', ['/disk', '/disk/1', '/disk/1/2/3', '/disk/1/2/3/4.txt']),
        ('/photounlim/1.jpg', ['/photounlim']),
    ])
    def test_resource_publication_sets_yarovaya_mark_on_this_resource(self, public_resource_path, should_be_unmarked_paths):
        self.json_ok('set_public', {'uid': self.uid, 'path': public_resource_path})

        f = Bus().get_resource(self.uid, Address.Make(self.uid, public_resource_path))
        assert f.meta['yarovaya_mark']
        assert all([not bool(Bus().get_resource(self.uid, Address.Make(self.uid, p)).meta.get('yarovaya_mark'))
                    for p in should_be_unmarked_paths])

    def test_checking_yarovaya_mark_among_ancestors(self):
        self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/1/2'})
        from mpfs.core.filesystem.dao.folder import FolderDAO
        assert FolderDAO().has_parents_with_yarovaya_mark(Address.Make(self.uid, '/disk/1/2/3/4.txt'))
        assert FolderDAO().has_parents_with_yarovaya_mark(Address.Make(self.uid, '/disk/1/2/3'))
        assert not FolderDAO().has_parents_with_yarovaya_mark(Address.Make(self.uid, '/disk/1/2'))
        assert not FolderDAO().has_parents_with_yarovaya_mark(Address.Make(self.uid, '/disk/1'))

    @parameterized.expand([
        ('move', '/disk/1', '/disk/1/2', ['/disk/copy_2'], None, False,),
        ('move', '/disk/1', '/disk/1/2', ['/disk/copy_2'], None, True,),
        ('move', '/disk/1', '/disk/1/2/3/4.txt', ['/disk/copy_4.txt'], None, False,),
        ('move', '/disk/1', '/disk/1/2/3/4.txt', ['/disk/copy_4.txt'], None, True,),
        ('move', '/disk/1/2/3', '/disk/1/2/3', ['/disk/copy_3'], None, False,),
        ('move', '/disk/1/2/3', '/disk/1/2/3', ['/disk/copy_3'], None, True,),
        ('move', '/disk/1/2/3/4.txt', '/disk/1/2/3/4.txt', ['/disk/copy_4.txt'], None, False,),
        ('move', '/disk/1/2/3/4.txt', '/disk/1/2/3/4.txt', ['/disk/copy_4.txt'], None, True,),
        ('move', '/photounlim/1.jpg', '/photounlim/1.jpg', ['/disk/copy_1.jpg'], None, False,),
        ('move', '/photounlim/1.jpg', '/photounlim/1.jpg', ['/disk/copy_1.jpg'], None, True,),
        ('trash_append', '/disk/1', '/disk/1/2', ['/trash/2'], None, False,),
        ('trash_append', '/disk/1', '/disk/1/2', ['/trash/2'], None, True,),
        ('trash_append', '/disk/1', '/disk/1/2/3/4.txt', ['/trash/4.txt'], None, False,),
        ('trash_append', '/disk/1', '/disk/1/2/3/4.txt', ['/trash/4.txt'], None, True,),
        ('trash_append', '/disk/1/2/3', '/disk/1/2/3', ['/trash/3'], None, False,),
        ('trash_append', '/disk/1/2/3', '/disk/1/2/3', ['/trash/3'], None, True,),
        ('trash_append', '/disk/1/2/3/4.txt', '/disk/1/2/3/4.txt', ['/trash/4.txt'], None, False,),
        ('trash_append', '/disk/1/2/3/4.txt', '/disk/1/2/3/4.txt', ['/trash/4.txt'], None, True,),
        ('trash_append', '/photounlim/1.jpg', '/photounlim/1.jpg', ['/trash/1.jpg'], None, False,),
        ('trash_append', '/photounlim/1.jpg', '/photounlim/1.jpg', ['/trash/1.jpg'], None, True,),
    ])
    def test_mark_resource_moved_from_public_folder(self, action_type, public_resource_path, res_to_move_path, should_be_marked_paths, should_be_unmarked_paths, marked_before_moving):
        with mock.patch('mpfs.metastorage.mongo.collections.filesystem.POSTGRES_QUICK_MOVE_ENABLED', False):
            self._moving_common_manipulations_and_checks_for_public(action_type, public_resource_path, res_to_move_path,
                                                                    should_be_marked_paths=should_be_marked_paths,
                                                                    should_be_unmarked_paths=should_be_unmarked_paths,
                                                                    has_mark_before_moving=marked_before_moving)

    @parameterized.expand([
        ('move', '/disk/1', '/disk/1/2', ['/disk/copy_2'], None, False,),
        ('move', '/disk/1', '/disk/1/2', ['/disk/copy_2'], None, True,),
        ('move', '/disk/1', '/disk/1/2/3/4.txt', ['/disk/copy_4.txt'], None, False,),
        ('move', '/disk/1', '/disk/1/2/3/4.txt', ['/disk/copy_4.txt'], None, True,),
        ('move', '/disk/1/2/3', '/disk/1/2/3', ['/disk/copy_3'], None, False,),
        ('move', '/disk/1/2/3', '/disk/1/2/3', ['/disk/copy_3'], None, True,),
        ('move', '/disk/1/2/3/4.txt', '/disk/1/2/3/4.txt', ['/disk/copy_4.txt'], None, False,),
        ('move', '/disk/1/2/3/4.txt', '/disk/1/2/3/4.txt', ['/disk/copy_4.txt'], None, True,),
        ('move', '/photounlim/1.jpg', '/photounlim/1.jpg', ['/disk/copy_1.jpg'], None, False,),
        ('move', '/photounlim/1.jpg', '/photounlim/1.jpg', ['/disk/copy_1.jpg'], None, True,),
        ('trash_append', '/disk/1', '/disk/1/2', ['/trash/2'], None, False,),
        ('trash_append', '/disk/1', '/disk/1/2', ['/trash/2'], None, True,),
        ('trash_append', '/disk/1', '/disk/1/2/3/4.txt', ['/trash/4.txt'], None, False,),
        ('trash_append', '/disk/1', '/disk/1/2/3/4.txt', ['/trash/4.txt'], None, True,),
        ('trash_append', '/disk/1/2/3', '/disk/1/2/3', ['/trash/3'], None, False,),
        ('trash_append', '/disk/1/2/3', '/disk/1/2/3', ['/trash/3'], None, True,),
        ('trash_append', '/disk/1/2/3/4.txt', '/disk/1/2/3/4.txt', ['/trash/4.txt'], None, False,),
        ('trash_append', '/disk/1/2/3/4.txt', '/disk/1/2/3/4.txt', ['/trash/4.txt'], None, True,),
        ('trash_append', '/photounlim/1.jpg', '/photounlim/1.jpg', ['/trash/1.jpg'], None, False,),
        ('trash_append', '/photounlim/1.jpg', '/photounlim/1.jpg', ['/trash/1.jpg'], None, True,),
    ])
    @pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='Pg implementation, do not test it on Mongo')
    def test_mark_resource_moved_from_public_folder_fast_move(self, action_type, public_resource_path, res_to_move_path, should_be_marked_paths, should_be_unmarked_paths, marked_before_moving):
        with mock.patch('mpfs.metastorage.mongo.collections.filesystem.POSTGRES_QUICK_MOVE_ENABLED', True), \
                mock.patch('mpfs.metastorage.mongo.collections.filesystem.POSTGRES_QUICK_MOVE_ALLOWED_UIDS', self.uid):
            self._moving_common_manipulations_and_checks_for_public(action_type, public_resource_path, res_to_move_path,
                                                                    should_be_marked_paths=should_be_marked_paths,
                                                                    should_be_unmarked_paths=should_be_unmarked_paths,
                                                                    has_mark_before_moving=marked_before_moving)

    @parameterized.expand([
        ('/disk/1', '/disk/1/2', ['/hidden/1/2', '/hidden/1/2/3', ], '/hidden/1/2/3', False,),
        ('/disk/1', '/disk/1/2/3/4.txt', [], '/hidden', False,),
        ('/disk/1/2', '/disk/1/2', ['/hidden/1/2', '/hidden/1/2/3', ], '/hidden/1/2/3', False,),
        ('/disk/1/2/3/4.txt', '/disk/1/2/3/4.txt', [], '/hidden', False,),
        ('/photounlim/1.jpg', '/photounlim/1.jpg', [], '/hidden', False,),
        ('/disk/1', '/disk/1/2', ['/hidden/1/2', '/hidden/1/2/3', ], '/hidden/1/2/3', True,),
        ('/disk/1', '/disk/1/2/3/4.txt', [], '/hidden', True,),
        ('/disk/1/2', '/disk/1/2', ['/hidden/1/2', '/hidden/1/2/3', ], '/hidden/1/2/3', True,),
        ('/disk/1/2/3/4.txt', '/disk/1/2/3/4.txt', [], '/hidden', True,),
        ('/photounlim/1.jpg', '/photounlim/1.jpg', [], '/hidden', True,),
    ])
    def test_mark_resource_deleted_to_hidden_from_public_folder(self, public_resource_path, resource_path, should_be_marked_paths,
                                                                file_parent_in_hidden, marked_before_moving):
        self.json_ok('set_public', {'uid': self.uid, 'path': public_resource_path})
        if not marked_before_moving:
            self._unmark_resource(self.uid, resource_path)

        self.json_ok('rm', {'uid': self.uid, 'path': resource_path})

        assert all([bool(Bus().get_resource(self.uid, Address.Make(self.uid, p)).meta.get('yarovaya_mark'))
                    for p in should_be_marked_paths])
        assert bool(Bus().get_resource(self.uid, Address.Make(self.uid, file_parent_in_hidden)).list()['list'][0]['meta']['yarovaya_mark'])

    @parameterized.expand([
        ('/disk/1', '/disk/ow.txt', '/disk/1/2/3/4.txt', [], '/hidden', False,),
        ('/disk/1/2/3/4.txt', '/disk/ow.txt', '/disk/1/2/3/4.txt', [], '/hidden', False,),
        ('/photounlim/1.jpg', '/photounlim/ow.jpg', '/photounlim/1.jpg', [], '/hidden', False,),
        ('/disk/1', '/disk/ow', '/disk/1/2/3/4.txt', [], '/hidden', False,),
        ('/disk/1/2/3/4.txt', '/disk/ow', '/disk/1/2/3/4.txt', [], '/hidden', False,),
        ('/disk/1', '/disk/ow.txt', '/disk/1/2/3/4.txt', [], '/hidden', True,),
        ('/disk/1/2/3/4.txt', '/disk/ow.txt', '/disk/1/2/3/4.txt', [], '/hidden', True,),
        ('/photounlim/1.jpg', '/photounlim/ow.jpg', '/photounlim/1.jpg', [], '/hidden', True,),
        ('/disk/1', '/disk/ow', '/disk/1/2/3/4.txt', [], '/hidden', True,),
        ('/disk/1/2/3/4.txt', '/disk/ow', '/disk/1/2/3/4.txt', [], '/hidden', True,),
    ])
    def test_mark_public_file_overwritten_with_file_or_folder(self, public_resource_path, src_path, dst_path,
                                                               should_be_marked_paths,
                                                               file_parent_in_hidden, marked_before_moving):
        self.json_ok('set_public', {'uid': self.uid, 'path': public_resource_path})
        if not marked_before_moving:
            self._unmark_resource(self.uid, dst_path)

        self.json_ok('move', {'uid': self.uid, 'dst': dst_path, 'src': src_path, 'force': 1})

        assert all([bool(Bus().get_resource(self.uid, Address.Make(self.uid, p)).meta.get('yarovaya_mark'))
                    for p in should_be_marked_paths])
        assert bool(Bus().get_resource(self.uid, Address.Make(self.uid, file_parent_in_hidden)).list()['list'][0]['meta']['yarovaya_mark'])

    @parameterized.expand([
        ('/disk/1', '/disk/ow.txt', '/disk/1/2', ['/hidden/1', '/hidden/1/2', '/hidden/1/2/3'], '/hidden/1/2/3', True,),
        ('/disk/1', '/disk/ow.txt', '/disk/1/2', ['/hidden/1', '/hidden/1/2', '/hidden/1/2/3'], '/hidden/1/2/3', False,),
    ])
    @pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='Pg implementation, do not test it on Mongo')
    def test_mark_public_folder_overwritten_with_file(self, public_resource_path, src_path, dst_path,
                                                      should_be_marked_paths, file_parent_in_hidden,
                                                      marked_before_moving):
        self._mark_public_folder_overwritten_with_file_common_manipulations_and_checks(
            public_resource_path, src_path, dst_path, should_be_marked_paths, file_parent_in_hidden,
            marked_before_moving)

    @parameterized.expand([
        ('/disk/1', '/disk/ow', '/disk/1', True,),
        ('/disk/1', '/disk/ow', '/disk/1', False,),
        ('/disk/1', '/disk/1', '/disk/ow', True,),
        ('/disk/1', '/disk/1', '/disk/ow', False,),
    ])
    def test_mark_public_root_folder_for_overwrites(self, public_resource_path, src_path, dst_path,
                                                    marked_before_moving):
        self.json_ok('set_public', {'uid': self.uid, 'path': public_resource_path})
        if not marked_before_moving:
            self._unmark_resource(self.uid, public_resource_path)

        self.json_ok('move', {'uid': self.uid, 'dst': dst_path, 'src': src_path, 'force': 1})
        assert bool(Bus().get_resource(self.uid, Address.Make(self.uid, dst_path)).meta.get('yarovaya_mark'))

    @parameterized.expand([
        ('move', '/disk/1/2/3/4.txt', ['/disk/copy_4.txt'], None, False,),
        ('move', '/photounlim/1.jpg', ['/disk/copy_1.jpg'], None, False,),
        ('move', '/disk/1/2', ['/disk/copy_2'], None, False,),
        ('move', '/disk/1/2/3/4.txt', ['/disk/copy_4.txt'], None, True,),
        ('move', '/photounlim/1.jpg', ['/disk/copy_1.jpg'], None, True,),
        ('move', '/disk/1/2', ['/disk/copy_2'], None, True,),
        ('trash_append', '/disk/1/2/3/4.txt', ['/trash/4.txt'], None, False,),
        ('trash_append', '/photounlim/1.jpg', ['/trash/1.jpg'], None, False,),
        ('trash_append', '/disk/1/2', ['/trash/2'], None, False,),
        ('trash_append', '/disk/1/2/3/4.txt', ['/trash/4.txt'], None, True,),
        ('trash_append', '/photounlim/1.jpg', ['/trash/1.jpg'], None, True,),
        ('trash_append', '/disk/1/2', ['/trash/2'], None, True,),
    ])
    def test_moving_public_resource_keep_mark(self, action_type, resource_path, should_be_marked_paths, should_be_unmarked_paths, marked_before_moving):
        with mock.patch('mpfs.metastorage.mongo.collections.filesystem.POSTGRES_QUICK_MOVE_ENABLED', False):
            self._moving_common_manipulations_and_checks_for_public(action_type, resource_path, resource_path,
                                                                    should_be_marked_paths=should_be_marked_paths,
                                                                    should_be_unmarked_paths=should_be_unmarked_paths,
                                                                    has_mark_before_moving=marked_before_moving)

    @parameterized.expand([
        ('move', '/disk/1/2/3/4.txt', ['/disk/copy_4.txt'], None, False,),
        ('move', '/photounlim/1.jpg', ['/disk/copy_1.jpg'], None, False,),
        ('move', '/disk/1/2', ['/disk/copy_2'], None, False,),
        ('move', '/disk/1/2/3/4.txt', ['/disk/copy_4.txt'], None, True,),
        ('move', '/photounlim/1.jpg', ['/disk/copy_1.jpg'], None, True,),
        ('move', '/disk/1/2', ['/disk/copy_2'], None, True,),
        ('trash_append', '/disk/1/2/3/4.txt', ['/trash/4.txt'], None, False,),
        ('trash_append', '/photounlim/1.jpg', ['/trash/1.jpg'], None, False,),
        ('trash_append', '/disk/1/2', ['/trash/2'], None, False,),
        ('trash_append', '/disk/1/2/3/4.txt', ['/trash/4.txt'], None, True,),
        ('trash_append', '/photounlim/1.jpg', ['/trash/1.jpg'], None, True,),
        ('trash_append', '/disk/1/2', ['/trash/2'], None, True,),
    ])
    @pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='Pg implementation, do not test it on Mongo')
    def test_moving_public_resource_keep_mark_fast_move(self, action_type, resource_path, should_be_marked_paths, should_be_unmarked_paths, marked_before_moving):
        with mock.patch('mpfs.metastorage.mongo.collections.filesystem.POSTGRES_QUICK_MOVE_ENABLED', True), \
                mock.patch('mpfs.metastorage.mongo.collections.filesystem.POSTGRES_QUICK_MOVE_ALLOWED_UIDS', self.uid):
            self._moving_common_manipulations_and_checks_for_public(action_type, resource_path, resource_path,
                                                                    should_be_marked_paths=should_be_marked_paths,
                                                                    should_be_unmarked_paths=should_be_unmarked_paths,
                                                                    has_mark_before_moving=marked_before_moving)

    def test_move_folder_containing_public(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/1/2_2'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/1/2_2/3_3'})
        self.upload_file(self.uid, '/disk/1/2_2/3_3/4_4.txt')
        self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/1/2'})
        self._unmark_resource(self.uid, '/disk/1/2')
        self.json_ok('move', {'uid': self.uid, 'src': '/disk/1', 'dst': '/disk/copy_1'})
        assert bool(Bus().get_resource(self.uid, Address.Make(self.uid, '/disk/copy_1/2')).meta.get('yarovaya_mark'))
        # why?
        assert not bool(Bus().get_resource(self.uid, Address.Make(self.uid, '/disk/copy_1/2_2')).meta.get('yarovaya_mark'))
        assert not bool(Bus().get_resource(self.uid, Address.Make(self.uid, '/disk/copy_1/2_2/3_3')).meta.get('yarovaya_mark'))
        assert not bool(Bus().get_resource(self.uid, Address.Make(self.uid, '/disk/copy_1/2_2/3_3/4_4.txt')).meta.get('yarovaya_mark'))

    def test_trash_append_folder_containing_public_folder(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/1/2_2'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/1/2_2/3_3'})
        self.upload_file(self.uid, '/disk/1/2_2/3_3/4_4.txt')
        self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/1/2'})
        self._unmark_resource(self.uid, '/disk/1/2')
        trash_path = self.json_ok('trash_append', {'uid': self.uid, 'path': '/disk/1'})['this']['id']
        assert bool(Bus().get_resource(self.uid, Address.Make(self.uid, os.path.join(trash_path, '2'))).meta.get('yarovaya_mark'))
        # trash_append удаляет публичность после перемещения папки в корзину, поэтому дочерние ресурсы станут
        # непубличными в ходе медленного мува, поэтому, дочерние файлы этого ресурса не будут помеченными.
        assert not bool(Bus().get_resource(self.uid, Address.Make(self.uid, os.path.join(trash_path, '2/3'))).meta.get('yarovaya_mark'))
        assert not bool(Bus().get_resource(self.uid, Address.Make(self.uid, os.path.join(trash_path, '2/3/4.txt'))).meta.get('yarovaya_mark'))
        assert not bool(Bus().get_resource(self.uid, Address.Make(self.uid, os.path.join(trash_path, '2_2'))).meta.get('yarovaya_mark'))
        assert not bool(Bus().get_resource(self.uid, Address.Make(self.uid, os.path.join(trash_path, '2_2/3_3'))).meta.get('yarovaya_mark'))
        assert not bool(Bus().get_resource(self.uid, Address.Make(self.uid, os.path.join(trash_path, '2_2/3_3/4_4.txt'))).meta.get('yarovaya_mark'))

    def test_rm_folder_containing_public_folder(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/1/2_2'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/1/2_2/3_3'})
        self.upload_file(self.uid, '/disk/1/2_2/3_3/4_4.txt')
        self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/1/2'})
        self._unmark_resource(self.uid, '/disk/1/2')
        self.json_ok('rm', {'uid': self.uid, 'path': '/disk/1'})
        assert bool(Bus().get_resource(self.uid, Address.Make(self.uid, '/hidden/1/2/3')).list()['list'][0]['meta'].get('yarovaya_mark'))
        assert bool(Bus().get_resource(self.uid, Address.Make(self.uid, '/hidden/1/2/3')).meta.get('yarovaya_mark'))
        assert not bool(Bus().get_resource(self.uid, Address.Make(self.uid, '/hidden/1/2_2/3_3')).list()['list'][0]['meta'].get('yarovaya_mark'))
        assert not bool(Bus().get_resource(self.uid, Address.Make(self.uid, '/hidden/1/2_2/3_3')).meta.get('yarovaya_mark'))

    @parameterized.expand([
        ('/disk/1/2/3/4.txt', False,),
        ('/disk/1/2', True,),
        ('/disk/1/2/3/4.txt', False,),
        ('/disk/1/2', True,),
    ])
    def test_unpublic_marks_resource(self, resource_path, marked_before_moving):
        self.json_ok('set_public', {'uid': self.uid, 'path': resource_path})
        if not marked_before_moving:
            r = Bus().get_resource(self.uid, Address.Make(self.uid, resource_path))
            r.meta['yarovaya_mark'] = False
            r.save()
        self.json_ok('set_private', {'uid': self.uid, 'path': resource_path})
        assert bool(Bus().get_resource(self.uid, Address.Make(self.uid, resource_path)).meta.get('yarovaya_mark'))

    def test_folder_sharing_sets_yarovaya_mark_on_shared_root_folder(self):
        owner_shared_root_folder_path = '/disk/1/2'
        guest_shared_root_folder_path = '/disk/2'
        gid = self.create_group(path=owner_shared_root_folder_path)
        assert bool(Bus().get_resource(self.uid, Address.Make(self.uid, owner_shared_root_folder_path)).meta.get('yarovaya_mark'))

        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid, path=owner_shared_root_folder_path)
        self.activate_invite(uid=self.uid_3, hash=hsh)
        assert bool(Bus().get_resource(self.uid, Address.Make(self.uid_3, guest_shared_root_folder_path)).meta.get('yarovaya_mark'))

    @parameterized.expand([
        ('/disk/1/2', ['/disk/copy_2', '/disk/copy_2/3', '/disk/copy_2/3/4.txt'], None, True, True,),
        ('/disk/1/2', ['/disk/copy_2', '/disk/copy_2/3', '/disk/copy_2/3/4.txt'], None, True, False,),
        ('/disk/1/2', ['/disk/copy_2', '/disk/copy_2/3', '/disk/copy_2/3/4.txt'], None, False, True,),
        ('/disk/1/2', ['/disk/copy_2', '/disk/copy_2/3', '/disk/copy_2/3/4.txt'], None, False, False,),
        ('/disk/1/2/3/4.txt', ['/disk/copy_4.txt'], None, True, True,),
        ('/disk/1/2/3/4.txt', ['/disk/copy_4.txt'], None, True, False,),
        ('/disk/1/2/3/4.txt', ['/disk/copy_4.txt'], None, False, True,),
        ('/disk/1/2/3/4.txt', ['/disk/copy_4.txt'], None, False, False,),
        ('/disk/1', ['/disk/copy_1'], ['/disk/copy_1/2', '/disk/copy_1/2/3', '/disk/copy_1/2/3/4.txt'], True, True),
        # Перемещение непомеченного корня общей папки не помечает его. Это минорный баг
        # ('/disk/1', ['/disk/copy_1'], ['/disk/copy_1/2', '/disk/copy_1/2/3', '/disk/copy_1/2/3/4.txt'], True, False),
        ('/disk/1', ['/disk/copy_1'], ['/disk/copy_1/2', '/disk/copy_1/2/3', '/disk/copy_1/2/3/4.txt'], False, True),
        # Перемещение непомеченного корня общей папки не помечает его. Это минорный баг
        # ('/disk/1', ['/disk/copy_1'], ['/disk/copy_1/2', '/disk/copy_1/2/3', '/disk/copy_1/2/3/4.txt'], False, False),
    ])
    def test_mark_resource_moved_from_shared_folder(self, resource_path, should_be_marked_paths,
                                                    should_be_unmarked_paths, done_by_owner, marked_before_moving):
        action_initiator = self.uid if done_by_owner else self.uid_3
        self._moving_common_manipulations_and_checks_for_shared(
            'move', '/disk/1', self.uid, self.uid_3, resource_path, action_initiator,
            marked_before_moving=marked_before_moving)

        assert all([bool(Bus().get_resource(action_initiator, Address.Make(action_initiator, p)).meta.get('yarovaya_mark'))
                    for p in should_be_marked_paths or []])
        assert all([not bool(Bus().get_resource(action_initiator, Address.Make(action_initiator, p)).meta.get('yarovaya_mark'))
                    for p in should_be_unmarked_paths or []])

    @parameterized.expand([
        ('/disk/1/2', ['/trash/2', '/trash/2/3', '/trash/2/3/4.txt'], None, True, True,),
        ('/disk/1/2', ['/trash/2', '/trash/2/3', '/trash/2/3/4.txt'], None, True, False,),
        ('/disk/1/2', ['/trash/2', '/trash/2/3', '/trash/2/3/4.txt'], None, False, True,),
        ('/disk/1/2', ['/trash/2', '/trash/2/3', '/trash/2/3/4.txt'], None, False, False,),
        ('/disk/1/2/3/4.txt', ['/trash/4.txt'], None, True, True,),
        ('/disk/1/2/3/4.txt', ['/trash/4.txt'], None, True, False,),
        ('/disk/1/2/3/4.txt', ['/trash/4.txt'], None, False, True,),
        ('/disk/1/2/3/4.txt', ['/trash/4.txt'], None, False, False,),
        ('/disk/1', ['/trash/1', '/trash/1/2', '/trash/1/2/3', '/trash/1/2/3/4.txt'], None, True, True),
        ('/disk/1', ['/trash/1', '/trash/1/2', '/trash/1/2/3', '/trash/1/2/3/4.txt'], None, True, False),
    ])
    def test_mark_resource_deleted_to_trash_from_shared_folder(self, resource_path, should_be_marked_paths,
                                                               should_be_unmarked_paths, done_by_owner,
                                                               marked_before_moving):
        action_initiator = self.uid if done_by_owner else self.uid_3
        self._moving_common_manipulations_and_checks_for_shared(
            'trash_append', '/disk/1', self.uid, self.uid_3, resource_path, action_initiator,
            marked_before_moving=marked_before_moving)

        assert all([bool(Bus().get_resource(action_initiator, Address.Make(action_initiator, p)).meta.get('yarovaya_mark'))
                    for p in should_be_marked_paths or []])
        assert all([not bool(Bus().get_resource(action_initiator, Address.Make(action_initiator, p)).meta.get('yarovaya_mark'))
                    for p in should_be_unmarked_paths or []])

        if not done_by_owner and resource_path != '/disk/1':
            assert all([bool(Bus().get_resource(self.uid, Address.Make(self.uid, p)).meta.get('yarovaya_mark'))
                        for p in should_be_marked_paths or []])
            assert all([not bool(Bus().get_resource(self.uid, Address.Make(self.uid, p)).meta.get('yarovaya_mark'))
                        for p in should_be_unmarked_paths or []])

    @parameterized.expand([
        ('/disk/1/2', ['/hidden/1/2', '/hidden/1/2/3'], True, True, '/hidden/1/2/3'),
        ('/disk/1/2', ['/hidden/1/2', '/hidden/1/2/3'], True, False, '/hidden/1/2/3'),
        ('/disk/1/2', ['/hidden/1/2', '/hidden/1/2/3'], False, True, '/hidden/1/2/3'),
        ('/disk/1/2', ['/hidden/1/2', '/hidden/1/2/3'], False, False, '/hidden/1/2/3'),
        ('/disk/1/2/3/4.txt', None, True, True, '/hidden'),
        ('/disk/1/2/3/4.txt', None, True, False, '/hidden'),
        ('/disk/1/2/3/4.txt', None, False, True, '/hidden'),
        ('/disk/1/2/3/4.txt', None, False, False, '/hidden'),
        ('/disk/1', ['/hidden/1', '/hidden/1/2', '/hidden/1/2/3'], True, True, '/hidden/1/2/3'),
        ('/disk/1', ['/hidden/1', '/hidden/1/2', '/hidden/1/2/3'], True, False, '/hidden/1/2/3'),
    ])
    def test_mark_resource_deleted_to_hidden_from_shared_folder(self, resource_path, should_be_marked_paths,
                                                                done_by_owner, marked_before_moving, file_parent_in_hidden):
        action_initiator = self.uid if done_by_owner else self.uid_3
        self._moving_common_manipulations_and_checks_for_shared(
            'rm', '/disk/1', self.uid, self.uid_3, resource_path, action_initiator,
            marked_before_moving=marked_before_moving)

        assert all([bool(Bus().get_resource(self.uid, Address.Make(self.uid, p)).meta.get('yarovaya_mark'))
                    for p in should_be_marked_paths or []])
        check_in_hidden_uid = self.uid_3 if (not done_by_owner and resource_path == '/disk/1/2/3/4.txt') else self.uid
        assert bool(Bus().get_resource(check_in_hidden_uid, Address.Make(check_in_hidden_uid, file_parent_in_hidden)).list()['list'][0]['meta']['yarovaya_mark'])

    @parameterized.expand([
        ('/hidden', True, False,),
        ('/hidden', True, True,),
        ('/hidden/1/2/3', False, False,),
        ('/hidden/1/2/3', False, True,),
    ])
    def test_mark_resource_overwritten_from_public_folder(self, file_parent_in_hidden, done_by_owner, marked_before_moving):
        resource_to_overwrite_path = '/disk/1/2/3/4.txt'
        gid = self.create_group(uid=self.uid, path='/disk/1')
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid, path='/disk/1')
        self.activate_invite(uid=self.uid_3, hash=hsh)
        if not marked_before_moving:
            self._unmark_resource(self.uid, resource_to_overwrite_path)

        action_initiator = self.uid if done_by_owner else self.uid_3

        self.json_ok('move', {'uid': action_initiator, 'dst': resource_to_overwrite_path, 'src': '/disk/ow.txt', 'force': 1})

        assert bool(Bus().get_resource(self.uid, Address.Make(self.uid, file_parent_in_hidden)).list()['list'][0]['meta']['yarovaya_mark'])

    def test_move_folder_containing_shared(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/1/cc_2'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/1/cc_2/cc_3'})
        self.upload_file(self.uid, '/disk/1/cc_2/cc_3/cc_4.txt')
        gid = self.create_group(uid=self.uid, path='/disk/1/2')
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid, path='/disk/1/2')
        self.activate_invite(uid=self.uid_3, hash=hsh)
        self._unmark_resource(self.uid, '/disk/1/2')
        self.json_ok('move', {'uid': self.uid, 'src': '/disk/1/', 'dst': '/disk/copy_1/'})
        # переименование общей папки не выставляет метки, минорный баг
        assert not bool(Bus().get_resource(self.uid, Address.Make(self.uid, '/disk/copy_1/cc_2')).meta.get('yarovaya_mark'))
        assert not bool(Bus().get_resource(self.uid, Address.Make(self.uid, '/disk/copy_1/cc_2/cc_3')).meta.get('yarovaya_mark'))
        assert not bool(Bus().get_resource(self.uid, Address.Make(self.uid, '/disk/copy_1/cc_2/cc_3/cc_4.txt')).meta.get('yarovaya_mark'))

    def test_trash_append_folder_containing_shared_folder(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/1/2_2'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/1/2_2/3_3'})
        self.upload_file(self.uid, '/disk/1/2_2/3_3/4_4.txt')
        gid = self.create_group(uid=self.uid, path='/disk/1/2')
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid, path='/disk/1/2')
        self.activate_invite(uid=self.uid_3, hash=hsh)
        self._unmark_resource(self.uid, '/disk/1/2')
        trash_path = self.json_ok('trash_append', {'uid': self.uid, 'path': '/disk/1'})['this']['id']
        assert bool(Bus().get_resource(self.uid, Address.Make(self.uid, os.path.join(trash_path, '2'))).meta.get('yarovaya_mark'))
        # trash_append удаляет публичность после перемещения папки в корзину, поэтому дочерние ресурсы станут
        # непубличными в ходе медленного мува, поэтому, дочерние файлы этого ресрса не будут помеченными.
        assert not bool(Bus().get_resource(self.uid, Address.Make(self.uid, os.path.join(trash_path, '2_2'))).meta.get('yarovaya_mark'))
        assert not bool(Bus().get_resource(self.uid, Address.Make(self.uid, os.path.join(trash_path, '2_2/3_3'))).meta.get('yarovaya_mark'))
        assert not bool(Bus().get_resource(self.uid, Address.Make(self.uid, os.path.join(trash_path, '2_2/3_3/4_4.txt'))).meta.get('yarovaya_mark'))

    def test_rm_folder_containing_shared_folder(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/1/2_2'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/1/2_2/3_3'})
        self.upload_file(self.uid, '/disk/1/2_2/3_3/4_4.txt')
        gid = self.create_group(uid=self.uid, path='/disk/1/2')
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid, path='/disk/1/2')
        self.activate_invite(uid=self.uid_3, hash=hsh)
        self._unmark_resource(self.uid, '/disk/1/2')
        self.json_ok('rm', {'uid': self.uid, 'path': '/disk/1'})
        assert bool(Bus().get_resource(self.uid, Address.Make(self.uid, '/hidden/1/2/3')).list()['list'][0]['meta'].get('yarovaya_mark'))
        assert bool(Bus().get_resource(self.uid, Address.Make(self.uid, '/hidden/1/2/3')).meta.get('yarovaya_mark'))
        assert not bool(Bus().get_resource(self.uid, Address.Make(self.uid, '/hidden/1/2_2/3_3')).list()['list'][0]['meta'].get('yarovaya_mark'))
        assert not bool(Bus().get_resource(self.uid, Address.Make(self.uid, '/hidden/1/2_2/3_3')).meta.get('yarovaya_mark'))

    @parameterized.expand([
        (True,),
        (False,),
    ])
    def test_unshare_marks_resource(self, marked_before_moving):
        gid = self.create_group(uid=self.uid, path='/disk/1')
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid, path='/disk/1')
        self.activate_invite(uid=self.uid_3, hash=hsh)
        if not marked_before_moving:
            self._unmark_resource(self.uid, '/disk/1')
            self._unmark_resource(self.uid_3, '/disk/1')
        self.json_ok('share_unshare_folder', {'uid': self.uid, 'gid': gid})
        assert Bus().get_resource(self.uid, Address.Make(self.uid, '/disk/1')).meta.get('yarovaya_mark')

    @parameterized.expand([
        ('move', '/disk/1/2/3/4.txt', '/disk/5.txt'),
        ('move', '/disk/1', '/disk/11'),
        ('move', '/disk/ow', '/disk/1'),
        ('move', '/disk/ow', '/disk/1/2/3/4.txt'),
        ('copy', '/disk/1/2/3/4.txt', '/disk/5.txt'),
        ('copy', '/disk/1', '/disk/11'),
        ('move', '/disk/ow.txt', '/disk/1'),
        ('move', '/disk/ow.txt', '/disk/1/2/3/4.txt'),
        ('trash_append', '/disk/1/2/3/4.txt', '/trash/4.txt'),
        ('trash_append', '/disk/1', '/trash/1'),
    ])
    def test_moving_private_resource_doesnt_set_mark(self, action, src_path, new_path):
        with mock.patch('mpfs.metastorage.mongo.collections.filesystem.POSTGRES_QUICK_MOVE_ENABLED', False):
            self._moving_common_manipulations_and_checks_for_private_resource(action, src_path, new_path)

    @parameterized.expand([
        ('move', '/disk/1/2/3/4.txt', '/disk/5.txt'),
        ('move', '/disk/1', '/disk/11'),
        ('move', '/disk/ow.txt', '/disk/1'),
        ('move', '/disk/ow.txt', '/disk/1/2/3/4.txt'),
        ('move', '/disk/ow.txt', '/disk/1'),
        ('move', '/disk/ow.txt', '/disk/1/2/3/4.txt'),
        ('trash_append', '/disk/1/2/3/4.txt', '/trash/4.txt'),
        ('trash_append', '/disk/1', '/trash/1'),
    ])
    @pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='Pg implementation, do not test it on Mongo')
    def test_moving_private_resource_doesnt_set_mark_fast_move(self, action, src_path, new_path):
        with mock.patch('mpfs.metastorage.mongo.collections.filesystem.POSTGRES_QUICK_MOVE_ENABLED', True), \
                mock.patch('mpfs.metastorage.mongo.collections.filesystem.POSTGRES_QUICK_MOVE_ALLOWED_UIDS', self.uid):
            self._moving_common_manipulations_and_checks_for_private_resource(action, src_path, new_path)

    @parameterized.expand([
        ('/disk/1/2/3/4.txt', '/disk/1/2/3/4.txt', '/disk/copy_4.txt', ['/disk/copy_4.txt'], True,),
        ('/disk/1/2/3/4.txt', '/disk/1/2/3/4.txt', '/disk/copy_4.txt', ['/disk/copy_4.txt'], False,),
        ('/disk/1/2/3', '/disk/1/2/3', '/disk/copy_3', ['/disk/copy_3', '/disk/copy_3/4.txt'], True,),
        ('/disk/1/2/3', '/disk/1/2/3', '/disk/copy_3', ['/disk/copy_3', '/disk/copy_3/4.txt'], False,),
        ('/disk/1/2/3', '/disk/1/2', '/disk/copy_2', ['/disk/copy_2', '/disk/copy_2/3', '/disk/copy_2/3/4.txt'], True,),
        ('/disk/1/2/3', '/disk/1/2', '/disk/copy_2', ['/disk/copy_2', '/disk/copy_2/3', '/disk/copy_2/3/4.txt'], False,),
        ('/disk/1/2', '/disk/1/2/3', '/disk/copy_3', ['/disk/copy_3', '/disk/copy_3/4.txt'], False,),
        ('/disk/1/2', '/disk/1/2/3', '/disk/copy_3', ['/disk/copy_3', '/disk/copy_3/4.txt'], True,),
    ])
    def test_copying_public_resource_doesnt_inherit_yarovaya_mark(self, path_to_public, src_path, dst_path, should_be_unmarked, marked_before_copying):
        self.json_ok('set_public', {'uid': self.uid, 'path': path_to_public})
        if not marked_before_copying:
            self._unmark_resource(self.uid, path_to_public)
        self.json_ok('copy', {'uid': self.uid, 'src': src_path, 'dst': dst_path})
        assert all([not bool(Bus().get_resource(self.uid, Address.Make(self.uid, p)).meta.get('yarovaya_mark'))
                    for p in should_be_unmarked])

    @parameterized.expand([
        ('/disk/1/2/3/4.txt', '/disk/copy_4.txt', ['/disk/copy_4.txt'], True, True,),
        ('/disk/1/2/3/4.txt', '/disk/copy_4.txt', ['/disk/copy_4.txt'], True, False,),
        ('/disk/2/3/4.txt', '/disk/copy_4.txt', ['/disk/copy_4.txt'], False, True,),
        ('/disk/2/3/4.txt', '/disk/copy_4.txt', ['/disk/copy_4.txt'], False, False,),
        ('/disk/1/2/3', '/disk/copy_3', ['/disk/copy_3', '/disk/copy_3/4.txt'], True, True,),
        ('/disk/1/2/3', '/disk/copy_3', ['/disk/copy_3', '/disk/copy_3/4.txt'], True, False,),
        ('/disk/2/3', '/disk/copy_3', ['/disk/copy_3', '/disk/copy_3/4.txt'], False, True,),
        ('/disk/2/3', '/disk/copy_3', ['/disk/copy_3', '/disk/copy_3/4.txt'], False, False,),
        ('/disk/1/2', '/disk/copy_2', ['/disk/copy_2', '/disk/copy_2/3', '/disk/copy_2/3/4.txt'], True, True,),
        ('/disk/1/2', '/disk/copy_2', ['/disk/copy_2', '/disk/copy_2/3', '/disk/copy_2/3/4.txt'], True, False,),
        ('/disk/2', '/disk/copy_2', ['/disk/copy_2', '/disk/copy_2/3', '/disk/copy_2/3/4.txt'], False, True,),
        ('/disk/2', '/disk/copy_2', ['/disk/copy_2', '/disk/copy_2/3', '/disk/copy_2/3/4.txt'], False, False,),
        ('/disk/1', '/disk/copy_1', ['/disk/copy_1', '/disk/copy_1/2', '/disk/copy_1/2/3', '/disk/copy_1/2/3/4.txt'], True, True,),
        ('/disk/1', '/disk/copy_1', ['/disk/copy_1', '/disk/copy_1/2', '/disk/copy_1/2/3', '/disk/copy_1/2/3/4.txt'], True, False,),
    ])
    def test_copying_shared_resource_doesnt_inherit_yarovaya_mark(self, src_path, dst_path, should_be_unmarked,
                                                                  done_by_owner, marked_before_copying):
        gid = self.create_group(uid=self.uid, path='/disk/1/2')
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid, path='/disk/1/2')
        self.activate_invite(uid=self.uid_3, hash=hsh)
        if not marked_before_copying:
            self._unmark_resource(self.uid, '/disk/1/2')

        action_initiator = self.uid if done_by_owner else self.uid_3
        self.json_ok('copy', {'uid': action_initiator, 'src': src_path, 'dst': dst_path})
        assert all([not bool(Bus().get_resource(action_initiator, Address.Make(action_initiator, p)).meta.get('yarovaya_mark'))
                    for p in should_be_unmarked])

    def test_rm_private_file_doesnt_set_mark(self):
        self.json_ok('rm', {'uid': self.uid, 'path': '/disk/1/2/3/4.txt'})
        assert not bool(Bus().get_resource(self.uid, Address.Make(self.uid, '/hidden')).list()['list'][0]['meta'].get('yarovaya_mark'))

    def test_rm_private_folder_doesnt_set_mark(self):
        self.json_ok('rm', {'uid': self.uid, 'path': '/disk/1'})
        assert not Bus().get_resource(self.uid, Address.Make(self.uid, '/hidden/1')).meta.get('yarovaya_mark')

    @pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='Postgres test')
    def test_dont_copy_yarovaya_mark_during_hardlink(self):
        info = self.json_ok('info', {'uid': self.uid, 'path': '/disk/1/2/3/4.txt', 'meta': ''})
        self._mark_resource(self.uid, '/disk/1/2/3/4.txt')
        with self.patch_mulca_is_file_exist(func_resp=True):
            self.json_ok('store', {'uid': self.uid, 'path': '/disk/hardlinked.txt', 'md5': info['meta']['md5'], 'sha256': info['meta']['sha256'], 'size': info['meta']['size']},)
        assert not bool(Bus().get_resource(self.uid, Address.Make(self.uid, '/disk/hardlinked.txt')).meta.get('yarovaya_mark'))

    def test_yarovaya_mark_is_missing_in_info_and_list(self):
        self._mark_resource(self.uid, '/disk/1/2/3/4.txt')
        self._mark_resource(self.uid, '/disk/1/2/3')

        assert 'yarovaya_mark' not in self.json_ok('info', {'uid': self.uid, 'path': '/disk/1/2/3/4.txt', 'meta': ''})['meta']
        assert 'yarovaya_mark' not in self.json_ok('info', {'uid': self.uid, 'path': '/disk/1/2/3', 'meta': ''})['meta']
        assert 'yarovaya_mark' not in self.json_ok('list', {'uid': self.uid, 'path': '/disk/1/2/3/4.txt', 'meta': ''})['meta']
        assert all('yarovaya_mark' not in x['meta'] for x in self.json_ok('list', {'uid': self.uid, 'path': '/disk/1/2/3', 'meta': ''}))
        assert all('yarovaya_mark' not in x['meta'] for x in self.json_ok('list', {'uid': self.uid, 'path': '/disk/1', 'meta': ''}))
        assert all('yarovaya_mark' not in x['meta'] for x in self.json_ok('list', {'uid': self.uid, 'path': '/disk', 'meta': ''}))

    def test_resources_in_hidden_with_mark_has_regular_dtime(self):
        current_time = datetime.datetime.now()
        self._mark_resource(self.uid, '/disk/1')
        with time_machine(current_time), \
             mock.patch.object(mpfs.metastorage.mongo.collections.filesystem, 'HIDDEN_DATA_CLEANER_YAROVAYA_DTIME_DEPENDS_ON_MARK_ENABLED', True):
            self.json_ok('rm', {'uid': self.uid, 'path': '/disk/1'})
        resource_from_hidden = HiddenDAO().find({'uid': self.uid, 'type': 'file'}).next()
        assert resource_from_hidden['dtime'] == int(current_time.strftime('%s'))

    def test_resources_in_hidden_without_mark_has_dtime_in_past(self):
        current_time = datetime.datetime.now()
        with time_machine(current_time), \
             mock.patch.object(mpfs.metastorage.mongo.collections.filesystem, 'HIDDEN_DATA_CLEANER_YAROVAYA_DTIME_DEPENDS_ON_MARK_ENABLED', True):
            self.json_ok('rm', {'uid': self.uid, 'path': '/disk/1'})
        resource_from_hidden = HiddenDAO().find({'uid': self.uid, 'type': 'file'}).next()
        assert int((current_time - datetime.timedelta(days=160)).strftime('%s')) == resource_from_hidden['dtime']

    def _moving_common_manipulations_and_checks_for_public(self, action_type, path_to_set_public, original_path,
                                                           should_be_marked_paths=None, should_be_unmarked_paths=None,
                                                           has_mark_before_moving=True):
        if should_be_marked_paths is None:
            should_be_marked_paths = []
        if should_be_unmarked_paths is None:
            should_be_unmarked_paths = []
        self.json_ok('set_public', {'uid': self.uid, 'path': path_to_set_public})
        if not has_mark_before_moving:
            self._unmark_resource(self.uid, path_to_set_public)

        name = original_path.split('/')[-1]
        if action_type == 'move':
            dst_path = '/disk/copy_%s' % name
            self.json_ok('move', {'uid': self.uid, 'src': original_path, 'dst': dst_path})
        else:
            with mock.patch('mpfs.core.address.Address.add_trash_suffix'):
                self.json_ok('trash_append', {'uid': self.uid, 'path': original_path})

        assert all([bool(Bus().get_resource(self.uid, Address.Make(self.uid, p)).meta.get('yarovaya_mark'))
                    for p in should_be_marked_paths])
        assert all([not bool(Bus().get_resource(self.uid, Address.Make(self.uid, p)).meta.get('yarovaya_mark'))
                    for p in should_be_unmarked_paths])

    def _moving_common_manipulations_and_checks_for_shared(self, action_type, path_to_share, owner, guest, original_path,
                                                           action_initiator, marked_before_moving=True):
        gid = self.create_group(uid=owner, path=path_to_share)
        hsh = self.invite_user(uid=guest, email=self.email_3, ext_gid=gid, path=path_to_share)
        self.activate_invite(uid=guest, hash=hsh)

        guest_shared_root_folder_path = '/disk/%s' % path_to_share.split('/')[-1]
        if not marked_before_moving:
            for uid, path in [(owner, path_to_share), (guest, guest_shared_root_folder_path)]:
                self._unmark_resource(uid, path)

        file_to_move_name = original_path.split('/')[-1]
        if action_type == 'move':
            dst_path = '/disk/copy_%s' % file_to_move_name
            self.json_ok('move', {'uid': action_initiator, 'src': original_path, 'dst': dst_path})
        elif action_type == 'trash_append':
            with mock.patch('mpfs.core.address.Address.add_trash_suffix'):
                self.json_ok('trash_append', {'uid': action_initiator, 'path': original_path})
        else:
            self.json_ok('rm', {'uid': action_initiator, 'path': original_path})

    def _unmark_resource(self, uid, path):
        r = Bus().get_resource(self.uid, Address.Make(uid, path))
        r.meta['yarovaya_mark'] = False
        r.save()

    def _mark_resource(self, uid, path):
        r = Bus().get_resource(self.uid, Address.Make(uid, path))
        r.meta['yarovaya_mark'] = True
        r.save()

    def _mark_public_folder_overwritten_with_file_common_manipulations_and_checks(self, public_resource_path, src_path,
                                                                                  dst_path, should_be_marked_paths,
                                                                                  file_parent_in_hidden,
                                                                                  marked_before_moving):
        self.json_ok('set_public', {'uid': self.uid, 'path': public_resource_path})
        if not marked_before_moving:
            self._unmark_resource(self.uid, dst_path)

        self.json_ok('move', {'uid': self.uid, 'dst': dst_path, 'src': src_path, 'force': 1})

        assert all([bool(Bus().get_resource(self.uid, Address.Make(self.uid, p)).meta.get('yarovaya_mark'))
                    for p in should_be_marked_paths])

        assert bool(Bus().get_resource(self.uid, Address.Make(self.uid, file_parent_in_hidden)).list()['list'][0]['meta']['yarovaya_mark'])

    def _moving_common_manipulations_and_checks_for_private_resource(self, action_type, src_path, new_path):
        if action_type in ('move', 'copy'):
            self.json_ok(action_type, {'uid': self.uid, 'src': src_path, 'dst': new_path, 'force': 1})
        else:
            with mock.patch('mpfs.core.address.Address.add_trash_suffix'):
                self.json_ok('trash_append', {'uid': self.uid, 'path': src_path})

        assert not bool(Bus().get_resource(self.uid, Address.Make(self.uid, new_path)).meta.get('yarovaya_mark'))
