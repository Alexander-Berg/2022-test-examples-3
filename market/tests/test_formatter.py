#!/usr/bin/python
# -*- coding: utf-8 -*-

import unittest
from getter.service.yt_ctr import LegacyCsvFormatter


class Test(unittest.TestCase):
    def test_formatter(self):
        f = LegacyCsvFormatter(
            order=['query', LegacyCsvFormatter.placeholder, 'clicks_num', 'shows_num'],
            ignore=['ofa_clicks_num'])

        self.assertEquals(f.deduce_format_from(('shows_num', 'clicks_num')),
                          '{clicks_num}\t{shows_num}\n')
        self.assertEquals(f.deduce_format_from(('clicks_num', 'shows_num', 'ware_md5')),
                          '{ware_md5}\t{clicks_num}\t{shows_num}\n')
        self.assertEquals(f.deduce_format_from(('clicks_num', 'category_hid', 'query')),
                          '{query}\t{category_hid}\t{clicks_num}\n')
        self.assertEquals(f.deduce_format_from(('clicks_num', 'category_hid', 'query', 'shows_num')),
                          '{query}\t{category_hid}\t{clicks_num}\t{shows_num}\n')
        self.assertEquals(f.deduce_format_from(('clicks_num', 'category_hid', 'query', 'shows_num', 'ofa_clicks_num')),
                          '{query}\t{category_hid}\t{clicks_num}\t{shows_num}\n')

        self.assertRaises(Exception, f.deduce_format_from, ('clicks_num', 'category_hid', 'query', 'unknown'))

    def test_ctrincuts_formatter(self):
        f = LegacyCsvFormatter(
            order=['query', LegacyCsvFormatter.placeholder, 'clicks_sum', 'shows_sum'])
        self.assertEquals(f.deduce_format_from(('shows_sum', 'clicks_sum')),
                          '{clicks_sum}\t{shows_sum}\n')
        self.assertEquals(f.deduce_format_from(('clicks_sum', 'shows_sum', 'inclid')),
                          '{inclid}\t{clicks_sum}\t{shows_sum}\n')


if __name__ == '__main__':
    unittest.main()
