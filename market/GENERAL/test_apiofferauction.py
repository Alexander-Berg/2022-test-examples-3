#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import Model, Offer, Shop


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.shops += [Shop(fesh=200)]

        cls.index.models += [Model(hyperid=100, title="model")]

        cls.index.shops += [Shop(fesh=3333, cpa=Shop.CPA_REAL)]

        cls.index.offers += [Offer(hyperid=100, fesh=3333, waremd5="FRNxd7-S67VQQyexo4gQqA", cpa=Offer.CPA_REAL)]

    def test_simple_request(self):
        response = self.report.request_xml(
            'place=api_offerauction&hyperid=100&api_shopid_to_rate=200&api_offerid_to_rate=FRNxd7-S67VQQyexo4gQqA'
        )
        self.assertFragmentIn(
            response,
            '''
            <offers numoffers="1">
            </offers>
        ''',
        )
        """Проверяется, что общее количество для показа = total"""
        self.assertFragmentIn(
            response,
            '<search_results adult="*" book-now-detected="*" sales-detected="*" shop-outlets="*" shops="*" total="1"></search_results>',
        )
        self.access_log.expect(total_renderable='1')

    def test_missing_pp(self):
        response = self.report.request_xml(
            'place=api_offerauction&hyperid=100&api_shopid_to_rate=200&api_offerid_to_rate=FRNxd7-S67VQQyexo4gQqA&ip=127.0.0.1',
            strict=False,
            add_defaults=False,
        )
        self.error_log.expect('Some client has not set PP value. Find and punish him violently').once()
        self.assertEqual(500, response.code)


if __name__ == '__main__':
    main()
