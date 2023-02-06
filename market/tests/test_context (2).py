# coding: utf-8

import contextlib
import copy
from itertools import chain
from unittest import TestCase

from market.pylibrary import common


class TestContext(TestCase):

    def test_close(self):
        test_data = 'test val'

        @contextlib.contextmanager
        def create_ctx():
            yield test_data

        val = common.context.close(create_ctx())
        self.assertEqual(val, test_data)

    def test_updated_mapping(self):
        mapping = {'attr0': 123, 'attr1': 234}
        update = {'new_attr': 456}
        delete = {'del_attr', 'attr0'}

        orig_mapping = copy.deepcopy(mapping)
        with common.context.updated_mapping(mapping, update, delete) as data:
            self.assertTrue(all(
                key in data for key in chain(mapping, update)
                if key not in delete
            ))
            self.assertTrue(all(key not in data for key in delete))
            self.assertTrue(all(
                data[key] == update.get(key, mapping[key])
                for key in chain(mapping, update) if key not in delete
            ))

        self.assertEqual(mapping, orig_mapping)
