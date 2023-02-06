#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import BlueOffer, Currency, MarketSku, Model, Offer, Shop, Tax, Vat
from core.testcase import TestCase, main
from core.matcher import Contains
from core.cpc import Cpc


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.models += [
            Model(hyperid=588171101, hid=91494, title='model1'),
        ]
        cls.index.shops += [
            Shop(
                fesh=1006,
                datafeed_id=6,
                priority_region=213,
                currency=Currency.RUR,
                supplier_type=Shop.FIRST_PARTY,
                fulfillment_program=True,
                tax_system=Tax.OSN,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
                warehouse_id=145,
            )
        ]
        cls.index.mskus += [
            MarketSku(
                title='Blue model',
                hyperid=588171101,
                sku=58001,
                blue_offers=[
                    BlueOffer(
                        price=57496,
                        vat=Vat.VAT_10,
                        feedid=6,
                        offerid='offer1',
                        fee=400,
                        waremd5='x2uPN3XNsizR0Kt2DeS6MQ',
                    )
                ],
            ),
        ]
        cls.index.offers += [Offer(title="White model", hyperid=101, hid=2, fesh=2, price=10000)]

    def test_adding_cpc_to_productoffers_result(self):
        request = "place=productoffers&hyperid=588171101&offers-set=default&show-urls=external&rgb=green_with_blue"
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"results": [{"urls": {"encrypted": Contains("%26cpc%3D")}}]})

    def test_adding_cpc_to_prime_result(self):
        request = "place=prime&text=\"Blue offer\"&rgb=green"
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"results": [{"urls": {"encrypted": Contains("%26cpc%3D")}}]})

    def test_adding_cpc_to_parallel_result(self):
        request = "place=parallel&text=\"Blue offer\"&rgb=green"
        response = self.report.request_bs(request)
        self.assertFragmentIn(response, {"items": [{"title": {"offercardUrl": Contains("%26cpc%3D")}}]})

    def test_cpc_param_on_blue(self):
        cpc = Cpc.create_for_offer(
            click_price=71,
            offer_id='x2uPN3XNsizR0Kt2DeS6MQ',
            bid=80,
            shop_id=1006,
            shop_fee=888,
            fee=777,
            minimal_fee=111,
        )
        request = 'place=sku_offers&market-sku=58001&rgb=blue&show-urls=cpa&cpc={}'.format(cpc)
        response = self.report.request_json(request)

        self.assertFragmentIn(
            response,
            {
                'urls': {
                    'cpa': Contains("/fee=0/", "/shop_fee=888/", "/shop_fee_ab=777/", "/min_fee=111/", "/price=57496/")
                }
            },
        )


if __name__ == '__main__':
    main()
