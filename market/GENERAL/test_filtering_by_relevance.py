#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    Book,
    DeliveryBucket,
    GLParam,
    GLType,
    HyperCategory,
    MnPlace,
    Offer,
    Outlet,
    PickupBucket,
    PickupOption,
    Shop,
)
from core.testcase import TestCase, main
from core.matcher import NotEmpty, NoKey, EmptyList, NotEmptyList, Contains


class T(TestCase):
    @classmethod
    def prepare(cls):

        cls.settings.default_search_experiment_flags += ['market_write_category_redirect_features=20']

        # Отключаем дефолты matrixnet для test_filter_by_relevance_threshold
        cls.matrixnet.set_defaults = False
        cls.matrixnet.on_place(MnPlace.PRODUCT_CLASSIFIER, 0).respond(0.7)

        cls.index.hypertree += [HyperCategory(hid=1, uniq_name='Тракторы')]

        cls.index.gltypes += [
            GLType(param_id=201, hid=1, gltype=GLType.BOOL, unit_name='Кабина', hasboolno=True),
            GLType(param_id=202, hid=1, gltype=GLType.ENUM, values=[2021, 2022, 2023], unit_name='Привод'),
        ]

        cls.index.shops += [
            Shop(fesh=1234, name='Беларус', priority_region=213, regions=[213]),
            Shop(fesh=9876, name='MasterYard', priority_region=2, regions=[2]),
        ]

        cls.index.outlets += [Outlet(fesh=9876, region=213, point_id=1)]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5001,
                fesh=9876,
                carriers=[99],
                options=[PickupOption(outlet_id=1)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        cls.index.offers += [
            Offer(
                hid=1,
                title='Трактор Машенька (переднеприводный с кабиной)',
                fesh=1234,
                ts=1,
                price=542000,
                glparams=[GLParam(param_id=201, value=1), GLParam(param_id=202, value=2021)],
            ),
            Offer(
                hid=1,
                title='Трактор Богдан (заднеприводный с кабиной)',
                fesh=1234,
                ts=2,
                price=3000000,
                glparams=[GLParam(param_id=201, value=1), GLParam(param_id=202, value=2022)],
            ),
            Offer(
                hid=1,
                title='Трактор Иванушка (полноприводный)',
                fesh=1234,
                ts=3,
                price=800000,
                glparams=[GLParam(param_id=202, value=2023)],
                manufacturer_warranty=True,
            ),
            Offer(
                hid=1,
                title='Трактор MasterYard M244 4WD (полноприводный с кабиной)',
                fesh=9876,
                ts=4,
                price=2400000,
                glparams=[GLParam(param_id=201, value=1), GLParam(param_id=202, value=2023)],
                manufacturer_warranty=True,
                pickup=True,
                pickup_buckets=[5001],
            ),
            Offer(
                hid=1,
                title='Трактор MasterYard ST24424W (открый заднеприводный)',
                fesh=9876,
                ts=5,
                price=350000,
                glparams=[GLParam(param_id=201, value=0), GLParam(param_id=202, value=2022)],
                manufacturer_warranty=True,
                pickup=True,
                pickup_buckets=[5001],
            ),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1).respond(0.51)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2).respond(None)  # INVALID_MN_VALUE
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3).respond(0.4)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 4).respond(0.6)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 5).respond(None)  # INVALID_MN_VALUE

    def test_filter_by_relevance_before_other_filters(self):
        '''Проверяем что фильтр по релевантности отрабатывает раньше других фильтров (пользовательских)
        Документы не прошедшие фильтра по релевантности не учитываются в initial-found
        '''
        response = self.report.request_json('place=prime&hid=1&text=трактор&rids=213&debug=da')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 3,
                    "results": [
                        {"titles": {"raw": "Трактор MasterYard M244 4WD (полноприводный с кабиной)"}},
                        {"titles": {"raw": "Трактор Машенька (переднеприводный с кабиной)"}},
                        {"titles": {"raw": "Трактор Иванушка (полноприводный)"}},
                    ],
                }
            },
            allow_different_len=False,
        )
        # заднеприводный трактор Богдан был отфильтрован по релевантности в фильтрах не участвовал
        self.assertFragmentNotIn(response, {"filters": [{"id": "202", "values": [{"value": "VALUE-2022"}]}]})

        # дозапрос за фильтрами не делался
        self.assertFragmentNotIn(response, "Make additional request for glfilters")
        # initialFound содержат реальные значения
        self.assertFragmentIn(
            response,
            {
                "search": NotEmpty(),
                "filters": [
                    {
                        # в initial значения фильтра по цене не вошли офферы за 3 000 000 и за 350 000
                        "id": "glprice",
                        "values": [
                            {"max": "2400000", "initialMax": "2400000", "initialMin": "542000", "min": "542000"}
                        ],
                    },
                    {
                        "id": "202",
                        "values": [
                            {
                                "initialFound": 1,
                                "found": 1,
                                "value": "VALUE-2021",
                            },
                            {"initialFound": 2, "found": 2, "value": "VALUE-2023", "id": "2023"},
                        ],
                    },
                ],
            },
            allow_different_len=True,
        )

        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "brief": {
                        "filters": {"RELEVANCE_THRESHOLD": 2},
                        "counters": {"TOTAL_DOCUMENTS_PROCESSED": 5, "TOTAL_DOCUMENTS_ACCEPTED": 3},
                    }
                }
            },
        )

        # фильтры по гуру-лайт могут применяться до фильтров по RELEVANCE_THRESHOLD
        # в рамках основного запроса в случае если делается отдельный дозапрос за фильтрами
        # поэтому 3 оффера будут отфильтрованы по GURULIGHT и только один по RELEVANCE_THRESHOLD (в основном запросе)

        response = self.report.request_json(
            'place=prime&hid=1&text=трактор&rids=213&glfilter=201:0&debug=da'
            '&rearr-factors=market_early_pre_early_gl_filtering=1'
        )

        # был отдельный дозапрос за фильтрами
        self.assertFragmentIn(response, "Make additional request for glfilters")
        # статистика основного запроса - GURULIGHT применяется на самой ранней стадии и даже не учитывается в TOTAL_DOCUMENTS_PROCESSED и вообще нигде o_O
        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "brief": {
                        "filters": {
                            # "GURULIGHT": 3, - вы этого не увидите больше
                            "GURULIGHT": NoKey("GURULIGHT"),
                            "RELEVANCE_THRESHOLD": 1,
                        },
                        "counters": {"TOTAL_DOCUMENTS_PROCESSED": 2, "TOTAL_DOCUMENTS_ACCEPTED": 1},
                    }
                }
            },
        )
        # статистика дозапроса за фильтрами - GURULIGHT применяется на обычной стадии
        # чтобы в фильтрах можно было бы учесть все доки прошедшие RELEVACNE_THRESHOLD и не прошедшие GURULIGHT
        self.assertFragmentIn(
            response,
            {
                "subrequests": [
                    {
                        "brief": {
                            "filters": {"GURULIGHT": 2, "RELEVANCE_THRESHOLD": 2},
                            "counters": {"TOTAL_DOCUMENTS_PROCESSED": 5, "TOTAL_DOCUMENTS_ACCEPTED": 1},
                        }
                    }
                ]
            },
            allow_different_len=True,
        )

        # останется один релевантный оффер Трактор Иванушка (полноприводный) - и он будет учтен 2 раза в основном запросе и в дозапросе за фильтрами
        # статистика initialFound при этом собирается и из основного запроса и из дозапроса
        # в ней будут учтены только офферы прошедшие RELEVANCE_THRESHOLD (причем документы в выдаче учтутся 2 раза)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "results": [
                        {"titles": {"raw": "Трактор Иванушка (полноприводный)"}},
                    ],
                }
            },
            allow_different_len=False,
        )

        self.assertFragmentIn(
            response,
            {
                "search": NotEmpty(),
                "filters": [
                    {
                        # в initial значения фильтра по цене не вошли офферы за 3 000 000 и за 350 000
                        "id": "glprice",
                        # "values": [{
                        #     "max": "2400000",
                        #     "initialMax": "2400000",
                        #     "initialMin": "542000",
                        #     "min": "542000"
                        # }]
                    },
                    {
                        "id": "202",
                        "values": [
                            {
                                "initialFound": 1,  # Трактор Машенька
                                "found": 0,
                                "value": "VALUE-2021",
                            },
                            {
                                "initialFound": 3,  # 2 в действительности Трактор Иванушка и Трактор MasterYard M244 4WD
                                "found": 2,  # 1 в действительности - Трактор Иванушка
                                "value": "VALUE-2023",
                                "id": "2023",
                            },
                        ],
                    },
                ],
            },
            allow_different_len=True,
        )

    def test_filter_by_relevance_before_other_filters_in_piter(self):
        '''Проверяем что фильтр по релевантности отрабатывает позже фильтра по доставке'''

        # из 5 офферов для региона [Питер] в первую очередь 3 будут отфильтрованы по доставке
        # а из оставшихся двух один - по RELEVANCE_THRESHOLD
        response = self.report.request_json('place=prime&hid=1&text=трактор&rids=2&debug=da')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "results": [{"titles": {"raw": "Трактор MasterYard M244 4WD (полноприводный с кабиной)"}}],
                }
            },
            allow_different_len=False,
        )

        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "brief": {
                        "filters": {"DELIVERY": 3, "RELEVANCE_THRESHOLD": 1},
                        "counters": {"TOTAL_DOCUMENTS_PROCESSED": 5, "TOTAL_DOCUMENTS_ACCEPTED": 1},
                    }
                }
            },
        )

    def test_market_relevance_limit(self):
        '''rearr-factors=market_calc_relevance_limit=n устанавливает ограничение на
        количество документов для которых подсчитана релевантность
        (не все документы для которых посчитана релевантность окажутся на выдаче
        некторые могут быть отсеяны по RELEVANCE_THRESHOLD или пользовательским фильтрам позднее)
        '''

        # без ограничения на выдаче 3 документа из 5
        response = self.report.request_json('place=prime&hid=1&text=трактор машенька&rids=213&debug=da')
        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "brief": {
                        "filters": {"RELEVANCE_THRESHOLD": 2},
                        "counters": {"TOTAL_DOCUMENTS_PROCESSED": 5, "TOTAL_DOCUMENTS_ACCEPTED": 3},
                    }
                }
            },
        )

        # с ограничением 1 - релевантность может быть посчитана только для одного документа
        # остальные документы будут отфильтрованы по RELEVANCE_LIMIT
        # что означает, что для них вовсе не будут посчитаны факторы и значение формулы
        response = self.report.request_json(
            'place=prime&hid=1&text=трактор машенька&rids=213&rearr-factors=market_calc_relevance_limit=1&debug=da'
        )
        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "brief": {
                        "filters": {"RELEVANCE_LIMIT": 4},
                        "counters": {"TOTAL_DOCUMENTS_PROCESSED": 5, "TOTAL_DOCUMENTS_ACCEPTED": 1},
                    }
                }
            },
        )

        # ограничение не действует там где пантера не включена (в частности на бестекстовых запросах)
        response = self.report.request_json(
            'place=prime&hid=1&rids=213&rearr-factors=market_calc_relevance_limit=1&debug=da'
        )
        self.assertFragmentIn(response, {"search": {"total": 5}})

    @classmethod
    def prepare_filter_by_relevance_threshold(cls):
        # Не фиксируем значение формулы для оферов,
        # чтобы протестировать фильтрующую формулу.
        cls.index.offers += [
            Offer(title='Экскаватор MasterYard'),
            Offer(title='Экскаватор SteelHand'),
            Offer(title='Экскаватор Lego'),
            Offer(title='Экскаватор JBC'),
            Offer(title='Погрузчик Lego H23-Z12'),
        ]

    def test_filter_by_relevance_threshold(self):
        """Проверяем, что флаг market_relevance_formula_threshold устанавливает
        порог фильтрации оферов по релевантности.
        """
        # TESTALGO_trivial_filter - релевантность каждого документа 1.0
        request = 'place=prime&text=экскаватор&debug=da&rearr-factors=market_search_mn_algo=TESTALGO_trivial_filter;'
        for threshold in ('', 'market_relevance_formula_threshold=0;'):
            response = self.report.request_json(request + threshold)
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "total": 4,
                        "results": [
                            {"titles": {"raw": "Экскаватор MasterYard"}},
                            {"titles": {"raw": "Экскаватор SteelHand"}},
                            {"titles": {"raw": "Экскаватор Lego"}},
                            {"titles": {"raw": "Экскаватор JBC"}},
                        ],
                    }
                },
                allow_different_len=False,
                preserve_order=False,
            )
            self.assertFragmentIn(
                response,
                {
                    "debug": {
                        "brief": {
                            "filters": {"RELEVANCE_THRESHOLD": NoKey("RELEVANCE_THRESHOLD")},
                            "counters": {"TOTAL_DOCUMENTS_PROCESSED": 4, "TOTAL_DOCUMENTS_ACCEPTED": 4},
                        }
                    }
                },
            )

        response = self.report.request_json(request + 'market_relevance_formula_threshold=5;')
        self.assertFragmentIn(response, {"search": {"results": []}}, allow_different_len=False)
        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "brief": {
                        "filters": {"RELEVANCE_THRESHOLD": 4},
                        "counters": {"TOTAL_DOCUMENTS_PROCESSED": 4, "TOTAL_DOCUMENTS_ACCEPTED": 0},
                    }
                }
            },
        )

    def test_filter_by_relaxed_relevance_threshold(self):
        """Порог релевантности задаваемый с помощью флага market_relaxed_relevance_formula_threshold
        действует также как и обычный порог, но включается только при &relax-relevance=1
        """

        request = 'place=prime&text=экскаватор&debug=da&rearr-factors=market_search_mn_algo=TESTALGO_trivial_filter;'
        thresholds = (
            '&rearr-factors=market_relevance_formula_threshold=5;market_relaxed_relevance_formula_threshold=0.1;'
        )
        # применится порог market_relevance_formula_threshold=5 и все документы будут отфильтрованы
        response = self.report.request_json(request + thresholds)
        self.assertFragmentIn(response, {'args': Contains('\nrelevance_formula_threshold: 5\n')})
        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "brief": {
                        "filters": {"RELEVANCE_THRESHOLD": 4},
                        "counters": {"TOTAL_DOCUMENTS_PROCESSED": 4, "TOTAL_DOCUMENTS_ACCEPTED": 0},
                    }
                }
            },
        )

        # применится порог market_relaxed_relevance_formula_threshold=0.1
        response = self.report.request_json(request + thresholds + '&relax-relevance=1')
        self.assertFragmentIn(response, {'args': Contains('\nrelevance_formula_threshold: 0.1\n')})
        self.assertFragmentIn(response, {"search": {"total": 4}})
        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "brief": {
                        "filters": {"RELEVANCE_THRESHOLD": NoKey("RELEVANCE_THRESHOLD")},
                        "counters": {"TOTAL_DOCUMENTS_PROCESSED": 4, "TOTAL_DOCUMENTS_ACCEPTED": 4},
                    }
                }
            },
        )

        # по умолчанию relax-relevance устанавливает порог релевантности в 0
        response = self.report.request_json(request + '&relax-relevance=1')
        self.assertFragmentIn(response, {'args': Contains('\nrelevance_formula_threshold: 0\n')})
        self.assertFragmentIn(response, {"search": {"total": 4}})

    def test_do_not_filter_vcode(self):
        """Проверяем что если запрос содержит вендоркод - то документ с вендоркодом не фильтруется
        если установлен флаг market_do_not_filter_vcode=1
        """

        # просто так находятся несколько документов подходящих под запрос
        response = self.report.request_json(
            'place=prime&text=Lego H23-Z12&debug=da' '&rearr-factors=market_search_mn_algo=TESTALGO_trivial_filter;'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "titles": {"raw": "Экскаватор Lego"},
                            "debug": {
                                "factors": {
                                    "HAS_ALL_WORDS_EX_TITLE": NoKey("HAS_ALL_WORDS_EX_TITLE"),
                                    "REQUEST_CONTAINS_CODE": "1",
                                }
                            },
                        },
                        {
                            "titles": {"raw": "Погрузчик Lego H23-Z12"},
                            "debug": {"factors": {"HAS_ALL_WORDS_EX_TITLE": "1", "REQUEST_CONTAINS_CODE": "1"}},
                        },
                    ]
                }
            },
            allow_different_len=True,
            preserve_order=False,
        )

        # с фильтром находятся 0 документов
        response = self.report.request_json(
            'place=prime&text=Lego H23-Z12&debug=da'
            '&rearr-factors=market_search_mn_algo=TESTALGO_trivial_filter;market_relevance_formula_threshold=5;'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {"total": 0, "results": EmptyList()},
                "debug": {"brief": {"filters": {"RELEVANCE_THRESHOLD": NotEmpty()}}},
            },
        )

        # с флагом market_do_not_filter_vcode=1 находится документ содержащий все слова из запроса
        response = self.report.request_json(
            'place=prime&text=Lego H23-Z12&debug=da'
            '&rearr-factors=market_search_mn_algo=TESTALGO_trivial_filter;market_relevance_formula_threshold=5;market_do_not_filter_vcode=1'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "results": [
                        {
                            "titles": {"raw": "Погрузчик Lego H23-Z12"},
                            "debug": {"factors": {"HAS_ALL_WORDS_EX_TITLE": "1", "REQUEST_CONTAINS_CODE": "1"}},
                        }
                    ],
                }
            },
            allow_different_len=False,
            preserve_order=False,
        )

    def test_filter_by_common_threshold(self):
        """Проверяем, что флаг market_formula_common_threshold устанавливает единый порог фильтрации,
        то есть порог для специальной фильтрующей части формулы. Проверяем на тестовой формуле
        TESTALGO_common_threshold_filter, в которой есть часть с таким же трейсом"""

        request = (
            'place=prime&text=экскаватор&debug=da&rearr-factors=market_search_mn_algo=TESTALGO_common_threshold_filter;'
        )
        no_threshold = ''
        with_threshold = 'market_formula_common_threshold=2;'

        response = self.report.request_json(request + no_threshold)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 4,
                    "results": [
                        {
                            "titles": {"raw": "Экскаватор MasterYard"},
                            "debug": {"properties": {"MATRIXNET_MODEL_FOR_COMMON_THRESHOLD_VALUE": "1"}},
                        },
                        {
                            "titles": {"raw": "Экскаватор SteelHand"},
                            "debug": {"properties": {"MATRIXNET_MODEL_FOR_COMMON_THRESHOLD_VALUE": "1"}},
                        },
                        {
                            "titles": {"raw": "Экскаватор Lego"},
                            "debug": {"properties": {"MATRIXNET_MODEL_FOR_COMMON_THRESHOLD_VALUE": "1"}},
                        },
                        {
                            "titles": {"raw": "Экскаватор JBC"},
                            "debug": {"properties": {"MATRIXNET_MODEL_FOR_COMMON_THRESHOLD_VALUE": "1"}},
                        },
                    ],
                }
            },
        )
        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "brief": {
                        "filters": {"RELEVANCE_THRESHOLD": NoKey("RELEVANCE_THRESHOLD")},
                        "counters": {"TOTAL_DOCUMENTS_PROCESSED": 4, "TOTAL_DOCUMENTS_ACCEPTED": 4},
                    }
                }
            },
        )

        response = self.report.request_json(request + with_threshold)
        self.assertFragmentIn(response, {"search": {"total": 0}})
        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "brief": {
                        "filters": {"RELEVANCE_THRESHOLD": 4},
                        "counters": {"TOTAL_DOCUMENTS_PROCESSED": 4, "TOTAL_DOCUMENTS_ACCEPTED": 0},
                    }
                }
            },
        )

    def test_relevance_formula_threshold_on_parallel_for_offers(self):
        # market_offers_incut_threshold и market_offers_incut_meta_threshold
        # нужны, чтобы плитка не отфильтровывалась по дефолтным порогам
        response = self.report.request_bs_pb(
            'place=parallel&text=экскаватор&trace_wizard=1'
            '&rearr-factors=market_relevance_formula_threshold_on_parallel_for_offers=0.055;'
            'market_offers_incut_threshold=0.25;market_offers_incut_meta_threshold=0.0'
        )

        self.assertFragmentIn(
            response,
            {'market_offers_wizard': {"offer_count": 4, "showcase": {"items": NotEmptyList()}}},
            allow_different_len=False,
        )

        # с увеличением порога офферный колдунщик исчезает
        response = self.report.request_bs_pb(
            'place=parallel&text=molotok&trace_wizard=1'
            '&rearr-factors=market_relevance_formula_threshold_on_parallel_for_offers=0.9;'
            'market_offers_incut_threshold=0.25;market_offers_incut_meta_threshold=0.0'
        )

        self.assertFragmentNotIn(response, {'market_offers_wizard': {}})

    @classmethod
    def prepare_relevance_formula_threshold_on_parallel_for_direct_offers(cls):
        # add direct offer
        cls.index.offers += [
            Offer(ts=6, fesh=1, offerid=40360 * 100 + 1, price=100, title='Direct offer', is_direct=True)
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 6).respond(0.4)

    def test_relevance_formula_threshold_on_parallel_for_direct_offers(self):
        request = 'place=parallel&text=Direct offer&trace_wizard=1&rearr-factors=market_enable_direct_offers_wizard=1;'
        response = self.report.request_bs_pb(request + 'market_relevance_formula_threshold_on_parallel_for_offers=0.4')

        # Проверяем, что колдунщик оффера директа проявился
        self.assertFragmentIn(response, {'market_direct_offers_wizard_0': NotEmpty()}, allow_different_len=False)

        response = self.report.request_bs_pb(request + 'market_relevance_formula_threshold_on_parallel_for_offers=0.25')

        # Проверяем, что колдунщик оффера директа отфильтрован при низком пороге
        self.assertFragmentIn(
            response,
            {'market_direct_offers_wizard_0': NoKey('market_direct_offers_wizard_0')},
            allow_different_len=False,
        )

        response = self.report.request_bs_pb(
            request
            + 'market_relevance_formula_threshold_on_parallel_for_offers=0.25;market_relevance_formula_threshold_on_parallel_for_direct_offers=0.4'
        )
        # Проверяем, что колдунщик оффера директа проявился по своему флагу market_relevance_formula_threshold_on_parallel_for_direct_offers,
        # несмотря на низкий порог
        self.assertFragmentIn(response, {'market_direct_offers_wizard_0': NotEmpty()}, allow_different_len=False)

    @classmethod
    def prepare_books_filter_by_relevance_threshold(cls):
        cls.index.books += [
            Book(title='Book Model', author='Author', isbn='978-5-699-12014-7'),
        ]

        cls.reqwizard.on_default_request().respond()
        # запрос формирует дерево (9785699120147 | (isbn:9785699120147) | (barcode:9785699120147))
        cls.reqwizard.on_request('9785699120147').respond(
            qtree='cHicpZKxS8NAHIXfO1Maj6SEDqVkCpmK07WoTTqJU3FydNRaIYIoxM3Fzk6dOohTdxfB0d3Rv8C_pXeXBEy0k7fdce973_04eSI9VwSiLyIMhEIXIWLsYYSJ5yJAH_ocCk'
            'etaesUZzhHxiXxTKyJN-KD0OuT-OI8fKfMZBFzS5yfjpODwzQdjtRwf6zZYhKVdN_SaemYwtCzl9X3q6j49WyjTCHhsdLqCOv34vp2QMVK_AEL4omedIxj13E7OXRZuKLMS-'
            '_dSFjv9uwiv7ybX9lpdOIa0tq3f8ymsi8mU0XXbOT-ekHPtJqSX55GbEl50xBzru9nt1utnK1WNvd_pdh3H4MF9XfZMSWNrWLC4t4GwoBf8w,,'
        )

    def test_books_filter_by_relevance_threshold(self):
        """Проверяем, что книги не отфильтровываются по порогу market_relevance_formula_threshold,
        если поиск по isbn - модели приходят от Пантеры как autoaccepted.
        """
        # TESTALGO_trivial_filter - релевантность каждого документа 1.0

        # текстовый запрос - порог отрабатывает
        request = 'place=prime&text=book+model&debug=da&rearr-factors=market_search_mn_algo=TESTALGO_trivial_filter;'
        for threshold in ('', 'market_relevance_formula_threshold=0;'):
            response = self.report.request_json(request + threshold)
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "total": 1,
                        "results": [
                            {"titles": {"raw": 'Author "Book Model"'}},
                        ],
                    }
                },
                allow_different_len=False,
            )
            self.assertFragmentIn(
                response,
                {
                    "debug": {
                        "brief": {
                            "filters": {"RELEVANCE_THRESHOLD": NoKey("RELEVANCE_THRESHOLD")},
                            "counters": {"TOTAL_DOCUMENTS_PROCESSED": 1, "TOTAL_DOCUMENTS_ACCEPTED": 1},
                        }
                    }
                },
            )

        response = self.report.request_json(request + 'market_relevance_formula_threshold=5;')
        self.assertFragmentIn(response, {"search": {"results": []}}, allow_different_len=False)
        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "brief": {
                        "filters": {"RELEVANCE_THRESHOLD": 1},
                        "counters": {"TOTAL_DOCUMENTS_PROCESSED": 1, "TOTAL_DOCUMENTS_ACCEPTED": 0},
                    }
                }
            },
        )

        # запрос с isbn - порог не работает
        request = 'place=prime&isbn=9785699120147&text=9785699120147&debug=da&rearr-factors=market_search_mn_algo=TESTALGO_trivial_filter;'
        for threshold in ('', 'market_relevance_formula_threshold=0;', 'market_relevance_formula_threshold=5;'):
            response = self.report.request_json(request + threshold)
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "total": 1,
                        "results": [
                            {"titles": {"raw": 'Author "Book Model"'}},
                        ],
                    }
                },
                allow_different_len=False,
            )
            self.assertFragmentIn(
                response,
                {
                    "debug": {
                        "brief": {
                            "filters": {"RELEVANCE_THRESHOLD": NoKey("RELEVANCE_THRESHOLD")},
                            "counters": {"TOTAL_DOCUMENTS_PROCESSED": 1, "TOTAL_DOCUMENTS_ACCEPTED": 1},
                        }
                    }
                },
            )

    def test_inf_in_factors(self):

        response = self.report.request_json(
            'place=prime&text=Богдан&debug=da&rearr-factors=market_search_mn_algo=MNA_fml_formula_779133'
        )
        self.assertFragmentIn(
            response,
            {
                'categories_ranking_json': [
                    {
                        'factors': {
                            'MATRIXNET_VALUE_Q0': '-inf',  # строка! -inf не является валидным значением в json
                            'REQUEST_LENGTH': 6,  # число
                        }
                    }
                ]
            },
        )


if __name__ == '__main__':
    main()
