#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.types import (
    CardCategory,
    HyperCategory,
    HyperCategoryType,
    Model,
    NavCategory,
    Opinion,
    RegionalModel,
    Vendor,
)

from core.testcase import TestCase, main
from core.matcher import LikeUrl, NoKey


class T(TestCase):
    @classmethod
    def prepare_category_wizard_universal_rating(cls):
        # Готовим стандартные данные для формирования колдунщика гуру-категории — несколько вендоров с моделями
        cls.index.hypertree += [HyperCategory(hid=10, name='mattress', output_type=HyperCategoryType.GURU)]

        cls.index.navtree += [NavCategory(nid=110, hid=10)]

        cls.index.vendors += [
            Vendor(vendor_id=10, name='bony'),
            Vendor(vendor_id=11, name='dormeo'),
            Vendor(vendor_id=12, name='consul'),
        ]

        cls.index.vendors += [
            Vendor(vendor_id=301),
            Vendor(vendor_id=302),
            Vendor(vendor_id=303),
        ]

        # Единственная особенность — явно задаем рейтинги у моделей, потому что их и будем тестить
        cls.index.models += [
            Model(
                hyperid=201,
                hid=10,
                title='mattress bony',
                vendor_id=301,
                opinion=Opinion(rating=2, total_count=10, precise_rating=2.34),
            ),
            Model(hyperid=202, hid=10, title='mattress dormeo', vendor_id=302),
            Model(
                hyperid=203,
                hid=10,
                title='mattress consul',
                vendor_id=303,
                opinion=Opinion(rating=4, total_count=20, precise_rating=4.56),
            ),
        ]

        cls.index.cards += [CardCategory(hid=10, vendor_ids=[10, 11, 12], hyperids=[201, 202, 203])]

        cls.index.regional_models += [
            RegionalModel(hyperid=201, offers=1),
            RegionalModel(hyperid=202, offers=1),
            RegionalModel(hyperid=203, offers=1),
        ]

    def test_category_wizard_universal_rating(self):
        """Рейтинги моделей в колдунщике гуру-категории под конструктор
        https://st.yandex-team.ru/MARKETOUT-14046
        """

        request = 'place=parallel&text=mattress'

        # С флагом market_ext_category_any_rating показываются рейтинги у всех моделей, у которых они есть
        response = self.report.request_bs(request + "&rearr-factors=market_ext_category_any_rating=1")
        self.assertFragmentIn(
            response,
            {
                "market_ext_category": [
                    {
                        "showcase": {
                            "top_models": [
                                {"rating": {"value": "2.34"}},
                                {"rating": NoKey("rating")},
                                {"rating": {"value": "4.56"}},
                            ]
                        }
                    }
                ]
            },
        )

        # С флагом market_ext_category_high_rating показываются рейтинги > 3
        response = self.report.request_bs(request + "&rearr-factors=market_ext_category_high_rating=1")
        self.assertFragmentIn(
            response,
            {
                "market_ext_category": [
                    {
                        "showcase": {
                            "top_models": [
                                {"rating": NoKey("rating")},
                                {"rating": NoKey("rating")},
                                {"rating": {"value": "4.56"}},
                            ]
                        }
                    }
                ]
            },
        )

        # По умолчанию рейтингов у моделей нет
        response = self.report.request_bs(request)
        self.assertFragmentIn(
            response,
            {
                "market_ext_category": [
                    {
                        "showcase": {
                            "top_models": [
                                {"rating": NoKey("rating")},
                                {"rating": NoKey("rating")},
                                {"rating": NoKey("rating")},
                            ]
                        }
                    }
                ]
            },
        )

    def test_precision_rating_in_category_wizard(self):
        """Проверяем, что в категорийном колдунщике используется рейтинг с точностью 0.01
        https://st.yandex-team.ru/MARKETOUT-31267
        https://st.yandex-team.ru/MARKETOUT-32452
        """
        response = self.report.request_bs_pb(
            'place=parallel&text=mattress&rearr-factors=' 'market_ext_category_any_rating=1'
        )
        self.assertFragmentIn(
            response,
            {
                "market_ext_category": {
                    "showcase": {
                        "top_models": [
                            {"rating": {"value": "2.34"}},
                            {"rating": NoKey("rating")},
                            {"rating": {"value": "4.56"}},
                        ]
                    }
                }
            },
        )

    def test_category_wizard_universal_reviews(self):
        """Отзывы моделей в колдунщике гуру-категории под конструктор
        https://st.yandex-team.ru/MARKETOUT-14046
        """

        # С флагом market_ext_category_reviews показываются отзывы у всех моделей, у которых они есть
        response = self.report.request_bs("place=parallel&text=mattress&rearr-factors=market_ext_category_reviews=1")
        self.assertFragmentIn(
            response,
            {
                "market_ext_category": [
                    {
                        "showcase": {
                            "top_models": [
                                {
                                    "title": {"text": {"__hl": {"text": "mattress bony", "raw": True}}},
                                    "reviews": {
                                        "count": "10",
                                        "url": LikeUrl.of(
                                            "//market.yandex.ru/product--mattress-bony/201/reviews?clid=500"
                                        ),
                                        "urlTouch": LikeUrl.of(
                                            "//m.market.yandex.ru/product--mattress-bony/201/reviews?clid=707"
                                        ),
                                    },
                                },
                                {
                                    "title": {"text": {"__hl": {"text": "mattress dormeo", "raw": True}}},
                                    "reviews": NoKey("reviews"),
                                },
                                {
                                    "title": {"text": {"__hl": {"text": "mattress consul", "raw": True}}},
                                    "reviews": {
                                        "count": "20",
                                        "url": LikeUrl.of(
                                            "//market.yandex.ru/product--mattress-consul/203/reviews?clid=500"
                                        ),
                                        "urlTouch": LikeUrl.of(
                                            "//m.market.yandex.ru/product--mattress-consul/203/reviews?clid=707"
                                        ),
                                    },
                                },
                            ]
                        }
                    }
                ]
            },
        )

    def test_not_onstock_parameter_in_urls(self):
        """Проверяем, что под флагом market_parallel_not_onstock_in_urls=1 в ссылки колдунщика неявной модели,
        ведущие на search и catalog добавляется параметр &onstock=0
        https://st.yandex-team.ru/MARKETOUT-32130
        """
        request = "place=parallel&text=mattress&rearr-factors=market_parallel_not_onstock_in_urls=1"
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                "market_ext_category": {
                    "url": LikeUrl.of("//market.yandex.ru/catalog--mattress/110?hid=10&clid=500&onstock=0"),
                    "urlTouch": LikeUrl.of("//m.market.yandex.ru/catalog?hid=10&nid=110&clid=707&onstock=0"),
                    "adGUrl": LikeUrl.of("//market.yandex.ru/catalog--mattress/110?hid=10&clid=916&onstock=0"),
                    "adGUrlTouch": LikeUrl.of("//m.market.yandex.ru/catalog?hid=10&nid=110&clid=922&onstock=0"),
                    "greenUrl": [
                        {
                            "url": LikeUrl.of("//market.yandex.ru/catalog--mattress/110?hid=10&clid=500&onstock=0"),
                            "urlTouch": LikeUrl.of("//m.market.yandex.ru/catalog?hid=10&nid=110&clid=707&onstock=0"),
                            "adGUrl": LikeUrl.of("//market.yandex.ru/catalog--mattress/110?hid=10&clid=916&onstock=0"),
                            "adGUrlTouch": LikeUrl.of("//m.market.yandex.ru/catalog?hid=10&nid=110&clid=922&onstock=0"),
                        }
                    ],
                    "button": [
                        {
                            "url": LikeUrl.of("//market.yandex.ru/catalog--mattress/110?hid=10&clid=500&onstock=0"),
                            "urlTouch": LikeUrl.of("//m.market.yandex.ru/catalog?hid=10&nid=110&clid=707&onstock=0"),
                            "adGUrl": LikeUrl.of("//market.yandex.ru/catalog--mattress/110?hid=10&clid=916&onstock=0"),
                            "adGUrlTouch": LikeUrl.of("//m.market.yandex.ru/catalog?hid=10&nid=110&clid=922&onstock=0"),
                        }
                    ],
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "url": LikeUrl.of(
                                        "//market.yandex.ru/catalog--mattress-bony/110/list?glfilter=7893318%3A10&hid=10&clid=500&onstock=0"
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/catalog--mattress-bony/110/list?glfilter=7893318%3A10&hid=10&clid=707&onstock=0"
                                    ),
                                    "adGUrl": LikeUrl.of(
                                        "//market.yandex.ru/catalog--mattress-bony/110/list?glfilter=7893318%3A10&hid=10&clid=916&onstock=0"
                                    ),
                                    "adGUrlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/catalog--mattress-bony/110/list?glfilter=7893318%3A10&hid=10&clid=922&onstock=0"
                                    ),
                                },
                                "thumb": {
                                    "url": LikeUrl.of(
                                        "//market.yandex.ru/catalog--mattress-bony/110/list?glfilter=7893318%3A10&hid=10&clid=500&onstock=0"
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/catalog--mattress-bony/110/list?glfilter=7893318%3A10&hid=10&clid=707&onstock=0"
                                    ),
                                    "adGUrl": LikeUrl.of(
                                        "//market.yandex.ru/catalog--mattress-bony/110/list?glfilter=7893318%3A10&hid=10&clid=916&onstock=0"
                                    ),
                                    "adGUrlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/catalog--mattress-bony/110/list?glfilter=7893318%3A10&hid=10&clid=922&onstock=0"
                                    ),
                                },
                            }
                        ]
                    },
                }
            },
        )


if __name__ == "__main__":
    main()
