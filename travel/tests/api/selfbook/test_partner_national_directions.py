# -*- coding: utf-8 -*-
import pytest
from mock import Mock

from travel.avia.library.python.avia_data.models import NationalVersion
from travel.avia.library.python.common.models.geo import CodeSystem

from travel.avia.library.python.ticket_daemon.memo import reset_all_caches
from travel.avia.ticket_daemon.ticket_daemon.api.selfbook.partner_national_directions import (
    SelfBookPartnerNationalDirectionRules
)
from travel.avia.library.python.tester.factories import create_partner, create_company, create_station
from travel.avia.ticket_daemon.tests.api.selfbook.factories import (
    create_selfbookrule, create_selfbookpartner, create_selfbookcompany,
    create_selfbookdirection, create_selfbooknationalversion
)


def create_airport(iata, **kwargs):
    iata_system = CodeSystem.objects.get(code='iata')
    return create_station(t_type='plane', __={'codes': {iata_system: iata}}, **kwargs)


@pytest.mark.parametrize("company_iatas", [
    (),
    ("SU",),
    ("SU", "NI"),
], ids=str)
@pytest.mark.parametrize("is_exclude_companies", [False, True])
@pytest.mark.parametrize("directions", [
    [("MOW", "SIN"), ],
], ids=str)
@pytest.mark.dbuser
def test_one_rule(
    company_iatas, is_exclude_companies, directions
):
    partner = create_partner(code="ott")
    national_version = "ru"

    companies = [create_company(iata=c_iata) for c_iata in company_iatas]

    rule = create_selfbookrule(
        description="OTT rule",
        exclude_partners=False,
        exclude_companies=is_exclude_companies,
        exclude_directions=False,
    )

    nv = NationalVersion.objects.get(code='ru')
    create_selfbooknationalversion(rule=rule, national_version=nv)
    create_selfbookpartner(partner=partner, rule=rule)

    [
        create_selfbookcompany(rule=rule, company=c)
        for c in companies
    ]

    for iata_from, iata_to in directions:
        create_selfbookdirection(
            rule=rule,
            station_from=create_airport(iata=iata_from),
            station_to=create_airport(iata=iata_to),
        )

    reset_all_caches()

    search_direction = directions[0]
    search_iata_from, search_iata_to = search_direction

    ruler = SelfBookPartnerNationalDirectionRules(
        partner, national_version, search_iata_from, search_iata_to
    )

    print ''
    print '='*79
    print(ruler._selfbook_company_codes)

    if company_iatas:
        if is_exclude_companies:
            assert ruler.is_selfbook_applicable(variant_with_company_iatas(['UNKNOWN_IATA']))
            assert not ruler.is_selfbook_applicable(variant_with_company_iatas(company_iatas))
            assert not ruler.is_selfbook_applicable(variant_with_company_iatas(['UNK', company_iatas[0]]))
        else:
            assert not ruler.is_selfbook_applicable(variant_with_company_iatas(['UNKNOWN_IATA']))
            assert not ruler.is_selfbook_applicable(variant_with_company_iatas(('UNK',) + company_iatas))
            assert ruler.is_selfbook_applicable(variant_with_company_iatas(company_iatas))
    else:
        assert not ruler.is_selfbook_applicable(variant_with_company_iatas(['ANY_IATA']))


def variant_with_company_iatas(iatas):
    return Mock(iter_all_segments=Mock(return_value=[
        Mock(company_iata=iata) for iata in iatas
    ]))
