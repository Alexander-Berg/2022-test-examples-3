# coding: utf-8

import os.path
import tarfile
from datetime import datetime
from os import makedirs

import pytest

from travel.rasp.admin.lib import tmpfiles
from travel.rasp.admin.scripts.rotate_temporary_data import OldFilesCleaner
from travel.rasp.admin.scripts.utils.import_file_storage import get_type_path
from common.tester.skippers import not_macos
from common.tester.utils.datetime import replace_now
from common.tester.utils.replace_setting import replace_setting


not_macos_file_name_1251 = not_macos(reason='''
    Can't run on MacOS. Даный тест зависит от возможности файловой системы использовать имена в кодировке CP1251.'''
)


@pytest.yield_fixture
def temporary_data_dirs():
    with tmpfiles.temporary_directory() as data_dir, \
            tmpfiles.temporary_directory() as archive_dir, \
            replace_setting('DATA_PATH', data_dir):
        yield data_dir, archive_dir


@not_macos_file_name_1251
@replace_now('2000-01-01 00:00:00')
def test_compress_old_tablo_data(temporary_data_dirs):
    data_dir, archive_dir = temporary_data_dirs

    cleaner = OldFilesCleaner()
    cleaner.archive_path = archive_dir
    cleaner.prepare()

    date_dir = os.path.join(get_type_path('tablo'), 'test_supplier', '1999-01-01')
    makedirs(date_dir)

    # в архив должны попадать файлы с названиями в любых кодировках (RASPADMIN-1264)
    with open(os.path.join(date_dir, u'файл').encode('cp1251'), 'w'):
        pass

    cleaner.compress_old_tablo_data()

    archive_name = os.path.join(archive_dir, '19990101.tar.gz')
    assert os.path.exists(archive_name)
    with tarfile.open(archive_name) as archive:
        assert u'test_supplier/1999-01-01/файл'.encode('cp1251') in archive.getnames()
    assert not os.path.exists(date_dir)


def test_clean_old_tablo(temporary_data_dirs):
    data_dir, archive_dir = temporary_data_dirs

    cleaner = OldFilesCleaner()
    cleaner.tablo_point_of_no_return_dt = datetime(2000, 1, 1)
    cleaner.archive_path = archive_dir
    cleaner.prepare()

    old_archive_name = os.path.join(archive_dir, '19991231.tar.gz')
    new_archive_name = os.path.join(archive_dir, '20000102.tar.gz')
    with open(old_archive_name, 'w'), open(new_archive_name, 'w'):
        pass

    cleaner.clean_old_tablo()

    assert not os.path.exists(old_archive_name)
    assert os.path.exists(new_archive_name)


@not_macos_file_name_1251
def test_clean_old_schdule(temporary_data_dirs):
    data_dir, archive_dir = temporary_data_dirs

    cleaner = OldFilesCleaner()
    cleaner.schedule_point_of_no_return_dt = datetime(2000, 1, 1)
    cleaner.archive_path = archive_dir
    cleaner.prepare()

    supplier_dir = os.path.join(get_type_path('schedule'), 'test_supplier')
    old_data_dir = os.path.join(supplier_dir, '1999-12-31')
    new_data_dir = os.path.join(supplier_dir, '2000-01-02')

    package_dir = os.path.join(supplier_dir, '33344')

    old_package_data_dir = os.path.join(package_dir, '1999-12-31')
    new_package_data_dir = os.path.join(package_dir, '2000-01-02')

    makedirs(old_data_dir)
    makedirs(new_data_dir)
    makedirs(old_package_data_dir)
    makedirs(new_package_data_dir)

    # должны удаляться файлы с названиями в любых кодировках (RASPADMIN-1264)
    with open(os.path.join(old_data_dir, u'файл').encode('cp1251'), 'w'):
        pass

    cleaner.clean_old_schedule()

    assert not os.path.exists(old_data_dir)
    assert os.path.exists(new_data_dir)
    assert not os.path.exists(old_package_data_dir)
    assert os.path.exists(new_package_data_dir)
