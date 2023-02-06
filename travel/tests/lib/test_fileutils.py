# -*- coding: utf-8 -*-

import os.path
import unittest

from travel.avia.admin.lib.fileutils import remove_tmp_dir, get_relative_path
from travel.avia.admin.lib.tmpfiles import get_tmp_dir


class RemoveTmpDirTest(unittest.TestCase):

    def testRemoveTmpDir(self):
        tmp_dir = get_tmp_dir('test/tmp/dir')
        if not os.path.exists(tmp_dir):
            os.makedirs(tmp_dir)

        test_file = os.path.join(tmp_dir, 'test.file')

        open(test_file, 'w').close()

        remove_tmp_dir(tmp_dir)

        self.assertFalse(os.path.exists(test_file))
        self.assertFalse(os.path.exists(tmp_dir))

        remove_tmp_dir(tmp_dir)

    def testGetRelativePath(self):
        self.assertEqual("scripts/schedule.py", get_relative_path('./scripts/schedule.py', '.'))
        self.assertEqual("scripts/schedule.py", get_relative_path('./scripts/../scripts/schedule.py', '.'))
