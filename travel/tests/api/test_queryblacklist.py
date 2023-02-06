# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from datetime import datetime, timedelta

import pytest
from mock import Mock

from travel.avia.ticket_daemon.ticket_daemon.api.flights import Variant, IATAFlight
from travel.avia.ticket_daemon.ticket_daemon.api.result.filtering import FiltersApplier
from travel.avia.library.python.tester.factories import (
    create_partner, create_settlement, create_station, create_company,
    create_currency
)
from travel.avia.library.python.ticket_daemon.memo import reset_all_caches
from travel.avia.ticket_daemon.ticket_daemon.lib.currency import Price

from travel.avia.ticket_daemon.tests.api.factories import create_queryblacklist


def _create_variant(partner, forward_segments):
    variant = Variant()
    variant.partner = partner
    variant.partner_code = partner.code
    variant.forward.segments = []

    for number, company, local_departure in forward_segments:
        flight = IATAFlight()
        flight.number = number
        flight.company = company
        flight.local_departure = local_departure
        variant.forward.segments.append(flight)

    return variant


def _variant_allowed(v, national_version):
    __tracebackhide__ = True
    query = Mock()
    query.national_version = national_version
    filters_applier = FiltersApplier(query, v.partner, {'RUR': 1.0, 'UAH': 1.0})
    allowed_variants = filters_applier._blacklisted_filter_applier([v])

    return list(allowed_variants) == [v]


@pytest.mark.dbuser
@pytest.mark.parametrize(('price', 'suitable', 'national_version'), [
    (Price(100, currency='RUR'), True, 'ru'),
    (Price(98, currency='RUR'), False, 'ru'),
    (Price(2, currency='EUR'), True, 'com'),
    (Price(1, currency='EUR'), False, 'com'),
])
def test_too_small_price_filter(price, suitable, national_version):
    __tracebackhide__ = True
    partner = create_partner(code='one')
    company = create_company(iata='SU')
    variant = _create_variant(partner, [('SU 132', company, datetime.now()), ])
    for seg in variant.iter_all_segments():
        seg.station_from = 'c213'
        seg.station_to = 'c2'
    variant.tariff = price
    variant.national_tariff = price
    query = Mock()
    query.national_version = national_version
    query.date_forward = datetime.now().date()
    query.date_backward = None
    filters_applier = FiltersApplier(query, variant.partner, {'RUR': 1.0, 'EUR': 85.0})
    actual = list(filters_applier._check_bad([variant]))
    if suitable:
        assert actual == [variant]
    else:
        assert actual == []


@pytest.mark.dbuser
def test_filter_blacklisted_variants_without_rules_allowed():
    partner = create_partner(code='one')
    company = create_company(iata='SU')
    variant = _create_variant(partner, [('SU 132', company, datetime.now()), ])
    reset_all_caches()
    assert _variant_allowed(variant, national_version='ru')


@pytest.mark.dbuser
@pytest.mark.parametrize('rule_flight_number', ['', None, '132'])
def test_price_from(rule_flight_number):
    partner = create_partner(code='one')
    company = create_company(iata='SU')
    RUR = 'RUR'

    variant_9 = _create_variant(partner, [('SU 132', company, datetime.now()), ])
    variant_9.tariff = Price(9, RUR)

    variant_3 = _create_variant(partner, [('SU 132', company, datetime.now()), ])
    variant_3.tariff = Price(3, RUR)

    currency = create_currency(code=RUR)
    create_queryblacklist(
        active_to=datetime.now().date() + timedelta(1),
        price_from=4,
        flight_number=rule_flight_number,
        currency=currency,
        national_version='ru',
        active=True
    )
    reset_all_caches()
    assert _variant_allowed(variant_3, national_version='ru')
    assert not _variant_allowed(variant_9, national_version='ru')


@pytest.mark.dbuser
@pytest.mark.parametrize('rule_flight_number', ['', None, '132'])
def test_price_to(rule_flight_number):
    partner = create_partner(code='one')
    company = create_company(iata='SU')
    RUR = 'RUR'

    variant_9 = _create_variant(partner, [('SU 132', company, datetime.now()), ])
    variant_9.tariff = Price(9, RUR)

    variant_3 = _create_variant(partner, [('SU 132', company, datetime.now()), ])
    variant_3.tariff = Price(3, RUR)

    currency = create_currency(code=RUR)
    create_queryblacklist(
        active_to=datetime.now().date() + timedelta(1),
        price_to=4,
        flight_number=rule_flight_number,
        currency=currency,
        national_version='ru',
        active=True
    )
    reset_all_caches()
    assert not _variant_allowed(variant_3, national_version='ru')
    assert _variant_allowed(variant_9, national_version='ru')


@pytest.mark.dbuser
def test_filter_blacklisted_variants_allow_even_if_unrelated_blackrule_with_same_national_version():
    partner = create_partner(code='one')
    company = create_company(iata='SU')
    variant = _create_variant(partner, [('SU 132', company, datetime.now()), ])
    create_queryblacklist(
        partner=partner,
        company=create_company(iata='NO'),
        active_to=datetime.now().date() + timedelta(1),
        national_version='ru',
        active=True
    )
    reset_all_caches()
    assert _variant_allowed(variant, national_version='ru')


@pytest.mark.dbuser
def test_filter_blacklisted_variants_deny_with_blackrule_with_partner():
    partner = create_partner(code='one')
    company = create_company(iata='SU')
    variant = _create_variant(partner, [('SU 132', company, datetime.now()), ])
    create_queryblacklist(
        company=company,
        partner=partner,
        active_to=datetime.now().date() + timedelta(1),
        active=True
    )
    reset_all_caches()
    assert not _variant_allowed(variant, national_version='ru')


@pytest.mark.dbuser
def test_filter_blacklisted_variants_deny_with_blackrule_without_partner():
    partner = create_partner(code='one')
    company = create_company(iata='SU')
    variant = _create_variant(partner, [('SU 132', company, datetime.now()), ])
    create_queryblacklist(
        company=company,
        active_to=datetime.now().date() + timedelta(1),
        active=True
    )
    reset_all_caches()
    assert not _variant_allowed(variant, national_version='ru')


@pytest.mark.dbuser
def test_filter_blacklisted_variants_deny_even_if_unrelated_blackrule_with_other_national_version():
    partner = create_partner(code='one')
    company = create_company(iata='SU')
    variant = _create_variant(partner, [('SU 132', company, datetime.now()), ])
    create_queryblacklist(
        allow=True,
        partner=partner,
        company=create_company(iata='NO'),
        active_to=datetime.now().date() + timedelta(1),
        national_version='ru',
        active=True
    )
    reset_all_caches()
    assert not _variant_allowed(variant, national_version='ua'), (
        'Если создать allow правило для одной национальной версии, то в другой варианты должны запрещаться')


@pytest.mark.dbuser
def test_filter_blacklisted_variants_allowed_without_related_blackrule():
    partner = create_partner(code='one')
    company = create_company(iata='SU')
    variant = _create_variant(partner, [('SU 132', company, datetime.now()), ])
    today = datetime.now().date()
    tomorrow = today + timedelta(1)
    create_queryblacklist(partner=create_partner(code='other'), active_to=tomorrow, active=True)
    create_queryblacklist(partner=partner, company=create_company(iata='NO'), active_to=tomorrow, active=True)
    create_queryblacklist(partner=partner, company=company, active_to=today - timedelta(1), active=False)
    reset_all_caches()
    assert _variant_allowed(variant, national_version='ru')


@pytest.mark.dbuser
def test_filter_blacklisted_variants_allowed_with_related_whiterule():
    partner = create_partner(code='one')
    company = create_company(iata='SU')
    variant = _create_variant(partner, [('SU 132', company, datetime.now()), ])
    tomorrow = datetime.now().date() + timedelta(1)
    create_queryblacklist(
        allow=True,
        partner=create_partner(code='other'),
        active_to=tomorrow,
        active=True
    )
    reset_all_caches()
    assert _variant_allowed(variant, national_version='ru')


@pytest.mark.dbuser
def test_filter_blacklisted_variants_denied_with_related_blackrule_settlement_from():
    partner = create_partner(code='one')
    company = create_company(iata='SU')
    sip = create_settlement(iata='SIP')
    dme = create_station(settlement=sip)
    variant = _create_variant(partner, [('SU 132', company, datetime.now()), ])
    variant.forward.segments[0].station_from = dme
    tomorrow = datetime.now().date() + timedelta(1)
    create_queryblacklist(
        allow=False,
        settlement_from=sip,
        active_to=tomorrow,
        active=True
    )
    reset_all_caches()
    assert not _variant_allowed(variant, national_version='ru')


@pytest.mark.dbuser
def test_filter_blacklisted_variants_denied_with_related_blackrule_station_from():
    partner = create_partner(code='one')
    company = create_company(iata='SU')
    dme = create_station()
    variant = _create_variant(partner, [('SU 132', company, datetime.now()), ])
    variant.forward.segments[0].station_from = dme
    tomorrow = datetime.now().date() + timedelta(1)
    create_queryblacklist(
        allow=False,
        station_from=dme,
        active_to=tomorrow,
        active=True
    )
    reset_all_caches()
    assert not _variant_allowed(variant, national_version='ru')


@pytest.mark.dbuser
def test_filter_blacklisted_variants_denied_with_related_blackrule_company():
    partner = create_partner(code='one')
    company = create_company(iata='SU')
    variant = _create_variant(partner, [('SU 132', company, datetime.now()), ])
    tomorrow = datetime.now().date() + timedelta(1)
    create_queryblacklist(
        allow=False,
        company=company,
        active_to=tomorrow,
        active=True
    )
    reset_all_caches()
    assert not _variant_allowed(variant, national_version='ru')


@pytest.mark.dbuser
def test_settlement_to_settlement_company():
    partner = create_partner(code='one')
    company = create_company(iata='SU')

    sip = create_settlement(iata='SIP')
    dme = create_station(settlement=sip)

    vena = create_settlement(sirena_id='ВЕН')
    vie = create_station(settlement=vena)

    variant = _create_variant(partner, [('SU 132', company, datetime.now()), ])
    variant.forward.segments[0].station_from = dme
    variant.forward.segments[0].station_to = vie

    create_queryblacklist(
        allow=False,
        settlement_from=sip,
        settlement_to=vena,
        company=company,
        active_to=datetime.now().date() + timedelta(1),
        active=True
    )
    reset_all_caches()
    assert not _variant_allowed(variant, national_version='ru')


@pytest.mark.dbuser
def test_filter_blacklisted_variants_denied_with_related_whiterule_other_company():
    partner = create_partner(code='one')
    company = create_company(iata='SU')
    variant = _create_variant(partner, [('SU 132', company, datetime.now()), ])
    tomorrow = datetime.now().date() + timedelta(1)
    create_queryblacklist(
        allow=True,
        partner=partner,
        company=create_company(iata='NO'),
        active_to=tomorrow,
        active=True
    )
    reset_all_caches()
    assert not _variant_allowed(variant, national_version='ru')


@pytest.mark.dbuser
def test_filter_blacklisted_variants_allowed_with_deactivated_unrelated_whiterule_other_company():
    partner = create_partner(code='one')
    company = create_company(iata='SU')
    variant = _create_variant(partner, [('SU 132', company, datetime.now()), ])
    today = datetime.now().date()
    create_queryblacklist(
        allow=True,
        partner=partner,
        company=create_company(iata='NO'),
        active_to=today - timedelta(1),
        active=False
    )
    reset_all_caches()
    assert _variant_allowed(variant, national_version='ru')
