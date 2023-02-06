#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.blackbox import BlackboxUser
from core.matcher import Contains


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.blackbox.on_request(uids=['308503668']).respond(
            [
                BlackboxUser(
                    uid='308503668', name='John Smith', avatar='avatar_john_smith', public_id='public_id_john_smith'
                ),
            ]
        )

    def test_blackbox_request(self):
        """https://st.yandex-team.ru/MARKETOUT-33204
        Testing that blackbox data is showed in parallel_data output
        """
        response = self.report.request_parallel_data('place=parallel_data&text=test')
        self.assertFragmentIn(
            response,
            {
                'opinions': {
                    'opinions': [
                        {
                            'author': {
                                'uid': 308503668,
                                'avatar': 'avatar_john_smith',
                                'name': 'John Smith',
                                'publicId': 'public_id_john_smith',
                                'anonymous': 0,
                            }
                        }
                    ]
                }
            },
        )

    def test_clid_in_market_urls(self):
        """https://st.yandex-team.ru/MARKETOUT-34579
        Test optinion url to market
        """
        response = self.report.request_parallel_data('place=parallel_data&text=test&hyperid=1')
        self.assertFragmentIn(response, {'opinions': {'opinions': [{'url': Contains("clid=632")}]}})
        self.assertFragmentIn(response, {'articles': {'articles': [{'url': Contains("clid=632")}]}})

    def test_correct_review_url(self):
        """https://st.yandex-team.ru/MARKETOUT-38005
        Проверяем что урл на отзыв имеет ожидаемый формат и корректен
        """
        response = self.report.request_parallel_data('place=parallel_data&text=test&hyperid=1')
        self.assertFragmentIn(
            response,
            {
                'opinions': {
                    'opinions': [{'url': '//market.yandex.ru/product/1/reviews?clid=632&firstReviewId=92879945'}]
                }
            },
        )


if __name__ == '__main__':
    main()
