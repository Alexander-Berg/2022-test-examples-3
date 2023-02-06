#!/usr/bin/env python
# coding=utf-8

from mbi_common import MbiShops, MbiPartner, OrganizationInfo
import unittest
from unittest import skip

class T(unittest.TestCase):
    @skip('Needs to be rewritten (create new shop in each test)')
    def test_mbi_20997(self):
        mbi = MbiPartner()

        # get old info
        info = mbi.show_organization_info(2254)

        # set new info
        info = info._replace(registration_number='100500')
        mbi.edit_organization_info(info)

        # check again
        info = mbi.show_organization_info(2254)
        self.assertEqual(info.registration_number, '100500')


if __name__ == '__main__':
    unittest.main()
