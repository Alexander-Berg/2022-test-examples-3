#!/usr/bin/env python
# -*- coding: utf-8 -*-
import runner  # noqa

from core.types import MarketSku, MnPlace, Model, Offer
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare_serp(cls):
        cls.index.models = [
            Model(hyperid=1, vendor_id=1, hid=1, title='MODEL' + str(1)),
            Model(hyperid=2, vendor_id=2, hid=2, title='MODEL' + str(2)),
            Model(hyperid=3, vendor_id=3, hid=2, title='MODEL' + str(3)),
        ]

        cls.index.mskus += [
            MarketSku(sku=1, hyperid=1, vendor_id=1, hid=1, title='SKU' + str(1), ts=1),
            MarketSku(sku=2, hyperid=1, vendor_id=1, hid=1, title='SKU' + str(2), ts=2),
            MarketSku(sku=3, hyperid=1, vendor_id=1, hid=1, title='SKU' + str(3), ts=3),
            MarketSku(sku=4, hyperid=1, vendor_id=1, hid=1, title='SKU' + str(4), ts=4),
            MarketSku(sku=5, hyperid=2, vendor_id=2, hid=2, title='SKU' + str(5), ts=5),
            MarketSku(sku=6, hyperid=3, vendor_id=3, hid=2, title='SKU' + str(6), ts=6),
        ]

        cls.index.offers += [
            Offer(sku=1, price=1, hyperid=1, vendor_id=1, hid=1, title='OFFER' + str(1), ts=1),
            Offer(sku=2, price=2, hyperid=1, vendor_id=1, hid=1, title='OFFER' + str(2), ts=2),
            Offer(sku=3, price=3, hyperid=1, vendor_id=1, hid=1, title='OFFER' + str(3), ts=3),
            Offer(sku=4, price=4, hyperid=1, vendor_id=1, hid=1, title='OFFER' + str(4), ts=4),
            Offer(sku=5, price=5, hyperid=2, vendor_id=2, hid=2, title='OFFER' + str(5), ts=5),
            Offer(sku=6, price=6, hyperid=3, vendor_id=3, hid=2, title='OFFER' + str(6), ts=6),
        ]

        for place in [MnPlace.BASE_SEARCH, MnPlace.META_REARRANGE]:
            for ts in range(1, 7):
                cls.matrixnet.on_place(place, ts).respond(7 - ts)

    def test_serp(self):
        request = 'place=prime&text=SKU&use-default-offers=1&rearr-factors=market_metadoc_search=skus'

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {'search': {'results': [{'id': str(i)} for i in [1, 2, 3, 4, 5, 6]]}},
            preserve_order=True,
            allow_different_len=False,
        )

    def test_diversity_model(self):
        request = (
            'place=prime&allow-collapsing=0&pp=7&text=SKU&use-default-offers=1&rearr-factors='
            'market_metadoc_search=skus;'
            'market_diversity_rearrange_type=2;'
            'market_diversity_penalty_text=0.1;'
            'market_diversity_model_duplication_threshold_text=0.2'
        )

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {'search': {'results': [{'id': str(i)} for i in [1, 5, 6, 2, 3, 4]]}},
            preserve_order=True,
            allow_different_len=False,
        )

    def test_diversity_model_top5(self):
        request = (
            'place=prime&allow-collapsing=0&pp=7&text=SKU&use-default-offers=1&rearr-factors='
            'market_metadoc_search=skus;'
            'market_diversity_rearrange_type=2;'
            'market_diversity_penalty_text=0.1;'
            'market_diversity_model_duplication_threshold_text=0.2;'
            'market_diversity_top_size=5'
        )

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {'search': {'results': [{'id': str(i)} for i in [1, 5, 2, 3, 4, 6]]}},
            preserve_order=True,
            allow_different_len=False,
        )

    def test_diversity_hid(self):
        request = (
            'place=prime&allow-collapsing=0&pp=7&text=SKU&use-default-offers=1&rearr-factors='
            'market_metadoc_search=skus;'
            'market_diversity_rearrange_type=2;'
            'market_diversity_penalty_text=0.1;'
            'market_diversity_categ_duplication_threshold_text=0.2'
        )

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {'search': {'results': [{'id': str(i)} for i in [1, 5, 2, 6, 3, 4]]}},
            preserve_order=True,
            allow_different_len=False,
        )

    def test_diversity_vendor(self):
        request = (
            'place=prime&allow-collapsing=0&pp=7&text=SKU&use-default-offers=1&rearr-factors='
            'market_metadoc_search=skus;'
            'market_diversity_rearrange_type=2;'
            'market_diversity_penalty_text=0.1;'
            'market_diversity_vendor_duplication_threshold_text=0.2'
        )

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {'search': {'results': [{'id': str(i)} for i in [1, 5, 6, 2, 3, 4]]}},
            preserve_order=True,
            allow_different_len=False,
        )

    def test_diversity_not_used_for_user_sort(self):
        request = (
            'place=prime&allow-collapsing=0&pp=7&how=aprice&text=SKU&use-default-offers=1&rearr-factors='
            'market_metadoc_search=skus;'
            'market_diversity_rearrange_type=2;'
            'market_diversity_penalty_text=0.1;'
            'market_diversity_model_duplication_threshold_text=0.2'
        )

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {'search': {'results': [{'id': str(i)} for i in [1, 2, 3, 4, 5, 6]]}},
            preserve_order=True,
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()
