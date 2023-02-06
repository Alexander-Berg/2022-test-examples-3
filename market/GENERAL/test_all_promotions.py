#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Model, Offer, Promo, PromoType, Region, Shop
from core.testcase import TestCase, main

from datetime import datetime


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.regiontree += [
            Region(
                rid=1,
                region_type=Region.FEDERATIVE_SUBJECT,
                children=[Region(rid=7, region_type=Region.FEDERATIVE_SUBJECT)],
            ),
            Region(rid=10, region_type=Region.FEDERATIVE_SUBJECT),
            Region(rid=43, region_type=Region.FEDERATIVE_SUBJECT),
        ]
        cls.index.shops += [Shop(fesh=774, priority_region=1, regions=[1, 7], cpa=Shop.CPA_REAL)]
        cls.index.models += [
            Model(hyperid=401, title='philips model 1'),
            Model(hyperid=402, title='philips model 2'),
            Model(hyperid=403, title='philips model 3'),
            Model(hyperid=404, title='philips model 4'),
        ]
        cls.index.offers += [
            Offer(
                title='offer_for_philips promo 1',
                feedid=101,
                offerid=201,
                fesh=774,
                hyperid=401,
                cpa=Offer.CPA_REAL,
                price=100,
                price_old=200,
            ),
            Offer(
                title='offer_for_philips promo 2',
                feedid=101,
                offerid=202,
                fesh=774,
                hyperid=403,
                cpa=Offer.CPA_REAL,
                price=40,
            ),
            Offer(
                title='offer_for_philips promo 3', feedid=101, offerid=203, fesh=774, hyperid=402, cpa=Offer.CPA_REAL
            ),
            Offer(
                title='offer_for_philips model 3 promo 1',
                fesh=774,
                hyperid=403,
                waremd5='Y9_iwgA17yd3FCI0LRhC_w',
                price=50,
                price_old=100,
                promo=Promo(
                    promo_type=PromoType.N_PLUS_ONE,
                    key='yMpCOKC5I4INzFCab3WEmw',
                    end_date=datetime(1986, 1, 1),
                    required_quantity=3,
                    free_quantity=34,
                ),
            ),
            Offer(
                title='offer_for_philips model 3 promo 2',
                fesh=774,
                hyperid=403,
                waremd5='Z9_iwgA17yd3FCI0LRhC_w',
                price=20,
                promo=Promo(
                    promo_type=PromoType.N_PLUS_ONE,
                    key='zMpCOKC5I4INzFCab3WEmw',
                    start_date=datetime(1984, 1, 1),
                    required_quantity=3,
                    free_quantity=34,
                ),
            ),
            Offer(
                title='offer_for_philips model 4 promo 3',
                fesh=774,
                hyperid=404,
                waremd5='X9_qazA17yd3FCI0LRhC_w',
                promo=Promo(
                    promo_type=PromoType.N_PLUS_ONE,
                    key='xMpCOKC5I4INzFCab3WEma',
                    start_date=datetime(2000, 1, 1),
                    end_date=datetime(2001, 1, 1),
                    required_quantity=3,
                    free_quantity=34,
                ),
            ),
            Offer(
                title='offer_for_philips model 3 promo 4',
                fesh=774,
                hyperid=403,
                waremd5='Z9_iwgA17ydaaaI0LRhC_w',
                price=10,
                promo=Promo(
                    promo_type=PromoType.N_PLUS_ONE, key='zMpCOaaaa4INzFCab3WEmw', required_quantity=3, free_quantity=34
                ),
            ),
        ]

    def test_prime(self):
        '''проверяем place=prime
        смотрим что у нас без флага force-promo-offer-filter и без схлопывания есть модели
        а также что с этим флагом и со схлопыванием есть модели
        '''

        filters_to_test = ['filter-promo-or-discount']
        for flt in filters_to_test:
            for collapse_and_forcefilter in [0, 1]:
                response = self.report.request_json(
                    "place=prime&allow-collapsing={}&text=philips&rids=1&{}=1&force-promo-offer-filter={}".format(
                        collapse_and_forcefilter, flt, collapse_and_forcefilter
                    )
                )
                # но должно найтись filter-promo-or-discount тк есть скидка
                self.assertFragmentIn(
                    response,
                    {
                        "results": [
                            {
                                'entity': 'product',
                                "id": 401,
                            }
                        ]
                    },
                )
                # не должно найтись, потому что модель обычная и промо нет
                self.assertFragmentNotIn(
                    response,
                    {
                        "results": [
                            {
                                'entity': 'product',
                                "id": 402,
                            }
                        ]
                    },
                )

                # должно найтись промо-фильтром, потому что есть промо-офер
                # должно найтись промо-скидочным, потому что есть промо-офер
                self.assertFragmentIn(
                    response,
                    {
                        "results": [
                            {
                                'entity': 'product',
                                "id": 403,
                            }
                        ]
                    },
                )

    def test_productoffers(self):
        '''проверяем place=productoffers'''
        filters_to_test = ['filter-promo-or-discount']
        for flt in filters_to_test:
            response = self.report.request_json('place=productoffers&hyperid=402&rids=1&%s=1' % flt)
            self.assertFragmentNotIn(
                response,
                {
                    "results": [
                        {
                            'entity': 'offer',
                        }
                    ]
                },
            )
            response = self.report.request_json('place=productoffers&hyperid=403&rids=1&%s=1' % flt)
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            'entity': 'offer',
                            'model': {'id': 403},
                        }
                    ]
                },
            )

            response = self.report.request_json('place=productoffers&hyperid=404&rids=1&%s=1' % flt)
            self.assertFragmentNotIn(
                response,
                {
                    "results": [
                        {
                            'entity': 'offer',
                            'model': {'id': 404},
                        }
                    ]
                },
            )

    def test_wo_filter(self):
        '''Проверяем, что с отключенным фильтром ничего не пропадает'''

        response = self.report.request_json(
            'place=prime&text=philips&rids=1&rearr-factors=market_do_not_split_promo_filter=1'
        )

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        'entity': 'product',
                        "id": 401,
                    }
                ]
            },
        )

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        'entity': 'product',
                        "id": 402,
                    }
                ]
            },
        )

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        'entity': 'product',
                        "id": 403,
                    }
                ]
            },
        )
        self.assertFragmentIn(response, {"filters": [{"id": "filter-promo-or-discount"}]})

        response = self.report.request_json('place=productoffers&hyperid=402&rids=1')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        'entity': 'offer',
                        'model': {'id': 402},
                    }
                ]
            },
        )
        response = self.report.request_json('place=productoffers&hyperid=403&rids=1')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        'entity': 'offer',
                        'model': {'id': 403},
                    }
                ]
            },
        )

    def test_market_disable_promo_exp(self):
        """
        Запрашиваем оффера среди которых есть и с промкой, и со скидкой.
        Проверяем что без rearr-factors=market_disable_promo=0 у нас появляется только фильтр discount_and_promo
        или только фильтр promo-type, в зависимости от market_do_not_split_promo_filter
        Без флага или с rearr-factors=market_disable_promo=1 у нас появляется только фильтр discount
        """
        for expflag in (
            "&rearr-factors=market_disable_promo=1;market_enable_common_promo_type_filter=0",
            "&rearr-factors=market_disable_promo=1;market_do_not_split_promo_filter=1;market_enable_common_promo_type_filter=0",
        ):
            request = "place=prime&text=offer_for_philips&rids=0" + expflag
            response = self.report.request_json(request)
            self.assertFragmentIn(response, {"filters": [{"id": "filter-discount-only"}]})
            self.assertFragmentNotIn(response, {"filters": [{"id": "filter-promo-or-discount"}]})
            self.assertFragmentNotIn(response, {"filters": [{"id": "promo-type"}]})

        request = "place=prime&text=offer_for_philips&rids=0&rearr-factors=market_enable_common_promo_type_filter=0"
        response = self.report.request_json(request)
        self.assertFragmentNotIn(response, {"filters": [{"id": "filter-discount-only"}]})
        self.assertFragmentNotIn(response, {"filters": [{"id": "filter-promo-or-discount"}]})
        self.assertFragmentIn(response, {"filters": [{"id": "promo-type"}]})

        request = "place=prime&text=offer_for_philips&rids=0&rearr-factors=market_do_not_split_promo_filter=1;market_enable_common_promo_type_filter=0"
        response = self.report.request_json(request)
        self.assertFragmentNotIn(response, {"filters": [{"id": "filter-discount-only"}]})
        self.assertFragmentNotIn(response, {"filters": [{"id": "promo-type"}]})
        self.assertFragmentIn(response, {"filters": [{"id": "filter-promo-or-discount"}]})


if __name__ == '__main__':
    main()
