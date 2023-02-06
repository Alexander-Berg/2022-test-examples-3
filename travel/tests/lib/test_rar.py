# -*- coding: utf-8 -*-

import os.path

from common.tester.skippers import skip_in_arcadia
from travel.rasp.admin.lib.unittests.testcase import TestCase
from travel.rasp.admin.lib.unrar import unpack_rar_file


@skip_in_arcadia
class TestRar(TestCase):
    def testRar(self):
        rar_file = os.path.join('travel', 'rasp', 'admin', 'tests', 'lib', 'data', 'test_rar', 'test.rar')

        with open(rar_file, 'rb') as f:
            file_map = unpack_rar_file(f)

        self.assertIn('a.txt', file_map)
        self.assertIn('subdir/b.txt', file_map)
