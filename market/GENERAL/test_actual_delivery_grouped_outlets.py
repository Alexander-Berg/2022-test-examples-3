#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa
import urllib

from core.types import Currency, Dimensions, GpsCoord, Offer, Outlet, OutletDeliveryOption, Region, Shop

from core.logs import ErrorCodes
from core.matcher import Contains, EmptyList, EmptyDict
from core.testcase import TestCase, main
from core.types.model import Model
from core.types.offer import OfferDimensions
from core.types.payment_methods import Payment, PaymentMethod
from core.types.sku import MarketSku, BlueOffer
from core.types.taxes import Vat, Tax
from core.types.delivery import (
    DeliveryBucket,
    DeliveryOption,
    OutletType,
    PickupBucket,
    PickupOption,
    ProhibitedBlueOffers,
    RegionalDelivery,
)
from core.types.combinator import (
    create_virtual_box,
    CombinatorOffer,
    CombinatorPickupPointGeoOutlet,
    CombinatorPickupPointGeoGroup,
    CombinatorPickupPointsGeoStatistic,
    CombinatorDeliveryParcel,
    CombinatorGpsCoords,
    DeliveryItem,
    PickupPointGrouped,
)

import datetime


TODAY = datetime.datetime.today()
DATE_FROM = TODAY + datetime.timedelta(days=2)

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

PITER_RID = 111
VASYA_OSTROV_RID = 112
PALACE_RID = 113

MOSCOW_RID = 213
HAMOVNIKY_RID = 122
CENTRAL_RID = 123

# ========== Доставка ==========

DELIVERY_SERVICE_ID_PITER = 121
DELIVERY_SERVICE_ID_MOSCOW = 123

# ========== Магазины ==========


class _Shops:
    class _Blue:
        B_1_ID = next(SHOP_ID_GENERATOR)
        B_1 = Shop(
            fesh=B_1_ID,
            datafeed_id=B_1_ID,
            priority_region=MOSCOW_RID,
            name='blue_shop_1',
            tax_system=Tax.OSN,
            supplier_type=Shop.THIRD_PARTY,
            blue=Shop.BLUE_REAL,
            warehouse_id=145,
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
            name='white_cpa_1',
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

        BLUE_SHOP_1_MOSCOW_2_ID = next(OUTLET_ID_GENERATOR)
        SHOP_B_1_MOSCOW_2 = create_pickup_outlet(
            BLUE_SHOP_1_MOSCOW_2_ID, MOSCOW_RID, _Shops._Blue.B_1_ID, gps_coord=GpsCoord(51.0, 51.0)
        )

        BLUE_SHOP_1_MOSCOW_3_ID = next(OUTLET_ID_GENERATOR)
        SHOP_B_1_MOSCOW_3 = create_pickup_outlet(
            BLUE_SHOP_1_MOSCOW_3_ID, MOSCOW_RID, _Shops._Blue.B_1_ID, gps_coord=GpsCoord(49.0, 49.0)
        )

        BLUE_SHOP_1_MOSCOW_4_ID = next(OUTLET_ID_GENERATOR)
        SHOP_B_1_MOSCOW_4 = create_pickup_outlet(
            BLUE_SHOP_1_MOSCOW_4_ID, MOSCOW_RID, _Shops._Blue.B_1_ID, gps_coord=GpsCoord(53.0, 53.0)
        )

        W_1_MOSCOW_1_ID = next(OUTLET_ID_GENERATOR)
        W_1_MOSCOW_1 = Outlet(
            fesh=_Shops._White.W_1_ID,
            point_id=W_1_MOSCOW_1_ID,
            region=MOSCOW_RID,
            point_type=Outlet.FOR_PICKUP,
            delivery_option=OutletDeliveryOption(shipper_id=247, day_from=1, day_to=3, price=400),
            dimensions=Dimensions(width=1000, height=1000, length=1000),
        )

    class _Post:
        BLUE_SHOP_1_MOSCOW_1_ID = next(OUTLET_ID_GENERATOR)
        SHOP_B_1_MOSCOW_1 = create_post_outlet(BLUE_SHOP_1_MOSCOW_1_ID, MOSCOW_RID, _Shops._Blue.B_1_ID)


def create_pickup_option(outlet_id):
    return PickupOption(outlet_id=outlet_id, day_from=1, day_to=20)


def create_courier_options(rid):
    return RegionalDelivery(
        rid=rid,
        options=[DeliveryOption(day_from=1, day_to=20, shop_delivery_price=5)],
        payment_methods=[Payment.PT_YANDEX, Payment.PT_CASH_ON_DELIVERY, Payment.PT_CARD_ON_DELIVERY],
    )


class _Buckets:
    class _Pickup:
        BLUE_1_ID = next(BUCKET_ID_GENERATOR)
        BLUE_1 = PickupBucket(
            bucket_id=BLUE_1_ID,
            dc_bucket_id=BLUE_1_ID,
            carriers=[DELIVERY_SERVICE_ID_MOSCOW],
            options=[
                create_pickup_option(_Outlets._Pickup.BLUE_SHOP_1_MOSCOW_1_ID),
                create_pickup_option(_Outlets._Pickup.BLUE_SHOP_1_MOSCOW_2_ID),
                create_pickup_option(_Outlets._Pickup.BLUE_SHOP_1_MOSCOW_3_ID),
            ],
            delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
        )

        BLUE_2_ID = next(BUCKET_ID_GENERATOR)
        BLUE_2 = PickupBucket(
            bucket_id=BLUE_2_ID,
            dc_bucket_id=BLUE_2_ID,
            carriers=[DELIVERY_SERVICE_ID_MOSCOW],
            options=[
                create_pickup_option(_Outlets._Pickup.BLUE_SHOP_1_MOSCOW_1_ID),
                create_pickup_option(_Outlets._Pickup.BLUE_SHOP_1_MOSCOW_2_ID),
                create_pickup_option(_Outlets._Pickup.BLUE_SHOP_1_MOSCOW_4_ID),
            ],
            delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
        )

    class _Post:
        BLUE_1_ID = next(BUCKET_ID_GENERATOR)
        BLUE_1 = PickupBucket(
            bucket_id=BLUE_1_ID,
            dc_bucket_id=BLUE_1_ID,
            fesh=_Shops._Blue.B_1_ID,
            carriers=[DELIVERY_SERVICE_ID_MOSCOW],
            options=[create_pickup_option(_Outlets._Pickup.BLUE_SHOP_1_MOSCOW_1_ID)],
            delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
        )


# ========== Офферы и модели ==========


class _Models:
    M_1 = Model(hyperid=100, hid=100)
    M_2 = Model(hyperid=101, hid=101)


DEFAULT_COURIER_DELIVERY_OPTION = DeliveryOption(price=100, day_from=2, day_to=3, order_before=23)
DEFAULT_PICKUP_DELIVERY_OPTION = DeliveryOption(price=100, day_from=2, day_to=3, order_before=23)


class _Offers:
    class _Blue:
        B_1 = BlueOffer(
            price=5,
            vat=Vat.VAT_10,
            waremd5='Sku1Price5-IiLVm1Goleg',
            weight=5,
            sku=1,
            hid=1,
            dimensions=OfferDimensions(length=20, width=30, height=10),
            post_term_delivery=True,
            cargo_types=[1, 2, 3],
            fesh=_Shops._Blue.B_1_ID,
            feedid=_Shops._Blue.B_1_ID,
            offerid='blue.offer.1=1',
            delivery_options=[DEFAULT_COURIER_DELIVERY_OPTION],
            pickup_option=DEFAULT_PICKUP_DELIVERY_OPTION,
        )

        B_2 = BlueOffer(
            price=5,
            vat=Vat.VAT_10,
            waremd5='Sku2Price5-IiLVm1Goleg',
            weight=5,
            sku=2,
            hid=2,
            dimensions=OfferDimensions(length=30, width=40, height=20),
            post_term_delivery=True,
            cargo_types=[1, 2, 3],
            fesh=_Shops._Blue.B_1_ID,
            feedid=_Shops._Blue.B_1_ID,
            offerid='blue.offer.1.2',
            delivery_options=[DEFAULT_COURIER_DELIVERY_OPTION],
            pickup_option=DEFAULT_PICKUP_DELIVERY_OPTION,
        )

        # Оффер, который будет принадлежать модели M_1
        B_3_M_1 = BlueOffer(
            price=5,
            vat=Vat.VAT_10,
            waremd5='B3M1Price5-IiLVm1Goleg',
            weight=5,
            sku=3,
            hid=_Models.M_1.hid,
            dimensions=OfferDimensions(length=20, width=30, height=10),
            post_term_delivery=True,
            cargo_types=[1, 2, 3],
            fesh=_Shops._Blue.B_1_ID,
            feedid=_Shops._Blue.B_1_ID,
            offerid='blue.offer.3.m.1',
            delivery_options=[DEFAULT_COURIER_DELIVERY_OPTION],
            pickup_option=DEFAULT_PICKUP_DELIVERY_OPTION,
        )

        # Оффер, который будет принадлежать модели M_2
        B_3_M_2 = BlueOffer(
            price=5,
            vat=Vat.VAT_10,
            waremd5='B3M2Price5-IiLVm1Goleg',
            weight=5,
            sku=4,
            hid=_Models.M_2.hid,
            dimensions=OfferDimensions(length=20, width=30, height=10),
            post_term_delivery=True,
            cargo_types=[1, 2, 3],
            fesh=_Shops._Blue.B_1_ID,
            feedid=_Shops._Blue.B_1_ID,
            offerid='blue.offer.3.m.2',
            delivery_options=[DEFAULT_COURIER_DELIVERY_OPTION],
            pickup_option=DEFAULT_PICKUP_DELIVERY_OPTION,
        )

    class _White:
        W_1 = Offer(
            feedid=_Shops._White.W_1_ID,
            cpa=Offer.CPA_REAL,
            fesh=_Shops._White.W_1_ID,
            price=100,
            waremd5='DSBS_0_________1333ggg',
            weight=10,
            delivery_options=[DeliveryOption(price=100, day_from=5, day_to=8, order_before=14)],
            dimensions=OfferDimensions(length=2, width=3, height=1),
        )


# ========== Представления офферов для mock-запросов в комбинатор ==========


class _CombinatorOffers:
    class _Blue:
        B_1 = CombinatorOffer(
            shop_sku='blue.offer.1=1',
            shop_id=_Shops._Blue.B_1_ID,
            partner_id=145,
            available_count=1,
            offer_feed_delivery_option=DEFAULT_COURIER_DELIVERY_OPTION,
            offer_feed_pickup_option=DEFAULT_PICKUP_DELIVERY_OPTION,
        )

        B_2 = CombinatorOffer(
            shop_sku='blue.offer.1.2',
            shop_id=_Shops._Blue.B_1_ID,
            partner_id=145,
            available_count=1,
            offer_feed_delivery_option=DEFAULT_COURIER_DELIVERY_OPTION,
            offer_feed_pickup_option=DEFAULT_PICKUP_DELIVERY_OPTION,
        )

        B_3_M_1 = CombinatorOffer(
            shop_sku='blue.offer.3.m.1',
            shop_id=_Shops._Blue.B_1_ID,
            partner_id=145,
            available_count=1,
            offer_feed_delivery_option=DEFAULT_COURIER_DELIVERY_OPTION,
            offer_feed_pickup_option=DEFAULT_PICKUP_DELIVERY_OPTION,
        )

        B_3_M_2 = CombinatorOffer(
            shop_sku='blue.offer.3.m.2',
            shop_id=_Shops._Blue.B_1_ID,
            partner_id=145,
            available_count=1,
            offer_feed_delivery_option=DEFAULT_COURIER_DELIVERY_OPTION,
            offer_feed_pickup_option=DEFAULT_PICKUP_DELIVERY_OPTION,
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
    dimensions=[40, 30, 20],
    cargo_types=[1, 2, 3],
    offers=[_CombinatorOffers._Blue.B_2],
    price=_Offers._Blue.B_2.price,
)
B_3_M_1_delivery_item = DeliveryItem(
    required_count=1,
    weight=5000,
    dimensions=[10, 20, 30],
    cargo_types=[1, 2, 3],
    offers=[_CombinatorOffers._Blue.B_3_M_1],
    price=_Offers._Blue.B_3_M_1.price,
)
B_3_M_2_delivery_item = DeliveryItem(
    required_count=1,
    weight=5000,
    dimensions=[10, 20, 30],
    cargo_types=[1, 2, 3],
    offers=[_CombinatorOffers._Blue.B_3_M_2],
    price=_Offers._Blue.B_3_M_2.price,
)

VIRTUAL_BOX = create_virtual_box(weight=5000, length=100, width=100, height=100)


def quote_with_level(value, quoted_level=1):
    result = value
    for _ in range(quoted_level):
        result = urllib.quote_plus(result)
    return result


def servialize_delivery_options(combinator_offer, quoted_level=1):
    def serialize_delivery_option(delivery_option, delivery_type):
        if delivery_option is None:
            return None
        return "d_f:{};d_t:{};o_b:{};type:{}".format(
            delivery_option.day_from, delivery_option.day_to, delivery_option.order_before, delivery_type
        )

    result = []
    if combinator_offer.offer_feed_delivery_option is not None:
        result.append(
            "option="
            + quote_with_level(
                serialize_delivery_option(combinator_offer.offer_feed_delivery_option, "delivery"), quoted_level
            )
        )
    if combinator_offer.offer_feed_pickup_option is not None:
        result.append(
            "option="
            + quote_with_level(
                serialize_delivery_option(combinator_offer.offer_feed_pickup_option, "pickup"), quoted_level
            )
        )
    return "|".join(result)


def serialize_combinator_delivery_item(offer, combinator_delivery_item, quoted_level=1):
    fields = [
        "w={weight}".format(weight=combinator_delivery_item.weight / 1000),
        "p={price}".format(price=combinator_delivery_item.price),
        "d={dimensions}".format(dimensions="x".join([str(d) for d in combinator_delivery_item.dimensions])),
        "wh={warehouse}".format(warehouse=combinator_delivery_item.offers[0].partner_id),
        "ffwh={ff_warehouse}".format(ff_warehouse=combinator_delivery_item.offers[0].partner_id),
        "ct={cargo_types}".format(cargo_types="/".join([str(ct) for ct in combinator_delivery_item.cargo_types])),
        "hid={hid}".format(hid=offer.hid) if hasattr(offer, "hid") else None,
        "msku={msku}".format(msku=offer.sku) if hasattr(offer, "sku") else None,
        "offer_id={shop_sku}".format(
            shop_sku=quote_with_level(combinator_delivery_item.offers[0].shop_sku, quoted_level)
        ),
        "supplier_id={shop_id}".format(shop_id=combinator_delivery_item.offers[0].shop_id),
        servialize_delivery_options(combinator_delivery_item.offers[0], quoted_level),
        "ff=0",
    ]
    return "|".join([v for v in fields if v])


def serialize_offer_for_parcel_info(offer, count, combinator_delivery_item, quoted_level=1):
    return "|".join(
        [
            "{}={}".format(offer.waremd5, count),
            serialize_combinator_delivery_item(offer, combinator_delivery_item, quoted_level),
        ]
    )


def serialize_offer_for_parcel_request(offer, count, combinator_delivery_item):
    """
    Сериализовать данные оффера в форматы посылки для запроса; дважды экранирует спец. символы,
    т.к. при парсинге запроса один слой экранирования снимается по умолчанию
    """
    return serialize_offer_for_parcel_info(offer, count, combinator_delivery_item, quoted_level=2)


def serialize_offer_for_parcel_response(offer, count, combinator_delivery_item):
    """
    Сериализовать данные оффера в форматы посылки для ответ; экранируются спец символы только один раз
    """
    return serialize_offer_for_parcel_info(offer, count, combinator_delivery_item, quoted_level=1)


def create_pickup_point_grouped(point_ids):
    return PickupPointGrouped(
        ids_list=point_ids,
        post_ids=point_ids,
        outlet_type=OutletType.FOR_PICKUP,
        service_id=DELIVERY_SERVICE_ID_MOSCOW,
        cost=1,
        date_from=DATE_FROM,
        date_to=DATE_FROM,
        payment_methods=[
            PaymentMethod.PT_YANDEX,
            PaymentMethod.PT_CASH_ON_DELIVERY,
        ],
    )


def make_base_request(offers):
    return "place=actual_delivery" + "&offers-list={}".format(
        ",".join("{}:1".format(offer.waremd5) for offer in offers)
    )


class T(TestCase):
    @classmethod
    def prepare_mskus(cls):
        cls.index.mskus += [
            MarketSku(
                title="blue offer sku1",
                sku=1,
                hid=1,
                hyperid=1,
                blue_offers=[_Offers._Blue.B_1],
                post_term_delivery=True,
            ),
            MarketSku(
                title="blue offer sku2",
                sku=2,
                hid=2,
                hyperid=2,
                blue_offers=[_Offers._Blue.B_2],
                post_term_delivery=True,
            ),
            MarketSku(
                title="blue offer model1",
                sku=3,
                hyperid=_Models.M_1.hyper,
                hid=_Models.M_1.hid,
                blue_offers=[_Offers._Blue.B_3_M_1],
                post_term_delivery=True,
            ),
            MarketSku(
                title="blue offer model2",
                sku=4,
                hyperid=_Models.M_2.hyper,
                hid=_Models.M_2.hid,
                blue_offers=[_Offers._Blue.B_3_M_2],
                post_term_delivery=True,
            ),
        ]

    @classmethod
    def prepare_regions(cls):
        cls.index.regiontree += [
            Region(
                rid=PITER_RID,
                region_type=Region.CITY,
                children=[
                    Region(rid=VASYA_OSTROV_RID, region_type=Region.CITY_DISTRICT),
                    Region(rid=PALACE_RID, region_type=Region.CITY_DISTRICT),
                ],
            ),
            Region(
                rid=MOSCOW_RID,
                region_type=Region.CITY,
                children=[
                    Region(rid=HAMOVNIKY_RID, region_type=Region.CITY_DISTRICT),
                    Region(rid=CENTRAL_RID, region_type=Region.CITY_DISTRICT),
                ],
            ),
        ]

    @classmethod
    def prepare_prohibited_blue_offers(cls):
        cls.index.prohibited_blue_offers = [
            ProhibitedBlueOffers(region_id=HAMOVNIKY_RID, categories=[100]),
            ProhibitedBlueOffers(region_id=MOSCOW_RID, categories=[101]),
        ]

    @classmethod
    def prepare_outlets(cls):
        cls.index.outlets += [
            _Outlets._Pickup.SHOP_B_1_MOSCOW_1,
            _Outlets._Pickup.SHOP_B_1_MOSCOW_2,
            _Outlets._Pickup.SHOP_B_1_MOSCOW_3,
            _Outlets._Pickup.SHOP_B_1_MOSCOW_4,
            _Outlets._Post.SHOP_B_1_MOSCOW_1,
        ]

    @classmethod
    def prepare_shops(cls):
        cls.index.shops += [
            _Shops._Blue.B_1,
        ]

    @classmethod
    def prepare_buckets(cls):
        cls.index.pickup_buckets += [
            _Buckets._Pickup.BLUE_1,
            _Buckets._Pickup.BLUE_2,
        ]

        cls.index.pickup_buckets += [
            _Buckets._Post.BLUE_1,
        ]

    # Проверка отлова ошибок в данных

    def test_empty_parcels_list(self):
        """
        Проверяем корректную ошибку в случае отсутствия параметра parcels-list
        """
        request = (
            "place=actual_delivery"
            "&geo_bounds_lb=50.0,50.0"
            "&geo_bounds_rt=55.0,55.0"
            "&max-outlets=100"
            "&zoom=10"
            "&show-outlet=groups"
        )
        self.report.request_json(request)
        self.error_log.expect("parcels-list param is required", code=ErrorCodes.RESPONSE_ERROR)

    def test_empty_offers_in_parcels_list(self):
        """
        Проверяем корректную ошибку в случае отсутствия параметра offers в parcels-list
        """
        request = (
            "place=actual_delivery"
            "&geo_bounds_lb=50.0,50.0"
            "&geo_bounds_rt=55.0,55.0"
            "&max-outlets=100"
            "&zoom=10"
            "&show-outlet=groups"
            "&parcels-list=tp:5;tpc:RUR;"
        )
        self.report.request_json(request)
        self.error_log.expect("offers subparam for parcels-list is required", code=ErrorCodes.RESPONSE_ERROR)

    def test_empty_geo_bounds(self):
        """
        Проверяем корректную ошибку в случае отсутствия параметра geo_bounds_lb или geo_bounds_rt
        """
        # Отсутствует и geo_bounds_rt, и geo_bounds_lb
        request = (
            "place=actual_delivery"
            + "&max-outlets=100"
            + "&zoom=10"
            + "&show-outlet=groups"
            + "&parcels-list=tp:5;tpc:RUR;offers:"
            + serialize_offer_for_parcel_request(_Offers._Blue.B_1, 1, B_1_delivery_item)
            + ";"
        )
        self.report.request_json(request)
        self.error_log.expect("geo_bounds_lb and geo_bounds_rt params are required", code=ErrorCodes.RESPONSE_ERROR)

        # Отсутствует geo_bounds_lb
        request = (
            "place=actual_delivery"
            "&geo_bounds_rt=50.0,50.0"
            "&max-outlets=100"
            "&zoom=10"
            "&show-outlet=groups"
            "&parcels-list=tp:5;tpc:RUR;offers:"
            + serialize_offer_for_parcel_request(_Offers._Blue.B_1, 1, B_1_delivery_item)
            + ";"
        )
        self.report.request_json(request)
        self.error_log.expect("geo_bounds_lb and geo_bounds_rt params are required", code=ErrorCodes.RESPONSE_ERROR)

        # Отсутствует и geo_bounds_rt
        request = (
            "place=actual_delivery"
            "&geo_bounds_lb=50.0,50.0"
            "&max-outlets=100"
            "&zoom=10"
            "&show-outlet=groups"
            "&parcels-list=tp:5;tpc:RUR;offers:"
            + serialize_offer_for_parcel_request(_Offers._Blue.B_1, 1, B_1_delivery_item)
            + ";"
        )
        self.report.request_json(request)
        self.error_log.expect("geo_bounds_lb and geo_bounds_rt params are required", code=ErrorCodes.RESPONSE_ERROR)

    def test_empty_zoom(self):
        """
        Проверяем корректную ошибку в случае отсутствия параметра zoom
        """
        request = (
            "place=actual_delivery"
            "&geo_bounds_lb=50.0,50.0"
            "&geo_bounds_rt=55.0,55.0"
            "&max-outlets=100"
            "&show-outlet=groups"
            "&parcels-list=tp:5;tpc:RUR;offers:"
            + serialize_offer_for_parcel_request(_Offers._Blue.B_1, 1, B_1_delivery_item)
            + ";"
        )
        self.report.request_json(request)
        self.error_log.expect("zoom param is required", code=ErrorCodes.RESPONSE_ERROR)

    # Проверка работы функционала pickupPointsGeo

    def test_receive_parcel_info_from_actual_delivery(self):
        """
        Проверяем получение строки с информацией о посылке
        """
        request = (
            make_base_request([_Offers._Blue.B_1])
            + "&rids={}".format(MOSCOW_RID)
            + "&actualize_outlets={}".format(_Outlets._Pickup.BLUE_SHOP_1_MOSCOW_1_ID)
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "parcelInfo": "tp:5;tpc:RUR;offers:"
                + serialize_offer_for_parcel_response(_Offers._Blue.B_1, 1, B_1_delivery_item)
                + ";"
            },
        )

    @classmethod
    def prepare_base_logic_blue(cls):
        cls.settings.check_combinator_errors = True
        cls.combinator.on_pickup_points_geo_request(
            filters=0,
            max_outlets=100,
            zoom=10,
            left_bottom=CombinatorGpsCoords(50.0, 50.0),
            right_top=CombinatorGpsCoords(55.0, 55.0),
            total_price=0,
            parcels=[
                CombinatorDeliveryParcel(
                    items=[B_1_delivery_item],
                    total_price=5,
                ),
            ],
        ).respond_with_pickup_points_geo(
            groups=[
                CombinatorPickupPointGeoGroup(
                    group_center=CombinatorGpsCoords(52.5, 52.5),
                    pickup_point_geo_outlets=[
                        CombinatorPickupPointGeoOutlet(
                            logistic_point_id=_Outlets._Pickup.BLUE_SHOP_1_MOSCOW_1_ID,
                            gps_coords=CombinatorGpsCoords(52.5, 52.5),
                            flags=24,
                        ),
                    ],
                    total=1,
                ),
                CombinatorPickupPointGeoGroup(
                    group_center=CombinatorGpsCoords(51, 51),
                    pickup_point_geo_outlets=[
                        CombinatorPickupPointGeoOutlet(
                            logistic_point_id=_Outlets._Pickup.BLUE_SHOP_1_MOSCOW_2_ID,
                            gps_coords=CombinatorGpsCoords(51, 51),
                            flags=24,
                        )
                    ],
                    total=1,
                ),
            ],
            statistic=[CombinatorPickupPointsGeoStatistic(flags=24, count=2)],
        )

    def test_base_logic_blue(self):
        """
        Проверяем работы базовых вещей: послали запрос в правильном формате
        """
        request = (
            "place=actual_delivery"
            "&geo_bounds_lb=50.0,50.0"
            "&geo_bounds_rt=55.0,55.0"
            "&max-outlets=100"
            "&zoom=10"
            "&show-outlet=groups"
            "&parcels-list=tp:5;tpc:RUR;offers:"
            + serialize_offer_for_parcel_request(_Offers._Blue.B_1, 1, B_1_delivery_item)
            + ";"
        )
        result = self.report.request_json(request)
        self.assertFragmentIn(
            result,
            {
                "groups": [
                    {
                        "lat": "52.5",
                        "lon": "52.5",
                        "outlets": [
                            {
                                "flags": "24",
                                "id": str(_Outlets._Pickup.BLUE_SHOP_1_MOSCOW_1_ID),
                                "lat": "52.5",
                                "lon": "52.5",
                            }
                        ],
                        "total": 1,
                    },
                    {
                        "lat": "51",
                        "lon": "51",
                        "outlets": [
                            {
                                "flags": "24",
                                "id": str(_Outlets._Pickup.BLUE_SHOP_1_MOSCOW_2_ID),
                                "lat": "51",
                                "lon": "51",
                            }
                        ],
                        "total": 1,
                    },
                ]
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_intersection_outlets_for_parcels(cls):
        cls.settings.check_combinator_errors = True
        cls.combinator.on_pickup_points_geo_request(
            filters=1,
            max_outlets=100,
            zoom=2,
            left_bottom=CombinatorGpsCoords(50.0, 50.0),
            right_top=CombinatorGpsCoords(55.0, 55.0),
            total_price=0,
            parcels=[
                CombinatorDeliveryParcel(
                    items=[B_1_delivery_item],
                    total_price=5,
                ),
                CombinatorDeliveryParcel(
                    items=[B_2_delivery_item],
                    total_price=5,
                ),
            ],
        ).respond_with_pickup_points_geo(
            groups=[
                CombinatorPickupPointGeoGroup(
                    group_center=CombinatorGpsCoords(51.75, 51.75),
                    pickup_point_geo_outlets=[
                        CombinatorPickupPointGeoOutlet(
                            logistic_point_id=_Outlets._Pickup.BLUE_SHOP_1_MOSCOW_1_ID,
                            gps_coords=CombinatorGpsCoords(52.5, 52.5),
                            flags=24,
                        ),
                        CombinatorPickupPointGeoOutlet(
                            logistic_point_id=_Outlets._Pickup.BLUE_SHOP_1_MOSCOW_2_ID,
                            gps_coords=CombinatorGpsCoords(51, 51),
                            flags=24,
                        ),
                    ],
                    total=2,
                )
            ],
            statistic=[CombinatorPickupPointsGeoStatistic(flags=24, count=2)],
        )

    def test_intersection_outlets_for_parcels(self):
        """
        Проверяем работу получения аутлетов для нескольких посылок:
        При наличии нескольких посылок, возвращаются данные об общих аутлетах (ищем пересечение)
        """
        request = (
            "place=actual_delivery"
            "&geo_bounds_lb=50.0,50.0"
            "&geo_bounds_rt=55.0,55.0"
            "&max-outlets=100"
            "&zoom=2"
            "&filters-key=1"
            "&show-outlet=groups"
            "&parcels-list=tp:5;tpc:RUR;offers:"
            + serialize_offer_for_parcel_request(_Offers._Blue.B_2, 1, B_2_delivery_item)
            + ";"
            + "&parcels-list=tp:5;tpc:RUR;offers:"
            + serialize_offer_for_parcel_request(_Offers._Blue.B_1, 1, B_1_delivery_item)
            + ";"
        )
        result = self.report.request_json(request)
        self.assertFragmentIn(
            result,
            {
                "groups": [
                    {
                        "lon": "51.75",
                        "lat": "51.75",
                        "outlets": [
                            {
                                "id": str(_Outlets._Pickup.BLUE_SHOP_1_MOSCOW_1_ID),
                                "lon": "52.5",
                                "lat": "52.5",
                                "flags": "24",
                            },
                            {
                                "id": str(_Outlets._Pickup.BLUE_SHOP_1_MOSCOW_2_ID),
                                "lon": "51",
                                "lat": "51",
                                "flags": "24",
                            },
                        ],
                        "total": 2,
                    }
                ],
            },
            allow_different_len=False,
        )

    def test_disable_market_combinator_enable_pickup_points_geo(self):
        """
        Проверяем возможность отключить поход в ручку комбинатора
        """
        request = (
            "place=actual_delivery"
            "&geo_bounds_lb=50.0,50.0"
            "&geo_bounds_rt=55.0,55.0"
            "&max-outlets=100"
            "&zoom=2"
            "&filters-key=1"
            "&show-outlet=groups"
            "&rearr-factors=market_combinator_enable_pickup_points_geo=0"
            "&parcels-list=tp:5;tpc:RUR;offers:"
            + serialize_offer_for_parcel_request(_Offers._Blue.B_1, 1, B_1_delivery_item)
            + ";"
        )
        result = self.report.request_json(request)
        self.assertFragmentIn(
            result,
            {
                "groups": EmptyList(),
                "statistic": EmptyDict(),
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_actual_delivery_outlets_param(cls):
        cls.combinator.on_pickup_points_cart_request(
            items=[B_1_delivery_item],
            point_types=[],
            total_price=_Offers._Blue.B_1.price,
            post_codes=[],
            logistic_point_ids=[_Outlets._Pickup.BLUE_SHOP_1_MOSCOW_1_ID],
        ).respond_with_pickup_points_cart(
            groups=[
                create_pickup_point_grouped([_Outlets._Pickup.BLUE_SHOP_1_MOSCOW_1_ID]),
            ],
            virtual_box=VIRTUAL_BOX,
        )

    def test_actual_delivery_outlets_param(self):
        """
        Проверка работы получения данных об актуализации по конкретным аутлетам
        """
        request = (
            make_base_request([_Offers._Blue.B_1])
            + "&rids={}".format(MOSCOW_RID)
            + "&actualize_outlets={}".format(_Outlets._Pickup.BLUE_SHOP_1_MOSCOW_1_ID)
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
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
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_actual_delivery_outlets_param_and_parcel_info(cls):
        cls.combinator.on_pickup_points_cart_request(
            items=[B_1_delivery_item],
            point_types=[],
            total_price=_Offers._Blue.B_1.price,
            post_codes=[],
            logistic_point_ids=[_Outlets._Pickup.BLUE_SHOP_1_MOSCOW_1_ID],
        ).respond_with_pickup_points_cart(
            groups=[
                create_pickup_point_grouped([_Outlets._Pickup.BLUE_SHOP_1_MOSCOW_1_ID]),
            ],
            virtual_box=VIRTUAL_BOX,
        )

    def test_actual_delivery_outlets_param_and_parcel_info(self):
        """
        Проверка работы получения данных об актуализации по конкретным аутлетам,
        а также получение информации по офферу из посылки
        """
        request = (
            "place=actual_delivery"
            + "&rids={}".format(MOSCOW_RID)
            + "&actualize_outlets={}".format(_Outlets._Pickup.BLUE_SHOP_1_MOSCOW_1_ID)
            + "&parcels-list=tp:5;tpc:RUR;offers:"
            + serialize_offer_for_parcel_request(_Offers._Blue.B_1, 1, B_1_delivery_item)
            + ";"
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
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
            },
            allow_different_len=False,
        )

    def test_actual_delivery_outlets_with_force_hedged_reqeust_policy(self):
        """
        Проверка, что вызывается GetPickupPointsGeo c ForceHedged Request Policy
        """
        request = (
            "place=actual_delivery"
            "&geo_bounds_lb=50.0,50.0"
            "&geo_bounds_rt=55.0,55.0"
            "&max-outlets=100"
            "&zoom=10"
            "&show-outlet=groups"
            "&debug=1"
            "&rearr-factors=combinator_request_policy=forcehedged"
            "&parcels-list=tp:5;tpc:RUR;offers:"
            + serialize_offer_for_parcel_request(_Offers._Blue.B_1, 1, B_1_delivery_item)
            + ";"
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"logicTrace": [Contains("Request Policy: forcehedged")]})

    def test_actual_delivery_outlets_with_force_sync_reqeust_policy(self):
        """
        Проверка, что вызывается GetPickupPointsGeo c ForceSync Request Policy
        """
        request = (
            "place=actual_delivery"
            "&geo_bounds_lb=50.0,50.0"
            "&geo_bounds_rt=55.0,55.0"
            "&max-outlets=100"
            "&zoom=10"
            "&show-outlet=groups"
            "&debug=1"
            "&rearr-factors=combinator_request_policy=forcesync"
            "&parcels-list=tp:5;tpc:RUR;offers:"
            + serialize_offer_for_parcel_request(_Offers._Blue.B_1, 1, B_1_delivery_item)
            + ";"
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"logicTrace": [Contains("Request Policy: forcesync")]})

    def test_actual_delivery_outlets_with_default_reqeust_policy(self):
        """
        Проверка, что вызывается GetPickupPointsGeo c Default Request Policy
        """
        request = (
            "place=actual_delivery"
            "&geo_bounds_lb=50.0,50.0"
            "&geo_bounds_rt=55.0,55.0"
            "&max-outlets=100"
            "&zoom=10"
            "&show-outlet=groups"
            "&debug=1"
            "&parcels-list=tp:5;tpc:RUR;offers:"
            + serialize_offer_for_parcel_request(_Offers._Blue.B_1, 1, B_1_delivery_item)
            + ";"
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"logicTrace": [Contains("Request Policy: default")]})

    @classmethod
    def prepare_actual_delivery_prohibiten_regions(cls):
        cls.settings.check_combinator_errors = False
        cls.combinator.on_pickup_points_cart_request(
            items=[B_3_M_1_delivery_item],
            point_types=[],
            total_price=_Offers._Blue.B_3_M_1.price,
            post_codes=[],
            logistic_point_ids=[_Outlets._Pickup.BLUE_SHOP_1_MOSCOW_1_ID],
        ).respond_with_pickup_points_cart(
            groups=[
                create_pickup_point_grouped([_Outlets._Pickup.BLUE_SHOP_1_MOSCOW_1_ID]),
            ],
            virtual_box=VIRTUAL_BOX,
        )

        cls.combinator.on_pickup_points_cart_request(
            items=[B_3_M_2_delivery_item],
            point_types=[],
            total_price=_Offers._Blue.B_3_M_2.price,
            post_codes=[],
            logistic_point_ids=[_Outlets._Pickup.BLUE_SHOP_1_MOSCOW_1_ID],
        ).respond_with_pickup_points_cart(
            groups=[
                create_pickup_point_grouped([_Outlets._Pickup.BLUE_SHOP_1_MOSCOW_1_ID]),
            ],
            virtual_box=VIRTUAL_BOX,
        )

        cls.combinator.on_pickup_points_geo_request(
            filters=0,
            max_outlets=0,
            zoom=10,
            left_bottom=CombinatorGpsCoords(50.0, 50.0),
            right_top=CombinatorGpsCoords(55.0, 55.0),
            total_price=0,
            parcels=[
                CombinatorDeliveryParcel(
                    items=[B_3_M_1_delivery_item],
                    total_price=5,
                ),
            ],
            unavailable_regions=[HAMOVNIKY_RID],
        ).respond_with_pickup_points_geo(
            groups=[
                CombinatorPickupPointGeoGroup(
                    group_center=CombinatorGpsCoords(52.5, 52.5),
                    pickup_point_geo_outlets=[
                        CombinatorPickupPointGeoOutlet(
                            logistic_point_id=_Outlets._Pickup.BLUE_SHOP_1_MOSCOW_1_ID,
                            gps_coords=CombinatorGpsCoords(52.5, 52.5),
                            flags=24,
                        ),
                    ],
                    total=1,
                )
            ],
            statistic=[CombinatorPickupPointsGeoStatistic(flags=24, count=1)],
        )

        cls.combinator.on_pickup_points_geo_request(
            filters=0,
            max_outlets=0,
            zoom=10,
            left_bottom=CombinatorGpsCoords(50.0, 50.0),
            right_top=CombinatorGpsCoords(55.0, 55.0),
            total_price=0,
            parcels=[
                CombinatorDeliveryParcel(
                    items=[B_3_M_2_delivery_item],
                    total_price=5,
                ),
            ],
            unavailable_regions=[MOSCOW_RID],
        ).respond_with_pickup_points_geo(
            groups=[
                CombinatorPickupPointGeoGroup(
                    group_center=CombinatorGpsCoords(52.5, 52.5),
                    pickup_point_geo_outlets=[
                        CombinatorPickupPointGeoOutlet(
                            logistic_point_id=_Outlets._Pickup.BLUE_SHOP_1_MOSCOW_1_ID,
                            gps_coords=CombinatorGpsCoords(52.5, 52.5),
                            flags=24,
                        ),
                    ],
                    total=1,
                )
            ],
            statistic=[CombinatorPickupPointsGeoStatistic(flags=24, count=1)],
        )

        cls.combinator.on_pickup_points_geo_request(
            filters=0,
            max_outlets=0,
            zoom=10,
            left_bottom=CombinatorGpsCoords(50.0, 50.0),
            right_top=CombinatorGpsCoords(55.0, 55.0),
            total_price=0,
            parcels=[
                CombinatorDeliveryParcel(
                    items=[B_3_M_1_delivery_item, B_3_M_2_delivery_item],
                    total_price=10,
                ),
            ],
            unavailable_regions=[MOSCOW_RID],
        ).respond_with_pickup_points_geo(
            groups=[
                CombinatorPickupPointGeoGroup(
                    group_center=CombinatorGpsCoords(52.5, 52.5),
                    pickup_point_geo_outlets=[
                        CombinatorPickupPointGeoOutlet(
                            logistic_point_id=_Outlets._Pickup.BLUE_SHOP_1_MOSCOW_1_ID,
                            gps_coords=CombinatorGpsCoords(52.5, 52.5),
                            flags=24,
                        ),
                    ],
                    total=1,
                )
            ],
            statistic=[CombinatorPickupPointsGeoStatistic(flags=24, count=1)],
        )

    def test_actual_delivery_prohibiten_regions(self):
        """
        Проверка ограничений регионов доставки по категориям
        """
        B_3_M_1_offer_info_response = (
            "offers:" + serialize_offer_for_parcel_response(_Offers._Blue.B_3_M_1, 1, B_3_M_1_delivery_item) + ";"
        )
        B_3_M_1_offer_info_request = (
            "offers:" + serialize_offer_for_parcel_request(_Offers._Blue.B_3_M_1, 1, B_3_M_1_delivery_item) + ";"
        )
        B_3_M_2_offer_info_response = (
            "offers:" + serialize_offer_for_parcel_response(_Offers._Blue.B_3_M_2, 1, B_3_M_2_delivery_item) + ";"
        )
        B_3_M_2_offer_info_request = (
            "offers:" + serialize_offer_for_parcel_request(_Offers._Blue.B_3_M_2, 1, B_3_M_2_delivery_item) + ";"
        )
        # Проверка получения информации parcelInfo для оффера B_3_M_1
        request = (
            make_base_request([_Offers._Blue.B_3_M_1])
            + "&rids={}".format(MOSCOW_RID)
            + "&actualize_outlets={}".format(_Outlets._Pickup.BLUE_SHOP_1_MOSCOW_1_ID)
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"parcelInfo": "tp:5;tpc:RUR;" + B_3_M_1_offer_info_response})
        # Проверка получения информации parcelInfo для оффера B_3_M_2
        request = (
            make_base_request([_Offers._Blue.B_3_M_2])
            + "&rids={}".format(PITER_RID)
            + "&actualize_outlets={}".format(_Outlets._Pickup.BLUE_SHOP_1_MOSCOW_1_ID)
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"parcelInfo": "tp:5;tpc:RUR;" + B_3_M_2_offer_info_response})
        # Ответ geo по умолчанию (чтобы меньше заморачиваться, используем его везде), важно,
        # чтобы совпадали входные параметры для mock-вызовов комбинатора
        default_geo_response = {
            "groups": [
                {
                    "lon": "52.5",
                    "lat": "52.5",
                    "outlets": [
                        {
                            "id": str(_Outlets._Pickup.BLUE_SHOP_1_MOSCOW_1_ID),
                            "lon": "52.5",
                            "lat": "52.5",
                            "flags": "24",
                        },
                    ],
                    "total": 1,
                },
            ],
        }
        # Проверка получения списка ПВЗ
        request = (
            "place=actual_delivery"
            "&filters-key=0"
            "&geo_bounds_lb=50.0,50.0"
            "&geo_bounds_rt=55.0,55.0"
            "&max-outlets=0"
            "&zoom=10"
            "&show-outlet=groups"
            "&debug=1"
            "&parcels-list=tp:5;tpc:RUR;" + B_3_M_1_offer_info_request
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            default_geo_response,
            allow_different_len=False,
        )
        # Проверка получения списка ПВЗ с ограчением по регионам
        request = (
            "place=actual_delivery"
            "&filters-key=0"
            "&geo_bounds_lb=50.0,50.0"
            "&geo_bounds_rt=55.0,55.0"
            "&max-outlets=0"
            "&zoom=10"
            "&show-outlet=groups"
            "&debug=1"
            "&parcels-list=tp:5;tpc:RUR;" + B_3_M_2_offer_info_request
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            default_geo_response,
            allow_different_len=False,
        )
        # Проверка получения списка ПВЗ с ограчением по регионам при слиянии ограничения регионов
        # до минимального набора (HAMOVNIKY_RID является узлом MOSCOW_RID, поэтому обобщаем ограничение
        # до единственного региона MOSCOW_RID)
        request = (
            "place=actual_delivery"
            "&filters-key=0"
            "&geo_bounds_lb=50.0,50.0"
            "&geo_bounds_rt=55.0,55.0"
            "&max-outlets=0"
            "&zoom=10"
            "&show-outlet=groups"
            "&debug=1"
            "&parcels-list=tp:10;tpc:RUR;" + B_3_M_1_offer_info_request + B_3_M_2_offer_info_request
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            default_geo_response,
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()
