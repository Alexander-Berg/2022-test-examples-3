# -*- coding: utf-8 -*-
from __future__ import unicode_literals, absolute_import

from datetime import date, timedelta

from travel.avia.library.python.common.models.transport import TransportType
from travel.avia.library.python.tester.factories import create_settlement, create_country, create_station
from travel.avia.backend.tests.main.api_test import TestApiHandler
from travel.avia.library.python.tester.factories import create_near_countries, create_min_price


def tomorrow():
    return date.today() + timedelta(1)


def date_future(delta=10):
    return date.today() + timedelta(delta)


PLANE_TYPE = TransportType.objects.get(code='plane')


def create_preload(from_city, to_country):
    return {
        'name': 'neighbourCountryDirections',
        'params': {
            'from_key': from_city.point_key,
            'to_key': to_country.point_key,
            'when': tomorrow().isoformat(),
            'return_date': date_future().isoformat()
        }
    }


class TestNeighboursCountryDirectionHandler(TestApiHandler):
    def setUp(self):
        super(TestNeighboursCountryDirectionHandler, self).setUp()

        self.from_city = create_settlement()
        self.to_country = create_country()

    def test_one_direction(self):
        russia = create_country()
        moscow = create_settlement(country=russia)
        create_station(country=russia, settlement=moscow, t_type=PLANE_TYPE)

        usa = create_country()
        boston = create_settlement(country=usa)
        create_station(country=usa, settlement=boston, t_type=PLANE_TYPE)

        create_min_price(
            departure_settlement=self.from_city,
            arrival_settlement=moscow,
            price=100,
            passengers='1_0_0'
        )
        create_min_price(
            departure_settlement=self.from_city,
            arrival_settlement=boston,
            price=200,
            passengers='1_0_0'
        )

        create_near_countries(country=self.to_country, __={
            'neighbours': [
                russia,
                usa
            ]
        })

        response = self.api_data(create_preload(self.from_city, self.to_country))

        contract = response['data'][0]

        assert len(contract) == 2

        actual_data = contract[0]
        assert actual_data['fromCity']['id'] == self.from_city.id
        assert actual_data['toCountry']['id'] == russia.id
        assert actual_data['price']['value'] == 100

        actual_data = contract[1]
        assert actual_data['fromCity']['id'] == self.from_city.id
        assert actual_data['toCountry']['id'] == usa.id
        assert actual_data['price']['value'] == 200
