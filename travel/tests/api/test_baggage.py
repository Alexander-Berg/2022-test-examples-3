# -*- coding: utf-8 -*-
import pytest

from travel.avia.library.python.avia_data.models import CompanyTariff
from travel.avia.library.python.common.models.transport import TransportType
from travel.avia.library.python.tester.factories import create_aviacompany, create_company

from travel.avia.ticket_daemon.ticket_daemon.lib.baggage import Baggage


def is_sources(baggage, source):
    check_fields = ('included', 'pieces', 'weight')
    for attr in check_fields:
        actual = getattr(baggage, attr)
        if actual is not None:
            assert getattr(actual, 'source') == source
    return True


def is_partner_sources(baggage):
    return is_sources(baggage, 'partner')


def is_db_sources(baggage):
    return is_sources(baggage, 'db')


def baggage_is_equal(baggage, **kv):
    for attr, value in kv.iteritems():
        actual = getattr(baggage, attr)
        assert actual == value, '%s is not equal. Expected %s. Actual %s' % (attr, value, actual)
    return True


def test_no_info_partner_bagggae():
    baggage = Baggage.from_partner()
    assert baggage_is_equal(baggage, pieces=None, weight=None, included=None)
    assert is_partner_sources(baggage)
    assert not baggage


def test_not_included_partner_bagggae():
    baggage = Baggage.from_partner(included=False)
    assert baggage_is_equal(baggage, pieces=0, weight=None, included=0)
    assert is_partner_sources(baggage)
    assert not baggage


def test_not_included_by_pieces_partner_bagggae():
    baggage = Baggage.from_partner(0)
    assert baggage_is_equal(baggage, pieces=0, weight=None, included=0)
    assert is_partner_sources(baggage)
    assert not baggage


def test_not_included_by_weight_partner_bagggae():
    baggage = Baggage.from_partner(weight=0)
    assert baggage_is_equal(baggage, pieces=0, weight=0, included=0)
    assert is_partner_sources(baggage)
    assert not baggage


def test_included_partner_bagggae():
    baggage = Baggage.from_partner(included=True)
    assert baggage_is_equal(baggage, pieces=None, weight=None, included=1)
    assert is_partner_sources(baggage)
    assert baggage


def test_pieces_partner_bagggae():
    baggage = Baggage.from_partner(1)
    assert baggage_is_equal(baggage, pieces=1, weight=None, included=1)
    assert is_partner_sources(baggage)
    assert baggage


def test_included_pieces_and_weight_partner_bagggae():
    baggage = Baggage.from_partner(pieces=1, weight=23)
    assert baggage_is_equal(baggage, pieces=1, weight=23, included=1)
    assert is_partner_sources(baggage)
    assert baggage


def test_weight_partner_bagggage():
    baggage = Baggage.from_partner(weight=10)
    assert baggage_is_equal(baggage, pieces=1, weight=10, included=1)
    assert is_partner_sources(baggage)
    assert baggage


@pytest.mark.dbuser
def test_create_baggage_from_empty_airline_tariff():
    baggage = Baggage.from_airline_tariff(airline_tariff=None)
    assert baggage_is_equal(baggage, pieces=None, weight=None, included=None)


@pytest.mark.dbuser
def test_create_baggage_from_airline_tariff():
    company = create_company(
        t_type_id=TransportType.PLANE_ID,
        iata='hhh'
    )
    airline = create_aviacompany(
        rasp_company=company,
    )

    airline_tariff = CompanyTariff.objects.get(avia_company_id=airline.rasp_company.id)
    airline_tariff.baggage_allowed = True
    airline_tariff.baggage_norm = 99
    airline_tariff.baggage_pieces = 9
    airline_tariff.save()

    airline_tariff = CompanyTariff.objects.get(avia_company_id=airline.rasp_company.id)
    baggage = Baggage.from_airline_tariff(airline_tariff=airline_tariff)
    assert baggage_is_equal(baggage, pieces=9, weight=99, included=True)
    is_db_sources(baggage)
