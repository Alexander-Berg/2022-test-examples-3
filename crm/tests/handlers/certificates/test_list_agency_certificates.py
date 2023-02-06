from unittest.mock import AsyncMock

import pytest

from crm.agency_cabinet.certificates.common.structs import AgencyCertificate
from crm.agency_cabinet.gateway.server.src.exceptions import AccessDenied, NotFound
from smb.common.testing_utils import dt

# /api/agencies/{agency_id:\d+}/certificates
URL = "/api/agencies/1234/certificates"

pytestmark = [pytest.mark.asyncio]


@pytest.fixture(autouse=True)
def procedure(mocker):
    mock = AsyncMock()
    mock.return_value = [
        AgencyCertificate(
            id=1337,
            project="Директ",
            expiration_time=dt("2020-08-07 00:00:00"),
            auto_renewal_is_met=True,
        ),
        AgencyCertificate(
            id=1337,
            project="Метрика",
            expiration_time=dt("2020-08-07 00:00:00"),
            auto_renewal_is_met=False,
        ),
    ]

    mocker.patch(
        "crm.agency_cabinet.gateway.server.src.procedures."
        "certificates.ListAgencyCertificates",
        return_value=mock,
    )

    return mock


async def test_calls_procedure(client, procedure):
    await client.get(URL, expected_status=200)

    procedure.assert_awaited_with(yandex_uid=42, agency_id=1234)


async def test_returns_certificates(client):
    result = await client.get(URL, expected_status=200)

    assert result == [
        {
            "id": 1337,
            "project": "Директ",
            "expiration_time": "2020-08-07T00:00:00+00:00",
            "auto_renewal_is_met": True
        },
        {
            "id": 1337,
            "project": "Метрика",
            "expiration_time": "2020-08-07T00:00:00+00:00",
            "auto_renewal_is_met": False
        },
    ]


async def test_returns_empty_list_if_no_certificates(client, procedure):
    procedure.return_value = []

    got = await client.get(URL, expected_status=200)

    assert got == []


async def test_returns_403_if_access_denied(client, procedure):
    procedure.side_effect = AccessDenied()

    got = await client.get(URL, expected_status=403)

    assert got == {
        "error": {
            "error_code": "ACCESS_DENIED",
            "http_code": 403,
            "messages": [{"params": {}, "text": "You don't have access"}],
        }
    }


async def test_returns_404_if_agency_not_found(client, procedure):
    procedure.side_effect = NotFound()

    got = await client.get(URL, expected_status=404)

    assert got == {
        "error": {
            "error_code": "NOT_FOUND",
            "http_code": 404,
            "messages": [{"params": {}, "text": "Object is not found"}],
        }
    }
