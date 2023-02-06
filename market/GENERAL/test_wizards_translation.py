#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    CardCategory,
    DeliveryBucket,
    DeliveryOption,
    HyperCategory,
    HyperCategoryType,
    MnPlace,
    Model,
    NavCategory,
    Offer,
    Opinion,
    Outlet,
    PickupBucket,
    PickupOption,
    Region,
    RegionalModel,
    Shop,
    Vendor,
)
from core.testcase import TestCase, main
from core.matcher import NoKey


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.regiontree += [
            Region(rid=213, name='Москва', genitive='Москвы', locative='Москве', preposition='в', en_name='Moscow')
        ]

        cls.index.shops += [
            Shop(fesh=1, name="shop 1", priority_region=213, regions=[225]),
            Shop(fesh=2, name="shop 2", priority_region=213, regions=[225], pickup_buckets=[5001]),
            Shop(fesh=3, name="shop 3", priority_region=213, regions=[225]),
            Shop(fesh=4, name="shop 4", priority_region=213, regions=[225]),
        ]

        cls.index.outlets += [
            Outlet(fesh=2, region=213, point_id=1),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5001,
                fesh=2,
                carriers=[99],
                options=[PickupOption(outlet_id=1)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        cls.index.models += [
            Model(hyperid=1, title="lenovo p780", opinion=Opinion(rating=3.5, total_count=100, forum=5, reviews=10)),
        ]

        cls.index.regional_models += [RegionalModel(hyperid=1, rids=[213], offers=10, geo_offers=20)]

        cls.index.offers += [
            Offer(
                title='lenovo p780 offer 1',
                hyperid=1,
                fesh=1,
                ts=1,
                delivery_options=[DeliveryOption(price=0, day_from=0, day_to=2)],
            ),
            Offer(title='lenovo p780 offer 2', hyperid=1, fesh=2, ts=2, has_delivery_options=False, pickup=True),
            Offer(title='lenovo p780 offer 3', hyperid=1, fesh=3, ts=3),
            Offer(title='lenovo p780 offer 4', hyperid=1, fesh=4, ts=4),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2).respond(0.8)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3).respond(0.7)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 4).respond(0.6)

        # ext category
        cls.index.hypertree += [
            HyperCategory(hid=100, name='Смартфоны', output_type=HyperCategoryType.GURU),
        ]

        cls.index.navtree += [
            NavCategory(hid=100, nid=100),
        ]

        cls.index.vendors += [
            Vendor(vendor_id=100),
            Vendor(vendor_id=101),
            Vendor(vendor_id=102),
        ]

        cls.index.cards += [
            CardCategory(hid=100, vendor_ids=[100, 101, 102]),
        ]

    def test_model_wizard_universal_translation(self):
        """Проверяем перевод модельного колдунщика под конструктором
        https://st.yandex-team.ru/MARKETOUT-30421
        """
        request = 'place=parallel&text=lenovo+p780&rids=213&rearr-factors=showcase_universal_model=1'

        # Русский язык
        response = self.report.request_bs_pb(request + '&lang=ru')
        self.assertFragmentIn(
            response,
            {
                "market_model": {
                    "showcase": {
                        "items": [
                            {
                                "title": {"text": {"__hl": {"text": "lenovo p780 offer 1", "raw": True}}},
                                "delivery": {"text": "Доставка бесплатно"},
                            },
                            {
                                "title": {"text": {"__hl": {"text": "lenovo p780 offer 2", "raw": True}}},
                                "delivery": {"text": "Самовывоз"},
                            },
                            {
                                "title": {"text": {"__hl": {"text": "lenovo p780 offer 3", "raw": True}}},
                                "delivery": {"currency": "RUR", "price": "100", "text": NoKey("text")},
                            },
                        ]
                    }
                }
            },
        )

        # Английский язык
        response = self.report.request_bs_pb(request + '&lang=en')
        self.assertFragmentIn(
            response,
            {
                "market_model": {
                    "showcase": {
                        "items": [
                            {
                                "title": {"text": {"__hl": {"text": "lenovo p780 offer 1", "raw": True}}},
                                "delivery": {"text": "Free delivery"},
                            },
                            {
                                "title": {"text": {"__hl": {"text": "lenovo p780 offer 2", "raw": True}}},
                                "delivery": {"text": "Customer pickup"},
                            },
                            {
                                "title": {"text": {"__hl": {"text": "lenovo p780 offer 3", "raw": True}}},
                                "delivery": {"currency": "RUR", "price": "100", "text": NoKey("text")},
                            },
                        ]
                    }
                }
            },
        )

        # Язык отсутствует в словаре, выдача на русском
        response = self.report.request_bs_pb(request + '&lang=de')
        self.assertFragmentIn(
            response,
            {
                "market_model": {
                    "showcase": {
                        "items": [
                            {
                                "title": {"text": {"__hl": {"text": "lenovo p780 offer 1", "raw": True}}},
                                "delivery": {"text": "Доставка бесплатно"},
                            },
                            {
                                "title": {"text": {"__hl": {"text": "lenovo p780 offer 2", "raw": True}}},
                                "delivery": {"text": "Самовывоз"},
                            },
                            {
                                "title": {"text": {"__hl": {"text": "lenovo p780 offer 3", "raw": True}}},
                                "delivery": {"currency": "RUR", "price": "100", "text": NoKey("text")},
                            },
                        ]
                    }
                }
            },
        )

    def test_offer_wizard_translation(self):
        """Проверяем перевод офферного колдунщика
        https://st.yandex-team.ru/MARKETOUT-30421
        """
        request = 'place=parallel&text=lenovo+p780&rids=213'

        # Русский язык
        response = self.report.request_bs_pb(request + '&lang=ru')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "geo": {
                        "title": "Адреса магазинов в Москве",
                    },
                    "showcase": {
                        "items": [
                            {
                                "title": {"text": {"__hl": {"text": "lenovo p780 offer 1", "raw": True}}},
                                "delivery": {"text": "Доставка бесплатно"},
                            },
                            {
                                "title": {"text": {"__hl": {"text": "lenovo p780 offer 2", "raw": True}}},
                                "delivery": {"text": "Самовывоз"},
                            },
                            {
                                "title": {"text": {"__hl": {"text": "lenovo p780 offer 3", "raw": True}}},
                                "delivery": {"currency": "RUR", "price": "100", "text": NoKey("text")},
                            },
                        ]
                    },
                }
            },
        )

        # Английский язык
        response = self.report.request_bs_pb(request + '&lang=en')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "geo": {
                        "title": "Shop addresses in Moscow",
                    },
                    "showcase": {
                        "items": [
                            {
                                "title": {"text": {"__hl": {"text": "lenovo p780 offer 1", "raw": True}}},
                                "delivery": {"text": "Free delivery"},
                            },
                            {
                                "title": {"text": {"__hl": {"text": "lenovo p780 offer 2", "raw": True}}},
                                "delivery": {"text": "Customer pickup"},
                            },
                            {
                                "title": {"text": {"__hl": {"text": "lenovo p780 offer 3", "raw": True}}},
                                "delivery": {"currency": "RUR", "price": "100", "text": NoKey("text")},
                            },
                        ]
                    },
                }
            },
        )

        # Язык отсутствует в словаре, выдача на русском
        response = self.report.request_bs_pb(request + '&lang=de')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "geo": {
                        "title": "Адреса магазинов в Москве",
                    },
                    "showcase": {
                        "items": [
                            {
                                "title": {"text": {"__hl": {"text": "lenovo p780 offer 1", "raw": True}}},
                                "delivery": {"text": "Доставка бесплатно"},
                            },
                            {
                                "title": {"text": {"__hl": {"text": "lenovo p780 offer 2", "raw": True}}},
                                "delivery": {"text": "Самовывоз"},
                            },
                            {
                                "title": {"text": {"__hl": {"text": "lenovo p780 offer 3", "raw": True}}},
                                "delivery": {"currency": "RUR", "price": "100", "text": NoKey("text")},
                            },
                        ]
                    },
                }
            },
        )

    def test_ext_category_wizard_translation(self):
        """Проверяем перевод категорийного колдунщика
        https://st.yandex-team.ru/MARKETOUT-31439
        """
        request = 'place=parallel&text=Смартфоны'

        # Русский язык
        response = self.report.request_bs_pb(request + '&lang=ru')
        self.assertFragmentIn(
            response,
            {
                "market_ext_category": {
                    "title": {"__hl": {"text": "Смартфоны на Маркете", "raw": True}},
                    "showcase": {"items": [{"label": {"text": "100 моделей"}}]},
                }
            },
        )

        # Английский язык
        response = self.report.request_bs_pb(request + '&lang=en')
        self.assertFragmentIn(
            response,
            {
                "market_ext_category": {
                    "title": {"__hl": {"text": "Смартфоны on Yandex.Market", "raw": True}},
                    "showcase": {"items": [{"label": {"text": "100 models"}}]},
                }
            },
        )

        # Язык отсутствует в словаре, выдача на русском
        response = self.report.request_bs_pb(request + '&lang=de')
        self.assertFragmentIn(
            response,
            {
                "market_ext_category": {
                    "title": {"__hl": {"text": "Смартфоны на Маркете", "raw": True}},
                    "showcase": {"items": [{"label": {"text": "100 моделей"}}]},
                }
            },
        )


if __name__ == '__main__':
    main()
