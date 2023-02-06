#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    DeliveryBucket,
    DeliveryCalendar,
    DeliveryOption,
    DeliveryServiceRegionToRegionInfo,
    DynamicDaysSet,
    DynamicDeliveryServiceInfo,
    GpsCoord,
    Model,
    Offer,
    Outlet,
    OutletDeliveryOption,
    PickupBucket,
    PickupOption,
    Region,
    RegionalDelivery,
    Shop,
    SortingCenterReference,
)
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.shops += [
            Shop(fesh=131, priority_region=213, delivery_service_outlets=[251, 252], cpa=Shop.CPA_REAL),
            Shop(fesh=502, priority_region=213, delivery_service_outlets=[251, 252], cpa=Shop.CPA_REAL),
            Shop(fesh=503, priority_region=213, delivery_service_outlets=[251, 252], cpa=Shop.CPA_REAL),
            Shop(fesh=504, priority_region=213, cpa=Shop.CPA_REAL),
        ]

        cls.index.models += [
            Model(hyperid=401),
            Model(hyperid=402),
            Model(hyperid=403),
            Model(hyperid=404),
            Model(hyperid=405),
            Model(hyperid=406),
        ]

        cls.index.offers += [
            Offer(fesh=131, hyperid=401, cpa=Offer.CPA_REAL, pickup_buckets=[5001, 5002]),
            Offer(fesh=502, hyperid=402, cpa=Offer.CPA_REAL, pickup_buckets=[5002]),
            Offer(fesh=503, hyperid=403, cpa=Offer.CPA_REAL, pickup_buckets=[5002]),
            Offer(fesh=504, hyperid=404, cpa=Offer.CPA_REAL, delivery_buckets=[801]),
            Offer(fesh=504, hyperid=405, cpa=Offer.CPA_REAL, delivery_buckets=[802]),
            Offer(fesh=504, hyperid=406, cpa=Offer.CPA_REAL, delivery_buckets=[803]),
        ]

        cls.index.delivery_calendars += [
            DeliveryCalendar(fesh=131, holidays=[0, 1, 2, 3, 4, 5, 6]),
        ]

        cls.index.shipment_service_calendars += [
            DeliveryCalendar(fesh=131, calendar_id=123, date_switch_hour=20, holidays=[0, 1, 2, 3, 4, 5, 6]),
            DeliveryCalendar(
                fesh=502,
                calendar_id=123,
                date_switch_hour=4,
                holidays=[27, 28],
                sc_references=[
                    SortingCenterReference(sc_id=7891, duration=1, default=True),
                    SortingCenterReference(sc_id=7892, duration=2),
                ],
            ),
            DeliveryCalendar(
                fesh=502,
                calendar_id=7891,
                date_switch_hour=5,
                holidays=[0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11],
                is_sorting_center=True,
            ),
            DeliveryCalendar(
                fesh=502,
                calendar_id=7892,
                date_switch_hour=5,
                holidays=[0, 1, 2, 5, 6, 14, 20, 21, 27, 28],
                is_sorting_center=True,
            ),
            DeliveryCalendar(fesh=503, calendar_id=123, date_switch_hour=4, holidays=[2, 3, 4, 5, 6]),
            DeliveryCalendar(fesh=504, calendar_id=50, date_switch_hour=20, holidays=[]),
            DeliveryCalendar(fesh=504, calendar_id=51, date_switch_hour=20, holidays=[]),
            DeliveryCalendar(fesh=504, calendar_id=52, date_switch_hour=20, holidays=[]),
        ]

        cls.settings.lms_autogenerate = False
        cls.dynamic.lms += [
            DynamicDeliveryServiceInfo(
                id=50,
                rating=2,
                region_to_region_info=[
                    DeliveryServiceRegionToRegionInfo(region_from=1, region_to=225, days_key=1),
                    DeliveryServiceRegionToRegionInfo(region_from=1, region_to=2, days_key=2),
                ],
            ),
            DynamicDeliveryServiceInfo(
                id=51,
                rating=2,
                region_to_region_info=[
                    DeliveryServiceRegionToRegionInfo(region_from=1, region_to=2, days_key=3),
                    DeliveryServiceRegionToRegionInfo(region_from=1, region_to=225, days_key=5),
                ],
            ),
            DynamicDeliveryServiceInfo(
                id=52,
                rating=2,
                region_to_region_info=[
                    DeliveryServiceRegionToRegionInfo(region_from=1, region_to=1, days_key=4),
                ],
            ),
            DynamicDeliveryServiceInfo(
                id=123,
                rating=2,
                region_to_region_info=[
                    DeliveryServiceRegionToRegionInfo(region_from=1, region_to=225, days_key=5),
                ],
            ),
            DynamicDaysSet(key=1, days=[0, 1, 2, 3]),
            DynamicDaysSet(key=2, days=[0, 1, 2]),
            DynamicDaysSet(key=3, days=[]),
            DynamicDaysSet(key=4, days=[0, 1, 2, 3, 4, 5]),
            DynamicDaysSet(
                key=5, days=[0, 1, 2, 3, 4, 5, 6, 7, 10]
            ),  # business calendar. default calendar for 225 region (Russia)
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=801,
                fesh=504,
                carriers=[50],
                regional_options=[
                    RegionalDelivery(rid=213, options=[DeliveryOption(price=5, day_from=1, day_to=2)]),
                    RegionalDelivery(rid=2, options=[DeliveryOption(price=5, day_from=1, day_to=2)]),
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=802,
                fesh=504,
                carriers=[51],
                regional_options=[
                    RegionalDelivery(rid=213, options=[DeliveryOption(price=5, day_from=1, day_to=2)]),
                    RegionalDelivery(rid=2, options=[DeliveryOption(price=5, day_from=1, day_to=2)]),
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=803,
                fesh=504,
                carriers=[52],
                regional_options=[RegionalDelivery(rid=1, options=[DeliveryOption(price=5, day_from=1, day_to=2)])],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
        ]

        cls.index.regiontree += [
            Region(
                rid=1,
                name='Москва и московская область',
                region_type=Region.FEDERATIVE_SUBJECT,
                children=[Region(rid=213, name='Москва')],
            )
        ]

        cls.index.outlets += [
            Outlet(
                point_id=251,
                delivery_service_id=123,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    shipper_id=123,
                    day_from=1,
                    day_to=1,
                    order_before=20,
                    work_in_holiday=True,
                    price=600,
                ),
                working_days=[i for i in range(15)],
                gps_coord=GpsCoord(37.1, 55.5),
            ),
            Outlet(
                point_id=252,
                delivery_service_id=123,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    shipper_id=123,
                    day_from=2,
                    day_to=2,
                    order_before=20,
                    work_in_holiday=True,
                    price=600,
                ),
                working_days=[i for i in range(15)],
                gps_coord=GpsCoord(37.1, 55.5),
            ),
            Outlet(
                point_id=257,
                fesh=131,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    shipper_id=123,
                    day_from=1,
                    day_to=1,
                    order_before=20,
                    work_in_holiday=True,
                    price=600,
                ),
                working_days=[i for i in range(15)],
                gps_coord=GpsCoord(37.2, 55.5),
            ),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5001,
                fesh=131,
                carriers=[99],
                options=[PickupOption(outlet_id=257, day_from=1, day_to=1, price=600)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5002,
                carriers=[123],
                options=[
                    PickupOption(outlet_id=251, day_from=1, day_to=1, price=100),
                    PickupOption(outlet_id=252, day_from=2, day_to=2, price=100),
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

    def test_business_calendar(self):
        """Проверяется, что время доставки в аутлет внешней службы учитывается по производственному календарю страны."""
        """Время доставки в собственный аутлет магазина просто прибавляется к общему времени доставки"""
        response = self.report.request_json('place=geo&hyperid=401&rids=213')
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "outlet": {
                        "id": "257",
                        "selfDeliveryRule": {
                            "dayFrom": 8,
                            "dayTo": 8,
                        },
                    },
                },
                {
                    "entity": "offer",
                    "outlet": {
                        "id": "251",
                        "serviceId": 123,
                        "selfDeliveryRule": {
                            "shipmentDay": 7,
                            "dayFrom": 9,  # время доставки в аутлет службы на 1 день больше, чем время доставки в собственный аутлет, т.к. ...
                            "dayTo": 9,  # ... day_from и day_to прибавляется с учетом производственного календаря, в котором 7 день -- выходной
                        },
                    },
                },
                {
                    "entity": "offer",
                    "outlet": {
                        "id": "252",
                        "serviceId": 123,
                        "selfDeliveryRule": {"shipmentDay": 7, "dayFrom": 11, "dayTo": 11},
                    },
                },
            ],
        )

    def test_sorting_center(self):
        """Проверяется, что:"""
        """    - orderBefore берется из соответствующей записи для сортировочного центра"""
        """    - dayFrom и dayTo рассчитываются с использованием смерженного календаря магазина и сортировочного центра, помеченного default=True"""
        """    - в dayFrom и dayTo учитывается время сортировки (duration)"""
        response = self.report.request_json('place=geo&hyperid=402&rids=213')
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "outlet": {
                        "id": "251",
                        "serviceId": 123,
                        "selfDeliveryRule": {
                            "shipmentDay": 12,
                            "orderBefore": 5,
                            "dayFrom": 14,  # 12(shipmentDay) + 1(sorting_center time) + 1(delivery_to_outlet time)
                            "dayTo": 14,
                        },
                    },
                }
            ],
        )

    def check_direction_calendar(self, request, day_from, day_to, service_id):
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "delivery": {"options": [{"dayFrom": day_from, "dayTo": day_to, "serviceId": service_id}]},
                }
            ],
        )

    def test_direction_calendar(self):
        request = 'place=prime&hyperid={}&rids='
        """Проверяется, что используется календарь без указанного направления для службы 50, т.к. 213 направление специально не задано"""
        request_1 = request.format('404')
        self.check_direction_calendar(request_1 + '213', 5, 6, '50')

        """Проверяется, что используется специальный календарь для службы 50 и направления 2"""
        self.check_direction_calendar(request_1 + '2', 4, 5, '50')

        """Проверяется, что если в календаре направлений задана пуустая запись для службы 51, то используется производственный календарь для неспециализированных направлений (например rids=213)"""
        request_2 = request.format('405')
        self.check_direction_calendar(request_2 + '213', 9, 11, '51')

        """Проверяется, что если holidays заданы как пустой массив (для службы 51 и направления 2), то считается, что службы работает ежедевно."""
        """&inlet-shipment-day=0 задано, чтобы убрать значение по умолчанию = 2 дня для тестовой среды"""
        self.check_direction_calendar(request_2 + '2&inlet-shipment-day=0', 1, 2, '51')

        """Проверяется, что если в календаре направлений нет в точности заданного региона, то в календаре направлений ищутся регионы среди предков (т.е. в данном случае выбирается регион 1)"""
        self.check_direction_calendar(request.format('406') + '213', 7, 8, '52')


if __name__ == '__main__':
    main()
