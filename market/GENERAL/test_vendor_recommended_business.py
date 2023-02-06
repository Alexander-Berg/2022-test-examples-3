#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import Offer
from core.matcher import Absent


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']
        cls.index.offers += [
            Offer(hyperid=14115323, title="abc", fesh=719, price=719),
            Offer(
                hyperid=14115324,
                title="def",
                fesh=720,
                price=720,
                is_vendor_recommended=True,
                vendor_recommended_business_datasource=15,
            ),
            Offer(
                hyperid=14115324,
                title="ghi",
                fesh=721,
                price=721,
                is_vendor_official=True,
                vendor_recommended_business_datasource=25,
            ),
            Offer(
                hyperid=14115325,
                title="jkl",
                fesh=722,
                price=722,
                is_vendor_recommended=True,
                is_vendor_official=True,
                vendor_recommended_business_datasource=35,
            ),
            Offer(
                hyperid=12345,
                title="real iphone 1",
                fesh=723,
                price=723,
                is_vendor_recommended=True,
                has_choosy_vendor=True,
            ),
            Offer(
                hyperid=12345,
                title="real iphone 2",
                fesh=724,
                price=724,
                is_vendor_official=True,
                has_choosy_vendor=True,
            ),
            Offer(hyperid=12345, title="grey shop iphone", fesh=725, price=725, has_choosy_vendor=True),
        ]

    def test_no_param(self):
        response = self.report.request_json('place=prime&hyperid=14115323')
        self.assertFragmentIn(
            response,
            {
                "total": 1,
                "results": [{"entity": "offer", "shop": {"id": 719}, "vendorRecommendedBusiness": Absent()}],
            },
        )
        self.click_log.expect(price=719, vnd_recommended=0, vnd_official=0)

    def test_one_param(self):
        response = self.report.request_json('place=prime&hyperid=14115324')
        self.assertFragmentIn(
            response,
            {
                "total": 2,
                "results": [
                    {
                        "entity": "offer",
                        "shop": {"id": 720},
                        "vendorRecommendedBusiness": {"isRecommended": True, "isOfficial": False},
                    },
                    {
                        "entity": "offer",
                        "shop": {"id": 721},
                        "vendorRecommendedBusiness": {"isRecommended": False, "isOfficial": True},
                    },
                ],
            },
        )
        self.click_log.expect(price=720, vnd_recommended=1, vnd_official=0, vnd_datasource=15)
        self.click_log.expect(price=721, vnd_recommended=0, vnd_official=1, vnd_datasource=25)

    def test_two_params(self):
        response = self.report.request_json('place=prime&hyperid=14115325')
        self.assertFragmentIn(
            response,
            {
                "total": 1,
                "results": [
                    {
                        "entity": "offer",
                        "shop": {"id": 722},
                        "vendorRecommendedBusiness": {"isRecommended": False, "isOfficial": True},
                    }
                ],
            },
        )
        self.click_log.expect(price=722, vnd_recommended=1, vnd_official=1, vnd_datasource=35)

    def test_choosy_vendor(self):
        '''Проверяем, что при выключенном show_offers_from_not_authorized_shops для привередливых вендоров
        показываются только оффера из авторизованных магазинов
        '''

        def gen_req(flag=None):
            req = 'place=productoffers&hyperid=12345&debug=1'
            if flag is not None:
                req += '&rearr-factors=show_offers_from_not_authorized_shops={}'.format(flag)
            return req

        # без выключенного флага показываются все оффера айфона
        for flag in (None, '1'):
            response = self.report.request_json(gen_req(flag))
            self.assertFragmentIn(
                response,
                {
                    'total': 3,
                    'results': [
                        {
                            'entity': 'offer',
                            'shop': {'id': 723},
                            'vendorRecommendedBusiness': {
                                'isRecommended': True,
                                'isOfficial': False,
                            },
                        },
                        {
                            'entity': 'offer',
                            'shop': {'id': 724},
                            'vendorRecommendedBusiness': {
                                'isRecommended': False,
                                'isOfficial': True,
                            },
                        },
                        {
                            'entity': 'offer',
                            'shop': {'id': 725},
                            'vendorRecommendedBusiness': Absent(),
                        },
                    ],
                },
                allow_different_len=False,
            )

        # с выключенным флагом оффер из не авторизованного магазина скрывается
        response = self.report.request_json(gen_req('0'))
        self.assertFragmentIn(
            response,
            {
                'total': 2,
                'results': [
                    {
                        'shop': {'id': 723},
                    },
                    {
                        'shop': {'id': 724},
                    },
                ],
            },
            allow_different_len=False,
        )
        self.assertFragmentIn(
            response,
            {
                'filters': {
                    'NOT_AUTHORIZED_SHOP': 1,
                }
            },
        )


if __name__ == '__main__':
    main()
