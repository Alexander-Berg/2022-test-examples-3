#!/usr/bin/env python
# coding=utf-8

from mbi_common import MbiShops
import unittest


class T(unittest.TestCase):
    @unittest.skip('Needs to be rewritten')
    def test_mbi_20999_global(self):
        mbiShops = MbiShops()
        response = mbiShops.get_shop_info(79620)
        self.assertEqual(response[0]['regnumName'], u'Регистрационный номер')

    @unittest.skip('Needs to be rewritten')
    def test_mbi_20999_not_global(self):
        mbiShops = MbiShops()
        response = mbiShops.get_shop_info(235)
        self.assertEqual(response[0]['regnumName'], u'ОГРН')


if __name__ == '__main__':
    unittest.main()
