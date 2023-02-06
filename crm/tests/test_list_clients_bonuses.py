from decimal import Decimal

import pytest
from smb.common.testing_utils import dt

from crm.agency_cabinet.client_bonuses.common.structs import BonusType, ClientBonus, ClientType
from crm.agency_cabinet.client_bonuses.proto.bonuses_pb2 import (
    BonusType as PbBonusType,
    ClientBonus as PbClientBonus,
    ClientsBonusesList as PbClientsBonusesList,
    ClientType as PbClientType,
    ListClientsBonusesInput as PbListClientsBonusesInput,
    ListClientsBonusesOutput as PbListClientsBonusesOutput,
)
from crm.agency_cabinet.client_bonuses.proto.common_pb2 import (
    TimePeriod as PbTimePeriod,
)
from crm.agency_cabinet.client_bonuses.proto.request_pb2 import (
    RpcRequest as PbRpcRequest,
)

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize(
    "bonus_type, pb_bonus_type",
    [
        (BonusType.ALL, PbBonusType.ALL_BONUSES),
        (
            BonusType.WITH_ACTIVATION_OVER_PERIOD,
            PbBonusType.WITH_ACTIVATION_OVER_PERIOD,
        ),
        (
            BonusType.WITH_SPENDS_OVER_PERIOD,
            PbBonusType.WITH_SPENDS_OVER_PERIOD,
        ),
    ],
)
@pytest.mark.parametrize(
    "client_type, pb_client_type",
    [
        (ClientType.ALL, PbClientType.ALL_CLIENTS),
        (ClientType.EXCLUDED, PbClientType.EXCLUDED),
        (ClientType.ACTIVE, PbClientType.ACTIVE),
    ],
)
@pytest.mark.parametrize("search_query", [None, "something"])
async def test_sends_correct_request(
    bonus_type, pb_bonus_type, client_type, pb_client_type, search_query, client, rmq_rpc_client
):
    rmq_rpc_client.send_proto_message.return_value = PbListClientsBonusesOutput(
        bonuses=PbClientsBonusesList(bonuses=[])
    )

    await client.list_clients_bonuses(
        agency_id=22,
        limit=10,
        offset=0,
        datetime_start=dt("2020-08-22 10:10:10"),
        datetime_end=dt("2020-08-22 10:10:10"),
        search_query=search_query,
        bonus_type=bonus_type,
        client_type=client_type,
    )

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name="client-bonuses",
        message=PbRpcRequest(
            list_clients_bonuses=PbListClientsBonusesInput(
                agency_id=22,
                limit=10,
                offset=0,
                period=PbTimePeriod(
                    datetime_start=dt("2020-08-22 10:10:10", as_proto=True),
                    datetime_end=dt("2020-08-22 10:10:10", as_proto=True),
                ),
                bonus_type=pb_bonus_type,
                client_type=pb_client_type,
                search_query=search_query,
            )
        ),
        response_message_type=PbListClientsBonusesOutput,
    )


async def test_returns_data(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = PbListClientsBonusesOutput(
        bonuses=PbClientsBonusesList(
            bonuses=[
                PbClientBonus(
                    client_id=1,
                    email="alpaca@yandex.ru",
                    accrued="2.20",
                    spent="300",
                    awarded="55",
                    active=True,
                    currency="RUR",
                ),
                PbClientBonus(
                    client_id=3,
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

    got = await client.list_clients_bonuses(
        agency_id=22,
        limit=10,
        offset=0,
        datetime_start=dt("2020-08-22 10:10:10"),
        datetime_end=dt("2020-08-22 10:10:10"),
        bonus_type=BonusType.ALL,
        client_type=ClientType.ALL,
        search_query=None,
    )

    assert got == [
        ClientBonus(
            client_id=1,
            email="alpaca@yandex.ru",
            accrued=Decimal("2.20"),
            spent=Decimal("300"),
            awarded=Decimal("55"),
            active=True,
            currency="RUR",
        ),
        ClientBonus(
            client_id=3,
            email="capibara@yandex.ru",
            accrued=None,
            spent=None,
            awarded=None,
            active=False,
            currency="RUR",
        ),
    ]


async def test_returns_empty_list_if_no_bonuses_found(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = PbListClientsBonusesOutput(
        bonuses=PbClientsBonusesList(bonuses=[])
    )

    result = await client.list_clients_bonuses(
        agency_id=22,
        limit=10,
        offset=0,
        bonus_type=BonusType.ALL,
        client_type=ClientType.ALL,
        datetime_start=dt("2020-08-22 10:10:10"),
        datetime_end=dt("2020-08-22 10:10:10"),
        search_query="112",
    )

    assert result == []
