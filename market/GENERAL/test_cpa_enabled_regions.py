#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Currency, DeliveryBucket, DeliveryOption, ExchangeRate, Offer, Region, RegionalDelivery, Shop
from core.testcase import TestCase, main
from core.matcher import NoKey


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.shops = [
            Shop(
                fesh=10204595,
                regions=[225, 134],
                priority_region=11514,
                priority_region_original=11514,
                home_region=134,
                currency=Currency.USD,
                cpa=Shop.CPA_REAL,
            ),
            Shop(
                fesh=10204596,
                regions=[225, 134],
                priority_region=11514,
                priority_region_original=11514,
                home_region=134,
                currency=Currency.EUR,
                cpa=Shop.CPA_REAL,
            ),
            Shop(fesh=111, regions=[134], priority_region=213, cpa=Shop.CPA_REAL),
        ]
        cls.index.currencies = [
            Currency(
                name=Currency.USD,
                exchange_rates=[
                    ExchangeRate(to=Currency.RUR, rate=66.68),
                    ExchangeRate(to=Currency.EUR, rate=0.9),
                    ExchangeRate(fr=Currency.UE, rate=1),
                ],
            ),
            Currency(
                name=Currency.EUR,
                exchange_rates=[
                    ExchangeRate(to=Currency.RUR, rate=73.84),
                    ExchangeRate(to=Currency.USD, rate=1.11),
                    ExchangeRate(fr=Currency.UE, rate=1.11),
                ],
            ),
        ]
        cls.index.regiontree += [
            # Region.FEDERATIVE_SUBJECT is attached to 225 (Russia)
            Region(
                rid=3,
                name="Центральный федеральный округ",
                region_type=Region.FEDERATIVE_SUBJECT,
                children=[Region(rid=213, name="Москва")],
            ),
            Region(
                rid=134, name="Китай", region_type=Region.COUNTRY, children=[Region(rid=11514, name="Гонконг (Сянган)")]
            ),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=10001,
                fesh=10204595,
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=10, day_from=1, day_to=2)])],
            ),
            DeliveryBucket(
                bucket_id=10002,
                fesh=10204596,
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=10, day_from=1, day_to=2)])],
            ),
        ]

        cls.index.offers = [
            Offer(title="CPA offer", fesh=10204595, cpa=Offer.CPA_REAL, delivery_buckets=[10001]),
            Offer(title="non-CPA offer", fesh=10204595, cpa=Offer.CPA_NO),
            Offer(title="CPA offer", fesh=10204596, cpa=Offer.CPA_REAL, delivery_buckets=[10001]),
            Offer(title="non-CPA offer", fesh=10204596, cpa=Offer.CPA_NO),
            Offer(fesh=111, cpa=Offer.CPA_REAL),
        ]

    def test_cpa_offers_from_cpa_enabled_region(self):
        # shop delivers to Russia (225) is listed in DEFAULT_CPA_ENABLED_REGION{225}
        for shop in (10204595, 10204596):
            response = self.report.request_json("place=prime&fesh={}&rids=213".format(shop))
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "cpaCount": 1,
                        "results": [
                            {"entity": "offer", "titles": {"raw": "CPA offer"}, "cpa": "real"},
                            {
                                "entity": "offer",
                                "titles": {"raw": "non-CPA offer"},
                            },
                        ],
                    }
                },
            )

    def test_cpa_offers_from_cpa_disabled_region_allow_cpc_pessimization(self):
        # shop delivers to China (134) but 134 is not listed in DEFAULT_CPA_ENABLED_REGION{225}
        for shop in (10204595, 10204596):
            response = self.report.request_json(
                "place=prime&fesh={}&rids=134&rearr-factors=market_filter_cpa_to_cpc_degradation=0".format(shop)
            )
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "cpaCount": 0,
                        "results": [
                            {
                                "entity": "offer",
                                "titles": {"raw": "CPA offer"},
                            },
                            {
                                "entity": "offer",
                                "titles": {"raw": "non-CPA offer"},
                            },
                        ],
                    }
                },
            )
            self.assertFragmentNotIn(response, {"entity": "offer", "cpa": "real"})

    def test_cpa_offers_from_cpa_disabled_region_forbid_cpc_pessimization(self):
        # shop delivers to China (134) but 134 is not listed in DEFAULT_CPA_ENABLED_REGION{225}
        for shop in (10204595, 10204596):
            response = self.report.request_json("place=prime&fesh={}&rids=134&debug=1".format(shop))
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "cpaCount": 0,
                        "results": [
                            {
                                "entity": "offer",
                                "titles": {"raw": "non-CPA offer"},
                            },
                        ],
                    }
                },
            )
            self.assertFragmentNotIn(response, {"entity": "offer", "cpa": "real"})
            self.assertFragmentIn(response, {"filters": {"HIDE_CPA_PESSIMIZATION_CLIENT_REGION_IS_NOT_CPA": 1}})

    def test_cpa_offers_from_all_regions(self):
        # region=0 ('all regions') means 'CPA-enabled regions' logic is not active
        for shop in (10204595, 10204596):
            response = self.report.request_json("place=prime&fesh={}".format(shop))
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "cpaCount": 1,
                        "results": [
                            {"entity": "offer", "titles": {"raw": "CPA offer"}, "cpa": "real"},
                            {
                                "entity": "offer",
                                "titles": {"raw": "non-CPA offer"},
                            },
                        ],
                    }
                },
            )

    def test_cpa_filter(self):
        """
        Проверяем что CGI параметр cpa= работает в случае несовпадения валюты
        магазина и пользователя.
        """
        response = self.report.request_json('place=prime&fesh=10204595&rids=213&currency=RUR&cpa=no')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "totalOffers": 1,
                    "cpaCount": 0,
                    "results": [{"entity": "offer", "titles": {"raw": "non-CPA offer"}}],
                }
            },
        )
        response = self.report.request_json('place=prime&fesh=10204595&rids=213&currency=RUR&cpa=-no')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "totalOffers": 1,
                    "cpaCount": 1,
                    "results": [{"entity": "offer", "titles": {"raw": "CPA offer"}, "cpa": "real"}],
                }
            },
        )

    def test_cpa_pessimisation_in_non_cpa_region_allow_cpc_pessimization(self):
        """Проверяется, что оффер становится из CPA -> CPC для не CPA региона (134)"""
        response = self.report.request_json(
            "place=prime&fesh=111&rids=134&debug=1&rearr-factors=market_filter_cpa_to_cpc_degradation=0"
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "cpa": NoKey("cpa"),
                "debug": {"properties": {"CPA_PESSIMIZATION_CLIENT_REGION_IS_NOT_CPA": "1"}},
            },
        )

    def test_cpa_pessimisation_in_non_cpa_region_forbid_cpc_pessimization(self):
        """Проверяется, что оффер скрывается из-за CPA -> CPC для не CPA региона (134)"""
        response = self.report.request_json("place=prime&fesh=111&rids=134&debug=1")
        self.assertFragmentIn(response, {"filters": {"HIDE_CPA_PESSIMIZATION_CLIENT_REGION_IS_NOT_CPA": 1}})


if __name__ == '__main__':
    main()
