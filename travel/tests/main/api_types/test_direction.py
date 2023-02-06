# -*- coding: utf-8 -*-

from __future__ import unicode_literals, absolute_import

from travel.avia.library.python.avia_data.models import SimilarDirection
from travel.avia.library.python.tester.factories import create_settlement, create_airport

from travel.avia.backend.tests.main.api_test import TestApiHandler


class TestDirectionHandler(TestApiHandler):
    def setUp(self):
        super(TestDirectionHandler, self).setUp()

        self.from_city = create_settlement(latitude=56.838607, longitude=60.605514)
        self.to_city = create_settlement(latitude=55.030199, longitude=82.92043)

    def test_distance(self):
        payload = {
            'name': 'direction',
            'params': {
                'from_key': self.from_city.point_key,
                'to_key': self.to_city.point_key,
            },
            'fields': ['distance']
        }

        data = self.api_data(payload)

        assert data == self.wrap_expect({
            'distance': 1398,
        })

    def test_similar(self):
        direction_key = '%s_%s' % (self.from_city.point_key, self.to_city.point_key)
        airport_1 = create_airport('iata_1', settlement={})
        airport_2 = create_airport('iata_2', settlement={})

        SimilarDirection.objects.create(
            direction=direction_key,
            similar_from=create_settlement(),
            similar_to=create_settlement(),
            count=2
        )
        SimilarDirection.objects.create(
            direction=direction_key,
            similar_from=create_settlement(),
            similar_to=airport_1.settlement,
            count=3
        )
        SimilarDirection.objects.create(
            direction=direction_key,
            similar_from=create_settlement(),
            similar_to=airport_2.settlement,
            count=1
        )

        payload = {
            'name': 'direction',
            'params': {
                'from_key': self.from_city.point_key,
                'to_key': self.to_city.point_key,
            },
            'fields': [{
                'name': 'similar',
                'params': {
                    'limit': 2
                }
            }]
        }

        data = self.api_data(payload)

        assert len(data['data'][0]['similar']) == 1
