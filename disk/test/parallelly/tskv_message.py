# -*- coding: utf-8 -*-
from unittest import TestCase

from mpfs.common.util.logger import TSKVMessage

class TSKVMessageTestCase(TestCase):
    def test_tab_escape(self):
        self.assertEqual(r'column1\tcolumn1', TSKVMessage.escape_special('column1\tcolumn1'))

    def test_lf_escape(self):
        self.assertEqual(r'line1\nline2', TSKVMessage.escape_special('line1\nline2'))

    def test_cr_escape(self):
        self.assertEqual(r'line1\rline2', TSKVMessage.escape_special('line1\rline2'))

    def test_c_ending_escape(self):
        self.assertEqual(r'line1\0line2', TSKVMessage.escape_special('line1\0line2'))

    def test_slash_escape(self):
        self.assertEqual(r'line1\\line2', TSKVMessage.escape_special('line1\\line2'))

    def test_list_escape(self):
        source = ['plain\\value', 'value\n with new line\t and tab', 'another plain value']
        expected = [r'plain\\value', r'value\n with new line\t and tab', 'another plain value']
        self.assertEqual(expected, TSKVMessage.escape_special(source))

    def test_dict_escape(self):
        source = {'key1': 'plain_value', 'key2': 'value\n with new line\t and tab', 'key3': 'another plain value'}
        expected = {'key1': 'plain_value', 'key2': r'value\n with new line\t and tab', 'key3': 'another plain value'}
        self.assertEqual(expected, TSKVMessage.escape_special(source))

    def test_with_special_escaped(self):
        actual = TSKVMessage.with_special_escaped('test\nnew_line', key='value\t tabbed')
        expected_list = [r'arg_0=test\nnew_line', r'key=value\t tabbed']
        self.assertEqual('\t'.join(expected_list), str(actual))
