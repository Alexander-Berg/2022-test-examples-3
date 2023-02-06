#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    DeliveryBucket,
    DeliveryCalendar,
    GpsCoord,
    Offer,
    Outlet,
    OutletDeliveryOption,
    PickupBucket,
    PickupOption,
    Region,
    Shop,
)
from core.testcase import TestCase, main
from core.matcher import ElementCount


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.enable_testing_features = False
        cls.index.regiontree += [Region(rid=213, name='Moscow', tz_offset=10800)]

        """Создаются аутлеты разных типов, относящиеся к 2 службам: 103, 123"""
        # Аутлеты на "карте"
        # Числа в скобках - координаты тайлов при zoom = 10
        #           37.0(617)     37.2(617)       37.4(618)      37.6(618)       37.8(619)
        # 55.8(321) |--------------|---------------|--------------|---------------
        #           |              |               |              |
        #           |              |               |              |      *(2004)
        #           |              |               |              |
        #           |              |               |              |
        # 55.6(322) |--------------|---------------|--------------|---------------
        #           |              |               |              |
        #           |              |    *(2002)    |              |
        #           |              |               |      *(2003) |
        #           |              |               |              |
        # 55.4(323) |--------------|---------------|--------------|---------------
        #           |              |               |              |
        #           |     *(2001)  |               |              |
        #           |              |               |              |
        #           |              |               |              |
        # 55.2(324) |--------------|---------------|--------------|---------------
        cls.index.outlets += [
            # delivery service 103
            Outlet(
                point_id=2001,
                delivery_service_id=103,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    day_from=1, day_to=1, order_before=2, work_in_holiday=True, price=100
                ),
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(37.12, 55.32),
            ),
            Outlet(
                point_id=2002,
                delivery_service_id=103,
                region=213,
                point_type=Outlet.FOR_POST_TERM,
                delivery_option=OutletDeliveryOption(
                    day_from=1, day_to=1, order_before=2, work_in_holiday=True, price=200
                ),
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(37.3, 55.5),
            ),
            # delivery service 123
            Outlet(
                point_id=2003,
                delivery_service_id=123,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(day_from=1, day_to=1, price=300),
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(37.5, 55.5),
            ),
            Outlet(
                point_id=2004,
                delivery_service_id=123,
                region=213,
                point_type=Outlet.FOR_POST_TERM,
                delivery_option=OutletDeliveryOption(day_from=1, day_to=1, price=400),
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(37.7, 55.7),
            ),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5001,
                carriers=[103],
                options=[PickupOption(outlet_id=2001, day_from=1, day_to=1, price=100)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5002,
                carriers=[103],
                options=[PickupOption(outlet_id=2002, day_from=1, day_to=1, price=200)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5003,
                carriers=[123],
                options=[PickupOption(outlet_id=2003, day_from=1, day_to=1, price=300)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5004,
                carriers=[123],
                options=[PickupOption(outlet_id=2004, day_from=1, day_to=1, price=400)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

    @classmethod
    def prepare_multiple_outlet_type_for_multiple_delivery_services(cls):
        cls.index.shops += [
            Shop(fesh=300, priority_region=213, delivery_service_outlets=[2001, 2002, 2003, 2004], cpa=Shop.CPA_REAL),
        ]

        cls.index.shipment_service_calendars += [
            DeliveryCalendar(fesh=300, calendar_id=123, date_switch_hour=2, holidays=[3, 4, 5, 6, 14, 20, 21, 27, 28]),
            DeliveryCalendar(
                fesh=300, calendar_id=103, date_switch_hour=2, holidays=[0, 1, 2, 5, 6, 14, 20, 21, 27, 28]
            ),
        ]

        cls.index.offers += [
            Offer(
                fesh=300,
                hyperid=3333,
                post_term_delivery=True,
                cpa=Offer.CPA_REAL,
                pickup_buckets=[5001, 5002, 5003, 5004],
            ),
            Offer(fesh=300, hyperid=4444, post_term_delivery=True, cpa=Offer.CPA_REAL, pickup_buckets=[5002, 5004]),
            Offer(
                fesh=300,
                hyperid=5555,
                post_term_delivery=True,
                pickup=False,
                cpa=Offer.CPA_REAL,
                pickup_buckets=[5001, 5002, 5003, 5004],
            ),
            Offer(fesh=300, hyperid=6666, post_term_delivery=True, cpa=Offer.CPA_NO),
            Offer(fesh=300, hyperid=7777, post_term_delivery=True, cpa=Offer.CPA_NO),
            Offer(fesh=300, hyperid=8888, post_term_delivery=True, pickup=False, cpa=Offer.CPA_NO),
        ]

    def test_multiple_outlet_type_for_multiple_delivery_services(self):
        """Проверяется, что в выдаче присутствуют аутлеты разных типов для разных служб"""
        """    Дополнительно проверяется, что &offer-shipping=pickup -- аггрегирующий фильтр (разрешает аутлеты depot и post_term типов)"""
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        for shipping in ['', '&offer-shipping=pickup']:
            response = self.report.request_json('place=geo&hyperid=3333&rids=213' + shipping + unified_off_flags)
            self.assertFragmentIn(
                response,
                {
                    "total": 4,
                    "shopOutlets": 4,
                    "results": [
                        {
                            "entity": "offer",
                            "outlet": {
                                "id": "2001",
                                "type": "pickup",
                                "serviceId": 103,
                                "selfDeliveryRule": {"shipmentDay": 3, "dayFrom": 4, "dayTo": 4, "cost": "100"},
                            },
                        },
                        {
                            "entity": "offer",
                            "outlet": {
                                "id": "2002",
                                "type": "pickup",
                                "serviceId": 103,
                                "selfDeliveryRule": {"shipmentDay": 3, "dayFrom": 4, "dayTo": 4, "cost": "200"},
                            },
                        },
                        {
                            "entity": "offer",
                            "outlet": {
                                "id": "2003",
                                "type": "pickup",
                                "serviceId": 123,
                                "selfDeliveryRule": {
                                    "shipmentDay": 1,
                                    "dayFrom": 2,  # 2 but not 1 day because
                                    "dayTo": 2,  # ordered after switch hour
                                    "cost": "300",
                                },
                            },
                        },
                        {
                            "entity": "offer",
                            "outlet": {
                                "id": "2004",
                                "type": "pickup",
                                "serviceId": 123,
                                "selfDeliveryRule": {
                                    "shipmentDay": 1,
                                    "dayFrom": 2,  # 2 but not 1 day because
                                    "dayTo": 2,  # ordered after switch hour
                                    "cost": "400",
                                },
                            },
                        },
                    ],
                },
            )
            self.assertEqual(4, response.count({"outlet": {}}))
            """    Проверяется, что атулеты разных типов собираются в счетчике pickupStoresCount"""
            self.assertFragmentIn(
                response,
                {
                    "shop": {
                        "id": 300,
                        "outletsCount": 4,
                        "storesCount": 0,
                        "pickupStoresCount": 4,
                        "depotStoresCount": 2,
                        "postomatStoresCount": 2,
                    }
                },
            )

        """Проверяется фильтр offer-shipping"""
        response = self.report.request_json('place=geo&hyperid=3333&rids=213&offer-shipping=depot' + unified_off_flags)
        self.assertFragmentIn(
            response,
            {
                "total": 2,
                "shopOutlets": 2,
                "results": [
                    {"entity": "offer", "outlet": {"id": "2001", "type": "pickup", "serviceId": 103}},
                    {"entity": "offer", "outlet": {"id": "2003", "type": "pickup", "serviceId": 123}},
                ],
            },
        )
        self.assertEqual(2, response.count({"outlet": {}}))
        """    Проверяется, что атулеты разных типов собираются в счетчике pickupStoresCount"""
        self.assertFragmentIn(
            response,
            {
                "shop": {
                    "id": 300,
                    "outletsCount": 2,
                    "storesCount": 0,
                    "pickupStoresCount": 2,
                    "depotStoresCount": 2,
                    "postomatStoresCount": 0,
                }
            },
        )

        response = self.report.request_json(
            'place=geo&hyperid=3333&rids=213&offer-shipping=postomat' + unified_off_flags
        )
        self.assertFragmentIn(
            response,
            {
                "total": 2,
                "shopOutlets": 2,
                "results": [
                    {"entity": "offer", "outlet": {"id": "2002", "type": "pickup", "serviceId": 103}},
                    {"entity": "offer", "outlet": {"id": "2004", "type": "pickup", "serviceId": 123}},
                ],
            },
        )
        self.assertEqual(2, response.count({"outlet": {}}))
        """    Проверяется, что атулеты разных типов собираются в счетчике pickupStoresCount"""
        self.assertFragmentIn(
            response,
            {
                "shop": {
                    "id": 300,
                    "outletsCount": 2,
                    "storesCount": 0,
                    "pickupStoresCount": 2,
                    "depotStoresCount": 0,
                    "postomatStoresCount": 2,
                }
            },
        )

        """Проверяется, что только почтоматы(2002, 2004) выводятся для оффера с hyperid=4444, т.к. только почтоматы выбраны для служб 123 и 103"""
        response = self.report.request_json('place=geo&hyperid=4444&rids=213' + unified_off_flags)
        self.assertFragmentIn(
            response,
            {
                "total": 2,
                "shopOutlets": 2,
                "results": [
                    {"entity": "offer", "outlet": {"id": "2002", "type": "pickup", "serviceId": 103}},
                    {"entity": "offer", "outlet": {"id": "2004", "type": "pickup", "serviceId": 123}},
                ],
            },
        )
        self.assertEqual(2, response.count({"outlet": {}}))

        common_request = (
            'place=geo&hyperid={0}&rids=213&tile=617,323&tile=618,322&tile=619,321&zoom=10' + unified_off_flags
        )
        """Проверяется, что в непустых тайлах появляются 2001, 2002, 2003, 2004 аутелты для оффера с hyperid=3333"""
        response = self.report.request_json(common_request.format('3333'))
        self.assertFragmentIn(
            response,
            {
                "tiles": [
                    {
                        "entity": "tile",
                        "coord": {"x": 617, "y": 323, "zoom": 10},
                        "outlets": [
                            {"entity": "outlet", "id": "2001"},
                        ],
                    },
                    {
                        "entity": "tile",
                        "coord": {"x": 618, "y": 322, "zoom": 10},
                        "outlets": [
                            {"entity": "outlet", "id": "2002"},
                            {"entity": "outlet", "id": "2003"},
                        ],
                    },
                    {
                        "entity": "tile",
                        "coord": {"x": 619, "y": 321, "zoom": 10},
                        "outlets": [
                            {"entity": "outlet", "id": "2004"},
                        ],
                    },
                ]
            },
        )

        """Проверяется, что тайлы фильтруются &offer-shipping"""
        response = self.report.request_json(common_request.format('3333') + "&offer-shipping=depot")
        self.assertFragmentIn(
            response,
            {
                "tiles": [
                    {
                        "entity": "tile",
                        "coord": {"x": 617, "y": 323, "zoom": 10},
                        "outlets": [
                            {"entity": "outlet", "id": "2001"},
                        ],
                    },
                    {
                        "entity": "tile",
                        "coord": {"x": 618, "y": 322, "zoom": 10},
                        "outlets": [{"entity": "outlet", "id": "2003"}],
                    },
                ]
            },
        )
        self.assertEqual(2, response.count({"outlet": {}}))
        response = self.report.request_json(common_request.format('3333') + "&offer-shipping=postomat")
        self.assertFragmentIn(
            response,
            {
                "tiles": [
                    {
                        "entity": "tile",
                        "coord": {"x": 618, "y": 322, "zoom": 10},
                        "outlets": [{"entity": "outlet", "id": "2002"}],
                    },
                    {
                        "entity": "tile",
                        "coord": {"x": 619, "y": 321, "zoom": 10},
                        "outlets": [
                            {"entity": "outlet", "id": "2004"},
                        ],
                    },
                ]
            },
        )
        self.assertEqual(2, response.count({"outlet": {}}))

        """Проверяется, что в непустых тайлах появляются только почтоматы(2002, 2004) для оффера с hyperid=4444"""
        response = self.report.request_json(common_request.format('4444'))
        self.assertFragmentIn(
            response,
            {
                "tiles": [
                    {"entity": "tile", "coord": {"x": 617, "y": 323, "zoom": 10}, "outlets": ElementCount(0)},
                    {
                        "entity": "tile",
                        "coord": {"x": 618, "y": 322, "zoom": 10},
                        "outlets": [
                            {"entity": "outlet", "id": "2002"},
                        ],
                    },
                    {
                        "entity": "tile",
                        "coord": {"x": 619, "y": 321, "zoom": 10},
                        "outlets": [
                            {"entity": "outlet", "id": "2004"},
                        ],
                    },
                ]
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                "entity": "tile",
                "coord": {"x": 618, "y": 322, "zoom": 10},
                "outlets": [
                    {"entity": "outlet", "id": "2003"},
                ],
            },
        )

        """Проверяется, что place=outlets правильно работает с аутлетами разных типов из нескольких служб"""
        response = self.report.request_xml('place=outlets&rids=213&outlets=2001,2002,2003,2004&fesh=300')
        self.assertFragmentIn(
            response,
            '''
            <outlets>
                <outlet>
                    <PointId>2001</PointId>
                    <PointType>DEPOT</PointType>
                    <SelfDeliveryRule>
                        <CalcInletShipmentDays>3</CalcInletShipmentDays>
                        <CalcMinDeliveryDays>4</CalcMinDeliveryDays>
                        <CalcMaxDeliveryDays>4</CalcMaxDeliveryDays>
                        <CalcCost>100</CalcCost>
                    </SelfDeliveryRule>
                </outlet>
                <outlet>
                    <PointId>2002</PointId>
                    <PointType>DEPOT</PointType>
                    <SelfDeliveryRule>
                        <CalcInletShipmentDays>3</CalcInletShipmentDays>
                        <CalcMinDeliveryDays>4</CalcMinDeliveryDays>
                        <CalcMaxDeliveryDays>4</CalcMaxDeliveryDays>
                        <CalcCost>200</CalcCost>
                    </SelfDeliveryRule>
                </outlet>
                <outlet>
                    <PointId>2003</PointId>
                    <PointType>DEPOT</PointType>
                    <SelfDeliveryRule>
                        <CalcInletShipmentDays>1</CalcInletShipmentDays>
                        <CalcMinDeliveryDays>2</CalcMinDeliveryDays>
                        <CalcMaxDeliveryDays>2</CalcMaxDeliveryDays>
                        <CalcCost>300</CalcCost>
                    </SelfDeliveryRule>
                </outlet>
                <outlet>
                    <PointId>2004</PointId>
                    <PointType>DEPOT</PointType>
                    <SelfDeliveryRule>
                        <CalcInletShipmentDays>1</CalcInletShipmentDays>
                        <CalcMinDeliveryDays>2</CalcMinDeliveryDays>
                        <CalcMaxDeliveryDays>2</CalcMaxDeliveryDays>
                        <CalcCost>400</CalcCost>
                    </SelfDeliveryRule>
                </outlet>
            </outlets>
        ''',
        )

        """Проверяется, что для оффера с hyperid=5555 флаг pickup=False отключает аутлеты типа DEPOT"""
        response = self.report.request_json('place=geo&hyperid=5555&rids=213')
        self.assertFragmentIn(
            response,
            {
                "total": 2,
                "shopOutlets": 2,
                "results": [
                    {"entity": "offer", "outlet": {"id": "2002", "type": "pickup", "serviceId": 103}},
                    {"entity": "offer", "outlet": {"id": "2004", "type": "pickup", "serviceId": 123}},
                ],
            },
            allow_different_len=False,
        )

    OUTLET_2001 = """
        <outlet>
            <PointId>2001</PointId>
            <PointType>DEPOT</PointType>
            <SelfDeliveryRule>
                <CalcInletShipmentDays>3</CalcInletShipmentDays>
                <CalcMinDeliveryDays>4</CalcMinDeliveryDays>
                <CalcMaxDeliveryDays>4</CalcMaxDeliveryDays>
                <CalcCost>100</CalcCost>
            </SelfDeliveryRule>
        </outlet>
    """

    OUTLET_2002 = """
        <outlet>
            <PointId>2002</PointId>
            <PointType>DEPOT</PointType>
            <SelfDeliveryRule>
                <CalcInletShipmentDays>3</CalcInletShipmentDays>
                <CalcMinDeliveryDays>4</CalcMinDeliveryDays>
                <CalcMaxDeliveryDays>4</CalcMaxDeliveryDays>
                <CalcCost>200</CalcCost>
            </SelfDeliveryRule>
        </outlet>
    """

    OUTLET_2003 = """
        <outlet>
            <PointId>2003</PointId>
            <PointType>DEPOT</PointType>
            <SelfDeliveryRule>
                <CalcInletShipmentDays>1</CalcInletShipmentDays>
                <CalcMinDeliveryDays>2</CalcMinDeliveryDays>
                <CalcMaxDeliveryDays>2</CalcMaxDeliveryDays>
                <CalcCost>300</CalcCost>
            </SelfDeliveryRule>
        </outlet>
    """

    OUTLET_2004 = """
        <outlet>
            <PointId>2004</PointId>
            <PointType>DEPOT</PointType>
            <SelfDeliveryRule>
                <CalcInletShipmentDays>1</CalcInletShipmentDays>
                <CalcMinDeliveryDays>2</CalcMinDeliveryDays>
                <CalcMaxDeliveryDays>2</CalcMaxDeliveryDays>
                <CalcCost>400</CalcCost>
            </SelfDeliveryRule>
        </outlet>
    """

    """
    outlet_id   delivery_service_id
    2001        103
    2002        103
    2003        123
    2004        123
    """

    @classmethod
    def prepare_delivery_service_outlets(cls):
        """
        Добавим в магазин собственный аутлет, чтобы проверить работает ли одновременно
        показ аутлета и службы доставки
        """

        cls.index.shops += [
            Shop(fesh=301, priority_region=213, delivery_service_outlets=[2001, 2002, 2003, 2004], cpa=Shop.CPA_REAL),
            Shop(fesh=401, priority_region=213, delivery_service_outlets=[2001, 2002, 2003, 2004]),
            Shop(fesh=501, priority_region=213, delivery_service_outlets=[2003, 2004], cpa=Shop.CPA_REAL),
        ]

        cls.index.shipment_service_calendars += [
            DeliveryCalendar(fesh=301, calendar_id=123, date_switch_hour=2, holidays=[3, 4, 5, 6, 14, 20, 21, 27, 28]),
            DeliveryCalendar(
                fesh=301, calendar_id=103, date_switch_hour=2, holidays=[0, 1, 2, 5, 6, 14, 20, 21, 27, 28]
            ),
            DeliveryCalendar(fesh=401, calendar_id=123, date_switch_hour=2, holidays=[3, 4, 5, 6, 14, 20, 21, 27, 28]),
            DeliveryCalendar(
                fesh=401, calendar_id=103, date_switch_hour=2, holidays=[0, 1, 2, 5, 6, 14, 20, 21, 27, 28]
            ),
            DeliveryCalendar(fesh=501, calendar_id=123, date_switch_hour=5, holidays=[3, 4, 5, 6, 14, 20, 21, 27, 28]),
        ]

        cls.index.outlets += [
            Outlet(point_id=11010, fesh=301, region=213),
            Outlet(point_id=21010, fesh=401, region=213),
        ]

    def _test_delivery_service_outlets(self, url_part, expected_outlets, fesh=301):
        """Проверяется, что place=outlets возвращает аутлеты по id службы"""
        url = "place=outlets&rids=213&fesh={}{}".format(fesh, url_part)
        expected = "<outlets>{}</outlets>".format("".join(expected_outlets))

        response = self.report.request_xml(url)
        self.assertFragmentIn(response, expected)

    def test_delivery_service_outlets_103(self):
        self._test_delivery_service_outlets("&deliveryServiceId=103", [self.OUTLET_2001, self.OUTLET_2002])

    def test_delivery_service_outlets_123(self):
        self._test_delivery_service_outlets("&deliveryServiceId=123", [self.OUTLET_2003, self.OUTLET_2004])

    def test_delivery_service_outlets_103_123(self):
        self._test_delivery_service_outlets(
            "&deliveryServiceId=103&deliveryServiceId=123",
            [self.OUTLET_2001, self.OUTLET_2002, self.OUTLET_2003, self.OUTLET_2004],
        )

        self._test_delivery_service_outlets(
            "&deliveryServiceId=103,123", [self.OUTLET_2001, self.OUTLET_2002, self.OUTLET_2003, self.OUTLET_2004]
        )

    def test_delivery_service_outlets_and_shop_own_oultet(self):
        self._test_delivery_service_outlets(
            "&outlets=11010&deliveryServiceId=103&deliveryServiceId=123",
            [
                "<outlet><PointId>11010</PointId></outlet>",
                self.OUTLET_2001,
                self.OUTLET_2002,
                self.OUTLET_2003,
                self.OUTLET_2004,
            ],
        )


if __name__ == '__main__':
    main()
