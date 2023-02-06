# encoding: UTF-8

import unittest

from ws_properties.environ.properties import DictPropertySource


class PropertySourceTestCase(unittest.TestCase):
    def test_get_list_property_inline(self):
        source = DictPropertySource({'inline_list': '1,2,3,4'})

        self.assertListEqual(
            source.get_list_property('inline_list'),
            ['1', '2', '3', '4'],
        )

    def test_get_list_property(self):
        source = DictPropertySource({'list': ['1', '2', '3', '4']})

        self.assertListEqual(
            source.get_list_property('list'),
            ['1', '2', '3', '4'],
        )

    def test_get_list_property_nonexistent(self):
        source = DictPropertySource({})

        self.assertIsNone(source.get_list_property('nonexistent_list'))

    def test_get_list_property_empty(self):
        source = DictPropertySource({'empty_list': ''})

        self.assertIsNone(source.get_list_property('empty_list'))
