#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Model, Offer, RegionalModel, Shop
from core.testcase import TestCase, main

from unittest import skip


class T(TestCase):
    @classmethod
    def prepare_cpa_only_in_parallel(cls):
        cls.index.regional_models += [
            RegionalModel(hyperid=101, rids=[54], has_good_cpa=True, has_cpa=True),
            RegionalModel(hyperid=102, rids=[54], has_good_cpa=True, has_cpa=True),
            RegionalModel(hyperid=103, rids=[54], has_good_cpa=True, has_cpa=True),
        ]

        cls.index.models += [
            Model(hyperid=101, hid=91122, title="model 101"),
            Model(hyperid=102, hid=91011, title="model 102"),
            Model(hyperid=103, hid=91012, title="model 103"),
        ]

        cls.index.shops += [
            Shop(fesh=1, regions=[54], cpa=Shop.CPA_REAL, name='CPA Магазин 1'),
            Shop(fesh=2, regions=[54], cpa=Shop.CPA_NO, cpc=Shop.CPC_REAL, name='CPC Магазин 2'),
            Shop(fesh=3, regions=[54], cpa=Shop.CPA_REAL, cpc=Shop.CPC_REAL, name='CPA CPC Магазин 3'),
        ]

        cls.index.offers += [
            Offer(fesh=1, title="CPA офер 1-1", cpa=Offer.CPA_REAL, hyperid=101, hid=91122),
            Offer(fesh=1, title="CPC офер 1-2", cpa=Offer.CPA_NO, hyperid=101, hid=91122),
            Offer(fesh=2, title="CPA офер 3-1", cpa=Offer.CPA_REAL, hyperid=102, hid=91011),
            Offer(fesh=2, title="CPC офер 3-2", cpa=Offer.CPA_NO, is_cpc=True, hyperid=102, hid=91011),
            Offer(fesh=3, title="CPA офер 4-1", cpa=Offer.CPA_REAL, hyperid=103, hid=91012),
            Offer(fesh=3, title="CPC офер 4-2", cpa=Offer.CPA_NO, is_cpc=True, hyperid=103, hid=91012),
        ]

    @skip('MARKETOUT-41660, из CPA-only убраны HIDs, возможно стоит переделать тест на HardHIDs')
    def test_cpa_only_in_parallel(self):
        response = self.report.request_json(
            'place=parallel&show-urls=external%2Ccpa&text=офер&rids=54&rearr-factors=market_parallel_cpa_only_enabled=1'
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": [
                    {
                        "showcase": {
                            "items": [
                                {"title": {"text": {"__hl": {"text": "CPA офер 1-1"}}}},
                                {"title": {"text": {"__hl": {"text": "CPA офер 4-1"}}}},
                            ]
                        }
                    }
                ]
            },
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()
