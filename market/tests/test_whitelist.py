# -*- coding: utf-8 -*-

import errno
import os
import shutil

import pytest
import yatest

import market.idx.pylibrary.mindexer_core.whitelist.whitelist as whitelist


def touch(*path_components, **kwargs):
    directory = kwargs.get('directory', False)
    path = os.path.join(*path_components)
    try:
        os.makedirs(os.path.dirname(path))
    except OSError as exc:
        if exc.errno != errno.EEXIST:
            raise

    if directory:
        os.mkdir(path)
    else:
        open(path, 'w').close()


def find(root):
    for dir_path, _, file_names in os.walk(root):
        if dir_path != root:
            yield os.path.relpath(dir_path, root)
        for file_name in file_names:
            path = os.path.join(dir_path, file_name)
            yield os.path.relpath(path, root)


@pytest.yield_fixture()
def root_dir():
    """Fixture for the directory to be cleaned.

               root(d)
              /      \\
            foo(d)  awol(d)
           /    \\    |
        bar(d)  baz   x
          |
        baz(d)
    """
    root = yatest.common.test_output_path('root')
    try:
        touch(root, 'foo', 'bar', 'baz', directory=True)
        touch(root, 'foo', 'baz')
        touch(root, 'awol', 'x')
        yield root
    finally:
        shutil.rmtree(root, ignore_errors=True)


def test_path_prefixes_relative():
    assert {'a/b', 'a', '.'} == set(whitelist.path_prefixes('a/b/c'))


def test_path_prefixes_empty():
    assert {'.'} == set(whitelist.path_prefixes(''))


def test_greylist_sanity():
    paths = [
        'foo/bar',
        'awol',
    ]

    expected_greylist = {
        '.',
        'foo',
    }

    actual_greylist = whitelist.greylist(paths)
    assert expected_greylist == actual_greylist


def test_clean_dir_all(root_dir):
    whitelist.clean_dir(root_dir)
    assert not os.path.exists(root_dir)


def test_clean_dir_sane(root_dir):
    whitelist.clean_dir(root_dir, whitelist_paths=['foo/baz', 'awol'])
    expected_files = set(['foo', 'foo/baz', 'awol', 'awol/x'])
    actual_files = set(find(root_dir))
    assert expected_files == actual_files


def test_clean_dir_link(root_dir):
    os.symlink(
        os.path.join(root_dir, 'foo'),
        os.path.join(root_dir, 'awol', 'y'),
    )

    whitelist.clean_dir(root_dir, whitelist_paths=['awol/x', 'foo/baz'])
    expected_files = set(['foo', 'foo/baz', 'awol', 'awol/x'])
    actual_files = set(find(root_dir))
    assert expected_files == actual_files


def test_clean_dir_non_existent(root_dir):
    whitelist.clean_dir(root_dir, whitelist_paths=['bogus/xyz'])
    assert not os.path.exists(root_dir)


def test_clean_dir_move(root_dir):
    def clean_func(path):
        os.rename(path, path + '.cleaned')

    whitelist.clean_dir(
        root_dir,
        whitelist_paths=['foo/bar/baz'],
        dir_clean_func=clean_func,
        file_clean_func=clean_func
    )

    expected_files = set([
        'foo',
        'foo/bar',
        'foo/bar/baz',
        'foo/baz.cleaned',
        'awol.cleaned',
        'awol.cleaned/x',
    ])

    actual_files = set(find(root_dir))
    assert expected_files == actual_files


def test_clean_dir_dot(root_dir):
    whitelist.clean_dir(root_dir, whitelist_paths=['.'])
    expected_files = set([
        'foo',
        'foo/baz',
        'foo/bar',
        'foo/bar/baz',
        'awol',
        'awol/x'
    ])
    actual_files = set(find(root_dir))
    assert expected_files == actual_files


def test_clean_dir_dot2(root_dir):
    whitelist.clean_dir(root_dir, whitelist_paths=['awol/.'])
    expected_files = set([
        'awol',
        'awol/x'
    ])
    actual_files = set(find(root_dir))
    assert expected_files == actual_files


def test_clean_dir_dotdot(root_dir):
    whitelist.clean_dir(root_dir, whitelist_paths=['foo/bar/baz/..'])
    expected_files = set([
        'foo',
        'foo/bar',
        'foo/bar/baz'
    ])
    actual_files = set(find(root_dir))
    assert expected_files == actual_files
