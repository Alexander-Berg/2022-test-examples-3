# coding: utf-8

import json
import os
import tempfile
import unittest

from market.idx.pylibrary.metrics.attr_context import AttrContext


class AttrContextTestCase(unittest.TestCase):

    def test_write_on_exit(self):
        with tempfile.NamedTemporaryFile('w+') as log_file:
            attr_context = AttrContext(log_file=log_file, close_on_exit=False)

            with attr_context:
                attr_context.attr1 = 'attr1_val'
                attr_context.new_node.attr2 = 'attr2_val'

                attr_context.flush()
                records = list(read_records(log_file.name))
                self.assertEqual(len(records), 0)

            attr_context.flush()
            records = list(read_records(log_file.name))

            expected = {
                'attr1': 'attr1_val',
                'new_node.attr2': 'attr2_val',
            }
            self.assertEqual(records[0], expected)

    def test_init_attrs(self):
        with tempfile.NamedTemporaryFile('w+') as log_file:
            attrs = {
                'init_attr0': 123,
                'init_attr1': 'test_init',
            }
            attr_context = AttrContext(log_file=log_file, close_on_exit=False, **attrs)

            with attr_context:
                attr_context.attr1 = 'attr1_val'

            attr_context.flush()
            records = list(read_records(log_file.name))

            expected = {
                'init_attr0': 123,
                'init_attr1': 'test_init',
                'attr1': 'attr1_val',
            }
            self.assertEqual(records[0], expected)

    def test_context_by_path(self):
        try:
            log_path = None
            with tempfile.NamedTemporaryFile('w+', delete=False) as log_file:
                log_path = log_file.name

            with AttrContext(log_path) as attr_context:
                attr_context.attr1 = 'attr1_val'
                attr_context.node.attr2 = 'node_attr2_val'

            records = list(read_records(log_path))
            expected = {
                'attr1': 'attr1_val',
                'node.attr2': 'node_attr2_val',
            }
            self.assertEqual(records[0], expected)

        finally:
            if log_path is not None:
                os.remove(log_path)

    def test_not_serializable_attr(self):
        with tempfile.NamedTemporaryFile('w+') as log_file:
            class NotSerializableClass(object):
                def __init__(self):
                    self._worker_id = 0

            attrs = {
                'init_attr0': 123,
                'init_attr1': 'test_init',
                'not_serializable': NotSerializableClass()
            }
            attr_context = AttrContext(log_file=log_file, close_on_exit=False, **attrs)

            with attr_context:
                attr_context.attr1 = 'attr1_val'

            attr_context.flush()
            records = list(read_records(log_file.name))

            expected = {
                'init_attr0': 123,
                'init_attr1': 'test_init',
                'attr1': 'attr1_val',
                'not_serializable': 'not_serialized'
            }
            self.assertEqual(records[0], expected)


def read_records(path):
    with open(path) as file_obj:
        for line in file_obj:
            if line:
                yield json.loads(line)
