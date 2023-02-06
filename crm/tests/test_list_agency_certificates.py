import pytest

from crm.agency_cabinet.certificates.client import Client
from crm.agency_cabinet.certificates.common.structs import AgencyCertificate
from crm.agency_cabinet.certificates.proto.certificates_pb2 import (
    AgencyCertificate as AgencyCertificatePb,
    AgencyCertificates as AgencyCertificatesPb,
    ListAgencyCertificatesRequest as ListAgencyCertificatesRequestPb,
    ListAgencyCertificatesResponse as ListAgencyCertificatesResponsePb,
)
from crm.agency_cabinet.certificates.proto.request_pb2 import RpcRequest
from smb.common.testing_utils import dt

pytestmark = [pytest.mark.asyncio]


async def test_sends_request(client: Client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = ListAgencyCertificatesResponsePb(
        certificates=AgencyCertificatesPb(certificates=[])
    )

    await client.list_agency_certificates(agency_id=228)

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name="certificates",
        message=RpcRequest(
            list_agency_certificates=ListAgencyCertificatesRequestPb(agency_id=228)
        ),
        response_message_type=ListAgencyCertificatesResponsePb,
    )


@pytest.mark.parametrize(
    "certificates_proto, expected_result",
    [
        ([], []),
        (
            [
                AgencyCertificatePb(
                    id=1337,
                    project="Директ",
                    expiration_time=dt("2020-07-07 00:00:00", as_proto=True),
                    auto_renewal_is_met=True,
                ),
                AgencyCertificatePb(
                    id=1338,
                    project="Метрика",
                    expiration_time=dt("2020-07-07 00:00:00", as_proto=True),
                    auto_renewal_is_met=False,
                ),
            ],
            [
                AgencyCertificate(
                    id=1337,
                    project="Директ",
                    expiration_time=dt("2020-07-07 00:00:00"),
                    auto_renewal_is_met=True,
                ),
                AgencyCertificate(
                    id=1338,
                    project="Метрика",
                    expiration_time=dt("2020-07-07 00:00:00"),
                    auto_renewal_is_met=False,
                ),
            ],
        ),
    ],
)
async def test_returns_certificates(
    client: Client, rmq_rpc_client, certificates_proto, expected_result
):
    rmq_rpc_client.send_proto_message.return_value = ListAgencyCertificatesResponsePb(
        certificates=AgencyCertificatesPb(certificates=certificates_proto)
    )

    result = await client.list_agency_certificates(agency_id=228)

    assert result == expected_result
