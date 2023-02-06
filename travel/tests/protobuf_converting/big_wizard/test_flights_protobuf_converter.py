# -*- coding: utf-8 -*-
import unittest
from copy import deepcopy
from itertools import imap

from travel.avia.library.python.ticket_daemon.protobuf_converting.big_wizard.search_result_converter import SearchResultConverter
from travel.proto.avia.wizard.search_result_pb2 import SearchResult


def make_fields_None(datum, fields):
    for field in fields:
        datum[field] = None
    return datum


def make_flights_fields_None(flights, fields):
    for flight in flights:
        make_fields_None(flights[flight], fields)
    return flights


class TestsFlightsConverter(unittest.TestCase):
    def setUp(self):
        self.search_result_converter = SearchResultConverter()
        self.qid = '180621-123405-726.api_avia.plane.c213_c23_2018-09-01_None_economy_1_0_0_ru.ru'
        self.version = 3

        self.flights = {
            '1809011515SU1324': {
                'arrival': {
                    'local': '123',
                    'tzname': 'EUROPE/MOSCOW',
                    'offset': 180
                },
                'to': 123,
                'companyTariff': 456,
                'from': 123,
                'key': 'abc',
                'company': 26,
                'aviaCompany': 26,
                'number': u'SU 1324',
                'departure': {
                    'local': '123',
                    'tzname': 'EUROPE/MOSCOW',
                    'offset': 180
                }
            }
        }

        self.fares = [
            {
                'charter': False,
                'created': 1529573651,
                'route': (('1809011515SU1324', ), ()),
                'baggage': [
                    [
                        '1d1p23d'
                    ],
                    [

                    ]
                ],
                'expire': 1529575151,
                'partner': u'test_partner',
                'conversion_partner': u'test_conversion_partner',
                'tariff': {
                    'currency': 'RUR',
                    'value': 14238.0
                },
                'popularity': 100,
                'tariffs': {},
                'promo': {
                    'code': 'some_code',
                    'end_ts': 1529575151,
                },
            }
        ]

        self.polling_status = {
            'asked_partners': ['test_partner_1', 'test_partner_2'],
            'asked_partners_count': 2,
            'remaining_partners': ['test_partner_3'],
            'remaining_partners_count': 1,
        }

        self.offers_count = len(self.fares)
        self.example_search_result = {
            'qid': self.qid,
            'version': self.version,
            'flights': self.flights,
            'fares': self.fares,
            'offers_count': self.offers_count,
            'polling_status': self.polling_status,
        }

        self.datum_with_absent_fields = {
            'qid': self.qid,
            'version': self.version,
            'flights': make_flights_fields_None(deepcopy(self.flights), ('company', 'aviaCompany', 'companyTariff')),
            'fares': self.fares,
            'offers_count': self.offers_count,
            'polling_status': self.polling_status,
        }

    def assertSearchResultsAreEqual(self, expected, actual):
        self.assertEqual(expected['qid'], actual['qid'])
        self.assertEqual(expected['version'], actual['version'])

        self.assertDictEqual(expected['flights'], actual['flights'])

        self.assertIsInstance(actual['fares'], imap)
        self.assertListEqual(expected['fares'], list(actual['fares']))

        self.assertDictEqual(actual['polling_status'], expected['polling_status'])

    def _get_pb_after_deserialization(self, datum):
        protobuffed_search_result = self.search_result_converter.to_protobuf(datum)
        serialized_string = protobuffed_search_result.SerializeToString()

        search_result = SearchResult()
        search_result.ParseFromString(serialized_string)
        return search_result

    def check_convertation(self, datum):
        search_result = self._get_pb_after_deserialization(datum)
        self.assertSearchResultsAreEqual(
            datum, self.search_result_converter.to_dictionary(search_result)
        )

    def test_deserialized_content_should_be_equal_to_original_one(self):
        self.check_convertation(self.example_search_result)

    def test_datum_with_some_absent_fields_should_be_successfully_converted(self):
        self.check_convertation(self.datum_with_absent_fields)

    def test_datum_without_polling_status(self):
        datum_without_polling_status = {
            i: self.example_search_result[i] for i in self.example_search_result if i != 'polling_status'
        }

        converting_result = self.search_result_converter.to_dictionary(
            self._get_pb_after_deserialization(datum_without_polling_status)
        )

        self.assertEqual(converting_result['polling_status']['asked_partners_count'], 0)
        self.assertEqual(converting_result['polling_status']['remaining_partners_count'], 0)
        self.assertListEqual(converting_result['polling_status']['asked_partners'], [])
        self.assertListEqual(converting_result['polling_status']['remaining_partners'], [])

    def test_serialization_with_flights_without_time(self):
        search_result = deepcopy(self.example_search_result)
        search_result['flights'] = make_flights_fields_None(
            deepcopy(self.flights), ('arrival', 'departure')
        )

        search_result_pb = self._get_pb_after_deserialization(search_result)
        for key, flight in search_result_pb.flights.iteritems():
            assert not flight.arrival.ByteSize()
            assert not flight.departure.ByteSize()

        deserialized_search_result = self.search_result_converter.to_dictionary(search_result_pb)
        for key, flight in deserialized_search_result['flights'].iteritems():
            assert flight['arrival'] is None
            assert flight['departure'] is None

    def test_filling_tariffs(self):
        search_result = self.example_search_result

        fare = search_result['fares'][0]
        fare['tariffs'] = {
            'with_baggage': {
                'created_at': fare['created'],
                'baggage': fare['baggage'],
                'expire_at': fare['expire'],
                'partner': fare['partner'],
                'conversion_partner': fare['partner'],
                'price': fare['tariff'],
            }
        }
        self.check_convertation(search_result)

    def test_filling_two_tariffs(self):
        search_result = self.example_search_result

        fare = search_result['fares'][0]
        fare['tariffs'] = {
            'with_baggage': {
                'created_at': fare['created'],
                'baggage': fare['baggage'],
                'expire_at': fare['expire'],
                'partner': fare['partner'],
                'conversion_partner': fare['partner'],
                'price': fare['tariff'],
            },
            'without_baggage': {
                'created_at': fare['created'],
                'baggage': [['0d0d0d'], []],
                'expire_at': fare['expire'],
                'partner': fare['partner'],
                'conversion_partner': fare['partner'],
                'price': fare['tariff'],
            },
        }
        self.check_convertation(search_result)
