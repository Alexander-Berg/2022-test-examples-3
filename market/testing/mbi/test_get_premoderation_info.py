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


class TestGetPremoderationIngo(case_base.MbiTestCase):
    """ Тестирование ручки getPremoderationInfo vs moderationRequestState

        TODO:
    Регнуть магаз. Послать на CPA, CPA, CPC+CPA: getPremoderationInfo / 3. если testing-info/ready = 'true' and testing-info/cancelled = 'false' and not(missed-fields/data/uni-shop-information[name!='outlet-to-publish']) то текст "Магазин на проверке. Как только вы получите уведомление об успешном результате проверки, вы сможете пополнить счет. Ваши товарные предложения появятся в системе Яндекс.Маркет сразу после оплаты."

    2. если need-testing=true and need-manager-approve=true то текст в плашке "Чтобы пройти проверку, пожалуйста, обратитесь в Службу поддержки или к Вашему менеджеру.»

    1. если need-quality-button-approve = 'true' and /need-manager-approve='false' and not(testing-info/ready = 'true' or /testing-info/in-progress = 'true') то рисуем галку "все проблемы исправлены" и кнопка отправки на проверку

    если (testing-info/ready = 'true' or testing-info/in-progress = 'true') and premod-end/data/text() то пишем "Результаты проверки будут доступны дата и время"

    материалам. Подробности в уведомлении"
            и если quality/data/testing-details/testing-info/attempts-left) = 0, то к нему добавляем текст
            «Вы исчерпали количество возможных попыток прохождения проверки. По вопросу дальнейшего размещения в Яндекс.Маркете вы можете обратиться к&#160;вашему менеджеру или в Службу поддержки"
            если quality/data/testing-details/testing-info/attempts-left) > 0 то добавляем надпись "осталось Х попыток"

    иначе текст "Размещение вашего магазина приостановлено. Причины отключения вы можете посмотреть в уведомлении, которое на почте"
            если need-manager-approve='false', то добавляется текст
            "Для возобновления размещения вам необходимо привести все материалы в соответствие с нашими требованиями, после чего отправить магазин на повторную проверку."
            "Чтобы отправить магазин на проверку, необходимо поставить галочку Все проблемы исправлены и нажать на кнопку  Отправить на проверку."
            если need-manager-approve='true', то добавляется текст
            "Ваш магазин был отключен менеджером Яндекс.Маркета. Это значит, что дальнейшее размещение в системе Яндекс.Маркет невозможно. ... обратитесь в поддержку"

        если number(testing-info/attempts-left) = 0 то добавляется текст "Вы исчерпали количество возможных попыток прохождения проверки. По вопросу дальнейшего размещения к поддержке или менеджеру"
            если есть cutoff-date то "размещение приостановлено"
            если timeout-finished = 'false' and testing-start-date/text() то "прохождение проверки возможно не ранее дата и время"

    Счетчики в новой ручке MRS и старой GPI должны совпадать.

    """

    def test_get_premoderation_info_null(self):
        """ Тест фикса бага: https://st.yandex-team.ru/MBI-21831

        Сценарий:
            Зарегать, заполнить, взести CPC:on.
            Не должно упасть (до фикса падает изза пустой выдачи ручки)
        """
        mbi_partner = mbi_common.MbiPartner(user_id=self.USER.id)
        campaign_id, shop_id = mbi_common.register_shop(mbi_partner, user=self.USER)
        helpers.fill_shop_params_for_global(mbi_partner, shop_id, ut=self)

        mbi_partner.update_cpc_placement(shop_id, True)

        # Не должно упасть. Был баг связанный с мемкешем и падало
        # https://st.yandex-team.ru/MBI-21831
        mbi_partner.get_premoderation_info(shop_id)



if __name__ == '__main__':
    unittest.main()
