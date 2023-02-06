from unittest.mock import AsyncMock

import pytest

from crm.agency_cabinet.client_bonuses.common.structs import CashbackProgram
from crm.agency_cabinet.gateway.server.src.exceptions import AccessDenied

URL = "/api/agencies/123/bonuses/cashback_programs"


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
        "crm.agency_cabinet.gateway.server.src.procedures.bonuses.ListCashbackPrograms",
        return_value=mock,
    )
    return mock


async def test_calls_procedure(client, procedure):
    await client.get(URL)

    procedure.assert_called_with(42, 123)


async def test_returns_cashback_programs(client):
    got = await client.get(URL)

    assert got == {
        "programs": [
            {
                "id": 1,
                "category_id": 1,
                "is_general": True,
                "is_enabled": True,
                "name_ru": "Программа 1",
                "name_en": "Program 1",
                "description_ru": "Программа кэшбека #1",
                "description_en": "Cashback program #1",
            },
            {
                "id": 2,
                "category_id": 2,
                "is_general": True,
                "is_enabled": False,
                "name_ru": "Программа 2",
                "name_en": "Program 2",
                "description_ru": "Программа кэшбека #2",
                "description_en": "Cashback program #2",
            },
            {
                "id": 3,
                "category_id": 1,
                "is_general": False,
                "is_enabled": False,
                "name_ru": "Программа 1",
                "name_en": "Program 1",
                "description_ru": "Программа кэшбека #1",
                "description_en": "Cashback program #1",
            },
            {
                "id": 4,
                "category_id": 3,
                "is_general": False,
                "is_enabled": True,
                "name_ru": "Программа 4",
                "name_en": "Program 4",
                "description_ru": "Программа кэшбека #4",
                "description_en": "Cashback program #4",
            },
        ]
    }


async def test_returns_403_if_access_denied(client, procedure):
    procedure.side_effect = AccessDenied()

    got = await client.get(URL, expected_status=403)

    assert got == {
        "error": {
            "error_code": "ACCESS_DENIED",
            "http_code": 403,
            "messages": [{"params": {}, "text": "You don't have access"}],
        }
    }
