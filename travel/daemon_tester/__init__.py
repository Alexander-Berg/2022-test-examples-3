# -*- coding: utf-8 -*-
import random
from datetime import timedelta, datetime

import pytz

from travel.avia.library.python.tester.factories import create_airport, create_settlement

from travel.avia.ticket_daemon.ticket_daemon.api.query import Query
from travel.avia.ticket_daemon.ticket_daemon.api.flights import Variant, Segment, IATAFlight
from travel.avia.ticket_daemon.ticket_daemon.lib.currency import Price


def create_query(
    when='2017-09-01', return_date=None, passengers='1_0_0', national_version='ru', lang='ru', from_is_settlement=True,
    to_is_settlement=True, attach_from_settlement=True, attach_to_settlement=True
):
    from_c = None
    to_c = None
    if attach_from_settlement:
        from_c = create_settlement(title='CITY-FROM')
    if attach_to_settlement:
        to_c = create_settlement(title='CITY-TO')

    def get_airport(point):
        rand = random.random()
        return create_airport(title='AIRPORT-{}'.format(rand), iata='IATA-{}'.format(rand), settlement=point)

    from_a = get_airport(from_c)
    to_a = get_airport(to_c)

    from_p = from_c if from_is_settlement else from_a
    to_p = to_c if to_is_settlement else to_a

    key = '{from_key}_{to_key}_{when}_{return_date}_economy_{passengers}_{nv}'.format(
        from_key=from_p.point_key,
        to_key=to_p.point_key,
        when=when,
        return_date=return_date,
        passengers=passengers,
        nv=national_version
    )

    return Query.from_key(key, service='ticket', lang=lang, t_code='plane')


def create_price(price=1000, currency='RUR'):
    return Price(price, currency)


def create_variant(query, partner, price=1000, national_price=2000, currency='RUR', national_currency='RUR',
                   forward_flights=None, backward_flights=None, charter=None):
    if forward_flights is None:
        forward_flights = []
    if backward_flights is None:
        backward_flights = []

    v = Variant()
    v.tariff = create_price(
        price=price,
        currency=currency
    )

    v.national_tariff = create_price(
        price=national_price,
        currency=national_currency
    )

    v.forward.segments = forward_flights[:]

    if query.date_backward:
        v.backward.segments = backward_flights[:]

    v.url = 'https://avia.yandex.ru'

    v.klass = query.klass

    v.order_data = {
        'url': v.url
    }

    v.partner_code = partner.code
    if v.partner_code.startswith('dohop_'):
        v.dohop_vendor_id = int(v.partner_code.split('_')[-1])
    v.partner = partner

    v.charter = charter

    return v


def create_flight(
    iata_from='FROM-IATA', iata_to='TO-IATA',
    company_iata='COMPANY-IATA',
    company=None,
    station_from=None,
    station_to=None,
    local_departure=datetime(2017, 9, 1),
    delta=60,
    baggage=None,
    avia_company=None,
    tz_from=None,
    tz_to=None,
    selfconnect=None,
    fare_code=None,
    fare_family=None,
    number=None,
):
    f = IATAFlight()

    f.station_from = station_from
    f.station_to = station_to
    if station_from:
        iata_from = station_from.iata
    if station_to:
        iata_to = station_to.iata

    f.station_from_iata = iata_from
    f.station_to_iata = iata_to

    f.local_departure = local_departure
    f.local_arrival = local_departure + timedelta(
        minutes=delta
    )

    tz_from = tz_from or pytz.utc
    tz_to = tz_to or pytz.utc

    f.departure = tz_from.localize(local_departure)
    f.arrival = tz_to.localize(f.local_arrival)

    f.company = company
    if company:
        f.company_iata = company.iata
    f.avia_company = avia_company

    f.number = number or '{} {}'.format(iata_from, iata_to)
    f.fare_code = fare_code
    return Segment(f, baggage=baggage, selfconnect=selfconnect, fare_family=fare_family)
