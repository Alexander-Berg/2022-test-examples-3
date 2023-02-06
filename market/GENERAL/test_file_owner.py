# coding: utf-8

import collections
import os
import shutil
import tempfile

import mock
import pytest

import common
import file_owner
import test_common


ALLOWED_USER_ID = 1
os_stat_orig = os.stat

WATCHED_CONFIG_PATH = [
    "bin_path",
    "data_directory",
    "pdata_directory",
    "pstate_directory",
    "state_directory",
]


class MockedStat(object):
    def __init__(self, os_stat, st_uid=None):
        self.os_stat = os_stat
        self.st_uid = st_uid

    def __getattr__(self, name):
        if name == 'st_uid' and self.st_uid is not None:
            return self.st_uid
        return getattr(self.os_stat, name)


@pytest.yield_fixture(scope='module')
def mocked_stat():
    def stat(file_path):
        st = os_stat_orig(file_path)
        st_uid = ALLOWED_USER_ID if os.path.basename(file_path).startswith(file_owner.ALLOWED_FILE_OWNER) else None
        return MockedStat(st, st_uid)
    with mock.patch('os.stat', new=stat):
        yield


@pytest.yield_fixture(scope='module')
def mocked_getpwuid():
    PWInfo = collections.namedtuple('PWInfo', ['pw_name'])
    def getpwuid(uid):
        return PWInfo(file_owner.ALLOWED_FILE_OWNER) if uid == ALLOWED_USER_ID else PWInfo('user')
    with mock.patch('pwd.getpwuid', new=getpwuid):
        yield


@pytest.yield_fixture(scope='function')
def patch_config(generate_config):
    base_path = None
    try:
        base_path = tempfile.mkdtemp(prefix=file_owner.ALLOWED_FILE_OWNER, dir=common.config().conf_path)
        for path in WATCHED_CONFIG_PATH:
            setattr(common.config(), path, base_path)
        yield
    finally:
        if base_path is not None:
            shutil.rmtree(base_path)


def prepare_dirs(base_dir_path, dirs=None, files=None):
    if dirs is not None:
        for dir in dirs:
            os.makedirs(os.path.join(base_dir_path, dir))
    if files is not None:
        for file_path in files:
            with open(os.path.join(base_dir_path, file_path), 'wt') as f:
                f.write(' ')


def preapare_allowed_items(base_dir_path):
    prepare_dirs(
        base_dir_path,
        ['loadbase'],
        [
            'loadbase.txt',
            'loadbase/loadbase.txt',
        ]
    )


def is_status_contain(paths):
    status_file_path = os.path.join(common.config().monitoring_directory, file_owner.STATUS_FILE_NAME)
    with open(status_file_path) as f:
        status = f.read()
        for path in paths:
            if status.find(path) == -1:
                raise AssertionError('path {} not found in status file {}'.format(path, status_file_path))
    return True


@pytest.mark.usefixtures('mocked_stat', 'mocked_getpwuid', 'patch_config')
def test_all_ok():
    preapare_allowed_items(common.config().bin_path)
    with test_common.OutputCapture() as capture:
        file_owner.main()
        assert capture.get_stdout() == 'PASSIVE-CHECK:market-report-file-owner;0;Ok\n'
        assert capture.get_stderr() == ''


@pytest.mark.usefixtures('mocked_stat', 'mocked_getpwuid', 'patch_config')
def test_dir_failed():
    preapare_allowed_items(common.config().bin_path)
    filed_dirs = [
        'user',
        'loadbase/user',
    ]
    prepare_dirs(common.config().bin_path, dirs=filed_dirs)
    with test_common.OutputCapture() as capture:
        file_owner.main()
        assert capture.get_stdout().startswith('PASSIVE-CHECK:market-report-file-owner;2;')
        assert capture.get_stderr() == ''
        assert is_status_contain(filed_dirs)


@pytest.mark.usefixtures('mocked_stat', 'mocked_getpwuid', 'patch_config')
def test_file_failed():
    preapare_allowed_items(common.config().bin_path)
    filed_files = [
        'user.txt',
        'loadbase/user.txt',
    ]
    prepare_dirs(common.config().bin_path, files=filed_files)
    with test_common.OutputCapture() as capture:
        file_owner.main()
        assert capture.get_stdout().startswith('PASSIVE-CHECK:market-report-file-owner;2;')
        assert capture.get_stderr() == ''
        assert is_status_contain(filed_files)


@pytest.mark.usefixtures('mocked_stat', 'mocked_getpwuid', 'patch_config', 'indigo_cluster')
def test_ignore_indigo():
    prepare_dirs(common.config().bin_path, dirs=['user'], files=['user.txt'])
    with test_common.OutputCapture() as capture:
        file_owner.main()
        assert capture.get_stdout() == 'PASSIVE-CHECK:market-report-file-owner;0;Ok\n'
        assert capture.get_stderr() == ''


@pytest.mark.usefixtures('mocked_stat', 'mocked_getpwuid', 'patch_config')
def test_ignore_tmp_files():
    tmp_files = ['user.swp']
    prepare_dirs(common.config().bin_path, files=tmp_files)
    with test_common.OutputCapture() as capture:
        file_owner.main()
        assert capture.get_stdout() == 'PASSIVE-CHECK:market-report-file-owner;0;Ok\n'
        assert capture.get_stderr() == ''
