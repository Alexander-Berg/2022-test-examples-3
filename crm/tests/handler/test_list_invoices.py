import pytest

from decimal import Decimal
from unittest.mock import AsyncMock

from smb.common.testing_utils import dt

from crm.agency_cabinet.documents.common.structs import invoices
from crm.agency_cabinet.documents.proto import invoices_pb2, request_pb2


pytestmark = [pytest.mark.asyncio]


@pytest.fixture(autouse=True)
def procedure(mocker):
    mock = AsyncMock()
    mock.return_value = [
        invoices.Invoice(
            invoice_id=1,
            eid='test1',
            contract_id=1234,
            contract_eid='test_contract_eid',
            amount=Decimal(12),
            currency='RUB',
            status=invoices.InvoiceStatus.paid,
            date=dt('2021-4-1 00:00:00'),
            payment_date=dt('2021-4-10 00:00:00'),
            has_facture=False,
        ),
        invoices.Invoice(
            invoice_id=2,
            eid='test2',
            contract_id=1234,
            contract_eid='test_contract_eid',
            amount=Decimal(13),
            currency='RUB',
            status=invoices.InvoiceStatus.not_paid,
            date=dt('2021-5-1 00:00:00'),
            payment_date=None,
            has_facture=False
        ),
    ]

    mocker.patch(
        "crm.agency_cabinet.documents.server.src.procedures.ListInvoices",
        return_value=mock,
    )

    return mock


async def test_calls_procedure(handler, procedure):
    input_pb = request_pb2.RpcRequest(
        list_invoices=invoices_pb2.ListInvoicesInput(
            agency_id=22,
            contract_id=1234
        )
    )

    await handler(input_pb.SerializeToString())

    procedure.assert_awaited_with(
        params=invoices.ListInvoicesInput(
            agency_id=22,
            contract_id=1234,
            date_from=None,
            date_to=None,
            status=None,
            limit=None,
            offset=None
        )
    )


async def test_returns_serialized_operation_result(handler):
    input_pb = request_pb2.RpcRequest(
        list_invoices=invoices_pb2.ListInvoicesInput(
            agency_id=22,
            contract_id=1234
        )
    )

    result = await handler(input_pb.SerializeToString())

    assert invoices_pb2.ListInvoicesOutput.FromString(result) == invoices_pb2.ListInvoicesOutput(
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
