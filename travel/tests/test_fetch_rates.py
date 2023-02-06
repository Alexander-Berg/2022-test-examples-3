# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest
from django.conf import settings

from travel.rasp.library.python.common23.models.currency.currency import Currency
from travel.rasp.library.python.common23.tester.factories import create_currency

RESPONSE_XML = u'''<?xml version="1.0" encoding="utf-8"?>
<response>
<status>ok</status>
<data>
<date>03.03.2017</date>
<src>Московской биржи</src>
<from>RUR</from>
<rates>
<to id="TRY" value="15.8"/>
<to id="UAH" value="2.1"/>
<to id="USD" value="58.6"/>
<to id="ZAR" value="4.4"/>
</rates>
</data>
</response>'''


@pytest.mark.dbuser
def test_fetch_currency_rates(httpretty):
    create_currency(code='RUR')
    create_currency(code='UAH')
    create_currency(code='TRY')
    create_currency(code='USD')
    create_currency(code='ZAR')

    httpretty.register_uri(httpretty.GET, settings.CURRENCY_RATES_URL, body=RESPONSE_XML.encode('utf8'))
    src, rates = Currency.fetch_rates(Currency.objects.all(), settings.MOSCOW_GEO_ID, 'RUR')
    assert rates == {'RUR': 1, 'TRY': 15.8, 'UAH': 2.1, 'USD': 58.6, 'ZAR': 4.4}
