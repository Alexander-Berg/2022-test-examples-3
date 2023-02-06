# -*- coding: utf-8 -*-

from __future__ import unicode_literals, absolute_import
from datetime import date, timedelta

from travel.avia.library.python.avia_data.models import AviaDirectionNational, MinPrice
from travel.avia.library.python.common.models.currency import Currency
from travel.avia.library.python.tester.factories import create_settlement, create_country, create_airport

from travel.avia.backend.tests.main.api_test import TestApiHandler


def tomorrow():
    return date.today() + timedelta(1)


def date_future(delta=10):
    return date.today() + timedelta(delta)


def create_preload(from_city, to_country):
    return {
        'name': 'countryDirections',
        'params': {
            'from_key': from_city.point_key,
            'to_key': to_country.point_key,
            'when': tomorrow().isoformat(),
            'return_date': date_future().isoformat(),
            'adult_seats': 1,
            'children_seats': 0,
            'infant_seats': 0
        }
    }


def create_direction(from_city, to_city, price, currency):
    AviaDirectionNational.objects.create(
        departure_settlement=from_city,
        arrival_settlement=to_city,
        national_version='ru'
    )

    MinPrice.objects.create(
        departure_settlement=from_city,
        arrival_settlement=to_city,
        price=price,
        currency=currency,
        date_forward=tomorrow(),
        date_backward=date_future(),
        passengers='1_0_0',
        direct_flight=True
    )


class TestCountryDirectionHandler(TestApiHandler):
    def setUp(self):
        super(TestCountryDirectionHandler, self).setUp()

        self.from_city = create_settlement()
        self.to_country = create_country()

        self.currency = Currency.objects.create(
            name='Рубли', code='RUR', iso_code='RUB',
            template='t', template_whole='t', template_cents='t',
            template_tr='t', template_whole_tr='t', template_cents_tr='t'
        )

    def test_one_direction(self):
        countries_city = create_settlement(country=self.to_country)
        create_direction(self.from_city, countries_city, 12500, self.currency)

        response = self.api_data(create_preload(self.from_city, self.to_country))

        contract = response['data'][0]

        assert len(contract) == 1

        actual_data = contract[0]

        assert actual_data['fromCity']['id'] == self.from_city.id
        assert actual_data['toCity']['id'] == countries_city.id
        assert actual_data['directPrice']['value'] == 12500

    def test_two_direction(self):
        countries_city = create_settlement(country=self.to_country)
        another_countries_city = create_settlement(country=self.to_country)
        create_direction(self.from_city, countries_city, 12500, self.currency)
        create_direction(self.from_city, another_countries_city, 15000, self.currency)

        response = self.api_data(create_preload(self.from_city, self.to_country))

        contract = response['data'][0]

        assert len(contract) == 2

        actual_data = contract[0]
        assert actual_data['fromCity']['id'] == self.from_city.id
        assert actual_data['toCity']['id'] == countries_city.id
        assert actual_data['directPrice']['value'] == 12500

        actual_data = contract[1]
        assert actual_data['fromCity']['id'] == self.from_city.id
        assert actual_data['toCity']['id'] == another_countries_city.id
        assert actual_data['directPrice']['value'] == 15000

    def test_directions_only_with_airports(self):
        countries_city = create_settlement(country=self.to_country)
        another_countries_city = create_settlement(country=self.to_country)
        create_direction(self.from_city, countries_city, 12500, self.currency)
        create_direction(self.from_city, another_countries_city, 15000, self.currency)
        airport = create_airport('iata')
        airport.settlement = countries_city
        airport.save()

        payload = create_preload(self.from_city, self.to_country)
        payload['params']['only_with_airports'] = True
        response = self.api_data(payload)

        contract = response['data'][0]

        assert len(contract) == 1

        actual_data = contract[0]
        assert actual_data['toCity']['id'] == countries_city.id
