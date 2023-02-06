# -*- coding: utf-8 -*-
from __future__ import unicode_literals, absolute_import

from datetime import date, timedelta

from travel.avia.library.python.avia_data.models import (
    AviaRecipe, AviaDirectionNational, MinPrice, CurrencyTranslation, CurrencyLang,
    Currency as AviaCurrency
)
from travel.avia.library.python.common.models.currency import Currency
from travel.avia.library.python.common.models.geo import Settlement
from travel.avia.library.python.common.models.transport import TransportType
from travel.avia.library.python.tester.factories import create_settlement, create_station, create_settlement_image

from travel.avia.backend.repository import currency_repository, currency_translation_repository
from travel.avia.backend.tests.main.api_test import TestApiHandler


def tomorrow():
    return date.today() + timedelta(1)


def date_future(delta=10):
    return date.today() + timedelta(delta)


def prepare_simple_test(currency, has_airport=True):
    from_city = create_settlement()
    to_city = create_settlement()
    if has_airport:
        create_station(settlement=to_city, t_type=TransportType.PLANE_ID)

    AviaDirectionNational.objects.create(
        departure_settlement=from_city,
        arrival_settlement=to_city,
        national_version='ru',
        direct_flights=10
    )

    p = MinPrice.objects.create(
        departure_settlement=from_city,
        arrival_settlement=to_city,
        price=12500,
        currency=currency,
        date_forward=tomorrow(),
        date_backward=date_future(),
        passengers='1_0_0',
        direct_flight=True
    )

    p2 = MinPrice.objects.create(
        departure_settlement=from_city,
        arrival_settlement=to_city,
        price=12000,
        currency=currency,
        date_forward=tomorrow(),
        date_backward=date_future(),
        passengers='1_0_0',
        direct_flight=False
    )

    return [{
        'fromCity': {
            "code": None,
            "country": None,
            "geoId": None,
            "hasAirport": False,
            "iataCode": None,
            "id": int(from_city.id),
            "key": "c{}".format(from_city.id),
            "phraseFrom": "НазваниеГорода",
            "phraseTo": "НазваниеГорода",
            "title": "НазваниеГорода",
            "urlTitle": "NazvanieGoroda"
        },
        'toCity': {
            "code": None,
            "country": None,
            "geoId": None,
            "hasAirport": has_airport,
            "iataCode": None,
            "id": int(to_city.id),
            "key": "c{}".format(to_city.id),
            "phraseFrom": "НазваниеГорода",
            "phraseTo": "НазваниеГорода",
            "title": "НазваниеГорода",
            "urlTitle": "NazvanieGoroda"
        },
        "image": None,
        'price': {
            'value': p2.price,
            'baseValue': p2.price,
            'currency': 'RUR',
            'roughly': True
        },
        'dateForward': unicode(p.date_forward.isoformat()),
        'dateBackward': unicode(p.date_backward.isoformat()),
    }]


class TestRecipeOffersHandler(TestApiHandler):
    def kick_trash_fields(self, items):
        for item in items['data'][0]:
            del item['directPrice']
            del item['transfersPrice']
        return items

    def setUp(self):
        super(TestRecipeOffersHandler, self).setUp()

        self.currency = Currency.objects.create(
            name='Рубли', code='RUR', iso_code='RUB',
            template='t', template_whole='t', template_cents='t',
            template_tr='t', template_whole_tr='t', template_cents_tr='t'
        )
        self.avia_currency = AviaCurrency.objects.create(title='Рубли', code='RUR', iso_code='RUB', enable=True)
        self.currency_lang = CurrencyLang.objects.create(title='Русский', code='RU', enable=True)
        CurrencyTranslation.objects.create(
            currency=self.avia_currency, lang=self.currency_lang, title='Рубли', title_in='в&nbsp;рублях',
            template='%d&nbsp;р. %d&nbsp;к.', template_whole='%d&nbsp;р. %d&nbsp;к.', template_cents='%d&nbsp;р. %d&nbsp;к.'
        )

        # TODO: Это хак, нужен механизм по откату репозитория к состоянию до теста
        self.currency_translation_repository_all_currencies = currency_translation_repository._all_currencies
        self.currency_translation_repository_index = currency_translation_repository._index
        self.currency_repository_by_id = currency_repository._by_id
        self.currency_repository_by_code = currency_repository._by_code
        self.currency_repository_by_iso_code = currency_repository._by_iso_code

        currency_translation_repository.fetch()
        currency_repository.fetch()

    def tearDown(self):
        # TODO: Это хак, нужен механизм по откату репозитория к состоянию до теста
        currency_translation_repository._all_currencies = self.currency_translation_repository_all_currencies
        currency_translation_repository._index = self.currency_translation_repository_index
        currency_repository._by_id = self.currency_repository_by_id
        currency_repository._by_code = self.currency_repository_by_code
        currency_repository._by_iso_code = self.currency_repository_by_iso_code

    def test_popular(self):
        expected = prepare_simple_test(self.currency)

        payload = {
            'name': 'recipeOffers',
            'params': {
                'recipeId': 0,
                'fromId': expected[0]['fromCity']['id'],
            }
        }

        data = self.kick_trash_fields(self.api_data(payload))

        assert data == self.wrap_expect(expected)

    def test_popular_passengers(self):
        expected = prepare_simple_test(self.currency)

        payload = {
            'name': 'recipeOffers',
            'params': {
                'recipeId': 0,
                'fromId': expected[0]['fromCity']['id'],
                'adultSeats': 2,
                'childrenSeats': 1,
            }
        }

        data = self.kick_trash_fields(self.api_data(payload))

        def multiply(value):
            return int(value * 2.6)

        price = expected[0]['price']
        price['value'] = multiply(price['value'])
        price['baseValue'] = multiply(price['baseValue'])

        assert data == self.wrap_expect(expected)

    def test_image(self):
        expected = prepare_simple_test(self.currency)

        settlement_image = create_settlement_image(settlement_id=expected[0]['toCity']['id'])

        payload = {
            'name': 'recipeOffers',
            'params': {
                'recipeId': 0,
                'fromId': expected[0]['fromCity']['id'],
            },
            'fields': [],
        }

        expected[0]['image'] = {
            'id': settlement_image.id,
            'url2': settlement_image.url2
        }

        data = self.kick_trash_fields(self.api_data(payload))

        assert data == self.wrap_expect(expected)

    def test_recipe(self):
        expected = prepare_simple_test(self.currency)

        r = AviaRecipe.objects.create(
            id=1, title_ru='test', enabled_ru=True, recipe_type='prices'
        )
        r.settlements.add(Settlement.objects.get(id=expected[0]['toCity']['id']))

        payload = {
            'name': 'recipeOffers',
            'params': {
                'recipeId': 1,
                'fromId': expected[0]['fromCity']['id'],
            }
        }

        data = self.kick_trash_fields(self.api_data(payload))

        assert data == self.wrap_expect(expected)


class TestRecipeOfferHandler(TestApiHandler):
    def kick_trash_fields(self, items):
        for item in items['data']:
            del item['directPrice']
            del item['transfersPrice']
        return items

    def setUp(self):
        super(TestRecipeOfferHandler, self).setUp()

        self.currency = Currency.objects.create(
            name='Рубли', code='RUR', iso_code='RUB',
            template='t', template_whole='t', template_cents='t',
            template_tr='t', template_whole_tr='t', template_cents_tr='t'
        )

    def test_simple(self):
        expected = prepare_simple_test(self.currency)

        payload = {
            'name': 'recipeOffer',
            'params': {
                'fromId': expected[0]['fromCity']['id'],
                'toId': expected[0]['toCity']['id'],
            }
        }

        data = self.kick_trash_fields(self.api_data(payload))

        assert data == self.wrap_expect(expected[0])

    def test_has_not_airport(self):
        expected = prepare_simple_test(self.currency, has_airport=False)

        payload = {
            'name': 'recipeOffer',
            'params': {
                'fromId': expected[0]['fromCity']['id'],
                'toId': expected[0]['toCity']['id']
            }
        }

        data = self.api_data(payload)

        assert data == self.wrap_expect(None)
