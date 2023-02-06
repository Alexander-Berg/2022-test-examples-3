#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import (
    BlueOffer,
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryOption,
    DynamicDeliveryServiceInfo,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseInfo,
    DynamicWarehousesPriorityInRegion,
    Elasticity,
    MarketSku,
    Offer,
    Region,
    RegionalDelivery,
    Shop,
    Model,
    UngroupedModel,
    MnPlace,
    WarehouseWithPriority,
    WarehousesPriorityInRegion,
)
from core.matcher import Contains, EmptyList, Absent, Greater, Capture, NotEmpty
from core.types.recommended_fee import RecommendedFee
from core.types.reserveprice_fee import ReservePriceFee


def dict_to_rearr(rearr_flags):
    result = ""
    for key in rearr_flags.keys():
        result += str(key) + "=" + str(rearr_flags[key]) + ";"
    return result


class T(TestCase):
    @classmethod
    def prepare(cls):

        cls.index.regiontree += [
            Region(rid=213, name='Москва', region_type=Region.FEDERATIVE_SUBJECT),
        ]

        cls.settings.lms_autogenerate = False
        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=1, home_region=213),
            DynamicWarehouseInfo(id=2, home_region=213),
            DynamicWarehouseInfo(id=3, home_region=213),
            DynamicWarehouseInfo(id=4, home_region=213),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=1,
                delivery_service_id=157,
                date_switch_time_infos=[
                    DateSwitchTimeAndRegionInfo(date_switch_hour=23, region_to=213),
                ],
            ),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=2,
                delivery_service_id=157,
                date_switch_time_infos=[
                    DateSwitchTimeAndRegionInfo(date_switch_hour=23, region_to=213),
                ],
            ),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=3,
                delivery_service_id=157,
                date_switch_time_infos=[
                    DateSwitchTimeAndRegionInfo(date_switch_hour=23, region_to=213),
                ],
            ),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=4,
                delivery_service_id=157,
                date_switch_time_infos=[
                    DateSwitchTimeAndRegionInfo(date_switch_hour=23, region_to=213),
                ],
            ),
            DynamicDeliveryServiceInfo(id=157, rating=2),
            DynamicWarehousesPriorityInRegion(region=213, warehouses=[1, 2, 3, 4]),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=1234,
                carriers=[157],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[
                    RegionalDelivery(
                        rid=213, options=[DeliveryOption(price=15, shop_delivery_price=15, day_from=1, day_to=2)]
                    ),
                ],
            ),
            DeliveryBucket(
                bucket_id=5678,
                carriers=[157],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
                regional_options=[
                    RegionalDelivery(
                        rid=213, options=[DeliveryOption(price=15, shop_delivery_price=15, day_from=1, day_to=6)]
                    ),
                ],
            ),
        ]

        cls.index.warehouse_priorities += [
            WarehousesPriorityInRegion(
                regions=[213],
                warehouse_with_priority=[
                    WarehouseWithPriority(1, 0),
                    WarehouseWithPriority(2, 1),
                    WarehouseWithPriority(3, 1),
                    WarehouseWithPriority(4, 0),
                ],
            )
        ]

        cls.index.shops += [
            Shop(
                fesh=3100 + i,
                datafeed_id=3100 + i,
                priority_region=213,
                regions=[213],
                supplier_type=Shop.THIRD_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                fulfillment_program=True,
                warehouse_id=i % 4 + 1,
            )
            for i in range(1, 11)
        ]

        cls.index.models += [
            Model(
                hyperid=94301,
                hid=91013,
                title="Исходная модель 1",
                ungrouped_blue=[
                    UngroupedModel(
                        group_id=1,
                        title="Расхлопнутая модель 1.1",
                        key='94301_1',
                    ),
                    UngroupedModel(
                        group_id=2,
                        title="Расхлопнутая модель 1.2",
                        key='94301_2',
                    ),
                    UngroupedModel(
                        group_id=3,
                        title="Расхлопнутая модель 1.3",
                        key='94301_3',
                    ),
                    UngroupedModel(
                        group_id=4,
                        title="Расхлопнутая модель 1.4",
                        key='94301_4',
                    ),
                    UngroupedModel(
                        group_id=5,
                        title="Расхлопнутая модель 1.5",
                        key='94301_5',
                    ),
                    UngroupedModel(
                        group_id=6,
                        title="Расхлопнутая модель 1.6",
                        key='94301_6',
                    ),
                ],
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                hyperid=94301,
                delivery_buckets=[1234],
                sku=1,
                buybox_elasticity=[
                    Elasticity(price_variant=2100, demand_mean=200),
                    Elasticity(price_variant=2200, demand_mean=160),
                    Elasticity(price_variant=2500, demand_mean=30),
                ],
                blue_offers=[
                    BlueOffer(
                        ts=7501,
                        price=2100,
                        feedid=3101,
                        waremd5='OFF1_2100_SKU1_SUP01_Q',
                    ),
                    BlueOffer(
                        ts=7502,
                        price=2150,
                        fee=100,
                        feedid=3102,
                        waremd5='OFF2_2150_SKU1_SUP02_Q',
                    ),
                ],
                ungrouped_model_blue=1,
            ),
            MarketSku(
                hyperid=94301,
                delivery_buckets=[1234],
                sku=2,
                buybox_elasticity=[
                    Elasticity(price_variant=2100, demand_mean=200),
                    Elasticity(price_variant=2200, demand_mean=160),
                    Elasticity(price_variant=2500, demand_mean=30),
                ],
                blue_offers=[
                    BlueOffer(
                        ts=7503,
                        price=2100,
                        fee=50,
                        feedid=3103,
                        waremd5='OFF3_2100_SKU2_SUP03_Q',
                    ),
                    BlueOffer(
                        ts=7504,
                        price=2150,
                        fee=100,
                        feedid=3104,
                        waremd5='OFF4_2150_SKU2_SUP04_Q',
                    ),
                ],
                ungrouped_model_blue=2,
            ),
            MarketSku(
                hyperid=94301,
                delivery_buckets=[1234],
                sku=3,
                buybox_elasticity=[
                    Elasticity(price_variant=2100, demand_mean=200),
                    Elasticity(price_variant=2200, demand_mean=190),
                    Elasticity(price_variant=2500, demand_mean=150),
                ],
                blue_offers=[
                    BlueOffer(
                        ts=7505,
                        price=2100,
                        fee=100,
                        feedid=3105,
                        waremd5='OFF5_2100_SKU3_SUP05_Q',
                    ),
                    BlueOffer(
                        ts=7506,
                        price=2300,
                        fee=500,
                        feedid=3106,
                        waremd5='OFF6_2150_SKU3_SUP06_Q',
                    ),
                ],
                ungrouped_model_blue=3,
            ),
            MarketSku(
                hyperid=94301,
                delivery_buckets=[1234],
                sku=4,
                buybox_elasticity=[
                    Elasticity(price_variant=2100, demand_mean=200),
                    Elasticity(price_variant=2200, demand_mean=160),
                    Elasticity(price_variant=2500, demand_mean=30),
                ],
                blue_offers=[
                    BlueOffer(
                        ts=7507,
                        price=2100,
                        feedid=3107,
                        waremd5='OFF7_2100_SKU4_SUP07_Q',
                    ),
                ],
                ungrouped_model_blue=4,
            ),
            MarketSku(
                hyperid=94301,
                delivery_buckets=[1234],
                sku=5,
                buybox_elasticity=[
                    Elasticity(price_variant=2100, demand_mean=200),
                    Elasticity(price_variant=2200, demand_mean=160),
                    Elasticity(price_variant=2500, demand_mean=30),
                ],
                blue_offers=[
                    BlueOffer(
                        ts=7508,
                        price=2100,
                        feedid=3108,
                        waremd5='OFF8_2100_SKU5_SUP08_Q',
                    ),
                ],
                ungrouped_model_blue=5,
            ),
            MarketSku(
                hyperid=94301,
                delivery_buckets=[1234],
                sku=6,
                buybox_elasticity=[
                    Elasticity(price_variant=2100, demand_mean=200),
                    Elasticity(price_variant=2200, demand_mean=160),
                    Elasticity(price_variant=2500, demand_mean=30),
                ],
                blue_offers=[
                    BlueOffer(
                        ts=7509,
                        price=2100,
                        feedid=3109,
                        waremd5='OFF9_2100_SKU6_SUP09_Q',
                    ),
                ],
                ungrouped_model_blue=6,
            ),
        ]

        for place in [MnPlace.BASE_SEARCH, MnPlace.META_REARRANGE]:
            cls.matrixnet.on_place(place, 7501).respond(0.9)
            cls.matrixnet.on_place(place, 7502).respond(0.9)
            cls.matrixnet.on_place(place, 7503).respond(0.903)
            cls.matrixnet.on_place(place, 7504).respond(0.903)
            cls.matrixnet.on_place(place, 7505).respond(0.904)
            cls.matrixnet.on_place(place, 7506).respond(0.904)
            cls.matrixnet.on_place(place, 7507).respond(0.85)
            cls.matrixnet.on_place(place, 7508).respond(0.84)
            cls.matrixnet.on_place(place, 7509).respond(0.79)

    def test_sponsorred_offers_pattern_desktop_list(self):
        # Проверяем что при запросе поиска с трафаретами и флагом market_buybox_auction_search_sponsored_places_web
        # для каждой скю могут появиться дубли потому что сейчас один и тот же оффер может быть как в поисковом списке
        # так и в трафарете с пометкой спонсорский товар.
        # Так же мы стали запрашивать из байбокса два оффера: первый как и раньше он используется в поисковом списке
        # , а второй победитель аукциона в байбоксе (предполагается что это оффер с большей ставкой) используется в рекламе
        # Если из байбокса пришел один оффер для скю и у него есть ставка, то мы используем его и в поиске и в рекламе
        # см https://st.yandex-team.ru/MADV-789

        vendor_conversion = 0.075
        sigmoid_alpha = 0.6
        sigmoid_beta = 0.0015
        sigmoid_gamma = 1

        rearr_flags_dict = {
            "market_report_mimicry_in_serp_pattern": 2,
            "market_buybox_auction_search_sponsored_places_web": 1,
            "market_buybox_auction_coef_b_multiplicative_bid_coef_search_sponsored": 0.001,
            "market_buybox_auction_rand_low": 1.00,
            "market_buybox_auction_rand_delta": 0.00,
            "market_buybox_auction_coef_w_rp_fee_coef_search_sponsored": 0.0,
            "market_buybox_auction_transfer_fee_to_search": 1,
            "market_buybox_enable_advert_buybox": 0,
            "market_tweak_search_auction_white_cpa_fee_no_text_params_desktop": "{},{},{}".format(
                sigmoid_alpha, sigmoid_beta, sigmoid_gamma
            ),
            "market_money_vendor_cpc_to_cpa_conversion_buybox": vendor_conversion,
            "market_white_search_auction_cpa_fee": 1,
            "market_white_search_auction_cpa_fee_transfer_fee_do_desktop": 1,
            "market_buybox_extra_auction_price_rel_max_threshold": 1.05,  # Возвращаем старый порог, под который написан тест
            "market_buybox_auction_search_sponsored_places_allow_duplicates": 1,
        }

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        request = (
            'pp=7&hid=91013&place=prime&rgb=green_with_blue&rids=213'
            '&show-urls=external,decrypted,direct%2Ccpa&bsformat=2&&viewtype=list'
            '&use-default-offers=1&allow-collapsing=1&allow-ungrouping=1&waitall=da&numdoc=48&debug=1'
            '&rearr-factors={}'.format(rearr_flags_str)
        )

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 9,
                    'results': [
                        {
                            'type': "model",
                            'id': 94301,
                            'offers': {
                                'items': [
                                    {
                                        # для скю=3 поисковый оффер совпадает с рекламным потому что в байбоксе участвует только один оффер
                                        'wareId': "OFF5_2100_SKU3_SUP05_Q",
                                        'marketSku': "3",
                                        'urls': {
                                            'cpa': Contains('/shop_fee=100/', '/shop_fee_ab=0/'),
                                        },
                                        'feeShowPlain': Contains('pp: 7'),  # органический pp для органики
                                    },
                                ]
                            },
                        },
                        {
                            'type': "model",
                            'id': 94301,
                            'sponsored': True,
                            'offers': {
                                'items': [
                                    {
                                        'wareId': "OFF5_2100_SKU3_SUP05_Q",
                                        'sponsored': True,
                                        'marketSku': "3",
                                        'urls': {
                                            # амнистируем об следующий рекламный оффер, а это OFF3_2100_SKU2_SUP03_Q
                                            'cpa': Contains('/shop_fee=100/', '/shop_fee_ab=27/', '/pp=231/'),
                                        },
                                        'feeShowPlain': Contains('pp: 231'),  # трафаретный pp для трафаретов
                                    },
                                ]
                            },
                        },
                        {
                            'type': "model",
                            'id': 94301,
                            'sponsored': True,
                            'offers': {
                                'items': [
                                    {
                                        # для скю=2 поисковый оффер совпадает с рекламным потому что победитель по GMV равен победителю по аукциону
                                        'wareId': "OFF3_2100_SKU2_SUP03_Q",
                                        'sponsored': True,
                                        'marketSku': "2",
                                        'urls': {
                                            # амнистируем об следующий рекламный оффер, а это OFF2_2150_SKU1_SUP02_Q
                                            'cpa': Contains('/shop_fee=50/', '/shop_fee_ab=32/', '/pp=231/'),
                                        },
                                        'feeShowPlain': Contains('pp: 231'),
                                        'debug': {
                                            'buyboxDebug': {
                                                'WonMethod': 'EXCHANGE_AND_AUCTION',
                                            }
                                        },
                                    },
                                ]
                            },
                        },
                        {
                            'type': "model",
                            'id': 94301,
                            'offers': {
                                'items': [
                                    {
                                        'wareId': "OFF3_2100_SKU2_SUP03_Q",
                                        'marketSku': "2",
                                        'urls': {
                                            'cpa': Contains('/shop_fee=50/', '/shop_fee_ab=0/'),
                                        },
                                        'debug': {
                                            'buyboxDebug': {
                                                'WonMethod': 'EXCHANGE_AND_AUCTION',
                                            }
                                        },
                                    },
                                ]
                            },
                        },
                        {
                            'type': "model",
                            'id': 94301,
                            'offers': {
                                'items': [
                                    {
                                        # для скю=1 это лучший оффер по GMV используется только в поиске
                                        'wareId': "OFF1_2100_SKU1_SUP01_Q",
                                        'marketSku': "1",
                                        'urls': {
                                            'cpa': Contains('/shop_fee=0/', '/shop_fee_ab=0/'),
                                        },
                                    },
                                ]
                            },
                        },
                        {
                            'type': "model",
                            'id': 94301,
                            'sponsored': True,
                            'offers': {
                                'items': [
                                    {
                                        # для скю=1 это победитель аукциона в байбоксе используется только в рекламе (sponsored)
                                        'wareId': "OFF2_2150_SKU1_SUP02_Q",
                                        'sponsored': True,
                                        'marketSku': "1",
                                        'urls': {
                                            # амнистировать не об кого, но ставка shop_fee_ab=86 берется из аукциона в байбоксе
                                            'cpa': Contains('/shop_fee=100/', '/shop_fee_ab=86/', '/pp=231/'),
                                        },
                                    },
                                ]
                            },
                        },
                        {
                            'type': "model",
                            'id': 94301,
                            'offers': {
                                'items': [
                                    {
                                        'wareId': "OFF7_2100_SKU4_SUP07_Q",
                                        'urls': {
                                            'cpa': Contains('/shop_fee=0/', '/shop_fee_ab=0/'),
                                        },
                                    },
                                ]
                            },
                        },
                        {
                            'type': "model",
                            'id': 94301,
                            'offers': {
                                'items': [
                                    {
                                        'wareId': "OFF8_2100_SKU5_SUP08_Q",
                                        'urls': {
                                            'cpa': Contains('/shop_fee=0/', '/shop_fee_ab=0/'),
                                        },
                                    },
                                ]
                            },
                        },
                        {
                            'type': "model",
                            'id': 94301,
                            'offers': {
                                'items': [
                                    {
                                        'wareId': "OFF9_2100_SKU6_SUP09_Q",
                                        'urls': {
                                            'cpa': Contains('/shop_fee=0/', '/shop_fee_ab=0/'),
                                        },
                                    },
                                ]
                            },
                        },
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        self.show_log.expect(ware_md5="OFF1_2100_SKU1_SUP01_Q", won_method=1, is_winner_by_random=0)
        self.show_log.expect(ware_md5="OFF2_2150_SKU1_SUP02_Q", won_method=6, is_winner_by_random=0)

    def test_sponsorred_offers_pattern_desktop_list_without_duplicates(self):
        # Проверяем что в выдаче с выключенным флагом market_buybox_auction_search_sponsored_places_allow_duplicates
        # нет одинаковых офферов
        # см https://st.yandex-team.ru/MADV-789

        vendor_conversion = 0.075
        sigmoid_alpha = 0.6
        sigmoid_beta = 0.0015
        sigmoid_gamma = 1

        rearr_flags_dict = {
            "market_report_mimicry_in_serp_pattern": 2,
            "market_buybox_auction_search_sponsored_places_web": 1,
            "market_buybox_auction_search_sponsored_places_allow_duplicates": 0,
            "market_buybox_auction_coef_b_multiplicative_bid_coef_search_sponsored": 0.001,
            "market_buybox_auction_rand_low": 1.00,
            "market_buybox_auction_rand_delta": 0.00,
            "market_buybox_auction_coef_w_rp_fee_coef_search_sponsored": 0.0,
            "market_buybox_auction_transfer_fee_to_search": 1,
            "market_buybox_enable_advert_buybox": 0,
            "market_tweak_search_auction_white_cpa_fee_no_text_params_desktop": "{},{},{}".format(
                sigmoid_alpha, sigmoid_beta, sigmoid_gamma
            ),
            "market_money_vendor_cpc_to_cpa_conversion_buybox": vendor_conversion,
            "market_white_search_auction_cpa_fee": 1,
            "market_white_search_auction_cpa_fee_transfer_fee_do_desktop": 1,
            "market_buybox_extra_auction_price_rel_max_threshold": 1.05,  # Возвращаем старый порог, под который написан тест
        }

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        request = (
            'pp=7&hid=91013&place=prime&rgb=green_with_blue&rids=213'
            '&show-urls=external,decrypted,direct%2Ccpa&bsformat=2&&viewtype=list'
            '&use-default-offers=1&allow-collapsing=1&allow-ungrouping=1&waitall=da&numdoc=48&debug=1'
            '&rearr-factors={}'.format(rearr_flags_str)
        )

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 7,
                    'results': [
                        {
                            'type': "model",
                            'id': 94301,
                            'offers': {
                                'items': [
                                    {
                                        # для скю=3 поисковый оффер совпадает с рекламным потому что в байбоксе участвует только один оффер
                                        'wareId': "OFF5_2100_SKU3_SUP05_Q",
                                        'marketSku': "3",
                                        'urls': {
                                            'cpa': Contains('/shop_fee=100/', '/shop_fee_ab=0/'),
                                        },
                                    },
                                ]
                            },
                        },
                        {
                            'type': "model",
                            'id': 94301,
                            'sponsored': True,
                            'offers': {
                                'items': [
                                    {
                                        # для скю=2 поисковый оффер совпадает с рекламным потому что победитель по GMV равен победителю по аукциону
                                        'wareId': "OFF3_2100_SKU2_SUP03_Q",
                                        'sponsored': True,
                                        'marketSku': "2",
                                        'urls': {
                                            # амнистируем об следующий рекламный оффер, а это OFF2_2150_SKU1_SUP02_Q
                                            'cpa': Contains('/shop_fee=50/', '/shop_fee_ab=32/', '/pp=231/'),
                                        },
                                    },
                                ]
                            },
                        },
                        {
                            'type': "model",
                            'id': 94301,
                            'sponsored': True,
                            'offers': {
                                'items': [
                                    {
                                        # для скю=1 это победитель аукциона в байбоксе используется только в рекламе (sponsored)
                                        'wareId': "OFF2_2150_SKU1_SUP02_Q",
                                        'sponsored': True,
                                        'marketSku': "1",
                                        'urls': {
                                            # амнистировать не об кого, но ставка shop_fee_ab=86 берется из аукциона в байбоксе
                                            'cpa': Contains('/shop_fee=100/', '/shop_fee_ab=86/', '/pp=231/'),
                                        },
                                    },
                                ]
                            },
                        },
                        {
                            'type': "model",
                            'id': 94301,
                            'offers': {
                                'items': [
                                    {
                                        # для скю=1 это лучший оффер по GMV используется только в поиске
                                        'wareId': "OFF1_2100_SKU1_SUP01_Q",
                                        'marketSku': "1",
                                        'urls': {
                                            'cpa': Contains('/shop_fee=0/', '/shop_fee_ab=0/'),
                                        },
                                    },
                                ]
                            },
                        },
                        {
                            'type': "model",
                            'id': 94301,
                            'offers': {
                                'items': [
                                    {
                                        'wareId': "OFF7_2100_SKU4_SUP07_Q",
                                        'marketSku': "4",
                                        'urls': {
                                            'cpa': Contains('/shop_fee=0/', '/shop_fee_ab=0/'),
                                        },
                                    },
                                ]
                            },
                        },
                        {
                            'type': "model",
                            'id': 94301,
                            'offers': {
                                'items': [
                                    {
                                        'wareId': "OFF8_2100_SKU5_SUP08_Q",
                                        'marketSku': "5",
                                        'urls': {
                                            'cpa': Contains('/shop_fee=0/', '/shop_fee_ab=0/'),
                                        },
                                    },
                                ]
                            },
                        },
                    ],
                }
            },
            preserve_order=True,
        )

        self.show_log.expect(ware_md5="OFF1_2100_SKU1_SUP01_Q", won_method=1, is_winner_by_random=0)
        self.show_log.expect(ware_md5="OFF2_2150_SKU1_SUP02_Q", won_method=6, is_winner_by_random=0)

    def test_sponsorred_offers_pattern_desktop_grid(self):
        # Аналогично test_sponsorred_offers_pattern_desktop_list только для грида на десктопе
        # см https://st.yandex-team.ru/MADV-789

        vendor_conversion = 0.075
        sigmoid_alpha = 0.6
        sigmoid_beta = 0.0015
        sigmoid_gamma = 1

        rearr_flags_dict = {
            "market_report_mimicry_in_serp_pattern": 2,
            "market_buybox_auction_search_sponsored_places_web": 1,
            "market_buybox_auction_coef_b_multiplicative_bid_coef_search_sponsored": 0.001,
            "market_buybox_auction_rand_low": 1.00,
            "market_buybox_auction_rand_delta": 0.00,
            "market_buybox_auction_coef_w_rp_fee_coef_search_sponsored": 0.0,
            "market_buybox_auction_transfer_fee_to_search": 1,
            "market_buybox_enable_advert_buybox": 0,
            "market_tweak_search_auction_white_cpa_fee_no_text_params_desktop": "{},{},{}".format(
                sigmoid_alpha, sigmoid_beta, sigmoid_gamma
            ),
            "market_money_vendor_cpc_to_cpa_conversion_buybox": vendor_conversion,
            "market_white_search_auction_cpa_fee": 1,
            "market_white_search_auction_cpa_fee_transfer_fee_do_desktop": 1,
            "market_buybox_extra_auction_price_rel_max_threshold": 1.05,  # Возвращаем старый порог, под который написан тест
            "market_buybox_auction_search_sponsored_places_allow_duplicates": 1,
            "market_report_mimicry_always_list_pattern": 0,
        }

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        request = (
            'pp=7&hid=91013&place=prime&rgb=green_with_blue&rids=213'
            '&show-urls=external,decrypted,direct%2Ccpa&bsformat=2&&viewtype=grid'
            '&use-default-offers=1&allow-collapsing=1&allow-ungrouping=1&waitall=da&numdoc=48&debug=1'
            '&rearr-factors={}'.format(rearr_flags_str)
        )

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 9,
                    'results': [
                        {
                            'type': "model",
                            'id': 94301,
                            'offers': {
                                'items': [
                                    {
                                        # для скю=3 поисковый оффер совпадает с рекламным потому что в байбоксе участвует только один оффер
                                        'wareId': "OFF5_2100_SKU3_SUP05_Q",
                                        'marketSku': "3",
                                        'urls': {
                                            'cpa': Contains('/shop_fee=100/', '/shop_fee_ab=0/'),
                                        },
                                    },
                                ]
                            },
                        },
                        {
                            'type': "model",
                            'id': 94301,
                            'offers': {
                                'items': [
                                    {
                                        'wareId': "OFF3_2100_SKU2_SUP03_Q",
                                        'marketSku': "2",
                                        'urls': {
                                            'cpa': Contains('/shop_fee=50/', '/shop_fee_ab=0/'),
                                        },
                                    },
                                ]
                            },
                        },
                        {
                            'type': "model",
                            'id': 94301,
                            'offers': {
                                'items': [
                                    {
                                        # для скю=1 это лучший оффер по GMV используется только в поиске
                                        'wareId': "OFF1_2100_SKU1_SUP01_Q",
                                        'marketSku': "1",
                                        'urls': {
                                            'cpa': Contains('/shop_fee=0/', '/shop_fee_ab=0/'),
                                        },
                                    },
                                ]
                            },
                        },
                        {
                            'type': "model",
                            'id': 94301,
                            'sponsored': True,
                            'offers': {
                                'items': [
                                    {
                                        'wareId': "OFF5_2100_SKU3_SUP05_Q",
                                        'sponsored': True,
                                        'marketSku': "3",
                                        'urls': {
                                            # амнистируем об следующий рекламный оффер, а это OFF3_2100_SKU2_SUP03_Q
                                            'cpa': Contains('/shop_fee=100/', '/shop_fee_ab=27/', '/pp=231/'),
                                        },
                                    },
                                ]
                            },
                        },
                        {
                            'type': "model",
                            'id': 94301,
                            'sponsored': True,
                            'offers': {
                                'items': [
                                    {
                                        # для скю=2 поисковый оффер совпадает с рекламным потому что победитель по GMV равен победителю по аукциону
                                        'wareId': "OFF3_2100_SKU2_SUP03_Q",
                                        'sponsored': True,
                                        'marketSku': "2",
                                        'urls': {
                                            # амнистируем об следующий рекламный оффер, а это OFF2_2150_SKU1_SUP02_Q
                                            'cpa': Contains('/shop_fee=50/', '/shop_fee_ab=32/', '/pp=231/'),
                                        },
                                    },
                                ]
                            },
                        },
                        {
                            'type': "model",
                            'id': 94301,
                            'sponsored': True,
                            'offers': {
                                'items': [
                                    {
                                        # для скю=1 это победитель аукциона в байбоксе используется только в рекламе (sponsored)
                                        'wareId': "OFF2_2150_SKU1_SUP02_Q",
                                        'sponsored': True,
                                        'marketSku': "1",
                                        'urls': {
                                            # амнистировать не об кого, но ставка shop_fee_ab=86 берется из аукциона в байбоксе
                                            'cpa': Contains('/shop_fee=100/', '/shop_fee_ab=86/', '/pp=231/'),
                                        },
                                    },
                                ]
                            },
                        },
                        {
                            'type': "model",
                            'id': 94301,
                            'offers': {
                                'items': [
                                    {
                                        'wareId': "OFF7_2100_SKU4_SUP07_Q",
                                        'urls': {
                                            'cpa': Contains('/shop_fee=0/', '/shop_fee_ab=0/'),
                                        },
                                    },
                                ]
                            },
                        },
                        {
                            'type': "model",
                            'id': 94301,
                            'offers': {
                                'items': [
                                    {
                                        'wareId': "OFF8_2100_SKU5_SUP08_Q",
                                        'urls': {
                                            'cpa': Contains('/shop_fee=0/', '/shop_fee_ab=0/'),
                                        },
                                    },
                                ]
                            },
                        },
                        {
                            'type': "model",
                            'id': 94301,
                            'offers': {
                                'items': [
                                    {
                                        'wareId': "OFF9_2100_SKU6_SUP09_Q",
                                        'urls': {
                                            'cpa': Contains('/shop_fee=0/', '/shop_fee_ab=0/'),
                                        },
                                    },
                                ]
                            },
                        },
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # проверим что с включенным флагом market_report_mimicry_always_list_pattern порядок стал как в листе
        rearr_flags_dict["market_report_mimicry_always_list_pattern"] = 1
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        request = (
            'pp=7&hid=91013&place=prime&rgb=green_with_blue&rids=213'
            '&show-urls=external,decrypted,direct%2Ccpa&bsformat=2&&viewtype=grid'
            '&use-default-offers=1&allow-collapsing=1&allow-ungrouping=1&waitall=da&numdoc=48&debug=1'
            '&rearr-factors={}'.format(rearr_flags_str)
        )

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 9,
                    'results': [
                        {
                            'type': "model",
                        },
                        {
                            'type': "model",
                            'sponsored': True,
                        },
                        {
                            'type': "model",
                            'sponsored': True,
                        },
                        {
                            'type': "model",
                        },
                        {
                            'type': "model",
                        },
                        {
                            'type': "model",
                            'sponsored': True,
                        },
                        {
                            'type': "model",
                        },
                        {
                            'type': "model",
                        },
                        {
                            'type': "model",
                        },
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_sponsorred_offers_pattern_app(self):
        # Проверяем, что флаги market_buybox_auction_search_sponsored_places_app и ..._web действительно разделены по платформам

        vendor_conversion = 0.075
        sigmoid_alpha = 0.6
        sigmoid_beta = 0.0015
        sigmoid_gamma = 1

        rearr_flags_dict = {
            "market_report_mimicry_in_serp_pattern": 2,
            "market_buybox_auction_search_sponsored_places_app": 1,
            "market_buybox_auction_coef_b_multiplicative_bid_coef_search_sponsored": 0.001,
            "market_buybox_auction_rand_low": 1.00,
            "market_buybox_auction_rand_delta": 0.00,
            "market_buybox_auction_coef_w_rp_fee_coef_search_sponsored": 0.0,
            "market_buybox_auction_transfer_fee_to_search": 1,
            "market_buybox_enable_advert_buybox": 0,
            "market_tweak_search_auction_white_cpa_fee_no_text_params_ios": "{},{},{}".format(
                sigmoid_alpha, sigmoid_beta, sigmoid_gamma
            ),
            "market_money_vendor_cpc_to_cpa_conversion_buybox": vendor_conversion,
            "market_white_search_auction_cpa_fee": 1,
            "market_white_search_auction_cpa_fee_transfer_fee_do_app": 1,
            "market_buybox_extra_auction_price_rel_max_threshold": 1.05,
            "market_buybox_auction_search_sponsored_places_allow_duplicates": 1,
        }

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        request = (
            'pp=1807&hid=91013&place=prime&rgb=green_with_blue&rids=213&client=IOS'
            '&show-urls=external,decrypted,direct%2Ccpa&bsformat=2&&viewtype=grid'
            '&use-default-offers=1&allow-collapsing=1&allow-ungrouping=1&waitall=da&numdoc=48&debug=1'
            '&rearr-factors={}'.format(rearr_flags_str)
        )

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'type': "model",
                        'id': 94301,
                        'sponsored': True,
                        'offers': {
                            'items': [
                                {
                                    'wareId': "OFF5_2100_SKU3_SUP05_Q",
                                    'sponsored': True,
                                    'marketSku': "3",
                                    'urls': {
                                        # амнистируем об следующий рекламный оффер, а это OFF3_2100_SKU2_SUP03_Q
                                        'cpa': Contains('/shop_fee=100/', '/shop_fee_ab=27/', '/pp=1810/'),
                                    },
                                },
                            ]
                        },
                    },
                    {
                        'type': "model",
                        'id': 94301,
                        'sponsored': True,
                        'offers': {
                            'items': [
                                {
                                    # для скю=2 поисковый оффер совпадает с рекламным потому что победитель по GMV равен победителю по аукциону
                                    'wareId': "OFF3_2100_SKU2_SUP03_Q",
                                    'sponsored': True,
                                    'marketSku': "2",
                                    'urls': {
                                        # амнистируем об следующий рекламный оффер, а это OFF2_2150_SKU1_SUP02_Q
                                        'cpa': Contains('/shop_fee=50/', '/shop_fee_ab=32/', '/pp=1810/'),
                                    },
                                },
                            ]
                        },
                    },
                ],
            },
            preserve_order=True,
        )

        # Проверяем, что флаг для веба не влияет на аппы
        rearr_flags_dict["market_buybox_auction_search_sponsored_places_app"] = 0
        rearr_flags_dict["market_buybox_auction_search_sponsored_places_web"] = 1
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        request = (
            'pp=7&hid=91013&place=prime&rgb=green_with_blue&rids=213&client=IOS'
            '&show-urls=external,decrypted,direct%2Ccpa&bsformat=2&&viewtype=grid'
            '&use-default-offers=1&allow-collapsing=1&allow-ungrouping=1&waitall=da&numdoc=48&debug=1'
            '&rearr-factors={}'.format(rearr_flags_str)
        )
        response = self.report.request_json(request)
        self.assertFragmentNotIn(response, {"sponsored": True})

    def test_sponsorred_offers_pattern_desktop_grid_without_duplicates(self):
        # сравниваем амнистии на поиске (shop_fee_ab_search) с дубликатами и без для оффера OFF6_2150_SKU3_SUP06_Q
        # Включаем повышенный ценовой фильтр
        # Амнистия (shop_fee_ab_search) остается прежней при отсутствии дубликатов потому что
        # мы подпираем оффер отфильтрованным OFF3_2100_SKU2_SUP03_Q
        # см https://st.yandex-team.ru/MADV-789

        vendor_conversion = 0.075
        sigmoid_alpha = 0.6
        sigmoid_beta = 0.0015
        sigmoid_gamma = 1

        rearr_flags_dict = {
            "market_report_mimicry_in_serp_pattern": 2,
            "market_buybox_auction_search_sponsored_places_web": 1,
            "market_buybox_auction_search_sponsored_places_allow_duplicates": 1,
            "market_buybox_extra_auction_price_rel_max_threshold": 1.1,
            "market_buybox_auction_coef_b_multiplicative_bid_coef_search_sponsored": 0.001,
            "market_buybox_auction_rand_low": 1.00,
            "market_buybox_auction_rand_delta": 0.00,
            "market_buybox_auction_coef_w_rp_fee_coef_search_sponsored": 0.0,
            "market_buybox_auction_transfer_fee_to_search": 1,
            "market_tweak_search_auction_white_cpa_fee_no_text_params_desktop": "{},{},{}".format(
                sigmoid_alpha, sigmoid_beta, sigmoid_gamma
            ),
            "market_money_vendor_cpc_to_cpa_conversion_buybox": vendor_conversion,
            "market_white_search_auction_cpa_fee": 1,
            "market_white_search_auction_cpa_fee_transfer_fee_do_desktop": 1,
            "market_report_mimicry_always_list_pattern": 0,
        }

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        request = (
            'pp=7&hid=91013&place=prime&rgb=green_with_blue&rids=213'
            '&show-urls=external,decrypted,direct%2Ccpa&bsformat=2&&viewtype=grid'
            '&use-default-offers=1&allow-collapsing=1&allow-ungrouping=1&waitall=da&numdoc=48&debug=1'
            '&rearr-factors={}'.format(rearr_flags_str)
        )

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 9,
                    'results': [
                        {
                            'offers': {
                                'items': [
                                    {
                                        'wareId': "OFF5_2100_SKU3_SUP05_Q",
                                    },
                                ]
                            },
                        },
                        {
                            'offers': {
                                'items': [
                                    {
                                        'wareId': "OFF3_2100_SKU2_SUP03_Q",
                                    },
                                ]
                            },
                        },
                        {
                            'offers': {
                                'items': [
                                    {
                                        'wareId': "OFF1_2100_SKU1_SUP01_Q",
                                    },
                                ]
                            },
                        },
                        {
                            'type': "model",
                            'id': 94301,
                            'sponsored': True,
                            'offers': {
                                'items': [
                                    {
                                        'wareId': "OFF6_2150_SKU3_SUP06_Q",
                                        'sponsored': True,
                                        'marketSku': "3",
                                        'urls': {
                                            # амнистируем об следующий рекламный оффер, а это OFF3_2100_SKU2_SUP03_Q
                                            'cpa': Contains(
                                                '/shop_fee=500/',
                                                '/shop_fee_ab=137/',
                                                '/shop_fee_ab_bb=137/',
                                                '/shop_fee_ab_search=25/',
                                                '/pp=231/',
                                            ),
                                        },
                                    },
                                ]
                            },
                        },
                        {
                            'sponsored': True,
                            'offers': {
                                'items': [
                                    {
                                        'wareId': "OFF3_2100_SKU2_SUP03_Q",
                                        'sponsored': True,
                                    },
                                ]
                            },
                        },
                        {
                            'sponsored': True,
                            'offers': {
                                'items': [
                                    {
                                        'wareId': "OFF2_2150_SKU1_SUP02_Q",
                                        'sponsored': True,
                                    },
                                ]
                            },
                        },
                    ],
                }
            },
            preserve_order=True,
        )

        rearr_flags_dict["market_buybox_auction_search_sponsored_places_allow_duplicates"] = 0
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        request = (
            'pp=7&hid=91013&place=prime&rgb=green_with_blue&rids=213'
            '&show-urls=external,decrypted,direct%2Ccpa&bsformat=2&&viewtype=grid'
            '&use-default-offers=1&allow-collapsing=1&allow-ungrouping=1&waitall=da&numdoc=48&debug=1'
            '&rearr-factors={}'.format(rearr_flags_str)
        )

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 8,
                    'results': [
                        {
                            'offers': {
                                'items': [
                                    {
                                        'wareId': "OFF5_2100_SKU3_SUP05_Q",
                                    },
                                ]
                            },
                        },
                        {
                            'offers': {
                                'items': [
                                    {
                                        'wareId': "OFF3_2100_SKU2_SUP03_Q",
                                    },
                                ]
                            },
                        },
                        {
                            'offers': {
                                'items': [
                                    {
                                        'wareId': "OFF1_2100_SKU1_SUP01_Q",
                                    },
                                ]
                            },
                        },
                        {
                            'type': "model",
                            'id': 94301,
                            'sponsored': True,
                            'offers': {
                                'items': [
                                    {
                                        'wareId': "OFF6_2150_SKU3_SUP06_Q",
                                        'sponsored': True,
                                        'marketSku': "3",
                                        'urls': {
                                            # амнистируем об следующий рекламный оффер, а это OFF3_2100_SKU2_SUP03_Q
                                            # несмотря на то что он в рекламу не попал потому что дубликат
                                            'cpa': Contains(
                                                '/shop_fee=500/',
                                                '/shop_fee_ab=137/',
                                                '/shop_fee_ab_bb=137/',
                                                '/shop_fee_ab_search=25/',
                                                '/pp=231/',
                                            ),
                                        },
                                    },
                                ]
                            },
                        },
                        {
                            'sponsored': True,
                            'offers': {
                                'items': [
                                    {
                                        'wareId': "OFF2_2150_SKU1_SUP02_Q",
                                        'sponsored': True,
                                    },
                                ]
                            },
                        },
                        {
                            'offers': {
                                'items': [
                                    {
                                        'wareId': "OFF7_2100_SKU4_SUP07_Q",
                                    },
                                ]
                            },
                        },
                    ],
                }
            },
            preserve_order=True,
        )

    def test_sponsorred_offers_pattern_touch(self):
        # Аналогично test_sponsorred_offers_pattern_desktop_list теперь и для touch
        # Уменьшаем кол-во документов на странице до 4-х и проверяем что дубли остались
        # см https://st.yandex-team.ru/MADV-789

        vendor_conversion = 0.075
        sigmoid_alpha = 0.6
        sigmoid_beta = 0.0015
        sigmoid_gamma = 1

        rearr_flags_dict = {
            "market_report_mimicry_in_serp_pattern": 2,
            "market_buybox_auction_search_sponsored_places_web": 1,
            "market_buybox_auction_coef_b_multiplicative_bid_coef_search_sponsored": 0.001,
            "market_buybox_auction_rand_low": 1.00,
            "market_buybox_auction_rand_delta": 0.00,
            "market_buybox_auction_coef_w_rp_fee_coef_search_sponsored": 0.0,
            "market_buybox_auction_transfer_fee_to_search": 1,
            "market_tweak_search_auction_white_cpa_fee_no_text_params_touch": "{},{},{}".format(
                sigmoid_alpha, sigmoid_beta, sigmoid_gamma
            ),
            "market_money_vendor_cpc_to_cpa_conversion_buybox": vendor_conversion,
            "market_white_search_auction_cpa_fee": 1,
            "market_white_search_auction_cpa_fee_transfer_fee_do_desktop": 1,
            "market_buybox_extra_auction_price_rel_max_threshold": 1.05,  # Возвращаем старый порог, под который написан тест
            "market_buybox_auction_search_sponsored_places_allow_duplicates": 1,
            "market_buybox_enable_advert_buybox": 0,
            "market_report_mimicry_always_list_pattern": 0,
        }

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        request = (
            'pp=48&hid=91013&place=prime&rgb=green_with_blue&rids=213'
            '&show-urls=external,decrypted,direct%2Ccpa&bsformat=2&&viewtype=grid&touch=1'
            '&use-default-offers=1&allow-collapsing=1&allow-ungrouping=1&waitall=da&page={page}&numdoc=4&debug=1'
            '&rearr-factors={flags}'
        )

        # запрос первой страницы
        response = self.report.request_json(request.format(page=1, flags=rearr_flags_str))
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 9,
                    'results': [
                        {
                            'type': "model",
                            'id': 94301,
                            'sponsored': Absent(),
                            'offers': {
                                'items': [
                                    {
                                        # для скю=3 поисковый оффер совпадает с рекламным потому что в байбоксе участвует только один оффер
                                        'wareId': "OFF5_2100_SKU3_SUP05_Q",
                                        'marketSku': "3",
                                        'urls': {
                                            'cpa': Contains('/shop_fee=100/', '/shop_fee_ab=0/'),
                                        },
                                    },
                                ]
                            },
                        },
                        {
                            'type': "model",
                            'id': 94301,
                            'sponsored': Absent(),
                            'offers': {
                                'items': [
                                    {
                                        'wareId': "OFF3_2100_SKU2_SUP03_Q",
                                        'marketSku': "2",
                                        'urls': {
                                            'cpa': Contains('/shop_fee=50/', '/shop_fee_ab=0/'),
                                        },
                                    },
                                ]
                            },
                        },
                        {
                            'type': "model",
                            'id': 94301,
                            'sponsored': True,
                            'offers': {
                                'items': [
                                    {
                                        'wareId': "OFF5_2100_SKU3_SUP05_Q",
                                        'sponsored': True,
                                        'marketSku': "3",
                                        'urls': {
                                            # амнистируем об следующий рекламный оффер, а это OFF3_2100_SKU2_SUP03_Q
                                            'cpa': Contains('/shop_fee=100/', '/shop_fee_ab=27/', '/pp=621/'),
                                        },
                                    },
                                ]
                            },
                        },
                        {
                            'type': "model",
                            'id': 94301,
                            'sponsored': True,
                            'offers': {
                                'items': [
                                    {
                                        # для скю=2 поисковый оффер совпадает с рекламным потому что победитель по GMV равен победителю по аукциону
                                        'wareId': "OFF3_2100_SKU2_SUP03_Q",
                                        'sponsored': True,
                                        'marketSku': "2",
                                        'urls': {
                                            # оффер OFF2_2150_SKU1_SUP02_Q подпирает снизу аукционную корзину, хоть сам в нее и не попадает
                                            'cpa': Contains('/shop_fee=50/', '/shop_fee_ab=32/', '/pp=621/'),
                                        },
                                    },
                                ]
                            },
                        },
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # запрос 2 страницы
        response = self.report.request_json(request.format(page=2, flags=rearr_flags_str))
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 9,
                    'results': [
                        {
                            'type': "model",
                            'id': 94301,
                            'sponsored': Absent(),
                            'offers': {
                                'items': [
                                    {
                                        'wareId': "OFF1_2100_SKU1_SUP01_Q",
                                        'marketSku': "1",
                                        'urls': {
                                            'cpa': Contains('/shop_fee=0/', '/shop_fee_ab=0/'),
                                        },
                                    },
                                ]
                            },
                        },
                        {
                            'type': "model",
                            'id': 94301,
                            'sponsored': Absent(),
                            'offers': {
                                'items': [
                                    {
                                        'wareId': "OFF7_2100_SKU4_SUP07_Q",
                                        'marketSku': "4",
                                    },
                                ]
                            },
                        },
                        {
                            'type': "model",
                            'id': 94301,
                            'sponsored': True,
                            'offers': {
                                'items': [
                                    {
                                        'wareId': "OFF2_2150_SKU1_SUP02_Q",
                                        'sponsored': True,
                                        'marketSku': "1",
                                        'urls': {
                                            'cpa': Contains('/shop_fee=100/', '/shop_fee_ab=86/', '/pp=621/'),
                                        },
                                    },
                                ]
                            },
                        },
                        {
                            'type': "model",
                            'id': 94301,
                            'sponsored': Absent(),
                            'offers': {
                                'items': [
                                    {
                                        'wareId': "OFF8_2100_SKU5_SUP08_Q",
                                        'marketSku': "5",
                                    },
                                ]
                            },
                        },
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # запрос 3 страницы которая без спонсорских товаров не набралась бы
        response = self.report.request_json(request.format(page=3, flags=rearr_flags_str))
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 9,
                    'results': [
                        {
                            'type': "model",
                            'id': 94301,
                            'sponsored': Absent(),
                            'offers': {
                                'items': [
                                    {
                                        'wareId': "OFF9_2100_SKU6_SUP09_Q",
                                        'urls': {
                                            'cpa': Contains('/shop_fee=0/', '/shop_fee_ab=0/'),
                                        },
                                    },
                                ]
                            },
                        },
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_sponsorred_offers_pattern_desktop_list_price_threshold(self):
        # проверяем что для дополнительного аукциона оффера берутся с учетом отдельного порога
        # см https://st.yandex-team.ru/MADV-789

        vendor_conversion = 0.075
        sigmoid_alpha = 0.6
        sigmoid_beta = 0.0015
        sigmoid_gamma = 1

        rearr_flags_dict = {
            "market_report_mimicry_in_serp_pattern": 2,
            "market_buybox_auction_search_sponsored_places_web": 1,
            "market_buybox_extra_auction_price_rel_max_threshold": 1.1,
            "market_buybox_auction_coef_b_multiplicative_bid_coef_search_sponsored": 0.001,
            "market_buybox_auction_rand_low": 1.00,
            "market_buybox_auction_rand_delta": 0.00,
            "market_buybox_auction_coef_w_rp_fee_coef_search_sponsored": 0.0,
            "market_buybox_auction_transfer_fee_to_search": 1,
            "market_tweak_search_auction_white_cpa_fee_no_text_params_desktop": "{},{},{}".format(
                sigmoid_alpha, sigmoid_beta, sigmoid_gamma
            ),
            "market_money_vendor_cpc_to_cpa_conversion_buybox": vendor_conversion,
            "market_white_search_auction_cpa_fee": 1,
            "market_white_search_auction_cpa_fee_transfer_fee_do_desktop": 1,
        }

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        request = (
            'pp=7&hid=91013&place=prime&rgb=green_with_blue&rids=213'
            '&show-urls=external,decrypted,direct%2Ccpa&bsformat=2&&viewtype=list'
            '&use-default-offers=1&allow-collapsing=1&allow-ungrouping=1&waitall=da&numdoc=48&debug=1'
            '&rearr-factors={}'.format(rearr_flags_str)
        )

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'type': "model",
                        'id': 94301,
                        'offers': {
                            'items': [
                                {
                                    'wareId': "OFF5_2100_SKU3_SUP05_Q",
                                    'marketSku': "3",
                                    'urls': {
                                        'cpa': Contains('/shop_fee=100/', '/shop_fee_ab=0/'),
                                    },
                                    'debug': {
                                        'buyboxDebug': {
                                            'WonMethod': 'SINGLE_OFFER_AFTER_BUYBOX_FILTERS',
                                            'Offers': [
                                                {
                                                    'WareMd5': 'OFF5_2100_SKU3_SUP05_Q',
                                                },
                                            ],
                                            'RejectedOffers': [
                                                {
                                                    'RejectReason': 'TOO_HIGH_PRICE',
                                                    'Offer': {
                                                        'WareMd5': 'OFF6_2150_SKU3_SUP06_Q',
                                                    },
                                                },
                                            ],
                                        }
                                    },
                                },
                            ]
                        },
                    },
                    {
                        'type': "model",
                        'id': 94301,
                        'sponsored': True,
                        'offers': {
                            'items': [
                                {
                                    'wareId': "OFF6_2150_SKU3_SUP06_Q",
                                    'sponsored': True,
                                    'marketSku': "3",
                                    'urls': {
                                        # после увеличения порога байбокс выигрывает оффер с более высокой ставкой
                                        'cpa': Contains(
                                            '/shop_fee=500/',
                                            '/shop_fee_ab=137/',
                                            '/shop_fee_ab_bb=137/',
                                            '/shop_fee_ab_search=25/',
                                            '/pp=231/',
                                        ),
                                    },
                                    'debug': {
                                        'buyboxDebug': {
                                            'WonMethod': 'WON_BY_AUCTION',
                                            'Offers': [
                                                {
                                                    'WareMd5': 'OFF5_2100_SKU3_SUP05_Q',
                                                },
                                                {
                                                    'WareMd5': 'OFF6_2150_SKU3_SUP06_Q',
                                                },
                                            ],
                                            'RejectedOffers': EmptyList(),
                                        }
                                    },
                                },
                            ]
                        },
                    },
                ]
            },
        )

    @classmethod
    def prepare_offers_without_fees(cls):
        # все тоже, но без ставок

        cls.index.models += [
            Model(
                hyperid=94302,
                hid=91014,
                title="Исходная модель 94302",
                ungrouped_blue=[
                    UngroupedModel(
                        group_id=11,
                        title="Расхлопнутая модель 94302.11",
                        key='94302_11',
                    ),
                    UngroupedModel(
                        group_id=12,
                        title="Расхлопнутая модель 94302.12",
                        key='94302_12',
                    ),
                    UngroupedModel(
                        group_id=13,
                        title="Расхлопнутая модель 94302.13",
                        key='94302_13',
                    ),
                    UngroupedModel(
                        group_id=14,
                        title="Расхлопнутая модель 94302.14",
                        key='94302_14',
                    ),
                    UngroupedModel(
                        group_id=15,
                        title="Расхлопнутая модель 94302.15",
                        key='94302_15',
                    ),
                    UngroupedModel(
                        group_id=16,
                        title="Расхлопнутая модель 94302.16",
                        key='94302_16',
                    ),
                ],
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                hyperid=94302,
                delivery_buckets=[1234],
                sku=11,
                buybox_elasticity=[
                    Elasticity(price_variant=2100, demand_mean=200),
                    Elasticity(price_variant=2200, demand_mean=160),
                    Elasticity(price_variant=2500, demand_mean=30),
                ],
                blue_offers=[
                    BlueOffer(
                        ts=7521,
                        price=2100,
                        feedid=3101,
                        waremd5='OFF21_2100_SKU11_SUP01',
                    ),
                    BlueOffer(
                        ts=7522,
                        price=2150,
                        feedid=3102,
                        waremd5='OFF22_2150_SKU11_SUP02',
                    ),
                ],
                ungrouped_model_blue=11,
            ),
            MarketSku(
                hyperid=94302,
                delivery_buckets=[1234],
                sku=12,
                buybox_elasticity=[
                    Elasticity(price_variant=2100, demand_mean=200),
                    Elasticity(price_variant=2200, demand_mean=160),
                    Elasticity(price_variant=2500, demand_mean=30),
                ],
                blue_offers=[
                    BlueOffer(
                        ts=7523,
                        price=2100,
                        feedid=3103,
                        waremd5='OFF23_2100_SKU12_SUP03',
                    ),
                    BlueOffer(
                        ts=7524,
                        price=2150,
                        feedid=3104,
                        waremd5='OFF24_2150_SKU12_SUP04',
                    ),
                ],
                ungrouped_model_blue=12,
            ),
            MarketSku(
                hyperid=94302,
                delivery_buckets=[1234],
                sku=13,
                buybox_elasticity=[
                    Elasticity(price_variant=2100, demand_mean=200),
                    Elasticity(price_variant=2200, demand_mean=160),
                    Elasticity(price_variant=2500, demand_mean=30),
                ],
                blue_offers=[
                    BlueOffer(
                        ts=7525,
                        price=2100,
                        feedid=3105,
                        waremd5='OFF25_2100_SKU13_SUP05',
                    ),
                    BlueOffer(
                        ts=7526,
                        price=2500,
                        feedid=3106,
                        waremd5='OFF26_2150_SKU13_SUP06',
                    ),
                ],
                ungrouped_model_blue=13,
            ),
            MarketSku(
                hyperid=94302,
                delivery_buckets=[1234],
                sku=14,
                buybox_elasticity=[
                    Elasticity(price_variant=2100, demand_mean=200),
                    Elasticity(price_variant=2200, demand_mean=160),
                    Elasticity(price_variant=2500, demand_mean=30),
                ],
                blue_offers=[
                    BlueOffer(
                        ts=7527,
                        price=2100,
                        feedid=3107,
                        waremd5='OFF27_2100_SKU14_SUP07',
                    ),
                ],
                ungrouped_model_blue=14,
            ),
            MarketSku(
                hyperid=94302,
                delivery_buckets=[1234],
                sku=15,
                buybox_elasticity=[
                    Elasticity(price_variant=2100, demand_mean=200),
                    Elasticity(price_variant=2200, demand_mean=160),
                    Elasticity(price_variant=2500, demand_mean=30),
                ],
                blue_offers=[
                    BlueOffer(
                        ts=7528,
                        price=2100,
                        feedid=3108,
                        waremd5='OFF28_2100_SKU15_SUP08',
                    ),
                ],
                ungrouped_model_blue=15,
            ),
            MarketSku(
                hyperid=94302,
                delivery_buckets=[1234],
                sku=16,
                buybox_elasticity=[
                    Elasticity(price_variant=2100, demand_mean=200),
                    Elasticity(price_variant=2200, demand_mean=160),
                    Elasticity(price_variant=2500, demand_mean=30),
                ],
                blue_offers=[
                    BlueOffer(
                        ts=7529,
                        price=2100,
                        feedid=3109,
                        waremd5='OFF29_2100_SKU16_SUP09',
                    ),
                ],
                ungrouped_model_blue=16,
            ),
        ]

        for place in [MnPlace.BASE_SEARCH, MnPlace.META_REARRANGE]:
            cls.matrixnet.on_place(place, 7521).respond(0.9)
            cls.matrixnet.on_place(place, 7522).respond(0.9)
            cls.matrixnet.on_place(place, 7523).respond(0.903)
            cls.matrixnet.on_place(place, 7524).respond(0.903)
            cls.matrixnet.on_place(place, 7525).respond(0.904)
            cls.matrixnet.on_place(place, 7526).respond(0.904)
            cls.matrixnet.on_place(place, 7527).respond(0.85)
            cls.matrixnet.on_place(place, 7528).respond(0.84)
            cls.matrixnet.on_place(place, 7529).respond(0.79)

    def test_sponsorred_offers_pattern_desktop_list_without_fees(self):
        # Проверяем что если нет ставок, то нет дублей, как и спонсорских предложений
        # см https://st.yandex-team.ru/MADV-789

        vendor_conversion = 0.075
        sigmoid_alpha = 0.6
        sigmoid_beta = 0.0015
        sigmoid_gamma = 1

        rearr_flags_dict = {
            "market_report_mimicry_in_serp_pattern": 2,
            "market_buybox_auction_search_sponsored_places_web": 1,
            "market_buybox_auction_coef_b_multiplicative_bid_coef_search_sponsored": 0.001,
            "market_buybox_auction_rand_low": 1.00,
            "market_buybox_auction_rand_delta": 0.00,
            "market_buybox_auction_coef_w_rp_fee_coef_search_sponsored": 0.0,
            "market_buybox_auction_transfer_fee_to_search": 1,
            "market_tweak_search_auction_white_cpa_fee_no_text_params_desktop": "{},{},{}".format(
                sigmoid_alpha, sigmoid_beta, sigmoid_gamma
            ),
            "market_money_vendor_cpc_to_cpa_conversion_buybox": vendor_conversion,
            "market_white_search_auction_cpa_fee": 1,
            "market_white_search_auction_cpa_fee_transfer_fee_do_desktop": 1,
        }

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        request = (
            'pp=7&hid=91014&place=prime&rgb=green_with_blue&rids=213'
            '&show-urls=external,decrypted,direct%2Ccpa&bsformat=2&&viewtype=list'
            '&use-default-offers=1&allow-collapsing=1&allow-ungrouping=1&waitall=da&numdoc=48&debug=1'
            '&rearr-factors={}'.format(rearr_flags_str)
        )

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'type': "model",
                        'id': 94302,
                        'offers': {
                            'items': [
                                {
                                    'wareId': "OFF25_2100_SKU13_SUP0w",
                                    'marketSku': "13",
                                    'urls': {
                                        'cpa': Contains('/shop_fee=0/', '/shop_fee_ab=0/'),
                                    },
                                },
                            ]
                        },
                    },
                    {
                        'type': "model",
                        'id': 94302,
                        'offers': {
                            'items': [
                                {
                                    'wareId': "OFF23_2100_SKU12_SUP0w",
                                    'marketSku': "12",
                                    'urls': {
                                        'cpa': Contains('/shop_fee=0/', '/shop_fee_ab=0/'),
                                    },
                                },
                            ]
                        },
                    },
                    {
                        'type': "model",
                        'id': 94302,
                        'offers': {
                            'items': [
                                {
                                    'wareId': "OFF21_2100_SKU11_SUP0w",
                                    'marketSku': "11",
                                    'urls': {
                                        'cpa': Contains('/shop_fee=0/', '/shop_fee_ab=0/'),
                                    },
                                },
                            ]
                        },
                    },
                    {
                        'type': "model",
                        'id': 94302,
                        'offers': {
                            'items': [
                                {
                                    'wareId': "OFF27_2100_SKU14_SUP0w",
                                    'marketSku': "14",
                                    'urls': {
                                        'cpa': Contains('/shop_fee=0/', '/shop_fee_ab=0/'),
                                    },
                                },
                            ]
                        },
                    },
                    {
                        'type': "model",
                        'id': 94302,
                        'offers': {
                            'items': [
                                {
                                    'wareId': "OFF28_2100_SKU15_SUP0w",
                                    'marketSku': "15",
                                    'urls': {
                                        'cpa': Contains('/shop_fee=0/', '/shop_fee_ab=0/'),
                                    },
                                },
                            ]
                        },
                    },
                    {
                        'type': "model",
                        'id': 94302,
                        'offers': {
                            'items': [
                                {
                                    'wareId': "OFF29_2100_SKU16_SUP0w",
                                    'marketSku': "16",
                                    'urls': {
                                        'cpa': Contains('/shop_fee=0/', '/shop_fee_ab=0/'),
                                    },
                                },
                            ]
                        },
                    },
                ]
            },
            preserve_order=True,
        )

    def test_won_by_exchange_and_auction(self):
        # Проверяем, что для случаев, где и основной, и доп. аукционный оффер - один и тот же, назначается won_method: EXCHANGE_AND_AUCTION

        vendor_conversion = 0.075
        sigmoid_alpha = 0.6
        sigmoid_beta = 0.0015
        sigmoid_gamma = 1

        rearr_flags_dict = {
            "market_buybox_auction_search_sponsored_places_web": 1,
            "market_buybox_auction_rand_low": 1.00,
            "market_buybox_auction_rand_delta": 0.00,
            "market_buybox_auction_coef_w_rp_fee_coef_search_sponsored": 0.0,
            "market_buybox_auction_transfer_fee_to_search": 1,
            "market_tweak_search_auction_white_cpa_fee_no_text_params_desktop": "{},{},{}".format(
                sigmoid_alpha, sigmoid_beta, sigmoid_gamma
            ),
            "market_money_vendor_cpc_to_cpa_conversion_buybox": vendor_conversion,
        }

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        request = (
            'pp=7&hid=91013&place=prime&rgb=green_with_blue&rids=213'
            '&show-urls=external,decrypted,direct%2Ccpa&bsformat=2&&viewtype=list'
            '&use-default-offers=1&allow-collapsing=1&allow-ungrouping=1&waitall=da&numdoc=48&debug=1'
            '&rearr-factors={}'.format(rearr_flags_str)
        )

        # Обычный и доп. аукционный байбоксы выигрывает один и тот же оффер OFF3_2100_SKU2_SUP03_Q
        # и его WonMethod переписывается на EXCHANGE_AND_AUCTION
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'offers': {
                            'items': [
                                {
                                    'wareId': 'OFF3_2100_SKU2_SUP03_Q',
                                    'debug': {
                                        'buyboxDebug': {
                                            'WonMethod': 'EXCHANGE_AND_AUCTION',
                                        },
                                    },
                                },
                            ],
                        },
                    },
                ],
            },
            preserve_order=True,
        )

        self.show_log.expect(ware_md5="OFF3_2100_SKU2_SUP03_Q", won_method=9, is_winner_by_random=0)

    @classmethod
    def prepare_buybox_auction_1p_pessimization(cls):
        cls.index.shops += [
            Shop(
                fesh=2,
                datafeed_id=2,
                priority_region=213,
                regions=[213],
                name="Один 1P поставщик",
                supplier_type=Shop.FIRST_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=2,
                fulfillment_program=True,
            ),
            Shop(
                fesh=3201,
                datafeed_id=3201,
                priority_region=213,
                regions=[213],
                supplier_type=Shop.THIRD_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                fulfillment_program=True,
                warehouse_id=2,
            ),
            Shop(
                fesh=3202,
                datafeed_id=3202,
                priority_region=213,
                regions=[213],
                supplier_type=Shop.THIRD_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                fulfillment_program=True,
                warehouse_id=2,
            ),
        ]
        cls.index.models += [
            Model(hid=3009, hyperid=3009, ts=509, title='model_9', vbid=11),
        ]
        cls.index.mskus += [
            MarketSku(
                title="model_9_msku_1",
                hid=3009,
                hyperid=3009,
                sku=100009,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(
                        price=1640,
                        feedid=2,
                        waremd5="BLUE-100009-FEED-0001Q",
                        title="model_9 1P buybox offer 1",
                        ts=7015,
                    ),
                    BlueOffer(
                        price=1640,
                        feedid=3201,
                        fee=200,
                        waremd5="BLUE-100009-FEED-0002Q",
                        title="model_9 3P buybox offer 2",
                        ts=7016,
                    ),
                    BlueOffer(
                        price=1600,
                        feedid=3202,
                        waremd5="BLUE-100009-FEED-0003Q",
                        title="model_9 1P buybox offer 3",
                        ts=7017,
                    ),
                ],
            ),
        ]
        # cls.recommender.on_request_accessory_models(
        #     model_id=3008, item_count=1000, version='SIBLINGS1_AUGMENTED'
        # ).respond({'models': ['3009:0.5']})
        cls.index.recommended_fee += [
            RecommendedFee(hyper_id=3009, recommended_bid=0.0220),
        ]
        cls.index.reserveprice_fee += [
            ReservePriceFee(hyper_id=3009, reserveprice_fee=0.01),
        ]

    def test_buybox_auction_1p_pessimization(self):
        # Проверяем, что работает пессимизация ставки 1p оффера в байбоксе для трафаретов

        vendor_conversion = 0.075
        sigmoid_alpha = 0.6
        sigmoid_beta = 0.0015
        sigmoid_gamma = 1

        rearr_flags_dict = {
            "market_buybox_auction_search_sponsored_places_web": 1,
            "market_buybox_auction_rand_low": 1.00,
            "market_buybox_auction_rand_delta": 0.00,
            "market_buybox_auction_coef_w_rp_fee_coef_search_sponsored": 0.0,
            "market_buybox_auction_transfer_fee_to_search": 1,
            "market_tweak_search_auction_white_cpa_fee_no_text_params_desktop": "{},{},{}".format(
                sigmoid_alpha, sigmoid_beta, sigmoid_gamma
            ),
            "market_money_vendor_cpc_to_cpa_conversion_buybox": vendor_conversion,
            "market_buybox_extra_auction_price_rel_max_threshold": 1.10,
        }

        request = (
            '{}&hid=3009&place=prime&rgb=green_with_blue&rids=213'
            '&show-urls=external,decrypted,direct%2Ccpa&bsformat=2&&viewtype=list'
            '&use-default-offers=1&allow-collapsing=1&allow-ungrouping=1&waitall=da&numdoc=12&debug=1'
            '&rearr-factors={}'
        )

        def get_expected_response(offer_id):
            return {
                'results': [
                    {
                        'offers': {
                            'items': [
                                {
                                    'wareId': 'BLUE-100009-FEED-0003Q',
                                },
                            ],
                        },
                    },
                    {
                        'offers': {
                            'items': [
                                {
                                    'wareId': offer_id,
                                    'sponsored': True,
                                },
                            ],
                        },
                    },
                ],
            }

        # Проверяем для веба
        rearr_flags_dict["market_buybox_auction_coef_1p_pessimization_search_sponsored_web"] = 1.0
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)

        response = self.report.request_json(request.format('pp=7', rearr_flags_str))
        self.assertFragmentIn(response, get_expected_response('BLUE-100009-FEED-0001Q'), preserve_order=True)

        response = self.report.request_json(request.format('pp=48&touch=1', rearr_flags_str))
        self.assertFragmentIn(response, get_expected_response('BLUE-100009-FEED-0001Q'), preserve_order=True)

        rearr_flags_dict["market_buybox_auction_coef_1p_pessimization_search_sponsored_web"] = 0.7
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)

        response = self.report.request_json(request.format('pp=7', rearr_flags_str))
        self.assertFragmentIn(response, get_expected_response('BLUE-100009-FEED-0002Q'), preserve_order=True)

        response = self.report.request_json(request.format('pp=48&touch=1', rearr_flags_str))
        self.assertFragmentIn(response, get_expected_response('BLUE-100009-FEED-0002Q'), preserve_order=True)

        # проверяем для аппа
        del rearr_flags_dict['market_buybox_auction_search_sponsored_places_web']
        rearr_flags_dict["market_buybox_auction_search_sponsored_places_app"] = 1
        del rearr_flags_dict['market_buybox_auction_coef_1p_pessimization_search_sponsored_web']

        rearr_flags_dict["market_buybox_auction_coef_1p_pessimization_search_sponsored_app"] = 1.0
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)

        response = self.report.request_json(request.format('pp=1707&client=ANDROID', rearr_flags_str))
        self.assertFragmentIn(response, get_expected_response('BLUE-100009-FEED-0001Q'), preserve_order=True)

        response = self.report.request_json(request.format('pp=1807&client=IOS', rearr_flags_str))
        self.assertFragmentIn(response, get_expected_response('BLUE-100009-FEED-0001Q'), preserve_order=True)

        rearr_flags_dict["market_buybox_auction_coef_1p_pessimization_search_sponsored_app"] = 0.7
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)

        response = self.report.request_json(request.format('pp=1707&client=ANDROID', rearr_flags_str))
        self.assertFragmentIn(response, get_expected_response('BLUE-100009-FEED-0002Q'), preserve_order=True)

        response = self.report.request_json(request.format('pp=1807&client=IOS', rearr_flags_str))
        self.assertFragmentIn(response, get_expected_response('BLUE-100009-FEED-0002Q'), preserve_order=True)

    @classmethod
    def prepare_auction_in_turboapp(cls):
        cls.index.models += [
            Model(hid=3010, hyperid=3010, ts=3010, title='model_3010 чехол', vbid=11),
            Model(hid=3010, hyperid=3011, ts=3011, title='model_3011 чехол', vbid=11),
            Model(hid=3010, hyperid=3012, ts=3012, title='model_3012 чехол', vbid=11),
            Model(hid=3010, hyperid=3013, ts=3013, title='model_3013 чехол', vbid=11),
        ]
        cls.index.mskus += [
            MarketSku(
                title="model_3010_msku_1 чехол",
                hid=3010,
                hyperid=3010,
                sku=301001,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(
                        price=1640,
                        feedid=3101,
                        fee=0,
                        waremd5="BLUE-301001-OFF1-0001Q",
                        title="model_3010 msku 301001 3P offer 1 чехол",
                        ts=3010011,
                    ),
                    BlueOffer(
                        price=1600,
                        feedid=3102,
                        fee=0,
                        waremd5="BLUE-301001-OFF2-0001Q",
                        title="model_3010 msku 301001 3P offer 2 чехол",
                        ts=3010012,
                    ),
                ],
            ),
            MarketSku(
                title="model_3011_msku_1 чехол",
                hid=3010,
                hyperid=3011,
                sku=301101,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(
                        price=1640,
                        feedid=3101,
                        fee=500,
                        waremd5="BLUE-301101-OFF1-0001Q",
                        title="model_3011 msku 301101 3P offer 1 чехол",
                        ts=3011011,
                    ),
                    BlueOffer(
                        price=1650,
                        feedid=3102,
                        fee=480,
                        waremd5="BLUE-301101-OFF2-0001Q",
                        title="model_3011 msku 301101 3P offer 2 чехол",
                        ts=3011012,
                    ),
                ],
            ),
            MarketSku(
                title="model_3012_msku_1 чехол",
                hid=3010,
                hyperid=3012,
                sku=301201,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(
                        price=1640,
                        feedid=3101,
                        fee=450,
                        waremd5="BLUE-301201-OFF1-0001Q",
                        title="model_3012 msku 301201 3P offer 1 чехол",
                        ts=3012011,
                    ),
                    BlueOffer(
                        price=1650,
                        feedid=3102,
                        fee=400,
                        waremd5="BLUE-301201-OFF2-0001Q",
                        title="model_3012 msku 301201 3P offer 2 чехол",
                        ts=3012012,
                    ),
                ],
            ),
            MarketSku(
                title="model_3013_msku_1 чехол",
                hid=3010,
                hyperid=3013,
                sku=301301,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(
                        price=1640,
                        feedid=3101,
                        fee=0,
                        waremd5="BLUE-301301-OFF1-0001Q",
                        title="model_3013 msku 301301 3P offer 1 чехол",
                        ts=3013011,
                    ),
                    BlueOffer(
                        price=1600,
                        feedid=3102,
                        fee=0,
                        waremd5="BLUE-301301-OFF2-0001Q",
                        title="model_3013 msku 301301 3P offer 2 чехол",
                        ts=3013012,
                    ),
                ],
            ),
        ]
        for place in [MnPlace.BASE_SEARCH, MnPlace.META_REARRANGE]:
            cls.matrixnet.on_place(place, 3010011).respond(0.9)
            cls.matrixnet.on_place(place, 3010012).respond(0.9)
            cls.matrixnet.on_place(place, 3011011).respond(0.87)
            cls.matrixnet.on_place(place, 3011012).respond(0.87)
            cls.matrixnet.on_place(place, 3012011).respond(0.85)
            cls.matrixnet.on_place(place, 3012012).respond(0.85)
            cls.matrixnet.on_place(place, 3013011).respond(0.83)
            cls.matrixnet.on_place(place, 3013012).respond(0.83)

    def test_sponsored_auction_on_special_pp(self):
        """
        Проверяем, что на "неожиданных" pp (например, в турбоаппах в поиске) включается
        аукцион в трафаретах, и только он
        (старый поисковый аукцион не включается, когда работает аукцион в трафаретах)
        """

        rearr_flags_dict = {
            "market_buybox_auction_search_sponsored_places_web": 1,
            "market_buybox_auction_search_sponsored_places_app": 1,
            "market_buybox_auction_rand_low": 1.00,
            "market_buybox_auction_rand_delta": 0.00,
            "market_buybox_auction_coef_w_rp_fee_coef_search_sponsored": 0.0,
            "market_buybox_auction_transfer_fee_to_search": 1,
        }

        request = (
            '{}&text=чехол&place=prime&rgb=green_with_blue&rids=213'
            '&show-urls=external,decrypted,direct%2Ccpa&bsformat=2&viewtype=list'
            '&use-default-offers=1&allow-collapsing=1&allow-ungrouping=1&waitall=da&numdoc=12&debug=da'
            '&rearr-factors={}'
        )

        pp_search_default = 7  # Проверяем и для обычного поиска на десктопе, чтобы была консистентность
        pp_turboapp_search = 3048  # Поиск в тубоаппах
        for pp in [pp_search_default, pp_turboapp_search]:
            response = self.report.request_json(request.format('pp=' + str(pp), dict_to_rearr(rearr_flags_dict)))
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {
                                # Первый сниппет - оффер модели 3010
                                # У этого оффера самый большой скор метаформулы
                                "id": 3010,
                                "offers": {
                                    "items": [
                                        {
                                            "wareId": "BLUE-301001-OFF2-0001Q",
                                            "sponsored": Absent(),
                                            "debug": {
                                                "sale": {
                                                    # Позиция первая, ставки нулевые - "чисто" органика
                                                    # (не трафарет)
                                                    "shopFee": 0,
                                                    "brokeredFee": 0,
                                                },
                                            },
                                        },
                                    ],
                                },
                            },
                            {
                                # Второй сниппет - оффер модели 3011
                                # Спонсорская позиция, попадает оффер со ставкой
                                "id": 3011,
                                "offers": {
                                    "items": [
                                        {
                                            "wareId": "BLUE-301101-OFF1-0001Q",
                                            "sponsored": True,
                                            "debug": {
                                                "sale": {
                                                    # Есть ставка и списываемая ставка,
                                                    # т.к. спонсорское место
                                                    "shopFee": Greater(0),
                                                    "brokeredFee": Greater(0),
                                                },
                                            },
                                        },
                                    ],
                                },
                            },
                            {
                                # Третий сниппет - оффер модели 3012
                                # Спонсорская позиция, попадает оффер со ставкой
                                "id": 3012,
                                "offers": {
                                    "items": [
                                        {
                                            "wareId": "BLUE-301201-OFF1-0001Q",
                                            "sponsored": True,
                                            "debug": {
                                                "sale": {
                                                    # Есть ставка и списываемая ставка,
                                                    # т.к. спонсорское место
                                                    "shopFee": Greater(0),
                                                    "brokeredFee": Greater(0),
                                                },
                                            },
                                        },
                                    ],
                                },
                            },
                            {
                                # Последний сниппет - оффер модели 3013
                                # У оффера нет ставки, самый маленький скор метаформулы
                                "id": 3013,
                                "offers": {
                                    "items": [
                                        {
                                            "wareId": "BLUE-301301-OFF2-0001Q",
                                            "sponsored": Absent(),
                                            "debug": {
                                                "sale": {
                                                    # Позиция четвёртая, ставки нулевые - не трафарет
                                                    "shopFee": 0,
                                                    "brokeredFee": 0,
                                                },
                                            },
                                        },
                                    ],
                                },
                            },
                        ],
                    },
                },
                preserve_order=True,  # Порядок очень важен - его и проверяем
                allow_different_len=False,
            )

    def test_turboapp_search_sponsored_pp(self):
        """
        Проверяем, что в turboapp проставляется правильный pp
        """

        rearr_flags_dict = {
            "market_buybox_auction_search_sponsored_places_web": 1,
            "market_buybox_auction_search_sponsored_places_app": 1,
            "market_buybox_auction_rand_low": 1.00,
            "market_buybox_auction_rand_delta": 0.00,
            "market_buybox_auction_coef_w_rp_fee_coef_search_sponsored": 0.0,
            "market_buybox_auction_transfer_fee_to_search": 1,
        }

        pp_turboapp_search = 3048  # Поиск в тубоаппах
        pp_turboapp_search_sponsored = 3010  # Трафареты в турбоаппах

        request = (
            'pp={}&text=чехол&place=prime&rgb=green_with_blue&rids=213'
            '&show-urls=external,decrypted,direct%2Ccpa&bsformat=2&viewtype=list'
            '&use-default-offers=1&allow-collapsing=1&allow-ungrouping=1&waitall=da&numdoc=12&debug=da'
            '&rearr-factors='.format(pp_turboapp_search)
        )

        response = self.report.request_json(request + dict_to_rearr(rearr_flags_dict))
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            # Первый сниппет - "чистая органика", не трафарет
                            "id": 3010,
                            "offers": {
                                "items": [
                                    {
                                        "wareId": "BLUE-301001-OFF2-0001Q",
                                        "sponsored": Absent(),
                                        "urls": {
                                            "cpa": Contains("/pp={}/".format(pp_turboapp_search)),
                                        },
                                        "feeShowPlain": Contains("\npp: {}\n".format(pp_turboapp_search)),
                                    },
                                ],
                            },
                        },
                        {
                            # Второй сниппет - трафарет
                            "id": 3011,
                            "offers": {
                                "items": [
                                    {
                                        "wareId": "BLUE-301101-OFF1-0001Q",
                                        "sponsored": True,
                                        "urls": {
                                            "cpa": Contains("/pp={}/".format(pp_turboapp_search_sponsored)),
                                        },
                                        "feeShowPlain": Contains("\npp: {}\n".format(pp_turboapp_search_sponsored)),
                                    },
                                ],
                            },
                        },
                    ],
                },
            },
        )

    @classmethod
    def prepare_sponsored_docs_with_regional_delimeter(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.shops += [
            Shop(
                fesh=3250 + i,
                priority_region=300,
                cpa=Shop.CPA_REAL,
            )
            for i in range(1, 4)
        ] + [Shop(fesh=3250 + i, priority_region=400, cpa=Shop.CPA_REAL, regions=[300]) for i in range(4, 7)]

        cls.index.models += [
            Model(
                hyperid=94450 + i,
                hid=91025,
                title="model_{}".format(94450 + i),
            )
            for i in range(1, 7)
        ]

        cls.index.mskus += [
            MarketSku(
                title="nokia msku {}".format(i),
                hid=91025,
                hyperid=94450 + i,
                sku=944500 + i,
                delivery_buckets=[1234],
                ts=944500 + i,
            )
            for i in range(1, 7)
        ]

        cls.index.offers += [
            Offer(
                hid=91025,
                hyperid=94450 + i,
                sku=944500 + i,
                fesh=3250 + i,
                ts=7650 + i,
                price=1000,
                title='nokia#{}'.format(7650 + i),
                cpa=Offer.CPA_REAL,
            )
            for i in range(1, 4)
        ] + [
            Offer(
                hid=91025,
                hyperid=94450 + i,
                sku=944500 + i,
                fesh=3250 + i,
                ts=7650 + i,
                price=1000,
                fee=i * 100,
                title='nokia#{}'.format(7650 + i),
                cpa=Offer.CPA_REAL,
            )
            for i in range(4, 7)
        ]

        for i in range(1, 11):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 7650 + i).respond(0.5 - i * 0.01)
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 944500 + i).respond(0.5 - i * 0.01)

    def test_sponsored_docs_with_regional_delimeter(self):
        # Проверяем как работает региональный разделитель для спонсорских доков набранных из премиальной
        rearr_flags_dict = {
            "market_report_mimicry_in_serp_pattern": 2,  # спонсорские позиции: [2, 3, 6, 7, 11, 12, ...]
            "market_buybox_auction_search_sponsored_places_web": 1,
            "market_premium_ads_in_search_sponsored_places_web": 0,
            "market_white_search_auction_cpa_fee": 1,
            "market_white_search_auction_cpa_fee_transfer_fee_do_desktop": 1,
        }

        request = (
            'place=prime&text=nokia&rids=300&pp=7&show-urls=external,decrypted,direct%2Ccpa&bsformat=2'
            '&use-default-offers=1&allow-collapsing=1&allow-ungrouping=1&waitall=da&numdoc=48&debug=1'
            '&rgb=green_with_blue&blender=1'
        )

        # случай когда региональный разделитель учитывает спонсорские оффера в выдаче
        rearr_flags_dict["market_regional_delimiter_ignore_sponsored_docs"] = 0
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(request + '&rearr-factors={}'.format(rearr_flags_str))

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"offers": {"items": [{"titles": {"raw": "nokia#7651"}}]}},
                    {"entity": "regionalDelimiter"},
                    {"offers": {"items": [{"titles": {"raw": "nokia#7654"}, "sponsored": True}]}},
                    {"offers": {"items": [{"titles": {"raw": "nokia#7655"}, "sponsored": True}]}},
                    {"offers": {"items": [{"titles": {"raw": "nokia#7652"}}]}},
                    {"offers": {"items": [{"titles": {"raw": "nokia#7653"}}]}},
                    {"offers": {"items": [{"titles": {"raw": "nokia#7656"}, "sponsored": True}]}},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # случай когда региональный разделитель не учитывает спонсорские оффера в выдаче
        rearr_flags_dict["market_regional_delimiter_ignore_sponsored_docs"] = 1
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(request + '&rearr-factors={}'.format(rearr_flags_str))

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"offers": {"items": [{"titles": {"raw": "nokia#7651"}}]}},
                    {"offers": {"items": [{"titles": {"raw": "nokia#7654"}, "sponsored": True}]}},
                    {"offers": {"items": [{"titles": {"raw": "nokia#7655"}, "sponsored": True}]}},
                    {"offers": {"items": [{"titles": {"raw": "nokia#7652"}}]}},
                    {"offers": {"items": [{"titles": {"raw": "nokia#7653"}}]}},
                    {"offers": {"items": [{"titles": {"raw": "nokia#7656"}, "sponsored": True}]}},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def __no_duplicates_in_shows_log(self, rearr_flags_dict):
        """
        Вспомогательная функция, чтобы проверить, что в шоулоге нет дублей спонсорских товаров
        """

        request = (
            'place=prime&text=nokia&pp=7&show-urls=external,decrypted,direct,promotion,cpa&bsformat=2'
            '&use-default-offers=1&allow-collapsing=1&allow-ungrouping=1&waitall=da&numdoc=48&debug=1'
            '&rgb=green_with_blue&blender=1&rids=300&'
            'viewtype=list&client=frontend&platform=desktop&rearr-factors='
        )
        # спонсорские позиции: [ 2, 3, 6, 7, 16, 17, 26, 27, 36, 37, 47, 48 ]
        rearr_flags_dict["market_report_mimicry_in_serp_pattern"] = 3
        response = self.report.request_json(request + dict_to_rearr(rearr_flags_dict))

        # Парсим из выдачи ware_md5 трафаретов
        sponsored_offers_count = 3
        organic_offers_count = 3
        sponsored_offers_positions = [2, 3, 6, 7, 16, 17, 26, 27, 36, 37, 47, 48]
        sponsored_offers = [Capture() for _ in range(sponsored_offers_count)]
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        # Трафареты
                        "offers": {
                            "items": [
                                {
                                    "sponsored": True,
                                    "wareId": NotEmpty(capture=capture),
                                },
                            ],
                        },
                    }
                    for capture in sponsored_offers
                ]
                + [
                    {
                        # Органика
                        "offers": {
                            "items": [
                                {
                                    "sponsored": Absent(),
                                },
                            ],
                        },
                    }
                    for _ in range(organic_offers_count)
                ]
            },
            # Важно, что спарсили все трафареты из выдачи
            allow_different_len=False,
        )

        # Проверяем, что дублей нет (проверяем оффеные и модельные ссылки)
        for offer_ware_md5, position in zip(sponsored_offers, sponsored_offers_positions):
            self.show_log.expect(url_type=6, pp=231, position=position, ware_md5=offer_ware_md5.value).once()
            self.show_log.expect(url_type=16, pp=231, position=position, ware_md5=offer_ware_md5.value).once()

    def test_no_duplicates_in_shows_log_ver3(self):
        """
        Проверяем, что в шоулоге нет дублей спонсорских товаров.
        Проверка для алгоритма всатвки трафаретов ver3
        (запрос трафаретов вместо с поисковыми ДО с помощью auction_extra).
        """
        rearr_flags_dict = {
            "market_buybox_auction_search_sponsored_places_web": 1,
            "market_premium_ads_in_search_sponsored_places_web": 0,
            "market_white_search_auction_cpa_fee": 1,
            "market_white_search_auction_cpa_fee_transfer_fee_do_desktop": 1,
        }
        self.__no_duplicates_in_shows_log(rearr_flags_dict)

    def test_no_duplicates_in_shows_log_from_cpa_shop_incut(self):
        """
        Проверяем, что в шоулоге нет дублей спонсорских товаров.
        Проверка для алгоритма вставки трафаретов, когда
        они запрашиваются из cpa_shop_incut.
        """
        rearr_flags_dict = {
            "market_buybox_auction_search_sponsored_places_web": 0,
            "market_premium_ads_in_search_sponsored_places_web": 1,
            "market_premium_ads_incut_get_docs_through_prime": 1,
            # TODO: убрать флаги ниже, когда они будут в проде по умолчанию
            "market_cpa_shop_inuct_enable_metadoc_search": 1,
            "market_cpa_shop_incut_use_vendor_and_msku_text_filters": 1,
            "market_enrich_sorting_params_in_cpa_shop_incut": 1,
            "market_premium_ads_in_search_sponsored_places_allow_duplicates": 0,
        }
        self.__no_duplicates_in_shows_log(rearr_flags_dict)


if __name__ == '__main__':
    main()
