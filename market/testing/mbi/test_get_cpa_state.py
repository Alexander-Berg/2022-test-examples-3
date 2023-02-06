#!/usr/bin/env python
# coding=utf-8

import unittest
import os
import mbi_common
import helpers
from lxml import etree
import time
import case_base
from lxml import etree


class T(case_base.MbiTestCase):
    """ Тестирование ручки getCPAState vs moderationRequestState
    """

    def test_get_cpa_state(self):
        """ Не должно быть неготовности к CPA в старой ручке getCPAState
        Сценарий:
            * Зарегать магазин
            * Заполнить все нужные поля
            * Взвести оба тумблера: CPA:on, CPC:on
            * Проверть что кнопка видна и что в getCPAState магазин рапортуется, как готовый к CPA
        """
        mbi_partner = mbi_common.MbiPartner(user_id=self.USER.id)
        campaign_id, shop_id = mbi_common.register_shop(mbi_partner, user=self.USER)
        helpers.fill_shop_params_for_global(mbi_partner, shop_id, ut=self)
        mbi_partner.update_cpc_placement(shop_id, True)
        mbi_partner.cpa_status_update(shop_id, True)

        # Кнопка "на модерацию" должна быть видна
        mrs = mbi_partner.moderation_request_state(shop_id)
        self.assertTrue(mrs['result']['moderationEnabled'])



if __name__ == '__main__':
    unittest.main()
