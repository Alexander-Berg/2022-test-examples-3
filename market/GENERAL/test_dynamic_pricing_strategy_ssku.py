#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BlueOffer,
    DynamicPriceControlData,
    GLParam,
    MarketSku,
    RegionalMsku,
    RtyOffer,
    Shop,
)
from core.testcase import TestCase, main
from core.types.dynamic_pricing_strategy_ssku import DynamicPricingStrategySSKU, DYNAMIC_PRICING_TYPE
from core.types.autogen import b64url_md5
from market.proto.common.promo_pb2 import ESourceType
from core.types.offer_promo import OffersMatchingRules, Promo, PromoDirectDiscount, PromoType


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.creation_time = 222
        cls.settings.rty_qpipe = True
        cls.settings.report_subrole = 'main'

        promo1 = Promo(
            promo_type=PromoType.DIRECT_DISCOUNT,
            feed_id=302,
            key=b64url_md5(3003),
            url='http://direct_discount_1.com/',
            shop_promo_id='promo1',
            source_type=ESourceType.DCO_3P_DISCOUNT,
            direct_discount=PromoDirectDiscount(
                items=[
                    {
                        'feed_id': 302,
                        'offer_id': "3003",
                        'discount_price': {'value': 700, 'currency': 'RUR'},
                        'old_price': {'value': 1000, 'currency': 'RUR'},
                        'max_discount': {'value': 1000, 'currency': 'RUR'},
                        'max_discount_percent': 100.0,
                    },
                ],
                allow_berubonus=True,
                allow_promocode=True,
            ),
            offers_matching_rules=[
                OffersMatchingRules(
                    feed_offer_ids=[
                        [3003, "3003"],
                    ]
                )
            ],
        )

        cls.index.mskus += [
            MarketSku(
                sku=2,
                hid=3,
                blue_offers=[
                    # оффер только со стратегией на весь ассортимент
                    BlueOffer(hid=3, price=1000, fesh=21, feedid=201, offerid=2002, ref_min_price=840),
                    # оффер со стратегией, приходящей из индексатора
                    BlueOffer(
                        hid=3,
                        price=1000,
                        fesh=22,
                        feedid=202,
                        offerid=2003,
                        ref_min_price=810,
                        dynamic_pricing_data=DynamicPricingStrategySSKU(
                            dynamic_pricing_type=DYNAMIC_PRICING_TYPE.MINREF,
                            dynamic_pricing_threshold_is_percent=True,
                            dynamic_pricing_threshold_value=22,
                        ),
                    ),
                    # оффер со стратегией, приходящей из RTY
                    BlueOffer(hid=3, price=1000, fesh=23, feedid=203, offerid=2004, ref_min_price=970),
                    # оффер с общей стратегией и со стратегией приходящей через RTY
                    BlueOffer(hid=3, price=1000, fesh=21, feedid=204, offerid=2005, ref_min_price=710),
                    # оффер со стратегией, приходящей из индексатора, которая не сработает, так как минреф недостижим
                    BlueOffer(
                        hid=3,
                        price=1000,
                        fesh=22,
                        feedid=204,
                        offerid=2006,
                        ref_min_price=810,
                        dynamic_pricing_data=DynamicPricingStrategySSKU(
                            dynamic_pricing_type=DYNAMIC_PRICING_TYPE.MINREF,
                            dynamic_pricing_threshold_is_percent=True,
                            dynamic_pricing_threshold_value=15.31,
                        ),
                    ),
                    # оффер со стратегией приходящей через RTY c нулевой скидкой
                    BlueOffer(hid=3, price=1000, fesh=25, feedid=205, offerid=2007, ref_min_price=710),
                ],
            ),
            MarketSku(
                sku=3,
                hid=4,
                blue_offers=[
                    BlueOffer(hid=4, price=800, fesh=30, feedid=301, offerid=3002, use_custom_regional_stats=True),
                    # оффер со стратегией, приходящей из индексатора
                    BlueOffer(
                        hid=4,
                        price=1000,
                        fesh=31,
                        feedid=302,
                        offerid=3003,
                        promo=promo1,
                        dynamic_pricing_data=DynamicPricingStrategySSKU(
                            dynamic_pricing_type=DYNAMIC_PRICING_TYPE.BUYBOX,
                            dynamic_pricing_threshold_is_percent=True,
                            dynamic_pricing_threshold_value=22,
                        ),
                        use_custom_regional_stats=True,
                    ),
                    BlueOffer(
                        hid=4,
                        price=790,
                        fesh=30,
                        feedid=301,
                        offerid=3007,
                        is_resale=True,
                        use_custom_regional_stats=True,
                    ),
                ],
            ),
            MarketSku(
                sku=4,
                hid=5,
                blue_offers=[
                    BlueOffer(
                        hid=5,
                        price=2775,
                        fesh=32,
                        feedid=303,
                        offerid=3004,
                        dynamic_pricing_data=DynamicPricingStrategySSKU(
                            dynamic_pricing_type=DYNAMIC_PRICING_TYPE.BUYBOX,
                            dynamic_pricing_threshold_is_percent=False,
                            dynamic_pricing_threshold_value=199.98,
                        ),
                    ),
                    BlueOffer(
                        hid=5,
                        price=2576,
                        fesh=33,
                        feedid=304,
                        offerid=3005,
                    ),
                ],
            ),
            MarketSku(
                sku=5,
                hid=6,
                hyperid=666,
                blue_offers=[
                    BlueOffer(
                        hid=6,
                        price=11990,
                        fesh=34,
                        feedid=305,
                        offerid=3005,
                        waremd5='gTL-3D5IXpiHAL-CvNRmNQ',
                    ),
                    BlueOffer(
                        hid=6,
                        price=9990,
                        fesh=35,
                        feedid=306,
                        offerid=3006,
                        waremd5='GTL-3D5IXpiHAL-CvNRmNQ',
                    ),
                ],
            ),
            MarketSku(
                title="msku_3",
                hyperid=33,
                sku=300001,
                hid=33,
                blue_offers=[
                    BlueOffer(
                        fesh=432,
                        price=106,
                        price_old=200,
                        feedid=402,
                        waremd5="BLUE-300001-FEED-1111g",
                        ts=9,
                        glparams=[GLParam(param_id=101, value=1)],
                    ),
                    BlueOffer(fesh=433, price=105, price_old=200, feedid=403, waremd5="BLUE-300001-FEED-1112g", ts=9),
                ],
            ),
        ]

        cls.index.shops += [
            Shop(fesh=20, datafeed_id=200, priority_region=213, cpa=Shop.CPA_REAL, blue=Shop.BLUE_REAL),  # Blue shop
            Shop(fesh=21, datafeed_id=201, priority_region=213, cpa=Shop.CPA_REAL, blue=Shop.BLUE_REAL),  # Blue shop
            Shop(fesh=22, datafeed_id=202, priority_region=213, cpa=Shop.CPA_REAL, blue=Shop.BLUE_REAL),  # Blue shop
            Shop(fesh=23, datafeed_id=203, priority_region=213, cpa=Shop.CPA_REAL, blue=Shop.BLUE_REAL),  # Blue shop
            Shop(fesh=24, datafeed_id=204, priority_region=213, cpa=Shop.CPA_REAL, blue=Shop.BLUE_REAL),  # Blue shop
            Shop(fesh=25, datafeed_id=205, priority_region=213, cpa=Shop.CPA_REAL, blue=Shop.BLUE_REAL),  # Blue shop
            Shop(fesh=30, datafeed_id=301, priority_region=213, cpa=Shop.CPA_REAL, blue=Shop.BLUE_REAL),  # Blue shop
            Shop(fesh=31, datafeed_id=302, priority_region=213, cpa=Shop.CPA_REAL, blue=Shop.BLUE_REAL),  # Blue shop
            Shop(fesh=32, datafeed_id=303, priority_region=213, cpa=Shop.CPA_REAL, blue=Shop.BLUE_REAL),  # Blue shop
            Shop(fesh=33, datafeed_id=304, priority_region=213, cpa=Shop.CPA_REAL, blue=Shop.BLUE_REAL),  # Blue shop
            Shop(fesh=34, datafeed_id=305, priority_region=213, cpa=Shop.CPA_REAL, blue=Shop.BLUE_REAL),  # Blue shop
            Shop(fesh=35, datafeed_id=306, priority_region=213, cpa=Shop.CPA_REAL, blue=Shop.BLUE_REAL),  # Blue shop
            Shop(fesh=432, datafeed_id=402, priority_region=213, cpa=Shop.CPA_REAL, blue=Shop.BLUE_REAL),  # Blue shop
            Shop(fesh=433, datafeed_id=403, priority_region=213, cpa=Shop.CPA_REAL, blue=Shop.BLUE_REAL),  # Blue shop
        ]

        cls.dynamic.market_dynamic.dynamic_price_control += [
            DynamicPriceControlData(21, 20, 1),
            DynamicPriceControlData(33, 50, 0),
            DynamicPriceControlData(34, 20, 0),
            DynamicPriceControlData(432, 20, 0),
        ]

        cls.index.blue_regional_mskus += [
            RegionalMsku(msku_id=3, offers=2, price_min=800, price_max=10000, rids=[213]),
            RegionalMsku(msku_id=4, offers=2, price_min=2576, price_max=2775, rids=[213]),
            RegionalMsku(msku_id=5, offers=2, price_min=9990, price_max=11990, rids=[213]),
            RegionalMsku(msku_id=300001, offers=2, price_min=105, price_max=106, rids=[213]),
        ]

        cls.index.promos += [
            promo1,
        ]

    def test_dynamic_pricing_strategy_full_assortment(self):
        """Проверяем доставку стратегий для всего ассортимента"""
        response = self.report.request_json("place=check_prices&feed_shoffer_id=201-2002")
        self.assertFragmentIn(
            response,
            {
                "price": {
                    "value": "840",
                },
                "dynamicPricingStrategy": {
                    "type": "REFERENCE",
                    "thresholdType": "PERCENT",
                    "value": 20,
                    "source": "ASSORTMENT",
                    "isApplied": True,
                },
            },
        )

        self.show_log.expect(
            price_before_dynamic_strategy='1000',
            price_after_dynamic_strategy='840',
            dynamic_strategy_type=1,
            is_dynamic_strategy_by_ssku=0,
            is_dynamic_strategy_applied=1,
            is_dynamic_strategy_threshold_percent=1,
            dynamic_strategy_threshold_value=20,
        ).times(2)

    def test_dynamic_pricing_strategy_from_indexer(self):
        """Проверяем доставку стратегий через индексатор"""
        response = self.report.request_json(
            "place=check_prices&feed_shoffer_id=202-2003&rearr-factors=enable_dynamic_pricing_by_ssku=1"
        )
        self.assertFragmentIn(
            response,
            {
                "price": {
                    "value": "810",
                },
                "dynamicPricingStrategy": {
                    "type": "REFERENCE",
                    "thresholdType": "PERCENT",
                    "value": 22,
                    "source": "SKU",
                    "isApplied": True,
                },
            },
        )

        self.show_log.expect(
            price_before_dynamic_strategy='1000',
            price_after_dynamic_strategy='810',
            dynamic_strategy_type=1,
            is_dynamic_strategy_by_ssku=1,
            is_dynamic_strategy_applied=1,
            is_dynamic_strategy_threshold_percent=1,
            dynamic_strategy_threshold_value=22,
        ).times(2)

    def test_dynamic_pricing_strategy_not_applied(self):
        """Проверяем доставку стратегий через индексатор, которая не сработает, так как минреф не достигнут"""
        response = self.report.request_json(
            "place=check_prices&feed_shoffer_id=204-2006&rearr-factors=enable_dynamic_pricing_by_ssku=1"
        )
        self.assertFragmentIn(
            response,
            {
                "price": {
                    "value": "1000",
                },
                "dynamicPricingStrategy": {
                    "type": "REFERENCE",
                    "thresholdType": "PERCENT",
                    "value": 15.31,
                    "source": "SKU",
                    "isApplied": False,
                },
            },
        )
        self.show_log.expect(
            price_before_dynamic_strategy='1000',
            price_after_dynamic_strategy='1000',
            dynamic_strategy_type=1,
            is_dynamic_strategy_by_ssku=1,
            is_dynamic_strategy_applied=0,
            is_dynamic_strategy_threshold_percent=1,
            dynamic_strategy_threshold_value=15.31,
        ).times(2)

    def test_dynamic_pricing_strategy_from_rty(self):
        """Проверяем доставку стратегий rty"""
        self.rty.offers += [
            RtyOffer(
                feedid=203,
                offerid=2004,
                dynamic_pricing=DynamicPricingStrategySSKU(
                    dynamic_pricing_type=DYNAMIC_PRICING_TYPE.MINREF,
                    dynamic_pricing_threshold_is_percent=0,
                    dynamic_pricing_threshold_value=200.5,
                ),
                dynamic_pricing_ts=333,
            ),
        ]
        response = self.report.request_json(
            "place=check_prices&feed_shoffer_id=203-2004&rearr-factors=enable_dynamic_pricing_by_ssku=1"
        )
        self.assertFragmentIn(
            response,
            {
                "price": {
                    "value": "970",
                },
                "dynamicPricingStrategy": {
                    "type": "REFERENCE",
                    "thresholdType": "VALUE",
                    "value": 200.5,
                    "source": "SKU",
                    "isApplied": True,
                },
            },
        )
        response = self.report.request_json(
            "place=offerinfo&feed_shoffer_id=203-2004&rids=213&regset=1&rearr-factors=enable_dynamic_pricing_by_ssku=1"
        )
        self.assertFragmentIn(
            response,
            {
                "dynamicPriceStrategy": 2,
                "maxAllowedDiscount": 20.05,
            },
        )
        self.show_log.expect(
            price_before_dynamic_strategy='1000',
            price_after_dynamic_strategy='970',
            dynamic_strategy_type=1,
            is_dynamic_strategy_by_ssku=1,
            is_dynamic_strategy_applied=1,
            is_dynamic_strategy_threshold_percent=0,
            dynamic_strategy_threshold_value=200.5,
        ).times(4)

    def test_dynamic_pricing_strategy_get_fresh(self):
        """Должна сработать стратегия байбокса, которая пришла через rty,
        так как она свежее чем время создания индекса"""
        self.rty.offers += [
            RtyOffer(
                feedid=202,
                offerid=2003,
                dynamic_pricing=DynamicPricingStrategySSKU(
                    dynamic_pricing_type=DYNAMIC_PRICING_TYPE.BUYBOX,
                    dynamic_pricing_threshold_is_percent=0,
                    dynamic_pricing_threshold_value=200.0,
                ),
                dynamic_pricing_ts=555,
            ),
        ]
        response = self.report.request_json(
            "place=check_prices&feed_shoffer_id=202-2003&rearr-factors=enable_dynamic_pricing_by_ssku=1"
        )
        self.assertFragmentIn(
            response,
            {
                "price": {
                    "value": "1000",
                },
                "dynamicPricingStrategy": {
                    "type": "BUYBOX",
                    "thresholdType": "VALUE",
                    "value": 200,
                    "source": "SKU",
                    "isApplied": False,
                },
            },
        )

        """Должна выбраться стратегия байбокса, которая пришла через rty,
           так как она свежее чем время создания индекса, но не сработает,
           так как задан реар флаг, отключающий стратегию байбокса"""
        response = self.report.request_json(
            "place=check_prices&feed_shoffer_id=202-2003&rearr-factors=enable_dynamic_pricing_by_ssku=1;disable_buybox_strategy=1"
        )
        self.assertFragmentIn(
            response,
            {
                "price": {
                    "value": "1000",
                },
                "dynamicPricingStrategy": {
                    "type": "BUYBOX",
                    "thresholdType": "VALUE",
                    "value": 200,
                    "source": "SKU",
                    "isApplied": False,
                },
            },
        )
        self.show_log.expect(
            price_before_dynamic_strategy='1000',
            price_after_dynamic_strategy='1000',
            dynamic_strategy_type=0,
            is_dynamic_strategy_by_ssku=1,
            is_dynamic_strategy_applied=0,
            is_dynamic_strategy_threshold_percent=0,
            dynamic_strategy_threshold_value=200,
        ).times(2)

    def test_dynamic_pricing_strategy_ssku_priority_above_dynamics(self):
        """Есть и общая стратегия, которая не может скинуть до минрефа, и ssku, которая может скинуть,
        выбирается ssku, так как приоритет за сскю"""
        self.rty.offers += [
            RtyOffer(
                feedid=204,
                offerid=2005,
                dynamic_pricing=DynamicPricingStrategySSKU(
                    dynamic_pricing_type=DYNAMIC_PRICING_TYPE.MINREF,
                    dynamic_pricing_threshold_is_percent=1,
                    dynamic_pricing_threshold_value=29.31,
                ),
                dynamic_pricing_ts=333,
            ),
        ]
        response = self.report.request_json(
            "place=check_prices&feed_shoffer_id=204-2005&rearr-factors=enable_dynamic_pricing_by_ssku=1"
        )
        self.assertFragmentIn(
            response,
            {
                "price": {
                    "value": "710",
                },
                "dynamicPricingStrategy": {
                    "type": "REFERENCE",
                    "thresholdType": "PERCENT",
                    "value": 29.31,
                    "source": "SKU",
                    "isApplied": True,
                },
            },
        )
        self.show_log.expect(
            price_before_dynamic_strategy='1000',
            price_after_dynamic_strategy='710',
            dynamic_strategy_type=1,
            is_dynamic_strategy_by_ssku=1,
            is_dynamic_strategy_applied=1,
            is_dynamic_strategy_threshold_percent=1,
            dynamic_strategy_threshold_value=29.31,
        ).times(2)

    def test_filtering_before_buybox(self):
        # при отсутствии фильтров оба оффера попадают в байбокс
        response = self.report.request_json(
            "place=productoffers&glfilter=101:1&market-sku=300001&rgb=green_with_blue&pp=6&hid=33&hyperid=33&rids=213&offers-set=defaultList,listCpa&rearr-factors=market_rel_filters_before_buybox=0;"
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "prices": {"value": "105"},
            },
        )

        # при включении фильтров только более дорогой оффер попадает в байбокс,
        # но цена все равно сбрасывается до 105, так мин цена посчитана до фильтров
        response = self.report.request_json(
            "place=productoffers&glfilter=101:1&market-sku=300001&rgb=green_with_blue&pp=6&hid=33&hyperid=33&rids=213&offers-set=defaultList,listCpa&"
            "rearr-factors=market_rel_filters_before_buybox=1;enable_offline_buybox_price=0"
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "prices": {"value": "105"},
                'wareId': "BLUE-300001-FEED-1111g",
                "dynamicPriceStrategy": 1,
            },
        )

        self.show_log.expect(
            price_before_dynamic_strategy='106',
            price_after_dynamic_strategy='105',
            dynamic_strategy_type=0,
            is_dynamic_strategy_by_ssku=0,
            is_dynamic_strategy_applied=1,
            is_dynamic_strategy_threshold_percent=1,
            dynamic_strategy_threshold_value=20,
        ).times(14)

        # тут более дешевый оффер отфильтруется gl фильтрами, и так как calculate_sku_min_price_before_filters=0
        # минимальная цена будет считаться только для набора офферов, прошедших фильтры
        response = self.report.request_json(
            "place=productoffers&glfilter=101:1&market-sku=300001&rgb=green_with_blue&pp=6&hid=33&hyperid=33&rids=213&"
            "offers-set=defaultList,listCpa&rearr-factors=market_rel_filters_before_buybox=1;calculate_sku_min_price_before_filters=0;enable_offline_buybox_price=0"
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "prices": {"value": "106"},
                'wareId': "BLUE-300001-FEED-1111g",
                "dynamicPriceStrategy": 1,
            },
        )

        # тут цена байбокса приходит из индексатора, поэтому неважно, что отфильтруется на репорте, а что - нет
        response = self.report.request_json(
            "place=productoffers&glfilter=101:1&market-sku=300001&rgb=green_with_blue&pp=6&hid=33&hyperid=33&rids=213&"
            "offers-set=defaultList,listCpa&rearr-factors=market_rel_filters_before_buybox=1;calculate_sku_min_price_before_filters=0;enable_offline_buybox_price=1;"
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "prices": {"value": "105"},
                'wareId': "BLUE-300001-FEED-1111g",
                "dynamicPriceStrategy": 1,
            },
        )
        self.show_log.expect(
            price_before_dynamic_strategy='106',
            price_after_dynamic_strategy='106',
            dynamic_strategy_type=0,
            is_dynamic_strategy_by_ssku=0,
            is_dynamic_strategy_applied=0,
            is_dynamic_strategy_threshold_percent=1,
            dynamic_strategy_threshold_value=20,
        ).times(2)

    def test_buybox_not_allow_subsidy(self):
        response = self.report.request_json(
            "place=offerinfo&feed_shoffer_id=302-3003&rids=213&regset=1&rearr-factors=enable_dynamic_pricing_by_ssku=1;enable_offline_buybox_price=0"
        )
        self.assertFragmentIn(
            response,
            {
                "prices": {"value": "800"},
            },
        )

        # при включенном оффлайн байбоксе есть возможность частичного субсидирования
        response = self.report.request_json(
            "place=offerinfo&feed_shoffer_id=302-3003&rids=213&regset=1&rearr-factors=enable_dynamic_pricing_by_ssku=1;enable_offline_buybox_price=1"
        )
        self.assertFragmentIn(
            response,
            {
                "price": {
                    "currency": "RUR",
                    "value": "700",
                    "subsidy": {"oldMin": "1000", "percent": 10, "absolute": "100"},
                    "discount": {"oldMin": "1000", "percent": 30, "absolute": "300"},
                },
                "has_dco_3p_subsidy": True,
            },
        )

        self.show_log.expect(
            price_before_dynamic_strategy='1000',
            price_after_dynamic_strategy='800',
            dynamic_strategy_type=0,
            is_dynamic_strategy_by_ssku=1,
            is_dynamic_strategy_applied=1,
            is_dynamic_strategy_threshold_percent=1,
            dynamic_strategy_threshold_value=22,
        ).times(4)

    def test_is_applied_for_buybox(self):
        response = self.report.request_json(
            "place=check_prices&feed_shoffer_id=303-3004&rids=213&regset=1&rearr-factors=enable_dynamic_pricing_by_ssku=1;enable_offline_buybox_price=0"
        )
        self.assertFragmentIn(
            response,
            {
                "price": {
                    "value": "2576",
                },
                "dynamicPricingStrategy": {
                    "type": "BUYBOX",
                    "thresholdType": "VALUE",
                    "value": 199.98,
                    "source": "SKU",
                    "isApplied": True,
                },
            },
        )

        response = self.report.request_json(
            "place=check_prices&feed_shoffer_id=303-3004&rids=213&regset=1&rearr-factors=enable_dynamic_pricing_by_ssku=1;enable_offline_buybox_price=1"
        )
        self.assertFragmentIn(
            response,
            {
                "price": {
                    "value": "2576",
                },
                "dynamicPricingStrategy": {
                    "type": "BUYBOX",
                    "thresholdType": "VALUE",
                    "value": 199.98,
                    "source": "SKU",
                    "isApplied": True,
                },
            },
        )

        self.show_log.expect(
            price_before_dynamic_strategy='2775',
            price_after_dynamic_strategy='2576',
            dynamic_strategy_type=0,
            is_dynamic_strategy_by_ssku=1,
            is_dynamic_strategy_applied=1,
            is_dynamic_strategy_threshold_percent=0,
            dynamic_strategy_threshold_value=199.98,
        ).times(4)

    def test_dynamic_pricing_strategy_erase_strategy(self):
        """Должна сработать стратегия None, которая пришла через rty,
        так как она свежее чем время создания индекса,
        несмотря на то что везде в стратегии будут нули"""
        self.rty.offers += [
            RtyOffer(
                feedid=202,
                offerid=2003,
                dynamic_pricing=DynamicPricingStrategySSKU(
                    dynamic_pricing_type=DYNAMIC_PRICING_TYPE.UNKNOWN,
                    dynamic_pricing_threshold_is_percent=0,
                    dynamic_pricing_threshold_value=0,
                ),
                dynamic_pricing_ts=555,
            ),
        ]
        response = self.report.request_json(
            "place=check_prices&feed_shoffer_id=202-2003&rearr-factors=enable_dynamic_pricing_by_ssku=1"
        )
        self.assertFragmentIn(
            response,
            {
                "price": {
                    "value": "1000",
                },
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                "dynamicPricingStrategy": {
                    "type": "REFERENCE",
                    "thresholdType": "PERCENT",
                    "value": 22,
                    "source": "SKU",
                }
            },
        )

    def test_buybox_with_do_ware_md5(self):
        # при отсутствии фильтров оба оффера попадают в байбокс
        response = self.report.request_json(
            "place=productoffers&market-sku=5&pp=6&rids=213&offers-set=defaultList,listCpa&do-waremd5=gTL-3D5IXpiHAL-CvNRmNQ&rearr-factors=enable_offline_buybox_price=0"
        )
        self.assertFragmentIn(
            response,
            {
                "wareId": "gTL-3D5IXpiHAL-CvNRmNQ",
                "entity": "offer",
                "prices": {"value": "9990"},
                "benefit": {"type": "waremd5"},
            },
        )

        response = self.report.request_json(
            "place=productoffers&market-sku=5&pp=6&rids=213&offers-set=defaultList,listCpa&do-waremd5=gTL-3D5IXpiHAL-CvNRmNQ&rearr-factors=enable_offline_buybox_price=1"
        )
        self.assertFragmentIn(
            response,
            {
                "wareId": "gTL-3D5IXpiHAL-CvNRmNQ",
                "entity": "offer",
                "prices": {"value": "9990"},
                "benefit": {"type": "waremd5"},
            },
        )

        self.show_log.expect(
            price_before_dynamic_strategy='11990',
            price_after_dynamic_strategy='9990',
            dynamic_strategy_type=0,
            is_dynamic_strategy_by_ssku=0,
            is_dynamic_strategy_applied=1,
            is_dynamic_strategy_threshold_percent=1,
            dynamic_strategy_threshold_value=20,
        ).times(6)

    def test_no_resale_offer_in_min_price(self):
        # самый дешевый оффер на мскю имеет цену 790, но так как этот оффер - б/у, он не участвует в определении мин цены
        response = self.report.request_json(
            "place=offerinfo&feed_shoffer_id=302-3003&rids=213&regset=1&rearr-factors=enable_dynamic_pricing_by_ssku=1;enable_offline_buybox_price=0"
        )
        self.assertFragmentIn(
            response,
            {
                "shop": {"feed": {"id": "302", "offerId": "3003"}},
                "prices": {"value": "800"},
            },
        )


if __name__ == '__main__':
    main()
