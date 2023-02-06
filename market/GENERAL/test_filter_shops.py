#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BlueOffer,
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryOption,
    DeliveryServiceRegionToRegionInfo,
    DynamicDaysSet,
    DynamicDeliveryRestriction,
    DynamicDeliveryServiceInfo,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseDelivery,
    DynamicWarehouseInfo,
    MarketSku,
    Model,
    Offer,
    Region,
    Shop,
    Tax,
)
from core.types.offer import OfferDimensions
from core.testcase import TestCase, main


class _Rids:
    russia = 225
    moscow = 213


class _DeliveryServices:
    internal = 99


class _Params:
    category_id = 1


class _Categories:
    class _White:
        simple = 2

    class _Blue:
        simple = 3


class _Feshes:
    class _White:
        simple = 20

    class _Blue:
        virtual = 30
        simple = 31


class _Feeds:
    class _White:
        simple = 200

    class _Blue:
        virtual = 300
        simple = 310


class _Warehouses:
    class _White:
        simple = 2000

    class _Blue:
        simple = 3000


class _Shops:
    class _White:
        def create(fesh, datafeed_id, warehouse_id, priority_region, regions, name):
            return Shop(
                fesh=fesh,
                datafeed_id=datafeed_id,
                warehouse_id=warehouse_id,
                priority_region=priority_region,
                regions=regions,
                client_id=fesh,
                cpa=Shop.CPA_REAL,
                name=name,
            )

        simple = create(
            fesh=_Feshes._White.simple,
            datafeed_id=_Feeds._White.simple,
            warehouse_id=_Warehouses._White.simple,
            priority_region=_Rids.moscow,
            regions=[_Rids.moscow],
            name='White simple shop',
        )

    class _Blue:
        virtual = Shop(
            fesh=_Feshes._Blue.virtual,
            datafeed_id=_Feeds._Blue.virtual,
            priority_region=_Rids.moscow,
            tax_system=Tax.OSN,
            fulfillment_virtual=True,
            cpa=Shop.CPA_REAL,
            virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
            name='Blue virtual shop',
        )

        simple = Shop(
            fesh=_Feshes._Blue.simple,
            datafeed_id=_Feeds._Blue.simple,
            warehouse_id=_Warehouses._Blue.simple,
            priority_region=_Rids.moscow,
            client_id=_Feshes._Blue.simple,
            cpa=Shop.CPA_REAL,
            medicine_courier=True,
            blue=Shop.BLUE_REAL,
            supplier_type=Shop.THIRD_PARTY,
            name='Blue simple shop',
        )


class _Buckets:
    class _White:
        simple_id = 20000

        def create_delivery(bucket_id, shop):
            return DeliveryBucket(
                bucket_id=bucket_id,
                dc_bucket_id=bucket_id,
                fesh=shop.fesh,
                carriers=[_DeliveryServices.internal],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            )

        simple = create_delivery(bucket_id=simple_id, shop=_Shops._White.simple)

    class _Blue:
        simple_id = 31000

        def create_delivery(bucket_id, shop):
            return DeliveryBucket(
                bucket_id=bucket_id,
                dc_bucket_id=bucket_id,
                fesh=shop.fesh,
                carriers=[_DeliveryServices.internal],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            )

        simple = create_delivery(bucket_id=simple_id, shop=_Shops._Blue.simple)


class _Mskus:
    class _White:
        simple_id = 200000

        def create(title, sku, hyperid):
            return MarketSku(title=title, sku=sku, hyperid=hyperid)

        simple = create(title="White simple MSKU", sku=simple_id, hyperid=_Categories._White.simple)

    class _Blue:
        simple_id = 310000

        def create(title, sku, hyperid, blue_offers):
            return MarketSku(title=title, sku=sku, hyperid=hyperid, blue_offers=blue_offers)

        blue_offer = BlueOffer(
            waremd5='simple_blue__________g',
            fesh=_Feshes._Blue.simple,
            feedid=_Feeds._Blue.simple,
            supplier_id=_Shops._Blue.simple.fesh,
            sku=simple_id,
            hyperid=_Categories._Blue.simple,
            delivery_buckets=[_Buckets._Blue.simple.bucket_id],
            price=10,
            weight=1,
            dimensions=OfferDimensions(length=3, width=3, height=3),
            delivery_options=[DeliveryOption(price=100, day_from=1, day_to=2, order_before=14)],
            stock_store_count=10,
            cpa=Offer.CPA_REAL,
        )

        simple = create(
            title="Blue simple MSKU", sku=simple_id, hyperid=_Categories._Blue.simple, blue_offers=[blue_offer]
        )


class _Models:
    class _White:
        def create(hid, msku):
            return Model(hid=hid, hyperid=msku.hyperid)

        simple = create(hid=_Params.category_id, msku=_Mskus._White.simple)

    class _Blue:
        def create(hid, msku):
            return Model(hid=hid, hyperid=msku.hyperid)

        simple = create(hid=_Params.category_id, msku=_Mskus._Blue.simple)


class _Offers:
    class _White:
        def create(
            waremd5,
            shop,
            msku,
            delivery_buckets,
        ):
            return Offer(
                waremd5=waremd5,
                fesh=shop.fesh,
                supplier_id=shop.fesh,
                sku=msku.sku,
                hyperid=msku.hyperid,
                price=10,
                weight=1,
                dimensions=OfferDimensions(length=3, width=3, height=3),
                delivery_options=[DeliveryOption(price=100, day_from=1, day_to=2, order_before=14)],
                stock_store_count=10,
                cpa=Offer.CPA_REAL,
                delivery_buckets=delivery_buckets,
            )

        simple = create(
            waremd5='simple_white_________g',
            shop=_Shops._White.simple,
            msku=_Mskus._White.simple,
            delivery_buckets=[_Buckets._White.simple.bucket_id],
        )


class _Requests:
    offer_info_hide = (
        'place=offerinfo'
        '&pp=18'
        '&rids=213'
        '&offerid={offer_id}'
        '&show-urls=direct'
        '&regset=1'
        '&rearr-factors=market_not_prescription_drugs_delivery=1'  # no restriction
        '&rearr-factors=enable_prescription_drugs_delivery=1'  # no restriction
        '&rearr-factors=market_hidden_shops={shop_id1},{shop_id2}'
        '&debug=1'
    )

    offer_info_show = (
        'place=offerinfo'
        '&pp=18'
        '&rids=213'
        '&offerid={offer_id}'
        '&show-urls=direct'
        '&regset=1'
        '&rearr-factors=market_not_prescription_drugs_delivery=1'  # no restriction
        '&rearr-factors=enable_prescription_drugs_delivery=1'  # no restriction
        '&debug=1'
    )


class T(TestCase):
    """
    Набор тестов для скрытия офферов по магазину:
    &rearr-factors=market_hidden_shops, на вход передается список идентефикаторов
    см.: https://st.yandex-team.ru/MARKETOUT-42846
    """

    @classmethod
    def prepare_regions(cls):
        cls.index.regiontree += [Region(rid=_Rids.moscow, name="Moscow", tz_offset=10800)]

    @classmethod
    def prepare_shops(cls):
        cls.index.shops += [
            _Shops._White.simple,
        ]

        cls.index.shops += [_Shops._Blue.virtual, _Shops._Blue.simple]

    @classmethod
    def prepare_mskus(cls):
        cls.index.mskus += [_Mskus._White.simple]

        cls.index.mskus += [_Mskus._Blue.simple]

    @classmethod
    def prepare_offers(cls):
        cls.index.offers += [_Offers._White.simple]

    @classmethod
    def prepare_models(cls):
        cls.index.models += [_Models._White.simple]

        cls.index.models += [_Models._Blue.simple]

    @classmethod
    def prepare_delivery_buckets(cls):
        cls.index.delivery_buckets += [_Buckets._White.simple]

        cls.index.delivery_buckets += [_Buckets._Blue.simple]

    @classmethod
    def prepare_lms(cls):
        cls.settings.lms_autogenerate = False
        cls.dynamic.lms += [DynamicDaysSet(key=1, days=[])]

        for wh_id, ds_id, ds_name in [
            (_Warehouses._White.simple, _DeliveryServices.internal, 'shop_1_delivery_service'),
            (_Warehouses._Blue.simple, _DeliveryServices.internal, 'shop_blue_delivery_service'),
        ]:
            cls.dynamic.lms += [
                DynamicDeliveryServiceInfo(
                    id=ds_id,
                    name=ds_name,
                    region_to_region_info=[
                        DeliveryServiceRegionToRegionInfo(region_from=_Rids.moscow, region_to=_Rids.russia, days_key=1)
                    ],
                ),
                DynamicWarehouseInfo(id=wh_id, home_region=_Rids.moscow, holidays_days_set_key=2),
                DynamicWarehouseAndDeliveryServiceInfo(
                    warehouse_id=wh_id,
                    delivery_service_id=ds_id,
                    operation_time=0,
                    date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=2, region_to=_Rids.russia)],
                ),
            ]

    @classmethod
    def prepare_blue_respond(cls):
        cls.delivery_calc.on_request_offer_buckets(
            weight=1, height=3, length=3, width=3, warehouse_id=_Warehouses._Blue.simple
        ).respond([_Buckets._Blue.simple_id], [], [])

    @classmethod
    def prepare_nordstream(cls):
        # Sets up delivery schedule response for blue offers
        cls.dynamic.nordstream += [
            DynamicWarehouseDelivery(
                _Warehouses._Blue.simple,
                {
                    _Rids.moscow: [
                        DynamicDeliveryRestriction(
                            max_phys_weight=40000,
                            max_dim_sum=250,
                            max_dimensions=[100, 100, 100],
                            min_days=1,
                            max_days=1,
                        ),
                    ],
                },
            )
        ]

    # White tests

    def test_white_offer_hide_offer(self):
        """
        Проверям что оффер скрыт с соответсвующей причной, если магазин оффера
        находится в черном списке.
        """

        request = _Requests.offer_info_hide.format(
            offer_id=_Offers._White.simple.ware_md5,
            shop_id1=_Shops._White.simple.fesh,
            shop_id2=_Shops._Blue.simple.fesh,
        )

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [],
                    "shops": 0,
                    "total": 0,
                    "totalFreeOffers": 0,
                    "totalModels": 0,
                    "totalOffers": 0,
                    "totalOffersBeforeFilters": 0,
                    "totalPassedAllGlFilters": 0,
                    "totalShopsBeforeFilters": 0,
                },
            },
            allow_different_len=False,
        )
        self.assertFragmentIn(response, {"filters": {"OFFER_FILTERED_OUT_BY_HIDDEN_SHOP": 1}})

    def test_white_offer_show_offer(self):
        """
        Проверям что без внесения в список запрещенных магазинов оффер есть на выдаче
        """

        request = _Requests.offer_info_show.format(offer_id=_Offers._White.simple.ware_md5)

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [{"wareId": _Offers._White.simple.ware_md5}],
                    "shops": 1,
                    "total": 1,
                    "totalOffers": 1,
                    "totalOffersBeforeFilters": 1,
                    "totalPassedAllGlFilters": 1,
                    "totalShopsBeforeFilters": 1,
                },
            },
            allow_different_len=False,
        )

    # Blue tests

    def test_blue_offer_hide_offer(self):
        """
        Проверям что оффер скрыт с соответсвующей причной, если магазин оффера
        находится в черном списке.
        """

        request = _Requests.offer_info_hide.format(
            offer_id=_Mskus._Blue.blue_offer.waremd5,
            shop_id1=_Shops._White.simple.fesh,
            shop_id2=_Shops._Blue.simple.fesh,
        )

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [],
                    "shops": 0,
                    "total": 0,
                    "totalOffers": 0,
                    "totalOffersBeforeFilters": 0,
                    "totalPassedAllGlFilters": 0,
                    "totalShopsBeforeFilters": 0,
                },
            },
            allow_different_len=False,
        )
        self.assertFragmentIn(response, {"filters": {"OFFER_FILTERED_OUT_BY_HIDDEN_SHOP": 1}})

    def test_blue_offer_show_offer(self):
        """
        Проверям что без внесения в список запрещенных магазинов оффер есть на выдаче
        """

        request = _Requests.offer_info_show.format(offer_id=_Mskus._Blue.blue_offer.waremd5)

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [{"wareId": _Mskus._Blue.blue_offer.waremd5}],
                    "shops": 1,
                    "total": 1,
                    "totalOffers": 1,
                    "totalOffersBeforeFilters": 1,
                    "totalPassedAllGlFilters": 1,
                    "totalShopsBeforeFilters": 1,
                },
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_hide_offer(cls):
        cls.index.offers += [
            Offer(cpa=Offer.CPA_REAL, fesh=1337, hid=1337, title="да что угодно"),
            Offer(cpa=Offer.CPA_REAL, fesh=228, hid=12494574, title="кружка по госту"),
            Offer(cpa=Offer.CPA_REAL, fesh=2260834, hid=12494574, business_id=890788, title="кружка мусор"),
        ]

    def test_hide_kruzhka_offer(self):
        """
        Скрываем кружки
        """
        response = self.report.request_json('place=prime&text=кружка')
        self.assertFragmentIn(response, {"shop": {"id": 2260834}})

        response = self.report.request_json('place=prime&hid=12494574')
        self.assertFragmentIn(response, {"shop": {"id": 2260834}})

        response = self.report.request_json('place=prime&text=угодно&hid=1337')
        self.assertFragmentIn(response, {"shop": {"id": 1337}})
        self.assertFragmentIn(response, {"shop": {"id": 1337}})
        self.assertFragmentNotIn(response, {"shop": {"id": 2260834}})

        response = self.report.request_json('place=prime&hid=12494574&rearr-factors=market_hide_kruzhka_shop=0')
        self.assertFragmentIn(response, {"shop": {"id": 2260834}})

        response = self.report.request_json('place=prime&text=кружка&rearr-factors=market_hide_kruzhka_shop=0')
        self.assertFragmentIn(response, {"shop": {"id": 2260834}})

        response = self.report.request_json('place=prime&fesh=890788')
        self.assertFragmentIn(response, {"shop": {"id": 2260834}})


if __name__ == '__main__':
    main()
