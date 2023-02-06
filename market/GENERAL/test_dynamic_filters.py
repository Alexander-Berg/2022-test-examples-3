#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import (
    DeliveryBucket,
    DynamicOutlet,
    DynamicPromo,
    DynamicShop,
    DynamicWhiteSupplier,
    Model,
    Offer,
    Outlet,
    PickupBucket,
    PickupOption,
    Promo,
    PromoType,
    Region,
    Shop,
    WhiteSupplier,
    MnPlace,
)


class T(TestCase):
    iphone_all_options = {
        'entity': 'offer',
        'titles': {'raw': 'Apple Iphone'},
        'delivery': {'isAvailable': True, 'hasPickup': True},
    }

    galaxy_all_options = {
        'entity': 'offer',
        'titles': {'raw': 'Samsung Galaxy'},
        'delivery': {'isAvailable': True, 'hasLocalStore': True},
    }

    @classmethod
    def prepare(cls):
        # Regions: 100+
        # Shops: 10..19
        # Outlets: 20..29
        # Hyperid: 30..39

        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.regiontree += [
            Region(rid=100, children=[Region(rid=101)]),
            Region(rid=110, children=[Region(rid=111)]),
        ]

        cls.index.shops += [
            Shop(fesh=10, regions=[100, 110], name='Apple Store', pickup_buckets=[5001]),
            Shop(fesh=11, regions=[100, 110], name='Samsung Store', pickup_buckets=[5002]),
            Shop(fesh=12, regions=[100], name='Google Store', pickup_buckets=[5003]),
        ]

        cls.index.outlets += [
            Outlet(fesh=10, region=100, point_type=Outlet.FOR_PICKUP, point_id=101),
            Outlet(fesh=10, region=101, point_type=Outlet.FOR_PICKUP, point_id=102),
            Outlet(fesh=10, region=110, point_type=Outlet.FOR_PICKUP, point_id=103),
            Outlet(fesh=10, region=111, point_type=Outlet.FOR_PICKUP, point_id=104),
            Outlet(fesh=11, region=100, point_type=Outlet.FOR_STORE, point_id=111),
            Outlet(fesh=11, region=101, point_type=Outlet.FOR_STORE, point_id=112),
            Outlet(fesh=11, region=110, point_type=Outlet.FOR_STORE, point_id=113),
            Outlet(fesh=11, region=111, point_type=Outlet.FOR_STORE, point_id=114),
            Outlet(fesh=12, region=100, point_type=Outlet.FOR_PICKUP, point_id=20),
            Outlet(fesh=12, region=100, point_type=Outlet.FOR_PICKUP, point_id=21),
            Outlet(fesh=12, region=100, point_type=Outlet.FOR_STORE, point_id=22),
            Outlet(fesh=12, region=100, point_type=Outlet.FOR_STORE, point_id=23),
            Outlet(fesh=12, region=100, point_id=24),
            Outlet(fesh=12, region=100, point_id=25),
            Outlet(fesh=12, region=110, point_type=Outlet.FOR_PICKUP, point_id=26),
            Outlet(fesh=12, region=110, point_type=Outlet.FOR_STORE, point_id=27),
            Outlet(fesh=12, region=110, point_id=28),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5001,
                fesh=10,
                carriers=[99],
                options=[
                    PickupOption(outlet_id=101),
                    PickupOption(outlet_id=102),
                    PickupOption(outlet_id=103),
                    PickupOption(outlet_id=104),
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5002,
                fesh=11,
                carriers=[99],
                options=[
                    PickupOption(outlet_id=111),
                    PickupOption(outlet_id=112),
                    PickupOption(outlet_id=113),
                    PickupOption(outlet_id=114),
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5003,
                fesh=12,
                carriers=[99],
                options=[
                    PickupOption(outlet_id=20),
                    PickupOption(outlet_id=21),
                    PickupOption(outlet_id=22),
                    PickupOption(outlet_id=23),
                    PickupOption(outlet_id=24),
                    PickupOption(outlet_id=25),
                    PickupOption(outlet_id=26),
                    PickupOption(outlet_id=27),
                    PickupOption(outlet_id=28),
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        cls.index.offers += [
            Offer(title='Apple Iphone', fesh=10),
            Offer(title='Samsung Galaxy', fesh=11),
            Offer(title='Google Pixel', fesh=12, hyperid=30, waremd5='CCCCCCCCCCCCCCCCCCCCCC'),
        ]

        cls.index.white_suppliers += [
            WhiteSupplier(ogrn="123", jur_name="name1", jur_address="address1"),
            WhiteSupplier(ogrn="124", jur_name="name2", jur_address="address2"),
            WhiteSupplier(ogrn="125", jur_name="name3", jur_address="address3"),
        ]

        cls.dynamic.market_dynamic.disabled_offer_by_supplier_id += [
            DynamicWhiteSupplier("123"),
            DynamicWhiteSupplier("124"),
        ]

    @classmethod
    def prepare_parallel(cls):
        for i in range(1, 9):
            cls.index.shops += [Shop(fesh=100 + i, priority_region=213, name='American Store')]

            cls.index.offers += [
                Offer(title='American Crew Master {}'.format(i), fesh=100 + i, bid=10 * i + 30, ts=100500 + i),
            ]
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 100500 + i).respond(0.1 + 0.01 * i)
            cls.matrixnet.on_place(MnPlace.INCUT_META, 100500 + i).respond(0.1 + 0.01 * i)

    def test_no_dynamic(self):
        for rid in (100, 101, 110, 111):
            response = self.report.request_json('place=prime&text=iphone&rids=' + str(rid))
            self.assertFragmentIn(response, self.iphone_all_options)
            response = self.report.request_json('place=prime&text=galaxy&rids=' + str(rid))
            self.assertFragmentIn(response, self.galaxy_all_options)

    def expect_outlet_count(self, count):
        for place in ('prime', 'productoffers', 'geo', 'defaultoffer'):
            response = self.report.request_json('place={}&hyperid=30&rids=100'.format(place))
            self.assertFragmentIn(response, {"shop": {"outletsCount": count}})

    def test_outlet_count(self):
        self.expect_outlet_count(6)

        self.dynamic.market_dynamic.disabled_outlets += [
            DynamicOutlet(20),
            DynamicOutlet(22),
            DynamicOutlet(25),
        ]

        self.expect_outlet_count(3)

        self.dynamic.market_dynamic.disabled_outlets += [
            DynamicOutlet(21),
        ]

        self.expect_outlet_count(2)

    def test_disable_all_outlets_no_delivery(self):
        self.dynamic.market_dynamic.disabled_outlets += [DynamicOutlet(26), DynamicOutlet(27), DynamicOutlet(28)]

        for place in ('prime', 'productoffers', 'geo', 'defaultoffer'):
            response = self.report.request_json('place={}&hyperid=30&rids=110'.format(place))
            self.assertFragmentIn(response, {"search": {"total": 0}})

    def test_filtered_outlets_in_log(self):
        all_outlets = ','.join(map(str, list(range(20, 29))))
        self.dynamic.market_dynamic.disabled_outlets += [DynamicOutlet(20)]

        for place in ('prime', 'productoffers'):
            response = self.report.request_json(
                'place={}&hyperid=30&rids=100&debug=1&outlets={}'.format(place, all_outlets)
            )
            self.assertFragmentIn(
                response,
                {"logicTrace": [r"\[ME\].*? TraceOutletError\(\): Filtered outlet 20, reason: dynamic restriction"]},
                use_regex=True,
            )

    def expect_outlet_status(self, pickup_available, store_available):
        def xml_bool(value):
            return '"yes"' if value else '"-"'

        for place in ('prime', 'productoffers', 'geo', 'defaultoffer'):
            response = self.report.request_json('place={}&hyperid=30&rids=100'.format(place))
            if place == 'geo' and not pickup_available and not store_available:
                self.assertFragmentIn(response, {"search": {"total": 0}})
            else:
                self.assertFragmentIn(
                    response,
                    {
                        "delivery": {
                            "isAvailable": True,
                            "hasPickup": pickup_available,
                            "hasLocalStore": store_available,
                        }
                    },
                )

    def test_disable_all_pickup_outlets(self):
        self.dynamic.market_dynamic.disabled_outlets += [
            DynamicOutlet(20),
            DynamicOutlet(21),
            DynamicOutlet(24),
            DynamicOutlet(25),
        ]

        self.expect_outlet_status(False, True)

    def test_disable_all_store_outlets(self):
        self.dynamic.market_dynamic.disabled_outlets += [
            DynamicOutlet(22),
            DynamicOutlet(23),
            DynamicOutlet(24),
            DynamicOutlet(25),
        ]

        self.expect_outlet_status(True, False)

    def test_disable_all_outlets(self):
        self.dynamic.market_dynamic.disabled_outlets += [DynamicOutlet(oid) for oid in range(20, 26)]

        self.expect_outlet_status(False, False)

    @classmethod
    def prepare_promo_dynamic_blocking(cls):
        cls.index.offers += [
            Offer(
                title='dynamicblocking',
                hyperid=101,
                waremd5='HaaaNBHs0nTF3o1f_cNbbQ',
                promo=Promo(promo_type=PromoType.SECOND_OFFER_FOR_FIXED_PRICE, key='Qh_2EAlZ5x2rGAZ7Daaalg'),
            ),
        ]

    def test_offer_promo_dynamic_blocking(self):
        """
        Вызываем плейс prime. Отфильтровываем интересующий нас оффер по тексту.
        Устанавливаем фильтр по типу промо-акции.

        Проверяем, что в выдаче содержится данный оффер.

        Затем блокируем промо-акцию, и проверяем, что теперь офферов выдаче нет
        """

        response = self.report.request_json(
            'place=prime&text=dynamicblocking&promo-type={}'.format(PromoType.SECOND_OFFER_FOR_FIXED_PRICE)
        )
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'wareId': 'HaaaNBHs0nTF3o1f_cNbbQ',
            },
        )

        self.dynamic.market_dynamic.disabled_promos += [DynamicPromo('Qh_2EAlZ5x2rGAZ7Daaalg')]

        response = self.report.request_json(
            'place=prime&text=dynamicblocking&promo-type={}'.format(PromoType.SECOND_OFFER_FOR_FIXED_PRICE)
        )
        self.assertFragmentNotIn(
            response,
            {
                'entity': 'offer',
            },
        )

    def test_parallel_dynamic_shop_filter(self):
        """
        Проверяем что динамические фильтры работают для параллельного поиска (офферного колдунщика)

        1) Нет выключенных магазинов - все предложения в выдаче:
            Запрашиваем параллельный поиск.
            Проверяем что предложение магазина 108 присутствует в выдаче.

        2) Выключенный магазины пропадают из выдачи:
            Выключаем магазин 108 через динамические фильтры.
            Запрашиваем параллельный поиск.
            Проверяем что предложение магазина 108 отсутствует в выдаче.
        """
        response = self.report.request_bs('place=parallel&rids=213&text=american+crew&ignore-mn=1')
        self.assertFragmentIn(response, "American Crew Master 8")

        self.dynamic.market_dynamic.disabled_cpc_shops += [DynamicShop(108)]

        response = self.report.request_bs('place=parallel&rids=213&text=american+crew&ignore-mn=1')
        self.assertFragmentNotIn(response, "American Crew Master 8")

    @classmethod
    def prepare_dynamic_filter_option(cls):
        cls.index.shops += [
            Shop(fesh=13, cpa=Shop.CPA_REAL),
        ]

        cls.index.offers += [
            Offer(title='BlockyShopOffer', fesh=13, cpa=Offer.CPA_REAL),
        ]

    def test_dynamic_filter_option(self):
        """
        Проверяем, что заблокированные магазины присутствуют в выдаче
        тогда и только тогда, когда запрос содержит опцию dynamic-filters=no
        """
        response = self.report.request_json('place=prime&text=BlockyShopOffer')
        self.assertFragmentIn(response, {'titles': {'raw': 'BlockyShopOffer'}})

        self.dynamic.market_dynamic.disabled_cpc_shops += [DynamicShop(13)]

        self.dynamic.market_dynamic.disabled_cpa_shops += [DynamicShop(13)]

        response = self.report.request_json('place=prime&text=BlockyShopOffer&dynamic-filters=no')
        self.assertFragmentIn(response, {'titles': {'raw': 'BlockyShopOffer'}})

        response = self.report.request_json('place=prime&text=BlockyShopOffer')
        self.assertFragmentNotIn(response, {'titles': {'raw': 'BlockyShopOffer'}})

    @classmethod
    def prepare_cpa_cpc_filters(cls):
        cls.index.shops += [
            Shop(fesh=1621801, name='CPA Real CPC Real', cpa=Shop.CPA_REAL, cpc=Shop.CPC_REAL, priority_region=213),
            Shop(fesh=1621802, name='CPA Real CPC Sbx', cpa=Shop.CPA_REAL, cpc=Shop.CPC_SANDBOX, priority_region=213),
            Shop(fesh=1621803, name='CPA Real CPC No', cpa=Shop.CPA_REAL, cpc=Shop.CPC_NO, priority_region=213),
            Shop(fesh=1621807, name='CPA No CPC Real', cpa=Shop.CPA_NO, cpc=Shop.CPC_REAL, priority_region=213),
            Shop(fesh=1621808, name='CPA No CPC Sbx', cpa=Shop.CPA_NO, cpc=Shop.CPC_SANDBOX, priority_region=213),
            Shop(fesh=1621809, name='CPA No CPC No', cpa=Shop.CPA_NO, cpc=Shop.CPC_NO, priority_region=213),
        ]

        for seq in [1, 2, 3, 7, 8, 9]:
            cls.index.offers += [
                Offer(fesh=1621800 + seq, title="CPA Real CPC Real offer", cpa=Offer.CPA_REAL, override_cpa_check=True),
                Offer(
                    fesh=1621800 + seq,
                    title="CPA Real CPC No offer",
                    cpa=Offer.CPA_REAL,
                    has_url=False,
                    override_cpa_check=True,
                ),
                Offer(fesh=1621800 + seq, title="CPA No CPC Real offer", cpa=Offer.CPA_NO, override_cpa_check=True),
                Offer(
                    fesh=1621800 + seq,
                    title="CPA No CPC No offer",
                    cpa=Offer.CPA_NO,
                    has_url=False,
                    override_cpa_check=True,
                ),
            ]

    def test_cpa_cpc_offers_present(self):
        """Что тестируем: в магазинах с CPA_REAL или CPC_REAL
        без динамических отключений на выдаче есть офферы
        """
        for shop_id in [1621801, 1621802, 1621803, 1621807]:
            response = self.report.request_json('place=prime&rids=213&fesh={}'.format(shop_id))
            self.assertFragmentIn(response, {"results": [{"entity": "offer", "shop": {"id": shop_id}}]})

    def test_cpc_real_cpa_real_dynamic(self):
        """Что тестируем: в магазине с CPA_REAL и CPC_REAL
        при отключении CPA CPA-офферы исчезают из выдачи, при отключении
        CPC CPC-офферы из выдачи.
        При отключении и CPC и CPA на выдаче нет офферов, но
        параметр cpa=-no возвращает офферы CPA_REAL на выдачу в этом случае
        """
        self.dynamic.market_dynamic.disabled_cpc_shops += [DynamicShop(1621801)]
        # Запрос без CPC - должны остаться только CPA_REAL офферы
        response = self.report.request_json('place=prime&rids=213&fesh=1621801&show-urls=external,cpa')
        self.assertFragmentIn(
            response,
            {"results": [{"titles": {"raw": "CPA Real CPC Real offer"}}, {"titles": {"raw": "CPA Real CPC No offer"}}]},
            allow_different_len=False,
        )
        self.dynamic.market_dynamic.disabled_cpc_shops.clear()
        self.dynamic.market_dynamic.disabled_cpa_shops += [DynamicShop(1621801)]
        # Запрос без CPA - должны остаться только CPС_REAL офферы
        response = self.report.request_json('place=prime&rids=213&fesh=1621801&show-urls=external,cpa&debug=1')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"titles": {"raw": "CPA No CPC Real offer"}},
                ]
            },
            allow_different_len=False,
        )
        # Отображение пессемизированных офферов с CPA до CPC запрещено
        self.assertFragmentIn(response, {"debug": {"brief": {"filters": {"HIDE_CPA_PESSIMIZATION_BAD_CPA_SHOP": 1}}}})

        self.dynamic.market_dynamic.disabled_cpc_shops += [DynamicShop(1621801)]

        # Запрос без CPA и без CPC - не должно быть офферов на выдаче
        response = self.report.request_json('place=prime&rids=213&fesh=1621801&show-urls=external,cpa')
        self.assertFragmentNotIn(response, {"entity": "offer"})

        # Запрос без CPA и без CPC, но с cpa=-no.
        # Должны быть все CPA_REAL-офферы магазина
        response = self.report.request_json('place=prime&rids=213&fesh=1621801&cpa=-no&show-urls=external,cpa')
        self.assertFragmentIn(
            response,
            {"results": [{"titles": {"raw": "CPA Real CPC Real offer"}}, {"titles": {"raw": "CPA Real CPC No offer"}}]},
            allow_different_len=False,
        )

    def test_cpa_real_dynamic(self):
        """Что тестируем: в магазинах с CPA_REAL и (CPС_SANDBOX / CPС_NO)
        при отключении CPA офферы исчезают из выдачи, а при отключении
        только CPC офферы CPA_REAL остаются на выдаче
        Параметр cpa=-no возвращает офферы CPA_REAL на выдачу
        """
        for shop_id in [1621802, 1621803]:
            self.dynamic.market_dynamic.disabled_cpc_shops += [DynamicShop(shop_id)]

            # Запрос без CPC - должны остаться только CPA_REAL офферы
            response = self.report.request_json('place=prime&rids=213&fesh={}&show-urls=external,cpa'.format(shop_id))
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {"titles": {"raw": "CPA Real CPC Real offer"}},
                        {"titles": {"raw": "CPA Real CPC No offer"}},
                    ]
                },
                allow_different_len=False,
            )

            self.dynamic.market_dynamic.disabled_cpc_shops.clear()

            self.dynamic.market_dynamic.disabled_cpa_shops += [DynamicShop(shop_id)]
            # Запрос без CPA - не должно быть офферов на выдаче (CPС_REAL нет в магазине)
            response = self.report.request_json('place=prime&rids=213&fesh={}&show-urls=external,cpa'.format(shop_id))
            self.assertFragmentNotIn(response, {"entity": "offer"})
            self.dynamic.market_dynamic.disabled_cpc_shops += [DynamicShop(shop_id)]
            # Запрос без CPA и без CPC - не должно быть офферов на выдаче
            response = self.report.request_json('place=prime&rids=213&fesh={}&show-urls=external,cpa'.format(shop_id))
            self.assertFragmentNotIn(response, {"entity": "offer"})
            # Запрос без CPA и без CPC, но с cpa=-no.
            # Должны быть все CPA_REAL-офферы магазина
            response = self.report.request_json(
                'place=prime&rids=213&fesh={}&cpa=-no&show-urls=external,cpa'.format(shop_id)
            )
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {"titles": {"raw": "CPA Real CPC Real offer"}},
                        {"titles": {"raw": "CPA Real CPC No offer"}},
                    ]
                },
                allow_different_len=False,
            )

    def test_cpc_real_dynamic(self):
        """Что тестируем: в магазинах с CPC_REAL и CPA_NO
        при отключении CPC офферы исчезают из выдачи, а при отключении
        только CPA офферы CPC_REAL остаются на выдаче
        """
        shop_id = 1621807
        self.dynamic.market_dynamic.disabled_cpc_shops += [DynamicShop(shop_id)]

        # Запрос без CPC - не должно быть офферов на выдаче (CPA_REAL нет в магазине)
        response = self.report.request_json('place=prime&rids=213&fesh={}&show-urls=external,cpa'.format(shop_id))
        self.assertFragmentNotIn(response, {"entity": "offer"})

        self.dynamic.market_dynamic.disabled_cpc_shops.clear()

        self.dynamic.market_dynamic.disabled_cpa_shops += [DynamicShop(shop_id)]

        # Запрос без CPA - должны остаться только CPС_REAL офферы
        response = self.report.request_json(
            'place=prime&rids=213&fesh={}&show-urls=external,cpa&debug=1'.format(shop_id)
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"titles": {"raw": "CPA No CPC Real offer"}},
                ]
            },
            allow_different_len=False,
        )
        # Отображение пессемизированных офферов с CPA до CPC запрещено
        self.assertFragmentIn(response, {"debug": {"brief": {"filters": {"HIDE_CPA_PESSIMIZATION_BAD_CPA_SHOP": 1}}}})

        self.dynamic.market_dynamic.disabled_cpc_shops += [DynamicShop(shop_id)]

        # Запрос без CPA и без CPC - не должно быть офферов на выдаче
        response = self.report.request_json('place=prime&rids=213&fesh={}&show-urls=external,cpa'.format(shop_id))
        self.assertFragmentNotIn(response, {"entity": "offer"})

    def test_cpa_cpc_offers_absent(self):
        """Что тестируем: в обычных запросах не приходят офферы магазинов,
        у которых нет ни CPC-программы, ни CPA-программы в статусе REAL
        При отключении динамиком офферов также нет
        """
        for shop_id in [1621808, 1621809]:
            response = self.report.request_json('place=prime&rids=213&fesh={}&show-urls=external,cpa'.format(shop_id))
            self.assertFragmentNotIn(response, {"results": [{"entity": "offer"}]})

            self.dynamic.market_dynamic.disabled_cpc_shops += [DynamicShop(shop_id)]

            response = self.report.request_json('place=prime&rids=213&fesh={}&show-urls=external,cpa'.format(shop_id))
            self.assertFragmentNotIn(response, {"results": [{"entity": "offer"}]})

            response = self.report.request_json('place=prime&rids=213&fesh={}&show-urls=external,cpa'.format(shop_id))
            self.assertFragmentNotIn(response, {"results": [{"entity": "offer"}]})

    @classmethod
    def prepare_offer_with_supplier(cls):
        cls.index.offers += [
            Offer(title="offer_by_supplier_id 1 blocked", mp_supl_ogrn="123", fesh=32),
            Offer(title="offer_by_supplier_id 2 blocked", mp_supl_ogrn="124", fesh=32),
            Offer(title="offer_by_supplier_id 3 not blocked", mp_supl_ogrn="125", fesh=32),
            Offer(title="offer_by_supplier_id 4 without id", fesh=32),
        ]

    def test_offer_with_supplier(self):
        """ """
        response = self.report.request_json('place=prime&text=offer_by_supplier_id&numdoc=48')
        self.assertFragmentIn(
            response,
            [
                {"titles": {"raw": "offer_by_supplier_id 3 not blocked"}},
                {"titles": {"raw": "offer_by_supplier_id 4 without id"}},
            ],
        )
        self.assertFragmentNotIn(
            response,
            [
                {"titles": {"raw": "offer_by_supplier_id 1 blocked"}},
                {"titles": {"raw": "offer_by_supplier_id 2 blocked"}},
            ],
        )

    @classmethod
    def prepare_productoffer_with_supplier(cls):
        cls.index.models += [
            Model(hid=2222, hyperid=123, title='Model test'),
        ]
        cls.index.offers += [
            Offer(mp_supl_ogrn="123", fesh=10, title="offer with supplier_id 1", price=100, hyperid=123),
            Offer(mp_supl_ogrn="124", fesh=11, title="offer with supplier id 2", price=101, hyperid=123),
            Offer(mp_supl_ogrn="125", fesh=12, title="offer with supplier id 3", price=200, hyperid=123),
            Offer(fesh=12, title="offer without supplier id 4", price=200, hyperid=123),
        ]

    def test_productoffer_with_supplier(self):
        """ """
        response = self.report.request_json('place=productoffers&hyperid=123&rids=101')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "titles": {"raw": "offer with supplier id 3"},
                    },
                    {
                        "entity": "offer",
                        "titles": {"raw": "offer without supplier id 4"},
                    },
                ]
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "titles": {"raw": "offer with supplier id 1"},
                    },
                    {
                        "entity": "offer",
                        "titles": {"raw": "offer with supplier id 2"},
                    },
                ]
            },
        )


if __name__ == '__main__':
    main()
