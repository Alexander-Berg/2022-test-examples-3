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
            structs.GetActiveClientsResponse(
                other=[structs.ActiveClientsPart(customers_at_left_date=2, customers_at_right_date=1)],
                current=structs.ActiveClientsPart(customers_at_left_date=1, customers_at_right_date=1),
                percent_less=Decimal('0.000')
            )
        )
    ]
)
async def test_get_active_clients(mocker, handler, request_params, expected):
    procedure = AsyncMock()
    procedure.return_value = expected

    mocker.patch(
        "crm.agency_cabinet.agencies.server.src.procedures.GetActiveClients",
        return_value=procedure,
    )

    request_pb = request_pb2.RpcRequest(
        get_active_clients=analytics_pb2.GetActiveClients(
            agency_id=1,
            **request_params

        )
    )
    output = await handler(request_pb.SerializeToString())

    procedure.assert_awaited_with(
        request=structs.GetActiveClientsRequest(
            agency_id=1,
            **{k: v.ToDatetime() for k, v in request_params.items()}
        )
    )

    response_proto = analytics_pb2.GetActiveClientsOutput.FromString(output)
    assert response_proto.HasField('result')
    response_struct = structs.GetActiveClientsResponse.from_proto(response_proto.result)
    assert response_struct == expected
