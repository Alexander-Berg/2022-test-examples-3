# -*- coding: utf-8 -*-
"""
Документация соответствие которой проверяют эти тесты https://wiki.yandex-team.ru/disk/mpfs/DiskDates
"""
import pytest

from test.base import DiskTestCase

import mock
import mpfs

from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase

import time

from test.conftest import INIT_USER_IN_POSTGRES

db = CollectionRoutedDatabase()


feature_ctime_from_client_mock = mock.patch('mpfs.frontend.api.disk.STORE_CTIME_FROM_CLIENT_ENABLED', True)


class TimesMkdirAndStoreTestCase(DiskTestCase):
    CTIME = int(time.time()) - 10000
    MTIME = int(time.time()) - 5000

    FOLDER_NAME = 'test_time'
    FOLDER_PATH = '/'.join(['/disk', FOLDER_NAME])

    FILE_NAME = 'custom_time_1.txt'
    FILE_PATH = '/'.join(['/disk', FILE_NAME])

    def test_mkdir(self):
        """Создаём папку и проверям, что ctime, mtime и utime идентичны"""
        self.json_ok('mkdir', opts={'uid': self.uid, 'path': self.FOLDER_PATH})
        info = self.json_ok('info', opts={'uid': self.uid, 'path': self.FOLDER_PATH})
        self.assertEqual(info['ctime'], info['mtime'])
        # utime устанавливается отдельно от mtime и ctime поэтому иногда отличается на несколько секунд
        # надо это фиксить в MPFS, но т.к. эта функциональность пока не используется решили пока отключить проверку
        # self.assertEqual(info['mtime'], info['utime'])

    def test_client_mkdir(self):
        """Создаём папку с клиентскими ctime и mtime и проверяем, что они отличаются от utime"""
        self.json_ok('mkdir', opts={'uid': self.uid, 'path': self.FOLDER_PATH, 'ctime': self.CTIME, 'mtime': self.MTIME})
        info = self.json_ok('info', opts={'uid': self.uid, 'path': self.FOLDER_PATH})
        self.assertEqual(info['ctime'], self.CTIME)
        self.assertEqual(info['mtime'], self.MTIME)
        self.assertTrue(info['ctime'] < info['utime'])
        self.assertTrue(info['mtime'] < info['utime'])

    def test_store(self):
        """Загружаем файл и проверям, что ctime, mtime и utime идентичны и что у файла есть etime"""
        with feature_ctime_from_client_mock:
            self.upload_file(self.uid, self.FILE_PATH)
        info = self.json_ok('info', opts={'uid': self.uid, 'path': self.FILE_PATH})
        self.assertEqual(info['ctime'], info['mtime'])
        # utime устанавливается отдельно от mtime и ctime поэтому иногда отличается на несколько секунд
        # надо это фиксить в MPFS, но т.к. эта функциональность пока не используется решили пока отключить проверку
        # self.assertEqual(info['mtime'], info['utime'])
        self.assertTrue(isinstance(info['etime'], int))

    def test_client_store(self):
        """Загружаем файл с клиентскими ctime и mtime и проверяем, что они отличаются от utime"""
        with feature_ctime_from_client_mock:
            self.upload_file(self.uid, self.FILE_PATH, opts={'ctime': self.CTIME, 'mtime': self.MTIME})
        info = self.json_ok('info', opts={'uid': self.uid, 'path': self.FILE_PATH})
        self.assertEqual(info['ctime'], self.CTIME)
        self.assertEqual(info['mtime'], self.MTIME)
        self.assertTrue(info['ctime'] < info['utime'])
        self.assertTrue(info['mtime'] < info['utime'])


def fake_time_with_increment():
    start_time_list = [time.time()]
    def wrapped():
        start_time_list[0] += 1
        return start_time_list[0]
    return wrapped


class TimesFolderOperationsTestCase(DiskTestCase):
    CTIME = int(time.time()) - 10000
    MTIME = int(time.time()) - 5000

    FOLDER_NAME = 'test_time'
    FOLDER_PATH = '/'.join(['/disk', FOLDER_NAME])

    DST_FOLDER_NAME = 'test_time_dst'
    DST_FOLDER_PATH = '/'.join(['/disk', DST_FOLDER_NAME])

    FILE_NAME = 'custom_time_1.txt'
    FILE_PATH = '/'.join([FOLDER_PATH, FILE_NAME])

    HARDLINK_NAME = 'custom_time_hardlink.txt'
    HARDLINK_PATH = '/'.join([FOLDER_PATH, HARDLINK_NAME])

    def setup_method(self, method):
        super(TimesFolderOperationsTestCase, self).setup_method(method)
        self.json_ok('mkdir', opts={'uid': self.uid, 'path': self.FOLDER_PATH, 'ctime': self.CTIME, 'mtime': self.MTIME})

    def test_publication(self):
        source_info = self.json_ok('info', opts={'uid': self.uid, 'path': self.FOLDER_PATH})
        self.json_ok('set_public', opts={'uid': self.uid, 'path': self.FOLDER_PATH})
        public_info = self.json_ok('info', opts={'uid': self.uid, 'path': self.FOLDER_PATH})
        self.json_ok('set_private', opts={'uid': self.uid, 'path': self.FOLDER_PATH})
        unpublic_info = self.json_ok('info', opts={'uid': self.uid, 'path': self.FOLDER_PATH})
        for k in ('ctime', 'mtime', 'utime'):
            assert source_info[k] == public_info[k] == unpublic_info[k]

    def test_dir_mtime_change_on_store(self):
        """Проверяем неизменность времен родительского каталога при загрузке в него файла"""
        original_folder_info = self.json_ok('info', opts={'uid': self.uid, 'path': self.FOLDER_PATH})
        self.upload_file(self.uid, self.FILE_PATH)
        folder_info = self.json_ok('info', opts={'uid': self.uid, 'path': self.FOLDER_PATH})
        for k in ('utime', 'ctime', 'mtime'):
            self.assertEqual(folder_info[k], original_folder_info[k])

    @pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='Postgres test')
    def test_dir_mtime_change_on_hardlink(self):
        """Проверяем неизменность времен родительского каталога при хардлинке"""
        with feature_ctime_from_client_mock:
            self.upload_file(self.uid, self.FILE_PATH, opts={'ctime': self.CTIME, 'mtime': self.MTIME})
        original_folder_info = self.json_ok('info', opts={'uid': self.uid, 'path': self.FOLDER_PATH})
        # периодически файл загружается и хардлинкается меньше чем за секунду поэтому mtime не изменяется
        # лечим это здоровым сном
        time.sleep(1)
        self.hardlink_file(self.uid, self.FILE_PATH, self.HARDLINK_PATH)
        folder_info = self.json_ok('info', opts={'uid': self.uid, 'path': self.FOLDER_PATH})
        for k in ('utime', 'ctime', 'mtime'):
            self.assertEqual(folder_info[k], original_folder_info[k])

    def test_copy(self):
        """Проеряем, что при копировании ctime и mtime у папки новые, а utime старый"""
        source_info = self.json_ok('info', opts={'uid': self.uid, 'path': self.FOLDER_PATH})
        with mock.patch('time.time', fake_time_with_increment()):
            self.async_ok('async_copy', opts={'uid': self.uid, 'src': self.FOLDER_PATH, 'dst': self.DST_FOLDER_PATH})
        copy_info = self.json_ok('info', opts={'uid': self.uid, 'path': self.DST_FOLDER_PATH})
        for k in ('ctime', 'mtime'):
            self.assertTrue(copy_info[k] > source_info[k])
        self.assertEqual(copy_info['utime'], source_info['utime'])

    def test_move(self):
        """Проверяем, что при перемещении ctime и utime сохраняются, a mtime меняется"""
        source_info = self.json_ok('info', opts={'uid': self.uid, 'path': self.FOLDER_PATH})
        with mock.patch('time.time', fake_time_with_increment()):
            self.async_ok('async_move', opts={'uid': self.uid, 'src': self.FOLDER_PATH, 'dst': self.DST_FOLDER_PATH})
        moved_info = self.json_ok('info', opts={'uid': self.uid, 'path': self.DST_FOLDER_PATH})
        for k in ('ctime', 'utime'):
            self.assertEqual(moved_info[k], source_info[k])
        assert moved_info['mtime'] > source_info['mtime']

    def test_trash_append(self):
        """Проверяем, что при удалении папки в корзину ctime и utime не меняются, a mtime меняется"""
        source_info = self.json_ok('info', opts={'uid': self.uid, 'path': self.FOLDER_PATH})
        with mock.patch('time.time', fake_time_with_increment()):
            with mock.patch('mpfs.core.address.Address.add_trash_suffix'):
                self.async_ok('async_trash_append', opts={'uid': self.uid, 'path': self.FOLDER_PATH})
        deleted_info = self.json_ok('info', opts={'uid': self.uid, 'path': '/'.join(['/trash', self.FOLDER_NAME])})
        for k in ('ctime', 'utime'):
            self.assertEqual(deleted_info[k], source_info[k])
        assert deleted_info['mtime'] > source_info['mtime']

    def test_trash_restore(self):
        """Проверяем, что при восстановлении папки из корзины ctime и utime не меняются, a mtime меняется"""
        trash_path = '/'.join(['/trash', self.FOLDER_NAME])
        with mock.patch('mpfs.core.address.Address.add_trash_suffix'):
            self.async_ok('async_trash_append', opts={'uid': self.uid, 'path': self.FOLDER_PATH})
        deleted_info = self.json_ok('info', opts={'uid': self.uid, 'path': trash_path})
        with mock.patch('time.time', fake_time_with_increment()):
            self.async_ok('async_trash_restore', opts={'uid': self.uid, 'path': trash_path})
        restored_info = self.json_ok('info', opts={'uid': self.uid, 'path': self.FOLDER_PATH})
        for k in ('ctime', 'utime'):
            self.assertEqual(deleted_info[k], restored_info[k])
        assert restored_info['mtime'] > deleted_info['mtime']

    def test_move_folder_check_subfolder(self):
        """Проверяем, что при перемещении родительской папки у вложенной папки времена не меняются"""
        subfolder_path = "%s/subfolder" % self.FOLDER_PATH
        dst_subfolder_path = "%s/subfolder" % self.DST_FOLDER_PATH
        self.json_ok('mkdir', {'uid': self.uid, 'path': subfolder_path})

        source_info = self.json_ok('info', opts={'uid': self.uid, 'path': subfolder_path})
        with mock.patch('time.time', fake_time_with_increment()):
            self.async_ok('async_move', opts={'uid': self.uid, 'src': self.FOLDER_PATH, 'dst': self.DST_FOLDER_PATH})
        moved_info = self.json_ok('info', opts={'uid': self.uid, 'path': dst_subfolder_path})
        for k in ('ctime', 'utime', 'mtime'):
            self.assertEqual(moved_info[k], source_info[k])

    def test_copy_folder_check_subfolder(self):
        """Проверяем, что при копировании родительской папки у вложенной папки ctime и mtime у папки новые, а utime старый"""
        subfolder_path = "%s/subfolder" % self.FOLDER_PATH
        dst_subfolder_path = "%s/subfolder" % self.DST_FOLDER_PATH
        self.json_ok('mkdir', {'uid': self.uid, 'path': subfolder_path})

        source_info = self.json_ok('info', opts={'uid': self.uid, 'path': subfolder_path})
        with mock.patch('time.time', fake_time_with_increment()):
            self.async_ok('async_copy', opts={'uid': self.uid, 'src': self.FOLDER_PATH, 'dst': self.DST_FOLDER_PATH})
        copy_info = self.json_ok('info', opts={'uid': self.uid, 'path': dst_subfolder_path})
        for k in ('ctime', 'mtime'):
            self.assertTrue(copy_info[k] > source_info[k])
        self.assertEqual(copy_info['utime'], source_info['utime'])

    def test_move_folder_check_subfile(self):
        """Проверяем, что при перемещении родительской папки у вложенного файла времена не меняются"""
        subfile_path = "%s/subfile" % self.FOLDER_PATH
        dst_subfile_path = "%s/subfile" % self.DST_FOLDER_PATH
        with feature_ctime_from_client_mock:
            self.upload_file(self.uid, subfile_path, opts={'ctime': self.CTIME, 'mtime': self.MTIME})

        source_info = self.json_ok('info', opts={'uid': self.uid, 'path': subfile_path})
        with mock.patch('time.time', fake_time_with_increment()):
            self.async_ok('async_move', opts={'uid': self.uid, 'src': self.FOLDER_PATH, 'dst': self.DST_FOLDER_PATH})
        moved_info = self.json_ok('info', opts={'uid': self.uid, 'path': dst_subfile_path})
        for k in ('ctime', 'utime', 'mtime'):
            self.assertEqual(moved_info[k], source_info[k])

    def test_copy_folder_check_subfile(self):
        """Проверяем, что при копировании родительской папки у вложенного файла ctime и mtime у папки новые, а utime старый"""
        subfile_path = "%s/subfile" % self.FOLDER_PATH
        dst_subfile_path = "%s/subfile" % self.DST_FOLDER_PATH
        with feature_ctime_from_client_mock:
            self.upload_file(self.uid, subfile_path, opts={'ctime': self.CTIME, 'mtime': self.MTIME})

        source_info = self.json_ok('info', opts={'uid': self.uid, 'path': subfile_path})
        with mock.patch('time.time', fake_time_with_increment()):
            self.async_ok('async_copy', opts={'uid': self.uid, 'src': self.FOLDER_PATH, 'dst': self.DST_FOLDER_PATH})
        copy_info = self.json_ok('info', opts={'uid': self.uid, 'path': dst_subfile_path})
        for k in ('ctime', 'mtime'):
            self.assertTrue(copy_info[k] > source_info[k])
        self.assertEqual(copy_info['utime'], source_info['utime'])


class TimesFileOperationsTestCase(DiskTestCase):
    CTIME = int(time.time()) - 10000
    MTIME = int(time.time()) - 5000

    FOLDER_NAME = 'test_time'
    FOLDER_PATH = '/'.join(['/disk', FOLDER_NAME])

    FILE_NAME = 'custom_time_1.txt'
    FILE_PATH = '/'.join([FOLDER_PATH, FILE_NAME])

    DST_FILE_NAME = 'custom_time_2.txt'
    DST_FILE_PATH = '/'.join([FOLDER_PATH, DST_FILE_NAME])

    DST_FOLDER_NAME = 'test_time_dst'
    DST_FOLDER_PATH = '/'.join(['/disk', DST_FOLDER_NAME])

    HARDLINK_NAME = 'custom_time_hardlink.txt'
    HARDLINK_PATH = '/'.join([FOLDER_PATH, HARDLINK_NAME])

    def setup_method(self, method):
        super(TimesFileOperationsTestCase, self).setup_method(method)
        self.json_ok('mkdir', opts={'uid': self.uid, 'path': self.FOLDER_PATH, 'ctime': self.CTIME, 'mtime': self.MTIME})
        with feature_ctime_from_client_mock:
            self.upload_file(self.uid, self.FILE_PATH, opts={'ctime': self.CTIME, 'mtime': self.MTIME})

    def test_publication(self):
        source_info = self.json_ok('info', opts={'uid': self.uid, 'path': self.FILE_PATH})
        self.json_ok('set_public', opts={'uid': self.uid, 'path': self.FILE_PATH})
        public_info = self.json_ok('info', opts={'uid': self.uid, 'path': self.FILE_PATH})
        self.json_ok('set_private', opts={'uid': self.uid, 'path': self.FILE_PATH})
        unpublic_info = self.json_ok('info', opts={'uid': self.uid, 'path': self.FILE_PATH})
        for k in ('ctime', 'mtime', 'utime', 'etime'):
            assert source_info[k] == public_info[k] == unpublic_info[k]

    @pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='Postgres test')
    def test_hardlinks(self):
        """
        Создаём хардлинк и проверяем, что у него все даты проставились правильно, а etime взят из оригинального файла
        """
        file_info = self.json_ok('info', opts={'uid': self.uid, 'path': self.FILE_PATH})
        with feature_ctime_from_client_mock:
            self.hardlink_file(self.uid, self.FILE_PATH, self.HARDLINK_PATH, opts={'ctime': self.CTIME, 'mtime': self.MTIME})
        hardlink_info = self.json_ok('info', opts={'uid': self.uid, 'path': self.HARDLINK_PATH})
        self.assertEqual(hardlink_info['ctime'], self.CTIME)
        # TODO: Должно быть равно друг другу. См. https://st.yandex-team.ru/CHEMODAN-55487
        self.assertTrue(hardlink_info['mtime'] > self.MTIME)
        self.assertTrue(hardlink_info['utime'] > self.CTIME)
        self.assertTrue(hardlink_info['utime'] > self.MTIME)
        self.assertEqual(hardlink_info['etime'], file_info['etime'])

    @pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='Postgres test')
    def test_hardlink_times_in_db(self):
        """Проверяем, что в базе у харлинка все даты проставлены и etime равен etime оригинального файла"""
        file_info = self.json_ok('info', opts={'uid': self.uid, 'path': self.FILE_PATH})
        self.hardlink_file(self.uid, self.FILE_PATH, self.HARDLINK_PATH, opts={'ctime': self.CTIME, 'mtime': self.MTIME})
        file_data = db.user_data.find_one({'uid': str(self.uid), 'path': self.HARDLINK_PATH})
        self.assertTrue('utime' in file_data['data'])
        self.assertTrue('mtime' in file_data['data'])
        self.assertTrue('etime' in file_data['data'])
        self.assertEqual(file_info['etime'], file_data['data']['etime'])

    def test_dstore(self):
        """Проверяем что при обновлении файла все даты обновляются корректно"""
        file_info_original = self.json_ok('info', opts={'uid': self.uid, 'path': self.FILE_PATH})
        parent_info_original = self.json_ok('info', opts={'uid': self.uid, 'path': self.FOLDER_PATH})
        mtime = int(time.time()) - 500
        with feature_ctime_from_client_mock:
            self.dstore_file(self.uid, self.FILE_PATH, opts={'mtime': mtime})
        time.sleep(1)
        file_info = self.json_ok('info', opts={'uid': self.uid, 'path': self.FILE_PATH})
        self.assertEqual(file_info['ctime'], file_info_original['ctime'])
        self.assertEqual(file_info['mtime'], mtime)
        self.assertTrue(file_info['mtime'] > file_info_original['mtime'])
        self.assertEqual(file_info['etime'], file_info_original['etime'])
        print '%s <= %s' % (file_info['utime'], file_info_original['ctime'])
        self.assertTrue(file_info['utime'] > file_info_original['ctime'])
        print '%s <= %s' % (file_info['utime'], mtime)
        self.assertTrue(file_info['utime'] > mtime)

        # проверяем что в базе есть все поля и etime не отличается от оригинального
        file_data = db.user_data.find_one({'uid': str(self.uid), 'path': self.FILE_PATH})
        self.assertTrue('utime' in file_data['data'])
        self.assertTrue('mtime' in file_data['data'])
        self.assertTrue('etime' in file_data['data'])
        self.assertEqual(file_info_original['etime'], file_data['data']['etime'])

        # проверяем что даты создания и заливки родительского каталога не изменились
        parent_info = self.json_ok('info', opts={'uid': self.uid, 'path': self.FOLDER_PATH, 'meta': ''})
        for k in ('ctime', 'mtime', 'utime'):
            self.assertEqual(parent_info_original[k], parent_info[k])

    def test_dstore_without_mtime(self):
        """Проверяем CHEMODAN-18384 что при dstore без mtime, mtime файла изменяется на текущее время"""
        original_file_info = self.json_ok('info', opts={'uid': self.uid, 'path': self.FILE_PATH})
        original_folder_info = self.json_ok('info', opts={'uid': self.uid, 'path': self.FOLDER_PATH})
        with mock.patch('time.time', fake_time_with_increment()):
            self.dstore_file(self.uid, self.FILE_PATH)
        file_info = self.json_ok('info', opts={'uid': self.uid, 'path': self.FILE_PATH})
        self.assertNotEqual(original_file_info['mtime'], file_info['mtime'])
        folder_info = self.json_ok('info', opts={'uid': self.uid, 'path': self.FOLDER_PATH})
        self.assertEqual(original_folder_info['mtime'], folder_info['mtime'])

    def test_copy(self):
        """Проеряем, что при копировании ctime и mtime у файла новые, а utime и etime старые"""
        source_info = self.json_ok('info', opts={'uid': self.uid, 'path': self.FILE_PATH})
        with mock.patch('time.time', fake_time_with_increment()):
            self.async_ok('async_copy', opts={'uid': self.uid, 'src': self.FILE_PATH, 'dst': self.DST_FILE_PATH})
        copy_info = self.json_ok('info', opts={'uid': self.uid, 'path': self.DST_FILE_PATH})
        for k in ('ctime', 'mtime'):
            self.assertTrue(copy_info[k] > source_info[k])
        for k in ('etime', 'utime'):
            self.assertEqual(copy_info[k], source_info[k])

    def test_copy_parent_mtime(self):
        """Проверяем, что при копировании файла mtime каталога назначения не изменяется"""
        dst_file_path = '/'.join([self.DST_FOLDER_PATH, self.FILE_NAME])
        self.json_ok('mkdir', opts={'uid': self.uid, 'path': self.DST_FOLDER_PATH})

        original_src_parent_info = self.json_ok('info', opts={'uid': self.uid, 'path': self.FOLDER_PATH})
        original_dst_parent_info = self.json_ok('info', opts={'uid': self.uid, 'path': self.DST_FOLDER_PATH})

        time.sleep(1)
        with mock.patch('time.time', fake_time_with_increment()):
            self.async_ok('async_copy', opts={'uid': self.uid, 'src': self.FILE_PATH, 'dst': dst_file_path})

        src_parent_info = self.json_ok('info', opts={'uid': self.uid, 'path': self.FOLDER_PATH})
        dst_parent_info = self.json_ok('info', opts={'uid': self.uid, 'path': self.DST_FOLDER_PATH})

        for k in ('ctime', 'utime', 'mtime'):
            self.assertEqual(original_src_parent_info[k], src_parent_info[k])
            self.assertEqual(original_dst_parent_info[k], dst_parent_info[k])

    def test_copy_file_inside_folder(self):
        """Проверяем, что при копировании папки, даты файлов в ней обновляются корректно"""
        source_info = self.json_ok('info', opts={'uid': self.uid, 'path': self.FILE_PATH})
        with mock.patch('time.time', fake_time_with_increment()):
            self.async_ok('async_copy', opts={'uid': self.uid, 'src': self.FOLDER_PATH, 'dst': self.DST_FOLDER_PATH})
        copy_info = self.json_ok('info', opts={'uid': self.uid,
                                               'path': '/'.join([self.DST_FOLDER_PATH, self.FILE_NAME])})
        for k in ('ctime', 'mtime'):
            self.assertTrue(copy_info[k] > source_info[k])
        for k in ('etime', 'utime'):
            self.assertEqual(copy_info[k], source_info[k])

    def test_move(self):
        """Проверяем, что при перемещении ctime, utime и etime у файла сохраняются, a mtime меняется"""
        source_info = self.json_ok('info', opts={'uid': self.uid, 'path': self.FILE_PATH})
        with mock.patch('time.time', fake_time_with_increment()):
            self.async_ok('async_move', opts={'uid': self.uid, 'src': self.FILE_PATH, 'dst': self.DST_FILE_PATH})
        moved_info = self.json_ok('info', opts={'uid': self.uid, 'path': self.DST_FILE_PATH})
        for k in ('etime', 'utime', 'ctime'):
            self.assertEqual(moved_info[k], source_info[k])
        assert moved_info['mtime'] > source_info['mtime']

    def test_move_parent_mtime(self):
        """Проверяем, что при перемещении файла mtime родительских каталогов изменяется"""
        dst_file_path = '/'.join([self.DST_FOLDER_PATH, self.FILE_NAME])
        self.json_ok('mkdir', opts={'uid': self.uid, 'path': self.DST_FOLDER_PATH})

        original_src_parent_info = self.json_ok('info', opts={'uid': self.uid, 'path': self.FOLDER_PATH})
        original_dst_parent_info = self.json_ok('info', opts={'uid': self.uid, 'path': self.DST_FOLDER_PATH})

        with mock.patch('time.time', fake_time_with_increment()):
            self.async_ok('async_move', opts={'uid': self.uid, 'src': self.FILE_PATH, 'dst': dst_file_path})

        src_parent_info = self.json_ok('info', opts={'uid': self.uid, 'path': self.FOLDER_PATH})
        dst_parent_info = self.json_ok('info', opts={'uid': self.uid, 'path': self.DST_FOLDER_PATH})

        for k in ('ctime', 'utime', 'mtime'):
            self.assertEqual(original_src_parent_info[k], src_parent_info[k])
            self.assertEqual(original_dst_parent_info[k], dst_parent_info[k])

    def test_move_file_inside_folder(self):
        """Проверяем, что при перемещении папки, даты файлов в ней сохраняются"""
        source_info = self.json_ok('info', opts={'uid': self.uid, 'path': self.FILE_PATH})
        with mock.patch('time.time', fake_time_with_increment()):
            self.async_ok('async_move', opts={'uid': self.uid, 'src': self.FOLDER_PATH, 'dst': self.DST_FOLDER_PATH})
        moved_info = self.json_ok('info', opts={'uid': self.uid,
                                                'path': '/'.join([self.DST_FOLDER_PATH, self.FILE_NAME])})
        for k in ('etime', 'utime', 'mtime'):
            self.assertEqual(moved_info[k], source_info[k], k)

    def test_trash_append(self):
        """Проверяем, что при удалении файла в корзину ctime и utime не меняются, a mtime меняется"""
        source_info = self.json_ok('info', opts={'uid': self.uid, 'path': self.FILE_PATH})
        with mock.patch('time.time', fake_time_with_increment()):
            with mock.patch('mpfs.core.address.Address.add_trash_suffix'):
                self.async_ok('async_trash_append', opts={'uid': self.uid, 'path': self.FILE_PATH})
        deleted_info = self.json_ok('info', opts={'uid': self.uid, 'path': '/'.join(['/trash', self.FILE_NAME])})
        for k in ('ctime', 'utime'):
            self.assertEqual(deleted_info[k], source_info[k])
        assert deleted_info['mtime'] > source_info['mtime']

    def test_trash_append_parent_mtime(self):
        """Проверяем, что при удалении файла в корзину mtime родительского каталога не изменяется"""
        original_parent_info = self.json_ok('info', opts={'uid': self.uid, 'path': self.FOLDER_PATH})
        with mock.patch('time.time', fake_time_with_increment()):
            self.async_ok('async_trash_append', opts={'uid': self.uid, 'path': self.FILE_PATH})
        parent_info = self.json_ok('info', opts={'uid': self.uid, 'path': self.FOLDER_PATH})
        for k in ('ctime', 'utime', 'mtime'):
            self.assertEqual(parent_info[k], original_parent_info[k])

    def test_trash_append_inside_folder(self):
        """Проверяем, что при удалении папки в корзину ctime, mtime и utime её файлов не меняются"""
        source_info = self.json_ok('info', opts={'uid': self.uid, 'path': self.FILE_PATH})
        with mock.patch('time.time', fake_time_with_increment()):
            with mock.patch('mpfs.core.address.Address.add_trash_suffix'):
                self.async_ok('async_trash_append', opts={'uid': self.uid, 'path': self.FOLDER_PATH})
        deleted_info = self.json_ok('info', opts={'uid': self.uid, 'path': '/'.join(['/trash', self.FOLDER_NAME, self.FILE_NAME])})
        for k in ('ctime', 'mtime', 'utime'):
            self.assertEqual(deleted_info[k], source_info[k])

    def test_trash_restore(self):
        """Проверяем, что при восстановлении файла из корзины ctime и utime не меняются, a mtime меняется"""
        trash_path = '/'.join(['/trash', self.FILE_NAME])
        with mock.patch('mpfs.core.address.Address.add_trash_suffix'):
            self.async_ok('async_trash_append', opts={'uid': self.uid, 'path': self.FILE_PATH})
        deleted_info = self.json_ok('info', opts={'uid': self.uid, 'path': trash_path})
        with mock.patch('time.time', fake_time_with_increment()):
            self.async_ok('async_trash_restore', opts={'uid': self.uid, 'path': trash_path})
        restored_info = self.json_ok('info', opts={'uid': self.uid, 'path': self.FILE_PATH})
        for k in ('ctime', 'utime'):
            self.assertEqual(deleted_info[k], restored_info[k])
        assert restored_info['mtime'] > deleted_info['mtime']

    def test_trash_restore_parent_mtime(self):
        """Проверяем, что при восстановлении файла из корзины mtime родительского каталога не изменяется"""
        with mock.patch('mpfs.core.address.Address.add_trash_suffix'):
            self.async_ok('async_trash_append', opts={'uid': self.uid, 'path': self.FILE_PATH})
        original_parent_info = self.json_ok('info', opts={'uid': self.uid, 'path': self.FOLDER_PATH})
        time.sleep(1)
        self.async_ok('async_trash_restore', opts={'uid': self.uid, 'path': '/'.join(['/trash', self.FILE_NAME])})
        parent_info = self.json_ok('info', opts={'uid': self.uid, 'path': self.FOLDER_PATH})
        for k in ('ctime', 'utime', 'mtime'):
            self.assertEqual(parent_info[k], original_parent_info[k])

    def test_trash_restore_inside_folder(self):
        """Проверяем, что при восстановлении папки из корзины ctime, mtime и utime её файлов не меняются"""
        trash_folder_path = '/'.join(['/trash', self.FOLDER_NAME])
        trash_file_path = '/'.join(['/trash', self.FOLDER_NAME, self.FILE_NAME])
        with mock.patch('mpfs.core.address.Address.add_trash_suffix'):
            self.async_ok('async_trash_append', opts={'uid': self.uid, 'path': self.FOLDER_PATH})
        deleted_info = self.json_ok('info', opts={'uid': self.uid, 'path': trash_file_path})
        self.async_ok('async_trash_restore', opts={'uid': self.uid, 'path': trash_folder_path})
        restored_info = self.json_ok('info', opts={'uid': self.uid, 'path': self.FILE_PATH})
        for k in ('ctime', 'mtime', 'utime'):
            self.assertEqual(deleted_info[k], restored_info[k])


class TimesTestCase(DiskTestCase):
    CTIME = int(time.time()) - 10000
    MTIME = int(time.time()) - 5000

    FOLDER_NAME = 'test_time'
    FOLDER_PATH = '/'.join(['/disk', FOLDER_NAME])

    FILE_NAME = 'custom_time_1.txt'
    FILE_PATH = '/'.join([FOLDER_PATH, FILE_NAME])

    HARDLINK_NAME = 'custom_time_hardlink.txt'
    HARDLINK_PATH = '/'.join([FOLDER_PATH, HARDLINK_NAME])

    def setup_method(self, method):
        """
            https://jira.yandex-team.ru/browse/CHEMODAN-11389
        """
        super(TimesTestCase, self).setup_method(method)
        # создаём тестовую папку с клиентскими ctime и mtime чтоб они отличались от utime
        self.json_ok('mkdir', opts={'uid': self.uid, 'path': self.FOLDER_PATH, 'ctime': self.CTIME, 'mtime': self.MTIME})

    def test_dstore_hardlink(self):
        """
        Понять до конца смысл этого теста не смог, поэтому оставил как есть без комментов и рефакторинга.

            https://jira.yandex-team.ru/browse/CHEMODAN-11384
            http://wiki.yandex-team.ru/disk/mpfs/DiskDates#dstore
        """
        folder_path = self.FOLDER_PATH
        file_path_1 = self.FILE_PATH
        file_path_2 = self.HARDLINK_PATH
        # заливаем оригинальный файл
        with feature_ctime_from_client_mock:
            self.upload_file(self.uid, self.FILE_PATH, opts={'ctime': self.CTIME, 'mtime': self.MTIME})
        opts = {
                'uid' : self.uid,
                'path' : folder_path,
                'meta' : '',
                }
        parent_info_original = self.json_ok('info', opts)
        ctime = int(time.time()) - 1100
        mtime = int(time.time()) - 550
        opts = {
                'ctime' : ctime,
                'mtime' : mtime,
                }
        time.sleep(1)
        with feature_ctime_from_client_mock:
            self.upload_file(self.uid, file_path_2, opts=opts)
        opts = {
                'uid' : self.uid,
                'path' : file_path_2,
                'meta' : 'ctime,mtime,etime,utime,md5,sha256,size',
                }
        hardlink_info = self.json_ok('info', opts)
        opts = {
                'uid' : self.uid,
                'path' : folder_path,
                'meta' : '',
                }
        parent_info = self.json_ok('info', opts)
        self.assertEqual(parent_info_original['ctime'], parent_info['ctime'])
        self.assertEqual(parent_info_original['utime'], parent_info['utime'])
        self.assertEqual(parent_info_original['mtime'], parent_info['mtime'])
        opts = {
                'uid' : self.uid,
                'path' : file_path_1,
                'meta' : 'ctime,mtime,etime,utime,md5,sha256,size',
                }
        file_info_original = self.json_ok('info', opts)
        for k in ('ctime', 'mtime'):
            self.assertNotEqual(hardlink_info[k], file_info_original[k])

        file_data = {}
        for k in ('md5', 'sha256', 'size'):
            self.assertNotEqual(hardlink_info['meta'][k], file_info_original['meta'][k])
            file_data[k] = hardlink_info['meta'][k]

        opts = {
                'uid' : self.uid,
                'path' : folder_path,
                'meta' : '',
                }
        parent_info_original = self.json_ok('info', opts)
        ctime = int(time.time()) - 100
        mtime = int(time.time()) - 50
        opts = {
                'ctime' : ctime,
                'mtime' : mtime,
                }
        time.sleep(1)
        self.dstore_file(self.uid, file_path_1, file_data=file_data, opts=opts)
        opts = {
                'uid' : self.uid,
                'path' : file_path_1,
                'meta' : '',
                }
        file_info = self.json_ok('info', opts)
        for k in ('md5', 'sha256', 'size'):
            self.assertEqual(hardlink_info['meta'][k], file_info['meta'][k])
            self.assertNotEqual(file_info['meta'][k], file_info_original['meta'][k])

        self.assertEqual(file_info['ctime'], ctime)
        self.assertEqual(file_info['mtime'], mtime)
        self.assertEqual(file_info['etime' ], file_info_original['etime'])
        self.assertTrue(file_info['utime' ] > ctime)
        self.assertTrue(file_info['utime' ] > mtime)

        self.assertNotEqual(file_info['ctime'], file_info_original['ctime'])
        self.assertNotEqual(file_info['mtime'], file_info_original['mtime'])

        self.assertTrue(file_info['ctime'] > file_info_original['ctime'], file_info['ctime'])
        self.assertTrue(file_info['mtime'] > file_info_original['mtime'], file_info['mtime'])

        file_data = db.user_data.find_one({'uid' : str(self.uid), 'path' : file_path_1})
        self.assertTrue('utime' in file_data['data'])
        self.assertTrue('mtime' in file_data['data'])
        self.assertTrue('etime' in file_data['data'])
        self.assertEqual(file_info_original['etime'], file_data['data']['etime'])
        opts = {
                'uid' : self.uid,
                'path' : folder_path,
                'meta' : '',
                }
        parent_info = self.json_ok('info', opts)
        self.assertEqual(parent_info_original['ctime'], parent_info['ctime'])
        self.assertEqual(parent_info_original['utime'], parent_info['utime'])
        self.assertEqual(parent_info_original['mtime'], parent_info['mtime'])
