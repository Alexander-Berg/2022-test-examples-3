#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa


from core.matcher import EmptyList

from core.types import (
    BlueOffer,
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryOption,
    DeliveryServiceRegionToRegionInfo,
    DynamicDaysSet,
    DynamicDeliveryServiceInfo,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseInfo,
    DynamicWarehouseToWarehouseInfo,
    MarketSku,
    Model,
    Offer,
    Outlet,
    PickupBucket,
    PickupOption,
    Region,
    RegionalDelivery,
    Shop,
    TimeInfo,
    WarehouseWithPriority,
    WarehousesPriorityInRegion,
)

from core.testcase import TestCase, main


class _Rids:
    russia = 225
    moscow = 213


class _Categories:
    usual = 123456
    medicine = 15758037


class _Hyperids:
    usual_pickup = 1
    medical_pickup = 2
    medical_courier = 3
    medical_express = 4


class _Feshes:
    class _White:
        usual_pickup = 10
        medical_pickup = 20
        medical_courier = 30
        medical_express = 40

    class _Blue:
        medical_pickup = 21
        medical_courier = 31
        medical_express = 41


class _Feeds:
    class _White:
        usual_pickup = 100
        medical_pickup = 200
        medical_courier = 300
        medical_express = 400

    class _Blue:
        medical_pickup = 201
        medical_courier = 301
        medical_express = 401


class _Skus:
    class _White:
        usual_pickup = 1000
        medical_pickup = 2000
        medical_courier = 3000
        medical_express = 4000

    class _Blue:
        medical_pickup = 2001
        medical_courier = 3001
        medical_express = 4001


class _Buckets:
    class _White:
        usual_pickup = 10000
        medical_pickup = 20000
        medical_courier = 30000
        medical_express = 40000

    class _Blue:
        medical_pickup = 20001
        medical_courier = 30001
        medical_express = 40001


class _Pickup:
    warehouse_id = 200000
    supplier_id = 2000000
    delivery_service_id = 20000000


class _Courier:
    warehouse_id = 300000
    supplier_id = 3000000
    delivery_service_id = 30000000


class _Express:
    warehouse_id = 400000
    supplier_id = 4000000
    delivery_service_id = 40000000


class _Shops:
    class _White:
        usual_pickup = Shop(
            fesh=_Feshes._White.usual_pickup,
            datafeed_id=_Feeds._White.usual_pickup,
            priority_region=_Rids.moscow,
            regions=[_Rids.moscow],
            medicine_courier=False,
            name='White usual pickup shop',
        )

        medical_pickup = Shop(
            fesh=_Feshes._White.medical_pickup,
            datafeed_id=_Feeds._White.medical_pickup,
            priority_region=_Rids.moscow,
            regions=[_Rids.moscow],
            cpa=Shop.CPA_REAL,
            medicine_courier=True,
            name='White medical pickup shop',
        )

        medical_courier = Shop(
            fesh=_Feshes._White.medical_courier,
            datafeed_id=_Feeds._White.medical_courier,
            priority_region=_Rids.moscow,
            regions=[_Rids.moscow],
            cpa=Shop.CPA_REAL,
            medicine_courier=True,
            name='White medical courier shop',
        )

        medical_express = Shop(
            fesh=_Feshes._White.medical_express,
            datafeed_id=_Feeds._White.medical_express,
            priority_region=_Rids.moscow,
            regions=[_Rids.moscow],
            cpa=Shop.CPA_REAL,
            medicine_courier=True,
            warehouse_id=_Express.warehouse_id,
            name='White medical express shop',
        )

    class _Blue:
        medical_pickup = Shop(
            fesh=_Feshes._Blue.medical_pickup,
            datafeed_id=_Feeds._Blue.medical_pickup,
            priority_region=_Rids.moscow,
            regions=[_Rids.moscow],
            cpa=Shop.CPA_REAL,
            medicine_courier=True,
            blue=Shop.BLUE_REAL,
            warehouse_id=_Pickup.warehouse_id,
            name='Blue medical pickup shop',
        )

        medical_courier = Shop(
            fesh=_Feshes._Blue.medical_courier,
            datafeed_id=_Feeds._Blue.medical_courier,
            priority_region=_Rids.moscow,
            regions=[_Rids.moscow],
            cpa=Shop.CPA_REAL,
            medicine_courier=True,
            blue=Shop.BLUE_REAL,
            warehouse_id=_Courier.warehouse_id,
            name='White medical courier shop',
        )

        medical_express = Shop(
            fesh=_Feshes._Blue.medical_express,
            datafeed_id=_Feeds._Blue.medical_express,
            priority_region=_Rids.moscow,
            regions=[_Rids.moscow],
            cpa=Shop.CPA_REAL,
            medicine_courier=True,
            blue=Shop.BLUE_REAL,
            warehouse_id=_Express.warehouse_id,
            name='Blue medical express shop',
        )


class _BlueOffers:
    medical_pickup = BlueOffer(
        waremd5='blue_med_pickup______g',
        hyperid=_Hyperids.medical_pickup,
        fesh=_Feshes._Blue.medical_pickup,
        feedid=_Feeds._Blue.medical_pickup,
        pickup_buckets=[_Buckets._Blue.medical_pickup],
        has_delivery_options=False,
        pickup=True,
        title="Blue medical offer with pickup",
        is_medicine=True,
    )

    medical_courier = BlueOffer(
        waremd5='blue_med_courier_____g',
        hyperid=_Hyperids.medical_courier,
        fesh=_Feshes._Blue.medical_courier,
        feedid=_Feeds._Blue.medical_courier,
        delivery_buckets=[_Buckets._Blue.medical_courier],
        title="Blue medical offer with courier",
        is_medicine=True,
    )

    medical_express = BlueOffer(
        waremd5='blue_med_express_____g',
        hyperid=_Hyperids.medical_express,
        fesh=_Feshes._Blue.medical_express,
        feedid=_Feeds._Blue.medical_express,
        delivery_buckets=[_Buckets._Blue.medical_express],
        is_express=True,
        title="Blue medical offer with express",
        is_medicine=True,
    )


class _Mskus:
    class _White:
        usual_pickup = MarketSku(
            hyperid=_Hyperids.usual_pickup, sku=_Skus._White.usual_pickup, title="White usual pickup market Sku"
        )

        medical_pickup = MarketSku(
            hyperid=_Hyperids.medical_pickup, sku=_Skus._White.medical_pickup, title="White medical pickup market Sku"
        )

        medical_courier = MarketSku(
            hyperid=_Hyperids.medical_courier,
            sku=_Skus._White.medical_courier,
            title="White medical courier market Sku",
        )

        medical_express = MarketSku(
            hyperid=_Hyperids.medical_express,
            sku=_Skus._White.medical_express,
            title="White medical express market Sku",
        )

    class _Blue:
        medical_pickup = MarketSku(
            hyperid=_Hyperids.medical_pickup,
            sku=_Skus._Blue.medical_pickup,
            blue_offers=[_BlueOffers.medical_pickup],
            title="Medical pickup market Sku",
        )

        medical_courier = MarketSku(
            hyperid=_Hyperids.medical_courier,
            sku=_Skus._Blue.medical_courier,
            blue_offers=[_BlueOffers.medical_courier],
            title="Blue medical courier market Sku",
        )

        medical_express = MarketSku(
            hyperid=_Hyperids.medical_express,
            sku=_Skus._Blue.medical_express,
            blue_offers=[_BlueOffers.medical_express],
            title="Blue medical express market Sku",
        )


class _WhiteOffers:
    usual_pickup = Offer(
        waremd5='white_usual_pickup___g',
        hyperid=_Hyperids.usual_pickup,
        sku=_Mskus._White.usual_pickup.sku,
        fesh=_Feshes._White.usual_pickup,
        pickup_buckets=[_Buckets._White.usual_pickup],
        has_delivery_options=False,
        pickup=True,
        title="White usual offer with pickup",
    )

    medical_pickup = Offer(
        waremd5='white_med_pickup_____g',
        hyperid=_Hyperids.medical_pickup,
        sku=_Mskus._White.medical_pickup.sku,
        fesh=_Feshes._White.medical_pickup,
        pickup_buckets=[_Buckets._White.medical_pickup],
        has_delivery_options=False,
        pickup=True,
        title="White medical offer with pickup",
        is_medicine=True,
    )

    medical_courier = Offer(
        waremd5='white_med_courier____g',
        hyperid=_Hyperids.medical_courier,
        sku=_Mskus._White.medical_courier.sku,
        fesh=_Feshes._White.medical_courier,
        delivery_buckets=[_Buckets._White.medical_courier],
        title="White medical offer with courier",
        is_medicine=True,
    )

    medical_express = Offer(
        waremd5='white_med_express____g',
        hyperid=_Hyperids.medical_express,
        sku=_Mskus._White.medical_express.sku,
        fesh=_Feshes._White.medical_express,
        delivery_buckets=[_Buckets._White.medical_express],
        is_express=True,
        supplier_id=_Express.supplier_id,
        title="White medical offer with express",
        is_medicine=True,
    )


class _Requests:
    white_prime = (
        'place=prime'
        '&rgb=white'
        '&pp=18'
        '&allow-collapsing=0'
        '&rearr-factors=enable_prescription_drugs_delivery=1'
        '&rearr-factors=market_not_prescription_drugs_delivery=1'
        '&rids={rids}'
        '&market-sku={msku}'
        '&filter-express-delivery={express_delivery}'
    )


class T(TestCase):
    """
    Набор тестов для поиска разных групп офферов с разными видами доставки:
    - https://st.yandex-team.ru/MARKETOUT-38509
    - Допустимые аргументы:
    - filter-offer-type=medicine   - фильтруем по принадлежности к группе мед товаров
                                     если не задан, фильтр не применяется
    - filter-delivery-type=any     - фильтруем по наличию любого вида доставки
    - filter-delivery-type=pickup  - фильтруем по наличию самовывоза
    - filter-delivery-type=courier - фильтруем по наличию курьерской доставки
    - filter-express-delivery=1    - фильтруем по наличию экспресс доставки
    """

    @classmethod
    def prepare_regions(cls):
        cls.index.regiontree += [Region(rid=_Rids.moscow)]

    @classmethod
    def prepare_outlets(cls):
        # White
        cls.index.outlets += [
            Outlet(
                fesh=_Feshes._White.usual_pickup,
                point_id=_Buckets._White.usual_pickup,
                region=_Rids.moscow,
                point_type=Outlet.FOR_PICKUP,
            ),
            Outlet(
                fesh=_Feshes._White.medical_pickup,
                point_id=_Buckets._White.medical_pickup,
                region=_Rids.moscow,
                point_type=Outlet.FOR_PICKUP,
            ),
        ]
        # Blue
        cls.index.outlets += [
            Outlet(
                fesh=_Feshes._Blue.medical_pickup,
                point_id=_Buckets._Blue.medical_pickup,
                region=_Rids.moscow,
                point_type=Outlet.FOR_PICKUP,
            )
        ]

    @classmethod
    def prepare_pickup_buckets(cls):
        # White
        cls.index.pickup_buckets += [
            PickupBucket(
                fesh=_Feshes._White.usual_pickup,
                bucket_id=_Buckets._White.usual_pickup,
                options=[PickupOption(outlet_id=_Buckets._White.usual_pickup)],
                carriers=[_Pickup.delivery_service_id],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                fesh=_Feshes._White.medical_pickup,
                bucket_id=_Buckets._White.medical_pickup,
                options=[PickupOption(outlet_id=_Buckets._White.medical_pickup)],
                carriers=[_Pickup.delivery_service_id],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]
        # Blue
        cls.index.pickup_buckets += [
            PickupBucket(
                fesh=_Feshes._Blue.medical_pickup,
                bucket_id=_Buckets._Blue.medical_pickup,
                options=[PickupOption(outlet_id=_Buckets._Blue.medical_pickup)],
                carriers=[_Pickup.delivery_service_id],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

    @classmethod
    def prepare_delivery_buckets(cls):
        # White
        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=_Buckets._White.medical_courier,
                carriers=[_Courier.delivery_service_id],
                regional_options=[
                    RegionalDelivery(
                        rid=_Rids.moscow,
                        options=[
                            # Courier delivery schedule
                            DeliveryOption(price=5, day_from=1, day_to=2, shop_delivery_price=10)
                        ],
                    )
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=_Buckets._White.medical_express,
                carriers=[_Express.delivery_service_id],
                regional_options=[
                    RegionalDelivery(
                        rid=_Rids.moscow,
                        options=[
                            # Express delivery schedule
                            DeliveryOption(price=5, day_from=1, day_to=1, shop_delivery_price=10)
                        ],
                    )
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]
        # Blue
        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=_Buckets._Blue.medical_courier,
                carriers=[_Courier.delivery_service_id],
                regional_options=[
                    RegionalDelivery(
                        rid=_Rids.moscow,
                        options=[
                            # Courier delivery schedule
                            DeliveryOption(price=5, day_from=1, day_to=2, shop_delivery_price=10)
                        ],
                    )
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=_Buckets._Blue.medical_express,
                carriers=[_Express.delivery_service_id],
                regional_options=[
                    RegionalDelivery(
                        rid=_Rids.moscow,
                        options=[
                            # Express delivery schedule
                            DeliveryOption(price=5, day_from=1, day_to=1, shop_delivery_price=10)
                        ],
                    )
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

    @classmethod
    def prepare_shops(cls):
        # White
        cls.index.shops += [
            _Shops._White.usual_pickup,
            _Shops._White.medical_pickup,
            _Shops._White.medical_courier,
            _Shops._White.medical_express,
        ]
        # Blue
        cls.index.shops += [_Shops._Blue.medical_pickup, _Shops._Blue.medical_courier, _Shops._Blue.medical_express]

    @classmethod
    def prepare_mskus(cls):
        # White
        cls.index.mskus += [
            _Mskus._White.usual_pickup,
            _Mskus._White.medical_pickup,
            _Mskus._White.medical_courier,
            _Mskus._White.medical_express,
        ]
        # Blue
        cls.index.mskus += [_Mskus._Blue.medical_pickup, _Mskus._Blue.medical_courier, _Mskus._Blue.medical_express]

    @classmethod
    def prepare_offers(cls):
        # White
        cls.index.offers += [
            _WhiteOffers.usual_pickup,
            _WhiteOffers.medical_pickup,
            _WhiteOffers.medical_courier,
            _WhiteOffers.medical_express,
        ]

    @classmethod
    def prepare_models(cls):
        cls.index.models += [
            Model(hid=_Categories.usual, hyperid=_Hyperids.usual_pickup),
            Model(
                hid=_Categories.medicine,
                hyperid=_Hyperids.medical_pickup,
            ),
            Model(
                hid=_Categories.medicine,
                hyperid=_Hyperids.medical_courier,
            ),
            Model(
                hid=_Categories.medicine,
                hyperid=_Hyperids.medical_express,
            ),
        ]

    @classmethod
    def prepare_warehouses(cls):
        cls.index.warehouse_priorities += [
            WarehousesPriorityInRegion(
                regions=[_Rids.russia, _Rids.moscow],
                warehouse_with_priority=[
                    WarehouseWithPriority(warehouse_id=_Pickup.warehouse_id, priority=1),
                    WarehouseWithPriority(warehouse_id=_Courier.warehouse_id, priority=1),
                    WarehouseWithPriority(warehouse_id=_Express.warehouse_id, priority=1),
                ],
            )
        ]

    @classmethod
    def prepare_lms(cls):
        cls.settings.lms_autogenerate = False
        cls.dynamic.lms += [DynamicDaysSet(key=1, days=[])]

        for wh_id, ds_id, ds_name in [
            (_Pickup.warehouse_id, _Pickup.delivery_service_id, 'pickup_delivery_service'),
            (_Courier.warehouse_id, _Courier.delivery_service_id, 'courier_delivery_service'),
            (_Express.warehouse_id, _Express.delivery_service_id, 'express_delivery_service'),
        ]:
            cls.dynamic.lms += [
                DynamicDeliveryServiceInfo(
                    id=ds_id,
                    name=ds_name,
                    region_to_region_info=[
                        DeliveryServiceRegionToRegionInfo(region_from=_Rids.moscow, region_to=_Rids.russia, days_key=1)
                    ],
                ),
                DynamicWarehouseInfo(id=wh_id, home_region=_Rids.moscow),
                DynamicWarehouseToWarehouseInfo(warehouse_from=wh_id, warehouse_to=wh_id),
                DynamicWarehouseAndDeliveryServiceInfo(
                    warehouse_id=wh_id,
                    delivery_service_id=ds_id,
                    operation_time=0,
                    date_switch_time_infos=[
                        DateSwitchTimeAndRegionInfo(
                            date_switch_hour=2,
                            region_to=_Rids.russia,
                            date_switch_time=TimeInfo(19, 0),
                            packaging_time=TimeInfo(3, 30),
                        )
                    ],
                ),
            ]
            cls.dynamic.lms += [
                DynamicWarehouseInfo(id=_Express.warehouse_id, home_region=_Rids.moscow, is_express=True),
            ]

    def test_offer_type(self):
        """
        Проверяем, что фильтрация по типам офферов производится.
        """

        offer_type_medicine = '&filter-offer-type=medicine'

        # Blue and white medical offer types have to be included
        for sku, waremd5 in {
            _Mskus._White.medical_pickup.sku: _WhiteOffers.medical_pickup.waremd5,
            _Mskus._White.medical_courier.sku: _WhiteOffers.medical_courier.waremd5,
            _Mskus._White.medical_express.sku: _WhiteOffers.medical_express.waremd5,
            _Mskus._Blue.medical_pickup.sku: _BlueOffers.medical_pickup.waremd5,
            _Mskus._Blue.medical_courier.sku: _BlueOffers.medical_courier.waremd5,
            _Mskus._Blue.medical_express.sku: _BlueOffers.medical_express.waremd5,
        }.items():
            response = self.report.request_json(
                _Requests.white_prime.format(rids=_Rids.moscow, msku=sku, express_delivery=0) + offer_type_medicine
            )
            self.assertFragmentIn(response, {'results': [{'entity': 'offer', 'wareId': waremd5}]})

        # Non medicical offer types have to be filtered
        response = self.report.request_json(
            _Requests.white_prime.format(rids=_Rids.moscow, msku=_Mskus._White.usual_pickup.sku, express_delivery=0)
            + offer_type_medicine
        )
        self.assertFragmentIn(response, {'results': EmptyList()})

    def test_delivery_type_any(self):
        """
        Проверяем, что любые офферы с любым видом доставки не будут фильтроваться.
        """

        delivery_type_any = '&filter-delivery-type=any'

        # Blue and white offers with any kind of delivery have to be included
        for sku, waremd5 in {
            _Mskus._White.usual_pickup.sku: _WhiteOffers.usual_pickup.waremd5,
            _Mskus._White.medical_pickup.sku: _WhiteOffers.medical_pickup.waremd5,
            _Mskus._White.medical_courier.sku: _WhiteOffers.medical_courier.waremd5,
            _Mskus._White.medical_express.sku: _WhiteOffers.medical_express.waremd5,
            _Mskus._Blue.medical_pickup.sku: _BlueOffers.medical_pickup.waremd5,
            _Mskus._Blue.medical_courier.sku: _BlueOffers.medical_courier.waremd5,
            _Mskus._Blue.medical_express.sku: _BlueOffers.medical_express.waremd5,
        }.items():
            response = self.report.request_json(
                _Requests.white_prime.format(rids=_Rids.moscow, msku=sku, express_delivery=0) + delivery_type_any
            )
            self.assertFragmentIn(response, {'results': [{'entity': 'offer', 'wareId': waremd5}]})

    def test_delivery_type_pickup(self):
        """
        Проверяем, что любые офферы без самовывоза будут фильтроваться.
        """

        delivery_type_pickup = '&filter-delivery-type=pickup'

        # Blue and white offers with pickup delivery have to be included
        for sku, waremd5 in {
            _Mskus._White.usual_pickup.sku: _WhiteOffers.usual_pickup.waremd5,
            _Mskus._White.medical_pickup.sku: _WhiteOffers.medical_pickup.waremd5,
            _Mskus._Blue.medical_pickup.sku: _BlueOffers.medical_pickup.waremd5,
        }.items():
            response = self.report.request_json(
                _Requests.white_prime.format(rids=_Rids.moscow, msku=sku, express_delivery=0) + delivery_type_pickup
            )
            self.assertFragmentIn(response, {'results': [{'entity': 'offer', 'wareId': waremd5}]})

        # Blue and white offers without pickup delivery have to be filtered
        for sku in [
            _Mskus._White.medical_courier.sku,
            _Mskus._White.medical_express.sku,
            _Mskus._Blue.medical_courier.sku,
            _Mskus._Blue.medical_express.sku,
        ]:
            response = self.report.request_json(
                _Requests.white_prime.format(rids=_Rids.moscow, msku=sku, express_delivery=0) + delivery_type_pickup
            )
            self.assertFragmentIn(response, {'results': EmptyList()})

    def test_delivery_type_courier(self):
        """
        Проверяем, что любые офферы без курьерской доставки будут фильтроваться.
        """

        delivery_type_courier = '&filter-delivery-type=courier'

        # Blue and white offers with courier delivery have to be included
        for sku, waremd5 in {
            _Mskus._White.medical_courier.sku: _WhiteOffers.medical_courier.waremd5,
            _Mskus._White.medical_express.sku: _WhiteOffers.medical_express.waremd5,
            _Mskus._Blue.medical_courier.sku: _BlueOffers.medical_courier.waremd5,
            _Mskus._Blue.medical_express.sku: _BlueOffers.medical_express.waremd5,
        }.items():
            response = self.report.request_json(
                _Requests.white_prime.format(rids=_Rids.moscow, msku=sku, express_delivery=0)
            )
            self.assertFragmentIn(response, {'results': [{'entity': 'offer', 'wareId': waremd5}]})

        # White offers without courier delivery have to be filtered
        for sku in [_Mskus._White.usual_pickup.sku, _Mskus._White.medical_pickup.sku, _Mskus._Blue.medical_pickup.sku]:
            response = self.report.request_json(
                _Requests.white_prime.format(rids=_Rids.moscow, msku=sku, express_delivery=0) + delivery_type_courier
            )
            self.assertFragmentIn(response, {'results': EmptyList()})

    def test_delivery_type_express(self):
        """
        Проверяем, что любые офферы без экспресс доставки будут отфильтрованы.
        """

        # Blue and white with 'filter-express-delivery' flag and 'express-offer' has to be included
        for sku, waremd5 in {
            _Mskus._White.medical_express.sku: _WhiteOffers.medical_express.waremd5,
            _Mskus._Blue.medical_express.sku: _BlueOffers.medical_express.waremd5,
        }.items():
            response = self.report.request_json(
                _Requests.white_prime.format(rids=_Rids.moscow, msku=sku, express_delivery=1)
            )
            self.assertFragmentIn(
                response, {'results': [{'entity': 'offer', 'wareId': waremd5, "delivery": {"isExpress": True}}]}
            )

        # Blue and white with 'filter-express-delivery' flag and 'non-express-offers' have to be filtered
        for sku in [
            _Mskus._White.usual_pickup.sku,
            _Mskus._White.medical_pickup.sku,
            _Mskus._White.medical_courier.sku,
            _Mskus._Blue.medical_pickup.sku,
            _Mskus._Blue.medical_courier.sku,
        ]:
            response = self.report.request_json(
                _Requests.white_prime.format(rids=_Rids.moscow, msku=sku, express_delivery=1)
            )
            self.assertFragmentIn(response, {'results': EmptyList()})

        # Blue and white without 'filter-express-delivery' flag and 'any-offers' have to be included
        for sku, waremd5 in {
            _Mskus._White.usual_pickup.sku: _WhiteOffers.usual_pickup.waremd5,
            _Mskus._White.medical_pickup.sku: _WhiteOffers.medical_pickup.waremd5,
            _Mskus._White.medical_courier.sku: _WhiteOffers.medical_courier.waremd5,
            _Mskus._White.medical_express.sku: _WhiteOffers.medical_express.waremd5,
            _Mskus._Blue.medical_pickup.sku: _BlueOffers.medical_pickup.waremd5,
            _Mskus._Blue.medical_courier.sku: _BlueOffers.medical_courier.waremd5,
            _Mskus._Blue.medical_express.sku: _BlueOffers.medical_express.waremd5,
        }.items():
            response = self.report.request_json(
                _Requests.white_prime.format(rids=_Rids.moscow, msku=sku, express_delivery=0)
            )
            self.assertFragmentIn(response, {'results': [{'entity': 'offer', 'wareId': waremd5}]})

    def test_medical_pickup(self):
        """
        Проверяем, что медицинские типы офферов с самовывозом не будут отфильтрованы
        в следующей комбинации фильтров:
        """

        offer_type_medicine = '&filter-offer-type=medicine'
        delivery_type_pickup = '&filter-delivery-type=pickup'

        # Medical pickup offer has to be included
        response = self.report.request_json(
            _Requests.white_prime.format(rids=_Rids.moscow, msku=_Mskus._White.medical_pickup.sku, express_delivery=0)
            + offer_type_medicine
            + delivery_type_pickup
        )
        self.assertFragmentIn(
            response, {'results': [{'entity': 'offer', 'wareId': _WhiteOffers.medical_pickup.waremd5}]}
        )

        # Non medical non pickup offer has to be filtered
        response = self.report.request_json(
            _Requests.white_prime.format(rids=_Rids.moscow, msku=_Mskus._White.usual_pickup.sku, express_delivery=0)
            + offer_type_medicine
            + delivery_type_pickup
        )
        self.assertFragmentIn(response, {'results': EmptyList()})


if __name__ == '__main__':
    main()
