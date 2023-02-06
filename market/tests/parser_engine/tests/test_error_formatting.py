# -*- coding: utf-8 -*-
import unittest

from market.idx.datacamp.parser.lib.parser_engine import utils


class TestErrorFormatting(unittest.TestCase):
    def test_without_add_info(self):
        self.assertEqual(utils.format_error('Bla bla bla', 20), 'Bla bla bla')

    def test_with_add_info(self):
        error = utils.format_error('Bla bla bla', 39, 'first log', 'second log')
        self.assertEqual(error, 'Bla bla bla; ERR: first log; second log')

    def test_log_trimming(self):
        self.assertEqual(utils.format_error('Bla bla bla', 10), 'Bla bla...')

    def test_log_trimming_with_add_info(self):
        error = utils.format_error('Bla bla bla', 23, 'first log', 'second log')
        self.assertEqual(error, 'Bla bla bla; ERR: fi...')

if '__main__' == __name__:
    unittest.main()
