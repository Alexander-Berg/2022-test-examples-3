#!/usr/bin/env python
# -*- coding: utf-8 -*-

import __classic_import  # noqa
import market.media_adv.incut_search.mt.env as env

from market.media_adv.incut_search.beam.incut import IncutModelsList
from market.media_adv.incut_search.beam.model import ModelWithBid

incut_count = 12
setted_rp = 21
flag_rp = 17
base_max_bids = 43
hids_with_incut = []


class T(env.MediaAdvIncutSearchSuite):
    @classmethod
    def setup_market_access_resources(cls):
        """
        Список hid с RP
        """
        hids_list_with_rp = [x for x in range(1, 13)]
        cls.access_resources.reserve_prices = {}
        for hid in hids_list_with_rp:
            cls.access_resources.reserve_prices[hid] = setted_rp

    @classmethod
    def create_hids(cls, start_hid=1, end_hid=10, step_for_hid=1):
        """
        Формирование списка для запроса
        :param start_hid: начало диапазона
        :param end_hid: конец диапазона
        :param step_for_hid:  шаг
        :return: list
        """
        hids = [x for x in range(start_hid, end_hid + 1, step_for_hid)]
        return hids

    @classmethod
    def hids_for_response(cls, hid_list=[]):
        """
        Преобразование списка hids в строку. Hids записываются через  ","
        :param hid_list: список с hids
        :return: str
        """
        return ','.join([str(hid) for hid in hid_list])

    @classmethod
    def create_answer(
        cls,
        hids_list=[],
        flag=False,
    ):
        """
        Создание ответа с заданными параметрами
        :param hids_list: список с hids
        :param flag: используется ли флаг
        :return: dict
        """
        answer = {}
        answer_hid = {}
        for hid in hids_list:
            min_bid = T.default_rp
            if hid in cls.access_resources.reserve_prices:
                min_bid = setted_rp
            else:
                if flag:
                    min_bid = flag_rp
            max_bid = min_bid
            if hid in hids_with_incut:
                max_bid = base_max_bids + 1
            bids = {
                "minBid": min_bid,
                "maxBid": max_bid,
            }
            answer_hid[str(hid)] = bids
            answer["result"] = answer_hid
        return answer

    @classmethod
    def setUpClass(cls):
        """
        переопределенный метод для дополнительного вызова настроек
        """
        cls.settings.access_using = True
        super(T, cls).setUpClass()

    @classmethod
    def prepare_incuts(cls):
        start_hid = 7
        start_vendor_id = 10
        start_datasource_id = 10
        start_model_id = 10
        global hids_with_incut
        hids_with_incut = [start_hid + x for x in range(1, incut_count + 1)]
        cls.content.incuts += [
            IncutModelsList(
                hid=start_hid + x,
                vendor_id=start_vendor_id + x,
                datasource_id=start_datasource_id + x,
                bid=base_max_bids,
                models=[ModelWithBid(model_id=start_model_id + z) for z in range(1, incut_count + 1)],
            )
            for x in range(1, incut_count + 1)
        ]

    """
    Диапазоны hids
    [1, 7] - hids, у которых есть только РП
    [13, 20] - hids, у которых есть только врезки
    [8 , 12] - hids, у которых есть оба параметра
    """
    """
    Тест 1.
    Проверка ответа для hids без РП и без флага exp_flags=market_madv_min_rp_for_all_hids
    Для всех hid нет врезки.
    Ответ минимальная ставка = 35, максимальная = 35
    """

    def test_answer_no_rp_no_flag_no_incut(self):

        target_hids = self.create_hids(21, 24, 1)
        hid = 11476
        resp = self.hids_for_response(target_hids)
        response = self.request({'hid': hid, 'target_hids': resp}, handler='recommends')
        answer = self.create_answer(target_hids, False)
        self.assertFragmentIn(response, answer)

    """
    Тест 2.
    Проверка ответа для hids без РП и без флага exp_flags=market_madv_min_rp_for_all_hids
    Для всех hid есть врезки есть.
    Ответ минимальная ставка = 35, максимальная = ставке из врезки + 1
    """

    def test_answer_no_rp_no_flag_with_incut(self):

        target_hids = self.create_hids(13, 16, 1)
        hid = 11476
        resp = self.hids_for_response(target_hids)
        response = self.request({'hid': hid, 'target_hids': resp}, handler='recommends')
        answer = self.create_answer(target_hids, False)
        self.assertFragmentIn(response, answer)

    """
    Тест 3.
    Проверка ответа для hids C РП и без флага exp_flags=market_madv_min_rp_for_all_hids
    Для всех hids нет врезки.
    Ответ - минимальная и максимальная ставка - заданная РП
    """

    def test_answer_with_rp_no_flag_no_incut(self):

        target_hids = self.create_hids(1, 5, 1)
        hid = 11476
        response = self.request({'hid': hid, 'target_hids': self.hids_for_response(target_hids)}, handler='recommends')
        answer = self.create_answer(target_hids, False)
        self.assertFragmentIn(response, answer)

    """
    Тест 4.
    Проверка ответа для hids C РП и без флага exp_flags=market_madv_min_rp_for_all_hids
    Для всех hid есть врезки.
    Ответ - минимальная равная заданной из РП, максимальная равна - ставка из врезки + 1
    """

    def test_answer_with_rp_no_flag_with_incut(self):

        target_hids = self.create_hids(8, 12, 1)
        hid = 11476
        response = self.request({'hid': hid, 'target_hids': self.hids_for_response(target_hids)}, handler='recommends')
        answer = self.create_answer(target_hids, False)
        self.assertFragmentIn(response, answer)

    """
    Тест 5.
    Проверка ответа для hids без РП, с флагом exp_flags=market_madv_min_rp_for_all_hids
    Для всех hids нет врезки.
    Ответ - минимальная и максимальная ставки равны значению, что установленно флагом
    """

    def test_with_flag_no_rp_no_incuts(self):
        target_hids = self.create_hids(21, 24, 1)
        hid = 11476
        response = self.request(
            {
                'hid': hid,
                'target_hids': self.hids_for_response(target_hids),
            },
            exp_flags={'market_madv_min_rp_for_all_hids': flag_rp},
            handler='recommends',
        )
        answer = self.create_answer(target_hids, True)
        self.assertFragmentIn(response, answer)

    """
    Тест 6.
    Проверка ответа для hids без РП, с флагом exp_flags=market_madv_min_rp_for_all_hids
    Для всех hids есть врезки.
    Ответ - минимальная равна значению, что установленно флагом. Максимальная равна значнию из врезки.
    """

    def test_with_flag_no_rp_with_incuts(self):
        target_hids = self.create_hids(15, 17, 1)
        hid = 11476
        response = self.request(
            {
                'hid': hid,
                'target_hids': self.hids_for_response(target_hids),
            },
            exp_flags={'market_madv_min_rp_for_all_hids': flag_rp},
            handler='recommends',
        )
        answer = self.create_answer(target_hids, True)
        self.assertFragmentIn(response, answer)

    """
    Тест 7.
    Проверка ответа для смешанного условия:
    часть имеет  только РП, часть только врезки, остальные - и врезки, и РП;
    без флага exp_flags=market_madv_min_rp_for_all_hids
    Ответ:
    Только РП - минимальная и максимальная ставка равна РП
    Только врекзи - минимальная = базовому значению (35), максимальная = равна значению из врезки.
    Остальные - минимальная ставка = значению из РП, максимальная = значению из врезки.
    """

    def test_mixed_no_flag(self):

        target_hids = [7, 11, 15]
        hid = 11476
        response = self.request({'hid': hid, 'target_hids': self.hids_for_response(target_hids)}, handler='recommends')
        answer = self.create_answer(target_hids, False)
        self.assertFragmentIn(response, answer)

    """
    Тест 8.
    Проверка ответа для смешанного условия:
    часть имеет  только РП, часть только врезки, остальные - и врезки, и РП;
    С флагом exp_flags=market_madv_min_rp_for_all_hids
    Ответ:
    Только РП - минимальная и максимальная ставка равна значению из флага
    Только врекзи - минимальная = значению из флага, максимальная = равна значению из врезки.
    Остальные - минимальная ставка = значению из флага, максимальная = значению из врезки.
    """

    def test_mixed_with_flag(self):
        target_hids = [7, 11, 15]
        hid = 11476
        response = self.request(
            {
                'hid': hid,
                'target_hids': self.hids_for_response(target_hids),
            },
            exp_flags={'market_madv_min_rp_for_all_hids': flag_rp},
            handler='recommends',
        )
        answer = self.create_answer(target_hids, True)
        self.assertFragmentIn(response, answer)


if __name__ == '__main__':
    env.main()
