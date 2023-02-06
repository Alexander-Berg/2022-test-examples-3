# coding: utf-8

import datetime
import os

import util


def update_mtime(path, mtime):
    os.utime(str(path), (mtime, mtime))


def test_list_files(tmpdir):
    files = [
        ('core_watcher.timestamp', 1630244000),
        ('core_1', 1630244002),
        ('core_2', 1630244001),
    ]
    for file_name, mtime in files:
        test_file = tmpdir / file_name
        test_file.write('')
        update_mtime(test_file, mtime)
    result = util.list_files(str(tmpdir))
    assert len(result) == 2
    it = iter(result)
    assert next(it) == 'core_2'
    assert next(it) == 'core_1'


def test_remove_old_files(tmpdir):
    total_files = 4  # 4 files for 1 bytes
    max_core_age_hours = 1
    expected_remain_files = 2
    for index in range(total_files):
        test_file = tmpdir / f'core_{index}'
        test_file.write('a')
        mtime = datetime.datetime.now() - datetime.timedelta(minutes=40 * index)
        update_mtime(test_file, mtime.timestamp())
    util.remove_old_files(str(tmpdir), max_core_age_hours)
    remain_files = util.list_files(str(tmpdir))
    assert len(remain_files) == expected_remain_files
    assert 'core_0' in remain_files
    assert 'core_1' in remain_files
