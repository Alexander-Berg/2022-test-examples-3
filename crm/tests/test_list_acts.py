from decimal import Decimal

import pytest
from crm.agency_cabinet.documents.proto.acts_pb2 import (
    Act as PbAct,
    ActsList as PbActsList,
    ListActsInput as PbListActsInput,
    ListActsOutput as PbListActsOutput,
)
from crm.agency_cabinet.documents.proto.request_pb2 import (
    RpcRequest as PbRpcRequest,
)
from smb.common.testing_utils import dt

from crm.agency_cabinet.documents.common.structs import Act

pytestmark = [pytest.mark.asyncio]


async def test_sends_correct_request(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = PbListActsOutput(
        acts=PbActsList(
            acts=[
                PbAct(
                    act_id=1,
                    invoice_id=1,
                    contract_id=1,
                    currency='RUR',
                    amount='66.6',
                    date=dt('2022-3-1 00:00:00', as_proto=True),
                    eid='test_eid',
                    contract_eid='test_contract_eid',
                    invoice_eid='1',
                ),
                PbAct(
                    act_id=2,
                    invoice_id=2,
                    contract_id=2,
                    currency='RUR',
                    amount='77.7',
                    date=dt('2022-8-1 00:00:00', as_proto=True),
                    eid='test_eid',
                    contract_eid='test_contract_eid',
                    invoice_eid='2',
                )
            ])
    )

    await client.list_acts(
        agency_id=22,
        invoice_id=1,
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
            list_acts=PbListActsInput(
                agency_id=22,
                invoice_id=1,
                contract_id=1,
                limit=10,
                offset=10,
                date_to=dt('2022-02-22 10:10:10', as_proto=True),
                date_from=dt('2020-09-22 10:10:10', as_proto=True),
                search_query='search_query',
            )
        ),
        response_message_type=PbListActsOutput,
    )


async def test_returns_data(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = PbListActsOutput(
        acts=PbActsList(
            acts=[
                PbAct(
                    act_id=1,
                    invoice_id=1,
                    invoice_eid='1',
                    contract_id=1,
                    currency='RUR',
                    amount='66.6',
                    date=dt('2022-3-1 00:00:00', as_proto=True),
                    eid='test_eid',
                    contract_eid='test_contract_eid',
                ),
                PbAct(
                    act_id=2,
                    invoice_id=1,
                    invoice_eid='2',
                    contract_id=1,
                    currency='RUR',
                    amount='77.7',
                    date=dt('2022-8-1 00:00:00', as_proto=True),
                    eid='test_eid',
                    contract_eid='test_contract_eid',
                )
            ])
    )

    got = await client.list_acts(
        agency_id=22,
        invoice_id=1,
        contract_id=1,
        limit=10,
        offset=10,
        date_to=dt('2022-02-22 10:10:10'),
        date_from=dt('2020-09-22 10:10:10'),
        search_query='search_query',
    )

    assert got == [
        Act(
            act_id=1,
            invoice_id=1,
            contract_id=1,
            currency='RUR',
            amount=Decimal('66.6'),
            date=dt('2022-3-1 00:00:00'),
            eid='test_eid',
            contract_eid='test_contract_eid',
            invoice_eid='1',
        ),
        Act(
            act_id=2,
            invoice_id=1,
            contract_id=1,
            currency='RUR',
            amount=Decimal('77.7'),
            date=dt('2022-8-1 00:00:00'),
            eid='test_eid',
            contract_eid='test_contract_eid',
            invoice_eid='2',
        )
    ]
