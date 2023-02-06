#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.matcher import Wildcard
from core.types import ClickType, HyperCategory, HyperCategoryType, MnPlace, Model, ModelGroup, Offer, Shop
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ["market_money_disable_bids=0"]
        cls.settings.default_search_experiment_flags += ['market_new_cpm_iterator=0']
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.models += [
            Model(ts=1, title='kijanka model 1', hyperid=501),
            Model(ts=2, title='kijanka model 2', hyperid=502),
            Model(ts=3, title='molotok model 3', hyperid=503),
            Model(ts=4, title='molotok model 4', hyperid=504),
            Model(title='shtyr bolshoy model 1', hyperid=505, hid=1),
        ]

        cls.index.shops += [
            Shop(fesh=1, priority_region=213, regions=[225]),
            Shop(fesh=2, priority_region=213, regions=[225]),
            Shop(fesh=3, priority_region=213, regions=[225]),
            Shop(fesh=4, priority_region=213, regions=[225]),
        ]

        cls.index.offers += [
            Offer(fesh=1, ts=5, title='kijanka offer 5', hyperid=501),
            Offer(fesh=2, ts=6, title='kijanka offer 6', hyperid=502),
            Offer(fesh=3, ts=7, title='molotok offer 7', hyperid=503),
            Offer(fesh=4, ts=8, title='molotok offer 8', hyperid=504),
            Offer(title='shtyr bolshoy dolblyonyi offer 1'),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1).respond(0.5)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2).respond(0.4)

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 5).respond(0.3)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 6).respond(0.2)

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3).respond(0.2)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 4).respond(0.3)

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 7).respond(0.4)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 8).respond(0.5)

    def test_market_remove_top_model_boost_top_offer_prime(self):
        response = self.report.request_json('place=prime&text=kijanka&rids=213')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "product", "titles": {"raw": "kijanka model 1"}},
                    {"entity": "product", "titles": {"raw": "kijanka model 2"}},
                    {"entity": "offer", "titles": {"raw": "kijanka offer 5"}},
                    {"entity": "offer", "titles": {"raw": "kijanka offer 6"}},
                ]
            },
            preserve_order=True,
        )

        response = self.report.request_json(
            'place=prime&text=kijanka&rids=213&rearr-factors=market_remove_top_model_boost_top_offer=1'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "offer", "titles": {"raw": "kijanka offer 5"}},
                    {"entity": "product", "titles": {"raw": "kijanka model 2"}},
                    {"entity": "offer", "titles": {"raw": "kijanka offer 6"}},
                    {"entity": "product", "titles": {"raw": "kijanka model 1"}},
                ]
            },
            preserve_order=True,
        )

    def test_market_remove_top_offer_boost_top_model_prime(self):
        response = self.report.request_json('place=prime&text=molotok&rids=213')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "offer", "titles": {"raw": "molotok offer 8"}},
                    {"entity": "offer", "titles": {"raw": "molotok offer 7"}},
                    {"entity": "product", "titles": {"raw": "molotok model 4"}},
                    {"entity": "product", "titles": {"raw": "molotok model 3"}},
                ]
            },
            preserve_order=True,
        )

        response = self.report.request_json(
            'place=prime&text=molotok&rids=213&rearr-factors=market_remove_top_offer_boost_top_model=1'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "product", "titles": {"raw": "molotok model 4"}},
                    {"entity": "offer", "titles": {"raw": "molotok offer 7"}},
                    {"entity": "product", "titles": {"raw": "molotok model 3"}},
                    {"entity": "offer", "titles": {"raw": "molotok offer 8"}},
                ]
            },
            preserve_order=True,
        )

    def test_market_reverse_top_prime(self):
        response = self.report.request_json('place=prime&text=molotok&rids=213&rearr-factors=market_reverse_top=1')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "product", "titles": {"raw": "molotok model 3"}},
                    {"entity": "product", "titles": {"raw": "molotok model 4"}},
                    {"entity": "offer", "titles": {"raw": "molotok offer 7"}},
                    {"entity": "offer", "titles": {"raw": "molotok offer 8"}},
                ]
            },
            preserve_order=True,
        )

    def test_market_fuzzy_rearr_factors_in_prime(self):
        '''
        Проверяем, что корректно работает связка с базовым поиском в контексте кворума
        на prime. Кворум стал безусловным для запросов с непустым &text после MARKETOUT-10917.

        1. Задаём запрос с текстом, чтобы удостовериться,
        что на базовый уходит "use_fuzzy_search: true" и пробрасываются
        кворумные параметры фильтрации и что модель
        по запросу находится
        2. Задаём точно такой же запрос, как предыдущий, только без текста.
        Проверяем, что ни один из флагов -- параметры фильтрации, проброс
        use_fuzzy_search -- не сработал. См. MARKETOUT-10534
        '''

        # 1.
        response = self.report.request_json('place=prime&text=shtyr+bolshoy+dolblyonyi&debug=da')

        self.assertFragmentIn(
            response,
            {
                "pron": [
                    "qspWordWidth:0.3,PrunCount:10000.0,FadeCount:500.0,LoWordLerp:0.1",
                ],
            },
        )
        self.assertFragmentIn(
            response,
            {
                "how": [
                    {
                        "args": Wildcard("*\nuse_fuzzy_search: true\n*"),
                    }
                ]
            },
        )
        self.assertFragmentIn(
            response, {"results": [{"entity": "product", "id": 505, "titles": {"raw": "shtyr bolshoy model 1"}}]}
        )

        # 2.
        response = self.report.request_json('place=prime&hid=1&debug=da')

        self.assertFragmentNotIn(
            response,
            {
                "pron": [
                    "qspWordWidth:0.3,PrunCount:10000.0,FadeCount:500.0,LoWordLerp:0.1",
                ],
            },
        )
        self.assertFragmentIn(
            response,
            {
                "how": [
                    {
                        "args": Wildcard("*\nuse_fuzzy_search: false\n*"),
                    }
                ]
            },
        )

    @classmethod
    def prepare_rearrangment_modifications_on_multicategory_output(cls):
        cls.index.hypertree += [
            HyperCategory(hid=60, name='Мобильные телефоны', output_type=HyperCategoryType.GURU, has_groups=False),
            HyperCategory(hid=61, name='Автомобильные шины', output_type=HyperCategoryType.GURU, has_groups=True),
            HyperCategory(hid=62, name='Планшеты', output_type=HyperCategoryType.GURU, has_groups=True),
        ]

        cls.index.shops += [
            Shop(fesh=2130, priority_region=213, regions=[225]),
            Shop(fesh=2131, priority_region=213, regions=[225]),
            Shop(fesh=2132, priority_region=213, regions=[225]),
        ]

        cls.index.model_groups += [
            ModelGroup(hid=61, hyperid=610, ts=610, title='Автомобильные шины Konig Nexus'),
            ModelGroup(hid=62, hyperid=620, ts=620, title='Планшет Нексус'),
        ]

        cls.index.models += [
            Model(hid=60, hyperid=601, ts=601, title='Мобилка Nexus X5'),
            Model(hid=61, hyperid=611, ts=611, group_hyperid=610, title='Автомобильные шины Konig Nexus 17'),
            Model(hid=61, hyperid=612, ts=612, group_hyperid=610, title='Автомобильные шины Konig Nexus 21'),
            Model(hid=62, hyperid=621, ts=621, group_hyperid=620, title='Планшет Asus Nexus 16Gb'),
            Model(hid=62, hyperid=622, ts=622, group_hyperid=620, title='Планшет Asus Nexus 32Gb'),
        ]

        cls.index.offers += [
            Offer(hid=60, hyperid=601, fesh=2130),
            Offer(hid=61, hyperid=611, ts=6111, fesh=2131, title='Konig Nexus 17 (Moscow)'),
            Offer(hid=61, hyperid=612, ts=6121, fesh=2131, title='Konig Nexus 21 (Moscow)'),
            Offer(hid=62, hyperid=621, ts=6211, fesh=2132, title='Asus Nexus 16Gb (Moscow)'),
            Offer(hid=62, hyperid=622, ts=6221, fesh=2132, title='Asus Nexus 32Gb (Moscow)'),
        ]

        # шины
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 610).respond(0.3)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 611).respond(0.28)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 612).respond(0.35)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 6121).respond(0.5)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 6111).respond(0.49)

        # мобилки
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 601).respond(0.32)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 621).respond(0.4)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 622).respond(0.2)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 6211).respond(0.1)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 6221).respond(0.36)

    def test_rearrangment_modifications_on_multicategory_output(self):
        '''Групповые модели, а также модели не имеющие групповой модели в выдаче остаются в head
        Модифткации не проходят релевантность и вообще не попадают в переранжирование, см. MARKETOUT-19760
        '''

        response = self.report.request_json('place=prime&cvredirect=0&text=nexus&rids=213')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    # head
                    {"titles": {"raw": "Konig Nexus 21 (Moscow)"}},  # оффер из первого магазина mnValue=0.5
                    {"titles": {"raw": "Asus Nexus 32Gb (Moscow)"}},  # оффер из второго магазина mnValue=0.36
                    {"titles": {"raw": "Мобилка Nexus X5"}},  # обычная модель mnValue=0.32
                    {
                        "titles": {"raw": "Автомобильные шины Konig Nexus"}
                    },  # групповая модель mnValue=0.3 - в head хотя имеет более релевантные модификации
                    # tail
                    {
                        "titles": {"raw": "Konig Nexus 17 (Moscow)"}
                    },  # еще оффер из первого магазина попадает в tail mnValue=0.49
                    {
                        "titles": {"raw": "Asus Nexus 16Gb (Moscow)"}
                    },  # ещё оффер из второго магазина попадает в tail mnValue=0.1
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_click_prices_after_page_rearrangements(cls):
        cls.index.offers += [
            Offer(fesh=1, title='cheap autobroker', bid=10),
            Offer(fesh=2, title='expensive autobroker', bid=100),
        ]

    def test_click_prices_after_page_rearrangements(self):
        self.report.request_json(
            'place=prime&text=autobroker&rids=213&yandexuid=1&rearr-factors=market_random_page_rearrange_on_text=1'
        )
        self.click_log.expect(ClickType.EXTERNAL, shop_id=1, position=1, cp=1)
        self.click_log.expect(ClickType.EXTERNAL, shop_id=2, position=2, cp=11)


if __name__ == '__main__':
    main()
