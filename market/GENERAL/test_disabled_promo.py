#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import Offer, Promo, PromoType, Region, Shop


class T(TestCase):

    # MARKETOUT-10347
    @classmethod
    def prepare_promo_place(cls):
        cls.settings.enable_promo = False

        cls.index.shops += [
            Shop(fesh=1034701, priority_region=213),
        ]

        cls.index.regiontree += [
            Region(rid=213, name='Москва', region_type=Region.FEDERATIVE_SUBJECT),
            Region(rid=214, name='Республика Пепястан', region_type=Region.FEDERATIVE_SUBJECT),
        ]

        cls.index.offers += [
            Offer(
                title='promo test',
                fesh=1034701,
                hyperid=1034701,
                waremd5='offer103470101_waremd5',
                promo=Promo(promo_type=PromoType.N_PLUS_ONE, key='promo1034701_01_key000'),
            )
        ]

    def test_disabled_promo(self):
        response = self.report.request_json('place=promo&promoid=promo1034701_01_key000')
        self.assertFragmentNotIn(response, {'promos': [{}]})
        self.error_log.ignore('Cannot load promo details data from file')
        self.base_logs_storage.error_log.ignore('Cannot load promo details data from file')


if __name__ == '__main__':
    main()
