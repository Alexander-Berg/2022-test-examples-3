#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import CpaCategory, Currency, DeliveryOption, ExchangeRate, MnPlace, Model, Offer, Region, Shop
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ["market_money_disable_bids=0"]
        cls.index.regiontree += [Region(rid=213, name='Москва', genitive='Москвы', locative='Москве', preposition='в')]

        cls.index.shops += [
            Shop(fesh=1, priority_region=213, regions=[225], cpa=Shop.CPA_REAL),
            Shop(fesh=2, priority_region=213, regions=[225], cpa=Shop.CPA_REAL),
        ]

        cls.index.currencies = [Currency('BYN', exchange_rates=[ExchangeRate(to=Currency.RUR, rate=33)], country=149)]

        cls.index.cpa_categories += [
            CpaCategory(hid=29167, regions=[213]),
        ]

        cls.index.models += [
            Model(hyperid=291671, hid=29167, title="phubbing 1", ts=291671),
            Model(hyperid=291672, hid=29167, title="phubbing 2", ts=291672),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 291671).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 291672).respond(0.8)

        cls.index.offers += [
            Offer(
                title='phubbing 1 offer 1',
                hid=29167,
                hyperid=291671,
                fesh=1,
                bid=100,
                price=234.5,
                delivery_options=[DeliveryOption(price=123.4)],
                price_old=357.9,
            ),
            Offer(
                title='phubbing 1 offer 2',
                hid=29167,
                hyperid=291671,
                fesh=2,
                bid=90,
                price=543.2,
                delivery_options=[DeliveryOption(price=321.9)],
            ),
            Offer(title='phubbing 2 offer 1', hid=29167, hyperid=291672, fesh=1),
        ]

    def test_price_rounding(self):
        """Округление работает единообразно с сервисом, по факту окгруляется до целого
        для всех валют кроме BYN.

        Тест на валюту, отличную от BYN (RUR).

        https://st.yandex-team.ru/MARKETOUT-29167
        https://st.yandex-team.ru/MARKETOUT-30822
        """

        enable_offers_incut = (
            "&rearr-factors=market_offers_wiz_top_offers_threshold=0"
            ";market_offers_incut_align=0"
            ";market_offers_incut_threshold_disable=1"
        )

        # 1. Офферный
        response = self.report.request_bs_pb('place=parallel&text=phubbing&rids=213' + enable_offers_incut)
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "phubbing 1 offer 1", "raw": True}},
                                },
                                "price": {"priceMax": "235", "currency": "RUR", "type": "average"},
                                "delivery": {
                                    "price": "123",
                                    "currency": "RUR",
                                },
                                "discount": {"currency": "RUR", "oldprice": "358", "percent": "34"},
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "phubbing 1 offer 2", "raw": True}},
                                },
                                "price": {"priceMax": "543", "currency": "RUR", "type": "average"},
                                "delivery": {
                                    "price": "322",
                                    "currency": "RUR",
                                },
                            },
                        ]
                    }
                }
            },
        )

        # 3. Неявная
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {
                        "items": [
                            {
                                "price": {"type": "min", "priceMin": "235", "currency": "RUR"},
                                "title": {
                                    "text": {"__hl": {"text": "phubbing 1", "raw": True}},
                                },
                            },
                        ]
                    }
                }
            },
            preserve_order=True,
            allow_different_len=True,
        )

        # 4. Модельный под конструктором
        response = self.report.request_bs_pb(
            'place=parallel&text=phubbing&rids=213&rearr-factors=showcase_universal_model=1'
        )
        self.assertFragmentIn(
            response,
            {
                "market_model": {
                    "price": {"priceMin": "235", "priceMax": "543", "currency": "RUR", "type": "range"},
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "phubbing 1 offer 1", "raw": True}},
                                },
                                "price": {"priceMax": "235", "currency": "RUR", "type": "average"},
                                "delivery": {
                                    "price": "123",
                                    "currency": "RUR",
                                },
                                "discount": {"currency": "RUR", "oldprice": "", "percent": ""},
                            }
                        ]
                    },
                }
            },
        )

    def test_byn_price_rounding(self):
        """Округление работает единообразно с сервисом, по факту окгруляется до целого
        для всех валют кроме BYN.

        Тест на  BYN (округление до 2 знаков).

        https://st.yandex-team.ru/MARKETOUT-29167
        https://st.yandex-team.ru/MARKETOUT-30822
        """

        enable_offers_incut = (
            "&rearr-factors=market_offers_wiz_top_offers_threshold=0"
            ";market_offers_incut_align=0"
            ";market_offers_incut_threshold_disable=1"
        )

        response = self.report.request_bs_pb(
            'place=parallel&text=phubbing&rids=213' + enable_offers_incut + '&currency=BYN'
        )

        # 1. Офферный (BYN)
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "phubbing 1 offer 1", "raw": True}},
                                },
                                "price": {"priceMax": "7.11", "currency": "BYN", "type": "average"},
                                "delivery": {
                                    "price": "3.74",
                                    "currency": "BYN",
                                },
                                "discount": {"currency": "BYN", "oldprice": "10.85", "percent": "34"},
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "phubbing 1 offer 2", "raw": True}},
                                },
                                "price": {"priceMax": "16.46", "currency": "BYN", "type": "average"},
                                "delivery": {
                                    "price": "9.75",
                                    "currency": "BYN",
                                },
                            },
                        ]
                    }
                }
            },
        )

        # 3. Неявная (BYN)
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {
                        "items": [
                            {
                                "price": {"type": "min", "priceMin": "7.11", "currency": "BYN"},
                                "title": {
                                    "text": {"__hl": {"text": "phubbing 1", "raw": True}},
                                },
                            },
                        ]
                    }
                }
            },
            preserve_order=True,
            allow_different_len=True,
        )

        # 4. Модельный под конструктором (BYN)
        response = self.report.request_bs_pb(
            'place=parallel&text=phubbing&rids=213&rearr-factors=showcase_universal_model=1&currency=BYN'
        )
        self.assertFragmentIn(
            response,
            {
                "market_model": {
                    "price": {"priceMin": "7.11", "priceMax": "16.46", "currency": "BYN", "type": "range"},
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "phubbing 1 offer 1", "raw": True}},
                                },
                                "price": {"priceMax": "7.11", "currency": "BYN", "type": "average"},
                                "delivery": {
                                    "price": "3.74",
                                    "currency": "BYN",
                                },
                                "discount": {"currency": "BYN", "oldprice": "", "percent": ""},
                            }
                        ]
                    },
                }
            },
        )


if __name__ == '__main__':
    main()
