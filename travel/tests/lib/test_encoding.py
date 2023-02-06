# -*- coding: utf-8 -*-
import os.path
import unittest

from travel.rasp.admin.lib.encoding import detect_file_encoding_by_path


def ge_test_filepath(filename):
    return os.path.join('travel', 'rasp', 'admin', 'tests', 'lib', 'data', 'encoding', filename)


def get_test_file_obj(filename):
    return open(ge_test_filepath(filename))


class DetectEncodingTest(unittest.TestCase):

    def testDetectFileEncodingByPath(self):
        self.assertEqual('utf-8', detect_file_encoding_by_path(ge_test_filepath('test_detect_encoding_utf8.txt')))
        self.assertEqual('cp1251', detect_file_encoding_by_path(ge_test_filepath('test_detect_encoding_cp1251.txt')))

