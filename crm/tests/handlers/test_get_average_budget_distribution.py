import pytest
from decimal import Decimal
from unittest.mock import AsyncMock
import datetime
from crm.agency_cabinet.agencies.proto import request_pb2, analytics_pb2
from crm.agency_cabinet.agencies.common import structs
from crm.agency_cabinet.common.proto_utils import timestamp_or_none


@pytest.mark.parametrize(
    ("date_to", "expected"),
    [
        (
            datetime.datetime(2020, 4, 1),
            structs.GetAverageBudgetDistributionResponse(
                current=[
                    structs.AverageBudgetMarketPiePart(
                        percent=Decimal('100.000'),
                        grade='0-50',
                        customers=1
                    ),
                    structs.AverageBudgetMarketPiePart(
                        percent=Decimal('0.000'),
                        grade='50-200',
                        customers=0
                    ),
                    structs.AverageBudgetMarketPiePart(
                        percent=Decimal('0.000'),
                        grade='200+',
                        customers=0
                    )
                ],
                other=[
                    structs.AverageBudgetMarketPiePart(
                        percent=Decimal('0.000'),
                        grade='0-50',
                        customers=0
                    ),
                    structs.AverageBudgetMarketPiePart(
                        percent=Decimal('100.000'),
                        grade='50-200',
                        customers=2
                    ),
                    structs.AverageBudgetMarketPiePart(
                        percent=Decimal('0.000'),
                        grade='200+',
                        customers=0
                    )
                ],
                median_budget_current=Decimal('10.000'),
                median_budget_other=Decimal('150.000')
            )
        ),
        (
            datetime.datetime(2020, 5, 1),
            structs.GetAverageBudgetDistributionResponse(
                current=[
                    structs.AverageBudgetMarketPiePart(
                        percent=Decimal('0.000'),
                        grade='0-50',
                        customers=0
                    ),
                    structs.AverageBudgetMarketPiePart(
                        percent=Decimal('0.000'),
                        grade='50-200',
                        customers=0
                    ),
                    structs.AverageBudgetMarketPiePart(
                        percent=Decimal('100.000'),
                        grade='200+',
                        customers=1
                    )
                ],
                other=[
                    structs.AverageBudgetMarketPiePart(
                        percent=Decimal('0.000'),
                        grade='0-50',
                        customers=0
                    ),
                    structs.AverageBudgetMarketPiePart(
                        percent=Decimal('50.000'),
                        grade='50-200',
                        customers=1
                    ),
                    structs.AverageBudgetMarketPiePart(
                        percent=Decimal('50.000'),
                        grade='200+',
                        customers=1
                    )
                ],
                median_budget_current=Decimal('505.000'),
                median_budget_other=Decimal('325.000')
            )
        )
    ]
)
async def test_get_average_budget_distribution(handler, mocker, date_to, expected):
    procedure = AsyncMock()
    procedure.return_value = expected

    mocker.patch(
        "crm.agency_cabinet.agencies.server.src.procedures.GetAverageBudgetDistribution",
        return_value=procedure,
    )

    request_pb = request_pb2.RpcRequest(
        get_average_budget_distribution=analytics_pb2.GetAverageBudgetDistribution(
            agency_id=1,
            date_from=timestamp_or_none(datetime.datetime(2020, 1, 1)),
            date_to=timestamp_or_none(date_to),
        )
    )
    output = await handler(request_pb.SerializeToString())

    procedure.assert_awaited_with(
        request=structs.GetAverageBudgetDistributionRequest(
            agency_id=1,
            date_from=datetime.datetime(2020, 1, 1),
            date_to=date_to,
        )
    )

    response_proto = analytics_pb2.GetAverageBudgetDistributionOutput.FromString(output)
    assert response_proto.HasField('result')
    response_struct = structs.GetAverageBudgetDistributionResponse.from_proto(response_proto.result)

    assert response_struct == expected
