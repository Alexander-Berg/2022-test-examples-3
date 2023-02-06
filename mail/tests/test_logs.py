# coding: utf-8

import mock
import pytest

from ora2pg.app.logs import (tree_by_uid,
                             get_paths_to_uid_dir,
                             make_dirs_with_perms,
                             current_user_is_root)


@pytest.mark.parametrize(['uid', 'is_root'], [
    (0, True),
    (4214, False),
])
def test_current_user_is_root(uid, is_root):
    with mock.patch('ora2pg.app.logs.os.getuid') as getuid_mock:
        getuid_mock.return_value = uid
        assert current_user_is_root() == is_root


def test_make_dirs_calls_mkdir_with_mode_from_base_dir_when_no_such_dir_exists():
    with \
        mock.patch('ora2pg.app.logs.os.stat') as stat_mock, \
        mock.patch('ora2pg.app.logs.os.mkdir') as mkdir_mock, \
        mock.patch('ora2pg.app.logs.os.path.exists') as path_exist_mock, \
        mock.patch('ora2pg.app.logs.current_user_is_root') as current_user_is_root_mock \
    :
        current_user_is_root_mock.return_value = False
        path_exist_mock.return_value = False

        make_dirs_with_perms('/tmp/', '/tmp/foo/bar')
        path_exist_mock.assert_called_once_with('/tmp/foo/bar')
        stat_mock.assert_called_once_with('/tmp/')
        mkdir_mock.assert_called_once_with('/tmp/foo/bar', stat_mock.return_value.st_mode)


def test_make_dirs_calls_chown_when_current_user_is_root():
    with \
        mock.patch('ora2pg.app.logs.os.stat') as stat_mock, \
        mock.patch('ora2pg.app.logs.os.mkdir'), \
        mock.patch('ora2pg.app.logs.current_user_is_root') as current_user_is_root_mock, \
        mock.patch('ora2pg.app.logs.os.chown') as chown_mock \
    :
        current_user_is_root_mock.return_value = True
        make_dirs_with_perms('/tmp/', '/tmp/foo/bar')
        st_res = stat_mock.return_value
        chown_mock.assert_called_once_with('/tmp/foo/bar', st_res.st_uid, st_res.st_gid)


@pytest.mark.parametrize(['uid', 'path'], [
    (111222, ['/logs/111', '/logs/111/222']),
    (42, ['/logs/420', '/logs/420/000']),
])
def test_get_path_to_uid_dir(uid, path):
    assert get_paths_to_uid_dir(uid, '/logs/') == path


def test_tree_by_uid_calls_make_dirs_with_perms_for_each_path_from_get_paths_for_uids_dir():
    with \
        mock.patch('ora2pg.app.logs.get_paths_to_uid_dir') as get_path_mock, \
        mock.patch('ora2pg.app.logs.make_dirs_with_perms') as mkdir_mock \
    :
        get_path_mock.return_value = ['/logs/first', '/logs/first/second']

        assert tree_by_uid(111222, '/logs/') == '/logs/first/second'
        mkdir_mock.assert_has_calls([
            mock.call('/logs/', '/logs/first'),
            mock.call('/logs/', '/logs/first/second')
        ])
