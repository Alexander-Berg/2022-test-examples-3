import pytest
from decimal import Decimal
from smb.common.testing_utils import dt
from crm.agency_cabinet.documents.common.structs import Invoice, InvoiceStatus
from crm.agency_cabinet.documents.proto import invoices_pb2, request_pb2


pytestmark = [pytest.mark.asyncio]


async def test_sends_correct_request(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = invoices_pb2.ListInvoicesOutput(
        invoices=invoices_pb2.InvoicesList(
            invoices=[
                invoices_pb2.Invoice(
                    invoice_id=1,
                    eid='test1',
                    contract_id=1234,
                    contract_eid='test_contract_eid',
                    amount='12',
                    currency='RUB',
                    status=invoices_pb2.InvoiceStatus.paid,
                    date=dt('2021-4-1 00:00:00', as_proto=True),
                    payment_date=dt('2021-4-10 00:00:00', as_proto=True),
                    has_facture=False
                ),
                invoices_pb2.Invoice(
                    invoice_id=2,
                    eid='test2',
                    contract_id=1234,
                    contract_eid='test_contract_eid',
                    amount='13',
                    currency='RUB',
                    status=invoices_pb2.InvoiceStatus.not_paid,
                    date=dt('2021-5-1 00:00:00', as_proto=True),
                    payment_date=None,
                    has_facture=False
                ),
            ]
        )
    )

    await client.list_invoices(agency_id=22)

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name="documents",
        message=request_pb2.RpcRequest(
            list_invoices=invoices_pb2.ListInvoicesInput(
                agency_id=22
            )
        ),
        response_message_type=invoices_pb2.ListInvoicesOutput,
    )


async def test_returns_data(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = invoices_pb2.ListInvoicesOutput(
        invoices=invoices_pb2.InvoicesList(
            invoices=[
                invoices_pb2.Invoice(
                    invoice_id=1,
                    eid='test1',
                    contract_id=1234,
                    contract_eid='test_contract_eid',
                    amount='12',
                    currency='RUB',
                    status=invoices_pb2.InvoiceStatus.paid,
                    date=dt('2021-4-1 00:00:00', as_proto=True),
                    payment_date=dt('2021-4-10 00:00:00', as_proto=True),
                    has_facture=False
                ),
                invoices_pb2.Invoice(
                    invoice_id=2,
                    eid='test2',
                    contract_id=1234,
                    contract_eid='test_contract_eid',
                    amount='13',
                    currency='RUB',
                    status=invoices_pb2.InvoiceStatus.not_paid,
                    date=dt('2021-5-1 00:00:00', as_proto=True),
                    payment_date=None,
                    has_facture=False
                ),
            ]
        )
    )

    got = await client.list_invoices(agency_id=22)

    assert got == [
        Invoice(
            invoice_id=1,
            eid='test1',
            contract_id=1234,
            contract_eid='test_contract_eid',
            amount=Decimal(12),
            currency='RUB',
            status=InvoiceStatus.paid,
            date=dt('2021-4-1 00:00:00'),
            payment_date=dt('2021-4-10 00:00:00'),
            has_facture=False,
        ),
        Invoice(
            invoice_id=2,
            eid='test2',
            contract_id=1234,
            contract_eid='test_contract_eid',
            amount=Decimal(13),
            currency='RUB',
            status=InvoiceStatus.not_paid,
            date=dt('2021-5-1 00:00:00'),
            payment_date=None,
            has_facture=False
        ),

    ]
