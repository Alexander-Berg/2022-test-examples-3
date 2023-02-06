#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    CpaCategory,
    CpaCategoryType,
    DeliveryBucket,
    DeliveryOption,
    MnPlace,
    Model,
    NewShopRating,
    Offer,
    Outlet,
    PickupBucket,
    PickupOption,
    Region,
    RegionalDelivery,
    Shop,
)
from core.testcase import TestCase, main


class T(TestCase):
    '''Тестируем работу галки "Предложения из моего региона" (local-offers-first - далее сокращенно lof)
    в комплекте с пессимизацией офферов без СиС (сроков и стоимости курьерской доставки)

    Примеч.: пессимизируются только предложения имеющие ТОЛЬКО курьерскую доставку с неизвесной стоимостью
    '''

    @classmethod
    def prepare(cls):

        cls.index.regiontree += [Region(rid=54, name='Екатеринбург'), Region(rid=56, name='Челябинск')]

        cls.settings.default_search_experiment_flags += ['market_new_cpm_iterator=4']
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.shops += [
            Shop(
                fesh=10,
                priority_region=54,
                regions=[225],
                name='ООО Деревообрабатывающий комбинат Ёлки-Палки (Екатеринбург)',
                new_shop_rating=NewShopRating(new_rating_total=3.0),
                cpa=Shop.CPA_REAL,
                pickup_buckets=[5001],
            ),
            Shop(
                fesh=11,
                priority_region=54,
                regions=[225],
                name='ООО Деревообрабатывающий комбинат Ёлки-Иголки (Екатеринбург)',
                new_shop_rating=NewShopRating(new_rating_total=5.0),
                cpa=Shop.CPA_REAL,
                pickup_buckets=[5002],
            ),
            Shop(
                fesh=20,
                priority_region=56,
                regions=[225],
                name='ООО Деревообрабатывающий комбинат Ёлочка (Челябинск)',
                new_shop_rating=NewShopRating(new_rating_total=4.0),
                cpa=Shop.CPA_REAL,
                pickup_buckets=[5003],
            ),
            Shop(
                fesh=21,
                priority_region=56,
                regions=[225],
                name='ООО Деревообрабатывающий комбинат Сосёночка (Челябинск)',
                new_shop_rating=NewShopRating(new_rating_total=2.0),
                cpa=Shop.CPA_REAL,
                pickup_buckets=[5004],
            ),
            Shop(
                fesh=22,
                priority_region=56,
                regions=[225],
                name='ООО Деревообрабатывающий комбинат Пихточка (Челябинск)',
                new_shop_rating=NewShopRating(new_rating_total=4.0),
                cpa=Shop.CPA_REAL,
                pickup_buckets=[5005],
            ),
            Shop(
                fesh=23,
                priority_region=56,
                regions=[225],
                name='ООО Деревообрабатывающий комбинат Кедр (Челябинск)',
                new_shop_rating=NewShopRating(new_rating_total=5.0),
                cpa=Shop.CPA_REAL,
            ),
            Shop(
                fesh=24,
                priority_region=56,
                regions=[225],
                name='ООО Деревообрабатывающий комбинат Туя (Челябинск)',
                new_shop_rating=NewShopRating(new_rating_total=5.0),
                cpa=Shop.CPA_REAL,
            ),
            Shop(
                fesh=25,
                priority_region=56,
                regions=[225],
                name='ООО Деревообрабатывающий комбинат Ягодный тис (Челябинск)',
                new_shop_rating=NewShopRating(new_rating_total=3.0),
                cpa=Shop.CPA_REAL,
            ),
            Shop(
                fesh=26,
                priority_region=56,
                regions=[225],
                name='ООО Деревообрабатывающий комбинат Можжевельник (Челябинск)',
                new_shop_rating=NewShopRating(new_rating_total=4.0),
                cpa=Shop.CPA_REAL,
            ),
        ]

        cls.index.outlets += [
            Outlet(fesh=10, region=54, point_type=Outlet.FOR_PICKUP, point_id=101),
            Outlet(fesh=11, region=54, point_type=Outlet.FOR_PICKUP, point_id=111),
            Outlet(fesh=20, region=54, point_type=Outlet.FOR_PICKUP, point_id=201),
            Outlet(fesh=21, region=54, point_type=Outlet.FOR_PICKUP, point_id=211),
            Outlet(fesh=22, region=54, point_type=Outlet.FOR_PICKUP, point_id=221),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5001,
                fesh=10,
                carriers=[99],
                options=[PickupOption(outlet_id=101)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5002,
                fesh=11,
                carriers=[99],
                options=[PickupOption(outlet_id=111)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5003,
                fesh=20,
                carriers=[99],
                options=[PickupOption(outlet_id=201)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5004,
                fesh=21,
                carriers=[99],
                options=[PickupOption(outlet_id=211)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5005,
                fesh=22,
                carriers=[99],
                options=[PickupOption(outlet_id=221)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        cls.index.cpa_categories += [
            CpaCategory(hid=300, regions=[54], cpa_type=CpaCategoryType.CPA_WITH_CPC_PESSIMIZATION),
        ]

        cls.index.models += [
            Model(hyperid=5000, hid=300),
            Model(hyperid=6000, hid=300),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=20,
                fesh=20,
                carriers=[1],
                regional_options=[
                    RegionalDelivery(
                        rid=54,
                        options=[
                            DeliveryOption(price=150),
                        ],
                    ),
                ],
            ),
            DeliveryBucket(
                bucket_id=23,
                fesh=23,
                carriers=[2],
                regional_options=[
                    RegionalDelivery(
                        rid=54,
                        options=[
                            DeliveryOption(price=300),
                        ],
                    ),
                    # вот этот костыль для прикрытия бага "Не ищется оффер имеющий только региональную доставку"
                    RegionalDelivery(
                        rid=56,
                        options=[
                            DeliveryOption(price=300),
                        ],
                    ),
                ],
            ),
        ]

        def init_offers(hyperid, bts, title, cpa=Offer.CPA_NO, bids=None):
            """bids = {shop-id: cbid} default cbid = 10"""

            def bid_for(fesh):
                return bids.get(fesh, 10) if bids else 10

            cls.index.offers += [
                Offer(
                    hyperid=hyperid,
                    ts=bts + 1,
                    fesh=10,
                    title=title + " (из Екб, доставка, т.самовывоза в Екб)",
                    discount=12,
                    price=4490,
                    cpa=cpa,
                    has_delivery_options=True,
                    pickup=True,
                    bid=bid_for(10),
                    delivery_options=[DeliveryOption(price=100, day_from=0, day_to=2)],
                ),
                Offer(
                    hyperid=hyperid,
                    ts=bts + 2,
                    fesh=11,
                    title=title + " (из Екб, т.самовывоза в Екб)",
                    discount=15,
                    price=4500,
                    cpa=cpa,
                    has_delivery_options=False,
                    pickup=True,
                    bid=bid_for(11),
                ),
                Offer(
                    hyperid=hyperid,
                    ts=bts + 3,
                    fesh=20,
                    title=title + " (из Челябинска, доставка с СиС, т.самовывоза в Екб)",
                    discount=23,
                    price=4400,
                    cpa=cpa,
                    has_delivery_options=True,
                    pickup=True,
                    bid=bid_for(20),
                    delivery_buckets=[20],
                ),
                Offer(
                    hyperid=hyperid,
                    ts=bts + 4,
                    fesh=21,
                    title=title + " (из Челябинска, т.самовывоза в Екб)",
                    discount=40,
                    price=3000,
                    cpa=cpa,
                    has_delivery_options=False,
                    pickup=True,
                    bid=bid_for(21),
                ),
                Offer(
                    hyperid=hyperid,
                    ts=bts + 5,
                    fesh=22,
                    title=title + " (из Челябинска, доставка без СиС, т.самовывоза в Екб)",
                    discount=18,
                    price=4000,
                    cpa=cpa,
                    has_delivery_options=True,
                    pickup=True,
                    bid=bid_for(22),
                ),
                Offer(
                    hyperid=hyperid,
                    ts=bts + 6,
                    fesh=23,
                    title=title + " (из Челябинска, доставка c CиС)",
                    discount=5,
                    price=5100,
                    cpa=cpa,
                    has_delivery_options=True,
                    pickup=False,
                    bid=bid_for(23),
                    delivery_buckets=[23],
                ),
                Offer(
                    hyperid=hyperid,
                    ts=bts + 7,
                    fesh=24,
                    title=title + " (из Челябинска, доставка без СиС) - должен пессимизироваться",
                    discount=13,
                    price=4250,
                    cpa=cpa,
                    has_delivery_options=True,
                    pickup=False,
                    bid=bid_for(24),
                ),
                Offer(
                    hyperid=hyperid,
                    ts=bts + 8,
                    fesh=25,
                    title=title + " (из Челябинска, доставка без СиС) - должен пессимизироваться",
                    discount=29,
                    price=4100,
                    has_delivery_options=True,
                    pickup=False,
                    bid=bid_for(25),
                ),
                Offer(
                    hyperid=hyperid,
                    ts=bts + 9,
                    fesh=26,
                    title=title + " (из Челябинска, доставка без СиС) - должен пессимизироваться",
                    discount=11,
                    price=4670,
                    cpa=cpa,
                    has_delivery_options=True,
                    pickup=False,
                    bid=bid_for(26),
                ),
            ]

            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, bts + 1).respond(0.03)
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, bts + 2).respond(0.02)
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, bts + 3).respond(0.03001)
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, bts + 4).respond(0.025)
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, bts + 5).respond(0.02001)
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, bts + 6).respond(0.01)
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, bts + 7).respond(0.023)
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, bts + 8).respond(0.016)
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, bts + 9).respond(0.014)

        # для проверки prime - офферы [1001, 1002, ... ] без привязки к модели
        init_offers(hyperid=None, bts=1000, title="Горбыль деловой")

        # точно такие же офферы [2001, 2002, ...] для проверки productoffers - с привязкой к модели hyperid=2000
        init_offers(hyperid=2000, bts=2000, title="Доска обрезная")

        # офферы [3001, 3002 ...] для проверки пересортировок по cbid в ResultsIterator в productoffers
        # для офферов из магазинов 21, 24, 25 выставлены cbid-ставки влияющие на ранжирование
        init_offers(hyperid=3000, bts=3000, title="Опилки", bids={11: 100, 21: 80, 24: 200, 25: 202})

        # по нескольку офферов из одних и тех же магазинов для проверки пересортировок head/tail на prime
        init_offers(hyperid=4000, bts=4000, title="Брус деревянный")
        init_offers(hyperid=4000, bts=4100, title="Брус деревянный")

        # аналог 2000 в категории CPA_WITH_CPC_PESSIMIZATION
        init_offers(hyperid=5000, bts=2000, title="Доска обрезная", cpa=Offer.CPA_REAL)

        # аналог 3000 в категории CPA_WITH_CPC_PESSIMIZATION
        init_offers(
            hyperid=6000, bts=3000, title="Опилки", cpa=Offer.CPA_REAL, bids={11: 100, 21: 80, 24: 200, 25: 210}
        )

    def assertFragment(self, response, fragment, contains):
        if contains:
            self.assertFragmentIn(response, fragment)
        else:
            self.assertFragmentNotIn(response, fragment)

    def check_search(self, text, cgi, expected_json, regional_delimiter=True):
        '''тестируем place=prime'''

        if expected_json:
            response = self.report.request_json('place=prime&text=' + text + cgi)
            self.assertFragmentIn(response, expected_json, preserve_order=True)
            self.assertFragment(response, {"entity": "regionalDelimiter"}, regional_delimiter)

    def check_offers(self, hyperid, cgi, expected_json, regional_delimiter=True):
        '''тестируем Запрос на productoffers офферов'''
        response = self.report.request_json('place=productoffers&hyperid={}'.format(hyperid) + cgi)
        self.assertFragmentIn(response, expected_json, preserve_order=True)
        self.assertFragment(response, {"entity": "regionalDelimiter"}, regional_delimiter)

    def test_default_sorting_with_lof(self):
        '''Галка "Предложения из моего региона" включена
        Галка "Цена с учетом доставки" включена/выключена
        Сортировка по умолчанию


        локальные предложения отделяются от региональных чертой
        региональные предложения без СиС пессимизируются только на КМ
        сортировка по matrixnet_value
        '''
        expected_json = {
            "results": [
                {"shop": {"id": 10}},  # 0.3
                {"shop": {"id": 11}},  # 0.2
                {"entity": "regionalDelimiter"},
                {"shop": {"id": 20}},  # 0.3001
                {"shop": {"id": 21}},  # 0.25
                {"shop": {"id": 24}},  # 0.23
                {"shop": {"id": 22}},  # 0.2001
                {"shop": {"id": 25}},  # 0.19
                {"shop": {"id": 26}},  # 0.14
                {"shop": {"id": 23}},  # 0.1
            ]
        }
        # на поиске офферы без СиС не пессимизируются
        cgi_with_delivery = '&rids=54&local-offers-first=1&deliveryincluded=1'
        cgi_without_delivery = '&rids=54&local-offers-first=1'
        self.check_search('Горбыль+Деловой', cgi_with_delivery, expected_json)
        self.check_search('Горбыль+Деловой', cgi_without_delivery, expected_json)

        # тестирование офферов с выставленным bid (hyperid=3000)
        # локальные, региональные непессимизированные офферы и пессимизированные офферы пересортируются отдельно (без перемешивания с пессимизированными)
        cgi_with_delivery = '&rids=54&local-offers-first=1&deliveryincluded=1&mcpriceto=4600'
        cgi_without_delivery = '&rids=54&local-offers-first=1&mcpriceto=4600'

        expected_json = {
            "results": [
                # local head
                {"shop": {"id": 10}},
                {"shop": {"id": 11}},
                {"entity": "regionalDelimiter"},
                # non local head
                {"shop": {"id": 20}},
                {"shop": {"id": 21}},
                {"shop": {"id": 22}},
                # non local tail - пессимизированые офферы без СиС
                {"shop": {"id": 24}},  # bid 200
                {"shop": {"id": 25}},  # bid 202
            ]
        }
        self.check_offers(3000, cgi_with_delivery, expected_json)
        self.check_offers(3000, cgi_without_delivery, expected_json)

    def test_default_sorting_head_tail_with_lof(self):
        '''Галка "Предложения из моего региона" включена
        Сортировка по умолчанию
        дублирующиеся офферы одной модели из одного и того же магазина теперь НЕ отправляются в tail
        офферы без СиС не пессимизируются
        '''

        cgi_with_delivery = '&rids=54&local-offers-first=1&deliveryincluded=1&numdoc=20&&rearr-factors=market_ranging_cpa_by_ue_in_top_cpa_multiplier=1'
        cgi_without_delivery = (
            '&rids=54&local-offers-first=1&numdoc=20&rearr-factors=market_ranging_cpa_by_ue_in_top_cpa_multiplier=1'
        )

        expected_json = {
            "results": [
                {"shop": {"id": 10}},  # head
                {"shop": {"id": 10}},  # head
                {"shop": {"id": 11}},  # head
                {"shop": {"id": 11}},  # head
                {"entity": "regionalDelimiter"},
                {"shop": {"id": 20}},  # head
                {"shop": {"id": 20}},  # head
                {"shop": {"id": 21}},  # head
                {"shop": {"id": 21}},  # head
                {"shop": {"id": 24}},  # head
                {"shop": {"id": 24}},  # head
                {"shop": {"id": 22}},  # head
                {"shop": {"id": 22}},  # head
                {"shop": {"id": 25}},  # head
                {"shop": {"id": 25}},  # head
                {"shop": {"id": 26}},  # head
                {"shop": {"id": 26}},  # head
                {"shop": {"id": 23}},  # head
                {"shop": {"id": 23}},  # head
            ]
        }

        self.check_search('Брус+деревянный', cgi_with_delivery, expected_json)
        self.check_search('Брус+деревянный', cgi_without_delivery, expected_json)

    def test_default_sorting_with_lof_for_sovetnik(self):
        '''Галка "Предложения из моего региона" включена по умолчанию
        Галка "Цена с учетом доставки" включена/выключена
        Сортировка по умолчанию


        локальные предложения отделяются от региональных чертой
        региональные предложения без СиС НЕ пессимизируются (MARKETOUT-13748)
        сортировка по matrixnet_value
        '''
        cgi_with_delivery = '&rids=54&deliveryincluded=1&client=sovetnik'
        cgi_without_delivery = '&rids=54&client=sovetnik'
        expected_json = {
            "results": [
                {"shop": {"id": 10}},  # 0.3
                {"shop": {"id": 11}},  # 0.2
                {"entity": "regionalDelimiter"},
                {"shop": {"id": 20}},  # 0.3001
                {"shop": {"id": 21}},  # 0.25
                {"shop": {"id": 24}},  # 0.23
                {"shop": {"id": 22}},  # 0.2001
                {"shop": {"id": 25}},  # 0.19
                {"shop": {"id": 26}},  # 0.14
                {"shop": {"id": 23}},  # 0.1
            ]
        }
        self.check_search('Горбыль+Деловой', cgi_with_delivery, expected_json)
        self.check_search('Горбыль+Деловой', cgi_without_delivery, expected_json)

        self.check_offers(2000, cgi_with_delivery, expected_json)
        self.check_offers(2000, cgi_without_delivery, expected_json)

    def test_aprice_sorting_with_lof(self):
        '''Галка "Предложения из моего региона" включена
        Галка "Цена с учетом доставки" выключена
        Сортировка по цене
        Пессимизация офферов без СиС включена

        локальные предложения отделяются от региональных чертой
        офферы без СиС не пессимизируются, т.к цена без учета стоимости доставки
        офферы отсортированы по цене по возрастанию (без учета стоимости доставки)

        '''
        cgi = '&rids=54&local-offers-first=1&how=aprice'
        expected_json = {
            "results": [
                {"shop": {"id": 10}, "prices": {"rawValue": "4490", "value": "4490", "isDeliveryIncluded": False}},
                {"shop": {"id": 11}, "prices": {"rawValue": "4500", "value": "4500", "isDeliveryIncluded": False}},
                {"entity": "regionalDelimiter"},
                {"shop": {"id": 21}, "prices": {"rawValue": "3000", "value": "3000", "isDeliveryIncluded": False}},
                {"shop": {"id": 22}, "prices": {"rawValue": "4000", "value": "4000", "isDeliveryIncluded": False}},
                {"shop": {"id": 25}, "prices": {"rawValue": "4100", "value": "4100", "isDeliveryIncluded": False}},
                {"shop": {"id": 24}, "prices": {"rawValue": "4250", "value": "4250", "isDeliveryIncluded": False}},
                {"shop": {"id": 20}, "prices": {"rawValue": "4400", "value": "4400", "isDeliveryIncluded": False}},
                {"shop": {"id": 26}, "prices": {"rawValue": "4670", "value": "4670", "isDeliveryIncluded": False}},
                {"shop": {"id": 23}, "prices": {"rawValue": "5100", "value": "5100", "isDeliveryIncluded": False}},
            ]
        }

        self.check_search('Горбыль+Деловой', cgi, expected_json)

    def test_aprice_sorting_include_delivery_with_lof(self):
        '''Галка "Предложения из моего региона" включена
        Галка "Цена с учетом доставки" включена
        Сортировка по цене

        локальные предложения отделяются от региональных чертой
        предложения без СиС НЕ пессимизируются
        предложения отсортированы по цене по возрастанию (с учетом стоимости доставки)
        если известна стоимость региональной доставки, то она прибавляется к цене оффера
        '''
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        cgi = '&rids=54&local-offers-first=1&how=aprice&deliveryincluded=1' + unified_off_flags
        expected_json = {
            "results": [
                {"shop": {"id": 11}, "prices": {"rawValue": "4500", "value": "4500", "isDeliveryIncluded": True}},
                {"shop": {"id": 10}, "prices": {"rawValue": "4490", "value": "4590", "isDeliveryIncluded": True}},
                {"entity": "regionalDelimiter"},
                {"shop": {"id": 21}, "prices": {"rawValue": "3000", "value": "3000", "isDeliveryIncluded": True}},
                {"shop": {"id": 22}, "prices": {"rawValue": "4000", "value": "4000", "isDeliveryIncluded": True}},
                {"shop": {"id": 25}, "prices": {"rawValue": "4100", "value": "4100", "isDeliveryIncluded": True}},
                {"shop": {"id": 24}, "prices": {"rawValue": "4250", "value": "4250", "isDeliveryIncluded": True}},
                {"shop": {"id": 20}, "prices": {"rawValue": "4400", "value": "4550", "isDeliveryIncluded": True}},
                {"shop": {"id": 26}, "prices": {"rawValue": "4670", "value": "4670", "isDeliveryIncluded": True}},
                {"shop": {"id": 23}, "prices": {"rawValue": "5100", "value": "5400", "isDeliveryIncluded": True}},
            ]
        }

        self.check_search('Горбыль+Деловой', cgi, expected_json)
        self.check_search('Опилки', cgi, expected_json)

    def test_rorp_sorting_with_lof(self):
        '''Галка "Предложения из моего региона" включена
        Галка "Цена с учетом доставки" выключена
        Сортировка по рейтингу и цене

        локальные офферы отделены от региональных чертой
        офферы без СиС не пессимизируются, т.к цена без учета стоимости доставки
        предложения отсортированы по рейтингу магазина (5,4,3,2,1,0) а затем по цене
        цена указана без учета доставки
        '''
        cgi = '&rids=54&local-offers-first=1&how=rorp'
        expected_json = {
            "results": [
                {
                    "shop": {"id": 10, "qualityRating": 3},
                    "prices": {"rawValue": "4490", "value": "4490", "isDeliveryIncluded": False},
                },
                {
                    "shop": {"id": 11, "qualityRating": 5},
                    "prices": {"rawValue": "4500", "value": "4500", "isDeliveryIncluded": False},
                },
                {
                    "shop": {"id": 21, "qualityRating": 2},
                    "prices": {"rawValue": "3000", "value": "3000", "isDeliveryIncluded": False},
                },
                {
                    "shop": {"id": 22, "qualityRating": 4},
                    "prices": {"rawValue": "4000", "value": "4000", "isDeliveryIncluded": False},
                },
                {
                    "shop": {"id": 25, "qualityRating": 3},
                    "prices": {"rawValue": "4100", "value": "4100", "isDeliveryIncluded": False},
                },
                {
                    "shop": {"id": 24, "qualityRating": 5},
                    "prices": {"rawValue": "4250", "value": "4250", "isDeliveryIncluded": False},
                },
                {
                    "shop": {"id": 20, "qualityRating": 4},
                    "prices": {"rawValue": "4400", "value": "4400", "isDeliveryIncluded": False},
                },
                {
                    "shop": {"id": 26, "qualityRating": 4},
                    "prices": {"rawValue": "4670", "value": "4670", "isDeliveryIncluded": False},
                },
                {
                    "shop": {"id": 23, "qualityRating": 5},
                    "prices": {"rawValue": "5100", "value": "5100", "isDeliveryIncluded": False},
                },
            ]
        }

        self.check_search('Горбыль+Деловой', cgi, expected_json)

    def test_rorp_sorting_include_delivery_with_lof(self):
        '''Галка "Предложения из моего региона" включена
        Галка "Цена с учетом доставки" включена
        Сортировка по рейтингу и цене

        локальные офферы отделены от региональных чертой
        региональные предложения без СиС пессимизируются только на КМ
        предложения отсортированы по рейтингу магазина (5, 4, 3, 2, 1, 0) а затем по цене (с учетом стоимости доставки)
        если известна стоимость региональной доставки, то она добавляется к цене оффера
        '''

        pessimized = {
            "results": [
                {
                    "shop": {"id": 11, "qualityRating": 5},
                    "prices": {"rawValue": "4500", "value": "4500", "isDeliveryIncluded": True},
                },
                {
                    "shop": {"id": 10, "qualityRating": 3},
                    "prices": {"rawValue": "4490", "value": "4590", "isDeliveryIncluded": True},
                },
                {
                    "shop": {"id": 21, "qualityRating": 2},
                    "prices": {"rawValue": "3000", "value": "3000", "isDeliveryIncluded": True},
                },
                {
                    "shop": {"id": 22, "qualityRating": 4},
                    "prices": {"rawValue": "4000", "value": "4000", "isDeliveryIncluded": True},
                },
                {
                    "shop": {"id": 20, "qualityRating": 4},
                    "prices": {"rawValue": "4400", "value": "4550", "isDeliveryIncluded": True},
                },
                {
                    "shop": {"id": 23, "qualityRating": 5},
                    "prices": {"rawValue": "5100", "value": "5400", "isDeliveryIncluded": True},
                },
                {
                    "shop": {"id": 25, "qualityRating": 3},
                    "prices": {"rawValue": "4100", "value": "4100", "isDeliveryIncluded": True},
                },
                {
                    "shop": {"id": 24, "qualityRating": 5},
                    "prices": {"rawValue": "4250", "value": "4250", "isDeliveryIncluded": True},
                },
                {
                    "shop": {"id": 26, "qualityRating": 4},
                    "prices": {"rawValue": "4670", "value": "4670", "isDeliveryIncluded": True},
                },
            ]
        }

        not_pessimized = {
            "results": [
                {
                    "shop": {"id": 11, "qualityRating": 5},
                    "prices": {"rawValue": "4500", "value": "4500", "isDeliveryIncluded": True},
                },
                {
                    "shop": {"id": 10, "qualityRating": 3},
                    "prices": {"rawValue": "4490", "value": "4590", "isDeliveryIncluded": True},
                },
                {
                    "shop": {"id": 21, "qualityRating": 2},
                    "prices": {"rawValue": "3000", "value": "3000", "isDeliveryIncluded": True},
                },
                {
                    "shop": {"id": 22, "qualityRating": 4},
                    "prices": {"rawValue": "4000", "value": "4000", "isDeliveryIncluded": True},
                },
                {
                    "shop": {"id": 25, "qualityRating": 3},
                    "prices": {"rawValue": "4100", "value": "4100", "isDeliveryIncluded": True},
                },
                {
                    "shop": {"id": 24, "qualityRating": 5},
                    "prices": {"rawValue": "4250", "value": "4250", "isDeliveryIncluded": True},
                },
                {
                    "shop": {"id": 20, "qualityRating": 4},
                    "prices": {"rawValue": "4400", "value": "4550", "isDeliveryIncluded": True},
                },
                {
                    "shop": {"id": 26, "qualityRating": 4},
                    "prices": {"rawValue": "4670", "value": "4670", "isDeliveryIncluded": True},
                },
                {
                    "shop": {"id": 23, "qualityRating": 5},
                    "prices": {"rawValue": "5100", "value": "5400", "isDeliveryIncluded": True},
                },
            ]
        }

        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        cgi = '&rids=54&local-offers-first=1&how=rorp&deliveryincluded=1' + unified_off_flags
        self.check_search('Горбыль+Деловой', cgi, not_pessimized)
        self.check_search('Опилки', cgi, not_pessimized)
        self.check_offers(3000, cgi, pessimized)

    def test_discount_sorting_with_lof(self):
        '''Галка "Предложения из моего региона" включена
        Галка "Цена с учетом доставки" выключена/выключена
        Сортировка по скидке

        локальные офферы отделены от региональных чертой
        предложения без СиС не пессимизируются, т.к. сортировка не ценовая и не по умолчанию
        предложения отсортированы по размеру скидки

        '''
        cgi_with_delivery = '&rids=54&local-offers-first=1&how=discount_p&deliveryincluded=1'
        cgi_without_delivery = '&rids=54&local-offers-first=1&how=discount_p'

        expected_json = {
            "results": [
                {"shop": {"id": 11}, "prices": {"discount": {"percent": 15}}},
                {"shop": {"id": 10}, "prices": {"discount": {"percent": 12}}},
                {"entity": "regionalDelimiter"},
                {"shop": {"id": 21}, "prices": {"discount": {"percent": 40}}},
                {"shop": {"id": 25}, "prices": {"discount": {"percent": 29}}},
                {"shop": {"id": 20}, "prices": {"discount": {"percent": 23}}},
                {"shop": {"id": 22}, "prices": {"discount": {"percent": 18}}},
                {"shop": {"id": 24}, "prices": {"discount": {"percent": 13}}},
                {"shop": {"id": 26}, "prices": {"discount": {"percent": 11}}},
                {"shop": {"id": 23}, "prices": {"discount": {"percent": 5}}},
            ]
        }

        self.check_search('Горбыль+Деловой', cgi_with_delivery, expected_json)
        self.check_search('Горбыль+Деловой', cgi_without_delivery, expected_json)
        self.check_search('Опилки', cgi_with_delivery, expected_json)
        self.check_search('Опилки', cgi_without_delivery, expected_json)

    def test_default_sorting_without_lof(self):
        '''Галка "Предложения из моего региона" выключена
        Галка "Цена с учетом доставки" включена/выключена
        Сортировка по умолчанию

        локальные и региональные предложения объединены (нет черты "Доставка из другого региона")
        '''

        expected_json = {
            "results": [
                {"shop": {"id": 20}},  # Члб, 0.3001
                {"shop": {"id": 10}},  # Екб, 0.3
                {"shop": {"id": 21}},  # Члб, 0.25
                {"shop": {"id": 24}},  # Члб, 0.23
                {"shop": {"id": 22}},  # Члб, 0.2001
                {"shop": {"id": 11}},  # Екб, 0.2
                {"shop": {"id": 25}},  # Члб, 0.19
                {"shop": {"id": 26}},  # Члб, 0.14
                {"shop": {"id": 23}},  # Члб, 0.1
            ]
        }

        # региональные офферы без СиС не пессимизируются на поиске на дефолтной сортировке
        cgi_with_delivery = '&rids=54&local-offers-first=0&deliveryincluded=1'
        cgi_without_delivery = '&rids=54&local-offers-first=0'

        self.check_search('Горбыль+Деловой', cgi_with_delivery, expected_json, regional_delimiter=False)
        self.check_search('Горбыль+Деловой', cgi_without_delivery, expected_json, regional_delimiter=False)

        # тестирование офферов с выставленным bid
        # непессимизированные офферы пересортируются отдельно (без перемешивания с пессимизированными)
        cgi = '&rids=54&local-offers-first=0&mcpriceto=4600'

        expected_json = {
            "results": [
                {"shop": {"id": 20}},
                {"shop": {"id": 10}},
                {"shop": {"id": 21}},
                {"shop": {"id": 22}},
                {"shop": {"id": 11}},
                {"shop": {"id": 24}},
                {"shop": {"id": 25}},
            ]
        }
        self.check_offers(3000, cgi, expected_json, regional_delimiter=False)

    def test_default_sorting_head_tail_without_lof(self):
        '''Галка "Предложения из моего региона" выключена
        Сортировка по умолчанию
        дублирующиеся офферы одной модели из одного и того же магазина теперь не отправляются в tail
        '''

        cgi_with_delivery = '&rids=54&local-offers-first=0&deliveryincluded=1&numdoc=20'
        cgi_without_delivery = '&rids=54&local-offers-first=0&numdoc=20'

        expected_json = {
            "results": [
                {"shop": {"id": 20}},  # Члб, 0.3001
                {"shop": {"id": 20}},  # Члб, 0.3001
                {"shop": {"id": 10}},  # Екб, 0.3
                {"shop": {"id": 10}},  # Екб, 0.3
                {"shop": {"id": 21}},  # Члб, 0.25
                {"shop": {"id": 21}},  # Члб, 0.25
                {"shop": {"id": 24}},  # Члб, 0.23
                {"shop": {"id": 24}},  # Члб, 0.23
                {"shop": {"id": 22}},  # Члб, 0.2001
                {"shop": {"id": 22}},  # Члб, 0.2001
                {"shop": {"id": 11}},  # Екб, 0.2
                {"shop": {"id": 11}},  # Екб, 0.2
                {"shop": {"id": 25}},  # Члб, 0.19
                {"shop": {"id": 25}},  # Члб, 0.19
                {"shop": {"id": 26}},  # Члб, 0.14
                {"shop": {"id": 26}},  # Члб, 0.14
                {"shop": {"id": 23}},  # Члб, 0.1
                {"shop": {"id": 23}},  # Члб, 0.1
            ]
        }

        self.check_search('Брус+деревянный', cgi_with_delivery, expected_json, regional_delimiter=False)
        self.check_search('Брус+деревянный', cgi_without_delivery, expected_json, regional_delimiter=False)

    def test_aprice_sorting_without_lof(self):
        '''Галка "Предложения из моего региона" выключена
        Галка "Цена с учетом доставки" выключена
        Сортировка по цене

        локальные и региональные предложения объединены (нет черты "Доставка из другого региона")
        предложения без СиС не пессимизируются, т.к. цена без учета доставки
        предложения отсортированы по цене (без учета стоимости доставки)
        '''
        cgi = '&rids=54&local-offers-first=0&how=aprice'
        expected_json = {
            "results": [
                {"shop": {"id": 21}, "prices": {"rawValue": "3000", "value": "3000", "isDeliveryIncluded": False}},
                {"shop": {"id": 22}, "prices": {"rawValue": "4000", "value": "4000", "isDeliveryIncluded": False}},
                {"shop": {"id": 25}, "prices": {"rawValue": "4100", "value": "4100", "isDeliveryIncluded": False}},
                {"shop": {"id": 24}, "prices": {"rawValue": "4250", "value": "4250", "isDeliveryIncluded": False}},
                {"shop": {"id": 20}, "prices": {"rawValue": "4400", "value": "4400", "isDeliveryIncluded": False}},
                {"shop": {"id": 10}, "prices": {"rawValue": "4490", "value": "4490", "isDeliveryIncluded": False}},
                {"shop": {"id": 11}, "prices": {"rawValue": "4500", "value": "4500", "isDeliveryIncluded": False}},
                {"shop": {"id": 26}, "prices": {"rawValue": "4670", "value": "4670", "isDeliveryIncluded": False}},
                {"shop": {"id": 23}, "prices": {"rawValue": "5100", "value": "5100", "isDeliveryIncluded": False}},
            ]
        }

        self.check_search('Горбыль+Деловой', cgi, expected_json, regional_delimiter=False)

    def test_aprice_sorting_include_delivery_without_lof(self):
        '''Галка "Предложения из моего региона" выключена
        Галка "Цена с учетом доставки" включена
        Сортировка по цене

        локальные и региональные предложения объединены (нет черты "Доставка из другого региона")
        предложения без СиС не пессимизируются
        предложения отсортированы по цене (с учетом стоимости доставки)
        если известна стоимость региональной доставки - она прибавляется к стоимости оффера
        '''
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        cgi = '&rids=54&local-offers-first=0&how=aprice&deliveryincluded=1' + unified_off_flags
        expected_json = {
            "results": [
                {"shop": {"id": 21}, "prices": {"rawValue": "3000", "value": "3000", "isDeliveryIncluded": True}},
                {"shop": {"id": 22}, "prices": {"rawValue": "4000", "value": "4000", "isDeliveryIncluded": True}},
                {"shop": {"id": 25}, "prices": {"rawValue": "4100", "value": "4100", "isDeliveryIncluded": True}},
                {"shop": {"id": 24}, "prices": {"rawValue": "4250", "value": "4250", "isDeliveryIncluded": True}},
                {"shop": {"id": 11}, "prices": {"rawValue": "4500", "value": "4500", "isDeliveryIncluded": True}},
                {"shop": {"id": 20}, "prices": {"rawValue": "4400", "value": "4550", "isDeliveryIncluded": True}},
                {"shop": {"id": 10}, "prices": {"rawValue": "4490", "value": "4590", "isDeliveryIncluded": True}},
                {"shop": {"id": 26}, "prices": {"rawValue": "4670", "value": "4670", "isDeliveryIncluded": True}},
                {"shop": {"id": 23}, "prices": {"rawValue": "5100", "value": "5400", "isDeliveryIncluded": True}},
            ]
        }

        self.check_search('Горбыль+Деловой', cgi, expected_json, regional_delimiter=False)

    def test_rorp_sorting_without_lof(self):
        '''Галка "Предложения из моего региона" выключена
        Галка "Цена с учетом доставки" выключена
        Сортировка по рейтингу и цене
        Пессимизация офферов без СиС

        локальные и региональные предложения объединены (нет черты "Доставка из другого региона")
        предложения без СиС не пессимизируются, т.к. цена без учета доставки
        предложения отсортированы по рейтингу магазина (4-5, 2-3, 0-1) а затем по цене
        цена указана без учета доставки
        региональные офферы без СиС НЕ пессимизируются
        '''
        cgi = '&rids=54&local-offers-first=0&how=rorp'
        expected_json = {
            "results": [
                {
                    "shop": {"id": 21, "qualityRating": 2},
                    "prices": {"rawValue": "3000", "value": "3000", "isDeliveryIncluded": False},
                },
                {
                    "shop": {"id": 22, "qualityRating": 4},
                    "prices": {"rawValue": "4000", "value": "4000", "isDeliveryIncluded": False},
                },
                {
                    "shop": {"id": 25, "qualityRating": 3},
                    "prices": {"rawValue": "4100", "value": "4100", "isDeliveryIncluded": False},
                },
                {
                    "shop": {"id": 24, "qualityRating": 5},
                    "prices": {"rawValue": "4250", "value": "4250", "isDeliveryIncluded": False},
                },
                {
                    "shop": {"id": 20, "qualityRating": 4},
                    "prices": {"rawValue": "4400", "value": "4400", "isDeliveryIncluded": False},
                },
                {
                    "shop": {"id": 10, "qualityRating": 3},
                    "prices": {"rawValue": "4490", "value": "4490", "isDeliveryIncluded": False},
                },
                {
                    "shop": {"id": 11, "qualityRating": 5},
                    "prices": {"rawValue": "4500", "value": "4500", "isDeliveryIncluded": False},
                },
                {
                    "shop": {"id": 26, "qualityRating": 4},
                    "prices": {"rawValue": "4670", "value": "4670", "isDeliveryIncluded": False},
                },
                {
                    "shop": {"id": 23, "qualityRating": 5},
                    "prices": {"rawValue": "5100", "value": "5100", "isDeliveryIncluded": False},
                },
            ]
        }

        self.check_search('Горбыль+Деловой', cgi, expected_json, regional_delimiter=False)

    def test_rorp_sorting_include_delivery_without_lof(self):
        '''Галка "Предложения из моего региона" выключена
        Галка "Цена с учетом доставки" включена
        Сортировка по рейтингу и цене

        локальные и региональные предложения объединены (нет черты "Доставка из другого региона")
        региональные предложения без СиС пессимизируются только на КМ
        предложения отсортированы по рейтингу магазина (5, 4, 3, 2, 1, 0) а затем по цене (с учетом стоимости доставки)
        если известна стоимость региональной доставки, то она добавляется к цене оффера
        '''
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        pessimized = {
            "results": [
                {
                    "shop": {"id": 21, "qualityRating": 2},
                    "prices": {"rawValue": "3000", "value": "3000", "isDeliveryIncluded": True},
                },
                {
                    "shop": {"id": 22, "qualityRating": 4},
                    "prices": {"rawValue": "4000", "value": "4000", "isDeliveryIncluded": True},
                },
                {
                    "shop": {"id": 11, "qualityRating": 5},
                    "prices": {"rawValue": "4500", "value": "4500", "isDeliveryIncluded": True},
                },
                {
                    "shop": {"id": 20, "qualityRating": 4},
                    "prices": {"rawValue": "4400", "value": "4550", "isDeliveryIncluded": True},
                },
                {
                    "shop": {"id": 10, "qualityRating": 3},
                    "prices": {"rawValue": "4490", "value": "4590", "isDeliveryIncluded": True},
                },
                {
                    "shop": {"id": 23, "qualityRating": 5},
                    "prices": {"rawValue": "5100", "value": "5400", "isDeliveryIncluded": True},
                },
                {
                    "shop": {"id": 25, "qualityRating": 3},
                    "prices": {"rawValue": "4100", "value": "4100", "isDeliveryIncluded": True},
                },
                {
                    "shop": {"id": 24, "qualityRating": 5},
                    "prices": {"rawValue": "4250", "value": "4250", "isDeliveryIncluded": True},
                },
                {
                    "shop": {"id": 26, "qualityRating": 4},
                    "prices": {"rawValue": "4670", "value": "4670", "isDeliveryIncluded": True},
                },
            ]
        }

        not_pessimized = {
            "results": [
                {
                    "shop": {"id": 21, "qualityRating": 2},
                    "prices": {"rawValue": "3000", "value": "3000", "isDeliveryIncluded": True},
                },
                {
                    "shop": {"id": 22, "qualityRating": 4},
                    "prices": {"rawValue": "4000", "value": "4000", "isDeliveryIncluded": True},
                },
                {
                    "shop": {"id": 25, "qualityRating": 3},
                    "prices": {"rawValue": "4100", "value": "4100", "isDeliveryIncluded": True},
                },
                {
                    "shop": {"id": 24, "qualityRating": 5},
                    "prices": {"rawValue": "4250", "value": "4250", "isDeliveryIncluded": True},
                },
                {
                    "shop": {"id": 11, "qualityRating": 5},
                    "prices": {"rawValue": "4500", "value": "4500", "isDeliveryIncluded": True},
                },
                {
                    "shop": {"id": 20, "qualityRating": 4},
                    "prices": {"rawValue": "4400", "value": "4550", "isDeliveryIncluded": True},
                },
                {
                    "shop": {"id": 10, "qualityRating": 3},
                    "prices": {"rawValue": "4490", "value": "4590", "isDeliveryIncluded": True},
                },
                {
                    "shop": {"id": 26, "qualityRating": 4},
                    "prices": {"rawValue": "4670", "value": "4670", "isDeliveryIncluded": True},
                },
                {
                    "shop": {"id": 23, "qualityRating": 5},
                    "prices": {"rawValue": "5100", "value": "5400", "isDeliveryIncluded": True},
                },
            ]
        }

        cgi = '&rids=54&local-offers-first=0&how=rorp&deliveryincluded=1' + unified_off_flags
        self.check_search('Горбыль+Деловой', cgi, not_pessimized, regional_delimiter=False)
        self.check_offers(3000, cgi, pessimized, regional_delimiter=False)

    def test_discount_sorting_without_lof(self):
        '''Галка "Предложения из моего региона" выключена
        Галка "Цена с учетом доставки" выключена/выключена
        Сортировка по скидке

        локальные и региональные предложения объединены (нет черты "Доставка из другого региона")
        предложения отсортированы по размеру скидки
        предложения без СиС не пессимизируются, т.к. сортировка не ценовая и не по умолчанию
        '''
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        cgi_with_delivery = '&rids=54&local-offers-first=0&how=discount_p&deliveryincluded=1' + unified_off_flags
        cgi_without_delivery = '&rids=54&local-offers-first=0&how=discount_p' + unified_off_flags

        expected_json = {
            "results": [
                {"shop": {"id": 21}, "prices": {"discount": {"percent": 40}}},
                {"shop": {"id": 25}, "prices": {"discount": {"percent": 29}}},
                {"shop": {"id": 20}, "prices": {"discount": {"percent": 23}}},
                {"shop": {"id": 22}, "prices": {"discount": {"percent": 18}}},
                {"shop": {"id": 11}, "prices": {"discount": {"percent": 15}}},
                {"shop": {"id": 24}, "prices": {"discount": {"percent": 13}}},
                {"shop": {"id": 10}, "prices": {"discount": {"percent": 12}}},
                {"shop": {"id": 26}, "prices": {"discount": {"percent": 11}}},
                {"shop": {"id": 23}, "prices": {"discount": {"percent": 5}}},
            ]
        }

        self.check_search('Горбыль+Деловой', cgi_with_delivery, expected_json, regional_delimiter=False)
        self.check_search('Горбыль+Деловой', cgi_without_delivery, expected_json, regional_delimiter=False)

        # со ставками bid сортировка не изменяется
        self.check_offers(3000, cgi_with_delivery, expected_json, regional_delimiter=False)
        self.check_offers(3000, cgi_without_delivery, expected_json, regional_delimiter=False)

    def test_disable_pbdo_none(self):
        '''Пессимизированые по СиС не выключена для всех'''
        cgi_for_list = '&rids=54&mcpriceto=4600&rearr-factors=market_disable_pessimize_by_delivery_options=none'
        cgi_for_do = (
            '&rids=54&rearr-factors=market_disable_pessimize_by_delivery_options=none&offers-set=default&fesh=23,24'
        )

        expected_json = {
            "results": [
                # local head
                {"shop": {"id": 10}},  # ML 0.03
                {"shop": {"id": 11}},  # ML 0.02
                {"entity": "regionalDelimiter"},
                # non local head
                {"shop": {"id": 20}},  # ML 0.03001
                {"shop": {"id": 21}},  # ML 0.025
                {"shop": {"id": 22}},  # ML 0.02001
                # non local tail - пессимизированые офферы без СиС
                {"shop": {"id": 24}},  # ML 0.023
                {"shop": {"id": 25}},  # ML 0.016
            ]
        }

        expected_json_do = {"results": [{"shop": {"id": 23}}]}

        self.check_offers(3000, cgi_for_list, expected_json)
        self.check_offers(3000, cgi_for_do, expected_json_do, False)

    def test_disable_pbdo_default_offers_only(self):
        '''Пессимизированые по СиС выключена только для ДО'''
        cgi_for_list = (
            '&rids=54&mcpriceto=4600&rearr-factors=market_disable_pessimize_by_delivery_options=default-offers-only'
        )
        cgi_for_do = '&rids=54&rearr-factors=market_disable_pessimize_by_delivery_options=default-offers-only&offers-set=default&fesh=23,24'

        expected_json = {
            "results": [
                # local head
                {"shop": {"id": 10}},  # ML 0.03
                {"shop": {"id": 11}},  # ML 0.02
                {"entity": "regionalDelimiter"},
                # non local head
                {"shop": {"id": 20}},  # ML 0.03001
                {"shop": {"id": 21}},  # ML 0.025
                {"shop": {"id": 22}},  # ML 0.02001
                # non local tail - пессимизированые офферы без СиС
                {"shop": {"id": 24}},  # ML 0.023
                {"shop": {"id": 25}},  # ML 0.016
            ]
        }

        expected_json_do = {"results": [{"shop": {"id": 24}}]}

        self.check_offers(3000, cgi_for_list, expected_json)
        self.check_offers(3000, cgi_for_do, expected_json_do, False)

    def test_disable_pbdo_all(self):
        '''Пессимизированые по СиС выключена только для всех'''
        cgi_for_list = '&rids=54&mcpriceto=4600&rearr-factors=market_disable_pessimize_by_delivery_options=all'
        cgi_for_do = (
            '&rids=54&rearr-factors=market_disable_pessimize_by_delivery_options=all&offers-set=default&fesh=23,24'
        )

        expected_json = {
            "results": [
                # local head
                {"shop": {"id": 10}},  # ML 0.03
                {"shop": {"id": 11}},  # ML 0.02
                {"entity": "regionalDelimiter"},
                # non local
                {"shop": {"id": 20}},  # ML 0.03001
                {"shop": {"id": 21}},  # ML 0.025
                {"shop": {"id": 24}},  # ML 0.023
                {"shop": {"id": 22}},  # ML 0.02001
                {"shop": {"id": 25}},  # ML 0.016
            ]
        }

        expected_json_do = {"results": [{"shop": {"id": 24}}]}

        self.check_offers(3000, cgi_for_list, expected_json)
        self.check_offers(3000, cgi_for_do, expected_json_do, False)


if __name__ == '__main__':
    main()
