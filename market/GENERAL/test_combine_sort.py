#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BlueOffer,
    DeliveryBucket,
    DeliveryCalcFeedInfo,
    DeliveryOption,
    ExpressDeliveryService,
    ExpressSupplier,
    MarketSku,
    OfferDimensions,
    RegionalDelivery,
    Shop,
    Tax,
)
from core.testcase import TestCase, main


PLACE_COMBINE_WITH_RGB = "place=combine&rgb=green_with_blue&use-virt-shop=0"


shop_express = Shop(
    fesh=43,
    datafeed_id=4240,
    priority_region=213,
    regions=[213],
    client_id=12,
    tax_system=Tax.OSN,
    supplier_type=Shop.THIRD_PARTY,
    blue=Shop.BLUE_REAL,
    warehouse_id=22,
    with_express_warehouse=True,
    fulfillment_program=False,
)

shop_express1 = Shop(
    fesh=143,
    datafeed_id=14240,
    priority_region=213,
    regions=[213],
    client_id=112,
    tax_system=Tax.OSN,
    supplier_type=Shop.THIRD_PARTY,
    blue=Shop.BLUE_REAL,
    warehouse_id=122,
    with_express_warehouse=True,
    fulfillment_program=False,
)

shop_express_no_delivery = Shop(
    fesh=1143,
    datafeed_id=114240,
    priority_region=213,
    regions=[213],
    client_id=1112,
    tax_system=Tax.OSN,
    supplier_type=Shop.THIRD_PARTY,
    blue=Shop.BLUE_REAL,
    warehouse_id=1122,
    with_express_warehouse=True,
)

shop_blue = Shop(
    fesh=44,
    datafeed_id=3232,
    priority_region=213,
    regions=[213],
    client_id=13,
    cpa=Shop.CPA_REAL,
    blue=Shop.BLUE_REAL,
)

shop_blue1 = Shop(
    fesh=144,
    datafeed_id=13232,
    priority_region=213,
    regions=[213],
    client_id=113,
    cpa=Shop.CPA_REAL,
    blue=Shop.BLUE_REAL,
    warehouse_id=155,
)

offer_express0 = BlueOffer(
    waremd5='Express0_____________g',
    offerid='express_dropship_sku1',
    fesh=shop_express.fesh,
    supplier_id=shop_express.fesh,
    feedid=shop_express.datafeed_id,
    weight=5,
    dimensions=OfferDimensions(length=30, width=30, height=30),
    is_express=True,
    delivery_buckets=[4243],
)

offer_express1 = BlueOffer(
    waremd5='Express1_____________g',
    offerid='express_dropship_sku2',
    fesh=shop_express1.fesh,
    supplier_id=shop_express1.fesh,
    feedid=shop_express1.datafeed_id,
    weight=5,
    dimensions=OfferDimensions(length=30, width=30, height=30),
    is_express=True,
    delivery_buckets=[4244],
)

offer_express_filtered = BlueOffer(
    waremd5='Express2_____________g',
    offerid='express_dropship_sku2',
    fesh=shop_express_no_delivery.fesh,
    feedid=shop_express_no_delivery.datafeed_id,
    is_express=True,
)

blue_offer0 = BlueOffer(
    waremd5='BlueOffer0___________g',
    offerid='blue____dropship_sku0',
    fesh=shop_blue.fesh,
    supplier_id=shop_blue.fesh,
    feedid=shop_blue.datafeed_id,
    weight=5,
    dimensions=OfferDimensions(length=30, width=30, height=30),
    delivery_buckets=[4245],
)

blue_offer1 = BlueOffer(
    waremd5='BlueOffer1___________g',
    offerid='blue____dropship_sku1',
    fesh=shop_blue1.fesh,
    supplier_id=shop_blue1.fesh,
    feedid=shop_blue1.datafeed_id,
    weight=3,
    dimensions=OfferDimensions(length=10, width=10, height=10),
    delivery_buckets=[4246],
)


msku_express = MarketSku(hyperid=222, sku=100501, blue_offers=[offer_express0])
mksu_common = MarketSku(hyperid=333, sku=100502, blue_offers=[blue_offer0])
mksu_for_filtered = MarketSku(hyperid=444, sku=100503, blue_offers=[offer_express_filtered])

msku_express1 = MarketSku(hyperid=2222, sku=200501, blue_offers=[offer_express1])
mksu_common1 = MarketSku(hyperid=3333, sku=200502, blue_offers=[blue_offer1])


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.nordstream_autogenerate = False
        cls.settings.default_search_experiment_flags += ['market_nordstream=0']
        # НЕ делайте так в новых тестах!
        # Походов в КД на проде уже нет, пожалуйста, проверяйте новую функциональность, создавая доставку через комбинатор
        cls.settings.default_search_experiment_flags += ['force_disable_delivery_calculator_requests=0']
        cls.index.mskus += [
            msku_express,
            mksu_common,
            mksu_for_filtered,
            msku_express1,
            mksu_common1,
        ]

        cls.index.shops += [shop_blue, shop_blue1, shop_express, shop_express1, shop_express_no_delivery]

        cls.index.express_partners.suppliers += [
            ExpressSupplier(
                feed_id=shop_express.datafeed_id, supplier_id=shop_express.fesh, warehouse_id=shop_express.warehouse_id
            ),
            ExpressSupplier(
                feed_id=shop_express1.datafeed_id,
                supplier_id=shop_express1.fesh,
                warehouse_id=shop_express1.warehouse_id,
            ),
        ]

        cls.index.express_partners.delivery_services += [
            ExpressDeliveryService(delivery_service_id=125, delivery_price_for_user=350),
            ExpressDeliveryService(delivery_service_id=126, delivery_price_for_user=350),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=4242,
                fesh=42,
                carriers=[99],
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=100, day_from=1, day_to=3)])],
            ),
            DeliveryBucket(
                bucket_id=4243,
                dc_bucket_id=4243,
                fesh=shop_express.fesh,
                carriers=[125],
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=100, day_from=0, day_to=0)])],
            ),
            DeliveryBucket(
                bucket_id=4244,
                dc_bucket_id=4244,
                fesh=shop_express1.fesh,
                carriers=[126],
                regional_options=[
                    RegionalDelivery(rid=213, options=[DeliveryOption(price=100, day_from=10, day_to=11)])
                ],
            ),
            DeliveryBucket(
                bucket_id=4245,
                dc_bucket_id=4245,
                fesh=shop_blue.fesh,
                carriers=[99],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[
                    RegionalDelivery(rid=213, options=[DeliveryOption(price=100, day_from=10, day_to=15)])
                ],
            ),
            DeliveryBucket(
                bucket_id=4246,
                dc_bucket_id=4246,
                fesh=shop_blue1.fesh,
                carriers=[99],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=100, day_from=3, day_to=4)])],
            ),
        ]

        cls.delivery_calc.on_request_offer_buckets(
            weight=5, height=30, length=30, width=30, warehouse_id=shop_express.warehouse_id
        ).respond([4243], [], [])
        cls.delivery_calc.on_request_offer_buckets(
            weight=5, height=30, length=30, width=30, warehouse_id=shop_express1.warehouse_id
        ).respond([4244], [], [])
        cls.delivery_calc.on_request_offer_buckets(weight=5, height=30, length=30, width=30, warehouse_id=145).respond(
            [4245], [], []
        )
        cls.delivery_calc.on_request_offer_buckets(weight=3, height=10, length=10, width=10, warehouse_id=155).respond(
            [4246], [], []
        )

        cls.index.delivery_calc_feed_info += [
            DeliveryCalcFeedInfo(feed_id=shop_express.datafeed_id, warehouse_id=shop_express.warehouse_id),
            DeliveryCalcFeedInfo(feed_id=shop_express1.datafeed_id, warehouse_id=shop_express1.warehouse_id),
            DeliveryCalcFeedInfo(feed_id=shop_blue.datafeed_id, warehouse_id=shop_blue.warehouse_id),
            DeliveryCalcFeedInfo(feed_id=shop_blue1.datafeed_id, warehouse_id=shop_blue1.warehouse_id),
        ]

    def request_combine(self, mskus, offers, region=213, count=1, flags=''):
        USE_DEPRECATED_DIRECT_SHIPPING_FLOW = "&rearr-factors=get_rid_of_direct_shipping=0"
        request = PLACE_COMBINE_WITH_RGB + '&rids={}'.format(region)
        assert len(mskus) == len(offers), 'len(mskus) == {} is not equal to len(offers) == {}'.format(
            len(mskus), len(offers)
        )

        request_offers = []
        for cart_item_id, (msku, offer) in enumerate(zip(mskus, offers)):
            request_offers += ['{}:{};msku:{};cart_item_id:{}'.format(offer.waremd5, count, msku.sku, cart_item_id + 1)]
        if request_offers:
            request += '&offers-list=' + ','.join(request_offers)
        request += flags
        request += USE_DEPRECATED_DIRECT_SHIPPING_FLOW
        return self.report.request_json(request)

    def test_new_combine_sort_positive(self):
        """
        В комбайне под флагом market_combine_new_sort
        должна происходить такая сортировка посылок:
        1) Express
        2) Остальное
        3) Поссылка с протухшими офферами
        + как раньше внутри каждой группы сортировка по дате доставки
        """

        # В этом запросе должен быть такой порядок посылок:
        # 1) offer_express0, offer_express1
        # 2) blue_offer1, blue_offer0 - тк у blue_offer1 доставка быстрее, чем у blue_offer0
        # 3) offer_express_filtered - протухший
        response = self.request_combine(
            (msku_express, mksu_common, msku_express1, mksu_common1, mksu_for_filtered),
            (offer_express0, blue_offer0, offer_express1, blue_offer1, offer_express_filtered),
            flags='&rearr-factors=market_combine_new_sort=1',
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "buckets": [
                            {
                                "IsEdaParcel": False,
                                "IsExpressParcel": True,
                                "offers": [
                                    {
                                        "cartItemIds": [1],
                                        "replacedId": "Express0_____________g",
                                        "wareId": "Express0_____________g",
                                    }
                                ],
                            },
                            {
                                "IsEdaParcel": False,
                                "IsExpressParcel": True,
                                "offers": [
                                    {
                                        "cartItemIds": [3],
                                        "replacedId": "Express1_____________g",
                                        "wareId": "Express1_____________g",
                                    }
                                ],
                            },
                            {
                                "IsEdaParcel": False,
                                "IsExpressParcel": False,
                                "offers": [
                                    {
                                        "cartItemIds": [4],
                                        "replacedId": "BlueOffer1___________g",
                                        "wareId": "BlueOffer1___________g",
                                    }
                                ],
                            },
                            {
                                "IsEdaParcel": False,
                                "IsExpressParcel": False,
                                "offers": [
                                    {
                                        "cartItemIds": [2],
                                        "replacedId": "BlueOffer0___________g",
                                        "wareId": "BlueOffer0___________g",
                                    }
                                ],
                            },
                            {
                                "IsEdaParcel": False,
                                "IsExpressParcel": False,
                                "offers": [
                                    {
                                        "cartItemIds": [5],
                                        "reason": "DELIVERY_BLUE",
                                        "replacedId": "Express2_____________g",
                                        "wareId": "",
                                    }
                                ],
                                "warehouseId": 0,
                            },
                        ]
                    }
                ]
            },
            allow_different_len=False,
            preserve_order=True,
        )


if __name__ == '__main__':
    main()
