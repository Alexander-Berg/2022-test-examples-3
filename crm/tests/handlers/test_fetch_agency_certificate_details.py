from decimal import Decimal
from unittest.mock import AsyncMock

import pytest

from crm.agency_cabinet.certificates.common.structs import (
    AgencyCertificateDetails,
    AgencyCertificateDetailsRequest,
    DirectBonusPoint,
    DirectCertificationCondition,
    DirectCertificationScores,
    DirectKPI,
)
from crm.agency_cabinet.certificates.proto.certificates_pb2 import (
    AgencyCertificateDetails as PBAgencyCertificateDetails,
    AgencyCertificateDetailsRequest as PBAgencyCertificateDetailsRequest,
    AgencyCertificateDetailsResponse as PBAgencyCertificateDetailsResponse,
)
from crm.agency_cabinet.certificates.proto.direct_details_pb2 import (
    DirectBonusPoint as PBDirectBonusPoint,
    DirectCertificationCondition as PBDirectCertificationCondition,
    DirectKPI as PBDirectKPI,
    DirectScores as PBDirectScores,
)
from crm.agency_cabinet.certificates.proto.errors_pb2 import CertificateNotFound
from crm.agency_cabinet.certificates.proto.request_pb2 import RpcRequest
from crm.agency_cabinet.certificates.server.lib.exceptions import (
    AgencyCertificateNotFound,
)
from crm.agency_cabinet.certificates.server.lib.handler import Handler

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def procedure(mocker):
    mock = AsyncMock()
    mocker.patch(
        "crm.agency_cabinet.certificates.server.lib."
        "handler.FetchAgencyCertificateDetails",
        return_value=mock,
    )

    return mock


async def test_calls_procedure(procedure, handler: Handler):
    procedure.return_value = AgencyCertificateDetails(
        agency_id=1234, conditions=[], bonus_points=[], kpis=[], scores=[]
    )

    request = RpcRequest(
        fetch_agency_certificate_details=PBAgencyCertificateDetailsRequest(
            agency_id=1234,
        )
    )

    await handler(request.SerializeToString())

    procedure.assert_awaited_with(
        request=AgencyCertificateDetailsRequest(agency_id=1234),
    )


async def test_returns_error_if_certificate_not_found(procedure, handler: Handler):
    procedure.side_effect = AgencyCertificateNotFound(agency_id=1234)

    request = RpcRequest(
        fetch_agency_certificate_details=PBAgencyCertificateDetailsRequest(
            agency_id=1234,
        )
    )

    result = await handler(request.SerializeToString())

    assert PBAgencyCertificateDetailsResponse.FromString(
        result
    ) == PBAgencyCertificateDetailsResponse(
        certificate_not_found=CertificateNotFound(agency_id=1234)
    )


async def test_return_certificate_details_with_no_optional_fields(
    procedure, handler: Handler
):
    procedure.return_value = AgencyCertificateDetails(
        agency_id=1234, conditions=[], bonus_points=[], kpis=[], scores=[]
    )

    request = RpcRequest(
        fetch_agency_certificate_details=PBAgencyCertificateDetailsRequest(
            agency_id=1234,
        )
    )

    result = await handler(request.SerializeToString())

    assert PBAgencyCertificateDetailsResponse.FromString(
        result
    ) == PBAgencyCertificateDetailsResponse(
        details=PBAgencyCertificateDetails(
            agency_id=1234,
            conditions=[],
            bonus_points=[],
            kpis=[],
        )
    )


async def test_return_certificate_details_with_all_fields_filled(
    procedure, handler: Handler
):
    procedure.return_value = AgencyCertificateDetails(
        agency_id=1234,
        conditions=[
            DirectCertificationCondition(
                name="Количество сер на 100 клиентов",
                value="1.0",
                threshold="2.0",
                is_met=False,
            )
        ],
        kpis=[
            DirectKPI(
                name="Количество чего-то где-то",
                group="Поиск (РСЯ)",
                value=Decimal("1.3"),
                max_value=Decimal("2.3"),
            )
        ],
        bonus_points=[
            DirectBonusPoint(
                name="Задолженность",
                value="присутствует",
                threshold="-",
                is_met=False,
                score=Decimal("-1.5"),
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

    request = RpcRequest(
        fetch_agency_certificate_details=PBAgencyCertificateDetailsRequest(
            agency_id=1234,
        )
    )

    result = await handler(request.SerializeToString())

    assert PBAgencyCertificateDetailsResponse.FromString(
        result
    ) == PBAgencyCertificateDetailsResponse(
        details=PBAgencyCertificateDetails(
            agency_id=1234,
            conditions=[
                PBDirectCertificationCondition(
                    name="Количество сер на 100 клиентов",
                    value="1.0",
                    threshold="2.0",
                    is_met=False,
                )
            ],
            bonus_points=[
                PBDirectBonusPoint(
                    name="Задолженность",
                    value="присутствует",
                    threshold="-",
                    is_met=False,
                    score="-1.5",
                )
            ],
            kpis=[
                PBDirectKPI(
                    name="Количество чего-то где-то",
                    group="Поиск (РСЯ)",
                    value="1.3",
                    max_value="2.3",
                )
            ],
            scores=[
                PBDirectScores(
                    score_group="general",
                    value="5",
                    threshold="6",
                    is_met=False,
                )
            ],
        )
    )
