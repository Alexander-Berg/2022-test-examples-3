#!/usr/bin/env python
# coding=utf-8

import unittest
import os
import time
import moderation
import case_base
import mbi_common
import helpers
import time
from unittest import skip


class T(case_base.MbiTestCase):

    def test_postmoderation_issues(self):
        """ Сценарий:
            * Зарегать магаз
            * Провести его через модерацию чтобы он начал размещаться
            * Начать открывать и закрывать ему всякие катофы от АБО и проверять, есть у него кнопка "Я исправился" или нету и снялся ил он с размещения

            Таблица входных магазинов
            CPA CPC
            x   x
            x
                x
            Итого: три варианта магазинов (API, предоплату и прочее не берем в тест)

            Таблица тестов:
            Катоф                   Результат   Примечание
            CPA_QUALITY             support
            CPC_QUALITY             support
            COMMON_QUALITY          support
            CPA_QUALITY_API[nt=0]   auto        Не должно быть открыто CPA_NEED_TESTING|CPA_FOR_TESTING. Закроем сами. Кнопку не видит.
            CPA_QUALITY_API[nt=1]   light
            CPA_QUALITY_AUTO        timed
            CPA_QUALITY_OTHER       light
            CPС_QUALITY_OTHER       light
            COMMON_OTHER            light

            nt=1/0 need testing. ABO сигнализирвует, надо ли отправлять на проверку по этому отключению или оно само
                снимет катоф, когда проблема исчезнет.

            Расшифровка исходов:
            support — магазин не должен видеть кнопку "Пройти модерацию" на странице
                        качества и на странице параметров (ручки getCPAState, moderationRequestState,
                        getPremoderationInfo, getPremoderationFullInfo). Спросить Наташу, по каким признакам
                        старый фронт отображает Кнопку и по каким Старый.
                        С кнопкой в новом фронте, которая завязана на moderationRequestState проблем не будет, там
                        простое API. А вот про другие надо спросить Наташу.
                        После того как убедились, что кнопок не видно, надо снять катофф и переходить к след. кейсу
            timed - магазин видит, что повторно пойти на модерацию он может после како-то числа. Проверять, что для
                        ПИ выдаются нужные тэги в ответах ручек (спросить Наташу aeronka@)
            light - проверить, что создался light-тикет для АБО. Дальше симитировать ответ АБО. Убедиться, что катоф закрылся
                        и магазин начал размещаться. Проверка того, что магазин размечается см. в test_moderation.py
            auto - магазин видит сообщение, что мы сами его включим, когда он поправит ошибки. Кнопки не видит.
        """
        pass

    @skip('SQL error')
    def test_postmoderation_issues_cpa_cpc(self):
        """
        CPA+CPC
        """
        mbi_partner = mbi_common.MbiPartner(user_id=self.USER.id)
        mbi_api = mbi_common.MbiApi()

        # Регаем магаз CPA+CPC
        _, shop_id = mbi_common.register_shop(mbi_partner, user=self.USER)
        mrs = mbi_partner.moderation_request_state(shop_id)
        print mrs
        self.assertTrue('PROGRAM_IS_NOT_SELECTED' in mrs['result']['cpcModerationDisabledReasons'])
        self.assertTrue('PROGRAM_IS_NOT_SELECTED' in mrs['result']['cpaModerationDisabledReasons'])

        co = mbi_api.shop_abo_cutoffs(shop_id, only_active=True)
        self.assertTrue(len(co) <= 0)

        # Начальное состояние
        cpc_state = mbi_partner.cpc_state(shop_id)
        self.assertTrue('44' in cpc_state['result']['cpcCutoffs'])  # Для старого фронта не долджно быть так!
        self.assertEqual(cpc_state['result']['canSwitchToOn'], True)
        cpa_state = mbi_partner.get_cpa_state(shop_id)
        cpa_cutoffs = set([x.text for x in cpa_state.xpath('/data/shop-cutoffs/cutoff-type')])
        self.assertTrue(set(['CPA_PARTNER', 'CPA_NEED_TESTING']).issubset(cpa_cutoffs))  # У магазина созданного через старый ПИ не долждно быть cpa_partner
        mrs = mbi_partner.moderation_request_state(shop_id)
        self.assertTrue('PROGRAM_IS_NOT_SELECTED' in mrs['result']['cpaModerationDisabledReasons'])
        self.assertTrue('PROGRAM_IS_NOT_SELECTED' in mrs['result']['cpcModerationDisabledReasons'])

        # (CPA: on, CPC: on)
        mbi_partner.cpa_status_update(shop_id, True)
        mbi_partner.update_cpc_placement(shop_id, True)
        cpc_state = mbi_partner.cpc_state(shop_id)
        self.assertTrue('44' not in cpc_state['result']['cpcCutoffs'])
        self.assertTrue('6' in cpc_state['result']['cpcCutoffs'])
        self.assertEqual(cpc_state['result']['canSwitchToOn'], True)
        cpa_state = mbi_partner.get_cpa_state(shop_id)
        cpa_cutoffs = set([x.text for x in cpa_state.xpath('/data/shop-cutoffs/cutoff-type')])
        self.assertTrue({'CPA_FOR_TESTING', 'CPA_NEED_TESTING'}.intersection(cpa_cutoffs)) # открыто или то или то (потому что может прилетать FINANCE со стороны)
        mrs = mbi_partner.moderation_request_state(shop_id)
        # закомменчено т.к. отключили, т.к. ломает фронт (спросить Витю)
        # self.assertTrue('MISSED_DATASOURCE_PARAMS' in mrs['result']['cpaModerationDisabledReasons'])
        self.assertTrue('PROGRAM_IS_NOT_SELECTED' not in mrs['result']['cpaModerationDisabledReasons'])
        self.assertTrue('PROGRAM_IS_NOT_SELECTED' not in mrs['result']['cpcModerationDisabledReasons'])

        # Заполнить параметры размещения и проверять, что вида кнопка "на модерацию"
        helpers.fill_shop_params_for_global(mbi_partner, shop_id, ut=self)
        mbi_partner.update_cpc_placement(shop_id, True)
        mbi_partner.cpa_status_update(shop_id, True)
        mrs = mbi_partner.moderation_request_state(shop_id)
        self.assertTrue(mrs['result']['moderationEnabled'])



    def test_light_ticket_followed_by_one_more_cutoff(self):
        """ Проверяем, что MBI корректно отрабатывает кейс, когда магазину, находящемуся уже на проверке
        долетают еще замечания (катофы).

        Сценарий:
            * Магазин размещается
            * Ему прилетает какой-то light-катофф
            * Вдогонку прилетает еще катоф.

        Пример:
            Магазин грубил по телефону. Находится на light-common. В это время прилетает cpc-fatal. Надо: включить только в cpa.
        """
        cmpg_id, shop_id = moderation.onboard(
            self,
            is_global=False,
            is_new_pi=False,
            is_cpc=True,
            is_cpa=True,
            is_api=False)

        mbi_api = mbi_common.MbiApi()
        mbi_partner = mbi_common.MbiPartner()

        mbi_api.abo_cutoff_open(shop_id, 'COMMON_OTHER')
        time.sleep(1)

        cpc_state = mbi_partner.cpc_state(shop_id)
        # TODO: check state

        cpa_state = mbi_partner.get_cpa_state(shop_id)
        # TODO: check state

        # будет пофикшено в релизе-4
        mrs = mbi_partner.moderation_request_state(shop_id)
        self.assertTrue(mrs['result']['moderationEnabled'])

        mbi_partner.push_ready_for_testing(shop_id)

        mrs = mbi_partner.moderation_request_state(shop_id)
        self.assertTrue('MODERATION_IN_PROGRESS' in mrs['result']['cpcModerationDisabledReasons'])
        self.assertTrue(not mrs['result']['moderationEnabled'])

        # check for light cutoff

        # open one more cutoff

        # make response to light-ticket

        # check that only one program is active


if __name__ == '__main__':
    unittest.main()
