# -*- coding: utf-8 -*-

from __future__ import unicode_literals, absolute_import

from travel.avia.library.python.avia_data.models import SimilarDirection
from travel.avia.library.python.tester.factories import create_settlement, create_airport

from travel.avia.backend.tests.main.api_test import TestApiHandler


def payload(from_key, to_key, only_with_airports=False):
    return {
        'name': 'similarDirections',
        'params': {
            'fromKey': from_key,
            'toKey': to_key,
            'onlyWithAirports': only_with_airports,
        },
        'fields': [
            {'name': 'fromPoint', 'fields': ['key']},
            {'name': 'toPoint', 'fields': ['key']},
        ],
    }


def similar_direction_response(similar_direction):
    return {
        'fromPoint': {'key': similar_direction.similar_from.point_key},
        'toPoint': {'key': similar_direction.similar_to.point_key},
    }


class TestSimilarDirectionsHandler(TestApiHandler):
    def setUp(self):
        super(TestSimilarDirectionsHandler, self).setUp()

        self.from_city = create_settlement()
        self.to_city = create_settlement()

    def test_simple(self):
        direction_key = '%s_%s' % (self.from_city.point_key, self.to_city.point_key)

        sd1 = SimilarDirection.objects.create(
            direction=direction_key,
            similar_from=create_settlement(),
            similar_to=create_settlement(),
            count=2
        )
        sd2 = SimilarDirection.objects.create(
            direction=direction_key,
            similar_from=create_settlement(),
            similar_to=create_settlement(),
            count=3
        )
        sd3 = SimilarDirection.objects.create(
            direction=direction_key,
            similar_from=create_settlement(),
            similar_to=create_settlement(),
            count=1
        )

        data = self.api_data(payload(
            self.from_city.point_key,
            self.to_city.point_key
        ))

        assert data == self.wrap_expect([
            similar_direction_response(sd2),
            similar_direction_response(sd1),
            similar_direction_response(sd3),
        ])

    def test_similar_directions_only_with_airports(self):
        direction_key = '%s_%s' % (self.from_city.point_key, self.to_city.point_key)

        similar_to = create_settlement()
        another_similar_to = create_settlement()
        airport = create_airport('iata')
        airport.settlement = similar_to
        airport.save()

        sd1 = SimilarDirection.objects.create(
            direction=direction_key,
            similar_from=create_settlement(),
            similar_to=similar_to,
            count=2
        )
        SimilarDirection.objects.create(
            direction=direction_key,
            similar_from=create_settlement(),
            similar_to=another_similar_to,
            count=3
        )

        data = self.api_data(payload(
            self.from_city.point_key,
            self.to_city.point_key,
            only_with_airports=True
        ))

        assert data == self.wrap_expect([
            similar_direction_response(sd1),
        ])
