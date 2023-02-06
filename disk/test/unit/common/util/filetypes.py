# -*- coding: utf-8 -*-

import unittest

from nose_parameterized import parameterized

from mpfs.common.util.filetypes import getGroupOnlyByName


class GroupTestCase(unittest.TestCase):

    @parameterized.expand([
        # name, expected result
        ('test.jpg', 'image'),
        ('.jpg', 'image'),
        ('.jpg.doc', 'document'),
        ('test.jpg.doc', 'document'),
        ('', None),
        ('test.jpg.', None),
        ('test.doc.', None),
        ('test.', None),
        ('.test.', None),
        ('audio', None),
    ])
    def test_get_group_only_by_name(self, name, expected_result):
        assert getGroupOnlyByName(name) == expected_result
