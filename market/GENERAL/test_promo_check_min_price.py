#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    DeliveryBucket,
    DeliveryOption,
    Model,
    Offer,
    Outlet,
    PickupBucket,
    PickupOption,
    Promo,
    PromoType,
    RegionalDelivery,
    Shop,
)
from core.testcase import TestCase, main
from core.matcher import NoKey


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']
        cls.index.shops += [
            Shop(fesh=1, priority_region=213, regions=[213], name='Котиковый магаз', delivery_service_outlets=[1]),
            Shop(fesh=2, priority_region=213, regions=[213], name='Магаз котов'),
            Shop(fesh=3, priority_region=213, regions=[213], name='Магаз котеек'),
            Shop(fesh=4, priority_region=213, regions=[213], name='У кота'),
            Shop(fesh=5, priority_region=213, regions=[213], name='Котодом'),
            Shop(fesh=6, priority_region=111, regions=[111], name='Оптовый дом Барские Наковальни'),
        ]

        cls.index.outlets += [
            Outlet(fesh=1, region=213, point_id=1),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=1, options=[PickupOption(outlet_id=1)], delivery_program=DeliveryBucket.REGULAR_PROGRAM
            )
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=1,
                regional_options=[
                    RegionalDelivery(rid=213, options=[DeliveryOption(price=123)]),
                ],
            ),
        ]

        cls.index.offers += [
            Offer(
                title='offer 1',
                price=100000,
                price_old=150000,
                price_history=200000,
                hyperid=100,
                fesh=1,
                pickup_buckets=[1],
            ),
            Offer(
                title='offer 2',
                price=110000,
                price_old=150000,
                price_history=200000,
                hyperid=100,
                fesh=1,
                pickup_buckets=[1],
            ),
            Offer(
                title='offer 3',
                price=100000,
                price_old=150000,
                price_history=200000,
                hyperid=101,
                fesh=2,
                delivery_buckets=[1],
            ),
            Offer(
                title='offer 4',
                hyperid=102,
                price=105000,
                promo=Promo(promo_type=PromoType.PROMO_CODE, key='promo1034701_01_key001', discount_value=50),
                fesh=3,
                delivery_buckets=[1],
            ),
            Offer(
                title='offer 5',
                hyperid=102,
                price=170000,
                promo=Promo(promo_type=PromoType.PROMO_CODE, key='promo1034701_01_key002', discount_value=50),
                fesh=3,
                delivery_buckets=[1],
            ),
            Offer(title='offer 6', hyperid=103, price=115000, fesh=4, delivery_buckets=[1]),
            Offer(
                title='offer 7',
                hyperid=103,
                price=185000,
                promo=Promo(promo_type=PromoType.PROMO_CODE, key='promo1034701_01_key003', discount_value=50),
                fesh=4,
                delivery_buckets=[1],
            ),
            Offer(title='offer 8', hyperid=101, price=110000, fesh=2, delivery_buckets=[1]),
            Offer(title='offer 9', hyperid=101, price=110000, fesh=2, delivery_buckets=[1]),
            Offer(
                title='offer 10', price=100000, price_old=150000, price_history=200000, hyperid=104, fesh=5
            ),  # no delivery
            # test_prime_promo_price
            Offer(
                title='promo price 11',
                price=100000,
                benefit_price=90000,
                promo=Promo(promo_type=PromoType.PROMO_CODE, key='p11', discount_value=50),
                hyperid=105,
                fesh=6,
            ),  # cheapest benefit_price for this model, good!
            Offer(
                title='promo price 12',
                price=100000,
                benefit_price=96000,
                promo=Promo(promo_type=PromoType.PROMO_CODE, key='p12', discount_value=50),
                hyperid=105,
                fesh=6,
            ),  # not so cheap benefit_price for this model, bad!
            Offer(title='promo price 13', price=95000, hyperid=105, fesh=6),  # just creating stats
            Offer(
                title='promo price 14',
                price=100000,
                benefit_price=90000,
                promo=Promo(promo_type=PromoType.GIFT_WITH_PURCHASE, key='p15'),
                hyperid=106,
                fesh=6,
            ),  # good
            Offer(
                title='promo price 15',
                price=90000,
                benefit_price=0,
                promo=Promo(promo_type=PromoType.GIFT_WITH_PURCHASE, key='p16'),
                hyperid=106,
                fesh=6,
            ),  # no benefit_price -> use main price
            Offer(title='promo price 16', price=95000, hyperid=106, fesh=6),  # just creating stats
            Offer(
                title='promo price 17',
                price=100000,
                benefit_price=90000,
                promo=Promo(promo_type=PromoType.N_PLUS_ONE, key='p17'),
                hyperid=107,
                fesh=6,
            ),  # good
            Offer(
                title='promo price 18',
                price=90000,
                benefit_price=0,
                promo=Promo(promo_type=PromoType.N_PLUS_ONE, key='p18'),
                hyperid=107,
                fesh=6,
            ),  # no benefit_price -> use main price
            Offer(title='promo price 19', price=95000, hyperid=107, fesh=6),  # just creating stats
        ]

        cls.index.models += [
            Model(hyperid=100, hid=100, title='model with offers 1'),
            Model(hyperid=101, hid=100, title='model with offers 2'),
            Model(hyperid=102, hid=100, title='model with offers 3'),
            Model(hyperid=103, hid=100, title='model with offers 4'),
            Model(hyperid=104, hid=100, title='model with offers 5'),
            Model(hyperid=105, hid=100, title='model with offers 6'),
            Model(hyperid=106, hid=100, title='model with offers 7'),
            Model(hyperid=107, hid=100, title='model with offers 8'),
        ]

    def test_promo_code_factor(self):
        response = self.report.request_json('place=prime&rids=213&text=offer&promo-check-min-price=5&debug=da')
        self.assertFragmentIn(
            response,
            {
                "titles": {"raw": "offer 4"},
                "debug": {
                    "factors": {
                        "GIFTS_WITH_PURCHASE_PROMO_TYPE": NoKey("GIFTS_WITH_PURCHASE_PROMO_TYPE"),
                        "N_PLUS_M_PROMO_TYPE": NoKey("N_PLUS_M_PROMO_TYPE"),
                        "PROMO_CODE_PROMO_TYPE": "1",
                    }
                },
            },
        )

    def test_check_prime(self):
        response = self.report.request_json('place=prime&rids=213&text=offer&promo-check-min-price=5')
        self.assertFragmentIn(
            response,
            [
                {"entity": "offer", "titles": {"raw": "offer 1"}},
                {"entity": "offer", "titles": {"raw": "offer 3"}},
                {"entity": "offer", "titles": {"raw": "offer 4"}},
                {"entity": "offer", "titles": {"raw": "offer 6"}},
            ],
        )
        self.assertFragmentNotIn(response, [{"entity": "offer", "titles": {"raw": "offer 2"}}])
        self.assertFragmentNotIn(response, [{"entity": "offer", "titles": {"raw": "offer 5"}}])
        self.assertFragmentNotIn(response, [{"entity": "offer", "titles": {"raw": "offer 7"}}])
        self.assertFragmentNotIn(response, [{"entity": "product"}])

        # если добавить фильтр по промо, то 6й оффер должен пропасть, но все остальное не собьется
        response = self.report.request_json('place=prime&rids=213&text=offer&promo-check-min-price=5&promo-type=market')
        self.assertFragmentIn(
            response,
            [
                {"entity": "offer", "titles": {"raw": "offer 1"}},
                {"entity": "offer", "titles": {"raw": "offer 3"}},
                {"entity": "offer", "titles": {"raw": "offer 4"}},
            ],
        )

    def test_prime_promo_price(self):
        response = self.report.request_json('place=prime&rids=111&text=promo+price&promo-check-min-price=1')
        # promo-code
        self.assertFragmentIn(
            response,
            [
                {"entity": "offer", "titles": {"raw": "promo price 11"}},
            ],
        )
        self.assertFragmentNotIn(
            response,
            [
                {"entity": "offer", "titles": {"raw": "promo price 12"}},
            ],
        )
        self.assertFragmentIn(
            response,
            [
                {"entity": "offer", "titles": {"raw": "promo price 13"}},
            ],
        )

        # gift with purchase
        self.assertFragmentIn(
            response,
            [
                {"entity": "offer", "titles": {"raw": "promo price 14"}},
            ],
        )
        self.assertFragmentIn(
            response,
            [
                {"entity": "offer", "titles": {"raw": "promo price 15"}},
            ],
        )
        self.assertFragmentIn(
            response,
            [
                {"entity": "offer", "titles": {"raw": "promo price 16"}},
            ],
        )

        # n plus m
        self.assertFragmentIn(
            response,
            [
                {"entity": "offer", "titles": {"raw": "promo price 17"}},
            ],
        )
        self.assertFragmentIn(
            response,
            [
                {"entity": "offer", "titles": {"raw": "promo price 18"}},
            ],
        )
        self.assertFragmentIn(
            response,
            [
                {"entity": "offer", "titles": {"raw": "promo price 19"}},
            ],
        )

    def test_check_promo_list(self):
        response = self.report.request_json('place=promo_list&rids=213')
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "promo",
                }
            ]
            * 5,
            allow_different_len=False,
        )

        response = self.report.request_json('place=promo_list&rids=213&promo-check-min-price=5')
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "promo",
                }
            ]
            * 3,
            allow_different_len=False,
        )

    def test_n_offers(self):
        response = self.report.request_json('place=promo_list&rids=213&promo-check-min-price=5')
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "promo",
                }
            ]
            * 3,
            allow_different_len=False,
        )

        response = self.report.request_json(
            'place=promo_list&rids=213&promo-check-min-price=5&promo-check-min-price-n-offers=3'
        )
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "promo",
                }
            ],
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()
