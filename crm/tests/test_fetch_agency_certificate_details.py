from decimal import Decimal

import pytest

from crm.agency_cabinet.certificates.client import CertificateNotFound, Client
from crm.agency_cabinet.certificates.common.structs import (
    AgencyCertificateDetails,
    DirectBonusPoint,
    DirectCertificationCondition,
    DirectCertificationScores,
    DirectKPI,
)
from crm.agency_cabinet.certificates.proto.certificates_pb2 import (
    AgencyCertificateDetails as PBAgencyCertificateDetails,
    AgencyCertificateDetailsRequest as PBAgencyCertificateDetailsRequest,
    AgencyCertificateDetailsResponse,
)
from crm.agency_cabinet.certificates.proto.direct_details_pb2 import (
    DirectBonusPoint as PBDirectBonusPoint,
    DirectCertificationCondition as PBDirectCertificationCondition,
    DirectKPI as PBDirectKPI,
    DirectScores as PBDirectScores,
)
from crm.agency_cabinet.certificates.proto.errors_pb2 import (
    CertificateNotFound as PBCertificateNotFound,
)
from crm.agency_cabinet.certificates.proto.request_pb2 import RpcRequest

pytestmark = [pytest.mark.asyncio]


async def test_sends_request(client: Client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = AgencyCertificateDetailsResponse(
        details=PBAgencyCertificateDetails(
            agency_id=1234,
        )
    )

    await client.fetch_agency_certificate_details(agency_id=1234)

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name="certificates",
        message=RpcRequest(
            fetch_agency_certificate_details=PBAgencyCertificateDetailsRequest(
                agency_id=1234
            )
        ),
        response_message_type=AgencyCertificateDetailsResponse,
    )


async def test_raises_if_certificate_not_found(client: Client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = AgencyCertificateDetailsResponse(
        certificate_not_found=PBCertificateNotFound(agency_id=1234)
    )

    with pytest.raises(CertificateNotFound) as err:
        await client.fetch_agency_certificate_details(agency_id=1234)

    assert err.value == CertificateNotFound(agency_id=1234)


async def test_returns_formatted_certificate_details_with_only_required_fields(
    client: Client, rmq_rpc_client
):
    rmq_rpc_client.send_proto_message.return_value = AgencyCertificateDetailsResponse(
        details=PBAgencyCertificateDetails(
            agency_id=1234,
        )
    )

    result = await client.fetch_agency_certificate_details(agency_id=1234)

    assert result == AgencyCertificateDetails(
        agency_id=1234,
        conditions=[],
        kpis=[],
        bonus_points=[],
        scores=[],
    )


async def test_returns_formatted_certificate_details_with_optional_fields(
    client: Client, rmq_rpc_client
):
    rmq_rpc_client.send_proto_message.return_value = AgencyCertificateDetailsResponse(
        details=PBAgencyCertificateDetails(
            agency_id=1234,
            conditions=[
                PBDirectCertificationCondition(
                    name="Договор с Яндексом",
                    threshold="-",
                    value="отсутсвует",
                    is_met=False,
                )
            ],
            kpis=[
                PBDirectKPI(
                    name="Количество чего-то где-то",
                    max_value="2.0",
                    value="0.5",
                    group="Поиск (РСЯ)",
                )
            ],
            bonus_points=[
                PBDirectBonusPoint(
                    name="Рекламные кейсы с Яндексом за полгода",
                    threshold="2.0",
                    value="3.5",
                    is_met=True,
                    score="2.5",
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

    result = await client.fetch_agency_certificate_details(agency_id=1234)

    assert result == AgencyCertificateDetails(
        agency_id=1234,
        conditions=[
            DirectCertificationCondition(
                name="Договор с Яндексом",
                threshold="-",
                value="отсутсвует",
                is_met=False,
            )
        ],
        kpis=[
            DirectKPI(
                name="Количество чего-то где-то",
                max_value=Decimal("2.0"),
                value=Decimal("0.5"),
                group="Поиск (РСЯ)",
            )
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
    )
