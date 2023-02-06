#!/usr/bin/env python
# coding=utf-8

import unittest
import os
import mbi_common
from lxml import etree


class T(unittest.TestCase):
    # Test https://st.yandex-team.ru/MBI-20802

    @unittest.skip('Rewrite it')
    def test_basic(self):
        # Тест открытия катофа CPA_QUALITY

        mbi = mbi_common.MbiApi('http://mbi10et.haze.yandex.net:34820')

        # Закрыть катоф, чтобы тестировать с чистого листа
        #
        mbi.close_cpa_cutoff(774, 'CPA_QUALITY_CHEESY')

        # Убеждаемся, что катоф не отображается в ручках
        #
        cutoffs = mbi.shop_abo_cutoffs(774)
        self.assertTrue(not cutoffs['CPA_QUALITY']['is_active'])

        shop_infos = mbi.get_cpa_shops(774)
        self.assertTrue(len(shop_infos) == 1)
        self.assertTrue('CPA_QUALITY_CHEESY' not in etree.tostring(shop_infos[0]))

        # Открываем катоф
        #
        mbi.abo_cutoff_open(774, 'CPA_QUALITY')


        # Убеждфемся, что он появился в ручках
        #
        cutoffs = mbi.shop_abo_cutoffs(774)
        self.assertTrue(cutoffs['CPA_QUALITY']['is_active'])

        shop_infos = mbi.get_cpa_shops(774)
        self.assertTrue(len(shop_infos) == 1)
        self.assertTrue('CPA_QUALITY_CHEESY' in etree.tostring(shop_infos[0]))


        # Пытаемся повторно открыть катофф. Должна быть ошибка
        #
        try:
            mbi.abo_cutoff_open(774, 'CPA_QUALITY')
        except Exception as e:
            self.assertTrue('Shop already has opened cutoff of type: CPA_QUALITY' in unicode(e))
        else:
            self.assertTrue(False)  # чтобы тест упал, если не было ошибки




if __name__ == '__main__':
    unittest.main()
