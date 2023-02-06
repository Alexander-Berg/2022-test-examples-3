#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.matcher import Wildcard, NoKey, Contains
from core.types import (
    BookingAvailability,
    ClickType,
    CpaCategory,
    CpaCategoryType,
    Currency,
    DeliveryBucket,
    DeliveryOption,
    ExchangeRate,
    GpsCoord,
    HyperCategory,
    HyperCategoryType,
    MnPlace,
    Model,
    NewShopRating,
    Offer,
    Outlet,
    OutletDeliveryOption,
    Phone,
    PickupBucket,
    PickupOption,
    Region,
    Shop,
    UrlType,
    Vendor,
    VendorLogo,
)

from core.testcase import TestCase, main
from core.datasync import DataSyncYandexUserAddress
from core.report import DefaultFlags
from collections import namedtuple
from core.matcher import Absent

SEARCH_FILTERS = '&mcpricefrom=100&mcpriceto=800&&offer-shipping=store&manufacturer_warranty=1&qrfrom=4&free_delivery=1&home_region_filter=225&delivery_interval=2&fesh=18001,18003,18004,18005&show-book-now-only=1&filter-discount-only=1'  # noqa

HOME_GPS_COORD = GpsCoord(37.40, 55.5)
WORK_GPS_COORD = GpsCoord(37.35, 55.15)
FAR_GPS_COORD = GpsCoord(17.17, 85.45)
LOCATION_GPS_COORD = GpsCoord(37.15, 55.15)


class _Offers(object):
    store_offer = Offer(
        hyperid=55564,
        fesh=12320,
        title='store',
        pickup=False,
        price=10,
        waremd5='StoreOffer___________g',
        pickup_buckets=[9301],
    )

    pickup_offer = Offer(
        hyperid=55565,
        fesh=12320,
        title='pickup',
        price=100,
        store=False,
        waremd5='PickupOffer__________g',
        pickup_buckets=[9301],
    )

    post_term_offer = Offer(
        hyperid=55566,
        fesh=12320,
        title='post_term',
        price=1000,
        pickup=False,
        store=False,
        post_term_delivery=True,
        waremd5='PostTermOffer________g',
        pickup_buckets=[9302],
    )


class T(TestCase):
    @classmethod
    def prepare_basic(cls):
        # rids: [1, 100]
        # fesh: [101, 200]
        # outlet id: [201, 300]
        # hyperid: [301, 400]
        # hid: [401, 500]

        # RANDX randomizing is disabled because these tests don't work with it. See MARKETOUT-21319
        cls.disable_randx_randomize()

        cls.index.regiontree += [
            Region(rid=1, name='Город'),
            Region(rid=10, name='Страна', region_type=Region.COUNTRY),
            Region(rid=213, name='Москва для non-guru'),
        ]

        cls.index.shops += [
            Shop(fesh=101, priority_region=1, name='Shop1'),
            Shop(fesh=102, priority_region=1, name='Shop2'),
            Shop(fesh=103, priority_region=1, name='Shop3'),
            Shop(fesh=104, priority_region=1, name='Shop4', new_shop_rating=NewShopRating(new_rating_total=5.0)),
            Shop(
                fesh=105,
                priority_region=1,
                name='Shop5',
                new_shop_rating=NewShopRating(new_rating_total=3.0),
                home_region=10,
            ),
            Shop(fesh=106, priority_region=1, name='Shop6', delivery_service_outlets=[261, 262]),
            Shop(fesh=107, priority_region=1, name='Shop7', delivery_service_outlets=[261, 262]),
            # For testing postomat output on geo
            Shop(fesh=110, priority_region=1, name='Shop10', delivery_service_outlets=[400, 401]),
            Shop(fesh=111, priority_region=1, name='Shop11', delivery_service_outlets=[400, 401]),
        ]

        cls.index.outlets += [
            Outlet(point_id=211, fesh=101, region=1, gps_coord=GpsCoord(37.1, 55.1), point_type=Outlet.FOR_STORE),
            Outlet(point_id=212, fesh=101, region=1, gps_coord=GpsCoord(37.1, 55.3)),
            Outlet(point_id=213, fesh=101, region=1, gps_coord=GpsCoord(37.3, 55.3), point_type=Outlet.FOR_PICKUP),
            Outlet(point_id=214, fesh=101, region=1, gps_coord=GpsCoord(37.3, 55.1), point_type=Outlet.FOR_STORE),
            Outlet(point_id=215, fesh=101, region=1, gps_coord=GpsCoord(37.5, 55.5)),
            Outlet(point_id=216, fesh=101, region=1, gps_coord=GpsCoord(37.7, 55.7), point_type=Outlet.FOR_PICKUP),
            Outlet(point_id=221, fesh=102, region=1, gps_coord=GpsCoord(37.1, 55.5), point_type=Outlet.FOR_STORE),
            Outlet(point_id=222, fesh=102, region=1, gps_coord=GpsCoord(37.3, 55.5), point_type=Outlet.FOR_PICKUP),
            Outlet(point_id=223, fesh=102, region=1, gps_coord=GpsCoord(37.72, 55.72), point_type=Outlet.FOR_PICKUP),
            Outlet(point_id=231, fesh=103, region=1, gps_coord=GpsCoord(37.12, 55.12), point_type=Outlet.FOR_STORE),
            Outlet(point_id=232, fesh=103, region=1, gps_coord=GpsCoord(37.12, 55.32), point_type=Outlet.FOR_PICKUP),
            Outlet(point_id=233, fesh=103, region=1, gps_coord=GpsCoord(37.32, 55.32)),
            Outlet(point_id=241, fesh=104, region=1, gps_coord=GpsCoord(37.42, 55.42)),
            # Аутлеты для тестирования is-main и pickup-price
            Outlet(
                point_id=251,
                fesh=105,
                region=1,
                is_main=False,
                gps_coord=GpsCoord(55.3, 37.7),
                point_type=Outlet.FOR_PICKUP,
                phones=[
                    Phone('+7-495-123-45-67*89'),
                    Phone('+7-495-987-65-43*21'),
                    Phone('+7 (812) 765-43-21'),
                    Phone('+7 495 765-43-21'),
                    Phone('+7 812 305 26 71'),
                    Phone('+7 123 4561234'),
                    Phone('8 (7655) 4-72110'),
                    Phone('(495) 355-43-21'),
                    Phone('355-43-21'),
                    Phone('1-23-456'),
                    Phone('987654'),
                ],
                delivery_option=OutletDeliveryOption(price=200),
            ),
            Outlet(
                point_id=252,
                fesh=105,
                region=1,
                is_main=False,
                gps_coord=GpsCoord(55.25, 37.7),
                point_type=Outlet.FOR_STORE,
                delivery_option=OutletDeliveryOption(price=100),
            ),
            Outlet(
                point_id=253,
                fesh=105,
                region=1,
                is_main=True,
                gps_coord=GpsCoord(55.5, 37.7),
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(price=100),
            ),
            Outlet(
                point_id=254,
                fesh=105,
                region=1,
                is_main=True,
                gps_coord=GpsCoord(55.45, 37.7),
                point_type=Outlet.FOR_STORE,
                delivery_option=OutletDeliveryOption(price=200),
            ),
            # Постаматы
            Outlet(
                point_id=261,
                region=1,
                gps_coord=GpsCoord(37.12, 55.02),
                point_type=Outlet.FOR_POST_TERM,
                delivery_service_id=103,
                delivery_option=OutletDeliveryOption(price=150),
            ),
            Outlet(
                point_id=262,
                region=1,
                gps_coord=GpsCoord(37.3, 55.25),
                point_type=Outlet.FOR_POST_TERM,
                delivery_service_id=103,
                delivery_option=OutletDeliveryOption(price=150),
            ),
            # Это не постамат, для тестирования фильтрации постаматов от book_now
            Outlet(
                point_id=263,
                region=1,
                fesh=107,
                gps_coord=GpsCoord(37.4, 55.25),
                point_type=Outlet.FOR_STORE,
                delivery_option=OutletDeliveryOption(price=150),
            ),
            # For testing postomat output on geo
            Outlet(
                point_id=400,
                delivery_service_id=103,
                region=1,
                gps_coord=GpsCoord(55.45, 37.7),
                point_type=Outlet.FOR_POST_TERM,
                delivery_option=OutletDeliveryOption(price=200),
            ),
            Outlet(
                point_id=401,
                delivery_service_id=103,
                region=1,
                gps_coord=GpsCoord(55.46, 37.7),
                point_type=Outlet.FOR_POST_TERM,
                delivery_option=OutletDeliveryOption(price=200),
            ),
            Outlet(
                point_id=410,
                fesh=110,
                region=1,
                gps_coord=GpsCoord(55.46, 37.7),
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(price=200),
            ),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5001,
                fesh=101,
                carriers=[99],
                options=[
                    PickupOption(outlet_id=211),
                    PickupOption(outlet_id=212),
                    PickupOption(outlet_id=213),
                    PickupOption(outlet_id=214),
                    PickupOption(outlet_id=215),
                    PickupOption(outlet_id=216),
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5002,
                fesh=102,
                carriers=[99],
                options=[PickupOption(outlet_id=221), PickupOption(outlet_id=222), PickupOption(outlet_id=223)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5003,
                fesh=103,
                carriers=[99],
                options=[PickupOption(outlet_id=231), PickupOption(outlet_id=232), PickupOption(outlet_id=233)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5004,
                fesh=104,
                carriers=[99],
                options=[PickupOption(outlet_id=241)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5005,
                fesh=105,
                carriers=[99],
                options=[
                    PickupOption(outlet_id=251, price=200),
                    PickupOption(outlet_id=252, price=100),
                    PickupOption(outlet_id=253, price=100),
                    PickupOption(outlet_id=254, price=200),
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5006,
                carriers=[103],
                options=[PickupOption(outlet_id=261, price=150), PickupOption(outlet_id=262, price=150)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5007,
                fesh=107,
                carriers=[99],
                options=[PickupOption(outlet_id=263, price=150)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5008,
                carriers=[103],
                options=[PickupOption(outlet_id=401), PickupOption(outlet_id=401)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5009,
                fesh=110,
                carriers=[99],
                options=[PickupOption(outlet_id=410)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        # Аутлеты на "карте"
        # Числа в скобках - координаты тайлов при zoom = 10
        #           37.0(617)     37.2(617)       37.4(618)      37.6(618)       37.8(619)
        # 55.8(321) |--------------|---------------|--------------|---------------
        #           |              |               |              |       *(223)
        #           |              |               |              |      *(216)
        #           |              |               |              |
        #           |              |               |              |
        # 55.6(322) |--------------|---------------|--------------|---------------
        #           |              |               |              |
        #           |     *(221)   |    *(222)     |              |      *(253)
        #           |              |               |      *(215)  |      *(254)
        #           |              |               |*(241)        |
        # 55.4(323) |--------------|---------------|--------------|---------------
        #           |              |               |              |
        #           |     *(232)   |      *(233)   |              |      *(251)
        #           |    *(212)    |    *(213)     |              |      *(252)
        #           |              | *(262)PostTerm|              |
        # 55.2(324) |--------------|---------------|--------------|---------------
        #           |      *We     |               |              |
        #           |     *(231)   |               |              |
        #           |  *(261)PostTe|    *(214)     |              |
        #           |  *(211)      |               |              |
        # 55.0(325) |--------------|---------------|--------------|---------------
        cls.index.vendors += [
            Vendor(
                vendor_id=401,
                name='samsung',
                website='www.samsung.com',
                webpage_recommended_shops='http://www.samsung.com/ru/brandshops/',
                description='VendorDescription',
                logos=[VendorLogo(url='//mdata.yandex.net/i?path=b0726220734_img_id5949902134120952009.png')],
            ),
        ]

        cls.index.hypertree += [HyperCategory(hid=504, output_type=HyperCategoryType.GURU)]

        cls.index.models += [
            Model(hyperid=301, title='Ноутбук с модификациями', hid=501, vendor_id=401),
            Model(hyperid=302, title='Телефон с модификациями', hid=502),
            Model(hyperid=303, title='Планшет с модификациями', hid=503),
            # For testing of postomat output on geo
            Model(hyperid=350, title='pepyaka-naturalnaya', hid=550),
        ]

        cls.index.offers += [
            Offer(
                fesh=101,
                title='good11',
                hyperid=301,
                price=100,
                bid=100,
                randx=100,
                booking_availabilities=[
                    BookingAvailability(outlet_id=211, region_id=1, amount=5),
                    BookingAvailability(outlet_id=212, region_id=1, amount=5),
                    BookingAvailability(outlet_id=213, region_id=1, amount=5),
                    BookingAvailability(outlet_id=214, region_id=1, amount=5),
                    BookingAvailability(outlet_id=215, region_id=1, amount=5),
                    BookingAvailability(outlet_id=216, region_id=1, amount=5),
                ],
                pickup_buckets=[5001],
            ),
            Offer(
                fesh=101,
                title='good12',
                hyperid=301,
                price=150,
                bid=150,
                randx=150,
                booking_availabilities=[
                    BookingAvailability(outlet_id=211, region_id=1, amount=5),
                    BookingAvailability(outlet_id=212, region_id=1, amount=5),
                    BookingAvailability(outlet_id=213, region_id=1, amount=5),
                ],
                pickup_buckets=[5001],
            ),
            Offer(
                fesh=101,
                title='good13',
                hyperid=301,
                price=90,
                bid=150,
                randx=150,
                booking_availabilities=[
                    BookingAvailability(outlet_id=211, region_id=1, amount=5),
                    BookingAvailability(outlet_id=212, region_id=1, amount=5),
                ],
                pickup_buckets=[5001],
            ),
            Offer(fesh=101, title='good14', hyperid=301, price=80, bid=100, randx=90, pickup_buckets=[5001]),
            Offer(
                fesh=102,
                title='good21',
                hyperid=301,
                vendor_id=401,
                price=100,
                bid=250,
                cbid=250,
                randx=250,
                waremd5='2b0-iAnHLZST2Ekoq4xElr',
                booking_availabilities=[
                    BookingAvailability(outlet_id=221, region_id=1, amount=5),
                    BookingAvailability(outlet_id=222, region_id=1, amount=5),
                ],
                pickup_buckets=[5002],
            ),
            Offer(
                fesh=102,
                title='good21_vcluster',
                vclusterid=1000000007,
                price=100,
                bid=250,
                randx=250,
                waremd5='ppp-iAnHLZST2Ekoq4xElr',
                booking_availabilities=[
                    BookingAvailability(outlet_id=221, region_id=1, amount=5),
                    BookingAvailability(outlet_id=222, region_id=1, amount=5),
                ],
                pickup_buckets=[5002],
            ),
            Offer(fesh=102, title='good22', hyperid=301, price=90, bid=100, randx=90, pickup_buckets=[5002]),
            Offer(
                fesh=103,
                title='good31',
                hyperid=301,
                price=100,
                bid=50,
                randx=50,
                booking_availabilities=[
                    BookingAvailability(outlet_id=231, region_id=1, amount=5),
                ],
                pickup_buckets=[5003],
            ),
            Offer(
                fesh=103,
                title='good32',
                hyperid=301,
                price=100,
                bid=40,
                randx=40,
                booking_availabilities=[
                    BookingAvailability(outlet_id=231, region_id=1, amount=5),
                    BookingAvailability(outlet_id=232, region_id=1, amount=5),
                    BookingAvailability(outlet_id=233, region_id=1, amount=5),
                ],
                pickup_buckets=[5003],
            ),
            Offer(fesh=103, title='good33', hyperid=301, price=90, bid=30, randx=30, pickup_buckets=[5003]),
            Offer(fesh=104, title='good41', hyperid=301, price=90, bid=100, randx=90, pickup_buckets=[5004]),
            Offer(
                fesh=106,
                title='good_PostTerm_6',
                hyperid=301,
                price=90,
                bid=100,
                randx=90,
                post_term_delivery=True,
                pickup_buckets=[5006],
            ),
            Offer(
                fesh=107,
                title='Post_terminal_and_book_now_6',
                hyperid=701,
                price=90,
                bid=100,
                randx=90,
                post_term_delivery=True,
                booking_availabilities=[
                    BookingAvailability(outlet_id=263, region_id=1, amount=5),
                ],
                pickup_buckets=[5007, 5006],
            ),
            Offer(
                fesh=105,
                title='good51',
                hyperid=302,
                price=90,
                bid=100,
                randx=90,
                booking_availabilities=[
                    BookingAvailability(outlet_id=251, region_id=1, amount=5),
                    BookingAvailability(outlet_id=252, region_id=1, amount=5),
                    BookingAvailability(outlet_id=253, region_id=1, amount=5),
                    BookingAvailability(outlet_id=254, region_id=1, amount=5),
                ],
                pickup_buckets=[5005],
            ),
            # Офферы для тестирования фильтров
            Offer(
                fesh=104,
                title='good52',
                hyperid=303,
                price=90,
                randx=90,
                manufacturer_warranty=True,
                has_delivery_options=False,
                available=True,
                pickup_buckets=[5004],
            ),
            Offer(
                fesh=105,
                title='good53',
                hyperid=303,
                price=90,
                randx=90,
                manufacturer_warranty=False,
                has_delivery_options=False,
                available=False,
                pickup_buckets=[5005],
            ),
            # For testing of postomat output on geo
            Offer(
                fesh=110,
                title='pepyaka-postomatish-1',
                price=1600,
                post_term_delivery=True,
                pickup_buckets=[5008, 5009],
            ),
            Offer(
                fesh=110,
                title='pepyaka-postomatish-2',
                price=1900,
                post_term_delivery=True,
                pickup_buckets=[5008, 5009],
            ),
            Offer(
                fesh=111,
                title='pepyaka-postomatish-3',
                price=1800,
                post_term_delivery=True,
            ),
            Offer(
                fesh=111,
                title='pepyaka-postomatish-4',
                price=1700,
                post_term_delivery=True,
            ),
        ]

        # Для тестирования групповой модели
        cls.index.models += [
            Model(hid=401, hyperid=311, group_hyperid=310),
            Model(hid=401, hyperid=312, group_hyperid=310),
        ]
        cls.index.offers += [
            Offer(fesh=104, hid=401, hyperid=310, pickup_buckets=[5004]),
            Offer(fesh=104, hid=401, hyperid=311, pickup_buckets=[5004]),
            Offer(fesh=104, hid=401, hyperid=312, pickup_buckets=[5004]),
        ]

        # Для тестирования offer-shipping
        for hyperid in range(1000, 1100):
            cls.index.models += [Model(hid=601, hyperid=hyperid)]

        cls.index.offers += [
            Offer(fesh=105, hid=601, hyperid=1100, pickup_buckets=[5005]),
        ]

    def test_geo_output_format(self):
        # Что тестируем: формат выдачи аутлета без применения фильтров
        # Оффер good21 доступен в трех аутлетах ("bundleCount": 3)
        # В аутлете 211 доступны два оффера ("bundleCount": 2)
        response = self.report.request_json('place=geo&hid=501&rids=0&show-urls=geo,geoOutlet,geoPointInfo')
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "titles": {"raw": "good21"},
                "bundleCount": 2,
                "bundled": {"outletId": 221, "count": 2},
                "outlet": {
                    "entity": "outlet",
                    "id": "221",
                    "name": "OUTLET-102-221",
                    "type": "store",
                    "email": "",
                    "shop": {"id": 102},
                    "address": {"locality": "", "street": "", "building": "", "block": "", "note": ""},
                    "region": {
                        "entity": "region",
                        "id": 1,
                        "name": u"Город",
                        "lingua": {"name": {"genitive": u"Город", "preposition": " ", "prepositional": u"Город"}},
                    },
                    "gpsCoord": {"longitude": "37.1", "latitude": "55.5"},
                    "bundleCount": 3,
                    "bundled": {"count": 3},
                },
                'vendor': {
                    'name': 'samsung',
                    'website': 'www.samsung.com',
                    "description": "VendorDescription",
                    "logo": {
                        "entity": "picture",
                        "url": "//mdata.yandex.net/i?path=b0726220734_img_id5949902134120952009.png",
                    },
                    "webpageRecommendedShops": "http://www.samsung.com/ru/brandshops/",
                },
            },
            preserve_order=True,
        )

        # Проверяем наличие записей в логах
        self.click_log.expect(ClickType.GEO, url_type=UrlType.GEO, shop_id=102, dtype='cpa', type_id=3)
        self.click_log.expect(ClickType.GEO_OUTLET, url_type=UrlType.GEO_OUTLET, shop_id=102)
        self.click_log.expect(ClickType.GEO_OUTLET_INFO, url_type=UrlType.GEO_OUTLET_INFO, shop_id=102)
        self.show_log.expect(url='//market.yandex.ru/geo?fesh=102&offerid=2b0-iAnHLZST2Ekoq4xElg', url_type=UrlType.GEO)
        self.show_log.expect(
            url='//market.yandex.ru/geo?fesh=102&offerid=2b0-iAnHLZST2Ekoq4xElg&point_id=221',
            url_type=UrlType.GEO_OUTLET,
        )
        self.show_log.expect(
            url='//market.yandex.ru/gate/maps/getpointinfo.xml?offerid=2b0-iAnHLZST2Ekoq4xElg&point_id=221',
            url_type=UrlType.GEO_OUTLET_INFO,
        )

    def test_show_offer_param(self):
        # Что тестируем: работу cgi-параметра &show-offer

        # Поведение по умолчанию - показ и офферов и тайлов
        response = self.report.request_json('place=geo&hyperid=301&tile=619,321&zoom=10&rids=0')
        self.assertFragmentIn(response, {"results": [], "tiles": []}, preserve_order=True)

        # Показ только офферов
        response = self.report.request_json('place=geo&show-outlet=offers&hyperid=301&rids=0')
        self.assertFragmentIn(response, {"results": []}, preserve_order=True)
        self.assertFragmentNotIn(response, {"tiles": []}, preserve_order=True)

        # Показ только тайлов
        response = self.report.request_json('place=geo&show-outlet=tiles&tile=619,321&zoom=10&hyperid=301&rids=0')
        self.assertFragmentIn(response, {"tiles": []}, preserve_order=True)
        self.assertFragmentNotIn(response, {"results": []}, preserve_order=True)

    def test_geo_bounding_small(self):
        # Что тестируем: выдачу с учетом баундинга в маленьком квадрате в двух вариантах:
        # с дефолтным ранжированием и с ранжированием по удаленности
        # В выдаче должны остаться аутлеты 211 и 231 (см "карту")
        response = self.report.request_json(
            'place=geo&geo-location=37.15,55.15&geo_bounds_lb=37.0,55.0&geo_bounds_rt=37.2,55.2&hyperid=301&rids=0'
        )
        self.assertFragmentIn(response, {"search": {"total": 3, "shops": 3, "shopOutlets": 3}}, preserve_order=True)
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "titles": {"raw": "good12"},
                    "bundleCount": 4,
                    "bundled": {"modelId": 301, "outletId": 211, "count": 4},
                    "outlet": {"id": "211", "bundleCount": 6, "bundled": {"modelId": 301, "count": 6}},
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "good_PostTerm_6"},
                    "bundleCount": 1,
                    "bundled": {"modelId": 301, "outletId": 261, "count": 1},
                    "outlet": {
                        "id": "261",
                    },
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "good31"},
                    "bundleCount": 3,
                    "bundled": {"modelId": 301, "outletId": 231, "count": 3},
                    "outlet": {"id": "231", "bundleCount": 3, "bundled": {"modelId": 301, "count": 3}},
                },
            ],
            preserve_order=True,
        )

        # Что тестируем: выдача не меняется при regset=1:
        response = self.report.request_json(
            'place=geo&geo-location=37.15,55.15&geo_bounds_lb=37.0,55.0&geo_bounds_rt=37.2,55.2&hyperid=301&rids=1&regset=1'
        )
        self.assertFragmentIn(response, {"search": {"total": 3, "shops": 3, "shopOutlets": 3}}, preserve_order=True)

        # escaped geo coordinates MARKETOUT-27050
        response = self.report.request_json(
            'place=geo&geo-location=37.15%252C55.15&geo_bounds_lb=37.0%252C55.0&geo_bounds_rt=37.2%252C55.2&hyperid=301&rids=1&regset=1'
        )
        self.assertFragmentIn(response, {"search": {"total": 3, "shops": 3, "shopOutlets": 3}}, preserve_order=True)

        response = self.report.request_json(
            'place=geo&how=distance&geo-location=37.15,55.15&geo_bounds_lb=37.0,55.0&geo_bounds_rt=37.2,55.2&hyperid=301&rids=0&touch=1'
        )
        self.assertFragmentIn(response, {"search": {"total": 3, "shops": 3, "shopOutlets": 3}}, preserve_order=True)
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "titles": {"raw": "good31"},
                    "bundleCount": 3,
                    "bundled": {"modelId": 301, "outletId": 231, "count": 3},
                    "outlet": {"id": "231", "bundleCount": 3, "bundled": {"modelId": 301, "count": 3}},
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "good12"},
                    "bundleCount": 4,
                    "bundled": {"modelId": 301, "outletId": 211, "count": 4},
                    "outlet": {"id": "211", "bundleCount": 6, "bundled": {"modelId": 301, "count": 6}},
                },
            ],
            preserve_order=True,
        )

    def test_geo_bounding_large(self):
        # Что тестируем: выдачу с учетом баундинга в большом квадрате в двух вариантах:
        # с дефолтным ранжированием и с ранжированием по удаленности
        # В выдаче должны остаться аутлеты 211-214 и 231-233 (см "карту") и 261,262(Постаматы)
        # Дефолтное значение geo-sort-gran = 2 (т.е. по два аутлета от магазина подряд)
        response = self.report.request_json(
            'place=geo&geo-location=37.15,55.15&geo_bounds_lb=37.0,55.0&geo_bounds_rt=37.4,55.4&hyperid=301&rids=0'
        )
        self.assertFragmentIn(response, {"search": {"total": 9, "shops": 3, "shopOutlets": 9}}, preserve_order=True)
        self.assertFragmentIn(
            response,
            [
                {"entity": "offer", "titles": {"raw": "good12"}, "outlet": {"id": "211"}},
                {"entity": "offer", "titles": {"raw": "good12"}, "outlet": {"id": "214"}},
                {"entity": "offer", "titles": {"raw": "good_PostTerm_6"}, "outlet": {"id": "261"}},
                {"entity": "offer", "titles": {"raw": "good_PostTerm_6"}, "outlet": {"id": "262"}},
                {"entity": "offer", "titles": {"raw": "good31"}, "outlet": {"id": "231"}},
                {"entity": "offer", "titles": {"raw": "good31"}, "outlet": {"id": "232"}},
                {"entity": "offer", "titles": {"raw": "good12"}, "outlet": {"id": "212"}},
                {"entity": "offer", "titles": {"raw": "good12"}, "outlet": {"id": "213"}},
                {"entity": "offer", "titles": {"raw": "good31"}, "outlet": {"id": "233"}},
            ],
            preserve_order=True,
        )

        response = self.report.request_json(
            'place=geo&how=distance&geo-location=37.15,55.15&geo_bounds_lb=37.0,55.0&geo_bounds_rt=37.4,55.4&hyperid=301&rids=0&touch=1'
        )
        self.assertFragmentIn(response, {"search": {"total": 9, "shops": 3, "shopOutlets": 9}}, preserve_order=True)
        self.assertFragmentIn(
            response,
            [
                {"entity": "offer", "titles": {"raw": "good31"}, "outlet": {"id": "231"}},
                {"entity": "offer", "titles": {"raw": "good12"}, "outlet": {"id": "211"}},
                {"entity": "offer", "titles": {"raw": "good12"}, "outlet": {"id": "214"}},
                {"entity": "offer", "titles": {"raw": "good12"}, "outlet": {"id": "212"}},
                {"entity": "offer", "titles": {"raw": "good31"}, "outlet": {"id": "232"}},
                {"entity": "offer", "titles": {"raw": "good12"}, "outlet": {"id": "213"}},
                {"entity": "offer", "titles": {"raw": "good31"}, "outlet": {"id": "233"}},
            ],
            preserve_order=True,
        )

    def test_geo_bounding_empty(self):
        # Что тестируем: выдачу с учетом баундинга пустом квадрате
        response = self.report.request_json(
            'place=geo&how=distance&geo-location=37.15,55.65&geo_bounds_lb=37.0,55.6&geo_bounds_rt=37.2,55.8&hyperid=301&rids=0'
        )
        self.assertFragmentIn(response, {"search": {"total": 0, "shops": 0, "shopOutlets": 0}}, preserve_order=True)
        self.assertFragmentNotIn(response, {"entity": "offer"}, preserve_order=True)

    def test_tile_format(self):
        # Что тестируем: формат выдачи тайла
        response = self.report.request_json('place=geo&tile=619,321&zoom=10&hyperid=301&show-outlet=tiles&rids=0')
        self.assertFragmentIn(
            response,
            {
                "tiles": [
                    {
                        "entity": "tile",
                        "coord": {"x": 619, "y": 321, "zoom": 10},
                        "outlets": [
                            {
                                "entity": "outlet",
                                "id": "216",
                                "type": "pickup",
                                "gpsCoord": {"longitude": "37.7", "latitude": "55.7"},
                            }
                        ],
                    }
                ]
            },
            preserve_order=True,
        )

    def test_multiple_tiles(self):
        # Что тестируем: выдачу аутлетов в группе тайлов
        response = self.report.request_json(
            'place=geo&tile=617,322&tile=617,323&tile=617,324&zoom=10&hyperid=301&show-outlet=tiles&rids=0'
        )
        self.assertFragmentIn(
            response,
            {
                "tiles": [
                    {"coord": {"x": 617, "y": 322}, "outlets": [{"id": "221"}]},
                    {"coord": {"x": 617, "y": 323}, "outlets": [{"id": "212"}, {"id": "232"}]},
                    {"coord": {"x": 617, "y": 324}, "outlets": [{"id": "211"}, {"id": "231"}, {"id": "261"}]},
                ]
            },
            preserve_order=True,
        )

    def test_empty_tile(self):
        # Что тестируем: выдачу пустого тайла
        response = self.report.request_json('place=geo&tile=617,321&zoom=10&hyperid=301&show-outlet=tiles&rids=0')
        self.assertFragmentIn(
            response, {"tiles": [{"coord": {"x": 617, "y": 321, "zoom": 10}, "outlets": []}]}, preserve_order=True
        )
        self.assertFragmentNotIn(response, {"outlets": [{"entity": "outlet"}]}, preserve_order=True)

    def test_total_renderable(self):
        """Проверяется, что общее количество для показа = total"""
        request = 'place=geo&show-book-now-only=1&point_id=211&hyperid=301&rids=0&grhow=offer'
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"total": 3})
        self.assertEqual(3, response.count({"entity": "offer"}))
        response = self.report.request_json(request + '&numdoc=1')
        self.assertFragmentIn(response, {"total": 3})
        self.assertEqual(1, response.count({"entity": "offer"}))
        self.access_log.expect(total_renderable='3').times(2)

    def test_is_main(self):
        # Что тестируем: признак is-main в аутлете. Is-main аутлеты должны располагаться выше не-is-main
        response = self.report.request_json('place=geo&hyperid=302&rids=0')
        self.assertFragmentIn(
            response,
            [
                {"entity": "offer", "outlet": {"id": "254"}},
                {"entity": "offer", "outlet": {"id": "251"}},
            ],
            preserve_order=True,
        )

    def test_pickup_price(self):
        # Что тестируем: признак pickup-price в аутлете. Аутлеты ранжируются по возрастанию цены и
        # возрастанию расстояния до пользователя в случае равенства цен
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        response = self.report.request_json('place=geo&geo-location=37.15,55.15&hyperid=302&rids=0' + unified_off_flags)
        self.assertFragmentIn(
            response,
            [
                {"entity": "offer", "outlet": {"id": "252"}},
                {"entity": "offer", "outlet": {"id": "253"}},
                {"entity": "offer", "outlet": {"id": "251"}},
                {"entity": "offer", "outlet": {"id": "254"}},
            ],
            preserve_order=True,
        )

    def test_geo_output_format_book_now(self):
        # Что тестируем: формат выдаче аутлета в BookNow-выдаче (show-book-now-only=1)
        response = self.report.request_json(
            'place=geo&hyperid=301&show-book-now-only=1&rids=0&show-urls=geo,geoOutlet,geoPointInfo'
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "titles": {"raw": "good21"},
                "bundleCount": 1,
                "bundled": {"modelId": 301, "outletId": 221, "count": 1},
                "outlet": {
                    "entity": "outlet",
                    "id": "221",
                    "name": "OUTLET-102-221",
                    "type": "store",
                    "email": "",
                    "shop": {"id": 102},
                    "address": {"locality": "", "street": "", "building": "", "block": "", "note": ""},
                    "region": {
                        "entity": "region",
                        "id": 1,
                        "name": u"Город",
                        "lingua": {"name": {"genitive": u"Город", "preposition": " ", "prepositional": u"Город"}},
                    },
                    "gpsCoord": {"longitude": "37.1", "latitude": "55.5"},
                    "bundleCount": 2,
                    "bundled": {"modelId": 301, "count": 2},
                },
            },
            preserve_order=True,
        )

        # Проверяем наличие записей в логах
        self.click_log.expect(ClickType.GEO, shop_id=102)
        self.click_log.expect(ClickType.GEO_OUTLET, shop_id=102)
        self.click_log.expect(ClickType.GEO_OUTLET_INFO, shop_id=102)
        self.show_log.expect(url='//market.yandex.ru/geo?fesh=102&offerid=2b0-iAnHLZST2Ekoq4xElg')
        self.show_log.expect(url='//market.yandex.ru/geo?fesh=102&offerid=2b0-iAnHLZST2Ekoq4xElg&point_id=221')
        self.show_log.expect(
            url='//market.yandex.ru/gate/maps/getpointinfo.xml?offerid=2b0-iAnHLZST2Ekoq4xElg&point_id=221'
        )

    def test_geo_output_sorting_book_now(self):
        # Что тестируем: дефолтное ранжирование аутлетов в рамках BookNow с geo-sort-gran=1
        # Также тестируем пейджинг
        # См. описание https://wiki.yandex-team.ru/market/projects/multiregion/projects/pickupinstore/dev/report/geo-response/#ranzhirovanietovarovnakarte
        response = self.report.request_json(
            'place=geo&show-book-now-only=1&numdoc=3&geo-sort-gran=1&hyperid=301&rids=0'
        )
        self.assertFragmentIn(response, {"search": {"total": 11, "shops": 3, "shopOutlets": 11}}, preserve_order=True)
        self.assertFragmentIn(
            response,
            [
                {"entity": "offer", "titles": {"raw": "good21"}, "outlet": {"id": "221"}},
                {"entity": "offer", "titles": {"raw": "good13"}, "outlet": {"id": "211"}},
                {"entity": "offer", "titles": {"raw": "good31"}, "outlet": {"id": "231"}},
            ],
            preserve_order=True,
        )
        response = self.report.request_json(
            'place=geo&show-book-now-only=1&numdoc=3&page=2&geo-sort-gran=1&hyperid=301&rids=0'
        )
        self.assertFragmentIn(
            response,
            [
                {"entity": "offer", "titles": {"raw": "good21"}, "outlet": {"id": "222"}},
                {"entity": "offer", "titles": {"raw": "good13"}, "outlet": {"id": "212"}},
                {"entity": "offer", "titles": {"raw": "good32"}, "outlet": {"id": "232"}},
            ],
            preserve_order=True,
        )
        response = self.report.request_json(
            'place=geo&show-book-now-only=1&numdoc=3&page=3&geo-sort-gran=1&hyperid=301&rids=0'
        )
        self.assertFragmentIn(
            response,
            [
                {"entity": "offer", "titles": {"raw": "good11"}, "outlet": {"id": "213"}},
                {"entity": "offer", "titles": {"raw": "good32"}, "outlet": {"id": "233"}},
                {"entity": "offer", "titles": {"raw": "good11"}, "outlet": {"id": "214"}},
            ],
            preserve_order=True,
        )
        response = self.report.request_json(
            'place=geo&show-book-now-only=1&numdoc=3&page=4&geo-sort-gran=1&hyperid=301&rids=0'
        )
        self.assertFragmentIn(
            response,
            [
                {"entity": "offer", "titles": {"raw": "good11"}, "outlet": {"id": "215"}},
                {"entity": "offer", "titles": {"raw": "good11"}, "outlet": {"id": "216"}},
            ],
            preserve_order=True,
        )

    def test_geo_output_distance_sorting_book_now(self):
        # Что тестируем: ранжирование аутлетов по расстоянию до пользователя в рамках BookNow с geo-sort-gran=1
        # Также тестируем пейджинг
        # См. описание https://wiki.yandex-team.ru/market/projects/multiregion/projects/pickupinstore/dev/report/geo-response/#ranzhirovanietovarovnakarte
        response = self.report.request_json(
            'place=geo&how=distance&numdoc=4&geo-location=37.15,55.15&show-book-now-only=1&geo-sort-gran=1&numdoc=20&hyperid=301&rids=0'
        )
        self.assertFragmentIn(response, {"search": {"total": 11, "shops": 3, "shopOutlets": 11}}, preserve_order=True)
        self.assertFragmentIn(
            response,
            [
                {"entity": "offer", "titles": {"raw": "good31"}, "outlet": {"id": "231"}},
                {"entity": "offer", "titles": {"raw": "good13"}, "outlet": {"id": "211"}},
                {"entity": "offer", "titles": {"raw": "good21"}, "outlet": {"id": "221"}},
                {"entity": "offer", "titles": {"raw": "good32"}, "outlet": {"id": "232"}},
            ],
            preserve_order=True,
        )
        response = self.report.request_json(
            'place=geo&how=distance&page=2&numdoc=4&geo-location=37.15,55.15&show-book-now-only=1&geo-sort-gran=1&numdoc=20&hyperid=301&rids=0'
        )
        self.assertFragmentIn(
            response,
            [
                {"entity": "offer", "titles": {"raw": "good13"}, "outlet": {"id": "212"}},
                {"entity": "offer", "titles": {"raw": "good21"}, "outlet": {"id": "222"}},
                {"entity": "offer", "titles": {"raw": "good32"}, "outlet": {"id": "233"}},
                {"entity": "offer", "titles": {"raw": "good11"}, "outlet": {"id": "213"}},
            ],
            preserve_order=True,
        )
        response = self.report.request_json(
            'place=geo&how=distance&page=3&numdoc=4&geo-location=37.15,55.15&show-book-now-only=1&geo-sort-gran=1&numdoc=20&hyperid=301&rids=0'
        )
        self.assertFragmentIn(
            response,
            [
                {"entity": "offer", "titles": {"raw": "good11"}, "outlet": {"id": "214"}},
                {"entity": "offer", "titles": {"raw": "good11"}, "outlet": {"id": "215"}},
                {"entity": "offer", "titles": {"raw": "good11"}, "outlet": {"id": "216"}},
            ],
            preserve_order=True,
        )

    def test_geo_bounding_booknow(self):
        # Что тестируем: выдачу с учетом баундинга с дефолтным ранжированием и с ранжированием по удаленности
        # В выдаче должны остаться аутлеты 211 и 231 (см "карту")
        response = self.report.request_json(
            'place=geo&show-book-now-only=1&geo-location=37.15,55.15&geo_bounds_lb=37.0,55.0&geo_bounds_rt=37.2,55.2&hyperid=301&rids=0'
        )
        self.assertFragmentIn(response, {"search": {"total": 2, "shops": 2, "shopOutlets": 2}}, preserve_order=True)
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "titles": {"raw": "good13"},
                    "bundleCount": 3,
                    "bundled": {"modelId": 301, "outletId": 211, "count": 3},
                    "outlet": {"id": "211", "bundleCount": 2, "bundled": {"modelId": 301, "count": 2}},
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "good31"},
                    "bundleCount": 2,
                    "bundled": {"modelId": 301, "outletId": 231, "count": 2},
                    "outlet": {
                        "id": "231",
                        "bundleCount": 1,
                        "bundled": {"modelId": 301, "count": 1},
                    },
                },
            ],
            preserve_order=True,
        )

        response = self.report.request_json(
            'place=geo&show-book-now-only=1&how=distance&geo-location=37.15,55.15&geo_bounds_lb=37.0,55.0&geo_bounds_rt=37.2,55.2&hyperid=301&rids=0'
        )
        self.assertFragmentIn(response, {"search": {"total": 2, "shops": 2, "shopOutlets": 2}}, preserve_order=True)
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "titles": {"raw": "good31"},
                    "bundleCount": 2,
                    "bundled": {"modelId": 301, "outletId": 231, "count": 2},
                    "outlet": {
                        "id": "231",
                        "bundleCount": 1,
                        "bundled": {"modelId": 301, "count": 1},
                    },
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "good13"},
                    "bundleCount": 3,
                    "bundled": {"modelId": 301, "outletId": 211, "count": 3},
                    "outlet": {
                        "id": "211",
                        "bundleCount": 2,
                        "bundled": {"modelId": 301, "count": 2},
                    },
                },
            ],
            preserve_order=True,
        )

    def test_multiple_tiles_booknow(self):
        # Что тестируем: выдачу аутлетов в группе тайлов в BookNow выдаче
        response = self.report.request_json(
            'place=geo&show-book-now-only=1&tile=617,322&tile=618,322&tile=619,322&zoom=10&hyperid=301&show-outlet=tiles&rids=0'
        )
        self.assertFragmentIn(
            response,
            {
                "tiles": [
                    {"coord": {"x": 617, "y": 322}, "outlets": [{"id": "221"}]},
                    {"coord": {"x": 618, "y": 322}, "outlets": [{"id": "215"}, {"id": "222"}]},
                    {"coord": {"x": 619, "y": 322}, "outlets": []},
                ]
            },
            preserve_order=True,
        )

    def test_single_outlet_booknow(self):
        # Что тестируем: режим выдачи для одного аутлета, сразу с BookNow
        response = self.report.request_json(
            'place=geo&show-book-now-only=1&point_id=211&hyperid=301&rids=0&grhow=offer'
        )
        self.assertFragmentIn(response, {"search": {"total": 3, "shops": 1, "shopOutlets": 1}}, preserve_order=True)
        self.assertFragmentIn(
            response,
            [
                {"entity": "offer", "titles": {"raw": "good12"}, "outlet": {"id": "211"}},
                {"entity": "offer", "titles": {"raw": "good13"}, "outlet": {"id": "211"}},
                {"entity": "offer", "titles": {"raw": "good11"}, "outlet": {"id": "211"}},
            ],
            preserve_order=True,
        )

    def test_top5_geo(self):
        # Что тестируем: режим выдачи Топ5-гео, сразу с BookNow
        response = self.report.request_json(
            'place=geo&show-book-now-only=1&numdoc=5&geo-location=37.15,55.15&geo_bounds_lb=37.0,55.0&geo_bounds_rt=37.4,55.4&geo-sort-gran=1&hyperid=301&rids=0'
        )
        self.assertFragmentIn(response, {"search": {"total": 7, "shops": 2, "shopOutlets": 7}}, preserve_order=True)
        self.assertFragmentIn(
            response,
            [
                {"entity": "offer", "titles": {"raw": "good13"}, "outlet": {"id": "211"}},
                {"entity": "offer", "titles": {"raw": "good31"}, "outlet": {"id": "231"}},
                {"entity": "offer", "titles": {"raw": "good13"}, "outlet": {"id": "212"}},
                {"entity": "offer", "titles": {"raw": "good32"}, "outlet": {"id": "232"}},
                {"entity": "offer", "titles": {"raw": "good11"}, "outlet": {"id": "213"}},
            ],
            preserve_order=True,
        )

    def test_sorting_aprice(self):
        # Что тестируем: сортировку по возрастанию цены
        response = self.report.request_json(
            'place=geo&how=aprice&geo-location=37.15,55.15&geo-sort-gran=2&numdoc=20&hyperid=301&rids=0&pp=18'
        )
        self.assertFragmentIn(response, {"search": {"total": 15, "shops": 5, "shopOutlets": 15}}, preserve_order=True)
        self.assertFragmentIn(
            response,
            [
                {"entity": "offer", "titles": {"raw": "good14"}, "outlet": {"id": "211"}},
                {"entity": "offer", "titles": {"raw": "good14"}, "outlet": {"id": "214"}},
                {"entity": "offer", "titles": {"raw": "good22"}, "outlet": {"id": "221"}},
                {"entity": "offer", "titles": {"raw": "good22"}, "outlet": {"id": "222"}},
                {"entity": "offer", "titles": {"raw": "good33"}, "outlet": {"id": "231"}},
                {"entity": "offer", "titles": {"raw": "good33"}, "outlet": {"id": "232"}},
                {"entity": "offer", "titles": {"raw": "good41"}, "outlet": {"id": "241"}},
                {"entity": "offer", "titles": {"raw": "good_PostTerm_6"}, "outlet": {"id": "261"}},
                {"entity": "offer", "titles": {"raw": "good_PostTerm_6"}, "outlet": {"id": "262"}},
                {"entity": "offer", "titles": {"raw": "good14"}, "outlet": {"id": "212"}},
                {"entity": "offer", "titles": {"raw": "good14"}, "outlet": {"id": "213"}},
                {"entity": "offer", "titles": {"raw": "good22"}, "outlet": {"id": "223"}},
                {"entity": "offer", "titles": {"raw": "good33"}, "outlet": {"id": "233"}},
                {"entity": "offer", "titles": {"raw": "good14"}, "outlet": {"id": "215"}},
                {"entity": "offer", "titles": {"raw": "good14"}, "outlet": {"id": "216"}},
            ],
            preserve_order=True,
        )

    def test_sorting_aprice_booknow(self):
        # Что тестируем: сортировку по возрастанию цены в BookNow-выдаче
        response = self.report.request_json(
            'place=geo&how=aprice&show-book-now-only=1&geo-location=37.15,55.15&geo-sort-gran=2&numdoc=20&hyperid=301&rids=0&pp=18'
        )
        self.assertFragmentIn(response, {"search": {"total": 11, "shops": 3, "shopOutlets": 11}}, preserve_order=True)
        self.assertFragmentIn(
            response,
            [
                {"entity": "offer", "titles": {"raw": "good13"}, "outlet": {"id": "211"}},
                {"entity": "offer", "titles": {"raw": "good13"}, "outlet": {"id": "212"}},
                {"entity": "offer", "titles": {"raw": "good31"}, "outlet": {"id": "231"}},
                {"entity": "offer", "titles": {"raw": "good32"}, "outlet": {"id": "232"}},
                {"entity": "offer", "titles": {"raw": "good21"}, "outlet": {"id": "221"}},
                {"entity": "offer", "titles": {"raw": "good21"}, "outlet": {"id": "222"}},
                {"entity": "offer", "titles": {"raw": "good11"}, "outlet": {"id": "214"}},
                {"entity": "offer", "titles": {"raw": "good11"}, "outlet": {"id": "213"}},
                {"entity": "offer", "titles": {"raw": "good32"}, "outlet": {"id": "233"}},
                {"entity": "offer", "titles": {"raw": "good11"}, "outlet": {"id": "215"}},
                {"entity": "offer", "titles": {"raw": "good11"}, "outlet": {"id": "216"}},
            ],
            preserve_order=True,
        )

    def test_sorting_dprice(self):
        # Что тестируем: сортировку по убыванию цены
        response = self.report.request_json(
            'place=geo&how=dprice&geo-location=37.15,55.15&geo-sort-gran=2&numdoc=20&hyperid=301&rids=0&pp=18'
        )
        self.assertFragmentIn(response, {"search": {"total": 15, "shops": 5, "shopOutlets": 15}}, preserve_order=True)
        self.assertFragmentIn(
            response,
            [
                {"entity": "offer", "titles": {"raw": "good12"}, "outlet": {"id": "211"}},
                {"entity": "offer", "titles": {"raw": "good12"}, "outlet": {"id": "214"}},
                {"entity": "offer", "titles": {"raw": "good21"}, "outlet": {"id": "221"}},
                {"entity": "offer", "titles": {"raw": "good21"}, "outlet": {"id": "222"}},
                {"entity": "offer", "titles": {"raw": "good31"}, "outlet": {"id": "231"}},
                {"entity": "offer", "titles": {"raw": "good31"}, "outlet": {"id": "232"}},
                {"entity": "offer", "titles": {"raw": "good41"}, "outlet": {"id": "241"}},
                {"entity": "offer", "titles": {"raw": "good_PostTerm_6"}, "outlet": {"id": "261"}},
                {"entity": "offer", "titles": {"raw": "good_PostTerm_6"}, "outlet": {"id": "262"}},
                {"entity": "offer", "titles": {"raw": "good12"}, "outlet": {"id": "212"}},
                {"entity": "offer", "titles": {"raw": "good12"}, "outlet": {"id": "213"}},
                {"entity": "offer", "titles": {"raw": "good21"}, "outlet": {"id": "223"}},
                {"entity": "offer", "titles": {"raw": "good31"}, "outlet": {"id": "233"}},
                {"entity": "offer", "titles": {"raw": "good12"}, "outlet": {"id": "215"}},
                {"entity": "offer", "titles": {"raw": "good12"}, "outlet": {"id": "216"}},
            ],
            preserve_order=True,
        )

    def test_sorting_dprice_booknow(self):
        # Что тестируем: сортировку по убыванию цены для BookNow-выдачи
        response = self.report.request_json(
            'place=geo&how=dprice&show-book-now-only=1&geo-location=37.15,55.15&geo-sort-gran=2&numdoc=20&hyperid=301&rids=0&pp=18'
        )
        self.assertFragmentIn(response, {"search": {"total": 11, "shops": 3, "shopOutlets": 11}}, preserve_order=True)
        self.assertFragmentIn(
            response,
            [
                {"entity": "offer", "titles": {"raw": "good13"}, "outlet": {"id": "211"}},
                {"entity": "offer", "titles": {"raw": "good13"}, "outlet": {"id": "212"}},
                {"entity": "offer", "titles": {"raw": "good31"}, "outlet": {"id": "231"}},
                {"entity": "offer", "titles": {"raw": "good32"}, "outlet": {"id": "232"}},
                {"entity": "offer", "titles": {"raw": "good21"}, "outlet": {"id": "221"}},
                {"entity": "offer", "titles": {"raw": "good21"}, "outlet": {"id": "222"}},
                {"entity": "offer", "titles": {"raw": "good11"}, "outlet": {"id": "213"}},
                {"entity": "offer", "titles": {"raw": "good11"}, "outlet": {"id": "214"}},
                {"entity": "offer", "titles": {"raw": "good32"}, "outlet": {"id": "233"}},
                {"entity": "offer", "titles": {"raw": "good11"}, "outlet": {"id": "215"}},
                {"entity": "offer", "titles": {"raw": "good11"}, "outlet": {"id": "216"}},
            ],
            preserve_order=True,
        )

    def test_price_filter(self):
        # Что тестируем: фильтрацию по цене
        response = self.report.request_json('place=geo&mcpricefrom=95&mcpriceto=105&hyperid=301&rids=0&pp=18')

        # Проверяем, что фильтр работает - аутлета, в котором нет подходящего оффера, нет в выдаче
        self.assertFragmentIn(response, {"search": {"total": 12, "shops": 3, "shopOutlets": 12}}, preserve_order=True)

    def test_warranty_filter(self):
        # Что тестируем: фильтрацию по наличию гарантиии
        response = self.report.request_json('place=geo&manufacturer_warranty=1&hid=503&rids=0&pp=18')

        # Проверяем, что фильтр работает - на выдаче нужный оффер
        self.assertFragmentIn(response, {"search": {"total": 1, "shops": 1, "shopOutlets": 1}}, preserve_order=True)
        self.assertFragmentIn(response, {"titles": {"raw": "good52"}}, preserve_order=True)

    def test_qrfrom_filter(self):
        # Что тестируем: фильтрацию по рейтингу магазина
        response = self.report.request_json('place=geo&qrfrom=4&hid=503&rids=0&pp=18')

        # Проверяем, что фильтр работает - аутлета, в котором нет подходящего оффера, нет в выдаче
        self.assertFragmentIn(response, {"search": {"total": 1, "shops": 1, "shopOutlets": 1}}, preserve_order=True)

    def test_offer_shipping_filter(self):
        # Что тестируем: фильтрацию по типу доставки

        # Запрос &offer-shipping=store
        response = self.report.request_json('place=geo&offer-shipping=store&hid=503&rids=0&pp=18')

        # Проверяем, что фильтр работает - аутлетов типа pickup, нет в выдаче
        self.assertFragmentNotIn(response, {"type": "pickup"}, preserve_order=True)

        # Запрос &offer-shipping=pickup
        response = self.report.request_json('place=geo&offer-shipping=pickup&hid=503&rids=0&pp=18')

        # Проверяем, что фильтр работает - аутлетов типа store, нет в выдаче
        self.assertFragmentNotIn(response, {"type": "store"}, preserve_order=True)

        # Что тестируем: отображение в выдаче только постомат-аутлетов
        response = self.report.request_json(
            'place=geo&geo-location=37.15,55.15&geo_bounds_lb=37.0,55.0&geo_bounds_rt=37.2,55.2&hyperid=301&rids=0&offer-shipping=postomat'
        )
        self.assertFragmentIn(response, {"search": {"total": 1, "shops": 1, "shopOutlets": 1}}, preserve_order=True)
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "titles": {"raw": "good_PostTerm_6"},
                    "bundleCount": 1,
                    "bundled": {"modelId": 301, "outletId": 261, "count": 1},
                    "outlet": {
                        "id": "261",
                    },
                },
            ],
            preserve_order=True,
        )

        # Что тестируем: отображение в выдаче только и постомат и store  аутлетов (постомат-аутлеты выводятся при фильтре по pickup)
        response = self.report.request_json(
            'place=geo&geo-location=37.15,55.15&geo_bounds_lb=37.0,55.0&geo_bounds_rt=37.2,55.2&hyperid=301&rids=0&offer-shipping=pickup,store'
        )
        self.assertFragmentIn(response, {"search": {"total": 3, "shops": 3, "shopOutlets": 3}}, preserve_order=True)

        # Что тесируем: В тайлы попадают только постаматы
        response = self.report.request_json(
            'place=geo&tile=617,322&tile=617,323&tile=617,324&zoom=10&hyperid=301&show-outlet=tiles&rids=0&offer-shipping=postomat'
        )
        self.assertFragmentIn(
            response,
            {
                "tiles": [
                    {"coord": {"x": 617, "y": 322}, "outlets": []},
                    {"coord": {"x": 617, "y": 323}, "outlets": []},
                    {"coord": {"x": 617, "y": 324}, "outlets": [{"id": "261"}]},
                ]
            },
            preserve_order=True,
        )

        # Что тестируем: В  выдаче нет других аутлетов, только постоматы. (в т.ч. pickup - аутлеты отфильтровываются от postomat)
        response = self.report.request_json('place=geo&rids=0&offer-shipping=postomat&text=good')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "titles": {"raw": "good_PostTerm_6"},
                        "bundleCount": 1,
                        "bundled": {"outletId": 261, "count": 1},
                        "outlet": {
                            "id": "261",
                        },
                    },
                    {
                        "entity": "offer",
                        "titles": {"raw": "good_PostTerm_6"},
                        "bundleCount": 1,
                        "bundled": {"outletId": 262, "count": 1},
                        "outlet": {
                            "id": "262",
                        },
                    },
                ],
            },
            preserve_order=True,
        )

    def test_home_region_filter(self):
        # Что тестируем: фильтрацию по стране магазина
        response = self.report.request_json('place=geo&home_region_filter=10&hid=503&rids=0&pp=18')

        # Проверяем, что фильтр работает - аутлета магазина другой страны нет в выдаче
        self.assertFragmentIn(response, {"search": {"total": 4, "shops": 1, "shopOutlets": 4}}, preserve_order=True)
        self.assertFragmentNotIn(response, {"shop": {"id": 104}}, preserve_order=True)

        # Проверяем формат
        self.assertFragmentNotIn(
            response,
            {
                "filters": [
                    {
                        "id": "home_region",
                    }
                ]
            },
        )

    def test_fesh_filter(self):
        # Что тестируем: фильтрацию по магазину
        response = self.report.request_json('place=geo&fesh=104&hid=503&rids=0&pp=18')

        # Проверяем, что фильтр работает - аутлетов других магазинов нет в выдаче
        self.assertFragmentIn(response, {"search": {"total": 1, "shops": 1, "shopOutlets": 1}}, preserve_order=True)
        self.assertFragmentIn(response, {"shop": {"id": 104}}, preserve_order=True)

    def test_group_model_expansion(self):
        # Что тестируем: для групповой модели возвращаются модификации
        response = self.report.request_json('place=geo&hyperid=310&point_id=241&grhow=offer')
        self.assertEqual(3, response.count({"entity": "offer"}))
        self.assertFragmentIn(response, {"entity": "offer", "model": {"id": 310}}, preserve_order=True)
        self.assertFragmentIn(response, {"entity": "offer", "model": {"id": 311}}, preserve_order=True)
        self.assertFragmentIn(response, {"entity": "offer", "model": {"id": 312}}, preserve_order=True)

    def test_group_model_expansion_multiple_hyperid(self):
        # Что тестируем: для групповой модели не возвращаются модификации если в запросе несколько hyperid
        response = self.report.request_json('place=geo&hyperid=310&hyperid=311&point_id=241&grhow=offer')
        self.assertEqual(2, response.count({"entity": "offer"}))
        self.assertFragmentIn(response, {"entity": "offer", "model": {"id": 310}}, preserve_order=True)
        self.assertFragmentIn(response, {"entity": "offer", "model": {"id": 311}}, preserve_order=True)

    def test_group_model_expansion_not_a_group_model(self):
        # Что тестируем: hyperid не групповой модели - возвращается только она
        response = self.report.request_json('place=geo&hyperid=311&point_id=241')
        self.assertEqual(1, response.count({"entity": "offer"}))
        self.assertFragmentIn(response, {"entity": "offer", "model": {"id": 311}}, preserve_order=True)

    def test_group_by(self):
        # Что тестируем: наличие groupBy на выдаче
        response = self.report.request_json('place=geo&hyperid=301&grhow=offer')
        self.assertFragmentIn(response, {"search": {"groupBy": "outlet"}})

        response = self.report.request_json('place=geo&hyperid=301&point_id=211&grhow=offer')
        self.assertFragmentIn(response, {"search": {"groupBy": "offer"}})

    def test_default_sorting_for_guru(self):
        # Что тестируем: сортировка по умолчанию в гуру-категории - SF_CPM
        response = self.report.request_json('place=geo&hid=504&rids=0&debug=1')
        self.assertFragmentIn(response, {"how": [{"args": Wildcard("*\nsorting_by: 1\n*")}]})

    def test_empty_results_booknow(self):
        # Что тестируем: выдачу с учетом баундинга пустом квадрате в BookNow-выдаче
        response = self.report.request_json(
            'place=geo&show-book-now-only=1&geo-location=37.15,55.65&geo_bounds_lb=37.0,55.6&geo_bounds_rt=37.2,55.8&hyperid=301&rids=0'
        )
        self.assertFragmentIn(response, {"search": {"total": 0, "shops": 0, "shopOutlets": 0}})
        self.assertFragmentNotIn(response, {"entity": "offer"})

    def test_booknow_with_post_terminals(self):
        # Что тестируем booknow и постаматы отображаются вместе на гео
        response = self.report.request_json('place=geo&hyperid=701&rids=0&show-urls=geo,geoOutlet,geoPointInfo')

        self.assertFragmentIn(response, {"search": {"total": 3, "shops": 1, "shopOutlets": 3}})
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "outlet": {
                            "id": "263",
                        }
                    },
                    {
                        "outlet": {
                            "id": "261",
                        }
                    },
                    {
                        "outlet": {
                            "id": "262",
                        }
                    },
                ]
            },
            preserve_order=True,
        )

        # Что тестируем при show-book-now-only=1 отображаем только один аутлет
        response = self.report.request_json(
            'place=geo&hyperid=701&rids=0&show-urls=geo,geoOutlet,geoPointInfo&show-book-now-only=1'
        )

        self.assertFragmentIn(response, {"search": {"total": 1, "shops": 1, "shopOutlets": 1}})
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "outlet": {
                            "id": "263",
                        }
                    }
                ]
            },
            preserve_order=True,
        )

    def test_empty_request_error(self):
        # Что тестируем: выдачу ошибки EMPTY REQUEST в JSON-формате
        response = self.report.request_json('place=geo&rids=0')
        self.assertFragmentIn(response, {"error": {"code": "EMPTY_REQUEST", "message": "Request is empty"}})

    def test_outlet_phones(self):
        # Что тестируем: выдачу telephone в outlet
        response = self.report.request_json('place=geo&fesh=105&rids=0')
        self.assertFragmentIn(
            response,
            {
                "outlet": {
                    "name": "OUTLET-105-251",
                    "telephones": [
                        {
                            "entity": "telephone",
                            "countryCode": "+7",
                            "cityCode": "495",
                            "telephoneNumber": "123-45-67",
                            "extensionNumber": "89",
                        },
                        {
                            "entity": "telephone",
                            "countryCode": "+7",
                            "cityCode": "495",
                            "telephoneNumber": "987-65-43",
                            "extensionNumber": "21",
                        },
                        {
                            "entity": "telephone",
                            "countryCode": "+7",
                            "cityCode": "812",
                            "telephoneNumber": " 765-43-21",
                            "extensionNumber": "",
                        },
                        {
                            "entity": "telephone",
                            "countryCode": "+7",
                            "cityCode": "495",
                            "telephoneNumber": "765-43-21",
                            "extensionNumber": "",
                        },
                        {
                            "entity": "telephone",
                            "countryCode": "+7",
                            "cityCode": "812",
                            "telephoneNumber": "305 26 71",
                            "extensionNumber": "",
                        },
                        {
                            "entity": "telephone",
                            "countryCode": "+7",
                            "cityCode": "123",
                            "telephoneNumber": "4561234",
                            "extensionNumber": "",
                        },
                        {
                            "entity": "telephone",
                            "countryCode": "8",
                            "cityCode": "7655",
                            "telephoneNumber": " 4-72110",
                            "extensionNumber": "",
                        },
                        {
                            "entity": "telephone",
                            "countryCode": "",
                            "cityCode": "495",
                            "telephoneNumber": " 355-43-21",
                            "extensionNumber": "",
                        },
                        {
                            "entity": "telephone",
                            "countryCode": "",
                            "cityCode": "",
                            "telephoneNumber": "355-43-21",
                            "extensionNumber": "",
                        },
                        {
                            "entity": "telephone",
                            "countryCode": "",
                            "cityCode": "",
                            "telephoneNumber": "1-23-456",
                            "extensionNumber": "",
                        },
                        {
                            "entity": "telephone",
                            "countryCode": "",
                            "cityCode": "",
                            "telephoneNumber": "987654",
                            "extensionNumber": "",
                        },
                    ],
                }
            },
            preserve_order=True,
        )

    def test_show_cpa_disabled(self):
        response = self.report.request_json('place=geo&text=good52')
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "titles": {
                    "raw": "good52",
                },
                "description": "",
                "cpa": NoKey("cpa"),
            },
        )

    def test_geo_flag(self):
        # Что тестируем: неявный флаг &geo=1 в place=geo
        response = self.report.request_json('place=geo&hid=501&rids=1&debug=1')
        self.assertFragmentIn(response, {"how": [{"args": Wildcard("*\ngeo: true\n*")}]})

    def test_bad_rs(self):
        # non base64 rs
        response = self.report.request_json('place=geo' '&text=good52' '&owner=offercard' '&rs=B@D_RS')
        self.assertFragmentIn(response, {"entity": "offer"})

        # bad zlib inside of base64 (incorrect header check)
        response = self.report.request_json(
            'place=geo' '&text=good52' '&owner=offercard' '&rs=ejzjybcsmty0mjftmdaxtzy2mdawbllmdcqylrgnaum-bii%2c'
        )
        self.assertFragmentIn(response, {"entity": "offer"})
        self.error_log.expect(code=3630, message=Contains('can not decode report state from')).times(4)

    def test_urls_for_offercard_without_cpa(self):
        # place=geo&text=good52&owner=offercard&show-urls=geoOutlet,phone,showPhone,cpa&pp=18
        _ = self.report.request_json(
            'place=geo' '&text=good53' '&owner=offercard' '&show-urls=encrypted,cpa,phone,showPhone' '&pp=18'
        )
        # 4 offers will be found
        self.show_log.expect(pp=18, pp_oi=2, url_type=UrlType.EXTERNAL).times(4)
        self.show_log.expect(pp=18, pp_oi=5, url_type=UrlType.CPA).times(0)
        self.show_log.expect(pp=18, pp_oi=3, url_type=UrlType.SHOW_PHONE).times(4)
        self.show_log.expect(pp=18, pp_oi=3, url_type=UrlType.PHONE).times(4)
        self.click_log.expect(clicktype=ClickType.EXTERNAL, url_type=UrlType.EXTERNAL, pp=18, pp_oi=2).times(4)
        self.click_log.expect(clicktype=ClickType.CPA, url_type=UrlType.CPA, pp=18, pp_oi=5).times(0)
        self.click_log.expect(clicktype=ClickType.PHONE, url_type=UrlType.PHONE, pp=18, pp_oi=3).times(4)
        self.click_log.expect(clicktype=ClickType.SHOW_PHONE, url_type=UrlType.SHOW_PHONE, pp=18, pp_oi=3).times(4)

    def test_invalid_glfilter_log_message(self):
        # Что тестируем: в логе должно быть только одно сообщение про невалидный GL фильтр
        self.report.request_json('place=geo&hid=501&rids=0&show-urls=geo,geoOutlet,geoPointInfo&glfilter=123:456')
        self.error_log.expect('Error in glfilters syntax:').once()

    def test_model_filtering(self):
        # Что тестируем: фильтрацию моделей на базовых поисках
        response = self.report.request_json('place=geo&hid=601&point_id=251&debug=1')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "results": [
                        {
                            "entity": "offer",
                            "outlet": {
                                "id": "251",
                            },
                        }
                    ],
                }
            },
        )

    def test_missing_pp(self):
        response = self.report.request_json(
            'place=geo&hid=501&rids=0&show-urls=geo,geoOutlet,geoPointInfo&ip=127.0.0.1',
            strict=False,
            add_defaults=DefaultFlags.BS_FORMAT,
        )
        self.error_log.expect('Some client has not set PP value. Find and punish him violently').once()
        self.assertEqual(500, response.code)

    def test_grhow_one_shop_and_only_one_offer_is_shown(self):
        # What we check: that parameter grhow=shop leads to grouping by shop with only one offer per shop
        response = self.report.request_json('place=geo&point_id=410&text=pepyaka-postomatish&grhow=shop')
        self.assertEqual(1, response.count({"entity": "offer"}))
        self.assertFragmentIn(response, {"entity": "offer", "shop": {"id": 110}})

    def test_nosearchresults(self):
        # Что тестируем: отсуствие результатов на выдаче и записей в show log при nosearchresults=1
        response = self.report.request_json(
            'place=geo&nosearchresults=1&show-outlet=offers,tiles&show-urls=geo,geoOutlet,geoPointInfo&geo-location=37.15,55.15&geo_bounds_lb=37.0,55.0&geo_bounds_rt=37.4,55.4&tile617,322&tile=617,323&tile=617,324&zoom=10&hyperid=301&rids=0'  # noqa
        )
        self.assertFragmentIn(response, {"search": {"total": 9, "shops": 3, "shopOutlets": 9}})
        self.assertFragmentNotIn(response, {"results": []})
        self.assertFragmentNotIn(response, {"tiles": []})
        self.show_log.expect(url=Wildcard('*')).never()

    def test_geo_old_for_checkouter(self):
        """Отображение в т.ч. инпост-аутлетов в xml-формате для чекаутера
        Проверяем что delivery-for-checkout=1 игнорируется ( MARKETOUT-12509 )"""
        expected = {
            "search": {
                "total": 3,
                "salesDetected": False,
                "shopOutlets": 3,
                "shops": 3,
                "results": [
                    {"entity": "offer", "outlet": {"id": "231"}},
                    {"entity": "offer", "outlet": {"id": "211"}},
                    {"entity": "offer", "outlet": {"id": "261"}},
                ],
            }
        }
        response = self.report.request_json(
            'place=geo&geo=1&geo-location=37.15,55.15&geo_bounds_lb=37.0,55.0&geo_bounds_rt=37.2,55.2&hyperid=301&rids=0&delivery-for-checkout=1&grhow=shop'
        )
        self.assertFragmentIn(response, expected)

        response = self.report.request_json(
            'place=geo&geo=1&geo-location=37.15,55.15&geo_bounds_lb=37.0,55.0&geo_bounds_rt=37.2,55.2&hyperid=301&rids=0&grhow=shop'
        )
        self.assertFragmentIn(response, expected)

    def test_sort_by_distance_post_terminals(self):
        """Отображение постаматов при сортировке по distance. Не зависит от &touch"""
        for add_param in ['', '&touch=1']:
            response = self.report.request_json(
                'place=geo&how=distance&geo-location=37.12,55.03&geo_bounds_lb=37.0,55.0&geo_bounds_rt=37.2,55.2&hyperid=301&rids=1&regset=1'
                + add_param
            )
            self.assertFragmentIn(response, {"search": {"total": 3, "shops": 3, "shopOutlets": 3}}, preserve_order=True)
            self.assertFragmentIn(
                response,
                [
                    {
                        "entity": "offer",
                        "titles": {"raw": "good_PostTerm_6"},
                        "outlet": {
                            "id": "261",
                        },
                    },
                    {
                        "entity": "offer",
                        "titles": {"raw": "good12"},
                        "bundleCount": 4,
                        "bundled": {"modelId": 301, "outletId": 211, "count": 4},
                        "outlet": {
                            "id": "211",
                            "bundleCount": 6,
                            "bundled": {"modelId": 301, "count": 6},
                        },
                    },
                    {
                        "entity": "offer",
                        "titles": {"raw": "good31"},
                        "bundleCount": 3,
                        "bundled": {"modelId": 301, "outletId": 231, "count": 3},
                        "outlet": {"id": "231", "bundleCount": 3, "bundled": {"modelId": 301, "count": 3}},
                    },
                ],
                preserve_order=True,
            )

    def test_product_type(self):
        _ = self.report.request_json('place=geo&show-outlet=offers&hyperid=301&rids=0')
        self.access_log.expect(product_type='MODEL')

        _ = self.report.request_json('place=geo&show-outlet=offers&hyperid=1000000007&rids=0')
        self.access_log.expect(product_type='VCLUSTER')

        _ = self.report.request_json(
            'place=geo&nosearchresults=1&show-outlet=offers,tiles&show-urls=geo,geoOutlet,geoPointInfo&geo-location=37.15,55.15&geo_bounds_lb=37.0,55.0&geo_bounds_rt=37.4,55.4&tile617,322&tile=617,323&tile=617,324&zoom=10&hyperid=301&rids=0'  # noqa
        )
        self.access_log.expect(product_type='NONE')

    @classmethod
    def prepare_model_search_shops(cls):
        cls.index.shops += [
            Shop(fesh=1101, priority_region=1),
            Shop(fesh=1102, priority_region=1),
            Shop(fesh=1103, priority_region=1),
            Shop(fesh=1104, priority_region=1),
            Shop(fesh=1105, priority_region=1),
        ]

        # Аутлеты на "карте"
        # Числа в скобках - координаты тайлов при zoom = 10
        #           37.0(617)     37.2(617)       37.4(618)      37.6(618)       37.8(619)
        # 55.8(321) |--------------|---------------|--------------|---------------
        #           |              |               |              |
        #           |              |               |              |
        #           |              |               |              |
        #           |              |               |              |
        # 55.6(322) |--------------|---------------|--------------|---------------
        #           |              |               |              |
        #           |     *(1221)  |    *(1222)    |              |
        #           |              |               |*Home         |
        #           |              |               |*(1241)       |
        # 55.4(323) |--------------|---------------|--------------|---------------
        #           |              |               |              |
        #           |     *(1232)  |      *(1233)  |              |      *(1251)
        #           |    *(1212)   |    *(1213)    |              |
        #           |              |               |              |
        # 55.2(324) |--------------|---------------|--------------|---------------
        #           |      *We     |               |              |
        #           |     *(1231)  |     *Work     |              |
        #           |              |    *(1214)    |              |
        #           |  *(1211)     |               |              |
        # 55.0(325) |--------------|---------------|--------------|---------------

        cls.index.outlets += [
            Outlet(
                point_id=1211,
                fesh=1101,
                region=1,
                gps_coord=GpsCoord(37.1, 55.1),
                point_type=Outlet.FOR_STORE,
                delivery_option=OutletDeliveryOption(price=100),
            ),
            Outlet(
                point_id=1212,
                fesh=1101,
                region=1,
                gps_coord=GpsCoord(37.1, 55.3),
                delivery_option=OutletDeliveryOption(price=200),
            ),
            Outlet(
                point_id=1213,
                fesh=1101,
                region=1,
                gps_coord=GpsCoord(37.3, 55.3),
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(price=300),
            ),
            Outlet(
                point_id=1214,
                fesh=1101,
                region=1,
                gps_coord=GpsCoord(37.3, 55.1),
                point_type=Outlet.FOR_STORE,
                delivery_option=OutletDeliveryOption(price=400),
            ),
            Outlet(point_id=1221, fesh=1102, region=1, gps_coord=GpsCoord(37.1, 55.5), point_type=Outlet.FOR_STORE),
            Outlet(point_id=1222, fesh=1102, region=1, gps_coord=GpsCoord(37.3, 55.5), point_type=Outlet.FOR_PICKUP),
            Outlet(point_id=1231, fesh=1103, region=1, gps_coord=GpsCoord(37.12, 55.12), point_type=Outlet.FOR_STORE),
            Outlet(point_id=1232, fesh=1103, region=1, gps_coord=GpsCoord(37.12, 55.32), point_type=Outlet.FOR_PICKUP),
            Outlet(point_id=1233, fesh=1103, region=1, gps_coord=GpsCoord(37.32, 55.32)),
            Outlet(point_id=1241, fesh=1104, region=1, gps_coord=GpsCoord(37.42, 55.42)),
            # Аутлеты вне зоны видимости
            Outlet(point_id=1251, fesh=1105, region=1, gps_coord=GpsCoord(37.7, 55.3)),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=7001,
                fesh=1101,
                carriers=[99],
                options=[
                    PickupOption(outlet_id=1211, price=100),
                    PickupOption(outlet_id=1212, price=200),
                    PickupOption(outlet_id=1213, price=300),
                    PickupOption(outlet_id=1214, price=400),
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=7002,
                fesh=1102,
                carriers=[99],
                options=[PickupOption(outlet_id=1221, price=0), PickupOption(outlet_id=1222, price=0)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=7003,
                fesh=1103,
                carriers=[99],
                options=[
                    PickupOption(outlet_id=1231, price=0),
                    PickupOption(outlet_id=1232, price=0),
                    PickupOption(outlet_id=1233, price=0),
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=7004,
                fesh=1104,
                carriers=[99],
                options=[PickupOption(outlet_id=1241, price=0)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=7005,
                fesh=1105,
                carriers=[99],
                options=[PickupOption(outlet_id=1251, price=0)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

    @classmethod
    def prepare_attraction_points(cls):
        '''
        Подготавливаем данные для тестирования точек притяжения (адрес жительства и места работы)
        Есть три точки:
         * Дом
         * Работа
         * Работа, находящаяся далеко за пределами экрана
        Есть 4 пользователя:
         * Без адресов (10) - результат должен совпадать с тем, как-будто пользователя нет
         * С одним адресом (11) - первыми отображаются точки близкие к этому адресу, потом к текущим координатам, потом все остальные
         * С двумя адресами (12) - первыми отображаются точки близкие к домашнему адресу, потом к работе, потом к текущим и все остальные
         * С далеким местом работы (13) - не должно быть точек притяжения
        '''
        home_address = DataSyncYandexUserAddress(address_id='home', gps_coord=HOME_GPS_COORD)
        work_address = DataSyncYandexUserAddress(address_id='work', gps_coord=WORK_GPS_COORD)
        far_address = DataSyncYandexUserAddress(address_id='work', gps_coord=FAR_GPS_COORD)
        cls.datasync.on_request_yandex_user_address(10).respond([])
        cls.datasync.on_request_yandex_user_address(11).respond([home_address])
        cls.datasync.on_request_yandex_user_address(12).respond([work_address, home_address])
        cls.datasync.on_request_yandex_user_address(13).respond([far_address])

    @classmethod
    def prepare_search(cls):
        cls.index.cpa_categories += [
            CpaCategory(hid=3001, regions=[1, 213], cpa_type=CpaCategoryType.CPA_NON_GURU),
            CpaCategory(hid=3002, regions=[1, 213], cpa_type=CpaCategoryType.CPC_AND_CPA),
        ]
        # CPA category. Offers are sorted as they should appear on search.
        cls.index.offers += [
            Offer(hid=3001, title='kayak 1_from_3001', fesh=1101, ts=30011, pickup_buckets=[7001]),
            Offer(hid=3001, title='kayak 2_from_3001', fesh=1105, ts=30012, pickup_buckets=[7005]),  # Out of sight
            Offer(hid=3001, title='kayak 3_from_3001', fesh=1102, ts=30013, pickup_buckets=[7002]),
            Offer(hid=3001, title='4_from_3001', bid=200, fesh=1103, ts=30014, pickup_buckets=[7003]),
            Offer(hid=3001, title='5_from_3001', bid=1000, fesh=1104, ts=30015, pickup_buckets=[7004]),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 30011).respond(0.5)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 30012).respond(0.5)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 30013).respond(0.52)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 30014).respond(0.48)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 30015).respond(0.5)
        # CPC category. Offers are sorted as they should appear on search.
        cls.index.offers += [
            Offer(hid=3002, title='1_from_3002', bid=50, fesh=1101, ts=30021, pickup_buckets=[7001]),
            Offer(hid=3002, title='2_from_3002', bid=40, fesh=1105, ts=30022, pickup_buckets=[7005]),  # Out of sight
            Offer(hid=3002, title='3_from_3002', bid=20, fesh=1102, ts=30023, pickup_buckets=[7002]),
            Offer(hid=3002, title='kayak 4_from_3002', bid=30, fesh=1103, ts=30024, pickup_buckets=[7003]),
            Offer(hid=3002, title='kayak 5_from_3002', bid=10, fesh=1104, ts=30025, pickup_buckets=[7004]),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 30021).respond(0.5)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 30022).respond(0.5)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 30023).respond(0.52)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 30024).respond(0.48)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 30025).respond(0.5)

    def test_geo_text_search_in_param_how(self):
        '''Что тестируем: сортировку как на поиске'''
        response = self.report.request_json(
            'place=geo&how=search&geo-location=37.15,55.15&geo_bounds_lb=37.0,55.0&geo_bounds_rt=37.6,55.6'
            '&numdoc=10&text=kayak&rids=1&regset=1'
        )
        self.assertFragmentIn(response, {"search": {"total": 10, "shops": 4, "shopOutlets": 10}})
        self.assertFragmentIn(
            response,
            [
                {"entity": "offer", "titles": {"raw": "kayak 3_from_3001"}},
                {"entity": "offer", "titles": {"raw": "kayak 5_from_3002"}},
                {"entity": "offer", "titles": {"raw": "kayak 1_from_3001"}},
                {"entity": "offer", "titles": {"raw": "kayak 4_from_3002"}},
                {"entity": "offer", "titles": {"raw": "kayak 3_from_3001"}},
                {"entity": "offer", "titles": {"raw": "kayak 1_from_3001"}},
                {"entity": "offer", "titles": {"raw": "kayak 4_from_3002"}},
                {"entity": "offer", "titles": {"raw": "kayak 1_from_3001"}},
                {"entity": "offer", "titles": {"raw": "kayak 4_from_3002"}},
                {"entity": "offer", "titles": {"raw": "kayak 1_from_3001"}},
            ],
            preserve_order=True,
            allow_different_len=False,
        )

    def test_geo_text_search_autobroker_head_in_param_how(self):
        '''Что тестируем: автоброкер как на поиске, для первого цикла по офферам'''
        _ = self.report.request_json(
            'place=geo&how=search&geo-location=37.15,55.15&geo_bounds_lb=37.0,55.0&geo_bounds_rt=37.6,55.6'
            '&numdoc=4&text=kayak&rids=1&regset=1&show-urls=external,cpa&show-geo-cpa=1'
        )
        self.click_log.expect(ClickType.EXTERNAL, shop_id=1102, cb=1, cp=1)
        self.click_log.expect(ClickType.EXTERNAL, shop_id=1101, cb=1, cp=1)
        self.click_log.expect(ClickType.EXTERNAL, shop_id=1104, cb=1, cp=1)
        self.click_log.expect(ClickType.EXTERNAL, shop_id=1103, cb=1, cp=1)

    def test_geo_text_search_autobroker_tail_in_param_how(self):
        '''Что тестируем: автоброкер как на поиске, для второго цикла по офферам'''
        _ = self.report.request_json(
            'place=geo&how=search&geo-location=37.15,55.15&geo_bounds_lb=37.0,55.0&geo_bounds_rt=37.6,55.6'
            '&numdoc=4&page=2&text=kayak&rids=1&regset=1&show-urls=external,cpa&show-geo-cpa=1'
        )
        # 1ый оффер подпертый 3им, оффер из CPA категории

        # 3ий оффер подпертый 4ым, оффер из CPA категории
        self.click_log.expect(ClickType.EXTERNAL, shop_id=1102, cb=1, cp=1)

        # 4ый оффер не подпертый, оффер из CPC категории
        self.click_log.expect(ClickType.EXTERNAL, shop_id=1103, cb=1, cp=1)

        # 1ый оффер подпертый 4ым, оффер из CPA категории
        self.click_log.expect(ClickType.EXTERNAL, shop_id=1101, cb=1, cp=1).times(2)

    def test_geo_category_search_cpc_autobroker_head_in_param_how(self):
        '''Что тестируем: автоброкер для категорийной выдачи, для первого цикла по офферам, категория CPC'''
        _ = self.report.request_json(
            'place=geo&how=search&geo-location=37.15,55.15&geo_bounds_lb=37.0,55.0&geo_bounds_rt=37.6,55.6'
            '&numdoc=4&hid=3002&rids=1&regset=1&show-urls=external,cpa&show-geo-cpa=1'
            '&rearr-factors=market_use_books_pessimization=1'
        )
        # 1ый оффер подпертый 3им
        self.click_log.expect(ClickType.EXTERNAL, shop_id=1101, cb=1, cp=1)

        # 3ий оффер подпертый 4ым
        self.click_log.expect(ClickType.EXTERNAL, shop_id=1102, cb=1, cp=1)

        # 4ый оффер подпертый 5ым
        self.click_log.expect(ClickType.EXTERNAL, shop_id=1103, cb=1, cp=1)

        # 5ый оффер, не подпертый
        self.click_log.expect(ClickType.EXTERNAL, shop_id=1104, cb=1, cp=1)

    def test_geo_category_search_cpc_autobroker_tail_in_param_how(self):
        '''Что тестируем: автоброкер для категорийной выдачи, для второго цикла по офферам, категория CPC'''
        _ = self.report.request_json(
            'place=geo&how=search&geo-location=37.15,55.15&geo_bounds_lb=37.0,55.0&geo_bounds_rt=37.6,55.6'
            '&numdoc=4&page=2&hid=3002&rids=1&regset=1&show-urls=external,cpa&show-geo-cpa=1'
            '&rearr-factors=market_use_books_pessimization=1'
        )
        # 1ый оффер подпертый 3им
        self.click_log.expect(ClickType.EXTERNAL, shop_id=1101, cb=1, cp=1)

        # 3ий оффер подпертый 4ым
        self.click_log.expect(ClickType.EXTERNAL, shop_id=1102, cb=1, cp=1)

        # 4ый оффер не подпертый
        self.click_log.expect(ClickType.EXTERNAL, shop_id=1103, cb=1, cp=1)

        # 1ый оффер подпертый 4ым
        self.click_log.expect(ClickType.EXTERNAL, shop_id=1101, cb=1, cp=1)

    def test_geo_search_outlet_ranking_in_param_how(self):
        '''Что тестируем: сортировку аутлетов по растоянию на поиске'''
        response = self.report.request_json(
            'place=geo&how=search&geo-location=37.15,55.15&geo_bounds_lb=37.0,55.0&geo_bounds_rt=37.6,55.6'
            '&numdoc=10&text=kayak&rids=1&regset=1&fesh=1101'
        )
        self.assertFragmentIn(
            response,
            [
                {"entity": "offer", "outlet": {"id": "1211"}},
                {"entity": "offer", "outlet": {"id": "1214"}},
                {"entity": "offer", "outlet": {"id": "1212"}},
                {"entity": "offer", "outlet": {"id": "1213"}},
            ],
            preserve_order=True,
            allow_different_len=False,
        )

    def test_geo_text_search(self):
        '''Что тестируем: сортировку как на поиске'''
        response = self.report.request_json(
            'place=geo&default-how-on-geo=search&geo-location=37.15,55.15&geo_bounds_lb=37.0,55.0&geo_bounds_rt=37.6,55.6'
            '&numdoc=10&text=kayak&rids=1&regset=1'
        )
        self.assertFragmentIn(response, {"search": {"total": 10, "shops": 4, "shopOutlets": 10}})
        self.assertFragmentIn(
            response,
            [
                {"entity": "offer", "titles": {"raw": "kayak 3_from_3001"}},
                {"entity": "offer", "titles": {"raw": "kayak 5_from_3002"}},
                {"entity": "offer", "titles": {"raw": "kayak 1_from_3001"}},
                {"entity": "offer", "titles": {"raw": "kayak 4_from_3002"}},
                {"entity": "offer", "titles": {"raw": "kayak 3_from_3001"}},
                {"entity": "offer", "titles": {"raw": "kayak 1_from_3001"}},
                {"entity": "offer", "titles": {"raw": "kayak 4_from_3002"}},
                {"entity": "offer", "titles": {"raw": "kayak 1_from_3001"}},
                {"entity": "offer", "titles": {"raw": "kayak 4_from_3002"}},
                {"entity": "offer", "titles": {"raw": "kayak 1_from_3001"}},
            ],
            preserve_order=True,
            allow_different_len=False,
        )

    def test_geo_text_search_autobroker_head(self):
        '''Что тестируем: автоброкер как на поиске, для первого цикла по офферам'''
        _ = self.report.request_json(
            'place=geo&default-how-on-geo=search&geo-location=37.15,55.15&geo_bounds_lb=37.0,55.0&geo_bounds_rt=37.6,55.6'
            '&numdoc=4&text=kayak&rids=1&regset=1&show-urls=external,cpa&show-geo-cpa=1'
        )
        self.click_log.expect(ClickType.EXTERNAL, shop_id=1102, cb=1, cp=1)
        self.click_log.expect(ClickType.EXTERNAL, shop_id=1101, cb=1, cp=1)
        self.click_log.expect(ClickType.EXTERNAL, shop_id=1104, cb=1, cp=1)
        self.click_log.expect(ClickType.EXTERNAL, shop_id=1103, cb=1, cp=1)

    def test_geo_text_search_autobroker_tail(self):
        '''Что тестируем: автоброкер как на поиске, для второго цикла по офферам'''
        _ = self.report.request_json(
            'place=geo&default-how-on-geo=search&geo-location=37.15,55.15&geo_bounds_lb=37.0,55.0&geo_bounds_rt=37.6,55.6'
            '&numdoc=4&page=2&text=kayak&rids=1&regset=1&show-urls=external,cpa&show-geo-cpa=1'
        )
        # 1ый оффер подпертый 3им, оффер из CPA категории

        # 3ий оффер подпертый 4ым, оффер из CPA категории
        self.click_log.expect(ClickType.EXTERNAL, shop_id=1102, cb=1, cp=1)

        # 4ый оффер не подпертый, оффер из CPC категории
        self.click_log.expect(ClickType.EXTERNAL, shop_id=1103, cb=1, cp=1)

        # 1ый оффер подпертый 4ым, оффер из CPA категории
        self.click_log.expect(ClickType.EXTERNAL, shop_id=1101, cb=1, cp=1).times(2)

    def test_geo_category_search_cpc_autobroker_head(self):
        '''Что тестируем: автоброкер для категорийной выдачи, для первого цикла по офферам, категория CPC'''
        _ = self.report.request_json(
            'place=geo&default-how-on-geo=search&geo-location=37.15,55.15&geo_bounds_lb=37.0,55.0&geo_bounds_rt=37.6,55.6'
            '&numdoc=4&hid=3002&rids=1&regset=1&show-urls=external,cpa&show-geo-cpa=1'
            '&rearr-factors=market_use_books_pessimization=1'
        )
        # 1ый оффер подпертый 3им
        self.click_log.expect(ClickType.EXTERNAL, shop_id=1101, cb=1, cp=1)

        # 3ий оффер подпертый 4ым
        self.click_log.expect(ClickType.EXTERNAL, shop_id=1102, cb=1, cp=1)

        # 4ый оффер подпертый 5ым
        self.click_log.expect(ClickType.EXTERNAL, shop_id=1103, cb=1, cp=1)

        # 5ый оффер, не подпертый
        self.click_log.expect(ClickType.EXTERNAL, shop_id=1104, cb=1, cp=1)

    def test_geo_category_search_cpc_autobroker_tail(self):
        '''Что тестируем: автоброкер для категорийной выдачи, для второго цикла по офферам, категория CPC'''
        _ = self.report.request_json(
            'place=geo&default-how-on-geo=search&geo-location=37.15,55.15&geo_bounds_lb=37.0,55.0&geo_bounds_rt=37.6,55.6'
            '&numdoc=4&page=2&hid=3002&rids=1&regset=1&show-urls=external,cpa&show-geo-cpa=1'
            '&rearr-factors=market_use_books_pessimization=1'
        )
        # 1ый оффер подпертый 3им
        self.click_log.expect(ClickType.EXTERNAL, shop_id=1101, cb=1, cp=1)

        # 3ий оффер подпертый 4ым
        self.click_log.expect(ClickType.EXTERNAL, shop_id=1102, cb=1, cp=1)

        # 4ый оффер не подпертый
        self.click_log.expect(ClickType.EXTERNAL, shop_id=1103, cb=1, cp=1)

        # 1ый оффер подпертый 4ым
        self.click_log.expect(ClickType.EXTERNAL, shop_id=1101, cb=1, cp=1)

    def test_geo_search_outlet_ranking(self):
        '''Что тестируем: сортировку аутлетов по растоянию на поиске'''
        response = self.report.request_json(
            'place=geo&default-how-on-geo=search&geo-location=37.15,55.15&geo_bounds_lb=37.0,55.0&geo_bounds_rt=37.6,55.6'
            '&numdoc=10&text=kayak&rids=1&regset=1&fesh=1101'
        )
        self.assertFragmentIn(
            response,
            [
                {"entity": "offer", "outlet": {"id": "1211"}},
                {"entity": "offer", "outlet": {"id": "1214"}},
                {"entity": "offer", "outlet": {"id": "1212"}},
                {"entity": "offer", "outlet": {"id": "1213"}},
            ],
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_clicks_for_fixtariff_shop_in_geo(cls):
        cls.index.hypertree += [HyperCategory(hid=700, output_type=HyperCategoryType.GURU)]
        cls.index.models += [
            Model(hyperid=3010, title='model for fix tariff', hid=700),
        ]
        cls.index.shops += [
            Shop(fesh=702, home_region=225, tariff="FIX", online=False),
        ]
        cls.index.outlets += [
            Outlet(point_id=702, fesh=702, region=213, point_type=Outlet.FOR_PICKUP),
        ]
        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=8001,
                fesh=702,
                carriers=[99],
                options=[PickupOption(outlet_id=702)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]
        cls.index.offers += [
            Offer(hyperid=3010, fesh=702, title='offer for fix tariff', pickup_buckets=[8001]),
        ]

    def test_clicks_for_fixtariff_shop_in_geo(self):
        for pp in [24, 25, 26, 27]:
            _ = self.report.request_json('place=geo&hyperid=3010&rids=213&show-urls=geo&pp={}'.format(pp))
        self.click_log.expect(ClickType.GEO, shop_id=702, cb=0, cp=0).times(4)

    @classmethod
    def prepare_filters_ignorance_test(cls):
        """
        Создаем магазины и аутлеты, а также 5 офферов.

        Один из офферов подходит под все фильтры из таблицы
        https://wiki.yandex-team.ru/users/msheglov/Kontekstnost-vydachi/#filtrytretegotipa

        Три оффера не подходят под один из фильтров shops, book-now и offer-shipping и все
        они CPA_NO

        Еще один не подходит под эти фильтры, за исключением cpa, shops, book-now и
        offer-shipping
        """
        cls.index.shops += [
            Shop(fesh=18001, priority_region=213, new_shop_rating=NewShopRating(new_rating_total=5.0)),
            Shop(fesh=18002, priority_region=213, new_shop_rating=NewShopRating(new_rating_total=5.0)),
            Shop(fesh=18003, priority_region=213, new_shop_rating=NewShopRating(new_rating_total=5.0)),
            Shop(fesh=18004, priority_region=213, new_shop_rating=NewShopRating(new_rating_total=5.0)),
            Shop(fesh=18005, priority_region=213, new_shop_rating=NewShopRating(new_rating_total=3.0), home_region=10),
        ]

        cls.index.outlets += [
            Outlet(point_id=18201, fesh=18001, region=213, point_type=Outlet.FOR_STORE),
            Outlet(point_id=18202, fesh=18002, region=213, point_type=Outlet.FOR_STORE),
            Outlet(point_id=18203, fesh=18003, region=213, point_type=Outlet.FOR_STORE),
            Outlet(point_id=18204, fesh=18004, region=213, point_type=Outlet.FOR_PICKUP),
            Outlet(point_id=18205, fesh=18005, region=213, point_type=Outlet.FOR_STORE),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=9001,
                fesh=18001,
                carriers=[99],
                options=[PickupOption(outlet_id=18201)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=9002,
                fesh=18002,
                carriers=[99],
                options=[PickupOption(outlet_id=18202)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=9003,
                fesh=18003,
                carriers=[99],
                options=[PickupOption(outlet_id=18203)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=9004,
                fesh=18004,
                carriers=[99],
                options=[PickupOption(outlet_id=18204)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=9005,
                fesh=18005,
                carriers=[99],
                options=[PickupOption(outlet_id=18205)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        booking1 = BookingAvailability(outlet_id=18201, region_id=213, amount=15)
        booking2 = BookingAvailability(outlet_id=18202, region_id=213, amount=15)
        _ = BookingAvailability(outlet_id=18203, region_id=213, amount=15)
        booking4 = BookingAvailability(outlet_id=18204, region_id=213, amount=15)
        booking5 = BookingAvailability(outlet_id=18205, region_id=213, amount=15)

        cls.index.offers += [
            Offer(
                hyperid=18101,
                fesh=18001,
                title='ideal-offer',
                price_old=150,
                booking_availabilities=[booking1],
                delivery_options=[
                    DeliveryOption(price=0, day_from=1, day_to=1),
                ],
                pickup_buckets=[9001],
            ),
            Offer(
                hyperid=18101,
                fesh=18002,
                title='no-cpa-filtered-shop',
                price_old=150,
                booking_availabilities=[booking2],
                pickup_buckets=[9002],
            ),
            Offer(hyperid=18101, fesh=18003, title='no-cpa-without-book-now', price_old=150, pickup_buckets=[9003]),
            Offer(
                hyperid=18101,
                fesh=18004,
                title='no-cpa-no-store',
                price_old=150,
                store=False,
                booking_availabilities=[booking4],
                pickup_buckets=[9004],
            ),
            Offer(
                hyperid=18101,
                fesh=18005,
                title='almost-ideal-offer',
                price=900,
                manufacturer_warranty=False,
                booking_availabilities=[booking5],
                delivery_options=[
                    DeliveryOption(price=100, day_from=1, day_to=5),
                ],
                pickup_buckets=[9005],
            ),
        ]

    def test_geo_without_filters(self):
        """
        Что тестируем: запрос за офферами на geo без явного указания фильтров
        возвращает все офферы модели

        Делаем запрос за офферами на geo без фильтров для модели 18101
        Ожидаем, что возвращается 5 офферов
        """
        response = self.report.request_json('place=geo&hyperid=18101&numdoc=20&rids=213')
        self.assertFragmentIn(response, {"search": {"total": 5}})
        self.assertEqual(response.count({"entity": "offer"}), 5)

    def test_geo_with_filters(self):
        """
        Что тестируем: запрос за офферами на geo с явным указанием фильтров
        отфильтровывает все офферы, кроме "идеального"

        Делаем запрос за офферами на странице offers с фильтрами для модели 18101
        Ожидаем, что возвращается один "идеальный" оффер
        """
        response = self.report.request_json('place=geo&hyperid=18101&numdoc=20&rids=213%s' % SEARCH_FILTERS)
        self.assertFragmentIn(response, {"search": {"total": 1}})
        self.assertEqual(response.count({"entity": "offer"}), 1)
        self.assertFragmentIn(response, {"titles": {"raw": "ideal-offer"}})

    def test_geo_with_filters_ignorance(self):
        """
        Что тестируем: запрос за офферами на странице offers с явным указанием фильтров
        и флажком &relax-filters=1 игнорирует фильтры, кроме cpa, shops, book-now и
        offer-shipping

        Делаем запрос за офферами на странице offers с фильтрами для модели 18101
        и флажком &relax-filters=1
        Ожидаем, что возвращается 2 оффера - "идеальный" и тот, который подходит под все
        фильтры, кроме игнорируемых
        """
        response = self.report.request_json(
            'place=geo&hyperid=18101&numdoc=20&rids=213&relax-filters=1%s' % SEARCH_FILTERS
        )
        self.assertFragmentIn(response, {"search": {"total": 2}})
        self.assertEqual(response.count({"entity": "offer"}), 2)
        self.assertFragmentIn(response, {"titles": {"raw": "ideal-offer"}})
        self.assertFragmentIn(response, {"titles": {"raw": "almost-ideal-offer"}})

    @classmethod
    def prepare_attraction(cls):
        cls.index.models += [
            Model(hyperid=2102, hid=2002),
        ]
        cls.index.offers += [
            Offer(hyperid=2102, title='1_from_2102', bid=500, fesh=1101, pickup_buckets=[7001]),
            Offer(hyperid=2102, title='2_from_2102', bid=400, fesh=1105, pickup_buckets=[7005]),  # Out of sight
            Offer(hyperid=2102, title='3_from_2102', bid=300, fesh=1102, pickup_buckets=[7002]),
            Offer(hyperid=2102, title='4_from_2102', bid=200, fesh=1103, pickup_buckets=[7003]),
            Offer(hyperid=2102, title='5_from_2102', bid=100, fesh=1104, pickup_buckets=[7004]),
        ]

    @staticmethod
    def __create_attraction_point_request(geoLocation=None, userId=None, addBounds=True, addDebug=True, how=None):
        request = 'place=geo&rids=1&hyperid=2102&geo-attraction-distance=7000'  # Расстояние 7000м - требуется для тестовых координат. 2000м, заданных по умолчанию мало.
        if how is not None:
            request += '&how={}'.format(how)
        if addDebug:
            request += '&debug=da'
        if geoLocation is not None:
            request += '&geo-location={}'.format(geoLocation)
        if addBounds:
            request += '&geo_bounds_lb=37.0,55.0&geo_bounds_rt=37.6,55.6'
        if userId is not None:
            request += '&puid={}'.format(userId)

        return request

    def __test_add_attraction_point(self, response, name, coord):
        # Проверка добавления точки притяжения
        # TODO Добавить проверку координат после исправления GeoPoint (перепутаны широта с долготой). https://st.yandex-team.ru/MARKETOUT-12400
        # addText = 'Add attraction point: {}: {}'.format(name, coord)
        addText = 'Add attraction point: {}:'.format(name)
        self.assertFragmentIn(response, {'logicTrace': [Contains(addText)]})

    def __test_missed_attraction_point(self, response, name=None, coord=None):
        # Проверка добавления точки притяжения
        missedText = 'Add attraction point: '
        if name is not None:
            missedText += '{}:'.format(name)
            # TODO Добавить проверку координат после исправления GeoPoint (перепутаны широта с долготой)
            # if coord is not None:
            #    missedText += ' {}'.format(coord)
        self.assertFragmentNotIn(response, {'logicTrace': [Contains(missedText)]})

    def __test_filter_attraction_point(self, response, name, coord):
        # Проверка отфильтровки точки притяжения, не попавшей в окно
        # TODO Добавить проверку координат после исправления GeoPoint (перепутаны широта с долготой)
        # filterText = 'Attraction point filtered by geo-bounds: {}: {}'.format(name, coord)
        filterText = 'Attraction point filtered by geo-bounds: {}:'.format(name)
        self.__test_missed_attraction_point(response, name)
        self.assertFragmentIn(response, {'logicTrace': [Contains(filterText)]})

    def test_cgi_param_attraction_points(self):
        '''
        Что тестируем: добавляются точки притяжения, переданные через параметры
        '''
        response = self.report.request_json(
            self.__create_attraction_point_request(geoLocation=LOCATION_GPS_COORD)
            + '&geo-attraction={}'.format(FAR_GPS_COORD)
            + '&geo-attraction={}'.format(HOME_GPS_COORD)
        )
        self.__test_add_attraction_point(response, 'cgi-param', FAR_GPS_COORD)
        self.__test_add_attraction_point(response, 'cgi-param', HOME_GPS_COORD)

    def assert_equal_json_responses_without_click_urls(self, request1, request2):
        self.assertEqualJsonResponses(request1, request2)

    def __test_ranking_with_attraction_points(self, request, outletsOrder):
        # Запрашиваем общий список
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            [{"entity": "offer", "outlet": {"id": "{}".format(outlet_id)}} for outlet_id in outletsOrder],
            preserve_order=True,
            allow_different_len=False,
        )

        # Проверяем сохранения порядка при разбиении на страницы
        for i in [1, 10]:
            response = self.report.request_json(request + '&numdoc=1&page={}'.format(i))
            self.assertFragmentIn(
                response,
                [{"entity": "offer", "outlet": {"id": "{}".format(outletsOrder[i - 1])}}],
                preserve_order=True,
                allow_different_len=False,
            )

    def test_offer_shipping_delivery(self):
        # Что тестируем: запрос c offer-shipping=delivery игнорирует этот фильтр
        # и возвращает аутлеты типов pickup и store
        response = self.report.request_json('place=geo&offer-shipping=delivery&hid=501&rids=0')
        self.assertFragmentIn(response, {"outlet": {"type": "pickup"}})
        self.assertFragmentIn(response, {"outlet": {"type": "store"}})

    def test_multiple_point_id(self):
        """Проверяется, что фильтр &point_id с несколькими значениями отрабатывает корректно"""
        response = self.report.request_json('place=geo&text=good11&rids=1&point_id=212,214,215')
        self.assertFragmentIn(
            response,
            {
                "total": 3,
                "results": [
                    {"entity": "offer", "outlet": {"id": "212"}},
                    {"entity": "offer", "outlet": {"id": "214"}},
                    {"entity": "offer", "outlet": {"id": "215"}},
                ],
            },
        )
        self.assertEqual(3, response.count({"outlet": {}}))

    @classmethod
    def prepare_bundle_min_price(cls):
        cls.index.hypertree += [
            HyperCategory(hid=730),
            HyperCategory(hid=731),
            HyperCategory(hid=732),
        ]
        cls.index.models += [
            Model(hyperid=73, hid=730),
            Model(hyperid=74, hid=730),
            Model(hyperid=75, hid=731),
            Model(hyperid=76, hid=732),
        ]
        cls.index.shops += [
            Shop(fesh=7301, priority_region=1, name='Shop_7301'),
            Shop(fesh=7302, priority_region=1, name='Shop_7302'),
            Shop(fesh=7303, priority_region=1),
        ]

        cls.index.outlets += [
            Outlet(point_id=7301, fesh=7301, region=1, gps_coord=GpsCoord(37.1, 55.1)),
            Outlet(point_id=7302, fesh=7302, region=1, gps_coord=GpsCoord(37.1, 55.1)),
            Outlet(point_id=7303, fesh=7303, region=1, gps_coord=GpsCoord(30.0, 50.0)),
            Outlet(point_id=7304, fesh=7304, region=1, gps_coord=GpsCoord(30.1, 50.1)),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=9101,
                fesh=7301,
                carriers=[99],
                options=[PickupOption(outlet_id=7301)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=9102,
                fesh=7302,
                carriers=[99],
                options=[PickupOption(outlet_id=7302)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=9103,
                fesh=7303,
                carriers=[99],
                options=[PickupOption(outlet_id=7303)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=9104,
                fesh=7304,
                carriers=[99],
                options=[PickupOption(outlet_id=7304)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        cls.index.offers += [
            Offer(hyperid=73, title='1_73_shop1', price=123, fesh=7301, pickup_buckets=[9101]),
            Offer(hyperid=73, title='2_73_shop1', price=126, fesh=7301, pickup_buckets=[9101]),
            Offer(hyperid=73, title='3_73_shop1', price=120, fesh=7301, pickup_buckets=[9101]),
            Offer(hyperid=74, title='1_74_shop1', price=140, fesh=7301, pickup_buckets=[9101]),
            Offer(hyperid=74, title='2_74_shop2', price=274.1, fesh=7302, pickup_buckets=[9102]),
            # Для проверки, что в минимальную цену не включаются офферы из других категорий
            Offer(hid=731, hyperid=75, title='telephone 1', price=456, fesh=7303, pickup_buckets=[9103], randx=100),
            Offer(hid=731, hyperid=75, title='telephone 2', price=420, fesh=7303, pickup_buckets=[9103], randx=100),
            Offer(hid=732, hyperid=76, title='telephone 3', price=410, fesh=7303, pickup_buckets=[9103], randx=1),
            Offer(hid=732, hyperid=76, title='telephone 4', price=440, fesh=7303, pickup_buckets=[9103], randx=1),
            Offer(hid=731, hyperid=75, title='telephone 5', price=100, fesh=7304, pickup_buckets=[9104], randx=100),
            Offer(hid=732, hyperid=76, title='telephone 6', price=101, fesh=7304, pickup_buckets=[9104], randx=1),
        ]
        cls.index.currencies = [
            Currency(
                name=Currency.BYN,
                exchange_rates=[
                    ExchangeRate(to=Currency.RUR, rate=0.5),
                ],
            ),
        ]

    def test_bundle_min_price_in_category(self):
        """
        Проверяем, что на заданном оффере минимальная цена считается среди офферов из этого же магазина и из категории оффера,
        а также, что кол-во офферов из этого же магазина учитывает только офферы из категории заданного оффера
        """

        response = self.report.request_json(
            'place=geo&geo-location=30.0,50.0&geo_bounds_lb=29.9,49.9&geo_bounds_rt=30.2,50.2&text=telephone&rids=1'
        )

        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "titles": {"raw": "telephone 1"},
                    "bundled": {
                        "shopCategory": {
                            "minPrice": {"value": "420"},
                            "count": 2,
                        },
                        "count": 4,
                    },
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "telephone 5"},
                    "bundled": {
                        "shopCategory": {
                            "minPrice": {"value": "100"},
                            "count": 1,
                        },
                        "count": 2,
                    },
                },
            ],
        )

        """
        Проверяем, что при задании конкретной точки на карте отдается 'bundled'
        """
        response = self.report.request_json("place=geo&point_id=7302&text=_shop")
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "bundled": {
                        "shopCategory": {
                            "minPrice": {
                                "value": "274",
                            },
                            "count": 1,
                        }
                    },
                }
            ],
        )

    def test_bundle_min_price_BYN(self):
        """
        Проверяем, что минимальная цена считается правильно (в валюте пользователя)
        """
        response = self.report.request_json(
            'place=geo&geo-location=37.15,55.15&geo_bounds_lb=37.0,55.0&geo_bounds_rt=37.2,55.2&text=_shop&rids=0&currency=BYN'
        )

        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "bundleCount": 4,
                    "bundled": {
                        "count": 4,
                        "shopCategory": {
                            "count": 4,
                            "minPrice": {
                                "currency": "BYN",
                                "value": "240",
                            },
                        },
                    },
                },
                {
                    "entity": "offer",
                    "bundleCount": 1,
                    "bundled": {
                        "count": 1,
                        "shopCategory": {
                            "count": 1,
                            "minPrice": {
                                "currency": "BYN",
                                "value": "548.2",
                            },
                        },
                    },
                },
            ],
        )

    def test_bundle_min_price_RUB(self):
        """
        Проверяем, что минимальная цена считается правильно
        """
        response = self.report.request_json(
            'place=geo&geo-location=37.15,55.15&geo_bounds_lb=37.0,55.0&geo_bounds_rt=37.2,55.2&text=_shop&rids=0'
        )

        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "bundleCount": 4,
                    "bundled": {
                        "count": 4,
                        "shopCategory": {
                            "count": 4,
                            "minPrice": {
                                "currency": "RUR",
                                "value": "120",
                            },
                        },
                    },
                },
                {
                    "entity": "offer",
                    "bundleCount": 1,
                    "bundled": {
                        "count": 1,
                        "shopCategory": {
                            "count": 1,
                            "minPrice": {
                                "currency": "RUR",
                                "value": "274",
                            },
                        },
                    },
                },
            ],
        )

    @classmethod
    def prepare_cpa_no_offers_with_delivery_service_on_geo(cls):
        cls.index.shops += [
            Shop(
                fesh=17000,
                priority_region=2,
                name='Shop17000',
                delivery_service_outlets=[17002],
            )
        ]

        cls.index.outlets += [
            Outlet(
                point_id=17002,
                delivery_service_id=17001,
                region=4,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    shipper_id=103, day_from=1, day_to=1, order_before=2, work_in_holiday=True, price=100
                ),
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(lon=37.12, lat=55.32),
            ),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=9201,
                carriers=[17001],
                options=[PickupOption(outlet_id=17002)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        cls.index.offers += [Offer(fesh=17000, title='NoneCpaOfferWithOutlet', hyperid=17003, pickup_buckets=[9201])]

    def test_cpa_no_offers_with_delivery_service_on_geo_in_param_how(self):
        """
        Что тестируем: показ на geo не СРА офера, имеющего точки ПВЗ от служб доставки
        """
        for extra in ['', '&how=model_card']:
            response = self.report.request_json(
                'place=geo&hyperid=17003&fesh=17000&rids=4&show-urls=geo&geo_bounds_rt=37.30%2C56.2&geo_bounds_lb=37.0%2C55.1&geo-location=37.1%2C55.15'
                + extra
            )
            self.assertFragmentIn(
                response,
                {'results': [{'entity': 'offer', 'titles': {'raw': 'NoneCpaOfferWithOutlet'}}]},
                allow_different_len=False,
            )

    def test_cpa_no_offers_with_delivery_service_on_geo(self):
        """
        Что тестируем: показ на geo не СРА офера, имеющего точки ПВЗ от служб доставки
        """
        for extra in ['', '&default-how-on-geo=model_card']:
            response = self.report.request_json(
                'place=geo&hyperid=17003&fesh=17000&rids=4&show-urls=geo&geo_bounds_rt=37.30%2C56.2&geo_bounds_lb=37.0%2C55.1&geo-location=37.1%2C55.15'
                + extra
            )
            self.assertFragmentIn(
                response,
                {'results': [{'entity': 'offer', 'titles': {'raw': 'NoneCpaOfferWithOutlet'}}]},
                allow_different_len=False,
            )

    @classmethod
    def prepare_not_only_top_offer_outlets(cls):
        """
        Для проверки отображения точек магазина, в которых нет самого "релевантного" оффера,
        создаём магазин с тремя офферами и тремя аутлетами.
        Каждый оффер продаётся только в одном аутлете. Сами офферы определены в классе _Offers.
        """
        cls.index.shops += [Shop(fesh=12320, priority_region=213, delivery_service_outlets=[12323])]

        cls.index.outlets += [
            Outlet(
                point_id=12321, fesh=12320, region=213, point_type=Outlet.FOR_STORE, gps_coord=GpsCoord(37.12, 55.32)
            ),
            Outlet(
                point_id=12322, fesh=12320, region=213, point_type=Outlet.FOR_PICKUP, gps_coord=GpsCoord(38.12, 54.67)
            ),
            Outlet(
                point_id=12323,
                region=213,
                point_type=Outlet.FOR_POST_TERM,
                delivery_service_id=1030,
                gps_coord=GpsCoord(36.19, 56.1),
                delivery_option=OutletDeliveryOption(price=10),
            ),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=9301,
                fesh=12320,
                carriers=[99],
                options=[PickupOption(outlet_id=12321, price=0), PickupOption(outlet_id=12322, price=0)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=9302,
                carriers=[1030],
                options=[PickupOption(outlet_id=12323, price=10)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        cls.index.offers += [_Offers.pickup_offer, _Offers.store_offer, _Offers.post_term_offer]

    def test_not_only_top_offer_outlets(self):
        """
        Проверка того, что на выдаче отображаются не только точки магазина, соответствующие
        наиболее релевантному документу.
        """
        geo_request = (
            'place=geo&rids=213&require-geo-coords=1&fesh=12320'
            '&tile=153%2C79&tile=153%2C80&tile=153%2C81&tile=154%2C79&tile=154%2C80&tile=154%2C81'
            '&tile=155%2C79&tile=155%2C80&tile=155%2C81&tile=156%2C79&tile=156%2C80&tile=156%2C81'
            '&zoom=8&ontile=5'
        )
        geo_tiles = geo_request + '&show-outlet=tiles'
        geo_offers = geo_request + '&show-outlet=offers'

        TestOutlet = namedtuple('Outlet', ['coord', 'id', 'type'])

        def as_tile(outlet):
            return {"coord": outlet.coord, "outlets": [{"id": str(outlet.id), "type": outlet.type}]}

        outlet_store = TestOutlet(coord={"x": 154, "y": 80, "zoom": 8}, id=12321, type='store')
        outlet_pickup = TestOutlet(coord={"x": 155, "y": 81, "zoom": 8}, id=12322, type='pickup')
        outlet_post_term = TestOutlet(coord={"x": 153, "y": 79, "zoom": 8}, id=12323, type='pickup')

        test_outlets = (outlet_store, outlet_pickup, outlet_post_term)
        test_offers = (_Offers.store_offer, _Offers.pickup_offer, _Offers.post_term_offer)

        # Проверяем, что по отдельности для каждого оффера доступна доставка только в один аутлет
        for offer, offer_outlet in zip(test_offers, test_outlets):
            tiles_response = self.report.request_json(geo_tiles + '&hyperid=' + offer.hyperid)
            for outlet in test_outlets:
                if outlet == offer_outlet:
                    self.assertFragmentIn(tiles_response, {"search": {"tiles": [as_tile(outlet)]}})
                else:
                    self.assertFragmentNotIn(tiles_response, {"search": {"tiles": [as_tile(outlet)]}})

            # И такая же проверка для варианта с запросом офферов
            offers_response = self.report.request_json(geo_offers + '&offerid=' + offer.ware_md5)
            self.assertFragmentIn(
                offers_response,
                {
                    "search": {
                        "results": [
                            {
                                "entity": "offer",
                                "outlet": {"entity": "outlet", "id": str(offer_outlet.id), "type": offer_outlet.type},
                            }
                        ]
                    }
                },
                allow_different_len=False,
            )

        # Для запроса без указания модели комбинациями флажков получаются все доступные точки магазина
        shop_response = self.report.request_json(geo_tiles)
        self.assertFragmentIn(shop_response, {"search": {"tiles": [as_tile(o) for o in test_outlets]}})

    @classmethod
    def prepare_mega_points(cls):
        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=2300,
                carriers=[270],
                options=[PickupOption(outlet_id=12345670, price=100)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=2301,
                carriers=[270],
                options=[PickupOption(outlet_id=12345671, price=100)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=2302,
                options=[PickupOption(outlet_id=12345672, price=100)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        cls.index.shops += [
            Shop(fesh=73001, regions=[1], delivery_service_outlets=[12345670, 12345671]),
            Shop(fesh=73002, regions=[1], delivery_service_outlets=[12345670, 12345671]),
            Shop(fesh=73003, regions=[1], delivery_service_outlets=[12345672]),
        ]

        cls.index.offers += [
            Offer(fesh=73001, title='offer from mega points 0 and 1 : 1', pickup_buckets=[2300, 2301]),
            Offer(fesh=73002, title='offer from mega points 0 and 1 : 2', pickup_buckets=[2300, 2301]),
            Offer(fesh=73003, title='offer from mega point 2 : 1', pickup_buckets=[2302]),
        ]

        cls.index.outlets += [
            Outlet(
                point_id=12345670,
                region=1,
                gps_coord=GpsCoord(37.1, 55.1),
                point_type=Outlet.FOR_PICKUP,
                delivery_service_id=270,
            ),
            Outlet(point_id=12345671, region=1, gps_coord=GpsCoord(37.15, 55.1), delivery_service_id=270),
            Outlet(point_id=12345672, region=1, gps_coord=GpsCoord(37.15, 55.15), fesh=73003),
        ]

    def test_show_outlet_entities(self):
        '''
        Проверяем логику работы geo с мега-точками: в один аутлет может доставлять несколько магазинов.
        Для этого geo должен возвращать по одному офферу на каждый аутлет
        и возвращать ответ results в другому формате: массив аутлетов, внутри которых лежит один оффер.
        Вся новая логика работает только при переданном параметре show-outlet-entities=true.
        '''
        req = 'place=geo&text=mega&geo-location=37.15,55.15&geo_bounds_lb=37.0,55.0&geo_bounds_rt=37.2,55.2&rids=1&debug=da&how=search&show-outlet=offers'
        # Проверка старого формата. Всего 5 офферов из 3-х магазинов. У некоторых совпадают точки доставки, но группировки по аутлетам нет
        response = self.report.request_json(req)
        self.assertFragmentIn(
            response,
            [
                # две точки у магазина 73001
                {
                    'titles': {'raw': 'offer from mega points 0 and 1 : 1'},
                    'shop': {'id': 73001},
                    'outlet': {'id': '12345670'},
                },
                {
                    'titles': {'raw': 'offer from mega points 0 and 1 : 1'},
                    'shop': {'id': 73001},
                    'outlet': {'id': '12345671'},
                },
                # они же у магазина 73002
                {
                    'titles': {'raw': 'offer from mega points 0 and 1 : 2'},
                    'shop': {'id': 73002},
                    'outlet': {'id': '12345670'},
                },
                {
                    'titles': {'raw': 'offer from mega points 0 and 1 : 2'},
                    'shop': {'id': 73002},
                    'outlet': {'id': '12345671'},
                },
                # одна точка у магазина 73003
                {'titles': {'raw': 'offer from mega point 2 : 1'}, 'shop': {'id': 73003}, 'outlet': {'id': '12345672'}},
            ],
        )

        # Проверка нового формата с группировкой по аутлетам
        response = self.report.request_json(req + '&show-outlet-entities=true')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "outlet",
                        "id": "12345670",
                        "isMegaPoint": True,
                        "serviceId": 270,
                        "offer": {
                            "entity": "offer",
                        },
                    },
                    {
                        "entity": "outlet",
                        "id": "12345671",
                        "isMegaPoint": True,
                        "serviceId": 270,
                        "offer": {
                            "entity": "offer",
                        },
                    },
                    {
                        "entity": "outlet",
                        "id": "12345672",
                        "isMegaPoint": False,
                        "serviceId": 99,
                        "offer": {
                            "entity": "offer",
                        },
                    },
                ]
            },
            allow_different_len=False,
        )

        '''
        Тест, что filters и sorts отсутствуют внутри search
        '''
        self.assertFragmentIn(response, {"search": {"results": [], "filters": Absent(), "sorts": Absent()}})

    @classmethod
    def prepare_regset(cls):
        cls.index.shops += [Shop(fesh=73004)]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=90002,
                options=[PickupOption(outlet_id=90002, price=100)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        cls.index.outlets += [
            Outlet(point_id=90002, region=2, gps_coord=GpsCoord(37.15, 55.15), fesh=73004),
        ]

        cls.index.offers += [Offer(fesh=73004, title='offer', pickup_buckets=[90002])]

    def test_regset(self):
        '''
        Проверяем работу regset=1
        Проверяем, что при запросе с rids=1 и аутлетом из rids=2 оффер приходит
        '''
        response = self.report.request_json('place=geo&fesh=73004&point_id=90002&rids=1&regset=1')
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "titles": {"raw": "offer"},
            },
        )

        ''' Проверяем, что без regset=1 и с rids=2 оффер есть (запрос из региона аутлета) '''
        response = self.report.request_json('place=geo&fesh=73004&point_id=90002&rids=2')
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "titles": {"raw": "offer"},
            },
        )

        ''' Проверяем, что с regset=1 и с rids=2 оффер есть (запрос из региона аутлета) '''
        response = self.report.request_json('place=geo&fesh=73004&point_id=90002&rids=2&regset=1')
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "titles": {"raw": "offer"},
            },
        )

        ''' Проверяем, что без regset=1 оффера нет (так как этот аутлет не из региона пользователя) '''
        response = self.report.request_json('place=geo&fesh=73004&point_id=90002&rids=1')
        self.assertFragmentNotIn(
            response,
            {
                "entity": "offer",
                "titles": {"raw": "offer"},
            },
        )

    virtual_model_id_range_start = int(2 * 1e12)
    virtual_model_id_range_finish = int(virtual_model_id_range_start + 1e15)
    virtual_model_id = (virtual_model_id_range_start + virtual_model_id_range_finish) // 2

    @classmethod
    def prepare_virtual_model(cls):
        cls.index.shops += [
            Shop(fesh=4242, priority_region=1, name='Shop11'),
        ]

        cls.index.outlets += [
            Outlet(point_id=4242, fesh=4242, region=1, gps_coord=GpsCoord(37.1, 55.1), point_type=Outlet.FOR_STORE),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=4242,
                fesh=4242,
                carriers=[99],
                options=[PickupOption(outlet_id=4242)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        cls.index.offers += [
            Offer(
                waremd5='OfferNoModel_________g',
                title="Наковальня #10",
                fesh=4242,
                hid=4242,
                pickup_buckets=[4242],
                virtual_model_id=T.virtual_model_id,
            ),
        ]

    def test_virtual_model(self):
        # Теперь флаги виртуальных карточек по-умолчанию включены
        request_base = (
            'place=geo&geo-location=37.15,55.15&geo_bounds_lb=37.0,55.0&geo_bounds_rt=37.2,55.2&hyperid=%s&rids=0'
            % T.virtual_model_id
        )
        flags = '&rearr-factors=market_cards_everywhere_range={}:{}'.format(
            T.virtual_model_id_range_start, T.virtual_model_id_range_finish
        )
        response = self.report.request_json(request_base + flags)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "shops": 1,
                    "shopOutlets": 1,
                    "results": [
                        {
                            "entity": "offer",
                            "bundleCount": 1,
                            "bundled": {
                                "count": 1,
                                "modelId": T.virtual_model_id,
                                "outletId": 4242,
                            },
                            "wareId": "OfferNoModel_________g",
                            "outlet": {
                                "id": "4242",
                            },
                        },
                    ],
                },
            },
            allow_different_len=False,
        )

        for flags in [
            '&rearr-factors=market_cards_everywhere_geo=0',
            '&rearr-factors=market_cards_everywhere_geo=1;market_cards_everywhere_range={}:{}'.format(
                T.virtual_model_id_range_start, T.virtual_model_id_range_start + 1
            ),
        ]:
            response = self.report.request_json(request_base + flags)
            self.assertFragmentIn(
                response,
                {
                    "search": {"total": 0, "shops": 0, "shopOutlets": 0, "results": NoKey("results")},
                },
                allow_different_len=False,
            )


if __name__ == '__main__':
    main()
