# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from search.martylib.test_utils import TestCase

from search.martylib.zipping import compress, decompress


class TestUtils(TestCase):
    def test_zipping(self):
        strings = [
            '1',
            '',
            '123test-string/|\\',
            'э✋🎃ё]~±!@#$%^&*()_+º—',
            'a' * int(1e7),
        ]

        for string in strings:
            for compressing_level in range(1, 10):
                self.assertEqual(decompress(compress(string, compressing_level)), decompress(compress(string, compressing_level)))
                self.assertEqual(decompress(compress(string, compressing_level)).decode('utf-8'), string)
            self.assertEqual(decompress(compress(string)), decompress(compress(string)))
