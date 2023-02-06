import pytest

from crm.agency_cabinet.client_bonuses.server.lib.celery.tasks.load_cashback_programs.synchronizers import (  # noqa: E501
    CashbackProgramsSynchronizer,
)

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def synchronizer(con):
    return CashbackProgramsSynchronizer(con)


@pytest.fixture(autouse=True)
async def initial_data(factory):
    for i in (1, 2):
        await factory.create_cashback_program(
            id=i,
            category_id=i,
            is_general=True,
            is_enabled=True,
            name_ru=f"Изначальная Программа {i}",
            name_en=f"Initial Program {i}",
            description_ru=f"Изначальная Программа кэшбека #{i}",
            description_en=f"Initial Cashback program #{i}",
        )


async def test_inserts_data_into_db(factory, synchronizer):
    await synchronizer.process_data(
        [
            (1, 1, True, False, "Программа 1", "Program 1", "Программа кэшбека #1", "Cashback program #1"),
            (2, 1, False, True, "Программа 1", "Program 1", "Программа кэшбека #1", "Cashback program #1"),
        ]
    )

    assert await factory.list_cashback_programs() == [
        {
            "id": 1,
            "category_id": 1,
            "is_general": True,
            "is_enabled": False,
            "name_ru": "Программа 1",
            "name_en": "Program 1",
            "description_ru": "Программа кэшбека #1",
            "description_en": "Cashback program #1",
        },
        {
            "id": 2,
            "category_id": 1,
            "is_general": False,
            "is_enabled": True,
            "name_ru": "Программа 1",
            "name_en": "Program 1",
            "description_ru": "Программа кэшбека #1",
            "description_en": "Cashback program #1",
        }
    ]
