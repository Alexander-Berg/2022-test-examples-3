#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import GradeDispersionItem, Model, ModelGroup, Offer, PhotoDataItem, ReviewDataItem, ReviewFactorItem
from core.testcase import TestCase, main
from core.blackbox import BlackboxUser


class T(TestCase):

    # start for https://st.yandex-team.ru/MARKETOUT-14487
    @classmethod
    def prepare_default_model_with_opinions(cls):
        """Добавляем модели с отзывами для проверки корректности отзывов в модельном колдунщике"""
        cls.index.models += [
            Model(hyperid=911, hid=30, title='t0 model'),
            Model(hyperid=911404, hid=31, title='noreviews andgrades'),  # model without reviews and grades
        ]

        cls.index.offers += [
            Offer(hyperid=911, title='t0 offer', price=101),
            Offer(
                hyperid=911404, title='noreviews andgrades offer', price=404
            ),  # offer for model without reviews and grades
        ]

        cls.index.model_reviews_data += [
            ReviewDataItem(
                review_id=9110,
                model_id=911,
                author_id=9111,
                region_id=9112,
                cpa=True,
                anonymous=1,
                usage_time=2,
                pro="911pro " + "ы" * 90,
                contra="911contra " + "ы" * 90,
                short_text="911short_text " + "ы" * 90,
                cr_time="2015-10-24T23:08:21",
                rank=905.1,
                agree=3,
                reject=4,
                total_votes=5,
                grade_value=2,
                author_reviews_count=21,
                photos=[
                    PhotoDataItem(group_id="911group_id_11", image_name="911image_name_11"),
                    PhotoDataItem(group_id="911group_id_21"),
                ],
                most_useful=1,
            ),
            ReviewDataItem(review_id=9111, model_id=911, author_id=9111, grade_value=2, freshest=1),
            ReviewDataItem(review_id=9112, model_id=911, author_id=9112, grade_value=3, max_grade_useful=1),
            ReviewDataItem(review_id=9113, model_id=911, author_id=9113, grade_value=4, min_grade_useful=1),
        ]
        cls.index.model_grade_dispersion_data += [
            GradeDispersionItem(
                model_id=911,
                one=1,
                two=2,
                three=3,
                four=4,
                five=5,
                factors=[
                    ReviewFactorItem(factor_id=1, factor_name="first", factor_avg=2.2, count=3),
                    ReviewFactorItem(factor_id=2, factor_name="second", factor_avg=3.3, count=4),
                ],
                recommend_percent=32.543,
            )
        ]

        cls.blackbox.on_request(uids=['9111', '9112', '9113']).respond(
            [
                BlackboxUser(uid='9113', name='', avatar='', public_id=''),
                BlackboxUser(uid='9112', name='name 1', avatar='avatar_1', public_id='public_id_1'),
                BlackboxUser(uid='9111', name='name 2', avatar='avatar_2', public_id='public_id_2'),
            ]
        )

    def test_wizard_model_reviews_data_correct(self):
        request = self.report.request_bs_pb(
            "place=parallel&text=t0+model+отзывы&rearr-factors=showcase_universal=1;wiz_mmc_reviews_group=1;market_enable_model_wizard_right_incut=1"
        )
        self.assertFragmentIn(
            request,
            {
                "market_model_right_incut": {
                    "reviews": [
                        {
                            "author": {
                                "anonymous": "1",
                                "avatar": "avatar_2",
                                "name": "name 2",
                                "publicId": "public_id_2",
                                "reviewsCount": "21",
                                "uid": "9111",
                            },
                            "pictures": [
                                "//avatars.mds.yandex.net/get-market-ugc/911group_id_11/911image_name_11",
                                "//avatars.mds.yandex.net/get-market-ugc/911group_id_21/",
                            ],
                            "rank": "905.1",
                            "reactions": {"dislikesCount": "4", "likesCount": "3"},
                            "reviewData": {
                                "advantages": "911pro ыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыы",
                                "comment": "911short_text ыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыы",
                                "disadvantages": "911contra ыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыыы",
                                "rating": "2",
                                "usageTime": "более года",
                            },
                            "reviewId": "9110",
                            "time": "2015-10-24T20:08:21Z",
                            "url": "//market.yandex.ru/product--t0-model/911/reviews?text=t0%20model%20%D0%BE%D1%82%D0%B7%D1%8B%D0%B2%D1%8B&firstReviewId=9110&clid=632",
                            "urlTouch": "//m.market.yandex.ru/product--t0-model/911/reviews?text=t0%20model%20%D0%BE%D1%82%D0%B7%D1%8B%D0%B2%D1%8B&firstReviewId=9110&clid=704",
                        }
                    ],
                }
            },
        )

    def test_wizard_model_reviews_unknown_group_is_useful(self):
        response = self.report.request_bs_pb(
            "place=parallel&text=t0+model&rearr-factors=showcase_universal=1;wiz_mmc_reviews_group=404;market_enable_model_wizard_right_incut=1"
        )
        self.assertFragmentIn(response, {"market_model_right_incut": {"reviews": [{"reviewId": "9110"}]}})

    def test_wizard_model_reviews_useful(self):
        response = self.report.request_bs_pb(
            "place=parallel&text=t0+model&rearr-factors=showcase_universal=1;wiz_mmc_reviews_group=1;market_enable_model_wizard_right_incut=1"
        )
        self.assertFragmentIn(response, {"market_model_right_incut": {"reviews": [{"reviewId": "9110"}]}})

    def test_wizard_model_reviews_latest(self):
        response = self.report.request_bs_pb(
            "place=parallel&text=t0+model&rearr-factors=showcase_universal=1;wiz_mmc_reviews_group=2;market_enable_model_wizard_right_incut=1"
        )
        self.assertFragmentIn(response, {"market_model_right_incut": {"reviews": [{"reviewId": "9111"}]}})

    def test_wizard_model_reviews_useful_best(self):
        response = self.report.request_bs_pb(
            "place=parallel&text=t0+model&rearr-factors=showcase_universal=1;wiz_mmc_reviews_group=3;market_enable_model_wizard_right_incut=1"
        )
        self.assertFragmentIn(response, {"market_model_right_incut": {"reviews": [{"reviewId": "9112"}]}})

    @classmethod
    def prepare_group_model_with_opinions(cls):
        """Добавляем групповые модели с отзывами для проверки корректности отзывов в модельном колдунщике"""

        # Аналогично для колдунщика групповой модели (проверяем только наличие полей).
        cls.index.model_groups += [ModelGroup(title='checkReviews modelGroup', hyperid=9110)]
        # Добавляем 5 модификаций, чтобы сработал колдунщик групповой модели
        cls.index.models += [
            Model(hyperid=91101, title='checkReviews modelGroup 1', group_hyperid=9110),
            Model(hyperid=91102, title='checkReviews modelGroup 2', group_hyperid=9110),
            Model(hyperid=91103, title='checkReviews modelGroup 3', group_hyperid=9110),
            Model(hyperid=91104, title='checkReviews modelGroup 4', group_hyperid=9110),
            Model(hyperid=91105, title='checkReviews modelGroup 5', group_hyperid=9110),
        ]
        # Добавляем оффер групповой модели, чтобы сработал шаблон model_group_default
        cls.index.offers += [
            Offer(hyperid=9110),
        ]

        cls.index.model_reviews_data += [
            ReviewDataItem(
                review_id=91101,
                model_id=9110,
                most_useful=1,
            ),
        ]
        cls.index.model_grade_dispersion_data += [
            GradeDispersionItem(
                model_id=9110,
                one=6,
                two=7,
                three=8,
                four=9,
                five=10,
                factors=[
                    ReviewFactorItem(factor_id=2, factor_name="firstGroup", factor_avg=4.4, count=6),
                    ReviewFactorItem(factor_id=4, factor_name="secondGroup", factor_avg=6.6, count=8),
                ],
            )
        ]

        # end for https://st.yandex-team.ru/MARKETOUT-14487


if __name__ == '__main__':
    main()
