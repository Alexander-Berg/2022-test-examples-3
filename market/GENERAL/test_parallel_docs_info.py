#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa


from core.types import MnPlace, Model, Offer
from core.testcase import TestCase, main

from core.matcher import NotEmpty


class T(TestCase):
    @classmethod
    def prepare_doc_props(cls):
        """Создаем модели и оферы для проверки их properties
        https://st.yandex-team.ru/MARKETOUT-26013
        """

        doc_count = 30
        cls.index.models += [
            Model(hyperid=280 + i, title='props model {}'.format(i), hid=60 + i) for i in range(doc_count)
        ]

        cls.index.offers += [
            Offer(title='props offer {}'.format(i), url='http://props.ru/{}'.format(i), hid=60 + i)
            for i in range(doc_count)
        ]

        cls.matrixnet.on_default_place(MnPlace.BASE_SEARCH).respond(
            None
        )  # None выключает дефолтное значение для базовой формулы, чтобы значение считалось, а не мокалось

    def test_doc_properties(self):
        """Проверяем, что в дебаге добавляются properties для каждого документа
        и что в них, если считается, есть MATRIXNET_ASSESSOR_VALUE
        https://st.yandex-team.ru/MARKETOUT-26013
        """

        request = (
            'place=parallel&text=props&debug=1&rearr-factors='
            'market_model_search_mn_algo=TESTALGO_combined;'
            'market_search_mn_algo=TESTALGO_combined;'
            'market_product_request_threshold=0;'
        )

        response = self.report.request_bs_pb(request)
        searcher_props = response.get_searcher_props()

        self.assertIn('Market.Debug.hyperModelFactors', searcher_props)
        self.assertIn('MATRIXNET_ASSESSOR_VALUE', searcher_props['Market.Debug.hyperModelFactors'])
        self.assertIn('MATRIXNET_CLICK_VALUE', searcher_props['Market.Debug.hyperModelFactors'])
        self.assertIn('MATRIXNET_SUM_VALUE', searcher_props['Market.Debug.hyperModelFactors'])

        response = self.report.request_bs(request)
        self.assertFragmentIn(
            response,
            {
                'hyperModelFactors': [
                    {
                        'properties': {
                            'MATRIXNET_ASSESSOR_VALUE': NotEmpty(),
                            'MATRIXNET_CLICK_VALUE': NotEmpty(),
                            'MATRIXNET_SUM_VALUE': NotEmpty(),
                        }
                    }
                ],
            },
        )


if __name__ == '__main__':
    main()
