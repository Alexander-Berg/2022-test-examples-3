#!/usr/bin/env python
# coding=utf-8

from case_base import MbiTestCase
import mbi_common
import unittest
from unittest import skip
from lxml import etree
import helpers


class T(MbiTestCase):
    def test_mbi_21866_global(self):
        """
        Проверяем, что CPA_TECNHICAL_NEED_INFO не появляется, если создать
        глобальный магазин без адреса возврата
        """
        mbi_partner = mbi_common.MbiPartner(user_id=self.USER.id)
        mbi_api = mbi_common.MbiApi()
        mbi_billing = mbi_common.MbiBilling()

        _, shop_id = mbi_common.register_shop(mbi_partner, user=self.USER)
        # создаём магазин без адреса возврата
        helpers.fill_shop_params_for_global(mbi_partner, shop_id, ut=self, return_address=False)
        # дёргаем джобу по проверке NeedInfo
        mbi_billing.force_run_cpa_technical_cutoff_need_info_executor()

        # запрашиваем катоффы, среди них не должно быть NEED_INFO
        cpa_cutoffs = mbi_partner.get_cpa_cutoffs(shop_id)
        self.assertTrue('CPA_TECHNICAL_NEED_INFO' not in cpa_cutoffs)

    @skip('этого не будет в релизе')
    def test_mbi_21866_not_global(self):
        """
        Проверяем, что CPA_TECNHICAL_NEED_INFO появляется, если создать
        НЕглобальный магазин без адреса возврата
        """
        mbi_partner = mbi_common.MbiPartner(user_id=self.USER.id)
        mbi_api = mbi_common.MbiApi()
        mbi_billing = mbi_common.MbiBilling()

        # создаём НЕ-глобал магазин без адреса возврата
        _, shop_id = mbi_common.register_shop(mbi_partner, user=self.USER, is_global=False)
        helpers.fill_shop_params_for_non_global(mbi_partner, shop_id, ut=self, return_address=False, check_missed_datasource_params=False)
        # дёргаем джобу по проверке NeedInfo
        mbi_billing.force_run_cpa_technical_cutoff_need_info_executor()

        # запрашиваем катоффы, среди них должен быть NEED_INFO
        cpa_cutoffs = mbi_partner.get_cpa_cutoffs(shop_id)
        self.assertTrue('CPA_TECHNICAL_NEED_INFO' in cpa_cutoffs)


if __name__ == '__main__':
    unittest.main()
