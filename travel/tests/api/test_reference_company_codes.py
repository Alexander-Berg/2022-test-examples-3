# -*- coding: utf-8 -*-
from __future__ import unicode_literals

import pytest

from travel.avia.ticket_daemon.ticket_daemon.api.flights import IATAFlight, Variant

ROSSIA_ID = 8565
AEROFLOT_ID = 26


@pytest.mark.parametrize('flight_number, company_id', [
    ('SU 2406', AEROFLOT_ID),
    ('ПЛ 2406', AEROFLOT_ID),
    ('SU 6406', ROSSIA_ID),
])
@pytest.mark.dbuser
def skip_test_aeroflot_russia(flight_number, company_id):
    # TODO move to test_complete_flights_company.py
    variant = Variant()
    flight = IATAFlight()

    flight.station_from_iata = 'MOW'
    flight.station_to_iata = 'SVX'
    code, number = flight_number.split()
    flight.company_iata = code
    flight.number = flight.company_iata + ' ' + number

    variant.forward.segments = [flight]

    for segment in variant.all_segments:
        segment.complete(variant.partner, variant.is_charter)

    company = variant.forward.segments[0].company
    assert company
    print 'Company', '(%s)%s' % (company.id, company.L_title('ru'))
    assert company.id == company_id
