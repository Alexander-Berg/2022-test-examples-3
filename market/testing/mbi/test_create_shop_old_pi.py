#!/usr/bin/env python
# coding=utf-8

import mbi_common
import unittest
import case_base
import time
import helpers


class T(case_base.MbiTestCase):

    def test_create_shop(self):
        """ Тестируем регистацию магазина старым ПИ (тест обратной совметимости)

        Сценарий:
            * Создать магазин
            * Проверить, что нет CPA_PARTNER и CPC_PARTNER
            * Заполнить поля
            * Через старые ручки (спросить Наташу) убедиться, что магазин готов отправиться на модерацию
        """
        mbi_partner = mbi_common.MbiPartner(user_id=self.USER.id)
        mbi_api = mbi_common.MbiApi()

        # НЕ передавая в registerCampaign enableProgramsTumblers=1 мы притворяемчя старым фронтом-ПИ, который
        # НЕ знает ничего про тумблеры и новые ручки
        _, shop_id = mbi_common.register_shop(mbi_partner, user=self.USER, enable_programs_tumblers=False)

        # Чтобы не создавать магазин каждый раз с нуля
        # shop_id = 44400058
        # print 'DSID:', shop_id

        co = mbi_api.shop_abo_cutoffs(shop_id, only_active=True)
        self.assertTrue(len(co) <= 0)


        # TODO: как проверить готовность магазина к прохождению модерации?

        cpc_state = mbi_partner.cpc_state(shop_id)

        # Проверка, что нету CPC_PARTNER
        self.assertTrue('44' not in cpc_state['result']['cpcCutoffs'])
        # Проверка, что нету CPA_PARTNER
        cpa_cutoffs = mbi_partner.get_cpa_cutoffs(shop_id)

        # Андрей Мещеряков, [Jun 9, 2017, 12:21 PM]:
        # Уу магазина зареганного без магического параметра все равно открывается катоф 'CPA_PARTNER'
        # Fedor Bokovikov, [Jun 9, 2017, 12:21 PM]:
        # так и должно быть
        # он и раньше открывался
        self.assertTrue('CPA_PARTNER' in cpa_cutoffs)

        mbi_partner.get_premoderation_info(shop_id)

        helpers.fill_shop_params_for_global(mbi_partner, shop_id, ut=self)




if __name__ == '__main__':
    unittest.main()
