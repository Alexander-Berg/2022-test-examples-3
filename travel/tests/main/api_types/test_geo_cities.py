# -*- coding: utf-8 -*-

from __future__ import unicode_literals, absolute_import
from datetime import date, timedelta

from travel.avia.library.python.avia_data.models import AviaDirectionNational, AviaSettlementNational, MinPrice
from travel.avia.library.python.common.models.currency import Currency
from travel.avia.library.python.tester.factories import create_settlement

from travel.avia.backend.tests.main.api_test import TestApiHandler


def tomorrow():
    return date.today() + timedelta(1)


class TestGeoCitiesHandler(TestApiHandler):
    def setUp(self):
        super(TestGeoCitiesHandler, self).setUp()

        self.from_city = create_settlement()

        self.currency = Currency.objects.create(
            name='Рубли', code='RUR', iso_code='RUB',
            template='t', template_whole='t', template_cents='t',
            template_tr='t', template_whole_tr='t', template_cents_tr='t'
        )

    def _create_direction(self, title='Settlement'):
        return AviaDirectionNational.objects.create(
            departure_settlement=self.from_city,
            arrival_settlement=create_settlement(title=title),
        )

    def test_letters(self):
        self._create_direction(title='Раз')
        self._create_direction(title='Два')
        self._create_direction(title='Ростов')

        payload = {
            'name': 'geoCities',
            'params': {
                'fromKey': self.from_city.point_key,
            },
            'fields': ['letters']
        }

        data = self.api_data(payload)

        assert data == self.wrap_expect({
            'letters': [{
                'name': u'д',
                'count': 1,
            }, {
                'name': u'р',
                'count': 2,
            }]
        })

    def _create_avia_settlement(self, title='Settlement', popularity=0):
        return AviaSettlementNational.objects.create(
            settlement=create_settlement(title=title),
            arrival=False,
            popularity=popularity
        )

    def _create_min_price(self, to_city, price=10000):
        return MinPrice.objects.create(
            departure_settlement=self.from_city,
            arrival_settlement=to_city,
            price=price,
            currency=self.currency,
            date_forward=tomorrow() + timedelta(days=5),
            date_backward=tomorrow() + timedelta(days=12),
            passengers='1_0_0',
            direct_flight=True
        )

    def test_popular(self):
        s1 = self._create_avia_settlement('Душанбе', popularity=10)
        s2 = self._create_avia_settlement('Дубай', popularity=100)
        s3 = self._create_avia_settlement('Владивосток', popularity=1)

        payload = {
            'name': 'geoCities',
            'fields': ['letters', {
                'name': 'offers',
                'fields': [{
                    'name': 'toCity',
                    'fields': ['id'],
                }]
            }]
        }

        data = self.api_data(payload)

        assert data == self.wrap_expect({
            'letters': [{
                'name': u'в',
                'count': 1,
            }, {
                'name': u'д',
                'count': 2,
            }],
            'offers': [{
                'toCity': {
                    'id': int(s2.settlement.id)
                }
            }, {
                'toCity': {
                    'id': int(s1.settlement.id)
                }
            }, {
                'toCity': {
                    'id': int(s3.settlement.id)
                }
            }]
        })

    def test_cheapest(self):
        s1 = self._create_direction()
        s2 = self._create_direction()

        self._create_min_price(s1.arrival_settlement, price=20000)
        self._create_min_price(s2.arrival_settlement, price=10000)

        payload = {
            'name': 'geoCities',
            'params': {
                'fromKey': self.from_city.point_key,
            },
            'fields': [{
                'name': 'offers',
                'fields': [{
                    'name': 'toCity',
                    'fields': ['id'],
                }, 'price']
            }]
        }

        data = self.api_data(payload)

        assert data == self.wrap_expect({
            'offers': [{
                'toCity': {
                    'id': int(s2.arrival_settlement.id)
                },
                'price': {
                    'baseValue': 10000,
                    'currency': 'RUR',
                    'roughly': True,
                    'value': 10000
                }
            }, {
                'toCity': {
                    'id': int(s1.arrival_settlement.id)
                },
                'price': {
                    'baseValue': 20000,
                    'currency': 'RUR',
                    'roughly': True,
                    'value': 20000
                }
            }]
        })

    def test_filtered_offers(self):
        s1 = self._create_direction(title='Душанбе')
        s2 = self._create_direction(title='Дубай')
        s3 = self._create_direction(title='Владивосток')

        self._create_min_price(s1.arrival_settlement, price=20000)
        self._create_min_price(s2.arrival_settlement, price=10000)
        self._create_min_price(s3.arrival_settlement, price=10000)

        payload = {
            'name': 'geoCities',
            'params': {
                'fromKey': self.from_city.point_key,
                'offersFilters': {
                    'letters': ['д']
                }
            },
            'fields': [{
                'name': 'offers',
                'fields': [{
                    'name': 'toCity',
                    'fields': ['id'],
                }, 'price']
            }]
        }

        data = self.api_data(payload)

        assert data == self.wrap_expect({
            'offers': [{
                'toCity': {
                    'id': int(s2.arrival_settlement.id)
                },
                'price': {
                    'baseValue': 10000,
                    'currency': 'RUR',
                    'roughly': True,
                    'value': 10000
                }
            }, {
                'toCity': {
                    'id': int(s1.arrival_settlement.id)
                },
                'price': {
                    'baseValue': 20000,
                    'currency': 'RUR',
                    'roughly': True,
                    'value': 20000
                }
            }]
        })
