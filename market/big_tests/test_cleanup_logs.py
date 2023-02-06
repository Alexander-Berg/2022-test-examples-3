# -*- coding: utf-8 -*-

import os
import shutil
import unittest

from market.idx.datacamp.parser.lib.parser_engine.feed_processor import cleanup_logs


temp_dir = './tmp'


def create_file(name):
    open(os.path.join(temp_dir, name), 'a').close()


class TestCleanUpLogs(unittest.TestCase):
    def setUp(self):
        os.mkdir(temp_dir)
        create_file('process_20140214_0405.log')
        create_file('process_20140214_0712.log')
        create_file('process_20140214_1712.log')

        create_file('process_20140213_0932.log')
        create_file('process_20140213_1522.log')
        create_file('process_20140213_1722.log')

        create_file('some_file')

    def tearDown(self):
        shutil.rmtree(temp_dir)

    def test_1(self):
        cleanup_logs(temp_dir, 2)

        logs = os.listdir(temp_dir)
        logs.sort()
        self.assertEqual(3, len(logs))
        self.assertEqual('process_20140214_0712.log', logs[0])
        self.assertEqual('process_20140214_1712.log', logs[1])
        self.assertEqual('some_file', logs[2])

    def test_2(self):
        cleanup_logs(temp_dir, 3)

        logs = os.listdir(temp_dir)
        logs.sort()
        self.assertEqual(4, len(logs))
        self.assertEqual('process_20140214_0405.log', logs[0])
        self.assertEqual('process_20140214_0712.log', logs[1])
        self.assertEqual('process_20140214_1712.log', logs[2])
        self.assertEqual('some_file', logs[3])


if '__main__' == __name__:
    unittest.main()
