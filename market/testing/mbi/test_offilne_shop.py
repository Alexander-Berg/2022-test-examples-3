#!/usr/bin/env python
# coding=utf-8

import unittest
import os
import time
import moderation
import case_base
import mbi_common


class T(case_base.MbiTestCase):

    def test_onboard_offline_shop_old_pi(self):
        """ Проверить регистрацию offline-магазина через старый ПИ
        """
        cmpg_id, shop_id = moderation.onboard(
            self,
            is_online=False,
            is_global=False,
            is_new_pi=False,
            is_cpc=False,
            is_cpa=False,
            is_api=False)

    def test_onboard_offline_shop_new_pi(self):
        """ Проверить регистрацию offline-магазина через новый ПИ
        """
        cmpg_id, shop_id = moderation.onboard(
            self,
            is_online=False,
            is_global=False,
            is_new_pi=True,
            is_cpc=False,
            is_cpa=False,
            is_api=False)


if __name__ == '__main__':
    unittest.main()
