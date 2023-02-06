from datetime import datetime, timezone

import pytest

from crm.agency_cabinet.certificates.common.structs import (
    CertifiedEmployee,
    EmployeeCertificate,
    EmployeeCertificateStatus,
)
from crm.agency_cabinet.gateway.server.src.exceptions import AccessDenied
from crm.agency_cabinet.gateway.server.src.procedures.certificates import (
    ListEmployeesCertificates,
)
from crm.agency_cabinet.grants.common.structs import AccessLevel


@pytest.fixture
def procedure(service_discovery):
    return ListEmployeesCertificates(service_discovery)


@pytest.mark.parametrize(
    "certificates",
    [
        [],
        [
            CertifiedEmployee(
                name="Альпак Альпакыч",
                email="alpaca@yandex.ru",
                agency_id=8765,
                certificates=[
                    EmployeeCertificate(
                        project="Дзен",
                        start_time=datetime(2020, 1, 6, 18, 0, 0, tzinfo=timezone.utc),
                        expiration_time=datetime(
                            2020, 2, 6, 18, 0, 0, tzinfo=timezone.utc
                        ),
                        external_id="some_id",
                        status=EmployeeCertificateStatus.EXPIRED
                    ),
                    EmployeeCertificate(
                        project="Директ",
                        start_time=datetime(2020, 9, 6, 18, 0, 0, tzinfo=timezone.utc),
                        expiration_time=datetime(
                            2020, 10, 6, 18, 0, 0, tzinfo=timezone.utc
                        ),
                        external_id="another_id",
                        status=EmployeeCertificateStatus.EXPIRES_IN_SEMIYEAR,
                    ),
                ],
            ),
            CertifiedEmployee(
                name=None,
                email="capibara@yandex.ru",
                agency_id=8765,
                certificates=[
                    EmployeeCertificate(
                        project="Директ",
                        start_time=datetime(2020, 7, 1, 18, 0, 0, tzinfo=timezone.utc),
                        expiration_time=datetime(
                            2020, 8, 1, 18, 0, 0, tzinfo=timezone.utc
                        ),
                        external_id="very_id",
                        status=EmployeeCertificateStatus.ACTIVE,
                    ),
                ],
            ),
        ],
    ],
)
async def test_returns_employees_certificates(
    certificates, procedure, service_discovery
):
    service_discovery.grants.check_access_level.return_value = AccessLevel.ALLOW
    service_discovery.certificates.list_employees_certificates.return_value = (
        certificates
    )

    got = await procedure(
        yandex_uid=123,
        agency_id=8765,
        offset=0,
        limit=100,
        project=None,
        search_query=None,
        status=None,
    )

    assert got == certificates


async def test_calls_other_services_for_info(procedure, service_discovery):
    service_discovery.grants.check_access_level.return_value = AccessLevel.ALLOW

    await procedure(
        yandex_uid=123,
        agency_id=8765,
        offset=0,
        limit=100,
        project=None,
        search_query=None,
        status=None,
    )

    service_discovery.grants.check_access_level.assert_called_with(
        yandex_uid=123, agency_id=8765
    )
    service_discovery.certificates.list_employees_certificates.assert_called_with(
        agency_id=8765,
        offset=0,
        limit=100,
        project=None,
        search_query=None,
        status=None,
    )


async def test_raises_if_access_denied(procedure, service_discovery):
    service_discovery.grants.check_access_level.return_value = AccessLevel.DENY

    with pytest.raises(AccessDenied):
        await procedure(
            yandex_uid=123,
            agency_id=8765,
            offset=0,
            limit=100,
            project=None,
            search_query=None,
            status=None,
        )


async def test_does_not_call_certificates_if_access_denied(
    procedure, service_discovery
):
    service_discovery.grants.check_access_level.return_value = AccessLevel.DENY

    with pytest.raises(AccessDenied):
        await procedure(
            yandex_uid=123,
            agency_id=8765,
            offset=0,
            limit=100,
            project=None,
            search_query=None,
            status=None,
        )

    service_discovery.certificates.list_employees_certificates.assert_not_called()
