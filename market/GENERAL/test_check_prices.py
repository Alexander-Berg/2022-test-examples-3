#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    Currency,
    DeliveryBucket,
    DeliveryOption,
    DynamicDaysSet,
    DynamicDeliveryServiceInfo,
    DynamicWarehouseInfo,
    DynamicWarehousesPriorityInRegion,
    Offer,
    Promo,
    PromoType,
    RegionalDelivery,
    Shop,
    Tax,
    Vat,
)
from core.testcase import TestCase, main
from core.types.sku import (
    MarketSku,
    BlueOffer,
)

from core.types.autogen import b64url_md5
from market.proto.common.promo_pb2 import ESourceType
from core.types.offer_promo import PromoDirectDiscount, OffersMatchingRules

import base64

# Сохранить ключ промо для проверки на выдаче
DCO_3P_PROMO_MD5_KEY = b64url_md5(5)

# Офферное промо для проверки ДЦО промо
promo11 = Promo(
    promo_type=PromoType.DIRECT_DISCOUNT,
    feed_id=777,
    key=DCO_3P_PROMO_MD5_KEY,
    url='http://direct_discount_11.com/',
    shop_promo_id='promo11',
    source_type=ESourceType.DCO_3P_DISCOUNT,
    direct_discount=PromoDirectDiscount(
        items=[
            {
                'feed_id': 777,
                'offer_id': 'offer_id_5000',
                'discount_price': {'value': 800, 'currency': 'RUR'},
                'old_price': {'value': 1000, 'currency': 'RUR'},
                'max_discount': {'value': 300, 'currency': 'RUR'},
                'max_discount_percent': 45.0,
            }
        ],
        allow_berubonus=True,
        allow_promocode=True,
    ),
    offers_matching_rules=[
        OffersMatchingRules(mskus=[9876]),
    ],
)

offer11 = BlueOffer(
    waremd5=b64url_md5(555666777),
    price=950,
    price_old=1000,
    fesh=777,
    feedid=777,
    offerid='offer_id_5000',
    promo=promo11,
)

msku11 = MarketSku(sku=9876, hyperid=9876, hid=9876, blue_offers=[offer11])


class T(TestCase):

    # Оферы для проверки доставки
    skuDelivery_offer1 = BlueOffer(
        price=8,
        price_old=8,
        vat=Vat.VAT_10,
        feedid=4,
        offerid='blue.offer.Del.1',
        waremd5='SkuDelPrice8_________g',
        delivery_buckets=[3, 4],
    )
    skuDelivery_offer2 = BlueOffer(
        price=9,
        price_old=8,
        vat=Vat.VAT_10,
        feedid=5,
        offerid='blue.offer.Del.2',
        waremd5='SkuDelPrice9_________g',
        delivery_buckets=[4],
    )

    @classmethod
    def prepare(cls):
        cls.settings.nordstream_autogenerate = False
        cls.settings.default_search_experiment_flags += ['market_nordstream=0']
        cls.index.offers += [
            Offer(feedid=100, offerid='001', price=100, fesh=1),
            Offer(feedid=200, offerid='001', price=200, sku=1, fesh=1),
            Offer(feedid=300, offerid='001', price=300, sku=1, fesh=2),
            Offer(feedid=400, offerid='001', price=400, sku=2, fesh=1),
            Offer(feedid=500, offerid='A,B'),
        ]

    @classmethod
    def prepare_delivery(cls):
        cls.index.shops += [
            Shop(
                fesh=3,
                datafeed_id=3,
                priority_region=213,
                name='virtual_shop',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                cpa=Shop.CPA_REAL,
            ),
            Shop(
                fesh=4,
                datafeed_id=4,
                priority_region=2,
                name='blue_shop_4',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
                warehouse_id=147,
            ),
            Shop(
                fesh=5,
                datafeed_id=5,
                priority_region=213,
                name='blue_shop_5',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
                warehouse_id=145,
            ),
            Shop(fesh=777, datafeed_id=777, priority_region=213, supplier_type=Shop.THIRD_PARTY, blue=Shop.BLUE_REAL),
            Shop(
                fesh=6,
                datafeed_id=6,
                priority_region=213,
                name="dsbs магазин",
                cpa=Shop.CPA_REAL,
            ),
        ]

        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=147, home_region=2, holidays_days_set_key=1),
            DynamicWarehouseInfo(id=145, home_region=213, holidays_days_set_key=1),
            DynamicWarehousesPriorityInRegion(region=225, warehouses=[145, 147]),
            DynamicWarehousesPriorityInRegion(region=2, warehouses=[147]),
            DynamicDeliveryServiceInfo(id=48, name="c_48"),
            DynamicDaysSet(key=1, days=[]),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=3,
                fesh=3,
                carriers=[48],
                regional_options=[
                    RegionalDelivery(rid=3, options=[DeliveryOption(price=5, day_from=1, day_to=2)]),
                ],
            ),
            DeliveryBucket(
                bucket_id=4,
                fesh=3,
                carriers=[48],
                regional_options=[
                    RegionalDelivery(rid=4, options=[DeliveryOption(price=5, day_from=1, day_to=2)]),
                ],
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                title="BuyboxDeliveryTest",
                hyperid=110,
                sku=116,
                blue_offers=[T.skuDelivery_offer1, T.skuDelivery_offer2],
            ),
            msku11,
        ]

        cls.index.offers += [
            Offer(
                fesh=6,
                feedid=6,
                price=29990,
                cpa=Offer.CPA_REAL,
                offerid='dsbs_offer',
            )
        ]

        cls.index.promos += [promo11]

    def test_feed_shoffer_id(self):
        response = self.report.request_json('place=check_prices' '&feed_shoffer_id=100-001,200-001')
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'feedId': 100,
                        'offerId': '001',
                        'shopId': 1,
                        'price': {
                            'currency': 'RUR',
                            'value': "100",
                        },
                    },
                    {
                        'feedId': 200,
                        'offerId': '001',
                        'shopId': 1,
                        'price': {
                            'currency': 'RUR',
                            'value': "200",
                        },
                    },
                ]
            },
            allow_different_len=False,
        )

    def test_market_sku_and_fesh(self):
        response = self.report.request_json('place=check_prices' '&market-sku=1,2' '&fesh=1,2')
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'feedId': 200,
                        'offerId': '001',
                        'shopId': 1,
                        'price': {
                            'currency': 'RUR',
                            'value': "200",
                        },
                    },
                    {
                        'feedId': 300,
                        'offerId': '001',
                        'shopId': 2,
                        'price': {
                            'currency': 'RUR',
                            'value': "300",
                        },
                    },
                    {
                        'feedId': 400,
                        'offerId': '001',
                        'shopId': 1,
                        'price': {
                            'currency': 'RUR',
                            'value': "400",
                        },
                    },
                ]
            },
            allow_different_len=False,
        )

    def test_feed_shoffer_id_and_market_sku_and_fesh(self):
        response = self.report.request_json('place=check_prices' '&feed_shoffer_id=100-001' '&market-sku=1' '&fesh=1')
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'feedId': 100,
                        'offerId': '001',
                        'shopId': 1,
                        'price': {
                            'currency': 'RUR',
                            'value': "100",
                        },
                    },
                    {
                        'feedId': 200,
                        'offerId': '001',
                        'shopId': 1,
                        'price': {
                            'currency': 'RUR',
                            'value': "200",
                        },
                    },
                ]
            },
            allow_different_len=False,
        )

    def test_feed_shoffer_id_base64(self):
        response = self.report.request_json(
            'place=check_prices' '&feed_shoffer_id_base64={}'.format(base64.urlsafe_b64encode('500-A,B'))
        )
        self.assertFragmentIn(
            response,
            {
                'feedId': 500,
                'offerId': 'A,B',
            },
        )

    def test_blue_offer_delivery_ignore_region(self):
        response = self.report.request_json(
            'place=check_prices'
            '&regset=1'
            '&rgb=blue'
            '&feed_shoffer_id_base64={},{}'.format(
                base64.urlsafe_b64encode('4-blue.offer.Del.1'), base64.urlsafe_b64encode('5-blue.offer.Del.2')
            )
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        "feedId": 3,
                        "offerId": "5.blue.offer.Del.2",
                        "shopId": 3,
                        "msku": "116",
                        "price": {"currency": "RUR", "value": "9"},
                        "isBlue": True,
                        "wareId": "SkuDelPrice9_________g",
                        "supplierId": 5,
                        "warehouseId": 145,
                        "deliveryOptions": {"hasCourier": True, "hasPickup": True, "hasPost": False},
                    },
                    {
                        "feedId": 3,
                        "offerId": "4.blue.offer.Del.1",
                        "shopId": 3,
                        "msku": "116",
                        "price": {"currency": "RUR", "value": "8"},
                        "isBlue": True,
                        "wareId": "SkuDelPrice8_________g",
                        "supplierId": 4,
                        "warehouseId": 147,
                        "deliveryOptions": {"hasCourier": True, "hasPickup": True, "hasPost": False},
                    },
                ]
            },
            allow_different_len=False,
        )

    def test_blue_offer_delivery_region_3(self):
        USE_DEPRECATED_DIRECT_SHIPPING_FLOW = "&rearr-factors=get_rid_of_direct_shipping=0"
        request = (
            'place=check_prices'
            '&regset=1'
            '&rids=3'
            '&rgb=blue'
            '&feed_shoffer_id_base64={},{}'.format(
                base64.urlsafe_b64encode('4-blue.offer.Del.1'), base64.urlsafe_b64encode('5-blue.offer.Del.2')
            )
        )
        request += USE_DEPRECATED_DIRECT_SHIPPING_FLOW
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        "feedId": 3,
                        "offerId": "5.blue.offer.Del.2",
                        "shopId": 3,
                        "msku": "116",
                        "price": {"currency": "RUR", "value": "9"},
                        "isBlue": True,
                        "wareId": "SkuDelPrice9_________g",
                        "supplierId": 5,
                        "warehouseId": 145,
                        "deliveryOptions": {"hasCourier": False, "hasPickup": False, "hasPost": False},
                    },
                    {
                        "feedId": 3,
                        "offerId": "4.blue.offer.Del.1",
                        "shopId": 3,
                        "msku": "116",
                        "price": {"currency": "RUR", "value": "8"},
                        "isBlue": True,
                        "wareId": "SkuDelPrice8_________g",
                        "supplierId": 4,
                        "warehouseId": 147,
                        "deliveryOptions": {"hasCourier": True, "hasPickup": False, "hasPost": False},
                    },
                ]
            },
            allow_different_len=False,
        )

    def test_direct_discount_dco_3p_promo(self):
        """
        Проверяем наличие флага has_dco_3p_subsidy на выдаче
        """
        response = self.report.request_json('place=check_prices&feed_shoffer_id=777-offer_id_5000')
        self.assertFragmentIn(
            response,
            {
                "offerId": "777.offer_id_5000",
                "price": {"value": "800", "currency": "RUR"},
                "promos": [
                    {
                        "type": "direct-discount",
                        "key": DCO_3P_PROMO_MD5_KEY,
                        "url": "http://direct_discount_11.com/",
                        "shopPromoId": "promo11",
                        "itemsInfo": {
                            "price": {
                                "value": "800",
                                "currency": "RUR",
                                "discount": {"oldMin": "1000", "absolute": "200"},
                                "subsidy": {"oldMin": "950", "absolute": "150"},
                            },
                            "has_dco_3p_subsidy": True,
                        },
                    }
                ],
            },
        )

    def test_dsbs_with_cpa_eq_real_request(self):
        """
        Проверяем, что cpa=real не влияет на выдачу
        """
        response = self.report.request_json('place=check_prices&feed_shoffer_id=6-dsbs_offer')
        self.assertFragmentIn(
            response,
            {
                "feedId": 6,
                "offerId": "dsbs_offer",
                "price": {"value": "29990", "currency": "RUR"},
            },
        )

        response = self.report.request_json('place=check_prices&feed_shoffer_id=6-dsbs_offer&cpa=real')
        self.assertFragmentIn(
            response,
            {
                "feedId": 6,
                "offerId": "dsbs_offer",
                "price": {"value": "29990", "currency": "RUR"},
            },
        )


if __name__ == '__main__':
    main()
