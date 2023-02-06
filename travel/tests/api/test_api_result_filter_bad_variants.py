# coding: utf-8
from datetime import date, datetime as dt

import pytest

from travel.avia.library.python.common.models.currency import Price
from travel.avia.library.python.common.models.geo import Settlement
from travel.avia.ticket_daemon.ticket_daemon.api.flights import Variant, Segment, IATAFlight
from travel.avia.ticket_daemon.ticket_daemon.api.query import Query


def _fill_segments(variant_flights, flight_params):
    if not flight_params:
        return

    for segment, flight_param in zip(variant_flights.segments, flight_params):
        segment.klass = flight_param['klass']
        flight = segment._flight
        flight.station_from = flight_param['from_id']
        flight.station_to = flight_param['to_id']
        flight.local_arrival = flight_param['loc_arr']
        flight.arrival = flight_param['loc_arr']
        flight.local_departure = flight_param['loc_dep']
        flight.departure = flight_param['loc_dep']


def _create_variants(forward, backward, klass='economy', value=0):
    variant = Variant()
    variant.forward.segments = [Segment(IATAFlight()) for _ in range(len(forward))]
    variant.backward.segments = [Segment(IATAFlight()) for _ in range(len(backward))]

    _fill_segments(variant.forward, forward)
    _fill_segments(variant.backward, backward)
    variant.klass = klass
    variant.tariff = Price(value)

    return variant


def _create_query(_query, point_from_id, point_to_id):
    return Query(
        point_from=Settlement(id=point_from_id),
        point_to=Settlement(id=point_to_id),
        **_query
    )


forward = ({'from_id': 9600365, 'loc_arr': dt(2020, 2, 2, 2),
            'to_id': 9600421, 'loc_dep': dt(2020, 2, 2, 0), 'klass': 'business'},
           {'from_id': 9600421, 'loc_arr': dt(2020, 2, 2, 7),
            'to_id': 9623597, 'loc_dep': dt(2020, 2, 2, 3), 'klass': 'business'})

backward = ({'from_id': 9623597, 'loc_arr': dt(2020, 2, 10, 8),
             'to_id': 9600421, 'loc_dep': dt(2020, 2, 10, 3), 'klass': 'business'},
            {'from_id': 9600421, 'loc_arr': dt(2020, 2, 10, 12),
             'to_id': 9600365, 'loc_dep': dt(2020, 2, 10, 10), 'klass': 'business'})

b_forward = ({'from_id': 9600365, 'loc_arr': dt(2020, 2, 2, 2),
              'to_id': 9600421, 'loc_dep': dt(2020, 1, 26, 5), 'klass': 'first'},
             {'from_id': 9600421, 'loc_arr': dt(2020, 2, 2, 7),
              'to_id': 9623597, 'loc_dep': dt(2020, 1, 26, 5), 'klass': 'business'})

b_backward = ({'from_id': 9623597, 'loc_arr': dt(2020, 2, 10, 8),
               'to_id': 9600421, 'loc_dep': dt(2020, 1, 26, 5), 'klass': 'first'},
              {'from_id': 9600421, 'loc_arr': dt(2020, 2, 10, 12),
               'to_id': 9600365, 'loc_dep': dt(2020, 1, 26, 5), 'klass': 'economy'})

duty_query = {'passengers': {'adults': 1, 'children': 0, 'infants': 0},
              'date_forward': date(2020, 2, 2),
              'date_backward': date(2020, 2, 10),
              'service': 'ticket',
              'klass': 'business'}


def good_variant():
    return _create_variants(forward, backward, klass='business', value=1000)


def bad_variant():
    return _create_variants(b_forward, b_backward, klass='economy', value=-100)


def empty_variant():
    return _create_variants((), ())


def query():
    return _create_query(duty_query, point_from_id=143, point_to_id=11499)


@pytest.mark.dbuser
@pytest.mark.parametrize('variant_factory,forward_exists,forward_transfer_ok', [
    (good_variant, True, True),
    (bad_variant, True, False),
    (empty_variant, False, True),
])
def test_filter_forward(variant_factory, forward_exists, forward_transfer_ok):
    variant = variant_factory()

    forw_exists = variant.forward_exists()
    forw_transfer_ok = variant.forward_transfer_ok()

    assert forw_exists == forward_exists
    assert forw_transfer_ok == forward_transfer_ok


@pytest.mark.dbuser
@pytest.mark.parametrize('variant_factory,query_factory,backward_fill_if_need,backward_transfer_ok', [
    (good_variant, query, True, True),
    (bad_variant, query, True, False),
    (empty_variant, query, False, True),
])
def test_filter_backward(variant_factory, query_factory, backward_fill_if_need, backward_transfer_ok):
    variant = variant_factory()
    query = query_factory()

    backward_fill = variant.backward_fill_if_need(query)
    back_trans_ok = variant.backward_transfer_ok()

    assert backward_fill == backward_fill_if_need
    assert back_trans_ok == backward_transfer_ok


@pytest.mark.dbuser
@pytest.mark.parametrize('variant_factory,depart_after_arrival', [
    (good_variant, True),
    (bad_variant, False),
])
def test_filter_depart_after_arrival(variant_factory, depart_after_arrival):
    variant = variant_factory()

    dep_after_arr = variant.departure_after_arrival_at_different_steps()

    assert dep_after_arr == depart_after_arrival


@pytest.mark.dbuser
@pytest.mark.parametrize('variant_factory,query_factory,date_forw_is_correct,date_back_is_correct', [
    (good_variant, query, True, True),
    (bad_variant, query, False, False),
])
def test_filter_correct_data(variant_factory, query_factory, date_forw_is_correct, date_back_is_correct):
    variant = variant_factory()
    query = query_factory()

    forw_date_is_ok = variant.date_forward_ok(query)
    back_date_is_ok = variant.date_backward_ok(query)

    assert forw_date_is_ok == date_forw_is_correct
    assert back_date_is_ok == date_back_is_correct


@pytest.mark.dbuser
@pytest.mark.parametrize('variant_factory,query_factory,segment_servise_ok,variant_servise_ok', [
    (good_variant, query, True, True),
    (bad_variant, query, False, False),
])
def test_filter_klasses(variant_factory, query_factory, segment_servise_ok, variant_servise_ok):
    variant = variant_factory()
    query = query_factory()

    segment_serv_ok = variant.is_segment_service_ok(query)
    variant_serv_ok = variant.variant_service_ok(query)

    assert segment_serv_ok == segment_servise_ok
    assert variant_serv_ok == variant_servise_ok


@pytest.mark.dbuser
@pytest.mark.parametrize('variant_factory,price_ok', [
    (good_variant, True),
    (bad_variant, False),
])
def test_filter_price(variant_factory, price_ok):
    variant = variant_factory()

    price_corr = variant.price_ok()

    assert price_corr == price_ok
