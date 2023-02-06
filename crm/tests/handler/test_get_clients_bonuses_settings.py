from unittest.mock import AsyncMock

import pytest
from google.protobuf.timestamp_pb2 import Timestamp
from datetime import datetime, timedelta

from crm.agency_cabinet.client_bonuses.common.structs import (
    ClientBonusSettings,
    GetClientsBonusesSettingsInput,
)

from crm.agency_cabinet.client_bonuses.proto.bonuses_pb2 import (
    ClientBonusSettings as PbClientBonusSettings,
    GetClientsBonusesSettingsInput as PbGetClientsBonusesSettingsInput,
    GetClientsBonusesSettingsOutput as PbGetClientsBonusesSettingsOutput,
)

from crm.agency_cabinet.client_bonuses.proto.request_pb2 import RpcRequest

pytestmark = [pytest.mark.asyncio]


@pytest.fixture(autouse=True)
def procedure(mocker):
    mock = AsyncMock()
    first_date = Timestamp()
    last_date = Timestamp()

    mock.return_value = ClientBonusSettings(
        first_date=first_date.FromDatetime(datetime.now() - timedelta(days=32)),
        last_date=last_date.FromDatetime(datetime.now() - timedelta(days=32))
    )

    mocker.patch(
        "crm.agency_cabinet.client_bonuses.server.lib.handler.GetClientsBonusesSettings",
        return_value=mock,
    )

    return mock


async def test_calls_procedure(handler, procedure):
    input_pb = RpcRequest(
        get_clients_bonuses_settings=PbGetClientsBonusesSettingsInput(
            agency_id=22,
        )
    )

    await handler(input_pb.SerializeToString())

    procedure.assert_awaited_with(
        params=GetClientsBonusesSettingsInput(agency_id=22)
    )


async def test_returns_serialized_operation_result(handler):
    input_pb = RpcRequest(
        get_clients_bonuses_settings=PbGetClientsBonusesSettingsInput(
            agency_id=22,
        )
    )

    result = await handler(input_pb.SerializeToString())

    first_date = Timestamp()
    last_date = Timestamp()

    assert PbGetClientsBonusesSettingsOutput.FromString(result) == PbGetClientsBonusesSettingsOutput(
        settings=PbClientBonusSettings(
            first_date=first_date.FromDatetime(datetime.now() - timedelta(days=32)),
            last_date=last_date.FromDatetime(datetime.now() - timedelta(days=32))
        )
    )


async def test_returns_empty_result(handler):
    input_pb = RpcRequest(
        get_clients_bonuses_settings=PbGetClientsBonusesSettingsInput(
            agency_id=0,
        )
    )

    result = await handler(input_pb.SerializeToString())

    assert PbGetClientsBonusesSettingsOutput.FromString(result) == PbGetClientsBonusesSettingsOutput(
        settings=PbClientBonusSettings(
            first_date=None,
            last_date=None
        )
    )
