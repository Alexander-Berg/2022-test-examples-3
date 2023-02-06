# -*- coding: utf-8 -*-
from __future__ import unicode_literals, absolute_import

from datetime import datetime

from travel.avia.backend.tests.main.api_test import TestApiHandler
from travel.avia.library.python.tester.factories import create_review, create_flight_number, create_company

import pytz


class TestReviewListHandler(TestApiHandler):
    def setUp(self):
        super(TestReviewListHandler, self).setUp()

        flight_numbers = [create_flight_number(flight_number='flight-' + str(i)) for i in range(0, 3)]
        self.reviews = [create_review(
            enable_show=True,
            __={'flight_numbers': [
                flight_numbers[2],
                flight_numbers[i]
            ]},
            review_url='https://avia.yandex.ru',
            review_datetime=datetime(2016, (12 - i), 10, 23, 14, 40)
        ) for i in range(0, 2)]

    def test_defaults(self):
        payload = {
            'name': 'reviewList',
            'params': {
                'flight_numbers': [
                    'flight-0',
                    'flight-2'
                ]
            }
        }

        data = self.api_data(payload)

        assert data == self.wrap_expect({
            'reviewList': [{
                'id': int(review.id),
                'flightNumbers': [fn.flight_number for fn in review.flight_numbers.all()],
                'reviewContent': review.review_content,
                'authorName': review.author_name,
                'reviewDatetime': unicode(pytz.utc.localize(review.review_datetime).isoformat()),
                'reviewUrl': 'https://avia.yandex.ru',
                'source': u''
            } for review in self.reviews]
        })


class TestAirlineReviewsHandler(TestApiHandler):
    def setUp(self):
        super(TestAirlineReviewsHandler, self).setUp()

        flight_numbers = [create_flight_number(flight_number='flight-' + str(i)) for i in range(0, 2)]
        airlines = [create_company() for i in range(0, 2)]

        self.airlines = airlines
        self.reviews = [
            create_review(
                enable_show=True,
                airline=airlines[0]
            ),
            create_review(
                enable_show=True,
                airline=airlines[0],
                __={'flight_numbers': [flight_numbers[0]]}
            ),
            create_review(
                enable_show=True,
                airline=airlines[0],
                __={'flight_numbers': [flight_numbers[1]]}
            ),
            create_review(
                enable_show=True,
                airline=airlines[1],
                __={'flight_numbers': [flight_numbers[0]]}
            ),
        ]

    def test_defaults(self):
        payload = {
            'name': 'airlineReviews',
            'params': {
                'airline_id': self.airlines[0].id,
                'exclude_flight_numbers': ['flight-1'],
            }
        }

        data = self.api_data(payload)
        reviews = data['data'][0].get('reviewList', [])

        assert len(reviews) == 2
        assert reviews[0]['id'] == self.reviews[0].id
        assert reviews[1]['id'] == self.reviews[1].id

    def test_limit(self):
        payload = {
            'name': 'airlineReviews',
            'params': {
                'limit': 1,
                'airline_id': self.airlines[1].id,
            }
        }

        data = self.api_data(payload)
        reviews = data['data'][0].get('reviewList', [])

        assert len(reviews) == 1
        assert reviews[0]['id'] == self.reviews[3].id
