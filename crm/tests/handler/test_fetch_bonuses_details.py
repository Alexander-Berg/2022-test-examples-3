from decimal import Decimal
from unittest.mock import AsyncMock

import pytest
from smb.common.testing_utils import dt

from crm.agency_cabinet.client_bonuses.common.structs import (
    BonusAmount,
    BonusDetails,
    BonusDetailsList,
    BonusStatusType as BonusStatusTypeEnum,
    FetchBonusesDetailsInput,
)
from crm.agency_cabinet.client_bonuses.proto import errors_pb2
from crm.agency_cabinet.client_bonuses.proto.bonuses_pb2 import (
    BonusAmount as BonusAmountPb,
    BonusDetails as BonusDetailsPb,
    BonusDetailsList as BonusDetailsListPb,
    BonusStatusType as BonusStatusTypePb,
    FetchBonusesDetailsInput as FetchBonusesDetailsInputPb,
    FetchBonusesDetailsOutput,
)
from crm.agency_cabinet.client_bonuses.proto.common_pb2 import TimePeriod
from crm.agency_cabinet.client_bonuses.proto.request_pb2 import RpcRequest
from crm.agency_cabinet.client_bonuses.server.lib.exceptions import ClientNotFound

pytestmark = [pytest.mark.asyncio]


@pytest.fixture(autouse=True)
def procedure(mocker):
    mock = AsyncMock()
    mock.return_value = BonusDetailsList(
        items=[
            BonusDetails(
                type=BonusStatusTypeEnum.spent,
                amounts=[],
                total=Decimal("500"),
                date=dt("2020-06-01 00:00:00"),
            ),
            BonusDetails(
                type=BonusStatusTypeEnum.accrued,
                amounts=[
                    BonusAmount(program_id=1, amount=Decimal("100.20")),
                    BonusAmount(program_id=2, amount=Decimal("10.20")),
                ],
                total=Decimal("110.40"),
                date=dt("2020-05-01 00:00:00"),
            ),
        ]
    )

    mocker.patch(
        "crm.agency_cabinet.client_bonuses.server.lib.handler.FetchBonusesDetails",
        return_value=mock,
    )

    return mock


async def test_calls_operation(handler, procedure):
    input_pb = RpcRequest(
        fetch_bonuses_details=FetchBonusesDetailsInputPb(
            agency_id=62342,
            client_id=11111,
            period=TimePeriod(
                datetime_start=dt("2021-01-05 14:48:00", as_proto=True),
                datetime_end=dt("2021-05-01 18:00:00", as_proto=True),
            ),
        )
    )

    await handler(input_pb.SerializeToString())

    procedure.assert_awaited_with(
        params=FetchBonusesDetailsInput(
            agency_id=62342,
            client_id=11111,
            datetime_start=dt("2021-01-05 14:48:00"),
            datetime_end=dt("2021-05-01 18:00:00"),
        )
    )


@pytest.mark.parametrize(
    "history_log, expected",
    [
        (BonusDetailsList(items=[]), []),
        (
            BonusDetailsList(
                items=[
                    BonusDetails(
                        type=BonusStatusTypeEnum.spent,
                        amounts=[],
                        total=Decimal("500"),
                        date=dt("2020-06-01 00:00:00"),
                    ),
                    BonusDetails(
                        type=BonusStatusTypeEnum.accrued,
                        amounts=[
                            BonusAmount(program_id=1, amount=Decimal("100.20")),
                            BonusAmount(program_id=2, amount=Decimal("10.20")),
                        ],
                        total=Decimal(2_000_000_000),
                        date=dt("2020-05-01 00:00:00"),
                    ),
                ]
            ),
            [
                BonusDetailsPb(
                    type=BonusStatusTypePb.SPENT,
                    amounts=[],
                    total="500",
                    date=dt("2020-06-01 00:00:00", as_proto=True),
                ),
                BonusDetailsPb(
                    type=BonusStatusTypePb.ACCRUED,
                    amounts=[
                        BonusAmountPb(program_id=1, amount="100.2"),
                        BonusAmountPb(program_id=2, amount="10.2"),
                    ],
                    total="2000000000",
                    date=dt("2020-05-01 00:00:00", as_proto=True),
                ),
            ],
        ),
    ],
)
async def test_returns_serialized_operation_result(
    history_log, expected, handler, procedure
):
    procedure.return_value = history_log

    input_pb = RpcRequest(
        fetch_bonuses_details=FetchBonusesDetailsInputPb(
            agency_id=62342,
            client_id=11111,
            period=TimePeriod(
                datetime_start=dt("2021-01-05 14:48:00", as_proto=True),
                datetime_end=dt("2021-05-01 18:00:00", as_proto=True),
            ),
        )
    )

    result = await handler(input_pb.SerializeToString())

    assert FetchBonusesDetailsOutput.FromString(result) == FetchBonusesDetailsOutput(
        details=BonusDetailsListPb(items=expected)
    )


async def test_returns_error_if_client_not_found(handler, procedure):
    procedure.side_effect = ClientNotFound(agency_id=62342, client_id=11111)

    input_pb = RpcRequest(
        fetch_bonuses_details=FetchBonusesDetailsInputPb(
            agency_id=62342,
            client_id=11111,
            period=TimePeriod(
                datetime_start=dt("2021-01-05 14:48:00", as_proto=True),
                datetime_end=dt("2021-05-01 18:00:00", as_proto=True),
            ),
        )
    )

    result = await handler(input_pb.SerializeToString())

    assert FetchBonusesDetailsOutput.FromString(result) == FetchBonusesDetailsOutput(
        client_not_found=errors_pb2.ClientNotFound(agency_id=62342, client_id=11111)
    )
