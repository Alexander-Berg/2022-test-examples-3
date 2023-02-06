#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    DeliveryBucket,
    DeliveryOption,
    DynamicShop,
    Offer,
    Outlet,
    OutletDeliveryOption,
    PickupBucket,
    PickupOption,
    Shop,
)

from core.types.offer import OfferDimensions
from core.types.sku import MarketSku, BlueOffer
from core.types.taxes import Vat
from core.testcase import TestCase, main

USUAL_WAREHOUSE_ID = 145
USUAL_OUTLET_ID = 1
DELIVERY_SERVICE_ID = 103
PICKUP_BUCKET_ID = 501
DC_PICKUP_BUCKET_ID = 4

PRIME_OFFER = {'width': 30, 'height': 10, 'length': 20, 'weight': 5}
BUYBOX_OFFER = {'width': 40, 'height': 20, 'length': 10, 'weight': 7}

DSBS_FESH = 6
DSBS_DATAFEED = 60

DSBS_BUYBOX_EXPENSIVE_FESH = 11
DSBS_BUYBOX_EXPENSIVE_DATAFEED = 110

DSBS_BUYBOX_CHEAP_FESH = 8
DSBS_BUYBOX_CHEAP_DATAFEED = 80

WHITE_FESH = 7
WHITE_DATAFEED = 7

DSBS_MSKU = 101010
DSBS_HYPERID = 10

DSBS_BUYBOX_MSKU = 101020


class _Shops(object):
    blue_virtual_shop = Shop(
        fesh=1,
        datafeed_id=1,
        priority_region=213,
        name='virtual_shop',
        fulfillment_virtual=True,
        delivery_service_outlets=[
            USUAL_OUTLET_ID,
        ],
        cpa=Shop.CPA_REAL,
        virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
    )

    prime_blue_shop_1 = Shop(
        fesh=2,
        datafeed_id=2,
        priority_region=213,
        name='prime_blue_shop_1',
        supplier_type=Shop.THIRD_PARTY,
        fulfillment_program=True,
        blue=Shop.BLUE_REAL,
        warehouse_id=145,
    )

    prime_blue_shop_2 = Shop(
        fesh=3,
        datafeed_id=3,
        priority_region=213,
        name='prime_blue_shop_2',
        supplier_type=Shop.FIRST_PARTY,
        fulfillment_program=True,
        blue=Shop.BLUE_REAL,
        cpa=Shop.CPA_REAL,
        warehouse_id=145,
    )

    prime_white_shop = Shop(
        fesh=WHITE_FESH,
        datafeed_id=WHITE_DATAFEED,
        priority_region=213,
        regions=[213],
        name='white Shop',
        client_id=11,
    )

    prime_dsbs_shop = Shop(
        fesh=DSBS_FESH,
        datafeed_id=DSBS_DATAFEED,
        priority_region=213,
        regions=[213],
        name='DSBS Shop',
        client_id=12,
        cpa=Shop.CPA_REAL,
        cpc=Shop.CPC_NO,
    )

    buybox_blue_shop_1 = Shop(
        fesh=4,
        datafeed_id=4,
        priority_region=213,
        name='buybox_blue_shop_1',
        supplier_type=Shop.THIRD_PARTY,
        fulfillment_program=True,
        blue=Shop.BLUE_REAL,
        warehouse_id=145,
    )

    buybox_blue_shop_2 = Shop(
        fesh=5,
        datafeed_id=5,
        priority_region=213,
        name='buybox_blue_shop_2',
        supplier_type=Shop.THIRD_PARTY,
        fulfillment_program=True,
        blue=Shop.BLUE_REAL,
        warehouse_id=145,
    )

    buybox_dsbs_shop_1 = Shop(
        fesh=DSBS_BUYBOX_EXPENSIVE_FESH,
        datafeed_id=DSBS_BUYBOX_EXPENSIVE_DATAFEED,
        priority_region=213,
        regions=[213],
        name='DSBS buybox shop 1',
        client_id=13,
        cpa=Shop.CPA_REAL,
        cpc=Shop.CPC_NO,
    )

    buybox_dsbs_shop_2 = Shop(
        fesh=DSBS_BUYBOX_CHEAP_FESH,
        datafeed_id=DSBS_BUYBOX_CHEAP_DATAFEED,
        priority_region=213,
        regions=[213],
        name='DSBS buybox shop 2',
        client_id=14,
        cpa=Shop.CPA_REAL,
        cpc=Shop.CPC_NO,
    )


class _Offers(object):
    prime_sku1 = BlueOffer(
        price=10,
        vat=Vat.VAT_10,
        feedid=2,
        waremd5='Sku1Price10-iLVm1Goleg',
        weight=PRIME_OFFER['weight'],
        dimensions=OfferDimensions(
            length=PRIME_OFFER['length'], width=PRIME_OFFER['width'], height=PRIME_OFFER['height']
        ),
        pickup_buckets=[PICKUP_BUCKET_ID],
        post_term_delivery=True,
    )

    prime_sku2 = BlueOffer(
        price=5,
        vat=Vat.VAT_10,
        feedid=3,
        waremd5='Sku2Price5-IiLVm1Goleg',
        weight=PRIME_OFFER['weight'],
        dimensions=OfferDimensions(
            length=PRIME_OFFER['length'], width=PRIME_OFFER['width'], height=PRIME_OFFER['height']
        ),
        pickup_buckets=[PICKUP_BUCKET_ID],
        post_term_delivery=True,
    )

    prime_white_offer = Offer(
        price=45,
        title='prime_red_smartphone_on_white',
        hyperid=DSBS_HYPERID,
        fesh=WHITE_FESH,
        waremd5='White_offer_1111111ggg',
        weight=PRIME_OFFER['weight'],
        dimensions=OfferDimensions(
            length=PRIME_OFFER['length'], width=PRIME_OFFER['width'], height=PRIME_OFFER['height']
        ),
        pickup_buckets=[PICKUP_BUCKET_ID],
        pickup_option=DeliveryOption(price=30, day_from=1, day_to=7, order_before=10),
        post_term_delivery=True,
    )

    prime_dsbs_offer = Offer(
        price=30,
        title="prime_red_smartphone_dsbs",
        hyperid=DSBS_HYPERID,
        fesh=DSBS_FESH,
        waremd5='DSBS_offer_11111111ggg',
        sku=DSBS_MSKU,
        cpa=Offer.CPA_REAL,
        weight=PRIME_OFFER['weight'],
        dimensions=OfferDimensions(
            length=PRIME_OFFER['length'], width=PRIME_OFFER['width'], height=PRIME_OFFER['height']
        ),
        pickup_buckets=[PICKUP_BUCKET_ID],
        pickup_option=DeliveryOption(price=30, day_from=1, day_to=7, order_before=10),
        post_term_delivery=True,
    )

    buybox_sku_expensive = BlueOffer(
        price=55,
        vat=Vat.VAT_18,
        feedid=4,
        offerid='blue.offer.2.1',
        waremd5='Sku2Price55-iLVm1Goleg',
        weight=BUYBOX_OFFER['weight'],
        dimensions=OfferDimensions(
            length=BUYBOX_OFFER['length'], width=BUYBOX_OFFER['width'], height=BUYBOX_OFFER['height']
        ),
        pickup_buckets=[PICKUP_BUCKET_ID],
        post_term_delivery=True,
    )

    buybox_sku_cheap = BlueOffer(
        price=45,
        vat=Vat.VAT_18,
        feedid=5,
        offerid='blue.offer.2.2',
        waremd5='Sku2Price45-iLVm1Goleg',
        weight=BUYBOX_OFFER['weight'],
        dimensions=OfferDimensions(
            length=BUYBOX_OFFER['length'], width=BUYBOX_OFFER['width'], height=BUYBOX_OFFER['height']
        ),
        pickup_buckets=[PICKUP_BUCKET_ID],
        post_term_delivery=True,
    )

    dsbs_buybox_offer_expensive = Offer(
        price=200,
        title="dsbs_buybox_offer",
        hyperid=DSBS_HYPERID,
        fesh=DSBS_BUYBOX_EXPENSIVE_FESH,
        waremd5='DSBS_offer_buybox_1ggg',
        sku=DSBS_BUYBOX_MSKU,
        cpa=Offer.CPA_REAL,
        weight=BUYBOX_OFFER['weight'],
        dimensions=OfferDimensions(
            length=BUYBOX_OFFER['length'], width=BUYBOX_OFFER['width'], height=BUYBOX_OFFER['height']
        ),
        pickup_buckets=[PICKUP_BUCKET_ID],
        pickup_option=DeliveryOption(price=30, day_from=1, day_to=7, order_before=10),
        post_term_delivery=True,
    )

    dsbs_buybox_offer_cheap = Offer(
        price=150,
        title="dsbs_buybox_offer",
        hyperid=DSBS_HYPERID,
        fesh=DSBS_BUYBOX_CHEAP_FESH,
        waremd5='DSBS_offer_buybox_2ggg',
        sku=DSBS_BUYBOX_MSKU,
        cpa=Offer.CPA_REAL,
        weight=BUYBOX_OFFER['weight'],
        dimensions=OfferDimensions(
            length=BUYBOX_OFFER['length'], width=BUYBOX_OFFER['width'], height=BUYBOX_OFFER['height']
        ),
        pickup_buckets=[PICKUP_BUCKET_ID],
        pickup_option=DeliveryOption(price=30, day_from=1, day_to=7, order_before=10),
        post_term_delivery=True,
    )


class T(TestCase):
    def check_shop(self, response, shop_type, shop_id):
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                shop_type: {
                    "entity": "shop",
                    "id": shop_id,
                },
            },
        )

    def check_shop_missed(self, response, shop_type, shop_id):
        self.assertFragmentNotIn(
            response,
            {
                "entity": "offer",
                shop_type: {
                    "entity": "shop",
                    "id": shop_id,
                },
            },
        )

    def check_offer_request(self, request, ware_id):
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {"results": [{"entity": "sku", "offers": {"items": [{"wareId": ware_id}]}}]},
            allow_different_len=False,
        )

    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']
        cls.index.outlets += [
            Outlet(
                point_id=USUAL_OUTLET_ID,
                delivery_service_id=DELIVERY_SERVICE_ID,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    shipper_id=DELIVERY_SERVICE_ID,
                    day_from=1,
                    day_to=1,
                    order_before=2,
                    work_in_holiday=True,
                    price=100,
                ),
                working_days=[i for i in range(10)],
            ),
        ]

        cls.index.shops += [
            _Shops.blue_virtual_shop,
            _Shops.prime_blue_shop_1,
            _Shops.prime_blue_shop_2,
            _Shops.prime_white_shop,
            _Shops.prime_dsbs_shop,
            _Shops.buybox_blue_shop_1,
            _Shops.buybox_blue_shop_2,
            _Shops.buybox_dsbs_shop_1,
            _Shops.buybox_dsbs_shop_2,
        ]

        cls.index.offers += [
            _Offers.prime_white_offer,
            _Offers.prime_dsbs_offer,
            _Offers.dsbs_buybox_offer_expensive,
            _Offers.dsbs_buybox_offer_cheap,
        ]

        cls.index.mskus += [
            MarketSku(
                title="prime_red_smartphone",
                hyperid=1,
                sku=10108,
                blue_offers=[_Offers.prime_sku1],
            ),
            MarketSku(
                title="prime_another_red_smartphone",
                hyperid=1,
                sku=10109,
                blue_offers=[_Offers.prime_sku2],
            ),
            MarketSku(
                title="prime_dsbs_red_smartphone",
                hyperid=DSBS_HYPERID,
                sku=DSBS_MSKU,
            ),
            MarketSku(
                title="blue_buybox_title",
                hyperid=1,
                sku=101011,
                blue_offers=[_Offers.buybox_sku_expensive, _Offers.buybox_sku_cheap],
            ),
            MarketSku(
                title="dsbs_buybox_title",
                hyperid=DSBS_HYPERID,
                sku=DSBS_BUYBOX_MSKU,
            ),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=PICKUP_BUCKET_ID,
                dc_bucket_id=DC_PICKUP_BUCKET_ID,
                fesh=1,
                carriers=[DELIVERY_SERVICE_ID],
                options=[
                    PickupOption(outlet_id=USUAL_OUTLET_ID, day_from=1, day_to=2, price=5),
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
        ]

    def test_show_hidden_blue_suppliers(self):
        """
        Что проверяем: отключение скрытия для списка поставщиков, переданных во флаге, на синем прайме
        """
        base_request = 'place=prime&text=red_smartphone&rgb=blue&allow-collapsing=0&numdoc=1000'

        "Проверяем наличие поставщиков"
        response = self.report.request_json(base_request)
        for supplier_id in [2, 3]:
            self.check_shop(response, "supplier", supplier_id)

        "Отключаем поставщиков"
        self.dynamic.market_dynamic.disabled_blue_suppliers += [
            DynamicShop(2),
            DynamicShop(3),
        ]

        "Проверяем, что поставщики отключились"
        response = self.report.request_json(base_request)
        for supplier_id in [2, 3]:
            self.check_shop_missed(response, "supplier", supplier_id)

        "Проверяем отключение фильтрации для списка поставщиков"
        for ignore_list in [[2], [3], [2, 3]]:
            rearr = "&rearr-factors=market_show_hidden_suppliers={}".format(",".join(str(i) for i in ignore_list))
            response = self.report.request_json(base_request + rearr)
            for supplier_id in ignore_list:
                self.check_shop(response, "supplier", supplier_id)

    def test_show_hidden_dsbs_shop_on_white(self):
        """
        Что проверяем: отключение скрытия для дсбс магазинов на белом
        """
        base_request = 'place=prime&text=red_smartphone&rgb=white&allow-collapsing=0&numdoc=1000&rids=213&rearr-factors=market_metadoc_search=no'
        shop_id = DSBS_FESH
        "Проверяем наличие поставщиков"
        response = self.report.request_json(base_request)
        self.check_shop(response, "shop", shop_id)

        "Отключаем поставщика"
        self.dynamic.market_dynamic.disabled_cpa_shops += [
            DynamicShop(shop_id),
        ]

        "Проверяем, что поставщик отключился"
        response = self.report.request_json(base_request)
        self.check_shop_missed(response, "shop", shop_id)

        "Проверяем отключение фильтрации для списка поставщиков"
        rearr = "&rearr-factors=market_show_hidden_suppliers={}".format(shop_id)
        response = self.report.request_json(base_request + rearr)
        self.check_shop(response, "shop", shop_id)

    def test_show_hidden_dsbs_shop_on_blue(self):
        """
        Что проверяем: отключение скрытия для дсбс магазинов на синем
        """
        base_request = 'place=prime&text=red_smartphone&rgb=blue&allow-collapsing=0&numdoc=1000&rids=213&'
        shop_id = DSBS_FESH

        "Проверяем наличие поставщиков"
        response = self.report.request_json(base_request)
        self.check_shop(response, "supplier", shop_id)

        "Отключаем поставщика"
        self.dynamic.market_dynamic.disabled_cpa_shops += [
            DynamicShop(shop_id),
        ]

        "Проверяем, что поставщик отключился"
        response = self.report.request_json(base_request)
        self.check_shop_missed(response, "supplier", shop_id)

        "Проверяем отключение фильтрации для списка поставщиков"
        rearr = "rearr-factors=market_show_hidden_suppliers={}".format(shop_id)
        response = self.report.request_json(base_request + rearr)
        self.check_shop(response, "supplier", shop_id)

    def test_show_hidden_blue_supplier_in_boybox(self):
        """
        Что проверяем: отключение фильтрации конкретных поставщиков в байбоксе при использовании флага market_show_hidden_suppliers
        """
        supplier_id = 5
        for pipeline in [0, 1]:
            base_request = "place=sku_offers&rgb=blue&market-sku={msku}&rids=213&rearr-factors=use_new_jump_table_pipeline={pipeline};".format(
                msku=101011, pipeline=pipeline
            )

            "Проверяем дефолтный оффер"
            self.check_offer_request(base_request, _Offers.buybox_sku_cheap.waremd5)

            "Отключаем поставщика дефолтного оффера"
            self.dynamic.market_dynamic.disabled_blue_suppliers += [
                DynamicShop(supplier_id),
            ]

            "Проверяем, что дефолтный оффер изменился"
            self.check_offer_request(base_request, _Offers.buybox_sku_expensive.waremd5)

            "Проверяем, что при использовании флага поставщик не был отфильтрован, и дефолтный оффер стал прежним"
            self.check_offer_request(
                base_request + "market_show_hidden_suppliers={}".format(supplier_id), _Offers.buybox_sku_cheap.waremd5
            )

            self.dynamic.market_dynamic.disabled_blue_suppliers.clear()

    def test_show_hidden_dsbs_supplier_in_boybox(self):
        """
        Что проверяем: отключение фильтрации конкретных поставщиков в байбоксе при использовании флага market_show_hidden_suppliers
        """
        supplier_id = DSBS_BUYBOX_CHEAP_FESH
        for pipeline in [0, 1]:
            base_request = "place=sku_offers&rgb=blue&market-sku={msku}&rids=213&rearr-factors=use_new_jump_table_pipeline={pipeline};".format(
                msku=DSBS_BUYBOX_MSKU, pipeline=pipeline
            )

            "Проверяем дефолтный оффер"
            self.check_offer_request(base_request, _Offers.dsbs_buybox_offer_cheap.waremd5)

            "Отключаем поставщика дефолтного оффера"
            self.dynamic.market_dynamic.disabled_cpa_shops += [
                DynamicShop(supplier_id),
            ]

            "Проверяем, что дефолтный оффер изменился"
            self.check_offer_request(base_request, _Offers.dsbs_buybox_offer_expensive.waremd5)

            "Проверяем, что при использовании флага поставщик не был отфильтрован, и дефолтный оффер стал прежним"
            self.check_offer_request(
                base_request + "market_show_hidden_suppliers={}".format(supplier_id),
                _Offers.dsbs_buybox_offer_cheap.waremd5,
            )

            self.dynamic.market_dynamic.disabled_cpa_shops.clear()

    def test_hidden_shop_for_cpc(self):
        """
        Что проверяем: не собирались открывать cpc магазины флагом market_show_hidden_suppliers, достаточно cpa, в дальнейшем можно добавить и cpc магазины, если потребуется
        """
        base_request = 'place=prime&text=red_smartphone&rgb=white&allow-collapsing=0&numdoc=1000&rids=213'
        shop_id = WHITE_FESH
        "Проверяем наличие поставщиков"
        response = self.report.request_json(base_request)
        self.check_shop(response, "shop", shop_id)

        "Отключаем cpc поставщика"
        self.dynamic.market_dynamic.disabled_cpc_shops += [
            DynamicShop(shop_id),
        ]

        "Проверяем, что поставщик отключился"
        response = self.report.request_json(base_request)
        self.check_shop_missed(response, "shop", shop_id)

        "Проверяем, что флаг не повлиял на скрытие"
        rearr = "&rearr-factors=market_show_hidden_suppliers={}".format(shop_id)
        response = self.report.request_json(base_request + rearr)
        self.check_shop_missed(response, "shop", shop_id)


if __name__ == '__main__':
    main()
