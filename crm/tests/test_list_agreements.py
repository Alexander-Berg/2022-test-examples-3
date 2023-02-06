import pytest
from crm.agency_cabinet.documents.proto.agreements_pb2 import (
    Agreement as PbAgreement,
    AgreementsList as PbAgreementsList,
    ListAgreementsInput as PbListAgreementsInput,
    ListAgreementsOutput as PbListAgreementsOutput,
)
from crm.agency_cabinet.documents.proto.request_pb2 import (
    RpcRequest as PbRpcRequest,
)
from smb.common.testing_utils import dt

from crm.agency_cabinet.documents.common.structs import Agreement

pytestmark = [pytest.mark.asyncio]


async def test_sends_correct_request(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = PbListAgreementsOutput(
        agreements=PbAgreementsList(
            agreements=[
                PbAgreement(
                    agreement_id=1,
                    name='Соглашение 1',
                    got_scan=True,
                    got_original=True,
                    date=dt('2023-4-1 00:00:00', as_proto=True),
                ),
                PbAgreement(
                    agreement_id=2,
                    name='Соглашение 2',
                    got_scan=True,
                    got_original=True,
                    date=dt('2023-4-1 00:00:00', as_proto=True),
                )
            ])
    )

    await client.list_agreements(
        agency_id=22,
        contract_id=1,
        limit=10,
        offset=10,
        date_to=dt('2022-02-22 10:10:10'),
        date_from=dt('2020-09-22 10:10:10'),
        search_query='search_query',
    )

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name='documents',
        message=PbRpcRequest(
            list_agreements=PbListAgreementsInput(
                agency_id=22,
                contract_id=1,
                limit=10,
                offset=10,
                date_to=dt('2022-02-22 10:10:10', as_proto=True),
                date_from=dt('2020-09-22 10:10:10', as_proto=True),
                search_query='search_query',
            )
        ),
        response_message_type=PbListAgreementsOutput,
    )


async def test_returns_data(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = PbListAgreementsOutput(
        agreements=PbAgreementsList(
            agreements=[
                PbAgreement(
                    agreement_id=1,
                    name='Соглашение 1',
                    got_scan=True,
                    got_original=True,
                    date=dt('2023-4-1 00:00:00', as_proto=True)
                ),
                PbAgreement(
                    agreement_id=2,
                    name='Соглашение 2',
                    got_scan=True,
                    got_original=True,
                    date=dt('2023-4-1 00:00:00', as_proto=True)
                ),
            ])
    )

    got = await client.list_agreements(
        agency_id=22,
        contract_id=1,
        limit=10,
        offset=10,
        date_to=dt('2022-02-22 10:10:10'),
        date_from=dt('2020-09-22 10:10:10'),
        search_query='search_query',
    )

    assert got == [
        Agreement(
            agreement_id=1,
            name='Соглашение 1',
            got_scan=True,
            got_original=True,
            date=dt('2023-4-1 00:00:00')
        ),
        Agreement(
            agreement_id=2,
            name='Соглашение 2',
            got_scan=True,
            got_original=True,
            date=dt('2023-4-1 00:00:00')
        ),
    ]
