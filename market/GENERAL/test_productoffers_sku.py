#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.types import (
    BlueOffer,
    Currency,
    DateSwitchTimeAndRegionInfo,
    DeliveryServiceRegionToRegionInfo,
    DynamicDaysSet,
    DynamicDeliveryServiceInfo,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseInfo,
    GLParam,
    GLType,
    GLValue,
    GpsCoord,
    ImagePickerData,
    MarketSku,
    MnPlace,
    Model,
    ModelGroup,
    Offer,
    Outlet,
    OutletDeliveryOption,
    ParameterValue,
    Shop,
    Tax,
    TimeInfo,
    NavCategory,
)

from core.testcase import TestCase, main
from core.matcher import Absent, EqualToOneOf, NotEmpty


class T(TestCase):
    @classmethod
    def prepare_none_sku_offers(cls):
        cls.settings.default_search_experiment_flags += ["market_money_disable_bids=0"]
        cls.index.gltypes += [
            GLType(
                hid=101,
                param_id=212,
                cluster_filter=True,
                gltype=GLType.ENUM,
                values=[1, 2, 3, 4, 5],
                model_filter_index=1,
            ),
            GLType(hid=101, param_id=213, cluster_filter=True, gltype=GLType.NUMERIC, model_filter_index=2),
            GLType(hid=101, param_id=214, cluster_filter=True, gltype=GLType.BOOL, model_filter_index=3),
        ]

        cls.index.mskus += [
            MarketSku(
                hid=101,
                hyperid=105,
                sku=103,
                title="MSKU-103",
                blue_offers=[BlueOffer(ts=10, price=3)],
                glparams=[
                    GLParam(param_id=212, value=3),
                    GLParam(param_id=213, value=5),
                    GLParam(param_id=214, value=0),
                ],
            ),
        ]

        cls.index.models += [
            Model(hid=101, hyperid=105, title="Model with msku"),
        ]

        cls.index.offers += [
            Offer(
                sku=103,
                hid=101,
                hyperid=105,
                price=333,
                title="Offer msku=103",
                waremd5='YqQnWXU28yTTghltMZJwNT',
                glparams=[
                    GLParam(param_id=212, value=3),
                    GLParam(param_id=213, value=5),
                    GLParam(param_id=214, value=0),
                ],
            ),
            # Offers without sku, with model=105 & category = 101
            Offer(
                hyperid=105,
                hid=101,
                price=1000,
                title="Offer without sku",
                glparams=[
                    GLParam(param_id=212, value=1),
                    GLParam(param_id=213, value=25),
                    GLParam(param_id=214, value=1),
                ],
            ),
            Offer(
                hyperid=105,
                hid=101,
                price=2000,
                title="Offer without sku 2",
                glparams=[
                    GLParam(param_id=212, value=5),
                    GLParam(param_id=213, value=15),
                    GLParam(param_id=214, value=0),
                ],
            ),
        ]

    def test_none_sku_offers(self):
        """
        Проверяем, что place=productoffers корректно обрабатывает модель, у которой у default offer sku=0.
        Т.е. все офферы, которые не имеют выставленной sku
        """

        request = 'place=productoffers&hyperid=105&hid=101&debug=1&onstock=0'
        with_market_sku = '&market-sku=0'

        # В случае запроса с sku=0 в выдачу попадают все офферы без указанного sku
        # Таблица переходов не формируется (т.е. поле marketSku в фильтрах отсутствует)
        # skuKadaver заполняется только при наличии флага generate_kadavers
        for generate_kadavers in (True, False):
            rearr = (
                '&rearr-factors=generate_kadavers=1;market_white_ungrouping=1;enable_business_id=0'
                if generate_kadavers
                else '&rearr-factors=enable_business_id=0'
            )
            response = self.report.request_json(request + with_market_sku + rearr)
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'results': [
                            {
                                'entity': 'offer',
                                'sku': Absent(),
                                'skuKadaver': True if generate_kadavers else Absent(),
                                'titles': {'raw': 'Offer without sku'},
                                "offerColor": "white",
                            },
                            {
                                'entity': 'offer',
                                'sku': Absent(),
                                'skuKadaver': True if generate_kadavers else Absent(),
                                'titles': {'raw': 'Offer without sku 2'},
                                "offerColor": "white",
                            },
                        ]
                    }
                },
            )

            # Перечислим все фильтры, чтобы удостовериться, что других нет - т.е. таблицы переходов нет
            self.assertFragmentIn(
                response,
                {
                    "filters": [
                        {
                            "id": "glprice",
                            "isGuruLight": Absent(),
                            "type": "number",
                            "values": [
                                {
                                    "id": "found",
                                    "initialMax": "2000",
                                    "initialMin": "1000",
                                    "max": "2000",
                                    "min": "1000",
                                }
                            ],
                        },
                        {
                            "id": "fesh",
                            "valuesCount": 2,
                        },
                        {
                            "id": "at-beru-warehouse",
                        },
                        {
                            "id": "with-yandex-delivery",
                        },
                        {
                            "id": "manufacturer_warranty",
                        },
                        {
                            "id": "qrfrom",
                        },
                        {
                            "id": "offer-shipping",
                        },
                        {"id": "212", "isGuruLight": True, "noffers": 2, "marketSku": Absent()},
                        {"id": "213", "isGuruLight": True, "noffers": 2, "marketSku": Absent()},
                        {"id": "214", "isGuruLight": True, "noffers": 2, "marketSku": Absent()},
                    ]
                },
                allow_different_len=False,
            )

            # В случае запроса без указанного sku в выдачу попадают все офферы данной модели
            # Таблица переходов не формируется (т.е. поле marketSku в фильтрах отсутствует)
            response = self.report.request_json(request + rearr)
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'results': [
                            {
                                'entity': 'offer',
                                'sku': Absent(),
                                'skuKadaver': True if generate_kadavers else Absent(),
                                'titles': {'raw': 'Offer without sku'},
                                "offerColor": "white",
                            },
                            {
                                'entity': 'offer',
                                'sku': '103',
                                'skuKadaver': False if generate_kadavers else Absent(),
                                'titles': {'raw': 'Offer msku=103'},
                                "offerColor": "white",
                            },
                            {
                                'entity': 'offer',
                                'sku': '103',
                                'skuKadaver': False if generate_kadavers else Absent(),
                                'titles': {'raw': 'MSKU-103'},
                                "offerColor": "blue",
                            },
                            {
                                'entity': 'offer',
                                'sku': Absent(),
                                'skuKadaver': True if generate_kadavers else Absent(),
                                'titles': {'raw': 'Offer without sku 2'},
                                "offerColor": "white",
                            },
                        ]
                    }
                },
            )

            # Перечислим все фильтры, чтобы удостовериться, что других нет - т.е. таблицы переходов нет
            self.assertFragmentIn(
                response,
                {
                    "filters": [
                        {
                            "id": "glprice",
                            "isGuruLight": Absent(),
                            "type": "number",
                            "values": [
                                {"id": "found", "initialMax": "2000", "initialMin": "3", "max": "2000", "min": "3"}
                            ],
                        },
                        {
                            "id": "fesh",
                        },
                        {
                            "id": "at-beru-warehouse",
                        },
                        {
                            "id": "with-yandex-delivery",
                        },
                        {
                            "id": "cpa",
                        },
                        {
                            "id": "manufacturer_warranty",
                        },
                        {
                            "id": "qrfrom",
                        },
                        {
                            "id": "offer-shipping",
                        },
                        {"id": "212", "isGuruLight": True, "noffers": 4, "marketSku": Absent()},
                        {"id": "213", "isGuruLight": True, "noffers": 4, "marketSku": Absent()},
                        {"id": "214", "isGuruLight": True, "noffers": 4, "marketSku": Absent()},
                    ]
                },
                allow_different_len=False,
            )

    @classmethod
    def prepare_filter_by_sku(cls):
        cls.index.mskus += [
            MarketSku(
                hid=1, hyperid=1, sku=1001, blue_offers=[BlueOffer(waremd5='_qQnWXU28-IUghltMZJwNw', ts=1, price=200)]
            ),
            MarketSku(
                hid=1, hyperid=1, sku=1002, blue_offers=[BlueOffer(waremd5='RPaDqEFjs1I6_lfC4Ai8jA', ts=2, price=200)]
            ),
        ]

        cls.index.offers += [
            Offer(hid=1, hyperid=1, price=110, waremd5='N22LX0LbSSgnAWRIl1zCLQ', ts=10),
            Offer(hid=1, hyperid=1, price=120, sku=1001, waremd5='Nq2LX0LbSSgnAWRIl1zCLQ', ts=3),
            Offer(hid=1, hyperid=1, sku=1001, waremd5='AnCWAPYMxfcz_Yeb6J9LOw', ts=4, bid=50),
            Offer(hid=1, hyperid=1, sku=1001, waremd5='fzZygbupG9aC1hNSKmeUmg', ts=5),
            Offer(hid=1, hyperid=1, price=130, sku=1002, waremd5='_dWxb8gTQzkInvJbPPDliA', ts=6),
            Offer(hid=1, hyperid=1, sku=1002, waremd5='-wOg8vXZSvSlwIkP_w724A', ts=7, bid=50),
            Offer(hid=1, hyperid=1, sku=1002, waremd5='_f9x_Ktlar---qvsqxp_bQ', ts=8),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1).respond(0.4)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2).respond(0.3)

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 4).respond(0.8)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 5).respond(0.7)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 6).respond(0.6)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 7).respond(0.5)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 8).respond(0.4)

    def test_filter_by_sku(self):
        """
        Проверяем, что place=productoffers фильтрует по cgi-параметру &market-sku
        """

        response = self.report.request_json(
            'place=productoffers&hyperid=1&offers-set=defaultList,top&hid=1'
            '&show-urls=beruOrder&rearr-factors=use_offer_type_priority_as_main_factor_in_top=0;market_cpa_default_offer=1;market_cpc_default_offer=1;'
            'market_ranging_cpa_by_ue_in_top_cpa_multiplier=1;market_premium_offer_logic=add-and-mark-touch'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {'wareId': 'N22LX0LbSSgnAWRIl1zCLQ'},
                        {'wareId': 'Nq2LX0LbSSgnAWRIl1zCLQ'},
                        {'wareId': 'AnCWAPYMxfcz_Yeb6J9LOw'},
                        {'wareId': 'fzZygbupG9aC1hNSKmeUmg'},
                        {'wareId': '_dWxb8gTQzkInvJbPPDliA'},
                        {'wareId': '-wOg8vXZSvSlwIkP_w724A'},
                        {'wareId': '_f9x_Ktlar---qvsqxp_bQ'},
                        {'wareId': '_qQnWXU28-IUghltMZJwNw'},
                        {'wareId': 'RPaDqEFjs1I6_lfC4Ai8jA'},
                        # default offers
                        {
                            'benefit': {'type': 'cheapest'},
                            'wareId': '_qQnWXU28-IUghltMZJwNw',
                        },
                        {
                            'benefit': {'type': 'default-cpc'},
                            'wareId': 'Nq2LX0LbSSgnAWRIl1zCLQ',
                        },
                        {
                            'benefit': {'type': 'default-cpa'},
                            'wareId': '_qQnWXU28-IUghltMZJwNw',
                        },
                        # premium offer
                        {
                            'benefit': {'type': 'premium'},
                            'wareId': 'AnCWAPYMxfcz_Yeb6J9LOw',
                        },
                    ]
                }
            },
            allow_different_len=False,
        )

        sku_request = (
            'place=productoffers&market-sku=1002&offers-set=defaultList,top&hid=1'
            '&show-urls=beruOrder&rearr-factors=use_offer_type_priority_as_main_factor_in_top=0;market_cpa_default_offer=1;market_cpc_default_offer=1;'
            'market_ranging_cpa_by_ue_in_top_cpa_multiplier=1;market_premium_offer_logic=add-and-mark-touch'
        )

        for request in (
            sku_request,
            sku_request + '&hyperid=1',
        ):
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'results': [
                            {'wareId': '_dWxb8gTQzkInvJbPPDliA'},
                            {'wareId': '-wOg8vXZSvSlwIkP_w724A'},
                            {'wareId': '_f9x_Ktlar---qvsqxp_bQ'},
                            {'wareId': 'RPaDqEFjs1I6_lfC4Ai8jA'},
                            # default offers
                            {
                                'benefit': {'type': 'cheapest'},
                                'wareId': 'RPaDqEFjs1I6_lfC4Ai8jA',
                            },
                            {
                                'benefit': {'type': 'default-cpc'},
                                'wareId': '_dWxb8gTQzkInvJbPPDliA',
                            },
                            {
                                'benefit': {'type': 'default-cpa'},
                                'wareId': 'RPaDqEFjs1I6_lfC4Ai8jA',
                            },
                            # premium offer
                            {
                                'benefit': {'type': 'premium'},
                                'wareId': '-wOg8vXZSvSlwIkP_w724A',
                            },
                        ]
                    }
                },
                allow_different_len=False,
            )

    @classmethod
    def prepare_jump_table(cls):
        cls.index.gltypes += [
            GLType(hid=2, param_id=201, cluster_filter=False, positionless=True),
            GLType(
                hid=2,
                param_id=202,
                cluster_filter=True,
                gltype=GLType.ENUM,
                values=[1, 2, 3, 4, 5, 6, 7],
                model_filter_index=1,
                unit_name='л',
                position=10,
            ),
            GLType(
                hid=2,
                param_id=203,
                cluster_filter=True,
                gltype=GLType.NUMERIC,
                model_filter_index=2,
                unit_name='кг',
                position=20,
                precision=3,
            ),
            GLType(
                hid=2, param_id=204, cluster_filter=True, gltype=GLType.BOOL, model_filter_index=3, positionless=True
            ),
            GLType(
                hid=2, param_id=205, cluster_filter=True, gltype=GLType.BOOL, model_filter_index=4, positionless=True
            ),
        ]

        cls.index.outlets += [
            Outlet(
                point_id=2001,
                delivery_service_id=103,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(shipper_id=103, day_from=1, day_to=1, order_before=2, price=10),
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(37.12, 55.32),
            ),
            Outlet(
                point_id=2002,
                delivery_service_id=104,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(shipper_id=103, day_from=1, day_to=1, order_before=2, price=10),
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(37.12, 55.32),
            ),
        ]

        cls.index.shops += [
            Shop(
                fesh=11,
                datafeed_id=1,
                priority_region=213,
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                fulfillment_virtual=True,
                delivery_service_outlets=[2001],
                cpa=Shop.CPA_REAL,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                work_schedule='virtual shop work schedule',
            ),
            Shop(
                fesh=201,
                datafeed_id=2001,
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
                fesh=301,
                datafeed_id=3001,
                priority_region=213,
                regions=[225],
                name="3P поставщик Вася",
                supplier_type=Shop.THIRD_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=146,
                fulfillment_program=True,
                business_fesh=2,
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                hid=2,
                hyperid=2,
                sku=2001,
                title="Игрушка 1",
                fesh=11,
                blue_offers=[
                    BlueOffer(ts=9, price=200),
                    BlueOffer(price=300, feedid=3001, business_id=2, is_express=True),
                ],
                glparams=[
                    GLParam(param_id=202, value=1),
                    GLParam(param_id=203, value=15),
                    GLParam(param_id=204, value=0),
                ],
            ),
            MarketSku(
                hid=2,
                hyperid=2,
                sku=2002,
                title="Игрушка 2",
                fesh=11,
                blue_offers=[
                    BlueOffer(ts=10, price=100),
                    BlueOffer(price=200, feedid=3001, business_id=2, is_express=True),
                ],
                glparams=[
                    GLParam(param_id=202, value=1),
                    GLParam(param_id=203, value=16),
                    GLParam(param_id=204, value=0),
                ],
            ),
            MarketSku(
                hid=2,
                hyperid=2,
                sku=2003,
                title="Игрушка 3",
                fesh=11,
                blue_offers=[
                    BlueOffer(ts=11, price=200),
                    BlueOffer(price=300, feedid=3001, business_id=2, is_express=True),
                ],
                glparams=[
                    GLParam(param_id=202, value=2),
                    GLParam(param_id=203, value=15),
                    GLParam(param_id=204, value=1),
                ],
            ),
            # no offers
            MarketSku(
                hid=2,
                hyperid=2,
                sku=2004,
                title="Игрушка 4",
                glparams=[
                    GLParam(param_id=202, value=2),
                    GLParam(param_id=203, value=15),
                    GLParam(param_id=204, value=1),
                ],
            ),
            # white offers only
            MarketSku(
                hid=2,
                hyperid=2,
                sku=2005,
                fesh=11,
                title="Игрушка 5",
                glparams=[
                    GLParam(param_id=202, value=3),
                    GLParam(param_id=203, value=16),
                    GLParam(param_id=204, value=1),
                ],
            ),
            # no offers msku, filtered in basic response, remains with use-skus-in-jump-table param
            MarketSku(
                hid=2,
                hyperid=2,
                sku=2006,
                title="Игрушка 6",
                glparams=[
                    GLParam(param_id=202, value=6),
                    GLParam(param_id=203, value=17),
                    GLParam(param_id=204, value=0),
                ],
            ),
            # no offers msku, filtered in basic response, remains with use-skus-in-jump-table param
            MarketSku(
                hid=2,
                hyperid=2,
                sku=2007,
                title="Игрушка 7",
                glparams=[
                    GLParam(param_id=202, value=7),
                    GLParam(param_id=203, value=17),
                    GLParam(param_id=204, value=0),
                ],
            ),
        ]

        cls.index.offers += [
            Offer(
                sku=2005,
                fesh=1,
                hyperid=2,
                price=100,
                glparams=[
                    GLParam(param_id=202, value=3),
                    GLParam(param_id=203, value=16),
                    GLParam(param_id=204, value=1),
                ],
            ),
            Offer(hyperid=2),
            Offer(hyperid=2, glparams=[GLParam(param_id=202, value=4)]),
        ]

        cls.settings.lms_autogenerate = False

        cls.dynamic.lms += [DynamicDaysSet(key=1, days=[])]

        cls.dynamic.lms += [
            DynamicDeliveryServiceInfo(
                id=103,
                name='ds_name',
                region_to_region_info=[DeliveryServiceRegionToRegionInfo(region_from=213, region_to=225, days_key=1)],
            ),
            DynamicDeliveryServiceInfo(
                id=104,
                name='expressDeliveryService',
                region_to_region_info=[DeliveryServiceRegionToRegionInfo(region_from=213, region_to=225, days_key=1)],
            ),
        ]

        cls.dynamic.lms += [
            DynamicWarehouseInfo(
                id=145,
                home_region=213,
            ),
            DynamicWarehouseInfo(
                id=146,
                home_region=213,
                is_express=True,
            ),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=145,
                delivery_service_id=103,
                operation_time=0,
                date_switch_time_infos=[
                    DateSwitchTimeAndRegionInfo(
                        date_switch_hour=2,
                        region_to=225,
                        date_switch_time=TimeInfo(19, 0),
                        packaging_time=TimeInfo(3, 30),
                    )
                ],
            ),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=146,
                delivery_service_id=104,
                operation_time=0,
                date_switch_time_infos=[
                    DateSwitchTimeAndRegionInfo(
                        date_switch_hour=2,
                        region_to=225,
                        date_switch_time=TimeInfo(19, 0),
                        packaging_time=TimeInfo(3, 30),
                    )
                ],
            ),
        ]

    def test_adding_no_offer_msku_to_jump_table(self):
        """
        Проверяем, что плейс productoffers возвращает msku из use-in-jump-table параметра когда для этой msku нет оффера
        """
        request = 'place=productoffers&hyperid=2&hid=2&platform=touch'
        with_param = '&use-skus-in-jump-table=2006,2007'
        response = self.report.request_json(request)
        self.assertFragmentNotIn(response, {"marketSku": "2006"})
        self.assertFragmentNotIn(response, {"marketSku": "2007"})
        response = self.report.request_json(request + with_param)
        self.assertFragmentIn(response, {"marketSku": "2006"})
        self.assertFragmentIn(response, {"marketSku": "2007"})

    def test_jump_table(self):
        """
        Проверяем, что в плейсе productoffers фильтры привязываются к ску
        Проверяем, что работают и старый, и новый пайплайны
        Удостоверяемся, что без market-sku таблица тоже формируется
        """

        request_no_msku = 'place=productoffers&hyperid=2&hid=2&debug=da&onstock=0&platform=touch&'

        request = request_no_msku + '&market-sku=2001'

        old_pipeline_flag = '&rearr-factors=use_new_jump_table_pipeline=0'
        for old_pipeline in (False, True):
            response = self.report.request_json(request + (old_pipeline_flag if old_pipeline else ''))
            # Для полей position и precision есть разница в старом и новом пайплайне:
            # - в старом пайплайне значения position берутся не из мбо
            # - в новом у precision есть значение по-умолчанию
            self.assertFragmentIn(
                response,
                {
                    "filters": [
                        {
                            "id": "glprice",
                            "isGuruLight": Absent(),
                        },
                        {
                            "id": "202",
                            "isGuruLight": True,
                            "type": "enum",
                            "unit": "л",
                            "position": NotEmpty() if old_pipeline else 10,
                            "precision": Absent() if old_pipeline else 0,
                            "values": [
                                {"id": "1", "marketSku": "2001", "checked": True, "fuzzy": Absent(), "found": 1},
                                {"id": "2", "marketSku": "2003", "checked": Absent(), "fuzzy": True, "found": 1},
                                {"id": "3", "marketSku": "2005", "checked": Absent(), "fuzzy": True, "found": 1},
                            ],
                        },
                        {
                            "id": "203",
                            "isGuruLight": True,
                            "type": "enum",
                            "unit": "кг",
                            "position": NotEmpty() if old_pipeline else 20,
                            "precision": 3,
                            "values": [
                                {
                                    "id": "15~15",
                                    "value": "15",
                                    "marketSku": "2001",
                                    "checked": True,
                                    "fuzzy": Absent(),
                                    "found": 1,
                                },
                                {
                                    "id": "16~16",
                                    "value": "16",
                                    "marketSku": "2002",
                                    "checked": Absent(),
                                    "fuzzy": Absent(),
                                    "found": 1,
                                },
                            ],
                        },
                        {
                            "id": "204",
                            "isGuruLight": True,
                            "type": "enum",
                            "unit": Absent(),
                            "position": NotEmpty() if old_pipeline else Absent(),
                            "precision": Absent() if old_pipeline else 0,
                            "values": [
                                {"id": "1", "marketSku": "2003", "checked": Absent(), "fuzzy": True, "found": 1},
                                {"id": "0", "marketSku": "2001", "checked": True, "fuzzy": Absent(), "found": 1},
                            ],
                        },
                    ]
                },
            )

            self.assertFragmentNotIn(response, {"filters": [{"id": "201"}]})
            self.assertFragmentNotIn(response, {"filters": [{"id": "202", "values": [{"id": "4"}]}]})
            self.assertFragmentNotIn(response, {"filters": [{"id": "205"}]})

            response = self.report.request_json(request_no_msku + old_pipeline_flag)
            self.assertFragmentIn(
                response,
                {
                    "filters": [
                        {
                            "id": "glprice",
                            "isGuruLight": Absent(),
                        },
                        {
                            "id": "202",
                            "isGuruLight": True,
                            "type": "enum",
                            "values": [
                                {
                                    "id": "1",
                                    "marketSku": EqualToOneOf("2001", "2002"),
                                    "checked": Absent(),
                                    "fuzzy": Absent(),
                                    "found": 1,
                                },
                                {
                                    "id": "2",
                                    "marketSku": EqualToOneOf("2003", "2004"),
                                    "checked": Absent(),
                                    "fuzzy": Absent(),
                                    "found": 1,
                                },
                                {
                                    "id": "3",
                                    "marketSku": "2005",
                                    "checked": Absent(),
                                    "fuzzy": Absent(),
                                    "found": 1,
                                },
                            ],
                        },
                        {
                            "id": "203",
                            "isGuruLight": True,
                            "type": "enum",
                            "values": [
                                {
                                    "id": "15~15",
                                    "value": "15",
                                    "marketSku": EqualToOneOf("2001", "2003", "2004"),
                                    "checked": Absent(),
                                    "fuzzy": Absent(),
                                    "found": 1,
                                },
                                {
                                    "id": "16~16",
                                    "value": "16",
                                    "marketSku": EqualToOneOf("2002", "2005"),
                                    "checked": Absent(),
                                    "fuzzy": Absent(),
                                    "found": 1,
                                },
                            ],
                        },
                        {
                            "id": "204",
                            "isGuruLight": True,
                            "type": "enum",
                            "values": [
                                {
                                    "id": "1",
                                    "marketSku": EqualToOneOf("2003", "2004", "2005"),
                                    "checked": Absent(),
                                    "fuzzy": Absent(),
                                    "found": 1,
                                },
                                {
                                    "id": "0",
                                    "marketSku": EqualToOneOf("2001", "2002"),
                                    "checked": Absent(),
                                    "fuzzy": Absent(),
                                    "found": 1,
                                },
                            ],
                        },
                    ]
                },
            )

            response = self.report.request_json(request + '&fesh=11' + old_pipeline_flag)
            self.assertFragmentIn(
                response,
                {
                    "filters": [
                        {
                            "id": "glprice",
                            "isGuruLight": Absent(),
                        },
                        {
                            "id": "202",
                            "isGuruLight": True,
                            "type": "enum",
                            "values": [
                                {"id": "1", "marketSku": "2001", "checked": True, "fuzzy": Absent(), "found": 1},
                                {"id": "2", "marketSku": "2003", "checked": Absent(), "fuzzy": True, "found": 1},
                            ],
                        },
                        {
                            "id": "203",
                            "isGuruLight": True,
                            "type": "enum",
                            "values": [
                                {
                                    "id": "15~15",
                                    "value": "15",
                                    "marketSku": "2001",
                                    "checked": True,
                                    "fuzzy": Absent(),
                                    "found": 1,
                                },
                                {
                                    "id": "16~16",
                                    "value": "16",
                                    "marketSku": "2002",
                                    "checked": Absent(),
                                    "fuzzy": Absent(),
                                    "found": 1,
                                },
                            ],
                        },
                        {
                            "id": "204",
                            "isGuruLight": True,
                            "type": "enum",
                            "values": [
                                {"id": "1", "marketSku": "2003", "checked": Absent(), "fuzzy": True, "found": 1},
                                {"id": "0", "marketSku": "2001", "checked": True, "fuzzy": Absent(), "found": 1},
                            ],
                        },
                    ]
                },
            )

            self.assertFragmentNotIn(response, {"filters": [{"id": "202", "values": [{"id": "3"}]}]})

            """
            а теперь дорогие офферы чтобы проверить что байбокс нас не остановит
            """

            response = self.report.request_json(request + '&fesh=2&market-force-business-id=1')
            self.assertFragmentIn(
                response,
                {
                    "filters": [
                        {
                            "id": "glprice",
                            "isGuruLight": Absent(),
                        },
                        {
                            "id": "202",
                            "isGuruLight": True,
                            "type": "enum",
                            "values": [
                                {"id": "1", "marketSku": "2001", "checked": True, "fuzzy": Absent(), "found": 1},
                                {"id": "2", "marketSku": "2003", "checked": Absent(), "fuzzy": True, "found": 1},
                            ],
                        },
                        {
                            "id": "203",
                            "isGuruLight": True,
                            "type": "enum",
                            "values": [
                                {
                                    "id": "15~15",
                                    "value": "15",
                                    "marketSku": "2001",
                                    "checked": True,
                                    "fuzzy": Absent(),
                                    "found": 1,
                                },
                                {
                                    "id": "16~16",
                                    "value": "16",
                                    "marketSku": "2002",
                                    "checked": Absent(),
                                    "fuzzy": Absent(),
                                    "found": 1,
                                },
                            ],
                        },
                        {
                            "id": "204",
                            "isGuruLight": True,
                            "type": "enum",
                            "values": [
                                {"id": "1", "marketSku": "2003", "checked": Absent(), "fuzzy": True, "found": 1},
                                {"id": "0", "marketSku": "2001", "checked": True, "fuzzy": Absent(), "found": 1},
                            ],
                        },
                    ]
                },
            )

            response = self.report.request_json(request_no_msku + '&fesh=11' + old_pipeline_flag)
            self.assertFragmentIn(
                response,
                {
                    "filters": [
                        {
                            "id": "glprice",
                            "isGuruLight": Absent(),
                        },
                        {
                            "id": "202",
                            "isGuruLight": True,
                            "type": "enum",
                            "values": [
                                {
                                    "id": "1",
                                    "marketSku": EqualToOneOf("2001", "2002"),
                                    "checked": Absent(),
                                    "fuzzy": Absent(),
                                    "found": 1,
                                },
                                {
                                    "id": "2",
                                    "marketSku": "2003",
                                    "checked": Absent(),
                                    "fuzzy": Absent(),
                                    "found": 1,
                                },
                            ],
                        },
                        {
                            "id": "203",
                            "isGuruLight": True,
                            "type": "enum",
                            "values": [
                                {
                                    "id": "15~15",
                                    "value": "15",
                                    "marketSku": EqualToOneOf("2001", "2003"),
                                    "checked": Absent(),
                                    "fuzzy": Absent(),
                                    "found": 1,
                                },
                                {
                                    "id": "16~16",
                                    "value": "16",
                                    "marketSku": "2002",
                                    "checked": Absent(),
                                    "fuzzy": Absent(),
                                    "found": 1,
                                },
                            ],
                        },
                        {
                            "id": "204",
                            "isGuruLight": True,
                            "type": "enum",
                            "values": [
                                {
                                    "id": "1",
                                    "marketSku": "2003",
                                    "checked": Absent(),
                                    "fuzzy": Absent(),
                                    "found": 1,
                                },
                                {
                                    "id": "0",
                                    "marketSku": EqualToOneOf("2001", "2002"),
                                    "checked": Absent(),
                                    "fuzzy": Absent(),
                                    "found": 1,
                                },
                            ],
                        },
                    ]
                },
            )

            self.assertFragmentNotIn(response, {"filters": [{"id": "202", "values": [{"id": "3"}]}]})

            response = self.report.request_json(request + '&mcpricefrom=90&mcpriceto=110' + old_pipeline_flag)
            self.assertFragmentIn(
                response,
                {
                    "filters": [
                        {
                            "id": "glprice",
                            "isGuruLight": Absent(),
                        },
                        {
                            "id": "202",
                            "isGuruLight": True,
                            "type": "enum",
                            "values": [
                                {"id": "1", "marketSku": "2001", "checked": True, "fuzzy": Absent(), "found": 0},
                                {"id": "3", "marketSku": "2005", "checked": Absent(), "fuzzy": True, "found": 1},
                            ],
                        },
                        {
                            "id": "203",
                            "isGuruLight": True,
                            "type": "enum",
                            "values": [
                                {
                                    "id": "15~15",
                                    "value": "15",
                                    "marketSku": "2001",
                                    "checked": True,
                                    "fuzzy": Absent(),
                                    "found": 0,
                                },
                                {
                                    "id": "16~16",
                                    "value": "16",
                                    "marketSku": "2002",
                                    "checked": Absent(),
                                    "fuzzy": Absent(),
                                    "found": 1,
                                },
                            ],
                        },
                        {
                            "id": "204",
                            "isGuruLight": True,
                            "type": "enum",
                            "values": [
                                {"id": "1", "marketSku": "2005", "checked": Absent(), "fuzzy": True, "found": 1},
                                {"id": "0", "marketSku": "2001", "checked": True, "fuzzy": Absent(), "found": 0},
                            ],
                        },
                    ]
                },
            )

            self.assertFragmentNotIn(response, {"filters": [{"id": "202", "values": [{"id": "2"}]}]})

            response = self.report.request_json(request_no_msku + '&mcpricefrom=90&mcpriceto=110' + old_pipeline_flag)
            self.assertFragmentIn(
                response,
                {
                    "filters": [
                        {
                            "id": "glprice",
                            "isGuruLight": Absent(),
                        },
                        {
                            "id": "202",
                            "isGuruLight": True,
                            "type": "enum",
                            "values": [
                                {
                                    "id": "1",
                                    "marketSku": "2002",
                                    "checked": Absent(),
                                    "fuzzy": Absent(),
                                    "found": 1,
                                },
                                {
                                    "id": "3",
                                    "marketSku": "2005",
                                    "checked": Absent(),
                                    "fuzzy": Absent(),
                                    "found": 1,
                                },
                            ],
                        },
                        {
                            "id": "204",
                            "isGuruLight": True,
                            "type": "enum",
                            "values": [
                                {
                                    "id": "1",
                                    "marketSku": "2005",
                                    "checked": Absent(),
                                    "fuzzy": Absent(),
                                    "found": 1,
                                },
                                {
                                    "id": "0",
                                    "marketSku": "2002",
                                    "checked": Absent(),
                                    "fuzzy": Absent(),
                                    "found": 1,
                                },
                            ],
                        },
                    ]
                },
            )

            self.assertFragmentNotIn(response, {"filters": [{"id": "202", "values": [{"id": "2"}]}]})

            self.assertFragmentNotIn(response, {"filters": [{"id": "203", "values": [{"marketSku": NotEmpty()}]}]})

    def test_jump_table_express(self):
        """
        Проверяем, что для express товаров при зажатом фильтре таблица переходов создается
        """

        request = 'place=productoffers&hyperid=2&hid=2&debug=da&onstock=0&platform=touch&filter-express-delivery=1&market-sku=2001'

        response = self.report.request_json(request)

        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": "glprice",
                        "isGuruLight": Absent(),
                    },
                    {
                        "id": "202",
                        "isGuruLight": True,
                        "type": "enum",
                        "unit": "л",
                        "position": 10,
                        "precision": 0,
                        "values": [
                            {"id": "1", "marketSku": "2001", "checked": True, "fuzzy": Absent(), "found": 1},
                            {"id": "2", "marketSku": "2003", "checked": Absent(), "fuzzy": True, "found": 1},
                        ],
                    },
                    {
                        "id": "203",
                        "isGuruLight": True,
                        "type": "enum",
                        "unit": "кг",
                        "position": 20,
                        "precision": 3,
                        "values": [
                            {
                                "id": "15~15",
                                "value": "15",
                                "marketSku": "2001",
                                "checked": True,
                                "fuzzy": Absent(),
                                "found": 1,
                            },
                            {
                                "id": "16~16",
                                "value": "16",
                                "marketSku": "2002",
                                "checked": Absent(),
                                "fuzzy": Absent(),
                                "found": 1,
                            },
                        ],
                    },
                    {
                        "id": "204",
                        "isGuruLight": True,
                        "type": "enum",
                        "unit": Absent(),
                        "precision": 0,
                        "values": [
                            {"id": "1", "marketSku": "2003", "checked": Absent(), "fuzzy": True, "found": 1},
                            {"id": "0", "marketSku": "2001", "checked": True, "fuzzy": Absent(), "found": 1},
                        ],
                    },
                ]
            },
        )

        self.assertFragmentNotIn(response, {"filters": [{"id": "202", "values": [{"id": "4"}]}]})
        self.assertFragmentNotIn(response, {"filters": [{"id": "205"}]})

    @classmethod
    def prepare_no_psku_jmp_table(cls):
        cls.index.models += [
            Model(hyperid=3, is_pmodel=True, hid=3),
        ]

        cls.index.gltypes += [
            GLType(hid=3, param_id=301, cluster_filter=True, gltype=GLType.NUMERIC, model_filter_index=1),
        ]

        cls.index.mskus += [
            MarketSku(
                hyperid=3,
                forbidden_market_mask=Offer.IS_PSKU,
                sku=3001,
                title="МСКУ 3001",
                blue_offers=[BlueOffer()],
            ),
            MarketSku(
                hyperid=3,
                forbidden_market_mask=Offer.IS_PSKU,
                sku=3002,
                title="МСКУ 3002",
                blue_offers=[BlueOffer()],
            ),
            MarketSku(
                hyperid=3,
                forbidden_market_mask=Offer.IS_PSKU,
                sku=3003,
                title="МСКУ 3003",
                blue_offers=[],
            ),
        ]

        cls.index.offers += [
            Offer(hyperid=3, hid=3, sku=3003, glparams=[GLParam(param_id=301, value=1)]),
            Offer(hyperid=3, hid=3, sku=3003, glparams=[GLParam(param_id=301, value=2)]),
        ]

    def test_no_psku_jmp_table(self):
        """
        Проверяем, что, если таблица переходов пскушная, формируются обычные
        белые фильтры
        """

        request_no_msku = 'place=productoffers&hyperid=3&hid=3&debug=da&onstock=0&platform=touch&'

        request_with_msku = request_no_msku + '&market-sku=3003'
        old_pipeline = '&rearr-factors=use_new_jump_table_pipeline=0'

        for request in (request_no_msku, request_with_msku):
            for old_pipeline_flag in ('', old_pipeline):
                response = self.report.request_json(request + old_pipeline_flag)
                self.assertFragmentIn(
                    response,
                    {
                        "search": {},
                        "filters": [
                            {
                                "id": "301",
                                "type": "enum",
                                "values": [
                                    {"value": "1", "marketSku": Absent()},
                                    {"value": "2", "marketSku": Absent()},
                                ],
                            }
                        ],
                    },
                )

                self.assertFragmentNotIn(response, {"filters": [{"id": "modifications"}]})

    COMMON = '//avatars.mds.yandex.net/get-mpic'

    GROUP_ID = '466729'
    IMG1 = 'img_id7441026923613423143'
    IMG2 = 'img_id7441026923613423144'
    IMG1_URL = '%s/%s/%s/orig' % (COMMON, GROUP_ID, IMG1)
    IMG2_URL = '%s/%s/%s/orig' % (COMMON, GROUP_ID, IMG2)

    IMG_MODEL = 'img_model'
    IMG_MODEL_URL = '%s/%s/%s/orig' % (COMMON, GROUP_ID, IMG_MODEL)

    @classmethod
    def prepare_image_picker(cls):
        cls.index.gltypes += [
            GLType(
                param_id=244,
                hid=72,
                gltype=GLType.ENUM,
                subtype='image_picker',
                cluster_filter=True,
                model_filter_index=1,
                values=[
                    GLValue(
                        1,
                        image=ImagePickerData(
                            url=T.IMG1_URL,
                            namespace="get-mpic",
                            group_id=T.GROUP_ID,
                            image_name=T.IMG1,
                        ),
                    ),
                    GLValue(
                        2,
                        image=ImagePickerData(
                            url=T.IMG2_URL,
                            namespace="get-mpic",
                            group_id=T.GROUP_ID,
                            image_name=T.IMG2,
                        ),
                    ),
                ],
            )
        ]

        cls.index.models += [
            Model(
                hid=72,
                hyperid=720,
                parameter_value_links=[
                    ParameterValue(
                        244,
                        2,
                        ImagePickerData(
                            url=T.IMG_MODEL_URL,
                            namespace="get-mpic",
                            group_id=T.GROUP_ID,
                            image_name=T.IMG_MODEL,
                        ),
                    ),
                ],
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                hid=72,
                hyperid=720,
                sku=7201,
                title="Платье 1",
                fesh=11,
                blue_offers=[BlueOffer(price=200)],
                glparams=[
                    GLParam(param_id=244, value=1),
                ],
            ),
            MarketSku(
                hid=72,
                hyperid=720,
                sku=7202,
                title="Платье 2",
                fesh=11,
                blue_offers=[BlueOffer(price=100)],
                glparams=[
                    GLParam(param_id=244, value=2),
                ],
            ),
        ]

    def test_image_picker(self):
        """
        Проверяем, что пикер формируется во всех пайплайнах формирования
        таблицы переходов на белом. При этом в новом пайплайне он берётся
        только из мбо, а в старом может взяться и из информации о модели.
        """

        request = 'place=productoffers&hyperid=720&filterList=all&hid=72&platform=touch&' 'debug=da'
        old_pipeline_flag = '&rearr-factors=use_new_jump_table_pipeline=0'

        for sku in ('', '&market-sku=7201'):
            response = self.report.request_json(request + sku)
            self.assertFragmentIn(
                response,
                {
                    "search": {},
                    "filters": [
                        {
                            "id": "244",
                            "type": "enum",
                            "subType": "image_picker",
                            "values": [
                                {
                                    "id": "1",
                                    "marketSku": "7201",
                                    "checked": True if sku else Absent(),
                                    "image": T.IMG1_URL,
                                    "picker": {
                                        "groupId": T.GROUP_ID,
                                        "imageName": T.IMG1,
                                    },
                                },
                                {
                                    "id": "2",
                                    "marketSku": "7202",
                                    "checked": Absent(),
                                    "image": T.IMG2_URL,
                                    "picker": {
                                        "groupId": T.GROUP_ID,
                                        "imageName": T.IMG2,
                                    },
                                },
                            ],
                        }
                    ],
                },
            )

            response = self.report.request_json(request + sku + old_pipeline_flag)
            self.assertFragmentIn(
                response,
                {
                    "search": {},
                    "filters": [
                        {
                            "id": "244",
                            "type": "enum",
                            "subType": "image_picker",
                            "values": [
                                {
                                    "id": "1",
                                    "marketSku": "7201",
                                    "checked": True if sku else Absent(),
                                    "image": T.IMG1_URL,
                                    "picker": {
                                        "groupId": T.GROUP_ID,
                                        "imageName": T.IMG1,
                                    },
                                },
                                {
                                    "id": "2",
                                    "marketSku": "7202",
                                    "checked": Absent(),
                                    "image": T.IMG_MODEL_URL,
                                    "picker": {
                                        "groupId": T.GROUP_ID,
                                        "imageName": T.IMG_MODEL,
                                    },
                                },
                            ],
                        }
                    ],
                },
            )

    GROUP_GROUP_ID = '466730'
    IMG_MODIF1 = 'img_id7441026923613423145'
    IMG_MODIF2 = 'img_id7441026923613423146'
    IMG_MODIF3 = 'img_id7441026923613423147'
    IMG_MODIF4 = 'img_id7441026923613423148'
    IMG_MODIF1_URL = '%s/%s/%s/orig' % (COMMON, GROUP_GROUP_ID, IMG_MODIF1)
    IMG_MODIF2_URL = '%s/%s/%s/orig' % (COMMON, GROUP_GROUP_ID, IMG_MODIF2)
    IMG_MODIF3_URL = '%s/%s/%s/orig' % (COMMON, GROUP_GROUP_ID, IMG_MODIF3)
    IMG_MODIF4_URL = '%s/%s/%s/orig' % (COMMON, GROUP_GROUP_ID, IMG_MODIF4)

    @classmethod
    def prepare_groups_and_modifications(cls):
        cls.index.gltypes += [
            GLType(
                param_id=233, hid=10, gltype=GLType.ENUM, cluster_filter=True, model_filter_index=1, values=[1, 2, 3, 4]
            ),
        ]

        cls.index.model_groups += [
            ModelGroup(hyperid=100, hid=10),
        ]

        cls.index.models += [
            Model(
                hyperid=101,
                hid=10,
                group_hyperid=100,
                parameter_value_links=[
                    ParameterValue(
                        233,
                        1,
                        ImagePickerData(
                            url=T.IMG_MODIF1_URL,
                            namespace="get-mpic",
                            group_id=T.GROUP_GROUP_ID,
                            image_name=T.IMG_MODIF1,
                        ),
                    ),
                    ParameterValue(
                        233,
                        2,
                        ImagePickerData(
                            url=T.IMG_MODIF2_URL,
                            namespace="get-mpic",
                            group_id=T.GROUP_GROUP_ID,
                            image_name=T.IMG_MODIF2,
                        ),
                    ),
                ],
            ),
            Model(
                hyperid=102,
                hid=10,
                group_hyperid=100,
                parameter_value_links=[
                    ParameterValue(
                        233,
                        3,
                        ImagePickerData(
                            url=T.IMG_MODIF3_URL,
                            namespace="get-mpic",
                            group_id=T.GROUP_GROUP_ID,
                            image_name=T.IMG_MODIF3,
                        ),
                    ),
                    ParameterValue(
                        233,
                        4,
                        ImagePickerData(
                            url=T.IMG_MODIF4_URL,
                            namespace="get-mpic",
                            group_id=T.GROUP_GROUP_ID,
                            image_name=T.IMG_MODIF4,
                        ),
                    ),
                ],
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                hid=10,
                hyperid=101,
                sku=1010,
                title="Карта 16 gb синяя",
                fesh=11,
                blue_offers=[BlueOffer(price=200)],
                glparams=[
                    GLParam(param_id=233, value=1),
                ],
            ),
            MarketSku(
                hid=10,
                hyperid=101,
                sku=1011,
                title="Карта 16 gb красная",
                fesh=11,
                blue_offers=[BlueOffer(price=100)],
                glparams=[
                    GLParam(param_id=233, value=2),
                ],
            ),
            MarketSku(
                hid=10,
                hyperid=102,
                sku=1020,
                title="Карта 32 gb жёлтая",
                fesh=11,
                blue_offers=[BlueOffer(price=200)],
                glparams=[
                    GLParam(param_id=233, value=3),
                ],
            ),
            MarketSku(
                hid=10,
                hyperid=102,
                sku=1021,
                title="Карта 32 gb зелёная",
                fesh=11,
                blue_offers=[BlueOffer(price=100)],
                glparams=[
                    GLParam(param_id=233, value=4),
                ],
            ),
        ]

    def test_groups_and_modifications(self):
        """
        Проверяем, что:
        1. Без флага market_white_use_parent_model_for_jump_table=1 таблица
        перехода не работает для групповых моделей и работает для модификаций
        только в рамках самих этих модификаций, каждой по отдельности
        2. С флагом таблица перехода что в групповой, что в модификации может
        вести на ску другой модификации той же группы, при этом в значениях
        заполняется соответствующий modelId модификации
        Проверяем т.ж., что пикеры для групп работают корректно
        """

        request = 'place=productoffers&hyperid=%d&filterList=all&hid=10&platform=touch&' 'debug=da'
        flag = '&rearr-factors=market_white_use_parent_model_for_jump_table=1'

        for sku in ('', '&market-sku=1010'):
            response = self.report.request_json(request % 101 + sku)
            self.assertFragmentIn(
                response,
                {
                    "search": {},
                    "filters": [
                        {
                            "id": "233",
                            "type": "enum",
                        }
                    ],
                },
            )
            self.assertFragmentIn(
                response,
                {
                    "id": "233",
                    "type": "enum",
                    "values": [
                        {
                            "id": "1",
                            "marketSku": "1010",
                            "checked": True if sku else Absent(),
                        },
                        {
                            "id": "2",
                            "marketSku": "1011",
                            "checked": Absent(),
                        },
                    ],
                },
                allow_different_len=False,
            )

            response = self.report.request_json(request % 101 + sku + flag)
            self.assertFragmentIn(
                response,
                {
                    "id": "233",
                    "type": "enum",
                    "values": [
                        {
                            "id": "1",
                            "marketSku": "1010",
                            "modelId": 101,
                            "checked": True if sku else Absent(),
                            "image": T.IMG_MODIF1_URL,
                            "picker": {
                                "groupId": T.GROUP_GROUP_ID,
                                "imageName": T.IMG_MODIF1,
                            },
                        },
                        {
                            "id": "2",
                            "marketSku": "1011",
                            "modelId": 101,
                            "checked": Absent(),
                            "image": T.IMG_MODIF2_URL,
                            "picker": {
                                "groupId": T.GROUP_GROUP_ID,
                                "imageName": T.IMG_MODIF2,
                            },
                        },
                        {
                            "id": "3",
                            "marketSku": "1020",
                            "modelId": 102,
                            "checked": Absent(),
                            "image": T.IMG_MODIF3_URL,
                            "picker": {
                                "groupId": T.GROUP_GROUP_ID,
                                "imageName": T.IMG_MODIF3,
                            },
                        },
                        {
                            "id": "4",
                            "marketSku": "1021",
                            "modelId": 102,
                            "checked": Absent(),
                            "image": T.IMG_MODIF4_URL,
                            "picker": {
                                "groupId": T.GROUP_GROUP_ID,
                                "imageName": T.IMG_MODIF4,
                            },
                        },
                    ],
                },
                allow_different_len=False,
            )

        response = self.report.request_json(request % 100)
        self.assertFragmentIn(
            response,
            {
                "search": {},
                "filters": [
                    {
                        "id": "233",
                        "type": "enum",
                    }
                ],
            },
        )
        self.assertFragmentIn(
            response,
            {
                "id": "233",
                "type": "enum",
                "values": [
                    {
                        "id": "1",
                        "marketSku": Absent(),
                        "checked": Absent(),
                    },
                    {
                        "id": "2",
                        "marketSku": Absent(),
                        "checked": Absent(),
                    },
                    {
                        "id": "3",
                        "marketSku": Absent(),
                        "checked": Absent(),
                    },
                    {
                        "id": "4",
                        "marketSku": Absent(),
                        "checked": Absent(),
                    },
                ],
            },
            allow_different_len=False,
        )

        for sku in ('', '&market-sku=1010'):
            response = self.report.request_json(request % 100 + sku + flag)
            self.assertFragmentIn(
                response,
                {
                    "id": "233",
                    "type": "enum",
                    "values": [
                        {
                            "id": "1",
                            "marketSku": "1010",
                            "modelId": 101,
                            "checked": True if sku else Absent(),
                            "image": T.IMG_MODIF1_URL,
                            "picker": {
                                "groupId": T.GROUP_GROUP_ID,
                                "imageName": T.IMG_MODIF1,
                            },
                        },
                        {
                            "id": "2",
                            "marketSku": "1011",
                            "modelId": 101,
                            "checked": Absent(),
                            "image": T.IMG_MODIF2_URL,
                            "picker": {
                                "groupId": T.GROUP_GROUP_ID,
                                "imageName": T.IMG_MODIF2,
                            },
                        },
                        {
                            "id": "3",
                            "marketSku": "1020",
                            "modelId": 102,
                            "checked": Absent(),
                            "image": T.IMG_MODIF3_URL,
                            "picker": {
                                "groupId": T.GROUP_GROUP_ID,
                                "imageName": T.IMG_MODIF3,
                            },
                        },
                        {
                            "id": "4",
                            "marketSku": "1021",
                            "modelId": 102,
                            "checked": Absent(),
                            "image": T.IMG_MODIF4_URL,
                            "picker": {
                                "groupId": T.GROUP_GROUP_ID,
                                "imageName": T.IMG_MODIF4,
                            },
                        },
                    ],
                },
                allow_different_len=False,
            )

    class category_1:
        hid = 1000

        class param_1:
            id = 2000

        class model_1:
            id = 3000

    class category_2:
        hid = 1001
        nid = 1001000

    @classmethod
    def prepare_test_build_jump_table_with_incorrect_nid(cls):
        cls.index.gltypes += [
            GLType(
                hid=T.category_1.hid,
                param_id=T.category_1.param_1.id,
                cluster_filter=True,
                gltype=GLType.ENUM,
                values=[1, 2, 3, 4, 5],
                model_filter_index=1,
            ),
        ]
        cls.index.mskus += [
            MarketSku(
                hid=T.category_1.hid,
                hyperid=T.category_1.model_1.id,
                sku=10001,
                blue_offers=[BlueOffer(waremd5='_qQNWXU28-IUghltMZJwNw', ts=1, price=200)],
                glparams=[
                    GLParam(param_id=T.category_1.param_1.id, value=2),
                ],
            ),
            MarketSku(
                hid=T.category_1.hid,
                hyperid=T.category_1.model_1.id,
                sku=10002,
                blue_offers=[BlueOffer(waremd5='RPDDqEFjs1I6_lfC4Ai8jA', ts=2, price=200)],
                glparams=[
                    GLParam(param_id=T.category_1.param_1.id, value=3),
                ],
            ),
        ]

        cls.index.navtree += [NavCategory(nid=T.category_2.nid, hid=T.category_2.hid)]

    def test_build_jump_table_with_incorrect_nid(self):
        """
        Проверяем, что при не корректном nid карта переходов корректно будет сформирована по hid документа
        """

        def table_request(model, hid, nid=None, flag=None):
            request = 'place=productoffers&pp=1'
            request += '&hyperid={}'.format(model)
            request += '&hid={}'.format(hid)
            if nid:
                request += '&nid={}'.format(nid)
            if flag:
                request += '&rearr-factors={}'.format(flag)

            return request

        """
        Стандатная проверка, карта переходов по 1 параметру, только по модели и hid
        """
        request = table_request(T.category_1.model_1.id, T.category_1.hid)
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {},
                "filters": [
                    {
                        "id": str(T.category_1.param_1.id),
                    }
                ],
            },
        )
        """
        Проверяем, что с не корректным nid карта переходов не строится, даже при наличии корректного hid
        """
        request = table_request(
            T.category_1.model_1.id,
            T.category_1.hid,
            T.category_2.nid,
            'market_build_jump_table_with_document_category=0',
        )
        response = self.report.request_json(request)
        self.assertFragmentNotIn(
            response,
            {
                "search": {},
                "filters": [
                    {
                        "id": str(T.category_1.param_1.id),
                    }
                ],
            },
        )
        """
        Проверяем, что с реар флагом карта переходов корректно строится, даже при наличии не корректного nid
        """
        request = table_request(
            T.category_1.model_1.id,
            T.category_1.hid,
            T.category_2.nid,
            'market_build_jump_table_with_document_category=1',
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {},
                "filters": [
                    {
                        "id": str(T.category_1.param_1.id),
                    }
                ],
            },
        )


if __name__ == '__main__':
    main()
