#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import Region, HyperCategory, Shop, Model, Offer, Currency
from core.matcher import EmptyList, NotEmptyList


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_write_category_redirect_features=20']

        cls.index.regiontree += [
            Region(
                rid=1,
                name='Московская область',
                region_type=Region.FEDERATIVE_SUBJECT,
                children=[
                    Region(rid=213, name='Москва'),
                ],
            )
        ]

        cls.index.hypertree += [
            HyperCategory(hid=1, name='Телефоны'),
        ]

        cls.index.shops += [
            Shop(
                fesh=1,
                priority_region=213,
                name='Shop 1',
                currency=Currency.RUR,
            ),
        ]

        cls.index.models += [
            Model(
                hyperid=1,
                hid=1,
                title='Cisco phone',
                vendor_id=1,
            ),
            Model(hyperid=2, hid=1, title='Samsung Galaxy S10'),
            Model(hyperid=3, hid=1, title='Apple iPhone Xr'),
            Model(hyperid=4, hid=1, title='Old Mobile Phone'),
        ]

        cls.index.offers += [
            Offer(
                title='Cisco phone 1',
                price=100,
                hyperid=1,
                fesh=1,
                price_old=150,
                waremd5='qAwB74BChgyNFRxLsy3iyQ',
                randx=236,
            ),
            Offer(
                title='Cisco phone 2',
                price=200,
                hyperid=1,
                fesh=1,
                price_old=150,
                waremd5='IPK_gjsGpUmy7cLt_EPxtw',
                randx=235,
            ),
            Offer(title='Samsung Galaxy S10 offer', price=1000, hyperid=2, fesh=1),
        ]

        cls.speller.on_default_request().respond(originalText=None, fixedText=None)
        cls.speller.on_request(text='ipxone').respond(
            originalText='ip<fix>x</fix>one',
            fixedText='ip<fix>h</fix>one',
        )

    def test_categories_ranking(self):
        """
        If request has debug=1&analytics-debug-only=1 GET-argument only short debugging section with categories ranking should
        be in response.
        https://st.yandex-team.ru/MARKETOUT-29764
        """
        response = self.report.request_json(
            'place=prime&text=iphone&debug=1&analytics-debug-only=1&rearr-factors=market_report_add_sku_stats_touch_only=1'
        )
        self.assertFragmentIn(
            response,
            {
                'debug': {
                    'categories_ranking': NotEmptyList(),
                    'metasearch': {
                        'subrequests': [
                            'debug',
                            {
                                'categories_ranking': EmptyList(),
                            },
                        ],
                    },
                },
            },
            allow_different_len=False,
        )

        # Request with subrequests
        response = self.report.request_json(
            'place=prime&text=ipxone&debug=1&analytics-debug-only=1&rearr-factors=market_report_add_sku_stats_touch_only=1'
        )
        self.assertFragmentIn(
            response,
            {
                'debug': {
                    'categories_ranking': EmptyList(),
                    'metasearch': {
                        'subrequests': [
                            'debug',
                            {
                                'categories_ranking': EmptyList(),
                            },
                            'debug',
                            {
                                'categories_ranking': NotEmptyList(),
                                'metasearch': {
                                    'subrequests': [
                                        'debug',
                                        {
                                            'categories_ranking': EmptyList(),
                                        },
                                    ],
                                },
                            },
                        ],
                    },
                },
            },
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()
