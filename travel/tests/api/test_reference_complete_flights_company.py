# -*- coding: utf-8 -*-
from __future__ import unicode_literals

import random

import pytest
from mock import patch

from travel.avia.library.python.common.models.schedule import Company
from travel.avia.library.python.common.models.transport import TransportType
from travel.avia.library.python.tester import factories
from travel.avia.library.python.tester.factories import create_aviacompany, create_companytariff, create_iatacorrection
from travel.avia.library.python.ticket_daemon.memo import reset_all_caches
from travel.avia.ticket_daemon.ticket_daemon.api.flights import IATAFlight
from travel.avia.ticket_daemon.ticket_daemon.daemon.utils import fill


def repr_company(self):
    return u'<Company {}>'.format(self.iata)


def create_company(**kwargs):
    return factories.create_company(t_type_id=TransportType.PLANE_ID, **kwargs)


def _create_flight(flight_number, **kwargs):
    return fill(
        IATAFlight(),
        company_iata=flight_number.split()[0],
        number=flight_number,
        station_from_iata=None,
        station_to_iata=None,
        **kwargs
    )


@pytest.mark.dbuser
def test_complete_flights_iata_company():
    reset_all_caches()
    # Доставание компании просто по iata.
    # Должна выбраться первая попавшаяся, нет правил коррекции.
    company_su = create_company(iata=u'SU')
    flight_su = _create_flight(u'SU 123')
    flight_su.complete()
    assert flight_su.company
    assert flight_su.company.id == company_su.id, 'Any company by iata'


@pytest.mark.dbuser
def test_complete_flights_iata_company_false_positive():
    reset_all_caches()
    company_su = create_company(iata=u'SU')  # noqa: F841
    flight_nocompany = _create_flight(u'NO 000')
    flight_nocompany.complete()
    assert not flight_nocompany.company,  'Iata correction false-positive'


@pytest.mark.dbuser
def test_complete_flights_iata_company_simple_correction():
    reset_all_caches()
    company_su_1 = create_company(iata=u'SU')
    company_su_2 = create_company(iata=u'SU')
    company_su_3 = create_company(iata=u'SU')

    create_iatacorrection(number=u'111', company=company_su_1, code=company_su_1.iata)
    create_iatacorrection(number=u'222', company=company_su_2, code=company_su_2.iata)
    create_iatacorrection(number=u'333', company=company_su_3, code=company_su_3.iata)

    flight_su_1 = _create_flight(u'SU 111')
    flight_su_2 = _create_flight(u'SU 222')
    flight_su_3 = _create_flight(u'SU 333')

    flight_su_1.complete()
    flight_su_2.complete()
    flight_su_3.complete()
    assert flight_su_1.company, 'Company not found by IATA'
    assert flight_su_2.company, 'Company not found by IATA'
    assert flight_su_3.company, 'Company not found by IATA'

    assert flight_su_1.company.id == company_su_1.id, 'Wrong simple iata correction'
    assert flight_su_2.company.id == company_su_2.id, 'Wrong simple iata correction'
    assert flight_su_3.company.id == company_su_3.id, 'Wrong simple iata correction'


@pytest.mark.dbuser
@patch.object(Company, '__repr__', side_effect=repr_company, autospec=True)
def test_iata_correction_s7_globus(_company_repr):
    """
    mysql> select i.*, c.title, c.iata, c.sirena_id
    from importinfo_iatacorrection as i, www_company as c
    where i.company_id=c.id and i.code = 'S7';
    +----+------+-----------------+------------+--------+------+-----------+
    | id | code | number          | company_id | title  | iata | sirena_id |
    +----+------+-----------------+------------+--------+------+-----------+
    | 13 | S7   | 16[1|3|5]       |        260 | Глобус | GH   | ГЛ        |
    | 14 | S7   | 26[1|3|5|7|9]   |        260 | Глобус | GH   | ГЛ        |
    ....
    | 23 | S7   | 270             |        260 | Глобус | GH   | ГЛ        |
    | 24 | S7   | 270GH           |        260 | Глобус | GH   | ГЛ        |
    +----+------+-----------------+------------+--------+------+-----------+

    mysql> select c.id, c.title, c.iata, c.sirena_id from www_company as c where c.iata in ('S7', 'GH');
    +-----+--------------+------+-----------+
    | id  | title        | iata | sirena_id |
    +-----+--------------+------+-----------+
    | 260 | Глобус       | GH   | ГЛ        |
    |  23 | S7 Airlines  | S7   | С7        |
    +-----+--------------+------+-----------+
    """

    # Порядок создания важен. Проверим оба случая. Когда возникает
    # неопределённость, из компаний берётся первая попавшаяся, с меньшим id.
    s3 = create_company(iata='S3', sirena_id='С3')
    globus = create_company(iata='GH', sirena_id='ГЛ')
    s7 = create_company(iata='S7', sirena_id='С7')

    for n in ['16[1|3|5]', '26[2|4|6|8]GH', '270', '270GH']:
        create_iatacorrection(number=n, company=globus, code=s3.iata)
        create_iatacorrection(number=n, company=globus, code=s7.iata)

    flight_s3 = _create_flight('S3 33')
    flight_globus = _create_flight('S7 161')
    flight_s7 = _create_flight('S7 77')

    reset_all_caches()
    flight_s3.complete()
    assert flight_s3.company, 'Company should be found by IATA'
    assert flight_s3.company == s3, 'Wrong iata correction'

    reset_all_caches()
    flight_globus.complete()
    assert flight_globus.company == globus, 'Wrong iata correction'

    reset_all_caches()
    flight_s7.complete()
    assert flight_s7.company, 'Company should be found by IATA'
    assert flight_s7.company == s7, 'Wrong iata correction'


@pytest.mark.dbuser
def test_iata_correction_on_company_with_sirena_number():
    company_iata = create_company(iata=u'FI')
    company_sirena = create_company(iata=u'FI', sirena_id=u'ФЫ')

    create_iatacorrection(number=u'111', company=company_iata, code=company_iata.iata)
    create_iatacorrection(number=u'222', company=company_sirena, code=company_sirena.iata)

    flight_iata_1 = _create_flight(u'FI 111')
    flight_sirena_1 = _create_flight(u'ФЫ 111')
    flight_sirena_2 = _create_flight(u'ФЫ 222')

    reset_all_caches()
    flight_iata_1.complete()
    flight_sirena_1.complete()
    flight_sirena_2.complete()

    assert flight_iata_1.company == company_iata, 'Wrong iata correction'
    assert flight_sirena_1.company == company_iata, 'Wrong iata correction'
    assert flight_sirena_2.company == company_sirena, 'Wrong iata correction'


@pytest.mark.dbuser
@pytest.mark.parametrize('iata', [None, ''], ids=repr)
def test_company_without_codes_will_not_be_used_even_by_correction(iata):
    company = create_company(iata=iata)
    create_iatacorrection(code='NO', number=u'111', company=company)
    flight = _create_flight(u'NO 111')

    reset_all_caches()
    flight.complete()

    assert flight.number == 'NO 111', 'Used company without any iata/sirena_id/icao'


@pytest.mark.dbuser
def test_complete_flights_iata_company_if_errors_in_correction():
    """ test iata_correction with wrong pattern """
    reset_all_caches()

    company_su_1 = create_company(iata=u'SU')
    # Нужно несколько компаний с тем же кодом чтобы начали применяться коррекции
    company_su_2 = create_company(iata=u'SU')  # noqa: F841

    correction_with_bad_pattern = create_iatacorrection(
        number=u'bad [ pattern', company=company_su_1, code=company_su_1.iata)
    correction_with_bad_pattern.match_number('whatever'),  'Should silently return None if bad pattern'

    flight_su_1 = _create_flight(u'SU 123')

    flight_su_none = fill(
        IATAFlight(),
        company_iata=company_su_1.iata, number=None, station_from_iata=None, station_to_iata=None
    )

    flight_su_1.complete()
    flight_su_none.complete()

    assert flight_su_1.company, 'Company found by IATA'
    assert flight_su_none.company, 'Company found by IATA'


@pytest.mark.dbuser
def test_complete_flights_iata_company_aviacompany():
    reset_all_caches()
    company = create_company(iata=u'SU')
    avia_company = create_aviacompany(rasp_company=company)

    flight = _create_flight(u'SU 123')
    flight.complete()
    assert flight.company
    assert flight.company.id == company.id, 'Any company by iata'
    assert flight.avia_company
    assert flight.avia_company.pk == avia_company.pk


@pytest.mark.dbuser
def test_complete_flights_get_aviacompany_tariff_by_fare_code():
    reset_all_caches()
    company = create_company(iata=u'SU')
    avia_company = create_aviacompany(rasp_company=company)
    # У всякой авиакомпании создаётся тариф по умолчанию
    t_default = avia_company.tariffs.get(mask='')
    tariff_m1 = create_companytariff(mask='m1', avia_company=avia_company)
    tariff_222 = create_companytariff(mask='2+', avia_company=avia_company)

    flight_no_fare_code = _create_flight(u'SU 123')
    flight_tariff_m1 = _create_flight(u'SU 123', fare_code='m1')
    flight_tariff_222 = _create_flight(u'SU 123', fare_code='222')

    flight_no_fare_code.complete()
    flight_tariff_m1.complete()
    flight_tariff_222.complete()

    assert flight_no_fare_code.company_tariff == t_default
    assert flight_tariff_m1.company_tariff == tariff_m1
    assert flight_tariff_222.company_tariff == tariff_222


@pytest.mark.dbuser
@patch.object(Company, '__repr__', side_effect=repr_company, autospec=True)
def test_commpany_completion_with_many_messed_flights(_company_repr):
    flights = []
    letters = u'abcdefghijklmnopqrstuvwxyz'.upper()
    iatas = set('{}{}'.format(x, y) for x in letters for y in letters)
    for iata in random.sample(iatas, 50):
        company = create_company(iata=iata)
        avia_company = (create_aviacompany(rasp_company=company)
                        if random.random() < 0.7 else None)
        for number in random.sample(range(100, 1000), 200):
            for _f in range(random.randrange(10)):
                flight = _create_flight('{} {}'.format(iata, number))
                flight._test_should_be_company = company
                flight._test_should_be_avia_company = avia_company
                flights.append(flight)

    reset_all_caches()
    for f in flights:
        f.complete()
    for f in flights:
        assert f.company == f._test_should_be_company
        assert f.avia_company == f._test_should_be_avia_company
