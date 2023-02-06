#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.types import (
    Currency,
    Dimensions,
    GpsCoord,
    Outlet,
    Region,
    Shop,
    DynamicWarehousesPriorityInRegion,
    DynamicWarehouseDelivery,
    DynamicDeliveryRestriction,
)

from core.matcher import EmptyList, Absent
from core.testcase import TestCase, main
from core.types.offer import OfferDimensions
from core.types.payment_methods import PaymentMethod
from core.types.sku import MarketSku, BlueOffer
from core.types.taxes import Vat, Tax
from core.types.delivery import OutletType, BlueDeliveryTariff
from core.types.combinator import (
    create_virtual_box,
    create_delivery_option,
    create_user_info,
    CombinatorOffer,
    DeliveryItem,
    DeliverySubtype,
    PickupPointGrouped,
    Destination,
)

import datetime


TODAY = datetime.datetime.today()
YESTERDAY = TODAY - datetime.timedelta(days=1)
TOMORROW = TODAY + datetime.timedelta(days=1)

# Коробов В.А
# Тестирование режимов работы actual_delivery в комбинации со способами доставки
# Пока что реализовано только для statistic

# ========== From C++ ==========


class EWorkMode:
    """
    see arcadia/market/report/library/cgi/types.h
    """

    WM_FULL = "full"
    WM_GEO = "geo"
    WM_STATISTIC = "statistic"


class EDeliveryMethod:
    """
    see arcadia/market/report/library/cgi/types.h
    """

    DM_PICKUP = "pickup"
    DM_COURIER = "courier"
    DM_POST = "post"
    DM_EXPRESS = "express"
    DM_ON_DEMAND = "on_demand"
    DM_ANY = "any"


# ========== Вспомогательные генераторы ==========


def id_generator(init_value=1):
    counter = init_value
    while True:
        yield counter
        counter += 1


SHOP_ID_GENERATOR = id_generator()
BUCKET_ID_GENERATOR = id_generator()
OUTLET_ID_GENERATOR = id_generator(100)

# ========== Регионы ==========

MOSCOW_RID = 213

# ========== Доставка ==========

DELIVERY_SERVICE_ID_MOSCOW = 123

# ========== Магазины ==========


class _Shops:
    class _Blue:
        B_1_ID = next(SHOP_ID_GENERATOR)
        B_1 = Shop(
            fesh=B_1_ID,
            datafeed_id=B_1_ID,
            priority_region=MOSCOW_RID,
            name='shop_blue_1',
            tax_system=Tax.OSN,
            supplier_type=Shop.THIRD_PARTY,
            blue=Shop.BLUE_REAL,
            warehouse_id=145,
        )

        E_1_ID = next(SHOP_ID_GENERATOR)
        E_1 = Shop(
            fesh=E_1_ID,
            datafeed_id=E_1_ID,
            priority_region=MOSCOW_RID,
            name='express_shop_blue_1',
            tax_system=Tax.OSN,
            supplier_type=Shop.THIRD_PARTY,
            blue=Shop.BLUE_REAL,
            warehouse_id=E_1_ID,
            with_express_warehouse=True,
        )

    class _White:
        W_1_ID = next(SHOP_ID_GENERATOR)
        W_1 = Shop(
            fesh=W_1_ID,
            priority_region=MOSCOW_RID,
            regions=[MOSCOW_RID],
            cpa=Shop.CPA_REAL,
            client_id=W_1_ID,
            datafeed_id=W_1_ID,
            name='shop_white_cpa_1',
            currency=Currency.RUR,
            tax_system=Tax.OSN,
        )


# ========== Оутлеты ==========


def create_pickup_outlet(
    outlet_id, rid, fesh=None, delivery_service_id=DELIVERY_SERVICE_ID_MOSCOW, gps_coord=GpsCoord(50.0, 50.0)
):
    return Outlet(
        fesh=fesh,
        point_id=outlet_id,
        post_code=outlet_id,
        delivery_service_id=delivery_service_id if fesh is None else None,
        region=rid,
        gps_coord=gps_coord,
        point_type=Outlet.FOR_PICKUP,
        working_days=[i for i in range(30)],
        bool_props=["prepayAllowed", "cashAllowed", "cardAllowed"],
        dimensions=Dimensions(width=1000, height=1000, length=1000),
    )


def create_post_outlet(
    post_code, rid, fesh=None, delivery_service_id=DELIVERY_SERVICE_ID_MOSCOW, gps_coord=GpsCoord(50.0, 50.0)
):
    return Outlet(
        point_id=post_code,
        fesh=fesh,
        post_code=post_code,
        delivery_service_id=delivery_service_id if fesh is None else None,
        region=rid,
        gps_coord=gps_coord,
        point_type=Outlet.FOR_POST,
        working_days=[i for i in range(30)],
        bool_props=["prepayAllowed", "cashAllowed", "cardAllowed"],
    )


class _Outlets:
    class _Pickup:
        BLUE_SHOP_1_MOSCOW_1_ID = next(OUTLET_ID_GENERATOR)
        SHOP_B_1_MOSCOW_1 = create_pickup_outlet(
            BLUE_SHOP_1_MOSCOW_1_ID, MOSCOW_RID, _Shops._Blue.B_1_ID, gps_coord=GpsCoord(52.5, 52.5)
        )

    class _Post:
        BLUE_SHOP_1_MOSCOW_1_ID = next(OUTLET_ID_GENERATOR)
        SHOP_B_1_MOSCOW_1 = create_post_outlet(BLUE_SHOP_1_MOSCOW_1_ID, MOSCOW_RID, _Shops._Blue.B_1_ID)


# ========== Офферы ==========


class _Offers:
    class _Blue:
        # Оффер с доставкой только pickup
        B_1 = BlueOffer(
            price=5,
            vat=Vat.VAT_10,
            waremd5='Sku1Price5-IiLVm1Goleg',
            weight=5,
            dimensions=OfferDimensions(length=20, width=30, height=10),
            post_term_delivery=True,
            cargo_types=[1, 2, 3],
            fesh=_Shops._Blue.B_1_ID,
            feedid=_Shops._Blue.B_1_ID,
            offerid='blue.offer.1.1',
        )

        # Оффер с доставкой только post
        B_2 = BlueOffer(
            price=5,
            vat=Vat.VAT_10,
            waremd5='Sku2Price5-IiLVm1Goleg',
            weight=5,
            dimensions=OfferDimensions(length=20, width=30, height=10),
            post_term_delivery=True,
            cargo_types=[1, 2, 3],
            fesh=_Shops._Blue.B_1_ID,
            feedid=_Shops._Blue.B_1_ID,
            offerid='blue.offer.1.2',
        )

        # Оффер с доставкой courier и on_demand
        B_3 = BlueOffer(
            price=5,
            vat=Vat.VAT_10,
            waremd5='Sku3Price5-IiLVm1Goleg',
            weight=5,
            dimensions=OfferDimensions(length=20, width=30, height=10),
            post_term_delivery=True,
            cargo_types=[1, 2, 3],
            fesh=_Shops._Blue.B_1_ID,
            feedid=_Shops._Blue.B_1_ID,
            offerid='blue.offer.1.3',
        )

        # Оффер с доставкой только on_demand
        B_4 = BlueOffer(
            price=5,
            vat=Vat.VAT_10,
            waremd5='Sku4Price5-IiLVm1Goleg',
            weight=5,
            dimensions=OfferDimensions(length=20, width=30, height=10),
            post_term_delivery=True,
            cargo_types=[1, 2, 3],
            fesh=_Shops._Blue.B_1_ID,
            feedid=_Shops._Blue.B_1_ID,
            offerid='blue.offer.1.4',
        )

        # Экспресс оффер
        E_1 = BlueOffer(
            price=5,
            vat=Vat.VAT_10,
            waremd5='Exp1Price5-IiLVm1Goleg',
            weight=5,
            dimensions=OfferDimensions(length=20, width=30, height=10),
            post_term_delivery=True,
            cargo_types=[1, 2, 3],
            fesh=_Shops._Blue.E_1_ID,
            feedid=_Shops._Blue.E_1_ID,
            offerid='express.offer.1.1',
            is_express=True,
        )


# ========== Представления офферов для mock-запросов в комбинатор ==========


class _CombinatorOffers:
    class _Blue:
        B_1 = CombinatorOffer(
            shop_sku='blue.offer.1.1',
            shop_id=_Shops._Blue.B_1_ID,
            partner_id=145,
            available_count=1,
        )
        B_2 = CombinatorOffer(
            shop_sku='blue.offer.1.2',
            shop_id=_Shops._Blue.B_1_ID,
            partner_id=145,
            available_count=1,
        )
        B_3 = CombinatorOffer(
            shop_sku='blue.offer.1.3',
            shop_id=_Shops._Blue.B_1_ID,
            partner_id=145,
            available_count=1,
        )
        B_4 = CombinatorOffer(
            shop_sku='blue.offer.1.4',
            shop_id=_Shops._Blue.B_1_ID,
            partner_id=145,
            available_count=1,
        )
        E_1 = CombinatorOffer(
            shop_sku='express.offer.1.1',
            shop_id=_Shops._Blue.E_1_ID,
            partner_id=_Shops._Blue.E_1_ID,
            available_count=1,
        )


B_1_delivery_item = DeliveryItem(
    required_count=1,
    weight=5000,
    dimensions=[10, 20, 30],
    cargo_types=[1, 2, 3],
    offers=[_CombinatorOffers._Blue.B_1],
    price=_Offers._Blue.B_1.price,
)

B_2_delivery_item = DeliveryItem(
    required_count=1,
    weight=5000,
    dimensions=[10, 20, 30],
    cargo_types=[1, 2, 3],
    offers=[_CombinatorOffers._Blue.B_2],
    price=_Offers._Blue.B_2.price,
)

B_3_delivery_item = DeliveryItem(
    required_count=1,
    weight=5000,
    dimensions=[10, 20, 30],
    cargo_types=[1, 2, 3],
    offers=[_CombinatorOffers._Blue.B_3],
    price=_Offers._Blue.B_3.price,
)

B_4_delivery_item = DeliveryItem(
    required_count=1,
    weight=5000,
    dimensions=[10, 20, 30],
    cargo_types=[1, 2, 3],
    offers=[_CombinatorOffers._Blue.B_4],
    price=_Offers._Blue.B_4.price,
)

E_1_delivery_item = DeliveryItem(
    required_count=1,
    weight=5000,
    dimensions=[10, 20, 30],
    cargo_types=[1, 2, 3],
    offers=[_CombinatorOffers._Blue.E_1],
    price=_Offers._Blue.E_1.price,
)

DELIVERY_SERVICE_ID_MOSCOW_delivery_option = create_delivery_option(
    cost=1,
    date_from=TODAY,
    date_to=TOMORROW,
    time_from=datetime.time(10, 0),
    time_to=datetime.time(22, 0),
    delivery_service_id=DELIVERY_SERVICE_ID_MOSCOW,
    payment_methods=[
        PaymentMethod.PT_YANDEX,
        PaymentMethod.PT_CARD_ON_DELIVERY,
    ],
)

DELIVERY_SERVICE_ID_MOSCOW_delivery_option_express = create_delivery_option(
    cost=1,
    date_from=TODAY,
    date_to=TOMORROW,
    time_from=datetime.time(10, 0),
    time_to=datetime.time(22, 0),
    delivery_service_id=DELIVERY_SERVICE_ID_MOSCOW,
    payment_methods=[
        PaymentMethod.PT_YANDEX,
        PaymentMethod.PT_CARD_ON_DELIVERY,
    ],
    is_wide_express=True,
    is_fastest_express=True,
)

DELIVERY_SERVICE_ID_MOSCOW_on_demand_delivery_option = create_delivery_option(
    cost=1,
    date_from=TODAY,
    date_to=TOMORROW,
    time_from=datetime.time(10, 0),
    time_to=datetime.time(22, 0),
    delivery_service_id=DELIVERY_SERVICE_ID_MOSCOW,
    delivery_subtype=DeliverySubtype.ON_DEMAND,
    payment_methods=[
        PaymentMethod.PT_YANDEX,
        PaymentMethod.PT_CARD_ON_DELIVERY,
    ],
)

VIRTUAL_BOX = create_virtual_box(weight=5000, length=100, width=100, height=100)


def create_post_point_grouped(post_ids):
    return PickupPointGrouped(
        ids_list=post_ids,
        post_ids=post_ids,
        outlet_type=OutletType.FOR_POST,
        service_id=DELIVERY_SERVICE_ID_MOSCOW,
        cost=1,
        date_from=TODAY,
        date_to=TOMORROW,
        payment_methods=[
            PaymentMethod.PT_YANDEX,
            PaymentMethod.PT_CASH_ON_DELIVERY,
        ],
    )


def create_pickup_point_grouped(point_ids):
    return PickupPointGrouped(
        ids_list=point_ids,
        post_ids=point_ids,
        outlet_type=OutletType.FOR_PICKUP,
        service_id=DELIVERY_SERVICE_ID_MOSCOW,
        cost=1,
        date_from=TODAY,
        date_to=TOMORROW,
        payment_methods=[
            PaymentMethod.PT_YANDEX,
            PaymentMethod.PT_CASH_ON_DELIVERY,
        ],
    )


# ========== Прочие вспомогательные методы ==========


def make_base_request(offers):
    return "place=actual_delivery" + "&offers-list={}".format(
        ",".join("{}:1".format(offer.waremd5) for offer in offers)
    )


def make_expected_available_methods(*available_methods):
    return {
        "search": {
            "results": [
                {
                    "delivery": {
                        "availableDeliveryMethods": [method for method in available_methods]
                        if available_methods
                        else EmptyList()
                    },
                    "offers": Absent(),
                    "fakeOffers": Absent(),
                    "parcelInfo": Absent(),
                }
            ]
        }
    }


# ========== Тесты ==========


class T(TestCase):
    @classmethod
    def prepare(cls):

        cls.index.mskus += [
            MarketSku(
                title="blue offer sku1",
                hyperid=1,
                sku=1,
                blue_offers=[_Offers._Blue.B_1],
                post_term_delivery=True,
            ),
            MarketSku(
                title="blue offer sku2",
                hyperid=2,
                sku=2,
                blue_offers=[_Offers._Blue.B_2],
                post_term_delivery=True,
            ),
            MarketSku(
                title="blue offer sku3",
                hyperid=3,
                sku=3,
                blue_offers=[_Offers._Blue.B_3],
                post_term_delivery=True,
            ),
            MarketSku(
                title="blue offer sku4",
                hyperid=4,
                sku=4,
                blue_offers=[_Offers._Blue.B_4],
                post_term_delivery=True,
            ),
            MarketSku(
                title="blue offer exp1",
                hyperid=5,
                sku=5,
                blue_offers=[_Offers._Blue.E_1],
            ),
        ]

        MSK_TARIFFS = [BlueDeliveryTariff(user_price=99, large_size=0, price_to=7), BlueDeliveryTariff(user_price=1)]
        # MSK_DSBS_TARIFFS = [BlueDeliveryTariff(user_price=5, large_size=0, price_to=7, dsbs_payment=6, is_dsbs_payment=True)]
        cls.index.blue_delivery_modifiers.add_modifier(tariffs=MSK_TARIFFS, regions=[MOSCOW_RID])
        # cls.index.blue_delivery_modifiers.set_default_modifier(tariffs=MSK_DSBS_TARIFFS, is_dsbs_payment=True)
        cls.index.blue_delivery_modifiers.set_default_modifier(tariffs=MSK_TARIFFS)

    @classmethod
    def prepare_regions(cls):
        cls.index.regiontree += [
            Region(
                rid=MOSCOW_RID,
                region_type=Region.CITY,
            ),
        ]

    @classmethod
    def prepare_outlets(cls):
        cls.index.outlets += [
            _Outlets._Pickup.SHOP_B_1_MOSCOW_1,
            _Outlets._Post.SHOP_B_1_MOSCOW_1,
        ]

    @classmethod
    def prepare_shops(cls):
        cls.index.shops += [
            _Shops._Blue.B_1,
            _Shops._Blue.E_1,
        ]

    @classmethod
    def prepare_combinator(cls):
        cls.settings.check_combinator_errors = False
        cls.settings.default_search_experiment_flags += ['enable_dsbs_combinator_request_in_actual_delivery=0']

    @classmethod
    def prepare_pickup_options(cls):
        cls.combinator.on_pickup_points_grouped_request(
            items=[B_1_delivery_item],
            destination_regions=[MOSCOW_RID],
            point_types=[],
            total_price=_Offers._Blue.B_1.price,
            post_codes=[],
        ).respond_with_grouped_pickup_points(
            groups=[
                create_pickup_point_grouped([_Outlets._Pickup.BLUE_SHOP_1_MOSCOW_1_ID]),
            ],
            virtual_box=VIRTUAL_BOX,
        )

    def test_pickup_options(self):
        """
        Проверяем получение pickup-опций в режимах 'full' (получаем список опций)
        и 'statistic' (получаем только информацию о наличии доставки)
        """
        expected_full = {
            "search": {
                "results": [
                    {
                        "delivery": {
                            "pickupOptions": [
                                {
                                    "serviceId": 123,
                                    "price": {"currency": "RUR", "value": "99"},
                                    "outletIds": [100],
                                }
                            ],
                        }
                    }
                ],
            },
        }
        base_request = make_base_request([_Offers._Blue.B_1]) + "&rids={}".format(MOSCOW_RID)
        # Если не передаем режим - работает в режиме 'full' по умолчанию
        request = base_request
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            expected_full,
            allow_different_len=False,
        )
        # Работа в режиме 'full'
        request = base_request + "&ad-work-mode={}".format(EWorkMode.WM_FULL)
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            expected_full,
            allow_different_len=False,
        )
        # Работа в режиме 'statistic', пытаемся получить все доступные способы доставки
        request = base_request + "&ad-work-mode={}".format(EWorkMode.WM_STATISTIC)
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            make_expected_available_methods(EDeliveryMethod.DM_PICKUP),
            allow_different_len=False,
        )
        # Работа в режиме 'statistic', пытаемся получить курьерку, но тогда способы доставки пусты
        request = (
            base_request
            + "&ad-work-mode={}".format(EWorkMode.WM_STATISTIC)
            + "&delivery-methods={}".format(EDeliveryMethod.DM_COURIER)
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            make_expected_available_methods(),
            allow_different_len=False,
        )

    @classmethod
    def prepare_post_options(cls):
        cls.combinator.on_pickup_points_grouped_request(
            items=[B_2_delivery_item],
            destination_regions=[MOSCOW_RID],
            point_types=[],
            total_price=_Offers._Blue.B_2.price,
            post_codes=[],
        ).respond_with_grouped_pickup_points(
            groups=[
                create_post_point_grouped([_Outlets._Post.BLUE_SHOP_1_MOSCOW_1_ID]),
            ],
            virtual_box=VIRTUAL_BOX,
        )

    @classmethod
    def prepare_delivery_statistics(cls):
        # по курьерке искусственно добавляем on_demand, чтобы проверить работу фильтра по методам доставки
        cls.combinator.on_courier_delivery_statistics_request(
            items=[B_3_delivery_item],
            destination=Destination(region_id=MOSCOW_RID),
            payment_methods=[],
            total_price=_Offers._Blue.B_3.price,
            user_info=create_user_info(logged_in=True),
            delivery_methods=[EDeliveryMethod.DM_ANY],
        ).respond_with_courier_delivery_statistics(
            delivery_methods=[EDeliveryMethod.DM_COURIER, EDeliveryMethod.DM_ON_DEMAND]
        )

        cls.combinator.on_courier_delivery_statistics_request(
            items=[B_3_delivery_item],
            destination=Destination(region_id=MOSCOW_RID),
            payment_methods=[],
            total_price=_Offers._Blue.B_3.price,
            user_info=create_user_info(logged_in=True),
            delivery_methods=[EDeliveryMethod.DM_ON_DEMAND],
        ).respond_with_courier_delivery_statistics(delivery_methods=[EDeliveryMethod.DM_ON_DEMAND])

        cls.combinator.on_pickup_delivery_statistics_request(
            items=[B_1_delivery_item],
            destination_regions=[MOSCOW_RID],
            point_types=[],
            total_price=_Offers._Blue.B_1.price,
            post_codes=[],
            delivery_methods=[EDeliveryMethod.DM_ANY],
        ).respond_with_pickup_delivery_statistics(delivery_methods=[EDeliveryMethod.DM_PICKUP])

        cls.combinator.on_pickup_delivery_statistics_request(
            items=[B_2_delivery_item],
            destination_regions=[MOSCOW_RID],
            point_types=[],
            total_price=_Offers._Blue.B_2.price,
            post_codes=[],
            delivery_methods=[EDeliveryMethod.DM_ANY],
        ).respond_with_pickup_delivery_statistics(delivery_methods=[EDeliveryMethod.DM_POST])

    def test_post_options(self):
        """
        Проверяем получение post-опций в режимах 'full' (получаем список опций)
        и 'statistic' (получаем только информацию о наличии доставки)
        """
        post_options = [
            {"paymentMethods": ["YANDEX", "CASH_ON_DELIVERY"], "outletIds": [101], "serviceId": 123},
        ]
        for disable_use_post_as_pickup in ['', '&rearr-factors=market_use_post_as_pickup=0']:
            expected_full = {
                "search": {
                    "results": [
                        {
                            "delivery": {
                                "pickupOptions": Absent() if disable_use_post_as_pickup else post_options,
                                "postOptions": post_options if disable_use_post_as_pickup else Absent(),
                            }
                        },
                    ],
                },
            }
            base_request = (
                make_base_request([_Offers._Blue.B_2]) + "&rids={}".format(MOSCOW_RID) + disable_use_post_as_pickup
            )
            # Если не передаем режим - работает в режиме 'full' по умолчанию
            request = base_request
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                expected_full,
                allow_different_len=False,
            )
            # Работа в режиме 'full'
            request = base_request + "&ad-work-mode={}".format(EWorkMode.WM_FULL)
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                expected_full,
                allow_different_len=False,
            )
            # Работа в режиме 'statistic', пытаемся получить все доступные способы доставки
            request = base_request + "&ad-work-mode={}".format(EWorkMode.WM_STATISTIC)
            response = self.report.request_json(request)
            if disable_use_post_as_pickup:
                self.assertFragmentIn(
                    response,
                    make_expected_available_methods(EDeliveryMethod.DM_POST),
                    allow_different_len=False,
                )
            else:
                self.assertFragmentIn(
                    response,
                    make_expected_available_methods(EDeliveryMethod.DM_PICKUP),
                    allow_different_len=False,
                )
            # Работа в режиме 'statistic', пытаемся получить курьерку, но тогда способы доставки пусты
            request = (
                base_request
                + "&ad-work-mode={}".format(EWorkMode.WM_STATISTIC)
                + "&delivery-methods={}".format(EDeliveryMethod.DM_COURIER)
            )
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                make_expected_available_methods(),
                allow_different_len=False,
            )

    @classmethod
    def prepare_courier_options(cls):
        cls.combinator.on_courier_options_request(
            items=[B_3_delivery_item],
            destination=Destination(region_id=MOSCOW_RID),
            payment_methods=[],
            total_price=_Offers._Blue.B_3.price,
            user_info=create_user_info(logged_in=True),
        ).respond_with_courier_options(
            options=[DELIVERY_SERVICE_ID_MOSCOW_delivery_option, DELIVERY_SERVICE_ID_MOSCOW_on_demand_delivery_option],
            virtual_box=VIRTUAL_BOX,
        )

    def test_courier_options(self):
        """
        Проверяем получение courier-опций в режимах 'full' (получаем список опций)
        и 'statistic' (получаем только информацию о наличии доставки)
        """
        expected_full = {
            "search": {
                "results": [
                    {
                        "delivery": {
                            "options": [
                                {
                                    "paymentMethods": ["YANDEX", "CASH_ON_DELIVERY", "CARD_ON_DELIVERY"],
                                    "price": {"currency": "RUR", "value": "99"},
                                    "serviceId": "123",
                                },
                                {
                                    "paymentMethods": ["YANDEX", "CASH_ON_DELIVERY", "CARD_ON_DELIVERY"],
                                    "price": {"currency": "RUR", "value": "99"},
                                    "serviceId": "123",
                                },
                            ]
                        }
                    }
                ],
            },
        }
        base_request = make_base_request([_Offers._Blue.B_3]) + "&rids={}".format(MOSCOW_RID) + "&logged-in=1"
        # Если не передаем режим - работает в режиме 'full' по умолчанию
        request = base_request
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            expected_full,
            allow_different_len=False,
        )
        # Работа в режиме 'full'
        request = base_request + "&ad-work-mode={}".format(EWorkMode.WM_FULL)
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            expected_full,
            allow_different_len=False,
        )
        # Работа в режиме 'statistic', пытаемся получить все доступные способы доставки
        request = base_request + "&ad-work-mode={}".format(EWorkMode.WM_STATISTIC)
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            make_expected_available_methods(EDeliveryMethod.DM_COURIER, EDeliveryMethod.DM_ON_DEMAND),
            allow_different_len=False,
        )
        # Работа в режиме 'statistic', пытаемся получить почту, но тогда способы доставки пусты
        request = (
            base_request
            + "&ad-work-mode={}".format(EWorkMode.WM_STATISTIC)
            + "&delivery-methods={}".format(EDeliveryMethod.DM_POST)
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            make_expected_available_methods(),
            allow_different_len=False,
        )

    @classmethod
    def prepare_on_demand_options(cls):
        cls.combinator.on_courier_options_request(
            items=[B_4_delivery_item],
            destination=Destination(region_id=MOSCOW_RID),
            payment_methods=[],
            total_price=_Offers._Blue.B_4.price,
            user_info=create_user_info(logged_in=True),
        ).respond_with_courier_options(
            options=[DELIVERY_SERVICE_ID_MOSCOW_on_demand_delivery_option],
            virtual_box=VIRTUAL_BOX,
        )

    def test_on_demand_options(self):
        """
        Проверяем получение on_demand-опций в режимах 'full' (получаем список опций)
        и 'statistic' (получаем только информацию о наличии доставки)
        """
        expected_full = {
            "search": {
                "results": [
                    {
                        "delivery": {
                            "options": [
                                {
                                    "paymentMethods": ["YANDEX", "CASH_ON_DELIVERY", "CARD_ON_DELIVERY"],
                                    "price": {"currency": "RUR", "value": "99"},
                                    "serviceId": "123",
                                    "isWideExpress": False,
                                    "isFastestExpress": False,
                                }
                            ]
                        }
                    }
                ],
            },
        }
        base_request = make_base_request([_Offers._Blue.B_4]) + "&rids={}".format(MOSCOW_RID) + "&logged-in=1"
        # Если не передаем режим - работает в режиме 'full' по умолчанию
        request = base_request
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            expected_full,
            allow_different_len=False,
        )
        # Работа в режиме 'full'
        request = base_request + "&ad-work-mode={}".format(EWorkMode.WM_FULL)
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            expected_full,
            allow_different_len=False,
        )
        # Работа в режиме 'statistic', пытаемся получить все доступные способы доставки
        request = base_request + "&ad-work-mode={}".format(EWorkMode.WM_STATISTIC)
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            make_expected_available_methods(EDeliveryMethod.DM_ON_DEMAND),
            allow_different_len=False,
        )
        # Работа в режиме 'statistic', пытаемся получить курьерку, тогда способ доставки по клику
        # расценивается как способ доставки курьеркой
        request = (
            base_request
            + "&ad-work-mode={}".format(EWorkMode.WM_STATISTIC)
            + "&delivery-methods={}".format(EDeliveryMethod.DM_COURIER)
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            make_expected_available_methods(EDeliveryMethod.DM_COURIER),
            allow_different_len=False,
        )
        # Работа в режиме 'statistic', пытаемся получить ПВЗ, но тогда способы доставки пусты
        request = (
            base_request
            + "&ad-work-mode={}".format(EWorkMode.WM_STATISTIC)
            + "&delivery-methods={}".format(EDeliveryMethod.DM_PICKUP)
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            make_expected_available_methods(),
            allow_different_len=False,
        )

    @classmethod
    def prepare_express_options(cls):
        cls.combinator.on_courier_options_request(
            items=[E_1_delivery_item],
            destination=Destination(region_id=MOSCOW_RID),
            payment_methods=[],
            total_price=_Offers._Blue.E_1.price,
        ).respond_with_courier_options(
            options=[DELIVERY_SERVICE_ID_MOSCOW_delivery_option_express],
            virtual_box=VIRTUAL_BOX,
        )

    @classmethod
    def prepare_lms(cls):
        cls.dynamic.lms += [
            DynamicWarehousesPriorityInRegion(
                region=MOSCOW_RID,
                warehouses=[_Shops._Blue.E_1_ID],
            ),
        ]

    @classmethod
    def prepare_nordstream(cls):
        cls.dynamic.nordstream += [
            DynamicWarehouseDelivery(
                _Shops._Blue.E_1_ID,
                {
                    MOSCOW_RID: [
                        DynamicDeliveryRestriction(
                            max_phys_weight=100000,
                            max_dim_sum=300,
                            max_dimensions=[100, 100, 100],
                            min_days=0,
                            max_days=4,
                        ),
                    ],
                },
            ),
        ]

    def test_express_options(self):
        """
        Проверяем получение express-опций (courier-опции для express-офферов) в режимах 'full' (получаем список опций)
        и 'statistic' (получаем только информацию о наличии доставки)
        """
        expected_full = {
            "search": {
                "results": [
                    {
                        "delivery": {
                            "options": [
                                {
                                    "paymentMethods": ["YANDEX", "CASH_ON_DELIVERY", "CARD_ON_DELIVERY"],
                                    "price": {"currency": "RUR", "value": "99"},
                                    "serviceId": "123",
                                    "isWideExpress": True,
                                    "isFastestExpress": True,
                                }
                            ]
                        }
                    }
                ],
            },
        }

        base_request = (
            make_base_request([_Offers._Blue.E_1])
            + "&rids={}".format(MOSCOW_RID)
            + "&rearr-factors=market_use_business_offer=1"
        )
        # Если не передаем режим - работает в режиме 'full' по умолчанию
        request = base_request
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            expected_full,
            allow_different_len=False,
        )
        # Работа в режиме 'full'
        request = base_request + "&ad-work-mode={}".format(EWorkMode.WM_FULL)
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            expected_full,
            allow_different_len=False,
        )
        # Работа в режиме 'statistic', пытаемся получить все доступные способы доставки
        request = base_request + "&ad-work-mode={}".format(EWorkMode.WM_STATISTIC)
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            make_expected_available_methods(EDeliveryMethod.DM_EXPRESS),
            allow_different_len=False,
        )
        # Работа в режиме 'statistic', пытаемся получить курьерку - экспресс расценивается как курьерка
        request = (
            base_request
            + "&ad-work-mode={}".format(EWorkMode.WM_STATISTIC)
            + "&delivery-methods={}".format(EDeliveryMethod.DM_COURIER)
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            make_expected_available_methods(EDeliveryMethod.DM_COURIER),
            allow_different_len=False,
        )
        # Работа в режиме 'statistic', пытаемся получить ПВЗ, но тогда способы доставки пусты
        request = (
            base_request
            + "&ad-work-mode={}".format(EWorkMode.WM_STATISTIC)
            + "&delivery-methods={}".format(EDeliveryMethod.DM_PICKUP)
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            make_expected_available_methods(),
            allow_different_len=False,
        )

    '''
    В тестах ниже проверяем новый режим статистики по методам доставки в actual_delivery
    В этом режиме мы получаем статистику из комбинатора
    Проверяем не только то, что мы получаем статистику в новом режиме, но и сокращение выдачи
    '''

    def test_combinator_delivery_statistics_pickup(self):
        # Проверка статистики по методам доставки для самовывоза
        request = make_base_request([_Offers._Blue.B_1]) + "&rids={rid}&ad-work-mode={mode}".format(
            rid=MOSCOW_RID, mode=EWorkMode.WM_STATISTIC
        )
        request = request + "&rearr-factors=market_report_use_combinator_delivery_statistics={}"
        for stats_enabled in [0, 1]:
            response = self.report.request_json(request.format(stats_enabled))
            self.assertFragmentIn(response, make_expected_available_methods(EDeliveryMethod.DM_PICKUP))

    def test_combinator_delivery_statistics_courier(self):
        # Проверка статистики по методам доставки для самовывоза
        request = (
            make_base_request([_Offers._Blue.B_3])
            + "&rids={rid}&ad-work-mode={mode}".format(rid=MOSCOW_RID, mode=EWorkMode.WM_STATISTIC)
            + "&logged-in=1"
        )
        request = request + "&rearr-factors=market_report_use_combinator_delivery_statistics={}"
        for stats_enabled in [0, 1]:
            response = self.report.request_json(request.format(stats_enabled))
            self.assertFragmentIn(
                response, make_expected_available_methods(EDeliveryMethod.DM_COURIER, EDeliveryMethod.DM_ON_DEMAND)
            )
        # проверяем работу фильтра по способам доставки
        with_filter_request = request.format(1) + '&delivery-methods=on_demand'
        response = self.report.request_json(with_filter_request)
        self.assertFragmentIn(response, make_expected_available_methods(EDeliveryMethod.DM_ON_DEMAND))

    def test_combinator_delivery_statistics_post(self):
        # Проверка статистики по методам доставки для самовывоза
        request = make_base_request([_Offers._Blue.B_2]) + "&rids={rid}&ad-work-mode={mode}".format(
            rid=MOSCOW_RID, mode=EWorkMode.WM_STATISTIC
        )
        request = (
            request + "&rearr-factors=market_use_post_as_pickup=0;market_report_use_combinator_delivery_statistics={}"
        )
        for stats_enabled in [0, 1]:
            response = self.report.request_json(request.format(stats_enabled))
            self.assertFragmentIn(response, make_expected_available_methods(EDeliveryMethod.DM_POST))

    def test_delivery_reminders_in_statistic_mode(self):
        '''
        Тут мы проверяем, что и в старом, и в новом режиме статистик возвращаются
        напоминания о более дешёвой доставке (работает метод FillDeliveryReminders)
        '''
        request = make_base_request([_Offers._Blue.B_3]) + "&rids={rid}&ad-work-mode={mode}".format(
            rid=MOSCOW_RID, mode=EWorkMode.WM_STATISTIC
        )
        stats_modes = ['']
        stats_modes.extend(
            [
                '&rearr-factors=market_report_use_combinator_delivery_statistics={flag}'.format(flag=flag)
                for flag in [0, 1]
            ]
        )
        for stats_mode in stats_modes:
            response = self.report.request_json(request + stats_mode)
            self.assertFragmentIn(
                response,
                {
                    "cheaperDeliveryThreshold": {"currency": "RUR", "value": "7"},
                    "cheaperDeliveryRemainder": {"currency": "RUR", "value": "2"},
                },
            )


if __name__ == '__main__':
    main()
