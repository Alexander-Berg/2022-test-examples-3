# -*- coding: utf-8 -*-
import datetime
import mock

from travel.avia.library.python.tester.testcase import TestCase
from travel.avia.ticket_daemon.tests.partners.helper import (
    expected_variants, get_mocked_response, get_query, assert_variants_equal,
    SettlementMock
)
from travel.avia.ticket_daemon.ticket_daemon.partners import bookandtrip


@mock.patch('requests.get', return_value=get_mocked_response('bookandtrip.xml'))
def test_bookandtrip_query(mocked_request):
    expected = expected_variants('bookandtrip.json')
    test_query = get_query(
        point_to=SettlementMock(iata='LED', code='RU', id=2),
        date_forward=datetime.date(2017, 2, 6),
        date_backward=None,
        passengers={'infants': 0, 'adults': 2, 'children': 0},
    )
    variants = next(bookandtrip.query(test_query))
    assert_variants_equal(expected, variants)


@mock.patch('requests.get', return_value=get_mocked_response('bookandtrip_empty.xml'))
def test_bookandtrip_query_empty_response(mocked_request):
    expected = []
    variants = next(bookandtrip.query(get_query()))
    assert_variants_equal(expected, variants)


class BookAndTripBaggageTest(TestCase):
    def test_broken(self):
        for raw_baggage in ['zzz', None]:
            assert bookandtrip._get_baggage(raw_baggage).as_dict() =={
                'info': {
                    'included': None,
                    'pc': None,
                    'wt': None
                },
                'key': None
            }

    def test_non_baggage(self):
        for raw_baggage in ('0N', '0P', '0PC', 'NIL'):
            assert bookandtrip._get_baggage(raw_baggage).as_dict() =={
                'info': {
                    'included': {'count': 0, 'source': 'partner'},
                    'pc': {'count': 0, 'source': 'partner'},
                    'wt': None
                },
                'key': '0p0pN'
            }, raw_baggage

    def test_baggage_with_one_place(self):
        for raw_baggage in ('1N', '1P', '1PC'):
            assert bookandtrip._get_baggage(raw_baggage).as_dict() =={
                'info': {
                    'included': {'count': 1, 'source': 'partner'},
                    'pc': {'count': 1, 'source': 'partner'},
                    'wt': None
                },
                'key': '1p1pN'
            }

    def test_baggage_with_limited_weight(self):
        for raw_baggage in ('20K', '20KG'):
            assert bookandtrip._get_baggage(raw_baggage).as_dict() =={
                'info': {
                    'included': {'count': 1, 'source': 'partner'},
                    'pc': {'count': 1, 'source': 'partner'},
                    'wt': {'count': 20, 'source': 'partner'}
                },
                'key': '1p1p20p'
            }

    def test_baggage_with_less_limited_weight(self):
        for raw_baggage in ('30K', '30KG'):
            assert bookandtrip._get_baggage(raw_baggage).as_dict() =={
                'info': {
                    'included': {'count': 1, 'source': 'partner'},
                    'pc': {'count': 1, 'source': 'partner'},
                    'wt': {'count': 30, 'source': 'partner'}
                },
                'key': '1p1p30p'
            }

    def test_infant_and_adult_baggage_place(self):
        for raw_baggage in ('2N/1N', '2P/1P', '2PC/1PC'):
            assert bookandtrip._get_baggage(raw_baggage).as_dict() =={
                'info': {
                    'included': {'count': 1, 'source': 'partner'},
                    'pc': {'count': 2, 'source': 'partner'},
                    'wt': None
                },
                'key': '1p2pN'
            }, raw_baggage

    def test_infant_and_adult_baggage_with_limited_weight(self):
        for raw_baggage in ('30K/20K', '30KG/20KG'):
            assert bookandtrip._get_baggage(raw_baggage).as_dict() =={
                'info': {
                    'included': {'count': 1, 'source': 'partner'},
                    'pc': {'count': 1, 'source': 'partner'},
                    'wt': {'count': 30, 'source': 'partner'}
                },
                'key': '1p1p30p'
            }, raw_baggage
