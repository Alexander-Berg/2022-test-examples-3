#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import Offer
from core.matcher import Absent, NotEmpty


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']
        cls.index.offers += [
            Offer(
                hyperid=14115324, title="kuvalda", fesh=720, price=342, waremd5='RcSMzi4tf73qGvxRx8atJg', is_smb=True
            ),
            Offer(hyperid=14115324, title="telefon", fesh=721, price=341, waremd5='RcSMzi4tf73qGvxRx8atJf'),
            Offer(
                hyperid=14115326,
                title="televizor",
                fesh=722,
                price=340,
                waremd5='RcSMzi4tf73qGvxRx8atJa',
                is_smb=True,
                has_url=False,
                is_cpc=True,
            ),
        ]

    def test_hidden_with_flag(self):
        for place in ['prime', 'productoffers']:
            response = self.report.request_json('place={}&hyperid=14115324'.format(place))
            self.assertFragmentIn(
                response,
                {
                    "total": 1,
                    "results": [
                        {
                            "entity": "offer",
                            "isSMB": False,
                        }
                    ],
                },
            )

            response = self.report.request_json(
                'place={}&hyperid=14115324&rearr-factors=market_hide_smb_offers=0'.format(place)
            )
            self.assertFragmentIn(
                response,
                {
                    "total": 2,
                    "results": [
                        {
                            "entity": "offer",
                            "isSMB": True,
                        },
                        {
                            "entity": "offer",
                            "isSMB": False,
                        },
                    ],
                },
                allow_different_len=False,
            )

        response = self.report.request_json('place=offerinfo&rids=0&regset=2&offerid=RcSMzi4tf73qGvxRx8atJg')
        self.assertFragmentNotIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "isSMB": True,
                    }
                ]
            },
        )

        response = self.report.request_json(
            'place=offerinfo&rids=0&regset=2&offerid=RcSMzi4tf73qGvxRx8atJg&rearr-factors=market_hide_smb_offers=0'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "isSMB": True,
                    }
                ]
            },
        )

        response = self.report.request_bs('place=parallel&text=kuvalda')
        self.assertFragmentNotIn(response, {"market_offers_wizard": NotEmpty()})

        response = self.report.request_bs('place=parallel&text=kuvalda&rearr-factors=market_hide_smb_offers=0')
        self.assertFragmentIn(response, {"market_offers_wizard": NotEmpty()})

    def test_filter(self):
        for place in ['prime', 'productoffers']:
            response = self.report.request_json(
                'place={}&hyperid=14115324&filter-smb=1&rearr-factors=market_hide_smb_offers=0'.format(place)
            )
            self.assertFragmentIn(
                response,
                {
                    "total": 1,
                    "results": [
                        {
                            "entity": "offer",
                            "isSMB": True,
                        }
                    ],
                },
            )

            response = self.report.request_json(
                'place={}&hyperid=14115324&rearr-factors=market_hide_smb_offers=0'.format(place)
            )
            self.assertFragmentIn(
                response,
                {
                    "total": 2,
                    "results": [
                        {
                            "entity": "offer",
                            "isSMB": True,
                        },
                        {
                            "entity": "offer",
                            "isSMB": False,
                        },
                    ],
                },
                allow_different_len=False,
            )

    def test_without_url(self):
        response = self.report.request_json(
            'place=prime&rids=0&regset=2&hyperid=14115324&rearr-factors=market_hide_smb_offers=0'
        )
        self.assertFragmentIn(response, {"entity": "offer", "isSMB": True, "withoutUrl": Absent()})

        response = self.report.request_json(
            'place=prime&rids=0&regset=2&hyperid=14115326&rearr-factors=market_hide_smb_offers=0'
        )
        self.assertFragmentIn(response, {"entity": "offer", "isSMB": True, "withoutUrl": True})


if __name__ == '__main__':
    main()
