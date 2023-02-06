#!/usr/bin/env python
# coding=utf-8

from case_base import MbiTestCase
import mbi_common
import unittest
from unittest import skip
from lxml import etree
import helpers
import json

class T(MbiTestCase):
    def test_enable_prepay_for_non_global_shop(self):
        mbi_partner = mbi_common.MbiPartner(user_id=self.USER.id)
        mbi_api = mbi_common.MbiApi()

        # создаём магазин
        _, shop_id = mbi_common.register_shop(mbi_partner, user=self.USER, is_global=False)
        helpers.fill_shop_params_for_non_global(mbi_partner, shop_id, ut=self)
        helpers.enable_prepay(mbi_partner=mbi_partner, mbi_api=mbi_api, is_global=False, shop_id=shop_id, ut=self)

        # как-то нужно проверить, что предоплата включена
        prepay_info = mbi_partner.get_prepay_info(shop_id)
        self.assertTrue('prepayRequests' in prepay_info['result'])
        self.assertTrue(prepay_info['result']['prepayRequests'][0]['status'] == '2') # 2 == COMPLETED, via Sergey Fedoseenkov
        self.assertTrue(prepay_info['result']['prepayRequests'][0]['prepayType'] == '1') # 0 - ЯД (старый), 1 - Маркет (новый), via Anatoly Vetokhin

        # может ещё нужно что-то проверить? неясно.
        # но я проверил вручную - всё работает, предоплата включается

    def test_enable_prepay_for_global_shop(self):
        mbi_partner = mbi_common.MbiPartner(user_id=self.USER.id)
        mbi_api = mbi_common.MbiApi()

        # создаём магазин
        _, shop_id = mbi_common.register_shop(mbi_partner, user=self.USER, is_global=True)
        helpers.fill_shop_params_for_global(mbi_partner, shop_id, ut=self)

        helpers.enable_prepay(mbi_partner=mbi_partner, mbi_api=mbi_api, is_global=True, shop_id=shop_id, ut=self)

        # проверяем, что предоплата включилась
        prepay_info = mbi_partner.get_prepay_info(shop_id)
        self.assertTrue('prepayRequests' in prepay_info['result'])
        self.assertTrue(prepay_info['result']['prepayRequests'][0]['status'] == '2') # 2 == COMPLETED, via Sergey Fedoseenkov
        self.assertTrue(prepay_info['result']['prepayRequests'][0]['prepayType'] == '0') # 0 - ЯД (старый), 1 - Маркет (новый), via Anatoly Vetokhin


if __name__ == '__main__':
    unittest.main()
