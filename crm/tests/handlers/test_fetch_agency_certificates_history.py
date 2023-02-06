from unittest.mock import AsyncMock

import pytest

from crm.agency_cabinet.certificates.common.structs import (
    AgencyCertificatesHistoryEntry,
    FetchAgencyCertificatesHistoryRequest,
    Pagination,
)
from crm.agency_cabinet.certificates.proto.certificates_pb2 import (
    AgencyCertificatesHistory as AgencyCertificatesHistoryPb,
    AgencyCertificatesHistoryEntry as AgencyCertificatesHistoryEntryPb,
    FetchAgencyCertificatesHistoryRequest as FetchAgencyCertificatesHistoryRequestPb,
    FetchAgencyCertificatesHistoryResponse as FetchAgencyCertificatesHistoryResponsePb,
)
from crm.agency_cabinet.certificates.proto.common_pb2 import Pagination as PaginationPb
from crm.agency_cabinet.certificates.proto.request_pb2 import RpcRequest
from smb.common.testing_utils import dt

pytestmark = [pytest.mark.asyncio]


@pytest.fixture(autouse=True)
def procedure(mocker):
    mock = AsyncMock()
    mock.return_value = [
        AgencyCertificatesHistoryEntry(
            id=1337,
            project="Директ",
            start_time=dt("2020-01-01 00:00:00"),
            expiration_time=dt("2020-08-08 00:00:00"),
        ),
        AgencyCertificatesHistoryEntry(
            id=1338,
            project="Метрика",
            start_time=dt("2020-01-01 00:00:00"),
            expiration_time=dt("2020-08-07 00:00:00"),
        ),
    ]
    mocker.patch(
        "crm.agency_cabinet.certificates.server.lib."
        "handler.FetchAgencyCertificatesHistory",
        return_value=mock,
    )

    return mock


@pytest.mark.parametrize(
    ("params", "expected"),
    (
        (dict(), dict(project=None)),
        (dict(project=None), dict(project=None)),
        (dict(project=""), dict(project="")),
        (dict(project="direct"), dict(project="direct")),
    ),
)
async def test_calls_procedure(procedure, handler, params, expected):
    request = RpcRequest(
        fetch_agency_certificates_history=FetchAgencyCertificatesHistoryRequestPb(
            agency_id=1234, pagination=PaginationPb(limit=100, offset=0), **params
        )
    )

    await handler(request.SerializeToString())

    procedure.assert_awaited_with(
        request=FetchAgencyCertificatesHistoryRequest(
            agency_id=1234, pagination=Pagination(limit=100, offset=0), **expected
        )
    )


async def test_returns_empty_list_if_no_certificates_history_found(procedure, handler):
    procedure.return_value = []

    request = RpcRequest(
        fetch_agency_certificates_history=FetchAgencyCertificatesHistoryRequestPb(
            agency_id=228, pagination=PaginationPb(limit=100, offset=0)
        )
    )

    result = await handler(request.SerializeToString())

    assert FetchAgencyCertificatesHistoryResponsePb.FromString(
        result
    ) == FetchAgencyCertificatesHistoryResponsePb(
        certificates=AgencyCertificatesHistoryPb(certificates=[])
    )


async def test_returns_certificates_history_if_found(handler):
    request = RpcRequest(
        fetch_agency_certificates_history=FetchAgencyCertificatesHistoryRequestPb(
            agency_id=1234, pagination=PaginationPb(limit=100, offset=0)
        )
    )

    result = await handler(request.SerializeToString())

    assert FetchAgencyCertificatesHistoryResponsePb.FromString(
        result
    ) == FetchAgencyCertificatesHistoryResponsePb(
        certificates=AgencyCertificatesHistoryPb(
            certificates=[
                AgencyCertificatesHistoryEntryPb(
                    id=1337,
                    project="Директ",
                    start_time=dt("2020-01-01 00:00:00", as_proto=True),
                    expiration_time=dt("2020-08-08 00:00:00", as_proto=True),
                ),
                AgencyCertificatesHistoryEntryPb(
                    id=1338,
                    project="Метрика",
                    start_time=dt("2020-01-01 00:00:00", as_proto=True),
                    expiration_time=dt("2020-08-07 00:00:00", as_proto=True),
                ),
            ]
        )
    )
