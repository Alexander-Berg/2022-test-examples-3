#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import (
    MnPlace,
    Model,
    Offer,
    Shop,
)
from core.matcher import Contains


class T(TestCase):
    @classmethod
    def prepare_docs_promo(cls):
        cls.index.shops += [
            Shop(
                fesh=30201,
                cpa=Shop.CPA_REAL,
                cpa20=Shop.CPA_REAL,
                fulfillment_program=False,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                priority_region=213,
                regions=[213],
                name="3P магазин 1",
            ),
            Shop(
                fesh=30202,
                cpa=Shop.CPA_REAL,
                cpa20=Shop.CPA_REAL,
                fulfillment_program=False,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                priority_region=213,
                regions=[213],
                name="3P магазин 2",
            ),
            Shop(
                fesh=30203,
                cpa=Shop.CPA_REAL,
                cpa20=Shop.CPA_REAL,
                fulfillment_program=False,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                priority_region=213,
                regions=[213],
                name="3P магазин 3",
            ),
        ]
        cls.index.models += [
            Model(hid=31013, hyperid=34301, ts=34301, vendor_id=1, vbid=15),
            Model(hid=31013, hyperid=34302, ts=34302, vendor_id=2, vbid=16),
            Model(hid=31013, hyperid=34303, ts=34303, vendor_id=3, vbid=17),
            Model(hid=31013, hyperid=34304, ts=34304, vendor_id=1, vbid=18),
            Model(hid=31013, hyperid=34305, ts=34305, vendor_id=2, vbid=19),
            Model(hid=31013, hyperid=34306, ts=34306, vendor_id=3, vbid=20),
            Model(hid=31013, hyperid=34307, ts=34307, vendor_id=1, vbid=21),
            Model(hid=31013, hyperid=34308, ts=34308, vendor_id=2, vbid=22),
            Model(hid=31013, hyperid=34309, ts=34309, vendor_id=3, vbid=23),
        ]
        cls.index.offers += [
            Offer(hid=31013, hyperid=34301, ts=49101, price=10000, cpa=Offer.CPA_REAL, fesh=30201, fee=50),
            Offer(hid=31013, hyperid=34302, ts=49102, price=10000, cpa=Offer.CPA_REAL, fesh=30201, fee=100),
            Offer(hid=31013, hyperid=34303, ts=49103, price=10000, cpa=Offer.CPA_REAL, fesh=30201, fee=150),
            Offer(hid=31013, hyperid=34304, ts=49104, price=10000, cpa=Offer.CPA_REAL, fesh=30202, fee=200),
            Offer(hid=31013, hyperid=34305, ts=49105, price=10000, cpa=Offer.CPA_REAL, fesh=30202, fee=250),
            Offer(hid=31013, hyperid=34306, ts=49106, price=10000, cpa=Offer.CPA_REAL, fesh=30202, fee=300),
            Offer(hid=31013, hyperid=34307, ts=49107, price=10000, cpa=Offer.CPA_REAL, fesh=30203, fee=350),
            Offer(hid=31013, hyperid=34308, ts=49108, price=10000, cpa=Offer.CPA_REAL, fesh=30203, fee=400),
            Offer(hid=31013, hyperid=34309, ts=49109, price=10000, cpa=Offer.CPA_REAL, fesh=30203, fee=450),
        ]
        for i in range(1, 10):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 34300 + i).respond(0.56)

    def test_promo_auction(self):
        request = (
            'pp=2003'
            '&hid=31013&show-urls=external,decrypted,direct%2Ccpa,productVendorBid&bsformat=2'
            '&place=prime&platform=touch&use-default-offers=1'
            '&allow-collapsing=1&rids=213&waitall=da&numdoc=48&debug=1'
            '&rearr-factors=market_white_search_auction_cpa_fee_promo_page=1;market_money_vendor_cpc_to_cpa_conversion=0.075;'
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'slug': "hyperid-34309",
                        'offers': {
                            'items': [
                                {
                                    'urls': {
                                        'cpa': Contains('/shop_fee=450/', '/shop_fee_ab=405/'),
                                    },
                                },
                            ]
                        },
                        'urls': {
                            'decrypted': Contains('/vendor_price=21/', '/vc_bid=23/'),
                        },
                    },
                    {
                        'slug': "hyperid-34308",
                        'offers': {
                            'items': [
                                {
                                    'urls': {
                                        'cpa': Contains('/shop_fee=400/', '/shop_fee_ab=356/'),
                                    },
                                },
                            ]
                        },
                        'urls': {
                            'decrypted': Contains('/vendor_price=20/', '/vc_bid=22/'),
                        },
                    },
                    {
                        'slug': "hyperid-34307",
                        'offers': {
                            'items': [
                                {
                                    'urls': {
                                        'cpa': Contains('/shop_fee=350/', '/shop_fee_ab=307/'),
                                    },
                                },
                            ]
                        },
                        'urls': {
                            'decrypted': Contains('/vendor_price=19/', '/vc_bid=21/'),
                        },
                    },
                    {
                        'slug': "hyperid-34306",
                        'offers': {
                            'items': [
                                {
                                    'urls': {
                                        'cpa': Contains('/shop_fee=300/', '/shop_fee_ab=258/'),
                                    },
                                },
                            ]
                        },
                        'urls': {
                            'decrypted': Contains('/vendor_price=18/', '/vc_bid=20/'),
                        },
                    },
                    {
                        'slug': "hyperid-34305",
                        'offers': {
                            'items': [
                                {
                                    'urls': {
                                        'cpa': Contains('/shop_fee=250/', '/shop_fee_ab=209/'),
                                    },
                                },
                            ]
                        },
                        'urls': {
                            'decrypted': Contains('/vendor_price=16/', '/vc_bid=19/'),
                        },
                    },
                    {
                        'slug': "hyperid-34304",
                        'offers': {
                            'items': [
                                {
                                    'urls': {
                                        'cpa': Contains('/shop_fee=200/', '/shop_fee_ab=160/'),
                                    },
                                },
                            ]
                        },
                        'urls': {
                            'decrypted': Contains('/vendor_price=15/', '/vc_bid=18/'),
                        },
                    },
                    {
                        'slug': "hyperid-34303",
                        'offers': {
                            'items': [
                                {
                                    'urls': {
                                        'cpa': Contains('/shop_fee=150/', '/shop_fee_ab=113/'),
                                    },
                                },
                            ]
                        },
                        'urls': {
                            'decrypted': Contains('/vendor_price=13/', '/vc_bid=17/'),
                        },
                    },
                    {
                        'slug': "hyperid-34302",
                        'offers': {
                            'items': [
                                {
                                    'urls': {
                                        'cpa': Contains('/shop_fee=100/', '/shop_fee_ab=67/'),
                                    },
                                },
                            ]
                        },
                        'urls': {
                            'decrypted': Contains('/vendor_price=11/', '/vc_bid=16/'),
                        },
                    },
                    {
                        'slug': "hyperid-34301",
                        'offers': {
                            'items': [
                                {
                                    'urls': {
                                        'cpa': Contains('/shop_fee=50/', '/shop_fee_ab=0/'),
                                    },
                                },
                            ]
                        },
                        'urls': {
                            'decrypted': Contains('/vendor_price=10/', '/vc_bid=15/'),
                        },
                    },
                ]
            },
        )

    def test_promo_auction_one_vendor(self):
        request = (
            'pp=2003'
            '&vendor_id=1'
            '&hid=31013&show-urls=external,decrypted,direct%2Ccpa,productVendorBid&bsformat=2'
            '&place=prime&platform=touch&use-default-offers=1'
            '&allow-collapsing=1&rids=213&waitall=da&numdoc=48&debug=1'
            '&rearr-factors=market_white_search_auction_cpa_fee_promo_page=1;market_money_vendor_cpc_to_cpa_conversion=0.075;'
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'slug': "hyperid-34307",
                        'offers': {
                            'items': [
                                {
                                    'urls': {
                                        'cpa': Contains('/shop_fee=350/', '/shop_fee_ab=272/'),
                                    },
                                },
                            ]
                        },
                        'urls': {
                            'decrypted': Contains('/vendor_price=0/', '/vc_bid=21/'),
                        },
                    },
                    {
                        'slug': "hyperid-34304",
                        'offers': {
                            'items': [
                                {
                                    'urls': {
                                        'cpa': Contains('/shop_fee=200/', '/shop_fee_ab=110/'),
                                    },
                                },
                            ]
                        },
                        'urls': {
                            'decrypted': Contains('/vendor_price=0/', '/vc_bid=18/'),
                        },
                    },
                    {
                        'slug': "hyperid-34301",
                        'offers': {
                            'items': [
                                {
                                    'urls': {
                                        'cpa': Contains('/shop_fee=50/', '/shop_fee_ab=0/'),
                                    },
                                },
                            ]
                        },
                        'urls': {
                            'decrypted': Contains('/vendor_price=0/', '/vc_bid=15/'),
                        },
                    },
                ]
            },
        )


if __name__ == '__main__':
    main()
