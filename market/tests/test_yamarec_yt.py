# -*- coding: utf-8 -*-

import json
import os
import shutil
import unittest
from datetime import date

from market.pylibrary.yatestwrap.yatestwrap import source_path

from getter.exceptions import Error
from getter.yamarec_config_yt import YamarecConfig


class Test(unittest.TestCase):

    ROOTDIR = os.path.join(os.getcwd(), 'tmp')

    def setUp(self):
        shutil.rmtree(self.ROOTDIR, ignore_errors=True)
        os.makedirs(self.ROOTDIR)

    def tearDown(self):
        shutil.rmtree(self.ROOTDIR, ignore_errors=True)

    def test_parsing(self):
        good_filepath = source_path('market/getter/tests/data/yamarec_good_yt.conf')
        good_config = YamarecConfig(date(year=2021, month=9, day=1))
        good_config.load(good_filepath)
        data_info = good_config.get_data_info()
        self.assertTrue('report' in data_info)
        self.assertEqual(len(data_info['report']), 22)
        good_config.validate()

    def run_bad_config(self, bad_filepath, message):
        bad_config = YamarecConfig(date(year=2021, month=9, day=1))
        bad_config.load(bad_filepath)
        data_info = bad_config.get_data_info()
        self.assertEqual(data_info, {})
        try:
            bad_config.validate()
        except Error as error:
            self.assertTrue(str(error).startswith(message))

    def test_bad_backend(self):
        self.run_bad_config(
            source_path('market/getter/tests/data/yamarec_bad_backend_yt.conf'),
            'bad backends')

    def test_patching(self):
        source = source_path('market/getter/tests/data/yamarec_good_yt.conf')
        target = os.path.join(self.ROOTDIR, 'yamarec.good.conf.patched')
        config = YamarecConfig(date(year=2021, month=9, day=1))
        try:
            config.load(source)
            config.dump_patched(target)
            with open(target, "r") as stream:
                target_json = json.load(stream)
            etalon = source_path('market/getter/tests/data/yamarec_good_yt.conf.patched.etalon')
            with open(etalon, "r") as stream:
                etalon_json = json.load(stream)
            self.assertEqual(
                json.dumps(etalon_json, sort_keys=True),
                json.dumps(target_json, sort_keys=True))
        finally:
            try:
                os.remove(target)
            except OSError:
                pass


if __name__ == '__main__':
    unittest.main()
