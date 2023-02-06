from decimal import Decimal

import pytest
from crm.agency_cabinet.documents.proto.payments_pb2 import (
    Payment as PbPayment,
    PaymentsList as PbPaymentsList,
    ListPaymentsInput as PbListPaymentsInput,
    ListPaymentsOutput as PbListPaymentsOutput,
)
from crm.agency_cabinet.documents.proto.request_pb2 import (
    RpcRequest as PbRpcRequest,
)
from smb.common.testing_utils import dt

from crm.agency_cabinet.documents.common.structs import Payment

pytestmark = [pytest.mark.asyncio]


async def test_sends_correct_request(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = PbListPaymentsOutput(
        payments=PbPaymentsList(
            payments=[
                PbPayment(
                    payment_id=1,
                    eid='eid',
                    invoice_id=1,
                    invoice_eid='1',
                    contract_id=1,
                    currency='RUR',
                    amount='66.6',
                    date=dt('2022-3-1 00:00:00', as_proto=True),
                ),
                PbPayment(
                    payment_id=2,
                    eid='eid2',
                    invoice_id=2,
                    invoice_eid='2',
                    contract_id=2,
                    currency='RUR',
                    amount='77.7',
                    date=dt('2022-8-1 00:00:00', as_proto=True),
                )
            ])
    )

    await client.list_payments(
        agency_id=22,
        invoice_id=1,
        contract_id=1,
        limit=10,
        offset=10,
        date_to=dt('2022-02-22 10:10:10'),
        date_from=dt('2020-09-22 10:10:10'),
    )

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name='documents',
        message=PbRpcRequest(
            list_payments=PbListPaymentsInput(
                agency_id=22,
                invoice_id=1,
                contract_id=1,
                limit=10,
                offset=10,
                date_to=dt('2022-02-22 10:10:10', as_proto=True),
                date_from=dt('2020-09-22 10:10:10', as_proto=True),
            )
        ),
        response_message_type=PbListPaymentsOutput,
    )


async def test_returns_data(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = PbListPaymentsOutput(
        payments=PbPaymentsList(
            payments=[
                PbPayment(
                    payment_id=1,
                    eid='eid',
                    invoice_id=1,
                    invoice_eid='1',
                    contract_id=1,
                    currency='RUR',
                    amount='66.6',
                    date=dt('2022-3-1 00:00:00', as_proto=True),
                ),
                PbPayment(
                    payment_id=2,
                    eid='eid2',
                    invoice_id=1,
                    invoice_eid='2',
                    contract_id=1,
                    currency='RUR',
                    amount='77.7',
                    date=dt('2022-8-1 00:00:00', as_proto=True),
                )
            ])
    )

    got = await client.list_payments(
        agency_id=22,
        invoice_id=1,
        contract_id=1,
        limit=10,
        offset=10,
        date_to=dt('2022-02-22 10:10:10'),
        date_from=dt('2020-09-22 10:10:10'),
    )

    assert got == [
        Payment(
            payment_id=1,
            eid='eid',
            invoice_id=1,
            invoice_eid='1',
            contract_id=1,
            currency='RUR',
            amount=Decimal('66.6'),
            date=dt('2022-3-1 00:00:00'),
        ),
        Payment(
            payment_id=2,
            eid='eid2',
            invoice_id=1,
            invoice_eid='2',
            contract_id=1,
            currency='RUR',
            amount=Decimal('77.7'),
            date=dt('2022-8-1 00:00:00'),
        )
    ]
