#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Currency, HyperCategory, Model, Offer, Shop, MnPlace, ReviewDataItem, GradeDispersionItem
from core.testcase import TestCase, main
from core.types.sku import MarketSku, BlueOffer
from core.types.taxes import Tax


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']
        cls.index.hypertree += [HyperCategory(hid=15723259, fee=123)]

        cls.index.shops += [
            Shop(
                fesh=1,
                datafeed_id=1,
                priority_region=213,
                name='virtual_shop',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                cpa=Shop.CPA_REAL,
            ),
            Shop(
                fesh=2,
                datafeed_id=2,
                priority_region=213,
                name='green_shop',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
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
            ),
        ]

        cls.index.models += [
            Model(hyperid=123456, hid=15723259, title='model'),
        ]

        cls.index.mskus += [
            MarketSku(
                hyperid=123456,
                sku=13,
                title='SkuSearchTest',
                waremd5='8rXbTj04e6jzGqjhE3IpdB',
                blue_offers=[],
                randx=13,
            ),
            # Слово nuclear в последующих СКУ важно для проверки отсутствия нерелевантных СКУ в поиске
            MarketSku(
                hyperid=123456,
                sku=14,
                title='Nuclear warhead',
                waremd5='8rXbTj04e6jzGqjhE3IpdC',
                blue_offers=[],
                randx=14,
                offerid="MS14",
            ),
            MarketSku(
                hyperid=123456,
                sku=15,
                title='Nuclear power plant',
                waremd5='8rXbTj04e6jzGqjhE3IpdD',
                blue_offers=[],
                randx=15,
                offerid="MS15",
            ),
            MarketSku(
                hyperid=123456,
                sku=16,
                title='Nuclear torpedo',
                waremd5='8rXbTj04e6jzGqjhE3IpdE',
                blue_offers=[],
                randx=16,
                offerid="MS16",
            ),
            MarketSku(
                hyperid=123456,
                sku=17,
                title='Nuclear missile',
                waremd5='8rXbTj04e6jzGqjhE3IpdF',
                vendor_string='Атомная прачечная:)',
                blue_offers=[
                    BlueOffer(
                        price=5,
                        feedid=3,
                        offerid='sku17offer1',
                        waremd5='sku17offer1aaaaaaaaaaa',
                    ),
                ],
                randx=17,
                offerid="MS17",
            ),
        ]

        for i in range(20, 28):
            cls.index.mskus.append(
                MarketSku(
                    hyperid=123456,
                    sku=str(i),
                    title='PagingTestSku' + str(i),
                    waremd5='8rXbTj04e6jzGqjhE3Ip' + str(i),
                    blue_offers=[],
                    randx=i,
                    offerid="MS" + str(i),
                )
            )

        cls.index.mskus += [
            # число 17 в СКУ 30 важно - проверяется совпадения числа в title и id другого СКУ
            MarketSku(
                hyperid=123456,
                sku=30,
                title='17 moments of spring movie disk',
                waremd5='8rXbTj04e6jzGqjhE31130',
                blue_offers=[
                    BlueOffer(
                        price=5,
                        feedid=3,
                        offerid='sku30offer1',
                        waremd5='sku30offer1aaaaaaaaaaa',
                    ),
                ],
                randx=30,
            ),
            # слово Suburban важно - по нему проверяется то, что sku_search не отдаёт оферы
            MarketSku(
                hyperid=123456,
                sku=31,
                title='Suburban area on the Moon',
                waremd5='8rXbTj04e6jzGqjhE31131',
                blue_offers=[
                    BlueOffer(
                        price=5,
                        feedid=3,
                        offerid='sku31offer1',
                        waremd5='sku31offer1aaaaaaaaaaa',
                    ),
                    BlueOffer(
                        price=5,
                        feedid=3,
                        offerid='sku31offer2',
                        waremd5='sku31offer2aaaaaaaaaaa',
                    ),
                ],
                randx=31,
            ),
            MarketSku(
                hyperid=123456,
                sku=32,
                title='Suburban area on the Mars',
                waremd5='8rXbTj04e6jzGqjhE31132',
                blue_offers=[
                    BlueOffer(
                        price=5,
                        feedid=3,
                        offerid='sku32offer1',
                        waremd5='sku32offer1aaaaaaaaaaa',
                    ),
                    BlueOffer(
                        price=5,
                        feedid=3,
                        offerid='sku32offer2',
                        waremd5='sku32offer2aaaaaaaaaaa',
                    ),
                ],
                randx=32,
            ),
            MarketSku(
                hyperid=123456,
                sku=33,
                title='The sword of 1000 truths, high damage, takes away all mana',
                waremd5='8rXbTj04e6jzGqjhE31133',
                blue_offers=[
                    BlueOffer(
                        price=5,
                        feedid=3,
                        offerid='sku33offer{:03d}'.format(n),
                        waremd5='sku33offer{:03d}aaaaaaaaa'.format(n),
                    )
                    for n in range(50)
                ],
                randx=33,
            ),
            MarketSku(
                hyperid=123456,
                sku=34,
                title='Iron sword, low damage',
                waremd5='8rXbTj04e6jzGqjhE31134',
                blue_offers=[
                    BlueOffer(
                        price=5,
                        feedid=3,
                        offerid='sku34offer{:03d}'.format(n),
                        waremd5='sku34offer{:03d}aaaaaaaaa'.format(n),
                    )
                    for n in range(50)
                ],
                randx=34,
            ),
        ]

        # Белые оферы в индексе - не должны портить выдачу
        cls.index.offers += [
            Offer(
                title='Suburban area on the Venus',
                fesh=2,
                feedid=2,
                price=3,
                hyperid=123456,
                waremd5='GreenOffer4ggggggggggg',
            ),
            Offer(
                title='17 floor building', fesh=2, price=3, feedid=2, hyperid=123456, waremd5='GreenOffer5ggggggggggg'
            ),
        ]

    def test_sku_search_trivial(self):
        """
        Что проверяем: поиск в плейсе sku_search в самом тривиальном случае.
        Ожидаем что определённый SKU найдётся по определённому текстовому запросу, пока без подробностей о формате.
        Также ожидаем, что не будет других SKU в выдаче
        """
        response = self.report.request_json('place=sku_search&text2=SkuSearchTest')
        self.assertFragmentIn(
            response,
            {
                "total": 1,
                "results": [
                    {
                        "entity": "sku",
                        "titles": {"raw": "SkuSearchTest"},
                    }
                ],
            },
        )

    def test_sku_search_miltiwords(self):
        """
        Что проверяем: поиск в плейсе sku_search в случае, когда в запрос входит частое слово из индекса.
        Ожидаем, что результат по тексту будет наиболее релевантен запросу, без лишних примесей

        В индексе имеются несколько СКУ со словом nuclear в title.
        Ожидается что по запросу Nuclear+power+plant будет отдано только СКУ "Nuclear power plant"
        """

        response = self.report.request_json('place=sku_search&text2=Nuclear+power+plant')
        self.assertFragmentIn(
            response,
            {
                "total": 1,
                "results": [
                    {
                        "entity": "sku",
                        "titles": {"raw": "Nuclear power plant"},
                    }
                ],
            },
        )

    def test_sku_search_several(self):
        """
        Что проверяем: поиск в плейсе sku_search с выдачей нескольких результатов.
        А также релевантность - на текущий момент это просто хэш от offerId (или фактически marketSku)
        """
        response = self.report.request_json('place=sku_search&text2=nuclear')
        self.assertFragmentIn(
            response,
            {
                "total": 4,
                "results": [
                    {"entity": "sku", "titles": {"raw": "Nuclear warhead"}},
                    {"entity": "sku", "titles": {"raw": "Nuclear torpedo"}},
                    {"entity": "sku", "titles": {"raw": "Nuclear missile"}},
                    {"entity": "sku", "titles": {"raw": "Nuclear power plant"}},
                ],
            },
            preserve_order=True,
        )

    def test_raw_vendor(self):
        """
        Что проверяем: в выдаче приезжает "rawName" с нужным значением
        """
        response = self.report.request_json('place=sku_search&text2=nuclear+missile')
        self.assertFragmentIn(
            response,
            {
                "total": 1,
                "results": [
                    {
                        "entity": "sku",
                        "vendor": {"entity": "vendor", "rawName": "Атомная прачечная:)"},
                        "titles": {"raw": "Nuclear missile"},
                    },
                ],
            },
        )

    def test_sku_search_paging(self):
        """
        Что проверяем: пейджинг при поиске в плейсе sku_search. С учётом релевантности по хэшу от offerId
        """
        response = self.report.request_json('place=sku_search&text2=PagingTestSku&numdoc=3')
        self.assertFragmentIn(
            response,
            {
                "total": 8,
                "results": [
                    {"entity": "sku", "titles": {"raw": "PagingTestSku26"}},
                    {"entity": "sku", "titles": {"raw": "PagingTestSku23"}},
                    {"entity": "sku", "titles": {"raw": "PagingTestSku24"}},
                ],
            },
            allow_different_len=False,
            preserve_order=True,
        )

        response = self.report.request_json('place=sku_search&text2=PagingTestSku&numdoc=3&page=2')
        self.assertFragmentIn(
            response,
            {
                "total": 8,
                "results": [
                    {"entity": "sku", "titles": {"raw": "PagingTestSku27"}},
                    {"entity": "sku", "titles": {"raw": "PagingTestSku22"}},
                    {"entity": "sku", "titles": {"raw": "PagingTestSku21"}},
                ],
            },
            allow_different_len=False,
            preserve_order=True,
        )

        response = self.report.request_json('place=sku_search&text2=PagingTestSku&numdoc=3&page=3')
        self.assertFragmentIn(
            response,
            {
                "total": 8,
                "results": [
                    {"entity": "sku", "titles": {"raw": "PagingTestSku20"}},
                    {"entity": "sku", "titles": {"raw": "PagingTestSku25"}},
                ],
            },
            allow_different_len=False,
            preserve_order=True,
        )

    def test_sku_search_by_id(self):
        """
        Что проверяем: поиск в плейсе sku_search по market_sku id (воспринимаем его как число из текста)
        """

        # обычный случай - SKU 15 имеет title = "Nuclear power plant"
        response = self.report.request_json('place=sku_search&text2=15')
        self.assertFragmentIn(
            response,
            {
                "total": 1,
                "results": [
                    {"entity": "sku", "titles": {"raw": "Nuclear power plant"}},
                ],
            },
        )

        # текст с пробелами - SKU 16 имеет title = "Nuclear torpedo"
        response = self.report.request_json('place=sku_search&text2=%20%2016%20%20%20')
        self.assertFragmentIn(
            response,
            {
                "total": 1,
                "results": [
                    {"entity": "sku", "titles": {"raw": "Nuclear torpedo"}},
                ],
            },
        )

        # id СКУ, который встречается как число также в тексте других СКУ - должны получить вывод для всех случаев.
        # Также в индексе есть белый офер с числом 17 в названии. Проверяется, что он не влияет на выдачу.
        # Если этот белый офер вернётся с базового, в выдаче будет ошибка вместо данных.
        response = self.report.request_json('place=sku_search&text2=17')
        self.assertFragmentIn(
            response,
            {
                "total": 2,
                "results": [
                    {"entity": "sku", "titles": {"raw": "Nuclear missile"}},
                    {"entity": "sku", "titles": {"raw": "17 moments of spring movie disk"}},
                ],
            },
        )

    def test_sku_search_no_offers(self):
        """
        Что проверяем:
        Поиск в плейсе sku_search не должен выдавать синие и белые оферы ни отдельно, ни внутри СКУ.
        Проверяется также, что из базового поиска на метапоиск приходят ТОЛЬКО market_sku записи.
        В проитвном случае репорт отдаст ошибку.
        """

        # в индексе есть синие и белые оферы со словом Suburban
        response = self.report.request_json('place=sku_search&text2=Suburban')
        self.assertFragmentIn(
            response,
            {
                "total": 2,
                "results": [
                    {"entity": "sku", "titles": {"raw": "Suburban area on the Moon"}, "offers": {}},
                    {"entity": "sku", "titles": {"raw": "Suburban area on the Mars"}, "offers": {}},
                ],
            },
            allow_different_len=False,
        )

    def test_sku_search_output_format(self):
        """
        Что проверяем: формат выдачи плейса sku_search - наличие нужных данных,
        пока не много - sku-id, title, категория
        """

        response = self.report.request_json('place=sku_search&text2=SkuSearchTest')
        self.assertFragmentIn(
            response,
            {
                "total": 1,
                "results": [
                    {
                        "entity": "sku",
                        "id": "13",
                        "titles": {
                            "raw": "SkuSearchTest",
                        },
                        "categories": [
                            {
                                "entity": "category",
                                "id": 15723259,
                            }
                        ],
                    }
                ],
            },
        )

    def test_sku_search_pruning(self):
        """
        Что проверяем: что прюнинг не приводит к искажению выдачи.
        В индексе есть несколько msku с большим количеством оферов.

        Все не-msku записи должны отфильтровываться через search literal. Нельзя фильтровать на релевантности,
        т.к. релевнтность происходит после прюнинга, и при включенном прюнинге может оказаться например 999 синих оферов и 1 msku
        - тогда данные будут незаметно теряться.

        Этот тест проверяет такой случай.
        """
        response = self.report.request_json('place=sku_search&text2=sword&prun-count=2')
        self.assertFragmentIn(
            response,
            {
                "total": 2,
                "results": [
                    {
                        "entity": "sku",
                        "titles": {"raw": "The sword of 1000 truths, high damage, takes away all mana"},
                        "offers": {},
                    },
                    {"entity": "sku", "titles": {"raw": "Iron sword, low damage"}, "offers": {}},
                ],
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_mixed_output_sorting(cls):
        cls.index.models += [
            Model(hyperid=1111, hid=10007, ts=1010101, title='MODEL BEFORE SKU'),
            Model(hyperid=2222, hid=10007, ts=2020202, title='HIDDEN BY SKU'),
            Model(hyperid=3333, hid=10007, ts=3030303, title='MODEL AFTER SKU'),
        ]

        cls.index.mskus += [
            MarketSku(sku=1212, hyperid=2222, ts=4040404, hid=10007, title='MODEL HIDING SKU'),
        ]

        cls.index.offers = [
            Offer(hid=10007, price=10, ts=100007, hyperid=1111, title='FIRST_OFFER_BEFORE_SKU'),
            Offer(hid=10007, price=11, ts=100008, hyperid=1111, title='SECOND_OFFER_BEFORE_SKU'),
            Offer(sku=1212, hid=10007, ts=100009, price=12, hyperid=2222, title='FIRST_OFFER_OF_SKU'),
            Offer(sku=1212, hid=10007, ts=100010, price=100500, hyperid=2222, title='SECOND_OFFER_OF_SKU'),
            Offer(hid=10007, price=13, ts=100011, hyperid=3333, title='FIRST_OFFER_AFTER_SKU'),
            Offer(hid=10007, price=14, ts=100012, hyperid=3333, title='SECOND_OFFER_AFTER_SKU'),
        ]

        for place in [MnPlace.BASE_SEARCH, MnPlace.META_REARRANGE]:
            for ts in [100007, 100009, 100011]:
                cls.matrixnet.on_place(place, ts).respond(0.1)
            for ts in [100008, 100010, 100012]:
                cls.matrixnet.on_place(place, ts).respond(0.9)

    def test_mixed_output_sorting(self):
        correctResponseFragWithoutDO = {
            'search': {
                'results': [
                    {'entity': 'product', 'id': 1111},
                    {'entity': 'sku', 'id': '1212'},
                    {'entity': 'product', 'id': 3333},
                ]
            }
        }
        requestWithoutDO = (
            'place=prime&hid=10007&use-default-offers=0&text=MODEL&how=aprice&'
            'rearr-factors=market_metadoc_search=skus&'
            'rearr-factors=market_replace_metadoc_id_with_child_id=%d'
        )
        for replaceFlag in [0, 1]:
            response = self.report.request_json(requestWithoutDO % replaceFlag)
            self.assertFragmentIn(
                response,
                correctResponseFragWithoutDO,
                preserve_order=True,
                allow_different_len=False,
            )

        correctResponseFragWithDO = {
            'search': {
                'results': [
                    {
                        'entity': 'product',
                        'id': 1111,
                        'offers': {'items': [{'titles': {'raw': 'SECOND_OFFER_BEFORE_SKU'}}]},
                    },
                    {
                        'entity': 'product',
                        'id': 3333,
                        'offers': {'items': [{'titles': {'raw': 'SECOND_OFFER_AFTER_SKU'}}]},
                    },
                    {'entity': 'sku', 'id': '1212', 'offers': {'items': [{'titles': {'raw': 'SECOND_OFFER_OF_SKU'}}]}},
                ]
            }
        }
        requestWithDO = (
            'place=prime&hid=10007&use-default-offers=1&text=MODEL&how=aprice&'
            'rearr-factors=market_metadoc_search=skus&'
            'rearr-factors=market_replace_metadoc_id_with_child_id=%d&'
        )
        for replaceFlag in [0, 1]:
            response = self.report.request_json(requestWithDO % replaceFlag)
            self.assertFragmentIn(
                response,
                correctResponseFragWithDO,
                preserve_order=True,
                allow_different_len=False,
            )

    @classmethod
    def prepare_sku_search_with_reviews(cls):

        cls.index.models += [
            Model(hyperid=2143231121, hid=561, title='Sku-non-coupled model'),
            Model(hyperid=2144231121, hid=561, title='Coupled model'),
        ]

        cls.index.mskus += [
            MarketSku(sku=2146231121, hyperid=2144231121, hid=561, title='Sku'),
        ]

        cls.index.offers += [
            Offer(hyperid=2143231121, title='Offer one', price=101),
            Offer(sku=2146231121, hyperid=2144231121, title='Offer sku-be-two', price=102),
        ]

        cls.index.model_reviews_data += [
            ReviewDataItem(
                review_id=2317231121,
                model_id=2143231121,
                author_id=12345,
                pro='works',
                contra='sometimes',
                short_text='better than nothing',
                agree=5,
                reject=5,
                total_votes=10,
                grade_value=4,
                most_useful=1,
            ),
            ReviewDataItem(
                review_id=2318231121,
                model_id=2144231121,
                author_id=23456,
                pro='concise',
                contra='almost',
                short_text='helps more than disturbs',
                agree=99,
                reject=1,
                total_votes=100,
                grade_value=4,
                most_useful=1,
            ),
        ]

        cls.index.model_grade_dispersion_data += [
            GradeDispersionItem(model_id=2143231121),
            GradeDispersionItem(model_id=2144231121),
        ]

    def test_sku_search_with_reviews(self):
        request = (
            'place=prime&'
            'hid=561&'
            'text=sku&'
            'use-default-offers=1&'
            'allow-collapsing=1&'
            'rearr-factors=market_metadoc_search=skus&'
            'rearr-factors=enable_sku_search_with_reviews=%d&'
            'show-reviews=1'
        )

        response = self.report.request_json(request % 1)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {'entity': 'product', 'id': 2143231121, 'review': {'id': 2317231121}},
                        {'entity': 'sku', 'id': '2146231121', 'review': {'id': 2318231121}},
                    ]
                }
            },
            preserve_order=False,
            allow_different_len=False,
        )

        response = self.report.request_json(request % 0)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {'entity': 'product', 'id': 2143231121, 'review': {'id': 2317231121}},
                        {'entity': 'product', 'id': 2144231121, 'review': {'id': 2318231121}},
                    ]
                }
            },
            preserve_order=False,
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()
