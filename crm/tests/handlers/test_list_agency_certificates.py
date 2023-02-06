from unittest.mock import AsyncMock

import pytest

from crm.agency_cabinet.certificates.common.structs import AgencyCertificate
from crm.agency_cabinet.certificates.proto.certificates_pb2 import (
    AgencyCertificate as AgencyCertificatePB,
    AgencyCertificates as AgencyCertificatesPb,
    ListAgencyCertificatesRequest,
    ListAgencyCertificatesResponse,
)
from crm.agency_cabinet.certificates.proto.request_pb2 import RpcRequest
from smb.common.testing_utils import dt

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
            id=1338,
            project="Метрика",
            expiration_time=dt("2020-08-07 00:00:00"),
            auto_renewal_is_met=False,
        ),
    ]
    mocker.patch(
        "crm.agency_cabinet.certificates.server.lib." "handler.ListAgencyCertificates",
        return_value=mock,
    )

    return mock


async def test_calls_procedure(procedure, handler):
    request = RpcRequest(
        list_agency_certificates=ListAgencyCertificatesRequest(agency_id=1234)
    )

    await handler(request.SerializeToString())

    procedure.assert_awaited_with(agency_id=1234)


async def test_returns_empty_list_if_no_certificates_found(procedure, handler):
    procedure.return_value = []

    request = RpcRequest(
        list_agency_certificates=ListAgencyCertificatesRequest(agency_id=1234)
    )

    result = await handler(request.SerializeToString())

    assert ListAgencyCertificatesResponse.FromString(
        result
    ) == ListAgencyCertificatesResponse(
        certificates=AgencyCertificatesPb(certificates=[])
    )


async def test_returns_certificates_if_found(handler):
    request = RpcRequest(
        list_agency_certificates=ListAgencyCertificatesRequest(agency_id=1234)
    )

    result = await handler(request.SerializeToString())

    assert ListAgencyCertificatesResponse.FromString(
        result
    ) == ListAgencyCertificatesResponse(
        certificates=AgencyCertificatesPb(
            certificates=[
                AgencyCertificatePB(
                    id=1337,
                    project="Директ",
                    expiration_time=dt("2020-08-07 00:00:00", as_proto=True),
                    auto_renewal_is_met=True,
                ),
                AgencyCertificatePB(
                    id=1338,
                    project="Метрика",
                    expiration_time=dt("2020-08-07 00:00:00", as_proto=True),
                    auto_renewal_is_met=False,
                ),
            ]
        )
    )
