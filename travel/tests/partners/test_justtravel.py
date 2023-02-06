# -*- coding: utf-8 -*-
import datetime
import mock

from travel.avia.ticket_daemon.ticket_daemon.lib.currency import Price
from travel.avia.ticket_daemon.tests.partners.helper import (
    get_mocked_response, create_flight, get_query, assert_variants_equal,
    ComparableVariant
)
from travel.avia.ticket_daemon.ticket_daemon.partners import justtravel


def test_justtravel_query():
    expected_variants = [
        ComparableVariant(
            forward=[create_flight(**{
                'station_from_iata': u'VKO',
                'klass': 'economy',
                'local_departure': datetime.datetime(2017, 1, 21, 1, 10),
                'company_iata': u'DP',
                'number': u'DP 405',
                'local_arrival': datetime.datetime(2017, 1, 21, 5, 30),
                'station_to_iata': u'SVX',
                'baggage': '1pc 10kg',
            })],
            backward=[create_flight(**{
                'station_from_iata': u'SVX',
                'klass': 'economy',
                'local_departure': datetime.datetime(2017, 1, 24, 22, 30),
                'company_iata': u'DP',
                'number': u'DP 408',
                'local_arrival': datetime.datetime(2017, 1, 24, 23, 10),
                'station_to_iata': u'VKO',
                'baggage': '1pc 10kg',
            })],
            klass='economy',
            order_data={
                'variant': u'.H4sIAAAAAAAAADXO3RJCQBgA0Ccyw-6HdRnWb362mpUrIyE0KDJ4-qaLzhMcByb38Gcnkcg6k7EHTdRedYgfKOtoNl19fRXHZtqqQj-5VsliRaQ55Xnue5ynRmunpR2q1RjuaYN0aqmsv3irOcJAy44Yzw9u3z0ukiW_b7wiUbjbgu_N8zkW4ohrt2nS5A6TOtirAeql_mUyhEDMJCAgASIiyIC_KEc9660AAAA',  # noqa
                'principal': 2240,
            },
            tariff=Price(currency='RUR', value=4458.0)
        ),
        ComparableVariant(
            forward=[create_flight(**{
                'station_from_iata': u'VKO',
                'klass': 'economy',
                'local_departure': datetime.datetime(2017, 1, 21, 20, 10),
                'company_iata': u'DP',
                'number': u'DP 1403',
                'local_arrival': datetime.datetime(2017, 1, 22, 0, 30),
                'station_to_iata': u'SVX',
                'baggage': '1pc 10kg',
            })],
            backward=[create_flight(**{
                'station_from_iata': u'SVX',
                'klass': 'economy',
                'local_departure': datetime.datetime(2017, 1, 24, 7, 30),
                'company_iata': u'DP',
                'number': u'DP 406',
                'local_arrival': datetime.datetime(2017, 1, 24, 8, 5),
                'station_to_iata': u'VKO',
                'baggage': '1pc 10kg',
            })],
            klass='economy',
            order_data={
                'variant': u'.H4sIAAAAAAAAADXO3Q5DMBgA0CeStHzFramMsJV1mN3Ilg3z02FB2qdfdrHzBMeHT-D87XO2JQZNnYZmSCzZkFKN-5MXYdR01s72hmBoIPTmoNc2oQ63TWZcjs2UbiLo4nGp4vBZQ_pyvaN14tfprLo2DCVzyOCP2JjFXeWitoqoJTNaOHVJxTLzTasSyAWvpip6IRNmrerBZPkrlboOqMRgAwbdRkAAvrRURHyzAAAA',  # noqa
                'principal': 2240,
            },
            tariff=Price(currency='RUR', value=4958.0)
        ),
    ]

    def mocked_responses():
        def wrapper(*args, **kwargs):
            url = args[0]
            if url == 'http://service.just.travel/json/authentication/login':
                return get_mocked_response('justtravel_auth.json')
            elif url == 'http://service.just.travel/json/avia/getTripList':
                return get_mocked_response('justtravel.json')
            else:
                raise AttributeError("Invalid url request parameter")
        return wrapper

    test_query = get_query()
    with mock.patch('requests.Session.post', side_effect=mocked_responses()):
        variants = next(justtravel.query(test_query))
    assert_variants_equal(expected_variants, variants)
