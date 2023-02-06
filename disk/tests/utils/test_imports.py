# encoding: UTF-8

import unittest

from ws_properties.utils.imports import import_module_attr, get_type


class ImportModuleAttrTestCase(unittest.TestCase):
    def test_import_builtin(self):
        self.assertIs(
            import_module_attr('exit'),
            exit,
        )

    def test_import_module(self):
        self.assertIs(
            import_module_attr('unittest.TestCase'),
            unittest.TestCase,
        )

    def test_raises_unknow_module(self):
        with self.assertRaisesRegexp(ImportError, 'No module named'):
            import_module_attr('unexistent_module_100500.test')

    def test_raises_unknow_attr(self):
        with self.assertRaisesRegexp(ImportError, 'has no attribute'):
            import_module_attr('exit100500')

class GetTypetestCase(unittest.TestCase):
    def test_import_from_string(self):
        self.assertIs(
            get_type('int'),
            int,
        )

    def test_import_itself(self):
        self.assertIs(
            get_type(float),
            float,
        )

    def test_raises_on_invalid_type(self):
        with self.assertRaises(ValueError):
            get_type(1)