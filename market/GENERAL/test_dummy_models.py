#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import HyperCategory, HyperCategoryType, MnPlace, Model, Offer, Picture, Shop
from core.matcher import Absent, ElementCount


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_new_cpm_iterator=0']
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.hypertree += [
            HyperCategory(hid=1, output_type=HyperCategoryType.GURU, show_offers=True),
            HyperCategory(hid=2, output_type=HyperCategoryType.GURU, show_offers=True),
        ]

        cls.index.models += [
            Model(hyperid=101, hid=1, title='GURU model', ts=10100),
            Model(hyperid=102, hid=1, title='GURUDUMMY model', ts=10200, is_guru_dummy=True),
            Model(hyperid=103, hid=2, title='GURU model from other category', ts=10300),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 10100).respond(0.6)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 10200).respond(0.5)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 10300).respond(0.4)

        cls.index.shops += [Shop(fesh=1, priority_region=213), Shop(fesh=2, priority_region=213)]

        cls.index.offers += [
            Offer(title='offer 1 for GURU model', hyperid=101, fesh=1),
            Offer(title='offer 2 for GURU model', hyperid=101, fesh=2),
            Offer(title='offer 1 for GURUDUMMY model', hyperid=102, fesh=1),
            Offer(
                title='offer 2 for GURUDUMMY model',
                hyperid=102,
                fesh=2,
                picture=Picture(picture_id='uS6z5i755IOLmUXx1CKyOQ', width=100, height=100, group_id=1),
            ),
            # Для проверки, что не схлопываются офферы для одной модели из одного магазина.
            # Но обязательно надо указать картинку, т.к иначе дубли с одинаковыми картинками отсеиваются в TFilterByPicture
            Offer(
                title='offer 3 for GURUDUMMY model',
                hyperid=102,
                fesh=2,
                picture=Picture(picture_id='oxGBOkyJnAGkH5rdYHRSQw', width=100, height=100, group_id=1),
            ),
            Offer(title='offer 1 for model 103', hyperid=103, fesh=1),
        ]

    def test_filtered_out_dummy_model(self):
        '''Проверка, что без show_dummy_models GURU_DUMMY модели игнорятся, а их офферы не привязываются к моделям'''

        response = self.report.request_json(
            'debug=1&place=prime&hid=1&allow-collapsing=1&rids=213' '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 4,
                    'totalOffers': 3,
                    'totalModels': 1,
                    'results': [
                        {
                            'entity': 'product',
                            'titles': {'raw': 'GURU model'},
                            'offers': {'count': 2},
                        },
                        {
                            'entity': 'offer',
                            'titles': {'raw': 'offer 1 for GURUDUMMY model'},
                            'model': Absent(),
                            'shop': {'id': 1},
                        },
                        {
                            'entity': 'offer',
                            'titles': {'raw': 'offer 2 for GURUDUMMY model'},
                            'model': Absent(),
                            'shop': {'id': 2},
                        },
                        {
                            'entity': 'offer',
                            'titles': {'raw': 'offer 3 for GURUDUMMY model'},
                            'model': Absent(),
                            'shop': {'id': 2},
                        },
                    ],
                },
                'debug': {'brief': {'filters': {'MODEL_IS_DUMMY': 1}}},
            },
            allow_different_len=False,
        )

    def test_dummy_model_in_prime_result(self):
        '''Проверка, что c show_dummy_models GURU_DUMMY модели показываются,
        и офферы к ним привязываются
        '''

        response = self.report.request_json(
            'debug=1&place=prime&hid=1&allow-collapsing=1&rids=213&rearr-factors=show_dummy_models=1'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 2,
                    'totalOffers': 0,
                    'totalModels': 2,
                    'results': [
                        {
                            'entity': 'product',
                            'titles': {'raw': 'GURU model'},
                            'offers': {'count': 2},
                        },
                        {
                            'entity': 'product',
                            'titles': {'raw': 'GURUDUMMY model'},
                            'offers': {'count': 3},
                        },
                    ],
                },
                'debug': {'brief': {'filters': ElementCount(0)}},
            },
        )

    def test_productoffers(self):
        req = 'place=productoffers&hyperid=102&rids=213'
        for exp in ('', '&rearr-factors=show_dummy_models=1'):
            response = self.report.request_json(req + exp)
            self.assertFragmentIn(
                response,
                {
                    'totalOffers': 3,
                    'results': [
                        {
                            'entity': 'offer',
                            'model': {'id': 102},
                            'titles': {'raw': 'offer 1 for GURUDUMMY model'},
                            'shop': {'id': 1},
                        },
                        {
                            'entity': 'offer',
                            'model': {'id': 102},
                            'titles': {'raw': 'offer 2 for GURUDUMMY model'},
                            'shop': {'id': 2},
                        },
                        {
                            'entity': 'offer',
                            'model': {'id': 102},
                            'titles': {'raw': 'offer 3 for GURUDUMMY model'},
                            'shop': {'id': 2},
                        },
                    ],
                },
            )

    def test_modelinfo(self):
        req = 'place=modelinfo&hyperid=101&hyperid=102&rids=213'
        for exp in (True, False):
            response = self.report.request_json(req + ('&rearr-factors=show_dummy_models=1' if exp else ''))
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'product',
                            'id': 101,
                            'titles': {'raw': 'GURU model'},
                            'categories': [{'id': 1, 'isLeaf': True}],
                            'offers': {'count': 2},
                        },
                        {
                            'entity': 'product',
                            'id': 102,
                            'titles': {'raw': 'GURUDUMMY model'},
                            'categories': [{'id': 1, 'isLeaf': True}],
                            'offers': {'count': 3},
                        },
                    ]
                },
            )

    def wizard_title_text_obj(self, title):
        return {'title': {'text': {'__hl': {'text': title}}}}

    def test_filtered_out_implicit_models_in_parallel(self):
        response = self.report.request_bs('place=parallel&text=model&rearr-factors=market_parallel_feature_log_rate=1')
        self.assertFragmentIn(
            response,
            {
                'market_implicit_model': [
                    {
                        'model_count': '2',
                        'offer_count': 6,
                        'showcase': {
                            'items': [
                                self.wizard_title_text_obj('GURU model'),
                                self.wizard_title_text_obj('GURU model from other category'),
                            ]
                        },
                    }
                ],
                'market_offers_wizard': [{'offer_count': 6}],
            },
            preserve_order=True,
            allow_different_len=False,
        )

        self.feature_log.expect(model_id=101, position=1)
        self.feature_log.expect(model_id=103, position=2)

    def test_dummy_implicit_models_in_parallel(self):
        response = self.report.request_bs(
            'place=parallel&text=model&rearr-factors=show_dummy_models=1;market_parallel_feature_log_rate=1'
        )
        self.assertFragmentIn(
            response,
            {
                'market_implicit_model': [
                    {
                        'model_count': '3',
                        'offer_count': 6,
                        'showcase': {
                            'items': [
                                self.wizard_title_text_obj('GURU model'),
                                self.wizard_title_text_obj('GURUDUMMY model'),
                                self.wizard_title_text_obj('GURU model from other category'),
                            ]
                        },
                    }
                ],
                'market_offers_wizard': [{'offer_count': 6}],
            },
            preserve_order=True,
            allow_different_len=False,
        )

        self.feature_log.expect(model_id=101, position=1)
        self.feature_log.expect(model_id=102, position=2)
        self.feature_log.expect(model_id=103, position=3)

    def test_not_implicit_models_in_parallel(self):
        req = 'place=parallel&text=GURUDUMMY'
        response = self.report.request_bs(req)
        self.assertFragmentIn(
            response,
            {'market_implicit_model': Absent(), 'market_model': Absent(), 'market_offers_wizard': [{'offer_count': 3}]},
        )

        response = self.report.request_bs(req + '&rearr-factors=show_dummy_models=1')
        self.assertFragmentIn(
            response,
            {
                'market_implicit_model': Absent(),  # должно быть не менее 2 моделей
                'market_model': [
                    {
                        'title': {"__hl": {"text": "GURUDUMMY model", "raw": True}},
                        'categoryId': 1,
                        'showcase': {
                            "items": ElementCount(2)
                        },  # найденно 3 оффера, но 2 из них из одного магазина, а такие схлопываются
                    }
                ],
                'market_offers_wizard': [{'offer_count': 3}],
            },
            allow_different_len=False,
        )

    def test_skip_dummy_model_docs(self):
        '''Проверка флага skip_dummy_model_docs_at_text_search. При нем на текстовом поиске должны отсеиваться документы-модели.
        Но при этом сами модели не должны пропасть из выдачи, а образоваться из схлопнутых офферов.
        '''

        def gen_req(allow_collapsing, skip_dummy_model_doc):
            req = 'place=prime&text=model&hid=1&allow-collapsing={}&rids=213&debug=1&rearr-factors=show_dummy_models=1;market_metadoc_search=no'.format(
                allow_collapsing
            )
            return req + ';skip_dummy_model_docs_at_text_search=1' if skip_dummy_model_doc else req

        product_101_json = {
            'entity': 'product',
            'slug': 'guru-model',
            'id': 101,
            'offers': {'count': 2},
            'debug': {'isCollapsed': False},
        }

        for allow_collapsing in (1, 0):
            for skip_dummy_model_doc in (False, True):
                response = self.report.request_json(gen_req(allow_collapsing, skip_dummy_model_doc))
                if skip_dummy_model_doc:
                    filter_debug = {'debug': {'brief': {'filters': {'MODEL_IS_DUMMY': 1}}}}
                    self.assertFragmentIn(response, filter_debug)

                # модель 101 будет всегда, а модель 102 пропадет при skip_dummy_model_docs_at_text_search=1 + allow-collapsing=0
                total_models = 2 if not skip_dummy_model_doc or allow_collapsing else 1
                total_offers = 5 if not allow_collapsing else 0

                self.assertFragmentIn(
                    response,
                    {
                        'total': total_models + total_offers,
                        'totalModels': total_models,
                        'totalOffers': total_offers,
                        'results': ElementCount(total_models + total_offers),
                    },
                )
                if total_models == 2:
                    self.assertFragmentIn(
                        response,
                        [
                            product_101_json,
                            {
                                'entity': 'product',
                                'slug': 'gurudummy-model',
                                'id': 102,
                                'offers': {'count': 3},
                                'debug': {'isCollapsed': skip_dummy_model_doc},
                            },
                        ],
                    )
                else:
                    self.assertFragmentIn(response, product_101_json)
                    self.assertFragmentNotIn(response, {'entity': 'product', 'id': 102})


if __name__ == '__main__':
    main()
