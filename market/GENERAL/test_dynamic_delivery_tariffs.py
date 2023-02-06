#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.types import (
    Region,
    Shop,
    DynamicDeliveryTariff,
    Currency,
    DeliveryOption,
    Dimensions,
    GpsCoord,
    HyperCategory,
    DynamicWarehouseInfo,
    Outlet,
    Offer,
    DynamicWarehousesPriorityInRegion,
    DynamicWarehouseDelivery,
    DynamicDeliveryRestriction,
    ExpressDeliveryService,
    ExpressSupplier,
)
from core.logs import ErrorCodes
from core.types.offer import OfferDimensions
from core.types.delivery import OutletType, BlueDeliveryTariff
from core.types.payment_methods import PaymentMethod
from core.types.sku import MarketSku, BlueOffer
from core.types.taxes import Vat, Tax
from core.testcase import TestCase, main
from core.matcher import (
    Absent,
    Contains,
)
from core.types.combinator import (
    create_virtual_box,
    create_delivery_option,
    CombinatorOffer,
    DeliveryItem,
    PickupPointGrouped,
    Destination,
)

import datetime

UNIFIED_EXP = 'unified'
UNIFIED_RID = 213

TODAY = datetime.datetime.today()
YESTERDAY = TODAY - datetime.timedelta(days=1)
TOMORROW = TODAY + datetime.timedelta(days=1)

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


class _Regions:
    SAINT_PETERSBURG = 2
    KRASNODAR = 35
    MOSCOW = 213
    SVERDLOVSK_OBLAST = 11162

    class _SverdlovskOblast:
        VOLCHANSKY_URBAN_OKRUG = 114719

        class _VolchanskyUrbanOkrug:
            VOLCHANSK = 103820

    UNKNOWN = 123456


# ========== Сервисы доставки ==========

DELIVERY_SERVICE_ID_MOSCOW = 123
DELIVERY_SERVICE_ID_VOLCHANSK = 124

# ========== Категории ==========


class _Categories:
    class _FMCG:
        BEAUTY = 90509  # Товары для красоты

        class _Beauty:
            VALUE_1 = 190509  # Какая-то подкатегория

        ANIMALS = 90813  # Товары для животных
        FOOD = 91307  # Продукты
        HOME = 90666  # Товары для дома


# ========== Магазины ==========


class _Shops:
    class _Blue:
        B_1_ID = next(SHOP_ID_GENERATOR)
        B_1 = Shop(
            fesh=B_1_ID,
            datafeed_id=B_1_ID,
            priority_region=_Regions.MOSCOW,
            name='shop_blue_1',
            tax_system=Tax.OSN,
            supplier_type=Shop.THIRD_PARTY,
            blue=Shop.BLUE_REAL,
            warehouse_id=145,
        )

        B_2_ID = next(SHOP_ID_GENERATOR)
        B_2 = Shop(
            fesh=B_2_ID,
            datafeed_id=B_2_ID,
            priority_region=2,
            currency=Currency.RUR,
            tax_system=Tax.OSN,
            supplier_type=Shop.FIRST_PARTY,
            blue=Shop.BLUE_REAL,
            warehouse_id=145,
        )

        # Express-магазин (для офферов не вычисляется ExtraCharge)
        E_1_ID = next(SHOP_ID_GENERATOR)
        E_1 = Shop(
            fesh=E_1_ID,
            datafeed_id=E_1_ID,
            priority_region=_Regions.MOSCOW,
            name='express_shop_blue_1',
            tax_system=Tax.OSN,
            supplier_type=Shop.THIRD_PARTY,
            blue=Shop.BLUE_REAL,
            warehouse_id=E_1_ID,
            with_express_warehouse=True,
        )

        # Shops from table https://st.yandex-team.ru/MARKETOUT-46981

        B_3_ID = next(SHOP_ID_GENERATOR)
        B_3 = Shop(
            fesh=B_3_ID,
            datafeed_id=B_3_ID,
            priority_region=_Regions._SverdlovskOblast._VolchanskyUrbanOkrug.VOLCHANSK,
            currency=Currency.RUR,
            tax_system=Tax.OSN,
            supplier_type=Shop.FIRST_PARTY,
            blue=Shop.BLUE_REAL,
            warehouse_id=B_3_ID,
            fulfillment_program=True,
        )

    class _White:
        # DSBS-магазин (для офферов не вычисляется ExtraCharge)
        DSBS_1_ID = next(SHOP_ID_GENERATOR)
        DSBS_1 = Shop(
            fesh=DSBS_1_ID,
            priority_region=_Regions.MOSCOW,
            regions=[_Regions.MOSCOW],
            cpa=Shop.CPA_REAL,
            client_id=DSBS_1_ID,
            datafeed_id=DSBS_1_ID,
            name='shop_white_cpa_1',
            currency=Currency.RUR,
            tax_system=Tax.OSN,
            business_fesh=DSBS_1_ID,
            warehouse_id=DSBS_1_ID,
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
            BLUE_SHOP_1_MOSCOW_1_ID, _Regions.MOSCOW, _Shops._Blue.B_1_ID, gps_coord=GpsCoord(52.5, 52.5)
        )

    class _Post:
        BLUE_SHOP_1_MOSCOW_1_ID = next(OUTLET_ID_GENERATOR)
        SHOP_B_1_MOSCOW_1 = create_post_outlet(BLUE_SHOP_1_MOSCOW_1_ID, _Regions.MOSCOW, _Shops._Blue.B_1_ID)


# ========== Фидовые опции ==========


class _DsbsFeedOption:
    OPTION = DeliveryOption(price=2, day_from=0, day_to=3, order_before=24)


# ========== Офферы ==========


class _Offers:
    class _Blue:
        SB_2_B_1 = BlueOffer(
            price=5,
            vat=Vat.VAT_10,
            feedid=_Shops._Blue.B_2_ID,
            waremd5='Sku1Price5-IiLVm1Goleg',
            offerid="blue.offer.2.1",
            weight=5,
            dimensions=OfferDimensions(length=30, width=10, height=20),
            post_term_delivery=True,
        )

        SB_2_B_2 = BlueOffer(
            price=5,
            vat=Vat.VAT_10,
            feedid=_Shops._Blue.B_2_ID,
            waremd5='Sku2Price5-IiLVm1Goleg',
            offerid="blue.offer.2.2",
            weight=20,
            dimensions=OfferDimensions(length=0, width=0, height=0),
            post_term_delivery=True,
        )

        # Экспресс оффер
        SE_1_E_1 = BlueOffer(
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

        # Offers from table https://st.yandex-team.ru/MARKETOUT-46981

        PROFESSIONAL_PERFECT_HAIR = BlueOffer(
            title='OLLIN Professional Perfect Hair несмываемый крем-спрей 15 в 1, 250 мл, бутылка',
            waremd5='OLLINPP495-IiLVm1Goleg',
            hid=_Categories._FMCG._Beauty.VALUE_1,
            feedid=_Shops._Blue.B_3_ID,
            fesh=_Shops._Blue.B_3_ID,
            offerid='ollin.professional',
            price=495,
            weight=0.28,  # 280 грамм
            dimensions=OfferDimensions(length=10, width=11, height=6),  # 660 объем
        )

    class _White:
        SD_1_DSBS_1 = Offer(
            title="DSBS Offer",
            offerid='dsbs.offer.1.1',
            price=2,
            dimensions=OfferDimensions(length=20, width=30, height=10),
            weight=5,
            feedid=_Shops._White.DSBS_1_ID,
            fesh=_Shops._White.DSBS_1_ID,
            business_id=_Shops._White.DSBS_1_ID,
            cpa=Offer.CPA_REAL,
            waremd5='Dsbs1Price2-ILVm1Goleg',
            cargo_types=[256, 10],
            delivery_options=[_DsbsFeedOption.OPTION],
            pickup_option=None,
            pickup_buckets=[],
        )


# ========== Представления офферов для mock-запросов в комбинатор ==========


class _CombinatorOffers:
    class _Blue:
        SB_2_B_1 = CombinatorOffer(
            shop_sku='blue.offer.2.1',
            shop_id=_Shops._Blue.B_2_ID,
            partner_id=145,
            available_count=1,
        )
        SB_2_B_2 = CombinatorOffer(
            shop_sku='blue.offer.2.2',
            shop_id=_Shops._Blue.B_2_ID,
            partner_id=145,
            available_count=1,
        )

        SE_1_E_1 = CombinatorOffer(
            shop_sku='express.offer.1.1',
            shop_id=_Shops._Blue.E_1_ID,
            partner_id=_Shops._Blue.E_1_ID,
            available_count=1,
        )

        # Offers from table https://st.yandex-team.ru/MARKETOUT-46981

        PROFESSIONAL_PERFECT_HAIR = CombinatorOffer(
            shop_sku='ollin.professional',
            shop_id=_Shops._Blue.B_3_ID,
            partner_id=_Shops._Blue.B_3_ID,
            available_count=1,
        )

    class _White:
        SD_1_DSBS_1 = CombinatorOffer(
            shop_sku='dsbs.offer.1.1',
            shop_id=_Shops._White.DSBS_1_ID,
            feed_id=_Shops._White.DSBS_1_ID,
            partner_id=_Shops._White.DSBS_1_ID,
            available_count=1,
            offer_feed_delivery_option=_DsbsFeedOption.OPTION,
        )


class _CombinatorDeliveryItems:
    class _Blue:
        SB_2_B_1 = DeliveryItem(
            required_count=1,
            weight=_Offers._Blue.SB_2_B_1.weight * 1000,
            dimensions=[
                _Offers._Blue.SB_2_B_1.dimensions.width,
                _Offers._Blue.SB_2_B_1.dimensions.height,
                _Offers._Blue.SB_2_B_1.dimensions.length,
            ],
            cargo_types=[],
            offers=[_CombinatorOffers._Blue.SB_2_B_1],
            price=_Offers._Blue.SB_2_B_1.price,
        )

        SB_2_B_2 = DeliveryItem(
            required_count=1,
            weight=_Offers._Blue.SB_2_B_2.weight * 1000,
            dimensions=[
                _Offers._Blue.SB_2_B_2.dimensions.width,
                _Offers._Blue.SB_2_B_2.dimensions.height,
                _Offers._Blue.SB_2_B_2.dimensions.length,
            ],
            cargo_types=[],
            offers=[_CombinatorOffers._Blue.SB_2_B_2],
            price=_Offers._Blue.SB_2_B_2.price,
        )

        SE_1_E_1 = DeliveryItem(
            required_count=1,
            weight=_Offers._Blue.SE_1_E_1.weight * 1000,
            dimensions=[
                _Offers._Blue.SE_1_E_1.dimensions.width,
                _Offers._Blue.SE_1_E_1.dimensions.height,
                _Offers._Blue.SE_1_E_1.dimensions.length,
            ],
            cargo_types=_Offers._Blue.SE_1_E_1.cargo_types,
            offers=[_CombinatorOffers._Blue.SE_1_E_1],
            price=_Offers._Blue.SE_1_E_1.price,
        )

        # Offers from table https://st.yandex-team.ru/MARKETOUT-46981

        PROFESSIONAL_PERFECT_HAIR = DeliveryItem(
            required_count=1,
            weight=int(_Offers._Blue.PROFESSIONAL_PERFECT_HAIR.weight * 1000),
            dimensions=[
                _Offers._Blue.PROFESSIONAL_PERFECT_HAIR.dimensions.width,
                _Offers._Blue.PROFESSIONAL_PERFECT_HAIR.dimensions.height,
                _Offers._Blue.PROFESSIONAL_PERFECT_HAIR.dimensions.length,
            ],
            cargo_types=[],
            offers=[_CombinatorOffers._Blue.PROFESSIONAL_PERFECT_HAIR],
            price=_Offers._Blue.PROFESSIONAL_PERFECT_HAIR.price,
        )

    class _White:
        SD_1_DSBS_1 = DeliveryItem(
            required_count=1,
            weight=_Offers._White.SD_1_DSBS_1.weight * 1000,
            dimensions=[
                _Offers._White.SD_1_DSBS_1.dimensions.width,
                _Offers._White.SD_1_DSBS_1.dimensions.height,
                _Offers._White.SD_1_DSBS_1.dimensions.length,
            ],
            cargo_types=[10, 256],
            offers=[_CombinatorOffers._White.SD_1_DSBS_1],
            price=_Offers._White.SD_1_DSBS_1.price,
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

DELIVERY_SERVICE_ID_VOLCHANSK_delivery_options = create_delivery_option(
    cost=1,
    date_from=TODAY,
    date_to=TOMORROW,
    time_from=datetime.time(10, 0),
    time_to=datetime.time(22, 0),
    delivery_service_id=DELIVERY_SERVICE_ID_VOLCHANSK,
    payment_methods=[
        PaymentMethod.PT_YANDEX,
        PaymentMethod.PT_CARD_ON_DELIVERY,
    ],
    is_market_courier=True,
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


# ========== Тарифы ==========


class Unified(object):
    tariffs = [
        BlueDeliveryTariff(
            large_size=1,
            user_price=10,
            courier_price=20,
            pickup_price=30,
            post_price=40,
        ),
    ]

    tariffs_default = [
        BlueDeliveryTariff(
            large_size=0,
            user_price=99,
            courier_price=199,
            pickup_price=299,
            post_price=399,
        ),
        BlueDeliveryTariff(
            large_size=1,
            user_price=100,
            courier_price=200,
            pickup_price=300,
            post_price=400,
        ),
    ]


# ========== Прочие вспомогательные методы ==========


def make_base_request(offers):
    return (
        "place=actual_delivery"
        + "&offers-list={}".format(",".join("{}:1".format(offer.waremd5) for offer in offers))
        + '&rearr-factors=market_dynamic_delivery_tariffs=1'
        + '&debug=true'
    )


# ========== Тесты ==========


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.blue_delivery_modifiers.add_modifier(
            exp_name=UNIFIED_EXP, tariffs=Unified.tariffs, regions=[UNIFIED_RID]
        )
        cls.index.blue_delivery_modifiers.set_default_modifier(exp_name=UNIFIED_EXP, tariffs=Unified.tariffs_default)
        MSK_TARIFFS = [BlueDeliveryTariff(user_price=99, large_size=0, price_to=7), BlueDeliveryTariff(user_price=1)]
        cls.index.blue_delivery_modifiers.add_modifier(tariffs=MSK_TARIFFS, regions=[_Regions.MOSCOW])
        cls.index.blue_delivery_modifiers.set_default_modifier(tariffs=MSK_TARIFFS)

        cls.index.express_partners.suppliers += [
            ExpressSupplier(
                feed_id=_Shops._Blue.E_1_ID,
                supplier_id=_Shops._Blue.E_1_ID,
                warehouse_id=_Shops._Blue.E_1_ID,
            ),
        ]
        cls.index.express_partners.delivery_services += [
            ExpressDeliveryService(delivery_service_id=DELIVERY_SERVICE_ID_MOSCOW, delivery_price_for_user=350)
        ]

    @classmethod
    def prepare_hypertree(cls):
        cls.index.hypertree = [
            HyperCategory(
                hid=_Categories._FMCG.BEAUTY,
                children=[HyperCategory(hid=_Categories._FMCG._Beauty.VALUE_1)],
            ),
        ]

    @classmethod
    def prepare_region_tree(cls):
        # Используйте https://geoadmin.yandex-team.ru/#region:10000 для построения дерева
        cls.index.regiontree += [
            Region(rid=2),
            Region(
                rid=_Regions.SVERDLOVSK_OBLAST,
                name='Свердловская область',
                region_type=Region.FEDERATIVE_SUBJECT,
                children=[
                    Region(
                        rid=_Regions._SverdlovskOblast.VOLCHANSKY_URBAN_OKRUG,
                        name='Волчанский городской округ',
                        region_type=Region.CITY_DISTRICT,
                        children=[
                            Region(
                                rid=_Regions._SverdlovskOblast._VolchanskyUrbanOkrug.VOLCHANSK,
                                name='Волчанск',
                                region_type=Region.CITY,
                            ),
                        ],
                    ),
                ],
            ),
            Region(
                rid=_Regions.MOSCOW,
                name='Москва',
                region_type=Region.CITY,
                children=[
                    Region(rid=216, name='Зеленоград'),
                ],
            ),
            Region(
                rid=26,
                name='Южный федеральный округ',
                children=[
                    Region(
                        rid=10995,
                        name='Краснодарский край',
                        children=[
                            Region(rid=_Regions.KRASNODAR, name='Краснодар'),
                        ],
                    ),
                ],
            ),
            Region(rid=_Regions.UNKNOWN, name='Регион, для которого нет тарифов'),
        ]

    @classmethod
    def prepare_shops(cls):
        cls.index.shops += [
            _Shops._Blue.B_1,
            _Shops._Blue.B_2,
            _Shops._Blue.E_1,
            _Shops._Blue.B_3,
            _Shops._White.DSBS_1,
        ]

    @classmethod
    def prepare_offers(cls):
        cls.index.mskus += [
            MarketSku(
                title="blue offer sku1",
                hyperid=90813,
                sku=101010,
                blue_offers=[_Offers._Blue.SB_2_B_1, _Offers._Blue.SB_2_B_2],
            ),
            MarketSku(
                title="blue offer exp1",
                hyperid=5,
                sku=5,
                blue_offers=[_Offers._Blue.SE_1_E_1],
            ),
            MarketSku(sku=100542701768, blue_offers=[_Offers._Blue.PROFESSIONAL_PERFECT_HAIR]),
        ]
        cls.index.offers += [_Offers._White.SD_1_DSBS_1]

    @classmethod
    def prepare_delivery_tariffs(cls):
        cls.index.dynamic_delivery_tariffs += {
            # cat_stream delivery_category is_kgt From  To    X0 (volume_coef)   X1 (constant_coef)  X2 (items_count_coef) X3 (weight_coef)   X4 (total_coef)
            # FMCG       CourierMarket     0      11162 11162 0.0089823626727249 -116.45621997471426 223.06484526855692    0.5509533511998358 0.1151469044800115 -> -290.837437056932
            DynamicDeliveryTariff(
                is_fmcg=True,
                is_large_size=False,
                region_from=_Regions.SVERDLOVSK_OBLAST,
                region_to=_Regions.SVERDLOVSK_OBLAST,
                delivery_type=DynamicDeliveryTariff.DeliveryType.COURIER_MARKET,
                volume_coef=0.0089823626727249,  # X0
                constant_coef=-116.45621997471426,  # X1
                items_count_coef=223.06484526855692,  # X2
                weight_coef=0.5509533511998358,  # X3
                total_coef=0.1151469044800115,  # X4
            ),
            DynamicDeliveryTariff(
                is_fmcg=True,
                is_large_size=False,
                region_from=11235,
                region_to=213,
                delivery_type=DynamicDeliveryTariff.DeliveryType.COURIER_PARTHER,
                volume_coef=10.0,
                constant_coef=None,
                items_count_coef=None,
                weight_coef=None,
                total_coef=None,
            ),
            DynamicDeliveryTariff(
                is_fmcg=False,
                is_large_size=False,
                region_from=213,
                region_to=35,
                delivery_type=DynamicDeliveryTariff.DeliveryType.COURIER_MARKET,
                constant_coef=-500.0,
            ),
            DynamicDeliveryTariff(
                is_fmcg=False,
                is_large_size=False,
                region_from=10841,
                region_to=10645,
                delivery_type=DynamicDeliveryTariff.DeliveryType.POST,
                items_count_coef=17.0,
            ),
            DynamicDeliveryTariff(
                is_fmcg=False,
                is_large_size=True,
                region_from=10946,
                region_to=10841,
                delivery_type=DynamicDeliveryTariff.DeliveryType.PICKUP_MARKET,
                weight_coef=2.0,
            ),
            DynamicDeliveryTariff(
                delivery_type=DynamicDeliveryTariff.DeliveryType.PICKUP_PARTHER,
                total_coef=0.3,
            ),
        }

    @classmethod
    def prepare_lms(cls):
        cls.settings.lms_autogenerate = False
        cls.dynamic.lms += [
            DynamicWarehouseInfo(
                id=_Shops._Blue.B_3_ID,
                home_region=_Regions._SverdlovskOblast._VolchanskyUrbanOkrug.VOLCHANSK,
            ),
            DynamicWarehousesPriorityInRegion(
                region=_Regions._SverdlovskOblast._VolchanskyUrbanOkrug.VOLCHANSK,
                warehouses=[_Shops._Blue.B_3_ID],
            ),
            DynamicWarehousesPriorityInRegion(
                region=_Regions.MOSCOW,
                warehouses=[_Shops._Blue.E_1_ID],
            ),
        ]

    @classmethod
    def prepare_combinator(cls):
        cls.settings.check_combinator_errors = False

    @classmethod
    def prepare_nordstream(cls):
        cls.dynamic.nordstream += [
            DynamicWarehouseDelivery(
                _Shops._Blue.E_1_ID,
                {
                    _Regions.MOSCOW: [
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

    @classmethod
    def prepare_config_loaded(cls):
        cls.combinator.on_pickup_points_grouped_request(
            items=[_CombinatorDeliveryItems._Blue.SB_2_B_1, _CombinatorDeliveryItems._Blue.SB_2_B_2],
            destination_regions=[_Regions.KRASNODAR],
            point_types=[],
            total_price=_Offers._Blue.SB_2_B_1.price + _Offers._Blue.SB_2_B_2.price,
            post_codes=[],
        ).respond_with_grouped_pickup_points(
            groups=[create_pickup_point_grouped([_Outlets._Pickup.BLUE_SHOP_1_MOSCOW_1_ID])],
            virtual_box=VIRTUAL_BOX,
        )

    def test_extra_charge_params(self):
        """Проверка что возвращаем поле с константами для мультизаказа"""
        request = (
            'place=actual_delivery'
            '&offers-list=Sku1Price5-IiLVm1Goleg:1'
            '&rids=35'
            '&rearr-factors=market_dynamic_delivery_tariffs=1'
        )

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "extraChargeParameters": {
                    "chargeQuant": "50",
                    "maxCharge": "100",
                    "minCharge": "199",
                    "minChargeOfGmv": "0.005",
                    "vatMultiplier": "1.2",
                    "version": 42,
                },
            },
        )

    @classmethod
    def prepare_used_default(cls):
        cls.combinator.on_pickup_points_grouped_request(
            items=[_CombinatorDeliveryItems._Blue.SB_2_B_1],
            destination_regions=[_Regions.UNKNOWN],
            point_types=[],
            total_price=_Offers._Blue.SB_2_B_1.price,
            post_codes=[],
        ).respond_with_grouped_pickup_points(
            groups=[
                create_pickup_point_grouped([_Outlets._Pickup.BLUE_SHOP_1_MOSCOW_1_ID]),
            ],
            virtual_box=VIRTUAL_BOX,
        )

    def test_used_default(self):
        """Проверка: используется дефолтный тариф для ПВЗ, т.к. других тарифов не найдено"""
        request = (
            make_base_request([_Offers._Blue.SB_2_B_1])
            + "&rids={}".format(_Regions.UNKNOWN)
            + "&rearr-factors=market_use_business_offer=1"
            + "&rearr-factors=enable_dsbs_combinator_request_in_actual_delivery=0"
        )
        response = self.report.request_json(request)
        # Будем видеть несколько записей с пропуском вычисления - таких тарифов нет
        self.assertFragmentIn(
            response, {"logicTrace": [Contains('Unable to find calculator, skip extra charge calculation')]}
        )
        # Для ПВЗ получим посчитанный extraCharge, по тарифу PICKUP_PARTHER
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "delivery": {
                            "pickupOptions": [
                                {
                                    "extraCharge": {"unitEconomyValue": "-112", "value": "0"},
                                    "serviceId": 123,
                                }
                            ],
                        }
                    }
                ]
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_zero_volume(cls):
        cls.combinator.on_courier_options_request(
            items=[_CombinatorDeliveryItems._Blue.SB_2_B_2],
            destination=Destination(region_id=_Regions.KRASNODAR),
            payment_methods=[],
            total_price=_Offers._Blue.SB_2_B_2.price,
        ).respond_with_courier_options(
            options=[DELIVERY_SERVICE_ID_MOSCOW_delivery_option],
            virtual_box=VIRTUAL_BOX,
        )

    def test_zero_volume(self):
        """Проверка на нулевой объем"""
        request = (
            make_base_request([_Offers._Blue.SB_2_B_2])
            + "&rids={}".format(_Regions.KRASNODAR)
            + "&rearr-factors=market_use_business_offer=1"
            + "&rearr-factors=enable_dsbs_combinator_request_in_actual_delivery=0"
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
                                    "extraCharge": Absent(),
                                    "serviceId": "123",
                                }
                            ],
                        }
                    }
                ]
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_dsbs_skip_extra_charge_calculation(cls):
        cls.combinator.on_courier_options_request(
            items=[_CombinatorDeliveryItems._White.SD_1_DSBS_1],
            destination=Destination(region_id=_Regions.MOSCOW),
            payment_methods=[],
            total_price=_Offers._White.SD_1_DSBS_1.price,
        ).respond_with_courier_options(
            options=[DELIVERY_SERVICE_ID_MOSCOW_delivery_option],
            virtual_box=VIRTUAL_BOX,
        )

    def test_dsbs_skip_extra_charge_calculation(self):
        """
        Проверяем, что доп. стоимость доставки не вычисляется для dsbs-офферов (приходит опция,
        но она не содержит вычисленное значение extraCharge)
        """
        request = (
            make_base_request([_Offers._White.SD_1_DSBS_1])
            + "&rids={}".format(_Regions.MOSCOW)
            + "&rearr-factors=market_use_business_offer=1"
            + "&rearr-factors=enable_dsbs_combinator_request_in_actual_delivery=1;use_dsbs_combinator_response_in_actual_delivery=1"
        )
        self.error_log.expect(code=ErrorCodes.FAILED_TO_FIND_DELICALC_GENERATION_NUMBER)
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "delivery": {
                            "options": [
                                {
                                    "extraCharge": Absent(),
                                    "serviceId": "123",
                                }
                            ],
                        }
                    }
                ]
            },
            allow_different_len=False,
        )
        self.assertFragmentIn(response, {"logicTrace": [Contains('Skip extra charge calculation for dsbs offers')]})

    @classmethod
    def prepare_express_skip_extra_charge_calculation(cls):
        cls.combinator.on_courier_options_request(
            items=[_CombinatorDeliveryItems._Blue.SE_1_E_1],
            destination=Destination(region_id=_Regions.MOSCOW),
            payment_methods=[],
            total_price=_Offers._Blue.SE_1_E_1.price,
        ).respond_with_courier_options(
            options=[DELIVERY_SERVICE_ID_MOSCOW_delivery_option],
            virtual_box=VIRTUAL_BOX,
        )

    def test_express_skip_extra_charge_calculation(self):
        """
        Проверяем, что доп. стоимость доставки не вычисляется для express-офферов (приходит опция,
        но она не содержит вычисленное значение extraCharge)
        """
        request = (
            make_base_request([_Offers._Blue.SE_1_E_1])
            + "&rids={}".format(_Regions.MOSCOW)
            + "&rearr-factors=market_use_business_offer=1"
            + "&rearr-factors=enable_dsbs_combinator_request_in_actual_delivery=0"
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
                                    "extraCharge": Absent(),
                                    "serviceId": "123",
                                },
                            ],
                        },
                    },
                ],
            },
            allow_different_len=False,
        )
        self.assertFragmentIn(response, {"logicTrace": [Contains('Skip extra charge calculation for express offers')]})

    @classmethod
    def prepare_professional_perfect_hair_extra_charge(cls):
        cls.combinator.on_courier_options_request(
            items=[_CombinatorDeliveryItems._Blue.PROFESSIONAL_PERFECT_HAIR],
            destination=Destination(region_id=_Regions._SverdlovskOblast._VolchanskyUrbanOkrug.VOLCHANSK),
            payment_methods=[],
            total_price=_Offers._Blue.PROFESSIONAL_PERFECT_HAIR.price,
        ).respond_with_courier_options(
            options=[DELIVERY_SERVICE_ID_VOLCHANSK_delivery_options],
            virtual_box=VIRTUAL_BOX,
        )

    def test_professional_perfect_hair_extra_charge(self):
        request = (
            make_base_request([_Offers._Blue.PROFESSIONAL_PERFECT_HAIR])
            + "&rids={}".format(_Regions._SverdlovskOblast._VolchanskyUrbanOkrug.VOLCHANSK)
            + "&rearr-factors=market_use_business_offer=1"
            + "&rearr-factors=enable_dsbs_combinator_request_in_actual_delivery=0"
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
                                    "extraCharge": {
                                        "currency": "RUR",
                                        "reasonCodes": [],
                                        "unitEconomyValue": "-290",  # rounded from -290.837437056932
                                        "value": "91",  # -(-290) - 199
                                    },
                                    "serviceId": "124",
                                },
                            ],
                        },
                    },
                ],
            },
            allow_different_len=False,
        )
        self.assertFragmentIn(
            response,
            {
                "logicTrace": [
                    Contains(
                        (
                            'Calculate(): UE calculation: 1 * (modelPerOrderFixCost(-116.45622)'
                            ' + deliveryCostVolumeWeight(-1 * 0.5509533512 * 0.008982362673 * 1680 = -8.314089531)'
                            ' + modelItemCountCost(-1 * 223.0648453 * 1 = -223.0648453)'
                            ' + modelSalesPriceRevenue(0.1151469045 * 495 = 56.99771772))'
                            ' = -290.8374371'
                        )
                    )
                ]
            },
        )


if __name__ == '__main__':
    main()
