import pytest
from decimal import Decimal

from crm.agency_cabinet.documents.common.structs import DetailedInvoice, InvoiceStatus
from crm.agency_cabinet.documents.proto import invoices_pb2, request_pb2
from smb.common.testing_utils import dt

pytestmark = [pytest.mark.asyncio]


async def test_sends_correct_request(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = invoices_pb2.GetInvoiceInfoOutput(
        invoice=invoices_pb2.DetailedInvoice(
            invoice_id=1,
            eid='test1',
            contract_id=1234,
            contract_eid='test_contract_eid',
            amount='12',
            currency='RUB',
            status=invoices_pb2.InvoiceStatus.paid,
            date=dt('2021-4-1 00:00:00', as_proto=True),
            payment_date=dt('2021-4-10 00:00:00', as_proto=True),
        )
    )

    await client.get_invoice_info(agency_id=22, invoice_id=1)

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name="documents",
        message=request_pb2.RpcRequest(
            get_invoice_info=invoices_pb2.GetInvoiceInfoInput(
                agency_id=22,
                invoice_id=1,
            )
        ),
        response_message_type=invoices_pb2.GetInvoiceInfoOutput,
    )


async def test_returns_data(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = invoices_pb2.GetInvoiceInfoOutput(
        invoice=invoices_pb2.DetailedInvoice(
            invoice_id=1,
            eid='test1',
            contract_id=1234,
            contract_eid='test_contract_eid',
            amount='12',
            currency='RUB',
            status=invoices_pb2.InvoiceStatus.paid,
            date=dt('2021-4-1 00:00:00', as_proto=True),
            payment_date=dt('2021-4-10 00:00:00', as_proto=True)
        )
    )

    got = await client.get_invoice_info(agency_id=22, invoice_id=1)

    assert got == DetailedInvoice(
        invoice_id=1,
        eid='test1',
        contract_id=1234,
        contract_eid='test_contract_eid',
        amount=Decimal(12),
        currency='RUB',
        status=InvoiceStatus.paid,
        date=dt('2021-4-1 00:00:00'),
        payment_date=dt('2021-4-10 00:00:00'),
        facture=None
    )
