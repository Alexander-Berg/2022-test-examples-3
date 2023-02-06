# -*- coding: utf-8 -*-

import os.path
import unittest

from common.tester.skippers import skip_in_arcadia
from travel.rasp.admin.lib.tmpfiles import get_tmp_dir
from travel.rasp.admin.lib.unzip import unpack_zip_file
from travel.rasp.admin.lib.fileutils import remove_tmp_dir


def get_test_file_obj(filename):
    filepath = os.path.join('travel', 'rasp', 'admin', 'tests', 'lib', 'data', 'unzip', filename)
    return open(filepath)


@skip_in_arcadia
class UnpuckZipFileTest(unittest.TestCase):
    def setUp(self):
        self.tmp_dir = get_tmp_dir('test/unpack_file')

    def testUnpuckZipFile(self):
        fileobj = get_test_file_obj('test_unpack_file.zip')
        file_map = unpack_zip_file(fileobj, self.tmp_dir)

        self.assertIn('test.file', file_map)
        self.assertEqual(file_map['test.file'], os.path.join(self.tmp_dir, 'test.file'))

        remove_tmp_dir(self.tmp_dir)

    def testUnpuckZipFileWithSubdirs(self):
        fileobj = get_test_file_obj('test_unpack_with_subdirs.zip')
        file_map = unpack_zip_file(fileobj, self.tmp_dir)

        self.assertIn('test.file', file_map)
        self.assertIn('empty/', file_map)
        self.assertIn('__msa/test.file', file_map)
        self.assertEqual(file_map['test.file'], os.path.join(self.tmp_dir, 'test.file'))

        remove_tmp_dir(self.tmp_dir)

