import pytest

from crm.agency_cabinet.client_bonuses.common.structs import (
    ClientBonusSettings
)

from crm.agency_cabinet.client_bonuses.proto.bonuses_pb2 import (
    ClientBonusSettings as PbClientBonusSettings,
    GetClientsBonusesSettingsInput as PbGetClientsBonusesSettingsInput,
    GetClientsBonusesSettingsOutput as PbGetClientsBonusesSettingsOutput,
)
from crm.agency_cabinet.client_bonuses.proto.request_pb2 import (
    RpcRequest as PbRpcRequest,
)
from smb.common.testing_utils import dt

pytestmark = [pytest.mark.asyncio]


async def test_sends_correct_request(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = PbGetClientsBonusesSettingsOutput(
        settings=PbClientBonusSettings(
            first_date=dt('2020-11-11 00:00:00', as_proto=True),
            last_date=dt('2021-11-11 00:00:00', as_proto=True)
        )
    )

    await client.get_clients_bonuses_settings(agency_id=22)

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name="client-bonuses",
        message=PbRpcRequest(
            get_clients_bonuses_settings=PbGetClientsBonusesSettingsInput(
                agency_id=22)
        ),
        response_message_type=PbGetClientsBonusesSettingsOutput,
    )


async def test_returns_data(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = PbGetClientsBonusesSettingsOutput(
        settings=PbClientBonusSettings(
            first_date=dt('2020-11-11 00:00:00', as_proto=True),
            last_date=dt('2021-11-11 00:00:00', as_proto=True)
        )
    )

    got = await client.get_clients_bonuses_settings(agency_id=22)

    assert got == ClientBonusSettings(
        first_date=dt('2020-11-11 00:00:00'),
        last_date=dt('2021-11-11 00:00:00')
    )


async def test_returns_empty_values_if_no_bonuses_found(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = PbGetClientsBonusesSettingsOutput(
        settings=PbClientBonusSettings(
            first_date=None,
            last_date=None
        )
    )

    result = await client.get_clients_bonuses_settings(agency_id=22)

    assert result == ClientBonusSettings(
        first_date=None,
        last_date=None
    )
