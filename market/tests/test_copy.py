# coding: utf-8

import hashlib
import os
import os.path
import re

from unittest import TestCase

from market.pylibrary import common


class TestCopy(TestCase):

    def test_copy_file(self):
        with common.temp.make_dir() as temp_dirpath:
            file_path = os.path.join(temp_dirpath, 'test00')
            with open(file_path, 'w') as fobj:
                fobj.write('test data')

            dest_path = os.path.join(temp_dirpath, 'dest00')
            common.copy.copy_file(file_path, dest_path, try_hardlink=True)
            self.assertEqual(
                _file_id(file_path, with_inode=True),
                _file_id(dest_path, with_inode=True),
            )

            dest_path = os.path.join(temp_dirpath, 'dir00', 'dest01')
            common.copy.copy_file(file_path, dest_path, try_hardlink=False)
            self.assertEqual(
                _file_id(file_path, with_inode=False),
                _file_id(dest_path, with_inode=False),
            )

    def test_copy_dir(self):
        with common.temp.make_dir() as root_dirpath:
            orig_dirpath = os.path.join(root_dirpath, 'orig')
            os.makedirs(orig_dirpath)
            with open(os.path.join(orig_dirpath, '0.txt'), 'w') as fobj:
                fobj.write('test data 0')
            with open(os.path.join(orig_dirpath, '1.txt'), 'w') as fobj:
                fobj.write('test data 1')

            os.makedirs(os.path.join(orig_dirpath, 'dir0'))
            with open(os.path.join(orig_dirpath, 'dir0', '0.txt'), 'w') as fobj:
                fobj.write('new dir: test data 0')

            dest_dirpath = os.path.join(root_dirpath, 'copy0')
            common.copy.copy_dir(orig_dirpath, dest_dirpath, try_hardlink=True)
            expected = _list_dir(orig_dirpath, with_inode=True)
            actual = _list_dir(dest_dirpath, with_inode=True)
            self.assertEqual(actual, expected)

            dest_dirpath = os.path.join(root_dirpath, 'copy1')
            common.copy.copy_dir(orig_dirpath, dest_dirpath, try_hardlink=False)
            expected = _list_dir(orig_dirpath, with_inode=False)
            actual = _list_dir(dest_dirpath, with_inode=False)
            self.assertEqual(actual, expected)

            dest_dirpath = os.path.join(root_dirpath, 'copy3')
            regexps = [re.compile('/1.txt')]
            common.copy.copy_dir(orig_dirpath, dest_dirpath, ignore_paths=regexps)
            expected = {key: val for key, val in list(expected.items()) if not key.endswith('1.txt')}
            actual = _list_dir(dest_dirpath, with_inode=False)
            self.assertEqual(actual, expected)


def _file_id(file_path, with_inode=False):
    with open(file_path) as fobj:
        data = fobj.read()
    return (
        hashlib.md5(data.encode('utf-8')).hexdigest(),
        os.stat(file_path).st_ino if with_inode else None,
    )


def _list_dir(root_dir, with_inode=False):
    result = {}
    for dirpath, _, filenames in os.walk(root_dir):
        for filename in filenames:
            file_path = os.path.join(dirpath, filename)
            rel_path = os.path.relpath(file_path, root_dir)
            result[rel_path] = _file_id(file_path)
    return result
