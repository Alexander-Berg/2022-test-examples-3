from unittest.mock import AsyncMock

import pytest

from crm.agency_cabinet.certificates.common.structs import (
    AgencyCertificatesHistoryEntry,
)
from crm.agency_cabinet.gateway.server.src.exceptions import (
    AccessDenied,
    NotFound,
)
from smb.common.testing_utils import dt

# /api/agencies/{agency_id:\d+}/certificates/history
URL = "/api/agencies/1234/certificates/history"

pytestmark = [pytest.mark.asyncio]


@pytest.fixture(autouse=True)
def procedure(mocker):
    mock = AsyncMock()
    mock.return_value = [
        AgencyCertificatesHistoryEntry(
            id=1337,
            project="Директ",
            start_time=dt("2020-01-07 00:00:00"),
            expiration_time=dt("2020-08-07 00:00:00"),
        ),
        AgencyCertificatesHistoryEntry(
            id=1338,
            project="Метрика",
            start_time=dt("2020-01-07 00:00:00"),
            expiration_time=dt("2020-08-07 00:00:00"),
        ),
    ]

    mocker.patch(
        "crm.agency_cabinet.gateway.server.src.procedures."
        "certificates.FetchAgencyCertificatesHistory",
        return_value=mock,
    )

    return mock


@pytest.mark.parametrize(
    "params",
    (
        dict(offset=0, limit=10),
        dict(project="direct", offset=30, limit=20),
    ),
)
async def test_calls_procedure(client, procedure, params):
    await client.get(
        URL,
        params=params,
        expected_status=200,
    )

    procedure.assert_awaited_with(
        **dict(
            yandex_uid=42,
            agency_id=1234,
            limit=params["limit"],
            offset=params["offset"],
            project=params.get("project"),
        )
    )


async def test_returns_certificates_history(client):
    result = await client.get(
        URL, params=dict(offset=0, limit=100), expected_status=200
    )

    assert result == [
        {
            "certificate_id": 1337,
            "project": "Директ",
            "start_time": "2020-01-07T00:00:00+00:00",
            "expiration_time": "2020-08-07T00:00:00+00:00",
        },
        {
            "certificate_id": 1338,
            "project": "Метрика",
            "start_time": "2020-01-07T00:00:00+00:00",
            "expiration_time": "2020-08-07T00:00:00+00:00",
        },
    ]


async def test_returns_empty_list_if_no_certificates_history(client, procedure):
    procedure.return_value = []

    got = await client.get(URL, params=dict(offset=0, limit=100), expected_status=200)

    assert got == []


async def test_returns_403_if_access_denied(client, procedure):
    procedure.side_effect = AccessDenied()

    got = await client.get(URL, params=dict(offset=0, limit=100), expected_status=403)

    assert got == {
        "error": {
            "error_code": "ACCESS_DENIED",
            "http_code": 403,
            "messages": [{"params": {}, "text": "You don't have access"}],
        }
    }


async def test_returns_404_if_agency_not_found(client, procedure):
    procedure.side_effect = NotFound()

    got = await client.get(URL, params=dict(offset=0, limit=100), expected_status=404)

    assert got == {
        "error": {
            "error_code": "NOT_FOUND",
            "http_code": 404,
            "messages": [{"params": {}, "text": "Object is not found"}],
        }
    }


@pytest.mark.parametrize(
    ("params", "expected_error"),
    (
        (
            dict(offset=-1, limit=10),
            "offset: Must be greater than or equal to 0.",
        ),
        (
            dict(offset=0, limit=0),
            "limit: Must be greater than or equal to 1 and less than or equal to 100.",
        ),
        (
            dict(offset=0, limit=101),
            "limit: Must be greater than or equal to 1 and less than or equal to 100.",
        ),
        (
            dict(offset=0, limit=10, project=""),
            "project: Shorter than minimum length 1.",
        ),
    ),
)
async def test_returns_422_if_unprocessable_input(
    client, procedure, params, expected_error
):
    got = await client.get(
        URL,
        params=params,
        expected_status=422,
    )

    assert got == {
        "error": {
            "error_code": "VALIDATION_ERROR",
            "http_code": 422,
            "messages": [{"params": {}, "text": expected_error}],
        }
    }


async def test_returns_certificates_history_if_extra_params_given(client):
    result = await client.get(
        URL, params=dict(offset=0, limit=100, srcwr="abacaba"), expected_status=200
    )

    assert result == [
        {
            "certificate_id": 1337,
            "project": "Директ",
            "start_time": "2020-01-07T00:00:00+00:00",
            "expiration_time": "2020-08-07T00:00:00+00:00",
        },
        {
            "certificate_id": 1338,
            "project": "Метрика",
            "start_time": "2020-01-07T00:00:00+00:00",
            "expiration_time": "2020-08-07T00:00:00+00:00",
        },
    ]
