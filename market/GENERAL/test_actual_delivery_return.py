#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa
import itertools

from datetime import (
    datetime,
    time,
    timedelta,
)
from core.types import (
    Currency,
    DeliveryBucket,
    DynamicWarehouseInfo,
    DynamicWarehouseToWarehouseInfo,
    GpsCoord,
    Outlet,
    OutletDeliveryOption,
    PickupBucket,
    PickupOption,
    Shop,
)
from core.report import REQUEST_TIMESTAMP

from core.types.offer import OfferDimensions
from core.types.sku import MarketSku, BlueOffer
from core.types.taxes import Vat, Tax
from core.matcher import Absent
from core.logs import ErrorCodes
from core.testcase import TestCase, main
from core.types.delivery import (
    BlueDeliveryTariff,
    DeliveryServiceModifier,
    OutletType,
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

from market.pylibrary.const.payment_methods import (
    PaymentMethod,
)

# This date is fixed for all test
TODAY = datetime.fromtimestamp(REQUEST_TIMESTAMP)
DATE_FROM = TODAY + timedelta(days=2)
DATE_TO = TODAY + timedelta(days=4)
DATE_FROM_RETURN = TODAY + timedelta(days=1)
DATE_TO_RETURN = TODAY + timedelta(days=5)

RETURN_WAREHOUSE_ID = 172
USUAL_WAREHOUSE_ID = 145
USUAL_OUTLET_ID = 1
RETURN_OUTLET_ID = 2
POST_USUAL_OUTLET_ID = 3
POST_RETURN_OUTLET_ID = 4
RETURN_TARIFF_OUTLET_ID = 5
RETURN_TARIFF_OUTLET_ID2 = 6
DELIVERY_SERVICE_ID = 103
POST_DELIVERY_SERVICE_ID = 104
RETURN_DELIVERY_SERVICE_ID = 105
PICKUP_BUCKET_ID1 = 501
PICKUP_BUCKET_ID2 = 502
POST_PICKUP_BUCKET_ID = 503
RETURN_TARIFFS_PICKUP_BUCKET_ID = 504
RETURN_TARIFFS_PICKUP_BUCKET_ID2 = 505
DC_PICKUP_BUCKET_ID1 = 4
DC_PICKUP_BUCKET_ID2 = 5
DC_POST_BUCKET_ID = 6
DC_RETURN_TARIFFS_BUCKET_ID = 7
DC_RETURN_TARIFFS_BUCKET_ID2 = 8

OFFER_PROPS1 = {'width': 10, 'height': 20, 'length': 30, 'weight': 5}
OFFER_PROPS2 = {'width': 40, 'height': 20, 'length': 10, 'weight': 7}
POST_OFFER_PROPS = {'width': 15, 'height': 20, 'length': 25, 'weight': 4}


class _Shops(object):
    virtual_shop_blue = Shop(
        fesh=1,
        datafeed_id=1,
        priority_region=213,
        name='virtual_shop',
        currency=Currency.RUR,
        tax_system=Tax.OSN,
        fulfillment_virtual=True,
        cpa=Shop.CPA_REAL,
        virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
        delivery_service_outlets=[
            USUAL_OUTLET_ID,
            RETURN_OUTLET_ID,
            POST_USUAL_OUTLET_ID,
            POST_RETURN_OUTLET_ID,
            RETURN_TARIFF_OUTLET_ID,
            RETURN_TARIFF_OUTLET_ID2,
        ],
    )

    blue_shop_1 = Shop(
        fesh=3,
        datafeed_id=3,
        priority_region=2,
        currency=Currency.RUR,
        tax_system=Tax.OSN,
        supplier_type=Shop.FIRST_PARTY,
        blue='REAL',
        warehouse_id=USUAL_WAREHOUSE_ID,
    )

    blue_shop_2 = Shop(
        fesh=3,
        datafeed_id=4,
        priority_region=2,
        currency=Currency.RUR,
        tax_system=Tax.OSN,
        supplier_type=Shop.FIRST_PARTY,
        blue='REAL',
        warehouse_id=RETURN_WAREHOUSE_ID,
    )


class _Offers(object):
    sku1_offer1 = BlueOffer(
        price=5,
        vat=Vat.VAT_10,
        feedid=3,
        waremd5='Sku1Price5-IiLVm1Goleg',
        offerid="blue_offer_sku1",
        weight=OFFER_PROPS1['weight'],
        dimensions=OfferDimensions(
            length=OFFER_PROPS1['length'], width=OFFER_PROPS1['width'], height=OFFER_PROPS1['height']
        ),
        pickup_buckets=[PICKUP_BUCKET_ID1, RETURN_TARIFFS_PICKUP_BUCKET_ID, RETURN_TARIFFS_PICKUP_BUCKET_ID2],
        post_term_delivery=True,
    )
    sku2_offer1 = BlueOffer(
        price=55,
        vat=Vat.VAT_18,
        feedid=4,
        offerid='blue.offer.2.1',
        waremd5='Sku2Price55-iLVm1Goleg',
        weight=OFFER_PROPS2['weight'],
        dimensions=OfferDimensions(
            length=OFFER_PROPS2['length'], width=OFFER_PROPS2['width'], height=OFFER_PROPS2['height']
        ),
        pickup_buckets=[PICKUP_BUCKET_ID2],
        post_term_delivery=True,
    )
    sku3_offer1 = BlueOffer(
        price=2990,
        vat=Vat.VAT_10,
        feedid=4,
        offerid='blue.offer.1.3',
        waremd5='Sku1Pr2990-IiLVm1Goleg',
        weight=POST_OFFER_PROPS['weight'],
        dimensions=OfferDimensions(
            length=POST_OFFER_PROPS['length'], width=POST_OFFER_PROPS['width'], height=POST_OFFER_PROPS['height']
        ),
        pickup_buckets=[PICKUP_BUCKET_ID2],
        post_buckets=[POST_PICKUP_BUCKET_ID],
        post_term_delivery=True,
    )


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=RETURN_WAREHOUSE_ID, home_region=213),
            DynamicWarehouseInfo(id=USUAL_WAREHOUSE_ID, home_region=213),
            DynamicWarehouseToWarehouseInfo(warehouse_from=RETURN_WAREHOUSE_ID, warehouse_to=RETURN_WAREHOUSE_ID),
            DynamicWarehouseToWarehouseInfo(warehouse_from=USUAL_WAREHOUSE_ID, warehouse_to=USUAL_WAREHOUSE_ID),
        ]
        # НЕ делайте так в новых тестах!
        # Походов в КД на проде уже нет, пожалуйста, проверяйте новую функциональность, создавая доставку через комбинатор
        cls.settings.default_search_experiment_flags += ['force_disable_delivery_calculator_requests=0']

        cls.index.outlets += [
            Outlet(
                point_id=USUAL_OUTLET_ID,
                delivery_service_id=DELIVERY_SERVICE_ID,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    shipper_id=DELIVERY_SERVICE_ID,
                    day_from=1,
                    day_to=1,
                    order_before=2,
                    work_in_holiday=True,
                    price=100,
                ),
                working_days=[i for i in range(10)],
            ),
            Outlet(
                point_id=RETURN_OUTLET_ID,
                delivery_service_id=DELIVERY_SERVICE_ID,
                region=213,
                point_type=Outlet.FOR_POST_TERM,
                delivery_option=OutletDeliveryOption(shipper_id=DELIVERY_SERVICE_ID, day_from=1, day_to=1, price=400),
                working_days=[i for i in range(10)],
                bool_props=["returnAllowed"],
            ),
            Outlet(
                point_id=POST_USUAL_OUTLET_ID,
                delivery_service_id=POST_DELIVERY_SERVICE_ID,
                region=213,
                point_type=Outlet.FOR_POST,
                post_code=115200,
                delivery_option=OutletDeliveryOption(
                    shipper_id=POST_DELIVERY_SERVICE_ID, day_from=2, day_to=4, price=400
                ),
                working_days=[i for i in range(30)],
                gps_coord=GpsCoord(67.8, 55.9),
            ),
            Outlet(
                point_id=POST_RETURN_OUTLET_ID,
                delivery_service_id=POST_DELIVERY_SERVICE_ID,
                region=213,
                point_type=Outlet.FOR_POST,
                post_code=115201,
                delivery_option=OutletDeliveryOption(
                    shipper_id=POST_DELIVERY_SERVICE_ID, day_from=1, day_to=1, price=500
                ),
                working_days=[i for i in range(30)],
                bool_props=["returnAllowed"],
                gps_coord=GpsCoord(67.8, 55.9),
            ),
        ]

        cls.index.shops += [_Shops.virtual_shop_blue, _Shops.blue_shop_1, _Shops.blue_shop_2]

        cls.index.mskus += [
            MarketSku(
                title="blue offer sku1",
                hyperid=1,
                sku=101010,
                blue_offers=[_Offers.sku1_offer1],
            ),
            MarketSku(title="blue offer sku2", hyperid=1, sku=202020, blue_offers=[_Offers.sku2_offer1]),
            MarketSku(
                title="blue offer sku3",
                hyperid=1,
                sku=303030,
                blue_offers=[
                    _Offers.sku3_offer1,
                ],
                post_term_delivery=True,
            ),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=PICKUP_BUCKET_ID1,
                dc_bucket_id=DC_PICKUP_BUCKET_ID1,
                fesh=1,
                carriers=[DELIVERY_SERVICE_ID],
                options=[
                    PickupOption(outlet_id=USUAL_OUTLET_ID, day_from=1, day_to=2, price=5),
                    PickupOption(outlet_id=RETURN_OUTLET_ID, day_from=1, day_to=2, price=5),
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            PickupBucket(
                bucket_id=PICKUP_BUCKET_ID2,
                dc_bucket_id=DC_PICKUP_BUCKET_ID2,
                fesh=1,
                carriers=[DELIVERY_SERVICE_ID],
                options=[
                    PickupOption(outlet_id=USUAL_OUTLET_ID, day_from=5, day_to=6, price=5),
                    PickupOption(outlet_id=RETURN_OUTLET_ID, day_from=5, day_to=6, price=5),
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            PickupBucket(
                bucket_id=POST_PICKUP_BUCKET_ID,
                dc_bucket_id=DC_POST_BUCKET_ID,
                fesh=1,
                carriers=[POST_DELIVERY_SERVICE_ID],
                options=[
                    PickupOption(outlet_id=POST_USUAL_OUTLET_ID, day_from=4, day_to=5, price=6),
                    PickupOption(outlet_id=POST_RETURN_OUTLET_ID, day_from=4, day_to=5, price=6),
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
        ]

        cls.delivery_calc.on_request_offer_buckets(
            weight=OFFER_PROPS1['weight'],
            width=OFFER_PROPS1['width'],
            height=OFFER_PROPS1['height'],
            length=OFFER_PROPS1['length'],
            warehouse_id=USUAL_WAREHOUSE_ID,
        ).respond([], [DC_PICKUP_BUCKET_ID1, DC_RETURN_TARIFFS_BUCKET_ID, DC_RETURN_TARIFFS_BUCKET_ID2], [])
        cls.delivery_calc.on_request_offer_buckets(
            weight=OFFER_PROPS1['weight'],
            width=OFFER_PROPS1['width'],
            height=OFFER_PROPS1['height'],
            length=OFFER_PROPS1['length'],
            warehouse_id=RETURN_WAREHOUSE_ID,
        ).respond([], [DC_PICKUP_BUCKET_ID2, DC_RETURN_TARIFFS_BUCKET_ID, DC_RETURN_TARIFFS_BUCKET_ID2], [])
        cls.delivery_calc.on_request_offer_buckets(
            weight=OFFER_PROPS2['weight'],
            width=OFFER_PROPS2['width'],
            height=OFFER_PROPS2['height'],
            length=OFFER_PROPS2['length'],
            warehouse_id=USUAL_WAREHOUSE_ID,
        ).respond([], [DC_PICKUP_BUCKET_ID1], [])
        cls.delivery_calc.on_request_offer_buckets(
            weight=OFFER_PROPS2['weight'],
            width=OFFER_PROPS2['width'],
            height=OFFER_PROPS2['height'],
            length=OFFER_PROPS2['length'],
            warehouse_id=RETURN_WAREHOUSE_ID,
        ).respond([], [DC_PICKUP_BUCKET_ID2], [])
        cls.delivery_calc.on_request_offer_buckets(
            weight=POST_OFFER_PROPS['weight'],
            width=POST_OFFER_PROPS['width'],
            height=POST_OFFER_PROPS['height'],
            length=POST_OFFER_PROPS['length'],
            warehouse_id=RETURN_WAREHOUSE_ID,
        ).respond([], [DC_PICKUP_BUCKET_ID2], [DC_POST_BUCKET_ID])

    @classmethod
    def prepare_combinator(cls):
        report_start_time_in_ms = REQUEST_TIMESTAMP * 10**6
        cls.combinator.set_start_date(microseconds_for_disabled_random=report_start_time_in_ms)
        cls.settings.default_search_experiment_flags += ['enable_dsbs_combinator_request_in_actual_delivery=0']

        for is_return in [0, 1]:
            for warehouse in [_Shops.blue_shop_1.warehouse_id, RETURN_WAREHOUSE_ID]:
                # курьерские опции для фейкового оффера
                cls.combinator.on_courier_options_request(
                    items=[
                        DeliveryItem(
                            required_count=1,
                            weight=_Offers.sku1_offer1.weight * 1000,
                            dimensions=[OFFER_PROPS1['width'], OFFER_PROPS1['height'], OFFER_PROPS1['length']],
                            cargo_types=[],
                            offers=[
                                CombinatorOffer(
                                    shop_sku="",
                                    shop_id=4294967295,  # default value - max(ui32)
                                    partner_id=warehouse,
                                    available_count=1,
                                ),
                            ],
                        ),
                    ],
                    destination=Destination(region_id=213),
                    payment_methods=[],
                    total_price=0,
                    is_return=is_return,
                ).respond_with_courier_options(
                    options=[
                        create_delivery_option(
                            cost=15,
                            date_from=DATE_FROM_RETURN if is_return else DATE_FROM,
                            date_to=DATE_TO_RETURN if is_return else DATE_TO,
                            time_from=time(8, 0),
                            time_to=time(21, 0),
                            delivery_service_id=140,
                            payment_methods=[
                                PaymentMethod.PT_YANDEX,
                                PaymentMethod.PT_CARD_ON_DELIVERY,
                            ],
                            leave_at_the_door=True,
                            do_not_call=True,
                        )
                    ],
                    virtual_box=create_virtual_box(weight=18000, length=100, width=80, height=50),
                    is_return=is_return,
                )

                # курьерские опции для обычного оффера
                cls.combinator.on_courier_options_request(
                    items=[
                        DeliveryItem(
                            required_count=1,
                            weight=_Offers.sku1_offer1.weight * 1000,
                            dimensions=[OFFER_PROPS1['width'], OFFER_PROPS1['height'], OFFER_PROPS1['length']],
                            cargo_types=[],
                            offers=[
                                CombinatorOffer(
                                    shop_sku=_Offers.sku1_offer1.offerid,
                                    shop_id=_Shops.blue_shop_1.fesh,
                                    partner_id=warehouse,
                                    available_count=1,
                                ),
                            ],
                            price=_Offers.sku1_offer1.price,
                        ),
                    ],
                    destination=Destination(region_id=213),
                    payment_methods=[],
                    total_price=_Offers.sku1_offer1.price,
                    is_return=is_return,
                ).respond_with_courier_options(
                    options=[
                        create_delivery_option(
                            cost=15,
                            date_from=DATE_FROM_RETURN if is_return else DATE_FROM,
                            date_to=DATE_TO_RETURN if is_return else DATE_TO,
                            time_from=time(8, 0),
                            time_to=time(21, 0),
                            delivery_service_id=140,
                            payment_methods=[
                                PaymentMethod.PT_YANDEX,
                                PaymentMethod.PT_CARD_ON_DELIVERY,
                            ],
                            leave_at_the_door=True,
                            do_not_call=True,
                        )
                    ],
                    virtual_box=create_virtual_box(weight=18000, length=100, width=80, height=50),
                    is_return=is_return,
                )

                # пикап опции для фейкового оффера
                cls.combinator.on_pickup_points_grouped_request(
                    items=[
                        DeliveryItem(
                            required_count=1,
                            weight=_Offers.sku1_offer1.weight * 1000,
                            dimensions=[OFFER_PROPS1['width'], OFFER_PROPS1['height'], OFFER_PROPS1['length']],
                            cargo_types=[],
                            offers=[
                                CombinatorOffer(
                                    shop_sku="",
                                    shop_id=4294967295,  # default value - max(ui32)
                                    partner_id=warehouse,
                                    available_count=1,
                                ),
                            ],
                        ),
                    ],
                    destination_regions=[213],
                    point_types=[],
                    total_price=0,
                    is_return=is_return,
                ).respond_with_grouped_pickup_points(
                    groups=[
                        PickupPointGrouped(
                            ids_list=[1],
                            outlet_type=OutletType.FOR_PICKUP,
                            service_id=322,
                            cost=100,
                            date_from=DATE_FROM_RETURN if is_return else DATE_FROM,
                            date_to=DATE_TO_RETURN if is_return else DATE_TO,
                            payment_methods=[PaymentMethod.PT_CASH_ON_DELIVERY],
                            delivery_intervals=[
                                PointTimeInterval(point_id=1, time_from=time(10, 0), time_to=time(22, 0)),
                            ],
                        ),
                    ],
                    virtual_box=create_virtual_box(weight=18000, length=100, width=80, height=50),
                    is_return=is_return,
                )

                # пикап опции для обычного оффера
                cls.combinator.on_pickup_points_grouped_request(
                    items=[
                        DeliveryItem(
                            required_count=1,
                            weight=_Offers.sku1_offer1.weight * 1000,
                            dimensions=[OFFER_PROPS1['width'], OFFER_PROPS1['height'], OFFER_PROPS1['length']],
                            cargo_types=[],
                            offers=[
                                CombinatorOffer(
                                    shop_sku=_Offers.sku1_offer1.offerid,
                                    shop_id=_Shops.blue_shop_1.fesh,
                                    partner_id=warehouse,
                                    available_count=1,
                                ),
                            ],
                            price=_Offers.sku1_offer1.price,
                        ),
                    ],
                    destination_regions=[213],
                    point_types=[],
                    total_price=_Offers.sku1_offer1.price,
                    is_return=is_return,
                ).respond_with_grouped_pickup_points(
                    groups=[
                        PickupPointGrouped(
                            ids_list=[1],
                            outlet_type=OutletType.FOR_PICKUP,
                            service_id=322,
                            cost=100,
                            date_from=DATE_FROM_RETURN if is_return else DATE_FROM,
                            date_to=DATE_TO_RETURN if is_return else DATE_TO,
                            payment_methods=[PaymentMethod.PT_CASH_ON_DELIVERY],
                            delivery_intervals=[
                                PointTimeInterval(point_id=1, time_from=time(10, 0), time_to=time(22, 0)),
                            ],
                        ),
                    ],
                    virtual_box=create_virtual_box(weight=18000, length=100, width=80, height=50),
                    is_return=is_return,
                )

    def test_usual_requests(self):
        '''Проверяется, что без флага is-return и парамера market_return_to_wh запрос будет работать как обычно'''
        for offer, day_from, day_to in [
            ('Sku1Price5-IiLVm1Goleg:1', 1, 2),
            ('Sku2Price55-iLVm1Goleg:1', 5, 6),
            ('Sku1Price5-IiLVm1Goleg:1;wh:145;ffWh:145;w:5;d:30x10x20;', 1, 2),
            ('Sku2Price55-iLVm1Goleg:1;wh:145;ffWh:145;w:7;d:40x20x10;', 1, 2),
            ('FakeOffer1-IiLVm1Goleg:1;wh:145;ffWh:145;w:5;d:30x10x20;', 1, 2),
        ]:
            request = (
                'place=actual_delivery'
                '&offers-list={}'
                '&rids=213'
                '&pickup-options=grouped'
                '&pickup-options-extended-grouping=1'
                '&combinator=0'
            )
            response = self.report.request_json(request.format(offer))
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "delivery": {
                                "pickupOptions": [
                                    {
                                        "serviceId": 103,
                                        "dayFrom": day_from,
                                        "dayTo": day_to,
                                        "outletIds": [1, 2],
                                    },
                                ]
                            }
                        }
                    ]
                },
                allow_different_len=False,
            )

    def test_with_is_return_flag_without_return_warehouse(self):
        '''Проверяется, что по умолчанию значение market_return_to_wh соответствует складу в Софьино и вместе с флагом is-return будет
        произведена подмена склада на склад в Софьино, а также отфильтрованы аутлеты, которые не поддерживают возврат'''
        for offer, day_from, day_to in [
            ('Sku1Price5-IiLVm1Goleg:1', 5, 6),
            ('Sku2Price55-iLVm1Goleg:1', 5, 6),
            ('Sku1Price5-IiLVm1Goleg:1;wh:145;ffWh:145;w:5;d:30x10x20;', 5, 6),
            ('Sku2Price55-iLVm1Goleg:1;wh:145;ffWh:145;w:7;d:40x20x10;', 5, 6),
            ('FakeOffer1-IiLVm1Goleg:1;wh:145;ffWh:145;w:5;d:30x10x20;', 5, 6),
        ]:
            request = (
                'place=actual_delivery'
                '&offers-list={}'
                '&rids=213'
                '&pickup-options=grouped'
                '&pickup-options-extended-grouping=1'
                '&is-return=1'
                '&rearr-factors=enable_dropship_return_through_market_outlets=0'
                '&combinator=0'
            )
            response = self.report.request_json(request.format(offer))
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "delivery": {
                                "pickupOptions": [
                                    {
                                        "serviceId": 103,
                                        "dayFrom": day_from,
                                        "dayTo": day_to,
                                        "outletIds": [2],
                                    },
                                ]
                            }
                        }
                    ]
                },
                allow_different_len=False,
            )

        '''Установка market_return_to_wh=0 или enable_dropship_return_through_market_outlets=1 отлючает подмену склада'''
        for offer, day_from, day_to in [
            ('Sku1Price5-IiLVm1Goleg:1', 1, 2),
            ('Sku2Price55-iLVm1Goleg:1', 5, 6),
            ('Sku1Price5-IiLVm1Goleg:1;wh:145;ffWh:145;w:5;d:30x10x20;', 1, 2),
            ('Sku2Price55-iLVm1Goleg:1;wh:145;ffWh:145;w:7;d:40x20x10;', 1, 2),
            ('FakeOffer1-IiLVm1Goleg:1;wh:145;ffWh:145;w:5;d:30x10x20;', 1, 2),
        ]:
            for flag in ['market_return_to_wh=0', 'enable_dropship_return_through_market_outlets=1']:
                request = (
                    'place=actual_delivery'
                    '&offers-list={}'
                    '&rids=213'
                    '&pickup-options=grouped'
                    '&pickup-options-extended-grouping=1'
                    '&is-return=1'
                    '&rearr-factors={}'
                    '&combinator=0'
                )
                response = self.report.request_json(request.format(offer, flag))
                self.assertFragmentIn(
                    response,
                    {
                        "results": [
                            {
                                "delivery": {
                                    "pickupOptions": [
                                        {
                                            "serviceId": DELIVERY_SERVICE_ID,
                                            "dayFrom": day_from,
                                            "dayTo": day_to,
                                            "outletIds": [RETURN_OUTLET_ID],
                                        },
                                    ]
                                }
                            }
                        ]
                    },
                    allow_different_len=False,
                )

    def test_with_is_return_and_return_warehouse_flags(self):
        '''Проверяется, что с флагом is-return и парамером market_return_to_wh будет расчитываться маршрут до склада,
        переданного в market_return_to_wh, и аутлеты, в которых невозможен возврат также будут отфильтрованы'''
        for offer in [
            'Sku1Price5-IiLVm1Goleg:1',
            'Sku2Price55-iLVm1Goleg:1',
            'Sku1Price5-IiLVm1Goleg:1;wh:145;ffWh:145;w:5;d:30x10x20;',
            'Sku2Price55-iLVm1Goleg:1;wh:145;ffWh:145;w:7;d:40x20x10;',
            'FakeOffer1-IiLVm1Goleg:1;wh:145;ffWh:145;w:5;d:30x10x20;',
        ]:
            for day_from, day_to, warehouse in [(1, 2, USUAL_WAREHOUSE_ID), (5, 6, RETURN_WAREHOUSE_ID)]:
                request = (
                    'place=actual_delivery'
                    '&offers-list={}'
                    '&rids=213'
                    '&pickup-options=grouped'
                    '&pickup-options-extended-grouping=1'
                    '&is-return=1'
                    '&rearr-factors=market_return_to_wh={};enable_dropship_return_through_market_outlets=0'
                    '&combinator=0'
                )
                response = self.report.request_json(request.format(offer, warehouse))
                self.assertFragmentIn(
                    response,
                    {
                        "results": [
                            {
                                "delivery": {
                                    "pickupOptions": [
                                        {
                                            "serviceId": DELIVERY_SERVICE_ID,
                                            "dayFrom": day_from,
                                            "dayTo": day_to,
                                            "outletIds": [RETURN_OUTLET_ID],
                                        },
                                    ]
                                }
                            }
                        ]
                    },
                    allow_different_len=False,
                )

    def test_with_absent_warehouse(self):
        '''Проверяется, что при передаче несуществующего склада в параметре market_return_to_wh
        и наличии флага is-return, не будут рассчитаны опции доставки'''
        request = (
            'place=actual_delivery'
            '&offers-list=Sku1Price5-IiLVm1Goleg:1'
            '&rids=213'
            '&pickup-options=grouped'
            '&pickup-options-extended-grouping=1'
            '&is-return=1'
            '&rearr-factors=market_return_to_wh=175;enable_dropship_return_through_market_outlets=0'
            '&combinator=0'
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"results": [{"delivery": {"pickupOptions": Absent()}}]})

        self.error_log.expect(code=ErrorCodes.FAILED_TO_FIND_DELICALC_GENERATION_NUMBER_BY_WAREHOUSE_ID)
        self.error_log.expect(code=ErrorCodes.EXTREQUEST_DELIVERY_CALC_FATAL_ERROR)

    def test_with_incorrect_warehouse(self):
        '''Проверяется, что при передаче текста в качестве номера склада в параметре market_return_to_wh
        и наличии флага is-return репорт не упадет и установится склад Софьино, указанный по умолчанию'''
        request = (
            'place=actual_delivery'
            '&offers-list=Sku1Price5-IiLVm1Goleg:1'
            '&rids=213'
            '&pickup-options=grouped'
            '&pickup-options-extended-grouping=1'
            '&is-return=1'
            '&rearr-factors=market_return_to_wh=abc;enable_dropship_return_through_market_outlets=0'
            '&combinator=0'
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
                                    "serviceId": DELIVERY_SERVICE_ID,
                                    "dayFrom": 5,
                                    "dayTo": 6,
                                    "outletIds": [RETURN_OUTLET_ID],
                                },
                            ]
                        }
                    }
                ]
            },
            allow_different_len=False,
        )

        self.error_log.expect(code=ErrorCodes.CGI_CANNOT_PARSE_EXPERIMENTAL_FLAG)
        self.base_logs_storage.error_log.expect(code=ErrorCodes.CGI_CANNOT_PARSE_EXPERIMENTAL_FLAG)

    def test_post_as_outlets_with_is_return(self):
        '''Проверяется, что при флаге is-return=1 почтовые отделения добавляются в аутлеты'''
        request = (
            'place=actual_delivery'
            '&offers-list=Sku1Pr2990-IiLVm1Goleg:1'
            '&rids=213'
            '&pickup-options=grouped'
            '&pickup-options-extended-grouping=1'
            '&is-return=1'
            '&combinator=0'
            '&rearr-factors=enable_simple_return_by_post=0;'
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
                                    "serviceId": DELIVERY_SERVICE_ID,
                                    "dayFrom": 5,
                                    "dayTo": 6,
                                    "outletIds": [RETURN_OUTLET_ID],
                                },
                                {
                                    "serviceId": POST_DELIVERY_SERVICE_ID,
                                    "dayFrom": 4,
                                    "dayTo": 5,
                                    "outletIds": [POST_RETURN_OUTLET_ID],
                                },
                            ]
                        }
                    }
                ]
            },
            allow_different_len=False,
        )

        '''Проверяется, что без флага is-return=1 или c использованием флага enable_simple_return_by_post=1,
           почтовые отделение не добавляются в аутлеты'''
        base_request = (
            'place=actual_delivery'
            '&offers-list=Sku1Pr2990-IiLVm1Goleg:1'
            '&rids=213'
            '&pickup-options=grouped'
            '&pickup-options-extended-grouping=1'
            '&combinator=0'
        )

        enable_simple_return_by_post_rearr = '&rearr-factors=enable_simple_return_by_post=1'
        disable_post_as_pickup_rearr = '&rearr-factors=market_use_post_as_pickup=0'

        pickup_options = [
            {
                "serviceId": DELIVERY_SERVICE_ID,
                "dayFrom": 5,
                "dayTo": 6,
                "outletIds": [USUAL_OUTLET_ID, RETURN_OUTLET_ID],
            },
        ]
        post_options = [
            {
                "serviceId": POST_DELIVERY_SERVICE_ID,
                "dayFrom": 4,
                "dayTo": 5,
                "outletIds": [POST_USUAL_OUTLET_ID, POST_RETURN_OUTLET_ID],
            },
        ]
        iterable = itertools.product(
            ('', enable_simple_return_by_post_rearr),  # enable_simple_return_by_post
            ('', disable_post_as_pickup_rearr),  # disable_post_as_pickup
        )
        for enable_simple_return_by_post, disable_post_as_pickup in iterable:
            response = self.report.request_json(base_request + enable_simple_return_by_post + disable_post_as_pickup)
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "delivery": {
                                "pickupOptions": pickup_options
                                if disable_post_as_pickup
                                else pickup_options + post_options,
                                "postOptions": post_options if disable_post_as_pickup else Absent(),
                            }
                        }
                    ]
                },
                allow_different_len=False,
            )

        '''Проверяется, что установка market_return_to_wh=0 отключает добавление почтовых отделений в аутлеты'''
        request = (
            'place=actual_delivery'
            '&offers-list=Sku1Pr2990-IiLVm1Goleg:1'
            '&rids=213'
            '&pickup-options=grouped'
            '&pickup-options-extended-grouping=1'
            '&is-return=1'
            '&rearr-factors=market_return_to_wh=0;'
            '&combinator=0'
        )
        pickup_options = [
            {
                "serviceId": DELIVERY_SERVICE_ID,
                "dayFrom": 5,
                "dayTo": 6,
                "outletIds": [RETURN_OUTLET_ID],
            },
        ]
        post_options = [
            {
                "serviceId": POST_DELIVERY_SERVICE_ID,
                "dayFrom": 4,
                "dayTo": 5,
                "outletIds": [POST_RETURN_OUTLET_ID],
            },
        ]
        for disable_post_as_pickup in ['', disable_post_as_pickup_rearr]:
            response = self.report.request_json(request + disable_post_as_pickup)
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "delivery": {
                                "pickupOptions": pickup_options
                                if disable_post_as_pickup
                                else pickup_options + post_options,
                                "postOptions": post_options if disable_post_as_pickup else Absent(),
                            }
                        }
                    ]
                },
                allow_different_len=False,
            )

    RETURN_TARIFF_RID = 123

    @classmethod
    def prepare_return_tariffs(cls):
        cls.index.outlets += [
            Outlet(
                point_id=RETURN_TARIFF_OUTLET_ID,
                region=cls.RETURN_TARIFF_RID,
                point_type=Outlet.FOR_PICKUP,
                delivery_service_id=DELIVERY_SERVICE_ID,
                delivery_option=OutletDeliveryOption(
                    day_from=3, day_to=5, order_before=6, price=500, shipper_id=DELIVERY_SERVICE_ID
                ),
                working_days=[i for i in range(10)],
                bool_props=["returnAllowed"],
            ),
            Outlet(
                point_id=RETURN_TARIFF_OUTLET_ID2,
                region=cls.RETURN_TARIFF_RID,
                point_type=Outlet.FOR_PICKUP,
                delivery_service_id=RETURN_DELIVERY_SERVICE_ID,
                delivery_option=OutletDeliveryOption(
                    day_from=3, day_to=5, order_before=6, price=500, shipper_id=RETURN_DELIVERY_SERVICE_ID
                ),
                working_days=[i for i in range(10)],
                bool_props=["returnAllowed"],
            ),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=RETURN_TARIFFS_PICKUP_BUCKET_ID,
                dc_bucket_id=DC_RETURN_TARIFFS_BUCKET_ID,
                fesh=3,
                carriers=[DELIVERY_SERVICE_ID],
                options=[
                    PickupOption(outlet_id=RETURN_TARIFF_OUTLET_ID, day_from=3, day_to=5, price=500),
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=RETURN_TARIFFS_PICKUP_BUCKET_ID2,
                dc_bucket_id=DC_RETURN_TARIFFS_BUCKET_ID2,
                fesh=3,
                carriers=[RETURN_DELIVERY_SERVICE_ID],
                options=[
                    PickupOption(outlet_id=RETURN_TARIFF_OUTLET_ID2, day_from=3, day_to=5, price=500),
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        '''
        Подготавливаем возвратные тарифы
        '''
        RETURN_TARIFFS = [
            BlueDeliveryTariff(user_price=150),
        ]

        DEFAULT_RETURN_TARIFFS = [
            BlueDeliveryTariff(user_price=350),
        ]

        DEFAULT_TARIFFS = [
            BlueDeliveryTariff(user_price=100),
        ]

        DELIVERY_SERVICE_MODIFIER = [
            DeliveryServiceModifier(
                service_ids=[RETURN_DELIVERY_SERVICE_ID], tariffs=[BlueDeliveryTariff(user_price=380)]
            )
        ]

        # возвратный тариф в конкретный город
        cls.index.blue_delivery_modifiers.add_modifier(
            tariffs=RETURN_TARIFFS,
            regions=[213],
            is_return=True,
        )

        # дефолтный возвратный тариф
        cls.index.blue_delivery_modifiers.set_default_modifier(
            tariffs=DEFAULT_RETURN_TARIFFS,
            is_return=True,
            delivery_service_modifiers=DELIVERY_SERVICE_MODIFIER,
        )

        # дефолтный прямой тариф
        cls.index.blue_delivery_modifiers.set_default_modifier(
            tariffs=DEFAULT_TARIFFS,
        )

    def test_return_tariff(self):
        '''Проверяется, что при флаге is-return=1 используются возвратные тарифы'''

        'Запрашиваем возврат в Москве'
        request = (
            'place=actual_delivery'
            '&offers-list=Sku1Price5-IiLVm1Goleg:1'
            '&rids=213'
            '&pickup-options=grouped'
            '&pickup-options-extended-grouping=1'
            '&is-return=1'
            '&combinator=0'
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
                                    "price": {"value": "150"},
                                    "outletIds": [RETURN_OUTLET_ID],
                                },
                            ]
                        }
                    }
                ]
            },
            allow_different_len=False,
        )

        'Запрашиваем обычную доставку в регион, не заданный в тарифах. Для него должен примениться дефолтный тариф'
        request = (
            'place=actual_delivery'
            '&offers-list=Sku1Price5-IiLVm1Goleg:1'
            '&rids={}'
            '&pickup-options=grouped'
            '&pickup-options-extended-grouping=1'
            '&combinator=0'.format(self.RETURN_TARIFF_RID)
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
                                    "price": {"value": "100"},
                                    "outletIds": [RETURN_TARIFF_OUTLET_ID],
                                    "serviceId": DELIVERY_SERVICE_ID,
                                },
                                {
                                    "price": {"value": "100"},
                                    "outletIds": [RETURN_TARIFF_OUTLET_ID2],
                                    "serviceId": RETURN_DELIVERY_SERVICE_ID,
                                },
                            ]
                        }
                    }
                ]
            },
            allow_different_len=False,
        )

        'Запрашиваем возврат в регион, не заданный в тарифах. Для него должен примениться дефолтный возвратный тариф'
        'При этом для для службы RETURN_DELIVERY_SERVICE_ID должен примениться специально заданный для нее тариф'
        request = (
            'place=actual_delivery'
            '&offers-list=Sku1Price5-IiLVm1Goleg:1'
            '&rids={}'
            '&pickup-options=grouped'
            '&pickup-options-extended-grouping=1'
            '&is-return=1'
            '&combinator=0'.format(self.RETURN_TARIFF_RID)
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
                                    "price": {"value": "350"},
                                    "outletIds": [RETURN_TARIFF_OUTLET_ID],
                                    "serviceId": DELIVERY_SERVICE_ID,
                                },
                                {
                                    "price": {"value": "380"},
                                    "outletIds": [RETURN_TARIFF_OUTLET_ID2],
                                    "serviceId": RETURN_DELIVERY_SERVICE_ID,
                                },
                            ]
                        }
                    }
                ]
            },
            allow_different_len=False,
        )

    def test_combinator_return_options(self):
        "Проверяется, что под флагом enable_dropship_return_through_market_outlets"
        "происходит поход в новую ручку комбинатора для возвратов"
        for offer in [
            'Sku1Price5-IiLVm1Goleg:1',
            'Sku1Price5-IiLVm1Goleg:1;wh:145;ffWh:145;w:5;d:30x10x20;',
        ]:
            for is_return in [1, 0]:
                request = (
                    'place=actual_delivery'
                    '&offers-list={}'
                    '&rids=213'
                    '&pickup-options=grouped'
                    '&pickup-options-extended-grouping=1'
                    '&is-return=1'
                    '&rearr-factors=enable_dropship_return_through_market_outlets={}'
                    '&combinator=1'
                )
                response = self.report.request_json(request.format(offer, is_return))
                self.assertFragmentIn(
                    response,
                    {
                        "results": [
                            {
                                "delivery": {
                                    "options": [
                                        {
                                            "dayFrom": 1 if is_return else 2,
                                            "dayTo": 5 if is_return else 4,
                                            "timeIntervals": [{"from": "08:00", "to": "21:00", "isDefault": True}],
                                            "paymentMethods": ["YANDEX", "CASH_ON_DELIVERY", "CARD_ON_DELIVERY"],
                                        }
                                    ],
                                    "pickupOptions": [
                                        {
                                            "serviceId": 322,
                                            "dayFrom": 1 if is_return else 2,
                                            "dayTo": 5 if is_return else 4,
                                            "paymentMethods": ["CASH_ON_DELIVERY"],
                                            "outletIds": [1],
                                        }
                                    ],
                                }
                            }
                        ]
                    },
                    allow_different_len=False,
                )

    def test_combinator_return_options_use_post_as_pickup(self):
        """
        Проверяется, что для возвратной ручки игнорируется флаг почта как ПВЗ
        """
        request = (
            'place=actual_delivery'
            '&offers-list=Sku1Price5-IiLVm1Goleg:1'
            '&rids=213'
            '&pickup-options=grouped'
            '&pickup-options-extended-grouping=1'
            '&is-return=1'
            '&rearr-factors=enable_dropship_return_through_market_outlets=1;market_use_post_as_pickup=1'
            '&combinator=1'
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "delivery": {
                            "options": [
                                {
                                    "dayFrom": 1,
                                    "dayTo": 5,
                                    "timeIntervals": [{"from": "08:00", "to": "21:00", "isDefault": True}],
                                    "paymentMethods": ["YANDEX", "CASH_ON_DELIVERY", "CARD_ON_DELIVERY"],
                                }
                            ],
                            "pickupOptions": [
                                {
                                    "serviceId": 322,
                                    "dayFrom": 1,
                                    "dayTo": 5,
                                    "paymentMethods": ["CASH_ON_DELIVERY"],
                                    "outletIds": [1],
                                }
                            ],
                        }
                    }
                ]
            },
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()
