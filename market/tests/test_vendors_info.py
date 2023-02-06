# -*- coding: utf-8 -*-

import logging
import os
import shutil
import subprocess
import unittest

from market.pylibrary.yatestwrap.yatestwrap import source_path

from getter import logadapter
from getter import vendors_info


DATA_DIR = 'market/getter/tests/data'
TMP_DIR = os.path.join(os.getcwd(), 'tmp')

logadapter.init_logger()


def get_source_path(path):
    return source_path(os.path.join(DATA_DIR, path))


class TestGenerationVendorsInfo(unittest.TestCase):
    def setUp(self):
        shutil.rmtree(TMP_DIR, ignore_errors=True)
        os.makedirs(TMP_DIR)

    def tearDown(self):
        shutil.rmtree(TMP_DIR, ignore_errors=True)

    def test_generation(self):
        output_filename = 'vendors-info.xml'
        output_file_path = os.path.join(TMP_DIR, output_filename)
        vendors_info.generate_vendors_info(
            output_file_path,
            get_source_path(vendors_info.VENDORS_FILENAME),
            get_source_path(vendors_info.RECOMMENDED_SHOPS_FILENAME))
        expectation = get_source_path(output_filename)

        process = subprocess.Popen(
            'diff -Nau %s %s' % (expectation, output_file_path), stdout=subprocess.PIPE, shell=True)
        diff_output, _ = process.communicate()
        if process.returncode != 0:
            logging.getLogger().error("\n%s", diff_output)
        self.assertEqual(process.returncode, 0)


if __name__ == '__main__':
    unittest.main()
