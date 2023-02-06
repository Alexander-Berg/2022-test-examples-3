#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.testcase import TestCase, main
from core.types import HyperCategory, Model, Offer, OverallModel
from core.types import BlueOffer, MarketSku
from core.types.hypercategory import ADULT_CATEG_ID

CATEGORY_NORMAL1 = 1277190  # Категория, для всех возрастов (тест на 4 оффера)

CATEGORY_NORMAL2 = 1277091  # Категория для всех возрастов (тест на 16 офферов)
CATEGORY_ADULT = ADULT_CATEG_ID  # 6091783 "Интим-товары" реальная категорий (тест на 16 офферов)
MODEL_ID2 = 1277071  # для полного теста (тест на 16 офферов)


class _Mskus:
    MODEL_ID = 1277177  # для (тест на 4 оффера)

    OFFER_NORM_CAT_NORM = BlueOffer(adult=False, hid=CATEGORY_NORMAL1, title="Normal offer in normal category")
    OFFER_NORM_CAT_NORM2 = BlueOffer(
        adult=False, hid=CATEGORY_NORMAL1, title="Normal offer in normal category, msku ADULT"
    )
    OFFER_ADULT_CAT_NORM = BlueOffer(adult=True, hid=CATEGORY_NORMAL1, title="ADULT offer in normal category")
    OFFER_ADULT_CAT_NORM2 = BlueOffer(
        adult=True, hid=CATEGORY_NORMAL1, title="ADULT offer in normal category, msku ADULT"
    )

    SKU_NORM_OFFER_NORM = MarketSku(
        title="msku-normal category-normal with offer-normal catoffer-normal",
        adult=False,
        hid=CATEGORY_NORMAL1,
        sku=1277256,
        blue_offers=[OFFER_NORM_CAT_NORM],
        hyperid=MODEL_ID,
    )
    SKU_NORM_OFFER_ADULT = MarketSku(
        title="msku-normal category-normal with offer-adult catoffer-normal",
        adult=False,
        hid=CATEGORY_NORMAL1,
        sku=1277257,
        blue_offers=[OFFER_ADULT_CAT_NORM],
        hyperid=MODEL_ID,
    )
    SKU_ADULT_OFFER_NORM = MarketSku(
        title="msku-ADULT category-normal with offer-normal catoffer-normal",
        adult=True,
        hid=CATEGORY_NORMAL1,
        sku=1277258,
        blue_offers=[OFFER_NORM_CAT_NORM2],
        hyperid=MODEL_ID,
    )
    SKU_ADULT_OFFER_ADULT = MarketSku(
        title="msku-ADULT category-normal with offer-adult catoffer-normal",
        adult=True,
        hid=CATEGORY_NORMAL1,
        sku=1277259,
        blue_offers=[OFFER_ADULT_CAT_NORM2],
        hyperid=MODEL_ID,
    )


class _Mskus2:
    def create_list_offer(adult, hid, title):
        list_offer = [
            BlueOffer(hyperid=MODEL_ID2, adult=adult, hid=hid, title=title + " N0", auto_creating_model=False),
            BlueOffer(hyperid=MODEL_ID2, adult=adult, hid=hid, title=title + " N1", auto_creating_model=False),
            BlueOffer(hyperid=MODEL_ID2, adult=adult, hid=hid, title=title + " N2", auto_creating_model=False),
            BlueOffer(hyperid=MODEL_ID2, adult=adult, hid=hid, title=title + " N3", auto_creating_model=False),
        ]
        return list_offer

    # offers
    offers_norml_cat_norml = create_list_offer(adult=False, hid=CATEGORY_NORMAL2, title="Norm offer in norm category")
    offers_norml_cat_ADULT = create_list_offer(adult=False, hid=CATEGORY_ADULT, title="Normal offer in ADULT category")
    offers_ADULT_cat_norml = create_list_offer(adult=True, hid=CATEGORY_NORMAL2, title="ADULT offer in norm category")
    offers_ADULT_cat_ADULT = create_list_offer(adult=True, hid=CATEGORY_ADULT, title="ADULT offer in ADULT category")

    # Mskus
    # with offer normal and category normal
    SKU_NORM_CAT_NORM_ONCN = MarketSku(
        title="msku-normal category-normal with offer-normal catoffer-normal",
        adult=False,
        hid=CATEGORY_NORMAL2,
        hyperid=MODEL_ID2,
        sku=1277051,
        blue_offers=[offers_norml_cat_norml[0]],
        auto_creating_model=False,
    )
    SKU_NORM_CAT_ADULT_ONCN = MarketSku(
        title="msku-normal category-ADULT with offer-normal catoffer-normal",
        adult=False,
        hid=CATEGORY_ADULT,
        hyperid=MODEL_ID2,
        sku=1277052,
        blue_offers=[offers_norml_cat_norml[1]],
        auto_creating_model=False,
    )
    SKU_ADULT_CAT_NORM_ONCN = MarketSku(
        title="msku-ADULT category-norm with offer-normal catoffer-normal",
        adult=True,
        hid=CATEGORY_NORMAL2,
        hyperid=MODEL_ID2,
        sku=1277053,
        blue_offers=[offers_norml_cat_norml[2]],
        auto_creating_model=False,
    )
    SKU_ADULT_CAT_ADULT_ONCN = MarketSku(
        title="msku-ADULT category-ADULT with offer-normal catoffer-normal",
        adult=True,
        hid=CATEGORY_ADULT,
        hyperid=MODEL_ID2,
        sku=1277054,
        blue_offers=[offers_norml_cat_norml[3]],
        auto_creating_model=False,
    )

    # with offer normal and category ADULT
    SKU_NORM_CAT_NORM_ONCA = MarketSku(
        title="msku-normal category-normal with offer-normal catoffer-ADULT",
        adult=False,
        hid=CATEGORY_NORMAL2,
        hyperid=MODEL_ID2,
        sku=1277055,
        blue_offers=[offers_norml_cat_ADULT[0]],
        auto_creating_model=False,
    )
    SKU_NORM_CAT_ADULT_ONCA = MarketSku(
        title="msku-normal category-ADULT with offer-normal catoffer-ADULT",
        adult=False,
        hid=CATEGORY_ADULT,
        hyperid=MODEL_ID2,
        sku=1277056,
        blue_offers=[offers_norml_cat_ADULT[1]],
        auto_creating_model=False,
    )
    SKU_ADULT_CAT_NORM_ONCA = MarketSku(
        title="msku-ADULT category-norm with offer-normal catoffer-ADULT",
        adult=True,
        hid=CATEGORY_NORMAL2,
        hyperid=MODEL_ID2,
        sku=1277057,
        blue_offers=[offers_norml_cat_ADULT[2]],
        auto_creating_model=False,
    )
    SKU_ADULT_CAT_ADULT_ONCA = MarketSku(
        title="msku-ADULT category-ADULT with offer-normal catoffer-ADULT",
        adult=True,
        hid=CATEGORY_ADULT,
        hyperid=MODEL_ID2,
        sku=1277058,
        blue_offers=[offers_norml_cat_ADULT[3]],
        auto_creating_model=False,
    )

    # with offer ADULT and category normal
    SKU_NORM_CAT_NORM_OACN = MarketSku(
        title="msku-normal category-normal with offer-ADULT catoffer-normal",
        adult=False,
        hid=CATEGORY_NORMAL2,
        hyperid=MODEL_ID2,
        sku=1277059,
        blue_offers=[offers_ADULT_cat_norml[0]],
        auto_creating_model=False,
    )
    SKU_NORM_CAT_ADULT_OACN = MarketSku(
        title="msku-normal category-ADULT with offer-ADULT catoffer-normal",
        adult=False,
        hid=CATEGORY_ADULT,
        hyperid=MODEL_ID2,
        sku=1277060,
        blue_offers=[offers_ADULT_cat_norml[1]],
        auto_creating_model=False,
    )
    SKU_ADULT_CAT_NORM_OACN = MarketSku(
        title="msku-ADULT category-norm with offer-ADULT catoffer-normal",
        adult=True,
        hid=CATEGORY_NORMAL2,
        hyperid=MODEL_ID2,
        sku=1277061,
        blue_offers=[offers_ADULT_cat_norml[2]],
        auto_creating_model=False,
    )
    SKU_ADULT_CAT_ADULT_OACN = MarketSku(
        title="msku-ADULT category-ADULT with offer-ADULT catoffer-normal",
        adult=True,
        hid=CATEGORY_ADULT,
        hyperid=MODEL_ID2,
        sku=1277062,
        blue_offers=[offers_ADULT_cat_norml[3]],
        auto_creating_model=False,
    )

    # with offer ADULT and category ADULT
    SKU_NORM_CAT_NORM_OACA = MarketSku(
        title="msku-normal category-normal with offer-ADULT catoffer-ADULT",
        adult=False,
        hid=CATEGORY_NORMAL2,
        hyperid=MODEL_ID2,
        sku=1277063,
        blue_offers=[offers_ADULT_cat_ADULT[0]],
        auto_creating_model=False,
    )
    SKU_NORM_CAT_ADULT_OACA = MarketSku(
        title="msku-normal category-ADULT with offer-ADULT catoffer-ADULT",
        adult=False,
        hid=CATEGORY_ADULT,
        hyperid=MODEL_ID2,
        sku=1277064,
        blue_offers=[offers_ADULT_cat_ADULT[1]],
        auto_creating_model=False,
    )
    SKU_ADULT_CAT_NORM_OACA = MarketSku(
        title="msku-ADULT category-norm with offer-ADULT catoffer-ADULT",
        adult=True,
        hid=CATEGORY_NORMAL2,
        hyperid=MODEL_ID2,
        sku=1277065,
        blue_offers=[offers_ADULT_cat_ADULT[2]],
        auto_creating_model=False,
    )
    SKU_ADULT_CAT_ADULT_OACA = MarketSku(
        title="msku-ADULT category-ADULT with offer-ADULT catoffer-ADULT",
        adult=True,
        hid=CATEGORY_ADULT,
        hyperid=MODEL_ID2,
        sku=1277066,
        blue_offers=[offers_ADULT_cat_ADULT[3]],
        auto_creating_model=False,
    )

    shown_skus = [SKU_NORM_CAT_NORM_ONCN, SKU_NORM_CAT_NORM_ONCA]
    hidden_skus = [
        SKU_ADULT_CAT_NORM_ONCN,
        SKU_ADULT_CAT_NORM_ONCA,
        SKU_NORM_CAT_NORM_OACN,
        SKU_ADULT_CAT_NORM_OACN,
        SKU_NORM_CAT_NORM_OACA,
        SKU_ADULT_CAT_NORM_OACA,
    ]

    shown_adult_skus = [
        SKU_NORM_CAT_ADULT_ONCA,
        SKU_ADULT_CAT_ADULT_ONCA,  # не совсем понятно почему показано (из общих соображений), но мой код это скрыть не может
    ]
    hidden_adult_skus = [
        SKU_NORM_CAT_ADULT_ONCN,  # не совсем понятно почему скрыто
        SKU_ADULT_CAT_ADULT_ONCN,
        SKU_NORM_CAT_ADULT_OACN,
        SKU_ADULT_CAT_ADULT_OACN,
        SKU_NORM_CAT_ADULT_OACA,
        SKU_ADULT_CAT_ADULT_OACA,
    ]


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.hypertree += [
            HyperCategory(hid=4175000, name="Any age"),
        ]

        cls.index.models += [
            Model(hid=4175000, hyperid=4175010, title="Adult model in any age category 1"),
            Model(hid=4175000, hyperid=4175020, title="Adult model in any age category 2"),
            Model(hid=4175000, hyperid=4175030, title="Any age model in any age category 1"),
            Model(hid=4175000, hyperid=4175040, title="Any age model in any age category 2"),
        ]

        cls.index.overall_models += [
            OverallModel(hyperid=4175010, price_med=None, is_adult=True),
            OverallModel(hyperid=4175020, price_med=None, is_adult=True),
        ]

        cls.index.offers += [
            Offer(hyperid=4175010, adult=True, title="Adult offer of adult model"),
            Offer(hyperid=4175020, adult=False, title="Any age offer of adult model"),
            Offer(hyperid=4175030, adult=True, title="Adult offer of any age model"),
            Offer(hyperid=4175040, adult=False, title="Any age offer of any age model"),
        ]

    def test_hide_adults_from_any_age_category_textless(self):
        request = "place=prime&hid=4175000&debug=da&rearr-factors=filter_adults_from_any_age_category=1"
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {"titles": {"raw": title}}
                        for title in [
                            "Any age offer of adult model",
                            "Any age offer of any age model",
                            "Any age model in any age category 1",
                            "Any age model in any age category 2",
                        ]
                    ]
                },
                'debug': {
                    'brief': {
                        'filters': {'ADULT_FROM_ANY_AGE_CATEGORY': 4},
                    }
                },
            },
        )
        adult_titles = [
            "Adult offer of adult model",
            "Adult offer of any age model",
            "Adult model in any age category 1",
            "Adult model in any age category 2",
        ]
        for title in adult_titles:
            self.assertFragmentNotIn(
                response,
                {"results": [{"titles": {"raw": title}}]},
            )

    @classmethod
    def prepare_offer_adult_category_norm(cls):
        cls.index.hypertree += [HyperCategory(hid=CATEGORY_NORMAL1, name="Any age")]

        cls.index.mskus += [
            _Mskus.SKU_NORM_OFFER_NORM,
            _Mskus.SKU_NORM_OFFER_ADULT,
            _Mskus.SKU_ADULT_OFFER_NORM,
            _Mskus.SKU_ADULT_OFFER_ADULT,
        ]

    def test_hide_adults_offer_from_normal_category_4_textless(self):
        '''
        Проверяем, что офферы/msku "случайно" помеченные adult, будут фильтроваться в обычных категориях, НЕТ признака "мне 18+"
        Не зажигать плашку "18+"
        Упрощенный вариант, данные консистенты, не разходятся категории (не ломаем модель)
                  O_N_cN  |  O_A_cN
        S_N_cN      -          F
        S_A_cN      F          F
        '''
        adult_titles = [
            _Mskus.SKU_NORM_OFFER_ADULT.title,
            _Mskus.SKU_ADULT_OFFER_NORM.title,
            _Mskus.SKU_ADULT_OFFER_ADULT.title,
        ]

        request = "place=prime&hid={}&debug=da&rearr-factors=filter_adults_from_any_age_category=1;market_metadoc_search=skus".format(
            CATEGORY_NORMAL1
        )
        request += "&hide_adult_offer_msku_in_any_age_category=1"

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {"results": [{"titles": {"raw": _Mskus.SKU_NORM_OFFER_NORM.title}}]},
                'debug': {
                    'brief': {
                        'filters': {'CHILDLESS_METADOC': len(adult_titles)},
                    }
                },
            },
        )

        for title in adult_titles:
            self.assertFragmentNotIn(
                response,
                {"results": [{"titles": {"raw": title}}]},
            )

    @classmethod
    def prepare_blue_offers(cls):
        cls.index.hypertree += [
            HyperCategory(hid=CATEGORY_NORMAL2, name="Any age"),
            HyperCategory(hid=CATEGORY_ADULT, name="Интим-товары/Adult category"),
        ]

        cls.index.models += [
            Model(
                hyperid=MODEL_ID2,
                hid=CATEGORY_NORMAL2,
                title="Model in normal category",
                is_allow_different_offer_hid=True,
            )
        ]

        cls.index.mskus += _Mskus2.shown_skus
        cls.index.mskus += _Mskus2.hidden_skus
        cls.index.mskus += _Mskus2.shown_adult_skus
        cls.index.mskus += _Mskus2.hidden_adult_skus

    def test_hide_adults_offer_from_normal_category_msku_textless(self):
        '''
                  O_N_cN  |  O_N_cA  |  O_A_cN  |  O_A_cA  |
        S_N_cN      -          -          F          F
        S_A_cN      F          F          F          F

        S_N_cA - SKU(S) нормальная (N), категория(c) adult (A)
        O_A_cN = Оффер (O) adult(A), категория(c) нормальная (N)
         "-" - не трогать
         "F" - применить фильтр
        это с учетом того что, у пользователя не подтвержден возраст

        Проверяем, что офферы/msku "случайно" помеченные adult, будут фильтроваться в обычных категориях, НЕТ признака "мне 18+"
        Не зажигать плашку "18+"
        '''
        request = "place=prime&hid={}&debug=da&rearr-factors=filter_adults_from_any_age_category=1;market_metadoc_search=skus".format(
            CATEGORY_NORMAL2
        )
        request += "&hide_adult_offer_msku_in_any_age_category=1"
        response = self.report.request_json(request)

        shown_skus = _Mskus2.shown_skus
        hidden_skus = _Mskus2.hidden_skus

        # мы все данные проверяем? (технические проверки)
        self.assertEqual(
            16, len(set(shown_skus) | set(hidden_skus) | set(_Mskus2.shown_adult_skus) | set(_Mskus2.hidden_adult_skus))
        )
        self.assertEqual(8, len(set(shown_skus) | set(hidden_skus)))
        # msku не может быть одновременно и показан, и скрыт (технические проверки) !!! Дописать
        self.assertEqual(
            0, len(set(shown_skus) & set(hidden_skus) & set(_Mskus2.shown_adult_skus) & set(_Mskus2.hidden_adult_skus))
        )

        adult_titles_msku = [isku.title for isku in hidden_skus]

        self.assertFragmentIn(
            response,
            {
                "search": {"adult": False, "results": [{"titles": {"raw": isku.title} for isku in shown_skus}]},
                'debug': {
                    'brief': {
                        'filters': {'CHILDLESS_METADOC': len(adult_titles_msku)},
                    }
                },
            },
        )

        for title in adult_titles_msku:
            self.assertFragmentNotIn(
                response,
                {"results": [{"titles": {"raw": title}}]},
            )

    def test_hide_adults_offer_from_adult_category_msku_textless(self):
        '''
                  O_N_cN  |  O_N_cA  |  O_A_cN  |  O_A_cA  |
        S_N_cA     ?(f)?       ?/-          F         ?(f)
        S_A_cA      F         ?/-?          F         ?(f)

        S_N_cA - SKU(S) нормальная (N), категория(c) adult (A)
        O_A_cN = Оффер (O) adult(A), категория(c) нормальная (N)
         "-" - sku показан
         "F" - применить наше скрытие
         "?" - наше скрытие здесь не работает
         "(f)" - скрыто другими правилами
        это с учетом того что, у пользователя не подтвержден возраст

        Проверяем, что офферы/msku помеченные adult, будут фильтроваться во взрослых категориях, НЕТ признака "мне 18+"
        Плашка "18+" загорится, так как категория adult
        '''
        request = "place=prime&hid={}&debug=da&rearr-factors=filter_adults_from_any_age_category=1;market_metadoc_search=skus".format(
            CATEGORY_ADULT
        )
        request += "&hide_adult_offer_msku_in_any_age_category=1"
        response = self.report.request_json(request)

        shown_adult_skus = _Mskus2.shown_adult_skus
        hidden_adult_skus = _Mskus2.hidden_adult_skus

        # мы все данные проверяем? (технические проверки)
        self.assertEqual(
            16, len(set(_Mskus2.shown_skus) | set(_Mskus2.hidden_skus) | set(shown_adult_skus) | set(hidden_adult_skus))
        )
        self.assertEqual(8, len(set(shown_adult_skus) | set(hidden_adult_skus)))
        # msku не может быть одновременно и показан, и скрыт (технические проверки)
        self.assertEqual(
            0, len(set(_Mskus2.shown_skus) & set(_Mskus2.hidden_skus) & set(shown_adult_skus) & set(hidden_adult_skus))
        )

        adult_titles_msku = [isku.title for isku in hidden_adult_skus]

        self.assertFragmentIn(
            response,
            {
                "search": {"adult": True, "results": [{"titles": {"raw": isku.title} for isku in shown_adult_skus}]},
                'debug': {
                    'brief': {
                        'filters': {'CHILDLESS_METADOC': 4, 'ADULT': 2},
                    }
                },
            },
        )

        for title in adult_titles_msku:
            self.assertFragmentNotIn(
                response,
                {"results": [{"titles": {"raw": title}}]},
            )


if __name__ == '__main__':
    main()
