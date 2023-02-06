#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BlueOffer,
    Contex,
    Currency,
    DeliveryBucket,
    DeliveryOption,
    MarketSku,
    Model,
    RegionalDelivery,
    RtyOffer,
    Shop,
    Tax,
)
from core.testcase import TestCase, main
from core.types.contex import create_experiment_mskus


class Offers(object):
    sku1_offer1 = BlueOffer(feedid=5, waremd5='Sku1Offer1-IiLVm1goleg', offerid='shop1_sku1_offer1', price=5)


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=1,
                dc_bucket_id=1,
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=50)])],
                carriers=[1],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
        ]

        cls.index.shops += [
            Shop(
                fesh=1,
                datafeed_id=5,
                priority_region=213,
                regions=[213],
                name='blue_shop_1',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                blue='REAL',
                cpa=Shop.CPA_REAL,
            ),
        ]

        cls.index.models += [
            Model(hyperid=1, hid=1, title='Mobile Phone'),
            Model(hyperid=2, hid=1, title='Super Mobile Phone', contex=Contex(parent_id=1, exp_name='rty-exp-1')),
        ]

        msku_no_exp_1, msku_exp_1 = create_experiment_mskus(
            base_class=MarketSku,
            offer_kwargs={
                'title': 'Mobile Phone',
                'hyperid': 1,
                'sku': 1,
                'blue_offers': [Offers.sku1_offer1],
                'randx': 2,
            },
            offer_exp_kwargs={
                'hyperid': 2,
                'sku': 2,
                'title': 'Super Mobile Phone',
            },
            exp_name='rty-exp-1',
        )

        cls.index.mskus += [msku_no_exp_1, msku_exp_1]
        cls.settings.rty_qpipe = True
        cls.settings.report_subrole = 'blue-main'
        cls.settings.use_external_snippets = False

    def _do_request(self, in_exp):
        rearr_factors = 'rearr-factors=' + ';'.join(['rty_qpipe=1', 'contex=1', 'rty-exp-1={}'.format(int(in_exp))])
        url = 'place=prime&text=Mobile+Phone&rgb=blue&' + rearr_factors
        return self.report.request_json(url)

    def _expected_offers(self, title, price):
        return {
            'offers': {
                'count': 1,
                'items': [
                    {
                        'titles': {'raw': title},
                        'prices': {'value': str(price), 'currency': 'RUR'},
                    }
                ],
            }
        }

    def test_rty_quick_price(self):
        PRICE_BEFORE_RTY = 5
        PRICE_AFTER_RTY = 10

        response = self._do_request(False)
        self.assertFragmentIn(
            response, self._expected_offers('Mobile Phone', PRICE_BEFORE_RTY), allow_different_len=False
        )
        response = self._do_request(True)
        self.assertFragmentIn(
            response, self._expected_offers('Super Mobile Phone', PRICE_BEFORE_RTY), allow_different_len=False
        )

        # Send RTY quick data
        self.rty.offers += [
            RtyOffer(feedid=5, offerid='shop1_sku1_offer1', price=PRICE_AFTER_RTY),
        ]

        response = self._do_request(False)
        self.assertFragmentIn(
            response, self._expected_offers('Mobile Phone', PRICE_AFTER_RTY), allow_different_len=False
        )
        response = self._do_request(True)
        self.assertFragmentIn(
            response, self._expected_offers('Super Mobile Phone', PRICE_AFTER_RTY), allow_different_len=False
        )


if __name__ == '__main__':
    main()
