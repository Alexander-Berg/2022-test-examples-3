#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.matcher import Absent
from core.testcase import main, TestCase
from core.types import (
    DeliveryBucket,
    DeliveryOption,
    DeliveryServiceRegionToRegionInfo,
    DynamicDaysSet,
    DynamicDeliveryRestriction,
    DynamicDeliveryServiceInfo,
    DynamicWarehouseDelivery,
    DynamicWarehouseInfo,
    DynamicWarehouseLink,
    DynamicWarehousesPriorityInRegion,
    Offer,
    OfferDimensions,
    Region,
    RegionalDelivery,
    Currency,
    ExchangeRate,
    SupplierRegionRestriction,
    generate_dsbs,
)
from core.types.shop import Shop
from core.types.sku import BlueOffer, MarketSku
from core.types.autogen import b64url_md5
from itertools import count

nummer = count()

RID_RU = 225
RID_BL = 149
RID_MSK = 213
RID_MINSK_AREA = 29630
RID_MINSK_CITY = 157

HID_ID = 94732
VENDOR_ID = 194732
MODEL_ID = 3521

RU_SORTING_CENTER_ID = 12345
BL_SORTING_CENTER_ID = 54321


def build_ware_md5(id):
    return id.ljust(21, "_") + "w"


# офферы
def create_blue_offer(
    price,
    fesh,
    feed,
    vendor_id,
    hid,
    delivery_buckets=None,
    pickup_buckets=None,
    is_fulfillment=True,
    name=None,
    post_term_delivery=None,
    price_old=None,
    weight=1,
    dimensions=OfferDimensions(width=10, length=10, height=10),
):

    if name is not None:
        offerid = name
        waremd5 = build_ware_md5(name)
    else:
        num = next(nummer)
        offerid = 'offerid_{}'.format(num)
        waremd5 = b64url_md5(num)

    return BlueOffer(
        waremd5=waremd5,
        price=price,
        price_old=price_old,
        fesh=fesh,
        feedid=feed,
        offerid=offerid,
        delivery_buckets=delivery_buckets,
        pickup_buckets=pickup_buckets,
        is_fulfillment=is_fulfillment,
        post_term_delivery=post_term_delivery,
        vendor_id=vendor_id,
        hid=hid,
        weight=5,
        blue_weight=5,
        title=name,
        dimensions=OfferDimensions(length=10, width=20, height=30),
        blue_dimensions=OfferDimensions(length=10, width=20, height=30),
    )


def create_white_offer(price, fesh, hyperid, feedid, sku, hid, title, vendor_id, delivery_buckets=None, is_dsbs=False):
    def build_ware_md5(id):
        return id.ljust(21, "_") + "w"

    if title is not None:
        offerid = title
        waremd5 = build_ware_md5(title)
    else:
        num = next(nummer)
        offerid = 'offerid_{}'.format(num)
        waremd5 = b64url_md5(num)

    return Offer(
        fesh=fesh,
        waremd5=waremd5,
        hyperid=hyperid,
        hid=hid,
        sku=sku,
        price=price,
        cpa=Offer.CPA_REAL if is_dsbs else Offer.CPA_NO,
        delivery_buckets=delivery_buckets,
        offerid=offerid,
        feedid=feedid,
        title=title,
        vendor_id=vendor_id,
    )


# Market SKU
def create_msku(offers_list=None, hid=None, num=None):
    num = num or next(nummer)
    msku_args = dict(
        sku=num,
        hyperid=num,
    )
    if offers_list is not None:
        assert isinstance(offers_list, list)
        msku_args["blue_offers"] = offers_list
    if hid is not None:
        msku_args.update(hid=hid)
    return MarketSku(**msku_args)


class RuShop:
    FEED_ID = 777
    SHOP_ID = 7770
    shop = Shop(
        fesh=SHOP_ID,
        datafeed_id=FEED_ID,
        priority_region=RID_MSK,
        supplier_type=Shop.THIRD_PARTY,
        blue=Shop.BLUE_REAL,
        warehouse_id=145,
        cpa=Shop.CPA_REAL,
        fulfillment_program=True,
    )

    blue_offer_ru = create_blue_offer(
        price=1000,
        price_old=2912,
        fesh=SHOP_ID,
        feed=FEED_ID,
        name="russian_offer",
        vendor_id=VENDOR_ID,
        hid=HID_ID,
        delivery_buckets=[],
        pickup_buckets=[],
        weight=1,
        dimensions=OfferDimensions(width=10, height=10, length=10),
    )

    offers = [
        blue_offer_ru,
    ]


class BlShop:
    FEED_ID = 888
    SHOP_ID = 8880
    shop = Shop(
        fesh=SHOP_ID,
        datafeed_id=FEED_ID,
        home_region=RID_BL,
        currency=Currency.BYN,
        priority_region=RID_MINSK_AREA,
        supplier_type=Shop.THIRD_PARTY,
        blue=Shop.BLUE_REAL,
        cpa=Shop.CPA_REAL,
        warehouse_id=321,
        fulfillment_program=True,
    )

    blue_offer_bl = create_blue_offer(
        price=2912,
        fesh=SHOP_ID,
        feed=FEED_ID,
        name="belarusian_offer",
        vendor_id=VENDOR_ID,
        hid=HID_ID,
        delivery_buckets=[],
        pickup_buckets=[],
    )

    offers = [blue_offer_bl]


class RuDsbsShop:
    FEED_ID = 11111
    SHOP_ID = 111110

    shop = Shop(
        fesh=SHOP_ID,
        datafeed_id=FEED_ID,
        priority_region=RID_MSK,
        name='Russian DSBS shop',
        cpa=Shop.CPA_REAL,
        warehouse_id=35984,
    )

    # dsbs оффер
    ru_dsbs_offer = create_white_offer(
        price=29120,
        sku=next(nummer),
        hyperid=MODEL_ID,
        hid=HID_ID,
        fesh=SHOP_ID,
        feedid=FEED_ID,
        title="russian_offer_dsbs",
        is_dsbs=True,
        vendor_id=VENDOR_ID,
        delivery_buckets=[42],
    )

    offers = [ru_dsbs_offer]


class BlDsbsShop:
    FEED_ID = 22222
    SHOP_ID = 222220

    shop = Shop(
        fesh=SHOP_ID,
        datafeed_id=FEED_ID,
        home_region=RID_BL,
        priority_region=RID_MINSK_AREA,
        currency=Currency.BYN,
        name='Belarusian DSBS shop',
        cpa=Shop.CPA_REAL,
        warehouse_id=35985,
    )

    # dsbs оффер
    bl_dsbs_offer = create_white_offer(
        price=1000,
        sku=next(nummer),
        hyperid=MODEL_ID,
        hid=HID_ID,
        fesh=SHOP_ID,
        feedid=FEED_ID,
        title="belarusian_offer_dsbs",
        is_dsbs=True,
        vendor_id=VENDOR_ID,
        delivery_buckets=[45],
    )

    offers = [bl_dsbs_offer]


class RuCpcShop:
    FEED_ID = 33333
    SHOP_ID = 333330

    shop = Shop(
        fesh=SHOP_ID,
        datafeed_id=FEED_ID,
        priority_region=RID_MSK,
        name='Russian CPC Shop',
        cpa=Shop.CPA_NO,
    )
    offers = [
        create_white_offer(
            price=1000,
            sku=next(nummer),
            hyperid=MODEL_ID,
            hid=HID_ID,
            vendor_id=VENDOR_ID,
            fesh=SHOP_ID,
            feedid=FEED_ID,
            title="russian_cpc_offer",
            is_dsbs=False,
        )
    ]


class RuRestrictedShop:
    FEED_ID = 999
    SHOP_ID = 9990
    shop = Shop(
        fesh=SHOP_ID,
        datafeed_id=FEED_ID,
        priority_region=RID_MSK,
        supplier_type=Shop.THIRD_PARTY,
        blue=Shop.BLUE_REAL,
        warehouse_id=150,
        cpa=Shop.CPA_REAL,
        fulfillment_program=True,
    )

    blue_restricted_offer_ru = create_blue_offer(
        price=10000000,
        fesh=SHOP_ID,
        feed=FEED_ID,
        name="rus_restricted_offer",
        vendor_id=VENDOR_ID,
        hid=HID_ID,
        delivery_buckets=[],
        pickup_buckets=[],
    )

    offers = [blue_restricted_offer_ru]


class BlCpcShop:
    FEED_ID = 44444
    SHOP_ID = 444440

    shop = Shop(
        fesh=SHOP_ID,
        datafeed_id=FEED_ID,
        home_region=RID_BL,
        priority_region=RID_MINSK_AREA,
        currency=Currency.BYN,
        name='Belarusian CPC Shop',
        cpa=Shop.CPA_NO,
    )
    offers = [
        create_white_offer(
            price=2912,
            sku=next(nummer),
            hyperid=MODEL_ID,
            hid=HID_ID,
            vendor_id=VENDOR_ID,
            fesh=SHOP_ID,
            feedid=FEED_ID,
            title="belarusian_cpc_offer",
            is_dsbs=False,
        )
    ]


class Shops:
    all_shops = [RuShop.shop, BlShop.shop, RuDsbsShop.shop, BlDsbsShop.shop, RuRestrictedShop.shop]
    ru_shops = [RuShop.shop, RuDsbsShop.shop, RuRestrictedShop.shop]
    bl_shops = [BlShop.shop, BlDsbsShop.shop]


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.lms_autogenerate = False
        cls.settings.default_search_experiment_flags += ['enable_cart_split_on_combinator=0']
        cls.settings.nordstream_autogenerate = False
        region_tree = [
            Region(
                rid=RID_MSK,
                name='Москва',
            ),
            Region(
                rid=RID_BL,
                name='Беларусь',
                region_type=Region.COUNTRY,
                children=[
                    Region(
                        rid=RID_MINSK_AREA, name='Минская область', children=[Region(rid=RID_MINSK_CITY, name='Минск')]
                    )
                ],
            ),
        ]

        cls.index.supplier_region_restrictions += [
            SupplierRegionRestriction(supplier_id=RuRestrictedShop.SHOP_ID, region_ids=[149])
        ]
        cls.index.regiontree += region_tree
        cls.index.currencies += [
            Currency(
                name=Currency.BYN,
                exchange_rates=[
                    ExchangeRate(to=Currency.RUR, rate=29.12),
                ],
                country=149,
            )
        ]

        for shop in Shops.all_shops:
            cls.dynamic.lms.append(DynamicWarehouseInfo(id=shop.warehouse_id, home_region=shop.priority_region))

        cls.dynamic.lms += [
            DynamicDaysSet(key=1, days=[]),
            DynamicWarehousesPriorityInRegion(region=RID_RU, warehouses=[shop.warehouse_id for shop in Shops.ru_shops]),
            DynamicWarehousesPriorityInRegion(region=RID_BL, warehouses=[shop.warehouse_id for shop in Shops.bl_shops]),
            DynamicDeliveryServiceInfo(
                9,
                "DPD",
                region_to_region_info=[
                    DeliveryServiceRegionToRegionInfo(region_from=213, region_to=RID_RU, days_key=1),
                    DeliveryServiceRegionToRegionInfo(region_from=213, region_to=RID_BL, days_key=1),
                    DeliveryServiceRegionToRegionInfo(region_from=RID_MINSK_AREA, region_to=RID_BL, days_key=1),
                ],
            ),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=42,
                dc_bucket_id=42,
                fesh=RuShop.SHOP_ID,
                carriers=[99],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
                regional_options=[
                    RegionalDelivery(rid=RID_MSK, options=[DeliveryOption(price=100, day_from=1, day_to=1)]),
                    RegionalDelivery(rid=RID_MINSK_AREA, options=[DeliveryOption(price=250, day_from=3, day_to=4)]),
                ],
            ),
            DeliveryBucket(
                bucket_id=43,
                fesh=BlShop.SHOP_ID,
                carriers=[99],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
                regional_options=[
                    RegionalDelivery(rid=RID_MINSK_AREA, options=[DeliveryOption(price=100, day_from=1, day_to=5)])
                ],
            ),
            DeliveryBucket(
                bucket_id=44,
                fesh=RuDsbsShop.SHOP_ID,
                carriers=[99],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
                regional_options=[
                    RegionalDelivery(rid=RID_MSK, options=[DeliveryOption(price=100, day_from=1, day_to=1)]),
                    RegionalDelivery(rid=RID_MINSK_AREA, options=[DeliveryOption(price=300, day_from=3, day_to=5)]),
                ],
            ),
            DeliveryBucket(
                bucket_id=45,
                fesh=BlDsbsShop.SHOP_ID,
                carriers=[99],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
                regional_options=[
                    RegionalDelivery(rid=RID_MINSK_AREA, options=[DeliveryOption(price=100, day_from=1, day_to=1)])
                ],
            ),
            DeliveryBucket(
                bucket_id=46,
                fesh=RuRestrictedShop.SHOP_ID,
                carriers=[99],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
                regional_options=[
                    RegionalDelivery(rid=RID_MSK, options=[DeliveryOption(price=100, day_from=1, day_to=1)]),
                    RegionalDelivery(rid=RID_MINSK_AREA, options=[DeliveryOption(price=250, day_from=3, day_to=4)]),
                ],
            ),
        ]

        cls.delivery_calc.on_request_offer_buckets(weight=5, height=10, length=30, width=20, warehouse_id=145).respond(
            [42], [], []
        )

        cls.index.shops += [
            RuShop.shop,
            BlShop.shop,
            RuDsbsShop.shop,
            BlDsbsShop.shop,
            RuCpcShop.shop,
            BlCpcShop.shop,
            RuRestrictedShop.shop,
        ]
        for shop in [RuShop, BlShop, RuRestrictedShop]:
            cls.index.mskus += [create_msku([offer], hid=HID_ID, num=MODEL_ID) for offer in shop.offers]

        cls.index.offers += RuDsbsShop.offers
        cls.index.offers += BlDsbsShop.offers
        cls.index.offers += RuCpcShop.offers
        cls.index.offers += BlCpcShop.offers

    @classmethod
    def prepare_nordstream(cls):

        cls.dynamic.nordstream += [
            DynamicWarehouseLink(shop.warehouse_id, [shop.warehouse_id]) for shop in Shops.all_shops
        ]

        cls.dynamic.nordstream += [
            DynamicWarehouseDelivery(
                shop.warehouse_id,
                {
                    213: [
                        DynamicDeliveryRestriction(
                            max_phys_weight=20000,
                            max_dim_sum=150,
                            max_dimensions=[50, 50, 50],
                            prohibited_cargo_types=[2],
                            max_payment_weight=50,
                            density=10,
                            min_days=1,
                            max_days=3,
                        ),
                        DynamicDeliveryRestriction(
                            max_phys_weight=40000,
                            max_dim_sum=250,
                            max_dimensions=[100, 100, 100],
                            min_days=3,
                            max_days=4,
                        ),
                    ],
                    RID_RU: [
                        DynamicDeliveryRestriction(
                            max_phys_weight=50000, max_dim_sum=220, max_dimensions=[80, 80, 80], min_days=5, max_days=6
                        )
                    ],
                    RID_BL: [
                        DynamicDeliveryRestriction(
                            max_phys_weight=50000, max_dim_sum=220, max_dimensions=[80, 80, 80], min_days=7, max_days=10
                        )
                    ],
                },
            )
            for shop in Shops.ru_shops
        ]

        cls.dynamic.nordstream += [
            DynamicWarehouseDelivery(
                shop.warehouse_id,
                {
                    RID_MINSK_AREA: [
                        DynamicDeliveryRestriction(
                            max_phys_weight=20000,
                            max_dim_sum=150,
                            max_dimensions=[50, 50, 50],
                            prohibited_cargo_types=[2],
                            max_payment_weight=50,
                            density=10,
                            min_days=1,
                            max_days=3,
                        ),
                        DynamicDeliveryRestriction(
                            max_phys_weight=40000,
                            max_dim_sum=250,
                            max_dimensions=[100, 100, 100],
                            min_days=3,
                            max_days=4,
                        ),
                    ],
                    RID_BL: [
                        DynamicDeliveryRestriction(
                            max_phys_weight=50000, max_dim_sum=220, max_dimensions=[80, 80, 80], min_days=5, max_days=6
                        )
                    ],
                },
            )
            for shop in Shops.bl_shops
        ]

        cls.dynamic.nordstream += generate_dsbs(cls.index)

    def test_cpa_restriction(self):
        '''
        Делаем запрос в productoffers (он самый показательный, т.к. выдаст все офферы)
        Ожидаем, что без rearr-флага cpa_enabled_countries в Беларуси увидим только cpc офферы
        С флагом - видим все офферы
        '''
        request = 'place=productoffers&rids={rids}&hyperid={hyper_id}&debug=da'
        rearr_flags = ['', '&rearr-factors=cpa_enabled_countries=149']
        '''
        Для России ответ должен быть одинаковым независимо от значения реарр-флага
        '''
        ru_response = {
            "search": {
                "results": [
                    {"entity": "offer", "wareId": "russian_offer_dsbs___w"},
                    {"entity": "offer", "wareId": "russian_cpc_offer____w"},
                    {"entity": "offer", "wareId": "russian_offer________w"},
                    {"entity": "offer", "wareId": "rus_restricted_offer_w"},
                ]
            }
        }
        '''
        С включенной cpa выдачей в Беларуси мы показываем российские CPA офферы,
        беларусские CPA и CPC офферы. DBS офферы должны быть скрыты
        '''
        bl_cpa_response = {
            "search": {
                "results": [
                    {"entity": "offer", "wareId": "belarusian_cpc_offer_w"},
                    {"entity": "regionalDelimiter"},
                    {"entity": "offer", "wareId": "belarusian_offer_____w"},
                    {"entity": "offer", "wareId": "russian_offer________w"},
                ]
            }
        }
        '''
        С выключенной cpa выдачей в Беларуси мы показываем беларусские CPС офферы
        '''
        bl_non_cpa_response = {
            "search": {
                "results": [
                    {"entity": "offer", "wareId": "belarusian_cpc_offer_w"},
                ]
            }
        }
        '''
        Проверяем, что работает скрытие CPA по стране
        В случае выключенного rearr - скрываем CPA офферы
        В случае включенного  rearr - скрываем DBS
        '''
        hide_reason_response = {
            "brief": {
                "filters": {
                    "CPA_DISABLED_IN_COUNTRY": 2,
                }
            }
        }

        for rid in [RID_MSK, RID_MINSK_CITY]:
            for rearr in rearr_flags:
                is_rearr_enabled = len(rearr) > 0
                response = self.report.request_json(request.format(rids=rid, hyper_id=MODEL_ID) + rearr)
                expected_response = (
                    ru_response if rid == RID_MSK else bl_cpa_response if is_rearr_enabled else bl_non_cpa_response
                )
                self.assertFragmentIn(response, expected_response, allow_different_len=False)
                if rid == RID_MINSK_AREA:
                    self.assertFragmentIn(response, hide_reason_response)

    def test_cpa_supplier_region_restrictions(self):
        '''
        Проверяем работу ограничей мерчей по регионам
        Ограничения указываются в supplier_region_restrictions.fb
        При включенном rearr оффер магазина 9990 должен быть исключен
        '''
        request = 'place=productoffers&rids={rid}&hyperid={hyper_id}&debug=da&rearr-factors=cpa_enabled_countries=149'
        rearr_flags = [';market_cpa_supplier_region_restrictions_enabled={enabled}', '']
        rids = [RID_MSK, RID_MINSK_CITY]

        rus_response = {
            "search": {
                "totalOffers": 4,
                "results": [
                    {"entity": "offer", "wareId": "rus_restricted_offer_w"},
                    {"entity": "offer", "wareId": "russian_offer_dsbs___w"},
                    {"entity": "offer", "wareId": "russian_cpc_offer____w"},
                    {"entity": "offer", "wareId": "russian_offer________w"},
                ],
            }
        }
        bl_response = {
            "search": {
                "totalOffers": 4,
                "results": [
                    {"entity": "offer", "wareId": "belarusian_cpc_offer_w"},
                    {"entity": "offer", "wareId": "rus_restricted_offer_w"},
                    {"entity": "offer", "wareId": "belarusian_offer_____w"},
                    {"entity": "offer", "wareId": "russian_offer________w"},
                ],
            }
        }
        bl_restricted_response = {
            "search": {
                "totalOffers": 3,
                "results": [
                    {"entity": "offer", "wareId": "belarusian_cpc_offer_w"},
                    {"entity": "offer", "wareId": "belarusian_offer_____w"},
                    {"entity": "offer", "wareId": "russian_offer________w"},
                ],
            }
        }

        hide_reason_response = {
            "brief": {
                "filters": {
                    "CPA_DISABLED_IN_REGION_BY_SUPPLIER": 1,
                }
            }
        }

        for restriction_enabled in [0, 1]:
            for rid in rids:
                for rearr in rearr_flags:
                    current_rearr = rearr
                    if len(rearr) > 0:
                        current_rearr = current_rearr.format(enabled=restriction_enabled)
                    current_request = request.format(rid=rid, hyper_id=MODEL_ID) + current_rearr
                    response = self.report.request_json(current_request)
                    expected_response = (
                        rus_response
                        if rid == RID_MSK
                        else bl_restricted_response
                        if restriction_enabled or len(rearr) == 0
                        else bl_response
                    )
                    self.assertFragmentIn(response, expected_response)
                    if rid == RID_MINSK_CITY and (restriction_enabled or len(rearr) == 0):
                        self.assertFragmentIn(response, hide_reason_response)

    def test_multiple_currencies(self):
        '''
        Проверяем, что в поле price работает режим мультивалютной выдачи
        Основная валюта - валюта из локали запроса (поле currency, валюта региона или валюта региона из флага force-region)
        Доп.поле с ценой в рублях добавляется при включенном флаге use-multiple-currencies и основной валюте, отличной от российских рублей
        '''
        one_currency_response = {
            "search": {
                "results": [
                    {
                        "entity": "offer",
                        "prices": {"value": "34.34", "currency": "BYN", "paymentCurrencyPrice": Absent()},
                        "delivery": {
                            "options": [
                                {
                                    "price": {"currency": "BYN", "value": "3.4", "paymentCurrencyPrice": Absent()},
                                }
                            ]
                        },
                    }
                ]
            }
        }
        multi_currency_response = {
            "search": {
                "results": [
                    {
                        "entity": "offer",
                        "prices": {
                            "value": "34.34",
                            "currency": "BYN",
                            "paymentCurrencyPrice": {"value": "1000", "currency": "RUR"},
                        },
                        "delivery": {
                            "options": [
                                {
                                    "price": {
                                        "currency": "BYN",
                                        "value": "3.4",
                                        "paymentCurrencyPrice": {"currency": "RUR", "value": "99"},
                                    },
                                }
                            ]
                        },
                    }
                ]
            }
        }
        rur_response = {
            "search": {
                "results": [
                    {
                        "entity": "offer",
                        "prices": {"value": "1000", "currency": "RUR", "paymentCurrencyPrice": Absent()},
                        "delivery": {
                            "options": [
                                {
                                    "price": {"currency": "RUR", "value": "99", "paymentCurrencyPrice": Absent()},
                                }
                            ]
                        },
                    }
                ]
            }
        }
        test_data = []
        # check with currency cgi-parameter
        # проверяем задание валюты по региону
        test_data.append(
            (
                'place=offerinfo&offerid=russian_offer________w&rids={rids}&regset=2&rearr-factors=cpa_enabled_countries=149;',
                False,
            )
        )
        # проверяем задание валюты по региону
        test_data.append(
            (
                'place=offerinfo&offerid=russian_offer________w&rids={rids}&regset=2&rearr-factors=force-region=149;',
                False,
            )
        )
        # проверям задание валюты по параметру currency
        test_data.append(
            ('place=offerinfo&offerid=russian_offer________w&rids={rids}&regset=2&currency=BYN&rearr-factors=', False)
        )
        # проверяем, что мультивалютность не включается, когда основная валюта - российский рубль
        # проверка валюты по российскому региону
        test_data.append(('place=offerinfo&offerid=russian_offer________w&rids={rids}&regset=2&rearr-factors=', True))
        # проверка валюты по явному заданию в CGI
        test_data.append(
            (
                'place=offerinfo&offerid=russian_offer________w&rids={rids}&regset=2&currency=RUR&rearr-factors=cpa_enabled_countries=149;',
                True,
            )
        )
        for request, rur_expected in test_data:
            for multi_currency_enabled in [0, 1]:
                current_request = request + 'use-multiple-currencies={multi_currency}'
                rid = RID_MINSK_CITY if 'cpa_enabled_countries' in request else RID_MSK
                response = self.report.request_json(
                    current_request.format(rids=rid, multi_currency=multi_currency_enabled)
                )
                expected_response = (
                    rur_response
                    if rur_expected
                    else multi_currency_response
                    if multi_currency_enabled
                    else one_currency_response
                )
                self.assertFragmentIn(response, expected_response)
        all_offers_request = 'place=productoffers&hyperid={hyper_id}&rids={rid}&rearr-factors=cpa_enabled_countries=149;use-multiple-currencies={multiple_currencies_enabled}'
        for multi_currency_enabled in [0, 1]:
            current_request = all_offers_request.format(
                hyper_id=MODEL_ID, rid=RID_MINSK_CITY, multiple_currencies_enabled=multi_currency_enabled
            )
            response = self.report.request_json(current_request)
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {
                                "entity": "offer",
                                "wareId": "belarusian_offer_____w",
                                "prices": {
                                    "currency": "BYN",
                                    "value": "100",
                                    "paymentCurrencyPrice": {"currency": "RUR", "value": "2912"}
                                    if multi_currency_enabled
                                    else Absent(),
                                },
                                "delivery": {
                                    "options": [
                                        {
                                            "price": {
                                                "currency": "BYN",
                                                "value": "3.4",
                                                "paymentCurrencyPrice": {"currency": "RUR", "value": "99"}
                                                if multi_currency_enabled
                                                else Absent(),
                                            },
                                        }
                                    ]
                                },
                            },
                            {
                                "entity": "offer",
                                "wareId": "belarusian_cpc_offer_w",
                                "prices": {
                                    "currency": "BYN",
                                    "value": "100",
                                    "paymentCurrencyPrice": {"currency": "RUR", "value": "2912"}
                                    if multi_currency_enabled
                                    else Absent(),
                                },
                                "delivery": {
                                    "options": [
                                        {
                                            "price": {
                                                "currency": "BYN",
                                                "value": "3.43",
                                                "paymentCurrencyPrice": {"currency": "RUR", "value": "100"}
                                                if multi_currency_enabled
                                                else Absent(),
                                            },
                                        }
                                    ]
                                },
                            },
                            {
                                "entity": "offer",
                                "wareId": "russian_offer________w",
                                "prices": {
                                    "currency": "BYN",
                                    "value": "34.34",
                                    "paymentCurrencyPrice": {"currency": "RUR", "value": "1000"}
                                    if multi_currency_enabled
                                    else Absent(),
                                },
                                "delivery": {
                                    "options": [
                                        {
                                            "price": {
                                                "currency": "BYN",
                                                "value": "3.4",
                                                "paymentCurrencyPrice": {"currency": "RUR", "value": "99"}
                                                if multi_currency_enabled
                                                else Absent(),
                                            },
                                        }
                                    ]
                                },
                            },
                        ]
                    }
                },
            )


if __name__ == '__main__':
    main()
