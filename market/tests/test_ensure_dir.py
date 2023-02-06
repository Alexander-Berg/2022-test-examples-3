# -*- coding: utf-8 -*-

import shutil
import os

import pytest
import yatest

from market.idx.pylibrary.disk_queue.disk_queue import ensure_dir


@pytest.yield_fixture()
def dir_path():
    path = yatest.common.test_output_path('tmp')
    try:
        os.mkdir(path)
        yield path
    finally:
        shutil.rmtree(path)


def test_normal(dir_path):
    path = os.path.join(dir_path, 'foo', 'bar')

    ensure_dir(path)
    assert os.path.exists(path)

    ensure_dir(path)
    assert os.path.exists(path)


def test_bogus():
    path = '/bogus'

    with pytest.raises(OSError):
        ensure_dir(path)
