#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.matcher import Absent, NotEmpty
from core.testcase import TestCase, main
from core.types import HyperCategory, Model, NewShopRating, Offer, Promo, PromoType, Shop


from datetime import datetime


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']
        cls.index.hypertree += [
            HyperCategory(hid=1),
        ]

        cls.index.shops += [
            Shop(fesh=1, priority_region=213, regions=[225], new_shop_rating=NewShopRating(new_rating_total=1.0)),
        ]

        cls.index.models += [
            Model(hyperid=1, title='model with offer meow', hid=1),
        ]

        cls.index.offers += [
            Offer(title='offer with discount meow', price=100, discount=50, hyperid=1),
            Offer(
                title='promo meow',
                promo=Promo(
                    promo_type=PromoType.N_PLUS_ONE,
                    start_date=datetime(1980, 1, 1),
                    end_date=datetime(2050, 1, 1),
                    key='xMpCOKC5I4INzFCab3WEmw',
                    required_quantity=3,
                    free_quantity=34,
                ),
            ),
            Offer(title='boring meow'),
        ]

    def test_disable_filters(self):
        """
        Проверяем что фильтры скрываются и перестают работать
        """
        response = self.report.request_json('place=prime&text=meow&pp=18&rearr-factors=market_metadoc_search=no')
        self.assertFragmentIn(response, {"search": {"total": 4}})
        self.assertFragmentIn(response, {"text": "по размеру скидки"})

        response = self.report.request_json(
            'place=prime&text=meow&pp=18&promo-type=All&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(response, {"search": {"total": 1}})

        response = self.report.request_json(
            'place=prime&text=meow&pp=18&filter-discount-only=1&allow-collapsing=1&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(response, {"search": {"total": 1, "results": [{"entity": "product"}]}})
        self.assertFragmentIn(response, {"text": "по размеру скидки"})

        response = self.report.request_json(
            'place=prime&text=meow&pp=18&filter-promo-or-discount=1&allow-collapsing=1&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(response, {"search": {"total": 2}})
        self.assertFragmentIn(response, {"text": "по размеру скидки"})

        response = self.report.request_json(
            'place=prime&text=meow&pp=18&promo-type=promo-code&filter-promo-or-discount=1&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(response, {"search": {"total": 0}})

        response = self.report.request_json(
            'place=prime&text=meow&pp=18&filter-discount-only=1&filter-promo-or-discount=1&rearr-factors=market_remove_promos=1&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(response, {"search": {"total": 4}})
        self.assertFragmentNotIn(response, "filter-discount-only")
        self.assertFragmentNotIn(response, "filter-promo-or-discount")

    def test_promo(self):
        """
        Проверяем что акции скрываются с потрохами
        """

        response = self.report.request_json('place=prime&text=promo&pp=18')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "offer", "promos": NotEmpty()},
                ]
            },
        )

        response = self.report.request_json('place=prime&text=promo&pp=18&rearr-factors=market_remove_promos=1')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "offer", "promos": Absent()},
                ]
            },
        )

    def test_discount(self):
        """
        Проверяем что скидки скрываются с потрохами
        """
        response = self.report.request_json('place=prime&text=offer&pp=18')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "product", "prices": {"discount": NotEmpty()}},
                    {"entity": "offer", "prices": {"discount": NotEmpty()}},
                ]
            },
        )
        self.assertFragmentIn(response, {"text": "по размеру скидки"})

        response = self.report.request_json('place=prime&text=offer&pp=18&rearr-factors=market_remove_promos=1')

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "product", "prices": {"discount": Absent()}},
                    {"entity": "offer", "prices": {"discount": Absent()}},
                ]
            },
        )
        self.assertFragmentNotIn(response, {"text": "по размеру скидки"})

        response = self.report.request_json('place=productoffers&hyperid=1&pp=18')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "offer", "prices": {"discount": NotEmpty()}},
                ]
            },
        )
        self.assertFragmentIn(response, {"text": "по размеру скидки"})

        response = self.report.request_json('place=productoffers&hyperid=1&pp=18&rearr-factors=market_remove_promos=1')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "offer", "prices": {"discount": Absent()}},
                ]
            },
        )
        self.assertFragmentNotIn(response, {"text": "по размеру скидки"})


if __name__ == '__main__':
    main()
