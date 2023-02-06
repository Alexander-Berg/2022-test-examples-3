#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

import datetime

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
    OrdersSplitRequest,
    OrdersSplitResponse,
    SplitBasket,
)
from core.types.currency import Currency
from core.types.delivery import (
    OutletDeliveryOption,
    OutletType,
)
from core.types.offer import OfferDimensions
from core.types.region import (
    GpsCoord,
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
from core.combinator import DeliveryStats

from market.combinator.proto.grpc.combinator_pb2 import (
    SplitStatus,
    PickupPointType,
)
from collections import namedtuple

CombinatorRequestItem = namedtuple("CombinatorRequestItem", "offer count warehouse")

# This date is fixed for all test
TODAY = datetime.date(2020, 5, 18)
DATE_FROM = TODAY + datetime.timedelta(days=2)
DATE_TO = TODAY + datetime.timedelta(days=4)

VIRTUAL_BOX = create_virtual_box(weight=18000, length=100, width=80, height=50)

# Regions
MOSCOW_RIDS = 213

BRANDED_OUTLET_OFFER_PRICE = 45

PLACE_COMBINE_WITH_RGB = "place=combine&rgb=green_with_blue&use-virt-shop=0"


class _Offers(object):
    offer1 = BlueOffer(
        price=BRANDED_OUTLET_OFFER_PRICE,
        vat=Vat.NO_VAT,
        feedid=4,
        offerid='blue.offer.branded.outlet',
        waremd5='SkuBrandedOultet45-1eg',
        weight=7,
        dimensions=OfferDimensions(length=113, width=77, height=52),
        stock_store_count=10,
        cargo_types=[600],
        supplier_id=1,
    )
    offer2 = BlueOffer(
        price=BRANDED_OUTLET_OFFER_PRICE * 2,
        vat=Vat.NO_VAT,
        feedid=5,
        offerid='blue.offer.branded.outlet2',
        waremd5='SkuBrandedOultet45-2eg',
        weight=3,
        dimensions=OfferDimensions(length=11, width=11, height=11),
        stock_store_count=10,
        cargo_types=[600],
        supplier_id=2,
    )


class _Mskus(object):
    msku1 = MarketSku(
        title="msku1",
        hyperid=1,
        sku=102,
        waremd5='BrandedOutletSku-sI1eg',
        blue_offers=[
            _Offers.offer1,
        ],
    )
    msku2 = MarketSku(
        title="msku2",
        hyperid=1,
        sku=103,
        waremd5='BrandedOutletSku-sI2eg',
        blue_offers=[
            _Offers.offer2,
        ],
    )


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.delivery_calendar_start_date = datetime.date(day=18, month=5, year=2020)
        cls.settings.report_subrole = 'blue-main'
        cls.settings.blue_market_free_delivery_threshold = 67
        cls.settings.default_search_experiment_flags += ['enable_dsbs_combinator_request_in_actual_delivery=0']
        # НЕ делайте так в новых тестах!
        # Походов в КД на проде уже нет, пожалуйста, проверяйте новую функциональность, создавая доставку через комбинатор
        cls.settings.default_search_experiment_flags += ['force_disable_delivery_calculator_requests=0']

        # Current date 18/05/2020 @ 23:16 MSK
        cls.settings.microseconds_for_disabled_random = 1589833013000000
        cls.index.shops += [
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
            Shop(
                fesh=5,
                datafeed_id=5,
                priority_region=MOSCOW_RIDS,
                name='blue_shop_3',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.THIRD_PARTY,
                blue='REAL',
                warehouse_id=145,
            ),
        ]

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
        ]

        cls.index.mskus += [_Mskus.msku1, _Mskus.msku2]

        cls.delivery_calc.on_request_offer_buckets(weight=7, width=77, height=52, length=113).respond([], [], [])
        cls.delivery_calc.on_request_offer_buckets(weight=3, width=11, height=11, length=11).respond([], [], [])

    @classmethod
    def prepare_courier_options(cls):
        # Добавляем курьерскую доставку с примеркой офферу offer1
        cls.combinator.on_courier_options_request(
            items=[
                DeliveryItem(
                    required_count=1,
                    weight=7000,
                    dimensions=[52, 77, 113],
                    cargo_types=[600],
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

    @classmethod
    def prepare_pickup_options(cls):
        # Добавляем доставку в пвз с примеркой офферу offer2
        cls.combinator.on_pickup_points_grouped_request(
            items=[
                DeliveryItem(
                    required_count=1,
                    weight=3000,
                    dimensions=[11, 11, 11],
                    cargo_types=[600],
                    offers=[
                        CombinatorOffer(
                            shop_sku="blue.offer.branded.outlet2",
                            shop_id=5,
                            partner_id=145,
                            available_count=1,
                        ),
                    ],
                    price=BRANDED_OUTLET_OFFER_PRICE * 2,
                ),
            ],
            destination_regions=[MOSCOW_RIDS],
            point_types=[],
            total_price=BRANDED_OUTLET_OFFER_PRICE * 2,
        ).respond_with_grouped_pickup_points(
            groups=[
                PickupPointGrouped(
                    ids_list=[2001],
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

    def request_combine(
        self, mskus, offers, region=213, count=1, flags='', no_replace_offers=[], marshrut_warehouse_priority=True
    ):
        request = PLACE_COMBINE_WITH_RGB + '&debug=1&rids={}'.format(region)
        assert len(mskus) == len(offers), 'len(mskus) == {} is not equal to len(offers) == {}'.format(
            len(mskus), len(offers)
        )

        request_offers = []
        for cart_item_id, (msku, offer) in enumerate(zip(mskus, offers)):
            no_replace = offer in set(no_replace_offers)
            request_offers += [
                '{}:{};msku:{};cart_item_id:{}{}'.format(
                    offer.waremd5,
                    count,
                    msku.sku,
                    cart_item_id + 1,
                    ';no_replace:1' if no_replace else '',
                )
            ]
        if request_offers:
            request += '&offers-list=' + ','.join(request_offers)

        request += '&rearr-factors=market_marshrut_warehouse_priority={market_marshrut_warehouse_priority}'.format(
            market_marshrut_warehouse_priority=1 if marshrut_warehouse_priority else 0
        )
        request += flags
        return self.report.request_json(request)

    @classmethod
    def mock_combinator_response(
        cls,
        request_orders,
        response_baskets,
        response_unreachable_items,
        add_delivery_stats=True,
        split_status=SplitStatus.SPLIT_OK,
        baskets_with_partial_delivery=[],
    ):
        cls.combinator.on_split_orders_request(
            orders=[
                OrdersSplitRequest(
                    order_id=id,
                    items=[
                        DeliveryItem(
                            required_count=count,
                            weight=offer.weight * 1000,
                            price=offer.price,
                            dimensions=[offer.dimensions.length, offer.dimensions.width, offer.dimensions.height],
                            cargo_types=offer.cargo_types,
                            offers=[
                                CombinatorOffer(
                                    shop_sku=offer.offerid,
                                    shop_id=offer.supplier_id,
                                    partner_id=warehouse,
                                    available_count=count,
                                )
                            ],
                        )
                        for offer, count, warehouse in order
                    ],
                )
                for id, order in enumerate(request_orders)
            ],
            destination=Destination(region_id=213),
        ).respond_with_split_orders(
            response_orders=[
                OrdersSplitResponse(
                    order_id=id,
                    status=split_status,
                    unreachable_items=[
                        DeliveryItem(
                            required_count=count,
                            weight=offer.weight * 1000,
                            price=offer.price,
                            dimensions=[offer.dimensions.length, offer.dimensions.width, offer.dimensions.height],
                            cargo_types=offer.cargo_types,
                            offers=[
                                CombinatorOffer(
                                    shop_sku=offer.offerid,
                                    shop_id=offer.supplier_id,
                                    partner_id=warehouse,
                                    available_count=count,
                                )
                            ],
                        )
                        for offer, count, warehouse in response_unreachable_items
                    ],
                    baskets=[
                        SplitBasket(
                            courier_stats=DeliveryStats(cost=10, day_from=1, day_to=3) if add_delivery_stats else None,
                            pickup_stats=DeliveryStats(cost=20, day_from=2, day_to=3) if add_delivery_stats else None,
                            post_stats=DeliveryStats(cost=30, day_from=3, day_to=3) if add_delivery_stats else None,
                            on_demand_stats=DeliveryStats(cost=40, day_from=2, day_to=4)
                            if add_delivery_stats
                            else None,
                            partial_delivery=baskets_with_partial_delivery[oid]
                            if len(baskets_with_partial_delivery) > oid
                            else False,
                            point_types=[PickupPointType.SERVICE_POINT, PickupPointType.POST_OFFICE],
                            items=[
                                DeliveryItem(
                                    required_count=count,
                                    weight=offer.weight * 1000,
                                    price=offer.price,
                                    dimensions=[
                                        offer.dimensions.length,
                                        offer.dimensions.width,
                                        offer.dimensions.height,
                                    ],
                                    cargo_types=offer.cargo_types,
                                    offers=[
                                        CombinatorOffer(
                                            shop_sku=offer.offerid,
                                            shop_id=offer.supplier_id,
                                            partner_id=warehouse,
                                            available_count=count,
                                        )
                                    ],
                                )
                                for offer, count, warehouse in order
                            ],
                        )
                        for oid, order in enumerate(basket)
                    ],
                )
                for id, basket in enumerate(response_baskets)
            ]
        )

    @classmethod
    def prepare_partial_delivery_computed(cls):
        offer1 = CombinatorRequestItem(offer=_Offers.offer1, count=1, warehouse=145)
        offer2 = CombinatorRequestItem(offer=_Offers.offer2, count=1, warehouse=145)
        order1 = [offer1]
        order2 = [offer2]

        cls.mock_combinator_response(
            request_orders=(order1,),
            response_baskets=(
                [
                    order1,
                ],
            ),
            response_unreachable_items=[],
            add_delivery_stats=False,
            baskets_with_partial_delivery=[False],
        )
        cls.mock_combinator_response(
            request_orders=(order2,),
            response_baskets=(
                [
                    order2,
                ],
            ),
            response_unreachable_items=[],
            add_delivery_stats=False,
            baskets_with_partial_delivery=[False],
        )

    def test_partial_delivery_computed_courier(self):
        # Проверяем что если комбинатор не вернул статистику, отдал isPartialDeliveryAvailable = False для бакета, isPartialDeliveryAvailable на выдаче будет True
        # так как у оффера в бакете есть курьерская доставка с примеркой в комбинаторе
        response = self.request_combine(
            [_Mskus.msku1],
            [_Offers.offer1],
            region=213,
            count=1,
            flags="&rearr-factors=enable_cart_split_on_combinator=1&combinator=1",
        )

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "buckets": [
                            {
                                "isPartialDeliveryAvailable": True,
                            },
                        ],
                    }
                ]
            },
            allow_different_len=False,
        )

    def test_partial_delivery_computed_pickup(self):
        # Проверяем что если комбинатор не вернул статистику, отдал isPartialDeliveryAvailable = False для бакета, isPartialDeliveryAvailable на выдаче будет True
        # так как у оффера в бакете есть доставка в ПВЗ с примеркой в комбинаторе
        response = self.request_combine(
            [_Mskus.msku2],
            [_Offers.offer2],
            region=213,
            count=1,
            flags="&rearr-factors=enable_cart_split_on_combinator=1&combinator=1",
        )

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "buckets": [
                            {
                                "isPartialDeliveryAvailable": True,
                            },
                        ],
                    }
                ]
            },
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()
