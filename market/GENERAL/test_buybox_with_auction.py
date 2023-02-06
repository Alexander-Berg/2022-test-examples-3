#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    Autostrategy,
    AutostrategyType,
    AutostrategyWithDatasourceId,
    BlueOffer,
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryOption,
    DynamicDeliveryRestriction,
    DynamicDeliveryServiceInfo,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseDelivery,
    DynamicWarehouseInfo,
    DynamicWarehouseLink,
    DynamicWarehousesPriorityInRegion,
    Elasticity,
    ExperimentalBoostFeeReservePrice,
    HyperCategory,
    HyperCategoryType,
    MarketSku,
    MnPlace,
    Model,
    Offer,
    Payment,
    PaymentRegionalGroup,
    RecommendedFee,
    Region,
    RegionalDelivery,
    ReservePriceFee,
    Shop,
    ShopPaymentMethods,
)
from core.testcase import TestCase, main
from core.matcher import Capture, ElementCount, Greater, NotEmpty
from unittest import skip


USE_DEPRECATED_DIRECT_SHIPPING_FLOW = "&rearr-factors=get_rid_of_direct_shipping=0"


def dict_to_rearr(rearr_flags):
    result = ""
    for key in rearr_flags.keys():
        result += str(key) + "=" + str(rearr_flags[key]) + ";"

    return result


base_rearr_flags_dict = {
    "market_blue_buybox_delivery_context_approx_use_shop_id": 1,
    "market_blue_buybox_delivery_switch_type": 3,
    "market_blue_buybox_disable_dsbs_pessimisation": 1,
    "market_operational_rating_everywhere": 1,
    "market_blue_buybox_1p_cancellation_rating_default": 0.01,
    "market_blue_buybox_with_dsbs_white": 1,
    "prefer_do_with_sku": 1,
    "market_buybox_auction_cpa_fee": 1,
    "market_buybox_auction_coef_b": 0.001,
    "market_buybox_auction_rand_low": 0.99,
    "market_buybox_auction_rand_delta": 0.01,
    "market_buybox_auction_coef_w": 0.0,
}


class T(TestCase):
    """
    Тесты для логики аукциона в байбоксе https://st.yandex-team.ru/MARKETMONEY-458
    """

    @classmethod
    def prepare(cls):
        cls.settings.put_white_cpa_offer_to_the_blue_shard = True
        cls.matrixnet.on_default_place(MnPlace.BASE_SEARCH).respond(0.001)

        cls.index.regiontree += [Region(rid=213, name='Нерезиновая')]

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
                warehouse_id=145,
                fulfillment_program=True,
            ),
            Shop(
                fesh=3,
                datafeed_id=3,
                priority_region=213,
                regions=[213],
                fulfillment_program=True,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                name="3P поставщик Анатлий",
                warehouse_id=145,
            ),
            Shop(
                fesh=4,
                datafeed_id=4,
                priority_region=213,
                regions=[213],
                fulfillment_program=True,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                name="3P поставщик клон Анатолия",
                warehouse_id=145,
            ),
            Shop(
                fesh=5,
                datafeed_id=5,
                business_fesh=4,
                name="dsbs магазин Пети",
                supplier_type=Shop.THIRD_PARTY,
                regions=[213],
                cis=Shop.CIS_REAL,
                cpa=Shop.CPA_REAL,
            ),
            Shop(
                fesh=6,
                datafeed_id=6,
                business_fesh=6,
                name="dsbs магазин клон Пети",
                regions=[213],
                cis=Shop.CIS_REAL,
                cpa=Shop.CPA_REAL,
                cpc=Shop.CPC_NO,
                priority_region=213,
                supplier_type=Shop.THIRD_PARTY,
            ),
            Shop(
                fesh=7,
                datafeed_id=7,
                priority_region=213,
                regions=[213],
                fulfillment_program=True,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                name="3P поставщик Анатлий",
                warehouse_id=145,
            ),
            Shop(
                fesh=8,
                datafeed_id=8,
                priority_region=213,
                regions=[213],
                fulfillment_program=True,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                name="3P поставщик клон Анатолия",
                warehouse_id=145,
            ),
            Shop(
                fesh=9,
                datafeed_id=9,
                priority_region=213,
                regions=[213],
                fulfillment_program=True,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                name="3P поставщик еще один клон Анатолия",
                warehouse_id=145,
            ),
            Shop(
                fesh=10,
                datafeed_id=10,
                priority_region=213,
                regions=[213],
                fulfillment_program=True,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                name="3P поставщик очередной клон Анатолия",
                warehouse_id=145,
            ),
            Shop(
                fesh=11,
                datafeed_id=11,
                business_fesh=5,
                name="dsbs клон магазин Пети",
                supplier_type=Shop.THIRD_PARTY,
                regions=[213],
                cis=Shop.CIS_REAL,
                cpa=Shop.CPA_REAL,
            ),
            Shop(
                fesh=12,
                datafeed_id=12,
                business_fesh=5,
                name="dsbs очередной клон магазин Пети",
                supplier_type=Shop.THIRD_PARTY,
                regions=[213],
                cis=Shop.CIS_REAL,
                cpa=Shop.CPA_REAL,
            ),
        ]

        cls.settings.lms_autogenerate = False
        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=145, home_region=213),
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
                regional_options=[RegionalDelivery(rid=225, options=[DeliveryOption(price=15, day_from=1, day_to=2)])],
            ),
            DeliveryBucket(
                bucket_id=4240,
                fesh=6,
                carriers=[99],
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=100, day_from=1, day_to=1)])],
            ),
        ]

        cls.index.shops_payment_methods += [
            ShopPaymentMethods(
                fesh=9,
                payment_groups=[
                    PaymentRegionalGroup(
                        included_regions=[213],
                        payment_methods=[
                            Payment.PT_PREPAYMENT_CARD,
                            Payment.PT_PREPAYMENT_OTHER,
                            Payment.PT_CARD_ON_DELIVERY,
                            Payment.PT_CASH_ON_DELIVERY,
                        ],
                    ),
                ],
            ),
            ShopPaymentMethods(
                fesh=10,
                payment_groups=[
                    PaymentRegionalGroup(
                        included_regions=[213],
                        payment_methods=[
                            Payment.PT_CASH_ON_DELIVERY,
                        ],
                    ),
                ],
            ),
            ShopPaymentMethods(
                fesh=11,
                payment_groups=[
                    PaymentRegionalGroup(
                        included_regions=[213],
                        payment_methods=[
                            Payment.PT_PREPAYMENT_CARD,
                            Payment.PT_PREPAYMENT_OTHER,
                            Payment.PT_CARD_ON_DELIVERY,
                            Payment.PT_CASH_ON_DELIVERY,
                        ],
                    ),
                ],
            ),
            ShopPaymentMethods(
                fesh=12,
                payment_groups=[
                    PaymentRegionalGroup(
                        included_regions=[213],
                        payment_methods=[
                            Payment.PT_CASH_ON_DELIVERY,
                        ],
                    ),
                ],
            ),
        ]

        cls.index.models += [
            Model(hid=1, ts=501, hyperid=1, title='model_1', vbid=0),
            Model(hid=1, ts=502, hyperid=2, title='model_2', vbid=0),
            Model(hid=1, ts=503, hyperid=3, title='model_3', vbid=0),
            Model(hid=8, ts=508, hyperid=8, title='model_8', vbid=10),  # duplicated offers (won by random)
            Model(hid=91235, ts=509, hyperid=9, title='model_9', vbid=10),
            Model(hid=10, ts=510, hyperid=10, title='model_10', vbid=10),
        ]

        cls.index.mskus += [
            MarketSku(
                title="msku_1",
                hyperid=1,
                sku=100001,
                hid=1,
                delivery_buckets=[1234],
                buybox_elasticity=[
                    Elasticity(price_variant=100, demand_mean=200),
                    Elasticity(price_variant=200, demand_mean=80),
                    Elasticity(price_variant=300, demand_mean=10),
                ],
                blue_offers=[
                    BlueOffer(
                        price=100,
                        feedid=7,
                        waremd5="BLUE-100001-FEED-2222Q",
                        ts=12,
                        autostrategy_vendor_with_datasource_id=AutostrategyWithDatasourceId(
                            id=444,
                            datasource_id=1,
                            strategy=Autostrategy(type=AutostrategyType.POSITIONAL, position=1, maxbid=3),
                        ),
                    ),
                    BlueOffer(
                        price=203,
                        feedid=8,
                        waremd5="BLUE-100001-FEED-3333g",
                        ts=13,
                        autostrategy_vendor_with_datasource_id=AutostrategyWithDatasourceId(
                            id=444,
                            datasource_id=2,
                            strategy=Autostrategy(type=AutostrategyType.POSITIONAL, position=1, maxbid=2),
                        ),
                    ),
                ],
            ),
            MarketSku(
                title="msku_2",
                hyperid=3,
                sku=100002,
                hid=1,
                delivery_buckets=[1234],
                buybox_elasticity=[
                    Elasticity(price_variant=100, demand_mean=200),
                    Elasticity(price_variant=200, demand_mean=80),
                    Elasticity(price_variant=300, demand_mean=10),
                ],
                blue_offers=[
                    BlueOffer(
                        price=100,
                        feedid=3,
                        fee=50,
                        waremd5="BLUE-100002-FEED-2222g",
                        ts=25,
                        autostrategy_vendor_with_datasource_id=AutostrategyWithDatasourceId(
                            id=444,
                            datasource_id=3,
                            strategy=Autostrategy(type=AutostrategyType.POSITIONAL, position=1, maxbid=1),
                        ),
                    ),
                    BlueOffer(
                        price=103,
                        feedid=3,
                        fee=100,
                        waremd5="BLUE-100002-FEED-3333Q",
                        ts=26,
                        autostrategy_vendor_with_datasource_id=AutostrategyWithDatasourceId(
                            id=444,
                            datasource_id=4,
                            strategy=Autostrategy(type=AutostrategyType.POSITIONAL, position=1, maxbid=1),
                        ),
                    ),
                ],
            ),
            MarketSku(
                title="msku_3",
                hyperid=2,
                sku=100003,
                hid=1,
                delivery_buckets=[1234],
                buybox_elasticity=[
                    Elasticity(price_variant=100, demand_mean=200),
                    Elasticity(price_variant=200, demand_mean=80),
                    Elasticity(price_variant=300, demand_mean=10),
                ],
                blue_offers=[
                    BlueOffer(
                        price=100,
                        feedid=4,
                        fee=100,
                        waremd5="BLUE-100003-FEED-2222g",
                        ts=35,
                        autostrategy_vendor_with_datasource_id=AutostrategyWithDatasourceId(
                            id=444,
                            datasource_id=6,
                            strategy=Autostrategy(type=AutostrategyType.POSITIONAL, position=1, maxbid=1),
                        ),
                    ),
                ],
            ),
            MarketSku(
                title="test_randomize_auction",
                hyperid=8,
                sku=8,
                hid=8,
                delivery_buckets=[1234],
                buybox_elasticity=[
                    Elasticity(price_variant=100, demand_mean=200),
                    Elasticity(price_variant=200, demand_mean=80),
                    Elasticity(price_variant=300, demand_mean=10),
                ],
                blue_offers=[
                    BlueOffer(price=200, feedid=3, fee=50, waremd5="BLUE-8-2----FEED-XXXXg", ts=82),
                    BlueOffer(price=200, feedid=3, fee=50, waremd5="BLUE-8-3----FEED-XXXXg", ts=83),
                    BlueOffer(price=200, feedid=3, fee=50, waremd5="BLUE-8-4----FEED-XXXXg", ts=84),
                ],
            ),
            MarketSku(
                title="test_cash_only_cehac",
                hyperid=9,
                sku=100010,
                hid=91235,
                delivery_buckets=[1234],
                buybox_elasticity=[
                    Elasticity(price_variant=100, demand_mean=200),
                    Elasticity(price_variant=200, demand_mean=80),
                    Elasticity(price_variant=300, demand_mean=10),
                ],
                blue_offers=[
                    BlueOffer(price=200, feedid=9, fee=50, waremd5="BLUE-100010-FEED-1111Q", ts=1000),
                    BlueOffer(price=200, feedid=10, fee=50, waremd5="BLUE-100010-FEED-2222Q", ts=1001),
                ],
            ),
            MarketSku(
                title="test_cash_only_not_cehac",
                hyperid=10,
                sku=100011,
                hid=10,
                delivery_buckets=[1234],
                buybox_elasticity=[
                    Elasticity(price_variant=100, demand_mean=200),
                    Elasticity(price_variant=200, demand_mean=80),
                    Elasticity(price_variant=300, demand_mean=10),
                ],
                blue_offers=[
                    BlueOffer(price=200, feedid=9, fee=50, waremd5="BLUE-100011-FEED-1111Q", ts=1000),
                    BlueOffer(price=200, feedid=10, fee=50, waremd5="BLUE-100011-FEED-2222Q", ts=1001),
                ],
            ),
        ]

        cls.index.offers += [
            Offer(
                title="market DSBS Offer msku_1",
                hid=1,
                hyperid=1,
                price=98,
                fesh=6,
                business_id=4,
                sku=100001,
                cpa=Offer.CPA_REAL,
                waremd5='sgf1xWYFqdGiLh4TT-lbqQ',
                feedid=6,
                delivery_buckets=[4240],
                offerid="proh.offer",
                ts=17,
                autostrategy_vendor_with_datasource_id=AutostrategyWithDatasourceId(
                    id=444,
                    datasource_id=5,
                    strategy=Autostrategy(type=AutostrategyType.POSITIONAL, position=1, maxbid=1),
                ),
            ),
            Offer(
                title="market DSBS Offer 2 msku_2",
                hid=1,
                hyperid=3,
                price=99,
                fesh=6,
                business_id=4,
                sku=100002,
                fee=10,
                cpa=Offer.CPA_REAL,
                waremd5='sgf1xWYFqdGiLh4TT-222Q',
                feedid=6,
                delivery_buckets=[4240],
                offerid="proh.offer",
                ts=28,
                autostrategy_vendor_with_datasource_id=AutostrategyWithDatasourceId(
                    id=444,
                    datasource_id=6,
                    strategy=Autostrategy(type=AutostrategyType.POSITIONAL, position=1, maxbid=1),
                ),
            ),
            Offer(
                title="market DSBS Offer 2 msku_3",
                hid=1,
                hyperid=2,
                price=99,
                fesh=6,
                business_id=4,
                sku=100003,
                cpa=Offer.CPA_REAL,
                waremd5='sgf1xWYFqdGiLh4TT-444Q',
                feedid=6,
                delivery_buckets=[4240],
                offerid="proh.offer",
                ts=28,
                autostrategy_vendor_with_datasource_id=AutostrategyWithDatasourceId(
                    id=444,
                    datasource_id=6,
                    strategy=Autostrategy(type=AutostrategyType.POSITIONAL, position=1, maxbid=1),
                ),
            ),
            Offer(
                title="market DSBS Offer 3 msku_2 cheap no bid",
                hid=1,
                hyperid=3,
                price=59,
                fesh=6,
                business_id=4,
                sku=100002,
                fee=0,
                cpa=Offer.CPA_REAL,
                waremd5='sgf1xWYFqdGiLh4TT-232Q',
                feedid=6,
                delivery_buckets=[4240],
                offerid="proh.offer",
                ts=28,
            ),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 82).respond(0.1)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 83).respond(0.1)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 84).respond(0.1)

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 12).respond(0.50)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 13).respond(0.50)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 17).respond(0.50)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 25).respond(0.52)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 26).respond(0.52)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 28).respond(0.52)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 35).respond(0.48)

        cls.index.hypertree += [
            HyperCategory(hid=1, output_type=HyperCategoryType.GURULIGHT),
        ]

        cls.index.experimental_boost_fee_reserve_prices += [ExperimentalBoostFeeReservePrice(1, 1000)]

        cls.index.recommended_fee += [RecommendedFee(hyper_id=1, recommended_bid=0.1)]

        cls.index.reserveprice_fee += [ReservePriceFee(hyper_id=1, reserveprice_fee=0.06)]

    @classmethod
    def prepare_nordstream(cls):
        cls.settings.nordstream_autogenerate = False
        for warehouse_id in (145, 147):
            cls.dynamic.nordstream += [DynamicWarehouseLink(warehouse_id, [warehouse_id])]
            cls.dynamic.nordstream += [
                DynamicWarehouseDelivery(
                    warehouse_id,
                    {
                        225: [
                            DynamicDeliveryRestriction(min_days=1, max_days=2, cost=15),
                        ],
                    },
                )
            ]

    def test_default_offer_choose_auction_winner_shop_fee(self):
        """
        Тестируем что в запросе выбирается оффер победивший аукцион с лучшей ставкой магазина.
        """

        rearr_flags_dict = base_rearr_flags_dict.copy()
        rearr_flags_dict["market_buybox_auction_transfer_fee_to_card"] = 1
        rearr_flags_dict["market_buybox_auction_rand_low"] = 1.0
        rearr_flags_dict["market_buybox_auction_rand_delta"] = 0.0
        # rearr_flags_dict["market_buybox_auction_coef_w"] = 0.0
        # use old conversion for this test
        rearr_flags_dict["market_money_vendor_cpc_to_cpa_conversion_buybox"] = 0.05
        rearr_flags_dict["market_buybox_enable_advert_buybox"] = 0
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            'place=productoffers&market-sku=100002&rgb=green_with_blue&pp=6&hyperid=3&rids=213&debug=da&offers-set=defaultList,listCpa&rearr-factors=%s'
            % rearr_flags_str
            + USE_DEPRECATED_DIRECT_SHIPPING_FLOW
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                'wareId': "BLUE-100002-FEED-3333Q",
                'benefit': NotEmpty(),
                "debug": {
                    "sale": {
                        'brokeredFee': 92,
                        'shopFee': 100,
                    },
                    "buyboxDebug": {
                        "WonMethod": "WON_BY_AUCTION",
                        'Offers': [
                            {
                                'WareMd5': 'sgf1xWYFqdGiLh4TT-222Q',
                                'AuctionValueRandomized': 31894.5,
                                'ShopFee': 10,
                                'OfferVendorFee': 606,
                            },
                            {
                                'WareMd5': 'BLUE-100002-FEED-2222g',
                                'AuctionValueRandomized': 35000.8,
                                'ShopFee': 50,
                                'OfferVendorFee': 600,
                            },
                            {
                                'WareMd5': 'BLUE-100002-FEED-3333Q',
                                'AuctionValueRandomized': 36088.5,
                                'ShopFee': 100,
                                'OfferVendorFee': 582,
                                'AuctionedShopFee': 92,
                                'BuyboxFilter': 'BUYBOX_ONLY',
                            },
                        ],
                    },
                },
            },
        )
        # Проверяем, что логируется WON_METHOD (логируется числами, WON_BY_AUCTION соответствует 6)
        self.show_log.expect(pp=200, won_method=6)

        rearr_flags_dict["market_buybox_auction_transfer_fee_to_card"] = 0
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            'place=productoffers&market-sku=100002&rgb=green_with_blue&pp=6&hyperid=3&rids=213&debug=da&offers-set=defaultList,listCpa&rearr-factors=%s'
            % rearr_flags_str
            + USE_DEPRECATED_DIRECT_SHIPPING_FLOW
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                'wareId': "BLUE-100002-FEED-3333Q",
                'benefit': NotEmpty(),
                "debug": {
                    "sale": {
                        'brokeredFee': 0,
                        'shopFee': 100,
                    },
                    "buyboxDebug": {
                        "Offers": ElementCount(3),
                        "WonMethod": "WON_BY_AUCTION",
                    },
                },
            },
        )

        # Проверяем, что при выключенном аукционе brokeredFee = 0
        rearr_flags_dict["market_buybox_auction_cpa_fee"] = 0
        rearr_flags_dict["market_blue_buybox_gvm_ue_rand_low"] = 1.0
        rearr_flags_dict["market_blue_buybox_gvm_ue_rand_delta"] = 0.0
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            'place=productoffers&market-sku=100003&rgb=green_with_blue&pp=6&hyperid=2&rids=213&debug=da&offers-set=defaultList,listCpa&rearr-factors=%s'
            % rearr_flags_str
            + USE_DEPRECATED_DIRECT_SHIPPING_FLOW
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                'wareId': "BLUE-100003-FEED-2222g",
                'benefit': NotEmpty(),
                "debug": {
                    "sale": {
                        'brokeredFee': 0,
                        'shopFee': 100,
                    },
                    "buyboxDebug": {
                        "Offers": ElementCount(2),
                        "WonMethod": "WON_BY_EXCHANGE",
                    },
                },
            },
        )

    def test_default_offer_choose_auction_winner_shop_fee_without_no_bid_pessimization(self):
        """
        Тестируем что в запросе с выключенным market_buybox_auction_no_bid_pessimization выигрывает оффер сильно более дешевый оффер (несмотря на то, что у него нет ставки)
        Оффера со ставками отфильтровываются (из-за порога по цене)
        """

        rearr_flags_dict = base_rearr_flags_dict.copy()
        rearr_flags_dict["market_buybox_auction_transfer_fee_to_card"] = 1
        rearr_flags_dict["market_buybox_auction_no_bid_pessimization"] = 0
        rearr_flags_dict["market_buybox_auction_rand_low"] = 1.0
        rearr_flags_dict["market_buybox_auction_rand_delta"] = 0.0
        # rearr_flags_dict["market_buybox_auction_coef_w"] = 0.0
        # use old conversion for this test
        rearr_flags_dict["market_money_vendor_cpc_to_cpa_conversion_buybox"] = 0.05
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            'place=productoffers&market-sku=100002&rgb=green_with_blue&pp=6&hyperid=3&rids=213&debug=da&offers-set=defaultList,listCpa&rearr-factors=%s'
            % rearr_flags_str
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                'wareId': "sgf1xWYFqdGiLh4TT-232Q",
                'benefit': NotEmpty(),
                "debug": {
                    "sale": {
                        'brokeredFee': 0,
                        'shopFee': 0,
                    },
                    "buyboxDebug": {
                        "WonMethod": "SINGLE_OFFER_AFTER_BUYBOX_FILTERS",
                    },
                },
            },
        )

    @skip('https://st.yandex-team.ru/MARKETOUT-40832')
    def test_flag_set_fees_and_bids_null(self):
        """
        Тестируем, что флаг market_set_fees_and_bids_null зануляет ставки и фи
        """

        rearr_flags_dict = base_rearr_flags_dict.copy()
        rearr_flags_dict["market_buybox_auction_transfer_fee_to_card"] = 1
        rearr_flags_dict["market_buybox_auction_rand_low"] = 1.0
        rearr_flags_dict["market_buybox_auction_rand_delta"] = 0.0
        rearr_flags_dict["market_set_fees_and_bids_null"] = 1
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            'place=productoffers&market-sku=100002&rgb=green_with_blue&pp=6&rids=213&debug=da&offers-set=defaultList,listCpa&rearr-factors=%s'
            % rearr_flags_str
        )
        # Для CPA офферов тоже есть bid, он должен выставляться равным minBid в эксперименте
        cpa_bid = Capture()
        cpa_minbid = Capture()
        cpa_click_price = Capture()
        cpa_brokered_click_price = Capture()

        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                'wareId': NotEmpty(),
                'benefit': NotEmpty(),
                "debug": {
                    "sale": {
                        'brokeredFee': 0,
                        'shopFee': 0,
                        'bid': NotEmpty(capture=cpa_bid),
                        'minBid': NotEmpty(capture=cpa_minbid),
                        'brokeredClickPrice': NotEmpty(capture=cpa_click_price),
                        'clickPrice': NotEmpty(capture=cpa_brokered_click_price),
                        'vBid': 0,
                    },
                    "buyboxDebug": {
                        "WonMethod": "WON_BY_AUCTION",
                        'Offers': [
                            {
                                'WareMd5': NotEmpty(),
                                'ShopFee': 0,
                                'OfferVendorFee': 0,
                            },
                            {
                                'WareMd5': NotEmpty(),
                                'ShopFee': 0,
                                'OfferVendorFee': 0,
                            },
                        ],
                    },
                },
            },
        )
        # Проверяем, что clickPrice == brokeredClickPrice == bid == minbid
        for sale_prop in [cpa_bid, cpa_click_price, cpa_brokered_click_price]:
            self.assertAlmostEqual(sale_prop.value, cpa_minbid.value, delta=0.0001)
        # Проверяем, что minbid не занулился
        self.assertTrue(cpa_minbid.value != 0)

    def test_default_offer_choose_auction_winner_with_small_coefB(self):
        """
        Тестируем что в запросе выбирается оффер победивший аукцион такой же как и победивший по GMV из-за маленького коэф B
        """

        rearr_flags_dict = base_rearr_flags_dict.copy()
        rearr_flags_dict["market_buybox_auction_coef_b"] = 0.000001
        rearr_flags_dict["market_buybox_enable_advert_buybox"] = 0
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            'place=productoffers&market-sku=100002&rgb=green_with_blue&pp=6&hyperid=3&rids=213&debug=da&offers-set=defaultList,listCpa&rearr-factors=%s'
            % rearr_flags_str
            + USE_DEPRECATED_DIRECT_SHIPPING_FLOW
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                'wareId': "BLUE-100002-FEED-3333Q",
                "debug": {
                    "buyboxDebug": {
                        "Offers": ElementCount(3),
                        "WonMethod": "WON_BY_AUCTION",
                    }
                },
            },
        )

    def test_default_offer_choose_auction_winner_vendor_fee(self):
        """
        Тестируем что в запросе выбирается оффер победивший аукцион c лучшей ставкой вендора на оффер.
        """

        rearr_flags_dict = base_rearr_flags_dict.copy()
        rearr_flags_dict["market_buybox_auction_transfer_fee_to_card"] = 1
        rearr_flags_dict["market_buybox_auction_rand_low"] = 1.0
        rearr_flags_dict["market_buybox_auction_rand_delta"] = 0.0
        rearr_flags_dict["market_money_vendor_cpc_to_cpa_conversion_buybox"] = 0.075
        rearr_flags_dict["market_buybox_enable_advert_buybox"] = 0
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            'place=productoffers&market-sku=100001&rgb=green_with_blue&pp=6&hyperid=1&rids=213&debug=da&offers-set=defaultList,listCpa&rearr-factors=%s'
            % rearr_flags_str
            + USE_DEPRECATED_DIRECT_SHIPPING_FLOW
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                'wareId': "BLUE-100001-FEED-2222Q",
                'benefit': NotEmpty(),
                "debug": {
                    "sale": {
                        "shopFee": 0,
                        "brokeredFee": 0,
                    },
                    "buyboxDebug": {
                        "RejectedOffers": [
                            {"RejectReason": "TOO_HIGH_PRICE", "Offer": {"WareMd5": "BLUE-100001-FEED-3333g"}},
                        ],
                        "Offers": [
                            {
                                'WareMd5': 'sgf1xWYFqdGiLh4TT-lbqQ',
                                'AuctionValueRandomized': 27508.6,
                                'ShopFee': 0,
                                'OfferVendorFee': 408,
                            },
                            {
                                'WareMd5': 'BLUE-100001-FEED-2222Q',
                                'AuctionValueRandomized': 46646.5,
                                'ShopFee': 0,
                                'OfferVendorFee': 1199,
                            },
                        ],
                        "WonMethod": "WON_BY_AUCTION",
                    },
                },
            },
        )

        rearr_flags_dict["market_buybox_auction_cpa_fee"] = 0
        rearr_flags_dict["market_blue_buybox_gvm_ue_rand_low"] = 1.0
        rearr_flags_dict["market_blue_buybox_gvm_ue_rand_delta"] = 0.0
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            'place=productoffers&market-sku=100001&rgb=green_with_blue&pp=6&hyperid=1&rids=213&debug=da&offers-set=defaultList,listCpa&rearr-factors=%s'
            % rearr_flags_str
            + USE_DEPRECATED_DIRECT_SHIPPING_FLOW
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                'wareId': "BLUE-100001-FEED-2222Q",
                'benefit': NotEmpty(),
                "debug": {
                    "buyboxDebug": {
                        "WonMethod": "WON_BY_EXCHANGE",
                    }
                },
            },
        )

    def test_default_offer_market_blue_buybox_max_price_rel_add_diff_500(self):
        """
        Тестируем что market_blue_buybox_max_price_rel_add_diff позволяет повысить порог фильтрации по цене и оффер не реджектится с TOO_HIGH_PRICE.
        """

        rearr_flags_dict = base_rearr_flags_dict.copy()
        rearr_flags_dict["market_buybox_auction_transfer_fee_to_card"] = 1
        rearr_flags_dict["market_buybox_auction_rand_low"] = 1.0
        rearr_flags_dict["market_buybox_auction_rand_delta"] = 0.0
        rearr_flags_dict["market_money_vendor_cpc_to_cpa_conversion_buybox"] = 0.075
        rearr_flags_dict["market_blue_buybox_max_price_rel_add_diff"] = 500
        rearr_flags_dict["market_blue_buybox_price_rel_max_threshold"] = 500
        rearr_flags_dict["market_buybox_enable_advert_buybox"] = 0
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            'place=productoffers&market-sku=100001&rgb=green_with_blue&pp=6&hyperid=1&rids=213&debug=da&offers-set=defaultList,listCpa&rearr-factors=%s'
            % rearr_flags_str
            + USE_DEPRECATED_DIRECT_SHIPPING_FLOW
        )
        self.assertFragmentNotIn(response, "TOO_HIGH_PRICE")

    @skip('Был некорректный hyperid у sgf1xWYFqdGiLh4TT-444Q')
    def test_white_search_take_brokered_fee_from_default_offer(self):
        """
        Проверяем, что на поиске выбирается ставка для оффера как max(ставка_поиск, ставка_ДО)
        """

        vendor_conversion = 0.075
        sigmoid_alpha = 0.21
        sigmoid_beta = 0.0015
        sigmoid_gamma = 2.742

        rearr_flags_dict = base_rearr_flags_dict.copy()
        rearr_flags_dict["market_buybox_auction_transfer_fee_to_search"] = 1
        rearr_flags_dict["market_buybox_auction_rand_low"] = 1.0
        rearr_flags_dict["market_buybox_auction_rand_delta"] = 0.0
        rearr_flags_dict["market_white_search_auction_cpa_fee"] = 1
        rearr_flags_dict["market_white_search_auction_cpa_fee_no_base_bids"] = 0
        rearr_flags_dict["market_white_search_auction_cpa_fee_minbid_ab"] = 0
        rearr_flags_dict["market_tweak_search_auction_white_cpa_fee_params"] = "{},{},{}".format(
            sigmoid_alpha, sigmoid_beta, sigmoid_gamma
        )
        rearr_flags_dict["market_money_vendor_cpc_to_cpa_conversion_buybox"] = vendor_conversion
        rearr_flags_dict["market_white_search_auction_cpa_fee_transfer_fee_do"] = 1
        rearr_flags_dict["market_set_1p_fee_recommended"] = 0
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)

        """Ожидаем что возьмется амнистированная ставка с ДО"""
        response = self.report.request_json(
            'place=prime&pp=7&text=msku_&rids=213&debug=da&numdoc=20&use-default-offers=1&allow-collapsing=1&rearr-factors=%s'
            % rearr_flags_str
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    "results": [
                        {
                            'entity': 'product',
                            'slug': 'model-3',
                            'offers': {
                                'items': [
                                    {
                                        'entity': 'offer',
                                        'slug': 'msku-2',
                                        'wareId': "BLUE-100002-FEED-3333Q",
                                        'debug': {
                                            'sale': {
                                                'shopFee': 100,
                                                'brokeredFee': 88,
                                            },
                                            'buyboxDebug': {
                                                'WonMethod': "WON_BY_AUCTION",
                                            },
                                        },
                                    }
                                ],
                            },
                        },
                    ]
                }
            },
        )

        rearr_flags_dict["market_buybox_auction_transfer_fee_to_search"] = 0
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        """Ожидаем что возьмется амнистированная ставка с поиска"""
        response = self.report.request_json(
            'place=prime&pp=7&text=msku_&rids=213&debug=da&numdoc=20&use-default-offers=1&allow-collapsing=1&rearr-factors=%s'
            % rearr_flags_str
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    "results": [
                        {
                            'entity': 'product',
                            'slug': 'model-3',
                            'offers': {
                                'items': [
                                    {
                                        'entity': 'offer',
                                        'slug': 'msku-2',
                                        'wareId': "BLUE-100002-FEED-3333Q",
                                        'debug': {
                                            'sale': {
                                                'shopFee': 100,
                                                'brokeredFee': 0,
                                            },
                                            'buyboxDebug': {
                                                'WonMethod': "WON_BY_AUCTION",
                                            },
                                        },
                                    }
                                ],
                            },
                        },
                    ]
                }
            },
        )

    def test_won_by_random(self):
        """
        Проверяю, что в ДО выбирается оффер по рандому (офферы с равными GMV), при этом причина выбора (рандом) логируется
        """

        def get_response(rearr_flags, yandex_uid):
            rearr_flags_str = dict_to_rearr(rearr_flags_dict)
            return self.report.request_json(
                'place=productoffers&market-sku=8&rgb=green_with_blue&pp=6&hyperid=8&rids=213&debug=da&allow_collapsing=0&yandexuid={}&offers-set=defaultList&rearr-factors=%s'.format(
                    yandex_uid
                )
                % rearr_flags_str
            )

        rearr_flags_dict = base_rearr_flags_dict.copy()
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)

        # yandex_uid = 1
        response = get_response(rearr_flags_str, 1)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "wareId": "BLUE-8-3----FEED-XXXXg",
                        "debug": {
                            "buyboxDebug": {
                                "Offers": [
                                    {
                                        "WareMd5": "BLUE-8-3----FEED-XXXXg",
                                        "IsWinnerByRandomInAuction": True,
                                        "IsLoserByRandomInAuction": False,
                                    },
                                    {
                                        "WareMd5": "BLUE-8-4----FEED-XXXXg",
                                        "IsWinnerByRandomInAuction": False,
                                        "IsLoserByRandomInAuction": True,
                                    },
                                ],
                            }
                        },
                    }
                ]
            },
        )
        # yandex_uid = 2, для этого пользователя рандомно выигрывает другой оффер
        response = get_response(rearr_flags_dict, 2)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "wareId": "BLUE-8-4----FEED-XXXXg",
                        "debug": {
                            "buyboxDebug": {
                                "Offers": [
                                    {
                                        "WareMd5": "BLUE-8-3----FEED-XXXXg",
                                        "IsWinnerByRandomInAuction": False,
                                        "IsLoserByRandomInAuction": True,
                                    },
                                    {
                                        "WareMd5": "BLUE-8-4----FEED-XXXXg",
                                        "IsWinnerByRandomInAuction": True,
                                        "IsLoserByRandomInAuction": False,
                                    },
                                ],
                            }
                        },
                    }
                ]
            },
        )
        # yandex_uid = 1, проверяем, что одному и тому же пользователю возвращается одинаковая выдача
        response = get_response(rearr_flags_dict, 1)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "wareId": "BLUE-8-3----FEED-XXXXg",
                    }
                ]
            },
        )

    @skip('https://st.yandex-team.ru/MARKETOUT-40832')
    def test_default_offer_choose_auction_winner_with_rp_fee(self):
        """
        Тестируем что в запросе выбирается оффер победивший аукцион с лучшей ставкой магазина, с учётом rp_fee.
        """

        rearr_flags_dict = base_rearr_flags_dict.copy()
        rearr_flags_dict["market_buybox_auction_transfer_fee_to_card"] = 1
        rearr_flags_dict["market_buybox_auction_rand_low"] = 1.0
        rearr_flags_dict["market_buybox_auction_rand_delta"] = 0.0
        rearr_flags_dict["market_buybox_auction_coef_w"] = 1.0
        # use old conversion for this test
        rearr_flags_dict["market_money_vendor_cpc_to_cpa_conversion_buybox"] = 0.05
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)

        # Проверяем, что при включении коэффициента W начинает применяться rp_fee
        response = self.report.request_json(
            'place=productoffers&market-sku=100002&rgb=green_with_blue&pp=6&rids=213&debug=da&offers-set=defaultList,listCpa&rearr-factors=%s'
            % rearr_flags_str
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                'wareId': "BLUE-100002-FEED-3333Q",
                'benefit': NotEmpty(),
                "debug": {
                    "sale": {
                        'brokeredFee': 93,
                        'shopFee': 100,
                    },
                    "buyboxDebug": {
                        "WonMethod": "WON_BY_AUCTION",
                        'Offers': [
                            {
                                'WareMd5': 'sgf1xWYFqdGiLh4TT-222Q',
                                'AuctionValueRandomized': 20052.5,
                                'ShopFee': 10,
                                'OfferVendorFee': 606,
                            },
                            {
                                'WareMd5': 'BLUE-100002-FEED-2222g',
                                'AuctionValueRandomized': 22273.2,
                                'ShopFee': 50,
                                'OfferVendorFee': 600,
                            },
                            {
                                'WareMd5': 'BLUE-100002-FEED-3333Q',
                                'AuctionValueRandomized': 23215.1,
                                'ShopFee': 100,
                                'OfferVendorFee': 582,
                            },
                        ],
                    },
                },
            },
        )

        # Проверяем значение shopFee для оффера с 1p (shopFee для 1p берётся из RecommendedPriceFee ==600)
        response = self.report.request_json(
            'place=productoffers&market-sku=100003&rgb=green_with_blue&pp=6&rids=213&debug=da&offers-set=defaultList,listCpa&rearr-factors=%s'
            % rearr_flags_str
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                'wareId': "sgf1xWYFqdGiLh4TT-444Q",
                'benefit': NotEmpty(),
                "debug": {
                    "sale": {
                        'brokeredFee': Greater(0),
                        'shopFee': 1000,  # ShopFee == recommendedFee
                    },
                    "buyboxDebug": {
                        "WonMethod": "WON_BY_AUCTION",
                        'Offers': [
                            {
                                'WareMd5': 'sgf1xWYFqdGiLh4TT-444Q',
                                'AuctionValueRandomized': 39631.9,
                                'ShopFee': 1000,  # ShopFee == recommendedFee
                                'OfferVendorFee': 606,
                            },
                            {
                                'WareMd5': 'BLUE-100003-FEED-2222g',
                                'AuctionValueRandomized': 23333.9,
                                'ShopFee': 100,
                                'OfferVendorFee': 600,
                            },
                        ],
                    },
                },
            },
        )

        # Проверяем, что при market_set_1p_fee_recommended=0 shopFee для 1p не выставляется
        rearr_flags_dict["market_set_1p_fee_recommended"] = 0
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)

        response = self.report.request_json(
            'place=productoffers&market-sku=100003&rgb=green_with_blue&pp=6&rids=213&debug=da&offers-set=defaultList,listCpa&rearr-factors=%s'
            % rearr_flags_str
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                'wareId': "BLUE-100003-FEED-2222g",
                'benefit': NotEmpty(),
                "debug": {
                    "sale": {
                        'brokeredFee': 0,
                        'shopFee': 100,
                    },
                    "buyboxDebug": {
                        "WonMethod": "WON_BY_AUCTION",
                        'Offers': [
                            {
                                'WareMd5': 'sgf1xWYFqdGiLh4TT-444Q',
                                'ShopFee': 0,
                                'OfferVendorFee': 606,
                            },
                            {
                                'WareMd5': 'BLUE-100003-FEED-2222g',
                                'ShopFee': 100,
                                'OfferVendorFee': 600,
                            },
                        ],
                    },
                },
            },
        )

    def test_cash_only_shop_coef(self):
        """
        Тестируем, что CashOnlyShopCoef выставляется для cash only офферов в сехак категориях
        """
        response = self.report.request_json(
            'place=productoffers&market-sku=100010&rgb=green_with_blue&pp=6&rids=213&debug=da&offers-set=defaultList,listCpa&rearr-factors=market_buybox_cash_only_shop_coef=0.5'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'wareId': 'BLUE-100010-FEED-1111Q',
                            'debug': {
                                'buyboxDebug': {
                                    'Offers': [
                                        {
                                            'CashOnlyShopCoef': 1,
                                        },
                                    ],
                                }
                            },
                        },
                        {
                            'wareId': 'BLUE-100010-FEED-2222Q',
                            'debug': {
                                'buyboxDebug': {
                                    'Offers': [
                                        {
                                            'CashOnlyShopCoef': 0.5,
                                        },
                                    ],
                                }
                            },
                        },
                    ]
                }
            },
        )

    def test_cash_only_shop_coef_not_cehac(self):
        """
        Тестируем, что CashOnlyShopCoef не выставляется для не cehac категорий
        """
        response = self.report.request_json(
            'place=productoffers&market-sku=100011&rgb=green_with_blue&pp=6&rids=213&debug=da&offers-set=defaultList,listCpa&rearr-factors=market_buybox_cash_only_shop_coef=0.5'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'wareId': 'BLUE-100011-FEED-1111Q',
                            'debug': {
                                'buyboxDebug': {
                                    'Offers': [
                                        {
                                            'CashOnlyShopCoef': 1,
                                        },
                                    ],
                                }
                            },
                        },
                        {
                            'wareId': 'BLUE-100011-FEED-2222Q',
                            'debug': {
                                'buyboxDebug': {
                                    'Offers': [
                                        {
                                            'CashOnlyShopCoef': 1,
                                        },
                                    ],
                                }
                            },
                        },
                    ]
                }
            },
        )

    def test_default_buybox_auction_not_affected_by_kkm_and_cpa_shop_incut(self):
        """
        Проверяем, что флаги, которые меняют аукцион байбокса в ККМ и в cpa_shop_incut, не затрагивают обычный аукцион (который в productoffers)
        За основу взят тест test_default_offer_choose_auction_winner_shop_fee, тут мы проверяем, что AuctionValueRandomized никак не меняется
        """

        rearr_flags_dict = base_rearr_flags_dict.copy()
        rearr_flags_dict["market_buybox_auction_transfer_fee_to_card"] = 1
        rearr_flags_dict["market_buybox_auction_rand_low"] = 1.0
        rearr_flags_dict["market_buybox_auction_rand_delta"] = 0.0
        # use old conversion for this test
        rearr_flags_dict["market_money_vendor_cpc_to_cpa_conversion_buybox"] = 0.05
        rearr_flags_dict["market_buybox_auction_search_sponsored_places_web"] = 0
        rearr_flags_dict["market_buybox_enable_advert_buybox"] = 0
        rearr_flags_str_base = '&rearr-factors=' + dict_to_rearr(rearr_flags_dict)
        base_request = 'place=productoffers&market-sku=100002&rgb=green_with_blue&pp=6&hyperid=3&rids=213&debug=da&offers-set=defaultList,listCpa'

        def check_with_additional_rearrs(new_rearr_factors_dict, request=base_request):
            response = self.report.request_json(
                request
                + rearr_flags_str_base
                + dict_to_rearr(new_rearr_factors_dict)
                + USE_DEPRECATED_DIRECT_SHIPPING_FLOW
            )
            self.assertFragmentIn(
                response,
                {
                    "entity": "offer",
                    "wareId": "BLUE-100002-FEED-3333Q",
                    "benefit": NotEmpty(),
                    "debug": {
                        "buyboxDebug": {
                            "WonMethod": "WON_BY_AUCTION",
                            "Offers": [
                                {
                                    'WareMd5': 'sgf1xWYFqdGiLh4TT-222Q',
                                    'AuctionValueRandomized': 31894.5,
                                },
                                {
                                    'WareMd5': 'BLUE-100002-FEED-2222g',
                                    'AuctionValueRandomized': 35000.8,
                                },
                                {
                                    'WareMd5': 'BLUE-100002-FEED-3333Q',
                                    'AuctionValueRandomized': 36088.5,
                                    'AuctionedShopFee': 92,
                                },
                            ],
                        },
                    },
                },
            )

        # Будем проверять запросами в productoffers (чтобы проверить аукцион на КМ) и в prime (чтобы проверить консистентность при вызове плейса TProductDefaultOffer
        # через ScheduleDefaultOffers)
        for request in (
            'place=productoffers&market-sku=100002&rgb=green_with_blue&pp=6&hyperid=3&rids=213&debug=da&offers-set=defaultList,listCpa',
            'place=prime&pp=7&text=msku_2&rids=213&debug=da&use-default-offers=1&allow-collapsing=1&debug=da',
        ):
            # Проверим сначала, что AuctionValueRandomized заданы корректно для выбранных параметров дефолтного аукциона в байбоксе
            check_with_additional_rearrs({}, request=request)

            # Проверим, что параметры аукциона в байбоксе для ККМ не затрагивают дефолтный аукцион в байбоксе
            check_with_additional_rearrs(
                {
                    'market_buybox_auction_cpa_competitive_model_card': 1,
                    'market_competitive_model_card_closeness_threshold': 10,
                    'market_buybox_auction_coef_a_additive_bid_coef_kkm': 1.5,
                    'market_buybox_auction_coef_b_multiplicative_bid_coef_kkm': 0.01,
                    'market_buybox_auction_coef_e_additive_coef_inside_bid_kkm': 0.2,
                    'market_buybox_auction_coef_f_div_price_coef_in_bid_kkm': 100,
                    'market_buybox_auction_coef_c_div_price_coef_inside_bid_kkm': 200,
                    'market_buybox_auction_coef_d_global_additive_coef_kkm': 500,
                    'market_buybox_auction_coef_g_ue_add_coef_kkm': 0.2,
                    'market_buybox_auction_coef_w_rp_fee_coef_kkm': 0,
                },
                request=request,
            )

            # Проверим, что параметры аукциона в байбоксе для cpa_shop_incut не затрагивают дефолтный аукцион в байбоксе
            check_with_additional_rearrs(
                {
                    'market_buybox_auction_coef_a_additive_bid_coef_cs_incut': 1.5,
                    'market_buybox_auction_coef_b_multiplicative_bid_coef_cs_incut': 0.01,
                    'market_buybox_auction_coef_e_additive_coef_inside_bid_cs_incut': 0.2,
                    'market_buybox_auction_coef_f_div_price_coef_in_bid_cs_incut': 100,
                    'market_buybox_auction_coef_c_div_price_coef_inside_bid_cs_incut': 200,
                    'market_buybox_auction_coef_d_global_additive_coef_cs_incut': 500,
                    'market_buybox_auction_coef_g_ue_add_coef_cs_incut': 0.2,
                    'market_buybox_auction_coef_w_rp_fee_coef_cs_incut': 0,
                },
                request=request,
            )


if __name__ == '__main__':
    main()
