# -*- coding: utf-8 -*-

import unittest
from StringIO import StringIO

from guruindexer import utils


class TestXMLGenerator(unittest.TestCase):
    def test(self):
        out = StringIO()
        gen = utils.XMLGenerator(out=out)
        gen.startAndEndElement('root', {'a': 'привет&пока'})

        self.assertEqual(out.getvalue(), '<root a="привет&amp;пока"/>')


class TestKeydataValidator(unittest.TestCase):
    def test_valid_file(self):
        lines = ['{key}:{value}\n'.format(key=i, value=(i + 1)) for i in range(1000)]
        result, msg = utils.is_keydata(lines)
        self.assertTrue(result)
        self.assertEqual(msg, '')

    def test_file_with_some_errors(self):
        lines = list()
        for i in range(1, 1001):
            if i % 100:
                lines.append('{key}:{value}'.format(key=i, value=(i + 1)))
            else:  # будет 10 невалидных строк, что меньше 1%
                lines.append('abc')
        lines.extend([''] * 1000)  # пустые строки должны игнориться
        result, msg = utils.is_keydata(lines)
        self.assertTrue(result)  # ошибки есть, но мало, и валидация пройдена
        self.assertEqual(msg, 'error on the line 100')

    def test_file_with_many_errors(self):
        lines = ['{key}:{value}\n'.format(key=i, value=(i + 1)) for i in range(900)]
        lines.extend([''] * 1000)
        lines.extend(['abc{}'.format(i) for i in range(100)])
        result, msg = utils.is_keydata(lines)
        self.assertFalse(result)
        self.assertEqual(msg, 'error on the line 1901')
