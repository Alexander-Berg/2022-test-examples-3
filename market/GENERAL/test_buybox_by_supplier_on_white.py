#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.matcher import EqualToOneOf
from core.testcase import TestCase, main
from core.types import (
    BlueOffer,
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryOption,
    DynamicDeliveryServiceInfo,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseInfo,
    DynamicWarehouseToWarehouseInfo,
    DynamicWarehousesPriorityInRegion,
    GLParam,
    GLType,
    GLValue,
    MarketSku,
    Offer,
    RegionalDelivery,
    Shop,
)


USE_DEPRECATED_DIRECT_SHIPPING_FLOW = "&rearr-factors=get_rid_of_direct_shipping=0"


class T(TestCase):
    """
    https://st.yandex-team.ru/MARKETOUT-32799
    В рамках перехода на Маркет 4.0 тестируем как будут отображаться синие офферы на белом
    В отличие от синего маркета байбокс должен выбираться с учетом поставщика (т.е. по совокупности Мску+3P поставщик)
    Все синие офферы одной модели на КМ отображаются как офферы от Беру, но с указанием поставщика, и группируются по магазину Беру
    """

    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.shops += [
            Shop(
                fesh=1,
                datafeed_id=1,
                priority_region=213,
                name='ВиртуальныйМагазинНаБеру',
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                business_fesh=1,
            ),
            Shop(
                fesh=11,
                datafeed_id=11,
                priority_region=213,
                regions=[225],
                name="Один 1P поставщик",
                supplier_type=Shop.FIRST_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
                fulfillment_program=True,
                business_fesh=1,
            ),
            Shop(
                fesh=12,
                datafeed_id=12,
                priority_region=213,
                regions=[225],
                name="Другой 1P поставщик",
                supplier_type=Shop.FIRST_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
                fulfillment_program=True,
                business_fesh=1,
            ),
            Shop(
                fesh=31,
                datafeed_id=31,
                priority_region=213,
                regions=[225],
                name="3P поставщик Вася",
                supplier_type=Shop.THIRD_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
                fulfillment_program=True,
                business_fesh=2,
            ),
            Shop(
                fesh=32,
                datafeed_id=32,
                priority_region=213,
                regions=[225],
                name="3P поставщик Петя",
                supplier_type=Shop.THIRD_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
                fulfillment_program=True,
                business_fesh=3,
            ),
            Shop(
                fesh=111,
                business_fesh=3,
                datafeed_id=110,
                priority_region=213,
                regions=[225],
                name="простой белый магазин Пети",
            ),
            Shop(
                fesh=1110,
                business_fesh=3,
                datafeed_id=1100,
                priority_region=213,
                regions=[225],
                name="dsbs белый магазин Пети",
                cpa=Shop.CPA_REAL,
            ),
        ]

        cls.index.gltypes += [
            GLType(
                hid=1,
                param_id=202,
                gltype=GLType.ENUM,
                values=[
                    GLValue(value_id=1, text='value1'),
                    GLValue(value_id=2, text='value2'),
                ],
            ),
        ]

        cls.settings.lms_autogenerate = False

        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=145, home_region=213),
            DynamicWarehouseToWarehouseInfo(warehouse_from=145, warehouse_to=145),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=145,
                delivery_service_id=157,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=23, region_to=225)],
            ),
            DynamicDeliveryServiceInfo(id=157, rating=2),
            DynamicWarehousesPriorityInRegion(region=225, warehouses=[145]),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=1234,
                carriers=[157],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[
                    RegionalDelivery(rid=225, options=[DeliveryOption(price=45, day_from=3, day_to=5)]),
                ],
            )
        ]

        cls.index.mskus += [
            MarketSku(
                hyperid=1,
                hid=1,
                sku=1,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(
                        price=2100,
                        feedid=feedid,
                        waremd5='OFF1_2100_SKU1_SUP{}_Q'.format(feedid),
                        randx=100 * feedid,
                        business_id=feedid / 16 + 1,
                    )
                    for feedid in [11, 12, 31, 32]
                ]
                + [
                    BlueOffer(
                        price=2000,
                        feedid=feedid,
                        waremd5='OFF2_2000_SKU1_SUP{}_Q'.format(feedid),
                        randx=200 * feedid,
                        business_id=feedid / 16 + 1,
                    )
                    for feedid in [11, 12, 31, 32]
                ],
            ),
            MarketSku(
                hyperid=1,
                hid=1,
                sku=2,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(
                        price=2000,
                        feedid=feedid,
                        waremd5='OFF1_2000_SKU2_SUP{}_Q'.format(feedid),
                        randx=300 * feedid,
                        business_id=feedid / 16 + 1,
                        glparams=[GLParam(param_id=202, value=1)],
                    )
                    for feedid in [11, 12, 32]
                ]
                + [
                    BlueOffer(
                        price=1991,
                        feedid=feedid,
                        waremd5='OFF1_2000_SKU2_SUP{}_Q'.format(feedid),
                        randx=300 * feedid,
                        business_id=feedid / 16 + 1,
                        glparams=[GLParam(param_id=202, value=1)],
                    )
                    for feedid in [
                        31,
                    ]
                ]
                + [
                    BlueOffer(
                        price=2100,
                        feedid=feedid,
                        waremd5='OFF2_2100_SKU2_SUP{}_Q'.format(feedid),
                        randx=400 * feedid,
                        business_id=feedid / 16 + 1,
                        glparams=[GLParam(param_id=202, value=1)],
                    )
                    for feedid in [11, 12, 31, 32]
                ],
            ),
        ]

        cls.index.offers += [
            Offer(
                price=1500,
                feedid=110,
                fesh=111,
                waremd5='OFF_3000_HID1_WHITE__Q',
                hyperid=1,
                hid=1,
                randx=400 * feedid,
                business_id=3,
                glparams=[GLParam(param_id=202, value=2)],
            )
        ]
        cls.index.offers += [
            Offer(
                price=2500,
                feedid=1100,
                fesh=1110,
                waremd5='OFF_DSBS_HID1_WHITE__Q',
                hyperid=1,
                hid=1,
                randx=401 * feedid,
                business_id=3,
                cpa=Offer.CPA_REAL,
            )
        ]

    def test_prime(self):
        """Проверяем что выбирается 1 байбокс среди офферов от поставщиков 1P (11 и 12)
        И разные байбоксы для 3P поставщиков: 1 байбокс от поставщика Васи (31) и один от Пети (32)
        На прайме офферы от разных мску не группируются (итого 6 офферов: по 3 байбокса для 2х мску)"""

        # фиксируем yandexuid чтобы buybox-ы выигрывались всегда одним и тем же оффером
        response = self.report.request_json(
            'place=prime&hid=1&allow-collapsing=0&rids=213&yandexuid=1&debug=da'
            '&rearr-factors=market_buybox_by_supplier_on_white=1;market_debug_buybox=1;market_blue_buybox_with_delivery_context=1;market_metadoc_search=no'
            + USE_DEPRECATED_DIRECT_SHIPPING_FLOW
        )

        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 9,
                    'results': [
                        {'entity': 'product'},
                        {'entity': 'offer', 'wareId': 'OFF_3000_HID1_WHITE__Q'},
                        {'entity': 'offer', 'wareId': 'OFF_DSBS_HID1_WHITE__Q'},
                        {
                            'entity': 'offer',
                            'wareId': EqualToOneOf('OFF2_2000_SKU1_SUP11_Q', 'OFF2_2000_SKU1_SUP12_Q'),
                            'debug': {
                                # в выборе байбокса участвуют офферы msku=1 от обоих 1P поставщиков s11 и s12
                                'buyboxDebug': {
                                    'WonMethod': 'WON_BY_EXCHANGE',
                                    'Offers': [
                                        {'WareMd5': 'OFF2_2000_SKU1_SUP11_Q', 'SupplierId': 11},
                                        {'WareMd5': 'OFF2_2000_SKU1_SUP12_Q', 'SupplierId': 12},
                                        {'WareMd5': 'OFF1_2100_SKU1_SUP11_Q', 'SupplierId': 11},
                                        {'WareMd5': 'OFF1_2100_SKU1_SUP12_Q', 'SupplierId': 12},
                                    ],
                                }
                            },
                        },
                        {
                            'entity': 'offer',
                            'wareId': 'OFF2_2000_SKU1_SUP31_Q',
                            'debug': {
                                # в выборе байбокса участвуют офферы msku=1 от поставщика s31 - выигрывает более дешевый
                                'buyboxDebug': {
                                    'WonMethod': 'WON_BY_EXCHANGE',
                                    'Offers': [
                                        {
                                            'WareMd5': 'OFF2_2000_SKU1_SUP31_Q',
                                            'SupplierId': 31,
                                            'RelevanceData': {'Price': 2000},
                                        },
                                        {
                                            'WareMd5': 'OFF1_2100_SKU1_SUP31_Q',
                                            'SupplierId': 31,
                                            'RelevanceData': {'Price': 2100},
                                        },
                                    ],
                                }
                            },
                        },
                        {
                            'entity': 'offer',
                            'wareId': 'OFF2_2000_SKU1_SUP32_Q',
                            'debug': {
                                # в выборе байбокса участвуют офферы msku=1 от поставщика s32 - выигрывает более дешевый
                                'buyboxDebug': {
                                    'WonMethod': 'WON_BY_EXCHANGE',
                                    'Offers': [
                                        {
                                            'WareMd5': 'OFF2_2000_SKU1_SUP32_Q',
                                            'SupplierId': 32,
                                            'RelevanceData': {'Price': 2000},
                                        },
                                        {
                                            'WareMd5': 'OFF1_2100_SKU1_SUP32_Q',
                                            'SupplierId': 32,
                                            'RelevanceData': {'Price': 2100},
                                        },
                                    ],
                                }
                            },
                        },
                        # аналогично 3 оффера для msku=2: один от поставщика Васи (s31), один от поставщика Пети (s32) и один от 1P поставщиков
                        {'entity': 'offer', 'wareId': 'OFF1_2000_SKU2_SUP31_Q'},
                        {'entity': 'offer', 'wareId': 'OFF1_2000_SKU2_SUP32_Q'},
                        {'entity': 'offer', 'wareId': EqualToOneOf('OFF1_2000_SKU2_SUP11_Q', 'OFF1_2000_SKU2_SUP12_Q')},
                    ],
                }
            },
            allow_different_len=False,
            preserve_order=False,
        )

    def test_business_id(self):
        """Проверяем что соответствующий флаг включает фильтрацию по business_id (параметр fesh)"""

        # фиксируем yandexuid чтобы buybox-ы выигрывались всегда одним и тем же оффером
        response = self.report.request_json(
            'place=prime&hid=1&allow-collapsing=0&rids=213&yandexuid=1&debug=da&fesh=3'
            '&rearr-factors=enable_business_id=1'
        )

        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 4,
                    'results': [
                        {
                            'entity': 'offer',
                            'wareId': 'OFF2_2000_SKU1_SUP32_Q',
                            'shop': {'business_id': 1},
                            'supplier': {'business_id': 3},
                        },
                        {'entity': 'offer', 'wareId': 'OFF_3000_HID1_WHITE__Q'},
                        {'entity': 'offer', 'wareId': 'OFF_DSBS_HID1_WHITE__Q'},
                        {'entity': 'offer', 'wareId': 'OFF1_2000_SKU2_SUP32_Q'},
                    ],
                }
            },
        )

    def test_enable_business_id_for_shop_filter_on_prime(self):
        """На place=prime при включении этого флага в поле id должен быть значение business_fesh вместо fesh"""

        response = self.report.request_json(
            'place=prime&hyperid=1&rids=213&fesh=2' '&rearr-factors=enable_business_id_for_shop_filter_on_prime=1'
        )

        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 2,
                    'results': [
                        {
                            'entity': 'offer',
                            'wareId': 'OFF2_2000_SKU1_SUP31_Q',
                            'shop': {'business_id': 1},
                            'supplier': {'business_id': 2},
                        },
                        {
                            'entity': 'offer',
                            'wareId': 'OFF1_2000_SKU2_SUP31_Q',
                            'shop': {'business_id': 1},
                            'supplier': {'business_id': 2},
                        },
                    ],
                }
            },
            allow_different_len=False,
        )

        self.assertFragmentIn(
            response,
            {
                'id': 'fesh',
                'values': [
                    {'id': '1', "value": "ВиртуальныйМагазинНаБеру"},
                    {'id': '2', "value": "3P поставщик Вася"},
                    {'id': '3', 'value': '3P поставщик Петя'},
                ],
            },
        )

    def test_force_business_id(self):
        """На place=prime при включении этого флага исчезают задизейбленные магазинным фильтры"""

        response = self.report.request_json(
            'place=prime&debug=1&rids=213&hid=1&fesh=2&rearr-factors=market_force_business_id=1'
        )
        self.assertFragmentNotIn(response, {'filters': [{'id': '202', 'values': [{'id': '2', 'found': 0}]}]})

        response = self.report.request_json(
            'place=prime&debug=1&rids=213&hid=1&fesh=2&rearr-factors=market_force_business_id=0'
        )
        self.assertFragmentIn(response, {'filters': [{'id': '202', 'values': [{'id': '2', 'found': 0}]}]})

        response = self.report.request_json(
            'place=prime&debug=1&rids=213&hid=1&fesh=2&market-force-business-id=1&rearr-factors=enable_business_id=0'
        )
        self.assertFragmentNotIn(response, {'filters': [{'id': '202', 'values': [{'id': '2', 'found': 0}]}]})

        response = self.report.request_json(
            'place=prime&debug=1&rids=213&hid=1&fesh=2&market-force-business-id=0&rearr-factors=enable_business_id=0'
        )
        self.assertFragmentIn(response, {'filters': [{'id': '202', 'values': [{'id': '2', 'found': 0}]}]})

        """Еще и все cpc фильтруются"""
        response = self.report.request_json(
            'place=productoffers&hyperid=1&market-force-business-id=1&rearr-factors=enable_business_id=0'
        )
        self.assertFragmentIn(
            response, {'filters': [{'id': 'fesh', 'values': [{'id': '1110', 'found': 1}, {'id': '1', 'found': 6}]}]}
        )
        self.assertFragmentNotIn(response, {'filters': [{'id': 'fesh', 'values': [{'id': '111'}]}]})

        """market-force-business отличает запрос от skk и дает схлопываться"""
        response = self.report.request_json('place=prime&rids=213&fesh=2&market-force-business-id=1&allow-collapsing=1')
        self.assertFragmentIn(response, {'entity': 'product'})

    def test_productoffers(self):
        """На place=productoffers все офферы одной модели от Беру группируются в один сниппет с указанием "ещё N предложений" """

        response = self.report.request_json(
            'place=productoffers&hyperid=1&grhow=shop&rids=213'
            '&rearr-factors=market_buybox_by_supplier_on_white=1&fesh=1'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [{'entity': 'offer', 'shop': {'name': 'ВиртуальныйМагазинНаБеру'}, 'bundleCount': 6}]
                }
            },
            allow_different_len=False,
        )

    def test_force_business_cpa(self):
        """На place=productoffers все офферы одной модели от Беру группируются в один сниппет с указанием "ещё N предложений" """

        response = self.report.request_json(
            'place=productoffers&hyperid=1&grhow=shop&rids=213'
            '&rearr-factors=market_buybox_by_supplier_on_white=1&fesh=1'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [{'entity': 'offer', 'shop': {'name': 'ВиртуальныйМагазинНаБеру'}, 'bundleCount': 6}]
                }
            },
            allow_different_len=False,
        )

    def test_disable_uncollapsing_do(self):
        """Байбокс фильтруется по цене"""
        response = self.report.request_json(
            'place=productoffers&hyperid=1&grhow=shop&rids=213&market-sku=2&offers-set=defaultList'
            '&fesh=1&mcpricefrom=1985&rearr-factors=use_offer_type_priority_as_main_factor_in_top=0'
        )
        self.assertFragmentNotIn(
            response, {'search': {'results': [{'entity': 'offer', 'wareId': 'OFF1_2000_SKU2_SUP32_Q'}]}}
        )

        """Байбокс расхлопнут, появляются дорогие офферы """
        response = self.report.request_json(
            'place=productoffers&hyperid=1&grhow=shop&rids=213&market-sku=2&offers-set=defaultList'
            '&rearr-factors=market_uncollapse_supplier=0;market_buybox_by_supplier_on_white=1&fesh=1&mcpricefrom=1985'
        )
        self.assertFragmentIn(
            response,
            {'search': {'results': [{'entity': 'offer', 'wareId': 'OFF1_2000_SKU2_SUP32_Q'}]}},
            allow_different_len=False,
        )

        """Байбокс снова есть, расхлопывание заблокировано """
        response = self.report.request_json(
            'place=productoffers&hyperid=1&grhow=shop&rids=213&market-sku=2&offers-set=defaultList'
            '&rearr-factors=market_uncollapse_supplier=1&fesh=1&mcpricefrom=1985'
        )
        self.assertFragmentNotIn(
            response, {'search': {'results': [{'entity': 'offer', 'wareId': 'OFF1_2000_SKU2_SUP32_Q'}]}}
        )

    def test_at_beru_warehouse(self):
        USE_DEPRECATED_DIRECT_SHIPPING_FLOW = "&rearr-factors=get_rid_of_direct_shipping=0"
        response = self.report.request_json('place=productoffers&hyperid=1&hid=1')
        self.assertFragmentIn(response, {'search': {'total': 8}})

        """А теперь оставляем только склад маркета"""
        response = self.report.request_json(
            'place=productoffers&hyperid=1&hid=1&at-beru-warehouse=1' + USE_DEPRECATED_DIRECT_SHIPPING_FLOW
        )
        self.assertFragmentIn(response, {'search': {'total': 6}})

    def test_productoffers_uncollapsed_supplier_business_id(self):
        """На place=productoffers все офферы одной модели от Беру группируются в один сниппет с указанием "ещё N предложений" """

        def test_body(changing_group_flag):
            response = self.report.request_json(
                'place=productoffers&hyperid=1'
                '&grhow=supplier&rids=213&rearr-factors=market_blue_buybox_disable_old_buybox_algo=0;use_offer_type_priority_as_main_factor_in_top=0;market_uncollapse_supplier=1;enable_business_id=1;market_enable_buybox_by_business=0&debug=1'  # noqa
            )
            # 4 оффера: белый, один от поставщика Васи (s31), один от поставщика Пети (s32) и один от 1P поставщиков
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'total': 4,
                        'results': [
                            {'entity': 'offer', 'wareId': 'OFF_DSBS_HID1_WHITE__Q'},
                            {'entity': 'offer', 'wareId': 'OFF_3000_HID1_WHITE__Q'},
                            {'entity': 'offer', 'wareId': 'OFF1_2000_SKU2_SUP31_Q'},
                            {'entity': 'offer', 'wareId': 'OFF1_2000_SKU2_SUP11_Q'},
                        ],
                    }
                },
                allow_different_len=False,
            )

            # 3 магазинa: белый, дсбс, один от поставщика Васи (s31), один от поставщика Пети (s32) и один от 1P поставщиков
            self.assertFragmentIn(
                response,
                {
                    'id': 'fesh',
                    'values': [
                        {'id': '1', 'found': 2},
                        {'id': '2', 'found': 2},
                        {'id': '3', 'found': 4},
                    ],
                },
                allow_different_len=False,
            )

            # фильтр по магазину
            response = self.report.request_json(
                'place=productoffers&hyperid=1&fesh=2'
                '&grhow=supplier&rids=213&rearr-factors=market_blue_buybox_disable_old_buybox_algo=0;use_offer_type_priority_as_main_factor_in_top=0;market_uncollapse_supplier=1;enable_business_id=1;{flag}&debug=1'.format(  # noqa
                    flag=changing_group_flag
                )
            )
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'total': 1,
                        'results': [{'entity': 'offer', 'wareId': 'OFF1_2000_SKU2_SUP31_Q'}],
                    }
                },
                allow_different_len=False,
            )

            response = self.report.request_json(
                'place=productoffers&hyperid=1&fesh=2'
                '&grhow=offer&rids=213&rearr-factors=market_blue_buybox_disable_old_buybox_algo=0;use_offer_type_priority_as_main_factor_in_top=0;market_uncollapse_supplier=1;enable_business_id=1;{flag}&debug=1'.format(  # noqa
                    flag=changing_group_flag
                )
            )
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'total': 2,
                        'results': [
                            {'entity': 'offer', 'wareId': 'OFF1_2000_SKU2_SUP31_Q'},
                            {'entity': 'offer', 'wareId': 'OFF2_2000_SKU1_SUP31_Q'},
                        ],
                    }
                },
                allow_different_len=False,
            )


if __name__ == '__main__':
    main()
