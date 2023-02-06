# -*- coding: utf-8 -*-
import pytest
from mock import Mock

from travel.avia.library.python.common.models.geo import CodeSystem
from travel.avia.library.python.tester.factories import create_company, create_station, create_partner

from travel.avia.library.python.ticket_daemon.memo import reset_all_caches
from travel.avia.ticket_daemon.ticket_daemon.api.selfbook.partner_national_directions import (
    SelfBookPartnerNationalDirectionRules)


def create_airport(iata, **kwargs):
    iata_system = CodeSystem.objects.get(code='iata')
    return create_station(t_type='plane', __={'codes': {iata_system: iata}}, **kwargs)


@pytest.mark.parametrize("company_iatas", [
    (),
    ("SU",),
    ("SU", "NI"),
], ids=str)
@pytest.mark.dbuser
def test_no_rules(company_iatas):
    partner = create_partner(code="ott")
    national_version = "ru"
    [create_company(iata=c_iata) for c_iata in company_iatas]
    reset_all_caches()
    search_iata_from, search_iata_to = ("MOW", "SIN")
    ruler = SelfBookPartnerNationalDirectionRules(
        partner, national_version, search_iata_from, search_iata_to)

    print ''
    print '='*79
    print(ruler._selfbook_company_codes)

    assert not ruler.is_selfbook_applicable(variant_with_company_iatas(['ANY']))
    for ciata in company_iatas:
        assert not ruler.is_selfbook_applicable(variant_with_company_iatas([ciata]))


def variant_with_company_iatas(iatas):
    return Mock(iter_all_segments=Mock(return_value=[
        Mock(company_iata=iata) for iata in iatas
    ]))
