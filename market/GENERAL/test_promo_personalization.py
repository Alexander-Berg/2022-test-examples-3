#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.bigb import WeightedValue, BigBKeyword
from core.matcher import NoKey
from core.types import HyperCategory, MnPlace, Model, Offer, Shop
from core.testcase import TestCase, main

USER_PROFILE = [
    BigBKeyword(
        id=BigBKeyword.GENDER,
        weighted_uint_values=[
            WeightedValue(value=BigBKeyword.GENDER_MALE, weight=920000),
            WeightedValue(value=BigBKeyword.GENDER_FEMALE, weight=80000),
        ],
    ),
    BigBKeyword(
        id=BigBKeyword.AGE6,
        weighted_uint_values=[
            WeightedValue(value=BigBKeyword.AGE6_0_17, weight=0),
            WeightedValue(value=BigBKeyword.AGE6_18_24, weight=100000),
            WeightedValue(value=BigBKeyword.AGE6_25_34, weight=500000),
            WeightedValue(value=BigBKeyword.AGE6_35_44, weight=300000),
            WeightedValue(value=BigBKeyword.AGE6_45_54, weight=100000),
            WeightedValue(value=BigBKeyword.AGE6_55_99, weight=0),
        ],
    ),
    BigBKeyword(
        id=BigBKeyword.REVENUE5,
        weighted_uint_values=[
            WeightedValue(value=BigBKeyword.REVENUE5_A, weight=100000),
            WeightedValue(value=BigBKeyword.REVENUE5_B1, weight=100000),
            WeightedValue(value=BigBKeyword.REVENUE5_B2, weight=500000),
            WeightedValue(value=BigBKeyword.REVENUE5_C1, weight=300000),
            WeightedValue(value=BigBKeyword.REVENUE5_C2, weight=0),
        ],
    ),
]


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.shops += [
            Shop(fesh=1, name='barbershop', priority_region=213, regions=[225]),
        ]

        cls.index.hypertree += [
            HyperCategory(hid=1),
            HyperCategory(hid=2),
        ]

        cls.index.models += [
            Model(hyperid=1, hid=1, title="big bagels", ts=1),
            Model(hyperid=2, hid=1, title="smol bagels", ts=2),
            Model(hyperid=3, hid=2, title="smol croissant", ts=3),
            Model(hyperid=4, hid=1, title="huge bagels", ts=4),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1).respond(0.3)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2).respond(0.2)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3).respond(0.1)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 4).respond(0.12)

        cls.index.offers += [
            Offer(hyperid=1, title='bagel 1', fesh=1, hid=1),
            Offer(hyperid=2, title='bagel 2', fesh=1, hid=1),
            Offer(hyperid=3, title='croissant 1', fesh=1, hid=2),
            Offer(hyperid=4, title='bagel 3', fesh=1, hid=1),
        ]

        cls.bigb.on_request(yandexuid='1', client='merch-machine').respond(keywords=USER_PROFILE)

    def test_prime_promo_personalization_formula(self):
        ''' 'Если ходим в prime c has_promo c флагом use_promo_personalization_prime то нужно использовать MNA_promo_personalization'''
        promo_hub_pps = [135, 335, 535, 635]
        for pp in promo_hub_pps:
            base = 'place=prime&hid=1&rids=213&x-yandex-icookie=1&pp=%s&debug=1' % pp
            flags = 'rearr-factors=use_promo_personalization_prime=0&x-yandex-icookie=1'

            response = self.report.request_json(base + '&' + flags)
            self.assertFragmentNotIn(response, 'Using MatrixNet formula: MNA_promo_personalization')
            self.assertFragmentNotIn(response, {"rankedWith": "MNA_promo_personalization"})

            response = self.report.request_json(base)
            self.assertFragmentIn(response, 'Using MatrixNet formula: MNA_promo_personalization')
            self.assertFragmentIn(response, {"rankedWith": "MNA_promo_personalization"})

    def test_prime_promo_personalization_factors(self):
        ''' 'Если ходим в prime c has_promo c флагом use_promo_personalization_prime то нужно выставлять факторы'''
        promo_hub_pps = [135, 335, 535, 635]
        for pp in promo_hub_pps:
            base = 'place=prime&hid=1&rids=213&x-yandex-icookie=1&yandexuid=1&pp=%s&debug=1' % pp
            flags = 'rearr-factors=use_promo_personalization_prime=1&x-yandex-icookie=1'

            response = self.report.request_json(base + '&' + flags)
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {
                                "debug": {
                                    "factors": {
                                        "USER_AGE6_0": NoKey("USER_AGE6_0"),
                                        "USER_AGE6_1": "100000",
                                        "USER_AGE6_2": "500000",
                                        "USER_AGE6_3": "300000",
                                        "USER_AGE6_4": "100000",
                                        "USER_AGE6_5": NoKey("USER_AGE6_0"),
                                        "USER_REVENUE5_0": "100000",
                                        "USER_REVENUE5_1": "100000",
                                        "USER_REVENUE5_2": "500000",
                                        "USER_REVENUE5_3": "300000",
                                        "USER_REVENUE5_4": NoKey("USER_REVENUE5_4"),
                                        "USER_GENDER_MALE": "920000",
                                        "USER_GENDER_FEMALE": "80000",
                                    }
                                }
                            }
                        ]
                    }
                },
            )

    def test_prime_promo_rearrangement(self):
        ''' 'Если ранжируем с помощью MNA_promo_personalization то нам нужно разряжать офера из одной категории'''
        promo_hub_pps = [135, 335, 535, 635]
        for pp in promo_hub_pps:
            base = 'place=prime&hid=1,2&has-promo=1&x-yandex-icookie=1&pp=%s' % pp
            base_flags = 'rearr-factors=market_hide_regional_delimiter=1'

            # request without rearrangement flags - documents arranged by formula values
            no_rearrange_requests = [
                base + '&' + base_flags + ';use_promo_personalization_rearrangement_less_sparsed=0',
                base + '&' + base_flags + ';use_promo_personalization_rearrangement_less_sparsed=0',
            ]
            for request in no_rearrange_requests:
                response = self.report.request_json(request)
                self.assertFragmentIn(
                    response,
                    {
                        'results': [
                            {'entity': 'product', 'id': 1},
                            {'entity': 'product', 'id': 2},
                            {'entity': 'product', 'id': 4},
                            {'entity': 'product', 'id': 3},
                        ]
                    },
                    allow_different_len=False,
                    preserve_order=True,
                )

            # add flags - models in same category should be thinned
            flags = (
                base_flags
                + ';use_promo_personalization_rearrangement=1;use_promo_personalization_rearrangement_less_sparsed=0'
            )
            response = self.report.request_json(base + '&' + flags)
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {'entity': 'product', 'id': 1},
                        {'entity': 'product', 'id': 3},
                        {'entity': 'product', 'id': 2},
                        {'entity': 'product', 'id': 4},
                    ]
                },
                allow_different_len=False,
                preserve_order=True,
            )

            # add flags - models should be rearranged by formula score_of_model^(1.1^how_much_time_this_cat_has_been_shown_already)
            flags = base_flags + ';use_promo_personalization_rearrangement_less_sparsed=1'
            response = self.report.request_json(base + '&' + flags)
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {'entity': 'product', 'id': 1},
                        {'entity': 'product', 'id': 2},
                        {'entity': 'product', 'id': 3},
                        {'entity': 'product', 'id': 4},
                    ]
                },
                allow_different_len=False,
                preserve_order=True,
            )


if __name__ == '__main__':
    main()
