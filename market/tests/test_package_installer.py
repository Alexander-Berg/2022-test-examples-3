# -*- coding: utf-8 -*-

import os
import shutil
import tarfile
import time
import unittest
from six.moves.configparser import SafeConfigParser

from market.pylibrary.mi_util import util
from pyb.plugin.package_installer import PackageInstaller
from pyb.taskrunner import TaskInfo

import yatest

PACKAGE = 'package'
VERSION = '111'
URL = 'package_url.torrent'
PACKAGES_DIR = 'packages'
DOWNLOAD_DIR = os.path.join(PACKAGES_DIR, 'download')
FORCE_RESTART_FILE = 'force_restart'
VERSIONS_DIR = 'versions'
INSTALL_DIR = 'installed'
PACKAGE_PATH = os.path.join(PACKAGES_DIR, PACKAGE)
VERSION_PATH = os.path.join(PACKAGE_PATH, VERSION)
INSTALLED_VERSION_PATH = os.path.join(VERSIONS_DIR, PACKAGE)


class MockContext(object):
    def __init__(self, task_runner):
        self.torrent_client = None
        self.torrent_client_config = None
        self.sky_downloader_client = None
        self.sky_downloader_client_config = None
        self.aria2_user = None
        self.task_runner = task_runner
        self.config = self._create_config()
        self.download_dir = DOWNLOAD_DIR
        self.dists = {PACKAGE: INSTALL_DIR}
        self.force_report_restart_file = FORCE_RESTART_FILE

    @staticmethod
    def _create_config():
        config = SafeConfigParser()
        config.add_section('general')
        config.set('general', 'download_dir', DOWNLOAD_DIR)
        config.set('general', 'install_dir', INSTALL_DIR)
        config.set('general', 'packages_dir', PACKAGES_DIR)
        config.set('general', 'versions_dir', VERSIONS_DIR)
        config.set('general', 'force_report_restart_file', FORCE_RESTART_FILE)
        uc_bin = yatest.common.binary_path('tools/uc/uc')
        config.set('general', 'uc_bin', uc_bin)
        config.set('general', 'decompress_threads', '1')
        config.add_section('dists')
        config.set('dists', PACKAGE, INSTALL_DIR)
        return config


class MockTorrent(object):
    def __init__(self):
        self._name_path = {}

    def start_dist(self, name, url, path, finished_file, throw_on_error=False):
        util.makedirs(path)
        util.touch(os.path.join(path, 'started'))
        self._name_path[name] = path
        return 0

    def _set_download_completed(self, name):
        path = self._name_path.get(name)
        if path:
            util.touch(os.path.join(path, 'completed'))

    def stop_dist_version(self, name, dist_version, move_to_dir, throw_on_error=False):
        path = self._name_path.get(name)
        util.makedirs(path)
        if move_to_dir:
            util.makedirs(move_to_dir)
        return 0


class MockSkyDownloader(object):
    def has_dist(self, dist_name, dist_version=None, throw_on_error=True):
        return False


class MockTaskRunner(object):
    def __init__(self):
        self.tasks = {}

    def start(self, name, action, args):
        self.tasks[name] = TaskInfo(name, TaskInfo.STARTING)

    def get(self, name):
        default_task = TaskInfo(name, TaskInfo.NOT_FOUND)
        return self.tasks.get(name, default_task)

    def kill(self, name):
        self.set_state(name, TaskInfo.FINISHED)

    def set_state(self, name, state):
        self.get(name).state = state


class PackageInstallerTest(unittest.TestCase):
    def setUp(self):
        self._task_runner = MockTaskRunner()
        context = MockContext(self._task_runner)
        self._installer = PackageInstaller(context)
        self._installer._torrent_client = MockTorrent()
        self._installer._sky_downloader_client = MockSkyDownloader()

    def tearDown(self):
        def remove(path):
            if os.path.exists(path):
                shutil.rmtree(path)
        remove(INSTALL_DIR)
        remove(PACKAGES_DIR)
        remove(VERSIONS_DIR)
        remove(DOWNLOAD_DIR)

    def assertSuccess(self, result):
        self.assertFalse(self._is_error(result))

    def assertFailure(self, result):
        self.assertTrue(self._is_error(result))

    def assertExists(self, path):
        self.assertTrue(os.path.exists(path))

    def test_remove_old(self):
        versions = ['111', '222', '333', '444']
        with open(INSTALLED_VERSION_PATH, 'w') as version_file:
            version_file.write('222')
        for version in versions:
            util.makedirs(os.path.join(PACKAGE_PATH, version))
            time.sleep(0.1)
        self.assertEqual(sorted(versions), sorted(os.listdir(PACKAGE_PATH)))

        self._installer.start_download(PACKAGE, URL, '555')
        self._installer._torrent_client._set_download_completed(PACKAGE)
        self._installer.stop_download(PACKAGE, '555')
        saved_versions = ['222', '555']
        self.assertEqual(saved_versions, sorted(os.listdir(PACKAGE_PATH)))

    def test_move_downloaded(self):
        self._installer.start_download(PACKAGE, URL, VERSION)
        self._installer._torrent_client._set_download_completed(PACKAGE)
        result = self._installer.stop_download(PACKAGE, '111')
        self.assertSuccess(result)
        self.assertTrue(os.path.exists(VERSION_PATH))

    def test_start_install(self):
        self.assertFailure(self._installer.start_install('bad_package'))
        self.assertFailure(self._installer.start_install(PACKAGE))
        util.makedirs(PACKAGE_PATH)
        self.assertFailure(self._installer.start_install(PACKAGE))

        util.makedirs(VERSION_PATH)
        self.assertSuccess(self._installer.start_install(PACKAGE, VERSION))
        self.assertFalse(self._get_task().not_found)
        self.assertSuccess(self._installer.start_install(PACKAGE))
        self.assertFailure(self._installer.start_install(PACKAGE, '222'))

    def test_cancel_install(self):
        self.assertSuccess(self._installer.stop_install())

        util.makedirs(VERSION_PATH)
        self._installer.start_install(PACKAGE, VERSION)
        self.assertSuccess(self._installer.stop_install())
        self.assertTrue(self._get_task().finished)

        self.assertSuccess(self._installer.stop_install())

    def test_check(self):
        self.assertFailure(self._installer.check())

        util.makedirs(VERSION_PATH)
        self._installer.start_install(PACKAGE)
        self.assertEqual(self._installer.check(), '! in progress')

        self._task_runner.kill(PackageInstaller.TASK_NAME)
        self._get_task().result = None
        self.assertFailure(self._installer.check())

        self._get_task().result = True
        self.assertSuccess(self._installer.check())

    def test_get_installed_version(self):
        version = self._installer.get_installed_version(PACKAGE)
        self.assertEqual(version, 'none')
        with open(INSTALLED_VERSION_PATH, 'w') as version_file:
            version_file.write(VERSION)
        version = self._installer.get_installed_version(PACKAGE)
        self.assertEqual(version, VERSION)

    def test_get_versions(self):
        versions = ['111', '222', '333', '444', '555']
        for version in versions:
            util.makedirs(os.path.join(PACKAGE_PATH, version))
        result = self._installer.get_versions(PACKAGE)
        self.assertEqual(sorted(versions), sorted(result.split(',')))
        result = self._installer.get_dist_generations(PACKAGE)
        self.assertEqual(sorted(versions), sorted(result.split(',')))

    @unittest.skip("sandbox environment cause this test to fail")
    def test_install(self):
        util.touch('top_file')
        util.makedirs('top_dir')
        util.touch(os.path.join('top_dir', 'inner_file'))

        util.makedirs(VERSION_PATH)
        archive_path = os.path.join(VERSION_PATH, 'archive.tar.gz')
        with tarfile.open(archive_path, 'w:gz') as archive_file:
            archive_file.add('top_dir')
            archive_file.add('top_file')

        util.touch(os.path.join(VERSION_PATH, 'not_archive'))
        self.assertTrue(self._installer.install(PACKAGE, VERSION))
        self.assertExists(os.path.join(INSTALL_DIR, 'top_file'))
        self.assertExists(os.path.join(INSTALL_DIR, 'top_dir', 'inner_file'))

    @unittest.skip("sandbox environment cause this test to fail")
    def test_install_multiple(self):
        util.makedirs(VERSION_PATH)
        files = ['file_{}'.format(i) for i in range(3)]
        for file_name in files:
            util.touch(file_name)
            archive_path = os.path.join(VERSION_PATH, file_name + '.tar.gz')
            with tarfile.open(archive_path, 'w:gz') as archive_file:
                archive_file.add(file_name)

        self.assertTrue(self._installer.install(PACKAGE, VERSION))
        for file_name in files:
            self.assertExists(os.path.join(INSTALL_DIR, file_name))

    def test_install_no_archives(self):
        util.makedirs(VERSION_PATH)
        util.touch(os.path.join(VERSION_PATH, 'not_archive'))
        self.assertFalse(self._installer.install(PACKAGE, VERSION))

    def test_install_failure(self):
        os.makedirs(VERSION_PATH)
        util.touch(os.path.join(VERSION_PATH, 'not_archive.tar.gz'))
        self.assertFalse(self._installer.install(PACKAGE, VERSION))
        with open(INSTALLED_VERSION_PATH) as version_file:
            installed = version_file.read()
        self.assertEqual(installed, 'incorrect')

    @staticmethod
    def _is_error(result):
        return result.startswith('!')

    def _get_task(self):
        return self._task_runner.get(PackageInstaller.TASK_NAME)
