#!/usr/bin/env python
# coding=utf-8

import unittest
import os
import helpers
import mbi_common
import time


def onboard(ut, is_global, is_api, is_cpc, is_cpa, is_new_pi, fail_moderaion_case=None, is_online=True):
    """ Зарегистрировать магазин и провести его через процесс модерации.
    Модерация закончится не обязательно успешно.
    """
    mbi_partner = mbi_common.MbiPartner(user_id=ut.USER.id)
    mbi_api = mbi_common.MbiApi()
    mbi_billing = mbi_common.MbiBilling()

    if not is_global:
        cmpg_id, shop_id = mbi_common.register_shop(
            mbi_partner,
            user=ut.USER,
            is_global=False,
            enable_programs_tumblers=is_new_pi,
            is_online=is_online)
    else:
        pass
        # assert 0

    # for debugging:
    # cmpg_id = 1086000677
    # shop_id = 44400243

    time.sleep(0.5)
    helpers.fill_shop_params_for_non_global(mbi_partner, shop_id, ut=ut)

    time.sleep(0.5)
    mbi_api.hack_manage_cutoff(shop_id, mbi_common.CutoffType.FINANCE, 'open')

    # API
    if is_api:
        time.sleep(0.5)
        enable_api(mbi_partner, shop_id)

    # Положить магазину денег на счет
    # mbi_billing.refill_balance(cmpg_id, 1000)
    # ut.assertEqual(mbi_partner.get_shop_balance(shop_id), 1000.0)
    # mbi_billing.force_run_fincnce_executor()

    if not is_new_pi:
        # offline-магазины могут быть ни cpc ни cpa

        time.sleep(0.5)
        mbi_partner.push_ready_for_testing(shop_id)

        # Pretend that the feed was loaded into the moderaion index
        # mbi_api.testing_shops_premoderation_feed_check_delete(shop_id, 'CPA')
        time.sleep(0.5)
        mbi_api.testing_shops_premoderation_feed_check_delete(shop_id, 'CPC')

        # Check the abo list
        time.sleep(0.5)
        checks = {ch.testing_type: ch for ch in mbi_api.qc_shops_premoderations_get_shop_checks_list(shop_id)}
        print checks
        ut.assertTrue(len(checks) == 1)  # магазин должен быть отправлен на две проверки
        ut.assertTrue('0' in checks)  # отправлен на модерацию по CPC
        ut.assertEqual(checks['0'].try_num, 1 if is_cpc or is_cpa else 0)  # TODO: https://st.yandex-team.ru/MBI-22095
        ut.assertEqual(checks['0'].offline, not (is_cpc or is_cpa))

        # Случаи фейла общих проверок
        if fail_moderaion_case and fail_moderaion_case.startswith('common_'):
            if fail_moderaion_case == 'common_clone':
                time.sleep(0.5)
                mbi_api.qc_shops_premoderations_result(shop_id, '0', clone_cs=mbi_common.PremoderationResult.HALTED)
                cpc_state = mbi_partner.cpc_state(shop_id)
                ut.assertTrue(str(mbi_common.CutoffType.FORTESTING) in cpc_state['result']['cpcCutoffs'])
                ut.assertTrue(str(mbi_common.CutoffType.QMANAGER_CLONE) in cpc_state['result']['cpcCutoffs'])
                # TODO: проверить, что магазину показывается верное сообщение в ПИ
                # TODO: Что показывается плашка про фатальное отключение
                #   эту плашку я в письме описывала, когда про старую ручку getPremoderationInfo спрашивала, показывается по условию если need-testing=true and need-manager-approve=true
            elif fail_moderaion_case == 'common_low_quality':
                time.sleep(0.5)
                mbi_api.qc_shops_premoderations_result(shop_id, '0', quality_cs=mbi_common.PremoderationResult.FAILED)
                time.sleep(0.5)
                cpc_state = mbi_partner.cpc_state(shop_id)
                ut.assertTrue(str(mbi_common.CutoffType.FORTESTING) in cpc_state['result']['cpcCutoffs'])
                # TODO: проверить, что магазину показывается верное сообщение в ПИ
                # https://jing.yandex-team.ru/files/fantamp/Otchyot_po_kachestvu_2017-06-15_17-59-59.png
                # кнопка и галка из той же ручки need-quality-button-approve
                # https://partner.market.pepelac10et.yandex.ru/report-quality.xml?id=1086000925
            elif fail_moderaion_case == 'common_critical_quality':
                time.sleep(0.5)
                mbi_api.qc_shops_premoderations_result(shop_id, '0', clone_cs=mbi_common.PremoderationResult.HALTED)
                time.sleep(0.5)
                cpc_state = mbi_partner.cpc_state(shop_id)
                # TODO: проверить, что магазину показывается верное сообщение в ПИ
                # https://partner.market.pepelac10et.yandex.ru/report-quality.xml?id=1086000927
                # https://jing.yandex-team.ru/files/fantamp/Otchyot_po_kachestvu_2017-06-15_18-08-36.png
                # Natalia Roguleva, [Jun 15, 2017, 6:13 PM]:
                # про желтый кружок, и вообще про статусы, думаю Егор сможет быстрее меня подсказать, он в этом уже разобрался вроде, как сейчас есть, а про кнопку need-quality-button-approve = 'true' and need-manager-approve='false' and not(testing-details/testing-info/ready = 'true' or testing-details/testing-info/in-progress = 'true')
                # из getPremoderationFullInfo
            else:
                raise Exception('Unkonwn common_ fail case')
            # TODO: что еще проверить?
            #  количество попыток и рисовать или нет кнопку отправки оттуда же берется
            # attempts-left
            # внутри datasource-premoderation-info

            return cmpg_id, shop_id
        else:
            # Push the notification from ABO
            time.sleep(0.5)
            mbi_api.qc_shops_premoderations_result(shop_id, '0')

            # Магазин перешел в состояние ожидания загрузки фида в большой индекс.
            # иммитируем, что офферы загрузились в индекс
            # Второе удаление:
            time.sleep(0.5)
            mbi_api.testing_shops_premoderation_feed_check_delete(shop_id, 'CPC')

            # Магия, которая нужна, чтобы исчез катоф CPC_FOR_TESTING
            time.sleep(0.5)
            mbi_billing.force_run_moderation_executor()

            # Не торчит катофов в ABO
            time.sleep(0.5)
            abo_cutoffs = mbi_api.shop_abo_cutoffs(shop_id)
            ut.assertTrue(len(abo_cutoffs) <= 0)

            # тест до закрытия финансового катофа
            time.sleep(0.5)
            cpc_state = mbi_partner.cpc_state(shop_id)
            ut.assertTrue(str(mbi_common.CutoffType.FINANCE) in cpc_state['result']['cpcCutoffs'])
            ut.assertEqual(cpc_state['result']['cpc'], 'NONE')

            time.sleep(0.5)
            mbi_api.hack_manage_cutoff(shop_id, mbi_common.CutoffType.FINANCE, 'close')
            time.sleep(1)  # нужно время, чтобы шутка дошла до mbi-partner

            # Ручка cpcState в порядке
            time.sleep(0.5)
            cpc_state = mbi_partner.cpc_state(shop_id)
            ut.assertTrue(len(cpc_state['result']['cpcCutoffs']) <= 0)
            ut.assertEqual(cpc_state['result']['cpc'], 'REAL')

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
            ut.assertEqual(status_for_pi, '1')  # 1 = Магазин включен/индекс опубликован

            # Нет желтой плашки что "CPA включается"" в ПИ (был такой баг)
            # оно показывается если есть катоф CPA_FOR_TESTING и can-switch-to-off не равно 'IMPOSSIBLE', из ручки getCpaState
            time.sleep(0.5)
            cpa_cutoffs = mbi_partner.get_cpa_cutoffs(shop_id)
            ut.assertTrue('CPA_FOR_TESTING' not in cpa_cutoffs)

        ### С этого момента считаем, что магазин включен в CPC и можно подключать его к CPA
        if is_cpa:
            time.sleep(0.5)
            mbi_partner.cpa_status_update(shop_id, True)
            time.sleep(0.5)
            mbi_api.testing_shops_premoderation_feed_check_delete(shop_id, 'CPA')

            # Check the abo list
            time.sleep(0.5)
            checks = {ch.testing_type: ch for ch in mbi_api.qc_shops_premoderations_get_shop_checks_list(shop_id)}
            ut.assertTrue(len(checks) == 1)  # магазин должен быть отправлен на две проверки
            ut.assertTrue('3' in checks)  # отправлен на модерацию по CPA
            # ut.assertEqual(checks['3'].try_num, 1)
            ut.assertEqual(checks['3'].offline, False)

            time.sleep(0.5)
            mbi_api.qc_shops_premoderations_result(shop_id, '3')

            time.sleep(0.5)
            mbi_api.testing_shops_premoderation_feed_check_delete(shop_id, 'CPA')

            # Не торчит катофов в ABO
            time.sleep(0.5)
            abo_cutoffs = mbi_api.shop_abo_cutoffs(shop_id)
            ut.assertTrue(len(abo_cutoffs) <= 0)

            time.sleep(0.5)
            mbi_billing.force_run_moderation_executor()

            # Не торчит CPA-катофов в ПИ
            time.sleep(0.5)
            cpa_cutoffs = mbi_partner.get_cpa_cutoffs(shop_id)
            ut.assertEqual(set([]), cpa_cutoffs)

            # На эту ручку смотрит фронт ПИ, чтобы рисовать что-то там пользователю
            time.sleep(0.5)
            status_for_pi = mbi_partner.get_campaign_states(shop_id)
            ut.assertEqual(status_for_pi, '1')  # 1 = Магазин включен/индекс опубликован

            # Проверяем, что магазин в CPA-состоянии REAL_NOT_INDEXED. Это означает, что индекс загружается.
            # Симмитировать загрузку фида с этой стороны сложно, т.к. состояние меняется в ответ на приход генлогов
            # индексатора. На этом остановимся.
            time.sleep(0.5)
            cpa_state = mbi_partner.get_cpa_state(shop_id)
            ut.assertEqual('REAL_NOT_INDEXED', cpa_state.xpath('/data/cpastate/cpa-real')[0].text)
    else:
        pass
        # assert 0
    return cmpg_id, shop_id


def enable_api(mbi_partner, shop_id):
    time.sleep(0.5)
    mbi_partner.generate_push_api_token(shop_id)
    time.sleep(0.5)
    mbi_partner.activate_push_api_token(shop_id)
    time.sleep(0.5)
    mbi_partner.push_api_settings(shop_id)
    time.sleep(0.5)
    mbi_partner.cpa_order_processing_mode_update(shop_id, 'API')


def main():
    pass


if __name__ == '__main__':
    if os.environ.get('UNITTEST') == '1':
        unittest.main()
    else:
        main()
