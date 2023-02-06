#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.matcher import Round, Absent
from core.testcase import TestCase, main
from core.types import (
    DeliveryBucket,
    DeliveryOption,
    DynamicDeliveryRestriction,
    DynamicWarehouseDelivery,
    DynamicWarehouseLink,
    GpsCoord,
    Outlet,
    OutletDeliveryOption,
    PickupBucket,
    PickupOption,
    Region,
    RegionalDelivery,
    Shop,
)
from core.types.taxes import Tax

NORDSTREAM_FLAGS = (
    ('', True),
    ('&rearr-factors=market_nordstream=0', False),
    ('&rearr-factors=market_nordstream=1', True),
)


class T(TestCase):
    FakeOfferDimensions = {"weight": 2, "width": 2, "height": 2, "length": 2}

    @classmethod
    def prepare(cls):
        cls.settings.logbroker_enabled = True
        cls.settings.nordstream_autogenerate = False
        # НЕ делайте так в новых тестах!
        # Походов в КД на проде уже нет, пожалуйста, проверяйте новую функциональность, создавая доставку через комбинатор
        cls.settings.default_search_experiment_flags += ['force_disable_delivery_calculator_requests=0']

        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

    @classmethod
    def prepare_shops(cls):
        cls.index.shops += [
            Shop(
                fesh=1,
                name='virtual_shop',
                tax_system=Tax.OSN,
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                delivery_service_outlets=[
                    10,
                    23,
                ],
                cpa=Shop.CPA_REAL,
            ),
            Shop(
                fesh=2,
                datafeed_id=2,
                name='blue_shop',
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
            ),
            Shop(
                fesh=3,
                datafeed_id=3,
                name='golden_partner',
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
            ),
            Shop(fesh=4, datafeed_id=4, name='green_shop', tax_system=Tax.OSN, blue=Shop.BLUE_NO, cpa=Shop.CPA_REAL),
        ]

    @classmethod
    def prepare_outlets(cls):
        cls.index.outlets += [
            Outlet(
                point_id=10,
                delivery_service_id=103,
                region=5005,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(shipper_id=1, day_from=1, day_to=1, order_before=2, price=100),
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(37.12, 55.32),
            ),
            Outlet(
                point_id=11,
                fesh=1,
                region=4004,
                point_type=Outlet.FOR_STORE,
                delivery_option=OutletDeliveryOption(shipper_id=1, day_from=1, day_to=1, order_before=1, price=100),
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(38.12, 54.67),
            ),
            Outlet(
                point_id=12,
                fesh=2,
                region=2002,
                point_type=Outlet.FOR_POST_TERM,
                delivery_option=OutletDeliveryOption(shipper_id=2, day_from=2, day_to=2, order_before=1, price=100),
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(36.13, 55.45),
            ),
            Outlet(
                point_id=13,
                fesh=3,
                region=3003,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(day_from=3, day_to=1, order_before=1, price=50),
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(36.14, 55.43),
            ),
            Outlet(
                point_id=14,
                fesh=4,
                region=81334,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(shipper_id=3, day_from=1, day_to=1, order_before=1, price=50),
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(37.14, 55.14),
            ),
            Outlet(
                point_id=15,
                region=7107,
                point_type=Outlet.FOR_PICKUP,
                delivery_service_id=103,
                delivery_option=OutletDeliveryOption(shipper_id=3, day_from=1, day_to=1, order_before=1, price=50),
                working_days=[i for i in range(10)],
            ),
            Outlet(
                point_id=23,
                delivery_service_id=103,
                region=10714,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(shipper_id=1, day_from=1, day_to=1, order_before=2, price=100),
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(34.12, 55.32),
            ),
        ]

    @classmethod
    def prepare_buckets(cls):
        cls.index.delivery_buckets += [
            # Доставка из Беру
            DeliveryBucket(
                bucket_id=801,
                fesh=1,
                carriers=[103],
                regional_options=[
                    RegionalDelivery(
                        rid=5005, options=[DeliveryOption(price=5, day_from=1, day_to=5, shop_delivery_price=10)]
                    ),
                    RegionalDelivery(
                        rid=4004,
                        options=[
                            DeliveryOption(price=5, day_from=1, day_to=2, shop_delivery_price=10),
                            DeliveryOption(price=3, day_from=4, day_to=5, shop_delivery_price=10),
                        ],
                    ),
                    RegionalDelivery(
                        rid=10714, options=[DeliveryOption(price=5, day_from=1, day_to=5, shop_delivery_price=10)]
                    ),
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            # Неактуальная доставка
            DeliveryBucket(
                bucket_id=1001,
                fesh=1,
                carriers=[103],
                regional_options=[RegionalDelivery(rid=11, options=[DeliveryOption(price=3, day_from=2, day_to=5)])],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM_RED,
            ),
            # Доставка зелёного магазина, не должны учитывать
            DeliveryBucket(
                bucket_id=803,
                fesh=4,
                carriers=[123],
                regional_options=[
                    RegionalDelivery(
                        rid=81334,
                        options=[
                            DeliveryOption(price=30, day_from=1, day_to=3),
                            DeliveryOption(price=5, day_from=4, day_to=5),
                        ],
                    )
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        cls.index.pickup_buckets += [
            # доставка из обычного магазина на синем маркете
            PickupBucket(
                bucket_id=5001,
                dc_bucket_id=4,
                fesh=2,
                carriers=[123],
                options=[PickupOption(outlet_id=12, day_from=1, day_to=2, price=5)],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            # доставка из магазина, который сам выполняет заказы (fulfillment=false)
            PickupBucket(
                bucket_id=5002,
                dc_bucket_id=5,
                fesh=3,
                carriers=[103],
                options=[PickupOption(outlet_id=13)],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
        ]

    @classmethod
    def prepare_geo(cls):
        """
        Добавляем используемые регионы в допустимые для указания в CGI, чтобы не зависеть от создаваемых по умолчанию.
        """
        cls.index.regiontree += [
            Region(rid=1001),
            Region(rid=2002),
            Region(rid=3003),
            Region(rid=4004),
            Region(rid=5005, name='parent', children=[Region(rid=10012, name='child')]),
            Region(rid=11111),
            Region(rid=81334),
        ]

    @classmethod
    def prepare_nordstream(cls):
        WAREHOUSE_ID = 1  # для теста добавляем доставку только от одного склада
        cls.dynamic.nordstream += [
            DynamicWarehouseLink(WAREHOUSE_ID, [WAREHOUSE_ID]),
            DynamicWarehouseDelivery(
                WAREHOUSE_ID,
                {
                    region: [DynamicDeliveryRestriction(min_days=1, max_days=2)]
                    for region in (
                        5005,
                        4004,
                        3003,
                        2002,
                        10714,
                        81334,
                        7107,
                        300,
                        310,
                        311,
                        322,
                        332,
                        201,
                        202,
                        203,
                        204,
                        400,
                    )
                },
            ),
        ]

    def check_blue(self, region, expected, check_nordstream=False):
        request = 'place=check_delivery_available&rids={}'.format(region)
        rearr_options = (
            [rearr_flag for (rearr_flag, _) in NORDSTREAM_FLAGS]
            if check_nordstream
            else ['&rearr-factors=market_nordstream=0']
        )
        for rearr in rearr_options:
            response = self.report.request_json(request + rearr)
            self.assertFragmentIn(response, {"deliveryAvailable": expected})

    def test_virtual_shop_region(self):
        """Проверяется, что возвращается регион 5005, указанный в доставке для синего маркета"""
        self.check_blue(5005, True, check_nordstream=True)

    def test_invalid_delivery_program(self):
        """Проверяется, что 11 регион, доступный только через MARKET_DELIVERY_PROGRAM_RED, не возвращается"""
        self.check_blue(11, False, check_nordstream=True)

    def test_supplier_shop(self):
        """Проверяется доставка из аутлета, привязанного к магазину на синем"""
        self.check_blue(2002, True)

    def test_delivery_fulfilled_by_supplier(self):
        """Проверяется доставка из аутлета, привязанного к магазину, самому исполняющему свои заказы на синем"""
        self.check_blue(3003, True, check_nordstream=True)

    def test_delivery_green(self):
        """Доставка из зелёного магазина не должна учитываться"""
        self.check_blue(81334, False)

    def test_region_without_buckets(self):
        """Для региона не было создано бакетов"""
        self.check_blue(11111, False)

    def test_child_region_request(self):
        """True для дочернего региона при существующей доставке в родителя"""
        self.check_blue(10012, True, check_nordstream=True)

    @classmethod
    def prepare_regions_near_180(cls):
        """
          Добавляем регионы на границе 180* долготы.
          179.5     180.0    -179.5
        +0.5+----------+-----------+
            |  201     |   202 205 |
          0 +----------+-----------+
            |  203 206 |   204     |
        -0.5+----------+-----------+
        """
        cls.index.regiontree += [
            Region(
                rid=201,
                latitude=0.2,
                longitude=179.6,
            ),
            Region(
                rid=202,
                latitude=0.2,
                longitude=-179.7,
            ),
            Region(
                rid=203,
                latitude=-0.2,
                longitude=179.6,
            ),
            Region(
                rid=204,
                latitude=-0.2,
                longitude=-179.6,
            ),
            # Регионы, в которые нет доставки
            Region(
                rid=205,
                latitude=0.45,
                longitude=-179.8,
            ),
            Region(
                rid=206,
                latitude=-0.45,
                longitude=179.8,
            ),
        ]

        cls.index.outlets += [
            Outlet(
                point_id=rid,
                delivery_service_id=123,
                region=rid,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(shipper_id=1, day_from=1, day_to=1, order_before=2, price=100),
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(37.12, 55.32),
            )
            for rid in range(201, 205)
        ]
        cls.index.pickup_buckets += [
            # доставка из обычного магазина на синем маркете
            PickupBucket(
                bucket_id=201,
                dc_bucket_id=201,
                fesh=2,
                carriers=[123],
                options=[PickupOption(outlet_id=o_id, day_from=1, day_to=2, price=5) for o_id in range(201, 205)],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
        ]

    def test_regions_near_180(self):
        """
        Проверяем, что для городов вблизи 180* долготы берутся регионы которые находятся в противоположном полушарии
        """
        for flag, _ in NORDSTREAM_FLAGS:
            request = 'place=check_delivery_available&rids={}' + flag
            for rid in [205, 206]:
                response = self.report.request_json(request.format(rid))
                self.assertFragmentIn(
                    response, [{"region": {"id": near_id}} for near_id in range(201, 205)], allow_different_len=False
                )

    def test_regions_near_180_distance(self):
        """
        Проверяем вычисление растояния до городов около 180* долготы
        """
        request = 'place=check_delivery_available&rids={}'

        for flag, _ in NORDSTREAM_FLAGS:
            response = self.report.request_json(request.format(205) + flag)
            self.assertFragmentIn(
                response,
                [
                    {"region": {"id": near_id}, "distanceKm": Round(distance, 1)}
                    for near_id, distance in [(202, 29.9), (201, 72.1), (204, 75.5), (203, 98.1)]
                ],
                preserve_order=True,
            )

            response = self.report.request_json(request.format(206) + flag)
            self.assertFragmentIn(
                response,
                [
                    {"region": {"id": near_id}, "distanceKm": Round(distance, 1)}
                    for near_id, distance in [(203, 35.5), (204, 72.1), (201, 75.5), (202, 91.0)]
                ],
                preserve_order=True,
            )

    @classmethod
    def prepare_nearest_regions_parsing(cls):
        cls.index.regiontree += [
            Region(
                rid=299,
                latitude=50.1,
                longitude=50.1,
            ),
            Region(
                # Курьерская доставка в общий регион с выключенным 301
                rid=300,
                latitude=50.1,
                longitude=50.1,
                region_type=Region.FEDERAL_DISTRICT,
                children=[
                    Region(
                        rid=region_id,
                        region_type=region_type,
                        latitude=50.1,
                        longitude=50.1,
                    )
                    for region_id, region_type in [
                        (301, Region.CITY),
                        (302, Region.CITY),
                        (303, Region.CITY),
                        (304, Region.VILLAGE),
                        (305, Region.CITY_DISTRICT),
                        (306, Region.OVERSEAS),
                        (307, Region.METRO_STATION),
                        (308, Region.SETTLEMENT),
                        (309, Region.SECONDARY_DISTRICT),
                    ]
                ],
            ),
            Region(
                # Курьерская доставка в общий регион с выключенным 311 + отдельно 311
                rid=310,
                latitude=50.1,
                longitude=50.1,
                region_type=Region.FEDERAL_DISTRICT,
                children=[
                    Region(
                        rid=rid,
                        latitude=50.1,
                        longitude=50.1,
                    )
                    for rid in [311, 312, 313]
                ],
            ),
            Region(
                # Доставка в оутлеты района города. Укрупняется до города
                rid=320,
                latitude=50.1,
                longitude=50.1,
                region_type=Region.FEDERAL_DISTRICT,
                children=[
                    Region(
                        rid=321,
                        latitude=50.1,
                        longitude=50.1,
                        region_type=Region.CITY,
                        children=[
                            Region(rid=322, latitude=50.1, longitude=50.1, region_type=Region.CITY_DISTRICT),
                        ],
                    ),
                ],
            ),
            Region(
                # Доставка почтой. Логика укрупнения такая же, как в оутлетах
                rid=330,
                latitude=50.1,
                longitude=50.1,
                region_type=Region.FEDERAL_DISTRICT,
                children=[
                    Region(
                        rid=331,
                        latitude=50.1,
                        longitude=50.1,
                        region_type=Region.CITY,
                        children=[
                            Region(rid=332, latitude=50.1, longitude=50.1, region_type=Region.CITY_DISTRICT),
                        ],
                    ),
                ],
            ),
        ]

        cls.index.outlets += [
            Outlet(
                point_id=322,
                delivery_service_id=103,
                region=322,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(shipper_id=1, day_from=1, day_to=1, order_before=2, price=100),
                working_days=[i for i in range(10)],
            ),
            # Почта
            Outlet(
                point_id=332,
                delivery_service_id=66,
                region=332,
                point_type=Outlet.FOR_POST,
                delivery_option=OutletDeliveryOption(shipper_id=1, day_from=1, day_to=1, order_before=2, price=100),
                working_days=[i for i in range(10)],
            ),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=300,
                fesh=1,
                carriers=[103],
                regional_options=[
                    RegionalDelivery(
                        rid=300, options=[DeliveryOption(price=5, day_from=1, day_to=5, shop_delivery_price=10)]
                    ),
                    RegionalDelivery(
                        rid=301, forbidden=True
                    ),  # 301 регион запрещен в этом бакете. Доставки в него не будет
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=310,
                fesh=1,
                carriers=[103],
                regional_options=[
                    RegionalDelivery(
                        rid=310, options=[DeliveryOption(price=5, day_from=1, day_to=5, shop_delivery_price=10)]
                    ),
                    RegionalDelivery(rid=311, forbidden=True),  # 311 регион запрещен в этом бакете
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            # Отдельный бакет для 311 региона разрешает доставку в него
            DeliveryBucket(
                bucket_id=311,
                fesh=1,
                carriers=[103],
                regional_options=[
                    RegionalDelivery(
                        rid=311, options=[DeliveryOption(price=5, day_from=1, day_to=5, shop_delivery_price=10)]
                    ),
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=320,
                dc_bucket_id=320,
                fesh=2,
                carriers=[123],
                options=[PickupOption(outlet_id=322, day_from=1, day_to=2, price=5)],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            PickupBucket(
                bucket_id=330,
                dc_bucket_id=330,
                fesh=2,
                carriers=[123],
                options=[PickupOption(outlet_id=332, day_from=1, day_to=2, price=5)],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
        ]

    def test_region_types(self):
        """
        Проверяем типы регионов, попавших в выдачу ближайших городов. Это должны быть города и деревни
        Для курьерской доставки берутся все города и деревни (если не сказано обратного), которые есть под регионом
        Для ПВЗ и почты берутся все города и деревни над регионом ПВЗ.
        """
        request = 'place=check_delivery_available&rids={}&numdoc=100'
        for flag, nordstream_enabled in NORDSTREAM_FLAGS:
            response = self.report.request_json(request.format(299) + flag)
            region_list = [
                # 300 - Запрещен по типу региона
                # 301 - Запрещен в бакете
                302,
                303,  # Являются дочерними городами в регионе с доставкой
                304,  # Является дочерней деревней в регионе с доставкой
                # 305, 306, 307, 308, 309 - Запрещены по типу региона
                # 310 - Запрещен по типу региона
                311,
                312,
                313,  # Являются дочерними городами в регионе с доставкой
                # 320 - Запрещен по типу региона
                321,  # Является родительским регионом для региона с ПВЗ
                # 322 - Запрещен по типу региона
                # 330 - Запрещен по типу региона
                331,  # Является родительским регионом для региона с почтой
                # 332 - Запрещен по типу региона
            ]
            if nordstream_enabled:
                region_list = region_list[:-2]  # пока не поддержан самовывоз
                region_list.append(301)  # и нет выкалывания регионов
            self.assertFragmentIn(
                response, [{"region": {"id": region_id}} for region_id in region_list], allow_different_len=False
            )

    def test_delivery_types(self):
        """
        Проверяем способы оплаты, доступные в регионах
        """

        def generate_rid_info(rid, courier, pickup, post):
            return {
                "region": {"id": rid},
                "courierAvailable": courier,
                "pickupAvailable": pickup,
                "postAvailable": post,
            }

        request = 'place=check_delivery_available&rids={}&numdoc=100'
        for flag, nordstream_enabled in NORDSTREAM_FLAGS:
            response = self.report.request_json(request.format(299) + flag)
            # courier
            for rid in (302, 303, 304, 311, 312, 313):
                self.assertFragmentIn(response, generate_rid_info(rid, True, False, False))

            if not nordstream_enabled:
                # pickup
                self.assertFragmentIn(response, generate_rid_info(321, False, True, False))
                # post
                self.assertFragmentIn(response, generate_rid_info(331, False, False, True))

    @classmethod
    def prepare_nearest_regions_page(cls):
        default_latitude = -50.0
        default_longitude = -50.2
        current_distance_in_gradus = 1.0

        regions = []
        for rid in range(401, 440):
            regions += [
                Region(
                    rid=rid,
                    latitude=default_latitude,
                    longitude=default_longitude + current_distance_in_gradus,
                )
            ]
            current_distance_in_gradus += 1.0

        cls.index.regiontree += [
            Region(
                rid=400,
                latitude=default_latitude,
                longitude=default_longitude,
                region_type=Region.FEDERAL_DISTRICT,
                children=regions,
            ),
            Region(
                rid=450,
                latitude=default_latitude,
                longitude=default_longitude,
            ),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=400,
                fesh=1,
                carriers=[103],
                regional_options=[
                    RegionalDelivery(
                        rid=400, options=[DeliveryOption(price=5, day_from=1, day_to=5, shop_delivery_price=10)]
                    ),
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
        ]

    def test_nearest_regions_page(self):
        """
        Проверяем вывод регионов на нескольких страницах
        Так же проверяем, что регионы выводятся в нужном порядке
        """
        for flag, _ in NORDSTREAM_FLAGS:
            request = 'place=check_delivery_available&rids=450' + flag

            # По умолчанию отдается первые 10 городов
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                [{"region": {"id": near_id}} for near_id in range(401, 411)],
                allow_different_len=False,
                preserve_order=True,
            )

            # Вторая страница
            response = self.report.request_json(request + "&page=2")
            self.assertFragmentIn(
                response,
                [{"region": {"id": near_id}} for near_id in range(411, 421)],
                allow_different_len=False,
                preserve_order=True,
            )

            # Меньше документов на странице
            response = self.report.request_json(request + "&numdoc=7")
            self.assertFragmentIn(
                response,
                [{"region": {"id": near_id}} for near_id in range(401, 408)],
                allow_different_len=False,
                preserve_order=True,
            )

            # Больше документов на странице
            response = self.report.request_json(request + "&numdoc=21")
            self.assertFragmentIn(
                response,
                [{"region": {"id": near_id}} for near_id in range(401, 422)],
                allow_different_len=False,
                preserve_order=True,
            )

            # Сколько документов попадет на страницу?
            # 28 штук. 28* на широте 50 - это больше 2000км (абсолютный предел)
            response = self.report.request_json(request + "&numdoc=100")
            self.assertFragmentIn(
                response,
                [{"region": {"id": near_id}} for near_id in range(401, 429)],
                allow_different_len=False,
                preserve_order=True,
            )

            # Очень большая страница
            response = self.report.request_json(request + "&numdoc=10&page=4")
            self.assertFragmentIn(response, {"nearestRegions": Absent()})

    @classmethod
    def prepare_output_region_info(cls):
        cls.index.regiontree += [
            Region(
                rid=7000,
                region_type=Region.COUNTRY,
                name="Эфиопия",
                children=[
                    Region(
                        rid=7001,
                        region_type=Region.FEDERATIVE_SUBJECT,
                        name="Столичный округ",
                        children=[
                            Region(
                                rid=7002,
                                region_type=Region.SUBJECT_FEDERATION_DISTRICT,
                                name="Шляпинский район",
                                children=[
                                    Region(
                                        rid=7003,
                                        region_type=Region.SETTLEMENT,
                                        name="Шляпинское поселение",
                                        children=[
                                            Region(rid=7004, region_type=Region.VILLAGE, name="Шляпино"),
                                        ],
                                    ),
                                ],
                            ),
                            Region(
                                rid=7005,
                                region_type=Region.CITY,
                                name="Володяград",
                                children=[
                                    Region(rid=7006, region_type=Region.CITY_DISTRICT, name="Ленинский р-н"),
                                    Region(rid=7007, region_type=Region.METRO_STATION, name="ст. Единственная"),
                                ],
                            ),
                        ],
                    ),
                ],
            ),
            # Регионы, добавляемые в Россию (см. core/type/region.py)
            Region(
                rid=7101,
                region_type=Region.FEDERATIVE_SUBJECT,
                name="Северная область",
                children=[
                    Region(
                        rid=7102,
                        region_type=Region.SUBJECT_FEDERATION_DISTRICT,
                        name="Район",
                        children=[
                            Region(
                                rid=7103,
                                region_type=Region.SETTLEMENT,
                                name="Село",
                                children=[
                                    Region(rid=7104, region_type=Region.VILLAGE, name="Деревня"),
                                ],
                            ),
                        ],
                    ),
                    Region(
                        rid=7105,
                        region_type=Region.CITY,
                        name="Город",
                        children=[
                            Region(rid=7106, region_type=Region.CITY_DISTRICT, name="Район Города"),
                        ],
                    ),
                    Region(rid=7107, region_type=Region.CITY, name="Город Без МарДо"),
                ],
            ),
        ]
        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=401,
                dc_bucket_id=401,
                fesh=1,
                carriers=[103],
                regional_options=[
                    RegionalDelivery(
                        rid=7107, options=[DeliveryOption(price=5, day_from=1, day_to=5, shop_delivery_price=10)]
                    ),
                ],
                delivery_program=DeliveryBucket.BERU_CROSSDOCK,
            ),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=402,
                dc_bucket_id=402,
                fesh=1,
                carriers=[103],
                options=[PickupOption(outlet_id=15, day_from=1, day_to=2, price=5)],
                delivery_program=DeliveryBucket.BERU_CROSSDOCK,
            ),
        ]

        cls.delivery_calc.on_request_offer_buckets(**T.FakeOfferDimensions).respond([401], [402], [])

    def test_output_current_region(self):
        """
        Проверяем, что на выдаче place=check_delivery_available есть информация о запрошенном регионе
        """

        def check_region(rid, name, subtitle, region_type=None):
            for flag, _ in NORDSTREAM_FLAGS:
                response = self.report.request_json('place=check_delivery_available&rids={}{}'.format(rid, flag))
                self.assertFragmentIn(
                    response,
                    {
                        "deliveryAvailable": False,
                        "region": {
                            "id": rid,
                            "name": name,
                            "subtitle": subtitle,
                            "type": region_type if region_type else Absent(),
                        },
                    },
                )

        check_region(7004, "Шляпино", "Шляпинский район, Столичный округ, Эфиопия", Region.VILLAGE)
        check_region(7007, "ст. Единственная", "Володяград, Столичный округ, Эфиопия", Region.METRO_STATION)
        check_region(7104, "Деревня", "Район, Северная область, Россия", Region.VILLAGE)
        check_region(7106, "Район Города", "Город, Северная область, Россия", Region.CITY_DISTRICT)

    def test_region_type_in_nearest(self):
        """
        Проверяем наличие типа у ближайших регионов
        """
        for flag, _ in NORDSTREAM_FLAGS:
            response = self.report.request_json('place=check_delivery_available&rids=450' + flag)
            self.assertFragmentIn(response, {"nearestRegions": [{"region": {"id": 401, "type": 6}}]})

    def test_region_external_buckets(self):
        """
        Проверяем что доставляемость check_delivery_available
        совпадает с доставляемостью с actual_delivery
        """

        req = (
            'place=actual_delivery&pickup-options=grouped&rgb=blue&offers-list=FakeOffer_g:1;w:{weight};d:{width}x{height}x{length};wh:145;ff:1&rids=7107'.format(
                **T.FakeOfferDimensions
            )
            + '&rearr-factors=disable_delivery_calculator_call_for_blue_offers=0'
        )
        response = self.report.request_json(req)
        self.assertFragmentIn(
            response,
            {
                "delivery": {
                    "isAvailable": True,
                    "hasPickup": True,
                }
            },
        )
        response = self.report.request_json('place=check_delivery_available&rids=7107')
        self.assertFragmentIn(
            response,
            {
                "deliveryAvailable": True,
                "region": {
                    "id": 7107,
                },
            },
        )


if __name__ == '__main__':
    main()
