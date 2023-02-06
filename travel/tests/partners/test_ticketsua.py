# -*- coding: utf-8 -*-
import datetime

import mock
import urllib
import pytest

from travel.avia.ticket_daemon.ticket_daemon.lib.currency import Price
from travel.avia.ticket_daemon.tests.partners.helper import (
    get_mocked_response, create_flight, get_query, assert_variants_equal,
    ComparableVariant,
)
from travel.avia.ticket_daemon.ticket_daemon.partners import ticketsua2


@pytest.mark.parametrize('national_version, currency, refid, values', [
    ('ru', 'RUR', 171, [6216.0, 7000.0]),
    ('tr', 'TRY', 342, [382.19, 430.39]),
])
@mock.patch('requests.get', return_value=get_mocked_response('ticketsua2.xml'))
def test_ticketsua2_query(mocked_request, national_version, currency, refid, values):
    redirect_url = 'https://avia.tickets.ru/preloader?StartAirp1Code=MOW&amp;EndAirp1Code=SVX&amp;Date1=21-01-2017&amp;StartAirp2Code=SVX&amp;EndAirp2Code=MOW&amp;Date2=24-01-2017&amp;adt=1&amp;chd=0&amp;inf=0&amp;hlp=0&amp;class=E&amp;st=10.01.2017_12:49'  # noqa
    expected_variants = [
        ComparableVariant(
            forward=[create_flight(
                **{
                    'station_from_iata': 'DME',
                    'local_departure': datetime.datetime(2017, 1, 21, 14, 50),
                    'company_iata': 'S7',
                    'number': 'S7 55',
                    'local_arrival': datetime.datetime(2017, 1, 21, 19, 10),
                    'station_to_iata': 'SVX',
                    'fare_code': 'WBSOW',
                }
            )],
            backward=[create_flight(
                **{
                    'station_from_iata': 'SVX',
                    'local_departure': datetime.datetime(2017, 1, 24, 6, 10),
                    'company_iata': 'S7',
                    'number': 'S7 52',
                    'local_arrival': datetime.datetime(2017, 1, 24, 6, 40),
                    'station_to_iata': 'DME',
                    'fare_code': 'WBSOW',
                }
            )],
            klass='economy',
            order_data=get_order_data(
                redirect_url=redirect_url,
                currency=currency,
                refid=refid,
                r_hash='b0e35b70fc4edd947a07c5910f2e0843_S755|:S752|:*407371776_47|S7|eJyVkV1PwjAUhv%2FK0uvatPte70ZATVRCQIxCyFJGmU32lW4zIYT%2F7ikjguiFXvK%2B55w%2Bz9gjLdOqKGS5Ea2qSsSXyz1qurrOldSIo1mAMNrmKntvk7Ir1sfQ8yDcyFrottMSguHTCBKhtfoQudl6ee0nklYVZsBmhDJiUxZYzOUe7ad%2FayPOjq3SqRbbFtpY6XXXWLHDIijSXDQNpIBdVsUOkrXIMpGZO3RsXu30SYW59LDCf%2FGxr3x6%2FrNP73fp434RU%2F9EfOHzrXWvfAaVVGVmBU5wE1Jq3WppaADkf3IeyK0wAkBl%2FrsyS641a61Ss%2BnbzCfUUKSd1rJMdxBO59Pj%2BUYmoqi6Esj2x5D7lNIDfCShf1ROX53TeXyPuB1EEXF9jOazIYBRhwQhRiOzEIXEjfDpLmBgNHkcI%2B7aEfFhJl7ADxYGxPEwelg8wwsODWHfxuhuMEE89AiD6nn6BlVoExbB882uaWWRqA0cCg6fJqvLkQ%3D%3D',  # noqa
            ),
            tariff=Price(currency=currency, value=values[0])
        ),
        ComparableVariant(
            forward=[create_flight(
                **{
                    'station_from_iata': 'SVO',
                    'local_departure': datetime.datetime(2017, 1, 21, 23, 5),
                    'company_iata': 'SU',
                    'number': 'SU 1416',
                    'local_arrival': datetime.datetime(2017, 1, 22, 3, 25),
                    'station_to_iata': 'SVX',
                    'fare_code': 'NVUR',
                }
                )],
            backward=[create_flight(
                **{
                    'station_from_iata': 'SVX',
                    'local_departure': datetime.datetime(2017, 1, 24, 7, 5),
                    'company_iata': 'SU',
                    'number': 'SU 1417',
                    'local_arrival': datetime.datetime(2017, 1, 24, 7, 35),
                    'station_to_iata': 'SVO',
                    'fare_code': 'NVUR',
                }
                )],
            klass='economy',
            order_data=get_order_data(
                redirect_url=redirect_url,
                currency=currency,
                refid=refid,
                r_hash='4b213d44d72af60e11ee2d8d7df3929c_SU1416|:SU1417|:*458752000_90|SU|eJytUdtqwkAU%2FBXZ52XZS2IubyktLbSoqClWkbDGNV3IjU1SEPHfezYWpfGlD308M3PmzOyekFFpVRSq3MtWVyUKN5sTarq6zrUyKESLGGF0yHX22SZlV%2Bx6kDlsDPBe1dK0nVFW9z4FRBqjv2Tez6uLIml1YQWcEcoIp8wbcRFS96K%2BsvzKUhHyntUmNfLQAhtps%2BuaUSQ4AyLNZdMACsHLqjgCspNZJjPrwyb2amd%2ByjCHnrf4b428u0arQaPpoJFzy%2BzdN%2FrFin9r5EKjLUaQSdsvK7Nk2K02OrWbHqWUUGr9O2NUmR4BnMfz3r5RiSyqroQwpx4MrfwMLyPNHeVcqBsaRy8oFMzlxHUwihePEIyNie9h9GQXGKMkYPhmjNHsbQJGvuhF0RoGbkU%2BRq%2FrJZh53B0TB9yeH2YoDFziBxgt5x%2BwJCgRAdxvjk2rikTvgafnb29NyMo%3D',  # noqa
            ),
            tariff=Price(currency=currency, value=values[1])
        ),
    ]

    test_query = get_query(national_version=national_version)
    variants = list(ticketsua2.query(test_query))
    assert_variants_equal(expected_variants, variants[0])


def get_order_data(redirect_url, currency, refid, r_hash):
    return {'url': '{}&{}'.format(redirect_url, urllib.urlencode({
        'currency': currency,
        'refid': refid,
        'r_hash': r_hash,
        'class': 'E',
        'act': 'book',
    }))}
