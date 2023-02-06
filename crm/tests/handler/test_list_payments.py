from decimal import Decimal
from unittest.mock import AsyncMock

import pytest

from crm.agency_cabinet.documents.common.structs import Payment, ListPaymentsInput
from crm.agency_cabinet.documents.proto.payments_pb2 import (
    Payment as PbPayment,
    PaymentsList as PbPaymentsList,
    ListPaymentsInput as PbListPaymentsInput,
    ListPaymentsOutput as PbListPaymentsOutput,
)

from crm.agency_cabinet.documents.proto.request_pb2 import RpcRequest
from smb.common.testing_utils import dt

pytestmark = [pytest.mark.asyncio]


@pytest.fixture(autouse=True)
def procedure(mocker):
    mock = AsyncMock()
    mock.return_value = [
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
        ),
    ]

    mocker.patch(
        "crm.agency_cabinet.documents.server.src.procedures.ListPayments",
        return_value=mock,
    )

    return mock


async def test_calls_procedure(handler, procedure):
    input_pb = RpcRequest(
        list_payments=PbListPaymentsInput(
            agency_id=22,
        )
    )

    await handler(input_pb.SerializeToString())

    procedure.assert_awaited_with(
        params=ListPaymentsInput(
            agency_id=22,
            invoice_id=None,
            contract_id=None,
            limit=None,
            offset=None,
            date_to=None,
            date_from=None,
        )
    )


async def test_returns_serialized_operation_result(handler):
    input_pb = RpcRequest(
        list_payments=PbListPaymentsInput(
            agency_id=22,
        )
    )

    result = await handler(input_pb.SerializeToString())

    assert PbListPaymentsOutput.FromString(result) == PbListPaymentsOutput(
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
            ]
        )
    )
