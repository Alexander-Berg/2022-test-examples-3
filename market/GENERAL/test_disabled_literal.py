#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import BlueOffer, MarketSku, Offer
from core.testcase import TestCase, main
from market.idx.pylibrary.offer_flags.flags import DisabledFlags


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.offers += [
            Offer(
                title='disabledinindex1',
                disabled_flags=DisabledFlags.build_offer_disabled_flags([DisabledFlags.MARKET_STOCK]),
            ),
            Offer(
                title='disabledinindex2',
                disabled_flags=DisabledFlags.build_offer_disabled_flags([DisabledFlags.MARKET_IDX]),
            ),
            Offer(title='disabledinindex3'),
        ]

        cls.index.mskus += [
            MarketSku(
                title='disabledinindex4',
                sku=36372828,
                hyperid=36372828,
                blue_offers=[
                    BlueOffer(
                        title='disabledinindex4',
                        feedid=9,
                        disabled_flags=DisabledFlags.build_offer_disabled_flags([DisabledFlags.MARKET_STOCK]),
                    )
                ],
            ),
            MarketSku(
                title='disabledinindex5',
                sku=36372829,
                hyperid=36372829,
                blue_offers=[
                    BlueOffer(
                        title='disabledinindex5',
                        feedid=9,
                        disabled_flags=DisabledFlags.build_offer_disabled_flags([DisabledFlags.MARKET_IDX]),
                    )
                ],
            ),
            MarketSku(
                title='disabledinindex6',
                sku=36372830,
                hyperid=36372830,
                blue_offers=[BlueOffer(title='disabledinindex6', feedid=9)],
            ),
        ]

    def test_panther_relevance_reducer_for_disabled_docs(self):
        """Под флагом market_panther_rel_reducing_coef_for_disabled_docs=x(double) пантерная релевантность
        отключенных документов должна умножаться на x"""
        response = self.report.request_json(
            'place=prime&text=disabledinindex&debug=da&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'titles': {'raw': 'disabledinindex3'},
                        'debug': {'factors': {'DOC_REL': '20059'}},
                    },
                    {
                        'titles': {'raw': 'disabledinindex4'},
                        'debug': {'factors': {'DOC_REL': '20059'}},
                    },
                    {
                        'titles': {'raw': 'disabledinindex6'},
                        'debug': {'factors': {'DOC_REL': '20059'}},
                    },
                ]
            },
            allow_different_len=False,
            preserve_order=False,
        )

        response = self.report.request_json(
            'place=prime&text=disabledinindex&debug=da&rearr-factors=market_metadoc_search=no'
            ';market_panther_rel_reducing_coef_for_disabled_docs=0.01'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'titles': {'raw': 'disabledinindex3'},
                        'debug': {'factors': {'DOC_REL': '20059'}},
                    },
                    {
                        'titles': {'raw': 'disabledinindex4'},
                        'debug': {'factors': {'DOC_REL': '200'}},
                    },
                    {
                        'titles': {'raw': 'disabledinindex6'},
                        'debug': {'factors': {'DOC_REL': '20059'}},
                    },
                ]
            },
            allow_different_len=False,
            preserve_order=False,
        )


if __name__ == '__main__':
    main()
