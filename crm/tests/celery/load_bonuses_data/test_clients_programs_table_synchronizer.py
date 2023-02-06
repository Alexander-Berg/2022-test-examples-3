import pytest
from smb.common.testing_utils import Any

from crm.agency_cabinet.client_bonuses.server.lib.celery.tasks.load_bonuses_data.synchronizers import (  # noqa: E501
    ClientsProgramsTableSynchronizer,
)

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def synchronizer(con):
    return ClientsProgramsTableSynchronizer(con)


@pytest.fixture(autouse=True)
async def initial_data(factory):
    for client_id in (1, 2, 3):
        await factory.create_client(id_=client_id)


async def test_inserts_data_into_db(factory, synchronizer):
    await factory.create_cashback_program(
        id=1,
        category_id=1,
        is_general=True,
        is_enabled=True,
        name_ru="Программа 1",
        name_en="Program 1",
        description_ru="Программа кэшбека #1",
        description_en="Cashback program #1",
    )
    await factory.create_cashback_program(
        id=2,
        category_id=2,
        is_general=True,
        is_enabled=True,
        name_ru="Программа 2",
        name_en="Program 2",
        description_ru="Программа кэшбека #2",
        description_en="Cashback program #2",
    )
    await synchronizer.process_data(
        [
            (1, 1),
            (1, 2),
            (3, 2),
        ]
    )

    assert await factory.list_clients_programs() == [
        {
            "id": Any(int),
            "client_id": 1,
            "program_id": 1,
        },
        {
            "id": Any(int),
            "client_id": 1,
            "program_id": 2,
        },
        {
            "id": Any(int),
            "client_id": 3,
            "program_id": 2,
        },
    ]


async def test_removes_existing_values(factory, synchronizer):
    await factory.create_cashback_program(
        id=1,
        category_id=1,
        is_general=True,
        is_enabled=True,
        name_ru="Программа 1",
        name_en="Program 1",
        description_ru="Программа кэшбека #1",
        description_en="Cashback program #1",
    )
    await factory.create_cashback_program(
        id=2,
        category_id=2,
        is_general=True,
        is_enabled=True,
        name_ru="Программа 2",
        name_en="Program 2",
        description_ru="Программа кэшбека #2",
        description_en="Cashback program #2",
    )
    await factory.create_client_program(client_id=1, program_id=1)
    await factory.create_client_program(client_id=2, program_id=2)

    await synchronizer.process_data(
        [
            (1, 1),
            (1, 1),
            (3, 2),
        ]
    )

    assert await factory.list_clients_programs() == [
        {
            "id": Any(int),
            "client_id": 1,
            "program_id": 1,
        },
        {
            "id": Any(int),
            "client_id": 1,
            "program_id": 1,
        },
        {
            "id": Any(int),
            "client_id": 3,
            "program_id": 2,
        },
    ]
