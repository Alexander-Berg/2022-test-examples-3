# -*- coding: utf-8 -*-
from __future__ import absolute_import

from datetime import datetime, timedelta

from travel.avia.library.python.avia_data.models import GoodPrice
from travel.avia.backend.tests.main.api_test import TestApiHandler


class TestGoodPriceHandler(TestApiHandler):
    def setUp(self):
        super(TestGoodPriceHandler, self).setUp()
        self.days = 4
        self.departure = (datetime.now() + timedelta(days=self.days))
        self.price = GoodPrice(
            point_from_type='c',
            point_from_id=213,
            point_to_type='c',
            point_to_id=146,
            route_uid='U6 2841',
            days_to_flight=self.days - 1,
            departure_weekday=self.departure.weekday(),
            q33=33.0,
            q67=67.0,
        )
        self.price.save()
        self.expected_price = {'q33': 33.0, 'q67': 67.0}

    def test_no_good_price(self):
        payload = self.get_payload(point_key='c10')
        data = self.api_data(payload)
        assert data == self.wrap_expect(None)

    def test_get_good_price(self):
        payload = self.get_payload()
        data = self.api_data(payload)
        assert data == self.wrap_expect(self.expected_price)

    def test_date_format(self):
        # (RASPTICKETS-17963) check compatibility with marshmallow==2.4.0
        payload = self.get_payload(departure=self.departure.strftime('%Y-%m-%d'))
        data = self.api_data(payload)
        assert data == self.wrap_expect(self.expected_price)

    def get_payload(self, point_key=None, departure=None):
        return {
            'name': 'goodPrice',
            'params': {
                'from_point_key': point_key or 'c213',
                'to_point_key': 'c146',
                'departure': departure or self.departure.isoformat(),
                'routes': 'U6 2841',
                'adult_seats': '1',
                'children_seats': '0',
                'infant_seats': '0',
            },
        }
