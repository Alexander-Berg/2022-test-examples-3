# -*- coding: utf-8 -*-

from __future__ import unicode_literals, absolute_import

from datetime import date

from travel.avia.library.python.avia_data.models import TopFlight
from travel.avia.library.python.tester.factories import create_settlement

from travel.avia.backend.tests.main.api_test import TestApiHandler


class TestTopFlightsHandler(TestApiHandler):
    def setUp(self):
        super(TestTopFlightsHandler, self).setUp()

        self.s1 = create_settlement()
        self.s2 = create_settlement()
        self.d = date(2015, 10, 21)

    def _create_top_flight(self, redirects=10):
        flight_num = getattr(self, '__flight_num', 1)
        setattr(self, '__flight_num', flight_num + 1)

        return TopFlight.objects.create(
            from_point_key=self.s1.point_key,
            to_point_key=self.s2.point_key,
            day_of_week=2,
            national_version='ru',
            redirects=redirects,
            flights='U6-%d' % flight_num
        )

    def test_simple(self):
        f = self._create_top_flight()

        payload = {
            'name': 'topFlights',
            'params': {
                'fromKey': self.s1.point_key,
                'toKey': self.s2.point_key,
                'date': '2015-10-21',
            },
            'fields': ['numbers', 'redirects'],
        }

        data = self.api_data(payload)

        assert data == self.wrap_expect([{
            'numbers': f.flights,
            'redirects': f.redirects,
        }])
