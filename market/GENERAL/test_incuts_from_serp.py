#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    MarketSku,
    MnPlace,
    Model,
    Offer,
    YamarecPlaceReasonsToBuy,
)
from core.testcase import TestCase, main
from core.blender_bundles import create_blender_bundles
from core.matcher import Contains

NUM_GOODS = 40

BUNDLE_CONST_SEARCH_POSITION = '''
{
    "incut_places": ["Search"],
    "incut_positions": [2],
    "incut_viewtypes": ["SimpleGallery"],
    "incut_ids": ["default"],
    "result_scores": [
        {
            "incut_place": "Search",
            "row_position": 2,
            "position": 2,
            "incut_viewtype": "SimpleGallery",
            "incut_id": "default",
            "score": 1.0
        }
    ],
    "calculator_type": "ConstPosition"
}
'''

BUNDLES_CONFIG = """
{}
"""


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.formulas_path = create_blender_bundles(
            cls.meta_paths.testroot,
            BUNDLES_CONFIG,
            {
                'const_incut_from_serp.json': BUNDLE_CONST_SEARCH_POSITION,
            },
        )

        for i in range(1, NUM_GOODS + 1):
            cls.index.models.append(
                Model(
                    hyperid=i,
                    hid=1,
                    title=('Model %d' % i),
                )
            )
            cls.index.mskus.append(
                MarketSku(
                    sku=i,
                    title=('MSKU %d' % i),
                    hid=1,
                    hyperid=i,
                )
            )

            ts = 100 + i
            cls.index.offers.append(
                Offer(
                    sku=i,
                    price=i * 1000,
                    title=('Offer %d' % i),
                    hid=1,
                    fesh=100,
                    hyperid=i,
                    ts=ts,
                    cpa=Offer.CPA_REAL,
                )
            )
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, ts).respond(100.0 - 0.001 * i)
            cls.matrixnet.on_place(MnPlace.META_REARRANGE, ts).respond(100.0 - 0.001 * i)

        rtb = YamarecPlaceReasonsToBuy().new_partition()
        for i in range(NUM_GOODS):
            reasons = [
                {
                    'type': 'statFactor',
                    'id': 'viewed_n_times',
                    'value': (i if (i % 5) == 0 else 0),
                },
                {
                    'type': 'statFactor',
                    'id': 'bought_n_times',
                    'value': (i if (i % 3) == 0 else 0),
                },
                {
                    'type': 'consumerFactor',
                    'id': 'customers_choice',
                    'value': (0.5 + i * 0.005 if (i % 4) == 0 else 0.0),
                    'share_threshold': str(0.4),
                    'rating_threshold': str(4.0),
                    'rating': '4.5',
                    'recommenders_count': '100',
                },
            ]

            rtb.add(hyperid=i, reasons=reasons)
        cls.index.yamarec_places += [rtb]

    BASE_REQUEST = (
        'place=prime&pp=18&text=MSKU&debug=1&cpa=real'
        + '&platform=desktop&supported-incuts={{%221%22%3A[1%2C4%2C6%2C5%2C15]%2C%222%22%3A[2%2C3%2C15]}}&blender=1&viewtype=list&allow-collapsing=1'
        + '&rearr-factors=market_blender_bundles_for_inclid={inclid}:const_incut_from_serp.json'
    )

    def test_bought_incut(self):
        inclid = 23
        request = T.BASE_REQUEST.format(inclid=inclid) + '&rearr-factors=market_blender_serp_incuts=bought'
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'searchIncut',
                            'title': 'Часто покупают',
                            'inClid': inclid,
                            'typeId': 15,
                            'items': [
                                {'slug': 'model-39'},
                                {'slug': 'model-36'},
                                {'slug': 'model-33'},
                                {'slug': 'model-30'},
                                {'slug': 'model-27'},
                                {'slug': 'model-24'},
                                {'slug': 'model-21'},
                                {'slug': 'model-18'},
                                {'slug': 'model-15'},
                                {'slug': 'model-12'},
                            ],
                        }
                    ]
                }
            },
        )

    def test_viewed_incut(self):
        inclid = 21
        request = T.BASE_REQUEST.format(inclid=inclid) + '&rearr-factors=market_blender_serp_incuts=viewed'
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'searchIncut',
                            'title': 'Часто смотрят',
                            'inClid': inclid,
                            'typeId': 15,
                            'items': [
                                {'slug': 'model-35'},
                                {'slug': 'model-30'},
                                {'slug': 'model-25'},
                                {'slug': 'model-20'},
                                {'slug': 'model-15'},
                                {'slug': 'model-10'},
                                {'slug': 'model-5'},
                            ],
                        }
                    ]
                }
            },
        )

    def test_rated_incut(self):
        inclid = 24
        request = T.BASE_REQUEST.format(inclid=inclid) + '&rearr-factors=market_blender_serp_incuts=rated'
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'searchIncut',
                            'title': 'Часто рекомендуют',
                            'inClid': inclid,
                            'typeId': 15,
                            'items': [
                                {'slug': 'model-36'},
                                {'slug': 'model-32'},
                                {'slug': 'model-28'},
                                {'slug': 'model-24'},
                                {'slug': 'model-20'},
                                {'slug': 'model-16'},
                                {'slug': 'model-12'},
                                {'slug': 'model-8'},
                                {'slug': 'model-4'},
                            ],
                        }
                    ]
                }
            },
        )

    def test_doc_stats_request(self):
        """Sanity check: должен выполниться запрос ДО для дин. статистик.
        В основной выдаче уже получены статистики для всех документов, должны получить 0 новых
        """

        inclid = 21
        request = (
            T.BASE_REQUEST.format(inclid=inclid)
            + '&rearr-factors=market_blender_serp_incuts=viewed&debug=1&numdoc=40&use-default-offers=1'
            + '&rearr-factors=market_optimize_default_offers_search_v2=0'
        )
        response = self.report.request_json(request)

        self.assertFragmentIn(
            response, {"logicTrace": [Contains("Found 0 ungrouped DO ts's, 40 ungrouped DO ts's known total")]}
        )

        # С оптимизацией ДО текст сообщения меняется
        request = (
            T.BASE_REQUEST.format(inclid=inclid)
            + '&rearr-factors=market_blender_serp_incuts=viewed&debug=1&numdoc=40&use-default-offers=1'
            + '&rearr-factors=market_optimize_default_offers_search_v2=1'
        )
        response = self.report.request_json(request)

        self.assertFragmentIn(
            response,
            {
                "logicTrace": [
                    Contains("Substituted from DO list 0 ungrouped DO ts's, 40 ungrouped DO ts's known total")
                ]
            },
        )
        self.assertFragmentIn(
            response,
            {"logicTrace": [Contains("Request for ungrouped DO ts's is empty, 40 ungrouped DO ts's known total")]},
        )
        self.assertFragmentIn(
            response,
            {
                "logicTrace": [
                    Contains("No need to explicitly search for ungrouped DO ts's, 40 ungrouped DO ts's known total")
                ]
            },
        )


if __name__ == '__main__':
    main()
