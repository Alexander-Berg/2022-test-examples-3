#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.matcher import Wildcard, LikeUrl
from core.testcase import TestCase, main
from core.types import Model, MarketSku, BlueOffer


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.models += [Model(hyperid=1)]
        cls.index.mskus += [
            MarketSku(
                hyperid=1,
                sku=2,
                blue_offers=[
                    BlueOffer(
                        price=100,
                        feedid=3,
                        offerid='blue.offer.1.1',
                        waremd5='xMpCOKC5I4INzFCab3WEmQ',
                    ),
                ],
            ),
        ]

    def test_return_bundle_url_in_defaultoffer_place(self):
        response = self.report.request_json(
            'place=defaultoffer&rgb=blue&hyperid=1&show_urls=cpa'
            '&rearr-factors=market_alice_return_bundle_urls=http://my.beru.ru:12345,456'
        )
        expected_url = LikeUrl.of(
            'http://my.beru.ru:12345/bundle/2?schema=type,objId,count'
            '&data=offer,xMpCOKC5I4INzFCab3WEmQ,1&fromTurbo=1&clid=456',
            ignore_len=True,
            ignore_params=["cpc"],
            fail_on_unexpected=True,
        )
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'sku': '2',
                'urls': {
                    'direct': expected_url,
                    'cpa': Wildcard('/redir/*'),
                },
            },
        )

    def test_return_bundle_url_in_sku_offers_place(self):
        response = self.report.request_json(
            'place=sku_offers&rgb=blue&market-sku=2&show_urls=cpa'
            '&rearr-factors=market_alice_return_bundle_urls=http://my.beru.ru:12345,456'
        )
        expected_url = LikeUrl.of(
            'http://my.beru.ru:12345/bundle/2?schema=type,objId,count'
            '&data=offer,xMpCOKC5I4INzFCab3WEmQ,1&fromTurbo=1&clid=456',
            ignore_len=True,
            ignore_params=["cpc"],
            fail_on_unexpected=True,
        )
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'sku': '2',
                'urls': {
                    'direct': expected_url,
                    'cpa': Wildcard('/redir/*'),
                },
            },
        )


if __name__ == '__main__':
    main()
