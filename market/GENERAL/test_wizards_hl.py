#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import CardCategory, HyperCategory, HyperCategoryType, MnPlace, Model, Offer, Shop, Vendor
from core.testcase import TestCase, main
from core.matcher import NotEmpty


class T(TestCase):
    @classmethod
    def prepare_dict_hl(cls):
        cls.index.hypertree += [
            HyperCategory(hid=1, name='iphone category 1', output_type=HyperCategoryType.GURU),
        ]

        cls.index.vendors += [
            Vendor(vendor_id=1, name='iphone vendor 1'),
            Vendor(vendor_id=2, name='iphone vendor 2'),
            Vendor(vendor_id=3, name='iphone vendor 3'),
        ]

        cls.index.cards += [
            CardCategory(hid=1, vendor_ids=[1, 2, 3]),
        ]

        cls.index.models += [
            Model(ts=1, hyperid=1, hid=1, title='iphone model 1'),
            Model(ts=2, hyperid=2, hid=1, title='iphone model 2'),
        ]

        cls.index.shops += [
            Shop(fesh=1),
            Shop(fesh=2),
            Shop(fesh=3),
            Shop(fesh=4),
        ]

        cls.index.offers += [
            Offer(ts=3, hyperid=1, title='iphone offer 1', fesh=1, vendor_id=1),
            Offer(ts=4, hyperid=1, title='iphone offer 2', fesh=2),
            Offer(ts=5, hyperid=2, title='iphone offer 3', fesh=3),
            Offer(ts=6, hyperid=2, title='iphone offer 4', fesh=4),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1).respond(0.39)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2).respond(0.38)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3).respond(0.37)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 4).respond(0.36)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 5).respond(0.35)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 6).respond(0.34)

    def test_dict_hl(self):
        """Проверяем, что в полях '__hl' лежит словарь {'text': ...}.
        https://st.yandex-team.ru/MARKETOUT-24422
        https://st.yandex-team.ru/MARKETOUT-31753
        """
        # market_offers_wizard_top_shops_max_count - для top_shops
        # market_offers_wizard_top_vendors_max_count - для top_vendors
        response = self.report.request_bs_pb(
            'place=parallel&text=iphone&rearr-factors='
            'market_offers_wizard_top_shops_max_count=1;'
            'market_offers_wizard_top_vendors_max_count=1;'
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "title": "\7[Iphone\7]",
                    "snippetTitle": {"__hl": {"text": "Iphone", "raw": True}},
                    "greenUrl": [
                        {
                            "snippetText": "Яндекс.Маркет",
                            "text": "Яндекс.Маркет",
                        },
                        {
                            "snippetText": "Iphone",
                            "text": "Iphone",
                        },
                    ],
                    "text": [{"__hl": {"text": NotEmpty()}}],
                    "showcase": {
                        "items": [
                            {
                                "title": {"text": {"__hl": {"text": "iphone offer 1", "raw": True}}},
                                "greenUrl": {"text": "SHOP-1"},
                            },
                            {
                                "title": {"text": {"__hl": {"text": "iphone offer 2", "raw": True}}},
                                "greenUrl": {"text": "SHOP-2"},
                            },
                            {
                                "title": {"text": {"__hl": {"text": "iphone offer 3", "raw": True}}},
                                "greenUrl": {"text": "SHOP-3"},
                            },
                        ],
                        "top_shops_title": {"__hl": {"text": NotEmpty()}},
                        "top_vendors": {
                            "title": {"__hl": {"text": "Производители на Маркете", "raw": True}},
                            "green_title": {"__hl": {"text": "Яндекс.Маркет", "raw": True}},
                            "all_vendors_title": {"__hl": {"text": "Больше производителей", "raw": True}},
                            "items": [
                                {"title": {"text": {"__hl": {"text": "iphone vendor 1", "raw": True}}}},
                            ],
                        },
                    },
                }
            },
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "title": "\7[Iphone\7]",
                    "greenUrl": [
                        {
                            "snippetText": "Яндекс.Маркет",
                            "text": "Яндекс.Маркет",
                        },
                        {
                            "text": "Iphone",
                        },
                    ],
                    "text": [{"__hl": {"text": NotEmpty()}}],
                    "showcase": {
                        "items": [
                            {"title": {"text": {"__hl": {"text": "iphone model 1", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "iphone model 2", "raw": True}}}},
                        ]
                    },
                }
            },
        )
        self.assertFragmentIn(
            response,
            {
                "market_ext_category": {
                    "title": {"__hl": {"text": "Iphone category 1 на Маркете", "raw": True}},
                    "greenUrl": [
                        {
                            "snippetText": "Яндекс.Маркет",
                            "text": "Яндекс.Маркет",
                        },
                        {
                            "text": "Iphone category 1",
                        },
                    ],
                    "text": [{"__hl": {"text": NotEmpty()}}],
                    "showcase": {
                        "items": [
                            {"title": {"text": {"__hl": {"text": "iphone vendor 1", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "iphone vendor 2", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "iphone vendor 3", "raw": True}}}},
                        ]
                    },
                }
            },
        )

    def test_model_offers_hl(self):
        """Проверяем подсветку тайтлов оферов в модельном колдунщике.
        https://st.yandex-team.ru/MARKETOUT-24850
        """
        response = self.report.request_bs_pb('place=parallel&text=iphone')
        self.assertFragmentIn(
            response,
            {
                "market_model": {
                    "showcase": {
                        "items": [
                            {
                                "title": {"text": {"__hl": {"text": "iphone offer 1", "raw": True}}},
                            },
                            {
                                "title": {"text": {"__hl": {"text": "iphone offer 2", "raw": True}}},
                            },
                        ]
                    }
                }
            },
        )

    def test_green_url_no_highlight(self):
        """Проверяем, что текст в greenUrl не оборачивается в "__hl"
        https://st.yandex-team.ru/MARKETOUT-30202
        https://st.yandex-team.ru/MARKETOUT-31753
        """
        # Офферный колдунщик
        response = self.report.request_bs_pb('place=parallel&text=iphone')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "greenUrl": [
                        {
                            "text": "Яндекс.Маркет",
                        },
                        {
                            "text": "Iphone",
                        },
                    ],
                    "showcase": {
                        "items": [
                            {
                                "greenUrl": {"text": "SHOP-1"},
                            },
                            {
                                "greenUrl": {"text": "SHOP-2"},
                            },
                            {
                                "greenUrl": {"text": "SHOP-3"},
                            },
                        ]
                    },
                }
            },
        )

        # Неявная модель
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "greenUrl": [
                        {
                            "text": "Яндекс.Маркет",
                        },
                        {
                            "text": "Iphone",
                        },
                    ]
                }
            },
        )

        # Категорийный
        self.assertFragmentIn(
            response,
            {
                "market_ext_category": {
                    "greenUrl": [
                        {
                            "text": "Яндекс.Маркет",
                        },
                        {
                            "text": "Iphone category 1",
                        },
                    ]
                }
            },
        )


if __name__ == '__main__':
    main()
