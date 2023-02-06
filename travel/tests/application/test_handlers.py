# coding=utf-8
import copy
import json
import pkgutil
import unittest

from tornado.web import HTTPError

from travel.avia.api_gateway.application.handlers import MailFlightByDepartureDateHandler as Handler


class MailFlightByDepartureDateHandlerTestCase(unittest.TestCase):
    def setUp(self):
        package = 'tests'
        resource = 'application/fixtures/flight_storage_flight_example.json'
        self.sample = json.loads(pkgutil.get_data(package, resource))

    def test_get_segment_by_departure_date(self):
        data = Handler._get_segment_by_departure_date(self.sample, '2020-01-15T02:55:00')

        self.assertDictEqual(self.sample[1]['segments'][1], data)

    def test_flight_with_wrong_time(self):
        with self.assertRaises(HTTPError) as cm:
            Handler._get_segment_by_departure_date(self.sample, '2019-10-16T20:00:59')

        error = cm.exception
        self.assertEqual(404, error.status_code)

    def test_regular_flight(self):
        regular_flights = copy.deepcopy(self.sample)

        for flight in regular_flights:
            flight['segments'] = []

        data = Handler._get_segment_by_departure_date(regular_flights, '2020-01-14T20:30:00')

        self.assertDictEqual(regular_flights[1], data)

    def test_regular_flight_with_wrong_time(self):
        regular_flights = copy.deepcopy(self.sample)

        for flight in regular_flights:
            flight['segments'] = []

        with self.assertRaises(HTTPError) as cm:
            Handler._get_segment_by_departure_date(regular_flights, '2019-10-17T20:00:00')

        error = cm.exception
        self.assertEqual(404, error.status_code)

    def test_parse_quoted_departure_date(self):
        actual = Handler._parse_departure_date('2020-01-15T10%3A35%3A00Z')

        self.assertEqual('2020-01-15T10:35:00', actual)

    def test_parse_not_justifed_departure_date(self):
        actual = Handler._parse_departure_date('2020-1-5T10:5:0Z')

        self.assertEqual('2020-01-05T10:05:00', actual)

    def test_parse_Z_stripped_departure_date(self):
        actual = Handler._parse_departure_date('2020-01-15T10:05:00')

        self.assertEqual('2020-01-15T10:05:00', actual)

    def test_parse_bad_format(self):
        with self.assertRaises(HTTPError) as cm:
            Handler._parse_departure_date('bad-format')

        error = cm.exception
        self.assertEqual(400, error.status_code)
