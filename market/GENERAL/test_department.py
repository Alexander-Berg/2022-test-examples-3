#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    Currency,
    HyperCategory,
    HyperCategoryType,
    Model,
    Offer,
    Shop,
    NavCategory,
)
from core.testcase import TestCase, main
from core.types.sku import MarketSku, BlueOffer


Model1 = 264711001


class _Offers(object):
    sku1_offer1 = BlueOffer(
        price=1,
        feedid=6,
        offerid='blue.offer.1.1',
        waremd5='Sku1Price5-IiLVm1Goleg',
        randx=2,
        cpa=Offer.CPA_REAL,
        fesh=1,
    )
    sku2_offer1 = BlueOffer(
        price=5,
        feedid=4,
        offerid='blue.offer.2.1',
        waremd5='Sku2Price5-IiLVm1Goleg',
        randx=1,
        cpa=Offer.CPA_REAL,
        fesh=1,
    )


class T(TestCase):
    @classmethod
    def prepare_wares(cls):
        cls.index.hypertree += [
            HyperCategory(
                hid=264711,
                name='department1',
                children=[
                    HyperCategory(
                        hid=264712,
                        name='category1',
                        children=[
                            HyperCategory(hid=264714, output_type=HyperCategoryType.GURU),
                        ],
                    ),
                ],
            ),
        ]

        cls.index.navtree += [
            NavCategory(
                nid=100500,
                hid=264711,
                children=[
                    NavCategory(hid=100501, nid=264712),
                ],
            )
        ]

        cls.index.shops += [
            Shop(
                fesh=1,
                priority_region=213,
                name='test_shop_1',
                currency=Currency.RUR,
                pickup_buckets=[5001],
            ),
        ]

        cls.index.models += [
            Model(hyperid=Model1, title='Яндекс.Поиск', hid=264711),
        ]

        cls.index.mskus += [
            MarketSku(
                title="разработчик 1",
                hyperid=Model1,
                sku=264711001,
                waremd5="Sku1-wdDXWsIiLVm1goleg",
                blue_offers=[_Offers.sku1_offer1, _Offers.sku2_offer1],
                randx=5,
            ),
        ]

    def test_department_for_offersinfo(self):
        request = 'place=offerinfo&offerid=Sku1Price5-IiLVm1Goleg&show-urls=cpa,external&regset=1&rids=213'
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {'search': {'results': [{'department': {'nid': 100500, 'title': 'department1'}}]}},
        )

    def test_department_for_modelinfo(self):
        request = 'place=modelinfo&hyperid={}&show-models-specs=full&rids=213'.format(Model1)
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {'search': {'results': [{'department': {'nid': 100500, 'title': 'department1'}}]}},
        )


if __name__ == '__main__':
    main()
