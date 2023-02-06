# -*- coding: utf-8 -*-

from six.moves.configparser import ConfigParser
import os
import unittest

from market.pylibrary.yatestwrap.yatestwrap import source_path

from market.idx.datacamp.parser.lib.parser_engine.feed_processor import extract_and_convert
from market.idx.datacamp.parser.lib.parser_engine.exception import ReportableFatalError


DATA_DIR = source_path('market/idx/datacamp/parser/tests/parser_engine/big_tests/dirs')


class TestExtractMessages(unittest.TestCase):
    def setUp(self):
        unittest.TestCase.setUp(self)

    def _test_invalid_zip(self):
        # TODO(manushkin) Вернуть тест
        with self.assertRaises(ReportableFatalError):
            extract_and_convert(os.path.join(DATA_DIR, 'invalid_zip'), None, ConfigParser())

    def _test_huge_zip(self):
        with self.assertRaises(ReportableFatalError):
            extract_and_convert(os.path.join(DATA_DIR, '4gb_zip'), None, ConfigParser())


if '__main__' == __name__:
    unittest.main()
