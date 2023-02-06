from decimal import Decimal

import pytest
from smb.common.testing_utils import dt

from crm.agency_cabinet.client_bonuses.client import ClientNotFound
from crm.agency_cabinet.client_bonuses.common.structs import (
    BonusAmount,
    BonusDetails,
    BonusStatusType,
)
from crm.agency_cabinet.client_bonuses.proto.bonuses_pb2 import (
    BonusAmount as PbBonusAmount,
    BonusDetails as PbBonusDetails,
    BonusDetailsList as PbBonusDetailsList,
    BonusStatusType as PbBonusStatusType,
    FetchBonusesDetailsInput as PbFetchBonusesDetailsInput,
    FetchBonusesDetailsOutput as PbFetchBonusesDetailsOutput,
)
from crm.agency_cabinet.client_bonuses.proto.common_pb2 import TimePeriod
from crm.agency_cabinet.client_bonuses.proto.errors_pb2 import (
    ClientNotFound as PbClientNotFound,
)
from crm.agency_cabinet.client_bonuses.proto.request_pb2 import RpcRequest

pytestmark = [pytest.mark.asyncio]


async def test_sends_correct_request(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = PbFetchBonusesDetailsOutput(
        details=PbBonusDetailsList(items=[])
    )

    await client.fetch_bonuses_details(
        agency_id=123,
        client_id=567,
        datetime_start=dt("2020-06-07 00:00:00"),
        datetime_end=dt("2020-08-07 00:00:00"),
    )

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name="client-bonuses",
        message=RpcRequest(
            fetch_bonuses_details=PbFetchBonusesDetailsInput(
                agency_id=123,
                client_id=567,
                period=TimePeriod(
                    datetime_start=dt("2020-06-07 00:00:00", as_proto=True),
                    datetime_end=dt("2020-08-07 00:00:00", as_proto=True),
                ),
            )
        ),
        response_message_type=PbFetchBonusesDetailsOutput,
    )


@pytest.mark.parametrize(
    "bonuses_details, expected_details",
    [
        ([], []),
        (
            [
                PbBonusDetails(
                    type=PbBonusStatusType.SPENT,
                    amounts=[],
                    total="500",
                    date=dt("2020-06-01 00:00:00", as_proto=True),
                ),
                PbBonusDetails(
                    type=PbBonusStatusType.ACCRUED,
                    amounts=[
                        PbBonusAmount(program_id=1, amount="100.2"),
                        PbBonusAmount(program_id=2, amount="10.2"),
                    ],
                    total="2000000000",
                    date=dt("2020-05-01 00:00:00", as_proto=True),
                ),
            ],
            [
                BonusDetails(
                    type=BonusStatusType.spent,
                    amounts=[],
                    total=Decimal("500"),
                    date=dt("2020-06-01 00:00:00"),
                ),
                BonusDetails(
                    type=BonusStatusType.accrued,
                    amounts=[
                        BonusAmount(program_id=1, amount=Decimal("100.20")),
                        BonusAmount(program_id=2, amount=Decimal("10.20")),
                    ],
                    total=Decimal(2_000_000_000),
                    date=dt("2020-05-01 00:00:00"),
                ),
            ],
        ),
    ],
)
async def test_returns_data(bonuses_details, expected_details, client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = PbFetchBonusesDetailsOutput(
        details=PbBonusDetailsList(items=bonuses_details)
    )

    got = await client.fetch_bonuses_details(
        agency_id=123,
        client_id=567,
        datetime_start=dt("2020-06-07 00:00:00"),
        datetime_end=dt("2020-08-07 00:00:00"),
    )

    assert got == expected_details


async def test_raises_if_client_not_found(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = PbFetchBonusesDetailsOutput(
        client_not_found=PbClientNotFound(client_id=567, agency_id=123)
    )

    with pytest.raises(ClientNotFound) as exc:
        await client.fetch_bonuses_details(
            agency_id=123,
            client_id=567,
            datetime_start=dt("2020-06-07 00:00:00"),
            datetime_end=dt("2020-08-07 00:00:00"),
        )

    assert exc.value.agency_id == 123
    assert exc.value.client_id == 567
