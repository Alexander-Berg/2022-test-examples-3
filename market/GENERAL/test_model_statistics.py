#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    Model,
    Offer,
    Region,
    Shop,
)
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare_force_fesh_filter(cls):
        cls.index.regiontree += [Region(rid=2, name='Питер'), Region(rid=213, name='Нерезиновая')]
        cls.index.shops += [
            Shop(fesh=3, priority_region=213, regions=[225], cpa=Shop.CPA_REAL, cpc=Shop.CPC_NO),
            Shop(fesh=4, priority_region=213, regions=[225], cpa=Shop.CPA_REAL, cpc=Shop.CPC_NO),
        ]

        cls.index.models += [
            Model(
                hid=13,
                hyperid=1400,
                vbid=120,
                vendor_id=27,
                datasource_id=1,
            ),
        ]

        cls.index.offers += [
            Offer(
                hid=13,
                title='market 4.0 cpa',
                waremd5='VkjX-08eqaaf0Q_MrNfQBw',
                hyperid=1400,
                fesh=3,
                cpa=Offer.CPA_REAL,
            ),
            Offer(
                hid=13,
                title='market 4.0 cpa',
                waremd5='q-u3w59LXka7z6srVl7zKw',
                hyperid=1400,
                fesh=4,
                cpa=Offer.CPA_REAL,
            ),
            Offer(
                hid=13,
                title='market 4.0 cpa',
                waremd5='p-u3w59LXka7z6srVl7zKw',
                hyperid=1400,
                fesh=4,
                cpa=Offer.CPA_REAL,
            ),
        ]

    def test_force_fesh_filter_off(self):
        """
        проверяем, что с &market-force-business-id=0 статистика считается по всем магазинам
        """
        response = self.report.request_json('place=model_statistics&modelid=1400&market-force-business-id=0&fesh=3')

        self.assertFragmentIn(
            response,
            {
                'search': {
                    'modelResults': [
                        {
                            'modelId': 1400,
                            'count': 3,
                        }
                    ],
                },
            },
            allow_different_len=True,
        )

    def test_force_fesh_filter_on_3(self):
        """
        проверяем, что с &market-force-business-id=1 статистика считается только по магазину 3
        """
        response = self.report.request_json('place=model_statistics&modelid=1400&market-force-business-id=1&fesh=3')

        self.assertFragmentIn(
            response,
            {
                'search': {
                    'modelResults': [
                        {
                            'modelId': 1400,
                            'count': 1,
                        }
                    ],
                },
            },
            allow_different_len=True,
        )

    def test_force_fesh_filter_on_4(self):
        """
        проверяем, что с &market-force-business-id=1 статистика считается только по магазину 4
        """
        response = self.report.request_json('place=model_statistics&modelid=1400&market-force-business-id=1&fesh=4')

        self.assertFragmentIn(
            response,
            {
                'search': {
                    'modelResults': [
                        {
                            'modelId': 1400,
                            'count': 2,
                        }
                    ],
                },
            },
            allow_different_len=True,
        )

    def test_force_fesh_filter_on_5(self):
        """
        проверяем, что с &market-force-business-id=1 статистика считается только по магазину 5 (которого нет)
        """
        response = self.report.request_json('place=model_statistics&modelid=1400&market-force-business-id=1&fesh=5')

        self.assertFragmentIn(
            response,
            {
                'search': {
                    'modelResults': [
                        {
                            'modelId': 1400,
                            'count': 0,
                        }
                    ],
                },
            },
            allow_different_len=True,
        )

    def test_force_fesh_filter_on_345(self):
        """
        проверяем, что с &market-force-business-id=1 статистика считается только по магазинам 3, 4 и 5
        """
        response = self.report.request_json(
            'place=model_statistics&modelid=1400&market-force-business-id=1&fesh=3&fesh=4&fesh=5'
        )

        self.assertFragmentIn(
            response,
            {
                'search': {
                    'modelResults': [
                        {
                            'modelId': 1400,
                            'count': 3,
                        }
                    ],
                },
            },
            allow_different_len=True,
        )


if __name__ == '__main__':
    main()
