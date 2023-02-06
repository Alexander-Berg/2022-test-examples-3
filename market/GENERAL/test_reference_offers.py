#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    Currency,
    DeliveryBucket,
    DeliveryOption,
    DeliveryServiceRegionToRegionInfo,
    DynamicDaysSet,
    DynamicDeliveryServiceInfo,
    DynamicWarehouseInfo,
    DynamicWarehouseToWarehouseInfo,
    HyperCategory,
    HyperCategoryType,
    Model,
    Offer,
    ReferenceShop,
    Region,
    RegionalDelivery,
    Shop,
    Tax,
    Vat,
    WarehouseWithPriority,
    WarehousesPriorityInRegion,
)
from core.types.autogen import Const
from core.testcase import TestCase, main
from core.types.sku import MarketSku, BlueOffer


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.nordstream_autogenerate = False
        cls.settings.default_search_experiment_flags += ['market_nordstream=0']
        cls.index.hypertree += [
            HyperCategory(hid=100, output_type=HyperCategoryType.GURU),
        ]

        cls.index.shops += [
            Shop(
                # Виртуальный магазин синего маркета
                fesh=1,
                datafeed_id=1,
                name='virtual_shop',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                cpa=Shop.CPA_REAL,
            ),
            Shop(
                fesh=3,
                datafeed_id=3,
                priority_region=2,
                name='blue_shop_1',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                blue='REAL',
                cpa=Shop.CPA_REAL,
            ),
            Shop(fesh=1001, name="Shopchik1"),
            Shop(fesh=1002, name="Shopchik2"),
            Shop(
                fesh=1003,
            ),
        ]
        cls.index.reference_shops += [
            ReferenceShop(hid=Const.ROOT_HID, fesh=1001),
            ReferenceShop(hid=100, fesh=1002),
        ]
        cls.index.models += [
            Model(hyperid=1000, hid=100),
            Model(hyperid=1001, hid=100),
            Model(hyperid=1002, hid=100),
            Model(hyperid=1003, hid=100),
        ]
        cls.index.mskus += [
            MarketSku(
                title="blueoffer1",
                hyperid=1000,
                sku='111',
                waremd5='Sku1-wdDXWsIiLVm1goleg',
                blue_offers=[
                    BlueOffer(
                        price=5,
                        price_old=8,
                        vat=Vat.VAT_10,
                        feedid=3,
                        offerid='blueoffer1',
                        waremd5='Sku1Price5-IiLVm1Goleg',
                    )
                ],
                randx=1,
            ),
        ]
        cls.index.offers += [
            Offer(title='offer1', fesh=1001, hid=100, hyperid=1000, price=10001, sku=111),
            Offer(title='offer2', fesh=1002, hid=100, hyperid=1000, price=10002, sku=111),
            Offer(title='offer3', fesh=1002, hid=100, hyperid=1000, price=52, sku=222),
            Offer(title='offer4', fesh=1002, hid=100, hyperid=1000, price=52),
            Offer(title='offer5', fesh=1003, hid=100, hyperid=1000, price=52, sku=111),
        ]

    def test_reference_offers(self):
        '''
        Запрашиваем плейс reference_shops для market-sku 111

        Проверяем, что прогноз соответствует логике прогнозатора. Точные значения взяты из фактической выдачи, и
        проверяются для того, чтобы зафиксировать результаты работы логики, и видеть влияющие на неё изменения. То
        есть для регрессионного тестирования. Проверки по смыслу делаются в юнит-тестах.
        '''
        response = self.report.request_json('place=reference_offers&market-sku=111')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "titles": {"raw": "offer1"},
                        "shop": {"id": 1001, "name": "Shopchik1"},
                        "prices": {"value": "10001"},
                    },
                    {
                        "titles": {"raw": "offer2"},
                        "shop": {"id": 1002, "name": "Shopchik2"},
                        "prices": {"value": "10002"},
                    },
                ],
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_fair_filter_tests(cls):
        # See MARKETOUT-19345, see unit tests for more cases
        # Blue shop - 2000, reference shops - 2100..2109
        # Models - 2000..2002

        cls.index.shops.append(Shop(fesh=2000, datafeed_id=2000, priority_region=213, blue='REAL'))
        for fesh in range(2100, 2110):
            cls.index.shops.append(Shop(fesh=fesh, datafeed_id=fesh, priority_region=213))
            cls.index.reference_shops.append(ReferenceShop(hid=200, fesh=fesh))

        cls.index.models += [
            Model(hyperid=2000, hid=200),
            Model(hyperid=2001, hid=200),
            Model(hyperid=2002, hid=200),
            Model(hyperid=2003, hid=200),
        ]

        cls.index.mskus += [
            MarketSku(
                hyperid=2000,
                sku=2000,
                blue_offers=[
                    BlueOffer(price=100, vat=Vat.VAT_18, offerid='2000', feedid=2000),
                ],
            ),
            MarketSku(
                hyperid=2001,
                sku=2001,
                blue_offers=[
                    BlueOffer(price=100, vat=Vat.VAT_18, offerid='2001', feedid=2000),
                ],
            ),
            MarketSku(
                hyperid=2002,
                sku=2002,
                blue_offers=[
                    BlueOffer(price=100, vat=Vat.VAT_18, offerid='2002', feedid=2000),
                ],
            ),
            MarketSku(
                hyperid=2003,
                sku=2003,
                blue_offers=[
                    BlueOffer(price=100, vat=Vat.VAT_18, offerid='2003', feedid=2000),
                ],
            ),
        ]

        # for market-sku=2000
        cls.index.offers += [
            Offer(fesh=2100, hyperid=2000, price=110, sku=2000),
            Offer(fesh=2101, hyperid=2000, price=105, sku=2000),
            Offer(fesh=2102, hyperid=2000, price=100, sku=2000),
            Offer(fesh=2103, hyperid=2000, price=99, sku=2000),
            Offer(fesh=2104, hyperid=2000, price=99, sku=2000),
        ]

        # for market-sku=2001
        cls.index.offers += [
            Offer(fesh=2100, hyperid=2001, price=110, sku=2001),
            Offer(fesh=2101, hyperid=2001, price=105, sku=2001),
            Offer(fesh=2102, hyperid=2001, price=100, sku=2001),
            Offer(fesh=2103, hyperid=2001, price=95, sku=2001),  # too cheap
            Offer(fesh=2104, hyperid=2001, price=90, sku=2001),  # too cheap
        ]

        # for market-sku=2002
        cls.index.offers += [
            Offer(fesh=2100, hyperid=2002, price=106, sku=2002),
            Offer(fesh=2101, hyperid=2002, price=105, sku=2002),
            Offer(fesh=2102, hyperid=2002, price=104, sku=2002),
            Offer(fesh=2103, hyperid=2002, price=103, sku=2002),
            Offer(fesh=2104, hyperid=2002, price=102, sku=2002),
            Offer(fesh=2105, hyperid=2002, price=101, sku=2002),
            Offer(fesh=2106, hyperid=2002, price=100, sku=2002),
            Offer(fesh=2107, hyperid=2002, price=99, sku=2002),
            Offer(fesh=2108, hyperid=2002, price=98, sku=2002),  # too cheap
            Offer(fesh=2109, hyperid=2002, price=97, sku=2002),  # too cheap
        ]

        # for market-sku=2003
        cls.index.offers += [
            Offer(fesh=2100, hyperid=2003, price=106, sku=2003),  # discarded
            Offer(fesh=2100, hyperid=2003, price=99, sku=2003),  # kept
            Offer(fesh=2101, hyperid=2003, price=105, sku=2003),
            Offer(fesh=2102, hyperid=2003, price=104, sku=2003),
            Offer(fesh=2103, hyperid=2003, price=103, sku=2003),
            Offer(fesh=2104, hyperid=2003, price=102, sku=2003),
            Offer(fesh=2105, hyperid=2003, price=101, sku=2003),
            Offer(fesh=2106, hyperid=2003, price=100, sku=2003),
            Offer(fesh=2107, hyperid=2003, price=100, sku=2003),
            Offer(fesh=2108, hyperid=2003, price=101, sku=2003),  # discarded
            Offer(fesh=2108, hyperid=2003, price=98, sku=2003),  # too cheap
            Offer(fesh=2109, hyperid=2003, price=97, sku=2003),  # too cheap
        ]

    def test_fair_filter_1(self):
        """Too many cheap offers (40%) - show nothing"""
        response = self.report.request_json('place=reference_offers&market-sku=2000&fair-filter=1')
        self.assertFragmentIn(response, {"results": []}, allow_different_len=False)

    def test_fair_filter_2(self):
        """Discard too cheap offers, show only expensive"""
        response = self.report.request_json('place=reference_offers&market-sku=2001&fair-filter=1')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"shop": {"id": 2100}},
                    {"shop": {"id": 2101}},
                    {"shop": {"id": 2102}},
                ]
            },
            allow_different_len=False,
        )

    def test_fair_filter_3(self):
        """Discard too cheap offers, show 7 expensive and 1 cheap"""
        response = self.report.request_json('place=reference_offers&market-sku=2002&fair-filter=1')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"shop": {"id": 2100}},
                    {"shop": {"id": 2101}},
                    {"shop": {"id": 2102}},
                    {"shop": {"id": 2103}},
                    {"shop": {"id": 2104}},
                    {"shop": {"id": 2105}},
                    {"shop": {"id": 2106}},
                    {"shop": {"id": 2107}},
                ]
            },
            allow_different_len=False,
        )

    def test_fair_filter_4(self):
        """For each shop keep only the best offer"""
        response = self.report.request_json('place=reference_offers&market-sku=2003&fair-filter=1')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"shop": {"id": 2100}, "prices": {"value": "99"}},
                    {"shop": {"id": 2101}},
                    {"shop": {"id": 2102}},
                    {"shop": {"id": 2103}},
                    {"shop": {"id": 2104}},
                    {"shop": {"id": 2105}},
                    {"shop": {"id": 2106}},
                    {"shop": {"id": 2107}},
                ]
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_blue_regions(cls):
        """
        Добавляем два магазина с доставкой в разные регионы для проверки
        фильтрации оффера по регионам при выборе базового оффера для сравнения
        цен с другими магазинами на белом
        """
        goodburg = 522
        newgoodburg = 1225
        goodburg_warehouse = 888
        newgoodburg_warehouse = 777
        goodburg_fesh = 5
        newgoodburg_fesh = 4
        cls.index.regiontree += [
            Region(rid=goodburg, name='Гудбург', region_type=Region.CITY),
            Region(rid=newgoodburg, name='Новый Гудбург', region_type=Region.CITY),
        ]
        cls.index.hypertree += [
            HyperCategory(hid=400, output_type=HyperCategoryType.GURU),
        ]

        cls.index.models += [
            Model(hyperid=4001, hid=400),
        ]

        cls.index.shops += [
            Shop(fesh=3002, priority_region=newgoodburg, name="goodshop"),
            Shop(fesh=3003, priority_region=newgoodburg, name="goodshop2"),
            Shop(fesh=3004, priority_region=newgoodburg, name="goodshop3"),
            Shop(
                fesh=newgoodburg_fesh,
                datafeed_id=newgoodburg_fesh,
                priority_region=newgoodburg,
                regions=[
                    newgoodburg,
                ],
                name='blue_shop_2',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                warehouse_id=newgoodburg_warehouse,
                fulfillment_program=False,
            ),
            Shop(
                fesh=goodburg_fesh,
                datafeed_id=goodburg_fesh,
                regions=[
                    goodburg,
                ],
                priority_region=goodburg,
                name='blue_shop_1',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                warehouse_id=goodburg_warehouse,
                fulfillment_program=False,
            ),
        ]

        cls.index.reference_shops += [
            ReferenceShop(hid=400, fesh=3002),
            ReferenceShop(hid=400, fesh=3003),
            ReferenceShop(hid=400, fesh=3004),
        ]

        cls.index.mskus += [
            MarketSku(
                title="blueoffer2",
                hyperid=4001,
                hid=400,
                sku='444',
                waremd5='Sku4-wdDXWsIiLVm1goleg',
                blue_offers=[
                    BlueOffer(
                        price=125,
                        feedid=newgoodburg_fesh,
                        offerid='blueoffer2_price125',
                        waremd5='Sku4Pric125-ILVm1Goleg',
                        is_fulfillment=False,
                        delivery_buckets=[
                            11,
                        ],
                    ),
                    BlueOffer(
                        price=80,
                        feedid=goodburg_fesh,
                        offerid='blueoffer2_price80',
                        waremd5='Sku4Pric80-IiLVm1Goleg',
                        is_fulfillment=False,
                        delivery_buckets=[
                            12,
                        ],
                    ),
                ],
            ),
        ]

        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=newgoodburg_warehouse, home_region=newgoodburg),
            DynamicWarehouseInfo(id=goodburg_warehouse, home_region=goodburg),
            DynamicWarehouseToWarehouseInfo(warehouse_from=newgoodburg_warehouse, warehouse_to=newgoodburg_warehouse),
            DynamicWarehouseToWarehouseInfo(warehouse_from=goodburg_warehouse, warehouse_to=goodburg_warehouse),
            DynamicDeliveryServiceInfo(
                189,
                "c_189",
                region_to_region_info=[
                    DeliveryServiceRegionToRegionInfo(region_from=newgoodburg, region_to=newgoodburg, days_key=1),
                ],
            ),
            DynamicDeliveryServiceInfo(
                190,
                "c_190",
                region_to_region_info=[
                    DeliveryServiceRegionToRegionInfo(region_from=goodburg, region_to=goodburg, days_key=1),
                ],
            ),
            DynamicDaysSet(key=1, days=[]),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=11,
                fesh=1,
                carriers=[189],
                regional_options=[
                    RegionalDelivery(rid=newgoodburg, options=[DeliveryOption(price=5, day_from=3, day_to=5)])
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=12,
                fesh=1,
                carriers=[190],
                regional_options=[
                    RegionalDelivery(rid=goodburg, options=[DeliveryOption(price=5, day_from=3, day_to=5)])
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        cls.index.warehouse_priorities += [
            WarehousesPriorityInRegion(
                regions=[newgoodburg], warehouse_with_priority=[WarehouseWithPriority(newgoodburg_warehouse, 100)]
            ),
            WarehousesPriorityInRegion(
                regions=[goodburg], warehouse_with_priority=[WarehouseWithPriority(goodburg_warehouse, 100)]
            ),
        ]

        # добавляем офферы так, чтобы часть из них была дороже оффера из 1225 региона,
        # при этом должны присутствовать офферы дороже "дешёвого" синего оффера, но дешевле "дорогого"
        # для того, чтобы по наличию этих офферов в выдаче можно было понять какой синий оффер был выбран для сравнения
        cls.index.offers += [
            Offer(
                title='offer41', fesh=3002, hid=400, hyperid=4001, sku=444, price=100
            ),  # оффер дороже дешёвого синего, но дешевле дорогого синего
            Offer(title='offer42', fesh=3003, hid=400, hyperid=4001, sku=444, price=150),
            Offer(title='offer43', fesh=3004, hid=400, hyperid=4001, sku=444, price=151),
        ]

    @classmethod
    def prepare_rids(cls):
        cls.index.regiontree += [
            Region(rid=213, name='Москва'),
        ]

        cls.index.hypertree += [
            HyperCategory(hid=300, output_type=HyperCategoryType.GURU),
        ]

        cls.index.shops += [
            Shop(fesh=3001, priority_region=213, name="Shopchik1"),
        ]

        cls.index.reference_shops += [
            ReferenceShop(hid=Const.ROOT_HID, fesh=3001),
        ]

        cls.index.models += [
            Model(hyperid=3000, hid=300),
        ]

        cls.index.offers += [
            Offer(title='offer31', fesh=3001, hid=300, sku=3100),
            Offer(title='offer32', fesh=3001, hid=300, sku=3100, has_delivery_options=False),
        ]

    def test_rids_moscow(self):
        '''
        Запрашиваем плейс reference_shops с фильтром по региону Москва

        Проверяем, что в выдаче присутствуют те и только те офферы, которые могут быть доставлены в Москву,
        либо могут быть забраны в московском магазине.
        '''
        response = self.report.request_json('place=reference_offers&market-sku=3100&rids=213')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"titles": {"raw": "offer31"}},
                ],
            },
            allow_different_len=False,
        )

    def test_blue_rids_offer_selection(self):
        '''
        Запрашиваем плейс reference_offers с фильтром по региону 1225, когда для данной ску у нас есть более выгодный
        оффер не доступный в 1225.

        Проверяем, что сравнение происходит именно по офферу доступному в нужном регионе.
        '''
        response = self.report.request_json('place=reference_offers&market-sku=444&rids=1225&fair-filter=1')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"titles": {"raw": "offer42"}},
                    {"titles": {"raw": "offer43"}},
                ],
            },
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()
