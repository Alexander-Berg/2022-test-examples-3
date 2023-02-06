#!/usr/bin/python
# -*- coding: utf-8 -*-

import unittest

from market.idx.snippets.src import shopsdat

DATA = '''
shop_id\t1
datafeed_id\t12
a\ta
b\tb
red_status\tNO

#shop_id\t2
datafeed_id\t13
a\taa

shop_id\t3
datafeed_id\t14
b\tbbb

shop_id\t4
datafeed_id\t15
b\tbbb
red_status\tREAL

shop_id\t5
datafeed_id\t16
a\taaaa
b\tbbbb
'''


class Test(unittest.TestCase):
    def test_1(self):
        def check_record(record, name, key):
            d = record.props
            self.assertEqual(d.get(name), key)

        import six
        records = shopsdat.load(six.StringIO(DATA))
        check_record(records[0], 'shop_id', '1')
        check_record(records[0], 'a', 'a')
        check_record(records[0], 'b', 'b')
        check_record(records[1], 'shop_id', '3')
        check_record(records[1], 'b', 'bbb')
        check_record(records[2], 'shop_id', '5')
        check_record(records[2], 'a', 'aaaa')
        check_record(records[2], 'b', 'bbbb')
