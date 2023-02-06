from decimal import Decimal

import pytest

from crm.agency_cabinet.certificates.client import CertificateNotFound
from crm.agency_cabinet.certificates.common.structs import (
    AgencyCertificateDetails,
    DirectKPI,
    DirectCertificationCondition,
    DirectCertificationScores,
    DirectBonusPoint,
)
from crm.agency_cabinet.gateway.server.src.exceptions import AccessDenied, NotFound
from crm.agency_cabinet.gateway.server.src.procedures.certificates import (
    GetAgencyCertificatesDetails,
)
from crm.agency_cabinet.grants.common.structs import AccessLevel

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def procedure(service_discovery):
    return GetAgencyCertificatesDetails(service_discovery)


async def test_calls_external_services_for_info(
    procedure: GetAgencyCertificatesDetails, service_discovery, agency_info
):
    service_discovery.agencies.get_agencies_info.return_value = [agency_info]
    service_discovery.grants.check_access_level.return_value = AccessLevel.ALLOW

    await procedure(
        yandex_uid=4321,
        agency_id=1234,
    )

    service_discovery.agencies.get_agencies_info.assert_awaited_with(agency_ids=[1234])
    service_discovery.grants.check_access_level.assert_awaited_with(
        yandex_uid=4321, agency_id=1234
    )


async def test_raises_if_access_denied(
    procedure: GetAgencyCertificatesDetails, service_discovery, agency_info
):
    service_discovery.agencies.get_agencies_info.return_value = [agency_info]
    service_discovery.grants.check_access_level.return_value = AccessLevel.DENY

    with pytest.raises(AccessDenied):
        await procedure(yandex_uid=4321, agency_id=1234)


async def test_does_not_call_grants_if_agency_doesnt_exist(
    procedure: GetAgencyCertificatesDetails, service_discovery
):
    service_discovery.agencies.get_agencies_info.return_value = []

    with pytest.raises(NotFound):
        await procedure(yandex_uid=4321, agency_id=1234)

    service_discovery.grants.check_access_level.assert_not_called()


async def test_does_not_call_certificates_if_access_denied(
    procedure: GetAgencyCertificatesDetails, service_discovery, agency_info
):
    service_discovery.agencies.get_agencies_info.return_value = [agency_info]
    service_discovery.grants.check_access_level.return_value = AccessLevel.DENY

    with pytest.raises(AccessDenied):
        await procedure(yandex_uid=4321, agency_id=1234)

    service_discovery.certificates.fetch_agency_certificate_details.assert_not_called()


async def test_raises_if_agency_not_found(
    procedure: GetAgencyCertificatesDetails, service_discovery
):
    service_discovery.grants.check_access_level.return_value = AccessLevel.ALLOW
    service_discovery.agencies.get_agencies_info.return_value = []

    with pytest.raises(NotFound) as err:
        await procedure(yandex_uid=4321, agency_id=1234)

    assert err.value.message == "HTTP code: 404 - NOT_FOUND -> Agency is not found"


async def test_does_not_call_certificates_if_agency_not_found(
    procedure: GetAgencyCertificatesDetails, service_discovery
):
    service_discovery.grants.check_access_level.return_value = AccessLevel.ALLOW
    service_discovery.agencies.get_agencies_info.return_value = []

    with pytest.raises(NotFound):
        await procedure(yandex_uid=4321, agency_id=1234)

    service_discovery.certificates.fetch_agency_certificate_details.assert_not_called()


async def test_raises_if_certificates_not_found(
    procedure: GetAgencyCertificatesDetails, service_discovery, agency_info
):
    service_discovery.agencies.get_agencies_info.return_value = [agency_info]
    service_discovery.grants.check_access_level.return_value = AccessLevel.ALLOW
    service_discovery.certificates.fetch_agency_certificate_details.side_effect = (
        CertificateNotFound(agency_id=1234)
    )

    with pytest.raises(NotFound) as err:
        await procedure(
            yandex_uid=4321,
            agency_id=1234,
        )

    assert err.value.message == "HTTP code: 404 - NOT_FOUND -> Certificate is not found"


async def test_calls_certificates(
    procedure: GetAgencyCertificatesDetails, service_discovery, agency_info
):
    service_discovery.grants.check_access_level.return_value = AccessLevel.ALLOW
    service_discovery.agencies.get_agencies_info.return_value = [agency_info]

    await procedure(yandex_uid=4321, agency_id=1234)

    service_discovery.certificates.fetch_agency_certificate_details.assert_awaited_with(
        agency_id=1234
    )


async def test_returns_agency_certificate_details_with_only_required_fields(
    procedure: GetAgencyCertificatesDetails, service_discovery, agency_info
):
    details = AgencyCertificateDetails(
        agency_id=1234,
        conditions=[],
        kpis=[],
        bonus_points=[],
        scores=[],
    )

    service_discovery.agencies.get_agencies_info.return_value = [agency_info]
    service_discovery.grants.check_access_level.return_value = AccessLevel.ALLOW
    service_discovery.certificates.fetch_agency_certificate_details.return_value = (
        details
    )

    result = await procedure(
        yandex_uid=4321,
        agency_id=1234,
    )

    assert result == AgencyCertificateDetails(
        agency_id=1234,
        conditions=[],
        kpis=[],
        bonus_points=[],
        scores=[],
    )


async def test_returns_agency_certificate_details_with_all_optional_fields(
    procedure: GetAgencyCertificatesDetails, service_discovery, agency_info
):
    details = AgencyCertificateDetails(
        agency_id=1234,
        kpis=[
            DirectKPI(
                name="Количество чего-то где-то",
                max_value=Decimal("2.0"),
                value=Decimal("1.5"),
                group="Поиск (РСЯ)",
            ),
        ],
        bonus_points=[
            DirectBonusPoint(
                name="Рекламные кейсы с Яндексом за полгода",
                threshold="2.0",
                value="3.5",
                is_met=True,
                score=Decimal("2.5"),
            )
        ],
        conditions=[
            DirectCertificationCondition(
                name="Договор с Яндексом",
                threshold="-",
                value="присутствует",
                is_met=True,
            )
        ],
        scores=[
            DirectCertificationScores(
                score_group="general",
                value=Decimal("5.0"),
                threshold=Decimal("6.0"),
                is_met=False,
            ),
        ],
    )

    service_discovery.agencies.get_agencies_info.return_value = [agency_info]
    service_discovery.grants.check_access_level.return_value = AccessLevel.ALLOW
    service_discovery.certificates.fetch_agency_certificate_details.return_value = (
        details
    )

    result = await procedure(
        yandex_uid=4321,
        agency_id=1234,
    )

    assert result == details
