#!/usr/bin/env python
# -*- coding: utf-8 -*-
import runner  # noqa
from core.types import (
    BlueOffer,
    Currency,
    DeliveryBucket,
    DeliveryOption,
    ExpressDeliveryService,
    ExpressSupplier,
    GpsCoord,
    MarketSku,
    Offer,
    OfferDimensions,
    Outlet,
    OutletDeliveryOption,
    Payment,
    PickupBucket,
    PickupOption,
    RegionalDelivery,
    Shop,
    Tax,
    DynamicWarehouseInfo,
    DynamicWarehouseToWarehouseInfo,
)
from core.testcase import TestCase, main
from core.matcher import Absent
from core.types.hypercategory import (
    EATS_CATEG_ID,
    CategoryStreamRecord,
    Stream,
)


def build_ware_md5(offerid):
    return '{}w'.format(offerid.ljust(21, "_"))


CNC_OUTLET = 3335
CNC_PICKUP_BUCKET = 94537
HID = EATS_CATEG_ID
FILTER_NAME = 'with-yandex-delivery'


class _Shops:
    dsbs_shop = Shop(fesh=100, datafeed_id=100, priority_region=213, cpa=Shop.CPA_REAL, name='DSBS')
    white_shop = Shop(fesh=101, datafeed_id=101, priority_region=213, cpa=Shop.CPA_NO, name='WHITE', client_id=101)
    ff_shop = Shop(
        fesh=103,
        datafeed_id=3,
        priority_region=2,
        name='FF',
        currency=Currency.RUR,
        tax_system=Tax.OSN,
        supplier_type=Shop.FIRST_PARTY,
        fulfillment_program=True,
        blue=Shop.BLUE_REAL,
        cpa=Shop.CPA_REAL,
        warehouse_id=145,
        medicine_courier=True,
    )
    third_party_shop = Shop(
        fesh=104,
        datafeed_id=104,
        priority_region=213,
        name='3P_SHOP',
        currency=Currency.RUR,
        tax_system=Tax.OSN,
        supplier_type=Shop.THIRD_PARTY,
        fulfillment_program=True,
        blue=Shop.BLUE_REAL,
        warehouse_id=145,
        medicine_courier=True,
    )
    crossdock_shop = Shop(
        fesh=105,
        datafeed_id=105,
        priority_region=213,
        name='CROSSDOCK',
        currency=Currency.RUR,
        tax_system=Tax.OSN,
        supplier_type=Shop.THIRD_PARTY,
        blue=Shop.BLUE_REAL,
        warehouse_id=1213,
        fulfillment_program=True,
        direct_shipping=None,
        medicine_courier=True,
    )
    dropship_shop = Shop(
        fesh=107,
        datafeed_id=107,
        priority_region=213,
        name='DROPSHIP',
        currency=Currency.RUR,
        tax_system=Tax.OSN,
        supplier_type=Shop.THIRD_PARTY,
        blue=Shop.BLUE_REAL,
        fulfillment_program=False,
        warehouse_id=777,
        medicine_courier=True,
    )
    cnc_shop = Shop(
        fesh=108,
        datafeed_id=108,
        warehouse_id=108,
        fulfillment_program=False,
        ignore_stocks=True,
        name="CNC_SHOP",
        priority_region=213,
        tax_system=Tax.OSN,
        supplier_type=Shop.THIRD_PARTY,
        blue=Shop.BLUE_REAL,
        client_id=4,
        delivery_service_outlets=[CNC_OUTLET],
    )

    # Экспресс-поставщик для синего оффера (добавляется в express_partners)
    blue_express_shop = Shop(
        fesh=109,
        datafeed_id=109,
        warehouse_id=109,
        priority_region=213,
        regions=[213],
        tax_system=Tax.OSN,
        supplier_type=Shop.THIRD_PARTY,
        blue=Shop.BLUE_REAL,
        fulfillment_program=False,
        with_express_warehouse=True,
    )

    # Экспресс-поставщик для dsbs оффера
    express_shop = Shop(
        fesh=110,
        datafeed_id=110,
        warehouse_id=110,
        priority_region=213,
        regions=[213],
        cpa=Shop.CPA_REAL,
        with_express_warehouse=True,
    )

    digital_shop = Shop(
        fesh=111,
        priority_region=213,
        cpa=Shop.CPA_REAL,
    )

    shops = [
        dsbs_shop,
        white_shop,
        ff_shop,
        third_party_shop,
        crossdock_shop,
        dropship_shop,
        cnc_shop,
        blue_express_shop,
        express_shop,
    ]


class _Offers:
    white_offer = Offer(
        fesh=_Shops.white_shop.fesh, hid=HID, price=10, title="find me white", waremd5=build_ware_md5("white_offer")
    )
    dsbs_offer = Offer(
        fesh=_Shops.dsbs_shop.fesh, hid=HID, cpa=Offer.CPA_REAL, title="find me dsbs", waremd5=build_ware_md5("dsbs")
    )
    ff_offer = BlueOffer(
        fesh=_Shops.ff_shop.fesh,
        feedid=_Shops.ff_shop.datafeed_id,
        offerid='ffoffer',
        waremd5=build_ware_md5("ff"),
    )
    third_party_offer = BlueOffer(
        fesh=_Shops.third_party_shop.fesh,
        feedid=_Shops.third_party_shop.datafeed_id,
        offerid='3poffer',
        waremd5=build_ware_md5("3poffer"),
    )
    crossdock_offer = BlueOffer(
        fesh=_Shops.crossdock_shop.fesh,
        feedid=_Shops.crossdock_shop.datafeed_id,
        offerid='crossdock_offer',
        waremd5=build_ware_md5("crossdock"),
    )
    dropship_offer = BlueOffer(
        fesh=_Shops.dropship_shop.fesh,
        feedid=_Shops.dropship_shop.datafeed_id,
        offerid='dropship_offer',
        waremd5=build_ware_md5("dropship"),
    )
    cnc_offer = BlueOffer(
        fesh=_Shops.cnc_shop.fesh,
        feedid=_Shops.cnc_shop.datafeed_id,
        offerid='cnc_offer',
        waremd5=build_ware_md5("cnc"),
    )

    blue_express_offer = BlueOffer(
        offerid='blue_express',
        waremd5=build_ware_md5("express_blue"),
        price=30,
        feedid=_Shops.blue_express_shop.datafeed_id,
        weight=5,
        dimensions=OfferDimensions(length=30, width=30, height=30),
        supplier_id=_Shops.blue_express_shop.fesh,
        delivery_buckets=[4241],
        is_express=True,
    )

    white_express_offer = Offer(
        waremd5=build_ware_md5("express_white"),
        hid=HID,
        sku=18,
        fesh=_Shops.express_shop.fesh,
        price=30,
        weight=5,
        dimensions=OfferDimensions(length=30, width=30, height=30),
        delivery_buckets=[4242],
        is_express=True,
    )

    downloadable_offer = Offer(
        cpa=Offer.CPA_REAL,
        fesh=1004,
        price=5,
        post_term_delivery=True,
        download=True,
        store=True,
        delivery_options=[DeliveryOption(price=0, day_from=1, day_to=2)],
    )

    lavka_offer = Offer(
        fesh=999,
        hid=HID,
        title='лавка',
        shop_category_path='лавка',
        shop_category_path_ids='1',
        is_lavka=True,
        sku=19,
        delivery_buckets=[4243],
    )

    digital_offer = Offer(
        cpa=Offer.CPA_REAL,
        hid=HID,
        fesh=_Shops.digital_shop.fesh,
        price=25,
        download=True,
        waremd5=build_ware_md5("digital"),
    )

    offers = [white_offer, dsbs_offer, white_express_offer, lavka_offer, digital_offer]


class _MSKU:
    ff_msku = MarketSku(title="ff msku", sku=11, hid=HID, blue_offers=[_Offers.ff_offer])
    third_party_msku = MarketSku(
        title="third party msku",
        sku=12,
        hid=HID,
        blue_offers=[_Offers.third_party_offer],
    )
    crossdock_msku = MarketSku(
        title="crossdock msku",
        sku=13,
        hid=HID,
        blue_offers=[
            _Offers.crossdock_offer,
        ],
    )
    dropship_msku = MarketSku(
        title="dropship msku",
        sku=14,
        hid=HID,
        blue_offers=[
            _Offers.dropship_offer,
        ],
    )
    cnc_msku = MarketSku(
        title="cnc msku",
        sku=15,
        hid=HID,
        blue_offers=[
            _Offers.cnc_offer,
        ],
        pickup_buckets=[
            CNC_PICKUP_BUCKET,
        ],
    )

    blue_express_msku = MarketSku(
        title="Синий экспресс оффер",
        hid=HID,
        sku=17,
        blue_offers=[_Offers.blue_express_offer],
    )

    white_express_msku = MarketSku(
        title="dsbs экспресс оффер",
        hid=HID,
        sku=18,
    )

    lavka_msku = MarketSku(title="lavka", hid=HID, sku=19)

    mskus = [
        ff_msku,
        third_party_msku,
        crossdock_msku,
        dropship_msku,
        cnc_msku,
        blue_express_msku,
        white_express_msku,
        lavka_msku,
    ]


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.market_access_settings.enabled = True
        cls.settings.market_access_settings.download_catstreams = True
        cls.index.category_streams += [
            CategoryStreamRecord(EATS_CATEG_ID, Stream.FMCG.value),
        ]
        cls.index.shops += _Shops.shops
        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=145, home_region=213),
            DynamicWarehouseInfo(id=1213, home_region=213),
            DynamicWarehouseInfo(id=777, home_region=213),
            DynamicWarehouseInfo(id=108, home_region=213),
            DynamicWarehouseInfo(id=109, home_region=213),
            DynamicWarehouseInfo(id=110, home_region=213),
            DynamicWarehouseToWarehouseInfo(warehouse_from=145, warehouse_to=145),
            DynamicWarehouseToWarehouseInfo(warehouse_from=1213, warehouse_to=1213),
            DynamicWarehouseToWarehouseInfo(warehouse_from=777, warehouse_to=777),
            DynamicWarehouseToWarehouseInfo(warehouse_from=108, warehouse_to=108),
            DynamicWarehouseToWarehouseInfo(warehouse_from=109, warehouse_to=109),
            DynamicWarehouseToWarehouseInfo(warehouse_from=110, warehouse_to=110),
        ]
        cls.index.mskus += _MSKU.mskus
        cls.index.offers += _Offers.offers
        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=CNC_PICKUP_BUCKET,
                fesh=_Shops.cnc_shop.fesh,
                carriers=[99],
                options=[
                    PickupOption(outlet_id=CNC_OUTLET),
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]
        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=4241,
                dc_bucket_id=1000,
                fesh=_Shops.blue_express_shop.fesh,
                carriers=[156],
                regional_options=[
                    RegionalDelivery(
                        rid=213,
                        options=[
                            DeliveryOption(
                                price=123,
                                day_from=0,
                                day_to=0,
                            )
                        ],
                        payment_methods=[Payment.PT_YANDEX, Payment.PT_CASH_ON_DELIVERY, Payment.PT_CARD_ON_DELIVERY],
                    )
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=4242,
                dc_bucket_id=1001,
                fesh=_Shops.express_shop.fesh,
                carriers=[156],
                regional_options=[
                    RegionalDelivery(
                        rid=213,
                        options=[
                            DeliveryOption(
                                price=123,
                                day_from=0,
                                day_to=0,
                            )
                        ],
                        payment_methods=[Payment.PT_YANDEX, Payment.PT_CASH_ON_DELIVERY, Payment.PT_CARD_ON_DELIVERY],
                    )
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=4243,
                fesh=999,
                carriers=[156],
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=100, day_from=1, day_to=1)])],
            ),
        ]
        cls.index.outlets += [
            Outlet(
                point_id=CNC_OUTLET,
                fesh=_Shops.cnc_shop.fesh,
                region=213,
                point_type=Outlet.FOR_STORE,
                delivery_option=OutletDeliveryOption(day_from=1, day_to=1, order_before=1, price=50),
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(36.19, 55.4),
            ),
        ]
        cls.index.express_partners.suppliers += [
            ExpressSupplier(
                feed_id=_Shops.express_shop.datafeed_id,
                supplier_id=_Shops.express_shop.fesh,
                warehouse_id=_Shops.express_shop.warehouse_id,
            ),
            ExpressSupplier(
                feed_id=_Shops.blue_express_shop.datafeed_id,
                supplier_id=_Shops.blue_express_shop.fesh,
                warehouse_id=_Shops.blue_express_shop.warehouse_id,
            ),
        ]
        cls.index.express_partners.delivery_services += [
            ExpressDeliveryService(delivery_service_id=156, delivery_price_for_user=350)
        ]

    def test_filter(self):
        '''
        Проверяем, что фильтрация "с доставкой яндекса"
        убирает cnc dsbs и cpc оффера
        '''
        for filter_set, offers_list in [
            (
                False,
                [
                    _Offers.ff_offer,
                    _Offers.dropship_offer,
                    _Offers.cnc_offer,
                    _Offers.crossdock_offer,
                    _Offers.third_party_offer,
                    _Offers.white_offer,
                    _Offers.dsbs_offer,
                    _Offers.blue_express_offer,
                    _Offers.white_express_offer,
                    _Offers.lavka_offer,
                    _Offers.digital_offer,
                ],
            ),
            (
                None,
                [
                    _Offers.ff_offer,
                    _Offers.dropship_offer,
                    _Offers.cnc_offer,
                    _Offers.crossdock_offer,
                    _Offers.third_party_offer,
                    _Offers.white_offer,
                    _Offers.dsbs_offer,
                    _Offers.blue_express_offer,
                    _Offers.white_express_offer,
                    _Offers.lavka_offer,
                    _Offers.digital_offer,
                ],
            ),
            (
                True,
                [
                    _Offers.ff_offer,
                    _Offers.dropship_offer,
                    _Offers.crossdock_offer,
                    _Offers.third_party_offer,
                    _Offers.blue_express_offer,
                    _Offers.white_express_offer,
                    _Offers.lavka_offer,
                    _Offers.digital_offer,
                ],
            ),
        ]:
            request = (
                'place=prime'
                '&hid={hid}'
                '&rids=213'
                '&rgb=green_with_blue'
                '&rearr-factors=market_hide_regional_delimiter=1'
                '&numdoc=100'
                '&enable-foodtech-offers=1'
            )
            if filter_set is not None:
                request += '&{}={}'.format(FILTER_NAME, 1 if filter_set else 0)
            response = self.report.request_json(request.format(hid=HID))
            self.assertFragmentIn(
                response, {"results": [{"wareId": offer.waremd5} for offer in offers_list]}, allow_different_len=False
            )

    def test_output_filter(self):
        base_requst = (
            'place=prime'
            '&hid={hid}'
            '&rids=213'
            '&rgb=green_with_blue'
            '&rearr-factors=market_hide_regional_delimiter=1'
            '&numdoc=100'
            '&enable-foodtech-offers=1'
        )

        for filter_values, filter_check in [([0, 1, None], (3, 8))]:
            for filter_set in filter_values:
                request = base_requst
                if filter_set is not None:
                    request += '&{}={}'.format(FILTER_NAME, 1 if filter_set else 0)
                response = self.report.request_json(request.format(hid=HID))

                zero_found, one_found = filter_check
                self.assertFragmentIn(
                    response,
                    {
                        "filters": [
                            {
                                "id": FILTER_NAME,
                                "type": "boolean",
                                "name": "С доставкой Яндекса",
                                "values": [
                                    {
                                        "value": "1",
                                        "found": one_found,
                                        "checked": True if filter_set == 1 else Absent(),
                                    },
                                    {
                                        "value": "0",
                                        "found": zero_found if filter_set != 1 else Absent(),
                                        "checked": True if filter_set == 0 else Absent(),
                                    },
                                ],
                            }
                        ]
                    },
                )

    def test_hide_filter(self):
        request = (
            'place=prime'
            '&hid={hid}'
            '&rids=213'
            '&rgb=green_with_blue'
            '&enable-foodtech-offers=1'
            '&numdoc=100'.format(hid=HID)
        )
        request += "&hide-filter={}".format(FILTER_NAME)
        response = self.report.request_json(request)
        self.assertFragmentNotIn(
            response,
            {
                "filters": [
                    {
                        "id": FILTER_NAME,
                    }
                ]
            },
        )


if __name__ == '__main__':
    main()
