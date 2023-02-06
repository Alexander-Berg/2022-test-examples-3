#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BookingAvailability,
    DeliveryBucket,
    DeliveryOption,
    GLParam,
    GLType,
    GpsCoord,
    HyperCategory,
    HyperCategoryType,
    Model,
    NewShopRating,
    Offer,
    Outlet,
    PickupBucket,
    PickupOption,
    Region,
    RegionalModel,
    Shop,
    VCluster,
    DynamicDeliveryServiceInfo,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseInfo,
    DateSwitchTimeAndRegionInfo,
    TimeInfo,
    DynamicDaysSet,
    DeliveryServiceRegionToRegionInfo,
    OutletDeliveryOption,
    MarketSku,
    BlueOffer,
)
from core.testcase import TestCase, main
from core.matcher import Absent, EmptyList, NoKey, Contains

from unittest import skip
import itertools


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']
        # hid: [1, 100]
        # vclusterid: [101, 200]
        # fesh: [201, 300]
        # model id: [301, 400]
        # glparam id: [500, 550]
        # glvalue id: [551, 599]

        # Output with price_from, base id = 0

        cls.index.hypertree += [
            HyperCategory(hid=1, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=2, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=3),
            HyperCategory(hid=4),
            HyperCategory(hid=5),
            HyperCategory(hid=10),
            HyperCategory(hid=100, output_type=HyperCategoryType.CLUSTERS, visual=True),
        ]

        cls.index.models += [
            Model(hyperid=301, hid=1),
            Model(hyperid=501, hid=2),
            Model(hyperid=302, hid=4, title='search me'),  # model w/o offers
            Model(hyperid=303, hid=4),  # model w/ onstock offers
            Model(hyperid=304, hid=5, title='tovar 2'),
            Model(hyperid=305, hid=4),  # model w offers in hid = 4
            Model(hyperid=306, hid=1, title='search me'),
            Model(hyperid=3001, hid=1),  # empty model
        ]

        cls.index.vclusters += [
            VCluster(vclusterid=1000000101, hid=100, title='search me'),
            VCluster(vclusterid=1000000102, hid=100, title='search me'),  # cluster w/o offers
        ]

        cls.index.regiontree += [
            Region(rid=3),
        ]

        cls.index.shops += [
            Shop(fesh=1001, priority_region=1, regions=[1]),
            Shop(fesh=1002, priority_region=2, regions=[2]),
        ]

        # реальные офферы приматченные к модели
        cls.index.offers += [
            Offer(hyperid=306, fesh=1001, price=460),
            Offer(hyperid=306, fesh=1001, price=400),
            Offer(hyperid=306, fesh=1001, price=600),
            Offer(hyperid=306, fesh=1002, price=655),
            Offer(hyperid=306, fesh=1002, price=500),
            Offer(hyperid=306, fesh=1002, price=700),
        ]

        # статистика посчитанная индексатором
        cls.index.regional_models += [
            RegionalModel(hyperid=306, rids=[1], price_min=400, price_max=600, offers=3),
            RegionalModel(hyperid=306, rids=[2], price_min=500, price_max=700, offers=3),
            RegionalModel(hyperid=306, rids=[-1], price_min=400, price_max=700, offers=6),
            RegionalModel(hyperid=303, onstock=3, offers=10),
        ]

        cls.index.offers += [
            Offer(
                hyperid=301,
                fesh=204,
                price=100,
                manufacturer_warranty=True,
                delivery_options=[
                    DeliveryOption(price=0, day_from=1, day_to=2, order_before=23),
                ],
            ),
            Offer(
                hyperid=301,
                fesh=205,
                price=200,
                cpa=4,
                delivery_options=[DeliveryOption(price=10, day_from=10, day_to=20)],
                title='tovar 1',
            ),
            Offer(hyperid=303, fesh=204),
            Offer(hyperid=501, fesh=205, adult=True, title='tovar 1'),
            Offer(fesh=206, price=300, title='tovar 1'),
            Offer(fesh=207, price=300, cpa=4, hid=3),
            Offer(fesh=207, price=300, hid=3),
            Offer(fesh=208, price=300, cpa=4, hid=5, title='tovar 2'),
            Offer(fesh=208, price=400, hid=5, title='tovar 2'),
            Offer(fesh=1001, vclusterid=1000000101, hid=100),
        ]

        cls.index.offers += [
            Offer(hid=259, title="OOOPS", delivery_options=[DeliveryOption(5, 10)]),
        ]

        # Test postomat shipping filter
        cls.index.offers += [
            Offer(
                fesh=210,
                title="postomat",
                hid=10,
                has_delivery_options=False,
                store=False,
                pickup=True,
                post_term_delivery=True,
            ),
            Offer(
                fesh=211,
                title="pickup",
                hid=10,
                has_delivery_options=False,
                store=False,
                pickup=True,
                post_term_delivery=False,
            ),
            Offer(
                fesh=211,
                title="store",
                hid=10,
                has_delivery_options=False,
                store=True,
                pickup=False,
                post_term_delivery=False,
            ),
            Offer(
                fesh=212,
                title="delivery",
                hid=10,
                has_delivery_options=True,
                store=False,
                pickup=False,
                post_term_delivery=False,
            ),
            Offer(
                fesh=212,
                title="not delivered",
                hid=10,
                has_delivery_options=False,
                store=False,
                pickup=False,
                post_term_delivery=False,
            ),
        ]

        cls.index.models += [
            Model(title="model", hid=10, hyperid=1001),
        ]
        cls.index.regional_models += [RegionalModel(hyperid=1001, rids=[1], offers=10)]

        # test warranty filter
        cls.index.offers += [
            Offer(hid=1, manufacturer_warranty=True, title="Thing with warranty"),
            Offer(hid=1, manufacturer_warranty=False, title="Thing without warranty"),
        ]

        cls.index.shops += [
            Shop(fesh=204, priority_region=1, regions=[225], new_shop_rating=NewShopRating(new_rating_total=5.0)),
            Shop(
                fesh=205,
                priority_region=1,
                regions=[225],
                new_shop_rating=NewShopRating(new_rating_total=4.0),
                cpa=Shop.CPA_REAL,
            ),
            Shop(
                fesh=207,
                priority_region=1,
                regions=[225],
                new_shop_rating=NewShopRating(new_rating_total=4.0),
                cpa=Shop.CPA_REAL,
            ),
            # Test postomat shipping filter
            Shop(
                fesh=210,
                priority_region=1,
                regions=[225],
                new_shop_rating=NewShopRating(new_rating_total=4.0),
                delivery_service_outlets=[200],
                pickup_buckets=[5001],
            ),
            Shop(
                fesh=211,
                priority_region=1,
                regions=[225],
                new_shop_rating=NewShopRating(new_rating_total=4.0),
                pickup_buckets=[5002],
            ),
            Shop(fesh=212, priority_region=1, regions=[225], new_shop_rating=NewShopRating(new_rating_total=4.0)),
            Shop(
                fesh=208,
                priority_region=1,
                regions=[225],
                new_shop_rating=NewShopRating(new_rating_total=4.0),
                cpa=Shop.CPA_REAL,
            ),
        ]

        # Test postomat shipping filter
        cls.index.outlets += [
            Outlet(
                point_id=200,
                region=1,
                point_type=Outlet.FOR_POST_TERM,
                gps_coord=GpsCoord(37.3, 55.3),
                delivery_service_id=103,
            ),
            Outlet(fesh=211, region=1, point_type=Outlet.FOR_PICKUP, gps_coord=GpsCoord(37.3, 56.3), point_id=1231),
            Outlet(fesh=211, region=1, point_type=Outlet.FOR_STORE, point_id=1232),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5001,
                carriers=[103],
                options=[PickupOption(outlet_id=200)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5002,
                fesh=211,
                carriers=[99],
                options=[PickupOption(outlet_id=1231), PickupOption(outlet_id=1232)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        cls.index.offers += [
            Offer(
                hyperid=305,
                fesh=204,
                delivery_options=[
                    DeliveryOption(price=10, day_from=1, day_to=2, order_before=23),
                ],
            )
        ]

        shop_names_in_increasing_offer_count_order = [
            'Ах-Подарки',  # 1 offer
            'APPLE-RU',  # 2 offers
            'ТехноГид',  # 3 offers
            'Web-Computers',  # 4 offers
            '003.ru',  # 5 offers
            'gbstore.ru',  # 6 offers
            'ДешевлеВсего.ru',  # 7 offers
            '"4точки-СПб"',  # 8 offers
            'UppStore',  # 9 offers
            'iCases.RU',  # 10 offers
            'Gosso Design',  # 11 offers
            'iCult.ru',  # 12 offers
            'СВЯЗНОЙ',  # 13 offers
            'Pleer.ru',  # 14 offers
            'Je-Store.ru',  # 15 offers
            'САЙДЕКС Гипермаркет',  # 16 offers
            'БайМобайл',  # 17 offers
        ]

        for idx, name in enumerate(shop_names_in_increasing_offer_count_order, start=1):
            shop_id = 2000 + idx
            offer_title = 'iphone' if idx % 2 == 0 else ''
            cls.index.shops += [Shop(fesh=shop_id, name=name, priority_region=1)]
            for i in range(1, idx + 1):
                cls.index.offers += [Offer(hid=42, fesh=shop_id, title=offer_title)]

        cls.index.shops += [
            Shop(fesh=3001, name='21vek.by', priority_region=1),
            Shop(fesh=3002, name='220 ВОЛЬТ-ВН', priority_region=1),
            Shop(fesh=3003, name='24shop.by', priority_region=1),
            Shop(fesh=3004, name='2case.mobi', priority_region=1),
        ]

        cls.index.offers += [
            Offer(hid=43, fesh=3001),
            Offer(hid=43, fesh=3002),
            Offer(hid=43, fesh=3003),
            Offer(hid=43, fesh=3004),
        ]

    def test_price_filter(self):
        # Want offers between 100 and 150 RUB
        response = self.report.request_json(
            'place=prime&hid=1&mcpricefrom=100&mcpriceto=150&rids=1' '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            {"results": [{"id": 301, "entity": "product"}, {"model": {"id": 301}, "entity": "offer"}]},
            preserve_order=True,
            allow_different_len=False,
        )

        # Want offers between 100 and 150 UE
        response = self.report.request_json(
            'place=prime&hid=1&mcpricefrom=100&mcpriceto=150&filter-currency=UE&rids=1'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(response, {"results": []}, preserve_order=True, allow_different_len=False)

        # Want offers between 100 and 150 RUB explicitly
        response = self.report.request_json(
            'place=prime&hid=1&mcpricefrom=100&mcpriceto=150&filter-currency=RUB&rids=1'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            {"results": [{"id": 301, "entity": "product"}, {"model": {"id": 301}, "entity": "offer"}]},
            preserve_order=True,
            allow_different_len=False,
        )

        # Want offers between 2 and 4 UE (assuming that rate is 30).
        response = self.report.request_json(
            'place=prime&hid=1&mcpricefrom=2&mcpriceto=4&filter-currency=UE&rids=1'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            {"results": [{"id": 301, "entity": "product"}, {"model": {"id": 301}, "entity": "offer"}]},
            preserve_order=True,
            allow_different_len=False,
        )

        # model 306 has prices from 400 to 600 in region 1; should be filtered out
        response = self.report.request_json(
            'place=prime&hid=1&mcpricefrom=650&mcpriceto=680&rids=1&allow-collapsing=1'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(response, {"results": []}, preserve_order=True, allow_different_len=False)

        # model 306 has prices from 500 to 700 in region 2
        response = self.report.request_json(
            'place=prime&hid=1&mcpricefrom=650&mcpriceto=680&rids=2&allow-collapsing=1'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "id": 306,
                        "entity": "product",
                        "prices": {"min": "655", "max": "655"},
                    }
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        _ = (
            'place=prime&hid=1&mcpricefrom=650&mcpriceto=680&allow-collapsing=1',
            'place=prime&hid=1&mcpricefrom=650&mcpriceto=680&rids=1&regset=1&allow-collapsing=1',
        )

        # no region - all offers - no delimiter
        # model 306 has prices from 400 to 700 in all regions
        response = self.report.request_json(
            'place=prime&hid=1&mcpricefrom=650&mcpriceto=680&allow-collapsing=1'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "id": 306,
                        "entity": "product",
                        "prices": {"min": "655", "max": "655"},
                    }
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # region = 1 and &regset=1 - we have regional delimiter
        # make sure the price range [400...700] is used (not [400...600]) when the user wants to see all offers (&regset=1)
        response = self.report.request_json(
            'place=prime&hid=1&mcpricefrom=650&mcpriceto=680&rids=1&regset=1&allow-collapsing=1'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {'entity': 'regionalDelimiter'},
                    {
                        "id": 306,
                        "entity": "product",
                        "prices": {"min": "655", "max": "655"},
                    },
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # make sure model 306 is not in the search results when &mcpricefrom=/&mcpriceto= is out of range [400...700]
        for query in (
            'place=prime&hid=1&mcpriceto=350&regset=1',
            'place=prime&hid=1&mcpricefrom=750&regset=1',
        ):
            response = self.report.request_json(query + '&rearr-factors=market_metadoc_search=no')
            self.assertFragmentNotIn(response, {"id": 306, "entity": "product"})

        # A model that is out of stock still should be shown...
        response = self.report.request_json('place=prime&hid=1&rids=3&rearr-factors=market_metadoc_search=no')
        self.assertFragmentIn(response, {"id": 306, "entity": "product", "prices": Absent()})

        # ...unless a price filter is active.
        for query in ('place=prime&hid=1&mcpriceto=700&rids=3', 'place=prime&hid=1&mcpricefrom=100&rids=3'):
            response = self.report.request_json(query + '&rearr-factors=market_metadoc_search=no')
            self.assertFragmentNotIn(response, {"id": 306, "entity": "product"})

    def test_qr_filter(self):
        response = self.report.request_json('place=prime&hid=1&qrfrom=5&rids=1')
        self.assertFragmentIn(
            response,
            {"results": [{"model": {"id": 301}, "entity": "offer"}]},
            preserve_order=True,
            allow_different_len=False,
        )

    def test_manufacturer_warranty_filter(self):
        # test show both offers without warranty filter
        response = self.report.request_json('place=prime&hid=1&numdoc=20')
        self.assertFragmentIn(response, {"results": [{"titles": {"raw": "Thing with warranty"}}]})
        self.assertFragmentIn(response, {"results": [{"titles": {"raw": "Thing without warranty"}}]})

        # test that manufacturer_warranty=0 is identical to absence of it
        response = self.report.request_json('place=prime&hid=1&manufacturer_warranty=0&numdoc=20')
        self.assertFragmentIn(response, {"results": [{"titles": {"raw": "Thing with warranty"}}]})
        self.assertFragmentIn(response, {"results": [{"titles": {"raw": "Thing without warranty"}}]})

        # test that if manufacturer_warranty=1 then only things with warranty are showed
        response = self.report.request_json('place=prime&hid=1&manufacturer_warranty=1&numdoc=20')
        self.assertFragmentIn(response, {"results": [{"titles": {"raw": "Thing with warranty"}}]})
        self.assertFragmentNotIn(response, {"results": [{"titles": {"raw": "Thing without warranty"}}]})

    def test_offer_shipping_filter(self):
        '''Тестируем фильтр "Спопособ доставки"
        При выборе фильтра по способу доставки из [pickup,delivery,store,postomat]
        в выдачу должны попадать только офферы доставляемые в регион пользователя данным способом
        (при этом офферы доставляемые через postomat являются подмножеством офферов доставляемых через pickup)
        даже если включена галка "Показывать предложения из других регионов" (только во внутренней сети)
        '''

        def check(flag, expected, regset=None):
            fragment = {"total": len(expected), "results": [{"titles": {"raw": title}} for title in expected]}

            # флаг regset=1 т.е. галка "Показывать предложения из других регионов" включена
            # флаг regset=2 галка выключена (значение передаваемое из прода)
            regset = regset or [1, 2]
            flag = '&offer-shipping=' + flag if flag is not None else ''
            for r, f in itertools.product(regset, (None, False, True)):
                if f is not None:  # регресим экспериментальный флаг — не должен влиять на белый фильтр
                    flag += '&rearr-factors=all_delivery_type_filter_enabled={}'.format(1 if f else 0)
                response = self.report.request_json('place=prime&hid=10&rids=1&regset={0}&{1}'.format(r, flag))
                self.assertFragmentIn(response, fragment)

        # примечание: наличие offer-shipping делает галку regset=1 не активной
        check(flag="delivery", expected=["delivery"])
        check(flag="pickup", expected=["pickup", "postomat"])
        check(flag="postomat", expected=["postomat"])
        check(flag="store", expected=["store"])
        check(flag="delivery,pickup", expected=["delivery", "pickup", "postomat"])
        check(flag="delivery,pickup,postomat", expected=["delivery", "pickup", "postomat"])
        check(flag="pickup,postomat", expected=["pickup", "postomat"])
        check(flag="store,pickup", expected=["store", "pickup", "postomat"])
        check(flag="delivery,store", expected=["delivery", "store"])
        check(flag="delivery,store,pickup", expected=["delivery", "store", "pickup", "postomat"])

        # пустой флаг или что-то что не парсится должным образом выфильтровывает вообще все офферы
        check(flag="", expected=["model"])
        check(flag="bla-bla-bla", expected=["model"])

        # секретный флаг model отфильтровывающий все офферы
        check(flag="model", expected=["model"])

        # без offer-shipping выдача зависит от параметра regset (при regset=1 показываются даже не доставляемые офферы)
        check(flag=None, regset=[1], expected=["model", "delivery", "pickup", "store", "postomat", "not delivered"])
        check(flag=None, regset=[2], expected=["model", "delivery", "pickup", "store", "postomat"])

    def test_onstock_filter(self):

        expected_with_not_on_stock = {
            "search": {
                "total": 5,
                "results": [
                    {"entity": "offer", "model": {"id": 305}},
                    {"entity": "offer", "model": {"id": 303}},
                    {"entity": "product", "id": 305, "offers": {"count": 1}},
                    {"entity": "product", "id": 303, "offers": {"count": 1}},
                    {"entity": "regionalDelimiter"},
                    # модель не имеет локальных офферов в регионе
                    {"entity": "product", "id": 302, "offers": {"count": 0}},
                ],
            }
        }
        expected_onstock_filter_do_not_checked = {
            "filters": [
                {
                    "id": "onstock",
                    "values": [{"value": "0", "checked": True}, {"value": "1", "checked": NoKey("checked")}],
                }
            ]
        }

        response = self.report.request_json('place=prime&hid=4&rids=1')
        self.assertFragmentIn(response, expected_with_not_on_stock, allow_different_len=False)
        self.assertFragmentIn(response, expected_onstock_filter_do_not_checked, allow_different_len=True)

        response = self.report.request_json('place=prime&hid=4&onstock=0&rids=1')
        self.assertFragmentIn(response, expected_with_not_on_stock, allow_different_len=False)
        self.assertFragmentIn(response, expected_onstock_filter_do_not_checked, allow_different_len=True)

        response = self.report.request_json('place=prime&hid=4&onstock=&rids=1')
        self.assertFragmentIn(response, expected_with_not_on_stock, allow_different_len=False)
        self.assertFragmentIn(response, expected_onstock_filter_do_not_checked, allow_different_len=True)

        response = self.report.request_json('place=prime&hid=4&onstock=1&rids=1')
        self.assertFragmentIn(
            response,
            {
                "search": {"total": 4},
                "filters": [
                    {
                        "id": "onstock",
                        "type": "boolean",
                        "name": u"В продаже",
                        "kind": 2,
                        "hasBoolNo": Absent(),
                        "values": [
                            {"initialFound": 1, "value": "0"},
                            {"initialFound": 4, "checked": True, "found": 4, "value": "1"},
                        ],
                    }
                ],
            },
        )

        self.assertFragmentNotIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "id": 302,
                    }
                ]
            },
        )

    def test_onstock_filter_shown_always(self):
        '''Когда показывается галка "В продаже" - всегда'''

        # когда нашлись модели в продаже и не в продаже
        response = self.report.request_json('place=prime&text=search+me')
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {'id': 306, 'offers': {'count': 6}},
                        {'id': 1000000101, 'offers': {'count': 1}},
                        {'id': 302, 'offers': {'count': 0}},
                    ]
                }
            },
            allow_different_len=False,
        )
        self.assertFragmentIn(response, {'filters': [{'id': 'onstock'}]})

        # когда модели не в продаже были отфильтрованы
        response = self.report.request_json('place=prime&text=search+me&onstock=1')
        self.assertFragmentIn(
            response,
            {'search': {'results': [{'id': 306, 'offers': {'count': 6}}, {'id': 1000000101, 'offers': {'count': 1}}]}},
            allow_different_len=False,
        )
        self.assertFragmentIn(response, {'filters': [{'id': 'onstock'}]})

        # когда нашлись модели не в продаже (даже если только не в продаже)
        response = self.report.request_json('place=prime&text=search+me&hid=4')
        self.assertFragmentIn(
            response, {'search': {'results': [{'id': 302, 'offers': {'count': 0}}]}}, allow_different_len=False
        )
        self.assertFragmentIn(response, {'filters': [{'id': 'onstock'}]})

        # когда нашлись модели только в продаже
        response = self.report.request_json('place=prime&text=search+me&hid=1')
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {'id': 306, 'offers': {'count': 6}},
                    ]
                }
            },
            allow_different_len=False,
        )
        self.assertFragmentIn(response, {'filters': [{'id': 'onstock'}]})

        # когда галка onstock включена (даже если все отфильтровалось)
        response = self.report.request_json('place=prime&text=search+me&hid=4&onstock=1')
        self.assertFragmentIn(response, {'search': {'results': EmptyList()}}, allow_different_len=False)
        self.assertFragmentIn(response, {'filters': [{'id': 'onstock'}]})

        # если в выдаче только оффер - фильтр тоже показывается
        response = self.report.request_json('place=prime&hid=259&text=ooops')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "offer", "titles": {"raw": "OOOPS"}},
                ]
            },
            allow_different_len=False,
        )

        self.assertFragmentIn(response, {"filters": [{"id": "onstock"}]})

    def test_onstock_hides_models_without_offers(self):
        # делаем запрос без фильтра, смотрим, что показалась как модель с офферами, так и без них
        response = self.report.request_json('place=prime&hid=4')
        self.assertFragmentIn(
            response,
            [
                {"entity": "product", "id": 303},
                {"entity": "product", "id": 302},
            ],
        )

        # делаем запрос с фильтром, смотрим, что моделька без офферов не показалась, а с офферами показалась
        response = self.report.request_json('place=prime&hid=4&onstock=1')
        self.assertFragmentIn(response, {"entity": "product", "id": 303})

        self.assertFragmentNotIn(response, {"entity": "product", "id": 302})

    def test_onstock_vclusters_in_text_search(self):
        '''MARKETOUT-15933
        при текстовом поиске под флагом market_not_filter_out_vclusters_not_onstock=1
        кластера не в продаже не скрываются даже если не выставлен onstock=1
        (фильтрация кластеров не в продаже происходит только на базовых)
        '''

        # по запросу [search me] находятся 2 модели и 2 кластера
        response = self.report.request_json(
            'place=prime&text=search+me&rids=1&local-offers-first=0&rearr-factors=market_not_filter_out_vclusters_not_onstock=1'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 4,
                    'results': [
                        {'id': 306, 'offers': {'count': 3}},
                        {'id': 1000000101, 'offers': {'count': 1}},
                        {'id': 302, 'offers': {'count': 0}},
                        {'id': 1000000102, 'offers': {'count': 0}},
                    ],
                }
            },
            allow_different_len=False,
        )

        # по запросу [search me] c флагом находятся 2 модели и 1 кластер (который был в продаже)
        response = self.report.request_json(
            'place=prime&text=search+me&rids=1&local-offers-first=0' '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 3,
                    'results': [
                        {'id': 306, 'offers': {'count': 3}},
                        {'id': 1000000101, 'offers': {'count': 1}},
                        {'id': 302, 'offers': {'count': 0}},
                    ],
                }
            },
            allow_different_len=False,
        )

        # по запросу [search me] c флагом и с onstock=1 находятся только модели и кластера в продаже
        response = self.report.request_json(
            'place=prime&text=search+me&rids=1&local-offers-first=0&onstock=1' '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 2,
                    'results': [
                        {'id': 306, 'offers': {'count': 3}},
                        {'id': 1000000101, 'offers': {'count': 1}},
                    ],
                }
            },
            allow_different_len=False,
        )

        # флаг не действует на бестекстовых запросах
        # в категории hid=100 найдется 2 кластера
        response = self.report.request_json(
            'place=prime&hid=100&rids=1&local-offers-first=0&allow-collapsing=1'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 2,
                    'results': [{'id': 1000000101, 'offers': {'count': 1}}, {'id': 1000000102, 'offers': {'count': 0}}],
                }
            },
            allow_different_len=False,
        )

    def test_adult_filter(self):
        response = self.report.request_json('place=prime&hid=2&adult=1&rids=1')
        self.assertFragmentIn(
            response,
            {"results": [{"id": 501, "entity": "product"}, {"model": {"id": 501}, "entity": "offer"}]},
            preserve_order=True,
            allow_different_len=False,
        )

    def test_cpa_filter(self):
        response = self.report.request_json('place=prime&hid=1&rids=1&cpa=real&debug=da')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "results": [
                        {
                            "model": {"id": 301},
                            "entity": "offer",
                            "cpa": "real",
                        }
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        self.assertFragmentIn(
            response,
            {
                'debug': {
                    'report': {
                        'context': {
                            'collections': {
                                "SHOP": {'text': [Contains('blue_doctype:"b" | blue_doctype:"w_cpa"')]},
                                "SHOP_BLUE": NoKey("SHOP_BLUE"),
                            }
                        }
                    }
                }
            },
        )

    def test_delivery_filter(self):
        '''Тестируем фильтр по времени доставки
        останется только один оффер от модели с интервалом доставки до 2х дней
        '''
        response = self.report.request_json('place=prime&hid=1&rids=1&delivery_interval=2')
        self.assertEqual(response.count({"model": {"id": 301}, "entity": "offer"}), 1)

    def test_price_filter_output(self):
        response = self.report.request_json('place=prime&hid=1&mcpricefrom=100&mcpriceto=150&rids=1')
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": "glprice",
                        "type": "number",
                        "values": [
                            {"max": "150", "min": "100", "id": "chosen"},
                            {"max": "600", "min": "100", "initialMax": "600", "initialMin": "100", "id": "found"},
                        ],
                    }
                ]
            },
            preserve_order=True,
        )

    @skip('because of offer collapsing into vcluster, see MARKETOUT-12117')
    def test_home_region_filter(self):
        response = self.report.request_json('place=prime&hid=1&home_region_filter=225&rids=1')
        self.assertFragmentIn(
            response,
            {"results": [{"model": {"id": 301}, "entity": "offer"}, {"model": {"id": 301}, "entity": "offer"}]},
            preserve_order=True,
        )

    def test_qr_filter_output(self):
        response = self.report.request_json('place=prime&hid=1&qrfrom=4&rids=1')
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {"id": "qrfrom", "type": "boolean", "values": [{"value": "4", "checked": True}, {"value": "3"}]}
                ]
            },
        )

    def test_manufacturer_warranty_output(self):
        response = self.report.request_json('place=prime&hid=1&manufacturer_warranty=1&rids=1')
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": "manufacturer_warranty",
                        "type": "boolean",
                        "values": [{"value": "1", "checked": True}, {"value": "0"}],
                    }
                ]
            },
        )

    def test_offer_shipping_output_order_of_filters(self):
        response = self.report.request_json('place=prime&hid=1&offer-shipping=store')
        self.assertFragmentIn(
            response,
            {
                "id": "offer-shipping",
                "type": "boolean",
                "values": [{"value": "delivery"}, {"value": "pickup"}, {"value": "store"}],
            },
            preserve_order=True,
        )
        self.assertFragmentNotIn(
            response,
            {
                "filters": [
                    {
                        "id": "offer-shipping",
                        "values": [
                            {"value": "postomat"},
                        ],
                    }
                ]
            },
        )

    def test_offer_shipping_output_delivery_checked(self):
        response = self.report.request_json('place=prime&hid=1&offer-shipping=delivery')
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": "offer-shipping",
                        "type": "boolean",
                        "values": [
                            {"value": "delivery", "checked": True},
                            {"value": "pickup", "checked": Absent()},
                        ],
                    }
                ]
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                "filters": [
                    {
                        "id": "offer-shipping",
                        "values": [
                            {"value": "postomat"},
                        ],
                    }
                ]
            },
        )

    def test_offer_shipping_output_pickup_checked(self):
        response = self.report.request_json('place=prime&hid=1&offer-shipping=pickup&rids=1')
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": "offer-shipping",
                        "values": [
                            {"value": "delivery", "checked": Absent()},
                            {"value": "pickup", "checked": True},
                        ],
                    }
                ]
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                "filters": [
                    {
                        "id": "offer-shipping",
                        "values": [
                            {"value": "postomat"},
                        ],
                    }
                ]
            },
        )

    def test_offer_shipping_output_store_checked(self):
        response = self.report.request_json('place=prime&hid=1&offer-shipping=store&rids=1')
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": "offer-shipping",
                        "values": [
                            {"value": "delivery", "checked": Absent(), "initialFound": 5},
                            {"value": "store", "checked": True, "initialFound": 0},
                        ],
                    }
                ]
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                "filters": [
                    {
                        "id": "offer-shipping",
                        "values": [
                            {"value": "postomat"},
                        ],
                    }
                ]
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                "filters": [
                    {
                        "id": "offer-shipping",
                        "values": [
                            {"value": "pickup"},
                        ],
                    }
                ]
            },
        )

    def test_offer_shipping_output_postomat_checked(self):
        response = self.report.request_json('place=prime&hid=1&offer-shipping=postomat&rids=1')
        self.assertFragmentNotIn(
            response,
            {
                "filters": [
                    {
                        "id": "offer-shipping",
                        "values": [
                            {"value": "postomat"},
                        ],
                    }
                ]
            },
        )

    def test_onstock_filter_output(self):
        response = self.report.request_json("place=prime&hid=1&onstock=1&rids=1&allow-collapsing=1")
        # onstock=1 - 2 оффера от модели 301, 3 оффера от модели 306, модели 301 и 306
        # onstock=0 - пустая модель 3001
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": "onstock",
                        "type": "boolean",
                        "values": [
                            {"value": "1", "initialFound": 7, "checked": True},
                            {"value": "0", "initialFound": 1},
                        ],
                    }
                ]
            },
        )

    def test_cpa_filter_output(self):
        response = self.report.request_json('place=prime&hid=3&rids=1')
        self.assertFragmentIn(
            response,
            {"filters": [{"id": "cpa", "type": "boolean", "values": [{"value": "1", "found": 1}, {"value": "0"}]}]},
        )

        response = self.report.request_json('place=prime&hid=3&rids=1&cpa=real')
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": "cpa",
                        "type": "boolean",
                        "values": [{"value": "1", "found": 1, "checked": True}, {"value": "0"}],
                    }
                ]
            },
        )

    @classmethod
    def prepare_delivery_interval_filter_output(cls):
        cls.index.hypertree += [HyperCategory(hid=9000)]
        cls.index.models += [Model(hyperid=80000, hid=9000)]
        cls.index.shops += [Shop(fesh=700100, priority_region=1)]
        cls.index.offers += [
            Offer(fesh=700100, hyperid=80000, delivery_options=[DeliveryOption(day_to=0)]),
            Offer(fesh=700100, hyperid=80000, delivery_options=[DeliveryOption(day_to=1)]),
            Offer(fesh=700100, hyperid=80000, delivery_options=[DeliveryOption(day_to=5)]),
            Offer(fesh=700100, hyperid=80000, delivery_options=[DeliveryOption(day_to=15)]),
        ]

    def test_delivery_interval_filter_output(self):
        response = self.report.request_json('place=prime&hid=9000&rids=1&delivery_interval=1')
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": "delivery-interval",
                        "type": "boolean",
                        "hasBoolNo": True,
                        "values": [{"value": "1", "checked": True}, {"value": "5"}, {"value": "0"}],
                    }
                ]
            },
        )

    def test_free_delivery_filter_output(self):
        response = self.report.request_json('place=prime&hid=1&rids=1')
        self.assertFragmentIn(
            response, {"filters": [{"id": "free-delivery", "type": "boolean", "values": [{"value": "1"}]}]}
        )

    def test_some_filters_order_on_white_prime(self):
        response = self.report.request_json('place=prime&hid=1&rids=1')
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {"id": "fastest-delivery-5"},
                    {"id": "delivery-interval"},
                    {"id": "glprice"},
                    {"id": "at-beru-warehouse"},
                    {'id': 'with-yandex-delivery'},
                    {"id": "cpa"},
                    {"id": "manufacturer_warranty"},
                    {"id": "onstock"},
                    {"id": "qrfrom"},
                    {"id": "free-delivery"},
                    {"id": "offer-shipping"},
                    {"id": "fesh"},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_shops_filter_output(self):
        response = self.report.request_json('place=prime&hid=1&rids=1&fesh=204')
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {"id": "fesh", "type": "enum", "values": [{"id": "204", "checked": True, "found": 2}]}  # really 1
                ]
            },
        )

    def test_shops_filter_sorting(self):
        # Only the shops with even numbers should be present in the output, sorted alphabetically.
        for query in (
            'place=prime&hid=42&rids=1&text=iphone&show-shops=top',
            'place=prime&hid=42&rids=1&text=iphone&show-shops=all',
        ):
            response = self.report.request_json(query)
            self.assertFragmentIn(
                response,
                {
                    "id": "fesh",
                    "values": [
                        {"value": '"4точки-СПб"', "found": 8, "checked": Absent()},
                        {"value": "APPLE-RU", "found": 2, "checked": Absent()},
                        {"value": "gbstore.ru", "found": 6, "checked": Absent()},
                        {"value": "iCases.RU", "found": 10, "checked": Absent()},
                        {"value": "iCult.ru", "found": 12, "checked": Absent()},
                        {"value": "Pleer.ru", "found": 14, "checked": Absent()},
                        {"value": "Web-Computers", "found": 4, "checked": Absent()},
                        {"value": "САЙДЕКС Гипермаркет", "found": 16, "checked": Absent()},
                    ],
                },
                preserve_order=True,
                allow_different_len=False,
            )

        # Check some shops and ensure the relative order is preserved.
        response = self.report.request_json('place=prime&hid=42&rids=1&text=iphone&fesh=2002&fesh=2004&fesh=2006')
        self.assertFragmentIn(
            response,
            {
                "id": "fesh",
                "values": [
                    {"value": "APPLE-RU", "found": 4, "checked": True},  # really 2 duplicated in stats
                    {"value": "gbstore.ru", "found": 12, "checked": True},  # really 6 duplicated in stats
                    {"value": "Web-Computers", "found": 8, "checked": True},  # really 4 duplicated in stats
                    {"value": '"4точки-СПб"', "found": 8, "checked": Absent()},
                    {"value": "iCases.RU", "found": 10, "checked": Absent()},
                    {"value": "iCult.ru", "found": 12, "checked": Absent()},
                    {"value": "Pleer.ru", "found": 14, "checked": Absent()},
                    {"value": "САЙДЕКС Гипермаркет", "found": 16, "checked": Absent()},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # Only most popular shops, sorted alphabetically.
        response = self.report.request_json('place=prime&hid=42&rids=1&show-shops=top')
        self.assertFragmentIn(
            response,
            {
                "id": "fesh",
                "values": [
                    {"value": '"4точки-СПб"', "found": 8, "checked": Absent()},
                    {"value": "gbstore.ru", "found": 6, "checked": Absent()},
                    {"value": "Gosso Design", "found": 11, "checked": Absent()},
                    {"value": "iCases.RU", "found": 10, "checked": Absent()},
                    {"value": "iCult.ru", "found": 12, "checked": Absent()},
                    {"value": "Je-Store.ru", "found": 15, "checked": Absent()},
                    {"value": "Pleer.ru", "found": 14, "checked": Absent()},
                    {"value": "UppStore", "found": 9, "checked": Absent()},
                    {"value": "БайМобайл", "found": 17, "checked": Absent()},
                    {"value": "ДешевлеВсего.ru", "found": 7, "checked": Absent()},
                    {"value": "САЙДЕКС Гипермаркет", "found": 16, "checked": Absent()},
                    {"value": "СВЯЗНОЙ", "found": 13, "checked": Absent()},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # Check some shops and ensure the relative order is preserved.
        response = self.report.request_json('place=prime&hid=42&rids=1&show-shops=top&fesh=2007&fesh=2008&fesh=2009')
        self.assertFragmentIn(
            response,
            {
                "id": "fesh",
                "values": [
                    {"value": '"4точки-СПб"', "found": 16, "checked": True},  # really 8
                    {"value": "UppStore", "found": 18, "checked": True},  # really 9
                    {"value": "ДешевлеВсего.ru", "found": 14, "checked": True},  # really 7
                    {"value": "gbstore.ru", "found": 6, "checked": Absent()},
                    {"value": "Gosso Design", "found": 11, "checked": Absent()},
                    {"value": "iCases.RU", "found": 10, "checked": Absent()},
                    {"value": "iCult.ru", "found": 12, "checked": Absent()},
                    {"value": "Je-Store.ru", "found": 15, "checked": Absent()},
                    {"value": "Pleer.ru", "found": 14, "checked": Absent()},
                    {"value": "БайМобайл", "found": 17, "checked": Absent()},
                    {"value": "САЙДЕКС Гипермаркет", "found": 16, "checked": Absent()},
                    {"value": "СВЯЗНОЙ", "found": 13, "checked": Absent()},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # Two groups of shops' "4точки-СПб"'..СВЯЗНОЙ (popular shops) and 003.ru...ТехноГид (not so popular). Both are sorted alphabetically.
        for query in ('place=prime&hid=42&rids=1&show-shops=all', 'place=prime&hid=42&rids=1'):
            response = self.report.request_json(query)
            self.assertFragmentIn(
                response,
                {
                    "id": "fesh",
                    "values": [
                        {"value": '"4точки-СПб"', "found": 8, "checked": Absent()},
                        {"value": "gbstore.ru", "found": 6, "checked": Absent()},
                        {"value": "Gosso Design", "found": 11, "checked": Absent()},
                        {"value": "iCases.RU", "found": 10, "checked": Absent()},
                        {"value": "iCult.ru", "found": 12, "checked": Absent()},
                        {"value": "Je-Store.ru", "found": 15, "checked": Absent()},
                        {"value": "Pleer.ru", "found": 14, "checked": Absent()},
                        {"value": "UppStore", "found": 9, "checked": Absent()},
                        {"value": "БайМобайл", "found": 17, "checked": Absent()},
                        {"value": "ДешевлеВсего.ru", "found": 7, "checked": Absent()},
                        {"value": "САЙДЕКС Гипермаркет", "found": 16, "checked": Absent()},
                        {"value": "СВЯЗНОЙ", "found": 13, "checked": Absent()},
                        {"value": "003.ru", "found": 5, "checked": Absent()},
                        {"value": "APPLE-RU", "found": 2, "checked": Absent()},
                        {"value": "Web-Computers", "found": 4, "checked": Absent()},
                        {"value": "Ах-Подарки", "found": 1, "checked": Absent()},
                        {"value": "ТехноГид", "found": 3, "checked": Absent()},
                    ],
                },
                preserve_order=True,
                allow_different_len=False,
            )

        # Check some shops and ensure the relative order is preserved.
        for query in (
            'place=prime&hid=42&rids=1&show-shops=all&fesh=2003&fesh=2004&fesh=2007',
            'place=prime&hid=42&rids=1&fesh=2003&fesh=2004&fesh=2007',
        ):
            response = self.report.request_json(query)
            self.assertFragmentIn(
                response,
                {
                    "id": "fesh",
                    "values": [
                        {"value": "Web-Computers", "found": 8, "checked": True},  # really 4
                        {"value": "ДешевлеВсего.ru", "found": 14, "checked": True},  # really 7
                        {"value": "ТехноГид", "found": 6, "checked": True},  # really 3
                        {"value": '"4точки-СПб"', "found": 8, "checked": Absent()},
                        {"value": "Gosso Design", "found": 11, "checked": Absent()},
                        {"value": "iCases.RU", "found": 10, "checked": Absent()},
                        {"value": "iCult.ru", "found": 12, "checked": Absent()},
                        {"value": "Je-Store.ru", "found": 15, "checked": Absent()},
                        {"value": "Pleer.ru", "found": 14, "checked": Absent()},
                        {"value": "UppStore", "found": 9, "checked": Absent()},
                        {"value": "БайМобайл", "found": 17, "checked": Absent()},
                        {"value": "САЙДЕКС Гипермаркет", "found": 16, "checked": Absent()},
                        {"value": "СВЯЗНОЙ", "found": 13, "checked": Absent()},
                        {"value": "003.ru", "found": 5, "checked": Absent()},
                        {"value": "APPLE-RU", "found": 2, "checked": Absent()},
                        {"value": "gbstore.ru", "found": 6, "checked": Absent()},
                        {"value": "Ах-Подарки", "found": 1, "checked": Absent()},
                    ],
                },
                preserve_order=True,
                allow_different_len=False,
            )

        # Make sure we correctly compare shop names starting from a number.
        response = self.report.request_json('place=prime&hid=43&rids=1')
        self.assertFragmentIn(
            response,
            {
                "id": "fesh",
                "values": [
                    {"value": "2case.mobi"},
                    {"value": "21vek.by"},
                    {"value": "24shop.by"},
                    {"value": "220 ВОЛЬТ-ВН"},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_filter_output_no_hid(self):
        # offer with price 300 cannot be delivered, e.g. initial = 200.
        response = self.report.request_json('place=prime&text="tovar 1"&rids=1&hyperid=301')
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": "glprice",
                        "type": "number",
                        "values": [
                            {"max": "200", "min": "200", "initialMax": "200", "initialMin": "200", "id": "found"}
                        ],
                    }
                ]
            },
        )

    def test_filter_output_no_chosen_price(self):
        '''
        Проверяем, что при запросе без фильтра по цене нет блока 'chosen'
        '''
        response = self.report.request_json('place=prime&text="tovar 1"&rids=1')
        self.assertFragmentNotIn(
            response, {"filters": [{"id": "glprice", "type": "number", "values": [{"id": "chosen"}]}]}
        )

    # MARKETOUT-9603
    def test_no_vendor_recommended_filter(self):
        response = self.report.request_json('place=prime&text="tovar 1"&rids=1')
        self.assertFragmentNotIn(response, {"search": {}, "filters": [{"id": "vendor-recommended"}]})

    # MARKETOUT-9603
    def test_filter_discount_only_filter_stats(self):
        # we have https://a.yandex-team.ru/arc/trunk/arcadia/market/report/lite/test_glparams.py?rev=3316953&blame=true#L1705 instead
        response = self.report.request_json('place=prime&text="tovar 1"&rids=1')
        self.assertFragmentNotIn(response, {"search": {}, "filters": [{"id": "filter-discount-only"}]})

    # MARKETOUT-9700
    # Нет CPA-предложений вообще
    def test_no_cpa_filter(self):
        response = self.report.request_json('place=prime&hyperid=302')
        self.assertFragmentNotIn(response, {"filters": [{"id": "cpa"}]})

    # Есть только CPA-предложения (фильтра быть не должно, см. MARKETOUT-8189)
    def test_no_cpa_filter_on_cpa_only_output(self):
        response = self.report.request_json('place=prime&hyperid=301&fesh=205')
        self.assertFragmentNotIn(response, {"filters": [{"id": "cpa"}]})

    @classmethod
    def prepare_request_min_price_filter_stats(cls):
        """
        Создаем две модели в категории 21. Перва модель доставляется
        в Мск и Екб, вторая - только в Мск
        При этом у второй модели не будет статистик в регионе 54 (Екб)
        """
        cls.index.regiontree += [Region(rid=54, name='Екатеринбург'), Region(rid=213, name='Москва')]

        cls.index.shops += [
            Shop(fesh=221, priority_region=213, regions=[54]),
            Shop(fesh=222, priority_region=213),
        ]

        cls.index.models += [
            Model(hyperid=307, hid=21),
            Model(hyperid=308, hid=21),
        ]

        cls.index.offers += [
            Offer(hyperid=307, fesh=221, price=100),
            Offer(hyperid=307, fesh=221, price=200),
            Offer(hyperid=308, fesh=222, price=300),
            Offer(hyperid=308, fesh=222, price=400),
        ]

    def test_request_min_price_filter_stats(self):
        """
        Что тестируем: модель, для которой в данном регионе нет
        статистик, не влияет на позапросную статистику цен

        Делаем запрос за двумя моделями категориии 21 в регионе 213.
        В этом регионе есть статистики для обоих моделей. Проверяем
        значения glprice на выдаче

        Делаем запрос за теми же моделями в регионе 54.
        В этом регионе нет статистик для модели 308. Проверяем, что
        значения glprice построены по статистикам первой модели и в
        initialMin не 0

        Делаем запрос за теми же моделями в регионе 54 с указанием фильтра по цене
        В этом регионе нет статистик для модели 308. Проверяем, что
        значения glprice построены по статистикам первой модели и в
        initialMin не 0. Проверяем значения в chosen
        """

        # Запрос в регионе 213
        response = self.report.request_json('place=prime&hid=21&rids=213')
        self.assertFragmentIn(
            response,
            {
                "id": "glprice",
                "values": [{"max": "400", "initialMax": "400", "initialMin": "100", "min": "100", "id": "found"}],
            },
        )

        # Запрос в регионе 54 без фильтров
        response = self.report.request_json('place=prime&hid=21&rids=54')
        self.assertFragmentIn(
            response,
            {
                "id": "glprice",
                "values": [{"max": "200", "initialMax": "200", "initialMin": "100", "min": "100", "id": "found"}],
            },
        )

        # Запрос в регионе 54 с фильтрами
        response = self.report.request_json('place=prime&hid=21&rids=54&mcpricefrom=50&mcpriceto=450')
        self.assertFragmentIn(
            response,
            {
                "id": "glprice",
                "values": [
                    {"max": "450", "min": "50", "id": "chosen"},
                    {"max": "200", "initialMax": "200", "initialMin": "100", "min": "100", "id": "found"},
                ],
            },
        )

    @classmethod
    def prepare_request_price_filter_stats(cls):
        """
        Создаем восемь офферов в одной модели с разными ценами,
        четыре из которых доступны по BookNow
        """
        cls.index.shops += [
            Shop(fesh=231, priority_region=213, pickup_buckets=[5003]),
            Shop(fesh=232, priority_region=213, pickup_buckets=[5004]),
            Shop(fesh=233, priority_region=213, pickup_buckets=[5005]),
            Shop(fesh=234, priority_region=213, pickup_buckets=[5006]),
        ]

        cls.index.outlets += [
            Outlet(point_id=531, fesh=231, region=213, point_type=Outlet.FOR_STORE),
            Outlet(point_id=532, fesh=232, region=213, point_type=Outlet.FOR_STORE),
            Outlet(point_id=533, fesh=233, region=213, point_type=Outlet.FOR_STORE),
            Outlet(point_id=534, fesh=234, region=213, point_type=Outlet.FOR_STORE),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5003,
                fesh=231,
                carriers=[99],
                options=[PickupOption(outlet_id=531)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5004,
                fesh=232,
                carriers=[99],
                options=[PickupOption(outlet_id=532)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5005,
                fesh=233,
                carriers=[99],
                options=[PickupOption(outlet_id=533)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5006,
                fesh=234,
                carriers=[99],
                options=[PickupOption(outlet_id=534)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        BOOKING1 = BookingAvailability(outlet_id=531, region_id=213, amount=15)
        BOOKING2 = BookingAvailability(outlet_id=532, region_id=213, amount=15)
        BOOKING3 = BookingAvailability(outlet_id=533, region_id=213, amount=15)
        BOOKING4 = BookingAvailability(outlet_id=534, region_id=213, amount=15)

        cls.index.offers += [
            Offer(hyperid=309, fesh=231, price=100),
            Offer(hyperid=309, fesh=232, price=200),
            Offer(hyperid=309, fesh=233, price=310),
            Offer(hyperid=309, fesh=234, price=410),
            Offer(hyperid=309, fesh=231, price=110, booking_availabilities=[BOOKING1]),
            Offer(hyperid=309, fesh=232, price=210, booking_availabilities=[BOOKING2]),
            Offer(hyperid=309, fesh=233, price=300, booking_availabilities=[BOOKING3]),
            Offer(hyperid=309, fesh=234, price=400, booking_availabilities=[BOOKING4]),
        ]

    def test_request_price_filter_stats(self):
        """
        Что тестируем: применение одного фильтра по цене не оказывает
        влияния на позапросные статистики. Применение фильтра по цене
        с каким-то другим фильтром (в данном случае BookNow) оставляет
        в позапросных статистиках только те офферы, которые подходят
        под этот другой фильтр

        Делаем запрос за офферами по цене от 200 до 350 рублей
        Ожидаем, что на выдаче 4 оффера, а в позапросных статистиках
        мин. цена 100 и макс. 410 рублей

        Делаем запрос за офферами по цене от 200 до 350 рублей,
        доступных по BookNow
        Ожидаем, что на выдаче 2 оффера, а в позапросных статистиках
        минимальная цена 110 и максимальная 400 рублей с учетом фильтров
        и минимум 100 максимум 410 без учета фильтров (initialMin/initialMax)
        """

        response = self.report.request_json('place=prime&hyperid=309&rids=213&mcpricefrom=200&mcpriceto=350')
        self.assertFragmentIn(response, {"search": {"total": 4}})
        self.assertFragmentIn(
            response,
            {
                "id": "glprice",
                "values": [{"max": "410", "initialMax": "410", "initialMin": "100", "min": "100", "id": "found"}],
            },
        )

        response = self.report.request_json(
            'place=prime&hyperid=309&rids=213&mcpricefrom=200&mcpriceto=350&show-book-now-only=1'
        )
        self.assertFragmentIn(response, {"search": {"total": 2}})
        self.assertFragmentIn(
            response,
            {
                "id": "glprice",
                "values": [{"max": "400", "initialMax": "410", "initialMin": "100", "min": "110", "id": "found"}],
            },
        )

    @classmethod
    def prepare_request_home_region_filter_stats(cls):
        """
        Создаем шесть офферов в одной модели из разных стран
        с доставкой в Россию, три из офферов доступны по BookNow
        """

        cls.index.regiontree += [
            Region(rid=134, name='Китай', region_type=Region.COUNTRY, children=[Region(rid=10590, name='Пекин')]),
            Region(rid=149, name='Беларусь', region_type=Region.COUNTRY, children=[Region(rid=157, name='Минск')]),
            Region(rid=187, name='Украина', region_type=Region.COUNTRY, children=[Region(rid=143, name='Киев')]),
        ]

        cls.index.shops += [
            Shop(fesh=235, priority_region=10590, home_region=134, regions=[225], pickup_buckets=[5007]),
            Shop(fesh=236, priority_region=157, home_region=149, regions=[225], pickup_buckets=[5008]),
            Shop(fesh=237, priority_region=143, home_region=187, regions=[225], pickup_buckets=[5009]),
        ]

        cls.index.outlets += [
            Outlet(point_id=535, fesh=235, region=213, point_type=Outlet.FOR_STORE),
            Outlet(point_id=536, fesh=236, region=213, point_type=Outlet.FOR_STORE),
            Outlet(point_id=537, fesh=237, region=213, point_type=Outlet.FOR_STORE),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5007,
                fesh=235,
                carriers=[99],
                options=[PickupOption(outlet_id=535)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5008,
                fesh=236,
                carriers=[99],
                options=[PickupOption(outlet_id=536)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5009,
                fesh=237,
                carriers=[99],
                options=[PickupOption(outlet_id=537)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        BOOKING1 = BookingAvailability(outlet_id=535, region_id=213, amount=15)
        BOOKING2 = BookingAvailability(outlet_id=536, region_id=213, amount=15)
        BOOKING3 = BookingAvailability(outlet_id=537, region_id=213, amount=15)

        cls.index.offers += [
            Offer(hyperid=310, fesh=235),
            Offer(hyperid=310, fesh=236),
            Offer(hyperid=310, fesh=237),
            Offer(hyperid=310, fesh=235, booking_availabilities=[BOOKING1]),
            Offer(hyperid=310, fesh=236, booking_availabilities=[BOOKING2]),
            Offer(hyperid=310, fesh=237, booking_availabilities=[BOOKING3]),
        ]

    def test_request_home_region_filter_stats(self):
        """
        Что тестируем: применение одного фильтра по стране продавца
        не оказывает влияния на позапросные статистики. Применение
        фильтра по стране продавца с каким-то другим фильтром
        (в данном случае BookNow) оставляет в позапросных статистиках
        только те офферы, которые подходят под этот другой фильтр

        Делаем запрос за офферами из региона 134
        Ожидаем, что на выдаче 2 оффера, а в позапросных статистиках
        учтены по 2 оффера регионов 149 и 187

        Делаем запрос за офферами из региона 134, доступных по BookNow
        Ожидаем, что на выдаче 1 оффер, а в позапросных статистиках
        учтено по одному офферу регионов 149 и 187
        """

        response = self.report.request_json(
            'place=prime&hyperid=310&rids=213&home_region_filter=134&allow-collapsing=0'
        )
        self.assertFragmentIn(response, {"search": {"total": 2}})

        response = self.report.request_json(
            'place=prime&hyperid=310&rids=213&home_region_filter=134&show-book-now-only=1&allow-collapsing=0'
        )
        self.assertFragmentIn(response, {"search": {"total": 1}})

    @classmethod
    def prepare_request_qr_filter_stats(cls):
        """
        Создаем восемь офферов в одной модели из магазинов с разным рейтингом,
        четыре оффера доступны по BookNow
        """

        cls.index.shops += [
            Shop(fesh=241, new_shop_rating=NewShopRating(new_rating_total=5.0), pickup_buckets=[5010]),
            Shop(fesh=242, new_shop_rating=NewShopRating(new_rating_total=4.0), pickup_buckets=[5011]),
            Shop(fesh=243, new_shop_rating=NewShopRating(new_rating_total=3.0), pickup_buckets=[5012]),
            Shop(fesh=244, new_shop_rating=NewShopRating(new_rating_total=2.0), pickup_buckets=[5013]),
        ]

        cls.index.outlets += [
            Outlet(point_id=541, fesh=241, region=213, point_type=Outlet.FOR_STORE),
            Outlet(point_id=542, fesh=242, region=213, point_type=Outlet.FOR_STORE),
            Outlet(point_id=543, fesh=243, region=213, point_type=Outlet.FOR_STORE),
            Outlet(point_id=544, fesh=244, region=213, point_type=Outlet.FOR_STORE),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5010,
                fesh=241,
                carriers=[99],
                options=[PickupOption(outlet_id=541)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5011,
                fesh=242,
                carriers=[99],
                options=[PickupOption(outlet_id=542)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5012,
                fesh=243,
                carriers=[99],
                options=[PickupOption(outlet_id=543)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5013,
                fesh=244,
                carriers=[99],
                options=[PickupOption(outlet_id=544)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        BOOKING1 = BookingAvailability(outlet_id=541, region_id=213, amount=15)
        BOOKING2 = BookingAvailability(outlet_id=542, region_id=213, amount=15)
        BOOKING3 = BookingAvailability(outlet_id=543, region_id=213, amount=15)
        _ = BookingAvailability(outlet_id=544, region_id=213, amount=15)

        cls.index.offers += [
            Offer(hyperid=311, fesh=241),
            Offer(hyperid=311, fesh=242),
            Offer(hyperid=311, fesh=243),
            Offer(hyperid=311, fesh=244),
            Offer(hyperid=311, fesh=241, booking_availabilities=[BOOKING1]),
            Offer(hyperid=311, fesh=242, booking_availabilities=[BOOKING2]),
            Offer(hyperid=311, fesh=243, booking_availabilities=[BOOKING3]),
            Offer(hyperid=311, fesh=244, booking_availabilities=[BOOKING3]),
        ]

    def test_request_qr_filter_stats(self):
        """
        Что тестируем: применение одного фильтра по рейтингу магазина
        не оказывает влияния на позапросные статистики. Применение
        фильтра по рейтингу с каким-то другим фильтром (в данном случае
        BookNow) оставляет в позапросных статистиках только те офферы,
        которые подходят под этот другой фильтр

        Делаем запрос за офферами из магазинов рейтингом не ниже 4
        Ожидаем, что на выдаче 4 оффера, а в позапросных статистиках
        учтены офферы магазинов рейтинга 2 и 3

        Делаем запрос за офферами из магазинов рейтингом не ниже 4,
        доступных по BookNow
        Ожидаем, что на выдаче 2 оффера, а в позапросных статистиках
        учтены офферы магазинов рейтинга 2 и 3

        рейтинг считается по нарастающей системе
        found(2) >= found(3) >= found(4)
        """

        response = self.report.request_json('place=prime&hyperid=311&rids=213&qrfrom=4')
        self.assertFragmentIn(response, {"search": {"total": 4}})
        self.assertFragmentIn(
            response,
            {
                "id": "qrfrom",
                "values": [{"found": 8, "value": "2"}, {"found": 6, "value": "3"}, {"found": 4, "value": "4"}],
            },
            allow_different_len=False,
        )

        response = self.report.request_json('place=prime&hyperid=311&rids=213&qrfrom=4&show-book-now-only=1')
        self.assertFragmentIn(response, {"search": {"total": 2}})
        self.assertFragmentIn(
            response,
            {
                "id": "qrfrom",
                "values": [{"found": 4, "value": "2"}, {"found": 3, "value": "3"}, {"found": 2, "value": "4"}],
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_request_qr_filter_virtual_shop(cls):
        cls.index.shops += [
            Shop(
                fesh=11,
                datafeed_id=111,
                priority_region=213,
                fulfillment_virtual=True,
                delivery_service_outlets=[2001],
                cpa=Shop.CPA_REAL,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                work_schedule='virtual shop work schedule',
                new_shop_rating=NewShopRating(new_rating_total=2.0),
            ),
            Shop(
                fesh=252,
                datafeed_id=2520,
                priority_region=213,
                regions=[225],
                name="Магазин 1",
                supplier_type=Shop.THIRD_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
                fulfillment_program=True,
                business_fesh=1,
                new_shop_rating=NewShopRating(new_rating_total=3.0),
            ),
            Shop(
                fesh=253,
                datafeed_id=2530,
                priority_region=213,
                regions=[225],
                name="Магазин 2",
                supplier_type=Shop.THIRD_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=146,
                fulfillment_program=True,
                business_fesh=2,
                new_shop_rating=NewShopRating(new_rating_total=5.0),
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

        cls.index.mskus += [
            MarketSku(
                title='MSKU blue 1',
                sku=101,
                hyperid=2,
                blue_offers=[
                    BlueOffer(
                        fesh=252,
                        feedid=2520,
                        sku=101,
                        hyperid=2,
                        price=205,
                        title='Blue offer 1',
                    ),
                ],
            ),
            MarketSku(
                title='MSKU blue 2',
                sku=102,
                hyperid=2,
                blue_offers=[
                    BlueOffer(
                        fesh=253,
                        feedid=2530,
                        sku=102,
                        hyperid=2,
                        price=205,
                        title='Blue offer 2',
                    ),
                ],
            ),
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

    def test_request_qr_filter_virtual_shop(self):
        '''Проверяем, что с новым флагом, вместо рейтинга виртуального магазина, приходит рейтинг настоящего магазина'''
        response = self.report.request_json(
            'place=prime&hyperid=2&rids=213&qrfrom=4&rearr-factors=market_use_supplier_shop_in_shop_rating=false'
        )
        self.assertFragmentIn(response, {"search": {"total": 0}})

        response = self.report.request_json(
            'place=prime&hyperid=2&rids=213&qrfrom=4&rearr-factors=market_use_supplier_shop_in_shop_rating=true'
        )
        self.assertFragmentIn(response, {"search": {"total": 1}})

        response = self.report.request_json(
            'place=prime&hyperid=2&rids=213&qrfrom=2&rearr-factors=market_use_supplier_shop_in_shop_rating=true'
        )
        self.assertFragmentIn(response, {"search": {"total": 2}})

    @classmethod
    def prepare_price_with_gl_filters(cls):
        '''Создаем gl-типы 1-го и второго рода (цвет)
        Создаем два набора офферов, с gl-параметрами Вес (3 штуки) и Цвет (3 штуки)
        Каждый gl-параметр представлен в трех ценах (базовая цена + 10, 20 и 30)
        '''
        RED, GREEN, BLUE = 1, 2, 3
        cls.index.gltypes += [
            GLType(
                param_id=201,
                hid=31,
                gltype=GLType.ENUM,
                values=[RED, GREEN, BLUE],
                unit_name="Color",
                cluster_filter=True,
            ),
            GLType(param_id=202, hid=32, gltype=GLType.NUMERIC, unit_name="Weight"),
        ]

        for price_add in [10, 20, 30]:
            cls.index.offers += [
                Offer(
                    title="Red offer",
                    hyperid=321,
                    hid=31,
                    price=100 + price_add,
                    glparams=[
                        GLParam(param_id=201, value=RED),
                    ],
                ),
                Offer(
                    title="Green offer",
                    hyperid=322,
                    hid=31,
                    price=200 + price_add,
                    glparams=[
                        GLParam(param_id=201, value=GREEN),
                    ],
                ),
                Offer(
                    title="Blue offer",
                    hyperid=323,
                    hid=31,
                    price=300 + price_add,
                    glparams=[
                        GLParam(param_id=201, value=BLUE),
                    ],
                ),
                Offer(
                    title="Light offer",
                    hyperid=324,
                    hid=32,
                    price=100 + price_add,
                    glparams=[
                        GLParam(param_id=202, value=500 + price_add),
                    ],
                ),
                Offer(
                    title="Medium offer",
                    hyperid=325,
                    hid=32,
                    price=200 + price_add,
                    glparams=[
                        GLParam(param_id=202, value=1500 + price_add),
                    ],
                ),
                Offer(
                    title="Heavy offer",
                    hyperid=326,
                    hid=32,
                    price=300 + price_add,
                    glparams=[
                        GLParam(param_id=202, value=10000 + price_add),
                    ],
                ),
            ]

    def test_price_with_gl_filters(self):
        '''Что тестируем: ваимодействие фильтров по цене и gl-фильтров первого
        и второго рода и влияние их на статистики

        Задаем запросы с указанием gl-фильтров и фильтра по цене. Проверяем
        соответствие значений в позапросных статистиках для этих фильтров
        '''
        # Запрос красных офферов с ценой от 120 до 300
        # В статистиках по цене
        #   в initialMin и initialMax - все офферы (от 110 до 330)
        #   в min и max - только красные, от 110 до 130
        # В статистиках по gl-фильтру Color
        #   в initial-found ожидаем увидеть все офферы,
        #   в found - только подходящие по цене (два красных, по цене 120 и 130
        #   и три зеленых, по ценам от 210 до 230, ни одного синего)
        response = self.report.request_json('place=prime&hid=31&glfilter=201:1&mcpricefrom=120&mcpriceto=300')
        self.assertFragmentIn(
            response,
            {
                'filters': [
                    {
                        'id': 'glprice',
                        'values': [
                            {"max": "130", "initialMax": "330", "initialMin": "110", "min": "110", "id": "found"}
                        ],
                    },
                    {
                        'id': '201',
                        'values': [
                            {"initialFound": 6, "found": 4, "checked": True, "id": "1"},  # really 3  # really 2
                            {"initialFound": 3, "found": 3, "id": "2"},  # really 3  # really 3
                            {"initialFound": 3, "found": 0, "id": "3"},
                        ],
                    },
                ]
            },
        )

        # Запрос средних по весу офферов с ценой от 120 до 300
        # В статистиках по цене
        #   в initialMin и initialMax - все офферы (от 110 до 330)
        #   в min и max - только средние по цене, от 210 до 230
        # В статистиках по gl-фильтру Weight
        #   в initialMin/initialMax ожидаем увидеть все офферы (от 510 до 10030),
        #   в min/max - только подходящие по цене (от 520 до 1530)
        response = self.report.request_json('place=prime&hid=32&glfilter=202:700,2000&mcpricefrom=120&mcpriceto=300')
        self.assertFragmentIn(
            response,
            {
                'filters': [
                    {
                        'id': 'glprice',
                        'values': [
                            {"max": "230", "initialMax": "330", "initialMin": "110", "min": "210", "id": "found"}
                        ],
                    },
                    {
                        'id': '202',
                        'values': [
                            {"max": "1530", "initialMax": "10030", "initialMin": "510", "min": "520", "id": "found"}
                        ],
                    },
                ]
            },
        )

    def test_filters_hiding(self):
        """Что тестируем: параметр &hide-filter позволяет скрыть
        все или некоторые не-gl-фильтры
        """
        # Проверяем, что можно скрыть фильтры частично
        response = self.report.request_json(
            'place=prime&hid=1&hide-filter=fesh,qrfrom,home_region,prepay-enabled,cpa&offer-shipping=delivery&rids=1'
        )
        self.assertFragmentNotIn(response, {"filters": [{"id": "fesh"}]})
        self.assertFragmentNotIn(response, {"filters": [{"id": "qrfrom"}]})
        self.assertFragmentNotIn(response, {"filters": [{"id": "home_region"}]})
        self.assertFragmentNotIn(response, {"filters": [{"id": "prepay-enabled"}]})
        self.assertFragmentNotIn(response, {"filters": [{"id": "cpa"}]})

    @classmethod
    def prepare_model_shop_filter(cls):
        """
        MARKETOUT-17870
        Подготовка данных для проверки фильтра по магазинам, полученным из модели
        """
        cls.index.shops += [Shop(fesh=410, name='ModelShop')]
        cls.index.offers += [
            Offer(hyperid=400, fesh=410),
        ]
        cls.index.models += [Model(title='ModelShopFilter', hyperid=400)]

    def test_model_shop_filter(self):
        """
        MARKETOUT-17870
        Проверки фильтра по магазинам, полученным из модели
        """

        # По запросу с текстом ModelShopFilter находится модель, но не оффер.
        # Для формирования фильтра по магазинам учитываются магазины не только
        # из офферов, но и из моделей, поэтому фильтр по магазину появляется.
        response = self.report.request_json('place=prime' '&show-shops=all' '&text=ModelShopFilter')
        self.assertFragmentIn(response, {"filters": [{"id": "fesh", "values": [{"value": "ModelShop", "id": "410"}]}]})

    @classmethod
    def prepare_model_shop_filter_show_more(cls):
        """
        MARKETOUT-19114
        Подготовка данных для проверки фильтра по магазинам, полученным из модели при клике на "показать все"
        """
        cls.index.shops += [Shop(fesh=1410, name='ModelShopShowMore')]
        cls.index.offers += [
            Offer(hyperid=1400, fesh=1410, title='ModelOfferShowMore'),
        ]
        cls.index.models += [Model(title='ModelShopFilterShowMore', hyperid=1400)]

    def test_model_shop_filter_show_more(self):
        """
        MARKETOUT-19114
        Проверки фильтра по магазинам, полученным из модели при клике на "показать все"
        """

        # С флагами use-default-offers=0 и nosearchresults=1 в выдаче ничего нет
        # и поэтому фильтру по магазину собираться не с чего.
        # Но под флагом &show-shops=all фильтр по магазину появляется.
        response = self.report.request_json(
            'place=prime'
            '&text=ModelShopFilterShowMore'
            '&nosearchresults=1'
            '&use-default-offers=0'
            '&show-shops=all'
            '&pp=18'
        )
        self.assertFragmentIn(
            response, {"filters": [{"id": "fesh", "values": [{"value": "ModelShopShowMore", "id": "1410"}]}]}
        )

        # Отдельно проверяем, что на схлапывании не возникают проблемы с отсутствием данных
        # о схлапываемом оффере из архивной коллекции при наличии &nosearchresults=1
        # см. MARKETINCIDENTS-3193
        response = self.report.request_json(
            'place=prime'
            '&text=ModelOfferShowMore'
            '&nosearchresults=1'
            '&use-default-offers=0'
            '&pp=18'
            '&show-shops=all'
            '&allow-collapsing=1'
        )

    @classmethod
    def prepare_avoid_duplicates_in_business_shop_filter(cls):
        cls.index.shops += [Shop(fesh=4000, name='BusinessShop', business_fesh=40)]

        cls.index.offers += [
            Offer(hyperid=4005, fesh=4000),
            Offer(hyperid=4006, fesh=4000, business_id=40, hid=4005),
            Offer(hyperid=4006, fesh=4000, business_id=40, hid=4005),
        ]

        cls.index.models += [Model(title='Model', hyperid=500, hid=4005)]

    def test_avoid_duplicates_in_business_shop_filter(self):
        response = self.report.request_json(
            'place=prime&show-shops=all&hid=4005' '&rearr-factors=enable_business_id_for_shop_filter_on_prime=1'
        )
        self.assertFragmentIn(
            response,
            {"id": "fesh", "values": [{"found": 2, "value": "BusinessShop", "id": "40"}]},
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()
