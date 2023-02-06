from unittest.mock import AsyncMock

import pytest

from crm.agency_cabinet.client_bonuses.common.structs import (
    CashbackProgram,
    ListCashbackProgramsInput,
)

from crm.agency_cabinet.client_bonuses.proto.bonuses_pb2 import (
    ListCashbackProgramsInput as PbListCashbackProgramsInput,
    ListCashbackProgramsOutput as PbListCashbackProgramsOutput,
    CashbackProgramsList as PbCashbackProgramsList,
    CashbackProgram as PbCashbackProgram,
)
from crm.agency_cabinet.client_bonuses.proto.request_pb2 import RpcRequest

pytestmark = [pytest.mark.asyncio]


@pytest.fixture(autouse=True)
def procedure(mocker):
    mock = AsyncMock()
    mock.return_value = [
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

    mocker.patch(
        "crm.agency_cabinet.client_bonuses.server.lib.handler.ListCashbackPrograms",
        return_value=mock,
    )

    return mock


async def test_calls_procedure(handler, procedure):
    input_pb = RpcRequest(
        list_cashback_programs=PbListCashbackProgramsInput(
            agency_id=22,
        )
    )

    await handler(input_pb.SerializeToString())

    procedure.assert_awaited_with(
        params=ListCashbackProgramsInput(
            agency_id=22,
        )
    )


async def test_returns_serialized_operation_result(handler):
    input_pb = RpcRequest(
        list_cashback_programs=PbListCashbackProgramsInput(
            agency_id=22,
        )
    )

    result = await handler(input_pb.SerializeToString())

    assert PbListCashbackProgramsOutput.FromString(result) == PbListCashbackProgramsOutput(
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
