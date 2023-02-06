#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import (
    BlueOffer,
    DynamicBlueGenericBundlesPromos,
    DynamicDeliveryRestriction,
    DynamicWarehouseDelivery,
    DynamicWarehouseLink,
    Elasticity,
    MarketSku,
    Promo,
    PromoType,
    Region,
    Shop,
)
from core.types.dynamic_filters import DynamicPromoSecondaries
from core.types.offer_promo import (
    PromoBlueFlash,
    OffersMatchingRules,
)
from core.types.buybox_settings import BuyboxSettings, BuyboxExceptionSettings


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.regiontree += [
            Region(
                rid=1,
                name='Московская область',
                region_type=Region.FEDERATIVE_SUBJECT,
                children=[
                    Region(rid=213, name='Москва'),
                ],
            )
        ]

        cls.index.shops += [
            Shop(
                fesh=13,
                datafeed_id=13,
                priority_region=213,
                regions=[225],
                name="3P поставщик Петя",
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
            ),
            Shop(
                fesh=31,
                datafeed_id=31,
                priority_region=213,
                regions=[225],
                name="3P поставщик Вася",
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
            ),
            Shop(
                fesh=17,
                datafeed_id=17,
                priority_region=213,
                regions=[225],
                name="Кроссдок поставщик",
                supplier_type=Shop.FIRST_PARTY,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
            ),
        ]

        promoOffer1 = BlueOffer(waremd5='pnO6jtfjEy9AfE4RIpBsnQ', offerid='promoOffer1', price=122, fesh=17, feedid=17)
        promoOffer2 = BlueOffer(waremd5='ozmCtRBXgUJgvxo4kHPBzg', offerid='promoOffer2', price=144, fesh=17, feedid=17)

        promo1 = Promo(
            promo_type=PromoType.BLUE_FLASH,
            key='Promo_blue_flash',
            url='http://blue_cashback.com/',
            blue_flash=PromoBlueFlash(
                items=[
                    {'feed_id': 17, 'offer_id': promoOffer1.offerid, 'price': {'value': 110, 'currency': 'RUR'}},
                    {'feed_id': 17, 'offer_id': promoOffer2.offerid, 'price': {'value': 110, 'currency': 'RUR'}},
                ],
            ),
            offers_matching_rules=[
                OffersMatchingRules(feed_offer_ids=[[17, promoOffer1.offerid], [17, promoOffer2.offerid]])
            ],
        )
        promoOffer1.promo = [promo1]
        promoOffer2.promo = [promo1]

        cls.index.mskus += [
            MarketSku(
                hyperid=1,
                title='blue market sku1',
                sku=1,
                buybox_elasticity=[
                    Elasticity(price_variant=100, demand_mean=200),
                    Elasticity(price_variant=200, demand_mean=80),
                    Elasticity(price_variant=300, demand_mean=10),
                ],
                blue_offers=[
                    BlueOffer(waremd5='EpnWVxDQxj4wg7vVI1ElnA', price=120, feedid=13),
                    BlueOffer(waremd5='xMpCOKC5I4INzFCab3WEmQ', price=160, feedid=31),
                ],
            ),
            MarketSku(
                title='blue market sku2',
                hyperid=2,
                hid=100,
                sku=2,
                buybox_elasticity=[
                    Elasticity(price_variant=100, demand_mean=200),
                    Elasticity(price_variant=200, demand_mean=80),
                    Elasticity(price_variant=300, demand_mean=10),
                ],
                blue_offers=[
                    BlueOffer(waremd5='BH8EPLtKmdLQhLUasgaOnA', price=120, feedid=13),
                    BlueOffer(waremd5='otENNVzevIeeT8bsxvY91w', price=125, feedid=13),
                    BlueOffer(waremd5='nx1WWdWID7Qn9uBK5QD8JQ', price=160, feedid=31),
                ],
            ),
            MarketSku(
                title='blue market sku3',
                hyperid=3,
                hid=300,
                sku=3,
                buybox_elasticity=[
                    Elasticity(price_variant=100, demand_mean=200),
                    Elasticity(price_variant=200, demand_mean=80),
                    Elasticity(price_variant=300, demand_mean=10),
                ],
                blue_offers=[
                    BlueOffer(waremd5='KXGI8T3GP_pqjgdd7HfoHQ', price=120, feedid=13),
                    BlueOffer(waremd5='jtz2CzErBm3oY90EJVeFSg', price=122, feedid=31),
                ],
            ),
            MarketSku(
                title='blue market sku4',
                hyperid=4,
                hid=400,
                sku=4,
                buybox_elasticity=[
                    Elasticity(price_variant=100, demand_mean=200),
                    Elasticity(price_variant=200, demand_mean=80),
                    Elasticity(price_variant=300, demand_mean=10),
                ],
                blue_offers=[
                    BlueOffer(waremd5='jsFnEBncNV6VLkT9w4BajQ', price=120, feedid=13),
                    promoOffer1,
                    promoOffer2,
                ],
            ),
            MarketSku(
                title='blue market sku5',
                hyperid=5,
                hid=500,
                sku=5,
                buybox_elasticity=[
                    Elasticity(price_variant=100, demand_mean=200),
                    Elasticity(price_variant=200, demand_mean=80),
                    Elasticity(price_variant=300, demand_mean=10),
                ],
                blue_offers=[
                    BlueOffer(waremd5='22222222222222gggggggg', price=120, feedid=31),
                    BlueOffer(waremd5='11111111111111gggggggg', price=125, feedid=13),
                    BlueOffer(waremd5='33333333333333gggggggg', price=130, feedid=31),
                ],
            ),
            MarketSku(
                title='blue market sku6',
                hyperid=6,
                hid=1000,
                sku=6,
                buybox_elasticity=[
                    Elasticity(price_variant=100, demand_mean=200),
                    Elasticity(price_variant=200, demand_mean=80),
                    Elasticity(price_variant=300, demand_mean=10),
                ],
                blue_offers=[
                    BlueOffer(waremd5='1sFnEBncNV6VLkT9w4BajQ', price=120, feedid=31),
                    BlueOffer(waremd5='dfslkm12dV6VLkT9w4Bajg', price=125, feedid=13),
                    BlueOffer(waremd5='fslniBnssVdVjkb9whghjg', price=130, feedid=31),
                ],
            ),
        ]

        cls.index.promos += [
            promo1,
        ]
        cls.dynamic.promo_secondaries += [
            DynamicPromoSecondaries(promos=cls.index.promos),
        ]

        cls.settings.loyalty_enabled = True
        cls.dynamic.loyalty += [DynamicBlueGenericBundlesPromos(whitelist=[promo.key for promo in cls.index.promos])]

        cls.index.buybox_settings += [
            BuyboxSettings(
                category_id=100, exchange=1, max_price_rel=2, max_price_diff=5000, max_gmv_rel=3, enable_flag=True
            ),
            BuyboxSettings(
                category_id=1000,
                fby_conv_coef=0.91,
                fbs_conv_coef=0.92,
                dbs_conv_coef=0.93,
                express_conv_coef=0.94,
                experiment_id=1234,
            ),
            BuyboxSettings(
                category_id=1000,
                fby_conv_coef=0.81,
                fbs_conv_coef=0.82,
                dbs_conv_coef=0.83,
                express_conv_coef=0.84,
                experiment_id=0,
            ),
        ]

        cls.index.buybox_exception_settings += [
            BuyboxExceptionSettings(
                category_id=300,
                msku_id=3,
                region_id=0,
                promo_type="",
                min_price=0,
                max_price=0,
                exchange=1,
                max_price_rel=2,
                max_price_diff=5000,
                max_gmv_rel=3,
                enable_flag=False,
                promo_offer_always_win=False,
                waremd5='',
            ),
            BuyboxExceptionSettings(
                category_id=400,
                msku_id="",
                region_id=0,
                promo_type="",
                min_price=0,
                max_price=0,
                exchange=1,
                max_price_rel=2,
                max_price_diff=5000,
                max_gmv_rel=3,
                enable_flag=False,
                promo_offer_always_win=True,
                waremd5='',
            ),
            BuyboxExceptionSettings(
                category_id=0,
                msku_id="5",
                region_id=0,
                promo_type="",
                min_price=0,
                max_price=0,
                exchange=1,
                max_price_rel=2,
                max_price_diff=5000,
                max_gmv_rel=3,
                enable_flag=True,
                promo_offer_always_win=False,
                waremd5='33333333333333gggggggg',
            ),
        ]

    @classmethod
    def prepare_nordstream(cls):
        cls.settings.nordstream_autogenerate = False
        for warehouse_id in [145]:
            cls.dynamic.nordstream += [DynamicWarehouseLink(warehouse_id, [warehouse_id])]
            cls.dynamic.nordstream += [
                DynamicWarehouseDelivery(
                    warehouse_id,
                    {
                        225: [
                            DynamicDeliveryRestriction(min_days=1, max_days=2, cost=15),
                        ],
                    },
                ),
            ]

    def test_buybox_settings_market_blue_buybox_by_gmv_ue_false(self):
        """Запрос с market_blue_buybox_by_gmv_ue=0. Должен работать старый байбокс"""
        response = self.report.request_json(
            'place=productoffers&offers-set=defaultList&pp=1&market-sku=1&debug=da&rearr-factors=market_blue_buybox_by_gmv_ue=0;market_blue_buybox_price_rel_max_threshold=1000'
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "wareId": "EpnWVxDQxj4wg7vVI1ElnA",
                'debug': {
                    'buyboxDebug': {
                        "Won": 1,
                        "WonMethod": "OLD_BUYBOX",
                        "Offers": [
                            {"WareMd5": "EpnWVxDQxj4wg7vVI1ElnA"},
                            {"WareMd5": "xMpCOKC5I4INzFCab3WEmQ"},
                        ],
                        "Settings": {"EnableFlag": False},
                    }
                },
            },
        )

    def test_buybox_settings_too_high_price(self):
        """Запрос с rearr фактором. Оффер не проходит по цене"""
        response = self.report.request_json(
            'place=productoffers&offers-set=defaultList&pp=1&market-sku=1&debug=da&rearr-factors=market_blue_buybox_by_gmv_ue_with_delivery=0;market_blue_buybox_max_price_rel_add_diff=0'
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "wareId": "EpnWVxDQxj4wg7vVI1ElnA",
                "debug": {
                    "buyboxDebug": {
                        "WonMethod": "SINGLE_OFFER_AFTER_BUYBOX_FILTERS",
                        "Offers": [{"WareMd5": "EpnWVxDQxj4wg7vVI1ElnA"}],
                        "RejectedOffers": [
                            {"RejectReason": "TOO_HIGH_PRICE", "Offer": {"WareMd5": "xMpCOKC5I4INzFCab3WEmQ"}}
                        ],
                    }
                },
            },
        )

    def test_buybox_settings_max_price_rel_2(self):
        """
        Запрос SKU = 2. Для него задана настройка раздела buybox_settings, с параметром max_price_rel = 2.
        В этом случае, оффер не отсеивается
        """
        response = self.report.request_json(
            'place=productoffers&offers-set=defaultList&pp=1&market-sku=2&debug=da&rearr-factors=market_blue_buybox_by_gmv_ue_with_delivery=0;market_blue_buybox_management=1;market_blue_buybox_price_rel_max_threshold=2'  # noqa
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "debug": {
                    "buyboxDebug": {
                        "WonMethod": "WON_BY_EXCHANGE",
                        "Offers": [
                            {"WareMd5": "BH8EPLtKmdLQhLUasgaOnA"},
                            {"WareMd5": "nx1WWdWID7Qn9uBK5QD8JQ"},
                            {"WareMd5": "otENNVzevIeeT8bsxvY91w"},
                        ],
                        "Settings": {"EnableFlag": True, "MaxPriceRel": 2},
                    }
                },
            },
        )
        self.assertFragmentIn(response, "Buybox settings found in buybox_settings.tsv")

    def test_buybox_settings_override(self):
        """
        Запрос SKU = 2. Проверяем, что rearr факторы переопределяют настройки BuyboxSettings
        """
        response = self.report.request_json(
            'place=productoffers&offers-set=defaultList&pp=1&market-sku=2&debug=da&rearr-factors=market_blue_buybox_by_gmv_ue_with_delivery=0;'
            'market_blue_buybox_management=1;market_blue_buybox_max_price_rel=1;market_blue_buybox_max_price_rel_add_diff=0'
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "wareId": "BH8EPLtKmdLQhLUasgaOnA",
                "debug": {
                    "buyboxDebug": {
                        "WonMethod": "SINGLE_OFFER_AFTER_BUYBOX_FILTERS",
                        "Offers": [
                            {"WareMd5": "BH8EPLtKmdLQhLUasgaOnA"},
                        ],
                        "RejectedOffers": [
                            {"RejectReason": "TOO_HIGH_PRICE", "Offer": {"WareMd5": "nx1WWdWID7Qn9uBK5QD8JQ"}},
                            {"RejectReason": "TOO_HIGH_PRICE", "Offer": {"WareMd5": "otENNVzevIeeT8bsxvY91w"}},
                        ],
                        "Settings": {"EnableFlag": True, "MaxPriceRel": 1},
                    }
                },
            },
        )
        self.assertFragmentIn(response, "Buybox settings found in buybox_settings.tsv")

    def test_buybox_settings_no_management(self):
        """
        Запрос SKU = 2. с отключенным оперативным управлением. Параметры размена берутся по умолчанию,
        поэтому один оффер отклоняется по слишком большой цене.
        """
        response = self.report.request_json(
            'place=productoffers&offers-set=defaultList&pp=1&market-sku=2&debug=da&rearr-factors=market_blue_buybox_by_gmv_ue_with_delivery=0;'
            'market_blue_buybox_max_price_rel_add_diff=0;market_blue_buybox_management=0;'
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "debug": {
                    "buyboxDebug": {
                        "WonMethod": "WON_BY_EXCHANGE",
                        "Offers": [
                            {"WareMd5": "BH8EPLtKmdLQhLUasgaOnA"},
                            {"WareMd5": "otENNVzevIeeT8bsxvY91w"},
                        ],
                        "RejectedOffers": [
                            {"RejectReason": "TOO_HIGH_PRICE", "Offer": {"WareMd5": "nx1WWdWID7Qn9uBK5QD8JQ"}}
                        ],
                        "Settings": {"EnableFlag": True, "MaxPriceRel": 1.05},
                    }
                },
            },
        )
        self.assertFragmentNotIn(response, "Buybox settings found in buybox_settings.tsv")

    def test_buybox_exception_settings(self):
        """
        Запрос SKU = 3. Для него задана настройка в файле buybox_exception_settings, с параметром enable_flag = False.
        В этом случае используется старый байбокс
        """
        response = self.report.request_json(
            'place=productoffers&offers-set=defaultList&pp=1&market-sku=3&debug=da&rearr-factors=market_debug_buybox=1;market_blue_buybox_by_gmv_ue_with_delivery=0;market_blue_buybox_management=1'  # noqa
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "wareId": "KXGI8T3GP_pqjgdd7HfoHQ",
                "debug": {
                    "buyboxDebug": {
                        "WonMethod": "OLD_BUYBOX",
                        "Offers": [{"WareMd5": "KXGI8T3GP_pqjgdd7HfoHQ"}, {"WareMd5": "jtz2CzErBm3oY90EJVeFSg"}],
                        "Settings": {"EnableFlag": False},
                    }
                },
            },
        )
        self.assertFragmentIn(response, "Buybox settings found in buybox_exceptions.tsv")

    def test_buybox_waremd5(self):
        """
        Запрос SKU = 5. У этого sku прописан Waremd5 оффера, который должен выиграть байбокс
        """
        response = self.report.request_json(
            'place=productoffers&offers-set=defaultList&pp=1&market-sku=5&debug=da&rearr-factors=market_debug_buybox=1;market_blue_buybox_by_gmv_ue_with_delivery=0;market_blue_buybox_management=1'  # noqa
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "wareId": "33333333333333gggggggg",
                "debug": {
                    "buyboxDebug": {
                        "WonMethod": "WON_BY_WAREMD5_SETTINGS",
                        "Offers": [
                            {"WareMd5": "22222222222222gggggggg"},
                            {"WareMd5": "11111111111111gggggggg"},
                            {"WareMd5": "33333333333333gggggggg"},
                        ],
                        "Settings": {"EnableFlag": True, "BuyboxWaremd5": "33333333333333gggggggg"},
                    }
                },
            },
        )
        self.assertFragmentIn(response, "Buybox settings found in buybox_exceptions.tsv")

    def test_buybox_by_gmv_ue_coef_settings(self):
        """
        Запрос SKU = 2. Для него задана настройка раздела buybox_settings, с параметром max_price_rel = 2.
        В этом случае, оффер не отсеивается
        """
        response = self.report.request_json(
            'place=productoffers&offers-set=defaultList&pp=1&market-sku=2&yandexuid=1&debug=da&rearr-factors=market_blue_buybox_by_gmv_ue_with_delivery=0;market_blue_buybox_management=1;market_blue_buybox_gmv_ue_mix_coef=1;market_blue_buybox_price_rel_max_threshold=2'  # noqa
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "wareId": "otENNVzevIeeT8bsxvY91w",
                "debug": {
                    "buyboxDebug": {
                        "WonMethod": "WON_BY_EXCHANGE",
                        "Offers": [
                            {"WareMd5": "BH8EPLtKmdLQhLUasgaOnA"},
                            {"WareMd5": "nx1WWdWID7Qn9uBK5QD8JQ"},
                            {"WareMd5": "otENNVzevIeeT8bsxvY91w"},
                        ],
                        "Settings": {"EnableFlag": True, "MaxPriceRel": 2},
                    }
                },
            },
        )
        self.assertFragmentIn(response, "Buybox settings found in buybox_settings.tsv")

    def test_buybox_by_gmv_ue_coef_settings_usual_behavior(self):
        """
        Запрос SKU = 2. Для него задана настройка раздела buybox_settings, с параметром max_price_rel = 2.
        Однако оффер все равно отсеивается, поскольку максимальное превышение цены 20% (из-за дефолтной эластичности)
        """
        response = self.report.request_json(
            'place=productoffers&offers-set=defaultList&pp=1&market-sku=2&yandexuid=1&debug=da&rearr-factors=market_debug_buybox=1;market_blue_buybox_by_gmv_ue_with_delivery=0;market_blue_buybox_management=1;market_blue_buybox_gmv_ue_mix_coef=1'  # noqa
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "wareId": "otENNVzevIeeT8bsxvY91w",
                "debug": {
                    "buyboxDebug": {
                        "WonMethod": "WON_BY_EXCHANGE",
                        "Offers": [
                            {"WareMd5": "BH8EPLtKmdLQhLUasgaOnA"},
                            {"WareMd5": "otENNVzevIeeT8bsxvY91w"},
                        ],
                        "RejectedOffers": [
                            {"RejectReason": "TOO_HIGH_PRICE", "Offer": {"WareMd5": "nx1WWdWID7Qn9uBK5QD8JQ"}}
                        ],
                        "Settings": {"EnableFlag": True, "MaxPriceRel": 2},
                    }
                },
            },
        )
        self.assertFragmentIn(response, "Buybox settings found in buybox_settings.tsv")

    def test_buybox_settings_exp(self):
        # Проверяем, что без флага market_blue_buybox_settings_exp считываются настройки с experiment_id = 0
        response = self.report.request_json(
            'place=productoffers&offers-set=defaultList&pp=6&market-sku=6&yandexuid=1&debug=da&rearr-factors=market_blue_buybox_management=1'  # noqa
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "wareId": "fslniBnssVdVjkb9whghjg",
                "debug": {
                    "buyboxDebug": {
                        "WonMethod": "WON_BY_EXCHANGE",
                        "Offers": [
                            {"WareMd5": "1sFnEBncNV6VLkT9w4BajQ"},
                            {"WareMd5": "dfslkm12dV6VLkT9w4Bajg"},
                            {"WareMd5": "fslniBnssVdVjkb9whghjg"},
                        ],
                        "Settings": {
                            "ExperimentId": 0,
                            "FbyConvCoef": 0.81,
                            "FbsConvCoef": 0.82,
                            "DbsConvCoef": 0.83,
                            "ExpressConvCoef": 0.84,
                            "MaxPriceDiff": 3000,  # В BuyboxSettings поле max_price_diff не задано, поэтому берем дефолтное значение (3000).
                            "EnableFlag": True,  # В BuyboxSettings поле enable_flag не задано, поэтому берем дефолтное значение (True).
                        },
                    }
                },
            },
        )
        self.assertFragmentIn(response, "Buybox settings found in buybox_settings.tsv")

        # Проверяем, что с флагом market_blue_buybox_settings_exp=1234 считываются настройки с experiment_id = 1234
        response = self.report.request_json(
            'place=productoffers&offers-set=defaultList&pp=6&market-sku=6&yandexuid=1&debug=da&rearr-factors=market_blue_buybox_management=1;market_blue_buybox_settings_exp=1234'  # noqa
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "wareId": "fslniBnssVdVjkb9whghjg",
                "debug": {
                    "buyboxDebug": {
                        "WonMethod": "WON_BY_EXCHANGE",
                        "Offers": [
                            {"WareMd5": "1sFnEBncNV6VLkT9w4BajQ"},
                            {"WareMd5": "dfslkm12dV6VLkT9w4Bajg"},
                            {"WareMd5": "fslniBnssVdVjkb9whghjg"},
                        ],
                        "Settings": {
                            "ExperimentId": 1234,
                            "FbyConvCoef": 0.91,
                            "FbsConvCoef": 0.92,
                            "DbsConvCoef": 0.93,
                            "ExpressConvCoef": 0.94,
                        },
                    }
                },
            },
        )
        self.assertFragmentIn(response, "Buybox settings found in buybox_settings.tsv")

        # Проверяем, что в топ6 при группировке по business id при вклченном флаге market_blue_buybox_settings_exp=1234 считываются настройки с experiment_id = 1234
        response = self.report.request_json(
            'place=productoffers&market-sku=6&pp=6&grhow=supplier&rearr-factors=market_uncollapse_supplier=1;enable_business_id=1;'
            'market_blue_buybox_management=1;market_blue_buybox_settings_exp=1234;market_enable_buybox_by_business=1;&debug=da'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            "entity": "offer",
                            "wareId": "fslniBnssVdVjkb9whghjg",
                            "debug": {
                                "buyboxDebug": {
                                    "Offers": [
                                        {"WareMd5": "1sFnEBncNV6VLkT9w4BajQ", "BuyboxFilter": "BUYBOX_BY_BUSINESS"},
                                        {"WareMd5": "fslniBnssVdVjkb9whghjg", "BuyboxFilter": "BUYBOX_BY_BUSINESS"},
                                    ],
                                    "Settings": {
                                        "ExperimentId": 1234,
                                        "FbyConvCoef": 0.91,
                                        "FbsConvCoef": 0.92,
                                        "DbsConvCoef": 0.93,
                                        "ExpressConvCoef": 0.94,
                                    },
                                }
                            },
                        },
                        {
                            "entity": "offer",
                            "wareId": "dfslkm12dV6VLkT9w4Bajg",
                            "debug": {
                                "buyboxDebug": {
                                    "Offers": [
                                        {"WareMd5": "dfslkm12dV6VLkT9w4Bajg", "BuyboxFilter": "BUYBOX_BY_BUSINESS"},
                                    ],
                                    "Settings": {
                                        "ExperimentId": 1234,
                                        "FbyConvCoef": 0.91,
                                        "FbsConvCoef": 0.92,
                                        "DbsConvCoef": 0.93,
                                        "ExpressConvCoef": 0.94,
                                    },
                                }
                            },
                        },
                    ]
                }
            },
        )
        self.assertFragmentIn(response, "Buybox settings found in buybox_settings.tsv")


if __name__ == '__main__':
    main()
