# -*- coding: utf-8 -*-
import sys
from copy import deepcopy
from datetime import datetime, timedelta
from itertools import chain
from operator import itemgetter
from unittest import TestCase

from travel.avia.library.python.ticket_daemon.protobuf_converting.big_wizard.filtering import search_result_filtering
from travel.avia.library.python.ticket_daemon.protobuf_converting.big_wizard.search_result_converter import SearchResultConverter

search_result_converter = SearchResultConverter()

filter_and_slice = search_result_filtering.filter_and_slice
reload(sys)
sys.setdefaultencoding('utf8')


PARTNER = 'partner'
CONVERSION_PARTNER = 'test_conversion_partner'


def fares_to_list(search_result_dict):
    search_result_dict['fares'] = list(search_result_dict['fares'])
    return search_result_dict


def to_dict(search_result):
    if not isinstance(search_result, dict):
        return search_result_converter.to_dictionary(search_result)
    return search_result


def create_flight(
    departure='2018-05-18T18:00:00',
    arrival='2018-05-18T19:00:00',
    station_from_id=9600213,
    station_to_id=9600366,
    company_id=2543,
    number='WW 666',
    tariff=184,
):
    departure_dt = datetime.strptime(departure, '%Y-%m-%dT%H:%M:%S')
    return {
        'departure': {
            'local': departure,
            'tzname': 'Europe/Moscow',
            'offset': 180
        },
        'arrival': {
            'local': arrival,
            'tzname': 'Europe/Moscow',
            'offset': 180
        },
        'from': station_from_id,
        'to': station_to_id,
        'company': company_id,
        'aviaCompany': company_id,
        'number': number,

        'companyTariff': tariff,
        'key': departure_dt.strftime('%y%m%d%H%M') + number.replace(' ', ''),
    }


def create_fare(
    forward_routes=(),
    backward_routes=(),
    forward_baggage=(),
    backward_baggage=(),
    partner=PARTNER,
    conversion_partner=CONVERSION_PARTNER,
    tariff=1000,
    popularity=100,
    tariffs=None,
):
    result = {
        'charter': False,
        'created': 1525416185,
        'expire': 1525417385,
        'route': [list(forward_routes), list(backward_routes)],
        'baggage': [list(forward_baggage), list(backward_baggage)],
        'partner': partner,
        'conversion_partner': conversion_partner,
        'tariff': {
            'currency': 'RUR',
            'value': tariff,
        },
        'popularity': popularity,
    }

    if tariffs is not None:
        result['tariffs'] = tariffs
    return result


def create_datum(fares, flights):
    datum = {
        'qid': '180504-094250-943.ticket.plane.c54_c2_2018-05-18_2018-05-20_economy_1_0_0_ru.ru',
        'flights': {flight['key']: flight for flight in flights},
        'version': 23,
        'fares': sorted(fares, key=lambda fare: fare['tariff']['value']),
        'offers_count': 140
    }

    return search_result_converter.to_protobuf(datum)


class TestWithBaggageFilter(TestCase):
    @staticmethod
    def one_way_datum():
        flights = (
            create_flight(number='WW 1'),
            create_flight(number='WW 2'),
        )
        fares = (
            create_fare(forward_routes=(flights[0]['key'],),
                        forward_baggage=('0d0d0d',), tariff=1234),
            create_fare(forward_routes=(flights[1]['key'],),
                        forward_baggage=('1d1d23d',), tariff=12345),

        )
        return create_datum(fares, flights)

    @staticmethod
    def return_datum():
        flights = (
            create_flight(number='WW 1'),
            create_flight(number='WW 2'),
            create_flight(number='WW 3', station_from_id=9600366, station_to_id=9600213),
        )
        fares = (
            create_fare(
                forward_routes=(flights[0]['key'],), forward_baggage=('0d0d0d',),
                backward_routes=(flights[2]['key'],), backward_baggage=('0d0d0d',),
                tariff=1234,
            ),
            create_fare(
                forward_routes=(flights[1]['key'],), forward_baggage=('1d1d23d',),
                backward_routes=(flights[2]['key'],), backward_baggage=('1d1d23d',),
                tariff=12345,
            ),

        )
        return create_datum(fares, flights)

    def test_with_baggage_true_filter_one_way(self):
        params = {
            'filters': {
                'with_baggage': True,
            }
        }

        datum = self.one_way_datum()
        filter_and_slice(datum, params)
        assert len(datum.fares) == 1
        assert datum.fares[0].tariff.value == 12345

    def test_with_baggage_false_filter_one_way(self):
        """
        Не нужно фильтровать если передан with_baggage false или null
        """
        params = {
            'filters': {
                'with_baggage': False,
            }
        }

        datum = self.one_way_datum()
        expected_datum = deepcopy(datum)
        filter_and_slice(datum, params)
        assert datum == expected_datum

    def test_with_baggage_true_filter_return(self):
        params = {
            'filters': {
                'with_baggage': True,
            }
        }

        datum = self.return_datum()
        filter_and_slice(datum, params)
        assert len(datum.fares) == 1
        assert datum.fares[0].tariff.value == 12345

    def test_with_baggage_false_filter_return(self):
        """
        Не нужно фильтровать если передан with_baggage false или null
        """
        params = {
            'filters': {
                'with_baggage': False,
            }
        }

        datum = self.return_datum()
        expected_datum = deepcopy(datum)
        filter_and_slice(datum, params)

        assert datum == expected_datum


class TestWithBaggageFilterManyTariffs(TestCase):
    @staticmethod
    def one_way_datum():
        flights = (
            create_flight(number='WW 1'),
            create_flight(number='WW 2'),
            create_flight(number='WW 3'),
        )

        without_baggage_fare_1 = create_fare(
            forward_routes=(flights[0]['key'],), forward_baggage=('0d0d0d',), tariff=1234
        )

        with_baggage_fare_2 = create_fare(
            forward_routes=(flights[1]['key'],), forward_baggage=('1d1d23d',), tariff=12345
        )

        without_baggage_fare_3 = create_fare(
            forward_routes=(flights[2]['key'],), forward_baggage=('0d0d0d',), tariff=123456
        )

        with_baggage_fare_3 = create_fare(
            forward_routes=(flights[2]['key'],), forward_baggage=('1d1d23d',), tariff=123457
        )

        TestWithBaggageFilterManyTariffs.fill_tariff_infoes(
            without_baggage_fare_1, without_baggage=without_baggage_fare_1
        )
        TestWithBaggageFilterManyTariffs.fill_tariff_infoes(
            with_baggage_fare_2, with_baggage=with_baggage_fare_2
        )
        fare3 = without_baggage_fare_3.copy()
        TestWithBaggageFilterManyTariffs.fill_tariff_infoes(
            fare3, without_baggage=without_baggage_fare_3,
            with_baggage=with_baggage_fare_3
        )
        fares = (
            without_baggage_fare_1, with_baggage_fare_2, fare3
        )
        return create_datum(fares, flights)

    @staticmethod
    def fill_tariff_infoes(base_fare, with_baggage=None, without_baggage=None):
        def fill(fare_tariff, tariff):
            fare_tariff.update({
                'baggage': tariff['baggage'],
                'partner': tariff['partner'],
                'conversion_partner': tariff['conversion_partner'],
                'created_at': tariff['created'],
                'expire_at': tariff['expire'],
                'price': tariff['tariff'],
            })

        if with_baggage:
            fill(base_fare.setdefault('tariffs', {}).setdefault('with_baggage', {}), with_baggage)

        if without_baggage:
            fill(base_fare.setdefault('tariffs', {}).setdefault('without_baggage', {}), without_baggage)

    def test(self):
        params = {
            'filters': {
                'with_baggage': True,
            }
        }

        datum = self.one_way_datum()
        filter_and_slice(datum, params)
        assert len(datum.fares) == 2
        assert datum.fares[0].tariff.value == 12345
        assert datum.fares[0].baggage.forward == ['1d1d23d']
        assert datum.fares[1].tariff.value == 123457
        assert datum.fares[1].baggage.forward == ['1d1d23d']
        for f in datum.fares:
            assert f.partner == PARTNER
            assert f.conversion_partner == CONVERSION_PARTNER


class TestSlicing(TestCase):
    def test_limit(self):
        params = {
            'limit': 1,
        }

        datum = TestWithBaggageFilter.one_way_datum()
        assert len(datum.fares) == 2

        filter_and_slice(datum, params)
        assert len(datum.fares) == 1
        assert datum.fares[0].tariff.value == 1234

    def test_offset(self):
        params = {
            'offset': 1,
        }
        datum = TestWithBaggageFilter.one_way_datum()
        assert len(datum.fares) == 2

        filter_and_slice(datum, params)
        assert len(datum.fares) == 1
        assert datum.fares[0].tariff.value == 12345

    def test_limit_with_offset(self):
        params = {
            'limit': 1,
            'offset': 1,
        }
        datum = TestWithBaggageFilter.one_way_datum()
        assert len(datum.fares) == 2

        filter_and_slice(datum, params)
        assert len(datum.fares) == 1
        assert datum.fares[0].tariff.value == 12345


class FiltersTestCase(TestCase):
    def assertFiltered(self, datum, params, filtered=True, msg='Invalid filtration result'):
        expected_datum = deepcopy(datum)
        actual_datum = deepcopy(datum)
        filter_and_slice(actual_datum, params)

        if filtered:
            assert len(actual_datum.fares) == 0, msg
            assert len(actual_datum.flights) == 0, msg
        else:
            assert expected_datum == actual_datum, msg


class TransferFilterTest(FiltersTestCase):
    def one_way_datum(self):
        from_id = 1
        to_id = 10
        no_stops_flight = (
            create_flight(
                number='WW 1', station_from_id=from_id, station_to_id=to_id
            ),
        )
        one_stops_flight = (
            create_flight(
                number='WW 2', station_from_id=from_id, station_to_id=2
            ),
            create_flight(
                number='WW 3', station_from_id=2, station_to_id=to_id
            ),
        )
        two_stops_flight = (
            create_flight(
                number='WW 4', station_from_id=from_id, station_to_id=2
            ),
            create_flight(
                number='WW 5', station_from_id=2, station_to_id=3
            ),
            create_flight(
                number='WW 5', station_from_id=3, station_to_id=to_id
            ),
        )
        flights = list(chain(
            no_stops_flight, one_stops_flight, two_stops_flight
        ))
        fares = (
            create_fare(forward_routes=map(itemgetter('key'), no_stops_flight), tariff=1234),
            create_fare(forward_routes=map(itemgetter('key'), one_stops_flight), tariff=12345),
            create_fare(forward_routes=map(itemgetter('key'), two_stops_flight), tariff=123456),
        )
        return create_datum(fares, flights)

    def return_datum(self):
        flights = (
            create_flight(number='WW 1'),
            create_flight(number='WW 2'),
            create_flight(number='WW 3', station_from_id=9600366,
                          station_to_id=9600213),
        )
        fares = (
            create_fare(
                forward_routes=(flights[0]['key'],),
                forward_baggage=('0d0d0d',),
                backward_routes=(flights[2]['key'],),
                backward_baggage=('0d0d0d',),
                tariff=1234,
            ),
            create_fare(
                forward_routes=(flights[1]['key'],),
                forward_baggage=('1d1d23d',),
                backward_routes=(flights[2]['key'],),
                backward_baggage=('1d1d23d',),
                tariff=12345,
            ),

        )
        return create_datum(fares, flights)

    def test_transfers_count_0(self):
        params = {
            'filters': {
                'transfer_filters': {
                    'count': 0,
                    'min_duration': None,
                    'max_duration': None,
                    'has_airport_change': None,
                    'has_night': None,
                }
            }
        }
        datum = self.one_way_datum()

        filter_and_slice(datum, params)

        assert len(datum.fares) == 1
        assert datum.fares[0].tariff.value == 1234

    def test_transfers_count_1(self):
        params = {
            'filters': {
                'transfer_filters': {
                    'count': 1,
                    'min_duration': None,
                    'max_duration': None,
                    'has_airport_change': None,
                    'has_night': None,
                }
            }
        }

        datum = self.one_way_datum()
        filter_and_slice(datum, params)

        assert len(datum.fares) == 2
        assert datum.fares[0].tariff.value == 1234
        assert datum.fares[1].tariff.value == 12345

    def test_airport_change(self):
        flights = (
            create_flight(
                number='WW 1', station_from_id=1, station_to_id=2
            ),
            create_flight(
                number='WW 2', station_from_id=3, station_to_id=4
            ),
        )
        fares = (
            create_fare(forward_routes=map(itemgetter('key'), flights)),
        )
        datum = create_datum(fares, flights)

        params = {
            'filters': {
                'transfer_filters': {
                    'count': None,
                    'min_duration': None,
                    'max_duration': None,
                    'has_airport_change': False,
                    'has_night': None,
                }
            }
        }

        filter_and_slice(datum, params)

        assert len(datum.fares) == 0
        assert len(datum.flights) == 0

    def _run_transfers_test(
        self, departure_1, arrival_1, departure_2, arrival_2, params, filtered=True
    ):
        flights = (
            create_flight(
                departure=departure_1,
                arrival=arrival_1,
                number='WW 1', station_from_id=1, station_to_id=2,
            ),
            create_flight(
                departure=departure_2,
                arrival=arrival_2,
                number='WW 2', station_from_id=2, station_to_id=3
            ),
        )
        fares = (
            create_fare(forward_routes=map(itemgetter('key'), flights)),
        )
        datum = create_datum(fares, flights)

        default_filter = dict.fromkeys(
            ('count', 'min_duration', 'max_duration', 'has_airport_change', 'has_night'),
            None
        )
        default_filter.update(params)
        params = {
            'filters': {
                'transfer_filters': default_filter,
            }
        }

        self.assertFiltered(datum, params, filtered=filtered)

    def test_night_transfers_night_arrival(self):
        self._run_transfers_test(
            departure_1='2018-05-18T18:00:00',
            arrival_1='2018-05-19T02:00:00',
            departure_2='2018-05-19T11:00:00',
            arrival_2='2018-05-19T12:00:00',
            params={'has_night': False},
            filtered=True
        )

    def test_night_transfers_night_departure(self):
        self._run_transfers_test(
            departure_1='2018-05-18T18:00:00',
            arrival_1='2018-05-18T19:00:00',
            departure_2='2018-05-19T02:00:00',
            arrival_2='2018-05-19T04:00:00',
            params={'has_night': False},
            filtered=True
        )

    def test_night_transfers_very_long_transfer(self):
        self._run_transfers_test(
            departure_1='2018-05-18T18:00:00',
            arrival_1='2018-05-18T19:00:00',
            departure_2='2018-05-19T18:00:00',
            arrival_2='2018-05-19T19:00:00',
            params={'has_night': False},
            filtered=True
        )

    def test_night_transfers_normal_transfer(self):
        self._run_transfers_test(
            departure_1='2018-05-18T18:00:00',
            arrival_1='2018-05-18T19:00:00',
            departure_2='2018-05-18T21:00:00',
            arrival_2='2018-05-18T22:00:00',
            params={'has_night': False},
            filtered=False
        )

    def test_min_transfer_duration_filtered(self):
        self._run_transfers_test(
            departure_1='2018-05-18T18:00:00',
            arrival_1='2018-05-18T19:00:00',
            departure_2='2018-05-18T21:00:00',
            arrival_2='2018-05-18T22:00:00',
            params={'min_duration': 121},
            filtered=True
        )

    def test_min_transfer_duration_not_filtered(self):
        self._run_transfers_test(
            departure_1='2018-05-18T18:00:00',
            arrival_1='2018-05-18T19:00:00',
            departure_2='2018-05-18T21:00:00',
            arrival_2='2018-05-18T22:00:00',
            params={'min_duration': 30},
            filtered=False
        )

    def test_max_transfer_duration_filtered(self):
        self._run_transfers_test(
            departure_1='2018-05-18T18:00:00',
            arrival_1='2018-05-18T19:00:00',
            departure_2='2018-05-18T21:00:00',
            arrival_2='2018-05-18T22:00:00',
            params={'max_duration': 30},
            filtered=True
        )

    def test_max_transfer_duration_not_filtered(self):
        self._run_transfers_test(
            departure_1='2018-05-18T18:00:00',
            arrival_1='2018-05-18T19:00:00',
            departure_2='2018-05-18T21:00:00',
            arrival_2='2018-05-18T22:00:00',
            params={'max_duration': 120},
            filtered=False
        )

    def test_transfer_duration_range_filtered(self):
        self._run_transfers_test(
            departure_1='2018-05-18T18:00:00',
            arrival_1='2018-05-18T19:00:00',
            departure_2='2018-05-18T21:00:00',
            arrival_2='2018-05-18T22:00:00',
            params={'min_duration': 121, 'max_duration': 200},
            filtered=True
        )
        self._run_transfers_test(
            departure_1='2018-05-18T18:00:00',
            arrival_1='2018-05-18T19:00:00',
            departure_2='2018-05-18T21:00:00',
            arrival_2='2018-05-18T22:00:00',
            params={'min_duration': 30, 'max_duration': 119},
            filtered=True
        )

    def test_transfer_duration_range_not_filtered(self):
        self._run_transfers_test(
            departure_1='2018-05-18T18:00:00',
            arrival_1='2018-05-18T19:00:00',
            departure_2='2018-05-18T21:00:00',
            arrival_2='2018-05-18T22:00:00',
            params={'min_duration': 30, 'max_duration': 120},
            filtered=False
        )


class TimeFilterTest(FiltersTestCase):
    DT_FMT = '%Y-%m-%dT%H:%M:%S'

    def _params_for_filter(self, params):
        keys = (
            'forward_departure_min', 'forward_departure_max',
            'forward_arrival_min', 'forward_arrival_max',
            'backward_departure_min', 'backward_departure_max',
            'backward_arrival_min', 'backward_arrival_max'
        )
        default_filter = dict.fromkeys(keys, None)

        default_filter.update(params)
        return {
            'filters': {
                'time_filters': default_filter,
            }
        }

    def _fmt_dt(self, dt):
        return dt.strftime(self.DT_FMT)

    def _run_cases(self, datum, orig_dt, direction, state):
        prefix = direction + '_' + state
        _min = prefix + '_min'
        _max = prefix + '_max'

        params = self._params_for_filter({
            _min: self._fmt_dt(orig_dt),
            _max: self._fmt_dt(orig_dt),
        })
        self.assertFiltered(
            datum, params, False,
            'Ошибка при фильтрации по времени на границе отрезка'
        )

        params = self._params_for_filter({
            _min: self._fmt_dt(orig_dt - timedelta(minutes=1)),
            _max: self._fmt_dt(orig_dt + timedelta(minutes=1))
        })
        self.assertFiltered(
            datum, params, False,
            'Ошибка при фильтрации по времени внутри интервала'
        )

        params = self._params_for_filter({
            _min: self._fmt_dt(orig_dt + timedelta(minutes=1))
        })
        self.assertFiltered(
            datum, params, True,
            'Ошибка при фильтрации по минимальному времени'
        )

        params = self._params_for_filter({
            _max: self._fmt_dt(orig_dt - timedelta(minutes=1))
        })
        self.assertFiltered(
            datum, params, True,
            'Ошибка при фильтрации по максимальному времени'
        )

        params = self._params_for_filter({
            _min: self._fmt_dt(orig_dt + timedelta(minutes=30)),
            _max: self._fmt_dt(orig_dt + timedelta(minutes=60))
        })
        self.assertFiltered(
            datum, params, True,
            'Ошибка при фильтрации время раньше левой границы интервала'
        )

        params = self._params_for_filter({
            _min: self._fmt_dt(orig_dt - timedelta(minutes=30)),
            _max: self._fmt_dt(orig_dt - timedelta(minutes=60))
        })
        self.assertFiltered(
            datum, params, True,
            'Ошибка при фильтрации время позже правой границы интервала'
        )

    def test_one_way_direct(self):
        departure_dt = datetime(2018, 5, 18, 18, 0)
        arrival_dt = datetime(2018, 5, 18, 19, 0)
        flights = (
            create_flight(departure=self._fmt_dt(departure_dt),
                          arrival=self._fmt_dt(arrival_dt)),
        )
        fares = (
            create_fare(forward_routes=map(itemgetter('key'), flights)),
        )
        datum = create_datum(fares, flights)
        self._run_cases(datum, departure_dt, 'forward', 'departure')
        self._run_cases(datum, arrival_dt, 'forward', 'arrival')

    def test_one_way_one_transfer(self):
        departure_dt = datetime(2018, 5, 18, 18, 0)
        departure_1 = self._fmt_dt(departure_dt)
        arrival_1 = '2018-05-18T19:00:00'
        departure_2 = '2018-05-18T23:00:00'
        arrival_dt = datetime(2018, 5, 19, 2, 0)
        arrival_2 = self._fmt_dt(arrival_dt)

        flights = (
            create_flight(departure=departure_1, arrival=arrival_1,
                          number='WW 1', station_from_id=1, station_to_id=2),
            create_flight(departure=departure_2, arrival=arrival_2,
                          number='WW 2', station_from_id=2, station_to_id=3),
        )
        fares = (
            create_fare(forward_routes=map(itemgetter('key'), flights)),
        )
        datum = create_datum(fares, flights)
        self._run_cases(datum, departure_dt, 'forward', 'departure')
        self._run_cases(datum, arrival_dt, 'forward', 'arrival')

    def test_one_way_two_transfers(self):
        departure_dt = datetime(2018, 5, 18, 18, 0)
        departure_1 = self._fmt_dt(departure_dt)
        arrival_1 = '2018-05-18T19:00:00'
        departure_2 = '2018-05-18T20:00:00'
        arrival_2 = '2018-05-18T22:00:00'
        departure_3 = '2018-05-18T23:00:00'
        arrival_dt = datetime(2018, 5, 19, 2, 0)
        arrival_3 = self._fmt_dt(arrival_dt)

        flights = (
            create_flight(departure=departure_1, arrival=arrival_1,
                          number='WW 1', station_from_id=1, station_to_id=2),
            create_flight(departure=departure_2, arrival=arrival_2,
                          number='WW 2', station_from_id=2, station_to_id=3),
            create_flight(departure=departure_3, arrival=arrival_3,
                          number='WW 3', station_from_id=3, station_to_id=4),
        )
        fares = (
            create_fare(forward_routes=map(itemgetter('key'), flights)),
        )
        datum = create_datum(fares, flights)
        self._run_cases(datum, departure_dt, 'forward', 'departure')
        self._run_cases(datum, arrival_dt, 'forward', 'arrival')

    def test_return_direct(self):
        fwd_departure_dt = datetime(2018, 5, 18, 18, 0)
        fwd_arrival_dt = datetime(2018, 5, 18, 20, 0)
        bwd_departure_dt = datetime(2018, 5, 21, 23, 0)
        bwd_arrival_dt = datetime(2018, 5, 22, 1, 0)

        fwd_flights = (
            create_flight(departure=self._fmt_dt(fwd_departure_dt),
                          arrival=self._fmt_dt(fwd_arrival_dt),
                          number='WW 1', station_from_id=1, station_to_id=2),
        )
        bwd_flights = (
            create_flight(departure=self._fmt_dt(bwd_departure_dt),
                          arrival=self._fmt_dt(bwd_arrival_dt),
                          number='WW 2', station_from_id=2, station_to_id=1),
        )
        fares = (
            create_fare(forward_routes=map(itemgetter('key'), fwd_flights),
                        backward_routes=map(itemgetter('key'), bwd_flights)),
        )
        datum = create_datum(fares, fwd_flights + bwd_flights)
        self._run_cases(datum, fwd_departure_dt, 'forward', 'departure')
        self._run_cases(datum, fwd_arrival_dt, 'forward', 'arrival')
        self._run_cases(datum, bwd_departure_dt, 'backward', 'departure')
        self._run_cases(datum, bwd_arrival_dt, 'backward', 'arrival')

    def test_return_with_transfers(self):
        fwd_departure_dt = datetime(2018, 5, 18, 18, 0)
        fwd_arrival_1 = '2018-05-18T19:00:00'
        fwd_departure_2 = '2018-05-19T03:00:00'
        fwd_arrival_dt = datetime(2018, 5, 19, 5, 0)
        bwd_departure_dt = datetime(2018, 5, 21, 23, 0)
        bwd_arrival_1 = '2018-05-22T02:00:00'
        bwd_departure_2 = '2018-05-22T03:00:00'
        bwd_arrival_dt = datetime(2018, 5, 22, 4, 0)

        fwd_flights = (
            create_flight(departure=self._fmt_dt(fwd_departure_dt),
                          arrival=fwd_arrival_1,
                          number='WW 1', station_from_id=1, station_to_id=2),
            create_flight(departure=fwd_departure_2,
                          arrival=self._fmt_dt(fwd_arrival_dt),
                          number='WW 2', station_from_id=2, station_to_id=3),
        )
        bwd_flights = (
            create_flight(departure=self._fmt_dt(bwd_departure_dt),
                          arrival=bwd_arrival_1,
                          number='WW 3', station_from_id=3, station_to_id=2),
            create_flight(departure=bwd_departure_2,
                          arrival=self._fmt_dt(bwd_arrival_dt),
                          number='WW 4', station_from_id=2, station_to_id=1),
        )
        fares = (
            create_fare(forward_routes=map(itemgetter('key'), fwd_flights),
                        backward_routes=map(itemgetter('key'), bwd_flights)),
        )
        datum = create_datum(fares, fwd_flights + bwd_flights)
        self._run_cases(datum, fwd_departure_dt, 'forward', 'departure')
        self._run_cases(datum, fwd_arrival_dt, 'forward', 'arrival')
        self._run_cases(datum, bwd_departure_dt, 'backward', 'departure')
        self._run_cases(datum, bwd_arrival_dt, 'backward', 'arrival')


class AirportFilterTest(FiltersTestCase):
    def _params_for_filter(self, params):
        keys = (
            'forward_departure', 'forward_arrival', 'forward_transfers',
            'backward_departure', 'backward_arrival', 'backward_transfers'
        )
        default_filter = dict.fromkeys(keys, None)

        default_filter.update(params)
        return {
            'filters': {
                'airports_filters': default_filter,
            }
        }

    def test_one_way_empty_filters(self):
        """Отправляем пустой фильтр ничего не должно происходить"""
        flights = (
            create_flight(),
        )
        fares = (
            create_fare(forward_routes=map(itemgetter('key'), flights)),
        )
        datum = create_datum(fares, flights)

        params = self._params_for_filter({})
        self.assertFiltered(
            datum, params, False,
        )

    def test_one_filter_direct_flights(self):
        """
        Если передан фильтр с пересадками в конкретных аэропортах,
         то все прямые рейсы удаляются
        """
        flights = (
            create_flight(),
        )
        fares = (
            create_fare(forward_routes=map(itemgetter('key'), flights)),
        )
        datum = create_datum(fares, flights)

        params = self._params_for_filter({
            'forward_transfers': [999],
        })
        self.assertFiltered(
            datum, params, True,
        )

    def _flights(self, departure_id=1, transfer_id=2, arrival_id=3):
        return (
            create_flight(
                departure='2018-05-18T18:00:00', arrival='2018-05-18T20:00:00',
                station_from_id=departure_id, station_to_id=transfer_id,
                number='AA 1'
            ),
            create_flight(
                departure='2018-05-19T02:00:00', arrival='2018-05-19T04:00:00',
                station_from_id=transfer_id, station_to_id=arrival_id,
                number='AA 2'
            ),
        )

    def _one_way_test_datum(self, departure_id=1, transfer_id=2, arrival_id=3):
        flights = self._flights(departure_id, transfer_id, arrival_id)
        fares = (
            create_fare(forward_routes=map(itemgetter('key'), flights)),
        )
        datum = create_datum(fares, flights)
        return datum

    def _return_test_datum(self, departure_id=1, transfer_id=2, arrival_id=3):
        forward_flights = (
            create_flight(
                departure='2018-05-01T14:00:00', arrival='2018-05-01T16:00:00',
                station_from_id=arrival_id, station_to_id=departure_id,
                number='AA 0'
            ),
        )
        backward_flights = self._flights(departure_id, transfer_id, arrival_id)
        fares = (
            create_fare(
                forward_routes=map(itemgetter('key'), forward_flights),
                backward_routes=map(itemgetter('key'), backward_flights),
            ),
        )
        datum = create_datum(fares, chain(forward_flights, backward_flights))
        return datum

    def _run_transfer_cases(self, datum, direction, transfer_id):
        key = direction + '_transfers'

        params = self._params_for_filter({
            key: [666],
        })
        self.assertFiltered(
            datum, params, True,
            'Рейсы, в которых нет пересадок из переданных - фильтруются'
        )

        params = self._params_for_filter({
            key: [transfer_id],
        })
        self.assertFiltered(
            datum, params, False,
            'Рейсы, в которых есть пересадки из переданных - не фильтруются'
        )

    def _run_endpoints_cases(self, datum, direction, departure_id, transfer_id,
                             arrival_id, unused_id):
        departure_key = direction + '_departure'
        direction_length = len(datum.fares[0].route.forward) if direction == 'forward' else len(datum.fares[0].route.backward)

        params = self._params_for_filter({
            departure_key: [arrival_id, transfer_id, unused_id],
        })
        self.assertFiltered(
            datum, params, direction_length > 0,
            'При несовпадении начала маршрута с переданными - перелет должен фильтроваться'
        )

        params = self._params_for_filter({
            departure_key: [departure_id, unused_id],
        })
        self.assertFiltered(
            datum, params, False,
            'При совпадении начала маршрута с переданными - перелет не должен фильтроваться'
        )

        arrival_key = direction + '_arrival'
        params = self._params_for_filter({
            arrival_key: [departure_id, transfer_id, unused_id],
        })
        self.assertFiltered(
            datum, params, direction_length > 0,
            'При несовпадении конца маршрута с переданными - перелет должен фильтроваться'
        )

        params = self._params_for_filter({
            arrival_key: [arrival_id, unused_id],
        })
        self.assertFiltered(
            datum, params, False,
            'При совпадении конца маршрута с переданными - перелет не должен фильтроваться'
        )

    def test_one_way_filter_by_transfers(self):
        departure_id, transfer_id, arrival_id = 1, 2, 3
        datum = self._one_way_test_datum(departure_id, transfer_id, arrival_id)

        self._run_transfer_cases(datum, 'forward', transfer_id)

    def test_return_filter_by_transfers(self):
        departure_id, transfer_id, arrival_id = 1, 2, 3
        datum = self._return_test_datum(departure_id, transfer_id, arrival_id)
        self._run_transfer_cases(datum, 'backward', transfer_id)

    def test_one_way_endpoints(self):
        departure_id, transfer_id, arrival_id = 1, 2, 3
        unused_id = 4
        datum = self._one_way_test_datum(departure_id, transfer_id, arrival_id)

        self._run_endpoints_cases(datum, 'forward', departure_id, transfer_id,
                                  arrival_id, unused_id)

    def test_return_endpoints(self):
        departure_id, transfer_id, arrival_id = 1, 2, 3
        unused_id = 4
        datum = self._return_test_datum(departure_id, transfer_id, arrival_id)

        self._run_endpoints_cases(datum, 'backward', departure_id, transfer_id,
                                  arrival_id, unused_id)

    def test_return_enpoints_on_one_way_fare(self):
        departure_id, transfer_id, arrival_id = 1, 2, 3
        unused_id = 4
        datum = self._one_way_test_datum(departure_id, transfer_id, arrival_id)

        self._run_endpoints_cases(datum, 'backward', departure_id, transfer_id,
                                  arrival_id, unused_id)


class AirlineFilterTest(FiltersTestCase):
    COMPANY_ID_1 = 1
    COMPANY_ID_2 = 2

    def test_filtered_one_company(self):
        flights = (
            create_flight(
                departure='2018-05-01T14:00:00', arrival='2018-05-01T16:00:00',
                station_from_id=1, station_to_id=2,
                number='AA 1', company_id=self.COMPANY_ID_1
            ),
        )
        fares = (
            create_fare(forward_routes=map(itemgetter('key'), flights)),
        )
        datum = create_datum(fares, flights)

        params = {'filters': {'airlines': [self.COMPANY_ID_1]}}
        self.assertFiltered(datum, params, False)

        params = {'filters': {'airlines': [self.COMPANY_ID_2]}}
        self.assertFiltered(datum, params, True)

    def test_filtered_two_companies(self):
        flights = (
            create_flight(
                departure='2018-05-01T14:00:00', arrival='2018-05-01T16:00:00',
                station_from_id=1, station_to_id=2,
                number='AA 1', company_id=self.COMPANY_ID_1
            ),
            create_flight(
                departure='2018-05-01T18:00:00', arrival='2018-05-01T20:00:00',
                station_from_id=2, station_to_id=3,
                number='BB 1', company_id=self.COMPANY_ID_2
            ),
        )
        fares = (
            create_fare(forward_routes=map(itemgetter('key'), flights)),
        )
        datum = create_datum(fares, flights)

        params = {'filters': {'airlines': [self.COMPANY_ID_1]}}
        self.assertFiltered(datum, params, True)

        params = {'filters': {'airlines': [self.COMPANY_ID_2]}}
        self.assertFiltered(datum, params, True)

        params = {'filters': {'airlines': [self.COMPANY_ID_1, self.COMPANY_ID_2]}}
        self.assertFiltered(datum, params, False)

    def test_filtered_backward_two_companies(self):
        forward_flights = (
            create_flight(
                departure='2018-05-01T14:00:00', arrival='2018-05-01T16:00:00',
                station_from_id=1, station_to_id=2,
                number='AA 1', company_id=self.COMPANY_ID_1
            ),
            create_flight(
                departure='2018-05-01T18:00:00', arrival='2018-05-01T20:00:00',
                station_from_id=2, station_to_id=3,
                number='BB 1', company_id=self.COMPANY_ID_2
            ),
        )
        backward_flights = (
            create_flight(
                departure='2018-05-02T14:00:00', arrival='2018-05-02T16:00:00',
                station_from_id=3, station_to_id=2,
                number='AA 2', company_id=self.COMPANY_ID_1
            ),
            create_flight(
                departure='2018-05-02T18:00:00', arrival='2018-05-02T20:00:00',
                station_from_id=2, station_to_id=1,
                number='BB 2', company_id=self.COMPANY_ID_2
            ),
        )
        fares = (
            create_fare(
                forward_routes=map(itemgetter('key'), forward_flights),
                backward_routes=map(itemgetter('key'), backward_flights),
            ),
        )
        datum = create_datum(fares, chain(forward_flights, backward_flights))

        params = {'filters': {'airlines': [self.COMPANY_ID_1]}}
        self.assertFiltered(datum, params, True)

        params = {'filters': {'airlines': [self.COMPANY_ID_2]}}
        self.assertFiltered(datum, params, True)

        params = {'filters': {'airlines': [self.COMPANY_ID_1, self.COMPANY_ID_2]}}
        self.assertFiltered(datum, params, False)
