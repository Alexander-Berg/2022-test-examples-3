# -*- coding: utf-8 -*-

import fcntl
import os.path

import pytest

from travel.avia.admin.lib.lock import acquire_exclusive_lock
from travel.avia.admin.lib.tmpfiles import clean_temp, get_tmp_filepath, get_tmp_dir


@clean_temp
def test_acquire_exclusive_lock_on_open_file():
    """
    Проверяем, что можем получить лок на открытый на запись файл
    """
    tmpfile = get_tmp_filepath()
    with open(tmpfile, 'w'):
        with acquire_exclusive_lock(tmpfile):
            pass


@clean_temp
def test_acquire_exclusive_lock_on_not_existing_file():
    """
    Проверяем, что можем получить лок на несуществующий файл
    """
    tmpfile = os.path.join(get_tmp_dir(), 'some.lock')

    assert not os.path.exists(tmpfile)

    with acquire_exclusive_lock(tmpfile):
        pass


@clean_temp
def test_acquire_exclusive_lock():
    """
    Проверяем, что лок захвачен
    """
    tmpfile = get_tmp_filepath()
    with acquire_exclusive_lock(tmpfile):
        with open(tmpfile, mode='w') as fp:
            with pytest.raises(IOError):
                fcntl.flock(fp.fileno(), fcntl.LOCK_EX + fcntl.LOCK_NB)


@clean_temp
def test_release_exclusive_lock():
    """
    Проверяем, что лок отпущен
    """
    tmpfile = get_tmp_filepath()
    with acquire_exclusive_lock(tmpfile):
        pass

    with open(tmpfile, mode='w') as fp:
        fcntl.flock(fp.fileno(), fcntl.LOCK_EX + fcntl.LOCK_NB)
