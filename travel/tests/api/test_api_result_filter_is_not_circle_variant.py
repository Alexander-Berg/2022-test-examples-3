# -*- coding: utf-8 -*-
from itertools import product, permutations
import pytest

from travel.avia.library.python.tester.factories import create_station, create_settlement, create_station2settlement
from travel.avia.ticket_daemon.ticket_daemon.api.flights import Variant, IATAFlight
from travel.avia.library.python.ticket_daemon.memo import reset_all_caches

FAIL_QUERY = ''
FAIL_PARTNER = ''


def create_stations():
    moscow = create_settlement(title='Москва')
    zhukovsky = create_settlement(title='Жуковский')
    cologne = create_settlement(title='Кельн')
    ekb = create_settlement(title='Екатеринбург')

    svx = create_station(settlement_id=ekb.id, title='Кольцово')
    dme = create_station(settlement_id=moscow.id, title='Домодедово')
    vnk = create_station(settlement_id=moscow.id, title='Внуково')
    zia = create_station(settlement_id=moscow.id, title='Жуковский')
    vnz = create_station(settlement_id=moscow.id, title='Внуково ЖД')
    bal = create_station(settlement_id=moscow.id, title='Балтийская ЖД')
    vix = create_station(settlement_id=moscow.id, title='Выхтно ЖД')
    cgn = create_station(settlement_id=cologne.id, title='Кельн-Бонн')
    qkl = create_station(settlement_id=cologne.id, title='Кельн Главный')

    create_station2settlement(station=zia, settlement=zhukovsky)

    return {
        'svx': svx, 'dme': dme, 'vnk': vnk, 'zia': zia, 'vnz': vnz,
        'cgn': cgn, 'qkl': qkl, 'bal': bal, 'vix': vix
    }


def fake_variant(flights_to, flights_from=None):
    variant = Variant()
    variant.forward.segments = flights_to
    if flights_from:
        variant.backward.segments = flights_from
    return variant


def fake_flight(station_from, station_to):
    flight = IATAFlight()
    flight.station_from = station_from
    flight.station_to = station_to
    return flight


def create_variants(st):
    circular_variants, non_circular_variants = [], []

    for msk1, msk2 in product([st['dme'], st['vnk'], st['zia']], repeat=2):
        for not_msk1, not_msk2 in permutations([st['svx'], st['cgn'], st['qkl']], r=2):
            circular_flights = (
                fake_flight(not_msk1, msk1),
                fake_flight(msk1, not_msk2),
                fake_flight(not_msk2, msk2),
            )
            non_circular_flights = (
                fake_flight(not_msk1, msk1),
                fake_flight(msk2, not_msk2),
            )

            circular_variants.extend([
                fake_variant(circular_flights),
                fake_variant(list(reversed(circular_flights))),
                fake_variant(circular_flights, non_circular_flights),
                fake_variant(circular_flights, circular_flights),
                fake_variant(non_circular_flights, circular_flights)
            ])

            non_circular_variants.extend([
                fake_variant(non_circular_flights),
                fake_variant(list(reversed(non_circular_flights))),
                fake_variant(non_circular_flights, non_circular_flights)
            ])

    return circular_variants, non_circular_variants


@pytest.mark.dbuser
def test_circular_variants():
    reset_all_caches()
    stations = create_stations()
    circular_variants, non_circular_variants = create_variants(stations)

    for good_variant in non_circular_variants:
        assert good_variant.is_not_circle_variant()

    for bad_variant in circular_variants:
        assert not bad_variant.is_not_circle_variant()


@pytest.mark.dbuser
def test_twice_in_city_on_train():
    reset_all_caches()
    stations = create_stations()
    good_flights = (
        fake_flight(stations['cgn'], stations['vnk']),  # to moscow of flight
        fake_flight(stations['vnz'], stations['vix']),  # inside moscow
        fake_flight(stations['vix'], stations['bal']),  # inside moscow
    )
    good_variant = fake_variant(good_flights)

    assert good_variant.is_not_circle_variant()
