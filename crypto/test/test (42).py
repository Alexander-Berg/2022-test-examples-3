#!/usr/bin/env python
# -*- coding: utf-8 -*-
import unittest

from crypta.profile.lib.frozen_dict import FrozenDict


class TestFrozenDict(unittest.TestCase):
    def test_plain_dict(self):
        d = FrozenDict({'a': 1, 'b': 2})

        self.assertTrue(len(d) == 2)
        self.assertTrue(d['a'] == 1)
        # check that hash calculation won't fail
        hash(d)

    def test_with_dict_inside(self):
        d = FrozenDict({
            'a': {
                'a': 1,
                'b': 2,
            },
            'b': [1, 2, 3],
        })

        self.assertTrue(len(d) == 2)
        hash(d)
