#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    CardCategory,
    HyperCategory,
    HyperCategoryType,
    MnPlace,
    Model,
    NavCategory,
    Offer,
    Region,
    RegionalModel,
    Shop,
    Vendor,
)
from core.testcase import TestCase, main
from core.matcher import LikeUrl, NotEmpty
from core.types.vendor import PublishedVendor
from unittest import skip


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.hypertree += [HyperCategory(hid=1, name='goods', tovalid=1)]

        cls.index.navtree += [NavCategory(hid=1, nid=1)]

        # test_guru_category
        cls.index.vendors += [
            Vendor(vendor_id=10, name='bakery'),
            Vendor(vendor_id=11, name='dairy'),
            Vendor(vendor_id=12, name='butcher'),
        ]

        cls.index.hypertree += [HyperCategory(hid=10, name='food', tovalid=10, output_type=HyperCategoryType.GURU)]

        cls.index.navtree += [NavCategory(hid=10, nid=10)]

        cls.index.vendors += [
            Vendor(vendor_id=301, name='nestle'),
            Vendor(vendor_id=302, name='danone'),
            Vendor(vendor_id=303, name='coca-cola hbc'),
        ]

        cls.index.models += [
            Model(
                hyperid=201, hid=10, title='kit kat', vendor_id=301, picinfo='//mdata.yandex.net/i?path=kit_kat_img.jpg'
            ),
            Model(
                hyperid=202, hid=10, title='actimel', vendor_id=302, picinfo='//mdata.yandex.net/i?path=actimel_img.jpg'
            ),
            Model(
                hyperid=203, hid=10, title='sprite', vendor_id=303, picinfo='//mdata.yandex.net/i?path=sprite_img.jpg'
            ),
        ]

        cls.index.cards += [CardCategory(hid=10, vendor_ids=[10, 11, 12], model_count=456, hyperids=[201, 202, 203])]

        cls.index.regional_models += [
            RegionalModel(hyperid=201, price_min=100, price_max=200, offers=5),
            RegionalModel(hyperid=202, price_min=150, price_max=250, offers=6),
            RegionalModel(hyperid=203, price_min=200.5, price_max=300.5, offers=7),
        ]

        # test_offers
        cls.index.regiontree += [Region(rid=50, name='treasury')]

        cls.index.hypertree += [
            HyperCategory(hid=20, name='rings', tovalid=20, output_type=HyperCategoryType.GURU),
        ]

        cls.index.navtree += [NavCategory(hid=20, nid=21)]

        cls.index.shops += [Shop(fesh=50, regions=[50])]

        cls.index.offers += [
            Offer(title='silver ring', fesh=50, hid=20),
            Offer(title='platinum ring', fesh=50),
            Offer(title='golden ring', fesh=50),
        ]

        # test_implicit_model
        cls.index.hypertree += [
            HyperCategory(hid=60, name='devices', tovalid=500, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=80, name='other devices', tovalid=501, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=90, name='devices without pictures', tovalid=502, output_type=HyperCategoryType.GURU),
        ]

        cls.index.navtree += [NavCategory(hid=60, nid=65)]

        cls.index.models += [
            Model(hyperid=60, hid=60, title='corporation device 101'),
            Model(hyperid=61, hid=60, title='corporation device 102'),
            Model(hyperid=62, hid=60, title='corporation device 103'),
            Model(hyperid=63, hid=60, title='corporation device 104'),
            Model(hyperid=64, hid=80, title='corporation device 105'),
            Model(hyperid=65, hid=90, title='invisible corporation device', no_picture=True),
        ]

        cls.index.offers += [
            Offer(hyperid=60),
            Offer(hyperid=61),
        ]

        cls.index.cards += [
            CardCategory(hid=60, model_count=4, hyperids=[60, 61, 62, 63]),
            CardCategory(hid=80, model_count=1, hyperids=[64]),
            CardCategory(hid=90, model_count=1, hyperids=[65]),
        ]

        cls.index.published_vendors += [PublishedVendor(vendor_id=30)]

    def test_guru_category(self):
        response = self.report.request_bs('place=parallel&text=food')
        self.assertFragmentIn(
            response,
            {
                "market_ext_category": [
                    {
                        "url": LikeUrl.of('//market.yandex.ru/catalog--food/10?hid=10&clid=500'),
                        "urlTouch": LikeUrl.of('//m.market.yandex.ru/catalog?hid=10&nid=10&clid=707'),
                        "priority": NotEmpty(),
                        "type": "market_constr",
                        "subtype": "market_ext_category",
                        "counter": {"path": "/snippet/market/market_ext_category"},
                        "title": {"__hl": {"text": "Food на Маркете", "raw": True}},
                        "model_count": "456",
                        "category_name": "Food",
                        "greenUrl": [
                            {
                                "url": LikeUrl.of('//market.yandex.ru?clid=500'),
                                "urlTouch": LikeUrl.of('//m.market.yandex.ru?clid=707'),
                                "text": "Яндекс.Маркет",
                            },
                            {
                                "url": LikeUrl.of('//market.yandex.ru/catalog--food/10?hid=10&clid=500'),
                                "urlTouch": LikeUrl.of('//m.market.yandex.ru/catalog?hid=10&nid=10&clid=707'),
                                "text": "Food",
                            },
                        ],
                        "favicon": {"faviconDomain": "market.yandex.ru"},
                        "button": [
                            {
                                "url": LikeUrl.of('//market.yandex.ru/catalog--food/10?hid=10&clid=500'),
                                "urlTouch": LikeUrl.of('//m.market.yandex.ru/catalog?hid=10&nid=10&clid=707'),
                                "text": "Подбор по параметрам",
                            }
                        ],
                        "showcase": {
                            "items": [
                                {
                                    "thumb": {
                                        "text": "",
                                        "url": LikeUrl.of(
                                            '//market.yandex.ru/catalog--food-bakery/10/list?hid=10&clid=500&glfilter=7893318:10'
                                        ),
                                        "urlTouch": LikeUrl.of(
                                            '//m.market.yandex.ru/catalog--food-bakery/10/list?hid=10&clid=707&glfilter=7893318:10'
                                        ),
                                        "urlForCounter": "",
                                    },
                                    "title": {
                                        "url": LikeUrl.of(
                                            '//market.yandex.ru/catalog--food-bakery/10/list?hid=10&clid=500&glfilter=7893318:10'
                                        ),
                                        "urlTouch": LikeUrl.of(
                                            '//m.market.yandex.ru/catalog--food-bakery/10/list?hid=10&clid=707&glfilter=7893318:10'
                                        ),
                                        "text": {"__hl": {"text": "bakery", "raw": True}},
                                        "urlForCounter": "",
                                    },
                                    "label": {"text": "100 моделей"},
                                },
                                {
                                    "thumb": {
                                        "text": "",
                                        "url": LikeUrl.of(
                                            '//market.yandex.ru/catalog--food-dairy/10/list?hid=10&clid=500&glfilter=7893318:11'
                                        ),
                                        "urlTouch": LikeUrl.of(
                                            '//m.market.yandex.ru/catalog--food-dairy/10/list?hid=10&clid=707&glfilter=7893318:11'
                                        ),
                                        "urlForCounter": "",
                                    },
                                    "title": {
                                        "url": LikeUrl.of(
                                            '//market.yandex.ru/catalog--food-dairy/10/list?hid=10&clid=500&glfilter=7893318:11'
                                        ),
                                        "urlTouch": LikeUrl.of(
                                            '//m.market.yandex.ru/catalog--food-dairy/10/list?hid=10&clid=707&glfilter=7893318:11'
                                        ),
                                        "text": {"__hl": {"text": "dairy", "raw": True}},
                                        "urlForCounter": "",
                                    },
                                    "label": {"text": "100 моделей"},
                                },
                                {
                                    "thumb": {
                                        "text": "",
                                        "url": LikeUrl.of(
                                            '//market.yandex.ru/catalog--food-butcher/10/list?hid=10&clid=500&glfilter=7893318:12'
                                        ),
                                        "urlTouch": LikeUrl.of(
                                            '//m.market.yandex.ru/catalog--food-butcher/10/list?hid=10&clid=707&glfilter=7893318:12'
                                        ),
                                        "urlForCounter": "",
                                    },
                                    "title": {
                                        "url": LikeUrl.of(
                                            '//market.yandex.ru/catalog--food-butcher/10/list?hid=10&clid=500&glfilter=7893318:12'
                                        ),
                                        "urlTouch": LikeUrl.of(
                                            '//m.market.yandex.ru/catalog--food-butcher/10/list?hid=10&clid=707&glfilter=7893318:12'
                                        ),
                                        "text": {"__hl": {"text": "butcher", "raw": True}},
                                        "urlForCounter": "",
                                    },
                                    "label": {"text": "100 моделей"},
                                },
                            ],
                            "top_models": [
                                {
                                    "thumb": {
                                        "source": "//mdata.yandex.net/i?path=kit_kat_img.jpg&size=2",
                                        "retinaSource": "//mdata.yandex.net/i?path=kit_kat_img.jpg&size=5",
                                        "url": LikeUrl.of(
                                            '//market.yandex.ru/product--kit-kat/201?clid=500&hid=10&nid=10&clid=500'
                                        ),
                                        "urlTouch": LikeUrl.of(
                                            '//m.market.yandex.ru/product--kit-kat/201?clid=707&hid=10&nid=10&clid=707'
                                        ),
                                    },
                                    "price": {"type": "range", "priceMax": "200", "priceMin": "100", "currency": "RUR"},
                                    "title": {
                                        "url": LikeUrl.of(
                                            '//market.yandex.ru/product--kit-kat/201?clid=500&hid=10&nid=10&clid=500'
                                        ),
                                        "urlTouch": LikeUrl.of(
                                            '//m.market.yandex.ru/product--kit-kat/201?clid=707&hid=10&nid=10&clid=707'
                                        ),
                                        "text": {"__hl": {"text": "kit kat", "raw": True}},
                                    },
                                    "vendor_name": {"text": "nestle"},
                                },
                                {
                                    "thumb": {
                                        "source": "//mdata.yandex.net/i?path=actimel_img.jpg&size=2",
                                        "retinaSource": "//mdata.yandex.net/i?path=actimel_img.jpg&size=5",
                                        "url": LikeUrl.of(
                                            '//market.yandex.ru/product--actimel/202?clid=500&hid=10&nid=10&clid=500'
                                        ),
                                        "urlTouch": LikeUrl.of(
                                            '//m.market.yandex.ru/product--actimel/202?clid=707&hid=10&nid=10&clid=707'
                                        ),
                                    },
                                    "price": {"type": "range", "priceMax": "250", "priceMin": "150", "currency": "RUR"},
                                    "title": {
                                        "url": LikeUrl.of(
                                            '//market.yandex.ru/product--actimel/202?clid=500&hid=10&nid=10&clid=500'
                                        ),
                                        "urlTouch": LikeUrl.of(
                                            '//m.market.yandex.ru/product--actimel/202?clid=707&hid=10&nid=10&clid=707'
                                        ),
                                        "text": {"__hl": {"text": "actimel", "raw": True}},
                                    },
                                    "vendor_name": {"text": "danone"},
                                },
                                {
                                    "thumb": {
                                        "source": "//mdata.yandex.net/i?path=sprite_img.jpg&size=2",
                                        "retinaSource": "//mdata.yandex.net/i?path=sprite_img.jpg&size=5",
                                        "url": LikeUrl.of(
                                            '//market.yandex.ru/product--sprite/203?clid=500&hid=10&nid=10&clid=500'
                                        ),
                                        "urlTouch": LikeUrl.of(
                                            '//m.market.yandex.ru/product--sprite/203?clid=707&hid=10&nid=10&clid=707'
                                        ),
                                    },
                                    "price": {"type": "range", "priceMax": "301", "priceMin": "201", "currency": "RUR"},
                                    "title": {
                                        "url": LikeUrl.of(
                                            '//market.yandex.ru/product--sprite/203?clid=500&hid=10&nid=10&clid=500'
                                        ),
                                        "urlTouch": LikeUrl.of(
                                            '//m.market.yandex.ru/product--sprite/203?clid=707&hid=10&nid=10&clid=707'
                                        ),
                                        "text": {"__hl": {"text": "sprite", "raw": True}},
                                    },
                                    "vendor_name": {"text": "coca-cola hbc"},
                                },
                            ],
                            "isAdv": 0,
                        },
                    }
                ]
            },
        )

    def test_offers(self):
        response = self.report.request_bs('place=parallel&text=ring&rids=50')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": [
                    {
                        "url": LikeUrl.of('//market.yandex.ru/search?text=ring&clid=545'),
                        "urlTouch": LikeUrl.of('//m.market.yandex.ru/search?text=ring&clid=708'),
                        "priority": NotEmpty(),
                        "type": "market_constr",
                        "subtype": "market_offers_wizard",
                        "counter": {"path": "/snippet/market/market_offers_wizard"},
                        "title": "\7[Ring\7] treasury",
                        "greenUrl": [
                            {
                                "url": LikeUrl.of('//market.yandex.ru?clid=545'),
                                "urlTouch": LikeUrl.of('//m.market.yandex.ru?clid=708'),
                                "text": "Яндекс.Маркет",
                            },
                            {
                                "url": LikeUrl.of('//market.yandex.ru/search?text=ring&clid=545'),
                                "urlTouch": LikeUrl.of('//m.market.yandex.ru/search?text=ring&clid=708'),
                                "text": "Ring   treasury",
                            },
                        ],
                        "offer_count": 3,
                        "favicon": {"faviconDomain": "market.yandex.ru"},
                        "text": [
                            {
                                "__hl": {
                                    "text": "1 магазин. Выбор по параметрам. Доставка из магазинов treasury и других регионов.",
                                    "raw": True,
                                }
                            }
                        ],
                        "button": [
                            {
                                "url": LikeUrl.of('//market.yandex.ru/search?text=ring&clid=545'),
                                "urlTouch": LikeUrl.of('//m.market.yandex.ru/search?text=ring&clid=708'),
                                "text": "Еще 3 предложения",
                            }
                        ],
                        "showcase": {"items": [], "isAdv": 1},
                    }
                ]
            },
        )

    def test_implicit_model(self):
        response = self.report.request_bs('place=parallel&text=corporation+device')
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": [
                    {
                        "url": LikeUrl.of('//market.yandex.ru/search?text=corporation%20device&clid=698'),
                        "urlTouch": LikeUrl.of('//m.market.yandex.ru/search?text=corporation%20device&clid=721'),
                        "priority": NotEmpty(),
                        "type": "market_constr",
                        "subtype": "market_implicit_model",
                        "counter": {"path": "/snippet/market/market_implicit_model"},
                        "title": "\7[Corporation device\7]",
                        "greenUrl": [
                            {
                                "url": LikeUrl.of('//market.yandex.ru?clid=698'),
                                "urlTouch": LikeUrl.of('//m.market.yandex.ru?clid=721'),
                                "text": "Яндекс.Маркет",
                            },
                            {
                                "url": LikeUrl.of('//market.yandex.ru/search?text=corporation%20device&clid=698'),
                                "urlTouch": LikeUrl.of(
                                    '//m.market.yandex.ru/search?text=corporation%20device&clid=721'
                                ),
                                "text": "Corporation device",
                            },
                        ],
                        "favicon": {"faviconDomain": "market.yandex.ru"},
                        "button": [
                            {
                                "url": LikeUrl.of('//market.yandex.ru/search?text=corporation%20device&clid=698'),
                                "urlTouch": LikeUrl.of(
                                    '//m.market.yandex.ru/search?text=corporation%20device&clid=721'
                                ),
                                "text": "Еще 0 предложений",
                            }
                        ],
                        "sitelinks": [
                            {
                                "url": LikeUrl.of(
                                    "//market.yandex.ru/search?show-reviews=1&text=corporation%20device&lr=0&clid=698"
                                ),
                                "urlTouch": LikeUrl.of(
                                    "//m.market.yandex.ru/search?show-reviews=1&text=corporation%20device&lr=0&clid=721"
                                ),
                                "text": "Отзывы",
                            },
                            {
                                "url": LikeUrl.of("//market.yandex.ru/geo?text=corporation%20device&lr=0&clid=698"),
                                "urlTouch": LikeUrl.of(
                                    "//m.market.yandex.ru/geo?text=corporation%20device&lr=0&clid=721"
                                ),
                                "text": "На карте",
                            },
                            {
                                "url": LikeUrl.of(
                                    "//market.yandex.ru/search?delivery-interval=1&text=corporation%20device&lr=0&clid=698"
                                ),
                                "urlTouch": LikeUrl.of(
                                    "//m.market.yandex.ru/search?delivery-interval=1&text=corporation%20device&lr=0&clid=721"
                                ),
                                "text": "С доставкой завтра",
                            },
                        ],
                        "showcase": {
                            "items": [
                                {
                                    "thumb": {
                                        "text": "",
                                        "url": LikeUrl.of(
                                            '//market.yandex.ru/product--corporation-device-101/60?hid=60&nid=65&clid=698'
                                        ),
                                        "urlTouch": LikeUrl.of(
                                            '//m.market.yandex.ru/product--corporation-device-101/60?hid=60&nid=65&clid=721'
                                        ),
                                        "urlForCounter": "",
                                    },
                                    "price": {"type": "min", "priceMin": "100", "currency": "RUR"},
                                    "title": {
                                        "url": LikeUrl.of(
                                            '//market.yandex.ru/product--corporation-device-101/60?hid=60&nid=65&clid=698'
                                        ),
                                        "urlTouch": LikeUrl.of(
                                            '//m.market.yandex.ru/product--corporation-device-101/60?hid=60&nid=65&clid=721'
                                        ),
                                        "text": {"__hl": {"text": "corporation device 101", "raw": True}},
                                        "urlForCounter": "",
                                    },
                                },
                                {
                                    "thumb": {
                                        "text": "",
                                        "url": LikeUrl.of(
                                            '//market.yandex.ru/product--corporation-device-102/61?hid=60&nid=65&clid=698'
                                        ),
                                        "urlTouch": LikeUrl.of(
                                            '//m.market.yandex.ru/product--corporation-device-102/61?hid=60&nid=65&clid=721'
                                        ),
                                        "urlForCounter": "",
                                    },
                                    "price": {"type": "min", "priceMin": "100", "currency": "RUR"},
                                    "title": {
                                        "url": LikeUrl.of(
                                            '//market.yandex.ru/product--corporation-device-102/61?hid=60&nid=65&clid=698'
                                        ),
                                        "urlTouch": LikeUrl.of(
                                            '//m.market.yandex.ru/product--corporation-device-102/61?hid=60&nid=65&clid=721'
                                        ),
                                        "text": {"__hl": {"text": "corporation device 102", "raw": True}},
                                        "urlForCounter": "",
                                    },
                                },
                            ],
                            "isAdv": 0,
                        },
                    }
                ]
            },
        )

    @classmethod
    def prepare_offers_incut_full_title(cls):
        """Подготовка данных для проверки что тайтл в офферной врезке не обрезается"""
        cls.index.offers += [
            Offer(title='bracelet 1 with very very long title', ts=1),
            Offer(title='bracelet 2 with very very long title', ts=2),
            Offer(title='bracelet 3 with very very long title', ts=3),
            Offer(title='bracelet 4 with very very long title', ts=4),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1).respond(0.5)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2).respond(0.4)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3).respond(0.3)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 4).respond(0.2)

    def test_offers_incut_full_title(self):
        """Проверка что тайтл в офферной врезке не обрезается
        https://st.yandex-team.ru/MARKETOUT-14914
        https://st.yandex-team.ru/MARKETOUT-25355 - флаг раскачен и удален
        """
        response = self.report.request_bs('place=parallel&text=bracelet')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": [
                    {
                        "showcase": {
                            "items": [
                                {
                                    "title": {
                                        "text": {"__hl": {"text": "bracelet 1 with very very long title", "raw": True}}
                                    }
                                },
                                {
                                    "title": {
                                        "text": {"__hl": {"text": "bracelet 2 with very very long title", "raw": True}}
                                    }
                                },
                                {
                                    "title": {
                                        "text": {"__hl": {"text": "bracelet 3 with very very long title", "raw": True}}
                                    }
                                },
                            ]
                        }
                    }
                ]
            },
        )

    @classmethod
    def prepare_implicit_model_sitelink_formula(cls):
        """Подготовка для проверки использования категорийной формулы для сайтлинков в колдунщике неявной модели"""
        cls.index.hypertree += [
            HyperCategory(hid=70, name='sitelink category 1'),
            HyperCategory(hid=71, name='sitelink category 2'),
            HyperCategory(hid=72, name='sitelink category 3'),
            HyperCategory(hid=73, name='sitelink category 4'),
        ]

        cls.matrixnet.on_place(MnPlace.CATEGORY_RELEVANCE, 71).respond(0.7)
        cls.matrixnet.on_place(MnPlace.CATEGORY_RELEVANCE, 72).respond(0.8)
        cls.matrixnet.on_place(MnPlace.CATEGORY_RELEVANCE, 73).respond(0.9)

        cls.index.models += [
            Model(hyperid=70, hid=70, title='sitelink model 1'),
            Model(hyperid=71, hid=71, title='sitelink model 2'),
            Model(hyperid=72, hid=72, title='sitelink model 3'),
            Model(hyperid=73, hid=73, title='sitelink model 4'),
        ]

        cls.index.offers += [
            Offer(hyperid=70),
            Offer(hyperid=71),
            Offer(hyperid=72),
            Offer(hyperid=73),
        ]

    def test_implicit_model_sitelink_formula(self):
        """Проверка использования категорийной формулы для сайтлинков в колдунщике неявной модели
        https://st.yandex-team.ru/MARKETOUT-15233
        """

        old_sitelinks_rearr = 'market_implicit_model_sitelink_reviews=0;market_implicit_wiz_without_offers_categories=0;market_implicit_model_sitelink_map=0;market_implicit_model_sitelink_next_day_delivery=0;market_implicit_model_sitelink_categories=1'  # noqa

        # Проверка с использованием категорийной формулы
        response = self.report.request_bs(
            'place=parallel&text=sitelink+model&rearr-factors='
            'market_implicit_wiz_with_redirect_formula=1;market_categ_wiz_dssm_factor_fast_calc=1;'
            + old_sitelinks_rearr
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": [
                    {
                        "sitelinks": [
                            {"text": "sitelink category 4"},
                            {"text": "sitelink category 3"},
                            {"text": "sitelink category 2"},
                        ]
                    }
                ]
            },
            preserve_order=True,
        )

        # Проверка без категорийной формулы
        response = self.report.request_bs('place=parallel&text=sitelink+model&rearr-factors=' + old_sitelinks_rearr)
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": [
                    {
                        "sitelinks": [
                            {"text": "sitelink category 1"},
                            {"text": "sitelink category 2"},
                            {"text": "sitelink category 3"},
                        ]
                    }
                ]
            },
            preserve_order=True,
        )

    def test_guru_category_reviews_intent_redirect(self):
        """
        https://st.yandex-team.ru/MARKETOUT-15955 добавляем отзывные триггеры
        https://st.yandex-team.ru/MARKETOUT-31733
        :return:
        """
        response = self.report.request_bs_pb('place=parallel&text=food%20отзывы')
        self.assertFragmentIn(
            response,
            {
                "market_ext_category": {
                    "url": LikeUrl.of('//market.yandex.ru/catalog--food/10?hid=10&clid=500'),
                    "urlTouch": LikeUrl.of('//m.market.yandex.ru/catalog?hid=10&nid=10&clid=707'),
                    "greenUrl": [
                        {
                            "url": LikeUrl.of('//market.yandex.ru/catalog--food/10?hid=10&clid=500&'),
                            "urlTouch": LikeUrl.of('//m.market.yandex.ru/catalog?hid=10&nid=10&clid=707'),
                        }
                    ],
                    "button": [
                        {
                            "url": LikeUrl.of('//market.yandex.ru/catalog--food/10?hid=10&clid=500'),
                            "urlTouch": LikeUrl.of('//m.market.yandex.ru/catalog?hid=10&nid=10&clid=707'),
                        }
                    ],
                    "showcase": {
                        "items": [
                            {
                                "thumb": {
                                    "url": LikeUrl.of(
                                        '//market.yandex.ru/catalog--food-bakery/10/list?hid=10&clid=500&glfilter=7893318:10'
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        '//m.market.yandex.ru/catalog--food-bakery/10/list?hid=10&clid=707&glfilter=7893318:10'
                                    ),
                                },
                                "title": {
                                    "url": LikeUrl.of(
                                        '//market.yandex.ru/catalog--food-bakery/10/list?hid=10&clid=500&glfilter=7893318:10'
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        '//m.market.yandex.ru/catalog--food-bakery/10/list?hid=10&clid=707&glfilter=7893318:10'
                                    ),
                                },
                            },
                            {
                                "thumb": {
                                    "url": LikeUrl.of(
                                        '//market.yandex.ru/catalog--food-dairy/10/list?hid=10&clid=500&glfilter=7893318:11'
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        '//m.market.yandex.ru/catalog--food-dairy/10/list?hid=10&clid=707&glfilter=7893318:11'
                                    ),
                                },
                                "title": {
                                    "url": LikeUrl.of(
                                        '//market.yandex.ru/catalog--food-dairy/10/list?hid=10&clid=500&glfilter=7893318:11'
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        '//m.market.yandex.ru/catalog--food-dairy/10/list?hid=10&clid=707&glfilter=7893318:11'
                                    ),
                                },
                            },
                            {
                                "thumb": {
                                    "url": LikeUrl.of(
                                        '//market.yandex.ru/catalog--food-butcher/10/list?hid=10&clid=500&glfilter=7893318:12'
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        '//m.market.yandex.ru/catalog--food-butcher/10/list?hid=10&clid=707&glfilter=7893318:12'
                                    ),
                                },
                                "title": {
                                    "url": LikeUrl.of(
                                        '//market.yandex.ru/catalog--food-butcher/10/list?hid=10&clid=500&glfilter=7893318:12'
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        '//m.market.yandex.ru/catalog--food-butcher/10/list?hid=10&clid=707&glfilter=7893318:12'
                                    ),
                                },
                            },
                        ]
                    },
                }
            },
        )

    def test_offers_reviews_intent_redirect(self):
        """
        https://st.yandex-team.ru/MARKETOUT-15955 добавляем отзывные триггеры - проверяем что пришел cvredirect=2
        :return:
        """
        response = self.report.request_bs_pb('place=parallel&text=ring%20отзывы&rids=50')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "url": LikeUrl.of('//market.yandex.ru/search?text=ring%20отзывы&clid=545&cvredirect=2'),
                    "urlTouch": LikeUrl.of('//m.market.yandex.ru/search?text=ring%20отзывы&clid=708&cvredirect=2'),
                    "greenUrl": [
                        {
                            "url": LikeUrl.of('//market.yandex.ru/search?text=ring%20отзывы&clid=545&cvredirect=2'),
                            "urlTouch": LikeUrl.of(
                                '//m.market.yandex.ru/search?text=ring%20отзывы&clid=708&cvredirect=2'
                            ),
                        }
                    ],
                    "button": [
                        {
                            "url": LikeUrl.of('//market.yandex.ru/search?text=ring%20отзывы&clid=545&cvredirect=2'),
                            "urlTouch": LikeUrl.of(
                                '//m.market.yandex.ru/search?text=ring%20отзывы&clid=708&cvredirect=2'
                            ),
                        }
                    ],
                }
            },
        )

    def test_implicit_model_reviews_intent_redirect(self):
        """
        https://st.yandex-team.ru/MARKETOUT-15955 добавляем отзывные триггеры - проверяем что пришел cvredirect=2
        https://st.yandex-team.ru/MARKETOUT-31733
        :return:
        """
        response = self.report.request_bs_pb('place=parallel&text=corporation+device+отзывы')
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "url": LikeUrl.of(
                        '//market.yandex.ru/search?text=corporation%20device%20отзывы&clid=698&cvredirect=2'
                    ),
                    "urlTouch": LikeUrl.of(
                        '//m.market.yandex.ru/search?text=corporation%20device%20отзывы&clid=721&cvredirect=2'
                    ),
                    "greenUrl": [
                        {
                            "url": LikeUrl.of(
                                '//market.yandex.ru/search?text=corporation%20device%20отзывы&clid=698&cvredirect=2'
                            ),
                            "urlTouch": LikeUrl.of(
                                '//m.market.yandex.ru/search?text=corporation%20device%20отзывы&clid=721&cvredirect=2'
                            ),
                        }
                    ],
                    "button": [
                        {
                            "url": LikeUrl.of(
                                '//market.yandex.ru/search?text=corporation%20device%20отзывы&clid=698&cvredirect=2'
                            ),
                            "urlTouch": LikeUrl.of(
                                '//m.market.yandex.ru/search?text=corporation%20device%20отзывы&clid=721&cvredirect=2'
                            ),
                        }
                    ],
                    "sitelinks": [
                        {
                            "url": LikeUrl.of(
                                "//market.yandex.ru/search?show-reviews=1&text=corporation%20device%20%D0%BE%D1%82%D0%B7%D1%8B%D0%B2%D1%8B&lr=0&clid=698"
                            ),
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/search?show-reviews=1&text=corporation%20device%20%D0%BE%D1%82%D0%B7%D1%8B%D0%B2%D1%8B&lr=0&clid=721"
                            ),
                            "text": "Отзывы",
                        },
                        {
                            "url": LikeUrl.of(
                                "//market.yandex.ru/geo?text=corporation%20device%20%D0%BE%D1%82%D0%B7%D1%8B%D0%B2%D1%8B&lr=0&clid=698"
                            ),
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/geo?text=corporation%20device%20%D0%BE%D1%82%D0%B7%D1%8B%D0%B2%D1%8B&lr=0&clid=721"
                            ),
                            "text": "На карте",
                        },
                        {
                            "url": LikeUrl.of(
                                "//market.yandex.ru/search?delivery-interval=1&text=corporation%20device%20%D0%BE%D1%82%D0%B7%D1%8B%D0%B2%D1%8B&lr=0&clid=698"
                            ),
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/search?delivery-interval=1&text=corporation%20device%20%D0%BE%D1%82%D0%B7%D1%8B%D0%B2%D1%8B&lr=0&clid=721"
                            ),
                            "text": "С доставкой завтра",
                        },
                    ],
                }
            },
        )

    @skip('white credits will be deleted soon')
    def test_offers_credits_intent_redirect(self):
        """
        MARKETOUT-25127:
        Добавляем кредитные триггеры. Проверяем, что пришел cvredirect=2 под флагом market_add_cvredirect_for_credit_requests=1
        """
        response = self.report.request_bs_pb(
            'place=parallel&text=ring%20в%20кредит&rids=50&rearr-factors=market_add_cvredirect_for_credit_requests=1'
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "url": LikeUrl.of(
                        '//market.yandex.ru/search?text=ring%20в%20кредит&clid=545&cvredirect=2&lr=50&utm_medium=cpc&utm_referrer=wizards'
                    ),
                    "urlTouch": LikeUrl.of(
                        '//m.market.yandex.ru/search?text=ring%20в%20кредит&clid=708&cvredirect=2&lr=50&utm_medium=cpc&utm_referrer=wizards'
                    ),
                    "greenUrl": [
                        {
                            "url": LikeUrl.of(
                                '//market.yandex.ru/search?text=ring%20в%20кредит&clid=545&cvredirect=2&lr=50&utm_medium=cpc&utm_referrer=wizards'
                            ),
                            "urlTouch": LikeUrl.of(
                                '//m.market.yandex.ru/search?text=ring%20в%20кредит&clid=708&cvredirect=2&lr=50&utm_medium=cpc&utm_referrer=wizards'
                            ),
                        }
                    ],
                    "button": [
                        {
                            "url": LikeUrl.of(
                                '//market.yandex.ru/search?text=ring%20в%20кредит&clid=545&cvredirect=2&lr=50&utm_medium=cpc&utm_referrer=wizards'
                            ),
                            "urlTouch": LikeUrl.of(
                                '//m.market.yandex.ru/search?text=ring%20в%20кредит&clid=708&cvredirect=2&lr=50&utm_medium=cpc&utm_referrer=wizards'
                            ),
                        }
                    ],
                }
            },
        )

        # Проверяем, что без флага нет cvredirect=2
        response = self.report.request_bs_pb('place=parallel&text=ring%20в%20кредит&rids=50')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "url": LikeUrl.of(
                        '//market.yandex.ru/search?text=ring%20в%20кредит&clid=545&lr=50&utm_medium=cpc&utm_referrer=wizards',
                        ignore_len=False,
                        ignore_params=['rs'],
                    ),
                    "urlTouch": LikeUrl.of(
                        '//m.market.yandex.ru/search?text=ring%20в%20кредит&clid=708&lr=50&utm_medium=cpc&utm_referrer=wizards',
                        ignore_len=False,
                        ignore_params=['rs'],
                    ),
                    "greenUrl": [
                        {
                            "url": LikeUrl.of(
                                '//market.yandex.ru/search?text=ring%20в%20кредит&clid=545&lr=50&utm_medium=cpc&utm_referrer=wizards',
                                ignore_len=False,
                                ignore_params=['rs'],
                            ),
                            "urlTouch": LikeUrl.of(
                                '//m.market.yandex.ru/search?text=ring%20в%20кредит&clid=708&lr=50&utm_medium=cpc&utm_referrer=wizards',
                                ignore_len=False,
                                ignore_params=['rs'],
                            ),
                        }
                    ],
                    "button": [
                        {
                            "url": LikeUrl.of(
                                '//market.yandex.ru/search?text=ring%20в%20кредит&clid=545&lr=50&utm_medium=cpc&utm_referrer=wizards',
                                ignore_len=False,
                                ignore_params=['rs'],
                            ),
                            "urlTouch": LikeUrl.of(
                                '//m.market.yandex.ru/search?text=ring%20в%20кредит&clid=708&lr=50&utm_medium=cpc&utm_referrer=wizards',
                                ignore_len=False,
                                ignore_params=['rs'],
                            ),
                        }
                    ],
                }
            },
        )

        # Проверяем, что без кредитных слов нет cvredirect=2
        response = self.report.request_bs_pb(
            'place=parallel&text=ring%20в&rids=50&rearr-factors=market_add_cvredirect_for_credit_requests=1'
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "url": LikeUrl.of(
                        '//market.yandex.ru/search?text=ring%20в&clid=545&lr=50&utm_medium=cpc&utm_referrer=wizards',
                        ignore_len=False,
                        ignore_params=['rs'],
                    ),
                    "urlTouch": LikeUrl.of(
                        '//m.market.yandex.ru/search?text=ring%20в&clid=708&lr=50&utm_medium=cpc&utm_referrer=wizards',
                        ignore_len=False,
                        ignore_params=['rs'],
                    ),
                    "greenUrl": [
                        {
                            "url": LikeUrl.of(
                                '//market.yandex.ru/search?text=ring%20в&clid=545&lr=50&utm_medium=cpc&utm_referrer=wizards',
                                ignore_len=False,
                                ignore_params=['rs'],
                            ),
                            "urlTouch": LikeUrl.of(
                                '//m.market.yandex.ru/search?text=ring%20в&clid=708&lr=50&utm_medium=cpc&utm_referrer=wizards',
                                ignore_len=False,
                                ignore_params=['rs'],
                            ),
                        }
                    ],
                    "button": [
                        {
                            "url": LikeUrl.of(
                                '//market.yandex.ru/search?text=ring%20в&clid=545&lr=50&utm_medium=cpc&utm_referrer=wizards',
                                ignore_len=False,
                                ignore_params=['rs'],
                            ),
                            "urlTouch": LikeUrl.of(
                                '//m.market.yandex.ru/search?text=ring%20в&clid=708&lr=50&utm_medium=cpc&utm_referrer=wizards',
                                ignore_len=False,
                                ignore_params=['rs'],
                            ),
                        }
                    ],
                }
            },
        )

    def test_cut_price_landings_with_redirect(self):
        """
        Проверяем, что под флагом на потоке запросов за уценёнными товарами в
        лендингах проставляется cvredirect=2
        """

        request_cut_price = 'place=parallel&text=ring%20уценочка&rids=50'
        request_no_cut_price = 'place=parallel&text=ring%20ушлепочка&rids=50'
        flag = '&rearr-factors=market_add_cvredirect_for_cut_price_requests=1'

        url = '//market.yandex.ru/search?clid=545&lr=50&utm_medium=cpc&utm_referrer=wizards'
        touch_url = '//m.market.yandex.ru/search?clid=708&lr=50&utm_medium=cpc&utm_referrer=wizards'
        cvredirect = '&cvredirect=2'

        response = self.report.request_bs_pb(request_cut_price + flag)
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "url": LikeUrl.of(url + cvredirect),
                    "urlTouch": LikeUrl.of(touch_url + cvredirect),
                    "greenUrl": [
                        {
                            "url": LikeUrl.of(url + cvredirect),
                            "urlTouch": LikeUrl.of(touch_url + cvredirect),
                        }
                    ],
                    "button": [
                        {
                            "url": LikeUrl.of(url + cvredirect),
                            "urlTouch": LikeUrl.of(touch_url + cvredirect),
                        }
                    ],
                }
            },
        )

        # Проверяем, что без флага нет cvredirect=2
        response = self.report.request_bs_pb(request_cut_price)
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "url": LikeUrl.of(url, ignore_len=False, ignore_params=['rs', 'text']),
                    "urlTouch": LikeUrl.of(touch_url, ignore_len=False, ignore_params=['rs', 'text']),
                    "greenUrl": [
                        {
                            "url": LikeUrl.of(url, ignore_len=False, ignore_params=['rs', 'text']),
                            "urlTouch": LikeUrl.of(touch_url, ignore_len=False, ignore_params=['rs', 'text']),
                        }
                    ],
                    "button": [
                        {
                            "url": LikeUrl.of(url, ignore_len=False, ignore_params=['rs', 'text']),
                            "urlTouch": LikeUrl.of(touch_url, ignore_len=False, ignore_params=['rs', 'text']),
                        }
                    ],
                }
            },
        )

        # Проверяем, что не на потоке уценённых нет cvredirect=2
        response = self.report.request_bs_pb(request_no_cut_price + flag)
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "url": LikeUrl.of(url, ignore_len=False, ignore_params=['rs', 'text']),
                    "urlTouch": LikeUrl.of(touch_url, ignore_len=False, ignore_params=['rs', 'text']),
                    "greenUrl": [
                        {
                            "url": LikeUrl.of(url, ignore_len=False, ignore_params=['rs', 'text']),
                            "urlTouch": LikeUrl.of(touch_url, ignore_len=False, ignore_params=['rs', 'text']),
                        }
                    ],
                    "button": [
                        {
                            "url": LikeUrl.of(url, ignore_len=False, ignore_params=['rs', 'text']),
                            "urlTouch": LikeUrl.of(touch_url, ignore_len=False, ignore_params=['rs', 'text']),
                        }
                    ],
                }
            },
        )

    def test_redirect_on_search_only(self):
        """Проверяем, что параметр &cvredirect=2 добавляется
        только в ссылки на серч при наличии параметра &text и без параметра &hid
        https://st.yandex-team.ru/MARKETOUT-30184
        https://st.yandex-team.ru/MARKETOUT-31733
        """

        # Колдунщик гуру-категории
        response = self.report.request_bs_pb('place=parallel&text=food+отзывы')
        self.assertFragmentIn(
            response,
            {
                "market_ext_category": {
                    "url": LikeUrl.of('//market.yandex.ru/catalog--food/10?hid=10&clid=500', no_params=['cvredirect']),
                    "urlTouch": LikeUrl.of(
                        '//m.market.yandex.ru/catalog?hid=10&nid=10&clid=707', no_params=['cvredirect']
                    ),
                    "greenUrl": [
                        {
                            "url": LikeUrl.of(
                                '//market.yandex.ru/catalog--food/10?hid=10&clid=500', no_params=['cvredirect']
                            ),
                            "urlTouch": LikeUrl.of(
                                '//m.market.yandex.ru/catalog?hid=10&nid=10&clid=707', no_params=['cvredirect']
                            ),
                        }
                    ],
                    "button": [
                        {
                            "url": LikeUrl.of(
                                '//market.yandex.ru/catalog--food/10?hid=10&clid=500', no_params=['cvredirect']
                            ),
                            "urlTouch": LikeUrl.of(
                                '//m.market.yandex.ru/catalog?hid=10&nid=10&clid=707', no_params=['cvredirect']
                            ),
                        }
                    ],
                    "showcase": {
                        "items": [
                            {
                                "thumb": {
                                    "url": LikeUrl.of(
                                        '//market.yandex.ru/catalog--food-bakery/10/list?hid=10&clid=500&glfilter=7893318:10',
                                        no_params=['cvredirect'],
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        '//m.market.yandex.ru/catalog--food-bakery/10/list?hid=10&clid=707&glfilter=7893318:10',
                                        no_params=['cvredirect'],
                                    ),
                                },
                                "title": {
                                    "url": LikeUrl.of(
                                        '//market.yandex.ru/catalog--food-bakery/10/list?hid=10&clid=500&glfilter=7893318:10',
                                        no_params=['cvredirect'],
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        '//m.market.yandex.ru/catalog--food-bakery/10/list?hid=10&clid=707&glfilter=7893318:10',
                                        no_params=['cvredirect'],
                                    ),
                                },
                            }
                        ]
                    },
                }
            },
        )

        # Колдунщик неявной модели
        response = self.report.request_bs_pb('place=parallel&text=corporation+device+отзывы')
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "url": LikeUrl.of(
                        '//market.yandex.ru/search?text=corporation%20device%20отзывы&clid=698&cvredirect=2'
                    ),
                    "urlTouch": LikeUrl.of(
                        '//m.market.yandex.ru/search?text=corporation%20device%20отзывы&clid=721&cvredirect=2'
                    ),
                    "greenUrl": [
                        {
                            "url": LikeUrl.of(
                                '//market.yandex.ru/search?text=corporation%20device%20отзывы&clid=698&cvredirect=2'
                            ),
                            "urlTouch": LikeUrl.of(
                                '//m.market.yandex.ru/search?text=corporation%20device%20отзывы&clid=721&cvredirect=2'
                            ),
                        }
                    ],
                    "button": [
                        {
                            "url": LikeUrl.of(
                                '//market.yandex.ru/search?text=corporation%20device%20отзывы&clid=698&cvredirect=2'
                            ),
                            "urlTouch": LikeUrl.of(
                                '//m.market.yandex.ru/search?text=corporation%20device%20отзывы&clid=721&cvredirect=2'
                            ),
                        }
                    ],
                    "sitelinks": [
                        {
                            "url": LikeUrl.of(
                                "//market.yandex.ru/search?show-reviews=1&text=corporation%20device%20%D0%BE%D1%82%D0%B7%D1%8B%D0%B2%D1%8B&lr=0&clid=698",
                                no_params=['cvredirect'],
                            ),
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/search?show-reviews=1&text=corporation%20device%20%D0%BE%D1%82%D0%B7%D1%8B%D0%B2%D1%8B&lr=0&clid=721",
                                no_params=['cvredirect'],
                            ),
                            "text": "Отзывы",
                        },
                        {
                            "url": LikeUrl.of(
                                "//market.yandex.ru/geo?text=corporation%20device%20%D0%BE%D1%82%D0%B7%D1%8B%D0%B2%D1%8B&lr=0&clid=698",
                                no_params=['cvredirect'],
                            ),
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/geo?text=corporation%20device%20%D0%BE%D1%82%D0%B7%D1%8B%D0%B2%D1%8B&lr=0&clid=721",
                                no_params=['cvredirect'],
                            ),
                            "text": "На карте",
                        },
                        {
                            "url": LikeUrl.of(
                                "//market.yandex.ru/search?delivery-interval=1&text=corporation%20device%20%D0%BE%D1%82%D0%B7%D1%8B%D0%B2%D1%8B&lr=0&clid=698",
                                no_params=['cvredirect'],
                            ),
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/search?delivery-interval=1&text=corporation%20device%20%D0%BE%D1%82%D0%B7%D1%8B%D0%B2%D1%8B&lr=0&clid=721",
                                no_params=['cvredirect'],
                            ),
                            "text": "С доставкой завтра",
                        },
                    ],
                }
            },
        )

        # Офферный колдунщик
        response = self.report.request_bs_pb(
            'place=parallel&text=ring+отзывы&rids=50&rearr-factors=market_top_categories_in_offers_wizard=1'
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "url": LikeUrl.of('//market.yandex.ru/search?text=ring%20отзывы&clid=545&cvredirect=2'),
                    "urlTouch": LikeUrl.of('//m.market.yandex.ru/search?text=ring%20отзывы&clid=708&cvredirect=2'),
                    "greenUrl": [
                        {
                            "url": LikeUrl.of('//market.yandex.ru/search?text=ring%20отзывы&clid=545&cvredirect=2'),
                            "urlTouch": LikeUrl.of(
                                '//m.market.yandex.ru/search?text=ring%20отзывы&clid=708&cvredirect=2'
                            ),
                        }
                    ],
                    "button": [
                        {
                            "url": LikeUrl.of('//market.yandex.ru/search?text=ring%20отзывы&clid=545&cvredirect=2'),
                            "urlTouch": LikeUrl.of(
                                '//m.market.yandex.ru/search?text=ring%20отзывы&clid=708&cvredirect=2'
                            ),
                        }
                    ],
                    "sitelinks": [
                        {
                            "url": LikeUrl.of(
                                '//market.yandex.ru/search?hid=20&nid=21&text=ring%20%D0%BE%D1%82%D0%B7%D1%8B%D0%B2%D1%8B&clid=545',
                                no_params=['cvredirect'],
                            ),
                            "urlTouch": LikeUrl.of(
                                '//m.market.yandex.ru/search?hid=20&nid=21&text=ring%20%D0%BE%D1%82%D0%B7%D1%8B%D0%B2%D1%8B&clid=708',
                                no_params=['cvredirect'],
                            ),
                            "adGUrl": LikeUrl.of(
                                '//market.yandex.ru/search?hid=20&nid=21&text=ring%20%D0%BE%D1%82%D0%B7%D1%8B%D0%B2%D1%8B&clid=913',
                                no_params=['cvredirect'],
                            ),
                            "adGUrlTouch": LikeUrl.of(
                                '//m.market.yandex.ru/search?hid=20&nid=21&text=ring%20%D0%BE%D1%82%D0%B7%D1%8B%D0%B2%D1%8B&clid=919',
                                no_params=['cvredirect'],
                            ),
                        }
                    ],
                }
            },
        )


if __name__ == '__main__':
    main()
