#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Offer
from core.testcase import TestCase, main
from core.dj import DjTextQuery, DjTextQueryFactor
from core.matcher import Absent, NotEmpty, Regex


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.set_default_reqid = False

    @classmethod
    def prepare_text_queries_recommendations(cls):
        cls.index.offers += [
            Offer(title='красный iphone xs 64Mb'),
        ]

        cls.text_query_dj.on_request(
            yandexuid='3227401', exp='market_query_recommender', text='красный iphone'
        ).respond(
            recommended_queries=[
                DjTextQuery(
                    text='iphone красный',
                    factors=[
                        DjTextQueryFactor(name='sum_top_3_dssm_score', value='0.945927'),
                        DjTextQueryFactor(name='serp_similarity_dssm_score', value='0.968848'),
                        DjTextQueryFactor(name='relevance', value='0.314542'),
                    ],
                ),
                DjTextQuery(
                    text='iphone xs',
                    factors=[
                        DjTextQueryFactor(name='sum_top_3_dssm_score', value='0.942327'),
                        DjTextQueryFactor(name='query_dssm_score_3', value='0.968848'),
                        DjTextQueryFactor(name='relevance', value='0.310242'),
                    ],
                ),
                DjTextQuery(
                    text='iphone x',
                    factors=[
                        DjTextQueryFactor(name='sum_top_3_dssm_score', value='0.243927'),
                        DjTextQueryFactor(name='serp_similarity_dssm_score', value='0.968848'),
                        DjTextQueryFactor(name='relevance', value='0.314542'),
                    ],
                ),
                DjTextQuery(
                    text='iphone xs красный',
                    factors=[
                        DjTextQueryFactor(name='sum_top_3_dssm_score', value='0.945927'),
                        DjTextQueryFactor(name='query_dssm_score_4', value='0.968848'),
                        DjTextQueryFactor(name='relevance', value='0.310098'),
                    ],
                ),
                DjTextQuery(
                    text='iphone xs черный',
                    factors=[
                        DjTextQueryFactor(name='sum_top_3_dssm_score', value='0.945927'),
                        DjTextQueryFactor(name='sf_one_se_dssm_score', value='0.968848'),
                        DjTextQueryFactor(name='relevance', value='0.304542'),
                    ],
                ),
            ]
        )

        cls.text_query_dj.on_request(yandexuid='3227401', exp='market_query_recommender_v4', text='red iphone').respond(
            recommended_queries=[
                DjTextQuery(
                    text='iphone xs',
                    factors=[
                        DjTextQueryFactor(name='sum_top_3_dssm_score', value='0.942327'),
                        DjTextQueryFactor(name='query_dssm_score_3', value='0.968848'),
                        DjTextQueryFactor(name='relevance', value='0.310242'),
                    ],
                ),
                DjTextQuery(
                    text='iphone x',
                    factors=[
                        DjTextQueryFactor(name='sum_top_3_dssm_score', value='0.243927'),
                        DjTextQueryFactor(name='serp_similarity_dssm_score', value='0.968848'),
                        DjTextQueryFactor(name='relevance', value='0.314542'),
                    ],
                ),
                DjTextQuery(
                    text='iphone красный',
                    factors=[
                        DjTextQueryFactor(name='sum_top_3_dssm_score', value='0.945927'),
                        DjTextQueryFactor(name='serp_similarity_dssm_score', value='0.968848'),
                        DjTextQueryFactor(name='relevance', value='0.314542'),
                    ],
                ),
                DjTextQuery(
                    text='iphone xs черный',
                    factors=[
                        DjTextQueryFactor(name='sum_top_3_dssm_score', value='0.945927'),
                        DjTextQueryFactor(name='sf_one_se_dssm_score', value='0.968848'),
                        DjTextQueryFactor(name='relevance', value='0.304542'),
                    ],
                ),
                DjTextQuery(
                    text='iphone xs красный',
                    factors=[
                        DjTextQueryFactor(name='sum_top_3_dssm_score', value='0.945927'),
                        DjTextQueryFactor(name='query_dssm_score_4', value='0.968848'),
                        DjTextQueryFactor(name='relevance', value='0.310098'),
                    ],
                ),
            ]
        )

        cls.text_query_dj.on_request(yandexuid='3227401', exp='market_query_recommender', text='белый iphone').respond(
            recommended_queries=[]
        )

    def test_text_queries_recommendations(self):
        '''Проверяем выдачу по умолчанию
        Рекомендации, повторяющие исходный запрос
        не показываются
        '''

        request = 'place=prime&text=красный+iphone&rearr-factors=market_dj_exp_for_text_queries=market_query_recommender&yandexuid=3227401&additional_entities=recommended_queries&touch=1'
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "recommendedQueries": [
                    {
                        "raw": "iphone xs",
                        "showUid": NotEmpty(),
                        "highlighted": [{"value": "iphone", "highlight": True}, {"value": " xs"}],
                    },
                    {
                        "raw": "iphone x",
                        "showUid": NotEmpty(),
                        "highlighted": [{"value": "iphone", "highlight": True}, {"value": " x"}],
                    },
                    {
                        "raw": "iphone xs красный",
                        "showUid": NotEmpty(),
                        "highlighted": [
                            {"value": "iphone", "highlight": True},
                            {"value": " xs "},
                            {"value": "красный", "highlight": True},
                        ],
                    },
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        self.feature_log.expect(
            factors_names='dj_sum_top_3_dssm_score dj_query_dssm_score_3 dj_relevance',
            factors='0.942327 0.968848 0.310242 ',
            position=1,
        )
        self.feature_log.expect(
            factors_names='dj_sum_top_3_dssm_score dj_serp_similarity_dssm_score dj_relevance',
            factors='0.243927 0.968848 0.314542 ',
            position=2,
        )
        self.feature_log.expect(
            factors_names='dj_sum_top_3_dssm_score dj_query_dssm_score_4 dj_relevance',
            factors='0.945927 0.968848 0.310098 ',
            position=3,
        )

    def test_default_experiment(self):
        '''Проверяем дефолтный эксперимент market_query_recommender_v4'''

        request = (
            'place=prime&text=red+iphone&yandexuid=3227401&additional_entities=recommended_queries&touch=1&debug=1'
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "recommendedQueries": [
                    {
                        "raw": "iphone xs",
                    },
                    {
                        "raw": "iphone x",
                    },
                    {
                        "raw": "iphone красный",
                    },
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )
        self.assertFragmentIn(
            response,
            {
                "logicTrace": [
                    Regex(
                        r'[ME].*GetDjTextQueryParams\(\)\: .*Creating request to.*with yandexuid = "3227401", exp = "market_query_recommender_v4".*'
                    )
                ]
            },
        )

    def test_text_queries_empty_recommendations(self):
        '''Проверяем случаи, когда рекомендации
        не показываются
        '''

        # Для запроса нет рекомендаций
        request = 'place=prime&text=белый+iphone&rearr-factors=market_dj_exp_for_text_queries=market_query_recommender&yandexuid=3227401&additional_entities=recommended_queries'
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"recommendedQueries": Absent()}, allow_different_len=False)

        # Эксперимент выключен
        request = 'place=prime&text=красный+iphone&rearr-factors=market_dj_exp_for_text_queries=&yandexuid=3227401&additional_entities=recommended_queries'
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"recommendedQueries": Absent()}, allow_different_len=False)

    def test_desktop(self):
        request = 'place=prime&text=красный+iphone&rearr-factors=market_dj_exp_for_text_queries=market_query_recommender&yandexuid=3227401&additional_entities=recommended_queries'
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "recommendedQueries": [
                    {
                        "raw": "iphone xs",
                    },
                    {
                        "raw": "iphone x",
                    },
                    {
                        "raw": "iphone xs красный",
                    },
                    {
                        "raw": "iphone xs черный",
                    },
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()
