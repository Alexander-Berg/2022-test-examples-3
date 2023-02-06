#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

import time

from itertools import count

from core.types.autogen import b64url_md5
from core.matcher import EmptyList, Regex, Absent
from core.testcase import TestCase, main
from core.types import (
    BlueOffer,
    DynamicDeliveryRestriction,
    DynamicShop,
    DynamicWarehouseDelivery,
    DynamicWarehouseInfo,
    DynamicWarehousesPriorityInRegion,
    ExpressSupplier,
    MarketSku,
    Offer,
    Region,
    RtyOffer,
    Shop,
)

from core.types.combinator import (
    CombinatorGpsCoords,
    CombinatorExpressWarehouse,
)

from core.types.offer_promo import (
    Promo,
    PromoType,
    OffersMatchingRules,
    MechanicsPaymentType,
)

from market.idx.pylibrary.offer_flags.flags import DisabledFlags

EKB = 54
MSK = 213

BUSINESS_FEED_ID = 20
SERVICE_FEED_ID = 21
HIDDEN_FEED_ID = 22
EKB_FEED_ID = 23

USE_EXPRESS = 2

RSD_DISABLED = 1 << 1
RSD_FORCE = 1 << 8

EXPRESS_BUSINESS_ID = 1201


class _Const:
    nummer = count()


class FilterDeliveryInterval:
    TODAY = 0
    TOMORROW = 1
    UP_TO_FIVE_DAYS = 2


class _Express_Warehouses(object):
    business = CombinatorExpressWarehouse(
        warehouse_id=BUSINESS_FEED_ID,
        zone_id=1,
        nearest_delivery_day=FilterDeliveryInterval.TODAY,
        nearest_delivery_interval=((0, 0), (23, 59)),
    )

    service = CombinatorExpressWarehouse(
        warehouse_id=SERVICE_FEED_ID,
        zone_id=1,
        nearest_delivery_day=FilterDeliveryInterval.TODAY,
        nearest_delivery_interval=((0, 0), (23, 59)),
    )

    hidden = CombinatorExpressWarehouse(
        warehouse_id=HIDDEN_FEED_ID,
        zone_id=1,
        nearest_delivery_day=FilterDeliveryInterval.TODAY,
        nearest_delivery_interval=((0, 0), (23, 59)),
    )

    _32 = CombinatorExpressWarehouse(
        warehouse_id=32,
        zone_id=1,
        nearest_delivery_day=FilterDeliveryInterval.TODAY,
        nearest_delivery_interval=((0, 0), (23, 59)),
    )

    _33 = CombinatorExpressWarehouse(
        warehouse_id=33,
        zone_id=1,
        nearest_delivery_day=FilterDeliveryInterval.TODAY,
        nearest_delivery_interval=((0, 0), (23, 59)),
    )


def make_gps(lat, lon):
    return 'lat:{lat};lon:{lon}'.format(lat=lat, lon=lon)


def make_same_gps(one_coord):
    return make_gps(one_coord, one_coord)


class _GPS_Coords:
    gps_0 = make_same_gps(0.0)
    gps_1 = make_same_gps(1.0)
    gps_2 = make_same_gps(2.0)
    gps_3 = make_same_gps(3.0)
    gps_4 = make_same_gps(4.0)
    gps_5 = make_same_gps(5.0)
    gps_6 = make_same_gps(6.0)
    gps_7 = make_same_gps(7.0)
    gps_8 = make_same_gps(8.0)
    gps_9 = make_same_gps(9.0)
    gps_10 = make_same_gps(10.0)
    gps_11 = make_same_gps(11.0)
    gps_12 = make_same_gps(12.0)
    gps_13 = make_same_gps(13.0)
    gps_14 = make_same_gps(14.0)
    gps_14_5 = make_same_gps(14.5)
    gps_15 = make_same_gps(15.0)  # probably it will be used in
    gps_16 = make_same_gps(16.0)  # test_cmagic_id
    gps_17 = make_same_gps(17.0)
    gps_18 = make_same_gps(18.0)
    gps_19 = make_same_gps(19.0)
    gps_20 = make_same_gps(20.0)
    gps_21 = make_same_gps(21.0)
    gps_22 = make_same_gps(22.0)
    gps_23 = make_same_gps(23.0)
    gps_24 = make_same_gps(24.0)
    gps_25 = make_same_gps(25.0)
    gps_26 = make_same_gps(26.0)
    gps_27 = make_same_gps(27.0)
    gps_28 = make_same_gps(28.0)

    # gps без складов
    gps_80 = make_same_gps(80.0)
    gps_81 = make_same_gps(81.0)
    gps_82 = make_same_gps(82.0)


def make_mock_rearr(rearr=''):
    suffix = 'parallel_smm=1.0;ext_snippet=1;no_snippet_arc=1;market_enable_sins_offers_wizard=1'
    if rearr != '':
        rearr += ';'
    return rearr + suffix


def make_rearr(**kwds):
    kvlist = ['{}={}'.format(key, kwds[key]) for key in kwds]
    kvlist.sort(key=lambda x: x[0])
    return ';'.join(kvlist)


class _Make_Rearrs:
    make_rearr_0 = make_rearr(
        market_use_business_offer=0,
        express_offers_hyperlocality=1,
        rty_dynamics=1,
        rty_stock_dynamics=RSD_DISABLED | RSD_FORCE,
    )

    make_rearr_1 = make_rearr(
        market_use_business_offer=USE_EXPRESS,
        express_offers_hyperlocality=1,
        rty_dynamics=1,
        rty_stock_dynamics=RSD_DISABLED | RSD_FORCE,
    )

    make_rearr_2 = make_rearr(
        market_use_business_offer=0,
        express_offers_hyperlocality=0,
    )

    make_rearr_3 = make_rearr(
        market_use_business_offer=0,
        express_offers_hyperlocality=1,
    )

    make_rearr_4 = make_rearr(
        market_use_business_offer=2,
        express_offers_hyperlocality=2,
    )

    make_rearr_5 = make_rearr(
        market_use_business_offer=0,
        express_offers_hyperlocality=2,
    )

    make_rearr_6 = make_rearr(
        market_metadoc_search='offers',
        market_use_business_offer=USE_EXPRESS,
        express_offers_hyperlocality=1,
        rty_dynamics=1,
        rty_stock_dynamics=RSD_DISABLED | RSD_FORCE,
    )

    make_rearr_7 = make_rearr(
        market_use_business_offer=2,
        express_offers_hyperlocality=1,
    )

    make_rearr_8 = make_rearr(
        market_use_business_offer=0,
    )

    make_rearr_9 = make_rearr(
        market_use_business_offer=1,
    )

    make_rearr_10 = make_rearr(
        market_use_business_offer=2,
    )

    make_rearr_11 = make_rearr(
        market_use_business_offer=USE_EXPRESS,
        express_offers_hyperlocality=2,
        rty_dynamics=1,
        rty_stock_dynamics=RSD_DISABLED | RSD_FORCE,
    )

    make_rearr_12 = make_rearr(
        market_use_business_offer=2,
        express_offers_hyperlocality=1,
    )


class _Make_Mock_Rearrs:
    make_mock_rearr_0 = make_mock_rearr(_Make_Rearrs.make_rearr_0)

    make_mock_rearr_1 = make_mock_rearr(_Make_Rearrs.make_rearr_1)

    make_mock_rearr_2 = make_mock_rearr(_Make_Rearrs.make_rearr_2)

    make_mock_rearr_3 = make_mock_rearr(_Make_Rearrs.make_rearr_3)

    make_mock_rearr_4 = make_mock_rearr(_Make_Rearrs.make_rearr_4)

    make_mock_rearr_5 = make_mock_rearr(_Make_Rearrs.make_rearr_5)

    make_mock_rearr_6 = make_mock_rearr(_Make_Rearrs.make_rearr_6)

    make_mock_rearr_7 = make_mock_rearr(_Make_Rearrs.make_rearr_7)

    make_mock_rearr_8 = make_mock_rearr(_Make_Rearrs.make_rearr_8)

    make_mock_rearr_9 = make_mock_rearr(_Make_Rearrs.make_rearr_9)

    make_mock_rearr_10 = make_mock_rearr(_Make_Rearrs.make_rearr_10)

    make_mock_rearr_11 = make_mock_rearr(_Make_Rearrs.make_rearr_11)

    make_mock_rearr_12 = make_mock_rearr(_Make_Rearrs.make_rearr_12)


class _Shops(object):
    blue_virtual_shop = Shop(
        fesh=999,
        datafeed_id=8888,
        priority_region=213,
        name='Beru!',
        fulfillment_virtual=True,
        virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
    )


def create_promo(feed_id, offer_id):
    return Promo(
        promo_type=PromoType.PROMO_CODE,
        promo_code='usual_promocode',
        description='usual_promocode',
        discount_value=15,
        feed_id=feed_id,
        key=b64url_md5(next(_Const.nummer)),
        url='http://promocode.com/',
        landing_url='http://promocode.com/',
        mechanics_payment_type=MechanicsPaymentType.CPA,
        shop_promo_id='promocode_{}_{}'.format(feed_id, offer_id),
        promo_internal_priority=4,
        offers_matching_rules=[
            OffersMatchingRules(
                feed_offer_ids=[
                    [feed_id, 'offer_id_{}'.format(offer_id)],
                ]
            )
        ],
    )


class _Promos(object):
    express_service_promo = create_promo(SERVICE_FEED_ID, 'ExpressOffer1')
    express_business_promo = create_promo(BUSINESS_FEED_ID, 'ExpressOffer1')
    only_service_promo = create_promo(33, 'PineappleDouble')


class _Offers(object):
    express_service_offer = BlueOffer(
        offerid='ExpressOffer1',
        is_express=True,
        price=35,
        feedid=SERVICE_FEED_ID,
        waremd5=Offer.generate_waremd5('express_service_offer'),
        cmagic=Offer.generate_cmagic('express_service_offer'),
        promo=_Promos.express_service_promo,
    )

    express_business_offer = BlueOffer(
        offerid='ExpressOffer1',
        is_express=True,
        price=100501,
        feedid=BUSINESS_FEED_ID,
        waremd5=Offer.generate_waremd5('express_business_offer'),
        cmagic=Offer.generate_cmagic('express_business_offer'),
        promo=_Promos.express_business_promo,
    )

    express_hidden_offer = BlueOffer(
        offerid='ExpressOffer1',
        is_express=True,
        price=1,
        feedid=HIDDEN_FEED_ID,
        waremd5=Offer.generate_waremd5('express_hidden_offer'),
        cmagic=Offer.generate_cmagic('express_hidden_offer'),
        disabled_flags=DisabledFlags.build_offer_disabled([DisabledFlags.MARKET_STOCK], []),
    )

    express_business_offer_20 = BlueOffer(
        offerid='ExpressOffer2',
        is_express=True,
        price=100501,
        feedid=BUSINESS_FEED_ID,
        waremd5=Offer.generate_waremd5('expr_biz_offer_20'),
        cmagic=Offer.generate_cmagic('expr_biz_offer_20'),
    )

    express_service_offer_21_hidden = BlueOffer(
        offerid='ExpressOffer2',
        is_express=True,
        price=35,
        feedid=HIDDEN_FEED_ID,
        waremd5=Offer.generate_waremd5('expr_srv_offer_21_hid'),
        cmagic=Offer.generate_cmagic('expr_srv_offer_21_hid'),
        has_gone=True,
    )

    express_business_offer_30 = BlueOffer(
        offerid='ExpressOffer3',
        is_express=True,
        price=55555,
        feedid=BUSINESS_FEED_ID,
        waremd5=Offer.generate_waremd5('expr_biz_offer_30'),
        cmagic=Offer.generate_cmagic('expr_biz_offer_30'),
    )

    express_business_offer_20_hidden = BlueOffer(
        offerid='Naushniki',
        is_express=True,
        price=100501,
        feedid=BUSINESS_FEED_ID,
        waremd5=Offer.generate_waremd5('expr_biz_offer_20_hid'),
        cmagic=Offer.generate_cmagic('expr_biz_offer_20_hid'),
        has_gone=True,
    )

    express_service_offer_21 = BlueOffer(
        offerid='Naushniki',
        is_express=True,
        price=35,
        feedid=HIDDEN_FEED_ID,
        waremd5=Offer.generate_waremd5('expr_srv_offer_21'),
        cmagic=Offer.generate_cmagic('expr_srv_offer_21'),
    )

    express_pineapple_offer_1 = BlueOffer(
        offerid='PineappleSingle',
        is_express=True,
        business_id=31,
        price=350,
        fesh=31,
        feedid=31,
        waremd5=Offer.generate_waremd5('pineapples_1'),
        cmagic=Offer.generate_cmagic('pineapples_1'),
    )

    express_pineapple_offer_2 = BlueOffer(
        offerid='PineappleDouble',
        is_express=True,
        business_id=32,
        price=35,
        fesh=32,
        feedid=32,
        waremd5=Offer.generate_waremd5('pineapples_2'),
        cmagic=Offer.generate_cmagic('pineapples_2'),
    )

    express_pineapple_offer_3 = BlueOffer(
        offerid='PineappleDouble',
        is_express=True,
        business_id=32,
        price=200,
        fesh=33,
        feedid=33,
        waremd5=Offer.generate_waremd5('pineapples_3'),
        cmagic=Offer.generate_cmagic('pineapples_3'),
        promo=_Promos.only_service_promo,
    )

    general_blue_offer = BlueOffer(
        offerid='GeneralOffer',
        is_express=False,
        business_id=100,
        price=101,
        fesh=101,
        feedid=102,
        waremd5=Offer.generate_waremd5('general_offer'),
        cmagic=Offer.generate_cmagic('general_offer'),
    )

    general_blue_offer_from_express_business = BlueOffer(
        offerid='ExpressOffer1',
        is_express=False,
        price=327000,
        feedid=202,
        waremd5=Offer.generate_waremd5('general_offer_conflict'),
        cmagic=Offer.generate_cmagic('general_offer_conflict'),
    )

    # Оферы из разных регионов
    business_offer_ekb = BlueOffer(
        offerid='regional',
        is_express=True,
        price=111,
        feedid=EKB_FEED_ID,
        waremd5=Offer.generate_waremd5('regional_busines_ekb'),
        cmagic=Offer.generate_cmagic('regional_busines_ekb'),
    )
    service_offer_msk = BlueOffer(
        offerid='regional',
        is_express=True,
        price=112,
        feedid=SERVICE_FEED_ID,
        waremd5=Offer.generate_waremd5('regional_service_msk'),
        cmagic=Offer.generate_cmagic('regional_service_msk'),
    )


def generate_sample(offer, warehouse, new_price=None):
    return {
        'entity': 'offer',
        'wareId': offer.waremd5,
        'prices': {'value': str(new_price or offer.price)},
        'shop': {
            'feed': {'offerId': '{}.{}'.format(offer.feedid, offer.offerid)},
        },
        'supplier': {
            'warehouseId': warehouse,
        },
    }


def generate_hidden_sample(offer):
    return {
        'entity': 'offer',
        'wareId': offer.waremd5,
    }


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.rty_use_service_offers = True
        cls.settings.rty_qpipe = True
        cls.settings.report_subrole = 'market'
        cls.index.creation_time = int(time.time()) // 60 * 60
        cls.dynamic.market_dynamic.creation_time = 5

        # Через файл экспресс фидов могут устанавливаться любые экспресс офера,
        # но только офера, установленные в ЛМС, могут считаться экспрессами для схлопывания в бизнес офер
        # Поэтому сюда я запишу единственный фид, которые не должен быть реальным экспресс
        cls.index.express_partners.suppliers += [
            ExpressSupplier(
                feed_id=202,
                supplier_id=201,
                warehouse_id=203,
            )
        ]

        cls.index.shops += [
            _Shops.blue_virtual_shop,
            Shop(
                fesh=SERVICE_FEED_ID,
                business_fesh=EXPRESS_BUSINESS_ID,
                datafeed_id=SERVICE_FEED_ID,
                blue='REAL',
                warehouse_id=SERVICE_FEED_ID,
                with_express_warehouse=True,
            ),
            Shop(
                fesh=BUSINESS_FEED_ID,
                business_fesh=EXPRESS_BUSINESS_ID,
                datafeed_id=BUSINESS_FEED_ID,
                blue='REAL',
                warehouse_id=BUSINESS_FEED_ID,
                with_express_warehouse=True,
            ),
            Shop(
                fesh=BUSINESS_FEED_ID,  # Специально сделал два фида у одного shop_id
                business_fesh=EXPRESS_BUSINESS_ID,
                datafeed_id=HIDDEN_FEED_ID,
                blue='REAL',
                warehouse_id=HIDDEN_FEED_ID,
                with_express_warehouse=True,
            ),
            Shop(
                fesh=EKB_FEED_ID,
                business_fesh=EXPRESS_BUSINESS_ID,
                datafeed_id=EKB_FEED_ID,
                blue='REAL',
                warehouse_id=EKB_FEED_ID,
                with_express_warehouse=True,
            ),
            Shop(
                business_fesh=100,
                fesh=101,
                datafeed_id=102,
                blue='REAL',
                warehouse_id=103,
                with_express_warehouse=False,
            ),
            Shop(
                business_fesh=EXPRESS_BUSINESS_ID,
                fesh=201,
                datafeed_id=202,
                blue='REAL',
                warehouse_id=203,
                with_express_warehouse=False,
            ),
            Shop(
                business_fesh=31,
                fesh=31,
                datafeed_id=31,
                blue='REAL',
                warehouse_id=31,
                with_express_warehouse=True,
            ),
            Shop(
                business_fesh=32,
                fesh=32,
                datafeed_id=32,
                blue='REAL',
                warehouse_id=32,
                with_express_warehouse=True,
            ),
            Shop(
                business_fesh=33,
                fesh=33,
                datafeed_id=33,
                blue='REAL',
                warehouse_id=33,
                with_express_warehouse=True,
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                hyperid=1,
                title='рябчики',
                sku=2,
                blue_offers=[
                    _Offers.express_business_offer,
                    _Offers.express_service_offer,
                    _Offers.express_hidden_offer,
                    _Offers.general_blue_offer_from_express_business,
                ],
            ),
            MarketSku(
                hyperid=101,
                title='рябчики обычные',
                sku=101,
                blue_offers=[
                    _Offers.general_blue_offer,
                ],
            ),
            MarketSku(
                hyperid=1,
                title='алебарды',
                sku=3,
                blue_offers=[
                    _Offers.express_business_offer_20,
                    _Offers.express_service_offer_21_hidden,
                ],
            ),
            MarketSku(
                hyperid=1,
                title='наушники',
                sku=4,
                blue_offers=[
                    _Offers.express_business_offer_20_hidden,
                    _Offers.express_service_offer_21,
                ],
            ),
            MarketSku(
                hyperid=1,
                title='pineapples',
                sku=5,
                blue_offers=[
                    _Offers.express_pineapple_offer_1,
                    _Offers.express_pineapple_offer_2,
                    _Offers.express_pineapple_offer_3,
                ],
            ),
            MarketSku(
                hyperid=2, title='regional', sku=6, blue_offers=[_Offers.business_offer_ekb, _Offers.service_offer_msk]
            ),
            MarketSku(
                hyperid=5,
                title='эспандеры',
                sku=7,
                blue_offers=[
                    _Offers.express_business_offer_30,
                ],
            ),
        ]

    @classmethod
    def prepare_lms(cls):
        cls.dynamic.lms += [
            DynamicWarehousesPriorityInRegion(
                region=MSK,
                warehouses=[BUSINESS_FEED_ID, SERVICE_FEED_ID, HIDDEN_FEED_ID, EKB_FEED_ID],
            ),
            DynamicWarehousesPriorityInRegion(
                region=EKB,
                warehouses=[EKB_FEED_ID],
            ),
        ]

        cls.dynamic.lms += [
            DynamicWarehouseInfo(
                id=SERVICE_FEED_ID,
                home_region=MSK,
                is_express=True,
            ),
            DynamicWarehouseInfo(
                id=BUSINESS_FEED_ID,
                home_region=MSK,
                is_express=True,
            ),
        ]

    @classmethod
    def prepare_nordstream(cls):
        cls.index.regiontree += [
            Region(rid=MSK, name='Москва'),
            Region(rid=EKB, name='Екатеринбург'),
        ]

        cls.dynamic.nordstream += [
            DynamicWarehouseDelivery(
                EKB_FEED_ID,
                {
                    EKB: [
                        DynamicDeliveryRestriction(
                            max_phys_weight=40000,
                            max_dim_sum=250,
                            max_dimensions=[100, 100, 100],
                            min_days=3,
                            max_days=4,
                        ),
                    ],
                },
            ),
            DynamicWarehouseDelivery(
                SERVICE_FEED_ID,
                {
                    MSK: [
                        DynamicDeliveryRestriction(
                            max_phys_weight=40000,
                            max_dim_sum=250,
                            max_dimensions=[100, 100, 100],
                            min_days=3,
                            max_days=4,
                        ),
                    ],
                },
            ),
        ]

    @classmethod
    def prepare_nearest_delivery_from_combinator(cls):
        cls.combinatorExpress.on_express_warehouses_request(
            region_id=213,
            gps_coords=CombinatorGpsCoords(0.0, 0.0),
            rear_factors=_Make_Mock_Rearrs.make_mock_rearr_0,
        ).respond_with_express_warehouses(
            [
                _Express_Warehouses.service,
            ]
        )
        cls.combinatorExpress.on_express_warehouses_request(
            region_id=213,
            gps_coords=CombinatorGpsCoords(1.0, 1.0),
            rear_factors=_Make_Mock_Rearrs.make_mock_rearr_1,
        ).respond_with_express_warehouses(
            [
                _Express_Warehouses.service,
            ]
        )
        cls.combinatorExpress.on_express_warehouses_request(
            region_id=213,
            gps_coords=CombinatorGpsCoords(2.0, 2.0),
            rear_factors=_Make_Mock_Rearrs.make_mock_rearr_1,
        ).respond_with_express_warehouses(
            [
                _Express_Warehouses.business,
            ]
        )
        cls.combinatorExpress.on_express_warehouses_request(
            region_id=213,
            gps_coords=CombinatorGpsCoords(3.0, 3.0),
            rear_factors=_Make_Mock_Rearrs.make_mock_rearr_1,
        ).respond_with_express_warehouses(
            [
                _Express_Warehouses.hidden,
            ]
        )
        cls.combinatorExpress.on_express_warehouses_request(
            region_id=213,
            gps_coords=CombinatorGpsCoords(4.0, 4.0),
            rear_factors=_Make_Mock_Rearrs.make_mock_rearr_2,
        ).respond_with_express_warehouses(
            [
                _Express_Warehouses.business,
            ]
        )
        cls.combinatorExpress.on_express_warehouses_request(
            region_id=213,
            gps_coords=CombinatorGpsCoords(5.0, 5.0),
            rear_factors=_Make_Mock_Rearrs.make_mock_rearr_2,
        ).respond_with_express_warehouses(
            [
                _Express_Warehouses.service,
            ]
        )
        cls.combinatorExpress.on_express_warehouses_request(
            region_id=213,
            gps_coords=CombinatorGpsCoords(6.0, 6.0),
            rear_factors=_Make_Mock_Rearrs.make_mock_rearr_3,
        ).respond_with_express_warehouses(
            [
                _Express_Warehouses.business,
            ]
        )
        cls.combinatorExpress.on_express_warehouses_request(
            region_id=213,
            gps_coords=CombinatorGpsCoords(7.0, 7.0),
            rear_factors=_Make_Mock_Rearrs.make_mock_rearr_3,
        ).respond_with_express_warehouses(
            [
                _Express_Warehouses.service,
            ]
        )
        cls.combinatorExpress.on_express_warehouses_request(
            region_id=213,
            gps_coords=CombinatorGpsCoords(8.0, 8.0),
            rear_factors=_Make_Mock_Rearrs.make_mock_rearr_4,
        ).respond_with_express_warehouses(
            [
                _Express_Warehouses.business,
            ]
        )
        cls.combinatorExpress.on_express_warehouses_request(
            region_id=213,
            gps_coords=CombinatorGpsCoords(9.0, 9.0),
            rear_factors=_Make_Mock_Rearrs.make_mock_rearr_4,
        ).respond_with_express_warehouses(
            [
                _Express_Warehouses.service,
            ]
        )
        cls.combinatorExpress.on_express_warehouses_request(
            region_id=213,
            gps_coords=CombinatorGpsCoords(10.0, 10.0),
            rear_factors=_Make_Mock_Rearrs.make_mock_rearr_4,
        ).respond_with_express_warehouses(
            [
                _Express_Warehouses.business,
                _Express_Warehouses.service,
            ]
        )
        cls.combinatorExpress.on_express_warehouses_request(
            region_id=213,
            gps_coords=CombinatorGpsCoords(11.0, 11.0),
            rear_factors=_Make_Mock_Rearrs.make_mock_rearr_5,
        ).respond_with_express_warehouses(
            [
                _Express_Warehouses.service,
            ]
        )
        cls.combinatorExpress.on_express_warehouses_request(
            region_id=213,
            gps_coords=CombinatorGpsCoords(12.0, 12.0),
            rear_factors=_Make_Mock_Rearrs.make_mock_rearr_4,
        ).respond_with_express_warehouses(
            [
                _Express_Warehouses.hidden,
            ]
        )
        cls.combinatorExpress.on_express_warehouses_request(
            region_id=213,
            gps_coords=CombinatorGpsCoords(13.0, 13.0),
            rear_factors=_Make_Mock_Rearrs.make_mock_rearr_6,
        ).respond_with_express_warehouses(
            [
                _Express_Warehouses.business,
                _Express_Warehouses.service,
            ]
        )
        cls.combinatorExpress.on_express_warehouses_request(
            region_id=213,
            gps_coords=CombinatorGpsCoords(14.0, 14.0),
            rear_factors=_Make_Mock_Rearrs.make_mock_rearr_1,
        ).respond_with_express_warehouses(
            [
                _Express_Warehouses.business,
                _Express_Warehouses.service,
            ]
        )
        cls.combinatorExpress.on_express_warehouses_request(
            region_id=213,
            gps_coords=CombinatorGpsCoords(14.5, 14.5),
            rear_factors=_Make_Mock_Rearrs.make_mock_rearr_7,
        ).respond_with_express_warehouses(
            [
                _Express_Warehouses.business,
                _Express_Warehouses.service,
            ]
        )
        cls.combinatorExpress.on_express_warehouses_request(
            region_id=213,
            gps_coords=CombinatorGpsCoords(17.0, 17.0),
            rear_factors=_Make_Mock_Rearrs.make_mock_rearr_8,
        ).respond_with_express_warehouses(
            [
                _Express_Warehouses.service,
            ]
        )
        cls.combinatorExpress.on_express_warehouses_request(
            region_id=213,
            gps_coords=CombinatorGpsCoords(18.0, 18.0),
            rear_factors=_Make_Mock_Rearrs.make_mock_rearr_9,
        ).respond_with_express_warehouses(
            [
                _Express_Warehouses.service,
            ]
        )
        cls.combinatorExpress.on_express_warehouses_request(
            region_id=213,
            gps_coords=CombinatorGpsCoords(19.0, 19.0),
            rear_factors=_Make_Mock_Rearrs.make_mock_rearr_10,
        ).respond_with_express_warehouses(
            [
                _Express_Warehouses.service,
            ]
        )
        cls.combinatorExpress.on_express_warehouses_request(
            region_id=213,
            gps_coords=CombinatorGpsCoords(20.0, 20.0),
            rear_factors=_Make_Mock_Rearrs.make_mock_rearr_8,
        ).respond_with_express_warehouses(
            [
                _Express_Warehouses.business,
            ]
        )
        cls.combinatorExpress.on_express_warehouses_request(
            region_id=213,
            gps_coords=CombinatorGpsCoords(21.0, 21.0),
            rear_factors=_Make_Mock_Rearrs.make_mock_rearr_9,
        ).respond_with_express_warehouses(
            [
                _Express_Warehouses.business,
            ]
        )
        cls.combinatorExpress.on_express_warehouses_request(
            region_id=213,
            gps_coords=CombinatorGpsCoords(22.0, 22.0),
            rear_factors=_Make_Mock_Rearrs.make_mock_rearr_10,
        ).respond_with_express_warehouses(
            [
                _Express_Warehouses.business,
            ]
        )
        cls.combinatorExpress.on_express_warehouses_request(
            region_id=213,
            gps_coords=CombinatorGpsCoords(23.0, 23.0),
            rear_factors=_Make_Mock_Rearrs.make_mock_rearr_8,
        ).respond_with_express_warehouses(
            [
                _Express_Warehouses._33,
            ]
        )
        cls.combinatorExpress.on_express_warehouses_request(
            region_id=213,
            gps_coords=CombinatorGpsCoords(24.0, 24.0),
            rear_factors=_Make_Mock_Rearrs.make_mock_rearr_9,
        ).respond_with_express_warehouses(
            [
                _Express_Warehouses._33,
            ]
        )
        cls.combinatorExpress.on_express_warehouses_request(
            region_id=213,
            gps_coords=CombinatorGpsCoords(25.0, 25.0),
            rear_factors=_Make_Mock_Rearrs.make_mock_rearr_10,
        ).respond_with_express_warehouses(
            [
                _Express_Warehouses._33,
            ]
        )
        cls.combinatorExpress.on_express_warehouses_request(
            region_id=213,
            gps_coords=CombinatorGpsCoords(26.0, 26.0),
            rear_factors=_Make_Mock_Rearrs.make_mock_rearr_8,
        ).respond_with_express_warehouses(
            [
                _Express_Warehouses._32,
            ]
        )
        cls.combinatorExpress.on_express_warehouses_request(
            region_id=213,
            gps_coords=CombinatorGpsCoords(27.0, 27.0),
            rear_factors=_Make_Mock_Rearrs.make_mock_rearr_9,
        ).respond_with_express_warehouses(
            [
                _Express_Warehouses._32,
            ]
        )
        cls.combinatorExpress.on_express_warehouses_request(
            region_id=213,
            gps_coords=CombinatorGpsCoords(28.0, 28.0),
            rear_factors=_Make_Mock_Rearrs.make_mock_rearr_10,
        ).respond_with_express_warehouses(
            [
                _Express_Warehouses._32,
            ]
        )
        cls.combinatorExpress.on_express_warehouses_request(
            region_id=213,
            gps_coords=CombinatorGpsCoords(80.0, 80.0),
            rear_factors=_Make_Mock_Rearrs.make_mock_rearr_11,
        ).respond_with_express_warehouses([])
        cls.combinatorExpress.on_express_warehouses_request(
            region_id=213,
            gps_coords=CombinatorGpsCoords(81.0, 81.0),
            rear_factors=_Make_Mock_Rearrs.make_mock_rearr_1,
        ).respond_with_express_warehouses([])
        cls.combinatorExpress.on_express_warehouses_request(
            region_id=213,
            gps_coords=CombinatorGpsCoords(82.0, 82.0),
            rear_factors=_Make_Mock_Rearrs.make_mock_rearr_12,
        ).respond_with_express_warehouses([])

    def test_use_business_offer_flag_on_express(self):
        def generate_offer_id(offer):
            return "{}.{}".format(offer.feedid, offer.offerid)

        def check_general_offer(response):
            self.assertFragmentIn(response, generate_sample(_Offers.general_blue_offer, 103))

        base_request = 'place=prime&rids=213&rgb=blue&text=рябчики&gps={gps}&debug=da&rearr-factors='
        # Запрос без флага. Нашли самый дешевый офер
        request = base_request.format(gps=_GPS_Coords.gps_0)
        request += _Make_Rearrs.make_rearr_0
        response = self.report.request_json(request + '&reqid=000')
        self.assertFragmentIn(response, generate_sample(_Offers.express_service_offer, SERVICE_FEED_ID))
        check_general_offer(response)
        self.assertFragmentNotIn(response, {'price': '100501'})
        self.show_log.expect(
            supplier_id=SERVICE_FEED_ID,
            feed_id=_Shops.blue_virtual_shop.datafeed_id,
            ware_md5=_Offers.express_service_offer.waremd5,
            offer_id=generate_offer_id(_Offers.express_service_offer),
            warehouse_id=SERVICE_FEED_ID,
            reqid='000',
        )

        # Запрос за оферами на складе сервисного офера
        request = base_request.format(gps=_GPS_Coords.gps_1)
        request += _Make_Rearrs.make_rearr_1

        response = self.report.request_json(request + '&reqid=111')
        self.assertFragmentIn(response, generate_sample(_Offers.express_service_offer, SERVICE_FEED_ID))
        check_general_offer(response)
        self.assertFragmentNotIn(response, generate_hidden_sample(_Offers.express_business_offer))
        self.show_log.expect(
            supplier_id=SERVICE_FEED_ID,
            feed_id=_Shops.blue_virtual_shop.datafeed_id,
            ware_md5=_Offers.express_service_offer.waremd5,
            offer_id=generate_offer_id(_Offers.express_service_offer),
            warehouse_id=SERVICE_FEED_ID,
            reqid='111',
        )
        # Теперь запрос не ограничен фидами. Нужный сервисный берется исходя из склада
        self.assertFragmentNotIn(response, {'text': [Regex('warehouse_id')]})
        self.assertFragmentIn(response, {'filters': {"SERVICE_OFFER": 2}})

        # Запрос за оферами на складе бизнес офера
        request = base_request.format(gps=_GPS_Coords.gps_2)
        request += _Make_Rearrs.make_rearr_1

        response = self.report.request_json(request + '&reqid=222')
        self.assertFragmentIn(response, generate_sample(_Offers.express_business_offer, BUSINESS_FEED_ID))
        check_general_offer(response)
        self.assertFragmentNotIn(response, generate_hidden_sample(_Offers.express_service_offer))
        self.show_log.expect(
            supplier_id=BUSINESS_FEED_ID,
            feed_id=_Shops.blue_virtual_shop.datafeed_id,
            ware_md5=_Offers.express_business_offer.waremd5,
            offer_id=generate_offer_id(_Offers.express_business_offer),
            warehouse_id=BUSINESS_FEED_ID,
            reqid='222',
        )

        # Запрос за товаром со скрытого склада
        # Обычный синий офер остается на выдаче
        request = base_request.format(gps=_GPS_Coords.gps_3)
        request += _Make_Rearrs.make_rearr_1
        response = self.report.request_json(request)
        self.assertFragmentIn(response, generate_sample(_Offers.general_blue_offer, 103), allow_different_len=False)

        # Проверяем изменение цены
        self.rty.offers += [
            RtyOffer(
                feedid=_Offers.express_service_offer.feedid, offerid=_Offers.express_service_offer.offerid, price=100600
            )
        ]

        request = base_request.format(gps=_GPS_Coords.gps_1)
        request += _Make_Rearrs.make_rearr_1
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response, generate_sample(_Offers.express_service_offer, SERVICE_FEED_ID, new_price=100600)
        )

        request = base_request.format(gps=_GPS_Coords.gps_2)
        request += _Make_Rearrs.make_rearr_1
        response = self.report.request_json(request)
        self.assertFragmentIn(response, generate_sample(_Offers.express_business_offer, BUSINESS_FEED_ID))

        self.rty.offers += [
            RtyOffer(
                feedid=_Offers.express_business_offer.feedid,
                offerid=_Offers.express_business_offer.offerid,
                price=133,
                modification_time=self.index.creation_time + 1,
            )
        ]

        request = base_request.format(gps=_GPS_Coords.gps_1)
        request += _Make_Rearrs.make_rearr_1
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response, generate_sample(_Offers.express_service_offer, SERVICE_FEED_ID, new_price=100600)
        )

        request = base_request.format(gps=_GPS_Coords.gps_2)
        request += _Make_Rearrs.make_rearr_1
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response, generate_sample(_Offers.express_business_offer, BUSINESS_FEED_ID, new_price=133)
        )

        # Скрытие офера
        hidden_offer = generate_hidden_sample(_Offers.express_business_offer)
        self.rty.offers += [
            RtyOffer(
                feedid=_Offers.express_business_offer.feedid,
                offerid=_Offers.express_business_offer.offerid,
                price=133,
                disabled=DisabledFlags.build_offer_disabled([DisabledFlags.MARKET_STOCK], [DisabledFlags.MARKET_STOCK]),
                disabled_ts=self.index.creation_time + 4,
                modification_time=self.index.creation_time + 4,
                version=self.index.creation_time + 4,
            )
        ]

        request = base_request.format(gps=_GPS_Coords.gps_1)
        request += _Make_Rearrs.make_rearr_1
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response, generate_sample(_Offers.express_service_offer, SERVICE_FEED_ID, new_price=100600)
        )

        request = base_request.format(gps=_GPS_Coords.gps_2)
        request += _Make_Rearrs.make_rearr_1
        response = self.report.request_json(request)
        self.assertFragmentNotIn(response, hidden_offer)

        # Снова разрешаем показ офера
        self.rty.offers += [
            RtyOffer(
                feedid=_Offers.express_business_offer.feedid,
                offerid=_Offers.express_business_offer.offerid,
                price=133,
                disabled=0,
                disabled_ts=self.index.creation_time + 5,
                modification_time=self.index.creation_time + 5,
                version=self.index.creation_time + 5,
            )
        ]
        request = base_request.format(gps=_GPS_Coords.gps_2)
        request += _Make_Rearrs.make_rearr_1
        response = self.report.request_json(request)
        self.assertFragmentIn(response, hidden_offer)

    def test_rty_after_restart(self):

        request = 'place=prime&rgb=blue&rids=213&text=эспандеры&gps={gps}&rearr-factors='.format(gps=_GPS_Coords.gps_2)
        request += _Make_Rearrs.make_rearr_1
        request += '&debug=da'

        response = self.report.request_json(request)
        self.assertFragmentIn(response, generate_sample(_Offers.express_business_offer_30, BUSINESS_FEED_ID))

        self.rty.offers += [
            RtyOffer(
                feedid=_Offers.express_business_offer_30.feedid,
                offerid=_Offers.express_business_offer_30.offerid,
                price=333,
                modification_time=self.index.creation_time + 1,
            )
        ]

        def check_rty_price():
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response, generate_sample(_Offers.express_business_offer_30, BUSINESS_FEED_ID, new_price=333)
            )

        check_rty_price()

        # Закрываем сегменты индекса и при этом сохраняем маппинг из айдишников репорта в айдишники rty
        self.rty_controller.reopen_indexes()

        self.stop_report()
        self.restart_report()

        # Проверяем, что после рестарта rty-цена не потерялась
        check_rty_price()

    def test_service_offer_flags_passing_from_generation(self):

        query_params = [
            'place=prime',
            'text={text}',
            'rearr-factors={}'.format(_Make_Rearrs.make_rearr_1),
            'gps={gps}',
            'rids=213',
        ]
        request_template = '&'.join(query_params)

        def do_request(text, gps_for_warehouse):
            return self.report.request_json(request_template.format(text=text, gps=gps_for_warehouse))

        # business offer should be shown
        response = do_request('алебарды', _GPS_Coords.gps_2)
        self.assertFragmentIn(response, generate_sample(_Offers.express_business_offer_20, BUSINESS_FEED_ID))

        # service offer should be hidden by has_gone=True in generation
        response = do_request('алебарды', _GPS_Coords.gps_3)
        self.assertFragmentNotIn(response, generate_hidden_sample(_Offers.express_service_offer_21_hidden))

        # business offer should be hidden by has_gone=True in generation
        response = do_request('наушники', _GPS_Coords.gps_2)
        self.assertFragmentNotIn(response, generate_hidden_sample(_Offers.express_business_offer_20_hidden))

        # service offer should be shown
        response = do_request('наушники', _GPS_Coords.gps_3)
        self.assertFragmentIn(response, generate_sample(_Offers.express_service_offer_21, HIDDEN_FEED_ID))

    def test_shoffer_id(self):
        base_query = (
            # изменил rid на 213, т.к. с 0 регионом не получиться получить время доставки в ветке с gps координатами
            'place=offerinfo&rids=213&regset=1&show-urls=&rearr-factors={rearr}'
            '&gps={gps}'
            '&debug=da'
        )

        query = base_query + '&feed_shoffer_id={feed_shoffer_id}'
        query_offerid = base_query + '&offerid={offerid}'

        def create_feed_shoffer_id(offer):
            return '{}-{}'.format(offer.feedid, offer.offerid)

        SERVICE_FEED_SHOFFER_ID = create_feed_shoffer_id(_Offers.express_service_offer)
        BUSINESS_FEED_SHOFFER_ID = create_feed_shoffer_id(_Offers.express_business_offer)

        def create_virtual_feed_shoffer_id(offer):
            return '{}-{}.{}'.format(_Shops.blue_virtual_shop.datafeed_id, offer.feedid, offer.offerid)

        VIRTUAL_SERVICE_FEED_SHOFFER_ID = create_virtual_feed_shoffer_id(_Offers.express_service_offer)
        VIRTUAL_BUSINESS_FEED_SHOFFER_ID = create_virtual_feed_shoffer_id(_Offers.express_business_offer)

        SERVICE_WAREMD5 = _Offers.express_service_offer.waremd5
        BUSINESS_WAREMD5 = _Offers.express_business_offer.waremd5

        # Без флага и без гиперлокальности мы берем оффер по feed_shoffer_id
        for gps in [_GPS_Coords.gps_4, _GPS_Coords.gps_5]:
            for id in [BUSINESS_FEED_SHOFFER_ID, VIRTUAL_BUSINESS_FEED_SHOFFER_ID]:
                response = self.report.request_json(
                    query.format(
                        feed_shoffer_id=id,
                        gps=gps,
                        rearr=_Make_Rearrs.make_rearr_2,
                    )
                )
                self.assertFragmentIn(response, {'wareId': BUSINESS_WAREMD5})

            for id in [SERVICE_FEED_SHOFFER_ID, VIRTUAL_SERVICE_FEED_SHOFFER_ID]:
                response = self.report.request_json(
                    query.format(
                        feed_shoffer_id=id,
                        gps=gps,
                        rearr=_Make_Rearrs.make_rearr_2,
                    )
                )
                self.assertFragmentIn(response, {'wareId': SERVICE_WAREMD5})

        # гиперлокальность. Тот же склад. мы берем оффер по feed_shoffer_id
        for id in [BUSINESS_FEED_SHOFFER_ID, VIRTUAL_BUSINESS_FEED_SHOFFER_ID]:
            response = self.report.request_json(
                query.format(
                    feed_shoffer_id=id,
                    gps=_GPS_Coords.gps_6,
                    rearr=_Make_Rearrs.make_rearr_3,
                )
            )
            self.assertFragmentIn(response, {'wareId': BUSINESS_WAREMD5})

        for id in [SERVICE_FEED_SHOFFER_ID, VIRTUAL_SERVICE_FEED_SHOFFER_ID]:
            response = self.report.request_json(
                query.format(
                    feed_shoffer_id=id,
                    gps=_GPS_Coords.gps_7,
                    rearr=_Make_Rearrs.make_rearr_3,
                )
            )
            self.assertFragmentIn(response, {'wareId': SERVICE_WAREMD5})

        # гиперлокальность. Другой склад. Выдача будет пустая
        response = self.report.request_json(
            query.format(
                feed_shoffer_id=BUSINESS_FEED_SHOFFER_ID,
                gps=_GPS_Coords.gps_7,
                rearr=_Make_Rearrs.make_rearr_3,
            )
        )
        self.assertFragmentIn(response, {'results': EmptyList()})
        response = self.report.request_json(
            query.format(
                feed_shoffer_id=SERVICE_FEED_SHOFFER_ID,
                rearr=_Make_Rearrs.make_rearr_3,
                gps=_GPS_Coords.gps_6,
            )
        )
        self.assertFragmentIn(response, {'results': EmptyList()})

        # С флагом подменяем оффер по складу
        for feed_shoffer_id in [SERVICE_FEED_SHOFFER_ID, BUSINESS_FEED_SHOFFER_ID]:
            response = self.report.request_json(
                query.format(
                    feed_shoffer_id=feed_shoffer_id,
                    gps=_GPS_Coords.gps_8,
                    rearr=_Make_Rearrs.make_rearr_4,
                )
            )
            self.assertFragmentIn(response, {'wareId': BUSINESS_WAREMD5})

            response = self.report.request_json(
                query.format(
                    feed_shoffer_id=feed_shoffer_id,
                    gps=_GPS_Coords.gps_9,
                    rearr=_Make_Rearrs.make_rearr_4,
                )
            )
            self.assertFragmentIn(response, {'wareId': SERVICE_WAREMD5})

        # но приоритет у запрошенного офера
        for feed_shoffer_id, ware_md5 in [
            (SERVICE_FEED_SHOFFER_ID, SERVICE_WAREMD5),
            (BUSINESS_FEED_SHOFFER_ID, BUSINESS_WAREMD5),
        ]:
            response = self.report.request_json(
                query.format(
                    feed_shoffer_id=feed_shoffer_id,
                    rearr=_Make_Rearrs.make_rearr_4,
                    gps=_GPS_Coords.gps_10,
                )
            )
            self.assertFragmentIn(response, {'wareId': ware_md5})

            response = self.report.request_json(
                query_offerid.format(
                    offerid=ware_md5,
                    gps=_GPS_Coords.gps_10,
                    rearr=_Make_Rearrs.make_rearr_4,
                )
            )
            self.assertFragmentIn(response, {'wareId': ware_md5})

    def test_use_business_offer_flag_on_offer_info(self):
        query = (
            'place=offerinfo&text=&rgb=green&offerid={waremd5}&rids=213&show-urls=&regset=1'
            '&rearr-factors={rearr}'
            '&gps={gps}'
            '&debug=da'
        )

        response = self.report.request_json(
            query.format(
                waremd5=_Offers.express_service_offer.waremd5,
                gps=_GPS_Coords.gps_11,
                rearr=_Make_Rearrs.make_rearr_5,
            )
        )
        self.assertFragmentIn(response, {'wareId': '{}'.format(_Offers.express_service_offer.waremd5)})

        response = self.report.request_json(
            query.format(
                waremd5=_Offers.express_service_offer.waremd5,
                gps=_GPS_Coords.gps_9,
                rearr=_Make_Rearrs.make_rearr_4,
            )
        )
        self.assertFragmentIn(response, {'wareId': '{}'.format(_Offers.express_service_offer.waremd5)})
        response = self.report.request_json(
            query.format(
                waremd5=_Offers.express_service_offer.waremd5,
                gps=_GPS_Coords.gps_12,
                rearr=_Make_Rearrs.make_rearr_4,
            )
        )
        self.assertFragmentIn(response, {'wareId': '{}'.format(_Offers.express_hidden_offer.waremd5)})

    def test_express_without_hyperlocal(self):
        '''
        Проверяем работу экспресс оферов без передачи списка локальных складов
        '''
        request = 'place=prime&rgb=blue&text=рябчики&rearr-factors=market_use_business_offer={};rty_dynamics=1'
        request += '&rearr-factors=rty_stock_dynamics={}'.format(RSD_DISABLED | RSD_FORCE)
        request += '&rearr-factors=market_show_express_out_of_working_hours=1'

        response = self.report.request_json(request.format(USE_EXPRESS) + '&reqid=111')
        self.assertFragmentIn(response, generate_sample(_Offers.express_business_offer, BUSINESS_FEED_ID))
        self.assertFragmentIn(response, generate_sample(_Offers.general_blue_offer, 103))

        # Скрытие бизнес офера
        _ = generate_hidden_sample(_Offers.express_business_offer)
        self.rty.offers += [
            RtyOffer(
                feedid=_Offers.express_business_offer.feedid,
                offerid=_Offers.express_business_offer.offerid,
                price=133,
                disabled=DisabledFlags.build_offer_disabled([DisabledFlags.MARKET_STOCK], [DisabledFlags.MARKET_STOCK]),
                disabled_ts=self.index.creation_time + 4,
                modification_time=self.index.creation_time + 4,
                version=self.index.creation_time + 4,
            )
        ]

        response = self.report.request_json(request.format(USE_EXPRESS) + '&reqid=111')
        self.assertFragmentIn(response, generate_sample(_Offers.express_service_offer, SERVICE_FEED_ID))
        self.assertFragmentIn(response, generate_sample(_Offers.general_blue_offer, 103))

    def test_express_filter(self):
        '''
        Проверяем показ только экспресс оферов
        '''
        request = 'place=prime&rids=213&rgb=blue&text=рябчики&rearr-factors='
        request += _Make_Rearrs.make_rearr_1
        request += '&gps={gps}'
        request += '&filter-express-delivery=1'
        request += '&debug=da'

        # Запрос за оферами на складе сервисного офера
        response = self.report.request_json(request.format(gps=_GPS_Coords.gps_1))
        # Не экспресс офер пропал из выдачи
        self.assertFragmentIn(
            response, generate_sample(_Offers.express_service_offer, SERVICE_FEED_ID), allow_different_len=False
        )
        self.assertFragmentIn(response, {'text': [Regex('is_express:"1"')]})

    def test_without_express_warehouse(self):
        '''
        Проверяем фильтрацию экспресс оферов, если по текущим координатам нет экспресс складов
        '''
        request = 'place=prime&rgb=blue&rids=213&text=рябчики&gps={gps}&rearr-factors='
        request += _Make_Rearrs.make_rearr_11
        request += '&debug=da'

        # Запрос за оферами на складе сервисного офера
        response = self.report.request_json(request.format(gps=_GPS_Coords.gps_80))
        # экспресс офер пропал из выдачи
        self.assertFragmentIn(response, generate_sample(_Offers.general_blue_offer, 103), allow_different_len=False)
        self.assertFragmentIn(response, {'text': [Regex('is_express:"0"')]})

    def test_not_express_offer_with_conflicted_business_id(self):
        '''
        Проверяем поиск не сервисного офера по его feed_shoffer_id
        Такой идентификатор не должен подменяться на бизнес идентификатор фида
        '''
        request = 'place=offerinfo&feed_shoffer_id=202-ExpressOffer1&rids=213&regset=1&show-urls=&rearr-factors=market_use_business_offer=2'
        request += '&rearr-factors=market_show_express_out_of_working_hours=1'

        response = self.report.request_json(request)
        offer = _Offers.general_blue_offer_from_express_business
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'wareId': offer.waremd5,
                'prices': {'value': str(offer.price)},
                'shop': {
                    'feed': {'id': str(offer.feedid), 'offerId': str(offer.offerid)},
                },
                'supplier': {
                    'warehouseId': 203,
                },
            },
        )

    def test_express_metadoc(self):
        base_query = 'place=prime&text={text}&debug=da'

        warehouses_query = (
            base_query
            + '&rearr-factors=market_metadoc_search=offers;market_use_business_offer={use_business_flag};express_offers_hyperlocality=1;rty_dynamics=1;market_show_express_out_of_working_hours=1;'
            + 'rty_stock_dynamics={}'.format(RSD_DISABLED | RSD_FORCE)
        )

        # несколько бизнесов на одном МСКУ, выбираем самый дешевый оффер
        for flag in [0, 2]:
            response = self.report.request_json(base_query.format(text='pineapples', use_business_flag=flag))
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'wareId': Offer.generate_waremd5('pineapples_2'),
                            'sku': '5',
                        },
                    ]
                },
                allow_different_len=False,
            )
            self.assertFragmentIn(response, {"logicTrace": [Regex("IsMetadocSearchOffers: 1")]})

        warehouses_query = base_query
        warehouses_query += '&rearr-factors={rearr}'.format(rearr=_Make_Rearrs.make_rearr_6)
        warehouses_query += '&gps={gps}'

        # несколько МСКУ - экспресс и обычный
        response = self.report.request_json(
            warehouses_query.format(
                text='рябчики',
                gps=_GPS_Coords.gps_13,
            )
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'wareId': _Offers.express_business_offer.waremd5,
                        'sku': '2',
                    },
                    {
                        'wareId': Offer.generate_waremd5('general_offer'),
                        'sku': '101',
                    },
                ]
            },
            allow_different_len=False,
        )

        self.assertFragmentIn(response, {"logicTrace": [Regex("IsMetadocSearchOffers: 1")]})

        # После блокировки бизнес офера будет выбран сервисный
        self.rty.offers += [
            RtyOffer(
                feedid=_Offers.express_business_offer.feedid,
                offerid=_Offers.express_business_offer.offerid,
                price=133,
                disabled=DisabledFlags.build_offer_disabled([DisabledFlags.MARKET_STOCK], [DisabledFlags.MARKET_STOCK]),
                disabled_ts=self.index.creation_time + 4,
                modification_time=self.index.creation_time + 4,
                version=self.index.creation_time + 4,
            )
        ]

        response = self.report.request_json(
            warehouses_query.format(
                text='рябчики',
                gps=_GPS_Coords.gps_13,
            )
            + "&debug=da"
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'wareId': _Offers.express_service_offer.waremd5,
                        'sku': '2',
                    },
                    {
                        'wareId': Offer.generate_waremd5('general_offer'),
                        'sku': '101',
                    },
                ]
            },
            allow_different_len=False,
        )

        # Разблокируем бизнес и блокируем сервисный
        self.rty.offers += [
            RtyOffer(
                feedid=_Offers.express_business_offer.feedid,
                offerid=_Offers.express_business_offer.offerid,
                price=133,
                disabled=0,
                disabled_ts=self.index.creation_time + 4,
                modification_time=self.index.creation_time + 4,
                version=self.index.creation_time + 4,
            )
        ]
        self.rty.offers += [
            RtyOffer(
                feedid=_Offers.express_service_offer.feedid,
                offerid=_Offers.express_service_offer.offerid,
                price=133,
                disabled=DisabledFlags.build_offer_disabled([DisabledFlags.MARKET_STOCK], [DisabledFlags.MARKET_STOCK]),
                disabled_ts=self.index.creation_time + 4,
                modification_time=self.index.creation_time + 4,
                version=self.index.creation_time + 4,
            )
        ]
        response = self.report.request_json(
            warehouses_query.format(
                text='рябчики',
                gps=_GPS_Coords.gps_13,
            )
            + "&debug=da"
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'wareId': _Offers.express_business_offer.waremd5,
                        'sku': '2',
                    },
                    {
                        'wareId': Offer.generate_waremd5('general_offer'),
                        'sku': '101',
                    },
                ]
            },
            allow_different_len=False,
        )

    def test_region_delivery(self):
        '''
        Проверяем доставку в разные регионы
        Доставка проверяется для сервисного офера
        '''
        request = 'place=offerinfo&regset=2&show-urls=&rids={region}'
        request += '&rearr-factors=market_use_business_offer={use_business_flag}'
        request += '&rearr-factors=market_show_express_out_of_working_hours=1'

        for offer, region in [(_Offers.business_offer_ekb, 54), (_Offers.service_offer_msk, 213)]:
            response = self.report.request_json(
                request.format(use_business_flag=2, region=region)
                + '&feed_shoffer_id={}-{}'.format(offer.feedid, offer.offerid)
            )
            self.assertFragmentIn(response, {'results': [{'wareId': offer.waremd5}]}, allow_different_len=False)

        for offer, region, founded in [
            (_Offers.business_offer_ekb, 213, _Offers.service_offer_msk),
            (_Offers.service_offer_msk, 54, _Offers.business_offer_ekb),
        ]:
            response = self.report.request_json(
                request.format(use_business_flag=2, region=region)
                + '&feed_shoffer_id={}-{}'.format(offer.feedid, offer.offerid)
            )
            self.assertFragmentIn(response, {'results': [{'wareId': founded.waremd5}]}, allow_different_len=False)

    def test_hyperlocal_business_offerinfo_without_warehouses(self):
        '''
        Если не передали список складов, но при этом гиперлокальность в режиме совместимости (переключается в просто поиск оферов),
        то офер все-равно должен быть доступен
        '''
        request = 'place=offerinfo&regset=2&show-urls=&rids=213'
        request += '&rearr-factors=market_use_business_offer=2'
        request += '&rearr-factors=express_offers_hyperlocality=1'
        request += '&rearr-factors=market_show_express_out_of_working_hours=1'

        offer = _Offers.service_offer_msk
        response = self.report.request_json(request + '&feed_shoffer_id={}-{}'.format(offer.feedid, offer.offerid))

        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'wareId': offer.waremd5,
                        'shop': {
                            'feed': {'id': str(offer.feedid), 'offerId': str(offer.offerid)},
                        },
                    }
                ]
            },
            allow_different_len=False,
        )

    def test_actual_delivery_without_hyperlocal(self):
        '''
        Проверяем получение нужного сервисного офера при отсутствии гиперлокальности
        '''
        request = (
            'place=actual_delivery&rids={region}&offers-list={id}:1'
            '&rearr-factors=market_use_business_offer=2'
            '&rearr-factors=market_show_express_out_of_working_hours=1'
        )

        offers = [
            (_Offers.service_offer_msk, MSK),
            (_Offers.business_offer_ekb, EKB),
            (_Offers.express_service_offer, MSK),
            (_Offers.express_business_offer, MSK),
        ]

        extra_params = [
            '',
            '&rearr-factors=express_offers_hyperlocality=0',
            '&rearr-factors=express_offers_hyperlocality=1',  # Без &express-warehouses=
        ]

        for offer, region in offers:
            for hyperlocal_param in extra_params:
                response = self.report.request_json(request.format(id=offer.waremd5, region=region) + hyperlocal_param)
                self.assertFragmentIn(
                    response,
                    {
                        'results': [
                            {
                                'offers': [
                                    {
                                        'wareId': offer.waremd5,
                                    }
                                ]
                            }
                        ]
                    },
                )

    def test_product_offers(self):
        '''
        Проверка получения дефолтного офера с мультискладом, учитывая фильтрацию релевантности до байбокса
        '''
        request = (
            '&place=productoffers'
            '&offers-set=defaultList,listCpa'
            '&grhow=supplier'
            '&rids=213'
            '&show-urls=external'
            '&cpa=real'
            '&regset=2'
            '&hyperid=1'
            '&bsformat=2'
            '&showdiscounts=1'
            '&client=frontend'
            '&platform=desktop'
            '&market-sku=2'
        )
        request += '&rearr-factors=' + _Make_Rearrs.make_rearr_1

        request_do = '&do-waremd5={waremd5}'

        def check(blocked_offer, requested_offer, wh_gps):
            # Проверяем, что приоритет запрошенного офера работает. Выбирается один из двух оферов
            response = self.report.request_json(request + request_do.format(waremd5=requested_offer.waremd5) + wh_gps)
            self.assertFragmentIn(
                response,
                {'results': [{'entity': 'offer', 'wareId': requested_offer.waremd5, 'benefit': {"type": "waremd5"}}]},
            )

            # Без приоритета берется всегда сервисный
            response = self.report.request_json(request + wh_gps)
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'offer',
                            'wareId': _Offers.express_business_offer.waremd5,
                            'benefit': {"type": "express-cpa"},
                        },
                        {
                            'entity': 'offer',
                            'wareId': _Offers.express_business_offer.waremd5,
                            'benefit': {"type": "cheapest"},
                        },
                    ]
                },
            )

            # Блокируем смежный офер
            self.rty.offers += [
                RtyOffer(
                    feedid=blocked_offer.feedid,
                    offerid=blocked_offer.offerid,
                    price=133,
                    disabled=DisabledFlags.build_offer_disabled(
                        [DisabledFlags.MARKET_STOCK], [DisabledFlags.MARKET_STOCK]
                    ),
                    disabled_ts=self.index.creation_time + 4,
                    modification_time=self.index.creation_time + 4,
                    version=self.index.creation_time + 4,
                )
            ]

            # Есть ДО в запросе
            response = self.report.request_json(request + request_do.format(waremd5=requested_offer.waremd5) + wh_gps)
            self.assertFragmentIn(
                response,
                {'results': [{'entity': 'offer', 'wareId': requested_offer.waremd5, 'benefit': {"type": "waremd5"}}]},
            )

            # Без ДО берется всегда доступный из двух возможных
            response = self.report.request_json(request + wh_gps)
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {'entity': 'offer', 'wareId': requested_offer.waremd5, 'benefit': {"type": "express-cpa"}},
                        {'entity': 'offer', 'wareId': requested_offer.waremd5, 'benefit': {"type": "cheapest"}},
                    ]
                },
            )

            # Разблокируем смежный офер
            self.rty.offers += [
                RtyOffer(
                    feedid=blocked_offer.feedid,
                    offerid=blocked_offer.offerid,
                    price=133,
                    disabled=0,
                    disabled_ts=self.index.creation_time + 4,
                    modification_time=self.index.creation_time + 4,
                    version=self.index.creation_time + 4,
                )
            ]

        check(
            blocked_offer=_Offers.express_business_offer,
            requested_offer=_Offers.express_service_offer,
            wh_gps='&gps=' + _GPS_Coords.gps_81,
        )

        check(
            blocked_offer=_Offers.express_service_offer,
            requested_offer=_Offers.express_business_offer,
            wh_gps='&gps=' + _GPS_Coords.gps_81,
        )

        check(
            blocked_offer=_Offers.express_business_offer,
            requested_offer=_Offers.express_service_offer,
            wh_gps='&gps=' + _GPS_Coords.gps_14,
        )
        check(
            blocked_offer=_Offers.express_service_offer,
            requested_offer=_Offers.express_business_offer,
            wh_gps='&gps=' + _GPS_Coords.gps_14,
        )

    def test_supplier_filter(self):
        '''
        Проверяем, что фильтр по поставщику применяется к сервисному оферу
        '''
        request = '&place=prime' '&rids=213' '&show-urls=external' '&regset=2' '&hyperid=1' '&bsformat=2' '&gps={gps}'
        request += '&rearr-factors='
        request += _Make_Rearrs.make_rearr_7

        def check(blocked_shop, expected_offers, wh_gps):
            self.dynamic.market_dynamic.disabled_blue_suppliers = [
                DynamicShop(blocked_shop),
            ]
            response = self.report.request_json(request.format(gps=wh_gps))
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'offer',
                            'wareId': offer.waremd5,
                        }
                        for offer in expected_offers
                    ]
                },
                allow_different_len=False,
            )

        check(BUSINESS_FEED_ID, [_Offers.express_service_offer, _Offers.express_pineapple_offer_2], _GPS_Coords.gps_82)

        check(
            SERVICE_FEED_ID,
            [
                _Offers.express_business_offer_20,
                _Offers.express_pineapple_offer_2,
                _Offers.express_service_offer_21,
                _Offers.express_business_offer,
            ],
            _GPS_Coords.gps_82,
        )

        # Дополнительно ограничиваем складами
        self.breakpoint("debug next request")
        check(BUSINESS_FEED_ID, [_Offers.express_service_offer], _GPS_Coords.gps_14_5)

        check(
            SERVICE_FEED_ID, [_Offers.express_business_offer_20, _Offers.express_business_offer], _GPS_Coords.gps_14_5
        )

    def test_promo(self):
        '''
        Проверяем, что промо берутся от сервисного офера.
        Вне зависимости от того, есть ли промо у бизнес документа
        '''
        request = (
            '&place=prime'
            '&rids=213'
            '&show-urls=external'
            '&regset=2'
            '&hyperid=1'
            '&bsformat=2'
            '&debug=da'
            '&rearr-factors=market_use_business_offer={}'
        )

        # для каждого use_business свои координаты
        warehouse_express_service_gps = [
            _GPS_Coords.gps_17,
            _GPS_Coords.gps_18,
            _GPS_Coords.gps_19,
        ]

        warehouse_express_business_gps = [
            _GPS_Coords.gps_20,
            _GPS_Coords.gps_21,
            _GPS_Coords.gps_22,
        ]

        warehouse_express_33_gps = [
            _GPS_Coords.gps_23,
            _GPS_Coords.gps_24,
            _GPS_Coords.gps_25,
        ]

        warehouse_express_32_gps = [
            _GPS_Coords.gps_26,
            _GPS_Coords.gps_27,
            _GPS_Coords.gps_28,
        ]

        def check(expected_offer, expected_promo, warehouse_gps):
            for use_business in [0, 2]:
                response = self.report.request_json(
                    request.format(use_business) + '&gps=' + warehouse_gps[use_business]
                )
                promos = Absent()
                if expected_promo:
                    promos = [
                        {
                            'shopPromoId': str(expected_promo),
                        }
                    ]
                self.assertFragmentIn(
                    response,
                    {
                        'results': [
                            {
                                'entity': 'offer',
                                'wareId': expected_offer.waremd5,
                                'promos': promos,
                            }
                        ]
                    },
                )

        check(_Offers.express_service_offer, _Promos.express_service_promo.shop_promo_id, warehouse_express_service_gps)
        check(
            _Offers.express_business_offer, _Promos.express_business_promo.shop_promo_id, warehouse_express_business_gps
        )
        check(_Offers.express_pineapple_offer_3, _Promos.only_service_promo.shop_promo_id, warehouse_express_33_gps)
        check(_Offers.express_pineapple_offer_2, None, warehouse_express_32_gps)


if __name__ == '__main__':
    main()
