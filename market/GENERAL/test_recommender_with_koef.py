#!/usr/bin/env python
# -*- coding: utf-8 -*-

import __classic_import  # noqa
import market.media_adv.incut_search.mt.env as env

from market.media_adv.incut_search.beam.incut import IncutModelsList
from market.media_adv.incut_search.beam.incut import IncutModelsWithBanner
from market.media_adv.incut_search.beam.model import ModelWithBid
from market.media_adv.incut_search.beam.media_element import Banner
from market.media_adv.incut_search.beam.image import Image


class T(env.MediaAdvIncutSearchSuite):
    banner_incut = 0  # Врезка с баннером
    model_list_incut = 0  # Врезка без баннера
    req_incut_vendor1 = 0  # Врезка без баннера для вендора 1
    req_incut_vendor3 = 0  # Врезка без баннера для вендора 3
    incut_with_foreign_hids1 = 0  # Врезка с доп. охватом
    empty_models_incut = 0  # Врезка без моделей

    @classmethod
    def prepare_incuts_for_normal_tests(cls):
        """
        Подготовка врезок:
        Две врезки в SaaS - без баннера (коэфф. 1); с баннером (коэфф. 1.3)
        Две врезки-кандидаты, для которых нужно вернуть макс. ставку (banner_incut, ordinary_incut)
        """
        start_hid = 7
        start_vendor_id = 10
        start_datasource_id = 10
        start_model_id = 10
        base_bid = 90  # Базовая ставка
        cls.content.incuts += [
            IncutModelsList(
                hid=start_hid,
                vendor_id=start_vendor_id,
                datasource_id=start_datasource_id,
                bid=base_bid,
                models=[ModelWithBid(model_id=start_model_id + z) for z in range(1, 4)],
            ),
            IncutModelsWithBanner(
                hid=start_hid + 1,
                vendor_id=start_vendor_id + 1,
                datasource_id=start_datasource_id + 1,
                bid=base_bid + 10,
                banner=Banner(
                    id=90,
                    image=Image(
                        url="image_url",
                        width=800,
                        height=600,
                    ),
                    click_url='click url',
                    pixel_url='pixel_url',
                    bid=67,
                ),
                models=[ModelWithBid(model_id=start_model_id + z * 10) for z in range(1, 4)],
            ),
        ]
        cls.model_list_incut = IncutModelsList(
            hid=start_hid + 2,
            vendor_id=start_vendor_id + 2,
            datasource_id=start_datasource_id + 2,
            bid=base_bid + 20,
            models=[ModelWithBid(model_id=start_model_id + z * 20) for z in range(1, 4)],
        )
        cls.banner_incut = IncutModelsWithBanner(
            hid=start_hid + 3,
            vendor_id=start_vendor_id + 3,
            datasource_id=start_datasource_id + 3,
            bid=base_bid + 30,
            models=[ModelWithBid(model_id=start_model_id + z * 30) for z in range(1, 4)],
            banner=Banner(
                id=90,
                image=Image(
                    url="image_url",
                    width=800,
                    height=600,
                ),
                click_url='click url',
                pixel_url='pixel_url',
                bid=67,
            ),
        )

    """
    Алгоритм вычисления максимальной ставки во всех тестах.
    Запрашиваем все имеющиеся врезки в SaaS.
    У врезки вычисляем ставку и домножаем ее на коэффициент врезки.
    Из имеющихся ставком ищем максимум.
    После того, как нашли максимумы для всех запрошенных категорий, учитываем коэффициент врезки-кандидата.
    У каждой категории делим ставку на коэффициент. Полученный результат округляется в большую сторону.
    """

    def test_same_koeff_both_no_banner(self):
        """
        Тест 1.
        Запрашиваем ответ для врезок с одинаковым коэффициентом.
        Обе врезки без баннера.
        Ответ 91.
        90 - ставка, 1.0 - коэффициент уже имеющейся врезки, 1.0 коэффициент врезки кандидата.
        90 * 1.0 / 1.0 = 91 (ceil 90)
        """
        response = self.request(
            {
                'target_hids': '7',
                'saas_incut': self.model_list_incut.serialize(),
            },
            exp_flags={
                'market_madv_incut_type_multiplier_models_with_banner': 1.3,
                'market_madv_incut_type_multiplier_models_with_adaptive_banner': 1.3,
                'market_madv_incut_type_multiplier_models_with_horizontal_banner': 1.3,
            },
            handler='recommends',
        )
        self.assertFragmentIn(response, {'result': {'7': {"maxBid": 91, "minBid": T.default_rp}}})

    def test_diff_koeff_saas_no_banner_requested_banner(self):
        """
        Тест 2.
        Запрашиваем ответ для врезок с разными коэффициентом.
        Врезка в SaaS без баннера, в запросе с баннером.
        Ответ 70.
        90 - ставка, 1.0 - коэффициент уже имеющейся врезки, 1.3 коэффициент врезки кандидата.
        90 * 1.0 / 1.3 = 70 (ceil 69.23)
        """
        response = self.request(
            {
                'target_hids': '7',
                'saas_incut': self.banner_incut.serialize(),
            },
            exp_flags={
                'market_madv_incut_type_multiplier_models_with_banner': 1.3,
                'market_madv_incut_type_multiplier_models_with_adaptive_banner': 1.3,
                'market_madv_incut_type_multiplier_models_with_horizontal_banner': 1.3,
            },
            handler='recommends',
        )
        self.assertFragmentIn(response, {'result': {'7': {"maxBid": 70, "minBid": T.default_rp}}})  #

    def test_diff_koeff_saas_banner_requested_no_banner(self):
        """
        Тест 3.
        Запрашиваем ответ для врезок с разными коэффициентом.
        Врезка в SaaS с баннером, в запросе без баннера.
        Ответ 131.
        100 - ставка, 1.3 - коэффициент уже имеющейся врезки, 1.0 коэффициент врезки кандидата.
        100 * 1.3 / 1.0 = 131 (ceil 130)
        """
        response = self.request(
            {
                'target_hids': '8',
                'saas_incut': self.model_list_incut.serialize(),
            },
            exp_flags={
                'market_madv_incut_type_multiplier_models_with_banner': 1.3,
                'market_madv_incut_type_multiplier_models_with_adaptive_banner': 1.3,
                'market_madv_incut_type_multiplier_models_with_horizontal_banner': 1.3,
            },
            handler='recommends',
        )
        self.assertFragmentIn(response, {'result': {'8': {"maxBid": 131, "minBid": T.default_rp}}})

    def test_same_koef_both_banner(self):
        """
        Тест 4.
        Запрашиваем ответ для врезок с одинаковым коэффициентом.
        Обе врезки с баннером.
        Ответ 101.
        100 - ставка, 1.3 - коэффициент уже имеющейся врезки, 1.3 коэффициент врезки кандидата.
        100 * 1.3 / 1.3 = 101 (ceil 100)
        """
        response = self.request(
            {
                'target_hids': '8',
                'saas_incut': self.banner_incut.serialize(),
            },
            exp_flags={
                'market_madv_incut_type_multiplier_models_with_banner': 1.3,
                'market_madv_incut_type_multiplier_models_with_adaptive_banner': 1.3,
                'market_madv_incut_type_multiplier_models_with_horizontal_banner': 1.3,
            },
            handler='recommends',
        )
        self.assertFragmentIn(response, {'result': {'8': {"maxBid": 101, "minBid": T.default_rp}}})

    @classmethod
    def prepare_incuts_for_wrong_answer(cls):
        start_hid = 10
        start_vendor_id = 40
        start_datasource_id = 40
        start_model_id = 40
        cls.content.incuts += [
            IncutModelsWithBanner(
                hid=start_hid,
                vendor_id=start_vendor_id + 1,
                datasource_id=start_datasource_id + 1,
                bid=109,
                banner=Banner(
                    id=90,
                    image=Image(
                        url="image_url",
                        width=800,
                        height=600,
                    ),
                    click_url='click url',
                    pixel_url='pixel_url',
                    bid=67,
                ),
                models=[ModelWithBid(model_id=start_model_id + z * 10) for z in range(1, 4)],
            )
        ]

    def test_wrong_answer(self):
        """
        Тест 5.
        Проверка работы правильного округления.
        С помощью флагов меняем коэффициенты и проверяем расчеты.
        Ответ должен быть 121.
        109 - ставка, 1.2 - коэффициент уже имеющейся врезки, 1.09 коэффициент врезки кандидата.
        109 * 1,2 / 1,09 = 121 (ceil 120)
        """

        response = self.request(
            {
                'target_hids': '10',
                'saas_incut': self.model_list_incut.serialize(),
            },
            exp_flags={
                'market_madv_incut_type_multiplier_models_list': 1.09,
                'market_madv_incut_type_multiplier_models_with_banner': 1.2,
                'market_madv_incut_type_multiplier_models_with_adaptive_banner': 1.2,
                'market_madv_incut_type_multiplier_models_with_horizontal_banner': 1.2,
            },
            handler='recommends',
        )
        self.assertFragmentIn(response, {'result': {'10': {"maxBid": 121, "minBid": T.default_rp}}})

    def test_recom_max_bid_coeff_flags(self):
        """
        Тест 6.
        проверка работы флагов, изменяющих макс ставку по формуле
        ставка = макс * (1 + mul) + add
        """
        response = self.request(
            {
                'target_hids': '7,8,9',
                'saas_incut': self.model_list_incut.serialize(),
            },
            exp_flags={
                'market_madv_incut_type_multiplier_models_with_banner': 1.3,
                'market_madv_incut_type_multiplier_models_with_adaptive_banner': 1.3,
                'market_madv_incut_type_multiplier_models_with_horizontal_banner': 1.3,
                'market_madv_recom_max_bid_coeff_mul': 2.5,
                'market_madv_recom_max_bid_coeff_add': 20,
                'market_madv_recom_max_bid_coeff_apply_to_rp': 0,
            },
            handler='recommends',
        )
        self.assertFragmentIn(
            response,
            {
                'result': {
                    '7': {"maxBid": 339, "minBid": T.default_rp},  # 91 * 3.5 + 20 = 338.5
                    '8': {"maxBid": 479, "minBid": T.default_rp},  # 131 * 3.5 + 20 = 478.5
                    '9': {"maxBid": T.default_rp, "minBid": T.default_rp},  # rp
                }
            },
        )

    def test_recom_max_bid_coeff_flags_for_rp(self):
        """
        Тест 7.
        Проверка работы флагов, изменяющих макс ставку по формуле, но без флагов изменяющих коэффициент врезки
        ставка = макс * (1 + mul) + add
        """
        response = self.request(
            {
                'target_hids': '9',
                'saas_incut': self.model_list_incut.serialize(),
            },
            exp_flags={
                'market_madv_recom_max_bid_coeff_mul': 2.5,
                'market_madv_recom_max_bid_coeff_add': 20,
                'market_madv_recom_max_bid_coeff_apply_to_rp': 1,
                'market_madv_min_rp_for_all_hids': 17,
            },
            handler='recommends',
        )
        self.assertFragmentIn(
            response,
            {
                'result': {
                    '9': {"maxBid": 80, "minBid": 17},  # 17 * 3.5 + 20 = 79.5
                }
            },
        )

    @classmethod
    def prepare_same_vendor_tests(cls):
        """
        Подготовка врезок - 2 врезки в SaaS  для проверки ставки того же вендора (vendor 1, vendor 2)
        Две врезки для запроса (vendor 1, vendor 3)
        """
        start_hid = 77
        start_vendor_id = 18
        start_datasource_id = 18
        start_model_id = 18
        base_bid = 50
        cls.content.incuts += [
            IncutModelsList(
                hid=start_hid,
                vendor_id=start_vendor_id,  # врезка вендора 1
                datasource_id=start_datasource_id,
                bid=base_bid,
                models=[ModelWithBid(model_id=start_model_id + z) for z in range(1, 4)],
            ),
            IncutModelsList(
                hid=start_hid,
                vendor_id=start_vendor_id + 1,
                datasource_id=start_datasource_id + 1,  # врезка вендора 2
                bid=base_bid - 10,
                models=[ModelWithBid(model_id=start_model_id + z) for z in range(1, 4)],
            ),
        ]
        cls.req_incut_vendor1 = IncutModelsList(
            hid=start_hid,
            vendor_id=start_vendor_id,  # запрашиваемая врезка вендора 1
            datasource_id=start_datasource_id,
            bid=base_bid,
            models=[ModelWithBid(model_id=start_model_id + z + 1) for z in range(1, 4)],
        )
        cls.req_incut_vendor3 = IncutModelsList(
            hid=start_hid,
            vendor_id=start_vendor_id + 2,  # запрашиваемая врезка вендора 3
            datasource_id=start_datasource_id,
            bid=base_bid + 5,
            models=[ModelWithBid(model_id=start_model_id + z + 2) for z in range(1, 4)],
        )

    def test_same_vendor_request(self):
        """
        Тест 8.
        Проверяем корректность ответа - запрашиваем ставку для врезки-кандидата того же вендора (вендор 1).
        В ответе - мин = РП, максимум = ставка вендора 2 + 1 (41)
        """
        response = self.request(
            {
                'target_hids': '77',
                'saas_incut': self.req_incut_vendor1.serialize(),
            },
            exp_flags={},
            handler='recommends',
        )
        self.assertFragmentIn(
            response,
            {
                'result': {
                    '77': {"maxBid": 41, "minBid": T.default_rp},  # 40*1.0 / 1.0 = 41 (ceil 40)
                }
            },
        )

    def test_diff_vendor_request(self):
        """
        Тест 9.
        Проверяем корректность ответа - запрашиваем ставку для другого вендора (vendor 3)
        В ответе - мин = РП, макс = самая большая ставка из врезок, что уже есть в SaaS (51)
        """
        response = self.request(
            {
                'target_hids': '77',
                'saas_incut': self.req_incut_vendor3.serialize(),
            },
            exp_flags={},
            handler='recommends',
        )
        self.assertFragmentIn(
            response,
            {
                'result': {
                    '77': {"maxBid": 51, "minBid": T.default_rp},  # 50*1.0 / 1.0 = 51 (ceil 50)
                }
            },
        )

    @classmethod
    def prepare_foreign_hids_tests(cls):
        """
        Подготовка врезок для проверки корректности отображения ставки для врезки с допоохватом в рекомендаторе
        Одна врезка в SaaS
        Две верзки-кандидаты.
        """
        start_hid = 1718
        start_vendor_id = 2024
        start_datasource_id = 2024
        start_model_id = 2024
        base_bid = 70
        cls.content.incuts += [
            IncutModelsList(
                hid=start_hid,
                foreign_hid=start_hid + 1,
                vendor_id=start_vendor_id,  # врезка вендора 1
                datasource_id=start_datasource_id,
                bid=base_bid,
                models=[ModelWithBid(model_id=start_model_id + z) for z in range(1, 4)],
            )
        ]
        cls.incut_with_foreign_hids1 = IncutModelsList(
            hid=start_hid,
            vendor_id=start_vendor_id + 2,
            datasource_id=start_datasource_id + 2,
            bid=base_bid - 10,
            foreign_hid=start_hid + 1,  # врезка - кандидат c доп. охватом, категория доп. охвата = 1719
            models=[ModelWithBid(model_id=start_model_id + z + 10) for z in range(1, 4)],
        )

    def test_main_hid_vs_main_hid(self):
        """
        Тест 10.
        Рассматриваем случай, когда для врезки в SaaS и врезки-кандидата категория является основной.
        В ответе - мин = РП, макс = ставка врезки из SaaS + 1
        """
        response = self.request(
            {
                'target_hids': '1718',
                'saas_incut': self.incut_with_foreign_hids1.serialize(),
            },
            exp_flags={},
            handler='recommends',
        )
        self.assertFragmentIn(
            response,
            {
                'result': {
                    '1718': {"maxBid": 71, "minBid": T.default_rp},  # 70*1.0 / 1.0 = 71 (70 + 1)
                }
            },
        )

    def test_main_hid_vs_foreign_hid(self):
        """
        Тест 11.
        Рассматриваем случай, когда для врезки в SaaS категория является основной, а для врезки-кандидата - доп.охватом.
        Врезка-кандидат не будет конкурентом для врезки из SaaS.
        В ответе - мин = РП, макс = ставка врезки из SaaS + 1
        """
        response = self.request(
            {
                'target_hids': '1720',
                'foreign_hids': '1718',
                'saas_incut': self.incut_with_foreign_hids1.serialize(),
            },
            exp_flags={},
            handler='recommends',
        )
        self.assertFragmentIn(
            response,
            {
                'result': {
                    '1718': {"maxBid": 71, "minBid": T.default_rp},  # 70*1.0 / 1.0 = 71 (70 + 1)
                }
            },
        )

    def test_foreign_hid_vs_main_hid(self):
        """
        Тест 12.
        Рассматриваем случай, когда для врезки из SaaS категория является доп. охватом, а для врезки-кандидата основной.
        Врезка из SaaS не будет считаться конкурентом.
        В ответе - мин и макс = РП
        """
        response = self.request(
            {
                'target_hids': '1719',
                'saas_incut': self.incut_with_foreign_hids1.serialize(),
            },
            exp_flags={'market_madv_use_foreign_incuts_for_main_hids': 0},
            handler='recommends',
        )
        self.assertFragmentIn(
            response,
            {
                'result': {
                    '1719': {"maxBid": T.default_rp, "minBid": T.default_rp},
                }
            },
        )

    def test_foreign_hid_vs_foreign_hid(self):
        """
        Тест 13.
        Рассматриваем случай, когда для обеих врезок категория - доп. охват.
        Так как нет врезки-конкурента, то в ответе - мин и макс = РП
        """
        response = self.request(
            {
                'target_hids': '1720',
                'foreign_hids': '1719',
                'saas_incut': self.incut_with_foreign_hids1.serialize(),
            },
            exp_flags={},
            handler='recommends',
        )
        self.assertFragmentIn(
            response,
            {
                'result': {
                    '1719': {"maxBid": 71, "minBid": T.default_rp},
                }
            },
        )

    def test_foreign_hid_vs_main_hid_with_flag(self):
        """
        Тест 14.
        Рассматриваем случай, когда для врезки из SaaS категория является доп. охватом, а для врезки-кандидата основной.
        Но при этом включаем флаг - hid
        Врезка из SaaS не будет считаться конкурентом.
        В ответе - мин и макс = РП
        """
        response = self.request(
            {
                'target_hids': '1719',
                'saas_incut': self.incut_with_foreign_hids1.serialize(),
            },
            exp_flags={'market_madv_use_foreign_incuts_for_main_hids': 1},
            handler='recommends',
        )
        self.assertFragmentIn(
            response,
            {
                'result': {
                    '1719': {"maxBid": 71, "minBid": T.default_rp},
                }
            },
        )

    @classmethod
    def prepare_empty_incut(cls):
        """
         Подготовка врезок для проверки парсинга врезки без моделей
        """
        start_hid = 445
        start_vendor_id = 2024
        start_datasource_id = 2024
        start_model_id = 2024
        base_bid = 70
        cls.content.incuts += [
            IncutModelsList(
                hid=start_hid,
                vendor_id=start_vendor_id,
                datasource_id=start_datasource_id,
                bid=base_bid,
                models=[ModelWithBid(model_id=start_model_id + z) for z in range(1, 4)],
            )
        ]
        cls.empty_models_incut = IncutModelsList(
            hid=start_hid,
            vendor_id=start_vendor_id + 2,
            datasource_id=start_datasource_id + 2,
            bid=base_bid - 10,
            foreign_hid=start_hid + 1,  # врезка - кандидат без моделей
        )

    def test_empty_models_incut(self):
        """
        Тест 15. В запрос передаем врезку без моделей
        В ответе - мин = РП, макс = ставка врезки из SaaS + 1
        """
        response = self.request(
            {
                'target_hids': '445',
                'saas_incut': self.empty_models_incut.serialize(),
            },
            exp_flags={},
            handler='recommends',
        )
        self.assertFragmentIn(
            response,
            {
                'result': {
                    '445': {"maxBid": 71, "minBid": T.default_rp},
                }
            },
        )


if __name__ == '__main__':
    env.main()
