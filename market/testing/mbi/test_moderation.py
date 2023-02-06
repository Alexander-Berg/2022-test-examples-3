#!/usr/bin/env python
# coding=utf-8

import case_log
import mbi_common
import unittest
import case_base
import time
import helpers
import subprocess
import moderation
from unittest import skip
import time


class T(case_base.MbiTestCase):
    """

    Сценарии
        * Прохождение модерации магазином (первичное включение). Оптимистичный сценарий.
        * Модерация от старого фронта
        * Фейл модерации
        * Статус, что модерация была отменена от АБО

    """
    def setUp(self):
        mbi_common.MbiBilling().disable_finance_executor()
        mbi_common.MbiBilling().disable_moderation_executor()

    def tearDown(self):
        mbi_common.MbiBilling().enable_finance_executor()  # FIXME
        mbi_common.MbiBilling().enable_moderation_executor()
        pass

    def test_onboard_positive_case_newpi_global_no_prepay(self):
        """ Прохождение модерации магазином (первичное включение). Оптимистичный сценарий. Глобал без предоплаты.

        Сценарий:
            * Создаем магазин
            * Заполнить поля
            * Отправить на модерацию
            * Проверить, на какие модерации он послан
            * Иммитировать прохождение/непрохождение модерации
        """
        mbi_partner = mbi_common.MbiPartner(user_id=self.USER.id)
        mbi_api = mbi_common.MbiApi()
        mbi_billing = mbi_common.MbiBilling()

        time.sleep(0.5)
        cmpg_id, shop_id = mbi_common.register_shop(mbi_partner, user=self.USER)
        time.sleep(0.5)
        helpers.fill_shop_params_for_global(mbi_partner, shop_id, ut=self)

        # for debugging:
        # cmpg_id = 1086000677
        # shop_id = 44400235

        # Положить магазину денег на счет
        # mbi_billing.refill_balance(cmpg_id, 1000)
        # self.assertEqual(mbi_partner.get_shop_balance(shop_id), 1000.0)
        # mbi_billing.force_run_fincnce_executor()

        time.sleep(0.5)
        mbi_partner.cpa_status_update(shop_id, True)
        time.sleep(0.5)
        mbi_partner.update_cpc_placement(shop_id, True)
        time.sleep(0.5)
        mrs = mbi_partner.moderation_request_state(shop_id)
        self.assertTrue(mrs['result']['moderationEnabled'])

        # Push the button
        time.sleep(0.5)
        mbi_partner.push_ready_for_testing(shop_id)
        time.sleep(0.5)
        mrs = mbi_partner.moderation_request_state(shop_id)
        self.assertTrue('MODERATION_IN_PROGRESS' in mrs['result']['cpaModerationDisabledReasons'])
        self.assertTrue('MODERATION_IN_PROGRESS' in mrs['result']['cpcModerationDisabledReasons'])

        # Pretend that the feed was loaded into the moderaion index
        time.sleep(0.5)
        mbi_api.testing_shops_premoderation_feed_check_delete(shop_id, 'CPA')
        time.sleep(0.5)
        mbi_api.testing_shops_premoderation_feed_check_delete(shop_id, 'CPC')

        # Check the abo list
        time.sleep(0.5)
        checks = {ch.testing_type: ch for ch in mbi_api.qc_shops_premoderations_get_shop_checks_list(shop_id)}
        self.assertTrue(len(checks) == 2)  # магазин должен быть отправлен на две проверки
        self.assertTrue('3' in checks)  # отправлен на модерацию по CPA
        self.assertTrue('0' in checks)  # отправлен на модерацию по CPC
        self.assertEqual(checks['3'].try_num, 1)
        self.assertEqual(checks['3'].offline, False)
        self.assertEqual(checks['0'].try_num, 1)
        self.assertEqual(checks['0'].offline, False)

        # Push the notification from ABO
        time.sleep(0.5)
        mbi_api.qc_shops_premoderations_result(shop_id, '0')
        time.sleep(0.5)
        mbi_api.qc_shops_premoderations_result(shop_id, '3')

        # Магазин перешел в состояние ожидания загрузки фида в большой индекс.
        # иммитируем, что офферы загрузились в индекс
        # Втрое удаление:
        time.sleep(0.5)
        mbi_api.testing_shops_premoderation_feed_check_delete(shop_id, 'CPA')
        time.sleep(0.5)
        mbi_api.testing_shops_premoderation_feed_check_delete(shop_id, 'CPC')

        # Магия, которая нужна, чтобы исчез катов CPC_FOR_TESTING
        time.sleep(0.5)
        mbi_billing.force_run_moderation_executor()

        # Кнопка отправки на модерацию должна исчезнуть
        time.sleep(0.5)
        mrs = mbi_partner.moderation_request_state(shop_id)
        self.assertTrue('MODERATION_IN_PROGRESS' not in mrs['result']['cpaModerationDisabledReasons'])
        self.assertTrue('MODERATION_IN_PROGRESS' not in mrs['result']['cpcModerationDisabledReasons'])
        self.assertTrue(not mrs['result']['moderationEnabled'])

        # Не торчит катофов в ABO
        time.sleep(0.5)
        abo_cutoffs = mbi_api.shop_abo_cutoffs(shop_id)
        self.assertTrue(len(abo_cutoffs) <= 0)

        # Не торчит CPA-катофов в ПИ
        time.sleep(0.5)
        cpa_cutoffs = mbi_partner.get_cpa_cutoffs(shop_id)
        self.assertEqual(set([]), cpa_cutoffs)

        # Ручка cpcState в порядке
        time.sleep(0.5)
        cpc_state = mbi_partner.cpc_state(shop_id)
        self.assertTrue(len(cpc_state['result']['cpcCutoffs']) <= 0)
        self.assertEqual(cpc_state['result']['cpc'], 'REAL')

        # Вручную устанавливаем магазину параметр 46 (IS_IN_INDEX), чтобы ПИ было хорошо
        # Значение этого параметр влияет на выдачу ручки getCampaignStates (см. код ниже, который
        # тестирует выдачу этой ручки)
        time.sleep(0.5)
        mbi_api.hack_set_shop_param_value(shop_id, 46, 'true')

        # Не ждем пока магаин будет включен джобой shopStateReportExecutor
        time.sleep(0.5)
        mbi_api.hack_set_shop_param_value(shop_id, 53, '0')

        # На эту ручку смотрит фронт ПИ, чтобы рисовать что-то там пользователю
        time.sleep(0.5)
        status_for_pi = mbi_partner.get_campaign_states(shop_id)
        self.assertEqual(status_for_pi, '1')  # 1 = Магазин включен/индекс опубликован

        # Проверяем, что магазин в CPA-состоянии REAL_NOT_INDEXED. Это означает, что индекс загружается.
        # Симмитировать загрузку фида с этой стороны сложно, т.к. состояние меняется в ответ на приход генлогов
        # индексатора. На этом остановимся.
        time.sleep(0.5)
        cpa_state = mbi_partner.get_cpa_state(shop_id)
        self.assertEqual('REAL_NOT_INDEXED', cpa_state.xpath('/data/cpastate/cpa-real')[0].text)

    def test_onboard_positive_case_newpi_global_prepay(self):
        """ Прохождение модерации магазином (первичное включение). Оптимистичный сценарий. Глобал+предоплата

        Сценарий:
            * Создаем магазин
            * Заполнить поля
            * Отправить на модерацию
            * Проверить, на какие модерации он послан
            * Иммитировать прохождение/непрохождение модерации
        """
        mbi_partner = mbi_common.MbiPartner(user_id=self.USER.id)
        mbi_api = mbi_common.MbiApi()
        mbi_billing = mbi_common.MbiBilling()

        time.sleep(0.5)
        cmpg_id, shop_id = mbi_common.register_shop(mbi_partner, user=self.USER)
        time.sleep(0.5)
        helpers.fill_shop_params_for_global(mbi_partner, shop_id, ut=self)
        time.sleep(0.5)
        helpers.enable_prepay(mbi_partner=mbi_partner, mbi_api=mbi_api, is_global=True, shop_id=shop_id, ut=self)

        # for debugging:
        # cmpg_id = 1086000677
        # shop_id = 44400235

        # Положить магазину денег на счет
        # mbi_billing.refill_balance(cmpg_id, 1000)
        # self.assertEqual(mbi_partner.get_shop_balance(shop_id), 1000.0)
        # mbi_billing.force_run_fincnce_executor()

        time.sleep(0.5)
        mbi_partner.cpa_status_update(shop_id, True)
        time.sleep(0.5)
        mbi_partner.update_cpc_placement(shop_id, True)
        time.sleep(0.5)
        mrs = mbi_partner.moderation_request_state(shop_id)
        self.assertTrue(mrs['result']['moderationEnabled'])

        # Push the button
        time.sleep(0.5)
        mbi_partner.push_ready_for_testing(shop_id)
        time.sleep(0.5)
        mrs = mbi_partner.moderation_request_state(shop_id)
        self.assertTrue('MODERATION_IN_PROGRESS' in mrs['result']['cpaModerationDisabledReasons'])
        self.assertTrue('MODERATION_IN_PROGRESS' in mrs['result']['cpcModerationDisabledReasons'])

        # Pretend that the feed was loaded into the moderaion index
        time.sleep(0.5)
        mbi_api.testing_shops_premoderation_feed_check_delete(shop_id, 'CPA')
        time.sleep(0.5)
        mbi_api.testing_shops_premoderation_feed_check_delete(shop_id, 'CPC')

        # Check the abo list
        time.sleep(0.5)
        checks = {ch.testing_type: ch for ch in mbi_api.qc_shops_premoderations_get_shop_checks_list(shop_id)}
        self.assertTrue(len(checks) == 2)  # магазин должен быть отправлен на две проверки
        self.assertTrue('3' in checks)  # отправлен на модерацию по CPA
        self.assertTrue('0' in checks)  # отправлен на модерацию по CPC
        self.assertEqual(checks['3'].try_num, 1)
        self.assertEqual(checks['3'].offline, False)
        self.assertEqual(checks['0'].try_num, 1)
        self.assertEqual(checks['0'].offline, False)

        # Push the notification from ABO
        time.sleep(0.5)
        mbi_api.qc_shops_premoderations_result(shop_id, '0')
        time.sleep(0.5)
        mbi_api.qc_shops_premoderations_result(shop_id, '3')

        # Магазин перешел в состояние ожидания загрузки фида в большой индекс.
        # иммитируем, что офферы загрузились в индекс
        # Втрое удаление:
        time.sleep(0.5)
        mbi_api.testing_shops_premoderation_feed_check_delete(shop_id, 'CPA')
        time.sleep(0.5)
        mbi_api.testing_shops_premoderation_feed_check_delete(shop_id, 'CPC')

        # Магия, которая нужна, чтобы исчез катов CPC_FOR_TESTING
        time.sleep(0.5)
        mbi_billing.force_run_moderation_executor()

        # Кнопка отправки на модерацию должна исчезнуть
        time.sleep(0.5)
        mrs = mbi_partner.moderation_request_state(shop_id)
        self.assertTrue('MODERATION_IN_PROGRESS' not in mrs['result']['cpaModerationDisabledReasons'])
        self.assertTrue('MODERATION_IN_PROGRESS' not in mrs['result']['cpcModerationDisabledReasons'])
        self.assertTrue(not mrs['result']['moderationEnabled'])

        # Не торчит катофов в ABO
        time.sleep(0.5)
        abo_cutoffs = mbi_api.shop_abo_cutoffs(shop_id)
        self.assertTrue(len(abo_cutoffs) <= 0)

        time.sleep(1)
        # Не торчит CPA-катофов в ПИ
        time.sleep(0.5)
        cpa_cutoffs = mbi_partner.get_cpa_cutoffs(shop_id)
        self.assertEqual(set([]), cpa_cutoffs)

        # Ручка cpcState в порядке
        time.sleep(0.5)
        cpc_state = mbi_partner.cpc_state(shop_id)
        self.assertTrue(len(cpc_state['result']['cpcCutoffs']) <= 0)
        self.assertEqual(cpc_state['result']['cpc'], 'REAL')

        # Вручную устанавливаем магазину параметр 46 (IS_IN_INDEX), чтобы ПИ было хорошо
        # Значение этого параметр влияет на выдачу ручки getCampaignStates (см. код ниже, который
        # тестирует выдачу этой ручки)
        time.sleep(0.5)
        mbi_api.hack_set_shop_param_value(shop_id, 46, 'true')

        # Не ждем пока магаин будет включен джобой shopStateReportExecutor
        time.sleep(0.5)
        mbi_api.hack_set_shop_param_value(shop_id, 53, '0')

        # На эту ручку смотрит фронт ПИ, чтобы рисовать что-то там пользователю
        time.sleep(0.5)
        status_for_pi = mbi_partner.get_campaign_states(shop_id)
        self.assertEqual(status_for_pi, '1')  # 1 = Магазин включен/индекс опубликован

        # Проверяем, что магазин в CPA-состоянии REAL_NOT_INDEXED. Это означает, что индекс загружается.
        # Симмитировать загрузку фида с этой стороны сложно, т.к. состояние меняется в ответ на приход генлогов
        # индексатора. На этом остановимся.
        time.sleep(0.5)
        cpa_state = mbi_partner.get_cpa_state(shop_id)
        self.assertEqual('REAL_NOT_INDEXED', cpa_state.xpath('/data/cpastate/cpa-real')[0].text)

    def test_onboard_positive_case_newpi_no_global_no_prepay(self):
        """ Прохождение модерации магазином (первичное включение). Оптимистичный сценарий. Не-глобал + нет предоплаты

        Сценарий:
            * Создаем магазин
            * Заполнить поля
            * Отправить на модерацию
            * Проверить, на какие модерации он послан
            * Иммитировать прохождение/непрохождение модерации
        """
        mbi_partner = mbi_common.MbiPartner(user_id=self.USER.id)
        mbi_api = mbi_common.MbiApi()
        mbi_billing = mbi_common.MbiBilling()

        time.sleep(0.5)
        cmpg_id, shop_id = mbi_common.register_shop(mbi_partner, user=self.USER, is_global=False)
        time.sleep(0.5)
        helpers.fill_shop_params_for_non_global(mbi_partner, shop_id, ut=self)
        time.sleep(0.5)
        helpers.enable_prepay(mbi_partner=mbi_partner, mbi_api=mbi_api, is_global=False, shop_id=shop_id, ut=self)

        # for debugging:
        # cmpg_id = 1086000677
        # shop_id = 44400235

        # Положить магазину денег на счет
        # mbi_billing.refill_balance(cmpg_id, 1000)
        # self.assertEqual(mbi_partner.get_shop_balance(shop_id), 1000.0)
        # mbi_billing.force_run_fincnce_executor()

        time.sleep(0.5)
        mbi_partner.cpa_status_update(shop_id, True)
        time.sleep(0.5)
        mbi_partner.update_cpc_placement(shop_id, True)
        time.sleep(0.5)
        mrs = mbi_partner.moderation_request_state(shop_id)
        self.assertTrue(mrs['result']['moderationEnabled'])

        # Push the button
        time.sleep(0.5)
        mbi_partner.push_ready_for_testing(shop_id)
        time.sleep(0.5)
        mrs = mbi_partner.moderation_request_state(shop_id)
        self.assertTrue('MODERATION_IN_PROGRESS' in mrs['result']['cpaModerationDisabledReasons'])
        self.assertTrue('MODERATION_IN_PROGRESS' in mrs['result']['cpcModerationDisabledReasons'])

        # Pretend that the feed was loaded into the moderaion index
        time.sleep(0.5)
        mbi_api.testing_shops_premoderation_feed_check_delete(shop_id, 'CPA')
        time.sleep(0.5)
        mbi_api.testing_shops_premoderation_feed_check_delete(shop_id, 'CPC')

        # Check the abo list
        time.sleep(0.5)
        checks = {ch.testing_type: ch for ch in mbi_api.qc_shops_premoderations_get_shop_checks_list(shop_id)}
        self.assertTrue(len(checks) == 2)  # магазин должен быть отправлен на две проверки
        self.assertTrue('3' in checks)  # отправлен на модерацию по CPA
        self.assertTrue('0' in checks)  # отправлен на модерацию по CPC
        self.assertEqual(checks['3'].try_num, 1)
        self.assertEqual(checks['3'].offline, False)
        self.assertEqual(checks['0'].try_num, 1)
        self.assertEqual(checks['0'].offline, False)

        # Push the notification from ABO
        time.sleep(0.5)
        mbi_api.qc_shops_premoderations_result(shop_id, '0')
        time.sleep(0.5)
        mbi_api.qc_shops_premoderations_result(shop_id, '3')

        # Магазин перешел в состояние ожидания загрузки фида в большой индекс.
        # иммитируем, что офферы загрузились в индекс
        # Втрое удаление:
        time.sleep(0.5)
        mbi_api.testing_shops_premoderation_feed_check_delete(shop_id, 'CPA')
        time.sleep(0.5)
        mbi_api.testing_shops_premoderation_feed_check_delete(shop_id, 'CPC')

        # Магия, которая нужна, чтобы исчез катов CPC_FOR_TESTING
        time.sleep(0.5)
        mbi_billing.force_run_moderation_executor()

        # Кнопка отправки на модерацию должна исчезнуть
        time.sleep(0.5)
        mrs = mbi_partner.moderation_request_state(shop_id)
        self.assertTrue('MODERATION_IN_PROGRESS' not in mrs['result']['cpaModerationDisabledReasons'])
        self.assertTrue('MODERATION_IN_PROGRESS' not in mrs['result']['cpcModerationDisabledReasons'])
        self.assertTrue(not mrs['result']['moderationEnabled'])

        # Не торчит катофов в ABO
        time.sleep(0.5)
        abo_cutoffs = mbi_api.shop_abo_cutoffs(shop_id)
        self.assertTrue(len(abo_cutoffs) <= 0)

        time.sleep(1)
        # Не торчит CPA-катофов в ПИ
        time.sleep(0.5)
        cpa_cutoffs = mbi_partner.get_cpa_cutoffs(shop_id)
        self.assertEqual(set([]), cpa_cutoffs)

        # Ручка cpcState в порядке
        time.sleep(0.5)
        cpc_state = mbi_partner.cpc_state(shop_id)
        self.assertTrue(len(cpc_state['result']['cpcCutoffs']) <= 0)
        self.assertEqual(cpc_state['result']['cpc'], 'REAL')

        # Вручную устанавливаем магазину параметр 46 (IS_IN_INDEX), чтобы ПИ было хорошо
        # Значение этого параметр влияет на выдачу ручки getCampaignStates (см. код ниже, который
        # тестирует выдачу этой ручки)
        time.sleep(0.5)
        mbi_api.hack_set_shop_param_value(shop_id, 46, 'true')

        # Не ждем пока магаин будет включен джобой shopStateReportExecutor
        time.sleep(0.5)
        mbi_api.hack_set_shop_param_value(shop_id, 53, '0')

        # На эту ручку смотрит фронт ПИ, чтобы рисовать что-то там пользователю
        time.sleep(0.5)
        status_for_pi = mbi_partner.get_campaign_states(shop_id)
        self.assertEqual(status_for_pi, '1')  # 1 = Магазин включен/индекс опубликован

        # Проверяем, что магазин в CPA-состоянии REAL_NOT_INDEXED. Это означает, что индекс загружается.
        # Симмитировать загрузку фида с этой стороны сложно, т.к. состояние меняется в ответ на приход генлогов
        # индексатора. На этом остановимся.
        time.sleep(0.5)
        cpa_state = mbi_partner.get_cpa_state(shop_id)
        self.assertEqual('REAL_NOT_INDEXED', cpa_state.xpath('/data/cpastate/cpa-real')[0].text)

    def test_onboard_positive_case_newpi_no_global_prepay(self):
        """ Прохождение модерации магазином (первичное включение). Оптимистичный сценарий. Не-глобал+предоплата

        Сценарий:
            * Создаем магазин
            * Заполнить поля
            * Отправить на модерацию
            * Проверить, на какие модерации он послан
            * Иммитировать прохождение/непрохождение модерации
        """
        mbi_partner = mbi_common.MbiPartner(user_id=self.USER.id)
        mbi_api = mbi_common.MbiApi()
        mbi_billing = mbi_common.MbiBilling()

        time.sleep(0.5)
        cmpg_id, shop_id = mbi_common.register_shop(mbi_partner, user=self.USER, is_global=False)
        time.sleep(0.5)
        helpers.fill_shop_params_for_non_global(mbi_partner, shop_id, ut=self)
        time.sleep(0.5)
        helpers.enable_prepay(mbi_partner=mbi_partner, mbi_api=mbi_api, is_global=False, shop_id=shop_id, ut=self)

        # for debugging:
        # cmpg_id = 1086000677
        # shop_id = 44400235

        # Положить магазину денег на счет
        # mbi_billing.refill_balance(cmpg_id, 1000)
        # self.assertEqual(mbi_partner.get_shop_balance(shop_id), 1000.0)
        # mbi_billing.force_run_fincnce_executor()

        time.sleep(0.5)
        mbi_partner.cpa_status_update(shop_id, True)
        time.sleep(0.5)
        mbi_partner.update_cpc_placement(shop_id, True)
        time.sleep(0.5)
        mrs = mbi_partner.moderation_request_state(shop_id)
        self.assertTrue(mrs['result']['moderationEnabled'])

        # Push the button
        time.sleep(0.5)
        mbi_partner.push_ready_for_testing(shop_id)
        time.sleep(0.5)
        mrs = mbi_partner.moderation_request_state(shop_id)
        self.assertTrue('MODERATION_IN_PROGRESS' in mrs['result']['cpaModerationDisabledReasons'])
        self.assertTrue('MODERATION_IN_PROGRESS' in mrs['result']['cpcModerationDisabledReasons'])

        # Pretend that the feed was loaded into the moderaion index
        time.sleep(0.5)
        mbi_api.testing_shops_premoderation_feed_check_delete(shop_id, 'CPA')
        time.sleep(0.5)
        mbi_api.testing_shops_premoderation_feed_check_delete(shop_id, 'CPC')

        # Check the abo list
        time.sleep(0.5)
        checks = {ch.testing_type: ch for ch in mbi_api.qc_shops_premoderations_get_shop_checks_list(shop_id)}
        self.assertTrue(len(checks) == 2)  # магазин должен быть отправлен на две проверки
        self.assertTrue('3' in checks)  # отправлен на модерацию по CPA
        self.assertTrue('0' in checks)  # отправлен на модерацию по CPC
        self.assertEqual(checks['3'].try_num, 1)
        self.assertEqual(checks['3'].offline, False)
        self.assertEqual(checks['0'].try_num, 1)
        self.assertEqual(checks['0'].offline, False)

        # Push the notification from ABO
        time.sleep(0.5)
        mbi_api.qc_shops_premoderations_result(shop_id, '0')
        time.sleep(0.5)
        mbi_api.qc_shops_premoderations_result(shop_id, '3')

        # Магазин перешел в состояние ожидания загрузки фида в большой индекс.
        # иммитируем, что офферы загрузились в индекс
        # Втрое удаление:
        time.sleep(0.5)
        mbi_api.testing_shops_premoderation_feed_check_delete(shop_id, 'CPA')
        time.sleep(0.5)
        mbi_api.testing_shops_premoderation_feed_check_delete(shop_id, 'CPC')

        # Магия, которая нужна, чтобы исчез катов CPC_FOR_TESTING
        time.sleep(0.5)
        mbi_billing.force_run_moderation_executor()

        # Кнопка отправки на модерацию должна исчезнуть
        time.sleep(0.5)
        mrs = mbi_partner.moderation_request_state(shop_id)
        self.assertTrue('MODERATION_IN_PROGRESS' not in mrs['result']['cpaModerationDisabledReasons'])
        self.assertTrue('MODERATION_IN_PROGRESS' not in mrs['result']['cpcModerationDisabledReasons'])
        self.assertTrue(not mrs['result']['moderationEnabled'])

        # Не торчит катофов в ABO
        time.sleep(0.5)
        abo_cutoffs = mbi_api.shop_abo_cutoffs(shop_id)
        self.assertTrue(len(abo_cutoffs) <= 0)

        time.sleep(1)
        # Не торчит CPA-катофов в ПИ
        time.sleep(0.5)
        cpa_cutoffs = mbi_partner.get_cpa_cutoffs(shop_id)
        self.assertEqual(set([]), cpa_cutoffs)

        # Ручка cpcState в порядке
        time.sleep(0.5)
        cpc_state = mbi_partner.cpc_state(shop_id)
        self.assertTrue(len(cpc_state['result']['cpcCutoffs']) <= 0)
        self.assertEqual(cpc_state['result']['cpc'], 'REAL')

        # Вручную устанавливаем магазину параметр 46 (IS_IN_INDEX), чтобы ПИ было хорошо
        # Значение этого параметр влияет на выдачу ручки getCampaignStates (см. код ниже, который
        # тестирует выдачу этой ручки)
        time.sleep(0.5)
        mbi_api.hack_set_shop_param_value(shop_id, 46, 'true')

        # Не ждем пока магаин будет включен джобой shopStateReportExecutor
        time.sleep(0.5)
        mbi_api.hack_set_shop_param_value(shop_id, 53, '0')

        # На эту ручку смотрит фронт ПИ, чтобы рисовать что-то там пользователю
        time.sleep(0.5)
        status_for_pi = mbi_partner.get_campaign_states(shop_id)
        self.assertEqual(status_for_pi, '1')  # 1 = Магазин включен/индекс опубликован

        # Проверяем, что магазин в CPA-состоянии REAL_NOT_INDEXED. Это означает, что индекс загружается.
        # Симмитировать загрузку фида с этой стороны сложно, т.к. состояние меняется в ответ на приход генлогов
        # индексатора. На этом остановимся.
        time.sleep(0.5)
        cpa_state = mbi_partner.get_cpa_state(shop_id)
        self.assertEqual('REAL_NOT_INDEXED', cpa_state.xpath('/data/cpastate/cpa-real')[0].text)

    def test_onboard_oldpi_cpc(self):
        """ Только CPC
        """
        time.sleep(0.5)
        moderation.onboard(
            self,
            is_global=False,
            is_new_pi=False,
            is_cpc=True,
            is_cpa=False,
            is_api=False)

    def test_onboard_oldpi_cpc_cpa(self):
        """ CPC+CPA
        """
        time.sleep(0.5)
        moderation.onboard(
            self,
            is_global=False,
            is_new_pi=False,
            is_cpc=True,
            is_cpa=True,
            is_api=False)

    def test_onboard_oldpi_cpc_cpa_api(self):
        """ CPC+CPA + API
        """
        time.sleep(0.5)
        moderation.onboard(
            self,
            is_global=False,
            is_new_pi=False,
            is_cpc=True,
            is_cpa=True,
            is_api=True)

    def test_failed_moderation_clone(self):
        """ ABO фейлит модерацию: клон
        """
        time.sleep(0.5)
        moderation.onboard(
            self,
            is_global=False,
            is_new_pi=False,
            is_cpc=True,
            is_cpa=False,
            is_api=False,
            fail_moderaion_case='common_clone')

    def test_failed_moderation_low_quality(self):
        """ ABO фейлит модерацию: плохое качество
        """
        time.sleep(0.5)
        moderation.onboard(
            self,
            is_global=False,
            is_new_pi=False,
            is_cpc=True,
            is_cpa=False,
            is_api=False,
            fail_moderaion_case='common_low_quality')

    def test_failed_moderation_low_quality_and_try_again(self):
        """ ABO фейлит модерацию: плохое качество, отправляемся на проверку снова и
            проходим ее и включаемся
        """
        time.sleep(0.5)
        cmpg_id, shop_id = moderation.onboard(
            self,
            is_global=False,
            is_new_pi=False,
            is_cpc=True,
            is_cpa=False,
            is_api=False,
            fail_moderaion_case='common_low_quality')
        mbi_partner = mbi_common.MbiPartner()
        mbi_billing = mbi_common.MbiBilling()

        # тут может мигать, если экзекьютор успел прокрутиться
        time.sleep(0.5)
        mrs = mbi_partner.moderation_request_state(shop_id)
        self.assertTrue('MODERATION_IN_PROGRESS' in mrs['result']['cpcModerationDisabledReasons'])
        self.assertTrue(not mrs['result']['moderationEnabled'])

        # перевдет проверку из ready_to_fail -> failed
        time.sleep(0.5)
        mbi_billing.force_run_moderation_executor()
        time.sleep(0.5)
        mrs = mbi_partner.moderation_request_state(shop_id)
        self.assertTrue(mrs['result']['moderationEnabled'])

        time.sleep(0.5)
        mbi_partner.push_ready_for_testing(shop_id)
        time.sleep(0.5)
        mrs = mbi_partner.moderation_request_state(shop_id)
        self.assertTrue('MODERATION_IN_PROGRESS' in mrs['result']['cpcModerationDisabledReasons'])
        self.assertTrue(not mrs['result']['moderationEnabled'])


    def test_failed_moderation_low_quality_and_try_again_and_gain_and_again_till_the_end(self):
        """ ABO фейлит модерацию: плохое качество, отправляемся на проверку снова и
            снова фейлим. Смотрим на счетчики. После того как число попыток исчерпается уже не можно
            отправиться на модерацию.
        """
        cmpg_id, shop_id = moderation.onboard(
            self,
            is_global=False,
            is_new_pi=False,
            is_cpc=True,
            is_cpa=False,
            is_api=False,
            fail_moderaion_case='common_low_quality')
        mbi_partner = mbi_common.MbiPartner()
        mbi_api = mbi_common.MbiApi()
        mbi_billing = mbi_common.MbiBilling()

        for i in xrange(6):
            case_log.CaseLogger.get().log('Will try to pass moderaion again. i={}'.format(i))
            time.sleep(0.5)
            mrs = mbi_partner.moderation_request_state(shop_id)

            if i == 5:
                self.assertTrue('NO_MORE_ATTEMPTS' in mrs['result']['cpcModerationDisabledReasons'])
                break
            self.assertTrue('MODERATION_IN_PROGRESS' in mrs['result']['cpcModerationDisabledReasons'])
            self.assertTrue(not mrs['result']['moderationEnabled'])

            # переведет проверку из ready_to_fail -> failed
            time.sleep(0.5)
            mbi_billing.force_run_moderation_executor()
            time.sleep(0.5)
            mrs = mbi_partner.moderation_request_state(shop_id)
            self.assertTrue(mrs['result']['moderationEnabled'])

            time.sleep(0.5)
            mbi_partner.push_ready_for_testing(shop_id)

            time.sleep(0.5)
            mbi_api.testing_shops_premoderation_feed_check_delete(shop_id, 'CPC')
            time.sleep(0.5)
            checks = {ch.testing_type: ch for ch in mbi_api.qc_shops_premoderations_get_shop_checks_list(shop_id)}
            print checks
            case_log.CaseLogger.get().log('Shop checks requestes: {}'.format(checks))
            self.assertTrue(len(checks) == 1)  # магазин должен быть отправлен на две проверки
            self.assertTrue('0' in checks)  # отправлен на модерацию по CPC
            self.assertEqual(checks['0'].try_num, 2+i)
            self.assertEqual(checks['0'].offline, False)

            time.sleep(0.5)
            mrs = mbi_partner.moderation_request_state(shop_id)
            self.assertTrue('MODERATION_IN_PROGRESS' in mrs['result']['cpcModerationDisabledReasons'])
            self.assertTrue(not mrs['result']['moderationEnabled'])

            # снова отвечаем от АБО, что качество Г
            time.sleep(0.5)
            mbi_api.qc_shops_premoderations_result(shop_id, '0', quality_cs=mbi_common.PremoderationResult.FAILED)
            time.sleep(0.5)
            cpc_state = mbi_partner.cpc_state(shop_id)
            self.assertTrue(str(mbi_common.CutoffType.FORTESTING) in cpc_state['result']['cpcCutoffs'])


    def test_failed_moderation_critical_quality(self):
        """ ABO фейлит модерацию: недопустимо плохое качество (не пускать на Маркет)
        """
        time.sleep(0.5)
        cmpg_id, shop_id = moderation.onboard(
            self,
            is_global=False,
            is_new_pi=False,
            is_cpc=True,
            is_cpa=False,
            is_api=False,
            fail_moderaion_case='common_critical_quality')

    def test_postmoderation_cutoff_cpc_for_cpc_only_shop(self):
        """ Размещающемуся магазину прилетает катоф. QUALITY_PINGER; QMANAGER_OTHER
        """
        time.sleep(0.5)
        cmpg_id, shop_id = moderation.onboard(
            self,
            is_global=False,
            is_new_pi=False,
            is_cpc=True,
            is_cpa=False,
            is_api=False)
        mbi_api = mbi_common.MbiApi()
        time.sleep(0.5)
        mbi_api.abo_cutoff_open(shop_id, 'UNAVAILABLE_SITE')

        # TODO:
        # Что в ПИ стало видно
        # Что в ручке для або начали торчать катофы
        # Проверить, что CPA тоже отрубилось

        time.sleep(0.5)
        abo_cutoffs = mbi_api.shop_abo_cutoffs(shop_id)
        self.assertTrue('UNAVAILABLE_SITE' in abo_cutoffs)
        self.assertTrue(len(abo_cutoffs) == 1)

    def test_postmoderation_cutoff_cpc_for_cpccpa_shop(self):
        """ Размещающемуся магазину прилетает катоф. QUALITY_PINGER; QMANAGER_OTHER
        """
        time.sleep(0.5)
        cmpg_id, shop_id = moderation.onboard(
            self,
            is_global=False,
            is_new_pi=False,
            is_cpc=True,
            is_cpa=True,
            is_api=False)
        mbi_api = mbi_common.MbiApi()
        mbi_partner = mbi_common.MbiPartner()
        time.sleep(0.5)
        mbi_api.abo_cutoff_open(shop_id, 'UNAVAILABLE_SITE')

        # TODO:
        # Что в ПИ стало видно
        # Что в ручке для або начали торчать катофы
        # Проверить, что CPA тоже отрубилось

        time.sleep(0.5)
        abo_cutoffs = mbi_api.shop_abo_cutoffs(shop_id)
        self.assertTrue('UNAVAILABLE_SITE' in abo_cutoffs)
        self.assertTrue(len(abo_cutoffs) == 1)

        # TODO: решить, что тут делать?
        time.sleep(0.5)
        cpa_cutoffs = mbi_partner.get_cpa_cutoffs(shop_id)
        self.assertTrue(not cpa_cutoffs)

    # TODO:
    # Завалить проерку по качеству, исправить и разместиться по CPC{CPA}
    # Задергать проверками и исчерпать счетчик


if __name__ == '__main__':
    unittest.main()
