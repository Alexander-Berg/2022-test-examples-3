# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from django.conf import settings

from travel.rasp.library.python.common23.models.currency.currency_converter import fetch_currency_rates


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


def test_fetch_currency_rates(httpretty):
    httpretty.register_uri(httpretty.GET, settings.CURRENCY_RATES_URL, body=RESPONSE_XML.encode('utf8'))
    src, rates = fetch_currency_rates(['RUR', 'TRY', 'UAH', 'USD', 'ZAR'])
    assert rates == {'TRY': 15.8, 'UAH': 2.1, 'USD': 58.6, 'ZAR': 4.4}
