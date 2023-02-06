#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import BlueOffer, MarketSku, Model, Offer

from core.testcase import TestCase, main
from core.matcher import Round, Contains


class T(TestCase):
    """Тестируем что можно набирать топ пантеры упорядочивая документы по dssm"""

    @classmethod
    def prepare(cls):

        cls.index.offers += [
            Offer(ts=1001, title="холодильник indesit"),
            Offer(ts=1002, title="холодильник indesit no frost"),
            Offer(ts=1003, title="холодильник bosch"),
        ]

        cls.index.dssm.hard2_query_embedding.on(query='холодильник indesit no frost').set(-0.5, -0.4, -0.3, -0.2)

        # по dssm максимально релевантным будет документ с ts=1 затем с ts=2 и затем с ts=3
        cls.index.dssm.hard2_dssm_values_binary.on(ts=1001).set(-0.5, -0.4, -0.3, -0.2)
        cls.index.dssm.hard2_dssm_values_binary.on(ts=1002).set(-0.2, -0.1, -0.2, -0.1)
        cls.index.dssm.hard2_dssm_values_binary.on(ts=1003).set(0.5, 0.4, 0.3, 0.2)

        cls.settings.dont_put_sku_to_blue_shard = True

    def test_default_doc_rel(self):
        """По умолчанию docrel считается из количества совпавших слов с запросом"""

        response = self.report.request_json(
            'place=prime&text=холодильник indesit no frost&debug=da' '&rearr-factors=market_custom_panther_relevance=0'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'titles': {'raw': 'холодильник indesit'},
                        'debug': {'factors': {'DOC_REL': '285', 'DSSM_HARD2': Round(0.752)}},
                    },
                    {
                        'titles': {'raw': 'холодильник indesit no frost'},
                        'debug': {'factors': {'DOC_REL': '597', 'DSSM_HARD2': Round(0.706)}},
                    },
                    {
                        'titles': {'raw': 'холодильник bosch'},
                        'debug': {'factors': {'DOC_REL': '139', 'DSSM_HARD2': Round(0.469)}},
                    },
                ]
            },
            allow_different_len=False,
            preserve_order=False,
        )

        response = self.report.request_json(
            'place=prime&text=холодильник indesit no frost&debug=da'
            '&rearr-factors=panther_offer_tpsz=1;market_custom_panther_relevance=0'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'titles': {'raw': 'холодильник indesit no frost'},
                        'debug': {'factors': {'DOC_REL': '597', 'DSSM_HARD2': Round(0.706)}},
                    }
                ]
            },
            allow_different_len=False,
            preserve_order=False,
        )

    def test_dssm_order(self):
        """Под флагом market_custom_panther_relevance=1 документы имеют DOC_REL повторяющий dssm"""

        response = self.report.request_json(
            'place=prime&text=холодильник indesit no frost&debug=da'
            '&rearr-factors=market_custom_panther_relevance=1;market_min_doc_rel=0;'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'titles': {'raw': 'холодильник indesit'},
                        'debug': {'factors': {'DOC_REL': '838166', 'DSSM_HARD2': Round(0.752)}},
                    },
                    {
                        'titles': {'raw': 'холодильник indesit no frost'},
                        'debug': {'factors': {'DOC_REL': '832293', 'DSSM_HARD2': Round(0.706)}},
                    },
                    {
                        'titles': {'raw': 'холодильник bosch'},
                        'debug': {'factors': {'DOC_REL': '818266', 'DSSM_HARD2': Round(0.469)}},
                    },
                ]
            },
            allow_different_len=False,
            preserve_order=False,
        )

        # при ограничении топа пантеры на выдаче остается 1 документ с максимальным DOC_REL
        response = self.report.request_json(
            'place=prime&text=холодильник indesit no frost&debug=da'
            '&rearr-factors=market_custom_panther_relevance=1;market_min_doc_rel=0;panther_offer_tpsz=1'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'titles': {'raw': 'холодильник indesit'},
                        'debug': {'factors': {'DOC_REL': '838166', 'DSSM_HARD2': Round(0.752)}},
                    }
                ]
            },
            allow_different_len=False,
            preserve_order=False,
        )

    def test_dssm_order_2(self):
        """Под флагом market_custom_panther_relevance=2 документы имеют DOC_REL = dssm * docrel"""

        # sum([unscaled(scaled(x1))*unscaled(scaled(y1)) for x1, y1 in zip(dssm(query), dssm(doc))])
        dssm1 = 1.03902
        dssm2 = 0.717278
        dssm3 = -0.0511676
        response = self.report.request_json(
            'place=prime&text=холодильник indesit no frost&debug=da'
            '&rearr-factors=market_custom_panther_relevance=2;market_min_doc_rel=0;'
            'market_min_dot_product=-0.5;market_max_dot_product=1.5'
        )

        mindp = -0.5
        maxdp = 1.5
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'titles': {'raw': 'холодильник indesit'},
                        'debug': {
                            'factors': {
                                'DOC_REL': Round(285 * 100 * (dssm1 - mindp) / (maxdp - mindp), 0),
                                'DSSM_HARD2': Round(0.752),
                            }
                        },
                    },
                    {
                        'titles': {'raw': 'холодильник indesit no frost'},
                        'debug': {
                            'factors': {
                                'DOC_REL': Round(597 * 100 * (dssm2 - mindp) / (maxdp - mindp), 0),
                                'DSSM_HARD2': Round(0.706),
                            }
                        },
                    },
                    {
                        'titles': {'raw': 'холодильник bosch'},
                        'debug': {
                            'factors': {
                                'DOC_REL': Round(139 * 100 * (dssm3 - mindp) / (maxdp - mindp), 0),
                                'DSSM_HARD2': Round(0.469),
                            }
                        },
                    },
                ]
            },
            allow_different_len=False,
            preserve_order=False,
        )

        # Что будет если dssm выйдет за границы?
        # norm_dssm = x - min/(max - min)  где x - значение dssm в диапазоне [min, max]
        response = self.report.request_json(
            'place=prime&text=холодильник indesit no frost&debug=da'
            '&rearr-factors=market_custom_panther_relevance=2;market_min_doc_rel=0;'
            'market_min_dot_product=0.2;market_max_dot_product=0.9'
        )

        mindp = 0.2
        maxdp = 0.9
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'titles': {'raw': 'холодильник indesit'},
                        'debug': {'factors': {'DOC_REL': Round(285 * 100 * 1.0), 'DSSM_HARD2': Round(0.752)}},
                    },
                    {
                        'titles': {'raw': 'холодильник indesit no frost'},
                        'debug': {
                            'factors': {
                                'DOC_REL': Round(597 * 100 * (dssm2 - mindp) / (maxdp - mindp), 0),
                                'DSSM_HARD2': Round(0.706),
                            }
                        },
                    },
                    {
                        'titles': {'raw': 'холодильник bosch'},
                        'debug': {'factors': {'DOC_REL': '1', 'DSSM_HARD2': Round(0.469)}},  # max(docrel * 0 * 100, 1)
                    },
                ]
            },
            allow_different_len=False,
            preserve_order=False,
        )

    def test_optimized_dssm_order(self):
        """Оффер с doc_rel <= 300 будет умножаться на некий средний dssm=0.5 для экономии"""

        dssm2 = 0.717278

        mindp = -0.5
        maxdp = 1.5
        response = self.report.request_json(
            'place=prime&text=холодильник indesit no frost&debug=da'
            '&rearr-factors=market_custom_panther_relevance=2;market_min_dot_product=-0.5;'
            'market_max_dot_product=1.5;market_min_doc_rel=300'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'titles': {'raw': 'холодильник indesit'},
                        'debug': {'factors': {'DOC_REL': Round(285 * 0.5 * 100, 0)}},
                    },
                    {
                        'titles': {'raw': 'холодильник indesit no frost'},
                        'debug': {'factors': {'DOC_REL': Round(597 * 100 * (dssm2 - mindp) / (maxdp - mindp), 0)}},
                    },
                    {
                        'titles': {'raw': 'холодильник bosch'},
                        'debug': {'factors': {'DOC_REL': Round(139 * 0.5 * 100, 0), 'DSSM_HARD2': Round(0.469)}},
                    },
                ]
            },
            allow_different_len=False,
            preserve_order=False,
        )

    @classmethod
    def prepare_parallel(cls):

        cls.index.offers += [Offer(ts=2000 + i, title="пылесос xiaomi {}".format(i)) for i in range(1, 10)]
        # по совпадению слов с запросом [xiaomi redmi] смартфоны будут более релеванты чем пылесосы
        cls.index.offers += [Offer(ts=2010 + i, title="смартфон xiaomi redmi note {}".format(i)) for i in range(1, 10)]

        # по дссм пылесосы будут более релевантны чем смартфоны
        cls.index.dssm.hard2_query_embedding.on(query='xiaomi redmi').set(-0.3, -0.2, -0.1)
        for i in range(1, 10):
            cls.index.dssm.hard2_dssm_values_binary.on(ts=2000 + i).set(-0.3, -0.2, -0.1)
            cls.index.dssm.hard2_dssm_values_binary.on(ts=2010 + i).set(0.3, 0.2, 0.1)

    def test_parallel(self):
        # на параллельном в тестах добавляется по дефолту parallel_smm=1.0
        response = self.report.request_bs(
            'place=parallel&text=xiaomi redmi&debug=da'
            '&rearr-factors=panther_parallel_tpsz=9;market_use_knn=0;panther_parallel_tpsz_mul=0'
        )

        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": [
                    {
                        "showcase": {
                            "items": [
                                {"title": {"text": {"__hl": {"text": Contains("смартфон xiaomi redmi note")}}}}
                                for i in range(9)
                            ]
                        }
                    }
                ]
            },
            allow_different_len=False,
        )

        response = self.report.request_bs(
            'place=parallel&text=xiaomi redmi&debug=da'
            '&rearr-factors=panther_parallel_tpsz=9;market_use_knn=0;panther_parallel_tpsz_mul=0;market_custom_panther_relevance=1'
        )

        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": [
                    {
                        "showcase": {
                            "items": [
                                {"title": {"text": {"__hl": {"text": Contains("пылесос xiaomi")}}}} for i in range(9)
                            ]
                        }
                    }
                ]
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_shards(cls):

        cls.index.offers += [Offer(ts=3001, title="panasonic white offer")]

        cls.index.mskus += [MarketSku(sku=1, title="panasonic blue offer", blue_offers=[BlueOffer(ts=3002)])]

        cls.index.models += [Model(ts=3003, title="panasonic model")]

        cls.index.dssm.hard2_query_embedding.on(query='panasonic').set(0.3, 0.2, -0.12, 0.23)

        cls.index.dssm.hard2_dssm_values_binary.on(ts=3001).set(-0.5, -0.4, -0.3, -0.2)
        cls.index.dssm.hard2_dssm_values_binary.on(ts=3002).set(-0.2, -0.1, -0.2, -0.1)
        cls.index.dssm.hard2_dssm_values_binary.on(ts=3003).set(0.5, 0.4, 0.3, 0.2)

    def test_different_custom_panther_relevance(self):
        """Флагами
        market_custom_panther_relevance_offers
        market_custom_panther_relevance_models
        можно задать ранжирование по dssm на каждом из шардов по отдельности"""

        query = 'place=prime&text=panasonic&debug=da&rearr-factors=market_use_knn=0;market_custom_panther_relevance=0;market_metadoc_search=no;'

        # только офферы отранжировались по dssm
        response = self.report.request_json(query + 'market_custom_panther_relevance_offers=1')
        self.assertFragmentIn(response, {'titles': {"raw": "panasonic model"}, "debug": {"docRel": "322"}})
        self.assertFragmentIn(response, {'titles': {"raw": "panasonic white offer"}, "debug": {"docRel": "823789"}})
        self.assertFragmentIn(response, {'titles': {"raw": "panasonic blue offer"}, "debug": {"docRel": "826729"}})

        # только модели отранжировались по dssm
        response = self.report.request_json(query + 'market_custom_panther_relevance_models=1')
        self.assertFragmentIn(response, {'titles': {"raw": "panasonic model"}, "debug": {"docRel": "829378"}})
        self.assertFragmentIn(response, {'titles': {"raw": "panasonic white offer"}, "debug": {"docRel": "289"}})
        self.assertFragmentIn(response, {'titles': {"raw": "panasonic blue offer"}, "debug": {"docRel": "289"}})


if __name__ == '__main__':
    main()
