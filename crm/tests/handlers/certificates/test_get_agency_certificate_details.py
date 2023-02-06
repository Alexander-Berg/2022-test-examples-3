from decimal import Decimal
from unittest.mock import AsyncMock

import pytest

from crm.agency_cabinet.certificates.common.structs import (
    AgencyCertificateDetails,
    DirectKPI,
    DirectCertificationCondition,
    DirectCertificationScores,
    DirectBonusPoint,
)
from crm.agency_cabinet.gateway.server.src.exceptions import AccessDenied, NotFound

pytestmark = [pytest.mark.asyncio]

# /api/agencies/{agency_id:\d+}/certificates/direct/details
URL = "/api/agencies/1234/certificates/direct/details"


@pytest.fixture(autouse=True)
def procedure(mocker):
    mock = AsyncMock()
    mock.return_value = AgencyCertificateDetails(
        agency_id=1234,
        conditions=[],
        kpis=[],
        bonus_points=[],
        scores=[],
    )

    mocker.patch(
        "crm.agency_cabinet.gateway.server.src.procedures.certificates.GetAgencyCertificatesDetails",
        return_value=mock,
    )

    return mock


async def test_calls_procedure(client, procedure):
    await client.get(URL)

    procedure.assert_awaited_with(
        yandex_uid=42,
        agency_id=1234,
    )


async def test_returns_404_if_agency_not_found(client, procedure):
    procedure.side_effect = NotFound("Agency is not found")

    got = await client.get(
        URL,
        expected_status=404,
    )

    assert got == {
        "error": {
            "error_code": "NOT_FOUND",
            "http_code": 404,
            "messages": [{"params": {}, "text": "Agency is not found"}],
        }
    }


async def test_returns_404_if_certificate_not_found(client, procedure):
    procedure.side_effect = NotFound("Certificate is not found")

    got = await client.get(
        URL,
        expected_status=404,
    )

    assert got == {
        "error": {
            "error_code": "NOT_FOUND",
            "http_code": 404,
            "messages": [{"params": {}, "text": "Certificate is not found"}],
        }
    }


async def test_returns_403_if_access_denied(client, procedure):
    procedure.side_effect = AccessDenied()

    got = await client.get(
        URL,
        expected_status=403,
    )

    assert got == {
        "error": {
            "error_code": "ACCESS_DENIED",
            "http_code": 403,
            "messages": [{"params": {}, "text": "You don't have access"}],
        }
    }


@pytest.mark.parametrize(
    "details, expected_result",
    [
        (
            AgencyCertificateDetails(
                agency_id=1234,
                conditions=[],
                kpis=[],
                bonus_points=[],
                scores=[],
            ),
            {
                "agency_id": 1234,
                "conditions": [],
                "kpis": [],
                "bonus_points": [],
                "scores": [],
            },
        ),
        (
            AgencyCertificateDetails(
                agency_id=1234,
                conditions=[
                    DirectCertificationCondition(
                        name="Договор с Яндексом",
                        threshold="-",
                        value="присутствует",
                        is_met=True,
                    )
                ],
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
                scores=[
                    DirectCertificationScores(
                        score_group="general",
                        value=Decimal("5.0"),
                        threshold=Decimal("6.0"),
                        is_met=False,
                    ),
                ],
            ),
            {
                "agency_id": 1234,
                "conditions": [
                    {
                        "is_met": True,
                        "name": "Договор с Яндексом",
                        "threshold": "-",
                        "value": "присутствует",
                    }
                ],
                "kpis": [
                    {
                        "group": "Поиск (РСЯ)",
                        "max_value": 2.0,
                        "name": "Количество чего-то где-то",
                        "value": 1.5,
                    }
                ],
                "bonus_points": [
                    {
                        "is_met": True,
                        "name": "Рекламные кейсы с Яндексом за полгода",
                        "score": 2.5,
                        "threshold": "2.0",
                        "value": "3.5",
                    }
                ],
                "scores": [
                    {
                        "score_group": "general",
                        "value": 5.0,
                        "threshold": 6.0,
                        "is_met": False,
                    }
                ],
            },
        ),
    ],
)
async def test_returns_certificate_details(details, expected_result, client, procedure):
    procedure.return_value = details
    got = await client.get(
        URL,
        expected_status=200,
    )

    assert got == expected_result
