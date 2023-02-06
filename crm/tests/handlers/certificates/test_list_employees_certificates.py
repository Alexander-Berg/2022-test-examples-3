from datetime import datetime, timezone
from unittest.mock import AsyncMock

import pytest

from crm.agency_cabinet.certificates.common.structs import (
    CertifiedEmployee,
    EmployeeCertificate,
    EmployeeCertificateStatus,
)
from crm.agency_cabinet.gateway.server.src.exceptions import AccessDenied

# /api/agencies/{agency_id:\d+}/employees/certificates
URL = "/api/agencies/8765/employees/certificates"


@pytest.fixture(autouse=True)
def procedure(mocker):
    mock = AsyncMock()
    mock.return_value = [
        CertifiedEmployee(
            name="Альпак Альпакыч",
            email="alpaca@yandex.ru",
            agency_id=8765,
            certificates=[
                EmployeeCertificate(
                    project="Дзен",
                    start_time=datetime(2020, 1, 6, 18, 0, 0, tzinfo=timezone.utc),
                    expiration_time=datetime(2020, 2, 6, 18, 0, 0, tzinfo=timezone.utc),
                    external_id="some_id",
                    status=EmployeeCertificateStatus.EXPIRED,
                ),
                EmployeeCertificate(
                    project="Директ",
                    start_time=datetime(2020, 9, 1, 18, 0, 0, tzinfo=timezone.utc),
                    expiration_time=datetime(
                        2020, 10, 6, 18, 0, 0, tzinfo=timezone.utc
                    ),
                    external_id="another_id",
                    status=EmployeeCertificateStatus.ACTIVE,
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
                    expiration_time=datetime(2020, 8, 1, 18, 0, 0, tzinfo=timezone.utc),
                    external_id="very_id",
                    status=EmployeeCertificateStatus.EXPIRES_IN_SEMIYEAR,
                ),
            ],
        ),
    ]

    mocker.patch(
        "crm.agency_cabinet.gateway.server.src.procedures.certificates.ListEmployeesCertificates",
        return_value=mock,
    )

    return mock


async def test_returns_employees_certificates(client):
    got = await client.get(URL, params=dict(offset=0, limit=100), expected_status=200)

    assert got == [
        {
            "name": "Альпак Альпакыч",
            "email": "alpaca@yandex.ru",
            "agency_id": 8765,
            "certificates": [
                {
                    "project": "Дзен",
                    "start_time": "2020-01-06T18:00:00+00:00",
                    "expiration_time": "2020-02-06T18:00:00+00:00",
                    "external_id": "some_id",
                    "status": "expired",
                },
                {
                    "project": "Директ",
                    "start_time": "2020-09-01T18:00:00+00:00",
                    "expiration_time": '2020-10-06T18:00:00+00:00',
                    "external_id": "another_id",
                    "status": "active",
                },
            ],
        },
        {
            "name": None,
            "email": "capibara@yandex.ru",
            "agency_id": 8765,
            "certificates": [
                {
                    "project": "Директ",
                    "start_time": "2020-07-01T18:00:00+00:00",
                    "expiration_time": '2020-08-01T18:00:00+00:00',
                    "external_id": "very_id",
                    "status": "expires_in_semiyear",
                },
            ],
        },
    ]


async def test_returns_empty_list_if_nothing_found(client, procedure):
    procedure.return_value = []

    got = await client.get(URL, params=dict(offset=0, limit=100), expected_status=200)

    assert got == []


@pytest.mark.parametrize(
    "params",
    (
        dict(offset=0, limit=10),
        dict(project="direct", offset=30, limit=20),
    ),
)
async def test_calls_procedure(client, procedure, params):
    await client.get(URL, params=params, expected_status=200)

    procedure.assert_called_with(
        yandex_uid=42,
        agency_id=8765,
        offset=params["offset"],
        limit=params["limit"],
        project=params.get("project"),
        search_query=params.get("search_query"),
        status=params.get("status"),
    )


async def test_handler_returns_403_if_access_denied(client, procedure):
    procedure.side_effect = AccessDenied()

    got = await client.get(URL, params=dict(offset=0, limit=100), expected_status=403)

    assert got == {
        "error": {
            "error_code": "ACCESS_DENIED",
            "http_code": 403,
            "messages": [{"params": {}, "text": "You don't have access"}],
        }
    }
