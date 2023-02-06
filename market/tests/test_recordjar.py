# -*- coding: utf-8 -*-

import unittest
from cStringIO import StringIO

from guruindexer import recordjar


class Test(unittest.TestCase):
    data = '''\
# 1
name: abc
key: 123
%%
# 2
name: qwe
%%
# 3
name: asd
'''

    def test(self):
        def check_record(record, name, key):
            d = dict(record)
            self.assertEqual(d.get('name'), name)
            self.assertEqual(d.get('key'), key)

        records = recordjar.read(StringIO(self.data))
        check_record(records[0], 'abc', '123')
        check_record(records[1], 'qwe', None)
        check_record(records[2], 'asd', None)
