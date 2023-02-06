#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import GLParam, MarketSku, Offer
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.mskus = [
            MarketSku(
                hid=100,
                title='SKU1',
                hyperid=100501,
                sku=100501,
                glparams=[GLParam(param_id=1, value=0), GLParam(param_id=2, value=0)],
            ),
            MarketSku(
                hid=100,
                title='SKU2',
                hyperid=100502,
                sku=100502,
                glparams=[GLParam(param_id=1, value=0), GLParam(param_id=2, value=1)],
            ),
        ]
        cls.index.offers = [
            Offer(
                hid=100,
                sku=100501,
                hyperid=100501,
                title='OFFER1',
                glparams=[GLParam(param_id=1, value=1), GLParam(param_id=2, value=1)],
            ),
            Offer(
                hid=100,
                sku=100502,
                hyperid=100502,
                title='OFFER2',
                glparams=[GLParam(param_id=1, value=1), GLParam(param_id=2, value=1)],
            ),
        ]

    def test_glfilters(self):

        response = self.report.request_json(
            "hid=100&"
            "text=OFFER1&"
            "place=prime&"
            "glfilter=1:1&"
            "rearr-factors=filter_offers_by_sku_params=0&"
            "&rearr-factors=market_metadoc_search=no"
        )
        self.assertFragmentIn(response, {'titles': {'raw': 'OFFER1'}})

        response = self.report.request_json(
            "hid=100&"
            "text=OFFER1&"
            "place=prime&"
            "glfilter=1:0&"
            "rearr-factors=filter_offers_by_sku_params=1&"
            "&rearr-factors=market_metadoc_search=no"
        )
        self.assertFragmentIn(response, {'titles': {'raw': 'OFFER1'}})

        response = self.report.request_json(
            "hid=100&"
            "text=OFFER1&"
            "place=prime&"
            "glfilter=1:1&"
            "rearr-factors=filter_offers_by_sku_params=1&"
            "&rearr-factors=market_metadoc_search=no"
        )
        self.assertFragmentNotIn(response, {'titles': {'raw': 'OFFER1'}})

        response = self.report.request_json(
            "hid=100&"
            "text=OFFER1&"
            "place=prime&"
            "glfilter=1:0&"
            "rearr-factors=filter_offers_by_sku_params=0&"
            "&rearr-factors=market_metadoc_search=no"
        )
        self.assertFragmentNotIn(response, {'titles': {'raw': 'OFFER1'}})

        response = self.report.request_json(
            "hid=100&"
            "text=OFFER&"
            "place=prime&"
            "glfilter=2:1&"
            "rearr-factors=filter_offers_by_sku_params=1&"
            "rearr-factors=market_early_pre_early_gl_filtering=1&"
            "rearr-factors=market_metadoc_search=no"
        )
        self.assertFragmentIn(response, {'totalOffers': 1, 'totalOffersBeforeFilters': 1})

        response = self.report.request_json(
            "hid=100&"
            "text=OFFER&"
            "place=prime&"
            "glfilter=2:1&"
            "rearr-factors=filter_offers_by_sku_params=1&"
            "rearr-factors=market_early_pre_early_gl_filtering=0&"
            "rearr-factors=market_metadoc_search=no"
        )
        self.assertFragmentIn(response, {'totalOffers': 1, 'totalOffersBeforeFilters': 2})

        response = self.report.request_json(
            "hid=100&"
            "text=OFFER&"
            "place=prime&"
            "glfilter=2:1&"
            "rearr-factors=filter_offers_by_sku_params=0&"
            "rearr-factors=market_early_pre_early_gl_filtering=1&"
            "rearr-factors=market_metadoc_search=no"
        )
        self.assertFragmentIn(response, {'totalOffers': 2, 'totalOffersBeforeFilters': 2})

        response = self.report.request_json(
            "hid=100&"
            "text=OFFER&"
            "place=prime&"
            "glfilter=2:1&"
            "rearr-factors=filter_offers_by_sku_params=0&"
            "rearr-factors=market_early_pre_early_gl_filtering=0&"
            "rearr-factors=market_metadoc_search=no"
        )
        self.assertFragmentIn(response, {'totalOffers': 2, 'totalOffersBeforeFilters': 2})


if __name__ == '__main__':
    main()
