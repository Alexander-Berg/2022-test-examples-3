#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BlueOffer,
    Currency,
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryOption,
    DeliveryServiceRegionToRegionInfo,
    DynamicDeliveryRestriction,
    DynamicDeliveryServiceInfo,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseDelivery,
    DynamicWarehouseInfo,
    DynamicWarehouseToWarehouseInfo,
    DynamicWarehouseLink,
    DynamicWarehousesPriorityInRegion,
    Elasticity,
    HyperCategory,
    HyperCategoryType,
    MarketSku,
    MinBidsCategory,
    MinBidsPriceGroup,
    MnPlace,
    Model,
    Offer,
    OfferDimensions,
    Region,
    RegionalDelivery,
    Shop,
    Tax,
    WarehouseToRegions,
    WarehouseWithPriority,
    WarehousesPriorityInRegion,
)
from core.testcase import TestCase, main


USE_DEPRECATED_DIRECT_SHIPPING_FLOW = "&rearr-factors=get_rid_of_direct_shipping=0"


def dict_to_rearr(rearr_flags):
    result = ""
    for key in rearr_flags.keys():
        result += str(key) + "=" + str(rearr_flags[key]) + ";"
    return result


NORTH_CAUCASIAN_FEDERAL_DISTRICT = 102444
NORTH_CAUCASIAN_FEDERAL_DISTRICT_CHILD = 10244411
SOURTHERN_FEDERAL_DISTRICT = 26


class T(TestCase):
    @classmethod
    def prepare(cls):
        """
        Тут тестируем репорт в области в вот этих флагов: https://st.yandex-team.ru/MARKETMONEY-415
        market_search_boost_regional_warehouses
        market_search_boost_regional_warehouses_in
        market_blue_buybox_shop_in_user_region_conversion_coef
        market_blue_buybox_no_warehouse_priority
        """
        cls.index.regiontree += [
            Region(
                rid=17,
                region_type=Region.FEDERAL_DISTRICT,
                children=[
                    Region(
                        rid=1,
                        region_type=Region.FEDERATIVE_SUBJECT,
                        children=[
                            Region(
                                rid=213,
                                tz_offset=10680,
                                children=[Region(rid=3, tz_offset=10800), Region(rid=4, tz_offset=10800)],
                            ),
                            Region(
                                rid=2,
                                tz_offset=10800,
                                children=[
                                    Region(rid=5, tz_offset=10800),
                                ],
                            ),
                        ],
                    )
                ],
            ),
            Region(
                rid=26,
                region_type=Region.FEDERAL_DISTRICT,
                children=[
                    Region(
                        rid=11029,
                        region_type=Region.FEDERATIVE_SUBJECT,
                        children=[
                            Region(rid=39, children=[Region(rid=123)]),
                        ],
                    ),
                ],
            ),
            Region(
                rid=NORTH_CAUCASIAN_FEDERAL_DISTRICT,
                region_type=Region.FEDERAL_DISTRICT,
                children=[
                    Region(
                        rid=11069,
                        region_type=Region.FEDERATIVE_SUBJECT,
                        children=[Region(rid=NORTH_CAUCASIAN_FEDERAL_DISTRICT_CHILD, tz_offset=10800)],
                    )
                ],
            ),
        ]

        cls.index.hypertree += [
            HyperCategory(hid=100, output_type=HyperCategoryType.GURU),
        ]

        cls.index.models += [
            Model(hyperid=100000, hid=100, title='Great model'),
        ]

        cls.index.shops += [
            Shop(
                fesh=1,
                datafeed_id=1,
                priority_region=213,
                regions=[225],
                name="3P-Магазин 145 склад",
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
            ),
            Shop(
                fesh=2,
                datafeed_id=2,
                priority_region=39,
                regions=[225],
                name="3P-Магазин 147 склад",
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                warehouse_id=147,
            ),
            Shop(fesh=3, priority_region=213, regions=[225], name="Белый магазин"),
            Shop(fesh=4, priority_region=39, regions=[225], name="Белый магазин"),
        ]

        cls.settings.lms_autogenerate = False
        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=145, home_region=213),
            DynamicWarehouseInfo(id=147, home_region=39),
            DynamicWarehouseToWarehouseInfo(warehouse_from=145, warehouse_to=145),
            DynamicWarehouseToWarehouseInfo(warehouse_from=147, warehouse_to=147),
            DynamicDeliveryServiceInfo(
                id=157,
                rating=2,
                region_to_region_info=[
                    DeliveryServiceRegionToRegionInfo(region_from=213, region_to=213, days_key=3),
                    DeliveryServiceRegionToRegionInfo(region_from=213, region_to=39, days_key=3),
                    DeliveryServiceRegionToRegionInfo(region_from=39, region_to=11069, days_key=3),
                ],
            ),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=145,
                delivery_service_id=157,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=23, region_to=225)],
            ),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=147,
                delivery_service_id=157,
                is_active=True,
                date_switch_time_infos=[
                    DateSwitchTimeAndRegionInfo(date_switch_hour=23, region_to=225),
                    DateSwitchTimeAndRegionInfo(date_switch_hour=23, region_to=11069),
                ],
            ),
            DynamicWarehousesPriorityInRegion(region=11069, warehouses=[147]),
            DynamicWarehousesPriorityInRegion(region=225, warehouses=[145, 147]),
        ]

        cls.index.warehouse_priorities += [
            # в Москве приоритет складов одинаков, и все офферы будут становиться buybox равновероятно
            WarehousesPriorityInRegion(
                regions=[213],
                warehouse_with_priority=[
                    WarehouseWithPriority(145, 0),
                    WarehouseWithPriority(147, 0),
                ],
            ),
            WarehousesPriorityInRegion(
                regions=[39],
                warehouse_with_priority=[
                    WarehouseWithPriority(145, 0),
                    WarehouseWithPriority(147, 0),
                ],
            ),
            WarehousesPriorityInRegion(
                regions=[11069],
                warehouse_with_priority=[
                    WarehouseWithPriority(145, 0),
                    WarehouseWithPriority(147, 0),
                ],
            ),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=1234,
                carriers=[157],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[
                    RegionalDelivery(
                        rid=225, options=[DeliveryOption(price=15, shop_delivery_price=15, day_from=1, day_to=2)]
                    ),
                    RegionalDelivery(
                        rid=213, options=[DeliveryOption(price=48, shop_delivery_price=48, day_from=1, day_to=2)]
                    ),
                    RegionalDelivery(
                        rid=39, options=[DeliveryOption(price=58, shop_delivery_price=58, day_from=1, day_to=2)]
                    ),
                    RegionalDelivery(
                        rid=11069, options=[DeliveryOption(price=58, shop_delivery_price=58, day_from=1, day_to=2)]
                    ),
                ],
            )
        ]

        cls.index.mskus += [
            MarketSku(
                title="Great msku",
                hyperid=100000,
                sku=100001,
                hid=100,
                delivery_buckets=[1234],
                buybox_elasticity=[
                    Elasticity(price_variant=100, demand_mean=300),
                    Elasticity(price_variant=200, demand_mean=10),
                    Elasticity(price_variant=300, demand_mean=5),
                ],
                blue_offers=[
                    BlueOffer(price=100, feedid=1, waremd5="BLUE-100001-FEED-0001Q"),
                    BlueOffer(price=104, feedid=2, waremd5="BLUE-100001-FEED-0002Q"),
                ],
                weight=5,
                blue_weight=5,
                dimensions=OfferDimensions(
                    length=20,
                    width=30,
                    height=10,
                ),
                blue_dimensions=OfferDimensions(
                    length=20,
                    width=30,
                    height=10,
                ),
            ),
        ]

        cls.index.offers += [
            Offer(title='Sith lightsaber', hid=100, hyperid=100000, price=100, bid=10, ts=1),
            Offer(title='Darksaber', hid=100, hyperid=100000, price=102, bid=10, ts=2),
        ]

        cls.index.min_bids_category_stats += [
            MinBidsCategory(
                category_id=100,
                geo_group_id=0,
                price_group_id=100,
                drr=0.1,
                search_conversion=0.01,
                card_conversion=0.01,
                full_card_conversion=1.0,
            ),
        ]

        cls.index.min_bids_price_groups += [
            MinBidsPriceGroup(0),
            MinBidsPriceGroup(100),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1).respond(0.05)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2).respond(0.05)

        cls.index.warehouse_to_regions += [
            WarehouseToRegions(region_id=39, warehouse_id=147),
        ]

    @classmethod
    def prepare_nordstream(cls):
        cls.settings.nordstream_autogenerate = False
        for warehouse_id in (145, 147, 300, 301, 302, 303):
            cls.dynamic.nordstream += [DynamicWarehouseLink(warehouse_id, [warehouse_id])]
            cls.dynamic.nordstream += [
                DynamicWarehouseDelivery(
                    warehouse_id,
                    {
                        225: [
                            DynamicDeliveryRestriction(min_days=1, max_days=2, cost=15),
                        ],
                        213: [
                            DynamicDeliveryRestriction(min_days=1, max_days=2, cost=48),
                        ],
                        39: [
                            DynamicDeliveryRestriction(min_days=1, max_days=2, cost=58),
                        ],
                        11069: [
                            DynamicDeliveryRestriction(min_days=1, max_days=2, cost=58),
                        ],
                        123: [
                            DynamicDeliveryRestriction(min_days=1, max_days=2, cost=58),
                        ],
                    },
                ),
            ]

    def test_locality_boosting_in_do_no_flag(self):
        """
        Проверяем, что никто не бустится при выключенных флагах
        """
        hyper_id = 100000
        rearr_flags_dict = {
            "market_search_boost_regional_warehouses": 0,
            "use_offer_type_priority_as_main_factor_in_do": 1,
            "market_uncollapse_supplier": 1,
            "market_blue_buybox_no_warehouse_priority": 1,
            "market_blue_buybox_gvm_ue_rand_delta": 0,
            "market_blue_buybox_fbs_hybrid_local_warehouse_priority": 0,
        }

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)

        # проверяем что победил московский оффер
        response = self.report.request_json(
            "place=productoffers&hyperid=%s&offers-set=default&rids=39&debug=da&rearr-factors=%s"
            % (hyper_id, rearr_flags_str)
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "wareId": "BLUE-100001-FEED-0001Q",
                'debug': {
                    'buyboxDebug': {
                        'Offers': [
                            {'WareMd5': 'BLUE-100001-FEED-0002Q', 'ShopInUserRegionConversionCoef': 1},
                            {'WareMd5': 'BLUE-100001-FEED-0001Q', 'ShopInUserRegionConversionCoef': 1},
                        ],
                    }
                },
            },
        )

    def test_locality_boosting_in_do_with_flag(self):
        """
        Проверяем, что в регионе 39 забустился Ростовский оффер
        """
        hyper_id = 100000
        rearr_flags_dict = {
            "use_offer_type_priority_as_main_factor_in_do": 1,
            "market_uncollapse_supplier": 1,
            "market_search_boost_regional_warehouses": 4,
            "market_search_boost_regional_warehouses_in": 39,
            "market_blue_buybox_shop_in_user_region_conversion_coef": 1.1,
            "market_blue_buybox_no_warehouse_priority": 1,
            "market_load_boost_locality_from_fb_file": 0,
        }

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            "place=productoffers&hyperid=%s&rids=39&offers-set=default&debug=da&rearr-factors=%s"
            % (hyper_id, rearr_flags_str)
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "wareId": "BLUE-100001-FEED-0002Q",
                'debug': {
                    'buyboxDebug': {
                        'Offers': [
                            {'WareMd5': 'BLUE-100001-FEED-0002Q', 'ShopInUserRegionConversionCoef': 1.1},
                            {'WareMd5': 'BLUE-100001-FEED-0001Q', 'ShopInUserRegionConversionCoef': 1},
                        ]
                    }
                },
            },
        )

    def test_locality_boosting_do_not_boost_in_regions_out_of_list(self):
        """
        Проверяем, что бустятся только те регионы, которые попадают в список. В списке нет Мск,
        смотрим что бы мск оффер не забустился для пользователя из Мск
        """
        hyper_id = 100000
        rearr_flags_dict = {
            "use_offer_type_priority_as_main_factor_in_do": 1,
            "market_uncollapse_supplier": 1,
            "market_search_boost_regional_warehouses": 4,
            "market_search_boost_regional_warehouses_in": 39,
            "market_blue_buybox_shop_in_user_region_conversion_coef": 1.1,
            "market_blue_buybox_no_warehouse_priority": 1,
        }

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)

        response = self.report.request_json(
            "place=productoffers&hyperid=%s&rids=213&offers-set=default&debug=da&rearr-factors=%s"
            % (hyper_id, rearr_flags_str)
        )
        self.assertFragmentIn(response, {"ShopInUserRegionConversionCoef": 1})

    @classmethod
    def prepare_hard_priorities(cls):
        cls.index.warehouse_priorities = [
            WarehousesPriorityInRegion(
                regions=[213],
                warehouse_with_priority=[
                    WarehouseWithPriority(145, 100),
                    WarehouseWithPriority(147, 100500),
                ],
            ),
            WarehousesPriorityInRegion(
                regions=[39],
                warehouse_with_priority=[
                    WarehouseWithPriority(145, 100),
                    WarehouseWithPriority(147, 0),
                ],
            ),
            WarehousesPriorityInRegion(
                regions=[11069],
                warehouse_with_priority=[WarehouseWithPriority(145, 100), WarehouseWithPriority(147, 100)],
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                title="Great msku",
                hyperid=100001,
                sku=100002,
                hid=100,
                delivery_buckets=[1234],
                buybox_elasticity=[
                    Elasticity(price_variant=100, demand_mean=300),
                    Elasticity(price_variant=200, demand_mean=10),
                    Elasticity(price_variant=300, demand_mean=5),
                ],
                blue_offers=[
                    BlueOffer(price=104, feedid=2, waremd5="BLUE-100002-FEED-0002Q"),
                    BlueOffer(price=100, feedid=1, waremd5="BLUE-100002-FEED-0001Q"),
                ],
                weight=5,
                blue_weight=5,
                dimensions=OfferDimensions(
                    length=20,
                    width=30,
                    height=10,
                ),
                blue_dimensions=OfferDimensions(
                    length=20,
                    width=30,
                    height=10,
                ),
            )
        ]

        cls.index.models += [
            Model(hyperid=100001, hid=100, title='Great model 2'),
        ]

        cls.index.warehouse_to_regions += [
            WarehouseToRegions(region_id=11069, warehouse_id=147),
        ]

    def test_locality_no_warehouse_priority_in_rostov_no_flag(self):
        """
        Тестируем, что без флага market_blue_buybox_no_warehouse_priority работает жесткий приоритет для Ростова. Москвы осекается, Ростов 0.
        """
        hyper_id = 100000
        rearr_flags_dict = {
            "use_offer_type_priority_as_main_factor_in_do": 1,
            "market_uncollapse_supplier": 1,
            "market_search_boost_regional_warehouses": 0,
            "market_search_boost_regional_warehouses_in": 39,
            "market_blue_buybox_shop_in_user_region_conversion_coef": 1.1,
            "market_blue_buybox_no_warehouse_priority": 0,
        }

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            "place=productoffers&hyperid=%s&rids=39&offers-set=default&debug=da&rearr-factors=%s"
            % (hyper_id, rearr_flags_str)
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                'debug': {
                    'buyboxDebug': {
                        'Offers': [{'WareMd5': "BLUE-100001-FEED-0002Q", 'WarehouseId': 147, 'WarehousePriority': 0}],
                        'RejectedOffers': [
                            {'RejectReason': 'LESS_PRIORITY_WAREHOUSE', 'Offer': {'WareMd5': 'BLUE-100001-FEED-0001Q'}}
                        ],
                    }
                },
            },
        )

        # проверяем что работает на офферах в другом порядке
        hyper_id = 100001
        rearr_flags_dict = {
            "use_offer_type_priority_as_main_factor_in_do": 1,
            "market_uncollapse_supplier": 1,
            "market_search_boost_regional_warehouses": 0,
            "market_search_boost_regional_warehouses_in": 39,
            "market_blue_buybox_shop_in_user_region_conversion_coef": 1.1,
            "market_blue_buybox_no_warehouse_priority": 0,
        }

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            "place=productoffers&hyperid=%s&rids=39&offers-set=default&debug=da&rearr-factors=%s"
            % (hyper_id, rearr_flags_str)
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                'debug': {
                    'buyboxDebug': {
                        'Offers': [{'WareMd5': "BLUE-100002-FEED-0002Q", 'WarehouseId': 147, 'WarehousePriority': 0}],
                        'RejectedOffers': [
                            {'RejectReason': 'LESS_PRIORITY_WAREHOUSE', 'Offer': {'WareMd5': 'BLUE-100002-FEED-0001Q'}}
                        ],
                    }
                },
            },
        )

        # Смотрим, что в Москве приоритет у Ростова ниже, чем у московского склада
        hyper_id = 100000
        rearr_flags_dict = {
            "use_offer_type_priority_as_main_factor_in_do": 1,
            "market_uncollapse_supplier": 1,
            "market_search_boost_regional_warehouses": 0,
            "market_search_boost_regional_warehouses_in": 213,
            "market_blue_buybox_no_warehouse_priority": 1,
        }

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            "place=productoffers&hyperid=%s&rids=213&offers-set=default&debug=da&rearr-factors=%s"
            % (hyper_id, rearr_flags_str)
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                'debug': {
                    'buyboxDebug': {
                        'Offers': [{'WareMd5': "BLUE-100001-FEED-0001Q", 'WarehouseId': 145, 'WarehousePriority': 100}],
                        'RejectedOffers': [
                            {'RejectReason': 'LESS_PRIORITY_WAREHOUSE', 'Offer': {'WareMd5': 'BLUE-100001-FEED-0002Q'}}
                        ],
                    }
                },
            },
        )

    def test_locality_no_warehouse_priority_in_rostov_with_flag(self):
        """
        Тестируем, что флаг market_blue_buybox_no_warehouse_priority вырубает жесткий приоритет для Ростова.
        """
        hyper_id = 100000
        rearr_flags_dict = {
            "use_offer_type_priority_as_main_factor_in_do": 1,
            "market_uncollapse_supplier": 1,
            "market_search_boost_regional_warehouses": 0,
            "market_search_boost_regional_warehouses_in": 39,
            "market_blue_buybox_no_warehouse_priority": 1,
            "market_blue_buybox_fbs_hybrid_local_warehouse_priority": 0,
        }

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            "place=productoffers&hyperid=%s&rids=39&offers-set=default&debug=da&rearr-factors=%s"
            % (hyper_id, rearr_flags_str)
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                'debug': {
                    'buyboxDebug': {
                        'Offers': [
                            {'WareMd5': "BLUE-100001-FEED-0001Q", 'WarehouseId': 145, 'WarehousePriority': 100},
                            {'WareMd5': "BLUE-100001-FEED-0002Q", 'WarehouseId': 147, 'WarehousePriority': 100},
                        ],
                        'WonMethod': "WON_BY_EXCHANGE",
                    }
                },
            },
        )

        hyper_id = 100001
        rearr_flags_dict = {
            "use_offer_type_priority_as_main_factor_in_do": 1,
            "market_uncollapse_supplier": 1,
            "market_search_boost_regional_warehouses": 0,
            "market_search_boost_regional_warehouses_in": 39,
            "market_blue_buybox_no_warehouse_priority": 1,
            "market_blue_buybox_fbs_hybrid_local_warehouse_priority": 0,
        }

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            "place=productoffers&hyperid=%s&rids=39&offers-set=default&debug=da&rearr-factors=%s"
            % (hyper_id, rearr_flags_str)
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                'debug': {
                    'buyboxDebug': {
                        'Offers': [
                            {'WareMd5': "BLUE-100002-FEED-0001Q", 'WarehouseId': 145, 'WarehousePriority': 100},
                            {'WareMd5': "BLUE-100002-FEED-0002Q", 'WarehouseId': 147, 'WarehousePriority': 100},
                        ],
                        'WonMethod': "WON_BY_EXCHANGE",
                    }
                },
            },
        )

        # Смотрим, что в Москве приоритет у Ростова ниже, чем у московского склада
        hyper_id = 100000
        rearr_flags_dict = {
            "use_offer_type_priority_as_main_factor_in_do": 1,
            "market_uncollapse_supplier": 1,
            "market_search_boost_regional_warehouses": 0,
            "market_search_boost_regional_warehouses_in": 213,
            "market_blue_buybox_no_warehouse_priority": 1,
        }

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            "place=productoffers&hyperid=%s&rids=213&offers-set=default&debug=da&rearr-factors=%s"
            % (hyper_id, rearr_flags_str)
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                'debug': {
                    'buyboxDebug': {
                        'Offers': [{'WareMd5': "BLUE-100001-FEED-0001Q", 'WarehouseId': 145, 'WarehousePriority': 100}],
                        'RejectedOffers': [
                            {'RejectReason': 'LESS_PRIORITY_WAREHOUSE', 'Offer': {'WareMd5': 'BLUE-100001-FEED-0002Q'}}
                        ],
                    }
                },
            },
        )

    def test_locality_hacked_priority(self):
        """
        Тестим, что для СКФО заодно бустятся склады из ЮФО.
        """
        hyper_id = 100000
        rearr_flags_dict = {
            "use_offer_type_priority_as_main_factor_in_do": 1,
            "market_uncollapse_supplier": 1,
            "market_search_boost_regional_warehouses": 2,
            "market_search_boost_regional_warehouses_in": "39,102444",
            "market_blue_buybox_no_warehouse_priority": 1,
            "market_blue_buybox_shop_in_user_region_conversion_coef": 1.1,
            "market_load_boost_locality_from_fb_file": 0,
        }

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            "place=productoffers&hyperid=%s&rids=11069&offers-set=default&debug=da&rearr-factors=%s"
            % (hyper_id, rearr_flags_str)
        )
        self.assertFragmentIn(
            response,
            {
                'debug': {
                    'buyboxDebug': {
                        'Offers': [
                            {'WareMd5': 'BLUE-100001-FEED-0002Q', 'ShopInUserRegionConversionCoef': 1.1},
                            {'WareMd5': 'BLUE-100001-FEED-0001Q', 'ShopInUserRegionConversionCoef': 1},
                        ]
                    }
                }
            },
        )

    @classmethod
    def prepare_dinamic_priority(cls):

        cls.index.shops += [
            Shop(
                fesh=5,
                datafeed_id=5,
                priority_region=213,
                regions=[225],
                name="1P-Магазин 300 склад",
                supplier_type=Shop.FIRST_PARTY,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
                warehouse_id=300,
                fulfillment_program=True,
                direct_shipping=True,
            ),
            Shop(
                fesh=6,
                datafeed_id=6,
                priority_region=123,
                regions=[225],
                name="3P-Магазин 301 склад",
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                warehouse_id=301,
                fulfillment_program=True,
                direct_shipping=True,
            ),
            Shop(
                fesh=7,
                datafeed_id=7,
                priority_region=123,
                regions=[225],
                name="3P-Магазин 302 неприоритетный склад",
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                warehouse_id=302,
                fulfillment_program=True,
                direct_shipping=True,
            ),
            Shop(
                fesh=8,
                datafeed_id=8,
                priority_region=123,
                regions=[225],
                name='Crossdock-Магазин 303 склад',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                warehouse_id=303,
                direct_shipping=False,
                fulfillment_program=True,
            ),
        ]

        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=300, home_region=123),
            DynamicWarehouseInfo(id=301, home_region=123),
            DynamicWarehouseInfo(id=302, home_region=123),
            DynamicWarehouseInfo(id=303, home_region=123),
            DynamicWarehouseInfo(id=304, home_region=123),
        ]

        cls.index.models += [
            Model(hyperid=100002, hid=100, title='Other msku'),
        ]

        cls.index.warehouse_to_regions += [
            WarehouseToRegions(region_id=39, warehouse_id=300),
            WarehouseToRegions(region_id=11029, warehouse_id=301),
            WarehouseToRegions(region_id=123, warehouse_id=303),
        ]

        cls.index.mskus += [
            MarketSku(
                title="Other msku",
                hyperid=100002,
                sku=100003,
                hid=100,
                delivery_buckets=[1234],
                buybox_elasticity=[
                    Elasticity(price_variant=100, demand_mean=300),
                    Elasticity(price_variant=200, demand_mean=10),
                    Elasticity(price_variant=300, demand_mean=5),
                ],
                blue_offers=[
                    BlueOffer(price=100, feedid=5, waremd5="BLUE-100002-FEED-0005Q"),
                    BlueOffer(price=100, feedid=6, waremd5="BLUE-100002-FEED-0006Q"),
                    BlueOffer(price=100, feedid=7, waremd5="BLUE-100002-FEED-0007Q"),
                    BlueOffer(price=100, feedid=8, waremd5="BLUE-100002-FEED-0008Q"),
                ],
                weight=5,
                blue_weight=5,
                dimensions=OfferDimensions(
                    length=20,
                    width=30,
                    height=10,
                ),
                blue_dimensions=OfferDimensions(
                    length=20,
                    width=30,
                    height=10,
                ),
            )
        ]

    def test_locality_dinamic_priority(self):
        """ """
        hyper_id = 100002
        rearr_flags_dict = {
            "market_blue_buybox_shop_in_user_region_conversion_coef": 1.1,
            "market_enable_boost_locality_from_dynamic": 1,
            "market_load_boost_locality_from_fb_file": 0,
        }

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            "place=productoffers&hyperid=%s&rids=123&offers-set=default&debug=da&rearr-factors=%s"
            % (hyper_id, rearr_flags_str)
            + USE_DEPRECATED_DIRECT_SHIPPING_FLOW
        )
        self.assertFragmentIn(
            response,
            {
                'debug': {
                    'buyboxDebug': {
                        'Offers': [
                            {'WareMd5': 'BLUE-100002-FEED-0005Q', 'ShopInUserRegionConversionCoef': 1.1},  # 1P
                            {'WareMd5': 'BLUE-100002-FEED-0006Q', 'ShopInUserRegionConversionCoef': 1.1},  # 3P_FF
                            {'WareMd5': 'BLUE-100002-FEED-0008Q', 'ShopInUserRegionConversionCoef': 1.1},  # Crossdock
                            {
                                'WareMd5': 'BLUE-100002-FEED-0007Q',
                                'ShopInUserRegionConversionCoef': 1,
                            },  # 3P_FF 302 склад не бустится
                        ],
                    }
                }
            },
        )


if __name__ == '__main__':
    main()
