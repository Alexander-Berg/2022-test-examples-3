#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

import datetime
from core.types import (
    BlueOffer,
    Offer,
    DynamicDeliveryRestriction,
    DynamicWarehouseDelivery,
    DynamicWarehouseLink,
    MarketSku,
    OfferDimensions,
    Shop,
)
from core.testcase import (
    TestCase,
    main,
)
from core.types.combinator import (
    create_delivery_option,
    DeliveryType,
    Destination,
    CombinatorOffer,
    RoutePoint,
    RoutePath,
)
from market.combinator.proto.grpc.combinator_pb2 import DeliveryService, Time
from core.types.delivery import BlueDeliveryTariff

REARR_FLAG_TEMPLATE = "&rearr-factors=kgt_volume_calculation={value}"


class _Requests:
    DeliveryRoute = (
        'place=delivery_route'
        '&pp=18'
        '&rids={rids}'
        '&offers-list={offers}'
        '&delivery-type=courier'
        '&delivery-interval=20211202.1000-20211212.2230'
        '&rearr-factors='
    )
    ActualDelivery = (
        'place=actual_delivery'
        '&offers-list={offers}'
        '&rids={rids}'
        '&pickup-options=grouped'
        '&pickup-options-extended-grouping=1'
    )
    Prime = 'place=prime' '&rids={rids}' '&text={text}'
    OfferInfo = 'place=offerinfo' '&offerid={offer}' '&rids={rids}' '&regset=2'


class _Shops:
    blue_shop = Shop(
        fesh=4,
        datafeed_id=4,
        priority_region=213,
        name='blue_shop',
        supplier_type=Shop.THIRD_PARTY,
        blue='REAL',
        warehouse_id=145,
    )
    shop_dsbs = Shop(
        fesh=42,
        datafeed_id=4240,
        priority_region=213,
        name='Dsbs партнер',
        client_id=11,
        cpa=Shop.CPA_REAL,
        warehouse_id=6874,
    )


class _Offers:
    NoKgtOffer = BlueOffer(
        title="NoKgtOffer",
        shop=_Shops.blue_shop,
        offerid="1",
        price=1000,
        waremd5=Offer.generate_waremd5("nokgt"),
        weight=5,
        blue_weight=5,
        dimensions=OfferDimensions(height=100, length=100, width=45),
        blue_dimensions=OfferDimensions(height=100, length=100, width=45),
    )

    WeightKGTOffer = BlueOffer(
        title="WeightKGTOffer",
        shop=_Shops.blue_shop,
        offerid="2",
        price=1000,
        waremd5=Offer.generate_waremd5("weightkgt"),
        weight=100,
        blue_weight=100,
        dimensions=OfferDimensions(height=10, length=30, width=20),
        blue_dimensions=OfferDimensions(height=10, length=30, width=20),
    )

    OneDimKGTOffer = BlueOffer(
        title="OneDimKGTOffer",
        shop=_Shops.blue_shop,
        offerid="2",
        price=1000,
        waremd5=Offer.generate_waremd5("onedimgkt"),
        weight=100,
        blue_weight=5,
        dimensions=OfferDimensions(height=151, length=30, width=20),
        blue_dimensions=OfferDimensions(height=151, length=30, width=20),
    )

    VolumeKGTOffer = BlueOffer(
        title="VolumeKGTOffer",
        shop=_Shops.blue_shop,
        offerid="2",
        price=1000,
        waremd5=Offer.generate_waremd5("volumekgt"),
        weight=100,
        blue_weight=5,
        dimensions=OfferDimensions(height=145, length=145, width=145),
        blue_dimensions=OfferDimensions(height=145, length=145, width=145),
    )

    offers = [
        NoKgtOffer,
        WeightKGTOffer,
        OneDimKGTOffer,
        VolumeKGTOffer,
    ]

    NoKgtOfferDsbs = Offer(
        title="NoKgtOfferDsbs",
        shop=_Shops.shop_dsbs,
        offerid="1",
        price=1000,
        waremd5=Offer.generate_waremd5("nokgtdsbs"),
        weight=5,
        blue_weight=5,
        dimensions=OfferDimensions(height=100, length=100, width=45),
        blue_dimensions=OfferDimensions(height=100, length=100, width=45),
    )


def SERVICE_TIME(hour, minute, day=0):
    return datetime.datetime(year=2022, month=3, day=22 + day, hour=hour, minute=minute)


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.shops += [_Shops.blue_shop, _Shops.shop_dsbs]
        cls.index.mskus = [
            MarketSku(
                sku=sku,
                title=offer.title,
                hid=1,
                blue_offers=[
                    offer,
                ],
            )
            for sku, offer in enumerate(_Offers.offers)
        ]
        cls.index.offers += [_Offers.NoKgtOfferDsbs]

    @classmethod
    def prepare_nordstream(cls):
        cls.settings.nordstream_autogenerate = False
        cls.dynamic.nordstream += [
            DynamicWarehouseLink(_Shops.blue_shop.warehouse_id, [_Shops.blue_shop.warehouse_id]),
            DynamicWarehouseLink(_Shops.shop_dsbs.warehouse_id, [_Shops.shop_dsbs.warehouse_id]),
        ]
        cls.dynamic.nordstream += [
            DynamicWarehouseDelivery(
                ware,
                {
                    213: [
                        DynamicDeliveryRestriction(
                            max_phys_weight=900000000,
                            max_dim_sum=2000000,
                            max_dimensions=[2000000, 2000000, 2000000],
                            min_days=0,
                            max_days=999999,
                        )
                    ],
                },
            )
            for ware in [
                _Shops.blue_shop.warehouse_id,
                _Shops.shop_dsbs.warehouse_id,
            ]
        ]

    @classmethod
    def prepare_combinator(cls):
        endPointRegion = RoutePoint(
            point_ids=Destination(region_id=213),
            segment_id=512005,
            segment_type="handing",
            services=(
                (
                    DeliveryService.OUTBOUND,
                    "HANDING",
                    SERVICE_TIME(20, 5),
                    datetime.timedelta(minutes=15),
                    (Time(hour=10), Time(hour=22, minute=30)),
                ),
            ),
        )

        deliveryDateFrom = datetime.date(year=2021, month=12, day=2)
        deliveryDateTo = datetime.date(year=2021, month=12, day=12)
        deliveryTimeFrom = datetime.time(hour=10, minute=0)
        deliveryTimeTo = datetime.time(hour=22, minute=30)

        warehouses = {
            _Shops.blue_shop.warehouse_id: RoutePoint(
                point_ids=Destination(partner_id=_Shops.blue_shop.warehouse_id),
                segment_id=512001,
                segment_type="warehouse",
                services=(
                    (DeliveryService.INTERNAL, "PROCESSING", SERVICE_TIME(13, 25), datetime.timedelta(hours=2)),
                    (DeliveryService.OUTBOUND, "SHIPMENT", SERVICE_TIME(15, 25), datetime.timedelta(minutes=35)),
                ),
                partner_type="FULFILLMENT",
            ),
            _Shops.shop_dsbs.warehouse_id: RoutePoint(
                point_ids=Destination(partner_id=_Shops.shop_dsbs.warehouse_id),
                segment_id=512002,
                segment_type="warehouse",
                services=(
                    (DeliveryService.INTERNAL, "PROCESSING", SERVICE_TIME(13, 25), datetime.timedelta(hours=2)),
                    (DeliveryService.OUTBOUND, "SHIPMENT", SERVICE_TIME(15, 25), datetime.timedelta(minutes=35)),
                    (DeliveryService.INTERNAL, "CUTOFF", SERVICE_TIME(13, 25), datetime.timedelta()),
                ),
                partner_type="DROPSHIP",
            ),
        }
        simplePath = [RoutePath(point_from=0, point_to=1)]
        for offer in _Offers.offers:
            combOffer = CombinatorOffer(
                shop_sku=offer.offerid,
                shop_id=offer.shop.fesh,
                partner_id=offer.shop.warehouse_id,
                available_count=5000,
            )
            for count in range(1, 10):
                cls.combinator.on_delivery_route_request(
                    delivery_type=DeliveryType.COURIER,
                    destination=endPointRegion,
                    delivery_option=create_delivery_option(
                        date_from=deliveryDateFrom,
                        date_to=deliveryDateTo,
                        time_from=deliveryTimeFrom,
                        time_to=deliveryTimeTo,
                    ),
                    total_price=offer.price * count,
                ).respond_with_delivery_route(
                    offers=[
                        combOffer,
                    ],
                    points=[
                        warehouses[offer.shop.warehouse_id],
                        endPointRegion,
                    ],
                    paths=simplePath,
                    date_from=deliveryDateFrom,
                    date_to=deliveryDateTo,
                    shipment_warehouse=offer.shop.warehouse_id,
                )

    @classmethod
    def prepare_unified_delivery_modifiers(cls):
        EXP_UNIFIED_TARIFFS = 'unified'

        # Эксперимент с едиными тарифами, тариф экспресс оферов в конкретном городе
        cls.index.blue_delivery_modifiers.set_default_modifier(
            exp_name=EXP_UNIFIED_TARIFFS,
            tariffs=[
                BlueDeliveryTariff(user_price=99, large_size=0),
                BlueDeliveryTariff(user_price=599, large_size=1),
            ],
            large_size_weight=30,
            large_size_max_item_dimension=1.5,
            large_size_volume=0.5,
        )

        cls.index.blue_delivery_modifiers.set_default_modifier(
            is_dsbs_payment=True,
            exp_name=EXP_UNIFIED_TARIFFS,
            tariffs=[
                BlueDeliveryTariff(is_dsbs_payment=True, dsbs_payment=199, large_size=0),
                BlueDeliveryTariff(is_dsbs_payment=True, dsbs_payment=1599, large_size=1),
            ],
            large_size_weight=30,
            large_size_max_item_dimension=1.5,
            large_size_volume=0.5,
        )

    def test_info_places(self):
        '''
        Проверяем, что в плейсах используемых на карточках и выдаче (и использующих один оффер)
        в рассчёте кгт будет использоваться объём
        '''

        def test_request(request, rearr, offer, is_kgt):
            response = self.report.request_json(
                request.format(rids=213, offer="{w}".format(w=offer.waremd5), text=offer.title)
                + (REARR_FLAG_TEMPLATE.format(value=rearr) if rearr is not None else "")
            )

            self.assertFragmentIn(
                response,
                {
                    "entity": "offer",
                    "wareId": offer.waremd5,
                    "largeSize": is_kgt,
                },
            )

        for request in [_Requests.Prime, _Requests.OfferInfo]:
            rearr_factors_values = [0]
            for offer, is_kgt in [
                (_Offers.NoKgtOffer, False),
                (_Offers.WeightKGTOffer, True),
                (_Offers.VolumeKGTOffer, False),
                (_Offers.OneDimKGTOffer, False),
                (_Offers.NoKgtOfferDsbs, False),
            ]:
                for rearr_val in rearr_factors_values:
                    test_request(request, rearr_val, offer, is_kgt)

            rearr_factors_values = [None, 1]
            for offer, is_kgt in [
                (_Offers.NoKgtOffer, False),
                (_Offers.WeightKGTOffer, True),
                (_Offers.VolumeKGTOffer, True),
                (_Offers.OneDimKGTOffer, True),
                (_Offers.NoKgtOfferDsbs, False),
            ]:
                for rearr_val in rearr_factors_values:
                    test_request(request, rearr_val, offer, is_kgt)

    def test_delivery_places(self):
        '''
        Проверяем, что в плейсах работающих с корзиной
        в рассчёте кгт будет использоваться измерения (для одного оффера)
        '''

        def test_request(request, rearr, offer, is_kgt):
            response = self.report.request_json(
                request.format(
                    rids=213,
                    offers="{w}:1".format(w=offer.waremd5),
                )
                + (REARR_FLAG_TEMPLATE.format(value=rearr) if rearr is not None else "")
            )
            self.assertFragmentIn(
                response,
                {
                    "entity": "deliveryGroup",
                    "largeSize": is_kgt,
                    "offers": [
                        {
                            "entity": "offer",
                            "wareId": offer.waremd5,
                            "largeSize": is_kgt,
                        }
                    ],
                },
            )

        for request in [_Requests.DeliveryRoute, _Requests.ActualDelivery]:

            rearr_factors_values = [0]
            for offer, is_kgt in [
                (_Offers.NoKgtOffer, False),
                (_Offers.WeightKGTOffer, True),
                (_Offers.VolumeKGTOffer, False),
                (_Offers.OneDimKGTOffer, False),
            ]:
                for rearr_val in rearr_factors_values:
                    test_request(request, rearr_val, offer, is_kgt)

            rearr_factors_values = [None, 1]
            for offer, is_kgt in [
                (_Offers.NoKgtOffer, False),
                (_Offers.WeightKGTOffer, True),
                (_Offers.VolumeKGTOffer, True),
                (_Offers.OneDimKGTOffer, True),
            ]:
                for rearr_val in rearr_factors_values:
                    test_request(request, rearr_val, offer, is_kgt)

    def test_delivery_places_multiple_volume(self):
        '''
        Проверяем, что в плейсах работающих с корзиной
        в рассчёте кгт будет использоваться измерения (для корзины)
        Тут проверяем, что кгт корзины будет определён по суммарному объёму
        '''

        def test_request(request, rearr, offer, is_kgt):
            response = self.report.request_json(
                request.format(
                    rids=213,
                    offers="{w}:3".format(w=offer.waremd5),
                )
                + (REARR_FLAG_TEMPLATE.format(value=rearr) if rearr is not None else "")
            )

            self.assertFragmentIn(
                response,
                {
                    "entity": "deliveryGroup",
                    "largeSize": is_kgt,
                    "offers": [
                        {
                            "entity": "offer",
                            "wareId": offer.waremd5,
                            "largeSize": False,
                        }
                    ],
                },
            )

        for request in [_Requests.DeliveryRoute, _Requests.ActualDelivery]:
            rearr_factors_values = [0]
            for offer, is_kgt in [
                (_Offers.NoKgtOffer, False),
                (_Offers.NoKgtOfferDsbs, False),
            ]:
                for rearr_val in rearr_factors_values:
                    test_request(request, rearr_val, offer, is_kgt)

            rearr_factors_values = [None, 1]
            for offer, is_kgt in [
                (_Offers.NoKgtOffer, True),
                (_Offers.NoKgtOfferDsbs, True),
            ]:
                for rearr_val in rearr_factors_values:
                    test_request(request, rearr_val, offer, is_kgt)


if __name__ == '__main__':
    main()
