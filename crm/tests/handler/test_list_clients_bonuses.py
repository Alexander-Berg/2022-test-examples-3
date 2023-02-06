from decimal import Decimal
from unittest.mock import AsyncMock

import pytest
from smb.common.testing_utils import dt

from crm.agency_cabinet.client_bonuses.common.structs import (
    BonusType,
    ClientBonus,
    ClientType,
    ListClientsBonusesInput,
)
from crm.agency_cabinet.client_bonuses.proto.bonuses_pb2 import (
    BonusType as PbBonusType,
    ClientBonus as PbClientBonus,
    ClientsBonusesList as PbClientsBonusesList,
    ClientType as PbClientType,
    ListClientsBonusesInput as PbListClientsBonusesInput,
    ListClientsBonusesOutput as PbListClientsBonusesOutput,
)
from crm.agency_cabinet.client_bonuses.proto.common_pb2 import TimePeriod
from crm.agency_cabinet.client_bonuses.proto.request_pb2 import RpcRequest

pytestmark = [pytest.mark.asyncio]


@pytest.fixture(autouse=True)
def procedure(mocker):
    mock = AsyncMock()
    mock.return_value = [
        ClientBonus(
            client_id=1,
            email="alpaca@yandex.ru",
            accrued=Decimal("50"),
            spent=Decimal("100"),
            awarded=Decimal("150"),
            active=True,
            currency="RUR",
        ),
        ClientBonus(
            client_id=2,
            email="capibara@yandex.ru",
            accrued=None,
            spent=None,
            awarded=None,
            active=False,
            currency="RUR",
        ),
    ]

    mocker.patch(
        "crm.agency_cabinet.client_bonuses.server.lib.handler.ListClientsBonuses",
        return_value=mock,
    )

    return mock


@pytest.mark.parametrize(
    "pb_bonus_type, bonus_type",
    [
        (PbBonusType.ALL_BONUSES, BonusType.ALL),
        (
            PbBonusType.WITH_ACTIVATION_OVER_PERIOD,
            BonusType.WITH_ACTIVATION_OVER_PERIOD,
        ),
        (
            PbBonusType.WITH_SPENDS_OVER_PERIOD,
            BonusType.WITH_SPENDS_OVER_PERIOD,
        )
    ],
)
@pytest.mark.parametrize(
    "pb_client_type, client_type",
    [
        (PbClientType.ALL_CLIENTS, ClientType.ALL),
        (PbClientType.EXCLUDED, ClientType.EXCLUDED),
        (PbClientType.ACTIVE, ClientType.ACTIVE),
    ],
)
@pytest.mark.parametrize("search_query", [None, "something"])
async def test_calls_procedure(
    bonus_type, pb_bonus_type, pb_client_type, client_type, search_query, handler, procedure
):
    input_pb = RpcRequest(
        list_clients_bonuses=PbListClientsBonusesInput(
            agency_id=22,
            bonus_type=pb_bonus_type,
            client_type=pb_client_type,
            limit=10,
            offset=0,
            period=TimePeriod(
                datetime_start=dt("2020-08-22 10:10:10", as_proto=True),
                datetime_end=dt("2020-08-22 10:10:10", as_proto=True),
            ),
            search_query=search_query,
        )
    )

    await handler(input_pb.SerializeToString())

    procedure.assert_awaited_with(
        params=ListClientsBonusesInput(
            agency_id=22,
            bonus_type=bonus_type,
            client_type=client_type,
            limit=10,
            offset=0,
            datetime_start=dt("2020-08-22 10:10:10"),
            datetime_end=dt("2020-08-22 10:10:10"),
            search_query=search_query,
        )
    )


async def test_returns_serialized_operation_result(handler):
    input_pb = RpcRequest(
        list_clients_bonuses=PbListClientsBonusesInput(
            agency_id=22,
            bonus_type=PbBonusType.ALL_BONUSES,
            client_type=PbClientType.ALL_CLIENTS,
            limit=10,
            offset=0,
            period=TimePeriod(
                datetime_start=dt("2020-08-22 10:10:10", as_proto=True),
                datetime_end=dt("2020-08-22 10:10:10", as_proto=True),
            ),
            search_query="something",
        )
    )

    result = await handler(input_pb.SerializeToString())

    assert PbListClientsBonusesOutput.FromString(result) == PbListClientsBonusesOutput(
        bonuses=PbClientsBonusesList(
            bonuses=[
                PbClientBonus(
                    client_id=1,
                    email="alpaca@yandex.ru",
                    accrued="50",
                    spent="100",
                    awarded="150",
                    active=True,
                    currency="RUR",
                ),
                PbClientBonus(
                    client_id=2,
                    email="capibara@yandex.ru",
                    accrued=None,
                    spent=None,
                    awarded=None,
                    active=False,
                    currency="RUR",
                ),
            ]
        )
    )
