# coding: utf-8

import os.path
from unittest import TestCase

from market.pylibrary import common


class TestMakeDir(TestCase):

    def test_make_dir(self):

        with self.assertRaises(RuntimeError):
            with common.temp.make_dir() as temp_dir:
                file_path = os.path.join(temp_dir, 'file00')
                dir_path = os.path.join(temp_dir, 'dir00')
                with open(file_path, 'w') as fobj:
                    fobj.write('test val')
                os.makedirs(dir_path)
                raise RuntimeError

        self.assertFalse(os.path.exists(temp_dir))
        self.assertFalse(os.path.exists(dir_path))
        self.assertFalse(os.path.exists(file_path))
