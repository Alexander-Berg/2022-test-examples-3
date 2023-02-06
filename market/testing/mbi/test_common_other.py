#!/usr/bin/env python
# coding=utf-8

from case_base import MbiTestCase
import mbi_common
import unittest
from unittest import skip
from lxml import etree
import helpers
import json
import time

class T(MbiTestCase):
    # MBI-21805
    @skip('until MBI-22007 resolultion')
    def test_common_other_cutoff(self):
        mbi_partner = mbi_common.MbiPartner(user_id=self.USER.id)
        mbi_api = mbi_common.MbiApi()

        _, shop_id = mbi_common.register_shop(mbi_partner, user=self.USER, is_global=False)

        # открываем cutoff COMMON_OTHER
        mbi_api.abo_cutoff_open(shop_id, 'COMMON_OTHER')

        # проверяем, что cutoff открылся
        r = mbi_api.shop_abo_cutoffs(shop_id=shop_id)
        self.assertTrue('COMMON_OTHER' in r)
        self.assertTrue(r['COMMON_OTHER']['is_active'] == True)

        # дёргаем ручку "Я исправился"
        time.sleep(0.5)
        mbi_partner.push_ready_for_testing(shop_id)

        # проверяем, что магазин появился на проверке
        time.sleep(0.5)
        r = mbi_api.qc_shops_light_checks(shop_id)


if __name__ == '__main__':
    unittest.main()
