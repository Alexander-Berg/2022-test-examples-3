#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BlueOffer,
    Book,
    Currency,
    DeliveryBucket,
    DeliveryCalendar,
    DeliveryOption,
    DynamicSkuOffer,
    GLParam,
    GLType,
    MarketSku,
    MnPlace,
    Model,
    ModelGroup,
    Offer,
    RegionalDelivery,
    Shop,
    SortingCenterReference,
    Tax,
    VCluster,
)
from core.matcher import NotEmpty, Contains, Absent, Greater, Round
from core.testcase import TestCase, main
from unittest import skip


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_new_cpm_iterator=0']
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

    @classmethod
    def prepare_trace_offers(cls):
        cls.index.shops += [
            Shop(fesh=1, priority_region=213),
            Shop(fesh=2, priority_region=2),
            Shop(
                fesh=3,
                priority_region=213,
                datafeed_id=1,
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                regions=[225],
                warehouse_id=145,
            ),
            Shop(
                fesh=400,
                datafeed_id=400,
                priority_region=2,
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
            ),
            Shop(
                fesh=100,
                datafeed_id=100,
                priority_region=213,
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
            ),
        ]

        cls.index.gltypes += [
            GLType(param_id=201, hid=1, gltype=GLType.ENUM, values=[1, 2]),
        ]

        cls.index.offers += [
            # 1. Есть в трейсе, есть в выдаче
            Offer(
                title='Карандаш кубиковый',
                url='books.ru/karandash',
                offer_url_hash='16218401816209216025',
                hid=1,
                fesh=1,
                glparams=[GLParam(param_id=201, value=1)],
                ts=1,
            ),
            # 2. Нет в трейсе, есть в выдаче
            Offer(
                title='Карандаш кубиковый 2',
                url='books.ru/karandash2',
                offer_url_hash='0000000000000000000',
                hid=1,
                fesh=1,
                glparams=[GLParam(param_id=201, value=1)],
                ts=2,
            ),
            # 3. Есть в трейсе, нет в выдаче
            # 3.1 Не попадает в релевантность
            Offer(
                title='Ручка шариковая',
                url='books.ru/ruchka',
                offer_url_hash='6525834649490936657',
                hid=2,
                waremd5='RPaDqEFjs1I6_lfC4Ai8jA',
                fesh=1,
                ts=3,
            ),
            # 3.2 Отфильтровывается по доставке
            Offer(
                title='Карандаш кубиковый 3',
                url='books.ru/karandash3',
                offer_url_hash='16977619441796023784',
                hid=1,
                fesh=2,
                glparams=[GLParam(param_id=201, value=1)],
                ts=4,
            ),
            # 3.3 Отфильтровывается гл-фильтром
            Offer(
                title='Карандаш тетраэдровый',
                url='books.ru/karandash4',
                offer_url_hash='2604135194776626513',
                hid=1,
                waremd5='qtZDmKlp7DGGgA1BL6erMQ',
                fesh=1,
                glparams=[GLParam(param_id=201, value=2)],
                ts=5,
            ),
        ]

        # 4. Синий оффер, присутствующий на белом Маркете. Трейсится как обычный
        cls.index.mskus += [
            MarketSku(
                title='Карандаш синий',
                blue_offers=[BlueOffer(waremd5='DuE098x_rinQLZn3KKrELw')],
                glparams=[GLParam(param_id=201, value=1)],
                offer_url_hash='7970319880973520256',
                hid=1,
                has_gone=True,
                sku=200001,
                hyperid=300001,
                ts=6,
            )
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1).respond(0.31)

    def test_trace_offers(self):
        """
        Проверяем, что работает трейс поиска документа для офферов
        """

        expected = {
            'books.ru/karandash': {
                "document": "books.ru/karandash",
                "type": "OFFER_BY_URL",
                "in_index": True,
                "in_accept_doc": True,
                "panther_doc_rel": Greater(0),
                "passed_accept_doc": True,
                "in_relevance": True,
                "base_formula_value": Round(0.31, 2),
                "passed_relevance": True,
                "in_rearrange": True,
                "on_page": True,
                "passed_rearrange": True,
            },
            "RPaDqEFjs1I6_lfC4Ai8jA": {
                "document": "RPaDqEFjs1I6_lfC4Ai8jA",
                "type": "OFFER_BY_WARE_MD5",
                "in_index": True,
                "in_accept_doc": False,
            },
            "ololo.ru/azaza?a=b": {"document": "ololo.ru/azaza?a=b", "type": "OFFER_BY_URL", "in_index": False},
            "books.ru/karandash3": {
                "document": "books.ru/karandash3",
                "type": "OFFER_BY_URL",
                "in_index": True,
                "in_accept_doc": True,
                "panther_doc_rel": Greater(0),
                "passed_accept_doc": True,
                "in_relevance": True,
                "passed_relevance": False,
                "relevance_filtered_reason": "DELIVERY",
            },
            "qtZDmKlp7DGGgA1BL6erMQ": {
                "document": "qtZDmKlp7DGGgA1BL6erMQ",
                "type": "OFFER_BY_WARE_MD5",
                "in_index": True,
                "in_accept_doc": True,
                "panther_doc_rel": Greater(0),
                "passed_accept_doc": True,
                "in_relevance": True,
                "passed_relevance": False,
                "relevance_filtered_reason": "GURULIGHT",
            },
            "beru.ru/product/200001?offerid=DuE098x_rinQLZn3KKrELw": {
                "document": "beru.ru/product/200001?offerid=DuE098x_rinQLZn3KKrELw",
                "type": "OFFER_BY_URL",  # Not BLUE_OFFER as it is traced by url hash
                "in_index": True,
                "in_accept_doc": True,
                "panther_doc_rel": Greater(0),
                "passed_accept_doc": False,
                "accept_doc_filtered_reason": "OFFER_HAS_GONE",
            },
        }

        for rgb_param in ('', '&rgb=green', '&rgb=green_with_blue'):
            for key, value in expected.items():
                query = (
                    'place=prime&text=карандаш+кубиковый&debug=da&rids=213&hid=1&glfilter=201:1&{}'
                    '&rearr-factors=market_metadoc_search=no'
                ).format(rgb_param) + '&rearr-factors=market_documents_search_trace={}'.format(key)

                response = self.report.request_json(query)
                self.assertFragmentIn(
                    response, {"docs_search_trace": {"traces": [value]}}, allow_different_len=False, preserve_order=True
                )

    def test_trace_offers_in_case_redirect(self):
        """
        Проверяем, что работает трейс поиска документа для офферов даже если будет редирект
        """

        expected = {
            'books.ru/karandash': {
                "document": "books.ru/karandash",
                "type": "OFFER_BY_URL",
                "in_index": True,
                "in_accept_doc": True,
                "panther_doc_rel": Greater(0),
                "passed_accept_doc": True,
                "in_relevance": True,
                "base_formula_value": Round(0.31, 2),
                "passed_relevance": True,
                "in_rearrange": False,
                "on_page": Absent(),
                "passed_rearrange": Absent(),
            },
            "RPaDqEFjs1I6_lfC4Ai8jA": {
                "document": "RPaDqEFjs1I6_lfC4Ai8jA",
                "type": "OFFER_BY_WARE_MD5",
                "in_index": True,
                "in_accept_doc": False,
            },
            "ololo.ru/azaza?a=b": {"document": "ololo.ru/azaza?a=b", "type": "OFFER_BY_URL", "in_index": False},
            "books.ru/karandash3": {
                "document": "books.ru/karandash3",
                "type": "OFFER_BY_URL",
                "in_index": True,
                "in_accept_doc": True,
                "panther_doc_rel": Greater(0),
                "passed_accept_doc": True,
                "in_relevance": True,
                "passed_relevance": False,
                "relevance_filtered_reason": "DELIVERY",
            },
            "qtZDmKlp7DGGgA1BL6erMQ": {
                "document": "qtZDmKlp7DGGgA1BL6erMQ",
                "type": "OFFER_BY_WARE_MD5",
                "in_index": True,
                "in_accept_doc": True,
                "panther_doc_rel": Greater(0),
                "passed_accept_doc": True,
                "in_relevance": True,
                "passed_relevance": True,
                "in_rearrange": False,
            },
            "beru.ru/product/200001?offerid=DuE098x_rinQLZn3KKrELw": {
                "document": "beru.ru/product/200001?offerid=DuE098x_rinQLZn3KKrELw",
                "type": "OFFER_BY_URL",
                "in_index": True,
                "in_accept_doc": True,
                "panther_doc_rel": Greater(0),
                "passed_accept_doc": False,
                "accept_doc_filtered_reason": "OFFER_HAS_GONE",
            },
        }

        for rgb_param in ('', '&rgb=green', '&rgb=green_with_blue'):
            for key, value in expected.items():
                query = (
                    'place=prime&text=карандаш+кубиковый&debug=da&rids=213&cvredirect=1{}'
                    '&rearr-factors=market_metadoc_search=no'
                ).format(rgb_param) + '&rearr-factors=market_documents_search_trace={}'.format(key)
                response = self.report.request_json(query)
                self.assertFragmentIn(response, {"redirect": {"params": {"hid": ["1"]}}})
                self.assertFragmentIn(
                    response, {"docs_search_trace": {"traces": [value]}}, allow_different_len=False, preserve_order=True
                )

    def test_trace_single_offer_has_gone(self):
        '''
        Проверяем, что трасировка работает, даже если нет ни одного офера на выдаче
        '''
        response = self.report.request_json(
            'place=prime&text=карандаш+кубиковый&debug=da&rids=213&hid=1&glfilter=201:1&rgb=blue'
            '&rearr-factors=market_documents_search_trace=DuE098x_rinQLZn3KKrELw'
        )
        self.assertFragmentIn(
            response,
            {
                "document": "DuE098x_rinQLZn3KKrELw",
                "type": "OFFER_BY_WARE_MD5",
                "in_index": True,
                "in_accept_doc": True,
                "panther_doc_rel": Greater(0),
                "passed_accept_doc": False,
                "accept_doc_filtered_reason": "OFFER_HAS_GONE",
            },
        )

    @classmethod
    def prepare_subrequests(cls):
        cls.speller.on_default_request().respond(originalText=None, fixedText=None)
        cls.speller.on_request(text='могила').respond(
            originalText='мо<fix>г</fix>ила',
            fixedText='мо<fix>б</fix>ила',
        )

        cls.index.offers += [Offer(title='мобила', url='chotko.ru/mobila', offer_url_hash='2076763204320763359', ts=7)]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 7).respond(0.92)

    def test_subrequests(self):
        """
        Проверяем, что на подзапросах в репорте всё тоже корректно трейсится
        """
        rearr_factors = "rearr-factors=market_documents_search_trace=chotko.ru/mobila"
        response = self.report.request_json('place=prime&text=могила&debug=da&{}'.format(rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "search": {"results": [{"entity": "offer", "titles": {"raw": "мобила"}}]},
                "debug": {
                    # Запрос с неисправленной опечаткой. Документ не нагребается пантерой
                    "docs_search_trace": {
                        "traces": [
                            {
                                "document": "chotko.ru/mobila",
                                "type": "OFFER_BY_URL",
                                "in_index": True,
                                "in_accept_doc": False,
                            }
                        ]
                    },
                    "metasearch": {
                        "subrequests": [
                            "debug",
                            NotEmpty(),  # подзапрос для проверки наличия документов в индексе (поле in_index)
                            "debug",
                            {
                                # Запрос с неисправленной опечаткой - вернул пустой серп
                                "docs_search_trace": {
                                    "traces": [
                                        {
                                            "document": "chotko.ru/mobila",
                                            "type": "OFFER_BY_URL",
                                            "in_index": True,
                                            "in_accept_doc": False,
                                        }
                                    ]
                                }
                            },
                            "debug",
                            {
                                # Запрос с исправленной опечаткой
                                "docs_search_trace": {
                                    "traces": [
                                        {
                                            "document": "chotko.ru/mobila",
                                            "type": "OFFER_BY_URL",
                                            "in_index": True,
                                            "in_accept_doc": True,
                                            "panther_doc_rel": Greater(0),
                                            "passed_accept_doc": True,
                                            "in_relevance": True,
                                            "base_formula_value": Round(0.92, 2),
                                            "passed_relevance": True,
                                            "in_rearrange": True,
                                            "on_page": True,
                                            "passed_rearrange": True,
                                        }
                                    ]
                                }
                            },
                        ]
                    },
                },
            },
            allow_different_len=False,
            preserve_order=True,
        )

    @classmethod
    def prepare_same_offer_url_hashes(cls):
        cls.index.offers += [
            # 2 следующих оффера различаются только utm_-параметром и будут иметь одинаковые хеши
            Offer(title='пенал', url='books.ru/penal?utm_geo=1', offer_url_hash='11093424550027755081'),
            Offer(title='пенал 2', url='books.ru/penal?utm_geo=2', offer_url_hash='11093424550027755081'),
            Offer(title='пенал 3', url='books.ru/penal?color=white', offer_url_hash='5862858135919582778'),
        ]

    def test_same_offer_url_hashes(self):
        """
        Проверяем, что при наличии нескольких одинаковых offer_url_hash в индексе выводится сообщение об ошибке
        """

        rearr_factors = (
            "rearr-factors=market_documents_search_trace="
            "books.ru/penal?utm_geo=1,"
            "books.ru/penal?utm_geo=2,"
            "books.ru/penal?color=white"
        )

        response = self.report.request_json('place=prime&text=пенал&debug=da&{}'.format(rearr_factors))

        self.assertFragmentIn(
            response, {"docs_search_trace": {"is_error": True, "error_msg": Contains("is not unique in index")}}
        )

    @classmethod
    def prepare_trace_models(cls):
        cls.index.vclusters += [VCluster(vclusterid=1000000101, title='Школьная форма в линейку', ts=8)]

        cls.index.models += [
            Model(hyperid=100, title='Линейка какая-то', ts=9),
            Model(hyperid=201, title='Кривейка', ts=11),
        ]

        cls.index.books += [Book(title='Книжка про линейки', hyperid=300, hid=90829, isbn='978-5-905463-15-0')]

        # Приматчены к визуальному кластеру
        for i in range(200):
            # 50 из Москвы, 150 из Питера
            fesh = 1
            if i >= 50:
                fesh = 2

            cls.index.offers += [
                Offer(
                    title='Школьная форма в линейку {}'.format(i),
                    vclusterid=1000000101,
                    url='forma{}.ru'.format(i),
                    fesh=fesh,
                    waremd5=('BH8EPLtKmdLQ-forma%03dw' % (i,)),
                )
            ]

        # 1 из Москвы, 1 из Питера, приматчены к групповой модели
        cls.index.offers += [
            Offer(
                title='Линейка какая-то 1',
                hyperid=100,
                url='grouplineika1.ru',
                fesh=1,
                waremd5='yRgmzyB-grouplineika1w',
            ),
            Offer(
                title='Линейка какая-то 2',
                hyperid=100,
                url='grouplineika2.ru',
                fesh=2,
                waremd5='yRgmzyB-grouplineika2w',
            ),
        ]

        # Приматчены к книге
        for i in range(2):
            cls.index.offers += [
                Offer(
                    title='Книжка про линейки {}'.format(i),
                    hyperid=300,
                    url='knigalineiki{}.ru'.format(i),
                    fesh=2,
                    waremd5='EpnWVxD-knigalineiki{}w'.format(i),
                )
            ]

        # Приматчены к модели, которая не нагребётся в релевантность
        for i in range(300):
            cls.index.offers += [
                Offer(
                    title='Кривейка, но не линейка {}'.format(i),
                    hyperid=201,
                    url='kriveika{}.ru'.format(i),
                    fesh=1,
                    waremd5=('yRgmzyBD4-kriveika%03dw' % (i,)),
                )
            ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 8).respond(0.32)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 9).respond(0.43)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 10).respond(0.54)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 11).respond(0.65)

    def test_trace_models(self):
        """
        Проверяем трейс кластеров, модификаций, визуальных моделей, книг
        """

        expected = {
            "1000000101": {
                "document": "1000000101",
                "type": "MODEL",
                "in_index": True,
                "in_accept_doc": True,
                "panther_doc_rel": Greater(0),
                "passed_accept_doc": True,
                "in_relevance": True,
                "base_formula_value": Round(0.32, 2),
                "passed_relevance": True,
                "in_rearrange": True,
                "on_page": True,
                "passed_rearrange": True,
                "stats_for_model": {
                    "model_id": 1000000101,
                    "offers_in_index": {"count": 200, "examples": [Contains("BH8EPLtKmdLQ-forma")] * 3},
                    "offers_in_accept_doc": {"1": {"count": 200, "examples": [Contains("BH8EPLtKmdLQ-forma")] * 3}},
                    "offers_passed_accept_doc": {"1": {"count": 200, "examples": [Contains("BH8EPLtKmdLQ-forma")] * 3}},
                    "offers_in_relevance": {"1": {"count": 200, "examples": [Contains("BH8EPLtKmdLQ-forma")] * 3}},
                    "offers_passed_relevance": {
                        "1": {
                            "count": 50,  # 200 - 150 из Питера
                            "examples": [Contains("BH8EPLtKmdLQ-forma")] * 3,
                        },
                        "0": {"count": 150, "examples": [Contains("BH8EPLtKmdLQ-forma")] * 3},
                    },
                    "offers_relevance_filtered_reason": {
                        "DELIVERY": {"count": 150, "examples": [Contains("BH8EPLtKmdLQ-forma")] * 3},
                    },
                },
            },
            "100": {
                "document": "100",
                "type": "MODEL",
                "in_index": True,
                "in_accept_doc": True,
                "panther_doc_rel": Greater(0),
                "passed_accept_doc": True,
                "in_relevance": True,
                "base_formula_value": Round(0.43, 2),
                "passed_relevance": True,
                "in_rearrange": True,
                "on_page": True,
                "passed_rearrange": True,
                "stats_for_model": {
                    "model_id": 100,
                    "offers_in_index": {
                        # 2 из модели с hyperid = 100
                        "count": 2,
                        "examples": ["yRgmzyB-grouplineika1w", "yRgmzyB-grouplineika2w"],
                    },
                    "offers_in_accept_doc": {
                        "1": {"count": 2, "examples": ["yRgmzyB-grouplineika1w", "yRgmzyB-grouplineika2w"]}
                    },
                    "offers_passed_accept_doc": {
                        "1": {"count": 2, "examples": ["yRgmzyB-grouplineika1w", "yRgmzyB-grouplineika2w"]}
                    },
                    "offers_in_relevance": {
                        "1": {"count": 2, "examples": ["yRgmzyB-grouplineika1w", "yRgmzyB-grouplineika2w"]}
                    },
                    "offers_passed_relevance": {
                        "1": {"count": 1, "examples": ["yRgmzyB-grouplineika1w"]},
                        "0": {
                            # один из офферов, приматченных к групповой модели, из Питера
                            "count": 1,
                            "examples": ["yRgmzyB-grouplineika2w"],
                        },
                    },
                    "offers_relevance_filtered_reason": {
                        "DELIVERY": {"count": 1, "examples": ["yRgmzyB-grouplineika2w"]}
                    },
                },
            },
            "117": {
                "document": "117",
                "type": "MODEL",
                "in_index": False,
            },
            "201": {
                "document": "201",
                "type": "MODEL",
                "in_index": True,
                "in_accept_doc": False,
                "on_page": False,
                "passed_rearrange": Absent(),
                "stats_for_model": {
                    "model_id": 201,
                    "offers_in_index": {"count": 300, "examples": [Contains("yRgmzyBD4-kriveika")] * 3},
                    "offers_in_accept_doc": {"1": {"count": 300, "examples": [Contains("yRgmzyBD4-kriveika")] * 3}},
                    "offers_passed_accept_doc": {"1": {"count": 300, "examples": [Contains("yRgmzyBD4-kriveika")] * 3}},
                    "offers_in_relevance": {"1": {"count": 300, "examples": [Contains("yRgmzyBD4-kriveika")] * 3}},
                    "offers_passed_relevance": {"1": {"count": 300, "examples": [Contains("yRgmzyBD4-kriveika")] * 3}},
                },
            },
            "300": {
                "document": "300",
                "type": "MODEL",
                "in_index": True,
                "in_accept_doc": True,
                "panther_doc_rel": Greater(0),
                "passed_accept_doc": True,
                "in_relevance": True,
                "passed_relevance": True,
                "in_rearrange": True,
                "stats_for_model": {
                    "model_id": 300,
                    "offers_in_index": {"count": 2, "examples": ["EpnWVxD-knigalineiki0w", "EpnWVxD-knigalineiki1w"]},
                    "offers_in_accept_doc": {
                        "1": {"count": 2, "examples": ["EpnWVxD-knigalineiki0w", "EpnWVxD-knigalineiki1w"]}
                    },
                    "offers_passed_accept_doc": {
                        "1": {"count": 2, "examples": ["EpnWVxD-knigalineiki0w", "EpnWVxD-knigalineiki1w"]}
                    },
                    "offers_in_relevance": {
                        "1": {"count": 2, "examples": ["EpnWVxD-knigalineiki0w", "EpnWVxD-knigalineiki1w"]}
                    },
                    "offers_passed_relevance": {
                        "0": {"count": 2, "examples": ["EpnWVxD-knigalineiki0w", "EpnWVxD-knigalineiki1w"]}
                    },
                    "offers_relevance_filtered_reason": {
                        "DELIVERY": {"count": 2, "examples": ["EpnWVxD-knigalineiki0w", "EpnWVxD-knigalineiki1w"]}
                    },
                },
            },
        }

        for rgb_param in ('', '&rgb=green', '&rgb=green_with_blue'):
            for key, trace in expected.items():
                response = self.report.request_json(
                    'place=prime&text=линейка&rids=213&debug=da'
                    '&rearr-factors=panther_offer_tpsz=512;market_documents_search_trace={}{}'.format(key, rgb_param)
                )

                # Проверяем содержимое с preserve_order=False
                self.assertFragmentIn(
                    response,
                    {"docs_search_trace": {"traces": [trace]}},
                    allow_different_len=False,
                    preserve_order=False,
                )

        for rgb_param in ('', '&rgb=green', '&rgb=green_with_blue'):
            # Книга не забанится, если запросить книжную категорию
            key = "300"
            response = self.report.request_json(
                'place=prime&text=линейка&rids=213&hid=90829&debug=da'
                '&rearr-factors=panther_offer_tpsz=512;market_documents_search_trace={}{}'.format(key, rgb_param)
            )

            self.assertFragmentIn(
                response,
                {
                    "document": key,
                    "in_relevance": True,
                    "passed_relevance": True,
                    "in_rearrange": True,
                    "on_page": True,
                    "passed_rearrange": True,
                },
            )

    def test_trace_doc_in_blue(self):

        response = self.report.request_json(
            "place=prime&rgb=BLUE&text=iphone&rids=213&pp=18&debug=da&rearr-factors=market_documents_search_trace=msku:100210861745"
        )
        self.assertFragmentIn(
            response,
            {"docs_search_trace": {"traces": [{"document": "100210861745", "type": "MSKU", "in_index": False}]}},
        )

    def test_nothing_to_trace(self):
        """
        Проверяем, что, если в список документов не было передано ни одного, трейса нет
        """

        response = self.report.request_json('place=prime&text=линейка&rids=213&debug=da')
        self.assertFragmentNotIn(response, {"docs_search_trace": {}})

    @classmethod
    def prepare_blue_market_offers(cls):
        cls.index.shipment_service_calendars += [
            DeliveryCalendar(
                fesh=3,
                calendar_id=1111,
                date_switch_hour=20,
                holidays=[7, 8, 9, 10, 11, 12, 13],
                is_sorting_center=True,
            ),
            DeliveryCalendar(
                fesh=3, calendar_id=257, sc_references=[SortingCenterReference(sc_id=1111, duration=0, default=True)]
            ),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=901,
                dc_bucket_id=1,
                fesh=1,
                carriers=[257],
                regional_options=[
                    RegionalDelivery(
                        rid=213,
                        options=[
                            DeliveryOption(price=5, day_from=1, day_to=2, shop_delivery_price=10),
                            DeliveryOption(price=3, day_from=4, day_to=5, shop_delivery_price=10),
                        ],
                    ),
                    RegionalDelivery(
                        rid=2,
                        options=[
                            DeliveryOption(price=5, day_from=1, day_to=2, shop_delivery_price=10),
                            DeliveryOption(price=3, day_from=4, day_to=5, shop_delivery_price=10),
                        ],
                    ),
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
        ]

        cls.index.models += [
            Model(hyperid=500001, hid=1, title="Транспортир 1"),
            Model(hyperid=500002, hid=1, title="Транспортир 2"),
        ]

        cls.index.gltypes = [
            GLType(param_id=401, hid=1, gltype=GLType.ENUM, values=[1, 2]),
        ]

        cls.index.mskus += [
            MarketSku(
                title='Транспортир зелёный',
                blue_offers=[
                    BlueOffer(waremd5='BH8EPLtKmdLQhLUasgaOnA', price=1, ts=12),
                    BlueOffer(waremd5='KXGI8T3GP_pqjgdd7HfoHQ', price=2),
                ],
                glparams=[GLParam(param_id=401, value=1)],
                sku=700001,
                hyperid=500001,
                delivery_buckets=[901],
            ),
            MarketSku(
                title='Транспортир синий',
                blue_offers=[BlueOffer(waremd5='_qQnWXU28-IUghltMZJwNw')],
                glparams=[GLParam(param_id=401, value=2)],
                sku=700002,
                hyperid=500002,
                delivery_buckets=[901],
            ),
            MarketSku(
                title='Скрытый транспортир',
                blue_offers=[BlueOffer(waremd5='pCl2on9YL4fCV8poq57hRg')],
                has_gone=True,
                sku=700003,
                hyperid=500001,
                delivery_buckets=[901],
            ),
            MarketSku(
                title='Транспортир динамо',
                blue_offers=[BlueOffer(waremd5='91t1fTRZw-k-mN2re5A5OA', offerid='aaabbbbb1111122222', feedid=100)],
                sku=700004,
                glparams=[GLParam(param_id=401, value=1)],
                hyperid=500001,
                delivery_buckets=[901],
            ),
            MarketSku(
                title='Транспортир на предзаказ',
                blue_offers=[BlueOffer(waremd5='TTnVlqbztMi95ithBNMa3g', offerid='64843435455fffffff', feedid=100)],
                sku=700005,
                hyperid=500001,
                delivery_buckets=[901],
            ),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 12).respond(0.33)

    def test_blue_market_offers(self):
        """
        Проверяем трассировку поиска офферов на синем Маркете
        """

        expected = {
            # Оффер найдётся
            "BH8EPLtKmdLQhLUasgaOnA,": {
                "document": "BH8EPLtKmdLQhLUasgaOnA",
                "type": "OFFER_BY_WARE_MD5",
                "in_index": True,
                "in_accept_doc": True,
                "panther_doc_rel": Greater(0),
                "passed_accept_doc": True,
                "in_relevance": True,
                "base_formula_value": Round(0.33, 2),
                "passed_relevance": True,
            },
            # Оффер не пройдёт релевантность, т.к. в его SKU есть более дешёвый
            "KXGI8T3GP_pqjgdd7HfoHQ,": {
                "document": "KXGI8T3GP_pqjgdd7HfoHQ",
                "type": "OFFER_BY_WARE_MD5",
                "in_index": True,
                "in_accept_doc": True,
                "panther_doc_rel": Greater(0),
                "passed_accept_doc": True,
                "in_relevance": True,
                "passed_relevance": False,
                "relevance_filtered_reason": "NOT_BEST_OFFER_IN_SKU",
            },
            # Оффер не в индексе
            "Ff57RdkwTkr04EREdiDSf4,": {
                "document": "Ff57RdkwTkr04EREdiDSf4",
                "type": "OFFER_BY_WARE_MD5",
                "in_index": False,
            },
            # Оффер не пройдёт релевантность из-за значения глфильтра в его SKU
            "_qQnWXU28-IUghltMZJwNw,": {
                "document": "_qQnWXU28-IUghltMZJwNw",
                "type": "OFFER_BY_WARE_MD5",
                "in_index": True,
                "in_accept_doc": True,
                "panther_doc_rel": Greater(0),
                "passed_accept_doc": True,
                "in_relevance": True,
                "passed_relevance": False,
                "relevance_filtered_reason": "GURULIGHT",
            },
            # Следующие офферы отфильтруются в AcceptDocWithHits
            # Оффер скрыт
            "pCl2on9YL4fCV8poq57hRg,": {
                "document": "pCl2on9YL4fCV8poq57hRg",
                "type": "OFFER_BY_WARE_MD5",
                "in_index": True,
                "in_accept_doc": True,
                "panther_doc_rel": Greater(0),
                "passed_accept_doc": False,
                "accept_doc_filtered_reason": "OFFER_HAS_GONE",
            },
            # Оффер отключен динамиком sku-feedid
            "91t1fTRZw-k-mN2re5A5OA,": {
                "document": "91t1fTRZw-k-mN2re5A5OA",
                "type": "OFFER_BY_WARE_MD5",
                "in_index": True,
                "in_accept_doc": True,
                "panther_doc_rel": Greater(0),
                "passed_accept_doc": False,
                "accept_doc_filtered_reason": "OFFER_DISABLED",
            },
            # Оффер с предзаказом
            "TTnVlqbztMi95ithBNMa3g": {
                "document": "TTnVlqbztMi95ithBNMa3g",
                "type": "OFFER_BY_WARE_MD5",
                "in_index": True,
                "in_accept_doc": True,
                "panther_doc_rel": Greater(0),
                "passed_accept_doc": False,
                "accept_doc_filtered_reason": "SHOP_SKU_PREORDER",
            },
        }

        self.dynamic.disabled_sku_offers += [
            DynamicSkuOffer(shop_id=100, sku="aaabbbbb1111122222"),
        ]
        self.dynamic.preorder_sku_offers = [DynamicSkuOffer(shop_id=100, sku="64843435455fffffff")]

        for key, trace in expected.items():

            response = self.report.request_json(
                'place=prime&rids=213&rgb=blue&text=транспортир&hid=1&glfilter=401:1&debug=da&disable-rotation-on-blue=1&'
                '&rearr-factors=market_blue_buybox_max_price_rel_add_diff=0;market_documents_search_trace={}'.format(
                    key
                )
            )

            self.assertFragmentIn(
                response, {"docs_search_trace": {"traces": [trace]}}, allow_different_len=False, preserve_order=True
            )

    @classmethod
    def prepare_blue_market_skus(cls):
        cls.index.models += [
            Model(hyperid=510001, hid=1, title="Циркуль 1"),
            Model(hyperid=510002, hid=1, title="Циркуль 2"),
        ]

        cls.index.gltypes = [
            GLType(param_id=901, hid=1, gltype=GLType.ENUM, values=[1, 2]),
        ]

        cls.index.mskus += [
            MarketSku(
                title='Циркуль острый',
                blue_offers=[BlueOffer(waremd5='Y9_iwgA17yd3FCI0LRhC_w', price=2)],
                glparams=[GLParam(param_id=901, value=1)],
                sku=710001,
                hyperid=510001,
                delivery_buckets=[901],
            ),
            MarketSku(
                title='Циркуль очень острый',
                blue_offers=[BlueOffer(waremd5='8rXbTj04e6jzGqjhE3IpdA', price=2)],
                glparams=[GLParam(param_id=901, value=1)],
                has_gone=True,
                sku=710002,
                hyperid=510001,
                delivery_buckets=[901],
            ),
            MarketSku(
                title='Циркули оптом',
                blue_offers=[BlueOffer() for i in range(99)],
                glparams=[GLParam(param_id=901, value=1)],
                sku=710003,
                hyperid=510002,
                delivery_buckets=[901],
            ),
            MarketSku(
                title='Циркули оптом 2',
                blue_offers=[
                    BlueOffer(offerid=('64843435455ffffffg' if i == 15 else None), feedid=400) for i in range(25)
                ],
                glparams=[GLParam(param_id=901, value=2)],
                sku=710004,
                hyperid=510002,
                delivery_buckets=[901],
            ),
            MarketSku(
                title='Странный тайтл без слова на букву Ц',
                blue_offers=[BlueOffer() for i in range(11)],
                glparams=[GLParam(param_id=901, value=1)],
                sku=710005,
                hyperid=510002,
                delivery_buckets=[901],
            ),
        ]

        # Приматченный к модели белый оффер. Проверяем, что учтётся только в offers_in_index.count
        cls.index.offers += [Offer(title='Цикруль белый', hyperid=510001, fesh=2, waremd5='nx1WWdWID7Qn9uBK5QD8JQ')]

    def test_blue_market_mskus(self):
        """
        Проверяем трассировку поиска SKU на синем Маркете
        Для самого SKU трассируется только факт наличия его в индексе
        Весь остальной трейс -- офферы модели, к которой относится SKU
        """

        self.dynamic.preorder_sku_offers = [DynamicSkuOffer(shop_id=400, sku="64843435455ffffffg")]

        stats_for_model_51001 = {
            "model_id": 510001,
            "offers_in_index": {
                "count": 3,  # 2 синих и 1 белый
                "examples": ["Y9_iwgA17yd3FCI0LRhC_w", "8rXbTj04e6jzGqjhE3IpdA", "nx1WWdWID7Qn9uBK5QD8JQ"],
            },
            "offers_in_accept_doc": {
                "1": {
                    "count": 2,  # Сюда уже попали только синие
                    "examples": ["Y9_iwgA17yd3FCI0LRhC_w", "8rXbTj04e6jzGqjhE3IpdA"],
                }
            },
            "offers_passed_accept_doc": {
                "1": {"count": 1, "examples": ["Y9_iwgA17yd3FCI0LRhC_w"]},
                "0": {"count": 1, "examples": ["8rXbTj04e6jzGqjhE3IpdA"]},
            },
            "offers_accept_doc_filtered_reason": {
                "OFFER_HAS_GONE": {"count": 1, "examples": ["8rXbTj04e6jzGqjhE3IpdA"]}
            },
            "offers_in_relevance": {"1": {"count": 1, "examples": ["Y9_iwgA17yd3FCI0LRhC_w"]}},
            "offers_passed_relevance": {
                "1": {"count": 1, "examples": ["Y9_iwgA17yd3FCI0LRhC_w"]},
            },
        }

        stats_for_model_51002 = {
            "model_id": 510002,
            "offers_in_index": {"count": 135},  # 99 из 710003 + 25 из 710004 + 11 из 710005
            "offers_in_accept_doc": {
                "1": {"count": 99}  # 11 офферов не нагреблись пантерой 25 не прошли раннюю фильтрацию
            },
            "offers_passed_accept_doc": {"1": {"count": 99}},
            "offers_in_relevance": {"1": {"count": 99}},
            "offers_passed_relevance": {"1": {"count": 1}, "0": {"count": 98}},
            "offers_relevance_filtered_reason": {
                "NOT_BEST_OFFER_IN_SKU": {
                    "count": 98  # Только по одному офферу из каждого SKU -- байбоксы, остальные отбрасываются
                }
            },
        }

        expected = {
            "msku:710001": {
                "document": "710001",
                "type": "MSKU",
                "in_index": True,
                "stats_for_model": stats_for_model_51001,
            },
            "msku:710002": {
                "document": "710002",
                "type": "MSKU",
                "in_index": True,
                "stats_for_model": stats_for_model_51001,
            },
            "msku:720001": {"document": "720001", "type": "MSKU", "in_index": False},
            "msku:710004": {
                "document": "710004",
                "type": "MSKU",
                "in_index": True,
                "stats_for_model": stats_for_model_51002,
            },
            "8rXbTj04e6jzGqjhE3IpdA": {
                "document": "8rXbTj04e6jzGqjhE3IpdA",
                "type": "OFFER_BY_WARE_MD5",
                "in_index": True,
                "in_accept_doc": True,
                "panther_doc_rel": Greater(0),
                "passed_accept_doc": False,
                "accept_doc_filtered_reason": "OFFER_HAS_GONE",
            },
        }

        for key, trace in expected.items():

            response = self.report.request_json(
                'place=prime&rids=2&rgb=blue&text=циркуль&hid=1&glfilter=901:1&disable-rotation-on-blue=1&debug=da&'
                '&rearr-factors=market_documents_search_trace={}'.format(key)
            )

            self.assertFragmentIn(
                response, {"docs_search_trace": {"traces": [trace]}}, allow_different_len=False, preserve_order=False
            )

    @classmethod
    def prepare_white_meta(cls):
        cls.index.vclusters += [VCluster(vclusterid=1000000201, title='Галстук для пионера', ts=13)]

        cls.index.model_groups += [ModelGroup(hyperid=800, title='Скоросшиватель для пионера', ts=14)]

        cls.index.models += [
            Model(hyperid=801, group_hyperid=800, title='Скоросшиватель картонный для пионера'),
            Model(hyperid=802, title='Закладки в хорошем смысле для пионеров', ts=16),
            Model(hyperid=803, title='Канцтовар без слова на букву П в тайтле', ts=17),
        ]

        cls.index.shops += [
            Shop(fesh=5, priority_region=2, regions=[225]),
            Shop(fesh=6, priority_region=213, regions=[225]),
            Shop(fesh=7, priority_region=213, regions=[225]),
            Shop(fesh=8, priority_region=2, regions=[225]),
            Shop(fesh=11, priority_region=213, regions=[225]),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=12225,
                fesh=5,
                carriers=[99],
                regional_options=[RegionalDelivery(rid=225, options=[DeliveryOption(price=100)])],
            ),
            DeliveryBucket(
                bucket_id=12226,
                fesh=6,
                carriers=[99],
                regional_options=[RegionalDelivery(rid=225, options=[DeliveryOption(price=100)])],
            ),
        ]

        cls.index.offers += [
            Offer(
                title='Галстук для пионера от gucci',
                url='gucci.com/galstuk',
                offer_url_hash='6869253586669411815',
                waremd5='FSqiKO1icV4qzU-I7w8qLg',
                vclusterid=1000000201,
                ts=18,
                fesh=5,
                has_delivery_options=True,
                delivery_options=[DeliveryOption(price=100, day_from=0, day_to=2, order_before=24)],
                delivery_buckets=[12225],
            ),
            Offer(
                title='Галстук для пионера от prada',
                url='prada.com/galstuk',
                offer_url_hash='1646053124991867432',
                waremd5='fzn4MX-9sZiO9MYo66AlkQ',
                vclusterid=1000000201,
                ts=19,
                fesh=8,
            ),
            Offer(
                title='Галстук для пионера от dolce & gabbana',
                url='dolcengabbana.com/galstuk',
                offer_url_hash='3082177343164004632',
                waremd5='09lEaAKkQll1XTaaaaaaaQ',
                vclusterid=1000000201,
                ts=21,
                fesh=6,
            ),
            Offer(
                title='Закладки в хорошем смысле для пионеров мексиканские',
                fesh=7,
                hyperid=802,
                waremd5='aOgVX7lvufxf5cgCc5gaTA',
                ts=22,
            ),
            Offer(
                title='Канцтовар со словом пионер в тайтле',
                url='kanztovary.ru/for-pioneers/1',
                offer_url_hash='17793261747001440769',
                waremd5='4ZJOjrcwdAOLvqXioL_6QA',
                hyperid=803,
                fesh=6,
                has_delivery_options=True,
                delivery_options=[DeliveryOption(price=100, day_from=0, day_to=2, order_before=24)],
                delivery_buckets=[12226],
            ),
        ]

        cls.matrixnet.on_place(MnPlace.META_REARRANGE, 13).respond(0.8)
        cls.matrixnet.on_place(MnPlace.META_REARRANGE, 14).respond(0.73)
        cls.matrixnet.on_place(MnPlace.META_REARRANGE, 16).respond(0.76)
        cls.matrixnet.on_place(MnPlace.META_REARRANGE, 18).respond(0.77)
        cls.matrixnet.on_place(MnPlace.META_REARRANGE, 21).respond(0.79)
        cls.matrixnet.on_place(MnPlace.META_REARRANGE, 22).respond(0.63)

    def __check_trace(self, query, expected):
        # сначала проверяем что каждый отдельный документ трейсится (для удобства нахождения ошибок)
        for docid, trace in expected.items():
            response = self.report.request_json(query.format(docid))
            self.assertFragmentIn(response, trace, allow_different_len=False)

        docids = ','.join(expected.keys())
        traces = {"docs_search_trace": {"traces": list(expected.values())}}

        response = self.report.request_json(query.format(docids))
        self.assertFragmentIn(response, traces, allow_different_len=False)

    def test_white_meta_no_collapsing(self):
        """
        Проверяем, что трассируется метапоиск для белого маркета для всех типов документов
        """

        expected = {
            # Кластер попадёт в head, есть офферы из приоритетного региона => DocRangeHead
            "1000000201": {
                "document": "1000000201",
                "type": "MODEL",
                "in_rearrange": True,
                "filtered_with_priorities": [
                    # Кластер попадал в хвост и отфильтровался там как уже обработанный
                    # При этом он попадал и в голову и там прошёл переранжирование
                    # (см. ниже passed_rearrange True)
                    {"filter_reason": Contains("doc not uniq by ts"), "priority": "DocRangeTail"}
                ],
                "priority": "DocRangeHead",
                "meta_formula_value": Round(0.8, 2),
                "bucket": 0,
                "on_page": True,
                "passed_rearrange": True,
                "stats_for_model": {
                    "offers_in_rearrange": {
                        "1": {
                            "count": 3,
                            "examples": ["FSqiKO1icV4qzU-I7w8qLg", "fzn4MX-9sZiO9MYo66AlkQ", "09lEaAKkQll1XTaaaaaaaQ"],
                        }
                    }
                },
            },
            # То же самое с моделями
            "model:802": {
                "document": "802",
                "type": "MODEL",
                "in_rearrange": True,
                "filtered_with_priorities": [
                    {"filter_reason": Contains("doc not uniq by ts"), "priority": "DocRangeTail"}
                ],
                "priority": "DocRangeHead",
                "meta_formula_value": Round(0.76, 2),
                "bucket": 0,
                "on_page": True,
                "passed_rearrange": True,
                "stats_for_model": {"offers_in_rearrange": {"1": {"count": 1, "examples": ["aOgVX7lvufxf5cgCc5gaTA"]}}},
            },
            # Без схлопывания не попадёт в переранжирование совсем
            "model:803": {
                "document": "803",
                "type": "MODEL",
                "in_rearrange": Absent(),
                "bucket": Absent(),
                "on_page": False,
                "passed_rearrange": Absent(),
                "stats_for_model": {
                    "offers_in_rearrange": {"1": {"count": 1, "examples": ["4ZJOjrcwdAOLvqXioL_6QA"]}},
                    "offers_meta_filtered_reason": {
                        "duplicate picture": {"count": 1, "examples": ["4ZJOjrcwdAOLvqXioL_6QA"]}
                    },
                },
            },
            # Есть delivery options, но не приоритетный регион => DocRangeNonLocalHead
            "gucci.com/galstuk": {
                "document": "gucci.com/galstuk",
                "type": "OFFER_BY_URL",
                "in_rearrange": True,
                "filtered_with_priorities": [
                    {"filter_reason": Contains("doc not uniq by ts"), "priority": "DocRangeNonLocalTail"}
                ],
                "priority": "DocRangeNonLocalHead",
                "meta_formula_value": Round(0.77, 2),
                "bucket": 2,
                "on_page": True,
                "passed_rearrange": True,
            },
            # Нет delivery options и неприоритетный регион (не пессимизируется) => DocRangeNonLocalHead
            "fzn4MX-9sZiO9MYo66AlkQ": {
                "document": "fzn4MX-9sZiO9MYo66AlkQ",
                "type": "OFFER_BY_WARE_MD5",
                "in_rearrange": True,
                "filtered_with_priorities": [
                    {"filter_reason": Contains("doc not uniq by ts"), "priority": "DocRangeNonLocalTail"}
                ],
                "priority": "DocRangeNonLocalHead",
                "meta_formula_value": Round(0.3),
                "bucket": 2,
                "on_page": True,
                "passed_rearrange": True,
            },
            # Есть delivery options, приоритетный регион => DocRangeHead
            "dolcengabbana.com/galstuk": {
                "document": "dolcengabbana.com/galstuk",
                "type": "OFFER_BY_URL",
                "in_rearrange": True,
                "filtered_with_priorities": [
                    {"filter_reason": Contains("doc not uniq by ts"), "priority": "DocRangeTail"}
                ],
                "priority": "DocRangeHead",
                "meta_formula_value": Round(0.79, 2),
                "bucket": 0,
                "on_page": True,
                "passed_rearrange": True,
            },
            # Есть delivery options, но не приоритетный регион => DocRangeTail
            "4ZJOjrcwdAOLvqXioL_6QA": {
                "document": "4ZJOjrcwdAOLvqXioL_6QA",
                "type": "OFFER_BY_WARE_MD5",
                "in_rearrange": True,
                "filtered_with_priorities": [{"filter_reason": "duplicate picture", "priority": "DocRangeHead"}],
                "priority": "DocRangeTail",
                "meta_formula_value": Absent(),
                "bucket": 1,
                "on_page": True,
                "passed_rearrange": True,
            },
        }

        query = (
            'place=prime&text=пионер&debug=da&rids=213&numdoc=20&'
            + 'rearr-factors=market_force_search_auction=No;market_documents_search_trace={}'
        )

        for rgb_param in ('', '&rgb=green', '&rgb=green_with_blue'):
            self.__check_trace(query + rgb_param, expected)

    @skip('MARKETOUT-29780')
    def test_white_meta_with_collapsing(self):
        """
        Проверяем, что трассируется метапоиск для белого маркета для всех типов документов со включенным схлопыванием
        """

        expected = {
            # Схлопнется из своих двух офферов (кроме prada.com/galstuk (fzn4MX-9sZiO9MYo66AlkQ), причины см. ниже),
            # но, т.к. кластер имеет на мете самое высокое значение переранжирующей формулы, схлопнутые будут
            # просмотрены позже исходного документа и отфильтрованы как дубликаты
            "1000000201": {
                "document": "1000000201",
                "type": "MODEL",
                "in_rearrange": True,
                "filtered_with_priorities": [
                    {
                        "filter_reason": "doc not uniq by ts",
                        "priority": "DocRangeHead",
                        "collapsed_from": "09lEaAKkQll1XTaaaaaaaQ",
                        "meta_formula_value": 0.79,
                    },
                    {"filter_reason": "doc not uniq by ts", "priority": "DocRangeTail"},
                    {
                        "filter_reason": "doc not uniq by ts",
                        "priority": "DocRangeTail",
                        "collapsed_from": "09lEaAKkQll1XTaaaaaaaQ",
                    },
                    {
                        "filter_reason": "doc not uniq by ts",
                        "priority": "DocRangeNonLocalTail",
                        "collapsed_from": "fzn4MX-9sZiO9MYo66AlkQ",
                    },
                    {
                        "filter_reason": "doc not uniq by ts",
                        "priority": "DocRangeNonLocalTail",
                        "collapsed_from": "FSqiKO1icV4qzU-I7w8qLg",
                    },
                ],
                "priority": "DocRangeHead",
                "collapsed_from": Absent(),  # победил несхлопнутый, см. prepare_white_meta
                "meta_formula_value": Round(0.8, 2),
                "bucket": 0,
                "on_page": True,
                "passed_rearrange": True,
                "stats_for_model": {
                    "offers_in_rearrange": {
                        "1": {
                            "count": 3,
                            "examples": ["FSqiKO1icV4qzU-I7w8qLg", "fzn4MX-9sZiO9MYo66AlkQ", "09lEaAKkQll1XTaaaaaaaQ"],
                        }
                    }
                },
            },
            # Не из чего схлапываться, модель просто попадёт в head. В tail из-за отсутствия офферов она тоже попытается
            # попасть в NonLocalTail, но отфильтруется как дубликат
            "https://market.yandex.ru/product/800": {
                "document": "https://market.yandex.ru/product/800",
                "type": "MODEL",
                "in_rearrange": True,
                "filtered_with_priorities": [
                    {"filter_reason": "doc not uniq by ts", "priority": "DocRangeNonLocalTail"}
                ],
                "priority": "DocRangeHead",
                "meta_formula_value": Round(0.73, 2),
                "on_page": True,
                "passed_rearrange": True,
            },
            # Попадёт в head сама и схлопнется в head; схлопнутая версия отфильтруется как дубликат
            "802": {
                "document": "802",
                "type": "MODEL",
                "in_rearrange": True,
                "filtered_with_priorities": [
                    {
                        "filter_reason": "doc not uniq by ts",
                        "priority": "DocRangeHead",
                        "collapsed_from": "aOgVX7lvufxf5cgCc5gaTA",
                        "meta_formula_value": Round(0.63, 2),
                    },
                    {
                        "filter_reason": "doc not uniq by ts",
                        "priority": "DocRangeTail",
                    },
                ],
                "priority": "DocRangeHead",
                "meta_formula_value": Round(0.76, 2),
                "bucket": 0,
                "on_page": True,
                "passed_rearrange": True,
                "stats_for_model": {
                    "offers_in_rearrange": {"1": {"count": 1, "examples": ["aOgVX7lvufxf5cgCc5gaTA"]}},
                    "offers_meta_filtered_reason": {
                        "Not in grouping _Name_hyper_ts for model 802": {
                            "count": 1,
                            "examples": ["aOgVX7lvufxf5cgCc5gaTA"],
                        }
                    },
                },
            },
            # Схлопнется из оффера, у которого регион пользователя приоритетный, и унаследует от него приоритет DocRangeHead
            # Повторно попытается схлопнуться из того же оффера по группировке ts и будет отфильтрована по уникальному ts
            "803": {
                "document": "803",
                "type": "MODEL",
                "in_rearrange": Absent(),
                # тот же оффер по
                "filtered_with_priorities": [
                    {
                        "filter_reason": "doc not uniq by ts",
                        "priority": "DocRangeTail",
                        "collapsed_from": "4ZJOjrcwdAOLvqXioL_6QA",
                    }
                ],
                "priority": "DocRangeHead",
                "collapsed_from": "4ZJOjrcwdAOLvqXioL_6QA",
                "bucket": NotEmpty(),
                "on_page": True,
                "passed_rearrange": True,
                "stats_for_model": {"offers_in_rearrange": {"1": {"count": 1, "examples": ["4ZJOjrcwdAOLvqXioL_6QA"]}}},
            },
            # При схлапывании схлопнется в кластер и не попадёт в passed_rearrange
            "gucci.com/galstuk": {
                "document": "gucci.com/galstuk",
                "type": "OFFER_BY_URL",
                "in_index": True,
                "in_accept_doc": True,
                "passed_accept_doc": True,
                "in_relevance": True,
                "passed_relevance": True,
                "in_rearrange": True,
                "collapsed_to": 1000000201,
                "priority": "DocRangeNonLocalTail",
            },
            # Схлопнется в кластер и не попадёт в passed_rearrange, вытеснится из группировки
            # hyper_ts своим кластером
            "dolcengabbana.com/galstuk": {
                "document": "dolcengabbana.com/galstuk",
                "type": "OFFER_BY_URL",
                "in_index": True,
                "in_accept_doc": True,
                "passed_accept_doc": True,
                "in_relevance": True,
                "passed_relevance": True,
                "in_rearrange": True,
                "collapsed_to": 1000000201,
                "priority": "DocRangeTail",
            },
            # Схлопнется в модель несмотря на дублирующуюся картинку
            "4ZJOjrcwdAOLvqXioL_6QA": {
                "document": "4ZJOjrcwdAOLvqXioL_6QA",
                "type": "OFFER_BY_WARE_MD5",
                "in_index": True,
                "in_accept_doc": True,
                "passed_accept_doc": True,
                "in_relevance": True,
                "passed_relevance": True,
                "in_rearrange": True,
                "collapsed_to": 803,
                "priority": "DocRangeTail",
            },
        }

        query = (
            'place=prime&text=пионер&debug=da&rids=213&numdoc=20&allow-collapsing=1'
            + '&rearr-factors=market_force_search_auction=No;market_documents_search_trace={}'
        )

        for rgb_param in ('', '&rgb=green', '&rgb=green_with_blue'):
            self.__check_trace(query + rgb_param, expected)

    @classmethod
    def prepare_white_meta_modification(cls):
        cls.index.model_groups += [ModelGroup(hyperid=900, hid=3, title='Штангенциркуль')]

        cls.index.models += [Model(hyperid=901, hid=3, group_hyperid=900, title='Нониусный штангенциркуль')]

        cls.index.gltypes += [GLType(param_id=1, hid=3, gltype=GLType.ENUM, values=[1, 2], cluster_filter=True)]

        cls.index.shops += [Shop(fesh=13, priority_region=213, regions=[225])]

        cls.index.offers += [
            Offer(title='Штангенциркуль зелёненький', fesh=13, hyperid=900, glparams=[GLParam(param_id=1, value=1)]),
            Offer(
                title='Нониусный штангенциркуль оранжевенький',
                fesh=13,
                hyperid=901,
                glparams=[GLParam(param_id=1, value=1)],
            ),
        ]

    @classmethod
    def prepare_not_on_page(cls):
        base_ts = 200
        base_fesh = 50
        offer_url_hashes = (None, None, None, '5810717825544390785', '7573111454538973337')
        for i in range(5):
            fesh = base_fesh + i
            cls.index.shops += [Shop(fesh=fesh, priority_region=2, regions=[225])]

            ts = base_ts + i
            cls.index.offers += [
                Offer(
                    title='Промокашка {}'.format(i),
                    ts=ts,
                    url='promokashka{}.ru'.format(i),
                    offer_url_hash=offer_url_hashes[i],
                    fesh=fesh,
                )
            ]
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, ts).respond(0.8 - 0.1 * i)

    def test_not_on_page(self):
        """
        Покрываем тестом ситуацию, когда оффер проходит все перипетии переранжирования,
        но не попадает на страницу
        """

        expected = {
            "promokashka3.ru": {
                "document": "promokashka3.ru",
                "type": "OFFER_BY_URL",
                "on_page": True,
                "priority": "DocRangeHead",
                "passed_rearrange": True,
            },
            "promokashka4.ru": {
                "document": "promokashka4.ru",
                "type": "OFFER_BY_URL",
                "priority": "DocRangeHead",
                "on_page": False,
                "passed_rearrange": Absent(),
            },
        }

        query = (
            'place=prime&text=промокашка&debug=da&rids=2&numdoc=2&page=2&rearr-factors=market_documents_search_trace={}'
        )

        for rgb_param in ('', '&rgb=green', '&rgb=green_with_blue'):
            self.__check_trace(query + rgb_param, expected)

    @classmethod
    def prepare_blue_meta(cls):

        cls.index.models += [
            Model(hyperid=900001, hid=2, title="Чернильница 1"),
            Model(hyperid=900002, hid=2, title="Чернильница 2"),
        ]

        cls.index.gltypes = [
            GLType(param_id=1001, hid=2, gltype=GLType.ENUM, values=[1, 2]),
        ]

        cls.index.mskus += [
            MarketSku(
                title='Чернильница непроливайка',
                blue_offers=[BlueOffer(waremd5='otENNVzevIeeT8bsxvY91w', ts=900)],
                glparams=[GLParam(param_id=1001, value=1)],
                sku=1010001,
                hyperid=900001,
            ),
            MarketSku(
                title='Чернильница проливайка',
                blue_offers=[BlueOffer(waremd5='EWkt-tvUywoY9K1ALCjeuA', ts=1000)],
                glparams=[GLParam(param_id=1001, value=2)],
                sku=1010002,
                hyperid=900001,
            ),
            MarketSku(
                title='Чернильница проливайка чёрная',
                blue_offers=[BlueOffer(waremd5='xzFUFhFuAvI1sVcwDnxXPQ')],
                glparams=[GLParam(param_id=1001, value=2)],
                sku=1010003,
                hyperid=900002,
            ),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1000).respond(0.8)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 900).respond(0.6)

    def test_blue_meta(self):
        """
        Проверяем, что трассируется метапоиск для синего маркета для всех типов документов
        Отдельно тестируем флаг dsk_product_ungroup=new
        """

        for ungrouping_enabled in (True, False):
            expected = {
                # Не попадёт в переранжирование, т.к. попадёт другой более релевантный оффер той же модели
                # (EWkt-tvUywoY9K1ALCjeuA)
                "otENNVzevIeeT8bsxvY91w": {
                    "document": "otENNVzevIeeT8bsxvY91w",
                    "type": "OFFER_BY_WARE_MD5",
                    "in_rearrange": False,
                },
                # Попадёт в переранжирование с приоритетом DocRangeHead, т.к. не пессимизирован по доставке
                "EWkt-tvUywoY9K1ALCjeuA": {
                    "document": "EWkt-tvUywoY9K1ALCjeuA",
                    "type": "OFFER_BY_WARE_MD5",
                    "passed_rearrange": Absent(),
                },
                # Модель этой sku получится схлопыванием из EWkt-tvUywoY9K1ALCjeuA (более релевантного, чем
                # otENNVzevIeeT8bsxvY91w), оффер этой sku не попадёт на выдачу в офферы модели
                "msku:1010001": {
                    "document": "1010001",
                    "type": "MSKU",
                    "model_in_rearrange": Absent(),
                    "model_collapsed_from": "EWkt-tvUywoY9K1ALCjeuA",
                    "model_priority": "DocRangeHead",
                    "model_on_page": True,
                    "model_passed_rearrange": True,
                    "sku_in_default_offer": False,
                    "stats_for_model": {
                        "model_id": 900001,
                    },
                },
                # То же самое, только оффер этой sku попадёт на выдачу в офферы модели
                "msku:1010002": {
                    "document": "1010002",
                    "type": "MSKU",
                    "model_in_rearrange": Absent(),
                    "model_collapsed_from": "EWkt-tvUywoY9K1ALCjeuA",
                    "model_priority": "DocRangeHead",
                    "model_on_page": True,
                    "model_passed_rearrange": True,
                    "sku_in_default_offer": True,
                    "stats_for_model": {
                        "model_id": 900001,
                    },
                },
                # Модель этой sku получится схлопыванием из xzFUFhFuAvI1sVcwDnxXPQ
                "msku:1010003": {
                    "document": "1010003",
                    "type": "MSKU",
                    "model_in_rearrange": Absent(),
                    "model_collapsed_from": "xzFUFhFuAvI1sVcwDnxXPQ",
                    "model_priority": "DocRangeHead",
                    "model_on_page": True,
                    "model_passed_rearrange": True,
                    "sku_in_default_offer": True,
                    "stats_for_model": {
                        "model_id": 900002,
                    },
                },
            }

            rearr = '&rearr-factors=market_documents_search_trace={}' + (
                ';dsk_product_ungroup=new' if ungrouping_enabled else ''
            )
            query = 'place=prime&rgb=blue&text=чернильница&debug=da&rids=213&allow-collapsing=1' + rearr
            self.__check_trace(query, expected)

    @classmethod
    def prepare_filtered_by_threshold(cls):
        cls.index.models += [
            Model(hyperid=800001, hid=4, title="Клей 1", ts=23),
            Model(hyperid=800002, hid=4, title="Клей 2", ts=24),
        ]

        cls.index.offers += [
            Offer(title='Клей 3', url='books.ru/kley1', offer_url_hash='12480920924627574622', hid=4, ts=25),
            Offer(title='Клей 4', url='books.ru/kley2', offer_url_hash='9937559984269022890', hid=4, ts=26),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 23).respond(0.4)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 24).respond(0.6)

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 25).respond(0.4)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 26).respond(0.6)

    def test_filtered_by_threshold(self):
        """
        Проверяем, что при фильтрации на базовом даже по порогу base_formula_value НЕ пишется в трейс
        """
        rearr = '&rearr-factors=market_relevance_formula_threshold=0.5;market_documents_search_trace={}'.format(
            ','.join(
                [
                    '800001',
                    '800002',
                    'books.ru/kley1',
                    'books.ru/kley2',
                ]
            )
        )

        response = self.report.request_json('place=prime&text=клей&debug=da' + rearr)

        for doc in '800001', 'books.ru/kley1':
            self.assertFragmentIn(
                response,
                {
                    "document": doc,
                    "in_index": True,
                    "in_accept_doc": True,
                    "panther_doc_rel": Greater(0),
                    "passed_accept_doc": True,
                    "in_relevance": True,
                    "base_formula_value": Absent(),
                    "passed_relevance": False,
                    "relevance_filtered_reason": "RELEVANCE_THRESHOLD",
                },
            )

        for doc in '800002', 'books.ru/kley2':
            self.assertFragmentIn(
                response,
                {
                    "document": doc,
                    "in_index": True,
                    "in_accept_doc": True,
                    "panther_doc_rel": Greater(0),
                    "passed_accept_doc": True,
                    "in_relevance": True,
                    "base_formula_value": Round(0.6, 2),
                    "passed_relevance": True,
                    "relevance_filtered_reason": Absent(),
                },
            )

    def test_trace_in_productoffers(self):
        """
        Проверяем, что трейс работает и в productoffers https://st.yandex-team.ru/MARKETOUT-40959
        Проверяем только то, что он пишется. Подробные проверки трейсов поиска в productoffers лежат в тесте test_buybox_with_dsbs.py
        """
        request = 'place=productoffers&rgb=green_with_blue&hyperid=300001&pp=6&rids=213&debug=da&allow_collapsing=0&offers-set=defaultList'
        request += '&rearr-factors=market_documents_search_trace_offers_list=300001;market_documents_search_trace_default_offer=300001'
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "docs_search_trace_default_offer": {
                    "traces": [
                        {
                            "document": "300001",
                            "type": "MODEL",
                            "in_index": NotEmpty(),
                            "in_accept_doc": NotEmpty(),
                            "on_page": NotEmpty(),
                            "stats_for_model": NotEmpty(),
                        },
                    ],
                },
                "docs_search_trace_offers_list": {
                    "traces": [
                        {
                            "document": "300001",
                            "type": "MODEL",
                            "in_index": NotEmpty(),
                            "in_accept_doc": NotEmpty(),
                            "on_page": NotEmpty(),
                            "stats_for_model": NotEmpty(),
                        },
                    ],
                },
            },
        )

    def test_meta_formula_in_shows_log(self):
        """
        Проверяем, что мета-формула записывается в shows log (для моделей и офферов)
        """

        query = 'place=prime&text=пионер&debug=da&rids=213&numdoc=20&' + 'rearr-factors=market_force_search_auction=No'

        self.report.request_json(query)
        self.show_log_tskv.expect(
            meta_formula_value=Round(0.8, 2),  # Модель 1000000201
        )
        self.show_log_tskv.expect(
            meta_formula_value=Round(0.77, 2),  # Оффер gucci.com/galstuk
        )

    @classmethod
    def prepare_metadoc_search(cls):
        cls.index.mskus += [
            MarketSku(
                sku=123,
                title='кулебяка',
                blue_offers=[
                    BlueOffer(
                        title='кулебяка 1',
                        price=1,
                        waremd5='DuE098x_rinRLZn3KKrELw',
                        offerid='aaabbbbb1111122223',
                        feedid=100,
                    ),
                    BlueOffer(title='кулебяка 2', price=2, waremd5='yRgmzyBD4j8r4rkCby6Iuw'),
                    BlueOffer(title='кулебяка 3', price=3, waremd5='bpQ3a9LXZAl_Kz34vaOpSg'),
                ],
            ),
            MarketSku(
                sku=456,
                title='кулебяка ещё одна',
                blue_offers=[BlueOffer(), BlueOffer(), BlueOffer(), BlueOffer()],
            ),
            MarketSku(
                sku=678,
                title='кулебяка пофильтрованная',
                hid=15723259,
                blue_offers=[
                    BlueOffer(waremd5='22222222222222gggg401g', hid=15723259),
                ],
            ),
            MarketSku(
                sku=901,
                title='кулебяка неопубликованная',
                published=False,
            ),
        ]

    def test_metadoc_search(self):
        """
        Проверяем, что поиск по ску работает с трейсером поиска
        """

        self.dynamic.disabled_sku_offers += [
            DynamicSkuOffer(shop_id=100, sku="aaabbbbb1111122223"),
        ]

        self.__check_trace(
            (
                'place=prime&text=кулебяка&debug=da&rearr-factors=market_documents_search_trace={};'
                'metadoc_effective_pruncount=3;contex_on_blue=1'
            ),
            {
                'msku:123': {
                    'type': 'MSKU',
                    'in_index': True,
                    'in_accept_doc': True,
                    'in_metadoc': False,
                    'child_selected_by': 'COMPOSITE',
                    'stats_for_metadoc': {
                        'offers_in_metadoc': {
                            '1': {
                                'count': 3,
                                'examples': [
                                    'DuE098x_rinRLZn3KKrELw',
                                    'yRgmzyBD4j8r4rkCby6Iuw',
                                    'bpQ3a9LXZAl_Kz34vaOpSg',
                                ],
                            }
                        },
                        'offers_metadoc_filtered_reason': {
                            'OFFER_DISABLED': {
                                'count': 1,
                                'examples': ['DuE098x_rinRLZn3KKrELw'],
                            }
                        },
                        'offers_selected_by': {
                            'COMPOSITE': {
                                'count': 2,
                                'examples': [
                                    'yRgmzyBD4j8r4rkCby6Iuw',
                                    'bpQ3a9LXZAl_Kz34vaOpSg',
                                ],
                            },
                        },
                    },
                    'in_relevance': True,
                },
                'DuE098x_rinRLZn3KKrELw': {
                    'type': 'OFFER_BY_WARE_MD5',
                    'in_index': True,
                    'in_accept_doc': False,
                    'in_metadoc': True,
                    'in_metadoc_filtered_reason': 'OFFER_DISABLED',
                    'best_in_metadoc': False,
                    'child_selected_by': Absent(),
                    'in_relevance': Absent(),
                    'in_rearrange': Absent(),
                    'on_page': Absent(),
                },
                'yRgmzyBD4j8r4rkCby6Iuw': {
                    'type': 'OFFER_BY_WARE_MD5',
                    'in_index': True,
                    'in_accept_doc': False,
                    'in_metadoc': True,
                    'in_metadoc_filtered_reason': Absent(),
                    'best_in_metadoc': True,
                    'child_selected_by': 'COMPOSITE',
                    'in_relevance': True,
                    'in_rearrange': True,
                    'on_page': True,
                },
                'bpQ3a9LXZAl_Kz34vaOpSg': {
                    'type': 'OFFER_BY_WARE_MD5',
                    'in_index': True,
                    'in_accept_doc': False,
                    'in_metadoc': True,
                    'in_metadoc_filtered_reason': Absent(),
                    'best_in_metadoc': False,
                    'child_selected_by': 'COMPOSITE',
                    'in_relevance': Absent(),
                    'in_rearrange': Absent(),
                    'on_page': Absent(),
                },
                'msku:456': {
                    'type': 'MSKU',
                    'in_index': True,
                    'in_accept_doc': True,
                    'hit_metadoc_effective_pruncount': True,
                    'child_selected_by': 'COMPOSITE',
                    'in_relevance': True,
                },
                'msku:678': {
                    'type': 'MSKU',
                    'in_index': True,
                    'in_accept_doc': True,
                    'metadoc_common_filter_reason': 'OFFER_IN_SUBSCRIPTION_CATEGORY',
                    'in_relevance': True,
                    'relevance_filtered_reason': 'OFFER_IN_SUBSCRIPTION_CATEGORY',
                },
                '22222222222222gggg401g': {
                    'type': 'OFFER_BY_WARE_MD5',
                    'in_index': True,
                    'in_accept_doc': False,
                    'in_metadoc': True,
                    'in_metadoc_filtered_reason': 'OFFER_IN_SUBSCRIPTION_CATEGORY',
                    'in_relevance': Absent(),
                },
                'msku:901': {
                    'type': 'MSKU',
                    'in_index': True,
                    'in_accept_doc': True,
                    'accept_doc_filtered_reason': 'MSKU_UNPUBLISHED',
                    'passed_accept_doc': False,
                    'in_relevance': Absent(),
                },
            },
        )


if __name__ == '__main__':
    main()
