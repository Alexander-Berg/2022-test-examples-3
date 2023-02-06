#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Ctr, HyperCategory, Model, Offer, QueryCtr, QueryEntityCtr, VCluster, MarketSku, BlueOffer
from core.testcase import TestCase, main

from core.matcher import NotEmpty


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.hypertree += [
            HyperCategory(hid=1, name='Автомобили'),
            HyperCategory(hid=2, name='Коллекционные модели автомобилей', visual=True),
        ]

        cls.index.models += [
            Model(hid=1, title='Lada Kalina хэтчбек', hyperid=1001, vendor_id=12345),
            Model(hid=1, title='Lada Kalina', hyperid=1002, vendor_id=67890),
        ]

        cls.index.vclusters += [
            VCluster(vclusterid=1000000001, hid=2, title='Lada Kalina 1:36'),
            VCluster(vclusterid=1000000002, hid=2, title='Lada Kalina 1:72'),
        ]

        cls.index.mskus += [
            MarketSku(
                hid=7654321,
                sku=7654322,
                hyperid=7654322,
                title="testmsku",
                blue_offers=[
                    BlueOffer(price=1000, feedid=7654322),
                ],
            ),
        ]

        cls.index.offers += [
            Offer(ts=100500, hid=1, hyperid=1001, title='Lada Kalina поддержанная', waremd5='wgrU12_pd1mqJ6DJm_9nEA'),
            Offer(ts=100501, hid=1, title='Lada Kalina с каско в комплекте', waremd5='ZRK9Q9nKpuAsmQsKgmUtyg'),
            Offer(vclusterid=1000000001),
            Offer(vclusterid=1000000002),
            Offer(hyperid=7654322, price=300),
        ]

        cls.index.ctr.msearch.query += [QueryCtr('lada kalina', 100, 1000)]
        cls.index.ctr.msearch.categ += [
            QueryEntityCtr(query='lada kalina', entity_id=1, clicks=33, shows=154),
            QueryEntityCtr(query='lada kalina', entity_id=2, clicks=4, shows=150),
        ]
        cls.index.ctr.msearch.model += [
            QueryEntityCtr('lada kalina', 1001, 12, 120),
            QueryEntityCtr('lada kalina', 1002, 0, 50),
        ]
        cls.index.ctr.msearch.ts += [QueryEntityCtr('lada kalina', 100500, 3, 89)]
        cls.index.ctr.msearch.zero = Ctr(2, 100)

        # источник: ctr по запросам, приведенными к нижнему регистру, слова в которых отсортированы
        # запрос Lada Kalina нормализуется к kalina lada
        # проверяем только на qlowsort т.к. все остальные источники будут работать по той же схеме (при наличии данных)

        cls.index.ctr.msearch_qlowsort.query += [QueryCtr('kalina lada', 105, 1100)]
        cls.index.ctr.msearch_qlowsort.categ += [
            QueryEntityCtr(query='kalina lada', entity_id=1, clicks=33, shows=154),
            QueryEntityCtr(query='kalina lada', entity_id=2, clicks=4, shows=150),
        ]
        cls.index.ctr.msearch_qlowsort.vendor += [
            QueryEntityCtr(query='kalina lada', entity_id=12345, clicks=35, shows=130),
            QueryEntityCtr(query='kalina lada', entity_id=67890, clicks=10, shows=165),
        ]
        cls.index.ctr.msearch_qlowsort.model += [
            QueryEntityCtr('kalina lada', 1001, 15, 200),
            QueryEntityCtr('kalina lada', 1002, 0, 50),
        ]
        cls.index.ctr.msearch_qlowsort.ts += [QueryEntityCtr('kalina lada', 100500, 5, 87)]

        cls.index.ctr.msearch.msku += [QueryEntityCtr('testmsku', 7654322, 15, 97)]

    def test_prime(self):
        response = self.report.request_json('place=prime&text=Lada+Kalina&debug=da')
        self.error_log.ignore('not found in head collector')

        # у модели 1002 клики равны 0 поэтому некоторые кликовые факторы совпали с дефолтными значениями и не выводятся
        self.assertFragmentIn(
            response,
            {
                "entity": "product",
                "type": "model",
                "id": 1002,
                "titles": {"raw": "Lada Kalina"},
                "debug": {
                    "factors": {
                        "DOCUMENT_QUERY_CTR": "0.1233245134",
                        "DOCUMENT_QUERY_SHOWS": "50",
                        "CATEGORY_QUERY_CLICKS": "33",
                        "CATEGORY_QUERY_SHOWS": "154",
                        "CATEGORY_QUERY_CTR_NAIVE": "0.2142857164",
                        "CATEGORY_QUERY_CTR_NAIVE_DIV_TOTAL_CATEGORIES_CTR": "1.760617852",
                        "DOCUMENT_QUERY_CTR_QSYNNORM": "0.01999999955",
                    }
                },
            },
        )

        # по запросу lada kalina на модель 1001 кликают явно больше чем на модель 1002 (12/120 против 0/50)
        # однако сглаженный ctr больше у второй модели...
        self.assertFragmentIn(
            response,
            {
                "entity": "product",
                "type": "model",
                "id": 1001,
                "titles": {"raw": "Lada Kalina хэтчбек"},
                "debug": {
                    "factors": {
                        # DOCUMENT_QUERY_* = MODEL_QUERY_* для моделей
                        "DOCUMENT_QUERY_CTR": "0.1219470277",
                        "DOCUMENT_QUERY_CLICKS": "12",
                        "DOCUMENT_QUERY_SHOWS": "120",
                        "DOCUMENT_QUERY_CTR_NAIVE": "0.1000000015",
                        "MODEL_QUERY_CTR": "0.1219470277",
                        "MODEL_QUERY_CLICKS": "12",
                        "MODEL_QUERY_SHOWS": "120",
                        "MODEL_QUERY_CTR_NAIVE": "0.1000000015",
                        "DOCUMENT_QUERY_CTR_NAIVE_DIV_TOTAL_DOCUMENTS_CTR": "1.726666689",
                        "CATEGORY_QUERY_CLICKS": "33",
                        "CATEGORY_QUERY_SHOWS": "154",
                        "CATEGORY_QUERY_CTR_NAIVE": "0.2142857164",
                        "CATEGORY_QUERY_CTR_NAIVE_DIV_TOTAL_CATEGORIES_CTR": "1.760617852",
                        "DOCUMENT_CLICKS_DIV_TOTAL_DOCUMENTS_CLICKS": "0.8000000119",
                        "DOCUMENT_CTR_NAIVE_MUL_TOTAL_DOCUMENTS_SHOWS": "25.89999962",
                        # по QLOW нет данных
                        "DOCUMENT_QUERY_CTR_QSYNNORM": "0.01999999955",
                        "MODEL_QUERY_CTR_QSYNNORM": "0.01999999955",
                    }
                },
            },
        )

        # есть данные по этому офферу и по его модели (1001)
        # факторы MODEL_QUERY_* совпадают по значениям с факторами от модели выше
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "wareId": "wgrU12_pd1mqJ6DJm_9nEA",
                "titles": {"raw": "Lada Kalina поддержанная"},
                "debug": {
                    "factors": {
                        "DOCUMENT_QUERY_CTR": "0.09364591539",
                        "DOCUMENT_QUERY_CLICKS": "3",
                        "DOCUMENT_QUERY_SHOWS": "89",
                        "DOCUMENT_QUERY_CTR_NAIVE": "0.03370786458",
                        "MODEL_QUERY_CTR": "0.1219470277",
                        "MODEL_QUERY_CLICKS": "12",
                        "MODEL_QUERY_SHOWS": "120",
                        "MODEL_QUERY_CTR_NAIVE": "0.1000000015",
                        "DOCUMENT_QUERY_CTR_NAIVE_DIV_TOTAL_DOCUMENTS_CTR": "0.5820224881",
                        "CATEGORY_QUERY_CLICKS": "33",
                        "CATEGORY_QUERY_SHOWS": "154",
                        "CATEGORY_QUERY_CTR_NAIVE": "0.2142857164",
                        "CATEGORY_QUERY_CTR_NAIVE_DIV_TOTAL_CATEGORIES_CTR": "1.760617852",
                        "DOCUMENT_CLICKS_DIV_TOTAL_DOCUMENTS_CLICKS": "0.200000003",
                        "DOCUMENT_CTR_NAIVE_MUL_TOTAL_DOCUMENTS_SHOWS": "8.730337143",
                        "DOCUMENT_QUERY_CTR_QSYNNORM": "0.01999999955",
                        "MODEL_QUERY_CTR_QSYNNORM": "0.01999999955",
                    }
                },
            },
        )

        # нет данных непосредственно по этому офферу, но есть данные по категории
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "wareId": "ZRK9Q9nKpuAsmQsKgmUtyg",
                "titles": {"raw": "Lada Kalina с каско в комплекте"},
                "debug": {
                    "factors": {
                        "DOCUMENT_QUERY_CTR": "0.1782948226",
                        "CATEGORY_QUERY_CLICKS": "33",
                        "CATEGORY_QUERY_SHOWS": "154",
                        "CATEGORY_QUERY_CTR_NAIVE": "0.2142857164",
                        "CATEGORY_QUERY_CTR_NAIVE_DIV_TOTAL_CATEGORIES_CTR": "1.760617852",
                        "DOCUMENT_QUERY_CTR_QSYNNORM": "0.01999999955",
                    }
                },
            },
        )

        self.assertFragmentIn(
            response,
            {
                "entity": "product",
                "type": "cluster",
                "id": 1000000002,
                "titles": {"raw": "Lada Kalina 1:72"},
                "debug": {
                    "factors": {
                        "DOCUMENT_QUERY_CTR": "0.04863807186",
                        "CATEGORY_QUERY_CLICKS": "4",
                        "CATEGORY_QUERY_SHOWS": "150",
                        "CATEGORY_QUERY_CTR_NAIVE": "0.02666666731",
                        "CATEGORY_QUERY_CTR_NAIVE_DIV_TOTAL_CATEGORIES_CTR": "0.2190991044",
                        "DOCUMENT_QUERY_CTR_QSYNNORM": "0.01999999955",
                    }
                },
            },
        )

        self.assertFragmentIn(
            response,
            {
                "entity": "product",
                "type": "cluster",
                "id": 1000000001,
                "titles": {"raw": "Lada Kalina 1:36"},
                "debug": {
                    "factors": {
                        "DOCUMENT_QUERY_CTR": "0.04863807186",
                        "CATEGORY_QUERY_CLICKS": "4",
                        "CATEGORY_QUERY_SHOWS": "150",
                        "CATEGORY_QUERY_CTR_NAIVE": "0.02666666731",
                        "CATEGORY_QUERY_CTR_NAIVE_DIV_TOTAL_CATEGORIES_CTR": "0.2190991044",
                        "DOCUMENT_QUERY_CTR_QSYNNORM": "0.01999999955",
                    }
                },
            },
        )

    def test_parallel(self):
        """На параллельном под флагом market_calc_normalized_ctr_factors=1 считаются факторы по нормализованным запросам.
        https://st.yandex-team.ru/MARKETOUT-16973
        """

        response = self.report.request_bs(
            'place=parallel&text=Lada+Kalina&rearr-factors=market_calc_normalized_ctr_factors=1'
        )
        self.assertFragmentIn(
            response, {"market_factors": [{"base.category_query_ctr_naive_qlowsort_min": NotEmpty()}]}
        )

    @classmethod
    def prepare_smoothed_ctr(cls):
        cls.index.hypertree += [
            HyperCategory(hid=100, tovalid=100100),
        ]

        cls.index.models += [
            Model(title='test qwer asdf 1', hyperid=500, hid=100),
            Model(title='test qwer asdf 2', hyperid=501, hid=100),
            Model(title='test qwer asdf 3', hyperid=502, hid=100),
        ]

        cls.index.offers += [
            Offer(title='test qwer asdf 1', hid=100, ts=100500, vendor_id=3706),
        ]

        cls.index.ctr.msearch.query += [
            QueryCtr('test', 1, 1000),
            QueryCtr('qwer', 20, 1000),
            QueryCtr('asdf', 30, 1000),
            QueryCtr('b956d5f0d5c4dopaliha666 100', 40, 1000),
        ]

        cls.index.ctr.msearch.categ += [QueryEntityCtr('test', 100, 5, 1000), QueryEntityCtr('qwer', 100, 20, 1000)]

        cls.index.ctr.msearch.model += [
            QueryEntityCtr('test', 500, 30, 1000),
            QueryEntityCtr('test', 501, 700, 1000),
            QueryEntityCtr('test', 502, 5, 1000),
        ]

        cls.index.ctr.msearch.ts += [QueryEntityCtr('test', 100500, 2, 1000)]
        cls.index.ctr.msearch.zero = Ctr(2, 100)

    # @see https://wiki.yandex-team.ru/market/development/sort/ for manual ctr calculation
    def test_offer_ctr(self):
        response = self.report.request_json('place=prime&text=test&debug=1')

        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "titles": {"raw": "test qwer asdf 1"},
                "debug": {"factors": {"DOCUMENT_QUERY_CTR": "0.002392026596"}},
            },
        )

    def test_category_ctr(self):
        response = self.report.request_json('place=prime&text=qwer&debug=1')

        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "titles": {"raw": "test qwer asdf 1"},
                "debug": {"factors": {"DOCUMENT_QUERY_CTR": "0.01999999955"}},
            },
        )

    def test_query_ctr(self):
        response = self.report.request_json('place=prime&text=asdf&debug=1')

        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "titles": {"raw": "test qwer asdf 1"},
                "debug": {"factors": {"DOCUMENT_QUERY_CTR": "0.02952381037"}},
            },
        )

    def test_model_ctr(self):
        response = self.report.request_json('place=prime&text=test&debug=1')

        self.assertFragmentIn(
            response,
            {
                "type": "model",
                "titles": {"raw": "test qwer asdf 3"},
                "debug": {"factors": {"DOCUMENT_QUERY_CTR": "0.004784053192"}},
            },
        )

    def test_msku_ctr(self):
        response = self.report.request_json('place=prime&text=testmsku&debug=1')

        self.assertFragmentIn(
            response,
            {
                "titles": {"raw": "testmsku"},
                "debug": {
                    "factors": {
                        "MSKU_QUERY_CLICKS": "15",
                        "MSKU_QUERY_SHOWS": "97",
                        "MSKU_QUERY_CTR_NAIVE": "0.1546391696",
                    }
                },
            },
        )

    def test_boosted_ctr(self):
        response = self.report.request_json('place=prime&text=test&debug=1')

        self.assertFragmentIn(
            response,
            {
                "type": "model",
                "titles": {"raw": "test qwer asdf 2"},
                "debug": {"factors": {"DOCUMENT_QUERY_CTR": "0.5589368939"}},
            },
        )

    def test_textless_ctr(self):
        response = self.report.request_json('place=prime&hid=100&pp=7&debug=1')

        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "titles": {"raw": "test qwer asdf 1"},
                "debug": {"factors": {"DOCUMENT_QUERY_CTR": "0.03904761747"}},
            },
        )

    def test_ctr_in_category_ranking(self):
        response = self.report.request_json(
            'place=prime&text=Lada+Kalina&debug=da&rearr-factors=market_write_category_redirect_features=20'
        )

        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "categories_ranking_json": [
                        {
                            "factors": {
                                "QUERY_SHOWS": NotEmpty(),
                                "QUERY_CLICKS": NotEmpty(),
                                "QUERY_CTR": NotEmpty(),
                                "QUERY_CTR_DIV_TOTAL_CATEGORIES_CTR": NotEmpty(),
                                # todo: add synnorm in ctr
                            }
                        }
                    ]
                }
            },
        )


if __name__ == '__main__':
    main()
