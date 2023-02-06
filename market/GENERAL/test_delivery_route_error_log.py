#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

import datetime
from core.types import (
    BlueOffer,
    DeliveryServiceRegionToRegionInfo,
    DynamicDaysSet,
    DynamicDeliveryServiceInfo,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseInfo,
    DynamicWarehouseToWarehouseInfo,
    GpsCoord,
    MarketSku,
    Model,
    OfferDimensions,
    Outlet,
    PickupBucket,
    PickupOption,
    Region,
    Shop,
    Tax,
    WarehouseWithPriority,
    WarehousesPriorityInRegion,
)
from core.logs import ErrorCodes
from core.testcase import TestCase, main
from core.types.combinator import (
    create_delivery_option,
    DeliveryType,
    Destination,
    CombinatorOffer,
    RoutePoint,
    RoutePath,
)
from core.types.delivery import OutletWorkingTime
from core.report import REQUEST_TIMESTAMP
from market.combinator.proto.grpc.combinator_pb2 import DeliveryService, Time


class _Constants:
    Now = datetime.datetime.fromtimestamp(REQUEST_TIMESTAMP)

    RussiaRids = 225
    MskRids = 213

    ModelId = 1
    CategoryId = 1

    VirtualBlueFesh = 1
    VirtualBlueFeedId = 1

    CrossdockFesh = 2
    CrossdockFeedId = 2

    FulfillmentWarehouseId = 145
    CrossdockWarehouseId = 71

    DeliveryServiceId = 135
    PickupOutletId = 100000

    PickupBucketDcId = 1000
    PickupBucketId = 2000

    DcDayFrom = 1
    DcDayTo = 1
    DcDeliveryCost = 50

    CombinatorDayFrom = 1
    CombinatorDayTo = 1
    CombinatorDateFrom = Now + datetime.timedelta(days=CombinatorDayFrom)
    CombinatorDateTo = Now + datetime.timedelta(days=CombinatorDayTo)

    CombinatorTimeFrom = datetime.time(hour=10, minute=0)
    CombinatorTimeTo = datetime.time(hour=18, minute=0)


class _Requests:
    BaseRequestTemplate = (
        'place=delivery_route'
        '&pp=18'
        '&rids={rids}'
        '&rgb=blue'
        '&offers-list={offers}'
        '&delivery-type={type}'
        '&{type_param}'
        '&debug=1'
    )


class _Shops:
    VirtualBlue = Shop(
        fesh=_Constants.VirtualBlueFesh,
        datafeed_id=_Constants.VirtualBlueFeedId,
        priority_region=_Constants.MskRids,
        tax_system=Tax.OSN,
        fulfillment_virtual=True,
        cpa=Shop.CPA_REAL,
        virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
        delivery_service_outlets=[_Constants.PickupOutletId],
    )
    Crossdock = Shop(
        fesh=_Constants.CrossdockFesh,
        datafeed_id=_Constants.CrossdockFeedId,
        warehouse_id=_Constants.CrossdockWarehouseId,
        name="Crossdock партнер",
        priority_region=_Constants.MskRids,
        tax_system=Tax.OSN,
        supplier_type=Shop.THIRD_PARTY,
        blue=Shop.BLUE_REAL,
        fulfillment_program=True,
        direct_shipping=False,
    )


class _Outlets:
    PickupOutlet = Outlet(
        point_id=_Constants.PickupOutletId,
        delivery_service_id=_Constants.DeliveryServiceId,
        region=_Constants.MskRids,
        point_type=Outlet.FOR_PICKUP,
        working_days=list(range(10)),
        working_times=[
            OutletWorkingTime(
                days_from=OutletWorkingTime.MONDAY,
                days_till=OutletWorkingTime.SUNDAY,
                hours_from='09:00',
                hours_till='21:00',
            )
        ],
        gps_coord=GpsCoord(37.12, 55.32),
    )


class _DeliveryBuckets:
    DefaultPickupBucket = PickupBucket(
        bucket_id=_Constants.PickupBucketId,
        dc_bucket_id=_Constants.PickupBucketDcId,
        fesh=_Constants.VirtualBlueFesh,
        carriers=[_Constants.DeliveryServiceId],
        options=[
            PickupOption(
                outlet_id=_Constants.PickupOutletId,
                day_from=_Constants.DcDayFrom,
                day_to=_Constants.DcDayTo,
                price=_Constants.DcDeliveryCost,
            )
        ],
    )


class _BlueOffers:
    CrossdockOffer = BlueOffer(
        offerid='crossdock_sku',
        waremd5='CrossdockOffer_______g',
        price=3000,
        feedid=_Constants.CrossdockFeedId,
        weight=1.3,
        dimensions=OfferDimensions(length=1.2, width=2.2, height=3.2),
        supplier_id=_Constants.CrossdockFesh,
        pickup_buckets=[_Constants.PickupBucketId],
    )


class _MarketSkus:
    CrossdockMsku = MarketSku(
        title="Crossdock оффер", hyperid=_Constants.ModelId, sku=1, blue_offers=[_BlueOffers.CrossdockOffer]
    )


def _service_time(hour, minute, day=0):
    return datetime.datetime(year=2020, month=8, day=2 + day, hour=hour, minute=minute)


class _Combinator:
    CrossdockWarehouse = RoutePoint(
        point_ids=Destination(partner_id=_Constants.CrossdockWarehouseId),
        segment_id=512001,
        segment_type='warehouse',
        services=[
            (DeliveryService.INTERNAL, 'PROCESSING', _service_time(10, 5), datetime.timedelta(hours=1)),
            (DeliveryService.OUTBOUND, 'SHIPMENT', _service_time(11, 5), datetime.timedelta(minutes=20)),
        ],
        partner_type='SUPPLIER',
    )
    CrossdockMovement = RoutePoint(
        point_ids=Destination(partner_id=_Constants.CrossdockWarehouseId),
        segment_id=512002,
        segment_type='movement',
        services=[
            (DeliveryService.INTERNAL, 'MOVEMENT', _service_time(11, 25), datetime.timedelta(minutes=20)),
            (DeliveryService.OUTBOUND, 'SHIPMENT', _service_time(11, 45), datetime.timedelta(minutes=40)),
        ],
    )
    FulfillmentWarehouse = RoutePoint(
        point_ids=Destination(partner_id=_Constants.FulfillmentWarehouseId),
        segment_id=512003,
        segment_type='warehouse',
        services=[
            (DeliveryService.INTERNAL, 'PROCESSING', _service_time(13, 25), datetime.timedelta(hours=2)),
            (DeliveryService.OUTBOUND, 'SHIPMENT', _service_time(15, 25), datetime.timedelta(minutes=35)),
        ],
        partner_type='FULFILLMENT',
    )
    Movement = RoutePoint(
        point_ids=Destination(partner_id=_Constants.DeliveryServiceId),
        segment_id=512004,
        segment_type='movement',
        services=[
            (DeliveryService.INBOUND, 'INBOUND', _service_time(16, 0), datetime.timedelta(seconds=15)),
            (DeliveryService.INTERNAL, 'MOVEMENT', _service_time(16, 1), datetime.timedelta(minutes=29)),
            (DeliveryService.OUTBOUND, 'SHIPMENT', _service_time(16, 30), datetime.timedelta(hours=1)),
        ],
    )
    Linehaul = RoutePoint(
        point_ids=Destination(partner_id=_Constants.DeliveryServiceId),
        segment_id=512005,
        segment_type='linehaul',
        services=[
            (DeliveryService.INTERNAL, 'DELIVERY', _service_time(17, 50), datetime.timedelta(hours=1, minutes=45)),
            (DeliveryService.INTERNAL, 'LAST_MILE', _service_time(19, 35), datetime.timedelta(minutes=30)),
        ],
    )
    EndPointPickup = RoutePoint(
        point_ids=Destination(logistic_point_id=_Constants.PickupOutletId),
        segment_id=512006,
        segment_type='pickup',
        services=[(DeliveryService.OUTBOUND, 'HANDING', _service_time(20, 5), datetime.timedelta(minutes=15))],
    )

    EndPointRegion = RoutePoint(
        point_ids=Destination(region_id=_Constants.MskRids),
        segment_id=512007,
        segment_type='handing',
        services=[
            (
                DeliveryService.OUTBOUND,
                'HANDING',
                _service_time(20, 5),
                datetime.timedelta(minutes=15),
                (Time(hour=10), Time(hour=22, minute=30)),
            )
        ],
    )

    CrossdockOffer = CombinatorOffer(
        shop_sku=_BlueOffers.CrossdockOffer.offerid,
        shop_id=_Constants.CrossdockFesh,
        partner_id=_Constants.CrossdockWarehouseId,
        available_count=1,
    )


class T(TestCase):
    @classmethod
    def prepare_regions(cls):
        cls.index.regiontree += [Region(rid=_Constants.MskRids, name="Москва")]

    @classmethod
    def prepare_shops_and_outlets(cls):
        cls.index.shops += [_Shops.VirtualBlue, _Shops.Crossdock]
        cls.index.outlets += [_Outlets.PickupOutlet]

    @classmethod
    def prepare_buckets(cls):
        cls.index.pickup_buckets += [_DeliveryBuckets.DefaultPickupBucket]

    @classmethod
    def prepare_warehouses(cls):
        cls.index.warehouse_priorities += [
            WarehousesPriorityInRegion(
                regions=[_Constants.RussiaRids],
                warehouse_with_priority=[
                    WarehouseWithPriority(warehouse_id=wh_id, priority=100)
                    for wh_id in [_Constants.FulfillmentWarehouseId, _Constants.CrossdockWarehouseId]
                ],
            )
        ]

    @classmethod
    def prepare_lms(cls):
        cls.settings.lms_autogenerate = False
        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=_Constants.FulfillmentWarehouseId, home_region=_Constants.MskRids),
            DynamicWarehouseInfo(id=_Constants.CrossdockWarehouseId, home_region=_Constants.MskRids),
            DynamicWarehouseToWarehouseInfo(
                warehouse_from=_Constants.CrossdockWarehouseId, warehouse_to=_Constants.FulfillmentWarehouseId
            ),
            DynamicDeliveryServiceInfo(
                id=_Constants.DeliveryServiceId,
                region_to_region_info=[
                    DeliveryServiceRegionToRegionInfo(
                        region_from=_Constants.MskRids, region_to=_Constants.RussiaRids, days_key=1
                    )
                ],
            ),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=_Constants.FulfillmentWarehouseId, delivery_service_id=_Constants.DeliveryServiceId
            ),
            DynamicDaysSet(key=1, days=[]),
        ]

    @classmethod
    def prepare_blue_offers(cls):
        cls.index.models += [Model(hid=_Constants.CategoryId, hyperid=_Constants.ModelId)]
        cls.index.mskus += [_MarketSkus.CrossdockMsku]

    @classmethod
    def prepare_combinator(cls):
        report_start_time_in_ms = REQUEST_TIMESTAMP * 10**6
        cls.combinator.set_start_date(microseconds_for_disabled_random=report_start_time_in_ms)

        cls.combinator.on_delivery_route_request(
            delivery_type=DeliveryType.PICKUP,
            destination=_Combinator.EndPointPickup,
            delivery_option=create_delivery_option(),
            total_price=_BlueOffers.CrossdockOffer.price,
        ).respond_with_delivery_route(
            offers=[_Combinator.CrossdockOffer],
            points=[
                _Combinator.CrossdockWarehouse,
                _Combinator.CrossdockMovement,
                _Combinator.FulfillmentWarehouse,
                _Combinator.Movement,
                _Combinator.Linehaul,
            ],
            paths=[RoutePath(point_from=i, point_to=i + 1) for i in range(3)],
            date_from=_Constants.CombinatorDateFrom,
            date_to=_Constants.CombinatorDateTo,
        )

        cls.combinator.on_delivery_route_request(
            delivery_type=DeliveryType.COURIER,
            destination=_Combinator.EndPointRegion,
            delivery_option=create_delivery_option(
                date_from=_Constants.CombinatorDateFrom,
                date_to=_Constants.CombinatorDateTo,
                time_from=_Constants.CombinatorTimeFrom,
                time_to=_Constants.CombinatorTimeTo,
            ),
            total_price=_BlueOffers.CrossdockOffer.price,
        ).respond_with_delivery_route(
            offers=[_Combinator.CrossdockOffer],
            points=[
                _Combinator.CrossdockWarehouse,
                _Combinator.CrossdockMovement,
                _Combinator.FulfillmentWarehouse,
                _Combinator.Movement,
                _Combinator.Linehaul,
                _Combinator.EndPointRegion,
            ],
            paths=[RoutePath(point_from=i, point_to=i + 1) for i in range(3)],
            date_from=_Constants.CombinatorDateFrom,
            date_to=_Constants.CombinatorDateTo,
            delivery_type=DeliveryType.PICKUP,
        )

    def test_missing_route_final_point(self):
        """
        Проверяем, что запись текста ошибки об отсутсвии конечной точки маршрута
        в error.log Репорта происходит корректно
        """
        self.report.request_json(
            _Requests.BaseRequestTemplate.format(
                rids=_Constants.MskRids,
                offers='{}:1'.format(_BlueOffers.CrossdockOffer.waremd5),
                type='pickup',
                type_param='point_id={}'.format(_Constants.PickupOutletId),
            )
        )
        self.error_log.expect(
            code=ErrorCodes.COMBINATOR_INVALID_ROUTE, message="Failed to find final route point"
        ).once()

    def test_delivery_type_mismatch(self):
        """
        Проверяем, что запись текста ошибки о несовпадении типа доставки
        в error.log Репорта происходит корректно
        """

        def date_time_to_string(dt, tm):
            return '{year}{month}{day}.{hour}{minute}'.format(
                year=dt.year, month=dt.month, day=dt.day, hour=tm.hour, minute=tm.minute
            )

        self.report.request_json(
            _Requests.BaseRequestTemplate.format(
                rids=_Constants.MskRids,
                offers='{}:1'.format(_BlueOffers.CrossdockOffer.waremd5),
                type='courier',
                type_param='delivery-interval={dt_from}-{dt_to}'.format(
                    dt_from=date_time_to_string(dt=_Constants.CombinatorDateFrom, tm=_Constants.CombinatorTimeFrom),
                    dt_to=date_time_to_string(dt=_Constants.CombinatorDateTo, tm=_Constants.CombinatorTimeTo),
                ),
            )
        )
        self.error_log.expect(
            code=ErrorCodes.COMBINATOR_ROUTE_FILTERS_MISMATCH,
            message="Delivery type mismatch, route: PICKUP, expected: COURIER",
        ).once()
        self.error_log.expect(
            code=ErrorCodes.COMBINATOR_INVALID_ROUTE,
        ).once()


if __name__ == '__main__':
    main()
