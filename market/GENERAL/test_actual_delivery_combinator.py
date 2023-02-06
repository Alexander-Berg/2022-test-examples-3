#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

import datetime

from core.logs import ErrorCodes
from core.matcher import (
    Absent,
    NoKey,
    NotEmpty,
    NotEmptyList,
)
from core.testcase import (
    TestCase,
    main,
)
from core.types.combinator import (
    CombinatorOffer,
    create_delivery_option,
    create_virtual_box,
    DeliveryItem,
    Destination,
    PickupPointGrouped,
    PointTimeInterval,
)
from core.types.currency import Currency
from core.types.delivery import (
    BlueDeliveryTariff,
    OutletDeliveryOption,
    OutletType,
)
from core.types.dynamic_filters import (
    DynamicWarehouseInfo,
)
from core.types.offer import OfferDimensions
from core.types.region import (
    GpsCoord,
    Region,
)
from core.types.shop import (
    Shop,
    Outlet,
)
from core.types.sku import (
    BlueOffer,
    MarketSku,
)
from core.types.taxes import (
    Tax,
    Vat,
)
from market.pylibrary.const.payment_methods import (
    PaymentMethod,
)

from unittest import skip


# This date is fixed for all test
TODAY = datetime.date(2020, 5, 18)
DATE_FROM = TODAY + datetime.timedelta(days=2)
DATE_TO = TODAY + datetime.timedelta(days=4)
DATE_FROM_MANY = TODAY + datetime.timedelta(days=3)
DATE_TO_MANY = TODAY + datetime.timedelta(days=3)
DATE_FROM_TWO = TODAY + datetime.timedelta(days=1)
DATE_TO_TWO = TODAY + datetime.timedelta(days=5)

VIRTUAL_BOX = create_virtual_box(weight=18000, length=100, width=80, height=50)

# Regions
MOSCOW_REGION_RIDS = 1
SPB_RIDS = 2
CFD_RIDS = 3
NWFD_RIDS = 17
VLADIMIR_RIDS = 192
MOSCOW_RIDS = 213
RUSSIA_RIDS = 225
KHIMKI_RIDS = 10758
MOSCOW_REGION_SUB_RIDS = 322  # fictional id
KHIMKI_SUB_RIDS = 10760  # fictional id
NO_COMBINATOR_DELIVERY_REGION = 9000
COMBINATOR_UNKNOWN_REGION = 9001

PARENT_RIDS = 100
CHILD_RIDS = 101

FIRST_OFFER_PRICE = 50
SECOND_OFFER_PRICE = 57
BRANDED_OUTLET_OFFER_PRICE = 45


class T(TestCase):
    @classmethod
    def beforePrepare(cls):
        cls.settings.delivery_calendar_start_date = datetime.date(day=18, month=5, year=2020)

    @classmethod
    def prepare(cls):
        cls.settings.check_combinator_errors = True
        cls.settings.report_subrole = 'blue-main'
        cls.settings.blue_market_free_delivery_threshold = 67
        # НЕ делайте так в новых тестах!
        # Походов в КД на проде уже нет, пожалуйста, проверяйте новую функциональность, создавая доставку через комбинатор
        cls.settings.default_search_experiment_flags += ['force_disable_delivery_calculator_requests=0']
        cls.settings.default_search_experiment_flags += ['enable_dsbs_combinator_request_in_actual_delivery=0']
        cls.index.regiontree += [
            Region(
                rid=CFD_RIDS,
                name="Центральный федеральный округ",
                region_type=Region.FEDERAL_DISTRICT,
                children=[
                    Region(
                        rid=MOSCOW_REGION_RIDS,
                        name="Московская область",
                        region_type=Region.FEDERATIVE_SUBJECT,
                        children=[
                            Region(
                                rid=MOSCOW_RIDS,
                                name="Москва",
                            ),
                            Region(
                                rid=KHIMKI_RIDS,
                                name="Химки",
                                children=[Region(rid=KHIMKI_SUB_RIDS, name="Химковский подрегион")],
                            ),
                            Region(
                                rid=MOSCOW_REGION_SUB_RIDS,
                                name="Подрегион Московской области",
                            ),
                        ],
                    ),
                    Region(rid=VLADIMIR_RIDS, name="Владимир"),
                    Region(
                        rid=NO_COMBINATOR_DELIVERY_REGION,
                        name="no delivery with combinator",
                        region_type=Region.SETTLEMENT,
                    ),
                    Region(rid=COMBINATOR_UNKNOWN_REGION, name="yayaya", region_type=Region.KAZAN),
                ],
            ),
            Region(
                rid=NWFD_RIDS,
                name="Северо-Западный федеральный округ",
                region_type=Region.FEDERATIVE_SUBJECT,
                children=[Region(rid=SPB_RIDS, name="Санкт-Петербург")],
            ),
        ]
        # Current date 18/05/2020 @ 23:16 MSK
        cls.settings.microseconds_for_disabled_random = 1589833013000000
        cls.index.shops += [
            Shop(
                fesh=1,
                datafeed_id=1,
                priority_region=MOSCOW_RIDS,
                name='virtual_shop',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                fulfillment_virtual=True,
                cpa=Shop.CPA_REAL,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                delivery_service_outlets=[
                    2001,
                    2002,
                    2004,
                    3001,
                    4001,
                    4002,
                    4101,
                    4102,
                    4401,
                ],
            ),
            Shop(
                fesh=4,
                datafeed_id=4,
                priority_region=MOSCOW_RIDS,
                name='blue_shop_2',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.THIRD_PARTY,
                blue='REAL',
                warehouse_id=145,
            ),
        ]
        cls.settings.lms_autogenerate = False
        cls.dynamic.lms += [DynamicWarehouseInfo(id=145, home_region=MOSCOW_RIDS, holidays_days_set_key=1)]

        cls.index.outlets += [
            Outlet(
                point_id=2001,
                delivery_service_id=103,
                region=MOSCOW_RIDS,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    shipper_id=103,
                    day_from=1,
                    day_to=1,
                    order_before=2,
                    work_in_holiday=True,
                    price=100,
                ),
                working_days=[i for i in range(10)],
                bool_props=["prepayAllowed", "cashAllowed", "cardAllowed", "isMarketBranded"],
                gps_coord=GpsCoord(37.12, 55.32),
            ),
            Outlet(
                point_id=2002,
                delivery_service_id=103,
                region=MOSCOW_RIDS,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    shipper_id=103,
                    day_from=2,
                    day_to=2,
                    order_before=2,
                    work_in_holiday=True,
                    price=100,
                ),
                working_days=[i for i in range(10)],
                bool_props=["prepayAllowed", "cashAllowed", "cardAllowed", "isMarketPartner"],
                gps_coord=GpsCoord(37.12, 55.32),
            ),
            Outlet(
                point_id=2003,
                delivery_service_id=103,
                region=MOSCOW_RIDS,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    shipper_id=103,
                    day_from=2,
                    day_to=2,
                    order_before=2,
                    work_in_holiday=True,
                    price=100,
                ),
                working_days=[i for i in range(10)],
                bool_props=["prepayAllowed", "cashAllowed", "cardAllowed", "isMarketPostTerm"],
                gps_coord=GpsCoord(37.15, 55.35),
            ),
            Outlet(
                point_id=2004,
                delivery_service_id=123,
                region=MOSCOW_RIDS,
                point_type=Outlet.FOR_POST_TERM,
                delivery_option=OutletDeliveryOption(
                    shipper_id=123,
                    day_from=1,
                    day_to=1,
                    price=400,
                ),
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(37.7, 55.7),
            ),
            Outlet(
                point_id=4001,
                delivery_service_id=201,
                region=MOSCOW_RIDS,
                point_type=Outlet.FOR_POST,
                post_code=115200,
                delivery_option=OutletDeliveryOption(
                    shipper_id=201,
                    day_from=2,
                    day_to=4,
                    price=400,
                ),
                working_days=[i for i in range(30)],
                bool_props=["prepayAllowed", "cashAllowed", "cardAllowed"],
                gps_coord=GpsCoord(67.7, 55.7),
            ),
            Outlet(
                point_id=4002,
                delivery_service_id=201,
                region=MOSCOW_RIDS,
                point_type=Outlet.FOR_POST,
                post_code=115201,
                delivery_option=OutletDeliveryOption(
                    shipper_id=201,
                    day_from=2,
                    day_to=4,
                    price=400,
                ),
                working_days=[i for i in range(30)],
                bool_props=["prepayAllowed", "cashAllowed", "cardAllowed"],
                gps_coord=GpsCoord(67.7, 55.7),
            ),
            Outlet(
                point_id=4101,
                delivery_service_id=202,
                region=MOSCOW_RIDS,
                point_type=Outlet.FOR_POST,
                post_code=115200,
                delivery_option=OutletDeliveryOption(
                    shipper_id=202,
                    day_from=2,
                    day_to=4,
                    price=400,
                ),
                working_days=[i for i in range(30)],
                bool_props=["prepayAllowed", "cashAllowed", "cardAllowed"],
                gps_coord=GpsCoord(67.7, 55.8),
            ),
            Outlet(
                point_id=4102,
                delivery_service_id=202,
                region=MOSCOW_RIDS,
                point_type=Outlet.FOR_POST,
                post_code=115202,
                delivery_option=OutletDeliveryOption(
                    shipper_id=202,
                    day_from=3,
                    day_to=5,
                    price=400,
                ),
                working_days=[i for i in range(30)],
                bool_props=["prepayAllowed", "cashAllowed", "cardAllowed"],
                gps_coord=GpsCoord(67.7, 55.8),
            ),
            Outlet(
                point_id=4401,
                delivery_service_id=203,
                region=MOSCOW_RIDS,
                point_type=Outlet.FOR_POST,
                post_code=115201,
                delivery_option=OutletDeliveryOption(
                    shipper_id=203,
                    day_from=2,
                    day_to=4,
                    price=401,
                ),
                working_days=[i for i in range(30)],
                bool_props=["prepayAllowed", "cashAllowed", "cardAllowed"],
                gps_coord=GpsCoord(67.7, 55.8),
            ),
        ]

        cls.settings.loyalty_enabled = True

    @classmethod
    def prepare_blue_delivery_modifiers(cls):
        '''
        Подготавливаем параметры для изменения цены доставки для пользователя
        '''
        TARIFFS = [
            BlueDeliveryTariff(user_price=99, courier_price=77, price_to=67),
            BlueDeliveryTariff(
                user_price=0,
                courier_price=0,
                # Опции доставки в ПВЗ и почту не заданы и будут иметь цену user_price
            ),
        ]
        AVIA_TARIFF = [
            BlueDeliveryTariff(user_price=150, for_plus=1),
            BlueDeliveryTariff(user_price=200, for_plus=0),
        ]
        cls.index.blue_delivery_modifiers.add_modifier(
            tariffs=TARIFFS,
            regions=[MOSCOW_RIDS],
            large_size_weight=70,
        )
        cls.index.blue_delivery_modifiers.set_default_modifier(
            tariffs=TARIFFS,
            large_size_weight=70,
        )
        cls.index.blue_delivery_modifiers.set_default_modifier(
            tariffs=AVIA_TARIFF,
            is_avia=True,
        )

    @classmethod
    def prepare_courier_options(cls):
        for rids in [MOSCOW_RIDS, SPB_RIDS, VLADIMIR_RIDS, KHIMKI_RIDS]:
            cls.combinator.on_courier_options_request(
                items=[
                    DeliveryItem(
                        required_count=1,
                        weight=11000,
                        dimensions=[175, 357, 543],
                        cargo_types=[200],
                        offers=[
                            CombinatorOffer(
                                shop_sku="blue.offer.single1",
                                shop_id=4,
                                partner_id=145,
                                available_count=1,
                            )
                        ],
                        price=FIRST_OFFER_PRICE,
                    ),
                ],
                destination=Destination(region_id=rids),
                payment_methods=[],
                total_price=FIRST_OFFER_PRICE,
            ).respond_with_courier_options(
                options=[
                    create_delivery_option(
                        cost=5,
                        date_from=DATE_FROM,
                        date_to=DATE_TO,
                        time_from=datetime.time(10, 0),
                        time_to=datetime.time(22, 0),
                        delivery_service_id=123,
                        payment_methods=[
                            PaymentMethod.PT_YANDEX,
                            PaymentMethod.PT_CARD_ON_DELIVERY,
                        ],
                        leave_at_the_door=True,
                        do_not_call=True,
                        airship_delivery=True,
                        customizers=[
                            {
                                "key": "leave_at_the_door",
                                "name": "Оставить у двери",
                                "type": "boolean",
                            },
                            {
                                "key": "not_call",
                                "name": "Не звонить",
                                "type": "boolean",
                            },
                        ],
                    )
                ],
                virtual_box=VIRTUAL_BOX,
            )

        cls.combinator.on_courier_options_request(
            items=[
                DeliveryItem(
                    required_count=2,
                    weight=11000,
                    dimensions=[175, 357, 543],
                    cargo_types=[200],
                    offers=[
                        CombinatorOffer(
                            shop_sku="blue.offer.single1",
                            shop_id=4,
                            partner_id=145,
                            available_count=2,
                        )
                    ],
                    price=FIRST_OFFER_PRICE,
                ),
            ],
            destination=Destination(region_id=MOSCOW_RIDS),
            payment_methods=[],
            total_price=FIRST_OFFER_PRICE * 2,
        ).respond_with_courier_options(
            options=[
                create_delivery_option(
                    cost=10,
                    date_from=DATE_FROM_MANY,
                    date_to=DATE_TO_MANY,
                    time_from=datetime.time(6, 0),
                    time_to=datetime.time(19, 0),
                    delivery_service_id=139,
                    payment_methods=[
                        PaymentMethod.PT_YANDEX,
                        PaymentMethod.PT_CARD_ON_DELIVERY,
                    ],
                    leave_at_the_door=True,
                    do_not_call=True,
                    airship_delivery=True,
                    customizers=[
                        {
                            "key": "leave_at_the_door",
                            "name": "Оставить у двери",
                            "type": "boolean",
                        },
                        {
                            "key": "not_call",
                            "name": "Не звонить",
                            "type": "boolean",
                        },
                    ],
                )
            ],
            virtual_box=VIRTUAL_BOX,
        )

        cls.combinator.on_courier_options_request(
            items=[
                DeliveryItem(
                    required_count=1,
                    weight=11000,
                    dimensions=[175, 357, 543],
                    cargo_types=[200],
                    offers=[
                        CombinatorOffer(
                            shop_sku="blue.offer.single1",
                            shop_id=4,
                            partner_id=145,
                            available_count=1,
                        ),
                    ],
                    price=FIRST_OFFER_PRICE,
                ),
                DeliveryItem(
                    required_count=1,
                    weight=7000,
                    dimensions=[52, 77, 113],  # note here values should be sorted!
                    cargo_types=[],
                    offers=[
                        CombinatorOffer(
                            shop_sku="blue.offer.second2",
                            shop_id=4,
                            partner_id=145,
                            available_count=1,
                        ),
                    ],
                    price=SECOND_OFFER_PRICE,
                ),
            ],
            destination=Destination(region_id=MOSCOW_RIDS),
            payment_methods=[],
            total_price=FIRST_OFFER_PRICE + SECOND_OFFER_PRICE,
        ).respond_with_courier_options(
            options=[
                create_delivery_option(
                    cost=15,
                    date_from=DATE_FROM_TWO,
                    date_to=DATE_TO_TWO,
                    time_from=datetime.time(8, 0),
                    time_to=datetime.time(21, 0),
                    delivery_service_id=140,
                    payment_methods=[
                        PaymentMethod.PT_YANDEX,
                        PaymentMethod.PT_CARD_ON_DELIVERY,
                    ],
                    leave_at_the_door=True,
                    do_not_call=True,
                    customizers=[
                        {
                            "key": "leave_at_the_door",
                            "name": "Оставить у двери",
                            "type": "boolean",
                        },
                        {
                            "key": "not_call",
                            "name": "Не звонить",
                            "type": "boolean",
                        },
                    ],
                )
            ],
            virtual_box=VIRTUAL_BOX,
        )

        cls.combinator.on_courier_options_request(
            items=[
                DeliveryItem(
                    required_count=1,
                    weight=11000,
                    dimensions=[175, 357, 543],
                    cargo_types=[200],
                    offers=[
                        CombinatorOffer(shop_sku="blue.offer.single1", shop_id=4, partner_id=145, available_count=1)
                    ],
                    price=FIRST_OFFER_PRICE,
                ),
            ],
            destination=Destination(region_id=NO_COMBINATOR_DELIVERY_REGION),
            payment_methods=[],
            total_price=FIRST_OFFER_PRICE,
        ).respond_with_courier_options(
            options=[],
            virtual_box=VIRTUAL_BOX,
        )

        cls.combinator.on_courier_options_request(
            items=[
                DeliveryItem(
                    required_count=1,
                    weight=7000,
                    dimensions=[52, 77, 113],
                    cargo_types=[],
                    offers=[
                        CombinatorOffer(
                            shop_sku="blue.offer.branded.outlet",
                            shop_id=4,
                            partner_id=145,
                            available_count=1,
                        ),
                    ],
                    price=BRANDED_OUTLET_OFFER_PRICE,
                ),
            ],
            destination=Destination(region_id=MOSCOW_RIDS),
            payment_methods=[],
            total_price=BRANDED_OUTLET_OFFER_PRICE,
        ).respond_with_courier_options(
            options=[
                create_delivery_option(
                    cost=50,
                    date_from=DATE_FROM,
                    date_to=DATE_TO,
                    time_from=datetime.time(6, 0),
                    time_to=datetime.time(19, 0),
                    delivery_service_id=199,
                    payment_methods=[
                        PaymentMethod.PT_YANDEX,
                        PaymentMethod.PT_CARD_ON_DELIVERY,
                    ],
                    trying_available=True,
                )
            ],
            virtual_box=VIRTUAL_BOX,
        )
        cls.combinator.set_error_on_unknown_region(True)

    @classmethod
    def prepare_pickup_options(cls):
        cls.combinator.on_pickup_points_grouped_request(
            items=[
                DeliveryItem(
                    required_count=1,
                    weight=11000,
                    dimensions=[175, 357, 543],
                    cargo_types=[200],
                    offers=[
                        CombinatorOffer(
                            shop_sku="blue.offer.single1",
                            shop_id=4,
                            partner_id=145,
                            available_count=1,
                        )
                    ],
                    price=FIRST_OFFER_PRICE,
                ),
            ],
            destination_regions=[MOSCOW_RIDS, MOSCOW_REGION_SUB_RIDS, KHIMKI_SUB_RIDS, KHIMKI_RIDS],
            point_types=[],
            total_price=FIRST_OFFER_PRICE,
        ).respond_with_grouped_pickup_points(
            groups=[
                PickupPointGrouped(
                    ids_list=[1],
                    outlet_type=OutletType.FOR_PICKUP,
                    service_id=322,
                    cost=100,
                    date_from=DATE_FROM,
                    date_to=DATE_FROM,
                    payment_methods=[PaymentMethod.PT_CASH_ON_DELIVERY],
                    delivery_intervals=[
                        PointTimeInterval(point_id=1, time_from=datetime.time(10, 0), time_to=datetime.time(22, 0)),
                    ],
                ),
                PickupPointGrouped(
                    ids_list=[2],
                    outlet_type=OutletType.FOR_PICKUP,
                    service_id=323,
                    cost=101,
                    date_from=DATE_FROM,
                    date_to=DATE_FROM,
                    payment_methods=[PaymentMethod.PT_YANDEX],
                ),
                PickupPointGrouped(
                    ids_list=[3],
                    post_ids=[115203],
                    outlet_type=OutletType.FOR_POST,
                    service_id=323,
                    cost=101,
                    date_from=DATE_FROM,
                    date_to=DATE_FROM,
                    payment_methods=[PaymentMethod.PT_ALL],
                ),
                PickupPointGrouped(
                    ids_list=[4],
                    post_ids=[115203],
                    outlet_type=OutletType.FOR_POST,
                    service_id=323,
                    cost=101,
                    date_from=DATE_FROM,
                    date_to=DATE_FROM,
                    payment_methods=[PaymentMethod.PT_ALL],
                ),
            ],
            virtual_box=VIRTUAL_BOX,
        )

        cls.combinator.on_pickup_points_grouped_request(
            items=[
                DeliveryItem(
                    required_count=1,
                    weight=11000,
                    dimensions=[175, 357, 543],
                    cargo_types=[200],
                    offers=[
                        CombinatorOffer(
                            shop_sku="blue.offer.single1",
                            shop_id=4,
                            partner_id=145,
                            available_count=1,
                        )
                    ],
                    price=FIRST_OFFER_PRICE,
                ),
            ],
            destination_regions=[SPB_RIDS],
            point_types=[],
            total_price=FIRST_OFFER_PRICE,
        ).respond_with_grouped_pickup_points(
            groups=[
                PickupPointGrouped(
                    ids_list=[1],
                    outlet_type=OutletType.FOR_PICKUP,
                    service_id=322,
                    cost=100,
                    date_from=DATE_FROM + datetime.timedelta(days=1),
                    date_to=DATE_FROM + datetime.timedelta(days=1),
                    payment_methods=[PaymentMethod.PT_CASH_ON_DELIVERY],
                ),
                PickupPointGrouped(
                    ids_list=[2],
                    outlet_type=OutletType.FOR_PICKUP,
                    service_id=323,
                    cost=101,
                    date_from=DATE_FROM + datetime.timedelta(days=1),
                    date_to=DATE_FROM + datetime.timedelta(days=1),
                    payment_methods=[PaymentMethod.PT_YANDEX],
                ),
                PickupPointGrouped(
                    ids_list=[3],
                    post_ids=[115203],
                    outlet_type=OutletType.FOR_POST,
                    service_id=323,
                    cost=101,
                    date_from=DATE_FROM + datetime.timedelta(days=1),
                    date_to=DATE_FROM + datetime.timedelta(days=1),
                    payment_methods=[PaymentMethod.PT_ALL],
                ),
                PickupPointGrouped(
                    ids_list=[4],
                    post_ids=[115203],
                    outlet_type=OutletType.FOR_POST,
                    service_id=323,
                    cost=101,
                    date_from=DATE_FROM + datetime.timedelta(days=1),
                    date_to=DATE_FROM + datetime.timedelta(days=1),
                    payment_methods=[PaymentMethod.PT_ALL],
                ),
            ],
            virtual_box=VIRTUAL_BOX,
        )

        cls.combinator.on_pickup_points_grouped_request(
            items=[
                DeliveryItem(
                    required_count=1,
                    weight=11000,
                    dimensions=[175, 357, 543],
                    cargo_types=[200],
                    offers=[
                        CombinatorOffer(
                            shop_sku="blue.offer.single1",
                            shop_id=4,
                            partner_id=145,
                            available_count=1,
                        )
                    ],
                    price=FIRST_OFFER_PRICE,
                ),
            ],
            destination_regions=[SPB_RIDS],
            point_types=[],
            post_codes=[115203],
            total_price=FIRST_OFFER_PRICE,
        ).respond_with_grouped_pickup_points(
            groups=[
                PickupPointGrouped(
                    ids_list=[1],
                    outlet_type=OutletType.FOR_PICKUP,
                    service_id=322,
                    cost=100,
                    date_from=DATE_FROM + datetime.timedelta(days=1),
                    date_to=DATE_FROM + datetime.timedelta(days=1),
                    payment_methods=[PaymentMethod.PT_CASH_ON_DELIVERY],
                ),
                PickupPointGrouped(
                    ids_list=[2],
                    outlet_type=OutletType.FOR_PICKUP,
                    service_id=323,
                    cost=101,
                    date_from=DATE_FROM + datetime.timedelta(days=1),
                    date_to=DATE_FROM + datetime.timedelta(days=1),
                    payment_methods=[PaymentMethod.PT_YANDEX],
                ),
                PickupPointGrouped(
                    ids_list=[3],
                    post_ids=[115203],
                    outlet_type=OutletType.FOR_POST,
                    service_id=323,
                    cost=101,
                    date_from=DATE_FROM + datetime.timedelta(days=1),
                    date_to=DATE_FROM + datetime.timedelta(days=1),
                    payment_methods=[PaymentMethod.PT_ALL],
                ),
                PickupPointGrouped(
                    ids_list=[4],
                    post_ids=[115203],
                    outlet_type=OutletType.FOR_POST,
                    service_id=323,
                    cost=101,
                    date_from=DATE_FROM + datetime.timedelta(days=1),
                    date_to=DATE_FROM + datetime.timedelta(days=1),
                    payment_methods=[PaymentMethod.PT_ALL],
                ),
            ],
            virtual_box=VIRTUAL_BOX,
        )

        cls.combinator.on_pickup_points_grouped_request(
            items=[
                DeliveryItem(
                    required_count=1,
                    weight=11000,
                    dimensions=[175, 357, 543],
                    cargo_types=[200],
                    offers=[
                        CombinatorOffer(
                            shop_sku="blue.offer.single1",
                            shop_id=4,
                            partner_id=145,
                            available_count=1,
                        )
                    ],
                    price=FIRST_OFFER_PRICE,
                ),
            ],
            destination_regions=[MOSCOW_RIDS, MOSCOW_REGION_SUB_RIDS, KHIMKI_SUB_RIDS, KHIMKI_RIDS],
            point_types=[],
            post_codes=[115201],
            total_price=FIRST_OFFER_PRICE,
        ).respond_with_grouped_pickup_points(
            groups=[
                PickupPointGrouped(
                    ids_list=[1],
                    outlet_type=OutletType.FOR_PICKUP,
                    service_id=322,
                    cost=100,
                    date_from=DATE_FROM,
                    date_to=DATE_FROM,
                    payment_methods=[PaymentMethod.PT_CASH_ON_DELIVERY],
                ),
                PickupPointGrouped(
                    ids_list=[2],
                    outlet_type=OutletType.FOR_PICKUP,
                    service_id=323,
                    cost=101,
                    date_from=DATE_FROM,
                    date_to=DATE_FROM,
                    payment_methods=[PaymentMethod.PT_YANDEX],
                ),
                PickupPointGrouped(
                    ids_list=[3],
                    post_ids=[115201],
                    outlet_type=OutletType.FOR_POST,
                    service_id=323,
                    cost=101,
                    date_from=DATE_FROM,
                    date_to=DATE_FROM,
                    payment_methods=[PaymentMethod.PT_ALL],
                ),
            ],
            virtual_box=VIRTUAL_BOX,
        )

        cls.combinator.on_pickup_points_grouped_request(
            items=[
                DeliveryItem(
                    required_count=2,
                    weight=11000,
                    dimensions=[175, 357, 543],
                    cargo_types=[200],
                    offers=[
                        CombinatorOffer(
                            shop_sku="blue.offer.single1",
                            shop_id=4,
                            partner_id=145,
                            available_count=2,
                        )
                    ],
                    price=FIRST_OFFER_PRICE,
                ),
            ],
            destination_regions=[MOSCOW_RIDS, MOSCOW_REGION_SUB_RIDS, KHIMKI_SUB_RIDS, KHIMKI_RIDS],
            point_types=[],
            total_price=FIRST_OFFER_PRICE * 2,
        ).respond_with_grouped_pickup_points(
            groups=[
                PickupPointGrouped(
                    ids_list=[4, 5],
                    outlet_type=OutletType.FOR_PICKUP,
                    service_id=324,
                    cost=102,
                    date_from=DATE_FROM,
                    date_to=DATE_FROM,
                    payment_methods=[
                        PaymentMethod.PT_YANDEX,
                        PaymentMethod.PT_CASH_ON_DELIVERY,
                    ],
                ),
                PickupPointGrouped(
                    ids_list=[6],
                    outlet_type=OutletType.FOR_PICKUP,
                    service_id=324,
                    cost=103,
                    date_from=DATE_FROM_MANY,
                    date_to=DATE_FROM_MANY,
                    payment_methods=[
                        PaymentMethod.PT_YANDEX,
                        PaymentMethod.PT_CARD_ON_DELIVERY,
                    ],
                ),
                PickupPointGrouped(
                    ids_list=[7],
                    post_ids=[115203],
                    outlet_type=OutletType.FOR_POST,
                    service_id=325,
                    cost=104,
                    date_from=DATE_FROM_MANY,
                    date_to=DATE_FROM_MANY,
                    payment_methods=[
                        PaymentMethod.PT_CASH_ON_DELIVERY,
                        PaymentMethod.PT_CARD_ON_DELIVERY,
                    ],
                ),
            ],
            virtual_box=VIRTUAL_BOX,
        )

        cls.combinator.on_pickup_points_grouped_request(
            items=[
                DeliveryItem(
                    required_count=1,
                    weight=11000,
                    dimensions=[175, 357, 543],
                    cargo_types=[200],
                    offers=[
                        CombinatorOffer(
                            shop_sku="blue.offer.single1",
                            shop_id=4,
                            partner_id=145,
                            available_count=1,
                        )
                    ],
                    price=FIRST_OFFER_PRICE,
                ),
                DeliveryItem(
                    required_count=1,
                    weight=7000,
                    dimensions=[52, 77, 113],
                    cargo_types=[],
                    offers=[
                        CombinatorOffer(
                            shop_sku="blue.offer.second2",
                            shop_id=4,
                            partner_id=145,
                            available_count=1,
                        ),
                    ],
                    price=SECOND_OFFER_PRICE,
                ),
            ],
            destination_regions=[MOSCOW_RIDS, MOSCOW_REGION_SUB_RIDS, KHIMKI_SUB_RIDS, KHIMKI_RIDS],
            point_types=[],
            total_price=FIRST_OFFER_PRICE + SECOND_OFFER_PRICE,
        ).respond_with_grouped_pickup_points(
            groups=[
                PickupPointGrouped(
                    ids_list=[8],
                    outlet_type=OutletType.FOR_PICKUP,
                    service_id=325,
                    cost=102,
                    date_from=DATE_FROM_TWO,
                    date_to=DATE_FROM_TWO,
                    payment_methods=[
                        PaymentMethod.PT_YANDEX,
                        PaymentMethod.PT_CARD_ON_DELIVERY,
                    ],
                ),
                PickupPointGrouped(
                    ids_list=[2001],
                    outlet_type=OutletType.FOR_PICKUP,
                    service_id=103,
                    cost=102,
                    date_from=DATE_FROM_TWO,
                    date_to=DATE_FROM_TWO,
                    payment_methods=[
                        PaymentMethod.PT_YANDEX,
                        PaymentMethod.PT_CARD_ON_DELIVERY,
                    ],
                ),
                PickupPointGrouped(
                    ids_list=[9, 10, 11],
                    post_ids=[115203, 115203, 115203],
                    outlet_type=OutletType.FOR_POST,
                    service_id=325,
                    cost=102,
                    date_from=DATE_FROM_TWO,
                    date_to=DATE_FROM_TWO,
                    payment_methods=[
                        PaymentMethod.PT_YANDEX,
                        PaymentMethod.PT_CASH_ON_DELIVERY,
                    ],
                ),
                PickupPointGrouped(
                    ids_list=[12],
                    post_ids=[115203],
                    outlet_type=OutletType.FOR_POST,
                    service_id=325,
                    cost=102,
                    date_from=DATE_FROM_TWO,
                    date_to=DATE_FROM_TWO,
                    payment_methods=[
                        PaymentMethod.PT_YANDEX,
                        PaymentMethod.PT_CARD_ON_DELIVERY,
                    ],
                ),
            ],
            virtual_box=VIRTUAL_BOX,
        )

        cls.combinator.on_pickup_points_grouped_request(
            items=[
                DeliveryItem(
                    required_count=1,
                    weight=7000,
                    dimensions=[52, 77, 113],
                    cargo_types=[],
                    offers=[
                        CombinatorOffer(
                            shop_sku="blue.offer.branded.outlet",
                            shop_id=4,
                            partner_id=145,
                            available_count=1,
                        ),
                    ],
                    price=BRANDED_OUTLET_OFFER_PRICE,
                ),
            ],
            destination_regions=[MOSCOW_RIDS, MOSCOW_REGION_SUB_RIDS, KHIMKI_SUB_RIDS, KHIMKI_RIDS],
            point_types=[],
            total_price=BRANDED_OUTLET_OFFER_PRICE,
        ).respond_with_grouped_pickup_points(
            groups=[
                PickupPointGrouped(
                    ids_list=[2001, 2002, 2003],
                    outlet_type=OutletType.FOR_PICKUP,
                    service_id=103,
                    cost=100,
                    date_from=DATE_FROM,
                    date_to=DATE_FROM,
                    payment_methods=[PaymentMethod.PT_CASH_ON_DELIVERY],
                    delivery_intervals=[
                        PointTimeInterval(point_id=1, time_from=datetime.time(10, 0), time_to=datetime.time(22, 0)),
                    ],
                    trying_available=True,
                )
            ],
            virtual_box=VIRTUAL_BOX,
        )

    @classmethod
    def prepare_single_offer(cls):
        cls.index.mskus += [
            MarketSku(
                title="single_offer_sku",
                hyperid=1,
                sku=100,
                waremd5='SingleSku-sIiLVm1goleg',
                blue_offers=[
                    BlueOffer(
                        price=FIRST_OFFER_PRICE,
                        vat=Vat.NO_VAT,
                        feedid=4,
                        offerid='blue.offer.single1',
                        waremd5='SkuSingle50-iLVm1Goleg',
                        weight=11,
                        dimensions=OfferDimensions(length=543, width=175, height=357),
                        cargo_types=[200],
                    ),
                ],
                post_term_delivery=True,
            ),
            MarketSku(
                title="second_offer_sku",
                hyperid=1,
                sku=101,
                waremd5='SecondSku-sIiLVm1goleg',
                blue_offers=[
                    BlueOffer(
                        price=SECOND_OFFER_PRICE,
                        vat=Vat.NO_VAT,
                        feedid=4,
                        offerid='blue.offer.second2',
                        waremd5='SkuSecond50-iLVm1Goleg',
                        weight=7,
                        dimensions=OfferDimensions(length=113, width=77, height=52),
                    ),
                ],
                post_term_delivery=True,
            ),
            MarketSku(
                title="offer_for_branded_oultets_offer_sku",
                hyperid=1,
                sku=102,
                waremd5='BrandedOutletSku-sIieg',
                blue_offers=[
                    BlueOffer(
                        price=BRANDED_OUTLET_OFFER_PRICE,
                        vat=Vat.NO_VAT,
                        feedid=4,
                        offerid='blue.offer.branded.outlet',
                        waremd5='SkuBrandedOultet45-ieg',
                        weight=7,
                        dimensions=OfferDimensions(length=113, width=77, height=52),
                    ),
                ],
                post_term_delivery=True,
            ),
        ]

    @staticmethod
    def create_expected_options(
        courier,
        tariff_id,
        day_from,
        day_to,
        price,
        intervals=Absent(),
        payment_methods=None,
        leave_at_the_door=False,
        do_not_call=False,
        customizers=[],
        partnerType=Absent(),
    ):
        # all methods by default
        if payment_methods is None:
            payment_methods = ["YANDEX", "CASH_ON_DELIVERY", "CARD_ON_DELIVERY"]
        return {
            "results": [
                {
                    "entity": "deliveryGroup",
                    "delivery": {
                        "options": [
                            {
                                "serviceId": courier,
                                "tariffId": tariff_id,
                                "dayFrom": day_from,
                                "dayTo": day_to,
                                "price": {
                                    "currency": "RUR",
                                    "value": price,
                                },
                                "supplierPrice": Absent(),
                                "supplierDiscount": Absent(),
                                "timeIntervals": intervals,
                                "paymentMethods": payment_methods,
                                "leaveAtTheDoor": leave_at_the_door,
                                "notCall": do_not_call,
                                "customizers": customizers,
                                "partnerType": partnerType,
                            }
                        ],
                    },
                }
            ]
        }

    @staticmethod
    def create_default_outlet_options_one():
        return {
            "results": [
                {
                    "entity": "deliveryGroup",
                    "weight": "11",
                    "dimensions": [
                        "175",
                        "357",
                        "543",
                    ],
                    "delivery": {
                        "pickupOptions": [
                            {
                                "dayFrom": 2,
                                "dayTo": 3,
                                "outletIds": [2004],
                                "price": {"currency": "RUR", "value": "99"},
                                "region": {
                                    "id": MOSCOW_RIDS,
                                },
                                "serviceId": 123,
                            }
                        ],
                        "postOptions": [
                            {
                                "dayFrom": 2,
                                "dayTo": 3,
                                "outletIds": [4001],
                                "postCodes": [115200],
                                "price": {"currency": "RUR", "value": "99"},
                                "region": {
                                    "id": MOSCOW_RIDS,
                                },
                                "serviceId": 201,
                            }
                        ],
                    },
                }
            ],
        }

    @staticmethod
    def create_default_outlet_options_many():
        return {
            "results": [
                {
                    "entity": "deliveryGroup",
                    "weight": "22",
                    "dimensions": [
                        "371",
                        "379",
                        "576",
                    ],
                    "delivery": {
                        "pickupOptions": [
                            {
                                "dayFrom": 2,
                                "dayTo": 3,
                                "outletIds": [2001],
                                "price": {"currency": "RUR", "value": "0"},
                                "region": {
                                    "id": MOSCOW_RIDS,
                                },
                                "serviceId": 103,
                            },
                            {
                                "dayFrom": 3,
                                "dayTo": 4,
                                "outletIds": [2002],
                                "price": {"currency": "RUR", "value": "0"},
                                "region": {
                                    "id": MOSCOW_RIDS,
                                },
                                "serviceId": 103,
                            },
                        ],
                        "postOptions": [
                            {
                                "dayFrom": 2,
                                "dayTo": 4,
                                "outletIds": [4101],
                                "postCodes": [115200],
                                "price": {"currency": "RUR", "value": "0"},
                                "region": {
                                    "id": MOSCOW_RIDS,
                                },
                                "serviceId": 202,
                            },
                            {
                                "outletIds": [4102],
                                "postCodes": [115202],
                                "region": {
                                    "id": MOSCOW_RIDS,
                                },
                                "serviceId": 202,
                            },
                            {
                                "outletIds": [4401],
                                "postCodes": [115201],
                                "region": {
                                    "id": MOSCOW_RIDS,
                                },
                                "serviceId": 203,
                            },
                        ],
                    },
                }
            ],
        }

    @staticmethod
    def create_no_pickup_options():
        return {
            "results": [
                {
                    "entity": "deliveryGroup",
                    "weight": "22",
                    "dimensions": [
                        "371",
                        "379",
                        "576",
                    ],
                    "delivery": {
                        "options": NotEmpty(),
                        "pickupOptions": NoKey("pickupOptions"),
                        "postOptions": NoKey("postOptions"),
                    },
                }
            ],
        }

    @staticmethod
    def create_combinator_outlet_options_one(post_code=115203, add_days=0):
        return {
            "results": [
                {
                    "entity": "deliveryGroup",
                    "weight": str(VIRTUAL_BOX.weight / 1000),
                    "dimensions": [str(i) for i in VIRTUAL_BOX.dimensions],
                    "delivery": {
                        "hasPost": True,
                        "pickupOptions": [
                            {
                                "dayFrom": 2 + add_days,
                                "dayTo": 2 + add_days,
                                "outletIds": [1],
                                "paymentMethods": [
                                    "CASH_ON_DELIVERY",
                                ],
                                "price": {"currency": "RUR", "value": "99"},
                                "serviceId": 322,
                                "partnerType": Absent(),
                            },
                            {
                                "dayFrom": 2 + add_days,
                                "dayTo": 2 + add_days,
                                "outletIds": [2],
                                "paymentMethods": [
                                    "YANDEX",
                                ],
                                "price": {"currency": "RUR", "value": "99"},
                                "serviceId": 323,
                                "partnerType": Absent(),
                            },
                        ],
                        "postOptions": [
                            {
                                "dayFrom": 2 + add_days,
                                "dayTo": 2 + add_days,
                                "outletIds": [3],
                                "postCodes": [post_code],
                                "paymentMethods": [
                                    "YANDEX",
                                    "CASH_ON_DELIVERY",
                                    "CARD_ON_DELIVERY",
                                ],
                                "price": {"currency": "RUR", "value": "99"},
                                "serviceId": 323,
                                "partnerType": Absent(),
                            }
                        ],
                    },
                }
            ],
        }

    @staticmethod
    def create_combinator_outlet_options_one_post_as_pickup(post_code=115203, add_days=0):
        return {
            "results": [
                {
                    "entity": "deliveryGroup",
                    "weight": str(VIRTUAL_BOX.weight / 1000),
                    "dimensions": [str(i) for i in VIRTUAL_BOX.dimensions],
                    "delivery": {
                        "hasPost": True,
                        "pickupOptions": [
                            {
                                "dayFrom": 2 + add_days,
                                "dayTo": 2 + add_days,
                                "outletIds": [1],
                                "paymentMethods": [
                                    "CASH_ON_DELIVERY",
                                ],
                                "price": {"currency": "RUR", "value": "99"},
                                "serviceId": 322,
                                "partnerType": Absent(),
                            },
                            {
                                "dayFrom": 2 + add_days,
                                "dayTo": 2 + add_days,
                                "outletIds": [2],
                                "paymentMethods": [
                                    "YANDEX",
                                ],
                                "price": {"currency": "RUR", "value": "99"},
                                "serviceId": 323,
                                "partnerType": Absent(),
                            },
                            {
                                "dayFrom": 2 + add_days,
                                "dayTo": 2 + add_days,
                                "outletIds": [3],
                                "postCodes": [post_code],
                                "paymentMethods": [
                                    "YANDEX",
                                    "CASH_ON_DELIVERY",
                                    "CARD_ON_DELIVERY",
                                ],
                                "price": {"currency": "RUR", "value": "99"},
                                "serviceId": 323,
                                "partnerType": Absent(),
                            },
                        ],
                        "postOptions": Absent(),
                    },
                }
            ],
        }

    @staticmethod
    def create_combinator_outlet_options_many():
        return {
            "results": [
                {
                    "entity": "deliveryGroup",
                    "weight": str(VIRTUAL_BOX.weight / 1000),
                    "dimensions": [str(i) for i in VIRTUAL_BOX.dimensions],
                    "delivery": {
                        "pickupOptions": [
                            {
                                "dayFrom": 2,
                                "dayTo": 2,
                                "outletIds": [4, 5],
                                "paymentMethods": [
                                    "YANDEX",
                                    "CASH_ON_DELIVERY",
                                ],
                                "price": {
                                    "currency": "RUR",
                                    "value": "0",
                                },
                                "serviceId": 324,
                                "partnerType": Absent(),
                            },
                            {
                                "dayFrom": 3,
                                "dayTo": 3,
                                "outletIds": [6],
                                "paymentMethods": [
                                    "YANDEX",
                                    "CARD_ON_DELIVERY",
                                ],
                                "price": {
                                    "currency": "RUR",
                                    "value": "0",
                                },
                                "serviceId": 324,
                                "partnerType": Absent(),
                            },
                        ],
                        "postOptions": [
                            {
                                "dayFrom": 3,
                                "dayTo": 3,
                                "outletIds": [7],
                                "postCodes": [115203],
                                "paymentMethods": [
                                    "CASH_ON_DELIVERY",
                                    "CARD_ON_DELIVERY",
                                ],
                                "price": {
                                    "currency": "RUR",
                                    "value": "0",
                                },
                                "serviceId": 325,
                                "partnerType": Absent(),
                            }
                        ],
                    },
                }
            ],
        }

    @staticmethod
    def create_combinator_outlet_options_two():
        return {
            "results": [
                {
                    "entity": "deliveryGroup",
                    "weight": str(VIRTUAL_BOX.weight / 1000),
                    "dimensions": [str(i) for i in VIRTUAL_BOX.dimensions],
                    "delivery": {
                        "pickupOptions": [
                            {
                                "dayFrom": 1,
                                "dayTo": 1,
                                "outletIds": [8],
                                "paymentMethods": ["YANDEX", "CARD_ON_DELIVERY"],
                                "price": {"currency": "RUR", "value": "0"},
                                "supplierPrice": Absent(),
                                "supplierDiscount": Absent(),
                                "serviceId": 325,
                                "partnerType": Absent(),
                                "isMarketBranded": False,
                            },
                            {
                                "dayFrom": 1,
                                "dayTo": 1,
                                "outletIds": [2001],
                                "paymentMethods": ["YANDEX", "CARD_ON_DELIVERY"],
                                "price": {"currency": "RUR", "value": "0"},
                                "supplierPrice": Absent(),
                                "supplierDiscount": Absent(),
                                "serviceId": 103,
                                "partnerType": Absent(),
                                "isMarketBranded": True,
                                "isMarketPartner": False,
                                "isMarketPostTerm": False,
                            },
                        ],
                        "postOptions": [
                            {
                                "dayFrom": 1,
                                "dayTo": 1,
                                "outletIds": [9, 10, 11],
                                "postCodes": [115203, 115203, 115203],
                                "paymentMethods": ["YANDEX", "CASH_ON_DELIVERY"],
                                "price": {"currency": "RUR", "value": "0"},
                                "supplierPrice": Absent(),
                                "supplierDiscount": Absent(),
                                "serviceId": 325,
                                "partnerType": Absent(),
                            },
                            {
                                "dayFrom": 1,
                                "dayTo": 1,
                                "outletIds": [12],
                                "postCodes": [115203],
                                "paymentMethods": ["YANDEX", "CARD_ON_DELIVERY"],
                                "price": {"currency": "RUR", "value": "0"},
                                "supplierPrice": Absent(),
                                "supplierDiscount": Absent(),
                                "serviceId": 325,
                                "partnerType": Absent(),
                            },
                        ],
                    },
                }
            ],
        }

    @staticmethod
    def create_combinator_outlet_options_two_use_post_as_pickup():
        return {
            "results": [
                {
                    "entity": "deliveryGroup",
                    "weight": str(VIRTUAL_BOX.weight / 1000),
                    "dimensions": [str(i) for i in VIRTUAL_BOX.dimensions],
                    "delivery": {
                        "pickupOptions": [
                            {
                                "dayFrom": 1,
                                "dayTo": 1,
                                "outletIds": [8],
                                "paymentMethods": ["YANDEX", "CARD_ON_DELIVERY"],
                                "price": {"currency": "RUR", "value": "0"},
                                "supplierPrice": Absent(),
                                "supplierDiscount": Absent(),
                                "serviceId": 325,
                                "partnerType": Absent(),
                                "isMarketBranded": False,
                            },
                            {
                                "dayFrom": 1,
                                "dayTo": 1,
                                "outletIds": [2001],
                                "paymentMethods": ["YANDEX", "CARD_ON_DELIVERY"],
                                "price": {"currency": "RUR", "value": "0"},
                                "supplierPrice": Absent(),
                                "supplierDiscount": Absent(),
                                "serviceId": 103,
                                "partnerType": Absent(),
                                "isMarketBranded": True,
                                "isMarketPartner": False,
                                "isMarketPostTerm": False,
                            },
                            {
                                "dayFrom": 1,
                                "dayTo": 1,
                                "outletIds": [9, 10, 11],
                                "paymentMethods": ["YANDEX", "CASH_ON_DELIVERY"],
                                "price": {"currency": "RUR", "value": "0"},
                                "supplierPrice": Absent(),
                                "supplierDiscount": Absent(),
                                "serviceId": 325,
                                "partnerType": Absent(),
                                "type": "post",
                            },
                            {
                                "dayFrom": 1,
                                "dayTo": 1,
                                "outletIds": [12],
                                "paymentMethods": ["YANDEX", "CARD_ON_DELIVERY"],
                                "price": {"currency": "RUR", "value": "0"},
                                "supplierPrice": Absent(),
                                "supplierDiscount": Absent(),
                                "serviceId": 325,
                                "partnerType": Absent(),
                                "type": "post",
                            },
                        ],
                        "postOptions": Absent(),
                    },
                }
            ],
        }

    BASE_REQUEST_PART = (
        "place=actual_delivery&"
        "rids={rids}&"
        "pickup-options=grouped&"
        "pickup-options-extended-grouping=1&"
        "combinator={combinator_delivery}&"
        "rearr-factors=market_combinator_use_courier_payment={courier_payment}&"
        "rearr-factors=market_nordstream_relevance=1&"
    )

    BASE_SINGLE_OFFER_REQUEST = (
        BASE_REQUEST_PART
        + "offers-list=SkuSingle50-iLVm1Goleg:{sku_count}&rearr-factors=market_use_post_as_pickup={use_post_as_pickup}"
    )

    BASE_TWO_OFFER_REQUEST = (
        BASE_REQUEST_PART + "offers-list=SkuSingle50-iLVm1Goleg:{sku_100_count},SkuSecond50-iLVm1Goleg:{sku_101_count}"
    )

    @skip('already muted check later')
    def test_combinator_service_one_offer_old_delivery(self):
        """
        Проверяем, что мы ходим в комбинатор,
        но так как combinator=0, то курьерские опции берутся из бакетов.
        """
        request = T.BASE_SINGLE_OFFER_REQUEST.format(
            rids=MOSCOW_RIDS,
            combinator_delivery=0,
            courier_payment=0,
            sku_count=1,
            use_post_as_pickup=0,
        )
        response = self.report.request_json(request)

        self.assertFragmentIn(
            response,
            T.create_expected_options(
                courier="157",
                tariff_id=322,
                day_from=7,
                day_to=7,
                price="77",  # price from blue tariff
                intervals=[
                    {
                        "from": "10:00",
                        "to": "18:30",
                    }
                ],
                payment_methods=Absent(),  # no data for old report configured
                leave_at_the_door=Absent(),
                do_not_call=Absent(),
                customizers=Absent(),
                partnerType=NotEmpty(),
            ),
        )
        self.assertFragmentIn(response, T.create_default_outlet_options_one())

    @skip('already muted check later')
    def test_combinator_service_one_offer_combinator_delivery(self):
        """
        Проверяем, что с флагом combinator=1 мы ходим в комбинатор,
        и берём курьерские опции из ответа комбинатора.
        То же проверяем для pickup и post опций.
        """
        for courier_payment in (0, 1):
            request = T.BASE_SINGLE_OFFER_REQUEST.format(
                rids=MOSCOW_RIDS,
                combinator_delivery=1,
                courier_payment=courier_payment,
                sku_count=1,
                use_post_as_pickup=0,
            )

            response = self.report.request_json(request)

            payment_methods = ["YANDEX", "CASH_ON_DELIVERY", "CARD_ON_DELIVERY"]
            if courier_payment == 1:
                payment_methods = ["YANDEX", "CARD_ON_DELIVERY"]
            self.assertFragmentIn(
                response,
                T.create_expected_options(
                    courier="123",
                    tariff_id=Absent(),
                    day_from=2,
                    day_to=4,
                    price="77",  # price from combinator was 5, but replaced by price from tariff
                    intervals=[
                        {
                            "from": "10:00",
                            "to": "22:00",
                        }
                    ],
                    payment_methods=payment_methods,
                    leave_at_the_door=True,
                    do_not_call=True,
                    customizers=[
                        {
                            "key": "leave_at_the_door",
                            "name": "Оставить у двери",
                            "type": "boolean",
                        },
                        {
                            "key": "not_call",
                            "name": "Не звонить",
                            "type": "boolean",
                        },
                    ],
                    partnerType=Absent(),
                ),
            )

            self.assertFragmentIn(response, T.create_combinator_outlet_options_one())

            # Если цены доставки выключены, то цена берется из ответа комбинатора
            response = self.report.request_json(request)

            self.assertFragmentIn(
                response,
                T.create_expected_options(
                    courier="123",
                    tariff_id=Absent(),
                    day_from=2,
                    day_to=4,
                    price="77",  # price from combinator was 5, but replaced by price from tariff
                    intervals=[
                        {
                            "from": "10:00",
                            "to": "22:00",
                        }
                    ],
                    payment_methods=payment_methods,
                    leave_at_the_door=True,
                    do_not_call=True,
                    customizers=[
                        {
                            "key": "leave_at_the_door",
                            "name": "Оставить у двери",
                            "type": "boolean",
                        },
                        {
                            "key": "not_call",
                            "name": "Не звонить",
                            "type": "boolean",
                        },
                    ],
                    partnerType=Absent(),
                ),
            )

            self.assertFragmentIn(response, T.create_combinator_outlet_options_one())

            # В Питере нет регионов спутников, проверяем, что будет один регион в запросе
            request = T.BASE_SINGLE_OFFER_REQUEST.format(
                rids=SPB_RIDS,
                combinator_delivery=1,
                courier_payment=courier_payment,
                sku_count=1,
                use_post_as_pickup=1,
            )
            response = self.report.request_json(request)

            self.assertFragmentIn(response, T.create_combinator_outlet_options_one_post_as_pickup(add_days=1))

    def test_combinator_has_post_field(self):
        """
        Проверяем, что с флагом combinator=1, если
        в комбинаторе есть почтовая опция, а в репорте нет.
        То с использованием комбинатора мы проставим значение hasPost = True,
        а в репортовом флоу будет hasPost = False
        """
        request = (
            T.BASE_SINGLE_OFFER_REQUEST.format(
                rids=SPB_RIDS,
                combinator_delivery=1,
                courier_payment=1,
                sku_count=1,
                use_post_as_pickup=0,
            )
            + "&post-index=115203"
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "deliveryGroup",
                        "delivery": {
                            "hasPost": True,
                            "postOptions": NotEmptyList(),
                        },
                    }
                ]
            },
        )
        # Тут нет ошибок
        self.assertFragmentIn(response, {"search": {"commonProblems": []}})

    def test_combinator_has_post_field_use_post_as_pickup(self):
        """
        Проверяем, что с флагом combinator=1, и использовании почты как ПВЗ, если
        в комбинаторе есть почтовая опция, а в репорте нет.
        То с использованием комбинатора мы проставим значение hasPost = True,
        а в репортовом флоу будет hasPost = False
        """
        for use_combinator in (1, 0):
            request = (
                T.BASE_SINGLE_OFFER_REQUEST.format(
                    rids=SPB_RIDS,
                    combinator_delivery=use_combinator,
                    courier_payment=use_combinator,
                    sku_count=1,
                    use_post_as_pickup=1,
                )
                + "&post-index=115203"
            )
            response = self.report.request_json(request)
            if use_combinator:
                self.assertFragmentIn(
                    response,
                    {
                        "results": [
                            {
                                "entity": "deliveryGroup",
                                "delivery": {
                                    "hasPost": True,
                                    "pickupOptions": NotEmptyList(),
                                    "postOptions": Absent(),
                                },
                            }
                        ]
                    },
                )
                # Тут нет ошибок
                self.assertFragmentIn(response, {"search": {"commonProblems": []}})
            else:
                self.assertFragmentIn(
                    response,
                    {
                        "results": [
                            {
                                "entity": "deliveryGroup",
                                "delivery": {
                                    "hasPost": False,
                                    "postOptions": Absent(),
                                },
                            }
                        ]
                    },
                )

                # С отключенным флагом disable_delivery_calculator_call_for_blue_offers репорт
                # ходит в калькулятор доставки
                response = self.report.request_json(
                    request + "&rearr-factors=disable_delivery_calculator_call_for_blue_offers=0"
                )
                # Ожидаем ошибку в логах
                self.error_log.expect(code=ErrorCodes.ACD_NO_POST_OFFICE_FOR_POST_CODE)
                # Видим в ответе ошибку, потому что репорт не видит опций
                self.assertFragmentIn(
                    response,
                    {
                        "search": {
                            "commonProblems": ["NO_POST_OFFICE_FOR_POST_CODE"],
                        }
                    },
                )

    @skip('already muted check later')
    def test_combinator_service_one_offer_combinator_delivery_post_filter(self):
        """
        Проверяем, что с флагом combinator=1 мы ходим в комбинатор,
        и берём курьерские опции из ответа комбинатора.
        То же проверяем для pickup и post опций.
        """
        for courier_payment in (0, 1):
            request = (
                T.BASE_SINGLE_OFFER_REQUEST.format(
                    rids=MOSCOW_RIDS,
                    combinator_delivery=1,
                    courier_payment=courier_payment,
                    sku_count=1,
                    use_post_as_pickup=1,
                )
                + "&post-index=115201"
            )
            response = self.report.request_json(request)

            payment_methods = ["YANDEX", "CASH_ON_DELIVERY", "CARD_ON_DELIVERY"]
            if courier_payment == 1:
                payment_methods = ["YANDEX", "CARD_ON_DELIVERY"]
            self.assertFragmentIn(
                response,
                T.create_expected_options(
                    courier="123",
                    tariff_id=Absent(),
                    day_from=2,
                    day_to=4,
                    price="77",  # price from combinator was 5, but was changed to 99 by blue tariff
                    intervals=[
                        {
                            "from": "10:00",
                            "to": "22:00",
                        }
                    ],
                    payment_methods=payment_methods,
                    leave_at_the_door=True,
                    do_not_call=True,
                    customizers=[
                        {
                            "key": "leave_at_the_door",
                            "name": "Оставить у двери",
                            "type": "boolean",
                        },
                        {
                            "key": "not_call",
                            "name": "Не звонить",
                            "type": "boolean",
                        },
                    ],
                    partnerType=Absent(),
                ),
            )

            self.assertFragmentIn(response, T.create_combinator_outlet_options_one(post_code=115201))

    @skip('already muted check later')
    def test_combinator_service_many_offers_combinator_delivery(self):
        """
        Проверяем, что с флагом combinator=1 мы ходим в комбинатор,
        и берём курьерские опции из ответа комбинатора.
        То же проверяем для pickup и post опций.
        В данном случае оффер не один, но все оффера представляется в виде одной посылки с общими размерами.
        """
        for courier_payment in (0, 1):
            request = T.BASE_SINGLE_OFFER_REQUEST.format(
                rids=MOSCOW_RIDS,
                combinator_delivery=1,
                courier_payment=courier_payment,
                sku_count=2,
                use_post_as_pickup=1,
            )
            response = self.report.request_json(request)

            payment_methods = ["YANDEX", "CASH_ON_DELIVERY", "CARD_ON_DELIVERY"]
            if courier_payment == 1:
                payment_methods = ["YANDEX", "CARD_ON_DELIVERY"]
            self.assertFragmentIn(
                response,
                T.create_expected_options(
                    courier="139",
                    tariff_id=Absent(),
                    day_from=3,
                    day_to=3,
                    price="0",  # total price of 2 offers is 100 which is greater than 67 (freeDeliveryThreshold)
                    intervals=[
                        {
                            "from": "06:00",
                            "to": "19:00",
                        }
                    ],
                    payment_methods=payment_methods,
                    leave_at_the_door=True,
                    do_not_call=True,
                    customizers=[
                        {
                            "key": "leave_at_the_door",
                            "name": "Оставить у двери",
                            "type": "boolean",
                        },
                        {
                            "key": "not_call",
                            "name": "Не звонить",
                            "type": "boolean",
                        },
                    ],
                    partnerType=Absent(),
                ),
            )

            self.assertFragmentIn(response, T.create_combinator_outlet_options_many())

    def test_combinator_service_two_offer_combinator_delivery(self):
        """
        Проверяем, что с флагом combinator=1 мы ходим в комбинатор,
        и берём курьерские опции из ответа комбинатора.
        То же проверяем для pickup и post опций.
        """
        for courier_payment in (0, 1):
            request = T.BASE_TWO_OFFER_REQUEST.format(
                rids=MOSCOW_RIDS,
                combinator_delivery=1,
                courier_payment=courier_payment,
                sku_100_count=1,
                sku_101_count=1,
            )
            response = self.report.request_json(request)

            payment_methods = ["YANDEX", "CASH_ON_DELIVERY", "CARD_ON_DELIVERY"]
            if courier_payment == 1:
                payment_methods = ["YANDEX", "CARD_ON_DELIVERY"]
            self.assertFragmentIn(
                response,
                T.create_expected_options(
                    courier="140",
                    tariff_id=Absent(),
                    day_from=1,
                    day_to=5,
                    price="0",
                    intervals=[
                        {
                            "from": "08:00",
                            "to": "21:00",
                        }
                    ],
                    payment_methods=payment_methods,
                    leave_at_the_door=True,
                    do_not_call=True,
                    customizers=[
                        {
                            "key": "leave_at_the_door",
                            "name": "Оставить у двери",
                            "type": "boolean",
                        },
                        {
                            "key": "not_call",
                            "name": "Не звонить",
                            "type": "boolean",
                        },
                    ],
                    partnerType=Absent(),
                ),
            )

            self.assertFragmentIn(response, T.create_combinator_outlet_options_two_use_post_as_pickup())

    def test_combinator_no_delivery(self):
        for region, error_code in (
            (NO_COMBINATOR_DELIVERY_REGION, ErrorCodes.COMBINATOR_NO_DELIVERY_OPTIONS),
            (COMBINATOR_UNKNOWN_REGION, ErrorCodes.EXTREQUEST_COMBINATOR_REQUEST_FAILED),
        ):
            request = T.BASE_SINGLE_OFFER_REQUEST.format(
                rids=region,
                combinator_delivery=1,
                courier_payment=0,
                sku_count=1,
                use_post_as_pickup=0,
            )
            self.report.request_json(request)
            self.error_log.expect(code=error_code)

    @classmethod
    def prepare_parent_city(cls):
        cls.index.regiontree += [
            Region(
                rid=PARENT_RIDS,
                region_type=Region.CITY,
                children=[
                    Region(
                        rid=CHILD_RIDS,
                        region_type=Region.CITY,
                    )
                ],
            )
        ]

        cls.index.outlets += [
            Outlet(
                point_id=3001,
                delivery_service_id=103,
                region=PARENT_RIDS,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    shipper_id=103,
                    day_from=1,
                    day_to=1,
                    order_before=2,
                    work_in_holiday=True,
                    price=100,
                ),
                working_days=[i for i in range(10)],
                bool_props=["prepayAllowed", "cashAllowed", "cardAllowed"],
                gps_coord=GpsCoord(37.12, 55.32),
            ),
        ]

        cls.combinator.on_pickup_points_grouped_request(
            items=[
                DeliveryItem(
                    required_count=1,
                    weight=11000,
                    dimensions=[175, 357, 543],
                    cargo_types=[200],
                    offers=[
                        CombinatorOffer(
                            shop_sku="blue.offer.single1",
                            shop_id=4,
                            partner_id=145,
                            available_count=1,
                        )
                    ],
                    price=FIRST_OFFER_PRICE,
                ),
            ],
            destination_regions=[PARENT_RIDS, CHILD_RIDS],
            point_types=[],
            total_price=FIRST_OFFER_PRICE,
        ).respond_with_grouped_pickup_points(
            groups=[
                PickupPointGrouped(
                    ids_list=[3001],
                    outlet_type=OutletType.FOR_PICKUP,
                    service_id=103,
                    cost=100,
                    date_from=DATE_FROM,
                    date_to=DATE_FROM,
                    payment_methods=[PaymentMethod.PT_CASH_ON_DELIVERY],
                ),
            ],
            virtual_box=VIRTUAL_BOX,
        )

        cls.combinator.on_pickup_points_grouped_request(
            items=[
                DeliveryItem(
                    required_count=1,
                    weight=11000,
                    dimensions=[175, 357, 543],
                    cargo_types=[200],
                    offers=[
                        CombinatorOffer(
                            shop_sku="blue.offer.single1",
                            shop_id=4,
                            partner_id=145,
                            available_count=1,
                        )
                    ],
                    price=FIRST_OFFER_PRICE,
                ),
            ],
            destination_regions=[PARENT_RIDS],
            point_types=[],
            total_price=FIRST_OFFER_PRICE,
        ).respond_with_grouped_pickup_points(
            groups=[
                PickupPointGrouped(
                    ids_list=[3001],
                    outlet_type=OutletType.FOR_PICKUP,
                    service_id=103,
                    cost=100,
                    date_from=DATE_TO,
                    date_to=DATE_TO,
                    payment_methods=[PaymentMethod.PT_CASH_ON_DELIVERY],
                ),
            ],
            virtual_box=VIRTUAL_BOX,
        )

        for region in [PARENT_RIDS, CHILD_RIDS]:
            cls.combinator.on_courier_options_request(
                items=[
                    DeliveryItem(
                        required_count=1,
                        weight=11000,
                        dimensions=[175, 357, 543],
                        cargo_types=[200],
                        offers=[
                            CombinatorOffer(shop_sku="blue.offer.single1", shop_id=4, partner_id=145, available_count=1)
                        ],
                        price=FIRST_OFFER_PRICE,
                    ),
                ],
                destination=Destination(region_id=region),
                payment_methods=[],
                total_price=FIRST_OFFER_PRICE,
            ).respond_with_courier_options(
                options=[],
                virtual_box=VIRTUAL_BOX,
            )

    def __check_parent_city(self, region, days):
        request = T.BASE_SINGLE_OFFER_REQUEST.format(
            rids=region,
            combinator_delivery=1,
            courier_payment=0,
            sku_count=1,
            use_post_as_pickup=0,
        )
        response = self.report.request_json(request)

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "delivery": {
                            "pickupOptions": [
                                {
                                    "dayFrom": days,
                                    "dayTo": days,
                                    "outletIds": [3001],
                                    "paymentMethods": [
                                        "CASH_ON_DELIVERY",
                                    ],
                                    "price": {
                                        "currency": "RUR",
                                        "value": "99",
                                    },
                                    "serviceId": 103,
                                }
                            ]
                        }
                    }
                ]
            },
            allow_different_len=False,
        )

    def test_parent_city(self):
        '''
        Проверяем запросы в комбинатор для городов, вложенных в другие города.
        Например, Сочи и Адлер. Москва и Сколково
        '''
        # Если пришли с родительским городом, то дочерние города не запрашиваются
        # 4 дня соответствует DATE_TO, заданного для запроса destination_regions=[PARENT_RIDS],
        self.__check_parent_city(PARENT_RIDS, 4)

        # Для города внутри другого города запрашивается еще родительский город
        # 4 дня соответствует DATE_FROM, заданного для запроса destination_regions=[PARENT_RIDS, CHILD_RIDS],
        self.__check_parent_city(CHILD_RIDS, 2)

    def test_force_delivery_id(self):
        '''
        Проверка пробрасывания параметра force-delivery-id в комбинатор
        '''
        REQUESTED_DELIVERY_ID = 123
        request = (
            "place=actual_delivery&rids={}&debug=1".format(MOSCOW_RIDS)
            + "&combinator=1"
            + "&offers-list=SkuSingle50-iLVm1Goleg:1"
            + "&force-delivery-id={}".format(REQUESTED_DELIVERY_ID)
        )
        expected_trace = {
            "logicTrace": [
                r"\[ME\].*? CalculateCourierDelivery\(\): Combinator request:.*? option \{ "
                + r"delivery_service_id: {}".format(REQUESTED_DELIVERY_ID)
                + r" \}"
            ]
        }
        self.assertFragmentIn(self.report.request_json(request), expected_trace, use_regex=True)

    def test_combinator_param(self):
        '''
        Проверка корректности работы флага &combinator
        '''
        expected_courier_trace = {
            "logicTrace": [
                r"\[ME\].*? CalculateCourierDelivery\(\): Combinator request:.*rearr_factors:.*parallel_smm=1.0;ext_snippet=1;no_snippet_arc=1"
            ]
        }
        expected_pickup_trace = {
            "logicTrace": [
                r"\[ME\].*? CalculatePickupDelivery\(\): Combinator request:.*rearr_factors:.*parallel_smm=1.0;ext_snippet=1;no_snippet_arc=1"
            ]
        }

        for param, should_use_combinator in (('', True), ('&combinator=0', False), ('&combinator=1', True)):
            request = (
                "place=actual_delivery&rids={}&debug=1".format(MOSCOW_RIDS)
                + "&offers-list=SkuSingle50-iLVm1Goleg:1"
                + param
            )
            response = self.report.request_json(request)

            if should_use_combinator:
                self.assertFragmentIn(response, expected_courier_trace, use_regex=True)
                self.assertFragmentIn(response, expected_pickup_trace, use_regex=True)
            else:
                self.assertFragmentNotIn(response, expected_courier_trace, use_regex=True)
                self.assertFragmentNotIn(response, expected_pickup_trace, use_regex=True)

    def test_outlet_time_interval(self):
        """
        Проверяется, что под флагом enable_outlet_time_interval приходит временной интервал доставки в аутлеты
        """

        for enable_outlet_interval in [0, 1]:
            request = T.BASE_SINGLE_OFFER_REQUEST.format(
                rids=MOSCOW_RIDS,
                combinator_delivery=1,
                courier_payment=0,
                sku_count=1,
                use_post_as_pickup=0,
            )
            # для первого аутлета должен приходить интервал под флагом, а для второго нет
            response = self.report.request_json(
                request + "&rearr-factors=enable_outlet_time_interval={}".format(enable_outlet_interval)
            )
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "entity": "deliveryGroup",
                            "delivery": {
                                "pickupOptions": [
                                    {
                                        "serviceId": 322,
                                        "outletIds": [1],
                                        "outletTimeIntervals": [
                                            {"outletId": 1, "from": "10:00", "to": "22:00"},
                                        ]
                                        if enable_outlet_interval
                                        else [],
                                    },
                                    {
                                        "serviceId": 323,
                                        "outletIds": [2],
                                        "outletTimeIntervals": [],
                                    },
                                ],
                            },
                        }
                    ]
                },
                allow_different_len=False,
            )

    def test_outlet_time_interval_use_post_as_pickup(self):
        """
        Проверяется, что под флагом enable_outlet_time_interval приходит временной интервал доставки в аутлеты
        для почты как ПВЗ
        """
        for enable_outlet_interval in [0, 1]:
            request = T.BASE_SINGLE_OFFER_REQUEST.format(
                rids=MOSCOW_RIDS,
                combinator_delivery=1,
                courier_payment=0,
                sku_count=1,
                use_post_as_pickup=1,
            )
            # для первого аутлета должен приходить интервал под флагом, а для второго нет
            response = self.report.request_json(
                request + "&rearr-factors=enable_outlet_time_interval={}".format(enable_outlet_interval)
            )
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "entity": "deliveryGroup",
                            "delivery": {
                                "pickupOptions": [
                                    {
                                        "serviceId": 322,
                                        "outletIds": [1],
                                        "outletTimeIntervals": [
                                            {"outletId": 1, "from": "10:00", "to": "22:00"},
                                        ]
                                        if enable_outlet_interval
                                        else [],
                                    },
                                    {"serviceId": 323, "outletIds": [2], "outletTimeIntervals": []},
                                    {"serviceId": 323, "outletIds": [3], "outletTimeIntervals": [], "type": "post"},
                                    {"serviceId": 323, "outletIds": [4], "outletTimeIntervals": [], "type": "post"},
                                ],
                            },
                        }
                    ]
                },
                allow_different_len=False,
            )

    def test_avia_delivery(self):
        """
        Проверяется, что под флагом avia_delivery приходят опции авиа курьерки с правильными ценами
        тестируем как обычный случай, так и случай с бесплатной доставкой в регионе (два товара в корзине),
        в этом случае стоимость не должна обнуляться
        """
        for avia_delivery in [0, 1]:
            for perk in ["yandex_plus", ""]:
                for sku_count, date_from, date_to in [(1, 2, 4), (2, 3, 3)]:
                    request = T.BASE_SINGLE_OFFER_REQUEST.format(
                        rids=MOSCOW_RIDS,
                        enable_combinator=1,
                        combinator_delivery=1,
                        courier_payment=0,
                        sku_count=sku_count,
                        use_post_as_pickup=1,
                    )
                    response = self.report.request_json(
                        request + "&rearr-factors=avia_delivery={}&perks={}".format(avia_delivery, perk)
                    )
                    self.assertFragmentIn(
                        response,
                        {
                            "results": [
                                {
                                    "entity": "deliveryGroup",
                                    "delivery": {
                                        "options": [
                                            {
                                                "price": {
                                                    "currency": "RUR",
                                                    "value": "150" if perk == "yandex_plus" else "200",
                                                },
                                                "dayFrom": date_from,
                                                "dayTo": date_to,
                                            },
                                        ]
                                        if avia_delivery
                                        else Absent(),
                                    },
                                }
                            ]
                        },
                        allow_different_len=False,
                    )

    def test_branded_outlets(self):
        """
        Проверяется, что при использовании комбинатора для аутлетов отдаются признаки
        брендированности, партнерства и постамата, а также
        что эти признаки участвует в группировке.
        """

        request = (
            T.BASE_REQUEST_PART.format(rids=MOSCOW_RIDS, combinator_delivery=1, courier_payment=0)
            + "offers-list=SkuBrandedOultet45-ieg:1"
        )

        # два аутлета пришли от комбинатора в одной группе, но, несмотря на то, что стоимость доставки одинаковая,
        # репорт их разгруппировал, так как один брендированный, а другой нет
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "deliveryGroup",
                        "delivery": {
                            "pickupOptions": [
                                {
                                    "serviceId": 103,
                                    "outletIds": [2001],
                                    "isMarketBranded": True,
                                    "isMarketPartner": False,
                                    "isMarketPostTerm": False,
                                    "price": {"currency": "RUR", "value": "99"},
                                },
                                {
                                    "serviceId": 103,
                                    "outletIds": [2002],
                                    "isMarketBranded": False,
                                    "isMarketPartner": True,
                                    "isMarketPostTerm": False,
                                    "price": {"currency": "RUR", "value": "99"},
                                },
                                {
                                    "serviceId": 103,
                                    "outletIds": [2003],
                                    "isMarketBranded": False,
                                    "isMarketPartner": False,
                                    "isMarketPostTerm": True,
                                    "price": {"currency": "RUR", "value": "99"},
                                },
                            ],
                        },
                    }
                ]
            },
            allow_different_len=False,
        )

    def test_trying_available(self):
        """
        Проверяется, что при использовании комбинатора для аутлетов и опций курьерской доставки
        отдается признак наличия примерки.
        """

        request = (
            T.BASE_REQUEST_PART.format(rids=MOSCOW_RIDS, combinator_delivery=1, courier_payment=0)
            + "offers-list=SkuBrandedOultet45-ieg:1"
        )

        response = self.report.request_json(request)

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "deliveryGroup",
                        "delivery": {
                            "options": [
                                {
                                    "dayFrom": 2,
                                    "dayTo": 4,
                                    "isTryingAvailable": True,
                                    "price": {"currency": "RUR", "value": "77"},
                                    "serviceId": "199",
                                }
                            ],
                            "pickupOptions": [
                                {
                                    "serviceId": 103,
                                    "outletIds": [2001],
                                    "isMarketBranded": True,
                                    "isMarketPartner": False,
                                    "isMarketPostTerm": False,
                                    "isTryingAvailable": True,
                                    "price": {"currency": "RUR", "value": "99"},
                                },
                            ],
                        },
                    }
                ]
            },
            allow_different_len=True,
        )


if __name__ == '__main__':
    main()
