# -*- coding: utf-8 -*-


import os
import unittest
import shutil


import context
from market.pylibrary.mindexerlib.mergers import merge_files_by_key
from market.pylibrary.mindexerlib import util


TMP = os.path.realpath('tmp')


class TestMerger(unittest.TestCase):
    def setUp(self):
        shutil.rmtree(TMP, ignore_errors=True, onerror=None)
        util.makedirs(TMP)

    def tearDown(self):
        shutil.rmtree(TMP, ignore_errors=True, onerror=None)

    def test_merger(self):
        delta = os.path.join(TMP, 'delta')
        with open(delta, 'w') as _f:
            d = ('91013,100,1',
                 '91013,101,2',
                 '91013,102,3',
                 '91013,103,4',
                 '91049,100,0')
            _f.write('\n'.join(d))

        base = os.path.join(TMP, 'base')
        with open(base, 'w') as _f:
            d = ('91012,100,1',
                 '91013,100,1',
                 '91013,104,4',
                 '91013,105,5',
                 '91014,106,6',
                 '91015,107,7')
            _f.write('\n'.join(d))

        result = '\n'.join((
            '91012,100,1',
            '91013,100,1',
            '91013,101,2',
            '91013,102,3',
            '91013,103,4',
            '91014,106,6',
            '91015,107,7',
            '91049,100,0',
            ''  # last '\n'
        ))

        def get_hid(line):
            return line.split(',', 1)[0]

        merged = os.path.join(TMP, 'merged')
        merge_files_by_key(delta, base, merged, get_hid)

        with open(merged, 'r') as _f:
            self.assertEqual(_f.read(), result, 'merger is broken')


if '__main__' == __name__:
    context.main()
