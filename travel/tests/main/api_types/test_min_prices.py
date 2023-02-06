# -*- coding: utf-8 -*-

from __future__ import unicode_literals, absolute_import

from datetime import date, timedelta

from travel.avia.library.python.avia_data.models import MinPrice
from travel.avia.library.python.common.models.currency import Currency
from travel.avia.library.python.tester.factories import create_settlement, create_avia_currency

from travel.avia.backend.tests.main.api_test import TestApiHandler
from travel.avia.backend.repository.currency import currency_repository


def tomorrow():
    return date.today() + timedelta(1)


class TestMinPricesHandler(TestApiHandler):
    def setUp(self):
        super(TestMinPricesHandler, self).setUp()

        self.from_city = create_settlement()
        self.to_city = create_settlement()

        self.currency = Currency.objects.create(
            name='Рубли', code='RUR', iso_code='RUB',
            template='t', template_whole='t', template_cents='t',
            template_tr='t', template_whole_tr='t', template_cents_tr='t'
        )
        create_avia_currency()

        currency_repository.fetch()

    def test_oneway(self):
        p = MinPrice.objects.create(
            departure_settlement=self.from_city,
            arrival_settlement=self.to_city,
            price=12500,
            currency=self.currency,
            date_forward=tomorrow() + timedelta(days=5),
            date_backward=None,
            passengers='1_0_0',
            direct_flight=True
        )

        left_date = tomorrow()
        right_date = left_date + timedelta(days=7)

        payload = {
            'name': 'minPrices',
            'params': {
                'fromPointKey': self.from_city.point_key,
                'toPointKey': self.to_city.point_key,
                'leftDate': left_date.isoformat(),
                'rightDate': right_date.isoformat(),
                'adultSeats': 1,
                'childrenSeats': 0,
                'infantSeats': 0,
            }
        }

        data = self.api_data(payload)

        expected = []

        d = left_date
        while d <= right_date:
            prices = None
            if d == p.date_forward:
                prices = {
                    'direct': {
                        'value': p.price,
                        'baseValue': p.price,
                        'currency': 'RUR',
                        'roughly': True,
                    }
                }

            expected.append({'date': unicode(d.isoformat()), 'prices': prices})

            d += timedelta(days=1)

        assert data == self.wrap_expect(expected)
