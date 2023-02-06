#!/usr/bin/env python
# coding=utf-8

import unittest
import os
import mbi_common
import helpers
from lxml import etree
import time
from lxml import etree
import case_base


class T(case_base.MbiTestCase):
    # Sources: https://github.yandex-team.ru/market-java/mbi/blob/release/2017.2.54_MBI-21242/mbi-core/src/java/ru/yandex/market/core/moderation/DefaultModerationService.java
    # API doc: https://wiki.yandex-team.ru/MBI/NewDesign/components/market-payment/moderationRequestState/
    """ Тестируем кнопку магазина про отправку на модерацию

    Минимальный сценарий:
        Зарегать магазин.
        Заполнить нужные поля.
        Убедиться, что в кнопка отправки на модерацию стала доступна.

    Полный Сценарий (потом):
        Зарегистрировать магазин.
        Отметить программу CPA
            Проверить, что кнопка недоступна из-за полей.
            Начать заполнять поля, проверяя, как они исчезают из ручки.
        Тоже самое CPC
        Тоже самое CPC+CPA
    """

    def test_mrs_all(self):
        mbi_partner = mbi_common.MbiPartner(user_id=self.USER.id)
        campaign_id, shop_id = mbi_common.register_shop(mbi_partner, user=self.USER)
        helpers.fill_shop_params_for_global(mbi_partner, shop_id, ut=self)
        mbi_partner.update_cpc_placement(shop_id, True)
        mbi_partner.cpa_status_update(shop_id, True)
        mrs = mbi_partner.moderation_request_state(shop_id)
        self.assertTrue(mrs['result']['moderationEnabled'])



if __name__ == '__main__':
    unittest.main()
