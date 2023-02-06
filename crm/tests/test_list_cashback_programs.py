import pytest

from crm.agency_cabinet.client_bonuses.common.structs import CashbackProgram
from crm.agency_cabinet.client_bonuses.proto.bonuses_pb2 import (
    ListCashbackProgramsOutput as PbListCashbackProgramsOutput,
    CashbackProgramsList as PbCashbackProgramsList,
    ListCashbackProgramsInput as PbListCashbackProgramsInput,
    CashbackProgram as PbCashbackProgram,
)
from crm.agency_cabinet.client_bonuses.proto.request_pb2 import (
    RpcRequest as PbRpcRequest,
)

pytestmark = [pytest.mark.asyncio]


async def test_sends_correct_request(
    client, rmq_rpc_client
):
    rmq_rpc_client.send_proto_message.return_value = PbListCashbackProgramsOutput(
        programs=PbCashbackProgramsList(programs=[])
    )

    await client.list_cashback_programs(agency_id=22)

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name="client-bonuses",
        message=PbRpcRequest(
            list_cashback_programs=PbListCashbackProgramsInput(
                agency_id=22,
            )
        ),
        response_message_type=PbListCashbackProgramsOutput,
    )


async def test_returns_data(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = PbListCashbackProgramsOutput(
        programs=PbCashbackProgramsList(
            programs=[
                PbCashbackProgram(
                    id=1,
                    category_id=1,
                    is_general=True,
                    is_enabled=True,
                    name_ru='Программа 1',
                    name_en='Program 1',
                    description_ru='Программа кэшбека #1',
                    description_en='Cashback program #1',
                ),
                PbCashbackProgram(
                    id=2,
                    category_id=2,
                    is_general=True,
                    is_enabled=False,
                    name_ru='Программа 2',
                    name_en='Program 2',
                    description_ru='Программа кэшбека #2',
                    description_en='Cashback program #2',
                ),
                PbCashbackProgram(
                    id=3,
                    category_id=1,
                    is_general=False,
                    is_enabled=False,
                    name_ru='Программа 1',
                    name_en='Program 1',
                    description_ru='Программа кэшбека #1',
                    description_en='Cashback program #1',
                ),
                PbCashbackProgram(
                    id=4,
                    category_id=3,
                    is_general=False,
                    is_enabled=True,
                    name_ru='Программа 4',
                    name_en='Program 4',
                    description_ru='Программа кэшбека #4',
                    description_en='Cashback program #4',
                ),
            ]
        )
    )

    got = await client.list_cashback_programs(agency_id=22)

    assert got == [
        CashbackProgram(
            id=1,
            category_id=1,
            is_general=True,
            is_enabled=True,
            name_ru='Программа 1',
            name_en='Program 1',
            description_ru='Программа кэшбека #1',
            description_en='Cashback program #1',
        ),
        CashbackProgram(
            id=2,
            category_id=2,
            is_general=True,
            is_enabled=False,
            name_ru='Программа 2',
            name_en='Program 2',
            description_ru='Программа кэшбека #2',
            description_en='Cashback program #2',
        ),
        CashbackProgram(
            id=3,
            category_id=1,
            is_general=False,
            is_enabled=False,
            name_ru='Программа 1',
            name_en='Program 1',
            description_ru='Программа кэшбека #1',
            description_en='Cashback program #1',
        ),
        CashbackProgram(
            id=4,
            category_id=3,
            is_general=False,
            is_enabled=True,
            name_ru='Программа 4',
            name_en='Program 4',
            description_ru='Программа кэшбека #4',
            description_en='Cashback program #4',
        ),
    ]


async def test_returns_empty_list_if_no_bonuses_found(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = PbListCashbackProgramsOutput(
        programs=PbCashbackProgramsList(programs=[])
    )

    result = await client.list_cashback_programs(agency_id=22)

    assert result == []
