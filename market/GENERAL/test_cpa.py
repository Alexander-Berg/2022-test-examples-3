#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BookingAvailability,
    ClickType,
    CpaCategory,
    CpaCategoryType,
    Currency,
    DeliveryBucket,
    DeliveryOption,
    DynamicShop,
    ExchangeRate,
    HyperCategory,
    HyperCategoryType,
    MnPlace,
    Model,
    NavCategory,
    Offer,
    Outlet,
    PickupBucket,
    PickupOption,
    Region,
    RegionalDelivery,
    RegionalModel,
    Shop,
    UrlType,
    VCluster,
    YamarecFeaturePartition,
    YamarecPlace,
)
from core.testcase import TestCase, main
from core.matcher import Absent, Contains, LikeUrl, NoKey, NotEmpty


class WareMd5:
    MODEL_1001_CPA_REAL = '09lEaAKkQll1XTjm0WPoIA'
    MICROSOFTBAND = '19lEaAKkQll1XTjm0WPoIA'


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        # hid: [1, 2, 3, 4, 123456, 654321, 456789, 987654, 90433, 91491]
        # fesh: [204..260]
        # model id: [301..315]
        # vcluster id: [1000000001, 1000000002]

        cls.index.regiontree += [
            Region(rid=143, region_type=Region.COUNTRY),
            Region(rid=213, name='Москва'),
            Region(rid=2, name='Санкт-Петербург'),
            Region(
                rid=149,
                name='Беларусь',
                region_type=Region.COUNTRY,
                children=[Region(rid=157, name='Минск', region_type=Region.CITY)],
            ),
        ]

        cls.index.shops += [
            Shop(fesh=204, priority_region=213, cpa=Shop.CPA_REAL),
            Shop(fesh=205, priority_region=213, cpa=Shop.CPA_REAL),
            Shop(fesh=206, priority_region=213, cpa=Shop.CPA_REAL),
            Shop(fesh=207, priority_region=213, cpa=Shop.CPA_REAL),
            Shop(fesh=208, priority_region=213, cpa=Shop.CPA_REAL),
            Shop(fesh=209, priority_region=213, cpa=Shop.CPA_REAL),
            Shop(fesh=210, priority_region=213, cpa=Shop.CPA_REAL),
            Shop(fesh=211, priority_region=213, cpa=Shop.CPA_REAL),
            Shop(fesh=212, priority_region=213, cpa=Shop.CPA_REAL),
            Shop(fesh=222, priority_region=213, cpa=Shop.CPA_REAL),
            Shop(fesh=223, priority_region=157, cpa=Shop.CPA_REAL),
            Shop(fesh=224, priority_region=157, cpa=Shop.CPA_REAL),
            Shop(fesh=225, priority_region=157, cpa=Shop.CPA_REAL),
            Shop(fesh=226, priority_region=157, cpa=Shop.CPA_REAL),
            Shop(fesh=227, priority_region=157, cpa=Shop.CPA_REAL),
            Shop(fesh=228, priority_region=157, cpa=Shop.CPA_REAL),
            Shop(fesh=229, priority_region=157, cpa=Shop.CPA_REAL),
            Shop(fesh=230, priority_region=157, cpa=Shop.CPA_REAL),
        ]

        # Kyiv
        cls.index.shops += [
            Shop(fesh=213, priority_region=143, currency=Currency.UAH, cpa=Shop.CPA_REAL),
            Shop(fesh=214, priority_region=143, currency=Currency.UAH, cpa=Shop.CPA_REAL),
            Shop(fesh=215, priority_region=143, currency=Currency.UAH, cpa=Shop.CPA_REAL),
            Shop(fesh=216, priority_region=143, currency=Currency.UAH, cpa=Shop.CPA_REAL),
            Shop(fesh=217, priority_region=143, currency=Currency.UAH, cpa=Shop.CPA_REAL),
            Shop(fesh=218, priority_region=143, currency=Currency.UAH, cpa=Shop.CPA_REAL),
            Shop(fesh=219, priority_region=143, currency=Currency.UAH, cpa=Shop.CPA_REAL),
            Shop(fesh=220, priority_region=143, currency=Currency.UAH, cpa=Shop.CPA_REAL),
            Shop(fesh=221, priority_region=143, currency=Currency.UAH, cpa=Shop.CPA_REAL),
        ]

        cls.index.currencies = [Currency(Currency.UAH, exchange_rates=[ExchangeRate(to=Currency.RUR, rate=2)])]

        # Shops with region added because of MARKETOUT-8388

        cls.index.hypertree += [
            HyperCategory(hid=1, visual=True),
            HyperCategory(hid=2),
            HyperCategory(hid=3),
            HyperCategory(hid=456789),
            HyperCategory(hid=987654, visual=True),
            HyperCategory(hid=123456),
            HyperCategory(hid=654321, visual=True),
        ]
        cls.index.models += [
            Model(hyperid=301, hid=2),
            Model(hyperid=302, hid=2),
            Model(hyperid=303, hid=2),
            Model(hyperid=304, hid=2),
            Model(hyperid=306, hid=3, title='exceptional cpa model'),
        ]
        cls.index.offers += [
            Offer(hyperid=301, fesh=204, price=100000, cpa=Offer.CPA_REAL),
            Offer(hyperid=301, fesh=205, price=300000, cpa=Offer.CPA_REAL),
            Offer(hyperid=301, fesh=206, price=100000, cpa=Offer.CPA_NO),
            Offer(hyperid=301, fesh=208, price=300000, cpa=Offer.CPA_NO),
            Offer(hyperid=301, fesh=209, price=249999, cpa=Offer.CPA_REAL),
            Offer(hyperid=301, fesh=210, price=250001, cpa=Offer.CPA_REAL),
            Offer(hyperid=302, fesh=211, price=400000, cpa=Offer.CPA_REAL),
            Offer(hyperid=302, fesh=212, price=500000, cpa=Offer.CPA_REAL),
        ]
        cls.index.offers += [
            Offer(hyperid=303, fesh=213, price=10000, cpa=Offer.CPA_REAL),
            Offer(hyperid=303, fesh=214, price=30000, cpa=Offer.CPA_REAL),
            Offer(hyperid=303, fesh=215, price=90000, cpa=Offer.CPA_NO),
            Offer(hyperid=303, fesh=216, price=120000, cpa=Offer.CPA_NO),
            Offer(hyperid=303, fesh=217, price=159999, cpa=Offer.CPA_REAL),
            Offer(hyperid=303, fesh=218, price=220001, cpa=Offer.CPA_REAL),
            Offer(hyperid=304, fesh=219, price=412345, cpa=Offer.CPA_REAL),
            Offer(hyperid=304, fesh=220, price=500000, cpa=Offer.CPA_REAL),
            Offer(hyperid=306, fesh=204, price=5000, cpa=Offer.CPA_REAL, title='exceptional cpa offer'),
        ]

        cls.index.cpa_categories += [
            CpaCategory(hid=456789, regions=[2, 213], cpa_type=CpaCategoryType.CPA_WITH_CPC_PESSIMIZATION),
            CpaCategory(hid=987654, regions=[157], cpa_type=CpaCategoryType.CPA_WITH_CPC_PESSIMIZATION),
            # don't filter here
            CpaCategory(hid=123456, regions=[213, 2, 157], cpa_type=CpaCategoryType.CPC_AND_CPA),
        ]

        # for main:
        cls.index.offers += [
            Offer(title="test_filter_non_cpa_main 1", cpa=Offer.CPA_REAL, hid=456789, fesh=222),
            Offer(title="test_filter_non_cpa_main 2", cpa=Offer.CPA_NO, hid=654321, fesh=222),
            # cpa=Offer.CPA_NO, category is of type CPA_WITH_CPC_PESSIMIZATION
            Offer(title="test_filter_non_cpa_main 3", cpa=Offer.CPA_NO, hid=456789, fesh=222),
            Offer(title="test_filter_non_cpa_main 4", cpa=Offer.CPA_NO, fesh=222),
            Offer(title="test_filter_non_cpa_main 5", cpa=Offer.CPA_NO, hid=987654, fesh=222),
        ]

        # for microcard
        cls.index.models += [
            Model(hyperid=305, hid=456789),
        ]

        cls.index.offers += [
            Offer(title="test_dont_filter_non_cpa_microcard 1", cpa=Offer.CPA_NO, hyperid=305, fesh=222),
        ]

        # for test_dont_filter_models_by_flag
        cls.index.regional_models += [
            RegionalModel(hyperid=307, rids=[213]),
        ]

        cls.index.models += [Model(hyperid=307, title="dont_filter", hid=456789)]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5003,
                fesh=503,
                carriers=[99],
                options=[PickupOption(outlet_id=207)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

    def test_cpa_searchresults_moscow(self):
        response = self.report.request_json('place=prime&hid=2&rids=213')

        self.assertFragmentIn(
            response,
            {"search": {"cpaCount": 6, "results": [{"type": "model", "id": 301}, {"type": "model", "id": 302}]}},
        )

        for shopId in [204, 205, 209, 210, 211, 212]:
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {
                                "entity": "offer",
                                "shop": {
                                    "entity": "shop",
                                    "id": shopId,
                                },
                                "cpa": "real",
                            },
                        ]
                    }
                },
            )

        for shopId in [206, 208]:
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {
                                "entity": "offer",
                                "shop": {
                                    "entity": "shop",
                                    "id": shopId,
                                },
                            },
                        ]
                    }
                },
            )
            self.assertFragmentNotIn(
                response,
                {
                    "search": {
                        "results": [
                            {
                                "entity": "offer",
                                "shop": {
                                    "entity": "shop",
                                    "id": shopId,
                                },
                                "cpa": "real",
                            },
                        ]
                    }
                },
            )

    # CPA does not work for Ukraine (law issues)
    def test_cpa_searchresults_kyiv(self):
        response = self.report.request_json('place=prime&hid=2&debug=da&rids=143')

        self.assertFragmentIn(response, {"search": {"cpaCount": 0}})
        self.assertFragmentNotIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "debug": {
                                "factors": {
                                    "CPA": "1",
                                    "CPA1": "1",
                                }
                            }
                        }
                    ]
                }
            },
        )

    @classmethod
    def prepare_phone_urls(cls):
        # output_type is to prevent experimental skipping
        cls.index.hypertree += [
            HyperCategory(hid=50001, name="Interesting things cpa and cpc", output_type=HyperCategoryType.SIMPLE),
            HyperCategory(
                hid=50002, name="Interesting things cpa_with_cpc_pessimization", output_type=HyperCategoryType.GURU
            ),
            HyperCategory(hid=50003, name="Interesting things cpa_non_guru", output_type=HyperCategoryType.GURULIGHT),
            HyperCategory(hid=50004, name="Cpc", output_type=HyperCategoryType.GURU),
            HyperCategory(hid=50005, name="Cpa without phone settings", output_type=HyperCategoryType.GURU),
            HyperCategory(
                hid=150001, name="Cpa without phone settings", output_type=HyperCategoryType.GURU, visual=True
            ),
        ]

        cls.index.cpa_categories += [
            CpaCategory(
                hid=50001,
                regions=[2, 213],
                cpa_type=CpaCategoryType.CPC_AND_CPA,
                phone_click_ratio=100,
                phone_click_threshold=1000000,
            ),
            CpaCategory(
                hid=50002,
                regions=[213],
                cpa_type=CpaCategoryType.CPA_WITH_CPC_PESSIMIZATION,
                phone_click_ratio=2000,
                phone_click_threshold=30000,
            ),
            CpaCategory(
                hid=50003,
                regions=[2, 157, 213],
                cpa_type=CpaCategoryType.CPA_NON_GURU,
                phone_click_ratio=3000,
                phone_click_threshold=3000,
            ),
            CpaCategory(hid=50005, regions=[2, 213], cpa_type=CpaCategoryType.CPC_AND_CPA),
            CpaCategory(
                hid=150001,
                regions=[2, 157, 213],
                cpa_type=CpaCategoryType.CPA_NON_GURU,
                phone_click_ratio=3000,
                phone_click_threshold=3000,
            ),
        ]

        cls.index.models += [
            Model(hyperid=50001, title="cpc_and_cpa", hid=50001),
            Model(hyperid=50002, title="cpa_with_cpc_pessimization", hid=50002),
            Model(hyperid=50003, title="cpa_non_guru", hid=50003),
            Model(hyperid=50004, title="not cpa at all", hid=50004),
            Model(hyperid=50005, title="cpa_with_cpc_pessimization without phone settings", hid=50005),
        ]

        cls.index.vclusters += [
            VCluster(vclusterid=1000000101, hid=150001),
        ]

        cls.index.shops += [
            Shop(fesh=501, priority_region=2, shop_fee=100, cpa=Shop.CPA_REAL),
            Shop(fesh=502, priority_region=213, shop_fee=200, cpa=Shop.CPA_REAL),
            Shop(fesh=503, priority_region=213, shop_fee=300, cpa=Shop.CPA_REAL),
            Shop(fesh=504, priority_region=213, shop_fee=400, cpa=Shop.CPA_REAL),
            Shop(fesh=505, priority_region=213, shop_fee=500, cpa=Shop.CPA_REAL),
        ]

        outlet = Outlet(point_id=207, fesh=503, region=213, point_type=Outlet.FOR_STORE)
        cls.index.outlets += [outlet]

        cls.index.offers += [
            Offer(cpa=Offer.CPA_NO, hyperid=50001, fesh=501, title="offer st petersburg", ts=11, price=1000),
            Offer(cpa=Offer.CPA_REAL, hyperid=50002, fesh=502, title="ruchnoy moscow", ts=12, price=60075),
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=50003,
                fesh=503,
                title="ruchnoy minsk",
                ts=13,
                price=3000,
                pickup_buckets=[5003],
            ),
            Offer(cpa=Offer.CPA_REAL, hyperid=50002, fesh=504, title="ruchnoy moscow 2", ts=14, price=990261),
            Offer(cpa=Offer.CPA_REAL, hyperid=50004, fesh=504, title="funny toy", ts=15, price=8000),
            Offer(cpa=Offer.CPA_REAL, hyperid=50002, fesh=505, title="very expensive toy", ts=16, price=6000000),
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=50005,
                fesh=504,
                title="funny toy",
                ts=17,
                price=777,
                booking_availabilities=[
                    BookingAvailability(outlet_id=outlet.point_id, region_id=outlet.region_id, amount=2),
                ],
            ),
            Offer(
                cpa=Offer.CPA_REAL,
                pricefrom=True,
                vclusterid=1000000101,
                fesh=504,
                title="funny visual offer",
                ts=18,
                price=5000,
            ),
        ]

    def test_phone_urls(self):
        """
        tests both for touch and web
        check that there is nothing in show log and click url if category is not in cpa_categories
        check that there is nothing in show log if phone settings are not set for cpa category
        there is no phone_click_price in show log for category of type CPC_AND_CPA and click price is not changed = '10'
        phone url is shown
        phone_click_threshold is used if price too high
        click price is calculated properly (brokered fee is used) and rounded properly
        check that all numbers are ok for cpa_non_guru category
        test in different places

        &touch=1 disabled because of offer collapsing in MARKETOUT-12117
        """

        # test no cpa category has no any lines in log (touch and browser client)
        self.report.request_json(
            "place=prime&hyperid=50004&show-urls=showPhone&rearr-factors=market_skip_cpa_category_settings=skip_none"
        )
        # self.report.request_json("place=prime&hyperid=50004&show-urls=phone&rearr-factors=market_skip_cpa_category_settings=skip_none&touch=1")

        # for url_phone_type in [UrlType.PHONE, UrlType.SHOW_PHONE]:
        for url_phone_type in [UrlType.SHOW_PHONE]:
            self.show_log_tskv.expect(
                click_type_id=1,
                url_type=url_phone_type,
                price=8000,
                phone_click_ratio=Absent(),
                phone_click_threshold=Absent(),
                click_price=11,
            )
            self.click_log.expect(clicktype=url_phone_type, phone_click_ratio=Absent())

        # test CPC_AND_CPA category
        self.report.request_json(
            "place=prime&hyperid=50001&show-urls=showPhone&rearr-factors=market_skip_cpa_category_settings=skip_none&rids=2"
        )
        # self.report.request_json("place=prime&hyperid=50001&show-urls=phone&rearr-factors=market_skip_cpa_category_settings=skip_none&rids=2&touch=1")
        # for url_phone_type in [UrlType.PHONE, UrlType.SHOW_PHONE]:
        for url_phone_type in [UrlType.SHOW_PHONE]:
            self.show_log_tskv.expect(
                click_type_id=1,
                url_type=url_phone_type,
                click_price=2,
                price=1000,
                phone_click_ratio=Absent(),
                phone_click_threshold=Absent(),
            )
            self.click_log.expect(clicktype=url_phone_type, phone_click_ratio=Absent())

        # test CPA_WITH_CPC_PESSIMIZATION category without phone settings
        self.report.request_json(
            "place=prime&hyperid=50005&show-urls=showPhone&rearr-factors=market_skip_cpa_category_settings=skip_none&rids=213"
        )
        # self.report.request_json("place=prime&hyperid=50005&show-urls=phone&rearr-factors=market_skip_cpa_category_settings=skip_none&rids=213&touch=1")
        # for url_phone_type in [UrlType.PHONE, UrlType.SHOW_PHONE]:
        for url_phone_type in [UrlType.SHOW_PHONE]:
            self.show_log_tskv.expect(
                click_type_id=1,
                url_type=url_phone_type,
                click_price=1,
                price=777,
                phone_click_ratio=Absent(),
                phone_click_threshold=Absent(),
            )
            self.click_log.expect(clicktype=url_phone_type, phone_click_ratio=Absent())

    @classmethod
    def prepare_prime_show_and_call_phone_urls(cls):
        # phone-urls
        cls.index.hypertree += [
            HyperCategory(hid=501, name="Interesting things cpa and cpc", output_type=HyperCategoryType.GURU),
        ]
        cls.index.navtree += [
            NavCategory(nid=301, hid=501, name="Test cat"),
        ]
        cls.index.cpa_categories += [
            CpaCategory(
                hid=501,
                regions=[2, 213],
                cpa_type=CpaCategoryType.CPA_WITH_CPC_PESSIMIZATION,
                phone_click_ratio=400,
                phone_click_threshold=1000000,
            ),
        ]
        cls.index.shops += [
            Shop(fesh=511, priority_region=213, shop_fee=200, cpa=Shop.CPA_REAL),
        ]
        cls.index.offers += [
            Offer(
                cpa=Offer.CPA_REAL,
                title='microsoftband',
                fesh=511,
                hid=501,
                price=6000,
                waremd5=WareMd5.MICROSOFTBAND,
                hyperid=901,
            )
        ]

    def test_prime_show_and_call_phone_urls(self):
        """
        check information in show log and click log for phone urls in prime
        for browser and touch version
        """
        # click_price = 6000 * 0.02 * 0.04 /30 * 100 = 16
        self.report.request_json('place=prime&text=microsoftband&show-urls=showPhone&rids=213')
        self.show_log_tskv.expect(
            click_type_id=1,
            url_type=UrlType.SHOW_PHONE,
            phone_click_threshold=1000000,
            phone_click_ratio=0.04,
            click_price=16,
        ).times(1)
        self.click_log.expect(clicktype=ClickType.SHOW_PHONE, url_type=UrlType.SHOW_PHONE)

        # touch version
        self.report.request_json(
            'place=prime&text=microsoftband&show-urls=phone&touch=1&phone=1&rids=213&allow-collapsing=0'
        )
        self.show_log_tskv.expect(
            click_type_id=1,
            url_type=UrlType.PHONE,
            url=LikeUrl.of(
                '//m.market.yandex.ru/offer/19lEaAKkQll1XTjm0WPoIA?shop_id=511&hid=501&nid=301&modelid=901&call=1'
            ),
            phone_click_ratio=0.04,
            phone_click_threshold=1000000,
            click_price=16,
        )

        self.click_log.expect(
            clicktype=ClickType.PHONE,
            url_type=UrlType.PHONE,
        )

    @classmethod
    def prepare_cpa_count_test(cls):
        cls.index.hypertree += [
            HyperCategory(hid=301091, visual=True),
            HyperCategory(hid=301090),
        ]
        cls.index.models += [
            Model(hyperid=3010, hid=301090),
        ]
        cls.index.vclusters += [
            VCluster(vclusterid=1000301091, hid=301091),
        ]
        cls.index.offers += [
            Offer(hyperid=3010, fesh=204, price=100000, cpa=Offer.CPA_REAL, title='cpaCount'),
            Offer(hyperid=3010, fesh=205, price=300000, cpa=Offer.CPA_REAL, title='cpaCount'),
            Offer(hyperid=3010, fesh=206, price=100000, cpa=Offer.CPA_NO, title='cpaCount'),
            Offer(hyperid=3010, fesh=208, price=300000, cpa=Offer.CPA_NO, title='cpaCount'),
            Offer(hyperid=3010, fesh=209, price=249999, cpa=Offer.CPA_REAL, title='cpaCount'),
            Offer(hyperid=3010, fesh=210, price=250001, cpa=Offer.CPA_REAL, title='cpaCount'),
            Offer(hyperid=3010, fesh=211, price=400000, cpa=Offer.CPA_REAL, title='cpaCount'),
            Offer(hyperid=3010, fesh=212, price=500000, cpa=Offer.CPA_REAL, title='cpaCount'),
            Offer(vclusterid=1000301091, fesh=204, price=100000, cpa=Offer.CPA_REAL, title='cpaCount'),
            Offer(vclusterid=1000301091, fesh=205, price=300000, cpa=Offer.CPA_REAL, title='cpaCount'),
            Offer(vclusterid=1000301091, fesh=206, price=100000, cpa=Offer.CPA_NO, title='cpaCount'),
            Offer(vclusterid=1000301091, fesh=208, price=300000, cpa=Offer.CPA_NO, title='cpaCount'),
            Offer(vclusterid=1000301091, fesh=209, price=249999, cpa=Offer.CPA_REAL, title='cpaCount'),
            Offer(vclusterid=1000301091, fesh=210, price=250001, cpa=Offer.CPA_REAL, title='cpaCount'),
            Offer(vclusterid=1000301091, fesh=211, price=400000, cpa=Offer.CPA_REAL, title='cpaCount'),
            Offer(vclusterid=1000301091, fesh=212, price=500000, cpa=Offer.CPA_REAL, title='cpaCount'),
        ]

    def test_cpa_count_prime(self):
        """
        Проверка корректного значения cpa-count для place=prime
        """
        response = self.report.request_json('place=prime&text=cpaCount&rids=213')
        self.assertFragmentIn(response, {"search": {"cpaCount": 12}}, preserve_order=True)

    def test_cpa_count_productoffers(self):
        """
        Проверка корректного значения cpa-count для place=productoffers
        """
        response = self.report.request_json('place=productoffers&hyperid=3010&rids=213')
        self.assertFragmentIn(response, {"search": {"cpaCount": 6}}, preserve_order=True)

    @classmethod
    def prepare_jewelry(cls):
        cls.index.hypertree += [
            HyperCategory(
                hid=6206931,
                name='Jewelry',
                children=[
                    HyperCategory(hid=2000),
                ],
            ),
        ]
        cls.index.offers += [
            Offer(hid=6206931, cpa=Offer.CPA_REAL, fesh=204),
            Offer(hid=2000, cpa=Offer.CPA_REAL, fesh=204),
        ]

    def test_jewelry_forbid_allow_cpc_pessimization(self):
        """
        Проверяем, что CPA офферы в категории "Ювелирные изделия" и во всех дочерних превращаются в CPC.
        Дополнительно проверяется, что причина пессимизации выведена в debug
        """
        response = self.report.request_json(
            'place=prime&hid=6206931&debug=1&rearr-factors=market_filter_cpa_to_cpc_degradation=0'
        )
        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'offer',
                    'cpa': Absent(),
                    "debug": {"properties": {"CPA_PESSIMIZATION_JEWELRY_CATEGORY": "1"}},
                },
                {
                    'entity': 'offer',
                    'cpa': Absent(),
                    "debug": {"properties": {"CPA_PESSIMIZATION_JEWELRY_CATEGORY": "1"}},
                },
            ],
        )

    def test_jewelry_forbid_cpc_pessimization(self):
        """
        Проверяем, что CPA офферы в категории "Ювелирные изделия" и во всех дочерних скрываются.
        """
        response = self.report.request_json('place=prime&hid=6206931&debug=1')
        self.assertFragmentIn(response, {"filters": {"HIDE_CPA_PESSIMIZATION_JEWELRY_CATEGORY": 2}})

    def test_cpa_auction_logs(self):
        """
        check that the fields for market auction exist in logs
        """
        self.report.request_json('place=prime&text=cpaCount&rids=213&&show-urls=cpa')
        self.click_log.expect(ClickType.CPA, fee=NotEmpty(), shop_fee=NotEmpty(), vendor_fee=NotEmpty())

        self.show_log.expect(fee=NotEmpty(), shop_fee=NotEmpty(), vc_bid=NotEmpty())

    # MARKETOUT-13090
    @classmethod
    def prepare_cpa_minus_no(cls):
        cls.index.shops += [
            Shop(fesh=1309001, cpa=Shop.CPA_REAL, priority_region=213),
        ]

        cls.index.offers += [
            Offer(fesh=1309001, hyperid=1309001, cpa=Offer.CPA_REAL),
        ]

    def test_cpa_minus_no(self):
        """
        Делаем запрос с cpa=-no, оффер должен быть в выдаче
        """
        self.dynamic.market_dynamic.disabled_cpa_shops += [DynamicShop(1309001)]

        request = "place=productoffers&hyperid=1309001&rids=213{}"
        response = self.report.request_json(request.format('&cpa=-no'))
        self.assertFragmentIn(response, {"entity": "offer", "model": {"id": 1309001}, "cpa": "real"})

    def test_cpa_minus_no_allow_cpc_pessimization(self):
        """Проверяется, что CPA оффер пессимизирован до cpa=no для магазина из shop_cpa_filter"""
        self.dynamic.market_dynamic.disabled_cpa_shops += [DynamicShop(1309001)]

        request = "place=productoffers&hyperid=1309001&rids=213{}"
        response = self.report.request_json(
            request.format('&debug=1&rearr-factors=market_filter_cpa_to_cpc_degradation=0')
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "model": {"id": 1309001},
                "cpa": NoKey("cpa"),
                "debug": {"properties": {"CPA_PESSIMIZATION_BAD_CPA_SHOP": "1"}},
            },
        )

    def test_cpa_minus_no_forbid_cpc_pessimization(self):
        """Проверяется, что CPA оффер скрыт из-за пессимизиции до cpa=no для магазина из shop_cpa_filter."""
        self.dynamic.market_dynamic.disabled_cpa_shops += [DynamicShop(1309001)]

        request = "place=productoffers&hyperid=1309001&rids=213{}"
        response = self.report.request_json(request.format('&debug=1'))
        self.assertFragmentIn(response, {"filters": {"HIDE_CPA_PESSIMIZATION_BAD_CPA_SHOP": 1}})

    @classmethod
    def prepare_no_url(cls):
        '''Создаем два оффера без URL с cpa=REAL'''
        cls.index.shops += [
            Shop(fesh=1309002, cpa=Shop.CPA_REAL, cpc=Shop.CPC_REAL, priority_region=213),
        ]

        cls.index.offers += [
            Offer(fesh=1309002, title='no_url', hyperid=1309002, has_url=False, cpa=Offer.CPA_REAL, is_cpc=True),
            Offer(fesh=1309002, title='no_url2', hyperid=1309002, has_url=False, cpa=Offer.CPA_REAL, is_cpc=True),
        ]

    def test_no_url(self):
        '''Что тестируем: офферы без URL возвращаются на основных плейсах
        и урлы типов CPA и OFFERCARD пишутся в логи показов и кликов с
        нулевым click_price
        '''

        response = self.report.request_json('place=prime&show-urls=cpa,external,offercard&text=no_url&rids=213')
        self.assertEqual(response.count({'entity': 'offer'}), 2)

        response = self.report.request_json(
            'place=productoffers&show-urls=cpa,external,offercard&cpa-pof=somepof&hyperid=1309002&rids=213'
        )
        self.assertEqual(response.count({'entity': 'offer'}), 2)

        response = self.report.request_json(
            'place=defaultoffer&hyperid=1309002&rids=213&show-urls=external,showPhone,cpa'
        )
        self.assertEqual(response.count({'entity': 'offer'}), 1)

        response = self.report.request_json('place=modelinfo&hyperid=1309002&rids=213')
        self.assertEqual(response.count({'entity': 'product'}), 1)

        self.show_log.expect(url_type=UrlType.OFFERCARD).times(4)
        self.click_log.expect(ClickType.EXTERNAL, url_type=UrlType.EXTERNAL).times(0)
        self.click_log.expect(ClickType.SHOW_PHONE, url_type=UrlType.SHOW_PHONE).times(0)
        self.click_log.expect(ClickType.OFFERCARD, url_type=UrlType.OFFERCARD).times(4)
        self.click_log.expect(ClickType.CPA, url_type=UrlType.CPA).times(5)

    @classmethod
    def prepare_cpa20(cls):
        cls.index.models += [
            Model(hyperid=1001, title="IsCpa20 Model_1001", hid=2211),
            Model(hyperid=1002, title="IsCpa20 Model_1002"),
            Model(hyperid=1003, title="conditional_cpa_2.0 model1", hid=2210, accessories=[1004], analogs=[1004]),
            Model(hyperid=1004, title="conditional_cpa_2.0 model2", hid=2210, accessories=[1003], analogs=[1003]),
        ]

        cls.index.regional_models += [
            RegionalModel(hyperid=1001, rids=[47], offers=4, cpa20=True),
            RegionalModel(hyperid=1001, rids=[54], offers=1, cpa20=False),
            RegionalModel(hyperid=1002, rids=[47], offers=1, cpa20=False),
            RegionalModel(
                hyperid=1003,
                price_old_min=300,
                price_min=100,
                price_max=200,
                max_discount=2,
                rids=[47],
                offers=2,
                cpa20=True,
            ),
            RegionalModel(
                hyperid=1004, price_old_min=300, price_min=100, price_max=200, max_discount=2, rids=[47], offers=1
            ),
        ]

        cls.index.delivery_buckets += [
            # fulfillment delivery buckets
            DeliveryBucket(
                bucket_id=1000,
                fesh=1000,
                regional_options=[
                    RegionalDelivery(rid=47, options=[DeliveryOption(price=10, day_from=1, day_to=2)]),
                ],
            ),
        ]

        cls.index.shops += [
            # ФФ магазин
            Shop(
                fesh=1000,
                priority_region=47,
                cpa=Shop.CPA_REAL,
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
            ),
            # Обычный магазин
            Shop(fesh=1001, priority_region=47, regions=[47, 54], cpa=Shop.CPA_REAL),
            # Магазин по программе CPA 2.0
            Shop(fesh=1002, priority_region=47, cpa=Shop.CPA_REAL, cpa20=True),
            Shop(fesh=1003, priority_region=47, cpa=Shop.CPA_REAL, cpa20=True),
        ]

        # данные для тестирования появления cpa2.0 магазина на первом месте в фильтре магазинов
        cpa20_shop_id = 1000200
        cls.index.shops.append(
            Shop(fesh=cpa20_shop_id, priority_region=47, cpa=Shop.CPA_REAL, cpa20=True, name="B_cpa20")
        )
        cls.index.offers.append(Offer(hyperid=2001001, fesh=cpa20_shop_id, cpa=Offer.CPA_REAL))
        for i in range(1, 15):
            shop_id = cpa20_shop_id + i
            cls.index.shops.append(Shop(fesh=shop_id, priority_region=47, cpa=Shop.CPA_REAL, name="A_{}".format(i)))
            # добавляется по 2 и более офферов в магазин, чтобы cpa2.0 магазин (B_cpa20) был самым неприоритетным в условии сортировки значений фильтра
            for k in range(1 + i):
                cls.index.offers.append(Offer(hyperid=2001001, fesh=shop_id, cpa=Offer.CPA_REAL))

        cls.index.outlets += [Outlet(point_id=2003, fesh=1003, region=47, point_type=Outlet.FOR_PICKUP)]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=7003,
                fesh=1003,
                carriers=[99],
                options=[PickupOption(outlet_id=2003)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        # Нужно, чтобы зафиксировать оффер на 1 позиции и сверять потом rs в карточке оффера, в котором есть position
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1).respond(0.99)

        cls.index.offers += [
            Offer(
                hyperid=1001,
                title='IsCpa20 Real Simple_Shop Model_1001',
                fesh=1001,
                price=1,
                cpa=Offer.CPA_REAL,
                url='http://simpleshop.ru/model-1001',
            ),
            Offer(
                hyperid=1001,
                title='IsCpa20 Real Cpa20_Shop Model_1001',
                fesh=1002,
                price=1,
                cpa=Offer.CPA_REAL,
                url='http://cpa20shop.ru/model-1001-cpa-real',
                waremd5=WareMd5.MODEL_1001_CPA_REAL,
                ts=1,
            ),
            Offer(
                hyperid=1001,
                title='IsCpa20 No Cpa20_Shop Model_1001',
                fesh=1002,
                price=1,
                cpa=Offer.CPA_NO,
                url='http://cpa20shop.ru/model-1001-no-cpa',
            ),
            Offer(hyperid=1002, title='IsCpa20 Real Simple_Shop Model_1002', fesh=1001, price=1, cpa=Offer.CPA_REAL),
            Offer(
                hyperid=1003,
                title="conditional_cpa_2.0 offer no_pickup",
                fesh=1003,
                price=10,
                cpa=Offer.CPA_REAL,
                pickup=False,
                discount=20,
                pickup_buckets=[7003],
            ),
            Offer(
                hyperid=1003,
                title="conditional_cpa_2.0 offer with_pickup",
                fesh=1003,
                price=10,
                cpa=Offer.CPA_NO,
                discount=20,
                pickup_buckets=[7003],
            ),
            Offer(
                hyperid=1004, title="conditional_cpa_2.0 no_cpa20", fesh=1001, price=10, cpa=Offer.CPA_REAL, discount=20
            ),
        ]

        cls.index.hypertree += [
            HyperCategory(hid=2210, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=2211, output_type=HyperCategoryType.GURU),
        ]

        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.CATEGORY_GENERIC,
                kind=YamarecPlace.Type.FORMULA,
                partitions=[
                    YamarecFeaturePartition(
                        feature_names=['category_id', 'position'],
                        feature_keys=['category_id'],
                        features=[[2210, 1]],
                        splits=['9'],
                    )
                ],
            ),
            YamarecPlace(
                name=YamarecPlace.Name.POPULAR_MODELS,
                kind=YamarecPlace.Type.FORMULA,
                partitions=[
                    YamarecFeaturePartition(
                        feature_names=['model_id', 'category_id', 'position'],
                        feature_keys=['model_id', 'category_id'],
                        features=[[1003, 2210, 1], [1004, 2210, 2]],
                        splits=['9'],
                    )
                ],
            ),
        ]

    @classmethod
    def prepare_price_from(cls):
        cls.index.shops += [
            Shop(fesh=1201, priority_region=213, cpa=Shop.CPA_REAL),
        ]

        cls.index.offers += [
            Offer(fesh=1201, price=100, cpa=Offer.CPA_REAL, pricefrom=True),
        ]

    def test_price_from_allow_cpc_pessimization(self):
        """
        Что тестируем: пессимизацию СРА-СРС, если цена офера указано "от"
        """
        response = self.report.request_json(
            "place=prime&fesh=1201&debug=1&rearr-factors=market_filter_cpa_to_cpc_degradation=0"
        )
        self.assertFragmentIn(
            response,
            {"entity": "offer", "cpa": NoKey("cpa"), "debug": {"properties": {"CPA_PESSIMIZATION_PRICE_FROM": "1"}}},
        )

    def test_price_from_forbid_cpc_pessimization(self):
        """
        Что тестируем: скрытие из-за пессимизации СРА-СРС, если цена офера указано "от"
        """
        response = self.report.request_json("place=prime&fesh=1201&debug=1")
        self.assertFragmentIn(response, {"filters": {"HIDE_CPA_PESSIMIZATION_PRICE_FROM": 1}})

    def test_fee_show_plain(self):
        """
        Проверяем, что правильный pp прокидывается в feeShow
        """

        response = self.report.request_json('place=productoffers&pp=6&offers-set=default&hyperid=304&debug=1')
        self.assertFragmentIn(response, {'feeShowPlain': Contains("pp: 200")})


if __name__ == '__main__':
    main()
