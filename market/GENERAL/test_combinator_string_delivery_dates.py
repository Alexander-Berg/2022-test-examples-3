#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa


from datetime import datetime, time, timedelta

from core.matcher import Absent
from core.report import REQUEST_TIMESTAMP
from core.testcase import TestCase, main
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
    MarketSku,
    OfferDimensions,
    Payment,
    Region,
    RegionalDelivery,
    Shop,
    Tax,
    TimeInfo,
    WarehouseWithPriority,
    WarehousesPriorityInRegion,
)
from core.types.combinator import (
    CombinatorOffer,
    create_delivery_dates,
    create_delivery_interval_cgi,
    create_delivery_option,
    create_string_delivery_dates,
    DeliverySubtype,
    DeliveryType,
    Destination,
    RoutePoint,
    RoutePath,
)
from market.combinator.proto.grpc.combinator_pb2 import DeliveryService, Time


SECONDS_IN_MINUTE = 60
SECONDS_IN_HOUR = 60 * SECONDS_IN_MINUTE
DATETIME_NOW = datetime.fromtimestamp(REQUEST_TIMESTAMP)


def _duration_to_string(duration_s):
    return 'PT{hour}H{minute}M'.format(
        hour=duration_s / SECONDS_IN_HOUR, minute=duration_s % SECONDS_IN_HOUR / SECONDS_IN_MINUTE
    )


class _Constants:
    russia_rids = 225
    moscow_rids = 213

    model_id = 1
    category_id = 1

    virtual_blue_fesh = 1
    virtual_blue_feed_id = 1

    third_party_fesh = 2
    third_party_feed_id = 2

    ff_warehouse_id = 10

    courier_delivery_service_id = 100
    courier_bucket_dc_id = 1000
    courier_bucket_id = 2000

    dc_day_from = 1
    dc_day_to = 1
    dc_delivery_cost = 50

    class _Combinator:
        day_from = 1
        day_to = 1
        delivery_cost = 50
        date_from = DATETIME_NOW + timedelta(days=day_from)
        date_to = DATETIME_NOW + timedelta(days=day_to)
        courier_time_from = time(18, 0)
        courier_time_to = time(22, 0)

        shipment_day = 1
        shipment_date = '1985-05-25T00:00:00+03:00'
        packaging_time = 12600  # PT3H30M
        shipment_by_supplier = '1985-05-25T02:30:00+03:00'
        reception_by_warehouse = '1985-05-25T04:00:00+03:00'

        string_shipment_date = '1985-05-24'
        string_packaging_time = 'PT2H30M'
        string_shipment_by_supplier = '1985-05-24T01:30:00+03:00'
        string_reception_by_warehouse = '1985-05-24T03:00:00+03:00'


class _Shops:
    virtual_blue_shop = Shop(
        fesh=_Constants.virtual_blue_fesh,
        datafeed_id=_Constants.virtual_blue_feed_id,
        priority_region=_Constants.moscow_rids,
        tax_system=Tax.OSN,
        fulfillment_virtual=True,
        cpa=Shop.CPA_REAL,
        virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
    )

    third_party_shop = Shop(
        fesh=_Constants.third_party_fesh,
        datafeed_id=_Constants.third_party_feed_id,
        warehouse_id=_Constants.ff_warehouse_id,
        priority_region=_Constants.moscow_rids,
        tax_system=Tax.OSN,
        supplier_type=Shop.THIRD_PARTY,
        blue=Shop.BLUE_REAL,
        fulfillment_program=True,
        direct_shipping=True,
    )


class _BlueOffers:
    third_party_offer = BlueOffer(
        offerid='third_party_sku1',
        waremd5='ThirdPartyWaremd5____w',
        price=30,
        feedid=_Constants.third_party_feed_id,
        weight=5,
        dimensions=OfferDimensions(length=30, width=30, height=30),
        supplier_id=_Constants.third_party_fesh,
        delivery_buckets=[_Constants.courier_bucket_id],
    )

    third_party_msku = MarketSku(
        title="Обычный 3P оффер", hyperid=_Constants.category_id, sku=1, blue_offers=[third_party_offer]
    )


class _CombinatorOffers:
    third_party_offer = CombinatorOffer(
        shop_sku=_BlueOffers.third_party_offer.offerid,
        shop_id=_Constants.third_party_fesh,
        partner_id=_Constants.ff_warehouse_id,
        available_count=1,
    )


class _LogisticGraph:
    def _service_time(day, hour, minute):
        return datetime(
            year=DATETIME_NOW.year, month=DATETIME_NOW.month, day=DATETIME_NOW.day + day, hour=hour, minute=minute
        )

    Warehouse = RoutePoint(
        segment_id=512001,
        segment_type='warehouse',
        partner_type='FULFILLMENT',
        point_ids=Destination(partner_id=_Constants.ff_warehouse_id),
        services=[
            (DeliveryService.INTERNAL, 'PROCESSING', _service_time(day=0, hour=10, minute=0), timedelta(hours=2)),
            (DeliveryService.OUTBOUND, 'SHIPMENT', _service_time(day=0, hour=12, minute=0), timedelta(minutes=30)),
        ],
    )

    Movement = RoutePoint(
        segment_id=512002,
        segment_type='movement',
        partner_type='DELIVERY',
        point_ids=Destination(partner_id=_Constants.courier_delivery_service_id),
        services=[
            (DeliveryService.INBOUND, 'INBOUND', _service_time(day=0, hour=12, minute=30), timedelta(minutes=5)),
            (DeliveryService.INTERNAL, 'MOVEMENT', _service_time(day=0, hour=12, minute=5), timedelta(minutes=20)),
            (DeliveryService.OUTBOUND, 'SHIPMENT', _service_time(day=0, hour=12, minute=30), timedelta()),
        ],
    )

    Linehaul = RoutePoint(
        segment_id=512003,
        segment_type='linehaul',
        partner_type='DELIVERY',
        point_ids=Destination(partner_id=_Constants.courier_delivery_service_id),
        services=(
            (
                DeliveryService.INTERNAL,
                'DELIVERY',
                _service_time(day=0, hour=14, minute=40),
                timedelta(hours=1, minutes=20),
            ),
            (DeliveryService.INTERNAL, 'LAST_MILE', _service_time(day=0, hour=16, minute=0), timedelta(minutes=30)),
        ),
    )

    Handing = RoutePoint(
        segment_id=512004,
        segment_type='handing',
        partner_type='DELIVERY',
        point_ids=Destination(region_id=_Constants.moscow_rids),
        services=[
            (
                DeliveryService.OUTBOUND,
                'HANDING',
                _service_time(day=0, hour=16, minute=30),
                timedelta(minutes=15),
                (
                    Time(
                        hour=_Constants._Combinator.courier_time_from.hour,
                        minute=_Constants._Combinator.courier_time_from.minute,
                    ),
                    Time(
                        hour=_Constants._Combinator.courier_time_to.hour,
                        minute=_Constants._Combinator.courier_time_to.minute,
                    ),
                ),
            )
        ],
    )


class _Requests:
    delivery_route_request = (
        'place=delivery_route'
        '&pp=18'
        '&rids={rids}'
        '&rgb=blue'
        '&offers-list={offers}'
        '&delivery-type={type}'
        '&cost={cost}'
        '&delivery-interval={interval}'
    )


class T(TestCase):
    @classmethod
    def prepare_regions(cls):
        cls.index.regiontree += [Region(rid=_Constants.moscow_rids, name="Москва", tz_offset=10800)]

    @classmethod
    def prepare_shops(cls):
        cls.index.shops += [_Shops.virtual_blue_shop, _Shops.third_party_shop]

    @classmethod
    def prepare_delivery_buckets(cls):
        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=_Constants.courier_bucket_id,
                dc_bucket_id=_Constants.courier_bucket_dc_id,
                fesh=_Constants.third_party_fesh,
                carriers=[_Constants.courier_delivery_service_id],
                regional_options=[
                    RegionalDelivery(
                        rid=_Constants.moscow_rids,
                        options=[
                            DeliveryOption(
                                price=_Constants.dc_delivery_cost,
                                day_from=_Constants.dc_day_from,
                                day_to=_Constants.dc_day_to,
                            )
                        ],
                        payment_methods=[Payment.PT_YANDEX, Payment.PT_CASH_ON_DELIVERY, Payment.PT_CARD_ON_DELIVERY],
                    )
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            )
        ]

    @classmethod
    def prepare_warehouses(cls):
        cls.index.warehouse_priorities += [
            WarehousesPriorityInRegion(
                regions=[_Constants.russia_rids, _Constants.moscow_rids],
                warehouse_with_priority=[WarehouseWithPriority(warehouse_id=_Constants.ff_warehouse_id, priority=1)],
            )
        ]

    @classmethod
    def prepare_lms(cls):
        cls.dynamic.lms += [
            DynamicDaysSet(key=1, days=[]),
            DynamicDeliveryServiceInfo(
                id=_Constants.courier_delivery_service_id,
                name='courier_delivery_service',
                region_to_region_info=[
                    DeliveryServiceRegionToRegionInfo(
                        region_from=_Constants.moscow_rids, region_to=_Constants.russia_rids, days_key=1
                    )
                ],
            ),
            DynamicWarehouseInfo(id=_Constants.ff_warehouse_id, home_region=_Constants.moscow_rids),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=_Constants.ff_warehouse_id,
                delivery_service_id=_Constants.courier_delivery_service_id,
                operation_time=0,
                date_switch_time_infos=[
                    DateSwitchTimeAndRegionInfo(
                        date_switch_hour=2,
                        region_to=_Constants.russia_rids,
                        date_switch_time=TimeInfo(19, 0),
                        packaging_time=TimeInfo(3, 30),
                    )
                ],
            ),
        ]

    @classmethod
    def prepare_combinator(cls):
        report_start_time_in_ms = REQUEST_TIMESTAMP * 10**6
        cls.combinator.set_start_date(microseconds_for_disabled_random=report_start_time_in_ms)

        cls.combinator.on_delivery_route_request(
            delivery_type=DeliveryType.COURIER,
            destination=_LogisticGraph.Handing,
            delivery_option=create_delivery_option(
                cost=_Constants._Combinator.delivery_cost,
                date_from=_Constants._Combinator.date_from,
                date_to=_Constants._Combinator.date_to,
                time_from=_Constants._Combinator.courier_time_from,
                time_to=_Constants._Combinator.courier_time_to,
                delivery_subtype=DeliverySubtype.ORDINARY,
            ),
            total_price=_BlueOffers.third_party_offer.price,
        ).respond_with_delivery_route(
            offers=[_CombinatorOffers.third_party_offer],
            points=[_LogisticGraph.Warehouse, _LogisticGraph.Movement, _LogisticGraph.Linehaul, _LogisticGraph.Handing],
            paths=[RoutePath(point_from=i, point_to=i + 1) for i in range(0, 3)],
            date_to=_Constants._Combinator.date_to,
            date_from=_Constants._Combinator.date_from,
            delivery_dates=create_delivery_dates(
                shipment_date=_Constants._Combinator.shipment_date,
                shipment_day=_Constants._Combinator.shipment_day,
                packaging_time=_Constants._Combinator.packaging_time,
                shipment_by_supplier=_Constants._Combinator.shipment_by_supplier,
                reception_by_warehouse=_Constants._Combinator.reception_by_warehouse,
            ),
            string_delivery_dates=create_string_delivery_dates(
                shipment_date=_Constants._Combinator.string_shipment_date,
                packaging_time=_Constants._Combinator.string_packaging_time,
                shipment_by_supplier=_Constants._Combinator.string_shipment_by_supplier,
                reception_by_warehouse=_Constants._Combinator.string_reception_by_warehouse,
            ),
        )

    @classmethod
    def prepare_blue_offers(cls):
        cls.index.mskus += [_BlueOffers.third_party_msku]

    def test_string_delivery_dates(self):
        """
        Проверяем, что в выдачу Репорта на запросы в 'place=delivery_route' даты доставки
        включаются в строковом формате
        """
        response = self.report.request_json(
            _Requests.delivery_route_request.format(
                rids=_Constants.moscow_rids,
                offers='{}:1'.format(_BlueOffers.third_party_offer.waremd5),
                type='courier',
                cost=_Constants._Combinator.delivery_cost,
                interval=create_delivery_interval_cgi(
                    date_from=_Constants._Combinator.date_from,
                    time_from=_Constants._Combinator.courier_time_from,
                    date_to=_Constants._Combinator.date_to,
                    time_to=_Constants._Combinator.courier_time_to,
                ),
            )
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'deliveryGroup',
                        'delivery': {
                            'option': {
                                'dayFrom': _Constants._Combinator.day_from,
                                'dayTo': _Constants._Combinator.day_to,
                                'timeIntervals': [{'from': '18:00', 'to': '22:00'}],
                                'supplierProcessing': Absent(),
                                'shipmentDate': _Constants._Combinator.string_shipment_date,
                                'shipmentDay': _Constants._Combinator.shipment_day,
                                'shipmentBySupplier': _Constants._Combinator.string_shipment_by_supplier,
                                'receptionByWarehouse': _Constants._Combinator.string_reception_by_warehouse,
                                "packagingTime": _Constants._Combinator.string_packaging_time,
                            }
                        },
                        'offers': [{'entity': 'offer', 'wareId': _BlueOffers.third_party_offer.waremd5}],
                    }
                ]
            },
        )


if __name__ == '__main__':
    main()
