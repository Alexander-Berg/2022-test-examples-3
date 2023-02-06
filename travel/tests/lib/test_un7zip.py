# -*- coding: utf-8 -*-

import os.path
import unittest

from common.tester.skippers import skip_in_arcadia
from travel.rasp.admin.lib.fileutils import remove_tmp_dir
from travel.rasp.admin.lib.tmpfiles import get_tmp_dir
from travel.rasp.admin.lib.un7zip import Un7zipError, un7zip, get_7zip_filedict


def get_test_file_path(filename):
    return os.path.join('travel', 'rasp', 'admin', 'tests', 'lib', 'data', 'un7zip', filename)


@skip_in_arcadia
class Un7ZipFileTest(unittest.TestCase):
    def setUp(self):
        self.tmp_dir = get_tmp_dir('test/unpack_file')

    def testNot7Zip(self):
        filepath = get_test_file_path('not_arch.7z')

        self.assertRaises(Un7zipError, un7zip, filepath, self.tmp_dir)

        remove_tmp_dir(self.tmp_dir)

    def testUn7Zip(self):
        filepath = get_test_file_path('arc_with_file.7z')

        un7zip(filepath, self.tmp_dir)

        self.assertTrue(os.path.exists(os.path.join(self.tmp_dir, 'file')))

        remove_tmp_dir(self.tmp_dir)

    def testGet7zipFiledict(self):
        filepath = get_test_file_path('arch_with_file_and_folder.7z')

        filedict = get_7zip_filedict(filepath, self.tmp_dir)

        self.assertIn('file', filedict)
        self.assertIn('folder/file', filedict)
        self.assertTrue(os.path.exists(filedict['file']))
        self.assertTrue(os.path.exists(filedict['folder/file']))
