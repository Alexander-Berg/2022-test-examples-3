# -*- coding: utf-8 -*-

import shutil
import os

import pytest
import yatest

from market.idx.pylibrary.disk_queue.disk_queue import global_lock


@pytest.yield_fixture()
def dir_path():
    path = yatest.common.test_output_path('tmp')
    try:
        os.mkdir(path)
        yield path
    finally:
        shutil.rmtree(path)


def test_non_existing(dir_path):
    bogus = os.path.join(dir_path, 'bogus')
    with pytest.raises(OSError):
        with global_lock(bogus):
            pass


def test_normal(dir_path):
    path = os.path.join(dir_path, 'foo')
    os.mkdir(path)

    # fresh lock file
    with global_lock(path):
        assert os.path.isfile(os.path.join(path, 'lock'))

    # repeated locking should work
    with global_lock(path):
        assert os.path.isfile(os.path.join(path, 'lock'))
