#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    DeliveryBucket,
    DeliveryOption,
    MarketSku,
    Model,
    Offer,
    Outlet,
    OutletDeliveryOption,
    PickupBucket,
    PickupOption,
    Region,
    RegionalDelivery,
    Shop,
    ShopPaymentMethods,
    PaymentRegionalGroup,
    Payment,
)
from core.testcase import TestCase, main
from core.matcher import Absent
from core.types.delivery import BlueDeliveryTariff


class _Shops:
    shop_for_delivery_filter_tests = Shop(fesh=19, priority_region=213, cpa=Shop.CPA_REAL, name='Shop for filter tests')
    shop_for_free_dsbs_retail_delivery = Shop(
        fesh=22,
        priority_region=213,
        cpa=Shop.CPA_REAL,
        name='Shop for free DSBS retail delivery tests',
        delivery_service_outlets=[503],
    )
    shop_for_not_listed_dsbs_retail_delivery = Shop(
        fesh=23,
        priority_region=213,
        cpa=Shop.CPA_REAL,
        name='Shop for free DSBS retail delivery tests not in list',
        delivery_service_outlets=[504],
    )
    shop_for_listed_dsbs_retail_delivery_custom = Shop(
        fesh=24,
        priority_region=213,
        cpa=Shop.CPA_REAL,
        name='Shop for free DSBS retail delivery tests with random delivery',
        delivery_service_outlets=[505],
    )
    shop_for_multiple_dsbs_retail_delivery = Shop(
        fesh=25,
        priority_region=213,
        cpa=Shop.CPA_REAL,
        name='Shop for free DSBS retail delivery multiple',
        delivery_service_outlets=[506, 507],
    )
    shop_with_free_dsbs_delivery = Shop(
        fesh=50,
        priority_region=213,
        cpa=Shop.CPA_REAL,
        name='Shop for free DSBS delivery',
    )


class _DeliveryBuckets:
    bucket_on_the_same_day = DeliveryBucket(
        bucket_id=421,
        fesh=19,
        carriers=[147],
        regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=150, day_from=0, day_to=0)])],
        delivery_program=DeliveryBucket.REGULAR_PROGRAM,
    )
    bucket_on_the_next_day = DeliveryBucket(
        bucket_id=422,
        fesh=19,
        carriers=[147],
        regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=150, day_from=1, day_to=1)])],
        delivery_program=DeliveryBucket.REGULAR_PROGRAM,
    )
    bucket_on_the_second_day = DeliveryBucket(
        bucket_id=423,
        fesh=19,
        carriers=[147],
        regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=150, day_from=2, day_to=2)])],
        delivery_program=DeliveryBucket.REGULAR_PROGRAM,
    )
    bucket_in_a_week = DeliveryBucket(
        bucket_id=425,
        fesh=19,
        carriers=[147],
        regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=150, day_from=4, day_to=4)])],
        delivery_program=DeliveryBucket.REGULAR_PROGRAM,
    )
    bucket_with_long_period = DeliveryBucket(
        bucket_id=430,
        fesh=50,
        carriers=[147],
        regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=150, day_from=0, day_to=4)])],
        delivery_program=DeliveryBucket.REGULAR_PROGRAM,
    )


class _Mskus:
    msku_same_day_delivery = MarketSku(hyperid=518, sku=301)
    msku_next_day_delivery = MarketSku(hyperid=519, sku=302)
    msku_second_day_delivery = MarketSku(hyperid=520, sku=303)
    msku_week_delivery = MarketSku(hyperid=521, sku=304)
    msku_free_dsbs_delivery = MarketSku(hyperid=522, sku=305)
    msku_with_multiple_warehouses_with_different_prices = MarketSku(hyperid=523, sku=306)


class _Offers:
    offer_same_day_delivery = Offer(
        fesh=_Shops.shop_for_delivery_filter_tests.fesh,
        hyperid=518,
        cpa=Offer.CPA_REAL,
        title="OFFER WITH DELIVERY ON THE SAME DAY",
        delivery_buckets=[_DeliveryBuckets.bucket_on_the_same_day.bucket_id],
        sku=_Mskus.msku_same_day_delivery.sku,
        waremd5='DeliverySameDay______g',
    )
    offer_next_day_delivery = Offer(
        fesh=_Shops.shop_for_delivery_filter_tests.fesh,
        hyperid=519,
        cpa=Offer.CPA_REAL,
        title="OFFER WITH DELIVERY ON THE NEXT DAY",
        delivery_buckets=[_DeliveryBuckets.bucket_on_the_next_day.bucket_id],
        sku=_Mskus.msku_next_day_delivery.sku,
        waremd5='DeliveryNextDay______g',
    )
    offer_second_day_delivery = Offer(
        fesh=_Shops.shop_for_delivery_filter_tests.fesh,
        hyperid=520,
        cpa=Offer.CPA_REAL,
        title="OFFER WITH DELIVERY ON THE SECOND DAY",
        delivery_buckets=[_DeliveryBuckets.bucket_on_the_second_day.bucket_id],
        sku=_Mskus.msku_second_day_delivery.sku,
        waremd5='DeliverySeconsDay____g',
    )
    offer_week_delivery = Offer(
        fesh=_Shops.shop_for_delivery_filter_tests.fesh,
        hyperid=521,
        cpa=Offer.CPA_REAL,
        title="OFFER WITH DELIVERY DURING THE WEEK",
        delivery_buckets=[_DeliveryBuckets.bucket_in_a_week.bucket_id],
        sku=_Mskus.msku_week_delivery.sku,
        waremd5='DeliveryWeek_________g',
    )
    offer_with_multiple_warehouses_with_different_prices = Offer(
        fesh=_Shops.shop_for_multiple_dsbs_retail_delivery.fesh,
        hyperid=_Mskus.msku_with_multiple_warehouses_with_different_prices.hyperid,
        cpa=Offer.CPA_REAL,
        title="OFFER WITH MULTIPLE DIFFERENT COST OUTLETS",
        pickup_buckets=[525],
        sku=_Mskus.msku_with_multiple_warehouses_with_different_prices.sku,
        waremd5='DeliveryMult_________g',
    )
    usual_white_offer = Offer(hyperid=518, fesh=20)
    offer_from_dsbs_shop = Offer(
        fesh=_Shops.shop_with_free_dsbs_delivery.fesh,
        hyperid=600,
        cpa=Offer.CPA_REAL,
        title="OFFER WITH FREE DELIVERY UNDER FLAG",
        delivery_buckets=[_DeliveryBuckets.bucket_with_long_period.bucket_id],
        waremd5='FreeDsbsDelivery_____g',
    )
    offer_from_dsbs_shop_2 = Offer(
        fesh=_Shops.shop_with_free_dsbs_delivery.fesh,
        hyperid=601,
        cpa=Offer.CPA_REAL,
        title="OFFER2 WITH FREE DELIVERY UNDER FLAG",
        delivery_options=[DeliveryOption(price=100, day_from=3, day_to=4, order_before=14)],
        waremd5='FreeDsbsDelivery2____g',
    )


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.settings.enable_testing_features = False
        cls.index.free_dsbs_delivery_shops += [50]

        cls.index.regiontree += [
            Region(rid=213, name='Moscow', tz_offset=10800),
            Region(rid=214, name='Novosibirsk', tz_offset=10800),
            Region(rid=75, name='Vladivostok', tz_offset=10800),
            Region(rid=76, name='Khabarovsk', tz_offset=10800),
        ]

        cls.index.shops += [
            Shop(fesh=15, priority_region=213, cpa=Shop.CPA_REAL),
            Shop(fesh=16, priority_region=213, cpa=Shop.CPA_REAL),
            Shop(fesh=17, priority_region=213, cpa=Shop.CPA_REAL, cpc=Shop.CPC_NO, is_cpa_partner=True),
            Shop(fesh=18, priority_region=213, cpa=Shop.CPA_REAL),
            _Shops.shop_for_delivery_filter_tests,
            _Shops.shop_for_free_dsbs_retail_delivery,
            _Shops.shop_for_not_listed_dsbs_retail_delivery,
            _Shops.shop_for_listed_dsbs_retail_delivery_custom,
            Shop(fesh=20, priority_region=213, cpa=Shop.CPA_REAL),
            Shop(fesh=21, priority_region=213, cpa=Shop.CPA_REAL),
            Shop(fesh=30, priority_region=213, cpa=Shop.CPA_REAL, is_cpa_partner=True),
            Shop(fesh=175, priority_region=75, cpa=Shop.CPA_REAL),
            Shop(fesh=176, priority_region=76, cpa=Shop.CPA_REAL),
            _Shops.shop_with_free_dsbs_delivery,
        ]

        cls.index.outlets += [
            Outlet(
                point_id=114,
                fesh=15,
                delivery_option=OutletDeliveryOption(
                    day_from=1, day_to=1, order_before=2, work_in_holiday=True, price=505, shipper_id=147
                ),
                region=213,
                point_type=Outlet.FOR_PICKUP,
            ),
            Outlet(
                point_id=115,
                fesh=15,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(day_from=3, day_to=5, order_before=16, price=500, price_to=1000),
                working_days=[i for i in range(10)],
            ),
            Outlet(
                point_id=116,
                fesh=16,
                region=213,
                point_type=Outlet.MIXED_TYPE,
                delivery_option=OutletDeliveryOption(day_from=1, day_to=7, order_before=20, price=300, price_to=1000),
                working_days=[i for i in range(10)],
            ),
            Outlet(
                point_id=120,
                fesh=20,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(day_from=1, day_to=7, order_before=10, price=678),
                working_days=[i for i in range(10)],
            ),
            Outlet(
                point_id=501,
                fesh=21,
                delivery_option=OutletDeliveryOption(
                    day_from=1, day_to=1, order_before=2, work_in_holiday=True, price=501
                ),
                region=213,
                point_type=Outlet.FOR_STORE,
            ),
            Outlet(
                point_id=502,
                fesh=22,
                delivery_option=OutletDeliveryOption(
                    day_from=1, day_to=1, order_before=2, work_in_holiday=True, price=502
                ),
                region=213,
                point_type=Outlet.FOR_PICKUP,
            ),
            Outlet(
                point_id=503,
                delivery_service_id=147,
                delivery_option=OutletDeliveryOption(
                    day_from=1, day_to=1, order_before=2, work_in_holiday=True, price=503
                ),
                region=213,
                point_type=Outlet.FOR_PICKUP,
            ),
            Outlet(
                point_id=504,
                fesh=23,
                delivery_option=OutletDeliveryOption(
                    day_from=1, day_to=1, order_before=2, work_in_holiday=True, price=504
                ),
                region=213,
                point_type=Outlet.FOR_PICKUP,
            ),
            Outlet(
                point_id=505,
                fesh=24,
                delivery_option=OutletDeliveryOption(
                    day_from=1, day_to=1, order_before=2, work_in_holiday=True, price=505, shipper_id=147
                ),
                region=213,
                point_type=Outlet.FOR_PICKUP,
            ),
            Outlet(
                point_id=506,
                fesh=_Shops.shop_for_multiple_dsbs_retail_delivery.fesh,
                delivery_option=OutletDeliveryOption(
                    day_from=1,
                    day_to=1,
                    order_before=2,
                    work_in_holiday=True,
                    price=506,
                ),
                region=213,
                point_type=Outlet.FOR_PICKUP,
            ),
            Outlet(
                point_id=507,
                fesh=_Shops.shop_for_multiple_dsbs_retail_delivery.fesh,
                delivery_option=OutletDeliveryOption(
                    day_from=1, day_to=1, order_before=2, work_in_holiday=True, price=507, shipper_id=147
                ),
                region=213,
                point_type=Outlet.FOR_PICKUP,
            ),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=416,
                fesh=15,
                carriers=[99],
                options=[PickupOption(outlet_id=114, day_from=3, day_to=5, price=500)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=417,
                fesh=15,
                carriers=[99],
                options=[PickupOption(outlet_id=115, day_from=3, day_to=5, price=500)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=419,
                fesh=15,
                carriers=[99],
                options=[PickupOption(outlet_id=115, day_from=3, day_to=5, price=0)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=430,
                fesh=20,
                carriers=[99],
                options=[PickupOption(outlet_id=120, day_from=1, day_to=7, price=678)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=521,
                fesh=21,
                carriers=[99],
                options=[PickupOption(outlet_id=501, day_from=1, day_to=7, price=501)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=522,
                fesh=22,
                carriers=[99],
                options=[PickupOption(outlet_id=502, day_from=1, day_to=7, price=502)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=523,
                fesh=24,
                carriers=[99],
                options=[PickupOption(outlet_id=505, day_from=1, day_to=7, price=503)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=524,
                fesh=23,
                carriers=[99],
                options=[PickupOption(outlet_id=504, day_from=1, day_to=7, price=504)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=525,
                fesh=_Shops.shop_for_multiple_dsbs_retail_delivery.fesh,
                carriers=[99],
                options=[
                    PickupOption(outlet_id=506, day_from=1, day_to=7, price=506),
                    PickupOption(outlet_id=507, day_from=1, day_to=7, price=507),
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=418,
                fesh=16,
                carriers=[99],
                regional_options=[
                    RegionalDelivery(
                        rid=213,
                        options=[
                            DeliveryOption(day_from=0, day_to=15, order_before=6, shop_delivery_price=140),
                        ],
                        payment_methods=[Payment.PT_YANDEX, Payment.PT_CASH_ON_DELIVERY, Payment.PT_CARD_ON_DELIVERY],
                    )
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=419,
                fesh=17,
                carriers=[1, 3],
                regional_options=[
                    RegionalDelivery(
                        rid=214,
                        options=[DeliveryOption(price=500, day_from=1, day_to=4, order_before=23)],
                        payment_methods=[Payment.PT_CASH_ON_DELIVERY, Payment.PT_CARD_ON_DELIVERY],
                    )
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=420,
                fesh=18,
                carriers=[147],
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=150, day_from=1, day_to=4)])],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=431,
                fesh=18,
                carriers=[147],
                regional_options=[RegionalDelivery(rid=214, options=[DeliveryOption(price=890, day_from=3, day_to=4)])],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            _DeliveryBuckets.bucket_on_the_same_day,
            _DeliveryBuckets.bucket_on_the_next_day,
            _DeliveryBuckets.bucket_on_the_second_day,
            _DeliveryBuckets.bucket_in_a_week,
            _DeliveryBuckets.bucket_with_long_period,
        ]

        cls.index.models += [
            Model(hyperid=510),
            Model(hyperid=511),
            Model(hyperid=512),
            Model(hyperid=513),
            Model(hyperid=514),
            Model(hyperid=515),
            Model(hyperid=516),
            Model(hyperid=517),
            Model(hyperid=520),
        ]

        cls.index.offers += [
            Offer(
                fesh=15,
                hyperid=510,
                cpa=Offer.CPA_REAL,
                title="OFFER WITH THIRD-PARTY SHIPPER",
                pickup_option=DeliveryOption(price=350, day_from=1, day_to=7, order_before=10),
                pickup_buckets=[416],
                has_delivery_options=False,
            ),
            Offer(
                fesh=15,
                hyperid=511,
                cpa=Offer.CPA_REAL,
                title="OFFER WITH PICKUP BUCKET",
                waremd5='MtR100q_tyqFJLn3KKrggg',
                pickup_buckets=[417],
                has_delivery_options=False,
            ),
            Offer(
                fesh=15,
                hyperid=512,
                cpa=Offer.CPA_REAL,
                title="OFFER WITH DELIVERY BUCKET",
                delivery_buckets=[418],
                waremd5='MMR100q_tyqFJLn3KKrggg',
            ),
            Offer(
                fesh=15,
                hyperid=513,
                pickup_option=DeliveryOption(price=350, day_from=1, day_to=7, order_before=10),
                cpa=Offer.CPA_REAL,
                title="OFFER WITH SELF PICKUP OPTIONS IN SHOP 15",
                pickup_buckets=[417],
                has_delivery_options=False,
            ),
            Offer(
                fesh=15,
                hyperid=514,
                cpa=Offer.CPA_REAL,
                title="OFFER WITHOUT PICKUP OPTIONS",
                pickup_buckets=[417],
                pickup=False,
            ),
            Offer(
                fesh=15,
                hyperid=520,
                pickup_option=DeliveryOption(price=0, day_from=1, day_to=7, order_before=10),
                cpa=Offer.CPA_REAL,
                title="OFFER WITH FREE SELF PICKUP OPTIONS IN SHOP 15",
                pickup_buckets=[419],
                has_delivery_options=False,
            ),
            Offer(
                fesh=16,
                hyperid=515,
                delivery_options=[DeliveryOption(price=100, day_from=3, day_to=4, order_before=14)],
                cpa=Offer.CPA_REAL,
                title="OFFER WITH SELF DELIVERY OPTIONS IN SHOP 16",
                delivery_buckets=[418],
                waremd5='MtR100q_tyqFJLn3KKrELp',
            ),
            Offer(
                fesh=16,
                hyperid=516,
                cpa=Offer.CPA_REAL,
                title="OFFER WITHOUT DELIVERY OPTIONS",
                delivery_buckets=[418],
                has_delivery_options=False,
            ),
            Offer(
                fesh=17,
                hyperid=517,
                cpa=Offer.CPA_REAL,
                title="OFFER WITH REGION DELIVERY OPTIONS FROM MOSCOW",
                delivery_buckets=[419],
                waremd5='DuE098x_rinQLZn3KKrELw',
            ),
            Offer(
                fesh=18,
                available=False,
                cpa=Offer.CPA_REAL,
                title="OFFER DSBS UNDER THE ORDER",
                delivery_buckets=[420],
                waremd5='DsbsUnderTheOrder____g',
            ),
            Offer(
                fesh=18,
                available=False,
                cpa=Offer.CPA_NO,
                title="OFFER CPC UNDER THE ORDER",
                delivery_buckets=[420],
                waremd5='CPCUnderTheOrder_____g',
            ),
            Offer(
                fesh=20,
                available=False,
                cpa=Offer.CPA_NO,
                title="OFFER CPC WITH DELIVERY OPTIONS",
                pickup_option=DeliveryOption(price=678, day_from=1, day_to=7, order_before=10),
                pickup_buckets=[430],
                delivery_options=[DeliveryOption(price=890, day_from=3, day_to=4, order_before=14)],
                delivery_buckets=[431],
                waremd5='CPCWithDelivery______g',
            ),
            Offer(
                fesh=21,
                hyperid=522,
                cpa=Offer.CPA_REAL,
                title="OFFER WITH RETAIL OUTLET",
                pickup_buckets=[521],
                waremd5="RetailTest___________g",
            ),
            Offer(
                fesh=22,
                hyperid=523,
                cpa=Offer.CPA_REAL,
                title="OFFER FOR PLUS TEST",
                pickup_buckets=[522],
                waremd5="PlusTest_____________g",
                price=2250,
            ),
            Offer(
                fesh=22,
                hyperid=522,
                cpa=Offer.CPA_REAL,
                title="OFFER WITH PICKUP OUTLET",
                pickup_buckets=[522],
                waremd5="PickupTest___________g",
            ),
            Offer(
                fesh=24,
                hyperid=522,
                cpa=Offer.CPA_REAL,
                title="OFFER WITH PICKUP OUTLET AND CUSTOM SERVICE",
                pickup_buckets=[523],
                waremd5="PickupTest2__________g",
            ),
            Offer(
                fesh=23,
                hyperid=522,
                cpa=Offer.CPA_REAL,
                title="OFFER WITH OUTLET DSBS NOT IN LIST",
                pickup_buckets=[524],
                waremd5="PickupTest3__________g",
            ),
            Offer(
                fesh=30,
                hyperid=522,
                cpa=Offer.CPA_REAL,
                title="DSBS OFFER IN SHOP 30",
                waremd5="DSBS_OFFER_FESH_30___g",
            ),
            _Offers.offer_same_day_delivery,
            _Offers.offer_next_day_delivery,
            _Offers.offer_second_day_delivery,
            _Offers.offer_week_delivery,
            _Offers.usual_white_offer,
            _Offers.offer_with_multiple_warehouses_with_different_prices,
            Offer(
                fesh=175,
                cpa=Offer.CPA_REAL,
                title="DSBS OFFER IN SHOP 175",
                waremd5="DSBS_OFFER_FESH_175__g",
            ),
            Offer(
                fesh=176,
                cpa=Offer.CPA_REAL,
                title="DSBS OFFER IN SHOP 176",
                waremd5="DSBS_OFFER_FESH_176__g",
            ),
            _Offers.offer_from_dsbs_shop,
            _Offers.offer_from_dsbs_shop_2,
        ]

        cls.index.mskus += [
            _Mskus.msku_same_day_delivery,
            _Mskus.msku_next_day_delivery,
            _Mskus.msku_second_day_delivery,
            _Mskus.msku_week_delivery,
            _Mskus.msku_free_dsbs_delivery,
            _Mskus.msku_with_multiple_warehouses_with_different_prices,
        ]

        cls.index.blue_delivery_modifiers.set_default_modifier(
            tariffs=[
                BlueDeliveryTariff(user_price=100, courier_price=200, pickup_price=300, post_price=400, large_size=0),
                BlueDeliveryTariff(user_price=100, courier_price=200, pickup_price=300, post_price=400, large_size=1),
            ]
        )

        cls.index.blue_delivery_modifiers.add_modifier(
            tariffs=[
                BlueDeliveryTariff(
                    user_price=111, courier_price=222, pickup_price=333, post_price=444, price_to=700, large_size=0
                ),
                BlueDeliveryTariff(user_price=0, courier_price=0, pickup_price=0, post_price=0, large_size=0),
                BlueDeliveryTariff(user_price=100, courier_price=200, pickup_price=300, post_price=400, large_size=1),
            ],
            regions=[213],
            also_use_for_dsbs_in_priority_region=True,  # Этот модификатор указывает тариф (222 руб. курьерка), который будет использоваться
            # для DSBS доставки в приоритетный регион 75 — для случая переиспользования
            # имеющегося тарифа а-ля "tier_1"
        )

        cls.index.blue_delivery_modifiers.add_modifier(
            tariffs=[
                BlueDeliveryTariff(user_price=0, courier_price=222, large_size=0),
                BlueDeliveryTariff(user_price=0, courier_price=222, large_size=1),
            ],
            regions=[76],
            only_use_for_dsbs_in_priority_region=True,  # А этот модификатор указывает тариф (222 руб. курьерка), который будет использоваться
            # для DSBS доставки в приоритетный регион 76 — тестируем кейс с отдельным
            # тарифом без переиспользования а-ля "tier_1" (на будущее, для гибкости)
        )

        # Модификатор ниже нужен, чтобы проверить, что без фикса https://st.yandex-team.ru/MARKETOUT-45383
        # DSBS доставка в приоритетные регионы 75 и 76 (будет 999 руб.) отличается от указанной выше
        cls.index.blue_delivery_modifiers.add_modifier(
            tariffs=[
                BlueDeliveryTariff(user_price=0, courier_price=999, large_size=0),
                BlueDeliveryTariff(user_price=0, courier_price=999, large_size=1),
            ],
            regions=[75, 76],
        )

        cls.index.shops_payment_methods += [
            ShopPaymentMethods(
                fesh=15,
                payment_groups=[
                    PaymentRegionalGroup(
                        included_regions=[213],
                        payment_methods=[
                            Payment.PT_YANDEX,
                            Payment.PT_PREPAYMENT_CARD,
                            Payment.PT_CASH_ON_DELIVERY,
                            Payment.PT_CARD_ON_DELIVERY,
                        ],
                    ),
                ],
            )
        ]

        cls.index.shops_payment_methods += [
            ShopPaymentMethods(
                fesh=17,
                payment_groups=[
                    PaymentRegionalGroup(
                        included_regions=[214],
                        payment_methods=[
                            Payment.PT_CASH_ON_DELIVERY,
                            Payment.PT_CARD_ON_DELIVERY,
                        ],
                    ),
                ],
            )
        ]

    def test_offers_with_pickup_options_geo(self):
        for hyperid in ('510', '513', '514'):
            response = self.report.request_json(
                'place=geo&hyperid={}&rids=213&pickup-options=raw&rgb=green_with_blue'.format(hyperid)
            )

            # У этого оффера есть собственные опции самовывоза в домашнем регионе,
            # они на выдаче заменяют опции всех аутлетов этого магазина в домашнем регионе.
            if hyperid == '513':
                self.assertFragmentIn(
                    response,
                    {
                        "titles": {"raw": "OFFER WITH SELF PICKUP OPTIONS IN SHOP 15"},
                        "delivery": {
                            "pickupOptions": [
                                {
                                    "serviceId": 99,
                                    "partnerType": "regular",
                                    "price": {
                                        "currency": "RUR",
                                        "value": "0",
                                    },  # цена берется из тарифов. Оффер удовлетворяет условиям бесплатного самовывоза (своя служба + тип аутлета)
                                    "dayFrom": 1,
                                    "dayTo": 7,
                                    "orderBefore": 10,
                                    "outlet": {"id": "115", "purpose": ["pickup"], "type": "pickup"},
                                }
                            ]
                        },
                        "outlet": {
                            "id": "115",
                            "type": "pickup",
                            "serviceId": 99,
                            "isMarketBranded": False,
                            "selfDeliveryRule": {
                                "currency": "RUR",
                                "cost": "350",
                                "dayFrom": 1,
                                "dayTo": 7,
                                "orderBefore": 10,
                            },
                        },
                    },
                )

            # у этого оффера сторонний shipper - поэтому цена берётся из тарифов, самовывоз не бесплатный
            if hyperid == '510':
                self.assertFragmentIn(
                    response,
                    {
                        "titles": {"raw": "OFFER WITH THIRD-PARTY SHIPPER"},
                        "delivery": {
                            "pickupOptions": [
                                {
                                    "serviceId": 147,
                                    "partnerType": "regular",
                                    "price": {"currency": "RUR", "value": "333"},  # цена берется из тарифов.
                                    "dayFrom": 1,
                                    "dayTo": 7,
                                    "orderBefore": 10,
                                    "outlet": {"id": "114", "purpose": ["pickup"], "type": "pickup"},
                                }
                            ]
                        },
                        "outlet": {
                            "id": "114",
                            "type": "pickup",
                            "serviceId": 147,
                            "isMarketBranded": False,
                            "selfDeliveryRule": {
                                "currency": "RUR",
                                "cost": "350",
                                "dayFrom": 1,
                                "dayTo": 7,
                                "orderBefore": 10,
                            },
                        },
                    },
                )

            # Для этого оффера стоит флаг pickup=False,
            # он виден в списке всех офферов, но не проходит в выдачу.
            if hyperid == '514':
                self.assertFragmentIn(response, {"totalOffers": 0, "totalOffersBeforeFilters": 1})

    def test_offer_with_self_pickup_options_prime(self):
        # Фильтр по магазину 15.
        response = self.report.request_json('place=prime&fesh=15&rids=213&pickup-options=raw&rgb=green_with_blue')

        # У этого оффера есть собственные опции самовывоза в домашнем регионе,
        # они на выдаче заменяют опции всех аутлетов этого магазина в домашнем регионе.
        self.assertFragmentIn(
            response,
            {
                "titles": {"raw": "OFFER WITH SELF PICKUP OPTIONS IN SHOP 15"},
                "delivery": {
                    "pickupOptions": [
                        {
                            "serviceId": 99,
                            "partnerType": "regular",
                            "price": {
                                "currency": "RUR",
                                "value": "0",
                            },  # цена берется из тарифов. Попадает под условия бесплатного самовывоза
                            "dayFrom": 1,
                            "dayTo": 7,
                            "orderBefore": 10,
                        }
                    ]
                },
                "outlet": {
                    "id": "115",
                    "type": "pickup",
                    "serviceId": 99,
                    "isMarketBranded": False,
                    "selfDeliveryRule": {
                        "currency": "RUR",
                        "cost": "350",
                    },
                },
            },
        )
        self.assertFragmentIn(
            response,
            {
                "titles": {"raw": "OFFER WITH THIRD-PARTY SHIPPER"},
                "delivery": {
                    "pickupOptions": [
                        {
                            "serviceId": 147,
                            "partnerType": "regular",
                            "price": {
                                "currency": "RUR",
                                "value": "333",
                            },  # цена берется из тарифов. Самовывоз платный (сторонняя служба)
                            "dayFrom": 1,
                            "dayTo": 7,
                            "orderBefore": 10,
                        }
                    ]
                },
                "outlet": {
                    "id": "114",
                    "type": "pickup",
                    "serviceId": 147,
                    "isMarketBranded": False,
                    "selfDeliveryRule": {
                        "currency": "RUR",
                        "cost": "350",
                    },
                },
            },
        )

        # Для этого оффера стоит флаг pickup=False.
        self.assertFragmentIn(
            response, {"titles": {"raw": "OFFER WITHOUT PICKUP OPTIONS"}, "delivery": {"hasPickup": False}}
        )

    def test_offer_with_self_pickup_options_productoffers(self):
        response = self.report.request_json(
            'place=productoffers&hyperid=513&rids=213&rgb=green_with_blue&pickup-options=raw'
        )

        # У этого оффера есть собственные опции самовывоза в домашнем регионе,
        # они на выдаче заменяют опции всех аутлетов этого магазина в домашнем регионе.
        self.assertFragmentIn(
            response,
            {
                "titles": {"raw": "OFFER WITH SELF PICKUP OPTIONS IN SHOP 15"},
                "delivery": {
                    "pickupOptions": [
                        {
                            "serviceId": 99,
                            "partnerType": "regular",
                            "price": {
                                "currency": "RUR",
                                "value": "0",
                            },  # цена берется из тарифов. Доставка бесплатная в случае доставки в торговый зал
                            "dayFrom": 1,
                            "dayTo": 7,
                            "orderBefore": 10,
                        }
                    ]
                },
                "outlet": {
                    "id": "115",
                    "type": "pickup",
                    "serviceId": 99,
                    "isMarketBranded": False,
                    "selfDeliveryRule": {
                        "currency": "RUR",
                        "cost": "350",
                    },
                },
            },
        )

        # проверяем случай платного самовывоза (со сторонней службой перевозки)
        response = self.report.request_json(
            'place=productoffers&hyperid=510&rids=213&rgb=green_with_blue&pickup-options=raw'
        )

        self.assertFragmentIn(
            response,
            {
                "titles": {"raw": "OFFER WITH THIRD-PARTY SHIPPER"},
                "delivery": {
                    "pickupOptions": [
                        {
                            "serviceId": 147,
                            "partnerType": "regular",
                            "price": {
                                "currency": "RUR",
                                "value": "333",
                            },  # цена берется из тарифов. Доставка бесплатная в случае доставки в торговый зал
                            "dayFrom": 1,
                            "dayTo": 7,
                            "orderBefore": 10,
                        }
                    ]
                },
                "outlet": {
                    "id": "114",
                    "type": "pickup",
                    "serviceId": 147,
                    "isMarketBranded": False,
                    "selfDeliveryRule": {
                        "currency": "RUR",
                        "cost": "350",
                    },
                },
            },
        )

    def test_offer_with_self_delivery_options(self):
        for request in (
            # Фильтр по магазину 16.
            'place=prime&fesh=16&rids=213&rgb=green_with_blue',
            # Офферы для модели 515.
            'place=productoffers&hyperid=515&rids=213&rgb=green_with_blue',
            # Конкретный оффер с offerid MtR100q_tyqFJLn3KKrELp.
            'place=offerinfo&offerid=MtR100q_tyqFJLn3KKrELp&regset=1&rids=213&rgb=green_with_blue',
        ):

            unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
            response = self.report.request_json(request + unified_off_flags)

            # У этого оффера есть собственные опции доставки в домашнем регионе,
            # проверяем, что они появляются в опциях доставки.
            self.assertFragmentIn(
                response,
                {
                    "titles": {"raw": "OFFER WITH SELF DELIVERY OPTIONS IN SHOP 16"},
                    "delivery": {
                        "availableServices": [{"serviceId": 99}],
                        "options": [
                            {
                                "isDefault": True,
                                "price": {"currency": "RUR", "value": "100"},
                                "dayFrom": 3,
                                "dayTo": 4,
                                "orderBefore": "14",
                            }
                        ],
                    },
                },
            )

    def test_offer_with_region_delivery_options(self):
        # Проверяем доставку в регион 214
        for request in (
            # Фильтр по магазину 17.
            'place=prime&fesh=17&rids=214&rgb=green_with_blue',
            # Офферы для модели 515.
            'place=productoffers&hyperid=517&rids=214&rgb=green_with_blue',
            # Конкретный оффер с offerid DuE098x_rinQLZn3KKrELw.
            'place=offerinfo&offerid=DuE098x_rinQLZn3KKrELw&regset=1&rids=214&rgb=green_with_blue',
        ):

            unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
            response = self.report.request_json(request + unified_off_flags)

            self.assertFragmentIn(
                response,
                {
                    "titles": {"raw": "OFFER WITH REGION DELIVERY OPTIONS FROM MOSCOW"},
                    "delivery": {
                        "availableServices": [{"serviceId": 1}, {"serviceId": 3}],
                        "options": [
                            {
                                "price": {"currency": "RUR", "value": "500"},
                                "dayFrom": 1,
                                "dayTo": 4,
                                "orderBefore": "23",
                            }
                        ],
                    },
                },
            )

    def test_dsbs_under_the_order_filtering(self):
        """
        Проверяем фильтрацию ТОЛЬКО dsbs офферов, у которых неопределенные сроки доставки (или "под заказ")
        (под флагом)
        MARKETOUT-37990 market_filter_dsbs_under_order
        """

        unified_off_flags = ';market_dsbs_tariffs=0;market_unified_tariffs=0'
        request = (
            'place=prime&fesh=18&rids=213&rgb=green_with_blue&rearr-factors=market_filter_dsbs_under_order=1'
            + unified_off_flags
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "wareId": "CPCUnderTheOrder_____g",
                        "delivery": {
                            "options": [
                                {
                                    "dayFrom": Absent(),
                                    "dayTo": Absent(),
                                    "partnerType": "regular",
                                    "price": {"value": "150"},
                                    "serviceId": "147",
                                }
                            ],
                        },
                    }
                ]
            },
            allow_different_len=False,
        )

        request = (
            'place=offerinfo&pp=18&regset=2&rids=213&offerid=DsbsUnderTheOrder____g&show-urls=direct&rearr-factors=market_filter_dsbs_under_order=1'
            + unified_off_flags
        )
        response = self.report.request_json(request)
        self.assertFragmentNotIn(response, {"results": [{"entity": "offer", "wareId": "DsbsUnderTheOrder____g"}]})

        request = (
            'place=prime&fesh=18&rids=213&rgb=green_with_blue&rearr-factors=market_filter_dsbs_under_order=0'
            + unified_off_flags
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "wareId": "CPCUnderTheOrder_____g",
                        "delivery": {
                            "options": [
                                {
                                    "dayFrom": Absent(),
                                    "dayTo": Absent(),
                                    "partnerType": "regular",
                                    "price": {"value": "150"},
                                    "serviceId": "147",
                                }
                            ],
                        },
                    },
                    {
                        "entity": "offer",
                        "wareId": "DsbsUnderTheOrder____g",
                        "delivery": {
                            "options": [
                                {
                                    "dayFrom": Absent(),
                                    "dayTo": Absent(),
                                    "partnerType": "regular",
                                    "price": {"value": "150"},
                                    "serviceId": "147",
                                }
                            ],
                        },
                    },
                ]
            },
            allow_different_len=False,
        )

    def test_dsbs_delivery_filter(self):
        template_fesh_request = 'place=prime&fesh={fesh}&rids=213'
        template_fesh_request_with_flag = template_fesh_request + '&rgb={rgb}'

        # один оффер с доставкой сегодня
        filter_value_today = {"found": 1, "value": "0"}
        # офферы с доставкой сегодня + завтра
        filter_value_tommorow = {"found": 2, "value": "1"}
        # офферы c доставкой до 5 дней
        filter_value_five_day = {"found": 4, "value": "5"}

        # Проверяем, что на белом Маркете всегда есть значения фильтра 0, 1, 5
        request = template_fesh_request_with_flag.format(fesh=_Shops.shop_for_delivery_filter_tests.fesh, rgb='white')
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "id": "delivery-interval",
                "name": "Срок доставки",
                "values": [filter_value_today, filter_value_tommorow, filter_value_five_day],
            },
        )

        template_fesh_request_with_filter = template_fesh_request + '&delivery_interval={filter_value}'
        request = template_fesh_request_with_filter.format(
            fesh=_Shops.shop_for_delivery_filter_tests.fesh, filter_value=0
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response, {"totalOffers": 1, "results": [{"titles": {"raw": "OFFER WITH DELIVERY ON THE SAME DAY"}}]}
        )

        request = template_fesh_request_with_filter.format(
            fesh=_Shops.shop_for_delivery_filter_tests.fesh, filter_value=1
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "totalOffers": 2,
                "results": [
                    {"titles": {"raw": "OFFER WITH DELIVERY ON THE SAME DAY"}},
                    {"titles": {"raw": "OFFER WITH DELIVERY ON THE NEXT DAY"}},
                ],
            },
        )

        request = template_fesh_request_with_filter.format(
            fesh=_Shops.shop_for_delivery_filter_tests.fesh, filter_value=5
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "totalOffers": 4,
                "results": [
                    {"titles": {"raw": "OFFER WITH DELIVERY ON THE SAME DAY"}},
                    {"titles": {"raw": "OFFER WITH DELIVERY ON THE NEXT DAY"}},
                    {"titles": {"raw": "OFFER WITH DELIVERY ON THE SECOND DAY"}},
                    {"titles": {"raw": "OFFER WITH DELIVERY DURING THE WEEK"}},
                ],
            },
        )

    def test_dsbs_tariffs(self):
        """
        Проверяем, что под флагом market_dsbs_tariffs тарифы для дсбс берутся аналогично синим офферам
        и на prime и в actual_delivery
        """

        def pickup_option(tariffs_flag, service_id, price):
            return {
                "pickupOptions": [
                    {
                        "serviceId": service_id,
                        "partnerType": "regular",
                        "price": {"currency": "RUR", "value": "333" if tariffs_flag else price},
                    }
                ]
            }

        # проверка бесплатного самовывоза
        def free_pickup_option(tariffs_flag, price):
            return {
                "pickupOptions": [
                    {
                        "serviceId": 99,
                        "partnerType": "regular",
                        "price": {"currency": "RUR", "value": "0" if tariffs_flag else price},
                    }
                ]
            }

        def courier_option(tariffs_flag):
            return {
                "options": [
                    {
                        "serviceId": "99",
                        "partnerType": "regular",
                        "price": {"currency": "RUR", "value": "222" if tariffs_flag else "100"},
                        "dayFrom": 0,
                        "dayTo": 15,
                    }
                ]
            }

        tariffs_flag = '&rearr-factors=market_dsbs_tariffs={flag}'
        prime_request = 'place=prime&fesh=15&rids=213&pickup-options=raw'
        actual_delivery_request = (
            'place=actual_delivery&offers-list={}&rids=213&regset=1&rearr-factors=market_white_actual_delivery=1'
        )

        for flag in (0, 1):
            # для всех аутлетов ниже используются собственные службы доставки - поэтому ожидаем бесплатный самовывоз
            response = self.report.request_json(prime_request + tariffs_flag.format(flag=flag))
            self.assertFragmentIn(
                response, {"titles": {"raw": "OFFER WITH PICKUP BUCKET"}, "delivery": free_pickup_option(flag, "500")}
            )
            self.assertFragmentIn(
                response,
                {
                    "titles": {"raw": "OFFER WITH SELF PICKUP OPTIONS IN SHOP 15"},
                    "delivery": free_pickup_option(flag, "350"),
                },
            )
            self.assertFragmentIn(
                response,
                {
                    "titles": {"raw": "OFFER WITH FREE SELF PICKUP OPTIONS IN SHOP 15"},
                    "delivery": free_pickup_option(flag, "0"),
                },
            )
            self.assertFragmentIn(
                response,
                {"titles": {"raw": "OFFER WITH THIRD-PARTY SHIPPER"}, "delivery": pickup_option(flag, 147, "350")},
            )
            self.assertFragmentIn(
                response, {"titles": {"raw": "OFFER WITH DELIVERY BUCKET"}, "delivery": courier_option(flag)}
            )
            response = self.report.request_json(
                actual_delivery_request.format('MtR100q_tyqFJLn3KKrggg:1') + tariffs_flag.format(flag=flag)
            )

    def test_dsbs_delivery_in_priority_region(self):
        for use_dsbs_delivery_modifiers_in_priority_regions in [None, False, True]:
            rearr_factors = ""
            if use_dsbs_delivery_modifiers_in_priority_regions is not None:
                rearr_factors = "&rearr-factors=use_dsbs_delivery_modifiers_in_priority_regions={rearr}".format(
                    rearr=use_dsbs_delivery_modifiers_in_priority_regions
                )

            for rids in [75, 76]:
                fesh = rids + 100
                response = self.report.request_json(
                    "place=prime&fesh={fesh}&rids={rids}{rearr}".format(rids=rids, fesh=fesh, rearr=rearr_factors)
                )

                # Тариф курьерки 222 руб. берется из модификатора, размеченного параметром
                # also_use_for_dsbs_in_priority_region=True (в регионе 75)
                # или параметром only_use_for_dsbs_in_priority_region=True (в регионе 76)
                # А тариф 999 руб. прилетает только без флага для проверки воспроизводимости проблемы,
                # которую починили в https://st.yandex-team.ru/MARKETOUT-45383
                price = 222 if use_dsbs_delivery_modifiers_in_priority_regions is not False else 999

                self.assertFragmentIn(
                    response,
                    {
                        "wareId": "DSBS_OFFER_FESH_{fesh}__g".format(fesh=fesh),
                        "delivery": {
                            "options": [
                                {
                                    "price": {"value": str(price)},
                                }
                            ],
                        },
                    },
                )

    def test_cpc_offers_and_unified_tariffs(self):
        '''
        Проверяем, что тарифы для сра не влияют на цену доставки срс офферов
        '''
        unified_flag = '&rearr-factors=market_dsbs_tariffs={}'

        for request in (
            # Фильтр по магазину 17.
            'place=prime&fesh=20&rids=213&rgb=green_with_blue',
            # Конкретный оффер с offerid CPCWithDelivery______g.
            'place=offerinfo&offerid=CPCWithDelivery______g&regset=1&rids=213&rgb=green_with_blue',
        ):
            for flag in (0, 1):

                response = self.report.request_json(request + unified_flag.format(flag))

                self.assertFragmentIn(
                    response,
                    {
                        "titles": {"raw": "OFFER CPC WITH DELIVERY OPTIONS"},
                        "delivery": {
                            "pickupPrice": {
                                "currency": "RUR",
                                "value": "678",  # цена не зависит от тарифов и берется из опций
                            },
                            "options": [
                                {
                                    "price": {"currency": "RUR", "value": "890"},  # и для курьерки тоже
                                    "dayFrom": 3,
                                    "dayTo": 4,
                                    "orderBefore": "14",
                                    "serviceId": "99",
                                }
                            ],
                        },
                    },
                )

    def test_delivery_parter_types(self):
        '''
        Проверяем, что тарифы для сра не влияют на цену доставки срс офферов
        '''
        unified_flag = '&rearr-factors=market_dsbs_tariffs={}'

        for request in (
            # Фильтр по магазину 17.
            'place=prime&fesh=20&rids=213&rgb=green_with_blue',
            # Конкретный оффер с offerid CPCWithDelivery______g.
            'place=offerinfo&offerid=CPCWithDelivery______g&regset=1&rids=213&rgb=green_with_blue',
        ):
            for flag in (0, 1):

                response = self.report.request_json(request + unified_flag.format(flag))

                self.assertFragmentIn(
                    response,
                    {
                        "titles": {"raw": "OFFER CPC WITH DELIVERY OPTIONS"},
                        "delivery": {
                            'deliveryPartnerTypes': ["SHOP"],
                        },
                    },
                )

    def test_free_dsbs_delivery(self):
        '''
        Проверяем, что доставка в пункты самовывоза типа OT_PICKUP  и  OT_RETAIL с собственной службой доставки:
        - бесплатная с включенным rearr-флагом market_free_dsbs_retail_delivery
        - платная с выключенным rearr-флагом market_free_dsbs_retail_delivery
        '''
        request = 'place=prime&hyperid=522&rids=213&rgb=green_with_blue&rearr-factors=market_dsbs_tariffs=1;market_free_dsbs_retail_delivery={}'
        for flag in (0, 1):
            response = self.report.request_json(request.format(flag))

            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "wareId": "PickupTest___________g",
                            "outlet": {
                                "id": "502",
                                "selfDeliveryRule": {
                                    "workInHoliday": True,
                                    "currency": "RUR",
                                    "cost": "0" if flag else "333",
                                },
                            },
                        },
                        {
                            "wareId": "PickupTest2__________g",
                            "outlet": {"id": "505", "selfDeliveryRule": {"currency": "RUR", "cost": "333"}},
                        },
                        {
                            "wareId": "PickupTest3__________g",
                            "outlet": {
                                "id": "504",
                                "selfDeliveryRule": {"currency": "RUR", "cost": "0" if flag else "333"},
                            },
                        },
                        {
                            "wareId": "RetailTest___________g",
                            "outlet": {
                                "id": "501",
                                "selfDeliveryRule": {
                                    "workInHoliday": True,
                                    "currency": "RUR",
                                    "cost": "0" if flag else "333",
                                },
                            },
                        },
                    ]
                },
            )
        '''
        Проверяем бесплатный самовывоз на карточке товара
        '''
        request = 'place=offerinfo&rids=213&rgb=green_with_blue&offerid=PlusTest_____________g&show-urls=1&regset=2'
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "wareId": "PlusTest_____________g",
                        "delivery": {"pickupPrice": {"currency": "RUR", "value": "0"}, "betterWithPlus": False},
                    }
                ]
            },
        )

    def test_cash_on_delivery_payment_method(self):
        '''
        Проверяем, что при выключенном флаге add_cash_on_delivery_payment_for_dsbs_in_local_region
        для DSBS оффера не добавляется метод оплаты CASH_ON_DELIVERY
        '''

        # флаг включен по дефолту
        response = self.report.request_json('place=prime&fesh=30&rids=213')
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "titles": {"raw": "DSBS OFFER IN SHOP 30"},
                "delivery": {
                    "options": [
                        {
                            "paymentMethods": ["CASH_ON_DELIVERY"],
                        },
                    ],
                },
            },
        )

        response = self.report.request_json(
            'place=prime&fesh=30&rids=213' + '&rearr-factors=add_cash_on_delivery_payment_for_dsbs_in_local_region=0'
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "titles": {"raw": "DSBS OFFER IN SHOP 30"},
                "delivery": {
                    "options": [
                        {
                            "paymentMethods": Absent(),
                        },
                    ],
                },
            },
        )

    def test_multiple_different_pickup_options(self):
        request = (
            'place=actual_delivery'
            '&offers-list={}&rids=213'
            '&regset=1'
            '&pickup-options=grouped'
            '&rearr-factors=market_white_actual_delivery=1'
        )
        response = self.report.request_json(
            request.format(_Offers.offer_with_multiple_warehouses_with_different_prices.waremd5 + ":1")
        )
        self.assertFragmentIn(
            response,
            {
                "delivery": {
                    "pickupOptions": [
                        {
                            "serviceId": 99,
                            "partnerType": "regular",
                            "price": {"currency": "RUR", "value": "0"},
                        },
                        {
                            "serviceId": 147,
                            "partnerType": "regular",
                            "price": {"currency": "RUR", "value": "333"},
                        },
                    ],
                },
            },
        )

        request = (
            'place=productoffers'
            '&rids=213'
            '&hyperid={}'
            '&regset=2'
            '&pickup-options=grouped'
            '&rearr-factors=market_white_actual_delivery=1'
        )
        response = self.report.request_json(
            request.format(_Offers.offer_with_multiple_warehouses_with_different_prices.hyperid)
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "wareId": _Offers.offer_with_multiple_warehouses_with_different_prices.waremd5,
                "delivery": {
                    "pickupOptions": [
                        {
                            "serviceId": 99,
                            "partnerType": "regular",
                            "price": {"currency": "RUR", "value": "0"},
                        },
                        {
                            "serviceId": 147,
                            "partnerType": "regular",
                            "price": {"currency": "RUR", "value": "333"},
                        },
                    ],
                },
            },
        )

    def test_payment_method_restrictions(self):
        """
        Проверяем, что при задании флага market_only_prepayment_on_product_card, у всех белых офферов на выдаче пропадает оплата при получении
        (и картой и наличкой, остается только предоплата)
        """
        PREPAYMENT = ["YANDEX"]
        ALL_PAYMENT = ["CASH_ON_DELIVERY", "CARD_ON_DELIVERY"] + PREPAYMENT
        prepayment_flag = "&rearr-factors=market_only_prepayment_on_product_card={}"
        for flag in (0, 1):
            request = "place=prime&pp=18&offerid=MMR100q_tyqFJLn3KKrggg&rids=213" + prepayment_flag.format(flag)
            self.assertFragmentIn(
                self.report.request_json(request),
                {
                    "search": {
                        "total": 1,
                        "results": [
                            {
                                "entity": "offer",
                                "wareId": "MMR100q_tyqFJLn3KKrggg",
                                "delivery": {
                                    "options": [
                                        {
                                            "paymentMethods": PREPAYMENT if flag else ALL_PAYMENT,
                                        }
                                    ]
                                },
                                "payments": {
                                    "deliveryCard": not flag,
                                    "deliveryCash": not flag,
                                    "prepaymentCard": True,
                                    "prepaymentOther": False,
                                },
                            }
                        ],
                    },
                    # проверяем, что фильтры соответствуют выдаче
                    "filters": [
                        {
                            "id": "payments",
                            "type": "enum",
                            "name": "Способы оплаты",
                            "values": [
                                {"initialFound": 1, "found": 1, "value": "Картой на сайте", "id": "prepayment_card"},
                                {
                                    "initialFound": 0 if flag else 1,
                                    "found": 0 if flag else 1,
                                    "value": "Картой курьеру",
                                    "id": "delivery_card",
                                },
                                {
                                    "initialFound": 0 if flag else 1,
                                    "found": 0 if flag else 1,
                                    "value": "Наличными курьеру",
                                    "id": "delivery_cash",
                                },
                            ],
                        }
                    ],
                },
            )
            # при фильтрации по предоплате, оффер всегда на выдаче
            request_with_filter = request + "&payments=prepayment_card"
            self.assertFragmentIn(
                self.report.request_json(request_with_filter),
                {
                    "search": {
                        "total": 1,
                        "results": [
                            {
                                "entity": "offer",
                                "wareId": "MMR100q_tyqFJLn3KKrggg",
                            }
                        ],
                    }
                },
            )
            # применяем фильтры по оплате при получении картой и наличными - оффер на выдаче только при выключенном флаге
            for payment in ("delivery_cash", "delivery_card"):
                request_with_filter = request + "&payments=" + payment
                self.assertFragmentIn(
                    self.report.request_json(request_with_filter),
                    {
                        "search": {
                            "total": 0 if flag else 1,
                            "totalOffersBeforeFilters": 1,
                            "results": [
                                {
                                    "entity": "offer",
                                    "wareId": "MMR100q_tyqFJLn3KKrggg",
                                }
                            ]
                            if not flag
                            else [],
                        }
                    },
                )

    def test_not_empty_payment_methods(self):
        """
        Проверяем, что при задании флага market_only_prepayment_on_product_card, если для белого оффера не настроена предоплата
        - опции оплаты при получении не отфильтруются
        """
        ALL_POST_PAYMENT = ["CASH_ON_DELIVERY", "CARD_ON_DELIVERY"]
        prepayment_flag = "&rearr-factors=market_only_prepayment_on_product_card={}"
        for flag in (0, 1):
            request = "place=prime&pp=18&offerid=DuE098x_rinQLZn3KKrELw&rids=214" + prepayment_flag.format(flag)
            self.assertFragmentIn(
                self.report.request_json(request),
                {
                    "search": {
                        "total": 1,
                        "results": [
                            {"entity": "regionalDelimiter"},
                            {
                                "entity": "offer",
                                "wareId": "DuE098x_rinQLZn3KKrELw",
                                "delivery": {
                                    "options": [
                                        {
                                            "paymentMethods": ALL_POST_PAYMENT,
                                        }
                                    ]
                                },
                                "payments": {
                                    "deliveryCard": True,
                                    "deliveryCash": True,
                                    "prepaymentCard": False,
                                    "prepaymentOther": False,
                                },
                            },
                        ],
                    },
                    # в фильтрах тоже есть только оба вида пост оплаты
                    "filters": [
                        {
                            "id": "payments",
                            "type": "enum",
                            "name": "Способы оплаты",
                            "values": [
                                {"initialFound": 0, "found": 0, "value": "Картой на сайте", "id": "prepayment_card"},
                                {"initialFound": 1, "found": 1, "value": "Картой курьеру", "id": "delivery_card"},
                                {"initialFound": 1, "found": 1, "value": "Наличными курьеру", "id": "delivery_cash"},
                            ],
                        }
                    ],
                },
            )
            # применяем фильтры по оплате картой и наличными при получении, оффер не отфильтровывается
            for payment in ("delivery_cash", "delivery_card"):
                request_with_filter = request + "&payments=" + payment
                self.assertFragmentIn(
                    self.report.request_json(request_with_filter),
                    {
                        "search": {
                            "total": 1,
                            "results": [
                                {
                                    "entity": "offer",
                                    "wareId": "DuE098x_rinQLZn3KKrELw",
                                }
                            ],
                        }
                    },
                )
            # применяем фильтры по предоплате картой - оффера нет на выдаче
            request_with_filter = request + "&payments=prepayment_card"
            self.assertFragmentIn(
                self.report.request_json(request_with_filter),
                {
                    "search": {
                        "total": 0,
                        "totalOffersBeforeFilters": 1,
                    }
                },
            )

    def test_free_dsbs_delivery_no_flag(self):
        """
        Проверяем ненулевую цену доставки DSBS магазина, указанного в free_dsbs_shops.json
        с отключенным флагом market_enable_free_dsbs_delivery.
        """
        request = (
            'place=prime'
            '&fesh=50'
            '&rids=213'
            '&pickup-options=raw'
            '&rgb=green_with_blue'
            '&rearr-factors=market_enable_free_dsbs_delivery=0'
        )
        response = self.report.request_json(request)

        self.assertFragmentIn(
            response,
            {
                "titles": {"raw": "OFFER WITH FREE DELIVERY UNDER FLAG"},
                "delivery": {
                    "options": [
                        {
                            "serviceId": "147",
                            "partnerType": "regular",
                            "price": {
                                "currency": "RUR",
                                "value": "222",
                            },
                            "dayFrom": 0,
                            "dayTo": 4,
                            "discount": Absent(),
                        }
                    ]
                },
            },
            allow_different_len=False,
        )

    def test_free_dsbs_delivery_with_flag(self):
        """
        Проверяем нулевую цену доставки DSBS магазина, указанного в free_dsbs_shops.json
        с включенным флагом market_enable_free_dsbs_delivery.
        Также должна быть указана причина бесплатной доставки is_free_dsbs.
        """
        request = (
            'place=prime'
            '&fesh=50'
            '&rids=213'
            '&pickup-options=raw'
            '&rgb=green_with_blue'
            '&rearr-factors=market_enable_free_dsbs_delivery=1'
        )
        response = self.report.request_json(request)

        self.assertFragmentIn(
            response,
            {
                "titles": {"raw": "OFFER WITH FREE DELIVERY UNDER FLAG"},
                "delivery": {
                    "options": [
                        {
                            "serviceId": "147",
                            "partnerType": "regular",
                            "price": {
                                "currency": "RUR",
                                "value": "0",
                            },
                            "dayFrom": 0,
                            "dayTo": 4,
                            "discount": {
                                "discountType": "is_free_dsbs",
                            },
                        }
                    ]
                },
            },
            allow_different_len=False,
        )

    def test_free_dsbs_delivery_with_flag_not_in_config(self):
        """
        Проверяем ненулевую цену доставки DSBS магазина, _не_ указанного в free_dsbs_shops.json
        с включенным флагом market_enable_free_dsbs_delivery.
        """
        request = (
            'place=prime'
            '&fesh=30'
            '&rids=213'
            '&pickup-options=raw'
            '&rgb=green_with_blue'
            '&rearr-factors=market_enable_free_dsbs_delivery=1'
        )
        response = self.report.request_json(request)

        self.assertFragmentIn(
            response,
            {
                "titles": {"raw": "DSBS OFFER IN SHOP 30"},
                "delivery": {
                    "options": [
                        {
                            "serviceId": "99",
                            "partnerType": "regular",
                            "price": {
                                "currency": "RUR",
                                "value": "222",
                            },
                            "dayFrom": 0,
                            "dayTo": 2,
                            "discount": Absent(),
                        }
                    ]
                },
            },
            allow_different_len=False,
        )

    def test_dsbs_payment_with_flag(self):
        """
        Проверяем 'обнуление' dsbs payment для посылки DSBS магазина, указанного в free_dsbs_shops.json
        с включенным флагом market_enable_free_dsbs_delivery.
        """
        request = (
            'place=actual_delivery'
            '&offers-list={}:1'
            '&rids=213'
            '&regset=1'
            '&rearr-factors=market_white_actual_delivery=1'
            '&rearr-factors=market_enable_free_dsbs_delivery=1'
            '&debug=1'
        )
        response = self.report.request_json(request.format(_Offers.offer_from_dsbs_shop_2.ware_md5))
        self.assertFragmentIn(response, "Dsbs payment: zero payment due to free_dsbs_shops.json")

    def test_dsbs_payment_no_flag(self):
        """
        Проверяем отсутствие 'обнуления' dsbs payment для посылки DSBS магазина, указанного в free_dsbs_shops.json
        с выключенным флагом market_enable_free_dsbs_delivery.
        """
        request = (
            'place=actual_delivery'
            '&offers-list={}:1'
            '&rids=213'
            '&regset=1'
            '&rearr-factors=market_white_actual_delivery=1'
            '&rearr-factors=market_enable_free_dsbs_delivery=0'
            '&debug=1'
        )
        response = self.report.request_json(request.format(_Offers.offer_from_dsbs_shop_2.ware_md5))
        self.assertFragmentNotIn(response, "Dsbs payment: zero payment due to free_dsbs_shops.json")


if __name__ == '__main__':
    main()
