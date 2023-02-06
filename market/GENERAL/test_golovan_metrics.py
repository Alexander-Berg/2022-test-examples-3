#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import HyperCategory, HyperCategoryType, Model, Offer, Shop
from core.bigb import WeightedValue, BigBKeyword
from core.dj import DjModel

DEFAULT_PROFILE = [
    BigBKeyword(
        id=BigBKeyword.GENDER,
        weighted_uint_values=[
            WeightedValue(value=BigBKeyword.GENDER_MALE, weight=621947),
            WeightedValue(value=BigBKeyword.GENDER_FEMALE, weight=375515),
        ],
    ),
]


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.set_default_reqid = False

    @classmethod
    def prepare_test_response_document_stats_presence(cls):
        cls.index.shops += [Shop(fesh=1, priority_region=66), Shop(fesh=2, priority_region=213)]

        cls.index.offers += [Offer(fesh=1, hid=100), Offer(fesh=2, hid=100)]

    def test_response_document_stats_presence(self):
        self.report.request_json('place=prime&hid=100&rids=66')

        response = self.report.request_tass()
        for signal in [
            'total_documents_processed_hgram',
            'total_documents_accepted_hgram',
            'total_documents_delivery_filtered_hgram',
            'total_documents_gurulight_filtered_hgram',
            'total_documents_before_accept_hgram',
            'total_documents_after_accept_hgram',
        ]:
            self.assertIn(signal, response)

    @classmethod
    def prepare_extservice_stats_presence(cls):
        cls.index.hypertree += [
            HyperCategory(hid=1832400, name='Телевизоры', output_type=HyperCategoryType.GURU),
        ]

        cls.index.models += [
            Model(hyperid=1832401, hid=1832400),
            Model(hyperid=1832402, hid=1832400),
            Model(hyperid=1832403, hid=1832400),
            Model(hyperid=1832404, hid=1832400),
        ]

        cls.index.offers += [Offer(hyperid=1832401), Offer(hyperid=1832401)]

        cls.fast_dj.on_request(exp="white_attractive_models", yandexuid='001').respond(
            [
                DjModel(id=1832403, title='title'),
                DjModel(id=1832404, title='title'),
                DjModel(id=1832402, title='title'),
                DjModel(id=1832401, title='title'),
            ]
        )

    def test_extservice_stats_presence(self):
        self.report.request_json('place=attractive_models&hid=1832400&yandexuid=001&page=1&numdoc=3&debug=1')

        response = self.report.request_tass()
        for signal in [
            'Dj_recommender_query_size_dmmm',
            'Dj_recommender_query_time_hgram',
            'Dj_recommender_resp_size_dmmm',
            'Dj_recommender_response_count_dmmm',
            'dj_request_time_hgram',
        ]:
            self.assertIn(signal, response.keys())


if __name__ == '__main__':
    main()
