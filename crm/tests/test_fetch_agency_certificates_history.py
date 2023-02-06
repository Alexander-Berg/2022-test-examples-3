import pytest

from crm.agency_cabinet.certificates.client import Client
from crm.agency_cabinet.certificates.common.structs import (
    AgencyCertificatesHistoryEntry,
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


@pytest.mark.parametrize("project", [None, "direct"])
async def test_sends_request(client: Client, rmq_rpc_client, project):
    rmq_rpc_client.send_proto_message.return_value = (
        FetchAgencyCertificatesHistoryResponsePb(
            certificates=AgencyCertificatesHistoryPb(certificates=[])
        )
    )

    await client.fetch_agency_certificates_history(
        agency_id=123, project=project, offset=0, limit=100
    )

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name="certificates",
        message=RpcRequest(
            fetch_agency_certificates_history=FetchAgencyCertificatesHistoryRequestPb(
                agency_id=123,
                pagination=PaginationPb(offset=0, limit=100),
                project=project,
            )
        ),
        response_message_type=FetchAgencyCertificatesHistoryResponsePb,
    )


@pytest.mark.parametrize(
    "certificates_history_proto, expected_result",
    [
        ([], []),
        (
            [
                AgencyCertificatesHistoryEntryPb(
                    id=111,
                    project="Метрика",
                    start_time=dt("2020-02-09 00:00:00", as_proto=True),
                    expiration_time=dt("2020-10-20 00:00:00", as_proto=True),
                ),
                AgencyCertificatesHistoryEntryPb(
                    id=222,
                    project="Директ",
                    start_time=dt("2020-03-09 00:00:00", as_proto=True),
                    expiration_time=dt("2020-11-20 00:00:00", as_proto=True),
                ),
            ],
            [
                AgencyCertificatesHistoryEntry(
                    id=111,
                    project="Метрика",
                    start_time=dt("2020-02-09 00:00:00"),
                    expiration_time=dt("2020-10-20 00:00:00"),
                ),
                AgencyCertificatesHistoryEntry(
                    id=222,
                    project="Директ",
                    start_time=dt("2020-03-09 00:00:00"),
                    expiration_time=dt("2020-11-20 00:00:00"),
                ),
            ],
        ),
    ],
)
async def test_returns_agency_certificates_history(
    client: Client,
    rmq_rpc_client,
    certificates_history_proto,
    expected_result,
):
    rmq_rpc_client.send_proto_message.return_value = (
        FetchAgencyCertificatesHistoryResponsePb(
            certificates=AgencyCertificatesHistoryPb(
                certificates=certificates_history_proto
            )
        )
    )

    result = await client.fetch_agency_certificates_history(
        agency_id=123, project=None, offset=0, limit=100
    )

    assert result == expected_result
