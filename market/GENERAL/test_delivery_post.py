#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BlueOffer,
    Currency,
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryCalendar,
    DeliveryServiceRegionToRegionInfo,
    DynamicDaysSet,
    DynamicDeliveryServiceInfo,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseInfo,
    ExchangeRate,
    GpsCoord,
    MarketSku,
    Offer,
    Outlet,
    PickupBucket,
    PickupOption,
    Region,
    Shop,
    SortingCenterReference,
)
from core.testcase import TestCase, main
from core.types.delivery import BlueDeliveryTariff
from core.types.offer import OfferDimensions

free_delivery_threshold_in_RUR = 3500  # порог стоимости корзины для бесплатной доставки на Синем
KZT_RUR_exchange_rate = 0.5

sku12_price3000 = BlueOffer(price=3000, offerid="Shop2_sku12_1", waremd5="Sku12Price3000-m1Goleg", feedid=3)
sku12_price4000 = BlueOffer(price=4000, offerid="Shop2_sku12_2", waremd5="Sku12Price4000-m1Goleg", feedid=3)
sku13_price100 = BlueOffer(
    price=100,
    offerid="Shop2_sku13",
    waremd5="Sku13Price100-vm1Goleg",
    weight=5,
    dimensions=OfferDimensions(length=20, width=30, height=10),
    feedid=3,
)
sku14_price200 = BlueOffer(
    price=200,
    offerid="Shop2_sku14",
    waremd5="Sku14Price200-vm1Goleg",
    weight=10,
    dimensions=OfferDimensions(length=40, width=60, height=20),
    feedid=3,
)


class T(TestCase):
    @classmethod
    def prepare_blue_delivery_price(cls):
        '''
        Тарифы доставки синих оферов
        '''
        cls.index.blue_delivery_modifiers.set_default_modifier(
            tariffs=[
                BlueDeliveryTariff(user_price=50, large_size=0, price_to=free_delivery_threshold_in_RUR),
                BlueDeliveryTariff(user_price=0, large_size=0),
                BlueDeliveryTariff(user_price=400, large_size=1),
            ],
            large_size_weight=205,
        )
        cls.index.blue_delivery_modifiers.add_modifier(
            tariffs=[
                BlueDeliveryTariff(user_price=50, large_size=0, price_to=free_delivery_threshold_in_RUR),
                BlueDeliveryTariff(user_price=0, large_size=0),
                BlueDeliveryTariff(user_price=400, large_size=1),
            ],
            regions=[2],
            large_size_weight=205,
        )

    @classmethod
    def prepare(cls):
        cls.settings.enable_testing_features = False
        # НЕ делайте так в новых тестах!
        # Походов в КД на проде уже нет, пожалуйста, проверяйте новую функциональность, создавая доставку через комбинатор
        cls.settings.default_search_experiment_flags += ['force_disable_delivery_calculator_requests=0']
        cls.settings.blue_market_free_delivery_threshold = free_delivery_threshold_in_RUR
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.regiontree += [
            Region(
                rid=3,
                name='Центральный федеральный округ',
                region_type=Region.FEDERAL_DISTRICT,
                children=[
                    Region(
                        rid=1,
                        name='Московская область',
                        region_type=Region.FEDERATIVE_SUBJECT,
                        children=[
                            Region(rid=213, name='Москва'),
                        ],
                    ),
                ],
            ),
            Region(
                rid=17,
                name='Северо-западный федеральный округ',
                region_type=Region.FEDERAL_DISTRICT,
                children=[
                    Region(
                        rid=10174,
                        name='Санкт-Петербург и Ленинградская область',
                        region_type=Region.FEDERATIVE_SUBJECT,
                        children=[
                            Region(rid=2, name='Санкт-Петербург'),
                        ],
                    ),
                ],
            ),
        ]

        cls.index.shops += [
            Shop(
                fesh=1,
                datafeed_id=1,
                priority_region=213,
                regions=[213],
                name='Московская пепячечная "Доставляем"',
                delivery_service_outlets=[2001, 2002, 2003, 2011],
            ),
            Shop(
                fesh=2,
                datafeed_id=2,
                warehouse_id=1111,
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                delivery_service_outlets=[12003, 12011],
            ),
            Shop(
                fesh=3,
                datafeed_id=3,
                priority_region=213,
                name='blue_shop_1',
                currency=Currency.RUR,
                blue='REAL',
                warehouse_id=145,
            ),
        ]

        cls.index.shipment_service_calendars += [
            DeliveryCalendar(
                fesh=2, calendar_id=1111, date_switch_hour=20, holidays=[0, 1, 2, 3, 4, 5, 6], is_sorting_center=True
            ),
            DeliveryCalendar(
                fesh=2, calendar_id=1102, sc_references=[SortingCenterReference(sc_id=1111, duration=0, default=True)]
            ),
            DeliveryCalendar(fesh=1, calendar_id=101, date_switch_hour=20, holidays=[0, 1, 2, 3]),
            DeliveryCalendar(fesh=1, calendar_id=102, date_switch_hour=21, holidays=[4, 5, 6]),
        ]

        cls.settings.lms_autogenerate = False
        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=145, home_region=2, holidays_days_set_key=2),
            DynamicDeliveryServiceInfo(
                id=1102,
                rating=2,
                region_to_region_info=[
                    DeliveryServiceRegionToRegionInfo(region_from=1, region_to=213, days_key=1),
                    DeliveryServiceRegionToRegionInfo(region_from=1, region_to=2, days_key=2),
                ],
            ),
            DynamicDeliveryServiceInfo(
                id=101,
                rating=2,
                region_to_region_info=[DeliveryServiceRegionToRegionInfo(region_from=1, region_to=213, days_key=3)],
            ),
            DynamicDeliveryServiceInfo(
                id=102,
                rating=2,
                region_to_region_info=[
                    DeliveryServiceRegionToRegionInfo(region_from=1, region_to=213, days_key=1),
                    DeliveryServiceRegionToRegionInfo(region_from=1, region_to=2, days_key=2),
                ],
            ),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=145,
                delivery_service_id=1102,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=23, region_to=225)],
            ),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=145,
                delivery_service_id=101,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=23, region_to=225)],
            ),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=145,
                delivery_service_id=102,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=23, region_to=225)],
            ),
            DynamicDaysSet(key=1, days=[2, 3]),
            DynamicDaysSet(key=2, days=[15, 16]),
            DynamicDaysSet(key=3, days=[1, 10]),
        ]

        cls.index.outlets += [
            Outlet(
                point_id=2001,
                delivery_service_id=101,
                region=213,
                point_type=Outlet.FOR_POST,
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(37.7, 55.7),
            ),
            Outlet(
                point_id=2002,
                delivery_service_id=102,
                region=213,
                point_type=Outlet.FOR_POST,
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(37.7, 55.8),
            ),
            Outlet(
                point_id=2003,
                delivery_service_id=102,
                region=213,
                point_type=Outlet.FOR_POST,
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(37.7, 55.9),
            ),
            Outlet(
                point_id=2011,
                delivery_service_id=102,
                region=2,
                point_type=Outlet.FOR_POST,
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(67.7, 55.7),
            ),
            Outlet(
                point_id=12003,
                delivery_service_id=1102,
                region=213,
                point_type=Outlet.FOR_POST,
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(37.7, 55.9),
            ),
            Outlet(
                point_id=12011,
                delivery_service_id=1102,
                region=2,
                point_type=Outlet.FOR_POST,
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(67.7, 55.7),
            ),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=0,
                dc_bucket_id=5001,
                fesh=1,
                carriers=[101],
                options=[PickupOption(outlet_id=2001, day_from=1, day_to=2, price=5)],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            PickupBucket(
                bucket_id=1,
                dc_bucket_id=5002,
                fesh=1,
                carriers=[102],
                options=[
                    PickupOption(outlet_id=2002, day_from=3, day_to=3, price=20),
                    PickupOption(outlet_id=2003, day_from=5, day_to=5, price=30),
                    PickupOption(outlet_id=2011, day_from=14, day_to=15, price=100500),
                ],  # Цена в бакете больше не влияет на цену доставки для синих оферов
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            PickupBucket(
                bucket_id=11,
                dc_bucket_id=15002,
                fesh=2,
                carriers=[1102],
                options=[
                    PickupOption(outlet_id=12003, day_from=5, day_to=5, price=30),
                    PickupOption(outlet_id=12011, day_from=14, day_to=15, price=100500),
                ],  # Цена в бакете больше не влияет на цену доставки для синих оферов
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
        ]

        cls.index.offers += [
            Offer(
                hyperid=1,
                fesh=1,
                feedid=1,
                price=100,
                waremd5='22222222222222gggggggg',
                weight=5,
                dimensions=OfferDimensions(length=20, width=30, height=10),
                post_buckets=[0],
            ),
            Offer(
                hyperid=2,
                fesh=1,
                feedid=1,
                price=100,
                waremd5='222222222222_3gggggggg',
                weight=5,
                dimensions=OfferDimensions(length=20, width=30, height=10),
                post_buckets=[0, 1],
            ),
            Offer(
                hyperid=3,
                fesh=1,
                feedid=1,
                price=100,
                waremd5='222222222222_4gggggggg',
                weight=5,
                dimensions=OfferDimensions(length=20, width=30, height=10),
                post_buckets=[1],
            ),
            Offer(
                hyperid=3,
                fesh=1,
                feedid=1,
                price=100,
                waremd5='222222222222_5gggggggg',
                weight=5,
                dimensions=OfferDimensions(length=20, width=30, height=10),
                post_buckets=[],
            ),
            Offer(hyperid=4, fesh=2, feedid=2, price=100, waremd5='WhiteOfferPrice4000-mg', post_buckets=[1, 11]),
        ]

        cls.index.mskus += [
            MarketSku(
                title="blue offer sku12",
                hyperid=1,
                sku="12",
                post_buckets=[1, 11],
                blue_offers=[
                    sku12_price3000,  # офер ценой меньше порога бесплатной доставки
                    sku12_price4000,  # офер ценой больше порога бесплатной доставки
                ],
            ),
            MarketSku(
                title="blue offer sku13",
                hyperid=1,
                sku="13",
                post_buckets=[1, 11],
                blue_offers=[sku13_price100],  # дешевый офер - для наполнения корзины до порога бесплатной доставки
            ),
            MarketSku(
                title="blue offer sku14",
                hyperid=1,
                sku="14",
                post_buckets=[1, 11],
                blue_offers=[sku14_price200],  # дешевый офер - для наполнения корзины до порога бесплатной доставки
            ),
        ]

        # Весо-габариты различных комбинаций оферов для тестирования place=actual_delivery
        cls.delivery_calc.on_request_offer_buckets(weight=15, length=64, width=32, height=43).respond(
            [], [], [5002, 15002]
        )
        cls.delivery_calc.on_request_offer_buckets(weight=150, length=60, width=300, height=40).respond(
            [], [], [5002, 15002]
        )
        cls.delivery_calc.on_request_offer_buckets(weight=175, length=96, width=96, height=106).respond(
            [], [], [5002, 15002]
        )
        cls.delivery_calc.on_request_offer_buckets(weight=200, length=117, width=106, height=128).respond(
            [], [], [5002, 15002]
        )
        cls.delivery_calc.on_request_offer_buckets(weight=80.9297862, width=59.5, height=49.5, length=89.5).respond(
            [], [], [5002, 15002]
        )
        cls.delivery_calc.on_request_offer_buckets(weight=67.5897115, width=59.5, height=49.5, length=89.5).respond(
            [], [], [5002, 15002]
        )
        cls.delivery_calc.on_request_offer_buckets(
            weight=13.1404276, width=34.979648, height=34.979648, length=34.979648
        ).respond([], [], [5002, 15002])
        cls.delivery_calc.on_request_offer_buckets(
            weight=64.8205769, width=63.230386, height=63.230386, length=63.230386
        ).respond([], [], [5002, 15002])

        cls.index.currencies = [
            Currency("KZT", exchange_rates=[ExchangeRate(fr=Currency.RUR, rate=KZT_RUR_exchange_rate)]),
        ]

        cls.settings.loyalty_enabled = True

    def test_post_stat_format(self):
        '''
        Что проверяем: формат выдачи статистики доставки почтой для оферов
        '''
        response = self.report.request_json('place=prime&rids=213&offerid=22222222222222gggggggg')
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "delivery": {
                    "hasPost": True,
                    "postStats": {
                        "minDays": 5,
                        "maxDays": 6,
                        "minPrice": {
                            "value": "5",
                        },
                        "maxPrice": {
                            "value": "5",
                        },
                    },
                },
            },
            allow_different_len=False,
        )

    def test_post_stat_multi_buckets(self):
        '''
        Что проверяем: рассчет сроков доставки для нескольких бакетов
        В 213 регион происходит доставка двумя службами.
        Сроки и цена доставки рассчитывается, как крайние значения для всех опций
        '''
        response = self.report.request_json('place=prime&rids=213&offerid=222222222222_3gggggggg')
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "delivery": {
                    "postStats": {
                        "minDays": 5,
                        "maxDays": 7,
                        "minPrice": {
                            "value": "5",  # Цена доставки бакета 0
                        },
                        "maxPrice": {
                            "value": "30",  # Цена доставки бакета 1
                        },
                    },
                },
            },
            allow_different_len=False,
        )

    def test_post_stat_multi_outlets_in_single_bucket(self):
        '''
        Что проверяем: рассчет статистики в рамках одного бакета, если в нем несколько оутлетов
        Выбран офер, который в 213 регион доставляется только одной службой.
        '''
        response = self.report.request_json('place=prime&rids=213&offerid=222222222222_4gggggggg')
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "delivery": {
                    "postStats": {
                        "minDays": 5,
                        "maxDays": 7,
                        "minPrice": {
                            "value": "20",  # Цена доставки бакета 0
                        },
                        "maxPrice": {
                            "value": "30",  # Цена доставки бакета 1
                        },
                    },
                },
            },
            allow_different_len=False,
        )

    def check_cart_delivery_price(
        self, request, offerWareIds, currency, offers_total_price, regular_post_price, free_delivery_threshold
    ):
        free_delivery = offers_total_price >= free_delivery_threshold

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,  # число deliveryGroups
                    "totalOffers": len(offerWareIds),
                    "offersTotalPrice": {"currency": currency, "value": str(offers_total_price)},
                    "freeDeliveryThreshold": {"currency": currency, "value": str(free_delivery_threshold)},
                    "freeDeliveryRemainder": {
                        "currency": currency,
                        "value": str(0 if free_delivery else free_delivery_threshold - offers_total_price),
                    },
                    "results": [
                        {
                            "entity": "deliveryGroup",
                            "delivery": {
                                "postStats": {
                                    "minPrice": {"value": str(0 if free_delivery else regular_post_price)},
                                    "maxPrice": {"value": str(0 if free_delivery else regular_post_price)},
                                }
                            },
                        }
                    ],
                }
            },
            allow_different_len=False,
        )

    def test_cart_delivery_price(self):
        """Проверяется вычисление общей стоимости заказа, остатка до бесплатной доставки и
        обнуление цены доставки почтой на различных корзинах синих оферов
        Цена доставки для синего офера берется из тарифов синего маркета, а не бакетов
        """
        request = "place=actual_delivery&offers-list=Sku13Price100-vm1Goleg:{},Sku14Price200-vm1Goleg:{}&rids=2&force-use-delivery-calc=1"

        # сумма заказа < free_delivery_threshold
        self.check_cart_delivery_price(
            request.format(1, 1),
            ["Sku13Price100-vm1Goleg", "Sku14Price200-vm1Goleg"],
            "RUR",
            100 + 200,
            50,
            free_delivery_threshold_in_RUR,
        )

        # проверка этого же условия в другой валюте
        self.check_cart_delivery_price(
            request.format(1, 1) + "&currency=KZT",
            ["Sku13Price100-vm1Goleg", "Sku14Price200-vm1Goleg"],
            "KZT",
            int((100 + 200) * KZT_RUR_exchange_rate),
            int(50 * KZT_RUR_exchange_rate),
            int(free_delivery_threshold_in_RUR * KZT_RUR_exchange_rate),
        )

        # сумма заказа == free_delivery_threshold
        self.check_cart_delivery_price(
            request.format(15, 10),
            ["Sku13Price100-vm1Goleg", "Sku14Price200-vm1Goleg"],
            "RUR",
            100 * 15 + 200 * 10,
            50,
            free_delivery_threshold_in_RUR,
        )

        # сумма заказа > free_delivery_threshold
        self.check_cart_delivery_price(
            request.format(10, 15),
            ["Sku13Price100-vm1Goleg", "Sku14Price200-vm1Goleg"],
            "RUR",
            100 * 10 + 200 * 15,
            50,
            free_delivery_threshold_in_RUR,
        )

    def check_offer_delivery_price(
        self, request, is_blue_offer, offerWareId, offer_price, regular_post_price, free_delivery_threshold
    ):
        free_delivery = is_blue_offer and (offer_price >= free_delivery_threshold)

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "wareId": offerWareId,
                "delivery": {
                    "postStats": {
                        "minPrice": {"value": str(0 if free_delivery else regular_post_price)},
                        "maxPrice": {"value": str(0 if free_delivery else regular_post_price)},
                    }
                },
            },
        )

    def test_offer_delivery_price(self):
        """Проверяется, что цена доставки почтой обнуляется для синего оффера,
           если цена оффера больше или равна free_delivery_threshold
        и не обнуляется для белого офера
        Цена доставки для синего офера берется из тарифов синего маркета, а не бакетов
        """
        prime_request = "place=prime&offerid={}&rids=2&allow-collapsing=0&pickup-options=grouped"

        for request in [
            prime_request + "&rgb=blue",
            prime_request + "&rgb=green_with_blue",
            "place=offerinfo&offerid={}&rids=2&show-urls=cpa&regset=1&pickup-options=grouped&rgb=green_with_blue",
            "place=sku_offers&market-sku=12&offerid={}&rids=2&pickup-options=grouped",
        ]:
            self.check_offer_delivery_price(
                request.format("Sku12Price3000-m1Goleg"),
                True,
                "Sku12Price3000-m1Goleg",
                3000,
                50,
                free_delivery_threshold_in_RUR,
            )
            self.check_offer_delivery_price(
                request.format("Sku12Price4000-m1Goleg"),
                True,
                "Sku12Price4000-m1Goleg",
                4000,
                50,
                free_delivery_threshold_in_RUR,
            )

        for request in [
            prime_request,
            prime_request + "&rgb=green_with_blue",
            "place=offerinfo&offerid={}&rids=2&show-urls=cpa&regset=1&pickup-options=grouped&rgb=green_with_blue",
        ]:
            self.check_offer_delivery_price(
                request.format("WhiteOfferPrice4000-mg"),
                False,
                "WhiteOfferPrice4000-mg",
                4000,
                100500,
                free_delivery_threshold_in_RUR,
            )

    def test_only_post_delivery(self):
        '''
        Что проверяем: доставку только почтой России.
        В регион 2 доставляет только одна СД (бакет 1), нет отделений магазина, ПВЗ и курьерки
        '''

        for req in ['place=prime&offerid=222222222222_3gggggggg', 'place=defaultoffer&hyperid=2']:
            response = self.report.request_json(req + '&rids=2&debug=da')
            self.assertFragmentIn(
                response,
                {
                    "entity": "offer",
                    "delivery": {
                        "postStats": {
                            "minDays": 14,  # Исходный срок 14 дней. Успеваем доставить в отделение до выходных
                            "maxDays": 17,  # Исходный срок 15 дней. Попали на выходные 15, 16 день для службы 102 в регионе 2.
                            "minPrice": {
                                "value": "100500",
                            },
                            "maxPrice": {
                                "value": "100500",
                            },
                        },
                    },
                },
            )
            # Проверяем, что доставка только почтой
            self.assertFragmentIn(
                response,
                {
                    "properties": {
                        "WARE_MD5": "222222222222_3gggggggg",
                        "DELIVERY_COURIER": "None",
                        "DELIVERY_DEPOT": "None",
                        "DELIVERY_POST_TERM": "None",
                        "DELIVERY_STORE": "None",
                        "DELIVERY_DOWNLOAD": "None",
                        "DELIVERY_POST": "1",
                    }
                },
            )

    def test_delivery_without_post(self):
        '''
        Что проверяем: отстутствие флага hasPost, если доставка офера почтой в заданный регион не осуществляется
        '''
        response = self.report.request_json('place=prime&rids=213&offerid=222222222222_5gggggggg')
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "wareId": '222222222222_5gggggggg',
                "delivery": {
                    "hasPost": False,
                },
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_experimental_post_delivery(cls):
        cls.index.shops += [
            Shop(fesh=148, priority_region=213),
            Shop(fesh=149, priority_region=213),
        ]
        cls.index.offers += [
            Offer(fesh=148, waremd5='123222222222_3gggggggg'),
            Offer(fesh=149, waremd5='321222222222_3gggggggg'),
        ]

    def test_experimental_post_delivery(self):
        """MARKETOUT-28804"""
        response = self.report.request_json(
            'place=prime&rids=213&offerid=123222222222_3gggggggg&rearr-factors=market_return_post_delivery=1'
        )
        self.assertFragmentIn(
            response,
            {
                "shop": {
                    "hasPostDelivery": True,
                }
            },
        )

        response = self.report.request_json(
            'place=prime&rids=213&offerid=321222222222_3gggggggg&rearr-factors=market_return_post_delivery=1'
        )
        self.assertFragmentNotIn(
            response,
            {
                "shop": {
                    "hasPostDelivery": True,
                }
            },
        )


if __name__ == '__main__':
    main()
