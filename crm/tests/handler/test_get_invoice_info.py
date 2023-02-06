import datetime
import pytest
from decimal import Decimal
from unittest.mock import AsyncMock

from smb.common.testing_utils import dt

from crm.agency_cabinet.documents.proto import invoices_pb2, request_pb2
from crm.agency_cabinet.documents.common.structs.invoices import InvoiceStatus, DetailedInvoice, Facture, GetInvoiceInfoInput


pytestmark = [pytest.mark.asyncio]


@pytest.fixture()
def procedure(mocker):
    mock = AsyncMock()
    mock.return_value = DetailedInvoice(
        invoice_id=1,
        eid='invoice_eid',
        contract_id=123,
        contract_eid='test_contract_eid',
        amount=Decimal('50'),
        currency='RUR',
        status=InvoiceStatus.paid,
        date=datetime.datetime(2021, 3, 1, 0, 0, tzinfo=datetime.timezone.utc),
        payment_date=datetime.datetime(2022, 4, 1, 0, 0, tzinfo=datetime.timezone.utc),
        facture=Facture(
            facture_id=1,
            amount=Decimal('50'),
            amount_with_nds=Decimal('60'),
            nds=Decimal('10'),
            date=datetime.datetime(2022, 3, 1, 0, 0, tzinfo=datetime.timezone.utc),
            currency='RUR'
        )
    )

    mocker.patch(
        "crm.agency_cabinet.documents.server.src.procedures.GetInvoiceInfo",
        return_value=mock,
    )

    return mock


@pytest.fixture()
def procedure_invoice_without_facture(mocker):
    mock = AsyncMock()
    mock.return_value = DetailedInvoice(
        invoice_id=1,
        eid='invoice_eid',
        contract_id=123,
        contract_eid='test_contract_eid',
        amount=Decimal('50'),
        currency='RUR',
        status=InvoiceStatus.paid,
        date=datetime.datetime(2021, 3, 1, 0, 0, tzinfo=datetime.timezone.utc),
        payment_date=None,
        facture=None
    )

    mocker.patch(
        "crm.agency_cabinet.documents.server.src.procedures.GetInvoiceInfo",
        return_value=mock,
    )

    return mock


async def test_calls_procedure(handler, procedure):
    input_pb = request_pb2.RpcRequest(
        get_invoice_info=invoices_pb2.GetInvoiceInfoInput(
            agency_id=22,
            invoice_id=1,
        )
    )

    await handler(input_pb.SerializeToString())

    procedure.assert_awaited_with(
        params=GetInvoiceInfoInput(
            agency_id=22,
            invoice_id=1,
        )
    )


async def test_returns_serialized_operation_result(handler, procedure):
    input_pb = request_pb2.RpcRequest(
        get_invoice_info=invoices_pb2.GetInvoiceInfoInput(
            agency_id=22,
            invoice_id=1,
        )
    )

    result = await handler(input_pb.SerializeToString())

    assert invoices_pb2.GetInvoiceInfoOutput.FromString(result) == invoices_pb2.GetInvoiceInfoOutput(
        invoice=invoices_pb2.DetailedInvoice(
            invoice_id=1,
            eid='invoice_eid',
            contract_id=123,
            contract_eid='test_contract_eid',
            amount='50',
            currency='RUR',
            status=invoices_pb2.InvoiceStatus.paid,
            date=dt('2021-3-1 00:00:00', as_proto=True),
            payment_date=dt('2022-4-1 00:00:00', as_proto=True),
            facture=invoices_pb2.Facture(
                facture_id=1,
                amount='50',
                amount_with_nds='60',
                nds='10',
                date=dt('2022-3-1 00:00:00', as_proto=True),
                currency='RUR'
            )
        )
    )


async def test_returns_serialized_operation_result_without_facture(handler, procedure_invoice_without_facture):
    input_pb = request_pb2.RpcRequest(
        get_invoice_info=invoices_pb2.GetInvoiceInfoInput(
            agency_id=22,
            invoice_id=1,
        )
    )

    result = await handler(input_pb.SerializeToString())

    assert invoices_pb2.GetInvoiceInfoOutput.FromString(result) == invoices_pb2.GetInvoiceInfoOutput(
        invoice=invoices_pb2.DetailedInvoice(
            invoice_id=1,
            eid='invoice_eid',
            contract_id=123,
            contract_eid='test_contract_eid',
            amount='50',
            currency='RUR',
            status=invoices_pb2.InvoiceStatus.paid,
            date=dt('2021-3-1 00:00:00', as_proto=True),
            payment_date=None,
            facture=None
        )
    )
