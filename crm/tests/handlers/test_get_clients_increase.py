import pytest
from decimal import Decimal
from unittest.mock import AsyncMock
import datetime
from crm.agency_cabinet.agencies.proto import request_pb2, analytics_pb2
from crm.agency_cabinet.agencies.common import structs
from crm.agency_cabinet.common.proto_utils import timestamp_or_none


@pytest.mark.parametrize(
    ("request_params", "expected"),
    [
        (
            {
                'left_date_from': timestamp_or_none(datetime.datetime(2020, 3, 1)),
                'left_date_to': timestamp_or_none(datetime.datetime(2020, 4, 1)),
                'right_date_from': timestamp_or_none(datetime.datetime(2020, 4, 1)),
                'right_date_to': timestamp_or_none(datetime.datetime(2020, 5, 1))
            },
            structs.GetClientsIncreaseResponse(
                other=[
                    structs.ClientsIncreasePart(new_customers=0, customers=1, new_customers_prev=0, customers_prev=0)],
                current_at_right_date=structs.ClientsIncreasePart(
                    new_customers=0,
                    customers=1,
                    new_customers_prev=0,
                    customers_prev=0
                ),
                current_at_left_date=structs.ClientsIncreasePart(
                    new_customers=1,
                    customers=1,
                    new_customers_prev=0,
                    customers_prev=0
                ),
                percent_less=Decimal('0.000')
            )
        )
    ]
)
async def test_get_clients_increase(mocker, handler, request_params, expected):
    procedure = AsyncMock()
    procedure.return_value = expected

    mocker.patch(
        "crm.agency_cabinet.agencies.server.src.procedures.GetClientsIncrease",
        return_value=procedure,
    )

    request_pb = request_pb2.RpcRequest(
        get_clients_increase=analytics_pb2.GetClientsIncrease(
            agency_id=1,
            **request_params
        )
    )
    output = await handler(request_pb.SerializeToString())

    procedure.assert_awaited_with(
        request=structs.GetClientsIncreaseRequest(
            agency_id=1,
            **{k: v.ToDatetime() for k, v in request_params.items()}
        )
    )

    response_proto = analytics_pb2.GetClientsIncreaseOutput.FromString(output)
    assert response_proto.HasField('result')
    response_struct = structs.GetClientsIncreaseResponse.from_proto(response_proto.result)
    assert response_struct == expected
