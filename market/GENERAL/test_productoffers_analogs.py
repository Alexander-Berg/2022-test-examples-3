#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from datetime import datetime
from core.types import (
    BlueOffer,
    ClickType,
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryOption,
    DeliveryServiceRegionToRegionInfo,
    DynamicDaysSet,
    DynamicDeliveryServiceInfo,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseInfo,
    DynamicWarehouseToWarehouseInfo,
    DynamicWarehousesPriorityInRegion,
    ExpressDeliveryService,
    ExpressSupplier,
    GLParam,
    GLType,
    GLValue,
    HyperCategory,
    HyperCategoryType,
    MarketSku,
    MnPlace,
    Model,
    Offer,
    Opinion,
    Region,
    RegionalDelivery,
    Shop,
    Tax,
    TimeInfo,
    WarehouseWithPriority,
    WarehousesPriorityInRegion,
    YamarecPlace,
    YamarecSettingPartition,
)
from core.testcase import TestCase, main
from core.matcher import Absent, NoKey, NotEmpty
from core.report import REQUEST_TIMESTAMP

DATETIME_NOW = datetime.fromtimestamp(REQUEST_TIMESTAMP)


def dict_to_rearr(rearr_flags):
    result = ""
    for key in rearr_flags.keys():
        result += str(key) + "=" + str(rearr_flags[key]) + ";"
    return result


class _Constants:
    # Constants to set up express delivery, like at test_express_delivery.py
    russia_rids = 225
    moscow_rids = 213

    class _ExpressPartners:
        # dropship_fesh should be different to 10, 20, 40
        dropship_fesh = 50
        dropship_feed_id = 50
        dropship_warehouse_id = 11

        delivery_service_id = 101

        dc_day_from = 0
        dc_day_to = 0
        dc_delivery_cost = 50


class T(TestCase):
    @classmethod
    def prepare_delivery(cls):
        # prepare express partners
        # the same as in test_express_delivery
        cls.index.express_partners.suppliers += [
            ExpressSupplier(
                feed_id=_Constants._ExpressPartners.dropship_feed_id,
                supplier_id=_Constants._ExpressPartners.dropship_fesh,
                warehouse_id=_Constants._ExpressPartners.dropship_warehouse_id,
            )
        ]
        cls.index.express_partners.delivery_services += [
            ExpressDeliveryService(
                delivery_service_id=_Constants._ExpressPartners.delivery_service_id, delivery_price_for_user=350
            )
        ]

        # prepare warehouses
        # the same as in test_express_delivery
        cls.index.warehouse_priorities += [
            WarehousesPriorityInRegion(
                regions=[_Constants.russia_rids, _Constants.moscow_rids],
                warehouse_with_priority=[
                    WarehouseWithPriority(warehouse_id=_Constants._ExpressPartners.dropship_warehouse_id, priority=1)
                ],
            )
        ]

        # prepare LMS
        # the same as in test_express_delivery
        cls.settings.lms_autogenerate = False

        cls.dynamic.lms += [DynamicDaysSet(key=1, days=[])]

        for wh_id, ds_id, ds_name in [
            (
                _Constants._ExpressPartners.dropship_warehouse_id,
                _Constants._ExpressPartners.delivery_service_id,
                'express_delivery_service',
            )
        ]:
            cls.dynamic.lms += [
                DynamicDeliveryServiceInfo(
                    id=ds_id,
                    name=ds_name,
                    region_to_region_info=[
                        DeliveryServiceRegionToRegionInfo(
                            region_from=_Constants.moscow_rids, region_to=_Constants.russia_rids, days_key=1
                        )
                    ],
                ),
                DynamicWarehouseInfo(id=wh_id, home_region=_Constants.moscow_rids),
                DynamicWarehouseToWarehouseInfo(warehouse_from=wh_id, warehouse_to=wh_id),
                DynamicWarehouseAndDeliveryServiceInfo(
                    warehouse_id=wh_id,
                    delivery_service_id=ds_id,
                    operation_time=0,
                    date_switch_time_infos=[
                        DateSwitchTimeAndRegionInfo(
                            date_switch_hour=2,
                            region_to=_Constants.russia_rids,
                            date_switch_time=TimeInfo(19, 0),
                            packaging_time=TimeInfo(3, 30),
                        )
                    ],
                ),
            ]

            cls.dynamic.lms += [
                DynamicWarehouseInfo(
                    id=_Constants._ExpressPartners.dropship_warehouse_id,
                    home_region=_Constants.moscow_rids,
                    is_express=True,
                ),
            ]

        # prepare for usual deivery
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
            # Market durable delivery
            DeliveryBucket(
                bucket_id=1225,
                carriers=[157],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[RegionalDelivery(rid=225, options=[DeliveryOption(price=15, day_from=2, day_to=4)])],
            ),
            # dsbs durable delivery
            DeliveryBucket(
                bucket_id=1226,
                fesh=16,
                carriers=[99],
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=20, day_from=3, day_to=8)])],
            ),
            # Market faster delivery
            DeliveryBucket(
                bucket_id=1228,
                carriers=[157],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[RegionalDelivery(rid=225, options=[DeliveryOption(price=15, day_from=1, day_to=1)])],
            ),
            # delivery like bucket 1225 but dsbs
            DeliveryBucket(
                bucket_id=1229,
                fesh=16,
                carriers=[99],
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=15, day_from=2, day_to=4)])],
            ),
        ]

    @classmethod
    def prepare(cls):
        """
        Модели, офферы и конфигурация для выдачи аналогов на КМ
        """

        cls.settings.nordstream_autogenerate = False
        cls.settings.default_search_experiment_flags += ['market_nordstream=0']
        cls.matrixnet.on_default_place(MnPlace.BASE_SEARCH).respond(0.01)

        cls.index.regiontree += [
            Region(rid=213, region_type=Region.CITY),
            Region(rid=2, region_type=Region.CITY),
        ]

        cls.index.shops += [
            Shop(fesh=10, priority_region=213, regions=[225], blue=Shop.BLUE_REAL),
            Shop(
                fesh=_Constants._ExpressPartners.dropship_fesh,  # 50
                datafeed_id=_Constants._ExpressPartners.dropship_feed_id,
                warehouse_id=_Constants._ExpressPartners.dropship_warehouse_id,
                priority_region=_Constants.moscow_rids,  # 213
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                is_supplier=True,
                regions=[225],  # THIRD_PARTY
                blue=Shop.BLUE_REAL,
                fulfillment_program=True,
            ),  # shop with express delivery # False
            Shop(fesh=20, priority_region=213, regions=[225], cpa=Shop.CPA_REAL),
            Shop(fesh=40, priority_region=213, regions=[225]),
            # Shops to test delivery score
            Shop(
                fesh=150005,
                datafeed_id=150005,
                priority_region=213,
                regions=[225],
                name="Один 1P поставщик",
                supplier_type=Shop.FIRST_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
                fulfillment_program=True,
            ),
            Shop(
                fesh=16,
                datafeed_id=16,
                business_fesh=16,
                name="dsbs магазин Пети",
                regions=[213],
                cis=Shop.CIS_REAL,
                cpa=Shop.CPA_REAL,
                cpc=Shop.CPC_NO,
                priority_region=213,
            ),
        ]

        cls.index.hypertree += [HyperCategory(hid=130, output_type=HyperCategoryType.SIMPLE)]

        cls.index.gltypes += [
            GLType(
                param_id=201,
                hid=130,
                model_filter_index=1,
                gltype=GLType.ENUM,
                values=[
                    GLValue(value_id=1, text='value1'),
                    GLValue(value_id=2, text='value2'),
                ],
                xslname='sku_filter',
            )
        ]

        cls.index.models += [
            Model(
                hyperid=110,
                hid=110,
                opinion=Opinion(
                    total_count=100, positive_count=95, rating=4.5, precise_rating=4.6, rating_count=200, reviews=5
                ),
            ),
            Model(
                hyperid=120,
                hid=120,
                opinion=Opinion(
                    total_count=100, positive_count=60, rating=3.5, precise_rating=3.4, rating_count=200, reviews=5
                ),
            ),
            Model(
                hyperid=130,
                hid=130,
                opinion=Opinion(
                    total_count=100, positive_count=65, rating=3.5, precise_rating=3.7, rating_count=200, reviews=5
                ),
            ),  # no CPA model
            Model(
                hyperid=140,
                hid=140,
                opinion=Opinion(
                    total_count=100, positive_count=55, rating=3.5, precise_rating=3.3, rating_count=200, reviews=5
                ),
            ),
            Model(
                hyperid=150,
                hid=150,
                opinion=Opinion(
                    total_count=100, positive_count=60, rating=3.5, precise_rating=3.4, rating_count=200, reviews=5
                ),
            ),
            Model(
                hyperid=160,
                hid=160,
                opinion=Opinion(
                    total_count=100, positive_count=60, rating=3.5, precise_rating=3.4, rating_count=200, reviews=5
                ),
            ),
            Model(
                hyperid=170,
                hid=170,
                opinion=Opinion(
                    total_count=0, positive_count=0, rating=4.5, precise_rating=4.75, rating_count=200, reviews=5
                ),
            ),  # no opinions CPA model
            Model(
                hyperid=180,
                hid=180,
                opinion=Opinion(
                    total_count=0, positive_count=0, rating=4.5, precise_rating=4.6, rating_count=200, reviews=5
                ),
            ),  # no opinions CPC model
            Model(
                hyperid=190,
                hid=190,
                opinion=Opinion(
                    total_count=100, positive_count=95, rating=3.5, precise_rating=3.3, rating_count=200, reviews=5
                ),
            ),  # CPC model
            Model(
                hyperid=200,
                hid=200,
                opinion=Opinion(
                    total_count=100, positive_count=5, rating=3.5, precise_rating=5.0, rating_count=200, reviews=5
                ),
            ),  # CPC model with the worth positive reviews and good other props
            Model(hyperid=210, hid=210),  # model without offers and data
            # Models to test delivery score
            Model(
                hyperid=800,
                hid=800,
                opinion=Opinion(
                    total_count=100, positive_count=60, rating=3.5, precise_rating=3.4, rating_count=200, reviews=5
                ),
                ts=800512,
                title='model_800',
                vbid=10,
            ),
            Model(
                hyperid=801,
                hid=801,
                opinion=Opinion(
                    total_count=100, positive_count=95, rating=4.5, precise_rating=4.6, rating_count=200, reviews=5
                ),
                ts=801512,
                title='model_801',
                vbid=10,
            ),
            Model(
                hyperid=802,
                hid=802,
                opinion=Opinion(
                    total_count=100, positive_count=95, rating=4.5, precise_rating=4.6, rating_count=200, reviews=5
                ),
                ts=802512,
                title='model_802',
                vbid=10,
            ),
            Model(
                hyperid=803,
                hid=803,
                opinion=Opinion(
                    total_count=100, positive_count=95, rating=4.5, precise_rating=4.6, rating_count=200, reviews=5
                ),
                ts=803512,
                title='model_803',
                vbid=10,
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                hyperid=110,
                sku=110,
                delivery_buckets=[1225],
                blue_offers=[
                    BlueOffer(price=1100, feedid=10, waremd5='OFFER-p1100-F10-OOOOWW', ts=109000),
                ],
            ),
            MarketSku(
                hyperid=120,
                sku=120,
                delivery_buckets=[1225],
                blue_offers=[
                    BlueOffer(price=1200, feedid=10, waremd5='OFFER-p1200-F10-OOOOWW', ts=109002),
                ],
            ),
            MarketSku(
                hyperid=140,
                sku=140,
                delivery_buckets=[1225],
                blue_offers=[
                    BlueOffer(price=200, feedid=10, waremd5='OFFER-p0200-F10-OOOOWW', ts=109004),
                ],
            ),
            MarketSku(
                hyperid=150,
                sku=150,
                blue_offers=[  # модель с экспресс-доставкой
                    BlueOffer(
                        price=1200,
                        feedid=50,
                        waremd5='OFFER-p1200-F50-OOOOWW',
                        ts=109006,
                        supplier_id=_Constants._ExpressPartners.dropship_fesh,
                    ),  # for express delivery
                ],
            ),
            MarketSku(
                hyperid=160,
                sku=160,
                blue_offers=[  # модель с экспресс-доставкой
                    BlueOffer(
                        price=1200,
                        feedid=50,
                        waremd5='OFFER-p1200-F50-AOOOWW',
                        ts=109008,
                        supplier_id=_Constants._ExpressPartners.dropship_fesh,
                    ),  # for express delivery
                ],
            ),
            MarketSku(
                hyperid=170,
                sku=170,
                delivery_buckets=[1225],
                blue_offers=[
                    BlueOffer(price=1120, feedid=10, waremd5='OFFER-p1120-F10-OOOOWW', ts=109010),
                ],
            ),
            # MSKUs to test delivery scores
            MarketSku(
                title='msku_800',
                hyperid=800,
                sku=800,
                hid=800,
                delivery_buckets=[1228],
                blue_offers=[
                    BlueOffer(price=1200, feedid=150005, waremd5='OFEER-p1000-F15-BLUEWW', ts=800200),
                ],
            ),
            MarketSku(title='msku_801', hyperid=801, sku=801, hid=801),
            MarketSku(
                title='msku_802',
                hyperid=802,
                sku=802,
                hid=802,
                delivery_buckets=[1228],
                blue_offers=[
                    BlueOffer(price=2000, feedid=150005, waremd5='OFEER-p2000-F15-BLUEWW', ts=800204),
                ],
            ),
            MarketSku(
                title='msku_803',
                hyperid=803,
                sku=803,
                hid=803,
                delivery_buckets=[1225],
                blue_offers=[
                    BlueOffer(price=1500, feedid=150005, waremd5='OFEER-p1500-F15-BLUEWW', ts=800206),
                ],
            ),
        ]

        cls.index.offers += [
            Offer(fesh=20, hyperid=110, price=1120, ts=107001, cpa=Offer.CPA_REAL, delivery_buckets=[1229]),
            Offer(fesh=40, hyperid=110, price=1140, ts=107003, delivery_buckets=[1229]),
            Offer(fesh=20, hyperid=120, price=1220, ts=107006, cpa=Offer.CPA_REAL, delivery_buckets=[1229]),
            Offer(fesh=40, hyperid=120, price=1240, ts=107007, delivery_buckets=[1229]),
            Offer(
                fesh=40,
                hyperid=130,
                sku=130,
                price=1340,
                ts=107009,
                glparams=[GLParam(param_id=201, value=1)],
                delivery_buckets=[1229],
            ),
            Offer(fesh=40, hyperid=180, sku=180, price=1120, ts=107011, delivery_buckets=[1229]),
            Offer(fesh=40, hyperid=190, sku=190, price=1120, ts=107013, delivery_buckets=[1229]),
            Offer(fesh=40, hyperid=200, sku=200, price=200, ts=107200, delivery_buckets=[1229]),
            # Offers to test delivery score
            Offer(
                title="offer with durable delivery",
                hid=801,
                hyperid=801,
                sku=801,
                price=2000,
                fesh=16,
                business_id=16,
                feedid=16,
                cpa=Offer.CPA_REAL,
                waremd5='OFFER-801-p2000---lbqQ',
                delivery_buckets=[1226],
                offerid="proh.offe801",
                ts=800203,
            ),
        ]

        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.COMPETITIVE_MODEL,
                kind=YamarecPlace.Type.SETTING,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    # no partition with split 'noconfig'
                    # partitions with data
                    YamarecSettingPartition(params={'version': 'SIBLINGS1_AUGMENTED'}, splits=[{'split': 'empty'}]),
                ],
            ),
        ]

        cls.recommender.on_request_accessory_models(
            model_id=130, item_count=1000, version='SIBLINGS1_AUGMENTED'
        ).respond({'models': ['110:0.2', '120:0.1', '140:0.15']})
        cls.recommender.on_request_accessory_models(
            model_id=120, item_count=1000, version='SIBLINGS1_AUGMENTED'
        ).respond({'models': ['110:0.5', '130:0.1', '140:0.15', '150:0.1']})
        # Аналоги для моделей с экспресс-доставкой
        cls.recommender.on_request_accessory_models(
            model_id=150, item_count=1000, version='SIBLINGS1_AUGMENTED'
        ).respond({'models': ['160:0.1', '140:0.05']})
        cls.recommender.on_request_accessory_models(
            model_id=160, item_count=1000, version='SIBLINGS1_AUGMENTED'
        ).respond({'models': ['150:0.1', '800:0.1']})
        cls.recommender.on_request_accessory_models(
            model_id=170, item_count=1000, version='SIBLINGS1_AUGMENTED'
        ).respond({'models': ['110:0.1']})
        cls.recommender.on_request_accessory_models(
            model_id=180, item_count=1000, version='SIBLINGS1_AUGMENTED'
        ).respond({'models': ['110:0.1']})
        cls.recommender.on_request_accessory_models(
            model_id=110, item_count=1000, version='SIBLINGS1_AUGMENTED'
        ).respond({'models': ['170:0.9']})
        cls.recommender.on_request_accessory_models(
            model_id=190, item_count=1000, version='SIBLINGS1_AUGMENTED'
        ).respond({'models': ['170:0.9', '150:0.2']})
        cls.recommender.on_request_accessory_models(
            model_id=200, item_count=1000, version='SIBLINGS1_AUGMENTED'
        ).respond({'models': ['110:0.9', '120:0.9', '140:0.9']})
        # recomendations to test delivery score
        cls.recommender.on_request_accessory_models(
            model_id=801, item_count=1000, version='SIBLINGS1_AUGMENTED'
        ).respond({'models': ['802:0.6']})
        cls.recommender.on_request_accessory_models(
            model_id=802, item_count=1000, version='SIBLINGS1_AUGMENTED'
        ).respond({'models': ['800:0.6', '801:0.6']})
        cls.recommender.on_request_accessory_models(
            model_id=803, item_count=1000, version='SIBLINGS1_AUGMENTED'
        ).respond({'models': ['110:0.6']})
        cls.recommender.on_request_accessory_models(
            model_id=800, item_count=1000, version='SIBLINGS1_AUGMENTED'
        ).respond({'models': ['803:0.6']})
        cls.recommender.on_request_accessory_models(
            model_id=210, item_count=1000, version='SIBLINGS1_AUGMENTED'
        ).respond({'models': ['800:0.6']})

        # data fo autotests
        model_with_50_analogs = 1000
        cls.index.models += [
            Model(
                hyperid=model_with_50_analogs,
                hid=model_with_50_analogs,
                opinion=Opinion(
                    total_count=100, positive_count=50, rating=3.5, precise_rating=3.6, rating_count=200, reviews=5
                ),
            )
        ]
        cls.index.mskus += [
            MarketSku(
                hyperid=model_with_50_analogs,
                sku=model_with_50_analogs,
                delivery_buckets=[1225],
                blue_offers=[
                    BlueOffer(
                        price=model_with_50_analogs,
                        feedid=10,
                        waremd5='OFFER-p1100-F10-' + str(model_with_50_analogs) + 'WW',
                        ts=110000 + model_with_50_analogs,
                    ),
                ],
            )
        ]
        recommended_models_to_model_with_50_analogs = []
        for i in range(1001, 1050):
            cls.index.models += [
                Model(
                    hyperid=i,
                    hid=i,
                    opinion=Opinion(
                        total_count=100, positive_count=95, rating=4.5, precise_rating=4.6, rating_count=200, reviews=5
                    ),
                )
            ]
            cls.index.mskus += [
                MarketSku(
                    hyperid=i,
                    sku=i,
                    delivery_buckets=[1225],
                    blue_offers=[
                        BlueOffer(price=i, feedid=10, waremd5='OFFER-p1100-F10-' + str(i) + 'WW', ts=110000 + i),
                    ],
                )
            ]
            recommended_models_to_model_with_50_analogs += [str(i) + ':0.9']
        cls.recommender.on_request_accessory_models(
            model_id=model_with_50_analogs, item_count=1000, version='SIBLINGS1_AUGMENTED'
        ).respond({'models': recommended_models_to_model_with_50_analogs})

    def test_market_disable_card_analogs(self):
        """
        Проверяем, что market_disable_card_analogs отключает аналоги
        """

        request = 'place=productoffers&hyperid=130&offers-set=top&show-card-analogs=1&rearr-factors=market_disable_card_analogs=1;market_card_analogs_hide_models_in_output=0;split=empty;market_card_analog_min_base_score=0.09&debug=1'  # noqa
        response = self.report.request_json(request)

        self.assertFragmentIn(
            response, "Do not show analogs because 'DisableCardAnalogs'='market_disable_card_analogs' flag is true"
        )
        self.assertFragmentNotIn(response, {"isAnalogModel": True})
        self.assertFragmentNotIn(response, {"isAnalogOffer": True})

    def test_model_with_analogs_top(self):
        """
        Проверяем, что в ответ попадают модели-аналоги
        """

        request = 'place=productoffers&hyperid=130&offers-set=top&show-card-analogs=1&rearr-factors=market_card_analogs_hide_models_in_output=0;split=empty;market_card_analog_min_base_score=0.09;market_card_analogs_dont_show_analogs_for_cpc_only=0&debug=1'  # noqa
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "analogs": [
                    {
                        "model": {"id": 110, "isAnalogModel": True},
                        "offer": {
                            "isAnalogOffer": True,
                            "cpa": "real",
                            "model": {"id": 110},
                            "prices": {"value": "1100"},
                        },
                    }
                ]
            },
        )
        # Проверим, что поиск моделей и ДО для аналогов происходил параллельно
        self.assertFragmentIn(response, "Find models and default offers for card analogs concurrently")
        self.assertFragmentIn(response, "CpcModelsInfo.size() > 0")

    def test_analogs_parallel_requests_flag(self):
        """
        Проверяем, что с выключенным флагом market_card_analogs_parallel_requests репорт не падает
        Когда флаг выключен, запросы для поиска моделей и ДО для аналогов выполняются последовательно
        """

        request = 'place=productoffers&hyperid=130&offers-set=top&show-card-analogs=1&rearr-factors=market_card_analogs_hide_models_in_output=0;split=empty;market_card_analog_min_base_score=0.09;market_card_analogs_parallel_requests=0;market_put_card_analogs_in_search_results=0;market_card_analogs_dont_show_analogs_for_cpc_only=0&debug=1'  # noqa
        response = self.report.request_json(request)
        self.assertFragmentNotIn(  # market_put_card_analogs_in_search_results=0
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "isAnalogOffer": True,
                        "isDefaultOffer": False,
                        "cpa": "real",
                        "model": {"id": 110},
                        "prices": {"value": "1100"},
                        "benefit": Absent(),
                    },  # Analog
                ]
            },
        )
        self.assertFragmentIn(
            response,
            {
                "analogs": [
                    {
                        "model": {"id": 110, "isAnalogModel": True},
                        "offer": {
                            "isAnalogOffer": True,
                            "cpa": "real",
                            "model": {"id": 110},
                            "prices": {"value": "1100"},
                        },
                    }
                ]
            },
        )
        # Эта строка есть в TRACE_ME, когда поиски моделей и ДО для аналогов работают последовательно
        self.assertFragmentIn(response, "Find models and default offers for card analogs in a single thread")

    def test_model_with_analogs_default_list_cpc_model(self):
        """
        Проверяем, что для исходного cpc-оффера в ответ попала cpa-модель-аналог
        """

        request = 'place=productoffers&hyperid=130&hid=130&glfilter=201:1&offers-set=defaultList,list&show-card-analogs=1&rearr-factors=market_card_analogs_hide_models_in_output=0;split=empty;market_card_analog_min_base_score=0.09;market_put_card_analogs_in_separate_block=0;market_card_analogs_dont_show_analogs_for_cpc_only=0&debug=1'  # noqa
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "isAnalogOffer": False,
                        "model": {"id": 130},
                        "prices": {"value": "1340"},
                        "benefit": NotEmpty(),
                    },  # DO
                    {
                        "entity": "offer",
                        "isAnalogOffer": True,
                        "isDefaultOffer": False,
                        "cpa": "real",
                        "model": {"id": 140},
                        "prices": {"value": "200"},
                        "benefit": Absent(),
                    },  # Analog
                ]
            },
        )
        self.assertFragmentNotIn(response, {"analogs": []})  # market_put_card_analogs_in_separate_block=0

    def test_no_analogs_for_cpc_only_models(self):
        """
        Проверяем отключение аналогов на CPC-only карточках - так как оригинал CPC и флаг market_card_analogs_dont_show_analogs_for_cpc_only=1, аналоги не покажутся
        """

        request = 'place=productoffers&hyperid=130&hid=130&glfilter=201:1&offers-set=defaultList,list&show-card-analogs=1&rearr-factors=market_card_analogs_hide_models_in_output=0;split=empty;market_card_analog_min_base_score=0.09;market_put_card_analogs_in_separate_block=0;market_card_analogs_dont_show_analogs_for_cpc_only=1&debug=1'  # noqa
        response = self.report.request_json(request)
        self.assertFragmentNotIn(response, {"isAnalogOffer": True})
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "isAnalogOffer": False,
                        "model": {"id": 130},
                        "prices": {"value": "1340"},
                        "benefit": NotEmpty(),
                    },  # DO остаётся прежним, из теста test_model_with_analogs_default_list_cpc_model
                ]
            },
        )

    def test_model_with_analogs_default_list_cpa_model(self):
        """
        Проверяем, что в ответ попадают модели-аналоги только с cpa офферами
        """

        request = 'place=productoffers&hyperid=120&offers-set=defaultList,list&show-card-analogs=1&rearr-factors=market_force_request_card_analogs=1;market_dont_request_cpc_if_card_analogs_found=0;market_card_analogs_hide_models_in_output=0;split=empty;market_card_analog_min_base_score=0.09;market_card_analogs_score_rand_low=1.0;market_card_analogs_score_rand_delta=0.0;market_card_analogs_dont_show_analogs_for_cpc_only=0&debug=1'  # noqa
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "isAnalogOffer": False,
                        "isDefaultOffer": False,
                        "model": {"id": 120},
                        "prices": {"value": "1200"},
                        "benefit": Absent(),
                    },
                    {
                        "entity": "offer",
                        "isAnalogOffer": False,
                        "isDefaultOffer": False,
                        "model": {"id": 120},
                        "prices": {"value": "1220"},
                        "benefit": Absent(),
                    },
                    {
                        "entity": "offer",
                        "isAnalogOffer": False,
                        "isDefaultOffer": False,
                        "model": {"id": 120},
                        "prices": {"value": "1240"},
                        "benefit": Absent(),
                    },
                    {
                        "entity": "offer",
                        "isAnalogOffer": False,
                        "isDefaultOffer": True,
                        "model": {"id": 120},
                        "prices": {"value": "1200"},
                        "benefit": NotEmpty(),
                    },  # ДО
                    {
                        "entity": "offer",
                        "isAnalogOffer": True,
                        "isDefaultOffer": False,
                        "cpa": "real",
                        "model": {"id": 140},
                        "prices": {"value": "200"},
                        "analog": {"reason": "Price"},
                        "benefit": Absent(),
                    },  # Analog
                    {
                        "entity": "offer",
                        "isAnalogOffer": True,
                        "isDefaultOffer": False,
                        "cpa": "real",
                        "model": {"id": 110},
                        "prices": {"value": "1100"},
                        "analog": {"reason": "ModelRating"},
                        "benefit": Absent(),
                    },  # Analog
                ]
            },
        )

        self.assertFragmentIn(
            response,
            {
                "analogs": [
                    {
                        "model": {
                            "id": 140,
                            "isAnalogModel": True,
                        },
                        "offer": {
                            "isAnalogOffer": True,
                            "cpa": "real",
                            "model": {"id": 140},
                            "prices": {"value": "200"},
                        },
                        "reason": "Price",
                    },
                    {
                        "model": {
                            "id": 110,
                            "isAnalogModel": True,
                        },
                        "offer": {
                            "isAnalogOffer": True,
                            "cpa": "real",
                            "model": {"id": 110},
                            "prices": {"value": "1100"},
                        },
                        "reason": "ModelRating",
                    },
                ]
            },
        )

        self.show_log.expect(
            prev_pp=Absent(),
            pp=266,
            hyper_id=140,
            price=200,
            url_type=0,
            analog_reason_type="Price",
            analog_reason_score=NotEmpty(),
            analog_general_score=NotEmpty(),
            original_model_id=120,
            analogs_data="140,150,-100,-97,0,0,4999,5101;150,100,0,0,1000,0,0,1200;110,500,1199,5250,0,0,90,7540",
        )
        self.show_log.expect(
            prev_pp=Absent(),
            pp=266,
            hyper_id=140,
            price=200,
            url_type=6,
            analog_reason_type="Price",
            analog_reason_score=NotEmpty(),
            analog_general_score=NotEmpty(),
            original_model_id=120,
        )
        self.show_log.expect(
            prev_pp=Absent(),
            pp=266,
            hyper_id=140,
            url_type=16,
            analog_reason_type="Price",
            analog_reason_score=NotEmpty(),
            analog_general_score=NotEmpty(),
            original_model_id=120,
        )
        self.show_log.expect(
            prev_pp=Absent(),
            pp=266,
            hyper_id=110,
            price=1100,
            url_type=0,
            analog_reason_type="ModelRating",
            analog_reason_score=NotEmpty(),
            analog_general_score=NotEmpty(),
            original_model_id=120,
        )
        self.show_log.expect(
            prev_pp=Absent(),
            pp=266,
            hyper_id=110,
            price=1100,
            url_type=6,
            analog_reason_type="ModelRating",
            analog_reason_score=NotEmpty(),
            analog_general_score=NotEmpty(),
            original_model_id=120,
        )
        self.show_log.expect(
            prev_pp=Absent(),
            pp=266,
            hyper_id=110,
            url_type=16,
            analog_reason_type="ModelRating",
            analog_reason_score=NotEmpty(),
            analog_general_score=NotEmpty(),
            original_model_id=120,
        )

        self.click_log.expect(prev_pp=Absent(), pp=266, hyper_id=140, price=200, clicktype=ClickType.EXTERNAL)
        self.click_log.expect(prev_pp=Absent(), pp=266, hyper_id=140, price=200, clicktype=ClickType.CPA)
        self.click_log.expect(prev_pp=Absent(), pp=266, hyper_id=110, price=1100, clicktype=ClickType.EXTERNAL)
        self.click_log.expect(prev_pp=Absent(), pp=266, hyper_id=110, price=1100, clicktype=ClickType.CPA)

        self.assertFragmentNotIn(response, {"model": {"id": 130}})  # модель только с cpc офферами не встретилась

        cpc_clicked_offer = response.root['analogs'][0]['offer']['cpc']
        cpc_clicked_model = response.root['analogs'][0]['model']['cpc']

        click_offer_request = (
            'place=productoffers&hyperid=120&offers-set=defaultList,list&show-card-analogs=1&rearr-factors=market_force_request_card_analogs=1;market_card_analogs_hide_models_in_output=0;split=empty;market_card_analog_min_base_score=0.09;market_card_analogs_dont_show_analogs_for_cpc_only=0;market_report_click_context_enabled=0&debug=1&cpc='  # noqa
            + cpc_clicked_offer
        )
        _ = self.report.request_json(click_offer_request)

        click_model_request = (
            'place=productoffers&hyperid=120&offers-set=defaultList,list&show-card-analogs=1&rearr-factors=market_force_request_card_analogs=1;market_card_analogs_hide_models_in_output=0;split=empty;market_card_analog_min_base_score=0.09;market_card_analogs_dont_show_analogs_for_cpc_only=0;market_report_click_context_enabled=0&debug=1&cpc='  # noqa
            + cpc_clicked_model
        )
        _ = self.report.request_json(click_model_request)

        self.show_log.expect(prev_pp=266).times(22)
        self.click_log.expect(prev_pp=266, clicktype=ClickType.EXTERNAL).times(14)
        self.click_log.expect(prev_pp=266, clicktype=ClickType.CPA).times(14)

    def test_model_with_analogs_default_list_cpa_model_on_touch(self):
        """
        Проверяем, что в ответ попадают модели-аналоги работают на таче с pp=660
        Проверяем, что не пишутся подробные логи, поскольку market_use_random_to_log_analogs_data_in_show_log_in_product_offers=1;market_random_threshold_to_log_analogs_data_in_show_log_in_product_offers=0.0  # noqa
        """

        request = 'place=productoffers&hyperid=120&offers-set=defaultList,list&show-card-analogs=1&rearr-factors=market_force_request_card_analogs=1;market_card_analogs_hide_models_in_output=0;market_use_random_to_log_analogs_data_in_show_log_in_product_offers=1;market_random_threshold_to_log_analogs_data_in_show_log_in_product_offers=0.0;split=empty;market_card_analog_min_base_score=0.09;market_card_analogs_dont_show_analogs_for_cpc_only=0&touch=1&debug=1'  # noqa
        _ = self.report.request_json(request)

        self.show_log.expect(pp=666, hyper_id=140, price=200, url_type=0, analogs_data=Absent())
        self.show_log.expect(pp=666, hyper_id=140, price=200, url_type=6, analogs_data=Absent())
        self.show_log.expect(pp=666, hyper_id=140, url_type=16, analogs_data=Absent())
        self.show_log.expect(pp=666, hyper_id=110, price=1100, url_type=0, analogs_data=Absent())
        self.show_log.expect(pp=666, hyper_id=110, price=1100, url_type=6, analogs_data=Absent())
        self.show_log.expect(pp=666, hyper_id=110, url_type=16, analogs_data=Absent())

        self.click_log.expect(pp=666, hyper_id=140, price=200, clicktype=ClickType.EXTERNAL)
        self.click_log.expect(pp=666, hyper_id=140, price=200, clicktype=ClickType.CPA)
        self.click_log.expect(pp=666, hyper_id=110, price=1100, clicktype=ClickType.EXTERNAL)
        self.click_log.expect(pp=666, hyper_id=110, price=1100, clicktype=ClickType.CPA)

    def test_model_with_analogs_default_list_cpa_model_with_sku_in_request(self):
        """
        Проверяем, что в ответ попадают модели-аналоги даже если в запросе есть параметр &market-sku
        """

        request = 'place=productoffers&hyperid=120&market-sku=120&offers-set=defaultList,list&show-card-analogs=1&rearr-factors=market_force_request_card_analogs=1;market_card_analogs_hide_models_in_output=0;split=empty;market_card_analog_min_base_score=0.09;market_card_analogs_score_rand_low=1.0;market_card_analogs_score_rand_delta=0.0;market_card_analogs_dont_show_analogs_for_cpc_only=0&debug=1'  # noqa
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "isAnalogOffer": False,
                "model": {"id": 120},
                "prices": {"value": "1200"},
                "benefit": NotEmpty(),
            },
        )  # ДО

        self.assertFragmentIn(
            response,
            {
                "analogs": [
                    {
                        "model": {"id": 140, "isAnalogModel": True},
                        "offer": {
                            "isAnalogOffer": True,
                            "cpa": "real",
                            "model": {"id": 140},
                            "prices": {"value": "200"},
                        },
                        "reason": "Price",
                    },
                    {
                        "model": {"id": 110, "isAnalogModel": True},
                        "offer": {
                            "isAnalogOffer": True,
                            "cpa": "real",
                            "model": {"id": 110},
                            "prices": {"value": "1100"},
                        },
                        "reason": "ModelRating",
                    },
                ]
            },
        )

        self.assertFragmentNotIn(response, {"model": {"id": 130}})  # модель только с cpc офферами не встретилась

    def test_model_with_analogs_default_list_cpc_model_with_sku_in_request(self):
        """
        Проверяем, что в ответ попадают модели-аналоги даже если в запросе есть параметр &market-sku
        """

        request = 'place=productoffers&hyperid=130&market-sku=130&offers-set=defaultList,list&show-card-analogs=1&rearr-factors=market_card_analogs_hide_models_in_output=0;split=empty;market_card_analog_min_base_score=0.09;market_card_analogs_dont_show_analogs_for_cpc_only=0&debug=1'  # noqa
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "isAnalogOffer": False,
                "model": {"id": 130},
                "prices": {"value": "1340"},
                "benefit": NotEmpty(),
            },
        )  # ДО
        self.assertFragmentIn(
            response,
            {
                "analogs": [
                    {
                        "model": {"id": 110, "isAnalogModel": True},
                        "offer": {
                            "isAnalogOffer": True,
                            "cpa": "real",
                            "model": {"id": 110},
                            "prices": {"value": "1100"},
                        },
                    }
                ]
            },
        )

    def test_model_without_analogs_default_list(self):
        """
        Проверяем, что без запроса аналогов (show-card-analogs=1 не передается) все корректно работает и аналоги не возвращаются
        """

        request = 'place=productoffers&hyperid=130&offers-set=defaultList,list&rearr-factors=market_card_analogs_hide_models_in_output=0;split=empty;market_card_analog_min_base_score=0.09&debug=1'
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "isAnalogOffer": False,
                "model": {"id": 130},
                "prices": {"value": "1340"},
                "benefit": NotEmpty(),
            },
        )  # ДО
        self.assertFragmentNotIn(response, {"analogs": NotEmpty()})

    def test_model_without_analogs_better_than_min_base_score(self):
        """
        Проверяем, что без запроса аналогов (show-card-analogs=1 не передается) все корректно работает и аналоги не возвращаются
        """

        request = 'place=productoffers&hyperid=130&offers-set=defaultList,list&show-card-analogs=1&rearr-factors=market_card_analogs_hide_models_in_output=0;split=empty;market_card_analog_min_base_score=0.99&debug=1'  # noqa
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "isAnalogOffer": False,
                "model": {"id": 130},
                "prices": {"value": "1340"},
                "benefit": NotEmpty(),
            },
        )  # ДО
        self.assertFragmentNotIn(response, {"analogs": NotEmpty()})

    def test_analog_for_model_with_express_delivery(self):
        """
        Проверяем, что для модели с экспресс-доставкой находится аналог. По этому тесту проще понять, верно ли настроена экспресс-доставка
        """

        # Если у модели экспресс-доставка, в запросе должны быть rids и rearr-factors=marker_express_delivery=1
        # &rids=213
        request = 'place=productoffers&hyperid=150&offers-set=defaultList,list&show-card-analogs=1&rearr-factors=market_force_request_card_analogs=1;market_card_analogs_hide_models_in_output=0;split=empty;market_card_analog_min_base_score=0.04&debug=1'  # noqa
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "isAnalogOffer": False,
                "model": {"id": 150},
                "prices": {"value": "1200"},
                "delivery": {"isExpress": True},
            },
        )
        # Находится аналог 140 (price = 200)
        self.assertFragmentIn(
            response,
            {
                "analogs": [
                    {
                        "model": {"id": 140, "isAnalogModel": True},
                        "offer": {
                            "isAnalogOffer": True,
                            "prices": {"value": "200"},
                        },
                        "reason": "Price",
                    }
                ]
            },
        )
        # У модели 140 нет экспресс-доставки
        self.assertFragmentNotIn(
            response,
            {"analogs": [{"model": {"id": 140, "isAnalogModel": True}, "offer": {"delivery": {"isExpress": True}}}]},
        )

    def test_analogs_with_express_delivery_both(self):
        """
        Проверяем аналоги для модели с экспресс-доставкой - reason не должно быть ExpressDelivery
        """

        # Если у модели экспресс-доставка, в запросе должны быть rids
        # &rids=213
        request = 'place=productoffers&hyperid=150&offers-set=defaultList,list&show-card-analogs=1&rearr-factors=market_force_request_card_analogs=1;market_card_analogs_hide_models_in_output=0;split=empty;market_card_analog_min_base_score=0.09&debug=1'  # noqa
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "isAnalogOffer": False,
                "model": {"id": 150},
                "prices": {"value": "1200"},
                "delivery": {"isExpress": True},
            },
        )
        # Есть аналог-дублёр 160 (price = 1200)
        self.assertFragmentNotIn(
            response,
            {
                "analogs": [
                    {
                        "model": {"id": 160, "isAnalogModel": True},
                        "offer": {
                            "isAnalogOffer": True,
                            "prices": {"value": "1200"},
                        },
                        # точно нельзя такой reason, т. к. Express Delivery есть у обеих моделей
                        "reason": "ExpressDelivery",
                    }
                ]
            },
        )

    def test_analog_with_express_delivery_reason(self):
        """
        Проверяем, что для модели без экспресс-доставки выбирается её клон с экспресс-доставкой,
        при этом reason = ExpressDelivery
        """
        request = 'place=productoffers&hyperid=120&offers-set=defaultList,list&show-card-analogs=1&rearr-factors=market_force_request_card_analogs=1;market_card_analogs_hide_models_in_output=0;split=empty;market_card_analog_min_base_score=0.09;market_card_analogs_dont_show_analogs_for_cpc_only=0&debug=1'  # noqa
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "isAnalogOffer": False,
                "model": {"id": 120},
                "prices": {"value": "1200"},
                "delivery": {"isExpress": False},
            },
        )
        self.assertFragmentIn(
            response,
            {
                "analogs": [
                    {
                        "model": {"id": 150, "isAnalogModel": True},
                        "offer": {"isAnalogOffer": True},
                        # точно нужно именно такой reason, т. к. Express Delivery - единственное отличие
                        "reason": "ExpressDelivery",
                    }
                ]
            },
        )

    def test_analog_for_cpa_without_opinions(self):
        """
        Проверяем, что для CPA модели без отзывов находится аналог
        """
        request = 'place=productoffers&hyperid=170&offers-set=top&show-card-analogs=1&rearr-factors=market_force_request_card_analogs=1;market_card_analogs_hide_models_in_output=0;split=empty;market_card_analog_min_base_score=0.09;market_card_analogs_seed=1281268736&debug=1'  # noqa
        response = self.report.request_json(request)
        self.assertFragmentNotIn(  # топ-6 не показывается без флага market_dont_request_cpc_if_card_analogs_found=0 если нашлись аналоги
            response,
            {
                "entity": "offer",
                "isAnalogOffer": False,
                "model": {"id": 170},
            },
        )
        self.assertFragmentIn(
            response,
            {
                "analogs": [
                    {
                        "model": {"id": 110, "isAnalogModel": True},
                        "offer": {"isAnalogOffer": True},
                    }
                ]
            },
        )
        self.assertFragmentIn(response, "CardAnalogs seed '1281268736'")

    def test_analog_for_cpc_without_opinions(self):
        """
        Проверяем, что для CPC модели без отзывов находится аналог
        """
        request = 'place=productoffers&hyperid=180&offers-set=top&show-card-analogs=1&rearr-factors=market_card_analogs_hide_models_in_output=0;split=empty;market_card_analog_min_base_score=0.09;market_card_analogs_dont_show_analogs_for_cpc_only=0&debug=1'  # noqa
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "analogs": [
                    {
                        "model": {"id": 110, "isAnalogModel": True},
                        "offer": {"isAnalogOffer": True},
                    }
                ]
            },
        )

    def test_for_cpa_analog_without_opinion(self):
        """
        Проверяем, что находится аналог без отзывов для CPA оффера
        """
        request = 'place=productoffers&hyperid=110&offers-set=top&show-card-analogs=1&rearr-factors=market_force_request_card_analogs=1;market_card_analogs_hide_models_in_output=0;split=empty;market_card_analog_min_base_score=0.09;market_card_analogs_dont_show_analogs_for_cpc_only=0&debug=1'  # noqa
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "analogs": [
                    {
                        "model": {"id": 170, "isAnalogModel": True},
                        "offer": {"isAnalogOffer": True},
                    }
                ]
            },
        )

    def test_for_cpc_analog_without_opinion(self):
        """
        Проверяем, что находится аналог без отзывов для CPC оффера
        """
        request = 'place=productoffers&hyperid=190&offers-set=top&show-card-analogs=1&rearr-factors=market_card_analogs_hide_models_in_output=0;split=empty;market_card_analog_min_base_score=0.09;market_card_analogs_dont_show_analogs_for_cpc_only=0&debug=1'  # noqa
        response = self.report.request_json(request)
        self.assertFragmentNotIn(  # топ-6 не показывается без флага market_dont_request_cpc_if_card_analogs_found=0 если нашлись аналоги
            response,
            {
                "entity": "offer",
                "isAnalogOffer": False,
                "model": {"id": 190},
            },
        )
        self.assertFragmentIn(
            response,
            {
                "analogs": [
                    {
                        "model": {"id": 170, "isAnalogModel": True},
                        "offer": {"isAnalogOffer": True},
                    }
                ]
            },
        )

    def test_scores_lower_limits(self):
        """
        Проверяем, что проигрышное свойство аналога не мешает ему быть рекомендованным по другим свойствам
        """
        request = 'place=productoffers&hyperid=190&offers-set=top&show-card-analogs=1&rearr-factors=market_card_analogs_hide_models_in_output=0;split=empty;market_card_analog_min_base_score=0.09;market_card_analogs_dont_show_analogs_for_cpc_only=0&debug=1'  # noqa
        response = self.report.request_json(request)
        self.assertFragmentNotIn(  # топ-6 не показывается без флага market_dont_request_cpc_if_card_analogs_found=0 если нашлись аналоги
            response, {"entity": "offer", "isAnalogOffer": False, "model": {"id": 190}, "benefit": Absent()}
        )
        self.assertFragmentIn(
            response,
            {
                "analogs": [
                    {
                        "model": {"id": 150, "isAnalogModel": True},
                        "offer": {"isAnalogOffer": True, "model": {"id": 150}},
                    }
                ]
            },
        )

    def test_three_analogs_with_one_reason(self):
        """
        Проверяем, что проигрышное свойство аналога не мешает ему быть рекомендованным по другим свойствам
        """
        request = 'place=productoffers&hyperid=200&offers-set=top&show-card-analogs=1&rearr-factors=market_force_request_card_analogs=1;market_card_analogs_hide_models_in_output=0;split=empty;market_card_analog_min_base_score=0.09;market_card_analogs_dont_show_analogs_for_cpc_only=0&debug=1'  # noqa
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "analogs": [
                    {
                        "model": {"id": 110, "isAnalogModel": True},
                        "offer": {"isAnalogOffer": True, "model": {"id": 110}},
                    },
                    {
                        "model": {"id": 120, "isAnalogModel": True},
                        "offer": {"isAnalogOffer": True, "model": {"id": 120}},
                    },
                    {
                        "model": {"id": 140, "isAnalogModel": True},
                        "offer": {"isAnalogOffer": True, "model": {"id": 140}},
                    },
                ]
            },
        )

    def test_model_with_50_analogs_card_analogs_count(self):
        """
        Проверяем, что
        1. модели с аналогами не теряются из-за ограчений других place, собирающих модели и оффера с базового
        2. работает параметр card-analogs-count
        """
        request = 'place=productoffers&hyperid=1000&offers-set=top&show-card-analogs=1&card-analogs-count=100&rearr-factors=market_force_request_card_analogs=1;market_card_analogs_hide_models_in_output=0;split=empty;market_card_analog_min_base_score=0.09&debug=1'  # noqa
        response = self.report.request_json(request)
        for i in range(1001, 1050):
            self.assertFragmentIn(
                response,
                {
                    "analogs": [
                        {
                            "model": {"id": i, "isAnalogModel": True},
                            "offer": {"isAnalogOffer": True, "model": {"id": i}},
                        }
                    ]
                },
            )

    def test_model_with_50_analogs_rearr_flag(self):
        """
        1. модели с аналогами не теряются из-за ограчений других place, собирающих модели и оффера с базового
        2. работает параметр market_default_card_analogs_count_in_responce
        """
        request = 'place=productoffers&hyperid=1000&offers-set=top&show-card-analogs=1&rearr-factors=market_force_request_card_analogs=1;market_card_analogs_hide_models_in_output=0;market_default_card_analogs_count_in_responce=100;split=empty;market_card_analog_min_base_score=0.09&debug=1'  # noqa
        response = self.report.request_json(request)
        for i in range(1001, 1050):
            self.assertFragmentIn(
                response,
                {
                    "analogs": [
                        {
                            "model": {"id": i, "isAnalogModel": True},
                            "offer": {"isAnalogOffer": True, "model": {"id": i}},
                        }
                    ]
                },
            )

    def test_analogs_choose_detail_info_logging(self):
        """
        Проверяем попадание analogs_choose_detail_info в логи (в зависимости от флагов)
        """
        request_base = (
            'place=productoffers&hyperid=120&offers-set=defaultList,list&show-card-analogs=1&debug=1&rearr-factors=%s'
        )

        def create_request(_rearr_flags_dict):
            _rearr_flags_str = dict_to_rearr(_rearr_flags_dict)
            return request_base % _rearr_flags_str

        # Проверка, что вывод в лог работает
        rearr_flags_dict = {
            "split": "empty",
            "market_card_analog_min_base_score": 0.09,
            "market_log_analog_choose_detail_info_in_show_log_in_product_offers": 1,
            "market_use_random_to_log_analogs_choose_detail_info_in_show_log_in_product_offers": 0,
            "market_log_analog_originals_cnt_to_show_log_in_choose_detail_info": 1,
            "market_log_do_cnt_to_show_log_in_choose_detail_info": 1,
            "market_card_analogs_hide_models_in_output": 0,
            "market_force_request_card_analogs": 1,
            "market_card_analogs_dont_show_analogs_for_cpc_only": 0,
        }
        request = create_request(rearr_flags_dict)
        self.report.request_json(request)

        self.show_log.expect(
            hyper_id=140, price=200, url_type=0, original_model_id=120, analogs_choose_detail_info="1,1"
        )
        self.show_log.expect(
            hyper_id=140, price=200, url_type=6, original_model_id=120, analogs_choose_detail_info="1,1"
        )
        self.show_log.expect(hyper_id=140, url_type=16, original_model_id=120, analogs_choose_detail_info="1,1")
        self.show_log.expect(
            hyper_id=110, price=1100, url_type=0, original_model_id=120, analogs_choose_detail_info="1,1"
        )
        self.show_log.expect(
            hyper_id=110, price=1100, url_type=6, original_model_id=120, analogs_choose_detail_info="1,1"
        )
        self.show_log.expect(hyper_id=110, url_type=16, original_model_id=120, analogs_choose_detail_info="1,1")

        # Проверка, что добавление кол-ва ДО можно отключить флагом
        rearr_flags_dict["market_log_do_cnt_to_show_log_in_choose_detail_info"] = 0
        request = create_request(rearr_flags_dict)
        self.report.request_json(request)
        self.show_log.expect(hyper_id=140, price=200, url_type=0, original_model_id=120, analogs_choose_detail_info="1")

        # Проверка, что добавление кол-ва оригиналов можно тоже отключить флагом
        rearr_flags_dict["market_log_do_cnt_to_show_log_in_choose_detail_info"] = 1
        rearr_flags_dict["market_log_analog_originals_cnt_to_show_log_in_choose_detail_info"] = 0
        request = create_request(rearr_flags_dict)
        self.report.request_json(request)
        self.show_log.expect(
            hyper_id=140, price=200, url_type=0, original_model_id=120, analogs_choose_detail_info=",1"
        )

        # Проверка, что логирование analogs_choose_detail_info отключается флагом
        rearr_flags_dict["market_log_do_cnt_to_show_log_in_choose_detail_info"] = 1
        rearr_flags_dict["market_log_analog_originals_cnt_to_show_log_in_choose_detail_info"] = 1
        rearr_flags_dict["market_log_analog_choose_detail_info_in_show_log_in_product_offers"] = 0
        request = create_request(rearr_flags_dict)
        self.report.request_json(request)
        self.show_log.expect(
            hyper_id=140,
            price=200,
            url_type=0,
            original_model_id=120,
            analogs_choose_detail_info=NoKey("analogs_choose_detail_info"),
        )

    def test_lose_by_express_delivery(self):
        """
        Проверяем, что аналог без экспресс доставки, но с более быстрой простой доставкой проигрывает оригиналу с экспресс доставкой
        """
        request = 'place=productoffers&hyperid=160&offers-set=top&show-card-analogs=1&rearr-factors=market_force_request_card_analogs=1;market_card_analogs_hide_models_in_output=0;split=empty;market_card_analog_min_base_score=0.09;market_analog_render_delivery_to_json=1&debug=1'  # noqa
        response = self.report.request_json(request)
        self.assertFragmentNotIn(response, {"analogs": [{"model": {"id": 800, "isAnalogModel": True}}]})

    def test_won_by_delivery_no_express(self):
        """
        Проверяем, что аналог рекомендуется по более быстрой обычной доставке, экспресс доставки нет ни у кого
        """
        request = 'place=productoffers&hyperid=801&rids=213&offers-set=top&show-card-analogs=1&rearr-factors=market_force_request_card_analogs=1;market_analog_create_delivery_for_score=1;market_card_analogs_hide_models_in_output=0;split=empty;market_card_analog_min_base_score=0.09;market_analog_render_delivery_to_json=1&debug=1'  # noqa
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "analogs": [
                    {
                        "model": {"id": 802, "isAnalogModel": True},
                        "offer": {"delivery": {"options": [{"dayFrom": 1, "dayTo": 1}]}},
                        "reason": "ExpressDelivery",
                    }
                ]
            },
        )
        # Проверка, что достаются верные сроки доставки (1 и 8 дней)
        self.assertFragmentIn(response, "; DeliveryDayTo= 1")
        # DeliveryScore = (8 - 1) / (8 + 1 + 0.01) = 0.77691...
        self.assertFragmentIn(response, "; DeliveryScore= 0.776915")

    def test_delivery_creation_for_score_disabling(self):
        """
        Проверяем, что у аналога по флагу 'market_analog_create_delivery_for_score' отключается создание доставки
        """
        request = 'place=productoffers&hyperid=801&rids=213&offers-set=top&show-card-analogs=1&rearr-factors=market_dont_request_cpc_if_card_analogs_found=0;market_force_request_card_analogs=1;market_card_analogs_hide_models_in_output=0;split=empty;market_card_analog_min_base_score=0.09;market_analog_render_delivery_to_json=1;market_analog_create_delivery_for_score=0&debug=1'  # noqa
        response = self.report.request_json(request)
        # модель из предыдущего теста не порекомендовалась, так как больше не выигрывает по доставке, так как доставку не достать
        self.assertFragmentNotIn(
            response,
            {
                "analogs": [
                    {
                        "model": {"id": 802, "isAnalogModel": True},
                        "offer": {"delivery": {"options": [{"dayFrom": 1, "dayTo": 1}]}},
                        "reason": "ExpressDelivery",
                    }
                ]
            },
        )
        # Проверка, что сроки доставки не достаются, так как выключен флаг
        self.assertFragmentIn(response, "; DeliveryDayTo= 255")
        # Проверка, что в этом случае скор нулевой, так как нет сроков доставки
        self.assertFragmentIn(response, "; DeliveryScore= 0")
        # Проверка, что у оригинала доставка достаётся доставка
        self.assertFragmentIn(response, {"model": {"id": 801}, "delivery": {"options": [{"dayFrom": 3, "dayTo": 8}]}})

    def test_switch_off_delivery_in_json_for_analogs(self):
        """
        Проверяем, что у аналога по флагу 'market_analog_render_delivery_to_json' отключается рендер доставки в json
        """
        request = 'place=productoffers&hyperid=801&rids=213&offers-set=top&show-card-analogs=1&rearr-factors=market_force_request_card_analogs=1;market_analog_create_delivery_for_score=1;market_card_analogs_hide_models_in_output=0;split=empty;market_card_analog_min_base_score=0.09;market_analog_render_delivery_to_json=0&debug=1'  # noqa
        response = self.report.request_json(request)
        # оффер-аналог рекомендуется, но его доставка не отрендерилась
        self.assertFragmentIn(
            response,
            {
                "analogs": [
                    {
                        "model": {"id": 802, "isAnalogModel": True},
                        "offer": {"delivery": Absent()},
                        "reason": "ExpressDelivery",
                    }
                ]
            },
        )

    def test_right_delivery_days(self):
        """
        Проверяем, что у аналога верные дни доставки (dayTo)
        """
        request = 'place=productoffers&hyperid=803&rids=213&offers-set=top&show-card-analogs=1&rearr-factors=market_force_request_card_analogs=1;market_analog_create_delivery_for_score=1;market_card_analogs_hide_models_in_output=0;split=empty;market_card_analog_min_base_score=0.09;market_analog_render_delivery_to_json=1&debug=1'  # noqa
        response = self.report.request_json(request)
        # оффер-аналог рекомендуется, но его доставка не отрендерилась
        self.assertFragmentIn(
            response,
            {
                "analogs": [
                    {
                        "model": {"id": 110, "isAnalogModel": True},
                        "offer": {"delivery": {"options": [{"dayFrom": 2, "dayTo": 4}]}},
                    }
                ]
            },
        )

    def test_analog_worse_delivery(self):
        """
        Проверяем, что у аналога c худшим скором не получается гигантский скор доставки из-за переполнения
        """
        request = 'place=productoffers&hyperid=800&rids=213&offers-set=top&show-card-analogs=1&rearr-factors=market_force_request_card_analogs=1;market_analog_create_delivery_for_score=1;market_card_analogs_hide_models_in_output=0;split=empty;market_card_analog_min_base_score=0.09&debug=1'  # noqa
        response = self.report.request_json(request)

        self.assertFragmentNotIn(response, "; DeliveryScore= 8.57279e+08")
        self.assertFragmentIn(response, "; DeliveryScore= -0.3")

    def test_analogs_for_empty_model(self):
        """
        Проверяем, что для пустой cpc-модели тоже находятся аналоги
        """
        # Сначала просто проверяем, что аналоги находятся для случая "нет в продаже"
        request = 'place=productoffers&hyperid=210&offers-set=top&show-card-analogs=1&rearr-factors=market_card_analogs_hide_models_in_output=0;split=empty;market_card_analog_min_base_score=0.09;market_card_analogs_model_min_good_rating=0.3&debug=1'  # noqa
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "analogs": [
                    {
                        "model": {"id": 800, "isAnalogModel": True},
                        "offer": {"isAnalogOffer": True, "model": {"id": 800}},
                    }
                ]
            },
        )
        # Проверяем, что аналоги находятся даже, когда есть параметр cpa=real (ведь модель без офферов)
        response = self.report.request_json(
            request + '&cpa=real&rearr-factors=market_competitive_model_card_fix_cpa_real=1'
        )
        self.assertFragmentIn(
            response,
            {
                "analogs": [
                    {
                        "model": {"id": 800, "isAnalogModel": True},
                        "offer": {"isAnalogOffer": True, "model": {"id": 800}},
                    }
                ]
            },
        )
        # Тут проверяем, что работает флаг фикса (а точнее, что флаг можно вырубить и вернуть, как было)
        response = self.report.request_json(
            request + '&cpa=real&rearr-factors=market_competitive_model_card_fix_cpa_real=0'
        )
        self.assertFragmentIn(response, "No analog model has pass all filters")

    def test_no_model_in_analogs_output(self):
        """
        Проверяем, что по-умолчанию (без market_card_analogs_hide_models_in_output) инфа о моделях в аналогах не выводится
        Проверяем, что аналоги ищутся всегдаа если включен market_enable_card_analogs_without_cgi=1
        """
        request = 'place=productoffers&hyperid=210&offers-set=top&rearr-factors=market_enable_card_analogs_without_cgi=1;split=empty;market_card_analog_min_base_score=0.09;market_card_analogs_model_min_good_rating=0.3&debug=1'  # noqa
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "analogs": [
                    {
                        "offer": {"isAnalogOffer": True, "model": {"id": 800}},
                    }
                ]
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                "analogs": [
                    {
                        "model": {"id": 800, "isAnalogModel": True},
                        "offer": {"isAnalogOffer": True, "model": {"id": 800}},
                    }
                ]
            },
        )


if __name__ == '__main__':
    main()
