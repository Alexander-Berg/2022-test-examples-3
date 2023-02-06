# -*- coding: utf-8 -*-
from __future__ import unicode_literals

import json

import hamcrest as h
import mock
import pytest
from django.conf import settings
from django.test import Client

from common.models.currency import Currency
from common.tester.factories import create_currency
from common.tester.utils.replace_setting import replace_setting


@pytest.mark.dbuser
@replace_setting('CURRENCY_RATES_URL', None)  # отключаем конвертер
def test_currencies():
    create_currency(code='RUR', iso_code='RUB', name='рубли', name_uk='рублi', order=1, order_ua=2)
    create_currency(code='USD', iso_code='USD', name='доллары', name_uk='долари', order=2, order_ua=1)
    create_currency(code='EUR', iso_code='EUR', name='евро', name_uk='євро', order=3, order_ua=3)

    # ожидаемый формат ответа и параметры fetch_rates по-умолчанию
    with mock.patch.object(Currency, 'fetch_rates',
                           return_value=(None, {'RUB': 1, 'USD': 20, 'EUR': 30})) as m_fetch_rates:
        response = Client().get('/uk/currencies/')

        m_fetch_rates.assert_called_once_with(mock.ANY, settings.MOSCOW_GEO_ID, 'RUB')

    assert response.status_code == 200
    h.assert_that(
        json.loads(response.content),
        h.has_entries('currencies', h.contains(
            h.has_entries({'code': 'RUB', 'title': 'рублi', 'rate': 1}),
            h.has_entries({'code': 'USD', 'title': 'долари', 'rate': 20}),
            h.has_entries({'code': 'EUR', 'title': 'євро', 'rate': 30}),
        ))
    )

    # значения курсов при выключенном конвертере
    response = Client().get('/uk/currencies/')

    assert response.status_code == 200
    h.assert_that(
        json.loads(response.content),
        h.has_entries('currencies', h.contains(
            h.has_entries('rate', 1), h.has_entries('rate', None), h.has_entries('rate', None)
        ))
    )

    # разбор параметров
    with mock.patch.object(Currency, 'fetch_rates',
                           return_value=(None, {'RUB': 10, 'EUR': 30, 'USD': 1})) as m_fetch_rates:
        response = Client().get('/uk/currencies/?base=USD&national_version=ua')

        m_fetch_rates.assert_called_once_with(mock.ANY, settings.KIEV_GEO_ID, 'USD')

    assert response.status_code == 200
    h.assert_that(
        json.loads(response.content),
        h.has_entries('currencies', h.contains(
            h.has_entries({'code': 'USD', 'title': 'долари', 'rate': 1}),
            h.has_entries({'code': 'RUB', 'title': 'рублi', 'rate': 10}),
            h.has_entries({'code': 'EUR', 'title': 'євро', 'rate': 30}),
        ))
    )
