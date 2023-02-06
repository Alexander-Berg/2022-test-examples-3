# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function


from builtins import str
from datetime import datetime, timedelta

import pytest

from travel.rasp.library.python.common23.utils.files.fileutils import smart_split_ext, get_project_relative_path, get_file_modify_dt


@pytest.mark.parametrize('filepath, name, ext', [
    ('/a/a.txt', '/a/a', '.txt'),
    ('/a/a.xml.gz', '/a/a', '.xml.gz'),
    ('a.xml.gz', 'a', '.xml.gz'),
    ('a.txt', 'a', '.txt'),
])
def test_smart_split_ext(filepath, name, ext):
    assert name, ext == smart_split_ext(filepath)


def test_get_project_relative_path():
    assert get_project_relative_path(__file__) == 'utils/tests/files/test_fileutils.py'


def test_get_file_modify_dt(tmpdir):
    some_file = tmpdir / 'some.txt'

    assert get_file_modify_dt(str(some_file)) is None

    now = datetime.utcnow()
    some_file.write('content42')
    modify_time = get_file_modify_dt(str(some_file))
    assert now - timedelta(seconds=3) <= modify_time <= now + timedelta(seconds=3)
