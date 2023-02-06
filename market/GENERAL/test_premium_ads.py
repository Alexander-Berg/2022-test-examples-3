#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BlueOffer,
    ClickType,
    Currency,
    GradeDispersionItem,
    HyperCategory,
    HyperCategoryType,
    MarketSku,
    MnPlace,
    Model,
    Offer,
    ReviewDataItem,
    Shop,
    Tax,
    Vat,
)
from core.testcase import TestCase, main
from core.matcher import Contains, NoKey, NotEmpty


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.hypertree += [HyperCategory(hid=100, output_type=HyperCategoryType.GURU)]

        cls.index.models += [
            Model(hid=100, hyperid=1, title="Первая модель", ts=1),
            Model(hid=100, hyperid=2, title="Вторая модель", ts=2),
            Model(hid=100, hyperid=3, title="Третья модель", ts=3),
            Model(hid=100, hyperid=4, title="Четвертая модель", ts=4),
            Model(hid=100, hyperid=5, title="Пятая модель", ts=5),
            Model(hid=100, hyperid=6, title="Шестая модель", ts=6),
        ]

        cls.index.shops += [
            Shop(fesh=40, domain="4you.com"),
            Shop(
                fesh=100,
                datafeed_id=1,
                priority_region=213,
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                currency=Currency.RUR,
                tax_system=Tax.OSN,
            ),
            Shop(
                fesh=200,
                datafeed_id=200,
                priority_region=213,
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                blue=Shop.BLUE_REAL,
            ),
            Shop(
                fesh=300,
                datafeed_id=300,
                priority_region=213,
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
            ),
            Shop(
                fesh=400,
                datafeed_id=400,
                priority_region=213,
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
            ),
        ]

        cls.index.offers += [
            Offer(hyperid=4, fesh=40, ts=41, title="CPC оффер от четвертой модели", cbid=500),
        ]
        cls.index.mskus += [
            MarketSku(
                hyperid=1,
                sku=1,
                blue_offers=[
                    BlueOffer(price=500, vat=Vat.NO_VAT, feedid=200, ts=11, title="ДО CPA оффер от первой модели"),
                    BlueOffer(
                        price=1000,
                        vat=Vat.NO_VAT,
                        feedid=300,
                        ts=12,
                        title="Премиальный CPA оффер от первой модели",
                        fee=5000,
                    ),
                    BlueOffer(
                        price=1000,
                        vat=Vat.NO_VAT,
                        feedid=400,
                        ts=13,
                        title="Не премиальный CPA оффер от первой модели",
                        fee=4000,
                    ),
                ],
            ),
            MarketSku(
                hyperid=2,
                sku=2,
                blue_offers=[
                    BlueOffer(
                        price=1000, vat=Vat.NO_VAT, feedid=200, ts=21, title="ДО CPA оффер от второй модели", fee=1000
                    ),
                ],
            ),
            MarketSku(
                hyperid=3,
                sku=3,
                blue_offers=[
                    BlueOffer(price=1000, vat=Vat.NO_VAT, feedid=200, ts=31, title="ДО CPA оффер от третьей модели"),
                ],
            ),
            MarketSku(
                hyperid=5,
                sku=5,
                blue_offers=[
                    BlueOffer(
                        price=1000, vat=Vat.NO_VAT, feedid=200, ts=51, title="ДО CPA оффер от пятой модели"
                    ),  # очень плохой и дешевый оффер, но один
                ],
            ),
            MarketSku(
                hyperid=6,
                sku=6,
                blue_offers=[
                    BlueOffer(price=1000, vat=Vat.NO_VAT, feedid=200, ts=61, title="ДО CPA оффер от шестой модели"),
                ],
            ),
        ]

        for place in [MnPlace.BASE_SEARCH, MnPlace.META_REARRANGE]:
            cls.matrixnet.on_place(place, 1).respond(0.15)
            cls.matrixnet.on_place(place, 11).respond(0.151)
            cls.matrixnet.on_place(place, 12).respond(0.15)
            cls.matrixnet.on_place(place, 13).respond(0.15)
            cls.matrixnet.on_place(place, 2).respond(0.3)
            cls.matrixnet.on_place(place, 21).respond(0.3)
            cls.matrixnet.on_place(place, 3).respond(0.23)
            cls.matrixnet.on_place(place, 31).respond(0.23)
            cls.matrixnet.on_place(place, 4).respond(0.4)
            cls.matrixnet.on_place(place, 41).respond(0.4)
            cls.matrixnet.on_place(place, 5).respond(0.001)
            cls.matrixnet.on_place(place, 51).respond(0.001)  # очень плохой и дешевый оффер, но один
            cls.matrixnet.on_place(place, 6).respond(0.06)
            cls.matrixnet.on_place(place, 61).respond(0.06)

        cls.index.model_reviews_data += [
            ReviewDataItem(
                review_id=101,
                model_id=1,
                short_text="Nice",
                most_useful=1,
            )
        ]
        cls.index.model_grade_dispersion_data += [
            GradeDispersionItem(model_id=1, five=1),
        ]

    def test_premium_ads_format(self):
        """Проверяем содержимое рекламного блока
        выдача содержит стандартные офферы
        """

        response = self.report.request_json(
            'place=prime&text=оффер&use-default-offers=1&debug=da&allow-collapsing=1'
            '&premium-ads=1&rearr-factors=market_ads_gallery=premium'
            '&rearr-factors=use_offer_type_priority_as_main_factor_in_top=0;market_premium_ads_gallery_full_offers_info=1;market_premium_ads_gallery_shop_fee_threshold=0'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            {
                "premiumAds": {
                    "title": {"url": "", "title": ""},
                    "items": [
                        {
                            "entity": "offer",
                            "titles": {"raw": "Премиальный CPA оффер от первой модели"},
                            "urls": {
                                "direct": NotEmpty(),
                                "encrypted": Contains("/redir/", "/dtype=market/", "/pp=230/"),
                                "offercard": Contains("/redir/", "/dtype=offercard/", "/pp=230/"),
                                "cpa": Contains("/safeclick/", "dtype=cpa/", "/pp=230/"),
                                "U_DIRECT_OFFER_CARD_URL": Contains("market.yandex.ru/offer/"),
                            },
                            "prices": {"value": "1000", "currency": "RUR"},
                            "isPremium": True,
                        },
                    ],
                }
            },
            allow_different_len=True,
        )

    def test_premium_offers_in_order_models_on_page(self):
        """Проверяем что в рекламную галерею попадают топ-1 офферы безо всякого порога
        Премиальные офферы располагаются в порядке в котором идут модели на выдаче
        """

        response = self.report.request_json(
            'place=prime&text=оффер&use-default-offers=1&debug=da&allow-collapsing=1'
            '&premium-ads=1&rearr-factors=market_ads_gallery=premium;market_premium_ads_gallery_cpa_top_auction=1;market_premium_ads_gallery_shop_fee_threshold=0'
            '&rearr-factors=market_metadoc_search=no'
        )

        # по слову 'оффер' ищутся только офферы и схлапываются в модели
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {'titles': {'raw': 'Четвертая модель'}},  # 0.4
                        {'titles': {'raw': 'Вторая модель'}},  # 0.3
                        {'titles': {'raw': 'Третья модель'}},  # 0.23
                        {'titles': {'raw': 'Первая модель'}},  # 0.15
                        {'titles': {'raw': 'Шестая модель'}},  # 0.06
                        {'titles': {'raw': 'Пятая модель'}},  # 0.001
                    ]
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

        self.assertFragmentIn(response, "Request Premium for [1, 2, 3, 4, 5, 6]")
        self.assertFragmentIn(response, "Found 5 Premium offers")

        # нашлось по меньшей мере 5 офферов для врезки
        self.assertFragmentIn(
            response,
            {
                "premiumAds": {
                    "title": {"url": "", "title": ""},
                    "items": [
                        # оффер четвертой модели CPC и не нужен
                        {'titles': {'raw': "ДО CPA оффер от второй модели"}},
                        {'titles': {'raw': "ДО CPA оффер от третьей модели"}},
                        {'titles': {'raw': "Премиальный CPA оффер от первой модели"}},
                        {'titles': {'raw': "ДО CPA оффер от шестой модели"}},
                        {'titles': {'raw': "ДО CPA оффер от пятой модели"}},
                    ],
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

    def test_premium_offers_text_non_text(self):
        """market_ads_gallery=premium_text ограничивает показ галереи только на текстовые запросы
        market_ads_gallery=premium_textless ограничивает показ галереи только на бестекстовые запросы
        """

        gallery = {"search": NotEmpty(), "premiumAds": NotEmpty()}
        no_gallery = {"search": NotEmpty(), "premiumAds": NoKey("premiumAds")}

        text_query = (
            'place=prime&text=оффер&use-default-offers=1&debug=da&allow-collapsing=1'
            '&premium-ads=1&rearr-factors=market_premium_ads_gallery_shop_fee_threshold=0'
            '&rearr-factors=market_metadoc_search=no'
        )
        textless_query = (
            'place=prime&hid=100&use-default-offers=1&debug=da&allow-collapsing=1'
            '&premium-ads=1&rearr-factors=market_premium_ads_gallery_cpa_top_auction=1;market_premium_ads_gallery_shop_fee_threshold=0'
            '&rearr-factors=market_metadoc_search=no'
        )

        # market_ads_gallery=premium показывает галерею и на тексте и на бестексте
        response = self.report.request_json(text_query + '&rearr-factors=market_ads_gallery=premium')
        self.assertFragmentIn(response, gallery, allow_different_len=False)
        response = self.report.request_json(textless_query + '&rearr-factors=market_ads_gallery=premium')
        self.assertFragmentIn(response, gallery, allow_different_len=False)

        # market_ads_gallery=premium_text показывает галерею только на текстовых запросах
        response = self.report.request_json(text_query + '&rearr-factors=market_ads_gallery=premium_text')
        self.assertFragmentIn(response, gallery, allow_different_len=False)
        response = self.report.request_json(textless_query + '&rearr-factors=market_ads_gallery=premium_text')
        self.assertFragmentIn(response, no_gallery, allow_different_len=False)

        # market_ads_gallery=premium_textless показывает галерею только на бестекстовых запросах
        response = self.report.request_json(text_query + '&rearr-factors=market_ads_gallery=premium_textless')
        self.assertFragmentIn(response, no_gallery, allow_different_len=False)
        response = self.report.request_json(textless_query + '&rearr-factors=market_ads_gallery=premium_textless')
        self.assertFragmentIn(response, gallery, allow_different_len=False)

        # по умолчанию галерея включена на текстовых запросах
        response = self.report.request_json(text_query)
        self.assertFragmentIn(response, gallery, allow_different_len=False)
        response = self.report.request_json(textless_query)
        self.assertFragmentIn(response, no_gallery, allow_different_len=False)

    def test_premium_offers_min_max(self):
        '''market_premium_ads_gallery_max_count ограничивает максимальное количество офферов для врезки (25 по умолчанию)
        market_premium_ads_gallery_min_count ограничивает показ врезки со слишком маленьким числом офферов (5 по умолчанию)
        market_premium_ads_gallery_top_models ограничивает топ документов, среди которых будут выбраны премиальные офферы
        market_premium_ads_gallery_top_models_text ограничивает топ документов, среди которых будут выбраны премиальные офферы, на текстовых запросах
        market_premium_ads_gallery_top_models_non_text ограничивает топ документов, среди которых будут выбраны премиальные офферы, на бестекстовых запросах
        '''

        # без флагов находится 5 офферов для врезки
        response = self.report.request_json(
            'place=prime&text=оффер&use-default-offers=1&debug=da&allow-collapsing=1'
            '&premium-ads=1&rearr-factors=market_ads_gallery=premium;market_premium_ads_gallery_cpa_top_auction=1;market_premium_ads_gallery_shop_fee_threshold=0'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            {
                "premiumAds": {
                    "title": {"url": "", "title": ""},
                    "items": [
                        # оффер четвертой модели CPC и не нужен
                        {'titles': {'raw': "ДО CPA оффер от второй модели"}},
                        {'titles': {'raw': "ДО CPA оффер от третьей модели"}},
                        {'titles': {'raw': "Премиальный CPA оффер от первой модели"}},
                        {'titles': {'raw': "ДО CPA оффер от шестой модели"}},
                        {'titles': {'raw': "ДО CPA оффер от пятой модели"}},
                    ],
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

        # market_premium_ads_gallery_max_count ограничивает максимальное количество офферов для врезки
        response = self.report.request_json(
            'place=prime&text=оффер&use-default-offers=1&debug=da&allow-collapsing=1'
            '&premium-ads=1&rearr-factors=market_ads_gallery=premium;market_premium_ads_gallery_cpa_top_auction=1;'
            'market_premium_ads_gallery_max_count=3;market_premium_ads_gallery_min_count=1;market_premium_ads_gallery_shop_fee_threshold=0'
            '&rearr-factors=market_metadoc_search=no'
        )
        # во врезке всего 3 оффера
        self.assertFragmentIn(
            response,
            {
                "premiumAds": {
                    "title": {"url": "", "title": ""},
                    "items": [
                        # оффер четвертой модели CPC и не нужен
                        {'titles': {'raw': "ДО CPA оффер от второй модели"}},
                        {'titles': {'raw': "ДО CPA оффер от третьей модели"}},
                        {'titles': {'raw': "Премиальный CPA оффер от первой модели"}},
                    ],
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

        # market_premium_ads_gallery_min_count ограничивает показ врезки со слишком маленьким числом офферов
        response = self.report.request_json(
            'place=prime&text=оффер&use-default-offers=1&debug=da&allow-collapsing=1'
            '&premium-ads=1&rearr-factors=market_ads_gallery=premium;market_premium_ads_gallery_cpa_top_auction=1;'
            'market_premium_ads_gallery_max_count=25;market_premium_ads_gallery_min_count=10;market_premium_ads_gallery_shop_fee_threshold=0'
            '&rearr-factors=market_metadoc_search=no'
        )

        # нашлось 5 офферов для врезки, поэтому врезка не показалась
        self.assertFragmentIn(response, {"search": NotEmpty(), "premiumAds": NoKey("premiumAds")})
        self.assertFragmentIn(response, "Request Premium for [1, 2, 3, 4, 5, 6]")
        self.assertFragmentIn(response, "Found 5 Premium offers")
        self.assertFragmentIn(response, "We need 10..25 premium offers for premium ads")
        self.assertFragmentIn(response, "No data for show premium ads")

        # market_premium_ads_gallery_top_models ограничивает топ документов, среди которых будут выбраны премиальные офферы
        response = self.report.request_json(
            'place=prime&text=оффер&use-default-offers=1&debug=da&allow-collapsing=1'
            '&premium-ads=1&rearr-factors=market_ads_gallery=premium;market_premium_ads_gallery_cpa_top_auction=1;'
            'market_premium_ads_gallery_min_count=1;market_premium_ads_gallery_top_models=3;market_premium_ads_gallery_shop_fee_threshold=0'
            '&rearr-factors=market_metadoc_search=no'
        )

        # во врезку попали только премиальные офферы от первых 3х моделей в выдаче
        self.assertFragmentIn(
            response,
            {
                "premiumAds": {
                    "title": {"url": "", "title": ""},
                    "items": [
                        # оффер четвертой модели CPC и не нужен
                        {'titles': {'raw': "ДО CPA оффер от второй модели"}},
                        {'titles': {'raw': "ДО CPA оффер от третьей модели"}},
                    ],
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

        # market_premium_ads_gallery_top_models_text ограничивает топ документов, среди которых будут выбраны премиальные офферы
        response = self.report.request_json(
            'place=prime&text=оффер&use-default-offers=1&debug=da&allow-collapsing=1'
            '&premium-ads=1&rearr-factors=market_ads_gallery=premium;market_premium_ads_gallery_cpa_top_auction=1;'
            'market_premium_ads_gallery_min_count=1;market_premium_ads_gallery_top_models_text=2;market_premium_ads_gallery_top_models_non_text=4;market_premium_ads_gallery_shop_fee_threshold=0'
            '&rearr-factors=market_metadoc_search=no'
        )

        # во врезку попали только премиальные офферы от первых 2х моделей в выдаче
        self.assertFragmentIn(
            response,
            {
                "premiumAds": {
                    "title": {"url": "", "title": ""},
                    "items": [
                        # оффер четвертой модели CPC и не нужен
                        {'titles': {'raw': "ДО CPA оффер от второй модели"}},
                    ],
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

        # market_premium_ads_gallery_top_models_non_text ограничивает топ документов, среди которых будут выбраны премиальные офферы
        response = self.report.request_json(
            'place=prime&hid=100&use-default-offers=1&debug=da&allow-collapsing=1'
            '&premium-ads=1&rearr-factors=market_ads_gallery=premium;market_premium_ads_gallery_cpa_top_auction=1;'
            'market_premium_ads_gallery_min_count=1;market_premium_ads_gallery_top_models_text=2;market_premium_ads_gallery_top_models_non_text=4;market_premium_ads_gallery_shop_fee_threshold=0'
            '&rearr-factors=market_metadoc_search=no'
        )

        # во врезку попали только премиальные офферы от первых 4х моделей в выдаче
        self.assertFragmentIn(
            response,
            {
                "premiumAds": {
                    "title": {"url": "", "title": ""},
                    "items": [
                        # оффер четвертой модели CPC и не нужен
                        {'titles': {'raw': "ДО CPA оффер от второй модели"}},
                        {'titles': {'raw': "ДО CPA оффер от третьей модели"}},
                        {'titles': {'raw': "Премиальный CPA оффер от первой модели"}},
                    ],
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

    def test_premium_offers_in_ads_gallery_autobroker(self):
        """Проверяем что списывается не минбид а ставка, такая чтобы поднять оффер выше порога"""

        response = self.report.request_json(
            'place=prime&text=оффер&use-default-offers=1&debug=da&allow-collapsing=1&premium-ads=1'
            '&rearr-factors=market_ads_gallery=premium;market_premium_ads_gallery_cpa_top_auction=1;market_premium_ads_gallery_shop_fee_threshold=0'
            '&rearr-factors=market_metadoc_search=no'
        )

        self.assertFragmentIn(
            response,
            {
                "premiumAds": {
                    "title": {"url": "", "title": ""},
                    "items": [
                        # списывается не минбид а ставка, такая чтобы поднять оффер выше порога
                        {
                            'titles': {'raw': "Премиальный CPA оффер от первой модели"},
                            "urls": {'cpa': Contains("/shop_fee_ab=4001/", "/shop_fee=5000/", "/pp=230/")},
                        }
                    ],
                }
            },
        )

        self.show_log_tskv.expect(
            pp=230,
            title="Премиальный CPA оффер от первой модели",
            shop_fee_ab=4001,
            shop_fee=5000,
            min_bid=1,
            is_premium_offer=1,
        )

    def test_uniq_premium_offer(self):
        """Флаг market_premium_ads_gallery_uniq=1 позволяет на первое место в выдаче
        поставить премиальный оффер от наиболее релевантной модели"""

        response = self.report.request_json(
            'place=prime&text=оффер&use-default-offers=1&debug=da&allow-collapsing=1'
            '&rearr-factors=market_ads_gallery=premium_uniq;market_premium_ads_gallery_cpa_top_auction=1;market_set_1p_fee_recommended=0'
            '&rearr-factors=market_metadoc_search=no'
        )

        # оффер от наиболее релевантной второй модели попадает в премиальное размещение
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            'entity': 'offer',
                            'titles': {'raw': 'ДО CPA оффер от второй модели'},
                            # 'urls': {'encrypted': Contains("/cp=22/", "/cb=500/", "/min_bid=1/", "/pp=230/")},
                            'isPremium': True,
                        },
                        {'titles': {'raw': 'Четвертая модель'}},
                        {'titles': {'raw': 'Вторая модель'}},
                        {'titles': {'raw': 'Третья модель'}},
                        {'titles': {'raw': 'Первая модель'}},
                        {'titles': {'raw': 'Шестая модель'}},
                        {'titles': {'raw': 'Пятая модель'}},
                    ]
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

    def test_uniq_premium_offer_on_view_grid(self):
        """На гридовой выдаче сохраняем запрошенное количество результатов на странице
        т.е. добавляемый премиальный оффер в выдачу вытесняет одни из документов
        """

        response = self.report.request_json(
            'place=prime&text=оффер&use-default-offers=1&debug=da&allow-collapsing=1&numdoc=6&viewtype=grid'
            '&rearr-factors=market_ads_gallery=premium_uniq;market_premium_ads_gallery_cpa_top_auction=1;market_set_1p_fee_recommended=0'
            '&rearr-factors=market_metadoc_search=no'
        )

        # оффер от наиболее релевантной второй модели попадает в премиальное размещение
        # т.к. запрошено 6 документов на странице то при гридовой выдаче будет отображено ровно 6 документов
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "view": "grid",
                    "results": [
                        {
                            'entity': 'offer',
                            'titles': {'raw': 'ДО CPA оффер от второй модели'},
                            # 'urls': {'encrypted': Contains("/cp=22/", "/cb=500/", "/min_bid=1/", "/pp=230/")},
                            'isPremium': True,
                        },
                        {'titles': {'raw': 'Четвертая модель'}},
                        {'titles': {'raw': 'Вторая модель'}},
                        {'titles': {'raw': 'Третья модель'}},
                        {'titles': {'raw': 'Первая модель'}},
                        {'titles': {'raw': 'Шестая модель'}},
                        # {'titles': {'raw': 'Пятая модель'}}, - исключена из выдачи, т.к. нарушает сетку гридовой выдачи
                    ],
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

        response = self.report.request_json(
            'place=prime&text=оффер&use-default-offers=1&debug=da&allow-collapsing=1&numdoc=6&viewtype=list'
            '&rearr-factors=market_ads_gallery=premium_uniq;market_premium_ads_gallery_cpa_top_auction=1;market_set_1p_fee_recommended=0'
            '&rearr-factors=market_metadoc_search=no'
        )

        # на листовой выдаче лишние документы не удаляются
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "view": "list",
                    "results": [
                        {
                            'entity': 'offer',
                            'titles': {'raw': 'ДО CPA оффер от второй модели'},
                            # 'urls': {'encrypted': Contains("/cp=22/", "/cb=500/", "/min_bid=1/", "/pp=230/")},
                            'isPremium': True,
                        },
                        {'titles': {'raw': 'Четвертая модель'}},
                        {'titles': {'raw': 'Вторая модель'}},
                        {'titles': {'raw': 'Третья модель'}},
                        {'titles': {'raw': 'Первая модель'}},
                        {'titles': {'raw': 'Шестая модель'}},
                        {'titles': {'raw': 'Пятая модель'}},
                    ],
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

    def test_uniq_premium_offer_text_non_text(self):
        """market_ads_gallery=premium_uniq выдает единственный премиальный оффер и на тексте и на бестексте
        market_ads_gallery=premium_uniq_text выдает единственный премиальный оффер только на тексте
        market_ads_gallery=premium_uniq_textless выдает единственный премиальный оффер только на бестексте
        """

        #  оффер от наиболее релевантной четвертой модели попадает в премиальное размещение
        premium_offer = {
            "search": {
                "results": [
                    {'entity': 'offer', 'titles': {'raw': 'ДО CPA оффер от второй модели'}, 'isPremium': True},
                    {'titles': {'raw': 'Четвертая модель'}},
                    {'titles': {'raw': 'Вторая модель'}},
                    {'titles': {'raw': 'Третья модель'}},
                ]
            }
        }

        no_premium_offer = {
            "search": {
                "results": [
                    {'titles': {'raw': 'Четвертая модель'}},
                    {'titles': {'raw': 'Вторая модель'}},
                    {'titles': {'raw': 'Третья модель'}},
                ]
            }
        }

        text_query = 'place=prime&text=оффер&use-default-offers=1&numdoc=3&debug=da&allow-collapsing=1&rearr-factors=market_premium_ads_gallery_cpa_top_auction=1;market_set_1p_fee_recommended=0;market_metadoc_search=no'  # noqa
        textless_query = 'place=prime&hid=100&use-default-offers=1&numdoc=3&debug=da&allow-collapsing=1&rearr-factors=market_premium_ads_gallery_cpa_top_auction=1;market_set_1p_fee_recommended=0;market_metadoc_search=no'  # noqa

        # market_ads_gallery=premium_uniq выдает единственный премиальный оффер и на тексте и на бестексте
        response = self.report.request_json(text_query + '&rearr-factors=market_ads_gallery=premium_uniq')
        self.assertFragmentIn(response, premium_offer, allow_different_len=False, preserve_order=True)
        response = self.report.request_json(textless_query + '&rearr-factors=market_ads_gallery=premium_uniq')
        self.assertFragmentIn(response, premium_offer, allow_different_len=False, preserve_order=True)

        # market_ads_gallery=premium_uniq_text выдает единственный премиальный оффер только на тексте
        response = self.report.request_json(text_query + '&rearr-factors=market_ads_gallery=premium_uniq_text')
        self.assertFragmentIn(response, premium_offer, allow_different_len=False, preserve_order=True)
        response = self.report.request_json(textless_query + '&rearr-factors=market_ads_gallery=premium_uniq_text')
        self.assertFragmentIn(response, no_premium_offer, allow_different_len=False, preserve_order=True)

        # market_ads_gallery=premium_uniq_textless выдает единственный премиальный оффер только на бестексте
        response = self.report.request_json(text_query + '&rearr-factors=market_ads_gallery=premium_uniq_textless')
        self.assertFragmentIn(response, no_premium_offer, allow_different_len=False, preserve_order=True)
        response = self.report.request_json(textless_query + '&rearr-factors=market_ads_gallery=premium_uniq_textless')
        self.assertFragmentIn(response, premium_offer, allow_different_len=False, preserve_order=True)

    def test_uniq_premium_offer_show_reviews(self):
        """Проверяем, что для отзывов премиального офера нет на выдаче.
        https://st.yandex-team.ru/MARKETOUT-32893
        """

        query = (
            'place=prime&text=оффер&use-default-offers=1&allow-collapsing=1&premium-ads=1'
            '&rearr-factors=market_ads_gallery=premium,premium_uniq;market_premium_ads_gallery_cpa_top_auction=1;'
            'market_premium_ads_gallery_min_count=1;market_premium_ads_gallery_top_models_text=5'
            '&rearr-factors=market_metadoc_search=no'
        )

        response = self.report.request_json(query)
        self.assertFragmentIn(response, {"search": {"results": [{"entity": "offer"}]}}, allow_different_len=True)

        response = self.report.request_json(query + '&show-reviews=1')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "entity": "product",
                            "titles": {"raw": "Первая модель"},
                        },
                    ]
                }
            },
            allow_different_len=False,
        )

    def test_cpa_preferation_in_ads_galery(self):
        """Проверяем что во рекламную галерею попадают только CPA оффера с соответствующими флагами
        CPA оффера располагаются в порядке в котором идут модели на выдаче
        """

        response = self.report.request_json(
            'place=prime&text=оффер&use-default-offers=1&debug=da&allow-collapsing=1'
            '&premium-ads=1'
            '&rearr-factors=market_ads_gallery=premium;'
            'market_premium_ads_gallery_cpa_do=1'
            '&rearr-factors=market_metadoc_search=no'
        )

        self.assertFragmentIn(response, "Request CPA for [1, 2, 3, 4, 5, 6]")
        self.assertFragmentIn(response, "Found 5 CPA offers")

        self.assertFragmentIn(
            response,
            {
                "premiumAds": {
                    "title": {"url": "", "title": ""},
                    "items": [
                        {'titles': {'raw': "ДО CPA оффер от второй модели"}},
                        {'titles': {'raw': "ДО CPA оффер от третьей модели"}},
                        {'titles': {'raw': "ДО CPA оффер от первой модели"}},
                        {'titles': {'raw': "ДО CPA оффер от шестой модели"}},
                        {'titles': {'raw': "ДО CPA оффер от пятой модели"}},
                    ],
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

    def test_fee_threshold(self):
        """
        Проверяем работу флага market_premium_ads_gallery_shop_fee_threshold
        Три кейса:
            1. Если флаг установлен, но меньше ставок, то в премиум эсп попадают только оффера со ставками
            2. Если отфильтровывается оффер со ставкой меньше порога
            3. если market_premium_ads_gallery_shop_fee_threshold == 0 флаг не влияет на ответ
        """
        response = self.report.request_json(
            'place=prime&text=оффер&use-default-offers=1&allow-collapsing=1&premium-ads=1&'
            'rearr-factors=market_ads_gallery=premium;market_premium_ads_gallery_cpa_top_auction=1;market_premium_ads_gallery_shop_fee_threshold=1;market_premium_ads_gallery_min_count=1'
            ';market_set_1p_fee_recommended=0'
            '&rearr-factors=market_metadoc_search=no'
        )

        self.assertFragmentIn(
            response,
            {
                "premiumAds": {
                    "title": {"url": "", "title": ""},
                    "items": [
                        {'titles': {'raw': "ДО CPA оффер от второй модели"}},
                        {'titles': {'raw': "Премиальный CPA оффер от первой модели"}},
                    ],
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

        response = self.report.request_json(
            'place=prime&text=оффер&use-default-offers=1&allow-collapsing=1&premium-ads=1&'
            'rearr-factors=market_ads_gallery=premium;market_premium_ads_gallery_cpa_top_auction=1;market_premium_ads_gallery_shop_fee_threshold=1001;market_premium_ads_gallery_min_count=1'
            ';market_set_1p_fee_recommended=0'
            '&rearr-factors=market_metadoc_search=no'
        )

        self.assertFragmentIn(
            response,
            {
                "premiumAds": {
                    "title": {"url": "", "title": ""},
                    "items": [
                        {'titles': {'raw': "Премиальный CPA оффер от первой модели"}},
                    ],
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

        response = self.report.request_json(
            'place=prime&text=оффер&use-default-offers=1&allow-collapsing=1&premium-ads=1&'
            'rearr-factors=market_ads_gallery=premium;market_premium_ads_gallery_cpa_top_auction=1;market_premium_ads_gallery_shop_fee_threshold=0;market_premium_ads_gallery_min_count=1'
            ';market_set_1p_fee_recommended=0'
            '&rearr-factors=market_metadoc_search=no'
        )

        self.assertFragmentIn(
            response,
            {
                "premiumAds": {
                    "title": {"url": "", "title": ""},
                    "items": [
                        {'titles': {'raw': "ДО CPA оффер от второй модели"}},
                        {'titles': {'raw': "ДО CPA оффер от третьей модели"}},
                        {'titles': {'raw': "Премиальный CPA оффер от первой модели"}},
                        {'titles': {'raw': "ДО CPA оффер от шестой модели"}},
                        {'titles': {'raw': "ДО CPA оффер от пятой модели"}},
                    ],
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

    @classmethod
    def prepare_autobroker_for_a_single_offer(cls):
        cls.index.mskus += [
            MarketSku(
                hyperid=7,
                sku=7,
                blue_offers=[
                    BlueOffer(price=1234, vat=Vat.NO_VAT, feedid=600, ts=82, title="каяк wavesport disel", fee=5000),
                ],
            ),
            MarketSku(
                hyperid=9,
                sku=9,
                blue_offers=[
                    BlueOffer(price=500, vat=Vat.NO_VAT, feedid=500, ts=71, title="каяк waka tuna"),
                ],
            ),
        ]

    def test_autobroker_for_a_single_offer(self):
        '''
        Проверяем, что если ставка одна, то спишется мин фин равное market_premium_ads_gallery_shop_fee_threshold в дефолтном состоянии == 1
        '''
        response = self.report.request_json(
            'place=prime&text=каяк&use-default-offers=1&allow-collapsing=1&premium-ads=1&'
            'rearr-factors=market_ads_gallery=premium;market_premium_ads_gallery_cpa_top_auction=1;market_premium_ads_gallery_min_count=1'
            '&rearr-factors=market_metadoc_search=no'
        )

        self.assertFragmentIn(
            response,
            {
                "premiumAds": {
                    "title": {"url": "", "title": ""},
                    "items": [
                        {'titles': {'raw': "каяк wavesport disel"}},
                    ],
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

        self.click_log.expect(clicktype=ClickType.CPA, shop_fee=5000, shop_fee_ab=1, price=1234, hyper_id=7)

    def test_autobroker_for_a_single_offer_with_threshold(self):
        '''
        Проверяем, что если ставка одна, то спишется мин фин явно заданное market_premium_ads_gallery_shop_fee_threshold == 42
        '''
        response = self.report.request_json(
            'place=prime&text=каяк&use-default-offers=1&allow-collapsing=1&premium-ads=1&'
            'rearr-factors=market_ads_gallery=premium;market_premium_ads_gallery_cpa_top_auction=1;market_premium_ads_gallery_shop_fee_threshold=42;market_premium_ads_gallery_min_count=1'
            '&rearr-factors=market_metadoc_search=no'
        )

        self.assertFragmentIn(
            response,
            {
                "premiumAds": {
                    "title": {"url": "", "title": ""},
                    "items": [
                        {'titles': {'raw': "каяк wavesport disel"}},
                    ],
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

        self.click_log.expect(clicktype=ClickType.CPA, shop_fee=5000, shop_fee_ab=42, price=1234, hyper_id=7)

    @classmethod
    def prepare_min_fee(cls):
        cls.index.mskus += [
            MarketSku(
                hyperid=10,
                sku=10,
                blue_offers=[
                    BlueOffer(
                        price=101010,
                        vat=Vat.NO_VAT,
                        feedid=1000,
                        ts=102,
                        title="красная лодка Dragorossi Critical Mass",
                        fee=400,
                    ),
                    BlueOffer(
                        price=1000,
                        vat=Vat.NO_VAT,
                        feedid=1100,
                        ts=112,
                        title="зеленая лодка Dragorossi Critical Mass",
                        fee=100,
                    ),
                    BlueOffer(
                        price=900, vat=Vat.NO_VAT, feedid=1200, ts=122, title="желтая лодка Dragorossi Critical Mass"
                    ),
                ],
            ),
        ]

        for place in [MnPlace.BASE_SEARCH, MnPlace.META_REARRANGE]:
            cls.matrixnet.on_place(place, 102).respond(0.4)
            cls.matrixnet.on_place(place, 112).respond(0.2)
            cls.matrixnet.on_place(place, 122).respond(0.95)

    def test_min_fee(self):
        '''
        Проверяем, что если после аукциона стоимость клика (в fee) меньше мин фии заданной в market_premium_ads_gallery_shop_fee_threshold, то спишется мин фии
        ( 0.2 * 100 ) . (0.4 * 400) = 0.125 - множитель автоброкера; 400 * 0.125 = 50 - амнистированная ставка на красную одку, но спишется 100, так как это мин фии
        '''
        response = self.report.request_json(
            'place=prime&text=лодка&use-default-offers=1&allow-collapsing=1&premium-ads=1&'
            'rearr-factors=market_ads_gallery=premium;market_premium_ads_gallery_cpa_top_auction=1;market_premium_ads_gallery_shop_fee_threshold=100;market_premium_ads_gallery_min_count=1'
            '&rearr-factors=market_metadoc_search=no'
        )

        self.assertFragmentIn(
            response,
            {
                "premiumAds": {
                    "title": {"url": "", "title": ""},
                    "items": [
                        {'titles': {'raw': "красная лодка Dragorossi Critical Mass"}},
                    ],
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

        self.click_log.expect(clicktype=ClickType.CPA, shop_fee=400, shop_fee_ab=100, price=101010, hyper_id=10)


if __name__ == '__main__':
    main()
