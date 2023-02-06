#!/usr/bin/env python
# coding=utf-8

import mbi_common
import unittest
import case_base
import time
import helpers


class T(case_base.MbiTestCase):
    """ Сценарии
        FronOld     is_global   API     Prepay
        -           X           -       -
        X           -           -       -
        X           -           -       X
        X           -           X       -
        X           -           X       X
    """

    def test_create_shop(self):
        """ Тестируем регистацию магазина

        Создаем магазин
        Дальше дергаем все комбинации тумблеров и проверяем:
            * катофы
            * выдачу ручки moderationRequestState
            * Заполняем поля магазину
            * Добиваемся того, чтобы отобразилась кнопка "на модерацию"
        """
        mbi_partner = mbi_common.MbiPartner(user_id=self.USER.id)
        mbi_api = mbi_common.MbiApi()

        _, shop_id = mbi_common.register_shop(mbi_partner, user=self.USER)

        # Чтобы не создавать магазин каждый раз с нуля
        # shop_id = 44400058
        # print 'DSID:', shop_id
        # TODO: remove it
        # mbi_partner.cpa_status_update(shop_id, False)
        # mbi_partner.update_cpc_placement(shop_id, False)

        mrs = mbi_partner.moderation_request_state(shop_id)
        print mrs
        self.assertTrue('PROGRAM_IS_NOT_SELECTED' in mrs['result']['cpcModerationDisabledReasons'])
        self.assertTrue('PROGRAM_IS_NOT_SELECTED' in mrs['result']['cpaModerationDisabledReasons'])

        co = mbi_api.shop_abo_cutoffs(shop_id, only_active=True)
        self.assertTrue(len(co) <= 0)

        # Начальное состояние (CPA: off, CPC: off)
        cpc_state = mbi_partner.cpc_state(shop_id)
        self.assertTrue('44' in cpc_state['result']['cpcCutoffs'])  # Для старого фронта не долджно быть так!
        self.assertEqual(cpc_state['result']['canSwitchToOn'], True)
        cpa_state = mbi_partner.get_cpa_state(shop_id)
        cpa_cutoffs = set([x.text for x in cpa_state.xpath('/data/shop-cutoffs/cutoff-type')])
        self.assertTrue(set(['CPA_PARTNER', 'CPA_NEED_TESTING']).issubset(cpa_cutoffs))  # У магазина созданного через старый ПИ не долждно быть cpa_partner
        mrs = mbi_partner.moderation_request_state(shop_id)
        self.assertTrue('PROGRAM_IS_NOT_SELECTED' in mrs['result']['cpaModerationDisabledReasons'])
        self.assertTrue('PROGRAM_IS_NOT_SELECTED' in mrs['result']['cpcModerationDisabledReasons'])

        # (CPA: on, CPC: off)
        mbi_partner.cpa_status_update(shop_id, True)
        cpc_state = mbi_partner.cpc_state(shop_id)
        self.assertTrue('44' in cpc_state['result']['cpcCutoffs'])  # Для CPC-программы ничего не поменялось
        self.assertTrue('6' not in cpc_state['result']['cpcCutoffs'])
        self.assertEqual(cpc_state['result']['canSwitchToOn'], True)
        cpa_state = mbi_partner.get_cpa_state(shop_id)
        cpa_cutoffs = set([x.text for x in cpa_state.xpath('/data/shop-cutoffs/cutoff-type')])
        self.assertTrue('CPA_PARTNER' not in cpa_cutoffs)  # катоф-тумблер исчез
        mrs = mbi_partner.moderation_request_state(shop_id)
        self.assertTrue('MISSED_DATASOURCE_PARAMS' in mrs['result']['cpaModerationDisabledReasons'])
        self.assertTrue('PROGRAM_IS_NOT_SELECTED' not in mrs['result']['cpaModerationDisabledReasons'])
        self.assertTrue('PROGRAM_IS_NOT_SELECTED' in mrs['result']['cpcModerationDisabledReasons'])

        # (CPA: on, CPC: on)
        mbi_partner.update_cpc_placement(shop_id, True)
        cpc_state = mbi_partner.cpc_state(shop_id)
        self.assertTrue('44' not in cpc_state['result']['cpcCutoffs'])
        self.assertTrue('6' in cpc_state['result']['cpcCutoffs'])
        self.assertEqual(cpc_state['result']['canSwitchToOn'], True)
        cpa_state = mbi_partner.get_cpa_state(shop_id)
        cpa_cutoffs = set([x.text for x in cpa_state.xpath('/data/shop-cutoffs/cutoff-type')])
        self.assertTrue({'CPA_FOR_TESTING', 'CPA_NEED_TESTING'}.intersection(cpa_cutoffs)) # открыто или то или то (потому что может прилетать FINANCE со стороны)
        mrs = mbi_partner.moderation_request_state(shop_id)
        self.assertTrue('MISSED_DATASOURCE_PARAMS' in mrs['result']['cpaModerationDisabledReasons'])
        self.assertTrue('PROGRAM_IS_NOT_SELECTED' not in mrs['result']['cpaModerationDisabledReasons'])
        self.assertTrue('PROGRAM_IS_NOT_SELECTED' not in mrs['result']['cpcModerationDisabledReasons'])

        # (CPA: off, CPC: on)
        mbi_partner.cpa_status_update(shop_id, False)
        cpc_state = mbi_partner.cpc_state(shop_id)
        self.assertTrue('44' not in cpc_state['result']['cpcCutoffs'])
        self.assertTrue('6' in cpc_state['result']['cpcCutoffs'])
        self.assertEqual(cpc_state['result']['canSwitchToOn'], True)
        cpa_state = mbi_partner.get_cpa_state(shop_id)
        cpa_cutoffs = set([x.text for x in cpa_state.xpath('/data/shop-cutoffs/cutoff-type')])
        self.assertTrue('CPA_PARTNER' in cpa_cutoffs)  # тумблер вернулся
        self.assertTrue({'CPA_FOR_TESTING', 'CPA_NEED_TESTING'}.intersection(cpa_cutoffs)) # открыто или то или то (потому что может прилетать FINANCE со стороны)
        mrs = mbi_partner.moderation_request_state(shop_id)
        self.assertTrue('MISSED_DATASOURCE_PARAMS' in mrs['result']['cpcModerationDisabledReasons'])
        self.assertTrue('PROGRAM_IS_NOT_SELECTED' in mrs['result']['cpaModerationDisabledReasons'])
        self.assertTrue('PROGRAM_IS_NOT_SELECTED' not in mrs['result']['cpcModerationDisabledReasons'])

        # (CPA: off, CPC: off)
        mbi_partner.update_cpc_placement(shop_id, False)
        cpc_state = mbi_partner.cpc_state(shop_id)
        self.assertTrue('44' in cpc_state['result']['cpcCutoffs'])
        self.assertTrue('6' not in cpc_state['result']['cpcCutoffs'])
        self.assertEqual(cpc_state['result']['canSwitchToOn'], True)
        cpa_state = mbi_partner.get_cpa_state(shop_id)
        cpa_cutoffs = set([x.text for x in cpa_state.xpath('/data/shop-cutoffs/cutoff-type')])
        self.assertTrue(set(['CPA_PARTNER', 'CPA_NEED_TESTING']).issubset(cpa_cutoffs))  # У магазина созданного через старый ПИ не долждно быть cpa_partner
        mrs = mbi_partner.moderation_request_state(shop_id)
        self.assertTrue('PROGRAM_IS_NOT_SELECTED' in mrs['result']['cpaModerationDisabledReasons'])
        self.assertTrue('PROGRAM_IS_NOT_SELECTED' in mrs['result']['cpcModerationDisabledReasons'])

        # Заполнить параметры размещения и проверять, что вида кнопка "на модерацию"
        helpers.fill_shop_params_for_global(mbi_partner, shop_id, ut=self)
        mbi_partner.update_cpc_placement(shop_id, True)
        mbi_partner.cpa_status_update(shop_id, True)
        mrs = mbi_partner.moderation_request_state(shop_id)
        self.assertTrue(mrs['result']['moderationEnabled'])


if __name__ == '__main__':
    unittest.main()
